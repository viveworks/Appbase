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

package com.viveworks.appbase.core.service.configuration.jmx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxConfigurationKeyTest {
    @Test
    void testSerde() throws Exception {
        final JmxConfigurationKey jmxConfigurationKey = new JmxConfigurationKey(
                "key1", DefaultConfigurationServiceConfiguration.class.getName());

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(JmxConfigurationKey.class)
                .writeValueAsString(jmxConfigurationKey);
        final JmxConfigurationKey jmxConfigurationKey1 = yamlMapper
                .readerFor(JmxConfigurationKey.class)
                .readValue(yaml);

        Assertions.assertEquals(
                jmxConfigurationKey.getPropertyKey(),
                jmxConfigurationKey1.getPropertyKey());
        Assertions.assertEquals(
                jmxConfigurationKey.getAppbaseServiceConfigurationTypeName(),
                jmxConfigurationKey1.getAppbaseServiceConfigurationTypeName());
        Assertions.assertEquals(
                jmxConfigurationKey,
                jmxConfigurationKey1);
        Assertions.assertEquals(
                jmxConfigurationKey.hashCode(),
                jmxConfigurationKey1.hashCode());
        Assertions.assertEquals(
                jmxConfigurationKey.toString(),
                jmxConfigurationKey1.toString());

        Assertions.assertThrows(AppbaseRuntimeException.class, () -> new JmxConfigurationKey(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> new JmxConfigurationKey("", null));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> new JmxConfigurationKey(null, ""));

        Assertions.assertDoesNotThrow(() -> new JmxConfigurationKey(
                "", DefaultMediatorServiceConfiguration.class.getName()));
        Assertions.assertDoesNotThrow(() -> new JmxConfigurationKey("key", ""));
    }
}
