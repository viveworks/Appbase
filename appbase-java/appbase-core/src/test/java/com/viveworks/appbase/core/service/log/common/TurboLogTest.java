/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
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

package com.viveworks.appbase.core.service.log.common;

import com.viveworks.appbase.core.service.log.filter.LogLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class TurboLogTest {
    @Test
    void testPojo() {
        final TurboLog turboLog = TurboLog
                .builder()
                .timestamp(System.currentTimeMillis())
                .throwable(new NullPointerException())
                .marker(null)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted message: {}")
                .logLevel(LogLevel.ERROR)
                .build();

        Assertions.assertTrue(turboLog.getTimestamp() > 0);
        Assertions.assertNotNull(turboLog.getThrowable());
        Assertions.assertNull(turboLog.getMarker());
        Assertions.assertNotNull(turboLog.getLogger());
        Assertions.assertNotNull(turboLog.getFormattedMessage());
        Assertions.assertNotNull(turboLog.getLogLevel());
        Assertions.assertNull(turboLog.getArgumentArray());
        Assertions.assertNotEquals(0, turboLog.hashCode());
        Assertions.assertNotNull(turboLog.toString());

        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(turboLog.getLogLevel())
                .formattedMessage(turboLog.getFormattedMessage())
                .logger(turboLog.getLogger())
                .marker(turboLog.getMarker())
                .throwable(turboLog.getThrowable())
                .timestamp(turboLog.getTimestamp())
                .argumentArray(turboLog.getArgumentArray())
                .build();
        Assertions.assertEquals(turboLog, turboLog1);
    }
}
