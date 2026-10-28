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

package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.DefaultAuthorizationServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AppbaseServicesTest {

    /**
     * Creates a mock {@link AppbaseCoreServices} rich enough for {@code AppbaseServices.Builder.build()}.
     *
     * <p>Since secure-by-default now always injects an {@link AuthorizationService}, {@code build()}
     * constructs a real {@code DefaultAuthorizationService}; every unstubbed accessor therefore
     * returns a mock ({@code RETURNS_MOCKS}), and the {@link ConfigurationService} is backed by a map
     * so the authorization factory can read a (pre-registered) {@link DefaultAuthorizationServiceConfiguration}.
     */
    private static AppbaseCoreServices mockCore() {
        Map<Class<?>, AppbaseConfiguration> typed = new HashMap<>();
        typed.put(DefaultAuthorizationServiceConfiguration.class,
            mock(DefaultAuthorizationServiceConfiguration.class));
        ConfigurationService cs = mock(ConfigurationService.class);
        when(cs.getTypedConfiguration(any())).thenAnswer(inv -> typed.get(inv.getArgument(0)));
        doAnswer(inv -> {
            typed.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(cs).putTypedConfiguration(any(), any());
        AppbaseCoreServices m = mock(AppbaseCoreServices.class, RETURNS_MOCKS);
        when(m.appbaseCore()).thenReturn(mock(AppbaseCore.class));
        when(m.getConfigurationService()).thenReturn(cs);
        return m;
    }

    /**
     * Returns a builder pre-wired with a mock {@link AppbaseClientCore}.
     * Used to build a minimal {@link AppbaseClientServices} without real infrastructure.
     */
    private static AppbaseClientServices emptyClientServices(final AppbaseCoreServices core) {
        return AppbaseClientServices.builder()
            .from(core)
            .withAppbaseClientCore(mock(AppbaseClientCore.class))
            .build();
    }

    // ── Container basics ──────────────────────────────────────────────────────

    @Test
    void services_containsOnlySecureByDefaultAuthorization_whenNoFactoriesRegistered() {
        // Secure-by-default injects exactly one service — the AuthorizationService — even when the
        // operator registers no factories of their own.
        Map<Class<?>, Object> services = AppbaseServices.builder().from(mockCore()).build().services();
        assertEquals(1, services.size());
        assertTrue(services.containsKey(AuthorizationService.class));
    }

    @Test
    void get_unknownType_throwsAppbaseRuntimeException() {
        AppbaseServices svc = AppbaseServices.builder().from(mockCore()).build();
        assertThrows(AppbaseRuntimeException.class, () -> svc.get(IdService.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_returnsRegisteredService() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseService mockSvc = mock(AppbaseService.class);
        AppbaseServiceFactory<AppbaseService> factory = mock(AppbaseServiceFactory.class);
        when(factory.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(factory.initializationOrder()).thenReturn(AppbaseServiceOrder.ID.order());
        when(factory.create(any(), any(), any())).thenReturn(mockSvc);
        AppbaseServices services = AppbaseServices.builder().from(mockCore).with(factory).build();
        assertSame(mockSvc, services.get(AppbaseService.class));
    }

    @Test
    void services_returnsUnmodifiableMap() {
        assertThrows(UnsupportedOperationException.class,
            () -> AppbaseServices.builder().from(mockCore()).build().services().put(AppbaseService.class, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void with_factoryOverridesByServiceType() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseService first  = mock(AppbaseService.class);
        AppbaseService second = mock(AppbaseService.class);
        AppbaseServiceFactory<AppbaseService> f1 = mock(AppbaseServiceFactory.class);
        AppbaseServiceFactory<AppbaseService> f2 = mock(AppbaseServiceFactory.class);
        when(f1.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(f1.initializationOrder()).thenReturn(AppbaseServiceOrder.ID.order());
        when(f1.create(any(), any(), any())).thenReturn(first);
        when(f2.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(f2.initializationOrder()).thenReturn(AppbaseServiceOrder.ID.order());
        when(f2.create(any(), any(), any())).thenReturn(second);
        AppbaseServices services = AppbaseServices.builder().from(mockCore).with(f1, f2).build();
        assertSame(second, services.get(AppbaseService.class));
    }

    @Test
    void build_fromNullCore_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> AppbaseServices.builder().from(null));
    }

    @Test
    void build_withoutFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> AppbaseServices.builder().build());
    }

    @Test
    void withAllJmxOperationsEnabled_enablesReloadAndShutdownPermissions() {
        AppbaseCoreServices core = mockCore();
        ConfigurationService configurationService = core.getConfigurationService();

        AppbaseServices.builder().from(core).withAllJmxOperationsEnabled().build();

        verify(configurationService).putBoolean(
            AbstractAppbaseServiceMXBean.RELOAD_OPERATIONS_PERMISSION_PROPERTY, true);
        verify(configurationService).putBoolean(
            AbstractAppbaseServiceMXBean.SHUTDOWN_OPERATION_PERMISSION_PROPERTY, true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void build_sortsByInitializationOrder() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseServiceFactory<AppbaseService> fHigh = mock(AppbaseServiceFactory.class);
        AppbaseServiceFactory<IdService>      fLow  = mock(AppbaseServiceFactory.class);

        when(fHigh.serviceType()).thenAnswer(i -> AppbaseService.class);
        when(fHigh.initializationOrder()).thenReturn(20);
        when(fHigh.create(any(), any(), any())).thenReturn(mock(AppbaseService.class));

        when(fLow.serviceType()).thenAnswer(i -> IdService.class);
        when(fLow.initializationOrder()).thenReturn(10);
        when(fLow.create(any(), any(), any())).thenReturn(mock(IdService.class));

        AppbaseServices.builder().from(mockCore).with(fHigh, fLow).build();

        InOrder inOrder = inOrder(fLow, fHigh);
        inOrder.verify(fLow).create(eq(mockCore), any(), any());
        inOrder.verify(fHigh).create(eq(mockCore), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void build_partialContainerAvailableToLaterFactories() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        IdService idSvc = mock(IdService.class);

        AppbaseServiceFactory<IdService> fId = mock(AppbaseServiceFactory.class);
        when(fId.serviceType()).thenAnswer(i -> IdService.class);
        when(fId.initializationOrder()).thenReturn(10);
        when(fId.create(any(), any(), any())).thenReturn(idSvc);

        // Second factory captures the partial container to assert IdService is visible
        AppbaseServiceFactory<AppbaseService> fDependent = mock(AppbaseServiceFactory.class);
        when(fDependent.serviceType()).thenAnswer(i -> AppbaseService.class);
        when(fDependent.initializationOrder()).thenReturn(20);
        when(fDependent.create(any(), any(), any())).thenAnswer(inv -> {
            AppbaseServices partial = inv.getArgument(2);
            assertSame(idSvc, partial.get(IdService.class));
            return mock(AppbaseService.class);
        });

        AppbaseServices.builder().from(mockCore).with(fId, fDependent).build();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shutdown_callsShutdownOnShutdownableServicesInReverseOrder() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        Shutdownable svcA = mock(Shutdownable.class);
        Shutdownable svcB = mock(Shutdownable.class);

        // Raw types allow returning Shutdownable regardless of the factory's generic param
        AppbaseServiceFactory fA = mock(AppbaseServiceFactory.class);
        AppbaseServiceFactory fB = mock(AppbaseServiceFactory.class);

        // fA at order 10 (initialized first), fB at order 20 (initialized second)
        // shutdown must call svcB.shutdown() before svcA.shutdown()
        when(fA.serviceType()).thenAnswer(i -> Shutdownable.class);
        when(fA.initializationOrder()).thenReturn(10);
        when(fA.create(any(), any(), any())).thenReturn(svcA);

        when(fB.serviceType()).thenAnswer(i -> IdService.class);
        when(fB.initializationOrder()).thenReturn(20);
        when(fB.create(any(), any(), any())).thenReturn(svcB);

        AppbaseServices services = AppbaseServices.builder().from(mockCore).with(fA, fB).build();
        services.shutdown();

        InOrder inOrder = inOrder(svcB, svcA);
        inOrder.verify(svcB).shutdown();
        inOrder.verify(svcA).shutdown();
    }

    @Test
    void isShutdown_returnsFalseInitially() {
        assertFalse(AppbaseServices.builder().from(mockCore()).build().isShutdown());
    }

    @Test
    void isShutdown_returnsTrueAfterShutdown() {
        AppbaseServices services = AppbaseServices.builder().from(mockCore()).build();
        services.shutdown();
        assertTrue(services.isShutdown());
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @Test
    void getCoreServices_returnsProvidedCore() {
        AppbaseCoreServices core = mockCore();
        assertSame(core, AppbaseServices.builder().from(core).build().getCoreServices());
    }

    @Test
    void getClientServices_returnsProvidedClientServices() {
        AppbaseCoreServices core = mockCore();
        AppbaseClientServices clients = emptyClientServices(core);
        assertSame(clients,
            AppbaseServices.builder().from(core).withClientServices(clients).build().getClientServices());
    }

    @Test
    void getClientServices_nullWhenNotProvided() {
        assertNull(AppbaseServices.builder().from(mockCore()).build().getClientServices());
    }

    @Test
    @SuppressWarnings("unchecked")
    void services_containsAllRegisteredFactoryResults() throws Exception {
        AppbaseCoreServices mockCore = mockCore();
        AppbaseService svc = mock(AppbaseService.class);
        AppbaseServiceFactory<AppbaseService> factory = mock(AppbaseServiceFactory.class);
        when(factory.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(factory.initializationOrder()).thenReturn(AppbaseServiceOrder.CLUSTER.order());
        when(factory.create(any(), any(), any())).thenReturn(svc);
        AppbaseServices services = AppbaseServices.builder().from(mockCore).with(factory).build();
        // The registered factory result plus the secure-by-default AuthorizationService.
        assertEquals(2, services.services().size());
        assertTrue(services.services().containsKey(AppbaseService.class));
        assertTrue(services.services().containsKey(AuthorizationService.class));
    }
}
