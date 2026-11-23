# P-027 R2a — Reactive Aerospike Twin — Design

**Status:** approved-pending-review (design phase)
**Date:** 2026-07-06
**Author:** Navaneeth Mysore G · Claude
**Parent:** P-027 (Reactive / Non-Blocking Appbase), R2a driver twins. Follows the merged Redis (internal work item 287), Cassandra (internal work item 296), and Kafka (internal work item 297) twins.
**Module:** `appbase-client/appbase-client-aerospike`

---

## 1. Goal

Add reactive + synchronous **service-acquired** accessor faces to the Aerospike client service, built on `aerospike-reactor-client` (official Reactor client), on the shared `AbstractAppbaseAccessor`/`AbstractReactiveAccessor` spine (config-gated metrics→JMX, tracing, exception-store, error mapping). Bump the Aerospike client to a version that ships a matching reactor-client AND a metrics API, and route Aerospike's driver-native metrics into the Appbase composite/JMX. One PR per task, on a phase branch off the reactive epic.

## 2. How Aerospike differs from the prior twins

- **A mature accessor already exists.** `AbstractAerospikeAccessor` is a user-EXTENDED abstract base (protected generic semantic methods `read<T>`/`delete`/`createOnly<T>`/`createOrUpdate`/`replaceOnly`/`createOrReplace`/`updateOnly`) with BESPOKE per-op timers — NOT on the shared spine, NOT service-acquired. It stays UNTOUCHED. The twin adds new, separate **service-acquired concrete** accessors (Decision B), consistent with the Redis/Cassandra/Kafka model.
- **Async infra already wired.** The integration service already builds `NettyEventLoops`/`EventLoops` + an `IAerospikeClient` — the foundation the reactor client wraps. No second cluster connection.
- **The reactor client is a NON-OWNING wrapper.** `AerospikeReactorClient(IAerospikeClient, EventLoops)` wraps the existing client; its `close()` closes the wrapped `IAerospikeClient` — a double-close hazard. The twin NEVER closes it (Decision D).
- **`AerospikeException` is a RuntimeException** surfaced as a reactive `onError`; the module's existing `AerospikeAccessorException` already maps it — reuse it.

## 3. Decisions

- **A — Driver bump `aerospike-client` 4.4.13 → 7.1.0 + add `aerospike-reactor-client:7.1.0`.** CONTAINED bump — a fail-at-end reactor build (main + test sources) proved the true scope is **exactly one** mechanical call-site migration: 7.x removed the generic `Bin(String, Object)` constructor, breaking `DefaultUserSessionStore.java:436` (`new Bin(k, v)` → `new Bin(k, Value.get(v))`, behavior-identical, verified via `javap`). Every other surface is source-compatible (`AerospikeClient`/`ClientPolicy`/`IAerospikeClient` methods, `com.aerospike.client.async.{EventLoops,NettyEventLoops}`, `WritePolicy`/`RecordExistsAction`, and the `ReadModeAP`/`ReadModeSC`/`Replica`/`AuthMode` enums through 7.x). _(Correction: the original spec claimed 0 breakage; the incremental investigation missed the removed `Bin(Object)` ctor — Task 1/internal work item 308 handles the one-line migration.)_ Licenses: `aerospike-client:7.1.0` and `aerospike-reactor-client:7.1.0` are both Apache-2.0. Matched pair (reactor-core 3.5.8 baseline, overridden by the repo's `${reactor.version}=3.6.8`; plain `aerospike-client` artifact, Java-8 bytecode → fine on Java 25). **7.1.0 (not 6.x)** because the `MetricsListener` API (Decision C) is 7.0+; 6.x has no metrics API, and the 4.4.x reactor-client has no matched 4.4.13 build. Avoid 8.x/9.x (artifactId switches to `aerospike-client-jdk8`).
- **B — Two service-acquired concrete accessors** on the shared spine: `ReactiveAerospikeAccessor` (`getReactiveAccessor()`) + `AerospikeAccessor` (`getAccessor()`), plus keyed variants. The existing `AbstractAerospikeAccessor` is left as-is.
- **C — Driver-native metrics via a `MetricsListener`→Micrometer bridge** (7.x), config-gated → composite/JMX.
- **D — Reactor client is a NON-OWNING wrapper**; never `close()`d by the twin.
- **E — decisions documented at the code site** (Javadoc/inline), not only in this spec.
- Reuse the existing `AerospikeAccessorException` (both faces map through it).

## 4. Components

### 4.1 `ReactiveAerospikeAccessor extends AbstractReactiveAccessor`
Wraps a `AerospikeReactorClient` (injected; built by the service from the existing `IAerospikeClient` + `EventLoops`). Aerospike ops are DISCRETE bounded ops → full `mono`/`flux` spine.
- `Mono<KeyRecord> get(String op, Key)` / `get(String op, Policy, Key)`
- `Mono<Key> put(String op, Key, Bin...)` / `put(String op, WritePolicy, Key, Bin...)`
- `Mono<Key> delete(String op, Key)` / `delete(String op, WritePolicy, Key)`
- `Mono<Key> exists(String op, Key)` (reactor-client `exists` emits the `Key` iff the record exists)
- `Mono<KeyRecord> operate(String op, WritePolicy, Key, Operation...)`
- `Flux<KeyRecord> query(String op, Statement)` / `query(String op, QueryPolicy, Statement)`
- Escape hatches: `<T> Mono<T> command(String op, Function<AerospikeReactorClient, Mono<T>>)`, `<T> Flux<T> queryMany(String op, Function<AerospikeReactorClient, Flux<T>>)`, `AerospikeReactorClient reactor()`.
- `@Override mapError` = `AerospikeAccessorException.map(t, super::mapError)`; `@Override isNotable` = true.

### 4.2 `AerospikeAccessor extends AbstractAppbaseAccessor` (sync)
Over `IAerospikeClient` (borrowed from the integration service; Aerospike client is a thread-safe singleton — no per-op pooled borrow/return).
- `Record get(String op, [Policy,] Key) throws Exception`, `void put(String op, [WritePolicy,] Key, Bin...) throws Exception`, `boolean delete(String op, [WritePolicy,] Key) throws Exception`, `boolean exists(String op, Key) throws Exception`, `Record operate(String op, WritePolicy, Key, Operation...) throws Exception`.
- `<T> T command(String op, Function<IAerospikeClient, T>) throws Exception`, `IAerospikeClient client()`.
- `mapError`/`isNotable` as above.

### 4.3 Acquisition
`AerospikeClientService`: `getReactiveAccessor()`/`(key)` + `getAccessor()`/`(key)`. Built with `appbaseCore`, `getReactorService()` (reactive), `getConfiguration().getReactiveOperationConfiguration()`. The `AerospikeReactorClient` is built once (cached, per key) from `getClientIntegrationService().getClient()` + the integration service's `EventLoops` and NEVER closed.

## 5. Configuration
- `reactiveOperationConfiguration` block on `DefaultAerospikeClientServiceConfiguration` (mirror the prior twins; same R0 knobs).
- `driverMetricsEnabled` (default true) on `DefaultAerospikeClientIntegrationServiceConfiguration` (gates §6).

## 6. Driver-native metrics (Decision C — 7.x)
When `driverMetricsEnabled`, the integration service calls `iAerospikeClient.enableMetrics(new MetricsPolicy().setListener(appbaseMetricsListener))` on start and `disableMetrics()` on shutdown. The custom `MetricsListener`:
- `onSnapshot(Cluster)` (fired every `MetricsPolicy.interval`s) walks `cluster.getNodes()` → per node registers/updates Micrometer meters on the shared composite (`MetricService.getCompositeMeterRegistry()`, → JMX): per-`LatencyType` (CONN/WRITE/READ/BATCH/QUERY) latency-bucket gauges + `errorCount`/`timeoutCount`/connection-stat counters, tagged by node + type.
- Registry is `MetricService`-owned — NEVER shut down; `disableMetrics()` stops the listener. Histogram buckets are exponential (per `latencyShift`) — map bucket counts to gauges.
- Feasible/moderate (~0.5–1.5 day); a PUSH listener, not file-only (set `MetricsPolicy.listener` to bypass the default file writer).

## 7. Lifecycle
The `AerospikeReactorClient` wraps the integration service's `IAerospikeClient` + `EventLoops` and is NEVER closed by the twin (its `close()` would close the shared sync client). The integration service remains the SOLE owner of the client + event loops. Documented at the build site.

## 8. Testing
- Non-gated unit tests (Mockito): each accessor (mock `AerospikeReactorClient`/`IAerospikeClient`), acquisition (autoStart=false, spy for the eager reactor-client build), the metrics bridge (feed a mock `Cluster` snapshot → assert meters registered / `getCompositeMeterRegistry()` consulted when enabled, `never()` when disabled), config default/round-trip.
- Gated IT (`-DAerospikeClientServiceTests.enabled=true`): reactive `put` → `get` round-trip against a live Aerospike (Docker scripts in `infra/docker/aerospike/` if present) → tracked in **J-018**.
- Build note: use the `install` goal for `-am` reactive-module builds (bnd Export-Package on the new `.reactive` package).

## 9. Dependencies (gate)
- Root depMgmt: bump `<aerospike-client.version>` 4.4.13 → 7.1.0; add managed `com.aerospike:aerospike-reactor-client` at `${aerospike-client.version}`. Keep `reactor.version=3.6.8` (overrides the reactor-client's transitive reactor-core baseline).
- Module `appbase-client-aerospike/pom.xml`: add version-less `aerospike-reactor-client`; Export-Package the new `.reactive` package.
- Task 1 build-verifies the existing (sync) code compiles + tests green against 7.1.0 (expected contained — 0 call-site edits).

## 10. Decision documentation (Decision E)
Every non-obvious choice gets a Javadoc/inline rationale at the code site: the reactor-client non-owning-wrapper / never-close rule (double-close hazard), why `exists` returns a `Key` not a boolean on the reactive face, the driver bump to 7.x being required for `MetricsListener`, the metrics-listener push-bridge + composite-never-shut-down, and any name chosen to avoid a base-method collision.

## 11. Task breakdown (one PR per task — finalized in the plan)
1. **Driver bump** `aerospike-client` 4.4.13→7.1.0 + `aerospike-reactor-client:7.1.0` (root depMgmt + module pom; build-verify existing code green). Prerequisite PR.
2. `reactiveOperationConfiguration` on the Aerospike client-service config.
3. `ReactiveAerospikeAccessor` (reuse `AerospikeAccessorException`) — get/put/delete/exists/operate/query + escape hatches; reactor client wired (non-owning).
4. `getReactiveAccessor()` acquisition (+ keyed), reactor-client build/cache.
5. Sync `AerospikeAccessor` + `getAccessor()` acquisition.
6. Driver-native metrics — `MetricsListener`→Micrometer bridge, config-gated (`driverMetricsEnabled`).
7. Opt-in IT + build-guide docs + J-018 tracking.

## 12. Spec coverage self-check
Decisions A–E map to §3/§4/§6/§7/§10 and Tasks 1–7: A→Task 1; B→§4/Tasks 3–5; C→§6/Task 6; D→§7 (Tasks 3/4); E→§10 (all tasks). Dependencies gated in §9/Task 1. Testing §8. Existing `AbstractAerospikeAccessor` + `AerospikeAccessorException` reused/untouched. No unresolved TBDs.
