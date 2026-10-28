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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class CustomEventTest {
    @Test
    void testPojo() {
        final AppEvent appEvent = new AppEvent();
        appEvent.setValue("value");

        final CustomEvent customEvent = new CustomEvent();
        customEvent.setEvent(appEvent);
        Assertions.assertNotNull(customEvent.getEvent());
        Assertions.assertEquals(appEvent, customEvent.getEvent());

        final CustomEvent customEvent1 = new CustomEvent();
        customEvent1.setEvent(appEvent);

        Assertions.assertEquals(customEvent, customEvent1);
        Assertions.assertEquals(customEvent.hashCode(), customEvent1.hashCode());
        Assertions.assertEquals(customEvent.toString(), customEvent1.toString());

        customEvent.reset();
        Assertions.assertNull(customEvent.getEvent());
    }

    public static class AppEvent {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof AppEvent)) {
                return false;
            }

            final AppEvent appEvent = (AppEvent) o;
            return Objects.equals(value, appEvent.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "AppEvent{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }
}
