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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link AppbaseClientCore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAppbaseClientCore
        extends AbstractAppbaseManagedService<DefaultAppbaseClientCoreConfiguration> implements AppbaseClientCore
{
    private final AtomicReference<AppbaseClientCoreSharedResources> appbaseClientCoreSharedResourcesRef;

    /** Starts a configuration-backed client core and registers it for configuration updates.
     * @param appbaseCore core runtime supplying configuration and lifecycle services
     * @throws AppbaseRuntimeException when startup fails
     */
    @Inject
    public DefaultAppbaseClientCore(final AppbaseCore appbaseCore)
    {
        super(appbaseCore,
                DefaultAppbaseClientCoreConfiguration.class,
                true,
                null);

        this.appbaseClientCoreSharedResourcesRef = new AtomicReference<>();

        try {
            start();
        } catch (AppbaseException ae) {
            throw new AppbaseRuntimeException("Failed to start " + getClass().getName(), ae);
        }
    }

    /** Starts the client core from an explicit configuration with listener registration enabled.
     * @param appbaseCore core runtime supplying lifecycle services
     * @param defaultAppbaseClientCoreConfiguration shared-resource configuration
     */
    public DefaultAppbaseClientCore(
            final AppbaseCore appbaseCore,
            final DefaultAppbaseClientCoreConfiguration defaultAppbaseClientCoreConfiguration)
    {
        this(appbaseCore, defaultAppbaseClientCoreConfiguration, true, null);
    }

    /** Starts an explicitly configured client core with selectable configuration listening.
     * @param appbaseCore core runtime supplying lifecycle services
     * @param defaultAppbaseClientCoreConfiguration shared-resource configuration
     * @param registerAsConfigurationListener whether configuration changes restart resources
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     * @throws AppbaseRuntimeException when startup fails
     */
    public DefaultAppbaseClientCore(
            final AppbaseCore appbaseCore,
            final DefaultAppbaseClientCoreConfiguration defaultAppbaseClientCoreConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseCore,
                DefaultAppbaseClientCoreConfiguration.class,
                defaultAppbaseClientCoreConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.appbaseClientCoreSharedResourcesRef = new AtomicReference<>();

        try {
            start();
        } catch (AppbaseException ae) {
            throw new AppbaseRuntimeException("Failed to start " + getClass().getName(), ae);
        }
    }

    /** Starts a core after validating a caller-supplied shared-resource reference.
     * Startup currently replaces that reference with newly constructed, configuration-backed resources;
     * callers must not rely on the supplied instance being retained or exposed.
     * @param appbaseCore core runtime supplying lifecycle services
     * @param appbaseClientCoreSharedResources non-null resource reference validated before startup
     * @throws AppbaseRuntimeException when startup fails
     */
    public DefaultAppbaseClientCore(
            final AppbaseCore appbaseCore,
            final AppbaseClientCoreSharedResources appbaseClientCoreSharedResources)
    {
        super(appbaseCore,
                DefaultAppbaseClientCoreConfiguration.class,
                true,
                null);

        AppbaseUtil.notNull(appbaseClientCoreSharedResources, "appbaseClientCoreSharedResources");

        this.appbaseClientCoreSharedResourcesRef = new AtomicReference<>(appbaseClientCoreSharedResources);

        try {
            start();
        } catch (AppbaseException ae) {
            throw new AppbaseRuntimeException("Failed to start " + getClass().getName(), ae);
        }
    }


    /** Starts a core after validating a supplied resource reference, with selectable configuration listening.
     * Startup currently replaces that reference with newly constructed, configuration-backed resources;
     * callers must not rely on the supplied instance being retained or exposed.
     * @param appbaseCore core runtime supplying lifecycle services
     * @param appbaseClientCoreSharedResources non-null resource reference validated before startup
     * @param registerAsConfigurationListener whether configuration changes restart resources
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     * @throws AppbaseRuntimeException when startup fails
     */
    public DefaultAppbaseClientCore(
            final AppbaseCore appbaseCore,
            final AppbaseClientCoreSharedResources appbaseClientCoreSharedResources,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseCore,
                DefaultAppbaseClientCoreConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(appbaseClientCoreSharedResources, "appbaseClientCoreSharedResources");

        this.appbaseClientCoreSharedResourcesRef = new AtomicReference<>(appbaseClientCoreSharedResources);

        try {
            start();
        } catch (AppbaseException ae) {
            throw new AppbaseRuntimeException("Failed to start " + getClass().getName(), ae);
        }
    }

    static {
        AppbaseClientCore.registerObjectMapperSubTypes();
    }

    /** {@inheritDoc} */
    @Override
    protected void initImpl(final DefaultAppbaseClientCoreConfiguration configuration) throws AppbaseException {
        final DefaultAppbaseClientCoreSharedResourcesConfiguration defaultAppbaseClientSharedResourcesConfiguration =
                configuration.getDefaultAppbaseClientSharedResourcesConfiguration();
        final DefaultAppbaseClientCoreSharedResources defaultAppbaseClientCoreSharedResources =
                new DefaultAppbaseClientCoreSharedResources(
                        appbaseCore, defaultAppbaseClientSharedResourcesConfiguration, false, null);
        defaultAppbaseClientCoreSharedResources.start();
        appbaseClientCoreSharedResourcesRef.set(defaultAppbaseClientCoreSharedResources);
    }

    /** {@inheritDoc} */
    @Override
    protected void shutdownImpl() {
        final AppbaseClientCoreSharedResources appbaseClientCoreSharedResources = appbaseClientCoreSharedResourcesRef.get();
        if (appbaseClientCoreSharedResources != null) {
            appbaseClientCoreSharedResources.shutdown();
        }
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientCoreSharedResources getAppbaseClientCoreSharedResources() {
        return appbaseClientCoreSharedResourcesRef.get();
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseCore getAppbaseCore() {
        return appbaseCore;
    }
}
