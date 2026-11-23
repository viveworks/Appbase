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

package com.viveworks.appbase.bootstrap.reactornetty.endpoint;

import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.DefaultUserAccountService;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.store.ReactiveUserAccountStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NativeUserAccountEndpointTest {

    @Test
    void accountReadRoute_returnsAccountResponseJson() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);

        final UserAccount userAccount = new UserAccount();
        userAccount.setEmail("user@example.com");
        Mockito.when(store.readByEmailReactive("user@example.com")).thenReturn(Mono.just(userAccount));

        NativeUserAccountEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .withQueryParam("email", "user@example.com")
                .withQueryParam("appbaseRequesterId", "req1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertTrue(exchange.body() instanceof ReadAccountByIdResponse);
        Assertions.assertSame(userAccount, ((ReadAccountByIdResponse) exchange.body()).getUserAccount());
    }

    @Test
    void accountReadRoute_missingAccount_returns404() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);
        Mockito.when(store.readByEmailReactive("user@example.com")).thenReturn(Mono.empty());

        NativeUserAccountEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .withQueryParam("email", "user@example.com")
                .withQueryParam("appbaseRequesterId", "req1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(404, exchange.status());
    }

    @Test
    void accountReadRoute_missingParam_returns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);

        NativeUserAccountEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH);
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(400, exchange.status());
        Mockito.verify(store, Mockito.never()).readByEmailReactive(Mockito.anyString());
    }

    @Test
    void accountReadRoute_missingRequesterId_returns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);

        NativeUserAccountEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .withQueryParam("email", "user@example.com");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(400, exchange.status());
        Mockito.verify(store, Mockito.never()).readByEmailReactive(Mockito.anyString());
    }

    @Test
    void accountReadRoute_storeUnavailable_returns503() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(null);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);

        NativeUserAccountEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .withQueryParam("email", "user@example.com")
                .withQueryParam("appbaseRequesterId", "req1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(503, exchange.status());
    }

    @Test
    void accountReadRoute_requesterUnauthorized_returns403_beforeStoreRead() throws AuthorizationServiceException {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        final AppbaseServiceEntry storeEntry = Mockito.mock(AppbaseServiceEntry.class);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class)).thenReturn(storeEntry);
        Mockito.when(storeEntry.getService()).thenReturn(store);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);
        Mockito.doThrow(new AuthorizationServiceException("not allowed"))
                .when(authz).checkAuthorized(Mockito.eq(DefaultUserAccountService.class), Mockito.eq("readAccountByEmail"), Mockito.any());

        NativeUserAccountEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH)
                .withQueryParam("email", "user@example.com")
                .withQueryParam("appbaseRequesterId", "intruder");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_ACCOUNT_READ_PATH).handle(exchange).block();

        Assertions.assertEquals(403, exchange.status());
        Mockito.verify(store, Mockito.never()).readByEmailReactive(Mockito.any());
    }
}
