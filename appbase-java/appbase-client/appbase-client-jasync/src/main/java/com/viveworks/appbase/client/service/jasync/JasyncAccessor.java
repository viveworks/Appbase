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

import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous accessor for jasync (MySQL/PostgreSQL over the async jasync driver), blocking on
 * jasync's {@link CompletableFuture}-based driver API via {@code .get()} then {@link #unwrap}
 * so the mapped failure is identical to the reactive twin ({@code ReactiveJasyncAccessor}). Each
 * {@link #sendQuery(String, String)}/{@link #sendPreparedStatement(String, String, List)}/{@link
 * #command(String, Function)} invocation borrows one pooled {@link AppbaseClientIntegrationService}
 * and returns it to the pool on completion or failure. Unlike the pooled drivers of other twins
 * (Cassandra's {@code CqlSession}, Kafka's {@code Producer}), jasync's pooled {@link Connection}
 * (a {@code ConnectionPool} at runtime) handles its own internal connection borrowing, so {@code
 * borrow.get()} here returns the shared pool itself rather than a per-operation lease --
 * {@code finalizeOperation()} is still called on every path for spine symmetry with the other
 * accessors, even though jasync itself does not require it. Cross-cutting concerns (metrics,
 * tracing, and exception-store reporting) are supplied by the inherited {@link
 * AbstractAppbaseAccessor}; this class contributes only the driver operation and jasync error
 * mapping, shared with the reactive twin via {@link JasyncAccessorException#map(Throwable,
 * Function)}.
 *
 * <p>Note: this synchronous accessor blocks on the driver's {@code CompletableFuture} via a bare
 * {@code .get()} with no timeout, and does NOT apply {@link ReactiveOperationConfiguration}'s
 * {@code timeout}/retry knobs (only its metrics/trace/exception-store flags are honored) -- rely on
 * jasync's own {@code queryTimeout} pool configuration to bound a hung query.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class JasyncAccessor extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<Connection, TextValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of a pooled jasync integration service, invoked once per {@link
     *               #sendQuery(String, String)}/{@link #sendPreparedStatement(String, String,
     *               List)}/{@link #command(String, Function)} call
     */
    public JasyncAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<Connection, TextValue>> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Run a plain SQL query: borrows a pooled integration service, blocks on {@link
     * Connection#sendQuery(String)}'s {@link CompletableFuture}, and returns the integration
     * service to the pool on completion or failure.
     *
     * <p>Declares {@code throws Exception}, matching the inherited {@code execute} contract: one
     * of the possible mapped failure types, {@link JasyncAccessorException}, is itself a checked
     * exception (it extends {@code AppbaseClientException}/{@code AppbaseException}, not {@code
     * AppbaseRuntimeException}). Catching it here and rethrowing an unchecked wrapper would lose
     * its identity for callers that specifically catch {@link JasyncAccessorException}, so this
     * method surfaces exactly what {@code execute} throws.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param query SQL query
     * @return {@link QueryResult} for {@code query}
     * @throws Exception The mapped exception, if the operation fails
     */
    public QueryResult sendQuery(final String op, final String query) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(query, "query");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<Connection, TextValue> svc = borrow.get();
            try {
                return unwrap(() -> svc.getClient().sendQuery(query).get());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a parameterized SQL statement: borrows a pooled integration service, blocks on {@link
     * Connection#sendPreparedStatement(String, List)}'s {@link CompletableFuture}, and returns the
     * integration service to the pool on completion or failure.
     *
     * <p>Declares {@code throws Exception} for the same reason as {@link #sendQuery(String,
     * String)}: {@link JasyncAccessorException} is a checked exception whose identity must be
     * preserved for callers.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param query Parameterized SQL query
     * @param values Bind values, positional
     * @return {@link QueryResult} for {@code query}
     * @throws Exception The mapped exception, if the operation fails
     */
    public QueryResult sendPreparedStatement(final String op, final String query, final List<Object> values)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(query, "query");
        AppbaseUtil.notNull(values, "values");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<Connection, TextValue> svc = borrow.get();
            try {
                return unwrap(() -> svc.getClient().sendPreparedStatement(query, values).get());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a general jasync driver call that returns a single value: borrows a pooled integration
     * service, applies {@code commandFn} to its {@link Connection}, blocks on the returned {@link
     * CompletableFuture}, and returns the integration service to the pool on completion or
     * failure.
     *
     * <p>Declares {@code throws Exception} for the same reason as {@link #sendQuery(String,
     * String)}: {@link JasyncAccessorException} is a checked exception whose identity must be
     * preserved for callers.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from a jasync connection to its driver {@link CompletableFuture}
     * @param <T> Value type
     * @return Result of {@code commandFn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final Function<Connection, CompletableFuture<T>> commandFn)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<Connection, TextValue> svc = borrow.get();
            try {
                return unwrap(() -> commandFn.apply(svc.getClient()).get());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Returns the raw pooled jasync {@link Connection} for advanced use. Operations issued
     * through it BYPASS this accessor's spine (no metrics/tracing/error-mapping). The underlying
     * {@code ConnectionPool} manages its own per-query connection borrowing internally, so there
     * is no {@code finalizeOperation} for the caller to invoke. Prefer {@link #sendQuery(String,
     * String)}/{@link #sendPreparedStatement(String, String, List)}/{@link #command(String,
     * Function)}.
     *
     * @return Borrowed jasync {@link Connection}
     */
    public Connection connection() {
        return borrow.get().getClient();
    }

    /**
     * Blocks on a jasync driver {@link CompletableFuture} via {@code call.call()} (expected to be
     * a {@code future::get} reference) and unwraps the {@link ExecutionException} cause.
     *
     * <p>Rationale: {@code Future.get()} (via the {@code call} reference here) wraps any driver
     * failure in an {@link ExecutionException} whose cause is the real {@code DatabaseException}.
     * {@link JasyncAccessorException#map(Throwable, Function)} is shared with the reactive twin,
     * which sees failures via {@code CompletionException} (the {@code Mono.fromFuture} path); its
     * cause-unwrapping already tolerates {@link ExecutionException} too, but unwrapping here keeps
     * the throwable that reaches {@code mapError} identical in shape to the Kafka/Cassandra sync
     * twins, and keeps the exception's stack trace anchored at the real driver failure rather than
     * the JDK's {@code Future.get()} wrapper.
     *
     * @param call The blocking call, typically a jasync {@code future::get} method reference
     * @param <T> Value type
     * @return Result of {@code call}
     * @throws Exception The unwrapped driver failure, or the original {@link ExecutionException} if
     *                   its cause is not itself an {@link Exception}
     */
    private static <T> T unwrap(final Callable<T> call) throws Exception {
        try {
            return call.call();
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        } catch (final InterruptedException e) {
            // Future.get() clears the interrupt on throw; restore it so callers can observe cancellation.
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * Maps jasync driver failures to {@link JasyncAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * JasyncAccessorException} so behavior stays identical to the reactive twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return JasyncAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level jasync error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
