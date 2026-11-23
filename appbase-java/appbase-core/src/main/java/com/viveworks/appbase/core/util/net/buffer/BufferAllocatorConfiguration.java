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

package com.viveworks.appbase.core.util.net.buffer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link io.netty.buffer.ByteBufAllocator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = BufferAllocatorConfiguration.Builder.class)
public final class BufferAllocatorConfiguration {
    private final BufferSource bufferSource;
    private final BufferPoolingStrategy bufferPoolingStrategy;
    private final PooledBufferAllocatorConfiguration pooledBufferAllocatorConfiguration;
    private final UnpooledBufferAllocatorConfiguration unpooledBufferAllocatorConfiguration;

    private BufferAllocatorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.bufferSource, "builder.bufferSource");
        AppbaseUtil.notNull(builder.bufferPoolingStrategy, "builder.bufferPoolingStrategy");
        if (builder.pooledBufferAllocatorConfiguration == null &&
                builder.unpooledBufferAllocatorConfiguration == null)
        {
            throw new AppbaseRuntimeException(
                    "One of builder.pooledBufferAllocatorConfiguration or " +
                            "builder.unpooledBufferAllocatorConfiguration must be set");
        }

        bufferSource = builder.bufferSource;
        bufferPoolingStrategy = builder.bufferPoolingStrategy;
        pooledBufferAllocatorConfiguration = builder.pooledBufferAllocatorConfiguration;
        unpooledBufferAllocatorConfiguration = builder.unpooledBufferAllocatorConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public BufferSource getBufferSource() {
        return bufferSource;
    }

    public BufferPoolingStrategy getBufferPoolingStrategy() {
        return bufferPoolingStrategy;
    }

    public PooledBufferAllocatorConfiguration getPooledBufferAllocatorConfiguration() {
        return pooledBufferAllocatorConfiguration;
    }

    public UnpooledBufferAllocatorConfiguration getUnpooledBufferAllocatorConfiguration() {
        return unpooledBufferAllocatorConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BufferAllocatorConfiguration that = (BufferAllocatorConfiguration) o;
        return bufferSource == that.bufferSource &&
                bufferPoolingStrategy == that.bufferPoolingStrategy &&
                Objects.equals(pooledBufferAllocatorConfiguration, that.pooledBufferAllocatorConfiguration) &&
                Objects.equals(unpooledBufferAllocatorConfiguration, that.unpooledBufferAllocatorConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                bufferSource,
                bufferPoolingStrategy,
                pooledBufferAllocatorConfiguration,
                unpooledBufferAllocatorConfiguration);
    }

    @Override
    public String toString() {
        return "BufferAllocatorConfiguration{" +
                "bufferSource=" + bufferSource +
                ", bufferPoolingStrategy=" + bufferPoolingStrategy +
                ", pooledBufferAllocatorConfiguration=" + pooledBufferAllocatorConfiguration +
                ", unpooledBufferAllocatorConfiguration=" + unpooledBufferAllocatorConfiguration +
                '}';
    }

    /**
     * Builder for {@link BufferAllocatorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final BufferSource DEFAULT_BUFFER_SOURCE = BufferSource.HEAP;
        private static final BufferPoolingStrategy DEFAULT_BUFFER_POOLING_STRATEGY =
                BufferPoolingStrategy.UNPOOLED;
        private static final PooledBufferAllocatorConfiguration DEFAULT_POOLED_BUFFER_ALLOCATOR_CONFIGURATION =
                PooledBufferAllocatorConfiguration.builder().build();
        private static final UnpooledBufferAllocatorConfiguration DEFAULT_UNPOOLED_BUFFER_ALLOCATOR_CONFIGURATION =
                UnpooledBufferAllocatorConfiguration.builder().build();

        private BufferSource bufferSource = DEFAULT_BUFFER_SOURCE;
        private BufferPoolingStrategy bufferPoolingStrategy =
                DEFAULT_BUFFER_POOLING_STRATEGY;
        private PooledBufferAllocatorConfiguration pooledBufferAllocatorConfiguration =
                DEFAULT_POOLED_BUFFER_ALLOCATOR_CONFIGURATION;
        private UnpooledBufferAllocatorConfiguration unpooledBufferAllocatorConfiguration =
                DEFAULT_UNPOOLED_BUFFER_ALLOCATOR_CONFIGURATION;

        private Builder() {
        }

        public Builder bufferSource(final BufferSource bufferSource) {
            this.bufferSource = bufferSource;
            return this;
        }

        public Builder bufferPoolingStrategy(final BufferPoolingStrategy bufferPoolingStrategy) {
            this.bufferPoolingStrategy = bufferPoolingStrategy;
            return this;
        }

        public Builder pooledBufferAllocatorConfiguration(
                final PooledBufferAllocatorConfiguration pooledBufferAllocatorConfiguration)
        {
            this.pooledBufferAllocatorConfiguration = pooledBufferAllocatorConfiguration;
            return this;
        }

        public Builder unpooledBufferAllocatorConfiguration(
                final UnpooledBufferAllocatorConfiguration unpooledBufferAllocatorConfiguration)
        {
            this.unpooledBufferAllocatorConfiguration = unpooledBufferAllocatorConfiguration;
            return this;
        }

        public BufferAllocatorConfiguration build() {
            return new BufferAllocatorConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
