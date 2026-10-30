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
package com.viveworks.appbase.stacktest.test.core.servlet;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.LogEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class LogServletTest extends AbstractServletTest {
    private final LogEndpointTestImpl logEndpointTestImpl;

    public LogServletTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.logEndpointTestImpl =
                new LogEndpointTestImpl(LogServletTest.class, appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        readLogRecordsBetweenTest();
        readLogRecordsAtTest();
        getLogStoreFactoryByConfigurationTypeMapTest();
        getLogStoresTest();

        readLogRecordsByCriteriaTest();
        synchronizeLogStoresTest();

        removeLogRecordsBetweenTest();
        removeLogRecordsAtTest();
        removeLogRecordsByCriteriaTest();
        unregisterLogStoreFactoryByConfigurationTypeTest();
        unregisterLogStoreTest();
        clearLogStoreFactoryByConfigurationTypeMapTest();
        clearLogStoresTest();

        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }


    private void readLogRecordsBetweenTest() throws Exception {
        logEndpointTestImpl.readLogRecordsBetweenTestImpl();
    }

    private void readLogRecordsAtTest() throws Exception {
        logEndpointTestImpl.readLogRecordsAtTestImpl();
    }

    private void getLogStoreFactoryByConfigurationTypeMapTest() throws Exception {
        logEndpointTestImpl.getLogStoreFactoryByConfigurationTypeMapTestImpl();
    }

    private void getLogStoresTest() throws Exception {
        logEndpointTestImpl.getLogStoresTestImpl();
    }


    private void readLogRecordsByCriteriaTest() throws Exception {
        logEndpointTestImpl.readLogRecordsByCriteriaTestImpl();
    }

    private void synchronizeLogStoresTest() throws Exception {
        logEndpointTestImpl.synchronizeLogStoresTestImpl();
    }


    private void removeLogRecordsBetweenTest() throws Exception {
        logEndpointTestImpl.removeLogRecordsBetweenTestImpl();
    }

    private void removeLogRecordsAtTest() throws Exception {
        logEndpointTestImpl.removeLogRecordsAtTestImpl();
    }

    private void removeLogRecordsByCriteriaTest() throws Exception {
        logEndpointTestImpl.removeLogRecordsByCriteriaTestImpl();
    }

    private void unregisterLogStoreFactoryByConfigurationTypeTest() throws Exception {
        logEndpointTestImpl.unregisterLogStoreFactoryByConfigurationTypeTestImpl();
    }

    private void unregisterLogStoreTest() throws Exception {
        logEndpointTestImpl.unregisterLogStoreTestImpl();
    }

    private void clearLogStoreFactoryByConfigurationTypeMapTest() throws Exception {
        logEndpointTestImpl.clearLogStoreFactoryByConfigurationTypeMapTestImpl();
    }

    private void clearLogStoresTest() throws Exception {
        logEndpointTestImpl.clearLogStoresTestImpl();
    }


    private void statusTest() throws Exception {
        logEndpointTestImpl.statusTestImpl();
    }

    private void reloadTest() throws Exception {
        logEndpointTestImpl.reloadTestImpl();
    }

    private void reloadWithConfigTest() throws Exception {
        logEndpointTestImpl.reloadWithConfigTestImpl();
    }

}
