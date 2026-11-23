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

package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AppbaseClientServicesTest {

    /** Creates a mock {@link AppbaseCoreServices} with {@code appbaseCore()} and {@code getConfigurationService()} stubbed. */
    private static AppbaseCoreServices mockCore() {
        AppbaseCoreServices m = mock(AppbaseCoreServices.class);
        when(m.appbaseCore()).thenReturn(mock(AppbaseCore.class));
        when(m.getConfigurationService()).thenReturn(mock(ConfigurationService.class));
        return m;
    }

    /**
     * Returns a builder pre-wired with a mock {@link AppbaseClientCore}.
     *
     * <p>{@link com.viveworks.appbase.client.core.DefaultAppbaseClientCore} calls {@code start()}
     * in its constructor and requires real infrastructure. Tests that do not exercise
     * {@code AppbaseClientCore} creation must supply a pre-built mock via
     * {@link AppbaseClientServices.Builder#withAppbaseClientCore}.
     */
    private static AppbaseClientServices.Builder builderWith(final AppbaseCoreServices core) {
        return AppbaseClientServices.builder()
            .from(core)
            .withAppbaseClientCore(mock(AppbaseClientCore.class));
    }

    @Test
    void services_isEmpty_whenNoFactoriesRegistered() {
        assertTrue(builderWith(mockCore()).build().services().isEmpty());
    }

    @Test
    void get_unknownType_throwsAppbaseRuntimeException() {
        AppbaseClientServices clients = builderWith(mockCore()).build();
        assertThrows(AppbaseRuntimeException.class,
            () -> clients.get(AerospikeClientService.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_returnsRegisteredService() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseService mockSvc = mock(AppbaseService.class);
        AppbaseClientServiceFactory<AppbaseService> factory = mock(AppbaseClientServiceFactory.class);
        when(factory.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(factory.initializationOrder()).thenReturn(ClientServiceOrder.AEROSPIKE.order());
        when(factory.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(mockSvc);
        AppbaseClientServices clients = builderWith(mockCore).with(factory).build();
        assertSame(mockSvc, clients.get(AppbaseService.class));
    }

    @Test
    void services_returnsUnmodifiableMap() {
        assertThrows(UnsupportedOperationException.class,
            () -> builderWith(mockCore()).build().services().put(AppbaseService.class, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void with_factoryOverridesByServiceType() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseService first  = mock(AppbaseService.class);
        AppbaseService second = mock(AppbaseService.class);
        AppbaseClientServiceFactory<AppbaseService> f1 = mock(AppbaseClientServiceFactory.class);
        AppbaseClientServiceFactory<AppbaseService> f2 = mock(AppbaseClientServiceFactory.class);
        when(f1.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(f1.initializationOrder()).thenReturn(ClientServiceOrder.AEROSPIKE.order());
        when(f1.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(first);
        when(f2.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(f2.initializationOrder()).thenReturn(ClientServiceOrder.AEROSPIKE.order());
        when(f2.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(second);
        AppbaseClientServices clients = builderWith(mockCore).with(f1, f2).build();
        assertSame(second, clients.get(AppbaseService.class));
    }

    @Test
    void build_fromNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> AppbaseClientServices.builder().from(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void build_sortsByInitializationOrder() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseClientServiceFactory<AppbaseService> fHigh = mock(AppbaseClientServiceFactory.class);
        AppbaseClientServiceFactory<AerospikeClientService> fLow = mock(AppbaseClientServiceFactory.class);

        when(fHigh.serviceType()).thenAnswer(i -> AppbaseService.class);
        when(fHigh.initializationOrder()).thenReturn(20);
        when(fHigh.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(mock(AppbaseService.class));

        when(fLow.serviceType()).thenAnswer(i -> AerospikeClientService.class);
        when(fLow.initializationOrder()).thenReturn(10);
        when(fLow.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(mock(AerospikeClientService.class));

        builderWith(mockCore).with(fHigh, fLow).build();

        InOrder inOrder = inOrder(fLow, fHigh);
        inOrder.verify(fLow).create(eq(mockCore), any(AppbaseClientCore.class));
        inOrder.verify(fHigh).create(eq(mockCore), any(AppbaseClientCore.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shutdown_callsShutdownOnShutdownableServicesInReverseOrder() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseService svcA = mock(AppbaseService.class);
        AerospikeClientService svcB = mock(AerospikeClientService.class);

        AppbaseClientServiceFactory<AppbaseService> fA = mock(AppbaseClientServiceFactory.class);
        AppbaseClientServiceFactory<AerospikeClientService> fB = mock(AppbaseClientServiceFactory.class);

        when(fA.serviceType()).thenAnswer(i -> AppbaseService.class);
        when(fA.initializationOrder()).thenReturn(10);
        when(fA.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(svcA);

        when(fB.serviceType()).thenAnswer(i -> AerospikeClientService.class);
        when(fB.initializationOrder()).thenReturn(20);
        when(fB.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(svcB);

        AppbaseClientServices clients = builderWith(mockCore).with(fA, fB).build();
        clients.shutdown();

        verify(svcA).shutdown();
        verify(svcB).shutdown();
    }

    @Test
    void isShutdown_returnsTrueAfterShutdown() {
        AppbaseClientServices clients = builderWith(mockCore()).build();
        assertFalse(clients.isShutdown());
        clients.shutdown();
        assertTrue(clients.isShutdown());
    }

    @Test
    void getCoreServices_returnsProvidedCore() {
        AppbaseCoreServices mockCore = mockCore();
        assertSame(mockCore, builderWith(mockCore).build().getCoreServices());
    }

    @Test
    void getAppbaseClientCore_nonNullWhenFactorySupplied() {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseClientCore mockClientCore = mock(AppbaseClientCore.class);
        assertNotNull(AppbaseClientServices.builder()
            .from(mockCore)
            .withAppbaseClientCoreFactory(ac -> mockClientCore)
            .build()
            .getAppbaseClientCore());
    }

    @Test
    void withAppbaseClientCore_usesProvidedInstance() {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseClientCore clientCore = mock(AppbaseClientCore.class);
        assertSame(clientCore,
            AppbaseClientServices.builder().from(mockCore).withAppbaseClientCore(clientCore)
                .build().getAppbaseClientCore());
    }

    @Test
    @SuppressWarnings("unchecked")
    void build_callsRegisterConfigurationOnEachFactoryBeforeCreate() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        ConfigurationService mockConfigService = mock(ConfigurationService.class);
        when(mockCore.getConfigurationService()).thenReturn(mockConfigService);

        AppbaseClientServiceFactory<AppbaseService> factory = mock(AppbaseClientServiceFactory.class);
        when(factory.serviceType()).thenAnswer(i -> AppbaseService.class);
        when(factory.initializationOrder()).thenReturn(ClientServiceOrder.AEROSPIKE.order());
        when(factory.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class)))
            .thenReturn(mock(AppbaseService.class));

        builderWith(mockCore).with(factory).build();

        InOrder order = inOrder(factory, mockConfigService);
        order.verify(factory).registerConfiguration(mockCore);
        order.verify(mockConfigService).load();
        order.verify(factory).create(eq(mockCore), any(AppbaseClientCore.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void build_callsConfigServiceLoadAfterAllRegisterConfiguration() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        ConfigurationService mockConfigService = mock(ConfigurationService.class);
        when(mockCore.getConfigurationService()).thenReturn(mockConfigService);

        AppbaseClientServiceFactory<AppbaseService> f1 = mock(AppbaseClientServiceFactory.class);
        AppbaseClientServiceFactory<AerospikeClientService> f2 = mock(AppbaseClientServiceFactory.class);
        when(f1.serviceType()).thenAnswer(i -> AppbaseService.class);
        when(f1.initializationOrder()).thenReturn(10);
        when(f1.create(any(), any())).thenReturn(mock(AppbaseService.class));
        when(f2.serviceType()).thenAnswer(i -> AerospikeClientService.class);
        when(f2.initializationOrder()).thenReturn(20);
        when(f2.create(any(), any())).thenReturn(mock(AerospikeClientService.class));

        builderWith(mockCore).with(f1, f2).build();

        InOrder order = inOrder(f1, f2, mockConfigService);
        order.verify(f1).registerConfiguration(mockCore);
        order.verify(f2).registerConfiguration(mockCore);
        order.verify(mockConfigService).load();
    }

    @Test
    void build_callsConfigServiceLoadEvenWithNoFactories() {
        AppbaseCoreServices mockCore = mockCore();
        ConfigurationService mockConfigService = mock(ConfigurationService.class);
        when(mockCore.getConfigurationService()).thenReturn(mockConfigService);

        builderWith(mockCore).build();

        verify(mockConfigService).load();
    }

    @Test
    @SuppressWarnings("unchecked")
    void services_containsAllRegisteredFactoryResults() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseService svc = mock(AppbaseService.class);
        AppbaseClientServiceFactory<AppbaseService> factory = mock(AppbaseClientServiceFactory.class);
        when(factory.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(factory.initializationOrder()).thenReturn(ClientServiceOrder.CASSANDRA.order());
        when(factory.create(any(AppbaseCoreServices.class), any(AppbaseClientCore.class))).thenReturn(svc);
        AppbaseClientServices clients = builderWith(mockCore).with(factory).build();
        assertEquals(1, clients.services().size());
        assertTrue(clients.services().containsKey(AppbaseService.class));
    }
}
