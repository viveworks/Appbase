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

package com.viveworks.appbase.client.service.clamav;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.clamav.client.ClamAVClient;
import com.viveworks.appbase.client.service.clamav.client.ClamAVClientSharedResources;
import com.viveworks.appbase.client.service.clamav.client.DefaultClamAVClient;
import com.viveworks.appbase.client.service.clamav.client.configuration.DefaultClamAVClientConfiguration;
import com.viveworks.appbase.client.service.clamav.configuration.DefaultClamAVClientIntegrationServiceConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <ClamAVClient, TextValue, DefaultClamAVClientIntegrationServiceConfiguration>
        implements ClamAVClientIntegrationService
{
    private final ClamAVClientSharedResources clamAVClientSharedResources;

    private final AtomicReference<ClamAVClient> clamAVClientRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultClamAVClientIntegrationServiceConfiguration defaultClamAVClientIntegrationServiceConfiguration;


    public DefaultClamAVClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<ClamAVClient, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final ClamAVClient clamAVClient)
    {
        super(appbaseClientCore,
                appbaseClientPool,
                appbaseClientKey,
                ClamAVClient.class);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        AppbaseUtil.notNull(clamAVClient, "clamAVClient");

        this.clamAVClientSharedResources = clamAVClientSharedResources;
        this.clamAVClientRef = new AtomicReference<>(clamAVClient);
        this.defaultClamAVClientIntegrationServiceConfiguration = null;
    }

    public DefaultClamAVClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<ClamAVClient, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final DefaultClamAVClientIntegrationServiceConfiguration defaultClamAVClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, ClamAVClient.class);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        AppbaseUtil.notNull(
                defaultClamAVClientIntegrationServiceConfiguration, "defaultClamAVClientIntegrationServiceConfiguration");

        this.clamAVClientSharedResources = clamAVClientSharedResources;
        this.defaultClamAVClientIntegrationServiceConfiguration = defaultClamAVClientIntegrationServiceConfiguration;
        this.clamAVClientRef = new AtomicReference<>();

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultClamAVClientIntegrationServiceConfiguration, "defaultClamAVClientIntegrationServiceConfiguration");

        final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration =
                defaultClamAVClientIntegrationServiceConfiguration.getDefaultClamAVClientConfiguration();
        final ClamAVClient clamAVClient =
                new DefaultClamAVClient(appbaseClientCore, clamAVClientSharedResources, defaultClamAVClientConfiguration);
        clamAVClient.reload();

        this.clamAVClientRef.set(clamAVClient);
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultClamAVClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final ClamAVClient clamAVClient = getClient();
        if (clamAVClient != null) {
            clamAVClient.shutdown();
        }

        clamAVClientRef.set(null);
    }

    @Override
    public ClamAVClient getClient() {
        return clamAVClientRef.get();
    }

    @Override
    public boolean isHealthy() {
        final ClamAVClient clamAVClient = getClient();
        return clamAVClient != null;
    }
}
