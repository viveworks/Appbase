# P-027 R1 Task 9 — Secure-by-Default Authorization — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make an unauthenticated request to a non-public endpoint return **403** on BOTH the servlet and reactive faces by default, with `/health` still public — closing Task 8 audit finding D1.

**Architecture:** The `ContextAuthorizer` is set once on `AppbaseServices.Builder` and flows through `DefaultAuthorizationServiceFactory` → `appbaseCore` → both faces' filters. A new `SecureByDefaultContextAuthorizer` (permit public paths, deny the rest) replaces the default `PermitAllContextAuthorizer` when `secureByDefault` is on, and the builder registers a FORBIDDEN(403) `AuthorizationServiceExceptionConfiguration` so the existing filter machinery emits 403 (no filter code change). Both faces inherit by consuming the same `AppbaseServices`.

**Tech Stack:** Java 25, Guice, JUnit Jupiter 5.5, Mockito 3; reactor-netty + servlet bootstraps for the end-to-end tests.

## Global Constraints

- JDK 25; **no FQCNs** (import); every NEW source file carries `@author Navaneeth Mysore G` then `@author Claude`.
- All dependency versions in the root `pom.xml` only.
- TDD: failing test first (red) → minimal code (green) → refactor. AAA.
- Branch: `feature/P-027-R1-task9-secure-by-default` → PR into `feature/P-027-R1-reactive-web`. One atomic PR.
- Defaults (verbatim): `secureByDefault = true`; `publicPaths = {"/health"}`.
- **The cross-face invariant:** the posture is configured in ONE place (`AppbaseServices.Builder`); both faces inherit. No per-web-builder setter.
- **An explicit `AppbaseServices.Builder.withContextAuthorizer(...)` always wins** (operator override) over the secure-by-default selection.
- Build gate: `mvn clean install -pl <module> -P develop-profile` (jacoco 90%). Do NOT use the `site` goal (JDK-25 spotbugs breakage, tracked internal work item 259).

## Reference signatures (confirmed — do not guess)

```java
// ContextAuthorizer (appbase-service-core/.../authorization) — 4 methods:
boolean isAuthorizedContext(String service, AuthorizationContext context);
boolean isAuthorizedContext(String service, String operation, AuthorizationContext context);
boolean isAuthorizedContext(Class<?> serviceType, AuthorizationContext context);
boolean isAuthorizedContext(Class<?> serviceType, String operation, AuthorizationContext context);
// The web filters call the String-service variant with service = request path, operation = HTTP method.

// AuthorizationServiceExceptionConfiguration (implements AppbaseConfiguration):
//   builder().build() already defaults respondForbidden=true → a 403 config.
//   AuthorizationServiceExceptionConfiguration.builder().respondForbidden(true).build();

// ConfigurationService (appbase-core):
<T extends AppbaseConfiguration> void putTypedConfiguration(Class<T> typeKey, T value);   // register instance
<T extends AppbaseConfiguration> T getTypedConfiguration(Class<T> typeKey);               // filter reads (null → pass through)

// AppbaseServices.Builder (appbase-bootstrap-core):
//   existing: withContextAuthorizer(ContextAuthorizer) — default value is PermitAllContextAuthorizer.
// The web filter denies 403 when !isAuthorized AND an AuthorizationServiceExceptionConfiguration
//   with respondForbidden is registered; otherwise it passes through.
```

## File structure

| File | Responsibility |
|---|---|
| `.../service/authorization/SecureByDefaultContextAuthorizer.java` (new, appbase-service-core) | `ContextAuthorizer` that permits `publicPaths`, denies the rest. |
| `.../bootstrap/service/AppbaseServices.java` (modify, appbase-bootstrap-core) | `Builder.secureByDefault(boolean)` + `publicPaths(Set<String>)`; select the authorizer + register the 403 config. |
| Tests (new) | Per task. |

---

## Task 1: `SecureByDefaultContextAuthorizer`

**Files:**
- Create: `appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/authorization/SecureByDefaultContextAuthorizer.java`
- Test: `.../src/test/java/com/viveworks/appbase/service/authorization/SecureByDefaultContextAuthorizerTest.java`

**Interfaces:**
- Produces: `SecureByDefaultContextAuthorizer(Set<String> publicPaths)` implementing all four `ContextAuthorizer.isAuthorizedContext(...)` methods. String-service variants: return `true` iff `service` (the request path) is in `publicPaths`, else `false`. Class-service variants: return `false` (no path context → deny). Stores an unmodifiable copy of `publicPaths`; rejects null.

- [ ] **Step 1: Write the failing test** — a public path permits; a non-public path denies; the `(service, operation, ctx)` variant matches the same rule; the `Class<?>` variants deny; null `publicPaths` → `IllegalArgumentException`; an empty `publicPaths` denies everything (String variants). Use a plain `AuthorizationContext` (mock or a simple instance — check its constructor).
- [ ] **Step 2: Run it, verify FAIL** (`cannot find symbol: class SecureByDefaultContextAuthorizer`).
  Run: `cd appbase-java && mvn -pl appbase-service/appbase-service-core test -Dtest=SecureByDefaultContextAuthorizerTest -P quick-profile`
- [ ] **Step 3: Implement** — final class implementing `ContextAuthorizer`; constructor stores `Set.copyOf(publicPaths)`; String variants `return publicPaths.contains(service);` Class variants `return false;`. `@author` tags. Stub:

```java
public final class SecureByDefaultContextAuthorizer implements ContextAuthorizer {
    private final Set<String> publicPaths;
    public SecureByDefaultContextAuthorizer(final Set<String> publicPaths) { /* notNull + Set.copyOf */ }
    // String variants: return publicPaths.contains(service);  Class variants: return false;
}
```

- [ ] **Step 4: Run the test, verify PASS.**
- [ ] **Step 5: Commit** — `feat(P-027-R1): Task 9 — SecureByDefaultContextAuthorizer (permit public paths, deny rest)`.

---

## Task 2: wire it into `AppbaseServices.Builder`

**Files:**
- Modify: `appbase-java/appbase-bootstrap/appbase-bootstrap-core/src/main/java/com/viveworks/appbase/bootstrap/service/AppbaseServices.java`
- Test: the existing `AppbaseServices`/builder test (extend), or a new `AppbaseServicesSecureByDefaultTest.java`.

**Interfaces:**
- Consumes: `SecureByDefaultContextAuthorizer` (Task 1); `ConfigurationService.putTypedConfiguration(...)`; `AuthorizationServiceExceptionConfiguration.builder()`.
- Produces: `Builder.secureByDefault(boolean)` (default `true`) and `Builder.publicPaths(Set<String>)` (default `{"/health"}`). When `secureByDefault` is true and no explicit `withContextAuthorizer(...)` was called, the effective authorizer is `new SecureByDefaultContextAuthorizer(publicPaths)`; and the built services register a FORBIDDEN `AuthorizationServiceExceptionConfiguration` via `putTypedConfiguration` so the filters emit 403. Explicit `withContextAuthorizer(...)` overrides the authorizer selection.

- [ ] **Step 1: Write the failing test** — (a) default builder (`secureByDefault` untouched) yields an `AppbaseServices` whose effective `ContextAuthorizer` is a `SecureByDefaultContextAuthorizer`; (b) `secureByDefault(false)` yields `PermitAllContextAuthorizer`; (c) an explicit `withContextAuthorizer(custom)` wins regardless of `secureByDefault`; (d) after build, `configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class)` is non-null and `isRespondForbidden()` is true when secure-by-default is on. Read `AppbaseServices`/`AppbaseCoreServices` to find how to reach the effective authorizer and the `ConfigurationService` for the assertion (add a package-private accessor if needed).
- [ ] **Step 2: Run, verify FAIL** (`cannot find symbol: method secureByDefault`).
- [ ] **Step 3: Implement** — add the two builder fields (defaults `true` / `Set.of("/health")`) + setters; in `build()` (or where the authorizer is resolved), if no explicit authorizer and `secureByDefault`, use `new SecureByDefaultContextAuthorizer(publicPaths)`; register the 403 config via `putTypedConfiguration` at the point the `ConfigurationService` is available (verify ordering — the ConfigurationService must be initialized before the put; if the build assembles services in stages, register right after the config service is ready). Mirror the existing builder-field style (`withContextAuthorizer`). Document the decision at the code site. Stub:

```java
private boolean secureByDefault = true;
private Set<String> publicPaths = Set.of("/health");
public Builder secureByDefault(final boolean v) { this.secureByDefault = v; return this; }
public Builder publicPaths(final Set<String> v) { this.publicPaths = Set.copyOf(v); return this; }
// resolve authorizer: explicit-wins; else secureByDefault ? new SecureByDefaultContextAuthorizer(publicPaths) : new PermitAllContextAuthorizer();
// after ConfigurationService available: putTypedConfiguration(AuthorizationServiceExceptionConfiguration.class, forbidden) when secureByDefault.
```

- [ ] **Step 4: Run the test, verify PASS.**
- [ ] **Step 5: Commit** — `feat(P-027-R1): Task 9 — AppbaseServices.Builder secureByDefault + publicPaths (default deny, /health public)`.

---

## Task 3: end-to-end deny + servlet-parity tests

**Files:**
- Test (new, reactor-netty): `.../reactornetty/server/ReactorNettyAppbaseWebServerSecureByDefaultTest.java`
- Test (new, one servlet bootstrap — jetty): `appbase-bootstrap-jetty/.../SecureByDefaultParityTest.java` (or extend an existing jetty integration test)

**Interfaces:**
- Consumes: the Task 2 `AppbaseServices.Builder.secureByDefault/publicPaths`; the Task 8 `ReactorNettyAppbaseWebServer.Builder.from(AppbaseServices)`; the jetty builder's `from(AppbaseServices)`.

- [ ] **Step 1: Write the failing reactive e2e test** — build an `AppbaseServices` (secure-by-default, real or suitably-stubbed core) → `ReactorNettyAppbaseWebServer.builder().from(services).build()`, start on an ephemeral port (mirror `ReactorNettyAppbaseWebServerHttpTest`), drive real HTTP: `GET /configuration?operation=securePropertyKeys` → **403**; `GET /health` → **200**. Then a second server with `secureByDefault(false)` → `/configuration` is NOT 403 (passes through to the handler). This is the end-to-end deny the Task 8 audit (D5) said was missing.
- [ ] **Step 2: Run, verify FAIL** (secure path not yet denying, or method missing).
- [ ] **Step 3: Make green** — no new production code should be needed if Tasks 1–2 are correct; this task proves them end-to-end. If the test reveals the 403 doesn't fire (e.g. the config wasn't registered in time), fix the Task 2 wiring and note it.
- [ ] **Step 4: Write the servlet-parity test** — the SAME secure-by-default `AppbaseServices` stood up on the jetty face → `GET /configuration` **403**, `GET /health` **200**. Proves both faces behave identically from one shared setting.
- [ ] **Step 5: Run the whole affected modules** — `mvn clean install -pl appbase-service/appbase-service-core,appbase-bootstrap/appbase-bootstrap-core,appbase-bootstrap/appbase-bootstrap-reactor-netty,appbase-bootstrap/appbase-bootstrap-jetty -P develop-profile`. Expected: BUILD SUCCESS, green, jacoco held.
- [ ] **Step 6: Commit** — `test(P-027-R1): Task 9 — end-to-end secure-by-default deny + servlet-parity`.

---

## Task 4: audit, code-review, Quality-Report, PR

Controller-run gate (not an implementation task).

- [ ] **5-lens audit** heavy on the new authorizer + wiring. Headline: on a default-built server of EACH face, a secret-exposing admin read returns 403 and `/health` returns 200 — asserted end-to-end. Also verify: the `Class<?>` deny-by-default doesn't break internal programmatic authorization paths that legitimately relied on permit-all (check callers of the `Class<?>` `isAuthorizedContext` variants); explicit `withContextAuthorizer` override still works. Fix confirmed findings with regression tests.
- [ ] **`code-review`** the branch diff; fix/push or leave with reason.
- [ ] **Append to `docs/Quality-Report.md`** — the D1 close as a Defects-caught row (finding → fix commit → the end-to-end regression test), plus the audit outcome and any near-miss.
- [ ] **Build green** under `develop-profile`; then PR into `feature/P-027-R1-reactive-web` (create the Task 9 sub-issue first; post the PR link).

---

## Self-review (author checklist — completed)

- **Spec coverage:** §3.1 authorizer → Task 1; §3.2 forbidden config + §3.3 single-setter wiring → Task 2; §4 tests (unit, e2e deny, servlet parity, config override) → Tasks 1–3; §5 security/D1 close → Task 4. Covered.
- **Placeholders:** none — files, commands, and stubs/signatures are concrete; full bodies deferred to PR review per the project's plan-stub convention. The one genuine plan-time discovery (exact point to call `putTypedConfiguration` relative to ConfigurationService init) is called out explicitly in Task 2 Step 3, not hidden.
- **Type consistency:** `SecureByDefaultContextAuthorizer(Set<String>)` used identically in Tasks 1–2; `secureByDefault(boolean)`/`publicPaths(Set<String>)` consistent across Tasks 2–3; the 403 config type `AuthorizationServiceExceptionConfiguration` consistent Tasks 2 & 4.
