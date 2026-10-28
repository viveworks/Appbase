# P-027 R2b — Reactive SQL over R2DBC — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a genuinely non-blocking SQL face to Appbase — a new reactive-only `appbase-client-r2dbc` module over R2DBC (native `ConnectionPool`), the reactive counterpart of the blocking `appbase-client-jdbc`.

**Architecture:** New `appbase-client` leaf, mirroring the JDBC leaf but with a native `io.r2dbc.pool.ConnectionPool` as the per-key client (vs JDBC's HikariCP `DataSource`) and a reactive accessor on the shared R0 spine. R2DBC is native Reactive-Streams (`Publisher`→`Flux`/`Mono` directly). Reactive-only: NO sync accessor (sync SQL = existing JDBC). Autocommit ops + an `inTransaction` pinned-connection helper. Dockerless H2 CI IT + gated MariaDB/Vitess IT.

**Tech Stack:** Java 25, Reactor 3.6.8, R2DBC (`io.r2dbc:r2dbc-spi`, `io.r2dbc:r2dbc-pool`; drivers `io.r2dbc:r2dbc-h2` test, `org.mariadb:r2dbc-mariadb` + `org.postgresql:r2dbc-postgresql` optional), Guice, Maven, JUnit 5 + Mockito + `reactor-test`.

**Reference leaf (mirror its shape):** `appbase-java/appbase-client/appbase-client-jdbc` — `JdbcClientService extends AppbaseClientService<DataSource, TextValue>`, `DefaultJdbcClientIntegrationService extends AbstractAppbaseClientIntegrationService<DataSource, TextValue, DefaultJdbcClientIntegrationServiceConfiguration>` (builds a HikariCP `DataSource` in `initImpl`, holds it, closes in `shutdownImpl`), `configuration/*`, `JdbcClientServiceGuiceModule`, `JdbcClientServiceTestHelper`. **Reference twin acquisition:** the merged Vitess/S3 twins (`getReactiveAccessor()`/keyed; `CoreFixtures` mock-spine in `ReactiveVitessGrpcAccessorTest`). **Spec:** `docs/specs/2026-07-08-P027-R2b-reactive-r2dbc-sql-design.md`.

## Global Constraints

- **Branch/PR (max 3 tasks/PR):** twin sub-issue off the epic `feature/P-027-reactive-appbase`. **PR 1** = T1–3 on `feature/P-027-R2b-r2dbc-t1t3`; **PR 2** = T4–5 on `feature/P-027-R2b-r2dbc-t4t5`; **PR 3** = T6–7 on `feature/P-027-R2b-r2dbc-t6t7`; all target the epic. Roll up when all merge.
- **Every Java file:** Apache header (copy verbatim from a JDBC-leaf file), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`; **no FQCNs** (imports only; class-name String constants excepted). Document at the code site: reactive-only (no sync face — point to `appbase-client-jdbc`); the native `ConnectionPool` borrow-via-`usingWhen`; the `inTransaction` begin/commit/rollback/close contract; the non-null-positional-binds limitation.
- **Key type:** `AppbaseClientKey<TextValue>` (`com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue`) — matching the JDBC client this mirrors.
- **Dependency versions only in root `<dependencyManagement>`** (`/Users/nav/Viveworks/Technology/Appbase/pom.xml`). reactor already managed. Add R2DBC coordinates there (see Task 1); use the current stable release of each (baseline: `r2dbc-spi 1.0.0.RELEASE`, `r2dbc-pool 1.0.2.RELEASE`, `r2dbc-h2 1.0.0.RELEASE`, `r2dbc-mariadb 1.3.0`, `r2dbc-postgresql 1.0.7.RELEASE`) — confirm latest stable that resolves on Java 25 + reactor 3.6.8 via `mvn dependency:tree`.
- **Build (use `install`, NOT `test` — bnd Export-Package on `.reactive` fails under `test`):** `cd /Users/nav/Viveworks/Technology/Appbase && mvn -q -pl appbase-java/appbase-client/appbase-client-r2dbc -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`. Scope with `-Dtest=<Class>`.
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- **Reactive-only:** the module has `getReactiveAccessor()` but NO `getAccessor()`. **Gated IT → J-018 clause (n). Learnings → `appbase-java/LEARNING.md`.**

## Established facts (verified)

- JDBC leaf key type `TextValue`; `JdbcClientService extends AppbaseClientService<DataSource, TextValue>`; `DefaultJdbcClientIntegrationService extends AbstractAppbaseClientIntegrationService<DataSource, TextValue, DefaultJdbcClientIntegrationServiceConfiguration>` with `protected void initImpl()` (build client), `protected void shutdownImpl()` (close client), `getClient()` returns the held `T`.
- `appbase-client` aggregator pom lists 18 leaf `<module>`s — add `appbase-client-r2dbc`.
- Bundle plugin `<Export-Package>` per leaf (jdbc exports `com.viveworks.appbase.client.service.jdbc`).
- `AbstractReactiveAccessor(AppbaseCore, ReactorService, ReactiveOperationConfiguration)` + `mono(op, Supplier<Mono>)` / `flux(op, Supplier<Flux>)`; `reactorService` is `protected final`, `getDefaultScheduler()` available; `mono`/`flux` apply `onErrorMap(this::mapError)` + timer/span. `DefaultReactiveOperationConfiguration.builder()` in `…client.core.reactive`.
- R2DBC SPI: `ConnectionFactories.get(url|ConnectionFactoryOptions) → ConnectionFactory`; `ConnectionPoolConfiguration.builder(connectionFactory).maxSize(n)…build()` → `new ConnectionPool(cfg)`; `pool.create() → Mono<Connection>` (Reactor type); `connection.close() → Publisher<Void>`; `connection.createStatement(sql).bind(i, v).execute() → Publisher<Result>`; `result.map(BiFunction<Row,RowMetadata,T>) → Publisher<T>`; `result.getRowsUpdated() → Publisher<Long>`; `connection.beginTransaction()/commitTransaction()/rollbackTransaction() → Publisher<Void>`; `pool.getMetrics() → Optional<PoolMetrics>`; errors are `io.r2dbc.spi.R2dbcException`. H2 reactive URL: `r2dbc:h2:mem:///<name>;DB_CLOSE_DELAY=-1`.

---

## PR 1 — foundation + core accessor (`feature/P-027-R2b-r2dbc-t1t3`)

### Task 1: Module scaffold + service/config + integration service (native `ConnectionPool`) + reactive-config getter

**Files:**
- Create module `appbase-java/appbase-client/appbase-client-r2dbc/` — `pom.xml` (parent `appbase-client`, deps: `appbase-client-core`, `io.r2dbc:r2dbc-spi`, `io.r2dbc:r2dbc-pool`, `reactor-core`; `io.r2dbc:r2dbc-h2` + `reactor-test` test scope; `org.mariadb:r2dbc-mariadb` + `org.postgresql:r2dbc-postgresql` `<optional>true</optional>`; maven-bundle-plugin with `<Export-Package>com.viveworks.appbase.client.service.r2dbc</Export-Package>` — copy the jdbc pom as the template).
- Modify `appbase-java/appbase-client/pom.xml` — add `<module>appbase-client-r2dbc</module>`.
- Modify root `pom.xml` `<dependencyManagement>` — add the 5 R2DBC coordinates + versions.
- Create package `…/service/r2dbc/`: `R2dbcClientService.java`, `DefaultR2dbcClientService.java`, `R2dbcClientIntegrationService.java`, `DefaultR2dbcClientIntegrationService.java`, `R2dbcClientServiceGuiceModule.java`, `R2dbcClientUtil.java`, `package-info.java`; `…/service/r2dbc/configuration/`: `R2dbcClientServiceConfiguration.java`(+Default), `R2dbcClientIntegrationServiceConfiguration.java`(+Default), `R2dbcConnectionConfiguration.java` (URL/host/port/db/user/password/ssl → `ConnectionFactoryOptions`), `R2dbcPoolConfiguration.java` (maxSize/initialSize/maxIdleTime/maxAcquireTime/validationQuery), `package-info.java`.
- Test: `configuration/DefaultR2dbcClientServiceConfigurationTest.java`, `DefaultR2dbcClientIntegrationServiceTest.java`, `R2dbcClientServiceTestHelper.java`.

**Interfaces produced:**
- `R2dbcClientService extends AppbaseClientService<ConnectionPool, TextValue>` — plus (Task 3) the acquisition getters; service config exposes `ReactiveOperationConfiguration getReactiveOperationConfiguration()`.
- `DefaultR2dbcClientIntegrationService` — `initImpl()` builds `ConnectionFactory` from `R2dbcConnectionConfiguration` → `ConnectionPool` (sized from `R2dbcPoolConfiguration`), holds it; `getClient()` returns the `ConnectionPool`; `shutdownImpl()` `pool.dispose()`.
- `R2dbcClientUtil` — `static ConnectionFactoryOptions options(R2dbcConnectionConfiguration)` + `static ConnectionPool connectionPool(R2dbcConnectionConfiguration, R2dbcPoolConfiguration)`.

- [ ] **Step 1: Scaffold the module** (pom copied from jdbc leaf with R2DBC deps + Export-Package; add to aggregator; add versions to root depMgmt). Run `mvn -q -pl appbase-java/appbase-client/appbase-client-r2dbc -am … install` → expect it to build an empty module (or fail only on missing classes you add next).
- [ ] **Step 2: Write failing tests.** `DefaultR2dbcClientServiceConfigurationTest`: default `getReactiveOperationConfiguration()` non-null + `isMetricsEnabled()==true`; `metricsEnabled(false)` round-trip; YAML round-trip (mirror the Vitess/S3 config tests + their subtype-registration setUp). `DefaultR2dbcClientIntegrationServiceTest`: given an H2 config (`r2dbc:h2:mem:///t1;DB_CLOSE_DELAY=-1`), after start `getClient()` returns a non-null `ConnectionPool` and a `pool.create()` yields a usable `Connection` (`.close()` it); after `shutdown()` the pool is disposed. Use `R2dbcClientServiceTestHelper` to bootstrap.
- [ ] **Step 3: Run — expect FAIL** (classes/config absent). `mvn … -Dtest=DefaultR2dbcClientServiceConfigurationTest,DefaultR2dbcClientIntegrationServiceTest install`.
- [ ] **Step 4: Implement.** Config classes (mirror `DefaultJdbcClientIntegrationServiceConfiguration` + `HikariDataSourceConfiguration`/`JdbcUrlConfiguration` for the field/Builder/equals/hashCode/toString shape; add the reactive-config getter to the service config, covariant `DefaultReactiveOperationConfiguration`, mirror the Vitess T1). `R2dbcClientUtil.connectionPool(...)`. `DefaultR2dbcClientIntegrationService` (mirror `DefaultJdbcClientIntegrationService`, swapping `DataSource`→`ConnectionPool`). `DefaultR2dbcClientService` (mirror `DefaultJdbcClientService`, all ctors). `R2dbcClientServiceGuiceModule`, `R2dbcClientServiceTestHelper` (mirror jdbc's).
- [ ] **Step 5: Run — expect PASS.**
- [ ] **Step 6: Whole-module build**, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 7: Commit** `feat(P-027-R2b): appbase-client-r2dbc scaffold — service/config/integration-service (native R2DBC ConnectionPool per key)`.

### Task 2: `R2dbcAccessorException` + `ReactiveR2dbcAccessor` core ops (query/update/command)

**Files:**
- Create `…/service/r2dbc/accessor/R2dbcAccessorException.java`, `…/service/r2dbc/accessor/reactive/ReactiveR2dbcAccessor.java`.
- Modify `appbase-client-r2dbc/pom.xml` — ensure the bnd `<Export-Package>` also lists `com.viveworks.appbase.client.service.r2dbc.accessor.reactive` (and `.accessor`), like the Vitess twin (module export has no wildcard).
- Test: `accessor/R2dbcAccessorExceptionTest.java`, `accessor/reactive/ReactiveR2dbcAccessorTest.java`.

**Interfaces:**
- Consumes: `DefaultR2dbcClientIntegrationService.getClient()` → `ConnectionPool` (Task 1); `AbstractReactiveAccessor.mono/flux`.
- Produces:
  - `R2dbcAccessorException extends AppbaseClientException` — ctors `(Throwable)`/`(String,Throwable)`; `static Throwable map(Throwable, Function<Throwable,Throwable> fallback)` unwrapping `CompletionException`/`ExecutionException` → cause, `cause instanceof io.r2dbc.spi.R2dbcException` → `new R2dbcAccessorException(cause)`, else fallback (mirror `VitessAccessorException.map`).
  - `ReactiveR2dbcAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<R2dbcClientIntegrationService> borrow)`. Ops:
    - `<T> Flux<T> query(String op, String sql, BiFunction<Row,RowMetadata,T> mapper, Object... binds)`
    - `Mono<Long> update(String op, String sql, Object... binds)`
    - `<T> Flux<T> command(String op, Function<Connection, Publisher<T>> fn)`
    - private `<T> Flux<T> withConnection(String op, Function<Connection, Publisher<T>> work)` (the nested-`usingWhen` borrow, verbatim from the spec §C).
    - `mapError` = `R2dbcAccessorException.map(t, super::mapError)`; `isNotable` = true.

- [ ] **Step 1: Write failing tests.**
  - `R2dbcAccessorExceptionTest`: bare `R2dbcException` (use `new R2dbcException("x"){}` or a concrete subclass like `io.r2dbc.spi.R2dbcNonTransientException`) → `R2dbcAccessorException`; `CompletionException(r2dbcEx)` / `ExecutionException(r2dbcEx)` unwrap; non-R2DBC `RuntimeException` → fallback identity.
  - `ReactiveR2dbcAccessorTest`: build a **real H2 `ConnectionPool`** (`ConnectionPoolConfiguration.builder(ConnectionFactories.get("r2dbc:h2:mem:///accessortest;DB_CLOSE_DELAY=-1")).build()`), and a mock `R2dbcClientIntegrationService` whose `getClient()` returns it (reuse the `CoreFixtures` mock-spine from `ReactiveVitessGrpcAccessorTest`). Cases: `update("create", "CREATE TABLE t(id INT, name VARCHAR(20))")` completes; `update("insert", "INSERT INTO t VALUES (?, ?)", 1, "a")` emits `1`; `query("select", "SELECT name FROM t WHERE id=?", (row,md) -> row.get("name", String.class), 1)` emits `"a"`; a bad SQL (`query(... "SELECT * FROM nope" ...)`) errors with `R2dbcAccessorException`; `finalizeOperation()` called on success AND error (verify on the mock integration service); spine `Timer` recorded. (H2 gives real `Result`/`Row` — do NOT mock the SPI.)
- [ ] **Step 2: Run — expect FAIL** (classes absent). `mvn … -Dtest=R2dbcAccessorExceptionTest,ReactiveR2dbcAccessorTest install`.
- [ ] **Step 3: Implement.** `R2dbcAccessorException`. `ReactiveR2dbcAccessor`: `withConnection` (spec §C nested `usingWhen`); `query` = `withConnection(op, conn -> { Statement s = conn.createStatement(sql); bindAll(s, binds); return Flux.from(s.execute()).flatMap(r -> r.map(mapper)); })`; `update` = `withConnection(op, conn -> …execute()).flatMap(Result::getRowsUpdated)).reduce(0L, Long::sum)` returned as `Mono` (`.next()`/`.reduce` on the Flux — return `Mono<Long>` via `mono(op, …)` wrapping, OR implement `update` on a `withConnectionMono`; keep the spine wrapper consistent — decide and note); `command` = `withConnection(op, fn)`. Private `bindAll(Statement, Object[])` = positional `s.bind(i, v)` (non-null; document nulls go via `command`). Javadoc every public method + the code-site notes.
- [ ] **Step 4: Run — expect PASS.**
- [ ] **Step 5: Whole-module build**, ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit** `feat(P-027-R2b): R2dbcAccessorException + ReactiveR2dbcAccessor (query/update/command over native ConnectionPool)`.

### Task 3: `getReactiveAccessor()` acquisition

**Files:** modify `R2dbcClientService.java` + `DefaultR2dbcClientService.java`; test `DefaultR2dbcClientServiceReactiveTest.java`.

**Interfaces:** `ReactiveR2dbcAccessor getReactiveAccessor()` + `getReactiveAccessor(AppbaseClientKey<TextValue>)`. **No `getAccessor()`.**

- [ ] **Step 1: Write failing test.** `DefaultR2dbcClientServiceReactiveTest` (autoStart(false) + mocked `AppbaseClientCore`, mirror `DefaultVitessGrpcClientServiceReactiveTest`): `getReactiveAccessor()` non-null; config passthrough (`accessor` uses `getConfiguration().getReactiveOperationConfiguration()` — assert `getReactiveOperationConfiguration().isMetricsEnabled()` matches); keyed `getReactiveAccessor(key)` non-null.
- [ ] **Step 2: Run — expect FAIL.**
- [ ] **Step 3: Implement.** Both getters on interface + impl; impl mirrors `DefaultVitessGrpcClientService.getReactiveAccessor()` / keyed: `new ReactiveR2dbcAccessor(appbaseClientCore.getAppbaseCore(), appbaseClientCore.getAppbaseCore().getReactorService(), getConfiguration().getReactiveOperationConfiguration(), this::getClientIntegrationService)` (keyed: `() -> getClientIntegrationService(key)`). No `getAccessor()`. No cache; no shutdown change.
- [ ] **Step 4: Run — expect PASS.**
- [ ] **Step 5: Whole-module build**, ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit** `feat(P-027-R2b): getReactiveAccessor acquisition on R2dbcClientService`. Then open **PR 1**; STOP for merge.

---

## PR 2 — transactions + metrics (`feature/P-027-R2b-r2dbc-t4t5`, off the epic after PR 1 merges)

### Task 4: `inTransaction` + `ReactiveSqlSession` + `batch`

**Files:** create `accessor/reactive/ReactiveSqlSession.java`, `accessor/reactive/DefaultReactiveSqlSession.java`; modify `accessor/reactive/ReactiveR2dbcAccessor.java` (add `inTransaction` + `batch`); test add cases to `ReactiveR2dbcAccessorTest.java`.

**Interfaces:**
- `ReactiveSqlSession` — `<T> Flux<T> query(String sql, BiFunction<Row,RowMetadata,T> mapper, Object... binds)`; `Mono<Long> update(String sql, Object... binds)` (on the pinned connection; no `op` — the enclosing `inTransaction` owns the spine timer).
- `DefaultReactiveSqlSession implements ReactiveSqlSession` — wraps one `Connection`.
- `ReactiveR2dbcAccessor`: `<T> Mono<T> inTransaction(String op, Function<ReactiveSqlSession, Publisher<T>> work)`; `Mono<Long> batch(String op, String sql, List<Object[]> bindsList)`.

- [ ] **Step 1: Write failing tests** (H2-backed, extend `ReactiveR2dbcAccessorTest`):
  - `inTransaction` COMMIT: create a table, then `inTransaction("tx", s -> Mono.from(s.update("INSERT INTO t VALUES (?,?)", 2, "b")))` → emits `1`; a subsequent autocommit `query` sees id=2.
  - `inTransaction` ROLLBACK: `inTransaction("tx", s -> Mono.from(s.update("INSERT INTO t VALUES (?,?)", 3, "c")).then(Mono.error(new IllegalStateException("boom"))))` → errors; a subsequent `query` for id=3 emits nothing (rolled back).
  - `batch("batch", "INSERT INTO t VALUES (?,?)", List.of(new Object[]{4,"d"}, new Object[]{5,"e"}))` → emits `2`; `query count` == 2 new rows.
  - `finalizeOperation()` + connection close on the tx path (success AND error).
- [ ] **Step 2: Run — expect FAIL.**
- [ ] **Step 3: Implement.** `DefaultReactiveSqlSession` (query/update on the pinned connection, same statement-building as the accessor's autocommit ops). `inTransaction` = the spec §E composition (`beginTransaction` → run `work` → `commitTransaction` on success / `rollbackTransaction` then re-error on failure → connection closed by the inner `usingWhen`). `batch` = one `Statement`, loop `bind(...).add()`, final `execute()` → sum `getRowsUpdated`. Javadoc the begin/commit/rollback/close contract.
- [ ] **Step 4: Run — expect PASS.**
- [ ] **Step 5: Whole-module build**, ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit** `feat(P-027-R2b): inTransaction + ReactiveSqlSession + batch on ReactiveR2dbcAccessor`.

### Task 5: config-gated native-pool metrics gauges

**Files:** modify `DefaultR2dbcClientIntegrationService.java` (bind/unbind gauges) + `configuration/R2dbcClientIntegrationServiceConfiguration`(+Default) (add `driverMetricsEnabled` gate, default false); test add cases to `DefaultR2dbcClientIntegrationServiceTest.java`.

**Interfaces:** private `bindPoolMetrics()` on `initImpl` (when gated on), `unbindPoolMetrics()` on `shutdownImpl`; per-key-tagged Micrometer gauges from `ConnectionPool.getMetrics()` (`acquiredSize`/`idleSize`/`pendingAcquireSize`/`allocatedSize`), via `appbaseCore.getMetricService()`. Mirror the jasync pool-counter / ZooKeeper connection-state-gauge pattern (read one for the exact registry/tag idiom).

- [ ] **Step 1: Write failing tests.** Gate default false (no gauges registered on start); with `driverMetricsEnabled(true)` + a metric-service spy, `initImpl` registers the pool gauges (assert `metricRegistry.gauge(...)`/`register` called with the per-key tag) and `shutdownImpl` unbinds. (Unit-test the seams; the live gauge values need the IT.) Mirror the jasync/ZK metrics tests.
- [ ] **Step 2: Run — expect FAIL.**
- [ ] **Step 3: Implement** the config gate + bind/unbind, guarded by `driverMetricsEnabled` + `ConnectionPool.getMetrics().isPresent()`.
- [ ] **Step 4: Run — expect PASS.**
- [ ] **Step 5: Whole-module build**, ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit** `feat(P-027-R2b): config-gated R2DBC ConnectionPool metrics gauges`. Then open **PR 2**; STOP for merge.

---

## PR 3 — ITs + docs (`feature/P-027-R2b-r2dbc-t6t7`, off the epic after PR 2 merges)

### Task 6: dockerless non-gated H2 round-trip IT

**Files:** create `accessor/reactive/ReactiveR2dbcAccessorH2IT.java` (or `…H2Test.java` — see below).

- [ ] **Step 1: Write the IT.** NON-gated, in-memory H2 (`r2dbc:h2:mem:///r2dbcit;DB_CLOSE_DELAY=-1`) — a full reactive round-trip via `getReactiveAccessor()`: `CREATE TABLE` → `update INSERT` → `query SELECT` (StepVerifier asserts the mapped row) → `inTransaction` COMMIT (assert visible) → `inTransaction` ROLLBACK on a thrown error (assert not visible) → `DROP`. Bootstrap a real `DefaultR2dbcClientService` via `R2dbcClientServiceTestHelper` with an H2 config. **This must run in a normal build (no gate, no infra).** NAME IT `*Test` (e.g. `ReactiveR2dbcAccessorH2Test`) so Surefire selects it by default — since the module has no failsafe and H2 needs no infra, a `*Test` name is correct here (unlike the gated `*IT`s); confirm it runs green in the plain `mvn … install`.
- [ ] **Step 2: Run — expect PASS in a plain module build.** `mvn … install` (no `-Dtest`) → the H2 test executes and passes; module green.
- [ ] **Step 3: Whole-module build**, ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 4: Commit** `test(P-027-R2b): dockerless H2 reactive round-trip test (CI-runnable)`.

### Task 7: gated MariaDB/Vitess IT + README + JDBC note + build-guide + J-018

**Files:** create `accessor/reactive/ReactiveR2dbcAccessorMariaDbIT.java`; create `appbase-java/appbase-client/appbase-client-r2dbc/README.md`; create/modify `appbase-java/appbase-client/appbase-client-jdbc/README.md`; modify `appbase-java/build-guide.md`; `Claude-Review.md` (J-018 clause (n) — on develop at roll-up).

- [ ] **Step 1: Gated MariaDB/Vitess IT.** `@EnabledIfSystemProperty(named = "R2dbcClientServiceTests.enabled", matches = "true")` (new gate; add the default-false entry to `appbase-client/pom.xml` systemPropertyVariables if that is where the other gates live — confirm). Reactive round-trip over `r2dbc-mariadb` (`r2dbc:mariadb://<host>:<port>/<db>`) against MariaDB / **vtgate:15306** (Vitess speaks MariaDB wire — the umbrella's called-out Vitess-over-SQL IT), bootstrapped via `R2dbcClientServiceTestHelper`. Same round-trip shape as the H2 test. `*IT` + no failsafe → needs BOTH the gate and `-Dtest=ReactiveR2dbcAccessorMariaDbIT`. `@AfterAll` best-effort cleanup.
- [ ] **Step 2: Verify SKIPPED by default** (`mvn … install` → the MariaDB IT not selected; the H2 test still runs green). State the exact enable command.
- [ ] **Step 3: README (module) — `appbase-client-r2dbc/README.md`.** State PROMINENTLY: reactive-only — **NO sync/blocking accessor**; for the synchronous SQL face use **`appbase-client-jdbc`** (link). Cover: R2DBC SPI + driver-by-config, keyed config (`TextValue`), `getReactiveAccessor()`/keyed, op surface (`query`/`update`/`batch`/`inTransaction`/`command`), supported/optional drivers, the H2 test + gated-MariaDB-IT commands, the non-null-positional-binds limitation.
- [ ] **Step 4: JDBC README note.** In `appbase-client-jdbc/README.md` (create if absent) add: "For a non-blocking/reactive SQL face, see `appbase-client-r2dbc` (P-027 R2b)."
- [ ] **Step 5: build-guide section.** Add "Reactive SQL (R2DBC)" to `appbase-java/build-guide.md`, matching the existing reactive-twin sections (Cassandra/S3/Vitess): acquisition (`getReactiveAccessor()`/keyed, no sync face), the native `ConnectionPool` model, op surface + `inTransaction`, the H2-runnable test note, and the gated MariaDB/Vitess IT command.
- [ ] **Step 6: Whole-module build**, ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 7: Commit** `test(P-027-R2b): gated MariaDB/Vitess R2DBC IT + README (no-sync + JDBC link) + build-guide`. Then open **PR 3**; STOP for merge. (J-018 clause (n) added on develop at roll-up.)

## Post-merge (roll-up on develop, after all three PRs merge to the epic)

- Append R2b learnings to `appbase-java/LEARNING.md` (R2DBC = native Reactive-Streams, no future adapter; the `ConnectionPool`-as-client keyed model — R2DBC:JDBC :: ConnectionPool:DataSource; the two-level nested `usingWhen` borrow; `inTransaction` begin/commit/rollback/close; H2 = first CI-runnable twin IT; reactive-only module, no sync face).
- Add J-018 clause (n) for the gated MariaDB/Vitess IT (ties to clause (a) Vitess infra); note the H2 test is NOT gated (runs in CI).
- Roll up into the epic. **This closes R2b.** Next per delivery order: R1 (reactive web) → R3 → R4 (incl. deferred Hibernate-Reactive) → R5.

## Self-review notes

- Spec coverage: A→T1 (module) · B→T1 (service/config/pool) · C→T2+T3 (borrow+acquisition) · D→T2 (query/update/command)+T4 (batch) · E→T4 (inTransaction/ReactiveSqlSession) · F→T2 (exception) · G→T5 (metrics) · H→T6 (H2 IT)+T7 (MariaDB IT/README/JDBC note/build-guide). Components in spec §Components all mapped. Testing (spec §Testing) → T1/T2/T4/T5 unit (H2-backed) + T6/T7 ITs.
- Type consistency: `Supplier<R2dbcClientIntegrationService> borrow` in T2 ctor + T3 acquisition; `ConnectionPool` client type from T1 used in T2's `getClient()`; `ReactiveSqlSession.query/update` signatures match the accessor's autocommit ops minus `op`; `R2dbcAccessorException.map(t, super::mapError)` consistent; key `TextValue` throughout; no `getAccessor()` anywhere.
- Placeholders: the two non-obvious compositions (nested `withConnection` borrow, `inTransaction` commit/rollback) are given from the spec; the `update` spine-wrapper choice (Mono vs Flux-collapse) is called out for the implementer to settle at T2 with a stated contract, not left vague.
