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

package com.viveworks.appbase.stacktest.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.ReactiveActivityFeedEndpoint;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.bootstrap.reactornetty.server.ReactorNettyAppbaseWebServer;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.store.ReactiveUserAccountStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Standard P-008 stacktest arm for the native reactor-netty reactive showcase.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveShowcaseStackTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private ReactorNettyAppbaseWebServer server;

    @AfterEach
    void stopServer() {
        if (server != null && server.isStarted()) {
            server.stop();
        }
    }

    @Test
    void nativeActivityFeedStreamsTheDocumentedAccountAndActivityEvents() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = configuredCore();
        ReactiveActivityFeedEndpoint.register(registry, core, 100);
        server = new ReactorNettyAppbaseWebServer(
                ReactorNettyAppbaseWebServerConfiguration.builder().host("127.0.0.1").port(0).build(),
                registry,
                List.of(),
                new ObjectMapper());
        server.start();

        final Tuple3<Integer, String, String> response = HttpClient.create()
                .host("127.0.0.1")
                .port(server.getPort())
                .get()
                .uri(ReactiveActivityFeedEndpoint.DEFAULT_PATH
                        + "?email=showcase@example.com&appbaseRequesterId=stacktest"
                        + "&inclusiveStartTimestamp=0&exclusiveEndTimestamp=2000&pageSize=2")
                .responseSingle((httpResponse, bytes) -> bytes.asString().map(body -> Tuples.of(
                        httpResponse.status().code(),
                        httpResponse.responseHeaders().get("Content-Type"),
                        body)))
                .block(TIMEOUT);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getT1());
        Assertions.assertEquals("application/x-ndjson", response.getT2());
        final List<? extends Map<?, ?>> events = response.getT3().lines()
                .map(this::readEvent)
                .toList();
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals("account", events.get(0).get("type"));
        Assertions.assertEquals("showcase@example.com", ((Map<?, ?>) events.get(0).get("account")).get("email"));
        Assertions.assertEquals("activity", events.get(1).get("type"));
        Assertions.assertEquals("first", ((Map<?, ?>) events.get(1).get("record")).get("message"));
        Assertions.assertEquals("activity", events.get(2).get("type"));
        Assertions.assertEquals("second", ((Map<?, ?>) events.get(2).get("record")).get("message"));
    }

    private Map<?, ?> readEvent(final String line) {
        try {
            return new ObjectMapper().readValue(line, Map.class);
        } catch (final java.io.IOException exception) {
            throw new AssertionError("invalid showcase NDJSON", exception);
        }
    }

    private static AppbaseCore configuredCore() {
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final AppbaseServiceEntry accountEntry = Mockito.mock(AppbaseServiceEntry.class);
        final ReactiveUserAccountStore accountStore = Mockito.mock(ReactiveUserAccountStore.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(accountEntry);
        Mockito.when(accountEntry.getService()).thenReturn(accountStore);
        final UserAccount account = new UserAccount();
        account.setEmail("showcase@example.com");
        Mockito.when(accountStore.readByEmailReactive("showcase@example.com")).thenReturn(Mono.just(account));

        final AppbaseServiceEntry authorizationEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class))
                .thenReturn(authorizationEntry);
        Mockito.when(authorizationEntry.getService()).thenReturn(Mockito.mock(AuthorizationService.class));
        Mockito.when(core.getLogService().getLogAccessor().streamLogRecordsBetweenReactive(0, 2000, 2))
                .thenReturn(Flux.just(
                        Map.entry("stacktest", record("first")),
                        Map.entry("stacktest", record("second"))));
        return core;
    }

    private static LogRecord record(final String message) {
        return LogRecord.builder()
                .timestamp(1000L)
                .threadName("stacktest")
                .logLevel(LogLevel.INFO)
                .message(message)
                .formattedMessage(message)
                .loggerName("com.example.Stacktest")
                .build();
    }
}
