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

package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.bootstrap.service.factory.DefaultAuthorizationServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.StaticClusterServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultFirebaseAuthServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultHealthCheckServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultIdServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultPasswordServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultSupportMessageServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.JdbiSupportMessageStoreFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultUserAccountServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.JdbiUserAccountStoreFactory;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.bootstrap.service.factory.AerospikeUserSessionStoreFactory;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.cluster.ClusterService;
import com.viveworks.appbase.service.cluster.StaticClusterServiceConfiguration;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.supportmessage.SupportMessageService;
import com.viveworks.appbase.service.supportmessage.store.SupportMessageStore;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.firebaseauth.FirebaseAuthService;
import com.viveworks.appbase.service.user.account.store.UserAccountStore;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for all 11 built-in {@link AppbaseServiceFactory} implementations.
 *
 * <p>Tests cover three behaviours per factory:
 * <ol>
 *   <li>Pre-built path — the same instance passed to the constructor is returned by {@code create()}.</li>
 *   <li>{@link AppbaseServiceFactory#serviceType()} — returns the expected service interface class.</li>
 *   <li>{@link AppbaseServiceFactory#initializationOrder()} — matches the corresponding
 *       {@link AppbaseServiceOrder} constant.</li>
 * </ol>
 *
 * <p>Config-path and default-path tests are integration-level concerns (they require real service
 * infrastructure). Pre-built path tests exercise the factory's most deterministic code path without
 * any external dependency.
 */
class ServiceFactoriesTest {

    private static AppbaseCoreServices mockCore() {
        // Secure-by-default injects an AuthorizationService, so build() constructs a real
        // DefaultAuthorizationService. RETURNS_MOCKS supplies its metric/mediator/registry
        // collaborators, and the ConfigurationService is backed by a map so the default-config path
        // (put-then-read-back a DefaultAuthorizationServiceConfiguration) resolves a non-null config.
        // The map starts empty on purpose — this exercises the secure-by-default default-config path,
        // not a pre-registered override.
        final Map<Class<?>, AppbaseConfiguration> typed = new HashMap<>();
        final ConfigurationService cs = mock(ConfigurationService.class);
        when(cs.getTypedConfiguration(any())).thenAnswer(inv -> typed.get(inv.getArgument(0)));
        doAnswer(inv -> {
            typed.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(cs).putTypedConfiguration(any(), any());
        AppbaseCoreServices m = mock(AppbaseCoreServices.class, RETURNS_MOCKS);
        when(m.appbaseCore()).thenReturn(mock(AppbaseCore.class));
        when(m.getConfigurationService()).thenReturn(cs);
        // getLogService() (and its LogAccessor / Logger) are left to RETURNS_MOCKS so the real
        // DefaultAuthorizationService constructed by secure-by-default build() gets a usable logger.
        return m;
    }

    /**
     * Builds an empty {@link AppbaseServices} to pass as the {@code partial} arg in pre-built
     * path tests. The pre-built factory path returns the predefined service immediately, so the
     * partial container is never dereferenced.
     */
    private static AppbaseServices emptyPartial() {
        return AppbaseServices.builder().from(mockCore()).build();
    }

    // ── DefaultAuthorizationServiceFactory ───────────────────────────────────────────

    @Test
    void authorizationFactory_predefined_returnsSameInstance() throws AppbaseException {
        AuthorizationService svc = mock(AuthorizationService.class);
        assertSame(svc,
            new DefaultAuthorizationServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void authorizationFactory_serviceType_isAuthorizationService() {
        assertEquals(AuthorizationService.class,
            new DefaultAuthorizationServiceFactory(mock(AuthorizationService.class)).serviceType());
    }

    @Test
    void authorizationFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.AUTHORIZATION.order(),
            new DefaultAuthorizationServiceFactory(mock(AuthorizationService.class)).initializationOrder());
    }

    // ── StaticClusterServiceFactory ─────────────────────────────────────────────────

    @Test
    void clusterFactory_predefined_returnsSameInstance() throws AppbaseException {
        ClusterService svc = mock(ClusterService.class);
        assertSame(svc,
            new StaticClusterServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void clusterFactory_configured_createsServiceWithHomeDataCenter() throws AppbaseException {
        final StaticClusterServiceConfiguration configuration = StaticClusterServiceConfiguration.builder()
                .homeDataCenter("stacktest")
                .build();

        final ClusterService service = new StaticClusterServiceFactory(configuration)
                .create(mockCore(), null, emptyPartial());

        assertEquals("stacktest", service.getHomeDataCenter());
    }

    @Test
    void clusterFactory_serviceType_isClusterService() {
        assertEquals(ClusterService.class,
            new StaticClusterServiceFactory(mock(ClusterService.class)).serviceType());
    }

    @Test
    void clusterFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.CLUSTER.order(),
            new StaticClusterServiceFactory(mock(ClusterService.class)).initializationOrder());
    }

    // ── DefaultHealthCheckServiceFactory ─────────────────────────────────────────────

    @Test
    void healthCheckFactory_predefined_returnsSameInstance() throws AppbaseException {
        HealthCheckService svc = mock(HealthCheckService.class);
        assertSame(svc,
            new DefaultHealthCheckServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void healthCheckFactory_serviceType_isHealthCheckService() {
        assertEquals(HealthCheckService.class,
            new DefaultHealthCheckServiceFactory(mock(HealthCheckService.class)).serviceType());
    }

    @Test
    void healthCheckFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.HEALTH_CHECK.order(),
            new DefaultHealthCheckServiceFactory(mock(HealthCheckService.class)).initializationOrder());
    }

    // ── DefaultIdServiceFactory ──────────────────────────────────────────────────────

    @Test
    void idFactory_predefined_returnsSameInstance() throws AppbaseException {
        IdService svc = mock(IdService.class);
        assertSame(svc,
            new DefaultIdServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void idFactory_serviceType_isIdService() {
        assertEquals(IdService.class,
            new DefaultIdServiceFactory(mock(IdService.class)).serviceType());
    }

    @Test
    void idFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.ID.order(),
            new DefaultIdServiceFactory(mock(IdService.class)).initializationOrder());
    }

    // ── DefaultPasswordServiceFactory ────────────────────────────────────────────────

    @Test
    void passwordFactory_predefined_returnsSameInstance() throws AppbaseException {
        PasswordService svc = mock(PasswordService.class);
        assertSame(svc,
            new DefaultPasswordServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void passwordFactory_serviceType_isPasswordService() {
        assertEquals(PasswordService.class,
            new DefaultPasswordServiceFactory(mock(PasswordService.class)).serviceType());
    }

    @Test
    void passwordFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.PASSWORD.order(),
            new DefaultPasswordServiceFactory(mock(PasswordService.class)).initializationOrder());
    }

    // ── DefaultFirebaseAuthServiceFactory ────────────────────────────────────────────

    @Test
    void firebaseAuthServiceFactory_predefined_returnsSameInstance() throws AppbaseException {
        FirebaseAuthService svc = mock(FirebaseAuthService.class);
        assertSame(svc,
            new DefaultFirebaseAuthServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void firebaseAuthServiceFactory_serviceType_isFirebaseAuthService() {
        assertEquals(FirebaseAuthService.class,
            new DefaultFirebaseAuthServiceFactory(mock(FirebaseAuthService.class)).serviceType());
    }

    @Test
    void firebaseAuthServiceFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.FIREBASE_AUTH.order(),
            new DefaultFirebaseAuthServiceFactory(mock(FirebaseAuthService.class)).initializationOrder());
    }

    // ── AerospikeUserSessionStoreFactory ───────────────────────────────────────────────

    @Test
    void userSessionStoreFactory_predefined_returnsSameInstance() throws AppbaseException {
        UserSessionStore svc = mock(UserSessionStore.class);
        assertSame(svc,
            new AerospikeUserSessionStoreFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void userSessionStoreFactory_serviceType_isUserSessionStore() {
        assertEquals(UserSessionStore.class,
            new AerospikeUserSessionStoreFactory(mock(UserSessionStore.class)).serviceType());
    }

    @Test
    void userSessionStoreFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.USER_SESSION_STORE.order(),
            new AerospikeUserSessionStoreFactory(mock(UserSessionStore.class)).initializationOrder());
    }

    // ── JdbiUserAccountStoreFactory ───────────────────────────────────────────────

    @Test
    void userAccountStoreFactory_predefined_returnsSameInstance() throws AppbaseException {
        UserAccountStore svc = mock(UserAccountStore.class);
        assertSame(svc,
            new JdbiUserAccountStoreFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void userAccountStoreFactory_serviceType_isUserAccountStore() {
        assertEquals(UserAccountStore.class,
            new JdbiUserAccountStoreFactory(mock(UserAccountStore.class)).serviceType());
    }

    @Test
    void userAccountStoreFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.USER_ACCOUNT_STORE.order(),
            new JdbiUserAccountStoreFactory(mock(UserAccountStore.class)).initializationOrder());
    }

    // ── DefaultUserAccountServiceFactory ─────────────────────────────────────────────

    @Test
    void userAccountServiceFactory_predefined_returnsSameInstance() throws AppbaseException {
        UserAccountService svc = mock(UserAccountService.class);
        assertSame(svc,
            new DefaultUserAccountServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void userAccountServiceFactory_serviceType_isUserAccountService() {
        assertEquals(UserAccountService.class,
            new DefaultUserAccountServiceFactory(mock(UserAccountService.class)).serviceType());
    }

    @Test
    void userAccountServiceFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.USER_ACCOUNT.order(),
            new DefaultUserAccountServiceFactory(mock(UserAccountService.class)).initializationOrder());
    }

    // ── JdbiSupportMessageStoreFactory ────────────────────────────────────────────

    @Test
    void supportMessageStoreFactory_predefined_returnsSameInstance() throws AppbaseException {
        SupportMessageStore svc = mock(SupportMessageStore.class);
        assertSame(svc,
            new JdbiSupportMessageStoreFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void supportMessageStoreFactory_serviceType_isSupportMessageStore() {
        assertEquals(SupportMessageStore.class,
            new JdbiSupportMessageStoreFactory(mock(SupportMessageStore.class)).serviceType());
    }

    @Test
    void supportMessageStoreFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.SUPPORT_MESSAGE_STORE.order(),
            new JdbiSupportMessageStoreFactory(mock(SupportMessageStore.class)).initializationOrder());
    }

    // ── DefaultSupportMessageServiceFactory ──────────────────────────────────────────

    @Test
    void supportMessageServiceFactory_predefined_returnsSameInstance() throws AppbaseException {
        SupportMessageService svc = mock(SupportMessageService.class);
        assertSame(svc,
            new DefaultSupportMessageServiceFactory(svc).create(mock(AppbaseCoreServices.class), null, emptyPartial()));
    }

    @Test
    void supportMessageServiceFactory_serviceType_isSupportMessageService() {
        assertEquals(SupportMessageService.class,
            new DefaultSupportMessageServiceFactory(mock(SupportMessageService.class)).serviceType());
    }

    @Test
    void supportMessageServiceFactory_order_matchesServiceOrder() {
        assertEquals(AppbaseServiceOrder.SUPPORT_MESSAGE.order(),
            new DefaultSupportMessageServiceFactory(mock(SupportMessageService.class)).initializationOrder());
    }

    // ── JdbiClientService-based factories: extra constructor coverage ─────────

    @Test
    void userAccountStoreFactory_jdbiClientServiceConstructor_serviceType() {
        assertEquals(UserAccountStore.class,
            new JdbiUserAccountStoreFactory(mock(JdbiClientService.class)).serviceType());
    }

    @Test
    void supportMessageStoreFactory_jdbiClientServiceConstructor_serviceType() {
        assertEquals(SupportMessageStore.class,
            new JdbiSupportMessageStoreFactory(mock(JdbiClientService.class)).serviceType());
    }
}
