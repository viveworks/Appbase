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

package com.viveworks.appbase.core.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.util.jackson.YamlUtil;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class CoreTestUtil {
    private CoreTestUtil() {
    }

    public static Method getMethod(
            final Class<?> type,
            final String methodName,
            final Class<?>... parameterTypes) throws NoSuchMethodException
    {
        final Method method = type.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        return method;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(
            final Method method,
            final Object object,
            final Object... args) throws Exception
    {
        return (T) method.invoke(object, args);
    }

    /**
     * Main to generate configuration files in YAML format.
     *
     * @param args Command-line arguments
     * @throws Exception on IO exceptions
     */
    public static void main(final String[] args) throws Exception {
        printMediatorServiceConfigurationYaml();
        printSampleComposingConfiguration();
        printSampleComposingConfigurationWithCustomNodeName();
    }

    private static void printMediatorServiceConfigurationYaml() throws Exception {
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration
                        .builder()
                        .ignoredMessageTypeSet(Set.of(MessageType.EXCEPTION))
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultMediatorServiceConfiguration.class)
                .writeValueAsString(defaultMediatorServiceConfiguration);

        System.out.println(System.lineSeparator() + yaml + System.lineSeparator());
    }

    private static void printSampleComposingConfiguration() throws Exception {
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration
                        .builder()
                        .ignoredMessageTypeSet(Set.of(MessageType.EXCEPTION))
                        .build();
        final SampleComposingConfiguration sampleComposingConfiguration =
                new SampleComposingConfiguration("localhost", 1234, defaultMediatorServiceConfiguration);
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(SampleComposingConfiguration.class)
                .writeValueAsString(sampleComposingConfiguration);

        System.out.println(System.lineSeparator() + yaml + System.lineSeparator());
    }

    private static void printSampleComposingConfigurationWithCustomNodeName() throws Exception {
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration
                        .builder()
                        .ignoredMessageTypeSet(Set.of(MessageType.EXCEPTION))
                        .build();
        final SampleComposingConfigurationWithCustomNodeName sampleComposingConfigurationWithCustomNodeName =
                new SampleComposingConfigurationWithCustomNodeName(
                        "localhost", 8080, 100L, defaultMediatorServiceConfiguration);
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(SampleComposingConfigurationWithCustomNodeName.class)
                .writeValueAsString(sampleComposingConfigurationWithCustomNodeName);

        System.out.println(System.lineSeparator() + yaml + System.lineSeparator());
    }

    @SuppressWarnings("unused")
    public static class SampleComposingConfiguration {
        private final String hostname;
        private final Integer port;
        private final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration;

        @JsonCreator
        SampleComposingConfiguration(@JsonProperty("hostname")
                                     final String hostname,
                                     @JsonProperty("port")
                                     final Integer port,
                                     @JsonProperty("defaultMediatorServiceConfiguration")
                                     final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration)
        {
            this.hostname = hostname;
            this.port = port;
            this.defaultMediatorServiceConfiguration = defaultMediatorServiceConfiguration;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        public DefaultMediatorServiceConfiguration getDefaultMediatorServiceConfiguration() {
            return defaultMediatorServiceConfiguration;
        }
    }

    @SuppressWarnings("unused")
    public static class SampleComposingConfigurationWithCustomNodeName {
        private final String hostname;

        private final Integer port;

        private final Long timeoutMillis;

        @JsonProperty("mediatorServiceConfig")
        private final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration;

        SampleComposingConfigurationWithCustomNodeName(
                @JsonProperty("hostname")
                final String hostname,
                @JsonProperty("port")
                final Integer port,
                @JsonProperty("timeoutMillis")
                final Long timeoutMillis,
                @JsonProperty("mediatorServiceConfig")
                final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration)
        {
            this.hostname = hostname;
            this.port = port;
            this.timeoutMillis = timeoutMillis;
            this.defaultMediatorServiceConfiguration = defaultMediatorServiceConfiguration;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        public Long getTimeoutMillis() {
            return timeoutMillis;
        }

        public DefaultMediatorServiceConfiguration getDefaultMediatorServiceConfiguration() {
            return defaultMediatorServiceConfiguration;
        }
    }
}
