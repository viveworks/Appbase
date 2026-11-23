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

package com.viveworks.appbase.core.service.log.filter.event;

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.log.filter.LogEventFilterHandler;
import com.viveworks.appbase.core.service.log.filter.TurboLogEventFilterHandler;

import java.util.Collection;

/**
 * A common interface for {@link LogEventFilterHandler} and {@link TurboLogEventFilterHandler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface LogAndTurboLogEventFilterDelegator
        extends LogEventFilterHandler, TurboLogEventFilterHandler, Shutdownable
{
    /**
     * Get the registered list of {@link LogEventFilterListener}s.
     *
     * @return LogEventFilterListeners
     */
    Collection<LogEventFilterListener> getLogEventFilterListeners();


    /**
     * Register a {@link LogEventFilterListener}.
     *
     * @param logEventFilterListener {@link LogEventFilterListener} to add
     */
    void registerLogEventListener(LogEventFilterListener logEventFilterListener);


    /**
     * Unregister a {@link LogEventFilterListener} by type.
     *
     * @param logEventFilterListenerType {@link LogEventFilterListener} to remove by type
     */
    void unregisterLogEventListener(Class<? extends LogEventFilterListener> logEventFilterListenerType);


    /**
     * Clear {@link LogEventFilterListener}.
     */
    void clearLogEventFilterListeners();


    /**
     * Get the registered list of {@link TurboLogEventFilterListener}s.
     *
     * @return TurboLogEventFilterListeners
     */
    Collection<TurboLogEventFilterListener> getTurboLogEventFilterListeners();


    /**
     * Register a {@link TurboLogEventFilterListener}.
     *
     * @param turboLogEventFilterListener {@link TurboLogEventFilterListener} to add
     */
    void registerTurboLogEventListener(TurboLogEventFilterListener turboLogEventFilterListener);


    /**
     * Unregister a {@link TurboLogEventFilterListener} by type.
     *
     * @param turboLogEventFilterListenerType {@link TurboLogEventFilterListener} to remove by type
     */
    void unregisterTurboLogEventListener(Class<? extends TurboLogEventFilterListener> turboLogEventFilterListenerType);


    /**
     * Clear {@link TurboLogEventFilterListener}.
     */
    void clearTurboLogEventListeners();
}
