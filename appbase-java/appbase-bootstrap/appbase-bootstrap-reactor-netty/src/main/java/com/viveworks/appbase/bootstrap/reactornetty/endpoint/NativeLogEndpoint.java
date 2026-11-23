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
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.accessor.LogReadPageSize;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
import com.viveworks.appbase.core.service.log.jmx.JmxLogServiceMXBean;
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
import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.requireBoundedInt;
import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.requireParam;

/**
 * The native, non-blocking {@code /log} route: R3 landed a reactive {@link LogAccessor}, so the
 * two reads that used to be offloaded ({@code logRecordsBetween}, {@code logRecordsAt}) now run
 * directly on the event loop via {@code readLogRecordsBetweenReactive}/{@code
 * readLogRecordsAtReactive} -- no {@code Mono.fromCallable(...).subscribeOn(...)}, no bounded
 * worker held for the duration of a Lucene search.
 *
 * <p>Every operation the offloaded {@code /log} arm served is reproduced here with the SAME
 * response bytes as that arm. The {@code status} value is JSON-quoted rather than
 * written as raw text (see below). Page sizes share the {@code 1..1000} service cap with the
 * servlet and JAX-RS faces; a lower native-route configured cap still applies.
 * For the reads,
 * {@code logRecordsBetween} and {@code logRecordsAt} go through the reactive accessor and are
 * serialised with the SAME {@code JsonUtil}-backed writer {@code JmxLogService#jsonOf} uses;
 * {@code logStoreFactoryByConfigurationTypeMap}, {@code logStores} and {@code status} are
 * in-memory already and stay on {@link JmxLogServiceMXBean} unchanged. A missing or unsupported
 * {@code operation}, or a missing required parameter, is mapped to 400 via the SAME {@link
 * DispatchException} and helpers {@link OffloadedEndpoints} uses ({@link EndpointReads}). A
 * non-numeric numeric parameter throws {@link NumberFormatException} unmapped, the same faithful
 * reproduction the offloaded endpoints give the other numeric parameters -- it surfaces as an
 * unmapped error, mapped to 500 further up the chain.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class NativeLogEndpoint {

    private NativeLogEndpoint() {
    }

    /**
     * Registers the native {@code /log} route on {@code registry}.
     *
     * @param registry route table to populate
     * @param appbaseCore Appbase core, providing the reactive {@link LogAccessor} and the JMX-backed
     *     {@link JmxLogServiceMXBean} for the in-memory operations
     * @param maxPageSize the inclusive upper bound applied to every {@code pageSize} parameter
     */
    public static void register(
            final AppbaseRouteRegistry registry, final AppbaseCore appbaseCore, final int maxPageSize) {
        AppbaseUtil.notNull(registry, "registry");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        registry.route("GET", AppbaseServiceConstants.DEFAULT_LOG_SERVLET_PATH,
                exchange -> Mono.defer(() -> {
                    try {
                        return dispatch(appbaseCore, exchange, maxPageSize);
                    } catch (final DispatchException dispatchException) {
                        exchange.status(dispatchException.status());
                        return exchange.writeJson(
                                errorBody(dispatchException.status(), dispatchException.getMessage()));
                    }
                }));
    }

    /**
     * Reproduces {@code LogServlet#doGet}'s read dispatch, with {@code logRecordsBetween} and
     * {@code logRecordsAt} running through the reactive {@link LogAccessor} instead of the
     * blocking {@link JmxLogServiceMXBean}.
     *
     * @param appbaseCore Appbase core
     * @param exchange the request/response exchange
     * @param maxPageSize the page-size cap applied to {@code pageSize}
     * @return completion signal; the response has already been written
     * @throws DispatchException a 400, for a missing/unsupported operation or missing/out-of-range
     *     parameter -- caught by {@link #register}, never left to escape
     */
    private static Mono<Void> dispatch(
            final AppbaseCore appbaseCore, final AppbaseServiceExchange exchange, final int maxPageSize) {
        final String operation = requireParam(exchange, AppbaseServiceConstants.OPERATION_PARAMETER).trim();

        if ("logRecordsBetween".equalsIgnoreCase(operation)) {
            final long inclusiveStartTimestamp = Long.parseLong(requireParam(exchange, "inclusiveStartTimestamp"));
            final long exclusiveEndTimestamp = Long.parseLong(requireParam(exchange, "exclusiveEndTimestamp"));
            final int pageSize = requireBoundedInt(exchange, "pageSize",
                    Math.min(maxPageSize, LogReadPageSize.MAX_PAGE_SIZE));
            return appbaseCore.getLogService().getLogAccessor()
                    .readLogRecordsBetweenReactive(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize)
                    .map(NativeLogEndpoint::toEnvelope)
                    .flatMap(exchange::writeJson);
        }
        if ("logRecordsAt".equalsIgnoreCase(operation)) {
            final long timestamp = Long.parseLong(requireParam(exchange, "timestamp"));
            final int pageSize = requireBoundedInt(exchange, "pageSize",
                    Math.min(maxPageSize, LogReadPageSize.MAX_PAGE_SIZE));
            return appbaseCore.getLogService().getLogAccessor()
                    .readLogRecordsAtReactive(timestamp, pageSize)
                    .map(NativeLogEndpoint::toEnvelope)
                    .flatMap(exchange::writeJson);
        }

        final JmxLogServiceMXBean jmxLogServiceMXBean = appbaseCore.getJmxService().getMXBean(JmxLogService.class);
        if ("logStoreFactoryByConfigurationTypeMap".equalsIgnoreCase(operation)) {
            return exchange.writeJson(
                    new MapOfStringStringValue(jmxLogServiceMXBean.getLogStoreFactoryByConfigurationTypeMap()));
        }
        if ("logStores".equalsIgnoreCase(operation)) {
            return exchange.writeJson(new ListOfStringsValue(jmxLogServiceMXBean.getLogStores()));
        }
        if ("status".equalsIgnoreCase(operation)) {
            // LogServlet#getStatus writes status.name() via writeOk -- raw text, no JSON envelope.
            // AppbaseServiceExchange exposes only writeJson: there is no raw-text write primitive
            // on the reactive face, so the same ServiceStatus name is carried as a JSON string
            // instead of raw text. Same value, JSON-quoted rather than unquoted.
            return exchange.writeJson(jmxLogServiceMXBean.getStatus().name());
        }

        throw new DispatchException(400, "Unsupported operation " + operation);
    }

    /**
     * Reproduces {@code JmxLogService#jsonOf}'s per-record serialisation, applied to a reactively
     * read {@code Map<String, Collection<LogRecord>>}, and wraps the result in the SAME envelope
     * type the offloaded (and, before it, the servlet) face used.
     *
     * @param logRecordsByStoreName the reactive accessor's result
     * @return the same {@code Map<String, List<String>>}-of-JSON envelope {@code
     *     JmxLogServiceMXBean#readLogRecordsBetween}/{@code readLogRecordsAt} produced
     */
    private static MapOfStringListOfStringsValue toEnvelope(
            final Map<String, Collection<LogRecord>> logRecordsByStoreName) {
        final Map<String, List<String>> jsonByStoreName = new HashMap<>();
        if (logRecordsByStoreName != null) {
            for (final Map.Entry<String, Collection<LogRecord>> entry : logRecordsByStoreName.entrySet()) {
                final List<String> jsonList = new ArrayList<>();
                for (final LogRecord logRecord : entry.getValue()) {
                    jsonList.add(writeLogRecordJson(logRecord));
                }
                jsonByStoreName.put(entry.getKey(), jsonList);
            }
        }

        return MapOfStringListOfStringsValue.from(jsonByStoreName);
    }

    /**
     * Serialises {@code logRecord} with the SAME writer {@code JmxLogService#jsonOf} uses --
     * {@code JsonUtil.getJsonMapper().writerFor(LogRecord.class)} -- so the JSON this route
     * produces is byte-identical to what the offloaded (and servlet) faces produced.
     *
     * @param logRecord the record to serialise
     * @return the serialised record
     * @throws UncheckedIOException wrapping a {@link JsonProcessingException}, if serialisation
     *     fails -- an unmapped failure, the reactive equivalent of {@code JmxLogService#jsonOf}
     *     wrapping the same failure in an {@code MBeanException}
     */
    private static String writeLogRecordJson(final LogRecord logRecord) {
        try {
            return JsonUtil.getJsonMapper().writerFor(LogRecord.class).writeValueAsString(logRecord);
        } catch (final JsonProcessingException jsonProcessingException) {
            throw new UncheckedIOException(jsonProcessingException);
        }
    }
}
