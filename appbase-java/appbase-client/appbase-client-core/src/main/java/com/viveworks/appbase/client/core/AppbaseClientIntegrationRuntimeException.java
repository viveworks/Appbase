/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
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

package com.viveworks.appbase.client.core;

/**
 * Base runtime exceptions for all exceptions thrown by implementations of {@link AppbaseClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseClientIntegrationRuntimeException extends AppbaseClientRuntimeException {
    private static final long serialVersionUID = 1388948697925290720L;

    /** Creates an AppbaseClientIntegrationRuntimeException without a detail message or cause for an unchecked integration failure. */
    public AppbaseClientIntegrationRuntimeException() {
    }

    /** Creates an AppbaseClientIntegrationRuntimeException with diagnostic text for an unchecked integration failure.
     * @param message diagnostic message, which may be {@code null}
     */
    public AppbaseClientIntegrationRuntimeException(final String message) {
        super(message);
    }

    /** Creates an AppbaseClientIntegrationRuntimeException retaining contextual text and its cause for an unchecked integration failure.
     * @param message diagnostic message, which may be {@code null}
     * @param cause originating failure, which may be {@code null}
     */
    public AppbaseClientIntegrationRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /** Creates an AppbaseClientIntegrationRuntimeException from an originating failure for an unchecked integration failure.
     * @param cause originating failure, which may be {@code null}
     */
    public AppbaseClientIntegrationRuntimeException(final Throwable cause) {
        super(cause);
    }

    /** Creates an AppbaseClientIntegrationRuntimeException with explicit throwable mechanics for an unchecked integration failure.
     * @param message diagnostic message, which may be {@code null}
     * @param cause originating failure, which may be {@code null}
     * @param enableSuppression whether suppressed exceptions can be attached
     * @param writableStackTrace whether construction captures a stack trace
     */
    public AppbaseClientIntegrationRuntimeException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
