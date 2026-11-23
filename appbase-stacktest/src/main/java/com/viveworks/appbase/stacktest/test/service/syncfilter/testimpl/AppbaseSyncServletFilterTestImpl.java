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

package com.viveworks.appbase.stacktest.test.service.syncfilter.testimpl;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.service.web.filter.servlet.AppbaseHttpServletRequestWrapper;
import com.viveworks.appbase.service.web.filter.servlet.AppbaseHttpServletResponseWrapper;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseSyncServletFilterTestImpl extends AbstractTest {
    private final Class<?> testType;

    public AppbaseSyncServletFilterTestImpl(
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

        final String url = baseUrl + StackTestConstants.APPBASE_SYNC_SERVLET_FILTER_TEST_SERVLET_PATH;
        final int code = httpHead(url);
        checkEquals(HttpServletResponse.SC_NO_CONTENT, code, "filterTestImpl", "code");

        endTest(testType, testName);
    }

    public static class AppbaseSyncFilterTestTestServlet extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = 3788442418118452020L;

        @Override
        protected int headImpl(
                final String traceId,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse) throws Exception
        {
            if (!AppbaseHttpServletRequestWrapper.class.isAssignableFrom(httpServletRequest.getClass())) {
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            if (!AppbaseHttpServletResponseWrapper.class.isAssignableFrom(httpServletResponse.getClass())) {
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            final AppbaseHttpServletRequestWrapper appbaseHttpServletRequestWrapper =
                    (AppbaseHttpServletRequestWrapper) httpServletRequest;
            final AppbaseCore appbaseCore = appbaseHttpServletRequestWrapper.getAppbaseCore();
            if (appbaseCore == null) {
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            final AppbaseHttpServletResponseWrapper appbaseHttpServletResponseWrapper =
                    (AppbaseHttpServletResponseWrapper) httpServletResponse;
            final AppbaseCore appbaseCore1 = appbaseHttpServletResponseWrapper.getAppbaseCore();
            if (appbaseCore1 == null) {
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
