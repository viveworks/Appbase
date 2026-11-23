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

package com.viveworks.appbase.client.service.r2dbc.accessor;

import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.service.r2dbc.accessor.reactive.ReactiveR2dbcAccessor;

import io.r2dbc.spi.R2dbcException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Thrown when an {@code R2DBC} driver failure (an {@link R2dbcException}, the checked exception
 * type thrown by every {@code io.r2dbc.spi} driver call) surfaces through {@link
 * ReactiveR2dbcAccessor}. The original driver exception is preserved as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class R2dbcAccessorException extends AppbaseClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Create with a cause only.
     *
     * @param cause Original driver failure
     */
    public R2dbcAccessorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Create with a message and cause.
     *
     * @param message Message
     * @param cause Original driver failure
     */
    public R2dbcAccessorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Shared R2DBC driver failure mapping used by {@link ReactiveR2dbcAccessor}: {@link
     * R2dbcException} maps to a new {@link R2dbcAccessorException} wrapping it; every other
     * throwable is delegated to {@code fallback} (typically the calling accessor's inherited
     * default mapping).
     *
     * <p>Reactor's {@code Mono}/{@code Flux} error path can surface a driver failure wrapped in a
     * {@link CompletionException} (an async composition/bridge path) or an {@link
     * ExecutionException} (a blocking future-get path). Both faces are unwrapped here before the
     * {@link R2dbcException} check so the mapping is identical regardless of which path the
     * failure travelled.
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied to non-driver throwables
     * @return Mapped throwable
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof R2dbcException) {
            return new R2dbcAccessorException(cause);
        }
        return fallback.apply(cause);
    }
}
