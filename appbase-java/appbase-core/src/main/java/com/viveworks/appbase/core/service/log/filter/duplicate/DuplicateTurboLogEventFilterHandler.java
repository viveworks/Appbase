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
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.TurboLogEventFilterHandler;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to handle duplicate log message.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DuplicateTurboLogEventFilterHandler implements TurboLogEventFilterHandler, Shutdownable {
    private static final boolean DEFAULT_ENABLED = true;

    private final AtomicBoolean enabled;
    private final LogDeduplicator logDeduplicator;
    private final AtomicBoolean shutdown;

    public DuplicateTurboLogEventFilterHandler(
            final MediatorService mediatorService,
            final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration)
    {
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(duplicateLogTurboFilterHandlerConfiguration, "duplicateLogTurboFilterHandlerConfiguration");

        this.enabled = new AtomicBoolean(DEFAULT_ENABLED);
        final AtomicBoolean bursting = new AtomicBoolean(false);

        final DuplicateLogHandler duplicateLogHandler = new DefaultDuplicateLogHandler(
                duplicateLogTurboFilterHandlerConfiguration.getDefaultDuplicateLogHandlerConfiguration());
        this.logDeduplicator = new DefaultLogDeduplicator(
                duplicateLogTurboFilterHandlerConfiguration.getDefaultLogDeduplicatorConfiguration(),
                mediatorService,
                duplicateLogHandler,
                bursting);
        this.shutdown = new AtomicBoolean(false);
    }

    public DuplicateTurboLogEventFilterHandler(
            final MediatorService mediatorService,
            final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration,
            final DuplicateLogHandler duplicateLogHandler,
            final LogDeduplicator logDeduplicator)
    {
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(duplicateLogTurboFilterHandlerConfiguration, "duplicateLogTurboFilterHandlerConfiguration");
        AppbaseUtil.notNull(duplicateLogHandler, "duplicateLogHandler");

        this.enabled = new AtomicBoolean(DEFAULT_ENABLED);
        this.logDeduplicator = logDeduplicator;
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public boolean accept(final TurboLog turboLog) {
        if (!enabled.get()) {
            return true;
        }

        final boolean duplicate = logDeduplicator.duplicate(turboLog);
        return !duplicate;
    }

    public boolean isEnabled() {
        return enabled.get();
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

        if (logDeduplicator != null) {
            logDeduplicator.shutdown();
        }

        enabled.set(false);
    }
}
