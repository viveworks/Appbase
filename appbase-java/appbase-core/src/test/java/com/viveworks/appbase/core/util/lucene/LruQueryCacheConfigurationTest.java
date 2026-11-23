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

package com.viveworks.appbase.core.util.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LruQueryCacheConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final LruQueryCacheConfiguration lruQueryCacheConfiguration = LruQueryCacheConfiguration
                .builder()
                .maxQueries(100)
                .maxRamBytesUsed(2 * 1024L * 1024L)
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LruQueryCacheConfiguration.class)
                .writeValueAsString(lruQueryCacheConfiguration);

        final LruQueryCacheConfiguration lruQueryCacheConfiguration1 = yamlMapper
                .readerFor(LruQueryCacheConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                lruQueryCacheConfiguration.isEnabled(),
                lruQueryCacheConfiguration1.isEnabled());
        Assertions.assertEquals(
                lruQueryCacheConfiguration.getMaxQueries(),
                lruQueryCacheConfiguration1.getMaxQueries());
        Assertions.assertEquals(
                lruQueryCacheConfiguration.getMaxRamBytesUsed(),
                lruQueryCacheConfiguration1.getMaxRamBytesUsed());
        Assertions.assertEquals(
                lruQueryCacheConfiguration,
                lruQueryCacheConfiguration1);
        Assertions.assertEquals(
                lruQueryCacheConfiguration.hashCode(),
                lruQueryCacheConfiguration1.hashCode());
        Assertions.assertEquals(
                lruQueryCacheConfiguration.toString(),
                lruQueryCacheConfiguration1.toString());
    }
}
