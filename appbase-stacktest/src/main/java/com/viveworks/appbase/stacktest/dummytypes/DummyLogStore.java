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
package com.viveworks.appbase.stacktest.dummytypes;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreException;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyLogStore implements LogStore {
    @Override
    public void shutdown() throws AppbaseRuntimeException {
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public Collection<LogRecord> readLogRecordsBetween(
            final long inclusiveStartTimestamp,
            final long exclusiveEndTimestamp,
            final int pageSize) throws LogStoreException
    {
        return null;
    }

    @Override
    public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsBetween(
            final long inclusiveStartTimestamp,
            final long exclusiveEndTimestamp,
            final int pageSize) {
        return null;
    }

    @Override
    public Collection<LogRecord> readLogRecordsAt(final long timestamp, final int pageSize) {
        return null;
    }

    @Override
    public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsAt(
            final long timestamp,
            final int pageSize) {
        return null;
    }

    @Override
    public Collection<LogRecord> readLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria,
            final int pageSize)
    {
        return null;
    }

    @Override
    public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria,
            final int pageSize) {
        return null;
    }

    @Override
    public void writeLogRecord(final LogRecord logRecord) {
        // no-op
    }

    @Override
    public CompletableFuture<Void> asyncWriteLogRecord(final LogRecord logRecord) {
        return null;
    }

    @Override
    public void removeLogRecordsBetween(
            final long inclusiveStartTimestamp,
            final long exclusiveEndTimestamp)
    {
        // no-op
    }

    @Override
    public CompletableFuture<Void> asyncRemoveLogRecordsBetween(
            final long inclusiveStartTimestamp,
            final long exclusiveEndTimestamp) {
        return null;
    }

    @Override
    public void removeLogRecordsAt(final long timestamp) throws LogStoreException {
        // no-op
    }

    @Override
    public CompletableFuture<Void> asyncRemoveLogRecordsAt(final long timestamp) {
        return null;
    }

    @Override
    public void removeLogRecordsByCriteria(final LogRecordCriteria logRecordCriteria) {
        // no-op
    }

    @Override
    public CompletableFuture<Void> asyncRemoveLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria) {
        return null;
    }

    @Override
    public void synchronize() throws LogStoreException {
    }
}
