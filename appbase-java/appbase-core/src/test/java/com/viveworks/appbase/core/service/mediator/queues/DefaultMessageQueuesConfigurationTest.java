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

package com.viveworks.appbase.core.service.mediator.queues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMessageQueuesConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration = DefaultMessageQueuesConfiguration
                .builder()
                .boundMessageQueueCapacity(512)
                .messageQueueType(MessageQueueType.UNBOUND)
                .messageQueuesCleanerInitialDelayMillis(5)
                .messageQueuesCleanerIntervalMillis(100)
                .expiredMessageThresholdMillis(TimeUnit.MINUTES.toMillis(15))
                .removeExpiredMessages(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultMessageQueuesConfiguration.class)
                .writeValueAsString(defaultMessageQueuesConfiguration);
        final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration1 = yamlMapper
                .readerFor(DefaultMessageQueuesConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultMessageQueuesConfiguration.getBoundMessageQueueCapacity(),
                defaultMessageQueuesConfiguration1.getBoundMessageQueueCapacity());
        Assertions.assertEquals(
                defaultMessageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis(),
                defaultMessageQueuesConfiguration1.getMessageQueuesCleanerIntervalMillis());
        Assertions.assertEquals(
                defaultMessageQueuesConfiguration.getMessageQueueType(),
                defaultMessageQueuesConfiguration1.getMessageQueueType());
        Assertions.assertEquals(
                defaultMessageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                defaultMessageQueuesConfiguration1.getExpiredMessageThresholdMillis());
        Assertions.assertEquals(
                defaultMessageQueuesConfiguration.isRemoveExpiredMessages(),
                defaultMessageQueuesConfiguration1.isRemoveExpiredMessages());
        Assertions.assertEquals(
                defaultMessageQueuesConfiguration.hashCode(), defaultMessageQueuesConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultMessageQueuesConfiguration, defaultMessageQueuesConfiguration1);
        Assertions.assertEquals(
                defaultMessageQueuesConfiguration.toString(), defaultMessageQueuesConfiguration1.toString());
    }
}
