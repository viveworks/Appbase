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

package com.viveworks.appbase.client.service.jdbi;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.jdbc.JdbcClientIntegrationService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientService;
import com.viveworks.appbase.client.service.jdbi.configuration.DefaultJdbiClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.DefaultJdbiClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link JdbiClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJdbiClientService
        extends AbstractAppbaseClientService<Jdbi, TextValue, DefaultJdbiClientServiceConfiguration>
        implements JdbiClientService
{
    private final JdbcClientService jdbcClientService;

    @Inject
    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final JdbcClientService jdbcClientService)
    {
        super(appbaseClientCore,
                DefaultJdbiClientServiceConfiguration.class,
                false,
                null);

        AppbaseUtil.notNull(jdbcClientService, "jdbcClientService");
        this.jdbcClientService = jdbcClientService;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final JdbcClientService jdbcClientService,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJdbiClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(jdbcClientService, "jdbcClientService");
        this.jdbcClientService = jdbcClientService;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final JdbcClientService jdbcClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Jdbi, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJdbiClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(jdbcClientService, "jdbcClientService");
        this.jdbcClientService = jdbcClientService;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final JdbcClientService jdbcClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJdbiClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(jdbcClientService, "jdbcClientService");
        this.jdbcClientService = jdbcClientService;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultJdbiClientServiceConfiguration defaultJdbiClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJdbiClientServiceConfiguration.class,
                defaultJdbiClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.jdbcClientService = null;
        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final JdbcClientService jdbcClientService,
            final DefaultJdbiClientServiceConfiguration defaultJdbiClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJdbiClientServiceConfiguration.class,
                defaultJdbiClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(jdbcClientService, "jdbcClientService");
        this.jdbcClientService = jdbcClientService;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Jdbi, TextValue> appbaseClientPool,
            final DefaultJdbiClientServiceConfiguration defaultJdbiClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJdbiClientServiceConfiguration.class,
                defaultJdbiClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.jdbcClientService = null;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final JdbcClientService jdbcClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Jdbi, TextValue> appbaseClientPool,
            final DefaultJdbiClientServiceConfiguration defaultJdbiClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJdbiClientServiceConfiguration.class,
                defaultJdbiClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(jdbcClientService, "jdbcClientService");
        this.jdbcClientService = jdbcClientService;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultJdbiClientServiceConfiguration defaultJdbiClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJdbiClientServiceConfiguration.class,
                defaultJdbiClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.jdbcClientService = null;

        startOnCreate();
    }

    public DefaultJdbiClientService(
            final AppbaseClientCore appbaseClientCore,
            final JdbcClientService jdbcClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultJdbiClientServiceConfiguration defaultJdbiClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJdbiClientServiceConfiguration.class,
                defaultJdbiClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(jdbcClientService, "jdbcClientService");
        this.jdbcClientService = jdbcClientService;

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultJdbiClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultJdbiClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultJdbiClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultJdbiClientIntegrationServiceConfiguration defaultDefaultJdbiClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJdbiClientIntegrationServiceConfiguration();
        return JdbiClientUtil.appbaseClientKey(defaultDefaultJdbiClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<Jdbi, TextValue>>
    appbaseClientIntegrationServices(final DefaultJdbiClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<Jdbi, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultJdbiClientIntegrationServiceConfiguration defaultDefaultJdbiClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJdbiClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultJdbiClientIntegrationServiceConfiguration,
                "defaultDefaultJdbiClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultJdbiClientIntegrationService defaultDefaultJdbiClientIntegrationService;
        if (jdbcClientService == null) {
            defaultDefaultJdbiClientIntegrationService = new DefaultJdbiClientIntegrationService(
                    appbaseClientCore,
                    getAppbaseClientPool(),
                    defaultAppbaseClientKey,
                    defaultDefaultJdbiClientIntegrationServiceConfiguration);
        } else {
            defaultDefaultJdbiClientIntegrationService = new DefaultJdbiClientIntegrationService(
                    appbaseClientCore,
                    getAppbaseClientPool(),
                    defaultAppbaseClientKey,
                    jdbcClientService.getClientIntegrationService(),
                    defaultDefaultJdbiClientIntegrationServiceConfiguration);
        }
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultJdbiClientIntegrationService);


        final Set<DefaultJdbiClientIntegrationServiceConfiguration> keyedDefaultJdbiClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultJdbiClientIntegrationServiceConfigurations();
        if (keyedDefaultJdbiClientIntegrationServiceConfigurations != null) {
            keyedDefaultJdbiClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultJdbiClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                JdbiClientUtil.appbaseClientKey(keyedDefaultJdbiClientIntegrationServiceConfiguration);

                        final DefaultJdbiClientIntegrationService defaultJdbiClientIntegrationService;
                        if (jdbcClientService != null) {
                            final JdbcClientIntegrationService jdbcClientIntegrationService =
                                    jdbcClientService.getClientIntegrationService(appbaseClientKey);
                            AppbaseUtil.notNull(jdbcClientIntegrationService, "jdbcClientIntegrationService");

                            defaultJdbiClientIntegrationService = new DefaultJdbiClientIntegrationService(
                                    appbaseClientCore,
                                    getAppbaseClientPool(),
                                    appbaseClientKey,
                                    jdbcClientIntegrationService,
                                    keyedDefaultJdbiClientIntegrationServiceConfiguration);
                        } else {
                            defaultJdbiClientIntegrationService = new DefaultJdbiClientIntegrationService(
                                    appbaseClientCore,
                                    getAppbaseClientPool(),
                                    appbaseClientKey,
                                    keyedDefaultJdbiClientIntegrationServiceConfiguration);
                        }
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultJdbiClientIntegrationService);
                    });
        }

        return appbaseClientIntegrationServices;
    }

    @Override
    protected void registerService() {
        super.registerService();

        appbaseClientCore
                .getAppbaseCore()
                .getServiceRegistry()
                .registerAppbaseService(JdbiClientService.class, this);
    }

    @Override
    public JdbiClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JdbiClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public JdbiClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JdbiClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
