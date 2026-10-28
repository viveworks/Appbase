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

package com.viveworks.appbase.core.service.mediator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMediatorServiceConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                CoreTestFixtures.getMessageQueuesConfiguration();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultMediatorServiceConfiguration.class)
                .writeValueAsString(defaultMediatorServiceConfiguration);
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultMediatorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultMediatorServiceConfiguration.getAppbaseRuntimeExceptionQueueCapacity(),
                defaultMediatorServiceConfiguration1.getAppbaseRuntimeExceptionQueueCapacity());
        Assertions.assertEquals(
                defaultMediatorServiceConfiguration.getRemovedMessageQueueCapacity(),
                defaultMediatorServiceConfiguration1.getRemovedMessageQueueCapacity());
        Assertions.assertEquals(
                defaultMediatorServiceConfiguration.getIgnoredMessageTypeSet(),
                defaultMediatorServiceConfiguration1.getIgnoredMessageTypeSet());
        Assertions.assertEquals(
                defaultMediatorServiceConfiguration.getDefaultMessageQueuesConfiguration(),
                defaultMediatorServiceConfiguration1.getDefaultMessageQueuesConfiguration());
        Assertions.assertEquals(
                defaultMediatorServiceConfiguration.getMessageObjectPoolCapacity(),
                defaultMediatorServiceConfiguration1.getMessageObjectPoolCapacity());
        Assertions.assertEquals(defaultMediatorServiceConfiguration, defaultMediatorServiceConfiguration1);
        Assertions.assertEquals(
                defaultMediatorServiceConfiguration.hashCode(), defaultMediatorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultMediatorServiceConfiguration.toString(), defaultMediatorServiceConfiguration1.toString());
    }
}
