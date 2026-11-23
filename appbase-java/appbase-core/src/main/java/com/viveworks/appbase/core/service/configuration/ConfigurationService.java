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

package com.viveworks.appbase.core.service.configuration;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializer;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializerNotFoundException;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * A service to manage configurations.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MethodCount")
public interface ConfigurationService extends AppbaseService {
    /**
     * Retrieve {@link Byte} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Byte getByte(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Byte} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Byte getByte(String propertyKey, Byte defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Byte} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putByte(String propertyKey, Byte value);


    /**
     * Remove a {@link Byte} property.
     *
     * @param propertyKey Property key
     */
    void removeByte(String propertyKey);



    /**
     * Retrieve <code>byte[]</code> property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    byte[] getBytes(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve <code>byte[]</code> property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    byte[] getBytes(String propertyKey, byte[] defaultValue) throws PropertyCastException;


    /**
     * Put a <code>byte[]</code> property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putBytes(String propertyKey, byte[] value);


    /**
     * Remove a <code>byte[]</code> property.
     *
     * @param propertyKey Property key
     */
    void removeBytes(String propertyKey);



    /**
     * Retrieve {@link Short} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Short getShort(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Short} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Short getShort(String propertyKey, Short defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Short} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putShort(String propertyKey, Short value);


    /**
     * Remove a {@link Short} property.
     *
     * @param propertyKey Property key
     */
    void removeShort(String propertyKey);



    /**
     * Retrieve {@link Integer} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Integer getInteger(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Integer} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Integer getInteger(String propertyKey, Integer defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Integer} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putInteger(String propertyKey, Integer value);


    /**
     * Remove a {@link Integer} property.
     *
     * @param propertyKey Property key
     */
    void removeInteger(String propertyKey);



    /**
     * Retrieve {@link Long} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Long getLong(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Long} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Long getLong(String propertyKey, Long defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Long} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putLong(String propertyKey, Long value);


    /**
     * Remove a {@link Long} property.
     *
     * @param propertyKey Property key
     */
    void removeLong(String propertyKey);



    /**
     * Retrieve {@link Float} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Float getFloat(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Float} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Float getFloat(String propertyKey, Float defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Float} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putFloat(String propertyKey, Float value);


    /**
     * Remove a {@link Float} property.
     *
     * @param propertyKey Property key
     */
    void removeFloat(String propertyKey);



    /**
     * Retrieve {@link Double} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Double getDouble(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Double} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Double getDouble(String propertyKey, Double defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Double} property.  If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putDouble(String propertyKey, Double value);


    /**
     * Remove a {@link Double} property.
     *
     * @param propertyKey Property key
     */
    void removeDouble(String propertyKey);



    /**
     * Retrieve {@link Boolean} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Boolean getBoolean(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Boolean} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Boolean getBoolean(String propertyKey, Boolean defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Boolean} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putBoolean(String propertyKey, Boolean value);


    /**
     * Remove a {@link Boolean} property.
     *
     * @param propertyKey Property key
     */
    void removeBoolean(String propertyKey);



    /**
     * Retrieve {@link Character} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    Character getCharacter(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link Character} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    Character getCharacter(String propertyKey, Character defaultValue) throws PropertyCastException;


    /**
     * Put a {@link Character} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putCharacter(String propertyKey, Character value);


    /**
     * Remove a {@link Character} property.
     *
     * @param propertyKey Property key
     */
    void removeCharacter(String propertyKey);



    /**
     * Retrieve {@link String} property.
     *
     * @param propertyKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    @Nullable
    String getString(String propertyKey) throws PropertyCastException;


    /**
     * Retrieve {@link String} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param propertyKey Property key
     * @param defaultValue Default property value
     * @return Property value
     * @throws PropertyCastException if the string property cannot be converted to the return type
     */
    String getString(String propertyKey, String defaultValue) throws PropertyCastException;


    /**
     * Put a {@link String} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param propertyKey Property key
     * @param value Property value
     */
    void putString(String propertyKey, String value);


    /**
     * Remove a {@link String} property.
     *
     * @param propertyKey Property key
     */
    void removeString(String propertyKey);



    /**
     * Retrieve {@link Object} property.
     *
     * @param configurationKey Property key
     * @return Property to which the key is mapped or null if no mapping exists
     */
    @Nullable
    Object getObject(ConfigurationKey configurationKey);


    /**
     * Retrieve {@link Object} property. If it does not exist, returns the <code>defaultValue</code> that is passed
     * as argument.
     *
     * @param configurationKey Configuration key
     * @param defaultValue Default property value
     * @return Property value
     */
    Object getObject(ConfigurationKey configurationKey, Object defaultValue);


    /**
     * Put a {@link Object} property. If there is a matching <code>propertyKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param configurationKey Configuration key
     * @param value Property value
     */
    void putObject(ConfigurationKey configurationKey, Object value);


    /**
     * Remove a {@link Object} property.
     *
     * @param configurationKey Configuration key
     */
    void removeObject(ConfigurationKey configurationKey);



    /**
     * Retrieve a configuration of type {@link T}.
     *
     * @param typeKey Typed configuration key
     * @param <T> The configuration type
     * @return Configuration to which the key is mapped or null if no mapping exists
     */
    @Nullable
    <T extends AppbaseConfiguration> T getTypedConfiguration(Class<T> typeKey);


    /**
     * Retrieve a configuration of type {@link T}. If it does not exist, returns the <code>defaultValue</code> that is
     * passed as argument.
     *
     * @param typeKey Typed configuration key
     * @param defaultValue Default property value
     * @param <T> The configuration type
     * @return Property value
     */
    <T extends AppbaseConfiguration> T getTypedConfiguration(Class<T> typeKey, T defaultValue);


    /**
     * Put a configuration of type {@link T}. If there is a matching <code>typedKey</code> loaded using
     * {@link ConfigurationAccessor}, this will take higher precedence.
     *
     * @param typeKey Typed configuration key
     * @param value Property value
     * @param <T> The configuration type
     */
    <T extends AppbaseConfiguration> void putTypedConfiguration(Class<T> typeKey, T value);


    /**
     * Remove a {@link Object} property.
     *
     * @param typeKey Typed configuration key
     * @param <T> The configuration type
     */
    <T extends AppbaseConfiguration> void removeTypedConfiguration(Class<T> typeKey);


    /**
     * Check if a property exists.
     *
     * @param propertyKey Property key
     * @return true if property exists, else false
     */
    boolean containsProperty(String propertyKey);


    /**
     * Check if a typed configuration exists.
     *
     * @param typeKey Typed configuration key
     * @param <T> The configuration type
     * @return true if property exists, else false
     */
    <T extends AppbaseConfiguration> boolean containsTypedConfiguration(Class<T> typeKey);


    /**
     * Return the configuration context (if any) for a {@link ConfigurationKey}.
     *
     * @param configurationKey Configuration key
     * @return Configuration context
     */
    @Nullable
    Map<String, String> getConfigurationContext(ConfigurationKey configurationKey);


    /**
     * Notify {@link ConfigurationAddedEvent}.
     *
     * @param configurationAddedEvent {@link ConfigurationAddedEvent}
     * @throws ConfigurationException if there were any errors
     */
    void notifyConfigurationAddedEvent(
            ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationException;


    /**
     * Notify {@link ConfigurationChangedEvent}.
     *
     * @param configurationChangedEvent {@link ConfigurationChangedEvent}
     * @throws ConfigurationException if there were any errors
     */
    void notifyConfigurationChangedEvent(
            ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationException;


    /**
     * Notify {@link ConfigurationRemovedEvent}.
     *
     * @param configurationRemovedEvent {@link ConfigurationRemovedEvent}
     * @throws ConfigurationException if there were any errors
     */
    void notifyConfigurationRemovedEvent(
            ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationException;


    /**
     * Get all configurations by property name mappings.
     *
     * @return Configuration by property name map
     */
    @Unmodifiable
    Map<String, Object> getConfigurationByPropertyNameMap();


    /**
     * Get all loaded configuration values by property name mappings.
     *
     * @return Configuration value by property name map
     */
    @Unmodifiable
    Map<String, ConfigurationValue<?>> getLoadedConfigurationValueByPropertyNameMap();


    /**
     * Get all configurations by type name mappings.
     *
     * @return Configuration by type name map
     */
    @Unmodifiable
    Map<String, Object> getConfigurationByTypeNameMap();


    /**
     * Get all loaded configuration values by type name mappings.
     *
     * @return Configuration value by type name map
     */
    @Unmodifiable
    Map<String, ConfigurationValue<?>> getLoadedConfigurationValueByTypeNameMap();


    /**
     * Get {@link PropertyDeserializerAndConfigurationType} by property key map.
     *
     * @return Property deserializer and configuration type by property key map
     */
    @Unmodifiable
    Map<String, PropertyDeserializerAndConfigurationType>
        getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();


    /**
     * Add property key to configuration type mapping. This can be utilized to convert a property value into
     * an object using an appropriate {@link PropertyDeserializer}. It throws a
     * {@link PropertyDeserializerNotFoundException}, if it is not added before calling this method. A
     * {@link PropertyDeserializer} can be added using {@link #registerPropertyDeserializer(PropertyDeserializer)}.
     *
     * @param propertyDeserializerAndConfigurationType Property deserializer and configuration type
     * @param propertyKey Property key
     * @throws PropertyDeserializerNotFoundException If the <code>propertyDeserializerType</code> is not added.
     */
    void registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
            String propertyKey,
            PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType)
            throws PropertyDeserializerNotFoundException;


    /**
     * Unregister {@link PropertyDeserializerAndConfigurationType} by property key.
     *
     * @param propertyKey Property key
     */
    void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(String propertyKey);


    /**
     * Clear {@link PropertyDeserializerAndConfigurationType} by property key map.
     */
    void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();


    /**
     * Get {@link PropertyDeserializer}s.
     *
     * @return Property deserializers
     */
    Collection<PropertyDeserializer> getPropertyDeserializers();


    /**
     * Add a property deserializer to use with de-serializing property values.
     * See {@link #registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
     * String, PropertyDeserializerAndConfigurationType)}.
     *
     * @param propertyDeserializer Property deserializer
     */
    void registerPropertyDeserializer(PropertyDeserializer propertyDeserializer);


    /**
     * Unregister {@link PropertyDeserializer} type.
     *
     * @param propertyDeserializerType Property deserializer type
     */
    void unregisterPropertyDeserializer(Class<? extends PropertyDeserializer> propertyDeserializerType);


    /**
     * Clear {@link PropertyDeserializer}s.
     */
    void clearPropertyDeserializers();


    /**
     * Get non validating type names.
     *
     * @return Non validating type names
     */
    @Unmodifiable
    Collection<String> getNonValidatingTypeNames();


    /**
     * Add validation to skip for a specific {@link AppbaseConfiguration} type.
     *
     * @param configurationType Configuration type
     */
    void registerNonValidatingType(Class<? extends AppbaseConfiguration> configurationType);


    /**
     * Unregister non validating {@link AppbaseConfiguration} type.
     *
     * @param configurationType Non validating configuration type
     */
    void unregisterNonValidatingType(Class<? extends AppbaseConfiguration> configurationType);


    /**
     * Clear non validating types.
     */
    void clearNonValidatingTypes();


    /**
     * Get secure property keys.
     *
     * @return Secure property keys
     */
    @Unmodifiable
    Collection<String> getSecurePropertyKeys();


    /**
     * Add property keys whose values contain secure information (e.x. passwords). This can be used to treat that
     * key 'special'. For example, to obfuscate their values in logs.
     *
     * @param propertyKey Secure property key
     */
    void registerSecurePropertyKey(String propertyKey);


    /**
     * Unregister secure property keys.
     *
     * @param propertyKey Secure property key
     */
    void unregisterSecurePropertyKey(String propertyKey);


    /**
     * Clear secure property keys.
     */
    void clearSecurePropertyKeys();


    /**
     * Get the class names of all {@link AppbaseConfiguration} types that are secure.
     *
     * @return Class names of all configuration types that are secure
     */
    @Unmodifiable
    Collection<String> getSecureTypeNames();


    /**
     * Add configuration types key whose values contain secure information (e.x. passwords). This can be used to treat
     * that type 'special'. For example, to obfuscate their values in logs.
     *
     * @param type Configuration type
     */
    void registerSecureType(Class<? extends AppbaseConfiguration> type);


    /**
     * Unregister secure {@link AppbaseConfiguration} type.
     *
     * @param type Secure configuration type
     */
    void unregisterSecureType(Class<? extends AppbaseConfiguration> type);


    /**
     * Clear secure {@link AppbaseConfiguration} types.
     */
    void clearSecureTypes();


    /**
     * Get secure field names by {@link AppbaseConfiguration} type name map.
     *
     * @return Secure field names by configuration type name map
     */
    @Unmodifiable
    Map<String, Collection<String>> getSecureFieldNamesByConfigurationTypeNameMap();


    /**
     * Add configuration type's field name key whose values contain secure information (e.x. passwords).  This can be
     * used to treat that field 'special'. For example, to obfuscate it when displayed.
     *
     * @param type Configuration type
     * @param fieldName Field name
     * @throws NoSuchFieldException If a field with name <code>fieldName</code> argument does not exist
     */
    void registerSecureFieldByConfigurationType(
            Class<? extends AppbaseConfiguration> type,
            String fieldName)
            throws NoSuchFieldException;


    /**
     * Unregister configuration type's field as secure.
     *
     * @param configurationType Configuration type
     * @param fieldName Configuration type field name
     */
    void unregisterSecureFieldByConfigurationType(
            Class<? extends AppbaseConfiguration> configurationType, String fieldName);


    /**
     * Clear secure field names by {@link AppbaseConfiguration} type name map.
     */
    void clearSecureFieldByConfigurationTypeMap();


    /**
     * Get {@link ConfigurationListener}s.
     *
     * @return Configuration listeners
     */
    @Unmodifiable
    Collection<ConfigurationListener> getGlobalConfigurationListeners();


    /**
     * Add a global configuration listener.
     *
     * @param configurationListener Configuration listener
     */
    void registerGlobalConfigurationListener(ConfigurationListener configurationListener);


    /**
     * Unregister {@link ConfigurationListener}.
     *
     * @param configurationListenerType Configuration listener type
     */
    void unregisterGlobalConfigurationListener(Class<? extends ConfigurationListener> configurationListenerType);


    /**
     * Clear {@link ConfigurationListener}s.
     */
    void clearGlobalConfigurationListeners();


    /**
     * Get {@link ConfigurationListener} by {@link ConfigurationKey} map.
     *
     * <p>
     *     <b>NOTE: This may involve building the result map from another map on each call</b>
     * </p>
     *
     * @return Configuration listener by key map
     */
    @Unmodifiable
    Map<ConfigurationKey, ConfigurationListener> getConfigurationListenerByKeyMap();


    /**
     * Add a configuration listener for a specific key.
     *
     * @param configurationListener Configuration listener
     * @param configurationKey Configuration key
     */
    void registerConfigurationListenerByKey(
            ConfigurationListener configurationListener, ConfigurationKey configurationKey);


    /**
     * Unregister {@link ConfigurationListener} by {@link ConfigurationKey}.
     *
     * @param configurationKey Configuration key
     */
    void unregisterConfigurationListenerByKey(ConfigurationKey configurationKey);


    /**
     * Unregister {@link ConfigurationListener} by {@link ConfigurationKey} and type name.
     *
     * @param configurationListenerType Configuration listener type
     * @param configurationKey Configuration key
     */
    void unregisterConfigurationListenerByKey(
            Class<? extends ConfigurationListener> configurationListenerType, ConfigurationKey configurationKey);


    /**
     * Clear {@link ConfigurationListener} by {@link ConfigurationKey} map.
     */
    void clearConfigurationListenerByKeyMap();


    /**
     * Get managed {@link AppbaseService} by {@link ConfigurationKey} map.
     *
     * <p>
     *     <b>NOTE: This may involve building the result map from another map on each call</b>
     * </p>
     *
     * @return Managed appbase service by configuration key map
     */
    Map<ConfigurationKey, AppbaseService> getManagedServiceByConfigurationKeyMap();


    /**
     * Add a service to manage for a specific configuration key. This will call
     * {@link AppbaseService#reload(AppbaseConfiguration)} if configuration
     * changes or was added for a specific configuration key and call {@link AppbaseService#shutdown()} the service if
     * the configuration was removed for a specific configuration key. These calls are made when this service
     * reloads configurations using {@link ConfigurationAccessor}.
     *
     * @param service Service to manage
     * @param configurationKey Configuration key that corresponds to a service
     */
    void registerManagedServiceByConfigurationKey(AppbaseService service, ConfigurationKey configurationKey);


    /**
     * Unregister {@link AppbaseService} by {@link ConfigurationKey}.
     *
     * @param configurationKey Configuration key
     */
    void unregisterManagedServiceByConfigurationKey(ConfigurationKey configurationKey);


    /**
     * Unregister {@link AppbaseService} by {@link ConfigurationKey} and type.
     *
     * @param appbaseServiceType Appbase service type
     * @param configurationKey Configuration key
     */
    void unregisterManagedServiceByConfigurationKey(
            Class<? extends AppbaseService> appbaseServiceType, ConfigurationKey configurationKey);


    /**
     * Clear managed {@link AppbaseService} by {@link ConfigurationKey} map.
     */
    void clearManagedServiceByConfigurationKeyMap();


    /**
     * Retrieve the {@link ConfigurationAccessor} that this service is (typically) initialized with for reading
     * and writing configurations to configuration stores.
     *
     * @return Configuration accessor
     */
    ConfigurationAccessor getConfigurationAccessor();


    /**
     * Load configurations using {@link ConfigurationAccessor}.
     */
    void load();

    /**
     * Return {@link ConfigurationsJson}.
     *
     * @return ConfigurationsJson
     */
    @Nullable
    ConfigurationsJson getConfigurationsJson();

    /**
     * Return {@link ConfigurationsJson}.
     *
     * @return ConfigurationsJson
     */
    @Nullable
    ConfigurationsJson getSecureConfigurationsJson();


    /**
     * Property de-serializer and type entry, used by
     * {@link ConfigurationService#registerPropertyDeserializerAndConfigurationTypesByPropertyKey}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    class PropertyDeserializerAndConfigurationType {
        private final Class<? extends PropertyDeserializer> propertyDeserializerType;
        private final Class<? extends AppbaseConfiguration> configurationType;

        public PropertyDeserializerAndConfigurationType(
                final Class<? extends PropertyDeserializer> propertyDeserializerType,
                final Class<? extends AppbaseConfiguration> configurationType)
        {
            AppbaseUtil.notNull(propertyDeserializerType, "propertyDeserializerType");
            AppbaseUtil.notNull(configurationType, "configurationType");

            this.propertyDeserializerType = propertyDeserializerType;
            this.configurationType = configurationType;
        }

         public Class<? extends PropertyDeserializer> getPropertyDeserializerType() {
             return propertyDeserializerType;
         }

        public Class<? extends AppbaseConfiguration> getConfigurationType() {
            return configurationType;
        }

         @Override
         public boolean equals(final Object o) {
             if (this == o) {
                 return true;
             }
             if (o == null || getClass() != o.getClass()) {
                 return false;
             }

             final PropertyDeserializerAndConfigurationType that = (PropertyDeserializerAndConfigurationType) o;
             return Objects.equals(propertyDeserializerType, that.propertyDeserializerType) &&
                     Objects.equals(configurationType, that.configurationType);
         }

         @Override
         public int hashCode() {
             return Objects.hash(propertyDeserializerType, configurationType);
         }

         @Override
         public String toString() {
             return "PropertyDeserializerAndConfigurationType{" +
                     "propertyDeserializerType=" + propertyDeserializerType +
                     ", configurationType=" + configurationType +
                     '}';
         }
     }
}
