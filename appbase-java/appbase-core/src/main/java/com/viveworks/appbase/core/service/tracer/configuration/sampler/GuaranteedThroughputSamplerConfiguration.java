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
 * Configuration for {@link io.jaegertracing.internal.samplers.GuaranteedThroughputSampler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@GuaranteedThroughputSamplerConfiguration.Valid
@JsonDeserialize(builder = GuaranteedThroughputSamplerConfiguration.Builder.class)
public final class GuaranteedThroughputSamplerConfiguration implements JaegerSamplerConfiguration {
    private final double samplingRate;
    private final double lowerBound;

    private GuaranteedThroughputSamplerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        GuaranteedThroughputSamplerConfigurationValidator.validate(
                builder.samplingRate,
                builder.lowerBound);

        samplingRate = builder.samplingRate;
        lowerBound = builder.lowerBound;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GuaranteedThroughputSamplerConfiguration that = (GuaranteedThroughputSamplerConfiguration) o;
        return Objects.equals(samplingRate, that.samplingRate) &&
                Objects.equals(lowerBound, that.lowerBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(samplingRate, lowerBound);
    }

    @Override
    public String toString() {
        return "GuaranteedThroughputSamplerConfiguration{" +
                "samplingRate=" + samplingRate +
                ", lowerBound=" + lowerBound +
                '}';
    }

    /**
     * {@link GuaranteedThroughputSamplerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final double DEFAULT_SAMPLING_RATE = 0.001;
        private static final double DEFAULT_LOWER_BOUND = 100;

        private double samplingRate = DEFAULT_SAMPLING_RATE;
        private double lowerBound = DEFAULT_LOWER_BOUND;

        private Builder() {
        }

        public Builder samplingRate(final double samplingRate) {
            this.samplingRate = samplingRate;
            return this;
        }

        public Builder lowerBound(final double lowerBound) {
            this.lowerBound = lowerBound;
            return this;
        }

        public GuaranteedThroughputSamplerConfiguration build() {
            return new GuaranteedThroughputSamplerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link GuaranteedThroughputSamplerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = GuaranteedThroughputSamplerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.tracer.configuration.sampler." +
                "GuaranteedThroughputSamplerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
