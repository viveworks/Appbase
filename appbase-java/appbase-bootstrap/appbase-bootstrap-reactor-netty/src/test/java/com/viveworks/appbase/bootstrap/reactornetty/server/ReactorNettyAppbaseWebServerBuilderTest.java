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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.user.account.web.filter.reactive.IdpUserTokenCheckWebFilter;
import com.viveworks.appbase.service.user.session.web.filter.reactive.UserSessionWebFilter;
import com.viveworks.appbase.service.web.reactive.AuthorizationWebFilter;
import com.viveworks.appbase.service.web.reactive.CorsWebFilter;
import com.viveworks.appbase.service.web.reactive.ExceptionHandlerWebFilter;
import com.viveworks.appbase.service.web.reactive.MetricWebFilter;
import com.viveworks.appbase.service.web.reactive.TraceIdWebFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * Tests {@link ReactorNettyAppbaseWebServer.Builder}, the production wiring that assembles a runnable
 * reactive server from an {@link AppbaseServices} container.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactorNettyAppbaseWebServerBuilderTest {

    private static final List<String> ALL_TEN_PATHS = List.of(
            "/health", "/metric", "/serviceRegistry", "/status",
            "/log", "/exception", "/configuration", "/core", "/jmx", "/mediator");

    private static AppbaseServices stubServices() throws Exception {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        // MXBean registration is idempotent and guarded by isMXBeanRegistered(...): reporting every
        // bean as already registered makes registerMXBeans a no-op, so no real Jmx* object is ever
        // constructed against the mock core.
        Mockito.when(appbaseCore.getJmxService().isMXBeanRegistered(Mockito.any())).thenReturn(true);

        final HealthCheckService healthCheckService = Mockito.mock(HealthCheckService.class);

        final AppbaseCoreServices coreServices = Mockito.mock(AppbaseCoreServices.class);
        Mockito.when(coreServices.appbaseCore()).thenReturn(appbaseCore);

        final AppbaseServices services = Mockito.mock(AppbaseServices.class);
        Mockito.when(services.getCoreServices()).thenReturn(coreServices);
        Mockito.when(services.get(HealthCheckService.class)).thenReturn(healthCheckService);
        return services;
    }

    @Test
    void buildRegistersAllTenReactiveRoutes() throws Exception {
        final ReactorNettyAppbaseWebServer server =
                ReactorNettyAppbaseWebServer.builder().from(stubServices()).build();

        for (final String path : ALL_TEN_PATHS) {
            Assertions.assertNotNull(server.routeRegistry().lookup("GET", path),
                    "expected a GET route for " + path);
        }
    }

    @Test
    void buildUsesTheConfiguredSingletonJsonMapper() throws Exception {
        final ReactorNettyAppbaseWebServer server =
                ReactorNettyAppbaseWebServer.builder().from(stubServices()).build();

        Assertions.assertSame(JsonUtil.getJsonMapper(), server.objectMapper());
    }

    @Test
    void buildAssemblesTheSevenFiltersInTheInvariantOrderWithAuthorizationLast() throws Exception {
        final ReactorNettyAppbaseWebServer server =
                ReactorNettyAppbaseWebServer.builder().from(stubServices()).build();

        final List<AppbaseServiceFilter> filters = server.filters();

        Assertions.assertEquals(7, filters.size(), "expected exactly the seven reactive filters");
        Assertions.assertInstanceOf(ExceptionHandlerWebFilter.class, filters.get(0));
        Assertions.assertInstanceOf(CorsWebFilter.class, filters.get(1));
        Assertions.assertInstanceOf(MetricWebFilter.class, filters.get(2));
        Assertions.assertInstanceOf(TraceIdWebFilter.class, filters.get(3));
        Assertions.assertInstanceOf(UserSessionWebFilter.class, filters.get(4));
        Assertions.assertInstanceOf(IdpUserTokenCheckWebFilter.class, filters.get(5));
        Assertions.assertInstanceOf(AuthorizationWebFilter.class, filters.get(6));
    }

    @Test
    void authorizationHasTheHighestFilterOrderSoItDispatchesLast() throws Exception {
        final ReactorNettyAppbaseWebServer server =
                ReactorNettyAppbaseWebServer.builder().from(stubServices()).build();

        final List<AppbaseServiceFilter> filters = server.filters();

        // List position (asserted above) does not govern dispatch order: DefaultAppbaseServiceFilterChain
        // re-sorts the assembled filters ascending by order() before running them. The security
        // invariant is that AuthorizationWebFilter's RESOLVED order is strictly greater than every
        // other assembled filter's order, so it is necessarily last after that sort -- regardless of
        // the list position the builder happened to assemble it at.
        final AppbaseServiceFilter authorizationFilter = filters.stream()
                .filter(AuthorizationWebFilter.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected an AuthorizationWebFilter among the assembled filters"));

        for (final AppbaseServiceFilter filter : filters) {
            if (filter == authorizationFilter) {
                continue;
            }
            Assertions.assertTrue(authorizationFilter.order() > filter.order(),
                    "expected authorization's order (" + authorizationFilter.order()
                            + ") to exceed " + filter.getClass().getSimpleName()
                            + "'s order (" + filter.order() + ")");
        }
    }
}
