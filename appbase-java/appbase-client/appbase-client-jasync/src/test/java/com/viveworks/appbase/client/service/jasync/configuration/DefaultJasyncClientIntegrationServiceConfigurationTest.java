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

package com.viveworks.appbase.client.service.jasync.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultJasyncClientIntegrationServiceConfiguration}, focused on the
 * {@code driverMetricsEnabled} flag: builder default and YAML round-trip of an overridden value.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultJasyncClientIntegrationServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testIsDriverMetricsEnabledDefaultsTrue() {
        // Arrange + Act
        final DefaultJasyncClientIntegrationServiceConfiguration config =
                DefaultJasyncClientIntegrationServiceConfiguration.builder().build();

        // Assert
        Assertions.assertTrue(config.isDriverMetricsEnabled());
    }

    @Test
    void testYamlRoundTripPreservesOverriddenDriverMetricsDisabled() throws Exception {
        // Arrange
        final DefaultJasyncClientIntegrationServiceConfiguration config =
                DefaultJasyncClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultJasyncClientIntegrationServiceConfiguration.class)
                .writeValueAsString(config);
        final DefaultJasyncClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultJasyncClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertFalse(deserialized.isDriverMetricsEnabled());
    }
}
