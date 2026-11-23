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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Provides a configured YAML {@link ObjectMapper}, for use within this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("WeakerAccess")
public final class YamlUtil {
    public static final String CLASSPATH_FILE_PREFIX = "classpath://";

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private YamlUtil() {
    }

    static {
        ObjectMapperConfigurator.configure(YAML_MAPPER);
    }


    /**
     * Get the configured YAML object mapper.
     *
     * @return YAML object mapper
     */
    public static ObjectMapper getYamlMapper() {
        return YAML_MAPPER;
    }

    /**
     * Reads a YAML file and converts it to <code>configurationType</code> argument. If the file path is prefixed
     * with <code>classpath://</code>, it will read the file from the classpath. It reads a specific YAML node and
     * tries to convert it to <code>configurationType</code> argument. This node name can be configured either using
     * system property (<code>yamlNodeNamePropertyName</code> argument) or environment variable
     * (<code>yamlNodeNameEnvName</code> argument). If they are not found, it falls back to
     * <code>defaultNodeName</code> argument.
     *
     * @param configurationFilePath The configuration file path
     * @param yamlNodeNamePropertyName System property of the YAML node name that represents
     *                                 <code>configurationType</code> argument
     * @param yamlNodeNameEnvName Environment variable of the YAML node name that represents
     *                                <code>configurationType</code> argument
     * @param defaultNodeName The default node name, if either the system property or environment variable is missing
     * @param configurationType The type of the configuration to convert / de-serialize the YAML
     * @param <T> Configuration type
     * @return Configuration
     * @throws IOException if there was an error while reading / de-serializing the YAML file
     */
    @SuppressWarnings("unchecked")
    public static <T> T readConfigurationFromFile(
            final String configurationFilePath,
            @Nullable final String yamlNodeNamePropertyName,
            @Nullable final String yamlNodeNameEnvName,
            @Nullable final String defaultNodeName,
            final Class<T> configurationType) throws IOException
    {
        AppbaseUtil.notBlank(configurationFilePath, "configurationFilePath");
        AppbaseUtil.notNull(configurationType, "configurationType");

        final JsonNode jsonNode = getYamlMapper().readTree(
                getConfigurationFileInputStream(configurationFilePath));
        final Map<String, Object> configurationMap =
                (Map<String, Object>) getYamlMapper().treeToValue(jsonNode, Map.class);

        final String yamlNode = AppbaseUtil.propertyOrEnv(yamlNodeNamePropertyName, yamlNodeNameEnvName);
        if (yamlNode == null || AppbaseUtil.isBlank(yamlNode)) {
            final Map<String, Object> configurationYamlMap =
                    (Map<String, Object>) configurationMap.get(defaultNodeName);
            if (configurationYamlMap != null) {
                return getYamlMapper().convertValue(configurationYamlMap, configurationType);
            } else {
                return getYamlMapper()
                        .readerFor(configurationType)
                        .readValue(getConfigurationFileInputStream(configurationFilePath));
            }
        } else {
            final Map<String, Object> configurationYamlMap = (Map<String, Object>) configurationMap.get(yamlNode);
            return getYamlMapper().convertValue(configurationYamlMap, configurationType);
        }
    }

    /**
     * Read a YAML file into a map.
     *
     * @param configurationFilePath Configuration file path
     * @return Map of nodes and their values
     * @throws IOException if there was an error while reading / de-serializing the YAML file
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> readConfigurationNodes(
            final String configurationFilePath) throws IOException
    {
        AppbaseUtil.notBlank(configurationFilePath, "configurationFilePath");

        final JsonNode jsonNode = getYamlMapper().readTree(
                getConfigurationFileInputStream(configurationFilePath));
        return (Map<String, Object>) getYamlMapper().treeToValue(jsonNode, Map.class);
    }

    /**
     * Convert map of values into a Java type.
     *
     * @param value Map of values
     * @param valueType Value type to convert to
     * @param <T> Java type
     * @return Java object of type T
     */
    public static <T> T convertYamlNode(final Map<String, Object> value, final Class<T> valueType) {
        return getYamlMapper().convertValue(value, valueType);
    }

    private static InputStream getConfigurationFileInputStream(final String configurationFilePath) throws IOException {
        String configurationFilePathCopy = configurationFilePath;
        if (configurationFilePathCopy.startsWith(CLASSPATH_FILE_PREFIX)) {
            configurationFilePathCopy = configurationFilePathCopy.substring(CLASSPATH_FILE_PREFIX.length());
            final InputStream inputStream = YamlUtil.class.getResourceAsStream(configurationFilePathCopy);

            if (inputStream == null) {
                throw new FileNotFoundException(configurationFilePathCopy + " cannot be found in the classpath");
            }

            return inputStream;
        } else {
            final Path path = Path.of(configurationFilePathCopy);
            if (!Files.exists(path)) {
                throw new AppbaseRuntimeException(
                        "Configuration file " + configurationFilePathCopy + " does not exist.");
            }

            if (!Files.isReadable(path)) {
                throw new AppbaseRuntimeException(
                        "Configuration file " + configurationFilePathCopy + " is not readable");
            }

            return Files.newInputStream(path);
        }
    }

    /**
     * Register subtypes with {@link #YAML_MAPPER}.
     *
     * @param subType Sub type
     */
    public static void registerSubType(final Class<?> subType) {
        AppbaseUtil.notNull(subType, "subType");

        ObjectMapperConfigurator.registerSubType(YAML_MAPPER, subType);
    }

    /**
     * Register subtypes with {@link #YAML_MAPPER}.
     *
     * @param subType Sub type
     * @param registerWithJsonMapper If it should be registered with other types of {@link ObjectMapper}s.
     */
    public static void registerSubType(final Class<?> subType, final boolean registerWithJsonMapper) {
        AppbaseUtil.notNull(subType, "subType");

        ObjectMapperConfigurator.registerSubType(YAML_MAPPER, subType);

        if (registerWithJsonMapper) {
            JsonUtil.registerSubType(subType, true);
        }
    }
}
