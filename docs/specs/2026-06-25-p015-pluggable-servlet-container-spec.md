# Spec: P-015 — Pluggable Servlet Container

**Date:** 2026-06-25  
**Status:** Draft — awaiting review

---

## Problem

`appbase-bootstrap` hard-codes Jetty 11. `JettyAppbaseServer`, `AppbaseServletRegistrar`, and `JettyAppbaseServerModule` all live in `appbase-bootstrap` and import `org.eclipse.jetty.*`. Any application using `appbase-bootstrap` gets Jetty whether it wants it or not.

`appbase-service-web` is already clean (zero Jetty imports; pure `jakarta.servlet` + JAX-RS). Only `appbase-bootstrap` needs surgery.

---

## Goal

Make the servlet container pluggable:

- `appbase-bootstrap` depends only on specs (`jakarta.servlet-api`, `jakarta.ws.rs-api`).
- `appbase-bootstrap-jetty` — drop-in Jetty 11 implementation (extracted from current bootstrap).
- `appbase-bootstrap-undertow` — Undertow 2.x implementation (validates the abstraction).
- A skill extension recipe for adding more container integrations.

---

## Current Structure (before)

```
appbase-bootstrap/
  server/
    JettyAppbaseServer.java          ← org.eclipse.jetty.* imports
    AppbaseServletRegistrar.java     ← takes ServletContextHandler (Jetty type)
    JettyAppbaseServerModule.java    ← Guice: binds JettyAppbaseServer
```

`appbase-bootstrap/pom.xml` deps: `jetty-server`, `jetty-servlet`, `jetty-servlets`

---

## Target Structure (after)

```
appbase-bootstrap/                   ← no Jetty deps; defines abstractions
  server/
    AppbaseWebServer.java            ← interface (NEW)
    AppbaseServletDeployment.java    ← interface (NEW): registers servlets/filters
    AppbaseServletRegistrar.java     ← refactored: uses AppbaseServletDeployment

appbase-bootstrap-jetty/            ← new module
  server/
    JettyAppbaseWebServer.java       ← implements AppbaseWebServer (moved + renamed)
    JettyServletDeployment.java      ← implements AppbaseServletDeployment (Jetty side)
    JettyAppbaseServerModule.java    ← moved here

appbase-bootstrap-undertow/         ← new module (validation)
  server/
    UndertowAppbaseWebServer.java    ← implements AppbaseWebServer
    UndertowServletDeployment.java   ← implements AppbaseServletDeployment
    UndertowAppbaseServerModule.java ← Guice binding

appbase-bootstrap-tomcat/           ← new module
  server/
    TomcatAppbaseWebServer.java      ← implements AppbaseWebServer
    TomcatServletDeployment.java     ← implements AppbaseServletDeployment
    TomcatAppbaseServerModule.java   ← Guice binding
```

---

## Interface Design

### `AppbaseWebServer`

```java
package com.viveworks.appbase.bootstrap.server;

public interface AppbaseWebServer {
    void start() throws Exception;
    void stop() throws Exception;
    int getPort();
    boolean isStarted();
}
```

### `AppbaseServletDeployment`

The key abstraction: each container maps servlets and filters differently at startup.
Jetty uses `ServletContextHandler.addServlet()/addFilter()`.
Undertow uses `DeploymentInfo.addServlet()/addFilter()`.

```java
package com.viveworks.appbase.bootstrap.server;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

public interface AppbaseServletDeployment {
    void addServlet(String name, Class<? extends Servlet> servlet, String... urlPatterns);
    void addFilter(String name, Class<? extends Filter> filter, String... urlPatterns);
    void addFilterMapping(String filterName, String... urlPatterns);
}
```

### `AppbaseServletRegistrar` (refactored)

Currently takes `ServletContextHandler` (Jetty-specific). After refactor takes `AppbaseServletDeployment`:

```java
// Before
public void registerServlets(ServletContextHandler context) { … }

// After
public void registerServlets(AppbaseServletDeployment deployment) { … }
```

This is the only source change in existing code. All servlet/filter registration logic stays in `AppbaseServletRegistrar` — only the parameter type changes.

---

## Jetty Implementation (`appbase-bootstrap-jetty`)

`JettyServletDeployment` wraps `ServletContextHandler`:

```java
public final class JettyServletDeployment implements AppbaseServletDeployment {
    private final ServletContextHandler context;
    // addServlet → context.addServlet(new ServletHolder(servlet), urlPattern)
    // addFilter  → context.addFilter(new FilterHolder(filter), urlPattern, EnumSet.of(DispatcherType.REQUEST))
}
```

`JettyAppbaseWebServer` (renamed from `JettyAppbaseServer`) wires Server + JettyServletDeployment + AppbaseServletRegistrar.

**Dependencies** (appbase-bootstrap-jetty/pom.xml):
- `appbase-bootstrap`
- `org.eclipse.jetty:jetty-server:11.0.11`
- `org.eclipse.jetty:jetty-servlet:11.0.11`
- `org.eclipse.jetty:jetty-servlets:11.0.11`

---

## Undertow Implementation (`appbase-bootstrap-undertow`)

Undertow uses `DeploymentInfo` for programmatic registration:

```java
public final class UndertowServletDeployment implements AppbaseServletDeployment {
    private final DeploymentInfo deploymentInfo;
    // addServlet → deploymentInfo.addServlet(Servlets.servlet(name, servlet).addMapping(urlPattern))
    // addFilter  → deploymentInfo.addFilter(Servlets.filter(name, filter))
    //              + deploymentInfo.addFilterUrlMapping(name, urlPattern, DispatcherType.REQUEST)
}
```

`UndertowAppbaseWebServer` boots Undertow's embedded server:

```java
DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
manager.deploy();
Undertow server = Undertow.builder()
    .addHttpListener(port, host)
    .setHandler(manager.start())
    .build();
server.start();
```

**Dependencies** (appbase-bootstrap-undertow/pom.xml):
- `appbase-bootstrap`
- `io.undertow:undertow-servlet:2.3.x` (brings `undertow-core` transitively)
- `jakarta.servlet-api` (provided — Undertow bundles its own, but we want our version)

---

## Tomcat Implementation (`appbase-bootstrap-tomcat`)

Tomcat Embedded uses `Tomcat` + `Context` for programmatic registration:

```java
public final class TomcatServletDeployment implements AppbaseServletDeployment {
    private final Context context;
    // addServlet → Tomcat.addServlet(context, name, servlet); context.addServletMappingDecoded(urlPattern, name)
    // addFilter  → context.addFilterDef(filterDef); context.addFilterMap(filterMap)
}
```

`TomcatAppbaseWebServer` boots embedded Tomcat:

```java
Tomcat tomcat = new Tomcat();
tomcat.setPort(port);
tomcat.getConnector();  // triggers connector creation
Context context = tomcat.addContext("", null);
// ... register servlets/filters via TomcatServletDeployment
tomcat.start();
```

**Dependencies** (appbase-bootstrap-tomcat/pom.xml):
- `appbase-bootstrap`
- `org.apache.tomcat.embed:tomcat-embed-core:10.1.x`
- `org.apache.tomcat.embed:tomcat-embed-jasper:10.1.x` (optional — needed only for JSP)

> **Version note:** Tomcat 10.1.x implements Jakarta EE 10 (Jakarta Servlet 6.0), matching the project's `jakarta.servlet-api:6.0.0`.

---

## JAX-RS with All Containers

Jersey 3.x (used in appbase-service-web) is container-agnostic. `jersey-container-servlet-core` provides `ServletContainer` which works identically on Jetty, Undertow, and Tomcat:

```java
// Same registration pattern for all three containers:
addServlet("jersey", ServletContainer.class, "/api/*");
// with init param: "jakarta.ws.rs.Application" → AppbaseJaxrsApplication
```

No container-specific Jersey glue needed beyond standard servlet registration.

---

## Skill Extension: `add-appbase-container` Recipe

New recipe file: `appbase-java/dev-tools/skills/add-appbase-service/add-appbase-container.md`

Dispatch entry in `SKILL.md`:

```
| Container | add-appbase-container.md | ⚠️ EXPERIMENTAL |
```

The recipe guides:
1. Create `appbase-bootstrap-<container>/` module
2. Implement `AppbaseServletDeployment` for the container's programmatic API
3. Implement `AppbaseWebServer` wrapping the container's server lifecycle
4. Write `<Container>AppbaseServerModule` (Guice binding)
5. POM: parent=appbase-bootstrap-parent, deps: appbase-bootstrap + container lib
6. Write unit + IT tests

---

## Module Placement

```
appbase-java/
  appbase-bootstrap/                  ← existing, trimmed of Jetty
  appbase-bootstrap-jetty/            ← new
  appbase-bootstrap-undertow/         ← new (validation)
  appbase-bootstrap-tomcat/           ← new
```

All three new modules sit alongside `appbase-bootstrap` at the same level. Root `appbase-java/pom.xml` adds them to `<modules>` after `appbase-bootstrap`.

---

## Migration Impact

| Existing code | Change needed |
|---|---|
| `AppbaseServletRegistrar` | Parameter type: `ServletContextHandler` → `AppbaseServletDeployment` |
| `JettyAppbaseServer` | Move + rename to `JettyAppbaseWebServer` in appbase-bootstrap-jetty |
| `JettyAppbaseServerModule` | Move to appbase-bootstrap-jetty |
| Any app using `JettyAppbaseServer` directly | Change import to `appbase-bootstrap-jetty` dep + `JettyAppbaseWebServer` |
| `appbase-bootstrap/pom.xml` | Remove `jetty-server`, `jetty-servlet`, `jetty-servlets` |

`appbase-stacktest` and existing demo apps depend on `appbase-bootstrap` today — they pick up Jetty transitively. After the split they would explicitly depend on `appbase-bootstrap-jetty` (or `appbase-bootstrap-undertow` for the demo).

---

## Validation Plan

1. `appbase-bootstrap` — remove Jetty deps; add `AppbaseWebServer` + `AppbaseServletDeployment` interfaces; refactor `AppbaseServletRegistrar`.
2. `appbase-bootstrap-jetty` — move Jetty classes; add `JettyServletDeployment`; wire with `AppbaseServletRegistrar`.
3. `./build.sh quick` — all existing tests still green (Jetty path untouched from the app's perspective).
4. `appbase-bootstrap-undertow` — implement Undertow server + deployment; write `UndertowAppbaseServerIT` that starts the server, hits `/health`, asserts 200.
5. `appbase-bootstrap-tomcat` — implement Tomcat embedded server + deployment; write `TomcatAppbaseServerIT` that starts the server, hits `/health`, asserts 200.
6. `./build.sh quick` with container IT flags — Undertow + Tomcat ITs pass.

---

## Stacktest Requirements (P-008)

All four new/updated modules must have coverage in `appbase-stacktest` as part of the **P-008** expansion (pre-release gate). These scenarios are in addition to the module-level ITs above.

| Module | Stacktest scenario |
|---|---|
| `appbase-client-redis` | PING, SET key, GET key (assert value), DEL key — using Docker `infra/docker/redis/` scripts |
| `appbase-bootstrap-jetty` | Start embedded Jetty with `StackTestMain` bootstrap, POST to `/health`, assert HTTP 200, stop |
| `appbase-bootstrap-undertow` | Start embedded Undertow with `StackTestMain` bootstrap, POST to `/health`, assert HTTP 200, stop |
| `appbase-bootstrap-tomcat` | Start embedded Tomcat with `StackTestMain` bootstrap, POST to `/health`, assert HTTP 200, stop |

**Container scenarios** (Jetty / Undertow / Tomcat) must also register Jersey's `ServletContainer` to confirm JAX-RS routes work end-to-end on each container — not just the `/health` servlet.

The `StackTestMain` class must accept a `--container` flag (`jetty` | `undertow` | `tomcat`) so CI can exercise all three containers in the same job without code changes.

---

## Risks

| Risk | Mitigation |
|---|---|
| Jersey ServletContainer not working on Undertow | Tested early; Jersey 3.x is servlet-container-agnostic; ServletContainer is the standard bridge |
| Undertow and Jetty servlet registration APIs differ enough to break `AppbaseServletRegistrar` | Abstraction layer (`AppbaseServletDeployment`) isolates this; registrar only uses the interface |
| OSGi Export-Package changes if classes move | Update maven-bundle-plugin Export-Package in each module's POM |

---

## Effort Estimate

| Task | Estimate |
|---|---|
| Define interfaces + refactor `AppbaseServletRegistrar` | XS (<2h) |
| Extract Jetty → `appbase-bootstrap-jetty` | S (2–4h) |
| Implement `appbase-bootstrap-undertow` | S–M (4–8h) |
| Undertow IT test | XS (<1h) |
| Implement `appbase-bootstrap-tomcat` | S (2–4h) |
| Tomcat IT test | XS (<1h) |
| Skill extension recipe | XS (<1h) |
| Stacktest scenarios (Redis + 3 containers + `--container` flag) | S (2–4h) |
| **Total** | **L (3–4d)** |
