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

import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.util.AppbaseRegistry;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * A registry used by {@link MediatorEventHandlerDelegator} to manage {@link MediatorEventListener}s.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface MediatorEventDelegatorRegistry extends AppbaseRegistry {
    /**
     * Get the unmodifiable list of registered {@link MediatorEventListener}s that listen for specific
     * {@link MediatorEventType}s.
     *
     * @param mediatorEventType MediatorEventType that listeners listen to
     * @return List of MediatorEventListeners
     */
    @Nullable
    @Unmodifiable
    Collection<MediatorEventListener> getMediatorEventListeners(Class<? extends MediatorEventType> mediatorEventType);


    /**
     * Register {@link MediatorEventListener} that listens to specific {@link MediatorEventType}.
     *
     * @param mediatorEventType Mediator event type to listen to
     * @param mediatorEventListener Mediator event listener
     */
    void registerMediatorEventListener(Class<? extends MediatorEventType> mediatorEventType,
                                       MediatorEventListener mediatorEventListener);


    /**
     * Unregister {@link MediatorEventListener} from specific {@link MediatorEventType}.
     *
     * @param mediatorEventType Mediator event type
     * @param mediatorEventListenerType Mediator event listener type
     */
    void unregisterMediatorEventListener(Class<? extends MediatorEventType> mediatorEventType,
                                         Class<? extends MediatorEventListener> mediatorEventListenerType);


    /**
     * Clear all {@link MediatorEventListener}s.
     */
    void clearMediatorEventListeners();
}
