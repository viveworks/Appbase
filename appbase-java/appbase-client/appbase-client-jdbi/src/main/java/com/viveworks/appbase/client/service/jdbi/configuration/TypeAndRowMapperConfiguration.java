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
import org.jdbi.v3.core.mapper.RowMapper;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Used to configure the mapping from {@link java.lang.reflect.Type} to {@link RowMapper}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 * @param <T> Type
 */
@Immutable
@JsonDeserialize(builder = TypeAndRowMapperConfiguration.Builder.class)
public final class TypeAndRowMapperConfiguration<T> {
    private final Class<T> type;
    private final InjectedTypeConfiguration<RowMapper<T>> rowMapperInjectedTypeConfiguration;

    private TypeAndRowMapperConfiguration(final Builder<T> builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.type, "builder.type");
        AppbaseUtil.notNull(builder.rowMapperInjectedTypeConfiguration, "builder.rowMapperInjectedTypeConfiguration");

        type = builder.type;
        rowMapperInjectedTypeConfiguration = builder.rowMapperInjectedTypeConfiguration;
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

    public Class<T> getType() {
        return type;
    }

    public InjectedTypeConfiguration<RowMapper<T>> getRowMapperInjectedTypeConfiguration() {
        return rowMapperInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TypeAndRowMapperConfiguration<?> that = (TypeAndRowMapperConfiguration<?>) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(rowMapperInjectedTypeConfiguration, that.rowMapperInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, rowMapperInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "TypeAndRowMapperConfiguration{" +
                "type=" + type +
                ", rowMapperInjectedTypeConfiguration=" + rowMapperInjectedTypeConfiguration +
                '}';
    }

    /**
     * Builder for {@link TypeAndRowMapperConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder<T> {
        private Class<T> type;
        private InjectedTypeConfiguration<RowMapper<T>> rowMapperInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder<T> type(final Class<T> type) {
            this.type = type;
            return this;
        }

        public Builder<T> rowMapperInjectedTypeConfiguration(
                final InjectedTypeConfiguration<RowMapper<T>> rowMapperInjectedTypeConfiguration)
        {
            this.rowMapperInjectedTypeConfiguration = rowMapperInjectedTypeConfiguration;
            return this;
        }

        public TypeAndRowMapperConfiguration<T> build() {
            return new TypeAndRowMapperConfiguration<>(this);
        }
    }
    // CheckStyle: Builder end
}
