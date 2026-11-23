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

package com.viveworks.appbase.core.service.metric.jmx.registry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.jmx.JmxConfig;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Used to configure {@link io.micrometer.jmx.JmxMeterRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JmxMeterRegistryConfiguration.Valid
@JsonDeserialize(builder = JmxMeterRegistryConfiguration.Builder.class)
public final class JmxMeterRegistryConfiguration implements JmxConfig, AppbaseConfiguration {
    private final boolean enabled;
    private final Map<String, String> additionalProperties;
    private final String prefix;
    private final String domain;
    private final Duration step;

    private JmxMeterRegistryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        JmxMeterRegistryConfigurationValidator.validate(
                builder.enabled,
                builder.additionalProperties,
                builder.prefix,
                builder.domain,
                builder.step);

        enabled = builder.enabled;
        if (builder.additionalProperties != null) {
            additionalProperties = Map.copyOf(builder.additionalProperties);
        } else {
            additionalProperties = null;
        }
        prefix = builder.prefix;
        domain = builder.domain;
        step = builder.step;
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
    @Nullable
    // Measured 2026-10-08 (internal work item 2536 L-10): NOT @NotNull on this parameter. This overrides Micrometer's
    // own MeterRegistryConfig#get(String), which declares no parameter constraint -- Bean Validation
    // (HV000151) correctly refuses to let an override strengthen its parent's precondition, which
    // made Hibernate Validator unable to build this class's metadata at all, for ANY validation of
    // it. The manual null check below already enforces the same contract at runtime.
    public String get(final String key) {
        AppbaseUtil.notNull(key, "key");

        if (additionalProperties != null) {
            return additionalProperties.get(key);
        }

        return null;
    }

    @Unmodifiable
    @Nullable
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties != null ? Map.copyOf(additionalProperties) : additionalProperties;
    }

    @Override
    @NotNull
    public String prefix() {
        return prefix;
    }

    @Override
    @NotNull
    public String domain() {
        return domain;
    }

    @Override
    @NotNull
    public Duration step() {
        return step;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JmxMeterRegistryConfiguration that = (JmxMeterRegistryConfiguration) o;
        return enabled == that.enabled &&
                Objects.equals(additionalProperties, that.additionalProperties) &&
                Objects.equals(prefix, that.prefix) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(step, that.step);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, additionalProperties, prefix, domain, step);
    }

    @Override
    public String toString() {
        return "JmxMeterRegistryConfiguration{" +
                "enabled=" + enabled +
                ", additionalProperties=" + additionalProperties +
                ", prefix='" + prefix + '\'' +
                ", domain='" + domain + '\'' +
                ", step=" + step +
                '}';
    }

    /**
     * Builder for {@link JmxMeterRegistryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = false;
        private static final Map<String, String> DEFAULT_ADDITIONAL_PROPERTIES = Map.of();
        private static final String DEFAULT_PREFIX = "jmx";
        private static final String DEFAULT_DOMAIN = "com.viveworks.appbase.metric";
        private static final Duration DEFAULT_STEP = Duration.ofMinutes(1);

        private boolean enabled = DEFAULT_ENABLED;
        private Map<String, String> additionalProperties = DEFAULT_ADDITIONAL_PROPERTIES;
        private String prefix = DEFAULT_PREFIX;
        private String domain = DEFAULT_DOMAIN;
        private Duration step = DEFAULT_STEP;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder additionalProperties(final Map<String, String> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public Builder prefix(final String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder domain(final String domain) {
            this.domain = domain;
            return this;
        }

        public Builder step(final Duration step) {
            this.step = step;
            return this;
        }

        public JmxMeterRegistryConfiguration build() {
            return new JmxMeterRegistryConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link JmxMeterRegistryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = JmxMeterRegistryConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.metric.jmx.registry.JmxMeterRegistryConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
