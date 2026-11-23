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
import com.viveworks.appbase.core.service.log.accessor.LogReadPageSize;
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
import com.viveworks.appbase.core.service.log.jmx.JmxLogServiceMXBean;
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
public class LogServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -6451599933353099240L;

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxLogServiceMXBean jmxLogServiceMXBean = jmxLogServiceMXBean(httpServletResponse);
        if (jmxLogServiceMXBean == null) {
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
            if ("logRecordsBetween".equalsIgnoreCase(operation)) {
                readLogRecordsBetween(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("logRecordsAt".equalsIgnoreCase(operation)) {
                readLogRecordsAt(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("logStoreFactoryByConfigurationTypeMap".equalsIgnoreCase(operation)) {
                getLogStoreFactoryByConfigurationTypeMap(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("logStores".equalsIgnoreCase(operation)) {
                getLogStores(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("status".equalsIgnoreCase(operation)) {
                getStatus(jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            }else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
        } catch (InvalidPageSizeException invalidPageSize) {
            sendBadRequest(operation, "Invalid pageSize", httpServletResponse);
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
        final JmxLogServiceMXBean jmxLogServiceMXBean = jmxLogServiceMXBean(httpServletResponse);
        if (jmxLogServiceMXBean == null) {
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
            if ("removeLogRecordsBetween".equalsIgnoreCase(operation)) {
                removeLogRecordsBetween(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("removeLogRecordsAt".equalsIgnoreCase(operation)) {
                removeLogRecordsAt(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterLogStoreFactoryByConfigurationType".equalsIgnoreCase(operation)) {
                unregisterLogStoreFactoryByConfigurationType(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearLogStoreFactoryByConfigurationTypeMap".equalsIgnoreCase(operation)) {
                clearLogStoreFactoryByConfigurationTypeMap(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterLogStore".equalsIgnoreCase(operation)) {
                unregisterLogStore(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearLogStores".equalsIgnoreCase(operation)) {
                clearLogStores(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
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
        final JmxLogServiceMXBean jmxLogServiceMXBean = jmxLogServiceMXBean(httpServletResponse);
        if (jmxLogServiceMXBean == null) {
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
            if ("readLogRecordsByCriteria".equalsIgnoreCase(operation)) {
                readLogRecordsByCriteria(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("removeLogRecordsByCriteria".equalsIgnoreCase(operation)) {
                removeLogRecordsByCriteria(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("synchronizeLogStores".equalsIgnoreCase(operation)) {
                synchronizeLogStores(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("reload".equalsIgnoreCase(operation)) {
                reloadWithConfig(
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
            } else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
        } catch (InvalidPageSizeException invalidPageSize) {
            sendBadRequest(operation, "Invalid pageSize", httpServletResponse);
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
        final JmxLogServiceMXBean jmxLogServiceMXBean = jmxLogServiceMXBean(httpServletResponse);
        if (jmxLogServiceMXBean == null) {
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
                        jmxLogServiceMXBean, httpServletRequest, httpServletResponse);
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
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxLogServiceMXBean.shutdown();
        sendNoContent(httpServletResponse);
    }

    private void getStatus(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxLogServiceMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void reloadWithConfig(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxLogServiceMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
    }

    private void reloadWithDefaultConfig(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxLogServiceMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    private void readLogRecordsBetween(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String inclusiveStartTimestamp = nonBlankParameter(
                "inclusiveStartTimestamp", httpServletRequest, httpServletResponse);
        if (inclusiveStartTimestamp == null) {
            return;
        }

        final String exclusiveEndTimestamp = nonBlankParameter(
                "exclusiveEndTimestamp", httpServletRequest, httpServletResponse);
        if (exclusiveEndTimestamp == null) {
            return;
        }

        final String pageSize = nonBlankParameter(
                "pageSize", httpServletRequest, httpServletResponse);
        if (pageSize == null) {
            return;
        }

        final Map<String, List<String>> result = jmxLogServiceMXBean.readLogRecordsBetween(
                Long.parseLong(inclusiveStartTimestamp),
                Long.parseLong(exclusiveEndTimestamp),
                boundedPageSize(pageSize));
        sendOK(MapOfStringListOfStringsValue.from(result),
                MapOfStringListOfStringsValue.class,
                httpServletResponse);
    }

    private void readLogRecordsAt(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {

        final String timestamp = nonBlankParameter(
                "timestamp", httpServletRequest, httpServletResponse);
        if (timestamp == null) {
            return;
        }

        final String pageSize = nonBlankParameter(
                "pageSize", httpServletRequest, httpServletResponse);
        if (pageSize == null) {
            return;
        }

        final Map<String, List<String>> result = jmxLogServiceMXBean.readLogRecordsAt(
                Long.parseLong(timestamp), boundedPageSize(pageSize));
        sendOK(MapOfStringListOfStringsValue.from(result),
                MapOfStringListOfStringsValue.class,
                httpServletResponse);
    }

    private void readLogRecordsByCriteria(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {

        final String pageSize = nonBlankParameter(
                "pageSize", httpServletRequest, httpServletResponse);
        if (pageSize == null) {
            return;
        }

        final String json = readString(httpServletRequest);
        final Map<String, List<String>> result = jmxLogServiceMXBean
                .readLogRecordsByCriteria(json, boundedPageSize(pageSize));
        sendOK(MapOfStringListOfStringsValue.from(result),
                MapOfStringListOfStringsValue.class,
                httpServletResponse);
    }

    private void removeLogRecordsBetween(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String inclusiveStartTimestamp = nonBlankParameter(
                "inclusiveStartTimestamp", httpServletRequest, httpServletResponse);
        if (inclusiveStartTimestamp == null) {
            return;
        }

        final String exclusiveEndTimestamp = nonBlankParameter(
                "exclusiveEndTimestamp", httpServletRequest, httpServletResponse);
        if (exclusiveEndTimestamp == null) {
            return;
        }

        final Map<String, Boolean> result = jmxLogServiceMXBean.removeLogRecordsBetween(
                Long.parseLong(inclusiveStartTimestamp),
                Long.parseLong(exclusiveEndTimestamp));
        sendOK(new MapOfStringBooleanValue(result), MapOfStringBooleanValue.class, httpServletResponse);
    }

    private void removeLogRecordsAt(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String timestamp = nonBlankParameter(
                "timestamp", httpServletRequest, httpServletResponse);
        if (timestamp == null) {
            return;
        }

        final Map<String, Boolean> result = jmxLogServiceMXBean.removeLogRecordsAt(Long.parseLong(timestamp));
        sendOK(new MapOfStringBooleanValue(result), MapOfStringBooleanValue.class, httpServletResponse);
    }

    private void removeLogRecordsByCriteria(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String json = readString(httpServletRequest);
        final Map<String, Boolean> result = jmxLogServiceMXBean.removeLogRecordsByCriteria(json);
        sendOK(new MapOfStringBooleanValue(result), MapOfStringBooleanValue.class, httpServletResponse);
    }

    private void getLogStoreFactoryByConfigurationTypeMap(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final Map<String, String> result = jmxLogServiceMXBean.getLogStoreFactoryByConfigurationTypeMap();
        sendOK(new MapOfStringStringValue(result), MapOfStringStringValue.class, httpServletResponse);
    }

    private void unregisterLogStoreFactoryByConfigurationType(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String logStoreFactoryTypeName = nonBlankParameter(
                "logStoreFactoryTypeName", httpServletRequest, httpServletResponse);
        if (logStoreFactoryTypeName == null) {
            return;
        }

        final String logStoreConfigurationTypeName = nonBlankParameter(
                "logStoreConfigurationTypeName", httpServletRequest, httpServletResponse);
        if (logStoreConfigurationTypeName == null) {
            return;
        }

        jmxLogServiceMXBean.unregisterLogStoreFactoryByConfigurationType(
                logStoreConfigurationTypeName,
                logStoreFactoryTypeName);
        sendNoContent(httpServletResponse);
    }

    private void clearLogStoreFactoryByConfigurationTypeMap(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxLogServiceMXBean.clearLogStoreFactoryByConfigurationTypeMap();
        sendNoContent(httpServletResponse);
    }

    private void getLogStores(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final List<String> result = jmxLogServiceMXBean.getLogStores();
        sendOK(new ListOfStringsValue(result), ListOfStringsValue.class, httpServletResponse);
    }

    private void unregisterLogStore(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String logStoreTypeName = nonBlankParameter(
                "logStoreTypeName", httpServletRequest, httpServletResponse);
        if (logStoreTypeName == null) {
            return;
        }

        jmxLogServiceMXBean.unregisterLogStore(logStoreTypeName);
        sendNoContent(httpServletResponse);
    }

    private void clearLogStores(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxLogServiceMXBean.clearLogStores();
        sendNoContent(httpServletResponse);
    }

    private void synchronizeLogStores(
            final JmxLogServiceMXBean jmxLogServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxLogServiceMXBean.synchronizeLogStores();
        sendNoContent(httpServletResponse);
    }

    private JmxLogServiceMXBean jmxLogServiceMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxLogServiceMXBean jmxLogServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxLogService.class);
        if (jmxLogServiceMXBean == null) {
            sendInternalServerError(
                    "jmxLogServiceMXBean",
                    JmxLogService.class.getName() + " is not initialized",
                    httpServletResponse);
            return null;
        }

        return jmxLogServiceMXBean;
    }

    private static int boundedPageSize(final String pageSize) {
        try {
            return LogReadPageSize.requireValid(Integer.parseInt(pageSize));
        } catch (IllegalArgumentException invalidPageSize) {
            throw new InvalidPageSizeException();
        }
    }

    private static final class InvalidPageSizeException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
    }
}
