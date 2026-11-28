# Part 1 — Install and first service

[Guide index](README.md) · next: [client integrations](02-client-integrations.md)

## 1. Install — local segment

Install every canonical [build prerequisite](../Configuration-Reference.md#build-prerequisites)
before running these commands.

```sh
git clone https://github.com/viveworks/Appbase.git
cd Appbase
java -version        # must report 25 or later
mvn -version         # must report 3.6.3 or later

mvn clean install -DskipTests -P quick-profile
```

The command drives the root Maven reactor: `appbase-java` (core, client, service, bootstrap),
`appbase-config-wizard`, `appbase-stacktest` and `appbase-bom`. `-DskipTests` compiles test sources and
creates required test-jars, but does not run infrastructure-bound or order-sensitive tests. It
installs every artifact into your local Maven repository (`~/.m2`). Do not replace it with
`-Dmaven.test.skip=true`: that prevents required test-jars from being built.

The other modes are listed in `./build.sh --help`:

| Mode | What it adds | Needs |
|---|---|---|
| `compile` | whole-repo test-compile, no tests | canonical build prerequisites |
| `quick` (default) | unit tests + install | canonical build prerequisites; the full reactor also reaches Jdbi database tests, Elasticsearch client tests, and bootstrap/config tests, and has observed order-sensitive core failures |
| `develop` | coverage, checkstyle and spotbugs gates, site | canonical build prerequisites, plus the test infrastructure required by the selected modules |
| `staging` / `release` | stacktest, wizard integration tests, frontend | **Docker-backed infrastructure and Flutter** — outside this guide's local segment |

To build one module and what it depends on (the inner loop):

```sh
./build.sh --mode quick --modules appbase-java/appbase-core
```

### Using Appbase from your own project

Import the bill of materials, then add the modules you need. The bill of materials is
`appbase-bom/pom.xml`:

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

> **groupId: finalized (internal work item 2546, resolved 2026-11-27).** `com.viveworks.appbase` is
> the real, permanent coordinate -- the 1.0.0 release published under it on
> [Maven Central](https://central.sonatype.com/artifact/com.viveworks.appbase/appbase-core). A rename
> to a shorter `com.viveworks` namespace was considered and abandoned: it would force a breaking
> change on anyone who already adopted 1.0.0, and a product-scoped groupId
> (`groupId.artifactId` naming the product, not just the company) is itself a common, legitimate
> pattern (for example `com.google.guava`, `com.fasterxml.jackson.core`). No further rename is
> planned.

Appbase 1.0.0 is published on Maven Central, so this resolves directly with no local build step. To
build from source instead (for example, to work on Appbase itself),
`mvn clean install -DskipTests -P quick-profile` installs the artifacts locally first.

> **BOM coverage.** The BOM manages every module named below, including Reactor Netty, both R2DBC
> modules, all five OAuth providers, the reactive OAuth module and both token-cache modules. When you
> import `appbase-bom`, do not add explicit versions for those artifacts. The earlier Reactor Netty
> coverage gap has been closed.

---

## 2. First service — local segment

The shortest path to a **running Appbase HTTP server** is the reactive showcase. It needs no Docker:

```sh
mvn test -pl appbase-stacktest -am \
  -Dappbase-stacktest.skipTests=false \
  -Dtest=ReactiveShowcaseStackTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

It starts a Reactor Netty server on an ephemeral port and serves `GET /reactive-showcase/activity`. The
request is authorized, an account is read through the reactive user-account store, and activity
records stream back as newline-delimited JSON. The test asserts the status, content type, event order
and payload, then stops the server. **Success is `BUILD SUCCESS` with `ReactiveShowcaseStackTest`
passing.** The test source is
`appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/reactive/ReactiveShowcaseStackTest.java`.
What it demonstrates is described in [`appbase-stacktest/README.md`](../../appbase-stacktest/README.md).

⚠️ **Be precise about what this is.** It is a real server, started and exercised inside a test, with
controlled fixtures in place of real backing stores. It is **not** a long-running service you start
and leave up. **Release 1 ships no runnable sample application.** An examples module is planned for 2.0.0.

## The shape of an Appbase service

Appbase is a library. Your application assembles four things, in this order, and
[part 4](04-bootstrap-flavors.md) shows each with code copied from the repository's own tests:

| Step | What you build | Where it comes from | Guide part |
|---|---|---|---|
| 1 | **Core services**: log, mediator, exception, configuration, service registry, tracer | `AppbaseCoreServices.builder().fromYaml(…)` in `appbase-bootstrap-core` | [4](04-bootstrap-flavors.md) |
| 2 | **Client services**: one per external system (Redis, Aerospike, JDBC, …) | `AppbaseClientServices.builder().from(core).with(<factory>)…` | [2](02-client-integrations.md) |
| 3 | **Application services**: authorization, health, user accounts, sessions, support messages, … | `AppbaseServices.builder().from(core).withClientServices(clients).with(<factory>)…` | [3](03-service-modules.md) |
| 4 | **A web server** that exposes the services over HTTP | Jetty, Tomcat, Undertow (servlet) or Reactor Netty (reactive) | [4](04-bootstrap-flavors.md) |

Configuration for steps 1 to 3 is YAML. The [configuration wizard](05-config-wizard.md) generates it.

### Counts, as of this tree

| Layer | Count | Where |
|---|---|---|
| Client modules | 19 directories: `appbase-client-core` plus 18 integrations | `appbase-java/appbase-client/pom.xml` |
| Service modules | 20 | `appbase-java/appbase-service/pom.xml` |
| OAuth providers | 5: Apple, GitHub, Google, Microsoft, Okta | `appbase-java/appbase-service/appbase-service-web-oauth-*` |
| Bootstrap modules | 6: core, reactor-netty, servlet (shared layer), servlet-jetty, servlet-tomcat, servlet-undertow | `appbase-java/appbase-bootstrap/pom.xml` |

### Where a module's authority comes from

The set of modules that Release 1 **supports** is not this guide's decision (see the note on the index
page). A module being listed here means it is built from this tree.

### Related existing documents

- [OAuth primer](../OAuth-Primer.md): the concepts (OAuth 2.0, OIDC, JWT) behind part 3's OAuth modules.
  The primer defers "how to configure a provider" to a usage guide; [part 3](03-service-modules.md) is
  that step.
- [Authorization and path matching](../configuration/authorization-and-path-matching.md): how the
  authorization, CORS, session and filter path rules match request paths.
- Maintainer-only development-environment guides are not part of the public User Guide and are
  deliberately not linked as user steps.
