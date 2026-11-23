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

package com.viveworks.appbase.core.service.tracer.reporter;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.DefaultTracerService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import io.jaegertracing.internal.JaegerSpan;
import io.opentracing.Span;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LoggingJaegerReporterTest {
    private static LogService logService;
    private static MetricService metricService;
    private static LoggingJaegerReporter loggingJaegerReporter;
    private static DefaultTracerService defaultTracingService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        loggingJaegerReporter = new LoggingJaegerReporter(logService);
        defaultTracingService = CoreTestFixtures.getDefaultTracingService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (defaultTracingService != null) {
            defaultTracingService.shutdown();
        }

        if (loggingJaegerReporter != null) {
            loggingJaegerReporter.close();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testReporter() {
        final Span span = defaultTracingService
                .getTracer()
                .buildSpan(getClass().getSimpleName() + ".operation")
                .start();
        span.finish(TimeUnit.SECONDS.toMicros(2));
        final String spanId = span.context().toSpanId();
        Assertions.assertNotNull(spanId);
        final String traceId = span.context().toTraceId();
        Assertions.assertNotNull(traceId);

        Assertions.assertDoesNotThrow(() -> loggingJaegerReporter.report((JaegerSpan) span));
        loggingJaegerReporter.close();
        Assertions.assertDoesNotThrow(() -> loggingJaegerReporter.report((JaegerSpan) span));
    }
}
