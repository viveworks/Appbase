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
import jakarta.validation.constraints.NotNull;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Configuration for {@link ExecutorService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@ExecutorServiceConfiguration.Valid
@JsonDeserialize(builder = ExecutorServiceConfiguration.Builder.class)
public final class ExecutorServiceConfiguration {
    private final ExecutorServiceType executorServiceType;
    private final int fixedThreadPoolSize;
    private final ThreadFactoryConfiguration threadFactoryConfiguration;

    private ExecutorServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        ExecutorServiceConfigurationValidator.validate(
                builder.executorServiceType,
                builder.fixedThreadPoolSize,
                builder.threadFactoryConfiguration);

        executorServiceType = builder.executorServiceType;
        fixedThreadPoolSize = builder.fixedThreadPoolSize;
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

    public ExecutorServiceType getExecutorServiceType() {
        return executorServiceType;
    }

    public int getFixedThreadPoolSize() {
        return fixedThreadPoolSize;
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
        final ExecutorServiceConfiguration that = (ExecutorServiceConfiguration) o;
        return fixedThreadPoolSize == that.fixedThreadPoolSize &&
                executorServiceType == that.executorServiceType &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executorServiceType, fixedThreadPoolSize, threadFactoryConfiguration);
    }

    /**
     * Builder for {@link ExecutorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        public static final ExecutorServiceType DEFAULT_EXECUTOR_SERVICE_TYPE = ExecutorServiceType.FIXED;
        public static final int DEFAULT_FIXED_THREAD_POOL_SIZE =
                (Runtime.getRuntime().availableProcessors() * 2 - 2) <= 0
                        ? Runtime.getRuntime().availableProcessors() * 2
                        : (Runtime.getRuntime().availableProcessors() * 2 - 2);
        private static final ThreadFactoryConfiguration DEFAULT_THREAD_FACTORY_CONFIGURATION =
                ThreadFactoryConfiguration.builder().nameFormat("appbase-mediator-thread-%d").build();


        private ExecutorServiceType executorServiceType = DEFAULT_EXECUTOR_SERVICE_TYPE;
        private int fixedThreadPoolSize = DEFAULT_FIXED_THREAD_POOL_SIZE;
        private ThreadFactoryConfiguration threadFactoryConfiguration = DEFAULT_THREAD_FACTORY_CONFIGURATION;


        private Builder() {
        }

        @JsonProperty("executorServiceType")
        public Builder executorType(@NotNull final ExecutorServiceType executorServiceType) {
            this.executorServiceType = executorServiceType;
            return this;
        }

        public Builder fixedThreadPoolSize(final int fixedThreadPoolSize) {
            this.fixedThreadPoolSize = fixedThreadPoolSize;
            return this;
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public ExecutorServiceConfiguration build() {
            return new ExecutorServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link ExecutorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ExecutorServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.mediator.ExecutorServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
