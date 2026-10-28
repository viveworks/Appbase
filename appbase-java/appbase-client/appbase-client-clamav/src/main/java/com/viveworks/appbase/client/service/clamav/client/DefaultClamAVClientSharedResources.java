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

package com.viveworks.appbase.client.service.clamav.client;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.clamav.client.configuration.DefaultClamAVClientSharedResourcesConfiguration;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVRequestEvent;
import com.viveworks.appbase.client.service.clamav.client.event.DefaultClamAVRequestEvent;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVPingResult;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVScanResult;
import com.viveworks.appbase.client.service.clamav.client.result.DefaultClamAVPingResult;
import com.viveworks.appbase.client.service.clamav.client.result.DefaultClamAVScanResult;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;
import com.viveworks.appbase.core.util.AppbaseSharedResources;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVClientSharedResources}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVClientSharedResources
        extends AbstractAppbaseManagedService<DefaultClamAVClientSharedResourcesConfiguration>
        implements ClamAVClientSharedResources
{
    private final AppbaseClientCore appbaseClientCore;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<ClamAVRequestEvent>> sharedClamAVRequestEventObjectPoolRef;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<ClamAVPingResult>> sharedClamAVPingResultObjectPoolRef;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<ClamAVScanResult>> sharedClamAVScanResultObjectPoolRef;


    @Inject
    public DefaultClamAVClientSharedResources(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultClamAVClientSharedResourcesConfiguration.class,
                true,
                null);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedClamAVRequestEventObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVPingResultObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVScanResultObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public DefaultClamAVClientSharedResources(
            final AppbaseClientCore appbaseClientCore,
            final DefaultClamAVClientSharedResourcesConfiguration defaultClamAVClientSharedResourcesConfiguration)
    {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultClamAVClientSharedResourcesConfiguration.class,
                defaultClamAVClientSharedResourcesConfiguration,
                true,
                null);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedClamAVRequestEventObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVPingResultObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVScanResultObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public DefaultClamAVClientSharedResources(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultClamAVClientSharedResourcesConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedClamAVRequestEventObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVPingResultObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVScanResultObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public DefaultClamAVClientSharedResources(
            final AppbaseClientCore appbaseClientCore,
            final DefaultClamAVClientSharedResourcesConfiguration defaultClamAVClientSharedResourcesConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                DefaultClamAVClientSharedResourcesConfiguration.class,
                defaultClamAVClientSharedResourcesConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.appbaseClientCore = appbaseClientCore;
        this.sharedClamAVRequestEventObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVPingResultObjectPoolRef = new AtomicReference<>();
        this.sharedClamAVScanResultObjectPoolRef = new AtomicReference<>();

        startOnCreate();
    }

    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultClamAVClientSharedResourcesConfiguration.class);
    }

    @Override
    protected void initImpl(final DefaultClamAVClientSharedResourcesConfiguration configuration) throws AppbaseException {
        final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVRequestEventObjectPoolConfiguration =
                configuration.getSharedClamAVRequestEventObjectPoolConfiguration();
        if (!sharedClamAVRequestEventObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedClamAVRequestEventObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVPingResultObjectPoolConfiguration =
                configuration.getSharedClamAVPingResultObjectPoolConfiguration();
        if (!sharedClamAVPingResultObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedClamAVPingResultObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVScanResultObjectPoolConfiguration =
                configuration.getSharedClamAVScanResultObjectPoolConfiguration();
        if (!sharedClamAVScanResultObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedClamAVScanResultObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPool<ClamAVRequestEvent> sharedClamAVRequestEventObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseClientCore.getAppbaseCore().getMetricService(),
                        sharedClamAVRequestEventObjectPoolConfiguration,
                        new DefaultClamAVRequestEvent.Factory());
        sharedClamAVRequestEventObjectPoolRef.set(sharedClamAVRequestEventObjectPool);

        final ManyToManyQueuedFixedObjectPool<ClamAVPingResult> sharedClamAVPingResultObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseClientCore.getAppbaseCore().getMetricService(),
                        sharedClamAVPingResultObjectPoolConfiguration,
                        new DefaultClamAVPingResult.Factory());
        sharedClamAVPingResultObjectPoolRef.set(sharedClamAVPingResultObjectPool);

        final ManyToManyQueuedFixedObjectPool<ClamAVScanResult> sharedClamAVScanResultObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseClientCore.getAppbaseCore().getMetricService(),
                        sharedClamAVScanResultObjectPoolConfiguration,
                        new DefaultClamAVScanResult.Factory());
        sharedClamAVScanResultObjectPoolRef.set(sharedClamAVScanResultObjectPool);

        sanityCheck();
    }

    @Override
    protected void shutdownImpl() {
        final ManyToManyQueuedFixedObjectPool<ClamAVRequestEvent> sharedClamAVRequestEventObjectPoo =
                sharedClamAVRequestEventObjectPoolRef.get();
        if (sharedClamAVRequestEventObjectPoo != null) {
            sharedClamAVRequestEventObjectPoo.shutdown();
        }

        final ManyToManyQueuedFixedObjectPool<ClamAVPingResult> sharedClamAVPingResultObjectPool =
                sharedClamAVPingResultObjectPoolRef.get();
        if (sharedClamAVPingResultObjectPool != null) {
            sharedClamAVPingResultObjectPool.shutdown();
        }

        final ManyToManyQueuedFixedObjectPool<ClamAVScanResult> sharedClamAVScanResultObjectPool =
                sharedClamAVScanResultObjectPoolRef.get();
        if (sharedClamAVScanResultObjectPool != null) {
            sharedClamAVScanResultObjectPool.shutdown();
        }
    }

    @Override
    public AppbaseClientCore getAppbaseClientCore() {
        return appbaseClientCore;
    }

    @Override
    public ManyToManyQueuedFixedObjectPool<ClamAVRequestEvent> getClamAVRequestEventObjectPool() {
        return sharedClamAVRequestEventObjectPoolRef.get();
    }

    @Override
    public ManyToManyQueuedFixedObjectPool<ClamAVPingResult> getClamAVPingResultObjectPool() {
        return sharedClamAVPingResultObjectPoolRef.get();
    }

    @Override
    public ManyToManyQueuedFixedObjectPool<ClamAVScanResult> getClamAVScanResultObjectPool() {
        return sharedClamAVScanResultObjectPoolRef.get();
    }

    @Override
    public void sanityCheck() throws AppbaseSharedResources.InvalidResourceException {
        if (appbaseCore == null) {
            throw new InvalidResourceException("appbaseClientCore must not be null");
        }

        if (appbaseClientCore == null) {
            throw new InvalidResourceException("appbaseClientCore must not be null");
        }

        if (sharedClamAVRequestEventObjectPoolRef == null) {
            throw new InvalidResourceException("sharedDruidQueryEventObjectPoolRef must not be null");
        }

        if (sharedClamAVRequestEventObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedDruidQueryEventObjectPoolRef.get() is in an invalid state");
        }

        if (sharedClamAVPingResultObjectPoolRef == null) {
            throw new InvalidResourceException("sharedClamAVPingResultObjectPoolRef must not be null");
        }

        if (sharedClamAVPingResultObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedClamAVPingResultObjectPoolRef.get() is in an invalid state");
        }

        if (sharedClamAVScanResultObjectPoolRef == null) {
            throw new InvalidResourceException("sharedClamAVScanResultObjectPoolRef must not be null");
        }

        if (sharedClamAVScanResultObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedClamAVScanResultObjectPoolRef.get() is in an invalid state");
        }
    }
}
