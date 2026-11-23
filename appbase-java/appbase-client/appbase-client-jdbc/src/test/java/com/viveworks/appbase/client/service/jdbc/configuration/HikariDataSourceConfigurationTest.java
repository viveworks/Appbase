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

package com.viveworks.appbase.client.service.jdbc.configuration;

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
 * Unit tests for {@link HikariDataSourceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class HikariDataSourceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void testBuilderDefaultsForNewFields() {
        // Arrange + Act
        final HikariDataSourceConfiguration config = HikariDataSourceConfiguration.builder().build();

        // Assert — new fields default to null
        Assertions.assertNull(config.getJdbcUrl());
        Assertions.assertNull(config.getCatalog());
    }

    @Test
    void testBuilderWithJdbcUrl() {
        // Arrange + Act
        final HikariDataSourceConfiguration config = HikariDataSourceConfiguration.builder()
                .jdbcUrl("jdbc:h2:mem:test")
                .build();

        // Assert
        Assertions.assertEquals("jdbc:h2:mem:test", config.getJdbcUrl());
        Assertions.assertNull(config.getCatalog());
    }

    @Test
    void testBuilderWithCatalog() {
        // Arrange + Act
        final HikariDataSourceConfiguration config = HikariDataSourceConfiguration.builder()
                .catalog("main")
                .build();

        // Assert
        Assertions.assertEquals("main", config.getCatalog());
        Assertions.assertNull(config.getJdbcUrl());
    }

    @Test
    void testEqualsComparesBoxedPoolSizesByValueNotReference() {
        // Regression for the RC_REF_COMPARISON fix (SpotBugs internal work item 1438): equals() must compare the
        // Integer maximumPoolSize / minimumIdleConnections by VALUE, not by reference.
        // A value above 127 escapes the Integer autobox cache, so two equal-valued configs hold
        // DISTINCT Integer objects — under the old `==` comparison equals() would wrongly return
        // false; under Objects.equals(...) it returns true. Values chosen > 127 on purpose.
        final HikariDataSourceConfiguration a = HikariDataSourceConfiguration.builder()
                .maximumPoolSize(Integer.valueOf(200))
                .minimumIdleConnections(Integer.valueOf(200))
                .build();
        final HikariDataSourceConfiguration b = HikariDataSourceConfiguration.builder()
                .maximumPoolSize(Integer.valueOf(200))
                .minimumIdleConnections(Integer.valueOf(200))
                .build();

        // Sanity: the boxed values really are distinct references, so `==` would say false.
        Assertions.assertNotSame(a.getMaximumPoolSize(), b.getMaximumPoolSize());

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testYamlRoundTripWithJdbcUrlAndCatalog() throws Exception {
        // Arrange
        final HikariDataSourceConfiguration config = HikariDataSourceConfiguration.builder()
                .jdbcUrl("jdbc:h2:mem:test")
                .catalog("main")
                .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(HikariDataSourceConfiguration.class)
                .writeValueAsString(config);
        final HikariDataSourceConfiguration deserialized = yamlMapper
                .readerFor(HikariDataSourceConfiguration.class)
                .readValue(yaml);

        // Assert
        Assertions.assertEquals(config, deserialized);
        Assertions.assertEquals("jdbc:h2:mem:test", deserialized.getJdbcUrl());
        Assertions.assertEquals("main", deserialized.getCatalog());
    }

    @Test
    void toString_doesNotDiscloseDataSourcePropertiesOrJdbcUrl() {
        // CFG-LEAK-01 (CWE-532, CWE-200): dataSourceProperties is an open-ended map that may carry
        // credentials, and jdbcUrl may embed JDBC userinfo. Both must be redacted in toString(),
        // matching the pattern DruidConnectionConfiguration already applies to its own
        // credential-bearing/open-ended fields.
        final String dataSourcePropertySentinel = "sentinel-hikari-datasourceprop-CFGLEAK01";
        final String jdbcUrlSentinel = "sentinel-hikari-jdbcurl-CFGLEAK01";
        final HikariDataSourceConfiguration config = HikariDataSourceConfiguration.builder()
                .dataSourceProperties(Map.of("arbitrary-property", dataSourcePropertySentinel))
                .jdbcUrl("jdbc:mysql://user:" + jdbcUrlSentinel + "@localhost:3306/test")
                .build();

        final String result = config.toString();

        assertFalse(result.contains(dataSourcePropertySentinel),
                "toString() must not contain dataSourceProperties' content");
        assertFalse(result.contains(jdbcUrlSentinel), "toString() must not contain the raw jdbcUrl");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void getDataSourcePropertiesAndGetJdbcUrl_stillReturnTheRealValues() {
        final Map<String, String> dataSourceProperties = Map.of("arbitrary-property", "sentinel-hikari-datasourceprop-CFGLEAK01");
        final String jdbcUrl = "jdbc:mysql://localhost:3306/test";
        final HikariDataSourceConfiguration config = HikariDataSourceConfiguration.builder()
                .dataSourceProperties(dataSourceProperties)
                .jdbcUrl(jdbcUrl)
                .build();

        Assertions.assertEquals(dataSourceProperties, config.getDataSourceProperties());
        Assertions.assertEquals(jdbcUrl, config.getJdbcUrl());
    }
}
