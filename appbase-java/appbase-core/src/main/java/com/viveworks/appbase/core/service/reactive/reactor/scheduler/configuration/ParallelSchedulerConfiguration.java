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
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.ParallelSchedulerFactory;
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
 * Configuration used by {@link ParallelSchedulerFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@ParallelSchedulerConfiguration.Valid
@JsonDeserialize(builder = ParallelSchedulerConfiguration.Builder.class)
public final class ParallelSchedulerConfiguration implements SchedulerConfiguration {
    private final String id;
    private final boolean defaultScheduler;
    private final int parallelism;
    private final ThreadFactoryConfiguration threadFactoryConfiguration;

    private ParallelSchedulerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        ParallelSchedulerConfigurationValidator.validate(
                builder.id,
                builder.threadFactoryConfiguration,
                builder.parallelism);

        id = builder.id;
        defaultScheduler = builder.defaultScheduler;
        parallelism = builder.parallelism;
        threadFactoryConfiguration = builder.threadFactoryConfiguration;
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

    public int getParallelism() {
        return parallelism;
    }

    public ThreadFactoryConfiguration getThreadFactoryConfiguration() {
        return threadFactoryConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ParallelSchedulerConfiguration that = (ParallelSchedulerConfiguration) o;
        return defaultScheduler == that.defaultScheduler &&
                parallelism == that.parallelism &&
                Objects.equals(id, that.id) &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, defaultScheduler, parallelism, threadFactoryConfiguration);
    }

    @Override
    public String toString() {
        return "ParallelSchedulerConfiguration{" +
                "id='" + id + '\'' +
                ", defaultScheduler=" + defaultScheduler +
                ", parallelism=" + parallelism +
                ", threadFactoryConfiguration=" + threadFactoryConfiguration +
                '}';
    }

    /**
     * {@link ParallelSchedulerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_ID = "Appbase-Reactor-ParallelScheduler";
        private static final boolean DEFAULT_DEFAULT_SCHEDULER = false;
        private static final ThreadFactoryConfiguration DEFAULT_THREAD_FACTORY_CONFIGURATION =
                ThreadFactoryConfiguration
                        .builder()
                        .nameFormat("appbase-reactor-parallel-scheduler-thread-%d")
                        .daemon(false)
                        .build();
        private static final int DEFAULT_PARALLELISM = (Runtime.getRuntime().availableProcessors() / 2);

        private String id = DEFAULT_ID;
        private boolean defaultScheduler = DEFAULT_DEFAULT_SCHEDULER;
        private int parallelism = DEFAULT_PARALLELISM;
        private ThreadFactoryConfiguration threadFactoryConfiguration = DEFAULT_THREAD_FACTORY_CONFIGURATION;

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

        public Builder parallelism(final int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public ParallelSchedulerConfiguration build() {
            return new ParallelSchedulerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link ParallelSchedulerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ParallelSchedulerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration." +
                "ParallelSchedulerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
