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

import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.vitess.VitessGrpcClientIntegrationService;
import com.viveworks.appbase.client.service.vitess.accessor.reactive.ReactiveVitessGrpcAccessor;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.vitess.client.Context;
import io.vitess.client.VTGateBlockingConnection;
import io.vitess.client.VTSession;
import io.vitess.client.cursor.Cursor;
import io.vitess.client.cursor.CursorWithError;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Vitess, the blocking twin of {@link ReactiveVitessGrpcAccessor}. It
 * runs over the real blocking {@link VTGateBlockingConnection}, fetched via {@code
 * svc.getClient().getVtGateBlockingConnection()} -- it deliberately never blocks on the async
 * {@code VTGateConnection}, since blocking on a non-blocking client's completion still ties up a
 * thread for the duration of the call, defeating the purpose of using it. Each driver call
 * returns a plain blocking value (not a {@code Mono}/{@code Flux}); the cross-cutting spine
 * (timer, span, error counter, failure logging, and exception-store reporting) is supplied by the
 * inherited {@link AbstractAppbaseAccessor} via {@link AbstractAppbaseAccessor#execute(String,
 * java.util.concurrent.Callable)}.
 *
 * <p>Each operation borrows one pooled {@link VitessGrpcClientIntegrationService} per call. The
 * point-query ops ({@code execute}/{@code executeBatch}/{@code command}) fully materialize their
 * result before returning, so they return it to the pool in a {@code finally} block (on both
 * success and error) via the private {@link #run(String, VitessCommand)}/{@link #runExplicit(
 * String, Context, VTSession, VitessCommand)} helpers below. {@code streamExecute}, however,
 * returns a lazily-draining {@link Cursor} that keeps pulling rows off the borrowed connection
 * after the call returns -- releasing the borrow eagerly there would let another borrower reuse
 * the connection while the stream is still open, so it instead hands the borrow off to a {@link
 * BorrowReleasingCursor}, releasing on error (immediately) or on the caller closing the returned
 * cursor (success); see {@link #streamExecute(String, String, Map)}. There is deliberately no
 * eager client build or cache: the pooled client is fetched lazily on each call (Cassandra/S3-
 * shaped, keyed-free acquisition).
 *
 * <p>Every op comes in two flavors, mirroring {@link ReactiveVitessGrpcAccessor}: the convenience
 * form ({@code execute(op, sql, bindVars)}) mints a FRESH autocommit {@link VTSession} per call
 * (via {@link VitessGrpcClientIntegrationService#newVtSession()}) and uses the integration
 * service's default {@link Context} -- safe for one-shot, non-transactional calls. The explicit
 * form ({@code execute(op, ctx, session, sql, bindVars)}) instead uses the CALLER's {@link
 * Context}/{@link VTSession} verbatim (e.g. a transaction spanning several calls): it must NOT
 * mint a fresh session, since that would silently break the caller's session affinity. Both
 * flavors of the point-query ops dispatch through the private {@link #run(String,
 * VitessCommand)}/{@link #runExplicit(String, Context, VTSession, VitessCommand)} helpers below;
 * {@code streamExecute}'s two flavors do not, for the borrow-lifetime reason above.
 *
 * <p>{@code VTGateBlockingConnection}'s driver calls throw a checked {@link SQLException}
 * directly -- there is no future to bridge, unlike the async {@code VTGateConnection} the
 * reactive twin runs over. {@link VitessCommand} is declared to throw it, and it is unwrapped in
 * {@link #mapError(Throwable)} via {@link VitessAccessorException#map(Throwable,
 * java.util.function.Function)}, the same shared helper the reactive twin uses, so both faces
 * expose identical error semantics.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class VitessGrpcAccessor extends AbstractAppbaseAccessor {

    private final ReactiveOperationConfiguration configuration;
    private final Supplier<VitessGrpcClientIntegrationService> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration (supplies the metrics/trace/
     *         exception-store gate flags shared with the reactive twin)
     * @param borrow Supplier of a pooled Vitess integration service, invoked once per operation
     */
    public VitessGrpcAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<VitessGrpcClientIntegrationService> borrow) {
        super(appbaseCore,
                configuration.isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.configuration = AppbaseUtil.notNull(configuration, "configuration");
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * The reactive operation configuration this accessor was built from. Mirrors {@link
     * ReactiveVitessGrpcAccessor#getReactiveOperationConfiguration()} so the synchronous and
     * reactive twins expose an identical acquisition contract.
     *
     * @return the reactive operation configuration
     */
    public ReactiveOperationConfiguration getReactiveOperationConfiguration() {
        return configuration;
    }

    /**
     * Execute a single SQL statement against a FRESH autocommit session (see the class Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sql SQL statement to execute
     * @param bindVars Bind variables for {@code sql}
     * @return the resulting {@link Cursor}
     * @throws Exception the mapped failure, if the driver call fails
     */
    public Cursor execute(final String op, final String sql, final Map<String, ?> bindVars) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return run(op, (conn, ctx, session) -> conn.execute(ctx, sql, bindVars, session));
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
     * @return the resulting {@link Cursor}
     * @throws Exception the mapped failure, if the driver call fails
     */
    public Cursor execute(
            final String op, final Context ctx, final VTSession session, final String sql, final Map<String, ?> bindVars)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(ctx, "ctx");
        AppbaseUtil.notNull(session, "session");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return runExplicit(op, ctx, session, (conn, explicitCtx, explicitSession) ->
                conn.execute(explicitCtx, sql, bindVars, explicitSession));
    }

    /**
     * Execute a batch of SQL statements against a FRESH autocommit session (see the class
     * Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sqlList SQL statements to execute
     * @param bindVarsList Bind variables, one entry per {@code sqlList} statement
     * @return the resulting {@link CursorWithError} list
     * @throws Exception the mapped failure, if the driver call fails
     */
    public List<CursorWithError> executeBatch(
            final String op, final List<String> sqlList, final List<Map<String, ?>> bindVarsList) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(sqlList, "sqlList");
        AppbaseUtil.notNull(bindVarsList, "bindVarsList");
        return run(op, (conn, ctx, session) -> conn.executeBatch(ctx, sqlList, bindVarsList, session));
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
     * @return the resulting {@link CursorWithError} list
     * @throws Exception the mapped failure, if the driver call fails
     */
    public List<CursorWithError> executeBatch(
            final String op, final Context ctx, final VTSession session,
            final List<String> sqlList, final List<Map<String, ?>> bindVarsList) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(ctx, "ctx");
        AppbaseUtil.notNull(session, "session");
        AppbaseUtil.notNull(sqlList, "sqlList");
        AppbaseUtil.notNull(bindVarsList, "bindVarsList");
        return runExplicit(op, ctx, session, (conn, explicitCtx, explicitSession) ->
                conn.executeBatch(explicitCtx, sqlList, bindVarsList, explicitSession));
    }

    /**
     * Stream the rows of a SQL statement's result against a FRESH autocommit session (see the
     * class Javadoc). Unlike {@link ReactiveVitessGrpcAccessor#streamExecute(String, String,
     * Map)}'s {@code Flux<Row>}, this synchronous face returns the blocking {@link Cursor}
     * directly -- the caller drains it (e.g. via {@code cursor.next()}).
     *
     * <p>Unlike the point-query ops ({@link #execute(String, String, Map)}, {@link
     * #executeBatch(String, List, List)}), which fully materialize their result before returning
     * and so release the borrowed {@link VitessGrpcClientIntegrationService} immediately, the
     * {@link Cursor} returned here holds that borrowed connection open for the duration of the
     * stream -- the caller MUST close it (e.g. via try-with-resources) to return the connection to
     * the pool. See {@link BorrowReleasingCursor}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sql SQL statement to execute
     * @param bindVars Bind variables for {@code sql}
     * @return the resulting {@link Cursor}; MUST be closed by the caller to release the pooled
     *         connection it holds open
     * @throws Exception the mapped failure, if obtaining the stream fails
     */
    public Cursor streamExecute(final String op, final String sql, final Map<String, ?> bindVars) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return execute(op, () -> {
            final VitessGrpcClientIntegrationService svc = borrow.get();
            try {
                final VTGateBlockingConnection conn = svc.getClient().getVtGateBlockingConnection();
                final Cursor cursor = conn.streamExecute(
                        svc.newContext(), sql, bindVars, svc.newVtSession());
                return new BorrowReleasingCursor(cursor, svc);
            } catch (final Exception e) {
                svc.finalizeOperation();
                throw e;
            }
        });
    }

    /**
     * Stream the rows of a SQL statement's result against the CALLER's context/session (see the
     * class Javadoc); does NOT mint a fresh session. As {@link #streamExecute(String, String,
     * Map)}, returns the blocking {@link Cursor} directly for the caller to drain, and it likewise
     * holds the borrowed connection open until the caller closes it.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param ctx Caller-supplied Vitess context
     * @param session Caller-supplied Vitess session
     * @param sql SQL statement to execute
     * @param bindVars Bind variables for {@code sql}
     * @return the resulting {@link Cursor}; MUST be closed by the caller to release the pooled
     *         connection it holds open
     * @throws Exception the mapped failure, if obtaining the stream fails
     */
    public Cursor streamExecute(
            final String op, final Context ctx, final VTSession session, final String sql, final Map<String, ?> bindVars)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(ctx, "ctx");
        AppbaseUtil.notNull(session, "session");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(bindVars, "bindVars");
        return execute(op, () -> {
            final VitessGrpcClientIntegrationService svc = borrow.get();
            try {
                final VTGateBlockingConnection conn = svc.getClient().getVtGateBlockingConnection();
                final Cursor cursor = conn.streamExecute(ctx, sql, bindVars, session);
                return new BorrowReleasingCursor(cursor, svc);
            } catch (final Exception e) {
                svc.finalizeOperation();
                throw e;
            }
        });
    }

    /**
     * Run a general {@link VitessCommand} against a FRESH autocommit session (see the class
     * Javadoc). Escape hatch for callers needing driver calls beyond the named ops above.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Command to run against the borrowed connection
     * @param <T> Value type
     * @return the driver result
     * @throws Exception the mapped failure, if the driver call fails
     */
    public <T> T command(final String op, final VitessCommand<T> fn) throws Exception {
        return run(op, fn);
    }

    /**
     * Maps Vitess driver failures to {@link VitessAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * VitessAccessorException} so behavior stays identical to the reactive twin.
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
     * Run a {@link VitessCommand} against a FRESH autocommit session -- minted per call via
     * {@link VitessGrpcClientIntegrationService#newVtSession()} -- through the inherited spine.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Command to run against the borrowed connection
     * @param <T> Value type
     * @return the driver result
     * @throws Exception the mapped failure, if the driver call fails
     */
    private <T> T run(final String op, final VitessCommand<T> fn) throws Exception {
        return execute(op, () -> {
            final VitessGrpcClientIntegrationService svc = borrow.get();
            try {
                return fn.apply(
                        svc.getClient().getVtGateBlockingConnection(),
                        svc.newContext(),
                        svc.newVtSession());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a {@link VitessCommand} against the CALLER's {@code ctx}/{@code session} through the
     * inherited spine; unlike {@link #run(String, VitessCommand)}, does NOT mint a fresh session
     * -- {@code ctx}/{@code session} are passed straight through to {@code fn}, preserving the
     * caller's session affinity (e.g. an in-flight transaction).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param ctx Caller-supplied Vitess context
     * @param session Caller-supplied Vitess session
     * @param fn Command to run against the borrowed connection
     * @param <T> Value type
     * @return the driver result
     * @throws Exception the mapped failure, if the driver call fails
     */
    private <T> T runExplicit(
            final String op, final Context ctx, final VTSession session, final VitessCommand<T> fn) throws Exception {
        return execute(op, () -> {
            final VitessGrpcClientIntegrationService svc = borrow.get();
            try {
                return fn.apply(svc.getClient().getVtGateBlockingConnection(), ctx, session);
            } finally {
                svc.finalizeOperation();
            }
        });
    }
}
