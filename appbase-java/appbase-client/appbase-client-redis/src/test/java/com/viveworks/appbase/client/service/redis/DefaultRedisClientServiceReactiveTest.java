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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientServiceConfiguration;
import com.viveworks.appbase.client.service.redis.reactive.ReactiveRedisAccessor;
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
import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.validation.Validator;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link DefaultRedisClientService#getReactiveAccessor()} and
 * {@link DefaultRedisClientService#getReactiveAccessor(AppbaseClientKey)}.
 *
 * <p>The {@code getReactiveAccessorReturnsAccessorWithServiceConfig} and
 * {@code keyedGetReactiveAccessorReturnsAccessorWithMockedPool} tests below are plain Mockito unit tests: they wire
 * {@link DefaultRedisClientService} via its pool-accepting constructor with a fully mocked
 * {@link AppbaseClientPool}/{@link AppbaseClientCore} and a configuration with {@code autoStart=false}, so
 * {@code start()}/{@code sanityCheck()} never run and no live Redis connection is attempted -- the pool is never
 * even invoked, since {@link DefaultRedisClientService#getReactiveAccessor()} only captures a lazy
 * {@code getClientIntegrationService} method reference that is invoked per-subscription, not at assembly time.
 *
 * <p>The remaining tests exercise the real end-to-end wiring against a running Redis instance. Enable via system
 * property: {@code -DRedisClientServiceTests.enabled=true}
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultRedisClientServiceReactiveTest {
    private static DefaultRedisClientService defaultRedisClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultRedisClientService.registerObjectMapperSubtypes();

        if (Boolean.getBoolean("RedisClientServiceTests.enabled")) {
            defaultRedisClientService = RedisClientServiceTestHelper.defaultRedisClientService();
        }
    }

    @AfterAll
    static void tearDown() {
        if (defaultRedisClientService != null) {
            defaultRedisClientService.shutdown();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "RedisClientServiceTests.enabled", matches = "true")
    void getReactiveAccessorReturnsConfiguredAccessor() {
        Assertions.assertNotNull(defaultRedisClientService);

        final ReactiveRedisAccessor reactiveRedisAccessor = defaultRedisClientService.getReactiveAccessor();

        Assertions.assertNotNull(reactiveRedisAccessor);
        Assertions.assertEquals(
                defaultRedisClientService.getConfiguration().getReactiveOperationConfiguration().isMetricsEnabled(),
                reactiveRedisAccessor.getReactiveOperationConfiguration().isMetricsEnabled());
    }

    @Test
    @EnabledIfSystemProperty(named = "RedisClientServiceTests.enabled", matches = "true")
    void keyedGetReactiveAccessorReturnsAccessor() {
        Assertions.assertNotNull(defaultRedisClientService);

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey =
                defaultRedisClientService.getDefaultAppbaseClientKey();

        final ReactiveRedisAccessor reactiveRedisAccessor =
                defaultRedisClientService.getReactiveAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(reactiveRedisAccessor);
    }

    // ── Non-gated unit tests: in-memory wiring only, no live Redis required ───────────────

    private static AppbaseCore mockAppbaseCore(final DefaultRedisClientServiceConfiguration configuration) {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultRedisClientServiceReactiveTest.class));

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
        Mockito.when(configurationService.getTypedConfiguration(DefaultRedisClientServiceConfiguration.class))
                .thenReturn(configuration);

        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(appbaseCore.getReactorService()).thenReturn(reactorService);

        return appbaseCore;
    }

    private static AppbaseClientCore mockAppbaseClientCore(final DefaultRedisClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        final AppbaseCore appbaseCore = mockAppbaseCore(configuration);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);
        return appbaseClientCore;
    }

    private static DefaultRedisClientServiceConfiguration testConfiguration() {
        return DefaultRedisClientServiceConfiguration.builder()
                .autoStart(false)
                .reactiveOperationConfiguration(
                        DefaultReactiveOperationConfiguration.builder().metricsEnabled(false).build())
                .build();
    }

    /**
     * Build a {@link DefaultRedisClientService} using the pool-accepting constructor with a fully mocked
     * {@link AppbaseClientPool} and {@link AppbaseClientCore}. Since {@code autoStart} is {@code false},
     * {@code startOnCreate()} never calls {@code start()}, so {@code sanityCheck()}/the pool are never touched --
     * this is pure in-memory constructor wiring.
     */
    @SuppressWarnings("unchecked")
    private static DefaultRedisClientService wiredService(final DefaultRedisClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = mockAppbaseClientCore(configuration);
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey =
                AppbaseClientUtil.textValueKey(new TextValue("test-redis"));
        final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool =
                Mockito.mock(AppbaseClientPool.class);

        return new DefaultRedisClientService(
                appbaseClientCore, defaultAppbaseClientKey, appbaseClientPool, false, null);
    }

    @Test
    void getReactiveAccessorReturnsAccessorWithServiceConfig() {
        final DefaultRedisClientService service = wiredService(testConfiguration());

        final ReactiveRedisAccessor accessor = service.getReactiveAccessor();

        Assertions.assertNotNull(accessor);
        Assertions.assertFalse(accessor.getReactiveOperationConfiguration().isMetricsEnabled());
        Assertions.assertEquals(
                service.getConfiguration().getReactiveOperationConfiguration().isMetricsEnabled(),
                accessor.getReactiveOperationConfiguration().isMetricsEnabled());
    }

    @Test
    void keyedGetReactiveAccessorReturnsAccessorWithMockedPool() {
        final DefaultRedisClientService service = wiredService(testConfiguration());
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = service.getDefaultAppbaseClientKey();

        final ReactiveRedisAccessor accessor = service.getReactiveAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(accessor);
    }

    @Test
    void getAccessorReturnsAccessorWithServiceConfig() {
        final DefaultRedisClientService service = wiredService(testConfiguration());

        final RedisAccessor accessor = service.getAccessor();

        Assertions.assertNotNull(accessor);
    }

    @Test
    void keyedGetAccessorReturnsAccessorWithMockedPool() {
        final DefaultRedisClientService service = wiredService(testConfiguration());
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = service.getDefaultAppbaseClientKey();

        final RedisAccessor accessor = service.getAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(accessor);
    }
}
