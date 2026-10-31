# appbase-client-jdbc

Blocking SQL for Appbase, over JDBC with a HikariCP-pooled `javax.sql.DataSource` per client key.

`JdbcClientService extends AppbaseClientService<DataSource, TextValue>`. This module has no
accessor layer: it hands you the pooled `DataSource` and you use JDBC directly.

```java
final DataSource dataSource = jdbcClientService.getClientIntegrationService().getClient();
```

For higher-level blocking access over the same pool, see `appbase-client-jdbi` (fluent SQL) and
`appbase-client-jpa` (Hibernate ORM).

> ### Looking for non-blocking SQL?
>
> See [`appbase-client-r2dbc`](../appbase-client-r2dbc/README.md) (P-027 R2b) — the reactive SQL
> face, over R2DBC. It is reactive-only (no synchronous accessor), just as this module is
> blocking-only. The two are siblings: pick the one matching your call style rather than blocking
> on a reactive driver or wrapping a blocking one.

## Tests

The JDBC tests are gated behind the `JdbcClientServiceTests.enabled` system property (default
`false` in `appbase-client/pom.xml`) because they need a real MariaDB:

```shell
./infra/docker/mariadb/run-mariadb.sh

mvn test -pl appbase-client/appbase-client-jdbc -DJdbcClientServiceTests.enabled=true
```

Coordinates live in `src/test/resources/appbase-client-jdbc.yaml`.
