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
package com.viveworks.appbase.stacktest.test;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractApiTest implements ApiTest {
    protected final Logger logger;
    protected final HttpClient httpClient;
    protected final CookieStore cookieStore;
    protected final String baseUrl;

    public AbstractApiTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        AppbaseUtil.notNull(appbaseServices, "appbaseServices");
        AppbaseUtil.notNull(httpClient, "httpClient");
        AppbaseUtil.notNull(cookieStore, "cookieStore");
        AppbaseUtil.notBlank(baseUrl, "baseUrl");

        this.logger = appbaseServices.getCoreServices().appbaseCore().getLogService().getLogger(getClass());
        this.httpClient = httpClient;
        this.cookieStore = cookieStore;
        this.baseUrl = baseUrl;
    }

    @Nullable
    protected abstract ResourceTest resourceTest();

    @Nullable
    protected abstract ServletTest servletTest();

    @Override
    public void runTests() {
        logger.info("Running test: {}", getClass().getName());

        final ResourceTest resourceTest = resourceTest();
        if (resourceTest != null) {
            try {
                logger.info("Running resource test: {}", resourceTest.getClass().getName());
                resourceTest.runResourceTests();
            } catch (Exception ex) {
                throw new RuntimeException("Error running resource tests: " + resourceTest.getClass().getName(), ex);
            } finally {
                resourceTest.cleanupResourceTests();
            }
        }

        final ServletTest servletTest = servletTest();
        if (servletTest != null) {
            try {
                logger.info("Running servlet test: {}", servletTest.getClass().getName());
                servletTest.runServletTests();
            } catch (Exception ex) {
                throw new RuntimeException("Error running servlet tests: " + servletTest.getClass().getName(), ex);
            } finally {
                servletTest.cleanupServletTests();
            }
        }
    }
}
