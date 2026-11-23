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
package com.viveworks.appbase.stacktest.test.core.servlet;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.CoreEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CoreServletTest extends AbstractServletTest {
    private final CoreEndpointTestImpl coreEndpointTestImpl;

    public CoreServletTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.coreEndpointTestImpl =
                new CoreEndpointTestImpl(CoreServletTest.class, appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        reloadServiceTest();
        shutdownServiceTest();
        shutdownAllServicesTest();
        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }

    private void reloadServiceTest() throws Exception {
        coreEndpointTestImpl.reloadServiceTestImpl();
    }

    private void shutdownServiceTest() throws Exception {
        coreEndpointTestImpl.shutdownServiceTestImpl();
    }

    private void shutdownAllServicesTest() throws Exception {
        coreEndpointTestImpl.shutdownAllServicesTest();
    }

    private void statusTest() throws Exception {
        coreEndpointTestImpl.statusTestImpl();
    }

    private void reloadTest() throws Exception {
        coreEndpointTestImpl.reloadTestImpl();
    }

    private void reloadWithConfigTest() throws Exception {
        coreEndpointTestImpl.reloadWithConfigTest();
    }

}
