# Part 4 — Bootstrap flavors

[Guide index](README.md) · previous: [service modules](03-service-modules.md) · next: [configuration wizard](05-config-wizard.md)

The **bootstrap** modules assemble your service and give it an HTTP face. There are **6 bootstrap
modules** under `appbase-java/appbase-bootstrap/`. You always use `appbase-bootstrap-core`; you then pick
**one web server** from the others.

Every snippet below is copied from a test or program in this repository, with its source path and line
range under it. The suite `scripts/test/user_guide_test.sh` checks each snippet line against its source.
Nothing here was started against a clean machine by this guide.

## The six modules

| Module | Role | HTTP stack | In the BOM |
|---|---|---|---|
| `appbase-bootstrap-core` | the container builders and all factories; the `AppbaseWebServer` contract | none | yes |
| `appbase-bootstrap-servlet` | shared layer for the servlet servers (`AppbaseServletRegistrar`, `AppbaseServletDeployment`) | Servlet API | yes |
| `appbase-bootstrap-servlet-jetty` | embedded Jetty 11 | servlet | yes |
| `appbase-bootstrap-servlet-tomcat` | embedded Tomcat | servlet | yes |
| `appbase-bootstrap-servlet-undertow` | embedded Undertow | servlet | yes |
| `appbase-bootstrap-reactor-netty` | embedded Reactor Netty, no servlet container | non-blocking | **yes** |

So there are **four web-server flavors**: Jetty, Tomcat, Undertow (servlet) and Reactor Netty (reactive),
on top of the core, with `appbase-bootstrap-servlet` as the layer the three servlet servers share. All
four servers implement `AppbaseWebServer` (`start()`, `stop()`, `getPort()`, `isStarted()`).

> **BOM coverage.** `appbase-bootstrap-reactor-netty` is managed by
> `appbase-bom/pom.xml`. The groupId remains a placeholder, **pending the rename**.

### Which one do I pick?

| If you need | Pick |
|---|---|
| the servlet and JAX-RS faces, the core REST resources, Swagger static files | **Jetty** (the most complete builder) |
| the servlet faces on another container | Tomcat or Undertow. Their builders offer fewer options (below) |
| an event-loop server that never blocks it | Reactor Netty. It exposes 12 endpoints (see its [README](../../appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty/README.md)); it is not servlet-based |

The stack test notes that the non-Jetty containers support the core Appbase services only and that
parity is not yet in place. Treat Tomcat and Undertow as less exercised than Jetty.

## Step 1 to 3: core, clients, services

All flavors share the same three builders. Core services are built from a YAML file. Client services and
application services are optional. A bare core and an empty client container is a valid start. The test
YAML used below is `appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-jetty/src/test/resources/appbase-test-config.yaml`.
For a real service you generate your own with the [configuration wizard](05-config-wizard.md), and
register clients ([part 2](02-client-integrations.md)) and services ([part 3](03-service-modules.md)).

## Flavor: Jetty

Core, clients, services and a Jetty server on an ephemeral port, from the Jetty module's integration
test. `port(0)` asks the operating system for a free port; leave `port(…)` out for the default **8080**.

<!-- snippet-source: appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-jetty/src/test/java/com/viveworks/appbase/bootstrap/jetty/AppbaseBootstrapIntegrationTest.java:157-177 -->
```java
AppbaseCoreServices core = AppbaseCoreServices.builder()
    .fromYaml(CONFIG)
    .build();
try {
    AppbaseClientServices clients = AppbaseClientServices.builder()
        .from(core)
        .build();
    AppbaseServices services = AppbaseServices.builder()
        .from(core)
        .withClientServices(clients)
        .build();
    // port(0) asks the OS for an ephemeral port — avoids conflicts in parallel CI runs
    JettyServletContainer server = JettyServletContainer.builder()
        .from(services)
        .port(0)
        .build();
    server.start();
    server.stop();
} finally {
    core.shutdown();
}
```

Source: `appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-jetty/src/test/java/com/viveworks/appbase/bootstrap/jetty/AppbaseBootstrapIntegrationTest.java`, lines 157-177.

Builder options on `JettyServletContainer.builder()` (verified in
`appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-jetty/src/main/java/com/viveworks/appbase/bootstrap/jetty/server/JettyServletContainer.java`):
`from`, `server`, `port`, `resourceConfig`, `withDefaultMXBeans`, `withAppbaseCoreServlets`,
`withAppbaseCoreOpenApi`,
`withAppbaseServiceFilters`, `withAppbaseServiceServlets`, `withSwaggerUI`, `withAdditionalHandler`,
`withServletContext`. The full chain used by the stack test, with the core REST resources, is under
*All three servlet servers* below.

## Flavor: Tomcat and Undertow

`TomcatServletContainer.builder()` and `UndertowServletContainer.builder()` have the same shape with a
smaller option set: `from`, `port` (default 8080), `host`, `resourceConfig`, `withAppbaseCoreServlets`,
`withAppbaseCoreOpenApi`,
`withAppbaseServiceFilters`, `withAppbaseServiceServlets`. There is **no** `withDefaultMXBeans` and **no**
`withSwaggerUI` on these two.

For every servlet container, core OpenAPI is separately opt-in and off by default. Chain
`withAppbaseCoreServlets().withAppbaseCoreOpenApi()` to serve the core-only document at
`/openapi.json`. Calling `withAppbaseCoreOpenApi()` without enabling core servlets has no effect.

### All three servlet servers

The stack test's program selects the server from a `--container` argument. These are its three cases,
with the application-specific `resourceConfig` and servlet context kept as they are in the source
(`buildResourceConfig`, `StackTestServletConfig`, `StackTestRuntimeException` and `StackTestServices` are test code):

<!-- snippet-source: appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java:290-297,300-307,311-327 -->
```java
        stackTestServer = UndertowServletContainer.builder()
                .from(appbaseServices)
                .port(jettyPort)
                .withAppbaseCoreServlets()
                .withAppbaseServiceFilters()
                .withAppbaseServiceServlets()
                .resourceConfig(buildResourceConfig(stackTestServices))
                .build();
// ...
        stackTestServer = TomcatServletContainer.builder()
                .from(appbaseServices)
                .port(jettyPort)
                .withAppbaseCoreServlets()
                .withAppbaseServiceFilters()
                .withAppbaseServiceServlets()
                .resourceConfig(buildResourceConfig(stackTestServices))
                .build();
// ...
        stackTestServer = JettyServletContainer.builder()
                .from(appbaseServices)
                .port(jettyPort)
                .withDefaultMXBeans()
                .withAppbaseCoreServlets()
                .withAppbaseServiceFilters()
                .withAppbaseServiceServlets()
                .withSwaggerUI("/restapi", "/swagger-ui/dist", "index.html")
                .resourceConfig(buildResourceConfig(stackTestServices))
                .withServletContext(ctx -> {
                    try {
                        StackTestServletConfig.configure(ctx, stackTestServices, appbaseServices);
                    } catch (IOException exception) {
                        throw new StackTestRuntimeException(exception);
                    }
                })
                .build();
```

Source: `appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java`, lines 290-297,300-307,311-327.

All three servlet builders support Jersey `ResourceConfig.packages(...)` scanning on JDK 25 through
the managed Jersey 3.1.11 runtime. Because Jersey embeds its own ASM scanner, adding a separate ASM
dependency does not extend that support. On Java 26 or later, a builder rejects package scanning
before constructing the Jersey servlet. The error says “Jersey package scanning is supported through
Java 25, but this server is running on Java N” and directs you to use explicit
`ResourceConfig.register(Class)`, a supported Java runtime, or upgraded Appbase/Jersey. Explicit
class registration remains available on later runtimes. If Appbase cannot inspect a future Jersey
layout safely, it fails closed with the same remedies rather than starting unverified scanning.

## Flavor: Reactor Netty

The reactive face needs a `HealthCheckService` in the service container (its builder reads one). This is
the minimal service container and server, copied from the module's secure-by-default test. The test uses
an explicit ephemeral port: without a configuration the builder binds **8080**.

<!-- snippet-source: appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty/src/test/java/com/viveworks/appbase/bootstrap/reactornetty/server/ReactorNettyAppbaseWebServerSecureByDefaultTest.java:100-107,119-126 -->
```java
final AppbaseCoreServices coreServices = AppbaseCoreServices.builder().fromYaml(CONFIG).build();
startedCoreServices.add(coreServices);

final AppbaseServices services = AppbaseServices.builder()
        .from(coreServices)
        .secureByDefault(secureByDefault)
        .with(new DefaultHealthCheckServiceFactory(DefaultHealthCheckServiceConfiguration.builder().build()))
        .build();
// ...
final ReactorNettyAppbaseWebServerConfiguration configuration =
        ReactorNettyAppbaseWebServerConfiguration.builder()
                .enabled(true).host("127.0.0.1").port(0).build();
final ReactorNettyAppbaseWebServer server = ReactorNettyAppbaseWebServer.builder()
        .from(services)
        .configuration(configuration)
        .build();
server.start();
```

Source: `appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty/src/test/java/com/viveworks/appbase/bootstrap/reactornetty/server/ReactorNettyAppbaseWebServerSecureByDefaultTest.java`, lines 100-107,119-126.

In that test `secureByDefault` is a method parameter and `CONFIG` is
`classpath:///appbase-test-config.yaml`; use `true` (the default) and your own YAML path.

The module's YAML form of this configuration is shown in its
[README](../../appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty/README.md), which also lists its twelve
endpoints. The module's own scope note says it is not a servlet container and has no servlet API on its
request path.

## What the web server serves, and who may call it

The container is **secure by default** ([part 3](03-service-modules.md)): without your own authorizer
every request except `/health` is denied with HTTP 403. The Jetty test file pins this over real HTTP: one
test asserts that an administrative configuration read is denied and another that the health path is
permitted. They are `builderPath_secureByDefault_jettyDeniesAdminConfigurationReadOverRealHttp` and
`builderPath_secureByDefault_jettyPermitsHealthPathOverRealHttp` in the Jetty integration test above.

## Stopping

Stop the server first with `stop()`, then shut the containers down. The Jetty test calls
`server.stop()`, and its container test calls `services.shutdown()`, `clients.shutdown()` and then
`core.shutdown()` (lines 106 to 109 of the Jetty integration test above). Each container has a
`shutdown()` method; the order shown there is the order the repository's tests use.
