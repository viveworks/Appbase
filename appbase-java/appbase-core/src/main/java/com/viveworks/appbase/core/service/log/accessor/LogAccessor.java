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

package com.viveworks.appbase.core.service.log.accessor;

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.LogStoreFactoryException;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.util.Unmodifiable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Accessor for {@link LogRecord}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface LogAccessor extends Shutdownable {
    /**
     * Streams records between timestamps with the originating {@link LogStore} type name.
     * Cancellation propagates to the selected store; stores capable of observing it while
     * fetching records do so.
     */
    Flux<Map.Entry<String, LogRecord>> streamLogRecordsBetweenReactive(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize);

    /**
     * Read {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return Results by {@link LogStore} type name
     */
    Map<String, Collection<LogRecord>> readLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize)
            throws LogAccessorException;

    /**
     * Asynchronously read {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return Results by {@link LogStore} type name
     */
    Map<String, CompletableFuture<Collection<LogRecord>>> asyncReadLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize);


    /**
     * Reactively read {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive),
     * across every registered {@link LogStore}.
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return a {@link Mono} that emits the results by {@link LogStore} type name, or errors
     */
    Mono<Map<String, Collection<LogRecord>>> readLogRecordsBetweenReactive(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize);


    /**
     * Read {@link LogRecord} at a specific timestamp.
     *
     * @param timestamp Specific log timestamp
     * @param pageSize Page size of the result
     * @return Results by {@link LogStore} type name
     */
    Map<String, Collection<LogRecord>> readLogRecordsAt(long timestamp, int pageSize) throws LogAccessorException;


    /**
     * Asynchronously read {@link LogRecord} at a specific timestamp.
     *
     * @param timestamp Specific log timestamp
     * @param pageSize Page size of the result
     * @return Results by {@link LogStore} type name
     */
    Map<String, CompletableFuture<Collection<LogRecord>>> asyncReadLogRecordsAt(long timestamp, int pageSize);


    /**
     * Reactively read {@link LogRecord} at a specific timestamp, across every registered {@link LogStore}.
     *
     * @param timestamp Specific log timestamp
     * @param pageSize Page size of the result
     * @return a {@link Mono} that emits the results by {@link LogStore} type name, or errors
     */
    Mono<Map<String, Collection<LogRecord>>> readLogRecordsAtReactive(long timestamp, int pageSize);


    /**
     * Read by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @param pageSize Page size of the result
     * @return Results by {@link LogStore} type name
     */
    Map<String, Collection<LogRecord>> readLogRecordsByCriteria(
            LogRecordCriteria logRecordCriteria, int pageSize) throws LogAccessorException;


    /**
     * Asynchronously read by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @param pageSize Page size of the result
     * @return Results by {@link LogStore} type name
     */
    Map<String, CompletableFuture<Collection<LogRecord>>> asyncReadLogRecordsByCriteria(
            LogRecordCriteria logRecordCriteria, int pageSize);


    /**
     * Reactively read by matching {@link LogRecordCriteria}, across every registered {@link LogStore}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @param pageSize Page size of the result
     * @return a {@link Mono} that emits the results by {@link LogStore} type name, or errors
     */
    Mono<Map<String, Collection<LogRecord>>> readLogRecordsByCriteriaReactive(
            LogRecordCriteria logRecordCriteria, int pageSize);


    /**
     * Write {@link LogRecord} to the stores.
     *
     * @param logRecord Log record to write
     * @return {@link LogStore} type names
     */
    Collection<String> writeLogRecord(LogRecord logRecord) throws LogAccessorException;


    /**
     * Asynchronously write {@link LogRecord} to the stores.
     *
     * @param logRecord Log record to write
     * @return Result by {@link LogStore} type name
     */
    Map<String, CompletableFuture<Void>> asyncWriteLogRecord(LogRecord logRecord);


    /**
     * Remove {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @return Results by {@link LogStore} type name
     */
    Collection<String> removeLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp) throws LogAccessorException;


    /**
     * Asynchronously remove {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @return Results by {@link LogStore} type name
     */
    Map<String, CompletableFuture<Void>> asyncRemoveLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp);

    /**
     * Remove {@link LogRecord}s at a given timestamp.
     *
     * @param timestamp Log timestamp
     * @return Results by {@link LogStore} type name
     */
    Collection<String> removeLogRecordsAt(long timestamp) throws LogAccessorException;

    /**
     * Asynchronously remove {@link LogRecord}s at a given timestamp.
     *
     * @param timestamp Log timestamp
     * @return Results by {@link LogStore} type name
     */
    Map<String, CompletableFuture<Void>> asyncRemoveLogRecordsAt(long timestamp);


    /**
     * Remove {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @return Results by {@link LogStore} type name
     */
    Collection<String> removeLogRecordsByCriteria(LogRecordCriteria logRecordCriteria) throws LogAccessorException;


    /**
     * Asynchronously remove {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @return Results by {@link LogStore} type name
     */
    Map<String, CompletableFuture<Void>> asyncRemoveLogRecordsByCriteria(
            LogRecordCriteria logRecordCriteria);


    /**
     * Get {@link LogStoreFactory} by {@link LogStoreConfiguration} type name map.
     *
     * @return Log store factory by configuration type name map
     */
    @Unmodifiable
    Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> getLogStoreFactoryByConfigurationTypeMap();


    /**
     * Register {@link LogStoreFactory} by {@link LogStoreConfiguration} type.
     *
     * @param logStoreConfigurationType Log store configuration type
     * @param logStoreFactory Log store factory
     * @throws LogStoreFactoryException if there were any errors during registration
     */
    void registerLogStoreFactoryByConfigurationType(
            Class<? extends LogStoreConfiguration> logStoreConfigurationType,
            LogStoreFactory logStoreFactory)
            throws LogStoreFactoryException;


    /**
     * Unregister {@link LogStoreFactory} by {@link LogStoreConfiguration}.
     *
     * @param logStoreConfigurationType Log store configuration type
     * @param logStoreFactoryType Log store factory type
     */
    void unregisterLogStoreFactoryByConfigurationType(
            Class<? extends LogStoreConfiguration> logStoreConfigurationType,
            Class<? extends LogStoreFactory> logStoreFactoryType);


    /**
     * Clear {@link LogStoreFactory} by {@link LogStoreConfiguration} type name map.
     */
    void clearLogStoreFactoryByConfigurationTypeMap();


    /**
     * Get {@link LogStore}s.
     *
     * @return Log stores
     */
    @Unmodifiable
    Collection<LogStore> getLogStores();


    /**
     * Register {@link LogStore}.
     *
     * @param logStore Log store
     */
    void registerLogStore(LogStore logStore);


    /**
     * Unregister {@link LogStore}.
     *
     * @param logStoreType Log store type
     */
    void unregisterLogStore(Class<? extends LogStore> logStoreType);


    /**
     * Clear {@link LogStore}s.
     */
    void clearLogStores();


    /**
     * Reload the accessor and stores.
     *
     * @param logAccessorConfiguration Configuration to use to reload
     * @param <C> Configuration type
     * @throws LogAccessorException if there were errors during reload
     */
    <C extends LogAccessorConfiguration> void reload(C logAccessorConfiguration) throws LogAccessorException;


    /**
     * Used to synchronize {@link LogStore}s.
     *
     * @throws LogAccessorException if there are any errors
     */
    void synchronizeLogStores() throws LogAccessorException;
}
