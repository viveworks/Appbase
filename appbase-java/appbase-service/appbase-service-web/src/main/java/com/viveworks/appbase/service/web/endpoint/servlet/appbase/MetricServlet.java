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

package com.viveworks.appbase.service.web.endpoint.servlet.appbase;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricService;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricServiceMXBean;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Timer;

import javax.management.MBeanException;
import java.io.IOException;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MetricServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -3203545416571904799L;

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxMetricServiceMXBean jmxMetricServiceMXBean = jmxMetricServiceMXBean(httpServletResponse);
        if (jmxMetricServiceMXBean == null) {
            return;
        }

        String operation = nonBlankParameter(
                AppbaseServiceConstants.OPERATION_PARAMETER, httpServletRequest, httpServletResponse);
        if (operation == null) {
            return;
        }
        operation = operation.trim();

        final Timer.Context timer = timer(operation).time();
        try {
            if ("status".equalsIgnoreCase(operation)) {
                getStatus(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("micrometerRegistryNames".equalsIgnoreCase(operation)) {
                getMicrometerRegistryNames(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("serializeMetricRegistryAsJson".equalsIgnoreCase(operation)) {
                serializeMetricRegistryAsJson(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
        // Defensive, intentional: GET metric reads are NOT JMX-permission-gated today (reads are open; HTTP-layer authorization is the real gate, applied on every face) -- this catch stays so a future gate maps cleanly to 403.
        } catch (PermissionDeniedException pde) {
            sendForbidden(operation, httpServletResponse);
        } catch (Exception ex) {
            sendInternalServerError(operation, ex, httpServletResponse);
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
        final JmxMetricServiceMXBean jmxMetricServiceMXBean = jmxMetricServiceMXBean(httpServletResponse);
        if (jmxMetricServiceMXBean == null) {
            return;
        }

        String operation = nonBlankParameter(
                AppbaseServiceConstants.OPERATION_PARAMETER, httpServletRequest, httpServletResponse);
        if (operation == null) {
            return;
        }
        operation = operation.trim();

        final Timer.Context timer = timer(operation).time();
        try {
            if ("reload".equalsIgnoreCase(operation)) {
                reloadWithConfig(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
        } catch (PermissionDeniedException pde) {
            sendForbidden(operation, httpServletResponse);
        } catch (Exception ex) {
            sendInternalServerError(operation, ex, httpServletResponse);
        } finally {
            timer.stop();
        }
    }

    @Override
    protected void doPut(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxMetricServiceMXBean jmxMetricServiceMXBean = jmxMetricServiceMXBean(httpServletResponse);
        if (jmxMetricServiceMXBean == null) {
            return;
        }

        String operation = nonBlankParameter(
                AppbaseServiceConstants.OPERATION_PARAMETER, httpServletRequest, httpServletResponse);
        if (operation == null) {
            return;
        }
        operation = operation.trim();

        final Timer.Context timer = timer(operation).time();
        try {
            if ("reload".equalsIgnoreCase(operation)) {
                reloadWithDefaultConfig(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
        } catch (PermissionDeniedException pde) {
            sendForbidden(operation, httpServletResponse);
        } catch (Exception ex) {
            sendInternalServerError(operation, ex, httpServletResponse);
        } finally {
            timer.stop();
        }
    }

    @Override
    protected void doDelete(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxMetricServiceMXBean jmxMetricServiceMXBean = jmxMetricServiceMXBean(httpServletResponse);
        if (jmxMetricServiceMXBean == null) {
            return;
        }

        String operation = nonBlankParameter(
                AppbaseServiceConstants.OPERATION_PARAMETER, httpServletRequest, httpServletResponse);
        if (operation == null) {
            return;
        }
        operation = operation.trim();

        final Timer.Context timer = timer(operation).time();
        try {
            if ("unregisterMicrometerRegistry".equalsIgnoreCase(operation)) {
                unregisterMicrometerRegistry(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearMicrometerRegistries".equalsIgnoreCase(operation)) {
                clearMicrometerRegistries(jmxMetricServiceMXBean, httpServletRequest, httpServletResponse);
            } else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
        } catch (PermissionDeniedException pde) {
            sendForbidden(operation, httpServletResponse);
        } catch (Exception ex) {
            sendInternalServerError(operation, ex, httpServletResponse);
        } finally {
            timer.stop();
        }
    }

    private void clearMicrometerRegistries(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxMetricServiceMXBean.clearMicrometerRegistries();
        sendNoContent(httpServletResponse);
    }

    private void unregisterMicrometerRegistry(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws MBeanException, IOException
    {
        final String meterRegistryTypeName =
                nonBlankParameter("meterRegistryTypeName", httpServletRequest, httpServletResponse);
        if (meterRegistryTypeName == null) {
            return;
        }

        jmxMetricServiceMXBean.unregisterMicrometerRegistry(meterRegistryTypeName);
        sendNoContent(httpServletResponse);
    }

    private void getMicrometerRegistryNames(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final Set<String> result = jmxMetricServiceMXBean.getMicrometerRegistryNames();
        writeJson(new SetOfStringsValue(result), SetOfStringsValue.class, httpServletResponse);
    }

    private void serializeMetricRegistryAsJson(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws MBeanException, IOException
    {
        final String json = jmxMetricServiceMXBean.serializeMetricRegistryAsJson();
        sendOKJson(json, httpServletResponse);
    }

    private void getStatus(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxMetricServiceMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void shutdown(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxMetricServiceMXBean.shutdown();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithDefaultConfig(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxMetricServiceMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithConfig(
            final JmxMetricServiceMXBean jmxMetricServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxMetricServiceMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
    }

    private JmxMetricServiceMXBean jmxMetricServiceMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxMetricServiceMXBean jmxMetricServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxMetricService.class);

        if (jmxMetricServiceMXBean == null) {
            sendInternalServerError(
                    "jmxMetricServiceMXBean",
                    JmxMetricService.class.getName() + " is not initialized",
                    httpServletResponse);
        }

        return jmxMetricServiceMXBean;
    }
}
