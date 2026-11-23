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

package com.viveworks.appbase.client.service.r2dbc.accessor.reactive;

import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientIntegrationService;
import com.viveworks.appbase.client.service.r2dbc.accessor.R2dbcAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive accessor for {@code R2DBC}, over the native {@code io.r2dbc.pool.ConnectionPool} held
 * by a borrowed {@link R2dbcClientIntegrationService}. This is the ONLY face of the R2DBC twin --
 * unlike the Vitess/JDBC twins there is no synchronous accessor here (see {@code appbase-client-
 * jdbc} for blocking access to a relational database); {@code appbase-client-r2dbc} is
 * reactive-only end to end, since a driver whose entire purpose is non-blocking access gains
 * nothing from a synchronous face and blocking on it would defeat the point of choosing it.
 *
 * <p>Every operation borrows one pooled {@link R2dbcClientIntegrationService}, then a real {@code
 * io.r2dbc.spi.Connection} FROM the native pool ({@code ConnectionPool#create()}), via the nested
 * {@link Flux#usingWhen(Publisher, Function, Function)}/{@link Mono#usingWhen(Publisher, Function,
 * Function)} borrow chain in {@link #withConnection(String, Function)}/{@link
 * #withConnectionMono(String, Function)}: the OUTER {@code usingWhen} returns the integration
 * service to the pool it was borrowed from (via {@code finalizeOperation()}) on completion, error,
 * or cancellation; the INNER {@code usingWhen} closes the borrowed {@code Connection} the same
 * way. There is deliberately no eager connection build or cache: both are fetched lazily on each
 * subscription (Cassandra/S3/Vitess-shaped, keyed-free acquisition).
 *
 * <p>{@link #query(String, String, BiFunction, Object...)}, {@link #update(String, String,
 * Object...)}, and {@link #command(String, Function)} run in AUTOCOMMIT mode against a fresh
 * connection per subscription. {@link #inTransaction(String, Function)} is the managed-transaction
 * face instead: it pins ONE connection for the whole {@code work} function and wraps it in {@code
 * beginTransaction}/{@code commitTransaction}/{@code rollbackTransaction} rather than autocommit.
 * {@link #bindAll(Statement, Object[])} binds are POSITIONAL and non-null only: {@code
 * Statement#bind(int, Object)} rejects a {@code null} value, since R2DBC requires the SQL type for
 * a null bind via {@code Statement#bindNull(int, Class)} instead; callers needing a typed null
 * bind must drop to {@link #command(String, Function)} and bind the {@link Statement} themselves.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveR2dbcAccessor extends AbstractReactiveAccessor {

    private final Supplier<R2dbcClientIntegrationService> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled R2DBC integration service, invoked once per subscription
     */
    public ReactiveR2dbcAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<R2dbcClientIntegrationService> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Execute a query and map each result row via {@code mapper}. {@code mapper} runs INSIDE
     * {@link Result#map(BiFunction)}, the only place a {@link Row}'s column accessors are valid.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sql SQL statement to execute
     * @param mapper Row mapper, invoked once per result row
     * @param binds Positional, non-null bind values for {@code sql}
     * @param <T> Mapped row type
     * @return Wrapped {@link Flux} of mapped rows
     */
    public <T> Flux<T> query(
            final String op, final String sql, final BiFunction<Row, RowMetadata, T> mapper, final Object... binds) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(mapper, "mapper");
        return withConnection(op, conn -> {
            final Statement statement = conn.createStatement(sql);
            bindAll(statement, binds);
            return Flux.from(statement.execute()).flatMap(result -> result.map(mapper));
        });
    }

    /**
     * Execute an update/DDL/DML statement and return the total affected row count, summed across
     * every {@link Result} segment produced by {@link Statement#execute()}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sql SQL statement to execute
     * @param binds Positional, non-null bind values for {@code sql}
     * @return Wrapped {@link Mono} of the total affected row count
     */
    public Mono<Long> update(final String op, final String sql, final Object... binds) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(sql, "sql");
        return withConnectionMono(op, conn -> {
            final Statement statement = conn.createStatement(sql);
            bindAll(statement, binds);
            return Flux.from(statement.execute())
                    .flatMap(Result::getRowsUpdated)
                    .reduce(0L, Long::sum);
        });
    }

    /**
     * Run a general command against a borrowed {@link Connection}. Escape hatch for callers
     * needing driver calls beyond the named {@link #query(String, String, BiFunction, Object...)}/
     * {@link #update(String, String, Object...)} ops above -- e.g. a typed-null bind via {@code
     * Statement#bindNull(int, Class)}, which {@link #bindAll(Statement, Object[])} does not
     * support.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Command to run against the borrowed connection
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    public <T> Flux<T> command(final String op, final Function<Connection, Publisher<T>> fn) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(fn, "fn");
        return withConnection(op, fn);
    }

    /**
     * Run {@code work} inside a managed transaction on ONE pinned {@link Connection}: {@code
     * beginTransaction}, then {@code work} against a {@link ReactiveSqlSession} wrapping that same
     * connection, then {@code commitTransaction} if {@code work} completes successfully -- WHETHER
     * OR NOT it emitted a value -- or {@code rollbackTransaction} followed by re-raising the
     * ORIGINAL error if {@code work} (or the begin/commit step) fails. The connection is always
     * closed, and the borrowed integration service is always returned, on completion, error, or
     * cancellation -- the same nested-borrow mechanics as {@link #withConnection(String, Function)}/
     * {@link #withConnectionMono(String, Function)}, see the class Javadoc.
     *
     * <p>{@code work} must emit AT MOST ONE value: its {@link Publisher} is collapsed to a single
     * terminal value via {@link Mono#from(Publisher)} before the commit step runs, so a caller
     * needing multiple statements/rows inside one transaction must reduce them to a single value
     * itself (e.g. {@code Flux...then(...)} for a final side-effecting statement, or {@code
     * collectList()} to gather multiple rows into one {@link java.util.List}). "At most one" INCLUDES
     * zero: {@code work} completing empty (e.g. a for-effect update collapsed via {@code .then()})
     * is a successful completion, not a failure, so it still commits -- the empty terminal value is
     * boxed in an {@link Optional} so it flows through the commit step instead of short-circuiting
     * it, since a plain {@code flatMap} on an empty {@link Mono} never runs.
     *
     * <p>If {@code work} fails, the rollback is attempted but its own failure is swallowed (not
     * propagated) so the caller always sees the ORIGINAL error that triggered the rollback, never a
     * rollback-specific failure masking it.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param work Transactional work, run against a session pinned to the transaction's connection
     * @param <T> Value type
     * @return Wrapped {@link Mono} of {@code work}'s single terminal value, empty if {@code work}
     *     completed with no value
     */
    public <T> Mono<T> inTransaction(final String op, final Function<ReactiveSqlSession, Publisher<T>> work) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(work, "work");
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.usingWhen(
                        svc.getClient().create(),
                        conn -> Mono.from(conn.beginTransaction())
                                .then(Mono.from(work.apply(new DefaultReactiveSqlSession(conn)))
                                        .map(Optional::of)
                                        .defaultIfEmpty(Optional.empty()))
                                .flatMap(optionalValue -> Mono.from(conn.commitTransaction())
                                        .then(Mono.justOrEmpty(optionalValue)))
                                .onErrorResume(error -> Mono.from(conn.rollbackTransaction())
                                        .onErrorResume(rollbackError -> Mono.empty())
                                        .then(Mono.error(error))),
                        Connection::close),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Execute {@code sql} once per element of {@code bindsList} as ONE batched {@link Statement}
     * against a fresh, autocommit connection: binds for each row are applied in turn, with {@link
     * Statement#add()} separating all but the last bind-set, followed by a single {@link
     * Statement#execute()}. Returns the total affected row count, summed across every {@link
     * Result} segment.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param sql SQL statement to execute once per row in {@code bindsList}
     * @param bindsList Non-empty list of positional, non-null bind values, one array per row
     * @return Wrapped {@link Mono} of the total affected row count
     */
    public Mono<Long> batch(final String op, final String sql, final List<Object[]> bindsList) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notEmpty(bindsList, "bindsList");
        return withConnectionMono(op, conn -> {
            final Statement statement = conn.createStatement(sql);
            for (int row = 0; row < bindsList.size(); row++) {
                bindAll(statement, bindsList.get(row));
                if (row < bindsList.size() - 1) {
                    statement.add();
                }
            }
            return Flux.from(statement.execute())
                    .flatMap(Result::getRowsUpdated)
                    .reduce(0L, Long::sum);
        });
    }

    /**
     * Maps R2DBC driver failures to {@link R2dbcAccessorException}; all other throwables delegate
     * to the inherited default mapping. Uses the shared static helper in {@link
     * R2dbcAccessorException}.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return R2dbcAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level R2DBC error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }

    /**
     * Borrow a pooled integration service, then a real {@link Connection} from its native pool,
     * running {@code work} against it; both are released (connection closed, integration service
     * returned) on completion, error, or cancellation. See the class Javadoc for the nested-borrow
     * mechanics.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param work Work to run against the borrowed connection
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    private <T> Flux<T> withConnection(final String op, final Function<Connection, Publisher<T>> work) {
        return flux(op, () -> Flux.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Flux.usingWhen(svc.getClient().create(),
                                      conn -> work.apply(conn),
                                      Connection::close),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Single-value twin of {@link #withConnection(String, Function)}, for ops (e.g. {@link
     * #update(String, String, Object...)}) whose driver call reduces to one emitted value; keeps
     * the timer/error-mapping spine on the {@link Mono} face rather than the {@link Flux} one.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param work Work to run against the borrowed connection
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    private <T> Mono<T> withConnectionMono(final String op, final Function<Connection, Mono<T>> work) {
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.usingWhen(svc.getClient().create(),
                                      work::apply,
                                      Connection::close),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Bind positional, non-null values onto {@code statement} in order. R2DBC has no notion of
     * "no bind" for a {@code null} value -- a typed null requires {@code Statement#bindNull(int,
     * Class)} -- so this helper does not support null entries in {@code binds}; callers needing a
     * typed null must use {@link #command(String, Function)} instead.
     *
     * <p>Package-private (not {@code private}) so {@link DefaultReactiveSqlSession} can reuse the
     * exact same bind loop for its transaction-pinned {@code query}/{@code update}, rather than
     * duplicating it.
     *
     * @param statement Statement to bind onto
     * @param binds Positional, non-null bind values
     */
    static void bindAll(final Statement statement, final Object[] binds) {
        for (int i = 0; i < binds.length; i++) {
            statement.bind(i, binds[i]);
        }
    }
}
