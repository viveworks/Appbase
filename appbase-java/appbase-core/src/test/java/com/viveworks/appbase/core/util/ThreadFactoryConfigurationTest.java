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

package com.viveworks.appbase.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ThreadFactoryConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final ThreadFactoryConfiguration threadFactoryConfiguration = ThreadFactoryConfiguration
                .builder()
                .daemon(false)
                .nameFormat("new-test-thread-%")
                .enableThreadAffinity(false)
                .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ThreadFactoryConfiguration.class)
                .writeValueAsString(threadFactoryConfiguration);
        final ThreadFactoryConfiguration threadFactoryConfiguration1 = yamlMapper
                .readerFor(ThreadFactoryConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                threadFactoryConfiguration.isDaemon(), threadFactoryConfiguration1.isDaemon());
        Assertions.assertEquals(
                threadFactoryConfiguration.getNameFormat(), threadFactoryConfiguration1.getNameFormat());
        Assertions.assertEquals(
                threadFactoryConfiguration.isEnableThreadAffinity(),
                threadFactoryConfiguration1.isEnableThreadAffinity());
        Assertions.assertEquals(threadFactoryConfiguration, threadFactoryConfiguration1);
        Assertions.assertEquals(threadFactoryConfiguration.hashCode(), threadFactoryConfiguration1.hashCode());
        Assertions.assertEquals(threadFactoryConfiguration.toString(), threadFactoryConfiguration1.toString());
    }
}
