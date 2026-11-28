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

package com.example.appbase.quickstart.jetty;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Appbase#2545 README Swagger-UI evidence: starts a real embedded Jetty + Jersey service with
 * Appbase Core's own servlets live ({@code withAppbaseCoreServlets()}) plus this example's own
 * {@link TasksResource}, serves one combined OpenAPI document covering both (see
 * {@link CombinedOpenApiSupport}), and a real Swagger UI (extracted from the real
 * {@code org.webjars:swagger-ui} jar already on this example's classpath -- the same artifact
 * {@code appbase-core} itself depends on, see its pom.xml) over that document.
 *
 * <p>Opt-in only ({@code -Dswagger.capture=true}): not part of the ordinary test run. Writes the
 * running port to {@code -Dswagger.capture.readyFile}, then blocks until
 * {@code -Dswagger.capture.stopFile} appears (an external process captures a real screenshot of
 * the real running page in between), then shuts the server down normally.
 */
@EnabledIfSystemProperty(named = "swagger.capture", matches = "true")
class TasksQuickstartJettyJerseySwaggerCaptureTest {

    private static final String INDEX_HTML = """
            <!doctype html>
            <html>
            <head>
              <meta charset="UTF-8">
              <title>Appbase Core + Tasks Quickstart (Jetty/Jersey)</title>
              <link rel="stylesheet" href="swagger-ui.css">
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="swagger-ui-bundle.js"></script>
              <script>
                window.onload = function () {
                  window.ui = SwaggerUIBundle({
                    url: "/openapi.json",
                    dom_id: "#swagger-ui",
                    presets: [SwaggerUIBundle.presets.apis],
                    layout: "BaseLayout"
                  });
                };
              </script>
            </body>
            </html>
            """;

    @Test
    void startsRealServiceWithSwaggerUiAndWaitsForExternalCapture() throws Exception {
        final String readyFileProperty = requireProperty("swagger.capture.readyFile");
        final String stopFileProperty = requireProperty("swagger.capture.stopFile");

        extractSwaggerUiAssets();

        final EntityManagerFactory entityManagerFactory =
                Persistence.createEntityManagerFactory("tasks-quickstart-jetty");
        try {
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
            CombinedOpenApiSupport.register(resourceConfig);

            final JettyServletContainer server = JettyServletContainer.builder()
                    .from(appbaseServices)
                    .port(0)
                    .withAppbaseCoreServlets()
                    .withSwaggerUI("/restapi", "/swagger-ui/dist", "index.html")
                    .resourceConfig(resourceConfig)
                    .build();
            server.start();
            try {
                final Path readyFile = Path.of(readyFileProperty);
                Files.writeString(readyFile, Integer.toString(server.getPort()), StandardCharsets.UTF_8);

                final Path stopFile = Path.of(stopFileProperty);
                final Instant deadline = Instant.now().plus(Duration.ofSeconds(180));
                while (!Files.exists(stopFile)) {
                    if (Instant.now().isAfter(deadline)) {
                        throw new IllegalStateException(
                                "Timed out waiting for stop file (external capture did not signal): " + stopFile);
                    }
                    Thread.sleep(250);
                }
            } finally {
                server.stop();
            }
        } finally {
            entityManagerFactory.close();
        }
    }

    private static String requireProperty(final String name) {
        final String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required system property not set: " + name);
        }
        return value;
    }

    /**
     * Extracts {@code swagger-ui-bundle.js} and {@code swagger-ui.css} from the real
     * {@code org.webjars:swagger-ui} jar already resolved onto this test's classpath, plus this
     * class's own {@link #INDEX_HTML}, into {@code target/test-classes/swagger-ui/dist/} so
     * {@code JettyServletContainer.Builder#withSwaggerUI} can serve them from the classpath.
     */
    private static void extractSwaggerUiAssets() throws IOException {
        final String classpath = System.getProperty("java.class.path");
        String webjarJarPath = null;
        for (final String entry : classpath.split(File.pathSeparator)) {
            if (entry.matches(".*[/\\\\]swagger-ui-[0-9][^/\\\\]*\\.jar$")) {
                webjarJarPath = entry;
                break;
            }
        }
        if (webjarJarPath == null) {
            throw new IllegalStateException(
                    "org.webjars:swagger-ui jar not found on the test classpath (expected transitively via "
                            + "appbase-core): " + classpath);
        }

        final Path destDir = Path.of("target", "test-classes", "swagger-ui", "dist");
        Files.createDirectories(destDir);

        try (ZipFile zip = new ZipFile(webjarJarPath)) {
            extractOne(zip, destDir, "swagger-ui-bundle.js");
            extractOne(zip, destDir, "swagger-ui.css");
        }
        Files.writeString(destDir.resolve("index.html"), INDEX_HTML, StandardCharsets.UTF_8);
    }

    private static void extractOne(final ZipFile zip, final Path destDir, final String fileName) throws IOException {
        ZipEntry match = null;
        for (final var entries = zip.entries(); entries.hasMoreElements(); ) {
            final ZipEntry candidate = entries.nextElement();
            if (candidate.getName().startsWith("META-INF/resources/webjars/swagger-ui/")
                    && candidate.getName().endsWith("/" + fileName)) {
                if (match != null) {
                    throw new IllegalStateException("Multiple matches for " + fileName + " in " + zip.getName());
                }
                match = candidate;
            }
        }
        if (match == null) {
            throw new IllegalStateException("No entry named " + fileName + " found in " + zip.getName());
        }
        try (var in = zip.getInputStream(match)) {
            Files.copy(in, destDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
