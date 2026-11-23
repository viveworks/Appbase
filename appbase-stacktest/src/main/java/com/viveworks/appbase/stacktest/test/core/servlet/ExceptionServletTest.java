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
package com.viveworks.appbase.stacktest.test.core.servlet;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.ExceptionEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ExceptionServletTest extends AbstractServletTest {
    private final ExceptionEndpointTestImpl exceptionEndpointTestImpl;

    public ExceptionServletTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.exceptionEndpointTestImpl =
                new ExceptionEndpointTestImpl(getClass(), appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        readAllExceptionMessageDetailsTest();
        readExceptionMessageDetailsByIdTest();
        readAllExceptionMessageDetailsByTimestampsTest();
        readUniqueExceptionTypeNamesTest();
        readExceptionMessageDetailsByThrowableTypeNameTest();
        getExceptionListenersByExceptionTypeMapTest();
        getGlobalExceptionListenersTest();
        getExceptionStoreFactoryByConfigurationTypeNameMapTest();
        getExceptionStoresTest();

        removeExceptionMessageDetailsByIdTest();
        removeExceptionMessageDetailsExclusiveBeforeTest();
        unregisterExceptionListenerByExceptionTypeTest();
        unregisterGlobalExceptionListenerTest();
        unregisterExceptionStoreFactoryByConfigurationTypeTest();
        unregisterExceptionStoreTest();
        clearExceptionListenersByExceptionTypeMapTest();
        clearGlobalExceptionListenersTest();
        clearExceptionStoreFactoryByConfigurationTypeMapTest();
        clearExceptionStoresTest();

        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }

    private void readAllExceptionMessageDetailsTest() throws Exception {
        exceptionEndpointTestImpl.readAllExceptionMessageDetailsTestImpl();
    }

    private void readExceptionMessageDetailsByIdTest() throws Exception {
        exceptionEndpointTestImpl.readExceptionMessageDetailsByIdTestImpl();
    }

    private void readAllExceptionMessageDetailsByTimestampsTest() throws Exception {
        exceptionEndpointTestImpl.readAllExceptionMessageDetailsByTimestampsTestImpl();
    }

    private void readUniqueExceptionTypeNamesTest() throws Exception {
        exceptionEndpointTestImpl.readUniqueExceptionTypeNamesTestImpl();
    }

    private void readExceptionMessageDetailsByThrowableTypeNameTest() throws Exception {
        exceptionEndpointTestImpl.readExceptionMessageDetailsByThrowableTypeNameTestImpl();
    }

    private void getExceptionListenersByExceptionTypeMapTest() throws Exception {
        exceptionEndpointTestImpl.getExceptionListenersByExceptionTypeMapTestImpl();
    }

    private void getGlobalExceptionListenersTest() throws Exception {
        exceptionEndpointTestImpl.getGlobalExceptionListenersTestImpl();
    }

    private void getExceptionStoreFactoryByConfigurationTypeNameMapTest() throws Exception {
        exceptionEndpointTestImpl.getExceptionStoreFactoryByConfigurationTypeNameMapTestImpl();
    }

    private void getExceptionStoresTest() throws Exception {
        exceptionEndpointTestImpl.getExceptionStoresTestImpl();
    }



    private void removeExceptionMessageDetailsByIdTest() throws Exception {
        exceptionEndpointTestImpl.removeExceptionMessageDetailsByIdTestImpl();
    }

    private void removeExceptionMessageDetailsExclusiveBeforeTest() throws Exception {
        exceptionEndpointTestImpl.removeExceptionMessageDetailsExclusiveBeforeTestImpl();
    }

    private void unregisterExceptionListenerByExceptionTypeTest() throws Exception {
        exceptionEndpointTestImpl.unregisterExceptionListenerByExceptionTypeTestImpl();
    }

    private void unregisterGlobalExceptionListenerTest() throws Exception {
        exceptionEndpointTestImpl.unregisterGlobalExceptionListenerTestImpl();
    }

    private void unregisterExceptionStoreFactoryByConfigurationTypeTest() throws Exception {
        exceptionEndpointTestImpl.unregisterExceptionStoreFactoryByConfigurationTypeTestImpl();
    }

    private void unregisterExceptionStoreTest() throws Exception {
        exceptionEndpointTestImpl.unregisterExceptionStoreTestImpl();
    }

    private void clearExceptionListenersByExceptionTypeMapTest() throws Exception {
        exceptionEndpointTestImpl.clearExceptionListenersByExceptionTypeMapTestImpl();
    }

    private void clearGlobalExceptionListenersTest() throws Exception {
        exceptionEndpointTestImpl.clearGlobalExceptionListenersTestImpl();
    }

    private void clearExceptionStoreFactoryByConfigurationTypeMapTest() throws Exception {
        exceptionEndpointTestImpl.clearExceptionStoreFactoryByConfigurationTypeMapTestImpl();
    }

    private void clearExceptionStoresTest() throws Exception {
        exceptionEndpointTestImpl.clearExceptionStoresTestImpl();
    }


    private void statusTest() throws Exception {
        exceptionEndpointTestImpl.statusTestImpl();
    }

    private void reloadTest() throws Exception {
        exceptionEndpointTestImpl.reloadTestImpl();
    }

    private void reloadWithConfigTest() throws Exception {
        exceptionEndpointTestImpl.reloadWithConfigTestImpl();
    }

}
