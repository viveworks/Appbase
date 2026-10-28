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

package com.viveworks.appbase.stacktest.test.service.traceid.testimpl;

import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.service.traceid.jaxrs.TraceIdJaxrsFilterTest;
import com.viveworks.appbase.stacktest.test.service.traceid.servlet.TraceIdServletFilterTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.MDC;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class TraceIdFilterTestImpl extends AbstractTest {
    private final Class<?> testType;

    public TraceIdFilterTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        this.testType = testType;
        checkTestType(testType, TraceIdJaxrsFilterTest.class, TraceIdServletFilterTest.class);
    }

    public void filterTestImpl() throws Exception {
        final String testName = "filterTestImpl";

        startTest(testType, testName);

        traceIdCookieTest();
        traceIdHeaderTest();
        traceIdQueryParamTest();
        serverTraceId();

        endTest(testType, testName);
    }

    private void serverTraceId() throws Exception {
        if (isRestApiTest()) {
            final HttpResponse httpResponse =
                    httpHeadResponse(baseUrl + StackTestConstants.TRACE_ID_TEST_RESOURCE_PATH);
            checkHttpResponse(httpResponse, null);
        } else {
            final HttpResponse httpResponse =
                    httpHeadResponse(baseUrl + StackTestConstants.TRACE_ID_TEST_SERVLET_PATH);
            checkHttpResponse(httpResponse, null);
        }
    }

    private void traceIdCookieTest() throws Exception {
        final String traceId = UUID.randomUUID().toString();

        final BasicClientCookie cookie = addCookie(TracerService.TRACE_ID_KEY, traceId);
        try {
            if (isRestApiTest()) {
                final HttpResponse httpResponse =
                        httpHeadResponse(baseUrl + StackTestConstants.TRACE_ID_TEST_RESOURCE_PATH);
                checkHttpResponse(httpResponse, traceId);
            } else {
                final HttpResponse httpResponse =
                        httpHeadResponse(baseUrl + StackTestConstants.TRACE_ID_TEST_SERVLET_PATH);
                checkHttpResponse(httpResponse, traceId);
            }
        } finally {
            clearCookies(cookie);
        }
    }

    private void traceIdHeaderTest() throws Exception {
        final String traceId = UUID.randomUUID().toString();

        if (isRestApiTest()) {
            final HttpResponse httpResponse = httpHeadResponse(
                    baseUrl + StackTestConstants.TRACE_ID_TEST_RESOURCE_PATH,
                    Map.of(TracerService.TRACE_ID_KEY, traceId));
            checkHttpResponse(httpResponse, traceId);
        } else {
            final HttpResponse httpResponse = httpHeadResponse(
                    baseUrl + StackTestConstants.TRACE_ID_TEST_SERVLET_PATH,
                    Map.of(TracerService.TRACE_ID_KEY, traceId));
            checkHttpResponse(httpResponse, traceId);
        }
    }

    private void traceIdQueryParamTest() throws Exception {
        final String traceId = UUID.randomUUID().toString();
        final String queryParam = "?" + TracerService.TRACE_ID_KEY + "=" + traceId;

        if (isRestApiTest()) {
            final HttpResponse httpResponse =
                    httpHeadResponse(baseUrl + StackTestConstants.TRACE_ID_TEST_RESOURCE_PATH + queryParam);
            checkHttpResponse(httpResponse, traceId);
        } else {
            final HttpResponse httpResponse =
                    httpHeadResponse(baseUrl + StackTestConstants.TRACE_ID_TEST_SERVLET_PATH + queryParam);
            checkHttpResponse(httpResponse, traceId);
        }
    }

    private void checkHttpResponse(final HttpResponse httpResponse, @Nullable final String traceId) {
        checkEquals(
                HttpServletResponse.SC_NO_CONTENT,
                httpResponse.getStatusLine().getStatusCode(),
                "filterTestImpl",
                "code");
        final Header traceIdHeader = httpResponse.getFirstHeader(TracerService.TRACE_ID_KEY);
        checkNotNull(traceIdHeader, "checkHttpResponse", "traceIdHeader");

        final String headerTraceId = traceIdHeader.getValue();
        checkNotNull(headerTraceId, "checkHttpResponse", "headerTraceId");

        if (traceId != null) {
            checkEquals(headerTraceId, traceId, "checkHttpResponse", "traceId");
        }
    }

    private boolean isRestApiTest() {
        return TraceIdJaxrsFilterTest.class.isAssignableFrom(testType);
    }

    @Path(StackTestConstants.TRACE_ID_TEST_RESOURCE_PATH)
    public static class TraceIdTestResource extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping(
                @Nullable
                @CookieParam(TracerService.TRACE_ID_KEY)
                final String cookieTraceId,
                @Nullable
                @HeaderParam(TracerService.TRACE_ID_KEY)
                final String headerTraceId,
                @Nullable
                @QueryParam(TracerService.TRACE_ID_KEY)
                final String queryParamTraceId)
        {
            if (!AppbaseUtil.isBlank(cookieTraceId)) {
                return checkTraceId(cookieTraceId);
            }

            if (!AppbaseUtil.isBlank(headerTraceId)) {
                return checkTraceId(headerTraceId);
            }

            if (!AppbaseUtil.isBlank(queryParamTraceId)) {
                return checkTraceId(queryParamTraceId);
            }

            final String mdcTraceId = MDC.get(TracerService.TRACE_ID_KEY);
            if (mdcTraceId == null) {
                return Response.serverError().build();
            }

            return Response.noContent().build();
        }

        private Response checkTraceId(final String requestTraceId) {
            final String mdcTraceId = MDC.get(TracerService.TRACE_ID_KEY);
            if (mdcTraceId == null) {
                return Response.serverError().build();
            }

            if (!mdcTraceId.equals(requestTraceId)) {
                return Response.serverError().build();
            }

            return Response.noContent().build();
        }
    }

    public static class TraceIdTestServlet extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = -3725764712554116471L;

        @Override
        protected int headImpl(
                final String traceId,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse) throws Exception
        {
            final String cookieTraceId = AppbaseServiceUtil.cookie(TracerService.TRACE_ID_KEY, httpServletRequest);
            if (!AppbaseUtil.isBlank(cookieTraceId)) {
                return checkTraceId(cookieTraceId);
            }

            final String headerTraceId = AppbaseServiceUtil.header(TracerService.TRACE_ID_KEY, httpServletRequest);
            if (!AppbaseUtil.isBlank(headerTraceId)) {
                return checkTraceId(headerTraceId);
            }

            final String queryParamTraceId = httpServletRequest.getParameter(TracerService.TRACE_ID_KEY);
            if (!AppbaseUtil.isBlank(queryParamTraceId)) {
                return checkTraceId(queryParamTraceId);
            }

            return HttpServletResponse.SC_NO_CONTENT;
        }

        private int checkTraceId(final String requestTraceId) {
            final String mdcTraceId = MDC.get(TracerService.TRACE_ID_KEY);
            if (mdcTraceId == null) {
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            if (!mdcTraceId.equals(requestTraceId)) {
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

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
