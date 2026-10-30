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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.NameResolver;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link NameResolver.Factory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = NameResolverFactoryConfiguration.Builder.class)
public final class NameResolverFactoryConfiguration {
    private final InjectedTypeConfiguration<? extends NameResolver.Factory> nameResolverInjectedTypeConfiguration;

    private NameResolverFactoryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.nameResolverInjectedTypeConfiguration, "builder.nameResolverInjectedTypeConfiguration");

        nameResolverInjectedTypeConfiguration = builder.nameResolverInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InjectedTypeConfiguration<? extends NameResolver.Factory> getNameResolverInjectedTypeConfiguration() {
        return nameResolverInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NameResolverFactoryConfiguration that = (NameResolverFactoryConfiguration) o;
        return Objects.equals(nameResolverInjectedTypeConfiguration, that.nameResolverInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameResolverInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "NameResolverFactoryConfiguration{" +
                "nameResolverInjectedTypeConfiguration=" + nameResolverInjectedTypeConfiguration +
                '}';
    }

    /**
     * Builder for {@link NameResolverFactoryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private InjectedTypeConfiguration<? extends NameResolver.Factory> nameResolverInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder nameResolverInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends NameResolver.Factory> nameResolverInjectedTypeConfiguration)
        {
            this.nameResolverInjectedTypeConfiguration = nameResolverInjectedTypeConfiguration;
            return this;
        }

        public NameResolverFactoryConfiguration build() {
            return new NameResolverFactoryConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
