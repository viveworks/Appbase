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

package com.viveworks.appbase.client.service.kafka.producer.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.service.kafka.configuration.SecurityProtocol;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultKafkaProducerClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultKafkaProducerClientIntegrationServiceConfigurationTest {
    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testBuilderDefaults() {
        // Arrange + Act
        final DefaultKafkaProducerClientIntegrationServiceConfiguration config =
                DefaultKafkaProducerClientIntegrationServiceConfiguration.builder().build();

        // Assert — all new fields must default to null
        Assertions.assertNull(config.getSecurityProtocol());
        Assertions.assertNull(config.getSocketConnectionSetupTimeoutMs());
        Assertions.assertNull(config.getSocketConnectionSetupTimeoutMaxMs());
        Assertions.assertNull(config.getPartitionerAvailabilityTimeoutMs());
        Assertions.assertNull(config.getPartitionerIgnoreKeys());

        // Assert — driver metrics default to enabled
        Assertions.assertTrue(config.isDriverMetricsEnabled());
    }

    @Test
    void testBuilderWithDriverMetricsDisabled() {
        // Arrange + Act
        final DefaultKafkaProducerClientIntegrationServiceConfiguration config =
                DefaultKafkaProducerClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();

        // Assert
        Assertions.assertFalse(config.isDriverMetricsEnabled());
    }

    @Test
    void testYamlRoundTripDriverMetricsEnabled() throws Exception {
        // Arrange
        final DefaultKafkaProducerClientIntegrationServiceConfiguration config =
                DefaultKafkaProducerClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultKafkaProducerClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertFalse(deserialized.isDriverMetricsEnabled());
    }

    @Test
    void testBuilderWithSecurityProtocol() {
        // Arrange + Act
        final DefaultKafkaProducerClientIntegrationServiceConfiguration config =
                DefaultKafkaProducerClientIntegrationServiceConfiguration.builder()
                        .securityProtocol(SecurityProtocol.SSL)
                        .build();

        // Assert
        Assertions.assertEquals(SecurityProtocol.SSL, config.getSecurityProtocol());
        Assertions.assertEquals("SSL", config.getSecurityProtocol().getText());
    }

    @Test
    void testYamlRoundTripSecurityProtocol() throws Exception {
        // Arrange
        final DefaultKafkaProducerClientIntegrationServiceConfiguration config =
                DefaultKafkaProducerClientIntegrationServiceConfiguration.builder()
                        .securityProtocol(SecurityProtocol.SSL)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultKafkaProducerClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(SecurityProtocol.SSL, deserialized.getSecurityProtocol());
    }
}
