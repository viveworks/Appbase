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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Used to configure the mapping from {@link GenericType} and {@link ColumnMapper}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 * @param <T> Type
 */
@Immutable
@JsonDeserialize(builder = GenericTypeAndColumnMapperConfiguration.Builder.class)
public final class GenericTypeAndColumnMapperConfiguration<T> {
    private final InjectedTypeConfiguration<GenericType<T>> genericTypeInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<ColumnMapper<T>> columnMapperInjectedTypeConfiguration;

    private GenericTypeAndColumnMapperConfiguration(final Builder<T> builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.genericTypeInjectedTypeConfiguration, "builder.genericTypeInjectedTypeConfiguration");
        AppbaseUtil.notNull(builder.columnMapperInjectedTypeConfiguration, "builder.columnMapperInjectedTypeConfiguration");

        genericTypeInjectedTypeConfiguration = builder.genericTypeInjectedTypeConfiguration;
        columnMapperInjectedTypeConfiguration = builder.columnMapperInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @param <T> Type
     * @return Builder for this object
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public InjectedTypeConfiguration<GenericType<T>> getGenericTypeInjectedTypeConfiguration() {
        return genericTypeInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<ColumnMapper<T>> getColumnMapperInjectedTypeConfiguration() {
        return columnMapperInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GenericTypeAndColumnMapperConfiguration<?> that = (GenericTypeAndColumnMapperConfiguration<?>) o;
        return Objects.equals(genericTypeInjectedTypeConfiguration, that.genericTypeInjectedTypeConfiguration) &&
                Objects.equals(columnMapperInjectedTypeConfiguration, that.columnMapperInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genericTypeInjectedTypeConfiguration, columnMapperInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "GenericTypeAndColumnMapperConfiguration{" +
                "genericTypeInjectedTypeConfiguration=" + genericTypeInjectedTypeConfiguration +
                ", columnMapperInjectedTypeConfiguration=" + columnMapperInjectedTypeConfiguration +
                '}';
    }

    /**
     * Builder for {@link GenericTypeAndColumnMapperConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     * @param <T> Type
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder<T> {
        private InjectedTypeConfiguration<GenericType<T>> genericTypeInjectedTypeConfiguration;
        private InjectedTypeConfiguration<ColumnMapper<T>> columnMapperInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder<T> genericTypeInjectedTypeConfiguration(
                final InjectedTypeConfiguration<GenericType<T>> genericTypeInjectedTypeConfiguration)
        {
            this.genericTypeInjectedTypeConfiguration = genericTypeInjectedTypeConfiguration;
            return this;
        }

        public Builder<T> columnMapperInjectedTypeConfiguration(
                final InjectedTypeConfiguration<ColumnMapper<T>> columnMapperInjectedTypeConfiguration)
        {
            this.columnMapperInjectedTypeConfiguration = columnMapperInjectedTypeConfiguration;
            return this;
        }

        public GenericTypeAndColumnMapperConfiguration<T> build() {
            return new GenericTypeAndColumnMapperConfiguration<>(this);
        }
    }
    // CheckStyle: Builder end
}
