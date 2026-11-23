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
package com.viveworks.appbase.stacktest.test.app;

import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractApiTest;
import com.viveworks.appbase.stacktest.test.ResourceTest;
import com.viveworks.appbase.stacktest.test.ServletTest;
import com.viveworks.appbase.stacktest.test.app.jaxrs.ProductJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.app.servlet.ProductServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ProductApiTest extends AbstractApiTest {
    private final ProductJaxrsResourceTest productJaxrsResourceTest;
    private final ProductServletTest productServletTest;

    public ProductApiTest(
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        this.productJaxrsResourceTest =
                new ProductJaxrsResourceTest(stackTestServices, httpClient, cookieStore, baseUrl);
        this.productServletTest =
                new ProductServletTest(stackTestServices, httpClient, cookieStore, baseUrl);
    }

    public static void runTests(
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        new ProductApiTest(stackTestServices, httpClient, cookieStore, baseUrl).runTests();
    }

    @Nullable
    @Override
    protected ResourceTest resourceTest() {
        return productJaxrsResourceTest;
    }

    @Nullable
    @Override
    protected ServletTest servletTest() {
        return productServletTest;
    }
}
