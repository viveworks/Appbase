# P-012 Implementation Plan — Modularize appbase-client + appbase-service

> **For agentic workers:** Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Break the monolithic `appbase-client` and `appbase-service` Maven modules into fine-grained sub-modules. Each external technology gets its own module. Consumers declare only the integrations they need.

**Spec:** `docs/specs/P-012-module-split-spec.md` — read it before starting. All grouping decisions and open question answers are recorded there.

**Verification gate:** All unit tests must pass at pre-P-012 baseline. Pre-existing failures in `DefaultSupportMessageServiceTest` and `JdbiSupportMessageStoreTest` (J-018 scope) are acceptable. No new failures permitted. Stack tests must pass in full.

**Tech stack:** Java 25, Maven 3.6+, `<packaging>pom</packaging>` for parent modules

## Maven hierarchy (final state)

```
appbase-parent  (appbase-java/pom.xml)          ← packaging=pom; ROOT — NO CHANGES TO <modules>
├── appbase-core                                  already listed; unchanged
├── appbase-client  (packaging=pom, parent=appbase-parent)   ← converted in Task 1
│   ├── appbase-client-core
│   ├── appbase-client-aerospike
│   ├── appbase-client-aws
│   ├── appbase-client-cassandra
│   ├── appbase-client-clamav
│   ├── appbase-client-druid
│   ├── appbase-client-elasticsearch
│   ├── appbase-client-firebase
│   ├── appbase-client-jasync
│   ├── appbase-client-jdbc
│   ├── appbase-client-jdbi
│   ├── appbase-client-jpa
│   ├── appbase-client-kafka
│   ├── appbase-client-smtp
│   ├── appbase-client-spark
│   ├── appbase-client-vitess
│   └── appbase-client-zookeeper
├── appbase-service  (packaging=pom, parent=appbase-parent)  ← converted in Task 6
│   ├── appbase-service-core
│   ├── appbase-service-password
│   ├── appbase-service-user
│   └── appbase-service-supportmessage
├── appbase-bootstrap                             already listed; unchanged
└── appbase-stacktest                             already listed; unchanged
```

**Root pom `<modules>` is already correct** (`appbase-core`, `appbase-client`, `appbase-service`, `appbase-bootstrap`, `appbase-stacktest`). Do NOT add `appbase-client-*` or `appbase-service-*` to the root `<modules>` — Maven discovers them transitively through their parent's `<modules>`. Only the root `<dependencyManagement>` section needs the new artifact IDs added (Task 9).

## Global Constraints

- Do NOT change any Java source code — only move files and write pom.xml files
- Do NOT modify root `pom.xml` `<modules>` — it is already correct
- All new `pom.xml` files: `groupId=com.viveworks.appbase`, `version=${project.parent.version}`, `parent=` the immediate Maven parent
- Every client sub-module depends on `appbase-client-core` (base interfaces)
- `appbase-client-jdbi` and `appbase-client-jpa` both depend on `appbase-client-jdbc`
- Drop `hbase-client` dep and `infra/docker/hbase/` scripts entirely — no stub module
- Drop `infra/docker/vault/` scripts entirely
- Consolidate `appbase-service/infra/vitess/` into `appbase-client-vitess/infra/local/`
- Branch: create a new GitHub issue first, use its number in the branch name
- Do NOT merge — open PR, human reviews and merges

---

## Task 0 — Preparation

- [ ] **Step 1: Record baseline test results**

```bash
cd appbase-java && ./build.sh quick 2>&1 | grep -E "Tests run:|<<< FAILURE|<<< ERROR" > /tmp/p012-baseline.txt
cat /tmp/p012-baseline.txt
```

Save the output. Any failure not in this baseline after P-012 is a regression.

- [ ] **Step 2: Create GitHub issue**

Title: `P-012: Modularize appbase-client and appbase-service into fine-grained Maven sub-modules`

Body: link to `docs/specs/P-012-module-split-spec.md`

- [ ] **Step 3: Create branch**

```bash
git checkout develop && git pull
git checkout -b <issue-number>/Java/Client+Service/Refactor/Modularize-maven-modules
```

---

## Task 1 — Convert appbase-client to pom parent

**Goal:** Make `appbase-client` a `<packaging>pom</packaging>` aggregator. All external dependencies move into `<dependencyManagement>`. A new `appbase-client-core` sub-module holds the existing source.

- [ ] **Step 1: Create appbase-client-core directory structure**

```bash
mkdir -p appbase-java/appbase-client/appbase-client-core/src/main/java
mkdir -p appbase-java/appbase-client/appbase-client-core/src/test/java
mkdir -p appbase-java/appbase-client/appbase-client-core/src/test/resources
```

- [ ] **Step 2: Move core source and tests into appbase-client-core**

```bash
# Source
mv appbase-java/appbase-client/src/main/java/com/viveworks/appbase/client/core \
   appbase-java/appbase-client/appbase-client-core/src/main/java/
mv appbase-java/appbase-client/src/main/java/com/viveworks/appbase/client/util \
   appbase-java/appbase-client/appbase-client-core/src/main/java/
# Test resources
mv appbase-java/appbase-client/src/test/resources/appbase-client-core.yaml \
   appbase-java/appbase-client/appbase-client-core/src/test/resources/
mv appbase-java/appbase-client/src/test/resources/appbase-core.yaml \
   appbase-java/appbase-client/appbase-client-core/src/test/resources/
# Move corresponding test classes (grep for imports of client.core or client.util)
# Then move the test java files alongside
```

- [ ] **Step 3: Move orchestrator infra scripts into appbase-client-core**

```bash
mkdir -p appbase-java/appbase-client/appbase-client-core/infra/docker
mv appbase-java/appbase-client/infra/docker/docker-run-all.sh \
   appbase-java/appbase-client/appbase-client-core/infra/docker/
mv appbase-java/appbase-client/infra/docker/docker-start-all.sh \
   appbase-java/appbase-client/appbase-client-core/infra/docker/
mv appbase-java/appbase-client/infra/docker/docker-stop-all.sh \
   appbase-java/appbase-client/appbase-client-core/infra/docker/
mv appbase-java/appbase-client/infra/docker/docker-build-all.sh \
   appbase-java/appbase-client/appbase-client-core/infra/docker/
mv appbase-java/appbase-client/infra/docker/docker-remove-all.sh \
   appbase-java/appbase-client/appbase-client-core/infra/docker/
mv appbase-java/appbase-client/infra/docker/common-all.sh \
   appbase-java/appbase-client/appbase-client-core/infra/docker/
mv appbase-java/appbase-client/infra/docker/README.adoc \
   appbase-java/appbase-client/appbase-client-core/infra/docker/
```

- [ ] **Step 4: Rewrite common-all.sh paths**

Edit `appbase-java/appbase-client/appbase-client-core/infra/docker/common-all.sh`:

```bash
#!/bin/bash
BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
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

- [ ] **Step 5: Write appbase-client-core/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-client</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>appbase-client-core</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-core</artifactId>
            <version>${project.parent.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 6: Drop hbase dep + infra; drop vault infra**

```bash
# Remove from appbase-client/pom.xml: <artifactId>hbase-client</artifactId> dependency block
# Delete dirs:
rm -rf appbase-java/appbase-client/infra/docker/hbase
rm -rf appbase-java/appbase-client/infra/docker/vault
```

- [ ] **Step 7: Convert appbase-client/pom.xml to pom parent**

Changes to `appbase-java/appbase-client/pom.xml`:
1. Add `<packaging>pom</packaging>`
2. Move all `<dependency>` entries from `<dependencies>` into `<dependencyManagement>`
3. Keep the `<dependency>` for `appbase-core` in plain `<dependencies>` (inherited by all sub-modules) OR move to `<dependencyManagement>` and have each sub-module declare it — use `<dependencyManagement>` for clarity
4. Add `<modules>` block (add modules as they are created in Tasks 2–4)

```xml
<modules>
    <module>appbase-client-core</module>
    <!-- add remaining modules here as each Task below completes -->
</modules>
```

---

## Task 2 — Create database client sub-modules

### 2a: appbase-client-jdbc

- [ ] Create directory: `appbase-client/appbase-client-jdbc/src/main/java/.../client/service/jdbc/`
- [ ] Move source: `appbase-client/src/main/java/.../client/service/jdbc/` → into new module
- [ ] Move test resources: `appbase-client-jdbc.yaml` → `appbase-client-jdbc/src/test/resources/`
- [ ] Move infra: `infra/docker/mariadb/` → `appbase-client-jdbc/infra/docker/mariadb/`
- [ ] Move infra: `infra/docker/postgresql/` → `appbase-client-jdbc/infra/docker/postgresql/`
- [ ] Write `appbase-client-jdbc/pom.xml` with deps: `appbase-client-core`, `HikariCP`, `mariadb-java-client`, `postgresql`
- [ ] Add `<module>appbase-client-jdbc</module>` to `appbase-client/pom.xml`

### 2b: appbase-client-jdbi

- [ ] Create directory and move `client/service/jdbi/` source + tests
- [ ] Move test resources: `appbase-client-jdbi.yaml`
- [ ] Write pom: deps = `appbase-client-jdbc`, `jdbi3-core`, `jdbi3-sqlobject`, `jdbi3-postgres`
- [ ] Add module to `appbase-client/pom.xml`

### 2c: appbase-client-jpa

- [ ] Create directory and move `client/service/jpa/` source + tests
- [ ] Move test resources: `appbase-client-jpa.yaml`, `META-INF/persistence.xml` → `appbase-client-jpa/src/test/resources/META-INF/persistence.xml`
- [ ] Write pom: deps = `appbase-client-jdbc`, `hibernate-core`
- [ ] Add module to `appbase-client/pom.xml`

### 2d: appbase-client-jasync

- [ ] Create directory and move `client/service/jasync/` source + tests
- [ ] Move test resources: `appbase-client-jasync.yaml`
- [ ] Write pom: deps = `appbase-client-core`, `jasync-mysql`, `jasync-postgresql`
- [ ] Add module to `appbase-client/pom.xml`

### 2e: appbase-client-cassandra

- [ ] Create directory and move `client/service/cassandra/` source + tests
- [ ] Move test resources: `appbase-client-cassandra.yaml`
- [ ] Move infra: `infra/docker/cassandra/` → `appbase-client-cassandra/infra/docker/cassandra/`
- [ ] Write pom: deps = `appbase-client-core`, `java-driver-core`, `java-driver-query-builder`, `java-driver-mapper-runtime`
- [ ] Add module to `appbase-client/pom.xml`

---

## Task 3 — Create messaging + search client sub-modules

### 3a: appbase-client-kafka

- [ ] Create directory and move `client/service/kafka/` source + tests (producer + consumer together)
- [ ] Move test resources: `appbase-client-kafka-consumer.yaml`, `appbase-client-kafka-producer.yaml`
- [ ] Move infra: `infra/docker/kafka/` → `appbase-client-kafka/infra/docker/kafka/`
- [ ] Write pom: deps = `appbase-client-core`, `kafka-clients`
- [ ] Add module to `appbase-client/pom.xml`

### 3b: appbase-client-elasticsearch

- [ ] Create directory and move `client/service/elasticsearch/` source + tests
- [ ] Move test resources: `appbase-client-elasticsearch-low-level.yaml`
- [ ] Move infra: `infra/docker/elasticsearch/` → `appbase-client-elasticsearch/infra/docker/elasticsearch/`
- [ ] Write pom: deps = `appbase-client-core`, `elasticsearch-rest-client`
- [ ] Add module to `appbase-client/pom.xml`

### 3c: appbase-client-zookeeper

- [ ] Create directory and move `client/service/zookeeper/` source + tests
- [ ] Move test resources: `appbase-client-zookeeper.yaml`
- [ ] Move infra: `infra/docker/zookeeper/` → `appbase-client-zookeeper/infra/docker/zookeeper/`
- [ ] Write pom: deps = `appbase-client-core`, `zookeeper`, `curator-*`
- [ ] Add module to `appbase-client/pom.xml`

---

## Task 4 — Create cloud + infra client sub-modules

### 4a: appbase-client-firebase

- [ ] Create directory and move `client/service/firebase/` source + tests (app + auth together)
- [ ] Move infra: `infra/docker/firebase/` → `appbase-client-firebase/infra/docker/firebase/`
- [ ] Write pom: deps = `appbase-client-core`, `firebase-admin`
- [ ] Add module to `appbase-client/pom.xml`

### 4b: appbase-client-aws

- [ ] Create directory and move `client/service/aws/` source + tests (common + httpclient + s3 together)
- [ ] Move test resources: `appbase-client-aws-async-http-client.yaml`, `appbase-client-aws-http-client.yaml`, `appbase-client-aws-s3.yaml`
- [ ] Move infra: `infra/docker/minio/` → `appbase-client-aws/infra/docker/minio/`
- [ ] Write pom: deps = `appbase-client-core`, `aws-sdk:s3`, `netty-nio-client`, `apache-client`
- [ ] Add module to `appbase-client/pom.xml`

### 4c: appbase-client-aerospike

- [ ] Create directory and move `client/service/aerospike/` source + tests
- [ ] Move test resources: `appbase-client-aerospike.yaml`
- [ ] Move infra: `infra/docker/aerospike/` → `appbase-client-aerospike/infra/docker/aerospike/`
- [ ] Write pom: deps = `appbase-client-core`, `aerospike-client`
- [ ] Add module to `appbase-client/pom.xml`

### 4d: appbase-client-druid

- [ ] Create directory and move `client/service/druid/` source + tests
- [ ] Move test resources: `appbase-client-druid-sql.yaml`, `appbase-client-druid-sql-shared-resources.yaml`
- [ ] Move infra: `infra/docker/druid/` → `appbase-client-druid/infra/docker/druid/`
- [ ] Write pom: deps = `appbase-client-core`, `avatica-core`
- [ ] Add module to `appbase-client/pom.xml`

### 4e: appbase-client-vitess

- [ ] Create directory and move `client/service/vitess/` source + tests
- [ ] Move test resources: `appbase-client-vitess-grpc.yaml`, `appbase-client-vitess-jasync.yaml`, `appbase-client-vitess-jdbc.yaml`, `appbase-client-vitess-mysql-jdbc.yaml`
- [ ] Move infra docker: `infra/docker/vitess/` → `appbase-client-vitess/infra/docker/vitess/`
- [ ] Move local vitess: `infra/local/vitess/` → `appbase-client-vitess/infra/local/`
- [ ] Also move `appbase-service/infra/vitess/` → `appbase-client-vitess/infra/local/service-schema/` with README note that schema is shared by service-user + service-supportmessage tests
- [ ] Write pom: deps = `appbase-client-core`, `vitess-grpc-client`, `vitess-jdbc`
- [ ] Add module to `appbase-client/pom.xml`

### 4f: appbase-client-spark

- [ ] Create directory and move `client/service/spark/` source + tests
- [ ] Move test resources: `appbase-client-spark.yaml`
- [ ] Move infra: `infra/docker/spark/` → `appbase-client-spark/infra/docker/spark/`
- [ ] Write pom: deps = `appbase-client-core`, `spark-core_2.12`, `spark-sql_2.12`, `spark-streaming_2.12`, `spark-sql-kafka-0-10_2.12`, `spark-streaming-kafka-0-10_2.12`
- [ ] Add module to `appbase-client/pom.xml`

### 4g: appbase-client-smtp

- [ ] Create directory and move `client/service/smtp/` source + tests
- [ ] Move test resources: `appbase-client-smtp.yaml`
- [ ] Move infra: `infra/docker/postfix/` → `appbase-client-smtp/infra/docker/postfix/`
- [ ] Write pom: deps = `appbase-client-core`, `NioSmtpClient`
- [ ] Add module to `appbase-client/pom.xml`

### 4h: appbase-client-clamav

- [ ] Create directory and move `client/service/clamav/` source + tests
- [ ] Move test resources: `appbase-client-clamav.yaml`, `appbase-client-clamav-shared-resources.yaml`
- [ ] Move infra: `infra/docker/clamav/` → `appbase-client-clamav/infra/docker/clamav/`
- [ ] Write pom: deps = `appbase-client-core` (no external dep — plain Java sockets)
- [ ] Add module to `appbase-client/pom.xml`

---

## Task 5 — Verify client split builds clean

- [ ] **Step 1: Confirm no source files remain in old locations**

```bash
find appbase-java/appbase-client/src/main/java -name "*.java" | head -20
# Should be empty — all source moved to sub-modules
```

- [ ] **Step 2: Confirm all test resources moved**

```bash
ls appbase-java/appbase-client/src/test/resources/
# Should be empty (or contain only resources shared by all sub-modules)
```

- [ ] **Step 3: Build client split**

```bash
cd appbase-java && mvn clean install -pl appbase-client -amd -P dev-profile,jacoco-no-rules-profile -DskipTests
# Fix any compile errors before proceeding
```

- [ ] **Step 4: Run tests**

```bash
cd appbase-java && mvn clean install -pl appbase-client -amd -P dev-profile,jacoco-no-rules-profile
# Confirm test results match or improve on baseline
```

---

## Task 6 — Convert appbase-service to pom parent + create appbase-service-core

- [ ] **Step 1: Create appbase-service-core directory structure**

```bash
mkdir -p appbase-java/appbase-service/appbase-service-core/src/main/java
mkdir -p appbase-java/appbase-service/appbase-service-core/src/test/java
mkdir -p appbase-java/appbase-service/appbase-service-core/src/test/resources
mkdir -p appbase-java/appbase-service/appbase-service-core/src/main/resources
mkdir -p appbase-java/appbase-service/appbase-service-core/infra
```

- [ ] **Step 2: Move core service packages into appbase-service-core**

Move the following packages from `appbase-service/src/main/java/.../service/` into `appbase-service-core/src/main/java/.../service/`:
- `appbase/` (management endpoints)
- `authorization/`
- `cluster/`
- `exceptionhandler/`
- `health/`
- `id/`
- `metric/`
- `trace/`
- `web/`
- `util/`

- [ ] **Step 3: Move core service resources**

```bash
# Move META-INF/services and any other resources
mv appbase-java/appbase-service/src/main/resources/META-INF \
   appbase-java/appbase-service/appbase-service-core/src/main/resources/
```

- [ ] **Step 4: Move core test resources**

```bash
mv appbase-java/appbase-service/src/test/resources/appbase-service-core.yaml \
   appbase-java/appbase-service/appbase-service-core/src/test/resources/
mv appbase-java/appbase-service/src/test/resources/appbase-service-client-core.yaml \
   appbase-java/appbase-service/appbase-service-core/src/test/resources/
mv appbase-java/appbase-service/src/test/resources/appbase-service-aerospike.yaml \
   appbase-java/appbase-service/appbase-service-core/src/test/resources/
```

- [ ] **Step 5: Move service infra scripts**

```bash
mv appbase-java/appbase-service/infra/run-infra.sh \
   appbase-java/appbase-service/appbase-service-core/infra/
mv appbase-java/appbase-service/infra/start-infra.sh \
   appbase-java/appbase-service/appbase-service-core/infra/
mv appbase-java/appbase-service/infra/stop-infra.sh \
   appbase-java/appbase-service/appbase-service-core/infra/
# infra/vitess/ already moved to appbase-client-vitess in Task 4e
```

- [ ] **Step 6: Update path references in service infra scripts**

Edit `appbase-service-core/infra/run-infra.sh` (and start/stop variants).

Old pattern: `APPBASE_INFRA_BASEDIR=$CWD/../../appbase-client/infra/docker/`

New pattern (from `appbase-service/appbase-service-core/infra/`):
```bash
CWD=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
APPBASE_CLIENT_ROOT=$(cd "$CWD/../../../.." && pwd)  # → appbase-java/
AEROSPIKE_DIR=$APPBASE_CLIENT_ROOT/appbase-client/appbase-client-aerospike/infra/docker/aerospike
ZOOKEEPER_DIR=$APPBASE_CLIENT_ROOT/appbase-client/appbase-client-zookeeper/infra/docker/zookeeper
FIREBASE_DIR=$APPBASE_CLIENT_ROOT/appbase-client/appbase-client-firebase/infra/docker/firebase
```

- [ ] **Step 7: Write appbase-service-core/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>appbase-service-core</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-core</artifactId>
            <version>${project.parent.version}</version>
        </dependency>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-client-core</artifactId>
            <version>${project.parent.version}</version>
        </dependency>
        <!-- micrometer-statsd, jackson-cbor — copy from current appbase-service pom -->
    </dependencies>
</project>
```

- [ ] **Step 8: Convert appbase-service/pom.xml to pom parent**

Changes:
1. Add `<packaging>pom</packaging>`
2. Move all `<dependency>` entries into `<dependencyManagement>` (keep for version management)
3. Remove the `appbase-client` dep (sub-modules will depend on specific client sub-modules)
4. Add `<modules>` block — start with `appbase-service-core`

---

## Task 7 — Create service sub-modules

### 7a: appbase-service-password

- [ ] Create directory and move `service/password/` source + tests
- [ ] Write pom: deps = `appbase-service-core`, `bcprov-jdk18on`
- [ ] Add `<module>appbase-service-password</module>` to `appbase-service/pom.xml`

### 7b: appbase-service-user

- [ ] Create directory
- [ ] Move source packages: `service/user/account/` + `service/user/session/` (both together)
- [ ] Move test resources: `appbase-service-firebase-app.yaml`, `appbase-service-firebase-auth.yaml`
- [ ] Check which test class loads `appbase-service-vitess-jdbc.yaml` / `appbase-service-vitess-jdbi.yaml` — if user tests, move here too
- [ ] Write pom: deps = `appbase-service-core`, `appbase-client-jdbi`, `appbase-client-firebase`
- [ ] Add module to `appbase-service/pom.xml`

### 7c: appbase-service-supportmessage

- [ ] Create directory and move `service/supportmessage/` source + tests
- [ ] Check which test class loads `appbase-service-vitess-jdbc.yaml` / `appbase-service-vitess-jdbi.yaml` — if supportmessage tests, move here too
- [ ] Write pom: deps = `appbase-service-core`, `appbase-client-jdbi`
- [ ] Add module to `appbase-service/pom.xml`

---

## Task 8 — Update appbase-bootstrap and appbase-stacktest

### 8a: appbase-bootstrap/pom.xml

- [ ] Replace single `<dependency>appbase-client</dependency>` with explicit sub-module deps:

```xml
<!-- Client sub-modules needed by bootstrap factories -->
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-core</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-aerospike</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-aws</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-cassandra</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-clamav</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-druid</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-elasticsearch</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-firebase</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-jasync</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-jdbc</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-jdbi</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-jpa</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-kafka</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-smtp</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-spark</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-vitess</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-zookeeper</artifactId><version>${appbase.version}</version></dependency>
```

- [ ] Replace single `<dependency>appbase-service</dependency>` with:

```xml
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-core</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-password</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-user</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-supportmessage</artifactId><version>${appbase.version}</version></dependency>
```

### 8b: appbase-stacktest/pom.xml

- [ ] Apply the same client + service sub-module dep list as bootstrap (stacktest exercises all integrations)

---

## Task 9 — Root pom dependencyManagement (NOT modules)

> The root `<modules>` block already lists the 5 correct entries and must NOT be changed.
> This task only adds the new artifact IDs to `<dependencyManagement>` so downstream modules
> can reference them without specifying a version.

- [ ] Add all new artifact IDs to the root `appbase-java/pom.xml` `<dependencyManagement>` section:

```xml
<!-- appbase-client sub-modules -->
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-core</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-client-aerospike</artifactId><version>${appbase.version}</version></dependency>
<!-- ... (one entry per sub-module, same list as Task 8) ... -->

<!-- appbase-service sub-modules -->
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-core</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-password</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-user</artifactId><version>${appbase.version}</version></dependency>
<dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-service-supportmessage</artifactId><version>${appbase.version}</version></dependency>
```

---

## Task 10 — Verify service split builds clean

- [ ] **Step 1: Confirm no source files remain in old appbase-service/src locations**

```bash
find appbase-java/appbase-service/src/main/java -name "*.java" | head -20
# Should be empty
```

- [ ] **Step 2: Full build — no tests**

```bash
cd appbase-java && mvn clean install -P dev-profile,jacoco-no-rules-profile -DskipTests
```

Fix any compile errors before continuing.

- [ ] **Step 3: Full build with tests**

```bash
cd appbase-java && ./build.sh quick
```

Compare output against `/tmp/p012-baseline.txt`. No new failures permitted. The two pre-existing failures (`DefaultSupportMessageServiceTest`, `JdbiSupportMessageStoreTest`) are acceptable.

- [ ] **Step 4: Verify bootstrap dependency tree**

```bash
mvn dependency:tree -pl appbase-bootstrap -P dev-profile | grep "com.viveworks"
```

Confirm all sub-modules appear. Confirm `appbase-client` and `appbase-service` do NOT appear as jar dependencies (only their sub-modules should).

- [ ] **Step 5: Stack tests**

```bash
mvn clean install -pl appbase-stacktest -P dev-profile,jacoco-no-rules-profile
```

All stack tests must pass.

---

## Task 11 — Update P-002 skill stub

> Full skill rewrite is the validation run of P-002 (separate ticket). This task only adds a note.

- [ ] Open `appbase-java/dev-tools/skills/add-appbase-service/SKILL.md` and add a prominent note at the top:

```
> ⚠️ POST-P-012 NOTE: This skill must be updated to generate a new Maven sub-module
> (directory + pom.xml + README + source) rather than adding a package to an existing
> module. See docs/specs/P-012-module-split-spec.md § "Updated skill requirements".
> Do not run validation until the skill is rewritten.
```

---

## Task 12 — PR, review, and merge gate

- [ ] Run full verification gate (all 5 checks from the spec)
- [ ] Open PR: `<issue-number>/Java/Client+Service/Refactor/Modularize-maven-modules` → `develop`
- [ ] Run `/code-review` — fix any findings
- [ ] Do NOT merge — wait for human approval
