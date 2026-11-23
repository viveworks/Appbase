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
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionService;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionServiceMXBean;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.ListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringBooleanValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Timer;

import javax.management.MBeanException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ExceptionServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -2311888394751246770L;

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxExceptionServiceMXBean jmxExceptionServiceMXBean =
                jmxExceptionServiceMXBean(httpServletResponse);
        if (jmxExceptionServiceMXBean == null) {
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
            if ("exceptionMessageDetails".equalsIgnoreCase(operation)) {
                readAllExceptionMessageDetails(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("exceptionMessageDetailsById".equalsIgnoreCase(operation)) {
                readExceptionMessageDetailsById(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("exceptionMessageDetailsByTimestamps".equalsIgnoreCase(operation)) {
                readAllExceptionMessageDetailsByTimestamps(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("uniqueExceptionTypeNames".equalsIgnoreCase(operation)) {
                readUniqueExceptionTypeNames(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("exceptionMessageDetailsByThrowableTypeName".equalsIgnoreCase(operation)) {
                readExceptionMessageDetailsByThrowableTypeName(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("exceptionListenersByExceptionTypeMap".equalsIgnoreCase(operation)) {
                getExceptionListenersByExceptionTypeMap(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("globalExceptionListeners".equalsIgnoreCase(operation)) {
                getGlobalExceptionListeners(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("exceptionStoreFactoryByConfigurationTypeNameMap".equalsIgnoreCase(operation)) {
                getExceptionStoreFactoryByConfigurationTypeNameMap(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("exceptionStores".equalsIgnoreCase(operation)) {
                getExceptionStores(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("status".equalsIgnoreCase(operation)) {
                getStatus(jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
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

    private void getStatus(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxExceptionServiceMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void getExceptionStores(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final List<String> exceptionStores = jmxExceptionServiceMXBean.getExceptionStores();
        sendOK(new ListOfStringsValue(exceptionStores), ListOfStringsValue.class, httpServletResponse);
    }

    private void getExceptionStoreFactoryByConfigurationTypeNameMap(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final Map<String, String> result =
                jmxExceptionServiceMXBean.getExceptionStoreFactoryByConfigurationTypeNameMap();
        sendOK(new MapOfStringStringValue(result), MapOfStringStringValue.class, httpServletResponse);
    }

    private void getGlobalExceptionListeners(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final List<String> result = jmxExceptionServiceMXBean.getGlobalExceptionListeners();
        sendOK(new ListOfStringsValue(result), ListOfStringsValue.class, httpServletResponse);
    }

    private void getExceptionListenersByExceptionTypeMap(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final Map<String, List<String>> result =
                jmxExceptionServiceMXBean.getExceptionListenersByExceptionTypeMap();
        sendOK(MapOfStringListOfStringsValue.from(result),
                MapOfStringListOfStringsValue.class,
                httpServletResponse);
    }

    private void readExceptionMessageDetailsByThrowableTypeName(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String throwableTypeName = nonBlankParameter(
                "throwableTypeName", httpServletRequest, httpServletResponse);
        if (throwableTypeName == null) {
            return;
        }

        final Map<String, List<String>> result =
                jmxExceptionServiceMXBean.readExceptionMessageDetailsByThrowableTypeName(throwableTypeName);
        sendOK(MapOfStringListOfStringsValue.from(result), MapOfStringListOfStringsValue.class, httpServletResponse);
    }

    private void readUniqueExceptionTypeNames(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final Map<String, List<String>> result = jmxExceptionServiceMXBean.readUniqueExceptionTypeNames();
        sendOK(MapOfStringListOfStringsValue.from(result), MapOfStringListOfStringsValue.class, httpServletResponse);
    }

    private void readAllExceptionMessageDetailsByTimestamps(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String inclusiveMinTimestamp =
                nonBlankParameter("inclusiveMinTimestamp", httpServletRequest, httpServletResponse);
        if (inclusiveMinTimestamp == null) {
            return;
        }

        final String exclusiveMaxTimestamp =
                nonBlankParameter("exclusiveMaxTimestamp", httpServletRequest, httpServletResponse);
        if (exclusiveMaxTimestamp == null) {
            return;
        }

        final Map<String, List<String>> result = jmxExceptionServiceMXBean
                .readAllExceptionMessageDetailsByTimestamps(
                        Long.parseLong(inclusiveMinTimestamp), Long.parseLong(exclusiveMaxTimestamp));
        sendOK(MapOfStringListOfStringsValue.from(result), MapOfStringListOfStringsValue.class, httpServletResponse);
    }

    private void readAllExceptionMessageDetails(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final Map<String, List<String>> result = jmxExceptionServiceMXBean.readAllExceptionMessageDetails();
        sendOK(MapOfStringListOfStringsValue.from(result), MapOfStringListOfStringsValue.class, httpServletResponse);
    }

    private void readExceptionMessageDetailsById(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String id = nonBlankParameter("id", httpServletRequest, httpServletResponse);
        if (id == null) {
            return;
        }

        final Map<String, String> result =
                jmxExceptionServiceMXBean.readExceptionMessageDetailsById(Integer.parseInt(id));
        writeOkJson(new MapOfStringStringValue(result), MapOfStringStringValue.class, httpServletResponse);
    }

    @Override
    protected void doPost(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws IOException
    {
        final JmxExceptionServiceMXBean jmxExceptionServiceMXBean =
                jmxExceptionServiceMXBean(httpServletResponse);
        if (jmxExceptionServiceMXBean == null) {
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
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
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
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxExceptionServiceMXBean.shutdown();
        sendNoContent(httpServletResponse);
    }

    private void reloadWithDefaultConfig(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxExceptionServiceMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    @Override
    protected void doPut(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxExceptionServiceMXBean jmxExceptionServiceMXBean =
                jmxExceptionServiceMXBean(httpServletResponse);
        if (jmxExceptionServiceMXBean == null) {
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
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
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

    private void reloadWithConfig(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxExceptionServiceMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
    }

    @Override
    protected void doDelete(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxExceptionServiceMXBean jmxExceptionServiceMXBean =
                jmxExceptionServiceMXBean(httpServletResponse);
        if (jmxExceptionServiceMXBean == null) {
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
            if ("removeExceptionMessageDetailsById".equalsIgnoreCase(operation)) {
                removeExceptionMessageDetailsById(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("removeExceptionMessageDetailsExclusiveBefore".equalsIgnoreCase(operation)) {
                removeExceptionMessageDetailsExclusiveBefore(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterExceptionListenerByExceptionType".equalsIgnoreCase(operation)) {
                unregisterExceptionListenerByExceptionType(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearExceptionListenerByExceptionTypeMap".equalsIgnoreCase(operation)) {
                clearExceptionListenerByExceptionTypeMap(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterGlobalExceptionListener".equalsIgnoreCase(operation)) {
                unregisterGlobalExceptionListener(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearGlobalExceptionListeners".equalsIgnoreCase(operation)) {
                clearGlobalExceptionListeners(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterExceptionStoreFactoryByConfigurationType".equalsIgnoreCase(operation)) {
                unregisterExceptionStoreFactoryByConfigurationType(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearExceptionStoreFactoryByConfigurationTypeMap".equalsIgnoreCase(operation)) {
                clearExceptionStoreFactoryByConfigurationTypeMap(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterExceptionStore".equalsIgnoreCase(operation)) {
                unregisterExceptionStore(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearExceptionStores".equalsIgnoreCase(operation)) {
                clearExceptionStores(
                        jmxExceptionServiceMXBean, httpServletRequest, httpServletResponse);
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

    private void clearExceptionStores(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxExceptionServiceMXBean.clearExceptionStores();
        sendNoContent(httpServletResponse);
    }

    private void unregisterExceptionStore(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String exceptionStoreTypeName =
                nonBlankParameter("exceptionStoreTypeName", httpServletRequest, httpServletResponse);
        if (exceptionStoreTypeName == null) {
            return;
        }

        jmxExceptionServiceMXBean.unregisterExceptionStore(exceptionStoreTypeName);
        sendNoContent(httpServletResponse);
    }

    private void clearExceptionStoreFactoryByConfigurationTypeMap(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxExceptionServiceMXBean.clearExceptionStoreFactoryByConfigurationTypeMap();
        sendNoContent(httpServletResponse);
    }

    private void unregisterExceptionStoreFactoryByConfigurationType(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String exceptionStoreConfigurationTypeName =
                nonBlankParameter("exceptionStoreConfigurationTypeName", httpServletRequest, httpServletResponse);
        if (exceptionStoreConfigurationTypeName == null) {
            return;
        }

        final String exceptionStoreFactoryTypeName =
                nonBlankParameter("exceptionStoreFactoryTypeName", httpServletRequest, httpServletResponse);
        if (exceptionStoreFactoryTypeName == null) {
            return;
        }

        jmxExceptionServiceMXBean.unregisterExceptionStoreFactoryByConfigurationType(
                exceptionStoreConfigurationTypeName, exceptionStoreFactoryTypeName);
        sendNoContent(httpServletResponse);
    }

    private void clearGlobalExceptionListeners(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxExceptionServiceMXBean.clearGlobalExceptionListeners();
        sendNoContent(httpServletResponse);
    }

    private void unregisterGlobalExceptionListener(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String exceptionListenerTypeName =
                nonBlankParameter("exceptionListenerTypeName", httpServletRequest, httpServletResponse);
        if (exceptionListenerTypeName == null) {
            return;
        }

        jmxExceptionServiceMXBean.unregisterGlobalExceptionListener(exceptionListenerTypeName);
        sendNoContent(httpServletResponse);
    }

    private void clearExceptionListenerByExceptionTypeMap(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxExceptionServiceMXBean.clearExceptionListenersByExceptionTypeMap();
        sendNoContent(httpServletResponse);
    }

    private void unregisterExceptionListenerByExceptionType(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String exceptionTypeName =
                nonBlankParameter("exceptionTypeName", httpServletRequest, httpServletResponse);
        if (exceptionTypeName == null) {
            return;
        }

        final String exceptionListenerTypeName =
                nonBlankParameter("exceptionListenerTypeName", httpServletRequest, httpServletResponse);
        if (exceptionListenerTypeName == null) {
            return;
        }

        jmxExceptionServiceMXBean.unregisterExceptionListenerByExceptionType(
                exceptionTypeName, exceptionListenerTypeName);
        sendNoContent(httpServletResponse);
    }

    private void removeExceptionMessageDetailsExclusiveBefore(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String timestamp = nonBlankParameter("timestamp", httpServletRequest, httpServletResponse);
        if (timestamp == null) {
            return;
        }

        final Map<String, Boolean> result =
                jmxExceptionServiceMXBean.removeExceptionMessageDetailsExclusiveBefore(Long.parseLong(timestamp));
        sendOK(new MapOfStringBooleanValue(result), MapOfStringBooleanValue.class, httpServletResponse);
    }

    private void removeExceptionMessageDetailsById(
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String id = nonBlankParameter("id", httpServletRequest, httpServletResponse);
        if (id == null) {
            return;
        }

        final Map<String, Boolean> result =
                jmxExceptionServiceMXBean.removeExceptionMessageDetailsById(Integer.parseInt(id));
        sendOK(new MapOfStringBooleanValue(result), MapOfStringBooleanValue.class, httpServletResponse);
    }

    private JmxExceptionServiceMXBean jmxExceptionServiceMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxExceptionServiceMXBean jmxExceptionServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxExceptionService.class);
        if (jmxExceptionServiceMXBean == null) {
            sendInternalServerError(
                    "jmxExceptionServiceMXBean",
                    JmxExceptionService.class.getName() + " is not initialized",
                    httpServletResponse);
            return null;
        }

        return jmxExceptionServiceMXBean;
    }
}
