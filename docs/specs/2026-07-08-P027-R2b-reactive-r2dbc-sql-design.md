# P-027 R2b — Reactive SQL over R2DBC — Design

**Status:** approved (2026-07-08) · **Epic:** `feature/P-027-reactive-appbase` · **Umbrella:** `docs/specs/2026-07-04-P027-reactive-appbase-design.md` (R2b row)

The first R2b deliverable: a genuinely non-blocking SQL face for Appbase over **R2DBC** (Reactive Relational Database Connectivity). A new `appbase-client-r2dbc` module — the reactive counterpart of the existing blocking `appbase-client-jdbc`. Distinct from R2a's Vitess Tier-1 gRPC twin (that is object-query gRPC; this is SQL).

**Scope decision (2026-07-08):** R2b is **R2DBC-only**. Hibernate-Reactive (reactive JPA) is **deferred to R4** (recorded in the umbrella spec R2b/R4 rows) — a different stack (Mutiny/Stage over Vert.x-SQL) and a thornier ORM effort that pairs naturally with R4's reactive store twins.

## Context (verified against the code)

- The sync SQL family lives in `appbase-client`: `appbase-client-jdbc` (`JdbcClientService extends AppbaseClientService<DataSource, TextValue>`, holds a HikariCP `DataSource` per key, built from a keyed `HikariDataSourceConfiguration`), `appbase-client-jdbi`, `appbase-client-jpa`. Key type across the SQL family is **`TextValue`** (NOT `ClusterIdValue`).
- Appbase already nests pools: `AbstractAppbaseClientService<T,V>` holds an object-pool (`AppbaseClientPool`) of integration-services per key (borrow via `getClientIntegrationService()` → `finalizeOperation()`); each integration service holds a **shared per-key connection pool** as its client `T` (JDBC's `T` = HikariCP `DataSource`). R2DBC slots into this exactly: `T` = native `io.r2dbc.pool.ConnectionPool`.
- No R2DBC / reactor-relational dependencies exist in the repo yet — all new, versions to be added to root `<dependencyManagement>`.
- Shared spine (all R2a twins): `AbstractReactiveAccessor(AppbaseCore, ReactorService, ReactiveOperationConfiguration)` + `mono`/`flux`; `DefaultReactiveOperationConfiguration` in `…client.core.reactive`. R2DBC is native Reactive-Streams (`org.reactivestreams.Publisher`), so it bridges to `Flux`/`Mono` directly (like Cassandra/Lettuce — `Flux.usingWhen`), NO future-adapter needed.

## Decisions

### A. New reactive-only module `appbase-client-r2dbc`
The reactive SQL face is a **new module**, not a second face on the JDBC service — R2DBC is a whole new connection stack (its own driver SPI, its own reactive pool), which the umbrella spec classifies as Tier-3 ("separate `-reactive` module"). The module is **reactive-only: there is NO sync accessor** — the blocking/sync SQL face is the existing `appbase-client-jdbc`. This is not a dropped capability (unlike the R2a twins, which needed both faces on one service); the sync counterpart already exists as a sibling module. **README requirement (see §H).**

### B. Service shape — R2DBC is to JDBC as `ConnectionPool` is to `DataSource`
`R2dbcClientService extends AppbaseClientService<ConnectionPool, TextValue>` (+ `DefaultR2dbcClientService`), mirroring `JdbcClientService extends AppbaseClientService<DataSource, TextValue>`. Client type `T` = `io.r2dbc.pool.ConnectionPool`; key type `TextValue`. Config-by-key is preserved unchanged:
- `R2dbcClientServiceConfiguration` (+ `Default`) — service config; adds `ReactiveOperationConfiguration getReactiveOperationConfiguration()` (the R2a reactive-config getter).
- `R2dbcClientIntegrationServiceConfiguration` (+ `Default`) — per-key connection config: the R2DBC URL / `io.r2dbc.spi.ConnectionFactoryOptions` (driver, host, port, database, user, password, SSL) **plus native-pool sizing** (`maxSize`, `initialSize`, `maxIdleTime`, `maxAcquireTime`, `validationQuery`) — the reactive analog of `HikariDataSourceConfiguration`.
- `DefaultR2dbcClientIntegrationService extends AbstractAppbaseClientIntegrationService<ConnectionPool, TextValue, DefaultR2dbcClientIntegrationServiceConfiguration>` — `initImpl` builds a `ConnectionFactory` from the config's `ConnectionFactoryOptions`, wraps it in a `ConnectionPool` (from `ConnectionPoolConfiguration` built off the pool-sizing fields), and holds it; `getClient()` returns the `ConnectionPool`; `shutdownImpl` calls `pool.dispose()`.
- **Appbase object-pool sizing:** the object-pool of integration-services per key should be small (default 1) — the real connection pooling is the native `ConnectionPool`; the integration-service layer is a lightweight shared holder (same shape JDBC uses with one shared `DataSource`). Confirm/set the per-key appbase-pool default at implementation.

### C. Acquisition (uniform with R2a) — nested borrow
`ReactiveR2dbcAccessor getReactiveAccessor()` + `getReactiveAccessor(AppbaseClientKey<TextValue>)` on `R2dbcClientService`; **no `getAccessor()`** (reactive-only). Impl mirrors the R2a twins: `new ReactiveR2dbcAccessor(appbaseClientCore.getAppbaseCore(), …getReactorService(), getConfiguration().getReactiveOperationConfiguration(), this::getClientIntegrationService)` (keyed: `() -> getClientIntegrationService(key)`).

The accessor nests two `usingWhen`s — the outer returns the appbase integration-service to its object-pool (fast in-memory borrow, identical to R2a); the inner reactively borrows and closes a real R2DBC `Connection` from the native pool:
```java
private <T> Flux<T> withConnection(final String op, final Function<Connection, Publisher<T>> work) {
    return flux(op, () -> Flux.usingWhen(
        Mono.fromSupplier(borrow),                                   // appbase integration svc (per key)
        svc -> Flux.usingWhen(svc.getClient().create(),              // r2dbc Connection from native pool
                              work::apply,
                              Connection::close),
        svc -> Mono.fromRunnable(svc::finalizeOperation)));
}
```
(A `Mono` sibling `withConnectionMono` for single-value ops, or `.next()`/collect from the `Flux` form.)

### D. Op surface
R2DBC is native Reactive-Streams (`Statement.execute()` → `Publisher<Result>`; `Result.map(BiFunction<Row,RowMetadata,T>)` → `Publisher<T>`; `Result.getRowsUpdated()` → `Publisher<Long>`).

- `<T> Flux<T> query(String op, String sql, BiFunction<Row,RowMetadata,T> mapper, Object... binds)` — `createStatement(sql)`, positional `bind(i, value)` for each bind, `execute()` → `Flux<Result>` → `flatMap(r -> r.map(mapper))`. **Mapper is required** (`Row` is only valid inside `map()`; returning a raw `Row` outside it is illegal).
- `Mono<Long> update(String op, String sql, Object... binds)` — execute → `Flux<Result>` → `flatMap(Result::getRowsUpdated)` → `reduce(0L, Long::sum)`.
- `Mono<Long> batch(String op, String sql, List<Object[]> bindsList)` — one `Statement`, `bind(...).add()` per row, `execute()` → sum rows-updated.
- `<T> Mono<T> inTransaction(String op, Function<ReactiveSqlSession, Publisher<T>> work)` — see §E.
- `<T> Flux<T> command(String op, Function<Connection, Publisher<T>> fn)` — generic escape hatch = `withConnection(op, fn)`.
- **Binds:** positional, by index, non-null (`statement.bind(i, value)`). NULL binds need `bindNull(i, Class)` (R2DBC requires the type) — v1 supports non-null positional binds; the typed-null / named-bind case goes through `command`. Documented limitation, revisit if the showcase needs it.

### E. Transactions — autocommit ops + `inTransaction()`
Per-op ops (§D) are autocommit: each borrows a fresh `Connection`, executes, closes. `inTransaction` pins ONE connection for a multi-statement unit:
```java
public <T> Mono<T> inTransaction(final String op, final Function<ReactiveSqlSession, Publisher<T>> work) {
    return mono(op, () -> Mono.usingWhen(
        Mono.fromSupplier(borrow),
        svc -> Mono.usingWhen(svc.getClient().create(),
            conn -> Mono.from(conn.beginTransaction())
                .thenMany(work.apply(new DefaultReactiveSqlSession(conn)))
                .as(f -> Mono.from(f))                 // collect the work's terminal value
                .flatMap(v -> Mono.from(conn.commitTransaction()).thenReturn(v))
                .onErrorResume(e -> Mono.from(conn.rollbackTransaction())
                                        .then(Mono.error(e))),
            Connection::close),
        svc -> Mono.fromRunnable(svc::finalizeOperation)));
}
```
`ReactiveSqlSession` = a small handle over the pinned `Connection` exposing `Flux<T> query(sql, mapper, binds)` / `Mono<Long> update(sql, binds)` (same signatures as the accessor's autocommit ops, minus `op`, running on the pinned connection). The exact commit/rollback composition (single-value vs streaming `work`) is finalised in the plan; the contract is: begin → run work → commit on success / rollback on error → always close the connection. Mirrors the Vitess fresh-vs-explicit-session split (autocommit convenience vs a pinned session for real transactions).

### F. Exception mapping
`R2dbcAccessorException extends AppbaseClientException` — ctors `(Throwable)` / `(String, Throwable)`; static `map(Throwable, Function<Throwable,Throwable> fallback)` unwrapping `CompletionException`/`ExecutionException` → cause (defensive — R2DBC surfaces errors via `onError` directly, but a driver may wrap), then `cause instanceof io.r2dbc.spi.R2dbcException` → `new R2dbcAccessorException(cause)`, else fallback. Shared by all ops. `mapError` = `R2dbcAccessorException.map(t, super::mapError)`; `isNotable` = true.

### G. Metrics
Spine `Timer` per op (inherited) **plus config-gated native-pool gauges**: `ConnectionPool.getMetrics()` returns `Optional<PoolMetrics>` (`acquiredSize`, `idleSize`, `pendingAcquireSize`, `allocatedSize`, `maxAllocatedSize`) — bind per-key-tagged gauges on `initImpl`, unbind on `shutdownImpl`, config-gated + default-off, mirroring the jasync pool-counter / ZooKeeper connection-state-gauge pattern. A dedicated metrics task.

### H. Drivers, README, and testing
- **SPI-generic:** code to `io.r2dbc.spi` (`ConnectionFactory`/`Connection`/`Statement`/`Result`/`Row`/`RowMetadata`); the concrete driver is chosen by config/URL. Main deps: `io.r2dbc:r2dbc-spi` + `io.r2dbc:r2dbc-pool` + `reactor-core` (managed). `r2dbc-mariadb` (`org.mariadb:r2dbc-mariadb`) + `r2dbc-postgresql` (`org.postgresql:r2dbc-postgresql`) declared **optional** for users. `io.r2dbc:r2dbc-h2` at **test** scope.
- **README (`appbase-client-r2dbc/README.md`) — REQUIRED deliverable:** state prominently that this module is **reactive-only — it has NO sync/blocking accessor**; for the synchronous/blocking SQL face use **`appbase-client-jdbc`** (link it). Cover: the R2DBC SPI + driver-by-config model, keyed config, `getReactiveAccessor()`/keyed, the op surface + `inTransaction`, and the supported/optional drivers. **Add a reciprocal note to `appbase-client-jdbc/README.md`** (create if absent): "For a non-blocking/reactive SQL face, see `appbase-client-r2dbc` (P-027 R2b)."
- **Testing:**
  - **Dockerless, non-gated, CI-runnable IT** via `r2dbc-h2` (in-memory `r2dbc:h2:mem:///…`): a full reactive round-trip — `CREATE TABLE` → `INSERT` (via `update`) → `query` (asserts the row via a mapper) → `inTransaction` (an update that COMMITS, and a second that ROLLS BACK on a thrown error — assert the rollback left no row) → `DROP`. This is the **first twin with a CI-green IT** (no gate, no infra).
  - **Gated real-DB IT** via `r2dbc-mariadb` — the umbrella's called-out **Vitess-over-MySQL-wire reactive-SQL IT** (Vitess speaks the MariaDB/MySQL protocol) against MariaDB / vtgate:15306. Gated by a new `-DR2dbcClientServiceTests.enabled` (default false); ties to J-018 clause (a)'s Vitess infra. `*IT` + no failsafe → needs `-Dtest=`.

## Components (each independently testable)

- `pom.xml` + `di/guice/R2dbcClientServiceGuiceModule` — module scaffold, bundle `Export-Package`, deps.
- `configuration/{R2dbcClientServiceConfiguration, Default…}` and `{R2dbcClientIntegrationServiceConfiguration, Default…}` — service + per-key connection/pool config; the reactive-config getter.
- `{R2dbcClientService, DefaultR2dbcClientService}` — service + acquisition getters.
- `{R2dbcClientIntegrationService, DefaultR2dbcClientIntegrationService}` — builds/holds/disposes the native `ConnectionPool` per key.
- `accessor/R2dbcAccessorException` — the mapper.
- `accessor/reactive/{ReactiveR2dbcAccessor, ReactiveSqlSession, DefaultReactiveSqlSession}` — the accessor, the transaction session handle.
- `R2dbcClientServiceTestHelper` + tests (`…ConfigurationTest`, `R2dbcAccessorExceptionTest`, `ReactiveR2dbcAccessorTest` with an H2-backed or mocked `ConnectionFactory`), the H2 IT, the gated MariaDB IT.
- `README.md` (module) + a note in `appbase-client-jdbc/README.md`.

## Error handling

- Per-op: connection borrowed via `usingWhen` (inner) is closed on complete/error/cancel; the appbase integration-service (outer) is finalized likewise. R2DBC errors (`R2dbcException`) map via `mapError` → `R2dbcAccessorException`.
- `inTransaction`: begin → work → commit on success / rollback on error → connection always closed (rollback error is chained, original error preserved).
- `command`: same borrow/close; caller owns the `Publisher` they return.

## Testing

- **Unit:** reuse the `CoreFixtures` mock-spine (from the R2a twin tests). For the accessor, prefer a **real in-memory H2 `ConnectionFactory`** over mocking the R2DBC SPI (Row/Result are awkward to mock and only valid inside `map()`) — a lightweight `r2dbc:h2:mem` factory gives real `Result`/`Row`. Assert: `query` maps rows; `update` returns the affected count; `inTransaction` commits on success and rolls back on a thrown error; `R2dbcException` → `R2dbcAccessorException`; the connection is closed and the integration service finalized on success AND error; spine `Timer` recorded. Pool-metrics gauge seams unit-tested (config gate, bind/unbind, per-key tag).
- **IT:** the H2 dockerless round-trip (non-gated) + the gated MariaDB/Vitess round-trip (§H).

## Constraints (binding)

- Every Java file: Apache header, Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`; **no FQCNs** (imports only). Document at the code site: reactive-only (no sync face — see JDBC); the native `ConnectionPool` borrow-via-`usingWhen`; the `inTransaction` begin/commit/rollback/close contract; the non-null-positional-binds limitation.
- Dependency versions only in root `<dependencyManagement>` (add r2dbc-spi/r2dbc-pool/r2dbc-h2/r2dbc-mariadb/r2dbc-postgresql; reactor already managed).
- Build with `install` (bnd Export-Package on `.reactive` fails under `test`): `mvn -q -pl appbase-java/appbase-client/appbase-client-r2dbc -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`. After each build: ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): file and plugin deleted; no post-build cleanup needed.**.
- Gated IT → J-018 (new clause (n)). Learnings → `appbase-java/LEARNING.md`. Wire the new module into the `appbase-client` aggregator pom.

## Delivery (~6 tasks / 3 PRs)

New sub-issue off the epic. Each task: TDD (Red→Green), fresh implementer subagent → task reviewer; a whole-branch review before each PR's merge.

- **PR 1 — foundation + core accessor** (`feature/P-027-R2b-r2dbc-t1t3`): T1 module scaffold (pom/bundle/deps, guice module, aggregator wiring) + service/config + integration service holding the native `ConnectionPool` per key + reactive-config getter. T2 `R2dbcAccessorException` + `ReactiveR2dbcAccessor` core ops (`query`/`update`/`command` + the nested-`usingWhen` borrow). T3 `getReactiveAccessor()`/keyed.
- **PR 2 — transactions + metrics** (`…-t4t5`): T4 `inTransaction` + `ReactiveSqlSession`/`DefaultReactiveSqlSession` + `batch`. T5 config-gated native-pool metrics gauges.
- **PR 3 — ITs + docs** (`…-t6t7`): T6 dockerless non-gated H2 round-trip IT. T7 gated MariaDB/Vitess IT + `README.md` (module, no-sync + JDBC link) + JDBC README note + build-guide "Reactive SQL (R2DBC)" section + J-018 clause (n).
- **Post-merge:** roll up into the epic; `LEARNING.md` R2b section; J-018 clause (n) on develop. **This closes R2b.** Next per delivery order: R1 (reactive web) → R3 → R4 (incl. deferred Hibernate-Reactive) → R5.

## Self-review notes

- Decisions A–H mapped: A→T1 (module) · B→T1 (service/config/pool) · C→T2/T3 (borrow + acquisition) · D→T2 (ops) · E→T4 (transactions) · F→T2 (exception) · G→T5 (metrics) · H→T6/T7 (drivers/README/IT). Reactive-only (no `getAccessor`), key type `TextValue`, native `ConnectionPool` client, R2DBC-Publisher→Flux direct bridge. No sync twin — README + JDBC cross-link cover it. H2 gives the first CI-runnable twin IT; MariaDB IT satisfies the umbrella's Vitess-SQL requirement.
