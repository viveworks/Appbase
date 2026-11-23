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

package com.viveworks.appbase.core.service.log.store.criteria.timestamp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ExactTimestampCriteriaTest {
    @Test
    void testYamlSerde() throws Exception {
        final ExactTimestampCriteria exactTimestampCriteria = new ExactTimestampCriteria(System.currentTimeMillis());

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ExactTimestampCriteria.class)
                .writeValueAsString(exactTimestampCriteria);

        final ExactTimestampCriteria exactTimestampCriteria1 = yamlMapper
                .readerFor(ExactTimestampCriteria.class)
                .readValue(yaml);

        Assertions.assertEquals(
                exactTimestampCriteria.getTimestamp(),
                exactTimestampCriteria1.getTimestamp());
        Assertions.assertEquals(
                exactTimestampCriteria,
                exactTimestampCriteria1);
        Assertions.assertEquals(
                exactTimestampCriteria.hashCode(),
                exactTimestampCriteria1.hashCode());
        Assertions.assertEquals(
                exactTimestampCriteria.toString(),
                exactTimestampCriteria1.toString());
    }
}
