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

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.Value;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.MediatorJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.MediatorServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MediatorEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public MediatorEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, MediatorJaxrsResourceTest.class, MediatorServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_MEDIATOR_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_MEDIATOR_SERVLET_PATH;
    }

    public void getConfigurationFilePathTestImpl() throws Exception {
        final String testName = "getConfigurationFilePathTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/configurationFilePath";
        } else {
            url = servletUrl + "?operation=configurationFilePath";
        }

        final Value value = httpGetJson(url, Value.class);
        final String actualValue = (String) value.getValue();
        checkNull(actualValue, testName, "actualValue");

        endTest(testType, testName);
    }

    public void isInitWithDefaultConfigurationTestImpl() throws Exception {
        final String testName = "isInitWithDefaultConfigurationTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/isInitWithDefaultConfiguration";
        } else {
            url = servletUrl + "?operation=isInitWithDefaultConfiguration";
        }

        final Value value = httpGetJson(url, Value.class);
        final Boolean actualValue = (Boolean) value.getValue();
        checkNotNull(actualValue, testName, "actualValue.notNull");
        checkFalse(actualValue, testName, "actualValue");

        endTest(testType, testName);
    }

    public void isInitWithConfigurationFileTestImpl() throws Exception {
        final String testName = "isInitWithConfigurationFileTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/isInitWithConfigurationFile";
        } else {
            url = servletUrl + "?operation=isInitWithConfigurationFile";
        }

        final Value value = httpGetJson(url, Value.class);
        final Boolean actualValue = (Boolean) value.getValue();
        checkNotNull(actualValue, testName, "actualValue.notNull");
        checkFalse(actualValue, testName, "actualValue");

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

        final String status = mediatorService().getStatus().name();
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

        final String status = mediatorService().getStatus().name();
        final int statusCode = httpPutEmpty(url);
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

        final String status1 = mediatorService().getStatus().name();
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

        final String status = mediatorService().getStatus().name();
        final DefaultMediatorServiceConfiguration configuration = mediatorService().getConfiguration();
        final String json = serialize(DefaultMediatorServiceConfiguration.class, configuration);
        httpPostJson(url, json, null);

        final String status1 = mediatorService().getStatus().name();
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void shutdownTestImpl() throws Exception {
        final String testName = "shutdownTest";
        startTest(testType, testName);

        /*
        FIXME - DOES NOT WORK!
        Tries to stop DefaultDisruptorService and throws an exception that the reload failed.
        Looks like due to shared executor and shared executor services still having running tasks:
        Caused by: java.util.concurrent.RejectedExecutionException:
            at java.base/java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:2055)
            at java.base/java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:825)
            at java.base/java.util.concurrent.ScheduledThreadPoolExecutor.delayedExecute(ScheduledThreadPoolExecutor.java:340)
            at java.base/java.util.concurrent.ScheduledThreadPoolExecutor.scheduleWithFixedDelay(ScheduledThreadPoolExecutor.java:680)
            at java.base/java.util.concurrent.Executors$DelegatedScheduledExecutorService.scheduleWithFixedDelay(Executors.java:788)
            at com.viveworks.appbase.core.service.mediator.DefaultMediatorService.init(DefaultMediatorService.java:302)

        final MediatorService mediatorService = mediatorService();
        final DefaultMediatorServiceConfiguration configuration = mediatorService.getConfiguration();
        checkEquals(ServiceStatus.STARTED, mediatorService.getStatus(), testName, "serviceStatus");

        final String url;
        if (isRestApiTest()) {
            url = servletUrl + "?operation=shutdown";
        } else {
            url = resourceUrl + "/shutdown";
        }

        httpPostEmpty(url);
        checkEquals(ServiceStatus.SHUTDOWN, mediatorService.getStatus(), testName, "serviceStatus1");

        mediatorService.reload(configuration);
        checkEquals(ServiceStatus.STARTED, mediatorService.getStatus(), testName, "serviceStatus2");

        endTest(testType, testName);
        */
    }


    // ..............................
    // Helpers .....
    // ..............................

    private MediatorService mediatorService() {
        return appbaseServices.getCoreServices().appbaseCore().getMediatorService();
    }

    private boolean isRestApiTest() {
        return MediatorJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
