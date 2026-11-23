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
import com.viveworks.appbase.client.service.jdbi.configuration.factory.SqlArrayTypeFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.SqlArrayTypesFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.array.SqlArrayTypes;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for {@link SqlArrayTypes}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SqlArrayTypesConfiguration.Builder.class)
public final class SqlArrayTypesConfiguration {
    private final JdbiSqlArrayArgumentStrategy jdbiSqlArrayArgumentStrategy;

    private final InjectedTypeConfiguration<SqlArrayTypesFactory<?>> sqlArrayTypesFactoryInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<SqlArrayTypeFactoriesFactory>
            sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration;

    @JsonSerialize(keyAs = Class.class, contentAs = String.class)
    @JsonDeserialize(keyAs = Class.class, contentAs = String.class)
    private final Map<Class<?>, String> sqlTypeNameByElementTypeMap;


    private SqlArrayTypesConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        jdbiSqlArrayArgumentStrategy =
                builder.jdbiSqlArrayArgumentStrategy;
        sqlArrayTypesFactoryInjectedTypeConfiguration =
                builder.sqlArrayTypesFactoryInjectedTypeConfiguration;
        sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration =
                builder.sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration;
        sqlTypeNameByElementTypeMap =
                builder.sqlTypeNameByElementTypeMap;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public JdbiSqlArrayArgumentStrategy getJdbiSqlArrayArgumentStrategy() {
        return jdbiSqlArrayArgumentStrategy;
    }

    public InjectedTypeConfiguration<SqlArrayTypesFactory<?>> getSqlArrayTypesFactoryInjectedTypeConfiguration() {
        return sqlArrayTypesFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<SqlArrayTypeFactoriesFactory>
    getSqlArrayTypeFactoriesFactoryInjectedTypeConfiguration()
    {
        return sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration;
    }

    public Map<Class<?>, String> getSqlTypeNameByElementTypeMap() {
        return sqlTypeNameByElementTypeMap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SqlArrayTypesConfiguration that = (SqlArrayTypesConfiguration) o;
        return jdbiSqlArrayArgumentStrategy == that.jdbiSqlArrayArgumentStrategy &&
                Objects.equals(
                        sqlArrayTypesFactoryInjectedTypeConfiguration,
                        that.sqlArrayTypesFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration,
                        that.sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration) &&
                Objects.equals(sqlTypeNameByElementTypeMap, that.sqlTypeNameByElementTypeMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                jdbiSqlArrayArgumentStrategy,
                sqlArrayTypesFactoryInjectedTypeConfiguration,
                sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration,
                sqlTypeNameByElementTypeMap);
    }

    @Override
    public String toString() {
        return "SqlArrayTypesConfiguration{" +
                "jdbiSqlArrayArgumentStrategy=" + jdbiSqlArrayArgumentStrategy +
                ", sqlArrayTypesFactoryInjectedTypeConfiguration=" + sqlArrayTypesFactoryInjectedTypeConfiguration +
                ", sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration=" +
                sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration +
                ", sqlTypeNameByElementTypeMap=" + sqlTypeNameByElementTypeMap +
                '}';
    }

    /**
     * Builder for {@link SqlArrayTypesConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private JdbiSqlArrayArgumentStrategy jdbiSqlArrayArgumentStrategy;

        private InjectedTypeConfiguration<SqlArrayTypesFactory<?>>
                sqlArrayTypesFactoryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<SqlArrayTypeFactoriesFactory>
                sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration;

        @JsonSerialize(keyAs = Class.class, contentAs = String.class)
        @JsonDeserialize(keyAs = Class.class, contentAs = String.class)
        private Map<Class<?>, String> sqlTypeNameByElementTypeMap;


        private Builder() {
        }

        public Builder jdbiSqlArrayArgumentStrategy(final JdbiSqlArrayArgumentStrategy jdbiSqlArrayArgumentStrategy) {
            this.jdbiSqlArrayArgumentStrategy = jdbiSqlArrayArgumentStrategy;
            return this;
        }

        public Builder sqlArrayTypesFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<SqlArrayTypesFactory<?>> sqlArrayTypesFactoryInjectedTypeConfiguration)
        {
            this.sqlArrayTypesFactoryInjectedTypeConfiguration = sqlArrayTypesFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<SqlArrayTypeFactoriesFactory>
                        sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration)
        {
            this.sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration =
                    sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder sqlTypeNameByElementTypeMap(
                @JsonSerialize(keyAs = Class.class, contentAs = String.class)
                @JsonDeserialize(keyAs = Class.class, contentAs = String.class)
                final Map<Class<?>, String> sqlTypeNameByElementTypeMap)
        {
            this.sqlTypeNameByElementTypeMap = sqlTypeNameByElementTypeMap;
            return this;
        }

        public SqlArrayTypesConfiguration build() {
            return new SqlArrayTypesConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
