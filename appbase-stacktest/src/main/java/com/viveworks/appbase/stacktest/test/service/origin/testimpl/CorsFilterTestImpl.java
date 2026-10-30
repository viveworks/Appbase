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

package com.viveworks.appbase.stacktest.test.service.origin.testimpl;

import com.viveworks.appbase.service.web.cors.CorsFilterConfiguration;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CorsFilterTestImpl extends AbstractTest {
    private final Class<?> testType;

    public CorsFilterTestImpl(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.testType = getClass();
    }

    public void filterTestImpl() throws Exception {
        final String testName = "filterTestImpl";

        startTest(testType, testName);

        final HttpResponse httpResponse1 = httpHeadResponse(baseUrl + StackTestConstants.CORS_TEST_RESOURCE_PATH);
        checkHttpResponse(httpResponse1);

        final HttpResponse httpResponse2 = httpHeadResponse(baseUrl + StackTestConstants.CORS_TEST_SERVLET_PATH);
        checkHttpResponse(httpResponse2);

        endTest(testType, testName);
    }

    private void checkHttpResponse(final HttpResponse httpResponse) {
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, "checkHttpResponse", "statusCode");

        final Header allowOrigin =
                httpResponse.getFirstHeader("Access-Control-Allow-Origin");
        checkNotNull(allowOrigin, "checkHttpResponse.originHeaders.notNull", "allowOrigin");
        checkEquals(
                CorsFilterConfiguration.Builder.DEFAULT_ALLOW_ORIGIN,
                allowOrigin.getValue(),
                "checkHttpResponse.originHeaders.equals",
                "allowOrigin");

        final Header allowMethods =
                httpResponse.getFirstHeader("Access-Control-Allow-Methods");
        checkNotNull(allowMethods, "checkHttpResponse.originHeaders.notNull", "allowMethods");
        checkEquals(
                CorsFilterConfiguration.Builder.DEFAULT_ALLOW_METHODS,
                allowMethods.getValue(),
                "checkHttpResponse.originHeaders.equals",
                "allowMethods");

        final Header allowHeaders =
                httpResponse.getFirstHeader("Access-Control-Allow-Headers");
        checkNotNull(allowHeaders, "checkHttpResponse.originHeaders.notNull", "allowHeaders");
        checkEquals(
                CorsFilterConfiguration.Builder.DEFAULT_ALLOW_HEADERS,
                allowHeaders.getValue(),
                "checkHttpResponse.originHeaders.equals",
                "allowHeaders");
    }

    @Path(StackTestConstants.CORS_TEST_RESOURCE_PATH)
    public static class CorsTestResource extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping() {
            return Response.noContent().build();
        }
    }

    public static class CorsTestServlet extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = 6857253239389563228L;

        @Override
        protected int headImpl(
                final String traceId,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse) throws Exception
        {
            return HttpServletResponse.SC_NO_CONTENT;
        }

        @Override
        protected boolean isHeadSupported() {
            return true;
        }

        @Override
        protected boolean isHeadResponseAutoHandled() {
            return true;
        }

        @Override
        protected boolean isGetSupported() {
            return false;
        }

        @Override
        protected boolean isPostSupported() {
            return false;
        }

        @Override
        protected boolean isPutSupported() {
            return false;
        }

        @Override
        protected boolean isDeleteSupported() {
            return false;
        }
    }
}
