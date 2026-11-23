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

import com.fasterxml.jackson.databind.JsonNode;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.jmx.JmxAppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MxBeanEntryList;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.appbase.data.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.management.MBeanException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class OffloadedEndpointsTest {

    /** Permissive default: unchanged behaviour for tests that do not exercise the policy itself. */
    private static final OffloadPolicy DEFAULT_POLICY = OffloadPolicy.of(1000, Duration.ofSeconds(30));

    @Test
    void offloadedEndpointsRegisterOnlyTheFourRetainedRoutes() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        OffloadedEndpoints.register(registry, core, Schedulers.boundedElastic(), DEFAULT_POLICY);

        // R3 moved /log and /exception to native endpoints; the four retained routes stay offloaded.
        Assertions.assertNull(registry.lookup("GET", "/log"));
        Assertions.assertNull(registry.lookup("GET", "/exception"));
        Assertions.assertNotNull(registry.lookup("GET", "/configuration"));
        Assertions.assertNotNull(registry.lookup("GET", "/core"));
        Assertions.assertNotNull(registry.lookup("GET", "/jmx"));
        Assertions.assertNotNull(registry.lookup("GET", "/mediator"));
    }

    @Test
    void anOffloadedHandlerRunsItsBlockingWorkOnTheOffloadSchedulerNotTheCallingThread() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationService = Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationService);
        Mockito.when(jmxConfigurationService.getStatus()).thenReturn(ServiceStatus.STARTED);
        final Scheduler offload = Schedulers.newSingle("offload-probe");

        OffloadedEndpoints.register(registry, core, offload, DEFAULT_POLICY);

        final String callingThread = Thread.currentThread().getName();
        final AtomicReference<String> workThread = new AtomicReference<>();

        final FakeExchange exchange = new FakeExchange("GET", "/configuration") {
            @Override
            public Mono<Void> writeJson(final Object body) {
                workThread.set(Thread.currentThread().getName());
                return super.writeJson(body);
            }
        };
        exchange.withQueryParam("operation", "status");

        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertNotEquals(callingThread, workThread.get(),
                "blocking work must not run on the calling (event-loop) thread");
        Assertions.assertTrue(workThread.get().startsWith("offload-probe"),
                "blocking work must run on the configured offload scheduler, got: " + workThread.get());

        offload.dispose();
    }

    @Test
    void restrictedIndividualConfigurationReadsReturn404OnReactiveFace() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ConfigurationService configuration = Mockito.mock(ConfigurationService.class);
        Mockito.when(configuration.getBoolean(JmxConfigurationService.SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY))
                .thenReturn(false);
        Mockito.when(configuration.getSecurePropertyKeys()).thenReturn(Set.of("secret.key"));
        Mockito.when(configuration.getString("secret.key")).thenReturn("credential-sentinel-2751");
        Mockito.when(configuration.getSecureTypeNames())
                .thenReturn(Set.of(DefaultMediatorServiceConfiguration.class.getName()));
        Mockito.when(configuration.getTypedConfiguration(DefaultMediatorServiceConfiguration.class))
                .thenReturn(DefaultMediatorServiceConfiguration.builder()
                        .messageObjectPoolCapacity(2751).build());
        final JmxConfigurationService bean = new JmxConfigurationService(
                Mockito.mock(LogService.class), configuration,
                DefaultConfigurationServiceConfiguration.class, configuration);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class)).thenReturn(bean);
        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange property = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "property")
                .withQueryParam("propertyKey", "secret.key");
        registry.lookup("GET", "/configuration").handle(property).block();

        final FakeExchange typed = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "typedConfiguration")
                .withQueryParam("typeName", DefaultMediatorServiceConfiguration.class.getName());
        registry.lookup("GET", "/configuration").handle(typed).block();

        if (Boolean.getBoolean("appbase.trace2751")) {
            System.out.printf("TRACE-2751 reactor-netty property status=%d body=%s%n",
                    property.status(), JsonUtil.getJsonMapper().writeValueAsString(property.body()));
            System.out.printf("TRACE-2751 reactor-netty typedConfiguration status=%d body=%s%n",
                    typed.status(), JsonUtil.getJsonMapper().writeValueAsString(typed.body()));
        }

        Assertions.assertEquals(404, property.status());
        Assertions.assertFalse(String.valueOf(property.body()).contains("credential-sentinel-2751"));
        Assertions.assertEquals(404, typed.status());
        Assertions.assertFalse(String.valueOf(typed.body()).contains("credential-sentinel-2751"));
    }

    @Test
    void configurationPropertyOperationReturnsTheValueWhenFound() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationServiceMXBean =
                Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationServiceMXBean);
        Mockito.when(jmxConfigurationServiceMXBean.getString("some.key")).thenReturn("some.value");

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "property")
                .withQueryParam("propertyKey", "some.key");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new ConfigurationValue<>("some.value"), exchange.body());
    }

    @Test
    void configurationPropertyOperationReturns404WhenNotFound() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationServiceMXBean =
                Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationServiceMXBean);
        Mockito.when(jmxConfigurationServiceMXBean.getString("missing.key")).thenReturn(null);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "property")
                .withQueryParam("propertyKey", "missing.key");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(404, exchange.status());
    }

    @Test
    void configurationWithAnUnsupportedOperationReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange =
                new FakeExchange("GET", "/configuration").withQueryParam("operation", "notARealOperation");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void configurationNonValidatingTypesOperationReturnsTheTypeNameSet() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationServiceMXBean = Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationServiceMXBean);
        final Set<String> typeNames = Set.of("com.example.SomeConfiguration");
        Mockito.when(jmxConfigurationServiceMXBean.getNonValidatingTypeNames()).thenReturn(typeNames);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "nonValidatingTypes");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new SetOfStringsValue(typeNames), exchange.body());
    }

    @Test
    void configurationTypedConfigurationOperationReturnsTheYamlAsAJsonStringWhenFound() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationServiceMXBean = Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationServiceMXBean);
        Mockito.when(jmxConfigurationServiceMXBean.getTypedConfigurationYaml("com.example.Type"))
                .thenReturn("key: value");

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "typedConfiguration")
                .withQueryParam("typeName", "com.example.Type");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        // The reactive face carries the servlet's raw-YAML body as a JSON string (no raw-text
        // write primitive on AppbaseServiceExchange) -- same content, JSON-quoted.
        Assertions.assertEquals("key: value", exchange.body());
    }

    @Test
    void configurationTypedConfigurationOperationReturns404WhenNotFound() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationServiceMXBean = Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationServiceMXBean);
        Mockito.when(jmxConfigurationServiceMXBean.getTypedConfigurationYaml("missing.Type")).thenReturn(null);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "typedConfiguration")
                .withQueryParam("typeName", "missing.Type");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(404, exchange.status());
    }

    @Test
    void configurationConfigurationsAsJsonOperationReturnsTheJsonWhenPresent() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationServiceMXBean = Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationServiceMXBean);
        final ConfigurationsJson configurationsJson = Mockito.mock(ConfigurationsJson.class);
        Mockito.when(jmxConfigurationServiceMXBean.getConfigurationsJson()).thenReturn(configurationsJson);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "configurationsAsJson");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertSame(configurationsJson, exchange.body());
    }

    @Test
    void configurationConfigurationsAsJsonOperationReturnsAnEmptyObjectWhenNull() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationServiceMXBean = Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationServiceMXBean);
        Mockito.when(jmxConfigurationServiceMXBean.getConfigurationsJson()).thenReturn(null);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "configurationsAsJson");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        // A Mono cannot carry a null onNext, so the null accessor result is reproduced as an empty
        // JSON object -- writeJson MUST still run (body non-null), not be skipped by an empty Mono.
        Assertions.assertEquals(Map.of(), exchange.body());
    }

    @Test
    void configurationPropertyWithoutAPropertyKeyReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        // operation is valid but the operation-specific required parameter is absent: a distinct
        // 400 path (requireParam on a secondary parameter) from the missing-operation case.
        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "property");
        registry.lookup("GET", "/configuration").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void coreStatusOperationReturnsTheServiceStatusName() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxAppbaseCore jmxAppbaseCoreMXBean = Mockito.mock(JmxAppbaseCore.class);
        Mockito.when(core.getJmxService().getMXBean(JmxAppbaseCore.class)).thenReturn(jmxAppbaseCoreMXBean);
        Mockito.when(jmxAppbaseCoreMXBean.getStatus()).thenReturn(ServiceStatus.STARTED);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/core").withQueryParam("operation", "status");
        registry.lookup("GET", "/core").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals("STARTED", exchange.body());
    }

    @Test
    void coreWithAnUnsupportedOperationReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/core").withQueryParam("operation", "notARealOperation");
        registry.lookup("GET", "/core").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void jmxStatusOperationReturnsTheServiceStatusName() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxJmxService jmxJmxServiceMXBean = Mockito.mock(JmxJmxService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxJmxService.class)).thenReturn(jmxJmxServiceMXBean);
        Mockito.when(jmxJmxServiceMXBean.getStatus()).thenReturn(ServiceStatus.STARTED);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx").withQueryParam("operation", "status");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals("STARTED", exchange.body());
    }

    @Test
    void jmxMxBeanEntriesOperationReturnsTheDeserialisedEntryList() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxJmxService jmxJmxServiceMXBean = Mockito.mock(JmxJmxService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxJmxService.class)).thenReturn(jmxJmxServiceMXBean);
        // Feed the accessor a real MXBeanEntry serialised with the SAME mapper the dispatch uses,
        // so the deserialise → MxBeanEntryList round-trip is exact rather than hand-rolled JSON.
        final MXBeanEntry entry = MXBeanEntry.builder().className("com.example.Foo").description("desc").build();
        final String entryJson = JsonUtil.getJsonMapper().writeValueAsString(entry);
        Mockito.when(jmxJmxServiceMXBean.getMXBeanEntriesJson()).thenReturn(List.of(entryJson));

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx").withQueryParam("operation", "mxBeanEntries");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        // The dispatch returns the JsonNode JsonUtil renders for the MxBeanEntryList. Assert
        // byte-for-byte fidelity to the servlet's serialisation by comparing the trees (the servlet
        // does writerFor(MxBeanEntryList.class).writeValueAsString on the same object).
        final JsonNode expected = JsonUtil.getJsonMapper().valueToTree(new MxBeanEntryList(List.of(entry)));
        Assertions.assertEquals(expected, exchange.body());
    }

    @Test
    void jmxMxBeanEntriesOperationReturnsAnEmptyObjectWhenNull() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxJmxService jmxJmxServiceMXBean = Mockito.mock(JmxJmxService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxJmxService.class)).thenReturn(jmxJmxServiceMXBean);
        Mockito.when(jmxJmxServiceMXBean.getMXBeanEntriesJson()).thenReturn(null);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx").withQueryParam("operation", "mxBeanEntries");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        // Servlet writes a zero-length body when null; an empty Mono would skip writeJson entirely,
        // so the faithful reproduction is an empty JSON object (writeJson still runs).
        Assertions.assertEquals(Map.of(), exchange.body());
    }

    @Test
    void jmxMxBeanEntryOperationReturnsTheDeserialisedEntryWhenFound() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxJmxService jmxJmxServiceMXBean = Mockito.mock(JmxJmxService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxJmxService.class)).thenReturn(jmxJmxServiceMXBean);
        final MXBeanEntry entry = MXBeanEntry.builder().className("com.example.Foo").description("desc").build();
        final String entryJson = JsonUtil.getJsonMapper().writeValueAsString(entry);
        Mockito.when(jmxJmxServiceMXBean.getMXBeanEntryJson("com.example.Foo")).thenReturn(entryJson);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx")
                .withQueryParam("operation", "mxBeanEntry")
                .withQueryParam("mxBeanTypeName", "com.example.Foo");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        // Dispatch returns the accessor's JSON parsed to a JsonNode -- assert it equals the tree of
        // the original entry JSON, proving the raw JSON is carried through byte-identically.
        Assertions.assertEquals(JsonUtil.getJsonMapper().readTree(entryJson), exchange.body());
    }

    @Test
    void jmxMxBeanEntryOperationReturns404WhenTheTypeIsUnknown() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxJmxService jmxJmxServiceMXBean = Mockito.mock(JmxJmxService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxJmxService.class)).thenReturn(jmxJmxServiceMXBean);
        // An unknown MXBean type surfaces as an MBeanException wrapping a ClassNotFoundException.
        Mockito.when(jmxJmxServiceMXBean.getMXBeanEntryJson("missing.Type"))
                .thenThrow(new MBeanException(new ClassNotFoundException("missing.Type")));

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx")
                .withQueryParam("operation", "mxBeanEntry")
                .withQueryParam("mxBeanTypeName", "missing.Type");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        Assertions.assertEquals(404, exchange.status());
    }

    @Test
    void jmxMxBeanEntryOperationReturnsAnEmptyObjectWhenTheAccessorYieldsNull() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxJmxService jmxJmxServiceMXBean = Mockito.mock(JmxJmxService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxJmxService.class)).thenReturn(jmxJmxServiceMXBean);
        Mockito.when(jmxJmxServiceMXBean.getMXBeanEntryJson("null.Type")).thenReturn(null);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx")
                .withQueryParam("operation", "mxBeanEntry")
                .withQueryParam("mxBeanTypeName", "null.Type");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        // A null accessor result must not leave the Mono empty (no response written); it becomes an
        // empty JSON object, and writeJson still runs (status stays 200).
        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(Map.of(), exchange.body());
    }

    @Test
    void jmxMxBeanEntryWithoutATypeNameReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        // operation is valid but the required mxBeanTypeName is absent: the secondary-parameter
        // 400 path (requireParam), distinct from the missing-operation case.
        final FakeExchange exchange = new FakeExchange("GET", "/jmx").withQueryParam("operation", "mxBeanEntry");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void jmxMxBeanEntryPropagatesANonClassNotFoundMBeanExceptionRatherThanMappingItTo404() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxJmxService jmxJmxServiceMXBean = Mockito.mock(JmxJmxService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxJmxService.class)).thenReturn(jmxJmxServiceMXBean);
        // An MBeanException whose target is NOT a ClassNotFoundException must fall through the 404
        // guard and escape unmapped -- the servlet's catch(Exception) maps the same failure to 500;
        // here ExceptionHandlerWebFilter would (the unit harness has no filter, so it surfaces).
        Mockito.when(jmxJmxServiceMXBean.getMXBeanEntryJson("boom.Type"))
                .thenThrow(new MBeanException(new IllegalStateException("boom")));

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx")
                .withQueryParam("operation", "mxBeanEntry")
                .withQueryParam("mxBeanTypeName", "boom.Type");

        Assertions.assertThrows(RuntimeException.class,
                () -> registry.lookup("GET", "/jmx").handle(exchange).block());
        // Proves the 404 DispatchException branch was NOT taken (status untouched, error escaped).
        Assertions.assertNotEquals(404, exchange.status());
    }

    @Test
    void jmxWithAnUnsupportedOperationReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/jmx").withQueryParam("operation", "notARealOperation");
        registry.lookup("GET", "/jmx").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void mediatorStatusOperationReturnsTheServiceStatusName() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxMediatorService jmxMediatorServiceMXBean = Mockito.mock(JmxMediatorService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxMediatorService.class)).thenReturn(jmxMediatorServiceMXBean);
        Mockito.when(jmxMediatorServiceMXBean.getStatus()).thenReturn(ServiceStatus.STARTED);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/mediator").withQueryParam("operation", "status");
        registry.lookup("GET", "/mediator").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals("STARTED", exchange.body());
    }

    @Test
    void mediatorIsInitWithConfigurationFileOperationReturnsTheBooleanValue() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxMediatorService jmxMediatorServiceMXBean = Mockito.mock(JmxMediatorService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxMediatorService.class)).thenReturn(jmxMediatorServiceMXBean);
        Mockito.when(jmxMediatorServiceMXBean.isInitWithConfigurationFile()).thenReturn(true);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/mediator")
                .withQueryParam("operation", "isInitWithConfigurationFile");
        registry.lookup("GET", "/mediator").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new Value(true), exchange.body());
    }

    @Test
    void mediatorIsInitWithDefaultConfigurationOperationReturnsTheBooleanValue() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxMediatorService jmxMediatorServiceMXBean = Mockito.mock(JmxMediatorService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxMediatorService.class)).thenReturn(jmxMediatorServiceMXBean);
        Mockito.when(jmxMediatorServiceMXBean.isInitWithDefaultConfiguration()).thenReturn(false);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/mediator")
                .withQueryParam("operation", "isInitWithDefaultConfiguration");
        registry.lookup("GET", "/mediator").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new Value(false), exchange.body());
    }

    @Test
    void mediatorConfigurationFilePathOperationReturnsThePathValue() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxMediatorService jmxMediatorServiceMXBean = Mockito.mock(JmxMediatorService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxMediatorService.class)).thenReturn(jmxMediatorServiceMXBean);
        Mockito.when(jmxMediatorServiceMXBean.getConfigurationFilePath()).thenReturn("/etc/appbase.yaml");

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange = new FakeExchange("GET", "/mediator")
                .withQueryParam("operation", "configurationFilePath");
        registry.lookup("GET", "/mediator").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertEquals(new Value("/etc/appbase.yaml"), exchange.body());
    }

    @Test
    void mediatorWithAnUnsupportedOperationReturns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);

        OffloadedEndpoints.register(registry, core, Schedulers.immediate(), DEFAULT_POLICY);

        final FakeExchange exchange =
                new FakeExchange("GET", "/mediator").withQueryParam("operation", "notARealOperation");
        registry.lookup("GET", "/mediator").handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void anOffloadedReadThatOutlivesThePolicyTimeoutErrorsWithATimeoutException() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final JmxConfigurationService jmxConfigurationService = Mockito.mock(JmxConfigurationService.class);
        Mockito.when(core.getJmxService().getMXBean(JmxConfigurationService.class))
                .thenReturn(jmxConfigurationService);
        Mockito.when(jmxConfigurationService.getStatus()).thenAnswer(invocation -> {
            // Simulate a stuck blocking read: sleeps well past the short policy timeout below.
            Thread.sleep(2000);
            return ServiceStatus.STARTED;
        });
        // Schedulers.immediate() (used by the other tests) runs synchronously and has no real
        // clock/worker for .timeout() to fire against -- a real, time-based scheduler is required.
        final OffloadPolicy shortTimeoutPolicy = OffloadPolicy.of(1000, Duration.ofMillis(100));

        OffloadedEndpoints.register(registry, core, Schedulers.boundedElastic(), shortTimeoutPolicy);

        final FakeExchange exchange = new FakeExchange("GET", "/configuration")
                .withQueryParam("operation", "status");

        final RuntimeException thrown = Assertions.assertThrows(RuntimeException.class,
                () -> registry.lookup("GET", "/configuration").handle(exchange).block());

        Throwable cause = thrown;
        boolean foundTimeoutException = false;
        while (cause != null) {
            if (cause instanceof TimeoutException) {
                foundTimeoutException = true;
                break;
            }
            cause = cause.getCause();
        }
        Assertions.assertTrue(foundTimeoutException,
                "expected a TimeoutException in the cause chain, got: " + thrown);
    }
}
