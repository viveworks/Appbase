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

import com.viveworks.appbase.core.AppbaseException;

/**
 * Checked base exception for recoverable failures reported by service-layer APIs.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AppbaseServiceException extends AppbaseException {
    private static final long serialVersionUID = 8341025863680024104L;

    /**
     * Constructs an empty checked service failure; neither a message nor cause is recorded.
     */
    public AppbaseServiceException() {
    }

    /**
     * Records a detail message for a recoverable service-layer failure.
     *
     * @param message diagnostic detail retained by the exception
     */
    public AppbaseServiceException(final String message) {
        super(message);
    }

    /**
     * Preserves both the diagnostic message and the cause of a recoverable service-layer failure.
     *
     * @param message diagnostic detail
     * @param cause originating failure; may be null
     */
    public AppbaseServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Wraps the cause of a recoverable service-layer failure; the superclass derives the detail message.
     *
     * @param cause originating failure; may be null
     */
    public AppbaseServiceException(final Throwable cause) {
        super(cause);
    }

    /**
     * Configures checked service failure with explicit suppression and stack-trace policies.
     *
     * @param message diagnostic detail
     * @param cause originating failure; may be null
     * @param enableSuppression whether subsequent failures can be attached as suppressed exceptions
     * @param writableStackTrace whether construction captures the current stack
     */
    public AppbaseServiceException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
