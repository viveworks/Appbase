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
import com.github.benmanes.caffeine.cache.Caffeine;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.StatementCustomizersFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.TemplateEngine;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link SqlStatements}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SqlStatementsConfiguration.Builder.class)
public final class SqlStatementsConfiguration {
    private final InjectedTypeConfiguration<StatementCustomizersFactory>
            statementCustomizersFactoryInjectedTypeConfiguration;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> statementAttributes;

    private final InjectedTypeConfiguration<SqlLogger> sqlLoggerInjectedTypeConfiguration;

    private final TimeoutConfiguration queryTimeoutConfiguration;

    private final InjectedTypeConfiguration<Caffeine<Object, Object>> templateCacheInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<TemplateEngine> templateEngineInjectedTypeConfiguration;

    private final boolean unusedBindingAllowed;


    private SqlStatementsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        statementCustomizersFactoryInjectedTypeConfiguration = builder.statementCustomizersFactoryInjectedTypeConfiguration;
        statementAttributes = builder.statementAttributes;
        sqlLoggerInjectedTypeConfiguration = builder.sqlLoggerInjectedTypeConfiguration;
        queryTimeoutConfiguration = builder.queryTimeoutConfiguration;
        templateCacheInjectedTypeConfiguration = builder.templateCacheInjectedTypeConfiguration;
        templateEngineInjectedTypeConfiguration = builder.templateEngineInjectedTypeConfiguration;
        unusedBindingAllowed = builder.unusedBindingAllowed;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InjectedTypeConfiguration<StatementCustomizersFactory> getStatementCustomizersFactoryInjectedTypeConfiguration() {
        return statementCustomizersFactoryInjectedTypeConfiguration;
    }

    public Map<String, String> getStatementAttributes() {
        return statementAttributes;
    }

    public InjectedTypeConfiguration<SqlLogger> getSqlLoggerInjectedTypeConfiguration() {
        return sqlLoggerInjectedTypeConfiguration;
    }

    public TimeoutConfiguration getQueryTimeoutConfiguration() {
        return queryTimeoutConfiguration;
    }

    public InjectedTypeConfiguration<Caffeine<Object, Object>> getTemplateCacheInjectedTypeConfiguration() {
        return templateCacheInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<TemplateEngine> getTemplateEngineInjectedTypeConfiguration() {
        return templateEngineInjectedTypeConfiguration;
    }

    public boolean isUnusedBindingAllowed() {
        return unusedBindingAllowed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SqlStatementsConfiguration that = (SqlStatementsConfiguration) o;
        return unusedBindingAllowed == that.unusedBindingAllowed &&
                Objects.equals(
                        statementCustomizersFactoryInjectedTypeConfiguration,
                        that.statementCustomizersFactoryInjectedTypeConfiguration) &&
                Objects.equals(statementAttributes, that.statementAttributes) &&
                Objects.equals(sqlLoggerInjectedTypeConfiguration, that.sqlLoggerInjectedTypeConfiguration) &&
                Objects.equals(queryTimeoutConfiguration, that.queryTimeoutConfiguration) &&
                Objects.equals(templateCacheInjectedTypeConfiguration, that.templateCacheInjectedTypeConfiguration) &&
                Objects.equals(templateEngineInjectedTypeConfiguration, that.templateEngineInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                statementCustomizersFactoryInjectedTypeConfiguration,
                statementAttributes,
                sqlLoggerInjectedTypeConfiguration,
                queryTimeoutConfiguration,
                templateCacheInjectedTypeConfiguration,
                templateEngineInjectedTypeConfiguration,
                unusedBindingAllowed);
    }

    @Override
    public String toString() {
        return "SqlStatementsConfiguration{" +
                "statementCustomizersFactoryInjectedTypeConfiguration=" +
                statementCustomizersFactoryInjectedTypeConfiguration +
                ", statementAttributes=" + statementAttributes +
                ", sqlLoggerInjectedTypeConfiguration=" + sqlLoggerInjectedTypeConfiguration +
                ", queryTimeoutConfiguration=" + queryTimeoutConfiguration +
                ", templateCacheInjectedTypeConfiguration=" + templateCacheInjectedTypeConfiguration +
                ", templateEngineInjectedTypeConfiguration=" + templateEngineInjectedTypeConfiguration +
                ", allowUnusedBinding=" + unusedBindingAllowed +
                '}';
    }


    /**
     * Builder for {@link SqlStatementsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final TimeoutConfiguration DEFAULT_QUERY_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .timeout(1)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final boolean DEFAULT_UNUSED_BINDING_ALLOWED = false;



        private InjectedTypeConfiguration<StatementCustomizersFactory> statementCustomizersFactoryInjectedTypeConfiguration;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> statementAttributes;

        private InjectedTypeConfiguration<SqlLogger> sqlLoggerInjectedTypeConfiguration;

        private TimeoutConfiguration queryTimeoutConfiguration = DEFAULT_QUERY_TIMEOUT_CONFIGURATION;

        private InjectedTypeConfiguration<Caffeine<Object, Object>> templateCacheInjectedTypeConfiguration;

        private InjectedTypeConfiguration<TemplateEngine> templateEngineInjectedTypeConfiguration;

        private boolean unusedBindingAllowed = DEFAULT_UNUSED_BINDING_ALLOWED;



        private Builder() {
        }

        public Builder statementCustomizersFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<StatementCustomizersFactory>
                        statementCustomizersFactoryInjectedTypeConfiguration)
        {
            this.statementCustomizersFactoryInjectedTypeConfiguration = statementCustomizersFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder statementAttributes(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> statementAttributes)
        {
            this.statementAttributes = statementAttributes;
            return this;
        }

        public Builder sqlLoggerInjectedTypeConfiguration(
                final InjectedTypeConfiguration<SqlLogger> sqlLoggerInjectedTypeConfiguration)
        {
            this.sqlLoggerInjectedTypeConfiguration = sqlLoggerInjectedTypeConfiguration;
            return this;
        }

        public Builder queryTimeoutConfiguration(final TimeoutConfiguration queryTimeoutConfiguration) {
            this.queryTimeoutConfiguration = queryTimeoutConfiguration;
            return this;
        }

        public Builder templateCacheInjectedTypeConfiguration(
                final InjectedTypeConfiguration<Caffeine<Object, Object>> templateCacheInjectedTypeConfiguration)
        {
            this.templateCacheInjectedTypeConfiguration = templateCacheInjectedTypeConfiguration;
            return this;
        }

        public Builder templateEngineInjectedTypeConfiguration(
                final InjectedTypeConfiguration<TemplateEngine> templateEngineInjectedTypeConfiguration)
        {
            this.templateEngineInjectedTypeConfiguration = templateEngineInjectedTypeConfiguration;
            return this;
        }

        public Builder unusedBindingAllowed(final boolean unusedBindingAllowed) {
            this.unusedBindingAllowed = unusedBindingAllowed;
            return this;
        }

        public SqlStatementsConfiguration build() {
            return new SqlStatementsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
