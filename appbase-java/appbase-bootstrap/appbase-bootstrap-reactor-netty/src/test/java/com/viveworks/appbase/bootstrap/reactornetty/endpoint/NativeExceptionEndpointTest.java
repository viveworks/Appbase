/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
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

import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionService;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.RedactedExceptionMessageDetails;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.appbase.data.ListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the native, non-blocking {@code /exception} route ({@link NativeExceptionEndpoint}): the five
 * detail reads served through the reactive {@link ExceptionAccessor}, and the five in-memory operations
 * left on {@link JmxExceptionService}. Asserts byte-faithful reproduction of the offloaded {@code
 * /exception} arm, plus the shared 400 mapping and the faithful unmapped {@link NumberFormatException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NativeExceptionEndpointTest {

    private static final String STORE = "com.example.ExceptionStore";

    private static ExceptionMessageDetails sampleDetails(final int id) {
        final ExceptionMessageDetails details = new ExceptionMessageDetails();
        details.setId(id);
        details.setTimestamp(1000L);
        details.setThreadName("main");
        details.setStackTraceHash(123);
        details.setStackTrace("stackTrace");
        details.setSourceTypeName("com.example.Foo");
        details.setMessage("message");
        details.setDiscriminator("discriminator");
        details.setThrowableMessage("throwableMessage");
        details.setThrowableTypeName("java.lang.NullPointerException");
        return details;
    }

    /**
     * Computes the SAME redacted JSON the production code now writes (Lens 4 / L4-04): the
     * endpoint serialises {@link RedactedExceptionMessageDetails}, not {@link
     * ExceptionMessageDetails} itself, so the parity oracle below must match that, not the raw
     * entity's JSON.
     */
    private static String jsonOf(final ExceptionMessageDetails details) throws Exception {
        return JsonUtil.getJsonMapper()
                .writerFor(RedactedExceptionMessageDetails.class)
                .writeValueAsString(RedactedExceptionMessageDetails.from(details));
    }

    @Test
    void exceptionMessageDetailsReturns200AndTheEnvelope() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ExceptionMessageDetails details = sampleDetails(1);
        final Map<String, Collection<ExceptionMessageDetails>> byStore = new HashMap<>();
        byStore.put(STORE, List.of(details));
        Mockito.when(core.getExceptionService().getExceptionAccessor().readAllExceptionMessageDetailsReactive())
                .thenReturn(Mono.just(byStore));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionMessageDetails");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(
                MapOfStringListOfStringsValue.from(Map.of(STORE, List.of(jsonOf(details)))), exchange.body());
    }

    /**
     * Lens 4 / L4-04 (CWE-200, CWE-209): the pre-fix endpoint serialised {@link
     * ExceptionMessageDetails} unchanged, so a sentinel planted in {@code throwableMessage},
     * {@code stackTrace}, or {@code context} reached the HTTP response verbatim -- exactly the
     * finding's own reproduction. This pins that none of the three reach the response body.
     */
    @Test
    void exceptionMessageDetails_doesNotDiscloseThrowableMessageStackTraceOrContext() throws Exception {
        final String sentinelStackTrace = "at com.example.Secret.leak(/private/sentinel-stack)";
        final String sentinelThrowableMessage = "sentinel-throwable-message-/private/secret";
        final String sentinelContext = "sentinel-context-token-abc123";

        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ExceptionMessageDetails details = sampleDetails(1);
        details.setStackTrace(sentinelStackTrace);
        details.setThrowableMessage(sentinelThrowableMessage);
        details.setContext(sentinelContext);
        final Map<String, Collection<ExceptionMessageDetails>> byStore = new HashMap<>();
        byStore.put(STORE, List.of(details));
        Mockito.when(core.getExceptionService().getExceptionAccessor().readAllExceptionMessageDetailsReactive())
                .thenReturn(Mono.just(byStore));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionMessageDetails");
        registry.lookup("GET", "/exception").handle(exchange).block();

        final String body = String.valueOf(exchange.body());
        Assertions.assertEquals(200, exchange.status());
        Assertions.assertFalse(body.contains(sentinelStackTrace), "stack trace sentinel must not reach the response");
        Assertions.assertFalse(body.contains(sentinelThrowableMessage),
                "throwable-message sentinel must not reach the response");
        Assertions.assertFalse(body.contains(sentinelContext), "context sentinel must not reach the response");
    }

    @Test
    void exceptionMessageDetailsByIdReturns200AndTheStringEnvelope() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ExceptionMessageDetails details = sampleDetails(42);
        Mockito.when(core.getExceptionService().getExceptionAccessor().readExceptionMessageDetailsByIdReactive(42))
                .thenReturn(Mono.just(Map.of(STORE, details)));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionMessageDetailsById")
                .withQueryParam("id", "42");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new MapOfStringStringValue(Map.of(STORE, jsonOf(details))), exchange.body());
    }

    @Test
    void exceptionMessageDetailsByTimestampsReturns200AndTheEnvelope() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ExceptionMessageDetails details = sampleDetails(2);
        final Map<String, Collection<ExceptionMessageDetails>> byStore = new HashMap<>();
        byStore.put(STORE, List.of(details));
        // Exact-value matchers (not anyLong()) so a wrong-order or wrong-parameter dispatch mapping of
        // inclusiveMinTimestamp / exclusiveMaxTimestamp reddens this test.
        Mockito.when(core.getExceptionService().getExceptionAccessor()
                        .readAllExceptionMessageDetailsByTimestampsReactive(0L, 2000L))
                .thenReturn(Mono.just(byStore));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionMessageDetailsByTimestamps")
                .withQueryParam("inclusiveMinTimestamp", "0")
                .withQueryParam("exclusiveMaxTimestamp", "2000");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(
                MapOfStringListOfStringsValue.from(Map.of(STORE, List.of(jsonOf(details)))), exchange.body());
    }

    @Test
    void uniqueExceptionTypeNamesReturns200AndTheRawTypeNamesEnvelope() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final Map<String, Collection<String>> byStore = new HashMap<>();
        byStore.put(STORE, List.of("java.lang.NullPointerException"));
        Mockito.when(core.getExceptionService().getExceptionAccessor().readUniqueExceptionTypeNamesReactive())
                .thenReturn(Mono.just(byStore));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "uniqueExceptionTypeNames");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        // Raw type-name strings, NOT JSON-serialised -- reproduces JmxExceptionService's passthrough.
        Assertions.assertEquals(
                MapOfStringListOfStringsValue.from(Map.of(STORE, List.of("java.lang.NullPointerException"))),
                exchange.body());
    }

    @Test
    void exceptionMessageDetailsByThrowableTypeNameReturns200AndTheEnvelope() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ExceptionMessageDetails details = sampleDetails(3);
        final Map<String, Collection<ExceptionMessageDetails>> byStore = new HashMap<>();
        byStore.put(STORE, List.of(details));
        Mockito.when(core.getExceptionService().getExceptionAccessor()
                        .readExceptionMessageDetailsByThrowableTypeNameReactive("java.lang.NullPointerException"))
                .thenReturn(Mono.just(byStore));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionMessageDetailsByThrowableTypeName")
                .withQueryParam("throwableTypeName", "java.lang.NullPointerException");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(
                MapOfStringListOfStringsValue.from(Map.of(STORE, List.of(jsonOf(details)))), exchange.body());
    }

    @Test
    void exceptionStoresOperationReturnsTheRegisteredStoreTypeNames() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxExceptionService jmxExceptionService = Mockito.mock(JmxExceptionService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxExceptionService.class)).thenReturn(jmxExceptionService);
        Mockito.when(jmxExceptionService.getExceptionStores()).thenReturn(List.of("LuceneExceptionStore"));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionStores");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new ListOfStringsValue(List.of("LuceneExceptionStore")), exchange.body());
    }

    @Test
    void statusOperationReturnsTheServiceStatusName() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxExceptionService jmxExceptionService = Mockito.mock(JmxExceptionService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxExceptionService.class)).thenReturn(jmxExceptionService);
        Mockito.when(jmxExceptionService.getStatus()).thenReturn(ServiceStatus.STARTED);

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "status");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals("STARTED", exchange.body());
    }

    @Test
    void globalExceptionListenersOperationReturnsTheList() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxExceptionService jmxExceptionService = Mockito.mock(JmxExceptionService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxExceptionService.class)).thenReturn(jmxExceptionService);
        Mockito.when(jmxExceptionService.getGlobalExceptionListeners()).thenReturn(List.of("com.example.Listener"));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "globalExceptionListeners");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new ListOfStringsValue(List.of("com.example.Listener")), exchange.body());
    }

    @Test
    void exceptionStoreFactoryByConfigurationTypeNameMapOperationReturnsTheMap() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxExceptionService jmxExceptionService = Mockito.mock(JmxExceptionService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxExceptionService.class)).thenReturn(jmxExceptionService);
        Mockito.when(jmxExceptionService.getExceptionStoreFactoryByConfigurationTypeNameMap())
                .thenReturn(Map.of("com.example.StoreConfiguration", "com.example.StoreFactory"));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionStoreFactoryByConfigurationTypeNameMap");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(
                new MapOfStringStringValue(Map.of("com.example.StoreConfiguration", "com.example.StoreFactory")),
                exchange.body());
    }

    @Test
    void exceptionListenersByExceptionTypeMapOperationReturnsTheMap() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxExceptionService jmxExceptionService = Mockito.mock(JmxExceptionService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxExceptionService.class)).thenReturn(jmxExceptionService);
        Mockito.when(jmxExceptionService.getExceptionListenersByExceptionTypeMap())
                .thenReturn(Map.of("java.lang.NullPointerException", List.of("com.example.Listener")));

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionListenersByExceptionTypeMap");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(
                MapOfStringListOfStringsValue.from(
                        Map.of("java.lang.NullPointerException", List.of("com.example.Listener"))),
                exchange.body());
    }

    @Test
    void withoutAnOperationParameterReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void withAnUnsupportedOperationReturns400AndTheErrorBody() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        NativeExceptionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "notARealOperation");
        registry.lookup("GET", "/exception").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
        Assertions.assertEquals(
                Map.of("status", 400, "message", "Unsupported operation notARealOperation"), exchange.body());
    }

    @Test
    void aMalformedIdParameterEscapesUnmappedRatherThanBecomingA400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        NativeExceptionEndpoint.register(registry, core);

        // Faithful reproduction of the offloaded arm/servlet: a non-numeric id is NOT validated to a
        // 400. Integer.parseInt throws NumberFormatException, which is not a DispatchException, so it
        // propagates out untouched -- mapped to 500 further up the chain in production; the unit harness
        // has no such filter, so the raw exception surfaces from block().
        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionMessageDetailsById")
                .withQueryParam("id", "not-a-number");

        Assertions.assertThrows(NumberFormatException.class,
                () -> registry.lookup("GET", "/exception").handle(exchange).block());
    }

    @Test
    void aMalformedTimestampParameterEscapesUnmappedRatherThanBecomingA400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        NativeExceptionEndpoint.register(registry, core);

        // Same faithful reproduction as the id case, for the other numeric parameter the javadoc names:
        // Long.parseLong on a non-numeric timestamp throws NumberFormatException, which is not a
        // DispatchException, so it propagates unmapped (500 in production).
        final FakeExchange exchange = new FakeExchange("GET", "/exception")
                .withQueryParam("operation", "exceptionMessageDetailsByTimestamps")
                .withQueryParam("inclusiveMinTimestamp", "not-a-number")
                .withQueryParam("exclusiveMaxTimestamp", "2000");

        Assertions.assertThrows(NumberFormatException.class,
                () -> registry.lookup("GET", "/exception").handle(exchange).block());
    }
}
