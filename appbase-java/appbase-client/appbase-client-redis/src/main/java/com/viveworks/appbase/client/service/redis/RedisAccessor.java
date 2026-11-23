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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.redis.reactive.RedisAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Redis, built on Lettuce's synchronous API. Each {@link
 * #command(String, Function)} invocation borrows one pooled {@link
 * AppbaseClientIntegrationService}, runs the supplied Lettuce sync command against it, and
 * returns it to the pool on completion or failure. Cross-cutting concerns (metrics, tracing,
 * and exception-store reporting) are supplied by the inherited {@link AbstractAppbaseAccessor};
 * this class contributes only the driver operation and Lettuce error mapping, shared with the
 * reactive twin ({@code ReactiveRedisAccessor}) via {@link
 * RedisAccessorException#map(Throwable, Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class RedisAccessor extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>>
            borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of a pooled Redis integration service, invoked once per {@link
     *               #command(String, Function)} call
     */
    public RedisAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>>
                    borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Raw escape-hatch helper: obtain Lettuce's synchronous command interface for a connection
     * already borrowed by the caller. Prefer {@link #command(String, Function)}, which also
     * handles pool borrow/return.
     *
     * @param connection Borrowed Redis connection
     * @return Lettuce sync commands for {@code connection}
     */
    public RedisCommands<String, String> sync(final StatefulRedisConnection<String, String> connection) {
        AppbaseUtil.notNull(connection, "connection");
        return connection.sync();
    }

    /**
     * Run a Redis command that returns a single value: borrows a pooled integration service,
     * applies {@code commandFn} to its Lettuce sync commands, and returns the integration
     * service to the pool on completion or failure.
     *
     * <p>Declares {@code throws Exception}, matching the inherited {@code execute} contract:
     * one of the possible mapped failure types, {@link RedisAccessorException}, is itself a
     * checked exception (it extends {@code AppbaseClientException}/{@code AppbaseException},
     * not {@code AppbaseRuntimeException}). Catching it here and rethrowing an unchecked
     * wrapper would lose its identity for callers that specifically catch {@link
     * RedisAccessorException}, so this method surfaces exactly what {@code execute} throws.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from Lettuce sync commands to the result
     * @param <T> Value type
     * @return Result of {@code commandFn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final Function<RedisCommands<String, String>, T> commandFn)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> svc =
                    borrow.get();
            try {
                return commandFn.apply(sync(svc.getClient()));
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Maps Lettuce driver failures to {@link RedisAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * RedisAccessorException} so behavior stays identical to the reactive twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return RedisAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level Redis error classification (e.g. expected cache-miss
     * conditions) is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
