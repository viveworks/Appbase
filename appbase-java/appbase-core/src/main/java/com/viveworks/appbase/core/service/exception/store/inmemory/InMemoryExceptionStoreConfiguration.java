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

package com.viveworks.appbase.core.service.exception.store.inmemory;

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
 * Configuration for {@link InMemoryExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@InMemoryExceptionStoreConfiguration.Valid
@JsonDeserialize(builder = InMemoryExceptionStoreConfiguration.Builder.class)
public final class InMemoryExceptionStoreConfiguration implements ExceptionStoreConfiguration {
    private final boolean enabled;
    private final String discriminator;
    private final int stackTraceSerializationMaxLength;

    private InMemoryExceptionStoreConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        InMemoryExceptionStoreConfigurationValidator.validate(builder.stackTraceSerializationMaxLength);

        enabled = builder.enabled;
        discriminator = Objects.requireNonNullElse(builder.discriminator, "");
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

    public boolean isEnabled() {
        return enabled;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public int getStackTraceSerializationMaxLength() {
        return stackTraceSerializationMaxLength;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends ExceptionStore> Class<S> getExceptionStoreType() {
        return (Class<S>) InMemoryExceptionStore.class;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InMemoryExceptionStoreConfiguration)) {
            return false;
        }

        final InMemoryExceptionStoreConfiguration that = (InMemoryExceptionStoreConfiguration) o;
        return enabled == that.enabled &&
                stackTraceSerializationMaxLength == that.stackTraceSerializationMaxLength &&
                Objects.equals(discriminator, that.discriminator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, discriminator, stackTraceSerializationMaxLength);
    }

    @Override
    public String toString() {
        return "InMemoryExceptionStoreConfiguration{" +
                "enabled=" + enabled +
                ", discriminator='" + discriminator + '\'' +
                ", stackTraceSerializationMaxLength=" + stackTraceSerializationMaxLength +
                '}';
    }

    /**
     * {@link InMemoryExceptionStoreConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final String DEFAULT_DISCRIMINATOR = "discriminator";

        private boolean enabled = DEFAULT_ENABLED;
        private String discriminator = DEFAULT_DISCRIMINATOR;
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

        public Builder stackTraceSerializationMaxLength(final int stackTraceSerializationMaxLength) {
            this.stackTraceSerializationMaxLength = stackTraceSerializationMaxLength;
            return this;
        }

        public InMemoryExceptionStoreConfiguration build() {
            return new InMemoryExceptionStoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link InMemoryExceptionStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = InMemoryExceptionStoreConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.exception.store.inmemory." +
                    "InMemoryExceptionStoreConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
