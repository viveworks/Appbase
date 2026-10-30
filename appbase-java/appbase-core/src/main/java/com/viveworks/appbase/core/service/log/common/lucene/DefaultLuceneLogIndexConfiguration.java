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

package com.viveworks.appbase.core.service.log.common.lucene;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneServiceConfiguration;
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
 * Configuration for {@link DefaultLuceneLogIndex}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultLuceneLogIndexConfiguration.Valid
@JsonDeserialize(builder = DefaultLuceneLogIndexConfiguration.Builder.class)
public final class DefaultLuceneLogIndexConfiguration implements LuceneLogIndexConfiguration {
    private final boolean enabled;
    private final LuceneServiceConfiguration luceneServiceConfiguration;
    private final String timestampStoredFieldName;
    private final String timestampLongPointName;
    private final String threadNameFieldName;
    private final String loggerNameFieldName;
    private final String logLevelFieldName;
    private final String formattedMessageFieldName;
    private final String messageFieldName;

    private final String throwableTypeNameFieldName;
    private final String throwableMessageFieldName;
    private final boolean throwableStackTraceIndexEnabled;
    private final String throwableStackTraceFieldName;

    private final boolean mdcIndexEnabled;
    private final String mdcFieldNamePrefix;
    private final String mdcFieldNameSuffix;

    private final boolean markerIndexEnabled;
    private final String markerFieldName;

    private final String throwableCauseTypeNameFieldName;
    private final String throwableCauseMessageFieldName;
    private final boolean throwableCauseStackTraceIndexEnabled;
    private final String throwableCauseStackTraceFieldName;

    private final boolean callerStackIndexEnabled;
    private final String callerStackFieldName;

    private DefaultLuceneLogIndexConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultLuceneLogIndexConfigurationValidator.validate(
                builder.enabled,
                builder.luceneServiceConfiguration,
                builder.timestampStoredFieldName,
                builder.timestampLongPointName,
                builder.threadNameFieldName,
                builder.loggerNameFieldName,
                builder.logLevelFieldName,
                builder.formattedMessageFieldName,
                builder.messageFieldName,
                builder.throwableTypeNameFieldName,
                builder.throwableMessageFieldName,
                builder.throwableStackTraceIndexEnabled,
                builder.throwableStackTraceFieldName,
                builder.markerIndexEnabled,
                builder.markerFieldName,
                builder.throwableCauseTypeNameFieldName,
                builder.throwableCauseMessageFieldName,
                builder.throwableCauseStackTraceIndexEnabled,
                builder.throwableCauseStackTraceFieldName,
                builder.callerStackIndexEnabled,
                builder.callerStackFieldName);

        enabled = builder.enabled;
        luceneServiceConfiguration = builder.luceneServiceConfiguration;

        timestampStoredFieldName = builder.timestampStoredFieldName;
        timestampLongPointName = builder.timestampLongPointName;
        threadNameFieldName = builder.threadNameFieldName;
        loggerNameFieldName = builder.loggerNameFieldName;
        logLevelFieldName = builder.logLevelFieldName;
        formattedMessageFieldName = builder.formattedMessageFieldName;
        messageFieldName = builder.messageFieldName;

        throwableTypeNameFieldName = builder.throwableTypeNameFieldName;
        throwableMessageFieldName = builder.throwableMessageFieldName;
        throwableStackTraceIndexEnabled = builder.throwableStackTraceIndexEnabled;
        throwableStackTraceFieldName = builder.throwableStackTraceFieldName;

        mdcIndexEnabled = builder.mdcIndexEnabled;
        mdcFieldNamePrefix = builder.mdcFieldNamePrefix;
        mdcFieldNameSuffix = builder.mdcFieldNameSuffix;

        markerIndexEnabled = builder.markerIndexEnabled;
        markerFieldName = builder.markerFieldName;

        throwableCauseTypeNameFieldName = builder.throwableCauseTypeNameFieldName;
        throwableCauseMessageFieldName = builder.throwableCauseMessageFieldName;
        throwableCauseStackTraceIndexEnabled = builder.throwableCauseStackTraceIndexEnabled;
        throwableCauseStackTraceFieldName = builder.throwableCauseStackTraceFieldName;

        callerStackIndexEnabled = builder.callerStackIndexEnabled;
        callerStackFieldName = builder.callerStackFieldName;
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
    public LuceneServiceConfiguration getLuceneServiceConfiguration() {
        return luceneServiceConfiguration;
    }

    @Override
    public String getTimestampStoredFieldName() {
        return timestampStoredFieldName;
    }

    @Override
    public String getTimestampLongPointName() {
        return timestampLongPointName;
    }

    @Override
    public String getThreadNameFieldName() {
        return threadNameFieldName;
    }

    @Override
    public String getLoggerNameFieldName() {
        return loggerNameFieldName;
    }

    @Override
    public String getLogLevelFieldName() {
        return logLevelFieldName;
    }

    @Override
    public String getFormattedMessageFieldName() {
        return formattedMessageFieldName;
    }

    @Override
    public String getMessageFieldName() {
        return messageFieldName;
    }

    @Override
    public String getThrowableTypeNameFieldName() {
        return throwableTypeNameFieldName;
    }

    @Override
    public String getThrowableMessageFieldName() {
        return throwableMessageFieldName;
    }

    @Override
    public boolean isThrowableStackTraceIndexEnabled() {
        return throwableStackTraceIndexEnabled;
    }

    @Override
    public String getThrowableStackTraceFieldName() {
        return throwableStackTraceFieldName;
    }

    @Override
    public boolean isMdcIndexEnabled() {
        return mdcIndexEnabled;
    }

    @Override
    public String getMdcFieldName(final String key) {
        String mdcFieldName = "";

        final String prefix = getMdcFieldNamePrefix();
        if (prefix != null) {
            mdcFieldName = prefix;
        }

        mdcFieldName += key;

        final String suffix = getMdcFieldNameSuffix();
        if (suffix != null) {
            mdcFieldName += suffix;
        }

        return mdcFieldName;
    }

    @Override
    public String getMdcFieldNamePrefix() {
        return mdcFieldNamePrefix;
    }

    @Override
    public String getMdcFieldNameSuffix() {
        return mdcFieldNameSuffix;
    }

    @Override
    public boolean isMarkerIndexEnabled() {
        return markerIndexEnabled;
    }

    @Override
    public String getMarkerFieldName() {
        return markerFieldName;
    }

    @Override
    public String getThrowableCauseTypeNameFieldName() {
        return throwableCauseTypeNameFieldName;
    }

    @Override
    public String getThrowableCauseMessageFieldName() {
        return throwableCauseMessageFieldName;
    }

    @Override
    public boolean isThrowableCauseStackTraceIndexEnabled() {
        return throwableCauseStackTraceIndexEnabled;
    }

    @Override
    public String getThrowableCauseStackTraceFieldName() {
        return throwableCauseStackTraceFieldName;
    }

    @Override
    public boolean isCallerStackIndexEnabled() {
        return callerStackIndexEnabled;
    }

    @Override
    public String getCallerStackFieldName() {
        return callerStackFieldName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultLuceneLogIndexConfiguration)) {
            return false;
        }

        final DefaultLuceneLogIndexConfiguration that = (DefaultLuceneLogIndexConfiguration) o;
        return enabled == that.enabled &&
                throwableStackTraceIndexEnabled == that.throwableStackTraceIndexEnabled &&
                mdcIndexEnabled == that.mdcIndexEnabled &&
                markerIndexEnabled == that.markerIndexEnabled &&
                throwableCauseStackTraceIndexEnabled == that.throwableCauseStackTraceIndexEnabled &&
                callerStackIndexEnabled == that.callerStackIndexEnabled &&
                Objects.equals(luceneServiceConfiguration, that.luceneServiceConfiguration) &&
                Objects.equals(timestampStoredFieldName, that.timestampStoredFieldName) &&
                Objects.equals(timestampLongPointName, that.timestampLongPointName) &&
                Objects.equals(threadNameFieldName, that.threadNameFieldName) &&
                Objects.equals(loggerNameFieldName, that.loggerNameFieldName) &&
                Objects.equals(logLevelFieldName, that.logLevelFieldName) &&
                Objects.equals(formattedMessageFieldName, that.formattedMessageFieldName) &&
                Objects.equals(messageFieldName, that.messageFieldName) &&
                Objects.equals(throwableTypeNameFieldName, that.throwableTypeNameFieldName) &&
                Objects.equals(throwableMessageFieldName, that.throwableMessageFieldName) &&
                Objects.equals(throwableStackTraceFieldName, that.throwableStackTraceFieldName) &&
                Objects.equals(mdcFieldNamePrefix, that.mdcFieldNamePrefix) &&
                Objects.equals(mdcFieldNameSuffix, that.mdcFieldNameSuffix) &&
                Objects.equals(markerFieldName, that.markerFieldName) &&
                Objects.equals(throwableCauseTypeNameFieldName, that.throwableCauseTypeNameFieldName) &&
                Objects.equals(throwableCauseMessageFieldName, that.throwableCauseMessageFieldName) &&
                Objects.equals(throwableCauseStackTraceFieldName, that.throwableCauseStackTraceFieldName) &&
                Objects.equals(callerStackFieldName, that.callerStackFieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                luceneServiceConfiguration,
                timestampStoredFieldName,
                timestampLongPointName,
                threadNameFieldName,
                loggerNameFieldName,
                logLevelFieldName,
                formattedMessageFieldName,
                messageFieldName,
                throwableTypeNameFieldName,
                throwableMessageFieldName,
                throwableStackTraceIndexEnabled,
                throwableStackTraceFieldName,
                mdcIndexEnabled,
                mdcFieldNamePrefix,
                mdcFieldNameSuffix,
                markerIndexEnabled,
                markerFieldName,
                throwableCauseTypeNameFieldName,
                throwableCauseMessageFieldName,
                throwableCauseStackTraceIndexEnabled,
                throwableCauseStackTraceFieldName,
                callerStackIndexEnabled,
                callerStackFieldName);
    }

    /**
     * Builder for {@link DefaultLuceneLogIndexConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        // R3 (internal work item 1073): file I/O on the hot log path — disabled by default; users opt in explicitly.
        private static final boolean DEFAULT_ENABLED = false;

        private static final String THROWABLE = "throwable";
        private static final String SEPARATOR = ".";
        private static final String TYPE_NAME = "typeName";
        private static final String MESSAGE = "message";
        private static final String STACK_TRACE = "stackTrace";
        private static final String CAUSE = "cause";
        private static final String CALLER_STACK = "callerStack";

        private static final LuceneServiceConfiguration DEFAULT_LUCENE_SERVICE_CONFIGURATION =
                DefaultLuceneServiceConfiguration.builder().build();
        private static final String DEFAULT_TIMESTAMP_STORED_FIELD_NAME = "creationTimestamp";
        private static final String DEFAULT_TIMESTAMP_LONG_POINT_NAME = "creationTimestampLongPoint";
        private static final String DEFAULT_THREAD_NAME_FIELD_NAME = "threadName";
        private static final String DEFAULT_LOGGER_NAME_FIELD_NAME = "loggerName";
        private static final String DEFAULT_LOG_LEVEL_FIELD_NAME = "logLevel";
        private static final String DEFAULT_FORMATTED_MESSAGE_FIELD_NAME = "formattedMessage";
        private static final String DEFAULT_MESSAGE_FIELD_NAME = MESSAGE;

        private static final String THROWABLE_FIELD_PREFIX = THROWABLE + SEPARATOR;
        private static final String DEFAULT_THROWABLE_TYPE_NAME_FIELD_NAME =
                THROWABLE_FIELD_PREFIX + TYPE_NAME;
        private static final String DEFAULT_THROWABLE_MESSAGE_FIELD_NAME =
                THROWABLE_FIELD_PREFIX + MESSAGE;
        private static final boolean DEFAULT_THROWABLE_STACK_TRACE_INDEX_ENABLED = true;
        private static final String DEFAULT_THROWABLE_STACK_TRACE_FIELD_NAME =
                THROWABLE_FIELD_PREFIX + STACK_TRACE;

        private static final boolean DEFAULT_MDC_INDEX_ENABLED = true;
        private static final String DEFAULT_MDC_FIELD_NAME_PREFIX = "mdc.";
        private static final String DEFAULT_MDC_FIELD_NAME_SUFFIX = "";

        private static final boolean DEFAULT_MARKER_INDEX_ENABLED = true;
        private static final String DEFAULT_MARKER_FIELD_NAME = "marker";

        private static final String THROWABLE_CAUSE_FIELD_PREFIX =
                THROWABLE + SEPARATOR + CAUSE + SEPARATOR;
        private static final String DEFAULT_THROWABLE_CAUSE_TYPE_NAME_FIELDNAME =
                THROWABLE_CAUSE_FIELD_PREFIX + TYPE_NAME;
        private static final String DEFAULT_THROWABLE_CAUSE_MESSAGE_FIELD_NAME =
                THROWABLE_CAUSE_FIELD_PREFIX + MESSAGE;
        private static final boolean DEFAULT_THROWABLE_CAUSE_STACK_TRACE_INDEX_ENABLED = true;
        private static final String DEFAULT_THROWABLE_CAUSE_STACK_TRACE_FIELD_NAME =
                THROWABLE_CAUSE_FIELD_PREFIX + STACK_TRACE;

        private static final boolean DEFAULT_CALLER_STACK_INDEX_ENABLED = false;
        private static final String DEFAULT_CALLER_STACK_FIELD_NAME = CALLER_STACK;

        private boolean enabled = DEFAULT_ENABLED;
        private LuceneServiceConfiguration luceneServiceConfiguration = DEFAULT_LUCENE_SERVICE_CONFIGURATION;

        private String timestampStoredFieldName = DEFAULT_TIMESTAMP_STORED_FIELD_NAME;
        private String timestampLongPointName = DEFAULT_TIMESTAMP_LONG_POINT_NAME;
        private String threadNameFieldName = DEFAULT_THREAD_NAME_FIELD_NAME;
        private String loggerNameFieldName = DEFAULT_LOGGER_NAME_FIELD_NAME;
        private String logLevelFieldName = DEFAULT_LOG_LEVEL_FIELD_NAME;
        private String formattedMessageFieldName = DEFAULT_FORMATTED_MESSAGE_FIELD_NAME;
        private String messageFieldName = DEFAULT_MESSAGE_FIELD_NAME;

        private String throwableTypeNameFieldName = DEFAULT_THROWABLE_TYPE_NAME_FIELD_NAME;
        private String throwableMessageFieldName = DEFAULT_THROWABLE_MESSAGE_FIELD_NAME;
        private boolean throwableStackTraceIndexEnabled = DEFAULT_THROWABLE_STACK_TRACE_INDEX_ENABLED;
        private String throwableStackTraceFieldName = DEFAULT_THROWABLE_STACK_TRACE_FIELD_NAME;

        private boolean mdcIndexEnabled = DEFAULT_MDC_INDEX_ENABLED;
        private String mdcFieldNamePrefix = DEFAULT_MDC_FIELD_NAME_PREFIX;
        private String mdcFieldNameSuffix = DEFAULT_MDC_FIELD_NAME_SUFFIX;

        private boolean markerIndexEnabled = DEFAULT_MARKER_INDEX_ENABLED;
        private String markerFieldName = DEFAULT_MARKER_FIELD_NAME;

        private String throwableCauseTypeNameFieldName = DEFAULT_THROWABLE_CAUSE_TYPE_NAME_FIELDNAME;
        private String throwableCauseMessageFieldName = DEFAULT_THROWABLE_CAUSE_MESSAGE_FIELD_NAME;
        private boolean throwableCauseStackTraceIndexEnabled = DEFAULT_THROWABLE_CAUSE_STACK_TRACE_INDEX_ENABLED;
        private String throwableCauseStackTraceFieldName = DEFAULT_THROWABLE_CAUSE_STACK_TRACE_FIELD_NAME;

        private boolean callerStackIndexEnabled = DEFAULT_CALLER_STACK_INDEX_ENABLED;
        private String callerStackFieldName = DEFAULT_CALLER_STACK_FIELD_NAME;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder luceneServiceConfiguration(final LuceneServiceConfiguration luceneServiceConfiguration) {
            this.luceneServiceConfiguration = luceneServiceConfiguration;
            return this;
        }

        public Builder timestampStoredFieldName(final String timestampStoredFieldName) {
            this.timestampStoredFieldName = timestampStoredFieldName;
            return this;
        }

        public Builder timestampLongPointName(final String timestampLongPointName) {
            this.timestampLongPointName = timestampLongPointName;
            return this;
        }

        public Builder threadNameFieldName(final String threadNameFieldName) {
            this.threadNameFieldName = threadNameFieldName;
            return this;
        }

        public Builder loggerNameFieldName(final String loggerNameFieldName) {
            this.loggerNameFieldName = loggerNameFieldName;
            return this;
        }

        public Builder logLevelFieldName(final String logLevelFieldName) {
            this.logLevelFieldName = logLevelFieldName;
            return this;
        }

        public Builder formattedMessageFieldName(final String formattedMessageFieldName) {
            this.formattedMessageFieldName = formattedMessageFieldName;
            return this;
        }

        public Builder messageFieldName(final String messageFieldName) {
            this.messageFieldName = messageFieldName;
            return this;
        }

        public Builder throwableTypeNameFieldName(final String throwableTypeNameFieldName) {
            this.throwableTypeNameFieldName = throwableTypeNameFieldName;
            return this;
        }

        public Builder throwableMessageFieldName(final String throwableMessageFieldName) {
            this.throwableMessageFieldName = throwableMessageFieldName;
            return this;
        }

        public Builder throwableStackTraceIndexEnabled(final boolean throwableStackTraceIndexEnabled) {
            this.throwableStackTraceIndexEnabled = throwableStackTraceIndexEnabled;
            return this;
        }

        public Builder throwableStackTraceFieldName(final String throwableStackTraceFieldName) {
            this.throwableStackTraceFieldName = throwableStackTraceFieldName;
            return this;
        }

        public Builder mdcIndexEnabled(final boolean mdcIndexEnabled) {
            this.mdcIndexEnabled = mdcIndexEnabled;
            return this;
        }

        public Builder mdcFieldNamePrefix(final String mdcFieldNamePrefix) {
            this.mdcFieldNamePrefix = mdcFieldNamePrefix;
            return this;
        }

        public Builder mdcFieldNameSuffix(final String mdcFieldNameSuffix) {
            this.mdcFieldNameSuffix = mdcFieldNameSuffix;
            return this;
        }

        public Builder markerIndexEnabled(final boolean markerIndexEnabled) {
            this.markerIndexEnabled = markerIndexEnabled;
            return this;
        }

        public Builder markerFieldName(final String markerFieldName) {
            this.markerFieldName = markerFieldName;
            return this;
        }

        public Builder throwableCauseTypeNameFieldName(final String throwableCauseTypeNameFieldName) {
            this.throwableCauseTypeNameFieldName = throwableCauseTypeNameFieldName;
            return this;
        }

        public Builder throwableCauseMessageFieldName(final String throwableCauseMessageFieldName) {
            this.throwableCauseMessageFieldName = throwableCauseMessageFieldName;
            return this;
        }

        public Builder throwableCauseStackTraceIndexEnabled(final boolean throwableCauseStackTraceIndexEnabled) {
            this.throwableCauseStackTraceIndexEnabled = throwableCauseStackTraceIndexEnabled;
            return this;
        }

        public Builder throwableCauseStackTraceFieldName(final String throwableCauseStackTraceFieldName) {
            this.throwableCauseStackTraceFieldName = throwableCauseStackTraceFieldName;
            return this;
        }

        public Builder callerStackIndexEnabled(final boolean callerStackIndexEnabled) {
            this.callerStackIndexEnabled = callerStackIndexEnabled;
            return this;
        }

        public Builder callerStackFieldName(final String callerStackFieldName) {
            this.callerStackFieldName = callerStackFieldName;
            return this;
        }

        public DefaultLuceneLogIndexConfiguration build() {
            return new DefaultLuceneLogIndexConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultLuceneLogIndexConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultLuceneLogIndexConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
