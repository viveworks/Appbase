# Part 2 — Client integrations

[Guide index](README.md) · previous: [install and first service](01-install-and-first-service.md) · next: [service modules](03-service-modules.md)

A **client integration** wraps one external system (a database, a cache, a message broker, a mail
server, a cloud API) behind a uniform Appbase client service. The tree has **19 client directories**
under `appbase-java/appbase-client/`: `appbase-client-core` (the shared framework) plus **18
integrations**. The `cloud` integration is a parent that holds three AWS sub-modules.

Everything below was read from the tree. Where a statement could not be verified it is marked.
Nothing here was run against live infrastructure.

## How every client is wired

All integrations follow one pattern, so learn it once.

**1. Add the module.** Depend on the `appbase-client-<name>` artifact (plus `appbase-bootstrap-core`, which
holds the factories). The coordinates' groupId is a placeholder pending internal work item 2546, as in
[part 1](01-install-and-first-service.md). `appbase-client-r2dbc` is managed by `appbase-bom/pom.xml`,
so it needs no explicit version when that BOM is imported.

**2. Write a YAML file.** Each integration reads a YAML document with `autoStart: true` and a key named
`defaultDefault<Name>ClientIntegrationServiceConfiguration` for the default client. Most also accept
`keyedDefault<Name>ClientIntegrationServiceConfigurations` for further clients selected by a key. The
sample file for each module is listed below. The [configuration wizard](05-config-wizard.md) can
generate these documents.

**3. Register a factory.** `appbase-bootstrap-core` has a `…ClientServiceFactory` per integration under
`appbase-java/appbase-bootstrap/appbase-bootstrap-core/src/main/java/com/viveworks/appbase/bootstrap/client/factory/`.
You pass it to `AppbaseClientServices.builder().from(core).with(…)`. The factories documented in their
Javadoc (Redis and Vitess JDBC were read in full) accept five forms:

| Constructor argument | Meaning |
|---|---|
| a pre-built client service | wrap an instance you made yourself |
| a `Default…ClientServiceConfiguration` object | configure in code |
| a classpath YAML path such as `classpath:///appbase-client-redis.yaml` | load the YAML |
| a `ConfigurationStore` plus a raw config name | load from any configuration store |
| nothing | use configuration already loaded into the core configuration service |

A few factories take two YAML paths (Druid takes a shared-resources file and a client file), or omit
some forms. Check the class before relying on a form.

The YAML path is not the only form. A factory also accepts a `Default…ClientServiceConfiguration`
object built directly in code, with no YAML file at all. The example below is composed across the
three classes named just after it, not a verbatim excerpt of any one of them, so it carries no
`snippet-source` marker (this file's own convention for an illustrative example, same as the
build/YAML/XML examples elsewhere in this guide) — the classes and paths it names are real, per
the citations below:

```java
final RedisClientServiceConfiguration redisConfig = DefaultRedisClientServiceConfiguration.builder()
        .defaultDefaultRedisClientIntegrationServiceConfiguration(
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .hostPortConfiguration(HostPortConfiguration.builder()
                                .host("localhost")
                                .port(6379)
                                .build())
                        .build())
        .build();

final AppbaseClientServices clientServices = AppbaseClientServices.builder()
        .from(coreServices)
        .with(new RedisClientServiceFactory(redisConfig))
        .build();
```

Source for the configuration classes' real builder setters:
`appbase-java/appbase-client/appbase-client-redis/src/main/java/com/viveworks/appbase/client/service/redis/configuration/DefaultRedisClientServiceConfiguration.java`
and `DefaultRedisClientIntegrationServiceConfiguration.java` in the same directory; `HostPortConfiguration`
is `appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/util/net/HostPortConfiguration.java`.
The YAML form above remains the recommended default for anything beyond a quick local test — it keeps
connection details out of compiled code.

**4. Use the service.** `AppbaseClientServices.get(RedisClientService.class)` returns the registered
service. Every client service hands out the native client (for example a Hikari `DataSource`, or
a reactive accessor) for its key.

The complete, working chain is in `StackTestMain` (it needs the Docker infrastructure of
[part 6](06-docker-infra.md)):

<!-- snippet-source: appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java:220-238 -->
```java
final AppbaseClientServices clientServices = AppbaseClientServices.builder()
        .from(coreServices)
        .with(new AerospikeClientServiceFactory("classpath:///appbase-stacktest-aerospike.yaml"))
        .with(new DruidSqlClientServiceFactory(
                "classpath:///appbase-stacktest-druid-sql-shared-resources.yaml",
                "classpath:///appbase-stacktest-druid-sql.yaml"))
        .with(new ElasticsearchRestClientFactory("classpath:///appbase-stacktest-elasticsearch-low-level.yaml"))
        .with(new FirebaseAppClientServiceFactory("classpath:///appbase-stacktest-firebase-app.yaml"))
        .with(new FirebaseAuthClientServiceFactory("classpath:///appbase-stacktest-firebase-auth.yaml"))
        .with(new VitessJdbcClientServiceFactory("classpath:///appbase-stacktest-vitess-jdbc.yaml"))
        .with(new VitessJdbiClientServiceFactory("classpath:///appbase-stacktest-vitess-jdbi.yaml"))
        .with(new VitessGrpcClientServiceFactory("classpath:///appbase-stacktest-vitess-grpc.yaml"))
        .with(new AwsSdkHttpClientServiceFactory("classpath:///appbase-stacktest-aws-http-client.yaml"))
        .with(new AwsSdkAsyncHttpClientServiceFactory(
                "classpath:///appbase-stacktest-aws-async-http-client.yaml"))
        .with(new S3ClientsServiceFactory("classpath:///appbase-stacktest-aws-s3.yaml"))
        .with(new ZooKeeperClientServiceFactory("classpath:///appbase-stacktest-zookeeper.yaml"))
        .with(new RedisClientServiceFactory("classpath:///appbase-stacktest-redis.yaml"))
        .build();
```

Source: `appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java`, lines 220-238.

A complete reference set of client YAML files, one per integration used above, is under
`appbase-stacktest/src/main/resources/` (`appbase-stacktest-*.yaml`).

### Configuration values and secrets

Configuration is YAML (or any `ConfigurationStore`). The sample files contain local-development
credentials such as `root` / `password` for a throwaway database container. **Do not copy those into
anything real.** Among the client modules only Firebase authentication reads environment variables
directly (in `DefaultFirebaseAuthClientIntegrationService`). The core module also has an environment
resolver and a system-property configuration store
(`appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/service/configuration/accessor/environment/DefaultEnvironmentResolver.java`).
There is no documented per-client environment-variable key. For the `-container.yaml` twin files that point the same sample at a container host, see
[part 6](06-docker-infra.md).

## The integrations at a glance

Package prefix for all Java types below is `com.viveworks.appbase.client.service.` unless stated. The
sample YAML lives in `src/test/resources/` of each module.

| Module | Wraps | Guice module | Factory in bootstrap-core |
|---|---|---|---|
| `appbase-client-core` | the shared client framework | `client.core.AppbaseClientCoreGuiceModule` | built automatically (`withAppbaseClientCore…` to override) |
| `appbase-client-aerospike` | Aerospike key-value store | `aerospike.AerospikeClientServiceGuiceModule` | `AerospikeClientServiceFactory` |
| `appbase-client-cassandra` | Cassandra driver | `cassandra.CassandraClientServiceGuiceModule` | `CassandraClientServiceFactory` |
| `appbase-client-clamav` | ClamAV antivirus daemon | `clamav.ClamAVClientServiceGuiceModule` | `ClamAVClientServiceFactory` |
| `appbase-client-cloud` | AWS (HTTP clients, S3) | `…cloud.aws.s3.S3ClientsServiceGuiceModule` and `…httpclient.AwsSdkAsyncHttpClientServiceGuiceModule` | `AwsSdkHttpClientServiceFactory`, `AwsSdkAsyncHttpClientServiceFactory`, `S3ClientsServiceFactory` |
| `appbase-client-druid` | Druid over SQL (JDBC/Avatica) | `druid.sql.DruidSqlClientServiceGuiceModule` | `DruidSqlClientServiceFactory` |
| `appbase-client-elasticsearch` | Elasticsearch low-level REST client | `elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientServiceGuiceModule`, `…restclient.api.ElasticsearchRestClientGuiceModule` | `ElasticsearchClientServiceFactory`, `ElasticsearchRestClientFactory` |
| `appbase-client-firebase` | Firebase app and auth admin | `firebase.app.FirebaseAppClientServiceGuiceModule`, `firebase.auth.FirebaseAuthClientServiceGuiceModule` | `FirebaseAppClientServiceFactory`, `FirebaseAuthClientServiceFactory` |
| `appbase-client-jasync` | non-blocking MySQL/MariaDB (jasync-sql) | `jasync.JasyncClientServiceGuiceModule` | `JasyncClientServiceFactory` |
| `appbase-client-jdbc` | blocking SQL, Hikari `DataSource` per key | `jdbc.JdbcClientServiceGuiceModule` | none by that name (see below) |
| `appbase-client-jdbi` | fluent SQL over the JDBC pool | `jdbi.JdbiClientServiceGuiceModule` | none by that name (see below) |
| `appbase-client-jpa` | Hibernate ORM / JPA | `jpa.JpaClientServiceGuiceModule` | `JpaClientServiceFactory` |
| `appbase-client-kafka` | Kafka producer and consumer | `kafka.producer.KafkaProducerClientServiceGuiceModule`, `kafka.consumer.KafkaConsumerClientServiceGuiceModule` | `KafkaProducerClientServiceFactory`, `KafkaConsumerClientServiceFactory` |
| `appbase-client-r2dbc` | reactive SQL (R2DBC) | `r2dbc.R2dbcClientServiceGuiceModule` | none (see below) |
| `appbase-client-redis` | Redis | none found | `RedisClientServiceFactory` |
| `appbase-client-smtp` | outgoing mail | `smtp.SmtpClientServiceGuiceModule` | `SmtpClientServiceFactory` |
| `appbase-client-spark` | Apache Spark session | `spark.SparkClientServiceGuiceModule` | `SparkClientServiceFactory` |
| `appbase-client-vitess` | Vitess over gRPC, JDBC, jasync, JDBI | `vitess.VitessGrpcClientServiceGuiceModule` | `VitessGrpcClientServiceFactory`, `VitessJdbcClientServiceFactory`, `VitessJdbiClientServiceFactory` |
| `appbase-client-zookeeper` | ZooKeeper via Curator | `zookeeper.ZooKeeperClientServiceGuiceModule` | `ZooKeeperClientServiceFactory` |

**JDBC, JDBI and R2DBC have no factory of their own** in `appbase-bootstrap-core`. The Vitess factories
(`VitessJdbcClientServiceFactory` builds a `JdbcClientService`, `VitessJdbiClientServiceFactory` a
`JdbiClientService`) are the ones the repository's own stack test uses for them. Whether they suit a
plain MariaDB or PostgreSQL setup is **not verified here**. Otherwise implement `AppbaseClientServiceFactory`
yourself, or build the service with the module's Guice module. The Jasync integration has its own
factory, and the support-message and user-account stores in [part 3](03-service-modules.md) take a
`JdbiClientService` directly.

## Per-module notes

Each entry gives what it does, the configuration class, the sample YAML (top-level key), and where to
start a local instance. Only two modules have a README of their own:
[`appbase-java/appbase-client/appbase-client-jdbc/README.md`](../../appbase-java/appbase-client/appbase-client-jdbc/README.md) and
[`appbase-java/appbase-client/appbase-client-r2dbc/README.md`](../../appbase-java/appbase-client/appbase-client-r2dbc/README.md).

### `appbase-client-core`

The framework the other 18 build on: a client pool, a pool manager and shared resources (object pools
for keys, host/port values and so on). Configuration class `DefaultAppbaseClientCoreConfiguration`
(`client.core`); sample `appbase-java/appbase-client/appbase-client-core/src/test/resources/appbase-client-core.yaml`,
top-level keys `autoStart` and `defaultAppbaseClientSharedResourcesConfiguration`. You normally do not
configure it: `AppbaseClientServices.builder().from(core)` creates it, and `withAppbaseClientCore(…)` or
`withAppbaseClientCoreFactory(…)` replaces it.

### `appbase-client-aerospike`

Aerospike key-value access. Configuration `DefaultAerospikeClientServiceConfiguration` with
`AerospikeNodeConfiguration` entries (`hostnameOrIp`, `port`, `tlsName`); sample
`appbase-client-aerospike.yaml`, key `defaultDefaultAerospikeClientIntegrationServiceConfiguration`.
Also reused as the session store and the OAuth token-cache backend ([part 3](03-service-modules.md)).
Local instance: `appbase-java/appbase-client/appbase-client-aerospike/infra/docker/aerospike/` (port 3000).

### `appbase-client-cassandra`

Cassandra driver client. Configuration `DefaultCassandraClientServiceConfiguration` (hosts and ports, local
datacenter, keyspace, optional driver config loader); sample `appbase-client-cassandra.yaml`, key
`defaultDefaultCassandraClientIntegrationServiceConfiguration`. Local instance:
`appbase-java/appbase-client/appbase-client-cassandra/infra/docker/cassandra/` (port 9042).

### `appbase-client-clamav`

Antivirus scanning through a ClamAV daemon socket. Configuration `DefaultClamAVClientServiceConfiguration`
(host and port, socket options); samples `appbase-client-clamav.yaml` and
`appbase-client-clamav-shared-resources.yaml`, key `defaultDefaultClamAVClientIntegrationServiceConfiguration`.
Local instance: `appbase-java/appbase-client/appbase-client-clamav/infra/docker/clamav/` (port 3310).

### `appbase-client-cloud`

A pom-only parent for AWS only today (its description defers other providers to 2.0.0). Three
sub-modules under `appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/`: `common`
(retry, credentials and override-configuration factories), `httpclient` (sync and async AWS SDK HTTP
clients) and `s3` (S3 clients). Configuration `DefaultS3ClientsServiceConfiguration`,
`DefaultAwsSdkHttpClientServiceConfiguration`, `DefaultAwsSdkAsyncHttpClientServiceConfiguration`;
samples `appbase-client-aws-s3.yaml`, `appbase-client-aws-http-client.yaml`,
`appbase-client-aws-async-http-client.yaml`. **Local S3 emulator:** the infrastructure scripts reference a
MinIO directory that does not exist (see [part 6](06-docker-infra.md)). AWS credentials are
**external and not covered here**.

### `appbase-client-druid`

Druid SQL through the Avatica JDBC remote driver. Configuration `DefaultDruidSqlClientServiceConfiguration`;
samples `appbase-client-druid-sql.yaml` (connection `url` such as
`jdbc:avatica:remote:url=http://localhost:8888/druid/v2/sql/avatica-protobuf/`) and
`appbase-client-druid-sql-shared-resources.yaml`, key `defaultDruidSqlClientIntegrationServiceConfiguration`.
Local instance: `appbase-java/appbase-client/appbase-client-druid/infra/docker/druid/` (router and SQL on 8888).

### `appbase-client-elasticsearch`

Elasticsearch through the low-level REST client, plus a REST-client API variant. Configuration
`DefaultElasticsearchLowLevelRestClientServiceConfiguration` with `HttpHostConfiguration` entries
(`hostname`, `port`, `scheme`); sample `appbase-client-elasticsearch-low-level.yaml`, key
`defaultDefaultElasticsearchRestClientIntegrationServiceConfiguration`. Local instance:
`appbase-java/appbase-client/appbase-client-elasticsearch/infra/docker/elasticsearch/` (ports 9200, 9300).

### `appbase-client-firebase`

Two services: the Firebase **app** (`FirebaseAppClientService`, with `FirebaseAccessTokenConfiguration`)
and Firebase **auth** admin (`FirebaseAuthClientService`). The module has no sample YAML of its own;
use `appbase-stacktest/src/main/resources/appbase-stacktest-firebase-app.yaml` and
`appbase-stacktest/src/main/resources/appbase-stacktest-firebase-auth.yaml`. Local emulator:
`appbase-java/appbase-client/appbase-client-firebase/infra/docker/firebase/` (UI on 4000, auth on 9099).
A real Firebase project and its credentials are **external and not covered here**.

### `appbase-client-jasync`

Non-blocking MySQL/MariaDB or PostgreSQL (jasync-sql); the `databaseType` setting is `MYSQL_OR_MARIADB` or `POSTGRESQL`. Sample
`appbase-client-jasync.yaml`: `databaseType`, `host`, `port`, `database`, `user`, `password`,
`maxActiveConnections`; key `defaultDefaultJasyncClientIntegrationServiceConfiguration`.
Configuration class `DefaultJasyncClientServiceConfiguration`. The sample points at a local MariaDB:
`appbase-java/appbase-client/appbase-client-jdbc/infra/docker/mariadb/`.

### `appbase-client-jdbc`

Blocking SQL. `JdbcClientService` hands you a pooled `javax.sql.DataSource` (HikariCP) per client key;
there is no accessor layer. Configuration `DefaultJdbcClientServiceConfiguration` with
`HikariDataSourceConfiguration` (`dataSourceType`, `serverHostPortConfiguration`, `databaseName`,
`username`, `password`); sample `appbase-client-jdbc.yaml`, key
`defaultDefaultJdbcClientIntegrationServiceConfiguration`. Its tests need MariaDB and are gated by the
`JdbcClientServiceTests.enabled` system property. Local instances:
`appbase-java/appbase-client/appbase-client-jdbc/infra/docker/mariadb/` (3306) and
`appbase-java/appbase-client/appbase-client-jdbc/infra/docker/postgresql/` (5432).

### `appbase-client-jdbi`

Fluent SQL over the JDBC pool (the sample enables the SQL-object plugin). Configuration
`DefaultJdbiClientServiceConfiguration`; sample `appbase-client-jdbi.yaml`, key
`defaultDefaultJdbiClientIntegrationServiceConfiguration`. It refers to a JDBC client service by
configuration. The user-account and support-message JDBI stores in [part 3](03-service-modules.md) are
built on it.

### `appbase-client-jpa`

Hibernate ORM / JPA. Configuration `DefaultJpaClientServiceConfiguration` with an
`entityManagerConfiguration` (`persistenceUnitName`, factory and manager properties); sample
`appbase-client-jpa.yaml`, key `defaultDefaultJpaClientIntegrationServiceConfiguration`. The persistence
unit itself is yours to define.

### `appbase-client-kafka`

Kafka producer and consumer as two services. Configuration
`DefaultKafkaProducerClientServiceConfiguration` and `DefaultKafkaConsumerClientServiceConfiguration`
(bootstrap servers, serializer types, `acks`, `groupId`, subscription topics); samples
`appbase-client-kafka-producer.yaml` and `appbase-client-kafka-consumer.yaml`. Local broker:
`appbase-java/appbase-client/appbase-client-kafka/infra/docker/kafka/` (port 9092). The `docker-*-all.sh`
scripts deliberately do not start Kafka.

### `appbase-client-r2dbc`

Reactive-only SQL over R2DBC (no synchronous accessor; use `appbase-client-jdbc` for blocking calls).
`R2dbcClientService` gives a `ReactiveR2dbcAccessor` over an `io.r2dbc.pool.ConnectionPool` per key.
Configuration `DefaultR2dbcClientServiceConfiguration` with `R2dbcConnectionConfiguration` (`url` or
driver, host, port, database, user, password); samples `appbase-client-r2dbc.yaml` (an in-memory H2 URL)
and `appbase-client-r2dbc-mariadb.yaml`. The R2DBC driver is chosen by configuration: declare the driver
you use in your own dependencies. Details: its README, linked above. The Appbase R2DBC module itself is
managed by the BOM (see [part 1](01-install-and-first-service.md)).

### `appbase-client-redis`

Redis, blocking and reactive accessors (`RedisClientService`, `ReactiveRedisAccessor`). Configuration
`DefaultRedisClientServiceConfiguration` (`client.redis.configuration`); sample
`appbase-java/appbase-client/appbase-client-redis/src/test/resources/appbase-client-redis.yaml`, key
`defaultDefaultRedisClientIntegrationServiceConfiguration` with `hostPortConfiguration.host` and `.port`,
`database`, `username`, `password`, `ssl`, `sslVerifyPeer`, `startTls`, `connectTimeoutMs`. There is no
Guice module in this module; register it through `RedisClientServiceFactory`. Local instance:
`appbase-java/appbase-client/appbase-client-redis/infra/docker/redis/` (port 6379).
Also the OAuth token-cache backend in [part 3](03-service-modules.md).

### `appbase-client-smtp`

Outgoing mail through a Jakarta Mail session. Configuration `DefaultSmtpClientServiceConfiguration` with
`SmtpSessionConfiguration` and `SmtpAuthConfiguration`; sample `appbase-client-smtp.yaml`, key
`defaultDefaultSmtpClientIntegrationServiceConfiguration`. Local mail server:
`appbase-java/appbase-client/appbase-client-smtp/infra/docker/postfix/` (a compose file; its `run`
script publishes no port). Real SMTP credentials are **external and not covered here**.

### `appbase-client-spark`

An Apache Spark session. Configuration `DefaultSparkClientServiceConfiguration` (`appName`, `sparkHome`,
`masterHostPortConfiguration`, executor environment, jars); sample `appbase-client-spark.yaml`, key
`defaultDefaultSparkClientIntegrationServiceConfiguration`. Local cluster:
`appbase-java/appbase-client/appbase-client-spark/infra/docker/spark/` (ports 7077, 8077, 7078, 8078).

### `appbase-client-vitess`

Vitess with four variants: gRPC (`VitessGrpcClientService`), JDBC, MySQL-JDBC, and jasync, plus a JDBI
face through `VitessJdbiClientServiceFactory`. Configuration
`DefaultVitessGrpcClientServiceConfiguration` for gRPC, `TargetConfiguration` and
`StaticAuthCredentialsConfiguration` for targets and credentials. Samples
`appbase-client-vitess-grpc.yaml`, `appbase-client-vitess-jdbc.yaml`, `appbase-client-vitess-mysql-jdbc.yaml`
and `appbase-client-vitess-jasync.yaml`, each with a `-container.yaml` twin. Local instance:
`appbase-java/appbase-client/appbase-client-vitess/infra/docker/vitess/` (see [part 6](06-docker-infra.md)).

### `appbase-client-zookeeper`

ZooKeeper through Curator. Configuration `DefaultZooKeeperClientServiceConfiguration` (hosts, connection and
session timeouts, retry policy); sample `appbase-client-zookeeper.yaml`, key
`defaultDefaultZooKeeperClientIntegrationServiceConfiguration`. Local instance:
`appbase-java/appbase-client/appbase-client-zookeeper/infra/docker/zookeeper/` (ports 2181, 2888, 3888).

## Adding a new client

The repository carries an assistant recipe that scaffolds a new client module; see
[part 8](08-agent-integration.md).
