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

package com.viveworks.appbase.core.service.log.filter.metrics;

import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.TurboLogEventFilterHandler;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link TurboLogEventFilterHandler} used to measure various log attributes.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class MetricsTurboLogEventFilterHandler implements TurboLogEventFilterHandler {
    private final MetricService metricService;
    private final Map<String, Counter> counterByLogLevelMap;
    private final Map<String, Counter> counterByLoggerNameMap;
    private final Map<String, Counter> counterByFormattedMessageMap;
    private final Map<String, Counter> counterByThrowableTypeNameMap;

    private final boolean measureLogLevels;

    private final boolean measureLoggerNames;
    private final Set<String> allowedLoggerNames;
    private final Set<String> blockedLoggerNames;

    private final boolean measureFormattedMessages;
    private final Set<String> allowedFormattedMessages;
    private final Set<String> blockedFormattedMessages;

    private final boolean measureThrowableTypeNames;
    private final Set<String> allowedThrowableTypeNames;
    private final Set<String> blockedThrowableTypeNames;

    public MetricsTurboLogEventFilterHandler(
            final MetricService metricService,
            final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration)
    {
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(metricsTurboFilterHandlerConfiguration, "metricsTurboFilterHandlerConfiguration");

        this.metricService = metricService;

        measureLogLevels = metricsTurboFilterHandlerConfiguration.isMeasureLogLevels();

        measureLoggerNames = metricsTurboFilterHandlerConfiguration.isMeasureLoggerNames();
        allowedLoggerNames = metricsTurboFilterHandlerConfiguration.getAllowedLoggerNames();
        blockedLoggerNames = metricsTurboFilterHandlerConfiguration.getBlockedLoggerNames();

        measureFormattedMessages = metricsTurboFilterHandlerConfiguration.isMeasureFormattedMessages();
        allowedFormattedMessages = metricsTurboFilterHandlerConfiguration.getAllowedFormattedMessages();
        blockedFormattedMessages = metricsTurboFilterHandlerConfiguration.getBlockedFormattedMessages();

        measureThrowableTypeNames = metricsTurboFilterHandlerConfiguration.isMeasureThrowableTypeNames();
        allowedThrowableTypeNames = metricsTurboFilterHandlerConfiguration.getAllowedThrowableTypeNames();
        blockedThrowableTypeNames = metricsTurboFilterHandlerConfiguration.getBlockedThrowableTypeNames();

        this.counterByLogLevelMap = new ConcurrentHashMap<>();
        this.counterByLoggerNameMap = new ConcurrentHashMap<>();
        this.counterByFormattedMessageMap = new ConcurrentHashMap<>();
        this.counterByThrowableTypeNameMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean accept(final TurboLog turboLog) {
        final LogLevel logLevel = turboLog.getLogLevel();
        if (logLevel != null && isMeasureLogLevels()) {
            counterByLogLevelMap
                    .computeIfAbsent(logLevel.name(), f -> metricService
                            .getMetricRegistry()
                            .counter(logLevel.name()))
                    .inc();
        }

        final Logger logger = turboLog.getLogger();
        if (logger != null && isMeasureLoggerNames()) {
            final String loggerName = logger.getName();
            if (accept(loggerName, allowedLoggerNames, blockedLoggerNames)) {
                counterByLoggerNameMap
                        .computeIfAbsent(loggerName, f -> metricService
                                .getMetricRegistry()
                                .counter(loggerName))
                        .inc();
            }
        }

        final String formattedMessage = turboLog.getFormattedMessage();
        if (formattedMessage != null && isMeasureFormattedMessages()) {
            if (accept(formattedMessage, allowedFormattedMessages, blockedFormattedMessages)) {
                counterByFormattedMessageMap
                        .computeIfAbsent(formattedMessage, f -> metricService
                                .getMetricRegistry()
                                .counter(formattedMessage))
                        .inc();
            }
        }

        final Throwable throwable = turboLog.getThrowable();
        if (throwable != null && isMeasureThrowableTypeNames()) {
            final String throwableTypeName = throwable.getClass().getName();
            if (accept(throwableTypeName, allowedThrowableTypeNames, blockedThrowableTypeNames)) {
                counterByThrowableTypeNameMap
                        .computeIfAbsent(throwableTypeName, f -> metricService
                                .getMetricRegistry()
                                .counter(throwableTypeName))
                        .inc();
            }
        }

        return true;
    }

    /**
     * Common logic to check if the value is allowed or accept.
     *
     * @param value Value
     * @param allowedSet Allowed value set
     * @param blockedSet Blocked value set
     * @return true if accept, false otherwise
     */
    private static boolean accept(final String value,
                                  final Set<String> allowedSet,
                                  final Set<String> blockedSet)
    {
        if (blockedSet != null && blockedSet.contains(value)) {
            return false;
        }

        if (allowedSet != null && !allowedSet.isEmpty()) {
            return allowedSet.contains(value);
        }

        return true;
    }

    public MetricService getMetricService() {
        return metricService;
    }

    public Map<String, Counter> getCounterByLogLevelMap() {
        return counterByLogLevelMap;
    }

    public Map<String, Counter> getCounterByLoggerNameMap() {
        return counterByLoggerNameMap;
    }

    public Map<String, Counter> getCounterByFormattedMessageMap() {
        return counterByFormattedMessageMap;
    }

    public Map<String, Counter> getCounterByThrowableTypeNameMap() {
        return counterByThrowableTypeNameMap;
    }

    public boolean isMeasureLogLevels() {
        return measureLogLevels;
    }

    public boolean isMeasureLoggerNames() {
        return measureLoggerNames;
    }

    public Set<String> getAllowedLoggerNames() {
        return allowedLoggerNames;
    }

    public Set<String> getBlockedLoggerNames() {
        return blockedLoggerNames;
    }

    public boolean isMeasureFormattedMessages() {
        return measureFormattedMessages;
    }

    public Set<String> getAllowedFormattedMessages() {
        return allowedFormattedMessages;
    }

    public Set<String> getBlockedFormattedMessages() {
        return blockedFormattedMessages;
    }

    public boolean isMeasureThrowableTypeNames() {
        return measureThrowableTypeNames;
    }

    public Set<String> getAllowedThrowableTypeNames() {
        return allowedThrowableTypeNames;
    }

    public Set<String> getBlockedThrowableTypeNames() {
        return blockedThrowableTypeNames;
    }
}
