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

package com.viveworks.appbase.core.service.exception.accessor;

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.util.Unmodifiable;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an exception accessor that can read / write exceptions from / to {@link ExceptionStore}s.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ExceptionAccessor extends Shutdownable {
    /**
     * Read {@link ExceptionMessageDetails} by ID from all registered {@link ExceptionStore}s.
     *
     * @param id ID
     * @return {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, ExceptionMessageDetails> readExceptionMessageDetailsById(Integer id) throws ExceptionAccessorException;

    /**
     * Asynchronously read {@link ExceptionMessageDetails} by ID from all registered {@link ExceptionStore}s.
     *
     * @param id ID
     * @return {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, CompletableFuture<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsById(Integer id);


    /**
     * Reactively read {@link ExceptionMessageDetails} by ID from all registered {@link ExceptionStore}s.
     * A store whose read yields {@code null} (id absent) contributes no entry — a {@link Mono} cannot carry
     * {@code null}, which matches the blocking {@link #readExceptionMessageDetailsById(Integer)}'s
     * skip-if-null behaviour.
     *
     * @param id ID
     * @return a {@link Mono} of {@link ExceptionMessageDetails} by {@link ExceptionStore} type name
     */
    Mono<Map<String, ExceptionMessageDetails>> readExceptionMessageDetailsByIdReactive(Integer id);


    /**
     * Read all {@link ExceptionMessageDetails} from all registered {@link ExceptionStore}s.
     *
     * @return All {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, Collection<ExceptionMessageDetails>> readAllExceptionMessageDetails() throws ExceptionAccessorException;


    /**
     * Asynchronously read all {@link ExceptionMessageDetails} from all registered {@link ExceptionStore}s.
     *
     * @return All {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>> asyncReadAllExceptionMessageDetails();


    /**
     * Reactively read all {@link ExceptionMessageDetails} from all registered {@link ExceptionStore}s.
     *
     * @return a {@link Mono} of all {@link ExceptionMessageDetails} by {@link ExceptionStore} type name
     */
    Mono<Map<String, Collection<ExceptionMessageDetails>>> readAllExceptionMessageDetailsReactive();


    /**
     * Read all {@link ExceptionMessageDetails} by timestamp from all registered {@link ExceptionStore}s.
     *
     * @param inclusiveMinTimestamp Minimum timestamp (inclusive)
     * @param exclusiveMaxTimestamp Maximum timestamp (exclusive)
     * @return All {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsByTimestamps(
            long inclusiveMinTimestamp, long exclusiveMaxTimestamp) throws ExceptionAccessorException;


    /**
     * Asynchronously read all {@link ExceptionMessageDetails} by timestamp from all registered {@link ExceptionStore}s.
     *
     * @param inclusiveMinTimestamp Minimum timestamp (inclusive)
     * @param exclusiveMaxTimestamp Maximum timestamp (exclusive)
     * @return All {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
        asyncReadAllExceptionMessageDetailsByTimestamps(long inclusiveMinTimestamp, long exclusiveMaxTimestamp);


    /**
     * Reactively read all {@link ExceptionMessageDetails} between min timestamp (inclusive) and max
     * timestamp (exclusive) from all registered {@link ExceptionStore}s.
     *
     * @param inclusiveMinTimestamp Minimum timestamp (inclusive)
     * @param exclusiveMaxTimestamp Maximum timestamp (exclusive)
     * @return a {@link Mono} of all {@link ExceptionMessageDetails} by {@link ExceptionStore} type name
     */
    Mono<Map<String, Collection<ExceptionMessageDetails>>> readAllExceptionMessageDetailsByTimestampsReactive(
            long inclusiveMinTimestamp, long exclusiveMaxTimestamp);


    /**
     * Asynchronously read unique {@link Throwable} type names.
     *
     * @return Unique exception type names by {@link ExceptionStore} type name
     */
    Map<String, Collection<String>> readUniqueExceptionTypeNames() throws ExceptionAccessorException;


    /**
     * Asynchronously read unique {@link Throwable} type names.
     *
     * @return Unique exception type names by {@link ExceptionStore} type name
     */
    Map<String, CompletableFuture<Collection<String>>> asyncReadUniqueExceptionTypeNames();


    /**
     * Reactively read unique {@link Throwable} type names from all registered {@link ExceptionStore}s.
     *
     * @return a {@link Mono} of unique exception type names by {@link ExceptionStore} type name
     */
    Mono<Map<String, Collection<String>>> readUniqueExceptionTypeNamesReactive();


    /**
     * Read {@link ExceptionMessageDetails} by {@link Throwable} type name.
     *
     * @param throwableTypeName Throwable type name
     * @return {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, Collection<ExceptionMessageDetails>>
        readExceptionMessageDetailsByThrowableTypeName(String throwableTypeName) throws ExceptionAccessorException;


    /**
     * Asynchronously read {@link ExceptionMessageDetails} by {@link Throwable} type name.
     *
     * @param throwableTypeName Throwable type name
     * @return {@link ExceptionMessageDetails} by {@link ExceptionStore} type name completable future
     */
    Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
        asyncReadExceptionMessageDetailsByThrowableTypeName(String throwableTypeName);


    /**
     * Reactively read {@link ExceptionMessageDetails} by {@link Throwable} type name from all registered
     * {@link ExceptionStore}s.
     *
     * @param throwableTypeName Throwable type name
     * @return a {@link Mono} of {@link ExceptionMessageDetails} by {@link ExceptionStore} type name
     */
    Mono<Map<String, Collection<ExceptionMessageDetails>>> readExceptionMessageDetailsByThrowableTypeNameReactive(
            String throwableTypeName);


    /**
     * Write {@link ExceptionMessage} from all registered {@link ExceptionStore}s.
     *
     * @param exceptionMessage Exception message
     * @return Result by {@link ExceptionStore} type name completable future
     */
    Collection<String> writeExceptionMessage(ExceptionMessage exceptionMessage) throws ExceptionAccessorException;


    /**
     * Asynchronously write {@link ExceptionMessage} from all registered {@link ExceptionStore}s.
     *
     * @param exceptionMessage Exception message
     * @return Result by {@link ExceptionStore} type name completable future
     */
    Map<String, CompletableFuture<Void>> asyncWriteExceptionMessage(ExceptionMessage exceptionMessage);


    /**
     * Remove {@link ExceptionMessageDetails} by ID.
     *
     * @param id ID
     * @return Result by {@link ExceptionStore} type name completable future
     */
    Collection<String> removeExceptionMessageDetailsById(int id) throws ExceptionAccessorException;


    /**
     * Asynchronously remove {@link ExceptionMessageDetails} by ID.
     *
     * @param id ID
     * @return Result by {@link ExceptionStore} type name completable future
     */
    Map<String, CompletableFuture<Void>> asyncRemoveExceptionMessageDetailsById(int id);


    /**
     * Remove {@link ExceptionMessageDetails} before certain timestamp (exclusive).
     *
     * @param timestamp Exclusive before timestamp
     * @return Result by {@link ExceptionStore} type name completable future
     */
    Collection<String> removeExceptionMessageDetailsExclusiveBefore(long timestamp) throws ExceptionAccessorException;


    /**
     * Asynchronously remove {@link ExceptionMessageDetails} before certain timestamp (exclusive).
     *
     * @param timestamp Exclusive before timestamp
     * @return Result by {@link ExceptionStore} type name completable future
     */
    Map<String, CompletableFuture<Void>> asyncRemoveExceptionMessageDetailsExclusiveBefore(long timestamp);


    /**
     * Get {@link ExceptionStoreFactory} by {@link ExceptionStoreConfiguration} type name map.
     *
     * @return Exception store factory by configuration type name map
     */
    @Unmodifiable
    Map<String, ExceptionStoreFactory> getExceptionStoreFactoryByConfigurationTypeNameMap();


    /**
     * Register {@link ExceptionStoreFactory} by {@link ExceptionStoreConfiguration} type.
     *
     * @param exceptionStoreConfigurationType Exception store configuration type
     * @param exceptionStoreFactory Exception store factory
     * @throws ExceptionStoreFactoryException if there are any errors
     */
    void registerExceptionStoreFactoryByConfigurationType(
            Class<? extends ExceptionStoreConfiguration> exceptionStoreConfigurationType,
            ExceptionStoreFactory exceptionStoreFactory)
            throws ExceptionStoreFactoryException;


    /**
     * Unregister {@link ExceptionStoreFactory} by {@link ExceptionStoreConfiguration} type.
     *
     * @param exceptionStoreConfigurationType Exception store configuration type
     * @param exceptionStoreFactory Exception store factory
     */
    void unregisterExceptionStoreFactoryByConfigurationType(
            Class<? extends ExceptionStoreConfiguration> exceptionStoreConfigurationType,
            Class<? extends ExceptionStoreFactory> exceptionStoreFactory);


    /**
     * Clear {@link ExceptionStoreFactory} by {@link ExceptionStoreConfiguration} type name map.
     */
    void clearExceptionStoreFactoryByConfigurationTypeMap();


    /**
     * Get {@link ExceptionStore}s.
     *
     * @return Exception stores
     */
    @Unmodifiable
    Collection<ExceptionStore> getExceptionStores();

    /**
     * Register an exception store.
     *
     * @param exceptionStore Exception store
     */
    void registerExceptionStore(ExceptionStore exceptionStore);


    /**
     * Unregister {@link ExceptionStore}.
     *
     * @param exceptionStoreType Exception store type
     */
    void unregisterExceptionStore(Class<? extends ExceptionStore> exceptionStoreType);


    /**
     * Clear {@link ExceptionStore}s.
     */
    void clearExceptionStores();


    /**
     * Reload the accessor and stores.
     *
     * @param exceptionAccessorConfiguration Configuration to use to reload
     * @param <C> Configuration type
     * @throws ExceptionAccessorException if there were errors during reload
     */
    <C extends ExceptionAccessorConfiguration> void reload(
            C exceptionAccessorConfiguration) throws ExceptionAccessorException;


    /**
     * Used to synchronize {@link ExceptionStore}s.
     *
     * @throws ExceptionAccessorException if there were any errors
     */
    void synchronizeExceptionStores() throws ExceptionAccessorException;
}
