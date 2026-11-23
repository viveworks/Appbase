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

package com.viveworks.appbase.client.service.r2dbc.accessor.reactive;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * Statement-issuing face handed to the {@code work} function of {@link
 * ReactiveR2dbcAccessor#inTransaction(String, java.util.function.Function)}. Every {@link
 * #query(String, BiFunction, Object...)}/{@link #update(String, Object...)} call runs on the
 * SAME pinned {@link Connection} the enclosing transaction began on -- there is no borrow/release
 * here, unlike the accessor's autocommit {@code query}/{@code update}, since the connection's
 * lifecycle (and the transaction's begin/commit/rollback) is owned entirely by the enclosing
 * {@code inTransaction} call.
 *
 * <p>There is deliberately no {@code op} parameter here: the enclosing {@code inTransaction} call
 * already owns the metrics/tracing timer for the whole transaction, so a per-statement timer
 * inside it would double-count.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ReactiveSqlSession {

    /**
     * Execute a query on the pinned transaction connection and map each result row via {@code
     * mapper}. {@code mapper} runs INSIDE {@link Statement#execute()}'s {@code Result#map(
     * BiFunction)}, the only place a {@link Row}'s column accessors are valid.
     *
     * @param sql SQL statement to execute
     * @param mapper Row mapper, invoked once per result row
     * @param binds Positional, non-null bind values for {@code sql}
     * @param <T> Mapped row type
     * @return Flux of mapped rows
     */
    <T> Flux<T> query(String sql, BiFunction<Row, RowMetadata, T> mapper, Object... binds);

    /**
     * Execute an update/DDL/DML statement on the pinned transaction connection and return the
     * total affected row count.
     *
     * @param sql SQL statement to execute
     * @param binds Positional, non-null bind values for {@code sql}
     * @return Mono of the total affected row count
     */
    Mono<Long> update(String sql, Object... binds);
}
