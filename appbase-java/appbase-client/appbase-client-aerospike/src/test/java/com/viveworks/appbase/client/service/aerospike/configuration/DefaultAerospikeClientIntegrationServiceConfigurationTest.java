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

package com.viveworks.appbase.client.service.aerospike.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for {@link DefaultAerospikeClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultAerospikeClientIntegrationServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testBuilderDefaults() {
        // Arrange + Act
        final DefaultAerospikeClientIntegrationServiceConfiguration config =
                DefaultAerospikeClientIntegrationServiceConfiguration.builder().build();

        // Assert — all new fields must default to null
        Assertions.assertNull(config.getTimeout());
        Assertions.assertNull(config.getLoginTimeout());
        Assertions.assertNull(config.getMaxConnsPerNode());
        Assertions.assertNull(config.getMinConnsPerNode());
        Assertions.assertNull(config.getConnPoolsPerNode());
        Assertions.assertNull(config.getMaxSocketIdle());
        Assertions.assertNull(config.getTendInterval());
        Assertions.assertNull(config.getFailIfNotConnected());
        Assertions.assertNull(config.getUser());
        Assertions.assertNull(config.getPassword());
        Assertions.assertNull(config.getAuthMode());
        Assertions.assertNull(config.getTlsEnabled());
        Assertions.assertNull(config.getReadModeSC());
        Assertions.assertNull(config.getReadModeAP());
        Assertions.assertNull(config.getReplicaPolicy());
        Assertions.assertNull(config.getRackAware());
        Assertions.assertNull(config.getRackIds());
        Assertions.assertTrue(config.isDriverMetricsEnabled());
    }

    @Test
    void testBuilderWithConnectionAndAuth() {
        // Arrange + Act
        final DefaultAerospikeClientIntegrationServiceConfiguration config =
                DefaultAerospikeClientIntegrationServiceConfiguration.builder()
                        .timeout(500)
                        .user("admin")
                        .authMode(AuthMode.INTERNAL)
                        .build();

        // Assert
        Assertions.assertEquals(500, config.getTimeout());
        Assertions.assertEquals("admin", config.getUser());
        Assertions.assertEquals(AuthMode.INTERNAL, config.getAuthMode());
        Assertions.assertEquals("INTERNAL", config.getAuthMode().getText());
    }

    @Test
    void testBuilderWithTlsAndReadPolicies() {
        // Arrange + Act
        final DefaultAerospikeClientIntegrationServiceConfiguration config =
                DefaultAerospikeClientIntegrationServiceConfiguration.builder()
                        .tlsEnabled(true)
                        .readModeSC(ReadModeSC.LINEARIZE)
                        .build();

        // Assert
        Assertions.assertEquals(Boolean.TRUE, config.getTlsEnabled());
        Assertions.assertEquals(ReadModeSC.LINEARIZE, config.getReadModeSC());
        Assertions.assertEquals("LINEARIZE", config.getReadModeSC().getText());
    }

    @Test
    void testYamlRoundTripWithAuthAndTimeout() throws Exception {
        // Arrange
        final DefaultAerospikeClientIntegrationServiceConfiguration config =
                DefaultAerospikeClientIntegrationServiceConfiguration.builder()
                        .user("admin")
                        .authMode(AuthMode.INTERNAL)
                        .timeout(2000)
                        .rackIds(List.of(1, 2))
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultAerospikeClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultAerospikeClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultAerospikeClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertEquals("admin", deserialized.getUser());
        Assertions.assertEquals(AuthMode.INTERNAL, deserialized.getAuthMode());
        Assertions.assertEquals(2000, deserialized.getTimeout());
        Assertions.assertEquals(List.of(1, 2), deserialized.getRackIds());
    }

    @Test
    void testYamlRoundTripPreservesOverriddenDriverMetricsDisabled() throws Exception {
        // Arrange
        final DefaultAerospikeClientIntegrationServiceConfiguration config =
                DefaultAerospikeClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultAerospikeClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultAerospikeClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultAerospikeClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertFalse(deserialized.isDriverMetricsEnabled());
    }
}
