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

package com.viveworks.appbase.core.service.mediator.event;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class TurboLogEventTest {
    @Test
    void testPojo() {
        final TurboLog turboLog = TurboLog
                .builder()
                .logLevel(LogLevel.INFO)
                .formattedMessage("fm: {}")
                .logger(LoggerFactory.getLogger(getClass()))
                .marker(null)
                .throwable(new AppbaseException())
                .timestamp(System.currentTimeMillis())
                .argumentArray(null)
                .build();
        final TurboLogEvent turboLogEvent = new TurboLogEvent();
        turboLogEvent.setTurboLog(turboLog);
        Assertions.assertNotNull(turboLogEvent.getTurboLog());
        Assertions.assertEquals(turboLog, turboLogEvent.getTurboLog());

        final TurboLogEvent turboLogEvent1 = new TurboLogEvent();
        turboLogEvent1.setTurboLog(turboLogEvent.getTurboLog());
        Assertions.assertEquals(turboLogEvent, turboLogEvent1);
        Assertions.assertEquals(turboLogEvent.hashCode(), turboLogEvent1.hashCode());
        Assertions.assertEquals(turboLogEvent.toString(), turboLogEvent1.toString());
    }
}
