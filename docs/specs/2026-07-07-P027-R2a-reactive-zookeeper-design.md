# P-027 R2a — Reactive ZooKeeper Twin — Design

**Status:** approved (brainstorm) · **Date:** 2026-07-07 · **Epic:** `feature/P-027-reactive-appbase` · **Spec family:** P-027 R2a reactive driver twins (8th twin: Redis, Cassandra, Kafka, Aerospike, Jasync, Elasticsearch, **ZooKeeper**).

## Goal

Add reactive + synchronous **service-acquired** accessor faces to `appbase-client-zookeeper`, on the shared Appbase accessor spine, wrapping Curator via **`curator-x-async`** (`AsyncCuratorFramework`) for the reactive face and the blocking `CuratorFramework` for the sync face, plus a config-gated Curator **connection-state gauge**. Uniform with the prior twins: same accessor topology, `getReactiveAccessor()`/`getAccessor()` acquisition (incl. free keyed variants), same config/exception patterns.

## Shape vs. prior twins

ZooKeeper is the **Cassandra-shaped** case (unlike Elasticsearch's typed-layer detour): `ZooKeeperClientService extends AppbaseClientService<CuratorFramework, ClusterIdValue>` hands out the rich `CuratorFramework` directly, so the accessors borrow the pooled integration service (`Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>>`) and return it via `usingWhen`/`finalizeOperation` — and keyed variants are **free** (getters on the client service, `this::getClientIntegrationService`). ZooKeeper is fully synchronous today (no async surface); the reactive face is entirely new. Curator exposes no Micrometer registry, but its `ConnectionStateListener` gives a connectivity signal worth a gauge.

## Decisions (approved)

- **A — `curator-x-async` reactive bridge.** Add the `curator-x-async` dependency (same Curator version, managed in root `<dependencyManagement>` via `${curation.version}`; version-less in the module pom). Wrap the borrowed `CuratorFramework` as `AsyncCuratorFramework`; each reactive op is `mono(op, () -> Mono.usingWhen(Mono.fromSupplier(borrow), svc -> Mono.fromCompletionStage(() -> AsyncCuratorFramework.wrap(svc.getClient()).getData().forPath(path)), svc -> Mono.fromRunnable(svc::finalizeOperation)))`. `AsyncStage<T>` IS a `CompletionStage<T>`. Rejected: blocking-wrap on bounded-elastic (weaker, not truly async). The dep is **additive** (new artifact at the already-pinned Curator version, not a version bump), so it is added **within the twin** (T2), not as a separate flat-merge-to-epic-first (which your convention reserves for driver *bumps*). Documented as a deliberate divergence from the Cassandra/Aerospike driver-bump flow.
- **B — Op surface: 6 core znode CRUD + generic `command`.** `create`, `getData`, `setData`, `delete`, `checkExists`, `getChildren` on both faces, plus a generic escape hatch — reactive `<T> Mono<T> command(String op, Function<AsyncCuratorFramework, CompletionStage<T>> fn)` and sync `<T> T command(String op, Function<CuratorFramework, T> fn) throws Exception`. Recipes (locks, leader election, barriers, queues) are out of scope; the existing sync recipe accessor `DistributedCounterZKA`/`DefaultDistributedCounterZKA` is **untouched**.
- **C — Reuse `ZooKeeperAccessorException`.** It already exists (`extends AppbaseClientException`, ctors only). Add a static `map(Throwable, Function<Throwable,Throwable> fallback)` that unwraps `CompletionException`/`ExecutionException` → cause, maps `org.apache.zookeeper.KeeperException` → `ZooKeeperAccessorException`, else fallback (mirror `JasyncAccessorException.map`, substituting `KeeperException`). Add `@author Claude` when touched.
- **D — Connection-state gauge (config-gated driver metric).** Add `driverMetricsEnabled` (default true) to `ZooKeeperClientIntegrationServiceConfiguration`. In `DefaultZooKeeperClientIntegrationService`, a `bindConnectionStateMetrics(CuratorFramework)` (called from `initImpl` after `curatorFramework.start()`, config-gated) registers a `ConnectionStateListener` that writes a numeric `AtomicInteger` state code (CONNECTED/RECONNECTED = 1, SUSPENDED = 2, LOST = 3, READ_ONLY = 4) and a Micrometer gauge `zookeeper.connection.state` reading it, **tagged per client-key** (`Tags.of("key", appbaseClientKey.toString())`), on the composite registry (`appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry()`). `unbindConnectionStateMetrics()` (called from `shutdownImpl`, and first inside `bindConnectionStateMetrics` for repair-staleness) removes the gauge meter AND the listener. Mirrors the jasync `bindPoolMetrics`/`unbindPoolMetrics` lessons: **tag per key + unbind on shutdown**. This is *in addition to* the spine's automatic per-operation `Timer`s (latency/count/error per op) that every accessor op records — ZooKeeper gets both op-level metrics and the connection-state gauge.
- **E — Keyed free.** Getters on `ZooKeeperClientService` (interface + `DefaultZooKeeperClientService`), impl mirrors `DefaultCassandraClientService.getReactiveAccessor`/keyed with `this::getClientIntegrationService` / `() -> getClientIntegrationService(key)`.

## Components

New (package `…service.zookeeper.accessor`, reactive sub-package `.reactive`):
- `reactive/ReactiveZooKeeperAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow)`. 6 CRUD `Mono` ops (`create`→`Mono<String>`, `getData`→`Mono<byte[]>`, `setData`→`Mono<Stat>`, `delete`→`Mono<Void>`, `checkExists`→`Mono<Stat>`, `getChildren`→`Mono<List<String>>`) each via `Mono.usingWhen(borrow, svc -> Mono.fromCompletionStage(() -> AsyncCuratorFramework.wrap(svc.getClient()).<op>().forPath(...)), svc -> finalizeOperation)`; generic `<T> Mono<T> command(op, Function<AsyncCuratorFramework, CompletionStage<T>>)`. `mapError` = `ZooKeeperAccessorException.map(t, super::mapError)`; `isNotable` = `true`.
- `ZooKeeperAccessor extends AbstractAppbaseAccessor` (sync, `.accessor` package) — ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow)` → `super(appbaseCore, config.isMetricsEnabled(), config.isTraceEnabled(), config.isExceptionStoreEnabled())`. Same 6 CRUD via `execute(op, () -> { svc = borrow.get(); try { return svc.getClient().<op>().forPath(...); } finally { svc.finalizeOperation(); } })` (Curator blocking API throws `Exception`); generic `<T> T command(op, Function<CuratorFramework, T>) throws Exception`. Same `mapError`/`isNotable`.

Modified:
- `ZooKeeperAccessorException` — add static `map`.
- `ZooKeeperClientServiceConfiguration` (interface) + `DefaultZooKeeperClientServiceConfiguration` — `reactiveOperationConfiguration` (mirror `DefaultCassandraClientServiceConfiguration`).
- `ZooKeeperClientIntegrationServiceConfiguration` (interface) + `DefaultZooKeeperClientIntegrationServiceConfiguration` — `driverMetricsEnabled` (default true).
- `ZooKeeperClientService` (interface) + `DefaultZooKeeperClientService` — `getReactiveAccessor()`/keyed + `getAccessor()`/keyed.
- `DefaultZooKeeperClientIntegrationService` — `isDriverMetricsEnabled()`, `bindConnectionStateMetrics`/`unbindConnectionStateMetrics`, wired into `initImpl`/`shutdownImpl`.
- Root `pom.xml` — managed `curator-x-async` (`${curation.version}`). Module `pom.xml` — version-less `curator-x-async` + `reactor-core`/`reactor-test` (test) + Export-Package `…zookeeper.accessor.reactive`.

## Error / data flow

- **Reactive:** `Mono.fromCompletionStage` over `AsyncStage` — Reactor unwraps `CompletionException`, so `mapError` sees the raw `KeeperException`.
- **Sync:** Curator blocking calls throw `KeeperException`/checked `Exception` directly through `execute`.
- Shared `ZooKeeperAccessorException.map` handles both depths.

## Testing

- Unit: `ZooKeeperAccessorExceptionTest` (bare `KeeperException` maps; `CompletionException`-wrapped unwraps+maps; non-ZK fallback); `ReactiveZooKeeperAccessorTest` (mock integration service → mock `CuratorFramework`; stub `AsyncCuratorFramework` op → completed/failed `CompletableFuture`; `StepVerifier`; `Timer` recorded; `finalizeOperation` on complete + error); `ZooKeeperAccessorTest` (sync happy path + `KeeperException` → `ZooKeeperAccessorException`, `finalizeOperation`, `Timer`); config round-trip tests; acquisition tests (`getReactiveAccessor()`/`getAccessor()` + keyed non-null + passthrough); metrics test (`bindConnectionStateMetrics` with a `SimpleMeterRegistry` → gauge registered + tagged when enabled, none when disabled; `unbindConnectionStateMetrics` removes it).
- Gated IT: `reactive/ReactiveZooKeeperAccessorIT` (reactive + sync round-trip: `create` → `getData` asserts payload → `setData` → `getData` asserts new payload → `getChildren` → `delete` → `checkExists` null), gated `@EnabledIfSystemProperty(named = "ZooKeeperClientServiceTests.enabled", matches = "true")`. Recorded in **J-018 clause (j)**.
- Docs: `build-guide.md` "Reactive ZooKeeper" section.

## Tasks / PRs (max 3 tasks per PR)

Twin branch `feature/P-027-R2a-zookeeper` off the epic HEAD. New twin sub-issue under internal work item 277; post each PR link in sequence; roll up to the epic when both merge (close the sub-issue manually — GitHub auto-close fires only on merge to `develop`).

**PR 1** — `feature/P-027-R2a-zookeeper-t1t3-reactive`:
- **T1** `reactiveOperationConfiguration` on `ZooKeeperClientServiceConfiguration` (+ Default, + round-trip test).
- **T2** managed + module `curator-x-async` dep; `ZooKeeperAccessorException.map`; `ReactiveZooKeeperAccessor` (6 CRUD + `command`, `Mono.fromCompletionStage`); pom reactor-core/reactor-test + Export-Package; tests.
- **T3** `getReactiveAccessor()` + keyed acquisition on `ZooKeeperClientService` (+ test).

**PR 2** — `feature/P-027-R2a-zookeeper-t4t6-sync-metrics-it`:
- **T4** sync `ZooKeeperAccessor` + `getAccessor()` + keyed (+ tests).
- **T5** `driverMetricsEnabled` on the integration config; connection-state gauge (`bindConnectionStateMetrics`/`unbindConnectionStateMetrics`, per-key tagged, wired into `initImpl`/`shutdownImpl`); tests.
- **T6** gated `ReactiveZooKeeperAccessorIT` + `build-guide.md` section + J-018 clause (j).

## Global constraints (inherited)

Java 25, Reactor 3.6.8; Apache header + Javadoc on public methods + `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0` on every file; no FQCNs (imports only); dependency versions only in root `<dependencyManagement>`; non-obvious decisions documented at the code site (the `Mono.fromCompletionStage`/`AsyncCuratorFramework` bridge, the `CompletionException`/`ExecutionException` unwrap, the connection-state-code mapping, the tag-per-key + unbind gauge lifecycle). Build via `mvn -q -pl appbase-client/appbase-client-zookeeper -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`; ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): file and plugin deleted; no post-build cleanup needed.** after each build. Gated tests → J-018. Learnings → `appbase-java/LEARNING.md` at the end.
