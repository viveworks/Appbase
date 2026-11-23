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

package com.viveworks.appbase.core.service.configuration;

import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MimeTypeTest {
    @Test
    void testYamlSerde() throws Exception {
        final MimeType mimeType1 = MimeType.YAML;
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(MimeType.class)
                .writeValueAsString(mimeType1);
        Assertions.assertNotNull(yaml);

        final MimeType mimeType2 = YamlUtil
                .getYamlMapper()
                .readerFor(MimeType.class)
                .readValue(yaml);
        Assertions.assertNotNull(mimeType2);

        Assertions.assertEquals(mimeType1.getValue(), mimeType2.getValue());
        Assertions.assertEquals(0, mimeType1.compareTo(mimeType2));
        Assertions.assertEquals(mimeType1, mimeType2);
        Assertions.assertEquals(mimeType1.hashCode(), mimeType2.hashCode());
        Assertions.assertEquals(mimeType1.toString(), mimeType2.toString());
    }
}
