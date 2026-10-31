# P-012 · Modularize appbase-client and appbase-service into fine-grained Maven sub-modules

**Status:** 🟨 In progress  
**Effort:** L (3–5d)  
**Priority:** 37 (before P-002 skill validation)  
**Implementation plan:** `docs/plans/2026-06-24-p012-module-split.md`

---

## Verification gate

P-012 is not complete until all of the following hold:

**Step 0 — Record baseline before starting:**
```bash
cd appbase-java && ./build.sh quick 2>&1 | grep -E "Tests run:|<<< FAILURE|<<< ERROR"
```

**Known pre-existing failures (J-018 scope — acceptable to carry through P-012):**
- `com.viveworks.appbase.service.supportmessage.DefaultSupportMessageServiceTest` — 1 error (requires running DB)
- `com.viveworks.appbase.service.supportmessage.store.JdbiSupportMessageStoreTest` — 1 error (requires running DB)

**Gate conditions — all must be true after P-012:**

| # | Check | Command |
|---|---|---|
| 1 | Unit tests: no new failures vs baseline | `cd appbase-java && ./build.sh quick` |
| 2 | Integration tests: pass at baseline parity (same env vars as before) | `ELASTICSEARCH_CLIENT_SERVICE_TESTS_ENABLED=true ./build.sh dev` (repeat per client) |
| 3 | Stack tests pass in full | `mvn clean install -pl appbase-stacktest -P dev-profile` |
| 4 | No phantom transitive deps in bootstrap | `mvn dependency:tree -pl appbase-bootstrap -P dev-profile \| grep "com.viveworks"` |
| 5 | No new compile warnings | `mvn clean compile -P dev-profile 2>&1 \| grep -i warning` (count must not increase) |

Any new test failure introduced by P-012 (i.e., not in the J-018 baseline list) is a blocking regression — must be fixed before merge.

---

## Description

Break the monolithic `appbase-client` and `appbase-service` modules into fine-grained Maven sub-modules — one per external technology or concern. Consumers declare only the integrations they actually need, eliminating transitive dep pollution (e.g., a service that uses only JDBI no longer pulls in Spark, Aerospike, Cassandra, etc.).

`appbase-client` and `appbase-service` become `<packaging>pom</packaging>` parent+aggregator modules; their current source moves into `*-core` sub-modules.

The `add-appbase-service` skill (P-002) must be updated to generate a new sub-module (not a new package in an existing module) before its validation run.

---

## Dependency-split principle

Any package requiring an external dependency not already in `appbase-core` gets its own sub-module. Packages sharing the same dependency family (e.g., `firebase-admin` covers both `firebase.app` and `firebase.auth`) stay together. Pure-abstraction and web-infrastructure packages with no new external deps stay in the `*-core` sub-module.

---

## New module map — appbase-client

| New module | Source packages | Key new deps | Notes |
|---|---|---|---|
| `appbase-client` *(pom)* | — no source — | `<dependencyManagement>` for all client dep versions | Parent + aggregator |
| `appbase-client-core` | `client.core`, `client.util` | None (appbase-core only) | Base interfaces, pool, clientkey, util |
| `appbase-client-aerospike` | `client.service.aerospike` | `aerospike-client` | |
| `appbase-client-aws` | `client.service.aws.*` (common + httpclient + s3) | `aws-sdk:s3`, `netty-nio-client`, `apache-client` | All three sub-packages share the AWS BOM; S3 is the only service so far — keep together |
| `appbase-client-cassandra` | `client.service.cassandra` | `java-driver-core`, `java-driver-query-builder`, `java-driver-mapper-runtime` | |
| `appbase-client-clamav` | `client.service.clamav` | None (plain Java sockets) | Own module for clean separation — ClamAV is a distinct external system |
| `appbase-client-druid` | `client.service.druid` | `avatica-core` | Apache Druid SQL |
| `appbase-client-elasticsearch` | `client.service.elasticsearch` | `elasticsearch-rest-client` | |
| `appbase-client-firebase` | `client.service.firebase.app` + `firebase.auth` | `firebase-admin` | **Together** — same dep; FirebaseAuth depends on FirebaseApp init |
| `appbase-client-jasync` | `client.service.jasync` | `jasync-mysql`, `jasync-postgresql` | Async SQL |
| `appbase-client-jdbc` | `client.service.jdbc` | `HikariCP`, `mariadb-java-client`, `postgresql` | Raw JDBC + connection pool; foundation for jdbi + jpa |
| `appbase-client-jdbi` | `client.service.jdbi` | `jdbi3-core`, `jdbi3-sqlobject`, `jdbi3-postgres` | Depends on `appbase-client-jdbc` for pool/drivers |
| `appbase-client-jpa` | `client.service.jpa` | `hibernate-core` | Depends on `appbase-client-jdbc` for pool/drivers |
| `appbase-client-kafka` | `client.service.kafka` (producer + consumer) | `kafka-clients` | **Together** — same dep; producer + consumer always co-deployed |
| `appbase-client-smtp` | `client.service.smtp` | `NioSmtpClient` (HubSpot) | |
| `appbase-client-spark` | `client.service.spark` | `spark-core_2.12`, `spark-sql_2.12`, `spark-streaming_2.12`, `spark-sql-kafka`, `spark-streaming-kafka` | Heavy; own module keeps Spark off the classpath for non-Spark services |
| `appbase-client-vitess` | `client.service.vitess` | `vitess-grpc-client`, `vitess-jdbc` | |
| `appbase-client-zookeeper` | `client.service.zookeeper` | `zookeeper`, `curator-*` | |

---

## New module map — appbase-service

| New module | Source packages | Key new deps / notes |
|---|---|---|
| `appbase-service` *(pom)* | — no source — | Parent + aggregator |
| `appbase-service-core` | `service.appbase`, `service.authorization`, `service.cluster`, `service.exceptionhandler`, `service.health`, `service.id`, `service.metric`, `service.trace`, `service.web`, `service.util` | `micrometer-statsd`, `jackson-cbor` (no new external deps vs current `appbase-service`); depends on `appbase-core` |
| `appbase-service-password` | `service.password` | `bcprov-jdk18on` (BouncyCastle / Argon2id); depends on `appbase-service-core` |
| `appbase-service-user` | `service.user.account` + `service.user.session` | `appbase-client-jdbi`, `appbase-client-firebase`; depends on `appbase-service-core` |
| `appbase-service-supportmessage` | `service.supportmessage` | `appbase-client-jdbi`; depends on `appbase-service-core` |

---

## Grouping rationale

**Firebase app + auth → `appbase-client-firebase`:** Both use only `firebase-admin`. FirebaseAuth is functionally dependent on a FirebaseApp instance. Splitting saves nothing and forces users to declare two modules.

**Kafka producer + consumer → `appbase-client-kafka`:** Producers and consumers share `kafka-clients`. In practice, services almost always deploy both.

**AWS common + httpclient + s3 → `appbase-client-aws`:** `aws.common` and `aws.httpclient` are internal plumbing with no standalone use — they exist to configure the S3 client. Keeping them together is the only meaningful grouping until a second AWS service (e.g., SQS, DynamoDB) is added.

**user.account + user.session → `appbase-service-user`:** Session validity references account IDs. The JDBI and Firebase deps are identical. Splitting creates a conceptual and practical circular reference.

**service.appbase (management endpoints) → `appbase-service-core`:** `ConfigurationJaxrsResource`, `CoreJaxrsResource`, `LogJaxrsResource`, etc. are framework introspection endpoints. They carry no new external dep and belong with the core web infrastructure.

**`appbase-client-jdbi` depends on `appbase-client-jdbc`:** JDBI3 sits on top of a JDBC connection pool. Rather than duplicating HikariCP + driver declarations in both modules, `appbase-client-jdbi` declares `appbase-client-jdbc` as a Maven dependency. Same applies to `appbase-client-jpa`.

**`appbase-client-clamav` — no external dep but own module:** ClamAV integration speaks the ClamAV TCP protocol via plain Java sockets. No third-party library dep, but it is a distinct external system integration that should be independently includable and versioned.

---

## Maven directory structure

```
appbase-java/
  pom.xml                          ← root (appbase-parent); <modules> unchanged at this level
  appbase-core/
  appbase-client/
    pom.xml                        ← packaging=pom; parent=appbase-parent; lists all appbase-client-* in <modules>
    appbase-client-core/
      pom.xml                      ← parent=appbase-client; packaging=jar
      src/main/java/.../client/core/
      src/main/java/.../client/util/
    appbase-client-aerospike/
      pom.xml
      src/main/java/.../client/service/aerospike/
    appbase-client-aws/
      pom.xml
      src/main/java/.../client/service/aws/
    appbase-client-cassandra/
    appbase-client-clamav/
    appbase-client-druid/
    appbase-client-elasticsearch/
    appbase-client-firebase/
    appbase-client-jasync/
    appbase-client-jdbc/
    appbase-client-jdbi/
    appbase-client-jpa/
    appbase-client-kafka/
    appbase-client-smtp/
    appbase-client-spark/
    appbase-client-vitess/
    appbase-client-zookeeper/
  appbase-service/
    pom.xml                        ← packaging=pom; parent=appbase-parent; lists all appbase-service-* in <modules>
    appbase-service-core/
      pom.xml
      src/main/java/.../service/appbase/
      src/main/java/.../service/authorization/
      src/main/java/.../service/cluster/
      src/main/java/.../service/exceptionhandler/
      src/main/java/.../service/health/
      src/main/java/.../service/id/
      src/main/java/.../service/metric/
      src/main/java/.../service/trace/
      src/main/java/.../service/web/
      src/main/java/.../service/util/
    appbase-service-password/
    appbase-service-user/
    appbase-service-supportmessage/
  appbase-bootstrap/
  appbase-stacktest/
```

---

## Implementation phases

### Phase 1 — appbase-client split

1. Convert `appbase-client/pom.xml` to `<packaging>pom</packaging>`; move all `<dependencies>` into `<dependencyManagement>`; add `<modules>` list
2. Create `appbase-client/appbase-client-core/` — pom.xml + move `client.core.*` + `client.util.*` source + tests
3. For each of the 17 client sub-modules: create directory, write `pom.xml` (parent=appbase-client; declare only own deps), move source + tests
4. Update `appbase-bootstrap/pom.xml`: replace single `appbase-client` dep with `appbase-client-core` + each specific sub-module dep (a factory exists per client in bootstrap)
5. Update `appbase-stacktest/pom.xml`: declare all client sub-modules (stacktest exercises every integration)
6. `mvn clean install -DskipTests` — fix compile errors; then `mvn clean install` — all tests green

### Phase 2 — appbase-service split

1. Convert `appbase-service/pom.xml` to `<packaging>pom</packaging>`
2. Create `appbase-service/appbase-service-core/` — pom.xml + move core packages
3. Create `appbase-service-password/`, `appbase-service-user/`, `appbase-service-supportmessage/` — poms + move source
4. Update `appbase-bootstrap/pom.xml` and `appbase-stacktest/pom.xml`
5. Build + full test suite

### Phase 3 — Root pom updates

- Add all new artifact IDs to root `pom.xml` `<dependencyManagement>` (version=`${appbase.version}`)
- Verify `mvn dependency:tree` on `appbase-bootstrap` shows no phantom transitive deps

### Phase 4 — Update P-002 skill and validate

- Update `add-appbase-service` SKILL recipes: generate a full new sub-module instead of a new package in an existing module
- Skill must now produce the artifacts listed in the table below
- Re-run skill validation with Redis as the target integration

---

## Updated skill requirements (P-002 impact)

After P-012, the `add-appbase-service` skill must generate:

| Artifact | Details |
|---|---|
| `appbase-client/appbase-client-<name>/pom.xml` | parent=appbase-client; groupId=com.viveworks.appbase; version=${appbase.version}; declares only the new integration's deps |
| `appbase-client/appbase-client-<name>/README.md` | What it is, Maven coordinates, quick-start snippet |
| Source + test files | Same as today (interface, impl, config, unit test, integration test) — paths now under the new module |
| Update `appbase-client/pom.xml` | Add `<module>appbase-client-<name></module>` |
| Update root `pom.xml` | Add artifact to `<dependencyManagement>` |

Same pattern for `appbase-service-*` sub-modules.

---

## Decisions

1. **`appbase-client-jdbi` depends on `appbase-client-jdbc`?** ✅ **Yes.** `appbase-client-jdbi` and `appbase-client-jpa` both declare `appbase-client-jdbc` as a Maven dependency to share HikariCP and JDBC driver management.

2. **`appbase-bootstrap` dep granularity — explicit vs BOM?** ✅ **Explicit.** Declare all client sub-modules explicitly in `appbase-bootstrap/pom.xml`. BOM deferred to a future cleanup.

3. **HBase dep with no source?** ✅ **Drop.** Remove `hbase-client` from `appbase-client/pom.xml`. Delete `infra/docker/hbase/`. No `appbase-client-hbase` module.

4. **Vault infra with no source?** ✅ **Drop.** Delete `infra/docker/vault/`. No placeholder module.

5. **`appbase-service/infra/vitess/`?** ✅ **Consolidate into `appbase-client-vitess`.** Move `local-vitess-start.sh`, `local-vitess-stop.sh`, `local-vitess-purge.sh` to `appbase-client-vitess/infra/local/`. Document in that module's README that the Vitess schema scripts may serve both `appbase-service-user` and `appbase-service-supportmessage` integration tests.

---

## Infrastructure scripts and test resources

This section covers the migration of all non-Java artifacts alongside the source splits.

### Infra script destinations — appbase-client

Per-technology docker scripts move with their Java source. Orchestrator scripts (`docker-*-all.sh` + `common-all.sh`) move to `appbase-client-core` since they have no single-technology home.

| Current location | Destination |
|---|---|
| `infra/docker/aerospike/` | `appbase-client-aerospike/infra/docker/` |
| `infra/docker/cassandra/` | `appbase-client-cassandra/infra/docker/` |
| `infra/docker/clamav/` | `appbase-client-clamav/infra/docker/` |
| `infra/docker/druid/` | `appbase-client-druid/infra/docker/` |
| `infra/docker/elasticsearch/` | `appbase-client-elasticsearch/infra/docker/` |
| `infra/docker/firebase/` | `appbase-client-firebase/infra/docker/` |
| `infra/docker/hbase/` | **Deleted** (no source, no module) |
| `infra/docker/kafka/` | `appbase-client-kafka/infra/docker/` |
| `infra/docker/mariadb/` | `appbase-client-jdbc/infra/docker/mariadb/` *(JDBC driver)* |
| `infra/docker/minio/` | `appbase-client-aws/infra/docker/minio/` *(S3-compatible)* |
| `infra/docker/postfix/` | `appbase-client-smtp/infra/docker/` |
| `infra/docker/postgresql/` | `appbase-client-jdbc/infra/docker/postgresql/` *(JDBC driver)* |
| `infra/docker/spark/` | `appbase-client-spark/infra/docker/` |
| `infra/docker/vault/` | **Deleted** (no source, no module) |
| `infra/docker/vitess/` | `appbase-client-vitess/infra/docker/` |
| `infra/docker/zookeeper/` | `appbase-client-zookeeper/infra/docker/` |
| `infra/docker/docker-*-all.sh` | `appbase-client-core/infra/docker/` |
| `infra/docker/common-all.sh` | `appbase-client-core/infra/docker/` *(paths rewritten — see below)* |
| `infra/docker/README.adoc` | `appbase-client-core/infra/docker/` |
| `infra/local/vitess/` | `appbase-client-vitess/infra/local/` *(consolidates with service vitess scripts; schema shared by user + supportmessage)* |

### `common-all.sh` path rewrite

`common-all.sh` currently sets `$DIR=$BASEDIR/<tech>`. After the move, the orchestrator sits at `appbase-client-core/infra/docker/` while each tech dir is in a sibling sub-module. New paths:

```bash
# Old (all under same infra/docker/):
AEROSPIKE_DIR=$BASEDIR/aerospike

# New (appbase-client-core/infra/docker/ → up 3 levels to appbase-client/, then into sibling):
CLIENT_ROOT=$(cd "$BASEDIR/../../.." && pwd)   # → appbase-client/
AEROSPIKE_DIR=$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike
CASSANDRA_DIR=$CLIENT_ROOT/appbase-client-cassandra/infra/docker/cassandra
CLAMAV_DIR=$CLIENT_ROOT/appbase-client-clamav/infra/docker/clamav
DRUID_DIR=$CLIENT_ROOT/appbase-client-druid/infra/docker/druid
ELASTICSEARCH_DIR=$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch
FIREBASE_DIR=$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase
KAFKA_DIR=$CLIENT_ROOT/appbase-client-kafka/infra/docker/kafka
MARIADB_DIR=$CLIENT_ROOT/appbase-client-jdbc/infra/docker/mariadb
MINIO_DIR=$CLIENT_ROOT/appbase-client-aws/infra/docker/minio
POSTFIX_DIR=$CLIENT_ROOT/appbase-client-smtp/infra/docker/postfix
POSTGRESQL_DIR=$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql
SPARK_DIR=$CLIENT_ROOT/appbase-client-spark/infra/docker/spark
VITESS_DIR=$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess
ZOOKEEPER_DIR=$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper
```

### Infra script destinations — appbase-service

`appbase-service/infra/` currently references `../../appbase-client/infra/docker/` (a path that won't exist after the split). These scripts move to `appbase-service-core/infra/` and their client path references update to the new sub-module locations.

| Current location | Destination |
|---|---|
| `infra/run-infra.sh` | `appbase-service-core/infra/` |
| `infra/start-infra.sh` | `appbase-service-core/infra/` |
| `infra/stop-infra.sh` | `appbase-service-core/infra/` |
| `infra/vitess/` | `appbase-client-vitess/infra/local/` *(consolidated with client vitess local scripts)* |

**Path update in service infra scripts:**

```bash
# Old (from appbase-service/infra/):
APPBASE_INFRA_BASEDIR=$CWD/../../appbase-client/infra/docker/

# New (from appbase-service/appbase-service-core/infra/):
# Up 4 levels → appbase-java/, then into appbase-client/
CLIENT_ROOT=$(cd "$CWD/../../../.." && pwd)   # → appbase-java/
AEROSPIKE_DIR=$CLIENT_ROOT/appbase-client/appbase-client-aerospike/infra/docker/aerospike
ZOOKEEPER_DIR=$CLIENT_ROOT/appbase-client/appbase-client-zookeeper/infra/docker/zookeeper
FIREBASE_DIR=$CLIENT_ROOT/appbase-client/appbase-client-firebase/infra/docker/firebase
# ...
```

### Test resource destinations — appbase-client

Each YAML in `appbase-client/src/test/resources/` moves to its sub-module's `src/test/resources/`:

| File | Destination module |
|---|---|
| `appbase-client-core.yaml`, `appbase-core.yaml` | `appbase-client-core` |
| `appbase-client-aerospike.yaml` | `appbase-client-aerospike` |
| `appbase-client-aws-*.yaml` (3 files) | `appbase-client-aws` |
| `appbase-client-cassandra.yaml` | `appbase-client-cassandra` |
| `appbase-client-clamav*.yaml` (2 files) | `appbase-client-clamav` |
| `appbase-client-druid-sql*.yaml` (2 files) | `appbase-client-druid` |
| `appbase-client-elasticsearch-low-level.yaml` | `appbase-client-elasticsearch` |
| `appbase-client-jasync.yaml` | `appbase-client-jasync` |
| `appbase-client-jdbc.yaml` | `appbase-client-jdbc` |
| `appbase-client-jdbi.yaml` | `appbase-client-jdbi` |
| `appbase-client-jpa.yaml` | `appbase-client-jpa` |
| `appbase-client-kafka-consumer.yaml`, `appbase-client-kafka-producer.yaml` | `appbase-client-kafka` |
| `appbase-client-smtp.yaml` | `appbase-client-smtp` |
| `appbase-client-spark.yaml` | `appbase-client-spark` |
| `appbase-client-vitess-*.yaml` (4 files) | `appbase-client-vitess` |
| `appbase-client-zookeeper.yaml` | `appbase-client-zookeeper` |
| `META-INF/persistence.xml` | `appbase-client-jpa/src/test/resources/META-INF/` |

### Test resource destinations — appbase-service

| File | Destination module |
|---|---|
| `appbase-service-core.yaml`, `appbase-service-client-core.yaml` | `appbase-service-core` |
| `appbase-service-aerospike.yaml` | `appbase-service-core` *(no aerospike sub-module in service layer; used by core tests)* |
| `appbase-service-firebase-app.yaml`, `appbase-service-firebase-auth.yaml` | `appbase-service-user` |
| `appbase-service-vitess-jdbc.yaml`, `appbase-service-vitess-jdbi.yaml` | Check which test class loads these and move to `appbase-service-user` or `appbase-service-supportmessage` accordingly |
