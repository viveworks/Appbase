# Appbase Module and Service Catalog — Release 1

The authoritative list of what Appbase Release 1 ships, one entry per artifact the build produces.
It is **checked mechanically** against the generated shipped-artifact inventory
([`docs/api-inventory/`](api-inventory/README.md)):

```sh
python3 scripts/catalogue-check.py                    # exits non-zero on any catalogue- or inventory-side problem
python3 scripts/catalogue-check.py --catalogue-only   # gates catalogue drift only; open inventory decisions are printed, not gated
```

**Where an entry goes.** Each `##` section admits only the support states its heading names: *Deferred*
sections hold `deferred-unsupported`, *Open decisions* hold `unclassified`, *Internal* sections hold
`internal-implementation`, and every other section holds shipped capability (`supported-public`, or
`generated-only` for what ships inside a tool). The check fails on an entry in the wrong section, and it
ignores entries inside code blocks or HTML comments, which a reader does not see.

**How to read an entry.** The heading is the artifact's `groupId:artifactId` (or `pub:<name>`).
`Coordinates`, `Module` and `Support` must equal the inventory. The other fields are written for people.
`Support` is one of:

| Support | Meaning |
|---|---|
| `supported-public` | Shipped in Release 1 and supported for consumers |
| `internal-implementation` | Built and published only because other artifacts need it; not a consumer dependency |
| `generated-only` | Built and shipped only inside another artifact |
| `deferred-unsupported` | Built, but explicitly not part of Release 1 |
| `unclassified` | **An open owner decision. Not shipped until it is decided**, and the check fails until then |

Nothing here is aspirational: an entry exists only for an artifact the build produces, and the check
rejects any entry the inventory does not contain. Modules planned for later releases (for example the gRPC
face, P-038, or multi-cloud clients, P-018) do not appear at all.

⚠️ **Release-1 status.** Every release coordinate is pinned to `1.0.0`; Javadoc publication
remains pending across these artifacts.

## Supported-public artifacts at a glance

<!-- api-inventory:supported-public:begin -->
| Artifact | What it is |
|---|---|
| `com.viveworks.appbase:appbase-bom` | Bill of Materials |
| `com.viveworks.appbase:appbase-core` | The Appbase core runtime every other module builds on |
| `com.viveworks.appbase:appbase-bootstrap-core` | Bootstraps a complete Appbase stack |
| `com.viveworks.appbase:appbase-bootstrap-servlet` | Container-neutral servlet-API deployment and registration layer shared by the Jetty, Tomcat and Undertow faces |
| `com.viveworks.appbase:appbase-bootstrap-servlet-jetty` | Embedded Jetty 11 servlet-container implementation of `AppbaseWebServer` |
| `com.viveworks.appbase:appbase-bootstrap-servlet-tomcat` | Embedded Tomcat servlet-container implementation of `AppbaseWebServer` |
| `com.viveworks.appbase:appbase-bootstrap-servlet-undertow` | Embedded Undertow servlet-container implementation of `AppbaseWebServer` |
| `com.viveworks.appbase:appbase-bootstrap-reactor-netty` | Reactor Netty (non-blocking) implementation of `AppbaseWebServer` |
| `com.viveworks.appbase:appbase-client-core` | Shared foundation of every Appbase data client |
| `com.viveworks.appbase:appbase-client-aerospike` | Managed Aerospike client for Appbase services |
| `com.viveworks.appbase:appbase-client-cassandra` | Managed Cassandra client for Appbase services |
| `com.viveworks.appbase:appbase-client-druid` | Managed Apache Druid SQL client for Appbase services |
| `com.viveworks.appbase:appbase-client-elasticsearch` | Managed Elasticsearch client for Appbase services |
| `com.viveworks.appbase:appbase-client-firebase` | Managed Firebase Admin SDK clients (app and auth) for Appbase services |
| `com.viveworks.appbase:appbase-client-jdbc` | Managed JDBC client for Appbase services |
| `com.viveworks.appbase:appbase-client-jdbi` | Managed Jdbi client for Appbase services |
| `com.viveworks.appbase:appbase-client-jpa` | Managed JPA client for Appbase services |
| `com.viveworks.appbase:appbase-client-kafka` | Managed Kafka client for Appbase services |
| `com.viveworks.appbase:appbase-client-r2dbc` | Managed reactive R2DBC SQL client for Appbase services |
| `com.viveworks.appbase:appbase-client-redis` | Managed Redis client for Appbase services |
| `com.viveworks.appbase:appbase-client-vitess` | Managed Vitess client for Appbase services |
| `com.viveworks.appbase:appbase-client-zookeeper` | Managed ZooKeeper client for Appbase services |
| `com.viveworks.appbase:appbase-client-cloud-aws-common` | Shared AWS SDK v2 building blocks used by the Appbase AWS clients |
| `com.viveworks.appbase:appbase-client-cloud-aws-httpclient` | Managed AWS SDK v2 HTTP clients (sync Apache, async Netty) shared by the AWS service clients |
| `com.viveworks.appbase:appbase-client-cloud-aws-s3` | Managed Amazon S3 clients plus an object-store accessor for Appbase services |
| `com.viveworks.appbase:appbase-service-core` | Application-service building blocks for Appbase services |
| `com.viveworks.appbase:appbase-service-web` | HTTP layer for Appbase services |
| `com.viveworks.appbase:appbase-service-web-oauth` | OAuth 2.0 / OIDC bearer-token validation for Appbase web services |
| `com.viveworks.appbase:appbase-service-web-oauth-github` | GitHub OAuth provider |
| `com.viveworks.appbase:appbase-service-web-oauth-google` | Google provider for the Appbase OAuth/OIDC filter |
| `com.viveworks.appbase:appbase-service-web-oauth-okta` | Okta provider for the Appbase OAuth/OIDC filter |
| `com.viveworks.appbase:appbase-service-web-oauth-microsoft` | Microsoft Entra ID provider for the Appbase OAuth/OIDC filter |
| `com.viveworks.appbase:appbase-service-web-oauth-apple` | Sign in with Apple provider for the Appbase OAuth/OIDC filter |
| `com.viveworks.appbase:appbase-service-web-oauth-reactive` | Reactive (non-blocking) face of the OAuth filter |
| `com.viveworks.appbase:appbase-service-web-oauth-tokencache-redis` | Redis-backed distributed `TokenCache` for the OAuth filter, replacing the in-memory Caffeine default across service instances |
| `com.viveworks.appbase:appbase-service-web-oauth-tokencache-aerospike` | Aerospike-backed distributed `TokenCache` for the OAuth filter, replacing the in-memory Caffeine default across service instances |
| `com.viveworks.appbase:appbase-service-user` | User-account and user-session services |
| `com.viveworks.appbase:appbase-service-user-account-firebaseauth` | Firebase Authentication backend for user accounts |
| `com.viveworks.appbase:appbase-service-user-account-jdbi` | Jdbi/SQL implementation of the user-account store |
| `com.viveworks.appbase:appbase-service-user-session-aerospike` | Aerospike implementation of the user-session store |
| `com.viveworks.appbase:appbase-service-supportmessage` | Support-message (contact/feedback) service with servlet and JAX-RS endpoints |
| `com.viveworks.appbase:appbase-config-wizard` | Interactive console and web tool that builds valid YAML configuration files for Appbase services |
<!-- api-inventory:supported-public:end -->

## Build and BOM

### `com.viveworks.appbase:appbase-bom`

- **Coordinates:** `com.viveworks.appbase:appbase-bom:1.0.0`
- **Module:** `appbase-bom`
- **Support:** `supported-public`
- **Purpose:** Bill of Materials — the one import that pins every published Appbase artifact to this release's version.
- **Capabilities:** `dependencyManagement` for every artifact it lists, so consumers declare Appbase dependencies without versions
- **Public entry points:** `<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-bom</artifactId><type>pom</type><scope>import</scope></dependency>`
- **Configuration / backend:** Maven `dependencyManagement` import; no runtime configuration
- **Examples / API docs:** usage comment in `appbase-bom/pom.xml`
- **Limitations:** On this epic base the BOM still disagrees with the lean Release 1.0 baseline: it exports the seven *Deferred* artifacts and `appbase-stacktest`, and omits seven supported ones (the four OAuth providers, both token caches, `appbase-client-r2dbc`). A pending BOM update corrects it. Release coordinate: `1.0.0`.

## Core

### `com.viveworks.appbase:appbase-core`

- **Coordinates:** `com.viveworks.appbase:appbase-core:1.0.0`
- **Module:** `appbase-java/appbase-core`
- **Support:** `supported-public`
- **Purpose:** The Appbase core runtime every other module builds on: core service bootstrap, configuration, logging, metrics, tracing, mediator/event queues, validation and JMX management.
- **Capabilities:** Configuration store and accessors (filesystem, JPA, system/environment); exception store; log appenders and filters (Disruptor, Lucene); metrics (JMX, MicroProfile/Micrometer); tracing; mediator event bus; validation; Reactor scheduler support; Guice DI utilities
- **Public entry points:** `AppbaseCore`, `AppbaseCoreBootstrap`, `AppbaseCoreBootstrapConfiguration`, `AppbaseConfiguration`, `AppbaseFactory`, `AppbaseHandler`/`AppbaseHandlerChain`
- **Configuration / backend:** YAML core configuration (`AppbaseCoreBootstrapConfiguration`); no external backend required — the JPA and Lucene stores are optional
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-core.yaml`; Javadoc is not published yet
- **Limitations:** The largest public surface (about 560 public types); the published-vs-internal package split is still undecided. Release coordinate: `1.0.0`.

## Bootstrap and web servers

### `com.viveworks.appbase:appbase-bootstrap-core`

- **Coordinates:** `com.viveworks.appbase:appbase-bootstrap-core:1.0.0`
- **Module:** `appbase-java/appbase-bootstrap/appbase-bootstrap-core`
- **Support:** `supported-public`
- **Purpose:** Bootstraps a complete Appbase stack — core, clients and application services — from configuration, behind a container-neutral web-server abstraction.
- **Capabilities:** Builder/factory containers for core, client and application services; YAML-driven stack assembly; `AppbaseWebServer` abstraction implemented by the servlet containers
- **Public entry points:** `AppbaseCoreServiceFactory`, `AppbaseClientCoreFactory`, `AppbaseClientServiceFactory`, `AppbaseServiceFactory`, `AppbaseWebServer`, `AppbaseCoreServicesConfiguration`
- **Configuration / backend:** YAML bootstrap configuration; the backends are whatever clients the configuration enables
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-bootstrap.yaml`; Javadoc is not published yet
- **Limitations:** Pulls in several client modules transitively (Aerospike, AWS, Cassandra, and others). Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-bootstrap-servlet`

- **Coordinates:** `com.viveworks.appbase:appbase-bootstrap-servlet:1.0.0`
- **Module:** `appbase-java/appbase-bootstrap/appbase-bootstrap-servlet`
- **Support:** `supported-public`
- **Purpose:** Container-neutral servlet-API deployment and registration layer shared by the Jetty, Tomcat and Undertow faces.
- **Capabilities:** Servlet/filter registration against Jakarta Servlet
- **Public entry points:** `AppbaseServletDeployment`
- **Configuration / backend:** None of its own; used through a container module
- **Examples / API docs:** Javadoc is not published yet
- **Limitations:** Not used alone — pick one of the container modules. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-bootstrap-servlet-jetty`

- **Coordinates:** `com.viveworks.appbase:appbase-bootstrap-servlet-jetty:1.0.0`
- **Module:** `appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-jetty`
- **Support:** `supported-public`
- **Purpose:** Embedded Jetty 11 servlet-container implementation of `AppbaseWebServer`.
- **Capabilities:** Serves the bootstrapped stack's servlet and JAX-RS (Jersey) endpoints from an embedded Jetty 11; Guice wiring
- **Public entry points:** the `com.viveworks.appbase.bootstrap.jetty.server` web server and its `…guice` module, selected through `appbase-bootstrap-core`
- **Configuration / backend:** bootstrap YAML web-server section; embedded Jetty 11 (jetty-server/jetty-servlet), no external server needed
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-bootstrap.yaml`; Javadoc is not published yet
- **Limitations:** Choose exactly one container per service. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-bootstrap-servlet-tomcat`

- **Coordinates:** `com.viveworks.appbase:appbase-bootstrap-servlet-tomcat:1.0.0`
- **Module:** `appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-tomcat`
- **Support:** `supported-public`
- **Purpose:** Embedded Tomcat servlet-container implementation of `AppbaseWebServer`.
- **Capabilities:** Serves the bootstrapped stack's servlet and JAX-RS (Jersey) endpoints from an embedded Tomcat; Guice wiring
- **Public entry points:** the `com.viveworks.appbase.bootstrap.tomcat.server` web server and its `…guice` module, selected through `appbase-bootstrap-core`
- **Configuration / backend:** bootstrap YAML web-server section; embedded Tomcat (tomcat-embed-core), no external server needed
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-bootstrap.yaml`; Javadoc is not published yet
- **Limitations:** Choose exactly one container per service. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-bootstrap-servlet-undertow`

- **Coordinates:** `com.viveworks.appbase:appbase-bootstrap-servlet-undertow:1.0.0`
- **Module:** `appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-undertow`
- **Support:** `supported-public`
- **Purpose:** Embedded Undertow servlet-container implementation of `AppbaseWebServer`.
- **Capabilities:** Serves the bootstrapped stack's servlet and JAX-RS (Jersey) endpoints from an embedded Undertow; Guice wiring
- **Public entry points:** the `com.viveworks.appbase.bootstrap.undertow.server` web server and its `…guice` module, selected through `appbase-bootstrap-core`
- **Configuration / backend:** bootstrap YAML web-server section; embedded Undertow (undertow-servlet), no external server needed
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-bootstrap.yaml`; Javadoc is not published yet
- **Limitations:** Choose exactly one container per service. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-bootstrap-reactor-netty`

- **Coordinates:** `com.viveworks.appbase:appbase-bootstrap-reactor-netty:1.0.0`
- **Module:** `appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty`
- **Support:** `supported-public`
- **Purpose:** Reactor Netty (non-blocking) implementation of `AppbaseWebServer`.
- **Capabilities:** Reactive HTTP face over reactor-netty-http
- **Public entry points:** `ReactorNettyAppbaseWebServer`, `ReactorNettyAppbaseWebServerConfiguration`, `ReactorNettyAppbaseServerModule` (Guice), `AppbaseRouteRegistry`
- **Configuration / backend:** bootstrap YAML; embedded Reactor Netty
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-bootstrap.yaml`; Javadoc is not published yet; `appbase-stacktest/README.md` reactive showcase
- **Limitations:** Supported in Release 1 by the owner ruling; `appbase-bom` exports it once the pending BOM update lands. Its admin endpoints are a read-only subset of the ten core ones; the mutating operations stay servlet-only (parity is P-039, 2.0.0). Release coordinate: `1.0.0`.

## Data and integration clients

### `com.viveworks.appbase:appbase-client-core`

- **Coordinates:** `com.viveworks.appbase:appbase-client-core:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-core`
- **Support:** `supported-public`
- **Purpose:** Shared foundation of every Appbase data client: client keys, pooling, pool management, integration-service lifecycle and reactive support.
- **Capabilities:** `AppbaseClientPool` / pool manager; client keys; shared resources; integration-service contract implemented by each client module; Project Reactor support
- **Public entry points:** `AppbaseClientCore`, `AppbaseClientCoreConfiguration`, `AppbaseClientIntegrationService`, `AppbaseClientPool`, `AppbaseClientPoolManager`, `AppbaseClientKey`
- **Configuration / backend:** YAML client-core configuration; no backend of its own
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-client-core.yaml`; Javadoc is not published yet
- **Limitations:** Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-aerospike`

- **Coordinates:** `com.viveworks.appbase:appbase-client-aerospike:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-aerospike`
- **Support:** `supported-public`
- **Purpose:** Managed Aerospike client for Appbase services — creates, pools, configures and health-checks Aerospike connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `AerospikeClientService`, `AerospikeClientServiceConfiguration`, `AerospikeClientIntegrationService`
- **Configuration / backend:** YAML/`AerospikeClientServiceConfiguration`; needs a reachable Aerospike cluster. Built on aerospike-client + aerospike-reactor-client (Micrometer metrics).
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-aerospike.yaml`; Javadoc is not published yet
- **Limitations:** Requires external Aerospike cluster infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-cassandra`

- **Coordinates:** `com.viveworks.appbase:appbase-client-cassandra:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-cassandra`
- **Support:** `supported-public`
- **Purpose:** Managed Cassandra client for Appbase services — creates, pools, configures and health-checks Cassandra connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `CassandraClientService`, `CassandraClientServiceConfiguration`, `CassandraClientIntegrationService`
- **Configuration / backend:** YAML/`CassandraClientServiceConfiguration`; needs a reachable Cassandra cluster (local datacenter configured). Built on DataStax java-driver 4 (core, query builder, mapper, Micrometer metrics).
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** Requires external Cassandra cluster (local datacenter configured) infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-druid`

- **Coordinates:** `com.viveworks.appbase:appbase-client-druid:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-druid`
- **Support:** `supported-public`
- **Purpose:** Managed Apache Druid SQL client for Appbase services — creates, pools, configures and health-checks Apache Druid SQL connections under the Appbase client core.
- **Capabilities:** SQL queries against Druid over Avatica, dispatched through mediator events; result-set handling
- **Public entry points:** `DruidSqlClientService`, `DruidSqlClientServiceConfiguration`, `DruidSqlClientIntegrationService`
- **Configuration / backend:** YAML/`DruidSqlClientServiceConfiguration`; needs a reachable Apache Druid broker. Built on Apache Calcite Avatica.
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-druid-sql.yaml`; stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-druid-sql-shared-resources.yaml`; Javadoc is not published yet
- **Limitations:** Requires external Apache Druid broker infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-elasticsearch`

- **Coordinates:** `com.viveworks.appbase:appbase-client-elasticsearch:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-elasticsearch`
- **Support:** `supported-public`
- **Purpose:** Managed Elasticsearch client for Appbase services — creates, pools, configures and health-checks Elasticsearch connections under the Appbase client core.
- **Capabilities:** Low-level REST client; typed document, index, search (query/sort/multi-search/scroll/point-in-time) and task APIs; reactive variants; `SearchIndexAccessor`
- **Public entry points:** `ElasticsearchLowLevelRestClientService`, `ElasticsearchLowLevelRestClientServiceConfiguration`, `ElasticsearchLowLevelRestClientIntegrationService`
- **Configuration / backend:** YAML/`ElasticsearchLowLevelRestClientServiceConfiguration`; needs a reachable Elasticsearch cluster. Built on the Elasticsearch low-level REST client + Jackson.
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-elasticsearch-low-level.yaml`; Javadoc is not published yet
- **Limitations:** Requires external Elasticsearch cluster infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-firebase`

- **Coordinates:** `com.viveworks.appbase:appbase-client-firebase:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-firebase`
- **Support:** `supported-public`
- **Purpose:** Managed Firebase Admin SDK clients (app and auth) for Appbase services.
- **Capabilities:** Firebase app initialisation; Firebase Auth commands, blocking and reactive
- **Public entry points:** `FirebaseAppClientService`, `FirebaseAuthClientService`, their `…Configuration` types, `FirebaseAuthCommand`
- **Configuration / backend:** YAML Firebase app/auth configuration; needs a Firebase project and service-account credentials (firebase-admin)
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-firebase-app.yaml`; stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-firebase-auth.yaml`; Javadoc is not published yet
- **Limitations:** Requires a real Firebase project or the Firebase emulators; a real project may incur cost. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-jdbc`

- **Coordinates:** `com.viveworks.appbase:appbase-client-jdbc:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-jdbc`
- **Support:** `supported-public`
- **Purpose:** Managed JDBC client for Appbase services — creates, pools, configures and health-checks JDBC connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `JdbcClientService`, `JdbcClientServiceConfiguration`, `JdbcClientIntegrationService`
- **Configuration / backend:** YAML/`JdbcClientServiceConfiguration`; needs a reachable MariaDB/MySQL or PostgreSQL database. Built on HikariCP with the MariaDB and PostgreSQL drivers.
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** Requires external MariaDB/MySQL or PostgreSQL database infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-jdbi`

- **Coordinates:** `com.viveworks.appbase:appbase-client-jdbi:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-jdbi`
- **Support:** `supported-public`
- **Purpose:** Managed Jdbi client for Appbase services — creates, pools, configures and health-checks Jdbi connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `JdbiClientService`, `JdbiClientServiceConfiguration`, `JdbiClientIntegrationService`
- **Configuration / backend:** YAML/`JdbiClientServiceConfiguration`; needs a reachable JDBC database (via `appbase-client-jdbc`). Built on Jdbi 3 (core, SQL objects, postgres) over `appbase-client-jdbc`.
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** Requires external JDBC database (via `appbase-client-jdbc`) infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-jpa`

- **Coordinates:** `com.viveworks.appbase:appbase-client-jpa:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-jpa`
- **Support:** `supported-public`
- **Purpose:** Managed JPA client for Appbase services — creates, pools, configures and health-checks JPA connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `JpaClientService`, `JpaClientServiceConfiguration`, `JpaClientIntegrationService`
- **Configuration / backend:** YAML/`JpaClientServiceConfiguration`; needs a reachable JDBC database (via `appbase-client-jdbc`). Built on Hibernate ORM over `appbase-client-jdbc`.
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** Requires external JDBC database (via `appbase-client-jdbc`) infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-kafka`

- **Coordinates:** `com.viveworks.appbase:appbase-client-kafka:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-kafka`
- **Support:** `supported-public`
- **Purpose:** Managed Kafka client for Appbase services — creates, pools, configures and health-checks Kafka connections under the Appbase client core.
- **Capabilities:** Producer and consumer clients, blocking and reactive; configuration-driven lifecycle
- **Public entry points:** `KafkaProducerClientService`, `KafkaConsumerClientService` and their `…Configuration`/`…IntegrationService` types
- **Configuration / backend:** YAML/`KafkaProducerClientServiceConfiguration`; needs a reachable Kafka cluster. Built on kafka-clients + reactor-kafka (Micrometer metrics).
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** Requires external Kafka cluster infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-r2dbc`

- **Coordinates:** `com.viveworks.appbase:appbase-client-r2dbc:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-r2dbc`
- **Support:** `supported-public`
- **Purpose:** Managed reactive R2DBC SQL client for Appbase services.
- **Capabilities:** R2DBC connection pooling (r2dbc-pool) with the MariaDB and PostgreSQL drivers; `ReactiveSqlSession`; `ReactiveR2dbcAccessor`
- **Public entry points:** `R2dbcClientService`, `R2dbcClientServiceConfiguration`, `R2dbcClientIntegrationService`, `ReactiveSqlSession`, `ReactiveR2dbcAccessor`
- **Configuration / backend:** YAML R2DBC connection and pool configuration; needs a MariaDB or PostgreSQL server
- **Examples / API docs:** Javadoc is not published yet
- **Limitations:** Not yet exported by `appbase-bom` on this epic base — a pending BOM update adds it, and the inventory check reports the disagreement until then. Reactive only — no blocking face. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-redis`

- **Coordinates:** `com.viveworks.appbase:appbase-client-redis:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-redis`
- **Support:** `supported-public`
- **Purpose:** Managed Redis client for Appbase services — creates, pools, configures and health-checks Redis connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `RedisClientService`, `RedisClientServiceConfiguration`, `RedisClientIntegrationService`
- **Configuration / backend:** YAML/`RedisClientServiceConfiguration`; needs a reachable Redis server. Built on Lettuce.
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-redis.yaml`; Javadoc is not published yet
- **Limitations:** Requires external Redis server infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-vitess`

- **Coordinates:** `com.viveworks.appbase:appbase-client-vitess:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-vitess`
- **Support:** `supported-public`
- **Purpose:** Managed Vitess client for Appbase services — creates, pools, configures and health-checks Vitess connections under the Appbase client core.
- **Capabilities:** gRPC client with blocking/async commands; JDBC access; channel/interceptor customisation
- **Public entry points:** `VitessGrpcClientService`, `VitessGrpcClientServiceConfiguration`, `VitessGrpcClientIntegrationService`
- **Configuration / backend:** YAML/`VitessGrpcClientServiceConfiguration`; needs a reachable Vitess cluster (vtgate). Built on vitess-grpc-client + vitess-jdbc.
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-vitess-grpc.yaml`; stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-vitess-jdbc.yaml`; stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-vitess-jdbi.yaml`; Javadoc is not published yet
- **Limitations:** Requires external Vitess cluster (vtgate) infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-zookeeper`

- **Coordinates:** `com.viveworks.appbase:appbase-client-zookeeper:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-zookeeper`
- **Support:** `supported-public`
- **Purpose:** Managed ZooKeeper client for Appbase services — creates, pools, configures and health-checks ZooKeeper connections under the Appbase client core.
- **Capabilities:** Curator-based client, retry policies, commands, distributed-counter accessor
- **Public entry points:** `ZooKeeperClientService`, `ZooKeeperClientServiceConfiguration`, `ZooKeeperClientIntegrationService`
- **Configuration / backend:** YAML/`ZooKeeperClientServiceConfiguration`; needs a reachable ZooKeeper ensemble. Built on Apache Curator (framework, recipes, async) (Micrometer metrics).
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-zookeeper.yaml`; Javadoc is not published yet
- **Limitations:** Requires external ZooKeeper ensemble infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

## Cloud clients (AWS)

### `com.viveworks.appbase:appbase-client-cloud-aws-common`

- **Coordinates:** `com.viveworks.appbase:appbase-client-cloud-aws-common:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-common`
- **Support:** `supported-public`
- **Purpose:** Shared AWS SDK v2 building blocks used by the Appbase AWS clients.
- **Capabilities:** Factories for credentials providers, profile files, retry and backoff policies, client override and async configuration, execution interceptors, TLS key managers
- **Public entry points:** `AwsSdkCredentialsProviderFactory`, `AwsSdkClientOverrideConfigurationFactory`, `AwsSdkRetryPolicyFactory`, `AwsSdkBackoffStrategyConfiguration` and related factories
- **Configuration / backend:** YAML AWS SDK settings; AWS credentials/profile files
- **Examples / API docs:** example profiles `appbase-stacktest/src/main/resources/aws-config.profile` and `aws-credentials.profile`; Javadoc is not published yet
- **Limitations:** AWS only in Release 1 — multi-provider cloud is P-018 (2.0.0); these modules move to a top-level `appbase-cloud` tree under P01. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-cloud-aws-httpclient`

- **Coordinates:** `com.viveworks.appbase:appbase-client-cloud-aws-httpclient:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-httpclient`
- **Support:** `supported-public`
- **Purpose:** Managed AWS SDK v2 HTTP clients (sync Apache, async Netty) shared by the AWS service clients.
- **Capabilities:** Sync and async SDK HTTP client lifecycle and configuration; event-loop-group and TLS customisation
- **Public entry points:** `AwsSdkHttpClientService`, `AwsSdkAsyncHttpClientService` and their `…Configuration`/`…IntegrationService` types
- **Configuration / backend:** YAML HTTP-client configuration; AWS SDK apache-client/netty-nio-client
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-aws-http-client.yaml`; stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-aws-async-http-client.yaml`; Javadoc is not published yet
- **Limitations:** AWS only in Release 1 — multi-provider cloud is P-018 (2.0.0); these modules move to a top-level `appbase-cloud` tree under P01. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-cloud-aws-s3`

- **Coordinates:** `com.viveworks.appbase:appbase-client-cloud-aws-s3:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3`
- **Support:** `supported-public`
- **Purpose:** Managed Amazon S3 clients plus an object-store accessor for Appbase services.
- **Capabilities:** Sync/async S3 clients; `ObjectStoreAccessor` (blocking and reactive)
- **Public entry points:** `S3ClientsService`, `S3ClientsServiceConfiguration`, `S3Clients`, `ObjectStoreAccessor`
- **Configuration / backend:** YAML S3 configuration; needs an S3 endpoint (AWS, or an S3-compatible local store) and credentials
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-aws-s3.yaml`; Javadoc is not published yet
- **Limitations:** Real AWS usage incurs cost; integration tests need an S3-compatible endpoint. AWS only in Release 1 — multi-provider cloud is P-018 (2.0.0); these modules move to a top-level `appbase-cloud` tree under P01. Release coordinate: `1.0.0`.

## Application services

### `com.viveworks.appbase:appbase-service-core`

- **Coordinates:** `com.viveworks.appbase:appbase-service-core:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-core`
- **Support:** `supported-public`
- **Purpose:** Application-service building blocks for Appbase services: exchanges, filters and handlers, authorization, cluster, health, ids and password hashing.
- **Capabilities:** `AppbaseServiceExchange` filter/handler chain; `AuthorizationService` and context authorizers; `ClusterService`; `HealthCheckService`/`HealthChecker`; `IdService`; `PasswordService` (Bouncy Castle)
- **Public entry points:** `AppbaseServiceExchange`, `AppbaseServiceFilter`, `AppbaseServiceHandler`, `AuthorizationService`, `ClusterService`, `HealthCheckService`, `IdService`, `PasswordService`
- **Configuration / backend:** YAML service configuration; pulls in the Firebase, S3, ZooKeeper and Jdbi clients, whose backends are needed only for the features you enable
- **Examples / API docs:** `docs/configuration/authorization-and-path-matching.md`; Javadoc is not published yet
- **Limitations:** Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web`
- **Support:** `supported-public`
- **Purpose:** HTTP layer for Appbase services: servlet and JAX-RS endpoints and filters for authorization, CORS, exception handling, health, metrics and tracing.
- **Capabilities:** Servlet, JAX-RS and reactive web filters; core/configuration admin endpoints; health endpoints; CORS; metric and trace filters
- **Public entry points:** `AuthorizationFilter`, `CorsFilter`, the `…endpoint.servlet`/`…endpoint.jaxrs` resources and the `Abstract…Filter` base classes
- **Configuration / backend:** YAML web/service configuration; runs inside a bootstrap container
- **Examples / API docs:** `docs/configuration/authorization-and-path-matching.md`; Javadoc is not published yet
- **Limitations:** Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth`
- **Support:** `supported-public`
- **Purpose:** OAuth 2.0 / OIDC bearer-token validation for Appbase web services.
- **Capabilities:** `OAuthFilter`; Nimbus JOSE+JWT validation; token caching (Caffeine) with hashed cache keys (SHA-256 / HMAC-SHA256)
- **Public entry points:** `OAuthFilter`, `OAuthConfiguration`, `TokenValidator`, `TokenCache`, `TokenKeyHasher`
- **Configuration / backend:** YAML OAuth configuration (issuer, audience, JWKS); needs the identity provider's endpoints
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** Release 1 providers are Google, Okta, Microsoft Entra, Apple and GitHub (baseline §4.1); Meta, X and Cognito are Release 2. The reactive face is `appbase-service-web-oauth-reactive`, supported in Release 1 by the owner ruling. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-github`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-github:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-github`
- **Support:** `supported-public`
- **Purpose:** GitHub OAuth provider: validates GitHub tokens and binds GitHub profile, organisation and team membership.
- **Capabilities:** `GitHubTokenValidator` via the GitHub user API; profile and org/team bindings
- **Public entry points:** `GitHubOAuthConfiguration`, `GitHubTokenValidator`, `GitHubUserApi`
- **Configuration / backend:** YAML GitHub OAuth configuration; needs network access to the GitHub API
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** GitHub is not OIDC — tokens are validated by calling the GitHub API rather than locally. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-google`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-google:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-google`
- **Support:** `supported-public`
- **Purpose:** Google provider for the Appbase OAuth/OIDC filter: provider defaults and profile mapping on top of `appbase-service-web-oauth`.
- **Capabilities:** Google OIDC discovery defaults (fixed issuer and JWKS); profile and group bindings
- **Public entry points:** `GoogleOAuthDefaults`, `GoogleProfileBinding`, `GoogleUserProfile`, `GoogleGroupBinding`
- **Configuration / backend:** YAML OAuth configuration (issuer, audience, JWKS) with the provider's defaults; needs network access to the provider's endpoints
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** Not yet exported by `appbase-bom` on this epic base — a pending BOM update adds it, and the inventory check reports the disagreement until then. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-okta`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-okta:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-okta`
- **Support:** `supported-public`
- **Purpose:** Okta provider for the Appbase OAuth/OIDC filter: provider defaults and profile mapping on top of `appbase-service-web-oauth`.
- **Capabilities:** Okta org discovery defaults; profile and group bindings
- **Public entry points:** `OktaOAuthDefaults`, `OktaProfileBinding`, `OktaUserProfile`, `OktaGroupBinding`
- **Configuration / backend:** YAML OAuth configuration (issuer, audience, JWKS) with the provider's defaults; needs network access to the provider's endpoints
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** Not yet exported by `appbase-bom` on this epic base — a pending BOM update adds it, and the inventory check reports the disagreement until then. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-microsoft`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-microsoft:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-microsoft`
- **Support:** `supported-public`
- **Purpose:** Microsoft Entra ID provider for the Appbase OAuth/OIDC filter: provider defaults and profile mapping on top of `appbase-service-web-oauth`.
- **Capabilities:** Entra ID discovery defaults; profile and group bindings
- **Public entry points:** `EntraOAuthDefaults`, `EntraProfileBinding`, `EntraUserProfile`, `EntraGroupBinding`
- **Configuration / backend:** YAML OAuth configuration (issuer, audience, JWKS) with the provider's defaults; needs network access to the provider's endpoints
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** Not yet exported by `appbase-bom` on this epic base — a pending BOM update adds it, and the inventory check reports the disagreement until then. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-apple`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-apple:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-apple`
- **Support:** `supported-public`
- **Purpose:** Sign in with Apple provider for the Appbase OAuth/OIDC filter: provider defaults and profile mapping on top of `appbase-service-web-oauth`.
- **Capabilities:** Apple discovery defaults; profile binding; client-secret (JWT) generation
- **Public entry points:** `AppleOAuthDefaults`, `AppleProfileBinding`, `AppleUserProfile`, `AppleClientSecretGenerator`
- **Configuration / backend:** YAML OAuth configuration (issuer, audience, JWKS) with the provider's defaults; needs network access to the provider's endpoints
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** Not yet exported by `appbase-bom` on this epic base — a pending BOM update adds it, and the inventory check reports the disagreement until then. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-reactive`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-reactive:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-reactive`
- **Support:** `supported-public`
- **Purpose:** Reactive (non-blocking) face of the OAuth filter.
- **Capabilities:** Reactive web filter chain
- **Public entry points:** `ReactiveOAuthFilter`, `ReactiveWebFilterChain`
- **Configuration / backend:** as `appbase-service-web-oauth`
- **Examples / API docs:** `docs/OAuth-Primer.md`
- **Limitations:** Supported in Release 1 by the owner ruling; `appbase-bom` exports it once the pending BOM update lands. Reactive (Reactor Netty) face only; the servlet face is `appbase-service-web-oauth`. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-tokencache-redis`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-tokencache-redis:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-tokencache-redis`
- **Support:** `supported-public`
- **Purpose:** Redis-backed distributed `TokenCache` for the OAuth filter, replacing the in-memory Caffeine default across service instances.
- **Capabilities:** Shared validated-token cache with hashed keys
- **Public entry points:** `RedisTokenCache`, `RedisTokenCacheGuiceModule`
- **Configuration / backend:** YAML OAuth cache configuration plus `appbase-client-redis` (a reachable Redis)
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** Not yet exported by `appbase-bom` on this epic base — a pending BOM update adds it, and the inventory check reports the disagreement until then. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-web-oauth-tokencache-aerospike`

- **Coordinates:** `com.viveworks.appbase:appbase-service-web-oauth-tokencache-aerospike:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-web-oauth-tokencache-aerospike`
- **Support:** `supported-public`
- **Purpose:** Aerospike-backed distributed `TokenCache` for the OAuth filter, replacing the in-memory Caffeine default across service instances.
- **Capabilities:** Shared validated-token cache with hashed keys
- **Public entry points:** `AerospikeTokenCache`, `AerospikeTokenCacheGuiceModule`
- **Configuration / backend:** YAML OAuth cache configuration plus `appbase-client-aerospike` (a reachable Aerospike)
- **Examples / API docs:** `docs/OAuth-Primer.md`; Javadoc is not published yet
- **Limitations:** Not yet exported by `appbase-bom` on this epic base — a pending BOM update adds it, and the inventory check reports the disagreement until then. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-user`

- **Coordinates:** `com.viveworks.appbase:appbase-service-user:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-user`
- **Support:** `supported-public`
- **Purpose:** User-account and user-session services: accounts, sessions, identity-provider token checks and their web filters.
- **Capabilities:** `UserAccountService`; account and session stores (blocking and reactive contracts); IdP token verification; session and IdP-token filters for servlet, JAX-RS and reactive
- **Public entry points:** `UserAccountService`, `UserAccountStore`, `UserSessionStore`, `ReactiveUserAccountStore`, `ReactiveUserSessionStore`, `IdpTokenVerifier`, `UserSessionFilter`
- **Configuration / backend:** YAML service configuration; the stores come from a backend module (Jdbi, Firebase Auth or Aerospike sessions)
- **Examples / API docs:** Javadoc is not published yet
- **Limitations:** Store implementations live in the backend modules; this module alone has no persistence. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-user-account-firebaseauth`

- **Coordinates:** `com.viveworks.appbase:appbase-service-user-account-firebaseauth:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-user-account-firebaseauth`
- **Support:** `supported-public`
- **Purpose:** Firebase Authentication backend for user accounts.
- **Capabilities:** Create users and check user tokens against Firebase Auth
- **Public entry points:** `FirebaseAuthService`, `DefaultFirebaseAuthServiceConfiguration`
- **Configuration / backend:** YAML configuration plus `appbase-client-firebase` (a Firebase project and credentials)
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-firebase-auth.yaml`; Javadoc is not published yet
- **Limitations:** Requires a Firebase project or the emulators. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-user-account-jdbi`

- **Coordinates:** `com.viveworks.appbase:appbase-service-user-account-jdbi:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-user-account-jdbi`
- **Support:** `supported-public`
- **Purpose:** Jdbi/SQL implementation of the user-account store.
- **Capabilities:** `JdbiUserAccountStore` with a health checker
- **Public entry points:** `JdbiUserAccountStore`, `JdbiUserAccountStoreConfiguration`
- **Configuration / backend:** YAML store configuration plus `appbase-client-jdbi` (a JDBC database)
- **Examples / API docs:** Javadoc is not published yet; no dedicated example
- **Limitations:** Requires a relational database and the account schema. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-user-session-aerospike`

- **Coordinates:** `com.viveworks.appbase:appbase-service-user-session-aerospike:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-user-session-aerospike`
- **Support:** `supported-public`
- **Purpose:** Aerospike implementation of the user-session store.
- **Capabilities:** Blocking and reactive session stores with a health checker
- **Public entry points:** `DefaultUserSessionStore`, `DefaultReactiveUserSessionStore`, `DefaultUserSessionStoreConfiguration`
- **Configuration / backend:** YAML store configuration plus `appbase-client-aerospike` (an Aerospike cluster)
- **Examples / API docs:** stack-test configuration `appbase-stacktest/src/main/resources/appbase-stacktest-aerospike.yaml`; Javadoc is not published yet
- **Limitations:** Requires Aerospike. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-supportmessage`

- **Coordinates:** `com.viveworks.appbase:appbase-service-supportmessage:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-supportmessage`
- **Support:** `supported-public`
- **Purpose:** Support-message (contact/feedback) service with servlet and JAX-RS endpoints.
- **Capabilities:** `SupportMessageService`, pluggable `SupportMessageStore` and listeners, HTTP endpoints
- **Public entry points:** `SupportMessageService`, `SupportMessageStore`, `SupportMessageListener`
- **Configuration / backend:** YAML service configuration (`DefaultSupportMessageServiceConfiguration`); JDBI store `JdbiSupportMessageStore` (`JdbiSupportMessageStoreConfiguration`) over `appbase-client-jdbi`
- **Examples / API docs:** Javadoc is not published yet
- **Limitations:** Release coordinate: `1.0.0`.

## Tools

### `com.viveworks.appbase:appbase-config-wizard`

- **Coordinates:** `com.viveworks.appbase:appbase-config-wizard:1.0.0`
- **Module:** `appbase-config-wizard`
- **Support:** `supported-public`
- **Purpose:** Interactive console and web tool that builds valid YAML configuration files for Appbase services.
- **Capabilities:** Console mode (picocli/JLine) and web mode (Jersey on Jetty, OpenAPI/Swagger UI, Flutter web frontend); discovers configuration classes on the classpath and writes validated YAML
- **Public entry points:** `bin/start-console.sh`, `bin/start-web.sh [port]` / `bin/stop-web.sh` — a runnable tool, not a library API
- **Configuration / backend:** Local only; no backend
- **Examples / API docs:** `appbase-config-wizard/README.md`; the User Guide config-wizard chapter
- **Limitations:** Its Java classes are implementation detail, not an API. The web frontend is built only by `--wizard-frontend` or the staging/release modes. Release coordinate: `1.0.0`.

### `pub:appbase_config_wizard`

- **Coordinates:** `pub:appbase_config_wizard:1.0.0+1`
- **Module:** `appbase-config-wizard/frontend`
- **Support:** `generated-only`
- **Purpose:** Flutter web frontend of `appbase-config-wizard`.
- **Capabilities:** Browser UI for the wizard's web mode
- **Public entry points:** None for consumers — it is served by the wizard (`publish_to: none`)
- **Configuration / backend:** Built into the wizard jar; Flutter toolchain needed only to build it
- **Examples / API docs:** `appbase-config-wizard/README.md`; dartdoc
- **Limitations:** Not published to pub.dev and not usable on its own.

## Internal build and test modules — not consumer dependencies

### `com.viveworks.appbase:appbase`

- **Coordinates:** `com.viveworks.appbase:appbase:1.0.0`
- **Module:** `.`
- **Support:** `internal-implementation`
- **Purpose:** Root parent POM and reactor aggregator. It owns the shared build configuration (plugin versions, profiles, quality gates) and builds every module in one invocation.
- **Capabilities:** Maven parent for every Appbase module; `quick`/`develop`/`staging`/`release` profiles; enforcer, Checkstyle and OSS Index rules
- **Public entry points:** None for consumers — it is inherited transitively as the parent of every artifact
- **Configuration / backend:** Maven build only
- **Examples / API docs:** `build.sh` and `appbase-java/build-guide.md`
- **Limitations:** Not a consumer dependency; never declare it directly.

### `com.viveworks.appbase:appbase-java`

- **Coordinates:** `com.viveworks.appbase:appbase-java:1.0.0`
- **Module:** `appbase-java`
- **Support:** `internal-implementation`
- **Purpose:** Build aggregator for the Java modules (core, client, service, bootstrap).
- **Capabilities:** Groups child modules in the reactor
- **Public entry points:** None — per `appbase-bom/pom.xml` an aggregator produces no artifact of its own and is not a consumable dependency
- **Configuration / backend:** Maven build only
- **Examples / API docs:** `build.sh --modules` selects modules under it
- **Limitations:** Not a consumer dependency.

### `com.viveworks.appbase:appbase-bootstrap`

- **Coordinates:** `com.viveworks.appbase:appbase-bootstrap:1.0.0`
- **Module:** `appbase-java/appbase-bootstrap`
- **Support:** `internal-implementation`
- **Purpose:** Build aggregator for the bootstrap modules.
- **Capabilities:** Groups child modules in the reactor
- **Public entry points:** None — per `appbase-bom/pom.xml` an aggregator produces no artifact of its own and is not a consumable dependency
- **Configuration / backend:** Maven build only
- **Examples / API docs:** `build.sh --modules` selects modules under it
- **Limitations:** Not a consumer dependency.

### `com.viveworks.appbase:appbase-client`

- **Coordinates:** `com.viveworks.appbase:appbase-client:1.0.0`
- **Module:** `appbase-java/appbase-client`
- **Support:** `internal-implementation`
- **Purpose:** Build aggregator for the client modules.
- **Capabilities:** Groups child modules in the reactor
- **Public entry points:** None — per `appbase-bom/pom.xml` an aggregator produces no artifact of its own and is not a consumable dependency
- **Configuration / backend:** Maven build only
- **Examples / API docs:** `build.sh --modules` selects modules under it
- **Limitations:** Not a consumer dependency.

### `com.viveworks.appbase:appbase-client-cloud`

- **Coordinates:** `com.viveworks.appbase:appbase-client-cloud:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-cloud`
- **Support:** `internal-implementation`
- **Purpose:** Build aggregator for the cloud client modules (AWS only in Release 1; multi-provider is P-018, 2.0.0).
- **Capabilities:** Groups child modules in the reactor
- **Public entry points:** None — per `appbase-bom/pom.xml` an aggregator produces no artifact of its own and is not a consumable dependency
- **Configuration / backend:** Maven build only
- **Examples / API docs:** `build.sh --modules` selects modules under it
- **Limitations:** Not a consumer dependency.

### `com.viveworks.appbase:appbase-client-cloud-aws`

- **Coordinates:** `com.viveworks.appbase:appbase-client-cloud-aws:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws`
- **Support:** `internal-implementation`
- **Purpose:** Build aggregator for the AWS client modules.
- **Capabilities:** Groups child modules in the reactor
- **Public entry points:** None — per `appbase-bom/pom.xml` an aggregator produces no artifact of its own and is not a consumable dependency
- **Configuration / backend:** Maven build only
- **Examples / API docs:** `build.sh --modules` selects modules under it
- **Limitations:** Not a consumer dependency.

### `com.viveworks.appbase:appbase-service`

- **Coordinates:** `com.viveworks.appbase:appbase-service:1.0.0`
- **Module:** `appbase-java/appbase-service`
- **Support:** `internal-implementation`
- **Purpose:** Build aggregator for the service modules.
- **Capabilities:** Groups child modules in the reactor
- **Public entry points:** None — per `appbase-bom/pom.xml` an aggregator produces no artifact of its own and is not a consumable dependency
- **Configuration / backend:** Maven build only
- **Examples / API docs:** `build.sh --modules` selects modules under it
- **Limitations:** Not a consumer dependency.

### `com.viveworks.appbase:appbase-stacktest`

- **Coordinates:** `com.viveworks.appbase:appbase-stacktest:1.0.0`
- **Module:** `appbase-stacktest`
- **Support:** `internal-implementation`
- **Purpose:** The representative stack test and example application: a service wired over the bootstrap containers and the supported clients, with example YAML for each.
- **Capabilities:** End-to-end stack tests (the pre-release gate, baseline §5.3); the reactive showcase; example configurations under `src/main/resources`
- **Public entry points:** None — it is an application and a test harness, not a library
- **Configuration / backend:** Docker-based test infrastructure (`appbase-stacktest/infra`)
- **Examples / API docs:** `appbase-stacktest/README.md`
- **Limitations:** Not a consumer dependency, although `appbase-bom` still lists it. The headed showcase is Release 2; it is renamed `appbase-examples` in 2.0.0.

## Deferred — built, but NOT part of Release 1

### `com.viveworks.appbase:appbase-client-clamav`

- **Coordinates:** `com.viveworks.appbase:appbase-client-clamav:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-clamav`
- **Support:** `deferred-unsupported`
- **Purpose:** Managed ClamAV client for Appbase services — creates, pools, configures and health-checks ClamAV connections under the Appbase client core.
- **Capabilities:** Virus-scan requests and pings against clamd through mediator events; configuration-driven client lifecycle
- **Public entry points:** `ClamAVClientService`, `ClamAVClient`, `ClamAVClientServiceConfiguration`, `ClamAVRequestEvent`, `ClamAVPingEvent`
- **Configuration / backend:** YAML/`ClamAVClientServiceConfiguration`; needs a reachable clamd (ClamAV daemon). Built on a built-in clamd protocol client.
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** **Deferred — not part of Release 1.** The lean Release 1.0 baseline §4.2 defers ClamAV (Release 2 registry). It is still exported by `appbase-bom` on this epic base; a pending BOM update removes it, and the inventory check reports it until then. Requires external clamd (ClamAV daemon) infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-jasync`

- **Coordinates:** `com.viveworks.appbase:appbase-client-jasync:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-jasync`
- **Support:** `deferred-unsupported`
- **Purpose:** Managed jasync-sql (async MySQL/PostgreSQL) client for Appbase services — creates, pools, configures and health-checks jasync-sql (async MySQL/PostgreSQL) connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `JasyncClientService`, `JasyncClientServiceConfiguration`, `JasyncClientIntegrationService`
- **Configuration / backend:** YAML/`JasyncClientServiceConfiguration`; needs a reachable MySQL/MariaDB or PostgreSQL server. Built on jasync-mysql + jasync-postgresql (Micrometer metrics).
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** **Deferred — not part of Release 1.** The lean Release 1.0 baseline §4.2 defers Jasync support certification (Release 2 registry). It is still exported by `appbase-bom` on this epic base; a pending BOM update removes it, and the inventory check reports it until then. Requires external MySQL/MariaDB or PostgreSQL server infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-smtp`

- **Coordinates:** `com.viveworks.appbase:appbase-client-smtp:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-smtp`
- **Support:** `deferred-unsupported`
- **Purpose:** Managed SMTP client for Appbase services — creates, pools, configures and health-checks SMTP connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `SmtpClientService`, `SmtpClientServiceConfiguration`, `SmtpClientIntegrationService`
- **Configuration / backend:** YAML/`SmtpClientServiceConfiguration`; needs a reachable SMTP server. Built on HubSpot NioSmtpClient.
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** **Deferred — not part of Release 1.** The lean Release 1.0 baseline §4.2 defers SMTP (Release 2 registry). It is still exported by `appbase-bom` on this epic base; a pending BOM update removes it, and the inventory check reports it until then. Requires external SMTP server infrastructure; integration tests need it running. Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-client-spark`

- **Coordinates:** `com.viveworks.appbase:appbase-client-spark:1.0.0`
- **Module:** `appbase-java/appbase-client/appbase-client-spark`
- **Support:** `deferred-unsupported`
- **Purpose:** Managed Apache Spark client for Appbase services — creates, pools, configures and health-checks Apache Spark connections under the Appbase client core.
- **Capabilities:** Configuration-driven client creation; shared client pooling via `appbase-client-core`; blocking and (where the library supports it) reactive access; integration-service lifecycle
- **Public entry points:** `SparkClientService`, `SparkClientServiceConfiguration`, `SparkClientIntegrationService`
- **Configuration / backend:** YAML/`SparkClientServiceConfiguration`; needs a reachable Spark master/cluster. Built on Spark 3 (Scala 2.12) core, SQL, streaming and Kafka connectors.
- **Examples / API docs:** Javadoc is not published yet; no stack-test example configuration
- **Limitations:** **Deferred — not part of Release 1.** The lean Release 1.0 baseline §4.2 defers Spark support certification (Release 2 registry). It is still exported by `appbase-bom` on this epic base; a pending BOM update removes it, and the inventory check reports it until then. Requires external Spark master/cluster infrastructure; integration tests need it running. Heavy transitive dependency set (Spark + Scala 2.12). Release coordinate: `1.0.0`.

### `com.viveworks.appbase:appbase-service-user-account-r2dbc`

- **Coordinates:** `com.viveworks.appbase:appbase-service-user-account-r2dbc:1.0.0`
- **Module:** `appbase-java/appbase-service/appbase-service-user-account-r2dbc`
- **Support:** `deferred-unsupported`
- **Purpose:** Reactive (R2DBC) user-account store.
- **Capabilities:** Reactive account store
- **Public entry points:** `R2dbcReactiveUserAccountStore`, `R2dbcReactiveUserAccountStoreConfiguration`
- **Configuration / backend:** `appbase-client-r2dbc` database
- **Examples / API docs:** Javadoc is not published yet
- **Limitations:** **Deferred — not part of Release 1** (Release 2), by the owner ruling. Not exported by `appbase-bom`. Release coordinate: `1.0.0`.
