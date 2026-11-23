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

import com.viveworks.appbase.client.core.AppbaseClientException;
import org.apache.kafka.common.KafkaException;

import java.util.function.Function;

/**
 * Thrown when a Kafka driver failure (a {@link KafkaException}) surfaces through a reactive or
 * synchronous Kafka accessor. Lives in the parent {@code kafka} package because the mapping is
 * shared by both the producer and the consumer side. The original driver exception is preserved
 * as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class KafkaAccessorException extends AppbaseClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Create with a cause only.
     *
     * @param cause Original driver failure
     */
    public KafkaAccessorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Create with a message and cause.
     *
     * @param message Message
     * @param cause Original driver failure
     */
    public KafkaAccessorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Shared Kafka driver failure mapping used by both the producer and consumer accessors (sync
     * and reactive): a {@link KafkaException} maps to a new {@link KafkaAccessorException}
     * wrapping it; every other throwable is delegated to {@code fallback} (typically the calling
     * accessor's inherited default mapping).
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied to non-Kafka throwables
     * @return Mapped throwable
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        if (throwable instanceof KafkaException) {
            return new KafkaAccessorException(throwable);
        }
        return fallback.apply(throwable);
    }
}
