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
import com.viveworks.appbase.service.user.account.data.ReadSessionResponse;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.store.ReactiveUserSessionStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NativeUserSessionEndpointTest {

    @Test
    void sessionReadRoute_returnsSessionResponseJson() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserSessionStore store = Mockito.mock(ReactiveUserSessionStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserSessionStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);

        final UserSession userSession = new UserSession();
        userSession.setAppbaseSessionId("s1");
        Mockito.when(store.readSessionReactive("u1", "s1")).thenReturn(Mono.just(userSession));

        NativeUserSessionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .withQueryParam("appbaseUserId", "u1")
                .withQueryParam("appbaseSessionId", "s1")
                .withQueryParam("appbaseRequesterId", "req1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertTrue(exchange.body() instanceof ReadSessionResponse);
        Assertions.assertSame(userSession, ((ReadSessionResponse) exchange.body()).getUserSession());
    }

    @Test
    void sessionReadRoute_missingSession_returns404() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserSessionStore store = Mockito.mock(ReactiveUserSessionStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserSessionStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);
        Mockito.when(store.readSessionReactive("u1", "s1")).thenReturn(Mono.empty());

        NativeUserSessionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .withQueryParam("appbaseUserId", "u1")
                .withQueryParam("appbaseSessionId", "s1")
                .withQueryParam("appbaseRequesterId", "req1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(404, exchange.status());
    }

    @Test
    void sessionReadRoute_missingParam_returns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserSessionStore store = Mockito.mock(ReactiveUserSessionStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserSessionStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);

        NativeUserSessionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .withQueryParam("appbaseUserId", "u1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(400, exchange.status());
        Mockito.verify(store, Mockito.never())
                .readSessionReactive(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void sessionReadRoute_missingRequesterId_returns400() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserSessionStore store = Mockito.mock(ReactiveUserSessionStore.class);
        final AppbaseServiceEntry entry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserSessionStore.class))
                .thenReturn(entry);
        Mockito.when(entry.getService()).thenReturn(store);

        NativeUserSessionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .withQueryParam("appbaseUserId", "u1")
                .withQueryParam("appbaseSessionId", "s1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(400, exchange.status());
        Mockito.verify(store, Mockito.never())
                .readSessionReactive(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void sessionReadRoute_storeUnavailable_returns503() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserSessionStore.class))
                .thenReturn(null);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);

        NativeUserSessionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .withQueryParam("appbaseUserId", "u1")
                .withQueryParam("appbaseSessionId", "s1")
                .withQueryParam("appbaseRequesterId", "req1");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .handle(exchange)
                .block();

        Assertions.assertEquals(503, exchange.status());
    }

    @Test
    void sessionReadRoute_requesterUnauthorized_returns403_beforeStoreRead() throws AuthorizationServiceException {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserSessionStore store = Mockito.mock(ReactiveUserSessionStore.class);
        final AppbaseServiceEntry storeEntry = Mockito.mock(AppbaseServiceEntry.class);
        final AuthorizationService authz = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authzEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserSessionStore.class)).thenReturn(storeEntry);
        Mockito.when(storeEntry.getService()).thenReturn(store);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class)).thenReturn(authzEntry);
        Mockito.when(authzEntry.getService()).thenReturn(authz);
        Mockito.doThrow(new AuthorizationServiceException("not allowed"))
                .when(authz).checkAuthorized(Mockito.eq(DefaultUserAccountService.class), Mockito.eq("readSession"), Mockito.any());

        NativeUserSessionEndpoint.register(registry, core);

        final FakeExchange exchange = new FakeExchange("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH)
                .withQueryParam("appbaseUserId", "u1")
                .withQueryParam("appbaseSessionId", "s1")
                .withQueryParam("appbaseRequesterId", "intruder");
        registry.lookup("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH).handle(exchange).block();

        Assertions.assertEquals(403, exchange.status());
        Mockito.verify(store, Mockito.never()).readSessionReactive(Mockito.any(), Mockito.any());
    }
}
