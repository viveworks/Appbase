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

package com.viveworks.appbase.core.service.mediator.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LogEventTest {
    @Test
    void testPojo() {
        final LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setMessage("Message");
        loggingEvent.setThreadName(Thread.currentThread().getName());
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setLevel(Level.INFO);

        final LogEvent logEvent = new LogEvent();
        logEvent.setLoggingEvent(loggingEvent);
        Assertions.assertNotNull(logEvent.getLoggingEvent());
        Assertions.assertEquals(loggingEvent, logEvent.getLoggingEvent());

        final LogEvent logEvent1 = new LogEvent();
        logEvent1.setLoggingEvent(loggingEvent);

        Assertions.assertEquals(logEvent, logEvent1);
        Assertions.assertEquals(logEvent.hashCode(), logEvent1.hashCode());
        Assertions.assertEquals(logEvent.toString(), logEvent1.toString());
    }
}
