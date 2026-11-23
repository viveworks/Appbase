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
package com.viveworks.appbase.stacktest.test.core.container;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractApiTest;
import com.viveworks.appbase.stacktest.test.ResourceTest;
import com.viveworks.appbase.stacktest.test.ServletTest;
import com.viveworks.appbase.stacktest.test.core.container.jaxrs.ContainerJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.container.servlet.ContainerServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import javax.annotation.Nullable;

/**
 * Smoke-tests that the active servlet container serves both servlet and JAX-RS routes.
 * Runs regardless of the {@code --container} flag value.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public class ContainerApiTest extends AbstractApiTest {

    private final ContainerJaxrsResourceTest containerJaxrsResourceTest;
    private final ContainerServletTest containerServletTest;

    public ContainerApiTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);
        this.containerJaxrsResourceTest =
                new ContainerJaxrsResourceTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.containerServletTest =
                new ContainerServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
    }

    /**
     * Runs container smoke tests verifying JAX-RS routing and servlet dispatch.
     *
     * @param appbaseServices the running Appbase services
     * @param httpClient      HTTP client for requests
     * @param cookieStore     cookie store
     * @param baseUrl         base URL of the running server
     */
    public static void runTests(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        new ContainerApiTest(appbaseServices, httpClient, cookieStore, baseUrl).runTests();
    }

    @Nullable
    @Override
    protected ResourceTest resourceTest() {
        return containerJaxrsResourceTest;
    }

    @Nullable
    @Override
    protected ServletTest servletTest() {
        return containerServletTest;
    }
}
