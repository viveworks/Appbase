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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.firebase.app.CustomFirebaseThreadManager}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = CustomFirebaseThreadManagerConfiguration.Builder.class)
public final class CustomFirebaseThreadManagerConfiguration {
    private final ThreadFactoryConfiguration threadFactoryConfiguration;

    private final boolean useSharedExecutorService;

    @Nullable
    private final ExecutorServiceConfiguration executorServiceConfiguration;

    private CustomFirebaseThreadManagerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.threadFactoryConfiguration, "builder.threadFactoryConfiguration");

        threadFactoryConfiguration = builder.threadFactoryConfiguration;
        useSharedExecutorService = builder.useSharedExecutorService;
        executorServiceConfiguration = builder.executorServiceConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public ThreadFactoryConfiguration getThreadFactoryConfiguration() {
        return threadFactoryConfiguration;
    }

    public boolean isUseSharedExecutorService() {
        return useSharedExecutorService;
    }

    @Nullable
    public ExecutorServiceConfiguration getExecutorServiceConfiguration() {
        return executorServiceConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CustomFirebaseThreadManagerConfiguration that = (CustomFirebaseThreadManagerConfiguration) o;
        return useSharedExecutorService == that.useSharedExecutorService &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration) &&
                Objects.equals(executorServiceConfiguration, that.executorServiceConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threadFactoryConfiguration, useSharedExecutorService, executorServiceConfiguration);
    }

    @Override
    public String toString() {
        return "CustomFirebaseThreadManagerConfiguration{" +
                "threadFactoryConfiguration=" + threadFactoryConfiguration +
                ", useSharedExecutorService=" + useSharedExecutorService +
                ", executorServiceConfiguration=" + executorServiceConfiguration +
                '}';
    }


    /**
     * Builder for {@link CustomFirebaseThreadManagerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final ThreadFactoryConfiguration DEFAULT_THREAD_FACTORY_CONFIGURATION =
                ThreadFactoryConfiguration
                        .builder()
                        .nameFormat("appbase-firebase-%d")
                        .daemon(false)
                        .enableThreadAffinity(true)
                        .build();
        private static final boolean DEFAULT_USE_SHARED_EXECUTOR_SERVICE = true;


        private ThreadFactoryConfiguration threadFactoryConfiguration = DEFAULT_THREAD_FACTORY_CONFIGURATION;
        private boolean useSharedExecutorService = DEFAULT_USE_SHARED_EXECUTOR_SERVICE;
        private ExecutorServiceConfiguration executorServiceConfiguration;


        private Builder() {
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public Builder useSharedExecutorService(final boolean useSharedExecutorService) {
            this.useSharedExecutorService = useSharedExecutorService;
            return this;
        }

        public Builder executorServiceConfiguration(final ExecutorServiceConfiguration executorServiceConfiguration) {
            this.executorServiceConfiguration = executorServiceConfiguration;
            return this;
        }

        public CustomFirebaseThreadManagerConfiguration build() {
            return new CustomFirebaseThreadManagerConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
