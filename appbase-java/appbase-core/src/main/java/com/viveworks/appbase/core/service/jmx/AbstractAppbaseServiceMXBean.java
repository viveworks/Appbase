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
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationUtil;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.slf4j.Logger;

import javax.management.MBeanException;

/**
 * Abstract implementation of {@link AppbaseServiceMXBean}.
 *
 * <p><strong>Behaviour change (AUD-08, owner decision DQ-1, 2026-10-01):</strong> every mutating
 * operation — {@link #reload()}, {@link #reload(String)}, {@link #shutdown()} — is now DENIED by
 * default at all three permission tiers (global {@code GLOBAL_ALL_OPERATIONS}, per-bean
 * {@code ALL_OPERATIONS}, and the operation-specific permission), including shutdown. An operator
 * who previously relied on the implicit permit-by-default behaviour must now explicitly permit it.
 * Read-only operations such as {@link #getStatus()} do not call {@link #checkPermission} and are
 * unaffected.
 *
 * <p>Permission configuration reads the environment-variable key first, then the system-property
 * key. A non-null system-property value takes precedence; when that property is absent, a non-null
 * environment-variable value remains effective. If both are absent, the deny-by-default value is
 * used.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @param <S> {@link AppbaseService} type
 * @param <C> {@link AppbaseConfiguration} type
 * @since 1.0.0
 */
public abstract class AbstractAppbaseServiceMXBean<S extends AppbaseService, C extends AppbaseConfiguration>
        implements AppbaseServiceMXBean
{
    public static final String GLOBAL_ALL_OPERATIONS_PERMISSION_ENV =
            AbstractAppbaseServiceMXBean.ENV_PREFIX + "GLOBAL_ALL_OPERATIONS";
    public static final String RELOAD_OPERATIONS_PERMISSION_ENV =
            AbstractAppbaseServiceMXBean.ENV_PREFIX + "RELOAD_OPERATIONS";
    public static final String SHUTDOWN_OPERATION_PERMISSION_ENV =
            AbstractAppbaseServiceMXBean.ENV_PREFIX + "SHUTDOWN_OPERATION";

    public static final String GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY =
            AbstractAppbaseServiceMXBean.PROPERTY_PREFIX + "globalAllOperations";
    public static final String RELOAD_OPERATIONS_PERMISSION_PROPERTY =
            AbstractAppbaseServiceMXBean.PROPERTY_PREFIX + "reloadOperations";
    public static final String SHUTDOWN_OPERATION_PERMISSION_PROPERTY =
            AbstractAppbaseServiceMXBean.PROPERTY_PREFIX + "shutdownOperation";

    // ⚠️ AUD-08 (CWE-276, owner decision DQ-1, 2026-10-01): these four defaults were `true`
    // (permit-by-default) at all three permission tiers, including shutdown. Every mutating
    // operation routed through checkPermission() — reload and shutdown — is now DENIED unless the
    // effective value permits it at every tier (global, per-bean "all operations", and the
    // operation-specific permission). As described above, a non-null system-property value takes
    // precedence over the matching environment-variable value.
    // Read-only operations (e.g. getStatus()) never call checkPermission() and are unaffected.
    public static final boolean DEFAULT_GLOBAL_ALL_OPERATIONS_PERMISSION = false;
    public static final boolean DEFAULT_ALL_OPERATIONS_PERMISSION = false;
    public static final boolean DEFAULT_RELOAD_OPERATIONS_PERMISSION = false;
    public static final boolean DEFAULT_SHUTDOWN_OPERATION_PERMISSION = false;

    // Moved below the public constants above (DeclarationOrderCheck requires all public static
    // fields before any private static field); referenced from those constants' own initializers
    // via the qualified ClassName.FIELD form, which JLS 8.3.3 exempts from the simple-name
    // forward-reference restriction that would otherwise be a compile error here.
    private static final String ENV_PREFIX = "APPBASE_JMX_SERVICE_PERMISSION_";
    private static final String PROPERTY_PREFIX =
            "appbase." + AppbaseServiceMXBean.class.getSimpleName().toLowerCase() + ".permission.";

    protected final Logger logger;
    protected final S appbaseService;
    protected final Class<? extends C> appbaseServiceConfigurationType;
    protected final ConfigurationService configurationService;

    public AbstractAppbaseServiceMXBean(final LogService logService,
                                        final S appbaseService,
                                        final Class<? extends C> appbaseServiceConfigurationType,
                                        final ConfigurationService configurationService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(appbaseService, "appbaseService");
        AppbaseUtil.notNull(appbaseServiceConfigurationType, "appbaseServiceConfigurationType");
        AppbaseUtil.notNull(configurationService, "configurationService");

        this.logger = logService.getLogger(getClass());
        this.appbaseService = appbaseService;
        this.appbaseServiceConfigurationType = appbaseServiceConfigurationType;
        this.configurationService = configurationService;
    }

    @Override
    public ServiceStatus getStatus() {
        return appbaseService.getStatus();
    }

    @Override
    public void reload() throws MBeanException {
        checkReloadPermission("reload");

        C appbaseServiceConfiguration = configurationService.getTypedConfiguration(appbaseServiceConfigurationType);
        if (appbaseServiceConfiguration == null) {
            appbaseServiceConfiguration = appbaseService.getConfiguration();
            logger.info("Reloading MXBean {} with current configuration {}",
                    getClass().getName(), appbaseServiceConfiguration);
        } else {
            logger.info("Reloading MXBean {} with configuration {} from {} ",
                    getClass().getName(), appbaseServiceConfiguration, configurationService.getClass().getName());
        }

        if (appbaseServiceConfiguration == null) {
            final ServiceReloadException serviceReloadException =
                    new ServiceReloadException("Cannot find configuration for MXBean " + getClass().getName());
            throw new MBeanException(serviceReloadException);
        }

        try {
            appbaseService.reload(appbaseServiceConfiguration);
        } catch (ServiceReloadException sre) {
            throw new MBeanException(sre,
                    "Error reloading " + getClass().getName() + " using " + appbaseServiceConfiguration);
        }
    }

    @Override
    public void reload(final String appbaseServiceConfigurationYaml) throws MBeanException {
        checkReloadPermission("reloadWithConfig");

        AppbaseUtil.notBlank(appbaseServiceConfigurationYaml, "appbaseServiceConfigurationYaml");

        logger.info("Reloading MXBean {} with YAML {}", getClass().getName(), appbaseServiceConfigurationYaml);

        try {
            final C appbaseServiceConfiguration = YamlUtil
                    .getYamlMapper()
                    .readerFor(appbaseServiceConfigurationType)
                    .readValue(appbaseServiceConfigurationYaml);

            appbaseService.reload(appbaseServiceConfiguration);
        } catch (JsonProcessingException | ServiceReloadException ex) {
            final ServiceReloadException serviceReloadException =
                    new ServiceReloadException("Error while de-serializing YAML '" +
                            appbaseServiceConfigurationYaml + "' to type " + appbaseServiceConfigurationType, ex);
            throw new MBeanException(serviceReloadException);
        }
    }

    @Override
    public void shutdown() {
        checkShutdownPermission("shutdown");

        logger.info("Shutting down MXBean: {}", getClass().getName());
        appbaseService.shutdown();
    }

    protected <D extends AppbaseCoreBootstrapConfiguration> D deserializeYaml(
            final String appbaseServiceConfigurationYaml,
            final String appbaseServiceConfigurationTypeName) throws MBeanException
    {
        final Class<D> localAppbaseServiceConfigurationType = getType(appbaseServiceConfigurationTypeName);
        try {
            return YamlUtil
                    .getYamlMapper()
                    .readerFor(localAppbaseServiceConfigurationType)
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

    protected String jmxBeanAllOperationsPermissionEnv() {
        return AppbaseConstants.PERMISSION_PREFIX_ENV +
                getClass().getSimpleName().toUpperCase() +
                AppbaseConstants.PERMISSION_SUFFIX_ENV +
                AppbaseConstants.ALL_OPERATIONS_ENV;
    }

    protected String jmxBeanAllOperationsPermissionProperty() {
        return AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
                getClass().getSimpleName().toLowerCase() +
                AppbaseConstants.PERMISSION_SUFFIX_PROPERTY +
                AppbaseConstants.ALL_OPERATIONS_PROPERTY;
    }

    protected boolean jmxBeanAllOperationsPermissionDefault() {
        return DEFAULT_ALL_OPERATIONS_PERMISSION;
    }

    protected void checkPermission(
            final String environmentVariable,
            final String property,
            final boolean defaultValue,
            final String operationName)
    {
        if (!ConfigurationUtil.config(
                configurationService,
                GLOBAL_ALL_OPERATIONS_PERMISSION_ENV,
                GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_GLOBAL_ALL_OPERATIONS_PERMISSION))
        {
            throw new PermissionDeniedException(
                    GLOBAL_ALL_OPERATIONS_PERMISSION_ENV,
                    GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY,
                    DEFAULT_GLOBAL_ALL_OPERATIONS_PERMISSION,
                    "globalAllOperations");
        }

        final String jmxBeanAllOperationsPermissionEnv = jmxBeanAllOperationsPermissionEnv();
        final String jmxBeanAllOperationsPermissionProperty = jmxBeanAllOperationsPermissionProperty();
        final boolean jmxBeanAllOperationsPermissionDefault = jmxBeanAllOperationsPermissionDefault();
        if (!ConfigurationUtil.config(
                configurationService,
                jmxBeanAllOperationsPermissionEnv,
                jmxBeanAllOperationsPermissionProperty,
                jmxBeanAllOperationsPermissionDefault))
        {
            throw new PermissionDeniedException(
                    jmxBeanAllOperationsPermissionEnv,
                    jmxBeanAllOperationsPermissionProperty,
                    jmxBeanAllOperationsPermissionDefault,
                    "allOperations");
        }

        // Global and per-bean "all operations" tiers passed; check the individual override.
        // AUD-08: by default this operation is now DENIED. It must be explicitly permitted.
        if (!ConfigurationUtil.config(configurationService, environmentVariable, property, defaultValue)) {
            throw new PermissionDeniedException(environmentVariable, property, defaultValue, operationName);
        }
    }

    private void checkReloadPermission(final String operationName) {
        checkPermission(
                RELOAD_OPERATIONS_PERMISSION_ENV,
                RELOAD_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_RELOAD_OPERATIONS_PERMISSION,
                operationName);
    }

    private void checkShutdownPermission(final String operationName) {
        checkPermission(
                SHUTDOWN_OPERATION_PERMISSION_ENV,
                SHUTDOWN_OPERATION_PERMISSION_PROPERTY,
                DEFAULT_SHUTDOWN_OPERATION_PERMISSION,
                operationName);
    }
}
