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

package com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
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
 * Configuration for {@link TimeoutBlockingWaitStrategy}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@YieldingWaitStrategyConfiguration.Valid
@JsonDeserialize(builder = YieldingWaitStrategyConfiguration.Builder.class)
public final class YieldingWaitStrategyConfiguration implements WaitStrategyConfiguration {
    private final boolean enabled;

    private YieldingWaitStrategyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        enabled = builder.enabled;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final YieldingWaitStrategyConfiguration that = (YieldingWaitStrategyConfiguration) o;
        return enabled == that.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled);
    }

    @Override
    public String toString() {
        return "YieldingWaitStrategyConfiguration{" +
                "enabled=" + enabled +
                '}';
    }

    /**
     * Builder for {@link YieldingWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;

        private boolean enabled = DEFAULT_ENABLED;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public YieldingWaitStrategyConfiguration build() {
            return new YieldingWaitStrategyConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link YieldingWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = YieldingWaitStrategyConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy." +
                "YieldingWaitStrategyConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
