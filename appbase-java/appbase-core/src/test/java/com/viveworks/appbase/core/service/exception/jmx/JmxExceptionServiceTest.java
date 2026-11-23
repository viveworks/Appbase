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

package com.viveworks.appbase.core.service.exception.jmx;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.exception.DefaultExceptionService;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessor;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.listener.ExceptionListener;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStore;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxExceptionServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ConfigurationService configurationService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static ExceptionAccessor exceptionAccessor;
    private static DefaultExceptionService exceptionService;
    private static DefaultJmxService jmxService;
    private static JmxExceptionService jmxExceptionService;

    @BeforeEach
    void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = new DefaultValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validationService);
        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);

        configurationService.putTypedConfiguration(
                DefaultExceptionServiceConfiguration.class,
                CoreTestFixtures.getDefaultExceptionServiceConfiguration());
        exceptionAccessor = new DefaultExceptionAccessor(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationService,
                new DefaultExceptionMessageContextCodec());
        final EntityManager entityManager = Persistence
                .createEntityManagerFactory("testAppbasePersistenceUnit2")
                .createEntityManager();
        final JpaExceptionStore jpaExceptionStore = new JpaExceptionStore(
                logService,
                metricService,
                mediatorService,
                validationService,
                entityManager,
                new DefaultStackTraceHashGenerator(),
                CoreTestFixtures.getJpaExceptionStoreConfiguration(),
                CoreTestFixtures.getContextCodec());
        exceptionAccessor.registerExceptionStore(jpaExceptionStore);
        exceptionService = new DefaultExceptionService(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationService,
                exceptionAccessor);

        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxExceptionService.class);
        jmxExceptionService = new JmxExceptionService(
                logService,
                exceptionService,
                DefaultExceptionServiceConfiguration.class,
                configurationService);
    }

    @AfterEach
    void tearDown() {
        if (jmxExceptionService != null) {
            jmxExceptionService.shutdown();
        }

        if (jmxService != null) {
            jmxService.shutdown();
        }

        if (exceptionService != null) {
            exceptionService.shutdown();
        }

        if (exceptionAccessor != null) {
            exceptionAccessor.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    @Order(1)
    void testMXBean() throws Exception {
        final Class<? extends JmxExceptionService> exceptionServiceType = jmxExceptionService.getClass();
        if (jmxService.isMXBeanRegistered(exceptionServiceType)) {
            jmxService.unregisterMXBean(exceptionServiceType);
        }
        jmxService.registerMXBean(jmxExceptionService);

        final JmxExceptionService mxBean = jmxService.getMXBean(JmxExceptionService.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(JmxExceptionService.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxExceptionService.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    @Order(2)
    void testCollections() throws Exception {
        jmxExceptionService.clearGlobalExceptionListeners();
        jmxExceptionService.clearExceptionListenersByExceptionTypeMap();

        final TypeExceptionListener typeExceptionListener = new TypeExceptionListener();
        exceptionService.registerExceptionListenerByExceptionType(ConfigurationException.class, typeExceptionListener);
        final Map<String, List<String>> exceptionListenersByExceptionTypeMap1 =
                jmxExceptionService.getExceptionListenersByExceptionTypeMap();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeMap1);
        Assertions.assertEquals(1, exceptionListenersByExceptionTypeMap1.size());
        Assertions.assertTrue(exceptionListenersByExceptionTypeMap1
                .containsKey(ConfigurationException.class.getName()));

        jmxExceptionService.unregisterExceptionListenerByExceptionType(
                ConfigurationException.class.getName(), TypeExceptionListener.class.getName());
        final Map<String, List<String>> exceptionListenersByExceptionTypeMap2 =
                jmxExceptionService.getExceptionListenersByExceptionTypeMap();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeMap2);
        Assertions.assertTrue(exceptionListenersByExceptionTypeMap2.isEmpty());

        exceptionService.registerExceptionListenerByExceptionType(ConfigurationException.class, typeExceptionListener);
        final Map<String, List<String>> exceptionListenersByExceptionTypeMap3 =
                jmxExceptionService.getExceptionListenersByExceptionTypeMap();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeMap3);
        Assertions.assertEquals(1, exceptionListenersByExceptionTypeMap3.size());
        Assertions.assertTrue(exceptionListenersByExceptionTypeMap3
                .containsKey(ConfigurationException.class.getName()));

        final GlobalExceptionListener globalExceptionListener = new GlobalExceptionListener();
        exceptionService.registerGlobalExceptionListener(globalExceptionListener);
        final List<String> globalExceptionListeners1 = jmxExceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners1);
        Assertions.assertEquals(1, globalExceptionListeners1.size());

        jmxExceptionService.unregisterGlobalExceptionListener(GlobalExceptionListener.class.getName());
        final List<String> globalExceptionListeners2 = jmxExceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners2);
        Assertions.assertTrue(globalExceptionListeners2.isEmpty());

        exceptionService.registerGlobalExceptionListener(globalExceptionListener);
        final List<String> globalExceptionListeners3 = jmxExceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners3);
        Assertions.assertEquals(1, globalExceptionListeners3.size());

        jmxExceptionService.clearGlobalExceptionListeners();
        final List<String> globalExceptionListeners4 = jmxExceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners4);
        Assertions.assertTrue(globalExceptionListeners4.isEmpty());

        jmxExceptionService.clearExceptionStoreFactoryByConfigurationTypeMap();
        jmxExceptionService.clearExceptionStores();

        exceptionAccessor.registerExceptionStoreFactoryByConfigurationType(
                TestExceptionStoreConfiguration.class, new TestExceptionStoreFactory());
        final Map<String, String> exceptionStoreFactoryByConfigurationTypeNameMap =
                jmxExceptionService.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap);
        Assertions.assertEquals(1, exceptionStoreFactoryByConfigurationTypeNameMap.size());
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap
                .containsKey(TestExceptionStoreConfiguration.class.getName()));

        jmxExceptionService.unregisterExceptionStoreFactoryByConfigurationType(
                TestExceptionStoreConfiguration.class.getName(),
                TestExceptionStoreFactory.class.getName());
        final Map<String, String> exceptionStoreFactoryByConfigurationTypeNameMap1 =
                jmxExceptionService.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap1);
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap1.isEmpty());

        exceptionAccessor.registerExceptionStoreFactoryByConfigurationType(
                TestExceptionStoreConfiguration.class, new TestExceptionStoreFactory());
        final Map<String, String> exceptionStoreFactoryByConfigurationTypeNameMap2 =
                jmxExceptionService.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap2);
        Assertions.assertEquals(1, exceptionStoreFactoryByConfigurationTypeNameMap2.size());
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap2
                .containsKey(TestExceptionStoreConfiguration.class.getName()));

        jmxExceptionService.clearExceptionStoreFactoryByConfigurationTypeMap();
        final Map<String, String> exceptionStoreFactoryByConfigurationTypeNameMap3 =
                jmxExceptionService.getExceptionStoreFactoryByConfigurationTypeNameMap();
        Assertions.assertNotNull(exceptionStoreFactoryByConfigurationTypeNameMap3);
        Assertions.assertTrue(exceptionStoreFactoryByConfigurationTypeNameMap3.isEmpty());

        exceptionAccessor.registerExceptionStore(new TestExceptionStore());
        final List<String> exceptionStores = jmxExceptionService.getExceptionStores();
        Assertions.assertNotNull(exceptionStores);
        Assertions.assertEquals(1, exceptionStores.size());

        jmxExceptionService.unregisterExceptionStore(TestExceptionStore.class.getName());
        final List<String> exceptionStores1 = jmxExceptionService.getExceptionStores();
        Assertions.assertNotNull(exceptionStores1);
        Assertions.assertTrue(exceptionStores1.isEmpty());

        exceptionAccessor.registerExceptionStore(new TestExceptionStore());
        final List<String> exceptionStores2 = jmxExceptionService.getExceptionStores();
        Assertions.assertNotNull(exceptionStores2);
        Assertions.assertEquals(1, exceptionStores2.size());

        jmxExceptionService.clearExceptionStores();
        final List<String> exceptionStores3 = jmxExceptionService.getExceptionStores();
        Assertions.assertNotNull(exceptionStores3);
        Assertions.assertTrue(exceptionStores3.isEmpty());
    }

    @Test
    @Order(3)
    void testAccessorMethods() throws Exception {
        final long timestamp1 = System.currentTimeMillis();
        final ExceptionMessage exceptionMessage1 = CoreTestFixtures.getExceptionMessage();
        final Map<String, CompletableFuture<Void>> writeExceptionMessageFutures =
                exceptionAccessor.asyncWriteExceptionMessage(exceptionMessage1);
        Assertions.assertNotNull(writeExceptionMessageFutures);
        writeExceptionMessageFutures.forEach((storeName, resultFuture) -> {
            try {
                final Void result = resultFuture.get();
                Assertions.assertNull(result);
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });



        final AtomicReference<Integer> removeIdRef = new AtomicReference<>();
        final Map<String, List<String>> readAllExceptionMessageDetails =
                jmxExceptionService.readAllExceptionMessageDetails();
        Assertions.assertNotNull(readAllExceptionMessageDetails);
        final AtomicReference<ExceptionMessageDetails> exceptionMessageDetails1Ref = new AtomicReference<>();
        readAllExceptionMessageDetails.forEach((storeName, jsonList) -> {
            Assertions.assertNotNull(jsonList);
            Assertions.assertEquals(1, jsonList.size());

            final String json = jsonList.get(0);
            final ExceptionMessageDetails exceptionMessageDetails = fromJson(json);
            removeIdRef.set(exceptionMessageDetails.getId());
            exceptionMessageDetails1Ref.set(exceptionMessageDetails);
            CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails);
        });



        final Map<String, String> readExceptionMessageDetailsById =
                jmxExceptionService.readExceptionMessageDetailsById(exceptionMessageDetails1Ref.get().getId());
        Assertions.assertNotNull(readExceptionMessageDetailsById);
        readExceptionMessageDetailsById.forEach((storeName, json) -> {
            final ExceptionMessageDetails exceptionMessageDetails = fromJson(json);
            Assertions.assertNotNull(exceptionMessageDetails);
            Assertions.assertEquals(exceptionMessageDetails1Ref.get(), exceptionMessageDetails);
        });



        final long timestamp2 = System.currentTimeMillis();
        final ExceptionMessage exceptionMessage2 = CoreTestFixtures.getExceptionMessage();
        exceptionMessage2.setThrowable(new NullPointerException());
        final Map<String, CompletableFuture<Void>> writeExceptionMessageFutures1 =
                exceptionAccessor.asyncWriteExceptionMessage(exceptionMessage2);
        Assertions.assertNotNull(writeExceptionMessageFutures1);
        writeExceptionMessageFutures1.forEach((storeName, resultFuture) -> {
            try {
                final Void result = resultFuture.get();
                Assertions.assertNull(result);
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });
        final long timestamp3 = System.currentTimeMillis();
        final Map<String, List<String>> readAllExceptionMessageDetailsByTimestamps1 =
                jmxExceptionService.readAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp2);
        Assertions.assertNotNull(readAllExceptionMessageDetailsByTimestamps1);
        Assertions.assertEquals(1, readAllExceptionMessageDetailsByTimestamps1.size());
        readAllExceptionMessageDetailsByTimestamps1.forEach((storeName, jsonList) -> {
            Assertions.assertNotNull(jsonList);
            Assertions.assertEquals(1, jsonList.size());
            final String json = jsonList.get(0);
            final ExceptionMessageDetails exceptionMessageDetails = fromJson(json);
            CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails);
        });

        final Map<String, List<String>> readAllExceptionMessageDetailsByTimestamps2 =
                jmxExceptionService.readAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp3);
        Assertions.assertNotNull(readAllExceptionMessageDetailsByTimestamps2);
        Assertions.assertEquals(1, readAllExceptionMessageDetailsByTimestamps2.size());
        final AtomicReference<ExceptionMessageDetails> exceptionMessageDetails4Ref = new AtomicReference<>();
        readAllExceptionMessageDetailsByTimestamps2.forEach((storeName, jsonList) -> {
            Assertions.assertNotNull(jsonList);
            Assertions.assertEquals(2, jsonList.size());

            final String json1 = jsonList.get(0);
            final ExceptionMessageDetails exceptionMessageDetails1 = fromJson(json1);
            CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails1);

            final String json2 = jsonList.get(1);
            final ExceptionMessageDetails exceptionMessageDetails2 = fromJson(json2);
            exceptionMessageDetails4Ref.set(exceptionMessageDetails2);
            CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage2, exceptionMessageDetails2);
        });



        final Map<String, List<String>> readUniqueExceptionTypeNamesFutures =
                jmxExceptionService.readUniqueExceptionTypeNames();
        Assertions.assertNotNull(readUniqueExceptionTypeNamesFutures);
        readUniqueExceptionTypeNamesFutures.forEach((storeName, typeNames) -> {
            Assertions.assertNotNull(typeNames);
            Assertions.assertEquals(2, typeNames.size());
            Assertions.assertTrue(typeNames.contains(AppbaseException.class.getName()));
            Assertions.assertTrue(typeNames.contains(NullPointerException.class.getName()));
        });



        final Map<String, List<String>> readExceptionMessageDetailsByTypeName1 =
                jmxExceptionService.readExceptionMessageDetailsByThrowableTypeName(AppbaseException.class.getName());
        Assertions.assertNotNull(readExceptionMessageDetailsByTypeName1);
        readExceptionMessageDetailsByTypeName1.forEach((storeName, jsonList) -> {
            Assertions.assertNotNull(jsonList);
            Assertions.assertEquals(1, jsonList.size());
        });



        final Map<String, List<String>> readExceptionMessageDetailsByTypeName2 =
                jmxExceptionService.readExceptionMessageDetailsByThrowableTypeName(
                        NullPointerException.class.getName());
        Assertions.assertNotNull(readExceptionMessageDetailsByTypeName2);
        readExceptionMessageDetailsByTypeName2.forEach((storeName, jsonList) -> {
            Assertions.assertNotNull(jsonList);
            Assertions.assertEquals(1, jsonList.size());

            final String json = jsonList.get(0);
            final ExceptionMessageDetails exceptionMessageDetails = fromJson(json);
            CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage2, exceptionMessageDetails);
        });


        final Map<String, String> readExceptionMessageDetailsById1 =
                jmxExceptionService.readExceptionMessageDetailsById(removeIdRef.get());
        Assertions.assertNotNull(readExceptionMessageDetailsById1);
        Assertions.assertFalse(readExceptionMessageDetailsById1.isEmpty());


        final Map<String, Boolean> removeExceptionMessageDetailsById =
                jmxExceptionService.removeExceptionMessageDetailsById(removeIdRef.get());
        Assertions.assertNotNull(removeExceptionMessageDetailsById);
        removeExceptionMessageDetailsById.forEach((storeName, result) -> {
            Assertions.assertTrue(result);
        });



        final Map<String, String> readExceptionMessageDetailsById2 =
                jmxExceptionService.readExceptionMessageDetailsById(exceptionMessageDetails4Ref.get().getId());
        Assertions.assertNotNull(readExceptionMessageDetailsById2);
        readExceptionMessageDetailsById2.forEach((storeName, json) -> {
            final ExceptionMessageDetails exceptionMessageDetails = fromJson(json);
            Assertions.assertNotNull(exceptionMessageDetails);
        });
        final Map<String, String> readExceptionMessageDetailsById3 =
                jmxExceptionService.readExceptionMessageDetailsById(exceptionMessageDetails4Ref.get().getId());
        Assertions.assertNotNull(readExceptionMessageDetailsById3);
        Assertions.assertFalse(readExceptionMessageDetailsById3.isEmpty());

        final Map<String, Boolean> removeExceptionMessageDetailsBefore =
                jmxExceptionService.removeExceptionMessageDetailsExclusiveBefore(System.currentTimeMillis());
        Assertions.assertNotNull(removeExceptionMessageDetailsBefore);
        removeExceptionMessageDetailsBefore.forEach((storeName, result) -> {
            Assertions.assertTrue(result);
        });



        final Map<String, String> noReadExceptionMessageDetailsById =
                jmxExceptionService.readExceptionMessageDetailsById(1111);
        Assertions.assertNotNull(noReadExceptionMessageDetailsById);
        Assertions.assertTrue(noReadExceptionMessageDetailsById.isEmpty());



        final Map<String, List<String>> noReadExceptionMessageDetailsByTypeName =
                jmxExceptionService
                        .readExceptionMessageDetailsByThrowableTypeName(ClassNotFoundException.class.getName());
        Assertions.assertNotNull(noReadExceptionMessageDetailsByTypeName);
        noReadExceptionMessageDetailsByTypeName.forEach((storeName, jsonList) -> {
            Assertions.assertNotNull(jsonList);
            Assertions.assertTrue(jsonList.isEmpty());
        });



        final long current = System.currentTimeMillis();
        final long after = current + 1000;
        final Map<String, List<String>> noReadAllExceptionMessageDetailsByTimestamps =
                jmxExceptionService.readAllExceptionMessageDetailsByTimestamps(current, after);
        Assertions.assertNotNull(noReadAllExceptionMessageDetailsByTimestamps);
        noReadAllExceptionMessageDetailsByTimestamps.forEach((storeName, jsonList) -> {
            Assertions.assertNotNull(jsonList);
            Assertions.assertTrue(jsonList.isEmpty());
        });



        Assertions.assertEquals(ServiceStatus.STARTED, jmxExceptionService.getStatus());
        Assertions.assertDoesNotThrow(() -> jmxExceptionService.reload());

        final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                DefaultExceptionServiceConfiguration.builder().build();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultExceptionServiceConfiguration.class)
                .writeValueAsString(defaultExceptionServiceConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxExceptionService.reload(yaml));
    }

    private ExceptionMessageDetails fromJson(final String json) {
        try {
            return JsonUtil
                    .getJsonMapper()
                    .readerFor(ExceptionMessageDetails.class)
                    .readValue(json);
        } catch (IOException ioe) {
            throw new AppbaseRuntimeException(ioe);
        }
    }

    static class TypeExceptionListener implements ExceptionListener {
        private final Logger logger;

        TypeExceptionListener() {
            logger = logService.getLogger(getClass());
        }

        @Override
        public void onException(final ExceptionMessage exceptionMessage) {
            logger.info("Handling exception message: {}", exceptionMessage);
        }
    }

    static class GlobalExceptionListener implements ExceptionListener {
        private final Logger logger;

        GlobalExceptionListener() {
            logger = logService.getLogger(getClass());
        }

        @Override
        public void onException(final ExceptionMessage exceptionMessage) {
            logger.info("Handling exception message: {}", exceptionMessage);
        }
    }

    private static class TestExceptionStore implements ExceptionStore {
        @Override
        public ExceptionMessageDetails readExceptionMessageDetailsById(final int id) {
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
