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

package com.viveworks.appbase.core.service.configuration.accessor;

import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseRegistry;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Represents a registry for {@link ConfigurationAccessor}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ConfigurationAccessorRegistry extends AppbaseRegistry {
    /**
     * Get {@link AppbaseCoreBootstrapConfiguration} type by <code>YAML</code> node name to be decode mappings.
     *
     * @return Configuration type by YAML node name to decode map
     */
    @Unmodifiable
    Map<String, Class<? extends AppbaseConfiguration>> getConfigurationTypeByYamlNodeNameToDecodeMap();


    /**
     * Register {@link AppbaseCoreBootstrapConfiguration} type by <code>YAML</code> node name to be decode mapping.
     *
     * @param configurationType {@link AppbaseCoreBootstrapConfiguration} type
     * @param yamlNodeNameToDecode <code>YAML</code> node name to decode
     */
    void registerConfigurationTypeByYamlNodeNameToDecode(
            Class<? extends AppbaseConfiguration> configurationType, String yamlNodeNameToDecode);


    /**
     * Unregister {@link AppbaseCoreBootstrapConfiguration} type by <code>YAML</code> node name to be decode.
     *
     * @param yamlNodeNameToDecode <code>YAML</code> node name to decode
     */
    void unregisterConfigurationTypeByYamlNodeNameToDecode(String yamlNodeNameToDecode);


    /**
     * Clear {@link AppbaseCoreBootstrapConfiguration} type by <code>YAML</code> node name to be decode mappings.
     */
    void clearConfigurationTypeByYamlNodeNameToDecodeMap();




    /**
     * Get {@link ConfigurationCodec} by {@link MimeType} mappings.
     *
     * @return Configuration codec by mime type mapping
     */
    @Nullable
    @Unmodifiable
    Map<MimeType, ConfigurationCodec> getConfigurationCodecByMimeTypeMap();


    /**
     * Add a {@link ConfigurationCodec} that can be retrieved by the {@link MimeType} that it supports.
     *
     * @param configurationCodec Configuration code
     * @param mimeType Mime type
     */
    void registerConfigurationCodecByMimeType(ConfigurationCodec configurationCodec, MimeType mimeType);


    /**
     * Unregister {@link ConfigurationCodec} by {@link MimeType}.
     *
     * @param mimeType Mime type
     */
    void unregisterConfigurationCodecByMimeType(MimeType mimeType);


    /**
     * Clear {@link ConfigurationCodec} by {@link MimeType} mappings.
     */
    void clearConfigurationCodecByMimeTypeMap();




    /**
     * Get configuration type by raw configuration name mappings.
     *
     * @return Configuration type by raw configuration name mappings
     */
    @Nullable
    @Unmodifiable
    Map<String, Class<? extends AppbaseConfiguration>> getConfigurationTypeByRawConfigurationNameMap();

    /**
     * Add the implementation of {@link AppbaseConfiguration} that corresponds to raw configuration
     * name ({@link RawConfiguration#getName()}).
     *
     * @param configurationType Configuration type
     * @param rawConfigurationNameMapping Raw configuration name
     */
    void registerConfigurationTypeByRawConfigurationName(
            Class<? extends AppbaseConfiguration> configurationType, String rawConfigurationNameMapping);


    /**
     * Unregister configuration type for a raw configuration name.
     *
     * @param rawConfigurationName Raw configuration name
     */
    void unregisterConfigurationTypeByRawConfigurationName(String rawConfigurationName);


    /**
     * Clear configuration type by raw configuration name mappings.
     */
    void clearConfigurationTypeByRawConfigurationNameMap();




    /**
     * Get {@link ConfigurationStore}s.
     *
     * @return Configuration stores by ID
     */
    @Nullable
    @Unmodifiable
    Map<String, ConfigurationStore> getConfigurationStoreByIdMap();

    /**
     * Add {@link ConfigurationStore} that the accessor uses to read / write configurations. Typically, the accessor
     * iterates over the stores in insertion order and thus merging configurations loaded.
     *
     * @param configurationStore Configuration store
     */
    void registerConfigurationStore(ConfigurationStore configurationStore);

    /**
     * Unregister {@link ConfigurationStore} by ID.
     *
     * @param configurationStoreId {@link ConfigurationStore} ID
     */
    void unregisterConfigurationStore(String configurationStoreId);


    /**
     * Unregister {@link ConfigurationStore} by type.
     *
     * @param configurationStoreType {@link ConfigurationStore} type
     */
    void unregisterConfigurationStore(Class<? extends ConfigurationStore> configurationStoreType);


    /**
     * Clear {@link ConfigurationStore}s.
     */
    void clearConfigurationStores();




    /**
     * Get {@link ConfigurationStore} IDs by {@link ConfigurationKey} mappings.
     *
     * @return Configuration stores by key mappings
     */
    @Nullable
    @Unmodifiable
    Map<ConfigurationKey, Set<String>> getWritingConfigurationStoreIdsByKeyMap();

    /**
     * Add {@link ConfigurationStore} implementation that should write a configuration with a particular
     * {@link ConfigurationKey}.
     *
     * @param configurationStore {@link ConfigurationStore}
     * @param configurationKey Configuration key
     */
    void registerWritingConfigurationStore(ConfigurationStore configurationStore, ConfigurationKey configurationKey);


    /**
     * Unregister {@link ConfigurationStore} type by {@link ConfigurationKey}.
     *
     * @param configurationStoreType {@link ConfigurationStore} type
     * @param configurationKey Configuration key
     */
    void unregisterWritingConfigurationStore(
            Class<? extends ConfigurationStore> configurationStoreType, ConfigurationKey configurationKey);


    /**
     * Unregister {@link ConfigurationStore} type by {@link ConfigurationKey}.
     *
     * @param configurationStoreId {@link ConfigurationStore} ID
     * @param configurationKey Configuration key
     */
    void unregisterWritingConfigurationStore(String configurationStoreId, ConfigurationKey configurationKey);


    /**
     * Clear writing configuration stores.
     */
    void clearWritingConfigurationStores();
}
