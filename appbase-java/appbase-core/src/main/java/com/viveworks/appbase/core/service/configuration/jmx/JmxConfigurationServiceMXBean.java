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

package com.viveworks.appbase.core.service.configuration.jmx;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializer;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.annotation.Nullable;
import javax.management.MBeanException;
import javax.management.MXBean;
import java.util.Set;

/**
 * Represents a {@link MXBean} for {@link ConfigurationService}. It contains a subset of methods from
 * {@link ConfigurationService}, as not all methods can be made {@link MXBean} compliant.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
@SuppressWarnings("checkstyle:MethodCount")
public interface JmxConfigurationServiceMXBean extends AppbaseServiceMXBean {
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
     * Get typed configuration as <code>YAML</code>.
     *
     * @param appbaseServiceConfigurationTypeName Typed configuration name
     * @return Typed configuration serialized as YAML
     * @throws MBeanException if there are any errors
     */
    String getTypedConfigurationYaml(String appbaseServiceConfigurationTypeName) throws MBeanException;


    /**
     * Put typed configuration <code>YAML</code>.
     *
     * @param appbaseServiceConfigurationYaml Typed configuration YAML
     * @param appbaseServiceConfigurationTypeName Typed configuration name
     * @throws MBeanException if there are any errors
     */
    void putTypedConfigurationYaml(String appbaseServiceConfigurationYaml,
                                   String appbaseServiceConfigurationTypeName) throws MBeanException;


    /**
     * Remove typed configuration. Although it's stored as an object, this method is suffixed with 'Yaml', just to be
     * consistent with the <code>get</code> and <code>put</code> methods.
     *
     * @param appbaseServiceConfigurationTypeName Typed configuration name
     * @throws MBeanException if there are any errors
     */
    void removeTypedConfigurationYaml(String appbaseServiceConfigurationTypeName) throws MBeanException;


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
     * @param typeName Typed configuration class name
     * @return true if property exists, else false
     * @throws MBeanException if there are any errors
     */
    boolean containsTypedConfiguration(String typeName) throws MBeanException;


    /**
     * Unregister {@link ConfigurationService.PropertyDeserializerAndConfigurationType} by property key.
     *
     * @param propertyKey Property key
     */
    void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(String propertyKey);


    /**
     * Clear {@link ConfigurationService.PropertyDeserializerAndConfigurationType} by property key map.
     */
    void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();


    /**
     * Unregister {@link PropertyDeserializer} type.
     *
     * @param propertyDeserializerTypeName Property deserializer type name
     * @throws MBeanException if there are any errors
     */
    void unregisterPropertyDeserializer(String propertyDeserializerTypeName) throws MBeanException;


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
    Set<String> getNonValidatingTypeNames();


    /**
     * Unregister non validating {@link AppbaseConfiguration} type.
     *
     * @param appbaseServiceConfigurationTypeName Non validating configuration type name
     * @throws MBeanException if there are any errors
     */
    void unregisterNonValidatingTypeName(String appbaseServiceConfigurationTypeName) throws MBeanException;


    /**
     * Clear non validating types.
     */
    void clearNonValidatingTypeNames();


    /**
     * Get secure property keys.
     *
     * @return Secure property keys
     */
    @Unmodifiable
    Set<String> getSecurePropertyKeys();


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
    Set<String> getSecureTypeNames();


    /**
     * Unregister secure {@link AppbaseConfiguration} type.
     *
     * @param appbaseServiceConfigurationTypeName Secure configuration type name
     * @throws MBeanException if there are any errors
     */
    void unregisterSecureType(String appbaseServiceConfigurationTypeName) throws MBeanException;


    /**
     * Clear secure {@link AppbaseConfiguration} types.
     */
    void clearSecureTypes();


    /**
     * Unregister configuration type's field as secure.
     *
     * @param appbaseServiceConfigurationTypeName Configuration type name
     * @param fieldName Configuration type field name
     * @throws MBeanException if there are any errors
     */
    void unregisterSecureFieldByConfigurationType(
            String appbaseServiceConfigurationTypeName, String fieldName) throws MBeanException;


    /**
     * Clear secure field names by {@link AppbaseConfiguration} type name map.
     */
    void clearSecureFieldByConfigurationTypeMap();


    /**
     * Unregister {@link ConfigurationListener}.
     *
     * @param configurationListenerTypeName Configuration listener type name
     * @throws MBeanException if there are any errors
     */
    void unregisterGlobalConfigurationListener(String configurationListenerTypeName) throws MBeanException;


    /**
     * Clear {@link ConfigurationListener}s.
     */
    void clearGlobalConfigurationListeners();


    /**
     * Unregister {@link ConfigurationListener} by {@link JmxConfigurationKey}.
     *
     * @param jmxConfigurationKey Configuration key for JMX
     * @throws MBeanException if there are any errors
     */
    void unregisterConfigurationListenerByKey(JmxConfigurationKey jmxConfigurationKey) throws MBeanException;


    /**
     * Clear {@link ConfigurationListener} by {@link JmxConfigurationKey} map.
     */
    void clearConfigurationListenerByKeyMap();


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
}
