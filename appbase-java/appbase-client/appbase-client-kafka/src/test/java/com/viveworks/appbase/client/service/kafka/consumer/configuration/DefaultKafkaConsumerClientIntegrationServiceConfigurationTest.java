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

package com.viveworks.appbase.client.service.kafka.consumer.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.service.kafka.configuration.GroupProtocol;
import com.viveworks.appbase.client.service.kafka.configuration.SecurityProtocol;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for {@link DefaultKafkaConsumerClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultKafkaConsumerClientIntegrationServiceConfigurationTest {
    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testBuilderDefaults() {
        // Arrange + Act
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .build();

        // Assert — all new fields must default to null
        Assertions.assertNull(config.getSecurityProtocol());
        Assertions.assertNull(config.getSocketConnectionSetupTimeoutMs());
        Assertions.assertNull(config.getSocketConnectionSetupTimeoutMaxMs());
        Assertions.assertNull(config.getGroupProtocol());
        Assertions.assertNull(config.getRetryBackoffMaxMs());
        Assertions.assertNull(config.getSubscriptionTopics());

        // Assert — driver metrics default to enabled
        Assertions.assertTrue(config.isDriverMetricsEnabled());
    }

    @Test
    void testBuilderWithDriverMetricsDisabled() {
        // Arrange + Act
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .driverMetricsEnabled(false)
                        .build();

        // Assert
        Assertions.assertFalse(config.isDriverMetricsEnabled());
    }

    @Test
    void testYamlRoundTripDriverMetricsEnabled() throws Exception {
        // Arrange
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .driverMetricsEnabled(false)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertFalse(deserialized.isDriverMetricsEnabled());
    }

    @Test
    void testBuilderWithSubscriptionTopics() {
        // Arrange + Act
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .subscriptionTopics(List.of("t1", "t2"))
                        .build();

        // Assert
        Assertions.assertEquals(List.of("t1", "t2"), config.getSubscriptionTopics());
    }

    @Test
    void testYamlRoundTripSubscriptionTopics() throws Exception {
        // Arrange
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .subscriptionTopics(List.of("t1", "t2"))
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(List.of("t1", "t2"), deserialized.getSubscriptionTopics());
    }

    @Test
    void testBuilderWithSecurityProtocol() {
        // Arrange + Act
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .securityProtocol(SecurityProtocol.SSL)
                        .build();

        // Assert
        Assertions.assertEquals(SecurityProtocol.SSL, config.getSecurityProtocol());
        Assertions.assertEquals("SSL", config.getSecurityProtocol().getText());
    }

    @Test
    void testBuilderWithGroupProtocol() {
        // Arrange + Act
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .groupProtocol(GroupProtocol.CONSUMER)
                        .build();

        // Assert
        Assertions.assertEquals(GroupProtocol.CONSUMER, config.getGroupProtocol());
        Assertions.assertEquals("consumer", config.getGroupProtocol().getText());
    }

    @Test
    void testYamlRoundTripSecurityProtocol() throws Exception {
        // Arrange
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration config =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder()
                        .groupId("test-group")
                        .securityProtocol(SecurityProtocol.SSL)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultKafkaConsumerClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(SecurityProtocol.SSL, deserialized.getSecurityProtocol());
    }
}
