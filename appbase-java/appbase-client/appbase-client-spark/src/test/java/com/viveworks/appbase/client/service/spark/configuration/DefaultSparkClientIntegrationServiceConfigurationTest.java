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

package com.viveworks.appbase.client.service.spark.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultSparkClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultSparkClientIntegrationServiceConfigurationTest {

    @Test
    void toString_doesNotDiscloseExecutorEnvironmentVariablesOrSessionProperties() {
        // CFG-LEAK-03 (CWE-532, CWE-200): executorEnvironmentVariables and sessionProperties are
        // open-ended maps that may carry credentials (e.g. executor env secrets, Spark session
        // auth properties). Both must be redacted in toString(), matching the pattern
        // DruidConnectionConfiguration already applies to its own open-ended map fields.
        final String executorEnvSentinel = "sentinel-spark-executorenv-CFGLEAK03";
        final String sessionPropertySentinel = "sentinel-spark-sessionprop-CFGLEAK03";
        final DefaultSparkClientIntegrationServiceConfiguration config =
                DefaultSparkClientIntegrationServiceConfiguration.builder()
                        .executorEnvironmentVariables(Map.of("arbitrary-env-var", executorEnvSentinel))
                        .sessionProperties(Map.of("arbitrary-session-property", sessionPropertySentinel))
                        .build();

        final String result = config.toString();

        assertFalse(result.contains(executorEnvSentinel),
                "toString() must not contain executorEnvironmentVariables' content");
        assertFalse(result.contains(sessionPropertySentinel),
                "toString() must not contain sessionProperties' content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void getExecutorEnvironmentVariablesAndGetSessionProperties_stillReturnTheRealValues() {
        final Map<String, String> executorEnvironmentVariables =
                Map.of("arbitrary-env-var", "sentinel-spark-executorenv-CFGLEAK03");
        final Map<String, String> sessionProperties =
                Map.of("arbitrary-session-property", "sentinel-spark-sessionprop-CFGLEAK03");
        final DefaultSparkClientIntegrationServiceConfiguration config =
                DefaultSparkClientIntegrationServiceConfiguration.builder()
                        .executorEnvironmentVariables(executorEnvironmentVariables)
                        .sessionProperties(sessionProperties)
                        .build();

        Assertions.assertEquals(executorEnvironmentVariables, config.getExecutorEnvironmentVariables());
        Assertions.assertEquals(sessionProperties, config.getSessionProperties());
    }
}
