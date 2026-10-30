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

package com.viveworks.appbase.core.service.log.store.criteria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.ExactTimestampCriteria;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 */
class LogRecordCriteriaTest {
    @Test
    void testYamlSerde() throws Exception {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .logLevelCriteria(new LogLevelCriteria(LogLevel.INFO, LogLevelCriteria.Match.GREATER_OR_EQUAL))
                .timestampCriteria(new ExactTimestampCriteria(System.currentTimeMillis()))
                .callerData(null)
                .formattedMessage("fm {}")
                .loggerName(getClass().getName())
                .markerName("marker")
                .mdcPropertyMap(null)
                .message("fm message")
                .threadName(Thread.currentThread().getName())
                .throwableCauseMessage("throwableCauseMessage")
                .throwableCauseStackTrace(null)
                .throwableCauseTypeName(AppbaseRuntimeException.class.getName())
                .throwableMessage("throwableMessage")
                .throwableStackTrace(null)
                .throwableTypeName(AppbaseRuntimeException.class.getName())
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LogRecordCriteria.class)
                .writeValueAsString(logRecordCriteria);

        final LogRecordCriteria logRecordCriteria1 = yamlMapper
                .readerFor(LogRecordCriteria.class)
                .readValue(yaml);

        Assertions.assertEquals(
                logRecordCriteria.getCallerData(),
                logRecordCriteria1.getCallerData());
        Assertions.assertEquals(
                logRecordCriteria.getFormattedMessage(),
                logRecordCriteria1.getFormattedMessage());
        Assertions.assertEquals(
                logRecordCriteria.getLoggerName(),
                logRecordCriteria1.getLoggerName());
        Assertions.assertEquals(
                logRecordCriteria.getMarkerName(),
                logRecordCriteria1.getMarkerName());
        Assertions.assertEquals(
                logRecordCriteria.getLogLevelCriteria(),
                logRecordCriteria1.getLogLevelCriteria());
        Assertions.assertEquals(
                logRecordCriteria.getMdcPropertyMap(),
                logRecordCriteria1.getMdcPropertyMap());
        Assertions.assertEquals(
                logRecordCriteria.getMessage(),
                logRecordCriteria1.getMessage());
        Assertions.assertEquals(
                logRecordCriteria.getThreadName(),
                logRecordCriteria1.getThreadName());
        Assertions.assertEquals(
                logRecordCriteria.getThrowableMessage(),
                logRecordCriteria1.getThrowableMessage());
        Assertions.assertEquals(
                logRecordCriteria.getThrowableTypeName(),
                logRecordCriteria1.getThrowableTypeName());
        Assertions.assertEquals(
                logRecordCriteria.getThrowableStackTrace(),
                logRecordCriteria1.getThrowableStackTrace());
        Assertions.assertEquals(
                logRecordCriteria.getThrowableCauseMessage(),
                logRecordCriteria1.getThrowableCauseMessage());
        Assertions.assertEquals(
                logRecordCriteria.getThrowableCauseTypeName(),
                logRecordCriteria1.getThrowableCauseTypeName());
        Assertions.assertEquals(
                logRecordCriteria.getThrowableCauseStackTrace(),
                logRecordCriteria1.getThrowableCauseStackTrace());
        Assertions.assertEquals(
                logRecordCriteria.getTimestampCriteria(),
                logRecordCriteria1.getTimestampCriteria());
        Assertions.assertEquals(
                logRecordCriteria,
                logRecordCriteria1);
        Assertions.assertEquals(
                logRecordCriteria.hashCode(),
                logRecordCriteria1.hashCode());
        Assertions.assertEquals(
                logRecordCriteria.toString(),
                logRecordCriteria1.toString());
    }
}
