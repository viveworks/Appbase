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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
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
 * Real-socket contract for the R5 activity-feed showcase. This test is written before the route so
 * the route cannot be reduced to an in-memory handler proof: it must stream account and activity
 * events over reactor-netty's NDJSON exchange.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveActivityFeedEndpointHttpTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private ReactorNettyAppbaseWebServer server;

    @AfterEach
    void stopServer() {
        if (server != null && server.isStarted()) {
            server.stop();
        }
    }

    @Test
    void activityFeedStreamsAnAccountAndTwoActivityRecordsOverRealHttp() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = configuredCore(Flux.just(
                Map.entry("showcase", record("first")),
                Map.entry("showcase", record("second"))));

        ReactiveActivityFeedEndpoint.register(registry, core, 100);
        server = newServer(registry);
        server.start();

        final Tuple3<Integer, String, String> response = HttpClient.create()
                .host("127.0.0.1")
                .port(server.getPort())
                .get()
                .uri(ReactiveActivityFeedEndpoint.DEFAULT_PATH
                        + "?email=user@example.com&appbaseRequesterId=req1"
                        + "&inclusiveStartTimestamp=0&exclusiveEndTimestamp=2000&pageSize=2")
                .responseSingle((httpResponse, bytes) -> bytes.asString()
                        .map(body -> Tuples.of(
                                httpResponse.status().code(),
                                httpResponse.responseHeaders().get("Content-Type"),
                                body)))
                .block(TIMEOUT);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getT1());
        Assertions.assertEquals("application/x-ndjson", response.getT2());
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<? extends Map<?, ?>> events = response.getT3().lines()
                .map(line -> readEvent(objectMapper, line))
                .toList();
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals("account", events.get(0).get("type"));
        Assertions.assertEquals("user@example.com", ((Map<?, ?>) events.get(0).get("account")).get("email"));
        Assertions.assertEquals("activity", events.get(1).get("type"));
        Assertions.assertEquals("showcase", events.get(1).get("store"));
        Assertions.assertEquals("first", ((Map<?, ?>) events.get(1).get("record")).get("message"));
        Assertions.assertEquals("activity", events.get(2).get("type"));
        Assertions.assertEquals("second", ((Map<?, ?>) events.get(2).get("record")).get("message"));
        Assertions.assertFalse(response.getT3().contains("passwordHashes"));
        Assertions.assertFalse(response.getT3().contains("passwordResetCodeHash"));
    }

    @Test
    void lateLogFailureEmitsASafeTerminalNdjsonErrorEvent() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final AppbaseCore core = configuredCore(Flux.concat(
                Flux.just(Map.entry("showcase", record("first"))),
                Mono.error(new IllegalStateException("store connection token=secret"))));

        ReactiveActivityFeedEndpoint.register(registry, core, 100);
        server = newServer(registry);
        server.start();

        final String body = HttpClient.create()
                .host("127.0.0.1")
                .port(server.getPort())
                .get()
                .uri(ReactiveActivityFeedEndpoint.DEFAULT_PATH
                        + "?email=user@example.com&appbaseRequesterId=req1"
                        + "&inclusiveStartTimestamp=0&exclusiveEndTimestamp=2000&pageSize=2")
                .responseContent()
                .aggregate()
                .asString()
                .block(TIMEOUT);

        Assertions.assertNotNull(body);
        final List<? extends Map<?, ?>> events = body.lines().map(line -> readEvent(new ObjectMapper(), line)).toList();
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals("account", events.get(0).get("type"));
        Assertions.assertEquals("activity", events.get(1).get("type"));
        Assertions.assertEquals("error", events.get(2).get("type"));
        Assertions.assertEquals(503, events.get(2).get("status"));
        Assertions.assertEquals("Activity feed unavailable", events.get(2).get("message"));
        Assertions.assertFalse(body.contains("token=secret"));
    }

    private static Map<?, ?> readEvent(final ObjectMapper objectMapper, final String line) {
        try {
            return objectMapper.readValue(line, Map.class);
        } catch (final java.io.IOException exception) {
            throw new AssertionError("invalid NDJSON event", exception);
        }
    }

    private static ReactorNettyAppbaseWebServer newServer(final AppbaseRouteRegistry registry) {
        return new ReactorNettyAppbaseWebServer(
                ReactorNettyAppbaseWebServerConfiguration.builder().host("127.0.0.1").port(0).build(),
                registry,
                List.of(),
                new ObjectMapper());
    }

    private static AppbaseCore configuredCore(final Flux<Map.Entry<String, LogRecord>> activityRecords) {
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        final ReactiveUserAccountStore accountStore = Mockito.mock(ReactiveUserAccountStore.class);
        final AppbaseServiceEntry accountEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class))
                .thenReturn(accountEntry);
        Mockito.when(accountEntry.getService()).thenReturn(accountStore);
        final AuthorizationService authorizationService = Mockito.mock(AuthorizationService.class);
        final AppbaseServiceEntry authorizationEntry = Mockito.mock(AppbaseServiceEntry.class);
        Mockito.when(core.getServiceRegistry().getAppbaseService(AuthorizationService.class))
                .thenReturn(authorizationEntry);
        Mockito.when(authorizationEntry.getService()).thenReturn(authorizationService);

        final UserAccount account = new UserAccount();
        account.setEmail("user@example.com");
        Mockito.when(accountStore.readByEmailReactive("user@example.com")).thenReturn(Mono.just(account));

        Mockito.when(core.getLogService().getLogAccessor().streamLogRecordsBetweenReactive(0, 2000, 2))
                .thenReturn(activityRecords);
        return core;
    }

    private static LogRecord record(final String message) {
        return LogRecord.builder()
                .timestamp(1000L)
                .threadName("showcase")
                .logLevel(LogLevel.INFO)
                .message(message)
                .formattedMessage(message)
                .loggerName("com.example.Showcase")
                .build();
    }
}
