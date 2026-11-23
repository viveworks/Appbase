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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueuesConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultMediatorService}.
 *
 * <blockquote>
 *     <p>
 *         <b>NOTE:</b> The default values are defined in {@link Builder}.
 *     </p>
 * </blockquote>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@DefaultMediatorServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultMediatorServiceConfiguration.Builder.class)
@SuppressFBWarnings("WOC_WRITE_ONLY_COLLECTION_FIELD")
public final class DefaultMediatorServiceConfiguration implements MediatorServiceConfiguration {
    @Nullable
    private final Set<MessageType> ignoredMessageTypeSet;

    @Positive
    private final int messageObjectPoolCapacity;

    @Positive
    private final int removedMessageQueueCapacity;

    @Positive
    private final int appbaseRuntimeExceptionQueueCapacity;

    @NotNull
    private final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration;

    @NotNull
    private final ExecutorServiceConfiguration sharedExecutorServiceConfiguration;

    @NotNull
    private final ScheduledExecutorServiceConfiguration sharedScheduledExecutorServiceConfiguration;

    private DefaultMediatorServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultMediatorServiceConfigurationValidator.validate(
                builder.messageobjectPoolCapacity,
                builder.removedMessageQueueCapacity,
                builder.appbaseRuntimeExceptionQueueCapacity,
                builder.defaultMessageQueuesConfiguration,
                builder.sharedExecutorServiceConfiguration,
                builder.sharedScheduledExecutorServiceConfiguration);

        if (builder.ignoredMessageTypeSet != null) {
            ignoredMessageTypeSet = new HashSet<>(builder.ignoredMessageTypeSet);
        } else {
            ignoredMessageTypeSet = Set.of();
        }

        messageObjectPoolCapacity = builder.messageobjectPoolCapacity;
        removedMessageQueueCapacity = builder.removedMessageQueueCapacity;
        appbaseRuntimeExceptionQueueCapacity = builder.appbaseRuntimeExceptionQueueCapacity;
        defaultMessageQueuesConfiguration = builder.defaultMessageQueuesConfiguration;
        sharedExecutorServiceConfiguration = builder.sharedExecutorServiceConfiguration;
        sharedScheduledExecutorServiceConfiguration = builder.sharedScheduledExecutorServiceConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public Set<MessageType> getIgnoredMessageTypeSet() {
        return ignoredMessageTypeSet;
    }

    public int getMessageObjectPoolCapacity() {
        return messageObjectPoolCapacity;
    }

    public int getRemovedMessageQueueCapacity() {
        return removedMessageQueueCapacity;
    }

    public int getAppbaseRuntimeExceptionQueueCapacity() {
        return appbaseRuntimeExceptionQueueCapacity;
    }

    public DefaultMessageQueuesConfiguration getDefaultMessageQueuesConfiguration() {
        return defaultMessageQueuesConfiguration;
    }

    public ExecutorServiceConfiguration getSharedExecutorServiceConfiguration() {
        return sharedExecutorServiceConfiguration;
    }

    public ScheduledExecutorServiceConfiguration getSharedScheduledExecutorServiceConfiguration() {
        return sharedScheduledExecutorServiceConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultMediatorServiceConfiguration that = (DefaultMediatorServiceConfiguration) o;
        return messageObjectPoolCapacity == that.messageObjectPoolCapacity &&
                removedMessageQueueCapacity == that.removedMessageQueueCapacity &&
                appbaseRuntimeExceptionQueueCapacity == that.appbaseRuntimeExceptionQueueCapacity &&
                Objects.equals(ignoredMessageTypeSet, that.ignoredMessageTypeSet) &&
                Objects.equals(defaultMessageQueuesConfiguration, that.defaultMessageQueuesConfiguration) &&
                Objects.equals(sharedExecutorServiceConfiguration, that.sharedExecutorServiceConfiguration) &&
                Objects.equals(
                        sharedScheduledExecutorServiceConfiguration,
                        that.sharedScheduledExecutorServiceConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                ignoredMessageTypeSet,
                messageObjectPoolCapacity,
                removedMessageQueueCapacity,
                appbaseRuntimeExceptionQueueCapacity,
                defaultMessageQueuesConfiguration,
                sharedExecutorServiceConfiguration,
                sharedScheduledExecutorServiceConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultMediatorServiceConfiguration{" +
                "ignoredMessageTypeSet=" + ignoredMessageTypeSet +
                ", messageObjectPoolCapacity=" + messageObjectPoolCapacity +
                ", removedMessageQueueCapacity=" + removedMessageQueueCapacity +
                ", appbaseRuntimeExceptionQueueCapacity=" + appbaseRuntimeExceptionQueueCapacity +
                ", defaultMessageQueuesConfiguration=" + defaultMessageQueuesConfiguration +
                ", sharedExecutorServiceConfiguration=" + sharedExecutorServiceConfiguration +
                ", sharedScheduledExecutorServiceConfiguration=" + sharedScheduledExecutorServiceConfiguration +
                '}';
    }

    /**
     * {@link DefaultMediatorServiceConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Set<MessageType> DEFAULT_IGNORED_MESSAGE_TYPE_SET = Set.of();
        private static final int DEFAULT_MESSAGE_OBJECT_POOL_CAPACITY = 256;
        private static final int DEFAULT_REMOVED_MESSAGE_QUEUE_CAPACITY = 128;
        private static final int DEFAULT_APPBASE_RUNTIME_EXCEPTION_QUEUE_CAPACITY = 128;
        private static final DefaultMessageQueuesConfiguration DEFAULT_MESSAGE_QUEUES_CONFIGURATION =
                DefaultMessageQueuesConfiguration.builder().build();
        private static final ExecutorServiceConfiguration DEFAULT_SHARED_EXECUTOR_SERVICE_CONFIGURATION =
                ExecutorServiceConfiguration.builder().build();
        private static final ScheduledExecutorServiceConfiguration
                DEFAULT_SHARED_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION =
                    ScheduledExecutorServiceConfiguration.builder().build();

        private Set<MessageType> ignoredMessageTypeSet = DEFAULT_IGNORED_MESSAGE_TYPE_SET;
        private int messageobjectPoolCapacity = DEFAULT_MESSAGE_OBJECT_POOL_CAPACITY;
        private int removedMessageQueueCapacity = DEFAULT_REMOVED_MESSAGE_QUEUE_CAPACITY;
        private int appbaseRuntimeExceptionQueueCapacity = DEFAULT_APPBASE_RUNTIME_EXCEPTION_QUEUE_CAPACITY;
        private DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration =
                DEFAULT_MESSAGE_QUEUES_CONFIGURATION;
        private ExecutorServiceConfiguration sharedExecutorServiceConfiguration =
                DEFAULT_SHARED_EXECUTOR_SERVICE_CONFIGURATION;
        private ScheduledExecutorServiceConfiguration sharedScheduledExecutorServiceConfiguration =
                DEFAULT_SHARED_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION;

        private Builder() {
        }

        public Builder ignoredMessageTypeSet(final Set<MessageType> ignoredMessageTypeSet) {
            this.ignoredMessageTypeSet = ignoredMessageTypeSet;
            return this;
        }

        public Builder messageObjectPoolCapacity(final int messageObjectPoolCapacity) {
            this.messageobjectPoolCapacity = messageObjectPoolCapacity;
            return this;
        }

        @JsonProperty("removedMessageQueueCapacity")
        public Builder droppedMessageQueueCapacity(final int removedMessageQueueCapacity) {
            this.removedMessageQueueCapacity = removedMessageQueueCapacity;
            return this;
        }

        public Builder appbaseRuntimeExceptionQueueCapacity(final int appbaseRuntimeExceptionQueueCapacity) {
            this.appbaseRuntimeExceptionQueueCapacity = appbaseRuntimeExceptionQueueCapacity;
            return this;
        }

        @JsonProperty("defaultMessageQueuesConfiguration")
        public Builder messageQueuesConfiguration(
                final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration)
        {
            this.defaultMessageQueuesConfiguration = defaultMessageQueuesConfiguration;
            return this;
        }

        public Builder sharedExecutorServiceConfiguration(
                final ExecutorServiceConfiguration sharedExecutorServiceConfiguration)
        {
            this.sharedExecutorServiceConfiguration = sharedExecutorServiceConfiguration;
            return this;
        }

        public Builder sharedScheduledExecutorServiceConfiguration(
                final ScheduledExecutorServiceConfiguration sharedScheduledExecutorServiceConfiguration)
        {
            this.sharedScheduledExecutorServiceConfiguration = sharedScheduledExecutorServiceConfiguration;
            return this;
        }

        public DefaultMediatorServiceConfiguration build() {
            return new DefaultMediatorServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultMediatorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultMediatorServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
