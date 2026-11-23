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

package com.viveworks.appbase.core.service.configuration.event;

import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationAddedEventTest {
    @Test
    void testYamlSerde() throws Exception {
        final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey("zzz");
        final ConfigurationValue<String> configurationValue = ConfigurationValue.fromValue("localhost");
        final ConfigurationAddedEvent configurationAddedEvent1 =
                new ConfigurationAddedEvent(configurationKey, configurationValue, System.currentTimeMillis());

        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationAddedEvent.class)
                .writeValueAsString(configurationAddedEvent1);
        Assertions.assertNotNull(yaml);

        final ConfigurationAddedEvent configurationAddedEvent2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationAddedEvent.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationAddedEvent2);

        Assertions.assertEquals(
                configurationAddedEvent1.getConfigurationKey(), configurationAddedEvent2.getConfigurationKey());
        Assertions.assertEquals(
                configurationAddedEvent1.getConfigurationValue(), configurationAddedEvent2.getConfigurationValue());
        Assertions.assertEquals(configurationAddedEvent1.getTimestamp(), configurationAddedEvent2.getTimestamp());
        Assertions.assertEquals(configurationAddedEvent1, configurationAddedEvent2);
        Assertions.assertEquals(configurationAddedEvent1.hashCode(), configurationAddedEvent2.hashCode());
        Assertions.assertEquals(configurationAddedEvent1.toString(), configurationAddedEvent2.toString());
    }
}
