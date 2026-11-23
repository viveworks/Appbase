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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.cassandra.reactive.CassandraAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Apache Cassandra, built on the DataStax driver's synchronous API. Each
 * {@link #executeStatement(String, Statement)}/{@link #command(String, Function)} invocation
 * borrows one pooled {@link AppbaseClientIntegrationService}, runs the supplied driver call
 * against its {@link CqlSession}, and returns it to the pool on completion or failure.
 * Cross-cutting concerns (metrics, tracing, and exception-store reporting) are supplied by the
 * inherited {@link AbstractAppbaseAccessor}; this class contributes only the driver operation and
 * DataStax error mapping, shared with the reactive twin ({@code ReactiveCassandraAccessor}) via
 * {@link CassandraAccessorException#map(Throwable, Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class CassandraAccessor extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of a pooled Cassandra integration service, invoked once per {@link
     *               #executeStatement(String, Statement)}/{@link #command(String, Function)} call
     */
    public CassandraAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Run a CQL statement that returns a result set: borrows a pooled integration service,
     * executes {@code statement} synchronously against its session, and returns the integration
     * service to the pool on completion or failure.
     *
     * <p>Named {@code executeStatement} rather than {@code execute} so it does not collide with
     * the inherited protected {@code execute(String, Callable)} spine method it delegates to.
     *
     * <p>Declares {@code throws Exception}, matching the inherited {@code execute} contract:
     * one of the possible mapped failure types, {@link CassandraAccessorException}, is itself a
     * checked exception (it extends {@code AppbaseClientException}/{@code AppbaseException}, not
     * {@code AppbaseRuntimeException}). Catching it here and rethrowing an unchecked wrapper would
     * lose its identity for callers that specifically catch {@link CassandraAccessorException}, so
     * this method surfaces exactly what {@code execute} throws.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param statement Statement to execute
     * @return Result set for {@code statement}
     * @throws Exception The mapped exception, if the operation fails
     */
    public ResultSet executeStatement(final String op, final Statement<?> statement) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(statement, "statement");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().execute(statement);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a Cassandra command that returns a single value: borrows a pooled integration service,
     * applies {@code commandFn} to its {@link CqlSession}, and returns the integration service to
     * the pool on completion or failure.
     *
     * <p>Declares {@code throws Exception}, matching the inherited {@code execute} contract:
     * one of the possible mapped failure types, {@link CassandraAccessorException}, is itself a
     * checked exception (it extends {@code AppbaseClientException}/{@code AppbaseException}, not
     * {@code AppbaseRuntimeException}). Catching it here and rethrowing an unchecked wrapper would
     * lose its identity for callers that specifically catch {@link CassandraAccessorException}, so
     * this method surfaces exactly what {@code execute} throws.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from a Cassandra session to the result
     * @param <T> Value type
     * @return Result of {@code commandFn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final Function<CqlSession, T> commandFn) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> svc = borrow.get();
            try {
                return commandFn.apply(svc.getClient());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Maps DataStax driver failures to {@link CassandraAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * CassandraAccessorException} so behavior stays identical to the reactive twin.
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
