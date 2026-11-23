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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultElasticsearchLowLevelRestClientServiceConfiguration} reactive
 * operation configuration field: builder defaults and YAML round-trip.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultElasticsearchLowLevelRestClientServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void reactiveConfigDefaultsWhenOmitted() {
        final ElasticsearchLowLevelRestClientServiceConfiguration c =
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.builder().build();

        assertNotNull(c.getReactiveOperationConfiguration());
        assertTrue(c.getReactiveOperationConfiguration().isMetricsEnabled());
    }

    @Test
    void yamlRoundTrip_withOverriddenReactiveOperationConfiguration() throws Exception {
        // Arrange
        final DefaultElasticsearchLowLevelRestClientServiceConfiguration original =
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.builder()
                        .reactiveOperationConfiguration(
                                DefaultReactiveOperationConfiguration.builder()
                                        .metricsEnabled(false)
                                        .build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultElasticsearchLowLevelRestClientServiceConfiguration.class)
                .writeValueAsString(original);
        final DefaultElasticsearchLowLevelRestClientServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultElasticsearchLowLevelRestClientServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        assertEquals(false, deserialized.getReactiveOperationConfiguration().isMetricsEnabled());
    }
}
