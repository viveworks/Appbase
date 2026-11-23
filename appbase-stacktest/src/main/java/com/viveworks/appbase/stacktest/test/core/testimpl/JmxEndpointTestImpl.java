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

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MxBeanEntryList;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer;
import com.viveworks.appbase.stacktest.dummytypes.DummyMXBean;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.core.jaxrs.JmxJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.JmxServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.Collection;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public JmxEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, JmxJaxrsResourceTest.class, JmxServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_JMX_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_JMX_SERVLET_PATH;
    }

    public void mxBeanEntriesTestImpl() throws Exception {
        final String testName = "mxBeanEntriesTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/mxBeanEntries";
        } else {
            url = servletUrl + "?operation=mxBeanEntries";
        }

        final MxBeanEntryList mxBeanEntryList = httpGetJson(url, MxBeanEntryList.class);
        checkNotNull(mxBeanEntryList, testName, "mxBeanEntryList");

        final List<MXBeanEntry> mxBeanEntries = mxBeanEntryList.getMxBeanEntries();
        checkNotNull(mxBeanEntries, testName, "mxBeanEntries");
        checkFalse(mxBeanEntries.isEmpty(), testName, "mxBeanEntries.empty");

        endTest(testType, testName);
    }

    public void mxBeanEntryTestImpl() throws Exception {
        final String testName = "mxBeanEntryTest";
        startTest(testType, testName);

        final JmxService jmxService = jmxService();
        checkFalse(jmxService.isMXBeanRegistered(DummyMXBean.class), testName, "jmxService.mxBeanRegistered");

        final DummyMXBean dummyMXBean = new DummyMXBean();
        jmxService.registerMXBean(dummyMXBean);

        final String mxBeanTypeName = DummyMXBean.class.getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/mxBeanEntries/" + mxBeanTypeName;
        } else {
            url = servletUrl + "?operation=mxBeanEntry&mxBeanTypeName=" + mxBeanTypeName;
        }

        try {
            final MXBeanEntry mxBeanEntry = httpGetJson(url, MXBeanEntry.class);
            checkNotNull(mxBeanEntry, testName, "mxBeanEntry");
            checkTrue(jmxService.isMXBeanRegistered(DummyMXBean.class), testName, "jmxService.mxBeanRegistered");
        } finally {
            if (jmxService.isMXBeanRegistered(DummyMXBean.class)) {
                jmxService.unregisterMXBean(DummyMXBean.class);
            }
        }

        endTest(testType, testName);
    }

    public void isMXBeanRegisteredTestImpl() throws Exception {
        final String testName = "isMXBeanRegisteredTest";
        startTest(testType, testName);

        final JmxService jmxService = jmxService();
        final String mxBeanTypeName = DummyMXBean.class.getName();
        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/mxBeans/" + mxBeanTypeName;
        } else {
            url = servletUrl + "?operation=isMXBeanRegistered&mxBeanTypeName=" + mxBeanTypeName;
        }

        try {
            checkFalse(jmxService.isMXBeanRegistered(DummyMXBean.class), testName, "jmxService.mxBeanRegistered");
            final int code1 = httpHead(url);
            checkEquals(HttpServletResponse.SC_NOT_FOUND, code1, testName, "code1");

            final DummyMXBean dummyMXBean = new DummyMXBean();
            jmxService.registerMXBean(dummyMXBean);

            checkTrue(jmxService.isMXBeanRegistered(DummyMXBean.class), testName, "jmxService.mxBeanRegistered");
            final int code2 = httpHead(url);
            checkEquals(HttpServletResponse.SC_NO_CONTENT, code2, testName, "code2");
        } finally {
            if (jmxService.isMXBeanRegistered(DummyMXBean.class)) {
                jmxService.unregisterMXBean(DummyMXBean.class);
            }
        }

        endTest(testType, testName);
    }

    public void unregisterMXBeanTestImpl() throws Exception {
        final String testName = "unregisterMXBeanTest";
        startTest(testType, testName);

        final JmxService jmxService = jmxService();
        final String mxBeanTypeName = DummyMXBean.class.getName();

        try {
            final DummyMXBean dummyMXBean = new DummyMXBean();
            jmxService.registerMXBean(dummyMXBean);
            checkTrue(jmxService.isMXBeanRegistered(DummyMXBean.class), testName, "jmxService.mxBeanRegistered");

            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/mxBeans/" + mxBeanTypeName;
            } else {
                url = servletUrl + "?operation=unregisterMXBean&mxBeanTypeName=" + mxBeanTypeName;
            }
            httpDelete(url);

            checkFalse(jmxService.isMXBeanRegistered(DummyMXBean.class), testName, "jmxService.mxBeanRegistered");
        } finally {
            if (jmxService.isMXBeanRegistered(DummyMXBean.class)) {
                jmxService.unregisterMXBean(DummyMXBean.class);
            }
        }

        endTest(testType, testName);
    }

    public void clearMXBeansTestImpl() throws Exception {
        final String testName = "clearMXBeansTest";
        startTest(testType, testName);

        final Collection<MXBeanEntry> mxBeanEntries1 = jmxService().getMXBeanEntries();
        checkNotNull(mxBeanEntries1, testName, "mxBeanEntries1");
        checkFalse(mxBeanEntries1.isEmpty(), testName, "mxBeanEntries1.empty");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/mxBeans";
        } else {
            url = servletUrl + "?operation=clearMXBeans";
        }

        try {
            httpDelete(url);

            final Collection<MXBeanEntry> mxBeanEntries2 = jmxService().getMXBeanEntries();
            checkNotNull(mxBeanEntries2, testName, "mxBeanEntries1");
            checkTrue(mxBeanEntries2.isEmpty(), testName, "mxBeanEntries2.empty");
        } finally {
            JettyServletContainer.registerMXBeans(appbaseServices.getCoreServices().appbaseCore());
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

        final String status = jmxService().getStatus().name();
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

        try {
            final String status = jmxService().getStatus().name();
            final int statusCode = httpPutEmpty(url);
            checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

            final String status1 = jmxService().getStatus().name();
            checkEquals(status1, status, testName, "status1");
        } finally {
            JettyServletContainer.registerMXBeans(appbaseServices.getCoreServices().appbaseCore());
        }

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

        try {
            final String status = jmxService().getStatus().name();
            final DefaultJmxServiceConfiguration configuration = jmxService().getConfiguration();
            final String json = serialize(DefaultJmxServiceConfiguration.class, configuration);
            httpPostJson(url, json, null);

            final String status1 = jmxService().getStatus().name();
            checkEquals(status1, status, testName, "status1");
        } finally {
            JettyServletContainer.registerMXBeans(appbaseServices.getCoreServices().appbaseCore());
        }

        endTest(testType, testName);
    }

    public void shutdownTestImpl() throws Exception {
        final String testName = "shutdownTest";
        startTest(testType, testName);

        final JmxService jmxService = jmxService();
        final DefaultJmxServiceConfiguration configuration = jmxService().getConfiguration();
        checkEquals(ServiceStatus.STARTED, jmxService.getStatus(), testName, "serviceStatus");

        try {
            final String url;
            if (isRestApiTest()) {
                url = resourceUrl + "/shutdown";
            } else {
                url = servletUrl + "?operation=shutdown";
            }

            httpPostEmpty(url);
            checkEquals(ServiceStatus.SHUTDOWN, jmxService.getStatus(), testName, "serviceStatus1");

            jmxService.reload(configuration);
            checkEquals(ServiceStatus.STARTED, jmxService.getStatus(), testName, "serviceStatus2");
        } finally {
            JettyServletContainer.registerMXBeans(appbaseServices.getCoreServices().appbaseCore());
        }

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private JmxService jmxService() {
        return appbaseServices.getCoreServices().appbaseCore().getJmxService();
    }

    private boolean isRestApiTest() {
        return JmxJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
