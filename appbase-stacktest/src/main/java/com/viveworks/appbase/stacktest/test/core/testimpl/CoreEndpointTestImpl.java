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
package com.viveworks.appbase.stacktest.test.core.testimpl;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.dummytypes.DummyAppbaseService;
import com.viveworks.appbase.stacktest.dummytypes.DummyAppbaseServiceConfiguration1;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.CoreJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.CoreServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CoreEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public CoreEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, CoreJaxrsResourceTest.class, CoreServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_CORE_SERVLET_PATH;
    }

    public void reloadServiceTestImpl() throws Exception {
        final String testName = "reloadServiceTest";
        startTest(testType, testName);

        final DummyAppbaseService dummyAppbaseService = new DummyAppbaseService();
        final DummyAppbaseServiceConfiguration1 dummyAppbaseServiceConfiguration1 =
                new DummyAppbaseServiceConfiguration1("localhost", 8080);
        appbaseCore().getServiceRegistry().registerAppbaseService(dummyAppbaseService);

        try {
            final String appbaseServiceTypeName = dummyAppbaseService.getClass().getName();
            final String appbaseServiceConfigurationTypeName = dummyAppbaseServiceConfiguration1.getClass().getName();

            checkFalse(dummyAppbaseService.isShutdown(), testName, "dummyAppbaseService.shutdown");
            checkFalse(dummyAppbaseService.isReloaded(), testName, "dummyAppbaseService.reloaded");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/reloadService/" + appbaseServiceTypeName + "/" + appbaseServiceConfigurationTypeName;
            } else {
                url = servletUrl + "?operation=reloadService" +
                        "&appbaseServiceTypeName=" + appbaseServiceTypeName +
                        "&appbaseServiceConfigurationTypeName=" + appbaseServiceConfigurationTypeName;
            }
            httpPost(url, dummyAppbaseServiceConfiguration1, DummyAppbaseServiceConfiguration1.class);

            checkFalse(dummyAppbaseService.isShutdown(), testName, "dummyAppbaseService.shutdown");
            checkTrue(dummyAppbaseService.isReloaded(), testName, "dummyAppbaseService.reloaded");
        } finally {
            appbaseCore().getServiceRegistry().unregisterAppbaseService(dummyAppbaseService.getClass());
        }

        endTest(testType, testName);
    }


    public void shutdownServiceTestImpl() throws Exception {
        final String testName = "shutdownServiceTest";
        startTest(testType, testName);

        final DummyAppbaseService dummyAppbaseService = new DummyAppbaseService();
        appbaseCore().getServiceRegistry().registerAppbaseService(dummyAppbaseService);

        try {
            final String appbaseServiceTypeName = dummyAppbaseService.getClass().getName();
            checkFalse(dummyAppbaseService.isShutdown(), testName, "dummyAppbaseService.shutdown");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/shutdownService/" + appbaseServiceTypeName;
            } else {
                url = servletUrl + "?operation=shutdownService" + "&appbaseServiceTypeName=" + appbaseServiceTypeName;
            }
            httpPostEmpty(url);

            checkTrue(dummyAppbaseService.isShutdown(), testName, "dummyAppbaseService.shutdown");
        } finally {
            appbaseCore().getServiceRegistry().unregisterAppbaseService(dummyAppbaseService.getClass());
        }

        endTest(testType, testName);
    }

    public void shutdownAllServicesTest() throws Exception {
        final String testName = "shutdownAllServicesTest";
        startTest(testType, testName);

        /*
        FIXME - DOES NOT WORK!
        This hangs while trying to shutdown DisruptorService. It could be due to inter-service dependencies.

        checkFalse(appbaseCore().isShutdown(), testName, "appbaseCore.shutdown");
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/shutdownAllServices";
        } else {
            url = servletUrl + "?operation=shutdownAllServices";
        }
        httpPostEmpty(url);

        //Thread.sleep(10_000);
        checkTrue(appbaseCore().isShutdown(), testName, "appbaseCore.shutdown");
        */

        endTest(testType, testName);
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

        final String status = appbaseCore().getStatus().name();
        final String status1 = httpGetText(url, String.class);
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void reloadTestImpl() throws Exception {
        final String testName = "reloadTest";
        startTest(testType, testName);

        /*
        FIXME - DOES NOT WORK!
        After this runs for the REST API test, the first test fails for the Servlet test. It is leaving appbase in an
        invalid state.

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final String status = appbaseCore().getStatus().name();
        final int statusCode = httpPutEmpty(url);
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

        //Thread.sleep(10_000);
        final String status1 = appbaseCore().getStatus().name();
        checkEquals(status1, status, testName, "status1");
        */

        endTest(testType, testName);
    }

    public void reloadWithConfigTest() throws Exception {
        final String testName = "reloadWithConfigTest";
        startTest(testType, testName);

        /*
        FIXME - CAN RESULT IN UNDEFINED STATE!
        After this runs for the REST API test, the first test fails for the Servlet test. It is leaving appbase in an
        invalid state.

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final String status = appbaseCore().getStatus().name();
        final AppbaseCoreBootstrapConfiguration appbaseBootstrapConfiguration =
                appbaseCore().getAppbaseBootstrapConfiguration();
        final String json = serialize(AppbaseCoreBootstrapConfiguration.class, appbaseBootstrapConfiguration);
        httpPostJson(url, json, null);

        final String status1 = appbaseCore().getStatus().name();
        checkEquals(status1, status, testName, "status1");
        */

        endTest(testType, testName);
    }

    public void shutdownTestImpl() throws Exception {
        final String testName = "shutdownTest";
        startTest(testType, testName);

        /*
        FIXME - CAN RESULT IN UNDEFINED STATE!
        After this runs for the REST API test, the first test fails for the Servlet test. It is leaving appbase in an
        invalid state.

        final AppbaseCore appbaseCore = appbaseCore();
        final AppbaseCoreBootstrapConfiguration appbaseBootstrapConfiguration =
                appbaseCore().getAppbaseBootstrapConfiguration();
        checkEquals(ServiceStatus.STARTED, appbaseCore.getStatus(), testName, "serviceStatus");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/shutdown";
        } else {
            url = servletUrl + "?operation=shutdown";
        }

        httpPostEmpty(url);
        checkEquals(ServiceStatus.SHUTDOWN, appbaseCore.getStatus(), testName, "serviceStatus");

        appbaseCore.reload(appbaseBootstrapConfiguration);
        checkEquals(ServiceStatus.STARTED, appbaseCore.getStatus(), testName, "serviceStatus");
        */

        endTest(testType, testName);
    }

    private AppbaseCore appbaseCore() {
        return appbaseServices.getCoreServices().appbaseCore();
    }

    private boolean isRestApiTest() {
        return CoreJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
