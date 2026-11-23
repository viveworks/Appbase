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

package com.viveworks.appbase.client.service.aerospike.reactive;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.query.Statement;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Reactive accessor for Aerospike, built on the Aerospike Java client's reactor module. Each
 * discrete, bounded operation ({@link #get}, {@link #put}, {@link #delete}, {@link #exists},
 * {@link #operate}, {@link #query}) runs directly against the injected {@link
 * IAerospikeReactorClient} through the full {@code mono}/{@code flux} spine inherited from {@link
 * AbstractReactiveAccessor} — timeout, retry/backoff, backpressure buffering (for {@link #query}),
 * scheduler dispatch, metrics, tracing, and exception-store reporting. The lighter {@code
 * fluxStream} spine is intentionally not used here: every operation this accessor exposes is a
 * single bounded call/result set, not a long-lived stream, so per-op timeout/retry/metrics remain
 * correct and cheap to re-run.
 *
 * <p>The injected {@link IAerospikeReactorClient} is a <strong>non-owning</strong> wrapper around
 * the Aerospike integration service's shared {@code IAerospikeClient}: this accessor never calls
 * {@link IAerospikeReactorClient#close()} on it. Closing it would close the shared synchronous
 * client underneath every other consumer of that connection pool — a double-close hazard. Lifecycle
 * (connect/close) stays owned by the integration service that constructed the shared client.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveAerospikeAccessor extends AbstractReactiveAccessor {

    private final IAerospikeReactorClient reactorClient;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param reactorClient Non-owning Aerospike reactor client, wrapping the shared sync client
     */
    public ReactiveAerospikeAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final IAerospikeReactorClient reactorClient) {
        super(appbaseCore, reactorService, configuration);
        this.reactorClient = AppbaseUtil.notNull(reactorClient, "reactorClient");
    }

    /**
     * Fetch a record by key, using the client's default read policy.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @return Wrapped {@link Mono} of the {@link KeyRecord}, or empty if the record is absent
     */
    public Mono<KeyRecord> get(final String op, final Key key) {
        return mono(op, () -> reactorClient.get(key))
                .filter(keyRecord -> keyRecord.record != null);
    }

    /**
     * Fetch a record by key, using an explicit read {@link Policy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Read policy
     * @param key Record key
     * @return Wrapped {@link Mono} of the {@link KeyRecord}, or empty if the record is absent
     */
    public Mono<KeyRecord> get(final String op, final Policy policy, final Key key) {
        return mono(op, () -> reactorClient.get(policy, key))
                .filter(keyRecord -> keyRecord.record != null);
    }

    /**
     * Write one or more bins to a record, using the client's default write policy.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @param bins Bins to write
     * @return Wrapped {@link Mono} of the written {@link Key}
     */
    public Mono<Key> put(final String op, final Key key, final Bin... bins) {
        return mono(op, () -> reactorClient.put(key, bins));
    }

    /**
     * Write one or more bins to a record, using an explicit {@link WritePolicy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Write policy
     * @param key Record key
     * @param bins Bins to write
     * @return Wrapped {@link Mono} of the written {@link Key}
     */
    public Mono<Key> put(final String op, final WritePolicy policy, final Key key, final Bin... bins) {
        return mono(op, () -> reactorClient.put(policy, key, bins));
    }

    /**
     * Delete a record by key, using the client's default write policy.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @return Wrapped {@link Mono} of the deleted {@link Key}
     */
    public Mono<Key> delete(final String op, final Key key) {
        return mono(op, () -> reactorClient.delete(key));
    }

    /**
     * Delete a record by key, using an explicit {@link WritePolicy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Write policy
     * @param key Record key
     * @return Wrapped {@link Mono} of the deleted {@link Key}
     */
    public Mono<Key> delete(final String op, final WritePolicy policy, final Key key) {
        return mono(op, () -> reactorClient.delete(policy, key));
    }

    /**
     * Check whether a record exists, using the client's default read policy. Note: per the
     * underlying {@link IAerospikeReactorClient#exists(Key)} contract, this emits the record's
     * {@link Key} when it exists and completes empty when it does not — it does NOT emit a
     * {@code boolean}. (The synchronous twin returns a {@code boolean}, matching the sync
     * client's own {@code exists} contract; the two accessors intentionally differ here because
     * each mirrors its own underlying driver API.)
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @return Wrapped {@link Mono} of {@code key} if the record exists, otherwise empty
     */
    public Mono<Key> exists(final String op, final Key key) {
        return mono(op, () -> reactorClient.exists(key));
    }

    /**
     * Run a record-level read/write operation batch (e.g. increment, append), using an explicit
     * {@link WritePolicy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Write policy
     * @param key Record key
     * @param operations Operations to apply
     * @return Wrapped {@link Mono} of the resulting {@link KeyRecord}
     */
    public Mono<KeyRecord> operate(final String op, final WritePolicy policy, final Key key,
                                    final Operation... operations) {
        return mono(op, () -> reactorClient.operate(policy, key, operations));
    }

    /**
     * Run a secondary-index query, using the client's default query policy.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param statement Query statement
     * @return Wrapped {@link Flux} of matching {@link KeyRecord}s
     */
    public Flux<KeyRecord> query(final String op, final Statement statement) {
        return flux(op, () -> reactorClient.query(statement));
    }

    /**
     * Run a secondary-index query, using an explicit {@link QueryPolicy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Query policy
     * @param statement Query statement
     * @return Wrapped {@link Flux} of matching {@link KeyRecord}s
     */
    public Flux<KeyRecord> query(final String op, final QueryPolicy policy, final Statement statement) {
        return flux(op, () -> reactorClient.query(policy, statement));
    }

    /**
     * Escape hatch for a single-value Aerospike reactor call not otherwise exposed by this
     * accessor: applies {@code fn} to the wrapped reactor client under the full spine.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from the Aerospike reactor client to its driver {@link Mono}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    public <T> Mono<T> command(final String op, final Function<IAerospikeReactorClient, Mono<T>> fn) {
        return mono(op, () -> fn.apply(reactorClient));
    }

    /**
     * Escape hatch for a stream-returning Aerospike reactor call not otherwise exposed by this
     * accessor: applies {@code fn} to the wrapped reactor client under the full spine.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from the Aerospike reactor client to its driver {@link Flux}
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    public <T> Flux<T> queryMany(final String op, final Function<IAerospikeReactorClient, Flux<T>> fn) {
        return flux(op, () -> fn.apply(reactorClient));
    }

    /**
     * Raw escape-hatch accessor for the wrapped reactor client, for callers that need direct
     * access outside the {@code mono}/{@code flux} spine. Callers must never call {@link
     * IAerospikeReactorClient#close()} on the returned instance (see class Javadoc).
     *
     * @return The wrapped, non-owning {@link IAerospikeReactorClient}
     */
    public IAerospikeReactorClient reactor() {
        return reactorClient;
    }

    /**
     * Maps Aerospike driver failures to {@link AerospikeAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * AerospikeAccessorException} so behavior stays identical to the synchronous twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return AerospikeAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level Aerospike error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
