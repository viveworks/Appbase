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

package com.viveworks.appbase.client.service.vitess.accessor;

import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.service.vitess.accessor.reactive.ReactiveVitessGrpcAccessor;

import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Thrown when a {@code vitess} driver failure (a {@link SQLException}, the checked exception
 * type thrown by every {@code VTGateConnection} driver call) surfaces through {@link
 * ReactiveVitessGrpcAccessor} or its synchronous twin, in the parent {@code accessor} package.
 * The original driver exception is preserved as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class VitessAccessorException extends AppbaseClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Create with a cause only.
     *
     * @param cause Original driver failure
     */
    public VitessAccessorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Create with a message and cause.
     *
     * @param message Message
     * @param cause Original driver failure
     */
    public VitessAccessorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Shared vitess driver failure mapping used by both the reactive and synchronous vitess
     * accessors: {@link SQLException} maps to a new {@link VitessAccessorException} wrapping it;
     * every other throwable is delegated to {@code fallback} (typically the calling accessor's
     * inherited default mapping).
     *
     * <p>The {@code SQLFuture}/{@code Mono} bridge in {@link ReactiveVitessGrpcAccessor} surfaces
     * a driver failure raised inside the future wrapped in a {@link CompletionException} (the
     * {@code Futures.addCallback}/reactive-sink path) or an {@link ExecutionException} (a blocking
     * {@code future.get()} path). Both faces are unwrapped here before the {@link SQLException}
     * check so the mapping is identical regardless of which path the caller used.
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied to non-driver throwables
     * @return Mapped throwable
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof SQLException) {
            return new VitessAccessorException(cause);
        }
        return fallback.apply(cause);
    }
}
