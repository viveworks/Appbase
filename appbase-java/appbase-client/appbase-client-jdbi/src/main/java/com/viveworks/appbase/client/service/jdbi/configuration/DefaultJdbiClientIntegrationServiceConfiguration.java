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
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.jdbc.JdbcClientIntegrationService;
import com.viveworks.appbase.client.service.jdbi.DefaultJdbiClientIntegrationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.collector.CollectorFactory;
import org.jdbi.v3.core.extension.ExtensionFactory;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultJdbiClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJdbiClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultJdbiClientIntegrationServiceConfiguration implements JdbiClientIntegrationServiceConfiguration {
    private final TextValueConfiguration textValueKeyConfiguration;

    private final InjectedTypeConfiguration<JdbcClientIntegrationService>
            jdbcClientIntegrationServiceInjectedTypeConfiguration;

    private final JdbiPluginsConfiguration jdbiPluginsConfiguration;

    private final SqlStatementsConfiguration sqlStatementsConfiguration;

    private final ArgumentsConfiguration argumentsConfiguration;

    private final HandlesConfiguration handlesConfiguration;

    private final RowMappersConfiguration rowMappersConfiguration;

    private final ColumnMappersConfiguration columnMappersConfiguration;

    private final SqlArrayTypesConfiguration sqlArrayTypesConfiguration;

    private final InjectedTypeConfiguration<CodecFactory> codecFactoryInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<CollectorFactory> collectorFactoryInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<ExtensionFactory> extensionFactoryInjectedTypeConfiguration;



    private DefaultJdbiClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.textValueKeyConfiguration, "builder.textValueKeyConfiguration");

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        jdbcClientIntegrationServiceInjectedTypeConfiguration =
                builder.jdbcClientIntegrationServiceInjectedTypeConfiguration;
        jdbiPluginsConfiguration = builder.jdbiPluginsConfiguration;
        sqlStatementsConfiguration = builder.sqlStatementsConfiguration;
        argumentsConfiguration = builder.argumentsConfiguration;
        handlesConfiguration = builder.handlesConfiguration;
        rowMappersConfiguration = builder.rowMappersConfiguration;
        columnMappersConfiguration = builder.columnMappersConfiguration;
        sqlArrayTypesConfiguration = builder.sqlArrayTypesConfiguration;
        codecFactoryInjectedTypeConfiguration = builder.codecFactoryInjectedTypeConfiguration;
        collectorFactoryInjectedTypeConfiguration = builder.collectorFactoryInjectedTypeConfiguration;
        extensionFactoryInjectedTypeConfiguration = builder.extensionFactoryInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    public InjectedTypeConfiguration<JdbcClientIntegrationService>
    getJdbcClientIntegrationServiceInjectedTypeConfiguration()
    {
        return jdbcClientIntegrationServiceInjectedTypeConfiguration;
    }

    public JdbiPluginsConfiguration getJdbiPluginsConfiguration() {
        return jdbiPluginsConfiguration;
    }

    public SqlStatementsConfiguration getSqlStatementsConfiguration() {
        return sqlStatementsConfiguration;
    }

    public ArgumentsConfiguration getArgumentsConfiguration() {
        return argumentsConfiguration;
    }

    public HandlesConfiguration getHandlesConfiguration() {
        return handlesConfiguration;
    }

    public RowMappersConfiguration getRowMappersConfiguration() {
        return rowMappersConfiguration;
    }

    public ColumnMappersConfiguration getColumnMappersConfiguration() {
        return columnMappersConfiguration;
    }

    public SqlArrayTypesConfiguration getSqlArrayTypesConfiguration() {
        return sqlArrayTypesConfiguration;
    }

    public InjectedTypeConfiguration<CodecFactory> getCodecFactoryInjectedTypeConfiguration() {
        return codecFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<CollectorFactory> getCollectorFactoryInjectedTypeConfiguration() {
        return collectorFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<ExtensionFactory> getExtensionFactoryInjectedTypeConfiguration() {
        return extensionFactoryInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultJdbiClientIntegrationServiceConfiguration that = (DefaultJdbiClientIntegrationServiceConfiguration) o;
        return Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(
                        jdbcClientIntegrationServiceInjectedTypeConfiguration,
                        that.jdbcClientIntegrationServiceInjectedTypeConfiguration) &&
                Objects.equals(jdbiPluginsConfiguration, that.jdbiPluginsConfiguration) &&
                Objects.equals(sqlStatementsConfiguration, that.sqlStatementsConfiguration) &&
                Objects.equals(argumentsConfiguration, that.argumentsConfiguration) &&
                Objects.equals(handlesConfiguration, that.handlesConfiguration) &&
                Objects.equals(rowMappersConfiguration, that.rowMappersConfiguration) &&
                Objects.equals(columnMappersConfiguration, that.columnMappersConfiguration) &&
                Objects.equals(sqlArrayTypesConfiguration, that.sqlArrayTypesConfiguration) &&
                Objects.equals(codecFactoryInjectedTypeConfiguration, that.codecFactoryInjectedTypeConfiguration) &&
                Objects.equals(collectorFactoryInjectedTypeConfiguration, that.collectorFactoryInjectedTypeConfiguration) &&
                Objects.equals(extensionFactoryInjectedTypeConfiguration, that.extensionFactoryInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration,
                jdbcClientIntegrationServiceInjectedTypeConfiguration,
                jdbiPluginsConfiguration,
                sqlStatementsConfiguration,
                argumentsConfiguration,
                handlesConfiguration,
                rowMappersConfiguration,
                columnMappersConfiguration,
                sqlArrayTypesConfiguration,
                codecFactoryInjectedTypeConfiguration,
                collectorFactoryInjectedTypeConfiguration,
                extensionFactoryInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultJdbiClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", jdbcClientIntegrationServiceInjectedTypeConfiguration=" +
                jdbcClientIntegrationServiceInjectedTypeConfiguration +
                ", jdbiPluginsConfiguration=" + jdbiPluginsConfiguration +
                ", sqlStatementsConfiguration=" + sqlStatementsConfiguration +
                ", argumentsConfiguration=" + argumentsConfiguration +
                ", handlesConfiguration=" + handlesConfiguration +
                ", rowMappersConfiguration=" + rowMappersConfiguration +
                ", columnMappersConfiguration=" + columnMappersConfiguration +
                ", sqlArrayTypesConfiguration=" + sqlArrayTypesConfiguration +
                ", codecFactoryInjectedTypeConfiguration=" + codecFactoryInjectedTypeConfiguration +
                ", collectorFactoryInjectedTypeConfiguration=" + collectorFactoryInjectedTypeConfiguration +
                ", extensionFactoryInjectedTypeConfiguration=" + extensionFactoryInjectedTypeConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultJdbiClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final TextValueConfiguration DEFAULT_TEXT_VALUE_KEY_CONFIGURATION = TextValueConfiguration.PRIMARY;
        private static final JdbiPluginsConfiguration DEFAULT_JDBI_PLUGINS_CONFIGURATION =
                JdbiPluginsConfiguration.builder().build();
        private static final SqlStatementsConfiguration DEFAULT_SQL_STATEMENTS_CONFIGURATION =
                SqlStatementsConfiguration.builder().build();
        private static final ArgumentsConfiguration DEFAULT_ARGUMENTS_CONFIGURATION =
                ArgumentsConfiguration.builder().build();
        private static final HandlesConfiguration DEFAULT_HANDLES_CONFIGURATION =
                HandlesConfiguration.builder().build();
        private static final RowMappersConfiguration DEFAULT_ROW_MAPPERS_CONFIGURATION =
                RowMappersConfiguration.builder().build();
        private static final ColumnMappersConfiguration DEFAULT_COLUMN_MAPPERS_CONFIGURATION =
                ColumnMappersConfiguration.builder().build();
        private static final SqlArrayTypesConfiguration DEFAULT_SQL_ARRAY_TYPES_CONFIGURATION =
                SqlArrayTypesConfiguration.builder().build();



        private TextValueConfiguration textValueKeyConfiguration = DEFAULT_TEXT_VALUE_KEY_CONFIGURATION;

        private InjectedTypeConfiguration<JdbcClientIntegrationService>
                jdbcClientIntegrationServiceInjectedTypeConfiguration;

        private JdbiPluginsConfiguration jdbiPluginsConfiguration = DEFAULT_JDBI_PLUGINS_CONFIGURATION;

        private SqlStatementsConfiguration sqlStatementsConfiguration = DEFAULT_SQL_STATEMENTS_CONFIGURATION;

        private ArgumentsConfiguration argumentsConfiguration = DEFAULT_ARGUMENTS_CONFIGURATION;

        private HandlesConfiguration handlesConfiguration = DEFAULT_HANDLES_CONFIGURATION;

        private RowMappersConfiguration rowMappersConfiguration = DEFAULT_ROW_MAPPERS_CONFIGURATION;

        private ColumnMappersConfiguration columnMappersConfiguration = DEFAULT_COLUMN_MAPPERS_CONFIGURATION;

        private SqlArrayTypesConfiguration sqlArrayTypesConfiguration = DEFAULT_SQL_ARRAY_TYPES_CONFIGURATION;

        private InjectedTypeConfiguration<CodecFactory> codecFactoryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<CollectorFactory> collectorFactoryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<ExtensionFactory> extensionFactoryInjectedTypeConfiguration;



        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder jdbcClientIntegrationServiceInjectedTypeConfiguration(
                final InjectedTypeConfiguration<JdbcClientIntegrationService>
                        jdbcClientIntegrationServiceInjectedTypeConfiguration)
        {
            this.jdbcClientIntegrationServiceInjectedTypeConfiguration =
                    jdbcClientIntegrationServiceInjectedTypeConfiguration;
            return this;
        }

        public Builder jdbiPluginsConfiguration(final JdbiPluginsConfiguration jdbiPluginsConfiguration) {
            this.jdbiPluginsConfiguration = jdbiPluginsConfiguration;
            return this;
        }

        public Builder sqlStatementsConfiguration(final SqlStatementsConfiguration sqlStatementsConfiguration) {
            this.sqlStatementsConfiguration = sqlStatementsConfiguration;
            return this;
        }

        public Builder argumentsConfiguration(final ArgumentsConfiguration argumentsConfiguration) {
            this.argumentsConfiguration = argumentsConfiguration;
            return this;
        }

        public Builder handlesConfiguration(final HandlesConfiguration handlesConfiguration) {
            this.handlesConfiguration = handlesConfiguration;
            return this;
        }

        public Builder rowMappersConfiguration(final RowMappersConfiguration rowMappersConfiguration) {
            this.rowMappersConfiguration = rowMappersConfiguration;
            return this;
        }

        public Builder columnMappersConfiguration(final ColumnMappersConfiguration columnMappersConfiguration) {
            this.columnMappersConfiguration = columnMappersConfiguration;
            return this;
        }

        public Builder sqlArrayTypesConfiguration(final SqlArrayTypesConfiguration sqlArrayTypesConfiguration) {
            this.sqlArrayTypesConfiguration = sqlArrayTypesConfiguration;
            return this;
        }

        public Builder codecFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<CodecFactory> codecFactoryInjectedTypeConfiguration)
        {
            this.codecFactoryInjectedTypeConfiguration = codecFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder collectorFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<CollectorFactory> collectorFactoryInjectedTypeConfiguration)
        {
            this.collectorFactoryInjectedTypeConfiguration = collectorFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder extensionFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<ExtensionFactory> extensionFactoryInjectedTypeConfiguration)
        {
            this.extensionFactoryInjectedTypeConfiguration = extensionFactoryInjectedTypeConfiguration;
            return this;
        }

        public DefaultJdbiClientIntegrationServiceConfiguration build() {
            return new DefaultJdbiClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
