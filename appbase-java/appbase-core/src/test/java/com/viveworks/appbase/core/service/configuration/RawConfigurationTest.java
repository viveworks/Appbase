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

import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationEntity;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class RawConfigurationTest {
    @Test
    void testYamlSerde() throws IOException {
        final RawConfiguration rawConfiguration1 = RawConfiguration
                .builder()
                .name("name")
                .value("{'key': 'value'}".getBytes())
                .mimeType(MimeType.JSON)
                .context(Map.of(
                        ConfigurationEntity.ID_RAW_CONFIGURATION_CONTEXT_KEY,
                        "1",
                        ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY,
                        "GLOBAL",
                        ConfigurationEntity.KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY,
                        DefaultConfigurationServiceConfiguration.class.getName(),
                        ConfigurationEntity.LAST_UPDATED_TIMESTAMP_RAW_CONFIGURATION_CONTEXT_KEY,
                        Date.from(Instant.now()).toString()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(RawConfiguration.class)
                .writeValueAsString(rawConfiguration1);
        Assertions.assertNotNull(yaml);

        final RawConfiguration rawConfiguration2 = YamlUtil
                .getYamlMapper()
                .readerFor(RawConfiguration.class)
                .readValue(yaml);
        Assertions.assertNotNull(rawConfiguration2);

        Assertions.assertEquals(rawConfiguration1.getName(), rawConfiguration2.getName());
        Assertions.assertEquals(rawConfiguration1.getMimeType(), rawConfiguration2.getMimeType());
        Assertions.assertArrayEquals(rawConfiguration1.getValue(), rawConfiguration2.getValue());
        Assertions.assertEquals(rawConfiguration1.getContext(), rawConfiguration2.getContext());
        Assertions.assertEquals(rawConfiguration1.getTimestamp(), rawConfiguration2.getTimestamp());
        Assertions.assertEquals(rawConfiguration1, rawConfiguration2);
        Assertions.assertEquals(rawConfiguration1.hashCode(), rawConfiguration2.hashCode());
        Assertions.assertEquals(rawConfiguration1.toString(), rawConfiguration2.toString());
    }

    @Test
    void toStringDoesNotDiscloseValueOrContext() {
        // CFG-LEAK-06 (CWE-532, CWE-200): value (the raw configuration payload bytes) and context
        // (an open-ended map) may both carry credentials. Both must be redacted in toString(),
        // matching the pattern DruidConnectionConfiguration already applies to its own
        // credential-bearing/open-ended fields.
        final String valueSentinel = "sentinel-rawconfig-value-CFGLEAK06";
        final String contextSentinel = "sentinel-rawconfig-context-CFGLEAK06";
        final byte[] value = valueSentinel.getBytes();
        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name("name")
                .value(value)
                .mimeType(MimeType.JSON)
                .context(Map.of("arbitrary-context-key", contextSentinel))
                .timestamp(System.currentTimeMillis())
                .build();

        final String result = rawConfiguration.toString();

        // value is a byte[]; Arrays.toString(value) renders the numeric byte representation, not the
        // literal sentinel text, so assert against that rendering to actually exercise the leak.
        Assertions.assertFalse(
                result.contains(Arrays.toString(value)),
                "toString() must not contain value's numeric byte-array rendering");
        Assertions.assertFalse(result.contains(contextSentinel), "toString() must not contain context's content");
        Assertions.assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void getValueAndGetContextStillReturnTheRealValues() {
        final byte[] value = "sentinel-rawconfig-value-CFGLEAK06".getBytes();
        final Map<String, String> context = Map.of("arbitrary-context-key", "sentinel-rawconfig-context-CFGLEAK06");
        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name("name")
                .value(value)
                .mimeType(MimeType.JSON)
                .context(context)
                .timestamp(System.currentTimeMillis())
                .build();

        Assertions.assertArrayEquals(value, rawConfiguration.getValue());
        Assertions.assertEquals(context, rawConfiguration.getContext());
    }
}
