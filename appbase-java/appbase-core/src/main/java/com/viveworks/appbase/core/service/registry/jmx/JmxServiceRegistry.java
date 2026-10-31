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

package com.viveworks.appbase.core.service.registry.jmx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.ServiceRegistryConfiguration;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;

import javax.annotation.Nullable;
import javax.management.MBeanException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link JmxServiceRegistryMXBean}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxServiceRegistry
        extends AbstractAppbaseServiceMXBean<ServiceRegistry, ServiceRegistryConfiguration>
        implements JmxServiceRegistryMXBean
{
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxServiceRegistry.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;
    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "ALL_OPERATIONS";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "UNREGISTER_OPERATIONS";
    public static final String CLEAR_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "CLEAR_OPERATIONS";

    private static final String PROPERTY_PREFIX = AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
            JmxServiceRegistry.class.getSimpleName().toLowerCase() +
            AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "unregisterOperations";
    public static final String CLEAR_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "clearOperations";

    public static final boolean DEFAULT_UNREGISTER_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_CLEAR_OPERATIONS_PERMISSION = true;

    public JmxServiceRegistry(final LogService logService,
                              final ServiceRegistry appbaseService,
                              final Class<? extends ServiceRegistryConfiguration> appbaseServiceConfigurationType,
                              final ConfigurationService configurationService)
    {
        super(logService, appbaseService, appbaseServiceConfigurationType, configurationService);
    }

    @Override
    public void pollAndUpdateStatuses() {
        appbaseService.pollAndUpdateStatuses();
    }

    @Override
    public String overallStatusJson() throws MBeanException {
        final ServiceRegistry.OverallStatus overallStatus = appbaseService.overallStatus();
        try {
            return JsonUtil
                    .getJsonMapper()
                    .writerFor(ServiceRegistry.OverallStatus.class)
                    .writeValueAsString(overallStatus);
        } catch (JsonProcessingException e) {
            throw new MBeanException(e, "Error while serializing OverallStatus");
        }
    }

    @Override
    public String pollOverallStatusJson() throws MBeanException {
        final ServiceRegistry.OverallStatus overallStatus = appbaseService.pollOverallStatus();
        try {
            return JsonUtil
                    .getJsonMapper()
                    .writerFor(ServiceRegistry.OverallStatus.class)
                    .writeValueAsString(overallStatus);
        } catch (JsonProcessingException e) {
            throw new MBeanException(e, "Error while serializing OverallStatus");
        }
    }

    @Nullable
    @Override
    public ServiceStatus getAppbaseServiceStatus(
            final String appbaseServiceTypeName) throws MBeanException
    {
        AppbaseUtil.notBlank(appbaseServiceTypeName, "appbaseServiceTypeName");

        final Class<? extends AppbaseService> appbaseServiceType = getType(appbaseServiceTypeName);
        final AppbaseServiceEntry appbaseServiceEntry = this.appbaseService.getAppbaseService(appbaseServiceType);
        if (appbaseServiceEntry != null) {
            return appbaseServiceEntry.getServiceStatus();
        }

        return null;
    }

    @Override
    public Map<String, ServiceStatus> getAllServiceStatusByServiceTypeName() {
        final Map<String, ServiceStatus> serviceStatusMap = new HashMap<>();

        final Collection<AppbaseServiceEntry> appbaseServiceEntries = appbaseService.getAppbaseServices();
        if (appbaseServiceEntries != null) {
            appbaseServiceEntries.forEach(appbaseServiceEntry -> {
                final AppbaseService service = appbaseServiceEntry.getService();
                final ServiceStatus serviceStatus = appbaseServiceEntry.getServiceStatus();

                if (service != null && serviceStatus != null) {
                    serviceStatusMap.put(service.getClass().getName(), serviceStatus);
                }
            });
        }

        return Collections.unmodifiableMap(serviceStatusMap);
    }

    @Override
    public void unregisterAppbaseService(final String appbaseServiceTypeName) throws MBeanException {
        checkUnregisterPermission("unregisterAppbaseService");

        AppbaseUtil.notBlank(appbaseServiceTypeName, "appbaseServiceTypeName");

        final Class<? extends AppbaseService> appbaseServiceType = getType(appbaseServiceTypeName);
        appbaseService.unregisterAppbaseService(appbaseServiceType);
    }

    @Override
    public void clearAppbaseServices() {
        checkClearPermission("clearAppbaseServices");

        appbaseService.clearAppbaseServices();
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
