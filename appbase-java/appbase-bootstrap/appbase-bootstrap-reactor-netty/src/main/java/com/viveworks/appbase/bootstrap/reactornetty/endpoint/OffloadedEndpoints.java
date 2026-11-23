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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.DispatchException;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.jmx.JmxAppbaseCore;
import com.viveworks.appbase.core.jmx.JmxAppbaseCoreMXBean;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationServiceMXBean;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MxBeanEntryList;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorServiceMXBean;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.appbase.data.Value;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.management.MBeanException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.errorBody;
import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.requireParam;

/**
 * The four Tier-1 offloaded read endpoints: {@code /configuration}, {@code /core}, {@code /jmx} and
 * {@code /mediator}, each wrapped in {@code Mono.fromCallable(...).subscribeOn(offloadScheduler)} -- the
 * event loop is released immediately, and the call runs on a bounded worker instead. None of these reads
 * actually blocks on the read path: {@code /core}, {@code /jmx} and {@code /mediator} read in-memory state
 * but co-host slow admin verbs on the same route, and {@code /configuration} reads an in-memory,
 * eventually-consistent snapshot (see the {@code /configuration} registration below). The offload is a
 * conservative fallback that keeps any residual latency off the event loop; it is RETAINED for all four
 * (R3 Option A) rather than removed.
 *
 * <p><b>{@code /log} and {@code /exception} are no longer here.</b> R3 landed a reactive {@code
 * LogAccessor} and a reactive {@code ExceptionAccessor}, so {@code /log} is served natively by
 * {@link NativeLogEndpoint} and {@code /exception} by {@link NativeExceptionEndpoint} -- no offload,
 * no scheduler. Those classes share their 400 mapping and bounded-parameter validation with this one
 * via {@link EndpointReads}.
 *
 * <p><strong>Tier-1 is an explicitly-labelled fallback, never sold as non-blocking.</strong> The
 * umbrella spec's non-negotiable principle: wrapping a blocking call in {@code
 * subscribeOn(boundedElastic)} yields a reactive-LOOKING API that still burns one thread per
 * in-flight call. It buys exactly one thing here -- the event loop is never stalled -- and nothing
 * more. Each route's registration comment names what blocks and the milestone that removes the
 * offload.
 *
 * <p><b>Read-only scope (deliberate, mirrors {@link NativeEndpoints}).</b> {@code ExceptionServlet}
 * and {@code ConfigurationServlet} each multiplex mutating/administrative operations onto the SAME
 * path via {@code doPost}/{@code doPut}/{@code doDelete} (reload, shutdown, clear*,
 * register/unregister*, property and typed-configuration writes). None of those are reproduced
 * here: they are destructive or state-changing, not reads, and stay servlet-only until they get a
 * proper reactive treatment (tracked as P-039, targeted 2.0.0). This class registers ONLY the
 * {@code doGet} read half of each path.
 *
 * <p>Each read dispatches on the SAME {@code operation} query parameter its servlet uses, calls the
 * SAME {@code JmxServiceMXBean} accessor, and wraps the result in the SAME response type -- so the
 * reactive face returns identical JSON to the servlet face for every reproduced operation. A missing
 * or unsupported {@code operation}, or a missing required parameter, is mapped to 400, mirroring the
 * servlets' {@code sendInvalidParameter}/{@code sendBadRequest}. An unexpected failure (e.g. an
 * {@code MBeanException} from the accessor) is left to propagate out of {@code Mono.fromCallable}
 * untouched, to be mapped to 500 by {@code ExceptionHandlerWebFilter} further up the chain -- the
 * same outcome as the servlets' catch-all {@code sendInternalServerError}.
 *
 * <p><b>{@code PermissionDeniedException -> 403} IS reproduced -- and on {@code /configuration}
 * it is more faithful than the servlet.</b> A {@code PermissionDeniedException} raised by an
 * accessor is not a {@link DispatchException}, so it propagates out of {@code Mono.fromCallable}
 * untouched (the JMX permission check lives inside the MXBean, below both faces) and {@code
 * ExceptionHandlerWebFilter} maps it to 403 -- the same 403 {@code ExceptionServlet#doGet}
 * produces via its explicit {@code catch (PermissionDeniedException)}. {@code
 * ConfigurationServlet#doGet}, however, has NO such catch: its only handler is {@code
 * catch (Exception) -> sendInternalServerError}, so a denied {@code /configuration} read returns
 * 500 on the servlet face but 403 on this reactive face. That is a deliberate, documented
 * divergence (403 is the correct status for a permission denial); it is the one behavioural
 * difference from the servlets across both endpoints.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class OffloadedEndpoints {

    private OffloadedEndpoints() {
    }

    /**
     * Registers all four offloaded read routes -- {@code /configuration}, {@code /core}, {@code /jmx}
     * and {@code /mediator} -- on {@code registry}.
     *
     * @param registry route table to populate
     * @param appbaseCore Appbase core, providing the JMX-backed services these endpoints read
     * @param offloadScheduler bounded scheduler on which blocking work runs
     * @param policy the page-size cap and per-read timeout applied to every offloaded read
     */
    public static void register(
            final AppbaseRouteRegistry registry,
            final AppbaseCore appbaseCore,
            final Scheduler offloadScheduler,
            final OffloadPolicy policy) {
        AppbaseUtil.notNull(registry, "registry");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(offloadScheduler, "offloadScheduler");
        AppbaseUtil.notNull(policy, "policy");

        /*
         * Tier-1 offload, RETAINED (R3 Option A). ConfigurationServlet's reads are backed by
         * JmxConfigurationServiceMXBean, but they do NOT block on disk/JDBC on the read path: the
         * blocking ConfigurationAccessor.readConfigurations() store scan runs in a background loader
         * (DefaultConfigurationService, scheduled via mediator scheduleWithFixedDelay) OFF the event
         * loop, caching an eventually-consistent ConfigurationsJson (configurationsJsonRef). The
         * endpoint's configurationsAsJson returns that cache; the other operations read already-loaded
         * in-memory maps. R3 added a reactive mirror of that background scan
         * (ConfigurationAccessor.readConfigurationsReactive -> SchedulerOffloadReactiveConfigurationStore
         * over the N01 ReactiveReadBoundary), but since the endpoint reads the cache rather than scanning
         * here, this arm is NOT rewired and stays offloaded -- the epic's "Done when" takes the Lucene
         * reads native (NativeLogEndpoint/NativeExceptionEndpoint) and keeps the non-Lucene config offload.
         */
        registry.route("GET", AppbaseServiceConstants.DEFAULT_CONFIGURATION_SERVLET_PATH,
                offloaded(offloadScheduler, appbaseCore, OffloadedEndpoints::dispatchConfiguration, policy));

        /*
         * Tier-1 offload (permanent): CoreServlet, JmxServlet and MediatorServlet co-host slow
         * administrative operations (reload/shutdown, which start and stop live services) on the
         * SAME path as their in-memory reads. The reads themselves do not block, but rather than
         * split one route across two execution models, the whole route is offloaded. Unlike the
         * two above, this offload is NOT removed by a later reactive-I/O milestone -- the admin
         * verbs are inherently slow -- but only the read (doGet) half is reproduced here; the
         * mutating verbs stay servlet-only (see the read-only scope note above).
         */
        registry.route("GET", AppbaseServiceConstants.DEFAULT_CORE_SERVLET_PATH,
                offloaded(offloadScheduler, appbaseCore, OffloadedEndpoints::dispatchCore, policy));
        registry.route("GET", AppbaseServiceConstants.DEFAULT_JMX_SERVLET_PATH,
                offloaded(offloadScheduler, appbaseCore, OffloadedEndpoints::dispatchJmx, policy));
        registry.route("GET", AppbaseServiceConstants.DEFAULT_MEDIATOR_SERVLET_PATH,
                offloaded(offloadScheduler, appbaseCore, OffloadedEndpoints::dispatchMediator, policy));
    }

    /**
     * Reproduces {@code ConfigurationServlet#doGet}'s read dispatch against
     * {@link JmxConfigurationServiceMXBean}.
     *
     * @param appbaseCore Appbase core
     * @param exchange the request/response exchange
     * @return the response body to serialise
     * @throws Exception any failure from the underlying MXBean call
     */
    private static Object dispatchConfiguration(final AppbaseCore appbaseCore, final AppbaseServiceExchange exchange)
            throws Exception {
        final String operation = requireParam(exchange, AppbaseServiceConstants.OPERATION_PARAMETER).trim();
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                appbaseCore.getJmxService().getMXBean(JmxConfigurationService.class);

        if ("property".equalsIgnoreCase(operation)) {
            final String propertyKey = requireParam(exchange, "propertyKey");
            final String value = jmxConfigurationServiceMXBean.getString(propertyKey);
            if (value == null) {
                // Mirrors ConfigurationServlet#getProperty's sendNotFound("getProperty", ...).
                throw new DispatchException(404, "Not Found");
            }
            return new ConfigurationValue<>(value);
        }
        if ("typedConfiguration".equalsIgnoreCase(operation)) {
            final String typeName = requireParam(exchange, "typeName");
            final String typedConfigurationYaml = jmxConfigurationServiceMXBean.getTypedConfigurationYaml(typeName);
            if (typedConfigurationYaml == null) {
                // Mirrors sendNotFound("getTypedConfigurationYaml", ...).
                throw new DispatchException(404, "Not Found");
            }
            // ConfigurationServlet#getTypedConfigurationYaml writes raw YAML text via
            // writeOkText. AppbaseServiceExchange (Task 1) exposes only writeJson -- there is no
            // raw-text write primitive on the reactive face -- so the same YAML string is carried
            // as a JSON string value instead of raw text. Same content, JSON-quoted rather than
            // unquoted.
            return typedConfigurationYaml;
        }
        if ("nonValidatingTypes".equalsIgnoreCase(operation)) {
            return new SetOfStringsValue(jmxConfigurationServiceMXBean.getNonValidatingTypeNames());
        }
        if ("securePropertyKeys".equalsIgnoreCase(operation)) {
            return new SetOfStringsValue(jmxConfigurationServiceMXBean.getSecurePropertyKeys());
        }
        if ("secureTypes".equalsIgnoreCase(operation)) {
            return new SetOfStringsValue(jmxConfigurationServiceMXBean.getSecureTypeNames());
        }
        if ("configurationsAsJson".equalsIgnoreCase(operation)) {
            final ConfigurationsJson configurationsJson = jmxConfigurationServiceMXBean.getConfigurationsJson();
            if (configurationsJson != null) {
                return configurationsJson;
            }
            // ConfigurationServlet returns 200 with a zero-length body when getConfigurationsJson()
            // is null. A Mono cannot legally carry a null onNext -- Mono.fromCallable returning
            // null completes EMPTY, so the downstream flatMap(exchange::writeJson) would never
            // run and no response would be written at all. An empty JSON object is the closest
            // legal reproduction of "nothing" the exchange's writeJson-only contract (Task 1)
            // allows.
            return Map.of();
        }
        if ("status".equalsIgnoreCase(operation)) {
            // Same raw-text-vs-JSON-string caveat as LogServlet#getStatus.
            return jmxConfigurationServiceMXBean.getStatus().name();
        }

        throw new DispatchException(400, "Unsupported operation " + operation);
    }

    /**
     * Reproduces {@code CoreServlet#doGet}'s read dispatch against {@link JmxAppbaseCoreMXBean}.
     *
     * @param appbaseCore Appbase core
     * @param exchange the request/response exchange
     * @return the response body to serialise
     * @throws Exception any failure from the underlying MXBean call
     */
    private static Object dispatchCore(final AppbaseCore appbaseCore, final AppbaseServiceExchange exchange)
            throws Exception {
        final String operation = requireParam(exchange, AppbaseServiceConstants.OPERATION_PARAMETER).trim();
        final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean =
                appbaseCore.getJmxService().getMXBean(JmxAppbaseCore.class);

        if ("status".equalsIgnoreCase(operation)) {
            // Same raw-text-vs-JSON-string caveat as LogServlet#getStatus.
            return jmxAppbaseCoreMXBean.getStatus().name();
        }

        throw new DispatchException(400, "Unsupported operation " + operation);
    }

    /**
     * Reproduces {@code JmxServlet#doGet}'s read dispatch against {@link JmxJmxServiceMXBean}.
     *
     * @param appbaseCore Appbase core
     * @param exchange the request/response exchange
     * @return the response body to serialise
     * @throws Exception any failure from the underlying MXBean call
     */
    private static Object dispatchJmx(final AppbaseCore appbaseCore, final AppbaseServiceExchange exchange)
            throws Exception {
        final String operation = requireParam(exchange, AppbaseServiceConstants.OPERATION_PARAMETER).trim();
        final JmxJmxServiceMXBean jmxJmxServiceMXBean =
                appbaseCore.getJmxService().getMXBean(JmxJmxService.class);

        if ("status".equalsIgnoreCase(operation)) {
            // Same raw-text-vs-JSON-string caveat as LogServlet#getStatus.
            return jmxJmxServiceMXBean.getStatus().name();
        }
        if ("mxBeanEntries".equalsIgnoreCase(operation)) {
            final List<String> mxBeanEntriesJson = jmxJmxServiceMXBean.getMXBeanEntriesJson();
            if (mxBeanEntriesJson == null) {
                // JmxServlet#mxBeanEntries writes a zero-length body when the accessor returns null
                // (no else branch). A Mono cannot carry a null onNext, so -- as with
                // ConfigurationServlet#configurationsAsJson -- the closest legal reproduction is an
                // empty JSON object.
                return Map.of();
            }
            // JmxServlet deserialises each entry JSON into an MXBeanEntry, wraps them in an
            // MxBeanEntryList and serialises THAT with JsonUtil's mapper. Reproduce that, but render
            // the result to a JsonNode with the SAME JsonUtil mapper and return the tree. The
            // exchange serialises a JsonNode literally, so the response bytes are fixed by JsonUtil's
            // configuration -- its registered modules and MXBean subtype registrations, which
            // MXBeanEntry's nested types depend on -- rather than by the exchange's injected mapper,
            // which is not guaranteed to be JsonUtil's. Returning a raw POJO would make the JSON
            // depend on (and could fail under) a differently-configured exchange mapper. (Guaranteeing
            // the wired exchange mapper is JsonUtil's is tracked as a Task 7 wiring assertion.)
            final ObjectMapper jsonMapper = JsonUtil.getJsonMapper();
            final List<MXBeanEntry> mxBeanEntries = new ArrayList<>();
            for (final String json : mxBeanEntriesJson) {
                mxBeanEntries.add(jsonMapper.readerFor(MXBeanEntry.class).readValue(json));
            }
            return jsonMapper.valueToTree(new MxBeanEntryList(mxBeanEntries));
        }
        if ("mxBeanEntry".equalsIgnoreCase(operation)) {
            final String mxBeanTypeName = requireParam(exchange, "mxBeanTypeName");
            final String mxBeanEntryJson;
            try {
                mxBeanEntryJson = jmxJmxServiceMXBean.getMXBeanEntryJson(mxBeanTypeName);
            } catch (final MBeanException mBeanException) {
                // JmxServlet#mxBeanEntry maps an unknown MXBean type -- surfaced as an
                // MBeanException wrapping a ClassNotFoundException -- to 404 via sendNotFound;
                // anything else is a 500. Reproduce the same split.
                final Throwable target = mBeanException.getTargetException();
                if (target != null && target.getClass().isAssignableFrom(ClassNotFoundException.class)) {
                    throw new DispatchException(404, "Not Found");
                }
                throw mBeanException;
            }
            if (mxBeanEntryJson == null) {
                // ObjectMapper.readTree(String) returns null for null content, which would make
                // Mono.fromCallable complete EMPTY and skip writeJson entirely (no response at
                // all). Reproduce "nothing" as an empty JSON object, consistent with mxBeanEntries
                // and ConfigurationServlet#configurationsAsJson.
                return Map.of();
            }
            // Servlet writes the accessor's JSON raw. Parse it into a JsonNode with JsonUtil and
            // return the tree: the exchange emits a JsonNode literally, so the bytes equal the
            // accessor's JSON regardless of the exchange mapper (a raw String would be JSON-quoted;
            // a POJO would depend on the exchange mapper's config). See mxBeanEntries above.
            return JsonUtil.getJsonMapper().readTree(mxBeanEntryJson);
        }

        throw new DispatchException(400, "Unsupported operation " + operation);
    }

    /**
     * Reproduces {@code MediatorServlet#doGet}'s read dispatch against
     * {@link JmxMediatorServiceMXBean}.
     *
     * @param appbaseCore Appbase core
     * @param exchange the request/response exchange
     * @return the response body to serialise
     * @throws Exception any failure from the underlying MXBean call
     */
    private static Object dispatchMediator(final AppbaseCore appbaseCore, final AppbaseServiceExchange exchange)
            throws Exception {
        final String operation = requireParam(exchange, AppbaseServiceConstants.OPERATION_PARAMETER).trim();
        final JmxMediatorServiceMXBean jmxMediatorServiceMXBean =
                appbaseCore.getJmxService().getMXBean(JmxMediatorService.class);

        if ("status".equalsIgnoreCase(operation)) {
            // Same raw-text-vs-JSON-string caveat as LogServlet#getStatus.
            return jmxMediatorServiceMXBean.getStatus().name();
        }
        if ("isInitWithConfigurationFile".equalsIgnoreCase(operation)) {
            return new Value(jmxMediatorServiceMXBean.isInitWithConfigurationFile());
        }
        if ("isInitWithDefaultConfiguration".equalsIgnoreCase(operation)) {
            return new Value(jmxMediatorServiceMXBean.isInitWithDefaultConfiguration());
        }
        if ("configurationFilePath".equalsIgnoreCase(operation)) {
            return new Value(jmxMediatorServiceMXBean.getConfigurationFilePath());
        }

        throw new DispatchException(400, "Unsupported operation " + operation);
    }

    /**
     * Wrap a per-operation dispatch so it runs on {@code scheduler} rather than the event loop, and
     * so a {@link DispatchException} becomes a status + error-body write rather than an escaping
     * error.
     *
     * @param scheduler bounded scheduler for the blocking work
     * @param appbaseCore Appbase core, passed through to {@code dispatch}
     * @param dispatch the per-operation read dispatch for one endpoint
     * @param policy the per-read timeout applied to the offloaded work
     * @return a handler that never blocks the calling thread, and never runs longer than {@code
     *     policy}'s timeout
     */
    private static AppbaseServiceHandler offloaded(
            final Scheduler scheduler, final AppbaseCore appbaseCore, final Dispatch dispatch,
            final OffloadPolicy policy) {
        return exchange -> Mono.fromCallable(() -> {
                    try {
                        return dispatch.read(appbaseCore, exchange);
                    } catch (final DispatchException dispatchException) {
                        exchange.status(dispatchException.status());
                        return errorBody(dispatchException.status(), dispatchException.getMessage());
                    }
                })
                .subscribeOn(scheduler)
                // Bounds the client-visible response (prompt 503 via ExceptionHandlerWebFilter) and
                // frees the reactive pipeline; the blocked worker itself is reclaimed only when the
                // underlying read returns -- Reactor cancellation does not interrupt a synchronous
                // blocking call. Fully bounding worker-hold needs interruptible I/O (R3). The
                // resulting TimeoutException is intentionally left to propagate -- mapped to 503
                // further up the chain, the same place a DispatchException-free unexpected failure
                // is mapped to 500.
                .timeout(policy.timeout())
                .flatMap(exchange::writeJson);
    }

    /**
     * One per-endpoint read dispatch: reads {@code exchange}'s {@code operation} query parameter
     * and any operation-specific parameters, calls the matching {@code JmxServiceMXBean} accessor,
     * and returns the response body to serialise. Runs on the offload scheduler, never the event
     * loop.
     */
    @FunctionalInterface
    private interface Dispatch {
        Object read(AppbaseCore appbaseCore, AppbaseServiceExchange exchange) throws Exception;
    }
}
