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
package com.viveworks.appbase.stacktest.test.app.testimpl;

import com.viveworks.appbase.stacktest.test.TestUtil;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.accessor.druid.DummyUser;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.CustomerStatisticsResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.service.CustomerService;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.app.jaxrs.CustomerStatisticsJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.app.servlet.CustomerStatisticsServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerStatisticsEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final StackTestServices stackTestServices;

    private final String url;

    public CustomerStatisticsEndpointTestImpl(
            final Class<?> testType,
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, CustomerStatisticsJaxrsResourceTest.class, CustomerStatisticsServletTest.class);

        this.testType = testType;
        this.stackTestServices = stackTestServices;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.STACK_TEST_CUSTOMER_STATISTICS_RESOURCE;
        } else {
            url = baseUrl + StackTestConstants.STACK_TEST_CUSTOMER_STATISTICS_PATH;
        }
    }

    public void statisticsTestImpl() throws Exception {
        final String testName = "statisticsTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        final String email = "test" + TestUtil.randomId() + "@test.com";

        try {
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            String finalUrl;
            if (isRestApiTest()) {
                finalUrl = url + "/" + createdCustomer1.getCustomerId();
            } else {
                finalUrl = url + "?id=" + createdCustomer1.getCustomerId();
            }
            final CustomerStatisticsResponse customerStatisticsResponse =
                    httpGetJson(finalUrl, CustomerStatisticsResponse.class);
            checkNotNull(customerStatisticsResponse, testName, "customerStatisticsResponse");
            final DummyUser dummyUser = customerStatisticsResponse.getDummyUser();
            checkNotNull(dummyUser, testName, "dummyUser");
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }
        }

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private CustomerService customerService() {
        return stackTestServices.getCustomerService();
    }

    private boolean isRestApiTest() {
        return CustomerStatisticsJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
