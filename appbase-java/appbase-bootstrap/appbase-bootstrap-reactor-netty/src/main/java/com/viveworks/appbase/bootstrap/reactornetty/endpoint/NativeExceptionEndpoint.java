/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.bootstrap.reactornetty.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.DispatchException;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionService;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionServiceMXBean;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.RedactedExceptionMessageDetails;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.ListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.errorBody;
import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.requireParam;

/**
 * The native, non-blocking {@code /exception} route: R3 landed a reactive {@link ExceptionAccessor}, so
 * the five reads that used to be offloaded ({@code exceptionMessageDetails}, {@code
 * exceptionMessageDetailsById}, {@code exceptionMessageDetailsByTimestamps}, {@code
 * uniqueExceptionTypeNames}, {@code exceptionMessageDetailsByThrowableTypeName}) now run through the
 * reactive accessor instead of {@code Mono.fromCallable(...).subscribeOn(...)} -- no bounded worker held
 * for the duration of a Lucene search. Mirrors {@link NativeLogEndpoint}.
 *
 * <p>Every operation the offloaded {@code /exception} arm served is reproduced here with the SAME
 * response bytes as that arm, which read through {@link JmxExceptionServiceMXBean}. The five detail
 * reads go through the reactive accessor and are then serialised with the SAME {@code
 * JsonUtil}-backed {@code writerFor(RedactedExceptionMessageDetails.class)} writer {@code
 * JmxExceptionService#jsonOf} uses (Lens 4 / L4-04: both faces serialise the redacted view, never
 * the raw entity, so the free-text {@code throwableMessage}/{@code stackTrace}/{@code context}
 * fields never reach either transport), so the JSON is byte-identical; {@code uniqueExceptionTypeNames}
 * carries the raw type-name strings unchanged, exactly as the MXBean does. The five in-memory
 * operations ({@code exceptionListenersByExceptionTypeMap}, {@code globalExceptionListeners}, {@code
 * exceptionStoreFactoryByConfigurationTypeNameMap}, {@code exceptionStores}, {@code status}) touch no
 * store I/O and stay on {@link JmxExceptionServiceMXBean} unchanged, run directly on the event loop.
 *
 * <p><b>Skip-if-null parity.</b> The offloaded arm's source, {@code JmxExceptionService}, drops any
 * store whose detail read yields {@code null} (its {@code if (... != null)} guards). The reactive
 * accessor reproduces this for free: a {@link Mono} cannot carry {@code null}, so a store contributing
 * {@code null} simply emits no map entry -- the same observable output as the offloaded arm.
 *
 * <p>A missing or unsupported {@code operation}, or a missing required parameter, is mapped to 400 via
 * the SAME {@link DispatchException} and helpers {@link OffloadedEndpoints} uses ({@link EndpointReads}).
 * A non-numeric numeric parameter ({@code id}, the timestamps) throws {@link NumberFormatException}
 * unmapped -- the same faithful reproduction the offloaded endpoints gave those parameters, surfaced as
 * an unmapped error mapped to 500 further up the chain. The {@code status} value is JSON-quoted rather
 * than raw text, the same divergence {@link NativeLogEndpoint} inherits from its offloaded predecessor.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class NativeExceptionEndpoint {

    private NativeExceptionEndpoint() {
    }

    /**
     * Registers the native {@code /exception} route on {@code registry}.
     *
     * @param registry route table to populate
     * @param appbaseCore Appbase core, providing the reactive {@link ExceptionAccessor} and the
     *     JMX-backed {@link JmxExceptionServiceMXBean} for the in-memory operations
     */
    public static void register(final AppbaseRouteRegistry registry, final AppbaseCore appbaseCore) {
        AppbaseUtil.notNull(registry, "registry");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        registry.route("GET", AppbaseServiceConstants.DEFAULT_EXCEPTION_SERVLET_PATH,
                exchange -> Mono.defer(() -> {
                    try {
                        return dispatch(appbaseCore, exchange);
                    } catch (final DispatchException dispatchException) {
                        exchange.status(dispatchException.status());
                        return exchange.writeJson(
                                errorBody(dispatchException.status(), dispatchException.getMessage()));
                    }
                }));
    }

    /**
     * Reproduces {@code ExceptionServlet#doGet}'s read dispatch, with the five detail reads running
     * through the reactive {@link ExceptionAccessor} instead of the blocking {@link
     * JmxExceptionServiceMXBean}.
     *
     * @param appbaseCore Appbase core
     * @param exchange the request/response exchange
     * @return completion signal; the response has already been written
     * @throws DispatchException a 400, for a missing/unsupported operation or missing parameter --
     *     caught by {@link #register}, never left to escape
     */
    private static Mono<Void> dispatch(final AppbaseCore appbaseCore, final AppbaseServiceExchange exchange) {
        final String operation = requireParam(exchange, AppbaseServiceConstants.OPERATION_PARAMETER).trim();
        final ExceptionAccessor exceptionAccessor = appbaseCore.getExceptionService().getExceptionAccessor();

        if ("exceptionMessageDetails".equalsIgnoreCase(operation)) {
            return exceptionAccessor.readAllExceptionMessageDetailsReactive()
                    .map(NativeExceptionEndpoint::toDetailsEnvelope)
                    .flatMap(exchange::writeJson);
        }
        if ("exceptionMessageDetailsById".equalsIgnoreCase(operation)) {
            final int id = Integer.parseInt(requireParam(exchange, "id"));
            return exceptionAccessor.readExceptionMessageDetailsByIdReactive(id)
                    .map(NativeExceptionEndpoint::toByIdEnvelope)
                    .flatMap(exchange::writeJson);
        }
        if ("exceptionMessageDetailsByTimestamps".equalsIgnoreCase(operation)) {
            final long inclusiveMinTimestamp = Long.parseLong(requireParam(exchange, "inclusiveMinTimestamp"));
            final long exclusiveMaxTimestamp = Long.parseLong(requireParam(exchange, "exclusiveMaxTimestamp"));
            return exceptionAccessor
                    .readAllExceptionMessageDetailsByTimestampsReactive(inclusiveMinTimestamp, exclusiveMaxTimestamp)
                    .map(NativeExceptionEndpoint::toDetailsEnvelope)
                    .flatMap(exchange::writeJson);
        }
        if ("uniqueExceptionTypeNames".equalsIgnoreCase(operation)) {
            return exceptionAccessor.readUniqueExceptionTypeNamesReactive()
                    .map(NativeExceptionEndpoint::toTypeNamesEnvelope)
                    .flatMap(exchange::writeJson);
        }
        if ("exceptionMessageDetailsByThrowableTypeName".equalsIgnoreCase(operation)) {
            final String throwableTypeName = requireParam(exchange, "throwableTypeName");
            return exceptionAccessor.readExceptionMessageDetailsByThrowableTypeNameReactive(throwableTypeName)
                    .map(NativeExceptionEndpoint::toDetailsEnvelope)
                    .flatMap(exchange::writeJson);
        }

        final JmxExceptionServiceMXBean jmxExceptionServiceMXBean =
                appbaseCore.getJmxService().getMXBean(JmxExceptionService.class);
        if ("exceptionListenersByExceptionTypeMap".equalsIgnoreCase(operation)) {
            return exchange.writeJson(
                    MapOfStringListOfStringsValue.from(jmxExceptionServiceMXBean.getExceptionListenersByExceptionTypeMap()));
        }
        if ("globalExceptionListeners".equalsIgnoreCase(operation)) {
            return exchange.writeJson(new ListOfStringsValue(jmxExceptionServiceMXBean.getGlobalExceptionListeners()));
        }
        if ("exceptionStoreFactoryByConfigurationTypeNameMap".equalsIgnoreCase(operation)) {
            return exchange.writeJson(
                    new MapOfStringStringValue(jmxExceptionServiceMXBean.getExceptionStoreFactoryByConfigurationTypeNameMap()));
        }
        if ("exceptionStores".equalsIgnoreCase(operation)) {
            return exchange.writeJson(new ListOfStringsValue(jmxExceptionServiceMXBean.getExceptionStores()));
        }
        if ("status".equalsIgnoreCase(operation)) {
            // Same raw-text-vs-JSON-string caveat as LogServlet#getStatus (see NativeLogEndpoint):
            // AppbaseServiceExchange exposes only writeJson, so the ServiceStatus name is JSON-quoted
            // rather than written as raw text. Same value, JSON-quoted.
            return exchange.writeJson(jmxExceptionServiceMXBean.getStatus().name());
        }

        throw new DispatchException(400, "Unsupported operation " + operation);
    }

    /**
     * Reproduces {@code JmxExceptionService#jsonListOf} applied to a reactively read {@code Map<String,
     * Collection<ExceptionMessageDetails>>}, wrapping the result in the SAME {@link
     * MapOfStringListOfStringsValue} envelope the offloaded (and, before it, the servlet) face used.
     *
     * @param detailsByStoreName the reactive accessor's result
     * @return the same {@code Map<String, List<String>>}-of-JSON envelope the MXBean produced
     */
    private static MapOfStringListOfStringsValue toDetailsEnvelope(
            final Map<String, Collection<ExceptionMessageDetails>> detailsByStoreName) {
        final Map<String, List<String>> jsonByStoreName = new HashMap<>();
        if (detailsByStoreName != null) {
            for (final Map.Entry<String, Collection<ExceptionMessageDetails>> entry : detailsByStoreName.entrySet()) {
                final List<String> jsonList = new ArrayList<>();
                for (final ExceptionMessageDetails exceptionMessageDetails : entry.getValue()) {
                    jsonList.add(writeExceptionMessageDetailsJson(exceptionMessageDetails));
                }
                jsonByStoreName.put(entry.getKey(), jsonList);
            }
        }

        return MapOfStringListOfStringsValue.from(jsonByStoreName);
    }

    /**
     * Reproduces {@code JmxExceptionService#readExceptionMessageDetailsById}'s per-store serialisation
     * applied to a reactively read {@code Map<String, ExceptionMessageDetails>}, wrapping the result in
     * the SAME {@link MapOfStringStringValue} envelope the offloaded face used.
     *
     * @param detailsByStoreName the reactive accessor's result
     * @return the same {@code Map<String, String>}-of-JSON envelope the MXBean produced
     */
    private static MapOfStringStringValue toByIdEnvelope(
            final Map<String, ExceptionMessageDetails> detailsByStoreName) {
        final Map<String, String> jsonByStoreName = new HashMap<>();
        if (detailsByStoreName != null) {
            for (final Map.Entry<String, ExceptionMessageDetails> entry : detailsByStoreName.entrySet()) {
                jsonByStoreName.put(entry.getKey(), writeExceptionMessageDetailsJson(entry.getValue()));
            }
        }

        return new MapOfStringStringValue(jsonByStoreName);
    }

    /**
     * Reproduces {@code JmxExceptionService#readUniqueExceptionTypeNames}' passthrough of the raw
     * type-name strings (no per-element JSON serialisation), wrapping the result in the SAME {@link
     * MapOfStringListOfStringsValue} envelope the offloaded face used.
     *
     * @param typeNamesByStoreName the reactive accessor's result
     * @return the same {@code Map<String, List<String>>} envelope the MXBean produced
     */
    private static MapOfStringListOfStringsValue toTypeNamesEnvelope(
            final Map<String, Collection<String>> typeNamesByStoreName) {
        final Map<String, List<String>> typeNamesListByStoreName = new HashMap<>();
        if (typeNamesByStoreName != null) {
            for (final Map.Entry<String, Collection<String>> entry : typeNamesByStoreName.entrySet()) {
                typeNamesListByStoreName.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        return MapOfStringListOfStringsValue.from(typeNamesListByStoreName);
    }

    /**
     * Serialises {@code exceptionMessageDetails} as its {@link RedactedExceptionMessageDetails} view,
     * with the SAME writer {@code JmxExceptionService#jsonOf} uses -- {@code
     * JsonUtil.getJsonMapper().writerFor(RedactedExceptionMessageDetails.class)} (Lens 4 / L4-04,
     * CWE-200/209) -- so the JSON this route produces is byte-identical to what the offloaded (and
     * servlet) faces produce, and none of the three omit free-text {@code throwableMessage}/{@code
     * stackTrace}/{@code context} content to the HTTP caller.
     *
     * @param exceptionMessageDetails the details to serialise
     * @return the serialised details
     * @throws UncheckedIOException wrapping a {@link JsonProcessingException}, if serialisation fails --
     *     an unmapped failure, the reactive equivalent of {@code JmxExceptionService#jsonOf} wrapping the
     *     same failure in an {@code MBeanException}
     */
    private static String writeExceptionMessageDetailsJson(final ExceptionMessageDetails exceptionMessageDetails) {
        try {
            return JsonUtil.getJsonMapper()
                    .writerFor(RedactedExceptionMessageDetails.class)
                    .writeValueAsString(RedactedExceptionMessageDetails.from(exceptionMessageDetails));
        } catch (final JsonProcessingException jsonProcessingException) {
            throw new UncheckedIOException(jsonProcessingException);
        }
    }
}
