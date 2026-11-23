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
package com.viveworks.appbase.stacktest.test.core.jaxrs;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractResourceTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.ServiceRegistryEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ServiceRegistryJaxrsResourceTest extends AbstractResourceTest {
    private final ServiceRegistryEndpointTestImpl serviceRegistryEndpointTestImpl;

    public ServiceRegistryJaxrsResourceTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.serviceRegistryEndpointTestImpl = new ServiceRegistryEndpointTestImpl(
                ServiceRegistryJaxrsResourceTest.class, appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runResourceTestsImpl() throws Exception {
        pollAndUpdateStatusesTest();
        overallStatusTest();
        pollOverallStatus();
        getAppbaseServiceStatusTest();
        getAllServiceStatusByServiceTypeNameTest();
        unregisterAppbaseServiceTest();
        clearAppbaseServicesTest();
        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }

    private void pollAndUpdateStatusesTest() throws Exception {
        serviceRegistryEndpointTestImpl.pollAndUpdateStatusesTestImpl();
    }

    private void overallStatusTest() throws Exception {
        serviceRegistryEndpointTestImpl.overallStatusTestImpl();
    }

    private void pollOverallStatus() throws Exception {
        serviceRegistryEndpointTestImpl.pollOverallStatusImpl();
    }

    private void getAppbaseServiceStatusTest() throws Exception {
        serviceRegistryEndpointTestImpl.getAppbaseServiceStatusTestImpl();
    }

    private void getAllServiceStatusByServiceTypeNameTest() throws Exception {
        serviceRegistryEndpointTestImpl.getAllServiceStatusByServiceTypeNameTestImpl();
    }

    private void unregisterAppbaseServiceTest() throws Exception {
        serviceRegistryEndpointTestImpl.unregisterAppbaseServiceTestImpl();
    }

    private void clearAppbaseServicesTest() throws Exception {
        serviceRegistryEndpointTestImpl.clearAppbaseServicesTestImpl();
    }

    private void statusTest() throws Exception {
        serviceRegistryEndpointTestImpl.statusTestImpl();
    }

    private void reloadTest() throws Exception {
        serviceRegistryEndpointTestImpl.reloadTestImpl();
    }

    private void reloadWithConfigTest() throws Exception {
        serviceRegistryEndpointTestImpl.reloadWithConfigTestImpl();
    }

}
