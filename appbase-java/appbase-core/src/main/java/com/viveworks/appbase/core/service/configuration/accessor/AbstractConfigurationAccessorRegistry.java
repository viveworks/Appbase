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

package com.viveworks.appbase.core.service.configuration.accessor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.microprofile.ConfigSourceAdapter;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of {@link ConfigurationAccessorRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractConfigurationAccessorRegistry implements ConfigurationAccessorRegistry {
    protected final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByYamlNodeNameToDecodeMap;
    protected final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMap;
    protected final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMap;
    protected final Map<String, ConfigurationStore> configurationStoreByIdMap;
    protected final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap;
    protected final Map<String, ConfigSourceAdapter> configSourceAdapterByIdMap;
    protected final AtomicBoolean shutdown;


    public AbstractConfigurationAccessorRegistry() {
        configurationTypeByYamlNodeNameToDecodeMap = new ConcurrentHashMap<>();
        configurationCodecByMimeTypeMap = new ConcurrentHashMap<>();
        configurationTypeByRawConfigurationNameMap = new ConcurrentHashMap<>();
        configurationStoreByIdMap = new ConcurrentHashMap<>();
        writingConfigurationStoreIdsByKeyMap = new ConcurrentHashMap<>();
        configSourceAdapterByIdMap = new ConcurrentHashMap<>();
        shutdown = new AtomicBoolean(false);
    }

    public AbstractConfigurationAccessorRegistry(
            final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMap,
            final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMap,
            final Collection<ConfigurationStore> configurationStores,
            final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap)
    {
        AppbaseUtil.notNull(configurationCodecByMimeTypeMap, "configurationCodecByMimeTypeMap");
        AppbaseUtil.notNull(configurationTypeByRawConfigurationNameMap, "configurationTypeByRawConfigurationNameMap");
        AppbaseUtil.notNull(configurationStores, "configurationStores");
        AppbaseUtil.notNull(writingConfigurationStoreIdsByKeyMap, "writingConfigurationStoreIdsByKeyMap");


        this.configurationTypeByYamlNodeNameToDecodeMap = new ConcurrentHashMap<>();
        this.configurationCodecByMimeTypeMap = new ConcurrentHashMap<>(configurationCodecByMimeTypeMap);


        this.configurationTypeByRawConfigurationNameMap =
                new ConcurrentHashMap<>(configurationTypeByRawConfigurationNameMap);

        this.configurationStoreByIdMap = new ConcurrentHashMap<>();
        configurationStores.forEach(configurationStore -> {
            final String id = configurationStore.getId();
            AppbaseUtil.notNull(id, "id in " + configurationStore.getClass().getName());

            configurationStoreByIdMap.put(id, configurationStore);
        });
        this.configSourceAdapterByIdMap = new ConcurrentHashMap<>();


        this.writingConfigurationStoreIdsByKeyMap = new ConcurrentHashMap<>(writingConfigurationStoreIdsByKeyMap);
        shutdown = new AtomicBoolean(false);
    }




    @Override
    public Map<String, Class<? extends AppbaseConfiguration>> getConfigurationTypeByYamlNodeNameToDecodeMap() {
        return Collections.unmodifiableMap(configurationTypeByYamlNodeNameToDecodeMap);
    }

    @Override
    public void registerConfigurationTypeByYamlNodeNameToDecode(
            final Class<? extends AppbaseConfiguration> configurationType,
            final String yamlNodeNameToDecode)
    {
        AppbaseUtil.notNull(configurationType, "configurationType");
        AppbaseUtil.notBlank(yamlNodeNameToDecode, "yamlNodeNameToDecode");

        configurationTypeByYamlNodeNameToDecodeMap.put(yamlNodeNameToDecode, configurationType);
    }

    @Override
    public void unregisterConfigurationTypeByYamlNodeNameToDecode(final String yamlNodeNameToDecode) {
        AppbaseUtil.notBlank(yamlNodeNameToDecode, "yamlNodeNameToDecode");

        configurationTypeByYamlNodeNameToDecodeMap.remove(yamlNodeNameToDecode);
    }

    @Override
    public void clearConfigurationTypeByYamlNodeNameToDecodeMap() {
        configurationTypeByYamlNodeNameToDecodeMap.clear();
    }




    @Nullable
    @Unmodifiable
    @Override
    public Map<MimeType, ConfigurationCodec> getConfigurationCodecByMimeTypeMap() {
        return Collections.unmodifiableMap(configurationCodecByMimeTypeMap);
    }

    @Override
    public void registerConfigurationCodecByMimeType(final ConfigurationCodec configurationCodec,
                                                     final MimeType mimeType)
    {
        AppbaseUtil.notNull(configurationCodec, "configurationCodec");
        AppbaseUtil.notNull(mimeType, "mimeType");

        configurationCodecByMimeTypeMap.put(mimeType, configurationCodec);
    }

    @Override
    public void unregisterConfigurationCodecByMimeType(final MimeType mimeType) {
        AppbaseUtil.notNull(mimeType, "mimeType");

        configurationCodecByMimeTypeMap.remove(mimeType);
    }

    @Override
    public void clearConfigurationCodecByMimeTypeMap() {
        configurationCodecByMimeTypeMap.clear();
    }




    @Nullable
    @Unmodifiable
    @Override
    public Map<String, Class<? extends AppbaseConfiguration>> getConfigurationTypeByRawConfigurationNameMap() {
        return Collections.unmodifiableMap(configurationTypeByRawConfigurationNameMap);
    }

    @Override
    public void registerConfigurationTypeByRawConfigurationName(
            final Class<? extends AppbaseConfiguration> configurationType,
            final String rawConfigurationNameMapping)
    {
        AppbaseUtil.notNull(configurationType, "configurationType");
        AppbaseUtil.notNull(rawConfigurationNameMapping, "rawConfigurationNameMapping");

        configurationTypeByRawConfigurationNameMap.put(rawConfigurationNameMapping, configurationType);
    }

    @Override
    public void unregisterConfigurationTypeByRawConfigurationName(final String rawConfigurationName) {
        AppbaseUtil.notNull(rawConfigurationName, "rawConfigurationName");

        configurationTypeByRawConfigurationNameMap.remove(rawConfigurationName);
    }

    @Override
    public void clearConfigurationTypeByRawConfigurationNameMap() {
        configurationTypeByRawConfigurationNameMap.clear();
    }




    @Nullable
    @Unmodifiable
    @Override
    public Map<String, ConfigurationStore> getConfigurationStoreByIdMap() {
        return Collections.unmodifiableMap(configurationStoreByIdMap);
    }

    @Override
    public void registerConfigurationStore(final ConfigurationStore configurationStore) {
        AppbaseUtil.notNull(configurationStore, "configurationStore");

        configurationStoreByIdMap.put(configurationStore.getId(), configurationStore);
    }

    @Override
    public void unregisterConfigurationStore(final String configurationStoreId) {
        AppbaseUtil.notNull(configurationStoreId, "configurationStoreId");

        configurationStoreByIdMap.remove(configurationStoreId);
    }

    @Override
    public void unregisterConfigurationStore(final Class<? extends ConfigurationStore> configurationStoreType) {
        AppbaseUtil.notNull(configurationStoreType, "configurationStoreType");

        configurationStoreByIdMap.forEach((configurationStoreId, configurationStore) -> {
            if (configurationStoreType.isAssignableFrom(configurationStore.getClass())) {
                configurationStoreByIdMap.remove(configurationStoreId);
            }
        });
    }

    @Override
    public void clearConfigurationStores() {
        configurationStoreByIdMap.clear();
    }




    @Nullable
    @Unmodifiable
    @Override
    public Map<ConfigurationKey, Set<String>> getWritingConfigurationStoreIdsByKeyMap() {
        return Collections.unmodifiableMap(writingConfigurationStoreIdsByKeyMap);
    }

    @Override
    public void registerWritingConfigurationStore(
            final ConfigurationStore configurationStore,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationStore, "configurationStore");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        registerConfigurationStore(configurationStore);
        writingConfigurationStoreIdsByKeyMap
                .computeIfAbsent(configurationKey, k -> new ConcurrentSkipListSet<>())
                .add(configurationStore.getId());
    }

    @Override
    public void unregisterWritingConfigurationStore(
            final String configurationStoreId,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationStoreId, "configurationStoreId");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        final Set<String> writingConfigurationStoreIds = writingConfigurationStoreIdsByKeyMap.get(configurationKey);
        if (writingConfigurationStoreIds != null) {
            writingConfigurationStoreIds.remove(configurationStoreId);

            if (writingConfigurationStoreIds.isEmpty()) {
                writingConfigurationStoreIdsByKeyMap.remove(configurationKey);
            }
        }
    }

    @Override
    public void unregisterWritingConfigurationStore(
            final Class<? extends ConfigurationStore> configurationStoreType,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationStoreType, "configurationStoreType");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        final Set<String> writingConfigurationStoreIds = writingConfigurationStoreIdsByKeyMap.get(configurationKey);
        if (writingConfigurationStoreIds != null) {
            writingConfigurationStoreIds.forEach(writingConfigurationStoreId -> {
                final ConfigurationStore configurationStore = configurationStoreByIdMap.get(writingConfigurationStoreId);
                if (configurationStore != null && configurationStoreType.isAssignableFrom(configurationStore.getClass())) {
                    writingConfigurationStoreIds.remove(writingConfigurationStoreId);
                }
            });

            if (writingConfigurationStoreIds.isEmpty()) {
                writingConfigurationStoreIdsByKeyMap.remove(configurationKey);
            }
        }
    }

    @Override
    public void clearWritingConfigurationStores() {
        writingConfigurationStoreIdsByKeyMap.clear();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        clearConfigurationTypeByYamlNodeNameToDecodeMap();
        clearConfigurationStores();
        clearConfigurationCodecByMimeTypeMap();
        clearConfigurationTypeByRawConfigurationNameMap();
        clearWritingConfigurationStores();
    }
}
