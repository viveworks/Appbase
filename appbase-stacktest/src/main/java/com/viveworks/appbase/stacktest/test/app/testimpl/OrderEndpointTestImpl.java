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
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.CreateOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.CreateOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.DeleteOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.DeleteOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.ReadOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.ReadOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.ReadOrdersResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.UpdateOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.UpdateOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductRequest;
import com.viveworks.appbase.stacktest.service.CustomerService;
import com.viveworks.appbase.stacktest.service.OrderService;
import com.viveworks.appbase.stacktest.service.ProductService;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.app.jaxrs.OrderJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.app.servlet.OrderServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class OrderEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final StackTestServices stackTestServices;

    private final String url;

    public OrderEndpointTestImpl(
            final Class<?> testType,
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, OrderJaxrsResourceTest.class, OrderServletTest.class);

        this.testType = testType;
        this.stackTestServices = stackTestServices;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.STACK_TEST_ORDERS_RESOURCE;
        } else {
            url = baseUrl + StackTestConstants.STACK_TEST_ORDER_PATH;
        }
    }

    public void createOrderTestImpl() throws Exception {
        final String testName = "createOrderTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;

        final String email = "test" + TestUtil.randomId() + "@test.com";
        final String sku = "sku" + TestUtil.randomId();
        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = httpPost(
                    url, createOrderRequest1, CreateOrderRequest.class, CreateOrderResponse.class);
            checkNotNull(createOrderResponse1, testName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, testName, "createdOrder1");

            final ReadOrderRequest readOrderRequest1 = new ReadOrderRequest(createdOrder1.getOrderId(), null);
            final ReadOrderResponse readOrderResponse1 = orderService().readOrder(readOrderRequest1);
            final Order expectedOrder = readOrderResponse1.getOrder();
            checkNotNull(readOrderResponse1, testName, "readOrderResponse1");
            checkNotNull(expectedOrder, testName, "expectedOrder");
            checkEquals(expectedOrder, createdOrder1, testName, "expectedOrder.equals");
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

            if (createdProduct1 != null) {
                final DeleteProductRequest deleteProductRequest =
                        new DeleteProductRequest(createdProduct1.getSku(), null);
                productService().deleteProduct(deleteProductRequest);
            }
        }

        endTest(testType, testName);
    }

    public void readOrdersTestImpl() throws Exception {
        final String testName = "readOrdersTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;

        final String email = "test" + TestUtil.randomId() + "@test.com";
        final String sku = "sku" + TestUtil.randomId();

        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = orderService().createOrder(createOrderRequest1);
            checkNotNull(createOrderResponse1, testName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, testName, "createdOrder1");

            final String finalUrl = url + "?limit=100";
            final ReadOrdersResponse readOrdersResponse = httpGetJson(finalUrl, ReadOrdersResponse.class);
            final List<Order> orders = readOrdersResponse.getOrders();
            checkNotNull(orders, testName, "orders");
            checkTrue(containsOrder(createdOrder1, orders), testName, "containsOrder");
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

            if (createdProduct1 != null) {
                final DeleteProductRequest deleteProductRequest =
                        new DeleteProductRequest(createdProduct1.getSku(), null);
                productService().deleteProduct(deleteProductRequest);
            }
        }

        endTest(testType, testName);
    }

    public void readOrderTestImpl() throws Exception {
        final String testName = "readOrderTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;

        final String email = "test" + TestUtil.randomId() + "@test.com";
        final String sku = "sku" + TestUtil.randomId();

        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = orderService().createOrder(createOrderRequest1);
            checkNotNull(createOrderResponse1, testName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, testName, "createdOrder1");

            String finalUrl1;
            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl1 = url + "/" + createdOrder1.getOrderId();
                finalUrl2 = url + "/12345678";
            } else {
                finalUrl1 = url + "?id=" + createdOrder1.getOrderId();
                finalUrl2 = url + "?id=12345678";
            }

            final ReadOrderResponse readOrderResponse1 = httpGetJson(finalUrl1, ReadOrderResponse.class);
            final Order actualOrder1 = readOrderResponse1.getOrder();
            checkNotNull(actualOrder1, testName, "actualOrder1");
            checkEquals(createdOrder1, actualOrder1, testName, "actualOrder1.equals");

            final ReadOrderResponse readOrderResponse2 = httpGetJson(finalUrl2, ReadOrderResponse.class);
            final Order actualOrder2 = readOrderResponse2.getOrder();
            checkNull(actualOrder2, testName, "actualOrder2");
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

            if (createdProduct1 != null) {
                final DeleteProductRequest deleteProductRequest =
                        new DeleteProductRequest(createdProduct1.getSku(), null);
                productService().deleteProduct(deleteProductRequest);
            }
        }

        endTest(testType, testName);
    }

    public void searchOrdersTestImpl() throws Exception {
        final String testName = "searchOrdersTest";
        startTest(testType, testName);

        /*
        FIXME - DOES NOT WORK! - Since orderId is a number / long, ES is not able to search.
        Customer createdCustomer1 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;

        final int random = (int) (Math.random() * 10000);
        final String email = "test" + random + "@test.com";
        final String sku = "sku" + random;

        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = orderService().createOrder(createOrderRequest1);
            checkNotNull(createOrderResponse1, testName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, testName, "createdOrder1");

            StackTestUtil.elasticsearchIndexSyncWait(logger);

            String finalUrl1;
            if (isRestApiTest()) {
                finalUrl1 = url + "/search/" + createdOrder1.getOrderId();
            } else {
                finalUrl1 = url + "?search=" + createdOrder1.getOrderId();
            }
            final SearchOrdersResponse searchOrdersResponse1 = httpGetJson(finalUrl1, SearchOrdersResponse.class);
            final List<Order> orders1 = searchOrdersResponse1.getOrders();
            checkNotNull(orders1, testName, "orders1");
            checkTrue(containsOrder(createdOrder1, orders1), testName, "containsOrder1");

            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl2 = url + "/search/" + random;
            } else {
                finalUrl2 = url + "?search=" + random;
            }
            final SearchOrdersResponse searchOrdersResponse2 = httpGetJson(finalUrl2, SearchOrdersResponse.class);
            final List<Order> orders2 = searchOrdersResponse2.getOrders();
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

            if (createdProduct1 != null) {
                final DeleteProductRequest deleteProductRequest =
                        new DeleteProductRequest(createdProduct1.getSku(), null);
                productService().deleteProduct(deleteProductRequest);
            }
        }
        */

        endTest(testType, testName);
    }

    public void updateOrderTestImpl() throws Exception {
        final String testName = "updateOrderTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;

        final String email = "test" + TestUtil.randomId() + "@test.com";
        final String sku = "sku" + TestUtil.randomId();

        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = orderService().createOrder(createOrderRequest1);
            checkNotNull(createOrderResponse1, testName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, testName, "createdOrder1");

            final Order clonedOrder = createdOrder1.clone();
            clonedOrder.setPrice(2.2);
            final UpdateOrderRequest updateOrderRequest1 = new UpdateOrderRequest(clonedOrder, null);
            final String json1 = serialize(UpdateOrderRequest.class, updateOrderRequest1);
            final UpdateOrderResponse updateOrderResponse1 = httpPutJson(url, json1, UpdateOrderResponse.class);
            final Order updatedOrder1 = updateOrderResponse1.getOrder();
            checkNotNull(updatedOrder1, testName, "updatedOrder1");
            checkEquals(clonedOrder, updatedOrder1, testName, "updatedOrder1");

            final Order nonExistingOrder = new Order();
            nonExistingOrder.setOrderId(123456789L);
            nonExistingOrder.setPrice(3.3);
            nonExistingOrder.setSku(sku);
            nonExistingOrder.setCustomerId(createdCustomer1.getCustomerId());
            final UpdateOrderRequest updateOrderRequest2 = new UpdateOrderRequest(nonExistingOrder, null);
            final String json2 = serialize(UpdateOrderRequest.class, updateOrderRequest2);
            final UpdateOrderResponse updateOrderResponse2 = httpPutJson(url, json2, UpdateOrderResponse.class);
            checkFalse(updateOrderResponse2.isUpdated(), testName, "updateOrderResponse2.updated");
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

            if (createdProduct1 != null) {
                final DeleteProductRequest deleteProductRequest =
                        new DeleteProductRequest(createdProduct1.getSku(), null);
                productService().deleteProduct(deleteProductRequest);
            }
        }

        endTest(testType, testName);
    }

    public void deleteOrderTestImpl() throws Exception {
        final String testName = "deleteOrderTest";
        startTest(testType, testName);

        Customer createdCustomer1 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;

        final String email = "test" + TestUtil.randomId() + "@test.com";
        final String sku = "sku" + TestUtil.randomId();

        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, testName, "createdCustomer1");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = orderService().createOrder(createOrderRequest1);
            checkNotNull(createOrderResponse1, testName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, testName, "createdOrder1");

            String finalUrl1;
            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl1 = url + "/" + createdOrder1.getOrderId();
                finalUrl2 = url + "/" + 123456789L;
            } else {
                finalUrl1 = url + "?id=" + createdOrder1.getOrderId();
                finalUrl2 = url + "?id=" + 123456789L;
            }

            final DeleteOrderResponse deleteOrderResponse1 = httpDelete(finalUrl1, DeleteOrderResponse.class);
            checkTrue(deleteOrderResponse1.isDeleted(), testName, "deleteOrderResponse1.deleted");

            final DeleteOrderResponse deleteOrderResponse2 = httpDelete(finalUrl2, DeleteOrderResponse.class);
            checkFalse(deleteOrderResponse2.isDeleted(), testName, "deleteOrderResponse2.deleted");
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

            if (createdProduct1 != null) {
                final DeleteProductRequest deleteProductRequest =
                        new DeleteProductRequest(createdProduct1.getSku(), null);
                productService().deleteProduct(deleteProductRequest);
            }
        }

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private boolean containsOrder(final Order expectedOrder, final List<Order> actualOrders) {
        for (final Order actualOrder : actualOrders) {
            if (actualOrder.equals(expectedOrder)) {
                return true;
            }
        }

        return false;
    }

    private ProductService productService() {
        return stackTestServices.getProductService();
    }

    private CustomerService customerService() {
        return stackTestServices.getCustomerService();
    }

    private OrderService orderService() {
        return stackTestServices.getOrderService();
    }

    private boolean isRestApiTest() {
        return OrderJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
