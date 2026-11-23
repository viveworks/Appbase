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

package com.viveworks.appbase.core;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The base exception for all <i>unchecked exceptions</i> thrown by <code>Appbase</code>. All other unchecked exceptions
 * are a subclass of this class in <code>Appbase</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_EQUALS")
public class AppbaseRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1608407991414548733L;

    private final long timestamp;

    /**
     * Creates an unchecked exception with no detail message or cause.
     */
    public AppbaseRuntimeException() {
        timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an unchecked exception describing the failed Appbase operation.
     *
     * @param message human-readable failure detail; may be {@code null}
     */
    public AppbaseRuntimeException(final String message) {
        super(message);
        timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an unchecked exception that preserves a failure description and its originating cause.
     *
     * @param message human-readable failure detail; may be {@code null}
     * @param cause originating failure; may be {@code null}
     */
    public AppbaseRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
        timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an unchecked exception whose detail message is derived from the originating cause.
     *
     * @param cause originating failure; may be {@code null}
     */
    public AppbaseRuntimeException(final Throwable cause) {
        super(cause);
        timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an unchecked exception with explicit control over suppression and stack-trace collection.
     *
     * @param message human-readable failure detail; may be {@code null}
     * @param cause originating failure; may be {@code null}
     * @param enableSuppression whether suppressed exceptions can be attached
     * @param writableStackTrace whether this exception records a stack trace
     */
    public AppbaseRuntimeException(final String message,
                                   final Throwable cause,
                                   final boolean enableSuppression,
                                   final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
        timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the wall-clock instant at which this exception object was constructed.
     *
     * @return creation time in milliseconds since the Unix epoch
     */
    public long getTimestamp() {
        return timestamp;
    }
}
