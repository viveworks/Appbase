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

package com.viveworks.appbase.core.util.disruptor.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.lmax.disruptor.dsl.ProducerType;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.WaitStrategyConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.agrona.BitUtil;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Configuration for {@link com.viveworks.appbase.core.util.disruptor.DisruptorService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultDisruptorServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultDisruptorServiceConfiguration.Builder.class)
public final class DefaultDisruptorServiceConfiguration implements DisruptorServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultDisruptorServiceConfiguration";

    @NotNull
    private final WaitStrategyConfiguration waitStrategyConfiguration;

    @Positive
    private final int eventHandlerCount;

    @Positive
    private final int ringBufferSize;

    @NotNull
    private final ProducerType producerType;

    @NotNull
    private final ThreadFactoryConfiguration threadFactoryConfiguration;

    private DefaultDisruptorServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultDisruptorServiceConfigurationValidator.validate(
                builder.waitStrategyConfiguration,
                builder.eventHandlerCount,
                builder.ringBufferSize,
                builder.producerType,
                builder.threadFactoryConfiguration);

        waitStrategyConfiguration = builder.waitStrategyConfiguration;
        eventHandlerCount = builder.eventHandlerCount;
        ringBufferSize = BitUtil.findNextPositivePowerOfTwo(builder.ringBufferSize);
        producerType = builder.producerType;
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
    public WaitStrategyConfiguration getWaitStrategyConfiguration() {
        return waitStrategyConfiguration;
    }

    @Override
    public int getEventHandlerCount() {
        return eventHandlerCount;
    }

    @Override
    public int getRingBufferSize() {
        return ringBufferSize;
    }

    @Override
    public ProducerType getProducerType() {
        return producerType;
    }

    @Override
    public ThreadFactoryConfiguration getThreadFactoryConfiguration() {
        return threadFactoryConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultDisruptorServiceConfiguration)) {
            return false;
        }

        final DefaultDisruptorServiceConfiguration that = (DefaultDisruptorServiceConfiguration) o;
        return eventHandlerCount == that.eventHandlerCount &&
                ringBufferSize == that.ringBufferSize &&
                Objects.equals(waitStrategyConfiguration, that.waitStrategyConfiguration) &&
                producerType == that.producerType &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                waitStrategyConfiguration,
                eventHandlerCount,
                ringBufferSize,
                producerType,
                threadFactoryConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultDisruptorServiceConfiguration{" +
                "waitStrategyConfiguration=" + waitStrategyConfiguration +
                ", eventHandlerCount=" + eventHandlerCount +
                ", ringBufferSize=" + ringBufferSize +
                ", producerType=" + producerType +
                ", threadFactoryConfiguration=" + threadFactoryConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultDisruptorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final WaitStrategyConfiguration DEFAULT_WAIT_STRATEGY_CONFIGURATION =
                SleepingWaitStrategyConfiguration.builder().build();
        private static final int DEFAULT_EVENT_HANDLER_COUNT = 1;
        private static final int DEFAULT_RING_BUFFER_SIZE = 1024;
        private static final ProducerType DEFAULT_PRODUCER_TYPE = ProducerType.MULTI;
        private static final ThreadFactoryConfiguration DEFAULT_THREAD_CONFIGURATION = ThreadFactoryConfiguration
                .builder()
                .nameFormat("disruptor-thread-%d")
                .daemon(false)
                .enableThreadAffinity(true)
                .build();

        private WaitStrategyConfiguration waitStrategyConfiguration = DEFAULT_WAIT_STRATEGY_CONFIGURATION;
        private int eventHandlerCount = DEFAULT_EVENT_HANDLER_COUNT;
        private int ringBufferSize = DEFAULT_RING_BUFFER_SIZE;
        private ProducerType producerType = DEFAULT_PRODUCER_TYPE;
        private ThreadFactoryConfiguration threadFactoryConfiguration = DEFAULT_THREAD_CONFIGURATION;

        private Builder() {
        }

        public Builder waitStrategyConfiguration(final WaitStrategyConfiguration waitStrategyConfiguration) {
            this.waitStrategyConfiguration = waitStrategyConfiguration;
            return this;
        }

        public Builder eventHandlerCount(final int eventHandlerCount) {
            this.eventHandlerCount = eventHandlerCount;
            return this;
        }

        public Builder ringBufferSize(final int ringBufferSize) {
            this.ringBufferSize = ringBufferSize;
            return this;
        }

        public Builder producerType(final ProducerType producerType) {
            this.producerType = producerType;
            return this;
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public DefaultDisruptorServiceConfiguration build() {
            return new DefaultDisruptorServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultDisruptorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultDisruptorServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
