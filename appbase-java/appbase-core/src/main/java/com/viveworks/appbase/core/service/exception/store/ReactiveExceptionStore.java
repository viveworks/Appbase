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

package com.viveworks.appbase.core.service.exception.store;

import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * The reactive read face of an {@link ExceptionStore} — mirrors the five blocking {@link ExceptionStore}
 * reads, offloading each onto the configured
 * {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary} instead of the calling
 * thread. Unlike the Log store, exception reads carry no {@code pageSize} and their return types are
 * heterogeneous (a by-id read yields a single {@link ExceptionMessageDetails}).
 * Lifecycle rejections after the underlying Lucene service has shut down are reported as an
 * {@link ExceptionStoreException}, whose cause is the shutdown-state {@link IllegalStateException}, matching the
 * blocking {@link ExceptionStore} contract.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ReactiveExceptionStore {
    /**
     * Reactively read the {@link ExceptionMessageDetails} for an id (completes empty if absent).
     *
     * @param id Exception message id
     * @return a {@link Mono} that emits the details, completes empty if absent, or errors
     */
    Mono<ExceptionMessageDetails> readExceptionMessageDetailsByIdReactive(int id);

    /**
     * Reactively read all {@link ExceptionMessageDetails}.
     *
     * @return a {@link Mono} that emits the result collection, or errors
     */
    Mono<Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsReactive();

    /**
     * Reactively read all {@link ExceptionMessageDetails} between min timestamp (inclusive) and max
     * timestamp (exclusive).
     *
     * @param inclusiveMinTimestamp Min timestamp (inclusive)
     * @param exclusiveMaxTimestamp Max timestamp (exclusive)
     * @return a {@link Mono} that emits the result collection, or errors
     */
    Mono<Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsByTimestampsReactive(
            long inclusiveMinTimestamp, long exclusiveMaxTimestamp);

    /**
     * Reactively read the unique throwable type names.
     *
     * @return a {@link Mono} that emits the result collection, or errors
     */
    Mono<Collection<String>> readUniqueThrowableTypeNamesReactive();

    /**
     * Reactively read the {@link ExceptionMessageDetails} for a throwable type name.
     *
     * @param throwableTypeName Throwable type name
     * @return a {@link Mono} that emits the result collection, or errors
     */
    Mono<Collection<ExceptionMessageDetails>> readExceptionMessageDetailsByThrowableTypeNameReactive(
            String throwableTypeName);
}
