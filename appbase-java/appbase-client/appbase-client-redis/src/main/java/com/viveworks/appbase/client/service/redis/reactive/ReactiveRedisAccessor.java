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

package com.viveworks.appbase.client.service.redis.reactive;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive accessor for Redis, built on Lettuce's reactive API. Each {@link #command(String,
 * Function)}/{@link #commandMany(String, Function)} invocation borrows one pooled {@link
 * AppbaseClientIntegrationService} per subscription, runs the supplied Lettuce reactive command
 * against it, and returns it to the pool on completion, error, or cancellation via {@link
 * Mono#usingWhen}/{@link Flux#usingWhen}. Cross-cutting concerns (timeout, retry, backpressure,
 * scheduler, metrics, tracing, and exception-store reporting) are supplied by the inherited
 * {@link AbstractReactiveAccessor}; this class contributes only the driver operation and Lettuce
 * error mapping.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveRedisAccessor extends AbstractReactiveAccessor {

    private final Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>>
            borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled Redis integration service, invoked once per subscription
     */
    public ReactiveRedisAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>>
                    borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Raw escape-hatch helper: obtain Lettuce's reactive command interface for a connection
     * already borrowed by the caller. Prefer {@link #command(String, Function)}/
     * {@link #commandMany(String, Function)}, which also handle pool borrow/return.
     *
     * @param connection Borrowed Redis connection
     * @return Lettuce reactive commands for {@code connection}
     */
    public RedisReactiveCommands<String, String> reactive(final StatefulRedisConnection<String, String> connection) {
        AppbaseUtil.notNull(connection, "connection");
        return connection.reactive();
    }

    /**
     * Run a Redis command that returns a single value: borrows a pooled integration service per
     * subscription, applies {@code commandFn} to its Lettuce reactive commands, and returns the
     * integration service to the pool on completion, error, or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from Lettuce reactive commands to the driver's {@link Mono}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    public <T> Mono<T> command(final String op,
                                final Function<RedisReactiveCommands<String, String>, Mono<T>> commandFn) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> commandFn.apply(reactive(svc.getClient())),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Run a Redis command that returns a stream: borrows a pooled integration service per
     * subscription, applies {@code commandFn} to its Lettuce reactive commands, and returns the
     * integration service to the pool on completion, error, or cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from Lettuce reactive commands to the driver's {@link Flux}
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    public <T> Flux<T> commandMany(final String op,
                                    final Function<RedisReactiveCommands<String, String>, Flux<T>> commandFn) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return flux(op, () -> Flux.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> commandFn.apply(reactive(svc.getClient())),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Maps Lettuce driver failures to {@link RedisAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * RedisAccessorException} so behavior stays identical to the synchronous twin.
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
