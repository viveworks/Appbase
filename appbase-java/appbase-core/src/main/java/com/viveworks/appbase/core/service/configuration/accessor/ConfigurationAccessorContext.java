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

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a configuration context that is typically used by {@link ConfigurationAccessor} to pass configuration /
 * metadata to {@link ConfigurationCodec} and
 * {@link com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore} while reading and writing
 * configurations.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class ConfigurationAccessorContext {
    private final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMapping;
    private final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMapping;
    private final Map<String, String> rawConfigurationNameByConfigurationTypeNameMapping;
    private final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMapping;

    public ConfigurationAccessorContext(
            final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMapping,
            final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMapping,
            final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMapping)
    {
        AppbaseUtil.notNull(configurationCodecByMimeTypeMapping, "configurationCodecByMimeTypeMapping");
        AppbaseUtil.notNull(
                configurationTypeByRawConfigurationNameMapping, "configurationTypeByRawConfigurationNameMapping");
        AppbaseUtil.notNull(writingConfigurationStoreIdsByKeyMapping, "writingConfigurationStoreIdsByKeyMapping");

        this.configurationCodecByMimeTypeMapping = configurationCodecByMimeTypeMapping;
        this.configurationTypeByRawConfigurationNameMapping = configurationTypeByRawConfigurationNameMapping;
        this.rawConfigurationNameByConfigurationTypeNameMapping = new ConcurrentHashMap<>();
        this.writingConfigurationStoreIdsByKeyMapping = writingConfigurationStoreIdsByKeyMapping;
    }

    public Map<MimeType, ConfigurationCodec> getConfigurationCodecByMimeTypeMapping() {
        return Collections.unmodifiableMap(configurationCodecByMimeTypeMapping);
    }

    private void copyConfigurationTypeByRawConfigurationNameMapping() {
        configurationTypeByRawConfigurationNameMapping.forEach((rawConfigurationName, configurationType) ->
                rawConfigurationNameByConfigurationTypeNameMapping.put(
                        configurationType.getName(), rawConfigurationName));
    }

    /**
     * Lookup a {@link ConfigurationCodec} configured for a {@link MimeType}.
     *
     * @param mimeType MIME type to lookup by
     * @return ConfigurationCodec
     */
    @Nullable
    public ConfigurationCodec findConfigurationCodec(final MimeType mimeType) {
        AppbaseUtil.notNull(mimeType, "mimeType");

        return configurationCodecByMimeTypeMapping.get(mimeType);
    }

    public Map<String, Class<? extends AppbaseConfiguration>>
    getConfigurationTypeByRawConfigurationNameMapping()
    {
        return Collections.unmodifiableMap(configurationTypeByRawConfigurationNameMapping);
    }

    /**
     * Get configuration type name by raw configuration name mapping.
     *
     * @return Configuration type name by raw configuration name mapping
     */
    public Map<String, String> getRawConfigurationNameByConfigurationTypeNameMapping() {
        if (configurationTypeByRawConfigurationNameMapping.size() !=
                rawConfigurationNameByConfigurationTypeNameMapping.size())
        {
            copyConfigurationTypeByRawConfigurationNameMapping();
        }

        return rawConfigurationNameByConfigurationTypeNameMapping;
    }

    /**
     * Lookup the implementation of a {@link AppbaseConfiguration} configured for
     * {@link RawConfiguration#getName()}.
     *
     * @param rawConfigurationName Name as in {@link RawConfiguration#getName()}
     * @return The implementation configured
     */
    @Nullable
    public Class<? extends AppbaseConfiguration> findConfigurationType(final String rawConfigurationName) {
        AppbaseUtil.notNull(rawConfigurationName, "rawConfigurationName");

        return configurationTypeByRawConfigurationNameMapping.get(rawConfigurationName);
    }

    public Map<ConfigurationKey, Set<String>> getWritingConfigurationStoreIdsByKeyMapping() {
        return writingConfigurationStoreIdsByKeyMapping;
    }

    /**
     * Lookup the {@link ConfigurationStore}s configured to write a configuration by {@link ConfigurationKey}.
     *
     * @param configurationKey Configuration key
     * @return Configuration store types
     */
    @Nullable
    public Set<String> findWritingConfigurationStoreIds(final ConfigurationKey configurationKey) {
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        final Set<String> configurationStoreIds = writingConfigurationStoreIdsByKeyMapping.get(configurationKey);
        return (configurationStoreIds != null) ? Collections.unmodifiableSet(configurationStoreIds) : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConfigurationAccessorContext that = (ConfigurationAccessorContext) o;
        return Objects.equals(configurationCodecByMimeTypeMapping, that.configurationCodecByMimeTypeMapping) &&
                Objects.equals(
                        configurationTypeByRawConfigurationNameMapping,
                        that.configurationTypeByRawConfigurationNameMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurationCodecByMimeTypeMapping, configurationTypeByRawConfigurationNameMapping);
    }

    @Override
    public String toString() {
        return "ConfigurationAccessorContext{" +
                "configurationCodecByMimeTypeMapping=" + configurationCodecByMimeTypeMapping +
                ", configurationTypeByRawConfigurationNameMapping=" + configurationTypeByRawConfigurationNameMapping +
                '}';
    }
}
