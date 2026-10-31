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

package com.viveworks.appbase.client.service.jasync;

import com.github.jasync.sql.db.exceptions.DatabaseException;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.service.jasync.reactive.ReactiveJasyncAccessor;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Thrown when a jasync driver failure (a {@link DatabaseException}) surfaces through {@link
 * ReactiveJasyncAccessor} or its synchronous twin, in the parent {@code jasync} package. The
 * original driver exception is preserved as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class JasyncAccessorException extends AppbaseClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Create with a cause only.
     *
     * @param cause Original driver failure
     */
    public JasyncAccessorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Create with a message and cause.
     *
     * @param message Message
     * @param cause Original driver failure
     */
    public JasyncAccessorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Shared jasync driver failure mapping used by both the reactive and synchronous jasync
     * accessors: {@link DatabaseException} maps to a new {@link JasyncAccessorException} wrapping
     * it; every other throwable is delegated to {@code fallback} (typically the calling
     * accessor's inherited default mapping).
     *
     * <p>jasync's driver calls return {@link java.util.concurrent.CompletableFuture}; a failure
     * raised inside the future is delivered wrapped in a {@link CompletionException} (the
     * completion-stage callback path, e.g. via {@code Mono.fromFuture}) or an {@link
     * ExecutionException} (the blocking {@code future.get()} path). Both faces are unwrapped here
     * before the {@link DatabaseException} check so the mapping is identical regardless of which
     * path the caller used.
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied to non-driver throwables
     * @return Mapped throwable
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof DatabaseException) {
            return new JasyncAccessorException(cause);
        }
        return fallback.apply(cause);
    }
}
