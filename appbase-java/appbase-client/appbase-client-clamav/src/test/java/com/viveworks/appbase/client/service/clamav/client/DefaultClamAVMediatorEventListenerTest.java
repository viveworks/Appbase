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

package com.viveworks.appbase.client.service.clamav.client;

import com.viveworks.appbase.client.service.clamav.ClamAVClientTestHelper;
import com.viveworks.appbase.client.service.clamav.ClamAVClientTestUtil;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVPingEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVRequestEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVScanPathEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVScanStreamEvent;
import com.viveworks.appbase.client.service.clamav.client.event.DefaultClamAVRequestEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "ClamAVClientServiceTests.enabled", matches = "true")
class DefaultClamAVMediatorEventListenerTest {
    private static Path uninfectedFile;
    private static Path infectedFile;
    private static DefaultClamAVMediatorEventListener defaultClamAVMediatorEventListener;

    @BeforeAll
    static void setUp() {
        uninfectedFile = ClamAVClientTestUtil.writeUninfectedFile();
        infectedFile = ClamAVClientTestUtil.writeInfectedFile();
        defaultClamAVMediatorEventListener = ClamAVClientTestHelper.defaultClamAVMediatorEventListener();
    }

    @AfterAll
    static void tearDown() {
        if (defaultClamAVMediatorEventListener != null) {
            defaultClamAVMediatorEventListener.shutdown();
        }

        if (uninfectedFile != null) {
            uninfectedFile.toFile().delete();
        }

        if (infectedFile != null) {
            infectedFile.toFile().delete();
        }
    }

    @Test
    void testPingEvent() {
        final AtomicInteger successCounter = new AtomicInteger(0);
        final AtomicInteger failureCounter = new AtomicInteger(0);
        final ClamAVClientTestUtil.CustomClamAVPingResultHandler customClamAVPingResultHandler =
                new ClamAVClientTestUtil.CustomClamAVPingResultHandler(successCounter, failureCounter);

        final ClamAVRequestEvent clamAVRequestEvent = new DefaultClamAVRequestEvent();
        clamAVRequestEvent.init(ClamAVPingEvent.class);
        clamAVRequestEvent
                .getClamAVPingEvent()
                .init(customClamAVPingResultHandler);
        handleClamAVRequestEvent(clamAVRequestEvent);

        Assertions.assertTrue(successCounter.get() > 0);
        Assertions.assertEquals(0, failureCounter.get());
    }

    @Test
    void testScanUninfectedPathEvent() {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);
        final ClamAVClientTestUtil.CustomClamAVScanResultHandler clamAVScanResultHandler =
                new ClamAVClientTestUtil.CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter);

        final ClamAVRequestEvent clamAVRequestEvent = new DefaultClamAVRequestEvent();
        clamAVRequestEvent.init(ClamAVScanPathEvent.class);
        clamAVRequestEvent
                .getClamAVScanPathEvent()
                .init(uninfectedFile, clamAVScanResultHandler);
        handleClamAVRequestEvent(clamAVRequestEvent);

        Assertions.assertTrue(virusNotFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusFoundCounter.get());
    }

    @Test
    void testScanInfectedPathEvent() {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);
        final ClamAVClientTestUtil.CustomClamAVScanResultHandler clamAVScanResultHandler =
                new ClamAVClientTestUtil.CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter);

        final ClamAVRequestEvent clamAVRequestEvent = new DefaultClamAVRequestEvent();
        clamAVRequestEvent.init(ClamAVScanPathEvent.class);
        clamAVRequestEvent
                .getClamAVScanPathEvent()
                .init(infectedFile, clamAVScanResultHandler);
        handleClamAVRequestEvent(clamAVRequestEvent);

        Assertions.assertTrue(virusFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusNotFoundCounter.get());
    }

    @Test
    void testScanUninfectedStreamEvent() {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);
        final ClamAVClientTestUtil.CustomClamAVScanResultHandler clamAVScanResultHandler =
                new ClamAVClientTestUtil.CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ClamAVClientTestUtil.UNINFECTED_BYTES);

        final ClamAVRequestEvent clamAVRequestEvent = new DefaultClamAVRequestEvent();
        clamAVRequestEvent.init(ClamAVScanStreamEvent.class);
        clamAVRequestEvent
                .getClamAVScanStreamEvent()
                .init(inputStream, false, clamAVScanResultHandler);
        handleClamAVRequestEvent(clamAVRequestEvent);

        Assertions.assertTrue(virusNotFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusFoundCounter.get());
    }

    @Test
    void testScanInfectedStreamEvent() {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);
        final ClamAVClientTestUtil.CustomClamAVScanResultHandler clamAVScanResultHandler =
                new ClamAVClientTestUtil.CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ClamAVClientTestUtil.VIRUS_BYTES);

        final ClamAVRequestEvent clamAVRequestEvent = new DefaultClamAVRequestEvent();
        clamAVRequestEvent.init(ClamAVScanStreamEvent.class);
        clamAVRequestEvent
                .getClamAVScanStreamEvent()
                .init(inputStream, false, clamAVScanResultHandler);
        handleClamAVRequestEvent(clamAVRequestEvent);

        Assertions.assertTrue(virusFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusNotFoundCounter.get());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void handleClamAVRequestEvent(final ClamAVRequestEvent clamAVRequestEvent) {
        final MediatorEvent mediatorEvent = new MediatorEvent();
        mediatorEvent.setMediatorEventType(ClamAVRequestEvent.class);
        mediatorEvent.getCustomEvent().setEvent(clamAVRequestEvent);

        try {
            defaultClamAVMediatorEventListener.onMediatorEvent(mediatorEvent);
            Thread.sleep(ClamAVClientTestUtil.TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            Assertions.fail(ex);
        }
    }
}
