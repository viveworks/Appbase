<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/branding/lockup-dark.svg">
  <img alt="Appbase" src="docs/branding/lockup-light.svg" width="520">
</picture>

# Appbase

[![Maven Central](https://img.shields.io/maven-central/v/com.viveworks.appbase/appbase-core.svg)](https://central.sonatype.com/artifact/com.viveworks.appbase/appbase-core)

**A modular Java 25 framework for building backend services: core runtime services, backend
adapters, application services, and a choice of embedded server, assembled from one YAML
configuration.**

The former platform overview, `README.adoc` (architecture, modules, the build), was removed.
Current documentation lives under [`docs/`](docs/); the former document is not part of the public export.
This file carries the short operator-facing command tables that people need before they need any of that.

## What is Appbase

Appbase is a set of Maven modules that you combine into a service.

The core modules supply the runtime a service needs: logging, metrics, validation, configuration,
a service registry, tracing, JMX and an internal mediator.

Adapter modules connect that runtime to data stores, messaging and identity providers.

Service modules add ready-made application features: user accounts, sessions, OAuth/OIDC sign-in,
and web endpoints.

A bootstrap module then runs everything on the embedded server you choose.

This README gives an overview of Release 1.0.0. Every supported module and artifact is described in
the shipped [catalogue](#documentation), including the release's [known limitations](docs/Known-Limitations.md).

## Capability families

| Family | What it gives you | Where to look |
|---|---|---|
| **Core services** | Log, metric, validation, mediator, configuration (including MicroProfile Config), service registry, exception, tracer and JMX services, plus YAML/JSON, Lucene and reactive utilities | `appbase-core`, `appbase-bootstrap-core` |
| **Backend adapters** | Clients for data, messaging, search and identity backends; see [Release 1 support](#release-1-support) for publication evidence | `appbase-client-*` |
| **Application services** | User accounts and sessions, web endpoints, and OAuth/OIDC sign-in with a token cache | `appbase-service-*` |
| **Server bootstrap** | Run on embedded Jetty, Tomcat or Undertow (servlet), or on Reactor Netty (reactive) | `appbase-bootstrap-*` |
| **Configuration wizard** | Generates a valid YAML configuration file, from the console or a web UI | `appbase-config-wizard` |
| **BOM** | One import that pins every Release 1 artifact to the same version. Deferred modules are left out | `appbase-bom` |
| **Stacktest** | A representative end-to-end test of a running Appbase stack, and the reference bootstrap | `appbase-stacktest` |

## Release 1 support

<!-- release-evidence:begin -->
<!-- release-evidence:end -->

## Quick start

Install the canonical [build prerequisites](docs/Configuration-Reference.md#build-prerequisites):
**JDK 25 or later, Maven 3.6.3 or later, git, Python 3 available as `python3`, ripgrep available as
`rg`, and bash**. Docker is needed only for backend integration tests.

Appbase 1.0.0 is published on Maven Central. Add the released coordinate directly:

```xml
<dependency>
  <groupId>com.viveworks.appbase</groupId>
  <artifactId>appbase-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

To build from source instead (for example, to work on Appbase itself):

```sh
git clone https://github.com/viveworks/Appbase.git && cd Appbase
./build.sh --mode compile            # whole-repo compile, no tests; refuses on a JDK older than 25
mvn test -pl appbase-stacktest -am \
  -Dappbase-stacktest.skipTests=false \
  -Dtest=ReactiveShowcaseStackTest \
  -Dsurefire.failIfNoSpecifiedTests=false   # starts a real Reactor Netty service; no Docker needed
```

> **NOTE:** The servlet bootstraps use Jersey 3.1.11, whose embedded ASM supports
> `ResourceConfig.packages(...)` scanning on JDK 25. On a later Java runtime, Jetty, Tomcat, and
> Undertow fail fast before startup when package scanning is configured; register resource classes
> explicitly with `ResourceConfig.register(Class)`, run on a supported Java runtime, or upgrade
> Appbase/Jersey. The failure identifies the running Java N: “Jersey package scanning is supported
> through Java 25, but this server is running on Java N.” If Appbase cannot inspect a future Jersey
> layout safely, it fails closed with the same remedies instead of starting with unverified scanning.

If `rg` is missing, use the [ripgrep installation guide](https://github.com/BurntSushi/ripgrep#installation),
or install it with `brew install ripgrep` on macOS, `sudo apt install ripgrep` on Debian/Ubuntu, or
`winget install BurntSushi.ripgrep.MSVC` on Windows. Confirm the command name with `rg --version`.

Next, generate a configuration file with `appbase-config-wizard/bin/start-console.sh` and read
[Configuration reference](docs/Configuration-Reference.md). Whether each of these steps has actually
been run is recorded in [Examples](docs/Examples.md). All five local examples have been EXECUTED: four
on JDK 25, plus the minimum-JDK refusal example against an unsupported older toolchain. Examples that
need Docker, a backend or external infrastructure are NOT MEASURED.

## Swagger UI

Swagger UI is **opt-in and meant for development**. A Jetty service serves it only if it calls
`withSwaggerUI(...)` on its container builder, as the stacktest's development server does. It is a
separate static-file handler that sits outside the servlet context, so the Appbase service filters,
authentication included, do not cover it. Do not turn it on for a service that is reachable from
outside.

## Documentation

| Document | What it covers |
|---|---|
| [Module and Service Catalog](docs/Module-and-Service-Catalog.md) | Every shipped module and artifact: purpose, entry points, configuration, support state and limitations. This is the completeness authority |
| [User Guide](docs/user-guide/README.md) | Installation, a first service, the configuration wizard and deployment, tested on a clean machine |
| [Developer MCP](docs/developer-mcp.md) | Give Claude Code, Codex CLI, Cursor, or GitHub Copilot source-derived Appbase API and convention lookup |
| [Examples](docs/Examples.md) | Supported examples, each marked EXECUTED or NOT MEASURED |
| [Configuration reference](docs/Configuration-Reference.md) | How a service finds and reads its YAML, the override keys, server choice and test switches |
| [Known limitations](docs/Known-Limitations.md) | Interim Release 1 limitations; publication adds the generated release-specific view |
| API documentation | Supported public Java and Dart APIs. Not published yet |
| [Release notes](https://github.com/viveworks/Appbase/releases/tag/1.0.0) | What 1.0.0 contains and what is deferred to Release 2 |

### API inventory gate

Run `bash scripts/api-inventory-fast-check.sh` to check generated API-inventory drift and this README consumer region. The same fast, fail-closed check runs from `build.sh`, the pre-push hook, and CI. The full inventory harness remains in CI and the leaf's own test harness.

The pre-push caller is a fast blocking convenience over the **current checkout only**; it does not
claim to bind evidence to each pushed ref. CI is the exact-commit authority: its checkout uses
`VERIFY_REF`, and the summary records the tested SHA.

<!-- api-inventory:supported-public:begin -->
| Supported public artifact | Family |
|---|---|
| `com.viveworks.appbase:appbase-bom` | build |
| `com.viveworks.appbase:appbase-bootstrap-core` | bootstrap |
| `com.viveworks.appbase:appbase-bootstrap-reactor-netty` | bootstrap |
| `com.viveworks.appbase:appbase-bootstrap-servlet` | bootstrap |
| `com.viveworks.appbase:appbase-bootstrap-servlet-jetty` | bootstrap |
| `com.viveworks.appbase:appbase-bootstrap-servlet-tomcat` | bootstrap |
| `com.viveworks.appbase:appbase-bootstrap-servlet-undertow` | bootstrap |
| `com.viveworks.appbase:appbase-client-aerospike` | client |
| `com.viveworks.appbase:appbase-client-cassandra` | client |
| `com.viveworks.appbase:appbase-client-cloud-aws-common` | client |
| `com.viveworks.appbase:appbase-client-cloud-aws-httpclient` | client |
| `com.viveworks.appbase:appbase-client-cloud-aws-s3` | client |
| `com.viveworks.appbase:appbase-client-core` | client |
| `com.viveworks.appbase:appbase-client-druid` | client |
| `com.viveworks.appbase:appbase-client-elasticsearch` | client |
| `com.viveworks.appbase:appbase-client-firebase` | client |
| `com.viveworks.appbase:appbase-client-jdbc` | client |
| `com.viveworks.appbase:appbase-client-jdbi` | client |
| `com.viveworks.appbase:appbase-client-jpa` | client |
| `com.viveworks.appbase:appbase-client-kafka` | client |
| `com.viveworks.appbase:appbase-client-r2dbc` | client |
| `com.viveworks.appbase:appbase-client-redis` | client |
| `com.viveworks.appbase:appbase-client-vitess` | client |
| `com.viveworks.appbase:appbase-client-zookeeper` | client |
| `com.viveworks.appbase:appbase-config-wizard` | tool |
| `com.viveworks.appbase:appbase-core` | core |
| `com.viveworks.appbase:appbase-service-core` | service |
| `com.viveworks.appbase:appbase-service-supportmessage` | service |
| `com.viveworks.appbase:appbase-service-user` | service |
| `com.viveworks.appbase:appbase-service-user-account-firebaseauth` | service |
| `com.viveworks.appbase:appbase-service-user-account-jdbi` | service |
| `com.viveworks.appbase:appbase-service-user-session-aerospike` | service |
| `com.viveworks.appbase:appbase-service-web` | service |
| `com.viveworks.appbase:appbase-service-web-oauth` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-apple` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-github` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-google` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-microsoft` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-okta` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-reactive` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-tokencache-aerospike` | service |
| `com.viveworks.appbase:appbase-service-web-oauth-tokencache-redis` | service |
<!-- api-inventory:supported-public:end -->

## What comes next

Release 2 work, including deferred adapter breadth, is tracked in the Release 2 Plan of
Record. The [1.0.0 release notes](https://github.com/viveworks/Appbase/releases/tag/1.0.0) record
the durable next-release decisions; this README does not copy that content.

## License

Apache License 2.0. See [`LICENSE`](LICENSE).

---

## Developing Appbase (contributors)

The rest of this README is for people working **on** Appbase, not with it.

- **Contributing** — Start with the public [contributor guide](CONTRIBUTING.md) for setup,
  checks, sign-off, licence expectations, review and merge, troubleshooting, and private
  security reports.

## Release 1.0.0 Stats

Measured against `develop`'s actual release-candidate tree and git history, not estimated.

- **Time to release:** 7 years, 9 months, 14 days (2,844 days) — first commit `2019-02-12` to
  `2026-11-26`.
- **Commits:** 2,806 on `develop`, from a single author across 3 git identities used over the
  project's life (`Navaneeth Mysore G`, `navaneeth-mysore-dev`, `navaneeth-mysore`).
- **Maven modules:** 55.
- **Lines of code:** 399,186 total (code lines only, via `cloc`, 3,698 files).
- **Tests:** 2,709 total.
  - Unit tests: 2,585
  - Integration tests: 93
  - Stacktests (`appbase-stacktest`, full-stack scenario coverage): 31
- **Full release-profile build** (`clean install site site:stage`, every module, every test
  tier): ~5h01m, last proven clean end to end on the exact commit this release ships.
