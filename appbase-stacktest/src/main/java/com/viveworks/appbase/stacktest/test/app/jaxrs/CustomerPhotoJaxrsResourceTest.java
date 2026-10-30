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
package com.viveworks.appbase.stacktest.test.app.jaxrs;

import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractResourceTest;
import com.viveworks.appbase.stacktest.test.app.testimpl.CustomerPhotoEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerPhotoJaxrsResourceTest extends AbstractResourceTest {
    private final CustomerPhotoEndpointTestImpl customerPhotoEndpointTestImpl;

    public CustomerPhotoJaxrsResourceTest(
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        this.customerPhotoEndpointTestImpl = new CustomerPhotoEndpointTestImpl(
                CustomerPhotoJaxrsResourceTest.class, stackTestServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runResourceTestsImpl() throws Exception {
        uploadPhotoTest();
        downloadPhotoByCustomerIdTest();
        downloadPhotoByPhotoIdTest();
    }

    private void uploadPhotoTest() throws Exception {
        customerPhotoEndpointTestImpl.uploadPhotoTestImpl();
    }

    private void downloadPhotoByCustomerIdTest() throws Exception {
        customerPhotoEndpointTestImpl.downloadPhotoByCustomerIdTestImpl();
    }

    private void downloadPhotoByPhotoIdTest() throws Exception {
        customerPhotoEndpointTestImpl.downloadPhotoByPhotoIdTestImpl();
    }
}
