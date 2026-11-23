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
package com.viveworks.appbase.stacktest.test.core.testimpl;

import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.StatusJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.StatusServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class StatusEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public StatusEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, StatusJaxrsResourceTest.class, StatusServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH;
    }

    public void getOverallStatusTestImpl() throws Exception {
        final String testName = "getOverallStatusTest";
        startTest(testType, testName);

        final ServiceRegistry.OverallStatus overallStatus = serviceRegistry().overallStatus();
        checkTrue(overallStatus.isStarted(), testName, "overallStatus.started");

        final StatusRequest request = overallStatusRequest(isRestApiTest(), resourceUrl, servletUrl);
        if (request.head()) {
            final int headCode = httpCodeOfHead(request.url());
            checkEquals(HttpServletResponse.SC_OK, headCode, testName, "headCode");
        } else {
            // internal work item 1329: exercise the servlet endpoint over the wire, not just the in-process registry.
            // A single GET yields both the status and body while releasing the connection.
            final StatusAndBody get = httpGetStatusAndBody(request.url());
            checkEquals(HttpServletResponse.SC_OK, get.statusCode(), testName, "getCode");
            checkTrue(get.body() != null && !get.body().isBlank(), testName, "body.nonEmpty");
            JsonUtil.getJsonMapper().readTree(get.body());

            // StatusServlet.isHeadSupported() is false, so its base template returns 405.
            final int headCode = httpCodeOfHead(request.url());
            checkEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, headCode, testName, "headCode");
        }

        endTest(testType, testName);
    }

    public void pollAndGetOverallStatusTestImpl() throws Exception {
        final String testName = "pollAndGetOverallStatusTest";
        startTest(testType, testName);

        final ServiceRegistry.OverallStatus overallStatus = serviceRegistry().pollOverallStatus();
        checkTrue(overallStatus.isStarted(), testName, "overallStatus.started");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/pollOverallStatus";
        } else {
            url = servletUrl;
        }

        final int code = httpCodeOfPost(url);
        checkEquals(HttpServletResponse.SC_OK, code, testName, "code");

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private ServiceRegistry serviceRegistry() {
        return appbaseServices.getCoreServices().appbaseCore().getServiceRegistry();
    }

    private boolean isRestApiTest() {
        return StatusJaxrsResourceTest.class.isAssignableFrom(testType);
    }

    static StatusRequest overallStatusRequest(
            final boolean restApi, final String statusResourceUrl, final String statusServletUrl)
    {
        return restApi
            ? new StatusRequest(statusResourceUrl + "/overallStatus", true)
            : new StatusRequest(statusServletUrl, false);
    }

    record StatusRequest(String url, boolean head) {}
}
