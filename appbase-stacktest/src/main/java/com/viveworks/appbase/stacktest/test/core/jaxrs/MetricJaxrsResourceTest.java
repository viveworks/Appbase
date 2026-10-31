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
package com.viveworks.appbase.stacktest.test.core.jaxrs;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractResourceTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.MetricEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MetricJaxrsResourceTest extends AbstractResourceTest {
    private final MetricEndpointTestImpl metricEndpointTestImpl;

    public MetricJaxrsResourceTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.metricEndpointTestImpl = new MetricEndpointTestImpl(
                MetricJaxrsResourceTest.class, appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runResourceTestsImpl() throws Exception {
        serializeMetricRegistryAsJsonTest();
        clearMicrometerRegistriesTest();
        unregisterMicrometerRegistryTest();
        micrometerRegistryNamesTest();
        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }

    private void serializeMetricRegistryAsJsonTest() throws Exception {
        metricEndpointTestImpl.serializeMetricRegistryAsJsonTestImpl();
    }

    private void clearMicrometerRegistriesTest() throws Exception {
        metricEndpointTestImpl.clearMicrometerRegistriesTestImpl();
    }

    private void unregisterMicrometerRegistryTest() throws Exception {
        metricEndpointTestImpl.unregisterMicrometerRegistryTestImpl();
    }

    private void micrometerRegistryNamesTest() throws Exception {
        metricEndpointTestImpl.micrometerRegistryNamesTestImpl();
    }

    private void statusTest() throws Exception {
        metricEndpointTestImpl.statusTestImpl();
    }

    private void reloadTest() throws Exception {
        metricEndpointTestImpl.reloadTestImpl();
    }

    private void reloadWithConfigTest() throws Exception {
        metricEndpointTestImpl.reloadWithConfigTestImpl();
    }

}
