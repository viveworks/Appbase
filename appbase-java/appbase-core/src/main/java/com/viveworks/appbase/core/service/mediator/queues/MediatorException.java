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

package com.viveworks.appbase.core.service.mediator.queues;

import com.viveworks.appbase.core.AppbaseException;

/**
 * Exception thrown during {@link com.viveworks.appbase.core.service.mediator.Mediator} operations.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MediatorException extends AppbaseException {
    private static final long serialVersionUID = 5410797125543964676L;

    public MediatorException() {
    }

    public MediatorException(final String message) {
        super(message);
    }

    public MediatorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MediatorException(final Throwable cause) {
        super(cause);
    }

    public MediatorException(final String message,
                             final Throwable cause,
                             final boolean enableSuppression,
                             final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
