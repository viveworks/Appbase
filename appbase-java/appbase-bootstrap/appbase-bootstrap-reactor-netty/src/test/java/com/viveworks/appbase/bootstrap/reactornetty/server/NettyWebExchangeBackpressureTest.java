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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Proves {@code NettyWebExchange#writeStream} honours transport write demand rather than collecting
 * the {@link Flux} first.
 *
 * <p>Asserting this with a {@code StepVerifier} on a bare {@link Flux} would prove nothing: that
 * test passes whether or not {@code writeStream} buffers, because it never touches
 * {@code writeStream}. This test streams through a real socket to a deliberately slow client, and
 * asserts the server-side source did not run unboundedly ahead of what the client consumed.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NettyWebExchangeBackpressureTest {

    /** Generous: reactor-netty plus the OS socket buffer legitimately permit some run-ahead. */
    private static final long RUN_AHEAD_LIMIT = 50_000L;

    private ReactorNettyAppbaseWebServer server;

    @AfterEach
    void stopServer() {
        if (server != null && server.isStarted()) {
            server.stop();
        }
    }

    @Test
    void writeStreamGatesAnUnboundedSourceOnTransportDemandRatherThanCollectingIt() {
        final AtomicLong produced = new AtomicLong();

        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", "/stream", exchange ->
                exchange.writeStream(Flux.generate(sink -> sink.next(produced.incrementAndGet()))));

        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder()
                        .enabled(true).host("127.0.0.1").port(0).build();

        server = new ReactorNettyAppbaseWebServer(configuration, registry, List.of(), new ObjectMapper());
        server.start();

        // A slow client: take only the first few chunks, then cancel the response.
        HttpClient.create()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .get().uri("/stream")
                .response((response, byteBufFlux) -> byteBufFlux.asByteArray().take(3))
                .take(3)
                .blockLast(Duration.ofSeconds(10));

        final long producedAfterCancel = produced.get();

        Assertions.assertTrue(producedAfterCancel > 0, "the source must have produced something");
        Assertions.assertTrue(producedAfterCancel < RUN_AHEAD_LIMIT,
                "writeStream must gate on transport demand, not collect the Flux; produced="
                        + producedAfterCancel + " elements for a client that read 3. A writeStream that "
                        + "collects first would run effectively unbounded before the first byte is written.");
    }

    @Test
    void aFullyConsumedStreamDeliversEveryElement() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", "/finite", exchange -> exchange.writeStream(Flux.range(1, 5)));

        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder()
                        .enabled(true).host("127.0.0.1").port(0).build();

        server = new ReactorNettyAppbaseWebServer(configuration, registry, List.of(), new ObjectMapper());
        server.start();

        final String body = HttpClient.create()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .get().uri("/finite")
                .responseSingle((response, content) -> content.asString())
                .block(Duration.ofSeconds(10));

        // NDJSON: one JSON value per line. See the writeStream framing decision in NettyWebExchange.
        Assertions.assertEquals("1\n2\n3\n4\n5\n", body,
                "every element must reach the client, newline-delimited, when fully consumed");
    }
}
