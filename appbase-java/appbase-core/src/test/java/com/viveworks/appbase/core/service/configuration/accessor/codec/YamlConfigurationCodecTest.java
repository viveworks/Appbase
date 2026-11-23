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

package com.viveworks.appbase.core.service.configuration.accessor.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class YamlConfigurationCodecTest {
    private static ConfigurationAccessorContext configurationAccessorContext;
    private static LogService logService;
    private static MetricService metricService;
    private static YamlConfigurationCodec yamlConfigurationCodec;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);

        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMapping = new HashMap<>();
        configurationCodecByMimeTypeMapping.put(
                MimeType.YAML,
                new YamlConfigurationCodec());

        final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMapping =
                new HashMap<>();
        configurationTypeByRawConfigurationNameMapping.put(
                YamlConfigurationCodecTest.class.getName(), DefaultMediatorServiceConfiguration.class);

        configurationAccessorContext = new ConfigurationAccessorContext(
                configurationCodecByMimeTypeMapping,
                configurationTypeByRawConfigurationNameMapping,
                new ConcurrentHashMap<>());
        yamlConfigurationCodec = new YamlConfigurationCodec();
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testDecode() throws Exception {
        final String name = YamlConfigurationCodecTest.class.getName();
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration.builder().build();
        final byte[] value = YamlUtil.getYamlMapper()
                .writerFor(DefaultMediatorServiceConfiguration.class)
                .writeValueAsBytes(defaultMediatorServiceConfiguration);
        final long timestamp = System.currentTimeMillis();
        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name(name)
                .value(value)
                .mimeType(MimeType.YAML)
                .timestamp(timestamp)
                .build();

        final Collection<Configuration> configurations =
                yamlConfigurationCodec.decode(List.of(rawConfiguration), configurationAccessorContext);

        boolean found = false;
        for (final Configuration configuration : configurations) {
            final ConfigurationKey configurationKey = configuration.getConfigurationKey();
            if (configurationKey.isTyped()) {
                final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
                if (typeKey != null) {
                    final ConfigurationValue<?> configurationValue = configuration.getConfigurationValue();
                    final DefaultMediatorServiceConfiguration value1 =
                            (DefaultMediatorServiceConfiguration) configurationValue.getValue();
                    Assertions.assertEquals(defaultMediatorServiceConfiguration, value1);
                    found = true;
                    break;
                }
            }
        }
        Assertions.assertTrue(found);

        // Test node name
        yamlConfigurationCodec.registerConfigurationTypeByYamlNodeNameToDecode(
                "defaultMediatorServiceConfiguration", DefaultMediatorServiceConfiguration.class);
        final SampleYamlConfiguration sampleYamlConfiguration =
                new SampleYamlConfiguration(defaultMediatorServiceConfiguration);
        final byte[] value1 = YamlUtil
                .getYamlMapper()
                .writerFor(SampleYamlConfiguration.class)
                .writeValueAsBytes(sampleYamlConfiguration);
        final RawConfiguration rawConfiguration1 = RawConfiguration
                .builder()
                .name(name)
                .value(value1)
                .mimeType(MimeType.YAML)
                .timestamp(timestamp)
                .build();
        final Collection<Configuration> configurations1 =
                yamlConfigurationCodec.decode(List.of(rawConfiguration1), configurationAccessorContext);

        boolean found1 = false;
        for (final Configuration configuration : configurations1) {
            final ConfigurationKey configurationKey = configuration.getConfigurationKey();
            if (configurationKey.isTyped()) {
                final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
                if (typeKey != null) {
                    final ConfigurationValue<?> configurationValue = configuration.getConfigurationValue();
                    final DefaultMediatorServiceConfiguration mediatorServiceConfiguration =
                            (DefaultMediatorServiceConfiguration) configurationValue.getValue();
                    Assertions.assertEquals(defaultMediatorServiceConfiguration, mediatorServiceConfiguration);
                    found1 = true;
                    break;
                }
            }
        }
        Assertions.assertTrue(found1);

        Assertions.assertNotNull(yamlConfigurationCodec.getConfigurationTypeByYamlNodeNameToDecodeMapSizeGauge());
    }

    @Test
    void testEncode() throws Exception {
        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(DefaultMediatorServiceConfiguration.class);

        final String name = YamlConfigurationCodecTest.class.getName();
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration.builder().build();
        final ConfigurationValue<DefaultMediatorServiceConfiguration> configurationValue =
                ConfigurationValue.fromValue(defaultMediatorServiceConfiguration);

        final Map<ConfigurationKey, ConfigurationValue<?>> configurationMap = new HashMap<>();
        configurationMap.put(configurationKey, configurationValue);

        final YamlConfigurationCodec newYamlConfigurationCodec = new YamlConfigurationCodec();
        final Collection<RawConfiguration> rawConfigurations =
                newYamlConfigurationCodec.encode(configurationMap, configurationAccessorContext);

        boolean found = false;
        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            final MimeType mimeType = rawConfiguration.getMimeType();
            if (mimeType.equals(MimeType.YAML)) {
                final byte[] rawConfigurationValue = rawConfiguration.getValue();
                final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration1 = YamlUtil
                        .getYamlMapper()
                        .readerFor(DefaultMediatorServiceConfiguration.class)
                        .readValue(rawConfigurationValue);
                Assertions.assertNotNull(defaultMediatorServiceConfiguration);
                Assertions.assertEquals(defaultMediatorServiceConfiguration, defaultMediatorServiceConfiguration1);
                found = true;
            }
        }

        Assertions.assertTrue(found);
    }

    static class SampleYamlConfiguration implements AppbaseConfiguration {
        private final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration;

        @JsonCreator
        SampleYamlConfiguration(
                @JsonProperty("defaultMediatorServiceConfiguration")
                final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration)
        {
            this.defaultMediatorServiceConfiguration = defaultMediatorServiceConfiguration;
        }

        public DefaultMediatorServiceConfiguration getDefaultMediatorServiceConfiguration() {
            return defaultMediatorServiceConfiguration;
        }
    }
}
