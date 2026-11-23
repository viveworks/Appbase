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
import com.viveworks.appbase.core.jmx.JmxAppbaseCore;
import com.viveworks.appbase.core.jmx.JmxAppbaseCoreMXBean;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.service.AppbaseServiceConstants;
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
public class CoreServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -1037084994776677852L;

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean = jmxAppbaseCoreMXBean(httpServletResponse);
        if (jmxAppbaseCoreMXBean == null) {
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
                getStatus(jmxAppbaseCoreMXBean, httpServletRequest, httpServletResponse);
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
            final HttpServletResponse httpServletResponse) throws IOException
    {
        final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean = jmxAppbaseCoreMXBean(httpServletResponse);
        if (jmxAppbaseCoreMXBean == null) {
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
                reloadWithDefaultConfig(
                        jmxAppbaseCoreMXBean, httpServletRequest, httpServletResponse);
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
        final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean = jmxAppbaseCoreMXBean(httpServletResponse);
        if (jmxAppbaseCoreMXBean == null) {
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
            if ("reloadService".equalsIgnoreCase(operation)) {
                reloadService(jmxAppbaseCoreMXBean, httpServletRequest, httpServletResponse);
            } else if ("reload".equalsIgnoreCase(operation)) {
                reloadWithConfig(jmxAppbaseCoreMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(jmxAppbaseCoreMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdownService".equalsIgnoreCase(operation)) {
                shutdownService(jmxAppbaseCoreMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdownAllServices".equalsIgnoreCase(operation)) {
                shutdownAllServices(jmxAppbaseCoreMXBean, httpServletRequest, httpServletResponse);
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

    private void shutdown(
            final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxAppbaseCoreMXBean.shutdown();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithConfig(
            final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxAppbaseCoreMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
    }

    private void reloadWithDefaultConfig(
            final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxAppbaseCoreMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    private void getStatus(
            final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxAppbaseCoreMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void reloadService(
            final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String appbaseServiceTypeName = nonBlankParameter(
                "appbaseServiceTypeName", httpServletRequest, httpServletResponse);
        if (appbaseServiceTypeName == null) {
            return;
        }

        final String appbaseServiceConfigurationTypeName = nonBlankParameter(
                "appbaseServiceConfigurationTypeName", httpServletRequest, httpServletResponse);
        if (appbaseServiceConfigurationTypeName == null) {
            return;
        }

        final String appbaseServiceConfigurationYaml = readString(httpServletRequest);

        jmxAppbaseCoreMXBean.reloadService(
                appbaseServiceTypeName,
                appbaseServiceConfigurationYaml,
                appbaseServiceConfigurationTypeName);
        sendNoContent(httpServletResponse);
    }

    private void shutdownService(
            final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String appbaseServiceTypeName = nonBlankParameter(
                "appbaseServiceTypeName", httpServletRequest, httpServletResponse);
        if (appbaseServiceTypeName == null) {
            return;
        }

        jmxAppbaseCoreMXBean.shutdownService(appbaseServiceTypeName);
        sendNoContent(httpServletResponse);
    }

    private void shutdownAllServices(
            final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxAppbaseCoreMXBean.shutdownAllServices();
        sendNoContent(httpServletResponse);
    }

    private JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean =
                appbaseCore.getJmxService().getMXBean(JmxAppbaseCore.class);
        if (jmxAppbaseCoreMXBean == null) {
            sendInternalServerError(
                    "jmxAppbaseCoreMXBean",
                    JmxAppbaseCore.class.getName() + " is not initialized",
                    httpServletResponse);
            return null;
        }

        return jmxAppbaseCoreMXBean;
    }
}
