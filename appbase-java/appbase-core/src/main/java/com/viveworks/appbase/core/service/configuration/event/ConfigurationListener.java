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

package com.viveworks.appbase.core.service.configuration.event;

/**
 * Represents a configuration listener.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ConfigurationListener {
    /**
     * Called when a configuration was added.
     *
     * @param configurationAddedEvent added configuration event
     * @throws ConfigurationListenerException if there is an exception while handling the event
     */
    void onConfigurationAdded(
            ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationListenerException;


    /**
     * Called when a configuration changed.
     *
     * @param configurationChangedEvent changed configuration event
     * @throws ConfigurationListenerException if there is an exception while handling the event
     */
    void onConfigurationChanged(
            ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationListenerException;


    /**
     * Called when a configuration was removed.
     *
     * @param configurationRemovedEvent removed configuration event
     * @throws ConfigurationListenerException if there is an exception while handling the event
     */
    void onConfigurationRemoved(
            ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationListenerException;
}
