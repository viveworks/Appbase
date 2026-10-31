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
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NOTE: {@code reactor-test}'s {@code StepVerifier} is not a declared dependency of {@code appbase-core}
 * (every other module that uses reactor-core reactively declares it as a test-scope dependency; this one
 * does not — see N02/internal work item 1073, which stands up the core-module BlockHound/reactor-test harness). Per the "add
 * no new dependency" constraint this suite asserts observable behaviour via {@link Mono#block(Duration)}
 * instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultReactiveReadBoundaryTest {
    private static ReactiveReadBoundary newBoundaryWithBoundedElastic(final int offloadTimeoutSeconds) {
        final ReactiveReadBoundaryConfiguration configuration = ReactiveReadBoundaryConfiguration
                .builder()
                .offloadTimeoutSeconds(offloadTimeoutSeconds)
                .build();

        return new DefaultReactiveReadBoundary(configuration, mock(ReactorService.class));
    }

    @Test
    void readEmitsBlockingResultAndCompletes() {
        final ReactiveReadBoundary boundary = newBoundaryWithBoundedElastic(5);

        final String result = boundary.read(() -> "hello").block(Duration.ofSeconds(5));

        assertEquals("hello", result);
    }

    @Test
    void readPropagatesUnderlyingException() {
        final ReactiveReadBoundary boundary = newBoundaryWithBoundedElastic(5);
        final IOException boom = new IOException("disk");
        final Mono<String> mono = boundary.read(() -> {
            throw boom;
        });

        final Throwable thrown = assertThrows(Throwable.class, () -> mono.block(Duration.ofSeconds(5)));

        assertTrue(thrown == boom || thrown.getCause() == boom,
                "expected the underlying IOException to propagate, got " + thrown);
    }

    @Test
    void readTimesOutWhenBlockingExceedsDeadline() {
        final ReactiveReadBoundary boundary = newBoundaryWithBoundedElastic(1);
        final Mono<String> mono = boundary.read(() -> {
            Thread.sleep(3000);
            return "late";
        });

        final Throwable thrown = assertThrows(Throwable.class, () -> mono.block(Duration.ofSeconds(5)));

        assertTrue(thrown instanceof TimeoutException || thrown.getCause() instanceof TimeoutException,
                "expected a TimeoutException, got " + thrown);
    }

    @Test
    void unknownSchedulerIdIsRejectedNotSilentlyNull() {
        final ReactorService reactorService = mock(ReactorService.class);
        when(reactorService.getSchedulerById("missing")).thenReturn(null);
        final ReactiveReadBoundaryConfiguration configuration = ReactiveReadBoundaryConfiguration
                .builder()
                .offloadSchedulerId("missing")
                .build();

        assertThrows(AppbaseRuntimeException.class,
                () -> new DefaultReactiveReadBoundary(configuration, reactorService));
    }

    @Test
    void cooperativeReadObservesCancellationOnTimeout() throws Exception {
        final ReactiveReadBoundary boundary = newBoundaryWithBoundedElastic(1);
        final AtomicBoolean sawCancel = new AtomicBoolean(false);
        final CountDownLatch done = new CountDownLatch(1);

        final Mono<String> mono = boundary.read((BooleanSupplier cancelled) -> {
            for (int i = 0; i < 100; i++) {
                if (cancelled.getAsBoolean()) {
                    sawCancel.set(true);
                    done.countDown();
                    return "aborted";
                }
                Thread.sleep(50);
            }
            done.countDown();
            return "finished";
        });

        final Throwable thrown = assertThrows(Throwable.class, () -> mono.block(Duration.ofSeconds(10)));

        assertTrue(thrown instanceof TimeoutException || thrown.getCause() instanceof TimeoutException,
                "expected a TimeoutException, got " + thrown);
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertTrue(sawCancel.get(), "the read must observe cooperative cancellation, not run to completion");
    }

    @Test
    void readUsesResolvedSchedulerWhenIdConfigured() {
        final Scheduler dedicated = Schedulers.newSingle("n01-test-resolved");
        try {
            final ReactorService reactorService = mock(ReactorService.class);
            when(reactorService.getSchedulerById("dedicated")).thenReturn(dedicated);
            final ReactiveReadBoundaryConfiguration configuration = ReactiveReadBoundaryConfiguration
                    .builder()
                    .offloadSchedulerId("dedicated")
                    .build();
            final ReactiveReadBoundary boundary = new DefaultReactiveReadBoundary(configuration, reactorService);

            final String threadName =
                    boundary.read(() -> Thread.currentThread().getName()).block(Duration.ofSeconds(5));

            assertTrue(threadName != null && threadName.contains("n01-test-resolved"),
                    "expected the blocking work to run on the resolved scheduler, got thread name "
                            + threadName);
        } finally {
            dedicated.dispose();
        }
    }

    @Test
    void maxPageSizeAndTimeoutDelegateToConfiguration() {
        final ReactiveReadBoundaryConfiguration configuration = ReactiveReadBoundaryConfiguration
                .builder()
                .maxPageSize(250)
                .offloadTimeoutSeconds(7)
                .build();
        final ReactiveReadBoundary boundary =
                new DefaultReactiveReadBoundary(configuration, mock(ReactorService.class));

        assertEquals(250, boundary.maxPageSize());
        assertEquals(Duration.ofSeconds(7), boundary.timeout());
    }

    @Test
    void cancellableReadEmitsResultAndCompletes() {
        final ReactiveReadBoundary boundary = newBoundaryWithBoundedElastic(5);

        final String result = boundary.read((BooleanSupplier cancelled) -> "ok").block(Duration.ofSeconds(5));

        assertEquals("ok", result);
    }

    @Test
    void cancellableReadPropagatesUnderlyingException() {
        final ReactiveReadBoundary boundary = newBoundaryWithBoundedElastic(5);
        final IOException boom = new IOException("disk");
        final Mono<String> mono = boundary.read((BooleanSupplier cancelled) -> {
            throw boom;
        });

        final Throwable thrown = assertThrows(Throwable.class, () -> mono.block(Duration.ofSeconds(5)));

        assertTrue(thrown == boom || thrown.getCause() == boom,
                "expected the underlying IOException to propagate, got " + thrown);
    }

    @Test
    void blankSchedulerIdConstructsWithNullReactorServiceAndReads() {
        // Regression guard: a default config (blank offloadSchedulerId) must NOT require a ReactorService.
        // It resolves to Schedulers.boundedElastic(), so reactive reads work with Reactor DISABLED — a
        // deployment can enable Lucene without enabling Reactor. Passing null here must not throw.
        final ReactiveReadBoundaryConfiguration configuration = ReactiveReadBoundaryConfiguration.builder().build();

        final ReactiveReadBoundary boundary = new DefaultReactiveReadBoundary(configuration, null);

        final String result = boundary.read(() -> "no-reactor").block(Duration.ofSeconds(5));
        assertEquals("no-reactor", result);
    }

    @Test
    void configuredSchedulerIdWithNullReactorServiceIsRejected() {
        // The mirror: a configured id can only be resolved through the ReactorService registry, so with no
        // ReactorService, construction must refuse rather than silently fall back to a different scheduler.
        final ReactiveReadBoundaryConfiguration configuration = ReactiveReadBoundaryConfiguration
                .builder()
                .offloadSchedulerId("needs-reactor")
                .build();

        assertThrows(AppbaseRuntimeException.class,
                () -> new DefaultReactiveReadBoundary(configuration, null));
    }
}
