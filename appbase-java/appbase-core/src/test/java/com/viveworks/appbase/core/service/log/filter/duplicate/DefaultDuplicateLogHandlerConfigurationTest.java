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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultDuplicateLogHandlerConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration =
                DefaultDuplicateLogHandlerConfiguration
                .builder()
                .formattedMessageLogLevel(LogLevel.INFO)
                .throwableLogLevel(LogLevel.ERROR)
                .countTextPrefix("duplicate count")
                .countTextSuffix("")
                .markerName("DUP")
                .throwableTypeNameAndMessageSeparator(":")
                .logParams(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDuplicateLogHandlerConfiguration.class)
                .writeValueAsString(defaultDuplicateLogHandlerConfiguration);

        final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration1 = yamlMapper
                .readerFor(DefaultDuplicateLogHandlerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.getFormattedMessageLogLevel(),
                defaultDuplicateLogHandlerConfiguration1.getFormattedMessageLogLevel());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.getThrowableLogLevel(),
                defaultDuplicateLogHandlerConfiguration1.getThrowableLogLevel());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.getCountTextPrefix(),
                defaultDuplicateLogHandlerConfiguration1.getCountTextPrefix());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.getCountTextSuffix(),
                defaultDuplicateLogHandlerConfiguration1.getCountTextSuffix());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.getMarkerName(),
                defaultDuplicateLogHandlerConfiguration1.getMarkerName());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.getThrowableTypeNameAndMessageSeparator(),
                defaultDuplicateLogHandlerConfiguration1.getThrowableTypeNameAndMessageSeparator());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.isLogParamsAndThrowable(),
                defaultDuplicateLogHandlerConfiguration1.isLogParamsAndThrowable());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration,
                defaultDuplicateLogHandlerConfiguration1);
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.hashCode(),
                defaultDuplicateLogHandlerConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultDuplicateLogHandlerConfiguration.toString(),
                defaultDuplicateLogHandlerConfiguration1.toString());
    }
}
