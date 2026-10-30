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
 * Configuration for {@code unpooled buffer allocator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = UnpooledBufferAllocatorConfiguration.Builder.class)
public final class UnpooledBufferAllocatorConfiguration {
    private boolean disableLeakDetector;

    @Nullable
    private Boolean tryNoCleaner;

    private UnpooledBufferAllocatorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        disableLeakDetector = builder.disableLeakDetector;
        tryNoCleaner = builder.tryNoCleaner;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isDisableLeakDetector() {
        return disableLeakDetector;
    }

    @Nullable
    public Boolean getTryNoCleaner() {
        return tryNoCleaner;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UnpooledBufferAllocatorConfiguration that = (UnpooledBufferAllocatorConfiguration) o;
        return disableLeakDetector == that.disableLeakDetector &&
                Objects.equals(tryNoCleaner, that.tryNoCleaner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disableLeakDetector, tryNoCleaner);
    }

    @Override
    public String toString() {
        return "UnpooledBufferAllocatorConfiguration{" +
                "disableLeakDetector=" + disableLeakDetector +
                ", tryNoCleaner=" + tryNoCleaner +
                '}';
    }

    /**
     * Builder for {@link UnpooledBufferAllocatorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_DISABLE_LEAK_DETECTOR = true;

        private boolean disableLeakDetector = DEFAULT_DISABLE_LEAK_DETECTOR;

        @Nullable
        private Boolean tryNoCleaner = (Boolean) AppbaseUtil.AUTO_DETECTED_CONFIGURATION_VALUE;

        private Builder() {
        }

        public Builder disableLeakDetector(final boolean disableLeakDetector) {
            this.disableLeakDetector = disableLeakDetector;
            return this;
        }

        public Builder tryNoCleaner(@Nullable final Boolean tryNoCleaner) {
            this.tryNoCleaner = tryNoCleaner;
            return this;
        }

        public UnpooledBufferAllocatorConfiguration build() {
            return new UnpooledBufferAllocatorConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
