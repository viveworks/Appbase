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

package com.viveworks.appbase.client.service.kafka.consumer.reactive;

import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.KafkaAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reactive consumer accessor over reactor-kafka's {@link KafkaReceiver}. A consumer {@code receive}
 * is a LONG-LIVED, unbounded subscription (an infinite stream of records that flows for the life of
 * the consumer), so it deliberately rides the LIGHTER cross-cutting spine
 * {@link AbstractReactiveAccessor#fluxStream(String, java.util.function.Supplier)} — error mapping,
 * failure reporting and scheduler dispatch ONLY — rather than the full
 * {@link #mono(String, java.util.function.Supplier)}/{@link #flux(String, java.util.function.Supplier)}
 * spine used by the discrete producer {@code send}. Each per-operation operator of the full spine
 * assumes ONE bounded call and is actively harmful on an infinite stream:
 * <ul>
 *   <li>a per-op {@code timeout} would kill a healthy consumer on an idle topic (inactivity is
 *       normal, not a fault);</li>
 *   <li>{@code retry} would resubscribe → tear down the consumer and trigger a consumer-group
 *       rebalance, not a cheap replay;</li>
 *   <li>a per-op {@code Timer}/span opened on subscribe never closes on an infinite stream
 *       (metric/span leak);</li>
 *   <li>an extra {@code onBackpressureBuffer} fights the receiver's own backpressure protocol and
 *       risks unbounded growth (OOM).</li>
 * </ul>
 * See the {@code fluxStream} Javadoc for the full rationale.
 *
 * <p><b>No accessor-level per-record counter.</b> This accessor intentionally does NOT add a
 * {@code doOnNext} throughput counter. Per-record consumer throughput
 * ({@code records-consumed-total}/{@code -rate}, bytes, consumer lag) is exported by the Kafka
 * driver-native {@link KafkaClientMetrics} bound on the receiver's consumer (Task 8b) → composite
 * meter registry → JMX. When {@code driverMetricsEnabled} is set, {@link #receive(String)} binds that
 * binder via {@link KafkaReceiver#doOnConsumer} tied to the receive STREAM lifecycle (bound on the
 * first record, closed on stream terminate/cancel), because the receiver's underlying consumer only
 * exists during an active subscription — see {@link #bindDriverMetrics(Flux)}. Duplicating this with
 * an accessor counter is avoided by design.</p>
 *
 * <p>The {@link KafkaReceiver} is INJECTED (built and owned by the consumer client service during
 * acquisition, Task 7); this accessor never opens a connection itself and contributes only the
 * driver operation and Kafka error mapping.</p>
 *
 * @param <K> Record key type
 * @param <V> Record value type
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveKafkaConsumerAccessor<K, V> extends AbstractReactiveAccessor {

    private final KafkaReceiver<K, V> receiver;
    private final boolean driverMetricsEnabled;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param receiver Injected reactor-kafka receiver, owned and closed by the consumer client service
     * @param driverMetricsEnabled When {@code true}, {@link #receive(String)} binds Kafka driver-native
     *                             {@link KafkaClientMetrics} on the receiver's consumer for the life of
     *                             the receive stream (see {@link #bindDriverMetrics(Flux)})
     */
    public ReactiveKafkaConsumerAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final KafkaReceiver<K, V> receiver,
            final boolean driverMetricsEnabled) {
        super(appbaseCore, reactorService, configuration);
        this.receiver = AppbaseUtil.notNull(receiver, "receiver");
        this.driverMetricsEnabled = driverMetricsEnabled;
    }

    /**
     * Long-lived receive stream on the LIGHTER {@code fluxStream} spine: subscribes to the injected
     * receiver's infinite {@link Flux} of {@link ReceiverRecord}s, applying only Kafka error mapping,
     * failure reporting and scheduler dispatch — no per-op timeout/retry/timer/span/backpressure
     * buffer (see the class Javadoc for why those are harmful on an unbounded consumer stream).
     *
     * @param op Operation name, used for failure reporting
     * @return Wrapped long-lived {@link Flux} of {@link ReceiverRecord}s
     */
    public Flux<ReceiverRecord<K, V>> receive(final String op) {
        AppbaseUtil.notNull(op, "op");
        final Flux<ReceiverRecord<K, V>> stream = fluxStream(op, receiver::receive);
        if (!driverMetricsEnabled) {
            return stream;
        }
        return bindDriverMetrics(stream);
    }

    /**
     * Binds Kafka driver-native {@link KafkaClientMetrics} on the receiver's underlying consumer for
     * the LIFETIME OF THE RECEIVE STREAM, then closes the binder when the stream ends. The binder's
     * lifetime is exactly the receive subscription's lifetime.
     *
     * <p><b>Why bind on the first record, not eagerly:</b> {@link KafkaReceiver#doOnConsumer} throws
     * {@link IllegalStateException} if invoked before a {@code receive*()} subscription is active — the
     * receiver's consumer only exists during an active subscription. A record having flowed guarantees
     * the subscription is live, so binding is triggered from {@code doOnNext} and guarded by
     * {@code bindStarted.compareAndSet} to run exactly once.</p>
     *
     * <p><b>Async-store race:</b> {@code doOnConsumer} runs its function on the receiver's own
     * scheduler, so the binder is stored ASYNCHRONOUSLY via {@code subscribe}. A stream that terminates
     * before or after that store must still close the binder (otherwise the driver metrics leak):
     * <ul>
     *   <li>if the binder is stored while the stream is still live, {@code doFinally} later closes it;</li>
     *   <li>if the stream has ALREADY terminated when the binder arrives, the {@code terminated} re-check
     *       inside the {@code subscribe} success callback closes the late binder immediately.</li>
     * </ul>
     * {@code terminated} is set in {@code doFinally} and re-read in the success callback to close either
     * side of the race. The composite registry is NEVER shut down — only the binder is closed.
     *
     * @param stream The wrapped long-lived receive stream to attach the binder lifecycle to
     * @return The stream augmented with driver-metrics bind-on-first-record / close-on-terminate
     */
    private Flux<ReceiverRecord<K, V>> bindDriverMetrics(final Flux<ReceiverRecord<K, V>> stream) {
        final AtomicReference<KafkaClientMetrics> binderRef = new AtomicReference<>();
        final AtomicBoolean bindStarted = new AtomicBoolean(false);
        final AtomicBoolean terminated = new AtomicBoolean(false);
        return stream
                .doOnNext(record -> {
                    // Bind exactly once, on the FIRST record: doOnConsumer requires an active receive
                    // subscription (throws IllegalStateException if called before), which is guaranteed
                    // once a record has flowed. The bind runs on the receiver's scheduler, so it stores
                    // the binder asynchronously; the terminated re-check below closes a binder that
                    // arrives after the stream has already ended (otherwise it would leak).
                    if (bindStarted.compareAndSet(false, true)) {
                        receiver.doOnConsumer(consumer -> {
                            final KafkaClientMetrics binder = new KafkaClientMetrics(consumer);
                            binder.bindTo(appbaseCore.getMetricService().getCompositeMeterRegistry());
                            return binder;
                        }).subscribe(
                                binder -> {
                                    binderRef.set(binder);
                                    if (terminated.get()) {
                                        final KafkaClientMetrics late = binderRef.getAndSet(null);
                                        if (late != null) {
                                            late.close();
                                        }
                                    }
                                },
                                error -> logger.warn("Failed to bind Kafka consumer driver metrics", error));
                    }
                })
                .doFinally(signalType -> {
                    // Stream ended (complete/error/cancel): the receiver's consumer is being torn down,
                    // so close the binder. terminated=true ensures a binder that arrives after this also
                    // gets closed.
                    terminated.set(true);
                    final KafkaClientMetrics binder = binderRef.getAndSet(null);
                    if (binder != null) {
                        binder.close();
                    }
                });
    }

    /**
     * Escape hatch: the injected receiver, for advanced reactor-kafka usage not covered by
     * {@link #receive(String)} (e.g. {@link KafkaReceiver#doOnConsumer}). Lifecycle stays with the
     * owning consumer client service; callers must not close it.
     *
     * @return The injected {@link KafkaReceiver}
     */
    public KafkaReceiver<K, V> receiver() {
        return receiver;
    }

    /**
     * Maps Kafka driver failures to {@link KafkaAccessorException}; all other throwables delegate to
     * the inherited default mapping. Uses the shared static helper in {@link KafkaAccessorException}
     * so behavior stays identical to the synchronous twin and the producer accessor.
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
