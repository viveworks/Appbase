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

package com.viveworks.appbase.client.service.jasync.reactive;

import com.github.jasync.sql.db.QueryResult;
import com.viveworks.appbase.client.service.jasync.DefaultJasyncClientService;
import com.viveworks.appbase.client.service.jasync.JasyncAccessor;
import com.viveworks.appbase.client.service.jasync.JasyncClientServiceTestHelper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.util.List;

/**
 * Integration tests for {@link ReactiveJasyncAccessor} and its synchronous twin {@link
 * JasyncAccessor} against a live MySQL/MariaDB (or Postgres) instance.
 *
 * <p>Requires a running MySQL/MariaDB or Postgres instance, reachable per the module's gated-test
 * configuration ({@code src/test/resources/appbase-client-jasync.yaml}). Enable via system
 * property: {@code -DJasyncClientServiceTests.enabled=true}
 *
 * <p>Use the Docker scripts in {@code infra/docker/} to start a local MySQL/MariaDB instance.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "JasyncClientServiceTests.enabled", matches = "true")
class ReactiveJasyncAccessorIT {

    private static final String REACTIVE_TABLE = "appbase_reactive_jasync_it";
    private static final String SYNC_TABLE = "appbase_sync_jasync_it";
    private static final String TEST_KEY = "appbase-jasync-it-key";
    private static final String TEST_VALUE = "appbase-jasync-it-value";

    private static DefaultJasyncClientService jasyncClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultJasyncClientService.registerObjectMapperSubtypes();
        jasyncClientService = JasyncClientServiceTestHelper.defaultJasyncClientService();
    }

    @AfterAll
    static void tearDown() {
        if (jasyncClientService != null) {
            jasyncClientService.shutdown();
        }
    }

    /**
     * Verifies a reactive schema setup, parameterized {@code INSERT}, and {@code SELECT}
     * round-trips the stored value through {@link ReactiveJasyncAccessor#sendQuery(String,
     * String)} and {@link ReactiveJasyncAccessor#sendPreparedStatement(String, String, List)}, and
     * that {@link ReactiveJasyncAccessor#queryRows(String, String, List)} emits the same row.
     * Drops the throwaway table in a {@code finally} block regardless of outcome.
     */
    @Test
    void reactiveInsertThenSelectRoundTrip() {
        final ReactiveJasyncAccessor accessor = jasyncClientService.getReactiveAccessor();
        try {
            StepVerifier.create(accessor.sendQuery("createTable",
                            "CREATE TABLE IF NOT EXISTS " + REACTIVE_TABLE
                                    + " (k VARCHAR(64) PRIMARY KEY, v VARCHAR(255))"))
                    .expectNextCount(1)
                    .verifyComplete();

            StepVerifier.create(accessor.sendPreparedStatement("insert",
                            "INSERT INTO " + REACTIVE_TABLE + " (k, v) VALUES (?, ?)",
                            List.of(TEST_KEY, TEST_VALUE)))
                    .assertNext(queryResult -> Assertions.assertEquals(1L, queryResult.getRowsAffected()))
                    .verifyComplete();

            StepVerifier.create(accessor.sendPreparedStatement("select",
                            "SELECT v FROM " + REACTIVE_TABLE + " WHERE k = ?", List.of(TEST_KEY)))
                    .assertNext(queryResult ->
                            Assertions.assertEquals(TEST_VALUE, queryResult.getRows().get(0).getString("v")))
                    .verifyComplete();

            StepVerifier.create(accessor.queryRows("selectRows",
                            "SELECT v FROM " + REACTIVE_TABLE + " WHERE k = ?", List.of(TEST_KEY)))
                    .assertNext(row -> Assertions.assertEquals(TEST_VALUE, row.getString("v")))
                    .verifyComplete();
        } finally {
            StepVerifier.create(accessor.sendQuery("dropTable", "DROP TABLE IF EXISTS " + REACTIVE_TABLE))
                    .expectNextCount(1)
                    .verifyComplete();
        }
    }

    /**
     * Verifies the synchronous twin's parameterized {@code INSERT}/{@code SELECT} round-trip
     * through {@link JasyncAccessor#sendPreparedStatement(String, String, List)}, asserting the
     * returned {@link QueryResult}. Drops the throwaway table in a {@code finally} block
     * regardless of outcome.
     *
     * @throws Exception the mapped exception, if any driver operation fails
     */
    @Test
    void syncInsertThenSelectRoundTrip() throws Exception {
        final JasyncAccessor accessor = jasyncClientService.getAccessor();
        try {
            accessor.sendQuery("createTable",
                    "CREATE TABLE IF NOT EXISTS " + SYNC_TABLE + " (k VARCHAR(64) PRIMARY KEY, v VARCHAR(255))");

            final QueryResult insertResult = accessor.sendPreparedStatement("insert",
                    "INSERT INTO " + SYNC_TABLE + " (k, v) VALUES (?, ?)", List.of(TEST_KEY, TEST_VALUE));
            Assertions.assertEquals(1L, insertResult.getRowsAffected());

            final QueryResult selectResult = accessor.sendPreparedStatement("select",
                    "SELECT v FROM " + SYNC_TABLE + " WHERE k = ?", List.of(TEST_KEY));
            Assertions.assertEquals(TEST_VALUE, selectResult.getRows().get(0).getString("v"));
        } finally {
            accessor.sendQuery("dropTable", "DROP TABLE IF EXISTS " + SYNC_TABLE);
        }
    }
}
