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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.ElasticSchedulerFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import reactor.core.scheduler.Schedulers;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Configuration used by {@link ElasticSchedulerFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@ElasticSchedulerConfiguration.Valid
@JsonDeserialize(builder = ElasticSchedulerConfiguration.Builder.class)
public final class ElasticSchedulerConfiguration implements SchedulerConfiguration {
    private final String id;
    private final boolean defaultScheduler;
    private final int threadCap;
    private final int queuedTaskCap;
    private final int ttlSeconds;
    private final ThreadFactoryConfiguration threadFactoryConfiguration;

    private ElasticSchedulerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        ElasticSchedulerConfigurationValidator.validate(
                builder.id,
                builder.threadCap,
                builder.queuedTaskCap,
                builder.ttlSeconds,
                builder.threadFactoryConfiguration);

        id = builder.id;
        defaultScheduler = builder.defaultScheduler;
        threadCap = builder.threadCap;
        queuedTaskCap = builder.queuedTaskCap;
        ttlSeconds = builder.ttlSeconds;
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

    /**
     * Get the maximum number of threads in the bounded elastic scheduler.
     *
     * @return thread cap
     */
    public int getThreadCap() {
        return threadCap;
    }

    /**
     * Get the maximum number of tasks that can be queued per thread in the bounded elastic scheduler.
     *
     * @return queued task cap
     */
    public int getQueuedTaskCap() {
        return queuedTaskCap;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
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
        final ElasticSchedulerConfiguration that = (ElasticSchedulerConfiguration) o;
        return defaultScheduler == that.defaultScheduler &&
                threadCap == that.threadCap &&
                queuedTaskCap == that.queuedTaskCap &&
                ttlSeconds == that.ttlSeconds &&
                Objects.equals(id, that.id) &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, defaultScheduler, threadCap, queuedTaskCap, ttlSeconds, threadFactoryConfiguration);
    }

    @Override
    public String toString() {
        return "ElasticSchedulerConfiguration{" +
                "id='" + id + '\'' +
                ", defaultScheduler=" + defaultScheduler +
                ", threadCap=" + threadCap +
                ", queuedTaskCap=" + queuedTaskCap +
                ", ttlSeconds=" + ttlSeconds +
                ", threadFactoryConfiguration=" + threadFactoryConfiguration +
                '}';
    }

    /**
     * {@link ElasticSchedulerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings(value = "FCBL_FIELD_COULD_BE_LOCAL",
            justification = "Builder fields require instance scope for Jackson deserialization via @JsonPOJOBuilder")
    public static final class Builder {
        private static final String DEFAULT_ID = "Appbase-Reactor-ElasticScheduler";
        private static final boolean DEFAULT_DEFAULT_SCHEDULER = false;
        private static final int DEFAULT_THREAD_CAP = Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE;
        private static final int DEFAULT_QUEUED_TASK_CAP = Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE;
        private static final int DEFAULT_TTL_SECONDS = 5;
        private static final ThreadFactoryConfiguration DEFAULT_THREAD_FACTORY_CONFIGURATION =
                ThreadFactoryConfiguration
                        .builder()
                        .nameFormat("appbase-elastic-scheduler-configuration-%d")
                        .daemon(false)
                        .build();
        private static final boolean DEFAULT_DAEMON_THREAD = false;

        private String id = DEFAULT_ID;
        private boolean defaultScheduler = DEFAULT_DEFAULT_SCHEDULER;
        private int threadCap = DEFAULT_THREAD_CAP;
        private int queuedTaskCap = DEFAULT_QUEUED_TASK_CAP;
        private int ttlSeconds = DEFAULT_TTL_SECONDS;
        private ThreadFactoryConfiguration threadFactoryConfiguration = DEFAULT_THREAD_FACTORY_CONFIGURATION;
        private boolean daemonThread = DEFAULT_DAEMON_THREAD;

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

        public Builder threadCap(final int threadCap) {
            this.threadCap = threadCap;
            return this;
        }

        public Builder queuedTaskCap(final int queuedTaskCap) {
            this.queuedTaskCap = queuedTaskCap;
            return this;
        }

        public Builder ttlSeconds(final int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
            return this;
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public Builder daemonThread(final boolean daemonThread) {
            this.daemonThread = daemonThread;
            return this;
        }

        public ElasticSchedulerConfiguration build() {
            return new ElasticSchedulerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link ElasticSchedulerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ElasticSchedulerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration." +
                "ElasticSchedulerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
