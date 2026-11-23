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

package com.viveworks.appbase.core.util.disruptor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 */
class DefaultExceptionHandlerTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testExceptionHandler() {
        final DefaultExceptionHandler<Object> defaultExceptionHandler = new DefaultExceptionHandler<>();
        defaultExceptionHandler.setLogService(logService);
        defaultExceptionHandler.initMetrics(metricService);

        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            defaultExceptionHandler.handleOnStartException(new IOException());
        });
        Assertions.assertDoesNotThrow(() -> defaultExceptionHandler.handleEventException(
                new IOException(), 100L, null));

        Assertions.assertDoesNotThrow(() -> {
            defaultExceptionHandler.handleOnShutdownException(new IOException());
        });
        Assertions.assertNotNull(defaultExceptionHandler.getLogger());
        Assertions.assertNotNull(defaultExceptionHandler.getErrorCounter());
    }
}
