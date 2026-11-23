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

package com.viveworks.appbase.core.service.exception.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class EntityManagerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put("k1", "v1");

        final Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put("k2", "v2");

        final EntityManagerConfiguration entityManagerConfiguration = new EntityManagerConfiguration(
                "pun", entityManagerFactoryProperties, entityManagerProperties);
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(EntityManagerConfiguration.class)
                .writeValueAsString(entityManagerConfiguration);
        final EntityManagerConfiguration entityManagerConfiguration1 = yamlMapper
                .readerFor(EntityManagerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                entityManagerConfiguration.getPersistenceUnitName(),
                entityManagerConfiguration1.getPersistenceUnitName());
        Assertions.assertEquals(
                entityManagerConfiguration.getEntityManagerFactoryProperties(),
                entityManagerConfiguration1.getEntityManagerFactoryProperties());
        Assertions.assertEquals(
                entityManagerConfiguration.getEntityManagerProperties(),
                entityManagerConfiguration1.getEntityManagerProperties());
        Assertions.assertEquals(entityManagerConfiguration, entityManagerConfiguration1);
        Assertions.assertEquals(entityManagerConfiguration.hashCode(), entityManagerConfiguration1.hashCode());
        Assertions.assertEquals(entityManagerConfiguration.toString(), entityManagerConfiguration1.toString());
    }

    @Test
    void toStringDoesNotDiscloseEntityManagerFactoryOrEntityManagerProperties() {
        // CFG-LEAK-02 (CWE-532, CWE-200): entityManagerFactoryProperties and entityManagerProperties
        // are open-ended maps that may carry credentials (e.g. JDBC/JPA connection properties).
        // Both must be redacted in toString(), matching the pattern DruidConnectionConfiguration
        // already applies to its own open-ended map fields.
        final String factoryPropertySentinel = "sentinel-entitymanager-factoryprop-CFGLEAK02";
        final String propertySentinel = "sentinel-entitymanager-prop-CFGLEAK02";
        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put("arbitrary-factory-property", factoryPropertySentinel);
        final Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put("arbitrary-property", propertySentinel);

        final EntityManagerConfiguration entityManagerConfiguration = new EntityManagerConfiguration(
                "pun", entityManagerFactoryProperties, entityManagerProperties);

        final String result = entityManagerConfiguration.toString();

        Assertions.assertFalse(result.contains(factoryPropertySentinel),
                "toString() must not contain entityManagerFactoryProperties' content");
        Assertions.assertFalse(result.contains(propertySentinel),
                "toString() must not contain entityManagerProperties' content");
        Assertions.assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void getEntityManagerFactoryPropertiesAndGetEntityManagerPropertiesStillReturnTheRealValues() {
        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put("arbitrary-factory-property", "sentinel-entitymanager-factoryprop-CFGLEAK02");
        final Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put("arbitrary-property", "sentinel-entitymanager-prop-CFGLEAK02");

        final EntityManagerConfiguration entityManagerConfiguration = new EntityManagerConfiguration(
                "pun", entityManagerFactoryProperties, entityManagerProperties);

        Assertions.assertEquals(
                entityManagerFactoryProperties, entityManagerConfiguration.getEntityManagerFactoryProperties());
        Assertions.assertEquals(entityManagerProperties, entityManagerConfiguration.getEntityManagerProperties());
    }
}
