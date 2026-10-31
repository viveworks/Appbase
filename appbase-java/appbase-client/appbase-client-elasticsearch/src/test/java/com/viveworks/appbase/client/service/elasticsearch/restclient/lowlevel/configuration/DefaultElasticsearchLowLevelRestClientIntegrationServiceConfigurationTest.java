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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testBuilderDefaultsForNewFields() {
        // Arrange + Act
        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration config =
                DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.builder().build();

        // Assert — new fields default to null
        Assertions.assertNull(config.getNodeSelector());
        Assertions.assertNull(config.getCompressionEnabled());
    }

    @Test
    void testBuilderWithNodeSelectorSkipDedicatedMasters() {
        // Arrange + Act
        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration config =
                DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.builder()
                        .nodeSelector(NodeSelectorType.SKIP_DEDICATED_MASTERS)
                        .build();

        // Assert
        Assertions.assertEquals(NodeSelectorType.SKIP_DEDICATED_MASTERS, config.getNodeSelector());
        Assertions.assertEquals("SKIP_DEDICATED_MASTERS", config.getNodeSelector().getText());
        Assertions.assertNull(config.getCompressionEnabled());
    }

    @Test
    void testBuilderWithCompressionEnabled() {
        // Arrange + Act
        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration config =
                DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.builder()
                        .compressionEnabled(true)
                        .build();

        // Assert
        Assertions.assertEquals(Boolean.TRUE, config.getCompressionEnabled());
        Assertions.assertNull(config.getNodeSelector());
    }

    @Test
    void testYamlRoundTripWithNodeSelectorAndCompressionEnabled() throws Exception {
        // Arrange
        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration config =
                DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.builder()
                        .nodeSelector(NodeSelectorType.SKIP_DEDICATED_MASTERS)
                        .compressionEnabled(true)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertEquals(NodeSelectorType.SKIP_DEDICATED_MASTERS, deserialized.getNodeSelector());
        Assertions.assertEquals(Boolean.TRUE, deserialized.getCompressionEnabled());
    }

    @Test
    void toString_doesNotDiscloseDefaultHeaders() {
        // CFG-LEAK-04 (CWE-532, CWE-200): defaultHeaders is an open-ended map that may carry
        // credentials (e.g. an Authorization header). It must be redacted in toString(), matching
        // the pattern DruidConnectionConfiguration already applies to its own open-ended map fields.
        final String defaultHeaderSentinel = "sentinel-es-defaultheader-CFGLEAK04";
        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration config =
                DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.builder()
                        .defaultHeaders(Map.of("Authorization", defaultHeaderSentinel))
                        .build();

        final String result = config.toString();

        assertFalse(result.contains(defaultHeaderSentinel), "toString() must not contain defaultHeaders' content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getDefaultHeaders_stillReturnsTheRealValue() {
        final Map<String, String> defaultHeaders = Map.of("Authorization", "sentinel-es-defaultheader-CFGLEAK04");
        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration config =
                DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.builder()
                        .defaultHeaders(defaultHeaders)
                        .build();

        Assertions.assertEquals(defaultHeaders, config.getDefaultHeaders());
    }
}
