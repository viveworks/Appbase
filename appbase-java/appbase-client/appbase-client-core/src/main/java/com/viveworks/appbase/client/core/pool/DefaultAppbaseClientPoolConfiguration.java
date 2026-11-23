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

package com.viveworks.appbase.client.core.pool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.pool.manager.DefaultAppbaseClientPoolManager;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultAppbaseClientPoolManager}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAppbaseClientPoolConfiguration.Builder.class)
public final class DefaultAppbaseClientPoolConfiguration implements AppbaseClientPoolConfiguration {
    private final boolean repairUnhealthyServices;

    private DefaultAppbaseClientPoolConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        repairUnhealthyServices = builder.repairUnhealthyServices;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isRepairUnhealthyServices() {
        return repairUnhealthyServices;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultAppbaseClientPoolConfiguration that = (DefaultAppbaseClientPoolConfiguration) o;
        return repairUnhealthyServices == that.repairUnhealthyServices;
    }

    @Override
    public int hashCode() {
        return Objects.hash(repairUnhealthyServices);
    }

    @Override
    public String toString() {
        return "DefaultAppbaseClientPoolConfiguration{" +
                "repairUnhealthyServices=" + repairUnhealthyServices +
                '}';
    }

    /**
     * Builder for {@link DefaultAppbaseClientPoolConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_REPAIR_UNHEALTHY_SERVICES = true;

        private boolean repairUnhealthyServices = DEFAULT_REPAIR_UNHEALTHY_SERVICES;

        private Builder() {
        }

        public Builder repairUnhealthyServices(final boolean repairUnhealthyServices) {
            this.repairUnhealthyServices = repairUnhealthyServices;
            return this;
        }

        public DefaultAppbaseClientPoolConfiguration build() {
            return new DefaultAppbaseClientPoolConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
