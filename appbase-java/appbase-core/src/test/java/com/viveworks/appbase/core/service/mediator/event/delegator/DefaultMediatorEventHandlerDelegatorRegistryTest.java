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

package com.viveworks.appbase.core.service.mediator.event.delegator;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.event.LogEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

/**
 * @author Navaneeth Mysore G
 */
class DefaultMediatorEventHandlerDelegatorRegistryTest {
    private static LogService logService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testRegistry() {
        final DefaultMediatorEventDelegatorRegistry defaultMediatorEventDelegatorRegistry =
                new DefaultMediatorEventDelegatorRegistry();
        defaultMediatorEventDelegatorRegistry.setLogService(logService);

        final Collection<MediatorEventListener> unmodifiableMediatorEventListeners1 =
                defaultMediatorEventDelegatorRegistry.getMediatorEventListeners(LogEvent.class);
        Assertions.assertNull(unmodifiableMediatorEventListeners1);

        defaultMediatorEventDelegatorRegistry.registerMediatorEventListener(
                LogEvent.class, new MediatorEventListenerImpl1());
        final Collection<MediatorEventListener> unmodifiableMediatorEventListeners2 =
                defaultMediatorEventDelegatorRegistry.getMediatorEventListeners(LogEvent.class);
        Assertions.assertNotNull(unmodifiableMediatorEventListeners2);

        defaultMediatorEventDelegatorRegistry.registerMediatorEventListener(
                LogEvent.class,
                new MediatorEventListenerImpl2());
        defaultMediatorEventDelegatorRegistry.unregisterMediatorEventListener(
                LogEvent.class,
                MediatorEventListenerImpl1.class);
        final Collection<MediatorEventListener> unmodifiableMediatorEventListeners3 =
                defaultMediatorEventDelegatorRegistry.getMediatorEventListeners(LogEvent.class);
        Assertions.assertNotNull(unmodifiableMediatorEventListeners3);
        Assertions.assertEquals(1, unmodifiableMediatorEventListeners3.size());

        defaultMediatorEventDelegatorRegistry.clearMediatorEventListeners();
        final Collection<MediatorEventListener> unmodifiableMediatorEventListeners4 =
                defaultMediatorEventDelegatorRegistry.getMediatorEventListeners(LogEvent.class);
        Assertions.assertNull(unmodifiableMediatorEventListeners4);
    }

    private static class MediatorEventListenerImpl1 implements MediatorEventListener {
        @Override
        public void onMediatorEvent(final MediatorEvent mediatorEvent) {
            // no-op
        }
    }

    private static class MediatorEventListenerImpl2 implements MediatorEventListener {
        @Override
        public void onMediatorEvent(final MediatorEvent mediatorEvent) {
            // no-op
        }
    }
}
