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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.accessor.ThrowableRecord;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import com.viveworks.appbase.core.util.lucene.LuceneUtil;
import org.apache.lucene.document.Document;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.Marker;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

/**
 * Default implementation of {@link LuceneLogIndex}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultLuceneLogIndex implements LuceneLogIndex {
    private final LuceneLogIndexConfiguration luceneLogIndexConfiguration;
    private final LuceneService luceneService;
    private final Counter errorCounter;
    private final Timer logIndexTimer;
    private final Timer turboLogIndexTimer;
    private final Timer logRecordIndexTimer;

    public DefaultLuceneLogIndex(
            final LogService logService,
            final MetricService metricService,
            final ValidationService validationService,
            final MediatorService mediatorService,
            final LuceneService luceneService,
            final LuceneLogIndexConfiguration luceneLogIndexConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(luceneLogIndexConfiguration, "luceneLogIndexConfiguration");

        this.luceneLogIndexConfiguration = luceneLogIndexConfiguration;
        this.luceneService = luceneService;
        this.errorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "error", "counter"));
        this.logIndexTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "logIndex", "timer"));
        this.turboLogIndexTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "turboLogIndex", "timer"));
        this.logRecordIndexTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "logRecordIndex", "timer"));
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexLog(final ILoggingEvent loggingEvent) {
        AppbaseUtil.notNull(loggingEvent, "loggingEvent");

        final Timer.Context context = logIndexTimer.time();
        try {
            final Document document = new Document();
            addTimestamp(loggingEvent.getTimeStamp(), document);
            addThreadName(loggingEvent.getThreadName(), document);
            addLoggerName(loggingEvent.getLoggerName(), document);

            final Level loggingEventLevel = loggingEvent.getLevel();
            final LogLevel logLevel = loggingEventLevel != null ? LogLevel.from(loggingEventLevel) : null;
            addLogLevel(logLevel, document);

            addFormattedMessage(loggingEvent.getFormattedMessage(), document);
            addMessage(loggingEvent.getMessage(), document);
            addThrowableProxy(loggingEvent.getThrowableProxy(), document);
            addMdcPropertyMap(loggingEvent.getMDCPropertyMap(), document);
            addCallerStack(loggingEvent.getCallerData(), document);
            addMarker(loggingEvent.getMarker(), document);
            luceneService.getIndexWriter().addDocument(document);
        } catch (Exception ex) {
            errorCounter.inc();
        } finally {
            context.stop();
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexTurboLog(final TurboLog turboLog) {
        AppbaseUtil.notNull(turboLog, "turboLog");

        final Timer.Context context = turboLogIndexTimer.time();
        try {
            final Document document = new Document();
            addTimestamp(turboLog.getTimestamp(), document);
            addLogger(turboLog.getLogger(), document);
            addLogLevel(turboLog.getLogLevel(), document);
            addFormattedMessage(turboLog.getFormattedMessage(), document);
            addThrowable(turboLog.getThrowable(), document);
            addMarker(turboLog.getMarker(), document);
            luceneService.getIndexWriter().addDocument(document);
        } catch (Exception ex) {
            errorCounter.inc();
        } finally {
            context.stop();
        }
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void indexLogRecord(final LogRecord logRecord) {
        AppbaseUtil.notNull(logRecord, "logRecord");

        final Timer.Context context = logRecordIndexTimer.time();
        try {
            final Document document = new Document();
            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getCallerStackFieldName(),
                    logRecord.getCallerData(),
                    document);
            addFormattedMessage(logRecord.getFormattedMessage(), document);
            addLoggerName(logRecord.getLoggerName(), document);
            addLogLevel(logRecord.getLogLevel(), document);
            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getMarkerFieldName(),
                    logRecord.getMarkerName(),
                    document);
            addMdcPropertyMap(logRecord.getMdcPropertyMap(), document);
            addMessage(logRecord.getMessage(), document);
            addThreadName(logRecord.getThreadName(), document);
            addTimestamp(logRecord.getTimestamp(), document);

            final ThrowableRecord throwableRecord = logRecord.getThrowableRecord();
            if (throwableRecord != null) {
                final String throwableRecordClassName = throwableRecord.getClassName();
                LuceneUtil.addTextField(
                        luceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        throwableRecordClassName,
                        document);
                LuceneUtil.addTextField(
                        luceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        throwableRecord.getMessage(),
                        document);
                LuceneUtil.addTextField(
                        luceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        throwableRecord.getStackTrace(),
                        document);

                final ThrowableRecord throwableRecordCause = throwableRecord.getCause();
                if (throwableRecordCause != null) {
                    LuceneUtil.addTextField(
                            luceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            throwableRecordCause.getClassName(),
                            document);
                    LuceneUtil.addTextField(
                            luceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            throwableRecordCause.getMessage(),
                            document);
                    LuceneUtil.addTextField(
                            luceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            throwableRecordCause.getStackTrace(),
                            document);
                }
            }
            luceneService.getIndexWriter().addDocument(document);
        } catch (Exception ex) {
            errorCounter.inc();
        } finally {
            context.stop();
        }
    }

    private void addTimestamp(final long timestamp, final Document document) {
        LuceneUtil.addLongPoint(
                luceneLogIndexConfiguration.getTimestampLongPointName(),
                luceneLogIndexConfiguration.getTimestampStoredFieldName(),
                timestamp,
                document);
    }

    private void addThreadName(final String threadName, final Document document) {
        LuceneUtil.addTextField(luceneLogIndexConfiguration.getThreadNameFieldName(), threadName, document);
    }

    private void addLogger(final Logger logger, final Document document) {
        if (logger != null) {
            final String loggerName = logger.getName();
            if (!AppbaseUtil.isBlank(loggerName)) {
                addLoggerName(loggerName, document);
            }
        }
    }

    private void addLoggerName(final String loggerName, final Document document) {
        LuceneUtil.addTextField(luceneLogIndexConfiguration.getLoggerNameFieldName(), loggerName, document);
    }

    private void addLogLevel(final LogLevel logLevel, final Document document) {
        if (logLevel != null) {
            final String logLevelName = logLevel.name();
            LuceneUtil.addTextField(luceneLogIndexConfiguration.getLogLevelFieldName(), logLevelName, document);
        }
    }

    private void addFormattedMessage(final String formattedMessage, final Document document) {
        LuceneUtil.addTextField(luceneLogIndexConfiguration.getFormattedMessageFieldName(), formattedMessage, document);
    }

    private void addMessage(final String message, final Document document) {
        LuceneUtil.addTextField(luceneLogIndexConfiguration.getMessageFieldName(), message, document);
    }

    private void addThrowableProxyStackTrace(final IThrowableProxy throwableProxy, final Document document) {
        if (luceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled()) {
            final StackTraceElementProxy[] stackTraceElementProxies = throwableProxy.getStackTraceElementProxyArray();

            if (stackTraceElementProxies != null) {
                final StackTraceElement[] stackTraceElements =
                        new StackTraceElement[stackTraceElementProxies.length];
                for (int i = 0; i < stackTraceElementProxies.length; ++i) {
                    final StackTraceElementProxy stackTraceElementProxy = stackTraceElementProxies[i];
                    if (stackTraceElementProxy != null) {
                        final StackTraceElement stackTraceElement = stackTraceElementProxy.getStackTraceElement();
                        if (stackTraceElement != null) {
                            stackTraceElements[i] = stackTraceElement;
                        }
                    }
                }
                addStackTraceElements(
                        stackTraceElements,
                        luceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        document);
            }
        }
    }

    private void addThrowableStackTrace(final Throwable throwable, final Document document) {
        if (luceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled()) {
            final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
            if (stackTraceElements != null) {
                addStackTraceElements(
                        stackTraceElements,
                        luceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        document);
            }
        }
    }

    private void addThrowableCause(final Throwable throwable, final Document document) {
        if (throwable != null) {
            final Throwable cause = throwable.getCause();
            if (cause != null) {
                LuceneUtil.addTextField(
                        luceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        cause.getClass().getName(),
                        document);

                LuceneUtil.addTextField(
                        luceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        cause.getMessage(),
                        document);

                if (luceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled()) {
                    final StackTraceElement[] stackTraceElements = cause.getStackTrace();
                    if (stackTraceElements != null) {
                        addStackTraceElements(
                                stackTraceElements,
                                luceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                                document);
                    }
                }
            }
        }
    }

    private void addThrowableProxyCause(final IThrowableProxy throwableProxy, final Document document) {
        final IThrowableProxy throwableProxyCause = throwableProxy.getCause();

        if (throwableProxyCause != null) {
            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                    throwableProxyCause.getClassName(),
                    document);

            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                    throwableProxyCause.getMessage(),
                    document);

            if (luceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled()) {
                final StackTraceElementProxy[] stackTraceElementProxies =
                        throwableProxyCause.getStackTraceElementProxyArray();

                if (stackTraceElementProxies != null) {
                    final StackTraceElement[] stackTraceElements =
                            new StackTraceElement[stackTraceElementProxies.length];
                    for (int i = 0; i < stackTraceElementProxies.length; ++i) {
                        final StackTraceElementProxy stackTraceElementProxy = stackTraceElementProxies[i];
                        if (stackTraceElementProxy != null) {
                            final StackTraceElement stackTraceElement = stackTraceElementProxy.getStackTraceElement();
                            if (stackTraceElement != null) {
                                stackTraceElements[i] = stackTraceElement;
                            }
                        }
                    }
                    addStackTraceElements(
                            stackTraceElements,
                            luceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            document);
                }
            }
        }
    }

    private void addThrowable(final Throwable throwable, final Document document) {
        if (throwable != null) {
            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                    throwable.getClass().getName(),
                    document);
            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getThrowableMessageFieldName(),
                    throwable.getMessage(),
                    document);
            addThrowableStackTrace(throwable, document);
            addThrowableCause(throwable, document);
        }
    }

    private void addThrowableProxy(final IThrowableProxy throwableProxy, final Document document) {
        if (throwableProxy != null) {
            final String throwableProxyClassName = throwableProxy.getClassName();
            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                    throwableProxyClassName,
                    document);

            final String throwableProxyMessage = throwableProxy.getMessage();
            LuceneUtil.addTextField(
                    luceneLogIndexConfiguration.getThrowableMessageFieldName(),
                    throwableProxyMessage,
                    document);

            addThrowableProxyStackTrace(throwableProxy, document);
            addThrowableProxyCause(throwableProxy, document);
        }
    }

    private void addMdcPropertyMap(final Map<String, String> mdcPropertyMap, final Document document) {
        if (luceneLogIndexConfiguration.isMdcIndexEnabled()) {
            if (mdcPropertyMap != null && !mdcPropertyMap.isEmpty()) {
                mdcPropertyMap.forEach((key, value) -> {
                    if (!AppbaseUtil.isBlank(key) && !AppbaseUtil.isBlank(value)) {
                        final String fieldName = luceneLogIndexConfiguration.getMdcFieldName(key);
                        LuceneUtil.addTextField(fieldName, value, document);
                    }
                });
            }
        }
    }

    private void addMarker(final Marker marker, final Document document) {
        if (luceneLogIndexConfiguration.isMarkerIndexEnabled()) {
            if (marker != null) {
                final String markerName = marker.getName();
                LuceneUtil.addTextField(
                        luceneLogIndexConfiguration.getMarkerFieldName(),
                        markerName,
                        document);
            }
        }
    }

    private void addCallerStack(final StackTraceElement[] callerStack, final Document document) {
        if (luceneLogIndexConfiguration.isCallerStackIndexEnabled()) {
            if (callerStack != null) {
                addStackTraceElements(
                        callerStack,
                        luceneLogIndexConfiguration.getCallerStackFieldName(),
                        document);
            }
        }
    }

    private void addStackTraceElements(final StackTraceElement[] stackTraceElements,
                                       final String fieldName,
                                       final Document document)
    {
        if (stackTraceElements != null) {
            final StringBuilder stackTraceElementBuilder = new StringBuilder();

            for (final StackTraceElement stackTraceElement : stackTraceElements) {
                stackTraceElementBuilder.append(stackTraceElement.toString());
            }

            if (stackTraceElementBuilder.length() > 0) {
                LuceneUtil.addTextField(fieldName, stackTraceElementBuilder.toString(), document);
            }
        }
    }

    @Override
    public LuceneService getLuceneService() {
        return luceneService;
    }

    @Override
    public LuceneLogIndexConfiguration getLuceneLogIndexConfiguration() {
        return luceneLogIndexConfiguration;
    }
}
