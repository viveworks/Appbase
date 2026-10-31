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

package com.viveworks.appbase.client.service.cassandra.reactive;

import com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet;
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveRow;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive accessor for Apache Cassandra, built on the DataStax driver's reactive API. Each
 * {@link #executeReactive(String, Statement)}/{@link #query(String, Function)} invocation
 * borrows one pooled {@link AppbaseClientIntegrationService} per subscription, runs the
 * supplied reactive driver call against it, and returns it to the pool on completion, error, or
 * cancellation via {@link Flux#usingWhen}. Cross-cutting concerns (timeout, retry,
 * backpressure, scheduler, metrics, tracing, and exception-store reporting) are supplied by the
 * inherited {@link AbstractReactiveAccessor}; this class contributes only the driver operation
 * and DataStax error mapping.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveCassandraAccessor extends AbstractReactiveAccessor {

    private final Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled Cassandra integration service, invoked once per subscription
     */
    public ReactiveCassandraAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Raw escape-hatch helper: obtain a {@link ReactiveResultSet} for a session already
     * borrowed by the caller. Prefer {@link #executeReactive(String, Statement)}/
     * {@link #query(String, Function)}, which also handle pool borrow/return.
     *
     * @param session Borrowed Cassandra session
     * @param statement Statement to execute
     * @return Reactive result set for {@code statement}
     */
    public ReactiveResultSet reactive(final CqlSession session, final Statement<?> statement) {
        AppbaseUtil.notNull(session, "session");
        AppbaseUtil.notNull(statement, "statement");
        return session.executeReactive(statement);
    }

    /**
     * Run a CQL statement that returns rows: borrows a pooled integration service per
     * subscription, executes {@code statement} reactively against its session, and returns the
     * integration service to the pool on completion, error, or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param statement Statement to execute
     * @return Wrapped {@link Flux} of {@link ReactiveRow}
     */
    public Flux<ReactiveRow> executeReactive(final String op, final Statement<?> statement) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(statement, "statement");
        return flux(op, () -> Flux.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Flux.from(svc.getClient().executeReactive(statement)),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Run a CQL statement, returning only the first row (or empty if none). Convenience over
     * {@link #executeReactive(String, Statement)} for single-row queries.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param statement Statement to execute
     * @return Wrapped {@link Mono} of the first {@link ReactiveRow}, or empty
     */
    public Mono<ReactiveRow> executeReactiveFirst(final String op, final Statement<?> statement) {
        return executeReactive(op, statement).next();
    }

    /**
     * Run a general reactive driver call: borrows a pooled integration service per subscription,
     * applies {@code fn} to its session, and returns the integration service to the pool on
     * completion, error, or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from a Cassandra session to the driver's {@link Publisher}
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    public <T> Flux<T> query(final String op, final Function<CqlSession, Publisher<T>> fn) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(fn, "fn");
        return flux(op, () -> Flux.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Flux.from(fn.apply(svc.getClient())),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Maps DataStax driver failures to {@link CassandraAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * CassandraAccessorException} so behavior stays identical to the synchronous twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return CassandraAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level Cassandra error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
