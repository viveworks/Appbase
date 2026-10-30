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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.DefaultMutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.MutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.DefaultClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.MutableClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.HostPortValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link AppbaseClientCoreSharedResources}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAppbaseClientCoreSharedResources
        extends AbstractAppbaseManagedService<DefaultAppbaseClientCoreSharedResourcesConfiguration>
        implements AppbaseClientCoreSharedResources
{
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<HostPortValue>> sharedHostPortValueObjectPoolRef;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<TextValue>> sharedTextValueObjectPoolRef;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<MutableClusterIdValue>> sharedClusterIdValueObjectPoolRef;
    private final AtomicReference<ManyToManyQueuedFixedObjectPool<MutableAppbaseClientKey<? extends ComparableValue>>>
            sharedMutableAppbaseClientKeyObjectPoolRef;


    /** Creates an unstarted shared-resource service using configuration obtained from the core runtime.
     * Call {@link #start()} before accessing any pool; construction itself initializes only empty references.
     * @param appbaseCore core runtime and configuration service
     */
    @Inject
    public DefaultAppbaseClientCoreSharedResources(final AppbaseCore appbaseCore)
    {
        super(appbaseCore, DefaultAppbaseClientCoreSharedResourcesConfiguration.class, true, null);

        this.sharedHostPortValueObjectPoolRef = new AtomicReference<>();
        this.sharedTextValueObjectPoolRef = new AtomicReference<>();
        this.sharedMutableAppbaseClientKeyObjectPoolRef = new AtomicReference<>();
        this.sharedClusterIdValueObjectPoolRef = new AtomicReference<>();
    }

    /** Creates an unstarted shared-resource service with explicit resource configuration.
     * Call {@link #start()} before accessing any pool; startup, rather than this constructor, creates the pools.
     * @param appbaseCore core runtime and lifecycle services
     * @param appbaseServiceConfiguration object-pool sizes and policies
     */
    public DefaultAppbaseClientCoreSharedResources(
            final AppbaseCore appbaseCore,
            final DefaultAppbaseClientCoreSharedResourcesConfiguration appbaseServiceConfiguration)
    {
        super(appbaseCore,
                DefaultAppbaseClientCoreSharedResourcesConfiguration.class,
                appbaseServiceConfiguration,
                true,
                null);

        this.sharedHostPortValueObjectPoolRef = new AtomicReference<>();
        this.sharedTextValueObjectPoolRef = new AtomicReference<>();
        this.sharedMutableAppbaseClientKeyObjectPoolRef = new AtomicReference<>();
        this.sharedClusterIdValueObjectPoolRef = new AtomicReference<>();
    }

    /** Creates an unstarted, configuration-backed service with selectable change-listener registration.
     * Call {@link #start()} before accessing any pool; startup can throw
     * {@link com.viveworks.appbase.core.AppbaseException}.
     * @param appbaseCore core runtime and configuration service
     * @param registerAsConfigurationListener whether changes rebuild the pools
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public DefaultAppbaseClientCoreSharedResources(
            final AppbaseCore appbaseCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseCore,
                DefaultAppbaseClientCoreSharedResourcesConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.sharedHostPortValueObjectPoolRef = new AtomicReference<>();
        this.sharedTextValueObjectPoolRef = new AtomicReference<>();
        this.sharedMutableAppbaseClientKeyObjectPoolRef = new AtomicReference<>();
        this.sharedClusterIdValueObjectPoolRef = new AtomicReference<>();
    }

    /** Creates an unstarted, explicitly configured service with selectable change-listener registration.
     * Call {@link #start()} before accessing any pool; startup can throw
     * {@link com.viveworks.appbase.core.AppbaseException}.
     * @param appbaseCore core runtime and lifecycle services
     * @param appbaseServiceConfiguration object-pool sizes and policies
     * @param registerAsConfigurationListener whether changes rebuild the pools
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public DefaultAppbaseClientCoreSharedResources(
            final AppbaseCore appbaseCore,
            final DefaultAppbaseClientCoreSharedResourcesConfiguration appbaseServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseCore,
                DefaultAppbaseClientCoreSharedResourcesConfiguration.class,
                appbaseServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.sharedHostPortValueObjectPoolRef = new AtomicReference<>();
        this.sharedTextValueObjectPoolRef = new AtomicReference<>();
        this.sharedMutableAppbaseClientKeyObjectPoolRef = new AtomicReference<>();
        this.sharedClusterIdValueObjectPoolRef = new AtomicReference<>();
    }

    /** {@inheritDoc} */
    @Override
    public ManyToManyQueuedFixedObjectPool<HostPortValue> getSharedHostPortValueObjectPool() {
        return sharedHostPortValueObjectPoolRef.get();
    }

    /** {@inheritDoc} */
    @Override
    public ManyToManyQueuedFixedObjectPool<TextValue> getSharedTextValueObjectPool() {
        return sharedTextValueObjectPoolRef.get();
    }

    /** {@inheritDoc} */
    @Override
    public ManyToManyQueuedFixedObjectPool<MutableClusterIdValue> getSharedClusterIdValueObjectPool() {
        return sharedClusterIdValueObjectPoolRef.get();
    }

    /** {@inheritDoc} */
    @Override
    public ManyToManyQueuedFixedObjectPool<MutableAppbaseClientKey<? extends ComparableValue>>
    getSharedMutableAppbaseClientKeyObjectPool()
    {
        return sharedMutableAppbaseClientKeyObjectPoolRef.get();
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientKey<HostPortValue> takeUninitializedPooledHostPortClientKey() {
        final HostPortValue hostPortValue = sharedHostPortValueObjectPoolRef.get().pollCleanObject();

        @SuppressWarnings("unchecked")
        final MutableAppbaseClientKey<HostPortValue> mutableAppbaseClientKey =
                (MutableAppbaseClientKey<HostPortValue>) sharedMutableAppbaseClientKeyObjectPoolRef.get().pollCleanObject();
        mutableAppbaseClientKey.initValue(hostPortValue);

        return mutableAppbaseClientKey;
    }

    /** {@inheritDoc} */
    @Override
    public void givePooledHostPortClientKey(final AppbaseClientKey<HostPortValue> hostPortClientKey) {
        AppbaseUtil.notNull(hostPortClientKey, "hostPortClientKey");

        final HostPortValue hostPortValue = hostPortClientKey.getValue();
        if (hostPortValue != null) {
            sharedHostPortValueObjectPoolRef
                    .get()
                    .offerCleanObject(hostPortValue);
        }

        sharedMutableAppbaseClientKeyObjectPoolRef
                .get()
                .offerCleanObject((MutableAppbaseClientKey<? extends ComparableValue>) hostPortClientKey);
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientKey<TextValue> takeUninitializedPooledTextClientKey() {
        final TextValue textValue = sharedTextValueObjectPoolRef.get().pollCleanObject();

        @SuppressWarnings("unchecked")
        final MutableAppbaseClientKey<TextValue> mutableAppbaseClientKey =
                (MutableAppbaseClientKey<TextValue>) sharedMutableAppbaseClientKeyObjectPoolRef.get().pollCleanObject();
        mutableAppbaseClientKey.initValue(textValue);

        return mutableAppbaseClientKey;
    }

    /** {@inheritDoc} */
    @Override
    public void givePooledTextClientKey(final AppbaseClientKey<TextValue> textClientKey) {
        AppbaseUtil.notNull(textClientKey, "textClientKey");

        final TextValue textValue = textClientKey.getValue();
        if (textValue != null) {
            sharedTextValueObjectPoolRef
                    .get()
                    .offerCleanObject(textValue);
        }

        sharedMutableAppbaseClientKeyObjectPoolRef
                .get()
                .offerCleanObject((MutableAppbaseClientKey<? extends ComparableValue>) textClientKey);
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientKey<MutableClusterIdValue> takeUninitializedPooledClusterIdClientKey() {
        final MutableClusterIdValue mutableClusterIdValue = sharedClusterIdValueObjectPoolRef.get().pollCleanObject();

        @SuppressWarnings("unchecked")
        final MutableAppbaseClientKey<MutableClusterIdValue> mutableAppbaseClientKey =
                (MutableAppbaseClientKey<MutableClusterIdValue>)
                        sharedMutableAppbaseClientKeyObjectPoolRef.get().pollCleanObject();
        mutableAppbaseClientKey.initValue(mutableClusterIdValue);

        return mutableAppbaseClientKey;
    }

    /** {@inheritDoc} */
    @Override
    public void givePooledClusterIdClientKey(final AppbaseClientKey<MutableClusterIdValue> mutableClusterIdClientKey) {
        AppbaseUtil.notNull(mutableClusterIdClientKey, "mutableClusterIdClientKey");

        final MutableClusterIdValue mutableClusterIdClientKeyValue = mutableClusterIdClientKey.getValue();
        if (mutableClusterIdClientKeyValue != null) {
            sharedClusterIdValueObjectPoolRef
                    .get()
                    .offerCleanObject(mutableClusterIdClientKeyValue);
        }

        sharedMutableAppbaseClientKeyObjectPoolRef
                .get()
                .offerCleanObject((MutableAppbaseClientKey<? extends ComparableValue>) mutableClusterIdClientKey);
    }

    /** {@inheritDoc} */
    @Override
    protected void initImpl(
            final DefaultAppbaseClientCoreSharedResourcesConfiguration configuration) throws InvalidResourceException
    {
        final ManyToManyQueuedFixedObjectPoolConfiguration sharedHostPortValueObjectPoolConfiguration =
                configuration.getSharedHostPortValueObjectPoolConfiguration();
        if (!sharedHostPortValueObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedHostPortValueObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPoolConfiguration sharedTextValueObjectPoolConfiguration =
                configuration.getSharedTextValueObjectPoolConfiguration();
        if (!sharedTextValueObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharsharedTextValueObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPoolConfiguration sharedMutableAppbaseClientKeyObjectPoolConfiguration =
                configuration.getSharedMutableAppbaseClientKeyObjectPoolConfiguration();
        if (!sharedMutableAppbaseClientKeyObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedMutableAppbaseClientKeyObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPoolConfiguration sharedClusterIdValueObjectPoolConfiguration =
                configuration.getSharedClusterIdValueObjectPoolConfiguration();
        if (!sharedClusterIdValueObjectPoolConfiguration.isEnabled()) {
            throw new AppbaseRuntimeException("sharedClusterIdValueObjectPoolConfiguration " +
                    "is disabled in " + configuration);
        }

        final ManyToManyQueuedFixedObjectPool<HostPortValue> sharedHostPortValueObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseCore.getMetricService(),
                        sharedHostPortValueObjectPoolConfiguration,
                        new HostPortValue.Factory());
        sharedHostPortValueObjectPoolRef.set(sharedHostPortValueObjectPool);

        final ManyToManyQueuedFixedObjectPool<TextValue> sharedTextValueObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseCore.getMetricService(),
                        sharedTextValueObjectPoolConfiguration,
                        new TextValue.Factory());
        sharedTextValueObjectPoolRef.set(sharedTextValueObjectPool);

        final ManyToManyQueuedFixedObjectPool<MutableClusterIdValue> sharedClusterIdValueObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseCore.getMetricService(),
                        sharedClusterIdValueObjectPoolConfiguration,
                        new DefaultClusterIdValue.Factory());
        sharedClusterIdValueObjectPoolRef.set(sharedClusterIdValueObjectPool);

        @SuppressWarnings({"unchecked", "rawtypes"})
        final ManyToManyQueuedFixedObjectPool<MutableAppbaseClientKey<? extends ComparableValue>>
                sharedMutableAppbaseClientKeyObjectPool =
                AppbaseUtil.manyToManyQueuedFixedObjectPool(
                        appbaseCore.getMetricService(),
                        sharedMutableAppbaseClientKeyObjectPoolConfiguration,
                        new DefaultMutableAppbaseClientKey.Factory());
        sharedMutableAppbaseClientKeyObjectPoolRef.set(sharedMutableAppbaseClientKeyObjectPool);

        sanityCheck();
    }

    /** {@inheritDoc} */
    @Override
    protected void shutdownImpl() {
        final ManyToManyQueuedFixedObjectPool<HostPortValue> sharedHostPortValueObjectPool =
                sharedHostPortValueObjectPoolRef.get();
        if (sharedHostPortValueObjectPool != null) {
            sharedHostPortValueObjectPool.shutdown();
        }

        final ManyToManyQueuedFixedObjectPool<TextValue> sharedTextValueObjectPool =
                sharedTextValueObjectPoolRef.get();
        if (sharedTextValueObjectPool != null) {
            sharedTextValueObjectPool.shutdown();
        }

        final ManyToManyQueuedFixedObjectPool<MutableAppbaseClientKey<? extends ComparableValue>>
                sharedMutableAppbaseClientKeyObjectPool = sharedMutableAppbaseClientKeyObjectPoolRef.get();
        if (sharedMutableAppbaseClientKeyObjectPool != null) {
            sharedMutableAppbaseClientKeyObjectPool.shutdown();
        }

        final ManyToManyQueuedFixedObjectPool<MutableClusterIdValue> sharedClusterIdValueObjectPool =
                sharedClusterIdValueObjectPoolRef.get();
        if (sharedClusterIdValueObjectPool != null) {
            sharedClusterIdValueObjectPool.shutdown();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sanityCheck() throws InvalidResourceException {
        if (appbaseCore == null) {
            throw new InvalidResourceException("appbaseCore must not be null");
        }

        if (sharedHostPortValueObjectPoolRef == null) {
            throw new InvalidResourceException("sharedHostPortValueObjectPoolRef must not be null");
        }

        if (sharedHostPortValueObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedHostPortValueObjectPoolRef.get() is in an invalid state");
        }

        if (sharedTextValueObjectPoolRef == null) {
            throw new InvalidResourceException("sharedTextValueObjectPoolRef must not be null");
        }

        if (sharedTextValueObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedTextValueObjectPoolRef.get() is in an invalid state");
        }

        if (sharedMutableAppbaseClientKeyObjectPoolRef == null) {
            throw new InvalidResourceException("sharedMutableAppbaseClientKeyObjectPoolRef must not be null");
        }

        if (sharedMutableAppbaseClientKeyObjectPoolRef.get() == null) {
            throw new InvalidResourceException(
                    "sharedMutableAppbaseClientKeyObjectPoolRef.get() is in an invalid state");
        }

        if (sharedClusterIdValueObjectPoolRef == null) {
            throw new InvalidResourceException("sharedClusterIdValueObjectPoolRef must not be null");
        }

        if (sharedClusterIdValueObjectPoolRef.get() == null) {
            throw new InvalidResourceException("sharedClusterIdValueObjectPoolRef.get() is in an invalid state");
        }
    }
}
