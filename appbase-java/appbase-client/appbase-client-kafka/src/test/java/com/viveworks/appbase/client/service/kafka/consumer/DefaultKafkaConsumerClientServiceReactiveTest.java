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

package com.viveworks.appbase.client.service.kafka.consumer;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.DefaultKafkaConsumerClientServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.consumer.reactive.ReactiveKafkaConsumerAccessor;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import jakarta.validation.Validator;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * Tests for {@link DefaultKafkaConsumerClientService} accessor acquisition:
 * {@link DefaultKafkaConsumerClientService#getAccessor()}/{@code getAccessor(key)} and
 * {@link DefaultKafkaConsumerClientService#getReactiveAccessor()}/{@code getReactiveAccessor(key)}.
 *
 * <p>These are plain Mockito unit tests: the service is wired via its pool-manager-accepting
 * constructor with a fully mocked {@link AppbaseClientPoolManager}/{@link AppbaseClientCore} and a
 * configuration with {@code autoStart=false}, so {@code start()}/{@code sanityCheck()} never run and
 * no live Kafka connection is attempted. The sync {@code getAccessor} captures only a lazy
 * {@code getClientIntegrationService} method reference (invoked per-operation, not at assembly time),
 * so the pool is never touched. For the EAGER reactive path a {@link Mockito#spy} stubs
 * {@code getClientIntegrationService()} to return a mocked integration service whose
 * {@code buildConsumerProperties()} yields a minimal {@link Properties} and whose
 * {@code subscriptionTopics()} yields a non-empty topic list — {@code ReceiverOptions}/
 * {@code KafkaReceiver} build without connecting because the reactor-kafka consumer is lazy.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultKafkaConsumerClientServiceReactiveTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultKafkaConsumerClientService.registerObjectMapperSubtypes();
    }

    private static AppbaseCore mockAppbaseCore(final DefaultKafkaConsumerClientServiceConfiguration configuration) {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultKafkaConsumerClientServiceReactiveTest.class));

        final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
        final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
        final Timer timer = Mockito.mock(Timer.class);
        final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
        final Counter counter = Mockito.mock(Counter.class);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
        Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
        Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(counter);
        Mockito.when(timer.time()).thenReturn(timerContext);

        final MediatorService mediatorService = Mockito.mock(MediatorService.class);
        final Mediator mediator = Mockito.mock(Mediator.class);
        Mockito.when(appbaseCore.getMediatorService()).thenReturn(mediatorService);
        Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
        Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

        final ValidationService validationService = Mockito.mock(ValidationService.class);
        Mockito.when(appbaseCore.getValidationService()).thenReturn(validationService);
        Mockito.when(validationService.getValidator()).thenReturn(Mockito.mock(Validator.class));

        final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        Mockito.when(configurationService.getTypedConfiguration(DefaultKafkaConsumerClientServiceConfiguration.class))
                .thenReturn(configuration);

        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(appbaseCore.getReactorService()).thenReturn(reactorService);

        return appbaseCore;
    }

    private static AppbaseClientCore mockAppbaseClientCore(
            final DefaultKafkaConsumerClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        final AppbaseCore appbaseCore = mockAppbaseCore(configuration);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);
        return appbaseClientCore;
    }

    private static DefaultKafkaConsumerClientServiceConfiguration testConfiguration() {
        return DefaultKafkaConsumerClientServiceConfiguration.builder()
                .autoStart(false)
                .reactiveOperationConfiguration(
                        DefaultReactiveOperationConfiguration.builder().metricsEnabled(false).build())
                .build();
    }

    private static DefaultKafkaConsumerClientService<String, String> wiredService(
            final DefaultKafkaConsumerClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = mockAppbaseClientCore(configuration);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey =
                AppbaseClientUtil.clusterIdValueKey(
                        ClusterIdValueConfiguration.builder().clusterId("test-kafka").build());
        final AppbaseClientPoolManager appbaseClientPoolManager = Mockito.mock(AppbaseClientPoolManager.class);

        return new DefaultKafkaConsumerClientService<>(
                appbaseClientCore, defaultAppbaseClientKey, appbaseClientPoolManager, configuration, false, null);
    }

    private static Properties minimalConsumerProperties() {
        final Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("group.id", "test-group");
        properties.put("key.deserializer", StringDeserializer.class.getName());
        properties.put("value.deserializer", StringDeserializer.class.getName());
        return properties;
    }

    @Test
    void getAccessorReturnsAccessorWithServiceConfig() {
        final DefaultKafkaConsumerClientService<String, String> service = wiredService(testConfiguration());

        final KafkaConsumerAccessor<String, String> accessor = service.getAccessor();

        Assertions.assertNotNull(accessor);
    }

    @Test
    void keyedGetAccessorReturnsAccessorWithMockedPool() {
        final DefaultKafkaConsumerClientService<String, String> service = wiredService(testConfiguration());
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = service.getDefaultAppbaseClientKey();

        final KafkaConsumerAccessor<String, String> accessor = service.getAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(accessor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReactiveAccessorReturnsAccessorWithServiceConfig()
            throws AppbaseClientIntegrationService.NotFoundException {
        final DefaultKafkaConsumerClientService<String, String> service = wiredService(testConfiguration());
        final DefaultKafkaConsumerClientService<String, String> spy = Mockito.spy(service);

        final KafkaConsumerClientIntegrationService<String, String> integrationService =
                Mockito.mock(KafkaConsumerClientIntegrationService.class);
        Mockito.when(integrationService.buildConsumerProperties()).thenReturn(minimalConsumerProperties());
        Mockito.when(integrationService.subscriptionTopics()).thenReturn(List.of("test-topic"));
        Mockito.doReturn(integrationService).when(spy).getClientIntegrationService();

        final ReactiveKafkaConsumerAccessor<String, String> accessor = spy.getReactiveAccessor();

        Assertions.assertNotNull(accessor);
        Assertions.assertFalse(accessor.getReactiveOperationConfiguration().isMetricsEnabled());
        Assertions.assertEquals(
                spy.getConfiguration().getReactiveOperationConfiguration().isMetricsEnabled(),
                accessor.getReactiveOperationConfiguration().isMetricsEnabled());
    }

    @Test
    @SuppressWarnings("unchecked")
    void keyedGetReactiveAccessorReturnsAccessorWithMockedPool()
            throws AppbaseClientIntegrationService.NotFoundException {
        final DefaultKafkaConsumerClientService<String, String> service = wiredService(testConfiguration());
        final DefaultKafkaConsumerClientService<String, String> spy = Mockito.spy(service);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = spy.getDefaultAppbaseClientKey();

        final KafkaConsumerClientIntegrationService<String, String> integrationService =
                Mockito.mock(KafkaConsumerClientIntegrationService.class);
        Mockito.when(integrationService.buildConsumerProperties()).thenReturn(minimalConsumerProperties());
        Mockito.when(integrationService.subscriptionTopics()).thenReturn(List.of("test-topic"));
        Mockito.doReturn(integrationService).when(spy).getClientIntegrationService(defaultAppbaseClientKey);

        final ReactiveKafkaConsumerAccessor<String, String> accessor =
                spy.getReactiveAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(accessor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReactiveAccessorWithNullSubscriptionTopicsThrows()
            throws AppbaseClientIntegrationService.NotFoundException {
        final DefaultKafkaConsumerClientService<String, String> service = wiredService(testConfiguration());
        final DefaultKafkaConsumerClientService<String, String> spy = Mockito.spy(service);

        final KafkaConsumerClientIntegrationService<String, String> integrationService =
                Mockito.mock(KafkaConsumerClientIntegrationService.class);
        Mockito.when(integrationService.subscriptionTopics()).thenReturn(null);
        Mockito.doReturn(integrationService).when(spy).getClientIntegrationService();

        Assertions.assertThrows(AppbaseRuntimeException.class, spy::getReactiveAccessor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReactiveAccessorWithEmptySubscriptionTopicsThrows()
            throws AppbaseClientIntegrationService.NotFoundException {
        final DefaultKafkaConsumerClientService<String, String> service = wiredService(testConfiguration());
        final DefaultKafkaConsumerClientService<String, String> spy = Mockito.spy(service);

        final KafkaConsumerClientIntegrationService<String, String> integrationService =
                Mockito.mock(KafkaConsumerClientIntegrationService.class);
        Mockito.when(integrationService.subscriptionTopics()).thenReturn(List.of());
        Mockito.doReturn(integrationService).when(spy).getClientIntegrationService();

        Assertions.assertThrows(AppbaseRuntimeException.class, spy::getReactiveAccessor);
    }
}
