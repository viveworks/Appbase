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

package com.viveworks.appbase.client.service.druid.sql;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClient;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClient;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientConfiguration;
import com.viveworks.appbase.client.service.druid.sql.configuration.DefaultDruidSqlClientIntegrationServiceConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link DruidSqlClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDruidSqlClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <DruidSqlClient, ClusterIdValue, DefaultDruidSqlClientIntegrationServiceConfiguration>
        implements DruidSqlClientIntegrationService
{
    private final DruidSqlClientSharedResources druidSqlClientSharedResources;

    private final AtomicReference<DruidSqlClient> druidSqlClientRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultDruidSqlClientIntegrationServiceConfiguration defaultDruidSqlClientIntegrationServiceConfiguration;



    public DefaultDruidSqlClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<DruidSqlClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlClient druidSqlClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, DruidSqlClient.class);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlClient, "druidSqlClient");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlClientRef = new AtomicReference<>(druidSqlClient);
        this.defaultDruidSqlClientIntegrationServiceConfiguration = null;
    }

    public DefaultDruidSqlClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<DruidSqlClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DefaultDruidSqlClientIntegrationServiceConfiguration defaultDruidSqlClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, DruidSqlClient.class);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(
                defaultDruidSqlClientIntegrationServiceConfiguration,
                "defaultDruidSqlClientIntegrationServiceConfiguration");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.defaultDruidSqlClientIntegrationServiceConfiguration = defaultDruidSqlClientIntegrationServiceConfiguration;
        this.druidSqlClientRef = new AtomicReference<>();

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultDruidSqlClientIntegrationServiceConfiguration,
                "defaultDruidSqlClientIntegrationServiceConfiguration");

        final DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration =
                defaultDruidSqlClientIntegrationServiceConfiguration.getDefaultDruidSqlClientConfiguration();
        final DruidSqlClient druidSqlClient = new DefaultDruidSqlClient(
                appbaseClientCore, druidSqlClientSharedResources, defaultDruidSqlClientConfiguration);
        druidSqlClient.reload();

        this.druidSqlClientRef.set(druidSqlClient);
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultDruidSqlClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final DruidSqlClient druidSqlClient = getClient();
        if (druidSqlClient != null) {
            druidSqlClient.shutdown();
        }

        druidSqlClientRef.set(null);
    }

    @Override
    public DruidSqlClient getClient() {
        return druidSqlClientRef.get();
    }

    @Override
    public boolean isHealthy() {
        final DruidSqlClient druidSqlClient = getClient();
        return druidSqlClient != null;
    }
}
