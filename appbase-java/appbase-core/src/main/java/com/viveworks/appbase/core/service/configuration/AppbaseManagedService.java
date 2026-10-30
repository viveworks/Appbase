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

package com.viveworks.appbase.core.service.configuration;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.Startable;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;

/**
 * An {@link AppbaseService} that is also a {@link ConfigurationListener}. The general contract is for the sub-classes
 * to:
 *
 * <ul>
 *   <li>
 *       Register for the {@link AppbaseConfiguration} that it uses.
 *   </li>
 *   <li>
 *       Reload on {@link ConfigurationListener#onConfigurationAdded(ConfigurationAddedEvent)} and
 *       {@link ConfigurationListener#onConfigurationChanged(ConfigurationChangedEvent)} events.
 *   </li>
 *   <li>
 *       Shutdown on {@link ConfigurationListener#onConfigurationRemoved(ConfigurationRemovedEvent)} event.
 *   </li>
 * </ul>
 *
 * <p>
 *     {@link AbstractAppbaseManagedService} implements this contract and it can be sub-classed, when possible.
 *     Additionally, it does configuration validation, registering for the
 *     {@link AppbaseConfiguration} type the sub-classes listen to, setting
 *     service status and so forth.
 * </p>
 * @author Navaneeth Mysore G
 * @see AbstractAppbaseManagedService
 * @since 1.0.0
 */
public interface AppbaseManagedService extends AppbaseService, ConfigurationListener, Startable {
    /**
     * Indicates if the implementation starts automatically during construction.
     *
     * @return {@code true} if it starts automatically, else {@code false}
     */
    boolean isAutoStart();
}
