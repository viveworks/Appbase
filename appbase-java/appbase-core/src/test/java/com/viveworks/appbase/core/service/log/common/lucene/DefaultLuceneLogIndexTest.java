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

package com.viveworks.appbase.core.service.log.common.lucene;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.accessor.ThrowableRecord;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultLuceneLogIndexTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;
    private static DefaultLuceneLogIndexConfiguration defaultLuceneLogIndexConfiguration;
    private static DefaultLuceneLogIndex defaultLuceneLogIndex;

    @BeforeAll
    static void setUp() {
        defaultLuceneLogIndexConfiguration =
                DefaultLuceneLogIndexConfiguration
                        .builder()
                        .enabled(true)
                        .mdcIndexEnabled(true)
                        .throwableStackTraceIndexEnabled(true)
                        .throwableCauseStackTraceIndexEnabled(true)
                        .markerIndexEnabled(true)
                        .callerStackIndexEnabled(true)
                        .build();
        logService = CoreTestFixtures.getLogService();
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
        defaultLuceneLogIndex = CoreTestFixtures.getDefaultLuceneLogIndex(
                logService,
                metricService,
                validationService,
                mediatorService,
                luceneService,
                defaultLuceneLogIndexConfiguration);
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (luceneService != null) {
            luceneService.shutdown();
        }

        defaultLuceneLogIndex.getLuceneService().shutdown();
    }

    @Test
    void testIndex() throws Exception {
        Assertions.assertNotNull(defaultLuceneLogIndex.getLuceneLogIndexConfiguration());
        Assertions.assertEquals(
                defaultLuceneLogIndexConfiguration, defaultLuceneLogIndex.getLuceneLogIndexConfiguration());

        final LuceneService service = defaultLuceneLogIndex.getLuceneService();
        Assertions.assertNotNull(service);

        assertLoggingEvent(defaultLuceneLogIndex);
        assertTurboLog(defaultLuceneLogIndex);
        assertLogRecord(defaultLuceneLogIndex);
    }

    void assertLoggingEvent(final DefaultLuceneLogIndex defaultLuceneLogIndex) throws Exception {
        final LoggingEvent loggingEvent = new LoggingEvent();
        final long timestamp = System.currentTimeMillis();
        final String throwableMessage = "Throwable message 1";
        final String causeMessage = "Cause message 1";
        final String message = "Message";
        final String threadName = Thread.currentThread().getName();
        final Level level = Level.INFO;
        final Throwable throwable = new AppbaseException(
                throwableMessage, new AppbaseRuntimeException(causeMessage));
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final StringBuilder stackTraceBuilder = new StringBuilder();
        for (final StackTraceElement stackTraceElement : stackTrace) {
            stackTraceBuilder.append(stackTraceElement.toString());
        }
        final String stackTraceString = stackTraceBuilder.toString();
        final String mdcKey = "Key";
        final String mdcValue = "Value";
        final Map<String, String> mdcMap = Map.of(mdcKey, mdcValue);
        final String markerName = "MARKER";
        final String loggerName = getClass().getName();

        loggingEvent.setLevel(level);
        loggingEvent.setMessage(message);
        loggingEvent.setTimeStamp(timestamp);
        loggingEvent.setThreadName(threadName);
        loggingEvent.setThrowableProxy(new ThrowableProxy(throwable));
        loggingEvent.setCallerData(stackTrace);
        loggingEvent.setMDCPropertyMap(mdcMap);
        loggingEvent.addMarker(MarkerFactory.getMarker(markerName));
        loggingEvent.setLoggerName(loggerName);
        loggingEvent.setArgumentArray(new Object[] {"A", "B", "C"});

        defaultLuceneLogIndex.indexLog(loggingEvent);

        final LuceneService service = defaultLuceneLogIndex.getLuceneService();
        final LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                defaultLuceneLogIndex.getLuceneLogIndexConfiguration();
        final Query messageQuery = service
                .getQueryBuilder()
                .createBooleanQuery(luceneLogIndexConfiguration.getMessageFieldName(), message);

        assertFields(
                service,
                luceneLogIndexConfiguration,
                LogLevel.from(level).name(),
                message,
                timestamp,
                threadName,
                loggerName,
                markerName,
                throwableMessage,
                throwable.getClass().getName(),
                stackTraceString,
                mdcValue,
                causeMessage,
                throwable.getCause().getClass().getName(),
                message,
                1,
                messageQuery);
    }

    void assertTurboLog(final DefaultLuceneLogIndex defaultLuceneLogIndex) throws Exception {
        final Logger logger = LoggerFactory.getLogger(getClass());
        final long timestamp1 = System.currentTimeMillis();
        final String formattedMessage1 = "Formatted Message 1";
        final LogLevel logLevel1 = LogLevel.ERROR;
        final Logger logger1 = LoggerFactory.getLogger(getClass());
        final String loggerName1 = logger.getName();
        final String markerName1 = "MARKER11";
        final String throwableMessage1 = "Throwable message 3";
        final String causeMessage1 = "Cause message 3";
        final Throwable throwable1 = new AppbaseException(
                throwableMessage1, new AppbaseRuntimeException(causeMessage1));
        final String threadName1 = Thread.currentThread().getName();

        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(logLevel1)
                .logger(logger1)
                .formattedMessage(formattedMessage1)
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker(markerName1))
                .throwable(throwable1)
                .timestamp(timestamp1)
                .build();
        defaultLuceneLogIndex.indexTurboLog(turboLog1);

        final long timestamp = System.currentTimeMillis();
        final String formattedMessage = "Formatted Message";
        final LogLevel logLevel = LogLevel.ERROR;
        final String loggerName = logger.getName();
        final String markerName = "MARKER1";
        final String throwableMessage = "Throwable message 2";
        final String causeMessage = "Cause message 2";
        final Throwable throwable = new AppbaseException(
                throwableMessage, new AppbaseRuntimeException(causeMessage));
        final String threadName = Thread.currentThread().getName();

        final TurboLog turboLog = TurboLog
                .builder()
                .logLevel(logLevel)
                .logger(logger)
                .formattedMessage(formattedMessage)
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker(markerName))
                .throwable(throwable)
                .timestamp(timestamp)
                .build();
        defaultLuceneLogIndex.indexTurboLog(turboLog);

        final LuceneService service = defaultLuceneLogIndex.getLuceneService();
        final LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                defaultLuceneLogIndex.getLuceneLogIndexConfiguration();
        final Query formattedMessageQuery = service
                .getQueryBuilder()
                .createBooleanQuery(luceneLogIndexConfiguration.getFormattedMessageFieldName(), formattedMessage);

        assertFields(
                service,
                luceneLogIndexConfiguration,
                logLevel.name(),
                null,
                timestamp,
                threadName,
                loggerName,
                markerName,
                throwableMessage,
                throwable.getClass().getName(),
                null,
                null,
                causeMessage,
                throwable.getCause().getClass().getName(),
                formattedMessage,
                1,
                formattedMessageQuery);

        final Query formattedMessageQuery1 = service
                .getQueryBuilder()
                .createBooleanQuery(luceneLogIndexConfiguration.getFormattedMessageFieldName(), formattedMessage1);
        assertFields(
                service,
                luceneLogIndexConfiguration,
                logLevel1.name(),
                null,
                timestamp1,
                threadName1,
                loggerName1,
                markerName1,
                throwableMessage1,
                throwable1.getClass().getName(),
                null,
                null,
                causeMessage1,
                throwable1.getCause().getClass().getName(),
                formattedMessage1,
                1,
                formattedMessageQuery1);
    }

    void assertLogRecord(final DefaultLuceneLogIndex defaultLuceneLogIndex) throws Exception {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final StringBuilder callerDataBuilder = new StringBuilder("");
        for (final StackTraceElement stackTraceElement : stackTrace) {
            callerDataBuilder.append(stackTraceElement.toString());
        }
        final String callerData = callerDataBuilder.toString();
        final String throwableMessage = "Throwable message 3";
        final String throwableCauseMessage = "Cause Message 3";
        final Throwable throwable = new AppbaseException(
                throwableMessage, new AppbaseRuntimeException(throwableCauseMessage));
        final String throwableClassName = throwable.getClass().getName();

        final ThrowableRecord throwableRecord = ThrowableRecord
                .builder()
                .stackTrace(callerDataBuilder.toString())
                .message(throwableMessage)
                .className(throwableClassName)
                .cause(ThrowableRecord
                        .builder()
                        .message(throwableCauseMessage)
                        .className(throwable.getCause().getClass().getName())
                        .stackTrace(callerData)
                        .build())
                .build();

        final String mdcKey = "Key3";
        final String mdcValue = "Value3";
        final String markerName = "MARKER21";
        final String formattedMessage = "Formatted Message22: {}";
        final String message = "Formatted Message22: ARG";
        final LogLevel logLevel = LogLevel.INFO;
        final String threadName = Thread.currentThread().getName();
        final long timestamp = System.currentTimeMillis();
        final String loggerName = getClass().getName();
        final LogRecord logRecord = LogRecord
                .builder()
                .threadName(threadName)
                .mdcPropertyMap(Map.of(mdcKey, mdcValue))
                .callerData(callerData)
                .throwableRecord(throwableRecord)
                .markerName(markerName)
                .loggerName(loggerName)
                .formattedMessage(formattedMessage)
                .message(message)
                .logLevel(logLevel)
                .threadName(threadName)
                .timestamp(timestamp)
                .build();
        defaultLuceneLogIndex.indexLogRecord(logRecord);

        final LuceneService service = defaultLuceneLogIndex.getLuceneService();
        final LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                defaultLuceneLogIndex.getLuceneLogIndexConfiguration();
        final Query formattedMessageQuery1 = service
                .getQueryBuilder()
                .createBooleanQuery(luceneLogIndexConfiguration.getFormattedMessageFieldName(), formattedMessage);
        assertFields(
                service,
                luceneLogIndexConfiguration,
                logLevel.name(),
                message,
                timestamp,
                threadName,
                loggerName,
                markerName,
                throwableMessage,
                throwableClassName,
                callerData,
                mdcValue,
                throwableCauseMessage,
                throwable.getCause().getClass().getName(),
                formattedMessage,
                1,
                formattedMessageQuery1);
    }

    public static void assertFields(final LuceneService luceneService,
                                    final LuceneLogIndexConfiguration luceneLogIndexConfiguration,
                                    final String logLevelName,
                                    final String message,
                                    final long timestamp,
                                    final String threadName,
                                    final String loggerName,
                                    final String markerName,
                                    final String throwableMessage,
                                    final String throwableTypeName,
                                    final String callerStack,
                                    final String mdcValue,
                                    final String throwableCauseMessage,
                                    final String throwableCauseTypeName,
                                    final String formattedMessage,
                                    final int expectedDocumentIdsSize,
                                    final Query... queries) throws Exception
    {
        final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

        for (final Query query : queries) {
            booleanQueryBuilder.add(query, BooleanClause.Occur.MUST);
        }

        luceneService.synchronizeIndex();
        final DirectoryReader directoryReader = luceneService.getDirectoryReader();
        final int numDocs = directoryReader.numDocs();
        Assertions.assertTrue(numDocs > 0);

        final IndexSearcher indexSearcher = luceneService.getIndexSearcher();
        final TopDocs topDocs = indexSearcher.search(booleanQueryBuilder.build(), 1);
        final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Assertions.assertNotNull(scoreDocs);
        Assertions.assertEquals(expectedDocumentIdsSize, scoreDocs.length);
        Document document = null;
        for (ScoreDoc scoreDoc : scoreDocs) {
            document = directoryReader.document(scoreDoc.doc);
            if (document != null) {
                break;
            }
        }
        Assertions.assertNotNull(document);

        final List<IndexableField> fields = document.getFields();
        Assertions.assertNotNull(fields);
        Assertions.assertFalse(fields.isEmpty());

        int count = 0;
        for (final IndexableField indexableField : fields) {
            final String fieldName = indexableField.name();
            Assertions.assertNotNull(fieldName);

            final String stringValue = indexableField.stringValue();
            if (logLevelName != null && fieldName.equals(luceneLogIndexConfiguration.getLogLevelFieldName())) {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(logLevelName, stringValue);
            } else if (message != null && fieldName.equals(luceneLogIndexConfiguration.getMessageFieldName())) {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(message, stringValue);
            } else if (fieldName.equals(luceneLogIndexConfiguration.getTimestampLongPointName())) {
                ++count;
                Assertions.assertNotNull(indexableField.numericValue());
                Assertions.assertEquals(timestamp, indexableField.numericValue().longValue());
            } else if (fieldName.equals(luceneLogIndexConfiguration.getTimestampStoredFieldName())) {
                ++count;
                Assertions.assertNotNull(indexableField.numericValue());
                final long longValue = indexableField.numericValue().longValue();
                Assertions.assertEquals(timestamp, longValue);
            } else if (threadName != null && fieldName.equals(luceneLogIndexConfiguration.getThreadNameFieldName())) {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(threadName, stringValue);
            } else if (loggerName != null && fieldName.equals(luceneLogIndexConfiguration.getLoggerNameFieldName())) {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(loggerName, stringValue);
            } else if (markerName != null && fieldName.equals(luceneLogIndexConfiguration.getMarkerFieldName())) {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(markerName, stringValue);
            } else if (throwableMessage != null &&
                    fieldName.equals(luceneLogIndexConfiguration.getThrowableMessageFieldName()))
            {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(throwableMessage, stringValue);
            } else if (throwableTypeName != null &&
                    fieldName.equals(luceneLogIndexConfiguration.getThrowableTypeNameFieldName()))
            {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(throwableTypeName, stringValue);
            } else if (callerStack != null &&
                    fieldName.equals(luceneLogIndexConfiguration.getCallerStackFieldName()))
            {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(callerStack, stringValue);
            } else if (mdcValue != null && fieldName.startsWith(luceneLogIndexConfiguration.getMdcFieldNamePrefix())) {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(mdcValue, stringValue);
            } else if (throwableCauseMessage != null &&
                    fieldName.equals(luceneLogIndexConfiguration.getThrowableCauseMessageFieldName()))
            {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(throwableCauseMessage, stringValue);
            } else if (throwableCauseTypeName != null &&
                    fieldName.equals(luceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName()))
            {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(throwableCauseTypeName, stringValue);
            } else if (fieldName.equals(luceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName())) {
                ++count;
                Assertions.assertNotNull(stringValue);
            } else if (fieldName.equals(luceneLogIndexConfiguration.getThrowableStackTraceFieldName())) {
                ++count;
                Assertions.assertNotNull(stringValue);
            } else if (formattedMessage != null &&
                    fieldName.equals(luceneLogIndexConfiguration.getFormattedMessageFieldName()))
            {
                ++count;
                Assertions.assertNotNull(stringValue);
                Assertions.assertEquals(formattedMessage, stringValue);
            }
        }
        Assertions.assertEquals(fields.size(), count);
    }
}
