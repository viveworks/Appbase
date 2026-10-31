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

package com.viveworks.appbase.core.service.exception.store.inmemory;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Map} based implementation of {@link ExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class InMemoryExceptionStore implements ExceptionStore {
    private final AtomicReference<InMemoryExceptionStoreConfiguration> inMemoryExceptionStoreConfigurationRef;
    private final AtomicInteger idGenerator;
    private final StackTraceHashGenerator stackTraceHashGenerator;
    private final ExceptionMessageContextCodec exceptionMessageContextCodec;
    private final AtomicReference<String> discriminatorRef;
    private final AtomicReference<Integer> stackTraceSerializationMaxLengthRef;
    private final Map<Integer, ExceptionMessageDetails> exceptionMessageDetailsByIdMap;
    private final NavigableMap<Long, Set<Integer>> idsByTimestampMap;
    private final Map<String, Set<Integer>> idsByThrowableTypeNameMap;
    private final AtomicBoolean shutdown;

    public InMemoryExceptionStore(final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration) {
        this(new DefaultStackTraceHashGenerator(),
                new DefaultExceptionMessageContextCodec(),
                inMemoryExceptionStoreConfiguration);
    }

    public InMemoryExceptionStore(final StackTraceHashGenerator stackTraceHashGenerator,
                                  final ExceptionMessageContextCodec exceptionMessageContextCodec,
                                  final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration)
    {
        AppbaseUtil.notNull(inMemoryExceptionStoreConfiguration, "inMemoryExceptionStoreConfiguration");
        InMemoryExceptionStoreConfigurationValidator.validate(
                inMemoryExceptionStoreConfiguration.getStackTraceSerializationMaxLength());

        AppbaseUtil.notNull(exceptionMessageContextCodec, "exceptionMessageContextCodec");
        AppbaseUtil.notNull(stackTraceHashGenerator, "stackTraceHashGenerator");

        this.inMemoryExceptionStoreConfigurationRef = new AtomicReference<>(inMemoryExceptionStoreConfiguration);
        this.idGenerator = new AtomicInteger(0);
        this.stackTraceHashGenerator = stackTraceHashGenerator;
        this.exceptionMessageContextCodec = exceptionMessageContextCodec;
        this.discriminatorRef = new AtomicReference<>(inMemoryExceptionStoreConfiguration.getDiscriminator());
        this.stackTraceSerializationMaxLengthRef = new AtomicReference<>(
                inMemoryExceptionStoreConfiguration.getStackTraceSerializationMaxLength());
        this.exceptionMessageDetailsByIdMap = new ConcurrentHashMap<>();
        this.idsByTimestampMap = new TreeMap<>(Long::compare);
        this.idsByThrowableTypeNameMap = new ConcurrentHashMap<>();
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public ExceptionMessageDetails readExceptionMessageDetailsById(final int id) {
        return exceptionMessageDetailsByIdMap.get(id);
    }

    @Override
    public CompletableFuture<ExceptionMessageDetails> asyncReadExceptionMessageDetailsById(final int id) {
        return CompletableFuture.completedFuture(readExceptionMessageDetailsById(id));
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetails() {
        return exceptionMessageDetailsByIdMap.values();
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetails() {
        return CompletableFuture.completedFuture(readAllExceptionMessageDetails());
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
    {
        final List<ExceptionMessageDetails> resultSet = new ArrayList<>();
        final Map<Long, Set<Integer>> subMap =
                idsByTimestampMap.subMap(inclusiveMinTimestamp, true, exclusiveMaxTimestamp - 1, false);

        if (subMap != null) {
            subMap.forEach((timestamp, ids) -> {
                if (timestamp != null && ids != null) {
                    ids.forEach((id) -> {
                        if (id != null) {
                            final ExceptionMessageDetails exceptionMessageDetails =
                                    exceptionMessageDetailsByIdMap.get(id);
                            if (exceptionMessageDetails != null) {
                                resultSet.add(exceptionMessageDetails);
                            }
                        }
                    });
                }
            });
        }

        return resultSet;
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp,
            final long exclusiveMaxTimestamp)
    {
        return CompletableFuture.completedFuture(
                readAllExceptionMessageDetailsByTimestamps(inclusiveMinTimestamp, exclusiveMaxTimestamp));
    }

    @Override
    public Collection<String> readUniqueThrowableTypeNames() {
        return idsByThrowableTypeNameMap.keySet();
    }

    @Override
    public CompletableFuture<Collection<String>> asyncReadUniqueThrowableTypeNames() {
        return CompletableFuture.completedFuture(readUniqueThrowableTypeNames());
    }

    @Override
    public Collection<ExceptionMessageDetails> readExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName)
    {
        AppbaseUtil.notNull(throwableTypeName, "throwableTypeName");

        final List<ExceptionMessageDetails> resultSet = new ArrayList<>();
        final Set<Integer> exceptionMessageDetailsMap = idsByThrowableTypeNameMap.get(throwableTypeName);

        if (exceptionMessageDetailsMap != null) {
            exceptionMessageDetailsMap.forEach((id) -> {
                if (id != null) {
                    final ExceptionMessageDetails exceptionMessageDetails = exceptionMessageDetailsByIdMap.get(id);
                    if (exceptionMessageDetails != null) {
                        resultSet.add(exceptionMessageDetails);
                    }
                }
            });
        }

        return resultSet;
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName)
    {
        return CompletableFuture.completedFuture(
                readExceptionMessageDetailsByThrowableTypeName(throwableTypeName));
    }

    @Override
    public void writeExceptionMessage(final ExceptionMessage exceptionMessage) {
        AppbaseUtil.notNull(exceptionMessage, "exceptionMessage");

        final Class<?> sourceType = exceptionMessage.getSourceType();

        final int id = idGenerator.getAndIncrement();
        final long timestamp = exceptionMessage.getTimestamp();
        final ExceptionMessageDetails exceptionMessageDetails = new ExceptionMessageDetails();
        exceptionMessageDetails.setId(id);
        exceptionMessageDetails.setDiscriminator(discriminatorRef.get());
        exceptionMessageDetails.setTimestamp(timestamp);
        exceptionMessageDetails.setMessage(exceptionMessage.getMessage());
        exceptionMessageDetails.setSourceTypeName((sourceType != null) ? sourceType.getName() : null);
        exceptionMessageDetails.setThreadName(exceptionMessage.getThreadName());

        final Throwable throwable = exceptionMessage.getThrowable();
        String throwableTypeName = null;
        if (throwable != null) {
            exceptionMessageDetails.setThrowableMessage(throwable.getMessage());

            final String stackTraceString = ExceptionStore.serializeThrowable(
                    throwable, stackTraceSerializationMaxLengthRef.get());
            exceptionMessageDetails.setStackTrace(stackTraceString);

            final int hash = stackTraceHashGenerator.generateHash(stackTraceString);
            exceptionMessageDetails.setStackTraceHash(hash);

            throwableTypeName = throwable.getClass().getName();
            exceptionMessageDetails.setThrowableTypeName(throwableTypeName);
        }

        final Map<String, String> context = exceptionMessage.getContext();
        if (context != null) {
            final String encodedContext = exceptionMessageContextCodec.encode(context);
            exceptionMessageDetails.setContext(encodedContext);
        }

        exceptionMessageDetailsByIdMap.put(id, exceptionMessageDetails);
        idsByTimestampMap
                .computeIfAbsent(timestamp, f -> new ConcurrentSkipListSet<>())
                .add(id);
        if (!AppbaseUtil.isBlank(throwableTypeName)) {
            idsByThrowableTypeNameMap
                    .computeIfAbsent(throwableTypeName, f -> new ConcurrentSkipListSet<>())
                    .add(id);
        }
    }

    @Override
    public CompletableFuture<Void> asyncWriteExceptionMessage(final ExceptionMessage exceptionMessage) {
        writeExceptionMessage(exceptionMessage);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void removeExceptionMessageDetailsById(final int id) {
        exceptionMessageDetailsByIdMap.remove(id);

        idsByThrowableTypeNameMap.forEach((throwableTypeName, ids) -> {
            if (throwableTypeName != null && ids != null) {
                ids.forEach((existingId) -> {
                    if (existingId != null && existingId == id) {
                        ids.remove(id);
                    }
                });

                if (ids.isEmpty()) {
                    idsByThrowableTypeNameMap.remove(throwableTypeName);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsById(final int id) {
        removeExceptionMessageDetailsById(id);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void removeExceptionMessageDetailsExclusiveBefore(final long timestamp) {
        final NavigableMap<Long, Set<Integer>> headMap = idsByTimestampMap.headMap(timestamp, false);
        if (headMap != null) {
            headMap.forEach((timestampKey, ids) -> {
                if (timestampKey != null && ids != null) {
                    ids.forEach((id) -> {
                        if (id != null) {
                            exceptionMessageDetailsByIdMap.remove(id);
                        }
                    });
                }
            });
        }

    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsExclusiveBefore(final long timestamp) {
        removeExceptionMessageDetailsExclusiveBefore(timestamp);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <C extends ExceptionStoreConfiguration> void reload(
            final C exceptionStoreConfiguration) throws ExceptionStoreException
    {
        AppbaseUtil.notNull(exceptionStoreConfiguration, "exceptionStoreConfiguration");

        if (!(exceptionStoreConfiguration instanceof InMemoryExceptionStoreConfiguration)) {
            throw new ExceptionStoreException(
                    "exceptionStoreConfiguration must be of type " +
                            InMemoryExceptionStoreConfiguration.class.getName());
        }

        final InMemoryExceptionStoreConfiguration newInMemoryExceptionStoreConfiguration =
                (InMemoryExceptionStoreConfiguration) exceptionStoreConfiguration;
        discriminatorRef.set(
                newInMemoryExceptionStoreConfiguration.getDiscriminator());
        stackTraceSerializationMaxLengthRef.set(
                newInMemoryExceptionStoreConfiguration.getStackTraceSerializationMaxLength());
        clear();

        InMemoryExceptionStoreConfigurationValidator.validate(
                newInMemoryExceptionStoreConfiguration.getStackTraceSerializationMaxLength());
        this.inMemoryExceptionStoreConfigurationRef.set(newInMemoryExceptionStoreConfiguration);
    }

    private void clear() {
        exceptionMessageDetailsByIdMap.clear();
        idsByTimestampMap.clear();
        idsByThrowableTypeNameMap.clear();
    }

    @Override
    public void synchronize() throws ExceptionStoreException {
        // no-op
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        clear();
    }

    public InMemoryExceptionStoreConfiguration getInMemoryExceptionStoreConfiguration() {
        return inMemoryExceptionStoreConfigurationRef.get();
    }
}
