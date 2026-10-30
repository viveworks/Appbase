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

package com.viveworks.appbase.core.service.mediator.queues;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
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
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultMessageQueues}.
 *
 * <blockquote>
 *     <p>
 *         <b>NOTE:</b> The default values are defined in {@link Builder}.
 *     </p>
 * </blockquote>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultMessageQueuesConfiguration.Valid
@JsonDeserialize(builder = DefaultMessageQueuesConfiguration.Builder.class)
@SuppressWarnings("WeakerAccess")
public final class DefaultMessageQueuesConfiguration {
    private final MessageQueueType messageQueueType;
    private final int boundMessageQueueCapacity;
    private final boolean removeExpiredMessages;
    private final long expiredMessageThresholdMillis;
    private final long messageQueuesCleanerInitialDelayMillis;
    private final long messageQueuesCleanerIntervalMillis;

    private DefaultMessageQueuesConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultMessageQueuesConfigurationValidator.validate(
                builder.messageQueueType,
                builder.boundMessageQueueCapacity,
                builder.removeExpiredMessages,
                builder.expiredMessageThresholdMillis,
                builder.messageQueuesCleanerInitialDelayMillis,
                builder.messageQueuesCleanerIntervalMillis);

        messageQueueType = builder.messageQueueType;
        boundMessageQueueCapacity = builder.boundMessageQueueCapacity;
        removeExpiredMessages = builder.removeExpiredMessages;
        expiredMessageThresholdMillis = builder.expiredMessageThresholdMillis;
        messageQueuesCleanerInitialDelayMillis = builder.messageQueuesCleanerInitialDelayMillis;
        messageQueuesCleanerIntervalMillis = builder.messageQueuesCleanerIntervalMillis;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public MessageQueueType getMessageQueueType() {
        return messageQueueType;
    }

    public int getBoundMessageQueueCapacity() {
        return boundMessageQueueCapacity;
    }

    public boolean isRemoveExpiredMessages() {
        return removeExpiredMessages;
    }

    public long getExpiredMessageThresholdMillis() {
        return expiredMessageThresholdMillis;
    }

    public long getMessageQueuesCleanerInitialDelayMillis() {
        return messageQueuesCleanerInitialDelayMillis;
    }

    public long getMessageQueuesCleanerIntervalMillis() {
        return messageQueuesCleanerIntervalMillis;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultMessageQueuesConfiguration that = (DefaultMessageQueuesConfiguration) o;
        return boundMessageQueueCapacity == that.boundMessageQueueCapacity &&
                removeExpiredMessages == that.removeExpiredMessages &&
                expiredMessageThresholdMillis == that.expiredMessageThresholdMillis &&
                messageQueuesCleanerInitialDelayMillis == that.messageQueuesCleanerInitialDelayMillis &&
                messageQueuesCleanerIntervalMillis == that.messageQueuesCleanerIntervalMillis &&
                messageQueueType == that.messageQueueType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                messageQueueType,
                boundMessageQueueCapacity,
                removeExpiredMessages,
                expiredMessageThresholdMillis,
                messageQueuesCleanerInitialDelayMillis,
                messageQueuesCleanerIntervalMillis);
    }

    @Override
    public String toString() {
        return "DefaultMessageQueuesConfiguration{" +
                "messageQueueType=" + messageQueueType +
                ", boundMessageQueueCapacity=" + boundMessageQueueCapacity +
                ", removeExpiredMessages=" + removeExpiredMessages +
                ", expiredMessageThresholdMillis=" + expiredMessageThresholdMillis +
                ", messageQueuesCleanerInitialDelayMillis=" + messageQueuesCleanerInitialDelayMillis +
                ", messageQueuesCleanerIntervalMillis=" + messageQueuesCleanerIntervalMillis +
                '}';
    }


    /**
     * {@link DefaultMessageQueuesConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final MessageQueueType DEFAULT_MESSAGE_QUEUE_TYPE = MessageQueueType.BOUND;
        private static final int DEFAULT_BOUND_MESSAGE_QUEUE_CAPACITY = 256;
        private static final boolean DEFAULT_REMOVE_EXPIRED_MESSAGES = true;
        private static final long DEFAULT_EXPIRED_MESSAGE_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(5);
        private static final long DEFAULT_MESSAGE_QUEUES_CLEANER_INITIAL_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(5);
        private static final long DEFAULT_MESSAGE_QUEUES_CLEANER_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(10);

        private MessageQueueType messageQueueType = DEFAULT_MESSAGE_QUEUE_TYPE;
        private int boundMessageQueueCapacity = DEFAULT_BOUND_MESSAGE_QUEUE_CAPACITY;
        private boolean removeExpiredMessages = DEFAULT_REMOVE_EXPIRED_MESSAGES;
        private long expiredMessageThresholdMillis = DEFAULT_EXPIRED_MESSAGE_THRESHOLD_MILLIS;
        private long messageQueuesCleanerInitialDelayMillis = DEFAULT_MESSAGE_QUEUES_CLEANER_INITIAL_DELAY_MILLIS;
        private long messageQueuesCleanerIntervalMillis = DEFAULT_MESSAGE_QUEUES_CLEANER_INTERVAL_MILLIS;

        private Builder() {
        }

        public Builder messageQueueType(final MessageQueueType messageQueueType) {
            this.messageQueueType = messageQueueType;
            return this;
        }

        public Builder boundMessageQueueCapacity(final int boundMessageQueueCapacity) {
            this.boundMessageQueueCapacity = boundMessageQueueCapacity;
            return this;
        }

        public Builder removeExpiredMessages(final boolean removeExpiredMessages) {
            this.removeExpiredMessages = removeExpiredMessages;
            return this;
        }

        public Builder expiredMessageThresholdMillis(final long expiredMessageThresholdMillis) {
            this.expiredMessageThresholdMillis = expiredMessageThresholdMillis;
            return this;
        }

        public Builder messageQueuesCleanerInitialDelayMillis(final long messageQueuesCleanerInitialDelayMillis) {
            this.messageQueuesCleanerInitialDelayMillis = messageQueuesCleanerInitialDelayMillis;
            return this;
        }

        public Builder messageQueuesCleanerIntervalMillis(final long messageQueuesCleanerIntervalMillis) {
            this.messageQueuesCleanerIntervalMillis = messageQueuesCleanerIntervalMillis;
            return this;
        }

        public DefaultMessageQueuesConfiguration build() {
            return new DefaultMessageQueuesConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultMessageQueuesConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultMessageQueuesConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueuesConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
