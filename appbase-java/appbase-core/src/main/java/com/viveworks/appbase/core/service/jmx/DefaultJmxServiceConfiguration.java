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

package com.viveworks.appbase.core.service.jmx;

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
 * Default implementation of {@link JmxServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultJmxServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultJmxServiceConfiguration.Builder.class)
public final class DefaultJmxServiceConfiguration implements JmxServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultJmxServiceConfiguration";

    private final boolean enabled;

    @JsonSerialize(contentAs = Class.class)
    @JsonDeserialize(contentAs = Class.class)
    private final Set<Class<?>> excludedMxBeanRegistrations;

    private DefaultJmxServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        enabled = builder.enabled;

        if (builder.excludedMxBeanRegistrations == null) {
            excludedMxBeanRegistrations = Set.of();
        } else {
            excludedMxBeanRegistrations = builder.excludedMxBeanRegistrations;
        }
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
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Set<Class<?>> getExcludedMxBeanRegistrations() {
        return excludedMxBeanRegistrations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultJmxServiceConfiguration that = (DefaultJmxServiceConfiguration) o;
        return enabled == that.enabled &&
                Objects.equals(excludedMxBeanRegistrations, that.excludedMxBeanRegistrations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, excludedMxBeanRegistrations);
    }

    @Override
    public String toString() {
        return "DefaultJmxServiceConfiguration{" +
                "enabled=" + enabled +
                ", excludedMxBeanRegistrations=" + excludedMxBeanRegistrations +
                '}';
    }

    /**
     * Builder for {@link DefaultJmxServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private boolean DEFAULT_ENABLED = true;
        private Set<Class<?>> DEFAULT_EXCLUDED_MXBEAN_REGISTRATIONS = Set.of();

        private boolean enabled = DEFAULT_ENABLED;

        @JsonSerialize(contentAs = Class.class)
        @JsonDeserialize(contentAs = Class.class)
        private Set<Class<?>> excludedMxBeanRegistrations = DEFAULT_EXCLUDED_MXBEAN_REGISTRATIONS;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder excludedMxBeanRegistrations(
                @JsonSerialize(contentAs = Class.class)
                @JsonDeserialize(contentAs = Class.class)
                final Set<Class<?>> excludedMxBeanRegistrations)
        {
            this.excludedMxBeanRegistrations = excludedMxBeanRegistrations;
            return this;
        }

        public DefaultJmxServiceConfiguration build() {
            return new DefaultJmxServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultJmxServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultJmxServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
