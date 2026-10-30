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
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationKey;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationServiceMXBean;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
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
public class ConfigurationServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = 506222130844219577L;

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                jmxConfigurationServiceMXBean(httpServletResponse);
        if (jmxConfigurationServiceMXBean == null) {
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
            if ("property".equalsIgnoreCase(operation)) {
                getProperty(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("typedConfiguration".equalsIgnoreCase(operation)) {
                getTypedConfigurationYaml(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("nonValidatingTypes".equalsIgnoreCase(operation)) {
                getNonValidatingTypes(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("securePropertyKeys".equalsIgnoreCase(operation)) {
                getSecurePropertyKeys(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("secureTypes".equalsIgnoreCase(operation)) {
                getSecureTypes(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("configurationsAsJson".equalsIgnoreCase(operation)) {
                getConfigurationsAsJson(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("status".equalsIgnoreCase(operation)) {
                status(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
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
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                jmxConfigurationServiceMXBean(httpServletResponse);
        if (jmxConfigurationServiceMXBean == null) {
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
            if ("property".equalsIgnoreCase(operation)) {
                setProperty(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("typedConfiguration".equalsIgnoreCase(operation)) {
                setTypedConfigurationYaml(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("load".equalsIgnoreCase(operation)) {
                load(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("reload".equalsIgnoreCase(operation)) {
                reloadWithConfig(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("shutdown".equalsIgnoreCase(operation)) {
                shutdown(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
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
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                jmxConfigurationServiceMXBean(httpServletResponse);
        if (jmxConfigurationServiceMXBean == null) {
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
                reload(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
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
    protected void doHead(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                jmxConfigurationServiceMXBean(httpServletResponse);
        if (jmxConfigurationServiceMXBean == null) {
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
            if ("property".equalsIgnoreCase(operation)) {
                containsProperty(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("typedConfiguration".equalsIgnoreCase(operation)) {
                containsTypedConfiguration(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else {
                sendBadRequest(operation, "Unsupported operation " + operation, httpServletResponse);
            }
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
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                jmxConfigurationServiceMXBean(httpServletResponse);
        if (jmxConfigurationServiceMXBean == null) {
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
            if ("property".equalsIgnoreCase(operation)) {
                removeProperty(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("typedConfiguration".equalsIgnoreCase(operation)) {
                removeTypedConfiguration(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey".equalsIgnoreCase(
                    operation))
            {
                unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearPropertyDeserializerAndConfigurationTypesMap".equalsIgnoreCase(operation)) {
                clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterPropertyDeserializer".equalsIgnoreCase(operation)) {
                unregisterPropertyDeserializer(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearPropertyDeserializers".equalsIgnoreCase(operation)) {
                clearPropertyDeserializers(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterNonValidatingType".equalsIgnoreCase(operation)) {
                unregisterNonValidatingType(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearNonValidatingTypes".equalsIgnoreCase(operation)) {
                clearNonValidatingTypes(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterSecurePropertyKey".equalsIgnoreCase(operation)) {
                unregisterSecurePropertyKey(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearSecurePropertyKeys".equalsIgnoreCase(operation)) {
                clearSecurePropertyKeys(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterSecureType".equalsIgnoreCase(operation)) {
                unregisterSecureType(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearSecureTypes".equalsIgnoreCase(operation)) {
                clearSecureTypes(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterSecureFieldByConfigurationType".equalsIgnoreCase(operation)) {
                unregisterSecureFieldByConfigurationType(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearSecureFieldByConfigurationTypeMap".equalsIgnoreCase(operation)) {
                clearSecureFieldByConfigurationTypeMap(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterGlobalConfigurationListener".equalsIgnoreCase(operation)) {
                unregisterGlobalConfigurationListener(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearGlobalConfigurationListeners".equalsIgnoreCase(operation)) {
                clearGlobalConfigurationListeners(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterConfigurationListenerByPropertyKey".equalsIgnoreCase(operation)) {
                unregisterConfigurationListenerByPropertyKey(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("unregisterConfigurationListenerByTypeName".equalsIgnoreCase(operation)) {
                unregisterConfigurationListenerByTypeName(
                        jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
            } else if ("clearConfigurationListeners".equalsIgnoreCase(operation)) {
                clearConfigurationListeners(jmxConfigurationServiceMXBean, httpServletRequest, httpServletResponse);
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

    private void load(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.load();
        sendNoContent(httpServletResponse);
    }


    private void shutdown(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxConfigurationServiceMXBean.shutdown();
        sendOK(httpServletResponse);
    }

    private void reloadWithConfig(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String yaml = readString(httpServletRequest);
        jmxConfigurationServiceMXBean.reload(yaml);
        sendNoContent(httpServletResponse);
    }

    private void reload(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        jmxConfigurationServiceMXBean.reload();
        sendNoContent(httpServletResponse);
    }

    private void status(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final ServiceStatus status = jmxConfigurationServiceMXBean.getStatus();
        writeOk(status.name(), httpServletResponse);
    }

    private void removeProperty(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String propertyKey = nonBlankParameter("propertyKey", httpServletRequest, httpServletResponse);
        if (propertyKey == null) {
            return;
        }

        jmxConfigurationServiceMXBean.removeString(propertyKey);
        sendNoContent(httpServletResponse);
    }

    private void removeTypedConfiguration(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        jmxConfigurationServiceMXBean.removeTypedConfigurationYaml(typeName);
        sendNoContent(httpServletResponse);
    }

    private void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String propertyKey = nonBlankParameter("propertyKey", httpServletRequest, httpServletResponse);
        if (propertyKey == null) {
            return;
        }

        jmxConfigurationServiceMXBean.unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(propertyKey);
        sendNoContent(httpServletResponse);
    }

    private void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        sendNoContent(httpServletResponse);
    }

    private void unregisterPropertyDeserializer(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        jmxConfigurationServiceMXBean.unregisterPropertyDeserializer(typeName);
        sendNoContent(httpServletResponse);
    }

    private void clearPropertyDeserializers(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearPropertyDeserializers();
        sendNoContent(httpServletResponse);
    }

    private void unregisterNonValidatingType(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        jmxConfigurationServiceMXBean.unregisterNonValidatingTypeName(typeName);
        sendNoContent(httpServletResponse);
    }

    private void clearNonValidatingTypes(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearNonValidatingTypeNames();
        sendNoContent(httpServletResponse);
    }

    private void unregisterSecurePropertyKey(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String propertyKey = nonBlankParameter("propertyKey", httpServletRequest, httpServletResponse);
        if (propertyKey == null) {
            return;
        }

        jmxConfigurationServiceMXBean.unregisterSecurePropertyKey(propertyKey);
        sendNoContent(httpServletResponse);
    }

    private void clearSecurePropertyKeys(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearSecurePropertyKeys();
        sendNoContent(httpServletResponse);
    }

    private void unregisterSecureType(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        jmxConfigurationServiceMXBean.unregisterSecureType(typeName);
        sendNoContent(httpServletResponse);
    }

    private void clearSecureTypes(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearSecureTypes();
        sendNoContent(httpServletResponse);
    }

    private void unregisterSecureFieldByConfigurationType(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        final String fieldName = nonBlankParameter("fieldName", httpServletRequest, httpServletResponse);
        if (fieldName == null) {
            return;
        }

        jmxConfigurationServiceMXBean.unregisterSecureFieldByConfigurationType(typeName, fieldName);
        sendNoContent(httpServletResponse);
    }

    private void clearSecureFieldByConfigurationTypeMap(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearSecureFieldByConfigurationTypeMap();
        sendNoContent(httpServletResponse);
    }

    private void unregisterGlobalConfigurationListener(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        jmxConfigurationServiceMXBean.unregisterGlobalConfigurationListener(typeName);
        sendNoContent(httpServletResponse);
    }

    private void clearGlobalConfigurationListeners(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearGlobalConfigurationListeners();
        sendNoContent(httpServletResponse);
    }

    private void unregisterConfigurationListenerByPropertyKey(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String propertyKey = nonBlankParameter("propertyKey", httpServletRequest, httpServletResponse);
        if (propertyKey == null) {
            return;
        }

        final JmxConfigurationKey jmxConfigurationKey = new JmxConfigurationKey(propertyKey, null);
        jmxConfigurationServiceMXBean.unregisterConfigurationListenerByKey(jmxConfigurationKey);
        sendNoContent(httpServletResponse);
    }

    private void unregisterConfigurationListenerByTypeName(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        final JmxConfigurationKey jmxConfigurationKey = new JmxConfigurationKey(null, typeName);
        jmxConfigurationServiceMXBean.unregisterConfigurationListenerByKey(jmxConfigurationKey);
        sendNoContent(httpServletResponse);
    }

    private void clearConfigurationListeners(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        jmxConfigurationServiceMXBean.clearConfigurationListenerByKeyMap();
        sendNoContent(httpServletResponse);
    }

    private void containsProperty(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String propertyKey = nonBlankParameter("propertyKey", httpServletRequest, httpServletResponse);
        if (propertyKey == null) {
            return;
        }

        if (jmxConfigurationServiceMXBean.containsProperty(propertyKey)) {
            sendOKNoContent(httpServletResponse);
            return;
        }

        sendNotFound("containsProperty", httpServletResponse);
    }

    private void containsTypedConfiguration(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        if (jmxConfigurationServiceMXBean.containsTypedConfiguration(typeName)) {
            sendOKNoContent(httpServletResponse);
            return;
        }

        sendNotFound("containsTypedConfiguration", httpServletResponse);
    }

    private void setProperty(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String propertyKey = nonBlankParameter("propertyKey", httpServletRequest, httpServletResponse);
        if (propertyKey == null) {
            return;
        }

        final String propertyValue = nonBlankParameter("propertyValue", httpServletRequest, httpServletResponse);
        if (propertyValue == null) {
            return;
        }

        jmxConfigurationServiceMXBean.putString(propertyKey, propertyValue);
        sendCreated(httpServletResponse);
    }

    private void setTypedConfigurationYaml(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        final String string = readString(httpServletRequest);
        if (string == null) {
            return;
        }

        jmxConfigurationServiceMXBean.putTypedConfigurationYaml(string, typeName);
        sendCreated(httpServletResponse);
    }

    private void getProperty(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, PropertyCastException
    {
        final String propertyKey = nonBlankParameter("propertyKey", httpServletRequest, httpServletResponse);
        if (propertyKey == null) {
            return;
        }

        final String string = jmxConfigurationServiceMXBean.getString(propertyKey);
        if (string == null) {
            sendNotFound("getProperty", httpServletResponse);
            return;
        }

        final ConfigurationValue<String> value = new ConfigurationValue<>(string);
        writeOkJson(value, ConfigurationValue.class, httpServletResponse);
    }

    private void getTypedConfigurationYaml(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException, MBeanException
    {
        final String typeName = nonBlankParameter("typeName", httpServletRequest, httpServletResponse);
        if (typeName == null) {
            return;
        }

        final String typedConfigurationYaml = jmxConfigurationServiceMXBean.getTypedConfigurationYaml(typeName);
        if (typedConfigurationYaml == null) {
            sendNotFound("getTypedConfigurationYaml", httpServletResponse);
            return;
        }

        writeOkText(typedConfigurationYaml, httpServletResponse);
    }

    private void getNonValidatingTypes(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final Set<String> nonValidatingTypeNames = jmxConfigurationServiceMXBean.getNonValidatingTypeNames();
        final SetOfStringsValue setOfStringsValue = new SetOfStringsValue(nonValidatingTypeNames);
        writeJson(setOfStringsValue, SetOfStringsValue.class, httpServletResponse);
    }

    private void getSecurePropertyKeys(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final Set<String> securePropertyKeys = jmxConfigurationServiceMXBean.getSecurePropertyKeys();
        final SetOfStringsValue setOfStringsValue = new SetOfStringsValue(securePropertyKeys);
        writeJson(setOfStringsValue, SetOfStringsValue.class, httpServletResponse);
    }

    private void getSecureTypes(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final Set<String> secureTypeNames = jmxConfigurationServiceMXBean.getSecureTypeNames();
        final SetOfStringsValue setOfStringsValue = new SetOfStringsValue(secureTypeNames);
        writeJson(setOfStringsValue, SetOfStringsValue.class, httpServletResponse);
    }

    private void getConfigurationsAsJson(
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final ConfigurationsJson configurationsJson = jmxConfigurationServiceMXBean.getConfigurationsJson();
        if (configurationsJson != null) {
            writeJson(configurationsJson, ConfigurationsJson.class, httpServletResponse);
            return;
        }

        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.flushBuffer();
    }

    private JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCore();
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                appbaseCore.getJmxService().getMXBean(JmxConfigurationService.class);
        if (jmxConfigurationServiceMXBean == null) {
            sendInternalServerError(
                    "jmxConfigurationServiceMXBean",
                    JmxConfigurationService.class.getName() + " is not initialized",
                    httpServletResponse);
            return null;
        }

        return jmxConfigurationServiceMXBean;
    }

}
