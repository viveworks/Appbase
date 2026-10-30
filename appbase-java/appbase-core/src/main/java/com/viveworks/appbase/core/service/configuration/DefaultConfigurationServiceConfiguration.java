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

package com.viveworks.appbase.core.service.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Configuration for {@link DefaultConfigurationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultConfigurationServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultConfigurationServiceConfiguration.Builder.class)
public final class DefaultConfigurationServiceConfiguration implements ConfigurationServiceConfiguration {
    private final boolean enableSchedulableConfigurationAccessor;

    @Nullable
    private final SchedulableConfiguration configurationAccessorSchedulableConfiguration;

    private DefaultConfigurationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultConfigurationServiceConfigurationValidator.validate(
                builder.enableSchedulableConfigurationAccessor,
                builder.configurationAccessorSchedulableConfiguration);

        enableSchedulableConfigurationAccessor = builder.enableSchedulableConfigurationAccessor;
        configurationAccessorSchedulableConfiguration = builder.configurationAccessorSchedulableConfiguration;
    }

    /**
     * Get the builder used to build this object.
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether the configuration accessor should be invoked on a schedule.
     *
     * @return whether the configuration accessor should be invoked on a schedule
     */
    public boolean isEnableSchedulableConfigurationAccessor() {
        return enableSchedulableConfigurationAccessor;
    }

    /**
     * Returns the schedule used for accessor polling.
     *
     * @return the configured schedule; it may be {@code null} when scheduled access is disabled
     */
    @Nullable
    public SchedulableConfiguration getConfigurationAccessorSchedulableConfiguration() {
        return configurationAccessorSchedulableConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultConfigurationServiceConfiguration that = (DefaultConfigurationServiceConfiguration) o;
        return Objects.equals(enableSchedulableConfigurationAccessor, that.enableSchedulableConfigurationAccessor) &&
                Objects.equals(
                        configurationAccessorSchedulableConfiguration,
                        that.configurationAccessorSchedulableConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableSchedulableConfigurationAccessor, configurationAccessorSchedulableConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultConfigurationServiceConfiguration{" +
                "enableSchedulableConfigurationAccessor=" + enableSchedulableConfigurationAccessor +
                ", configurationAccessorSchedulableConfiguration=" + configurationAccessorSchedulableConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultConfigurationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLE_SCHEDULABLE_CONFIGURATION_ACCESSOR = true;
        private static final SchedulableConfiguration DEFAULT_CONFIGURATION_ACCESSOR_SCHEDULABLE_CONFIGURATION =
                SchedulableConfiguration.builder().build();

        private boolean enableSchedulableConfigurationAccessor =
                DEFAULT_ENABLE_SCHEDULABLE_CONFIGURATION_ACCESSOR;
        private SchedulableConfiguration configurationAccessorSchedulableConfiguration =
                DEFAULT_CONFIGURATION_ACCESSOR_SCHEDULABLE_CONFIGURATION;

        private Builder() {
        }

        public Builder enableSchedulableConfigurationAccessor(final boolean enableSchedulableConfigurationAccessor) {
            this.enableSchedulableConfigurationAccessor = enableSchedulableConfigurationAccessor;
            return this;
        }

        public Builder configurationAccessorSchedulableConfiguration(
                final SchedulableConfiguration configurationAccessorSchedulableConfiguration)
        {
            this.configurationAccessorSchedulableConfiguration = configurationAccessorSchedulableConfiguration;
            return this;
        }

        public DefaultConfigurationServiceConfiguration build() {
            return new DefaultConfigurationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultConfigurationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultConfigurationServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
