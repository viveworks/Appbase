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

package com.viveworks.appbase.core.service.exception.store.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JpaExceptionStoreConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration =
                CoreTestFixtures.getJpaExceptionStoreConfiguration();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(JpaExceptionStoreConfiguration.class)
                .writeValueAsString(jpaExceptionStoreConfiguration);
        final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration1 = yamlMapper
                .readerFor(JpaExceptionStoreConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getEntityManagerConfiguration(),
                jpaExceptionStoreConfiguration1.getEntityManagerConfiguration());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                jpaExceptionStoreConfiguration1.getStackTraceSerializationMaxLength());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                jpaExceptionStoreConfiguration1.getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getDiscriminator(),
                jpaExceptionStoreConfiguration1.getDiscriminator());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                jpaExceptionStoreConfiguration1.getDiscriminatorColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getIdColumnName(),
                jpaExceptionStoreConfiguration1.getIdColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getMessageColumnName(),
                jpaExceptionStoreConfiguration1.getMessageColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                jpaExceptionStoreConfiguration1.getThrowableTypeNameColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getTimestampColumnName(),
                jpaExceptionStoreConfiguration1.getTimestampColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.isEnabled(),
                jpaExceptionStoreConfiguration1.isEnabled());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                jpaExceptionStoreConfiguration1.getSourceTypeNameColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                jpaExceptionStoreConfiguration1.getStackTraceHashColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                jpaExceptionStoreConfiguration1.getStackTraceColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                jpaExceptionStoreConfiguration1.getStackTraceSerializationMaxLength());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                jpaExceptionStoreConfiguration1.getThreadNameColumnName());
        Assertions.assertEquals(
                jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                jpaExceptionStoreConfiguration1.getThrowableMessageColumnName());
        Assertions.assertEquals(jpaExceptionStoreConfiguration, jpaExceptionStoreConfiguration1);
        Assertions.assertEquals(jpaExceptionStoreConfiguration.hashCode(), jpaExceptionStoreConfiguration1.hashCode());
        Assertions.assertEquals(jpaExceptionStoreConfiguration.toString(), jpaExceptionStoreConfiguration1.toString());
    }
}
