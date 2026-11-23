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

package com.viveworks.appbase.client.cloud.aws.common.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.StringList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AwsSdkClientOverrideConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AwsSdkClientOverrideConfigurationTest {

    @Test
    void toString_doesNotDiscloseHeaders() {
        // CFG-LEAK-05 (CWE-532, CWE-200): headers is an open-ended map that may carry credentials
        // (e.g. an Authorization header). It must be redacted in toString(), matching the pattern
        // DruidConnectionConfiguration already applies to its own open-ended map fields.
        final String headerSentinel = "sentinel-aws-header-CFGLEAK05";
        final AwsSdkClientOverrideConfiguration config = AwsSdkClientOverrideConfiguration
                .builder()
                .headers(Map.of(
                        "Authorization",
                        StringList.builder().strings(List.of(headerSentinel)).build()))
                .build();

        final String result = config.toString();

        assertFalse(result.contains(headerSentinel), "toString() must not contain headers' content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getHeaders_stillReturnsTheRealValue() {
        final Map<String, StringList> headers = Map.of(
                "Authorization",
                StringList.builder().strings(List.of("sentinel-aws-header-CFGLEAK05")).build());
        final AwsSdkClientOverrideConfiguration config = AwsSdkClientOverrideConfiguration
                .builder()
                .headers(headers)
                .build();

        assertEquals(headers, config.getHeaders());
    }
}
