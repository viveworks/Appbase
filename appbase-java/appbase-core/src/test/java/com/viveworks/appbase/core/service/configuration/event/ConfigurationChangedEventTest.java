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
class ConfigurationChangedEventTest {
    @Test
    void testYamlSerde() throws Exception {
        final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey("zzz");
        final ConfigurationValue<String> configurationValue1 = ConfigurationValue.fromValue("localhost");
        final ConfigurationValue<String> configurationValue2 = ConfigurationValue.fromValue("prodhost");
        final ConfigurationChangedEvent configurationChangedEvent1 =
                new ConfigurationChangedEvent(
                        configurationKey,
                        configurationValue1,
                        configurationValue2,
                        System.currentTimeMillis());

        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationChangedEvent.class)
                .writeValueAsString(configurationChangedEvent1);
        Assertions.assertNotNull(yaml);

        final ConfigurationChangedEvent configurationChangedEvent2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationChangedEvent.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationChangedEvent2);


        Assertions.assertEquals(
                configurationChangedEvent1.getConfigurationKey(),
                configurationChangedEvent2.getConfigurationKey());
        Assertions.assertEquals(
                configurationChangedEvent1.getNewConfigurationValue(),
                configurationChangedEvent2.getNewConfigurationValue());
        Assertions.assertEquals(
                configurationChangedEvent1.getOldConfigurationValue(),
                configurationChangedEvent2.getOldConfigurationValue());
        Assertions.assertEquals(
                configurationChangedEvent1.getTimestamp(),
                configurationChangedEvent2.getTimestamp());
        Assertions.assertEquals(configurationChangedEvent1, configurationChangedEvent2);
        Assertions.assertEquals(configurationChangedEvent1.hashCode(), configurationChangedEvent2.hashCode());
        Assertions.assertEquals(configurationChangedEvent1.toString(), configurationChangedEvent2.toString());
    }
}
