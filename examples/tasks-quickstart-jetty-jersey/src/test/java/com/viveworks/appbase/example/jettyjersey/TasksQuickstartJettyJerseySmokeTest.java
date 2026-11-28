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

package com.viveworks.appbase.example.jettyjersey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServicesConfiguration;
import com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Appbase#2545 RL-11 consumer smoke, embedded-Jetty variant: this standalone Maven project
 * depends only on the published com.viveworks.appbase 1.0.0 coordinates resolved from Maven
 * Central (see pom.xml) and starts a real embedded Jetty + Jersey server with embedded JPA +
 * H2 -- the same Tasks resource/service pattern as the Reactor Netty example, under a
 * different embedded-server choice. Every assertion is against a real HTTP response from a
 * real running server, using the plain JDK HttpClient (no extra dependency needed).
 */
class TasksQuickstartJettyJerseySmokeTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private EntityManagerFactory entityManagerFactory;
    private JettyServletContainer server;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() {
        entityManagerFactory = Persistence.createEntityManagerFactory("tasks-quickstart-jetty");

        final AppbaseCoreServicesConfiguration coreConfig = AppbaseCoreServicesConfiguration.builder()
                .logServiceConfiguration(DefaultLogServiceConfiguration.builder().build())
                .metricServiceConfiguration(DefaultMetricServiceConfiguration.builder().build())
                .validationServiceConfiguration(new DefaultValidationServiceConfiguration())
                .mediatorServiceConfiguration(DefaultMediatorServiceConfiguration.builder().build())
                .configurationServiceConfiguration(DefaultConfigurationServiceConfiguration.builder().build())
                .serviceRegistryConfiguration(DefaultServiceRegistryConfiguration.builder().build())
                .exceptionServiceConfiguration(DefaultExceptionServiceConfiguration.builder().build())
                .tracerServiceConfiguration(DefaultTracerServiceConfiguration.builder().build())
                .jmxServiceConfiguration(DefaultJmxServiceConfiguration.builder().build())
                .build();
        final AppbaseCoreServices coreServices = AppbaseCoreServices.builder().from(coreConfig).build();
        final AppbaseServices appbaseServices = AppbaseServices.builder().from(coreServices).build();

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new TasksResource(entityManagerFactory));
        resourceConfig.register(JacksonFeature.class);

        server = JettyServletContainer.builder()
                .from(appbaseServices)
                .port(0)
                .resourceConfig(resourceConfig)
                .build();
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
    void createsListsUpdatesAndDeletesATaskOverRealHttp() throws IOException, InterruptedException {
        final HttpResponse<String> created = request("POST", "/tasks", "{\"title\":\"write README\"}");
        System.out.println("POST /tasks -> " + created.statusCode() + " " + created.body());
        Assertions.assertEquals(201, created.statusCode());
        final JsonNode createdBody = readJson(created.body());
        Assertions.assertEquals("write README", createdBody.get("title").asText());
        Assertions.assertFalse(createdBody.get("done").asBoolean());
        final long id = createdBody.get("id").asLong();

        final HttpResponse<String> listed = request("GET", "/tasks", null);
        System.out.println("GET /tasks -> " + listed.statusCode() + " " + listed.body());
        Assertions.assertEquals(200, listed.statusCode());
        final JsonNode listedBody = readJson(listed.body());
        Assertions.assertTrue(listedBody.isArray());
        Assertions.assertEquals(1, listedBody.size());

        final HttpResponse<String> updated = request("PUT", "/tasks/" + id, "{\"done\":true}");
        System.out.println("PUT /tasks/" + id + " -> " + updated.statusCode() + " " + updated.body());
        Assertions.assertEquals(200, updated.statusCode());
        Assertions.assertTrue(readJson(updated.body()).get("done").asBoolean());

        final HttpResponse<String> fetched = request("GET", "/tasks/" + id, null);
        System.out.println("GET /tasks/" + id + " -> " + fetched.statusCode() + " " + fetched.body());
        Assertions.assertEquals(200, fetched.statusCode());
        Assertions.assertTrue(readJson(fetched.body()).get("done").asBoolean());

        final HttpResponse<String> deleted = request("DELETE", "/tasks/" + id, null);
        System.out.println("DELETE /tasks/" + id + " -> " + deleted.statusCode() + " " + deleted.body());
        Assertions.assertEquals(200, deleted.statusCode());

        final HttpResponse<String> missing = request("GET", "/tasks/" + id, null);
        System.out.println("GET /tasks/" + id + " (after delete) -> " + missing.statusCode() + " " + missing.body());
        Assertions.assertEquals(404, missing.statusCode());
    }

    private HttpResponse<String> request(final String method, final String path, final String body)
            throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + server.getPort() + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json");
        final HttpRequest.BodyPublisher bodyPublisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        builder.method(method, bodyPublisher);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode readJson(final String text) {
        try {
            return new ObjectMapper().readTree(text);
        } catch (final IOException exception) {
            throw new AssertionError("invalid JSON response: " + text, exception);
        }
    }
}
