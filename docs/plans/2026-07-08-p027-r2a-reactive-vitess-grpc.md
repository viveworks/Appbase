# P-027 R2a — Vitess Tier-1 gRPC reactive+sync twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reactive + synchronous service-acquired accessor faces to `appbase-client-vitess`, on the shared Appbase accessor spine, over the Vitess Java client's vtgate gRPC connections — reactive via `SQLFuture`→`Mono` (Guava `ListenableFuture` bridge) over `VTGateConnection`, streaming via Tier-1 offload over the blocking `Cursor`, sync over `VTGateBlockingConnection`.

**Architecture:** S3-shaped dual-client split (reactive over the async connection, sync over the blocking one); Firebase-shaped future bridge (`SQLFuture` is a Guava `ListenableFuture`, not a `CompletionStage`); Cassandra/S3-shaped keyed-free acquisition. The only genuine Tier-1 (scheduler-offload) path is `streamExecute` (a blocking cursor iterator). This is the LAST R2a-in-repo twin (10th of 10); distinct from R2b's R2DBC/JDBC-Vitess.

**Tech Stack:** Java 25, Reactor 3.6.8, `vitess-client` 17.0.0 (`VTGateConnection`/`VTGateBlockingConnection`/`SQLFuture`/`Cursor`/`Row`/`VTSession`/`Context`), Guava (`Futures`/`FutureCallback`/`MoreExecutors` — transitive via vitess), Guice, Maven, JUnit 5 + Mockito + `reactor-test`.

**Reference twins (in the working tree / epic):** S3 (`ReactiveS3Accessor`/`S3Accessor` dual-client + per-op-arg ops + `S3AccessorException.map`), Firebase (`ReactiveFirebaseAuthAccessor` `Mono.create`+callback bridge over `ApiFuture`; `FirebaseAuthCommand` throwing interface), Cassandra (`DefaultCassandraClientService` keyed-free acquisition). **Spec:** `docs/specs/2026-07-08-P027-R2a-reactive-vitess-grpc-design.md`.

## Global Constraints

- **Branch/PR (max 3 tasks/PR):** twin sub-issue off the epic `feature/P-027-reactive-appbase`. **PR 1** = T1–3 on `feature/P-027-R2a-vitess-t1t3-reactive`; **PR 2** = T4–5 on `feature/P-027-R2a-vitess-t4t5-sync-it`; both target the epic. Roll up when both merge.
- **Every Java file:** Apache header (copy verbatim from an existing module file), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`; **no FQCNs** (imports only; class-name String constants excepted). Document at the code site: the async/blocking connection split, the `SQLFuture`→`Mono` (Guava `ListenableFuture`) bridge, the throwing-command reason, the Tier-1 offload of `streamExecute`, the fresh-vs-explicit session model.
- **Dependency versions only in root `<dependencyManagement>`** (reactor + vitess already managed). Add version-less `reactor-test` (test scope) to the module pom; add `com.viveworks.appbase.client.service.vitess.accessor.reactive` to the bnd `Export-Package` (currently only `…vitess` is exported).
- **Build (use `install`, NOT `test` — bnd Export-Package on `.reactive` fails under `test`):** `cd /Users/nav/Viveworks/Technology/Appbase && mvn -q -pl appbase-java/appbase-client/appbase-client-vitess -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`. Scope with `-Dtest=<Class>`.
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- **Key type:** `AppbaseClientKey<ClusterIdValue>` (like Cassandra/S3). **Gated tests → J-018 (new clause (m)). Learnings → `appbase-java/LEARNING.md`.**

## Established facts (verified)

- `VitessGrpcClientService extends AppbaseClientService<VitessGrpcClient, ClusterIdValue>`; `VitessGrpcClientIntegrationService` is the module's integration-service type (extends `AppbaseClientIntegrationService<VitessGrpcClient, ClusterIdValue>`); `DefaultVitessGrpcClientService.getClientIntegrationService()`/`(key)` return the vitess-specific integration service.
- `VitessGrpcClient` (interface): `getVtGateConnection()` (async `VTGateConnection`), `getVtGateBlockingConnection()` (blocking `VTGateBlockingConnection`), `getDefaultContext()` (`Context`), `getVtSession()` (default `VTSession`).
- Async `VTGateConnection` (17.0.0): `execute(Context, String, Map<String,?>, VTSession) → SQLFuture<Cursor>` throws `SQLException`; `executeBatch(Context, List<String>, List<Map<String,?>>, VTSession) → SQLFuture<List<CursorWithError>>` throws `SQLException`; `streamExecute(Context, String, Map<String,?>, VTSession) → Cursor` (blocking) throws `SQLException`. `VTGateBlockingConnection` mirrors these but returns the value directly (`Cursor` / `List<CursorWithError>` / `Cursor`), each `throws SQLException` (confirm exact signatures with `javap io.vitess.client.VTGateBlockingConnection` at T4).
- `SQLFuture<V> extends com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture<V>` — a Guava `ListenableFuture`; `checkedGet()` throws `SQLException`. Bridge with `com.google.common.util.concurrent.Futures.addCallback(future, FutureCallback, MoreExecutors.directExecutor())`.
- `Cursor implements AutoCloseable`; `Cursor.next() → Row` (null at end) throws `SQLException`.
- The fresh autocommit `VTSession` is already built by `DefaultVitessGrpcClientIntegrationService`'s **private** `vtSession()` (lines ~346–360): `new VTSession(VitessGrpcClientUtil.target(targetConfig), executeOptions)` + `setAutoCommit(...)` from `VtSessionConfiguration`. **T2 promotes this to a public `newVtSession()` on `VitessGrpcClientIntegrationService`** so the accessor can mint a fresh per-op session.
- `AbstractReactiveAccessor(AppbaseCore, ReactorService, ReactiveOperationConfiguration)` + `mono`/`flux`; `AbstractAppbaseAccessor(AppbaseCore, metricsEnabled, traceEnabled, exceptionStoreEnabled)` + `execute(String, Callable<T>)`; both read flags from `ReactiveOperationConfiguration` (`isMetricsEnabled()`/`isTraceEnabled()`/`isExceptionStoreEnabled()`). `DefaultReactiveOperationConfiguration` in `…client.core.reactive` (builder `.retryEnabled(false)`).
- Gate for existing live tests: `-DVitessClientServiceTests.enabled` (default false; used by `DefaultVitessGrpcClientServiceTest`, `VitessGrpcClientServiceGuiceModuleTest`). Bootstrap helpers: `VitessGrpcClientServiceTestHelper`, `VitessGrpcClientServiceTestUtil` (the latter already drives `vtGateConnection.execute(ctx, "SELECT …", null, vtSession)` → `SQLFuture<Cursor>`). Module has **no `maven-failsafe-plugin`** — an `*IT` needs `-Dtest=` to run.

---

## PR 1 — `feature/P-027-R2a-vitess-t1t3-reactive` (Tasks 1–3)

### Task 1: `reactiveOperationConfiguration` on `VitessGrpcClientServiceConfiguration`

**Files:**
- Modify: `…/vitess/configuration/VitessGrpcClientServiceConfiguration.java` (add abstraction getter)
- Modify: `…/vitess/configuration/DefaultVitessGrpcClientServiceConfiguration.java` (covariant impl: field + constant + ctor-notNull + Builder setter + `equals`/`hashCode`/`toString`)
- Test: `…/vitess/configuration/DefaultVitessGrpcClientServiceConfigurationTest.java`

**Interfaces:**
- Produces: `ReactiveOperationConfiguration getReactiveOperationConfiguration()` (interface); impl returns the concrete `DefaultReactiveOperationConfiguration` (covariant), non-null default `DefaultReactiveOperationConfiguration.builder().build()`.

- [ ] **Step 1: Write the failing test.** In `DefaultVitessGrpcClientServiceConfigurationTest`: (a) default config's `getReactiveOperationConfiguration()` non-null and `isMetricsEnabled() == true`; (b) a config built with `reactiveOperationConfiguration(DefaultReactiveOperationConfiguration.builder().metricsEnabled(false).build())` round-trips `isMetricsEnabled() == false`. Mirror `DefaultS3ClientsServiceConfigurationTest` / `DefaultCassandraClientServiceConfigurationTest`.
- [ ] **Step 2: Run — expect FAIL** (method/Builder setter missing). `mvn … -Dtest=DefaultVitessGrpcClientServiceConfigurationTest install` → compile error / red.
- [ ] **Step 3: Implement.** Add the abstraction getter to the interface; add the covariant field/constant/ctor-notNull/Builder-setter/`equals`/`hashCode`/`toString` to the Default impl (copy the shape from `DefaultCassandraClientServiceConfiguration`).
- [ ] **Step 4: Run — expect PASS** (both tests green).
- [ ] **Step 5: Whole-module build** (`mvn … install`, no `-Dtest`), then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit** `feat(P-027-R2a): reactive operation config on Vitess gRPC client-service config`.

### Task 2: `VitessAccessorException` + `VitessAsyncCommand` + `ReactiveVitessGrpcAccessor` (+ `newVtSession()` on the integration service)

**Files:**
- Create: `…/vitess/accessor/VitessAccessorException.java`, `…/vitess/accessor/reactive/VitessAsyncCommand.java`, `…/vitess/accessor/reactive/ReactiveVitessGrpcAccessor.java`
- Modify: `…/vitess/VitessGrpcClientIntegrationService.java` (add `VTSession newVtSession()`), `…/vitess/DefaultVitessGrpcClientIntegrationService.java` (promote private `vtSession()` → public `newVtSession()` implementing it), `appbase-client-vitess/pom.xml` (version-less `reactor-test` test dep + Export-Package `…vitess.accessor.reactive`)
- Test: `…/vitess/accessor/VitessAccessorExceptionTest.java`, `…/vitess/accessor/reactive/ReactiveVitessGrpcAccessorTest.java`

**Interfaces:**
- Consumes: `newVtSession()` (new), `VitessGrpcClient.getDefaultContext()`, `AbstractReactiveAccessor.mono/flux`, `ReactiveOperationConfiguration`.
- Produces:
  - `VitessAccessorException extends AppbaseClientException` — ctors `(Throwable)` / `(String, Throwable)`; `static Throwable map(Throwable t, Function<Throwable,Throwable> fallback)`.
  - `@FunctionalInterface VitessAsyncCommand<T> { SQLFuture<T> apply(VTGateConnection conn, Context ctx, VTSession session) throws SQLException; }`
  - `ReactiveVitessGrpcAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<VitessGrpcClientIntegrationService> borrow)`; ops:
    - `Mono<Cursor> execute(String op, String sql, Map<String,?> bindVars)` + `Mono<Cursor> execute(String op, Context ctx, VTSession session, String sql, Map<String,?> bindVars)`
    - `Mono<List<CursorWithError>> executeBatch(String op, List<String> sqlList, List<Map<String,?>> bindVarsList)` (+ explicit `ctx,session` overload)
    - `Flux<Row> streamExecute(String op, String sql, Map<String,?> bindVars)` (+ explicit overload) — **Tier-1 offload**
    - `<T> Mono<T> command(String op, VitessAsyncCommand<T> fn)`

- [ ] **Step 1: Promote `newVtSession()`.** Rename `DefaultVitessGrpcClientIntegrationService`'s private `vtSession()` to public `newVtSession()` (keep the exact body — `new VTSession(target, executeOptions)` + autocommit) and update its internal call site; declare `VTSession newVtSession();` on `VitessGrpcClientIntegrationService`. Javadoc: "mints a FRESH autocommit session per call from this integration service's config — concurrency-safe; not for cross-op transactions."
- [ ] **Step 2: Write the failing tests.**
  - `VitessAccessorExceptionTest`: a bare `SQLException` maps to `VitessAccessorException`; `new CompletionException(sqlException)` and `new ExecutionException(sqlException)` unwrap to `VitessAccessorException`; a non-SQL `RuntimeException` falls through to the fallback (identity). Mirror `S3AccessorExceptionTest`/`JasyncAccessorException.map`.
  - `ReactiveVitessGrpcAccessorTest` (reuse the `CoreFixtures` mock-spine pattern from `ReactiveS3AccessorTest`): mock `VitessGrpcClientIntegrationService` → `getClient()` → mock `VitessGrpcClient` (`getVtGateConnection()`, `getDefaultContext()`), stub `integrationService.newVtSession()` → a real `new VTSession("t", Query.ExecuteOptions.getDefaultInstance())`. Cases: (a) `execute` — stub `conn.execute(...)` → `new SQLFuture<>(Futures.immediateFuture(cursor))` → StepVerifier asserts the emitted `Cursor`; (b) failure — `new SQLFuture<>(Futures.immediateFailedFuture(new SQLException("x")))` → `expectError(VitessAccessorException.class)`; (c) synchronous `SQLException` thrown by `conn.execute` → also `expectError(VitessAccessorException.class)`; (d) `finalizeOperation()` called on success AND error; (e) `streamExecute` — a fake `Cursor` yielding 2 `Row`s then `null` → `Flux<Row>` emits 2 and the cursor is closed; (f) spine `Timer` recorded. (`SQLFuture` is a concrete class wrapping a `ListenableFuture`, so `new SQLFuture<>(Futures.immediateFuture(v))` is the test double — no mocking of finals needed.)
- [ ] **Step 3: Run — expect FAIL** (classes absent). `mvn … -Dtest=VitessAccessorExceptionTest,ReactiveVitessGrpcAccessorTest install`.
- [ ] **Step 4: Implement.**
  - `VitessAccessorException.map`: unwrap `CompletionException`/`ExecutionException` → cause; `cause instanceof SQLException` → `new VitessAccessorException(cause)`; else `fallback.apply(t)`.
  - `ReactiveVitessGrpcAccessor`: private `call` helper (the Firebase-shaped bridge from the spec §C) —
    ```java
    private <T> Mono<T> call(String op, VitessAsyncCommand<T> fn) {
        return mono(op, () -> Mono.usingWhen(
            Mono.fromSupplier(borrow),
            svc -> Mono.create(sink -> {
                try {
                    SQLFuture<T> f = fn.apply(svc.getClient().getVtGateConnection(),
                                             svc.getClient().getDefaultContext(), svc.newVtSession());
                    Futures.addCallback(f, new FutureCallback<T>() {
                        public void onSuccess(T v) { if (v == null) sink.success(); else sink.success(v); }
                        public void onFailure(Throwable t) { sink.error(t); }
                    }, MoreExecutors.directExecutor());
                    sink.onCancel(() -> f.cancel(false));
                } catch (SQLException e) { sink.error(e); }
            }),
            svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }
    ```
    Convenience ops delegate to `call` supplying `(conn, ctx, session)` from the bridge; explicit overloads build a `call`-like path but pass the caller's `ctx`/`session` (do NOT mint a fresh session). `streamExecute` uses the `flux(op, …)` + `Flux.using(cursorSupplier, cursor -> Flux.generate(...), Cursor::close).subscribeOn(reactorService.getDefaultScheduler())` shape from the spec §F, borrowing via `usingWhen`. `mapError` = `VitessAccessorException.map(t, super::mapError)`; `isNotable` = `true`. Javadoc every public method + the code-site notes from Global Constraints.
- [ ] **Step 5: Run — expect PASS** (all cases green).
- [ ] **Step 6: Whole-module build**, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 7: Commit** `feat(P-027-R2a): VitessAccessorException + ReactiveVitessGrpcAccessor (SQLFuture→Mono bridge, streamExecute Flux)`.

### Task 3: `getReactiveAccessor()` acquisition

**Files:**
- Modify: `…/vitess/VitessGrpcClientService.java` (interface), `…/vitess/DefaultVitessGrpcClientService.java` (impl)
- Test: `…/vitess/DefaultVitessGrpcClientServiceReactiveTest.java`

**Interfaces:**
- Consumes: `ReactiveVitessGrpcAccessor` ctor (Task 2), `getConfiguration().getReactiveOperationConfiguration()` (Task 1).
- Produces: `ReactiveVitessGrpcAccessor getReactiveAccessor()` + `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)`.

- [ ] **Step 1: Write the failing test.** `DefaultVitessGrpcClientServiceReactiveTest` (autoStart(false) + mocked `AppbaseClientCore`, mirror `DefaultS3ClientsServiceReactiveTest`/`DefaultCassandraClientServiceReactiveTest`): `getReactiveAccessor()` non-null + config passthrough (`accessor.getReactiveOperationConfiguration().isMetricsEnabled()` matches the service config); keyed `getReactiveAccessor(key)` non-null.
- [ ] **Step 2: Run — expect FAIL** (methods missing).
- [ ] **Step 3: Implement.** Add both getters to the interface + impl. Impl mirrors `DefaultS3ClientsService`/`DefaultCassandraClientService`: `new ReactiveVitessGrpcAccessor(appbaseClientCore.getAppbaseCore(), appbaseClientCore.getAppbaseCore().getReactorService(), getConfiguration().getReactiveOperationConfiguration(), this::getClientIntegrationService)` (keyed: `() -> getClientIntegrationService(key)`). No cache; no shutdown change. (Confirm the exact `AppbaseCore`/`ReactorService` accessor chain against `DefaultS3ClientsService`.)
- [ ] **Step 4: Run — expect PASS.**
- [ ] **Step 5: Whole-module build**, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit** `feat(P-027-R2a): getReactiveAccessor acquisition on Vitess gRPC client service`. Then open **PR 1**; STOP for merge.

---

## PR 2 — `feature/P-027-R2a-vitess-t4t5-sync-it` (Tasks 4–5, off the epic after PR 1 merges)

### Task 4: `VitessCommand` + sync `VitessGrpcAccessor` + `getAccessor()`

**Files:**
- Create: `…/vitess/accessor/VitessCommand.java`, `…/vitess/accessor/VitessGrpcAccessor.java`
- Modify: `…/vitess/VitessGrpcClientService.java` + `…/vitess/DefaultVitessGrpcClientService.java` (`getAccessor()` + keyed)
- Test: `…/vitess/accessor/VitessGrpcAccessorTest.java` + sync-acquisition cases appended to `…/vitess/DefaultVitessGrpcClientServiceReactiveTest.java`

**Interfaces:**
- Consumes: `VitessAccessorException` (T2), `newVtSession()` (T2), `AbstractAppbaseAccessor.execute(String, Callable<T>)`.
- Produces:
  - `@FunctionalInterface VitessCommand<T> { T apply(VTGateBlockingConnection conn, Context ctx, VTSession session) throws SQLException; }`
  - `VitessGrpcAccessor extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<VitessGrpcClientIntegrationService> borrow)` → `super(appbaseCore, config.isMetricsEnabled(), config.isTraceEnabled(), config.isExceptionStoreEnabled())`; store the config + expose `getReactiveOperationConfiguration()` (mirror `S3Accessor`, for the acquisition passthrough test). Ops mirror the reactive set, blocking returns: `Cursor execute(op, sql, bindVars) throws Exception` (+ explicit overload), `List<CursorWithError> executeBatch(...)`, `Cursor streamExecute(op, sql, bindVars)` (blocking cursor — the sync counterpart of the reactive Flux), `<T> T command(op, VitessCommand<T>) throws Exception`.
  - `VitessGrpcAccessor getAccessor()` + `getAccessor(AppbaseClientKey<ClusterIdValue>)`.

- [ ] **Step 1: Confirm `VTGateBlockingConnection` signatures** with `javap -cp <vitess-client-17.0.0.jar> io.vitess.client.VTGateBlockingConnection` (expect `execute → Cursor`, `executeBatch → List<CursorWithError>`, `streamExecute → Cursor`, all `throws SQLException`). Note any deviation in the report.
- [ ] **Step 2: Write the failing tests.** `VitessGrpcAccessorTest` (mirror `S3AccessorTest` / the reactive test, but blocking): mock integration service → `VitessGrpcClient` → `VTGateBlockingConnection`; stub `execute(...)` → a mock `Cursor` (assert returned); `finalizeOperation()` on success AND error; a thrown `SQLException` → `VitessAccessorException`; spine `Timer` recorded. Append to `DefaultVitessGrpcClientServiceReactiveTest`: `getAccessor()` non-null + config passthrough + keyed non-null.
- [ ] **Step 3: Run — expect FAIL** (classes/methods absent). `mvn … -Dtest=VitessGrpcAccessorTest,DefaultVitessGrpcClientServiceReactiveTest install`.
- [ ] **Step 4: Implement.** `VitessGrpcAccessor` private `run` helper:
    ```java
    private <T> T run(String op, VitessCommand<T> fn) throws Exception {
        return execute(op, () -> {
            VitessGrpcClientIntegrationService svc = borrow.get();
            try { return fn.apply(svc.getClient().getVtGateBlockingConnection(),
                                  svc.getClient().getDefaultContext(), svc.newVtSession()); }
            finally { svc.finalizeOperation(); }
        });
    }
    ```
    Convenience ops mint a fresh session (via `run`); explicit overloads pass the caller's `ctx`/`session`. `mapError` = `VitessAccessorException.map(t, super::mapError)`; `isNotable` = `true`. Add `getAccessor()`/keyed on the service (mirror the reactive getters, no `ReactorService` arg). Javadoc: sync face uses the real blocking `VTGateBlockingConnection` — NOT the async connection blocked.
- [ ] **Step 5: Run — expect PASS.**
- [ ] **Step 6: Whole-module build**, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 7: Commit** `feat(P-027-R2a): sync VitessGrpcAccessor + getAccessor acquisition`.

### Task 5: Gated IT + build-guide + J-018

**Files:**
- Create: `…/vitess/accessor/reactive/ReactiveVitessGrpcAccessorIT.java`
- Modify: `appbase-java/build-guide.md`; `Claude-Review.md` (J-018 clause (m) — on develop at roll-up)

- [ ] **Step 1: Write the gated IT.** `@EnabledIfSystemProperty(named = "VitessClientServiceTests.enabled", matches = "true")`. Bootstrap a live `DefaultVitessGrpcClientService` via `VitessGrpcClientServiceTestHelper` (mirror `DefaultVitessGrpcClientServiceTest` / `VitessGrpcClientServiceTestUtil`) against vtgate on `:15306` (pre-existing J-018 (a) infra). Round-trip:
  - **Reactive** via `getReactiveAccessor()`: `execute("create", CREATE TABLE …)` → `execute("insert", INSERT …)` → `execute("select", SELECT …)` (StepVerifier drains the returned `Cursor`, asserts the row) → `streamExecute("stream", SELECT …)` (StepVerifier asserts the streamed `Row`s) → `execute("drop", DROP TABLE …)`.
  - **Sync** via `getAccessor()`: the same round-trip with blocking assertions.
  - `@AfterAll` best-effort `DROP TABLE` + `service.shutdown()`, swallow errors.
  - Apache header + `@author`/`@since`; document gated-by-default; reactive uses `VTGateConnection`, sync uses `VTGateBlockingConnection`.
- [ ] **Step 2: Verify SKIPPED by default.** `mvn … install` (no gate) → module green, IT not selected (no failsafe; `*IT` excluded by Surefire default). Paste the `Tests run: … Skipped:` line. State that running it needs BOTH `-DVitessClientServiceTests.enabled=true` AND `-Dtest=ReactiveVitessGrpcAccessorIT`.
- [ ] **Step 3: build-guide section.** Add "Reactive Vitess (gRPC)" to `appbase-java/build-guide.md`, matching the existing reactive-twin sections (Cassandra/S3/Firebase): acquisition (`getReactiveAccessor()`/`getAccessor()` + keyed), the `SQLFuture`→`Mono` (Guava `ListenableFuture`) bridge, the sync-uses-blocking-`VTGateBlockingConnection` note, the `streamExecute` Tier-1 offload, the fresh-vs-explicit session model, and the exact gated-IT command (both flags).
- [ ] **Step 4: Whole-module build**, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 5: Commit** `test(P-027-R2a): gated reactive+sync Vitess gRPC IT + build-guide`. Then open **PR 2**; STOP for merge. (J-018 clause (m) is added on develop at roll-up, not on this branch.)

## Post-merge (roll-up on develop, after both PRs merge to the epic)

- Append Vitess learnings to `appbase-java/LEARNING.md` (the `SQLFuture`/Guava-`ListenableFuture` bridge; the async-command-throws-checked wrinkle — first twin where even the reactive command needs a throwing interface; the Tier-1 `streamExecute` offload over the blocking `Cursor`; fresh-vs-explicit `VTSession` via the promoted `newVtSession()`).
- Bump the twin-set summary to **10 of 10** (R2a-in-repo COMPLETE); update the J-018 summary to (c)–(m) and add clause (m) for `ReactiveVitessGrpcAccessorIT`.
- Roll the twin up into the epic. **This closes R2a-in-repo** — per the delivery order, next is R2b → R1 → R3 → R4 → R5 → epic→develop.

## Self-review notes

- Spec coverage: A→T2 (op scope) · B→T2/T4 (dual client) · C→T2 (`call` bridge) · D→T2/T4 (`VitessAsyncCommand`/`VitessCommand`) · E→T2/T4 (session overloads + `newVtSession()`) · F→T2/T4 (op surface + `streamExecute` Flux) · G→T2 (`VitessAccessorException.map`) · H→no metrics task · I→T3/T4 (acquisition). Components in spec §"Components" all mapped to a task. Testing (spec §Testing) → T2/T4 unit + T5 IT.
- Type consistency: `Supplier<VitessGrpcClientIntegrationService> borrow` used identically in T2/T4 ctors and T3/T4 acquisition; `newVtSession()` declared T2, consumed T2/T4; `VitessAccessorException.map(t, super::mapError)` identical both faces; key `ClusterIdValue` throughout.
- No placeholders: the one non-obvious body (the `call` bridge) and the `run` helper are shown in full; trivial per-op bodies are specified by signature + the driver call they wrap (per the project's plan-stub convention — implementer writes them from the interface + reference twins).
