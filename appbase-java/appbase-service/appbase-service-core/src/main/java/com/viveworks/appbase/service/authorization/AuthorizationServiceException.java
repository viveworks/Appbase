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
package com.viveworks.appbase.service.authorization;

import com.viveworks.appbase.service.AppbaseServiceException;

/**
 * Checked failure thrown when an authorization check denies a service invocation.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AuthorizationServiceException extends AppbaseServiceException {
    private static final long serialVersionUID = -3368741608580306905L;

    /** Creates an authorization denial without a message or cause. */
    public AuthorizationServiceException() {
    }

    /**
     * Creates an authorization denial with a diagnostic message.
     * @param message diagnostic message
     */
    public AuthorizationServiceException(final String message) {
        super(message);
    }

    /**
     * Creates an authorization denial retaining its diagnostic message and cause.
     * @param message diagnostic message
     * @param cause originating cause
     */
    public AuthorizationServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Wraps the originating authorization failure as the cause.
     * @param cause originating cause
     */
    public AuthorizationServiceException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a denial with explicit suppression and stack-trace policies.
     * @param message diagnostic message
     * @param cause originating cause
     * @param enableSuppression whether suppression is enabled
     * @param writableStackTrace whether the stack trace is writable
     */
    public AuthorizationServiceException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
