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

package com.viveworks.appbase.bootstrap.reactornetty.bootstrap;

import com.viveworks.appbase.bootstrap.reactornetty.endpoint.OffloadPolicy;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.ReactiveActivityFeedEndpoint;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.health.HealthCheckService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactorNettyEndpointRegistrarTest {

    /** Permissive default: unchanged behaviour for tests that do not exercise the policy itself. */
    private static final OffloadPolicy DEFAULT_POLICY = OffloadPolicy.of(1000, Duration.ofSeconds(30));

    private static final List<String> ALL_THIRTEEN_PATHS = List.of(
            "/health", "/metric", "/serviceRegistry", "/status",
            "/log", "/exception", "/configuration", "/core", "/jmx", "/mediator",
            AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH,
            AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH,
            ReactiveActivityFeedEndpoint.DEFAULT_PATH);

    @Test
    void buildRegistryRegistersAllThirteenRoutesWhenNothingIsExcluded() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);

        final AppbaseRouteRegistry registry = ReactorNettyEndpointRegistrar.buildRegistry(
                appbaseCore, healthCheckService, Schedulers.immediate(), DEFAULT_POLICY, Set.of());

        for (final String path : ALL_THIRTEEN_PATHS) {
            Assertions.assertNotNull(registry.lookup("GET", path), "expected a route for " + path);
        }
    }

    @Test
    void buildRegistryOmitsAnExcludedPathButKeepsTheOtherTwelve() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);

        final AppbaseRouteRegistry registry = ReactorNettyEndpointRegistrar.buildRegistry(
                appbaseCore, healthCheckService, Schedulers.immediate(), DEFAULT_POLICY, Set.of("/log"));

        Assertions.assertNull(registry.lookup("GET", "/log"));
        for (final String path : ALL_THIRTEEN_PATHS) {
            if ("/log".equals(path)) {
                continue;
            }
            Assertions.assertNotNull(registry.lookup("GET", path), "expected a route for " + path);
        }
    }

    @Test
    void buildRegistryRegistersTheTwoNativeUserStoreRoutes() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);

        final AppbaseRouteRegistry registry = ReactorNettyEndpointRegistrar.buildRegistry(
                appbaseCore, healthCheckService, Schedulers.immediate(), DEFAULT_POLICY, Set.of());

        Assertions.assertNotNull(
                registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH),
                "expected a route for " + AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH);
        Assertions.assertNotNull(
                registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH),
                "expected a route for " + AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH);
    }
}
