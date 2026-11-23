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

package com.viveworks.appbase.core.service.exception.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultExpiredMessageCleanerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration =
                DefaultExpiredMessageCleanerConfiguration
                .builder()
                .exclusiveRemoveBeforeTimePeriodUnit(TimeUnit.SECONDS)
                .exclusiveRemoveBeforeTimePeriod(10L)
                .schedulableConfiguration(SchedulableConfiguration.builder().build())
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultExpiredMessageCleanerConfiguration.class)
                .writeValueAsString(defaultExpiredMessageCleanerConfiguration);
        final DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration1 = yamlMapper
                .readerFor(DefaultExpiredMessageCleanerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(defaultExpiredMessageCleanerConfiguration, defaultExpiredMessageCleanerConfiguration1);
        Assertions.assertEquals(
                defaultExpiredMessageCleanerConfiguration.isEnabled(),
                defaultExpiredMessageCleanerConfiguration1.isEnabled());
        Assertions.assertEquals(
                defaultExpiredMessageCleanerConfiguration.getExclusiveRemoveBeforeTimePeriod(),
                defaultExpiredMessageCleanerConfiguration1.getExclusiveRemoveBeforeTimePeriod());
        Assertions.assertEquals(
                defaultExpiredMessageCleanerConfiguration.getExclusiveRemoveBeforeTimePeriodUnit(),
                defaultExpiredMessageCleanerConfiguration1.getExclusiveRemoveBeforeTimePeriodUnit());
        Assertions.assertEquals(
                defaultExpiredMessageCleanerConfiguration.getSchedulableConfiguration(),
                defaultExpiredMessageCleanerConfiguration1.getSchedulableConfiguration());
        Assertions.assertEquals(
                defaultExpiredMessageCleanerConfiguration.hashCode(),
                defaultExpiredMessageCleanerConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultExpiredMessageCleanerConfiguration.toString(),
                defaultExpiredMessageCleanerConfiguration1.toString());
    }
}
