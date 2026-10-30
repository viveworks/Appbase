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

package com.viveworks.appbase.client.service.kafka.producer;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.KafkaAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Apache Kafka producers, built on the Kafka driver's blocking send path.
 * Each {@link #send(String, ProducerRecord)}/{@link #command(String, Function)} invocation borrows
 * one pooled {@link AppbaseClientIntegrationService}, runs the supplied driver call against its
 * {@link Producer}, and returns it to the pool on completion or failure. Cross-cutting concerns
 * (metrics, tracing, and exception-store reporting) are supplied by the inherited
 * {@link AbstractAppbaseAccessor}; this class contributes only the driver operation and Kafka error
 * mapping, shared with the reactive twin ({@code ReactiveKafkaProducerAccessor}) via
 * {@link KafkaAccessorException#map(Throwable, Function)}.
 *
 * @param <K> Record key type
 * @param <V> Record value type
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class KafkaProducerAccessor<K, V> extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<Producer<K, V>, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of a pooled Kafka producer integration service, invoked once per
     *               {@link #send(String, ProducerRecord)}/{@link #command(String, Function)} call
     */
    public KafkaProducerAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<Producer<K, V>, ClusterIdValue>> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Blocking send: borrows a pooled integration service, publishes {@code record} through its
     * {@link Producer} and blocks on the returned ack, then returns the integration service to the
     * pool on completion or failure.
     *
     * <p>Declares {@code throws Exception}, matching the inherited {@code execute} contract: one of
     * the possible mapped failure types, {@link KafkaAccessorException}, is itself a checked
     * exception (it extends {@code AppbaseClientException}/{@code AppbaseException}, not
     * {@code AppbaseRuntimeException}). Catching it here and rethrowing an unchecked wrapper would
     * lose its identity for callers that specifically catch {@link KafkaAccessorException}, so this
     * method surfaces exactly what {@code execute} throws.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param record Record to publish
     * @return Broker ack {@link RecordMetadata} for {@code record}
     * @throws Exception The mapped exception, if the operation fails
     */
    public RecordMetadata send(final String op, final ProducerRecord<K, V> record) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(record, "record");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<Producer<K, V>, ClusterIdValue> svc = borrow.get();
            try {
                return unwrap(svc.getClient().send(record));
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a producer command that returns a single value: borrows a pooled integration service,
     * applies {@code commandFn} to its {@link Producer}, and returns the integration service to the
     * pool on completion or failure.
     *
     * <p>Declares {@code throws Exception} for the same reason as {@link #send(String,
     * ProducerRecord)}: {@link KafkaAccessorException} is a checked exception whose identity must be
     * preserved for callers.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Function from a Kafka producer to the result
     * @param <T> Value type
     * @return Result of {@code commandFn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final Function<Producer<K, V>, T> commandFn) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<Producer<K, V>, ClusterIdValue> svc = borrow.get();
            try {
                return commandFn.apply(svc.getClient());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Blocks on the producer's async send and unwraps the driver failure.
     *
     * <p>Rationale: {@link Producer#send(ProducerRecord)} returns a {@link Future}, and
     * {@link Future#get()} wraps any driver failure in a {@link ExecutionException} whose cause is
     * the real {@code KafkaException}. The shared {@link KafkaAccessorException#map(Throwable,
     * Function)} keys on {@code instanceof KafkaException}, so an un-unwrapped
     * {@link ExecutionException} would NOT map. Rethrowing the cause here lets the spine's
     * {@code mapError} see the real {@code KafkaException} and map it consistently with the reactive
     * face (reactor-kafka surfaces the {@code KafkaException} directly, unwrapped).
     *
     * @param future Producer send future
     * @return The record's {@link RecordMetadata}
     * @throws Exception The unwrapped driver failure, or the original {@link ExecutionException} if
     *                   its cause is not itself an {@link Exception}
     */
    private static RecordMetadata unwrap(final Future<RecordMetadata> future) throws Exception {
        try {
            return future.get();
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw (cause instanceof Exception) ? (Exception) cause : e;
        } catch (final InterruptedException e) {
            // Future.get() clears the interrupt on throw; restore it so callers can observe cancellation.
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * Maps Kafka driver failures to {@link KafkaAccessorException}; all other throwables delegate to
     * the inherited default mapping. Uses the shared static helper in {@link KafkaAccessorException}
     * so behavior stays identical to the reactive twin.
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
