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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
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
 * Configuration for {@link DefaultDuplicateLogHandler}.
 *
 * @author Navaneeth Mysore
 * @since 1.0.0
 */
@Immutable
@DefaultDuplicateLogHandlerConfiguration.Valid
@JsonDeserialize(builder = DefaultDuplicateLogHandlerConfiguration.Builder.class)
public final class DefaultDuplicateLogHandlerConfiguration {
    private final String markerName;
    private final LogLevel formattedMessageLogLevel;
    private final LogLevel throwableLogLevel;
    private final String countTextPrefix;
    private final String countTextSuffix;
    private final String throwableTypeNameAndMessageSeparator;
    private final boolean logParamsAndThrowable;

    private DefaultDuplicateLogHandlerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultDuplicateLogHandlerConfigurationValidator.validate(
                builder.formattedMessageLogLevel,
                builder.throwableLogLevel);

        countTextPrefix = Objects.requireNonNullElse(builder.countTextPrefix, "");
        countTextSuffix = Objects.requireNonNullElse(builder.countTextSuffix, "");
        throwableTypeNameAndMessageSeparator =
                Objects.requireNonNullElse(builder.throwableTypeNameAndMessageSeparator, "");

        markerName = builder.markerName;
        formattedMessageLogLevel = builder.formattedMessageLogLevel;
        throwableLogLevel = builder.throwableLogLevel;
        logParamsAndThrowable = builder.logParamsAndThrowable;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getMarkerName() {
        return markerName;
    }

    public LogLevel getFormattedMessageLogLevel() {
        return formattedMessageLogLevel;
    }

    public LogLevel getThrowableLogLevel() {
        return throwableLogLevel;
    }

    public String getCountTextPrefix() {
        return countTextPrefix;
    }

    public String getCountTextSuffix() {
        return countTextSuffix;
    }

    public String getThrowableTypeNameAndMessageSeparator() {
        return throwableTypeNameAndMessageSeparator;
    }

    public boolean isLogParamsAndThrowable() {
        return logParamsAndThrowable;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultDuplicateLogHandlerConfiguration)) {
            return false;
        }

        final DefaultDuplicateLogHandlerConfiguration that = (DefaultDuplicateLogHandlerConfiguration) o;
        return logParamsAndThrowable == that.logParamsAndThrowable &&
                Objects.equals(markerName, that.markerName) &&
                formattedMessageLogLevel == that.formattedMessageLogLevel &&
                throwableLogLevel == that.throwableLogLevel &&
                Objects.equals(countTextPrefix, that.countTextPrefix) &&
                Objects.equals(countTextSuffix, that.countTextSuffix) &&
                Objects.equals(throwableTypeNameAndMessageSeparator, that.throwableTypeNameAndMessageSeparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                markerName,
                formattedMessageLogLevel,
                throwableLogLevel,
                countTextPrefix,
                countTextSuffix,
                throwableTypeNameAndMessageSeparator,
                logParamsAndThrowable);
    }

    @Override
    public String toString() {
        return "DefaultDuplicateLogHandlerConfiguration{" +
                "markerName='" + markerName + '\'' +
                ", formattedMessageLogLevel=" + formattedMessageLogLevel +
                ", throwableLogLevel=" + throwableLogLevel +
                ", countTextPrefix='" + countTextPrefix + '\'' +
                ", countTextSuffix='" + countTextSuffix + '\'' +
                ", throwableTypeNameAndMessageSeparator='" + throwableTypeNameAndMessageSeparator + '\'' +
                ", logParamsAndThrowable=" + logParamsAndThrowable +
                '}';
    }

    /**
     * {@link DefaultDuplicateLogHandlerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_MARKER_NAME = DefaultDuplicateLogHandler.class.getSimpleName();
        private static final LogLevel DEFAULT_FORMATTED_MESSAGE_LOG_LEVEL = LogLevel.INFO;
        private static final LogLevel DEFAULT_THROWABLE_LOG_LEVEL = LogLevel.ERROR;
        private static final String DEFAULT_COUNT_TEXT_PREFIX = " (count=";
        private static final String DEFAULT_COUNT_TEXT_SUFFIX = ")";
        private static final String DEFAULT_THROWABLE_TYPE_NAME_AND_MESSAGE_SEPARATOR = " - ";
        private static final boolean DEFAULT_LOG_PARAMS_AND_THROWABLE = true;

        private String markerName = DEFAULT_MARKER_NAME;
        private LogLevel formattedMessageLogLevel = DEFAULT_FORMATTED_MESSAGE_LOG_LEVEL;
        private LogLevel throwableLogLevel = DEFAULT_THROWABLE_LOG_LEVEL;
        private String countTextPrefix = DEFAULT_COUNT_TEXT_PREFIX;
        private String countTextSuffix = DEFAULT_COUNT_TEXT_SUFFIX;
        private String throwableTypeNameAndMessageSeparator = DEFAULT_THROWABLE_TYPE_NAME_AND_MESSAGE_SEPARATOR;
        private boolean logParamsAndThrowable = DEFAULT_LOG_PARAMS_AND_THROWABLE;

        private Builder() {
        }

        public Builder markerName(final String markerName) {
            this.markerName = markerName;
            return this;
        }

        public Builder formattedMessageLogLevel(final LogLevel formattedMessageLogLevel) {
            this.formattedMessageLogLevel = formattedMessageLogLevel;
            return this;
        }

        public Builder throwableLogLevel(final LogLevel throwableLogLevel) {
            this.throwableLogLevel = throwableLogLevel;
            return this;
        }

        public Builder countTextPrefix(final String countTextPrefix) {
            this.countTextPrefix = countTextPrefix;
            return this;
        }

        public Builder countTextSuffix(final String countTextSuffix) {
            this.countTextSuffix = countTextSuffix;
            return this;
        }

        public Builder throwableTypeNameAndMessageSeparator(final String throwableTypeNameAndMessageSeparator) {
            this.throwableTypeNameAndMessageSeparator = throwableTypeNameAndMessageSeparator;
            return this;
        }

        public Builder logParams(final boolean logParams) {
            this.logParamsAndThrowable = logParams;
            return this;
        }

        public DefaultDuplicateLogHandlerConfiguration build() {
            return new DefaultDuplicateLogHandlerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultDuplicateLogHandlerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultDuplicateLogHandlerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.filter.duplicate." +
                "DefaultDuplicateLogHandlerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
