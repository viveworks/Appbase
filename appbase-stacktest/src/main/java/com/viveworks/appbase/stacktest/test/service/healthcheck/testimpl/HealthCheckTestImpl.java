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
package com.viveworks.appbase.stacktest.test.service.healthcheck.testimpl;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.health.DefaultHealthCheckService;
import com.viveworks.appbase.service.health.DefaultHealthCheckServiceConfiguration;
import com.viveworks.appbase.service.health.HealthCheck;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.health.HealthChecker;
import com.viveworks.appbase.service.health.HealthChecks;
import com.viveworks.appbase.service.web.health.endpoint.servlet.HealthCheckServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.service.healthcheck.jaxrs.HealthCheckJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.service.healthcheck.servlet.HealthCheckServletTest;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class HealthCheckTestImpl extends AbstractTest {
    private final Class<?> testType;

    public HealthCheckTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        this.testType = testType;
        checkTestType(testType, HealthCheckJaxrsResourceTest.class, HealthCheckServletTest.class);
    }

    public void endpointTestImpl() throws Exception {
        final String testName = "filterTestImpl";

        startTest(testType, testName);

        final HealthCheckService healthCheckService = healthCheckService();

        try {
            if (isRestApiTest()) {
                runTests(healthCheckService, baseUrl + AppbaseServiceConstants.DEFAULT_HEALTH_CHECK_RESTAPI_PATH);
            } else {
                runTests(healthCheckService, baseUrl + HealthCheckServlet.DEFAULT_PATH);
            }
        } finally {
            healthCheckService.removeHealthChecker(DummyHealthChecker1.class);
            healthCheckService.removeHealthChecker(DummyHealthChecker2.class);
        }

        endTest(testType, testName);
    }

    private void runTests(final HealthCheckService healthCheckService, final String url) throws Exception {
        healthCheckService.addHealthChecker(new DummyHealthChecker1());

        try {
            Thread.sleep(2000);
            final HealthChecks healthChecks1 = httpGetJson(url, HealthChecks.class);
            checkEquals(true, healthChecks1.isHealthy(), "endpointTestImpl.1", "isHealthy");

            final HttpResponse httpResponse0 = executeHttpGet(url + "?statusOnly=true");
            checkEquals(
                    HttpServletResponse.SC_OK,
                    httpResponse0.getStatusLine().getStatusCode(),
                    "endpointTestImpl.2.1",
                    "isHealthy");

            healthCheckService.addHealthChecker(new DummyHealthChecker2());
            Thread.sleep(2000);
            final String url2 = url + "?force=true";
            final HttpResponse httpResponse1 = executeHttpGet(url2);
            checkEquals(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    httpResponse1.getStatusLine().getStatusCode(),
                    "endpointTestImpl.3", "isHealthy");
            Thread.sleep(1000);
            final HttpResponse httpResponse11 = executeHttpGet(url2 + "&statusOnly=true");
            checkEquals(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    httpResponse11.getStatusLine().getStatusCode(),
                    "endpointTestImpl.3.1", "isHealthy");

            healthCheckService.removeHealthChecker(DummyHealthChecker2.class);

            Thread.sleep(2000);
            final HttpResponse httpResponse2 = executeHttpGet(url2);
            checkEquals(
                    HttpServletResponse.SC_OK,
                    httpResponse2.getStatusLine().getStatusCode(),
                    "endpointTestImpl.4", "isHealthy");
            final HealthChecks healthChecks3 = httpGetJson(url2, HealthChecks.class);
            checkEquals(true, healthChecks3.isHealthy(), "endpointTestImpl.5", "isHealthy");
            Thread.sleep(1000);
            final HttpResponse httpResponse3 = executeHttpGet(url2 + "&statusOnly=true");
            checkEquals(
                    HttpServletResponse.SC_OK,
                    httpResponse3.getStatusLine().getStatusCode(),
                    "endpointTestImpl.4.1", "isHealthy");
        } finally {
            healthCheckService.removeHealthChecker(DummyHealthChecker1.class);
            healthCheckService.removeHealthChecker(DummyHealthChecker2.class);
        }
    }

    private boolean isRestApiTest() {
        return HealthCheckJaxrsResourceTest.class.isAssignableFrom(testType);
    }

    private HealthCheckService healthCheckService() {
        final AppbaseCore appbaseCore = appbaseServices.getCoreServices().appbaseCore();
        final AppbaseServiceEntry appbaseServiceEntry =
                appbaseCore.getServiceRegistry().getAppbaseService(HealthCheckService.class);
        if (appbaseServiceEntry == null) {
            return newHealthCheckService();
        }

        final HealthCheckService healthCheckService = (HealthCheckService) appbaseServiceEntry.getService();
        if (healthCheckService == null) {
            return newHealthCheckService();
        }

        return healthCheckService;
    }

    private HealthCheckService newHealthCheckService() {
        final AppbaseCore appbaseCore = appbaseServices.getCoreServices().appbaseCore();
        return new DefaultHealthCheckService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultHealthCheckServiceConfiguration.builder().build());
    }

    public static class DummyHealthChecker1 implements HealthChecker {
        @Override
        public HealthCheck runHealthCheck() {
            return new HealthCheck(DummyHealthChecker1.class.getName(), true);
        }
    }

    public static class DummyHealthChecker2 implements HealthChecker {
        @Override
        public HealthCheck runHealthCheck() {
            return new HealthCheck(DummyHealthChecker2.class.getName(), false);
        }
    }
}
