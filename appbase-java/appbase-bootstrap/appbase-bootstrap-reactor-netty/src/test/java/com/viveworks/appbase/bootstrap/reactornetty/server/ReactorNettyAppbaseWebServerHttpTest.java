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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Map;

/**
 * End-to-end HTTP tests: start the reactor-netty server on an ephemeral port and drive it with a
 * real {@link HttpClient} over a real socket. Ungated -- no external infra -- so it runs in every
 * normal build. Proves route dispatch, the 404 route-miss body, and the port lifecycle over the
 * wire rather than against an in-memory handler.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactorNettyAppbaseWebServerHttpTest {

    private ReactorNettyAppbaseWebServer server;

    @AfterEach
    void stopServer() {
        if (server != null && server.isStarted()) {
            server.stop();
        }
    }

    private ReactorNettyAppbaseWebServer startOnEphemeralPort(final List<AppbaseServiceFilter> filters) {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", "/ping", exchange -> exchange.writeJson(Map.of("pong", true)));

        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder()
                        .enabled(true).host("127.0.0.1").port(0).build();

        server = new ReactorNettyAppbaseWebServer(configuration, registry, filters, new ObjectMapper());
        server.start();
        return server;
    }

    @Test
    void aRegisteredRouteAnswersOverRealHttp() {
        startOnEphemeralPort(List.of());

        final String body = HttpClient.create()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .get().uri("/ping")
                .responseSingle((response, content) -> content.asString())
                .block();

        Assertions.assertEquals("{\"pong\":true}", body);
    }

    @Test
    void anUnknownRouteReturns404() {
        startOnEphemeralPort(List.of());

        final Integer status = HttpClient.create()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .get().uri("/nope")
                .responseSingle((response, content) -> Mono.just(response.status().code()))
                .block();

        Assertions.assertEquals(404, status);
    }

    @Test
    void getPortIsMinusOneBeforeStartAndRealAfter() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder()
                        .enabled(true).host("127.0.0.1").port(0).build();

        server = new ReactorNettyAppbaseWebServer(configuration, registry, List.of(), new ObjectMapper());

        Assertions.assertEquals(-1, server.getPort());
        Assertions.assertFalse(server.isStarted());

        server.start();

        Assertions.assertTrue(server.getPort() > 0);
        Assertions.assertTrue(server.isStarted());
    }
}
