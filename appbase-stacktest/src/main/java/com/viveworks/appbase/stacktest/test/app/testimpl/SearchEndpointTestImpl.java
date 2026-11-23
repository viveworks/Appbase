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
import com.viveworks.appbase.stacktest.StackTestUtil;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.DeleteOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.search.SearchRequest;
import com.viveworks.appbase.stacktest.data.pojo.search.SearchResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import com.viveworks.appbase.stacktest.service.OrderService;
import com.viveworks.appbase.stacktest.service.ProductService;
import com.viveworks.appbase.stacktest.service.SearchService;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.app.jaxrs.SearchJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.app.servlet.SearchServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SearchEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final StackTestServices stackTestServices;

    private final String url;

    public SearchEndpointTestImpl(
            final Class<?> testType,
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, SearchJaxrsResourceTest.class, SearchServletTest.class);

        this.testType = testType;
        this.stackTestServices = stackTestServices;

        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.STACK_TEST_SEARCH_RESOURCE;
        } else {
            url = baseUrl + StackTestConstants.STACK_TEST_SEARCH_PATH;
        }
    }

    public void searchTestImpl() throws Exception {
        final String testName = "searchTest";
        startTest(testType, testName);

        searchProductTest();
        searchCustomerTest();
        searchOrderTest();

        endTest(testType, testName);
    }

    private void searchProductTest() throws Exception {
        final String childTestName = "searchProductTest";
        startChildTest(testType, childTestName);

        final String sku = "sku" + TestUtil.randomId();
        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 =
                    productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, childTestName, "createProductResponse1");
            final Product createdProduct1 = createProductResponse1.getProduct();

            StackTestUtil.elasticsearchIndexSyncWait(logger);

            searchProductTestImpl(createdProduct1, createdProduct1, childTestName, true);

            final Product product1 = new Product();
            product1.setSku(sku);
            searchProductTestImpl(product1, createdProduct1, childTestName, true);
        } finally {
            final DeleteProductRequest deleteProductRequest = new DeleteProductRequest(sku, null);
            productService().deleteProduct(deleteProductRequest);
        }

        endChildTest(testType, childTestName);
    }

    private void searchProductTestImpl(
            final Product searchProduct,
            final Product actualProduct,
            final String childTestName,
            final boolean shouldExist) throws Exception
    {
        final SearchRequest searchRequest = new SearchRequest(searchProduct);
        final SearchResponse searchResponse = httpPost(url, searchRequest, SearchRequest.class, SearchResponse.class);
        checkNotNull(searchResponse, childTestName, "searchResponse");
        final List<Product> products = searchResponse.getProducts();

        if (shouldExist) {
            checkTrue(productExists(actualProduct, products, childTestName), childTestName, "productExists1");
        } else {
            checkFalse(productExists(actualProduct, products, childTestName), childTestName, "productExists1");
        }

        final List<Customer> customers = searchResponse.getCustomers();
        checkNotNull(customers, childTestName, "customers");
        checkTrue(customers.isEmpty(), childTestName, "customers.empty");

        final List<Order> orders = searchResponse.getOrders();
        checkNotNull(orders, childTestName, "orders");
        checkTrue(orders.isEmpty(), childTestName, "orders.empty");
    }

    private boolean productExists(
            final Product expectedProduct,
            final List<Product> actualProducts,
            final String childTestName)
    {
        checkNotNull(actualProducts, childTestName, "actualProducts");

        for (final Product actualProduct : actualProducts) {
            if (actualProduct != null && actualProduct.equals(expectedProduct)) {
                return true;
            }
        }

        return false;
    }

    private void searchCustomerTest() throws Exception {
        final String childTestName = "searchCustomerTest";
        startChildTest(testType, childTestName);

        Customer createdCustomer1 = null;
        try {
            /*
            final String email = "test" + TestUtil.randomId() + "@test.com";
            final CreateCustomerRequest createCustomerRequest1 =
                    new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, childTestName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, childTestName, "createdCustomer1");

            StackTestUtil.elasticsearchIndexSyncWait(logger);

            // FIXME .....
            // Searching by full email will not work due to tokenization of '@'
            */
        } finally {
            if (createdCustomer1 != null) {
                final DeleteCustomerRequest deleteCustomerRequest =
                        new DeleteCustomerRequest(createdCustomer1.getCustomerId(), null);
                customerService().deleteCustomer(deleteCustomerRequest);
            }
        }

        endChildTest(testType, childTestName);
    }

    private void searchCustomerTestImpl(
            final Customer searchCustomer,
            final Customer actualCustomer,
            final String childTestName,
            final boolean shouldExist) throws Exception
    {
        final SearchRequest searchRequest = new SearchRequest(searchCustomer);
        final SearchResponse searchResponse = httpPost(url, searchRequest, SearchRequest.class, SearchResponse.class);
        checkNotNull(searchResponse, childTestName, "searchResponse");
        final List<Customer> customers = searchResponse.getCustomers();

        if (shouldExist) {
            checkTrue(customerExists(actualCustomer, customers, childTestName), childTestName, "customerExists");
        } else {
            checkFalse(customerExists(actualCustomer, customers, childTestName), childTestName, "customerExists");
        }

        final List<Product> products = searchResponse.getProducts();
        checkNotNull(products, childTestName, "products");
        checkTrue(products.isEmpty(), childTestName, "products.empty");

        final List<Order> orders = searchResponse.getOrders();
        checkNotNull(orders, childTestName, "orders");
        checkTrue(orders.isEmpty(), childTestName, "orders.empty");
    }

    private boolean customerExists(
            final Customer expectedCustomer,
            final List<Customer> actualCustomers,
            final String childTestName)
    {
        checkNotNull(actualCustomers, childTestName, "actualCustomers");

        for (final Customer actualCustomer : actualCustomers) {
            if (actualCustomer != null && actualCustomer.equals(expectedCustomer)) {
                return true;
            }
        }

        return false;
    }

    private void searchOrderTest() throws Exception {
        final String childTestName = "searchOrderTest";
        startChildTest(testType, childTestName);

        Customer createdCustomer1 = null;
        Product createdProduct1 = null;
        Order createdOrder1 = null;
        try {
            /*
            final String email = "test" + TestUtil.randomId() + "@test.com";
            final String sku = "sku" + TestUtil.randomId();

            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            checkNotNull(createProductResponse1, childTestName, "createProductResponse1");
            createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, childTestName, "createdProduct1");

            final CreateCustomerRequest createCustomerRequest1 = new CreateCustomerRequest(email, null, null);
            final CreateCustomerResponse createCustomerResponse1 =
                    customerService().createCustomer(createCustomerRequest1);
            checkNotNull(createCustomerResponse1, childTestName, "createCustomerResponse1");
            createdCustomer1 = createCustomerResponse1.getCustomer();
            checkNotNull(createdCustomer1, childTestName, "createdCustomer1");

            final CreateOrderRequest createOrderRequest1 =
                    new CreateOrderRequest(createdCustomer1.getCustomerId(), createdProduct1.getSku(), 24.09, null);
            final CreateOrderResponse createOrderResponse1 = orderService().createOrder(createOrderRequest1);
            checkNotNull(createOrderResponse1, childTestName, "createOrderResponse1");
            createdOrder1 = createOrderResponse1.getOrder();
            checkNotNull(createdOrder1, childTestName, "createdOrder1");

            StackTestUtil.elasticsearchIndexSyncWait(logger);

            // FIXME .....
            // Searching by orderId does not work since it's a number and something to do with index field property
            */
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

        endChildTest(testType, childTestName);
    }

    private void searchOrderTestImpl(
            final Order searchOrder,
            final Order actualOrder,
            final String childTestName,
            final boolean shouldExist) throws Exception
    {
        final SearchRequest searchRequest = new SearchRequest(searchOrder);
        final SearchResponse searchResponse = httpPost(url, searchRequest, SearchRequest.class, SearchResponse.class);
        checkNotNull(searchResponse, childTestName, "searchResponse");
        final List<Order> orders = searchResponse.getOrders();

        if (shouldExist) {
            checkTrue(orderExists(actualOrder, orders, childTestName), childTestName, "orderExists");
        } else {
            checkFalse(orderExists(actualOrder, orders, childTestName), childTestName, "orderExists");
        }

        final List<Customer> customers = searchResponse.getCustomers();
        checkNotNull(customers, childTestName, "customers");
        checkTrue(customers.isEmpty(), childTestName, "customers.empty");

        final List<Product> products = searchResponse.getProducts();
        checkNotNull(products, childTestName, "products");
        checkTrue(products.isEmpty(), childTestName, "products.empty");
    }

    private boolean orderExists(
            final Order expectedOrder,
            final List<Order> actualOrders,
            final String childTestName)
    {
        checkNotNull(actualOrders, childTestName, "actualOrders");

        for (final Order actualOrder : actualOrders) {
            if (actualOrder != null && actualOrder.equals(expectedOrder)) {
                return true;
            }
        }

        return false;
    }


    // ..............................
    // Helpers .....
    // ..............................

    private SearchService searchService() {
        return stackTestServices.getSearchService();
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
        return SearchJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
