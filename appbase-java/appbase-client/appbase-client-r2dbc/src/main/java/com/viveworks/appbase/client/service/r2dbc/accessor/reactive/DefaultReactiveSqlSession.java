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

import com.viveworks.appbase.core.util.AppbaseUtil;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * Default {@link ReactiveSqlSession}, wrapping the ONE {@link Connection} pinned by the enclosing
 * {@link ReactiveR2dbcAccessor#inTransaction(String, java.util.function.Function)} call. Statement
 * building/binding mirrors the accessor's own autocommit {@code query}/{@code update} exactly
 * (same {@link ReactiveR2dbcAccessor#bindAll(Statement, Object[])} helper), the only difference
 * being that the connection is supplied by the caller rather than borrowed fresh per call.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class DefaultReactiveSqlSession implements ReactiveSqlSession {

    private final Connection connection;

    /**
     * Create a session pinned to {@code connection}.
     *
     * @param connection Connection the enclosing transaction began on
     */
    DefaultReactiveSqlSession(final Connection connection) {
        this.connection = AppbaseUtil.notNull(connection, "connection");
    }

    /** {@inheritDoc} */
    @Override
    public <T> Flux<T> query(final String sql, final BiFunction<Row, RowMetadata, T> mapper, final Object... binds) {
        AppbaseUtil.notBlank(sql, "sql");
        AppbaseUtil.notNull(mapper, "mapper");
        final Statement statement = connection.createStatement(sql);
        ReactiveR2dbcAccessor.bindAll(statement, binds);
        return Flux.from(statement.execute()).flatMap(result -> result.map(mapper));
    }

    /** {@inheritDoc} */
    @Override
    public Mono<Long> update(final String sql, final Object... binds) {
        AppbaseUtil.notBlank(sql, "sql");
        final Statement statement = connection.createStatement(sql);
        ReactiveR2dbcAccessor.bindAll(statement, binds);
        return Flux.from(statement.execute())
                .flatMap(Result::getRowsUpdated)
                .reduce(0L, Long::sum);
    }
}
