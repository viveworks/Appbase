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

package com.viveworks.appbase.core.service.metric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMetricServiceConfigurationTest {
    @Test
    void testMetricServiceConfiguration() throws Exception {
        final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration =
                DefaultMetricServiceConfiguration
                .builder()
                .enableJvmAndSystemMetrics(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultMetricServiceConfiguration.class)
                .writeValueAsString(defaultMetricServiceConfiguration);
        final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration1 = yamlMapper
                .readerFor(DefaultMetricServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultMetricServiceConfiguration.isEnableJvmAndSystemMetrics(),
                defaultMetricServiceConfiguration1.isEnableJvmAndSystemMetrics());
        Assertions.assertEquals(
                defaultMetricServiceConfiguration.getJmxMeterRegistryConfiguration(),
                defaultMetricServiceConfiguration1.getJmxMeterRegistryConfiguration());
        Assertions.assertEquals(
                defaultMetricServiceConfiguration.hashCode(),
                defaultMetricServiceConfiguration1.hashCode());
        Assertions.assertEquals(defaultMetricServiceConfiguration, defaultMetricServiceConfiguration1);
        Assertions.assertEquals(
                defaultMetricServiceConfiguration.toString(),
                defaultMetricServiceConfiguration1.toString());
    }
}
