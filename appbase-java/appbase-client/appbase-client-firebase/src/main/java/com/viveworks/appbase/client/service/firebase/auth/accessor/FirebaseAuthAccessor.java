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

package com.viveworks.appbase.client.service.firebase.auth.accessor;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.accessor.reactive.ReactiveFirebaseAuthAccessor;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Firebase Auth, built on the Admin SDK's blocking sync API. Each
 * operation borrows one pooled {@link AppbaseClientIntegrationService}, runs the corresponding
 * blocking {@link FirebaseAuth} call against its client, and returns the integration service to
 * the pool on completion or failure.
 *
 * <p>Cross-cutting concerns (metrics, tracing, and exception-store reporting) are supplied by the
 * inherited {@link AbstractAppbaseAccessor}; this class contributes only the Firebase Auth
 * operation and Firebase error mapping, shared with the reactive twin ({@link
 * ReactiveFirebaseAuthAccessor}) via {@link FirebaseAuthAccessorException#map(Throwable,
 * java.util.function.Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class FirebaseAuthAccessor extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of a pooled Firebase Auth integration service, invoked once per
     *               operation
     */
    public FirebaseAuthAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Create a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Create request
     * @return The created {@link UserRecord}
     * @throws Exception The mapped exception, if the operation fails
     */
    public UserRecord createUser(final String op, final UserRecord.CreateRequest request) throws Exception {
        return run(op, auth -> auth.createUser(request));
    }

    /**
     * Look up a user by UID.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @return The {@link UserRecord}
     * @throws Exception The mapped exception, if the operation fails
     */
    public UserRecord getUser(final String op, final String uid) throws Exception {
        return run(op, auth -> auth.getUser(uid));
    }

    /**
     * Look up a user by email address.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param email User email address
     * @return The {@link UserRecord}
     * @throws Exception The mapped exception, if the operation fails
     */
    public UserRecord getUserByEmail(final String op, final String email) throws Exception {
        return run(op, auth -> auth.getUserByEmail(email));
    }

    /**
     * Update a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Update request
     * @return The updated {@link UserRecord}
     * @throws Exception The mapped exception, if the operation fails
     */
    public UserRecord updateUser(final String op, final UserRecord.UpdateRequest request) throws Exception {
        return run(op, auth -> auth.updateUser(request));
    }

    /**
     * Delete a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @throws Exception The mapped exception, if the operation fails
     */
    public void deleteUser(final String op, final String uid) throws Exception {
        run(op, auth -> {
            auth.deleteUser(uid);
            return null;
        });
    }

    /**
     * Set custom claims on a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @param claims Custom claims to set
     * @throws Exception The mapped exception, if the operation fails
     */
    public void setCustomUserClaims(final String op, final String uid, final Map<String, Object> claims)
            throws Exception {
        run(op, auth -> {
            auth.setCustomUserClaims(uid, claims);
            return null;
        });
    }

    /**
     * Verify an ID token.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param idToken ID token to verify
     * @return The decoded {@link FirebaseToken}
     * @throws Exception The mapped exception, if the operation fails
     */
    public FirebaseToken verifyIdToken(final String op, final String idToken) throws Exception {
        return run(op, auth -> auth.verifyIdToken(idToken));
    }

    /**
     * Create a custom token for a user.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param uid User UID
     * @return The custom token
     * @throws Exception The mapped exception, if the operation fails
     */
    public String createCustomToken(final String op, final String uid) throws Exception {
        return run(op, auth -> auth.createCustomToken(uid));
    }

    /**
     * Run a general Firebase Auth command that returns a single value: borrows a pooled
     * integration service, applies {@code fn} to its {@link FirebaseAuth} client, and returns the
     * integration service to the pool on completion or failure.
     *
     * <p>Uses {@link FirebaseAuthCommand} rather than {@link java.util.function.Function} so the
     * lambda can call the Admin SDK's blocking sync API directly: its terminal calls throw checked
     * {@code FirebaseAuthException}, which {@code Function.apply} cannot declare.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Checked-throwing command from a {@link FirebaseAuth} client to the result
     * @param <T> Value type
     * @return Result of {@code fn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final FirebaseAuthCommand<T> fn) throws Exception {
        return run(op, fn);
    }

    /**
     * Shared per-operation spine: borrows a pooled integration service, applies {@code fn} to its
     * {@link FirebaseAuth} client, and returns the integration service to the pool on completion
     * or failure.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Checked-throwing command from a {@link FirebaseAuth} client to the result
     * @param <T> Value type
     * @return Result of {@code fn}
     * @throws Exception The mapped exception, if the operation fails
     */
    private <T> T run(final String op, final FirebaseAuthCommand<T> fn) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(fn, "fn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<FirebaseAuth, TextValue> svc = borrow.get();
            try {
                return fn.apply(svc.getClient());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Maps Firebase Auth failures to {@link FirebaseAuthAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * FirebaseAuthAccessorException} so behavior stays identical to the reactive twin.
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
