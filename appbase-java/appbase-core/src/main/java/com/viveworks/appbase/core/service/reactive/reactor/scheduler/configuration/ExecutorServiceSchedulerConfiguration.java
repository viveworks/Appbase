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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.ExecutorServiceSchedulerFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Configuration used by {@link ExecutorServiceSchedulerFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@ExecutorServiceSchedulerConfiguration.Valid
@JsonDeserialize(builder = ExecutorServiceSchedulerConfiguration.Builder.class)
public final class ExecutorServiceSchedulerConfiguration implements SchedulerConfiguration {
    private final String id;
    private final boolean defaultScheduler;
    private final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration;
    private final String executorName;

    private ExecutorServiceSchedulerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        ExecutorServiceSchedulerConfigurationValidator.validate(
                builder.id,
                builder.scheduledExecutorServiceConfiguration,
                builder.executorName);

        id = builder.id;
        defaultScheduler = builder.defaultScheduler;
        scheduledExecutorServiceConfiguration = builder.scheduledExecutorServiceConfiguration;
        executorName = builder.executorName;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isDefaultScheduler() {
        return defaultScheduler;
    }

    public ScheduledExecutorServiceConfiguration getScheduledExecutorServiceConfiguration() {
        return scheduledExecutorServiceConfiguration;
    }

    public String getExecutorName() {
        return executorName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ExecutorServiceSchedulerConfiguration that = (ExecutorServiceSchedulerConfiguration) o;
        return defaultScheduler == that.defaultScheduler &&
                Objects.equals(id, that.id) &&
                Objects.equals(scheduledExecutorServiceConfiguration, that.scheduledExecutorServiceConfiguration) &&
                Objects.equals(executorName, that.executorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, defaultScheduler, scheduledExecutorServiceConfiguration, executorName);
    }

    @Override
    public String toString() {
        return "ExecutorServiceSchedulerConfiguration{" +
                "id='" + id + '\'' +
                ", defaultScheduler=" + defaultScheduler +
                ", scheduledExecutorServiceConfiguration=" + scheduledExecutorServiceConfiguration +
                ", executorName='" + executorName + '\'' +
                '}';
    }

    /**
     * {@link ExecutorServiceSchedulerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_ID = "Appbase-Reactor-ExecutorServiceScheduler";
        private static final boolean DEFAULT_DEFAULT_SCHEDULER = false;
        private static final ScheduledExecutorServiceConfiguration
                DEFAULT_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION =
                ScheduledExecutorServiceConfiguration.builder().build();
        private static final String DEFAULT_EXECUTOR_NAME = "appbase-reactor-scheduler-executor";

        private String id = DEFAULT_ID;
        private boolean defaultScheduler = DEFAULT_DEFAULT_SCHEDULER;
        private ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration =
                DEFAULT_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION;
        private String executorName = DEFAULT_EXECUTOR_NAME;

        private Builder() {
        }

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder defaultScheduler(boolean defaultScheduler) {
            this.defaultScheduler = defaultScheduler;
            return this;
        }

        public Builder scheduledExecutorServiceConfiguration(
                final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration)
        {
            this.scheduledExecutorServiceConfiguration = scheduledExecutorServiceConfiguration;
            return this;
        }

        public Builder executorName(final String executorName) {
            this.executorName = executorName;
            return this;
        }

        public ExecutorServiceSchedulerConfiguration build() {
            return new ExecutorServiceSchedulerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link ExecutorServiceSchedulerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ExecutorServiceSchedulerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration." +
                "ExecutorServiceSchedulerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
