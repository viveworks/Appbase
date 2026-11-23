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

package com.viveworks.appbase.core.service.exception.store;

import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SchedulerOffloadReactiveExceptionStore} — a {@link ReactiveExceptionStore} adapter that
 * offloads a non-Lucene {@link ExceptionStore}'s blocking sync reads through the N01 {@link
 * ReactiveReadBoundary}.
 *
 * <p>Per the "add no new dependency" constraint (see {@code DefaultReactiveReadBoundaryTest}'s note),
 * {@code reactor-test}'s {@code StepVerifier} is unavailable — this suite asserts observable behaviour via
 * {@link Mono#block(Duration)} instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class SchedulerOffloadReactiveExceptionStoreTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private final ReactiveReadBoundary reactiveReadBoundary = CoreTestFixtures.getReactiveReadBoundary();

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(Exception.class,
                () -> new SchedulerOffloadReactiveExceptionStore(null, reactiveReadBoundary));
    }

    @Test
    void constructorRejectsNullBoundary() {
        final ExceptionStore delegate = mock(ExceptionStore.class);

        assertThrows(Exception.class,
                () -> new SchedulerOffloadReactiveExceptionStore(delegate, null));
    }

    @Test
    void byIdReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final ExceptionStore delegate = mock(ExceptionStore.class);
        final ExceptionMessageDetails details = mock(ExceptionMessageDetails.class);
        when(delegate.readExceptionMessageDetailsById(anyInt())).thenReturn(details);
        final SchedulerOffloadReactiveExceptionStore store =
                new SchedulerOffloadReactiveExceptionStore(delegate, reactiveReadBoundary);

        final ExceptionMessageDetails result =
                store.readExceptionMessageDetailsByIdReactive(7).block(BLOCK_TIMEOUT);

        assertSame(details, result);
        verify(delegate).readExceptionMessageDetailsById(7);
    }

    @Test
    void byIdReactiveWithAbsentIdCompletesEmpty() throws Exception {
        final ExceptionStore delegate = mock(ExceptionStore.class);
        when(delegate.readExceptionMessageDetailsById(anyInt())).thenReturn(null);
        final SchedulerOffloadReactiveExceptionStore store =
                new SchedulerOffloadReactiveExceptionStore(delegate, reactiveReadBoundary);

        // A Mono cannot carry null; a null delegate result completes the Mono empty (block() yields null).
        final ExceptionMessageDetails result =
                store.readExceptionMessageDetailsByIdReactive(7).block(BLOCK_TIMEOUT);

        assertEquals(null, result);
        verify(delegate).readExceptionMessageDetailsById(7);
    }

    @Test
    void readAllReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final ExceptionStore delegate = mock(ExceptionStore.class);
        final ExceptionMessageDetails details = mock(ExceptionMessageDetails.class);
        when(delegate.readAllExceptionMessageDetails()).thenReturn(List.of(details));
        final SchedulerOffloadReactiveExceptionStore store =
                new SchedulerOffloadReactiveExceptionStore(delegate, reactiveReadBoundary);

        final Collection<ExceptionMessageDetails> result =
                store.readAllExceptionMessageDetailsReactive().block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains(details));
        verify(delegate).readAllExceptionMessageDetails();
    }

    @Test
    void byTimestampsReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final ExceptionStore delegate = mock(ExceptionStore.class);
        final ExceptionMessageDetails details = mock(ExceptionMessageDetails.class);
        when(delegate.readAllExceptionMessageDetailsByTimestamps(anyLong(), anyLong()))
                .thenReturn(List.of(details));
        final SchedulerOffloadReactiveExceptionStore store =
                new SchedulerOffloadReactiveExceptionStore(delegate, reactiveReadBoundary);

        final Collection<ExceptionMessageDetails> result =
                store.readAllExceptionMessageDetailsByTimestampsReactive(0, Long.MAX_VALUE).block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains(details));
        verify(delegate).readAllExceptionMessageDetailsByTimestamps(0, Long.MAX_VALUE);
    }

    @Test
    void uniqueThrowableTypeNamesReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final ExceptionStore delegate = mock(ExceptionStore.class);
        when(delegate.readUniqueThrowableTypeNames()).thenReturn(List.of("java.lang.NullPointerException"));
        final SchedulerOffloadReactiveExceptionStore store =
                new SchedulerOffloadReactiveExceptionStore(delegate, reactiveReadBoundary);

        final Collection<String> result =
                store.readUniqueThrowableTypeNamesReactive().block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains("java.lang.NullPointerException"));
        verify(delegate).readUniqueThrowableTypeNames();
    }

    @Test
    void byThrowableTypeNameReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final ExceptionStore delegate = mock(ExceptionStore.class);
        final ExceptionMessageDetails details = mock(ExceptionMessageDetails.class);
        when(delegate.readExceptionMessageDetailsByThrowableTypeName(anyString()))
                .thenReturn(List.of(details));
        final SchedulerOffloadReactiveExceptionStore store =
                new SchedulerOffloadReactiveExceptionStore(delegate, reactiveReadBoundary);

        final Collection<ExceptionMessageDetails> result =
                store.readExceptionMessageDetailsByThrowableTypeNameReactive("java.lang.NullPointerException")
                        .block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains(details));
        verify(delegate).readExceptionMessageDetailsByThrowableTypeName("java.lang.NullPointerException");
    }

    @Test
    void delegateErrorSurfacesThroughTheMono() throws Exception {
        final ExceptionStore delegate = mock(ExceptionStore.class);
        final ExceptionStoreException boom = new ExceptionStoreException("boom");
        when(delegate.readAllExceptionMessageDetails()).thenThrow(boom);
        final SchedulerOffloadReactiveExceptionStore store =
                new SchedulerOffloadReactiveExceptionStore(delegate, reactiveReadBoundary);

        final Mono<Collection<ExceptionMessageDetails>> mono = store.readAllExceptionMessageDetailsReactive();

        final Throwable thrown = assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));
        assertTrue(thrown == boom || thrown.getCause() == boom,
                "expected the underlying ExceptionStoreException to propagate, got " + thrown);
    }
}
