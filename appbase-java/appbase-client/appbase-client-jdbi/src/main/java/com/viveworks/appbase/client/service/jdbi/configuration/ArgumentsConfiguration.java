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

/**
 * This package contains client services for {@code JDBC} (Java Database Connectivity).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.service.jdbi.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.ArgumentFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.QualifiedArgumentFactoriesFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NullArgument;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link Arguments}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ArgumentsConfiguration.Builder.class)
public final class ArgumentsConfiguration {
    private final InjectedTypeConfiguration<ArgumentFactoriesFactory>
            argumentFactoriesFactoryInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<QualifiedArgumentFactoriesFactory>
            qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration;
    private final boolean bindingNullToPrimitivesPermitted;
    private final InjectedTypeConfiguration<NullArgument> untypedNullArgumentInjectedTypeConfiguration;

    private ArgumentsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        argumentFactoriesFactoryInjectedTypeConfiguration =
                builder.argumentFactoriesFactoryInjectedTypeConfiguration;
        qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration =
                builder.qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration;
        bindingNullToPrimitivesPermitted = builder.bindingNullToPrimitivesPermitted;
        untypedNullArgumentInjectedTypeConfiguration = builder.untypedNullArgumentInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InjectedTypeConfiguration<ArgumentFactoriesFactory> getArgumentFactoriesFactoryInjectedTypeConfiguration() {
        return argumentFactoriesFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<QualifiedArgumentFactoriesFactory>
    getQualifiedArgumentFactoriesFactoryInjectedTypeConfiguration()
    {
        return qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration;
    }

    public boolean isBindingNullToPrimitivesPermitted() {
        return bindingNullToPrimitivesPermitted;
    }

    public InjectedTypeConfiguration<NullArgument> getUntypedNullArgumentInjectedTypeConfiguration() {
        return untypedNullArgumentInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ArgumentsConfiguration that = (ArgumentsConfiguration) o;
        return bindingNullToPrimitivesPermitted == that.bindingNullToPrimitivesPermitted &&
                Objects.equals(
                        argumentFactoriesFactoryInjectedTypeConfiguration,
                        that.argumentFactoriesFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration,
                        that.qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        untypedNullArgumentInjectedTypeConfiguration,
                        that.untypedNullArgumentInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                argumentFactoriesFactoryInjectedTypeConfiguration,
                qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration,
                bindingNullToPrimitivesPermitted,
                untypedNullArgumentInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "ArgumentsConfiguration{" +
                "argumentFactoriesFactoryInjectedTypeConfiguration=" +
                argumentFactoriesFactoryInjectedTypeConfiguration +
                ", qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration=" +
                qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration +
                ", bindingNullToPrimitivesPermitted=" + bindingNullToPrimitivesPermitted +
                ", untypedNullArgument=" + untypedNullArgumentInjectedTypeConfiguration +
                '}';
    }

    /**
     * Builder for {@link ArgumentsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_BINDING_NULL_TO_PRIMITIVES_PERMITTED = true;

        private InjectedTypeConfiguration<ArgumentFactoriesFactory> argumentFactoriesFactoryInjectedTypeConfiguration;
        private InjectedTypeConfiguration<QualifiedArgumentFactoriesFactory>
                qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration;
        private boolean bindingNullToPrimitivesPermitted = DEFAULT_BINDING_NULL_TO_PRIMITIVES_PERMITTED;
        private InjectedTypeConfiguration<NullArgument> untypedNullArgumentInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder argumentFactoriesFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<ArgumentFactoriesFactory>
                        argumentFactoriesFactoryInjectedTypeConfiguration)
        {
            this.argumentFactoriesFactoryInjectedTypeConfiguration = argumentFactoriesFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<QualifiedArgumentFactoriesFactory>
                        qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration)
        {
            this.qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration =
                    qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder bindingNullToPrimitivesPermitted(final boolean bindingNullToPrimitivesPermitted) {
            this.bindingNullToPrimitivesPermitted = bindingNullToPrimitivesPermitted;
            return this;
        }

        public Builder untypedNullArgumentInjectedTypeConfiguration(
                final InjectedTypeConfiguration<NullArgument> untypedNullArgumentInjectedTypeConfiguration)
        {
            this.untypedNullArgumentInjectedTypeConfiguration = untypedNullArgumentInjectedTypeConfiguration;
            return this;
        }

        public ArgumentsConfiguration build() {
            return new ArgumentsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
