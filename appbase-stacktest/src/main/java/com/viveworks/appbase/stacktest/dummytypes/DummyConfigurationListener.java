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
package com.viveworks.appbase.stacktest.dummytypes;

import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListenerException;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyConfigurationListener implements ConfigurationListener {
    @Override
    public void onConfigurationAdded(
            final ConfigurationAddedEvent configurationAddedEvent)
            throws ConfigurationListenerException {
        // no-op
    }

    @Override
    public void onConfigurationChanged(
            final ConfigurationChangedEvent configurationChangedEvent)
            throws ConfigurationListenerException {
        // no-op
    }

    @Override
    public void onConfigurationRemoved(
            final ConfigurationRemovedEvent configurationRemovedEvent)
            throws ConfigurationListenerException {
        // no-op
    }
}
