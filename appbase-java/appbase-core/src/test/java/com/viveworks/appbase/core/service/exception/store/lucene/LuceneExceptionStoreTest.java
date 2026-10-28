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

package com.viveworks.appbase.core.service.exception.store.lucene;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import com.viveworks.appbase.core.util.lucene.TimestampBasedIndexCleanerConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LuceneExceptionStoreTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static LuceneService luceneService;
    private static LuceneExceptionStore luceneExceptionStore;


    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);

        final StackTraceHashGenerator stackTraceHashGenerator = new DefaultStackTraceHashGenerator();
        final ExceptionMessageContextCodec exceptionMessageContextCodec = new DefaultExceptionMessageContextCodec();
        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration =
                LuceneExceptionStoreConfiguration
                .builder()
                .enabled(true)
                .build();
        final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration =
                DefaultLuceneServiceConfiguration
                        .builder()
                        .defaultIndexCleanerConfiguration(
                                TimestampBasedIndexCleanerConfiguration
                                        .builder()
                                        .enabled(false)
                                        .build())
                        .standardAnalyzerStopWords(List.of("zzaacc"))
                        .build();
        luceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                defaultLuceneServiceConfiguration,
                CoreTestFixtures.getReactiveReadBoundary());
        luceneExceptionStore = new LuceneExceptionStore(
                logService,
                metricService,
                mediatorService,
                validationService,
                luceneService,
                stackTraceHashGenerator,
                exceptionMessageContextCodec,
                luceneExceptionStoreConfiguration);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (luceneExceptionStore != null) {
            luceneExceptionStore.shutdown();
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
    void testExceptionStore() throws Exception {
        runTests();

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration =
                LuceneExceptionStoreConfiguration
                        .builder()
                        .enabled(true)
                        .build();
        luceneExceptionStore.reload(luceneExceptionStoreConfiguration);
        runTests();

        Assertions.assertDoesNotThrow(luceneExceptionStore::synchronize);
    }

    @Test
    private void runTests() throws Exception {
        final long timestamp1 = System.currentTimeMillis();
        final ExceptionMessage exceptionMessage1 = CoreTestFixtures.getExceptionMessage(timestamp1);
        luceneExceptionStore.writeExceptionMessage(exceptionMessage1);


        final Collection<ExceptionMessageDetails> allExceptionMessageDetails = luceneExceptionStore
                .asyncReadAllExceptionMessageDetails()
                .get();
        Assertions.assertNotNull(allExceptionMessageDetails);
        Assertions.assertEquals(1, allExceptionMessageDetails.size());
        final ExceptionMessageDetails exceptionMessageDetails1 = allExceptionMessageDetails.iterator().next();
        CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails1);

        final Collection<ExceptionMessageDetails> allExceptionMessageDetails1 = luceneExceptionStore
                .readAllExceptionMessageDetails();
        Assertions.assertNotNull(allExceptionMessageDetails1);
        Assertions.assertEquals(1, allExceptionMessageDetails1.size());
        final ExceptionMessageDetails exceptionMessageDetails11 = allExceptionMessageDetails1.iterator().next();
        CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails11);

        final ExceptionMessageDetails exceptionMessageDetails0 = luceneExceptionStore
                .readExceptionMessageDetailsById(exceptionMessageDetails1.getId());
        Assertions.assertNotNull(exceptionMessageDetails0);
        Assertions.assertEquals(exceptionMessageDetails1, exceptionMessageDetails0);

        final long timestamp2 = timestamp1 + 10_000;
        final ExceptionMessage exceptionMessage2 = CoreTestFixtures.getExceptionMessage(timestamp2);
        exceptionMessage2.setThrowable(new NullPointerException());
        final Void writeResult2 = luceneExceptionStore
                .asyncWriteExceptionMessage(exceptionMessage2)
                .get();
        Assertions.assertNull(writeResult2);

        final long timestamp3 = timestamp2 + 10_000;
        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTimestamps1 = luceneExceptionStore
                .asyncReadAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp2)
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTimestamps1);
        Assertions.assertEquals(1, exceptionMessageDetailsByTimestamps1.size());
        final ExceptionMessageDetails exceptionMessageDetails2 = exceptionMessageDetailsByTimestamps1.iterator().next();
        CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails2);

        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTimestamps11 = luceneExceptionStore
                .readAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp2);
        Assertions.assertNotNull(exceptionMessageDetailsByTimestamps11);
        Assertions.assertEquals(1, exceptionMessageDetailsByTimestamps11.size());
        final ExceptionMessageDetails exceptionMessageDetails12 = exceptionMessageDetailsByTimestamps11.iterator().next();
        CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails12);

        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTimestamps2 = luceneExceptionStore
                .asyncReadAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp3)
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTimestamps2);
        Assertions.assertEquals(2, exceptionMessageDetailsByTimestamps2.size());
        final Iterator<ExceptionMessageDetails> iterator = exceptionMessageDetailsByTimestamps2.iterator();
        final ExceptionMessageDetails exceptionMessageDetails3 = iterator.next();
        CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage1, exceptionMessageDetails3);
        final ExceptionMessageDetails exceptionMessageDetails4 = iterator.next();
        CoreTestFixtures.checkExceptionMessageDetails(exceptionMessage2, exceptionMessageDetails4);


        final Collection<String> typeNames = luceneExceptionStore
                .asyncReadUniqueThrowableTypeNames()
                .get();
        Assertions.assertNotNull(typeNames);
        Assertions.assertEquals(2, typeNames.size());
        Assertions.assertTrue(typeNames.contains(AppbaseException.class.getName()));
        Assertions.assertTrue(typeNames.contains(NullPointerException.class.getName()));

        final Collection<String> typeNames1 = luceneExceptionStore
                .readUniqueThrowableTypeNames();
        Assertions.assertNotNull(typeNames1);
        Assertions.assertEquals(2, typeNames1.size());
        Assertions.assertTrue(typeNames1.contains(AppbaseException.class.getName()));
        Assertions.assertTrue(typeNames1.contains(NullPointerException.class.getName()));

        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTypeName1 = luceneExceptionStore
                .asyncReadExceptionMessageDetailsByThrowableTypeName(AppbaseException.class.getName())
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTypeName1);
        Assertions.assertEquals(1, exceptionMessageDetailsByTypeName1.size());
        CoreTestFixtures.checkExceptionMessageDetails(
                exceptionMessage1, exceptionMessageDetailsByTypeName1.iterator().next());
        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTypeName11 = luceneExceptionStore
                .readExceptionMessageDetailsByThrowableTypeName(AppbaseException.class.getName());
        Assertions.assertNotNull(exceptionMessageDetailsByTypeName11);
        Assertions.assertEquals(1, exceptionMessageDetailsByTypeName11.size());
        CoreTestFixtures.checkExceptionMessageDetails(
                exceptionMessage1,
                exceptionMessageDetailsByTypeName11.iterator().next());

        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTypeName2 = luceneExceptionStore
                .asyncReadExceptionMessageDetailsByThrowableTypeName(NullPointerException.class.getName())
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTypeName2);
        Assertions.assertEquals(1, exceptionMessageDetailsByTypeName2.size());
        CoreTestFixtures.checkExceptionMessageDetails(
                exceptionMessage2, exceptionMessageDetailsByTypeName2.iterator().next());

        final int removeId = exceptionMessageDetails1.getId();
        final ExceptionMessageDetails exceptionMessageDetails6 = luceneExceptionStore
                .readExceptionMessageDetailsById(removeId);
        Assertions.assertNotNull(exceptionMessageDetails6);
        final Void result = luceneExceptionStore
                .asyncRemoveExceptionMessageDetailsById(removeId)
                .get();
        Assertions.assertNull(result);
        luceneExceptionStore.removeExceptionMessageDetailsById(removeId);

        final ExceptionMessageDetails exceptionMessageDetails7 = luceneExceptionStore
                .readExceptionMessageDetailsById(removeId);
        Assertions.assertNull(exceptionMessageDetails7);


        final long timestamp4 = timestamp3 + 10_0000;
        final ExceptionMessageDetails exceptionMessageDetails8 = luceneExceptionStore
                .asyncReadExceptionMessageDetailsById(exceptionMessageDetails4.getId())
                .get();
        Assertions.assertNotNull(exceptionMessageDetails8);
        final Void removeExceptionMessageDetailsBefore = luceneExceptionStore
                .asyncRemoveExceptionMessageDetailsExclusiveBefore(timestamp4)
                .get();
        Assertions.assertNull(removeExceptionMessageDetailsBefore);
        luceneExceptionStore.removeExceptionMessageDetailsExclusiveBefore(timestamp4);

        final ExceptionMessageDetails exceptionMessageDetails9 = luceneExceptionStore
                .asyncReadExceptionMessageDetailsById(exceptionMessageDetails4.getId())
                .get();
        Assertions.assertNull(exceptionMessageDetails9);


        final ExceptionMessageDetails noExceptionMessageDetails1 = luceneExceptionStore
                .asyncReadExceptionMessageDetailsById(1111)
                .get();
        Assertions.assertNull(noExceptionMessageDetails1);


        final long current = timestamp4 + 10_0000;
        final long after = current + 1000;
        final Collection<ExceptionMessageDetails> noExceptionMessageDetails3 = luceneExceptionStore
                .asyncReadAllExceptionMessageDetailsByTimestamps(current, after)
                .get();
        Assertions.assertNotNull(noExceptionMessageDetails3);
        Assertions.assertTrue(noExceptionMessageDetails3.isEmpty());
    }
}
