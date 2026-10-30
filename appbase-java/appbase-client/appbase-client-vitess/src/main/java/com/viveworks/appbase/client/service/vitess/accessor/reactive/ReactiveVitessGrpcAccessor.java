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

package com.viveworks.appbase.client.service.vitess.accessor.reactive;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.vitess.VitessGrpcClientIntegrationService;
import com.viveworks.appbase.client.service.vitess.accessor.VitessAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.vitess.client.Context;
import io.vitess.client.SQLFuture;
import io.vitess.client.VTSession;
import io.vitess.client.cursor.Cursor;
import io.vitess.client.cursor.CursorWithError;
import io.vitess.client.cursor.Row;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reactive accessor for Vitess, over the async {@link io.vitess.client.VTGateConnection}. This is
 * the true non-blocking face of the Vitess twin (the synchronous twin, a later task, instead uses
 * the real blocking {@code VTGateBlockingConnection} -- it never blocks on the async connection,
 * since blocking on a non-blocking client's completion still ties up a thread for the duration of
 * the call, defeating the purpose of using it).
 *
 * <p>{@code VTGateConnection}'s driver calls ({@code execute}/{@code executeBatch}) return an
 * {@link SQLFuture}, a Guava {@link com.google.common.util.concurrent.ListenableFuture}, and throw
 * a checked {@link SQLException} synchronously for argument/connection-state failures raised
 * before the future is even handed back -- distinct from a failure surfacing asynchronously
 * through the future itself. The private {@link #call(String, VitessAsyncCommand)}/{@link
 * #callExplicit(String, Context, VTSession, VitessAsyncCommand)} bridge helpers below handle both:
 * the synchronous throw is caught and routed to {@link Mono#create}'s sink, and the future's
 * eventual outcome is bridged into the same sink via {@link Futures#addCallback} on Guava's {@link
 * MoreExecutors#directExecutor()} (cheap: the callback only forwards to the sink, no blocking
 * work). {@code sink.onCancel} cancels the underlying future so a downstream cancellation
 * propagates to the in-flight gRPC call rather than leaking it.
 *
 * <p>{@code VTGateConnection.streamExecute} has no reactive/future-based API at all -- it returns
 * a blocking {@link Cursor} whose {@link Cursor#next()} blocks per row. {@link #streamExecute(
 * String, String, Map)} is therefore a Tier-1 offload (see {@link AbstractReactiveAccessor}):
 * {@code streamExecute} itself, and the per-row {@code cursor.next()} polling loop that follows,
 * both run on the configured scheduler via {@code subscribeOn}, so callers get backpressure-aware
 * {@link Flux} semantics without literally being non-blocking end-to-end.
 *
 * <p>Every op comes in two flavors: the convenience form ({@code execute(op, sql, bindVars)})
 * mints a FRESH autocommit {@link VTSession} per call (via {@link
 * VitessGrpcClientIntegrationService#newVtSession()}) and uses the integration service's default
 * {@link Context} -- safe for one-shot, non-transactional calls, and concurrency-safe since each
 * subscription gets its own session. The explicit form ({@code execute(op, ctx, session, sql,
 * bindVars)}) instead uses the CALLER's {@link Context}/{@link VTSession} verbatim (e.g. a
 * transaction spanning several calls, or a caller-supplied deadline/caller-id): it must NOT mint a
 * fresh session, since that would silently break the caller's session affinity. This is done by
 * dispatching through {@link #callExplicit(String, Context, VTSession, VitessAsyncCommand)}, a
 * bridge variant that takes the caller's {@code ctx}/{@code session} as arguments instead of
 * asking the borrowed integration service to mint them.
 *
 * <p>Each operation borrows one pooled {@link VitessGrpcClientIntegrationService} per subscription
 * and returns it to the pool on completion, error, or cancellation via {@link
 * Mono#usingWhen(org.reactivestreams.Publisher, java.util.function.Function,
 * java.util.function.Function)}/{@link Flux#usingWhen(org.reactivestreams.Publisher,
 * java.util.function.Function, java.util.function.Function)}. There is deliberately no eager
 * client build or cache: the pooled client is fetched lazily on each subscription
 * (Cassandra/S3-shaped, keyed-free acquisition).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveVitessGrpcAccessor extends AbstractReactiveAccessor {

    private final Supplier<VitessGrpcClientIntegrationService> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled Vitess integration service, invoked once per subscription
     */
    public ReactiveVitessGrpcAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<VitessGrpcClientIntegrationService> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Execute a single SQL statement against a FRESH autocommit session (see the class Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sql SQL statement to execute
     * @param bindVars Bind variables for {@code sql}
     * @return Wrapped {@link Mono} of the resulting {@link Cursor}
     */
    public Mono<Cursor> execute(final String op, final String sql, final Map<String, ?> bindVars) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return call(op, (conn, ctx, session) -> conn.execute(ctx, sql, bindVars, session));
    }

    /**
     * Execute a single SQL statement against the CALLER's context/session (see the class
     * Javadoc); does NOT mint a fresh session.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param ctx Caller-supplied Vitess context
     * @param session Caller-supplied Vitess session
     * @param sql SQL statement to execute
     * @param bindVars Bind variables for {@code sql}
     * @return Wrapped {@link Mono} of the resulting {@link Cursor}
     */
    public Mono<Cursor> execute(
            final String op, final Context ctx, final VTSession session, final String sql, final Map<String, ?> bindVars) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(ctx, "ctx");
        AppbaseUtil.notNull(session, "session");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return callExplicit(op, ctx, session, (conn, explicitCtx, explicitSession) ->
                conn.execute(explicitCtx, sql, bindVars, explicitSession));
    }

    /**
     * Execute a batch of SQL statements against a FRESH autocommit session (see the class
     * Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sqlList SQL statements to execute
     * @param bindVarsList Bind variables, one entry per {@code sqlList} statement
     * @return Wrapped {@link Mono} of the resulting {@link CursorWithError} list
     */
    public Mono<List<CursorWithError>> executeBatch(
            final String op, final List<String> sqlList, final List<Map<String, ?>> bindVarsList) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(sqlList, "sqlList");
        AppbaseUtil.notNull(bindVarsList, "bindVarsList");
        return call(op, (conn, ctx, session) -> conn.executeBatch(ctx, sqlList, bindVarsList, session));
    }

    /**
     * Execute a batch of SQL statements against the CALLER's context/session (see the class
     * Javadoc); does NOT mint a fresh session.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param ctx Caller-supplied Vitess context
     * @param session Caller-supplied Vitess session
     * @param sqlList SQL statements to execute
     * @param bindVarsList Bind variables, one entry per {@code sqlList} statement
     * @return Wrapped {@link Mono} of the resulting {@link CursorWithError} list
     */
    public Mono<List<CursorWithError>> executeBatch(
            final String op, final Context ctx, final VTSession session,
            final List<String> sqlList, final List<Map<String, ?>> bindVarsList) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(ctx, "ctx");
        AppbaseUtil.notNull(session, "session");
        AppbaseUtil.notNull(sqlList, "sqlList");
        AppbaseUtil.notNull(bindVarsList, "bindVarsList");
        return callExplicit(op, ctx, session, (conn, explicitCtx, explicitSession) ->
                conn.executeBatch(explicitCtx, sqlList, bindVarsList, explicitSession));
    }

    /**
     * Stream the rows of a SQL statement's result against a FRESH autocommit session (see the
     * class Javadoc). Tier-1 offload: {@code streamExecute} itself and the {@code cursor.next()}
     * polling loop both run on the configured scheduler.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sql SQL statement to execute
     * @param bindVars Bind variables for {@code sql}
     * @return Wrapped {@link Flux} of the resulting {@link Row}s
     */
    public Flux<Row> streamExecute(final String op, final String sql, final Map<String, ?> bindVars) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return flux(op, () -> Flux.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Flux.using(
                        () -> svc.getClient().getVtGateConnection()
                                .streamExecute(svc.newContext(), sql, bindVars, svc.newVtSession()),
                        cursor -> Flux.<Row>generate(sink -> {
                            try {
                                final Row r = cursor.next();
                                if (r == null) {
                                    sink.complete();
                                } else {
                                    sink.next(r);
                                }
                            } catch (final SQLException e) {
                                sink.error(e);
                            }
                        }),
                        cursor -> {
                            try {
                                cursor.close();
                            } catch (final Exception ignored) {
                                // best-effort close; the stream has already completed or errored
                            }
                        })
                        .subscribeOn(reactorService.getDefaultScheduler()),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Stream the rows of a SQL statement's result against the CALLER's context/session (see the
     * class Javadoc); does NOT mint a fresh session. Tier-1 offload, as {@link #streamExecute(
     * String, String, Map)}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param ctx Caller-supplied Vitess context
     * @param session Caller-supplied Vitess session
     * @param sql SQL statement to execute
     * @param bindVars Bind variables for {@code sql}
     * @return Wrapped {@link Flux} of the resulting {@link Row}s
     */
    public Flux<Row> streamExecute(
            final String op, final Context ctx, final VTSession session, final String sql, final Map<String, ?> bindVars) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(ctx, "ctx");
        AppbaseUtil.notNull(session, "session");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return flux(op, () -> Flux.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Flux.using(
                        () -> svc.getClient().getVtGateConnection().streamExecute(ctx, sql, bindVars, session),
                        cursor -> Flux.<Row>generate(sink -> {
                            try {
                                final Row r = cursor.next();
                                if (r == null) {
                                    sink.complete();
                                } else {
                                    sink.next(r);
                                }
                            } catch (final SQLException e) {
                                sink.error(e);
                            }
                        }),
                        cursor -> {
                            try {
                                cursor.close();
                            } catch (final Exception ignored) {
                                // best-effort close; the stream has already completed or errored
                            }
                        })
                        .subscribeOn(reactorService.getDefaultScheduler()),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Run a general {@link VitessAsyncCommand} against a FRESH autocommit session (see the class
     * Javadoc). Escape hatch for callers needing driver calls beyond the named ops above.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Command to run against the borrowed connection
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    public <T> Mono<T> command(final String op, final VitessAsyncCommand<T> fn) {
        return call(op, fn);
    }

    /**
     * Maps Vitess driver failures to {@link VitessAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * VitessAccessorException} so behavior stays identical to the synchronous twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return VitessAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level Vitess error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }

    /**
     * Bridge a {@link VitessAsyncCommand} run against a FRESH autocommit session -- minted per
     * call via {@link VitessGrpcClientIntegrationService#newVtSession()} -- into a {@link Mono}.
     * See the class Javadoc for the {@link SQLFuture}-to-{@link Mono} bridge mechanics.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Command to run against the borrowed connection
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    private <T> Mono<T> call(final String op, final VitessAsyncCommand<T> fn) {
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.create(sink -> {
                    try {
                        final SQLFuture<T> f = fn.apply(
                                svc.getClient().getVtGateConnection(),
                                svc.newContext(),
                                svc.newVtSession());
                        Futures.addCallback(f, new FutureCallback<T>() {
                            @Override public void onSuccess(final T v) { if (v == null) sink.success(); else sink.success(v); }
                            @Override public void onFailure(final Throwable t) { sink.error(t); }
                        }, MoreExecutors.directExecutor());
                        sink.onCancel(() -> f.cancel(false));
                    } catch (final SQLException e) {
                        sink.error(e);
                    }
                }),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Bridge a {@link VitessAsyncCommand} run against the CALLER's {@code ctx}/{@code session}
     * into a {@link Mono}; unlike {@link #call(String, VitessAsyncCommand)}, does NOT mint a
     * fresh session -- {@code ctx}/{@code session} are passed straight through to {@code fn},
     * preserving the caller's session affinity (e.g. an in-flight transaction). See the class
     * Javadoc for the {@link SQLFuture}-to-{@link Mono} bridge mechanics.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param ctx Caller-supplied Vitess context
     * @param session Caller-supplied Vitess session
     * @param fn Command to run against the borrowed connection
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    private <T> Mono<T> callExplicit(
            final String op, final Context ctx, final VTSession session, final VitessAsyncCommand<T> fn) {
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.create(sink -> {
                    try {
                        final SQLFuture<T> f = fn.apply(svc.getClient().getVtGateConnection(), ctx, session);
                        Futures.addCallback(f, new FutureCallback<T>() {
                            @Override public void onSuccess(final T v) { if (v == null) sink.success(); else sink.success(v); }
                            @Override public void onFailure(final Throwable t) { sink.error(t); }
                        }, MoreExecutors.directExecutor());
                        sink.onCancel(() -> f.cancel(false));
                    } catch (final SQLException e) {
                        sink.error(e);
                    }
                }),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }
}
