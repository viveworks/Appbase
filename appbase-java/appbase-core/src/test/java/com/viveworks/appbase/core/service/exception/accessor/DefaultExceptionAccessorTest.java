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

package com.viveworks.appbase.core.service.exception.accessor;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.eclipse.microprofile.metrics.Gauge;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultExceptionAccessorTest {
    private static final long OPERATION_TIMEOUT_SECONDS = 10L;

    private static LogService logService;
    private static MetricService metricService;
    private static MediatorService mediatorService;
    private static ValidationService validationService;
    private static DefaultExceptionAccessor defaultExceptionAccessor;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getPlainLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        defaultExceptionAccessor = CoreTestFixtures.getExceptionAccessor(
                logService, metricService, mediatorService, validationService);
    }

    @AfterAll
    static void tearDown() {
        if (defaultExceptionAccessor != null) {
            defaultExceptionAccessor.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    @Order(1)
    void testAccessor() throws Exception {
        runTests(defaultExceptionAccessor);

        final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                CoreTestFixtures.getDefaultExceptionServiceConfiguration();
        defaultExceptionAccessor.reload(
                defaultExceptionServiceConfiguration.getDefaultExceptionAccessorConfiguration());
        runTests(defaultExceptionAccessor);

        final Gauge exceptionStoreByTypeNameMapSizeGauge =
                defaultExceptionAccessor.getExceptionStoreByTypeNameMapSizeGauge();
        Assertions.assertNotNull(exceptionStoreByTypeNameMapSizeGauge);

        Assertions.assertNotNull(defaultExceptionAccessor.getExceptionStoreByTypeNameMapSizeGauge());
        Assertions.assertNotNull(
                defaultExceptionAccessor.getExceptionStoreConfigurationByExceptionStoreTypeNameMapSizeGauge());
        Assertions.assertNotNull(defaultExceptionAccessor.getExceptionStoreConfigurationByTypeNameMapSizeGauge());
        Assertions.assertNotNull(
                defaultExceptionAccessor.getExceptionStoreFactoryByConfigurationTypeNameMapSizeGauge());


        Assertions.assertDoesNotThrow(defaultExceptionAccessor::shutdown);
    }

    private void runTests(final DefaultExceptionAccessor defaultExceptionAccessor) throws Exception {
        final long timestamp1 = System.currentTimeMillis();
        final ExceptionMessage exceptionMessage1 = CoreTestFixtures.getExceptionMessage();
        final Collection<String> writeExceptionMessages =
                defaultExceptionAccessor.writeExceptionMessage(exceptionMessage1);
        Assertions.assertNotNull(writeExceptionMessages);



        final AtomicReference<Integer> removeIdRef = new AtomicReference<>();
        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
                readAllExceptionMessageDetailsFutures = defaultExceptionAccessor.asyncReadAllExceptionMessageDetails();
        Assertions.assertNotNull(readAllExceptionMessageDetailsFutures);
        final AtomicReference<ExceptionMessageDetails> exceptionMessageDetails1Ref = new AtomicReference<>();
        readAllExceptionMessageDetailsFutures.forEach((storeName, resultFuture) -> {
            try {
                final Collection<ExceptionMessageDetails> exceptionMessageDetailsList =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(exceptionMessageDetailsList);
                Assertions.assertEquals(1, exceptionMessageDetailsList.size());
                final ExceptionMessageDetails exceptionMessageDetails = exceptionMessageDetailsList.iterator().next();
                removeIdRef.set(exceptionMessageDetails.getId());
                exceptionMessageDetails1Ref.set(exceptionMessageDetails);
                CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });

        final Map<String, Collection<ExceptionMessageDetails>> readAllExceptionMessageDetails =
                defaultExceptionAccessor.readAllExceptionMessageDetails();
        Assertions.assertNotNull(readAllExceptionMessageDetails);
        readAllExceptionMessageDetails.forEach((storeName, exceptionMessageDetailsList) -> {
            Assertions.assertNotNull(exceptionMessageDetailsList);
            Assertions.assertEquals(1, exceptionMessageDetailsList.size());
            final ExceptionMessageDetails exceptionMessageDetails = exceptionMessageDetailsList.iterator().next();
            removeIdRef.set(exceptionMessageDetails.getId());
            CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails);
        });



        final Map<String, ExceptionMessageDetails> readExceptionMessageDetailsByIds =
                defaultExceptionAccessor.readExceptionMessageDetailsById(exceptionMessageDetails1Ref.get().getId());
        Assertions.assertNotNull(readExceptionMessageDetailsByIds);
        readExceptionMessageDetailsByIds.forEach((storeName, exceptionMessageDetails) -> {
            Assertions.assertNotNull(exceptionMessageDetails);
            Assertions.assertEquals(exceptionMessageDetails1Ref.get(), exceptionMessageDetails);
        });



        final long timestamp2 = System.currentTimeMillis();
        final ExceptionMessage exceptionMessage2 = CoreTestFixtures.getExceptionMessage();
        exceptionMessage2.setThrowable(new NullPointerException());
        final Map<String, CompletableFuture<Void>> writeExceptionMessageFutures1 =
                defaultExceptionAccessor.asyncWriteExceptionMessage(exceptionMessage2);
        Assertions.assertNotNull(writeExceptionMessageFutures1);
        writeExceptionMessageFutures1.forEach((storeName, resultFuture) -> {
            try {
                final Void result = resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNull(result);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });
        final long timestamp3 = System.currentTimeMillis();
        final Map<String, Collection<ExceptionMessageDetails>>
                readAllExceptionMessageDetailsByTimestamps1 =
                defaultExceptionAccessor.readAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp2);
        Assertions.assertNotNull(readAllExceptionMessageDetailsByTimestamps1);
        Assertions.assertEquals(1, readAllExceptionMessageDetailsByTimestamps1.size());
        readAllExceptionMessageDetailsByTimestamps1.forEach((storeName, exceptionMessageDetailsList) -> {
            Assertions.assertNotNull(exceptionMessageDetailsList);
            Assertions.assertEquals(1, exceptionMessageDetailsList.size());
            final ExceptionMessageDetails exceptionMessageDetails3 = exceptionMessageDetailsList.iterator().next();
            CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails3);
        });

        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
                readAllExceptionMessageDetailsByTimestampsFutures2 =
                defaultExceptionAccessor.asyncReadAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp3);
        Assertions.assertNotNull(readAllExceptionMessageDetailsByTimestampsFutures2);
        Assertions.assertEquals(1, readAllExceptionMessageDetailsByTimestampsFutures2.size());
        final AtomicReference<ExceptionMessageDetails> exceptionMessageDetails4Ref = new AtomicReference<>();
        readAllExceptionMessageDetailsByTimestampsFutures2.forEach((storeName, resultFuture) -> {
            try {
                final Collection<ExceptionMessageDetails> exceptionMessageDetailsList =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(exceptionMessageDetailsList);
                Assertions.assertFalse(exceptionMessageDetailsList.isEmpty());
                final Iterator<ExceptionMessageDetails> iterator = exceptionMessageDetailsList.iterator();
                final ExceptionMessageDetails exceptionMessageDetails3 = iterator.next();
                CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails3);
                final ExceptionMessageDetails exceptionMessageDetails4 = iterator.next();
                exceptionMessageDetails4Ref.set(exceptionMessageDetails4);
                CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage2, exceptionMessageDetails4);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });



        final Map<String, CompletableFuture<Collection<String>>> readUniqueExceptionTypeNamesFutures =
                defaultExceptionAccessor.asyncReadUniqueExceptionTypeNames();
        Assertions.assertNotNull(readUniqueExceptionTypeNamesFutures);
        readUniqueExceptionTypeNamesFutures.forEach((storeName, resultFuture) -> {
            try {
                final Collection<String> typeNames = resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(typeNames);
                Assertions.assertEquals(2, typeNames.size());
                Assertions.assertTrue(typeNames.contains(AppbaseException.class.getName()));
                Assertions.assertTrue(typeNames.contains(NullPointerException.class.getName()));
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });

        final Map<String, Collection<String>> readUniqueExceptionTypeNames =
                defaultExceptionAccessor.readUniqueExceptionTypeNames();
        Assertions.assertNotNull(readUniqueExceptionTypeNames);
        readUniqueExceptionTypeNames.forEach((storeName, typeNames) -> {
            Assertions.assertNotNull(typeNames);
            Assertions.assertEquals(2, typeNames.size());
            Assertions.assertTrue(typeNames.contains(AppbaseException.class.getName()));
            Assertions.assertTrue(typeNames.contains(NullPointerException.class.getName()));
        });


        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
                readExceptionMessageDetailsByTypeNameFutures1 =
                defaultExceptionAccessor.asyncReadExceptionMessageDetailsByThrowableTypeName(
                        AppbaseException.class.getName());
        Assertions.assertNotNull(readExceptionMessageDetailsByTypeNameFutures1);
        readExceptionMessageDetailsByTypeNameFutures1.forEach((storeName, resultFuture) -> {
            try {
                final Collection<ExceptionMessageDetails> exceptionMessageDetailsList =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(exceptionMessageDetailsList);
                Assertions.assertEquals(1, exceptionMessageDetailsList.size());
                CoreTestFixtures.checkExceptionMessageDetails(
                        exceptionMessage1, exceptionMessageDetailsList.iterator().next());
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });


        final Map<String, Collection<ExceptionMessageDetails>>
                readExceptionMessageDetailsByTypeNames1 =
                defaultExceptionAccessor.readExceptionMessageDetailsByThrowableTypeName(
                        AppbaseException.class.getName());
        Assertions.assertNotNull(readExceptionMessageDetailsByTypeNames1);
        readExceptionMessageDetailsByTypeNames1.forEach((storeName, exceptionMessageDetailsList) -> {
            Assertions.assertNotNull(exceptionMessageDetailsList);
            Assertions.assertEquals(1, exceptionMessageDetailsList.size());
            CoreTestFixtures.checkExceptionMessageDetails(
                    exceptionMessage1, exceptionMessageDetailsList.iterator().next());
        });



        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
                readExceptionMessageDetailsByTypeNameFutures2 =
                defaultExceptionAccessor.asyncReadExceptionMessageDetailsByThrowableTypeName(
                        NullPointerException.class.getName());
        Assertions.assertNotNull(readExceptionMessageDetailsByTypeNameFutures2);
        readExceptionMessageDetailsByTypeNameFutures2.forEach((storeName, resultFuture) -> {
            try {
                final Collection<ExceptionMessageDetails> exceptionMessageDetailsList =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(exceptionMessageDetailsList);
                Assertions.assertEquals(1, exceptionMessageDetailsList.size());
                CoreTestFixtures.checkExceptionMessageDetails(
                        exceptionMessage2, exceptionMessageDetailsList.iterator().next());
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });


        final Map<String, ExceptionMessageDetails> readExceptionMessageDetailsByIds1 =
                defaultExceptionAccessor.readExceptionMessageDetailsById(removeIdRef.get());
        Assertions.assertNotNull(readExceptionMessageDetailsByIds1);

        final Collection<String> removeExceptionMessageDetailsByIds =
                defaultExceptionAccessor.removeExceptionMessageDetailsById(removeIdRef.get());
        Assertions.assertNotNull(removeExceptionMessageDetailsByIds);

        final Map<String, CompletableFuture<Void>> removeExceptionMessageDetailsByIdFutures =
                defaultExceptionAccessor.asyncRemoveExceptionMessageDetailsById(removeIdRef.get());
        Assertions.assertNotNull(removeExceptionMessageDetailsByIdFutures);
        removeExceptionMessageDetailsByIdFutures.forEach((storeName, resultFuture) -> {
            try {
                final Void result = resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNull(result);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });



        final Map<String, CompletableFuture<ExceptionMessageDetails>> readExceptionMessageDetailsByIdFutures2 =
                defaultExceptionAccessor.asyncReadExceptionMessageDetailsById(exceptionMessageDetails4Ref.get().getId());
        Assertions.assertNotNull(readExceptionMessageDetailsByIdFutures2);
        readExceptionMessageDetailsByIdFutures2.forEach((storeName, resultFuture) -> {
            try {
                final ExceptionMessageDetails exceptionMessageDetails =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(exceptionMessageDetails);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });
        final Map<String, CompletableFuture<Void>> removeExceptionMessageDetailsBeforeFutures =
                defaultExceptionAccessor.asyncRemoveExceptionMessageDetailsExclusiveBefore(System.currentTimeMillis());
        Assertions.assertNotNull(removeExceptionMessageDetailsBeforeFutures);
        removeExceptionMessageDetailsBeforeFutures.forEach((storeName, resultFuture) -> {
            try {
                final Void result = resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNull(result);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });
        final Collection<String> removeExceptionMessageDetailsBefores =
                defaultExceptionAccessor.removeExceptionMessageDetailsExclusiveBefore(System.currentTimeMillis());
        Assertions.assertNotNull(removeExceptionMessageDetailsBefores);

        final Map<String, CompletableFuture<ExceptionMessageDetails>> readExceptionMessageDetailsByIdFutures3 =
                defaultExceptionAccessor.asyncReadExceptionMessageDetailsById(exceptionMessageDetails4Ref.get().getId());
        Assertions.assertNotNull(readExceptionMessageDetailsByIdFutures3);
        readExceptionMessageDetailsByIdFutures3.forEach((storeName, resultFuture) -> {
            try {
                final ExceptionMessageDetails exceptionMessageDetails =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNull(exceptionMessageDetails);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });



        final Map<String, CompletableFuture<ExceptionMessageDetails>> noReadExceptionMessageDetailsByIdFutures =
                defaultExceptionAccessor.asyncReadExceptionMessageDetailsById(1111);
        Assertions.assertNotNull(noReadExceptionMessageDetailsByIdFutures);
        noReadExceptionMessageDetailsByIdFutures.forEach((storeName, resultFuture) -> {
            try {
                final ExceptionMessageDetails noExceptionMessageDetails =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNull(noExceptionMessageDetails);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });



        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
                noReadExceptionMessageDetailsByTypeNameFutures =
                defaultExceptionAccessor.asyncReadExceptionMessageDetailsByThrowableTypeName(
                        ClassNotFoundException.class.getName());
        Assertions.assertNotNull(noReadExceptionMessageDetailsByTypeNameFutures);
        noReadExceptionMessageDetailsByTypeNameFutures.forEach((storeName, resultFuture) -> {
            try {
                final Collection<ExceptionMessageDetails> noExceptionMessageDetailsList =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(noExceptionMessageDetailsList);
                Assertions.assertTrue(noExceptionMessageDetailsList.isEmpty());
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });



        final long current = System.currentTimeMillis();
        final long after = current + 1000;
        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
                noReadAllExceptionMessageDetailsByTimestampsFutures =
                defaultExceptionAccessor.asyncReadAllExceptionMessageDetailsByTimestamps(current, after);
        Assertions.assertNotNull(noReadAllExceptionMessageDetailsByTimestampsFutures);
        noReadAllExceptionMessageDetailsByTimestampsFutures.forEach((storeName, resultFuture) -> {
            try {
                final Collection<ExceptionMessageDetails> noExceptionMessageDetailsList =
                        resultFuture.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assertions.assertNotNull(noExceptionMessageDetailsList);
                Assertions.assertTrue(noExceptionMessageDetailsList.isEmpty());
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });

        Assertions.assertDoesNotThrow(defaultExceptionAccessor::synchronizeExceptionStores);
    }

    @Test
    @Order(2)
    void testCollections() throws Exception {
        defaultExceptionAccessor.clearExceptionStoreFactoryByConfigurationTypeMap();
        defaultExceptionAccessor.clearExceptionStores();

        defaultExceptionAccessor.registerExceptionStoreFactoryByConfigurationType(
                TestExceptionStoreConfiguration.class, new TestExceptionStoreFactory());
        final Map<String, ExceptionStoreFactory> exceptionStoreFactoryByConfigurationTypeNameMap =
                defaultExceptionAccessor.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap);
        Assertions.assertEquals(1, exceptionStoreFactoryByConfigurationTypeNameMap.size());
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap
                .containsKey(TestExceptionStoreConfiguration.class.getName()));

        defaultExceptionAccessor.unregisterExceptionStoreFactoryByConfigurationType(
                TestExceptionStoreConfiguration.class, TestExceptionStoreFactory.class);
        final Map<String, ExceptionStoreFactory> exceptionStoreFactoryByConfigurationTypeNameMap1 =
                defaultExceptionAccessor.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap1);
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap1.isEmpty());

        defaultExceptionAccessor.registerExceptionStoreFactoryByConfigurationType(
                TestExceptionStoreConfiguration.class, new TestExceptionStoreFactory());
        final Map<String, ExceptionStoreFactory>
                exceptionStoreFactoryByConfigurationTypeNameMap2 =
                defaultExceptionAccessor.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap2);
        Assertions.assertEquals(1, exceptionStoreFactoryByConfigurationTypeNameMap2.size());
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap2
                .containsKey(TestExceptionStoreConfiguration.class.getName()));

        defaultExceptionAccessor.clearExceptionStoreFactoryByConfigurationTypeMap();
        final Map<String, ExceptionStoreFactory>
                exceptionStoreFactoryByConfigurationTypeNameMap3 =
                defaultExceptionAccessor.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap3);
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap3.isEmpty());


        defaultExceptionAccessor.registerExceptionStore(new TestExceptionStore());
        final Collection<ExceptionStore> exceptionStores = defaultExceptionAccessor.getExceptionStores();
        Assertions.assertNotNull(exceptionStores);
        Assertions.assertEquals(1, exceptionStores.size());

        defaultExceptionAccessor.unregisterExceptionStore(TestExceptionStore.class);
        final Collection<ExceptionStore> exceptionStores1 = defaultExceptionAccessor.getExceptionStores();
        Assertions.assertNotNull(exceptionStores1);
        Assertions.assertTrue(exceptionStores1.isEmpty());

        defaultExceptionAccessor.registerExceptionStore(new TestExceptionStore());
        final Collection<ExceptionStore> exceptionStores2 = defaultExceptionAccessor.getExceptionStores();
        Assertions.assertNotNull(exceptionStores2);
        Assertions.assertEquals(1, exceptionStores2.size());

        defaultExceptionAccessor.clearExceptionStores();
        final Collection<ExceptionStore> exceptionStores3 = defaultExceptionAccessor.getExceptionStores();
        Assertions.assertNotNull(exceptionStores3);
        Assertions.assertTrue(exceptionStores3.isEmpty());
    }

    private static class TestExceptionStore implements ExceptionStore {
        @Override
        public ExceptionMessageDetails readExceptionMessageDetailsById(final int id) throws ExceptionStoreException {
            return null;
        }

        @Override
        public CompletableFuture<ExceptionMessageDetails> asyncReadExceptionMessageDetailsById(final int id) {
            return null;
        }

        @Override
        public Collection<ExceptionMessageDetails> readAllExceptionMessageDetails() {
            return null;
        }

        @Override
        public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetails() {
            return null;
        }

        @Override
        public Collection<ExceptionMessageDetails> readAllExceptionMessageDetailsByTimestamps(
                final long inclusiveMinTimestamp,
                final long exclusiveMaxTimestamp)
        {
            return null;
        }

        @Override
        public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetailsByTimestamps(
                final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
        {
            return null;
        }

        @Override
        public Collection<String> readUniqueThrowableTypeNames() {
            return null;
        }

        @Override
        public CompletableFuture<Collection<String>> asyncReadUniqueThrowableTypeNames() {
            return null;
        }

        @Override
        public Collection<ExceptionMessageDetails> readExceptionMessageDetailsByThrowableTypeName(
                final String throwableTypeName) throws ExceptionStoreException
        {
            return null;
        }

        @Override
        public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsByThrowableTypeName(
                final String throwableTypeName)
        {
            return null;
        }

        @Override
        public void writeExceptionMessage(final ExceptionMessage exceptionMessage) {
        }

        @Override
        public CompletableFuture<Void> asyncWriteExceptionMessage(final ExceptionMessage exceptionMessage) {
            return null;
        }

        @Override
        public void removeExceptionMessageDetailsById(final int id) throws ExceptionStoreException {
        }

        @Override
        public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsById(final int id) {
            return null;
        }

        @Override
        public void removeExceptionMessageDetailsExclusiveBefore(final long timestamp) throws ExceptionStoreException {
        }

        @Override
        public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsExclusiveBefore(final long timestamp) {
            return null;
        }

        @Override
        public <C extends ExceptionStoreConfiguration> void reload(
                final C exceptionStoreConfiguration) throws ExceptionStoreException
        {
        }

        @Override
        public void synchronize() throws ExceptionStoreException {
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public void shutdown() throws AppbaseRuntimeException {
        }
    }

    private static class TestExceptionStoreConfiguration implements ExceptionStoreConfiguration {
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends ExceptionStore> Class<S> getExceptionStoreType() {
            return (Class<S>) TestExceptionStore.class;
        }
    }

    private static class TestExceptionStoreFactory
            implements ExceptionStoreFactory<TestExceptionStore, TestExceptionStoreConfiguration>
    {
        @Override
        public TestExceptionStore newExceptionStore(
                final TestExceptionStoreConfiguration exceptionStoreConfiguration)
                throws ExceptionStoreFactoryException
        {
            return new TestExceptionStore();
        }
    }
}
