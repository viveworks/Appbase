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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive.ReactiveElasticsearchAccessor;
import org.elasticsearch.client.ResponseException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Thrown when an Elasticsearch REST client failure (a {@link ResponseException}) surfaces
 * through {@link ReactiveElasticsearchAccessor} or its synchronous twin, in the parent {@code
 * restclient.api} package. The original driver exception is preserved as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ElasticsearchAccessorException extends AppbaseClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Create with a cause only.
     *
     * @param cause Original driver failure
     */
    public ElasticsearchAccessorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Create with a message and cause.
     *
     * @param message Message
     * @param cause Original driver failure
     */
    public ElasticsearchAccessorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Shared Elasticsearch REST client failure mapping used by both the reactive and
     * synchronous accessors: {@link ResponseException} maps to a new {@link
     * ElasticsearchAccessorException} wrapping it; every other throwable is delegated to
     * {@code fallback} (typically the calling accessor's inherited default mapping).
     *
     * <p>{@link ResponseException} is Elasticsearch's non-2xx signal (extends {@link
     * IOException}); other {@link IOException}s (e.g. transport failures) deliberately fall
     * through to the default mapping.
     *
     * <p>The typed client's async calls return {@link CompletableFuture};
     * a failure raised inside the future is delivered wrapped in a {@link CompletionException}
     * (the completion-stage callback path, e.g. via {@code Mono.fromFuture}) or an {@link
     * ExecutionException} (the blocking {@code future.get()} path). Both faces are unwrapped
     * here before the {@link ResponseException} check so the mapping is identical regardless of
     * which path the caller used.
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied to non-driver throwables
     * @return Mapped throwable
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof ResponseException) {
            return new ElasticsearchAccessorException(cause);
        }
        return fallback.apply(cause);
    }
}
