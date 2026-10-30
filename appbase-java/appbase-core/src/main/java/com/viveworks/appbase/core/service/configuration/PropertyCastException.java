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

package com.viveworks.appbase.core.service.configuration;

/**
 * Exception thrown by methods of {@link ConfigurationService} when a string cannot be converted back to the
 * return type.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class PropertyCastException extends ConfigurationException {
    /**
     * Creates an empty property-cast exception.
     */
    public PropertyCastException() {
    }

    /**
     * Creates an exception with the supplied detail message.
     *
     * @param message detail message, which may be {@code null}
     */
    public PropertyCastException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with a detail message and underlying cause.
     *
     * @param message detail message, which may be {@code null}
     * @param cause underlying failure, which may be {@code null}
     */
    public PropertyCastException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception whose detail message is derived from the cause.
     *
     * @param cause underlying failure, which may be {@code null}
     */
    public PropertyCastException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a conversion failure with explicit suppression and stack-trace behavior.
     *
     * @param message detail message, which may be {@code null}
     * @param cause underlying failure, which may be {@code null}
     * @param enableSuppression whether suppressed exceptions can be recorded
     * @param writableStackTrace whether this exception captures a stack trace
     */
    public PropertyCastException(final String message,
                                 final Throwable cause,
                                 final boolean enableSuppression,
                                 final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
