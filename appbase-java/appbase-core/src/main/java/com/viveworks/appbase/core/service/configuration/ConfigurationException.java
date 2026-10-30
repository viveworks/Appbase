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

import com.viveworks.appbase.core.AppbaseException;

/**
 * Base exception of the <code>configuration</code> module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ConfigurationException extends AppbaseException {
    private static final long serialVersionUID = -3416762583638744821L;

    /**
     * Creates an empty configuration exception.
     */
    public ConfigurationException() {
    }

    /**
     * Creates an exception with the supplied detail message.
     *
     * @param message detail message, which may be {@code null}
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with a detail message and underlying cause.
     *
     * @param message detail message, which may be {@code null}
     * @param cause underlying failure, which may be {@code null}
     */
    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception whose detail message is derived from the cause.
     *
     * @param cause underlying failure, which may be {@code null}
     */
    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with explicit suppression and stack-trace behavior.
     *
     * @param message detail message, which may be {@code null}
     * @param cause underlying failure, which may be {@code null}
     * @param enableSuppression whether suppressed exceptions can be recorded
     * @param writableStackTrace whether this exception captures a stack trace
     */
    public ConfigurationException(final String message,
                                  final Throwable cause,
                                  final boolean enableSuppression,
                                  final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
