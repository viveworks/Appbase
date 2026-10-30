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
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorServiceMXBean;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.Value;
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
public class MediatorServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -1869337448942484004L;

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxMediatorServiceMXBean jmxMediatorServiceMXBean = jmxMediatorServiceMXBean(httpServletResponse);
        if (jmxMediatorServiceMXBean == null) {
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
                getStatus(jmxMediatorServiceMXBean, httpServletRequest, httpServletResponse);
            } else if("isInitWithConfigurationFile".equalsIgnoreCase(operation)) {
                isInitWithConfigurationFile(jmxMediatorServiceMXBean, httpServletRequest, httpServletResponse);
            } else if("isInitWithDefaultConfiguration".equalsIgnoreCase(operation)) {
                isInitWithDefaultConfiguration(jmxMediatorServiceMXBean, httpServletRequest, httpServletResponse);
            } else if("configurationFilePath".equalsIgnoreCase(operation)) {
                getConfigurationFilePath(jmxMediatorServiceMXBean, httpServletRequest, httpServletResponse);
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
        final JmxMediatorServiceMXBean jmxMediatorServiceMXBean = jmxMediatorServiceMXBean(httpServletResponse);
        if (jmxMediatorServiceMXBean == null) {
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
                reloadWithConfig(
                        jmxMediatorServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(
                        jmxMediatorServiceMXBean, httpServletRequest, httpServletResponse);
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
        final JmxMediatorServiceMXBean jmxMediatorServiceMXBean = jmxMediatorServiceMXBean(httpServletResponse);
        if (jmxMediatorServiceMXBean == null) {
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
                        jmxMediatorServiceMXBean, httpServletRequest, httpServletResponse);
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

    private void getConfigurationFilePath(
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String configurationFilePath = jmxMediatorServiceMXBean.getConfigurationFilePath();
        writeJson(new Value(configurationFilePath), Value.class, httpServletResponse);
    }

    private void isInitWithDefaultConfiguration(
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final boolean initWithDefaultConfiguration = jmxMediatorServiceMXBean.isInitWithDefaultConfiguration();
        writeJson(new Value(initWithDefaultConfiguration), Value.class, httpServletResponse);
    }

    private void isInitWithConfigurationFile(
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final boolean initWithConfigurationFile = jmxMediatorServiceMXBean.isInitWithConfigurationFile();
        writeJson(new Value(initWithConfigurationFile), Value.class, httpServletResponse);
    }

    private void getStatus(
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxMediatorServiceMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void shutdown(
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxMediatorServiceMXBean.shutdown();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithDefaultConfig(
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxMediatorServiceMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithConfig(
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxMediatorServiceMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
    }

    private JmxMediatorServiceMXBean jmxMediatorServiceMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxMediatorServiceMXBean jmxMediatorServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxMediatorService.class);

        if (jmxMediatorServiceMXBean == null) {
            sendInternalServerError(
                    "jmxMediatorServiceMXBean",
                    JmxMediatorService.class.getName() + " is not initialized",
                    httpServletResponse);
        }

        return jmxMediatorServiceMXBean;
    }
}
