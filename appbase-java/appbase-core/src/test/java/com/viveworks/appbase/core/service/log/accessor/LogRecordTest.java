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

package com.viveworks.appbase.core.service.log.accessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LogRecordTest {
    @Test
    void testYamlSerde() throws Exception {
        final LogRecord logRecord = LogRecord
                .builder()
                .throwableRecord(ThrowableRecord.builder().build())
                .logLevel(LogLevel.TRACE)
                .callerData("callerStackTrace")
                .formattedMessage("Formatted message: {}")
                .loggerName(LogRecordTest.class.getName())
                .markerName("MARK")
                .mdcPropertyMap(Map.of("user", "abcd"))
                .message("Formated message: message")
                .threadName(Thread.currentThread().getName())
                .timestamp(System.currentTimeMillis())
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LogRecord.class)
                .writeValueAsString(logRecord);

        final LogRecord logRecord1 = yamlMapper
                .readerFor(LogRecord.class)
                .readValue(yaml);

        Assertions.assertEquals(
                logRecord.getCallerData(),
                logRecord1.getCallerData());
        Assertions.assertEquals(
                logRecord.getFormattedMessage(),
                logRecord1.getFormattedMessage());
        Assertions.assertEquals(
                logRecord.getLoggerName(),
                logRecord1.getLoggerName());
        Assertions.assertEquals(
                logRecord.getLogLevel(),
                logRecord1.getLogLevel());
        Assertions.assertEquals(
                logRecord.getMarkerName(),
                logRecord1.getMarkerName());
        Assertions.assertEquals(
                logRecord.getMdcPropertyMap(),
                logRecord1.getMdcPropertyMap());
        Assertions.assertEquals(
                logRecord.getMessage(),
                logRecord1.getMessage());
        Assertions.assertEquals(
                logRecord.getThreadName(),
                logRecord1.getThreadName());
        Assertions.assertEquals(
                logRecord.getThrowableRecord(),
                logRecord1.getThrowableRecord());
        Assertions.assertEquals(
                logRecord.getTimestamp(),
                logRecord1.getTimestamp());
        Assertions.assertEquals(
                logRecord,
                logRecord1);
        Assertions.assertEquals(
                logRecord.hashCode(),
                logRecord1.hashCode());
        Assertions.assertEquals(
                logRecord.toString(),
                logRecord1.toString());
    }
}
