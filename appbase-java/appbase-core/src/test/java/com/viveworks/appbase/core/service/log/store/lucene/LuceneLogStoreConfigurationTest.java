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

package com.viveworks.appbase.core.service.log.store.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LuceneLogStoreConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final LuceneLogStoreConfiguration luceneLogStoreConfiguration = LuceneLogStoreConfiguration
                .builder()
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LuceneLogStoreConfiguration.class)
                .writeValueAsString(luceneLogStoreConfiguration);

        final LuceneLogStoreConfiguration luceneLogStoreConfiguration1 = yamlMapper
                .readerFor(LuceneLogStoreConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                luceneLogStoreConfiguration.isEnabled(),
                luceneLogStoreConfiguration1.isEnabled());
        Assertions.assertEquals(
                luceneLogStoreConfiguration.getLogStoreType(),
                luceneLogStoreConfiguration1.getLogStoreType());
        Assertions.assertEquals(
                LuceneLogStore.class,
                luceneLogStoreConfiguration.getLogStoreType());
        Assertions.assertEquals(
                LuceneLogStore.class,
                luceneLogStoreConfiguration1.getLogStoreType());
        Assertions.assertEquals(
                luceneLogStoreConfiguration,
                luceneLogStoreConfiguration1);
        Assertions.assertEquals(
                luceneLogStoreConfiguration.hashCode(),
                luceneLogStoreConfiguration1.hashCode());
        Assertions.assertEquals(
                luceneLogStoreConfiguration.toString(),
                luceneLogStoreConfiguration1.toString());
    }

    @Test
    void logStoreIsDisabledByDefault() {
        // R3 (internal work item 1073, spec §10): the Lucene log store does file I/O on the hot log path — off by default;
        // users who want indexed/searchable logs opt in explicitly.
        Assertions.assertFalse(
                LuceneLogStoreConfiguration.builder().build().isEnabled(),
                "the Lucene log store must default to disabled (R3 §10)");
    }
}
