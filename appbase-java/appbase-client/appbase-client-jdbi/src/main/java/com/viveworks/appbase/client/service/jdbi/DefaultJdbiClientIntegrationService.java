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

package com.viveworks.appbase.client.service.jdbi;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.jdbc.JdbcClientIntegrationService;
import com.viveworks.appbase.client.service.jdbi.configuration.ArgumentsConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.ColumnMappersConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.DefaultJdbiClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.GenericTypeAndColumnMapperConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.GenericTypeAndRowMapperConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.HandlesConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.JdbiPluginsConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.JdbiSqlArrayArgumentStrategy;
import com.viveworks.appbase.client.service.jdbi.configuration.QualifiedTypeAndColumnMapperConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.RowMappersConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.SqlArrayTypesConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.SqlStatementsConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.TypeAndColumnMapperConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.TypeAndRowMapperConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.ArgumentFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.ColumnMapperFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.JdbiPluginsFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.QualifiedArgumentFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.QualifiedColumnMapperFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.RowMapperFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.RowMappersFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.SqlArrayTypeFactoriesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.SqlArrayTypesFactory;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.StatementCustomizersFactory;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Handles;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.array.SqlArrayType;
import org.jdbi.v3.core.array.SqlArrayTypeFactory;
import org.jdbi.v3.core.array.SqlArrayTypes;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.collector.CollectorFactory;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.QualifiedColumnMapperFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link JdbiClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJdbiClientIntegrationService
        extends AbstractAppbaseClientIntegrationService<Jdbi, TextValue, DefaultJdbiClientIntegrationServiceConfiguration>
        implements JdbiClientIntegrationService
{
    private final AtomicReference<Jdbi> jdbiRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final JdbcClientIntegrationService jdbcClientIntegrationService;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultJdbiClientIntegrationServiceConfiguration defaultJdbiClientIntegrationServiceConfiguration;

    public DefaultJdbiClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Jdbi, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final Jdbi jdbi)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, Jdbi.class);

        AppbaseUtil.notNull(jdbi, "jdbi");

        this.jdbiRef = new AtomicReference<>(jdbi);
        this.jdbcClientIntegrationService = null;
        this.defaultJdbiClientIntegrationServiceConfiguration = null;
    }

    public DefaultJdbiClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Jdbi, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultJdbiClientIntegrationServiceConfiguration defaultJdbiClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, Jdbi.class);

        AppbaseUtil.notNull(
                defaultJdbiClientIntegrationServiceConfiguration, "defaultJdbiClientIntegrationServiceConfiguration");

        this.jdbiRef = new AtomicReference<>();
        this.jdbcClientIntegrationService = null;
        this.defaultJdbiClientIntegrationServiceConfiguration = defaultJdbiClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    public DefaultJdbiClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Jdbi, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final JdbcClientIntegrationService jdbcClientIntegrationService,
            final DefaultJdbiClientIntegrationServiceConfiguration defaultJdbiClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, Jdbi.class);

        AppbaseUtil.notNull(jdbcClientIntegrationService, "jdbcClientIntegrationService");
        AppbaseUtil.notNull(
                defaultJdbiClientIntegrationServiceConfiguration, "defaultJdbiClientIntegrationServiceConfiguration");

        this.jdbiRef = new AtomicReference<>();
        this.jdbcClientIntegrationService = jdbcClientIntegrationService;
        this.defaultJdbiClientIntegrationServiceConfiguration = defaultJdbiClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultJdbiClientIntegrationServiceConfiguration, "defaultJdbiClientIntegrationServiceConfiguration");


        final InjectedTypeConfiguration<JdbcClientIntegrationService> jdbcClientIntegrationServiceInjectedTypeConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getJdbcClientIntegrationServiceInjectedTypeConfiguration();
        if (jdbcClientIntegrationService == null && jdbcClientIntegrationServiceInjectedTypeConfiguration == null) {
            throw new AppbaseClientException(
                    "One of jdbiClientIntegrationService or jdbcClientIntegrationServiceInjectedTypeConfiguration " +
                            "must not be null");
        }

        final JdbcClientIntegrationService localJdbcClientIntegrationService;
        if (jdbcClientIntegrationService != null) {
            localJdbcClientIntegrationService = jdbcClientIntegrationService;
        } else {
            localJdbcClientIntegrationService = AppbaseUtil.injectedInstance(
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                    jdbcClientIntegrationServiceInjectedTypeConfiguration);
        }


        AppbaseUtil.notNull(localJdbcClientIntegrationService, "localJdbcClientIntegrationService");
        final Jdbi jdbi = Jdbi.create(localJdbcClientIntegrationService.getClient());


        final JdbiPluginsConfiguration jdbiPluginsConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getJdbiPluginsConfiguration();
        initPlugins(jdbi, jdbiPluginsConfiguration);


        final RowMappersConfiguration rowMappersConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getRowMappersConfiguration();
        initRowMappers(jdbi, rowMappersConfiguration);


        final ColumnMappersConfiguration columnMappersConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getColumnMappersConfiguration();
        initColumnMappers(jdbi, columnMappersConfiguration);


        final SqlArrayTypesConfiguration sqlArrayTypesConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getSqlArrayTypesConfiguration();
        initSqlArrayTypes(jdbi, sqlArrayTypesConfiguration);


        final SqlStatementsConfiguration sqlStatementsConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getSqlStatementsConfiguration();
        initSqlStatements(jdbi, sqlStatementsConfiguration);


        final HandlesConfiguration handlesConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getHandlesConfiguration();
        initHandles(jdbi, handlesConfiguration);


        final ArgumentsConfiguration argumentsConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getArgumentsConfiguration();
        initArguments(jdbi, argumentsConfiguration);


        final InjectedTypeConfiguration<CollectorFactory> collectorFactoryInjectedTypeConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getCollectorFactoryInjectedTypeConfiguration();
        initCollectorFactory(jdbi, collectorFactoryInjectedTypeConfiguration);


        final InjectedTypeConfiguration<CodecFactory> codecFactoryInjectedTypeConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getCodecFactoryInjectedTypeConfiguration();
        initCodecFactory(jdbi, codecFactoryInjectedTypeConfiguration);


        final InjectedTypeConfiguration<ExtensionFactory> extensionFactoryInjectedTypeConfiguration =
                defaultJdbiClientIntegrationServiceConfiguration.getExtensionFactoryInjectedTypeConfiguration();
        initExtensionFactory(jdbi, extensionFactoryInjectedTypeConfiguration);

        jdbiRef.set(jdbi);
    }

    private void initPlugins(final Jdbi jdbi, final JdbiPluginsConfiguration jdbiPluginsConfiguration) {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (jdbiPluginsConfiguration.isInstallSqlObjectPlugin()) {
            jdbi.installPlugin(new SqlObjectPlugin());
        }

        final InjectedTypeConfiguration<JdbiPluginsFactory> jdbiPluginsFactoryInjectedTypeConfiguration =
                jdbiPluginsConfiguration.getJdbiPluginsFactoryInjectedTypeConfiguration();
        if (jdbiPluginsFactoryInjectedTypeConfiguration != null) {
            final JdbiPluginsFactory jdbiPluginsFactory = AppbaseUtil.injectedInstance(
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                    jdbiPluginsFactoryInjectedTypeConfiguration);
            if (jdbiPluginsFactory != null) {
                final List<JdbiPlugin> jdbiPlugins = jdbiPluginsFactory.getInstance();
                if (jdbiPlugins != null) {
                    jdbiPlugins.forEach(jdbi::installPlugin);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initRowMappers(final Jdbi jdbi, final RowMappersConfiguration rowMappersConfiguration) {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (rowMappersConfiguration != null) {
            final RowMappers rowMappers = jdbi.getConfig(RowMappers.class);

            final InjectedTypeConfiguration<RowMappersFactory> rowMappersFactoryInjectedTypeConfiguration =
                    rowMappersConfiguration.getRowMappersFactoryInjectedTypeConfiguration();
            if (rowMappersFactoryInjectedTypeConfiguration != null) {
                final RowMappersFactory rowMappersFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        rowMappersFactoryInjectedTypeConfiguration);
                if (rowMappersFactory != null) {
                    final List<RowMapper<?>> rowMapperList = rowMappersFactory.getInstance();
                    if (rowMapperList != null) {
                        rowMapperList.forEach(rowMappers::register);
                    }
                }
            }

            final InjectedTypeConfiguration<RowMapperFactoriesFactory> rowMapperFactoriesFactoryInjectedTypeConfiguration =
                    rowMappersConfiguration.getRowMapperFactoriesFactoryInjectedTypeConfiguration();
            if (rowMapperFactoriesFactoryInjectedTypeConfiguration != null) {
                final RowMapperFactoriesFactory rowMapperFactoriesFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        rowMapperFactoriesFactoryInjectedTypeConfiguration);
                if (rowMapperFactoriesFactory != null) {
                    final List<RowMapperFactory> rowMapperFactoryList = rowMapperFactoriesFactory.getInstance();
                    if (rowMapperFactoryList != null) {
                        rowMapperFactoryList.forEach(rowMappers::register);
                    }
                }
            }

            final List<TypeAndRowMapperConfiguration<?>> typeAndRowMapperConfigurations =
                    rowMappersConfiguration.getTypeAndRowMapperConfigurations();
            if (typeAndRowMapperConfigurations != null) {
                typeAndRowMapperConfigurations.forEach(typeAndRowMapperConfiguration -> {
                    if (typeAndRowMapperConfiguration != null) {
                        final Class<?> type = typeAndRowMapperConfiguration.getType();
                        final InjectedTypeConfiguration<? extends RowMapper<?>> rowMapperInjectedTypeConfiguration =
                                typeAndRowMapperConfiguration.getRowMapperInjectedTypeConfiguration();
                        final RowMapper<?> rowMapper = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                rowMapperInjectedTypeConfiguration);
                        if (rowMapper != null) {
                            rowMappers.register(type, rowMapper);
                        }
                    }
                });
            }

            final List<GenericTypeAndRowMapperConfiguration<?>> genericTypeAndRowMapperConfigurations =
                    rowMappersConfiguration.getGenericTypeAndRowMapperConfigurations();
            if (genericTypeAndRowMapperConfigurations != null) {
                genericTypeAndRowMapperConfigurations.forEach(genericTypeAndRowMapperConfiguration -> {
                    if (genericTypeAndRowMapperConfiguration != null) {
                        final InjectedTypeConfiguration<? extends GenericType<?>> genericTypeInjectedTypeConfiguration =
                                genericTypeAndRowMapperConfiguration.getGenericTypeInjectedTypeConfiguration();
                        final GenericType<?> genericType = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                genericTypeInjectedTypeConfiguration);

                        final InjectedTypeConfiguration<? extends RowMapper<?>> rowMapperInjectedTypeConfiguration =
                                genericTypeAndRowMapperConfiguration.getRowMapperInjectedTypeConfiguration();
                        final RowMapper<?> rowMapper = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                rowMapperInjectedTypeConfiguration);

                        if (genericType != null && rowMapper != null) {
                            rowMappers.register((GenericType<Object>) genericType, (RowMapper<Object>) rowMapper);
                        }
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initColumnMappers(final Jdbi jdbi, final ColumnMappersConfiguration columnMappersConfiguration) {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (columnMappersConfiguration != null) {
            final ColumnMappers columnMappers = jdbi.getConfig(ColumnMappers.class);

            final InjectedTypeConfiguration<ColumnMapperFactory> columnMapperFactoryInjectedTypeConfiguration =
                    columnMappersConfiguration.getColumnMapperFactoryInjectedTypeConfiguration();
            if (columnMapperFactoryInjectedTypeConfiguration != null) {
                final ColumnMapperFactory columnMapperFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        columnMapperFactoryInjectedTypeConfiguration);
                if (columnMapperFactory != null) {
                    columnMappers.register(columnMapperFactory);
                }
            }

            final InjectedTypeConfiguration<ColumnMapperFactoriesFactory>
                    columnMapperFactoriesFactoryInjectedTypeConfiguration =
                    columnMappersConfiguration.getColumnMapperFactoriesFactoryInjectedTypeConfiguration();
            if (columnMapperFactoriesFactoryInjectedTypeConfiguration != null) {
                final ColumnMapperFactoriesFactory columnMapperFactoriesFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        columnMapperFactoriesFactoryInjectedTypeConfiguration);
                final List<ColumnMapperFactory> columnMapperFactoryList = columnMapperFactoriesFactory.getInstance();
                if (columnMapperFactoryList != null) {
                    columnMapperFactoryList.forEach(columnMappers::register);
                }
            }

            final InjectedTypeConfiguration<QualifiedColumnMapperFactoriesFactory>
                    qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration =
                    columnMappersConfiguration.getQualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration();
            if (qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration != null) {
                final QualifiedColumnMapperFactoriesFactory qualifiedColumnMapperFactoriesFactory =
                        AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                qualifiedColumnMapperFactoriesFactoryInjectedTypeConfiguration);
                if (qualifiedColumnMapperFactoriesFactory != null) {
                    final QualifiedColumnMapperFactory qualifiedColumnMapperFactory =
                            qualifiedColumnMapperFactoriesFactory.getInstance();
                    columnMappers.register(qualifiedColumnMapperFactory);
                }
            }

            final List<QualifiedTypeAndColumnMapperConfiguration<?>> qualifiedTypeAndColumnMapperConfigurations =
                    columnMappersConfiguration.getQualifiedTypeAndColumnMapperConfigurations();
            if (qualifiedTypeAndColumnMapperConfigurations != null) {
                qualifiedTypeAndColumnMapperConfigurations.forEach(qualifiedTypeAndColumnMapperConfiguration -> {
                    if (qualifiedTypeAndColumnMapperConfiguration != null) {
                        final InjectedTypeConfiguration<? extends QualifiedType<?>> qualifiedTypeInjectedTypeConfiguration =
                                qualifiedTypeAndColumnMapperConfiguration.getQualifiedTypeInjectedTypeConfiguration();
                        final QualifiedType<?> qualifiedType = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                qualifiedTypeInjectedTypeConfiguration);

                        final InjectedTypeConfiguration<? extends ColumnMapper<?>> columnMapperInjectedTypeConfiguration =
                                qualifiedTypeAndColumnMapperConfiguration.getColumnMapperInjectedTypeConfiguration();
                        final ColumnMapper<?> columnMapper = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                columnMapperInjectedTypeConfiguration);

                        if (qualifiedType != null && columnMapper != null) {
                            columnMappers.register(
                                    (QualifiedType<Object>) qualifiedType, (ColumnMapper<Object>) columnMapper);
                        }
                    }
                });
            }
            final List<GenericTypeAndColumnMapperConfiguration<?>> genericTypeAndColumnMapperConfigurations =
                    columnMappersConfiguration.getGenericTypeAndColumnMapperConfigurations();
            if (genericTypeAndColumnMapperConfigurations != null) {
                genericTypeAndColumnMapperConfigurations.forEach(genericTypeAndColumnMapperConfiguration -> {
                    if (genericTypeAndColumnMapperConfiguration != null) {
                        final InjectedTypeConfiguration<? extends GenericType<?>> genericTypeInjectedTypeConfiguration =
                                genericTypeAndColumnMapperConfiguration.getGenericTypeInjectedTypeConfiguration();
                        final GenericType<?> genericType = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                genericTypeInjectedTypeConfiguration);

                        final InjectedTypeConfiguration<? extends ColumnMapper<?>> columnMapperInjectedTypeConfiguration =
                                genericTypeAndColumnMapperConfiguration.getColumnMapperInjectedTypeConfiguration();
                        final ColumnMapper<?> columnMapper = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                columnMapperInjectedTypeConfiguration);

                        if (genericType != null && columnMapper != null) {
                            columnMappers.register((GenericType<Object>) genericType, (ColumnMapper<Object>) columnMapper);
                        }
                    }
                });
            }

            final List<TypeAndColumnMapperConfiguration<?>> typeAndColumnMapperConfigurations =
                    columnMappersConfiguration.getTypeAndColumnMapperConfigurations();
            if (typeAndColumnMapperConfigurations != null) {
                typeAndColumnMapperConfigurations.forEach(typeAndColumnMapperConfiguration -> {
                    if (typeAndColumnMapperConfiguration != null) {
                        final Class<?> type = typeAndColumnMapperConfiguration.getType();

                        final InjectedTypeConfiguration<? extends ColumnMapper<?>> columnMapperInjectedTypeConfiguration =
                                typeAndColumnMapperConfiguration.getColumnMapperInjectedTypeConfiguration();
                        final ColumnMapper<?> columnMapper = AppbaseUtil.injectedInstance(
                                appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                                columnMapperInjectedTypeConfiguration);

                        if (type != null && columnMapper != null) {
                            columnMappers.register(type, columnMapper);
                        }
                    }
                });
            }
        }
    }

    private void initSqlArrayTypes(final Jdbi jdbi, final SqlArrayTypesConfiguration sqlArrayTypesConfiguration) {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (sqlArrayTypesConfiguration != null) {
            final SqlArrayTypes sqlArrayTypes = jdbi.getConfig(SqlArrayTypes.class);

            final InjectedTypeConfiguration<SqlArrayTypesFactory<?>> sqlArrayTypesFactoryInjectedTypeConfiguration =
                    sqlArrayTypesConfiguration.getSqlArrayTypesFactoryInjectedTypeConfiguration();
            if (sqlArrayTypesFactoryInjectedTypeConfiguration != null) {
                final SqlArrayTypesFactory<?> sqlArrayTypesFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        sqlArrayTypesFactoryInjectedTypeConfiguration);
                if (sqlArrayTypesFactory != null) {
                    final List<? extends SqlArrayType<?>> sqlArrayTypeList = sqlArrayTypesFactory.getInstance();
                    sqlArrayTypeList.forEach(sqlArrayTypes::register);
                }
            }

            final JdbiSqlArrayArgumentStrategy jdbiSqlArrayArgumentStrategy =
                    sqlArrayTypesConfiguration.getJdbiSqlArrayArgumentStrategy();
            if (jdbiSqlArrayArgumentStrategy != null) {
                sqlArrayTypes.setArgumentStrategy(jdbiSqlArrayArgumentStrategy.getSqlArrayArgumentStrategy());
            }

            final InjectedTypeConfiguration<SqlArrayTypeFactoriesFactory>
                    sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration =
                    sqlArrayTypesConfiguration.getSqlArrayTypeFactoriesFactoryInjectedTypeConfiguration();
            if (sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration != null) {
                final SqlArrayTypeFactoriesFactory sqlArrayTypeFactoriesFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        sqlArrayTypeFactoriesFactoryInjectedTypeConfiguration);
                if (sqlArrayTypeFactoriesFactory != null) {
                    final List<SqlArrayTypeFactory> sqlArrayTypeFactories = sqlArrayTypeFactoriesFactory.getInstance();
                    sqlArrayTypeFactories.forEach(sqlArrayTypes::register);
                }
            }

            final Map<Class<?>, String> sqlTypeNameByElementTypeMap =
                    sqlArrayTypesConfiguration.getSqlTypeNameByElementTypeMap();
            if (sqlTypeNameByElementTypeMap != null) {
                sqlTypeNameByElementTypeMap.forEach(sqlArrayTypes::register);
            }
        }
    }

    private void initSqlStatements(final Jdbi jdbi, final SqlStatementsConfiguration sqlStatementsConfiguration) {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (sqlStatementsConfiguration != null) {
            final SqlStatements sqlStatements = jdbi.getConfig(SqlStatements.class);

            final boolean unusedBindingAllowed = sqlStatementsConfiguration.isUnusedBindingAllowed();
            sqlStatements.setUnusedBindingAllowed(unusedBindingAllowed);

            final TimeoutConfiguration queryTimeoutConfiguration = sqlStatementsConfiguration.getQueryTimeoutConfiguration();
            if (queryTimeoutConfiguration != null) {
                sqlStatements.setQueryTimeout((int) queryTimeoutConfiguration
                        .getTimeoutTimeUnit()
                        .toSeconds(queryTimeoutConfiguration.getTimeout()));
            }

            final InjectedTypeConfiguration<SqlLogger> sqlLoggerInjectedTypeConfiguration =
                    sqlStatementsConfiguration.getSqlLoggerInjectedTypeConfiguration();
            if (sqlLoggerInjectedTypeConfiguration != null) {
                final SqlLogger sqlLogger = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        sqlLoggerInjectedTypeConfiguration);
                if (sqlLogger != null) {
                    sqlStatements.setSqlLogger(sqlLogger);
                }
            }

            final Map<String, String> statementAttributes = sqlStatementsConfiguration.getStatementAttributes();
            if (statementAttributes != null) {
                sqlStatements.defineMap(statementAttributes);
            }

            final InjectedTypeConfiguration<StatementCustomizersFactory>
                    statementCustomizersFactoryInjectedTypeConfiguration =
                    sqlStatementsConfiguration.getStatementCustomizersFactoryInjectedTypeConfiguration();
            if (statementCustomizersFactoryInjectedTypeConfiguration != null) {
                final StatementCustomizersFactory statementCustomizersFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        statementCustomizersFactoryInjectedTypeConfiguration);
                if (statementCustomizersFactory != null) {
                    final StatementCustomizer statementCustomizer = statementCustomizersFactory.getInstance();
                    sqlStatements.addCustomizer(statementCustomizer);
                }
            }

            final InjectedTypeConfiguration<Caffeine<Object, Object>> templateCacheInjectedTypeConfiguration =
                    sqlStatementsConfiguration.getTemplateCacheInjectedTypeConfiguration();
            if (templateCacheInjectedTypeConfiguration != null) {
                final Caffeine<Object, Object> templateCache = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        templateCacheInjectedTypeConfiguration);
                if (templateCache != null) {
                    sqlStatements.setTemplateCache(templateCache);
                }
            }

            final InjectedTypeConfiguration<TemplateEngine> templateEngineInjectedTypeConfiguration =
                    sqlStatementsConfiguration.getTemplateEngineInjectedTypeConfiguration();
            if (templateEngineInjectedTypeConfiguration != null) {
                final TemplateEngine templateEngine = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        templateEngineInjectedTypeConfiguration);
                if (templateEngine != null) {
                    sqlStatements.setTemplateEngine(templateEngine);
                }
            }
        }
    }

    private void initHandles(final Jdbi jdbi, final HandlesConfiguration handlesConfiguration) {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (handlesConfiguration != null) {
            final Handles handles = jdbi.getConfig(Handles.class);

            final boolean forceEndTransactions = handlesConfiguration.isForceEndTransactions();
            handles.setForceEndTransactions(forceEndTransactions);
        }
    }

    private void initArguments(final Jdbi jdbi, final ArgumentsConfiguration argumentsConfiguration) {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (argumentsConfiguration != null) {
            final Arguments arguments = jdbi.getConfig(Arguments.class);

            final boolean bindingNullToPrimitivesPermitted = argumentsConfiguration.isBindingNullToPrimitivesPermitted();
            arguments.setBindingNullToPrimitivesPermitted(bindingNullToPrimitivesPermitted);

            final InjectedTypeConfiguration<NullArgument> untypedNullArgumentInjectedTypeConfiguration =
                    argumentsConfiguration.getUntypedNullArgumentInjectedTypeConfiguration();
            if (untypedNullArgumentInjectedTypeConfiguration != null) {
                final NullArgument nullArgument = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        untypedNullArgumentInjectedTypeConfiguration);
                if (nullArgument != null) {
                    arguments.setUntypedNullArgument(nullArgument);
                }
            }

            final InjectedTypeConfiguration<ArgumentFactoriesFactory> argumentFactoriesFactoryInjectedTypeConfiguration =
                    argumentsConfiguration.getArgumentFactoriesFactoryInjectedTypeConfiguration();
            if (argumentFactoriesFactoryInjectedTypeConfiguration != null) {
                final ArgumentFactoriesFactory argumentFactoriesFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        argumentFactoriesFactoryInjectedTypeConfiguration);
                if (argumentFactoriesFactory != null) {
                    final List<ArgumentFactory> argumentFactories = argumentFactoriesFactory.getInstance();
                    if (argumentFactories != null) {
                        argumentFactories.forEach(arguments::register);
                    }
                }
            }

            final InjectedTypeConfiguration<QualifiedArgumentFactoriesFactory>
                    qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration =
                    argumentsConfiguration.getQualifiedArgumentFactoriesFactoryInjectedTypeConfiguration();
            if (qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration != null) {
                final QualifiedArgumentFactoriesFactory qualifiedArgumentFactoriesFactory = AppbaseUtil.injectedInstance(
                        appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                        qualifiedArgumentFactoriesFactoryInjectedTypeConfiguration);
                if (qualifiedArgumentFactoriesFactory != null) {
                    final List<QualifiedArgumentFactory> qualifiedArgumentFactories =
                            qualifiedArgumentFactoriesFactory.getInstance();
                    if (qualifiedArgumentFactories != null) {
                        qualifiedArgumentFactories.forEach(arguments::register);
                    }
                }
            }
        }
    }

    private void initCollectorFactory(
            final Jdbi jdbi,
            final InjectedTypeConfiguration<CollectorFactory> collectorFactoryInjectedTypeConfiguration)
    {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (collectorFactoryInjectedTypeConfiguration != null) {
            final CollectorFactory collectorFactory = AppbaseUtil.injectedInstance(
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                    collectorFactoryInjectedTypeConfiguration);
            if (collectorFactory != null) {
                jdbi.registerCollector(collectorFactory);
            }
        }
    }

    private void initCodecFactory(
            final Jdbi jdbi,
            final InjectedTypeConfiguration<CodecFactory> codecFactoryInjectedTypeConfiguration)
    {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (codecFactoryInjectedTypeConfiguration != null) {
            final CodecFactory codecFactory = AppbaseUtil.injectedInstance(
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                    codecFactoryInjectedTypeConfiguration);
            if (codecFactory != null) {
                jdbi.registerCodecFactory(codecFactory);
            }
        }
    }

    private void initExtensionFactory(
            final Jdbi jdbi,
            final InjectedTypeConfiguration<ExtensionFactory> extensionFactoryInjectedTypeConfiguration)
    {
        AppbaseUtil.notNull(jdbi, "jdbi");

        if (extensionFactoryInjectedTypeConfiguration != null) {
            final ExtensionFactory extensionFactory = AppbaseUtil.injectedInstance(
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                    extensionFactoryInjectedTypeConfiguration);
            if (extensionFactory != null) {
                jdbi.registerExtension(extensionFactory);
            }
        }
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultJdbiClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        jdbiRef.set(null);
    }

    @Override
    public Jdbi getClient() {
        return jdbiRef.get();
    }

    @VisibleForTesting
    public JdbcClientIntegrationService getJdbcClientIntegrationService() {
        return jdbcClientIntegrationService;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public boolean isHealthy() {
        final Jdbi jdbi = getClient();

        if (jdbi == null) {
            return false;
        }

        try (Handle handle = jdbi.open()) {
            return handle != null && !handle.isClosed();
        } catch (Exception ex) {
            return false;
        }
    }
}
