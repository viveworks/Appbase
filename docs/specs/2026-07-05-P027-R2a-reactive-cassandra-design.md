# P-027 R2a — Reactive Cassandra twin (design)

**Status:** Design for review. Second R2a client twin, replicating the proven Redis-twin pattern
(`2026-07-04-P027-R2a-reactive-redis-design.md`) on the shared `AbstractAppbaseAccessor` spine that
already shipped with the Redis twin. This doc captures only the **Cassandra-specific** decisions;
all cross-cutting rationale (why POJO accessors, shared spine, metrics→JMX, selective
ExceptionStore, config-gated tracing) is inherited from the Redis design and not repeated.

**Goal:** a reactive Cassandra twin on the DataStax driver's native reactive API, plus a sync
accessor with the same per-op metrics/trace/logging/exception-mapping, wired into the existing
`appbase-client-cassandra` module.

---

## 1. Codebase facts (verified)

- `CassandraClientService extends AppbaseClientService<CqlSession, ClusterIdValue>` — hands out the
  DataStax `CqlSession`; `getClientIntegrationService()` / `(key)` borrow from the pool exactly like
  Redis. `DefaultCassandraClientService extends AbstractAppbaseClientService<CqlSession, ClusterIdValue, …>`.
- **DataStax java-driver-core 4.9.0 already ships the reactive API** (no new dependency):
  `CqlSession.executeReactive(Statement)` → `com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet`,
  which is a **Reactive Streams `Publisher<ReactiveRow>`** — adapts to Reactor with `Flux.from(...)`.
  Sync counterpart: `session.execute(Statement)` → `ResultSet`.
- Driver exceptions extend `com.datastax.oss.driver.api.core.DriverException` (base of
  `AllNodesFailedException`, `QueryExecutionException`, `QueryValidationException`, request timeouts).
- Cross-cutting spine already exists from the Redis twin: `AbstractAppbaseAccessor`
  (`execute(op, Callable)`, metrics/trace/log/`mapError`/`isNotable`/`reportNotable`) and
  `AbstractReactiveAccessor` (named `mono(op,…)`/`flux(op,…)`), plus `MetricService.getCompositeMeterRegistry()`.

## 2. Cassandra-specific decisions

1. **In-module, no new dependency.** New sub-package `com.viveworks.appbase.client.service.cassandra.reactive`
   in `appbase-client-cassandra`. `executeReactive` is part of java-driver-core 4.9.0.
2. **Reactive result is naturally `Flux`-shaped.** `executeReactive` yields a stream of rows. The
   primary method is `flux`-shaped; a `mono`-shaped convenience returns the first row (or completes
   empty), built on the same source.
3. **Reactive→Reactor adapter.** `Flux.from(session.executeReactive(statement))` — the `ReactiveResultSet`
   is a Reactive Streams Publisher, so no manual bridging; it flows through the base `flux(op, …)`
   for scheduler/timeout/retry/backpressure/error-map/metrics/trace.
4. **`CassandraAccessorException extends AppbaseClientException`**; `mapError` maps DataStax
   `DriverException` → `CassandraAccessorException`, else `super.mapError`. A shared static
   `CassandraAccessorException.map(throwable, fallback)` used by both the reactive and sync accessors
   (mirrors `RedisAccessorException.map`).
5. **Pool borrow/return per operation via `usingWhen`** — identical to Redis: borrow the integration
   service, `getClient()` = `CqlSession`, run the statement, `finalizeOperation()` on
   success/error/cancel. (CqlSession is itself thread-safe and shared; the borrow keeps the pool
   accounting and lifecycle consistent with every other Appbase client.)
6. **Config:** add an optional `ReactiveOperationConfiguration` field to the Cassandra client-service
   config (default = R0 defaults), exactly as Redis Task 2.

## 3. Surface

`ReactiveCassandraAccessor extends AbstractReactiveAccessor`:
- `Flux<ReactiveRow> executeReactive(String op, Statement<?> statement)` — `flux(op, () -> usingWhen(borrow, svc -> Flux.from(svc.getClient().executeReactive(statement)), finalize))`.
- `Mono<ReactiveRow> executeReactiveFirst(String op, Statement<?> statement)` — `.next()` on the above (single-row/among-first).
- `<T> Flux<T> query(String op, Function<CqlSession, Publisher<T>> fn)` — general escape hatch running any driver reactive publisher through the spine + pool.
- `ReactiveResultSet reactive(CqlSession session, Statement<?> statement)` — raw escape hatch.
- overrides `mapError` (DataStax → `CassandraAccessorException`) / `isNotable`.

`CassandraAccessor extends AbstractAppbaseAccessor` (sync):
- `ResultSet execute(String op, Statement<?> statement)` — via base `execute(op, Callable)` borrowing the session.
- `<T> T command(String op, Function<CqlSession, T> fn)` — general sync escape hatch.
- same `mapError`/`isNotable`.

Acquisition on `CassandraClientService`/`DefaultCassandraClientService`: `getReactiveAccessor()` /
`getReactiveAccessor(key)` and `getAccessor()` / `getAccessor(key)`, wiring `AppbaseCore`,
`getReactorService()`, `getConfiguration().getReactiveOperationConfiguration()`, and the pool-borrow
supplier — mirrors Redis Tasks 4/5.

## 4. Decomposition → per-task PRs

Per the R1+ rule (one PR per task → `feature/P-027-reactive-appbase`; wait for human merge each):

1. **Driver bump 4.9.0 → 4.17.0 (prerequisite; own PR).** Bump `cassandra-client.version` (root depMgmt — covers `java-driver-core`, `java-driver-query-builder`, `java-driver-mapper-runtime`) from 4.9.0 to **4.17.0** (last `com.datastax.oss` 4.x; 4.18+ moved to `org.apache.cassandra` groupId — avoid). Required because 4.9.0 has **no Micrometer support** (Dropwizard-only, no injectable registry — Micrometer support landed in 4.13+). The reactive API (`executeReactive`) is unchanged across 4.4→4.17, so this is metrics-enabling, not reactive-enabling. Verify the Cassandra module compiles + unit tests green; note ITs are gated (J-018). Isolated dependency PR.
2. **Config** — `ReactiveOperationConfiguration` field on the Cassandra client config + tests.
3. **`ReactiveCassandraAccessor` + `CassandraAccessorException`** — `executeReactive`/`executeReactiveFirst`/`query`/`reactive` escape hatch, `usingWhen` pool borrow/return, DataStax error mapping + shared `.map()` helper; unit tests (mocked `CqlSession`/`ReactiveResultSet`).
4. **Acquisition** — `getReactiveAccessor()` (+ keyed) on the client service; non-gated unit test (config passthrough).
5. **Sync `CassandraAccessor`** — on the shared spine; unit tests; shares the DataStax `.map()` helper.
6. **DataStax-native driver metrics → composite/JMX** *(LOCKED in — not deferred).* On 4.17: add the add-on artifact `com.datastax.oss:java-driver-metrics-micrometer` (managed in root depMgmt; the implementer confirms the exact artifact/class online at exec time), set `advanced.metrics.factory.class = MicrometerMetricsFactory` in the driver config, enable the desired session/node metrics (`advanced.metrics.session.enabled` / `node.enabled`), and bind the session builder to Appbase's composite via `SessionBuilder.withMetricRegistry(appbaseCore.getMetricService().getCompositeMeterRegistry())`. Config-gated by a `commandLatencyMetricsEnabled`-style flag on the Cassandra integration config (default true), mirroring Redis Task 7. Wired in `AppbaseCassandraSessionBuilder` / the integration service's session build.
7. **Opt-in IT + docs** — `ReactiveCassandraAccessorIT` (gated like the existing Cassandra IT; reactive `CREATE`/`INSERT`/`SELECT` round-trip; assert a metric registered) + a `build-guide.md` Cassandra reactive subsection.

## 5. Testing

Unit (mocked, StepVerifier): `executeReactive` emits rows through the spine; pool `finalizeOperation()`
on success/error/cancel; DataStax `DriverException` → `CassandraAccessorException`; metrics recorded;
sync `execute` returns/maps/meters. IT: opt-in, gated like the existing Cassandra IT, reactive
round-trip against a real Cassandra (docker). (Note: like the Redis ITs, the Cassandra IT bootstrap
may share the same test-resource gap tracked in J-018 — verify; if so, note it, don't fix here.)

## 6. Resolved decisions

- **A. Driver-native metrics — LOCKED IN (not deferred).** Deliver DataStax metrics→composite/JMX.
  Since 4.9.0 has no Micrometer support, this requires the **driver bump to 4.17.0** (Task 1) so the
  4.13+ Micrometer factory + `withMetricRegistry` are available; the metrics wiring is Task 6.
- **B. No object mapping / entity layer — LOCKED.** The twin exposes driver-native `ReactiveRow`/
  `ResultSet`/`Statement` only. `java-driver-mapper-runtime` stays a dependency (bumped with the
  others) but the twin does not use it; entity mapping is out of scope.
- **C. Driver-bump blast radius.** 4.9.0 → 4.17.0 is 8 minor versions; the reactive + core query APIs
  are stable across the range, but config-loader defaults and a few internal-context hooks
  (`AppbaseCassandraDriverContext`, `AppbaseCassandraNettyOptions`) may need review. Task 1 verifies
  compile + unit tests; the (J-018-gated) IT is the deeper check.
