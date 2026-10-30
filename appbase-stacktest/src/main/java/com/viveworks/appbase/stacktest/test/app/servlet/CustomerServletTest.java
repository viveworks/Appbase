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
package com.viveworks.appbase.stacktest.test.app.servlet;

import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import com.viveworks.appbase.stacktest.test.app.testimpl.CustomerEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerServletTest extends AbstractServletTest {
    private final CustomerEndpointTestImpl customerEndpointTestImpl;

    public CustomerServletTest(
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        this.customerEndpointTestImpl = new CustomerEndpointTestImpl(
                CustomerServletTest.class, stackTestServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        createCustomerTest();
        readCustomersTest();
        readCustomerTest();
        readCustomerOrdersTest();
        searchCustomersTest();
        updateCustomerTest();
        deleteCustomerTest();
    }

    private void createCustomerTest() throws Exception {
        customerEndpointTestImpl.createCustomerTestImpl();
    }

    private void readCustomersTest() throws Exception {
        customerEndpointTestImpl.readCustomersTestImpl();
    }

    private void readCustomerTest() throws Exception {
        customerEndpointTestImpl.readCustomerTestImpl();
    }

    private void readCustomerOrdersTest() throws Exception {
        customerEndpointTestImpl.readCustomerOrdersTestImpl();
    }

    private void searchCustomersTest() throws Exception {
        customerEndpointTestImpl.searchCustomersTestImpl();
    }

    private void updateCustomerTest() throws Exception {
        customerEndpointTestImpl.updateCustomerTestImpl();
    }

    private void deleteCustomerTest() throws Exception {
        customerEndpointTestImpl.deleteCustomerTestImpl();
    }
}
