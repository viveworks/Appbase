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

package com.viveworks.appbase.core.service.configuration.microprofile;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationException;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializer;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializerNotFoundException;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link MicroprofileConfigurationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MethodCount")
public class MicroprofileConfigurationServiceImpl implements MicroprofileConfigurationService {
    private final ConfigurationService configurationService;

    @Inject
    public MicroprofileConfigurationServiceImpl(final ConfigurationService configurationService) {
        AppbaseUtil.notNull(configurationService, "configurationService");

        this.configurationService = configurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(final String propertyName, final Class<T> propertyType) {
        if (AppbaseConfiguration.class.isAssignableFrom(propertyType)) {
            final Class<? extends AppbaseConfiguration> appbaseServiceConfigurationType =
                    (Class<? extends AppbaseConfiguration>) propertyType;
            final AppbaseConfiguration configuration = getTypedConfiguration(appbaseServiceConfigurationType);
            if (configuration != null) {
                return (T) configuration;
            }
        }

        final Object object = getObject(ConfigurationKey.fromPropertyKey(propertyName));
        if (object != null && propertyType.isAssignableFrom(object.getClass())) {
            return (T) object;
        }

        return null;
    }

    @Override
    public <T> Optional<T> getOptionalValue(final String propertyName, final Class<T> propertyType) {
        final T value = getValue(propertyName, propertyType);
        return value == null ? Optional.empty() : Optional.of(value);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        final Map<String, Object> localConfigurationValueByPropertyNameMap =
                configurationService.getConfigurationByPropertyNameMap();
        if (localConfigurationValueByPropertyNameMap != null) {
            return Collections.unmodifiableSet(localConfigurationValueByPropertyNameMap.keySet());
        }

        return null;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return getConfigurationAccessor().getConfigSources();
    }

    @Nullable
    @Override
    public Byte getByte(final String propertyKey) throws PropertyCastException {
        return configurationService.getByte(propertyKey);
    }

    @Override
    public Byte getByte(final String propertyKey, final Byte defaultValue) throws PropertyCastException {
        return configurationService.getByte(propertyKey, defaultValue);
    }

    @Override
    public void putByte(final String propertyKey, final Byte value) {
        configurationService.putByte(propertyKey, value);
    }

    @Override
    public void removeByte(final String propertyKey) {
        configurationService.removeByte(propertyKey);
    }

    @Nullable
    @Override
    public byte[] getBytes(final String propertyKey) throws PropertyCastException {
        return configurationService.getBytes(propertyKey);
    }

    @Override
    public byte[] getBytes(final String propertyKey, final byte[] defaultValue) throws PropertyCastException {
        return configurationService.getBytes(propertyKey, defaultValue);
    }

    @Override
    public void putBytes(final String propertyKey, final byte[] value) {
        configurationService.putBytes(propertyKey, value);
    }

    @Override
    public void removeBytes(final String propertyKey) {
        configurationService.removeBytes(propertyKey);
    }

    @Nullable
    @Override
    public Short getShort(final String propertyKey) throws PropertyCastException {
        return configurationService.getShort(propertyKey);
    }

    @Override
    public Short getShort(final String propertyKey, final Short defaultValue) throws PropertyCastException {
        return configurationService.getShort(propertyKey, defaultValue);
    }

    @Override
    public void putShort(final String propertyKey, final Short value) {
        configurationService.putShort(propertyKey, value);
    }

    @Override
    public void removeShort(final String propertyKey) {
        configurationService.removeShort(propertyKey);
    }

    @Nullable
    @Override
    public Integer getInteger(final String propertyKey) throws PropertyCastException {
        return configurationService.getInteger(propertyKey);
    }

    @Override
    public Integer getInteger(final String propertyKey, final Integer defaultValue) throws PropertyCastException {
        return configurationService.getInteger(propertyKey, defaultValue);
    }

    @Override
    public void putInteger(final String propertyKey, final Integer value) {
        configurationService.putInteger(propertyKey, value);
    }

    @Override
    public void removeInteger(final String propertyKey) {
        configurationService.removeInteger(propertyKey);
    }

    @Nullable
    @Override
    public Long getLong(final String propertyKey) throws PropertyCastException {
        return configurationService.getLong(propertyKey);
    }

    @Override
    public Long getLong(final String propertyKey, final Long defaultValue) throws PropertyCastException {
        return configurationService.getLong(propertyKey, defaultValue);
    }

    @Override
    public void putLong(final String propertyKey, final Long value) {
        configurationService.putLong(propertyKey, value);
    }

    @Override
    public void removeLong(final String propertyKey) {
        configurationService.removeLong(propertyKey);
    }

    @Nullable
    @Override
    public Float getFloat(final String propertyKey) throws PropertyCastException {
        return configurationService.getFloat(propertyKey);
    }

    @Override
    public Float getFloat(final String propertyKey, final Float defaultValue) throws PropertyCastException {
        return configurationService.getFloat(propertyKey, defaultValue);
    }

    @Override
    public void putFloat(final String propertyKey, final Float value) {
        configurationService.putFloat(propertyKey, value);
    }

    @Override
    public void removeFloat(final String propertyKey) {
        configurationService.removeFloat(propertyKey);
    }

    @Nullable
    @Override
    public Double getDouble(final String propertyKey) throws PropertyCastException {
        return configurationService.getDouble(propertyKey);
    }

    @Override
    public Double getDouble(final String propertyKey, final Double defaultValue) throws PropertyCastException {
        return configurationService.getDouble(propertyKey, defaultValue);
    }

    @Override
    public void putDouble(final String propertyKey, final Double value) {
        configurationService.putDouble(propertyKey, value);
    }

    @Override
    public void removeDouble(final String propertyKey) {
        configurationService.removeDouble(propertyKey);
    }

    @Nullable
    @Override
    public Boolean getBoolean(final String propertyKey) throws PropertyCastException {
        return configurationService.getBoolean(propertyKey);
    }

    @Override
    public Boolean getBoolean(final String propertyKey, final Boolean defaultValue) throws PropertyCastException {
        return configurationService.getBoolean(propertyKey, defaultValue);
    }

    @Override
    public void putBoolean(final String propertyKey, final Boolean value) {
        configurationService.putBoolean(propertyKey, value);
    }

    @Override
    public void removeBoolean(final String propertyKey) {
        configurationService.removeBoolean(propertyKey);
    }

    @Nullable
    @Override
    public Character getCharacter(final String propertyKey) throws PropertyCastException {
        return configurationService.getCharacter(propertyKey);
    }

    @Override
    public Character getCharacter(final String propertyKey, final Character defaultValue) throws PropertyCastException {
        return configurationService.getCharacter(propertyKey, defaultValue);
    }

    @Override
    public void putCharacter(final String propertyKey, final Character value) {
        configurationService.putCharacter(propertyKey, value);
    }

    @Override
    public void removeCharacter(final String propertyKey) {
        configurationService.removeCharacter(propertyKey);
    }

    @Nullable
    @Override
    public String getString(final String propertyKey) throws PropertyCastException {
        return configurationService.getString(propertyKey);
    }

    @Override
    public String getString(final String propertyKey, final String defaultValue) throws PropertyCastException {
        return configurationService.getString(propertyKey, defaultValue);
    }

    @Override
    public void putString(final String propertyKey, final String value) {
        configurationService.putString(propertyKey, value);
    }

    @Override
    public void removeString(final String propertyKey) {
        configurationService.removeString(propertyKey);
    }

    @Nullable
    @Override
    public Object getObject(final ConfigurationKey configurationKey) {
        return configurationService.getObject(configurationKey);
    }

    @Override
    public Object getObject(final ConfigurationKey configurationKey, final Object defaultValue) {
        return configurationService.getObject(configurationKey, defaultValue);
    }

    @Override
    public void putObject(final ConfigurationKey configurationKey, final Object value) {
        configurationService.putObject(configurationKey, value);
    }

    @Override
    public void removeObject(final ConfigurationKey configurationKey) {
        configurationService.removeObject(configurationKey);
    }

    @Nullable
    @Override
    public <T extends AppbaseConfiguration> T getTypedConfiguration(final Class<T> typeKey) {
        return configurationService.getTypedConfiguration(typeKey);
    }

    @Override
    public <T extends AppbaseConfiguration> T getTypedConfiguration(final Class<T> typeKey,
                                                                    final T defaultValue)
    {
        return configurationService.getTypedConfiguration(typeKey, defaultValue);
    }

    @Override
    public <T extends AppbaseConfiguration> void putTypedConfiguration(final Class<T> typeKey,
                                                                       final T value)
    {
        configurationService.putTypedConfiguration(typeKey, value);
    }

    @Override
    public <T extends AppbaseConfiguration> void removeTypedConfiguration(final Class<T> typeKey) {
        configurationService.removeTypedConfiguration(typeKey);
    }

    @Override
    public boolean containsProperty(final String propertyKey) {
        return configurationService.containsProperty(propertyKey);
    }

    @Override
    public <T extends AppbaseConfiguration> boolean containsTypedConfiguration(final Class<T> typeKey) {
        return configurationService.containsTypedConfiguration(typeKey);
    }

    @Nullable
    @Override
    public Map<String, String> getConfigurationContext(final ConfigurationKey configurationKey) {
        return configurationService.getConfigurationContext(configurationKey);
    }

    @Override
    public void notifyConfigurationAddedEvent(
            final ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationException
    {
        configurationService.notifyConfigurationAddedEvent(configurationAddedEvent);
    }

    @Override
    public void notifyConfigurationChangedEvent(
            final ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationException
    {
        configurationService.notifyConfigurationChangedEvent(configurationChangedEvent);
    }

    @Override
    public void notifyConfigurationRemovedEvent(
            final ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationException
    {
        configurationService.notifyConfigurationRemovedEvent(configurationRemovedEvent);
    }

    @Override
    public Map<String, Object> getConfigurationByPropertyNameMap() {
        return configurationService.getConfigurationByPropertyNameMap();
    }

    @Override
    public Map<String, ConfigurationValue<?>> getLoadedConfigurationValueByPropertyNameMap() {
        return configurationService.getLoadedConfigurationValueByPropertyNameMap();
    }

    @Override
    public Map<String, Object> getConfigurationByTypeNameMap() {
        return configurationService.getConfigurationByTypeNameMap();
    }

    @Override
    public Map<String, ConfigurationValue<?>> getLoadedConfigurationValueByTypeNameMap() {
        return configurationService.getLoadedConfigurationValueByTypeNameMap();
    }

    @Override
    public Map<String, PropertyDeserializerAndConfigurationType>
        getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap()
    {
        return configurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
    }

    @Override
    public void registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
            final String propertyKey,
            final PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType)
            throws PropertyDeserializerNotFoundException
    {
        configurationService.registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
                propertyKey, propertyDeserializerAndConfigurationType);
    }

    @Override
    public void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(final String propertyKey) {
        configurationService.unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(propertyKey);
    }

    @Override
    public void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap() {
        configurationService.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
    }

    @Override
    public Collection<PropertyDeserializer> getPropertyDeserializers() {
        return configurationService.getPropertyDeserializers();
    }

    @Override
    public void registerPropertyDeserializer(final PropertyDeserializer propertyDeserializer) {
        configurationService.registerPropertyDeserializer(propertyDeserializer);
    }

    @Override
    public void unregisterPropertyDeserializer(final Class<? extends PropertyDeserializer> propertyDeserializerType) {
        configurationService.unregisterPropertyDeserializer(propertyDeserializerType);
    }

    @Override
    public void clearPropertyDeserializers() {
        configurationService.clearPropertyDeserializers();
    }

    @Override
    public Collection<String> getNonValidatingTypeNames() {
        return configurationService.getNonValidatingTypeNames();
    }

    @Override
    public void registerNonValidatingType(final Class<? extends AppbaseConfiguration> configurationType) {
        configurationService.registerNonValidatingType(configurationType);
    }

    @Override
    public void unregisterNonValidatingType(final Class<? extends AppbaseConfiguration> configurationType) {
        configurationService.unregisterNonValidatingType(configurationType);
    }

    @Override
    public void clearNonValidatingTypes() {
        configurationService.clearNonValidatingTypes();
    }

    @Override
    public Collection<String> getSecurePropertyKeys() {
        return configurationService.getSecurePropertyKeys();
    }

    @Override
    public void registerSecurePropertyKey(final String propertyKey) {
        configurationService.registerSecurePropertyKey(propertyKey);
    }

    @Override
    public void unregisterSecurePropertyKey(final String propertyKey) {
        configurationService.unregisterSecurePropertyKey(propertyKey);
    }

    @Override
    public void clearSecurePropertyKeys() {
        configurationService.clearSecurePropertyKeys();
    }

    @Override
    public Collection<String> getSecureTypeNames() {
        return configurationService.getSecureTypeNames();
    }

    @Override
    public void registerSecureType(final Class<? extends AppbaseConfiguration> type) {
        configurationService.registerSecureType(type);
    }

    @Override
    public void unregisterSecureType(final Class<? extends AppbaseConfiguration> type) {
        configurationService.unregisterSecureType(type);
    }

    @Override
    public void clearSecureTypes() {
        configurationService.clearSecureTypes();
    }

    @Override
    public Map<String, Collection<String>> getSecureFieldNamesByConfigurationTypeNameMap() {
        return configurationService.getSecureFieldNamesByConfigurationTypeNameMap();
    }

    @Override
    public void registerSecureFieldByConfigurationType(
            final Class<? extends AppbaseConfiguration> type,
            final String fieldName) throws NoSuchFieldException
    {
        configurationService.registerSecureFieldByConfigurationType(type, fieldName);
    }

    @Override
    public void unregisterSecureFieldByConfigurationType(
            final Class<? extends AppbaseConfiguration> configurationType,
            final String fieldName)
    {
        configurationService.unregisterSecureFieldByConfigurationType(configurationType, fieldName);
    }

    @Override
    public void clearSecureFieldByConfigurationTypeMap() {
        configurationService.clearSecureFieldByConfigurationTypeMap();
    }

    @Override
    public Collection<ConfigurationListener> getGlobalConfigurationListeners() {
        return configurationService.getGlobalConfigurationListeners();
    }

    @Override
    public void registerGlobalConfigurationListener(final ConfigurationListener configurationListener) {
        configurationService.registerGlobalConfigurationListener(configurationListener);
    }

    @Override
    public void unregisterGlobalConfigurationListener(
            final Class<? extends ConfigurationListener> configurationListenerType)
    {
        configurationService.unregisterGlobalConfigurationListener(configurationListenerType);
    }

    @Override
    public void clearGlobalConfigurationListeners() {
        configurationService.clearGlobalConfigurationListeners();
    }

    @Override
    public Map<ConfigurationKey, ConfigurationListener> getConfigurationListenerByKeyMap() {
        return configurationService.getConfigurationListenerByKeyMap();
    }

    @Override
    public void registerConfigurationListenerByKey(
            final ConfigurationListener configurationListener,
            final ConfigurationKey configurationKey)
    {
        configurationService.registerConfigurationListenerByKey(configurationListener, configurationKey);
    }

    @Override
    public void unregisterConfigurationListenerByKey(final ConfigurationKey configurationKey) {
        configurationService.unregisterConfigurationListenerByKey(configurationKey);
    }

    @Override
    public void unregisterConfigurationListenerByKey(
            final Class<? extends ConfigurationListener> configurationListenerType,
            final ConfigurationKey configurationKey)
    {
        configurationService.unregisterConfigurationListenerByKey(configurationListenerType, configurationKey);
    }

    @Override
    public void clearConfigurationListenerByKeyMap() {
        configurationService.clearConfigurationListenerByKeyMap();
    }

    @Override
    public Map<ConfigurationKey, AppbaseService> getManagedServiceByConfigurationKeyMap() {
        return configurationService.getManagedServiceByConfigurationKeyMap();
    }

    @Override
    public void registerManagedServiceByConfigurationKey(
            final AppbaseService service,
            final ConfigurationKey configurationKey)
    {
        configurationService.registerManagedServiceByConfigurationKey(service, configurationKey);
    }

    @Override
    public void unregisterManagedServiceByConfigurationKey(final ConfigurationKey configurationKey) {
        configurationService.unregisterManagedServiceByConfigurationKey(configurationKey);
    }

    @Override
    public void unregisterManagedServiceByConfigurationKey(
            final Class<? extends AppbaseService> appbaseServiceType,
            final ConfigurationKey configurationKey)
    {
        configurationService.unregisterManagedServiceByConfigurationKey(appbaseServiceType, configurationKey);
    }

    @Override
    public void clearManagedServiceByConfigurationKeyMap() {
        configurationService.clearManagedServiceByConfigurationKeyMap();
    }

    @Override
    public ConfigurationAccessor getConfigurationAccessor() {
        return configurationService.getConfigurationAccessor();
    }

    @Override
    public void load() {
        configurationService.load();
    }

    @Nullable
    @Override
    public ConfigurationsJson getConfigurationsJson() {
        return configurationService.getConfigurationsJson();
    }

    @Nullable
    @Override
    public ConfigurationsJson getSecureConfigurationsJson() {
        return configurationService.getSecureConfigurationsJson();
    }

    @Override
    public <C extends AppbaseConfiguration> void reload(final C configuration) throws ServiceReloadException {
        configurationService.reload(configuration);
    }

    @Override
    public ServiceStatus getStatus() {
        return configurationService.getStatus();
    }

    @Override
    public <C extends AppbaseConfiguration> C getConfiguration() {
        return configurationService.getConfiguration();
    }

    @Override
    public boolean isShutdown() {
        return configurationService.isShutdown();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        configurationService.shutdown();
    }
}
