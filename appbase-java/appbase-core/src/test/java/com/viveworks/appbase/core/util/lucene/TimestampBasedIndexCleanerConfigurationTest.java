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

package com.viveworks.appbase.core.util.lucene;

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
class TimestampBasedIndexCleanerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration =
                TimestampBasedIndexCleanerConfiguration
                .builder()
                .schedulableConfiguration(SchedulableConfiguration.builder().build())
                .creationTimestampFieldName("creationTimestamp")
                .removeBeforeTimePeriod(1)
                .removeBeforeTimePeriodUnit(TimeUnit.HOURS)
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(TimestampBasedIndexCleanerConfiguration.class)
                .writeValueAsString(timestampBasedIndexCleanerConfiguration);

        final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration1 = yamlMapper
                .readerFor(TimestampBasedIndexCleanerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration.getCreationTimestampFieldName(),
                timestampBasedIndexCleanerConfiguration1.getCreationTimestampFieldName());
        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration.getRemoveBeforeTimePeriod(),
                timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriod());
        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration.getRemoveBeforeTimePeriodUnit(),
                timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriodUnit());
        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration.getSchedulableConfiguration(),
                timestampBasedIndexCleanerConfiguration1.getSchedulableConfiguration());
        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration.isEnabled(),
                timestampBasedIndexCleanerConfiguration1.isEnabled());
        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration,
                timestampBasedIndexCleanerConfiguration1);
        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration.hashCode(),
                timestampBasedIndexCleanerConfiguration1.hashCode());
        Assertions.assertEquals(
                timestampBasedIndexCleanerConfiguration.toString(),
                timestampBasedIndexCleanerConfiguration1.toString());
    }
}
