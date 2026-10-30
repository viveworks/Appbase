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

package com.viveworks.appbase.core.service.log.appender.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LuceneAppenderConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final LuceneAppenderConfiguration luceneAppenderConfiguration = LuceneAppenderConfiguration
                .builder()
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LuceneAppenderConfiguration.class)
                .writeValueAsString(luceneAppenderConfiguration);

        final LuceneAppenderConfiguration luceneAppenderConfiguration1 = yamlMapper
                .readerFor(LuceneAppenderConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                luceneAppenderConfiguration.isEnabled(),
                luceneAppenderConfiguration1.isEnabled());
        Assertions.assertEquals(
                luceneAppenderConfiguration,
                luceneAppenderConfiguration1);
        Assertions.assertEquals(
                luceneAppenderConfiguration.hashCode(),
                luceneAppenderConfiguration1.hashCode());
        Assertions.assertEquals(
                luceneAppenderConfiguration.toString(),
                luceneAppenderConfiguration1.toString());
    }

    @Test
    void appenderIsDisabledByDefault() {
        // R3 (internal work item 1073, spec §10): the Lucene appender does file I/O on the hot log path — off by default;
        // users who want indexed/searchable logs opt in explicitly.
        Assertions.assertFalse(
                LuceneAppenderConfiguration.builder().build().isEnabled(),
                "the Lucene appender must default to disabled (R3 §10)");
    }
}
