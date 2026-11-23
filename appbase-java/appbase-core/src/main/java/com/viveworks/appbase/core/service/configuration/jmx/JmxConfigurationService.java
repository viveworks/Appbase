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

package com.viveworks.appbase.core.service.configuration.jmx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializer;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationUtil;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;

import javax.annotation.Nullable;
import javax.management.MBeanException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation for {@link JmxConfigurationServiceMXBean}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MethodCount")
public class JmxConfigurationService
        extends AbstractAppbaseServiceMXBean<ConfigurationService, ConfigurationServiceConfiguration>
        implements JmxConfigurationServiceMXBean
{
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxConfigurationService.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;

    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "PUT_OPERATIONS";
    public static final String PUT_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "PUT_OPERATIONS";
    public static final String REMOVE_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "REMOVE_OPERATIONS";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "UNREGISTER_OPERATIONS";
    public static final String CLEAR_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "CLEAR_OPERATIONS";
    public static final String SECURE_CONFIG_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "SECURE_CONFIG_OPERATIONS";

    private static final String PROPERTY_PREFIX =
            AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
                    JmxConfigurationService.class.getSimpleName().toLowerCase() +
                    AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";
    public static final String PUT_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "putOperations";
    public static final String REMOVE_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "removeOperations";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "unregisterOperations";
    public static final String CLEAR_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "clearOperations";
    public static final String SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "secureConfigOperations";

    public static final boolean DEFAULT_PUT_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_REMOVE_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_UNREGISTER_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_CLEAR_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_SECURE_CONFIG_OPERATIONS_PERMISSION = true;

    public JmxConfigurationService(
            final LogService logService,
            final ConfigurationService configurationService,
            final Class<? extends ConfigurationServiceConfiguration> appbaseServiceConfigurationType,
            @Nullable final ConfigurationService configurationService1)
    {
        super(logService,
                configurationService,
                appbaseServiceConfigurationType,
                configurationService);
    }

    @Nullable
    @Override
    public Byte getByte(final String propertyKey) throws PropertyCastException {
        return appbaseService.getByte(propertyKey);
    }

    @Override
    public Byte getByte(final String propertyKey, final Byte defaultValue) throws PropertyCastException {
        return appbaseService.getByte(propertyKey, defaultValue);
    }

    @Override
    public void putByte(final String propertyKey, final Byte value) {
        checkPutPermission("putByte");

        appbaseService.putByte(propertyKey, value);
    }

    @Override
    public void removeByte(final String propertyKey) {
        checkRemovePermission("removeByte");

        appbaseService.removeByte(propertyKey);
    }

    @Nullable
    @Override
    public byte[] getBytes(final String propertyKey) throws PropertyCastException {
        return appbaseService.getBytes(propertyKey);
    }

    @Override
    public byte[] getBytes(final String propertyKey, final byte[] defaultValue) throws PropertyCastException {
        return appbaseService.getBytes(propertyKey, defaultValue);
    }

    @Override
    public void putBytes(final String propertyKey, final byte[] value) {
        checkPutPermission("putBytes");

        appbaseService.putBytes(propertyKey, value);
    }

    @Override
    public void removeBytes(final String propertyKey) {
        checkRemovePermission("removeBytes");

        appbaseService.removeBytes(propertyKey);
    }

    @Nullable
    @Override
    public Short getShort(final String propertyKey) throws PropertyCastException {
        return appbaseService.getShort(propertyKey);
    }

    @Override
    public Short getShort(final String propertyKey, final Short defaultValue) throws PropertyCastException {
        return appbaseService.getShort(propertyKey, defaultValue);
    }

    @Override
    public void putShort(final String propertyKey, final Short value) {
        checkPutPermission("putShort");

        appbaseService.putShort(propertyKey, value);
    }

    @Override
    public void removeShort(final String propertyKey) {
        checkRemovePermission("removeShort");

        appbaseService.removeShort(propertyKey);
    }

    @Nullable
    @Override
    public Integer getInteger(final String propertyKey) throws PropertyCastException {
        return appbaseService.getInteger(propertyKey);
    }

    @Override
    public Integer getInteger(final String propertyKey, final Integer defaultValue) throws PropertyCastException {
        return appbaseService.getInteger(propertyKey, defaultValue);
    }

    @Override
    public void putInteger(final String propertyKey, final Integer value) {
        checkPutPermission("putInteger");

        appbaseService.putInteger(propertyKey, value);
    }

    @Override
    public void removeInteger(final String propertyKey) {
        checkRemovePermission("removeInteger");

        appbaseService.removeInteger(propertyKey);
    }

    @Nullable
    @Override
    public Long getLong(final String propertyKey) throws PropertyCastException {
        return appbaseService.getLong(propertyKey);
    }

    @Override
    public Long getLong(final String propertyKey, final Long defaultValue) throws PropertyCastException {
        return appbaseService.getLong(propertyKey, defaultValue);
    }

    @Override
    public void putLong(final String propertyKey, final Long value) {
        checkPutPermission("putLong");

        appbaseService.putLong(propertyKey, value);
    }

    @Override
    public void removeLong(final String propertyKey) {
        checkRemovePermission("removeLong");

        appbaseService.removeLong(propertyKey);
    }

    @Nullable
    @Override
    public Float getFloat(final String propertyKey) throws PropertyCastException {
        return appbaseService.getFloat(propertyKey);
    }

    @Override
    public Float getFloat(final String propertyKey, final Float defaultValue) throws PropertyCastException {
        return appbaseService.getFloat(propertyKey, defaultValue);
    }

    @Override
    public void putFloat(final String propertyKey, final Float value) {
        checkPutPermission("putFloat");

        appbaseService.putFloat(propertyKey, value);
    }

    @Override
    public void removeFloat(final String propertyKey) {
        checkRemovePermission("removeFloat");

        appbaseService.removeFloat(propertyKey);
    }

    @Nullable
    @Override
    public Double getDouble(final String propertyKey) throws PropertyCastException {
        return appbaseService.getDouble(propertyKey);
    }

    @Override
    public Double getDouble(final String propertyKey, final Double defaultValue) throws PropertyCastException {
        return appbaseService.getDouble(propertyKey, defaultValue);
    }

    @Override
    public void putDouble(final String propertyKey, final Double value) {
        checkPutPermission("putDouble");

        appbaseService.putDouble(propertyKey, value);
    }

    @Override
    public void removeDouble(final String propertyKey) {
        checkRemovePermission("removeDouble");

        appbaseService.removeDouble(propertyKey);
    }

    @Nullable
    @Override
    public Boolean getBoolean(final String propertyKey) throws PropertyCastException {
        return appbaseService.getBoolean(propertyKey);
    }

    @Override
    public Boolean getBoolean(final String propertyKey, final Boolean defaultValue) throws PropertyCastException {
        return appbaseService.getBoolean(propertyKey, defaultValue);
    }

    @Override
    public void putBoolean(final String propertyKey, final Boolean value) {
        checkPutPermission("putBoolean");

        appbaseService.putBoolean(propertyKey, value);
    }

    @Override
    public void removeBoolean(final String propertyKey) {
        checkRemovePermission("removeBoolean");

        appbaseService.removeBoolean(propertyKey);
    }

    @Nullable
    @Override
    public Character getCharacter(final String propertyKey) throws PropertyCastException {
        return appbaseService.getCharacter(propertyKey);
    }

    @Override
    public Character getCharacter(final String propertyKey, final Character defaultValue) throws PropertyCastException {
        return appbaseService.getCharacter(propertyKey, defaultValue);
    }

    @Override
    public void putCharacter(final String propertyKey, final Character value) {
        checkPutPermission("putCharacter");

        appbaseService.putCharacter(propertyKey, value);
    }

    @Override
    public void removeCharacter(final String propertyKey) {
        checkRemovePermission("removeCharacter");

        appbaseService.removeCharacter(propertyKey);
    }

    @Nullable
    @Override
    public String getString(final String propertyKey) throws PropertyCastException {
        if (isRestrictedSecureProperty(propertyKey)) {
            return null;
        }
        return appbaseService.getString(propertyKey);
    }

    @Override
    public String getString(final String propertyKey, final String defaultValue) throws PropertyCastException {
        if (isRestrictedSecureProperty(propertyKey)) {
            return defaultValue;
        }
        return appbaseService.getString(propertyKey, defaultValue);
    }

    @Override
    public void putString(final String propertyKey, final String value) {
        checkPutPermission("putString");

        appbaseService.putString(propertyKey, value);
    }

    @Override
    public void removeString(final String propertyKey) {
        checkRemovePermission("removeString");

        appbaseService.removeString(propertyKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getTypedConfigurationYaml(final String appbaseServiceConfigurationTypeName) throws MBeanException {
        AppbaseUtil.notBlank(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");

        if (isRestrictedSecureType(appbaseServiceConfigurationTypeName)) {
            return null;
        }

        final Class<? extends AppbaseConfiguration> type;
        try {
            type = (Class<? extends AppbaseConfiguration>) Class.forName(appbaseServiceConfigurationTypeName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + appbaseServiceConfigurationTypeName);
        }

        final AppbaseConfiguration typedConfiguration = appbaseService.getTypedConfiguration(type);
        if (typedConfiguration != null) {
            try {
                return YamlUtil.getYamlMapper()
                        .writerFor(type)
                        .writeValueAsString(typedConfiguration);
            } catch (JsonProcessingException jpe) {
                throw new MBeanException(jpe, "Error serializing " + typedConfiguration);
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putTypedConfigurationYaml(final String appbaseServiceConfigurationYaml,
                                          final String appbaseServiceConfigurationTypeName) throws MBeanException
    {
        checkPutPermission("putTypedConfigurationYaml");

        AppbaseUtil.notBlank(appbaseServiceConfigurationYaml, "appbaseServiceConfigurationYaml");
        AppbaseUtil.notBlank(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");

        // Lens 4 F3 / Lens 5 S1: resolve WITHOUT initializing (initialize=false) and apply the
        // AppbaseConfiguration constraint BEFORE any side effect runs. The previous Class.forName(name)
        // call initializes the named class as a side effect of merely naming it to this
        // permission-gated operation, and only discovered a mismatched type later -- via a
        // ClassCastException from the generic-cast checkcast once deserialization produced a
        // non-AppbaseConfiguration instance -- by which point the class's static initializer had
        // already run.
        final Class<?> resolvedConfigurationType;
        try {
            resolvedConfigurationType = Class.forName(
                    appbaseServiceConfigurationTypeName, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + appbaseServiceConfigurationTypeName);
        }

        if (!AppbaseConfiguration.class.isAssignableFrom(resolvedConfigurationType)) {
            throw new MBeanException(
                    new ClassCastException(resolvedConfigurationType.getName() + " is not an AppbaseConfiguration"),
                    "Type name " + appbaseServiceConfigurationTypeName + " is not an AppbaseConfiguration");
        }

        @SuppressWarnings("unchecked")
        final Class<AppbaseConfiguration> type = (Class<AppbaseConfiguration>) resolvedConfigurationType;

        try {
            final AppbaseConfiguration value = YamlUtil
                    .getYamlMapper()
                    .readerFor(type)
                    .readValue(appbaseServiceConfigurationYaml);
            appbaseService.putTypedConfiguration(type, value);
        } catch (JsonProcessingException jpe) {
            throw new MBeanException(jpe, "Error de-serializing " + appbaseServiceConfigurationYaml);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeTypedConfigurationYaml(final String appbaseServiceConfigurationTypeName) throws MBeanException {
        checkRemovePermission("removeTypedConfigurationYaml");

        AppbaseUtil.notBlank(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");

        final Class<AppbaseConfiguration> type;
        try {
            type = (Class<AppbaseConfiguration>) Class.forName(appbaseServiceConfigurationTypeName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + appbaseServiceConfigurationTypeName);
        }

        appbaseService.removeTypedConfiguration(type);
    }

    @Override
    public boolean containsProperty(final String propertyKey) {
        return appbaseService.containsProperty(propertyKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsTypedConfiguration(final String typeName) throws MBeanException {
        AppbaseUtil.notNull(typeName, "typeName");

        try {
            final Class<? extends AppbaseConfiguration> type =
                    (Class<? extends AppbaseConfiguration>) Class.forName(typeName);
            return appbaseService.containsTypedConfiguration(type);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + typeName);
        }
    }

    @Override
    public void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(final String propertyKey) {
        checkUnregisterPermission("unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey");

        appbaseService.unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(propertyKey);
    }

    @Override
    public void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap() {
        checkClearPermission("clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap");

        appbaseService.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPropertyDeserializer(final String propertyDeserializerTypeName) throws MBeanException {
        checkUnregisterPermission("unregisterPropertyDeserializer");

        AppbaseUtil.notNull(propertyDeserializerTypeName, "propertyDeserializerTypeName");

        try {
            final Class<? extends PropertyDeserializer> type =
                    (Class<? extends PropertyDeserializer>) Class.forName(propertyDeserializerTypeName);
            appbaseService.unregisterPropertyDeserializer(type);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + propertyDeserializerTypeName);
        }
    }

    @Override
    public void clearPropertyDeserializers() {
        checkClearPermission("clearPropertyDeserializers");

        appbaseService.clearPropertyDeserializers();
    }

    @Override
    public Set<String> getNonValidatingTypeNames() {
        final Collection<String> nonValidatingTypeNames = appbaseService.getNonValidatingTypeNames();
        if (nonValidatingTypeNames != null) {
            final HashSet<String> set = new HashSet<>(nonValidatingTypeNames);
            return Collections.unmodifiableSet(set);
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterNonValidatingTypeName(
            final String appbaseServiceConfigurationTypeName) throws MBeanException
    {
        checkUnregisterPermission("unregisterNonValidatingTypeName");

        AppbaseUtil.notNull(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");

        try {
            final Class<? extends AppbaseConfiguration> type =
                    (Class<? extends AppbaseConfiguration>) Class.forName(appbaseServiceConfigurationTypeName);
            appbaseService.unregisterNonValidatingType(type);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + appbaseServiceConfigurationTypeName);
        }
    }

    @Override
    public void clearNonValidatingTypeNames() {
        checkClearPermission("clearNonValidatingTypeNames");

        appbaseService.clearNonValidatingTypes();
    }

    @Override
    public Set<String> getSecurePropertyKeys() {
        final Collection<String> securePropertyKeys = appbaseService.getSecurePropertyKeys();
        if (securePropertyKeys != null) {
            final Set<String> set = new HashSet<>(securePropertyKeys);
            return Collections.unmodifiableSet(set);
        }

        return null;
    }

    @Override
    public void unregisterSecurePropertyKey(final String propertyKey) {
        checkUnregisterPermission("unregisterSecurePropertyKey");

        appbaseService.unregisterSecurePropertyKey(propertyKey);
    }

    @Override
    public void clearSecurePropertyKeys() {
        checkClearPermission("clearSecurePropertyKeys");

        appbaseService.clearSecurePropertyKeys();
    }

    @Override
    public Set<String> getSecureTypeNames() {
        final Collection<String> secureTypeNames = appbaseService.getSecureTypeNames();
        if (secureTypeNames != null) {
            final Set<String> set = new HashSet<>(secureTypeNames);
            return Collections.unmodifiableSet(set);
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterSecureType(final String appbaseServiceConfigurationTypeName) throws MBeanException {
        checkUnregisterPermission("unregisterSecureType");

        AppbaseUtil.notNull(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");

        try {
            final Class<? extends AppbaseConfiguration> type =
                    (Class<? extends AppbaseConfiguration>) Class.forName(appbaseServiceConfigurationTypeName);
            appbaseService.unregisterSecureType(type);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + appbaseServiceConfigurationTypeName);
        }
    }

    @Override
    public void clearSecureTypes() {
        checkClearPermission("clearSecureTypes");

        appbaseService.clearSecureTypes();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterSecureFieldByConfigurationType(
            final String appbaseServiceConfigurationTypeName,
            final String fieldName) throws MBeanException
    {
        checkUnregisterPermission("unregisterSecureFieldByConfigurationType");

        AppbaseUtil.notNull(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");

        try {
            final Class<? extends AppbaseConfiguration> type =
                    (Class<? extends AppbaseConfiguration>) Class.forName(appbaseServiceConfigurationTypeName);
            appbaseService.unregisterSecureFieldByConfigurationType(type, fieldName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + appbaseServiceConfigurationTypeName);
        }
    }

    @Override
    public void clearSecureFieldByConfigurationTypeMap() {
        checkClearPermission("clearSecureFieldByConfigurationTypeMap");

        appbaseService.clearSecureFieldByConfigurationTypeMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterGlobalConfigurationListener(
            final String configurationListenerTypeName) throws MBeanException
    {
        checkUnregisterPermission("unregisterGlobalConfigurationListener");

        AppbaseUtil.notNull(configurationListenerTypeName, "configurationListenerTypeName");

        try {
            final Class<? extends ConfigurationListener> type =
                    (Class<? extends ConfigurationListener>) Class.forName(configurationListenerTypeName);
            appbaseService.unregisterGlobalConfigurationListener(type);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name " + configurationListenerTypeName);
        }
    }

    @Override
    public void clearGlobalConfigurationListeners() {
        checkClearPermission("clearGlobalConfigurationListeners");

        appbaseService.clearGlobalConfigurationListeners();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterConfigurationListenerByKey(
            final JmxConfigurationKey jmxConfigurationKey) throws MBeanException
    {
        checkUnregisterPermission("unregisterConfigurationListenerByKey");

        AppbaseUtil.notNull(jmxConfigurationKey, "jmxConfigurationKey");

        final String propertyKey = jmxConfigurationKey.getPropertyKey();
        final String appbaseServiceConfigurationTypeName = jmxConfigurationKey.getAppbaseServiceConfigurationTypeName();
        Class<? extends AppbaseConfiguration> typeKey = null;
        if (!AppbaseUtil.isBlank(appbaseServiceConfigurationTypeName)) {
            try {
                typeKey = (Class<? extends AppbaseConfiguration>)
                        Class.forName(appbaseServiceConfigurationTypeName);
            } catch (ClassNotFoundException cnfe) {
                throw new MBeanException(cnfe, "Error with type name " + typeKey);
            }
        }

        final ConfigurationKey configurationKey;
        if (propertyKey != null && typeKey != null) {
            configurationKey = new ConfigurationKey(propertyKey, typeKey, null);
        } else if (propertyKey != null) {
            configurationKey = ConfigurationKey.fromPropertyKey(propertyKey);
        } else {
            configurationKey = ConfigurationKey.fromTypeKey(typeKey);
        }

        appbaseService.unregisterConfigurationListenerByKey(configurationKey);
    }

    @Override
    public void clearConfigurationListenerByKeyMap() {
        checkClearPermission("clearConfigurationListenerByKeyMap");

        appbaseService.clearConfigurationListenerByKeyMap();
    }

    @Override
    public void load() {
        appbaseService.load();
    }

    @Nullable
    @Override
    public ConfigurationsJson getConfigurationsJson() {
        return isSecureConfigOperationPermitted() ?
                appbaseService.getConfigurationsJson() :
                appbaseService.getSecureConfigurationsJson();
    }

    private void checkPutPermission(final String operationName) {
        checkPermission(
                PUT_OPERATIONS_PERMISSION_ENV,
                PUT_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_PUT_OPERATIONS_PERMISSION,
                operationName);
    }

    private void checkRemovePermission(final String operationName) {
        checkPermission(
                REMOVE_OPERATIONS_PERMISSION_ENV,
                REMOVE_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_REMOVE_OPERATIONS_PERMISSION,
                operationName);
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

    private boolean isSecureConfigOperationPermitted() {
        return ConfigurationUtil.config(
                configurationService,
                SECURE_CONFIG_OPERATIONS_PERMISSION_ENV,
                SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_SECURE_CONFIG_OPERATIONS_PERMISSION);
    }

    private boolean isRestrictedSecureProperty(final String propertyKey) {
        if (isSecureConfigOperationPermitted()) {
            return false;
        }
        final Collection<String> secureKeys = appbaseService.getSecurePropertyKeys();
        return secureKeys != null && secureKeys.contains(propertyKey);
    }

    private boolean isRestrictedSecureType(final String typeName) {
        if (isSecureConfigOperationPermitted()) {
            return false;
        }
        final Collection<String> secureTypes = appbaseService.getSecureTypeNames();
        if (secureTypes != null && secureTypes.contains(typeName)) {
            return true;
        }
        final Map<String, Collection<String>> secureFields =
                appbaseService.getSecureFieldNamesByConfigurationTypeNameMap();
        return secureFields != null && secureFields.containsKey(typeName);
    }
}
