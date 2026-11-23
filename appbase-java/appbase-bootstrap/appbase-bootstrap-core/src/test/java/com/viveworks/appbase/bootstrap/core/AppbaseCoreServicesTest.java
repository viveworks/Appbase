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

package com.viveworks.appbase.bootstrap.core;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppbaseCoreServicesTest {

    private LogService mockLog;
    private MetricService mockMetric;
    private ValidationService mockValidation;
    private MediatorService mockMediator;
    private ConfigurationService mockConfig;
    private MicroprofileConfigurationService mockMpConfig;
    private ServiceRegistry mockRegistry;
    private MicroprofileServiceRegistry mockMpRegistry;
    private ExceptionService mockException;
    private TracerService mockTracer;
    private JmxService mockJmx;

    @BeforeEach
    void setUp() {
        mockLog        = mock(LogService.class);
        mockMetric     = mock(MetricService.class);
        mockValidation = mock(ValidationService.class);
        mockMediator   = mock(MediatorService.class);
        mockConfig     = mock(ConfigurationService.class);
        mockMpConfig   = mock(MicroprofileConfigurationService.class);
        mockRegistry   = mock(ServiceRegistry.class);
        mockMpRegistry = mock(MicroprofileServiceRegistry.class);
        mockException  = mock(ExceptionService.class);
        mockTracer     = mock(TracerService.class);
        mockJmx        = mock(JmxService.class);
    }

    private AppbaseCoreServices buildWithAllMandatory() {
        return AppbaseCoreServices.builder()
            .with(
                mockFactory(LogService.class,                       CoreServiceOrder.LOG,                          mockLog),
                mockFactory(MetricService.class,                    CoreServiceOrder.METRIC,                       mockMetric),
                mockFactory(ValidationService.class,                CoreServiceOrder.VALIDATION,                   mockValidation),
                mockFactory(MediatorService.class,                  CoreServiceOrder.MEDIATOR,                     mockMediator),
                mockFactory(ConfigurationService.class,             CoreServiceOrder.CONFIGURATION,                mockConfig),
                mockFactory(MicroprofileConfigurationService.class, CoreServiceOrder.MICROPROFILE_CONFIGURATION,   mockMpConfig),
                mockFactory(ServiceRegistry.class,                  CoreServiceOrder.SERVICE_REGISTRY,             mockRegistry),
                mockFactory(MicroprofileServiceRegistry.class,      CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY, mockMpRegistry),
                mockFactory(ExceptionService.class,                 CoreServiceOrder.EXCEPTION,                    mockException),
                mockFactory(TracerService.class,                    CoreServiceOrder.TRACER,                       mockTracer),
                mockFactory(JmxService.class,                       CoreServiceOrder.JMX,                          mockJmx)
            )
            .build();
    }

    @SuppressWarnings("unchecked")
    private <T extends AppbaseService> AppbaseCoreServiceFactory<T> mockFactory(
            Class<T> type, CoreServiceOrder order, T instance) {
        AppbaseCoreServiceFactory<T> f = mock(AppbaseCoreServiceFactory.class);
        when(f.serviceType()).thenReturn(type);
        when(f.initializationOrder()).thenReturn(order.order());
        try { when(f.create(any())).thenReturn(instance); } catch (Exception ignored) { }
        return f;
    }

    @Test
    void getLogService_returnsLogService() {
        assertSame(mockLog, buildWithAllMandatory().getLogService());
    }

    @Test
    void fromYaml_initializesDefaultLogAccessor() throws Exception {
        final AppbaseCoreServices services = AppbaseCoreServices.builder()
            .fromYaml("classpath:///appbase-test-config.yaml")
            .build();
        try {
            assertNotNull(services.getLogService().getLogAccessor());
        } finally {
            services.shutdown();
        }
    }

    @Test
    void getMetricService_returnsMetricService() {
        assertSame(mockMetric, buildWithAllMandatory().getMetricService());
    }

    @Test
    void getJmxService_returnsJmxService() {
        assertSame(mockJmx, buildWithAllMandatory().getJmxService());
    }

    @Test
    void get_byType_returnsService() {
        assertSame(mockLog, buildWithAllMandatory().get(LogService.class));
    }

    @Test
    void get_unknownType_throwsAppbaseRuntimeException() {
        AppbaseCoreServices services = buildWithAllMandatory();
        assertThrows(AppbaseRuntimeException.class, () -> services.get(ReactorService.class));
    }

    @Test
    void services_containsAll11Mandatory() {
        Map<Class<?>, Object> map = buildWithAllMandatory().services();
        assertEquals(11, map.size());
        assertTrue(map.containsKey(LogService.class));
        assertTrue(map.containsKey(JmxService.class));
        assertTrue(map.containsKey(MicroprofileConfigurationService.class));
        assertTrue(map.containsKey(MicroprofileServiceRegistry.class));
    }

    @Test
    void services_returnsUnmodifiableMap() {
        assertThrows(UnsupportedOperationException.class,
            () -> buildWithAllMandatory().services().put(LogService.class, null));
    }

    @Test
    void build_missingMandatoryService_throwsAppbaseRuntimeException() {
        AppbaseCoreServiceFactory<LogService> logOnly =
            mockFactory(LogService.class, CoreServiceOrder.LOG, mockLog);
        assertThrows(AppbaseRuntimeException.class,
            () -> AppbaseCoreServices.builder().with(logOnly).build());
    }

    @Test
    void getReactorService_whenAbsent_returnsNull() {
        assertNull(buildWithAllMandatory().getReactorService());
    }

    @Test
    void with_factoryOverridesByServiceType() {
        LogService altLog = mock(LogService.class);
        AppbaseCoreServices services = AppbaseCoreServices.builder()
            .with(mockFactory(LogService.class, CoreServiceOrder.LOG, mockLog))
            .with(
                mockFactory(MetricService.class,                    CoreServiceOrder.METRIC,                       mockMetric),
                mockFactory(ValidationService.class,                CoreServiceOrder.VALIDATION,                   mockValidation),
                mockFactory(MediatorService.class,                  CoreServiceOrder.MEDIATOR,                     mockMediator),
                mockFactory(ConfigurationService.class,             CoreServiceOrder.CONFIGURATION,                mockConfig),
                mockFactory(MicroprofileConfigurationService.class, CoreServiceOrder.MICROPROFILE_CONFIGURATION,   mockMpConfig),
                mockFactory(ServiceRegistry.class,                  CoreServiceOrder.SERVICE_REGISTRY,             mockRegistry),
                mockFactory(MicroprofileServiceRegistry.class,      CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY, mockMpRegistry),
                mockFactory(ExceptionService.class,                 CoreServiceOrder.EXCEPTION,                    mockException),
                mockFactory(TracerService.class,                    CoreServiceOrder.TRACER,                       mockTracer),
                mockFactory(JmxService.class,                       CoreServiceOrder.JMX,                          mockJmx),
                mockFactory(LogService.class,                       CoreServiceOrder.LOG,                          altLog)
            )
            .build();
        assertSame(altLog, services.getLogService());
    }

    @Test
    void shutdown_callsShutdownOnRegisteredServices() throws Exception {
        buildWithAllMandatory().shutdown();
        verify(mockLog).shutdown();
        verify(mockJmx).shutdown();
    }
}
