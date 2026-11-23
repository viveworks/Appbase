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

package com.viveworks.appbase.core.service.log.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Abstract implementation of all {@link Filter}s that wrap a {@link LogEventFilterHandler}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Type of {@link LogEventFilterHandler} that is wrapped
 * @since 1.0.0
 */
public abstract class AbstractLogEventFilter<T extends LogEventFilterHandler> extends Filter<ILoggingEvent> {
    private final T filterHandler;

    protected AbstractLogEventFilter(final T filterHandler) {
        AppbaseUtil.notNull(filterHandler, "filterHandler");

        this.filterHandler = filterHandler;
        start();
        setName(getClass().getName());
    }

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        if (filterHandler.accept(event)) {
            return FilterReply.NEUTRAL;
        }

        return FilterReply.DENY;
    }

    public T getFilterHandler() {
        return filterHandler;
    }
}
