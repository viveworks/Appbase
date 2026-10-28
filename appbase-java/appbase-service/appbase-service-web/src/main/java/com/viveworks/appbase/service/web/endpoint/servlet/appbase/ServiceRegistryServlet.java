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
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringServiceStatusValue;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Timer;

import javax.management.MBeanException;
import java.io.IOException;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ServiceRegistryServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -6930397138125243453L;

    @Override
    protected void doHead(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = jmxServiceRegistryMXBean(httpServletResponse);
        if (jmxServiceRegistryMXBean == null) {
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
            if ("appbaseServiceStatus".equalsIgnoreCase(operation)) {
                getAppbaseServiceStatus(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
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
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = jmxServiceRegistryMXBean(httpServletResponse);
        if (jmxServiceRegistryMXBean == null) {
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
                getStatus(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
            } else if ("overallStatus".equalsIgnoreCase(operation)) {
                overallStatusJson(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
            } else if ("appbaseServiceStatuses".equalsIgnoreCase(operation)) {
                getAllServiceStatusByServiceTypeName(
                        jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
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
    protected void doPost(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = jmxServiceRegistryMXBean(httpServletResponse);
        if (jmxServiceRegistryMXBean == null) {
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
                reloadWithConfig(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
            } else if ("pollAndUpdateStatuses".equalsIgnoreCase(operation)) {
                pollAndUpdateStatuses(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
            } else if ("pollOverallStatus".equalsIgnoreCase(operation)) {
                pollOverallStatusJson(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
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
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = jmxServiceRegistryMXBean(httpServletResponse);
        if (jmxServiceRegistryMXBean == null) {
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
                reloadWithDefaultConfig(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
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
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = jmxServiceRegistryMXBean(httpServletResponse);
        if (jmxServiceRegistryMXBean == null) {
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
            if ("unregisterAppbaseService".equalsIgnoreCase(operation)) {
                unregisterAppbaseService(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearAppbaseServices".equalsIgnoreCase(operation)) {
                clearAppbaseServices(jmxServiceRegistryMXBean, httpServletRequest, httpServletResponse);
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

    private void getAppbaseServiceStatus(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String appbaseServiceTypeName =
                nonBlankParameter("appbaseServiceTypeName", httpServletRequest, httpServletResponse);
        if (appbaseServiceTypeName == null) {
            return;
        }

        final ServiceStatus appbaseServiceStatus = jmxServiceRegistryMXBean
                .getAppbaseServiceStatus(appbaseServiceTypeName);
        if (appbaseServiceStatus == ServiceStatus.STARTED) {
            sendOK(httpServletResponse);
        } else {
            sendServiceUnavailable(httpServletResponse, "appbaseServiceStatus");
        }
    }

    private void getAllServiceStatusByServiceTypeName(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final Map<String, ServiceStatus> result =
                jmxServiceRegistryMXBean.getAllServiceStatusByServiceTypeName();
        final MapOfStringServiceStatusValue value = new MapOfStringServiceStatusValue(result);
        sendOK(value, MapOfStringServiceStatusValue.class, httpServletResponse);
    }

    private void pollAndUpdateStatuses(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxServiceRegistryMXBean.pollAndUpdateStatuses();
        sendNoContent(httpServletResponse);
    }

    private void pollOverallStatusJson(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws MBeanException, IOException
    {
        final String json = jmxServiceRegistryMXBean.pollOverallStatusJson();
        overallStatus(json, httpServletResponse);
    }

    private void unregisterAppbaseService(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws MBeanException, IOException
    {
        final String appbaseServiceTypeName =
                nonBlankParameter("appbaseServiceTypeName", httpServletRequest, httpServletResponse);
        if (appbaseServiceTypeName == null) {
            return;
        }

        jmxServiceRegistryMXBean.unregisterAppbaseService(appbaseServiceTypeName);
        sendNoContent(httpServletResponse);
    }

    private void clearAppbaseServices(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxServiceRegistryMXBean.clearAppbaseServices();
        sendNoContent(httpServletResponse);
    }

    private void overallStatusJson(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws MBeanException, IOException
    {
        final String json = jmxServiceRegistryMXBean.overallStatusJson();
        overallStatus(json, httpServletResponse);
    }

    private void getStatus(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxServiceRegistryMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void shutdown(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxServiceRegistryMXBean.shutdown();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithDefaultConfig(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxServiceRegistryMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithConfig(
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxServiceRegistryMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
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
