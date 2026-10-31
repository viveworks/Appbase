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

package com.viveworks.appbase.service.web.endpoint.servlet.appbase;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Timer;

import javax.management.MBeanException;
import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class StatusServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = 5124273857701238084L;

    /**
     * Reads the overall status on GET rather than HEAD -- HEAD is an unusual verb choice for a
     * body-bearing read and left this endpoint inconsistent with the reactive {@code /status} face
     * (Task 5's {@code NativeEndpoints}, which is GET-only by construction). Renamed from
     * {@code doHead} to {@code doGet} with no other logic change: same body, same 200/503 status
     * semantics via {@link ServiceRegistry.OverallStatus#isStarted()}. HEAD is explicitly NOT
     * supported here ({@link #isHeadSupported()} returns {@code false}, so the base {@code doHead}
     * template sends 405 Method Not Allowed) -- consistent with the reactive {@code /status} face,
     * where HEAD is not a registered route. No test or IT asserted the HEAD verb on this path.
     */
    @Override
    protected boolean isHeadSupported() {
        // /status is read via GET (see doGet); HEAD is not supported -> base doHead sends 405,
        // matching the reactive face where HEAD /status is not a registered route.
        return false;
    }

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = jmxServiceRegistryMXBean(httpServletResponse);
        if (jmxServiceRegistryMXBean == null) {
            return;
        }

        final Timer.Context timer = timer("overallStatus").time();
        try {
            final String json = jmxServiceRegistryMXBean.overallStatusJson();
            overallStatus(json, httpServletResponse);
        } catch (MBeanException e) {
            logger.error("Error reading overall status", e);
            sendInternalServerError("overallStatus", "Error reading overall status", httpServletResponse);
        } finally {
            timer.stop();
        }
    }

    @Override
    protected void doPost(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = jmxServiceRegistryMXBean(httpServletResponse);
        if (jmxServiceRegistryMXBean == null) {
            return;
        }

        final Timer.Context timer = timer("pollOverallStatus").time();
        try {
            final String json = jmxServiceRegistryMXBean.pollOverallStatusJson();
            overallStatus(json, httpServletResponse);
        } catch (MBeanException e) {
            logger.error("Error reading overall status", e);
            sendInternalServerError("pollOverallStatus", "Error reading overall status", httpServletResponse);
        } finally {
            timer.stop();
        }
    }

    private void overallStatus(
            final String json,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final ServiceRegistry.OverallStatus overallStatus = JsonUtil
                .getJsonMapper()
                .readerFor(ServiceRegistry.OverallStatus.class)
                .readValue(json);
        if (overallStatus.isStarted()) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        } else {
            httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        writeJson(json, httpServletResponse);
    }

    private JmxServiceRegistryMXBean jmxServiceRegistryMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxServiceRegistry.class);
        if (jmxServiceRegistryMXBean == null) {
            sendInternalServerError(
                    "jmxServiceRegistryMXBean",
                    JmxServiceRegistry.class.getName() + " is not initialized",
                    httpServletResponse);
            return null;
        }

        return jmxServiceRegistryMXBean;
    }
}
