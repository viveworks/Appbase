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

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import com.viveworks.appbase.core.util.jackson.YamlUtil;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Used to load (and reload) bootstrap configuration file.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class CoreBootstrapConfigurationLoader {
    private final String configurationFilePathFromSystem;
    private final Map<String, Class<? extends AppbaseConfiguration>>
            configurationTypeByYamlNodeNameToDecodeMap;
    private final ConfigurationService configurationService;
    private final AtomicReference<Map<String, AppbaseConfiguration>> configurationByTypeNameMapRef;

    /**
     * Creates a loader whose successful {@link #load()} calls atomically replace its last decoded snapshot.
     * The supplied node-to-type map is retained and read on every load.
     *
     * @param configurationFilePathFromSystem nonblank YAML file path
     * @param configurationTypeByYamlNodeNameToDecodeMap node names mapped to configuration classes
     * @param configurationService service updated and notified when the snapshot changes
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if the path is blank or another argument is null
     */
    public CoreBootstrapConfigurationLoader(
            final String configurationFilePathFromSystem,
            final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByYamlNodeNameToDecodeMap,
            final ConfigurationService configurationService)
    {
        AppbaseUtil.notBlank(configurationFilePathFromSystem, "configurationFilePathFromSystem");
        AppbaseUtil.notNull(configurationTypeByYamlNodeNameToDecodeMap, "configurationTypeByYamlNodeNameToDecodeMap");
        AppbaseUtil.notNull(configurationService, "configurationService");

        this.configurationFilePathFromSystem = configurationFilePathFromSystem;
        this.configurationTypeByYamlNodeNameToDecodeMap = configurationTypeByYamlNodeNameToDecodeMap;
        this.configurationService = configurationService;
        this.configurationByTypeNameMapRef = new AtomicReference<>(new ConcurrentHashMap<>());
    }

    /**
     * Load (and reload) bootstrap configuration file.
     *
     * @throws AppbaseException if there were any errors
     */
    @SuppressWarnings({"checkstyle:IllegalCatch", "unchecked"})
    public void load() throws AppbaseException {
        try {
            final Map<String, AppbaseConfiguration> newConfigurationByTypeNameMap = new ConcurrentHashMap<>();

            final Map<String, Object> configurationNodes =
                    YamlUtil.readConfigurationNodes(configurationFilePathFromSystem);
            if (configurationNodes != null) {
                configurationNodes.forEach((nodeName, configuration) -> {
                    final Class<AppbaseConfiguration> configurationType =
                            (Class<AppbaseConfiguration>)
                                    configurationTypeByYamlNodeNameToDecodeMap.get(nodeName);
                    if (configurationType != null) {
                        final AppbaseConfiguration appbaseConfiguration =
                                YamlUtil.convertYamlNode((Map<String, Object>) configuration, configurationType);
                        newConfigurationByTypeNameMap.put(configurationType.getName(), appbaseConfiguration);
                    }
                });
            }

            final long timestamp = System.currentTimeMillis();
            final Map<String, AppbaseConfiguration> currentConfigurationByTypeNameMap =
                    configurationByTypeNameMapRef.get();
            final MapDifference<String, AppbaseConfiguration> differences =
                    Maps.difference(currentConfigurationByTypeNameMap, newConfigurationByTypeNameMap);
            if (!differences.areEqual()) {
                final Map<String, AppbaseConfiguration> newConfigurations =
                        differences.entriesOnlyOnRight();
                if (newConfigurations != null && !newConfigurations.isEmpty()) {
                    for (final Map.Entry<String, AppbaseConfiguration> entry : newConfigurations.entrySet()) {
                        final String typeName = entry.getKey();
                        final AppbaseConfiguration appbaseConfiguration = entry.getValue();
                        final Class<AppbaseConfiguration> type =
                                (Class<AppbaseConfiguration>) Class.forName(typeName);
                        configurationService.putTypedConfiguration(type, appbaseConfiguration);

                        final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(type);
                        final ConfigurationValue<AppbaseConfiguration> configurationValue =
                                ConfigurationValue.fromValue(appbaseConfiguration);
                        final ConfigurationAddedEvent configurationAddedEvent =
                                new ConfigurationAddedEvent(configurationKey, configurationValue, timestamp);
                        configurationService.notifyConfigurationAddedEvent(configurationAddedEvent);
                    }
                }

                final Map<String, AppbaseConfiguration> removedConfigurations = differences.entriesOnlyOnLeft();
                if (removedConfigurations != null && !removedConfigurations.isEmpty()) {
                    for (final Map.Entry<String, AppbaseConfiguration> entry :
                            removedConfigurations.entrySet())
                    {
                        final String typeName = entry.getKey();
                        final Class<AppbaseConfiguration> type =
                                (Class<AppbaseConfiguration>) Class.forName(typeName);
                        configurationService.removeTypedConfiguration(type);

                        final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(type);
                        final ConfigurationRemovedEvent configurationRemovedEvent =
                                new ConfigurationRemovedEvent(configurationKey, timestamp);
                        configurationService.notifyConfigurationRemovedEvent(configurationRemovedEvent);
                    }
                }

                final Map<String, MapDifference.ValueDifference<AppbaseConfiguration>> changedConfigurations =
                        differences.entriesDiffering();
                if (changedConfigurations != null && !changedConfigurations.isEmpty()) {
                    for (final Map.Entry<String, MapDifference.ValueDifference<AppbaseConfiguration>> entry :
                            changedConfigurations.entrySet())
                    {
                        final String typeName = entry.getKey();
                        final Class<AppbaseConfiguration> type =
                                (Class<AppbaseConfiguration>) Class.forName(typeName);
                        final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(type);

                        final MapDifference.ValueDifference<AppbaseConfiguration> valueDifference =
                                entry.getValue();
                        final AppbaseConfiguration oldConfiguration = valueDifference.leftValue();
                        final ConfigurationValue<AppbaseConfiguration> oldConfigurationValue =
                                ConfigurationValue.fromValue(oldConfiguration);

                        final AppbaseConfiguration newConfiguration = valueDifference.rightValue();
                        final ConfigurationValue<AppbaseConfiguration> newConfigurationValue =
                                ConfigurationValue.fromValue(newConfiguration);
                        configurationService.removeTypedConfiguration(type);
                        configurationService.putTypedConfiguration(type, newConfiguration);

                        final ConfigurationChangedEvent configurationChangedEvent =
                                new ConfigurationChangedEvent(
                                        configurationKey, oldConfigurationValue, newConfigurationValue, timestamp);
                        configurationService.notifyConfigurationChangedEvent(configurationChangedEvent);
                    }
                }
            }

            configurationByTypeNameMapRef.set(newConfigurationByTypeNameMap);
        } catch (Exception ex) {
            throw new AppbaseException("Error reading YAML nodes", ex);
        }
    }

    /**
     * Returns a read-only view of the most recently decoded type-name map.
     *
     * @return an unmodifiable map that is replaced after each successful load
     */
    @Unmodifiable
    public Map<String, AppbaseConfiguration> getConfigurationByTypeNameMap() {
        return Collections.unmodifiableMap(configurationByTypeNameMapRef.get());
    }
}
