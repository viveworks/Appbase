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

package com.viveworks.appbase.core.service.exception;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessorConfiguration;
import com.viveworks.appbase.core.service.exception.store.DefaultExpiredMessageCleanerConfiguration;
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
import java.util.Objects;

/**
 * Default implementation of {@link ExceptionServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultExceptionServiceConfiguration.Builder.class)
@DefaultExceptionServiceConfiguration.Valid
public final class DefaultExceptionServiceConfiguration implements ExceptionServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultExceptionServiceConfiguration";

    @NotNull
    private final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration;

    @NotNull
    private final DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration;

    private DefaultExceptionServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultExceptionServiceConfigurationValidator.validate(
                builder.defaultExceptionAccessorConfiguration,
                builder.defaultExpiredMessageCleanerConfiguration);

        defaultExceptionAccessorConfiguration = builder.defaultExceptionAccessorConfiguration;
        defaultExpiredMessageCleanerConfiguration = builder.defaultExpiredMessageCleanerConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultExceptionAccessorConfiguration getDefaultExceptionAccessorConfiguration() {
        return defaultExceptionAccessorConfiguration;
    }

    public DefaultExpiredMessageCleanerConfiguration getDefaultExpiredMessageCleanerConfiguration() {
        return defaultExpiredMessageCleanerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultExceptionServiceConfiguration)) {
            return false;
        }

        final DefaultExceptionServiceConfiguration that = (DefaultExceptionServiceConfiguration) o;
        return Objects.equals(defaultExceptionAccessorConfiguration, that.defaultExceptionAccessorConfiguration) &&
                Objects.equals(
                        defaultExpiredMessageCleanerConfiguration, that.defaultExpiredMessageCleanerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultExceptionAccessorConfiguration, defaultExpiredMessageCleanerConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultExceptionServiceConfiguration{" +
                "defaultExceptionAccessorConfiguration=" + defaultExceptionAccessorConfiguration +
                ", defaultExpiredMessageCleanerConfiguration=" + defaultExpiredMessageCleanerConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultExceptionServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private static final DefaultExceptionAccessorConfiguration DEFAULT_DEFAULT_EXCEPTION_ACCESSOR_CONFIGURATION =
                DefaultExceptionAccessorConfiguration.builder().build();
        private static final DefaultExpiredMessageCleanerConfiguration DEFAULT_DEFAULT_OLD_MESSAGES_CLEANER_CONFIGURATION =
                DefaultExpiredMessageCleanerConfiguration.builder().build();

        private DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration =
                DEFAULT_DEFAULT_EXCEPTION_ACCESSOR_CONFIGURATION;
        private DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration =
                DEFAULT_DEFAULT_OLD_MESSAGES_CLEANER_CONFIGURATION;

        private Builder() {
        }

        public Builder defaultExceptionAccessorConfiguration(
                final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration)
        {
            this.defaultExceptionAccessorConfiguration = defaultExceptionAccessorConfiguration;
            return this;
        }

        public Builder defaultExpiredMessageCleanerConfiguration(
                final DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration)
        {
            this.defaultExpiredMessageCleanerConfiguration = defaultExpiredMessageCleanerConfiguration;
            return this;
        }

        public DefaultExceptionServiceConfiguration build() {
            return new DefaultExceptionServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultExceptionServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultExceptionServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
