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

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringServiceStatusValue;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.dummytypes.DummyAppbaseService;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.ServiceRegistryJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.ServiceRegistryServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ServiceRegistryEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public ServiceRegistryEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, ServiceRegistryJaxrsResourceTest.class, ServiceRegistryServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_SERVLET_PATH;
    }

    public void pollAndUpdateStatusesTestImpl() throws Exception
    {
        final String testName = "pollAndUpdateStatusesTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/pollAndUpdateStatuses";
        } else {
            url = servletUrl + "?operation=pollAndUpdateStatuses";
        }

        final ServiceRegistry.OverallStatus overallStatus1 = serviceRegistry().pollOverallStatus();
        httpPostEmpty(url);
        final ServiceRegistry.OverallStatus overallStatus2 = serviceRegistry().pollOverallStatus();
        checkOverallStatuses(overallStatus1, overallStatus2, testName);

        endTest(testType, testName);
    }

    public void overallStatusTestImpl() throws Exception {
        final String testName = "overallStatusTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/overallStatus";
        } else {
            url = servletUrl + "?operation=overallStatus";
        }

        final String json = httpGetJson(url, String.class);
        final ServiceRegistry.OverallStatus overallStatus1 = deserialize(ServiceRegistry.OverallStatus.class, json);
        final ServiceRegistry.OverallStatus overallStatus2 = serviceRegistry().pollOverallStatus();
        checkOverallStatuses(overallStatus1, overallStatus2, testName);

        endTest(testType, testName);
    }

    public void pollOverallStatusImpl() throws Exception {
        final String testName = "pollOverallStatus";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/pollOverallStatus";
        } else {
            url = servletUrl + "?operation=pollOverallStatus";
        }

        final String json = httpPostEmpty(url, String.class);
        final ServiceRegistry.OverallStatus overallStatus1 = deserialize(ServiceRegistry.OverallStatus.class, json);
        final ServiceRegistry.OverallStatus overallStatus2 = serviceRegistry().pollOverallStatus();
        checkOverallStatuses(overallStatus1, overallStatus2, testName);

        endTest(testType, testName);
    }

    public void getAppbaseServiceStatusTestImpl() throws Exception {
        final String testName = "getAppbaseServiceStatusTest";
        startTest(testType, testName);

        final DummyAppbaseService dummyAppbaseService = new DummyAppbaseService();
        serviceRegistry().registerAppbaseService(dummyAppbaseService);

        final String appbaseServiceTypeName = dummyAppbaseService.getClass().getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/appbaseServiceStatuses/" + appbaseServiceTypeName;
        } else {
            url = servletUrl + "?operation=appbaseServiceStatus" + "&appbaseServiceTypeName=" + appbaseServiceTypeName;
        }

        try {
            final int code1 = httpHead(url);
            checkEquals(HttpServletResponse.SC_OK, code1, testName, "code");

            dummyAppbaseService.shutdown();
            serviceRegistry().pollAndUpdateStatuses();
            final int code2 = httpHead(url);
            checkEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, code2, testName, "code");
        } finally {
            serviceRegistry().unregisterAppbaseService(DummyAppbaseService.class);
        }

        endTest(testType, testName);
    }

    public void getAllServiceStatusByServiceTypeNameTestImpl() throws Exception {
        final String testName = "getAllServiceStatusByServiceTypeNameTest";
        startTest(testType, testName);

        final DummyAppbaseService dummyAppbaseService = new DummyAppbaseService();
        serviceRegistry().registerAppbaseService(dummyAppbaseService);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/appbaseServiceStatuses";
        } else {
            url = servletUrl + "?operation=appbaseServiceStatuses";
        }

        try {
            final MapOfStringServiceStatusValue result1 = httpGetJson(url, MapOfStringServiceStatusValue.class);
            final Map<String, ServiceStatus> resultMap1 = result1.getServiceStatusByNameMap();
            checkNotNull(resultMap1, testName, "resultMap1");
            checkTrue(resultMap1.containsKey(DummyAppbaseService.class.getName()), testName, "resultMap1.containsKey");
            checkEquals(ServiceStatus.STARTED,
                    resultMap1.get(DummyAppbaseService.class.getName()),
                    testName,
                    "ServiceStatus.STARTED");

            dummyAppbaseService.shutdown();
            serviceRegistry().pollAndUpdateStatuses();
            final MapOfStringServiceStatusValue result2 = httpGetJson(url, MapOfStringServiceStatusValue.class);
            final Map<String, ServiceStatus> resultMap2 = result2.getServiceStatusByNameMap();
            checkNotNull(resultMap2, testName, "resultMap2");
            checkTrue(resultMap2.containsKey(DummyAppbaseService.class.getName()), testName, "resultMap2.containsKey");
            checkEquals(ServiceStatus.SHUTDOWN,
                    resultMap2.get(DummyAppbaseService.class.getName()),
                    testName,
                    "ServiceStatus.SHUTDOWN");
        } finally {
            serviceRegistry().unregisterAppbaseService(DummyAppbaseService.class);
        }

        endTest(testType, testName);
    }

    public void unregisterAppbaseServiceTestImpl() throws Exception {
        final String testName = "unregisterAppbaseServiceTest";
        startTest(testType, testName);

        final DummyAppbaseService dummyAppbaseService = new DummyAppbaseService();
        serviceRegistry().registerAppbaseService(dummyAppbaseService);
        final ServiceRegistry.OverallStatus overallStatus1 = serviceRegistry().pollOverallStatus();
        checkTrue(overallStatus1.getStartedServiceNames().contains(DummyAppbaseService.class.getName()),
                testName,
                "overallStatus1.startedServiceNames.contains");

        final String appbaseServiceTypeName = dummyAppbaseService.getClass().getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/appbaseServices/" + appbaseServiceTypeName;
        } else {
            url = servletUrl + "?operation=unregisterAppbaseService&appbaseServiceTypeName=" + appbaseServiceTypeName;
        }

        try {
            httpDelete(url);

            final ServiceRegistry.OverallStatus overallStatus2 = serviceRegistry().pollOverallStatus();
            checkFalse(overallStatus2.getStartedServiceNames().contains(DummyAppbaseService.class.getName()),
                    testName,
                    "overallStatus2.startedServiceNames.contains");
        } finally {
            serviceRegistry().unregisterAppbaseService(DummyAppbaseService.class);
        }

        endTest(testType, testName);
    }

    public void clearAppbaseServicesTestImpl() throws Exception {
        /*
        FIXME If this is enabled, UserAccountApiTestImpl fails (maybe because FirebaseApp or FirebaseAuth is removed)?
        final String testName = "clearAppbaseServicesTest";
        startTest(testType, testName);

        final DummyAppbaseService dummyAppbaseService = new DummyAppbaseService();
        final ServiceRegistry serviceRegistry = serviceRegistry();
        serviceRegistry.registerAppbaseService(dummyAppbaseService);
        final Collection<AppbaseServiceEntry> appbaseServiceEntries1 = serviceRegistry.getAppbaseServices();
        checkNotNull(appbaseServiceEntries1, testName, "appbaseServiceEntries1");
        checkTrue(containsAppbaseService(appbaseServiceEntries1, dummyAppbaseService),
                testName,
                "containsAppbaseService1");

        try {
            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/appbaseServices";
            } else {
                url = servletUrl + "?operation=clearAppbaseServices";
            }

            httpDelete(url);
            final Collection<AppbaseServiceEntry> appbaseServiceEntries2 = serviceRegistry.getAppbaseServices();
            checkNotNull(appbaseServiceEntries2, testName, "appbaseServiceEntries2");
            checkTrue(appbaseServiceEntries2.isEmpty(), testName, "appbaseServiceEntries2.empty");
            checkFalse(containsAppbaseService(appbaseServiceEntries2, dummyAppbaseService),
                    testName,
                    "containsAppbaseService2");
        } finally {
            for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServiceEntries1) {
                final AppbaseService appbaseService = appbaseServiceEntry.getService();
                if (!appbaseService.getClass().isAssignableFrom(dummyAppbaseService.getClass())) {
                    serviceRegistry.registerAppbaseService(appbaseService);
                }
            }
        }

        endTest(testType, testName);
        */
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

        final String status = serviceRegistry().getStatus().name();
        final String status1 = httpGetText(url, String.class);
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void reloadTestImpl() throws Exception {
        /*
        FIXME If this is enabled, UserAccountApiTestImpl fails (maybe because FirebaseApp or FirebaseAuth is removed)?
        final String testName = "reloadTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final ServiceRegistry serviceRegistry = serviceRegistry();
        final Collection<AppbaseServiceEntry> appbaseServiceEntries = serviceRegistry.getAppbaseServices();
        try {
            final String status = serviceRegistry.getStatus().name();
            final int statusCode = httpPutEmpty(url);
            checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

            final String status1 = serviceRegistry.getStatus().name();
            checkEquals(status1, status, testName, "status1");
        } finally {
            for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServiceEntries) {
                final AppbaseService appbaseService = appbaseServiceEntry.getService();
                serviceRegistry.registerAppbaseService(appbaseService);
            }
        }

        endTest(testType, testName);
        */
    }

    public void reloadWithConfigTestImpl() throws Exception {
        /*
        FIXME If this is enabled, UserAccountApiTestImpl fails (maybe because FirebaseApp or FirebaseAuth is removed)?
        final String testName = "reloadWithConfigTest";
        startTest(testType, testName);

        final ServiceRegistry serviceRegistry = serviceRegistry();
        final Collection<AppbaseServiceEntry> appbaseServiceEntries = serviceRegistry.getAppbaseServices();

        final String status = serviceRegistry.getStatus().name();
        final DefaultServiceRegistryConfiguration configuration = serviceRegistry.getConfiguration();
        final String json = serialize(DefaultServiceRegistryConfiguration.class, configuration);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        try {
            httpPostJson(url, json, null);

            final String status1 = serviceRegistry.getStatus().name();
            checkEquals(status1, status, testName, "status1");
        } finally {
            for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServiceEntries) {
                final AppbaseService appbaseService = appbaseServiceEntry.getService();
                serviceRegistry.registerAppbaseService(appbaseService);
            }
        }

        endTest(testType, testName);
        */
    }

    public void shutdownTestImpl() throws Exception {
        /*
        FIXME If this is enabled, UserAccountApiTestImpl fails (maybe because FirebaseApp or FirebaseAuth is removed)?
        final String testName = "shutdownTest";
        startTest(testType, testName);

        final ServiceRegistry serviceRegistry = serviceRegistry();
        final Collection<AppbaseServiceEntry> appbaseServiceEntries = serviceRegistry.getAppbaseServices();
        final DefaultServiceRegistryConfiguration configuration = serviceRegistry().getConfiguration();
        checkEquals(ServiceStatus.STARTED, serviceRegistry.getStatus(), testName, "serviceStatus");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/shutdown";
        } else {
            url = servletUrl + "?operation=shutdown";
        }

        try {
            httpPostEmpty(url);
            checkEquals(ServiceStatus.SHUTDOWN, serviceRegistry.getStatus(), testName, "serviceStatus1");

            serviceRegistry.reload(configuration);
            checkEquals(ServiceStatus.STARTED, serviceRegistry.getStatus(), testName, "serviceStatus2");
        } finally {
            for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServiceEntries) {
                final AppbaseService appbaseService = appbaseServiceEntry.getService();
                serviceRegistry.registerAppbaseService(appbaseService);
            }
        }

        endTest(testType, testName);
        */
    }


    // ..............................
    // GET tests .....
    // ..............................

    private boolean containsAppbaseService(
            final Collection<AppbaseServiceEntry> appbaseServiceEntries,
            final AppbaseService appbaseService)
    {
        for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServiceEntries) {
            if (appbaseServiceEntry.getService().getClass().isAssignableFrom(appbaseService.getClass())) {
                return true;
            }
        }

        return false;
    }

    private void checkOverallStatuses(
            final ServiceRegistry.OverallStatus actualOverallStatus,
            final ServiceRegistry.OverallStatus expectedOverallStatus,
            final String testName)
    {
        final Map<String, ServiceStatus> notStartedServiceStatusByNameMap1 =
                actualOverallStatus.getNotStartedServiceStatusByNameMap();
        final Map<String, ServiceStatus> notStartedServiceStatusByNameMap2 =
                expectedOverallStatus.getNotStartedServiceStatusByNameMap();
        checkNotNull(notStartedServiceStatusByNameMap1, testName, "notStartedServiceStatusByNameMap1");
        checkNotNull(notStartedServiceStatusByNameMap2, testName, "notStartedServiceStatusByNameMap2");
        checkEquals(
                notStartedServiceStatusByNameMap1,
                notStartedServiceStatusByNameMap2,
                testName,
                "notStartedServiceStatusByNameMap");

        final Set<String> startedServiceNames1 = actualOverallStatus.getStartedServiceNames();
        final Set<String> startedServiceNames2 = expectedOverallStatus.getStartedServiceNames();
        checkNotNull(startedServiceNames1, testName, "startedServiceNames1");
        checkNotNull(startedServiceNames2, testName, "startedServiceNames2");
        checkEquals(startedServiceNames1, startedServiceNames2, testName, "startedServiceNames");
    }

    private ServiceRegistry serviceRegistry() {
        return appbaseServices.getCoreServices().appbaseCore().getServiceRegistry();
    }

    private boolean isRestApiTest() {
        return ServiceRegistryJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
