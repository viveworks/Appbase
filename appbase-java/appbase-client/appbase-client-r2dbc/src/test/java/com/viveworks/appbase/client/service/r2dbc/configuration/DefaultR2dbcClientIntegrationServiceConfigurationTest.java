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

package com.viveworks.appbase.client.service.r2dbc.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultR2dbcClientIntegrationServiceConfiguration}, focused on the
 * {@code driverMetricsEnabled} flag: builder default and YAML round-trip of an overridden value.
 * Uniform with the jasync/Aerospike/Cassandra/ZooKeeper twins, this flag defaults to
 * {@code true}: driver-metrics gates default ON across all client-integration-service twins.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultR2dbcClientIntegrationServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testIsDriverMetricsEnabledDefaultsTrue() {
        // Arrange + Act
        final DefaultR2dbcClientIntegrationServiceConfiguration config =
                DefaultR2dbcClientIntegrationServiceConfiguration.builder().build();

        // Assert
        Assertions.assertTrue(config.isDriverMetricsEnabled());
    }

    @Test
    void testIsDriverMetricsEnabledExplicitFalse() {
        // Arrange + Act
        final DefaultR2dbcClientIntegrationServiceConfiguration config =
                DefaultR2dbcClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();

        // Assert
        Assertions.assertFalse(config.isDriverMetricsEnabled());
    }

    @Test
    void testYamlRoundTripPreservesOverriddenDriverMetricsEnabled() throws Exception {
        // Arrange
        final DefaultR2dbcClientIntegrationServiceConfiguration config =
                DefaultR2dbcClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(true)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultR2dbcClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultR2dbcClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultR2dbcClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertTrue(deserialized.isDriverMetricsEnabled());
    }
}
