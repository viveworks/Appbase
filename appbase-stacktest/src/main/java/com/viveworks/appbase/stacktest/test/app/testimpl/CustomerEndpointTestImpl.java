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
package com.viveworks.appbase.stacktest.test.app.testimpl;

import com.viveworks.appbase.stacktest.test.TestUtil;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.StackTestUtil;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerOrdersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.CreateOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.CreateOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.DeleteOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import com.viveworks.appbase.stacktest.service.OrderService;
import com.viveworks.appbase.stacktest.service.ProductService;
import com.viveworks.appbase.stacktest.service.StackTestServiceException;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.app.jaxrs.CustomerJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.app.servlet.CustomerServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final StackTestServices stackTestServices;

    private final String url;

    public CustomerEndpointTestImpl(
            final Class<?> testType,
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, CustomerJaxrsResourceTest.class, CustomerServletTest.class);

        this.testType = testType;
        this.stackTestServices = stackTestServices;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.STACK_TEST_CUSTOMERS_RESOURCE;
        } else {
            url = baseUrl + StackTestConstants.STACK_TEST_CUSTOMER_PATH;
        }
    }

    public void createCustomerTestImpl() throws Exception {
        final String testName = "createCustomerTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        final String email = "test" + TestUtil.randomId() + "@test.com";
        try {
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    httpPost(url, createCustomerRequest1, CreateCustomerRequest.class, CreateCustomerResponse.class);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final ReadCustomerRequest readCustomerRequest1 = new ReadCustomerRequest(
                    createdCustomer1.getCustomerId(), null);
            final ReadCustomerResponse readCustomerResponse1 = customerService().readCustomer(readCustomerRequest1);
            checkNotNull(readCustomerResponse1, testName, "readCustomerResponse1");

            final Customer expectedCustomer1 = readCustomerResponse1.getCustomer();
            checkNotNull(expectedCustomer1, testName, "expectedCustomer1");
            checkEquals(expectedCustomer1, createdCustomer1, testName, "expectedCustomer1.equals");
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }
        }

        endTest(testType, testName);
    }

    public void readCustomersTestImpl() throws Exception {
        final String testName = "readCustomersTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        final String email = "test" + TestUtil.randomId() + "@test.com";
        try {
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    httpPost(url, createCustomerRequest1, CreateCustomerRequest.class, CreateCustomerResponse.class);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final String finalUrl = url + "?limit=100";
            final ReadCustomersResponse readCustomersResponse = httpGetJson(finalUrl, ReadCustomersResponse.class);
            final List<Customer> customers = readCustomersResponse.getCustomers();
            checkNotNull(customers, testName, "customers");
            checkTrue(containsCustomer(createdCustomer1, customers), testName, "containsCustomer");
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }
        }

        endTest(testType, testName);
    }

    public void readCustomerTestImpl() throws Exception {
        final String testName = "readCustomerTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        final String email = "test" + TestUtil.randomId() + "@test.com";
        try {
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    httpPost(url, createCustomerRequest1, CreateCustomerRequest.class, CreateCustomerResponse.class);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            String finalUrl1;
            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl1 = url + "/" + createdCustomer1.getCustomerId();
                finalUrl2 = url + "/" + 1234561234L;
            } else {
                finalUrl1 = url + "?id=" + createdCustomer1.getCustomerId();
                finalUrl2 = url + "?id=" + 1234561234L;
            }

            final ReadCustomerResponse readCustomerResponse1 = httpGetJson(finalUrl1, ReadCustomerResponse.class);
            final Customer actualCustomer1 = readCustomerResponse1.getCustomer();
            checkNotNull(actualCustomer1, testName, "actualCustomer1");
            checkEquals(createdCustomer1, actualCustomer1, testName, "actualCustomer1.equals");

            final ReadCustomerResponse readCustomerResponse2 = httpGetJson(finalUrl2, ReadCustomerResponse.class);
            final Customer actualCustomer2 = readCustomerResponse2.getCustomer();
            checkNull(actualCustomer2, testName, "actualCustomer2");
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }
        }

        endTest(testType, testName);
    }

    public void readCustomerOrdersTestImpl() throws Exception {
        final String testName = "readCustomerOrdersTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        Customer createdCustomer2 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;

        final String email1 = "test" + TestUtil.randomId() + "@test.com";
        Thread.sleep(50);
        final String email2 = "test" + TestUtil.randomId() + "@test.com";
        final String sku = "sku" + TestUtil.randomId();

        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email1, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final CreateCustomerRequest createCustomerRequest2 = new CreateCustomerRequest(email2, null, null);
            final CreateCustomerResponse createCustomerResponse2 =
                    customerService().createCustomer(createCustomerRequest2);
            checkNotNull(createCustomerResponse2, testName, "createCustomerResponse2");
            createdCustomer2 = createCustomerResponse2.getCustomer();
            checkNotNull(createdCustomer2, testName, "createdCustomer2");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = orderService().createOrder(createOrderRequest1);
            checkNotNull(createOrderResponse1, testName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, testName, "createdOrder1");

            String finalUrl1;
            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl1 = url + "/" + createdCustomer1.getCustomerId() + "/orders";
                finalUrl2 = url + "/" + createdCustomer2.getCustomerId() + "/orders";
            } else {
                finalUrl1 = baseUrl + "/" + StackTestConstants.STACK_TEST_CUSTOMER_ORDERS_PATH +
                        "?id=" + createdCustomer1.getCustomerId();
                finalUrl2 = baseUrl + "/" + StackTestConstants.STACK_TEST_CUSTOMER_ORDERS_PATH +
                        "?id=" + createdCustomer2.getCustomerId();
            }

            final ReadCustomerOrdersResponse readCustomerOrdersResponse1 =
                    httpGetJson(finalUrl1, ReadCustomerOrdersResponse.class);
            checkNotNull(readCustomerOrdersResponse1, testName, "readCustomerOrdersResponse1");
            final List<Order> orders1 = readCustomerOrdersResponse1.getOrders();
            checkTrue(containsOrder(orders1, createdOrder1), testName, "containsOrder1");

            final ReadCustomerOrdersResponse readCustomerOrdersResponse2 =
                    httpGetJson(finalUrl2, ReadCustomerOrdersResponse.class);
            checkNotNull(readCustomerOrdersResponse2, testName, "readCustomerOrdersResponse2");
            final List<Order> orders2 = readCustomerOrdersResponse2.getOrders();
            checkNotNull(orders2, testName, "orders2");
            checkTrue(orders2.isEmpty(), testName, "orders2.empty");
        } finally {
            if (createdOrder1 != null) {
                final DeleteOrderRequest deleteOrderRequest =
                        new DeleteOrderRequest(createdOrder1.getOrderId(), null);
                orderService().deleteOrder(deleteOrderRequest);
            }

            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }

            if (createdCustomer2 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer2.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }

            if (createdProduct1 != null) {
                final DeleteProductRequest deleteProductRequest =
                        new DeleteProductRequest(createdProduct1.getSku(), null);
                productService().deleteProduct(deleteProductRequest);
            }
        }

        endTest(testType, testName);
    }

    public void searchCustomersTestImpl() throws Exception {
        final String testName = "searchCustomersTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        final int random = (int) (Math.random() * 10000);
        final String email = "test" +  + random + "@test.com";
        try {
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    httpPost(url, createCustomerRequest1, CreateCustomerRequest.class, CreateCustomerResponse.class);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");
            StackTestUtil.elasticsearchIndexSyncWait(logger);

            // FIXME DOES NOT WORK! - if parameter is full email ("test" + random + "@test.com")
            String finalUrl1;
            if (isRestApiTest()) {
                finalUrl1 = url + "/search/test" + random;
            } else {
                finalUrl1 = url + "?search=test" + random;
            }
            final SearchCustomersResponse searchCustomersResponse1 =
                    httpGetJson(finalUrl1, SearchCustomersResponse.class);
            final List<Customer> customers1 = searchCustomersResponse1.getCustomers();
            checkNotNull(customers1, testName, "customers1");
            checkTrue(containsCustomer(createdCustomer1, customers1), testName, "containsCustomer1");

            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl2 = url + "/search/email" + (Math.random() * 10_000);
            } else {
                finalUrl2 = url + "?search=email" + (Math.random() * 10_000);
            }
            final SearchCustomersResponse searchCustomersResponse2 =
                    httpGetJson(finalUrl2, SearchCustomersResponse.class);
            final List<Customer> customers2 = searchCustomersResponse2.getCustomers();
            checkNotNull(customers2, testName, "customers2");
            checkTrue(customers2.isEmpty(), testName, "customers2.empty");
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }
        }

        endTest(testType, testName);
    }

    public void updateCustomerTestImpl() throws Exception {
        final String testName = "updateCustomerTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        final String email = "test" + TestUtil.randomId() + "@test.com";
        Thread.sleep(200);
        final String updatedEmail = "test" + TestUtil.randomId() + "@test.com";
        try {
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    httpPost(url, createCustomerRequest1, CreateCustomerRequest.class, CreateCustomerResponse.class);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final Customer updatedCustomerRequest1 = createdCustomer1.clone();
            updatedCustomerRequest1.setEmail(updatedEmail);
            final UpdateCustomerRequest updateCustomerRequest1 =
                    new UpdateCustomerRequest(updatedCustomerRequest1, null);
            final String json1 = serialize(UpdateCustomerRequest.class, updateCustomerRequest1);

            final UpdateCustomerResponse updateCustomerResponse1 =
                    httpPutJson(url, json1, UpdateCustomerResponse.class);
            final Customer updatedCustomer1 = updateCustomerResponse1.getCustomer();
            checkNotNull(updatedCustomer1, testName, "updatedCustomer1");
            checkEquals(updatedCustomer1, updatedCustomerRequest1, testName, "updatedCustomer1.equals");

            final Customer updatedCustomerRequest2 = new Customer();
            updatedCustomerRequest2.setCustomerId(12356789123L);
            updatedCustomerRequest2.setEmail("non-existent-email@nope.com");
            updatedCustomerRequest2.setPhotoId(null);
            final UpdateCustomerRequest updateCustomerRequest2 =
                    new UpdateCustomerRequest(updatedCustomerRequest2, null);
            final String json2 = serialize(UpdateCustomerRequest.class, updateCustomerRequest2);
            boolean exception = false;
            try {
                httpPutJson(url, json2, UpdateCustomerResponse.class);
            } catch (StackTestServiceException ex) {
                exception = true;
            }
            checkTrue(exception, testName, "exception");
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }
        }

        endTest(testType, testName);
    }

    public void deleteCustomerTestImpl() throws Exception {
        final String testName = "deleteCustomerTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        final String email = "test" + TestUtil.randomId() + "@test.com";
        try {
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    httpPost(url, createCustomerRequest1, CreateCustomerRequest.class, CreateCustomerResponse.class);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            String finalUrl1;
            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl1 = url + "/" + createdCustomer1.getCustomerId();
                finalUrl2 = url + "/" + 1234561234L;
            } else {
                finalUrl1 = url + "?id=" + createdCustomer1.getCustomerId();
                finalUrl2 = url + "?id=" + 1234561234L;
            }

            final DeleteProductResponse deleteProductResponse1 = httpDelete(finalUrl1, DeleteProductResponse.class);
            checkTrue(deleteProductResponse1.isDeleted(), testName, "deleteProductResponse1.deleted");

            boolean exception = false;
            try {
                httpDelete(finalUrl2, DeleteProductResponse.class);
            } catch (StackTestServiceException stse) {
                exception = true;
            }
            checkTrue(exception, testName, "exception");
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                try {
                    customerService().deleteCustomer(deleteCustomerRequest);
                } catch (StackTestServiceException stse) {
                    // ignore .....
                }
            }
        }

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private boolean containsOrder(final List<Order> actualOrders, final Order expectedOrder) {
        for (final Order actualOrder : actualOrders) {
            if (actualOrder.equals(expectedOrder)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsCustomer(final Customer expectedCustomer, final List<Customer> actualCustomers) {
        for (final Customer actualCustomer : actualCustomers) {
            if (actualCustomer.equals(expectedCustomer)) {
                return true;
            }
        }

        return false;
    }

    private CustomerService customerService() {
        return stackTestServices.getCustomerService();
    }

    private ProductService productService() {
        return stackTestServices.getProductService();
    }

    private OrderService orderService() {
        return stackTestServices.getOrderService();
    }

    private boolean isRestApiTest() {
        return CustomerJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
