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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessorException;
import com.viveworks.appbase.core.service.exception.listener.ExceptionListener;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.ListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.dummytypes.DummyExceptionListener;
import com.viveworks.appbase.stacktest.dummytypes.DummyExceptionStore;
import com.viveworks.appbase.stacktest.dummytypes.DummyExceptionStoreConfiguration;
import com.viveworks.appbase.stacktest.dummytypes.DummyExceptionStoreFactory;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.ExceptionJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.ExceptionServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ExceptionEndpointTestImpl extends AbstractTest {
    private final Logger logger;

    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public ExceptionEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        checkTestType(testType, ExceptionJaxrsResourceTest.class, ExceptionServletTest.class);

        this.testType = testType;
        this.logger = appbaseServices.getCoreServices().appbaseCore().getLogService().getLogger(getClass());
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_EXCEPTION_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_EXCEPTION_SERVLET_PATH;
    }


    // ..............................
    // GET tests .....
    // ..............................

    public void readAllExceptionMessageDetailsTestImpl() throws Exception {
        final String testName = "readAllExceptionMessageDetailsTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionMessageDetails";
        } else {
            url = servletUrl + "?operation=exceptionMessageDetails";
        }

        final ExceptionMessage exceptionMessage = writeExceptionMessage();
        ExceptionMessageDetails exceptionMessageDetails = null;
        try {
            final MapOfStringListOfStringsValue result = httpGetJson(url, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();

            exceptionMessageDetails = findExceptionMessageDetails(resultMap, exceptionMessage);
            checkNotNull(exceptionMessageDetails, testName, "exceptionMessageDetails");
        } finally {
            removeExceptionMessage(exceptionMessageDetails);
        }

        endTest(testType, testName);
    }

    public void readExceptionMessageDetailsByIdTestImpl() throws Exception {
        final String testName = "readExceptionMessageDetailsByIdTest";
        startTest(testType, testName);

        final ExceptionMessage exceptionMessage = writeExceptionMessage();

        ExceptionMessageDetails exceptionMessageDetails = null;
        try {
            final Map<String, Collection<ExceptionMessageDetails>> allExceptionMessageDetails =
                    exceptionAccessor().readAllExceptionMessageDetails();
            final Map.Entry<String, ExceptionMessageDetails> matchingEntry =
                    findExceptionMessageDetailsEntry(allExceptionMessageDetails, exceptionMessage);
            checkNotNull(matchingEntry, testName, "matchingEntry");
            exceptionMessageDetails = matchingEntry.getValue();
            checkNotNull(exceptionMessageDetails, testName, "exceptionMessageDetails");

            final Integer id = exceptionMessageDetails.getId();
            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/exceptionMessageDetails/id/" + id;
            } else {
                url = servletUrl + "?operation=exceptionMessageDetailsById" + "&id=" + id;
            }

            final MapOfStringStringValue result = httpGetJson(url, MapOfStringStringValue.class);
            final Map<String, String> resultMap = result.getValue();
            final String json = resultMap.get(matchingEntry.getKey());
            checkNotNull(json, testName, "json");
            final ExceptionMessageDetails foundExceptionMessageDetails = JsonUtil
                    .getJsonMapper()
                    .readerFor(ExceptionMessageDetails.class)
                    .readValue(json);
            checkNotNull(foundExceptionMessageDetails, testName, "foundExceptionMessageDetails");
            checkRedactedExceptionMessageDetails(
                    exceptionMessageDetails, foundExceptionMessageDetails, testName);
        } finally {
            removeExceptionMessage(exceptionMessageDetails);
        }

        endTest(testType, testName);
    }

    public void readAllExceptionMessageDetailsByTimestampsTestImpl() throws Exception {
        final String testName = "readAllExceptionMessageDetailsByTimestampsTest";
        startTest(testType, testName);

        Set<ExceptionMessageDetails> exceptionMessageDetailsSet = null;
        try {
            final ExceptionMessage exceptionMessage1 = writeExceptionMessage();
            Thread.sleep(1000);
            final ExceptionMessage exceptionMessage2 = writeExceptionMessage();

            final long timestamp1 = exceptionMessage1.getTimestamp() - 1000;
            final long timestamp2 = exceptionMessage2.getTimestamp() + 1000;
            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/exceptionMessageDetails/timestamps/" + timestamp1 + "/" + timestamp2;
            } else {
                url = servletUrl + "?operation=exceptionMessageDetailsByTimestamps" +
                        "&inclusiveMinTimestamp=" + timestamp1 +
                        "&exclusiveMaxTimestamp=" + timestamp2;
            }

            final MapOfStringListOfStringsValue result = httpGetJson(url, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();
            exceptionMessageDetailsSet = exceptionMessageDetails(resultMap);
            final boolean containsExceptionMessages =
                    containsExceptionMessages(exceptionMessageDetailsSet, exceptionMessage1, exceptionMessage2);
            checkTrue(containsExceptionMessages, testName, "containsExceptionMessages");
        } finally {
            if (exceptionMessageDetailsSet != null) {
                for (final ExceptionMessageDetails exceptionMessageDetails : exceptionMessageDetailsSet) {
                    removeExceptionMessage(exceptionMessageDetails);
                }
            }
        }

        endTest(testType, testName);
    }

    public void readUniqueExceptionTypeNamesTestImpl() throws Exception {
        final String testName = "readUniqueExceptionTypeNamesTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/uniqueExceptionTypeNames";
        } else {
            url = servletUrl + "?operation=uniqueExceptionTypeNames";
        }

        ExceptionMessageDetails exceptionMessageDetails = null;
        try {
            final ExceptionMessage exceptionMessage = writeExceptionMessage();
            final Map<String, Collection<ExceptionMessageDetails>> allExceptionMessageDetails =
                    exceptionAccessor().readAllExceptionMessageDetails();
            exceptionMessageDetails = findExceptionMessageDetails1(allExceptionMessageDetails, exceptionMessage);

            final Map<String, Collection<String>> expectedUniqueExceptionTypeNames = exceptionAccessor()
                    .readUniqueExceptionTypeNames();
            final MapOfStringListOfStringsValue result = httpGetJson(url, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();
            final Map<String, Collection<String>> actualUniqueExceptionTypeNames = new HashMap<>();
            if (resultMap != null) {
                resultMap.forEach((key, listOfStringsValue) -> {
                    final List<String> values = listOfStringsValue.getValue();
                    actualUniqueExceptionTypeNames.put(key, values);
                });
            }
            checkEquals(expectedUniqueExceptionTypeNames.size(),
                    actualUniqueExceptionTypeNames.size(),
                    testName,
                    "uniqueExceptionTypeNames.size");

            for (final Map.Entry<String, Collection<String>> entry : expectedUniqueExceptionTypeNames.entrySet()) {
                final String key = entry.getKey();

                checkTrue(actualUniqueExceptionTypeNames.containsKey(key),
                        testName,
                        "actualUniqueExceptionTypeNames.containsKey");
                final Collection<String> actualExceptionTypeNames = actualUniqueExceptionTypeNames.get(key);
                final Set<String> actualExceptionTypeNameSet = new TreeSet<>(actualExceptionTypeNames);

                final Collection<String> expectedExceptionTypeNames = expectedUniqueExceptionTypeNames.get(key);
                final Set<String> expectedExceptionTypeNameSet = new TreeSet<>(expectedExceptionTypeNames);

                checkEquals(expectedExceptionTypeNameSet, actualExceptionTypeNameSet, testName, "exceptionTypeNameSet");
            }
        } finally {
            removeExceptionMessage(exceptionMessageDetails);
        }

        endTest(testType, testName);
    }

    public void readExceptionMessageDetailsByThrowableTypeNameTestImpl() throws Exception {
        final String testName = "readExceptionMessageDetailsByThrowableTypeNameTest";
        startTest(testType, testName);

        final String throwableTypeName = IOException.class.getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionMessageDetails/throwable/" + throwableTypeName;
        } else {
            url = servletUrl + "?operation=exceptionMessageDetailsByThrowableTypeName" +
                    "&throwableTypeName=" + throwableTypeName;
        }

        ExceptionMessageDetails exceptionMessageDetails = null;
        try {
            final ExceptionMessage exceptionMessage = writeExceptionMessage();
            final Map<String, Collection<ExceptionMessageDetails>> allExceptionMessageDetails =
                    exceptionAccessor().readAllExceptionMessageDetails();
            exceptionMessageDetails = findExceptionMessageDetails1(allExceptionMessageDetails, exceptionMessage);

            final MapOfStringListOfStringsValue result = httpGetJson(url, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();
            final ExceptionMessageDetails foundExceptionMessageDetails =
                    findExceptionMessageDetails(resultMap, exceptionMessage);
            checkNotNull(foundExceptionMessageDetails, testName, "foundExceptionMessageDetails");
            checkRedactedExceptionMessageDetails(
                    exceptionMessageDetails, foundExceptionMessageDetails, testName);
        } finally {
            removeExceptionMessage(exceptionMessageDetails);
        }

        endTest(testType, testName);
    }

    public void getExceptionListenersByExceptionTypeMapTestImpl() throws Exception {
        final String testName = "getExceptionListenersByExceptionTypeMapTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionListenersByExceptionTypeMap";
        } else {

            url = servletUrl + "?operation=exceptionListenersByExceptionTypeMap";
        }

        exceptionService().clearExceptionListenersByExceptionTypeMap();
        final DummyExceptionListener dummyExceptionListener = new DummyExceptionListener();
        exceptionService().registerExceptionListenerByExceptionType(IOException.class, dummyExceptionListener);

        try {
            final MapOfStringListOfStringsValue result = httpGetJson(url, MapOfStringListOfStringsValue.class);
            final Map<String, ListOfStringsValue> resultMap = result.getValue();
            checkNotNull(resultMap, testName, "resultMap");
            checkEquals(1, resultMap.size(), testName, "resultMap.size");

            final ListOfStringsValue listOfStringsValue = resultMap.get(IOException.class.getName());
            final List<String> values = listOfStringsValue.getValue();
            checkNotNull(values, testName, "values");
            checkEquals(1, values.size(), testName, "values.size");

            final String exceptionListenerTypeName = values.get(0);
            checkEquals(DummyExceptionListener.class.getName(), exceptionListenerTypeName, testName, "");
        } finally {
            exceptionService().clearExceptionListenersByExceptionTypeMap();
        }

        endTest(testType, testName);
    }

    public void getGlobalExceptionListenersTestImpl() throws Exception {
        final String testName = "getGlobalExceptionListenersTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/globalExceptionListeners";
        } else {
            url = servletUrl + "?operation=globalExceptionListeners";
        }

        exceptionService().clearGlobalExceptionListeners();
        final DummyExceptionListener dummyExceptionListener = new DummyExceptionListener();
        exceptionService().registerGlobalExceptionListener(dummyExceptionListener);

        try {
            final ListOfStringsValue result = httpGetJson(url, ListOfStringsValue.class);
            final List<String> resultList = result.getValue();
            checkNotNull(resultList, testName, "values");
            checkEquals(1, resultList.size(), testName, "values.size");

            final String exceptionListenerTypeName = resultList.get(0);
            checkEquals(DummyExceptionListener.class.getName(),
                    exceptionListenerTypeName,
                    testName,
                    "exceptionListenerTypeName");
        } finally {
            exceptionService().clearGlobalExceptionListeners();
        }

        endTest(testType, testName);
    }

    public void getExceptionStoreFactoryByConfigurationTypeNameMapTestImpl() throws Exception {
        final String testName = "getExceptionStoreFactoryByConfigurationTypeNameMapTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionStoreFactoryByConfigurationTypeNameMap";
        } else {
            url = servletUrl + "?operation=exceptionStoreFactoryByConfigurationTypeNameMap";
        }

        exceptionAccessor().clearExceptionStoreFactoryByConfigurationTypeMap();
        final DummyExceptionStoreFactory dummyExceptionStoreFactory = new DummyExceptionStoreFactory();
        exceptionAccessor().registerExceptionStoreFactoryByConfigurationType(
                DummyExceptionStoreConfiguration.class, dummyExceptionStoreFactory);

        try {
            final MapOfStringStringValue result = httpGetJson(url, MapOfStringStringValue.class);
            final Map<String, String> resultMap = result.getValue();
            checkNotNull(resultMap, testName, "resultMap");
            checkEquals(1, resultMap.size(), testName, "resultMap.size");

            final String actualExceptionStoreFactoryTypeName =
                    resultMap.get(DummyExceptionStoreConfiguration.class.getName());
            checkEquals(DummyExceptionStoreFactory.class.getName(),
                    actualExceptionStoreFactoryTypeName,
                    testName,
                    "exceptionStoreFactoryTypeName");
        } finally {
            exceptionAccessor().clearExceptionStoreFactoryByConfigurationTypeMap();
        }

        endTest(testType, testName);
    }

    public void getExceptionStoresTestImpl() throws Exception {
        final String testName = "getExceptionStoresTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionStores";
        } else {
            url = servletUrl + "?operation=exceptionStores";
        }

        final Collection<ExceptionStore> exceptionStores = exceptionAccessor().getExceptionStores();
        boolean containsExceptionStores = false;
        for (final ExceptionStore exceptionStore : exceptionStores) {
            if (exceptionStore.getClass().isAssignableFrom(DummyExceptionStore.class)) {
                containsExceptionStores = true;
            }
        }
        checkFalse(containsExceptionStores, testName, "containsExceptionStores");

        exceptionAccessor().registerExceptionStore(new DummyExceptionStore());
        try {
            final ListOfStringsValue result = httpGetJson(url, ListOfStringsValue.class);
            final List<String> values = result.getValue();
            checkNotNull(values, testName, "values");
            checkEquals(2, values.size(), testName, "values.size");

            boolean containsDummyExceptionStore = false;
            for (final String actualClassName : values) {
                if (actualClassName.equals(DummyExceptionStore.class.getName())) {
                    containsDummyExceptionStore = true;
                }
            }
            checkTrue(containsDummyExceptionStore, testName, "containsDummyExceptionStore");
        } finally {
            exceptionAccessor().unregisterExceptionStore(DummyExceptionStore.class);
        }

        endTest(testType, testName);
    }


    // ..............................
    // DELETE tests .....
    // ..............................

    public void removeExceptionMessageDetailsByIdTestImpl() throws Exception {
        final String testName = "removeExceptionMessageDetailsByIdTest";
        startTest(testType, testName);

        final ExceptionMessage exceptionMessage = writeExceptionMessage();
        final Map<String, Collection<ExceptionMessageDetails>> allExceptionMessageDetails =
                exceptionAccessor().readAllExceptionMessageDetails();
        final ExceptionMessageDetails exceptionMessageDetails =
                findExceptionMessageDetails1(allExceptionMessageDetails, exceptionMessage);
        final Integer id = exceptionMessageDetails.getId();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionMessageDetails/id/" + id;
        } else {
            url = servletUrl + "?operation=removeExceptionMessageDetailsById" + "&id=" + id;
        }

        try {
            httpDelete(url);

            final Map<String, Collection<ExceptionMessageDetails>> allExceptionMessageDetails1 =
                    exceptionAccessor().readAllExceptionMessageDetails();
            final ExceptionMessageDetails exceptionMessageDetails1 =
                    findExceptionMessageDetails1(allExceptionMessageDetails1, exceptionMessage);
            checkNull(exceptionMessageDetails1, testName, "exceptionMessageDetails1");
        } finally {
            removeExceptionMessage(exceptionMessageDetails);
        }

        endTest(testType, testName);
    }

    public void removeExceptionMessageDetailsExclusiveBeforeTestImpl() throws Exception {
        final String testName = "removeExceptionMessageDetailsExclusiveBeforeTest";
        startTest(testType, testName);

        exceptionAccessor().removeExceptionMessageDetailsExclusiveBefore(System.currentTimeMillis());

        final ExceptionMessage exceptionMessage1 = writeExceptionMessage();
        final long timestamp1 = exceptionMessage1.getTimestamp() - 1000;
        Thread.sleep(1000);
        final ExceptionMessage exceptionMessage2 = writeExceptionMessage();
        final long timestamp2 = exceptionMessage2.getTimestamp() + 1000;

        final String url;
        try {
            if (isRestApiTest()) {
                url = resourceUrl + "/exceptionMessageDetails/timestamp/" + timestamp2;
            } else {
                url = servletUrl + "?operation=removeExceptionMessageDetailsExclusiveBefore&timestamp=" + timestamp2;
            }
            httpDelete(url);

            final Map<String, Collection<ExceptionMessageDetails>> result =
                    exceptionAccessor().readAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp2);

            boolean empty = true;
            for (final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection : result.values()) {
                if (!exceptionMessageDetailsCollection.isEmpty()) {
                    exceptionMessageDetailsCollection.forEach(exceptionMessageDetails -> {
                        logger.warn("Unexpected exceptionMessageDetails={}", exceptionMessageDetails);
                    });
                    empty = false;
                    break;
                }
            }

            checkTrue(empty, testName, "empty");
        } finally {
            exceptionAccessor().removeExceptionMessageDetailsExclusiveBefore(timestamp1);
        }

        endTest(testType, testName);
    }

    public void unregisterExceptionListenerByExceptionTypeTestImpl() throws Exception {
        final String testName = "unregisterExceptionListenerByExceptionTypeTest";
        startTest(testType, testName);

        final String url;
        final String exceptionListenerTypeName = DummyExceptionListener.class.getName();
        final String exceptionTypeName = IOException.class.getName();
        if (isRestApiTest()) {
            url = resourceUrl +
                    "/exceptionListener/" + exceptionListenerTypeName +
                    "/exceptionType/" + exceptionTypeName;
        } else {
            url = servletUrl + "?operation=unregisterExceptionListenerByExceptionType" +
                    "&exceptionListenerTypeName=" + exceptionListenerTypeName +
                    "&exceptionTypeName=" + exceptionTypeName;
        }

        final DummyExceptionListener dummyExceptionListener = new DummyExceptionListener();
        exceptionService().registerExceptionListenerByExceptionType(IOException.class, dummyExceptionListener);
        final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap1 =
                exceptionService().getExceptionListenersByExceptionTypeMap();
        checkNotNull(exceptionListenersByExceptionTypeMap1, testName, "exceptionListenersByExceptionTypeMap1");
        checkTrue(exceptionListenersByExceptionTypeMap1.containsKey(exceptionTypeName),
                testName,
                "exceptionListenersByExceptionTypeMap1.containsKey");

        try {
            httpDelete(url);

            final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap2 =
                    exceptionService().getExceptionListenersByExceptionTypeMap();
            checkNotNull(exceptionListenersByExceptionTypeMap2, testName, "exceptionListenersByExceptionTypeMap2");
            checkFalse(exceptionListenersByExceptionTypeMap2.containsKey(exceptionTypeName),
                    testName,
                    "exceptionListenersByExceptionTypeMap2.containsKey");
        } finally {
            exceptionService()
                    .unregisterExceptionListenerByExceptionType(IOException.class, DummyExceptionListener.class);
        }

        endTest(testType, testName);
    }

    public void unregisterGlobalExceptionListenerTestImpl() throws Exception {
        final String testName = "unregisterGlobalExceptionListenerTest";
        startTest(testType, testName);

        final String exceptionListenerTypeName = DummyExceptionListener.class.getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/globalExceptionListeners/" + exceptionListenerTypeName;
        } else {
            url = servletUrl + "?operation=unregisterGlobalExceptionListener" +
                    "&exceptionListenerTypeName=" + exceptionListenerTypeName;
        }

        exceptionService().unregisterGlobalExceptionListener(DummyExceptionListener.class);
        final Collection<ExceptionListener> globalExceptionListeners1 =
                exceptionService().getGlobalExceptionListeners();
        checkFalse(containsExceptionListener(globalExceptionListeners1, DummyExceptionListener.class),
                testName,
                "containsExceptionListener1");

        exceptionService().registerGlobalExceptionListener(new DummyExceptionListener());
        final Collection<ExceptionListener> globalExceptionListeners2 =
                exceptionService().getGlobalExceptionListeners();
        checkTrue(containsExceptionListener(globalExceptionListeners2, DummyExceptionListener.class),
                testName,
                "containsExceptionListener2");

        try {
            httpDelete(url);

            final Collection<ExceptionListener> globalExceptionListeners3 =
                    exceptionService().getGlobalExceptionListeners();
            checkFalse(containsExceptionListener(globalExceptionListeners3, DummyExceptionListener.class),
                    testName,
                    "containsExceptionListener3");
        } finally {
            exceptionService().unregisterGlobalExceptionListener(DummyExceptionListener.class);
        }

        endTest(testType, testName);
    }

    public void unregisterExceptionStoreFactoryByConfigurationTypeTestImpl() throws Exception {
        final String testName = "unregisterExceptionStoreFactoryByConfigurationTypeTest";
        startTest(testType, testName);

        final String exceptionStoreConfigurationTypeName = DummyExceptionStoreConfiguration.class.getName();
        final String exceptionStoreFactoryTypeName = DummyExceptionStoreFactory.class.getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionStoreFactory/" + exceptionStoreFactoryTypeName +
                    "/configurationType/" + exceptionStoreConfigurationTypeName;
        } else {
            url = servletUrl + "?operation=unregisterExceptionStoreFactoryByConfigurationType" +
                    "&exceptionStoreConfigurationTypeName=" + exceptionStoreConfigurationTypeName +
                    "&exceptionStoreFactoryTypeName=" + exceptionStoreFactoryTypeName;
        }

        exceptionAccessor().unregisterExceptionStoreFactoryByConfigurationType(
                DummyExceptionStoreConfiguration.class, DummyExceptionStoreFactory.class);
        final Map<String, ExceptionStoreFactory> map1 = exceptionAccessor()
                .getExceptionStoreFactoryByConfigurationTypeNameMap();
        checkFalse(map1.containsKey(DummyExceptionStoreConfiguration.class.getName()), testName, "containsKey1");

        final DummyExceptionStoreFactory dummyExceptionStoreFactory = new DummyExceptionStoreFactory();
        exceptionAccessor().registerExceptionStoreFactoryByConfigurationType(
                DummyExceptionStoreConfiguration.class, dummyExceptionStoreFactory);
        final Map<String, ExceptionStoreFactory> map2 = exceptionAccessor()
                .getExceptionStoreFactoryByConfigurationTypeNameMap();
        checkTrue(map2.containsKey(DummyExceptionStoreConfiguration.class.getName()), testName, "containsKey2");

        try {
            httpDelete(url);

            final Map<String, ExceptionStoreFactory> map3 = exceptionAccessor()
                    .getExceptionStoreFactoryByConfigurationTypeNameMap();
            checkFalse(map3.containsKey(DummyExceptionStoreConfiguration.class.getName()), testName, "containsKey3");
        } finally {
            exceptionAccessor().unregisterExceptionStoreFactoryByConfigurationType(
                    DummyExceptionStoreConfiguration.class, DummyExceptionStoreFactory.class);
        }

        endTest(testType, testName);
    }

    public void unregisterExceptionStoreTestImpl() throws Exception {
        final String testName = "unregisterExceptionStoreTest";
        startTest(testType, testName);

        final String exceptionStoreTypeName = DummyExceptionStore.class.getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionStores/" + exceptionStoreTypeName;
        } else {
            url = servletUrl + "?operation=unregisterExceptionStore" +
                    "&exceptionStoreTypeName=" + exceptionStoreTypeName;
        }

        exceptionAccessor().unregisterExceptionStore(DummyExceptionStore.class);
        final Collection<ExceptionStore> exceptionStores1 = exceptionAccessor().getExceptionStores();
        checkFalse(containsExceptionStore(exceptionStores1, DummyExceptionStore.class), testName, "containsKey1");

        final DummyExceptionStore dummyExceptionStore = new DummyExceptionStore();
        exceptionAccessor().registerExceptionStore(dummyExceptionStore);
        final Collection<ExceptionStore> exceptionStores2 = exceptionAccessor().getExceptionStores();
        checkTrue(containsExceptionStore(exceptionStores2, DummyExceptionStore.class), testName, "containsKey2");

        try {
            httpDelete(url);

            final Collection<ExceptionStore> exceptionStores3 = exceptionAccessor().getExceptionStores();
            checkFalse(containsExceptionStore(exceptionStores3, DummyExceptionStore.class), testName, "containsKey3");
        } finally {
            exceptionAccessor().unregisterExceptionStore(DummyExceptionStore.class);
        }

        endTest(testType, testName);
    }

    public void clearExceptionListenersByExceptionTypeMapTestImpl() throws Exception {
        final String testName = "clearExceptionListenersByExceptionTypeMapTest";
        startTest(testType, testName);

        final String exceptionListenerTypeName = DummyExceptionListener.class.getName();
        final String exceptionTypeName = IOException.class.getName();
        final DummyExceptionListener dummyExceptionListener = new DummyExceptionListener();
        exceptionService().registerExceptionListenerByExceptionType(IOException.class, dummyExceptionListener);
        final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap1 =
                exceptionService().getExceptionListenersByExceptionTypeMap();
        checkNotNull(exceptionListenersByExceptionTypeMap1, testName, "exceptionListenersByExceptionTypeMap1");
        checkTrue(exceptionListenersByExceptionTypeMap1.containsKey(exceptionTypeName),
                testName,
                "exceptionListenersByExceptionTypeMap1.containsKey");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionListener/exceptionType";
        } else {
            url = servletUrl + "?operation=clearExceptionListenerByExceptionTypeMap";
        }

        try {
            httpDelete(url);

            final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap2 =
                    exceptionService().getExceptionListenersByExceptionTypeMap();
            checkNotNull(exceptionListenersByExceptionTypeMap2, testName, "exceptionListenersByExceptionTypeMap2");
            checkTrue(exceptionListenersByExceptionTypeMap2.isEmpty(),
                    testName,
                    "exceptionListenersByExceptionTypeMap2.empty");
        } finally {
            exceptionService().clearExceptionListenersByExceptionTypeMap();
        }

        endTest(testType, testName);
    }

    public void clearGlobalExceptionListenersTestImpl() throws Exception {
        final String testName = "clearGlobalExceptionListenersTest";
        startTest(testType, testName);

        exceptionService().unregisterGlobalExceptionListener(DummyExceptionListener.class);
        final Collection<ExceptionListener> globalExceptionListeners1 =
                exceptionService().getGlobalExceptionListeners();
        checkFalse(containsExceptionListener(globalExceptionListeners1, DummyExceptionListener.class),
                testName,
                "containsExceptionListener1");

        exceptionService().registerGlobalExceptionListener(new DummyExceptionListener());
        final Collection<ExceptionListener> globalExceptionListeners2 =
                exceptionService().getGlobalExceptionListeners();
        checkTrue(containsExceptionListener(globalExceptionListeners2, DummyExceptionListener.class),
                testName,
                "containsExceptionListener2");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/globalExceptionListeners";
        } else {
            url = servletUrl + "?operation=clearGlobalExceptionListeners";
        }

        try {
            httpDelete(url);

            final Collection<ExceptionListener> globalExceptionListeners3 =
                    exceptionService().getGlobalExceptionListeners();
            checkTrue(globalExceptionListeners3.isEmpty(), testName, "globalExceptionListeners3.empty");
        } finally {
            exceptionService().clearGlobalExceptionListeners();
        }

        endTest(testType, testName);
    }

    public void clearExceptionStoreFactoryByConfigurationTypeMapTestImpl() throws Exception {
        final String testName = "clearExceptionStoreFactoryByConfigurationTypeMapTest";
        startTest(testType, testName);

        final DummyExceptionStoreFactory dummyExceptionStoreFactory = new DummyExceptionStoreFactory();
        exceptionAccessor().registerExceptionStoreFactoryByConfigurationType(
                DummyExceptionStoreConfiguration.class, dummyExceptionStoreFactory);
        final Map<String, ExceptionStoreFactory> map2 = exceptionAccessor()
                .getExceptionStoreFactoryByConfigurationTypeNameMap();
        checkTrue(map2.containsKey(DummyExceptionStoreConfiguration.class.getName()), testName, "containsKey2");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionStoreFactory/configurationType";
        } else {
            url = servletUrl + "?operation=clearExceptionStoreFactoryByConfigurationTypeMap";
        }

        try {
            httpDelete(url);

            final Map<String, ExceptionStoreFactory> map3 = exceptionAccessor()
                    .getExceptionStoreFactoryByConfigurationTypeNameMap();
            checkTrue(map3.isEmpty(), testName, "map3.empty");
        } finally {
            exceptionAccessor().clearExceptionStoreFactoryByConfigurationTypeMap();
        }

        endTest(testType, testName);
    }

    public void clearExceptionStoresTestImpl() throws Exception {
        final String testName = "clearExceptionStoresTest";
        startTest(testType, testName);

        final DummyExceptionStore dummyExceptionStore = new DummyExceptionStore();
        exceptionAccessor().registerExceptionStore(dummyExceptionStore);
        final Collection<ExceptionStore> exceptionStores2 = exceptionAccessor().getExceptionStores();
        checkTrue(containsExceptionStore(exceptionStores2, DummyExceptionStore.class), testName, "containsKey2");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/exceptionStores";
        } else {
            url = servletUrl + "?operation=clearExceptionStores";
        }

        try {
            httpDelete(url);

            final Collection<ExceptionStore> exceptionStores3 = exceptionAccessor().getExceptionStores();
            checkTrue(exceptionStores3.isEmpty(), testName, "exceptionStores3.isEmpty");
        } finally {
            exceptionAccessor().clearExceptionStores();
            exceptionAccessor().registerExceptionStore(
                    new InMemoryExceptionStore(InMemoryExceptionStoreConfiguration.builder().build()));
        }

        endTest(testType, testName);
    }


    // ..............................
    // COMMON tests .....
    // ..............................

    public void statusTestImpl() throws Exception {
        final String testName = "statusTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/status";
        } else {
            url = servletUrl + "?operation=status";
        }

        final String status = exceptionService().getStatus().name();
        final String status1 = httpGetText(url, String.class);
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void reloadTestImpl() throws Exception {
        final String testName = "reloadTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final String status = exceptionService().getStatus().name();
        final int statusCode = httpPutEmpty(url);
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

        final String status1 = exceptionService().getStatus().name();
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void reloadWithConfigTestImpl() throws Exception {
        final String testName = "reloadWithConfigTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final String status = exceptionService().getStatus().name();
        final DefaultExceptionServiceConfiguration configuration = exceptionService().getConfiguration();
        final String json = serialize(DefaultExceptionServiceConfiguration.class, configuration);
        httpPostJson(url, json, null);

        final String status1 = exceptionService().getStatus().name();
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void shutdownTestImpl() throws Exception {
        final String testName = "shutdownTest";
        startTest(testType, testName);

        final ExceptionService exceptionService = exceptionService();
        final DefaultExceptionServiceConfiguration configuration = exceptionService.getConfiguration();
        checkEquals(ServiceStatus.STARTED, exceptionService.getStatus(), testName, "serviceStatus");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/shutdown";
        } else {
            url = servletUrl + "?operation=shutdown";
        }

        httpPostEmpty(url);
        checkEquals(ServiceStatus.SHUTDOWN, exceptionService.getStatus(), testName, "serviceStatus1");
        appbaseServices.getCoreServices().ensureDefaultExceptionStores();
        exceptionService.reload(configuration);
        checkEquals(ServiceStatus.STARTED, exceptionService.getStatus(), testName, "serviceStatus2");

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private void checkRedactedExceptionMessageDetails(
            final ExceptionMessageDetails expected,
            final ExceptionMessageDetails actual,
            final String testName)
    {
        checkEquals(expected.getId(), actual.getId(), testName, "exceptionMessageDetails.id");
        checkEquals(expected.getDiscriminator(), actual.getDiscriminator(),
                testName, "exceptionMessageDetails.discriminator");
        checkEquals(expected.getMessage(), actual.getMessage(), testName, "exceptionMessageDetails.message");
        checkEquals(expected.getThrowableTypeName(), actual.getThrowableTypeName(),
                testName, "exceptionMessageDetails.throwableTypeName");
        checkEquals(expected.getStackTraceHash(), actual.getStackTraceHash(),
                testName, "exceptionMessageDetails.stackTraceHash");
        checkEquals(expected.getSourceTypeName(), actual.getSourceTypeName(),
                testName, "exceptionMessageDetails.sourceTypeName");
        checkEquals(expected.getThreadName(), actual.getThreadName(),
                testName, "exceptionMessageDetails.threadName");
        checkEquals(expected.getTimestamp(), actual.getTimestamp(), testName, "exceptionMessageDetails.timestamp");

        checkNull(actual.getThrowableMessage(), testName, "exceptionMessageDetails.throwableMessage");
        checkNull(actual.getStackTrace(), testName, "exceptionMessageDetails.stackTrace");
        checkNull(actual.getContext(), testName, "exceptionMessageDetails.context");
    }

    private boolean containsExceptionMessages(
            final Set<ExceptionMessageDetails> exceptionMessageDetailsSet, final ExceptionMessage... exceptionMessages)
    {
        for (final ExceptionMessage exceptionMessage : exceptionMessages) {
            boolean contains = false;
            for (final ExceptionMessageDetails exceptionMessageDetails1 : exceptionMessageDetailsSet) {
                if (exceptionMessage.getTimestamp() == exceptionMessageDetails1.getTimestamp()) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                return false;
            }
        }

        return true;
    }

    private ExceptionMessage writeExceptionMessage() throws ExceptionAccessorException {
        final ExceptionAccessor exceptionAccessor = exceptionAccessor();
        final ExceptionMessage exceptionMessage = exceptionMessage1();
        exceptionAccessor.writeExceptionMessage(exceptionMessage);

        return exceptionMessage;
    }

    private void removeExceptionMessage(
            @Nullable
            final ExceptionMessageDetails exceptionMessageDetails) throws ExceptionAccessorException
    {
        if (exceptionMessageDetails != null) {
            exceptionAccessor().removeExceptionMessageDetailsById(exceptionMessageDetails.getId());
        }
    }

    private ExceptionMessageDetails findExceptionMessageDetails1(
            @Nullable
            final Map<String, Collection<ExceptionMessageDetails>> resultMap,
            final ExceptionMessage exceptionMessage)
    {
        if (resultMap == null) {
            return null;
        }

        for (final Map.Entry<String, Collection<ExceptionMessageDetails>> entry : resultMap.entrySet()) {
            final Collection<ExceptionMessageDetails> exceptionMessageDetailCollection = entry.getValue();
            if (exceptionMessageDetailCollection != null) {
                for (final ExceptionMessageDetails exceptionMessageDetails : exceptionMessageDetailCollection) {
                    if (exceptionMessage.getTimestamp() == exceptionMessageDetails.getTimestamp()) {
                        return exceptionMessageDetails;
                    }
                }
            }
        }

        return null;
    }

    private Map.Entry<String, ExceptionMessageDetails> findExceptionMessageDetailsEntry(
            @Nullable
            final Map<String, Collection<ExceptionMessageDetails>> resultMap,
            final ExceptionMessage exceptionMessage)
    {
        if (resultMap == null) {
            return null;
        }

        for (final Map.Entry<String, Collection<ExceptionMessageDetails>> entry : resultMap.entrySet()) {
            final Collection<ExceptionMessageDetails> detailsCollection = entry.getValue();
            if (detailsCollection != null) {
                for (final ExceptionMessageDetails details : detailsCollection) {
                    if (exceptionMessage.getTimestamp() == details.getTimestamp()) {
                        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), details);
                    }
                }
            }
        }

        return null;
    }

    private ExceptionMessageDetails findExceptionMessageDetails(
            @Nullable
            final Map<String, ListOfStringsValue> resultMap,
            final ExceptionMessage exceptionMessage) throws JsonProcessingException
    {
        if (resultMap == null) {
            return null;
        }

        for (final Map.Entry<String, ListOfStringsValue> entry : resultMap.entrySet()) {
            final ListOfStringsValue jsonList = entry.getValue();
            final List<String> jsons = jsonList.getValue();
            if (jsons != null) {
                for (final String json : jsons) {
                    final ExceptionMessageDetails exceptionMessageDetails = JsonUtil
                            .getJsonMapper()
                            .readerFor(ExceptionMessageDetails.class)
                            .readValue(json);
                    if (exceptionMessage.getTimestamp() == exceptionMessageDetails.getTimestamp()) {
                        return exceptionMessageDetails;
                    }
                }
            }
        }

        return null;
    }

    private Set<ExceptionMessageDetails> exceptionMessageDetails(
            @Nullable
            final Map<String, ListOfStringsValue> resultMap) throws JsonProcessingException {
        if (resultMap == null) {
            return  null;
        }

        final Set<ExceptionMessageDetails> exceptionMessageDetailsSet = new HashSet<>();
        for (final ListOfStringsValue listOfStringsValue : resultMap.values()) {
            final List<String> jsons = listOfStringsValue.getValue();
            if (jsons != null) {
                for (final String json : jsons) {
                    final ExceptionMessageDetails exceptionMessageDetails = JsonUtil
                            .getJsonMapper()
                            .readerFor(ExceptionMessageDetails.class)
                            .readValue(json);
                    exceptionMessageDetailsSet.add(exceptionMessageDetails);
                }
            }
        }

        return exceptionMessageDetailsSet;
    }

    private ExceptionMessage exceptionMessage1() {
        final ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setMessage("Message1");
        exceptionMessage.setThrowable(new IOException());
        exceptionMessage.setSourceType(getClass());
        exceptionMessage.setThreadName(Thread.currentThread().getName());
        exceptionMessage.setTimestamp(System.currentTimeMillis());

        return exceptionMessage;
    }

    private ExceptionService exceptionService() {
        return appbaseServices.getCoreServices().appbaseCore().getExceptionService();
    }

    private ExceptionAccessor exceptionAccessor() {
        return exceptionService().getExceptionAccessor();
    }

    private boolean isRestApiTest() {
        return ExceptionJaxrsResourceTest.class.isAssignableFrom(testType);
    }

    private boolean containsExceptionListener(
            final Collection<ExceptionListener> exceptionListeners,
            final Class<? extends ExceptionListener> exceptionListenerType)
    {
        for (final ExceptionListener exceptionListener : exceptionListeners) {
            if (exceptionListenerType.isAssignableFrom(exceptionListener.getClass())) {
                return true;
            }
        }

        return false;
    }

    private boolean containsExceptionStore(
            final Collection<ExceptionStore> exceptionStores,
            final Class<? extends ExceptionStore> exceptionStoreTypeName)
    {
        for (final ExceptionStore exceptionStore : exceptionStores) {
            if (exceptionStoreTypeName.isAssignableFrom(exceptionStore.getClass())) {
                return true;
            }
        }

        return false;
    }
}
