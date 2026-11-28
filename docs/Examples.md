# Supported examples — Release 1.0.0

Each example below is a supported Release 1 path. Each one carries a **Status** line saying whether
it has actually been run:

- **EXECUTED** — it was run exactly as written. The line gives the date, the revision and what was
  observed.
- **NOT MEASURED** — it has not been run. The line names what the run needs. Each of these examples
  stays unproven until someone runs it with that prerequisite, and an owner-authorized gate runs any
  example that needs external or credentialed infrastructure. A local run never counts as evidence
  for an external one.

Run every command from the repository root. Prerequisites are listed in the
[Configuration reference](Configuration-Reference.md#build-prerequisites). Deferred capabilities
have no examples here (see [Known limitations](Known-Limitations.md)).

## Local, non-destructive examples

### 1. Check the toolchain before building

```sh
./build.sh --help               # the modes and module-selection options
./build.sh --mode compile       # refuses immediately on a JDK older than 25
```

**Status:** EXECUTED 2026-09-26 at `db781e5cf` against a JDK below the required minimum. `--help`
printed its usage text on stderr and exited 1. `--mode compile` refused before Maven started with the
expected JDK 25+ requirement. This proves only the preflight. It does not prove that a build succeeds.

### 2. Compile the whole repository from source

```sh
./build.sh --mode compile       # whole-repo test-compile; runs no tests
```

**Status:** EXECUTED 2026-09-26 at `6b9160a3d` on OpenJDK 25.0.2 and Maven 3.9.11. It took about two
minutes (22:35–22:37Z) and printed `compile gate: every module compiles (main + test sources); no tests
run`. It needs no backend or credentials.

### 3. Run the Reactive Netty showcase

This example starts a small, real HTTP service on an ephemeral port and serves one streamed route.
It needs no Docker. The details are in the [stacktest README](../appbase-stacktest/README.md).

```sh
mvn test -pl appbase-stacktest -am \
  -Dappbase-stacktest.skipTests=false \
  -Dtest=ReactiveShowcaseStackTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

**Status:** EXECUTED 2026-09-26 at `6b9160a3d` on OpenJDK 25.0.2. Result:
`ReactiveShowcaseStackTest` — Tests run: 1, Failures: 0, Errors: 0 — `BUILD SUCCESS`, in 33 s from an
already-compiled tree. No Docker or external backend was used.
- **Re-run 2026-09-28 at `d31bedd3c`** on OpenJDK 25.0.4.1. That revision comes after `2bf09f548` made
  bootstrap-core's deferred dependencies optional. Result: Tests run: 1, Failures: 0, Errors: 0.

### 4. Generate a configuration file with the wizard

```sh
appbase-config-wizard/bin/start-console.sh     # builds the JAR first if it is missing
```

**Status:** EXECUTED 2026-09-27 at `0af3da249` on OpenJDK 25.0.2, from a clean checkout with no
`com.viveworks` artifacts in the Maven repository. The script's own build reached `BUILD SUCCESS` in
1m54s, the console printed its main menu (`[1] Configurations · [E] Exit`), and it printed `Goodbye.` on
end of input.
- Before `0af3da249`, the script built with `-Dmaven.test.skip=true`. That flag never compiles
  test-jars, so the deferred `appbase-client-clamav` could not resolve `appbase-client-core:jar:tests`,
  and the build failed from a clean checkout (the 2026-09-26 run at `6b9160a3d`). The scripts now use
  `-DskipTests`. `scripts/test/wizard_scripts_build_test.sh` fails if either script goes back to the old
  flag.
- **Re-run 2026-09-28 at `d31bedd3c`** on OpenJDK 25.0.4.1, after the optional-dependency change
  (`2bf09f548`). The JAR was removed first so the script rebuilt it: `BUILD SUCCESS`, then the main menu,
  then `Goodbye.` on end of input. The Maven repository was not emptied for this run. The build's OSS Index
  vulnerability lookup could not reach its service from the run environment (HTTP 403). It logged a
  warning and did not fail the build.
- The web mode (`bin/start-web.sh [port]`) uses the same build step. Its UI also needs a Flutter
  toolchain; that mode is NOT MEASURED.

### 5. Import the BOM and load configuration from YAML

In your service's `pom.xml`, after the reactor has been installed locally with `mvn install`:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.viveworks.appbase</groupId>
      <artifactId>appbase-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

In Java, following the documented builder in `AppbaseCoreServices`:

```java
AppbaseCoreServices core = AppbaseCoreServices.builder()
    .fromYaml("classpath:///appbase-config.yaml")   // or a filesystem path
    .build();
```

**Status:** EXECUTED 2026-09-26 at `6b9160a3d` on OpenJDK 25.0.2.
- **Setup.** `mvn install -pl appbase-bom,appbase-java/appbase-bootstrap/appbase-bootstrap-core -am
  -DskipTests`. Then a scratch consumer project, outside the repository, imported the BOM, depended
  on `appbase-bootstrap-core` with no version, and ran the snippet above against a copy of
  `appbase-bootstrap-core/src/test/resources/appbase-test-config.yaml`.
- **Result.** It built the core services, printed a success marker and shut down cleanly.
- **Re-run 2026-09-27 at `2bf09f548`** on OpenJDK 25.0.4.1, after bootstrap-core's deferred
  dependencies became optional. The setup was the same, from a Maven repository with no
  `com.viveworks` artifacts. The snippet built the core services and shut down cleanly. The consumer's
  classpath had 27 Appbase jars. This was a historical test before the three empty service shells
  were removed from the reactor; its classpath count is not a current Release-1 publication claim.
  A later owner ruling also deferred `appbase-service-user-account-r2dbc`.
  See [Known limitations](Known-Limitations.md#build-and-packaging-limits-on-this-revision).

### 6. Consumer smoke test — Reactor Netty + embedded JPA/H2

A minimal external Maven project, outside the reactor, depends on the published `1.0.0`
coordinates and starts a real Reactor Netty service backed by embedded JPA + H2, exposing one
Tasks REST resource. See [`examples/tasks-quickstart-reactor-netty/`](../examples/tasks-quickstart-reactor-netty/).

```sh
cd examples/tasks-quickstart-reactor-netty
mvn -Dmaven.repo.local=<empty dir> clean test
```

**Status:** EXECUTED 2026-11-28 at `e663e73fc` on OpenJDK 25.0.3. Resolved against an empty
local repository with network only to Maven Central: `appbase-bom`, `appbase-core`,
`appbase-service-core` and `appbase-bootstrap-reactor-netty` at `1.0.0`, plus their own
transitive dependencies, all from `repo.maven.apache.org`. The smoke test started the server on
an ephemeral port and drove full CRUD over real HTTP: `POST /tasks` → 201, `GET /tasks` → 200
(one task listed), `PUT /tasks/item?id=1` → 200 (`done:true`), `GET /tasks/item?id=1` → 200,
`DELETE /tasks/item?id=1` → 200, then `GET /tasks/item?id=1` → 404. `BUILD SUCCESS`, Tests run:
1, Failures: 0.

### 7. Consumer smoke test — embedded Jetty + Jersey + embedded JPA/H2

The same Tasks resource/service pattern as example 6, under a different embedded-server
choice: embedded Jetty with a JAX-RS (Jersey) resource. See
[`examples/tasks-quickstart-jetty-jersey/`](../examples/tasks-quickstart-jetty-jersey/).

```sh
cd examples/tasks-quickstart-jetty-jersey
mvn -Dmaven.repo.local=<empty dir> clean test
```

**Status:** EXECUTED 2026-11-28 at `e663e73fc` on OpenJDK 25.0.3. Resolved against an empty
local repository with network only to Maven Central: `appbase-bom`, `appbase-core` and
`appbase-bootstrap-servlet-jetty` at `1.0.0` (30 distinct `com.viveworks.appbase` artifacts in
total, pulled in transitively), from `repo.maven.apache.org`. One resolution attempt logged a
harmless failed metadata fetch against an unrelated legacy host (`jvnet-nexus-releases`,
expired certificate); it did not affect the actual resolution, which completed from Central.
The smoke test started the server on an ephemeral port and drove full CRUD over real HTTP:
`POST /tasks` → 201, `GET /tasks` → 200 (one task listed), `PUT /tasks/1` → 200 (`done:true`),
`GET /tasks/1` → 200, `DELETE /tasks/1` → 200, then `GET /tasks/1` → 404. `BUILD SUCCESS`,
Tests run: 1, Failures: 0.

## Examples that need a backend, Docker or credentials

### 8. Run a supported adapter's integration tests

```sh
KAFKA_CLIENT_SERVICE_TESTS_ENABLED=true ./build.sh develop
```

The integration-test switch for every supported adapter is listed in the
[Configuration reference](Configuration-Reference.md#integration-test-switches).

**Status:** NOT MEASURED — needs Docker and a running backend container.

### 9. Run the full stacktest

```sh
./build.sh --mode staging       # includes appbase-stacktest and the config-wizard IT suite
```

**Status:** NOT MEASURED — needs Docker-backed infrastructure for several backends.

### 10. Deploy to external infrastructure

No deploy command is documented here yet. The deploy path belongs to the User Guide
([`docs/user-guide/README.md`](user-guide/README.md), planned).

**Status:** NOT MEASURED — needs external, credentialed infrastructure. It stays NOT MEASURED until
an owner-authorized gate runs it.
