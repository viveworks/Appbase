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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.List;

/**
 * End-to-end coverage for {@link NettyWebExchange#writeStream(Flux)} and for
 * {@link ReactorNettyAppbaseWebServer#start()}'s double-start guard, driven against a REAL server
 * bound on an ephemeral port with a real {@link HttpClient}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NettyWebExchangeWriteStreamTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private ReactorNettyAppbaseWebServer server;

    @AfterEach
    void tearDown() {
        if (server != null && server.isStarted()) {
            server.stop();
        }
    }

    @Test
    void writeStreamProducesNdjsonBodyWithTheDeclaredContentType() {
        server = newServer();
        server.start();

        final Tuple2<String, String> bodyAndContentType = fetchStream();

        Assertions.assertEquals("1\n2\n3\n4\n5\n", bodyAndContentType.getT1());
        Assertions.assertEquals("application/x-ndjson", bodyAndContentType.getT2());
    }

    @Test
    void startingTwiceThrowsAndTheServerStaysUsableAndStoppable() {
        server = newServer();
        server.start();

        Assertions.assertThrows(IllegalStateException.class, () -> server.start());

        // the rejected second start must not have disturbed the first bound server
        final Tuple2<String, String> bodyAndContentType = fetchStream();
        Assertions.assertEquals("1\n2\n3\n4\n5\n", bodyAndContentType.getT1());

        server.stop();
        Assertions.assertFalse(server.isStarted());
    }

    private Tuple2<String, String> fetchStream() {
        return HttpClient.create()
                .host("127.0.0.1")
                .port(server.getPort())
                .get()
                .uri("/stream")
                .responseSingle((response, bytes) -> bytes.asString()
                        .map(body -> Tuples.of(body, response.responseHeaders().get("Content-Type"))))
                .block(TIMEOUT);
    }

    private static ReactorNettyAppbaseWebServer newServer() {
        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder()
                        .host("127.0.0.1")
                        .port(0)
                        .build();

        final AppbaseRouteRegistry routeRegistry = new AppbaseRouteRegistry();
        routeRegistry.route("GET", "/stream", exchange -> exchange.writeStream(Flux.range(1, 5)));

        return new ReactorNettyAppbaseWebServer(configuration, routeRegistry, List.of(), new ObjectMapper());
    }
}
