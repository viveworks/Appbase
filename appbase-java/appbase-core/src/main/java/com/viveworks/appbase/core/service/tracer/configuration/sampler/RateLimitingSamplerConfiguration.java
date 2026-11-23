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

package com.viveworks.appbase.core.service.tracer.configuration.sampler;

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

/**
 * Configuration for {@link io.jaegertracing.internal.samplers.RateLimitingSampler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RateLimitingSamplerConfiguration.Builder.class)
public final class RateLimitingSamplerConfiguration implements JaegerSamplerConfiguration {
    private final double maxTracesPerSecond;

    private RateLimitingSamplerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        RateLimitingSamplerConfigurationValidator.validate(builder.maxTracesPerSecond);

        maxTracesPerSecond = builder.maxTracesPerSecond;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public double getMaxTracesPerSecond() {
        return maxTracesPerSecond;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RateLimitingSamplerConfiguration that = (RateLimitingSamplerConfiguration) o;
        return Objects.equals(maxTracesPerSecond, that.maxTracesPerSecond);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxTracesPerSecond);
    }

    @Override
    public String toString() {
        return "RateLimitingSamplerConfiguration{" +
                "maxTracesPerSecond=" + maxTracesPerSecond +
                '}';
    }

    /**
     * {@link RateLimitingSamplerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final double DEFAULT_MAX_TRACES_PER_SECOND = 2000;

        private double maxTracesPerSecond = DEFAULT_MAX_TRACES_PER_SECOND;

        private Builder() {
        }

        public Builder maxTracesPerSecond(final double maxTracesPerSecond) {
            this.maxTracesPerSecond = maxTracesPerSecond;
            return this;
        }

        public RateLimitingSamplerConfiguration build() {
            return new RateLimitingSamplerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link RateLimitingSamplerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = RateLimitingSamplerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.tracer.configuration.sampler." +
                "RateLimitingSamplerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
