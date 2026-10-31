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

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceTest;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.DefaultReadinessChecker;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistry;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ReadinessChecker;
import com.viveworks.appbase.core.service.registry.UnreadyAppbaseServiceEntries;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MicroprofileServiceRegistryImplTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static DefaultMediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static SchedulableConfiguration schedulableConfiguration;
    private static DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration;
    private static DefaultServiceRegistry defaultServiceRegistry;
    private static MicroprofileServiceRegistry microprofileServiceRegistry;

    @BeforeAll
    static void setUp() throws Exception {
        logService = new DefaultLogService(
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                DefaultLogServiceConfiguration.builder().build());
        metricService = new DefaultMetricService(
                logService,
                DefaultMetricServiceConfiguration.builder().build());
        validationService = new DefaultValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validationService);
        configurationService = DefaultConfigurationServiceTest.newConfigurationService(
                logService, metricService, mediatorService, validationService, false);

        schedulableConfiguration = SchedulableConfiguration
                .builder()
                .initialDelay(100L)
                .delay(10L)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        defaultServiceRegistryConfiguration =
                DefaultServiceRegistryConfiguration
                        .builder()
                        .enablePolling(true)
                        .registerAsConfigurationListener(true)
                        .pollingSchedulableConfiguration(schedulableConfiguration)
                        .build();
        configurationService.putTypedConfiguration(
                DefaultServiceRegistryConfiguration.class, defaultServiceRegistryConfiguration);

        final ReadinessChecker readinessChecker = new DefaultReadinessChecker();
        defaultServiceRegistry = new DefaultServiceRegistry(
                logService, metricService, mediatorService, validationService, configurationService, readinessChecker);
        microprofileServiceRegistry = new MicroprofileServiceRegistryImpl(defaultServiceRegistry);
        Thread.sleep(500L);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (microprofileServiceRegistry != null) {
            microprofileServiceRegistry.shutdown();
        }

        if (defaultServiceRegistry != null) {
            defaultServiceRegistry.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    @Order(1)
    void testService() throws Exception {
        final DefaultServiceRegistryConfiguration newDefaultServiceRegistryConfiguration =
                microprofileServiceRegistry.getConfiguration();
        Assertions.assertNotNull(newDefaultServiceRegistryConfiguration);

        microprofileServiceRegistry.registerAppbaseService(LogService.class, logService);
        final Collection<AppbaseServiceEntry> allAppbaseServices1 = microprofileServiceRegistry.getAppbaseServices();
        Assertions.assertNotNull(allAppbaseServices1);
        Assertions.assertEquals(1, allAppbaseServices1.size());
        final AppbaseServiceEntry appbaseServiceEntry2 =
                microprofileServiceRegistry.getAppbaseService(DefaultLogService.class);
        Assertions.assertNull(appbaseServiceEntry2);

        microprofileServiceRegistry.registerAppbaseService(logService);
        final AppbaseServiceEntry appbaseServiceEntry = microprofileServiceRegistry.getAppbaseService(LogService.class);
        Assertions.assertNotNull(appbaseServiceEntry);
        final AppbaseServiceEntry appbaseServiceEntry1 =
                microprofileServiceRegistry.getAppbaseService(MetricService.class);
        Assertions.assertNull(appbaseServiceEntry1);

        microprofileServiceRegistry.registerAppbaseService(MetricService.class, metricService);
        microprofileServiceRegistry.registerAppbaseService(MediatorService.class, mediatorService);
        microprofileServiceRegistry.registerAppbaseService(ConfigurationService.class, configurationService);
        microprofileServiceRegistry.registerAppbaseService(configurationService);
        final Collection<AppbaseServiceEntry> allAppbaseServices2 = microprofileServiceRegistry.getAppbaseServices();
        Assertions.assertEquals(6, allAppbaseServices2.size());

        final HealthCheckResponse healthCheckResponse = microprofileServiceRegistry.call();
        Assertions.assertNotNull(healthCheckResponse);

        final String healthCheckResponseName = healthCheckResponse.getName();
        Assertions.assertNotNull(healthCheckResponseName);
        Assertions.assertEquals("Appbase", healthCheckResponseName);

        final HealthCheckResponse.State healthCheckResponseState = healthCheckResponse.getState();
        Assertions.assertNotNull(healthCheckResponseState);

        final Optional<Map<String, Object>> healthCheckResponseData = healthCheckResponse.getData();
        Assertions.assertTrue(healthCheckResponseData.isPresent());

        final Map<String, Object> data = healthCheckResponseData.get();
        Assertions.assertNotNull(data);
        Assertions.assertFalse(data.isEmpty());

        final AppbaseServiceEntry appbaseServiceEntry3 =
                microprofileServiceRegistry.getAppbaseService(MetricService.class);
        Assertions.assertNotNull(appbaseServiceEntry3);

        final AppbaseServiceEntry appbaseServiceEntry4 =
                microprofileServiceRegistry.getAppbaseService(MediatorService.class);
        Assertions.assertNotNull(appbaseServiceEntry4);

        final AppbaseServiceEntry appbaseServiceEntry5 =
                microprofileServiceRegistry.getAppbaseService(ConfigurationService.class);
        Assertions.assertNotNull(appbaseServiceEntry5);
        final ServiceStatus serviceStatus = appbaseServiceEntry5.getServiceStatus();
        Assertions.assertNotEquals(serviceStatus, ServiceStatus.SHUTDOWN);

        configurationService.shutdown();
        Thread.sleep(1000);
        microprofileServiceRegistry.pollAndUpdateStatuses();
        final AppbaseServiceEntry appbaseServiceEntry6 =
                microprofileServiceRegistry.getAppbaseService(ConfigurationService.class);
        Assertions.assertNotNull(appbaseServiceEntry6);
        final ServiceStatus serviceStatus1 = appbaseServiceEntry6.getServiceStatus();
        Assertions.assertEquals(serviceStatus1, ServiceStatus.SHUTDOWN);

        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(DefaultServiceRegistryConfiguration.class);
        final ConfigurationValue<DefaultServiceRegistryConfiguration> configurationValue =
                ConfigurationValue.fromValue(MicroprofileServiceRegistryImplTest.defaultServiceRegistryConfiguration);
        final long timestamp = System.currentTimeMillis();

        final ConfigurationRemovedEvent configurationRemovedEvent1 =
                new ConfigurationRemovedEvent(configurationKey, timestamp);
        microprofileServiceRegistry.onConfigurationRemoved(configurationRemovedEvent1);
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, microprofileServiceRegistry.getStatus());

        final ConfigurationAddedEvent configurationAddedEvent =
                new ConfigurationAddedEvent(configurationKey, configurationValue, timestamp);
        microprofileServiceRegistry.onConfigurationAdded(configurationAddedEvent);
        Assertions.assertEquals(ServiceStatus.STARTED, microprofileServiceRegistry.getStatus());

        final Message message = new Message();
        message.setMessageType(MessageType.SERVICE_STATUS);
        message.getServiceStatusMessage().setServiceType(LogService.class);
        message.getServiceStatusMessage().setServiceStatus(ServiceStatus.STARTED);
        microprofileServiceRegistry.onMessage(message);

        final ConfigurationChangedEvent configurationChangedEvent =
                new ConfigurationChangedEvent(configurationKey, configurationValue, configurationValue, timestamp);
        microprofileServiceRegistry.onConfigurationChanged(configurationChangedEvent);
        Assertions.assertEquals(ServiceStatus.STARTED, microprofileServiceRegistry.getStatus());

        final Optional<UnreadyAppbaseServiceEntries> unreadyAppbaseServiceEntriesOptional =
                microprofileServiceRegistry.checkReady();
        Assertions.assertNotNull(unreadyAppbaseServiceEntriesOptional);

        microprofileServiceRegistry.shutdown();
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, microprofileServiceRegistry.getStatus());

        Assertions.assertThrows(ServiceReloadException.class,
                () -> microprofileServiceRegistry.reload(
                        MicroprofileServiceRegistryImplTest.defaultServiceRegistryConfiguration));
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, microprofileServiceRegistry.getStatus());
    }

    @Test
    @Order(1)
    void testCollections() {
        microprofileServiceRegistry.clearAppbaseServices();

        microprofileServiceRegistry.registerAppbaseService(LogService.class, logService);
        final Collection<AppbaseServiceEntry> appbaseServices = microprofileServiceRegistry.getAppbaseServices();
        Assertions.assertNotNull(appbaseServices);
        Assertions.assertEquals(1, appbaseServices.size());

        final AppbaseServiceEntry appbaseServiceEntry = microprofileServiceRegistry.getAppbaseService(LogService.class);
        Assertions.assertNotNull(appbaseServiceEntry);
        final AppbaseService service = appbaseServiceEntry.getService();
        Assertions.assertTrue(LogService.class.isAssignableFrom(service.getClass()));

        microprofileServiceRegistry.unregisterAppbaseService(LogService.class);
        final Collection<AppbaseServiceEntry> appbaseServices1 = microprofileServiceRegistry.getAppbaseServices();
        Assertions.assertNotNull(appbaseServices1);
        Assertions.assertTrue(appbaseServices1.isEmpty());

        microprofileServiceRegistry.registerAppbaseService(LogService.class, logService);
        final Collection<AppbaseServiceEntry> appbaseServices2 = microprofileServiceRegistry.getAppbaseServices();
        Assertions.assertNotNull(appbaseServices2);
        Assertions.assertEquals(1, appbaseServices2.size());

        microprofileServiceRegistry.clearAppbaseServices();
        final Collection<AppbaseServiceEntry> appbaseServices3 = microprofileServiceRegistry.getAppbaseServices();
        Assertions.assertNotNull(appbaseServices3);
        Assertions.assertTrue(appbaseServices3.isEmpty());
    }
}
