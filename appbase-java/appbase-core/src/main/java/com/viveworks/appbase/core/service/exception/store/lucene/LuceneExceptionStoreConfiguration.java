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

package com.viveworks.appbase.core.service.exception.store.lucene;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
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
 * Configuration for {@link LuceneExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@LuceneExceptionStoreConfiguration.Valid
@JsonDeserialize(builder = LuceneExceptionStoreConfiguration.Builder.class)
public final class LuceneExceptionStoreConfiguration implements ExceptionStoreConfiguration {
    private final boolean enabled;
    private final String discriminator;
    private final String idFieldName;
    private final String idIntPointName;
    private final String discriminatorFieldName;
    private final String messageFieldName;
    private final String throwableTypeNameFieldName;
    private final String stackTraceHashFieldName;
    private final String stackTraceHashIntPointName;
    private final String throwableMessageFieldName;
    private final String stackTraceFieldName;
    private final String sourceTypeNameFieldName;
    private final String threadNameFieldName;
    private final String contextFieldName;
    private final String timestampFieldName;
    private final String timestampLongPointName;
    private final int stackTraceSerializationMaxLength;

    private LuceneExceptionStoreConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        LuceneExceptionStoreConfigurationValidator.validate(
                builder.enabled,
                builder.discriminator,
                builder.idFieldName,
                builder.idIntPointName,
                builder.discriminatorFieldName,
                builder.messageFieldName,
                builder.throwableTypeNameFieldName,
                builder.stackTraceHashFieldName,
                builder.stackTraceHashIntPointName,
                builder.throwableMessageFieldName,
                builder.stackTraceFieldName,
                builder.sourceTypeNameFieldName,
                builder.threadNameFieldName,
                builder.contextFieldName,
                builder.timestampFieldName,
                builder.timestampLongPointName);

        enabled = builder.enabled;
        discriminator = builder.discriminator;
        idFieldName = builder.idFieldName;
        idIntPointName = builder.idIntPointName;
        discriminatorFieldName = builder.discriminatorFieldName;
        messageFieldName = builder.messageFieldName;
        throwableTypeNameFieldName = builder.throwableTypeNameFieldName;
        stackTraceHashFieldName = builder.stackTraceHashFieldName;
        stackTraceHashIntPointName = builder.stackTraceHashIntPointName;
        throwableMessageFieldName = builder.throwableMessageFieldName;
        stackTraceFieldName = builder.stackTraceFieldName;
        sourceTypeNameFieldName = builder.sourceTypeNameFieldName;
        threadNameFieldName = builder.threadNameFieldName;
        contextFieldName = builder.contextFieldName;
        timestampFieldName = builder.timestampFieldName;
        timestampLongPointName = builder.timestampLongPointName;
        stackTraceSerializationMaxLength = builder.stackTraceSerializationMaxLength;
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
    @SuppressWarnings("unchecked")
    public <S extends ExceptionStore> Class<S> getExceptionStoreType() {
        return (Class<S>) LuceneExceptionStore.class;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public String getIdIntPointName() {
        return idIntPointName;
    }

    public String getDiscriminatorFieldName() {
        return discriminatorFieldName;
    }

    public String getMessageFieldName() {
        return messageFieldName;
    }

    public String getThrowableTypeNameFieldName() {
        return throwableTypeNameFieldName;
    }

    public String getStackTraceHashFieldName() {
        return stackTraceHashFieldName;
    }

    public String getStackTraceHashIntPointName() {
        return stackTraceHashIntPointName;
    }

    public String getThrowableMessageFieldName() {
        return throwableMessageFieldName;
    }

    public String getStackTraceFieldName() {
        return stackTraceFieldName;
    }

    public String getSourceTypeNameFieldName() {
        return sourceTypeNameFieldName;
    }

    public String getThreadNameFieldName() {
        return threadNameFieldName;
    }

    public String getContextFieldName() {
        return contextFieldName;
    }

    public String getTimestampFieldName() {
        return timestampFieldName;
    }

    public String getTimestampLongPointName() {
        return timestampLongPointName;
    }

    public int getStackTraceSerializationMaxLength() {
        return stackTraceSerializationMaxLength;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LuceneExceptionStoreConfiguration that = (LuceneExceptionStoreConfiguration) o;
        return enabled == that.enabled &&
                stackTraceSerializationMaxLength == that.stackTraceSerializationMaxLength &&
                Objects.equals(discriminator, that.discriminator) &&
                Objects.equals(idFieldName, that.idFieldName) &&
                Objects.equals(idIntPointName, that.idIntPointName) &&
                Objects.equals(discriminatorFieldName, that.discriminatorFieldName) &&
                Objects.equals(messageFieldName, that.messageFieldName) &&
                Objects.equals(throwableTypeNameFieldName, that.throwableTypeNameFieldName) &&
                Objects.equals(stackTraceHashFieldName, that.stackTraceHashFieldName) &&
                Objects.equals(stackTraceHashIntPointName, that.stackTraceHashIntPointName) &&
                Objects.equals(throwableMessageFieldName, that.throwableMessageFieldName) &&
                Objects.equals(stackTraceFieldName, that.stackTraceFieldName) &&
                Objects.equals(sourceTypeNameFieldName, that.sourceTypeNameFieldName) &&
                Objects.equals(threadNameFieldName, that.threadNameFieldName) &&
                Objects.equals(contextFieldName, that.contextFieldName) &&
                Objects.equals(timestampFieldName, that.timestampFieldName) &&
                Objects.equals(timestampLongPointName, that.timestampLongPointName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                discriminator,
                idFieldName,
                idIntPointName,
                discriminatorFieldName,
                messageFieldName,
                throwableTypeNameFieldName,
                stackTraceHashFieldName,
                stackTraceHashIntPointName,
                throwableMessageFieldName,
                stackTraceFieldName,
                sourceTypeNameFieldName,
                threadNameFieldName,
                contextFieldName,
                timestampFieldName,
                timestampLongPointName,
                stackTraceSerializationMaxLength);
    }

    @Override
    public String toString() {
        return "LuceneExceptionStoreConfiguration{" +
                "enabled=" + enabled +
                ", discriminator='" + discriminator + '\'' +
                ", idFieldName='" + idFieldName + '\'' +
                ", idIntPointName='" + idIntPointName + '\'' +
                ", discriminatorFieldName='" + discriminatorFieldName + '\'' +
                ", messageFieldName='" + messageFieldName + '\'' +
                ", throwableTypeNameFieldName='" + throwableTypeNameFieldName + '\'' +
                ", stackTraceHashFieldName='" + stackTraceHashFieldName + '\'' +
                ", stackTraceHashIntPointName='" + stackTraceHashIntPointName + '\'' +
                ", throwableMessageFieldName='" + throwableMessageFieldName + '\'' +
                ", stackTraceFieldName='" + stackTraceFieldName + '\'' +
                ", sourceTypeNameFieldName='" + sourceTypeNameFieldName + '\'' +
                ", threadNameFieldName='" + threadNameFieldName + '\'' +
                ", contextFieldName='" + contextFieldName + '\'' +
                ", timestampFieldName='" + timestampFieldName + '\'' +
                ", timestampLongPointName='" + timestampLongPointName + '\'' +
                ", stackTraceSerializationMaxLength=" + stackTraceSerializationMaxLength +
                '}';
    }

    /**
     * Builder for {@link LuceneExceptionStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = false;
        private static final String DEFAULT_DISCRIMINATOR = "discriminator";
        private static final String DEFAULT_ID_FIELD_NAME = "id";
        private static final String DEFAULT_ID_INT_POINT_NAME = "idIntPoint";
        private static final String DEFAULT_DISCRIMINATOR_FIELD_NAME = "discriminator";
        private static final String DEFAULT_MESSAGE_FIELD_NAME = "message";
        private static final String DEFAULT_THROWABLE_TYPE_NAME_FIELD_NAME = "throwableTypeName";
        private static final String DEFAULT_STACK_TRACE_HASH_FIELD_NAME = "stackTraceHash";
        private static final String DEFAULT_STACK_TRACE_HASH_INT_POINT_NAME = "stackTraceHashIntPoint";
        private static final String DEFAULT_THROWABLE_MESSAGE_FIELD_NAME = "throwableMessage";
        private static final String DEFAULT_STACK_TRACE_FIELD_NAME = "stackTrace";
        private static final String DEFAULT_SOURCE_TYPE_NAME_FIELD_NAME = "sourceTypeName";
        private static final String DEFAULT_THREAD_NAME_FIELD_NAME = "threadName";
        private static final String DEFAULT_CONTEXT_FIELD_NAME = "context";
        private static final String DEFAULT_TIMESTAMP_FIELD_NAME = "timestamp";
        private static final String DEFAULT_TIMESTAMP_LONG_POINT_NAME = "timestampLongPoint";

        private boolean enabled = DEFAULT_ENABLED;
        private String discriminator = DEFAULT_DISCRIMINATOR;
        private String idFieldName = DEFAULT_ID_FIELD_NAME;
        private String idIntPointName = DEFAULT_ID_INT_POINT_NAME;
        private String discriminatorFieldName = DEFAULT_DISCRIMINATOR_FIELD_NAME;
        private String messageFieldName = DEFAULT_MESSAGE_FIELD_NAME;
        private String throwableTypeNameFieldName = DEFAULT_THROWABLE_TYPE_NAME_FIELD_NAME;
        private String stackTraceHashFieldName = DEFAULT_STACK_TRACE_HASH_FIELD_NAME;
        private String stackTraceHashIntPointName = DEFAULT_STACK_TRACE_HASH_INT_POINT_NAME;
        private String throwableMessageFieldName = DEFAULT_THROWABLE_MESSAGE_FIELD_NAME;
        private String stackTraceFieldName = DEFAULT_STACK_TRACE_FIELD_NAME;
        private String sourceTypeNameFieldName = DEFAULT_SOURCE_TYPE_NAME_FIELD_NAME;
        private String threadNameFieldName = DEFAULT_THREAD_NAME_FIELD_NAME;
        private String contextFieldName = DEFAULT_CONTEXT_FIELD_NAME;
        private String timestampFieldName = DEFAULT_TIMESTAMP_FIELD_NAME;
        private String timestampLongPointName = DEFAULT_TIMESTAMP_LONG_POINT_NAME;
        private int stackTraceSerializationMaxLength = ExceptionStore.DEFAULT_STACK_TRACE_SERIALIZATION_MAX_LENGTH;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder discriminator(final String discriminator) {
            this.discriminator = discriminator;
            return this;
        }

        public Builder idFieldName(final String idFieldName) {
            this.idFieldName = idFieldName;
            return this;
        }

        public Builder idIntPointName(final String idIntPointName) {
            this.idIntPointName = idIntPointName;
            return this;
        }

        public Builder discriminatorFieldName(final String discriminatorFieldName) {
            this.discriminatorFieldName = discriminatorFieldName;
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

        public Builder stackTraceHashFieldName(final String stackTraceHashFieldName) {
            this.stackTraceHashFieldName = stackTraceHashFieldName;
            return this;
        }

        public Builder stackTraceHashIntPointName(final String stackTraceHashIntPointName) {
            this.stackTraceHashIntPointName = stackTraceHashIntPointName;
            return this;
        }

        public Builder throwableMessageFieldName(final String throwableMessageFieldName) {
            this.throwableMessageFieldName = throwableMessageFieldName;
            return this;
        }

        public Builder stackTraceFieldName(final String stackTraceFieldName) {
            this.stackTraceFieldName = stackTraceFieldName;
            return this;
        }

        public Builder sourceTypeNameFieldName(final String sourceTypeNameFieldName) {
            this.sourceTypeNameFieldName = sourceTypeNameFieldName;
            return this;
        }

        public Builder threadNameFieldName(final String threadNameFieldName) {
            this.threadNameFieldName = threadNameFieldName;
            return this;
        }

        public Builder contextFieldName(final String contextFieldName) {
            this.contextFieldName = contextFieldName;
            return this;
        }

        public Builder timestampFieldName(final String timestampFieldName) {
            this.timestampFieldName = timestampFieldName;
            return this;
        }

        public Builder timestampLongPointName(final String timestampLongPointName) {
            this.timestampLongPointName = timestampLongPointName;
            return this;
        }

        public Builder stackTraceSerializationMaxLength(final int stackTraceSerializationMaxLength) {
            this.stackTraceSerializationMaxLength = stackTraceSerializationMaxLength;
            return this;
        }

        public LuceneExceptionStoreConfiguration build() {
            return new LuceneExceptionStoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link LuceneExceptionStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = LuceneExceptionStoreConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.exception.store.lucene.LuceneExceptionStoreConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
