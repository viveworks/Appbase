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

import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.metric.microprofile.jsonserializer.MetricRegistryJsonSerializer;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.service.appbase.data.MapOfStringServiceStatusValue;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.health.HealthChecks;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NativeEndpointsTest {

    @Test
    void healthRouteServesTheSnapshotAndNeverForcesARun() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);
        Mockito.when(healthCheckService.checkHealthSnapshot())
                .thenReturn(new HealthChecks(true, List.of(), List.of(), 5L));

        NativeEndpoints.register(registry, core, healthCheckService);

        Assertions.assertNotNull(registry.lookup("GET", "/health"));

        registry.lookup("GET", "/health").handle(new FakeExchange("GET", "/health")).block();

        Mockito.verify(healthCheckService).checkHealthSnapshot();
        Mockito.verify(healthCheckService, Mockito.never()).checkHealth(true);
        Mockito.verify(healthCheckService, Mockito.never()).checkHealth();
    }

    @Test
    void allFourNativeRoutesAreRegistered() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);

        NativeEndpoints.register(registry, core, healthCheckService);

        Assertions.assertNotNull(registry.lookup("GET", "/health"));
        Assertions.assertNotNull(registry.lookup("GET", "/metric"));
        Assertions.assertNotNull(registry.lookup("GET", "/serviceRegistry"));
        Assertions.assertNotNull(registry.lookup("GET", "/status"));
    }

    @Test
    void healthReturns503WhenTheSnapshotIsUnhealthy() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);
        Mockito.when(healthCheckService.checkHealthSnapshot())
                .thenReturn(new HealthChecks(false, List.of(), List.of(), 5L));

        NativeEndpoints.register(registry, core, healthCheckService);

        final FakeExchange exchange = new FakeExchange("GET", "/health");
        registry.lookup("GET", "/health").handle(exchange).block();

        Assertions.assertEquals(503, exchange.status());
    }

    @Test
    void statusRouteReturns200WhenTheServiceRegistryIsStarted() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);
        Mockito.when(core.getServiceRegistry().overallStatus().isStarted()).thenReturn(true);

        NativeEndpoints.register(registry, core, healthCheckService);

        final FakeExchange exchange = new FakeExchange("GET", "/status");
        registry.lookup("GET", "/status").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertNotNull(exchange.body());
    }

    @Test
    void statusRouteReturns503WhenTheServiceRegistryIsNotStarted() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);
        Mockito.when(core.getServiceRegistry().overallStatus().isStarted()).thenReturn(false);

        NativeEndpoints.register(registry, core, healthCheckService);

        final FakeExchange exchange = new FakeExchange("GET", "/status");
        registry.lookup("GET", "/status").handle(exchange).block();

        Assertions.assertEquals(503, exchange.status());
    }

    @Test
    void serviceRegistryRouteWritesTheServiceStatusMap() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);

        final AppbaseServiceEntry appbaseServiceEntry = Mockito.mock(AppbaseServiceEntry.class);
        final AppbaseService appbaseService = Mockito.mock(AppbaseService.class);
        final ServiceStatus serviceStatus = Mockito.mock(ServiceStatus.class);
        Mockito.when(appbaseServiceEntry.getService()).thenReturn(appbaseService);
        Mockito.when(appbaseServiceEntry.getServiceStatus()).thenReturn(serviceStatus);
        Mockito.when(core.getServiceRegistry().getAppbaseServices()).thenReturn(List.of(appbaseServiceEntry));

        NativeEndpoints.register(registry, core, healthCheckService);

        final FakeExchange exchange = new FakeExchange("GET", "/serviceRegistry");
        registry.lookup("GET", "/serviceRegistry").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertTrue(exchange.body() instanceof MapOfStringServiceStatusValue);
    }

    @Test
    void metricRouteWritesTheMetricRegistrySerializer() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);

        NativeEndpoints.register(registry, core, healthCheckService);

        final FakeExchange exchange = new FakeExchange("GET", "/metric");
        registry.lookup("GET", "/metric").handle(exchange).block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertTrue(exchange.body() instanceof MetricRegistryJsonSerializer);
    }
}
