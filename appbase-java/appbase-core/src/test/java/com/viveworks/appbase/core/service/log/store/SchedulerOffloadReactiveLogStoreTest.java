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

package com.viveworks.appbase.core.service.log.store;

import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SchedulerOffloadReactiveLogStore} — a {@link ReactiveLogStore} adapter that offloads a
 * non-Lucene {@link LogStore}'s blocking sync reads through the N01 {@link ReactiveReadBoundary}.
 *
 * <p>Per the "add no new dependency" constraint (see {@code DefaultReactiveReadBoundaryTest}'s note),
 * {@code reactor-test}'s {@code StepVerifier} is unavailable — this suite asserts observable behaviour via
 * {@link Mono#block(Duration)} instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class SchedulerOffloadReactiveLogStoreTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private final ReactiveReadBoundary reactiveReadBoundary = CoreTestFixtures.getReactiveReadBoundary();

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(Exception.class,
                () -> new SchedulerOffloadReactiveLogStore(null, reactiveReadBoundary));
    }

    @Test
    void constructorRejectsNullBoundary() {
        final LogStore delegate = mock(LogStore.class);

        assertThrows(Exception.class,
                () -> new SchedulerOffloadReactiveLogStore(delegate, null));
    }

    @Test
    void betweenReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final LogStore delegate = mock(LogStore.class);
        final LogRecord record = record("MARKER0");
        when(delegate.readLogRecordsBetween(anyLong(), anyLong(), anyInt())).thenReturn(List.of(record));
        final SchedulerOffloadReactiveLogStore store =
                new SchedulerOffloadReactiveLogStore(delegate, reactiveReadBoundary);

        final Collection<LogRecord> result =
                store.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10).block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains(record));
        verify(delegate).readLogRecordsBetween(0, Long.MAX_VALUE, 10);
    }

    @Test
    void atReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final LogStore delegate = mock(LogStore.class);
        final LogRecord record = record("MARKER1");
        when(delegate.readLogRecordsAt(anyLong(), anyInt())).thenReturn(List.of(record));
        final SchedulerOffloadReactiveLogStore store =
                new SchedulerOffloadReactiveLogStore(delegate, reactiveReadBoundary);

        final Collection<LogRecord> result =
                store.readLogRecordsAtReactive(42L, 10).block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains(record));
        verify(delegate).readLogRecordsAt(42L, 10);
    }

    @Test
    void byCriteriaReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final LogStore delegate = mock(LogStore.class);
        final LogRecord record = record("MARKER2");
        final LogRecordCriteria criteria = LogRecordCriteria.builder().markerName("MARKER2").build();
        when(delegate.readLogRecordsByCriteria(any(), anyInt())).thenReturn(List.of(record));
        final SchedulerOffloadReactiveLogStore store =
                new SchedulerOffloadReactiveLogStore(delegate, reactiveReadBoundary);

        final Collection<LogRecord> result =
                store.readLogRecordsByCriteriaReactive(criteria, 10).block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains(record));
        verify(delegate).readLogRecordsByCriteria(criteria, 10);
    }

    @Test
    void delegateErrorSurfacesThroughTheMono() throws Exception {
        final LogStore delegate = mock(LogStore.class);
        final LogStoreException boom = new LogStoreException("boom");
        when(delegate.readLogRecordsBetween(anyLong(), anyLong(), anyInt())).thenThrow(boom);
        final SchedulerOffloadReactiveLogStore store =
                new SchedulerOffloadReactiveLogStore(delegate, reactiveReadBoundary);

        final Mono<Collection<LogRecord>> mono = store.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10);

        final Throwable thrown = assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));
        assertTrue(thrown == boom || thrown.getCause() == boom,
                "expected the underlying LogStoreException to propagate, got " + thrown);
    }

    private static LogRecord record(final String markerName) {
        return CoreTestFixtures.getLogRecord(
                "fmt {}", "msg ARG", "Throwable message", "Cause Message",
                "Key", "Value", markerName, System.currentTimeMillis(),
                CoreTestFixtures.class.getName(), LogLevel.INFO);
    }
}
