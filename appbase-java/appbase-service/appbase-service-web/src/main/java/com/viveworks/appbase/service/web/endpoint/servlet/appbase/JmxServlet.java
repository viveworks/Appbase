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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MxBeanEntryList;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Timer;

import javax.management.MBeanException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -6277662161626882465L;

    @Override
    protected void doHead(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws ServletException, IOException
    {
        final JmxJmxServiceMXBean jmxJmxServiceMXBean = jmxJmxServiceMXBean(httpServletResponse);
        if (jmxJmxServiceMXBean == null) {
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
            if ("isMXBeanRegistered".equalsIgnoreCase(operation)) {
                isMXBeanRegistered(jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
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
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxJmxServiceMXBean jmxJmxServiceMXBean = jmxJmxServiceMXBean(httpServletResponse);
        if (jmxJmxServiceMXBean == null) {
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
                getStatus(jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("mxBeanEntries".equalsIgnoreCase(operation)) {
                mxBeanEntries(jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("mxBeanEntry".equalsIgnoreCase(operation)) {
                mxBeanEntry(jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
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
        final JmxJmxServiceMXBean jmxJmxServiceMXBean = jmxJmxServiceMXBean(httpServletResponse);
        if (jmxJmxServiceMXBean == null) {
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
                        jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(
                        jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
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
        final JmxJmxServiceMXBean jmxJmxServiceMXBean = jmxJmxServiceMXBean(httpServletResponse);
        if (jmxJmxServiceMXBean == null) {
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
                        jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
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
        final JmxJmxServiceMXBean jmxJmxServiceMXBean = jmxJmxServiceMXBean(httpServletResponse);
        if (jmxJmxServiceMXBean == null) {
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
            if ("unregisterMXBean".equalsIgnoreCase(operation)) {
                unregisterMXBean(jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearMXBeans".equalsIgnoreCase(operation)) {
                clearMXBeans(jmxJmxServiceMXBean, httpServletRequest, httpServletResponse);
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

    private void isMXBeanRegistered(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String mxBeanTypeName = nonBlankParameter("mxBeanTypeName", httpServletRequest, httpServletResponse);
        if (mxBeanTypeName == null) {
            return;
        }

        try {
            final boolean registered = jmxJmxServiceMXBean.isMXBeanRegistered(mxBeanTypeName);
            if (registered) {
                sendNoContent(httpServletResponse);
            } else {
                sendNotFound("isMXBeanRegistered", httpServletResponse);
            }
        } catch (MBeanException me) {
            if (me.getTargetException() != null &&
                    me.getTargetException().getClass().isAssignableFrom(ClassNotFoundException.class))
            {
                sendNotFound("mxBeanEntry", httpServletResponse);
                return;
            }

            sendInternalServerError("mxBeanEntry", me, httpServletResponse);
        }
    }

    private void mxBeanEntries(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final List<String> mxBeanEntriesJson = jmxJmxServiceMXBean.getMXBeanEntriesJson();
        if (mxBeanEntriesJson != null) {
            final ObjectMapper jsonMapper = JsonUtil.getJsonMapper();
            final List<MXBeanEntry> mxBeanEntries = new ArrayList<>();
            for (final String json : mxBeanEntriesJson) {
                final MXBeanEntry mxBeanEntry = jsonMapper
                        .readerFor(MXBeanEntry.class)
                        .readValue(json);
                mxBeanEntries.add(mxBeanEntry);
            }

            final MxBeanEntryList mxBeanEntryList = new MxBeanEntryList(mxBeanEntries);
            final String json = jsonMapper.writerFor(MxBeanEntryList.class).writeValueAsString(mxBeanEntryList);
            writeJson(json, httpServletResponse);
        }
    }

    private void mxBeanEntry(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String mxBeanTypeName = nonBlankParameter("mxBeanTypeName", httpServletRequest, httpServletResponse);
        if (mxBeanTypeName == null) {
            return;
        }

        try {
            final String mxBeanEntryJson = jmxJmxServiceMXBean.getMXBeanEntryJson(mxBeanTypeName);
            writeJson(mxBeanEntryJson, httpServletResponse);
        }  catch (MBeanException me) {
            if (me.getTargetException() != null &&
                    me.getTargetException().getClass().isAssignableFrom(ClassNotFoundException.class))
            {
                sendNotFound("mxBeanEntry", httpServletResponse);
                return;
            }

            sendInternalServerError("mxBeanEntry", me, httpServletResponse);
        }
    }

    private void unregisterMXBean(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String mxBeanTypeName = nonBlankParameter("mxBeanTypeName", httpServletRequest, httpServletResponse);
        if (mxBeanTypeName == null) {
            return;
        }

        try {
            jmxJmxServiceMXBean.unregisterMXBean(mxBeanTypeName);
            sendNoContent(httpServletResponse);
        } catch (MBeanException me) {
            if (me.getTargetException() != null &&
                    me.getTargetException().getClass().isAssignableFrom(ClassNotFoundException.class))
            {
                sendNotFound("mxBeanEntry", httpServletResponse);
                return;
            }

            sendInternalServerError("mxBeanEntry", me, httpServletResponse);
        }
    }

    private void clearMXBeans(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxJmxServiceMXBean.clearMXBeans();
        sendNoContent(httpServletResponse);
    }

    private void getStatus(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxJmxServiceMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void shutdown(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxJmxServiceMXBean.shutdown();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithDefaultConfig(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxJmxServiceMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithConfig(
            final JmxJmxServiceMXBean jmxJmxServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxJmxServiceMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
    }

    private JmxJmxServiceMXBean jmxJmxServiceMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxJmxServiceMXBean jmxServiceMXBean =
                appbaseCore.getJmxService().getMXBean(JmxJmxService.class);

        if (jmxServiceMXBean == null) {
            sendInternalServerError(
                    "jmxJmxServiceMXBean",
                    JmxJmxService.class.getName() + " is not initialized",
                    httpServletResponse);
        }

        return jmxServiceMXBean;
    }
}
