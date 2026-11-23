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

package com.viveworks.appbase.client.core.pool.manager;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents disabling the feature of unregistering unhealthy and irreparable
 * {@link AppbaseClientIntegrationService}s represented by
 * {@link AppbaseClientKeyConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.Builder.class)
public final class DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration {
    private final boolean disable;
    private final AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration;

    private DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.appbaseClientKeyConfiguration, "builder.appbaseClientKeyConfiguration");

        disable = builder.disable;
        appbaseClientKeyConfiguration = builder.appbaseClientKeyConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isDisable() {
        return disable;
    }

    public AppbaseClientKeyConfiguration<?> getAppbaseClientKeyConfiguration() {
        return appbaseClientKeyConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration that =
                (DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration) o;
        return disable == that.disable &&
                Objects.equals(appbaseClientKeyConfiguration, that.appbaseClientKeyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disable, appbaseClientKeyConfiguration);
    }

    @Override
    public String toString() {
        return "DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration{" +
                "disable=" + disable +
                ", appbaseClientKeyConfiguration=" + appbaseClientKeyConfiguration +
                '}';
    }

    /**
     * Builder for {@link DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_DISABLE = true;

        private boolean disable = DEFAULT_DISABLE;
        private AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration;

        private Builder() {
        }

        public Builder disable(final boolean disable) {
            this.disable = disable;
            return this;
        }

        public Builder appbaseClientKeyConfiguration(final AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration) {
            this.appbaseClientKeyConfiguration = appbaseClientKeyConfiguration;
            return this;
        }

        public DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration build() {
            return new DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
