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

package com.viveworks.appbase.core.service.mediator.event.delegator;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link MediatorEventDelegatorRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public final class DefaultMediatorEventDelegatorRegistry implements MediatorEventDelegatorRegistry {
    private final AtomicReference<Logger> loggerRef;
    private final Map<String, List<MediatorEventListener>> mediatorEventListenersByEventTypeMap;
    private final AtomicBoolean shutdown;

    public DefaultMediatorEventDelegatorRegistry() {
        this.mediatorEventListenersByEventTypeMap = new ConcurrentHashMap<>();
        this.loggerRef = new AtomicReference<>();
        this.shutdown = new AtomicBoolean(false);
    }

    @Nullable
    @Unmodifiable
    @Override
    public Collection<MediatorEventListener> getMediatorEventListeners(
            final Class<? extends MediatorEventType> mediatorEventType)
    {
        final List<MediatorEventListener> mediatorEventListeners =
                mediatorEventListenersByEventTypeMap.get(mediatorEventType.getName());
        if (mediatorEventListeners != null) {
            return Collections.unmodifiableCollection(mediatorEventListeners);
        }

        return null;
    }

    @Override
    public void registerMediatorEventListener(
            final Class<? extends MediatorEventType> mediatorEventType,
            final MediatorEventListener mediatorEventListener)
    {
        AppbaseUtil.notNull(mediatorEventType, "eventType");
        AppbaseUtil.notNull(mediatorEventListener, "mediatorEventListener");

        final Logger logger = loggerRef.get();
        if (logger != null) {
            logger.info("Registering {} for event type {}",
                    mediatorEventListener.getClass().getName(), mediatorEventType);
        }

        mediatorEventListenersByEventTypeMap
                .computeIfAbsent(mediatorEventType.getName(), f -> new CopyOnWriteArrayList<>())
                .add(mediatorEventListener);
    }

    @Override
    public void unregisterMediatorEventListener(
            final Class<? extends MediatorEventType> mediatorEventType,
            final Class<? extends MediatorEventListener> mediatorEventListenerType)
    {
        AppbaseUtil.notNull(mediatorEventType, "mediatorEventType");
        AppbaseUtil.notNull(mediatorEventListenerType, "mediatorEventListenerType");
        final Logger logger = loggerRef.get();

        if (logger != null) {
            logger.info("Unregistering {} for event type {}", mediatorEventListenerType.getName(), mediatorEventType);
        }

        final List<MediatorEventListener> mediatorEventListeners =
                mediatorEventListenersByEventTypeMap.get(mediatorEventType.getName());
        if (mediatorEventListeners != null) {
            final List<Integer> listenerIndices = new ArrayList<>();
            int listIndex = 0;
            for (final MediatorEventListener mediatorEventListener : mediatorEventListeners) {
                if (mediatorEventListenerType.isAssignableFrom(mediatorEventListener.getClass())) {
                    listenerIndices.add(listIndex);
                }
                ++listIndex;
            }

            if (!listenerIndices.isEmpty()) {
                int removedCount = 0;

                for (final Integer index : listenerIndices) {
                    final int indexToRemove;
                    if (removedCount == 0) {
                        indexToRemove = index;
                    } else {
                        indexToRemove = index - removedCount;
                    }
                    final MediatorEventListener removed = mediatorEventListeners.remove(indexToRemove);
                    if (removed == null) {
                        if (logger != null) {
                            logger.warn("Failed to remove listener {} at index {}",
                                    mediatorEventListeners.get(index), index);
                        }
                    } else {
                        ++removedCount;
                    }
                }
            }
        }
    }

    @Override
    public void clearMediatorEventListeners() {
        final Logger logger = loggerRef.get();
        if (logger != null) {
            logger.info("Clearing mediatorEventListenersByEventTypeMap");
        }

        mediatorEventListenersByEventTypeMap.clear();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        clearMediatorEventListeners();
    }

    /**
     * Set the {@link LogService}.
     *
     * @param logService Log service to set
     */
    public void setLogService(final LogService logService) {
        AppbaseUtil.notNull(logService, "logService");

        this.loggerRef.set(logService.getLogger(getClass()));
    }
}
