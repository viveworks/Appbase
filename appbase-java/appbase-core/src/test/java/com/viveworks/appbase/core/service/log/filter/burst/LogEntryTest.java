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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LogEntryTest {
    @Test
    void testPojo() {
        final LogEntry logEntry = LogEntry
                .builder()
                .logLevel(LogLevel.INFO)
                .marker(null)
                .threadGroupName("threadGroupName")
                .threadName("threadName")
                .throwable(new AppbaseException())
                .params(null)
                .build();

        Assertions.assertEquals(LogLevel.INFO, logEntry.getLogLevel());
        Assertions.assertNull(logEntry.getMarker());
        Assertions.assertEquals("threadGroupName", logEntry.getThreadGroupName());
        Assertions.assertEquals("threadName", logEntry.getThreadName());
        Assertions.assertEquals(AppbaseException.class, logEntry.getThrowable().getClass());
        Assertions.assertNull(logEntry.getParams());

        final LogEntry logEntry1 = LogEntry
                .builder()
                .logLevel(logEntry.getLogLevel())
                .marker(logEntry.getMarker())
                .threadGroupName(logEntry.getThreadGroupName())
                .threadName(logEntry.getThreadName())
                .throwable(logEntry.getThrowable())
                .params(logEntry.getParams())
                .build();
        Assertions.assertEquals(logEntry, logEntry1);
        Assertions.assertEquals(logEntry.hashCode(), logEntry1.hashCode());
        Assertions.assertEquals(logEntry.toString(), logEntry1.toString());
    }
}
