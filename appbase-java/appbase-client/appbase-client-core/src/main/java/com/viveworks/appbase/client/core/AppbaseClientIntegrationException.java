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
 * Base checked exceptions for all exceptions thrown by implementations of {@link AppbaseClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseClientIntegrationException extends AppbaseClientException {
    private static final long serialVersionUID = 4584006536952325240L;

    /** Creates an AppbaseClientIntegrationException without a detail message or cause. */
    public AppbaseClientIntegrationException() {
    }

    /** Creates an AppbaseClientIntegrationException with diagnostic text.
     * @param message diagnostic message, which may be {@code null}
     */
    public AppbaseClientIntegrationException(final String message) {
        super(message);
    }

    /** Creates an AppbaseClientIntegrationException retaining contextual text and its cause.
     * @param message diagnostic message, which may be {@code null}
     * @param cause originating failure, which may be {@code null}
     */
    public AppbaseClientIntegrationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /** Creates an AppbaseClientIntegrationException from an originating failure.
     * @param cause originating failure, which may be {@code null}
     */
    public AppbaseClientIntegrationException(final Throwable cause) {
        super(cause);
    }

    /** Creates an AppbaseClientIntegrationException with explicit throwable mechanics.
     * @param message diagnostic message, which may be {@code null}
     * @param cause originating failure, which may be {@code null}
     * @param enableSuppression whether suppressed exceptions can be attached
     * @param writableStackTrace whether construction captures a stack trace
     */
    public AppbaseClientIntegrationException(final String message,
                                             final Throwable cause,
                                             final boolean enableSuppression,
                                             final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
