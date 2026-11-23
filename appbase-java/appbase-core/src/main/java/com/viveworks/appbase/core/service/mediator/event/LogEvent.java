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

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.viveworks.appbase.core.util.Resettable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;

/**
 * Represents a log event.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
public final class LogEvent implements MediatorEventType, Resettable {
    private ILoggingEvent loggingEvent;

    public ILoggingEvent getLoggingEvent() {
        return loggingEvent;
    }

    public void setLoggingEvent(final ILoggingEvent loggingEvent) {
        this.loggingEvent = loggingEvent;
    }

    /**
     * Reset the state.
     */
    @Override
    public void reset() {
        loggingEvent = null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LogEvent)) {
            return false;
        }

        final LogEvent that = (LogEvent) o;
        return Objects.equals(loggingEvent, that.loggingEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loggingEvent);
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "loggingEvent=" + loggingEvent +
                '}';
    }
}
