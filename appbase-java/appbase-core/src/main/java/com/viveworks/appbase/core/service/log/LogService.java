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

package com.viveworks.appbase.core.service.log;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.appender.disruptor.MediatorEventDelegatingAppender;
import com.viveworks.appbase.core.service.log.filter.event.LogAndTurboLogEventFilterDelegator;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * A service for SLF4J's {@link Logger}. By default, <code>logback</code> is used as the SLF4J binding to
 * do the log.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface LogService extends AppbaseService {
    /**
     * Get the {@link Logger} for a <code>class</code>.
     *
     * @param type Class's logger to return
     * @return Logger
     */
    Logger getLogger(Class<?> type);


    /**
     * Return the {@link LogAccessor} used, or {@code null} if no log accessor has been
     * configured (e.g. when the Lucene log store is disabled).
     *
     * @return Log accessor, or {@code null} if not configured
     */
    @Nullable
    LogAccessor getLogAccessor();


    /**
     * Get the {@link LogAndTurboLogEventFilterDelegator}.
     *
     * @return LogAndTurboLogEventFilterDelegator
     */
    @Nullable
    LogAndTurboLogEventFilterDelegator getLogAndTurboLogEventFilterDelegator();

    /**
     * Get the {@link MediatorEventDelegatingAppender}.
     *
     * @return MediatorEventDelegatingAppender
     */
    @Nullable
    MediatorEventDelegatingAppender getMediatorEventDelegatingAppender();
}
