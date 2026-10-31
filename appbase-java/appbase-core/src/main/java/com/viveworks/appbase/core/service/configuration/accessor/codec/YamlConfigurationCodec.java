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

package com.viveworks.appbase.core.service.configuration.accessor.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.eclipse.microprofile.metrics.Gauge;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encoder (serializer) / Decoder (de-serializer) for <code>YAML</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class YamlConfigurationCodec implements ConfigurationCodec {
    private Logger logger;
    private final ObjectMapper yamlMapper;
    private final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByYamlNodeNameToDecodeMap;
    private final Gauge configurationTypeByYamlNodeNameToDecodeMapSizeGauge;

    public YamlConfigurationCodec() {
        this(YamlUtil.getYamlMapper());
    }

    public YamlConfigurationCodec(final ObjectMapper yamlMapper) {
        AppbaseUtil.notNull(yamlMapper, "yamlMapper");

        this.yamlMapper = yamlMapper;
        this.configurationTypeByYamlNodeNameToDecodeMap = new ConcurrentHashMap<>();
        this.configurationTypeByYamlNodeNameToDecodeMapSizeGauge = configurationTypeByYamlNodeNameToDecodeMap::size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Configuration> decode(
            final Collection<RawConfiguration> rawConfigurations,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationCodecException
    {
        final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMapping =
                configurationAccessorContext.getConfigurationTypeByRawConfigurationNameMapping();
        final Collection<Configuration> configurations = new ArrayList<>();

        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            final String rawConfigurationName = rawConfiguration.getName();
            boolean decoded = false;

            // Try node names first
            try {
                final JsonNode jsonNode = yamlMapper.readTree(rawConfiguration.getValue());
                final Map<String, Object> configurationMap =
                        (Map<String, Object>) yamlMapper.treeToValue(jsonNode, Map.class);
                for (final Map.Entry<String, Object> entry : configurationMap.entrySet()) {
                    final String nodeName = entry.getKey();
                    final Object nodeValue = entry.getValue();

                    final Class<? extends AppbaseConfiguration> configurationType =
                            configurationTypeByYamlNodeNameToDecodeMap.get(nodeName);
                    if (configurationType != null) {
                        if (logger != null) {
                            logger.debug("Decoding YAML node {} as type {}", nodeName, configurationType);
                        }
                        final AppbaseConfiguration appbaseConfiguration =
                                yamlMapper.convertValue(nodeValue, configurationType);
                        final ConfigurationValue<AppbaseConfiguration> configurationValue =
                                ConfigurationValue.fromValue(appbaseConfiguration);
                        final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(configurationType);
                        final Configuration configuration = Configuration
                                .builder()
                                .rawConfiguration(rawConfiguration)
                                .configurationKey(configurationKey)
                                .configurationValue(configurationValue)
                                .build();
                        configurations.add(configuration);
                        decoded = true;
                    }
                }
            } catch (IOException ioe) {
                throw new ConfigurationCodecException(
                        "Error while decoding " + rawConfigurationName + " into YAML tree",
                        ioe);
            }

            // Try the whole object next
            final Class<? extends AppbaseConfiguration> type =
                    configurationTypeByRawConfigurationNameMapping.get(rawConfigurationName);
            if (type != null) {
                try {
                    if (logger != null) {
                        logger.debug("Decoding raw configuration {} as type {}", rawConfigurationName, type);
                    }
                    final Object value = yamlMapper
                            .readerFor(type)
                            .readValue(rawConfiguration.getValue());
                    final ConfigurationValue<Object> configurationValue = ConfigurationValue.fromValue(value);
                    final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(type);
                    final Configuration configuration = Configuration
                            .builder()
                            .rawConfiguration(rawConfiguration)
                            .configurationKey(configurationKey)
                            .configurationValue(configurationValue)
                            .build();
                    configurations.add(configuration);
                    decoded = true;
                } catch (IOException ioe) {
                    throw new ConfigurationCodecException(
                            "Error while decoding " + rawConfigurationName + " into YAML",
                            ioe);
                }
            }

            if (!decoded) {
                throw new ConfigurationCodecException("Raw configuration " +
                        rawConfigurationName +
                        " could not be decoded");
            }
        }

        return configurations;
    }

    @Override
    public Collection<RawConfiguration> encode(
            final Map<ConfigurationKey, ConfigurationValue<?>> configurationMap,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationCodecException
    {
        final long timestamp = System.currentTimeMillis();
        final Collection<RawConfiguration> rawConfigurations = new ArrayList<>();
        for (final Map.Entry<ConfigurationKey, ConfigurationValue<?>> entry : configurationMap.entrySet()) {
            final ConfigurationKey entryKey = entry.getKey();
            final ConfigurationValue<?> entryValue = entry.getValue();

            final Class<? extends AppbaseConfiguration> typeKey = entryKey.getTypeKey();
            if (typeKey == null) {
                throw new ConfigurationCodecException(
                        "Typed key cannot be null to encode to YAML, [configurationKey=" + entryKey + "]");
            }
            final Map<String, String> rawConfigurationNameByConfigurationTypeNameMapping =
                    configurationAccessorContext.getRawConfigurationNameByConfigurationTypeNameMapping();
            final String rawConfigurationName =
                    rawConfigurationNameByConfigurationTypeNameMapping.get(typeKey.getName());
            if (AppbaseUtil.isBlank(rawConfigurationName)) {
                throw new ConfigurationCodecException("Cannot find raw configuration name for type " + typeKey);
            }

            final Object value = entryValue.getValue();
            try {
                final byte[] bytes = yamlMapper
                        .writerFor(typeKey)
                        .writeValueAsBytes(value);
                final RawConfiguration rawConfiguration = RawConfiguration
                        .builder()
                        .mimeType(MimeType.YAML)
                        .name(rawConfigurationName)
                        .value(bytes)
                        .timestamp(timestamp)
                        .build();
                rawConfigurations.add(rawConfiguration);
            } catch (JsonProcessingException jpe) {
                throw new ConfigurationCodecException(
                        "Error while encoding " + typeKey + " to YAML", jpe);
            }
        }

        return rawConfigurations;
    }

    /**
     * Register configuration type by YAML node name used to decode.
     *
     * @param yamlNodeName YAML node name
     * @param configurationType Configuration type
     */
    public void registerConfigurationTypeByYamlNodeNameToDecode(
            final String yamlNodeName,
            final Class<? extends AppbaseConfiguration> configurationType)
    {
        AppbaseUtil.notNull(yamlNodeName, "yamlNodeName");
        AppbaseUtil.notNull(configurationType, "configurationType");

        if (logger != null) {
            logger.info("Registering configuration type {} for YAML node name {}",
                    configurationType.getName(), yamlNodeName);
        }
        configurationTypeByYamlNodeNameToDecodeMap.put(yamlNodeName, configurationType);
    }

    public Gauge getConfigurationTypeByYamlNodeNameToDecodeMapSizeGauge() {
        return configurationTypeByYamlNodeNameToDecodeMapSizeGauge;
    }


    /**
     * Set {@link LogService}.
     *
     * @param logService Set log service
     */
    public void setLogService(final LogService logService) {
        AppbaseUtil.notNull(logService, "logService");

        this.logger = logService.getLogger(getClass());
    }
}
