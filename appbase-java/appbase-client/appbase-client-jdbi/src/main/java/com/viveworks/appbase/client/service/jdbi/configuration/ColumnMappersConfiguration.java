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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.ColumnMapperFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.QualifiedColumnMapperFactoriesFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for {@link ColumnMapper}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ColumnMappersConfiguration.Builder.class)
public final class ColumnMappersConfiguration {
    private final boolean coalesceNullPrimitivesToDefaults;

    private final InjectedTypeConfiguration<ColumnMapperFactory> columnMapperFactoryInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<ColumnMapperFactoriesFactory>
            columnMapperFactoriesFactoryInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<QualifiedColumnMapperFactoriesFactory>
            qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration;

    @JsonSerialize(contentAs = TypeAndColumnMapperConfiguration.class)
    @JsonDeserialize(contentAs = TypeAndColumnMapperConfiguration.class)
    private final List<TypeAndColumnMapperConfiguration<?>> typeAndColumnMapperConfigurations;

    @JsonSerialize(contentAs = QualifiedTypeAndColumnMapperConfiguration.class)
    @JsonDeserialize(contentAs = QualifiedTypeAndColumnMapperConfiguration.class)
    private final List<QualifiedTypeAndColumnMapperConfiguration<?>> qualifiedTypeAndColumnMapperConfigurations;

    @JsonSerialize(contentAs = GenericTypeAndColumnMapperConfiguration.class)
    @JsonDeserialize(contentAs = GenericTypeAndColumnMapperConfiguration.class)
    private final List<GenericTypeAndColumnMapperConfiguration<?>> genericTypeAndColumnMapperConfigurations;


    private ColumnMappersConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        coalesceNullPrimitivesToDefaults = builder.coalesceNullPrimitivesToDefaults;
        columnMapperFactoryInjectedTypeConfiguration = builder.columnMapperFactoryInjectedTypeConfiguration;
        columnMapperFactoriesFactoryInjectedTypeConfiguration =
                builder.columnMapperFactoriesFactoryInjectedTypeConfiguration;
        qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration =
                builder.qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration;
        typeAndColumnMapperConfigurations = builder.typeAndColumnMapperConfigurations;
        qualifiedTypeAndColumnMapperConfigurations = builder.qualifiedTypeAndColumnMapperConfigurations;
        genericTypeAndColumnMapperConfigurations = builder.genericTypeAndColumnMapperConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isCoalesceNullPrimitivesToDefaults() {
        return coalesceNullPrimitivesToDefaults;
    }

    public InjectedTypeConfiguration<ColumnMapperFactory> getColumnMapperFactoryInjectedTypeConfiguration() {
        return columnMapperFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<ColumnMapperFactoriesFactory>
    getColumnMapperFactoriesFactoryInjectedTypeConfiguration()
    {
        return columnMapperFactoriesFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<QualifiedColumnMapperFactoriesFactory>
    getQualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration()
    {
        return qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration;
    }

    public List<TypeAndColumnMapperConfiguration<?>> getTypeAndColumnMapperConfigurations() {
        return typeAndColumnMapperConfigurations;
    }

    public List<QualifiedTypeAndColumnMapperConfiguration<?>> getQualifiedTypeAndColumnMapperConfigurations() {
        return qualifiedTypeAndColumnMapperConfigurations;
    }

    public List<GenericTypeAndColumnMapperConfiguration<?>> getGenericTypeAndColumnMapperConfigurations() {
        return genericTypeAndColumnMapperConfigurations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ColumnMappersConfiguration that = (ColumnMappersConfiguration) o;
        return coalesceNullPrimitivesToDefaults == that.coalesceNullPrimitivesToDefaults &&
                Objects.equals(
                        columnMapperFactoryInjectedTypeConfiguration,
                        that.columnMapperFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        columnMapperFactoriesFactoryInjectedTypeConfiguration,
                        that.columnMapperFactoriesFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration,
                        that.qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        typeAndColumnMapperConfigurations,
                        that.typeAndColumnMapperConfigurations) &&
                Objects.equals(
                        qualifiedTypeAndColumnMapperConfigurations,
                        that.qualifiedTypeAndColumnMapperConfigurations) &&
                Objects.equals(
                        genericTypeAndColumnMapperConfigurations,
                        that.genericTypeAndColumnMapperConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                coalesceNullPrimitivesToDefaults,
                columnMapperFactoryInjectedTypeConfiguration,
                columnMapperFactoriesFactoryInjectedTypeConfiguration,
                qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration,
                typeAndColumnMapperConfigurations,
                qualifiedTypeAndColumnMapperConfigurations,
                genericTypeAndColumnMapperConfigurations);
    }

    @Override
    public String toString() {
        return "ColumnMappersConfiguration{" +
                "coalesceNullPrimitivesToDefaults=" + coalesceNullPrimitivesToDefaults +
                ", columnMapperFactoryInjectedTypeConfiguration=" + columnMapperFactoryInjectedTypeConfiguration +
                ", columnMapperFactoriesFactoryInjectedTypeConfiguration=" +
                columnMapperFactoriesFactoryInjectedTypeConfiguration +
                ", qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration=" +
                qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration +
                ", typeAndColumnMapperConfigurations=" + typeAndColumnMapperConfigurations +
                ", qualifiedTypeAndColumnMapperConfigurations=" + qualifiedTypeAndColumnMapperConfigurations +
                ", genericTypeAndColumnMapperConfigurations=" + genericTypeAndColumnMapperConfigurations +
                '}';
    }


    /**
     * Builder for {@link ColumnMappersConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_COALESCE_NULL_PRIMITIVES_TO_DEFAULTS = true;



        private boolean coalesceNullPrimitivesToDefaults = DEFAULT_COALESCE_NULL_PRIMITIVES_TO_DEFAULTS;

        private InjectedTypeConfiguration<ColumnMapperFactory> columnMapperFactoryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<ColumnMapperFactoriesFactory>
                columnMapperFactoriesFactoryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<QualifiedColumnMapperFactoriesFactory>
                qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration;

        @JsonSerialize(contentAs = TypeAndColumnMapperConfiguration.class)
        @JsonDeserialize(contentAs = TypeAndColumnMapperConfiguration.class)
        private List<TypeAndColumnMapperConfiguration<?>> typeAndColumnMapperConfigurations;

        @JsonSerialize(contentAs = QualifiedTypeAndColumnMapperConfiguration.class)
        @JsonDeserialize(contentAs = QualifiedTypeAndColumnMapperConfiguration.class)
        private List<QualifiedTypeAndColumnMapperConfiguration<?>> qualifiedTypeAndColumnMapperConfigurations;

        @JsonSerialize(contentAs = GenericTypeAndColumnMapperConfiguration.class)
        @JsonDeserialize(contentAs = GenericTypeAndColumnMapperConfiguration.class)
        private List<GenericTypeAndColumnMapperConfiguration<?>> genericTypeAndColumnMapperConfigurations;



        private Builder() {
        }

        public Builder coalesceNullPrimitivesToDefaults(final boolean coalesceNullPrimitivesToDefaults) {
            this.coalesceNullPrimitivesToDefaults = coalesceNullPrimitivesToDefaults;
            return this;
        }

        public Builder columnMapperFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<ColumnMapperFactory> columnMapperFactoryInjectedTypeConfiguration)
        {
            this.columnMapperFactoryInjectedTypeConfiguration = columnMapperFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder columnMapperFactoriesFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<ColumnMapperFactoriesFactory>
                        columnMapperFactoriesFactoryInjectedTypeConfiguration)
        {
            this.columnMapperFactoriesFactoryInjectedTypeConfiguration =
                    columnMapperFactoriesFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<QualifiedColumnMapperFactoriesFactory>
                        qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration)
        {
            this.qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration =
                    qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder typeAndColumnMapperConfigurations(
                @JsonSerialize(contentAs = TypeAndColumnMapperConfiguration.class)
                @JsonDeserialize(contentAs = TypeAndColumnMapperConfiguration.class)
                final List<TypeAndColumnMapperConfiguration<?>> typeAndColumnMapperConfigurations)
        {
            this.typeAndColumnMapperConfigurations = typeAndColumnMapperConfigurations;
            return this;
        }

        public Builder qualifiedTypeAndColumnMapperConfigurations(
                @JsonSerialize(contentAs = QualifiedTypeAndColumnMapperConfiguration.class)
                @JsonDeserialize(contentAs = QualifiedTypeAndColumnMapperConfiguration.class)
                final List<QualifiedTypeAndColumnMapperConfiguration<?>> qualifiedTypeAndColumnMapperConfigurations)
        {
            this.qualifiedTypeAndColumnMapperConfigurations = qualifiedTypeAndColumnMapperConfigurations;
            return this;
        }

        public Builder genericTypeAndColumnMapperConfigurations(
                @JsonSerialize(contentAs = GenericTypeAndColumnMapperConfiguration.class)
                @JsonDeserialize(contentAs = GenericTypeAndColumnMapperConfiguration.class)
                final List<GenericTypeAndColumnMapperConfiguration<?>> genericTypeAndColumnMapperConfigurations)
        {
            this.genericTypeAndColumnMapperConfigurations = genericTypeAndColumnMapperConfigurations;
            return this;
        }

        public ColumnMappersConfiguration build() {
            return new ColumnMappersConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
