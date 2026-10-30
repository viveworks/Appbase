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

package com.viveworks.appbase.core.service.log.filter.event.listener.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LuceneLogAndTurboLogEventFilterListenerConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final LuceneLogAndTurboLogEventFilterListenerConfiguration
                luceneLogAndTurboLogEventFilterListenerConfiguration =
                LuceneLogAndTurboLogEventFilterListenerConfiguration
                .builder()
                .indexTurboLog(true)
                .indexLogEvent(true)
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LuceneLogAndTurboLogEventFilterListenerConfiguration.class)
                .writeValueAsString(luceneLogAndTurboLogEventFilterListenerConfiguration);

        final LuceneLogAndTurboLogEventFilterListenerConfiguration
                luceneLogAndTurboLogEventFilterListenerConfiguration1 = yamlMapper
                .readerFor(LuceneLogAndTurboLogEventFilterListenerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                luceneLogAndTurboLogEventFilterListenerConfiguration.isIndexLogEvent(),
                luceneLogAndTurboLogEventFilterListenerConfiguration1.isIndexLogEvent());
        Assertions.assertEquals(
                luceneLogAndTurboLogEventFilterListenerConfiguration.isEnabled(),
                luceneLogAndTurboLogEventFilterListenerConfiguration1.isEnabled());
        Assertions.assertEquals(
                luceneLogAndTurboLogEventFilterListenerConfiguration.isIndexTurboLog(),
                luceneLogAndTurboLogEventFilterListenerConfiguration1.isIndexTurboLog());
        Assertions.assertEquals(
                luceneLogAndTurboLogEventFilterListenerConfiguration,
                luceneLogAndTurboLogEventFilterListenerConfiguration1);
        Assertions.assertEquals(
                luceneLogAndTurboLogEventFilterListenerConfiguration.hashCode(),
                luceneLogAndTurboLogEventFilterListenerConfiguration1.hashCode());
        Assertions.assertEquals(
                luceneLogAndTurboLogEventFilterListenerConfiguration.toString(),
                luceneLogAndTurboLogEventFilterListenerConfiguration1.toString());
    }
}
