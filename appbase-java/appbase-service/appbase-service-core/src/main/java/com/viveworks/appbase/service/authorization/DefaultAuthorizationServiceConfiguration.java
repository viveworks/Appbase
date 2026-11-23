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

package com.viveworks.appbase.service.authorization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.Immutable;

/**
 * Configuration for {@link DefaultAuthorizationService}. It carries no state.
 *
 * <p>This class previously declared two maps, {@code allowedOperationByServiceTypeNameMap} and
 * {@code blockedOperationByServiceTypeNameMap}, intended for YAML-driven authorization rules. They
 * were deleted (pre-1.0.0, no users, no migration) because all three of the following were true and
 * made them not merely unused but unusable:
 * <ul>
 *   <li>{@link DefaultAuthorizationService} never read them. It maintains its own allow/block maps
 *       and populates them only through {@code addAllowedService*(...)} /
 *       {@code addBlockedService*(...)}, never from this configuration.</li>
 *   <li>The builder's ternary was inverted: {@code field = builder.field == null ? new
 *       ConcurrentHashMap<>() : null}. Supplying a map stored {@code null}; omitting one stored an
 *       empty map. The getter therefore returned {@code null} exactly when an operator had
 *       configured something.</li>
 *   <li>The type did not match. These were {@code Map<String, String>}, while the service's
 *       internal maps are {@code Map<String, Map<String, String>>}, so they could not have been
 *       wired to the service even if someone had tried.</li>
 * </ul>
 *
 * <p>Anyone re-adding YAML-driven authorization configuration must actually wire it into
 * {@link DefaultAuthorizationService} and match the service's
 * {@code Map<String, Map<String, String>>} shape. The class, its {@link Builder}, {@code builder()},
 * and the {@code @JsonDeserialize(builder = ...)} contract are retained because this remains the
 * service's {@link AppbaseConfiguration}, passed to {@code super(...)}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAuthorizationServiceConfiguration.Builder.class)
public class DefaultAuthorizationServiceConfiguration implements AppbaseConfiguration {

    private DefaultAuthorizationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
    }

    /**
     * Creates a builder for this stateless configuration.
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Compares by class only: this configuration holds no state, so any two instances of the same class are equal. */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        return o != null && getClass() == o.getClass();
    }

    /** Computes a hash consistent with DefaultAuthorizationServiceConfiguration equality. */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Formats the DefaultAuthorizationServiceConfiguration state for diagnostics. */
    @Override
    public String toString() {
        return "DefaultAuthorizationServiceConfiguration{}";
    }

    /**
     * Builds the stateless {@link DefaultAuthorizationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Builder() {
        }

        /**
         * Creates a new stateless configuration.
         * @return a new configuration instance
         */
        public DefaultAuthorizationServiceConfiguration build() {
            return new DefaultAuthorizationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
