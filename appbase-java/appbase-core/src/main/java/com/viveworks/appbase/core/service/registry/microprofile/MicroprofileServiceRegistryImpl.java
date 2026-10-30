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

package com.viveworks.appbase.core.service.registry.microprofile;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListenerException;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.mediator.MessageHandlerException;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.UnreadyAppbaseServiceEntries;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link MicroprofileServiceRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class MicroprofileServiceRegistryImpl implements MicroprofileServiceRegistry {
    private final ServiceRegistry serviceRegistry;

    @Inject
    public MicroprofileServiceRegistryImpl(final ServiceRegistry serviceRegistry) {
        AppbaseUtil.notNull(serviceRegistry, "serviceRegistry");

        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public HealthCheckResponse call() {
        final DefaultServiceRegistryConfiguration configuration = getConfiguration();
        final Map<String, Object> serviceStatusMap = new HashMap<>();
        final Collection<AppbaseServiceEntry> appbaseServices = getAppbaseServices();
        HealthCheckResponse.State state = HealthCheckResponse.State.UP;

        for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServices) {
            if (state.equals(HealthCheckResponse.State.DOWN)) {
                break;
            }

            final ServiceStatus serviceStatus = appbaseServiceEntry.getServiceStatus();
            if (!serviceStatus.equals(ServiceStatus.STARTED)) {
                state = HealthCheckResponse.State.DOWN;
            }

            final AppbaseService service = appbaseServiceEntry.getService();
            serviceStatusMap.put(service.getClass().getName(), serviceStatus);
        }

        return new DefaultHealthCheckResponse(configuration.getAppName(), state, serviceStatusMap);
    }

    @Override
    public void pollAndUpdateStatuses() {
        serviceRegistry.pollAndUpdateStatuses();
    }

    @Override
    public AppbaseServiceEntry getAppbaseService(final Class<? extends AppbaseService> appbaseServiceType) {
        return serviceRegistry.getAppbaseService(appbaseServiceType);
    }

    @Override
    public Collection<AppbaseServiceEntry> getAppbaseServices() {
        return serviceRegistry.getAppbaseServices();
    }

    @Override
    public Optional<UnreadyAppbaseServiceEntries> checkReady() {
        return serviceRegistry.checkReady();
    }

    @Override
    public OverallStatus overallStatus() {
        return serviceRegistry.overallStatus();
    }

    @Override
    public OverallStatus pollOverallStatus() {
        return serviceRegistry.pollOverallStatus();
    }

    @Override
    public void registerAppbaseService(final AppbaseService service) {
        serviceRegistry.registerAppbaseService(service);
    }

    @Override
    public <T extends AppbaseService> void registerAppbaseService(
            final Class<T> appbaseServiceType,
            final T appbaseService)
    {
        serviceRegistry.registerAppbaseService(appbaseServiceType, appbaseService);
    }

    @Override
    public void unregisterAppbaseService(final Class<? extends AppbaseService> appbaseServiceType) {
        serviceRegistry.unregisterAppbaseService(appbaseServiceType);
    }

    @Override
    public void clearAppbaseServices() {
        serviceRegistry.clearAppbaseServices();
    }

    @Override
    public <C extends AppbaseConfiguration> void reload(final C configuration) throws ServiceReloadException {
        serviceRegistry.reload(configuration);
    }

    @Override
    public ServiceStatus getStatus() {
        return serviceRegistry.getStatus();
    }

    @Override
    public <C extends AppbaseConfiguration> C getConfiguration() {
        return serviceRegistry.getConfiguration();
    }

    @Override
    public void onConfigurationAdded(
            final ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationListenerException
    {
        serviceRegistry.onConfigurationAdded(configurationAddedEvent);
    }

    @Override
    public void onConfigurationChanged(
            final ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationListenerException
    {
        serviceRegistry.onConfigurationChanged(configurationChangedEvent);
    }

    @Override
    public void onConfigurationRemoved(
            final ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationListenerException
    {
        serviceRegistry.onConfigurationRemoved(configurationRemovedEvent);
    }

    @Override
    public void onMessage(final Message message) throws MessageHandlerException {
        serviceRegistry.onMessage(message);
    }

    @Override
    public boolean isShutdown() {
        return serviceRegistry.isShutdown();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        serviceRegistry.shutdown();
    }
}
