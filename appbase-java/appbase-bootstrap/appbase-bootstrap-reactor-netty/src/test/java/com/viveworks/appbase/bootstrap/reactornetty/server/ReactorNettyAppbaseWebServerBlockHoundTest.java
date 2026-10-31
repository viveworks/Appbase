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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.ReactiveActivityFeedEndpoint;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.store.ReactiveUserAccountStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Proves BlockHound is actually armed in this module's tests. If BlockHound silently fails to
 * install, every other "the event loop never blocks" assertion in this suite becomes vacuous -- the
 * tests would pass whether or not the code blocks. So the first test asserts that a deliberate
 * blocking call on a non-blocking scheduler IS detected; the second asserts that blocking on the
 * bounded-elastic scheduler (where the Tier-1 offload deliberately runs blocking work) is allowed.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReactorNettyAppbaseWebServerBlockHoundTest {

    @BeforeAll
    static void installBlockHound() {
        // Requires -XX:+AllowRedefinitionToAddDeleteMethods, supplied by the module's surefire
        // argLine. BlockHound 1.0.13 is the first release that instruments on JDK 25.
        BlockHound.install();
    }

    @Test
    void blockHoundIsArmedAndDetectsABlockingCallOnANonBlockingThread() {
        Assertions.assertThrows(Throwable.class, () ->
                        Mono.fromRunnable(() -> {
                            try {
                                Thread.sleep(10);
                            } catch (final InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }).subscribeOn(Schedulers.parallel()).block(Duration.ofSeconds(5)),
                "BlockHound must detect Thread.sleep on a parallel scheduler thread; if this does not "
                        + "throw, BlockHound failed to install and every non-blocking assertion is vacuous");
    }

    @Test
    void boundedElasticIsAllowedToBlock() {
        // The Tier-1 offload runs blocking work on boundedElastic ON PURPOSE. BlockHound must not
        // flag it, or the offloaded endpoints would fail their own tests.
        final String result = Mono.fromCallable(() -> {
            Thread.sleep(1);
            return "ok";
        }).subscribeOn(Schedulers.boundedElastic()).block(Duration.ofSeconds(5));

        Assertions.assertEquals("ok", result);
    }

    @Test
    void reactiveActivityFeedRequestPathIsBlockHoundClean() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore accountStore = Mockito.mock(ReactiveUserAccountStore.class);
        final AppbaseServiceEntry accountEntry = Mockito.mock(AppbaseServiceEntry.class);
        final AppbaseServiceEntry authorizationEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(accountEntry);
        Mockito.when(accountEntry.getService()).thenReturn(accountStore);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class))
                .thenReturn(authorizationEntry);
        Mockito.when(authorizationEntry.getService()).thenReturn(Mockito.mock(AuthorizationService.class));
        final UserAccount account = new UserAccount();
        account.setEmail("user@example.com");
        Mockito.when(accountStore.readByEmailReactive("user@example.com")).thenReturn(Mono.just(account));
        Mockito.when(core.getLogService().getLogAccessor().streamLogRecordsBetweenReactive(0, 2000, 2))
                .thenReturn(reactor.core.publisher.Flux.empty());
        ReactiveActivityFeedEndpoint.register(registry, core, 100);

        final ReactorNettyAppbaseWebServer server = new ReactorNettyAppbaseWebServer(
                ReactorNettyAppbaseWebServerConfiguration.builder().host("127.0.0.1").port(0).build(),
                registry,
                List.of(),
                new ObjectMapper());
        server.start();
        try {
            final String body = HttpClient.create()
                    .host("127.0.0.1")
                    .port(server.getPort())
                    .get()
                    .uri(ReactiveActivityFeedEndpoint.DEFAULT_PATH
                            + "?email=user@example.com&appbaseRequesterId=req1"
                            + "&inclusiveStartTimestamp=0&exclusiveEndTimestamp=2000&pageSize=2")
                    .responseContent().aggregate().asString().block(Duration.ofSeconds(5));
            Assertions.assertNotNull(body);
            Assertions.assertTrue(body.contains("\"type\":\"account\""));
        } finally {
            server.stop();
        }
    }
}
