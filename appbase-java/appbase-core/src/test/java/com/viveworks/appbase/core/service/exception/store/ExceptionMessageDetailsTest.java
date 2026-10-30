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

package com.viveworks.appbase.core.service.exception.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ExceptionMessageDetailsTest {
    @Test
    void testSerde() throws Exception {
        final ExceptionMessageDetails exceptionMessageDetails = new ExceptionMessageDetails();
        exceptionMessageDetails.setId(1);
        exceptionMessageDetails.setTimestamp(System.currentTimeMillis());
        exceptionMessageDetails.setThreadName("threadName1");
        exceptionMessageDetails.setStackTraceHash(123);
        exceptionMessageDetails.setStackTrace("stackTrace1");
        exceptionMessageDetails.setSourceTypeName(getClass().getName());
        exceptionMessageDetails.setMessage("message1");
        exceptionMessageDetails.setDiscriminator("discriminator1");
        exceptionMessageDetails.setThrowableMessage("throwableMessage1");
        exceptionMessageDetails.setThrowableTypeName(AppbaseException.class.getName());

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ExceptionMessageDetails.class)
                .writeValueAsString(exceptionMessageDetails);
        final ExceptionMessageDetails exceptionMessageDetails1 = yamlMapper
                .readerFor(ExceptionMessageDetails.class)
                .readValue(yaml);

        Assertions.assertEquals(
                exceptionMessageDetails.getDiscriminator(),
                exceptionMessageDetails1.getDiscriminator());
        Assertions.assertEquals(
                exceptionMessageDetails.getId(),
                exceptionMessageDetails1.getId());
        Assertions.assertEquals(
                exceptionMessageDetails.getMessage(),
                exceptionMessageDetails1.getMessage());
        Assertions.assertEquals(
                exceptionMessageDetails.getSourceTypeName(),
                exceptionMessageDetails1.getSourceTypeName());
        Assertions.assertEquals(
                exceptionMessageDetails.getStackTrace(),
                exceptionMessageDetails1.getStackTrace());
        Assertions.assertEquals(
                exceptionMessageDetails.getStackTraceHash(),
                exceptionMessageDetails1.getStackTraceHash());
        Assertions.assertEquals(
                exceptionMessageDetails.getThreadName(),
                exceptionMessageDetails1.getThreadName());
        Assertions.assertEquals(
                exceptionMessageDetails.getThrowableMessage(),
                exceptionMessageDetails1.getThrowableMessage());
        Assertions.assertEquals(
                exceptionMessageDetails.getThrowableTypeName(),
                exceptionMessageDetails1.getThrowableTypeName());
        Assertions.assertEquals(
                exceptionMessageDetails.getTimestamp(),
                exceptionMessageDetails1.getTimestamp());
        Assertions.assertEquals(exceptionMessageDetails, exceptionMessageDetails1);
        Assertions.assertEquals(exceptionMessageDetails.hashCode(), exceptionMessageDetails1.hashCode());
        Assertions.assertEquals(exceptionMessageDetails.toString(), exceptionMessageDetails1.toString());
    }
}
