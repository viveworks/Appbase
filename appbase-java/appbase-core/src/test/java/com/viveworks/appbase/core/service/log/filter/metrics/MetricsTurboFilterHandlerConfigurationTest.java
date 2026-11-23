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

package com.viveworks.appbase.core.service.log.filter.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MetricsTurboFilterHandlerConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration =
                MetricsTurboFilterHandlerConfiguration
                .builder()
                .allowedLogFormats(Set.of("allowed {}"))
                .allowedLoggerNames(Set.of(getClass().getName()))
                .allowedThrowableTypeNames(Set.of(IOException.class.getName()))
                .blockedLogFormats(Set.of("blocked {}"))
                .blockedLoggerNames(Set.of(AppbaseCore.class.getName()))
                .blockedThrowableTypeNames(Set.of(NullPointerException.class.getName()))
                .measureLogFormats(true)
                .measureLoggerNames(true)
                .measureLogLevels(true)
                .measureThrowableTypeNames(true)
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(MetricsTurboFilterHandlerConfiguration.class)
                .writeValueAsString(metricsTurboFilterHandlerConfiguration);

        final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration1 = yamlMapper
                .readerFor(MetricsTurboFilterHandlerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.getAllowedFormattedMessages(),
                metricsTurboFilterHandlerConfiguration1.getAllowedFormattedMessages());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.getAllowedLoggerNames(),
                metricsTurboFilterHandlerConfiguration1.getAllowedLoggerNames());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.getAllowedThrowableTypeNames(),
                metricsTurboFilterHandlerConfiguration1.getAllowedThrowableTypeNames());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.getBlockedFormattedMessages(),
                metricsTurboFilterHandlerConfiguration1.getBlockedFormattedMessages());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.getBlockedLoggerNames(),
                metricsTurboFilterHandlerConfiguration1.getBlockedLoggerNames());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.getBlockedThrowableTypeNames(),
                metricsTurboFilterHandlerConfiguration1.getBlockedThrowableTypeNames());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.isEnabled(),
                metricsTurboFilterHandlerConfiguration1.isEnabled());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.isMeasureFormattedMessages(),
                metricsTurboFilterHandlerConfiguration1.isMeasureFormattedMessages());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.isMeasureLoggerNames(),
                metricsTurboFilterHandlerConfiguration1.isMeasureLoggerNames());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.isMeasureLogLevels(),
                metricsTurboFilterHandlerConfiguration1.isMeasureLogLevels());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.isMeasureThrowableTypeNames(),
                metricsTurboFilterHandlerConfiguration1.isMeasureThrowableTypeNames());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration,
                metricsTurboFilterHandlerConfiguration1);
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.hashCode(),
                metricsTurboFilterHandlerConfiguration1.hashCode());
        Assertions.assertEquals(
                metricsTurboFilterHandlerConfiguration.toString(),
                metricsTurboFilterHandlerConfiguration1.toString());
    }
}
