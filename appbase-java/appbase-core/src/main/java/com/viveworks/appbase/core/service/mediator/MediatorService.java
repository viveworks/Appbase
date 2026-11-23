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

package com.viveworks.appbase.core.service.mediator;

import com.viveworks.appbase.core.service.AppbaseService;

import javax.annotation.Nullable;

/**
 * A service for {@link Mediator} configurable with {@link DefaultMediatorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface MediatorService extends AppbaseService {
    /**
     * Get the managed {@link Mediator} object.
     *
     * @return Mediator
     */
    Mediator getMediator();


    /**
     * Used to check if the initialization was done using a file (usually a YAML file).
     *
     * @return true if configured using a YAML file, false otherwise
     */
    boolean isInitWithConfigurationFile();


    /**
     * Used to check if the initialization was done using default configurations.
     *
     * @return true if default configuration, false otherwise
     */
    boolean isInitWithDefaultConfiguration();


    /**
     * Get the configuration file path, if configured using a file (usually a YAML file).
     *
     * @return the configuration file
     */
    @Nullable
    String getConfigurationFilePath();
}
