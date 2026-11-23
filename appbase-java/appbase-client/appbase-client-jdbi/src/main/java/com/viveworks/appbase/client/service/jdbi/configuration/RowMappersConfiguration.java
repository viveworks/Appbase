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

/**
 * This package contains client services for {@code JDBC} (Java Database Connectivity).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.service.jdbi.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.RowMapperFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.RowMappersFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.mapper.RowMappers;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for {@link RowMappers}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RowMappersConfiguration.Builder.class)
public final class RowMappersConfiguration {
    private final InjectedTypeConfiguration<RowMappersFactory> rowMappersFactoryInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<RowMapperFactoriesFactory> rowMapperFactoriesFactoryInjectedTypeConfiguration;

    @JsonSerialize(contentAs = TypeAndRowMapperConfiguration.class)
    @JsonDeserialize(contentAs = TypeAndRowMapperConfiguration.class)
    private final List<TypeAndRowMapperConfiguration<?>> typeAndRowMapperConfigurations;

    @JsonSerialize(contentAs = GenericTypeAndRowMapperConfiguration.class)
    @JsonDeserialize(contentAs = GenericTypeAndRowMapperConfiguration.class)
    private final List<GenericTypeAndRowMapperConfiguration<?>> genericTypeAndRowMapperConfigurations;

    private RowMappersConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        rowMappersFactoryInjectedTypeConfiguration = builder.rowMappersFactoryInjectedTypeConfiguration;
        rowMapperFactoriesFactoryInjectedTypeConfiguration = builder.rowMapperFactoriesFactoryInjectedTypeConfiguration;
        typeAndRowMapperConfigurations = builder.typeAndRowMapperConfigurations;
        genericTypeAndRowMapperConfigurations = builder.genericTypeAndRowMapperConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InjectedTypeConfiguration<RowMappersFactory> getRowMappersFactoryInjectedTypeConfiguration() {
        return rowMappersFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<RowMapperFactoriesFactory> getRowMapperFactoriesFactoryInjectedTypeConfiguration() {
        return rowMapperFactoriesFactoryInjectedTypeConfiguration;
    }

    public List<TypeAndRowMapperConfiguration<?>> getTypeAndRowMapperConfigurations() {
        return typeAndRowMapperConfigurations;
    }

    public List<GenericTypeAndRowMapperConfiguration<?>> getGenericTypeAndRowMapperConfigurations() {
        return genericTypeAndRowMapperConfigurations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RowMappersConfiguration that = (RowMappersConfiguration) o;
        return Objects.equals(
                rowMappersFactoryInjectedTypeConfiguration, that.rowMappersFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        rowMapperFactoriesFactoryInjectedTypeConfiguration,
                        that.rowMapperFactoriesFactoryInjectedTypeConfiguration) &&
                Objects.equals(typeAndRowMapperConfigurations, that.typeAndRowMapperConfigurations) &&
                Objects.equals(genericTypeAndRowMapperConfigurations, that.genericTypeAndRowMapperConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                rowMappersFactoryInjectedTypeConfiguration,
                rowMapperFactoriesFactoryInjectedTypeConfiguration,
                typeAndRowMapperConfigurations,
                genericTypeAndRowMapperConfigurations);
    }

    @Override
    public String toString() {
        return "RowMappersConfiguration{" +
                "rowMappersFactoryInjectedTypeConfiguration=" + rowMappersFactoryInjectedTypeConfiguration +
                ", rowMapperFactoriesFactoryInjectedTypeConfiguration=" +
                rowMapperFactoriesFactoryInjectedTypeConfiguration +
                ", typeAndRowMapperConfigurations=" + typeAndRowMapperConfigurations +
                ", genericTypeAndRowMapperConfigurations=" + genericTypeAndRowMapperConfigurations +
                '}';
    }

    /**
     * Builder for {@link RowMappersConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private InjectedTypeConfiguration<RowMappersFactory> rowMappersFactoryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<RowMapperFactoriesFactory> rowMapperFactoriesFactoryInjectedTypeConfiguration;

        @JsonSerialize(contentAs = TypeAndRowMapperConfiguration.class)
        @JsonDeserialize(contentAs = TypeAndRowMapperConfiguration.class)
        private List<TypeAndRowMapperConfiguration<?>> typeAndRowMapperConfigurations;

        @JsonSerialize(contentAs = GenericTypeAndRowMapperConfiguration.class)
        @JsonDeserialize(contentAs = GenericTypeAndRowMapperConfiguration.class)
        private List<GenericTypeAndRowMapperConfiguration<?>> genericTypeAndRowMapperConfigurations;

        private Builder() {
        }

        public Builder rowMappersFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<RowMappersFactory> rowMappersFactoryInjectedTypeConfiguration)
        {
            this.rowMappersFactoryInjectedTypeConfiguration = rowMappersFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder rowMapperFactoriesFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<RowMapperFactoriesFactory>
                        rowMapperFactoriesFactoryInjectedTypeConfiguration)
        {
            this.rowMapperFactoriesFactoryInjectedTypeConfiguration = rowMapperFactoriesFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder typeAndRowMapperConfigurations(
                @JsonSerialize(contentAs = TypeAndRowMapperConfiguration.class)
                @JsonDeserialize(contentAs = TypeAndRowMapperConfiguration.class)
                final List<TypeAndRowMapperConfiguration<?>> typeAndRowMapperConfigurations)
        {
            this.typeAndRowMapperConfigurations = typeAndRowMapperConfigurations;
            return this;
        }

        public Builder genericTypeAndRowMapperConfigurations(
                @JsonSerialize(contentAs = GenericTypeAndRowMapperConfiguration.class)
                @JsonDeserialize(contentAs = GenericTypeAndRowMapperConfiguration.class)
                final List<GenericTypeAndRowMapperConfiguration<?>> genericTypeAndRowMapperConfigurations)
        {
            this.genericTypeAndRowMapperConfigurations = genericTypeAndRowMapperConfigurations;
            return this;
        }

        public RowMappersConfiguration build() {
            return new RowMappersConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
