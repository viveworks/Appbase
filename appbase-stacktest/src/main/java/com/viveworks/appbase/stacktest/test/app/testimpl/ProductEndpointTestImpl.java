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
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.ReadProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.ReadProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.ReadProductsResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.SearchProductsResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.UpdateProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.UpdateProductResponse;
import com.viveworks.appbase.stacktest.service.ProductService;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.app.jaxrs.ProductJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.app.servlet.ProductServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ProductEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final StackTestServices stackTestServices;

    private final String url;

    public ProductEndpointTestImpl(
            final Class<?> testType,
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, ProductJaxrsResourceTest.class, ProductServletTest.class);

        this.testType = testType;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.STACK_TEST_PRODUCTS_RESOURCE;
        } else {
            url = baseUrl + StackTestConstants.STACK_TEST_PRODUCT_PATH;
        }

        this.stackTestServices = stackTestServices;
    }

    public void createProductTestImpl() throws Exception {
        final String testName = "createProductTest";
        startTest(testType, testName);

        final String sku = "sku" + TestUtil.randomId();
        try {
            final CreateProductRequest createProductRequest1 =
                    new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 =
                    httpPost(url, createProductRequest1, CreateProductRequest.class, CreateProductResponse.class);
            checkNotNull(createProductResponse1, testName, "createProductResponse1");
            final Product createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final ReadProductRequest readProductRequest1 = new ReadProductRequest(sku, null);
            final ReadProductResponse readProductResponse1 = productService().readProduct(readProductRequest1);
            checkNotNull(readProductResponse1, testName, "readProductResponse1");

            final Product expectedProduct1 = readProductResponse1.getProduct();
            checkNotNull(expectedProduct1, testName, "expectedProduct1");
            checkEquals(expectedProduct1, createdProduct1, testName, "expectedProduct1.equals");

            boolean exception = false;
            try {
                final CreateProductRequest createProductRequest2 =
                        new CreateProductRequest(null, "description1", 1.1, null);
                httpPost(url, createProductRequest2, CreateProductRequest.class, CreateProductResponse.class);
            } catch (Exception ex) {
                exception = true;
            }
            checkTrue(exception, testName, "exception");
        } finally {
            final DeleteProductRequest deleteProductRequest = new DeleteProductRequest(sku, null);
            productService().deleteProduct(deleteProductRequest);
        }

        endTest(testType, testName);
    }

    public void readProductsTestImpl() throws Exception {
        final String testName = "readProductsTest";
        startTest(testType, testName);

        final String sku = "sku" + TestUtil.randomId();
        try {
            final CreateProductRequest createProductRequest = new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse = productService().createProduct(createProductRequest);
            final Product createdProduct = createProductResponse.getProduct();
            checkNotNull(createdProduct, testName, "createdProduct");

            final String finalUrl = url + "?limit=100";
            final ReadProductsResponse readProductsResponse = httpGetJson(finalUrl, ReadProductsResponse.class);
            final List<Product> products = readProductsResponse.getProducts();
            checkNotNull(products, testName, "products");
            checkTrue(containsProduct(createdProduct, products), testName, "containsProduct");
        } finally {
            final DeleteProductRequest deleteProductRequest = new DeleteProductRequest(sku, null);
            productService().deleteProduct(deleteProductRequest);
        }

        endTest(testType, testName);
    }

    public void readProductTestImpl() throws Exception {
        final String testName = "readProductTest";
        startTest(testType, testName);

        final String sku = "sku" + TestUtil.randomId();
        try {
            final CreateProductRequest createProductRequest1 = new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            final Product createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            String finalUrl1;
            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl1 = url + "/" + sku;
                finalUrl2 = url + "/non-existent-sku";
            } else {
                finalUrl1 = url + "?id=" + sku;
                finalUrl2 = url + "?id=non-existent-sku";
            }

            final ReadProductResponse readProductResponse1 = httpGetJson(finalUrl1, ReadProductResponse.class);
            final Product actualProduct1 = readProductResponse1.getProduct();
            checkNotNull(actualProduct1, testName, "actualProduct1");
            checkEquals(createdProduct1, actualProduct1, testName, "actualProduct1.equals");

            final ReadProductResponse readProductResponse2 = httpGetJson(finalUrl2, ReadProductResponse.class);
            final Product actualProduct2 = readProductResponse2.getProduct();
            checkNull(actualProduct2, testName, "actualProduct2");
        } finally {
            final DeleteProductRequest deleteProductRequest = new DeleteProductRequest(sku, null);
            productService().deleteProduct(deleteProductRequest);
        }

        endTest(testType, testName);
    }

    public void searchProductsTestImpl() throws Exception {
        final String testName = "searchProductsTest";
        startTest(testType, testName);

        final String sku = "sku" + TestUtil.randomId();
        try {
            final CreateProductRequest createProductRequest = new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse = productService().createProduct(createProductRequest);
            final Product createdProduct = createProductResponse.getProduct();
            checkNotNull(createdProduct, testName, "createdProduct");
            StackTestUtil.elasticsearchIndexSyncWait(logger);

            String finalUrl1;
            if (isRestApiTest()) {
                finalUrl1 = url + "/search/" + sku;
            } else {
                finalUrl1 = url + "?search=" + sku;
            }
            final SearchProductsResponse searchProductsResponse1 = httpGetJson(finalUrl1, SearchProductsResponse.class);
            final List<Product> products1 = searchProductsResponse1.getProducts();
            checkNotNull(products1, testName, "products1");
            checkTrue(containsProduct(createdProduct, products1), testName, "containsProduct1");

            String finalUrl2;
            if (isRestApiTest()) {
                finalUrl2 = url + "/search/sku" + (Math.random() * 10_000);
            } else {
                finalUrl2 = url + "?search=sku" + (Math.random() * 10_000);
            }
            final SearchProductsResponse searchProductsResponse2 = httpGetJson(finalUrl2, SearchProductsResponse.class);
            final List<Product> products2 = searchProductsResponse2.getProducts();
            checkNotNull(products2, testName, "products2");
            checkTrue(products2.isEmpty(), testName, "products2.empty");
        } finally {
            final DeleteProductRequest deleteProductRequest = new DeleteProductRequest(sku, null);
            productService().deleteProduct(deleteProductRequest);
        }

        endTest(testType, testName);
    }

    public void updateProductTestImpl() throws Exception {
        final String testName = "updateProductTest";
        startTest(testType, testName);

        final String sku = "sku" + TestUtil.randomId();
        try {
            final CreateProductRequest createProductRequest1 = new CreateProductRequest(sku, "description1", 1.1, null);
            final CreateProductResponse createProductResponse1 = productService().createProduct(createProductRequest1);
            final Product createdProduct1 = createProductResponse1.getProduct();
            checkNotNull(createdProduct1, testName, "createdProduct1");

            final Product updatedProductRequest1 = createdProduct1.clone();
            final String updatedDescription1 = "Updated description";
            updatedProductRequest1.setDescription(updatedDescription1);

            final UpdateProductRequest updateProductRequest1 = new UpdateProductRequest(updatedProductRequest1, null);
            final String json1 = serialize(UpdateProductRequest.class, updateProductRequest1);

            final UpdateProductResponse updateProductResponse1 = httpPutJson(url, json1, UpdateProductResponse.class);
            final Product updatedProduct1 = updateProductResponse1.getProduct();
            checkNotNull(updatedProduct1, testName, "updatedProduct1");
            checkEquals(updatedProduct1, updatedProductRequest1, testName, "updatedProduct1.equals");

            final Product updatedProductRequest2 = new Product();
            updatedProductRequest2.setSku("non-existent-sku");
            updatedProductRequest2.setDescription("desc1");
            updatedProductRequest2.setPrice(24.09);
            final UpdateProductRequest updateProductRequest2 = new UpdateProductRequest(updatedProductRequest2, null);
            final String json2 = serialize(UpdateProductRequest.class, updateProductRequest2);
            final UpdateProductResponse updateProductResponse2 = httpPutJson(url, json2, UpdateProductResponse.class);
            checkFalse(updateProductResponse2.isUpdated(), testName, "updateProductResponse2.updated");
        } finally {
            final DeleteProductRequest deleteProductRequest = new DeleteProductRequest(sku, null);
            productService().deleteProduct(deleteProductRequest);
        }

        endTest(testType, testName);
    }

    public void deleteProductTestImpl() throws Exception {
        final String testName = "deleteProductTest";
        startTest(testType, testName);

        final String sku = "sku" + TestUtil.randomId();
        final CreateProductRequest createProductRequest = new CreateProductRequest(sku, "description1", 1.1, null);
        final CreateProductResponse createProductResponse = productService().createProduct(createProductRequest);
        final Product createdProduct = createProductResponse.getProduct();
        checkNotNull(createdProduct, testName, "createdProduct");

        String finalUrl1;
        String finalUrl2;
        if (isRestApiTest()) {
            finalUrl1 = url + "/" + sku;
            finalUrl2 = url + "/non-existent-sku";
        } else {
            finalUrl1 = url + "?id=" + sku;
            finalUrl2 = url + "?id=non-existent-sku";
        }

        final DeleteProductResponse deleteProductResponse1 = httpDelete(finalUrl1, DeleteProductResponse.class);
        checkTrue(deleteProductResponse1.isDeleted(), testName, "deleteProductResponse1.deleted");

        final DeleteProductResponse deleteProductResponse2 = httpDelete(finalUrl2, DeleteProductResponse.class);
        checkFalse(deleteProductResponse2.isDeleted(), testName, "deleteProductResponse2.deleted");

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private boolean containsProduct(final Product expectedProduct, final List<Product> actualProducts) {
        for (final Product actualProduct : actualProducts) {
            if (actualProduct.equals(expectedProduct)) {
                return true;
            }
        }

        return false;
    }

    private ProductService productService() {
        return stackTestServices.getProductService();
    }

    private boolean isRestApiTest() {
        return ProductJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
