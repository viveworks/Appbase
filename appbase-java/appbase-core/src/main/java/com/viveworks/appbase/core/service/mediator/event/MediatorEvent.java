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

import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventHandlerDelegator;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventListener;
import com.viveworks.appbase.core.util.Resettable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;

/**
 * Represents a mediato event. The {@link MediatorEventListener} must be registered with
 * {@link MediatorEventHandlerDelegator} for specific {@link #mediatorEventType} types / class.
 * 
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
public final class MediatorEvent implements Resettable {
    private Class<? extends MediatorEventType> mediatorEventType;
    private final LogEvent logEvent = new LogEvent();
    private final TurboLogEvent turboLogEvent = new TurboLogEvent();
    private final CustomEvent customEvent = new CustomEvent();

    public MediatorEvent() {
    }

    public MediatorEvent(final MediatorEvent mediatorEvent) {
        copy(mediatorEvent);
    }

    /**
     * Copy constructor.
     *
     * @param mediatorEvent Object to copy from
     */
    public void copy(final MediatorEvent mediatorEvent) {
        if (mediatorEvent != null) {
            mediatorEventType = mediatorEvent.mediatorEventType;
            logEvent.setLoggingEvent(mediatorEvent.logEvent.getLoggingEvent());
            turboLogEvent.setTurboLog(mediatorEvent.turboLogEvent.getTurboLog());
            customEvent.setEvent(mediatorEvent.customEvent.getEvent());
        }
    }

    public Class<? extends MediatorEventType> getMediatorEventType() {
        return mediatorEventType;
    }

    public void setMediatorEventType(final Class<? extends MediatorEventType> mediatorEventType) {
        this.mediatorEventType = mediatorEventType;
    }

    public LogEvent getLogEvent() {
        return logEvent;
    }

    public TurboLogEvent getTurboLogEvent() {
        return turboLogEvent;
    }

    public CustomEvent getCustomEvent() {
        return customEvent;
    }

    /**
     * Reset the state.
     */
    @Override
    public void reset() {
        mediatorEventType = null;
        logEvent.reset();
        turboLogEvent.reset();
        customEvent.reset();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof MediatorEvent)) {
            return false;
        }

        final MediatorEvent that = (MediatorEvent) o;
        return Objects.equals(mediatorEventType, that.mediatorEventType) &&
                Objects.equals(logEvent, that.logEvent) &&
                Objects.equals(turboLogEvent, that.turboLogEvent) &&
                Objects.equals(customEvent, that.customEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediatorEventType, logEvent, turboLogEvent, customEvent);
    }

    @Override
    public String toString() {
        return "MediatorEvent{" +
                "mediatorEventType=" + mediatorEventType +
                ", logEvent=" + logEvent +
                ", turboLogEvent=" + turboLogEvent +
                ", customEvent=" + customEvent +
                '}';
    }
}
