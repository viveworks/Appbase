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

package com.viveworks.appbase.client.util;

import com.viveworks.appbase.client.core.AppbaseClientRuntimeException;

/**
 * Runtime exception thrown while performing a blocking operation.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class BlockingOperationRuntimeException extends AppbaseClientRuntimeException {
    private static final long serialVersionUID = 1664805878141071537L;

    public BlockingOperationRuntimeException() {
    }

    public BlockingOperationRuntimeException(final String message) {
        super(message);
    }

    public BlockingOperationRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public BlockingOperationRuntimeException(final Throwable cause) {
        super(cause);
    }

    public BlockingOperationRuntimeException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
