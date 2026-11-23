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

package com.viveworks.appbase.client.service.aerospike.accessor;

import com.aerospike.client.AerospikeException;
import com.viveworks.appbase.client.core.AppbaseClientException;

import java.util.function.Function;

/**
 * Exception thrown by {@link AbstractAerospikeAccessor}. Also serves as the shared error-mapping
 * target for the reactive and synchronous service-acquired Aerospike accessors, via {@link
 * #map(Throwable, Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AerospikeAccessorException extends AppbaseClientException {
    private static final long serialVersionUID = 1978776267306877058L;

    public AerospikeAccessorException() {
    }

    public AerospikeAccessorException(final String message) {
        super(message);
    }

    public AerospikeAccessorException(
            final String message,
            final Throwable cause)
    {
        super(message, cause);
    }

    public AerospikeAccessorException(final Throwable cause) {
        super(cause);
    }

    public AerospikeAccessorException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Map a raised {@link Throwable} to {@link AerospikeAccessorException} when it originates
     * from the Aerospike driver ({@link AerospikeException}); any other throwable is delegated to
     * {@code fallback} (e.g. the accessor's inherited default mapping), so unrelated failures
     * (validation, programming errors) are not mislabeled as Aerospike accessor failures.
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied when {@code throwable} is not an {@link AerospikeException}
     * @return {@code throwable} wrapped in {@link AerospikeAccessorException}, or the result of
     *     {@code fallback}
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        if (throwable instanceof AerospikeException) {
            return new AerospikeAccessorException(throwable);
        }
        return fallback.apply(throwable);
    }
}
