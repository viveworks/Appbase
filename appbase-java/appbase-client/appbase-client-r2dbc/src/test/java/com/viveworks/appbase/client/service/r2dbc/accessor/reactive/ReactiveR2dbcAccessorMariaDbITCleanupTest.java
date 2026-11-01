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
package com.viveworks.appbase.client.service.r2dbc.accessor.reactive;

import com.viveworks.appbase.client.service.r2dbc.DefaultR2dbcClientService;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Offline cleanup and cancellation regressions folded from internal work item 2485 into the current richer IT.
 *
 * @author Navaneeth Mysore G
 */
class ReactiveR2dbcAccessorMariaDbITCleanupTest {
    @Test
    void dropFailureRemainsPrimaryAndShutdownFailureIsSuppressed() {
        final DefaultR2dbcClientService service = Mockito.mock(DefaultR2dbcClientService.class);
        final ReactiveR2dbcAccessor accessor = Mockito.mock(ReactiveR2dbcAccessor.class);
        final RuntimeException drop = new IllegalStateException("drop failed");
        final RuntimeException shutdown = new IllegalStateException("shutdown failed");
        Mockito.when(service.getReactiveAccessor()).thenReturn(accessor);
        Mockito.when(accessor.update("drop-cleanup", "DROP TABLE IF EXISTS table_one"))
                .thenReturn(Mono.error(drop));
        Mockito.doThrow(shutdown).when(service).shutdown();

        final RuntimeException reported = Assertions.assertThrows(RuntimeException.class,
                () -> ReactiveR2dbcAccessorMariaDbIT.cleanup(service, "table_one"));

        Assertions.assertSame(drop, reported);
        Assertions.assertTrue(Arrays.asList(reported.getSuppressed()).contains(shutdown),
                "shutdown failure must remain suppressed on the primary DROP failure");
    }

    @Test
    void cleanupTimeoutCancelsPublisherAndStillShutsDown() {
        final DefaultR2dbcClientService service = Mockito.mock(DefaultR2dbcClientService.class);
        final ReactiveR2dbcAccessor accessor = Mockito.mock(ReactiveR2dbcAccessor.class);
        final AtomicBoolean cancelled = new AtomicBoolean();
        Mockito.when(service.getReactiveAccessor()).thenReturn(accessor);
        Mockito.when(accessor.update("drop-cleanup", "DROP TABLE IF EXISTS table_two"))
                .thenReturn(Mono.<Long>never().doOnCancel(() -> cancelled.set(true)));

        final RuntimeException failure = Assertions.assertThrows(RuntimeException.class,
                () -> ReactiveR2dbcAccessorMariaDbIT.cleanup(service, "table_two"));

        Assertions.assertInstanceOf(TimeoutException.class, failure.getCause());
        Assertions.assertTrue(cancelled.get(), "publisher-level timeout must cancel upstream");
        Mockito.verify(service).shutdown();
    }

    @Test
    void everyBoundedOperationCancelsAStalledPublisher() {
        final AtomicBoolean cancelled = new AtomicBoolean();

        StepVerifier.create(ReactiveR2dbcAccessorMariaDbIT.bounded(
                        Mono.<Long>never().doOnCancel(() -> cancelled.set(true))))
                .expectError(TimeoutException.class)
                .verify();

        Assertions.assertTrue(cancelled.get(), "operation timeout must cancel upstream");
    }

    @Test
    void tableNamesAndCleanupSqlAreRunScoped() {
        final String first = ReactiveR2dbcAccessorMariaDbIT.uniqueTableName();
        final String second = ReactiveR2dbcAccessorMariaDbIT.uniqueTableName();
        Assertions.assertNotEquals(first, second);
        Assertions.assertEquals("DROP TABLE IF EXISTS " + first,
                ReactiveR2dbcAccessorMariaDbIT.cleanupSql(first));
    }
}
