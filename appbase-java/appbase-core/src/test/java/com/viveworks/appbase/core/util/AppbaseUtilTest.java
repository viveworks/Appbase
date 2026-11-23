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

package com.viveworks.appbase.core.util;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseUtilTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
    }

    @AfterAll
    static void tearDown() {
        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testNotNull() {
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> AppbaseUtil.notNull(null, "fieldName"));
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.notNull(new Object(), "object"));
    }

    @Test
    void testNotEmptyArray() {
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> AppbaseUtil.notEmpty((Object[]) null, "fieldName"));
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> AppbaseUtil.notEmpty(new Object[] {}, "fieldName"));
        Assertions.assertDoesNotThrow(
                () -> AppbaseUtil.notEmpty(new String[] {"a", "b" }, "fieldName"));
    }

    @Test
    void testNotEmptyCollection() {
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> AppbaseUtil.notEmpty(new ArrayList<>(), "fieldName"));
        Assertions.assertDoesNotThrow(
                () -> AppbaseUtil.notEmpty(List.of("a"), "fieldName"));
    }

    @Test
    void testNotEmptyMap() {
        Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> AppbaseUtil.notEmpty(new HashMap<>(), "fieldName"));
        Assertions.assertDoesNotThrow(
                () -> AppbaseUtil.notEmpty(Map.of("a", "b"), "fieldName"));
    }

    @Test
    void testIsBlank() {
        Assertions.assertTrue(AppbaseUtil.isBlank(null));
        Assertions.assertTrue(AppbaseUtil.isBlank(""));
        Assertions.assertTrue(AppbaseUtil.isBlank("  "));
        Assertions.assertFalse(AppbaseUtil.isBlank("  a  "));
    }

    @Test
    void testNotBlank() {
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> AppbaseUtil.notBlank("  ", "fieldName"));
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.notBlank("  a  ", "fieldName"));
    }

    @Test
    void testPositive() {
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> AppbaseUtil.positive(0, "fieldsName"));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> AppbaseUtil.positive(-1, "fieldsName"));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> AppbaseUtil.positive(-1.2, "fieldsName"));
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.positive(1, "fieldName"));
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.positive(1.2, "fieldName"));
    }

    @Test
    void testPositiveOrZero() {
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> AppbaseUtil.positiveOrZero(-1, "fieldName"));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> AppbaseUtil.positiveOrZero(-1.2, "fieldName"));
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.positiveOrZero(0, "fieldName"));
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.positiveOrZero(1, "fieldName"));
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.positiveOrZero(1.2, "fieldName"));
    }

    @Test
    void testDefaultUncaughtExceptionHandler() {
        final Logger logger = LoggerFactory.getLogger(getClass());
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler1 =
                AppbaseUtil.defaultUncaughtExceptionHandler(logger);
        Assertions.assertNotNull(uncaughtExceptionHandler1);
        Assertions.assertDoesNotThrow(() ->
                uncaughtExceptionHandler1.uncaughtException(Thread.currentThread(), new AppbaseRuntimeException("e")));

        final AtomicReference<Logger> loggerRef = new AtomicReference<>(logger);
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler2 =
                AppbaseUtil.defaultUncaughtExceptionHandler(loggerRef);
        Assertions.assertNotNull(uncaughtExceptionHandler2);
        Assertions.assertDoesNotThrow(() ->
                uncaughtExceptionHandler2.uncaughtException(Thread.currentThread(), new AppbaseRuntimeException("e")));
    }

    @Test
    void testNewThreadFactory() {
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> { throw new RuntimeException(e); };
        final ThreadFactory threadFactory = AppbaseUtil.newThreadFactory(
                ThreadFactoryConfiguration
                        .builder()
                        .nameFormat("appbaseUtil-test-%d")
                        .daemon(false)
                        .enableThreadAffinity(false)
                        .build(),
                uncaughtExceptionHandler);
        final Thread thread = threadFactory.newThread(() -> { });
        Assertions.assertNotNull(thread);
    }

    @Test
    void testCsvToSet() {
        final String csv = "a, b, c, 1";
        final Set<String> values = new HashSet<>();
        AppbaseUtil.csvToSet(csv, values);
        Assertions.assertFalse(values.isEmpty());
        Assertions.assertEquals(values.size(), 4);

        Assertions.assertTrue(values.contains("a"));
        Assertions.assertTrue(values.contains("b"));
        Assertions.assertTrue(values.contains("c"));
        Assertions.assertTrue(values.contains("1"));

        Assertions.assertFalse(values.contains("d"));
        Assertions.assertFalse(values.contains("2"));
    }

    @Test
    void testValidate() {
        final ExecutorServiceConfiguration executorServiceConfiguration = ExecutorServiceConfiguration
                .builder()
                .build();
        Assertions.assertDoesNotThrow(() -> AppbaseUtil.validate(validationService, executorServiceConfiguration));
    }

    @Test
    void testShutdownExecutor() {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        Assertions.assertFalse(executorService.isShutdown());
        Assertions.assertFalse(executorService.isTerminated());

        AppbaseUtil.shutdownExecutor(executorService);
        Assertions.assertTrue(executorService.isShutdown());
        Assertions.assertTrue(executorService.isTerminated());
    }

    @Test
    void testShutdownExecutorWithTimeout() {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        Assertions.assertFalse(executorService.isShutdown());
        Assertions.assertFalse(executorService.isTerminated());

        AppbaseUtil.shutdownExecutor(executorService, 5, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(executorService.isShutdown());
        Assertions.assertTrue(executorService.isTerminated());
    }

    @Test
    void testThreadName() {
        final String threadName1 = Thread.currentThread().getName();
        Assertions.assertNotNull(threadName1);

        final String threadName2 = AppbaseUtil.threadName();
        Assertions.assertNotNull(threadName2);

        Assertions.assertEquals(threadName1, threadName2);
    }

    @Test
    void testPropertyOrEnv() {
        final String tmpDir = AppbaseUtil.propertyOrEnv("java.io.tmpdir", "HOME");
        Assertions.assertNotNull(tmpDir);
    }
}
