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

package com.viveworks.appbase.stacktest.test.service.exceptionhandler.testimpl;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.service.exceptionhandler.jaxrs.JaxrsExceptionHandlerMapperTest;
import com.viveworks.appbase.stacktest.test.service.exceptionhandler.servlet.ServletExceptionHandlerMapperTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ExceptionHandlerTestImpl extends AbstractTest {
    private static final String EXCEPTION_TYPE_PARAM = "exceptionType";

    private final Class<?> testType;

    public ExceptionHandlerTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        this.testType = testType;
        checkTestType(testType, JaxrsExceptionHandlerMapperTest.class, ServletExceptionHandlerMapperTest.class);

    }

    public void testImpl() throws Exception {
        final String testName = "testImpl";

        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.EXCEPTION_HANDLER_TEST_RESOURCE_PATH;
            runTests(url);
        } else {
            url = baseUrl + StackTestConstants.EXCEPTION_HANDLER_TEST_SERVLET_PATH;
        }
        runTests(url);

        endTest(testType, testName);
    }

    private void runTests(final String url) throws Exception {
        final HttpResponse httpResponse1 = executeHttpGet(
                url + "?" + EXCEPTION_TYPE_PARAM + "=" + PermissionDeniedException.class.getSimpleName());
        checkEquals(
                HttpServletResponse.SC_FORBIDDEN,
                httpResponse1.getStatusLine().getStatusCode(),
                "testImpl.1",
                "code1");

        final HttpResponse httpResponse4 = executeHttpGet(
                url + "?" + EXCEPTION_TYPE_PARAM + "=" + AppbaseRuntimeException.class.getSimpleName());
        checkEquals(
                HttpServletResponse.SC_BAD_REQUEST,
                httpResponse4.getStatusLine().getStatusCode(),
                "testImpl.4",
                "code4");

        if (isRestApiTest()) {
            final HttpResponse httpResponse5 = executeHttpGet(
                    url + "?" + EXCEPTION_TYPE_PARAM + "=" + WebApplicationException.class.getSimpleName());
            checkEquals(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    httpResponse5.getStatusLine().getStatusCode(),
                    "testImpl.5",
                    "code5");
            final HttpResponse httpResponse2 = executeHttpGet(
                    url + "?" + EXCEPTION_TYPE_PARAM + "=" + AuthorizationServiceException.class.getSimpleName());
            checkEquals(
                    HttpServletResponse.SC_FORBIDDEN,
                    httpResponse2.getStatusLine().getStatusCode(),
                    "testImpl.2",
                    "code2");

            final HttpResponse httpResponse3 = executeHttpGet(
                    url + "?" + EXCEPTION_TYPE_PARAM + "=" + ValidationServiceException.class.getSimpleName());
            checkEquals(
                    HttpServletResponse.SC_BAD_REQUEST,
                    httpResponse3.getStatusLine().getStatusCode(),
                    "testImpl.3",
                    "code3");
        }

        final HttpResponse httpResponse6 = executeHttpGet(
                url + "?" + EXCEPTION_TYPE_PARAM + "=" + IOException.class.getSimpleName());
        checkEquals(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                httpResponse6.getStatusLine().getStatusCode(),
                "testImpl.6",
                "code6");
    }

    private boolean isRestApiTest() {
        return JaxrsExceptionHandlerMapperTest.class.isAssignableFrom(testType);
    }

    @Path(StackTestConstants.EXCEPTION_HANDLER_TEST_RESOURCE_PATH)
    public static class ExceptionHandlerTestResource extends AbstractAppbaseJaxrsEntity {
        @GET
        public Response ping(
                @QueryParam(EXCEPTION_TYPE_PARAM)
                final String exceptionTypeParam)
                throws Exception
        {
            if (exceptionTypeParam.equals(PermissionDeniedException.class.getSimpleName())) {
                throw new PermissionDeniedException("environmentVariable", "property", true, "operationName");
            } else if (exceptionTypeParam.equals(AuthorizationServiceException.class.getSimpleName())) {
                throw new AuthorizationServiceException();
            } else if (exceptionTypeParam.equals(ValidationServiceException.class.getSimpleName())) {
                throw new ValidationServiceException("value");
            } else if (exceptionTypeParam.equals(AppbaseRuntimeException.class.getSimpleName())) {
                throw new AppbaseRuntimeException("message");
            } else if (exceptionTypeParam.equals(WebApplicationException.class.getSimpleName())) {
                throw new WebApplicationException(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
            }

            throw new IOException("message");
        }
    }

    public static class ExceptionHandlerTestServlet extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = 6141521354974518229L;

        @Override
        protected void doGet(
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse)
                throws IOException
        {
            final String exceptionType = nonBlankParameter(EXCEPTION_TYPE_PARAM, httpServletRequest, httpServletResponse);

            if (exceptionType.equals(PermissionDeniedException.class.getSimpleName())) {
                throw new PermissionDeniedException("environmentVariable", "property", true, "operationName");
            } else if (exceptionType.equals(AppbaseRuntimeException.class.getSimpleName())) {
                throw new AppbaseRuntimeException("message");
            }

            throw new IOException("message");
        }
    }
}
