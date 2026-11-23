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

package com.viveworks.appbase.core.service.log.filter.metrics;

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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link MetricsTurboLogEventFilterHandler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@MetricsTurboFilterHandlerConfiguration.Valid
@JsonDeserialize(builder = MetricsTurboFilterHandlerConfiguration.Builder.class)
public final class MetricsTurboFilterHandlerConfiguration {
    private final boolean enabled;
    private final boolean measureLogLevels;

    private final boolean measureLoggerNames;
    private final Set<String> allowedLoggerNames;
    private final Set<String> blockedLoggerNames;

    private final boolean measureFormattedMessages;
    private final Set<String> allowedFormattedMessages;
    private final Set<String> blockedFormattedMessages;

    private final boolean measureThrowableTypeNames;
    private final Set<String> allowedThrowableTypeNames;
    private final Set<String> blockedThrowableTypeNames;

    private MetricsTurboFilterHandlerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        enabled = builder.enabled;
        measureLogLevels = builder.measureLogLevels;

        measureLoggerNames = builder.measureLoggerNames;
        if (builder.allowedLoggerNames != null) {
            allowedLoggerNames = new HashSet<>(builder.allowedLoggerNames);
        } else {
            allowedLoggerNames = Set.of();
        }

        if (builder.blockedLoggerNames != null) {
            blockedLoggerNames = new HashSet<>(builder.blockedLoggerNames);
        } else {
            blockedLoggerNames = Set.of();
        }

        measureFormattedMessages = builder.measureFormattedMessages;
        if (builder.allowedFormattedMessages != null) {
            allowedFormattedMessages = new HashSet<>(builder.allowedFormattedMessages);
        } else {
            allowedFormattedMessages = Set.of();
        }

        if (builder.blockedFormattedMessages != null) {
            blockedFormattedMessages = new HashSet<>(builder.blockedFormattedMessages);
        } else {
            blockedFormattedMessages = Set.of();
        }

        measureThrowableTypeNames = builder.measureThrowableTypeNames;
        if (builder.allowedThrowableTypeNames != null) {
            allowedThrowableTypeNames = new HashSet<>(builder.allowedThrowableTypeNames);
        } else {
            allowedThrowableTypeNames = Set.of();
        }

        if (builder.blockedThrowableTypeNames != null) {
            blockedThrowableTypeNames = new HashSet<>(builder.blockedThrowableTypeNames);
        } else {
            blockedThrowableTypeNames = Set.of();
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

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMeasureLogLevels() {
        return measureLogLevels;
    }

    public boolean isMeasureLoggerNames() {
        return measureLoggerNames;
    }

    public Set<String> getAllowedLoggerNames() {
        return allowedLoggerNames;
    }

    public Set<String> getBlockedLoggerNames() {
        return blockedLoggerNames;
    }

    public boolean isMeasureFormattedMessages() {
        return measureFormattedMessages;
    }

    public Set<String> getAllowedFormattedMessages() {
        return allowedFormattedMessages;
    }

    public Set<String> getBlockedFormattedMessages() {
        return blockedFormattedMessages;
    }

    public boolean isMeasureThrowableTypeNames() {
        return measureThrowableTypeNames;
    }

    public Set<String> getAllowedThrowableTypeNames() {
        return allowedThrowableTypeNames;
    }

    public Set<String> getBlockedThrowableTypeNames() {
        return blockedThrowableTypeNames;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MetricsTurboFilterHandlerConfiguration that = (MetricsTurboFilterHandlerConfiguration) o;
        return enabled == that.enabled &&
                measureLogLevels == that.measureLogLevels &&
                measureLoggerNames == that.measureLoggerNames &&
                measureFormattedMessages == that.measureFormattedMessages &&
                measureThrowableTypeNames == that.measureThrowableTypeNames &&
                Objects.equals(allowedLoggerNames, that.allowedLoggerNames) &&
                Objects.equals(blockedLoggerNames, that.blockedLoggerNames) &&
                Objects.equals(allowedFormattedMessages, that.allowedFormattedMessages) &&
                Objects.equals(blockedFormattedMessages, that.blockedFormattedMessages) &&
                Objects.equals(allowedThrowableTypeNames, that.allowedThrowableTypeNames) &&
                Objects.equals(blockedThrowableTypeNames, that.blockedThrowableTypeNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                measureLogLevels,
                measureLoggerNames,
                allowedLoggerNames,
                blockedLoggerNames,
                measureFormattedMessages,
                allowedFormattedMessages,
                blockedFormattedMessages,
                measureThrowableTypeNames,
                allowedThrowableTypeNames,
                blockedThrowableTypeNames);
    }

    @Override
    public String toString() {
        return "MetricsTurboFilterHandlerConfiguration{" +
                "enabled=" + enabled +
                ", measureLogLevels=" + measureLogLevels +
                ", measureLoggerNames=" + measureLoggerNames +
                ", allowedLoggerNames=" + allowedLoggerNames +
                ", blockedLoggerNames=" + blockedLoggerNames +
                ", measureFormattedMessages=" + measureFormattedMessages +
                ", allowedFormattedMessages=" + allowedFormattedMessages +
                ", blockedFormattedMessages=" + blockedFormattedMessages +
                ", measureThrowableTypeNames=" + measureThrowableTypeNames +
                ", allowedThrowableTypeNames=" + allowedThrowableTypeNames +
                ", blockedThrowableTypeNames=" + blockedThrowableTypeNames +
                '}';
    }

    /**
     * {@link MetricsTurboFilterHandlerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final Set<String> EMPTY_STRING_SET = Set.of();

        private static final boolean DEFAULT_MEASURE_LOG_LEVELS = true;
        private static final boolean DEFAULT_MEASURE_LOGGER_NAMES = true;
        private static final boolean DEFAULT_MEASURE_FORMATTED_MESSAGES = false;
        private static final boolean DEFAULT_MEASURE_THROWABLE_TYPE_NAMES = true;

        private boolean enabled = DEFAULT_ENABLED;
        private boolean measureLogLevels = DEFAULT_MEASURE_LOG_LEVELS;

        private boolean measureLoggerNames = DEFAULT_MEASURE_LOGGER_NAMES;
        private Set<String> allowedLoggerNames = EMPTY_STRING_SET;
        private Set<String> blockedLoggerNames = EMPTY_STRING_SET;

        private boolean measureFormattedMessages = DEFAULT_MEASURE_FORMATTED_MESSAGES;
        private Set<String> allowedFormattedMessages = EMPTY_STRING_SET;
        private Set<String> blockedFormattedMessages = EMPTY_STRING_SET;

        private boolean measureThrowableTypeNames = DEFAULT_MEASURE_THROWABLE_TYPE_NAMES;
        private Set<String> allowedThrowableTypeNames = EMPTY_STRING_SET;
        private Set<String> blockedThrowableTypeNames = EMPTY_STRING_SET;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder measureLogLevels(final boolean measureLogLevels) {
            this.measureLogLevels = measureLogLevels;
            return this;
        }

        public Builder measureLoggerNames(final boolean measureLoggerNames) {
            this.measureLoggerNames = measureLoggerNames;
            return this;
        }

        public Builder allowedLoggerNames(final Set<String> allowedLoggerNames) {
            this.allowedLoggerNames = allowedLoggerNames;
            return this;
        }

        public Builder blockedLoggerNames(final Set<String> blockedLoggerNames) {
            this.blockedLoggerNames = blockedLoggerNames;
            return this;
        }

        public Builder measureLogFormats(final boolean measureLogFormats) {
            this.measureFormattedMessages = measureLogFormats;
            return this;
        }

        public Builder allowedLogFormats(final Set<String> allowedLogFormats) {
            this.allowedFormattedMessages = allowedLogFormats;
            return this;
        }

        public Builder blockedLogFormats(final Set<String> blockedLogFormats) {
            this.blockedFormattedMessages = blockedLogFormats;
            return this;
        }

        public Builder measureThrowableTypeNames(final boolean measureThrowableTypeNames) {
            this.measureThrowableTypeNames = measureThrowableTypeNames;
            return this;
        }

        public Builder allowedThrowableTypeNames(final Set<String> allowedThrowableTypeNames) {
            this.allowedThrowableTypeNames = allowedThrowableTypeNames;
            return this;
        }

        public Builder blockedThrowableTypeNames(final Set<String> blockedThrowableTypeNames) {
            this.blockedThrowableTypeNames = blockedThrowableTypeNames;
            return this;
        }

        public MetricsTurboFilterHandlerConfiguration build() {
            return new MetricsTurboFilterHandlerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link MetricsTurboFilterHandlerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = MetricsTurboFilterHandlerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.filter.metric." +
                "MetricsTurboFilterHandlerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
