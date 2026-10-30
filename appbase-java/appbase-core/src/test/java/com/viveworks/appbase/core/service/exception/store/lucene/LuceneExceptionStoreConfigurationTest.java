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

package com.viveworks.appbase.core.service.exception.store.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LuceneExceptionStoreConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration =
                LuceneExceptionStoreConfiguration
                .builder()
                .contextFieldName("context")
                .discriminator("localhost")
                .discriminatorFieldName("discriminator")
                .enabled(true)
                .idFieldName("id")
                .idIntPointName("idIntPointName")
                .messageFieldName("message")
                .sourceTypeNameFieldName("sourceTypeName")
                .stackTraceFieldName("stackTrace")
                .stackTraceHashFieldName("stackTraceHash")
                .stackTraceHashIntPointName("stackTraceHashIntPoint")
                .threadNameFieldName("threadName")
                .throwableMessageFieldName("throwableMessage")
                .throwableTypeNameFieldName("throwableTypeName")
                .timestampFieldName("timestamp")
                .timestampLongPointName("timestampLongPoint")
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LuceneExceptionStoreConfiguration.class)
                .writeValueAsString(luceneExceptionStoreConfiguration);

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration1 = yamlMapper
                .readerFor(LuceneExceptionStoreConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                luceneExceptionStoreConfiguration,
                luceneExceptionStoreConfiguration1);
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getContextFieldName(),
                luceneExceptionStoreConfiguration1.getContextFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getDiscriminator(),
                luceneExceptionStoreConfiguration1.getDiscriminator());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getDiscriminatorFieldName(),
                luceneExceptionStoreConfiguration1.getDiscriminatorFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getIdFieldName(),
                luceneExceptionStoreConfiguration1.getIdFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getIdIntPointName(),
                luceneExceptionStoreConfiguration1.getIdIntPointName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getMessageFieldName(),
                luceneExceptionStoreConfiguration1.getMessageFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getSourceTypeNameFieldName(),
                luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getStackTraceFieldName(),
                luceneExceptionStoreConfiguration1.getStackTraceFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getStackTraceHashFieldName(),
                luceneExceptionStoreConfiguration1.getStackTraceHashFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getStackTraceHashIntPointName(),
                luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getThreadNameFieldName(),
                luceneExceptionStoreConfiguration1.getThreadNameFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getThrowableMessageFieldName(),
                luceneExceptionStoreConfiguration1.getThrowableMessageFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getThrowableTypeNameFieldName(),
                luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getTimestampFieldName(),
                luceneExceptionStoreConfiguration1.getTimestampFieldName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getTimestampLongPointName(),
                luceneExceptionStoreConfiguration1.getTimestampLongPointName());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.getExceptionStoreType(),
                luceneExceptionStoreConfiguration1.getExceptionStoreType());
        Assertions.assertEquals(
                LuceneExceptionStore.class,
                luceneExceptionStoreConfiguration.getExceptionStoreType());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration,
                luceneExceptionStoreConfiguration1);
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.hashCode(),
                luceneExceptionStoreConfiguration1.hashCode());
        Assertions.assertEquals(
                luceneExceptionStoreConfiguration.toString(),
                luceneExceptionStoreConfiguration1.toString());
    }
}
