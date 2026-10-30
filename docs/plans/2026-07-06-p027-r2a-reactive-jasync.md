# P-027 R2a — Reactive Jasync Twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reactive + synchronous **service-acquired** accessor faces to the `appbase-client-jasync` async-SQL client service (jasync-sql for MySQL/MariaDB + PostgreSQL), on the shared Appbase accessor spine, bridging jasync's `CompletableFuture` API to Reactor and exposing jasync's connection-pool counters to the Micrometer composite.

**Architecture:** In-module (`appbase-client-jasync`). This is a **greenfield** twin — the module has NO existing accessor or exception class. jasync is **`CompletableFuture`-based, not Reactor-native**: `Connection.sendQuery/sendPreparedStatement/inTransaction` all return `CompletableFuture<QueryResult>`. The reactive face wraps each call in `Mono.fromFuture(supplier)` inside the full `mono`/`flux` spine — so there is NO eager driver-client build, NO per-key cache, and NO non-owning-wrapper hazard (unlike Aerospike). Both faces use a **lazy borrow-`Supplier`** of the integration service (like Cassandra). jasync exposes no driver-native metrics registry, so "driver metrics" is a lightweight bridge of `ConnectionPool` size counters (in-use/idle/waiting) to Micrometer gauges, config-gated.

**Tech Stack:** Java 25, Project Reactor 3.6.8, jasync-sql 1.2.3 (`jasync-mysql` + `jasync-postgresql`), Micrometer (`MetricService` composite → JMX), Guice, Maven, JUnit 5 + Mockito + `reactor-test` StepVerifier.

**Reference twins (mirror these exactly):** merged Cassandra (SQL-closest — `appbase-client-cassandra/.../reactive/ReactiveCassandraAccessor.java`, `CassandraAccessor.java`, `CassandraAccessorException.java`, `DefaultCassandraClientService.getReactiveAccessor`/`getAccessor`, `DefaultCassandraClientServiceConfiguration` reactive-config field) and Kafka (the `CompletableFuture`→unwrap pattern in the sync producer `send().get()`; `driverMetricsEnabled` gate).

## Global Constraints

- **Branch/PR (max 3 tasks per PR, per request):** twin branch `feature/P-027-R2a-jasync` created off the reactive epic (`feature/P-027-reactive-appbase`) HEAD. **PR 1** = Tasks 1–3 on branch `feature/P-027-R2a-jasync-t1t3-reactive`; **PR 2** = Tasks 4–6 on branch `feature/P-027-R2a-jasync-t4t6-sync-metrics-it`. Both target the twin branch. When both merge, roll up `feature/P-027-R2a-jasync` → the reactive epic (closes the twin sub-issue manually — GitHub auto-close only fires on merge to `develop`). There is NO driver-bump prerequisite (jasync is already a dependency), so nothing merges flat into the epic first. Post each PR link to the twin sub-issue in sequence.
- **Every Java file:** Apache license header (copy verbatim from a sibling in the module), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`. No fully-qualified class names in code (use imports) — sole exception: class-name string constants.
- **Decisions documented in code:** every non-obvious choice gets a Javadoc/inline rationale at the code site (the `CompletableFuture`→`Mono.fromFuture` bridge, the `CompletionException`/`ExecutionException` unwrap, why there is no eager build/cache, the pool-gauge polling model).
- **Dependency versions ONLY in root `pom.xml` `<dependencyManagement>`**; module poms are version-less. (`reactor`/`reactor-test` are already managed in root from R0 — the module pom just adds version-less entries.)
- **Build command** (module + deps; use `install`, NOT `test` — bnd Export-Package on `.reactive` fails under `test`):
  `cd /Users/nav/Viveworks/Technology/Appbase/appbase-java && mvn -q -pl appbase-client/appbase-client-jasync -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`
  `-Dsurefire.failIfNoSpecifiedTests=false` is MANDATORY (else `-am` aborts at appbase-core). Scope a single class with `-Dtest=<Class>` (still `install`).
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.** (regenerated; never commit).
- **Key/value types:** the jasync client service is `AppbaseClientService<Connection, TextValue>` — the accessor key is `AppbaseClientKey<TextValue>` (NOT `ClusterIdValue`). Mirror Cassandra's acquisition but substitute `TextValue`.
- **Gated tests → J-018.** **Learnings → `appbase-java/LEARNING.md`** at the end.

## Decisions (for review)

- **A — `Mono.fromFuture` bridge, no eager build/cache.** jasync's `Connection` (runtime `com.github.jasync.sql.db.pool.ConnectionPool`) is future-based. The reactive accessor calls `mono(op, () -> Mono.fromFuture(() -> client.sendPreparedStatement(sql, params)))`. Because the wrap is lazy per subscription, the reactive accessor takes the SAME lazy `Supplier<integration-service>` borrow as the sync one — no cached reactor client, no shutdown/never-close complexity.
- **B — Two service-acquired accessors** on the shared spine: `ReactiveJasyncAccessor` (`getReactiveAccessor()`) + sync `JasyncAccessor` (`getAccessor()`), plus keyed variants. New `JasyncAccessorException`.
- **C — Error unwrap.** A `CompletableFuture` failure wraps the driver exception: `Mono.fromFuture` surfaces (Reactor-unwrapped) the raw `DatabaseException`, while the sync `.get()` throws `ExecutionException(cause=DatabaseException)`. `JasyncAccessorException.map` unwraps BOTH `CompletionException` and `ExecutionException` to their cause, then maps `com.github.jasync.sql.db.exceptions.DatabaseException` → `JasyncAccessorException`, else fallback. Sync `.get()` also restores the interrupt flag on `InterruptedException` (like Kafka).
- **D — Pool-counter metrics (no driver registry).** jasync has no metrics API; `ConnectionPool` exposes `getInUseConnectionsCount()`/`getIdleConnectionsCount()`/`getFuturesWaitingForConnectionCount()`. Bridge these to Micrometer **gauges bound to a function of the live pool** (Micrometer polls them) on the composite (→ JMX), config-gated by `driverMetricsEnabled` (default true). The composite is `MetricService`-owned — never shut down. Documented as differing from Kafka/Aerospike (which had real driver metrics).
- **E — Greenfield.** No existing accessor/exception to preserve.
- **F — Grouping.** 6 tasks in 2 PRs of 3 (per the max-3-per-PR request).

---

## File Structure

**Jasync module (`.../service/jasync/`):**
- Modify `configuration/JasyncClientServiceConfiguration.java` (interface) + `configuration/DefaultJasyncClientServiceConfiguration.java` — `reactiveOperationConfiguration`.
- Modify `configuration/JasyncClientIntegrationServiceConfiguration.java` (interface) + `configuration/DefaultJasyncClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled`.
- Create `JasyncAccessorException.java` (top package).
- Create `reactive/ReactiveJasyncAccessor.java`.
- Create `JasyncAccessor.java` (sync, top package).
- Modify `JasyncClientService.java` (interface) + `DefaultJasyncClientService.java` — `getReactiveAccessor()`/`getAccessor()` (+ keyed).
- Modify `JasyncClientIntegrationService.java` (interface) + `DefaultJasyncClientIntegrationService.java` — `isDriverMetricsEnabled()`; pool-gauge binding in `initImpl` (config-gated).
- Module `pom.xml` — add version-less `reactor-core` (if not transitively present for the module), `reactor-test` (test); Export-Package `...service.jasync.reactive`.

**Tests/IT + docs:** unit tests per accessor + acquisition + exception map + pool-metrics + config; gated `ReactiveJasyncAccessorIT`; `build-guide.md` "Reactive Jasync" section; J-018 entry.

---

## PR 1 — branch `feature/P-027-R2a-jasync-t1t3-reactive` (Tasks 1–3)

### Task 1: `reactiveOperationConfiguration` on the Jasync client-service config

**Files:**
- Modify: `configuration/JasyncClientServiceConfiguration.java` (interface getter) + `configuration/DefaultJasyncClientServiceConfiguration.java` (field/Builder/getter/equals/hashCode/toString).
- Test: `configuration/DefaultJasyncClientServiceConfigurationTest.java`.

**Interfaces:**
- Produces: `ReactiveOperationConfiguration getReactiveOperationConfiguration()` returning a non-null concrete `DefaultReactiveOperationConfiguration` (default `DefaultReactiveOperationConfiguration.builder().build()`), mirroring `DefaultCassandraClientServiceConfiguration` exactly.

- [ ] **Step 1: Failing test** — assert the getter defaults non-null with `metricsEnabled()==true`, and a Jackson YAML round-trip preserves an overridden `metricsEnabled(false)`. Mirror `DefaultCassandraClientServiceConfigurationTest`.
- [ ] **Step 2: Run — FAIL** (`-Dtest=DefaultJasyncClientServiceConfigurationTest`).
- [ ] **Step 3: Add the field** — mirror `DefaultCassandraClientServiceConfiguration` (field, `DEFAULT_REACTIVE_OPERATION_CONFIGURATION`, ctor-from-builder, interface + impl getter, Builder setter defaulting to the constant, equals/hashCode/toString). **Confirm `Builder.getThis()` returns `this`.**
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression.**
- [ ] **Step 6: Commit** (`feat(P-027-R2a): reactive operation config on Jasync client service config`).

### Task 2: `JasyncAccessorException` + `ReactiveJasyncAccessor` (Mono.fromFuture full spine)

**Files:**
- Create: `JasyncAccessorException.java`; `reactive/ReactiveJasyncAccessor.java`.
- Modify: `appbase-client-jasync/pom.xml` — add version-less `io.projectreactor:reactor-core` (if the module doesn't already resolve `reactor.core.publisher.*`) + `io.projectreactor:reactor-test` (test); Export-Package `...service.jasync.reactive`.
- Test: `JasyncAccessorExceptionTest.java`; `reactive/ReactiveJasyncAccessorTest.java`.

**Interfaces:**
- Consumes: `AbstractReactiveAccessor` (`mono`/`flux`), the lazy `Supplier<AppbaseClientIntegrationService<Connection, TextValue>>` borrow.
- Produces:
  - `public class JasyncAccessorException extends AppbaseClientException` with ctors `(Throwable)` / `(String, Throwable)` and:
    ```java
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof DatabaseException) {
            return new JasyncAccessorException(cause);
        }
        return fallback.apply(cause);
    }
    ```
    (`com.github.jasync.sql.db.exceptions.DatabaseException`, `java.util.concurrent.{CompletionException,ExecutionException}`, `java.util.function.Function`.) Mirror `CassandraAccessorException`.
  - `public class ReactiveJasyncAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<Connection, TextValue>> borrow)` → `super(appbaseCore, reactorService, configuration)`.
    - `public Mono<QueryResult> sendQuery(String op, String query)` — `mono(op, () -> { svc = borrow.get(); return Mono.fromFuture(() -> svc.getClient().sendQuery(query)).doFinally(s -> svc.finalizeOperation()); })`.
    - `public Mono<QueryResult> sendPreparedStatement(String op, String query, List<Object> values)` — same shape via `svc.getClient().sendPreparedStatement(query, values)`.
    - `public Flux<RowData> queryRows(String op, String query, List<Object> values)` — `sendPreparedStatement(op, query, values).flatMapMany(qr -> Flux.fromIterable(qr.getRows()))` (`com.github.jasync.sql.db.RowData`). Javadoc: convenience over `getRows()`.
    - `public <T> Mono<T> command(String op, Function<Connection, CompletableFuture<T>> fn)` — `mono(op, () -> { svc = borrow.get(); return Mono.fromFuture(() -> fn.apply(svc.getClient())).doFinally(s -> svc.finalizeOperation()); })`.
    - `public Connection connection()` — `borrow.get().getClient()` (escape hatch; Javadoc: bypasses the spine).
    - `@Override mapError` = `JasyncAccessorException.map(t, super::mapError)`; `@Override isNotable` = `true`.
    - **Javadoc: `Mono.fromFuture` bridges jasync's `CompletableFuture` API; `doFinally` returns the borrow on complete/error/cancel; no eager client build (the pool IS the client).**

- [ ] **Step 1: Failing tests** — `JasyncAccessorExceptionTest`: a bare `DatabaseException` maps; a `CompletionException(cause=DatabaseException)` unwraps+maps; a non-DB throwable delegates to fallback. `ReactiveJasyncAccessorTest`: mock the integration service → mock `Connection`; stub `sendPreparedStatement(...)` → `CompletableFuture.completedFuture(mockQueryResult)`; assert `sendPreparedStatement("q", sql, params)` emits it (StepVerifier), a failed future with a `DatabaseException` surfaces `JasyncAccessorException`, `finalizeOperation()` runs, and a `Timer` is recorded (mirror `ReactiveCassandraAccessorTest`).
- [ ] **Step 2: Run — FAIL** (`-Dtest=JasyncAccessorExceptionTest,ReactiveJasyncAccessorTest`).
- [ ] **Step 3: Implement** the exception, accessor, pom changes.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression.**
- [ ] **Step 6: Commit** (`feat(P-027-R2a): JasyncAccessorException + ReactiveJasyncAccessor (Mono.fromFuture spine)`).

### Task 3: `getReactiveAccessor()` acquisition

**Files:**
- Modify: `JasyncClientService.java` (interface) + `DefaultJasyncClientService.java` — `getReactiveAccessor()` + keyed.
- Test: `DefaultJasyncClientServiceReactiveTest.java`.

**Interfaces:**
- Consumes: `getConfiguration().getReactiveOperationConfiguration()`, `appbaseClientCore.getAppbaseCore()` + `.getReactorService()`, the lazy `this::getClientIntegrationService` / `() -> getClientIntegrationService(key)` borrow.
- Produces (mirror `DefaultCassandraClientService.getReactiveAccessor`, substituting `TextValue`):
  - `ReactiveJasyncAccessor getReactiveAccessor()` = `new ReactiveJasyncAccessor(appbaseCore, reactorService, reactiveOperationConfiguration, this::getClientIntegrationService)`.
  - `ReactiveJasyncAccessor getReactiveAccessor(AppbaseClientKey<TextValue> appbaseClientKey)` = same with `() -> getClientIntegrationService(appbaseClientKey)`. No cache, no `shutdownImpl` change (lazy borrow; nothing to close).

- [ ] **Step 1: Failing test** — `autoStart(false)` + mocked core (mirror `DefaultCassandraClientServiceReactiveTest`): `getReactiveAccessor()` non-null + reactive-config passthrough; keyed non-null.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** getter (interface + impl).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression.**
- [ ] **Step 6: Commit** (`feat(P-027-R2a): getReactiveAccessor acquisition on Jasync client service`). Then open **PR 1** (Tasks 1–3) into the twin branch; STOP for merge.

---

## PR 2 — branch `feature/P-027-R2a-jasync-t4t6-sync-metrics-it` (Tasks 4–6, off the twin branch after PR 1 merges)

### Task 4: Sync `JasyncAccessor` + `getAccessor()`

**Files:**
- Create: `JasyncAccessor.java`.
- Modify: `JasyncClientService.java` + `DefaultJasyncClientService.java` — `getAccessor()` + keyed.
- Test: `JasyncAccessorTest.java`; acquisition cases in `DefaultJasyncClientServiceReactiveTest.java`.

**Interfaces:**
- Consumes: `AbstractAppbaseAccessor` (`execute`), `JasyncAccessorException.map`, the lazy borrow.
- Produces:
  - `public class JasyncAccessor extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<Connection, TextValue>> borrow)` → `super(appbaseCore, config.isMetricsEnabled(), config.isTraceEnabled(), config.isExceptionStoreEnabled())` (mirror `CassandraAccessor`).
  - `public QueryResult sendQuery(String op, String query) throws Exception` — `execute(op, () -> { svc = borrow.get(); try { return unwrap(() -> svc.getClient().sendQuery(query).get()); } finally { svc.finalizeOperation(); } })`.
  - `public QueryResult sendPreparedStatement(String op, String query, List<Object> values) throws Exception` — same via `sendPreparedStatement(query, values).get()`.
  - `public <T> T command(String op, Function<Connection, CompletableFuture<T>> fn) throws Exception` — `fn.apply(svc.getClient()).get()`.
  - `public Connection connection()` — `borrow.get().getClient()`.
  - private `unwrap` helper: catches `ExecutionException` → rethrows `getCause()`; catches `InterruptedException` → `Thread.currentThread().interrupt()` then rethrow (mirror Kafka's `KafkaProducerAccessor.unwrap`). (`.get()` wraps the driver exception in `ExecutionException`; unwrapping lets `JasyncAccessorException.map` match the `DatabaseException`.)
  - `mapError` = `JasyncAccessorException.map(t, super::mapError)`; `isNotable` = true.
  - On the service: `JasyncAccessor getAccessor()` + keyed (mirror `DefaultCassandraClientService.getAccessor`).

- [ ] **Step 1: Failing tests** — `JasyncAccessorTest`: mock integration service → mock `Connection`; assert `sendQuery`/`sendPreparedStatement` return the `QueryResult`, `finalizeOperation` on success AND error, a failed future's `DatabaseException` (wrapped in `ExecutionException`) surfaces `JasyncAccessorException`, `Timer` recorded. Acquisition: `getAccessor()` non-null + config passthrough.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** accessor + acquisition.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression.**
- [ ] **Step 6: Commit** (`feat(P-027-R2a): sync JasyncAccessor + getAccessor acquisition`).

### Task 5: Connection-pool metrics (config-gated)

**Files:**
- Modify: `configuration/JasyncClientIntegrationServiceConfiguration.java` (interface) + `configuration/DefaultJasyncClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled` (default true).
- Modify: `JasyncClientIntegrationService.java` (interface) + `DefaultJasyncClientIntegrationService.java` — `isDriverMetricsEnabled()`; a protected `bindPoolMetrics(Connection)` seam called from `initImpl` after `connect()` (config-gated).
- Test: `configuration/DefaultJasyncClientIntegrationServiceConfigurationTest.java`; `DefaultJasyncClientIntegrationServicePoolMetricsTest.java`.

**Interfaces:**
- Produces:
  - `boolean isDriverMetricsEnabled()` on the integration config (default true) + on the integration-service interface + impl (`config != null && config.isDriverMetricsEnabled()`; mirror Kafka).
  - protected `void bindPoolMetrics(final Connection client)` on `DefaultJasyncClientIntegrationService`:
    ```java
    if (!isDriverMetricsEnabled() || !(client instanceof ConnectionPool)) return;
    final ConnectionPool<?> pool = (ConnectionPool<?>) client;
    final MeterRegistry registry = appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry();
    Gauge.builder("jasync.pool.connections.inuse",  pool, ConnectionPool::getInUseConnectionsCount).register(registry);
    Gauge.builder("jasync.pool.connections.idle",   pool, ConnectionPool::getIdleConnectionsCount).register(registry);
    Gauge.builder("jasync.pool.connections.waiting",pool, p -> p.getFuturesWaitingForConnectionCount()).register(registry);
    ```
    Javadoc: jasync has no metrics registry — these gauges POLL the live pool; the composite is MetricService-owned (never shut down); differs from Kafka/Aerospike's real driver metrics. (`io.micrometer.core.instrument.{Gauge,MeterRegistry}`, `com.github.jasync.sql.db.pool.ConnectionPool`.)
  - Call `bindPoolMetrics(connection)` in `initImpl` after `connection.connect()`.
- Module pom may need `io.micrometer:micrometer-core` as a direct dep (add version-less if `io.micrometer.*` doesn't resolve).

- [ ] **Step 1: Failing tests** — config `isDriverMetricsEnabled()` default true + round-trip false; `bindPoolMetrics` with a mock `ConnectionPool` (stub the 3 counters) + a real `SimpleMeterRegistry` (via mocked core) → assert the 3 gauges registered with the polled values when enabled, and NONE registered when `driverMetricsEnabled(false)`.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** config, interface method, seam + wiring, pom.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression.**
- [ ] **Step 6: Commit** (`feat(P-027-R2a): Jasync connection-pool metrics bridge (config-gated)`).

### Task 6: Gated IT + build-guide docs + J-018

**Files:**
- Create: `reactive/ReactiveJasyncAccessorIT.java` (gated).
- Modify: `appbase-java/build-guide.md` — "Reactive Jasync (P-027 R2a)" section (after the Reactive Aerospike section).
- (Controller handles the J-018 clause in `Claude-Review.md` on `develop`.)

**Interfaces:**
- Consumes: `getReactiveAccessor()`, `getAccessor()`, a live MySQL/Postgres (via the module's existing gated-test config).

- [ ] **Step 1: Gated IT** — mirror `ReactiveCassandraAccessorIT`'s gating (`@EnabledIfSystemProperty` with the module's existing `Jasync…Tests.enabled` constant — confirm the exact name from `DefaultJasyncClientServiceTest` / `JasyncClientServiceGuiceModuleTest`). Reactive: `sendPreparedStatement` an INSERT then a SELECT → StepVerifier asserts the `QueryResult` rows. Sync: `getAccessor()` same round-trip. Use a temp table; clean up in `finally`.
- [ ] **Step 2: Run gated OFF** — confirm SKIPPED in a normal `install`.
- [ ] **Step 3: Docs** — add the "Reactive Jasync" build-guide section (both faces, the `Mono.fromFuture` bridge, pool-metrics gate, gated-IT flag, `install`-goal note).
- [ ] **Step 4: Whole-module build — no regression (IT skipped).**
- [ ] **Step 5: Commit** (`test(P-027-R2a): gated reactive+sync Jasync IT + build-guide`). Then open **PR 2** (Tasks 4–6) into the twin branch; STOP for merge.

---

## Twin completion (after both PRs merge)

- Append Jasync learnings to `appbase-java/LEARNING.md`.
- Open the roll-up `feature/P-027-R2a-jasync` → the reactive epic; post to the twin sub-issue; close it manually on merge.
- Controller adds the J-018 clause (Jasync gated IT) on `develop`.
- Do NOT merge the reactive epic → `develop` (only when the whole P-027 spec is done).

---

## Self-Review

**1. Coverage:** reactive config (T1); reactive accessor + exception + Mono.fromFuture bridge + pom (T2); reactive acquisition (T3); sync accessor + acquisition (T4); pool metrics + config gate (T5); gated IT + docs (T6). CompletableFuture bridge (Decision A) → T2; error unwrap both faces (C) → T2/T4; pool metrics not driver-registry (D) → T5; greenfield (E) → no untouched-code constraint. ✅
**2. Placeholders:** none — each task carries exact files, signatures, code, and a RED→GREEN cycle. Implementation bodies are shown as stubs/signatures per the project's plan-stub convention (reviewer verifies the PR). ✅
**3. Type consistency:** `Connection`/`QueryResult`/`RowData`/`TextValue`/`JasyncAccessorException.map`/`getReactiveAccessor()`/`getAccessor()`/`getReactiveOperationConfiguration()`/`isDriverMetricsEnabled()`/`bindPoolMetrics` used consistently T1–T6. Reactive returns `Mono<QueryResult>`/`Flux<RowData>`; sync returns `QueryResult`. ✅
