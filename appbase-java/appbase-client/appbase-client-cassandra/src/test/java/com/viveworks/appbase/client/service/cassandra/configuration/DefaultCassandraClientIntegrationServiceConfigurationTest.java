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

package com.viveworks.appbase.client.service.cassandra.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Unit tests for {@link DefaultCassandraClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultCassandraClientIntegrationServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testBuilderWithAuthCredentials() {
        // Arrange + Act
        final DefaultCassandraClientIntegrationServiceConfiguration config =
                DefaultCassandraClientIntegrationServiceConfiguration.builder()
                        .username("cassandra")
                        .password("secret")
                        .build();

        // Assert
        Assertions.assertEquals("cassandra", config.getUsername());
        Assertions.assertEquals("secret", config.getPassword());
        // password must be masked in toString
        Assertions.assertTrue(config.toString().contains("[REDACTED]"));
        Assertions.assertFalse(config.toString().contains("secret"));
    }

    @Test
    void testBuilderWithSslAndTimeout() {
        // Arrange + Act
        final DefaultCassandraClientIntegrationServiceConfiguration config =
                DefaultCassandraClientIntegrationServiceConfiguration.builder()
                        .sslEnabled(true)
                        .requestTimeout(Duration.ofSeconds(5))
                        .build();

        // Assert
        Assertions.assertEquals(Boolean.TRUE, config.isSslEnabled());
        Assertions.assertEquals(Duration.ofSeconds(5), config.getRequestTimeout());
    }

    @Test
    void testDriverMetricsEnabledDefaultsToTrue() {
        // Arrange + Act
        final DefaultCassandraClientIntegrationServiceConfiguration config =
                DefaultCassandraClientIntegrationServiceConfiguration.builder()
                        .build();

        // Assert
        Assertions.assertTrue(config.isDriverMetricsEnabled());
    }

    @Test
    void testDriverMetricsEnabledFalseSurvivesYamlRoundTrip() throws Exception {
        // Arrange
        final DefaultCassandraClientIntegrationServiceConfiguration config =
                DefaultCassandraClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultCassandraClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultCassandraClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultCassandraClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertFalse(deserialized.isDriverMetricsEnabled());
    }

    @Test
    void testYamlRoundTripWithAuthAndSsl() throws Exception {
        // Arrange
        final DefaultCassandraClientIntegrationServiceConfiguration config =
                DefaultCassandraClientIntegrationServiceConfiguration.builder()
                        .username("admin")
                        .sslEnabled(true)
                        .requestTimeout(Duration.ofSeconds(10))
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultCassandraClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultCassandraClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultCassandraClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertEquals("admin", deserialized.getUsername());
        Assertions.assertEquals(Boolean.TRUE, deserialized.isSslEnabled());
        Assertions.assertEquals(Duration.ofSeconds(10), deserialized.getRequestTimeout());
    }
}
