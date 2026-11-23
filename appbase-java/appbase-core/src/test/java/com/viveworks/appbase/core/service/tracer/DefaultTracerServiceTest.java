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

package com.viveworks.appbase.core.service.tracer;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.propagation.JaegerPropagationConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.propagation.JaegerPropagationType;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.InMemoryReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.RemoteReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.GuaranteedThroughputSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.PerOperationSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ProbabilisticSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RateLimitingSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RemoteControlledSamplerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultTracerServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDown() {
        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testInMemoryReporter() throws Exception {
        final InMemoryReporterConfiguration inMemoryReporterConfiguration =
                InMemoryReporterConfiguration.builder().enabled(true).build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerReporterConfigurations(List.of(inMemoryReporterConfiguration))
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(defaultTracerServiceConfiguration, "testInMemoryReporter");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testInMemoryReporter");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testLoggingReporter() throws Exception {
        final LoggingReporterConfiguration loggingReporterConfiguration =
                LoggingReporterConfiguration.builder().enabled(true).build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerReporterConfigurations(List.of(loggingReporterConfiguration))
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(defaultTracerServiceConfiguration, "testLoggingReporter");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testLoggingReporter");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testCompositeReporter() throws Exception {
        final InMemoryReporterConfiguration inMemoryReporterConfiguration =
                InMemoryReporterConfiguration.builder().enabled(true).build();
        final LoggingReporterConfiguration loggingReporterConfiguration =
                LoggingReporterConfiguration.builder().enabled(true).build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerReporterConfigurations(List.of(inMemoryReporterConfiguration, loggingReporterConfiguration))
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(defaultTracerServiceConfiguration, "testCompositeReporter");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testCompositeReporter");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testConstSampler() throws Exception {
        final ConstSamplerConfiguration constSamplerConfiguration =
                ConstSamplerConfiguration.builder().decision(true).build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerSamplerConfiguration(constSamplerConfiguration)
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(defaultTracerServiceConfiguration, "testConstSampler");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testConstSampler");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testGuaranteedThroughputSampler() throws Exception {
        final GuaranteedThroughputSamplerConfiguration guaranteedThroughputSamplerConfiguration =
                GuaranteedThroughputSamplerConfiguration.builder().build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerSamplerConfiguration(guaranteedThroughputSamplerConfiguration)
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(
                defaultTracerServiceConfiguration, "testGuaranteedThroughputSampler");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(
                defaultTracerServiceConfiguration, "testGuaranteedThroughputSampler");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testPerOperationSampler() throws Exception {
        final PerOperationSamplerConfiguration perOperationSamplerConfiguration =
                PerOperationSamplerConfiguration
                        .builder()
                        .samplingRateByOperationNameMap(Map.of("testPerOperationSampler", 1.0))
                        .build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerSamplerConfiguration(perOperationSamplerConfiguration)
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(defaultTracerServiceConfiguration, "testPerOperationSampler");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testPerOperationSampler");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testProbabilisticSampler() throws Exception {
        final ProbabilisticSamplerConfiguration probabilisticSamplerConfiguration =
                ProbabilisticSamplerConfiguration.builder().samplingRate(1.0).build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerSamplerConfiguration(probabilisticSamplerConfiguration)
                .jaegerPropagationConfiguration(JaegerPropagationConfiguration
                        .builder()
                        .jaegerPropagationTypes(Set.of(JaegerPropagationType.B3))
                        .build())
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(defaultTracerServiceConfiguration, "testProbabilisticSampler");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testProbabilisticSampler");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testRateLimitingSampler() throws Exception {
        final RateLimitingSamplerConfiguration rateLimitingSamplerConfiguration =
                RateLimitingSamplerConfiguration.builder().build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerSamplerConfiguration(rateLimitingSamplerConfiguration)
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService = assertTracer(defaultTracerServiceConfiguration, "testRateLimitingSampler");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testRateLimitingSampler");
        tracerService.shutdown();
        tracerService1.shutdown();
    }

    @Test
    void testConfigurationEvents() throws Exception {
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration =
                DefaultTracerServiceConfiguration.builder().build();
        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(DefaultTracerServiceConfiguration.class);
        final ConfigurationValue<DefaultTracerServiceConfiguration> configurationValue =
                ConfigurationValue.fromValue(defaultTracerServiceConfiguration);
        final ConfigurationService configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
        configurationService.putTypedConfiguration(
                DefaultTracerServiceConfiguration.class,
                DefaultTracerServiceConfiguration.builder().build());
        final DefaultTracerService defaultTraceService =
                new DefaultTracerService(
                        logService, metricService, mediatorService, validationService, configurationService);

        try {
            configurationService.putTypedConfiguration(
                    DefaultTracerServiceConfiguration.class, defaultTracerServiceConfiguration);
            final Tracer tracer = defaultTraceService.getTracer();
            Assertions.assertNotNull(tracer);

            final ConfigurationAddedEvent configurationAddedEvent =
                    new ConfigurationAddedEvent(configurationKey, configurationValue, System.currentTimeMillis());
            defaultTraceService.onConfigurationAdded(configurationAddedEvent);
            assertTracer(tracer, "onConfigurationAdded");

            final ConfigurationChangedEvent configurationChangedEvent =
                    new ConfigurationChangedEvent(
                            configurationKey, configurationValue, configurationValue, System.currentTimeMillis());
            defaultTraceService.onConfigurationChanged(configurationChangedEvent);
            assertTracer(tracer, "configurationChangedEvent");

            final ConfigurationRemovedEvent configurationRemovedEvent =
                    new ConfigurationRemovedEvent(configurationKey, System.currentTimeMillis());
            defaultTraceService.onConfigurationRemoved(configurationRemovedEvent);
            assertTracer(tracer, "configurationRemovedEvent");
        } finally {
            defaultTraceService.shutdown();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "DefaultTracerServiceTest.testRemoteReporter", matches = "true")
    void testRemoteReporter() throws Exception {
        final RemoteReporterConfiguration remoteReporterConfiguration =
                RemoteReporterConfiguration.builder().build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerReporterConfigurations(List.of(remoteReporterConfiguration))
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService =
                assertTracer(defaultTracerServiceConfiguration, "testRemoteReporter");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testRemoteReporter");
        tracerService.shutdown();
        tracerService1.shutdown();

        final Metric metric = metricService.getMetricRegistry().getMetrics().get(new MetricID("jaeger_tracer_traces"));
        Assertions.assertNotNull(metric);
    }

    @Test
    @EnabledIfSystemProperty(named = "DefaultTracerServiceTest.testRemoteSampler", matches = "true")
    void testRemoteSampler() throws Exception {
        final RemoteControlledSamplerConfiguration remoteControlledSamplerConfiguration =
                RemoteControlledSamplerConfiguration.builder().build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerSamplerConfiguration(remoteControlledSamplerConfiguration)
                .tags(Map.of("k1", "v1"))
                .build();
        final TracerService tracerService =
                assertTracer(defaultTracerServiceConfiguration, "testRemoteSampler");
        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        Assertions.assertNotNull(configuration);

        tracerService.reload(defaultTracerServiceConfiguration);
        final TracerService tracerService1 = assertTracer(defaultTracerServiceConfiguration, "testRemoteSampler");
        tracerService.shutdown();
        tracerService1.shutdown();

        final Metric metric = metricService.getMetricRegistry().getMetrics().get(new MetricID("jaeger_tracer_traces"));
        Assertions.assertNotNull(metric);
    }

    private TracerService assertTracer(
            final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration,
            final String operationName)
    {
        final ConfigurationService configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
        try {
            configurationService.putTypedConfiguration(
                    DefaultTracerServiceConfiguration.class,
                    defaultTracerServiceConfiguration);
            final DefaultTracerService defaultTraceService = new DefaultTracerService(
                    logService, metricService, mediatorService, validationService, configurationService);

            final Tracer tracer = defaultTraceService.getTracer();
            Assertions.assertNotNull(tracer);
            assertTracer(tracer, operationName);

            /*
            // Uncomment to verify
            final List<Meter> meters = metricService.getCompositeMeterRegistry().getMeters();
            meters.forEach(meter -> {
                logger.info("Meter Name: {}", meter.getId().getName());
            });
            */

            return defaultTraceService;
        } finally {
            configurationService.shutdown();
        }
    }

    private void assertTracer(final Tracer tracer, final String operationName) {
        final Span span = tracer
                .buildSpan(operationName)
                .start();
        span.finish(1000_000L);
        final String spanId = span.context().toSpanId();
        Assertions.assertNotNull(spanId);
        final String traceId = span.context().toTraceId();
        Assertions.assertNotNull(traceId);
    }
}
