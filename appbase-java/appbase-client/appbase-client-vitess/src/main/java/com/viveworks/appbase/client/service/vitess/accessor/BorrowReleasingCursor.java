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

package com.viveworks.appbase.client.service.vitess.accessor;

import com.viveworks.appbase.client.service.vitess.VitessGrpcClientIntegrationService;
import com.viveworks.appbase.client.service.vitess.accessor.reactive.ReactiveVitessGrpcAccessor;
import io.vitess.client.cursor.Cursor;
import io.vitess.client.cursor.Row;
import io.vitess.proto.Query;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Delegating {@link Cursor} returned by {@link VitessGrpcAccessor#streamExecute} that defers
 * releasing its borrowed {@link VitessGrpcClientIntegrationService} back to the pool until the
 * cursor itself is closed, instead of the moment the driver call returns.
 *
 * <p>{@code VTGateBlockingConnection.streamExecute(...)} returns a lazy cursor that pulls rows off
 * a still-open gRPC stream on the borrowed connection as the caller calls {@link #next()}. If the
 * borrowed {@link VitessGrpcClientIntegrationService} were released as soon as the cursor was
 * obtained (as the point-query ops correctly do, since those fully materialize before returning),
 * another borrower could reuse the pooled connection while this stream is still open. Wrapping the
 * driver's cursor in this class ties the pool release to {@link #close()} instead, mirroring the
 * borrow-held-until-terminal contract {@link ReactiveVitessGrpcAccessor}
 * enforces via {@code Flux.usingWhen}. Callers MUST close the returned cursor (e.g.
 * try-with-resources) to return the connection to the pool.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class BorrowReleasingCursor extends Cursor {

    private final Cursor delegate;
    private final VitessGrpcClientIntegrationService integrationService;
    private final AtomicBoolean released = new AtomicBoolean(false);

    /**
     * Wrap {@code delegate}, deferring release of {@code integrationService} to {@link
     * #close()}.
     *
     * @param delegate Cursor returned by the driver's {@code streamExecute} call
     * @param integrationService Borrowed integration service to release exactly once, on close
     */
    BorrowReleasingCursor(final Cursor delegate, final VitessGrpcClientIntegrationService integrationService) {
        this.delegate = delegate;
        this.integrationService = integrationService;
    }

    /**
     * Delegate to the wrapped cursor.
     *
     * @return the next row, or {@code null} if the stream is exhausted
     * @throws SQLException the underlying driver failure, if the call fails
     */
    @Override
    public Row next() throws SQLException {
        return delegate.next();
    }

    /**
     * Delegate to the wrapped cursor.
     *
     * @return rows affected
     * @throws SQLException the underlying driver failure, if the call fails
     */
    @Override
    public long getRowsAffected() throws SQLException {
        return delegate.getRowsAffected();
    }

    /**
     * Delegate to the wrapped cursor.
     *
     * @return the last insert id
     * @throws SQLException the underlying driver failure, if the call fails
     */
    @Override
    public long getInsertId() throws SQLException {
        return delegate.getInsertId();
    }

    /**
     * Delegate to the wrapped cursor.
     *
     * @return the result fields
     * @throws SQLException the underlying driver failure, if the call fails
     */
    @Override
    public List<Query.Field> getFields() throws SQLException {
        return delegate.getFields();
    }

    /**
     * Close the wrapped cursor, then release the borrowed {@link
     * VitessGrpcClientIntegrationService} back to the pool -- guarded so the release happens
     * exactly once even if {@code close()} is called more than once.
     *
     * @throws Exception the underlying cursor's close failure, if any; the integration service is
     *         still released via {@code finally}
     */
    @Override
    public void close() throws Exception {
        try {
            delegate.close();
        } finally {
            if (released.compareAndSet(false, true)) {
                integrationService.finalizeOperation();
            }
        }
    }
}
