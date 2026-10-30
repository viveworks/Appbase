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

package com.viveworks.appbase.client.service.kafka.producer.reactive;

import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.KafkaAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

/**
 * Reactive producer accessor over reactor-kafka's {@link KafkaSender}. A producer {@code send} is a
 * DISCRETE, bounded operation (one record or one bounded batch, each yielding a broker ack), so it
 * rides the FULL cross-cutting spine inherited from {@link AbstractReactiveAccessor} via
 * {@link #mono(String, java.util.function.Supplier)}/{@link #flux(String, java.util.function.Supplier)}:
 * per-attempt timeout, retry/backoff, backpressure buffering (flux), the configured scheduler, plus
 * config-gated metrics, tracing, and exception-store reporting. This deliberately contrasts with the
 * consumer {@code receive}, which is a LONG-LIVED stream and therefore uses the lighter
 * {@link AbstractReactiveAccessor#fluxStream(String, java.util.function.Supplier)} spine (no
 * timeout/retry/per-op timer/backpressure buffer, which would be actively harmful on an unbounded
 * stream).
 *
 * <p>The {@link KafkaSender} is INJECTED (built by the producer client service during acquisition);
 * this accessor never opens a connection itself and contributes only the driver operation and Kafka
 * error mapping.</p>
 *
 * <p><b>Delivery semantics — retry can duplicate.</b> When reactive retry is enabled on the
 * {@link ReactiveOperationConfiguration}, a failed {@code send} is re-subscribed and therefore
 * re-sent; against a non-idempotent producer this can produce DUPLICATE records on the topic. For
 * at-least-once-without-duplicates, enable Kafka producer idempotence
 * ({@code enable.idempotence=true}, which also constrains {@code acks=all} and bounded in-flight
 * requests) on the producer configuration, or disable reactive retry for this client. This is an
 * inherent property of retrying a produce, not specific to this accessor.</p>
 *
 * @param <K> Record key type
 * @param <V> Record value type
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveKafkaProducerAccessor<K, V> extends AbstractReactiveAccessor {

    private final KafkaSender<K, V> sender;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param sender Injected reactor-kafka sender, owned and closed by the producer client service
     */
    public ReactiveKafkaProducerAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final KafkaSender<K, V> sender) {
        super(appbaseCore, reactorService, configuration);
        this.sender = AppbaseUtil.notNull(sender, "sender");
    }

    /**
     * Full-spine batch send: publishes {@code records} through the injected sender and emits one
     * {@link SenderResult} per record, with the caller's correlation-metadata type {@code T} flowing
     * through end to end. Per-record failures surface on their own {@link SenderResult}
     * ({@link SenderResult#exception()} non-null) rather than as a terminal error, so a partial
     * batch failure does not abort the remaining sends — callers inspect each result.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param records Records to publish
     * @param <T> Correlation-metadata type carried on each record/result
     * @return Wrapped {@link Flux} of per-record {@link SenderResult}
     */
    public <T> Flux<SenderResult<T>> send(final String op, final Publisher<SenderRecord<K, V, T>> records) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(records, "records");
        return flux(op, () -> sender.send(records));
    }

    /**
     * Full-spine single send: publishes exactly one {@code record} and emits its
     * {@link RecordMetadata}. reactor-kafka reports a per-record failure as a non-null
     * {@link SenderResult#exception()} on an otherwise-successful signal; to let the shared spine's
     * {@code onErrorMap} map it (to {@link KafkaAccessorException}) and drive failure reporting, that
     * per-record exception is re-raised here as an error signal ({@link Mono#error}) instead of being
     * swallowed. A {@code null} correlation metadata is used since the caller only wants the ack.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param record Record to publish
     * @return Wrapped {@link Mono} of the record's {@link RecordMetadata}
     */
    public Mono<RecordMetadata> send(final String op, final ProducerRecord<K, V> record) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(record, "record");
        return mono(op, () ->
                Mono.from(sender.send(Mono.just(SenderRecord.<K, V, Object>create(record, null))))
                        .flatMap(result -> result.exception() != null
                                ? Mono.error(result.exception())
                                : Mono.just(result.recordMetadata())));
    }

    /**
     * Escape hatch: the injected sender, for advanced reactor-kafka usage not covered by the
     * {@code send} helpers (e.g. {@link KafkaSender#doOnProducer}). Lifecycle stays with the owning
     * producer client service; callers must not close it.
     *
     * @return The injected {@link KafkaSender}
     */
    public KafkaSender<K, V> sender() {
        return sender;
    }

    /**
     * Maps Kafka driver failures to {@link KafkaAccessorException}; all other throwables delegate to
     * the inherited default mapping. Uses the shared static helper in {@link KafkaAccessorException}
     * so behavior stays identical to the synchronous twin.
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
