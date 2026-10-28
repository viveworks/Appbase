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

package com.viveworks.appbase.service.health;

import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Builds a {@link DefaultHealthCheckService} whose scheduled executor never fires, so a test owns
 * every checker invocation, and whose clock is injected, so staleness is deterministic.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class HealthCheckServiceTestHelper {

    private HealthCheckServiceTestHelper() {
    }

    static DefaultHealthCheckService serviceWith(final HealthChecker healthChecker, final LongSupplier nowMillis) {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(HealthCheckServiceTestHelper.class));

        // AbstractAppbaseService's constructor eagerly wires reload/shutdown timers and error
        // counters off getMetricRegistry(); CALLS_REAL_METHODS lets MetricService#name(...)
        // (a default method) run for real while everything else stays a plain mock.
        final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
        final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
        final Timer timer = Mockito.mock(Timer.class);
        final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
        final Counter counter = Mockito.mock(Counter.class);
        Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
        Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(counter);
        Mockito.when(timer.time()).thenReturn(timerContext);

        final MediatorService mediatorService = Mockito.mock(MediatorService.class, Mockito.RETURNS_DEEP_STUBS);
        final ServiceRegistry serviceRegistry = Mockito.mock(ServiceRegistry.class);

        // A scheduled executor that accepts the task and never runs it.
        final ScheduledExecutorService neverFires = Mockito.mock(ScheduledExecutorService.class);

        final DefaultHealthCheckServiceConfiguration configuration =
                DefaultHealthCheckServiceConfiguration.builder()
                        .cacheEnabled(true)
                        .cacheSchedulerDelayConfiguration(TimeUnitConfiguration.builder()
                                .enabled(true).time(30).timeUnit(TimeUnit.SECONDS).build())
                        .staleAfterConfiguration(TimeUnitConfiguration.builder()
                                .enabled(true).time(90).timeUnit(TimeUnit.SECONDS).build())
                        .build();

        final DefaultHealthCheckService service = new DefaultHealthCheckService(
                logService, metricService, mediatorService, serviceRegistry, neverFires, configuration, nowMillis);
        service.addHealthChecker(healthChecker);
        return service;
    }
}
