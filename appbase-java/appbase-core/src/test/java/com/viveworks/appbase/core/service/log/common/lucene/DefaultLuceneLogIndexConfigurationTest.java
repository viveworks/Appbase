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

package com.viveworks.appbase.core.service.log.common.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLuceneLogIndexConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final DefaultLuceneLogIndexConfiguration defaultLuceneLogIndexConfiguration =
                DefaultLuceneLogIndexConfiguration
                        .builder()
                        .callerStackIndexEnabled(true)
                        .callerStackFieldName("callerStack")
                        .formattedMessageFieldName("formattedMessage")
                        .markerIndexEnabled(true)
                        .markerFieldName("marker")
                        .messageFieldName("message")
                        .mdcIndexEnabled(true)
                        .mdcFieldNamePrefix("prefix")
                        .mdcFieldNameSuffix("suffix")
                        .loggerNameFieldName("loggerName")
                        .logLevelFieldName("logLevel")
                        .luceneServiceConfiguration(DefaultLuceneServiceConfiguration.builder().build())
                        .threadNameFieldName("threadName")
                        .throwableCauseMessageFieldName("throwableCause")
                        .throwableCauseTypeNameFieldName("throwableCauseTypeName")
                        .throwableCauseStackTraceIndexEnabled(true)
                        .throwableCauseStackTraceFieldName("throwableCauseStackTrace")
                        .throwableMessageFieldName("throwableMessage")
                        .throwableTypeNameFieldName("throwableTypeName")
                        .throwableStackTraceIndexEnabled(true)
                        .throwableStackTraceFieldName("throwableStack")
                        .timestampLongPointName("timestampLongPoint")
                        .timestampStoredFieldName("timestamp")
                        .enabled(true)
                        .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultLuceneLogIndexConfiguration.class)
                .writeValueAsString(defaultLuceneLogIndexConfiguration);

        final DefaultLuceneLogIndexConfiguration defaultLuceneLogIndexConfiguration1 = yamlMapper
                .readerFor(DefaultLuceneLogIndexConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getCallerStackFieldName(),
                defaultLuceneLogIndexConfiguration1.getCallerStackFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getMdcFieldNamePrefix(),
                defaultLuceneLogIndexConfiguration1.getMdcFieldNamePrefix());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getMdcFieldName("k"),
                defaultLuceneLogIndexConfiguration1.getMdcFieldName("k"));
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getMdcFieldNameSuffix(),
                defaultLuceneLogIndexConfiguration1.getMdcFieldNameSuffix());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                defaultLuceneLogIndexConfiguration1.getFormattedMessageFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                defaultLuceneLogIndexConfiguration1.getLoggerNameFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                defaultLuceneLogIndexConfiguration1.getLogLevelFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                defaultLuceneLogIndexConfiguration1.getMarkerFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                defaultLuceneLogIndexConfiguration1.getLuceneServiceConfiguration());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                defaultLuceneLogIndexConfiguration1.getMessageFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                defaultLuceneLogIndexConfiguration1.getThreadNameFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                defaultLuceneLogIndexConfiguration1.getThrowableCauseMessageFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                defaultLuceneLogIndexConfiguration1.getThrowableCauseStackTraceFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                defaultLuceneLogIndexConfiguration1.getThrowableCauseTypeNameFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                defaultLuceneLogIndexConfiguration1.getThrowableMessageFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                defaultLuceneLogIndexConfiguration1.getThrowableStackTraceFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                defaultLuceneLogIndexConfiguration1.getThrowableTypeNameFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                defaultLuceneLogIndexConfiguration1.getTimestampLongPointName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                defaultLuceneLogIndexConfiguration1.getTimestampStoredFieldName());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                defaultLuceneLogIndexConfiguration1.isCallerStackIndexEnabled());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                defaultLuceneLogIndexConfiguration1.isMarkerIndexEnabled());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.isMdcIndexEnabled(),
                defaultLuceneLogIndexConfiguration1.isMdcIndexEnabled());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                defaultLuceneLogIndexConfiguration1.isThrowableCauseStackTraceIndexEnabled());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                defaultLuceneLogIndexConfiguration1.isThrowableStackTraceIndexEnabled());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.isEnabled(),
                defaultLuceneLogIndexConfiguration1.isEnabled());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration,
                defaultLuceneLogIndexConfiguration1);
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.hashCode(),
                defaultLuceneLogIndexConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration.toString(),
                defaultLuceneLogIndexConfiguration1.toString());
    }

    @Test
    void indexingIsDisabledByDefault() {
        // R3 (internal work item 1073): the Lucene log index is file I/O on the hot log path; nothing should be slowed
        // down by default. A zero-config builder must yield a DISABLED index — users opt in explicitly.
        Assertions.assertFalse(
                DefaultLuceneLogIndexConfiguration.builder().build().isEnabled(),
                "log-index must be disabled by default (R3): file I/O on the hot log path");
    }
}
