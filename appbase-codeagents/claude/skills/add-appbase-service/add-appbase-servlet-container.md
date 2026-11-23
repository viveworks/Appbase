# add-appbase-servlet-container

Step-by-step recipe for adding a new embedded servlet container implementation to Appbase.

---

## 1. Overview

Use this recipe when you need to integrate an embedded servlet container beyond the existing
Jetty, Undertow, and Tomcat implementations.

**Two interfaces to implement** (both in `appbase-bootstrap`):
- `AppbaseServletDeployment` — registers servlets and filters via the container's programmatic API
- `AppbaseWebServer` — manages the container lifecycle: `start()`, `stop()`, `getPort()`, `isStarted()`

**Module naming:** `appbase-bootstrap-<container>` with package `com.viveworks.appbase.bootstrap.<container>.*`

Replace `<container>` with the lowercase container name (e.g. `netty`, `tomee`) and
`<Container>` with its PascalCase equivalent (e.g. `Netty`, `TomEE`) throughout.

---

## 2. POM Scaffold

### New module: `appbase-java/appbase-bootstrap-<CONTAINER>/pom.xml`

Derive from the Tomcat POM. Mark every container-specific string with `<CONTAINER>`.

```xml
<?xml version='1.0' encoding='UTF-8'?>

<!--
  Copyright 2019 The original author or authors of Appbase

  This project includes software developed by the author or authors of
  Appbase (https://example.invalid/scratch-rehearsal).

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-parent</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>appbase-bootstrap-<CONTAINER></artifactId>
    <name>Appbase Bootstrap <Container></name>
    <description><Container> embedded servlet-container implementation of AppbaseWebServer</description>
    <url>https://example.invalid/scratch-rehearsal/tree/develop/Java/${project.artifactId}</url>
    <scm><url>https://example.invalid/scratch-rehearsal/tree/develop/Java/${project.artifactId}</url></scm>
    <distributionManagement>
        <site><id>${project.artifactId}</id>
            <url>https://example.invalid/scratch-rehearsal/tree/develop/Java/${project.artifactId}</url></site>
    </distributionManagement>

    <dependencies>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-bootstrap</artifactId>
            <version>${appbase.version}</version>
        </dependency>
        <!-- Container-specific embed library; version is managed in root depMgmt -->
        <dependency>
            <groupId><!-- container groupId --></groupId>
            <artifactId><!-- container embed artifactId --></artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.glassfish.jersey.containers</groupId>
            <artifactId>jersey-container-servlet-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.inject</groupId>
            <artifactId>guice</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <configuration>
                    <archive><manifest>
                        <addClasspath>${archive.addClasspath}</addClasspath>
                        <addDefaultImplementationEntries>${archive.addDefaultImplementationEntries}</addDefaultImplementationEntries>
                        <addDefaultSpecificationEntries>${archive.addDefaultSpecificationEntries}</addDefaultSpecificationEntries>
                        <useUniqueVersions>${archive.useUniqueVersions}</useUniqueVersions>
                        <packageName>com.viveworks.appbase.bootstrap.<container></packageName>
                    </manifest></archive>
                    <instructions>
                        <Export-Package>com.viveworks.appbase.bootstrap.<container>.*</Export-Package>
                        <Import-Package>!.,*</Import-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!--
            Opts this module into the failsafe IT execution defined in the parent's
            develop-profile/staging-profile/release-profile pluginManagement (version
            + executions). Since Maven merges same-id profiles across POMs in the
            reactor, declaring one profile per id here causes failsafe to run
            *IT.java tests during the verify phase whenever any of those 3 profiles
            is active; quick-profile never runs them.
        -->
        <profile>
            <id>develop-profile</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-failsafe-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>staging-profile</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-failsafe-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>release-profile</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-failsafe-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### POM changes — two different files, not one

⚠️ **The version lives at the repo root; the module registration lives with its siblings.**
`appbase-java/pom.xml` is a pure aggregator — it has **no `<properties>` and no
`<dependencyManagement>`** — so 1 and 3 below cannot go there.

**1. Property** — in the **REPO-ROOT `pom.xml`** `<properties>`:
```xml
<<container>.version>X.Y.Z</<container>.version>
```

**2. Module** — in `appbase-java/appbase-bootstrap/pom.xml` `<modules>`, beside
`appbase-bootstrap-jetty` / `-tomcat` / `-undertow` / `-reactor-netty`:
```xml
<module>appbase-bootstrap-<container></module>
```

**3. Dependency management** — in the **REPO-ROOT `pom.xml`** `<dependencyManagement><dependencies>`:
```xml
<dependency>
    <groupId><!-- container groupId --></groupId>
    <artifactId><!-- container embed artifactId --></artifactId>
    <version>${<container>.version}</version>
</dependency>
```

🔴 **Never declare a `<version>` inside a module POM.** The module declares the dependency by
groupId/artifactId only and inherits the version from the root pom's `<dependencyManagement>`.

---

## 3. AppbaseServletDeployment Implementation

### Comparison: how each existing container maps servlets and filters

| Container | `addServlet` | `addFilter` |
|---|---|---|
| **Jetty** | `context.addServlet(new ServletHolder(name, clazz), pattern)` — once per pattern | `FilterHolder holder = new FilterHolder(clazz); holder.setName(name);` then `context.addFilter(holder, pattern, REQUEST_FORWARD)` once per pattern. `REQUEST_FORWARD = EnumSet.of(REQUEST, FORWARD)` |
| **Undertow** | `deploymentInfo.addServlet(Servlets.servlet(name, clazz).addMappings(urlPatterns))` — all patterns in one call | `deploymentInfo.addFilter(Servlets.filter(name, clazz))` once, then `deploymentInfo.addFilterUrlMapping(name, pattern, DispatcherType.REQUEST)` per pattern |
| **Tomcat** | `Tomcat.addServlet(ctx, name, clazz.getName())` + `ctx.addServletMappingDecoded(pattern, name)` per pattern | `FilterDef def = new FilterDef(); def.setFilterName(name); def.setFilterClass(clazz.getName()); ctx.addFilterDef(def);` then per pattern: `FilterMap map = new FilterMap(); map.setFilterName(name); map.addURLPattern(pattern); ctx.addFilterMap(map);` |

### Implementation skeleton

```java
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
package com.viveworks.appbase.bootstrap.<container>.server;

import com.viveworks.appbase.bootstrap.server.AppbaseServletDeployment;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
// Container-specific imports

import java.util.Objects;

/**
 * <Container> implementation of {@link AppbaseServletDeployment}, wrapping the
 * container's programmatic deployment API.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
public final class <Container>ServletDeployment implements AppbaseServletDeployment {

    private final <ContainerContext> context;

    /**
     * Creates a deployment over the given <container> context.
     *
     * @param context the <container> context to populate
     */
    public <Container>ServletDeployment(final <ContainerContext> context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Adds a servlet to the <container> context under the given URL patterns.
     *
     * @param name        the servlet name
     * @param servlet     the servlet class
     * @param urlPatterns one or more URL patterns to map the servlet to
     */
    @Override
    public void addServlet(final String name, final Class<? extends Servlet> servlet,
                           final String... urlPatterns) {
        // Use the container's programmatic API — see comparison table above.
    }

    /**
     * Adds a filter to the <container> context under the given URL patterns.
     *
     * @param name        the filter name
     * @param filter      the filter class
     * @param urlPatterns one or more URL patterns to map the filter to
     */
    @Override
    public void addFilter(final String name, final Class<? extends Filter> filter,
                          final String... urlPatterns) {
        // Use the container's programmatic API — see comparison table above.
    }
}
```

---

## 4. AppbaseWebServer Implementation (Builder pattern)

Follow the pattern in `UndertowAppbaseWebServer` and `TomcatAppbaseWebServer`.

### Step 4.R: Builder property review *(complete this before generating the Builder)*

**1. Discover properties from the container's configuration API**

Look at the embed library added to the POM (e.g. `netty-all`, `tomcat-embed-core`, `undertow-servlet`). Find its server/connector configuration API — the builder, options class, or connector setters that control networking, threading, TLS, and request limits. List every configurable property the library exposes for the embedded HTTP server.

Good places to look:
- Netty: `ServerBootstrap`, `ChannelOption`, `SslContextBuilder`, `HttpServerCodec` constructor params
- Tomcat: `Connector` setters, `ProtocolHandler` attributes
- Undertow: `Undertow.Builder` and `UndertowOptions` constants
- Other containers: the server builder or factory class

**2. Merge with seed suggestions and present the combined menu**

Merge the container-discovered properties with the seed list below (de-duplicate by name). Present the merged table to the user:

> "I found the following configurable properties in [Container name]. These are the ones I can expose in the `<Container>AppbaseWebServer.Builder`. Check which ones you want to include, remove any you don't need, or add properties that are missing from this list."

*Seed suggestions (extend with container discovery — do not limit to this list):*

| # | Property | Java Type | `@Nullable`? | Default | Notes |
|---|---|---|---|---|---|
| 1 | `host` | `String` | No | `"0.0.0.0"` | Bind address; `"0.0.0.0"` = all interfaces |
| 2 | `port` | `int` | No | `8080` | HTTP listen port |
| 3 | `minThreads` | `int` | No | `8` | Min threads in the server thread pool |
| 4 | `maxThreads` | `int` | No | `200` | Max threads in the server thread pool |
| 5 | `idleTimeoutMs` | `long` | No | `30000` | Idle thread / connection timeout (ms) |
| 6 | `requestTimeoutMs` | `long` | No | `60000` | Max time to process one HTTP request (ms) |
| 7 | `maxRequestSizeBytes` | `long` | No | `10485760` | Max HTTP request body size (10 MB default) |
| 8 | `acceptQueueSize` | `int` | No | `128` | OS accept-queue depth (TCP backlog) |
| 9 | `ssl` | `boolean` | No | `false` | Enable HTTPS; requires `keystorePath` + `keystorePassword` |
| 10 | `keystorePath` | `String` | Yes | `null` | Path to JKS/PKCS12 keystore (when `ssl=true`) |
| 11 | `keystorePassword` | `String` | Yes | `null` | Keystore password (when `ssl=true`) |
| 12 | `tlsProtocol` | `String` | No | `"TLSv1.3"` | TLS protocol version (when `ssl=true`) |
| 13 | `clientAuth` | `boolean` | No | `false` | Require client certificate / mTLS (when `ssl=true`) |
| 14 | `sendServerVersion` | `boolean` | No | `false` | Include `Server` header in HTTP responses |
| 15 | `compressionEnabled` | `boolean` | No | `false` | Enable gzip response compression |

**3. Accept user input**

The user may:
- Confirm the full list as-is
- Remove specific properties by name or number
- Add properties not in the list (provide: name, Java type, default, `@Nullable`?)
- Rename, retype, or change defaults on any entry

**Wait for final user confirmation before generating the Builder.** Include only the confirmed properties. The `appbaseServices`, `resourceConfig`, `coreServlets`, `serviceFilters`, and `serviceServlets` fields are always required — do not remove them.

---

### Builder fields

```java
@Nullable private AppbaseServices appbaseServices;
@Nullable private ResourceConfig  resourceConfig;
private int    port = DEFAULT_PORT;   // static final int DEFAULT_PORT = 8080;
private String host = DEFAULT_HOST;   // static final String DEFAULT_HOST = "0.0.0.0";
private boolean coreServlets;
private boolean serviceFilters;
private boolean serviceServlets;
```

### `build()` outline

```java
public <Container>AppbaseWebServer build() {
    Objects.requireNonNull(appbaseServices,
        "appbaseServices required — call from(AppbaseServices) before build()");
    final ResourceConfig rc = resourceConfig != null ? resourceConfig : new ResourceConfig();

    // 1. Set up the container.
    //    If Tomcat-derived: call getConnector() BEFORE addContext() to initialize
    //    the connector so the address property can be set.
    //    If Undertow-derived: use a unique deployment name to avoid collisions in
    //    the JVM-global Undertow deployment registry, e.g.:
    //      .setDeploymentName("appbase-" + System.nanoTime() + ".war")

    // 2. Wire Jersey via a pre-configured instance — NOT init-param class-name indirection.
    //    new ServletContainer(rc)   ← pass the ResourceConfig instance directly
    //    Undertow: wrap with ImmediateInstanceFactory<>(new ServletContainer(rc))
    //    Tomcat:   Tomcat.addServlet(ctx, "jersey", new ServletContainer(rc))
    //              ctx.addServletMappingDecoded("/*", "jersey")

    // 3. Create the deployment adapter and register Appbase servlets/filters.
    final <Container>ServletDeployment deployment = new <Container>ServletDeployment(/* context */);
    if (coreServlets) {
        AppbaseServletRegistrar.addCoreServlets(deployment,
            AppbaseServletRegistrar.emptyExcludes(),
            AppbaseServletRegistrar.emptyPathOverrides());
    }
    if (serviceFilters) {
        AppbaseServletRegistrar.addServiceFilters(deployment,
            AppbaseServletRegistrar.emptyExcludes());
    }
    if (serviceServlets) {
        AppbaseServletRegistrar.addServiceServlets(deployment,
            AppbaseServletRegistrar.emptyExcludes(),
            AppbaseServletRegistrar.emptyPathOverrides());
    }

    // 4. Return the configured server — do NOT call start() here.
    return new <Container>AppbaseWebServer(/* container handle */, appbaseServices, port);
}
```

### `start()` / `stop()`

```java
@Override
public void start() {
    try {
        // Start the container.
        started = true;
    } catch (ContainerSpecificCheckedException e) {
        throw new AppbaseRuntimeException("Failed to start <Container>AppbaseWebServer", e);
    }
}

@Override
public void stop() {
    try {
        // Stop (and destroy if applicable) the container.
    } catch (ContainerSpecificCheckedException e) {
        throw new AppbaseRuntimeException("Failed to stop <Container>AppbaseWebServer", e);
    } finally {
        started = false;              // always clear the flag
        appbaseServices.shutdown();  // always shut down services
    }
}
```

Catch the container's narrowest checked exception (e.g. `LifecycleException` for Tomcat).
Do NOT catch bare `Exception` unless the container's stop method declares only `Exception`.

---

## 5. Guice Module

```java
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
package com.viveworks.appbase.bootstrap.<container>.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.<container>.server.<Container>AppbaseWebServer;

import java.util.Objects;

/**
 * Guice module that binds {@link <Container>AppbaseWebServer}.
 *
 * <p>Usage:
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new AppbaseCoreServicesModule(core),
 *     new AppbaseClientServicesModule(clientServices),
 *     new AppbaseServicesModule(appbaseServices),
 *     new <Container>AppbaseServerModule(server));
 * <Container>AppbaseWebServer server = injector.getInstance(<Container>AppbaseWebServer.class);
 * server.start();
 * }</pre>
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
public final class <Container>AppbaseServerModule extends AbstractModule {

    private final <Container>AppbaseWebServer server;

    /**
     * Constructs a module backed by an already-built (but not yet started)
     * {@link <Container>AppbaseWebServer}.
     *
     * @param server the fully-configured server instance
     */
    public <Container>AppbaseServerModule(final <Container>AppbaseWebServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    protected void configure() {
        bind(<Container>AppbaseWebServer.class).toInstance(server);
    }
}
```

---

## 6. Integration Test

The class name **must** end in `IT` so Maven Failsafe picks it up.
Run with: `mvn verify -pl appbase-bootstrap-<container> -P develop-profile`

Use `PingResource` rather than the Appbase `/health` endpoint because the health endpoint
requires a `HealthCheckService` in the service registry. The IT goal is to prove that
the container boots and routes requests through Jersey's `ServletContainer` correctly.

You also need a `<Container>TestServices` helper (see `TomcatTestServices` for the pattern)
that builds a minimal `AppbaseServices` from a test YAML with all heavyweight services
(JPA, Lucene) disabled.

```java
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
package com.viveworks.appbase.bootstrap.<container>.server;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test that boots an embedded <container> server and asserts an HTTP
 * endpoint answers 200.
 *
 * <p>A trivial JAX-RS ping resource is used instead of the full appbase health endpoint
 * because the health endpoint requires a {@code HealthCheckService} instance to be present
 * in the Appbase service registry. The purpose of this test is to prove that <Container>
 * starts, accepts HTTP connections, and routes requests through the Jersey
 * {@code ServletContainer} correctly.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
final class <Container>AppbaseServerIT {

    /**
     * Trivial JAX-RS resource with no service dependencies, used only by this IT.
     */
    @Path("/ping")
    public static class PingResource {
        /**
         * Returns a plain-text {@code "pong"} body with HTTP 200.
         *
         * @return the pong body
         */
        @GET
        public String ping() {
            return "pong";
        }
    }

    @Test
    void pingEndpoint_returns200() throws Exception {
        final int port = freePort();
        final AppbaseServices services = <Container>TestServices.minimal();
        final <Container>AppbaseWebServer server = <Container>AppbaseWebServer.builder()
            .from(services)
            .port(port)
            .resourceConfig(new ResourceConfig().register(PingResource.class))
            .build();
        server.start();
        try {
            final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ping"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(),
                "<Container> + Jersey must return 200 on GET /ping");
        } finally {
            server.stop();
        }
    }

    /**
     * Returns an available TCP port by briefly binding to port 0 and reading the
     * OS-assigned port before releasing the socket.
     */
    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
```

---

## 7. Checklist before committing

- [ ] `@author Navaneeth Mysore G` on every new Java class
- [ ] `@author Claude Sonnet 4.6` on every new Java class
- [ ] Javadoc on every `public` method
- [ ] Apache 2.0 license header on every new Java and XML file
- [ ] `started = false` in the `finally` block of `stop()`
- [ ] `appbaseServices.shutdown()` in the `finally` block of `stop()`
- [ ] Catch the container's narrowest checked exception in `start()` / `stop()` — not bare `Exception` (e.g. `LifecycleException` for Tomcat)
- [ ] If Tomcat-derived: call `tomcat.getConnector()` **before** `tomcat.addContext()` to initialize the connector so the address property can be set
- [ ] If Undertow-derived: use a unique deployment name (e.g. `"appbase-" + System.nanoTime() + ".war"`) to avoid collisions in the JVM-global Undertow deployment registry
- [ ] Jersey wired via `new ServletContainer(rc)` instance — NOT class-name init-param
- [ ] `build()` returns the server but does NOT call `start()`
- [ ] Integration test class name ends in `IT`
- [ ] `<module>appbase-bootstrap-<container></module>` added to `appbase-java/appbase-bootstrap/pom.xml`
- [ ] `<<container>.version>X.Y.Z</<container>.version>` property added to root `pom.xml`
- [ ] Dependency management entry for the container library added to root `pom.xml`
