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

package com.viveworks.appbase.client.service.r2dbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.r2dbc.DefaultR2dbcClientIntegrationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultR2dbcClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultR2dbcClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultR2dbcClientIntegrationServiceConfiguration implements R2dbcClientIntegrationServiceConfiguration {
    @NullableConditionally(dependsOn = "R2dbcConnectionConfiguration.host")
    private final TextValueConfiguration textValueKeyConfiguration;

    private final R2dbcConnectionConfiguration r2dbcConnectionConfiguration;

    private final R2dbcPoolConfiguration r2dbcPoolConfiguration;

    // Metrics
    private final boolean driverMetricsEnabled;


    private DefaultR2dbcClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.r2dbcConnectionConfiguration, "builder.r2dbcConnectionConfiguration");
        AppbaseUtil.notNull(builder.r2dbcPoolConfiguration, "builder.r2dbcPoolConfiguration");

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        r2dbcConnectionConfiguration = builder.r2dbcConnectionConfiguration;
        r2dbcPoolConfiguration = builder.r2dbcPoolConfiguration;
        driverMetricsEnabled = builder.driverMetricsEnabled;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    public R2dbcConnectionConfiguration getR2dbcConnectionConfiguration() {
        return r2dbcConnectionConfiguration;
    }

    public R2dbcPoolConfiguration getR2dbcPoolConfiguration() {
        return r2dbcPoolConfiguration;
    }

    @Override
    public boolean isDriverMetricsEnabled() {
        return driverMetricsEnabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultR2dbcClientIntegrationServiceConfiguration that = (DefaultR2dbcClientIntegrationServiceConfiguration) o;
        return Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(r2dbcConnectionConfiguration, that.r2dbcConnectionConfiguration) &&
                Objects.equals(r2dbcPoolConfiguration, that.r2dbcPoolConfiguration) &&
                driverMetricsEnabled == that.driverMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration, r2dbcConnectionConfiguration, r2dbcPoolConfiguration, driverMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultR2dbcClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", r2dbcConnectionConfiguration=" + r2dbcConnectionConfiguration +
                ", r2dbcPoolConfiguration=" + r2dbcPoolConfiguration +
                ", driverMetricsEnabled=" + driverMetricsEnabled +
                '}';
    }



    /**
     * Builder for {@link DefaultR2dbcClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final R2dbcConnectionConfiguration DEFAULT_R2DBC_CONNECTION_CONFIGURATION =
                R2dbcConnectionConfiguration.builder().build();
        private static final R2dbcPoolConfiguration DEFAULT_R2DBC_POOL_CONFIGURATION =
                R2dbcPoolConfiguration.builder().build();


        private TextValueConfiguration textValueKeyConfiguration;
        private R2dbcConnectionConfiguration r2dbcConnectionConfiguration = DEFAULT_R2DBC_CONNECTION_CONFIGURATION;
        private R2dbcPoolConfiguration r2dbcPoolConfiguration = DEFAULT_R2DBC_POOL_CONFIGURATION;

        // Metrics
        private boolean driverMetricsEnabled = true;


        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder r2dbcConnectionConfiguration(final R2dbcConnectionConfiguration r2dbcConnectionConfiguration) {
            this.r2dbcConnectionConfiguration = r2dbcConnectionConfiguration;
            return this;
        }

        public Builder r2dbcPoolConfiguration(final R2dbcPoolConfiguration r2dbcPoolConfiguration) {
            this.r2dbcPoolConfiguration = r2dbcPoolConfiguration;
            return this;
        }

        /**
         * Set whether native R2DBC connection-pool metrics should be bridged onto Appbase's
         * shared Micrometer composite registry (&rarr; JMX). Defaults to {@code true}.
         *
         * @param driverMetricsEnabled driver metrics enabled flag
         * @return this builder
         */
        public Builder driverMetricsEnabled(final boolean driverMetricsEnabled) {
            this.driverMetricsEnabled = driverMetricsEnabled;
            return this;
        }

        public DefaultR2dbcClientIntegrationServiceConfiguration build() {
            return new DefaultR2dbcClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
