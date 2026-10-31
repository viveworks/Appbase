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

import com.viveworks.appbase.client.core.reactive.ReactiveOperationException;
import com.viveworks.appbase.client.service.r2dbc.DefaultR2dbcClientService;
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientServiceTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * End-to-end reactive round-trip over an in-memory H2 database, driven through a REAL, fully
 * bootstrapped {@link DefaultR2dbcClientService} -- pool manager, keyed integration-service pool,
 * and native {@code ConnectionPool} all live.
 *
 * <p>This is deliberately NOT a duplicate of {@code ReactiveR2dbcAccessorTest}. That test pins a
 * hand-built {@code ConnectionPool} behind a MOCKED {@code R2dbcClientIntegrationService} and a
 * mocked {@code AppbaseCore} spine, so it exercises the accessor's SQL/transaction mechanics in
 * isolation. This test exercises the ACQUISITION path those mocks stand in for: config load ->
 * service start -> pool-manager borrow via {@link DefaultR2dbcClientService#getReactiveAccessor()}
 * -> {@code finalizeOperation()} return. A regression in the borrow/return wiring would pass there
 * and fail here.
 *
 * <p>Named {@code *Test}, not {@code *IT}, ON PURPOSE: H2 is in-memory and needs no external infra,
 * so Surefire must select it in a plain {@code mvn test} and it must stay ungated. This is the
 * first P-027 reactive twin with real CI coverage of a live backend -- every other twin's backing
 * store (Cassandra, Kafka, vtgate, ...) needs a container, so its ITs are gated off by default.
 *
 * <p>Table names are prefixed {@code h2_roundtrip_} because the classpath test config points every
 * test in this module at the same shared in-memory database ({@code r2dbc:h2:mem:///t1}).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveR2dbcAccessorH2Test {

    private static final String TABLE = "h2_roundtrip_t";

    private static DefaultR2dbcClientService defaultR2dbcClientService;

    @BeforeAll
    static void setUp() {
        defaultR2dbcClientService = R2dbcClientServiceTestHelper.defaultR2dbcClientService();
    }

    @AfterAll
    static void tearDown() {
        if (defaultR2dbcClientService != null) {
            defaultR2dbcClientService.shutdown();
        }
    }

    private static ReactiveR2dbcAccessor accessor() {
        return defaultR2dbcClientService.getReactiveAccessor();
    }

    @Test
    void fullReactiveRoundTripAgainstLiveH2ViaBootstrappedService() {
        final ReactiveR2dbcAccessor accessor = accessor();

        StepVerifier.create(accessor.update(
                        "create", "CREATE TABLE " + TABLE + "(id INT PRIMARY KEY, name VARCHAR(20))"))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(accessor.update("insert", "INSERT INTO " + TABLE + " VALUES (?, ?)", 1, "autocommit"))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(accessor.query("select", "SELECT name FROM " + TABLE + " WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 1))
                .expectNext("autocommit")
                .verifyComplete();

        // Committed work is visible to a subsequent autocommit read on a DIFFERENT pooled connection.
        StepVerifier.create(accessor.inTransaction("tx-commit",
                        session -> Mono.from(session.update("INSERT INTO " + TABLE + " VALUES (?, ?)", 2, "committed"))))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(accessor.query("select-committed", "SELECT name FROM " + TABLE + " WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 2))
                .expectNext("committed")
                .verifyComplete();

        // A failure inside the transaction rolls the insert back and re-raises the ORIGINAL error,
        // wrapped by the shared reactive spine's default mapping (the caller's IllegalStateException
        // is not an R2dbcException, so it is not mapped to R2dbcAccessorException).
        StepVerifier.create(accessor.inTransaction("tx-rollback",
                        session -> Mono.from(session.update("INSERT INTO " + TABLE + " VALUES (?, ?)", 3, "rolledback"))
                                .then(Mono.error(new IllegalStateException("boom")))))
                .expectErrorSatisfies(error -> {
                    Assertions.assertInstanceOf(ReactiveOperationException.class, error);
                    Assertions.assertInstanceOf(IllegalStateException.class, error.getCause());
                    Assertions.assertEquals("boom", error.getCause().getMessage());
                })
                .verify();

        StepVerifier.create(accessor.query("select-rolledback", "SELECT name FROM " + TABLE + " WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 3))
                .verifyComplete();

        StepVerifier.create(accessor.update("drop", "DROP TABLE " + TABLE))
                .expectNextCount(1)
                .verifyComplete();
    }
}
