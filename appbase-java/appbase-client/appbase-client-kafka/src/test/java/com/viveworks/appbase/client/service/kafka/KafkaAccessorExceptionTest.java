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

package com.viveworks.appbase.client.service.kafka;

import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link KafkaAccessorException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class KafkaAccessorExceptionTest {

    @Test
    void mapsKafkaExceptionToKafkaAccessorException() {
        final KafkaException kafkaException = new KafkaException("boom");
        final Throwable mapped = KafkaAccessorException.map(
                kafkaException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(KafkaAccessorException.class, mapped);
        assertSame(kafkaException, mapped.getCause());
    }

    @Test
    void delegatesNonKafkaThrowableToFallback() {
        final RuntimeException foreign = new RuntimeException("x");
        final IllegalStateException fallbackResult = new IllegalStateException("fallback");
        final Throwable mapped = KafkaAccessorException.map(foreign, throwable -> fallbackResult);
        assertSame(fallbackResult, mapped);
    }
}
