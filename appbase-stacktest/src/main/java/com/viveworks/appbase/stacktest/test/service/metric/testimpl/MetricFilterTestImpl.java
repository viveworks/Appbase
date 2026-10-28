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

package com.viveworks.appbase.stacktest.test.service.metric.testimpl;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.service.metric.jaxrs.MetricJaxrsFilterTest;
import com.viveworks.appbase.stacktest.test.service.metric.servlet.MetricServletFilterTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MetricFilterTestImpl extends AbstractTest {
    private final Class<?> testType;

    public MetricFilterTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        this.testType = testType;
        checkTestType(testType, MetricJaxrsFilterTest.class, MetricServletFilterTest.class);
    }

    public void filterTestImpl() throws Exception {
        final String testName = "filterTestImpl";

        startTest(testType, testName);

        // Multiple HTTP calls to make sure the counter value is updated
        final int totalHttpRequests = 10;
        if (isRestApiTest()) {
            for (int i = 0; i < totalHttpRequests; ++i) {
                final int code = httpHead(baseUrl + StackTestConstants.METRIC_TEST_RESOURCE1_PATH);
                checkEquals(HttpServletResponse.SC_NO_CONTENT, code, "filterTestImpl", "code");
            }

            for (int i = 0; i < totalHttpRequests; ++i) {
                final int code = httpHead(baseUrl + StackTestConstants.METRIC_TEST_RESOURCE2_PATH);
                checkEquals(HttpServletResponse.SC_NO_CONTENT, code, "filterTestImpl", "code");
            }
        } else {
            for (int i = 0; i < totalHttpRequests; ++i) {
                final int code = httpHead(baseUrl + StackTestConstants.METRIC_TEST_SERVLET1_PATH);
                checkEquals(HttpServletResponse.SC_NO_CONTENT, code, "filterTestImpl", "code");
            }
        }

        endTest(testType, testName);
    }

    private boolean isRestApiTest() {
        return MetricJaxrsFilterTest.class.isAssignableFrom(testType);
    }

    @Path(StackTestConstants.METRIC_TEST_RESOURCE1_PATH)
    public static class MetricTestResource1 extends AbstractAppbaseJaxrsEntity {
        private static final AtomicInteger COUNTER = new AtomicInteger(0);

        @HEAD
        @Timed
        public Response ping() {
            final MetricService metricService = appbaseCoreFromContext().getMetricService();
            final String name = metricService.name(
                    MetricTestResource1.class,
                    "ping" + MetricService.METRIC_NAME_SEPARATOR + "timer");
            final Timer timer = metricService.getMetricRegistry().timer(name);
            if (COUNTER.get() != timer.getCount()) {
                return Response.serverError().build();
            }

            COUNTER.incrementAndGet();
            return Response.noContent().build();
        }
    }

    @Path(StackTestConstants.METRIC_TEST_RESOURCE2_PATH)
    public static class MetricTestResource2 extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping() {
            final MetricService metricService = appbaseCoreFromContext().getMetricService();
            final String name = metricService.name(
                    MetricTestResource2.class,
                    "ping" + MetricService.METRIC_NAME_SEPARATOR + "timer");
            final Timer timer = metricService.getMetricRegistry().timer(name);
            if (timer.getCount() != 0) {
                return Response.serverError().build();
            }

            return Response.noContent().build();
        }
    }

    public static class MetricTestServlet1 extends AbstractAppbaseSyncServlet {
        private static final AtomicInteger COUNTER = new AtomicInteger(0);

        private static final long serialVersionUID = 7607026028412633508L;

        @Override
        protected int headImpl(
                final String traceId,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse) throws Exception
        {
            final AppbaseCore appbaseCore = appbaseCore();
            final MetricService metricService = appbaseCore.getMetricService();
            final String name = metricService.name(
                    StackTestConstants.METRIC_TEST_SERVLET1_PATH,
                    "HEAD" + MetricService.METRIC_NAME_SEPARATOR + "timer");
            final Timer timer = metricService.getMetricRegistry().timer(name);
            if (COUNTER.get() != timer.getCount()) {
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            COUNTER.incrementAndGet();
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
