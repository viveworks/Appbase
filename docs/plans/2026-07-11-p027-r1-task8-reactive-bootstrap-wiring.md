# P-027 R1 Task 8 — Reactive Bootstrap Wiring & Offload Hardening — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the reactor-netty reactive web face actually run in production — build a fully-wired `ReactorNettyAppbaseWebServer` from `AppbaseServices`, and bound the offloaded reads (`maxPageSize` → 400, per-read timeout → 503) before the routes go live.

**Architecture:** Mirror the servlet-bootstrap `Builder` + registrar pattern. A new `ReactorNettyEndpointRegistrar` populates an `AppbaseRouteRegistry` via the existing `NativeEndpoints.register` / `OffloadedEndpoints.register`; a new `ReactorNettyAppbaseWebServer.Builder.from(AppbaseServices)` assembles config, scheduler, registry, the 7-filter chain (authorization last), `JsonUtil.getJsonMapper()`, MXBeans, and the server. Offload hardening is carried by an immutable `OffloadPolicy`.

**Tech Stack:** Java 25, Guice, Project Reactor / reactor-netty, Jackson (`JsonUtil`), JUnit Jupiter 5.5, Mockito 3.

## Global Constraints

- JDK 25; no FQCNs in code (import instead); every NEW source file gets `@author Navaneeth Mysore G` then `@author Claude`.
- All dependency versions live in the root `pom.xml` dependency management only.
- TDD: failing test first (red) → minimal code (green) → refactor. AAA, one assertion per test where practical.
- Branch: `feature/P-027-R1-task8-bootstrap-wiring` → PR into `feature/P-027-R1-reactive-web`. One atomic PR.
- Config defaults (verbatim): `maxPageSize = 1000`, `offloadTimeoutSeconds = 30`, `enabled = true`.
- Filter order invariant (verbatim): `exceptionHandler → CORS → metric → traceId → userSession → idpTokenCheck → authorization`. Authorization LAST — a security invariant, asserted by test.
- Faithful reproduction: over-cap → `DispatchException(400)`; timeout → `TimeoutException` (mapped to 503 by the existing `ExceptionHandlerWebFilter`, which already has a `TimeoutException → 503` branch — do not re-map here).
- Build gate: `mvn clean install -pl appbase-bootstrap/appbase-bootstrap-reactor-netty -P develop-profile` (compile + tests + jacoco 90%). The full-reactor `site` goal fails repo-wide on JDK 25 (spotbugs/ASM, tracked internal work item 259) — do not use `site` for verification.

---

## Reference signatures (confirmed against the codebase — do not guess)

```java
// ReactorNettyAppbaseWebServer (existing constructor — unchanged):
public ReactorNettyAppbaseWebServer(
        ReactorNettyAppbaseWebServerConfiguration configuration,
        AppbaseRouteRegistry routeRegistry,
        List<AppbaseServiceFilter> filters,
        ObjectMapper objectMapper)

// NativeEndpoints (existing):
public static void register(AppbaseRouteRegistry registry, AppbaseCore appbaseCore,
        HealthCheckService healthCheckService)

// OffloadedEndpoints (existing — extended in Task 2):
public static void register(AppbaseRouteRegistry registry, AppbaseCore appbaseCore,
        Scheduler offloadScheduler)   // → add OffloadPolicy parameter

// AppbaseServices (existing): getCoreServices() → AppbaseCoreServices → appbaseCore()
// Reactive filters (existing, Task 3): ExceptionHandlerWebFilter, CorsWebFilter, MetricWebFilter,
//   TraceIdWebFilter (appbase-service-web/reactive); UserSessionWebFilter, IdpUserTokenCheckWebFilter
//   (appbase-service-user/.../reactive); AuthorizationWebFilter (appbase-service-web/reactive).
// JsonUtil.getJsonMapper() → the configured singleton ObjectMapper (appbase-core).
```

## File structure

| File | Responsibility |
|---|---|
| `.../reactornetty/endpoint/OffloadPolicy.java` (new) | Immutable (`maxPageSize`, `timeout`) with validation. |
| `.../reactornetty/endpoint/OffloadedEndpoints.java` (modify) | `register(...)` takes `OffloadPolicy`; page-size guard; `.timeout(...)`. |
| `.../reactornetty/configuration/ReactorNettyAppbaseWebServerConfiguration.java` (modify) | Add `maxPageSize`, `offloadTimeoutSeconds` fields + builder + defaults. |
| `.../reactornetty/bootstrap/ReactorNettyEndpointRegistrar.java` (new) | Populate an `AppbaseRouteRegistry` with the ten routes; exclude hooks. |
| `.../reactornetty/server/ReactorNettyAppbaseWebServer.java` (modify) | Add `Builder.from(AppbaseServices)`. |
| Tests (new/modified) | Per task below. |

---

## Task 1: `OffloadPolicy` value object + config fields

**Files:**
- Create: `.../reactornetty/endpoint/OffloadPolicy.java`
- Modify: `.../reactornetty/configuration/ReactorNettyAppbaseWebServerConfiguration.java`
- Test: `.../reactornetty/endpoint/OffloadPolicyTest.java`; extend the existing configuration test.

**Interfaces:**
- Produces: `OffloadPolicy(int maxPageSize, Duration timeout)`; accessors `int maxPageSize()`, `Duration timeout()`; static `OffloadPolicy of(int maxPageSize, Duration timeout)` rejecting `maxPageSize < 1` / null-or-non-positive `timeout` with `IllegalArgumentException`. Config adds `int getMaxPageSize()` (default 1000) and `int getOffloadTimeoutSeconds()` (default 30) + builder setters.

- [ ] **Step 1: Write the failing `OffloadPolicy` test** — construction stores the pair; `maxPageSize < 1` and non-positive/`null` timeout throw `IllegalArgumentException`; `equals`/`hashCode` by value.
- [ ] **Step 2: Run it, verify it fails** (`cannot find symbol: class OffloadPolicy`).
  Run: `mvn -pl appbase-bootstrap/appbase-bootstrap-reactor-netty test -Dtest=OffloadPolicyTest -P quick-profile`
- [ ] **Step 3: Implement `OffloadPolicy`** — final class, two final fields, validating `of(...)`/constructor, `@author` tags. Stub (signatures + validation intent):

```java
public final class OffloadPolicy {
    private final int maxPageSize;
    private final Duration timeout;
    // of(int, Duration) validates (maxPageSize >= 1, timeout > 0) then constructs; accessors; equals/hashCode.
}
```

- [ ] **Step 4: Run `OffloadPolicyTest`, verify PASS.**
- [ ] **Step 5: Add config fields** — `maxPageSize` (default 1000) and `offloadTimeoutSeconds` (default 30) to `ReactorNettyAppbaseWebServerConfiguration` + its `Builder` (mirror the existing `enabled`/`host` field+builder+equals/hashCode/toString shape). Extend the configuration test for the two new defaults.
- [ ] **Step 6: Run the configuration test, verify PASS.**
- [ ] **Step 7: Commit** — `feat(P-027-R1): Task 8 — OffloadPolicy value object + maxPageSize/offloadTimeout config`.

---

## Task 2: page-size guard + per-read timeout in `OffloadedEndpoints`

**Files:**
- Modify: `.../reactornetty/endpoint/OffloadedEndpoints.java`
- Test: `.../reactornetty/endpoint/OffloadedEndpointsTest.java` (extend)

**Interfaces:**
- Consumes: `OffloadPolicy` (Task 1).
- Produces: `register(AppbaseRouteRegistry, AppbaseCore, Scheduler, OffloadPolicy)` — the `Scheduler` overload gains a trailing `OffloadPolicy`. A private `requireBoundedInt(exchange, name, maxPageSize)` returning an `int` in `[1, maxPageSize]` or throwing `DispatchException(400)`. The `offloaded(...)` wrapper applies `.timeout(policy.timeout())`.

- [ ] **Step 1: Write the failing tests** — (a) `/log?operation=logRecordsAt&…&pageSize=<maxPageSize+1>` → 400 **and the MXBean accessor is never called** (Mockito `verify(..., never())`); (b) `pageSize=0` → 400; (c) an offloaded handler whose accessor sleeps past a short test `OffloadPolicy` timeout → the `Mono` errors with `TimeoutException` (assert via `assertThrows` on `.block()`, cause chain contains `TimeoutException`). Update every existing `OffloadedEndpoints.register(...)` call in the test to pass a permissive `OffloadPolicy.of(1000, Duration.ofSeconds(30))`.
- [ ] **Step 2: Run, verify the new tests fail** (compile error on the 4-arg `register`).
  Run: `mvn -pl appbase-bootstrap/appbase-bootstrap-reactor-netty test -Dtest=OffloadedEndpointsTest -P quick-profile`
- [ ] **Step 3: Implement** — thread `OffloadPolicy` through `register(...)` into the per-endpoint dispatch closures and the `offloaded(...)` wrapper. Replace the raw `Integer.parseInt(requireParam(exchange,"pageSize"))` in `dispatchLog` with `requireBoundedInt(exchange, "pageSize", policy.maxPageSize())`. Add `.timeout(policy.timeout())` to the `Mono` chain in `offloaded(...)` (before `.flatMap(exchange::writeJson)`). Do NOT catch/re-map the `TimeoutException` — it propagates to `ExceptionHandlerWebFilter` → 503. Stub:

```java
// requireBoundedInt: parse the required param; if value < 1 or > maxPageSize -> DispatchException(400).
// offloaded(scheduler, appbaseCore, dispatch, policy): ... .subscribeOn(scheduler).timeout(policy.timeout()).flatMap(exchange::writeJson)
```

- [ ] **Step 4: Run `OffloadedEndpointsTest`, verify all PASS** (new + existing green).
- [ ] **Step 5: Commit** — `feat(P-027-R1): Task 8 — bound pageSize (400) + per-read offload timeout (503)`.

---

## Task 3: `ReactorNettyEndpointRegistrar`

**Files:**
- Create: `.../reactornetty/bootstrap/ReactorNettyEndpointRegistrar.java`
- Test: `.../reactornetty/bootstrap/ReactorNettyEndpointRegistrarTest.java`

**Interfaces:**
- Consumes: `NativeEndpoints.register(registry, appbaseCore, healthCheckService)`, `OffloadedEndpoints.register(registry, appbaseCore, offloadScheduler, policy)` (Task 2).
- Produces: `static AppbaseRouteRegistry buildRegistry(AppbaseCore appbaseCore, HealthCheckService healthCheckService, Scheduler offloadScheduler, OffloadPolicy policy, Set<String> excludedPaths)` — a new registry populated with the ten routes minus any in `excludedPaths`.

- [ ] **Step 1: Write the failing test** — `buildRegistry(...)` with a mocked core/health/scheduler and empty excludes → `lookup("GET", p)` non-null for all ten paths (`/health`, `/metric`, `/serviceRegistry`, `/status`, `/log`, `/exception`, `/configuration`, `/core`, `/jmx`, `/mediator`); with `excludedPaths=Set.of("/log")` → `/log` is null, the other nine non-null.
- [ ] **Step 2: Run, verify FAIL** (`cannot find symbol: ReactorNettyEndpointRegistrar`).
- [ ] **Step 3: Implement** — a final class with a private constructor and the static `buildRegistry`. It creates an `AppbaseRouteRegistry`, calls the two `register(...)` methods, and (if `excludedPaths` non-empty) skips those routes. Simplest faithful approach: register all, then the registry exposes/relies on a filtered view — OR pass excludes down. Choose registering into a fresh registry and, for excludes, guard each `route(...)` — but since `register(...)` is monolithic, the pragmatic form is: register all into the registry, then the excludes are applied by the Builder omitting them (document the chosen mechanism at the code site). `@author` tags.

```java
public final class ReactorNettyEndpointRegistrar {
    private ReactorNettyEndpointRegistrar() {}
    public static AppbaseRouteRegistry buildRegistry(AppbaseCore appbaseCore,
            HealthCheckService healthCheckService, Scheduler offloadScheduler,
            OffloadPolicy policy, Set<String> excludedPaths) { /* ... */ }
}
```

- [ ] **Step 4: Run the registrar test, verify PASS.**
- [ ] **Step 5: Commit** — `feat(P-027-R1): Task 8 — ReactorNettyEndpointRegistrar populates the ten routes`.

---

## Task 4: `ReactorNettyAppbaseWebServer.Builder.from(AppbaseServices)`

**Files:**
- Modify: `.../reactornetty/server/ReactorNettyAppbaseWebServer.java`
- Test: `.../reactornetty/server/ReactorNettyAppbaseWebServerBuilderTest.java`

**Interfaces:**
- Consumes: `ReactorNettyEndpointRegistrar.buildRegistry(...)` (Task 3), the reactive filters, `JsonUtil.getJsonMapper()`, `AppbaseServices.getCoreServices().appbaseCore()`.
- Produces: `static Builder builder()`; `Builder from(AppbaseServices services)`; `ReactorNettyAppbaseWebServer build()` — returns a server whose registry has the ten routes, whose `objectMapper` is `JsonUtil.getJsonMapper()`, and whose filter list is in the invariant order.

- [ ] **Step 1: Write the failing test** — build via `ReactorNettyAppbaseWebServer.builder().from(testServices).build()` (mocked/stub `AppbaseServices` exposing a core + health service + the filters + scheduler config). Assert: (a) the server exposes the ten routes (drive each through a `FakeExchange`/lookup, or expose a package-private `routeRegistry()` accessor for the test); (b) `server.objectMapper() == JsonUtil.getJsonMapper()` (same instance — add a package-private accessor if needed); (c) the filter list types are exactly `[ExceptionHandlerWebFilter, CorsWebFilter, MetricWebFilter, TraceIdWebFilter, UserSessionWebFilter, IdpUserTokenCheckWebFilter, AuthorizationWebFilter]` in that order (authorization last).
- [ ] **Step 2: Run, verify FAIL** (`cannot find symbol: method builder()`).
- [ ] **Step 3: Implement the Builder** — mirror `JettyAppbaseWebServer.Builder`: `from(AppbaseServices)` captures the services; `build()` resolves the config (`ReactorNettyAppbaseWebServerConfiguration`, incl. `maxPageSize`/`offloadTimeoutSeconds` → an `OffloadPolicy`), resolves the offload `Scheduler` from `offloadSchedulerId` (default a bounded scheduler), builds the registry via the registrar, assembles the filter chain in the invariant order from the service layer, passes `JsonUtil.getJsonMapper()`, registers the standard MXBeans on `appbaseCore` (idempotent, mirroring the Jetty builder's `registerMXBeans`), and constructs the server. Add package-private `routeRegistry()` / `objectMapper()` accessors if the assertions need them. `@author` tag on any new nested type. Stub:

```java
public static Builder builder() { return new Builder(); }
public static final class Builder {
    private AppbaseServices services;
    public Builder from(AppbaseServices services) { this.services = services; return this; }
    public ReactorNettyAppbaseWebServer build() { /* resolve config+scheduler+policy; registrar; filters; JsonUtil mapper; MXBeans; new ReactorNettyAppbaseWebServer(...) */ }
}
```

- [ ] **Step 4: Run the builder test, verify PASS.**
- [ ] **Step 5: Run the whole module** — `mvn clean install -pl appbase-bootstrap/appbase-bootstrap-reactor-netty -P develop-profile`. Expected: BUILD SUCCESS, all tests green, jacoco 90% held.
- [ ] **Step 6: Commit** — `feat(P-027-R1): Task 8 — ReactorNettyAppbaseWebServer.Builder.from(AppbaseServices) wires the live face`.

---

## Task 5: Security & defect audit, code-review, PR

Not an implementation task — the per-PR gate. The routes are now live, so the audit re-focuses on that.

- [ ] **5-lens audit** (heavy on the new wiring + hardening). Specifically re-verify, now that routes serve traffic: the assembled filter chain **contains an `AuthorizationWebFilter` and it is last** (the one control gating the admin reads); no secret-exposing endpoint (`/configuration`, `/log`, …) is reachable without it; the `maxPageSize` guard runs before the accessor; the timeout bounds worker-hold time. Fix every confirmed finding with a regression test.
- [ ] **`code-review`** the branch diff; fix/push findings or leave as-is with reason.
- [ ] **Append to `docs/Quality-Report.md`** — a Defects-caught row for each of the two DoS fixes (pageSize bound, timeout) with fix commit + regression test, and the audit outcome. Log any near-miss.
- [ ] **Build green** under `develop-profile`; then **PR** into `feature/P-027-R1-reactive-web` (create the Task 8 sub-issue first; post the PR link to it).

---

## Self-review (author checklist — completed)

- **Spec coverage:** §3.1 Registrar → Task 3; §3.2 Builder + mapper==JsonUtil → Task 4; §3.3 pageSize+timeout → Task 2; §3.4 config → Task 1; §4 tests → Tasks 1–4 + Task 5 audit; §5 security → Task 5. All covered.
- **Placeholders:** none — every step names files, commands, and the stub/signature it produces (full bodies deferred to PR review per the project's plan-stub convention).
- **Type consistency:** `OffloadPolicy.of(int, Duration)` / `maxPageSize()` / `timeout()` used identically in Tasks 1–4; `buildRegistry(...)` signature identical in Tasks 3–4; `register(..., OffloadPolicy)` 4-arg form consistent across Tasks 2–3.
