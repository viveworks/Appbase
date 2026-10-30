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

package com.viveworks.appbase.core.service.log.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.slf4j.Marker;

/**
 * Abstract implementation of all {@link TurboFilter}s that wrap a {@link TurboLogEventFilterHandler}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Type of {@link TurboLogEventFilterHandler} that is wrapped
 * @since 1.0.0
 */
public abstract class AbstractTurboLogEventFilter<T extends TurboLogEventFilterHandler> extends TurboFilter {
    private final T turboFilterHandler;

    protected AbstractTurboLogEventFilter(final T turboFilterHandler) {
        AppbaseUtil.notNull(turboFilterHandler, "turboFilterHandler");

        this.turboFilterHandler = turboFilterHandler;
        start();
        setName(getClass().getName());
    }

    @Override
    public FilterReply decide(final Marker marker,
                              final Logger logger,
                              final Level level,
                              final String format,
                              final Object[] params,
                              final Throwable t)
    {
        final long timestamp = System.currentTimeMillis();
        final LogLevel logLevel = (level != null) ? LogLevel.from(level) : null;
        final TurboLog turboLog = TurboLog
                .builder()
                .argumentArray(params)
                .formattedMessage(format)
                .logger(logger)
                .logLevel(logLevel)
                .marker(marker)
                .throwable(t)
                .timestamp(timestamp)
                .build();
        if (turboFilterHandler.accept(turboLog)) {
            return FilterReply.NEUTRAL;
        }

        return FilterReply.DENY;
    }

    public T getTurboFilterHandler() {
        return turboFilterHandler;
    }
}
