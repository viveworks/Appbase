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
package com.viveworks.appbase.stacktest.test.core.testimpl;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.ListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.dummytypes.DummyLogStore;
import com.viveworks.appbase.stacktest.dummytypes.DummyLogStoreConfiguration;
import com.viveworks.appbase.stacktest.dummytypes.DummyLogStoreFactory;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.LogJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.LogServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class LogEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public LogEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, LogJaxrsResourceTest.class, LogServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_LOG_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_LOG_SERVLET_PATH;
    }

    public void readLogRecordsBetweenTestImpl() throws Exception {
        final String testName = "readLogRecordsBetweenTest";
        startTest(testType, testName);

        long timestamp1 = 0;
        long timestamp2 = 0;
        final LogAccessor logAccessor = logAccessor();
        try {
            Thread.sleep(100);
            final LogRecord logRecord1 = writeLogRecord();
            timestamp1 = logRecord1.getTimestamp() - 10;
            Thread.sleep(1000);
            final LogRecord logRecord2 = writeLogRecord();
            timestamp2 = logRecord2.getTimestamp() + 10;

            final Map<String, Collection<LogRecord>> logRecords1 =
                    logAccessor.readLogRecordsBetween(timestamp1, timestamp2, 100);
            checkTrue(containsLogRecords(logRecords1.values(), logRecord1, logRecord2),
                    testName,
                    "containsLogRecords1");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logRecords/" +
                        "/inclusiveStartTimestamp/" + timestamp1 +
                        "/exclusiveEndTimestamp/" + timestamp2 +
                        "/pageSize/10";
            } else {
                url = servletUrl + "?operation=logRecordsBetween" +
                        "&inclusiveStartTimestamp=" + timestamp1 +
                        "&exclusiveEndTimestamp=" + timestamp2 +
                        "&pageSize=10";
            }

            final MapOfStringListOfStringsValue result = httpGetJson(url, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();
            checkNotNull(resultMap, testName, "resultMap");
            checkTrue(containsLogRecord(resultMap, logRecord1), testName, "containsLogRecord1");
            checkTrue(containsLogRecord(resultMap, logRecord2), testName, "containsLogRecord2");
        } finally {
            logAccessor().removeLogRecordsBetween(timestamp1, timestamp2);
        }

        endTest(testName);
    }

    public void readLogRecordsAtTestImpl() throws Exception {
        final String testName = "readLogRecordsAtTest";
        startTest(testType, testName);

        Thread.sleep(100);
        long timestamp1 = 0;
        long timestamp2 = 0;
        final LogAccessor logAccessor = logAccessor();
        try {
            final LogRecord logRecord1 = writeLogRecord();

            timestamp1 = logRecord1.getTimestamp();
            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logRecords" +
                        "/timestamp/" + timestamp1 +
                        "/pageSize/10";
            } else {
                url = servletUrl + "?operation=logRecordsAt" +
                        "&timestamp=" + timestamp1 +
                        "&pageSize=10";
            }

            timestamp2 = System.currentTimeMillis() + 1000;
            final Map<String, Collection<LogRecord>> logRecords1 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkTrue(containsLogRecords(logRecords1.values(), logRecord1),
                    testName,
                    "containsLogRecords1");

            final MapOfStringListOfStringsValue result = httpGetJson(url, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();
            checkNotNull(resultMap, testName, "resultMap");
            checkTrue(containsLogRecord(resultMap, logRecord1), testName, "containsLogRecord1");
        } finally {
            logAccessor().removeLogRecordsBetween(timestamp1 - 1000, timestamp2);
        }

        endTest(testName);
    }

    public void getLogStoreFactoryByConfigurationTypeMapTestImpl() throws Exception {
        final String testName = "getLogStoreFactoryByConfigurationTypeMapTest";
        startTest(testType, testName);

        final LogAccessor logAccessor = logAccessor();
        final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map =
                logAccessor.getLogStoreFactoryByConfigurationTypeMap();
        checkFalse(
                map.containsKey(DummyLogStoreConfiguration.class.getName()),
                testName,
                "dummyLogStoreConfiguration1");

        final DummyLogStoreFactory dummyLogStoreFactory = new DummyLogStoreFactory();
        logAccessor.registerLogStoreFactoryByConfigurationType(
                DummyLogStoreConfiguration.class, dummyLogStoreFactory);

        final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map1 =
                logAccessor.getLogStoreFactoryByConfigurationTypeMap();
        checkTrue(
                map1.containsKey(DummyLogStoreConfiguration.class.getName()),
                testName,
                "dummyLogStoreConfiguration2");

        try {
            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logStoreFactory/configurationType";
            } else {
                url = servletUrl + "?operation=logStoreFactoryByConfigurationTypeMap";
            }

            final MapOfStringStringValue result = httpGetJson(url, MapOfStringStringValue.class);
            final Map<String, String> resultMap = result.getValue();
            checkNotNull(resultMap, testName, "resultMap");
            checkTrue(resultMap.containsKey(DummyLogStoreConfiguration.class.getName()),
                    testName,
                    "dummyLogStoreConfiguration3");
        } finally {
            logAccessor().unregisterLogStoreFactoryByConfigurationType(
                    DummyLogStoreConfiguration.class, DummyLogStoreFactory.class);
        }

        endTest(testName);
    }

    public void getLogStoresTestImpl() throws Exception {
        final String testName = "getLogStoresTest";
        startTest(testType, testName);

        final LogAccessor logAccessor = logAccessor();
        final Collection<LogStore> logStores1 = logAccessor.getLogStores();
        final boolean containsLogStore1 = containsLogStore(logStores1, DummyLogStore.class);
        checkFalse(containsLogStore1, testName, "containsLogStore1");

        final DummyLogStore dummyLogStore = new DummyLogStore();
        logAccessor.registerLogStore(dummyLogStore);
        final boolean containsLogStore2 = containsLogStore(logStores1, DummyLogStore.class);
        checkTrue(containsLogStore2, testName, "containsLogStore2");

        try {
            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logStores";
            } else {
                url = servletUrl + "?operation=logStores";
            }

            final ListOfStringsValue result = httpGetJson(url, ListOfStringsValue.class);
            final List<String> values = result.getValue();
            checkNotNull(values, testName, "values");

            boolean containsLogStore = false;
            for (final String className : values) {
                if (DummyLogStore.class.getName().equals(className)) {
                    containsLogStore = true;
                    break;
                }
            }
            checkTrue(containsLogStore, testName, "containsLogStore");
        } finally {
            logAccessor().unregisterLogStore(DummyLogStore.class);
        }

        endTest(testName);
    }

    public void readLogRecordsByCriteriaTestImpl() throws Exception {
        final String testName = "readLogRecordsByCriteriaTest";
        startTest(testType, testName);

        Thread.sleep(100);
        long timestamp1 = 0;
        long timestamp2 = 0;
        final LogAccessor logAccessor = logAccessor();

        try {
            final LogRecord logRecord1 = writeLogRecord();

            timestamp1 = logRecord1.getTimestamp();
            timestamp2 = System.currentTimeMillis() + 1000;
            final Map<String, Collection<LogRecord>> logRecords1 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkTrue(containsLogRecords(logRecords1.values(), logRecord1), testName, "containsLogRecords1");

            final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                    .builder()
                    .message(logRecord1.getMessage())
                    .build();
            final Map<String, Collection<LogRecord>> logRecords2 =
                    logAccessor.readLogRecordsByCriteria(logRecordCriteria, 100);
            checkTrue(containsLogRecords(logRecords2.values(), logRecord1), testName, "containsLogRecords2");


            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/readLogRecordsByCriteria/pageSize/10";
            } else {
                url = servletUrl + "?operation=readLogRecordsByCriteria&pageSize=10";
            }

            final MapOfStringListOfStringsValue result =
                    httpPost(url, logRecordCriteria, LogRecordCriteria.class, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();
            checkNotNull(resultMap, testName, "resultMap");
            checkTrue(containsLogRecord(resultMap, logRecord1), testName, "containsLogRecord3");
        } finally {
            logAccessor().removeLogRecordsBetween(timestamp1 - 1000, timestamp2);
        }

        endTest(testName);
    }

    public void synchronizeLogStoresTestImpl() throws Exception {
        final String testName = "synchronizeLogStoresTest";
        startTest(testType, testName);

        Thread.sleep(100);
        long timestamp1 = 0;
        long timestamp2 = 0;
        final LogAccessor logAccessor = logAccessor();

        try {
            final LogRecord logRecord1 = writeLogRecord(false);

            timestamp1 = logRecord1.getTimestamp();
            timestamp2 = System.currentTimeMillis() + 1000;

            final Map<String, Collection<LogRecord>> logRecords1 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkFalse(containsLogRecords(logRecords1.values(), logRecord1), testName, "containsLogRecords1");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/synchronizeLogStores";
            } else {
                url = servletUrl + "?operation=synchronizeLogStores";
            }
            httpPostEmpty(url);
            Thread.sleep(100);

            final Map<String, Collection<LogRecord>> logRecords2 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkTrue(containsLogRecords(logRecords2.values(), logRecord1), testName, "containsLogRecords2");
        } finally {
            logAccessor().removeLogRecordsBetween(timestamp1 - 1000, timestamp2);
        }

        endTest(testName);
    }

    public void removeLogRecordsBetweenTestImpl() throws Exception {
        final String testName = "removeLogRecordsBetweenTest";
        startTest(testType, testName);

        Thread.sleep(100);
        long timestamp1 = 0;
        long timestamp2 = 0;
        final LogAccessor logAccessor = logAccessor();

        try {
            final LogRecord logRecord1 = writeLogRecord();

            timestamp1 = logRecord1.getTimestamp();
            timestamp2 = System.currentTimeMillis() + 1000;

            final Map<String, Collection<LogRecord>> logRecords1 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkTrue(containsLogRecords(logRecords1.values(), logRecord1), testName, "containsLogRecords1");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logRecords" +
                        "/inclusiveStartTimestamp/" + timestamp1 +
                        "/exclusiveEndTimestamp/" + timestamp2;
            } else {
                url = servletUrl + "?operation=removeLogRecordsBetween" +
                        "&inclusiveStartTimestamp=" + timestamp1 +
                        "&exclusiveEndTimestamp=" + timestamp2;
            }

            httpDelete(url);
            logAccessor.synchronizeLogStores();
            Thread.sleep(100);

            final Map<String, Collection<LogRecord>> logRecords2 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkFalse(containsLogRecords(logRecords2.values(), logRecord1), testName, "containsLogRecords2");
        } finally {
            logAccessor().removeLogRecordsBetween(timestamp1 - 1000, timestamp2);
        }

        endTest(testName);
    }

    public void removeLogRecordsAtTestImpl() throws Exception {
        final String testName = "removeLogRecordsAtTest";
        startTest(testType, testName);

        Thread.sleep(100);
        long timestamp1 = 0;
        long timestamp2 = 0;
        final LogAccessor logAccessor = logAccessor();

        try {
            final LogRecord logRecord1 = writeLogRecord();

            timestamp1 = logRecord1.getTimestamp();
            timestamp2 = System.currentTimeMillis() + 1000;

            final Map<String, Collection<LogRecord>> logRecords1 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkTrue(containsLogRecords(logRecords1.values(), logRecord1), testName, "containsLogRecords1");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logRecords" + "/timestamp/" + timestamp1;
            } else {
                url = servletUrl + "?operation=removeLogRecordsAt&timestamp=" + timestamp1;
            }

            httpDelete(url);
            logAccessor.synchronizeLogStores();
            Thread.sleep(100);

            final Map<String, Collection<LogRecord>> logRecords2 = logAccessor.readLogRecordsAt(timestamp1, 100);
            checkFalse(containsLogRecords(logRecords2.values(), logRecord1), testName, "containsLogRecords2");
        } finally {
            logAccessor().removeLogRecordsBetween(timestamp1 - 1000, timestamp2);
        }

        endTest(testName);
    }

    public void removeLogRecordsByCriteriaTestImpl() throws Exception {
        final String testName = "removeLogRecordsByCriteriaTest";
        startTest(testType, testName);

        Thread.sleep(100);
        long timestamp1 = 0;
        long timestamp2 = 0;
        final LogAccessor logAccessor = logAccessor();

        try {
            final LogRecord logRecord1 = writeLogRecord();

            timestamp1 = logRecord1.getTimestamp();
            timestamp2 = System.currentTimeMillis() + 1000;

            final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                    .builder()
                    .message(logRecord1.getMessage())
                    .build();
            final Map<String, Collection<LogRecord>> logRecords2 =
                    logAccessor.readLogRecordsByCriteria(logRecordCriteria, 100);
            checkTrue(containsLogRecords(logRecords2.values(), logRecord1), testName, "containsLogRecords2");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/removeLogRecordsByCriteria";
            } else {
                url = servletUrl + "?operation=removeLogRecordsByCriteria";
            }

            httpPost(url, logRecordCriteria, LogRecordCriteria.class, null);
            logAccessor.synchronizeLogStores();
            Thread.sleep(100);

            final Map<String, Collection<LogRecord>> logRecords3 =
                    logAccessor.readLogRecordsByCriteria(logRecordCriteria, 100);
            checkFalse(containsLogRecords(logRecords3.values(), logRecord1), testName, "containsLogRecords3");
        } finally {
            logAccessor().removeLogRecordsBetween(timestamp1 - 1000, timestamp2);
        }

        endTest(testName);
    }

    public void unregisterLogStoreFactoryByConfigurationTypeTestImpl() throws Exception {
        final String testName = "unregisterLogStoreFactoryByConfigurationTypeTest";
        startTest(testType, testName);

        final String logStoreFactoryTypeName = DummyLogStoreFactory.class.getName();
        final String logStoreConfigurationTypeName = DummyLogStoreConfiguration.class.getName();

        final LogAccessor logAccessor = logAccessor();
        try {
            final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map =
                    logAccessor.getLogStoreFactoryByConfigurationTypeMap();
            checkFalse(
                    map.containsKey(DummyLogStoreConfiguration.class.getName()),
                    testName,
                    "dummyLogStoreConfiguration1");

            final DummyLogStoreFactory dummyLogStoreFactory = new DummyLogStoreFactory();
            logAccessor.registerLogStoreFactoryByConfigurationType(
                    DummyLogStoreConfiguration.class, dummyLogStoreFactory);

            final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map1 =
                    logAccessor.getLogStoreFactoryByConfigurationTypeMap();
            checkTrue(
                    map1.containsKey(DummyLogStoreConfiguration.class.getName()),
                    testName,
                    "dummyLogStoreConfiguration2");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/unregisterLogStoreFactory" +
                        "/logStoreFactoryTypeName/" + logStoreFactoryTypeName +
                        "/logStoreConfigurationTypeName/" + logStoreConfigurationTypeName;
            } else {
                url = servletUrl + "?operation=unregisterLogStoreFactoryByConfigurationType" +
                        "&logStoreFactoryTypeName=" + logStoreFactoryTypeName +
                        "&logStoreConfigurationTypeName=" + logStoreConfigurationTypeName;
            }
            httpDelete(url);

            final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map2 =
                    logAccessor.getLogStoreFactoryByConfigurationTypeMap();
            checkFalse(
                    map2.containsKey(DummyLogStoreConfiguration.class.getName()),
                    testName,
                    "dummyLogStoreConfiguration3");
        } finally {
            logAccessor().unregisterLogStoreFactoryByConfigurationType(
                    DummyLogStoreConfiguration.class, DummyLogStoreFactory.class);
        }

        endTest(testName);
    }

    public void unregisterLogStoreTestImpl() throws Exception {
        final String testName = "unregisterLogStoreTest";
        startTest(testType, testName);

        final LogAccessor logAccessor = logAccessor();
        final String logStoreTypeName = DummyLogStore.class.getName();
        try {
            final Collection<LogStore> logStores1 = logAccessor.getLogStores();
            checkFalse(containsLogStore(logStores1, DummyLogStore.class), testName, "containsLogStore1");

            final DummyLogStore dummyLogStore = new DummyLogStore();
            logAccessor.registerLogStore(dummyLogStore);
            checkTrue(containsLogStore(logStores1, DummyLogStore.class), testName, "containsLogStore2");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logStores/" + logStoreTypeName;
            } else {
                url = servletUrl + "?operation=unregisterLogStore&logStoreTypeName=" + logStoreTypeName;
            }
            httpDelete(url);

            final boolean containsLogStore3 = containsLogStore(logStores1, DummyLogStore.class);
            checkFalse(containsLogStore3, testName, "containsLogStore3");
        } finally {
            logAccessor().unregisterLogStore(DummyLogStore.class);
        }

        endTest(testName);
    }

    public void clearLogStoreFactoryByConfigurationTypeMapTestImpl() throws Exception {
        final String testName = "clearLogStoreFactoryByConfigurationTypeMapTest";
        startTest(testType, testName);

        final LogAccessor logAccessor = logAccessor();

        try {
            final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map =
                    logAccessor.getLogStoreFactoryByConfigurationTypeMap();
            checkFalse(
                    map.containsKey(DummyLogStoreConfiguration.class.getName()),
                    testName,
                    "dummyLogStoreConfiguration1");

            final DummyLogStoreFactory dummyLogStoreFactory = new DummyLogStoreFactory();
            logAccessor.registerLogStoreFactoryByConfigurationType(
                    DummyLogStoreConfiguration.class, dummyLogStoreFactory);

            final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map1 =
                    logAccessor.getLogStoreFactoryByConfigurationTypeMap();
            checkTrue(
                    map1.containsKey(DummyLogStoreConfiguration.class.getName()),
                    testName,
                    "dummyLogStoreConfiguration2");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logStoreFactory/configurationType";
            } else {
                url = servletUrl + "?operation=clearLogStoreFactoryByConfigurationTypeMap";
            }
            httpDelete(url);

            final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> map2 =
                    logAccessor.getLogStoreFactoryByConfigurationTypeMap();
            checkFalse(
                    map2.containsKey(DummyLogStoreConfiguration.class.getName()),
                    testName,
                    "dummyLogStoreConfiguration2");
        } finally {
            logAccessor().unregisterLogStoreFactoryByConfigurationType(
                    DummyLogStoreConfiguration.class, DummyLogStoreFactory.class);
        }

        endTest(testName);
    }

    public void clearLogStoresTestImpl() throws Exception {
        final String testName = "clearLogStoresTest";
        startTest(testType, testName);

        final LogAccessor logAccessor = logAccessor();
        try {
            final Collection<LogStore> logStores1 = logAccessor.getLogStores();
            LuceneLogStore luceneLogStore = null;
            for (final LogStore logStore : logStores1) {
                if (LuceneLogStore.class.isAssignableFrom(logStore.getClass())) {
                    luceneLogStore = (LuceneLogStore) logStore;
                    break;
                }
            }
            checkNotNull(luceneLogStore, testName, "luceneLogStore");

            checkFalse(containsLogStore(logStores1, DummyLogStore.class), testName, "containsLogStore1");

            final DummyLogStore dummyLogStore = new DummyLogStore();
            logAccessor.registerLogStore(dummyLogStore);
            checkTrue(containsLogStore(logStores1, DummyLogStore.class), testName, "containsLogStore2");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/logStores";
            } else {
                url = servletUrl + "?operation=clearLogStores";
            }
            httpDelete(url);

            checkFalse(containsLogStore(logStores1, DummyLogStore.class), testName, "containsLogStore2");
            if (luceneLogStore != null) {
                luceneLogStore.setShutdown(false);
                logAccessor.registerLogStore(luceneLogStore);
            }
        } finally {
            logAccessor().unregisterLogStore(DummyLogStore.class);
        }

        endTest(testName);
    }

    public void statusTestImpl() throws Exception {
        final String testName = "statusTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/status";
        } else {
            url = servletUrl + "?operation=status";
        }

        final String status = logService().getStatus().name();
        final String status1 = httpGetText(url, String.class);
        checkEquals(status1, status, testName, "status1");

        endTest(testName);
    }

    public void reloadTestImpl() throws Exception {
        final String testName = "reloadTest";
        startTest(testType, testName);

        /*
        FIXME - CAN RESULT IN UNDEFINED STATE
        If this runs for RestApiTest, ServletTest fails since accessor is shutdown and is not reloading properly
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final DefaultLogService logService = (DefaultLogService) logService();
        final String status = logService.getStatus().name();
        final int statusCode = httpPutEmpty(url);
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

        final String status1 = logService.getStatus().name();
        checkEquals(status1, status, testName, "status1");
        */

        endTest(testName);
    }

    public void reloadWithConfigTestImpl() throws Exception {
        final String testName = "reloadWithConfigTest";
        startTest(testType, testName);

        /*
        FIXME - CAN RESULT IN UNDEFINED STATE
        If this runs for RestApiTest, ServletTest fails since accessor is shutdown and is not reloading properly

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }
        final String status = logService().getStatus().name();
        final DefaultLogServiceConfiguration configuration = logService().getConfiguration();
        final String json = serialize(DefaultLogServiceConfiguration.class, configuration);
        httpPostJson(url, json, null);

        final String status1 = logService().getStatus().name();
        checkEquals(status1, status, testName, "status1");
        */

        endTest(testName);
    }

    public void shutdownTestImpl() throws Exception {
        final String testName = "shutdownTest";
        startTest(testType, testName);

        /*
        FIXME - CAN RESULT IN UNDEFINED STATE
        If this runs for RestApiTest, ServletTest fails since accessor is shutdown and is not reloading properly

        final LogService logService = logService();
        final LogAccessor logAccessor = logAccessor();
        final Collection<LogStore> logStores1 = logAccessor.getLogStores();
        LuceneLogStore luceneLogStore = null;
        for (final LogStore logStore : logStores1) {
            if (LuceneLogStore.class.isAssignableFrom(logStore.getClass())) {
                luceneLogStore = (LuceneLogStore) logStore;
                break;
            }
        }
        checkNotNull(luceneLogStore, testName, "luceneLogStore");

        final DefaultLogServiceConfiguration configuration = logService().getConfiguration();
        checkEquals(ServiceStatus.STARTED, logService.getStatus(), testName, "serviceStatus");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/shutdown";
        } else {
            url = servletUrl + "?operation=shutdown";
        }

        httpPostEmpty(url);
        checkEquals(ServiceStatus.SHUTDOWN, logService.getStatus(), testName, "serviceStatus1");
        logService.reload(configuration);
        if (luceneLogStore != null) {
            luceneLogStore.setShutdown(false);
            logAccessor.registerLogStore(luceneLogStore);
        }
        checkEquals(ServiceStatus.STARTED, logService.getStatus(), testName, "serviceStatus2");
        */

        endTest(testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private boolean containsLogStore(final Collection<LogStore> logStores, Class<? extends LogStore> logStoreType) {
        for (final LogStore logStore : logStores) {
            if (DummyLogStore.class.isAssignableFrom(logStore.getClass())) {
                return true;
            }
        }

        return false;
    }

    private boolean containsLogRecord(
            final Map<String, ListOfStringsValue> in,
            final LogRecord logRecord) throws IOException
    {
        for (final Map.Entry<String, ListOfStringsValue> entry : in.entrySet()) {
            final List<String> jsons = entry.getValue().getValue();

            if (jsons != null) {
                for (final String json : jsons) {
                    final LogRecord logRecord1 = JsonUtil
                            .getJsonMapper()
                            .readerFor(LogRecord.class)
                            .readValue(json);
                    if (logRecord1.getTimestamp() == logRecord.getTimestamp()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean containsLogRecords(
            final Collection<Collection<LogRecord>> actualLogRecordsCollection,
            final LogRecord... expectedLogRecords)
    {
        for (final LogRecord expectedLogRecord : expectedLogRecords) {
            boolean contains = false;
            for (final Collection<LogRecord> actualLogRecords : actualLogRecordsCollection) {
                for (final LogRecord actualLogRecord : actualLogRecords) {
                    if (expectedLogRecord.getTimestamp() == actualLogRecord.getTimestamp()) {
                        contains = true;
                        break;
                    }
                }
            }

            if (!contains) {
                return false;
            }
        }

        return true;
    }

    private LogRecord writeLogRecord() throws Exception {
        return writeLogRecord(true);
    }

    private LogRecord writeLogRecord(final boolean synchronizeLogStores) throws Exception {
        final LogRecord logRecord = LogRecord
                .builder()
                .logLevel(LogLevel.INFO)
                .threadName(Thread.currentThread().getName())
                .loggerName(getClass().getName())
                .timestamp(System.currentTimeMillis())
                .formattedMessage("Message={}")
                .message("Message=message1")
                .build();
        final LogAccessor logAccessor = logAccessor();
        logAccessor.writeLogRecord(logRecord);

        if (synchronizeLogStores) {
            logAccessor.synchronizeLogStores();
        }
        Thread.sleep(500);

        return logRecord;
    }

    private LogAccessor logAccessor() {
        return logService().getLogAccessor();
    }

    private LogService logService() {
        return appbaseServices.getCoreServices().appbaseCore().getLogService();
    }

    private boolean isRestApiTest() {
        return LogJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
