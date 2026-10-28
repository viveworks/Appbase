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

package com.viveworks.appbase.core.service.registry;

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
 * Configuration for {@link DefaultServiceRegistryConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultServiceRegistryConfiguration.Valid
@JsonDeserialize(builder = DefaultServiceRegistryConfiguration.Builder.class)
public final class DefaultServiceRegistryConfiguration implements ServiceRegistryConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultServiceRegistryConfiguration";

    private final String appName;

    private final boolean enablePolling;

    private final boolean registerAsConfigurationListener;

    @Nullable
    private final SchedulableConfiguration pollingSchedulableConfiguration;

    private DefaultServiceRegistryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultServiceRegistryConfigurationValidator.validate(
                builder.appName,
                builder.enablePolling,
                builder.registerAsConfigurationListener,
                builder.pollingSchedulableConfiguration);

        appName = builder.appName;
        enablePolling = builder.enablePolling;
        registerAsConfigurationListener = builder.registerAsConfigurationListener;
        pollingSchedulableConfiguration = builder.pollingSchedulableConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getAppName() {
        return appName;
    }

    public boolean isEnablePolling() {
        return enablePolling;
    }

    public boolean isRegisterAsConfigurationListener() {
        return registerAsConfigurationListener;
    }

    @Nullable
    public SchedulableConfiguration getPollingSchedulableConfiguration() {
        return pollingSchedulableConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultServiceRegistryConfiguration that = (DefaultServiceRegistryConfiguration) o;
        return enablePolling == that.enablePolling &&
                registerAsConfigurationListener == that.registerAsConfigurationListener &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(pollingSchedulableConfiguration, that.pollingSchedulableConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, enablePolling, registerAsConfigurationListener, pollingSchedulableConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultServiceRegistryConfiguration{" +
                "appName='" + appName + '\'' +
                ", enablePolling=" + enablePolling +
                ", registerAsConfigurationListener=" + registerAsConfigurationListener +
                ", pollingSchedulableConfiguration=" + pollingSchedulableConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultServiceRegistryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_APP_NAME = "Appbase";
        private static final boolean DEFAULT_ENABLE_POLLING = true;
        private static final boolean DEFAULT_REGISTER_AS_CONFIGURATION_LISTENER = true;
        private static final SchedulableConfiguration DEFAULT_POLLING_SCHEDULABLE_CONFIGURATION =
                SchedulableConfiguration.builder().build();

        private String appName = DEFAULT_APP_NAME;
        private boolean enablePolling = DEFAULT_ENABLE_POLLING;
        private boolean registerAsConfigurationListener = DEFAULT_REGISTER_AS_CONFIGURATION_LISTENER;
        private SchedulableConfiguration pollingSchedulableConfiguration = DEFAULT_POLLING_SCHEDULABLE_CONFIGURATION;

        private Builder() {
        }

        public Builder appName(final String appName) {
            this.appName = appName;
            return this;
        }

        public Builder enablePolling(final boolean enablePolling) {
            this.enablePolling = enablePolling;
            return this;
        }

        public Builder registerAsConfigurationListener(final boolean registerAsConfigurationListener) {
            this.registerAsConfigurationListener = registerAsConfigurationListener;
            return this;
        }

        public Builder pollingSchedulableConfiguration(final SchedulableConfiguration pollingSchedulableConfiguration) {
            this.pollingSchedulableConfiguration = pollingSchedulableConfiguration;
            return this;
        }

        public DefaultServiceRegistryConfiguration build() {
            return new DefaultServiceRegistryConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultServiceRegistryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultServiceRegistryConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
