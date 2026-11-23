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
package com.viveworks.appbase.service;

/**
 * Checked exception raised when a service store cannot complete a persistence operation.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AppbaseStoreException extends AppbaseServiceException {
    private static final long serialVersionUID = 2700157538907342717L;

    /** Constructs an empty persistence failure without a message or cause. */
    public AppbaseStoreException() {
    }

    /**
     * Records a diagnostic detail for an unsuccessful store operation.
     * @param message diagnostic detail retained by the exception
     */
    public AppbaseStoreException(final String message) {
        super(message);
    }

    /**
     * Preserves the diagnostic message and underlying persistence cause.
     * @param message diagnostic detail
     * @param cause originating storage failure; may be null
     */
    public AppbaseStoreException(
            final String message,
            final Throwable cause) {
        super(message, cause);
    }

    /**
     * Wraps an originating storage failure and derives its message through the superclass.
     * @param cause originating storage failure; may be null
     */
    public AppbaseStoreException(final Throwable cause) {
        super(cause);
    }

    /**
     * Configures a persistence failure with explicit suppression and stack-trace policies.
     * @param message diagnostic detail
     * @param cause originating storage failure; may be null
     * @param enableSuppression whether subsequent failures can be attached as suppressed exceptions
     * @param writableStackTrace whether construction captures the current stack
     */
    public AppbaseStoreException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
