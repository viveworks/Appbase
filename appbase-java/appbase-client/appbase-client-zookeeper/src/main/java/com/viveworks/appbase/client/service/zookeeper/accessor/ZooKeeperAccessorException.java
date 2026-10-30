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

package com.viveworks.appbase.client.service.zookeeper.accessor;

import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.service.zookeeper.accessor.reactive.ReactiveZooKeeperAccessor;
import org.apache.zookeeper.KeeperException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Thrown when a ZooKeeper/Curator failure (a {@link KeeperException}) surfaces through {@link
 * ReactiveZooKeeperAccessor} or its synchronous twin. The original driver exception is preserved
 * as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ZooKeeperAccessorException extends AppbaseClientException {
    private static final long serialVersionUID = -129786622623504627L;

    public ZooKeeperAccessorException() {
    }

    public ZooKeeperAccessorException(final String message) {
        super(message);
    }

    public ZooKeeperAccessorException(final String message, Throwable cause) {
        super(message, cause);
    }

    public ZooKeeperAccessorException(final Throwable cause) {
        super(cause);
    }

    public ZooKeeperAccessorException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Shared ZooKeeper/Curator failure mapping used by both the reactive and synchronous
     * ZooKeeper accessors: {@link KeeperException} maps to a new {@link ZooKeeperAccessorException}
     * wrapping it; every other throwable is delegated to {@code fallback} (typically the calling
     * accessor's inherited default mapping).
     *
     * <p>Curator's async operations (via {@code curator-x-async}'s {@code AsyncStage}, a {@link
     * java.util.concurrent.CompletionStage}) deliver a failure raised inside the stage wrapped in
     * a {@link CompletionException} (the completion-stage callback path, e.g. via {@code
     * Mono.fromCompletionStage}) or an {@link ExecutionException} (the blocking {@code
     * future.get()} path). Both faces are unwrapped here before the {@link KeeperException} check
     * so the mapping is identical regardless of which path the caller used.
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied to non-driver throwables
     * @return Mapped throwable
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof KeeperException) {
            return new ZooKeeperAccessorException(cause);
        }
        return fallback.apply(cause);
    }
}
