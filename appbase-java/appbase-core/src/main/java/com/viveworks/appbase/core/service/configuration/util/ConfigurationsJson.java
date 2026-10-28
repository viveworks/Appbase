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

package com.viveworks.appbase.core.service.configuration.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public class ConfigurationsJson {
    @JsonSerialize(keyAs = String.class)
    @JsonDeserialize(keyAs = String.class)
    private final Set<String> securePropertyKeys;

    @JsonSerialize(keyAs = String.class)
    @JsonDeserialize(keyAs = String.class)
    private final Set<String> secureTypeNames;

    @JsonSerialize(keyAs = String.class, contentAs = Set.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Set.class)
    private final Map<String, Set<String>> secureFieldNamesByConfigurationTypeNameMap;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> configurationByPropertyNameMap;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> loadedConfigurationValueByPropertyNameMap;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> configurationByTypeNameMap;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> loadedConfigurationValueByTypeNameMap;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> managedServiceClassNameByConfigurationKeyMap;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> configSourceClassNames;

    @JsonSerialize(keyAs = String.class, contentAs = PropertyDeserializerAndConfigurationTypeNames.class)
    @JsonDeserialize(keyAs = String.class, contentAs = PropertyDeserializerAndConfigurationTypeNames.class)
    private final Map<String, PropertyDeserializerAndConfigurationTypeNames>
            propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> nonValidatingTypeNames;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> globalConfigurationListenerClassNames;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> configurationListenerClassNameByKeyMap;

    @JsonCreator
    public ConfigurationsJson(
            @JsonProperty("securePropertyKeys")
            final Set<String> securePropertyKeys,
            @JsonProperty("secureTypeNames")
            final Set<String> secureTypeNames,
            @JsonProperty("secureFieldNamesByConfigurationTypeNameMap")
            final Map<String, Set<String>> secureFieldNamesByConfigurationTypeNameMap,
            @JsonProperty("configurationByPropertyNameMap")
            final Map<String, String> configurationByPropertyNameMap,
            @JsonProperty("loadedConfigurationValueByPropertyNameMap")
            final Map<String, String> loadedConfigurationValueByPropertyNameMap,
            @JsonProperty("configurationByTypeNameMap")
            final Map<String, String> configurationByTypeNameMap,
            @JsonProperty("loadedConfigurationValueByTypeNameMap")
            final Map<String, String> loadedConfigurationValueByTypeNameMap,
            @JsonProperty("managedServiceClassNameByConfigurationKeyMap")
            final Map<String, String> managedServiceClassNameByConfigurationKeyMap,
            @JsonProperty("configSourceClassNames")
            final Set<String> configSourceClassNames,
            @JsonProperty("propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap")
            final Map<String, PropertyDeserializerAndConfigurationTypeNames>
                    propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap,
            @JsonProperty("nonValidatingTypeNames")
            final Set<String> nonValidatingTypeNames,
            @JsonProperty("globalConfigurationListenerClassNames")
            final Set<String> globalConfigurationListenerClassNames,
            @JsonProperty("configurationListenerClassNameByKeyMap")
            final Map<String, String> configurationListenerClassNameByKeyMap)
    {
        this.securePropertyKeys = securePropertyKeys;
        this.secureTypeNames = secureTypeNames;
        this.secureFieldNamesByConfigurationTypeNameMap = secureFieldNamesByConfigurationTypeNameMap;
        this.configurationByPropertyNameMap = configurationByPropertyNameMap;
        this.loadedConfigurationValueByPropertyNameMap = loadedConfigurationValueByPropertyNameMap;
        this.configurationByTypeNameMap = configurationByTypeNameMap;
        this.loadedConfigurationValueByTypeNameMap = loadedConfigurationValueByTypeNameMap;
        this.managedServiceClassNameByConfigurationKeyMap = managedServiceClassNameByConfigurationKeyMap;
        this.configSourceClassNames = configSourceClassNames;
        this.propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap =
                propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap;
        this.nonValidatingTypeNames = nonValidatingTypeNames;
        this.globalConfigurationListenerClassNames = globalConfigurationListenerClassNames;
        this.configurationListenerClassNameByKeyMap = configurationListenerClassNameByKeyMap;
    }

    private static Map<String, String> configurationByPropertyNameMap(
            final ConfigurationService configurationService,
            final Set<String> securePropertyKeys,
            final Set<String> secureTypeNames)
    {
        AppbaseUtil.notNull(configurationService, "configurationService");

        final Map<String, String> configurationByPropertyNameMap = new HashMap<>();

        return configurationByPropertyNameMap;
    }

    private static Set<String> securePropertyKeys(final ConfigurationService configurationService) {
        final Set<String> result = new HashSet<>();

        final Collection<String> securePropertyKeys = configurationService.getSecurePropertyKeys();
        if (securePropertyKeys != null) {
            result.addAll(securePropertyKeys);
        }

        return Collections.unmodifiableSet(result);
    }

    private static Set<String> secureTypeNames(final ConfigurationService configurationService) {
        final Set<String> result = new HashSet<>();

        final Collection<String> secureTypeNames = configurationService.getSecureTypeNames();
        if (secureTypeNames != null) {
            result.addAll(secureTypeNames);
        }

        return Collections.unmodifiableSet(result);
    }

    private static Map<String, Set<String>> secureFieldNamesByConfigurationTypeNameMap(
            final ConfigurationService configurationService)
    {
        final Map<String, Set<String>> result = new HashMap<>();
        final Map<String, Collection<String>> secureFieldNamesByConfigurationTypeNameMap =
                configurationService.getSecureFieldNamesByConfigurationTypeNameMap();
        if (secureFieldNamesByConfigurationTypeNameMap != null) {
            secureFieldNamesByConfigurationTypeNameMap.forEach((configurationTypeName, secureFieldNames) -> {
                if (result.containsKey(configurationTypeName)) {
                    result.get(configurationTypeName).addAll(secureFieldNames);
                } else {
                    final Set<String> fieldNames = new HashSet<>(secureFieldNames);
                    result.put(configurationTypeName, fieldNames);
                }
            });
        }

        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> configurationByPropertyNameMap(
            final ConfigurationService configurationService,
            final Set<String> securePropertyKeys,
            final boolean filterSecureConfig)
    {
        final Map<String, String> configurationByPropertyNameMap = new HashMap<>();

        final Map<String, Object> originalConfigurationByPropertyNameMap =
                configurationService.getConfigurationByPropertyNameMap();
        if (originalConfigurationByPropertyNameMap != null) {
            originalConfigurationByPropertyNameMap.forEach((s, o) -> {
                if (filterSecureConfig && securePropertyKeys.contains(s)) {
                    return;
                }
                configurationByPropertyNameMap.put(s, String.valueOf(o));
            });
        }

        return Collections.unmodifiableMap(configurationByPropertyNameMap);
    }

    private static Map<String, String> loadedConfigurationValueByPropertyNameMap(
            final ConfigurationService configurationService,
            final Set<String> securePropertyKeys,
            final boolean filterSecureConfig)
    {
        final Map<String, String> loadedConfigurationValueByPropertyNameMap = new HashMap<>();

        final Map<String, ConfigurationValue<?>> originalLoadedConfigurationValueByPropertyNameMap =
                configurationService.getLoadedConfigurationValueByPropertyNameMap();
        if (originalLoadedConfigurationValueByPropertyNameMap != null) {
            originalLoadedConfigurationValueByPropertyNameMap.forEach((s, configurationValue) -> {
                if (filterSecureConfig && securePropertyKeys.contains(s)) {
                    return;
                }

                final Object value = configurationValue.getValue();
                if (value != null) {
                    loadedConfigurationValueByPropertyNameMap.put(s, String.valueOf(value));
                }
            });
        }

        return Collections.unmodifiableMap(loadedConfigurationValueByPropertyNameMap);
    }

    private static Map<String, String> configurationByTypeNameMap(
            final ConfigurationService configurationService,
            final Set<String> secureTypeNames,
            final Map<String, Set<String>> secureFieldNamesByConfigurationTypeNameMap,
            final boolean filterSecureConfig)
    {
        final Map<String, String> configurationByTypeNameMap = new HashMap<>();

        final Map<String, Object> originalConfigurationByTypeNameMap =
                configurationService.getConfigurationByTypeNameMap();
        if (originalConfigurationByTypeNameMap != null) {
            originalConfigurationByTypeNameMap.forEach((s, o) -> {
                if (filterSecureConfig &&
                        (secureTypeNames.contains(s) || secureFieldNamesByConfigurationTypeNameMap.containsKey(s)))
                {
                    return;
                }

                configurationByTypeNameMap.put(s, String.valueOf(o));
            });
        }

        return Collections.unmodifiableMap(configurationByTypeNameMap);
    }

    private static Map<String, String> loadedConfigurationValueByTypeNameMap(
            final ConfigurationService configurationService,
            final Set<String> secureTypeNames,
            final Map<String, Set<String>> secureFieldNamesByConfigurationTypeNameMap,
            final boolean filterSecureConfig)
    {
        final Map<String, String> loadedConfigurationValueByTypeNameMap = new HashMap<>();

        final Map<String, ConfigurationValue<?>> originalLoadedConfigurationValueByTypeNameMap =
                configurationService.getLoadedConfigurationValueByTypeNameMap();
        if (originalLoadedConfigurationValueByTypeNameMap != null) {
            originalLoadedConfigurationValueByTypeNameMap.forEach((s, configurationValue) -> {
                if (filterSecureConfig &&
                        (secureTypeNames.contains(s) || secureFieldNamesByConfigurationTypeNameMap.containsKey(s)))
                {
                    return;
                }

                final Object value = configurationValue.getValue();
                if (value != null) {
                    loadedConfigurationValueByTypeNameMap.put(s, String.valueOf(value));
                }
            });
        }

        return Collections.unmodifiableMap(loadedConfigurationValueByTypeNameMap);
    }

    private static Map<String, String> managedServiceClassNameByConfigurationKeyMap(
            final ConfigurationService configurationService)
    {
        final Map<String, String> managedServiceClassNameByConfigurationKeyMap = new HashMap<>();

        final Map<ConfigurationKey, AppbaseService> managedServiceByConfigurationKeyMap =
                configurationService.getManagedServiceByConfigurationKeyMap();
        managedServiceByConfigurationKeyMap.forEach((configurationKey, appbaseService) -> {
            managedServiceClassNameByConfigurationKeyMap
                    .put(configurationKey.getKey(), appbaseService.getClass().getName());
        });

        return Collections.unmodifiableMap(managedServiceClassNameByConfigurationKeyMap);
    }

    private static Map<String, String> configurationListenerClassNameByKeyMap(
            final ConfigurationService configurationService)
    {
        final Map<String, String> configurationListenerClassNameByKeyMap = new HashMap<>();

        final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap =
                configurationService.getConfigurationListenerByKeyMap();
        if (configurationListenerByKeyMap != null) {
            configurationListenerByKeyMap.forEach((configurationKey, configurationListener) -> {
                configurationListenerClassNameByKeyMap
                        .put(configurationKey.getKey(), configurationListener.getClass().getName());
            });
        }

        return Collections.unmodifiableMap(configurationListenerClassNameByKeyMap);
    }

    private static Set<String> configSourceClassNames(final ConfigurationService configurationService) {
        final Set<String> configSourceClassNames = new HashSet<>();

        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();
        if (configurationAccessor != null) {
            final Iterable<ConfigSource> configSources = configurationAccessor.getConfigSources();
            if (configSources != null) {
                configSources.forEach(configSource -> configSourceClassNames.add(configSource.getName()));
            }
        }

        return configSourceClassNames;
    }

    private static Map<String, PropertyDeserializerAndConfigurationTypeNames>
            propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap(
                    final ConfigurationService configurationService)
    {
        final Map<String, PropertyDeserializerAndConfigurationTypeNames>
                propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap = new HashMap<>();

        final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType>
                propertyDeserializerAndConfigurationTypesByPropertyKeyMap =
                configurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        if (propertyDeserializerAndConfigurationTypesByPropertyKeyMap != null) {
            propertyDeserializerAndConfigurationTypesByPropertyKeyMap.forEach(
                    (s, propertyDeserializerAndConfigurationType) -> {
                        propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap.put(
                                s,
                                new PropertyDeserializerAndConfigurationTypeNames(
                                        propertyDeserializerAndConfigurationType.getPropertyDeserializerType().getName(),
                                        propertyDeserializerAndConfigurationType.getConfigurationType().getName()));
                    });
        }

        return Collections.unmodifiableMap(propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap);
    }

    private static Set<String> nonValidatingTypeNames(final ConfigurationService configurationService) {
        final Set<String> nonValidatingTypeNames = new HashSet<>();

        final Collection<String> originalNonValidatingTypeNames = configurationService.getNonValidatingTypeNames();
        if (originalNonValidatingTypeNames != null) {
            nonValidatingTypeNames.addAll(originalNonValidatingTypeNames);
        }

        return Collections.unmodifiableSet(nonValidatingTypeNames);
    }

    private static Set<String> globalConfigurationListenerClassNames(final ConfigurationService configurationService) {
        final Set<String> globalConfigurationListenerClassNames = new HashSet<>();

        final Collection<ConfigurationListener> globalConfigurationListeners =
                configurationService.getGlobalConfigurationListeners();
        if (globalConfigurationListeners != null) {
            globalConfigurationListeners.forEach(configurationListener ->
                    globalConfigurationListenerClassNames.add(configurationListener.getClass().getName()));
        }

        return Collections.unmodifiableSet(globalConfigurationListenerClassNames);
    }

    public static ConfigurationsJson build(
            final ConfigurationService configurationService,
            final boolean filterSecureConfig)
    {
        AppbaseUtil.notNull(configurationService, "configurationService");

        final Set<String> securePropertyKeys = securePropertyKeys(configurationService);
        final Set<String> secureTypeNames = secureTypeNames(configurationService);
        final Map<String, Set<String>> secureFieldNamesByConfigurationTypeNameMap =
                secureFieldNamesByConfigurationTypeNameMap(configurationService);

        return new ConfigurationsJson(
                securePropertyKeys,
                secureTypeNames,
                secureFieldNamesByConfigurationTypeNameMap,
                configurationByPropertyNameMap(
                        configurationService, securePropertyKeys, filterSecureConfig),
                loadedConfigurationValueByPropertyNameMap(
                        configurationService, securePropertyKeys, filterSecureConfig),
                configurationByTypeNameMap(
                        configurationService,
                        secureTypeNames,
                        secureFieldNamesByConfigurationTypeNameMap,
                        filterSecureConfig),
                loadedConfigurationValueByTypeNameMap(
                        configurationService,
                        secureTypeNames,
                        secureFieldNamesByConfigurationTypeNameMap,
                        filterSecureConfig),
                managedServiceClassNameByConfigurationKeyMap(configurationService),
                configSourceClassNames(configurationService),
                propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap(configurationService),
                nonValidatingTypeNames(configurationService),
                globalConfigurationListenerClassNames(configurationService),
                configurationListenerClassNameByKeyMap(configurationService));
    }

    public Set<String> getSecurePropertyKeys() {
        return securePropertyKeys;
    }

    public Set<String> getSecureTypeNames() {
        return secureTypeNames;
    }

    public Map<String, Set<String>> getSecureFieldNamesByConfigurationTypeNameMap() {
        return secureFieldNamesByConfigurationTypeNameMap;
    }

    public Map<String, String> getConfigurationByPropertyNameMap() {
        return Collections.unmodifiableMap(configurationByPropertyNameMap);
    }

    public Map<String, String> getLoadedConfigurationValueByPropertyNameMap() {
        return Collections.unmodifiableMap(loadedConfigurationValueByPropertyNameMap);
    }

    public Map<String, String> getConfigurationByTypeNameMap() {
        return Collections.unmodifiableMap(configurationByTypeNameMap);
    }

    public Map<String, String> getLoadedConfigurationValueByTypeNameMap() {
        return Collections.unmodifiableMap(loadedConfigurationValueByTypeNameMap);
    }

    public Map<String, String> getManagedServiceClassNameByConfigurationKeyMap() {
        return Collections.unmodifiableMap(managedServiceClassNameByConfigurationKeyMap);
    }

    public Set<String> getConfigSourceClassNames() {
        return Collections.unmodifiableSet(configSourceClassNames);
    }

    public Map<String, PropertyDeserializerAndConfigurationTypeNames>
        getPropertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap()
    {
        return propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap;
    }

    public Set<String> getNonValidatingTypeNames() {
        return nonValidatingTypeNames;
    }

    public Set<String> getGlobalConfigurationListenerClassNames() {
        return globalConfigurationListenerClassNames;
    }

    public Map<String, String> getConfigurationListenerClassNameByKeyMap() {
        return Collections.unmodifiableMap(configurationListenerClassNameByKeyMap);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConfigurationsJson that = (ConfigurationsJson) o;
        return Objects.equals(securePropertyKeys, that.securePropertyKeys) &&
                Objects.equals(
                        secureTypeNames,
                        that.secureTypeNames) &&
                Objects.equals(
                        secureFieldNamesByConfigurationTypeNameMap,
                        that.secureFieldNamesByConfigurationTypeNameMap) &&
                Objects.equals(
                        configurationByPropertyNameMap,
                        that.configurationByPropertyNameMap) &&
                Objects.equals(
                        loadedConfigurationValueByPropertyNameMap,
                        that.loadedConfigurationValueByPropertyNameMap) &&
                Objects.equals(configurationByTypeNameMap, that.configurationByTypeNameMap) &&
                Objects.equals(
                        loadedConfigurationValueByTypeNameMap,
                        that.loadedConfigurationValueByTypeNameMap) &&
                Objects.equals(
                        managedServiceClassNameByConfigurationKeyMap,
                        that.managedServiceClassNameByConfigurationKeyMap) &&
                Objects.equals(
                        configurationListenerClassNameByKeyMap,
                        that.configurationListenerClassNameByKeyMap) &&
                Objects.equals(configSourceClassNames, that.configSourceClassNames) &&
                Objects.equals(
                        propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap,
                        that.propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap) &&
                Objects.equals(
                        nonValidatingTypeNames,
                        that.nonValidatingTypeNames) &&
                Objects.equals(
                        globalConfigurationListenerClassNames,
                        that.globalConfigurationListenerClassNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                securePropertyKeys,
                configurationByPropertyNameMap,
                loadedConfigurationValueByPropertyNameMap,
                configurationByTypeNameMap,
                loadedConfigurationValueByTypeNameMap,
                managedServiceClassNameByConfigurationKeyMap,
                configurationListenerClassNameByKeyMap,
                configSourceClassNames,
                propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap,
                nonValidatingTypeNames,
                globalConfigurationListenerClassNames);
    }

    @Override
    public String toString() {
        return "ConfigurationsJson{" +
                "securePropertyKeys=" + securePropertyKeys +
                ", configurationByPropertyNameMap=" + AppbaseUtil.OBFUSCATE +
                ", loadedConfigurationValueByPropertyNameMap=" + AppbaseUtil.OBFUSCATE +
                ", configurationByTypeNameMap=" + AppbaseUtil.OBFUSCATE +
                ", loadedConfigurationValueByTypeNameMap=" + AppbaseUtil.OBFUSCATE +
                ", managedServiceClassNameByConfigurationKeyMap=" + managedServiceClassNameByConfigurationKeyMap +
                ", configurationListenerClassNameByKeyMap=" + configurationListenerClassNameByKeyMap +
                ", configSourceClassNames=" + configSourceClassNames +
                ", propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap=" +
                propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap +
                ", nonValidatingTypeNames=" + nonValidatingTypeNames +
                ", globalConfigurationListenerClassNames=" + globalConfigurationListenerClassNames +
                '}';
    }

    public static class PropertyDeserializerAndConfigurationTypeNames {
        private final String propertyDeserializerTypeName;
        private final String configurationTypeName;

        @JsonCreator
        public PropertyDeserializerAndConfigurationTypeNames(
                @JsonProperty("propertyDeserializerTypeName")
                final String propertyDeserializerTypeName,
                @JsonProperty("configurationTypeName")
                final String configurationTypeName)
        {
            this.propertyDeserializerTypeName = propertyDeserializerTypeName;
            this.configurationTypeName = configurationTypeName;
        }

        public String getPropertyDeserializerTypeName() {
            return propertyDeserializerTypeName;
        }

        public String getConfigurationTypeName() {
            return configurationTypeName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final PropertyDeserializerAndConfigurationTypeNames that =
                    (PropertyDeserializerAndConfigurationTypeNames) o;
            return Objects.equals(propertyDeserializerTypeName, that.propertyDeserializerTypeName) &&
                    Objects.equals(configurationTypeName, that.configurationTypeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(propertyDeserializerTypeName, configurationTypeName);
        }

        @Override
        public String toString() {
            return "PropertyDeserializerAndConfigurationTypeNames{" +
                    "propertyDeserializerTypeName='" + propertyDeserializerTypeName + '\'' +
                    ", configurationTypeName='" + configurationTypeName + '\'' +
                    '}';
        }
    }
}
