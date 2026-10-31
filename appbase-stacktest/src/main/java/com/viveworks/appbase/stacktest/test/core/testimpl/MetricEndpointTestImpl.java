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

import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.metric.microprofile.micrometer.MicroMetricRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.dummytypes.DummyMeterRegistry;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.MetricJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.MetricServletTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.eclipse.microprofile.metrics.MetricRegistry;

import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MetricEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public MetricEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, MetricJaxrsResourceTest.class, MetricServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_METRIC_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_METRIC_SERVLET_PATH;
    }

    public void serializeMetricRegistryAsJsonTestImpl() throws Exception {
        final String testName = "serializeMetricRegistryAsJsonTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/serializeMetricRegistryAsJson";
        } else {
            url = servletUrl + "?operation=serializeMetricRegistryAsJson";
        }

        final String actualJson = httpGetJson(url);
        checkNotNull(actualJson, testName, "actualJson");

        endTest(testType, testName);
    }

    public void clearMicrometerRegistriesTestImpl() throws Exception {
        final String testName = "clearMicrometerRegistriesTest";
        startTest(testType, testName);

        final MetricService metricService = metricService();

        final Set<MeterRegistry> micrometerRegistries = metricService.getMicrometerRegistries();
        checkNotNull(micrometerRegistries, testName, "micrometerRegistries");

        final DummyMeterRegistry dummyMeterRegistry = new DummyMeterRegistry();
        metricService.registerMicrometerRegistry(dummyMeterRegistry);
        final Set<MeterRegistry> micrometerRegistries1 = metricService.getMicrometerRegistries();
        checkTrue(
                containsMeterRegistry(micrometerRegistries1, DummyMeterRegistry.class),
                testName,
                "containsMeterRegistry1");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/clearMicrometerRegistries";
        } else {
            url = servletUrl + "?operation=clearMicrometerRegistries";
        }

        try {
            httpDelete(url);

            final Set<MeterRegistry> micrometerRegistries2 = metricService.getMicrometerRegistries();
            checkFalse(
                    containsMeterRegistry(micrometerRegistries2, DummyMeterRegistry.class),
                    testName,
                    "containsMeterRegistry2");
        } finally {
            metricService.unregisterMicrometerRegistry(dummyMeterRegistry);
        }

        resetMeterRegistries(micrometerRegistries1);

        endTest(testType, testName);
    }

    public void unregisterMicrometerRegistryTestImpl() throws Exception {
        final String testName = "unregisterMicrometerRegistryTest";
        startTest(testType, testName);

        final Set<MeterRegistry> micrometerRegistries = metricService().getMicrometerRegistries();
        checkNotNull(micrometerRegistries, testName, "micrometerRegistries");

        final DummyMeterRegistry dummyMeterRegistry = new DummyMeterRegistry();
        metricService().registerMicrometerRegistry(dummyMeterRegistry);
        final Set<MeterRegistry> micrometerRegistries1 = metricService().getMicrometerRegistries();
        checkTrue(
                containsMeterRegistry(micrometerRegistries1, DummyMeterRegistry.class),
                testName,
                "containsMeterRegistry1");

        final String meterRegistryTypeName = DummyMeterRegistry.class.getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/unregisterMicrometerRegistry/"+ meterRegistryTypeName;
        } else {
            url = servletUrl + "?operation=unregisterMicrometerRegistry" +
                    "&meterRegistryTypeName=" + meterRegistryTypeName;
        }

        try {
            httpDelete(url);

            final Set<MeterRegistry> micrometerRegistries2 = metricService().getMicrometerRegistries();
            checkFalse(
                    containsMeterRegistry(micrometerRegistries2, DummyMeterRegistry.class),
                    testName,
                    "containsMeterRegistry2");
        } finally {
            metricService().unregisterMicrometerRegistry(dummyMeterRegistry);
        }

        endTest(testType, testName);
    }

    public void micrometerRegistryNamesTestImpl() throws Exception {
        final String testName = "micrometerRegistryNamesTest";
        startTest(testType, testName);

        final DummyMeterRegistry dummyMeterRegistry = new DummyMeterRegistry();
        metricService().registerMicrometerRegistry(dummyMeterRegistry);
        final Set<MeterRegistry> micrometerRegistries1 = metricService().getMicrometerRegistries();
        checkTrue(
                containsMeterRegistry(micrometerRegistries1, DummyMeterRegistry.class),
                testName,
                "containsMeterRegistry1");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/micrometerRegistryNames";
        } else {
            url = servletUrl + "?operation=micrometerRegistryNames";
        }

        try {
            final SetOfStringsValue result = httpGetJson(url, SetOfStringsValue.class);
            final Set<String> resultSet = result.getValue();
            checkNotNull(resultSet, testName, "resultSet");
            checkTrue(containsMeterRegistryName(resultSet, dummyMeterRegistry.getClass()),
                    testName,
                    "containsMeterRegistryName1");
        } finally {
            metricService().unregisterMicrometerRegistry(dummyMeterRegistry);
        }

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

        final String status = metricService().getStatus().name();
        final String status1 = httpGetText(url, String.class);
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void reloadTestImpl() throws Exception {
        final String testName = "reloadTest";
        startTest(testType, testName);

        /*
        FIXME - DOES NOT WORK!
        If this test runs, then MetricFilterTestImpl fails

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final MetricService metricService = metricService();
        final Set<MeterRegistry> micrometerRegistries = metricService.getMicrometerRegistries();
        final String status = metricService.getStatus().name();
        final int statusCode = httpPutEmpty(url);
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

        final String status1 = metricService.getStatus().name();
        checkEquals(status1, status, testName, "status1");

        resetMeterRegistries(micrometerRegistries);
        */

        endTest(testType, testName);
    }

    public void reloadWithConfigTestImpl() throws Exception {
        final String testName = "reloadWithConfigTest";
        startTest(testType, testName);

        /*
        FIXME - DOES NOT WORK!
        If this test runs, then MetricFilterTestImpl fails

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final MetricService metricService = metricService();
        final Set<MeterRegistry> micrometerRegistries = metricService.getMicrometerRegistries();

        final String status = metricService.getStatus().name();
        final DefaultMetricServiceConfiguration configuration = metricService.getConfiguration();
        final String json = serialize(DefaultMetricServiceConfiguration.class, configuration);
        httpPostJson(url, json, null);

        final String status1 = metricService.getStatus().name();
        checkEquals(status1, status, testName, "status1");

        resetMeterRegistries(micrometerRegistries);
        */

        endTest(testType, testName);
    }

    public void shutdownTestImpl() throws Exception {
        final String testName = "shutdownTest";
        startTest(testType, testName);

        /*
        FIXME - DOES NOT WORK!
        If this test runs, then MetricFilterTestImpl fails

        final MetricService metricService = metricService();
        final Set<MeterRegistry> micrometerRegistries = metricService.getMicrometerRegistries();

        final DefaultMetricServiceConfiguration configuration = metricService().getConfiguration();
        checkEquals(ServiceStatus.STARTED, metricService.getStatus(), testName, "serviceStatus");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/shutdown";
        } else {
            url = servletUrl + "?operation=shutdown";
        }

        httpPostEmpty(url);
        checkEquals(ServiceStatus.SHUTDOWN, metricService.getStatus(), testName, "serviceStatus1");

        metricService.reload(configuration);
        checkEquals(ServiceStatus.STARTED, metricService.getStatus(), testName, "serviceStatus2");

        resetMeterRegistries(micrometerRegistries);
        */

        endTest(testType, testName);
    }

    private void resetMeterRegistries(final Set<MeterRegistry> micrometerRegistries) {
        final MetricService metricService = metricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        if (MicroMetricRegistry.class.isAssignableFrom(metricRegistry.getClass())) {
            final MicroMetricRegistry microMetricRegistry = (MicroMetricRegistry) metricRegistry;
            final CompositeMeterRegistry compositeMeterRegistry = microMetricRegistry.getCompositeMeterRegistry();
            micrometerRegistries.forEach(meterRegistry -> {
                if (!meterRegistry.getClass().isAssignableFrom(DummyMeterRegistry.class)) {
                    compositeMeterRegistry.add(meterRegistry);
                }
            });
        }
    }


    // ..............................
    // Helpers .....
    // ..............................

    private boolean containsMeterRegistryName(
            final Set<String> micrometerRegistryNames,
            final Class<? extends MeterRegistry> meterRegistryType)
    {
        for (final String micrometerRegistryName : micrometerRegistryNames) {
            if (meterRegistryType.getName().equals(micrometerRegistryName)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsMeterRegistry(
            final Set<MeterRegistry> micrometerRegistries,
            final Class<? extends MeterRegistry> meterRegistryType)
    {
        for (final MeterRegistry meterRegistry : micrometerRegistries) {
            if (meterRegistryType.isAssignableFrom(meterRegistry.getClass())) {
                return true;
            }
        }

        return false;
    }

    private MetricService metricService() {
        return appbaseServices.getCoreServices().appbaseCore().getMetricService();
    }

    private boolean isRestApiTest() {
        return MetricJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
