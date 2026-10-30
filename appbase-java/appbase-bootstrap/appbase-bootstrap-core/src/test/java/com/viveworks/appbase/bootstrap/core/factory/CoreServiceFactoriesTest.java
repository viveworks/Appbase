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

package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for all 13 built-in {@link AppbaseCoreServiceFactory} implementations.
 *
 * <p>Tests {@code serviceType()}, {@code initializationOrder()}, and basic factory
 * construction. Integration tests for the full build path live in
 * {@link com.viveworks.appbase.bootstrap.core.AppbaseCoreServicesTest}.
 */
class CoreServiceFactoriesTest {

    // ── serviceType() ──────────────────────────────────────────────────────────

    @Test
    void logServiceFactory_serviceType() {
        assertEquals(LogService.class,
            new LogServiceFactory(mock(DefaultLogServiceConfiguration.class)).serviceType());
    }

    @Test
    void metricServiceFactory_serviceType() {
        assertEquals(MetricService.class,
            new MetricServiceFactory(mock(DefaultMetricServiceConfiguration.class)).serviceType());
    }

    @Test
    void validationServiceFactory_serviceType() {
        assertEquals(ValidationService.class,
            new ValidationServiceFactory(mock(DefaultValidationServiceConfiguration.class)).serviceType());
    }

    @Test
    void mediatorServiceFactory_serviceType() {
        LogServiceFactory logFactory = new LogServiceFactory(mock(DefaultLogServiceConfiguration.class));
        assertEquals(MediatorService.class,
            new MediatorServiceFactory(logFactory, mock(DefaultMediatorServiceConfiguration.class)).serviceType());
    }

    @Test
    void configurationServiceFactory_serviceType() {
        assertEquals(ConfigurationService.class,
            new ConfigurationServiceFactory(mock(DefaultConfigurationServiceConfiguration.class)).serviceType());
    }

    @Test
    void microprofileConfigurationServiceFactory_serviceType() {
        assertEquals(MicroprofileConfigurationService.class,
            new MicroprofileConfigurationServiceFactory().serviceType());
    }

    @Test
    void serviceRegistryFactory_serviceType() {
        assertEquals(ServiceRegistry.class,
            new ServiceRegistryFactory(mock(DefaultServiceRegistryConfiguration.class)).serviceType());
    }

    @Test
    void microprofileServiceRegistryFactory_serviceType() {
        assertEquals(MicroprofileServiceRegistry.class,
            new MicroprofileServiceRegistryFactory().serviceType());
    }

    @Test
    void exceptionServiceFactory_serviceType() {
        assertEquals(ExceptionService.class,
            new ExceptionServiceFactory(mock(DefaultExceptionServiceConfiguration.class)).serviceType());
    }

    @Test
    void tracerServiceFactory_serviceType() {
        assertEquals(TracerService.class,
            new TracerServiceFactory(mock(DefaultTracerServiceConfiguration.class)).serviceType());
    }

    @Test
    void jmxServiceFactory_serviceType() {
        assertEquals(JmxService.class,
            new JmxServiceFactory(mock(DefaultJmxServiceConfiguration.class)).serviceType());
    }

    @Test
    void reactorServiceFactory_serviceType() {
        assertEquals(ReactorService.class,
            new ReactorServiceFactory(mock(DefaultReactorServiceConfiguration.class)).serviceType());
    }

    @Test
    void luceneServiceFactory_serviceType() {
        assertEquals(LuceneService.class,
            new LuceneServiceFactory(mock(DefaultLuceneServiceConfiguration.class)).serviceType());
    }

    // ── initializationOrder() ─────────────────────────────────────────────────

    @Test
    void logServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.LOG.order(),
            new LogServiceFactory(mock(DefaultLogServiceConfiguration.class)).initializationOrder());
    }

    @Test
    void metricServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.METRIC.order(),
            new MetricServiceFactory(mock(DefaultMetricServiceConfiguration.class)).initializationOrder());
    }

    @Test
    void validationServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.VALIDATION.order(),
            new ValidationServiceFactory(mock(DefaultValidationServiceConfiguration.class)).initializationOrder());
    }

    @Test
    void mediatorServiceFactory_initializationOrder() {
        LogServiceFactory logFactory = new LogServiceFactory(mock(DefaultLogServiceConfiguration.class));
        assertEquals(CoreServiceOrder.MEDIATOR.order(),
            new MediatorServiceFactory(logFactory, mock(DefaultMediatorServiceConfiguration.class))
                .initializationOrder());
    }

    @Test
    void configurationServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.CONFIGURATION.order(),
            new ConfigurationServiceFactory(mock(DefaultConfigurationServiceConfiguration.class))
                .initializationOrder());
    }

    @Test
    void microprofileConfigurationServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.MICROPROFILE_CONFIGURATION.order(),
            new MicroprofileConfigurationServiceFactory().initializationOrder());
    }

    @Test
    void serviceRegistryFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.SERVICE_REGISTRY.order(),
            new ServiceRegistryFactory(mock(DefaultServiceRegistryConfiguration.class)).initializationOrder());
    }

    @Test
    void microprofileServiceRegistryFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY.order(),
            new MicroprofileServiceRegistryFactory().initializationOrder());
    }

    @Test
    void exceptionServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.EXCEPTION.order(),
            new ExceptionServiceFactory(mock(DefaultExceptionServiceConfiguration.class)).initializationOrder());
    }

    @Test
    void tracerServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.TRACER.order(),
            new TracerServiceFactory(mock(DefaultTracerServiceConfiguration.class)).initializationOrder());
    }

    @Test
    void jmxServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.JMX.order(),
            new JmxServiceFactory(mock(DefaultJmxServiceConfiguration.class)).initializationOrder());
    }

    @Test
    void reactorServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.REACTOR.order(),
            new ReactorServiceFactory(mock(DefaultReactorServiceConfiguration.class)).initializationOrder());
    }

    @Test
    void luceneServiceFactory_initializationOrder() {
        assertEquals(CoreServiceOrder.LUCENE.order(),
            new LuceneServiceFactory(mock(DefaultLuceneServiceConfiguration.class)).initializationOrder());
    }

    // ── LogServiceFactory shared registry ─────────────────────────────────────

    @Test
    void logServiceFactory_registryIsNotNullAfterConstruction() {
        assertNotNull(
            new LogServiceFactory(mock(DefaultLogServiceConfiguration.class))
                .getMediatorEventDelegatorRegistry());
    }

    // ── create() for factories with no complex infra deps ─────────────────────

    @Test
    void microprofileConfigurationServiceFactory_create_returnsNonNull() throws Exception {
        AppbaseCoreServices partial = buildWithAllMandatory(mock(ConfigurationService.class));
        assertNotNull(new MicroprofileConfigurationServiceFactory().create(partial));
    }

    @Test
    void microprofileServiceRegistryFactory_create_returnsNonNull() throws Exception {
        AppbaseCoreServices partial = buildWithAllMandatory(mock(ServiceRegistry.class));
        assertNotNull(new MicroprofileServiceRegistryFactory().create(partial));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T extends AppbaseService> AppbaseCoreServiceFactory<T> mockFactory(
            Class<T> type, CoreServiceOrder order, T instance) {
        AppbaseCoreServiceFactory<T> f = mock(AppbaseCoreServiceFactory.class);
        when(f.serviceType()).thenReturn(type);
        when(f.initializationOrder()).thenReturn(order.order());
        try { when(f.create(any())).thenReturn(instance); } catch (Exception ignored) { }
        return f;
    }

    /** Builds a full mandatory container with a real configSvc and mock registry. */
    private AppbaseCoreServices buildWithAllMandatory(ConfigurationService configSvc) {
        return AppbaseCoreServices.builder().with(
            mockFactory(LogService.class,                       CoreServiceOrder.LOG,                          mock(LogService.class)),
            mockFactory(MetricService.class,                    CoreServiceOrder.METRIC,                       mock(MetricService.class)),
            mockFactory(ValidationService.class,                CoreServiceOrder.VALIDATION,                   mock(ValidationService.class)),
            mockFactory(MediatorService.class,                  CoreServiceOrder.MEDIATOR,                     mock(MediatorService.class)),
            mockFactory(ConfigurationService.class,             CoreServiceOrder.CONFIGURATION,                configSvc),
            mockFactory(MicroprofileConfigurationService.class, CoreServiceOrder.MICROPROFILE_CONFIGURATION,   mock(MicroprofileConfigurationService.class)),
            mockFactory(ServiceRegistry.class,                  CoreServiceOrder.SERVICE_REGISTRY,             mock(ServiceRegistry.class)),
            mockFactory(MicroprofileServiceRegistry.class,      CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY, mock(MicroprofileServiceRegistry.class)),
            mockFactory(ExceptionService.class,                 CoreServiceOrder.EXCEPTION,                    mock(ExceptionService.class)),
            mockFactory(TracerService.class,                    CoreServiceOrder.TRACER,                       mock(TracerService.class)),
            mockFactory(JmxService.class,                       CoreServiceOrder.JMX,                          mock(JmxService.class))
        ).build();
    }

    /** Builds a full mandatory container with a real serviceRegistry and mock config. */
    private AppbaseCoreServices buildWithAllMandatory(ServiceRegistry serviceRegistry) {
        return AppbaseCoreServices.builder().with(
            mockFactory(LogService.class,                       CoreServiceOrder.LOG,                          mock(LogService.class)),
            mockFactory(MetricService.class,                    CoreServiceOrder.METRIC,                       mock(MetricService.class)),
            mockFactory(ValidationService.class,                CoreServiceOrder.VALIDATION,                   mock(ValidationService.class)),
            mockFactory(MediatorService.class,                  CoreServiceOrder.MEDIATOR,                     mock(MediatorService.class)),
            mockFactory(ConfigurationService.class,             CoreServiceOrder.CONFIGURATION,                mock(ConfigurationService.class)),
            mockFactory(MicroprofileConfigurationService.class, CoreServiceOrder.MICROPROFILE_CONFIGURATION,   mock(MicroprofileConfigurationService.class)),
            mockFactory(ServiceRegistry.class,                  CoreServiceOrder.SERVICE_REGISTRY,             serviceRegistry),
            mockFactory(MicroprofileServiceRegistry.class,      CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY, mock(MicroprofileServiceRegistry.class)),
            mockFactory(ExceptionService.class,                 CoreServiceOrder.EXCEPTION,                    mock(ExceptionService.class)),
            mockFactory(TracerService.class,                    CoreServiceOrder.TRACER,                       mock(TracerService.class)),
            mockFactory(JmxService.class,                       CoreServiceOrder.JMX,                          mock(JmxService.class))
        ).build();
    }
}
