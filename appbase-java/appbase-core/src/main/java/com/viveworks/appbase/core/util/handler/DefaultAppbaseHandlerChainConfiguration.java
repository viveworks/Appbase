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

package com.viveworks.appbase.core.util.handler;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for {@link DefaultAppbaseHandlerChain}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAppbaseHandlerChainConfiguration.Builder.class)
public final class DefaultAppbaseHandlerChainConfiguration {
    @JsonSerialize(contentAs = InjectedTypeConfiguration.class)
    @JsonDeserialize(contentAs = InjectedTypeConfiguration.class)
    private final List<InjectedTypeConfiguration<AppbaseHandler<?>>> appbaseHandlerInjectedTypeConfigurations;

    private DefaultAppbaseHandlerChainConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notEmpty(
                builder.appbaseHandlerInjectedTypeConfigurations,
                "builder.appbaseHandlerInjectedTypeConfigurations");

        appbaseHandlerInjectedTypeConfigurations = builder.appbaseHandlerInjectedTypeConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public List<InjectedTypeConfiguration<AppbaseHandler<?>>> getAppbaseHandlerInjectedTypeConfigurations() {
        return appbaseHandlerInjectedTypeConfigurations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultAppbaseHandlerChainConfiguration that = (DefaultAppbaseHandlerChainConfiguration) o;
        return Objects.equals(appbaseHandlerInjectedTypeConfigurations, that.appbaseHandlerInjectedTypeConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appbaseHandlerInjectedTypeConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultAppbaseHandlerChainConfiguration{" +
                "appbaseHandlerInjectedTypeConfigurations=" + appbaseHandlerInjectedTypeConfigurations +
                '}';
    }

    /**
     * Builder for {@link DefaultAppbaseHandlerChainConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        @JsonSerialize(contentAs = InjectedTypeConfiguration.class)
        @JsonDeserialize(contentAs = InjectedTypeConfiguration.class)
        private List<InjectedTypeConfiguration<AppbaseHandler<?>>> appbaseHandlerInjectedTypeConfigurations;

        private Builder() {
        }

        public Builder appbaseHandlerInjectedTypeConfigurations(
                @JsonSerialize(contentAs = InjectedTypeConfiguration.class)
                @JsonDeserialize(contentAs = InjectedTypeConfiguration.class)
                final List<InjectedTypeConfiguration<AppbaseHandler<?>>> appbaseHandlerInjectedTypeConfigurations)
        {
            this.appbaseHandlerInjectedTypeConfigurations = appbaseHandlerInjectedTypeConfigurations;
            return this;
        }

        public DefaultAppbaseHandlerChainConfiguration build() {
            return new DefaultAppbaseHandlerChainConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
