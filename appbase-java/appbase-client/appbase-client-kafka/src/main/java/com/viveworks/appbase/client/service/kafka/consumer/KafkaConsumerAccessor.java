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

package com.viveworks.appbase.client.service.kafka.consumer;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.KafkaAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.KafkaException;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Apache Kafka consumers, built on the Kafka driver's blocking poll path.
 * Each {@link #poll(String, Duration)}/{@link #command(String, Function)} invocation borrows one
 * pooled {@link AppbaseClientIntegrationService}, runs the supplied driver call against its
 * {@link Consumer}, and returns it to the pool on completion or failure. Cross-cutting concerns
 * (metrics, tracing, and exception-store reporting) are supplied by the inherited
 * {@link AbstractAppbaseAccessor}; this class contributes only the driver operation and Kafka error
 * mapping, shared with the reactive twin ({@code ReactiveKafkaConsumerAccessor}) via
 * {@link KafkaAccessorException#map(Throwable, Function)}.
 *
 * <p>{@link #poll(String, Duration)} is a BOUNDED operation — it carries its own {@code timeout} and
 * returns whatever records are available within it — so it rides the FULL cross-cutting spine via
 * the base {@code execute} (per-op timer/span/retry are appropriate for one bounded call). This
 * contrasts with the reactive {@code receive}, an infinite stream that deliberately rides the lighter
 * {@code fluxStream} spine. Unlike the producer's {@code send().get()}, {@link Consumer#poll(Duration)}
 * throws {@link KafkaException} subtypes DIRECTLY (no {@code ExecutionException} wrapping), so the
 * shared {@link KafkaAccessorException#map(Throwable, Function)} maps them without any unwrap helper.
 *
 * @param <K> Record key type
 * @param <V> Record value type
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class KafkaConsumerAccessor<K, V> extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<Consumer<K, V>, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of a pooled Kafka consumer integration service, invoked once per
     *               {@link #poll(String, Duration)}/{@link #command(String, Function)} call
     */
    public KafkaConsumerAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<Consumer<K, V>, ClusterIdValue>> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Blocking poll: borrows a pooled integration service, polls its {@link Consumer} for up to
     * {@code timeout}, then returns the integration service to the pool on completion or failure.
     *
     * <p>Declares {@code throws Exception}, matching the inherited {@code execute} contract: one of
     * the possible mapped failure types, {@link KafkaAccessorException}, is itself a checked
     * exception (it extends {@code AppbaseClientException}/{@code AppbaseException}, not
     * {@code AppbaseRuntimeException}). Catching it here and rethrowing an unchecked wrapper would
     * lose its identity for callers that specifically catch {@link KafkaAccessorException}, so this
     * method surfaces exactly what {@code execute} throws.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param timeout Maximum time the poll blocks waiting for records
     * @return The {@link ConsumerRecords} fetched within {@code timeout}
     * @throws Exception The mapped exception, if the operation fails
     */
    public ConsumerRecords<K, V> poll(final String op, final Duration timeout) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(timeout, "timeout");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<Consumer<K, V>, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().poll(timeout);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a consumer command that returns a single value: borrows a pooled integration service,
     * applies {@code commandFn} to its {@link Consumer}, and returns the integration service to the
     * pool on completion or failure.
     *
     * <p>Declares {@code throws Exception} for the same reason as {@link #poll(String, Duration)}:
     * {@link KafkaAccessorException} is a checked exception whose identity must be preserved for
     * callers.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from a Kafka consumer to the result
     * @param <T> Value type
     * @return Result of {@code commandFn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final Function<Consumer<K, V>, T> commandFn) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<Consumer<K, V>, ClusterIdValue> svc = borrow.get();
            try {
                return commandFn.apply(svc.getClient());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Maps Kafka driver failures to {@link KafkaAccessorException}; all other throwables delegate to
     * the inherited default mapping. Uses the shared static helper in {@link KafkaAccessorException}
     * so behavior stays identical to the reactive twin. {@link Consumer#poll(Duration)} surfaces
     * {@link KafkaException} subtypes directly, so no unwrapping is needed before mapping.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return KafkaAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level Kafka error classification is introduced (mirror Redis/Cassandra).
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
