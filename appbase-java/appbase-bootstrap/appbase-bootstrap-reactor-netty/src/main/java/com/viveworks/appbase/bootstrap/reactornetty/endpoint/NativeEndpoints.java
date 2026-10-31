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

package com.viveworks.appbase.bootstrap.reactornetty.endpoint;

import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.metric.microprofile.jsonserializer.MetricRegistryJsonSerializer;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringServiceStatusValue;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.health.HealthChecks;
import com.viveworks.appbase.service.web.health.endpoint.servlet.HealthCheckServlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The four endpoints that touch only in-memory state, and therefore run DIRECTLY on the event loop
 * with no scheduler offload.
 *
 * <p>Every other ported endpoint performs blocking I/O (JDBC, ZooKeeper, S3, Aerospike, ...) and
 * belongs on a bounded elastic scheduler instead -- that offloaded family is out of scope for this
 * task. The split is not stylistic: running blocking work on an event-loop thread would stall every
 * connection that thread multiplexes, which is precisely what this module exists to prevent.
 *
 * <p><b>Read-only scope (deliberate).</b> Each of the four servlets this class mirrors also carries
 * mutating or administrative operations reached through the SAME path via an {@code operation} query
 * parameter dispatched onto a JMX MXBean -- e.g. {@code ServiceRegistryServlet}'s {@code reload},
 * {@code shutdown}, {@code unregisterAppbaseService}, {@code clearAppbaseServices};
 * {@code MetricServlet}'s POST/PUT/DELETE operations; {@code StatusServlet}'s {@code doPost} (which
 * forces a poll, i.e. re-runs checks). None of those are pure in-memory reads, several are
 * destructive, and JMX permission checks are no small addition to reproduce faithfully. This class
 * registers ONLY the read half of each path; the servlets remain the sole way to reach the mutating
 * operations until they get a proper reactive treatment (tracked as P-039, targeted 2.0.0).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class NativeEndpoints {

    private NativeEndpoints() {
    }

    /**
     * Registers the four natively non-blocking, read-only routes on {@code registry}.
     *
     * @param registry route table to populate
     * @param appbaseCore Appbase core, providing the metric service and the service registry
     * @param healthCheckService health service, read via its non-blocking snapshot
     */
    public static void register(
            final AppbaseRouteRegistry registry,
            final AppbaseCore appbaseCore,
            final HealthCheckService healthCheckService) {
        AppbaseUtil.notNull(registry, "registry");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(healthCheckService, "healthCheckService");

        registry.route("GET", HealthCheckServlet.DEFAULT_PATH, exchange -> {
            // checkHealthSnapshot() is a pure AtomicReference read (Task 2): no lock, no I/O, no
            // checker run. Calling checkHealth(true) (or checkHealth()) here would block the event
            // loop on S3, ZooKeeper, Aerospike and two JDBC round-trips -- exactly what BlockHound
            // exists to catch. See P-027 R1 spec section 5.2.
            final HealthChecks healthChecks = healthCheckService.checkHealthSnapshot();
            // 503 (not the servlet's 500) is intentional -- 503 Service Unavailable is the REST
            // convention for an unhealthy health check.
            exchange.status(healthChecks.isHealthy() ? 200 : 503);
            return exchange.writeJson(healthChecks);
        });

        registry.route("GET", AppbaseServiceConstants.DEFAULT_METRIC_SERVLET_PATH, exchange ->
                // Mirrors DefaultMetricService#serializeMetricRegistryAsJson(): the servlet's
                // "serializeMetricRegistryAsJson" operation wraps the MicroProfile MetricRegistry in
                // this same encoder before handing it to Jackson. Walking the registry is an
                // in-memory map read; no I/O, no lock.
                // In sync with MetricServlet: metric GET reads are intentionally NOT JMX-permission-gated
                // (only unregister/clear/reload/shutdown are); reads are open and the real access control
                // is the HTTP-layer authorization filter chain, applied on every face.
                exchange.writeJson(
                        new MetricRegistryJsonSerializer(appbaseCore.getMetricService().getMetricRegistry())));

        registry.route("GET", AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_SERVLET_PATH, exchange -> {
            // Mirrors ServiceRegistryServlet's "appbaseServiceStatuses" operation (GET ?operation=
            // appbaseServiceStatuses -> JmxServiceRegistryMXBean#getAllServiceStatusByServiceTypeName()),
            // rebuilt directly against ServiceRegistry rather than through the JMX MXBean: the JMX
            // layer adds a permission check this native read does not need, and the underlying
            // read -- getAppbaseServices() plus a per-entry getServiceStatus() -- is already an
            // in-memory collection walk over volatile field reads.
            final Collection<AppbaseServiceEntry> appbaseServiceEntries =
                    appbaseCore.getServiceRegistry().getAppbaseServices();
            final Map<String, ServiceStatus> serviceStatusByNameMap = new HashMap<>();
            if (appbaseServiceEntries != null) {
                appbaseServiceEntries.forEach(appbaseServiceEntry -> {
                    final AppbaseService service = appbaseServiceEntry.getService();
                    final ServiceStatus serviceStatus = appbaseServiceEntry.getServiceStatus();
                    if (service != null && serviceStatus != null) {
                        serviceStatusByNameMap.put(service.getClass().getName(), serviceStatus);
                    }
                });
            }
            return exchange.writeJson(new MapOfStringServiceStatusValue(serviceStatusByNameMap));
        });

        registry.route("GET", AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH, exchange -> {
            // StatusServlet answers this same read on HTTP HEAD (doHead -> overallStatusJson()),
            // an unusual verb choice for a read; Task 4's route table is GET-only by construction,
            // so the read is exposed here as GET instead -- same body, same 200/503 status
            // semantics (OverallStatus#isStarted()), different verb. overallStatus() is an
            // in-memory registry read followed by Jackson serialisation, no I/O.
            final ServiceRegistry.OverallStatus overallStatus = appbaseCore.getServiceRegistry().overallStatus();
            exchange.status(overallStatus.isStarted() ? 200 : 503);
            return exchange.writeJson(overallStatus);
        });
    }
}
