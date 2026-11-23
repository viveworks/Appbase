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

package com.viveworks.appbase.core.service.reactive.reactor;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.SchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SchedulerConfiguration;
import com.viveworks.appbase.core.util.Unmodifiable;
import reactor.core.scheduler.Scheduler;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * <code>Project Reactor</code> related service.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ReactorService extends AppbaseService {
    /**
     * Return the default {@link Scheduler}.
     *
     * @return Default scheduler
     */
    Scheduler getDefaultScheduler();


    /**
     * Return {@link Scheduler} by ID.
     *
     * @param schedulerId Scheduler ID
     * @return Scheduler
     */
    @Nullable
    Scheduler getSchedulerById(String schedulerId);


    /**
     * Get {@link SchedulerFactory} by {@link SchedulerConfiguration} type name map.
     *
     * @return Scheduler factory by configuration type name map
     */
    @Unmodifiable
    Map<String, SchedulerFactory<?>> getSchedulerFactoryByConfigurationTypeMap();


    /**
     * Register a scheduler factory.
     *
     * @param schedulerConfigurationType Scheduler configuration class
     * @param schedulerFactory Scheduler factory
     * @param <C> Scheduler configuration type
     */
    <C extends SchedulerConfiguration> void registerSchedulerFactoryByConfigurationType(
            Class<C> schedulerConfigurationType, SchedulerFactory<C> schedulerFactory);


    /**
     * Unregister scheduler factory by {@link SchedulerConfiguration} type.
     *
     * @param schedulerConfigurationType Schedule configuration type
     */
    void unregisterSchedulerFactoryByConfigurationType(
            Class<? extends SchedulerConfiguration> schedulerConfigurationType);


    /**
     * Clear {@link SchedulerFactory} by {@link SchedulerConfiguration} type map.
     */
    void clearSchedulerFactoryByConfigurationTypeMap();
}
