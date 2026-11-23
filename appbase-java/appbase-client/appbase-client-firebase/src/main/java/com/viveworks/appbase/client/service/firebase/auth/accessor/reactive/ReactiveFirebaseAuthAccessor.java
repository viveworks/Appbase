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

package com.viveworks.appbase.client.service.firebase.auth.accessor.reactive;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.accessor.FirebaseAuthAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive accessor for Firebase Auth, built on the Admin SDK's {@link ApiFuture}-returning
 * async API. Each operation borrows one pooled {@link AppbaseClientIntegrationService} per
 * subscription, runs the supplied Firebase Auth call against it, and returns it to the pool on
 * completion, error, or cancellation via {@link Mono#usingWhen}. Cross-cutting concerns
 * (timeout, retry, scheduler, metrics, tracing, and exception-store reporting) are supplied by
 * the inherited {@link AbstractReactiveAccessor}; this class contributes only the Firebase Auth
 * operation, the {@code ApiFuture}-to-{@link Mono} bridge, and Firebase error mapping.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveFirebaseAuthAccessor extends AbstractReactiveAccessor {

    private final Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled Firebase Auth integration service, invoked once per
     *     subscription
     */
    public ReactiveFirebaseAuthAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Create a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Create request
     * @return Wrapped {@link Mono} of the created {@link UserRecord}
     */
    public Mono<UserRecord> createUser(final String op, final UserRecord.CreateRequest request) {
        return call(op, auth -> auth.createUserAsync(request));
    }

    /**
     * Look up a user by UID.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @return Wrapped {@link Mono} of the {@link UserRecord}
     */
    public Mono<UserRecord> getUser(final String op, final String uid) {
        return call(op, auth -> auth.getUserAsync(uid));
    }

    /**
     * Look up a user by email address.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param email User email address
     * @return Wrapped {@link Mono} of the {@link UserRecord}
     */
    public Mono<UserRecord> getUserByEmail(final String op, final String email) {
        return call(op, auth -> auth.getUserByEmailAsync(email));
    }

    /**
     * Update a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Update request
     * @return Wrapped {@link Mono} of the updated {@link UserRecord}
     */
    public Mono<UserRecord> updateUser(final String op, final UserRecord.UpdateRequest request) {
        return call(op, auth -> auth.updateUserAsync(request));
    }

    /**
     * Delete a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @return Wrapped {@link Mono} that completes empty
     */
    public Mono<Void> deleteUser(final String op, final String uid) {
        return call(op, auth -> auth.deleteUserAsync(uid));
    }

    /**
     * Set custom claims on a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @param claims Custom claims to set
     * @return Wrapped {@link Mono} that completes empty
     */
    public Mono<Void> setCustomUserClaims(final String op, final String uid, final Map<String, Object> claims) {
        return call(op, auth -> auth.setCustomUserClaimsAsync(uid, claims));
    }

    /**
     * Verify an ID token.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param idToken ID token to verify
     * @return Wrapped {@link Mono} of the decoded {@link FirebaseToken}
     */
    public Mono<FirebaseToken> verifyIdToken(final String op, final String idToken) {
        return call(op, auth -> auth.verifyIdTokenAsync(idToken));
    }

    /**
     * Create a custom token for a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @return Wrapped {@link Mono} of the custom token
     */
    public Mono<String> createCustomToken(final String op, final String uid) {
        return call(op, auth -> auth.createCustomTokenAsync(uid));
    }

    /**
     * General escape hatch: borrows a pooled integration service per subscription, applies
     * {@code fn} to its {@link FirebaseAuth} client, and returns the integration service to the
     * pool on completion, error, or cancellation. Prefer the named operation methods above where
     * one already covers the call being made.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from a Firebase Auth client to its {@link ApiFuture}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    public <T> Mono<T> command(final String op, final Function<FirebaseAuth, ApiFuture<T>> fn) {
        return call(op, fn);
    }

    /**
     * Bridges Firebase's {@link ApiFuture} (which is not a {@link java.util.concurrent.CompletionStage})
     * to Reactor via {@link Mono#create}: {@link ApiFutures#addCallback} drives the sink directly
     * on the future's own completion thread. {@code onSuccess(null)} completes the sink empty,
     * which is what a {@code Mono<Void>} operation needs since
     * {@link reactor.core.publisher.MonoSink#success(Object)}
     * rejects a {@code null} argument. {@code onCancel} propagates Reactor cancellation back to
     * the {@link ApiFuture} via {@link ApiFuture#cancel(boolean)}. {@link Mono#usingWhen} returns
     * the borrowed integration service to the pool on complete, error, or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from a Firebase Auth client to its {@link ApiFuture}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    private <T> Mono<T> call(final String op, final Function<FirebaseAuth, ApiFuture<T>> fn) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(fn, "fn");
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.<T>create(sink -> {
                    final ApiFuture<T> future = fn.apply(svc.getClient());
                    ApiFutures.addCallback(future, new ApiFutureCallback<T>() {
                        @Override
                        public void onSuccess(final T value) {
                            if (value == null) {
                                sink.success();
                            } else {
                                sink.success(value);
                            }
                        }

                        @Override
                        public void onFailure(final Throwable throwable) {
                            sink.error(throwable);
                        }
                    }, MoreExecutors.directExecutor());
                    sink.onCancel(() -> future.cancel(false));
                }),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Maps Firebase Auth failures to {@link FirebaseAuthAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * FirebaseAuthAccessorException} so behavior stays identical to the synchronous twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return FirebaseAuthAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level Firebase Auth error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
