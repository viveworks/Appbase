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

package com.viveworks.appbase.core.service.mediator.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
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
 * Configuration for {@link java.util.concurrent.ScheduledExecutorService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@ScheduledExecutorServiceConfiguration.Valid
@JsonDeserialize(builder = ScheduledExecutorServiceConfiguration.Builder.class)
public final class ScheduledExecutorServiceConfiguration {
    private final ScheduledExecutorServiceType scheduledExecutorServiceType;
    private final int threadPoolSize;
    private final ThreadFactoryConfiguration threadFactoryConfiguration;

    private ScheduledExecutorServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        ScheduledExecutorServiceConfigurationValidator.validate(
                builder.scheduledExecutorServiceType,
                builder.threadPoolSize,
                builder.threadFactoryConfiguration);

        scheduledExecutorServiceType = builder.scheduledExecutorServiceType;
        threadPoolSize = builder.threadPoolSize;
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

    public ScheduledExecutorServiceType getScheduledExecutorServiceType() {
        return scheduledExecutorServiceType;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
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
        final ScheduledExecutorServiceConfiguration that = (ScheduledExecutorServiceConfiguration) o;
        return threadPoolSize == that.threadPoolSize &&
                scheduledExecutorServiceType == that.scheduledExecutorServiceType &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduledExecutorServiceType, threadPoolSize, threadFactoryConfiguration);
    }

    @Override
    public String toString() {
        return "ScheduledExecutorServiceConfiguration{" +
                "scheduledExecutorServiceType=" + scheduledExecutorServiceType +
                ", threadPoolSize=" + threadPoolSize +
                ", threadFactoryConfiguration=" + threadFactoryConfiguration +
                '}';
    }

    /**
     * {@link ScheduledExecutorServiceConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private ScheduledExecutorServiceType scheduledExecutorServiceType =
                ScheduledExecutorServiceType.SINGLE_THREADED;
        private int threadPoolSize = (Runtime.getRuntime().availableProcessors() / 2);
        private ThreadFactoryConfiguration threadFactoryConfiguration =
                ThreadFactoryConfiguration
                .builder()
                .nameFormat("appbase-scheduled-executor-%d")
                .daemon(false)
                .build();

        private Builder() {
        }

        @JsonProperty("scheduledExecutorServiceType")
        public Builder scheduledExecutorType(final ScheduledExecutorServiceType scheduledExecutorServiceType) {
            this.scheduledExecutorServiceType = scheduledExecutorServiceType;
            return this;
        }

        public Builder threadPoolSize(final int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public ScheduledExecutorServiceConfiguration build() {
            return new ScheduledExecutorServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link ScheduledExecutorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ScheduledExecutorServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.mediator.ScheduledExecutorServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
