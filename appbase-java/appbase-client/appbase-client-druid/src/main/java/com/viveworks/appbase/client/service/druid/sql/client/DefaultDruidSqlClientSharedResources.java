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

package com.viveworks.appbase.client.service.druid.sql.client;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientSharedResourcesConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link DruidSqlClientSharedResources}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDruidSqlClientSharedResources
        extends AbstractAppbaseManagedService<DefaultDruidSqlClientSharedResourcesConfiguration>
        implements DruidSqlClientSharedResources
{
    private final AppbaseClientCore appbaseClientCore;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<DruidQueryEvent>> sharedDruidQueryEventObjectPoolRef;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<DruidResultSet>> sharedDruidResultSetObjectPoolRef;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<DruidSqlParameter>> sharedDruidSqlParameterObjectPoolRef;

    @Inject
    public DefaultDruidSqlClientSharedResources(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultDruidSqlClientSharedResourcesConfiguration.class,
                true,
                null);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedDruidQueryEventObjectPoolRef = new AtomicReference<>();
        this.sharedDruidResultSetObjectPoolRef = new AtomicReference<>();
        this.sharedDruidSqlParameterObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public DefaultDruidSqlClientSharedResources(
            final AppbaseClientCore appbaseClientCore,
            final DefaultDruidSqlClientSharedResourcesConfiguration defaultDruidSqlClientSharedResourcesConfiguration)
    {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultDruidSqlClientSharedResourcesConfiguration.class,
                defaultDruidSqlClientSharedResourcesConfiguration,
                true,
                null);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedDruidQueryEventObjectPoolRef = new AtomicReference<>();
        this.sharedDruidResultSetObjectPoolRef = new AtomicReference<>();
        this.sharedDruidSqlParameterObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public DefaultDruidSqlClientSharedResources(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultDruidSqlClientSharedResourcesConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedDruidQueryEventObjectPoolRef = new AtomicReference<>();
        this.sharedDruidResultSetObjectPoolRef = new AtomicReference<>();
        this.sharedDruidSqlParameterObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public DefaultDruidSqlClientSharedResources(
            final AppbaseClientCore appbaseClientCore,
            final DefaultDruidSqlClientSharedResourcesConfiguration defaultDruidSqlClientSharedResourcesConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultDruidSqlClientSharedResourcesConfiguration.class,
                defaultDruidSqlClientSharedResourcesConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedDruidQueryEventObjectPoolRef = new AtomicReference<>();
        this.sharedDruidResultSetObjectPoolRef = new AtomicReference<>();
        this.sharedDruidSqlParameterObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultDruidSqlClientSharedResourcesConfiguration.class);
    }

    @Override
    protected void initImpl(final DefaultDruidSqlClientSharedResourcesConfiguration configuration) throws AppbaseException {
        final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidQueryEventObjectPoolConfiguration =
                configuration.getSharedDruidQueryEventObjectPoolConfiguration();
        if (!sharedDruidQueryEventObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedDruidQueryEventObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidResultSetObjectPoolConfiguration =
                configuration.getSharedDruidResultSetObjectPoolConfiguration();
        if (!sharedDruidResultSetObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedDruidResultSetObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidSqlParameterObjectPoolConfiguration =
                configuration.getSharedDruidSqlParameterObjectPoolConfiguration();
        if (!sharedDruidSqlParameterObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedDruidSqlParameterObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }


        final ManyToManyQueuedFixedObjectPool<DruidQueryEvent> sharedDruidQueryEventObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseCore.getMetricService(),
                        sharedDruidQueryEventObjectPoolConfiguration,
                        new DefaultDruidQueryEvent.Factory());
        sharedDruidQueryEventObjectPoolRef.set(sharedDruidQueryEventObjectPool);

        final ManyToManyQueuedFixedObjectPool<DruidResultSet> sharedDruidResultSetObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseCore.getMetricService(),
                        sharedDruidResultSetObjectPoolConfiguration,
                        new DefaultDruidResultSet.Factory());
        sharedDruidResultSetObjectPoolRef.set(sharedDruidResultSetObjectPool);

        final ManyToManyQueuedFixedObjectPool<DruidSqlParameter> sharedDruidSqlParameterObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseCore.getMetricService(),
                        sharedDruidSqlParameterObjectPoolConfiguration,
                        new DefaultDruidSqlParameter.Factory());
        sharedDruidSqlParameterObjectPoolRef.set(sharedDruidSqlParameterObjectPool);

        sanityCheck();
    }

    @Override
    protected void shutdownImpl() {
        final ManyToManyQueuedFixedObjectPool<DruidQueryEvent> sharedDruidQueryEventObjectPool =
                sharedDruidQueryEventObjectPoolRef.get();
        if (sharedDruidQueryEventObjectPool != null) {
            sharedDruidQueryEventObjectPool.shutdown();
        }

        final ManyToManyQueuedFixedObjectPool<DruidResultSet> sharedDruidResultSetObjectPool =
                sharedDruidResultSetObjectPoolRef.get();
        if (sharedDruidResultSetObjectPool != null) {
            sharedDruidResultSetObjectPool.shutdown();
        }

        final ManyToManyQueuedFixedObjectPool<DruidSqlParameter> sharedDruidSqlParameterObjectPool =
                sharedDruidSqlParameterObjectPoolRef.get();
        if (sharedDruidSqlParameterObjectPool != null) {
            sharedDruidSqlParameterObjectPool.shutdown();
        }
    }

    @Override
    public AppbaseClientCore getAppbaseClientCore() {
        return appbaseClientCore;
    }

    @Override
    public ManyToManyQueuedFixedObjectPool<DruidQueryEvent> getSharedDruidQueryEventObjectPool() {
        return sharedDruidQueryEventObjectPoolRef.get();
    }

    @Override
    public ManyToManyQueuedFixedObjectPool<DruidResultSet> getSharedDruidResultSetObjectPool() {
        return sharedDruidResultSetObjectPoolRef.get();
    }

    @Override
    public ManyToManyQueuedFixedObjectPool<DruidSqlParameter> getSharedDruidSqlParameterObjectPool() {
        return sharedDruidSqlParameterObjectPoolRef.get();
    }

    @Override
    public void sanityCheck() throws InvalidResourceException {
        if (appbaseCore == null) {
            throw new InvalidResourceException("appbaseCore must not be null");
        }

        if (sharedDruidQueryEventObjectPoolRef == null) {
            throw new InvalidResourceException("sharedDruidQueryEventObjectPoolRef must not be null");
        }

        if (sharedDruidQueryEventObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedHostPortValueObjectPoolRef.get() is in an invalid state");
        }

        if (sharedDruidResultSetObjectPoolRef == null) {
            throw new InvalidResourceException("sharedDruidResultSetObjectPoolRef must not be null");
        }

        if (sharedDruidResultSetObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedDruidResultSetObjectPoolRef.get() is in an invalid state");
        }

        if (sharedDruidSqlParameterObjectPoolRef == null) {
            throw new InvalidResourceException("sharedDruidSqlParameterObjectPoolRef must not be null");
        }

        if (sharedDruidSqlParameterObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedDruidSqlParameterObjectPoolRef.get() is in an invalid state");
        }
    }
}
