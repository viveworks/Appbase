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

package com.viveworks.appbase.configwizard.configurations.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientIntegrationServiceConfiguration;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FieldPrompter} — introspect and buildFromMap.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class FieldPrompterTest {

    private final ConfigScanner scanner = new ConfigScanner();
    private final FieldPrompter prompter = new FieldPrompter(new ObjectMapper());

    @Test
    void introspect_redisConfig_returnsExpectedFields() {
        var info = scanner.scan().stream()
                .filter(c -> c.simpleName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();

        var fields = prompter.introspect(info);

        assertFalse(fields.isEmpty());
        // hostPortConfiguration must appear (it is a complex nested type)
        assertTrue(fields.stream().anyMatch(f -> "hostPortConfiguration".equals(f.name())));
        // at least one field must be recognised as a nested buildable type
        assertTrue(fields.stream().anyMatch(f -> f.nestedClass() != null));
        // database is @Nullable Integer — reference type, not required (required=false)
        assertFalse(fields.stream()
                .filter(f -> "database".equals(f.name()))
                .findFirst().orElseThrow().required());
    }

    @Test
    void buildFromMap_redisConfig_roundTrips() throws Exception {
        var info = scanner.scan().stream()
                .filter(c -> c.simpleName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();

        var fieldValues = new HashMap<String, Object>();
        fieldValues.put("hostPortConfiguration", Map.of("host", "myhost", "port", 1234));
        fieldValues.put("ssl", true);

        var built = prompter.buildFromMap(info, fieldValues);

        assertNotNull(built);
        // Verify the ssl field was populated from the input map
        ObjectMapper inspector = new ObjectMapper();
        JsonNode tree = inspector.valueToTree(built);
        assertTrue(tree.path("ssl").asBoolean(), "ssl field should be true");
    }

    @Test
    void introspect_firebaseConfig_usesCanonicalJacksonFactoryProperty() {
        var fields = prompter.introspect(firebaseInfo());

        assertTrue(fields.stream().anyMatch(
                field -> "useDefaultJacksonFactory".equals(field.name())));
        assertFalse(fields.stream().anyMatch(
                field -> "createJacksonFactory".equals(field.name())));
    }

    @Test
    void buildFromMap_firebaseJacksonFactoryFalse_succeeds() {
        var built = (DefaultFirebaseAppClientIntegrationServiceConfiguration)
                prompter.buildFromMap(firebaseInfo(),
                        Map.of("useDefaultJacksonFactory", false));

        assertFalse(built.isUseDefaultJacksonFactory());
    }

    @Test
    void firebaseJacksonFactoryProperty_yamlRoundTrips() throws Exception {
        ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        var built = (DefaultFirebaseAppClientIntegrationServiceConfiguration)
                prompter.buildFromMap(firebaseInfo(),
                        Map.of("useDefaultJacksonFactory", false));

        String yaml = yamlMapper.writeValueAsString(built);
        var roundTripped = yamlMapper.readValue(
                yaml, DefaultFirebaseAppClientIntegrationServiceConfiguration.class);

        assertFalse(roundTripped.isUseDefaultJacksonFactory());
        JsonNode yamlTree = yamlMapper.readTree(yaml);
        assertTrue(yamlTree.has("useDefaultJacksonFactory"));
        assertFalse(yamlTree.path("useDefaultJacksonFactory").asBoolean());
    }

    @Test
    void buildFromMap_mediatorRemovedQueueCapacity_succeeds() {
        ObjectMapper mapper = YamlUtil.getYamlMapper();
        var info = configInfo(DefaultMediatorServiceConfiguration.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> defaults = mapper.convertValue(info.defaultInstance(), Map.class);
        defaults.put("removedMessageQueueCapacity", 129);

        var built = (DefaultMediatorServiceConfiguration)
                prompter.buildFromMap(info, defaults);

        assertTrue(built.getRemovedMessageQueueCapacity() == 129);
        assertNotNull(built.getDefaultMessageQueuesConfiguration());
    }

    @Test
    void buildFromMap_reactorDefaultSchedulerSubtype_succeeds() {
        ObjectMapper mapper = YamlUtil.getYamlMapper();
        var info = configInfo(DefaultReactorServiceConfiguration.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> defaults = mapper.convertValue(
                info.defaultInstance(), Map.class);

        var built = (DefaultReactorServiceConfiguration)
                prompter.buildFromMap(info, defaults);

        assertNotNull(built.getSchedulerConfigurations());
        assertFalse(built.getSchedulerConfigurations().isEmpty());
        assertTrue(built.getSchedulerConfigurations().get(0).getClass()
                .getSimpleName().equals("MediatorExecutorServiceSchedulerConfiguration"));
    }

    private ConfigClassInfo firebaseInfo() {
        return configInfo(DefaultFirebaseAppClientIntegrationServiceConfiguration.class);
    }

    private ConfigClassInfo configInfo(final Class<?> configClass) {
        return scanner.scan().stream()
                .filter(info -> configClass.equals(info.configClass()))
                .findFirst()
                .orElseThrow();
    }
}
