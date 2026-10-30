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

package com.viveworks.appbase.core.util.net.buffer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@code pooled buffer allocator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = PooledBufferAllocatorConfiguration.Builder.class)
public final class PooledBufferAllocatorConfiguration {
    @Nullable
    private final Integer numberOfHeapArenas;

    @Nullable
    private final Integer numberOfDirectArenas;

    @Nullable
    private final Integer pageSize;

    @Nullable
    private final Integer maxOrder;

    @Nullable
    private final Integer smallCacheSize;

    @Nullable
    private final Integer normalCacheSize;

    @Nullable
    private final Boolean useCacheForAllThreads;

    @Nullable
    private final Integer directMemoryCacheAlignment;

    private PooledBufferAllocatorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        numberOfHeapArenas = builder.numberOfHeapArenas;
        numberOfDirectArenas = builder.numberOfDirectArenas;
        pageSize = builder.pageSize;
        maxOrder = builder.maxOrder;
        smallCacheSize = builder.smallCacheSize;
        normalCacheSize = builder.normalCacheSize;
        useCacheForAllThreads = builder.useCacheForAllThreads;
        directMemoryCacheAlignment = builder.directMemoryCacheAlignment;
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
    public Integer getNumberOfHeapArenas() {
        return numberOfHeapArenas;
    }

    @Nullable
    public Integer getNumberOfDirectArenas() {
        return numberOfDirectArenas;
    }

    @Nullable
    public Integer getPageSize() {
        return pageSize;
    }

    @Nullable
    public Integer getMaxOrder() {
        return maxOrder;
    }

    @Nullable
    public Integer getSmallCacheSize() {
        return smallCacheSize;
    }

    @Nullable
    public Integer getNormalCacheSize() {
        return normalCacheSize;
    }

    @Nullable
    public Boolean getUseCacheForAllThreads() {
        return useCacheForAllThreads;
    }

    @Nullable
    public Integer getDirectMemoryCacheAlignment() {
        return directMemoryCacheAlignment;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PooledBufferAllocatorConfiguration that = (PooledBufferAllocatorConfiguration) o;
        return Objects.equals(numberOfHeapArenas, that.numberOfHeapArenas) &&
                Objects.equals(numberOfDirectArenas, that.numberOfDirectArenas) &&
                Objects.equals(pageSize, that.pageSize) &&
                Objects.equals(maxOrder, that.maxOrder) &&
                Objects.equals(smallCacheSize, that.smallCacheSize) &&
                Objects.equals(normalCacheSize, that.normalCacheSize) &&
                Objects.equals(useCacheForAllThreads, that.useCacheForAllThreads) &&
                Objects.equals(directMemoryCacheAlignment, that.directMemoryCacheAlignment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                numberOfHeapArenas,
                numberOfDirectArenas,
                pageSize,
                maxOrder,
                smallCacheSize,
                normalCacheSize,
                useCacheForAllThreads,
                directMemoryCacheAlignment);
    }

    @Override
    public String toString() {
        return "PooledBufferAllocatorConfiguration{" +
                "numberOfHeapArenas=" + numberOfHeapArenas +
                ", numberOfDirectArenas=" + numberOfDirectArenas +
                ", pageSize=" + pageSize +
                ", maxOrder=" + maxOrder +
                ", smallCacheSize=" + smallCacheSize +
                ", normalCacheSize=" + normalCacheSize +
                ", useCacheForAllThreads=" + useCacheForAllThreads +
                ", directMemoryCacheAlignment=" + directMemoryCacheAlignment +
                '}';
    }

    /**
     * Builder for {@link PooledBufferAllocatorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        @Nullable
        private Integer numberOfHeapArenas = (Integer) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        @Nullable
        private Integer numberOfDirectArenas = (Integer) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        @Nullable
        private Integer pageSize = (Integer) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        @Nullable
        private Integer maxOrder = (Integer) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        @Nullable
        private Integer smallCacheSize = (Integer) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        @Nullable
        private Integer normalCacheSize = (Integer) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        @Nullable
        private Boolean useCacheForAllThreads = (Boolean) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        @Nullable
        private Integer directMemoryCacheAlignment = (Integer) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        private Builder() {
        }

        public Builder numberOfHeapArenas(@Nullable final Integer numberOfHeapArenas) {
            this.numberOfHeapArenas = numberOfHeapArenas;
            return this;
        }

        public Builder numberOfDirectArenas(@Nullable final Integer numberOfDirectArenas) {
            this.numberOfDirectArenas = numberOfDirectArenas;
            return this;
        }

        public Builder pageSize(@Nullable final Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder maxOrder(@Nullable final Integer maxOrder) {
            this.maxOrder = maxOrder;
            return this;
        }

        public Builder smallCacheSize(@Nullable final Integer smallCacheSize) {
            this.smallCacheSize = smallCacheSize;
            return this;
        }

        public Builder normalCacheSize(@Nullable final Integer normalCacheSize) {
            this.normalCacheSize = normalCacheSize;
            return this;
        }

        public Builder useCacheForAllThreads(@Nullable final Boolean useCacheForAllThreads) {
            this.useCacheForAllThreads = useCacheForAllThreads;
            return this;
        }

        public Builder directMemoryCacheAlignment(@Nullable final Integer directMemoryCacheAlignment) {
            this.directMemoryCacheAlignment = directMemoryCacheAlignment;
            return this;
        }

        public PooledBufferAllocatorConfiguration build() {
            return new PooledBufferAllocatorConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
