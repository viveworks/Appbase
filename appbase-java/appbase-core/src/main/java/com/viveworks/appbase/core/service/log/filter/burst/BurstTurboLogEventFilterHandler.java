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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.TurboLogEventFilterHandler;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultDuplicateLogHandler;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultLogDeduplicator;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogHandler;
import com.viveworks.appbase.core.service.log.filter.duplicate.LogDeduplicator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to handle a burst of log message.
 *
 * <p>
 * Behavior: Detect burst - De-dup mode - De-duped message handler (log when free / discard).
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class BurstTurboLogEventFilterHandler implements TurboLogEventFilterHandler, Shutdownable {
    private static final boolean DEFAULT_ENABLED = true;

    private final AtomicBoolean enabled;
    private final double burstRateThreshold;
    private final AtomicBoolean bursting;
    private final LogRateCalculator logRateCalculator;
    private final LogDeduplicator logDeduplicator;
    private final DuplicateLogHandler duplicateLogHandler;
    private final AtomicBoolean shutdown;

    public BurstTurboLogEventFilterHandler(
            final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration,
            final MediatorService mediatorService)
    {
        this(
                burstTurboLogEventFilterHandlerConfiguration,
                mediatorService,
                new DefaultDuplicateLogHandler(
                        burstTurboLogEventFilterHandlerConfiguration.getDefaultDuplicateLogHandlerConfiguration()));
    }

    public BurstTurboLogEventFilterHandler(
            final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration,
            final MediatorService mediatorService,
            final DuplicateLogHandler duplicateLogHandler)
    {
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(
                burstTurboLogEventFilterHandlerConfiguration, "burstTurboLogEventFilterHandlerConfiguration");
        AppbaseUtil.notNull(duplicateLogHandler, "duplicateLogHandler");

        this.enabled = new AtomicBoolean(burstTurboLogEventFilterHandlerConfiguration.isEnabled());
        this.burstRateThreshold = burstTurboLogEventFilterHandlerConfiguration.getBurstRateThreshold();
        this.bursting = new AtomicBoolean(false);

        this.logRateCalculator = new DefaultLogRateCalculator(
                burstTurboLogEventFilterHandlerConfiguration.getDefaultLogRateCalculatorConfiguration());
        this.duplicateLogHandler = duplicateLogHandler;
        this.logDeduplicator = new DefaultLogDeduplicator(
                burstTurboLogEventFilterHandlerConfiguration.getDefaultLogDeduplicatorConfiguration(),
                mediatorService,
                duplicateLogHandler,
                bursting);
        this.shutdown = new AtomicBoolean(false);
    }

    public BurstTurboLogEventFilterHandler(final double burstRateThreshold,
                                           final LogRateCalculator logRateCalculator,
                                           final LogDeduplicator logDeduplicator,
                                           final DuplicateLogHandler duplicateLogHandler)
    {
        AppbaseUtil.positive(burstRateThreshold, "burstRateThreshold");
        AppbaseUtil.notNull(logRateCalculator, "logRateCalculator");
        AppbaseUtil.notNull(logDeduplicator, "logDeduplicator");

        this.enabled = new AtomicBoolean(DEFAULT_ENABLED);
        this.burstRateThreshold = burstRateThreshold;
        this.logRateCalculator = logRateCalculator;
        this.logDeduplicator = logDeduplicator;
        this.duplicateLogHandler = duplicateLogHandler;
        this.bursting = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public boolean accept(final TurboLog turboLog) {
        if (!enabled.get()) {
            return true;
        }

        final double currentRate = logRateCalculator.measure(turboLog);
        if (currentRate >= burstRateThreshold) {
            bursting.compareAndSet(false, true);
            logDeduplicator.duplicate(turboLog);
            return false;
        } else {
            bursting.compareAndSet(true, false);
        }

        return true;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public double getBurstRateThreshold() {
        return burstRateThreshold;
    }

    public boolean isBursting() {
        return bursting.get();
    }

    public LogDeduplicator getLogDeduplicator() {
        return logDeduplicator;
    }

    @Nullable
    public DuplicateLogHandler getDuplicateLogHandler() {
        return duplicateLogHandler;
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        if (logDeduplicator != null) {
            logDeduplicator.shutdown();
        }

        enabled.set(false);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}
