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

import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.util.Resettable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;

/**
 * Represents a {@link TurboLog} event.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
public final class TurboLogEvent implements MediatorEventType, Resettable {
    private TurboLog turboLog;

    public TurboLog getTurboLog() {
        return turboLog;
    }

    public void setTurboLog(final TurboLog turboLog) {
        this.turboLog = turboLog;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TurboLogEvent)) {
            return false;
        }

        final TurboLogEvent that = (TurboLogEvent) o;
        return Objects.equals(turboLog, that.turboLog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(turboLog);
    }

    @Override
    public String toString() {
        return "TurboLogEvent{" +
                "turboLog=" + turboLog +
                '}';
    }

    /**
     * Reset the state.
     */
    @Override
    public void reset() {
        this.turboLog = null;
    }
}
