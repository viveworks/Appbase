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

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.burst.LogEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MarkerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultDuplicateLogHandlerTest {
    @Test
    void testHandler() {
        final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration =
                DefaultDuplicateLogHandlerConfiguration.builder().build();
        final DefaultDuplicateLogHandler defaultDuplicateLogHandler =
                new DefaultDuplicateLogHandler(defaultDuplicateLogHandlerConfiguration);

        final Map<Long, LogEntry> logEntryByTimestampMap = new HashMap<>();
        final long timestamp = System.currentTimeMillis();
        for (int i = 1; i <= 10; ++i) {
            int paramSuffix = 1;
            if (i > 6) {
                paramSuffix = 10;
            }

            final LogEntry logEntry = LogEntry
                    .builder()
                    .throwable(new AppbaseException("Error", new AppbaseRuntimeException("Cause")))
                    .logLevel(LogLevel.ERROR)
                    .marker(MarkerFactory.getMarker("Marker"))
                    .threadGroupName(Thread.currentThread().getThreadGroup().getName())
                    .threadName(Thread.currentThread().getName())
                    .params(new Object[] {"A" + paramSuffix, "B" + paramSuffix})
                    .build();
            logEntryByTimestampMap.put(timestamp * i * 1000L, logEntry);
        }

        Assertions.assertDoesNotThrow(() -> {
            defaultDuplicateLogHandler.duplicateFormattedMessage(
                    getClass().getName(), "Fmt msg 1: {}, {}", logEntryByTimestampMap);
        });

        Assertions.assertDoesNotThrow(() -> {
            defaultDuplicateLogHandler.duplicateThrowableMessage(
                    getClass().getName(),
                    AppbaseException.class.getName(),
                    "Exception msg 1",
                    logEntryByTimestampMap);
        });

        Assertions.assertDoesNotThrow(() -> {
            defaultDuplicateLogHandler.duplicateThrowableTypeName(
                    getClass().getName(), AppbaseException.class.getName(), logEntryByTimestampMap);
        });
    }
}
