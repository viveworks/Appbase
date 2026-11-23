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
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.store.ReactiveUserAccountStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Flux;
import reactor.core.Disposable;

/**
 * Validation and safe-error coverage for {@link ReactiveActivityFeedEndpoint}; the complementary
 * real-socket streaming contract is {@link ReactiveActivityFeedEndpointHttpTest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveActivityFeedEndpointTest {

    @Test
    void missingRequiredInputReturns400BeforeAnyServiceLookup() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        ReactiveActivityFeedEndpoint.register(registry, core, 100);

        final FakeExchange exchange = new FakeExchange("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH);
        registry.lookup("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH).handle(exchange).block();

        Assertions.assertEquals(400, exchange.status());
        Assertions.assertEquals(Map.of("status", 400, "message", "Invalid parameter: email"), exchange.body());
        Mockito.verifyNoInteractions(core);
    }

    @Test
    void missingAccountReturns404() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        registerServices(core, store);
        Mockito.when(store.readByEmailReactive("user@example.com")).thenReturn(Mono.empty());
        ReactiveActivityFeedEndpoint.register(registry, core, 100);

        final FakeExchange exchange = request();
        registry.lookup("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH).handle(exchange).block();

        Assertions.assertEquals(404, exchange.status());
        Assertions.assertEquals(Map.of("status", 404, "message", "Not Found"), exchange.body());
    }

    @Test
    void missingReactiveAccountStoreReturns503WithoutLeakingImplementationDetail() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final AuthorizationService authorizationService = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authorizationEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class))
                .thenReturn(authorizationEntry);
        Mockito.when(authorizationEntry.getService()).thenReturn(authorizationService);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class)).thenReturn(null);
        ReactiveActivityFeedEndpoint.register(registry, core, 100);

        final FakeExchange exchange = request();
        registry.lookup("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH).handle(exchange).block();

        Assertions.assertEquals(503, exchange.status());
        Assertions.assertEquals(
                Map.of("status", 503, "message", "ReactiveUserAccountStore unavailable"), exchange.body());
    }

    @Test
    void accountReadPermissionDoesNotImplicitlyGrantGlobalLogReadPermission() throws Exception {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        final AuthorizationService authorizationService = registerServices(core, store);
        Mockito.doThrow(new AuthorizationServiceException("denied"))
                .when(authorizationService)
                .checkAuthorized(
                        Mockito.eq(AppbaseServiceConstants.DEFAULT_LOG_SERVLET_PATH),
                        Mockito.eq("GET"),
                        Mockito.any());
        ReactiveActivityFeedEndpoint.register(registry, core, 100);

        final FakeExchange exchange = request();
        registry.lookup("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH).handle(exchange).block();

        Assertions.assertEquals(403, exchange.status());
        Assertions.assertEquals(Map.of("status", 403, "message", "Forbidden"), exchange.body());
        Mockito.verify(store, Mockito.never()).readByEmailReactive(Mockito.anyString());
    }

    @Test
    void accountLookupFailureReturnsSafe503WithoutInternalDetail() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        registerServices(core, store);
        Mockito.when(store.readByEmailReactive("user@example.com"))
                .thenReturn(Mono.error(new IllegalStateException("internal store detail")));
        ReactiveActivityFeedEndpoint.register(registry, core, 100);

        final FakeExchange exchange = request();
        registry.lookup("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH).handle(exchange).block();

        Assertions.assertEquals(503, exchange.status());
        Assertions.assertEquals(
                Map.of("status", 503, "message", "Activity feed unavailable"), exchange.body());
    }

    @Test
    void cancellingAfterTheFirstActivityCancelsTheLogSource() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore store = Mockito.mock(ReactiveUserAccountStore.class);
        registerServices(core, store);
        final com.viveworks.appbase.service.user.account.data.UserAccount account =
                new com.viveworks.appbase.service.user.account.data.UserAccount();
        account.setEmail("user@example.com");
        Mockito.when(store.readByEmailReactive("user@example.com")).thenReturn(Mono.just(account));
        final CountDownLatch sourceSubscribed = new CountDownLatch(1);
        final CountDownLatch sourceCancelledLatch = new CountDownLatch(1);
        Mockito.when(core.getLogService().getLogAccessor().streamLogRecordsBetweenReactive(0, 2000, 2))
                .thenReturn(Flux.create(sink -> {
                    sourceSubscribed.countDown();
                    sink.onCancel(() -> sourceCancelledLatch.countDown());
                    sink.next(Map.entry("showcase", Mockito.mock(LogRecord.class)));
                }));
        ReactiveActivityFeedEndpoint.register(registry, core, 100);

        final FakeExchange exchange = request();
        final Disposable response = registry.lookup("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH)
                .handle(exchange)
                .subscribe();
        try {
            Assertions.assertTrue(sourceSubscribed.await(5, TimeUnit.SECONDS));
        } catch (final InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interruptedException);
        }
        response.dispose();
        try {
            Assertions.assertTrue(sourceCancelledLatch.await(5, TimeUnit.SECONDS));
        } catch (final InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interruptedException);
        }
    }

    private static FakeExchange request() {
        return new FakeExchange("GET", ReactiveActivityFeedEndpoint.DEFAULT_PATH)
                .withQueryParam("email", "user@example.com")
                .withQueryParam("appbaseRequesterId", "req1")
                .withQueryParam("inclusiveStartTimestamp", "0")
                .withQueryParam("exclusiveEndTimestamp", "2000")
                .withQueryParam("pageSize", "2");
    }

    private static AuthorizationService registerServices(final AppbaseCore core, final ReactiveUserAccountStore store) {
        final AppbaseServiceEntry accountEntry = Mockito.mock(AppbaseServiceEntry.class);
        final AuthorizationService authorizationService = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authorizationEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(accountEntry);
        Mockito.when(accountEntry.getService()).thenReturn(store);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class))
                .thenReturn(authorizationEntry);
        Mockito.when(authorizationEntry.getService()).thenReturn(authorizationService);
        return authorizationService;
    }
}
