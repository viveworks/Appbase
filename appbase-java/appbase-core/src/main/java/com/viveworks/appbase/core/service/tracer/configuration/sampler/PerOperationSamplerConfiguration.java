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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for {@link io.jaegertracing.internal.samplers.PerOperationSampler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@PerOperationSamplerConfiguration.Valid
@JsonDeserialize(builder = PerOperationSamplerConfiguration.Builder.class)
public final class PerOperationSamplerConfiguration implements JaegerSamplerConfiguration {
    private final int maxOperations;
    private final double defaultSamplingProbability;
    private final double defaultLowerBoundTracesPerSecond;
    @JsonSerialize(keyAs = String.class, contentAs = Double.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Double.class)
    private final Map<String, Double> samplingRateByOperationNameMap;

    private PerOperationSamplerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        PerOperationSamplerConfigurationValidator.validate(
                builder.maxOperations,
                builder.defaultSamplingProbability,
                builder.defaultLowerBoundTracesPerSecond,
                builder.samplingRateByOperationNameMap);

        maxOperations = builder.maxOperations;
        defaultSamplingProbability = builder.defaultSamplingProbability;
        defaultLowerBoundTracesPerSecond = builder.defaultLowerBoundTracesPerSecond;
        samplingRateByOperationNameMap = builder.samplingRateByOperationNameMap;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getMaxOperations() {
        return maxOperations;
    }

    public double getDefaultSamplingProbability() {
        return defaultSamplingProbability;
    }

    public double getDefaultLowerBoundTracesPerSecond() {
        return defaultLowerBoundTracesPerSecond;
    }

    public Map<String, Double> getSamplingRateByOperationNameMap() {
        return samplingRateByOperationNameMap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PerOperationSamplerConfiguration that = (PerOperationSamplerConfiguration) o;
        return Objects.equals(maxOperations, that.maxOperations) &&
                Objects.equals(defaultSamplingProbability, that.defaultSamplingProbability) &&
                Objects.equals(defaultLowerBoundTracesPerSecond, that.defaultLowerBoundTracesPerSecond) &&
                Objects.equals(samplingRateByOperationNameMap, that.samplingRateByOperationNameMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                maxOperations,
                defaultSamplingProbability,
                defaultLowerBoundTracesPerSecond,
                samplingRateByOperationNameMap);
    }

    @Override
    public String toString() {
        return "PerOperationSamplerConfiguration{" +
                "maxOperations=" + maxOperations +
                ", defaultSamplingProbability=" + defaultSamplingProbability +
                ", defaultLowerBoundTracesPerSecond=" + defaultLowerBoundTracesPerSecond +
                ", samplingRateByOperationNameMap=" + samplingRateByOperationNameMap +
                '}';
    }

    /**
     * {@link PerOperationSamplerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL", "DMC_DUBIOUS_MAP_COLLECTION"})
    public static final class Builder {
        private static final int DEFAULT_MAX_OPERATIONS = 2000;
        private static final double DEFAULT_DEFAULT_SAMPLING_PROBABILITY = 0.001;
        private static final double DEFAULT_LOWER_BOUND_TRACES_PER_SECOND = 500;
        private static final Map<String, Double> DEFAULT_SAMPLING_RATE_BY_OPERATION_NAME_MAP = Map.of();

        private int maxOperations = DEFAULT_MAX_OPERATIONS;

        private double defaultSamplingProbability = DEFAULT_DEFAULT_SAMPLING_PROBABILITY;

        private double defaultLowerBoundTracesPerSecond = DEFAULT_LOWER_BOUND_TRACES_PER_SECOND;

        @JsonSerialize(keyAs = String.class, contentAs = Double.class)
        @JsonDeserialize(keyAs = String.class, contentAs = Double.class)
        private Map<String, Double> samplingRateByOperationNameMap = DEFAULT_SAMPLING_RATE_BY_OPERATION_NAME_MAP;

        private Builder() {
        }

        public Builder maxOperations(final int maxOperations) {
            this.maxOperations = maxOperations;
            return this;
        }

        public Builder defaultSamplingProbability(final double defaultSamplingProbability) {
            this.defaultSamplingProbability = defaultSamplingProbability;
            return this;
        }

        public Builder defaultLowerBoundTracesPerSecond(final double defaultLowerBoundTracesPerSecond) {
            this.defaultLowerBoundTracesPerSecond = defaultLowerBoundTracesPerSecond;
            return this;
        }

        public Builder samplingRateByOperationNameMap(
                @JsonSerialize(keyAs = String.class, contentAs = Double.class)
                @JsonDeserialize(keyAs = String.class, contentAs = Double.class)
                final Map<String, Double> samplingRateByOperationNameMap)
        {
            this.samplingRateByOperationNameMap = samplingRateByOperationNameMap;
            return this;
        }

        public PerOperationSamplerConfiguration build() {
            return new PerOperationSamplerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link PerOperationSamplerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = PerOperationSamplerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.tracer.configuration.sampler." +
                "PerOperationSamplerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
