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

package com.viveworks.appbase.core.util.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class YamlUtilTest {
    @Test
    void testGetYamlMapper() {
        final ObjectMapper mapper = YamlUtil.getYamlMapper();
        Assertions.assertNotNull(mapper);
    }

    @Test
    void testReadConfigurationFromFile() throws IOException {
        final String classpathFile = "classpath:///appbaseBootstrapConfiguration.yaml";
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                YamlUtil.readConfigurationFromFile(
                        classpathFile,
                        AppbaseConstants.CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME,
                        AppbaseConstants.CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME_ENV_NAME,
                        AppbaseConstants.DEFAULT_CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME,
                        DefaultMediatorServiceConfiguration.class);
        Assertions.assertNotNull(defaultMediatorServiceConfiguration);
    }

    @Test
    void testReadConfigurationNodes() throws Exception {
        final Map<String, Object> configurationNodes =
                YamlUtil.readConfigurationNodes("classpath:///appbaseBootstrapConfiguration.yaml");
        Assertions.assertNotNull(configurationNodes);
        Assertions.assertTrue(configurationNodes.containsKey(
                AppbaseConstants.DEFAULT_CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME));
    }

    @Test
    void testConvertYamlNode() throws Exception {
        final Map<String, Object> configurationNodes =
                YamlUtil.readConfigurationNodes("classpath:///appbaseBootstrapConfiguration.yaml");
        Assertions.assertNotNull(configurationNodes);

        final Map<String, Object> defaultConfigurationServiceConfigurationNodes =
                (Map<String, Object>) configurationNodes.get(
                        AppbaseConstants.DEFAULT_CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME);
        Assertions.assertNotNull(defaultConfigurationServiceConfigurationNodes);

        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                YamlUtil.convertYamlNode(
                        defaultConfigurationServiceConfigurationNodes, DefaultMediatorServiceConfiguration.class);
        Assertions.assertNotNull(defaultMediatorServiceConfiguration);

    }
}
