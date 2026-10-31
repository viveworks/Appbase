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
package com.viveworks.appbase.stacktest.test.core.servlet;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.JmxEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxServletTest extends AbstractServletTest {
    private final JmxEndpointTestImpl jmxEndpointTestImpl;

    public JmxServletTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.jmxEndpointTestImpl =
                new JmxEndpointTestImpl(JmxServletTest.class, appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        mxBeanEntriesTest();
        mxBeanEntryTest();
        isMXBeanRegisteredTest();
        unregisterMXBeanTest();
        clearMXBeansTest();
        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }

    private void mxBeanEntriesTest() throws Exception {
        jmxEndpointTestImpl.mxBeanEntriesTestImpl();
    }

    private void mxBeanEntryTest() throws Exception {
        jmxEndpointTestImpl.mxBeanEntryTestImpl();
    }

    private void isMXBeanRegisteredTest() throws Exception {
        jmxEndpointTestImpl.isMXBeanRegisteredTestImpl();
    }

    private void unregisterMXBeanTest() throws Exception {
        jmxEndpointTestImpl.unregisterMXBeanTestImpl();
    }

    private void clearMXBeansTest() throws Exception {
        jmxEndpointTestImpl.clearMXBeansTestImpl();
    }

    private void statusTest() throws Exception {
        jmxEndpointTestImpl.statusTestImpl();
    }

    private void reloadTest() throws Exception {
        jmxEndpointTestImpl.reloadTestImpl();
    }

    private void reloadWithConfigTest() throws Exception {
        jmxEndpointTestImpl.reloadWithConfigTestImpl();
    }

}
