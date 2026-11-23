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

package com.viveworks.appbase.core.service.log.store;

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a store for {@link LogRecord}s.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface LogStore extends Shutdownable {
    /**
     * Read {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return Result collection
     */
    Collection<LogRecord> readLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize) throws LogStoreException;

    /**
     * Asynchronously read {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return Result collection completable future
     */
    CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize);


    /**
     * Read {@link LogRecord}s at a specific timestamp.
     *
     * @param timestamp Specific log timestamp
     * @param pageSize Page size of the result
     * @return Result collection
     */
    Collection<LogRecord> readLogRecordsAt(long timestamp, int pageSize) throws LogStoreException;


    /**
     * Asynchronously read {@link LogRecord}s at a specific timestamp.
     *
     * @param timestamp Specific log timestamp
     * @param pageSize Page size of the result
     * @return Result collection completable future
     */
    CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsAt(long timestamp, int pageSize);


    /**
     * Read {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @param pageSize Page size of the result
     * @return Result collection
     */
    Collection<LogRecord> readLogRecordsByCriteria(
            LogRecordCriteria logRecordCriteria, int pageSize) throws LogStoreException;


    /**
     * Asynchronously read {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @param pageSize Page size of the result
     * @return Result collection completable future
     */
    CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsByCriteria(
            LogRecordCriteria logRecordCriteria, int pageSize);


    /**
     * Write {@link LogRecord} to the store.
     *
     * @param logRecord Log record
     */
    void writeLogRecord(LogRecord logRecord) throws LogStoreException;


    /**
     * Write {@link LogRecord} to the store.
     *
     * @param logRecord Log record
     * @return Result completable future
     */
    CompletableFuture<Void> asyncWriteLogRecord(LogRecord logRecord);


    /**
     * Remove {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     */
    void removeLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp) throws LogStoreException;


    /**
     * Asynchronously remove {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @return Result completable future
     */
    CompletableFuture<Void> asyncRemoveLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp);


    /**
     * Remove {@link LogRecord}s at a given timestamp.
     *
     * @param timestamp Log timestamp
     */
    void removeLogRecordsAt(long timestamp) throws LogStoreException;


    /**
     * Asynchronously remove {@link LogRecord}s at a given timestamp.
     *
     * @param timestamp Log timestamp
     * @return Result completable future
     */
    CompletableFuture<Void> asyncRemoveLogRecordsAt(long timestamp);


    /**
     * Remove {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     */
    void removeLogRecordsByCriteria(LogRecordCriteria logRecordCriteria) throws LogStoreException;


    /**
     * Asynchronously remove {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @return Result collection completable future
     */
    CompletableFuture<Void> asyncRemoveLogRecordsByCriteria(LogRecordCriteria logRecordCriteria);


    /**
     * Used to synchronize the store.
     *
     * @throws LogStoreException if there were any errors
     */
    void synchronize() throws LogStoreException;
}
