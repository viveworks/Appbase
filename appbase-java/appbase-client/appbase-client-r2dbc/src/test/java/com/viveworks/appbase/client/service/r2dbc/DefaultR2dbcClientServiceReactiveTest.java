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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.r2dbc.accessor.reactive.ReactiveR2dbcAccessor;
import com.viveworks.appbase.client.service.r2dbc.configuration.DefaultR2dbcClientServiceConfiguration;
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
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link DefaultR2dbcClientService#getReactiveAccessor()} and
 * {@link DefaultR2dbcClientService#getReactiveAccessor(AppbaseClientKey)}.
 *
 * <p>These are plain Mockito unit tests: they wire {@link DefaultR2dbcClientService} via its
 * pool-manager-accepting constructor with a fully mocked {@link AppbaseClientPoolManager}/
 * {@link AppbaseClientCore} and a configuration with {@code autoStart=false}, so {@code start()}/
 * {@code sanityCheck()} never run and no live R2DBC connection is attempted -- the pool is never
 * even invoked, since {@link DefaultR2dbcClientService#getReactiveAccessor()} only captures a
 * lazy {@code getClientIntegrationService} method reference that is invoked per-subscription, not
 * at assembly time.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultR2dbcClientServiceReactiveTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultR2dbcClientService.registerObjectMapperSubtypes();
    }

    private static AppbaseCore mockAppbaseCore(final DefaultR2dbcClientServiceConfiguration configuration) {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultR2dbcClientServiceReactiveTest.class));

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
        Mockito.when(configurationService.getTypedConfiguration(DefaultR2dbcClientServiceConfiguration.class))
                .thenReturn(configuration);

        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(appbaseCore.getReactorService()).thenReturn(reactorService);

        return appbaseCore;
    }

    private static AppbaseClientCore mockAppbaseClientCore(final DefaultR2dbcClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        final AppbaseCore appbaseCore = mockAppbaseCore(configuration);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);
        return appbaseClientCore;
    }

    private static DefaultR2dbcClientServiceConfiguration testConfiguration() {
        return DefaultR2dbcClientServiceConfiguration.builder()
                .autoStart(false)
                .reactiveOperationConfiguration(
                        DefaultReactiveOperationConfiguration.builder().metricsEnabled(false).build())
                .build();
    }

    /**
     * Build a {@link DefaultR2dbcClientService} using the pool-manager-accepting constructor
     * with a fully mocked {@link AppbaseClientPoolManager} and {@link AppbaseClientCore}. Since
     * {@code autoStart} is {@code false}, {@code startOnCreate()} never calls {@code start()}, so
     * {@code sanityCheck()}/the pool are never touched -- this is pure in-memory constructor
     * wiring.
     */
    private static DefaultR2dbcClientService wiredService(final DefaultR2dbcClientServiceConfiguration configuration) {
        final AppbaseClientCore appbaseClientCore = mockAppbaseClientCore(configuration);
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey =
                AppbaseClientUtil.textValueKey(new TextValue("test-r2dbc"));
        final AppbaseClientPoolManager appbaseClientPoolManager = Mockito.mock(AppbaseClientPoolManager.class);

        return new DefaultR2dbcClientService(
                appbaseClientCore, defaultAppbaseClientKey, appbaseClientPoolManager, configuration, false, null);
    }

    @Test
    void getReactiveAccessorReturnsAccessorWithServiceConfig() {
        final DefaultR2dbcClientService service = wiredService(testConfiguration());

        final ReactiveR2dbcAccessor accessor = service.getReactiveAccessor();

        Assertions.assertNotNull(accessor);
        Assertions.assertFalse(accessor.getReactiveOperationConfiguration().isMetricsEnabled());
        Assertions.assertEquals(
                service.getConfiguration().getReactiveOperationConfiguration().isMetricsEnabled(),
                accessor.getReactiveOperationConfiguration().isMetricsEnabled());
    }

    @Test
    void keyedGetReactiveAccessorReturnsAccessorWithMockedPool() {
        final DefaultR2dbcClientService service = wiredService(testConfiguration());
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = service.getDefaultAppbaseClientKey();

        final ReactiveR2dbcAccessor accessor = service.getReactiveAccessor(defaultAppbaseClientKey);

        Assertions.assertNotNull(accessor);
    }
}
