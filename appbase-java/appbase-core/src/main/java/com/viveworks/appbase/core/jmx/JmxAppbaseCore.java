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

package com.viveworks.appbase.core.jmx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationUtil;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.management.MBeanException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean.DEFAULT_GLOBAL_ALL_OPERATIONS_PERMISSION;
import static com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_ENV;
import static com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY;

/**
 * Default implementation of {@link JmxAppbaseCoreMXBean}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxAppbaseCore implements JmxAppbaseCoreMXBean {
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxAppbaseCore.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;
    /**
     * Name of the environment variable for this MXBean's all-operations permission. The control checks in
     * this class read the global all-operations setting and the control-operations setting instead.
     */
    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "ALL_OPERATIONS";
    /** Name of the environment variable that grants or denies this MXBean's control operations. */
    public static final String CONTROL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "CONTROL_OPERATIONS";

    private static final String PROPERTY_PREFIX =
            AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
                    JmxAppbaseCore.class.getSimpleName().toLowerCase() +
                    AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    /**
     * Name of the system property for this MXBean's all-operations permission. The control checks in
     * this class read the global all-operations setting and the control-operations setting instead.
     */
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";
    /** Name of the system property that grants or denies this MXBean's control operations. */
    public static final String CONTROL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "controlOperations";

    /** Default for the all-operations permission when neither the environment variable nor the property is set. */
    public static final boolean DEFAULT_ALL_OPERATIONS_PERMISSION = true;
    /** Whether control operations are permitted when neither the environment variable nor the property is set. */
    public static final boolean DEFAULT_CONTROL_OPERATIONS_PERMISSION = true;

    private final Logger logger;
    private final AppbaseCore appbaseCore;
    private final ServiceRegistry serviceRegistry;
    private final JmxServiceRegistryMXBean jmxServiceRegistryMXBean;
    private final JmxJmxServiceMXBean jmxJmxServiceMXBean;

    /**
     * Creates the core MXBean facade and delegates status operations to the two supplied service MXBeans.
     *
     * @param appbaseCore core whose logger and services back management operations
     * @param serviceRegistry registry used to locate services by their implementation type
     * @param jmxServiceRegistryMXBean delegate for service-status polling and JSON snapshots
     * @param jmxJmxServiceMXBean delegate for MBean registration snapshots
     * @throws AppbaseRuntimeException if any argument is {@code null}
     */
    public JmxAppbaseCore(final AppbaseCore appbaseCore,
                          final ServiceRegistry serviceRegistry,
                          final JmxServiceRegistryMXBean jmxServiceRegistryMXBean,
                          final JmxJmxServiceMXBean jmxJmxServiceMXBean)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(serviceRegistry, "serviceRegistry");
        AppbaseUtil.notNull(jmxServiceRegistryMXBean, "jmxServiceRegistryMXBean");
        AppbaseUtil.notNull(jmxJmxServiceMXBean, "jmxJmxServiceMXBean");

        this.appbaseCore = appbaseCore;
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.serviceRegistry = serviceRegistry;
        this.jmxServiceRegistryMXBean = jmxServiceRegistryMXBean;
        this.jmxJmxServiceMXBean = jmxJmxServiceMXBean;
    }

    @Override
    public void reloadService(final String appbaseServiceTypeName,
                              final String appbaseServiceConfigurationYaml,
                              final String appbaseServiceConfigurationTypeName) throws MBeanException
    {
        checkControlPermission("reloadService");

        AppbaseUtil.notBlank(appbaseServiceTypeName, "appbaseServiceTypeName");
        AppbaseUtil.notBlank(appbaseServiceConfigurationYaml, "appbaseServiceConfigurationYaml");
        AppbaseUtil.notBlank(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");

        final Class<? extends AppbaseService> serviceType = getType(appbaseServiceTypeName);
        final AppbaseServiceEntry appbaseServiceEntry = serviceRegistry.getAppbaseService(serviceType);
        if (appbaseServiceEntry == null) {
            final String message = "Cannot find service with type name: " + appbaseServiceTypeName;
            throw new MBeanException(new AppbaseRuntimeException(message), message);
        }

        final AppbaseService appbaseService = appbaseServiceEntry.getService();
        final AppbaseConfiguration appbaseConfiguration =
                deserializeYaml(appbaseServiceConfigurationYaml, appbaseServiceConfigurationTypeName);
        try {
            appbaseService.reload(appbaseConfiguration);
        } catch (ServiceReloadException sre) {
            throw new MBeanException(sre,
                    "Error while reloading service " + appbaseServiceTypeName +
                            " using configuration " + appbaseConfiguration);
        }
        pollAndUpdateStatuses();
    }

    @Override
    public void shutdownService(final String appbaseServiceTypeName) throws MBeanException {
        checkControlPermission("shutdownService");

        AppbaseUtil.notBlank(appbaseServiceTypeName, "appbaseServiceTypeName");

        final Class<? extends AppbaseService> serviceType = getType(appbaseServiceTypeName);
        final AppbaseServiceEntry appbaseServiceEntry = serviceRegistry.getAppbaseService(serviceType);
        if (appbaseServiceEntry == null) {
            final String message = "Cannot find service with type name: " + appbaseServiceTypeName;
            throw new MBeanException(new AppbaseRuntimeException(message), message);
        }

        final AppbaseService appbaseService = appbaseServiceEntry.getService();
        appbaseService.shutdown();
        pollAndUpdateStatuses();
    }

    @Override
    public void shutdownAllServices() {
        checkControlPermission("shutdownAllServices");

        final Collection<AppbaseServiceEntry> appbaseServices = serviceRegistry.getAppbaseServices();
        if (appbaseServices != null) {
            appbaseServices.forEach(appbaseServiceEntry -> {
                final AppbaseService appbaseService = appbaseServiceEntry.getService();
                appbaseService.shutdown();
            });
        }
        pollAndUpdateStatuses();
    }

    @Override
    public void pollAndUpdateStatuses() {
        jmxServiceRegistryMXBean.pollAndUpdateStatuses();
    }

    @Override
    public String overallStatusJson() throws MBeanException {
        return jmxServiceRegistryMXBean.overallStatusJson();
    }

    @Override
    public String pollOverallStatusJson() throws MBeanException {
        return jmxServiceRegistryMXBean.pollOverallStatusJson();
    }

    @Nullable
    @Override
    public ServiceStatus getAppbaseServiceStatus(final String appbaseServiceTypeName) throws MBeanException {
        AppbaseUtil.notBlank(appbaseServiceTypeName, "appbaseServiceTypeName");

        return jmxServiceRegistryMXBean.getAppbaseServiceStatus(appbaseServiceTypeName);
    }

    @Override
    public Map<String, ServiceStatus> getAllServiceStatusByServiceTypeName() {
        return jmxServiceRegistryMXBean.getAllServiceStatusByServiceTypeName();
    }

    @Override
    public void unregisterAppbaseService(final String appbaseServiceTypeName) throws MBeanException {
        AppbaseUtil.notBlank(appbaseServiceTypeName, "appbaseServiceTypeName");

        jmxServiceRegistryMXBean.unregisterAppbaseService(appbaseServiceTypeName);
    }

    @Override
    public void clearAppbaseServices() {
        jmxServiceRegistryMXBean.clearAppbaseServices();
    }

    @Override
    public String getMXBeanEntryJson(final String mxBeanTypeName) throws MBeanException {
        AppbaseUtil.notBlank(mxBeanTypeName, "mxBeanTypeName");

        return jmxJmxServiceMXBean.getMXBeanEntryJson(mxBeanTypeName);
    }

    @Override
    public List<String> getMXBeanEntriesJson() throws MBeanException {
        return jmxJmxServiceMXBean.getMXBeanEntriesJson();
    }

    @Override
    public boolean isMXBeanRegistered(final String mxBeanTypeName) throws MBeanException {
        AppbaseUtil.notBlank(mxBeanTypeName, "mxBeanTypeName");

        return jmxJmxServiceMXBean.isMXBeanRegistered(mxBeanTypeName);
    }

    @Override
    public void unregisterMXBean(final String mxBeanTypeName) throws MBeanException {
        AppbaseUtil.notBlank(mxBeanTypeName, "mxBeanTypeName");

        jmxJmxServiceMXBean.unregisterMXBean(mxBeanTypeName);
    }

    @Override
    public void clearMXBeans() throws MBeanException {
        jmxJmxServiceMXBean.clearMXBeans();
    }

    @Override
    public ServiceStatus getStatus() {
        return appbaseCore.getStatus();
    }

    @Override
    public void reload() throws MBeanException {
        logger.info("Reloading {}", getClass().getName());
        appbaseCore.reload();
    }

    /**
     * Reload <code>core services</code> using {@link AppbaseCoreBootstrapConfiguration}.
     *
     * @param defaultAppbaseBootstrapConfigurationYaml Serialized AppbaseCoreBootstrapConfiguration
     * @throws MBeanException If there were any errors
     */
    @Override
    public void reload(final String defaultAppbaseBootstrapConfigurationYaml) throws MBeanException {
        AppbaseUtil.notBlank(defaultAppbaseBootstrapConfigurationYaml, "defaultAppbaseBootstrapConfigurationYaml");

        logger.info("Reloading {} using {}", getClass().getName(), defaultAppbaseBootstrapConfigurationYaml);
        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                deserializeYaml(
                        defaultAppbaseBootstrapConfigurationYaml,
                        DefaultAppbaseCoreBootstrapConfiguration.class.getName());
        appbaseCore.reload(appbaseCoreBootstrapConfiguration);
    }

    @Override
    public void shutdown() {
        appbaseCore.shutdown();
        jmxServiceRegistryMXBean.shutdown();
        jmxJmxServiceMXBean.shutdown();
    }

    private <C> C deserializeYaml(
            final String appbaseServiceConfigurationYaml,
            final String appbaseServiceConfigurationTypeName) throws MBeanException
    {
        final Class<C> appbaseServiceConfigurationType = getType(appbaseServiceConfigurationTypeName);

        try {
            return YamlUtil
                    .getYamlMapper()
                    .readerFor(appbaseServiceConfigurationType)
                    .readValue(appbaseServiceConfigurationYaml);
        } catch (JsonProcessingException jpe) {
            throw new MBeanException(jpe, "Error de-serializing " +
                    appbaseServiceConfigurationYaml +
                    " to type " +
                    appbaseServiceConfigurationTypeName);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> Class<T> getType(final String typeName) throws MBeanException {
        try {
            return (Class<T>) Class.forName(typeName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name: " + typeName);
        }
    }

    private void checkControlPermission(final String operationName) {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();

        ConfigurationUtil.checkPermission(
                configurationService,
                GLOBAL_ALL_OPERATIONS_PERMISSION_ENV,
                GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_GLOBAL_ALL_OPERATIONS_PERMISSION,
                "globalAllOperations");
        ConfigurationUtil.checkPermission(
                configurationService,
                GLOBAL_ALL_OPERATIONS_PERMISSION_ENV,
                GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_GLOBAL_ALL_OPERATIONS_PERMISSION,
                operationName);
        ConfigurationUtil.checkPermission(
                configurationService,
                CONTROL_OPERATIONS_PERMISSION_ENV,
                CONTROL_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_CONTROL_OPERATIONS_PERMISSION,
                operationName);
    }
}
