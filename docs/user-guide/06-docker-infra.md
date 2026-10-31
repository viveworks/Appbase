# Part 6 — Docker infrastructure for local tests

[Guide index](README.md) · previous: [configuration wizard](05-config-wizard.md) · next: [REST documentation](07-swagger.md)

The client modules' integration tests and the stack test run against **disposable Docker containers**.
This part lists the commands that exist, the ports they publish, and the six umbrella scripts, which
now resolve correctly (see "Fixed: path resolution" below). It is a maintainer and test aid. **It is not a
deployment** and it is outside the guide's acceptance environment
([index](README.md#acceptance-environment-and-boundary)).

You need Docker and bash. The Vitess local scripts also refuse to run as root.

## Fixed: path resolution — the umbrella scripts now resolve client-owned paths

Six scripts are meant to start/stop the whole stack:

- `appbase-stacktest/infra/start-infra.sh`, `appbase-stacktest/infra/run-infra.sh`, `appbase-stacktest/infra/stop-infra.sh`
- `appbase-java/appbase-service/infra/start-infra.sh`, `appbase-java/appbase-service/infra/run-infra.sh`, `appbase-java/appbase-service/infra/stop-infra.sh`

**Previously, five of the six** set a base directory `APPBASE_INFRA_BASEDIR` to a shared
`…/appbase-client/infra/docker/` path that never existed — the Docker files live per client module,
under `appbase-java/appbase-client/appbase-client-<name>/infra/docker/`. A later fix updated all six to resolve
each service through its owning client module instead (`appbase-client-jdbc` for `postgresql`,
`appbase-client-zookeeper`, `appbase-client-aerospike`, `appbase-client-druid`,
`appbase-client-elasticsearch`, `appbase-client-firebase`), and fixed the matching problem in
`appbase-stacktest/infra/vitess/local-vitess-start.sh`, which had sourced its Vitess helpers and
schema from two other nonexistent paths — both now resolve into
`appbase-java/appbase-client/appbase-client-vitess/infra/local/{vitess,service-schema}/`.

**MinIO was removed from the three stacktest scripts, not fixed**: the old scripts named a `minio`
directory under the nonexistent shared base, and **no MinIO Docker infra exists anywhere in this
tree** — it was never built, under either the old or new layout. The path fix deliberately did not create
one (that is a separate undertaking, not a side effect of a path-fix), so these scripts simply no
longer try to start/stop a MinIO container. Standing gap, not yet its own tracked issue.

**Verified**: `scripts/test/infra_client_paths_test.sh` (a Bash 3.2 harness that mocks the child
shell and asserts the exact `cwd`/script pair each of the six orchestrators resolves to for every
service) passes. That fix also included one full live smoke cycle (Redis: run → stop → start → stop) through
the equivalent per-client path pattern. A fresh attempt at a live multi-service smoke check here hit
three environment issues unrelated to path resolution — a deprecated `bitnami/*:*-debian-10` image
tag (zookeeper, and likely postgresql, same family), an unrelated package-install failure building
firebase's own custom image, and a container-name collision with another concurrent session's
already-running `elasticsearch.local` — each occurring *after* the script had already `cd`'d into the
correct directory, so none of them are a regression from that fix. Not fixed here; flagging in case they
affect a real local run.

## The commands that exist

Every infrastructure directory has the same script set (read from the tree):

| Script | Does |
|---|---|
| `build-<x>.sh` | build or pull the image |
| `run-<x>.sh` | **first time only**: create and start the container |
| `start-<x>.sh` / `stop-<x>.sh` | start or stop the existing container |
| `exec-<x>.sh` | open a shell or client inside it |
| `remove-<x>.sh` | remove the container |
| `env-<x>.sh` | the image, host name, port and data directory it uses (read by the others) |

For example, Redis (`appbase-java/appbase-client/appbase-client-redis/infra/docker/redis/`, per its README):

```sh
cd appbase-java/appbase-client/appbase-client-redis/infra/docker/redis
./build-redis.sh
./run-redis.sh        # first time
./stop-redis.sh
./start-redis.sh      # later
```

`run-redis.sh` publishes port 6379 and mounts a data directory under your home directory
(`$HOME/Temporary/Data/Redis`, set in `env-redis.sh`). Change the `env-*.sh` file, not the run script, to
change such values.

### Where each directory is

| Integration | Directory | Host ports published by its `run` script |
|---|---|---|
| Aerospike | `appbase-java/appbase-client/appbase-client-aerospike/infra/docker/aerospike` | 3000-3002 |
| Cassandra | `appbase-java/appbase-client/appbase-client-cassandra/infra/docker/cassandra` | 9042, 7199, 7197 (`--publish 7197:7000`: host 7197 to container 7000; the script also sets `CASSANDRA_TRANSPORT_PORT_NUMBER=7197`; read from the script, not run) |
| ClamAV | `appbase-java/appbase-client/appbase-client-clamav/infra/docker/clamav` | 3310 |
| Druid | `appbase-java/appbase-client/appbase-client-druid/infra/docker/druid` | 8081-8083, 8090-8091, 8100-8105, 8888 |
| Elasticsearch | `appbase-java/appbase-client/appbase-client-elasticsearch/infra/docker/elasticsearch` | 9200, 9300 |
| Firebase emulator | `appbase-java/appbase-client/appbase-client-firebase/infra/docker/firebase` | 4000 (UI), 9099 (auth) |
| MariaDB | `appbase-java/appbase-client/appbase-client-jdbc/infra/docker/mariadb` | 3306 |
| PostgreSQL | `appbase-java/appbase-client/appbase-client-jdbc/infra/docker/postgresql` | 5432 |
| Kafka | `appbase-java/appbase-client/appbase-client-kafka/infra/docker/kafka` | 9092 |
| Redis | `appbase-java/appbase-client/appbase-client-redis/infra/docker/redis` | 6379 |
| Postfix (SMTP) | `appbase-java/appbase-client/appbase-client-smtp/infra/docker/postfix` | none published by `run-postfix.sh`; a `docker-compose.yml` is present |
| Spark | `appbase-java/appbase-client/appbase-client-spark/infra/docker/spark` | 7077, 8077, 7078, 8078 |
| Vitess | `appbase-java/appbase-client/appbase-client-vitess/infra/docker/vitess` | 15000 (web), 15999 (gRPC), others |
| ZooKeeper | `appbase-java/appbase-client/appbase-client-zookeeper/infra/docker/zookeeper` | 2181, 2888, 3888 |

Ports are read from each `run-*.sh`. The Vitess web and gRPC ports are `VITESS_WEB_PORT` and
`VITESS_GRPC_PORT` in `env-vitess.sh`. The Vitess directory's own README warns that the container
"seems unstable" and that installing Vitess from source worked for its author, so treat Vitess as the
least reliable item here.

Most directories carry a `README.adoc`; read it for image versions and exec commands.

### Starting several at once

`appbase-java/appbase-client/appbase-client-core/infra/docker/` has helpers that act on the whole set. Its
`README.adoc` gives the first-time and later commands:

```sh
cd appbase-java/appbase-client/appbase-client-core/infra/docker
./docker-build-all.sh
./docker-run-all.sh        # first time
./docker-stop-all.sh
./docker-start-all.sh      # later
./docker-remove-all.sh     # remove permanently
```

These resolve each directory from the `appbase-client` root (`common-all.sh`), so they never had the
path bug described above. Two cautions from reading the scripts. They deliberately **skip Kafka and Spark**
(`docker-start-all.sh` prints "NOT starting …"). And `common-all.sh` sets `MINIO_DIR` to
`appbase-java/appbase-client/appbase-client-aws/infra/docker/minio`, which does not exist, so the MinIO step will not find its
script. How the rest of the run behaves after that failure is **not verified here**.

## A sample Druid datasource

`appbase-stacktest/infra/druid/create-druid-datasource.sh` loads the sample file
`appbase-stacktest/infra/druid/stacktest-healthcheck.csv` into a container named `druid.local` with the
specification `appbase-stacktest/infra/druid/druid-spec.json`, then queries the datasource
`appbase_stacktest_healthcheck_data` over `/druid/v2/sql` on port 8888. It needs the Druid container
from the table above to be running.

## Container twin configuration files

Many test YAML files come in pairs: `appbase-client-redis.yaml` and `appbase-client-redis-container.yaml`.
The first points at `localhost`; the twin points at a container host name. For example the stack test's
`appbase-service-vitess-jdbc-container.yaml` differs from `appbase-service-vitess-jdbc.yaml` in exactly
one line, `host: "appbase-infra"` instead of `host: "localhost"`.

A system property selects the twin. This is shown by the repository's twin-resolution test:

<!-- snippet-source: appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/ServiceVitessJdbcConfigTwinResolutionTest.java:56-59,72-73 -->
```java
System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
assertEquals(
        AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + TWIN,
        AppbaseTestInfraProfile.resolve(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + BASE));
// ...
final String base = AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + BASE;
assertEquals(base, AppbaseTestInfraProfile.resolve(base));
```

Source: `appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/ServiceVitessJdbcConfigTwinResolutionTest.java`, lines 56-59,72-73.

`AppbaseTestInfraProfile.PROFILE_PROPERTY` is `appbase.infra.profile` and `AppbaseTestInfraProfile.CONTAINER`
is `container` (`appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/util/AppbaseTestInfraProfile.java`).
So `-Dappbase.infra.profile=container` makes a resource such as `classpath://…/appbase-service-vitess-jdbc.yaml`
resolve to its `-container` twin, and an unset property leaves it unchanged. The twin is a **test
mechanism**. Your own service decides which YAML it loads.

## What is and is not covered

| Covered | Not covered |
|---|---|
| the per-client script set and the ports their `run` scripts publish | that every image still builds or pulls on your machine today (two unrelated environment issues are flagged above) |
| the umbrella-script path fix and its own verification (harness test + live smoke) | a MinIO Docker infra (never built, standing gap) |
| the twin-file mechanism | any real or credentialed infrastructure |
