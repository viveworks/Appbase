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

package com.viveworks.appbase.core.service.exception.store.jpa;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
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
 * Configuration for {@link JpaExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JpaExceptionStoreConfiguration.Valid
@JsonDeserialize(builder = JpaExceptionStoreConfiguration.Builder.class)
public final class JpaExceptionStoreConfiguration implements ExceptionStoreConfiguration {
    private final boolean enabled;
    private final String threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded;
    private final String discriminator;
    private final String idColumnName;
    private final String discriminatorColumnName;
    private final String messageColumnName;
    private final String throwableTypeNameColumnName;
    private final String stackTraceHashColumnName;
    private final String throwableMessageColumnName;
    private final String stackTraceColumnName;
    private final String sourceTypeNameColumnName;
    private final String threadNameColumnName;
    private final String timestampColumnName;
    private final int stackTraceSerializationMaxLength;
    private final EntityManagerConfiguration entityManagerConfiguration;

    private JpaExceptionStoreConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        JpaExceptionStoreConfigurationValidator.validate(
                builder.enabled,
                builder.threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded,
                builder.discriminator,
                builder.idColumnName,
                builder.discriminatorColumnName,
                builder.messageColumnName,
                builder.throwableTypeNameColumnName,
                builder.stackTraceHashColumnName,
                builder.throwableMessageColumnName,
                builder.stackTraceColumnName,
                builder.sourceTypeNameColumnName,
                builder.threadNameColumnName,
                builder.timestampColumnName,
                builder.stackTraceSerializationMaxLength,
                builder.entityManagerConfiguration);

        enabled = builder.enabled;
        threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded =
                builder.threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded;
        discriminator = Objects.requireNonNullElse(builder.discriminator, "");
        idColumnName = builder.idColumnName;
        discriminatorColumnName = builder.discriminatorColumnName;
        messageColumnName = builder.messageColumnName;
        throwableTypeNameColumnName = builder.throwableTypeNameColumnName;
        stackTraceHashColumnName = builder.stackTraceHashColumnName;
        throwableMessageColumnName = builder.throwableMessageColumnName;
        stackTraceColumnName = builder.stackTraceColumnName;
        sourceTypeNameColumnName = builder.sourceTypeNameColumnName;
        threadNameColumnName = builder.threadNameColumnName;
        timestampColumnName = builder.timestampColumnName;
        stackTraceSerializationMaxLength = builder.stackTraceSerializationMaxLength;
        entityManagerConfiguration = builder.entityManagerConfiguration;
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

    public String getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded() {
        return threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    public String getDiscriminatorColumnName() {
        return discriminatorColumnName;
    }

    public String getMessageColumnName() {
        return messageColumnName;
    }

    public String getThrowableTypeNameColumnName() {
        return throwableTypeNameColumnName;
    }

    public String getStackTraceHashColumnName() {
        return stackTraceHashColumnName;
    }

    public String getThrowableMessageColumnName() {
        return throwableMessageColumnName;
    }

    public String getStackTraceColumnName() {
        return stackTraceColumnName;
    }

    public String getSourceTypeNameColumnName() {
        return sourceTypeNameColumnName;
    }

    public String getThreadNameColumnName() {
        return threadNameColumnName;
    }

    public String getTimestampColumnName() {
        return timestampColumnName;
    }

    public int getStackTraceSerializationMaxLength() {
        return stackTraceSerializationMaxLength;
    }

    public EntityManagerConfiguration getEntityManagerConfiguration() {
        return entityManagerConfiguration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends ExceptionStore> Class<S> getExceptionStoreType() {
        return (Class<S>) JpaExceptionStore.class;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JpaExceptionStoreConfiguration that = (JpaExceptionStoreConfiguration) o;
        return Objects.equals(enabled, that.enabled) &&
                Objects.equals(discriminator, that.discriminator) &&
                Objects.equals(
                        threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded,
                        that.threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded) &&
                Objects.equals(idColumnName, that.idColumnName) &&
                Objects.equals(discriminatorColumnName, that.discriminatorColumnName) &&
                Objects.equals(messageColumnName, that.messageColumnName) &&
                Objects.equals(throwableTypeNameColumnName, that.throwableTypeNameColumnName) &&
                Objects.equals(stackTraceHashColumnName, that.stackTraceHashColumnName) &&
                Objects.equals(throwableMessageColumnName, that.throwableMessageColumnName) &&
                Objects.equals(stackTraceColumnName, that.stackTraceColumnName) &&
                Objects.equals(sourceTypeNameColumnName, that.sourceTypeNameColumnName) &&
                Objects.equals(threadNameColumnName, that.threadNameColumnName) &&
                Objects.equals(timestampColumnName, that.timestampColumnName) &&
                Objects.equals(stackTraceSerializationMaxLength, that.stackTraceSerializationMaxLength) &&
                Objects.equals(entityManagerConfiguration, that.entityManagerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded,
                discriminator,
                idColumnName,
                discriminatorColumnName,
                messageColumnName,
                throwableTypeNameColumnName,
                stackTraceHashColumnName,
                throwableMessageColumnName,
                stackTraceColumnName,
                sourceTypeNameColumnName,
                threadNameColumnName,
                timestampColumnName,
                stackTraceSerializationMaxLength,
                entityManagerConfiguration);
    }

    @Override
    public String toString() {
        return "JpaExceptionStoreConfiguration{" +
                "enabled=" + enabled +
                ", threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded='" +
                threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded + '\'' +
                ", discriminator='" + discriminator + '\'' +
                ", idColumnName='" + idColumnName + '\'' +
                ", discriminatorColumnName='" + discriminatorColumnName + '\'' +
                ", messageColumnName='" + messageColumnName + '\'' +
                ", throwableTypeNameColumnName='" + throwableTypeNameColumnName + '\'' +
                ", stackTraceHashColumnName='" + stackTraceHashColumnName + '\'' +
                ", throwableMessageColumnName='" + throwableMessageColumnName + '\'' +
                ", stackTraceColumnName='" + stackTraceColumnName + '\'' +
                ", sourceTypeNameColumnName='" + sourceTypeNameColumnName + '\'' +
                ", threadNameColumnName='" + threadNameColumnName + '\'' +
                ", timestampColumnName='" + timestampColumnName + '\'' +
                ", stackTraceSerializationMaxLength=" + stackTraceSerializationMaxLength +
                ", entityManagerConfiguration=" + entityManagerConfiguration +
                '}';
    }

    /**
     * {@link JpaExceptionStoreConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL", "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION"})
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = false;
        private static final String DEFAULT_THREAD_NAME_FORMAT_IF_MEDIATOR_SHARED_EXECUTOR_IS_NOT_SINGLE_THREADED =
                "appbase-jpa-exception-store-%d";
        private static final String DEFAULT_DISCRIMINATOR = "discriminator";
        private static final String DEFAULT_ID_COLUMN_NAME = "id";
        private static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "discriminator";
        private static final String DEFAULT_MESSAGE_COLUMN_NAME = "message";
        private static final String DEFAULT_THROWABLE_TYPE_NAME_COLUMN_NAME = "throwableTypeName";
        private static final String DEFAULT_STACK_TRACE_HASH_COLUMN_NAME = "stackTraceHash";
        private static final String DEFAULT_THROWABLE_MESSAGE_COLUMN_NAME = "throwableMessage";
        private static final String DEFAULT_STACK_TRACE_COLUMN_NAME = "stackTrace";
        private static final String DEFAULT_SOURCE_TYPE_NAME_COLUMN_NAME = "sourceTypeName";
        private static final String DEFAULT_THREAD_NAME_COLUMN_NAME = "threadName";
        private static final String DEFAULT_TIMESTAMP_COLUMN_NAME = "timestamp";
        private static final EntityManagerConfiguration DEFAULT_ENTITY_MANAGER_CONFIGURATION =
                new EntityManagerConfiguration("appbase", null, null);

        private boolean enabled = DEFAULT_ENABLED;
        private String threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded =
                DEFAULT_THREAD_NAME_FORMAT_IF_MEDIATOR_SHARED_EXECUTOR_IS_NOT_SINGLE_THREADED;

        private String discriminator = DEFAULT_DISCRIMINATOR;
        private String idColumnName = DEFAULT_ID_COLUMN_NAME;
        private String discriminatorColumnName = DEFAULT_DISCRIMINATOR_COLUMN_NAME;
        private String messageColumnName = DEFAULT_MESSAGE_COLUMN_NAME;
        private String throwableTypeNameColumnName = DEFAULT_THROWABLE_TYPE_NAME_COLUMN_NAME;
        private String stackTraceHashColumnName = DEFAULT_STACK_TRACE_HASH_COLUMN_NAME;
        private String throwableMessageColumnName = DEFAULT_THROWABLE_MESSAGE_COLUMN_NAME;
        private String stackTraceColumnName = DEFAULT_STACK_TRACE_COLUMN_NAME;
        private String sourceTypeNameColumnName = DEFAULT_SOURCE_TYPE_NAME_COLUMN_NAME;
        private String threadNameColumnName = DEFAULT_THREAD_NAME_COLUMN_NAME;
        private String timestampColumnName = DEFAULT_TIMESTAMP_COLUMN_NAME;
        private int stackTraceSerializationMaxLength = ExceptionStore.DEFAULT_STACK_TRACE_SERIALIZATION_MAX_LENGTH;
        private EntityManagerConfiguration entityManagerConfiguration = DEFAULT_ENTITY_MANAGER_CONFIGURATION;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(
                final String threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded)
        {
            this.threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded =
                    threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded;
            return this;
        }

        public Builder discriminator(final String discriminator) {
            this.discriminator = discriminator;
            return this;
        }

        public Builder idColumnName(final String idColumnName) {
            this.idColumnName = idColumnName;
            return this;
        }

        public Builder discriminatorColumnName(final String discriminatorColumnName) {
            this.discriminatorColumnName = discriminatorColumnName;
            return this;
        }

        public Builder messageColumnName(final String messageColumnName) {
            this.messageColumnName = messageColumnName;
            return this;
        }

        public Builder throwableTypeNameColumnName(final String throwableTypeNameColumnName) {
            this.throwableTypeNameColumnName = throwableTypeNameColumnName;
            return this;
        }

        public Builder stackTraceHashColumnName(final String stackTraceHashColumnName) {
            this.stackTraceHashColumnName = stackTraceHashColumnName;
            return this;
        }

        public Builder throwableMessageColumnName(final String throwableMessageColumnName) {
            this.throwableMessageColumnName = throwableMessageColumnName;
            return this;
        }

        public Builder stackTraceColumnName(final String stackTraceColumnName) {
            this.stackTraceColumnName = stackTraceColumnName;
            return this;
        }

        public Builder sourceTypeNameColumnName(final String sourceTypeNameColumnName) {
            this.sourceTypeNameColumnName = sourceTypeNameColumnName;
            return this;
        }

        public Builder threadNameColumnName(final String threadNameColumnName) {
            this.threadNameColumnName = threadNameColumnName;
            return this;
        }

        public Builder timestampColumnName(final String timestampColumnName) {
            this.timestampColumnName = timestampColumnName;
            return this;
        }

        public Builder stackTraceSerializationMaxLength(final int stackTraceSerializationMaxLength) {
            this.stackTraceSerializationMaxLength = stackTraceSerializationMaxLength;
            return this;
        }

        public Builder entityManagerConfiguration(final EntityManagerConfiguration entityManagerConfiguration) {
            this.entityManagerConfiguration = entityManagerConfiguration;
            return this;
        }

        public JpaExceptionStoreConfiguration build() {
            return new JpaExceptionStoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link JpaExceptionStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = JpaExceptionStoreConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStoreConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
