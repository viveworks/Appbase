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

package com.viveworks.appbase.example.reactornetty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.bootstrap.reactornetty.server.ReactorNettyAppbaseWebServer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Appbase#2545 RL-11 consumer smoke: this standalone Maven project depends only on the
 * published com.viveworks.appbase 1.0.0 coordinates resolved from Maven Central (see pom.xml)
 * and starts a real Reactor Netty server with embedded JPA + H2, following the README quick
 * start. Every assertion here is against a real HTTP response from that running server, not a
 * mocked call.
 */
class TasksQuickstartSmokeTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private EntityManagerFactory entityManagerFactory;
    private ReactorNettyAppbaseWebServer server;

    @BeforeEach
    void startServer() {
        entityManagerFactory = Persistence.createEntityManagerFactory("tasks-quickstart");

        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        TasksEndpoint.register(registry, entityManagerFactory, new ObjectMapper());

        server = new ReactorNettyAppbaseWebServer(
                ReactorNettyAppbaseWebServerConfiguration.builder().host("127.0.0.1").port(0).build(),
                registry,
                List.of(),
                new ObjectMapper());
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null && server.isStarted()) {
            server.stop();
        }
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void createsListsUpdatesAndDeletesATaskOverRealHttp() {
        final Tuple2<Integer, String> created = post("/tasks", "{\"title\":\"write README\"}");
        System.out.println("POST /tasks -> " + created.getT1() + " " + created.getT2());
        Assertions.assertEquals(201, created.getT1());
        final JsonNode createdBody = readJson(created.getT2());
        Assertions.assertEquals("write README", createdBody.get("title").asText());
        Assertions.assertFalse(createdBody.get("done").asBoolean());
        final long id = createdBody.get("id").asLong();

        final Tuple2<Integer, String> listed = get("/tasks");
        System.out.println("GET /tasks -> " + listed.getT1() + " " + listed.getT2());
        Assertions.assertEquals(200, listed.getT1());
        final JsonNode listedBody = readJson(listed.getT2());
        Assertions.assertTrue(listedBody.isArray());
        Assertions.assertEquals(1, listedBody.size());

        final Tuple2<Integer, String> updated = put("/tasks/item?id=" + id, "{\"done\":true}");
        System.out.println("PUT /tasks/item?id=" + id + " -> " + updated.getT1() + " " + updated.getT2());
        Assertions.assertEquals(200, updated.getT1());
        Assertions.assertTrue(readJson(updated.getT2()).get("done").asBoolean());

        final Tuple2<Integer, String> fetched = get("/tasks/item?id=" + id);
        System.out.println("GET /tasks/item?id=" + id + " -> " + fetched.getT1() + " " + fetched.getT2());
        Assertions.assertEquals(200, fetched.getT1());
        Assertions.assertTrue(readJson(fetched.getT2()).get("done").asBoolean());

        final Tuple2<Integer, String> deleted = delete("/tasks/item?id=" + id);
        System.out.println("DELETE /tasks/item?id=" + id + " -> " + deleted.getT1() + " " + deleted.getT2());
        Assertions.assertEquals(200, deleted.getT1());

        final Tuple2<Integer, String> missing = get("/tasks/item?id=" + id);
        System.out.println("GET /tasks/item?id=" + id + " (after delete) -> " + missing.getT1() + " " + missing.getT2());
        Assertions.assertEquals(404, missing.getT1());
    }

    private HttpClient client() {
        return HttpClient.create().host("127.0.0.1").port(server.getPort());
    }

    private Tuple2<Integer, String> get(final String uri) {
        return client().get().uri(uri)
                .responseSingle(TasksQuickstartSmokeTest::toResult)
                .block(TIMEOUT);
    }

    private Tuple2<Integer, String> delete(final String uri) {
        return client().delete().uri(uri)
                .responseSingle(TasksQuickstartSmokeTest::toResult)
                .block(TIMEOUT);
    }

    private Tuple2<Integer, String> post(final String uri, final String body) {
        return client().post().uri(uri)
                .send(ByteBufFlux.fromString(Mono.just(body)))
                .responseSingle(TasksQuickstartSmokeTest::toResult)
                .block(TIMEOUT);
    }

    private Tuple2<Integer, String> put(final String uri, final String body) {
        return client().put().uri(uri)
                .send(ByteBufFlux.fromString(Mono.just(body)))
                .responseSingle(TasksQuickstartSmokeTest::toResult)
                .block(TIMEOUT);
    }

    private static Mono<Tuple2<Integer, String>> toResult(
            final reactor.netty.http.client.HttpClientResponse response, final reactor.netty.ByteBufMono bytes) {
        return bytes.asString().defaultIfEmpty("")
                .map(text -> Tuples.of(response.status().code(), text));
    }

    private JsonNode readJson(final String text) {
        try {
            return new ObjectMapper().readTree(text);
        } catch (final IOException exception) {
            throw new AssertionError("invalid JSON response: " + text, exception);
        }
    }
}
