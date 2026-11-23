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

package com.viveworks.appbase.core.service.jmx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;

import javax.management.MBeanException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link JmxJmxServiceMXBean}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxJmxService
        extends AbstractAppbaseServiceMXBean<JmxService, JmxServiceConfiguration>
        implements JmxJmxServiceMXBean
{
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxJmxService.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;
    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "ALL_OPERATIONS";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "UNREGISTER_OPERATIONS";
    public static final String CLEAR_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "CLEAR_OPERATIONS";

    private static final String PROPERTY_PREFIX = AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
            JmxJmxService.class.getSimpleName().toLowerCase() +
            AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "unregisterOperations";
    public static final String CLEAR_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "clearOperations";

    public static final boolean DEFAULT_UNREGISTER_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_CLEAR_OPERATIONS_PERMISSION = true;

    public JmxJmxService(final LogService logService,
                         final JmxService appbaseService,
                         final Class<? extends JmxServiceConfiguration> appbaseServiceConfigurationType,
                         final ConfigurationService configurationService)
    {
        super(logService, appbaseService, appbaseServiceConfigurationType, configurationService);
    }

    @Override
    public String getMXBeanEntryJson(final String mxBeanTypeName) throws MBeanException {
        AppbaseUtil.notBlank(mxBeanTypeName, "mxBeanTypeName");

        final Class<?> type = getType(mxBeanTypeName);
        final MXBeanEntry mxBeanEntry = appbaseService.getMXBeanEntry(type);
        return toJson(mxBeanEntry);
    }

    @Override
    public List<String> getMXBeanEntriesJson() throws MBeanException {
        final List<String> result = new ArrayList<>();

        final Collection<MXBeanEntry> mxBeanEntries = appbaseService.getMXBeanEntries();
        if (mxBeanEntries != null) {
            for (final MXBeanEntry mxBeanEntry : mxBeanEntries) {
                final String json = toJson(mxBeanEntry);
                result.add(json);
            }
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean isMXBeanRegistered(final String mxBeanTypeName) throws MBeanException {
        AppbaseUtil.notBlank(mxBeanTypeName, "mxBeanTypeName");

        final Class<?> type = getType(mxBeanTypeName);
        try {
            return appbaseService.isMXBeanRegistered(type);
        } catch (JmxServiceException jse) {
            throw new MBeanException(jse, "Error while checking if " + mxBeanTypeName + " is registered");
        }
    }

    @Override
    public void unregisterMXBean(final String mxBeanTypeName) throws MBeanException {
        checkUnregisterPermission("unregisterMXBean");

        AppbaseUtil.notBlank(mxBeanTypeName, "mxBeanTypeName");

        final Class<?> type = getType(mxBeanTypeName);
        try {
            appbaseService.unregisterMXBean(type);
        } catch (JmxServiceException jse) {
            throw new MBeanException(jse, "Error while unregistering " + mxBeanTypeName);
        }
    }

    @Override
    public void clearMXBeans() throws MBeanException {
        checkClearPermission("clearMXBeans");

        try {
            appbaseService.clearMXBeans();
        } catch (JmxServiceException jse) {
            throw new MBeanException(jse, "Error while clearing MXBeans");
        }
    }

    private String toJson(final MXBeanEntry mxBeanEntry) throws MBeanException {
        AppbaseUtil.notNull(mxBeanEntry, "mxBeanEntry");

        try {
            return JsonUtil
                    .getJsonMapper()
                    .writerFor(MXBeanEntry.class)
                    .writeValueAsString(mxBeanEntry);
        } catch (JsonProcessingException jpe) {
            throw new MBeanException(jpe, "Error while de-serializing MXBeanEntry: " + mxBeanEntry);
        }
    }

    private void checkUnregisterPermission(final String operationName) {
        checkPermission(
                UNREGISTER_OPERATIONS_PERMISSION_ENV,
                UNREGISTER_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_UNREGISTER_OPERATIONS_PERMISSION,
                operationName);
    }

    private void checkClearPermission(final String operationName) {
        checkPermission(
                CLEAR_OPERATIONS_PERMISSION_ENV,
                CLEAR_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_CLEAR_OPERATIONS_PERMISSION,
                operationName);
    }
}
