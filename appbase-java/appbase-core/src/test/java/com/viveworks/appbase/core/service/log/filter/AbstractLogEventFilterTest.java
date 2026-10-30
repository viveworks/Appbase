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

package com.viveworks.appbase.core.service.log.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AbstractLogEventFilterTest {
    @Test
    void testDecide() {
        final LoggingEvent loggingEvent = new LoggingEvent();

        final TestLogEventFilterHandler neutral = new TestLogEventFilterHandler(true);
        final TestLogEventFilter testLogEventFilter1 = new TestLogEventFilter(neutral);
        Assertions.assertEquals(FilterReply.NEUTRAL, testLogEventFilter1.decide(loggingEvent));
        Assertions.assertNotNull(testLogEventFilter1.getFilterHandler());

        final TestLogEventFilterHandler deny = new TestLogEventFilterHandler(false);
        final TestLogEventFilter testLogEventFilter2 = new TestLogEventFilter(deny);
        Assertions.assertEquals(FilterReply.DENY, testLogEventFilter2.decide(loggingEvent));
        Assertions.assertNotNull(testLogEventFilter2.getFilterHandler());
    }

    static class TestLogEventFilter extends AbstractLogEventFilter<TestLogEventFilterHandler> {
        TestLogEventFilter(final TestLogEventFilterHandler filterHandler) {
            super(filterHandler);
        }
    }

    static class TestLogEventFilterHandler implements LogEventFilterHandler {
        private final boolean accept;

        TestLogEventFilterHandler(final boolean accept) {
            this.accept = accept;
        }

        @Override
        public boolean accept(final ILoggingEvent loggingEvent) {
            return accept;
        }
    }
}
