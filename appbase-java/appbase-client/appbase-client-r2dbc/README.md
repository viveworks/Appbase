# appbase-client-r2dbc

Genuinely non-blocking SQL for Appbase, over [R2DBC](https://r2dbc.io/). The reactive counterpart
of the blocking `appbase-client-jdbc`.

> ## This module is reactive-only. There is no synchronous accessor.
>
> There is no `getAccessor()` here, and there never will be — only `getReactiveAccessor()`.
> **For synchronous/blocking SQL, use [`appbase-client-jdbc`](../appbase-client-jdbc/README.md).**
>
> This is a deliberate design decision, not a gap. A driver whose entire purpose is non-blocking
> access gains nothing from a synchronous face, and blocking on it would tie up a thread for the
> duration of every call — exactly what choosing R2DBC was meant to avoid. The two modules are
> siblings, not layers: pick the one matching your call style.
>
> Note the asymmetry: `appbase-client-jdbc` has no accessor layer at all — it hands you the pooled
> `DataSource` directly. This module does have one, because bridging the R2DBC SPI onto Appbase's
> reactive spine (metrics, tracing, retry, timeout) is what `ReactiveR2dbcAccessor` exists to do.

Distinct from the Vitess reactive twin (`appbase-client-vitess`), which is object-query gRPC.
This module is SQL.

## The client model

R2DBC is to JDBC as `ConnectionPool` is to `DataSource`:

| | Blocking | Reactive |
|---|---|---|
| Module | `appbase-client-jdbc` | `appbase-client-r2dbc` |
| Native client | `javax.sql.DataSource` (HikariCP) | `io.r2dbc.pool.ConnectionPool` |
| Service | `JdbcClientService` | `R2dbcClientService` |
| Access | the pooled `DataSource`, used directly | `ReactiveR2dbcAccessor` |

`R2dbcClientService extends AppbaseClientService<ConnectionPool, TextValue>`. Appbase's
config-by-key model is preserved: one native `ConnectionPool` per key.

The driver is chosen **by configuration**, not by dependency: `r2dbc-spi` and `r2dbc-pool` are
required, and the concrete driver is resolved through the R2DBC SPI's `ServiceLoader` lookup.
`r2dbc-mariadb` and `r2dbc-postgresql` ship as `optional` dependencies (declare the one you use
in your own POM); `r2dbc-h2` is present at `test` scope.

## Acquisition

```java
// Default client.
final ReactiveR2dbcAccessor accessor = r2dbcClientService.getReactiveAccessor();

// Keyed client.
final ReactiveR2dbcAccessor keyed =
        r2dbcClientService.getReactiveAccessor(AppbaseClientUtil.textValueKey(new TextValue("reporting")));
```

Acquisition is lazy and keyed-free in the same shape as the Cassandra/S3/Vitess twins: nothing is
connected at assembly time. Each subscription borrows a pooled integration service, then a real
`Connection` from its native pool, and returns both on completion, error, **or cancellation**.

## Operations

`query`, `update`, `batch`, and `command` run in **autocommit** against a fresh connection per
subscription. `inTransaction` is the managed-transaction face instead.

```java
Flux<String> names = accessor.query("select", "SELECT name FROM t WHERE id = ?",
        (row, meta) -> row.get("name", String.class), 1);

Mono<Long> inserted = accessor.update("insert", "INSERT INTO t VALUES (?, ?)", 1, "a");

Mono<Long> batched  = accessor.batch("batch", "INSERT INTO t VALUES (?, ?)",
        List.of(new Object[] {2, "b"}, new Object[] {3, "c"}));

// Escape hatch: any driver call against the borrowed Connection.
Flux<Result> raw = accessor.command("command", conn -> conn.createStatement("SELECT 1").execute());
```

### `inTransaction`

Pins **one** connection for the whole `work` function, wrapping it in
`beginTransaction`/`commitTransaction`/`rollbackTransaction` rather than autocommit. `work` runs
against a `ReactiveSqlSession` bound to that same connection.

```java
Mono<Long> moved = accessor.inTransaction("transfer", session ->
        Mono.from(session.update("UPDATE acct SET bal = bal - ? WHERE id = ?", 10, 1))
                .then(Mono.from(session.update("UPDATE acct SET bal = bal + ? WHERE id = ?", 10, 2))));
```

Semantics worth knowing:

- `work` must emit **at most one** value — its `Publisher` is collapsed via `Mono.from(...)` before
  the commit step. Reduce multiple statements yourself (`.then(...)`, `collectList()`).
- "At most one" **includes zero**. `work` completing empty is a success, so it still commits.
- If `work` fails, the rollback is attempted and its own failure is swallowed, so the caller always
  sees the **original** error rather than a rollback failure masking it.

### Limitation: non-null positional binds

Binds are positional and **non-null only**. `Statement#bind(int, Object)` rejects `null` — R2DBC
requires the SQL type for a null bind via `Statement#bindNull(int, Class)`. Callers needing a typed
null bind must drop to `command(...)` and bind the `Statement` themselves.

## Configuration

`url` **wins outright**: when set, it is handed to `ConnectionFactoryOptions.parse(url)` and every
discrete field below — including `user`/`password` — is ignored. Use discrete fields when you need
credentials.

```yaml
defaultDefaultR2dbcClientIntegrationServiceConfiguration:
  r2dbcConnectionConfiguration:
    url: "r2dbc:h2:mem:///mydb;DB_CLOSE_DELAY=-1"   # or leave null and use the fields below
    driver: "mariadb"
    host: "localhost"
    port: 3306
    database: "appbase-test-mariadb-db"
    user: "root"
    password: "password"
  r2dbcPoolConfiguration:
    maxSize: 10
    initialSize: 0
    maxIdleTimeSeconds: 30
```

Native `ConnectionPool` metrics gauges are published when `driverMetricsEnabled` is `true`
(the default).

## Tests

**`ReactiveR2dbcAccessorH2Test` is ungated and runs in a normal build.** H2 is in-memory and needs
no infra, which makes this the first P-027 reactive twin with real CI coverage of a live backend.

```shell
mvn test -pl appbase-client/appbase-client-r2dbc
```

**`ReactiveR2dbcAccessorMariaDbIT` is gated and needs a server.** It is skipped twice over — by the
`R2dbcClientServiceTests.enabled` system property (default `false`) and by its `*IT` suffix, which
Surefire does not select. Both must be overridden:

```shell
# Start MariaDB (reuses the JDBC twin's container).
./appbase-client/appbase-client-jdbc/infra/docker/mariadb/run-mariadb.sh

mvn test -pl appbase-client/appbase-client-r2dbc \
    -DR2dbcClientServiceTests.enabled=true \
    -Dtest=ReactiveR2dbcAccessorMariaDbIT
```

### Running the same IT against Vitess

Vitess speaks the MySQL wire protocol, so `r2dbc-mariadb` talks to `vtgate` unchanged. Point `port`
at `vtgate` (15306) instead of MariaDB (3306) in
`src/test/resources/appbase-client-r2dbc-mariadb.yaml` and run the command above. This is the
umbrella's reactive-SQL-over-Vitess coverage.

## See also

- [`appbase-client-jdbc`](../appbase-client-jdbc/README.md) — the blocking SQL face.
- `build-guide.md` § *Reactive SQL (R2DBC)*.
