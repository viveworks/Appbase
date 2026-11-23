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

package com.viveworks.appbase.core.service.log.filter;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LogLevelTest {
    @Test
    void testLogLevel() {
        final LogLevel off = LogLevel.OFF;
        Assertions.assertEquals(Level.OFF_INT, off.getLogbackValue());
        Assertions.assertEquals(Level.OFF, off.getLogbackLevel());
        Assertions.assertEquals(off.getLocationAwareLoggerValue(), -1);
        Assertions.assertEquals(LogLevel.OFF.toString(), off.toString());

        Assertions.assertEquals(LogLevel.OFF, LogLevel.from(Level.OFF));
        Assertions.assertEquals(LogLevel.INFO, LogLevel.from(Level.INFO));
        Assertions.assertEquals(LogLevel.DEBUG, LogLevel.from(Level.DEBUG));
        Assertions.assertEquals(LogLevel.WARN, LogLevel.from(Level.WARN));
        Assertions.assertEquals(LogLevel.ERROR, LogLevel.from(Level.ERROR));
        Assertions.assertEquals(LogLevel.TRACE, LogLevel.from(Level.TRACE));
        Assertions.assertEquals(LogLevel.ALL, LogLevel.from(Level.ALL));
    }
}
