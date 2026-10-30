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
import com.viveworks.appbase.core.service.log.common.TurboLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MediatorEventTest {
    @Test
    void testPojo() {
        final MediatorEvent mediatorEvent = new MediatorEvent();
        mediatorEvent.reset();
        Assertions.assertNull(mediatorEvent.getMediatorEventType());
        Assertions.assertNotNull(mediatorEvent.getLogEvent());
        Assertions.assertNotNull(mediatorEvent.getTurboLogEvent());
        Assertions.assertNotNull(mediatorEvent.getCustomEvent());

        final LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setMessage("Message");
        loggingEvent.setThreadName(Thread.currentThread().getName());
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setLevel(Level.INFO);
        mediatorEvent.setMediatorEventType(LogEvent.class);
        mediatorEvent.getLogEvent().setLoggingEvent(loggingEvent);
        Assertions.assertEquals(loggingEvent, mediatorEvent.getLogEvent().getLoggingEvent());

        final MediatorEvent mediatorEvent1 = new MediatorEvent();
        mediatorEvent1.setMediatorEventType(mediatorEvent.getMediatorEventType());
        mediatorEvent1.getLogEvent().setLoggingEvent(loggingEvent);
        Assertions.assertEquals(mediatorEvent, mediatorEvent1);
        Assertions.assertEquals(mediatorEvent.hashCode(), mediatorEvent1.hashCode());
        Assertions.assertEquals(mediatorEvent.toString(), mediatorEvent1.toString());

        mediatorEvent.reset();
        Assertions.assertNull(mediatorEvent.getMediatorEventType());
        Assertions.assertNotNull(mediatorEvent.getLogEvent());
        Assertions.assertNotNull(mediatorEvent.getTurboLogEvent());
        Assertions.assertNotNull(mediatorEvent.getCustomEvent());

        final TurboLog turboLog = TurboLog.builder().build();
        mediatorEvent.getTurboLogEvent().setTurboLog(turboLog);
        Assertions.assertEquals(turboLog, mediatorEvent.getTurboLogEvent().getTurboLog());
        final MediatorEvent mediatorEvent2 = new MediatorEvent();
        mediatorEvent2.setMediatorEventType(TurboLogEvent.class);
        mediatorEvent2.getTurboLogEvent().setTurboLog(mediatorEvent.getTurboLogEvent().getTurboLog());
        Assertions.assertEquals(TurboLogEvent.class, mediatorEvent2.getMediatorEventType());
        Assertions.assertEquals(turboLog, mediatorEvent2.getTurboLogEvent().getTurboLog());

        final MediatorEvent mediatorEvent3 = new MediatorEvent(mediatorEvent2);
        Assertions.assertEquals(mediatorEvent2, mediatorEvent3);
        Assertions.assertEquals(mediatorEvent2.hashCode(), mediatorEvent3.hashCode());
        Assertions.assertEquals(mediatorEvent2.toString(), mediatorEvent3.toString());

        mediatorEvent.reset();
        final CustomEventTest.AppEvent appEvent = new CustomEventTest.AppEvent();
        appEvent.setValue("value");
        mediatorEvent.getCustomEvent().setEvent(appEvent);
        Assertions.assertEquals(appEvent, mediatorEvent.getCustomEvent().getEvent());
        final MediatorEvent mediatorEvent4 = new MediatorEvent();
        mediatorEvent4.setMediatorEventType(CustomMediatorEventType.class);
        mediatorEvent4.getCustomEvent().setEvent(appEvent);
        Assertions.assertEquals(CustomMediatorEventType.class, mediatorEvent4.getMediatorEventType());
        Assertions.assertEquals(appEvent, mediatorEvent4.getCustomEvent().getEvent());
    }

    private static class CustomMediatorEventType implements MediatorEventType {
    }
}
