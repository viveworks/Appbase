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

package com.viveworks.appbase.core.service.reactive.reactor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SchedulerConfiguration;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for {@link DefaultReactorService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultReactorServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultReactorServiceConfiguration.Builder.class)
public final class DefaultReactorServiceConfiguration implements ReactorServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultReactiveServiceConfiguration";

    private final boolean enabled;

    @Nullable
    @JsonSerialize(contentAs = SchedulerConfiguration.class)
    @JsonDeserialize(contentAs = SchedulerConfiguration.class)
    private final List<SchedulerConfiguration> schedulerConfigurations;

    private DefaultReactorServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultReactorServiceConfigurationValidator.validate(
                builder.enabled,
                builder.schedulerConfigurations);

        enabled = builder.enabled;

        if (builder.schedulerConfigurations != null) {
            schedulerConfigurations = new ArrayList<>(builder.schedulerConfigurations);
        } else {
            schedulerConfigurations = null;
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

    @Nullable
    @JsonSerialize(contentAs = SchedulerConfiguration.class)
    @JsonDeserialize(contentAs = SchedulerConfiguration.class)
    public List<SchedulerConfiguration> getSchedulerConfigurations() {
        return schedulerConfigurations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultReactorServiceConfiguration that = (DefaultReactorServiceConfiguration) o;
        return enabled == that.enabled &&
                Objects.equals(schedulerConfigurations, that.schedulerConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, schedulerConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultReactorServiceConfiguration{" +
                "enabled=" + enabled +
                ", schedulerConfigurations=" + schedulerConfigurations +
                '}';
    }

    /**
     * Builder for {@link DefaultReactorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final List<SchedulerConfiguration> DEFAULT_SCHEDULER_CONFIGURATIONS =
                List.of(MediatorExecutorServiceSchedulerConfiguration.builder().build());

        private boolean enabled = DEFAULT_ENABLED;

        @JsonSerialize(contentAs = SchedulerConfiguration.class)
        @JsonDeserialize(contentAs = SchedulerConfiguration.class)
        private List<SchedulerConfiguration> schedulerConfigurations = DEFAULT_SCHEDULER_CONFIGURATIONS;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder schedulerConfigurations(
                @JsonSerialize(contentAs = SchedulerConfiguration.class)
                @JsonDeserialize(contentAs = SchedulerConfiguration.class)
                final List<SchedulerConfiguration> schedulerConfigurations)
        {
            this.schedulerConfigurations = schedulerConfigurations;
            return this;
        }

        public DefaultReactorServiceConfiguration build() {
            return new DefaultReactorServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultReactorServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultReactorServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
