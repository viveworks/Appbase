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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous, service-acquired accessor for Aerospike, built on the Aerospike Java client's
 * synchronous API. Each operation borrows an {@link AppbaseClientIntegrationService} via {@link
 * #borrow}, runs the supplied driver call against its {@link IAerospikeClient}, and calls {@link
 * AppbaseClientIntegrationService#finalizeOperation()} on completion or failure.
 *
 * <p>Unlike Kafka's pooled producer/consumer accessors, the underlying {@link IAerospikeClient} is
 * a <strong>thread-safe singleton</strong> shared by the whole integration service, not a
 * per-operation pooled resource: there is nothing to check out or leak. {@link #borrow} therefore
 * returns the very same integration service on every call, and {@code finalizeOperation()} is
 * invoked purely for spine symmetry with the pooled accessors (e.g. {@code CassandraAccessor}),
 * not because it releases anything back to a pool.
 *
 * <p>The no-{@link Policy}/{@link WritePolicy} overloads ({@link #get(String, Key)}, {@link
 * #put(String, Key, Bin...)}, {@link #delete(String, Key)}, {@link #exists(String, Key)}) pass
 * {@code null} for the policy argument, which is the Aerospike driver's own documented convention
 * for "use the client's configured default policy" -- this is intentional, not an omission.
 *
 * <p>Cross-cutting concerns (metrics, tracing, and exception-store reporting) are supplied by the
 * inherited {@link AbstractAppbaseAccessor}; this class contributes only the driver operation and
 * Aerospike error mapping, shared with the reactive twin ({@code ReactiveAerospikeAccessor}) via
 * {@link AerospikeAccessorException#map(Throwable, Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AerospikeAccessor extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of the Aerospike integration service, invoked once per operation.
     *               Since the underlying client is a thread-safe singleton, this always returns
     *               the same integration service rather than checking one out of a pool
     */
    public AerospikeAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue>> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Fetch a record by key, using the client's default read policy ({@code null} -- see class
     * Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @return The {@link Record}, or {@code null} if absent
     * @throws Exception The mapped exception, if the operation fails
     */
    public Record get(final String op, final Key key) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(key, "key");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().get(null, key);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Fetch a record by key, using an explicit read {@link Policy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Read policy
     * @param key Record key
     * @return The {@link Record}, or {@code null} if absent
     * @throws Exception The mapped exception, if the operation fails
     */
    public Record get(final String op, final Policy policy, final Key key) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(policy, "policy");
        AppbaseUtil.notNull(key, "key");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().get(policy, key);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Write one or more bins to a record, using the client's default write policy ({@code null} --
     * see class Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @param bins Bins to write
     * @throws Exception The mapped exception, if the operation fails
     */
    public void put(final String op, final Key key, final Bin... bins) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(key, "key");
        execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                svc.getClient().put(null, key, bins);
                return null;
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Write one or more bins to a record, using an explicit {@link WritePolicy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Write policy
     * @param key Record key
     * @param bins Bins to write
     * @throws Exception The mapped exception, if the operation fails
     */
    public void put(final String op, final WritePolicy policy, final Key key, final Bin... bins) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(policy, "policy");
        AppbaseUtil.notNull(key, "key");
        execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                svc.getClient().put(policy, key, bins);
                return null;
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Delete a record by key, using the client's default write policy ({@code null} -- see class
     * Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @return {@code true} if a record existed and was deleted
     * @throws Exception The mapped exception, if the operation fails
     */
    public boolean delete(final String op, final Key key) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(key, "key");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().delete(null, key);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Delete a record by key, using an explicit {@link WritePolicy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Write policy
     * @param key Record key
     * @return {@code true} if a record existed and was deleted
     * @throws Exception The mapped exception, if the operation fails
     */
    public boolean delete(final String op, final WritePolicy policy, final Key key) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(policy, "policy");
        AppbaseUtil.notNull(key, "key");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().delete(policy, key);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Check whether a record exists, using the client's default read policy ({@code null} -- see
     * class Javadoc).
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param key Record key
     * @return {@code true} if the record exists
     * @throws Exception The mapped exception, if the operation fails
     */
    public boolean exists(final String op, final Key key) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(key, "key");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().exists(null, key);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a record-level read/write operation batch (e.g. increment, append), using an explicit
     * {@link WritePolicy}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param policy Write policy
     * @param key Record key
     * @param operations Operations to apply
     * @return The resulting {@link Record}
     * @throws Exception The mapped exception, if the operation fails
     */
    public Record operate(final String op, final WritePolicy policy, final Key key, final Operation... operations)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(policy, "policy");
        AppbaseUtil.notNull(key, "key");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().operate(policy, key, operations);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Escape hatch for a single-value Aerospike sync call not otherwise exposed by this accessor:
     * applies {@code commandFn} to the borrowed {@link IAerospikeClient} under the full spine.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from the Aerospike client to the result
     * @param <T> Value type
     * @return Result of {@code commandFn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final Function<IAerospikeClient, T> commandFn) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> svc = borrow.get();
            try {
                return commandFn.apply(svc.getClient());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Raw escape-hatch accessor for the underlying {@link IAerospikeClient}, for callers that need
     * direct access outside the {@code execute} spine. Since the Aerospike client is a thread-safe
     * singleton (see class Javadoc), there is no pool to leak by holding onto the returned
     * reference -- but calls made directly through it bypass this accessor's metrics, tracing, and
     * exception-store reporting. Prefer {@link #command(String, Function)} when those cross-cutting
     * concerns matter.
     *
     * @return The borrowed {@link IAerospikeClient}
     */
    public IAerospikeClient client() {
        return borrow.get().getClient();
    }

    /**
     * Maps Aerospike driver failures to {@link AerospikeAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * AerospikeAccessorException} so behavior stays identical to the reactive twin.
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
