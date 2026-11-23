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

package com.viveworks.appbase.core.service.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessorConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultExceptionServiceConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                DefaultExceptionServiceConfiguration.builder().build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultExceptionServiceConfiguration.class)
                .writeValueAsString(defaultExceptionServiceConfiguration);
        final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration1 = yamlMapper
                .readerFor(DefaultExceptionServiceConfiguration.class)
                .readValue(yaml);

        final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration =
                defaultExceptionServiceConfiguration.getDefaultExceptionAccessorConfiguration();
        Assertions.assertNotNull(defaultExceptionAccessorConfiguration);
        Assertions.assertEquals(
                defaultExceptionServiceConfiguration.getDefaultExceptionAccessorConfiguration(),
                defaultExceptionServiceConfiguration1.getDefaultExceptionAccessorConfiguration());
        Assertions.assertEquals(
                defaultExceptionServiceConfiguration.getDefaultExpiredMessageCleanerConfiguration(),
                defaultExceptionServiceConfiguration1.getDefaultExpiredMessageCleanerConfiguration());
        Assertions.assertEquals(
                defaultExceptionServiceConfiguration,
                defaultExceptionServiceConfiguration1);
        Assertions.assertEquals(
                defaultExceptionServiceConfiguration.hashCode(),
                defaultExceptionServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultExceptionServiceConfiguration.toString(),
                defaultExceptionServiceConfiguration1.toString());
    }
}
