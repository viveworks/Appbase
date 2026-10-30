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

package com.viveworks.appbase.client.core;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@code client core}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAppbaseClientCoreConfiguration.Builder.class)
public final class DefaultAppbaseClientCoreConfiguration
        extends AbstractAppbaseManagedServiceConfiguration implements AppbaseClientCoreConfiguration
{
    private final DefaultAppbaseClientCoreSharedResourcesConfiguration defaultAppbaseClientSharedResourcesConfiguration;

    private DefaultAppbaseClientCoreConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultAppbaseClientSharedResourcesConfiguration,
                "builder.defaultAppbaseClientSharedResourcesConfiguration");

        defaultAppbaseClientSharedResourcesConfiguration = builder.defaultAppbaseClientSharedResourcesConfiguration;
    }

    /**
     * Starts a builder whose shared-resource settings initially use their framework defaults.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the settings used when the core constructs its shared key and comparable-value pools.
     *
     * @return non-null shared-resource configuration captured by the builder
     */
    public DefaultAppbaseClientCoreSharedResourcesConfiguration getDefaultAppbaseClientSharedResourcesConfiguration() {
        return defaultAppbaseClientSharedResourcesConfiguration;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        final DefaultAppbaseClientCoreConfiguration that = (DefaultAppbaseClientCoreConfiguration) o;
        return Objects.equals(
                defaultAppbaseClientSharedResourcesConfiguration, that.defaultAppbaseClientSharedResourcesConfiguration);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), defaultAppbaseClientSharedResourcesConfiguration);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DefaultAppbaseClientCoreConfiguration{" +
                "defaultAppbaseClientSharedResourcesConfiguration=" + defaultAppbaseClientSharedResourcesConfiguration +
                "} " + super.toString();
    }

    /**
     * Builder for assembling client-core settings.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseManagedServiceConfiguration.Builder<Builder> {
        private static final DefaultAppbaseClientCoreSharedResourcesConfiguration
                DEFAULT_DEFAULT_APPBASE_CLIENT_SHARED_RESOURCES_CONFIGURATION =
                DefaultAppbaseClientCoreSharedResourcesConfiguration.builder().build();

        private DefaultAppbaseClientCoreSharedResourcesConfiguration defaultAppbaseClientSharedResourcesConfiguration =
                DEFAULT_DEFAULT_APPBASE_CLIENT_SHARED_RESOURCES_CONFIGURATION;

        public Builder() {
        }

        /** {@inheritDoc} */
        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultAppbaseClientSharedResourcesConfiguration(
                final DefaultAppbaseClientCoreSharedResourcesConfiguration defaultAppbaseClientSharedResourcesConfiguration)
        {
            this.defaultAppbaseClientSharedResourcesConfiguration = defaultAppbaseClientSharedResourcesConfiguration;
            return this;
        }

        public DefaultAppbaseClientCoreConfiguration build() {
            return new DefaultAppbaseClientCoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
