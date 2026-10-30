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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.viveworks.appbase.core.service.log.filter.burst.LogEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link DuplicateLogHandler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultDuplicateLogHandler implements DuplicateLogHandler {
    private final Marker marker;
    private final int formattedMessageLogLevel;
    private final int throwableLogLevel;
    private final String countTextPrefix;
    private final String countTextSuffix;
    private final String throwableTypeNameAndMessageSeparator;
    private final boolean logParamsAndThrowable;

    public DefaultDuplicateLogHandler(
            final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration)
    {
        AppbaseUtil.notNull(defaultDuplicateLogHandlerConfiguration, "defaultDuplicateLogHandlerConfiguration");
        AppbaseUtil.notNull(
                defaultDuplicateLogHandlerConfiguration.getFormattedMessageLogLevel(),
                "defaultDuplicateLogHandlerConfiguration.getFormattedMessageLogLevel()");
        AppbaseUtil.notNull(
                defaultDuplicateLogHandlerConfiguration.getThrowableLogLevel(),
                "defaultDuplicateLogHandlerConfiguration.getThrowableLogLevel()");

        final String markerName = defaultDuplicateLogHandlerConfiguration.getMarkerName();
        if (!markerName.isEmpty()) {
            marker = MarkerFactory.getMarker(markerName);
        } else {
            marker = null;
        }

        this.formattedMessageLogLevel = defaultDuplicateLogHandlerConfiguration
                .getFormattedMessageLogLevel()
                .getLocationAwareLoggerValue();
        this.throwableLogLevel = defaultDuplicateLogHandlerConfiguration
                .getThrowableLogLevel()
                .getLocationAwareLoggerValue();
        this.logParamsAndThrowable = defaultDuplicateLogHandlerConfiguration.isLogParamsAndThrowable();

        this.countTextPrefix = Objects.requireNonNullElse(
                defaultDuplicateLogHandlerConfiguration.getCountTextPrefix(), "");
        this.countTextSuffix = Objects.requireNonNullElse(
                defaultDuplicateLogHandlerConfiguration.getCountTextSuffix(), "");
        this.throwableTypeNameAndMessageSeparator = Objects.requireNonNullElse(
                defaultDuplicateLogHandlerConfiguration.getThrowableTypeNameAndMessageSeparator(), "");
    }

    @Override
    public void duplicateFormattedMessage(final String loggerName,
                                          final String formattedMessage,
                                          final Map<Long, LogEntry> logEntryByTimestampMap)
    {
        AppbaseUtil.notBlank(loggerName, "loggerName");
        AppbaseUtil.notBlank(formattedMessage, "formattedMessage");
        AppbaseUtil.notNull(logEntryByTimestampMap, "logEntryByTimestampMap");

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = loggerContext.getLogger(loggerName);
        final String fqcn = logger.getClass().getName();
        if (logParamsAndThrowable) {
            final List<DeDuplicateEntry> deduplicateParams = deduplicateParams(logEntryByTimestampMap);
            deduplicateParams.forEach(deDuplicateEntry -> {
                if (deDuplicateEntry != null) {
                    final String message = formattedMessage +
                            countTextPrefix +
                            deDuplicateEntry.getCount() +
                            countTextSuffix;
                    logger.log(
                            marker,
                            fqcn,
                            formattedMessageLogLevel,
                            message,
                            deDuplicateEntry.getParams(),
                            deDuplicateEntry.getThrowable());
                }
            });
        } else {
            final String message = formattedMessage + countTextPrefix + logEntryByTimestampMap.size() + countTextSuffix;
            logger.log(marker, fqcn, formattedMessageLogLevel, message, null, null);
        }
    }

    @Override
    public void duplicateThrowableTypeName(final String loggerName,
                                           final String throwableTypeName,
                                           final Map<Long, LogEntry> logEntryByTimestampMap)
    {
        AppbaseUtil.notBlank(loggerName, "loggerName");
        AppbaseUtil.notBlank(throwableTypeName, "throwableTypeName");
        AppbaseUtil.notNull(logEntryByTimestampMap, "logEntryByTimestampMap");

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = loggerContext.getLogger(loggerName);
        final String fqcn = logger.getClass().getName();

        if (logParamsAndThrowable) {
            final List<DeDuplicateEntry> deduplicateParams = deduplicateParams(logEntryByTimestampMap);
            deduplicateParams.forEach(deDuplicateEntry -> {
                if (deDuplicateEntry != null) {
                    final String message =
                            throwableTypeName + countTextPrefix + deDuplicateEntry.getCount() + countTextSuffix;
                    logger.log(
                            marker,
                            fqcn,
                            throwableLogLevel,
                            message,
                            deDuplicateEntry.getParams(),
                            deDuplicateEntry.getThrowable());
                }
            });
        } else {
            final String message =
                    throwableTypeName + countTextPrefix + logEntryByTimestampMap.size() + countTextSuffix;
            logger.log(marker, fqcn, throwableLogLevel, message, null, null);
        }
    }

    @Override
    public void duplicateThrowableMessage(final String loggerName,
                                          final String throwableTypeName,
                                          final String throwableMessage,
                                          final Map<Long, LogEntry> logEntryByTimestampMap)
    {
        AppbaseUtil.notBlank(loggerName, "loggerName");
        AppbaseUtil.notBlank(throwableTypeName, "throwableTypeName");
        AppbaseUtil.notBlank(throwableMessage, "throwableMessage");
        AppbaseUtil.notNull(logEntryByTimestampMap, "logEntryByTimestampMap");

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = loggerContext.getLogger(loggerName);
        final String fqcn = logger.getClass().getName();
        if (logParamsAndThrowable) {
            final List<DeDuplicateEntry> deduplicateParams = deduplicateParams(logEntryByTimestampMap);
            deduplicateParams.forEach(deDuplicateEntry -> {
                if (deDuplicateEntry != null) {
                    final String message = throwableTypeName +
                            throwableTypeNameAndMessageSeparator +
                            throwableMessage +
                            countTextPrefix +
                            deDuplicateEntry.getCount() +
                            countTextSuffix;
                    logger.log(
                            marker,
                            fqcn,
                            throwableLogLevel,
                            message,
                            deDuplicateEntry.getParams(),
                            deDuplicateEntry.getThrowable());
                }
            });
        } else {
            final String message = throwableTypeName +
                    throwableTypeNameAndMessageSeparator +
                    throwableMessage +
                    countTextPrefix +
                    logEntryByTimestampMap.size() +
                    countTextSuffix;
            logger.log(marker, fqcn, throwableLogLevel, message, null, null);
        }
    }

    private List<DeDuplicateEntry> deduplicateParams(final Map<Long, LogEntry> logEntryByTimestampMap) {
        final List<DeDuplicateEntry> deduplicatedEntries = new ArrayList<>();
        logEntryByTimestampMap.forEach((timestamp, logEntry) -> {
            final DeDuplicateEntry.Builder deDuplicateEntryBuilder = DeDuplicateEntry.builder();

            final Object[] params = logEntry.getParams();
            if (params != null) {
                boolean found = false;
                for (final DeDuplicateEntry deDuplicateEntry : deduplicatedEntries) {
                    final Object[] deDuplicateEntryParams = deDuplicateEntry.getParams();
                    if (deDuplicateEntryParams != null) {
                        if (Arrays.equals(deDuplicateEntryParams, params)) {
                            found = true;
                            deDuplicateEntry.getCount().incrementAndGet();
                        }
                    }
                }

                if (!found) {
                    deDuplicateEntryBuilder.params(params);

                    final Throwable throwable = logEntry.getThrowable();
                    if (throwable != null) {
                        deDuplicateEntryBuilder.throwable(throwable);
                    }

                    deduplicatedEntries.add(deDuplicateEntryBuilder.build());
                }
            }
        });

        return deduplicatedEntries;
    }

    private static final class DeDuplicateEntry {
        private final Throwable throwable;
        private final Object[] params;
        private final AtomicInteger count;

        private DeDuplicateEntry(final Builder builder) {
            throwable = builder.throwable;
            params = builder.params;
            count = new AtomicInteger(1);
        }

        public static Builder builder() {
            return new Builder();
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public Object[] getParams() {
            return params;
        }

        public AtomicInteger getCount() {
            return count;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof DeDuplicateEntry)) {
                return false;
            }

            final DeDuplicateEntry that = (DeDuplicateEntry) o;
            return Objects.equals(throwable, that.throwable) &&
                    Arrays.equals(params, that.params) &&
                    Objects.equals(count, that.count);
        }

        @Override
        @SuppressWarnings("MagicNumber")
        public int hashCode() {
            int result = Objects.hash(throwable, count);
            result = 31 * result + Arrays.hashCode(params);
            return result;
        }

        @Override
        public String toString() {
            return "DeDuplicateEntry{" +
                    "throwable=" + throwable +
                    ", params=" + Arrays.toString(params) +
                    ", count=" + count +
                    '}';
        }

        public static final class Builder {
            private Throwable throwable;
            private Object[] params;

            private Builder() {
            }

            public Builder throwable(final Throwable throwable) {
                this.throwable = throwable;
                return this;
            }

            public Builder params(final Object[] params) {
                this.params = params;
                return this;
            }

            public DeDuplicateEntry build() {
                return new DeDuplicateEntry(this);
            }
        }
    }
}
