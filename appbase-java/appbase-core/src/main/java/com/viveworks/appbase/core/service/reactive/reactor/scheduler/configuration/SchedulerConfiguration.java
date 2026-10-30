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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Common configuration used to create {@link reactor.core.scheduler.Scheduler}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BoundedElasticSchedulerConfiguration.class,
                name = SchedulerTypeNames.PREFIX
                        + "BoundedElasticSchedulerConfiguration"),
        @JsonSubTypes.Type(value = ElasticSchedulerConfiguration.class,
                name = SchedulerTypeNames.PREFIX
                        + "ElasticSchedulerConfiguration"),
        @JsonSubTypes.Type(value = ExecutorServiceSchedulerConfiguration.class,
                name = SchedulerTypeNames.PREFIX
                        + "ExecutorServiceSchedulerConfiguration"),
        @JsonSubTypes.Type(value = ImmediateSchedulerConfiguration.class,
                name = SchedulerTypeNames.PREFIX
                        + "ImmediateSchedulerConfiguration"),
        @JsonSubTypes.Type(value = MediatorExecutorServiceSchedulerConfiguration.class,
                name = SchedulerTypeNames.PREFIX
                        + "MediatorExecutorServiceSchedulerConfiguration"),
        @JsonSubTypes.Type(value = ParallelSchedulerConfiguration.class,
                name = SchedulerTypeNames.PREFIX
                        + "ParallelSchedulerConfiguration"),
        @JsonSubTypes.Type(value = SingleSchedulerConfiguration.class,
                name = SchedulerTypeNames.PREFIX
                        + "SingleSchedulerConfiguration")
})
public interface SchedulerConfiguration {
    /**
     * Return a unique ID for the scheduler.
     *
     * @return Unique ID
     */
    String getId();


    /**
     * Check if the scheduler is default.
     *
     * @return True if default scheduler, false otherwise
     */
    boolean isDefaultScheduler();
}

final class SchedulerTypeNames {
    static final String PREFIX =
            "com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.";

    private SchedulerTypeNames() {
    }
}
