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

package com.viveworks.appbase.core.service.exception.store;

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an exception store.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ExceptionStore extends Shutdownable {
    int DEFAULT_STACK_TRACE_SERIALIZATION_MAX_LENGTH = 1024;

    /**
     * Read {@link ExceptionMessageDetails} by ID.
     *
     * @param id ID
     * @return {@link ExceptionMessageDetails} if exists, else null
     */
    ExceptionMessageDetails readExceptionMessageDetailsById(int id) throws ExceptionStoreException;

    /**
     * Asynchronously read {@link ExceptionMessageDetails} by ID.
     *
     * @param id ID
     * @return {@link ExceptionMessageDetails} if exists, else null
     */
    CompletableFuture<ExceptionMessageDetails> asyncReadExceptionMessageDetailsById(int id);


    /**
     * Read all {@link ExceptionMessageDetails}.
     *
     * @return All {@link ExceptionMessageDetails}
     */
    Collection<ExceptionMessageDetails> readAllExceptionMessageDetails() throws ExceptionStoreException;


    /**
     * Asynchronously read all {@link ExceptionMessageDetails}.
     *
     * @return All {@link ExceptionMessageDetails}
     */
    CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetails();


    /**
     * Read all {@link ExceptionMessageDetails} by timestamp.
     *
     * @param inclusiveMinTimestamp Minimum timestamp (inclusive)
     * @param exclusiveMaxTimestamp Maximum timestamp (exclusive)
     * @return All {@link ExceptionMessageDetails} by timestamp, else null
     */
    Collection<ExceptionMessageDetails> readAllExceptionMessageDetailsByTimestamps(
            long inclusiveMinTimestamp, long exclusiveMaxTimestamp) throws ExceptionStoreException;


    /**
     * Asynchronously read all {@link ExceptionMessageDetails} by timestamp.
     *
     * @param inclusiveMinTimestamp Minimum timestamp (inclusive)
     * @param exclusiveMaxTimestamp Maximum timestamp (exclusive)
     * @return All {@link ExceptionMessageDetails} by timestamp, else null
     */
    CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetailsByTimestamps(
            long inclusiveMinTimestamp, long exclusiveMaxTimestamp);


    /**
     * Read unique exception type names.
     *
     * @return Unique exception type names if exists, else null
     */
    Collection<String> readUniqueThrowableTypeNames() throws ExceptionStoreException;


    /**
     * Asynchronously read unique exception type names.
     *
     * @return Unique exception type names if exists, else null
     */
    CompletableFuture<Collection<String>> asyncReadUniqueThrowableTypeNames();


    /**
     * Read {@link ExceptionMessageDetails} by type name.
     *
     * @param throwableTypeName Exception type name
     * @return {@link ExceptionMessageDetails} by type name if exists, else null
     */
    Collection<ExceptionMessageDetails> readExceptionMessageDetailsByThrowableTypeName(
            String throwableTypeName) throws ExceptionStoreException;


    /**
     * Asynchronously read {@link ExceptionMessageDetails} by type name.
     *
     * @param throwableTypeName Exception type name
     * @return {@link ExceptionMessageDetails} by type name if exists, else null
     */
    CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsByThrowableTypeName(
            String throwableTypeName);


    /**
     * Write an {@link ExceptionMessage} to the store.
     *
     * @param exceptionMessage Exception message
     */
    void writeExceptionMessage(ExceptionMessage exceptionMessage) throws ExceptionStoreException;


    /**
     * Asynchronously write an {@link ExceptionMessage} to the store.
     *
     * @param exceptionMessage Exception message
     * @return Result
     */
    CompletableFuture<Void> asyncWriteExceptionMessage(ExceptionMessage exceptionMessage);


    /**
     * Remove {@link ExceptionMessageDetails} by ID.
     *
     * @param id {@link ExceptionMessageDetails} ID
     */
    void removeExceptionMessageDetailsById(int id) throws ExceptionStoreException;


    /**
     * Asynchronously remove {@link ExceptionMessageDetails} by ID.
     *
     * @param id {@link ExceptionMessageDetails} ID
     * @return Result
     */
    CompletableFuture<Void> asyncRemoveExceptionMessageDetailsById(int id);


    /**
     * Remove {@link ExceptionMessageDetails} before certain timestamp (exclusive).
     *
     * @param timestamp Exclusive before timestamp
     */
    void removeExceptionMessageDetailsExclusiveBefore(long timestamp) throws ExceptionStoreException;


    /**
     * Asynchronously remove {@link ExceptionMessageDetails} before certain timestamp (exclusive).
     *
     * @param timestamp Exclusive before timestamp
     * @return Result
     */
    CompletableFuture<Void> asyncRemoveExceptionMessageDetailsExclusiveBefore(long timestamp);


    /**
     * Reload the store with the new configuration.
     *
     * @param exceptionStoreConfiguration New configuration
     * @param <C> Configuration type
     * @throws ExceptionStoreException if there was any error
     */
    <C extends ExceptionStoreConfiguration> void reload(C exceptionStoreConfiguration) throws ExceptionStoreException;


    /**
     * Used to synchronize the store.
     *
     * @throws ExceptionStoreException if there were any errors
     */
    void synchronize() throws ExceptionStoreException;


    /**
     * Serialize throwable into string of a specified length.
     *
     * @param throwable Throwable to serialize
     * @param stackTraceSerializationMaxLength Length of the serialized string
     * @return Serialized string
     */
    static String serializeThrowable(
            final Throwable throwable,
            final int stackTraceSerializationMaxLength)
    {
        AppbaseUtil.notNull(throwable, "throwable");

        final StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        String stackTraceString = stringWriter.toString();
        if (stackTraceString.length() > stackTraceSerializationMaxLength) {
            stackTraceString = stackTraceString.substring(0, stackTraceSerializationMaxLength);
        }

        return stackTraceString;
    }
}
