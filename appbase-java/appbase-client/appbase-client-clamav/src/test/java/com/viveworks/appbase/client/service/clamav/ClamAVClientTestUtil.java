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

package com.viveworks.appbase.client.service.clamav;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.service.clamav.client.ClamAVClient;
import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVPingResultHandler;
import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVScanResultHandler;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVPingResult;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVScanResult;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ClamAVClientTestUtil {
    // http://www.eicar.org/86-0-Intended-use.html
    public static final String VIRUS =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    public static final byte[] VIRUS_BYTES = VIRUS.getBytes(StandardCharsets.US_ASCII);
    public static final String UNINFECTED_STRING = "abcdefghijklmnopqrstuvwxyz";
    public static final byte[] UNINFECTED_BYTES = UNINFECTED_STRING.getBytes(StandardCharsets.UTF_8);
    public static final int TEST_SLEEP_MILLIS = 15_000;

    private ClamAVClientTestUtil() {
    }

    public static void clamAVClientServiceTest(
            final ClamAVClientService clamAVClientService,
            final ClamAVClientIntegrationService clamAVClientIntegrationService)
    {
        clamAVClientServiceTestImpl(clamAVClientService, clamAVClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        clamAVClientIntegrationService.shutdown();
        clamAVClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
            @Override
            public void onSuccess() {
                repaired.setReference(true);
            }

            @Override
            public void onFailure() {
                repaired.setReference(false);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                throw new AppbaseRuntimeException(throwable);
            }
        });
        Assertions.assertTrue(repaired.getReference());

        clamAVClientServiceTestImpl(clamAVClientService, clamAVClientIntegrationService);
        clamAVClientService.shutdown();

        Assertions.assertNull(clamAVClientIntegrationService.getClient());
        Assertions.assertFalse(clamAVClientIntegrationService.isHealthy());
    }

    public static void clamAVClientServiceTestImpl(
            final ClamAVClientService clamAVClientService,
            final ClamAVClientIntegrationService clamAVClientIntegrationService)
    {
        Assertions.assertNotNull(clamAVClientService);

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = clamAVClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(clamAVClientIntegrationService);

        final ClamAVClient clamAVClient = clamAVClientIntegrationService.getClient();
        pingTest(clamAVClient);
        scanPathTest(clamAVClient);
        scanStreamTest(clamAVClient);

        Assertions.assertTrue(clamAVClientIntegrationService.isHealthy());
        Assertions.assertTrue(clamAVClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(ClamAVClient.class, clamAVClientIntegrationService.getClientType());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void pingTest(final ClamAVClient clamAVClient) {
        Assertions.assertNotNull(clamAVClient);

        final AtomicInteger successCounter = new AtomicInteger(0);
        final AtomicInteger failureCounter = new AtomicInteger(0);

        try {
            clamAVClient.ping(new CustomClamAVPingResultHandler(successCounter, failureCounter));
            Thread.sleep(TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            Assertions.fail(ex);
        }

        Assertions.assertTrue(successCounter.get() > 0);
        Assertions.assertEquals(0, failureCounter.get());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void scanPathTest(final ClamAVClient clamAVClient) {
        Assertions.assertNotNull(clamAVClient);

        scanUninfectedFileTest(clamAVClient);
        scanInfectedFileTest(clamAVClient);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void scanUninfectedFileTest(final ClamAVClient clamAVClient) {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);

        final Path uninfectedFile = writeUninfectedFile();
        try {
            clamAVClient.scan(uninfectedFile, new CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter));
            Thread.sleep(TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            Assertions.fail(ex);
        } finally {
            try {
                Files.deleteIfExists(uninfectedFile);
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to delete " + uninfectedFile, ioe);
            }
        }

        Assertions.assertTrue(virusNotFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusFoundCounter.get());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void scanInfectedFileTest(final ClamAVClient clamAVClient) {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);

        final Path infectedFile = writeInfectedFile();
        try {
            clamAVClient.scan(infectedFile, new CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter));
            Thread.sleep(TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            Assertions.fail(ex);
        } finally {
            try {
                Files.deleteIfExists(infectedFile);
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to delete " + infectedFile, ioe);
            }
        }

        Assertions.assertTrue(virusFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusNotFoundCounter.get());
    }

    public static Path writeUninfectedFile() {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 26; ++i) {
            builder.append(i);
        }

        try {
            final Path tempFile = Files.createTempFile(ClamAVClientTestUtil.class.getSimpleName(), null);
            Files.writeString(tempFile, builder.toString());
            tempFile.toFile().deleteOnExit();
            return tempFile;
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to write temp file", ioe);
        }
    }

    public static Path writeInfectedFile() {
        try {
            final Path tempFile = Files.createTempFile(ClamAVClientTestUtil.class.getSimpleName(), null);
            Files.writeString(tempFile, VIRUS);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to write temp file", ioe);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void scanStreamTest(final ClamAVClient clamAVClient) {
        Assertions.assertNotNull(clamAVClient);

        scanUninfectedStreamTest(clamAVClient);
        scanInfectedStreamTest(clamAVClient);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void scanUninfectedStreamTest(final ClamAVClient clamAVClient) {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);

        final ByteArrayInputStream uninfectedStream = new ByteArrayInputStream(UNINFECTED_BYTES);
        try {
            clamAVClient.scan(
                    uninfectedStream, false, new CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter));
            Thread.sleep(TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            Assertions.fail(ex);
        }

        Assertions.assertTrue(virusNotFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusFoundCounter.get());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void scanInfectedStreamTest(final ClamAVClient clamAVClient) {
        final AtomicInteger virusFoundCounter = new AtomicInteger(0);
        final AtomicInteger virusNotFoundCounter = new AtomicInteger(0);

        final ByteArrayInputStream infectedStream = new ByteArrayInputStream(VIRUS_BYTES);
        try {
            clamAVClient.scan(
                    infectedStream, false, new CustomClamAVScanResultHandler(virusFoundCounter, virusNotFoundCounter));
            Thread.sleep(TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            Assertions.fail(ex);
        }

        Assertions.assertTrue(virusFoundCounter.get() > 0);
        Assertions.assertEquals(0, virusNotFoundCounter.get());
    }

    public static final class CustomClamAVPingResultHandler implements ClamAVPingResultHandler {
        private final AtomicInteger successCounter;
        private final AtomicInteger failureCounter;

        public CustomClamAVPingResultHandler(
                final AtomicInteger successCounter,
                final AtomicInteger failureCounter)
        {
            this.successCounter = successCounter;
            this.failureCounter = failureCounter;
        }

        @Override
        public void complete(final ClamAVPingResult result) {
            Assertions.assertNotNull(result);

            if (result.isPingSuccess()) {
                successCounter.incrementAndGet();
            } else {
                failureCounter.incrementAndGet();
            }
        }

        @Override
        public void completeWithException(final Throwable throwable) {
            Assertions.fail(throwable);
        }
    }

    public static final class CustomClamAVScanResultHandler implements ClamAVScanResultHandler {
        private final AtomicInteger virusFoundCounter;
        private final AtomicInteger virusNotFoundCounter;

        public CustomClamAVScanResultHandler(
                final AtomicInteger virusFoundCounter,
                final AtomicInteger virusNotFoundCounter)
        {
            this.virusFoundCounter = virusFoundCounter;
            this.virusNotFoundCounter = virusNotFoundCounter;
        }

        @Override
        public void complete(final ClamAVScanResult result) {
            Assertions.assertNotNull(result);

            if (result.isVirusFound()) {
                virusFoundCounter.incrementAndGet();
            } else {
                virusNotFoundCounter.incrementAndGet();
            }
        }

        @Override
        public void completeWithException(final Throwable throwable) {
            Assertions.fail(throwable);
        }
    }
}
