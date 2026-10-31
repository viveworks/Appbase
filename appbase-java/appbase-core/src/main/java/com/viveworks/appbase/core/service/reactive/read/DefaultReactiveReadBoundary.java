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

package com.viveworks.appbase.core.service.reactive.read;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default {@link ReactiveReadBoundary}. Resolves its offload {@link Scheduler} <b>once</b>, at
 * construction: a blank/{@code null} {@link ReactiveReadBoundaryConfiguration#getOffloadSchedulerId()}
 * resolves to {@link Schedulers#boundedElastic()}; otherwise the id is resolved via
 * {@link ReactorService#getSchedulerById(String)}, and an unknown id fails construction with an
 * {@link AppbaseRuntimeException} rather than silently handing a {@code null} {@link Scheduler} downstream.
 *
 * <p>Both {@link #read(Callable)} and {@link #read(CancellableRead)} schedule the blocking work via
 * {@link Scheduler#schedule(Runnable)} inside {@link Mono#create}, with the returned {@code Disposable}
 * left undisposed on cancellation — <b>not</b> {@code Mono.fromCallable(...).subscribeOn(scheduler)}, whose
 * cancellation path disposes the in-flight task via {@code Future#cancel(true)}, i.e. {@link Thread#interrupt()}.
 * A single Lucene {@code search(query, pageSize)} call is {@code pageSize}-bounded and cannot be interrupted
 * mid-call; {@link #read(CancellableRead)} bounds the <i>multi-document fetch loop</i> instead, via
 * cooperative cancellation — the read observes a checked {@code BooleanSupplier} flag, flipped by the
 * sink's {@code onCancel} hook when {@link #timeout()} fires, rather than {@link Thread#interrupt()}, which
 * would throw {@code ClosedByInterruptException} and close the underlying {@code NIOFSDirectory}. A read
 * that does not observe the flag in time keeps running to completion in the background rather than being
 * interrupted; residual worker-hold past the deadline is bounded by {@code maxPageSize} and the offload
 * scheduler's own pool/queue caps, not by this class.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultReactiveReadBoundary implements ReactiveReadBoundary {
    private final ReactiveReadBoundaryConfiguration configuration;
    private final Scheduler scheduler;

    @Inject
    public DefaultReactiveReadBoundary(
            final ReactiveReadBoundaryConfiguration configuration,
            @Nullable final ReactorService reactorService)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        this.configuration = configuration;

        final String offloadSchedulerId = configuration.getOffloadSchedulerId();
        if (AppbaseUtil.isBlank(offloadSchedulerId)) {
            // No configured scheduler id: use the shared bounded-elastic scheduler. ReactorService is NOT
            // consulted in this (default) case, so it may be absent — a deployment can enable Lucene
            // reactive reads without also enabling Reactor. Requiring it here would be a false dependency.
            this.scheduler = Schedulers.boundedElastic();
        } else {
            // A configured id must be resolved through the ReactorService scheduler registry; without it,
            // the id cannot be honoured, so refuse rather than silently fall back to a different scheduler.
            if (reactorService == null) {
                throw new AppbaseRuntimeException(
                        "offloadSchedulerId '" + offloadSchedulerId
                                + "' is configured but no ReactorService is available to resolve it");
            }
            final Scheduler resolved = reactorService.getSchedulerById(offloadSchedulerId);
            if (resolved == null) {
                throw new AppbaseRuntimeException(
                        "no scheduler registered for id '" + offloadSchedulerId + "'");
            }

            this.scheduler = resolved;
        }
    }

    @Override
    public <T> Mono<T> read(final Callable<T> blockingRead) {
        AppbaseUtil.notNull(blockingRead, "blockingRead");

        // Deliberately NOT `Mono.fromCallable(...).subscribeOn(scheduler)`: on cancellation (e.g. this
        // read's own .timeout() firing below), Reactor's schedulers dispose the in-flight Callable task via
        // Future#cancel(true) — i.e. Thread#interrupt() — which is exactly what must never reach a blocking
        // Lucene read (it throws ClosedByInterruptException and closes the NIOFSDirectory). Mono.create +
        // Scheduler#schedule(Runnable), with the returned Disposable left undisposed, lets the scheduled
        // task keep running to completion in the background — never interrupted — while the downstream
        // Mono terminates on its own schedule; the still-running task's eventual result is a silent no-op
        // once the sink has already terminated.
        return Mono.<T>create(sink -> scheduler.schedule(() -> {
            try {
                sink.success(blockingRead.call());
            } catch (Exception e) {
                sink.error(e);
            }
        })).timeout(configuration.timeout());
    }

    @Override
    public <T> Mono<T> read(final CancellableRead<T> cancellableRead) {
        AppbaseUtil.notNull(cancellableRead, "cancellableRead");

        final AtomicBoolean cancelled = new AtomicBoolean(false);

        // Same non-interrupting scheduling as read(Callable) above, plus the cooperative-cancel wiring:
        // sink.onCancel (fired e.g. when .timeout() below cancels this Mono) flips the flag the still-running
        // CancellableRead observes on its own schedule — never Thread.interrupt().
        return Mono.<T>create(sink -> {
            scheduler.schedule(() -> {
                try {
                    sink.success(cancellableRead.read(cancelled::get));
                } catch (Exception e) {
                    sink.error(e);
                }
            });
            sink.onCancel(() -> cancelled.set(true));
        }).timeout(configuration.timeout());
    }

    @Override
    public int maxPageSize() {
        return configuration.getMaxPageSize();
    }

    @Override
    public Duration timeout() {
        return configuration.timeout();
    }
}
