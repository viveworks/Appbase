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
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationRemovedEventTest {
    @Test
    void testYamlSerde() throws Exception {
        final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey("zzz");
        final ConfigurationRemovedEvent configurationRemovedEvent1 =
                new ConfigurationRemovedEvent(configurationKey, System.currentTimeMillis());

        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationRemovedEvent.class)
                .writeValueAsString(configurationRemovedEvent1);
        Assertions.assertNotNull(yaml);

        final ConfigurationRemovedEvent configurationRemovedEvent2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationRemovedEvent.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationRemovedEvent2);

        Assertions.assertEquals(
                configurationRemovedEvent1.getConfigurationKey(), configurationRemovedEvent2.getConfigurationKey());
        Assertions.assertEquals(configurationRemovedEvent1.getTimestamp(), configurationRemovedEvent2.getTimestamp());
        Assertions.assertEquals(configurationRemovedEvent1, configurationRemovedEvent2);
        Assertions.assertEquals(configurationRemovedEvent1.hashCode(), configurationRemovedEvent2.hashCode());
        Assertions.assertEquals(configurationRemovedEvent1.toString(), configurationRemovedEvent2.toString());
    }
}
