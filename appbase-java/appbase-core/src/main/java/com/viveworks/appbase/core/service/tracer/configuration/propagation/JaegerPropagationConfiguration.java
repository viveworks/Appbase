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

package com.viveworks.appbase.core.service.tracer.configuration.propagation;

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
import java.util.Objects;
import java.util.Set;

/**
 * Represents Jaeger propagation configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JaegerPropagationConfiguration.Valid
@JsonDeserialize(builder = JaegerPropagationConfiguration.Builder.class)
public final class JaegerPropagationConfiguration {
    @JsonSerialize(contentAs = JaegerPropagationType.class)
    @JsonDeserialize(contentAs = JaegerPropagationType.class)
    private final Set<JaegerPropagationType> jaegerPropagationTypes;

    private final String baggagePrefix;

    private JaegerPropagationConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        JaegerPropagationConfigurationValidator.validate(
                builder.jaegerPropagationTypes,
                builder.baggagePrefix);

        jaegerPropagationTypes = builder.jaegerPropagationTypes;
        baggagePrefix = builder.baggagePrefix;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Set<JaegerPropagationType> getJaegerPropagationTypes() {
        return jaegerPropagationTypes;
    }

    public String getBaggagePrefix() {
        return baggagePrefix;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JaegerPropagationConfiguration that = (JaegerPropagationConfiguration) o;
        return Objects.equals(jaegerPropagationTypes, that.jaegerPropagationTypes) &&
                Objects.equals(baggagePrefix, that.baggagePrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jaegerPropagationTypes, baggagePrefix);
    }

    @Override
    public String toString() {
        return "JaegerPropagationConfiguration{" +
                "jaegerPropagationTypes=" + jaegerPropagationTypes +
                ", baggagePrefix='" + baggagePrefix + '\'' +
                '}';
    }

    /**
     * {@link JaegerPropagationConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Set<JaegerPropagationType> DEFAULT_JAEGER_PROPAGATION_TYPES =
                Set.of(JaegerPropagationType.W3C_TRACE_CONTEXT);
        private static final String DEFAULT_BAGGAGE_PREFIX = "baggage-";

        @JsonSerialize(contentAs = JaegerPropagationType.class)
        @JsonDeserialize(contentAs = JaegerPropagationType.class)
        private Set<JaegerPropagationType> jaegerPropagationTypes = DEFAULT_JAEGER_PROPAGATION_TYPES;

        private String baggagePrefix = DEFAULT_BAGGAGE_PREFIX;

        private Builder() {
        }

        public Builder jaegerPropagationTypes(
                @JsonSerialize(contentAs = JaegerPropagationType.class)
                @JsonDeserialize(contentAs = JaegerPropagationType.class)
                final Set<JaegerPropagationType> jaegerPropagationTypes)
        {
            this.jaegerPropagationTypes = jaegerPropagationTypes;
            return this;
        }

        public Builder baggagePrefix(final String baggagePrefix) {
            this.baggagePrefix = baggagePrefix;
            return this;
        }

        public JaegerPropagationConfiguration build() {
            return new JaegerPropagationConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link JaegerPropagationConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = JaegerPropagationConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.tracer.configuration.propagation." +
                "JaegerPropagationConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
