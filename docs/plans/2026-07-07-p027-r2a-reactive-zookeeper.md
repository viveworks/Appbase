# P-027 R2a — Reactive ZooKeeper Twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reactive + synchronous **service-acquired** accessor faces to `appbase-client-zookeeper`, on the shared Appbase accessor spine, wrapping Curator via `curator-x-async` (`AsyncCuratorFramework`, `Mono.fromCompletionStage`) for the reactive face and the blocking `CuratorFramework` for the sync face, plus a config-gated Curator connection-state gauge.

**Architecture:** In-module, **Cassandra-shaped**. `ZooKeeperClientService extends AppbaseClientService<CuratorFramework, ClusterIdValue>` hands out the rich `CuratorFramework`, so accessors borrow the pooled integration service (`Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>>`) and return it via `usingWhen`/`finalizeOperation`; keyed variants are free (getters on the client service). ZooKeeper is fully sync today — the reactive face is new. Curator exposes no Micrometer registry, so "driver metrics" is a `ConnectionStateListener`-fed gauge (in addition to the spine's automatic per-op Timers).

**Tech Stack:** Java 25, Project Reactor 3.6.8, Apache Curator 5.x (`curator-framework` + new `curator-x-async`), Micrometer, Guice, Maven, JUnit 5 + Mockito + `reactor-test` StepVerifier.

**Reference twins (mirror exactly):** merged Cassandra (`appbase-client-cassandra/.../reactive/ReactiveCassandraAccessor.java` — `Mono.usingWhen(Mono.fromSupplier(borrow), svc -> …, svc -> finalizeOperation)`; `CassandraAccessor.java`; `DefaultCassandraClientService.getReactiveAccessor`/`getAccessor` + keyed; `DefaultCassandraClientServiceConfiguration` reactive-config field). Jasync (`JasyncAccessorException.map` unwrap; `DefaultJasyncClientIntegrationService.bindPoolMetrics`/`unbindPoolMetrics` — the per-key-tagged, unbind-on-shutdown gauge pattern). These are present in the working tree (this branch is off the epic, which merged both).

## Global Constraints

- **Branch/PR (max 3 tasks per PR):** twin branch `feature/P-027-R2a-zookeeper` off the reactive epic (`feature/P-027-reactive-appbase`) HEAD. **PR 1** = Tasks 1–3 on `feature/P-027-R2a-zookeeper-t1t3-reactive`; **PR 2** = Tasks 4–6 on `feature/P-027-R2a-zookeeper-t4t6-sync-metrics-it`. Both target the twin branch. When both merge, roll up `feature/P-027-R2a-zookeeper` → the epic (close the twin sub-issue manually — GitHub auto-close fires only on merge to `develop`). Post each PR link to the twin sub-issue in sequence.
- **`curator-x-async` is additive** (new artifact at the already-pinned `${curation.version}`, NOT a version bump), so it is added in-twin (Task 2), not flat-merged to the epic first.
- **Every Java file:** Apache license header (copy verbatim from a sibling in the module), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`. No fully-qualified class names in code (imports only) — sole exception: class-name string constants.
- **Decisions documented in code:** the `AsyncCuratorFramework`/`Mono.fromCompletionStage` bridge, the `CompletionException`/`ExecutionException` unwrap, the connection-state-code mapping, the tag-per-key + unbind-on-shutdown gauge lifecycle.
- **Dependency versions ONLY in root `pom.xml` `<dependencyManagement>`**; module poms version-less. (`reactor`/`reactor-test` already managed from R0; `curator-x-async` added to root in Task 2.)
- **Build command:** `cd /Users/nav/Viveworks/Technology/Appbase/appbase-java && mvn -q -pl appbase-client/appbase-client-zookeeper -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install` (use `install`, NOT `test` — bnd Export-Package on `.reactive` fails under `test`). Scope one class with `-Dtest=<Class>` (still `install`). `-Dsurefire.failIfNoSpecifiedTests=false` is MANDATORY.
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.** (regenerated; never commit).
- **Key type:** `AppbaseClientKey<ClusterIdValue>` (like Cassandra).
- **Gated tests → J-018.** **Learnings → `appbase-java/LEARNING.md`** at the end.

## Established facts (verified against the codebase)

- `AbstractReactiveAccessor(AppbaseCore, ReactorService, ReactiveOperationConfiguration)`; protected `<T> Mono<T> mono(String op, Supplier<Mono<T>> source)` / `<T> Flux<T> flux(String op, Supplier<Flux<T>> source)`; overridable `mapError`/`isNotable`.
- `AbstractAppbaseAccessor(AppbaseCore, boolean metricsEnabled, boolean traceEnabled, boolean exceptionStoreEnabled)`; protected `execute(String op, Callable<T>)` throws `Exception`.
- Both accessors read metrics/trace/exceptionStore flags from `ReactiveOperationConfiguration` (`isMetricsEnabled`/`isTraceEnabled`/`isExceptionStoreEnabled`/`getTimeout`). `DefaultReactiveOperationConfiguration`/`ReactiveOperationConfiguration` in `com.viveworks.appbase.client.core.reactive`.
- `ZooKeeperClientService extends AppbaseClientService<CuratorFramework, ClusterIdValue>` with `getClientIntegrationService()` + keyed. `DefaultZooKeeperClientService extends AbstractAppbaseClientService`; has `appbaseClientCore` (→ `getAppbaseCore()`/`getReactorService()`) and `getConfiguration()`. Mirror `DefaultCassandraClientService` acquisition.
- Both config interfaces are EMPTY (`ZooKeeperClientServiceConfiguration extends AppbaseClientServiceConfiguration {}`, `ZooKeeperClientIntegrationServiceConfiguration extends AppbaseClientIntegrationServiceConfiguration {}`) — Task 1 / Task 5 add the getters.
- `ZooKeeperAccessorException extends AppbaseClientException` exists with ctors only (`()`, `(String)`, `(String,Throwable)`, `(Throwable)`, 4-arg) + `serialVersionUID`; Task 2 adds the static `map`. Missing `@author Claude` — add it when touched.
- `DefaultZooKeeperClientIntegrationService extends AbstractAppbaseClientIntegrationService<CuratorFramework, ClusterIdValue, DefaultZooKeeperClientIntegrationServiceConfiguration>`; `initImpl()` builds + `curatorFramework.start()` (line ~229) then `curatorFrameworkRef.set(...)`; `shutdownImpl()`; `getClient()` → `CuratorFramework`. Has `appbaseClientCore` + `appbaseClientKey` (like jasync). Metric registry: `appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry()`.
- Root pom curator version property is `${curation.version}` (note spelling); `curator-framework`/`curator-recipes` managed at pom.xml:978–987.
- `AsyncCuratorFramework.wrap(CuratorFramework)` → `AsyncCuratorFramework`; ops return `AsyncStage<T>` (which `extends CompletionStage<T>`): `create().forPath(path, byte[])`→`String`, `getData().forPath(path)`→`byte[]`, `setData().forPath(path, byte[])`→`Stat`, `delete().forPath(path)`→`Void`, `checkExists().forPath(path)`→`Stat` (null if absent), `getChildren().forPath(path)`→`List<String>`. (`org.apache.zookeeper.data.Stat`.) Sync `CuratorFramework` has the same fluent chains returning `T` directly and throwing checked `Exception`.
- Gate: `@EnabledIfSystemProperty(named = "ZooKeeperClientServiceTests.enabled", matches = "true")` (mirror the module's existing gated tests — confirm the exact property name from an existing `*Test` in the module before writing the IT).

---

## File Structure

**ZooKeeper module (`.../service/zookeeper/`):**
- Modify `configuration/ZooKeeperClientServiceConfiguration.java` (interface) + `configuration/DefaultZooKeeperClientServiceConfiguration.java` — `reactiveOperationConfiguration`.
- Modify `configuration/ZooKeeperClientIntegrationServiceConfiguration.java` (interface) + `configuration/DefaultZooKeeperClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled`.
- Modify `accessor/ZooKeeperAccessorException.java` — static `map`.
- Create `accessor/reactive/ReactiveZooKeeperAccessor.java`.
- Create `accessor/ZooKeeperAccessor.java` (sync).
- Modify `ZooKeeperClientService.java` (interface) + `DefaultZooKeeperClientService.java` — `getReactiveAccessor()`/`getAccessor()` (+ keyed).
- Modify `ZooKeeperClientIntegrationService.java` (interface) + `DefaultZooKeeperClientIntegrationService.java` — `isDriverMetricsEnabled()`; `bindConnectionStateMetrics`/`unbindConnectionStateMetrics`; wire into `initImpl`/`shutdownImpl`.
- Root `pom.xml` — managed `curator-x-async` (`${curation.version}`). Module `pom.xml` — version-less `curator-x-async` + version-less `reactor-core` (if not resolved) + `reactor-test` (test); Export-Package `…zookeeper.accessor.reactive`.

**Tests/IT + docs:** unit tests per accessor + acquisition + exception map + config + metrics; gated `ReactiveZooKeeperAccessorIT`; `build-guide.md` "Reactive ZooKeeper" section; J-018 clause (j).

---

## PR 1 — branch `feature/P-027-R2a-zookeeper-t1t3-reactive` (Tasks 1–3)

### Task 1: `reactiveOperationConfiguration` on the ZooKeeper client-service config

**Files:** Modify `configuration/ZooKeeperClientServiceConfiguration.java` (interface) + `configuration/DefaultZooKeeperClientServiceConfiguration.java`. Test: `configuration/DefaultZooKeeperClientServiceConfigurationTest.java`.

**Interfaces:** Produces `ReactiveOperationConfiguration getReactiveOperationConfiguration()` on the interface (abstraction), impl returns concrete `DefaultReactiveOperationConfiguration` (default `DefaultReactiveOperationConfiguration.builder().build()`), mirroring `DefaultCassandraClientServiceConfiguration` EXACTLY (field, `DEFAULT_REACTIVE_OPERATION_CONFIGURATION`, ctor-from-builder with `AppbaseUtil.notNull`, Builder setter inside `// CheckStyle: Builder start`/`end`, equals/hashCode/toString). Interface declares the abstraction `ReactiveOperationConfiguration`; impl covariantly returns `DefaultReactiveOperationConfiguration`.

- [ ] **Step 1: Failing test** — assert getter defaults non-null with `metricsEnabled()==true`, and a Jackson YAML round-trip preserves an overridden `metricsEnabled(false)`. Mirror `DefaultCassandraClientServiceConfigurationTest`.
- [ ] **Step 2: Run — FAIL** (`-Dtest=DefaultZooKeeperClientServiceConfigurationTest`).
- [ ] **Step 3: Add** the interface getter (`ReactiveOperationConfiguration`) + the Default impl field/constant/ctor/getter(covariant)/Builder-setter/equals/hashCode/toString. Confirm `Builder.getThis()` returns `this`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): reactive operation config on ZooKeeper client-service config`.

### Task 2: `curator-x-async` dep + `ZooKeeperAccessorException.map` + `ReactiveZooKeeperAccessor`

**Files:** Root `pom.xml` (managed `curator-x-async`); module `pom.xml` (version-less `curator-x-async`, `reactor-core` if needed, `reactor-test` test; Export-Package `…accessor.reactive`). Modify `accessor/ZooKeeperAccessorException.java`. Create `accessor/reactive/ReactiveZooKeeperAccessor.java`. Tests: `accessor/ZooKeeperAccessorExceptionTest.java`, `accessor/reactive/ReactiveZooKeeperAccessorTest.java`.

**Interfaces:**
- `ZooKeeperAccessorException` gains:
  ```java
  public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
      final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
              && throwable.getCause() != null ? throwable.getCause() : throwable;
      if (cause instanceof KeeperException) {              // org.apache.zookeeper.KeeperException
          return new ZooKeeperAccessorException(cause);
      }
      return fallback.apply(cause);
  }
  ```
  (`org.apache.zookeeper.KeeperException`, `java.util.concurrent.{CompletionException,ExecutionException}`, `java.util.function.Function`.) Mirror `JasyncAccessorException.map`. Add `@author Claude`.
- `ReactiveZooKeeperAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow)` → `super(appbaseCore, reactorService, configuration)`; `borrow = AppbaseUtil.notNull(borrow,"borrow")`. Each op mirrors `ReactiveCassandraAccessor`'s `usingWhen` shape but with `Mono.fromCompletionStage`:
  ```java
  public Mono<byte[]> getData(final String op, final String path) {
      AppbaseUtil.notNull(op, "op"); AppbaseUtil.notNull(path, "path");
      return mono(op, () -> Mono.usingWhen(
              Mono.fromSupplier(borrow),
              svc -> Mono.fromCompletionStage(() -> AsyncCuratorFramework.wrap(svc.getClient()).getData().forPath(path)),
              svc -> Mono.fromRunnable(svc::finalizeOperation)));
  }
  ```
  Ops: `create(op, path, byte[] data)`→`Mono<String>` (`create().forPath(path, data)`), `getData(op, path)`→`Mono<byte[]>`, `setData(op, path, byte[] data)`→`Mono<Stat>`, `delete(op, path)`→`Mono<Void>`, `checkExists(op, path)`→`Mono<Stat>`, `getChildren(op, path)`→`Mono<List<String>>`. Generic `<T> Mono<T> command(String op, Function<AsyncCuratorFramework, CompletionStage<T>> fn)` — `Mono.usingWhen(borrow, svc -> Mono.fromCompletionStage(() -> fn.apply(AsyncCuratorFramework.wrap(svc.getClient()))), svc -> finalizeOperation)`. `mapError` = `ZooKeeperAccessorException.map(t, super::mapError)`; `isNotable` = `true`. **Javadoc: `AsyncCuratorFramework.wrap` + `Mono.fromCompletionStage` bridge Curator's `AsyncStage` (a `CompletionStage`) to Reactor; Reactor unwraps `CompletionException`; `usingWhen` returns the borrowed integration service on complete/error/cancel.**

- [ ] **Step 1: Failing tests** — `ZooKeeperAccessorExceptionTest` (bare `KeeperException` maps; `CompletionException(cause=KeeperException)` unwraps+maps; non-ZK → fallback). `ReactiveZooKeeperAccessorTest` (mock integration service → mock `CuratorFramework`; because `AsyncCuratorFramework.wrap` is static over the real `CuratorFramework`, prefer testing via the `command(op, fn)` seam with a stubbed `fn` returning `CompletableFuture.completedFuture(value)` / a failed future with a `KeeperException`, asserting StepVerifier emit / `ZooKeeperAccessorException`, `finalizeOperation` on complete + error, and a `Timer` recorded — mirror `ReactiveCassandraAccessorTest`. If a per-op test needs `AsyncCuratorFramework`, drive it through `command`).
- [ ] **Step 2: Run — FAIL** (`-Dtest=ZooKeeperAccessorExceptionTest,ReactiveZooKeeperAccessorTest`).
- [ ] **Step 3: Implement** the exception `map`, the accessor (6 CRUD + `command`), the root+module pom deps + Export-Package.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): curator-x-async + ZooKeeperAccessorException.map + ReactiveZooKeeperAccessor`.

### Task 3: `getReactiveAccessor()` acquisition

**Files:** Modify `ZooKeeperClientService.java` (interface) + `DefaultZooKeeperClientService.java`. Test: `DefaultZooKeeperClientServiceReactiveTest.java`.

**Interfaces:** `ReactiveZooKeeperAccessor getReactiveAccessor()` + `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` on the interface; impl mirrors `DefaultCassandraClientService`:
```java
@Override public ReactiveZooKeeperAccessor getReactiveAccessor() {
    return new ReactiveZooKeeperAccessor(appbaseClientCore.getAppbaseCore(),
            appbaseClientCore.getAppbaseCore().getReactorService(),
            getConfiguration().getReactiveOperationConfiguration(),
            this::getClientIntegrationService);
}
@Override public ReactiveZooKeeperAccessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> key) {
    return new ReactiveZooKeeperAccessor(appbaseClientCore.getAppbaseCore(),
            appbaseClientCore.getAppbaseCore().getReactorService(),
            getConfiguration().getReactiveOperationConfiguration(),
            () -> getClientIntegrationService(key));
}
```
(`getConfiguration()` returns the ZK client-service configuration; if the base `getConfiguration()` is generic, use the same call style as `DefaultCassandraClientService`.) No cache; no `shutdownImpl` change.

- [ ] **Step 1: Failing test** — `autoStart(false)` + mocked core (mirror `DefaultCassandraClientServiceReactiveTest`): `getReactiveAccessor()` non-null + reactive-config passthrough; keyed non-null.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** getters (interface + impl).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): getReactiveAccessor acquisition on ZooKeeper client service`. Then open **PR 1** (Tasks 1–3) into the twin branch; STOP for merge.

---

## PR 2 — branch `feature/P-027-R2a-zookeeper-t4t6-sync-metrics-it` (Tasks 4–6, off the twin branch after PR 1 merges)

### Task 4: Sync `ZooKeeperAccessor` + `getAccessor()`

**Files:** Create `accessor/ZooKeeperAccessor.java`. Modify `ZooKeeperClientService.java` + `DefaultZooKeeperClientService.java` — `getAccessor()` + keyed. Tests: `accessor/ZooKeeperAccessorTest.java`; acquisition cases in `DefaultZooKeeperClientServiceReactiveTest.java`.

**Interfaces:** `ZooKeeperAccessor extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration configuration, Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow)` → `super(appbaseCore, configuration.isMetricsEnabled(), configuration.isTraceEnabled(), configuration.isExceptionStoreEnabled())` (mirror `CassandraAccessor`, `AppbaseUtil.notNull` on configuration + borrow). Each op:
```java
public byte[] getData(final String op, final String path) throws Exception {
    AppbaseUtil.notNull(op, "op"); AppbaseUtil.notNull(path, "path");
    return execute(op, () -> {
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
        try { return svc.getClient().getData().forPath(path); }
        finally { svc.finalizeOperation(); }
    });
}
```
Ops: `create`/`getData`/`setData`/`delete`/`checkExists`/`getChildren` (same signatures as the reactive face but blocking return types, `throws Exception`) + generic `<T> T command(String op, Function<CuratorFramework, T> fn) throws Exception`. `mapError` = `ZooKeeperAccessorException.map(t, super::mapError)`; `isNotable` = `true`. On the service: `ZooKeeperAccessor getAccessor()` + keyed (mirror `DefaultCassandraClientService.getAccessor`, borrow `this::getClientIntegrationService` / `() -> getClientIntegrationService(key)`).

- [ ] **Step 1: Failing tests** — `ZooKeeperAccessorTest` (mock integration service → mock `CuratorFramework` fluent chain; assert `getData`/`create` return the value, `finalizeOperation` on success AND error, a `KeeperException` surfaces `ZooKeeperAccessorException`, `Timer` recorded). Acquisition: `getAccessor()` non-null + config passthrough; keyed non-null.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** accessor + acquisition.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): sync ZooKeeperAccessor + getAccessor acquisition`.

### Task 5: Connection-state gauge (config-gated)

**Files:** Modify `configuration/ZooKeeperClientIntegrationServiceConfiguration.java` (interface) + `configuration/DefaultZooKeeperClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled` (default true). Modify `ZooKeeperClientIntegrationService.java` (interface) + `DefaultZooKeeperClientIntegrationService.java` — `isDriverMetricsEnabled()`; `bindConnectionStateMetrics`/`unbindConnectionStateMetrics`; wire into `initImpl`/`shutdownImpl`. Tests: `configuration/DefaultZooKeeperClientIntegrationServiceConfigurationTest.java`; `DefaultZooKeeperClientIntegrationServiceConnectionMetricsTest.java`.

**Interfaces:**
- `boolean isDriverMetricsEnabled()` on the integration config (default true) + integration-service interface + impl (`config != null && config.isDriverMetricsEnabled()`; mirror jasync).
- `protected void bindConnectionStateMetrics(final CuratorFramework client)` on `DefaultZooKeeperClientIntegrationService` (mirror `DefaultJasyncClientIntegrationService.bindPoolMetrics`):
  ```java
  if (!isDriverMetricsEnabled()) { return; }
  unbindConnectionStateMetrics();                                  // repair-staleness (register() dedups by Id)
  final MeterRegistry registry = appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry();
  final Tags keyTag = Tags.of("key", appbaseClientKey.toString());
  // AtomicInteger connectionStateCode holds the latest numeric state
  this.connectionStateListener = (c, newState) -> connectionStateCode.set(stateCode(newState));  // ConnectionState -> int
  client.getConnectionStateListenable().addListener(connectionStateListener);
  this.connectionStateGauge = Gauge.builder("zookeeper.connection.state", connectionStateCode, AtomicInteger::get)
          .tags(keyTag).register(registry);
  ```
  with a private `static int stateCode(ConnectionState s)` mapping CONNECTED/RECONNECTED→1, SUSPENDED→2, LOST→3, READ_ONLY→4 (0 = unknown/initial). Fields: `AtomicInteger connectionStateCode = new AtomicInteger(0)`, nullable `ConnectionStateListener connectionStateListener`, nullable `Meter connectionStateGauge`. (`io.micrometer.core.instrument.{Gauge,MeterRegistry,Meter,Tags}`, `org.apache.curator.framework.state.{ConnectionState,ConnectionStateListener}`.)
- `protected void unbindConnectionStateMetrics()` — if a gauge was registered, `registry.remove(connectionStateGauge)` and, if a listener was added, `getClient().getConnectionStateListenable().removeListener(connectionStateListener)`; null the fields (guard against `getClient()` returning null during shutdown — read the CuratorFramework defensively).
- Call `bindConnectionStateMetrics(curatorFramework)` in `initImpl` **after** `curatorFramework.start()` (line ~229). Call `unbindConnectionStateMetrics()` in `shutdownImpl` (before the client is closed/nulled).
- Module pom may need version-less `io.micrometer:micrometer-core` if `io.micrometer.*` doesn't already resolve for the module (mirror jasync).
- **Javadoc: Curator has no metrics registry — this gauge reflects the live `ConnectionStateListener` state, per-key tagged; the composite registry is MetricService-owned (never shut down); unbind on shutdown so repair/reconnect re-registers fresh. This is in addition to the spine's per-op Timers.**

- [ ] **Step 1: Failing tests** — config `isDriverMetricsEnabled()` default true + round-trip false; `bindConnectionStateMetrics` with a mock `CuratorFramework` (stub `getConnectionStateListenable()` to capture the listener) + a real `SimpleMeterRegistry` (via mocked core) → assert the gauge registered + key-tagged when enabled, NONE when `driverMetricsEnabled(false)`; driving the captured listener with `ConnectionState.LOST` moves the gauge value to `3`; `unbindConnectionStateMetrics` removes the gauge + listener.
- [ ] **Step 2: Run — FAIL** (`-Dtest=DefaultZooKeeperClientIntegrationServiceConfigurationTest,DefaultZooKeeperClientIntegrationServiceConnectionMetricsTest`).
- [ ] **Step 3: Implement** config flag, `isDriverMetricsEnabled`, bind/unbind, `initImpl`/`shutdownImpl` wiring, pom micrometer-core if needed.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): config-gated Curator connection-state gauge (per-key tagged, unbind on shutdown)`.

### Task 6: Gated reactive+sync IT + build-guide + J-018 clause

**Files:** Create `src/test/java/.../accessor/reactive/ReactiveZooKeeperAccessorIT.java`. Modify `appbase-java/build-guide.md`; `Claude-Review.md` (J-018 clause (j) — on develop at roll-up).

**Interfaces:** Consumes `DefaultZooKeeperClientService.getReactiveAccessor()`/`getAccessor()`, a live ZooKeeper bootstrapped via the module's existing ZK test helper/config (`ZooKeeperClientTestHelper`).

- [ ] **Step 1: Write the gated IT** — `@EnabledIfSystemProperty(named = "ZooKeeperClientServiceTests.enabled", matches = "true")` (confirm the exact property from an existing module `*Test`). `@BeforeAll` boot the client against a live ZooKeeper (reuse `ZooKeeperClientTestHelper`). Reactive round-trip via `getReactiveAccessor()`: `create` a znode → `getData` (StepVerifier asserts the payload) → `setData` → `getData` (asserts new payload) → `getChildren` (parent) → `delete` → `checkExists` (asserts `Stat` is null / empty). Sync round-trip via `getAccessor()` mirroring the sequence. Javadoc: requires a running ZooKeeper reachable per the module's gated-test config.
- [ ] **Step 2: Run — verify SKIPPED by default** (build command, no property → IT skipped, module green). If a live ZK is available, optionally run gated; else note it un-run and record under J-018.
- [ ] **Step 3: build-guide section** — "Reactive ZooKeeper": acquire `getReactiveAccessor()`/`getAccessor()` (+ keyed via `AppbaseClientKey<ClusterIdValue>`), the `AsyncCuratorFramework`/`Mono.fromCompletionStage` bridge note, the connection-state gauge (config-gated `driverMetricsEnabled`, per-key tag), and the gated-IT command. Mirror the prior twins' build-guide sections.
- [ ] **Step 4: J-018 clause (j)** — append to the J-018 action-plan table row: the new `ReactiveZooKeeperAccessorIT` (reactive + sync `create`→`getData`→`setData`→`getChildren`→`delete`→`checkExists` round-trip) gated by `-DZooKeeperClientServiceTests.enabled=true`, skipped by default; needs a live ZooKeeper + test-resource wiring; the connection-state gauge's live binding (listener + gauge against a connected `CuratorFramework`) is validated only by this IT since `initImpl` connects — bind/unbind/tag seams are unit-tested, live binding is not. Un-gate, run, fix here. **(Do this on `develop` at roll-up time, alongside the LEARNING.md append.)**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `test(P-027-R2a): gated reactive+sync ZooKeeper IT + build-guide`. Then open **PR 2** (Tasks 4–6) into the twin branch; STOP for merge.

---

## Post-merge (both PRs merged into the twin branch)

- Roll up `feature/P-027-R2a-zookeeper` → `feature/P-027-reactive-appbase` (merge; push the epic).
- Close the twin sub-issue manually (comment the roll-up merge SHA).
- On `develop`: append the ZooKeeper learnings to `appbase-java/LEARNING.md` (the `AsyncCuratorFramework`/`Mono.fromCompletionStage` bridge; reuse-the-existing-exception + add-map; connection-state gauge as a listener-fed metric vs jasync's pool-poll gauge; the same tag-per-key + unbind lesson) and the J-018 clause (j).

## Self-review notes (spec coverage)

- Decisions A–E mapped: A→Task 2 (curator-x-async + `Mono.fromCompletionStage`); B→Tasks 2+4 (6 CRUD + `command`, both faces); C→Task 2 (`ZooKeeperAccessorException.map`); D→Task 5 (connection-state gauge, per-key tagged, unbind); E→Tasks 3+4 (keyed getters on the client service). Spine per-op Timers are inherent to every `mono`/`execute` (no task).
- Type consistency: `ReactiveOperationConfiguration` supplies the sync accessor's flags; keyed uses `AppbaseClientKey<ClusterIdValue>`; reactive op return types match the `AsyncStage<T>` element types (`String`/`byte[]`/`Stat`/`Void`/`Stat`/`List<String>`); the gauge is `zookeeper.connection.state` tagged `key`.
