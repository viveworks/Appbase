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

package com.viveworks.appbase.bootstrap.reactornetty.bootstrap;

import com.viveworks.appbase.bootstrap.reactornetty.endpoint.NativeEndpoints;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.NativeExceptionEndpoint;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.NativeLogEndpoint;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.NativeUserAccountEndpoint;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.NativeUserSessionEndpoint;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.OffloadPolicy;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.OffloadedEndpoints;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.ReactiveActivityFeedEndpoint;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.health.HealthCheckService;
import reactor.core.scheduler.Scheduler;

import java.util.Set;

/**
 * Builds a fresh {@link AppbaseRouteRegistry} carrying the thirteen ported reactive routes -- the four
 * native routes from {@link NativeEndpoints}, the native {@code /log} and {@code /exception} routes
 * from {@link NativeLogEndpoint} and {@link NativeExceptionEndpoint}, the two native reactive
 * user-store routes from {@link NativeUserSessionEndpoint} and {@link NativeUserAccountEndpoint}, and
 * the four remaining offloaded routes from {@link OffloadedEndpoints} -- for a later {@code Builder}
 * (Task 4) to hand to the server.
 *
 * <p><b>Excludes mechanism (pre-resolved design).</b> {@link NativeEndpoints#register} and
 * {@link OffloadedEndpoints#register} are each monolithic -- they register their whole family of
 * routes in one call -- so a caller cannot skip one path mid-registration. Rather than split those
 * methods to accept a per-route filter, this class registers everything first and then removes
 * each excluded {@code GET} route from the populated registry via
 * {@link AppbaseRouteRegistry#remove(String, String)}. This keeps {@code NativeEndpoints} and
 * {@code OffloadedEndpoints} focused solely on wiring one route family each, and puts the
 * cross-cutting "which routes does this deployment actually want" decision in exactly one place.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ReactorNettyEndpointRegistrar {

    private ReactorNettyEndpointRegistrar() {
    }

    /**
     * Builds and returns a new, fully populated {@link AppbaseRouteRegistry}.
     *
     * @param appbaseCore Appbase core, passed through to both route families
     * @param healthCheckService health service, passed through to {@link NativeEndpoints}
     * @param offloadScheduler bounded scheduler, passed through to {@link OffloadedEndpoints}
     * @param policy the page-size cap and per-read timeout; {@code maxPageSize} is passed through
     *     to {@link NativeLogEndpoint}, the whole policy to {@link OffloadedEndpoints}
     * @param excludedPaths {@code GET} paths to leave out of the returned registry; non-null, may
     *     be empty. A path not among the thirteen registered routes is silently ignored.
     * @return a new registry carrying the thirteen routes minus any named in {@code excludedPaths}
     */
    public static AppbaseRouteRegistry buildRegistry(
            final AppbaseCore appbaseCore,
            final HealthCheckService healthCheckService,
            final Scheduler offloadScheduler,
            final OffloadPolicy policy,
            final Set<String> excludedPaths) {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(healthCheckService, "healthCheckService");
        AppbaseUtil.notNull(offloadScheduler, "offloadScheduler");
        AppbaseUtil.notNull(policy, "policy");
        AppbaseUtil.notNull(excludedPaths, "excludedPaths");

        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        NativeEndpoints.register(registry, appbaseCore, healthCheckService);
        NativeLogEndpoint.register(registry, appbaseCore, policy.maxPageSize());
        NativeExceptionEndpoint.register(registry, appbaseCore);
        NativeUserSessionEndpoint.register(registry, appbaseCore);
        NativeUserAccountEndpoint.register(registry, appbaseCore);
        ReactiveActivityFeedEndpoint.register(registry, appbaseCore, policy.maxPageSize());
        OffloadedEndpoints.register(registry, appbaseCore, offloadScheduler, policy);

        for (final String excludedPath : excludedPaths) {
            registry.remove("GET", excludedPath);
        }

        return registry;
    }
}
