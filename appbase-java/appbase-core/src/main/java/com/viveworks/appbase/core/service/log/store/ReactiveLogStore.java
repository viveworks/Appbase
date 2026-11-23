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

import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * The reactive read face of a {@link LogStore} — mirrors the three blocking {@link LogStore} reads that
 * block on file I/O, offloading each onto the configured
 * {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary} instead of the calling
 * thread.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ReactiveLogStore {
    /**
     * Reactively emit {@link LogRecord}s between start timestamp (inclusive) and end timestamp
     * (exclusive). Implementations that can observe cancellation while fetching records should do
     * so; the default preserves the existing collection-read contract for other stores.
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return a record stream, or an error
     */
    default Flux<LogRecord> streamLogRecordsBetweenReactive(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize) {
        return readLogRecordsBetweenReactive(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize)
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Reactively read {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return a {@link Mono} that emits the result collection, or errors
     */
    Mono<Collection<LogRecord>> readLogRecordsBetweenReactive(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize);

    /**
     * Reactively read {@link LogRecord}s at a specific timestamp.
     *
     * @param timestamp Specific log timestamp
     * @param pageSize Page size of the result
     * @return a {@link Mono} that emits the result collection, or errors
     */
    Mono<Collection<LogRecord>> readLogRecordsAtReactive(long timestamp, int pageSize);

    /**
     * Reactively read {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteria Matching log record criteria
     * @param pageSize Page size of the result
     * @return a {@link Mono} that emits the result collection, or errors
     */
    Mono<Collection<LogRecord>> readLogRecordsByCriteriaReactive(LogRecordCriteria logRecordCriteria, int pageSize);
}
