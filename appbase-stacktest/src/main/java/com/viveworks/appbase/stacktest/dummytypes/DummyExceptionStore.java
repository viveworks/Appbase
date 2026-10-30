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
package com.viveworks.appbase.stacktest.dummytypes;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyExceptionStore implements ExceptionStore {
    @Override
    public void shutdown() throws AppbaseRuntimeException {
        // no-op
    }

    @Override
    public boolean isShutdown() {
        // no-op
        return false;
    }

    @Override
    public ExceptionMessageDetails readExceptionMessageDetailsById(
            final int id) throws ExceptionStoreException
    {
        // no-op
        return null;
    }

    @Override
    public CompletableFuture<ExceptionMessageDetails> asyncReadExceptionMessageDetailsById(
            final int id)
    {
        // no-op
        return null;
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetails()
            throws ExceptionStoreException
    {
        // no-op
        return null;
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetails() {
        // no-op
        return null;
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp,
            final long exclusiveMaxTimestamp) throws ExceptionStoreException
    {
        // no-op
        return null;
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp,
            final long exclusiveMaxTimestamp)
    {
        // no-op
        return null;
    }

    @Override
    public Collection<String> readUniqueThrowableTypeNames() throws ExceptionStoreException {
        // no-op
        return null;
    }

    @Override
    public CompletableFuture<Collection<String>> asyncReadUniqueThrowableTypeNames() {
        // no-op
        return null;
    }

    @Override
    public Collection<ExceptionMessageDetails> readExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName) throws ExceptionStoreException
    {
        // no-op
        return null;
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName)
    {
        // no-op
        return null;
    }

    @Override
    public void writeExceptionMessage(final ExceptionMessage exceptionMessage) throws ExceptionStoreException {
        // no-op
    }

    @Override
    public CompletableFuture<Void> asyncWriteExceptionMessage(final ExceptionMessage exceptionMessage) {
        // no-op
        return null;
    }

    @Override
    public void removeExceptionMessageDetailsById(final int id) throws ExceptionStoreException {
        // no-op
    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsById(final int id) {
        // no-op
        return null;
    }

    @Override
    public void removeExceptionMessageDetailsExclusiveBefore(
            final long timestamp) throws ExceptionStoreException
    {
        // no-op
    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsExclusiveBefore(
            final long timestamp)
    {
        // no-op
        return null;
    }

    @Override
    public <C extends ExceptionStoreConfiguration> void reload(
            final C exceptionStoreConfiguration) throws ExceptionStoreException
    {
        // no-op
    }

    @Override
    public void synchronize() throws ExceptionStoreException {
        // no-op
    }
}
