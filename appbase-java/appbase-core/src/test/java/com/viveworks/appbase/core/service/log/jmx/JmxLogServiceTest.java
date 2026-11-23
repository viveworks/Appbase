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

package com.viveworks.appbase.core.service.log.jmx;

import ch.qos.logback.classic.LoggerContext;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration;
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.listener.lucene.LuceneLogAndTurboLogEventFilterListenerConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreException;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.LogStoreFactoryException;
import com.viveworks.appbase.core.service.log.store.criteria.LogLevelCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.ExactTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.RangeTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.SetTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreFactory;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxLogServiceTest {
    private static DefaultLogServiceConfiguration defaultLogServiceConfiguration;
    private static DefaultLogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;
    private static LuceneLogIndex luceneLogIndex;
    private static LuceneLogStore luceneLogStore;
    private static LogAccessor logAccessor;
    private static ConfigurationService configurationService;
    private static DefaultJmxService jmxService;
    private static JmxLogService jmxLogService;

    @BeforeEach
    void setUp() throws Exception {
        final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration =
                DefaultLogAccessorConfiguration.builder().enabled(true).build();
        defaultLogServiceConfiguration = DefaultLogServiceConfiguration
                .builder()
                .defaultLogAccessorConfiguration(defaultLogAccessorConfiguration)
                .duplicateLogTurboFilterHandlerConfiguration(DuplicateLogTurboFilterHandlerConfiguration
                        .builder()
                        .enabled(true)
                        .build())
                .luceneLogAndTurboLogEventFilterListenerConfiguration(
                        LuceneLogAndTurboLogEventFilterListenerConfiguration
                                .builder()
                                .enabled(true)
                                .indexLogEvent(true)
                                .indexTurboLog(true)
                                .build())
                // R3 (internal work item 1073): enabling a Lucene component opts it in explicitly (§10 defaults off).
                .luceneLogIndexConfiguration(
                        DefaultLuceneLogIndexConfiguration.builder().enabled(true).build())
                .build();
        logService = new DefaultLogService(
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                defaultLogServiceConfiguration);
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        luceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                DefaultLuceneServiceConfiguration.builder().build(),
                CoreTestFixtures.getReactiveReadBoundary());
        logService.setMediatorService(mediatorService);
        logService.setMetricService(metricService);
        logService.setLuceneService(luceneService);
        logService.setValidationService(validationService);
        logService.setLuceneService(luceneService);
        logService.initFiltersAndAppenders((LoggerContext) LoggerFactory.getILoggerFactory());
        logAccessor = logService.getLogAccessor();

        luceneLogIndex = CoreTestFixtures.getDefaultLuceneLogIndex(
                logService,
                metricService,
                validationService,
                mediatorService,
                luceneService,
                DefaultLuceneLogIndexConfiguration.builder().build());
        final LuceneLogStoreFactory luceneLogStoreFactory = new LuceneLogStoreFactory(
                logService,
                metricService,
                luceneLogIndex,
                mediatorService.getMediator().getSharedExecutorService());
        logAccessor.registerLogStoreFactoryByConfigurationType(
                LuceneLogStoreConfiguration.class,
                luceneLogStoreFactory);
        luceneLogStore = new LuceneLogStore(
                logService,
                metricService,
                luceneLogIndex,
                mediatorService.getMediator().getSharedExecutorService(),
                LuceneLogStoreConfiguration.builder().enabled(true).build());
        logAccessor.registerLogStore(luceneLogStore);

        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);

        configurationService.putTypedConfiguration(
                DefaultLogServiceConfiguration.class, defaultLogServiceConfiguration);
        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxLogService.class);
        jmxLogService = new JmxLogService(
                logService,
                logService,
                DefaultLogServiceConfiguration.class,
                configurationService);
    }

    @AfterEach
    void tearDown() {
        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (jmxLogService != null) {
            jmxLogService.shutdown();
        }

        if (jmxService != null) {
            jmxService.shutdown();
        }

        if (logAccessor != null) {
            logAccessor.shutdown();
        }

        if (luceneLogStore != null) {
            luceneLogStore.shutdown();
        }

        if (luceneLogIndex != null) {
            luceneLogIndex.getLuceneService().shutdown();
        }

        if (luceneService != null) {
            luceneService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
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
        final Class<? extends JmxLogService> jmxLogServiceType = jmxLogService.getClass();
        if (jmxService.isMXBeanRegistered(jmxLogServiceType)) {
            jmxService.unregisterMXBean(jmxLogServiceType);
        }
        jmxService.registerMXBean(jmxLogService);

        final JmxLogService mxBean = jmxService.getMXBean(JmxLogService.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(JmxLogService.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxLogService.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    @Order(2)
    void testLogAccessorMethods() throws Exception {
        final LogRecord logRecord1 = CoreTestFixtures.getLogRecord1();
        final Map<String, CompletableFuture<Void>> writeLogRecordMap1 = logAccessor.asyncWriteLogRecord(logRecord1);
        Assertions.assertNotNull(writeLogRecordMap1);
        getFuture(writeLogRecordMap1);


        final LogRecord logRecord2 = CoreTestFixtures.getLogRecord2();
        final Map<String, CompletableFuture<Void>> writeLogRecordMap2 = logAccessor.asyncWriteLogRecord(logRecord2);
        Assertions.assertNotNull(writeLogRecordMap2);
        getFuture(writeLogRecordMap2);


        logAccessor.synchronizeLogStores();


        runReadLogRecordsByCriteriaTests("Formatted Message1: ARG");
        runReadLogRecordsByCriteriaTests(logRecord1.getTimestamp());
        runReadLogRecordsByCriteriaTests("Formatted Message2: ARG");
        runReadLogRecordsByCriteriaTests(logRecord2.getTimestamp());
        runReadLogRecordsByCriteriaTests("Formatted Message1: {}");
        runReadLogRecordsByCriteriaTests("Formatted Message2: {}");


        final Map<String, List<String>> readLogRecordsAt1 =
                jmxLogService.readLogRecordsAt(logRecord1.getTimestamp(), 10);
        Assertions.assertNotNull(readLogRecordsAt1);
        Assertions.assertNotNull(readLogRecordsAt1);
        Assertions.assertEquals(1, readLogRecordsAt1.size());
        final List<String> readLogRecordsList1 = readLogRecordsAt1.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsList1);
        Assertions.assertEquals(1, readLogRecordsList1.size());
        final String json1 = readLogRecordsList1.get(0);
        final LogRecord readLogRecord1 = fromJson(json1);
        Assertions.assertEquals(logRecord1.getTimestamp(), readLogRecord1.getTimestamp());


        final Map<String, List<String>> readLogRecordsBetween1Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween1Map);
        Assertions.assertEquals(1, readLogRecordsBetween1Map.size());
        final List<String> readLogRecordsBetween1 =
                readLogRecordsBetween1Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween1);
        Assertions.assertEquals(2, readLogRecordsBetween1.size());


        final Map<String, Boolean> removeLogRecordsAt1Map =
                jmxLogService.removeLogRecordsAt(logRecord1.getTimestamp());
        Assertions.assertNotNull(removeLogRecordsAt1Map);
        final Boolean removeLogRecordsAt1Result = removeLogRecordsAt1Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(removeLogRecordsAt1Result);
        Assertions.assertTrue(removeLogRecordsAt1Result);


        jmxLogService.synchronizeLogStores();


        final Map<String, List<String>> readLogRecordsBetween2Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween2Map);
        Assertions.assertEquals(1, readLogRecordsBetween2Map.size());
        final List<String> readLogRecordsBetween2 =
                readLogRecordsBetween2Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween2);
        Assertions.assertEquals(1, readLogRecordsBetween2.size());


        final Map<String, List<String>> readLogRecordsBetween3Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, logRecord1.getTimestamp() - 100, 10);
        Assertions.assertNotNull(readLogRecordsBetween3Map);
        Assertions.assertEquals(1, readLogRecordsBetween3Map.size());
        final List<String> readLogRecordsBetween3 =
                readLogRecordsBetween3Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween3);
        Assertions.assertTrue(readLogRecordsBetween3.isEmpty());


        final LogRecord logRecord3 = CoreTestFixtures.getLogRecord3();
        final Map<String, CompletableFuture<Void>> writeLogRecordMap3 = logAccessor.asyncWriteLogRecord(logRecord3);
        Assertions.assertNotNull(writeLogRecordMap3);
        getFuture(writeLogRecordMap3);

        final LogRecord logRecord4 = CoreTestFixtures.getLogRecord4();
        final Map<String, CompletableFuture<Void>> writeLogRecordMap4 = logAccessor.asyncWriteLogRecord(logRecord4);
        Assertions.assertNotNull(writeLogRecordMap4);
        getFuture(writeLogRecordMap4);


        jmxLogService.synchronizeLogStores();


        final Map<String, List<String>> readLogRecordsBetween4Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween4Map);
        final List<String> readLogRecordsBetween4 =
                readLogRecordsBetween4Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween4);
        Assertions.assertEquals(3, readLogRecordsBetween4.size());


        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .message("Another")
                .build();
        final String json5 = toJson(logRecordCriteria);
        final Map<String, Boolean> removeLogRecordsByCriteria1Map = jmxLogService.removeLogRecordsByCriteria(json5);
        Assertions.assertNotNull(removeLogRecordsByCriteria1Map);
        final Boolean removeLogRecordsByCriteria1Result =
                removeLogRecordsByCriteria1Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(removeLogRecordsByCriteria1Result);
        Assertions.assertTrue(removeLogRecordsByCriteria1Result);


        jmxLogService.synchronizeLogStores();


        final Map<String, List<String>> readLogRecordsBetween5Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween5Map);
        final List<String> readLogRecordsBetween5 =
                readLogRecordsBetween5Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween5);
        Assertions.assertEquals(2, readLogRecordsBetween5.size());


        final LogRecordCriteria logRecordCriteria1 = LogRecordCriteria
                .builder()
                .timestampCriteria(new SetTimestampCriteria(Set.of(logRecord2.getTimestamp())))
                .build();
        final String json6 = toJson(logRecordCriteria1);
        final Map<String, Boolean> removeLogRecordsByCriteria2Map = jmxLogService.removeLogRecordsByCriteria(json6);
        Assertions.assertNotNull(removeLogRecordsByCriteria2Map);
        final Boolean removeLogRecordsByCriteria2Result =
                removeLogRecordsByCriteria2Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(removeLogRecordsByCriteria2Result);
        Assertions.assertTrue(removeLogRecordsByCriteria2Result);


        jmxLogService.synchronizeLogStores();


        final Map<String, List<String>> readLogRecordsBetween6Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween6Map);
        final List<String> readLogRecordsBetween6 =
                readLogRecordsBetween6Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween6);
        Assertions.assertEquals(1, readLogRecordsBetween6.size());


        final LogRecordCriteria logRecordCriteria2 = LogRecordCriteria
                .builder()
                .timestampCriteria(new RangeTimestampCriteria(logRecord1.getTimestamp(), Long.MAX_VALUE))
                .build();
        final String json7 = toJson(logRecordCriteria2);
        final Map<String, Boolean> removeLogRecordsByCriteria3Map = jmxLogService.removeLogRecordsByCriteria(json7);
        Assertions.assertNotNull(removeLogRecordsByCriteria3Map);
        final Boolean removeLogRecordsByCriteria3Result =
                removeLogRecordsByCriteria3Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(removeLogRecordsByCriteria3Result);
        Assertions.assertTrue(removeLogRecordsByCriteria3Result);


        jmxLogService.synchronizeLogStores();


        final Map<String, List<String>> readLogRecordsBetween7Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween7Map);
        final List<String> readLogRecordsBetween7 =
                readLogRecordsBetween7Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween7);
        Assertions.assertTrue(readLogRecordsBetween7.isEmpty());


        final long timestamp5 = System.currentTimeMillis();
        final LogRecord logRecord5 = CoreTestFixtures.getLogRecord(
                "Msg fmt 5: {}",
                "Msg fmt 5: ARG",
                "Throwable message5",
                "Cause Message5",
                "Key5",
                "Value5",
                "MARKER5",
                timestamp5,
                CoreTestFixtures.class.getName(),
                LogLevel.TRACE);
        final Map<String, CompletableFuture<Void>> writeLogRecordMap5 = logAccessor.asyncWriteLogRecord(logRecord5);
        Assertions.assertNotNull(writeLogRecordMap5);
        getFuture(writeLogRecordMap5);

        final long timestamp6 = System.currentTimeMillis();
        final LogRecord logRecord6 = CoreTestFixtures.getLogRecord(
                "Msg fmt 6: {}",
                "Msg fmt 6: ARG",
                "Throwable message6",
                "Cause Message6",
                "Key6",
                "Value6",
                "MARKER6",
                timestamp6,
                CoreTestFixtures.class.getName(),
                LogLevel.ERROR);
        final Map<String, CompletableFuture<Void>> writeLogRecordMap6 = logAccessor.asyncWriteLogRecord(logRecord6);
        Assertions.assertNotNull(writeLogRecordMap6);
        getFuture(writeLogRecordMap6);


        jmxLogService.synchronizeLogStores();


        runReadLogRecordsByCriteriaTests(LogLevel.OFF, LogLevelCriteria.Match.GREATER_OR_EQUAL, 0);
        runReadLogRecordsByCriteriaTests(LogLevel.ERROR, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.WARN, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.INFO, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.DEBUG, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.TRACE, LogLevelCriteria.Match.GREATER_OR_EQUAL, 2);
        runReadLogRecordsByCriteriaTests(LogLevel.TRACE, LogLevelCriteria.Match.EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.ALL, LogLevelCriteria.Match.GREATER_OR_EQUAL, 2);


        final Map<String, Boolean> removeLogRecordsBetween =
                jmxLogService.removeLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE);
        Assertions.assertNotNull(removeLogRecordsBetween);


        jmxLogService.synchronizeLogStores();


        final Map<String, List<String>> readLogRecordsBetween8Map =
                jmxLogService.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween8Map);
        final List<String> readLogRecordsBetween8 =
                readLogRecordsBetween8Map.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsBetween8);
        Assertions.assertTrue(readLogRecordsBetween8.isEmpty());
    }

    @Test
    @Order(3)
    void testCollections() throws Exception {
        jmxLogService.clearLogStoreFactoryByConfigurationTypeMap();
        jmxLogService.clearLogStores();

        logAccessor.registerLogStoreFactoryByConfigurationType(
                TestLogStoreConfiguration.class, new TestLogStoreFactory());
        final Map<String, String> logStoreFactoryByConfigurationTypeNameMap1 =
                jmxLogService.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap1);
        Assertions.assertEquals(1, logStoreFactoryByConfigurationTypeNameMap1.size());
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap1
                .containsKey(TestLogStoreConfiguration.class.getName()));

        jmxLogService.unregisterLogStoreFactoryByConfigurationType(
                TestLogStoreConfiguration.class.getName(), TestLogStoreFactory.class.getName());
        final Map<String, String> logStoreFactoryByConfigurationTypeNameMap2 =
                jmxLogService.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap2);
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap2.isEmpty());

        logAccessor.registerLogStoreFactoryByConfigurationType(
                TestLogStoreConfiguration.class, new TestLogStoreFactory());
        final Map<String, String> logStoreFactoryByConfigurationTypeNameMap3 =
                jmxLogService.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap3);
        Assertions.assertEquals(1, logStoreFactoryByConfigurationTypeNameMap3.size());
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap3
                .containsKey(TestLogStoreConfiguration.class.getName()));

        jmxLogService.clearLogStoreFactoryByConfigurationTypeMap();
        final Map<String, String> logStoreFactoryByConfigurationTypeNameMap4 =
                jmxLogService.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap4);
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap4.isEmpty());


        logAccessor.registerLogStore(new TestLogStore());
        final List<String> logStores = jmxLogService.getLogStores();
        Assertions.assertNotNull(logStores);
        Assertions.assertEquals(1, logStores.size());

        jmxLogService.unregisterLogStore(TestLogStore.class.getName());
        final List<String> logStores1 = jmxLogService.getLogStores();
        Assertions.assertNotNull(logStores1);
        Assertions.assertTrue(logStores1.isEmpty());

        logAccessor.registerLogStore(new TestLogStore());
        final List<String> logStores2 = jmxLogService.getLogStores();
        Assertions.assertNotNull(logStores2);
        Assertions.assertEquals(1, logStores2.size());

        jmxLogService.clearLogStores();
        final List<String> logStores3 = jmxLogService.getLogStores();
        Assertions.assertNotNull(logStores3);
        Assertions.assertTrue(logStores3.isEmpty());
    }

    @Test
    @Order(4)
    void testLifecycleMethods() throws Exception {
        Assertions.assertEquals(ServiceStatus.STARTED, jmxLogService.getStatus());
        Assertions.assertDoesNotThrow(() -> jmxLogService.reload());

        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultLogServiceConfiguration.class)
                .writeValueAsString(defaultLogServiceConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxLogService.reload(yaml));
        Assertions.assertDoesNotThrow(() -> jmxLogService.synchronizeLogStores());
    }

    private String toJson(final LogRecordCriteria logRecordCriteria) throws Exception {
        return JsonUtil
                .getJsonMapper()
                .writerFor(LogRecordCriteria.class)
                .writeValueAsString(logRecordCriteria);
    }

    private LogRecord fromJson(final String json) throws Exception {
        return JsonUtil
                .getJsonMapper()
                .readerFor(LogRecord.class)
                .readValue(json);
    }

    private void runReadLogRecordsByCriteriaTests(final LogLevel logLevel,
                                                  final LogLevelCriteria.Match match,
                                                  final int size) throws Exception
    {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .logLevelCriteria(new LogLevelCriteria(logLevel, match))
                .build();
        final String json = toJson(logRecordCriteria);
        final Map<String, List<String>> logRecordsByCriteria = jmxLogService.readLogRecordsByCriteria(json, 10);
        Assertions.assertNotNull(logRecordsByCriteria);
        Assertions.assertEquals(1, logRecordsByCriteria.size());

        final List<String> logRecords = logRecordsByCriteria.values().stream().findFirst().orElse(null);
        Assertions.assertNotNull(logRecords);
        Assertions.assertNotNull(logRecords);
        Assertions.assertEquals(size, logRecords.size());
    }

    private void runReadLogRecordsByCriteriaTests(final long timestamp) throws Exception {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .timestampCriteria(new ExactTimestampCriteria(timestamp))
                .build();
        final String logRecordCriteriaJson = JsonUtil
                .getJsonMapper()
                .writerFor(LogRecordCriteria.class)
                .writeValueAsString(logRecordCriteria);
        final Map<String, List<String>> resultMap =
                jmxLogService.readLogRecordsByCriteria(logRecordCriteriaJson, 10);
        Assertions.assertNotNull(resultMap);
        Assertions.assertFalse(resultMap.isEmpty());
        Assertions.assertEquals(1, resultMap.size());
    }

    private void runReadLogRecordsByCriteriaTests(final String message) throws Exception {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .message(message)
                .build();
        final String logRecordCriteriaJson = JsonUtil
                .getJsonMapper()
                .writerFor(LogRecordCriteria.class)
                .writeValueAsString(logRecordCriteria);
        final Map<String, List<String>> resultMap =
                jmxLogService.readLogRecordsByCriteria(logRecordCriteriaJson, 10);
        Assertions.assertNotNull(resultMap);
        Assertions.assertFalse(resultMap.isEmpty());
    }

    private void getFuture(final Map<String, CompletableFuture<Void>> completableFutureMap) throws Exception {
        final Set<Map.Entry<String, CompletableFuture<Void>>> entries = completableFutureMap.entrySet();
        for (final Map.Entry<String, CompletableFuture<Void>> entry : entries) {
            final CompletableFuture<Void> future = entry.getValue();
            future.get();
        }
    }

    private static class TestLogStore implements LogStore {
        @Override
        public Collection<LogRecord> readLogRecordsBetween(
                final long inclusiveStartTimestamp,
                final long exclusiveEndTimestamp,
                final int pageSize) throws LogStoreException
        {
            return null;
        }

        @Override
        public Collection<LogRecord> readLogRecordsAt(
                final long timestamp,
                final int pageSize) throws LogStoreException
        {
            return null;
        }

        @Override
        public Collection<LogRecord> readLogRecordsByCriteria(
                final LogRecordCriteria logRecordCriteria,
                final int pageSize) throws LogStoreException
        {
            return null;
        }

        @Override
        public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsBetween(
                final long inclusiveStartTimestamp,
                final long exclusiveEndTimestamp,
                final int pageSize)
        {
            return null;
        }

        @Override
        public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsAt(final long timestamp, final int pageSize) {
            return null;
        }

        @Override
        public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsByCriteria(
                final LogRecordCriteria logRecordCriteria, final int pageSize)
        {
            return null;
        }

        @Override
        public void writeLogRecord(final LogRecord logRecord) {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncWriteLogRecord(final LogRecord logRecord) {
            return null;
        }

        @Override
        public void removeLogRecordsBetween(
                final long inclusiveStartTimestamp,
                final long exclusiveEndTimestamp)
        {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncRemoveLogRecordsBetween(
                final long inclusiveStartTimestamp, final long exclusiveEndTimestamp)
        {
            return null;
        }

        @Override
        public void removeLogRecordsAt(final long timestamp) throws LogStoreException {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncRemoveLogRecordsAt(final long timestamp) {
            return null;
        }

        @Override
        public void removeLogRecordsByCriteria(final LogRecordCriteria logRecordCriteria) {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncRemoveLogRecordsByCriteria(final LogRecordCriteria logRecordCriteria) {
            return null;
        }

        @Override
        public void synchronize() throws LogStoreException {

        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public void shutdown() throws AppbaseRuntimeException {

        }
    }

    private static class TestLogStoreConfiguration implements LogStoreConfiguration {
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends LogStore> Class<S> getLogStoreType() {
            return (Class<S>) TestLogStore.class;
        }
    }

    private static class TestLogStoreFactory implements LogStoreFactory<TestLogStore, TestLogStoreConfiguration> {
        @Override
        public TestLogStore newLogStore(
                final TestLogStoreConfiguration logStoreConfiguration) throws LogStoreFactoryException
        {
            return new TestLogStore();
        }
    }
}
