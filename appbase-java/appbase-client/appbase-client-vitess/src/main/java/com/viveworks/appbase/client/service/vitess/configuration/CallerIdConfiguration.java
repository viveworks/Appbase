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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link io.vitess.proto.Vtrpc.CallerID}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = CallerIdConfiguration.Builder.class)
public final class CallerIdConfiguration {
    private final String principal;
    private final String component;
    private final String subComponent;

    private CallerIdConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        principal = builder.principal;
        component = builder.component;
        subComponent = builder.subComponent;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getPrincipal() {
        return principal;
    }

    public String getComponent() {
        return component;
    }

    public String getSubComponent() {
        return subComponent;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CallerIdConfiguration that = (CallerIdConfiguration) o;
        return Objects.equals(principal, that.principal) &&
                Objects.equals(component, that.component) &&
                Objects.equals(subComponent, that.subComponent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal, component, subComponent);
    }

    @Override
    public String toString() {
        return "CallerIdConfiguration{" +
                "principal='" + principal + '\'' +
                ", component='" + component + '\'' +
                ", subComponent='" + subComponent + '\'' +
                '}';
    }

    /**
     * Builder for {@link CallerIdConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String principal;
        private String component;
        private String subComponent;

        private Builder() {
        }

        public Builder principal(final String principal) {
            this.principal = principal;
            return this;
        }

        public Builder component(final String component) {
            this.component = component;
            return this;
        }

        public Builder subComponent(final String subComponent) {
            this.subComponent = subComponent;
            return this;
        }

        public CallerIdConfiguration build() {
            return new CallerIdConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
