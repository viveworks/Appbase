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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.core.AppbaseException;

/**
 * Base exceptions for all exceptions thrown by the {@code client} module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseClientException extends AppbaseException {
    private static final long serialVersionUID = 4584006536952325240L;

    /** Creates an AppbaseClientException without a detail message or cause. */
    public AppbaseClientException() {
    }

    /** Creates an AppbaseClientException with diagnostic text.
     * @param message diagnostic message, which may be {@code null}
     */
    public AppbaseClientException(final String message) {
        super(message);
    }

    /** Creates an AppbaseClientException retaining contextual text and its cause.
     * @param message diagnostic message, which may be {@code null}
     * @param cause originating failure, which may be {@code null}
     */
    public AppbaseClientException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /** Creates an AppbaseClientException from an originating failure.
     * @param cause originating failure, which may be {@code null}
     */
    public AppbaseClientException(final Throwable cause) {
        super(cause);
    }

    /** Creates an AppbaseClientException with explicit throwable mechanics.
     * @param message diagnostic message, which may be {@code null}
     * @param cause originating failure, which may be {@code null}
     * @param enableSuppression whether suppressed exceptions can be attached
     * @param writableStackTrace whether construction captures a stack trace
     */
    public AppbaseClientException(final String message,
                                  final Throwable cause,
                                  final boolean enableSuppression,
                                  final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
