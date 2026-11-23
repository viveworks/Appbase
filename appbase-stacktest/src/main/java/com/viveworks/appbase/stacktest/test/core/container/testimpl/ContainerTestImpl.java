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
package com.viveworks.appbase.stacktest.test.core.container.testimpl;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.health.endpoint.servlet.HealthCheckServlet;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.container.jaxrs.ContainerJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.container.servlet.ContainerServletTest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * Smoke-tests that the selected servlet container is up and serving both
 * servlet and JAX-RS routes.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public class ContainerTestImpl extends AbstractTest {

    private final Class<?> testType;

    public ContainerTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);
        checkTestType(testType, ContainerJaxrsResourceTest.class, ContainerServletTest.class);
        this.testType = testType;
    }

    /**
     * Runs the appropriate container smoke test based on the test type (JAX-RS or servlet).
     *
     * @throws Exception on HTTP failure
     */
    public void containerTestImpl() throws Exception {
        final String testName = "containerTestImpl";
        startTest(testType, testName);

        if (ContainerJaxrsResourceTest.class.isAssignableFrom(testType)) {
            jaxrsTestImpl();
        } else {
            servletTestImpl();
        }

        endTest(testType, testName);
    }

    private void jaxrsTestImpl() throws Exception {
        final int statusCode = httpHead(
                baseUrl + AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH + "/overallStatus");
        checkEquals(
                HttpServletResponse.SC_OK,
                statusCode,
                "jaxrsTestImpl",
                "statusCode");
    }

    private void servletTestImpl() throws Exception {
        final HttpResponse response = executeHttpGet(baseUrl + HealthCheckServlet.DEFAULT_PATH + "?statusOnly=true");
        checkEquals(
                HttpServletResponse.SC_OK,
                response.getStatusLine().getStatusCode(),
                "servletTestImpl",
                "statusCode");
    }
}
