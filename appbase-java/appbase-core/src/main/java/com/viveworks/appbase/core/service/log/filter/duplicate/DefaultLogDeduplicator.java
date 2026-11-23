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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.burst.LogEntry;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link LogDeduplicator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultLogDeduplicator implements LogDeduplicator {
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
            logEntryByTimestampByFormatByLoggerNameMap;
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
            logEntryByTimestampByThrowableTypeNameByLoggerNameMap;
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>>
            logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap;
    private final AtomicBoolean bursting;
    private final DuplicateLogHandler duplicateLogHandler;
    private final SchedulableConfiguration duplicateLogHandlerSchedulableConfiguration;
    private final ReentrantLock duplicateLogHandlerSchedulableLock;
    private final DuplicateLogHandlerSchedulable duplicateLogHandlerSchedulable;
    private final ScheduledFuture<Void> duplicateLogHandlerScheduledFuture;
    private final AtomicBoolean shutdown;

    public DefaultLogDeduplicator(
            final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration,
            final MediatorService mediatorService,
            final DuplicateLogHandler duplicateLogHandler,
            final AtomicBoolean bursting)
    {
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(duplicateLogHandler, "duplicateLogHandler");
        AppbaseUtil.notNull(defaultLogDeduplicatorConfiguration, "defaultLogDeduplicatorConfiguration");
        AppbaseUtil.notNull(
                defaultLogDeduplicatorConfiguration.getDuplicateLogHandlerSchedulableConfiguration(),
                "defaultLogDeduplicatorConfiguration.getDuplicateLogHandlerSchedulableConfiguration()");
        AppbaseUtil.notNull(bursting, "bursting");

        this.logEntryByTimestampByFormatByLoggerNameMap = new ConcurrentHashMap<>();
        this.logEntryByTimestampByThrowableTypeNameByLoggerNameMap = new ConcurrentHashMap<>();
        this.logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap = new ConcurrentHashMap<>();

        this.bursting = bursting;
        this.duplicateLogHandler = duplicateLogHandler;
        this.duplicateLogHandlerSchedulable = new DuplicateLogHandlerSchedulable();
        this.duplicateLogHandlerSchedulableConfiguration =
                defaultLogDeduplicatorConfiguration.getDuplicateLogHandlerSchedulableConfiguration();
        this.duplicateLogHandlerSchedulableLock = new ReentrantLock();
        this.duplicateLogHandlerScheduledFuture = mediatorService
                .getMediator()
                .scheduleWithFixedDelay(
                        new DuplicateLogHandlerSchedulable(),
                        duplicateLogHandlerSchedulableConfiguration.getInitialDelay(),
                        duplicateLogHandlerSchedulableConfiguration.getInterval(),
                        duplicateLogHandlerSchedulableConfiguration.getTimeUnit());

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public boolean duplicate(final TurboLog turboLog) {
        final Logger logger = turboLog.getLogger();
        final LogLevel logLevel = turboLog.getLogLevel();
        if (logger == null || logLevel == null) {
            return false;
        }

        final String loggerName = logger.getName();
        if (AppbaseUtil.isBlank(loggerName)) {
            return false;
        }

        final String format = turboLog.getFormattedMessage();
        final Throwable throwable = turboLog.getThrowable();
        final Thread currentThread = Thread.currentThread();
        final ThreadGroup threadGroup = currentThread.getThreadGroup();
        final LogEntry logEntry = LogEntry
                .builder()
                .threadGroupName(threadGroup != null ? threadGroup.getName() : null)
                .threadName(currentThread.getName())
                .formattedMessage(turboLog.getFormattedMessage())
                .logLevel(logLevel)
                .marker(turboLog.getMarker())
                .params(turboLog.getArgumentArray())
                .throwable(throwable)
                .build();
        final long timestamp = System.currentTimeMillis();
        final AtomicBoolean duplicate = new AtomicBoolean(true);
        if (throwable != null) {
            final String throwableMessage = throwable.getMessage();
            if (AppbaseUtil.isBlank(throwableMessage)) {
                logEntryByTimestampByThrowableTypeNameByLoggerNameMap
                        .computeIfAbsent(loggerName, f -> {
                            duplicate.set(false);
                            return new ConcurrentHashMap<>();
                        })
                        .computeIfAbsent(throwable.getClass().getName(), f -> {
                            duplicate.set(false);
                            return new ConcurrentHashMap<>();
                        })
                        .put(timestamp, logEntry);
            } else {
                logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap
                        .computeIfAbsent(loggerName, f -> {
                            duplicate.set(false);
                            return new ConcurrentHashMap<>();
                        })
                        .computeIfAbsent(throwable.getClass().getName(), f -> {
                            duplicate.set(false);
                            return new ConcurrentHashMap<>();
                        })
                        .computeIfAbsent(throwableMessage, f -> {
                            duplicate.set(false);
                            return new ConcurrentHashMap<>();
                        })
                        .put(timestamp, logEntry);
            }
        } else if (!AppbaseUtil.isBlank(format)) {
            logEntryByTimestampByFormatByLoggerNameMap
                    .computeIfAbsent(loggerName, f -> {
                        duplicate.set(false);
                        return new ConcurrentHashMap<>();
                    })
                    .computeIfAbsent(format, f -> {
                        duplicate.set(false);
                        return new ConcurrentHashMap<>();
                    })
                    .put(timestamp, logEntry);
        }

        return duplicate.get();
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

        if (!duplicateLogHandlerScheduledFuture.isCancelled()) {
            duplicateLogHandlerScheduledFuture.cancel(true);
        }

        logEntryByTimestampByFormatByLoggerNameMap.clear();
        logEntryByTimestampByThrowableTypeNameByLoggerNameMap.clear();
        logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap.clear();
    }

    @Unmodifiable
    public Map<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
        getLogEntryByTimestampByFormatByLoggerNameMap()
    {
        return Collections.unmodifiableMap(logEntryByTimestampByFormatByLoggerNameMap);
    }

    @Unmodifiable
    public Map<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
        getLogEntryByTimestampByThrowableTypeNameByLoggerNameMap()
    {
        return Collections.unmodifiableMap(logEntryByTimestampByThrowableTypeNameByLoggerNameMap);
    }

    @Unmodifiable
    public Map<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>>
        getLogEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap()
    {
        return Collections.unmodifiableMap(logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap);
    }

    public boolean isBursting() {
        return bursting.get();
    }

    public DuplicateLogHandlerSchedulable getDuplicateLogHandlerSchedulable() {
        return duplicateLogHandlerSchedulable;
    }

    /**
     * A schedulable for handling duplicate logs.
     */
    public class DuplicateLogHandlerSchedulable implements Schedulable<Void> {
        @Override
        public Void schedule() {
            if (duplicateLogHandlerSchedulableLock.isLocked()) {
                return null;
            }

            duplicateLogHandlerSchedulableLock.lock();
            try {
                logEntryByTimestampByFormatByLoggerNameMap.forEach((loggerName, logEntryByTimestampByFormatMap) -> {
                    logEntryByTimestampByFormatMap.forEach((format, logEntryByTimestampMap) -> {
                        if (!bursting.get()) {
                            duplicateLogHandler.duplicateFormattedMessage(
                                    loggerName, format, logEntryByTimestampMap);
                            logEntryByTimestampByFormatMap.remove(format);
                        }
                    });
                });

                logEntryByTimestampByThrowableTypeNameByLoggerNameMap.forEach(
                        (loggerName, logEntryByTimestampByThrowableTypeNameMap) -> {
                            logEntryByTimestampByThrowableTypeNameMap.forEach(
                                    (throwableTypeName, logEntryByTimestampMap) -> {
                                        if (!bursting.get()) {
                                            duplicateLogHandler.duplicateThrowableTypeName(
                                                    loggerName, throwableTypeName, logEntryByTimestampMap);
                                            logEntryByTimestampByThrowableTypeNameMap.remove(throwableTypeName);
                                        }
                                    });
                        }
                );

                logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap.forEach(
                        (loggerName, logEntryByTimestampByThrowableMessageByTypeNameMap) -> {
                            logEntryByTimestampByThrowableMessageByTypeNameMap.forEach(
                                    (throwableTypeName, logEntryByTimestampByThrowableMessageMap) -> {
                                        logEntryByTimestampByThrowableMessageMap.forEach(
                                                (throwableMessage, logEntryByTimestampMap) -> {
                                                    if (!bursting.get()) {
                                                        duplicateLogHandler.duplicateThrowableMessage(
                                                                loggerName,
                                                                throwableTypeName,
                                                                throwableMessage,
                                                                logEntryByTimestampMap);
                                                        logEntryByTimestampByThrowableMessageMap
                                                                .remove(throwableMessage);
                                                    }
                                                });
                                    });
                        });

                cleanupMaps();
            } finally {
                duplicateLogHandlerSchedulableLock.unlock();
            }
            return null;
        }

        private void cleanupMaps() {
            logEntryByTimestampByFormatByLoggerNameMap.forEach((loggerName, logEntryByTimestampByFormatMap) -> {
                if (logEntryByTimestampByFormatMap.isEmpty()) {
                    logEntryByTimestampByFormatByLoggerNameMap.remove(loggerName);
                }
            });

            logEntryByTimestampByThrowableTypeNameByLoggerNameMap.forEach(
                    (loggerName, logEntryByTimestampByThrowableTypeNameMap) -> {
                        if (logEntryByTimestampByThrowableTypeNameMap.isEmpty()) {
                            logEntryByTimestampByThrowableTypeNameByLoggerNameMap.remove(loggerName);
                        }
                    }
            );

            logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap.forEach(
                    (loggerName, logEntryByTimestampByThrowableMessageByTypeNameMap) -> {
                        logEntryByTimestampByThrowableMessageByTypeNameMap.forEach(
                                (throwableTypeName, logEntryByTimestampByThrowableMessageMap) -> {
                                    if (logEntryByTimestampByThrowableMessageMap.isEmpty()) {
                                        logEntryByTimestampByThrowableMessageByTypeNameMap.remove(throwableTypeName);
                                    }
                                });

                        if (logEntryByTimestampByThrowableMessageByTypeNameMap.isEmpty()) {
                            logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap.remove(loggerName);
                        }
                    });
        }

        @Override
        public SchedulableConfiguration getSchedulableConfiguration() {
            return duplicateLogHandlerSchedulableConfiguration;
        }
    }
}
