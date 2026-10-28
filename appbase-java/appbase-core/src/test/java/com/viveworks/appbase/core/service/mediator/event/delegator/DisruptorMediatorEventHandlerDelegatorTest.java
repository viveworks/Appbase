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

package com.viveworks.appbase.core.service.mediator.event.delegator;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.TurboLogEvent;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DisruptorMediatorEventHandlerDelegatorTest {
    private static LogService logService;
    private static MetricService metricService;
    private static MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry;
    private static DisruptorMediatorEventHandlerDelegator disruptorMediatorEventHandlerDelegator;
    private static MediatorEvent originalMediatorEvent;
    private static AtomicInteger eventCounter;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        mediatorEventDelegatorRegistry = new DefaultMediatorEventDelegatorRegistry();
        eventCounter = new AtomicInteger(0);
        disruptorMediatorEventHandlerDelegator =
                new DisruptorMediatorEventHandlerDelegator(mediatorEventDelegatorRegistry);
        disruptorMediatorEventHandlerDelegator.setLogService(logService);
        disruptorMediatorEventHandlerDelegator.setMetricService(metricService);

        final TurboLog turboLog = TurboLog
                .builder()
                .logger(LoggerFactory.getLogger(DisruptorMediatorEventHandlerDelegatorTest.class.getName()))
                .marker(MarkerFactory.getMarker("MARKER"))
                .formattedMessage("Formatted message: {}")
                .build();
        originalMediatorEvent = new MediatorEvent();
        originalMediatorEvent.setMediatorEventType(TurboLogEvent.class);
        originalMediatorEvent.getTurboLogEvent().setTurboLog(turboLog);
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (mediatorEventDelegatorRegistry != null) {
            mediatorEventDelegatorRegistry.clearMediatorEventListeners();
        }

        eventCounter.set(0);
    }

    @Test
    void testDelegator() throws InterruptedException {
        final MediatorEventDelegatorRegistry newMediatorEventDelegatorRegistry =
                disruptorMediatorEventHandlerDelegator.getMediatorEventDelegatorRegistry();
        Assertions.assertNotNull(newMediatorEventDelegatorRegistry);

        final TestMediatorEventListener testMediatorEventListener = new TestMediatorEventListener();
        newMediatorEventDelegatorRegistry.registerMediatorEventListener(TurboLogEvent.class, testMediatorEventListener);

        disruptorMediatorEventHandlerDelegator.onEvent(originalMediatorEvent, 10L, false);
        Thread.sleep(1000);
        Assertions.assertEquals(1, eventCounter.get());
    }

    private static class TestMediatorEventListener implements MediatorEventListener {
        @Override
        public void onMediatorEvent(final MediatorEvent mediatorEvent) {
            if (mediatorEvent != null) {
                eventCounter.incrementAndGet();
                Assertions.assertEquals(originalMediatorEvent, mediatorEvent);
            }
        }
    }
}
