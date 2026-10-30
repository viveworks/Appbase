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

package com.viveworks.appbase.core.service.exception.accessor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for {@link DefaultExceptionAccessor}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultExceptionAccessorConfiguration.Valid
@JsonDeserialize(builder = DefaultExceptionAccessorConfiguration.Builder.class)
public final class DefaultExceptionAccessorConfiguration implements ExceptionAccessorConfiguration {
    @NotNull
    @JsonSerialize(contentAs = ExceptionStoreConfiguration.class)
    @JsonDeserialize(contentAs = ExceptionStoreConfiguration.class)
    private final List<ExceptionStoreConfiguration> exceptionStoreConfigurations;

    private DefaultExceptionAccessorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultExceptionAccessorConfigurationValidator.validate(builder.exceptionStoreConfigurations);

        exceptionStoreConfigurations = builder.exceptionStoreConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public List<ExceptionStoreConfiguration> getExceptionStoreConfigurations() {
        return exceptionStoreConfigurations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultExceptionAccessorConfiguration)) {
            return false;
        }

        final DefaultExceptionAccessorConfiguration that = (DefaultExceptionAccessorConfiguration) o;
        return Objects.equals(exceptionStoreConfigurations, that.exceptionStoreConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exceptionStoreConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultExceptionAccessorConfiguration{" +
                "exceptionStoreConfigurations=" + exceptionStoreConfigurations +
                '}';
    }

    /**
     * Builder for {@link DefaultExceptionAccessorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private List<ExceptionStoreConfiguration> DEFAULT_EXCEPTION_STORE_CONFIGURATIONS =
                List.of(InMemoryExceptionStoreConfiguration.builder().build());

        @JsonSerialize(contentAs = ExceptionStoreConfiguration.class)
        @JsonDeserialize(contentAs = ExceptionStoreConfiguration.class)
        private List<ExceptionStoreConfiguration> exceptionStoreConfigurations =
                DEFAULT_EXCEPTION_STORE_CONFIGURATIONS;

        private Builder() {
        }

        public Builder exceptionStoreConfigurations(
                @JsonSerialize(contentAs = ExceptionStoreConfiguration.class)
                @JsonDeserialize(contentAs = ExceptionStoreConfiguration.class)
                final List<ExceptionStoreConfiguration> exceptionStoreConfigurations)
        {
            this.exceptionStoreConfigurations = exceptionStoreConfigurations;
            return this;
        }

        public DefaultExceptionAccessorConfiguration build() {
            return new DefaultExceptionAccessorConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultExceptionAccessorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultExceptionAccessorConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessorConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
