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
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
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
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultLogDeduplicator}.
 *
 * @author Navaneeth Mysore
 * @since 1.0.0
 */
@Immutable
@DefaultLogDeduplicatorConfiguration.Valid
@JsonDeserialize(builder = DefaultLogDeduplicatorConfiguration.Builder.class)
public final class DefaultLogDeduplicatorConfiguration {
    @NotNull
    private final SchedulableConfiguration duplicateLogHandlerSchedulableConfiguration;

    private DefaultLogDeduplicatorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultLogDeduplicatorConfigurationValidator.validate(builder.duplicateLogHandlerSchedulableConfiguration);

        duplicateLogHandlerSchedulableConfiguration = builder.duplicateLogHandlerSchedulableConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public SchedulableConfiguration getDuplicateLogHandlerSchedulableConfiguration() {
        return duplicateLogHandlerSchedulableConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultLogDeduplicatorConfiguration that = (DefaultLogDeduplicatorConfiguration) o;
        return Objects.equals(
                        duplicateLogHandlerSchedulableConfiguration,
                        that.duplicateLogHandlerSchedulableConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duplicateLogHandlerSchedulableConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultDuplicateLogHandlerConfiguration{" +
                "duplicateLogHandlerSchedulableConfiguration=" + duplicateLogHandlerSchedulableConfiguration +
                '}';
    }

    /**
     * {@link DefaultLogDeduplicatorConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final long DEFAULT_DUPLICATE_LOG_HANDLER_INITIAL_DELAY_SECONDS = 0L;
        private static final long DEFAULT_DUPLICATE_LOG_HANDLER_DELAY_SECONDS = 60L;
        private static final SchedulableConfiguration DEFAULT_DUPLICATE_LOG_HANDLER_SCHEDULABLE_CONFIGURATION =
                SchedulableConfiguration
                        .builder()
                        .initialDelay(DEFAULT_DUPLICATE_LOG_HANDLER_INITIAL_DELAY_SECONDS)
                        .delay(DEFAULT_DUPLICATE_LOG_HANDLER_DELAY_SECONDS)
                        .timeUnit(TimeUnit.SECONDS)
                        .build();

        private SchedulableConfiguration duplicateLogHandlerSchedulableConfiguration =
                DEFAULT_DUPLICATE_LOG_HANDLER_SCHEDULABLE_CONFIGURATION;

        private Builder() {
        }

        public Builder duplicateLogHandlerSchedulableConfiguration(
                final SchedulableConfiguration duplicateLogHandlerSchedulableConfiguration)
        {
            this.duplicateLogHandlerSchedulableConfiguration = duplicateLogHandlerSchedulableConfiguration;
            return this;
        }

        public DefaultLogDeduplicatorConfiguration build() {
            return new DefaultLogDeduplicatorConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultLogDeduplicatorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultLogDeduplicatorConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.filter.duplicate." +
                "DefaultLogDeduplicatorConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
