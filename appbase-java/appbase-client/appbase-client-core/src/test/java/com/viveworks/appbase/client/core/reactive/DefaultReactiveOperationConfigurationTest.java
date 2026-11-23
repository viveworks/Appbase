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

package com.viveworks.appbase.client.core.reactive;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultReactiveOperationConfigurationTest {

    @Test
    void zeroConfigYieldsSensibleDefaults() {
        final ReactiveOperationConfiguration config =
                DefaultReactiveOperationConfiguration.builder().build();

        assertTrue(config.isEnabled());
        assertNull(config.getSchedulerId());
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertTrue(config.isRetryEnabled());
        assertEquals(3, config.getRetryMaxAttempts());
        assertEquals(Duration.ofMillis(100), config.getRetryMinBackoff());
        assertEquals(Duration.ofSeconds(5), config.getRetryMaxBackoff());
        assertEquals(0.5d, config.getRetryJitterFactor());
        assertEquals(256, config.getBackpressureBufferSize());
    }

    @Test
    void everyValueIsOverridable() {
        final ReactiveOperationConfiguration config =
                DefaultReactiveOperationConfiguration.builder()
                        .enabled(false)
                        .schedulerId("my-scheduler")
                        .timeout(Duration.ofSeconds(2))
                        .retryEnabled(false)
                        .retryMaxAttempts(7)
                        .retryMinBackoff(Duration.ofMillis(50))
                        .retryMaxBackoff(Duration.ofSeconds(1))
                        .retryJitterFactor(0.1d)
                        .backpressureBufferSize(1024)
                        .build();

        assertEquals("my-scheduler", config.getSchedulerId());
        assertEquals(Duration.ofSeconds(2), config.getTimeout());
        assertEquals(7, config.getRetryMaxAttempts());
        assertEquals(1024, config.getBackpressureBufferSize());
    }

    @Test
    void crossCuttingKnobsHaveDefaults() {
        final ReactiveOperationConfiguration config =
                DefaultReactiveOperationConfiguration.builder().build();
        assertTrue(config.isMetricsEnabled());
        assertTrue(config.isTraceEnabled());
        assertFalse(config.isExceptionStoreEnabled());
    }

    @Test
    void crossCuttingKnobsOverridable() {
        final ReactiveOperationConfiguration config =
                DefaultReactiveOperationConfiguration.builder()
                        .metricsEnabled(false).traceEnabled(false).exceptionStoreEnabled(true).build();
        assertFalse(config.isMetricsEnabled());
        assertFalse(config.isTraceEnabled());
        assertTrue(config.isExceptionStoreEnabled());
    }
}
