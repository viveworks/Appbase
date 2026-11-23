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

package com.viveworks.appbase.core.service.reactive.read;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Bridges a blocking read (e.g. Lucene file I/O) onto a bounded {@link Mono} — offloaded to a
 * {@link reactor.core.scheduler.Scheduler}, deadlined by {@link #timeout()}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ReactiveReadBoundary {
    /**
     * Offload a blocking read, bounded by {@link #timeout()}.
     *
     * <p><b>Only the downstream {@link Mono} signal is bounded by the timeout — the underlying blocking
     * work itself is NOT interrupted.</b> Past the deadline this method's {@link Mono} errors with a
     * {@link java.util.concurrent.TimeoutException}, but {@code blockingRead} keeps running to completion
     * on the offload scheduler regardless; its eventual result or exception is simply discarded. A caller
     * that passes an unbounded or indefinitely-blocking {@code Callable} therefore holds a worker from the
     * offload scheduler's pool (by default the shared, process-wide {@code boundedElastic} pool) until that
     * call returns naturally — callers must only pass page-bounded, short blocking reads.
     *
     * <p>A {@code blockingRead} that returns {@code null} yields an <b>empty</b> {@link Mono}, per
     * {@link reactor.core.publisher.MonoSink#success(Object)}'s contract for a {@code null} value — this is
     * not treated as an error.
     *
     * @param blockingRead Blocking read to offload
     * @param <T> Type of value read
     * @return a {@link Mono} that emits the read result, completes empty if {@code blockingRead} returns
     *      {@code null}, or errors (including with a {@link java.util.concurrent.TimeoutException} past the
     *      deadline)
     */
    <T> Mono<T> read(Callable<T> blockingRead);

    /**
     * Offload a blocking, cooperatively-cancellable read, bounded by {@link #timeout()}. If the deadline
     * elapses, the read's cancellation flag is set so a well-behaved {@link CancellableRead} can abort
     * early rather than continuing to hold the offload worker.
     *
     * @param cancellableRead Blocking, cooperatively-cancellable read to offload
     * @param <T> Type of value read
     * @return a {@link Mono} that emits the read result or errors (including with a
     *      {@link java.util.concurrent.TimeoutException} past the deadline)
     */
    <T> Mono<T> read(CancellableRead<T> cancellableRead);

    /**
     * Get the maximum number of items returned by a single bounded read.
     *
     * @return max page size
     */
    int maxPageSize();

    /**
     * Get the deadline after which an offloaded read is timed out.
     *
     * @return offload timeout
     */
    Duration timeout();
}
