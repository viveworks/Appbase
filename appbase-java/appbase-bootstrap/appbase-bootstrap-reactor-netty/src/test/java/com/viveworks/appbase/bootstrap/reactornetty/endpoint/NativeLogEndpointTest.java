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
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
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
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NativeLogEndpointTest {

    private static final int DEFAULT_MAX_PAGE_SIZE = 1000;

    private static final LogRecord SAMPLE_LOG_RECORD = LogRecord.builder()
            .timestamp(1000L)
            .threadName("main")
            .logLevel(LogLevel.INFO)
            .message("hello")
            .formattedMessage("hello")
            .loggerName("com.example.Foo")
            .build();

    @Test
    void nativeLogRecordsBetweenReturns200AndTheEnvelope() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final Map<String, Collection<LogRecord>> logRecordsByStoreName = new HashMap<>();
        logRecordsByStoreName.put("com.example.Store", List.of(SAMPLE_LOG_RECORD));
        Mockito.when(core.getLogService().getLogAccessor()
                        .readLogRecordsBetweenReactive(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(Mono.just(logRecordsByStoreName));

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        final FakeExchange exchange = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logRecordsBetween")
                .withQueryParam("inclusiveStartTimestamp", "0")
                .withQueryParam("exclusiveEndTimestamp", "2000")
                .withQueryParam("pageSize", "10");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        final String expectedJson =
                JsonUtil.getJsonMapper().writerFor(LogRecord.class).writeValueAsString(SAMPLE_LOG_RECORD);
        final Map<String, List<String>> expectedByStoreName = Map.of("com.example.Store", List.of(expectedJson));
        Assertions.assertEquals(MapOfStringListOfStringsValue.from(expectedByStoreName), exchange.body());
    }

    @Test
    void nativeLogRecordsAtReturns200AndTheEnvelope() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final Map<String, Collection<LogRecord>> logRecordsByStoreName = new HashMap<>();
        logRecordsByStoreName.put("com.example.Store", List.of(SAMPLE_LOG_RECORD));
        Mockito.when(core.getLogService().getLogAccessor()
                        .readLogRecordsAtReactive(Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(Mono.just(logRecordsByStoreName));

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        final FakeExchange exchange = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logRecordsAt")
                .withQueryParam("timestamp", "1000")
                .withQueryParam("pageSize", "10");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        final String expectedJson =
                JsonUtil.getJsonMapper().writerFor(LogRecord.class).writeValueAsString(SAMPLE_LOG_RECORD);
        final Map<String, List<String>> expectedByStoreName = Map.of("com.example.Store", List.of(expectedJson));
        Assertions.assertEquals(MapOfStringListOfStringsValue.from(expectedByStoreName), exchange.body());
    }

    @Test
    void nativeLogStoresOperationReturnsTheRegisteredLogStoreTypeNames() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxLogService jmxLogServiceMXBean = Mockito.mock(JmxLogService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxLogService.class)).thenReturn(jmxLogServiceMXBean);
        Mockito.when(jmxLogServiceMXBean.getLogStores()).thenReturn(List.of("LuceneLogStore"));

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        final FakeExchange exchange = new FakeExchange("GET", "/log").withQueryParam("operation", "logStores");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new ListOfStringsValue(List.of("LuceneLogStore")), exchange.body());
    }

    @Test
    void nativeLogStatusOperationReturnsTheServiceStatusName() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxLogService jmxLogServiceMXBean = Mockito.mock(JmxLogService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxLogService.class)).thenReturn(jmxLogServiceMXBean);
        Mockito.when(jmxLogServiceMXBean.getStatus()).thenReturn(ServiceStatus.STARTED);

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        final FakeExchange exchange = new FakeExchange("GET", "/log").withQueryParam("operation", "status");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals("STARTED", exchange.body());
    }

    @Test
    void nativeLogStoreFactoryByConfigurationTypeMapOperationReturnsTheMap() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxLogService jmxLogServiceMXBean = Mockito.mock(JmxLogService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxLogService.class)).thenReturn(jmxLogServiceMXBean);
        Mockito.when(jmxLogServiceMXBean.getLogStoreFactoryByConfigurationTypeMap())
                .thenReturn(Map.of("com.example.LogStoreConfiguration", "com.example.LogStoreFactory"));

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        final FakeExchange exchange = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logStoreFactoryByConfigurationTypeMap");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(
                new MapOfStringStringValue(Map.of("com.example.LogStoreConfiguration", "com.example.LogStoreFactory")),
                exchange.body());
    }

    @Test
    void nativeLogWithoutAnOperationParameterReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        final FakeExchange exchange = new FakeExchange("GET", "/log");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void nativeLogWithAnUnsupportedOperationReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        final FakeExchange exchange =
                new FakeExchange("GET", "/log").withQueryParam("operation", "notARealOperation");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
        Assertions.assertEquals(
                Map.of("status", 400, "message", "Unsupported operation notARealOperation"), exchange.body());
    }

    @Test
    void nativeLogRecordsAtWithAnOverCapPageSizeReturns400AndNeverCallsTheAccessor() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final LogAccessor logAccessor = core.getLogService().getLogAccessor();

        NativeLogEndpoint.register(registry, core, 5);

        final FakeExchange exchange = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logRecordsAt")
                .withQueryParam("timestamp", "1000")
                .withQueryParam("pageSize", "6");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
        Mockito.verify(logAccessor, Mockito.never())
                .readLogRecordsAtReactive(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    void nativeLogRecordsAtWithAZeroPageSizeReturns400AndNeverCallsTheAccessor() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final LogAccessor logAccessor = core.getLogService().getLogAccessor();

        NativeLogEndpoint.register(registry, core, 5);

        final FakeExchange exchange = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logRecordsAt")
                .withQueryParam("timestamp", "1000")
                .withQueryParam("pageSize", "0");
        registry.lookup("GET", "/log").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
        Mockito.verify(logAccessor, Mockito.never())
                .readLogRecordsAtReactive(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    void nativeLogReadUsesSharedMaximumEvenWhenConfiguredHigher() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final LogAccessor accessor = core.getLogService().getLogAccessor();
        NativeLogEndpoint.register(registry, core, 2000);

        final FakeExchange oversized = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logRecordsAt")
                .withQueryParam("timestamp", "1000")
                .withQueryParam("pageSize", "1001");
        registry.lookup("GET", "/log").handle(oversized).block();
        Assertions.assertEquals(400, oversized.status());
        Mockito.verify(accessor, Mockito.never())
                .readLogRecordsAtReactive(Mockito.anyLong(), Mockito.anyInt());

        Mockito.when(accessor.readLogRecordsAtReactive(1000L, 1000))
                .thenReturn(Mono.just(Map.of()));
        final FakeExchange maximum = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logRecordsAt")
                .withQueryParam("timestamp", "1000")
                .withQueryParam("pageSize", "1000");
        registry.lookup("GET", "/log").handle(maximum).block();
        Assertions.assertEquals(200, maximum.status());
        Mockito.verify(accessor).readLogRecordsAtReactive(1000L, 1000);
    }

    @Test
    void aMalformedNumericParameterEscapesUnmappedRatherThanBecomingA400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        NativeLogEndpoint.register(registry, core, DEFAULT_MAX_PAGE_SIZE);

        // Faithful reproduction of dispatchLog/the servlet: a non-numeric numeric parameter is NOT
        // validated to a 400. Long.parseLong throws NumberFormatException, which is not a
        // DispatchException, so it propagates out untouched -- mapped to 500 further up the chain
        // in production; the unit harness has no such filter, so the raw exception surfaces from
        // block().
        final FakeExchange exchange = new FakeExchange("GET", "/log")
                .withQueryParam("operation", "logRecordsAt")
                .withQueryParam("timestamp", "not-a-number")
                .withQueryParam("pageSize", "10");

        Assertions.assertThrows(NumberFormatException.class,
                () -> registry.lookup("GET", "/log").handle(exchange).block());
    }
}
