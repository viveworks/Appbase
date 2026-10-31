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

package com.viveworks.appbase.client.service.jasync.reactive;

import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.RowData;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.jasync.JasyncAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive accessor for jasync (MySQL/PostgreSQL over the async jasync driver). Unlike the other
 * reactive twins in this module family, jasync's driver API is {@link CompletableFuture}-based,
 * not Reactor-native: every driver call here is bridged into the Reactor world with {@link
 * Mono#fromFuture(Supplier)} inside the full {@link #mono(String, Supplier)} spine (timeout,
 * retry, scheduler, metrics, tracing, and exception-store reporting are all supplied by the
 * inherited {@link AbstractReactiveAccessor}). Each {@code sendQuery}/{@code
 * sendPreparedStatement}/{@code command} invocation borrows one pooled {@link
 * AppbaseClientIntegrationService} per subscription and returns it to the pool on completion,
 * error, or cancellation via {@code doFinally}. There is deliberately no eager client build or
 * cache: the pooled {@link Connection} (a {@code ConnectionPool} at runtime) IS the client,
 * fetched lazily on each subscription -- unlike the Aerospike twin, which builds and caches a
 * reactive facade once.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveJasyncAccessor extends AbstractReactiveAccessor {

    private final Supplier<AppbaseClientIntegrationService<Connection, TextValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled jasync integration service, invoked once per subscription
     */
    public ReactiveJasyncAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<Connection, TextValue>> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Run a plain SQL query: borrows a pooled integration service per subscription, bridges
     * {@link Connection#sendQuery(String)}'s {@link CompletableFuture} into a {@link Mono}, and
     * returns the integration service to the pool on completion, error, or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param query SQL query
     * @return Wrapped {@link Mono} of the {@link QueryResult}
     */
    public Mono<QueryResult> sendQuery(final String op, final String query) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(query, "query");
        return mono(op, () -> {
            final AppbaseClientIntegrationService<Connection, TextValue> svc = borrow.get();
            return Mono.fromFuture(() -> svc.getClient().sendQuery(query)).doFinally(sig -> svc.finalizeOperation());
        });
    }

    /**
     * Run a parameterized SQL statement: borrows a pooled integration service per subscription,
     * bridges {@link Connection#sendPreparedStatement(String, List)}'s {@link CompletableFuture}
     * into a {@link Mono}, and returns the integration service to the pool on completion, error,
     * or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param query Parameterized SQL query
     * @param values Bind values, positional
     * @return Wrapped {@link Mono} of the {@link QueryResult}
     */
    public Mono<QueryResult> sendPreparedStatement(final String op, final String query, final List<Object> values) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(query, "query");
        AppbaseUtil.notNull(values, "values");
        return mono(op, () -> {
            final AppbaseClientIntegrationService<Connection, TextValue> svc = borrow.get();
            return Mono.fromFuture(() -> svc.getClient().sendPreparedStatement(query, values))
                    .doFinally(sig -> svc.finalizeOperation());
        });
    }

    /**
     * Run a parameterized SQL statement, returning only its rows. Convenience over {@link
     * #sendPreparedStatement(String, String, List)} for callers that only need {@link
     * QueryResult#getRows()}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param query Parameterized SQL query
     * @param values Bind values, positional
     * @return Wrapped {@link Flux} of {@link RowData}
     */
    public Flux<RowData> queryRows(final String op, final String query, final List<Object> values) {
        return sendPreparedStatement(op, query, values).flatMapMany(qr -> Flux.fromIterable(qr.getRows()));
    }

    /**
     * Run a general jasync driver call: borrows a pooled integration service per subscription,
     * applies {@code commandFn} to its {@link Connection}, bridges the returned {@link
     * CompletableFuture} into a {@link Mono}, and returns the integration service to the pool on
     * completion, error, or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from a jasync connection to its driver {@link CompletableFuture}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    public <T> Mono<T> command(final String op, final Function<Connection, CompletableFuture<T>> commandFn) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return mono(op, () -> {
            final AppbaseClientIntegrationService<Connection, TextValue> svc = borrow.get();
            return Mono.fromFuture(() -> commandFn.apply(svc.getClient())).doFinally(sig -> svc.finalizeOperation());
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
     * Maps jasync driver failures to {@link JasyncAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * JasyncAccessorException} so behavior stays identical to the synchronous twin.
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
