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

package com.viveworks.appbase.client.service.zookeeper.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

/**
 * Unit tests for {@link DefaultZooKeeperClientIntegrationServiceConfiguration}, including the
 * {@code driverMetricsEnabled} flag: builder default and YAML round-trip of an overridden value.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultZooKeeperClientIntegrationServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testBuilderWithAuthInfoList() {
        // Arrange + Act
        final AuthInfoConfiguration authInfo = AuthInfoConfiguration.builder()
                .scheme("digest")
                .auth("user:pass")
                .build();
        final DefaultZooKeeperClientIntegrationServiceConfiguration config =
                DefaultZooKeeperClientIntegrationServiceConfiguration.builder()
                        .authInfoList(List.of(authInfo))
                        .build();

        // Assert
        Assertions.assertNotNull(config.getAuthInfoList());
        Assertions.assertEquals(1, config.getAuthInfoList().size());
        Assertions.assertEquals("digest", config.getAuthInfoList().get(0).getScheme());
        Assertions.assertEquals("user:pass", config.getAuthInfoList().get(0).getAuth());
    }

    @Test
    void testBuilderWithConnectionHandlingPolicyAndSessionTimeout() {
        // Arrange + Act
        final DefaultZooKeeperClientIntegrationServiceConfiguration config =
                DefaultZooKeeperClientIntegrationServiceConfiguration.builder()
                        .connectionHandlingPolicy(ConnectionHandlingPolicyType.STANDARD)
                        .sessionCreationTimeout(Duration.ofSeconds(30))
                        .build();

        // Assert
        Assertions.assertEquals(ConnectionHandlingPolicyType.STANDARD, config.getConnectionHandlingPolicy());
        Assertions.assertEquals("standard", config.getConnectionHandlingPolicy().getText());
        Assertions.assertEquals(Duration.ofSeconds(30), config.getSessionCreationTimeout());
    }

    @Test
    void testYamlRoundTripWithAuthInfoList() throws Exception {
        // Arrange
        final AuthInfoConfiguration authInfo = AuthInfoConfiguration.builder()
                .scheme("digest")
                .auth("admin:secret")
                .build();
        final DefaultZooKeeperClientIntegrationServiceConfiguration config =
                DefaultZooKeeperClientIntegrationServiceConfiguration.builder()
                        .authInfoList(List.of(authInfo))
                        .connectionHandlingPolicy(ConnectionHandlingPolicyType.LEGACY)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultZooKeeperClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultZooKeeperClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultZooKeeperClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertNotNull(deserialized.getAuthInfoList());
        Assertions.assertEquals(1, deserialized.getAuthInfoList().size());
        Assertions.assertEquals("digest", deserialized.getAuthInfoList().get(0).getScheme());
        Assertions.assertEquals(ConnectionHandlingPolicyType.LEGACY, deserialized.getConnectionHandlingPolicy());
    }

    @Test
    void testIsDriverMetricsEnabledDefaultsTrue() {
        // Arrange + Act
        final DefaultZooKeeperClientIntegrationServiceConfiguration config =
                DefaultZooKeeperClientIntegrationServiceConfiguration.builder().build();

        // Assert
        Assertions.assertTrue(config.isDriverMetricsEnabled());
    }

    @Test
    void testYamlRoundTripPreservesOverriddenDriverMetricsDisabled() throws Exception {
        // Arrange
        final DefaultZooKeeperClientIntegrationServiceConfiguration config =
                DefaultZooKeeperClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultZooKeeperClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultZooKeeperClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultZooKeeperClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertFalse(deserialized.isDriverMetricsEnabled());
    }
}
