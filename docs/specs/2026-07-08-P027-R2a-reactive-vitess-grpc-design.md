# P-027 R2a — Vitess Tier-1 gRPC (object query) reactive+sync twin — Design

**Status:** approved (2026-07-08) · **Epic:** `feature/P-027-reactive-appbase` · **Umbrella:** `docs/specs/2026-07-04-P027-reactive-appbase-design.md`

The last R2a-in-repo driver twin (10th of 10). Adds reactive + synchronous service-acquired accessor faces to `appbase-client-vitess`, on the shared Appbase accessor spine, over the Vitess Java client's vtgate gRPC connections. **Distinct from R2b's R2DBC/JDBC-Vitess** — this is the Tier-1 `appbase-client-vitess` gRPC path (`VTGateConnection`/`VTGateBlockingConnection`), not SQL-over-JDBC.

## Context (verified against the code)

- `VitessGrpcClientService extends AppbaseClientService<VitessGrpcClient, ClusterIdValue>`; key type `ClusterIdValue` (uniform with S3/Cassandra).
- `VitessGrpcClient` exposes **both** connections: `getVtGateConnection()` (async `VTGateConnection`) and `getVtGateBlockingConnection()` (blocking `VTGateBlockingConnection`), plus `getDefaultContext()` and `getVtSession()`.
- `vitess-client` 17.0.0 (root `<dependencyManagement>` `vitess.version`). Async surface:
  - `VTGateConnection.execute(Context, String sql, Map<String,?> bindVars, VTSession) → SQLFuture<Cursor>` **throws `SQLException`**.
  - `executeBatch(Context, List<String>, List<Map<String,?>>, VTSession) → SQLFuture<List<CursorWithError>>` **throws `SQLException`**.
  - `streamExecute(Context, String, Map<String,?>, VTSession) → Cursor` (a **blocking** streaming cursor) **throws `SQLException`**.
- `SQLFuture<V> extends com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture<V>` — a **Guava `ListenableFuture`** (NOT a `CompletionStage`/`CompletableFuture`); `checkedGet()` throws `SQLException`.
- `Cursor implements AutoCloseable`; `Cursor.next() → Row` (null at end) throws `SQLException`.
- `VTSession` carries mutable session/transaction state — **not safe to share across concurrent ops**.
- Shared spine (all prior twins): `AbstractReactiveAccessor(AppbaseCore, ReactorService, ReactiveOperationConfiguration)` + `mono`/`flux`; `AbstractAppbaseAccessor(AppbaseCore, metricsEnabled, traceEnabled, exceptionStoreEnabled)` + `execute`. `DefaultReactiveOperationConfiguration` in `…client.core.reactive`.

## Decisions

### A. Op scope — async point queries + Tier-1 streaming (both faces)
The twin covers the full driver vocabulary: point queries (`execute`, `executeBatch`) on the **genuinely async** `VTGateConnection`, plus streaming (`streamExecute`) which is a **blocking** cursor iterator and is the only true Tier-1 (scheduler-offload) path. This keeps the twin uniform with the twins that expose a `Flux` (Cassandra/ES/Kafka) rather than dropping a capability the driver has. The umbrella spec's blanket "Tier-1" label for `appbase-client-vitess` is refined here: point queries have a real async face (better than offload); only streaming is offload-Tier-1.

### B. Dual-client split (S3-shaped) — sync ≠ non-blocking
Reactive face uses the async `VTGateConnection`; sync face uses the blocking `VTGateBlockingConnection`. The sync accessor never blocks the async connection (blocking an async completion still ties up a thread for the call's duration). Documented at the code site, mirroring the S3 twin's `S3AsyncClient`/`S3Client` split.

### C. Reactor bridge — Firebase-shaped (`Mono.create` + Guava `Futures.addCallback`)
`SQLFuture` is a Guava `ListenableFuture`, so neither `Mono.fromFuture` (jasync/ES/Kafka/S3, `CompletableFuture`) nor `Mono.fromCompletionStage` (ZooKeeper) applies — it is the same class of bridge as Firebase's `ApiFuture`:

```java
Mono.create(sink -> {
    SQLFuture<T> f = asyncCommand.apply(conn, ctx, session);   // throws SQLException
    Futures.addCallback(f, new FutureCallback<T>() {
        public void onSuccess(T v) { if (v == null) sink.success(); else sink.success(v); }
        public void onFailure(Throwable t) { sink.error(t); }
    }, MoreExecutors.directExecutor());
    sink.onCancel(() -> f.cancel(false));
})
```

wrapped in `Mono.usingWhen(Mono.fromSupplier(borrow), svc -> …, svc -> Mono.fromRunnable(svc::finalizeOperation))`. `com.google.common.util.concurrent.{Futures, FutureCallback, MoreExecutors, ListenableFuture}` are transitive via the vitess client — **no new dependency**. `onSuccess(null) → sink.success()` (empty), never `sink.success(null)` (NPEs). `directExecutor()` runs the callback on the gRPC completion thread straight into the thread-safe `MonoSink` — no scheduler re-subscription hazard.

### D. Two throwing functional interfaces (new wrinkle vs all prior twins)
Vitess methods declare `throws SQLException` **at call construction** — so even the *reactive* generic command cannot take a plain `java.util.function.Function` (all prior twins' async command did; only the *sync* commands of ZooKeeper/Firebase needed a throwing interface). Both faces get one:

```java
@FunctionalInterface public interface VitessAsyncCommand<T> {
    SQLFuture<T> apply(VTGateConnection conn, Context ctx, VTSession session) throws SQLException;
}
@FunctionalInterface public interface VitessCommand<T> {
    T apply(VTGateBlockingConnection conn, Context ctx, VTSession session) throws SQLException;
}
```

The reactive `call` helper catches the synchronous `SQLException` thrown by `apply` and routes it through `sink.error` (so it maps like any other failure). Lesson generalised: **the throwing-command decision is per-method-signature, not per-face** — check whether the driver call itself throws checked, regardless of whether it returns a future.

### E. Session/Context handling — convenience + explicit overloads
Every op needs a `Context` + `VTSession`, and `VTSession` is not concurrency-safe. Each op has two forms:
- **Convenience:** `execute(op, sql, bindVars)` borrows the client's default `Context` and builds a **fresh per-op autocommit `VTSession`** from the client's session config (target / tablet-type / workload / execute-options via the module's `VtSessionConfiguration`/`VitessGrpcClientUtil`). No shared mutable state → concurrency-safe for reads and autocommit writes.
- **Explicit:** `execute(op, ctx, session, sql, bindVars)` takes a caller-owned `Context`+`VTSession` for cross-op transactions or a pinned tablet.
- **Generic `command`** for full control.

Javadoc states: cross-op transactions require the explicit form with a caller-owned session; the convenience form is autocommit-per-op. Transaction *semantics* are out of R2a scope (this twin is about the non-blocking I/O face, not tx management).

### F. Op surface

| op | reactive (`VTGateConnection`) | sync (`VTGateBlockingConnection`) |
|---|---|---|
| `execute(op, sql, bindVars)` + `execute(op, ctx, session, sql, bindVars)` | `Mono<Cursor>` | `Cursor` |
| `executeBatch(op, sqlList, bindVarsList)` + explicit overload | `Mono<List<CursorWithError>>` | `List<CursorWithError>` |
| `streamExecute(op, sql, bindVars)` + explicit overload | `Flux<Row>` **(Tier-1 offload)** | `Cursor` |
| `command` | `Mono<T>` over `VitessAsyncCommand` | `T` over `VitessCommand` |

- `bindVars` = `Map<String,?>` (nullable → no bind vars). `sql`/`sqlList` non-blank.
- `execute` → `Mono<Cursor>` returns the raw driver cursor (a point-query cursor is fully buffered, so the caller drains it in-memory with no blocking I/O). Streaming stays exclusively on `streamExecute`.
- **`streamExecute` Flux (the only genuine Tier-1 path):** `streamExecute` returns a blocking `Cursor`; drain it under `Flux.using`, published on the reactor-service offload scheduler, closing the cursor on any termination:

```java
flux(op, () -> Mono.usingWhen(Mono.fromSupplier(borrow),
    svc -> Mono.just(svc), svc -> Mono.fromRunnable(svc::finalizeOperation))
  .flatMapMany(svc -> Flux.using(
      () -> conn(svc).streamExecute(ctx, sql, bindVars, session),   // throws SQLException
      cursor -> Flux.<Row>generate(sink -> {
          try { Row r = cursor.next(); if (r == null) sink.complete(); else sink.next(r); }
          catch (SQLException e) { sink.error(e); }
      }),
      Cursor::close)
    .subscribeOn(offloadScheduler)));
```

Javadoc documents that backpressure is bounded by the offload scheduler and that this path is Tier-1 (blocking iterator), unlike the async point path. (Exact borrow/close composition finalised in the plan; the resource/close and offload semantics are as above.)

### G. Exception mapping
`VitessAccessorException extends AppbaseClientException` — ctors `(Throwable)` / `(String, Throwable)`; static `map(Throwable, Function<Throwable,Throwable> fallback)` unwrapping `CompletionException`/`ExecutionException` → cause, then `cause instanceof java.sql.SQLException` (Vitess surfaces failures as `SQLException` via `SQLFuture.checkedGet`/`mapException`) → `new VitessAccessorException(cause)`, else `fallback`. Shared verbatim by both faces (mirrors `JasyncAccessorException.map` / `S3AccessorException.map`). `mapError` = `VitessAccessorException.map(t, super::mapError)`; `isNotable` = `true`.

### H. Metrics
Spine `Timer` only (like Elasticsearch/Firebase). The vitess client exposes nothing cheaply gaugeable; **no driver-metrics task**.

### I. Acquisition (Cassandra/S3-shaped, keyed-free)
`ReactiveVitessGrpcAccessor getReactiveAccessor()` + `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` and `VitessGrpcAccessor getAccessor()` + keyed, on `VitessGrpcClientService`. Impl mirrors `DefaultS3ClientsService`/`DefaultCassandraClientService`: `appbaseClientCore.getAppbaseCore()`, `.getReactorService()` (reactive only), `getConfiguration().getReactiveOperationConfiguration()`, borrow `this::getClientIntegrationService` / `() -> getClientIntegrationService(key)`. No cache; no shutdown change.

## Components (each independently testable)

- `accessor/VitessAccessorException.java` — the mapper (static `map` + ctors).
- `accessor/VitessCommand.java`, `accessor/reactive/VitessAsyncCommand.java` — the two throwing functional interfaces.
- `accessor/reactive/ReactiveVitessGrpcAccessor.java` — extends `AbstractReactiveAccessor`; the `SQLFuture→Mono` bridge, the `streamExecute` Tier-1 `Flux`, the op set, `mapError`/`isNotable`, fresh-session helper.
- `accessor/VitessGrpcAccessor.java` — extends `AbstractAppbaseAccessor`; blocking op set over `VTGateBlockingConnection`, `run` helper (borrow → try/finally finalize), same `mapError`/`isNotable`.
- `configuration/{VitessGrpcClientServiceConfiguration,DefaultVitessGrpcClientServiceConfiguration}` — add `ReactiveOperationConfiguration getReactiveOperationConfiguration()` (abstraction getter + covariant impl).
- `{VitessGrpcClientService,DefaultVitessGrpcClientService}` — the four acquisition getters.
- Tests: `VitessAccessorExceptionTest`, `ReactiveVitessGrpcAccessorTest`, `VitessGrpcAccessorTest`, acquisition cases in a `DefaultVitessGrpcClientServiceReactiveTest`, gated `accessor/reactive/ReactiveVitessGrpcAccessorIT`.

## Error handling

- Reactive: driver `SQLException` thrown at call construction → caught, `sink.error`; async failure → `onFailure` → `sink.error`; both mapped by `mapError` → `VitessAccessorException`. `finalizeOperation` on complete/error/cancel via `usingWhen`. `streamExecute` cursor closed via `Flux.using` on any termination.
- Sync: `run` wraps borrow in try/finally `finalizeOperation`; `SQLException` mapped identically.
- `command` (both faces): same mapping; caller owns any explicit session.

## Testing

- **Unit:** mock `VitessGrpcClient` → `VTGateConnection`/`VTGateBlockingConnection`; stub `execute` → a real `SQLFuture` wrapping Guava `Futures.immediateFuture(cursor)` / `immediateFailedFuture(new SQLException(...))`; StepVerifier asserts the cursor emitted, `finalizeOperation` on success AND error, `SQLException`→`VitessAccessorException`, spine `Timer` recorded (reuse the `CoreFixtures` pattern). `streamExecute` — a fake `Cursor` yielding N rows then `null`; assert the `Flux` emits N `Row`s and the cursor is closed. Sync mirror over `VTGateBlockingConnection`.
- **Gated IT** `ReactiveVitessGrpcAccessorIT` — reuses the module's existing `-DVitessClientServiceTests.enabled` gate (confirm the exact property from the module's existing tests) + `VitessGrpcClientServiceTestHelper` against vtgate on `:15306` (the pre-existing J-018 (a) infra). Reactive + sync round-trip: `CREATE TABLE` → `INSERT` → `execute SELECT` (assert the row) → `streamExecute` (assert streamed rows) → `DROP`. `@AfterAll` best-effort cleanup. Named `*IT`; run with both the gate and `-Dtest=ReactiveVitessGrpcAccessorIT` if the module has no failsafe (confirm).

## Constraints (binding)

- Every Java file: Apache header, Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`; **no FQCNs** (imports only). Document at the code site: the async/blocking connection split, the `SQLFuture`→`Mono` (Guava `ListenableFuture`) bridge, the throwing-command reason, the Tier-1 offload of `streamExecute`, the fresh-vs-explicit session model.
- Dependency versions only in root `<dependencyManagement>` (reactor already managed; vitess already managed). Add version-less `reactor-test` (test) + Export-Package `…vitess.accessor.reactive` to the module pom if absent.
- Build with `install` (bnd Export-Package on `.reactive` fails under `test`), scoped: `mvn -q -pl appbase-java/appbase-client/appbase-client-vitess -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`. After each build: ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): file and plugin deleted; no post-build cleanup needed.**.
- Gated IT → J-018 (new clause (m)). Learnings → `appbase-java/LEARNING.md`.

## Delivery (5 tasks / 2 PRs — ES/S3 shape)

New sub-issue off the epic. Each task: TDD (Red→Green), fresh implementer subagent → task reviewer.

- **PR 1** `feature/P-027-R2a-vitess-t1t3-reactive`: **T1** reactive-config getter on `VitessGrpcClientServiceConfiguration` (+ covariant impl, config round-trip test). **T2** `VitessAccessorException` + `VitessAsyncCommand` + `ReactiveVitessGrpcAccessor` (bridge + `streamExecute` Flux + ops). **T3** `getReactiveAccessor()`/keyed.
- **PR 2** `feature/P-027-R2a-vitess-t4t5-sync-it` (off the epic after PR 1 merges): **T4** `VitessCommand` + sync `VitessGrpcAccessor` + `getAccessor()`/keyed. **T5** gated `ReactiveVitessGrpcAccessorIT` + build-guide "Reactive Vitess (gRPC)" section + J-018 clause (m) on develop at roll-up.
- **Post-merge:** roll the twin up into the epic; append learnings to `LEARNING.md` (the `SQLFuture`/Guava-`ListenableFuture` bridge; the async-command-throws-checked wrinkle; the Tier-1 `streamExecute` offload; fresh-vs-explicit `VTSession`) + bump the twin-set summary to **10 of 10** and the J-018 summary to (c)–(m). **This closes R2a-in-repo.**

## Self-review notes

- Decisions A–I mapped: A→T2 (op scope) · B→T2/T4 (dual client) · C→T2 (bridge) · D→T2/T4 (throwing ifaces) · E→T2/T4 (session overloads) · F→T2/T4 (op surface) · G→T2 (exception) · H→no metrics task · I→T3/T4 (acquisition). Types: reactive over `VTGateConnection` (`SQLFuture`), sync over `VTGateBlockingConnection` (`Cursor`); key `ClusterIdValue`; `SQLException` base for `map`. No new dependency (Guava + vitess transitive).
