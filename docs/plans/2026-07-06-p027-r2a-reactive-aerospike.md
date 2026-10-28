# P-027 R2a — Reactive Aerospike Twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reactive + synchronous **service-acquired** accessor faces to the Aerospike client service on `aerospike-reactor-client`, on the shared Appbase accessor spine, with Aerospike's driver-native metrics routed into the composite/JMX — bumping the Aerospike client to 7.1.0 (matched reactor client + `MetricsListener` API).

**Architecture:** In-module (`appbase-client-aerospike`). The existing user-extended `AbstractAerospikeAccessor` (bespoke timers, not service-acquired) stays UNTOUCHED. Two NEW service-acquired concrete accessors are added: `ReactiveAerospikeAccessor` over `AerospikeReactorClient` and sync `AerospikeAccessor` over `IAerospikeClient`, both on the shared spine. Aerospike ops are DISCRETE bounded ops → both faces use the FULL `mono`/`flux` / `execute` spine (no lighter-spine variant, unlike Kafka's long-lived `receive`). The `AerospikeReactorClient` is a NON-OWNING wrapper of the integration service's `IAerospikeClient` + `EventLoops` — built once, cached per key, and NEVER closed (its `close()` would close the shared sync client).

**Tech Stack:** Java 25, Project Reactor 3.6.8, `aerospike-client` 7.1.0, `aerospike-reactor-client` 7.1.0, Micrometer (`MetricService` composite → JMX), Guice, Maven, JUnit 5 + Mockito + `reactor-test` StepVerifier.

**Spec:** `docs/specs/2026-07-06-P027-R2a-reactive-aerospike-design.md`
**Reference twins (mirror these exactly):** merged Redis (`appbase-client-redis/.../reactive/ReactiveRedisAccessor.java`, `RedisAccessor.java`, `RedisAccessorException.java`, `DefaultRedisClientService` acquisition), Cassandra (`appbase-client-cassandra/.../reactive/ReactiveCassandraAccessor.java`, `CassandraAccessor.java`, `DefaultCassandraClientService.getReactiveAccessor`/`getAccessor`, `DefaultCassandraClientServiceReactiveTest.java` — closest mirror: discrete ops full spine + driver metrics), and Kafka (config `reactiveOperationConfiguration` pattern, `driverMetricsEnabled` gate).

## Global Constraints

- **Branch/PR (NESTED twin branch):** one PR PER TASK, branch `feature/P-027-R2a-aerospike-t<N>-<slug>`. **Task 1 (driver bump)** merges FLAT into `feature/P-027-reactive-appbase` as a prerequisite exception (mirror Kafka internal work item 295); from Task 2 on, PRs target the **twin branch** `feature/P-027-R2a-aerospike` (created off reactive HEAD once Task 1 merges). When all Aerospike tasks are merged, open `feature/P-027-R2a-aerospike` → `feature/P-027-reactive-appbase` (closes the Aerospike twin issue). STOP for human merge before each next task. **Twin-level GitHub sub-issue only** (created at execution, child of reactive epic internal work item 277): post each task PR link there in sequence + tick its checkbox.
- **Every Java file:** Apache license header (copy verbatim from a sibling in the module), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`. No fully-qualified class names in code (use imports) — sole exception: string constants naming classes.
- **Decisions documented in code (Decision E):** every non-obvious choice gets a Javadoc/inline rationale AT THE CODE SITE — the reactor-client non-owning-wrapper / never-close rule (double-close hazard), why reactive `exists` returns a `Key` not a boolean, why the driver bump to 7.x is required (`MetricsListener` is 7.0+), the metrics-listener push-bridge + composite-never-shut-down, and any name chosen to avoid a base-method collision.
- **Dependency versions ONLY in root `pom.xml` `<dependencyManagement>`**; module poms are version-less.
- **Build command** (module + deps; use `install`, NOT `test` — bnd Export-Package on `.reactive` fails under `test`):
  `cd /Users/nav/Viveworks/Technology/Appbase/appbase-java && mvn -q -pl appbase-client/appbase-client-aerospike -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`
  `-Dsurefire.failIfNoSpecifiedTests=false` is MANDATORY (else `-am` aborts at appbase-core). Scope a single class with `-Dtest=<Class>` (still `install`).
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.** (regenerated; never commit).
- **Generics/keys:** service-acquired accessors follow the module's existing key model (`AppbaseClientKey<ClusterIdValue>`); mirror Redis/Cassandra keyed-acquisition exactly.
- **Gated tests → J-018** (per standing policy). **Learnings → `appbase-java/LEARNING.md`** at the end.

---

## File Structure

**Root pom (`pom.xml`):**
- Modify `<dependencyManagement>` + `<aerospike-client.version>` — bump 4.4.13→7.1.0; add managed `com.aerospike:aerospike-reactor-client` at `${aerospike-client.version}`.

**Aerospike module (`.../service/aerospike/`):**
- Modify `configuration/AerospikeClientServiceConfiguration.java` (interface) + `configuration/DefaultAerospikeClientServiceConfiguration.java` — `reactiveOperationConfiguration`.
- Modify `configuration/AerospikeClientIntegrationServiceConfiguration.java` + `configuration/DefaultAerospikeClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled`.
- Modify `DefaultAerospikeClientIntegrationService.java` — expose `EventLoops` to the client service for the reactor-client build; enable/disable driver metrics (config-gated).
- Create `reactive/ReactiveAerospikeAccessor.java`.
- Create `AerospikeAccessor.java` (sync, service-acquired — distinct from the existing `accessor/AbstractAerospikeAccessor.java`).
- Modify `AerospikeClientService.java` (interface) + `DefaultAerospikeClientService.java` — `getReactiveAccessor()`/`getAccessor()` (+ keyed); reactor-client build+cache.
- Create `metrics/AppbaseAerospikeMetricsListener.java` — `MetricsListener`→Micrometer bridge.
- Reuse (UNTOUCHED): `accessor/AerospikeAccessorException.java`, `accessor/AbstractAerospikeAccessor.java`.

**Module pom (`appbase-client-aerospike/pom.xml`):** add version-less `aerospike-reactor-client`; add `reactor-test` (test); Export-Package `...service.aerospike.reactive` (+ `...metrics` if needed by consumers).

**Tests/IT + docs:** unit tests per accessor + acquisition + metrics seam + config; gated `ReactiveAerospikeAccessorIT`; `build-guide.md` "Reactive Aerospike" section; J-018 entry.

---

## Task 1: Driver bump — `aerospike-client` 4.4.13 → 7.1.0 + `aerospike-reactor-client` 7.1.0

**Branch:** `feature/P-027-R2a-aerospike-t1-driver-bump` → merges FLAT into `feature/P-027-reactive-appbase` (prerequisite).
**Files:** Modify root `pom.xml` (`<aerospike-client.version>` + `<dependencyManagement>`); modify `appbase-client-aerospike/pom.xml`.

**Interfaces:**
- Produces: `com.aerospike:aerospike-client:7.1.0` and `com.aerospike:aerospike-reactor-client:7.1.0` available to the module (reactor client version-less in the module pom, managed in root).

- [ ] **Step 1: Bump the pin.** Root `pom.xml`: `<aerospike-client.version>4.4.13</...>` → `7.1.0`.
- [ ] **Step 2: Manage the reactor client.** In root `<dependencyManagement>`, add `com.aerospike:aerospike-reactor-client` at `${aerospike-client.version}`. Keep `<reactor.version>3.6.8</...>` (overrides the reactor-client's transitive reactor-core 3.5.8 baseline — confirm no `reactor-core` appears with a hardcoded version).
- [ ] **Step 3: Module dep.** In `appbase-client-aerospike/pom.xml`, add version-less `com.aerospike:aerospike-reactor-client`.
- [ ] **Step 4: Full-reactor regression** (bump touches every Aerospike consumer of the pin):
  `cd appbase-java && mvn -q -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`
  Expected: BUILD SUCCESS; `appbase-client-aerospike` + `appbase-stacktest` green. **Known pre-existing exception:** the Vitess IT (J-018, needs `vtgate:15306`) fails environmentally — NOT caused by this bump; confirm the ONLY failure is Vitess and every Aerospike-touching module compiles + unit-tests green. Scope-verify with `-pl appbase-client/appbase-client-aerospike -am` if the full reactor is noisy.
- [ ] **Step 5: Call-site check.** Spec §3-A asserts 0 breakage (`AerospikeClient`/`ClientPolicy`/`IAerospikeClient`, `async.{EventLoops,NettyEventLoops}`, `WritePolicy`/`RecordExistsAction`, `ReadModeAP`/`ReadModeSC`/`Replica`/`AuthMode`). If ANY source-incompat surfaces, STOP and report — do not patch call sites silently; the bump is supposed to be contained.
- [ ] **Step 6: ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**, commit + PR** (`feat(P-027-R2a): bump aerospike-client to 7.1.0 + add aerospike-reactor-client`). STOP for merge. After merge, create the Aerospike twin sub-issue (child of internal work item 277) and branch `feature/P-027-R2a-aerospike` off reactive HEAD.

---

## Task 2: `reactiveOperationConfiguration` on the Aerospike client-service config

**Branch:** `feature/P-027-R2a-aerospike-t2-config`
**Files:**
- Modify: `configuration/AerospikeClientServiceConfiguration.java` (interface getter) + `configuration/DefaultAerospikeClientServiceConfiguration.java` (field/Builder/getter/equals/hashCode/toString).
- Test: `configuration/DefaultAerospikeClientServiceConfigurationTest.java`.

**Interfaces:**
- Produces: `ReactiveOperationConfiguration getReactiveOperationConfiguration()` returning a non-null concrete `DefaultReactiveOperationConfiguration` (default `DefaultReactiveOperationConfiguration.builder().build()`), mirroring `DefaultCassandraClientServiceConfiguration` exactly (concrete type for Jackson round-trip; `DEFAULT_REACTIVE_OPERATION_CONFIGURATION` constant; Builder default).

- [ ] **Step 1: Failing test** — assert `getReactiveOperationConfiguration()` defaults non-null with `metricsEnabled()==true`, and a YAML round-trip preserves an overridden `metricsEnabled(false)`. Mirror `DefaultCassandraClientServiceConfigurationTest`.
- [ ] **Step 2: Run — FAIL** (getter missing). `-Dtest=DefaultAerospikeClientServiceConfigurationTest`.
- [ ] **Step 3: Add the field** — mirror `DefaultCassandraClientServiceConfiguration` (field, `DEFAULT_REACTIVE_OPERATION_CONFIGURATION`, ctor-from-builder, interface + impl getter, Builder setter defaulting to the constant, equals/hashCode/toString). **Confirm `Builder.getThis()` returns `this`** (guard against the null-getThis bug found in Cassandra).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** build — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): reactive operation config on Aerospike client service config`). STOP.

---

## Task 3: Reactive accessor — `ReactiveAerospikeAccessor` (full spine, non-owning reactor client)

**Branch:** `feature/P-027-R2a-aerospike-t3-reactive-accessor`
**Files:**
- Create: `reactive/ReactiveAerospikeAccessor.java`.
- Modify: `appbase-client-aerospike/pom.xml` — add `reactor-test` (test, version-less), Export-Package `...service.aerospike.reactive`.
- Test: `reactive/ReactiveAerospikeAccessorTest.java`.

**Interfaces:**
- Consumes: `AbstractReactiveAccessor` (`mono`/`flux`), `AerospikeAccessorException.map`, an injected `AerospikeReactorClient`.
- Produces:
  - `public class ReactiveAerospikeAccessor extends AbstractReactiveAccessor`
  - ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, AerospikeReactorClient reactorClient)` — reactor client injected (built by the service; injectable for tests). Calls `super(appbaseCore, reactorService, configuration)`.
  - `public Mono<KeyRecord> get(String op, Key key)` / `get(String op, Policy policy, Key key)` — `mono(op, () -> reactorClient.get(...))`.
  - `public Mono<Key> put(String op, Key key, Bin... bins)` / `put(String op, WritePolicy policy, Key key, Bin... bins)`.
  - `public Mono<Key> delete(String op, Key key)` / `delete(String op, WritePolicy policy, Key key)`.
  - `public Mono<Key> exists(String op, Key key)` — reactor-client `exists` emits the `Key` iff the record exists. **Javadoc: why it returns `Key` not `boolean`** (the reactor client's contract — absence = empty `Mono`).
  - `public Mono<KeyRecord> operate(String op, WritePolicy policy, Key key, Operation... ops)`.
  - `public Flux<KeyRecord> query(String op, Statement statement)` / `query(String op, QueryPolicy policy, Statement statement)` — `flux(op, () -> reactorClient.query(...))`.
  - Escape hatches: `public <T> Mono<T> command(String op, Function<AerospikeReactorClient, Mono<T>> fn)`, `public <T> Flux<T> queryMany(String op, Function<AerospikeReactorClient, Flux<T>> fn)`, `public AerospikeReactorClient reactor()`.
  - `@Override protected Throwable mapError(Throwable t)` = `AerospikeAccessorException.map(t, super::mapError)`; `@Override protected boolean isNotable(Throwable m)` = `true`.
  - **Javadoc: the injected `AerospikeReactorClient` is a NON-OWNING wrapper — this accessor NEVER closes it** (Decision D; its `close()` closes the shared `IAerospikeClient`).

- [ ] **Step 1: Failing test** — mock `AerospikeReactorClient`; stub `get(...)` → `Mono.just(keyRecord)`; assert `accessor.get("get", key)` emits it (StepVerifier), an `AerospikeException` on the stream maps to `AerospikeAccessorException`, `exists` on a missing record completes empty, and a metrics `Timer` is recorded (mock `MetricService`, mirror `ReactiveCassandraAccessorTest`). Non-gated.
- [ ] **Step 2: Run — FAIL.** `-Dtest=ReactiveAerospikeAccessorTest`.
- [ ] **Step 3: Implement** the accessor (stubs above). Confirm each `AerospikeReactorClient` method return type against the 7.1.0 jar (`get`→`Mono<KeyRecord>`, `put`/`delete`/`exists`→`Mono<Key>`, `operate`→`Mono<KeyRecord>`, `query`→`Flux<KeyRecord>`).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): ReactiveAerospikeAccessor on shared spine (non-owning reactor client)`). STOP.

---

## Task 4: Reactive acquisition — `getReactiveAccessor()` + reactor-client build/cache

**Branch:** `feature/P-027-R2a-aerospike-t4-reactive-acquisition`
**Files:**
- Modify: `AerospikeClientService.java` (interface) + `DefaultAerospikeClientService.java` (impl) — `getReactiveAccessor()` (+ keyed).
- Modify: `DefaultAerospikeClientIntegrationService.java` — expose the `EventLoops` it already builds (package-private/interface getter) so the client service can construct the reactor client.
- Test: `DefaultAerospikeClientServiceReactiveTest.java`.

**Interfaces:**
- Consumes: `getClientIntegrationService().getClient()` (the `IAerospikeClient`) + the integration service's `EventLoops`, `getReactorService()`, `getConfiguration().getReactiveOperationConfiguration()`.
- Produces:
  - `AerospikeClientService`: `ReactiveAerospikeAccessor getReactiveAccessor()` + `getReactiveAccessor(AppbaseClientKey<ClusterIdValue> key)`.
  - `DefaultAerospikeClientService`: builds an `AerospikeReactorClient(iAerospikeClient, eventLoops)` **once, cached per key** (`ConcurrentMap`), and returns a `ReactiveAerospikeAccessor` wrapping it. Mirror `DefaultCassandraClientService.getReactiveAccessor`.
  - **NEVER closes the cached `AerospikeReactorClient`** on `shutdownImpl` — the integration service remains the SOLE owner of the client + event loops. `shutdownImpl` clears the cache only (do NOT iterate-and-close). **Javadoc: the double-close hazard** (Decision D/§7).
- Produces (for Task 6): the reactor-client build site + integration-service start/shutdown hooks where driver metrics attach.

- [ ] **Step 1: Failing test** — `autoStart(false)`, spy/mocked integration service exposing a mock `IAerospikeClient` + `EventLoops`; assert `getReactiveAccessor()` non-null, reactive-config passthrough, the reactor client is built once + cached (second call returns same underlying), and `shutdown()` does NOT call `AerospikeReactorClient.close()` (verify `never()`). Mirror `DefaultCassandraClientServiceReactiveTest`. Non-gated.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** the getter, `EventLoops` exposure, build+cache. Confirm the `AerospikeReactorClient(IAerospikeClient, EventLoops)` ctor signature against the 7.1.0 jar.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): getReactiveAccessor acquisition + cached non-owning reactor client`). STOP.

---

## Task 5: Sync accessor — `AerospikeAccessor` + `getAccessor()` acquisition

**Branch:** `feature/P-027-R2a-aerospike-t5-sync-accessor`
**Files:**
- Create: `AerospikeAccessor.java` (service-acquired, distinct from `accessor/AbstractAerospikeAccessor.java`).
- Modify: `AerospikeClientService.java` + `DefaultAerospikeClientService.java` — `getAccessor()` (+ keyed).
- Test: `AerospikeAccessorTest.java`; acquisition assertions in `DefaultAerospikeClientServiceReactiveTest.java`.

**Interfaces:**
- Consumes: `AbstractAppbaseAccessor` (`execute`), `AerospikeAccessorException.map`, `getClientIntegrationService().getClient()` (thread-safe singleton — **no per-op pooled borrow/return**; document why).
- Produces:
  - `public class AerospikeAccessor extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration, IAerospikeClient client)` (or a `Supplier` mirroring the module's other service-acquired accessors; follow Redis's ctor shape).
  - `public Record get(String op, Key key) throws Exception` / `get(String op, Policy policy, Key key)`.
  - `public void put(String op, Key key, Bin... bins) throws Exception` / `put(String op, WritePolicy policy, Key key, Bin... bins)`.
  - `public boolean delete(String op, Key key) throws Exception` / `delete(String op, WritePolicy policy, Key key)`.
  - `public boolean exists(String op, Key key) throws Exception`.
  - `public Record operate(String op, WritePolicy policy, Key key, Operation... ops) throws Exception`.
  - `public <T> T command(String op, Function<IAerospikeClient, T> fn) throws Exception`; `public IAerospikeClient client()`.
  - `mapError` = `AerospikeAccessorException.map(t, super::mapError)`; `isNotable` = true.
  - On the service: `AerospikeAccessor getAccessor()` + keyed — returns an accessor over the borrowed singleton client. Mirror `DefaultCassandraClientService.getAccessor`.
  - **Javadoc: the Aerospike client is a thread-safe singleton — no pooled borrow/return** (unlike Kafka's pooled producer/consumer).

- [ ] **Step 1: Failing tests** — `AerospikeAccessorTest`: mock `IAerospikeClient`; assert `get`/`put`/`delete`/`exists`/`operate` delegate correctly, `finalizeOperation` runs on success AND error, `AerospikeException`→`AerospikeAccessorException`, `Timer` recorded (mirror `CassandraAccessorTest`). Acquisition: `autoStart(false)`, `getAccessor()` non-null + config passthrough.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** accessor + acquisition (stubs above).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): sync AerospikeAccessor + getAccessor acquisition`). STOP.

---

## Task 6: Driver-native metrics — `MetricsListener`→Micrometer bridge (config-gated)

**Branch:** `feature/P-027-R2a-aerospike-t6-driver-metrics`
**Files:**
- Modify: `configuration/AerospikeClientIntegrationServiceConfiguration.java` + `configuration/DefaultAerospikeClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled` (default true).
- Create: `metrics/AppbaseAerospikeMetricsListener.java` (implements `com.aerospike.client.metrics.MetricsListener`).
- Modify: `DefaultAerospikeClientIntegrationService.java` — on start, if `driverMetricsEnabled`, `client.enableMetrics(new MetricsPolicy().setListener(listener))`; on `shutdownImpl`, `client.disableMetrics()`.
- Test: `configuration/DefaultAerospikeClientIntegrationServiceConfigurationTest.java` (default/round-trip); `metrics/AppbaseAerospikeMetricsListenerTest.java`; enable/disable seam test on the integration service.

**Interfaces:**
- Produces:
  - `boolean isDriverMetricsEnabled()` on the integration config (default true; mirror Kafka's `driverMetricsEnabled`).
  - `public class AppbaseAerospikeMetricsListener implements MetricsListener` — ctor `(MeterRegistry compositeRegistry)`. `onEnable(Cluster, MetricsPolicy)` / `onSnapshot(Cluster)` walks `cluster.getNodes()` → per-node, per-`LatencyType` (CONN/WRITE/READ/BATCH/QUERY) registers/updates gauges from the exponential latency buckets + `errorCount`/`timeoutCount`/connection-stat counters, tagged by node + type; `onNodeClose(Node)` / `onDisable(Cluster)` no-ops (or deregister). **NEVER shuts down the composite registry** — `MetricService`-owned; `disableMetrics()` stops the listener. Document the push-bridge rationale (set `MetricsPolicy.listener` to bypass the default file writer).
- Consumes: `MetricService.getCompositeMeterRegistry()` (→ JMX).

- [ ] **Step 1: Failing tests** — config default `isDriverMetricsEnabled()==true` + round-trip `false`; listener: feed a mock `Cluster` with a mock `Node`/latency buckets → assert meters registered on the passed registry with node+type tags; integration seam: `enableMetrics` called when enabled, `never()` when disabled, `disableMetrics` on shutdown, composite registry NEVER `close()`d.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** config field, listener, enable/disable wiring. Confirm `MetricsListener` / `MetricsPolicy.setListener` / `Cluster.getNodes()` / `Node` latency-bucket API against the 7.1.0 jar.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): Aerospike driver-native metrics via MetricsListener bridge`). STOP.

---

## Task 7: Opt-in IT + build-guide docs + J-018 tracking

**Branch:** `feature/P-027-R2a-aerospike-t7-it-docs`
**Files:**
- Create: `reactive/ReactiveAerospikeAccessorIT.java` (gated).
- Modify: `appbase-java/build-guide.md` — "Reactive Aerospike (P-027 R2a)" section.
- Modify: J-018 tracking (in `Claude-Review.md`) — add the Aerospike gated IT clause.

**Interfaces:**
- Consumes: `getReactiveAccessor()`, `getAccessor()`, a live Aerospike (Docker under `infra/docker/aerospike/` if present).

- [ ] **Step 1: Gated IT** — `-DAerospikeClientServiceTests.enabled=true` (mirror the module's existing gate constant). Reactive `put(key,bin)` → `get(key)` round-trip via StepVerifier; assert the returned `Record`/`KeyRecord` bin value. Gated OFF by default (skips when the flag is absent).
- [ ] **Step 2: Run gated OFF** — confirm the IT is SKIPPED in a normal `install` (no live cluster needed).
- [ ] **Step 3: Docs** — add the "Reactive Aerospike" section to `build-guide.md` (accessor faces, non-owning reactor-client rule, metrics gate, gated-IT flag + how to run against Docker).
- [ ] **Step 4: J-018** — add the Aerospike gated IT as a new clause (mirror the Kafka clause (f)); note it needs a live Aerospike.
- [ ] **Step 5: Whole-module** — no regression (IT skipped).
- [ ] **Step 6: Commit + PR** (`test(P-027-R2a): gated reactive Aerospike IT + build-guide + J-018`). STOP.

---

## Twin completion (after Task 7 merges)

- Open `feature/P-027-R2a-aerospike` → `feature/P-027-reactive-appbase` (roll-up PR). Post it to the twin sub-issue; on merge, close the twin issue manually (GitHub auto-close only fires on merge to `develop`).
- Append Aerospike learnings to `appbase-java/LEARNING.md`.
- Do NOT merge the reactive epic → `develop` — that happens only when the WHOLE P-027 spec (all twins + web/file-I/O phases) is done.

---

## Self-Review

**1. Spec coverage:** A→Task 1; B→Tasks 3–5 (reactive + sync service-acquired accessors); C→Task 6 (MetricsListener bridge); D→Tasks 3/4 (non-owning, never-close, cache-clear-only); E→documented-in-code across all tasks. §5 config→Tasks 2 (reactiveOperationConfiguration) + 6 (driverMetricsEnabled). §8 testing→unit each task + gated IT Task 7. §9 dependency gate→Task 1. Existing `AbstractAerospikeAccessor` + `AerospikeAccessorException` reused/untouched. ✅ No gaps.

**2. Placeholder scan:** No TBD/TODO/"handle edge cases" — each task carries exact files, signatures, and a RED→GREEN test cycle. Implementation bodies are intentionally left as stubs (per the project's plan-stub convention; reviewer verifies the PR). ✅

**3. Type consistency:** `ReactiveAerospikeAccessor` / `AerospikeAccessor` / `AerospikeReactorClient` / `IAerospikeClient` / `AerospikeAccessorException.map` / `getReactiveAccessor()` / `getAccessor()` / `getReactiveOperationConfiguration()` / `isDriverMetricsEnabled()` used consistently Tasks 1–7. Reactive `exists` returns `Mono<Key>`; sync `exists` returns `boolean` — divergence is intentional and Javadoc-documented (Task 3). ✅
