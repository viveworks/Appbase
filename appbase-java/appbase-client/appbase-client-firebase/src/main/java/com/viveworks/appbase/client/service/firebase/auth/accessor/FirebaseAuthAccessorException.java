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

package com.viveworks.appbase.client.service.firebase.auth.accessor;

import com.google.firebase.auth.FirebaseAuthException;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.service.firebase.auth.accessor.reactive.ReactiveFirebaseAuthAccessor;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Thrown when a Firebase Auth failure (a {@link FirebaseAuthException}) surfaces through
 * {@link ReactiveFirebaseAuthAccessor} or its synchronous twin, in the parent {@code accessor}
 * package. The original Firebase exception is preserved as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class FirebaseAuthAccessorException extends AppbaseClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Create with a cause only.
     *
     * @param cause Original Firebase Auth failure
     */
    public FirebaseAuthAccessorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Create with a message and cause.
     *
     * @param message Message
     * @param cause Original Firebase Auth failure
     */
    public FirebaseAuthAccessorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Shared Firebase Auth failure mapping used by both the reactive and synchronous Firebase
     * Auth accessors: {@link FirebaseAuthException} maps to a new
     * {@link FirebaseAuthAccessorException} wrapping it; every other throwable is delegated to
     * {@code fallback} (typically the calling accessor's inherited default mapping).
     *
     * <p>Firebase's {@code ApiFuture} callback path (bridged to Reactor via {@code Mono.create}
     * in {@link ReactiveFirebaseAuthAccessor}) delivers a failure directly, but a blocking
     * {@code future.get()} path wraps it in an {@link ExecutionException}, and a
     * completion-stage-style path wraps it in a {@link CompletionException}. Both faces are
     * unwrapped here before the {@link FirebaseAuthException} check so the mapping is identical
     * regardless of which path the caller used.
     *
     * @param throwable Raised throwable
     * @param fallback Mapping applied to non-Firebase throwables
     * @return Mapped throwable
     */
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof FirebaseAuthException) {
            return new FirebaseAuthAccessorException(cause);
        }
        return fallback.apply(cause);
    }
}
