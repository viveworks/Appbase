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
package com.viveworks.appbase.stacktest.test.core;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractApiTest;
import com.viveworks.appbase.stacktest.test.ResourceTest;
import com.viveworks.appbase.stacktest.test.ServletTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.LogJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.LogServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class LogApiTest extends AbstractApiTest {
    private final LogJaxrsResourceTest logJaxrsResourceTest;
    private final LogServletTest logServletTest;

    public LogApiTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.logJaxrsResourceTest =
                new LogJaxrsResourceTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.logServletTest =
                new LogServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
    }

    public static void runTests(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        new LogApiTest(appbaseServices, httpClient, cookieStore, baseUrl).runTests();
    }

    @Nullable
    @Override
    protected ResourceTest resourceTest() {
        return logJaxrsResourceTest;
    }

    @Nullable
    @Override
    protected ServletTest servletTest() {
        return logServletTest;
    }
}
