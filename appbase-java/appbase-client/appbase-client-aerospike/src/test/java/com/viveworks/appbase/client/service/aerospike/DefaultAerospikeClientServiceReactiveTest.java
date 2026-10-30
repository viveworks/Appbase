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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientServiceConfiguration;
import com.viveworks.appbase.client.service.aerospike.reactive.ReactiveAerospikeAccessor;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseCore;
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
import java.io.IOException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link DefaultAerospikeClientService#getReactiveAccessor()},
 * {@link DefaultAerospikeClientService#getReactiveAccessor(AppbaseClientKey)}, and the cached,
 * non-owning {@link com.aerospike.client.reactor.AerospikeReactorClient} wiring behind them.
 *
 * <p>Unlike Cassandra's reactive accessor, {@link ReactiveAerospikeAccessor} takes a
 * <strong>concrete</strong> {@link IAerospikeReactorClient} in its constructor, so
 * {@link DefaultAerospikeClientService#getReactiveAccessor(AppbaseClientKey)} must build one eagerly
 * rather than passing a lazy {@code Supplier}. To keep this unit-testable without a live Aerospike
 * cluster, these tests spy {@link DefaultAerospikeClientService} and stub the protected seam
 * {@link DefaultAerospikeClientService#buildReactorClient(AppbaseClientKey)}, which is where the real
 * (checked-exception-wrapping) construction of the reactor client happens.
 *
 * <p>All tests below are plain Mockito unit tests: they wire {@link DefaultAerospikeClientService} via
 * its pool-manager-accepting constructor with a fully mocked {@link AppbaseClientPoolManager}/
 * {@link AppbaseClientCore} and a configuration with {@code autoStart=false}, so {@code start()}/
 * {@code sanityCheck()} never run and no live Aerospike connection is attempted. A live-gated
 * round-trip belongs in the Task 7 integration test, not here.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultAerospikeClientServiceReactiveTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultAerospikeClientService.registerObjectMapperSubtypes();
    }

    private static AppbaseCore mockAppbaseCore(final DefaultAerospikeClientServiceConfiguration configuration) {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultAerospikeClientServiceReactiveTest.class));

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
        Mockito.when(configurationService.getTypedConfiguration(DefaultAerospikeClientServiceConfiguration.class))
                .thenReturn(configuration);

        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(appbaseCore.getReactorService()).thenReturn(reactorService);

        return appbaseCore;
    }

    private static AppbaseClientCore mockAppbaseClientCore(
            final DefaultAerospikeClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        final AppbaseCore appbaseCore = mockAppbaseCore(configuration);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);
        return appbaseClientCore;
    }

    private static DefaultAerospikeClientServiceConfiguration testConfiguration() {
        return DefaultAerospikeClientServiceConfiguration.builder()
                .autoStart(false)
                .reactiveOperationConfiguration(
                        DefaultReactiveOperationConfiguration.builder().metricsEnabled(false).build())
                .build();
    }

    /**
     * Build a {@link DefaultAerospikeClientService} using the pool-manager-accepting constructor with
     * a fully mocked {@link AppbaseClientPoolManager} and {@link AppbaseClientCore}. Since
     * {@code autoStart} is {@code false}, {@code startOnCreate()} never calls {@code start()}, so
     * {@code sanityCheck()}/the pool/{@code appbaseClientIntegrationServices(...)} are never touched --
     * this is pure in-memory constructor wiring, no live Aerospike cluster required.
     */
    private static DefaultAerospikeClientService wiredService(
            final DefaultAerospikeClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = mockAppbaseClientCore(configuration);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey =
                AppbaseClientUtil.clusterIdValueKey(
                        ClusterIdValueConfiguration.builder().clusterId("test-aerospike").build());
        final AppbaseClientPoolManager appbaseClientPoolManager = Mockito.mock(AppbaseClientPoolManager.class);

        return new DefaultAerospikeClientService(
                appbaseClientCore, defaultAppbaseClientKey, appbaseClientPoolManager, configuration, false, null);
    }

    /**
     * Spy {@code wiredService(configuration)} and stub its protected {@code buildReactorClient} seam to
     * return {@code mockReactorClient} instead of touching a real {@code IAerospikeClient}/{@code
     * EventLoops} pair -- this is what keeps {@link DefaultAerospikeClientService#getReactiveAccessor}'s
     * eager build unit-testable without a live cluster.
     */
    private static DefaultAerospikeClientService spiedService(
            final DefaultAerospikeClientServiceConfiguration configuration,
            final IAerospikeReactorClient mockReactorClient) {
        final DefaultAerospikeClientService spyService = Mockito.spy(wiredService(configuration));
        Mockito.doReturn(mockReactorClient).when(spyService).buildReactorClient(Mockito.any());
        return spyService;
    }

    @Test
    void getReactiveAccessorReturnsAccessorWithServiceConfig() {
        final IAerospikeReactorClient mockReactorClient = Mockito.mock(IAerospikeReactorClient.class);
        final DefaultAerospikeClientService service = spiedService(testConfiguration(), mockReactorClient);

        final ReactiveAerospikeAccessor accessor = service.getReactiveAccessor();

        Assertions.assertNotNull(accessor);
        Assertions.assertFalse(accessor.getReactiveOperationConfiguration().isMetricsEnabled());
        Assertions.assertEquals(
                service.getConfiguration().getReactiveOperationConfiguration().isMetricsEnabled(),
                accessor.getReactiveOperationConfiguration().isMetricsEnabled());
    }

    @Test
    void keyedGetReactiveAccessorReturnsAccessor() {
        final IAerospikeReactorClient mockReactorClient = Mockito.mock(IAerospikeReactorClient.class);
        final DefaultAerospikeClientService service = spiedService(testConfiguration(), mockReactorClient);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = service.getDefaultAppbaseClientKey();

        final ReactiveAerospikeAccessor accessor = service.getReactiveAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(accessor);
    }

    @Test
    void getReactiveAccessorCachesReactorClientPerKey() {
        final IAerospikeReactorClient mockReactorClient = Mockito.mock(IAerospikeReactorClient.class);
        final DefaultAerospikeClientService spyService = spiedService(testConfiguration(), mockReactorClient);

        final ReactiveAerospikeAccessor first = spyService.getReactiveAccessor();
        final ReactiveAerospikeAccessor second = spyService.getReactiveAccessor();

        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Mockito.verify(spyService, Mockito.times(1)).buildReactorClient(Mockito.any());
    }

    @Test
    void shutdownNeverClosesReactorClientAndClearsCacheSoNextAccessorRebuilds() throws IOException {
        final IAerospikeReactorClient mockReactorClient = Mockito.mock(IAerospikeReactorClient.class);
        final DefaultAerospikeClientService spyService = spiedService(testConfiguration(), mockReactorClient);

        Assertions.assertNotNull(spyService.getReactiveAccessor());
        Mockito.verify(spyService, Mockito.times(1)).buildReactorClient(Mockito.any());

        spyService.shutdown();

        Mockito.verify(mockReactorClient, Mockito.never()).close();

        Assertions.assertNotNull(spyService.getReactiveAccessor());
        Mockito.verify(spyService, Mockito.times(2)).buildReactorClient(Mockito.any());
    }

    @Test
    void getAccessorReturnsAccessorWithServiceConfig() {
        final IAerospikeReactorClient mockReactorClient = Mockito.mock(IAerospikeReactorClient.class);
        final DefaultAerospikeClientService service = spiedService(testConfiguration(), mockReactorClient);

        final AerospikeAccessor accessor = service.getAccessor();

        Assertions.assertNotNull(accessor);
    }

    @Test
    void keyedGetAccessorReturnsAccessor() {
        final IAerospikeReactorClient mockReactorClient = Mockito.mock(IAerospikeReactorClient.class);
        final DefaultAerospikeClientService service = spiedService(testConfiguration(), mockReactorClient);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = service.getDefaultAppbaseClientKey();

        final AerospikeAccessor accessor = service.getAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(accessor);
    }
}
