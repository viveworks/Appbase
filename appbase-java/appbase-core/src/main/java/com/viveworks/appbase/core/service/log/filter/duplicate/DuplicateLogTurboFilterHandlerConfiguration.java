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

package com.viveworks.appbase.core.service.log.filter.duplicate;

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
 * Configuration for {@link DuplicateTurboLogEventFilterHandler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DuplicateLogTurboFilterHandlerConfiguration.Valid
@JsonDeserialize(builder = DuplicateLogTurboFilterHandlerConfiguration.Builder.class)
public final class DuplicateLogTurboFilterHandlerConfiguration {
    private final boolean enabled;
    private final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration;
    private final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration;

    private DuplicateLogTurboFilterHandlerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DuplicateLogTurboFilterHandlerConfigurationValidator.validate(
                builder.enabled,
                builder.defaultLogDeduplicatorConfiguration,
                builder.defaultDuplicateLogHandlerConfiguration);

        enabled = builder.enabled;
        defaultLogDeduplicatorConfiguration = builder.defaultLogDeduplicatorConfiguration;
        defaultDuplicateLogHandlerConfiguration = builder.defaultDuplicateLogHandlerConfiguration;
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

    public DefaultLogDeduplicatorConfiguration getDefaultLogDeduplicatorConfiguration() {
        return defaultLogDeduplicatorConfiguration;
    }

    public DefaultDuplicateLogHandlerConfiguration getDefaultDuplicateLogHandlerConfiguration() {
        return defaultDuplicateLogHandlerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DuplicateLogTurboFilterHandlerConfiguration that = (DuplicateLogTurboFilterHandlerConfiguration) o;
        return enabled == that.enabled &&
                Objects.equals(defaultLogDeduplicatorConfiguration, that.defaultLogDeduplicatorConfiguration) &&
                Objects.equals(defaultDuplicateLogHandlerConfiguration, that.defaultDuplicateLogHandlerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, defaultLogDeduplicatorConfiguration, defaultDuplicateLogHandlerConfiguration);
    }

    @Override
    public String toString() {
        return "DuplicateLogTurboFilterHandlerConfiguration{" +
                "enabled=" + enabled +
                ", defaultLogDeduplicatorConfiguration=" + defaultLogDeduplicatorConfiguration +
                ", defaultDuplicateLogHandlerConfiguration=" + defaultDuplicateLogHandlerConfiguration +
                '}';
    }

    /**
     * {@link DuplicateLogTurboFilterHandlerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = false;
        private static final DefaultLogDeduplicatorConfiguration DEFAULT_DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION =
                DefaultLogDeduplicatorConfiguration.builder().build();
        private static final DefaultDuplicateLogHandlerConfiguration
                DEFAULT_DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION =
                DefaultDuplicateLogHandlerConfiguration.builder().build();

        private boolean enabled = DEFAULT_ENABLED;
        private DefaultLogDeduplicatorConfiguration
                defaultLogDeduplicatorConfiguration = DEFAULT_DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION;
        private DefaultDuplicateLogHandlerConfiguration
                defaultDuplicateLogHandlerConfiguration = DEFAULT_DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder defaultLogDeduplicatorConfiguration(
                final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration)
        {
            this.defaultLogDeduplicatorConfiguration = defaultLogDeduplicatorConfiguration;
            return this;
        }

        public Builder defaultDuplicateLogHandlerConfiguration(
                final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration)
        {
            this.defaultDuplicateLogHandlerConfiguration = defaultDuplicateLogHandlerConfiguration;
            return this;
        }

        public DuplicateLogTurboFilterHandlerConfiguration build() {
            return new DuplicateLogTurboFilterHandlerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DuplicateLogTurboFilterHandlerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DuplicateLogTurboFilterHandlerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.filter.duplicate." +
                "DuplicateLogTurboFilterHandlerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
