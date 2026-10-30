# Build Guide

See `CONTRIBUTING.md` for the quick-reference build commands (`build.sh`).
This document provides the full reference for the Maven build system.

---

## Quick Start

**`build.sh` lives at the repo root** (one level above this directory) — run it from there.

```shell
# WHOLE-REPO test-compile, no tests run. REQUIRED before every PR.
./build.sh --mode compile

# Fast feedback (default)
./build.sh

# Full build with coverage enforcement
./build.sh --mode develop

# Everything develop does, plus appbase-stacktest and the appbase-config-wizard IT suite
./build.sh --mode staging

# Release build
./build.sh --mode release

# Narrow to specific modules — PATHS, not artifactIds; deps resolved from the POMs via -am
./build.sh --mode quick --modules appbase-java/appbase-core
./build.sh --mode quick --modules appbase-config-wizard/frontend   # the wizard + its Flutter web UI

# Build the config-wizard's Flutter web UI as part of a full build (opt-in; staging/release imply it)
./build.sh --mode develop --wizard-frontend
```

See `CONTRIBUTING.md` for integration-test env vars and targeted Maven invocations.

---

## Build Lifecycle Goals

| Phase | Goal | Active when |
|---|---|---|
| `clean` | `default-clean` | always |
| `initialize` | `enforce-quick-environment` | `quick-profile` |
| `initialize` | `enforce-develop-environment` | `develop-profile` or `staging-profile` |
| `initialize` | `enforce-release-environment` | `release-profile`; `requireReleaseDeps` uses `onlyWhenRelease=true`, so Maven's dry-run build of unchanged SNAPSHOT working-copy POMs can proceed while a real prepare's rewritten release POMs still reject SNAPSHOT dependencies |
| `initialize` | `git-commit-id-plugin` | always |
| `generate-sources` | `copy-licenses-output-directory` — copies LICENSE, NOTICE to `${project.build.outputDirectory}/META-INF` | always |
| `process-sources` | `copy-licenses-generated-sources` — copies LICENSE, NOTICE, git.properties to `${project.build.directory}/generated-sources/resources` | always |
| `generate-resources` | `enforce-maven-resources-plugin` — verifies the above copies succeeded | always |
| `process-resources` | `build-helper-maven-plugin:add-resources` | always |
| `compile` | `default-compile` | always |
| `process-classes` | `generate-manifest` | always |
| `test` | `maven-surefire-plugin` | always |
| `test` | `jacoco-maven-plugin` | any of the 4 profiles (no coverage rules in `quick-profile`; rules enforced in the other 3) |
| `package` | `maven-bundle-plugin` — OSGi-compliant manifest | always |
| `package` | `maven-source-plugin` | `develop-profile`, `staging-profile`, `release-profile` |
| `package` | `maven-shade-plugin` | `develop-profile`, `staging-profile`, `release-profile` (pluginManagement only; modules opt in) |
| `package` | `maven-javadoc-plugin` | `release-profile` |
| `verify` | `maven-failsafe-plugin` | `develop-profile`, `staging-profile`, `release-profile` (per-module opt-in, e.g. `appbase-bootstrap-tomcat`) |
| `verify` | `spotbugs-maven-plugin:check` | `staging-profile`, `release-profile` only (threshold High, fails the build on a High finding; report-only in `quick-profile`/`develop-profile`) — N04 (internal work item 1206) |
| `verify` | `dependency-check-maven:check` (OWASP) | `security-scan` profile only (opt-in; fails at CVSS ≥ 7.0) — N05 (internal work item 1207) |
| `install` | `default-install` | always |
| `initialize` | `enforce-release-environment` (`requireReleaseVersion` included) | `release-profile`; rejects SNAPSHOT projects in ordinary release builds and in `release:perform`. The release plugin's preparation-only `clean verify` goals pass `-Denforcer.skip=true`, because a dry run verifies the unchanged SNAPSHOT working-copy POM. |

---

## Site Goals

| Plugin | Output |
|---|---|
| `maven-project-info-reports-plugin` | Standard Maven site |
| `maven-surefire-report-plugin` | Unit and integration test reports |
| `spotbugs-maven-plugin` | Static analysis report |
| `maven-checkstyle-plugin` | Coding style report |
| `jacoco-maven-plugin:report` | Code coverage report |
| `maven-javadoc-plugin:javadoc-no-fork` | Per-module javadoc |
| `maven-javadoc-plugin:aggregate-no-fork` | Aggregate javadoc (parent only) |
| `versions-maven-plugin` | Dependency/plugin update report (`release-profile`) |
| `maven-doap-plugin` | DOAP file (`release-profile`) |

---

## Maven Profiles

| Profile | Description |
|---|---|
| `quick-profile` | Runs unit tests, no coverage threshold, skips javadoc. Used when developing features and want to quickly check if things are working. |
| `develop-profile` | Runs unit tests, coverage gate, ITs (per-module opt-in), builds source/shade jars. Used to check if the feature works properly. |
| `staging-profile` | Runs everything `develop-profile` does, plus `appbase-stacktest` and the `appbase-config-wizard` console & web IT suite — does all the checks `release-profile` does, but doesn't release. |
| `release-profile` | Runs everything `staging-profile` does, plus javadoc/DOAP/versions-check, and performs the actual release (version bump in poms, tag, ...) via `maven-release-plugin`. Used before release. |

Maven has no profile-inheritance mechanism, so each profile's plugin configuration is
fully self-contained in `pom.xml` — `staging-profile` and `release-profile` duplicate
`develop-profile`'s content rather than extending it. This is intentional: Maven cannot inherit
profile configuration, and the profiles implement progressively stronger build behavior.

Failsafe integration tests remain a **per-module opt-in**: a module declares its own
`<profile>` with an id matching `develop-profile`/`staging-profile`/`release-profile`
(see `appbase-bootstrap-tomcat/pom.xml`) to bind `maven-failsafe-plugin`; Maven merges
same-id profiles across POMs in the reactor. The `appbase-config-wizard` console/web IT suite
works the same way but under its own profile id (`appbase-config-wizard-it-profile`,
self-contained in `appbase-config-wizard/pom.xml`), co-activated alongside `staging-profile`/
`release-profile` via `build.sh`'s `-P` flag rather than merged into their bodies.

Two `appbase-service` modules join that opt-in as of N11 (internal work item 1301):
`appbase-service-supportmessage` and `appbase-service-user-account-jdbi`. Five of their store
tests open a live JDBI `Handle` against the vtgate declared by
`appbase-service-vitess-jdbc.yaml` (`localhost:15306`). They were named `*Test`, so Surefire
selected them on every ordinary offline build and they failed on connection-refused. They are now
named `*IT` — which takes them off Surefire and onto the Failsafe opt-in above — and are
additionally gated by
`@EnabledIfSystemProperty(named = "JdbiClientServiceTests.enabled", matches = "true")`, the same
gate `DefaultJdbiClientServiceTest` uses and the same pattern the Aerospike store tests in
`appbase-service-user-session-aerospike` already follow.

**Both halves are load-bearing.** The rename alone keeps `quick-profile` green, but
`develop-profile`/`staging-profile`/`release-profile` *do* select `*IT` through Failsafe; the gate
is what keeps them skipped there. To run them you need a live vtgate **and** the explicit opt-in:

```
./build.sh --mode develop --modules appbase-java/appbase-service/appbase-service-supportmessage \
  -DJdbiClientServiceTests.enabled=true
```

**Recorded reason for the default skip (spec T7 / epic acceptance):** what is missing is a reachable
vtgate on `localhost:15306`; task N03 (internal work item 1294) provides it. With that up and the gate enabled, these
ITs run.

`SupportMessageStoreItGateConventionTest` and `UserAccountStoreItGateConventionTest` guard the
arrangement: each discovers its own module's compiled `*IT` classes and fails if any of them lacks
the gate, so a newly added ungated integration test cannot silently redden the offline build again.

---

## Tests and Analyzers

| Tool | Role |
|---|---|
| `maven-surefire-plugin` | Unit tests |
| `jacoco-maven-plugin` | Code coverage |
| `spotbugs-maven-plugin` | Static analysis (extended by `sb-contrib` and `findsecbugs-plugin`). **Build-time gate**: `spotbugs:check` bound to `verify` at threshold High in `staging-profile`/`release-profile` **only** (fails the build on a High finding); report-only in `quick-profile`/`develop-profile`. Promoted from report-only by N04 (internal work item 1206). |
| `maven-checkstyle-plugin` | Style and standards (extended by `sevntu-checks`). **Report-only** — the checkstyle build-failing gate was carved out of internal work item 1132 (spec internal work item 1336) and is **owned by internal work item 1204** (P04·E05), which depends on **internal work item 710**'s (P04·E04) `google-java-format --aosp` reformat landing first; internal work item 710 owns the format, internal work item 1204 owns the checkstyle ruleset and gate (ownership reconciled 2026-08-20). |
| `maven-failsafe-plugin` | Integration tests (per-module opt-in profile, see Maven Profiles below) |
| `dependency-check-maven` (OWASP) | CVE gate — fails at CVSS ≥ 7.0 under the opt-in `security-scan` profile (N05 internal work item 1207). |

---

## Plugins Inherited by All Child Modules

- `git-commit-id-plugin`
- `maven-resources-plugin` (`copy-licenses-output-directory`, `copy-licenses-generated-sources`)
- `maven-enforcer-plugin` (`enforce-maven-resources-plugin`)
- `build-helper-maven-plugin`

---

## Child Module Structure

Each child module defines (see `appbase-core/pom.xml` for a sample):

- Basic: `modelVersion`, `parent`, `artifactId`, `packaging`, `name`, `description`, `url`, SCM URL, `distributionManagement`
- `dependencies`
- `maven-bundle-plugin` configuration
- `maven-failsafe-plugin` profile configuration (if the module has integration tests)

---

## Extensions

- `os-maven-plugin` — detects OS and CPU architecture (used by Netty native transport)

---

## Important Properties

| Property | Purpose |
|---|---|
| `build.failOnError` | Whether build failures are fatal |
| `jacoco.coveredRatio` | Minimum JaCoCo instruction coverage ratio |
| `jacoco.missedCount` | Maximum allowed missed class count |

---

## JMX management permissions

JMX `reload` and `shutdown` operations now deny by default at the global, per-bean, and
operation-specific permission tiers. Read-only operations such as status queries remain available.
This is a behavior change for deployments that previously relied on implicit permission for
mutating JMX operations.

Operators must explicitly grant every applicable permission tier. Applications bootstrapped with
`AppbaseServices.Builder` can instead call `withAllJmxOperationsEnabled()` to opt in to all exposed
JMX operations. A system-property value takes precedence over the corresponding environment value;
when the system property is absent, the environment value remains effective.

---

## Reactive accessors (P-027 R0)

`appbase-client-core` gains a new package, `com.viveworks.appbase.client.core.reactive`,
providing a reactive (Project Reactor) operation surface for accessors. This is **opt-in
and additive**: no new module was introduced, and the existing synchronous accessor face
is unchanged. A client accessor may implement both the synchronous and reactive surfaces
side by side, or reactive-only, depending on what the underlying driver supports.

### `AbstractReactiveAccessor`

Subclasses call the native driver to produce a `Mono`/`Flux`; `AbstractReactiveAccessor`
supplies three protected helpers that wrap the driver call with the configured scheduler,
timeout, retry/backoff, backpressure (Flux only), and Appbase error mapping:

| Helper | Signature | Use |
|---|---|---|
| `mono` | `protected <T> Mono<T> mono(Supplier<Mono<T>> source)` | Driver already returns a `Mono` (Tier-2 reactive driver) |
| `flux` | `protected <T> Flux<T> flux(Supplier<Flux<T>> source)` | Driver already returns a `Flux` (Tier-2 reactive driver) |
| `monoFromBlocking` | `protected <T> Mono<T> monoFromBlocking(Callable<T> blockingCall)` | Driver has no reactive API (Tier-1 async facade) |

**Tier-1 vs Tier-2:**

- **Tier-1 (`monoFromBlocking`)** — an async *facade* over a blocking driver call. It
  offloads the call onto the configured scheduler via `Mono.fromCallable`, but the call
  itself still holds one thread for its entire duration. It is **not** non-blocking; use it
  only where the underlying driver exposes no reactive API.
- **Tier-2 (`mono` / `flux`)** — true non-blocking. The driver itself produces the
  `Mono`/`Flux` (a reactive driver), and no thread is held while the operation is in
  flight.

**Order of operations — `mono`:**

1. `Mono.defer(source)` — defers driver invocation until subscription
2. Per-attempt `timeout(configuration.getTimeout())`
3. Retry/backoff via `retryWhen(...)`, only when `retryEnabled`
4. Appbase error mapping — `onErrorMap(ReactiveErrorMapper::map)`, wraps any non-Appbase
   error in `ReactiveOperationException`
5. `subscribeOn(scheduler())` — dispatches onto the resolved scheduler

**Order of operations — `flux`:** identical to `mono`, plus bounded backpressure
buffering, applied **after** retry and **before** error mapping:

1. `Flux.defer(source)` → per-attempt `timeout`
2. Retry/backoff (when enabled)
3. `onBackpressureBuffer(configuration.getBackpressureBufferSize())`
4. Error mapping (`ReactiveErrorMapper::map`)
5. `subscribeOn(scheduler())`

Buffering is deliberately placed after retry: a buffer overflow (slow consumer) is a
backpressure condition, not a transient driver error, so it must not re-invoke a
possibly non-idempotent source.

### `ReactiveOperationConfiguration` knobs

Every knob has a default, so a caller that configures nothing still gets a working
reactive setup:

| Knob | Type | Default | Meaning |
|---|---|---|---|
| `enabled` | `boolean` | `true` | Whether the reactive surface is active |
| `schedulerId` | `String` (nullable) | `null` | ID of the `ReactorService` scheduler to run on; `null` selects the default scheduler |
| `timeout` | `Duration` | `30s` | Per-attempt timeout applied before retry |
| `retryEnabled` | `boolean` | `true` | Whether retry/backoff is applied on error |
| `retryMaxAttempts` | `int` | `3` | Max retry attempts (`Retry.backoff`) |
| `retryMinBackoff` | `Duration` | `100ms` | Initial backoff delay |
| `retryMaxBackoff` | `Duration` | `5s` | Backoff delay ceiling |
| `retryJitterFactor` | `double` | `0.5` | Jitter applied to backoff delay |
| `backpressureBufferSize` | `int` | `256` | Bounded buffer size for `flux`'s `onBackpressureBuffer` |
| `metricsEnabled` | `boolean` | `true` | Whether a per-operation `Timer` (success) and `Counter` (error) are recorded via the shared `AbstractAppbaseAccessor` spine |
| `traceEnabled` | `boolean` | `true` | Whether a per-operation OpenTracing span is created around the call |
| `exceptionStoreEnabled` | `boolean` | `false` | Whether notable failures are reported to the exception store via the mediator |

### Scheduler selection

Schedulers are not configured by the reactive accessor layer itself — they come from the
existing `ReactorService` / `SchedulerFactory` SPI in `appbase-core`
(`com.viveworks.appbase.core.service.reactive.reactor`). `schedulerId` selects a scheduler
by its `SchedulerConfiguration#getId()`; if the id is unknown, `AbstractReactiveAccessor`
logs a warning and falls back to `ReactorService#getDefaultScheduler()` (the scheduler
configuration whose `isDefaultScheduler()` is `true`).

### Configuration examples

Zero-config YAML — omitting the node (or providing an empty one) is valid; every field
falls back to its default:

```yaml
defaultReactiveOperationConfiguration: {}
```

Overriding a single value — any subset of fields may be supplied, the rest keep their
defaults:

```yaml
defaultReactiveOperationConfiguration:
  schedulerId: aerospike-io
  timeout: 10s
  retryMaxAttempts: 5
```

Builder example, same overrides as above, plus every remaining default made explicit for
illustration:

```java
ReactiveOperationConfiguration configuration = DefaultReactiveOperationConfiguration.builder()
        .schedulerId("aerospike-io")
        .timeout(Duration.ofSeconds(10))
        .retryMaxAttempts(5)
        // Left at defaults: enabled=true, retryEnabled=true, retryMinBackoff=100ms,
        // retryMaxBackoff=5s, retryJitterFactor=0.5, backpressureBufferSize=256,
        // metricsEnabled=true, traceEnabled=true, exceptionStoreEnabled=false
        .build();
```

### Reactive Redis (P-027 R2a)

`appbase-client-redis` is the first client to wire up both faces of the accessor layer.
`RedisClientService` exposes:

| Accessor | Getter (default pool) | Getter (keyed pool) |
|---|---|---|
| `ReactiveRedisAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<TextValue>)` |
| `RedisAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<TextValue>)` |

Both accessors are thin wrappers over Lettuce: each `command`/`commandMany` invocation
borrows one pooled connection from the corresponding `AppbaseClientIntegrationService`,
runs the supplied Lettuce command against it, and returns the connection to the pool on
completion, error, or cancellation.

**Reactive usage** — `command` for a single value (`Mono`), `commandMany` for a stream
(`Flux`):

```java
final ReactiveRedisAccessor reactive = redisClientService.getReactiveAccessor();

Mono<String> value = reactive.command("get", cmds -> cmds.get(key));

Flux<String> keys = reactive.commandMany("keys", cmds -> cmds.keys("appbase:*"));
```

An escape hatch is available for callers that already hold a borrowed connection and want
Lettuce's raw reactive command interface directly, bypassing pooling/borrow-return:

```java
RedisReactiveCommands<String, String> raw = reactive.reactive(connection);
```

**Synchronous counterpart** — same shape, `command` returns the value directly instead of
a `Mono`. Unlike the reactive face (which carries failures as an error signal on the `Mono`),
the synchronous `command` is declared `throws Exception`: a failure is thrown to the caller,
mapped to the same `RedisAccessorException` the reactive `onErrorMap` produces, so the caller
must handle (or propagate) it:

```java
final RedisAccessor sync = redisClientService.getAccessor();
try {
    final String value = sync.command("get", cmds -> cmds.get(key));
} catch (final Exception e) {
    // a Lettuce failure surfaces here as a RedisAccessorException —
    // the same mapped type the reactive face emits via onErrorMap
}
```

**Shared spine** — every operation through either accessor, reactive or synchronous, runs
through the same `AbstractAppbaseAccessor`/`AbstractReactiveAccessor` cross-cutting layer:

- **Metrics**: a per-operation `Timer` (success) and `Counter` (error), named via
  `MetricService#name(Class, String, String...)` (e.g.
  `...ReactiveRedisAccessor.get.timer`). These are registered on the shared Micrometer
  `CompositeMeterRegistry` and are therefore **auto-exported to JMX** alongside every
  other Appbase metric — no extra wiring needed.
- **Tracing**: a config-gated OpenTracing span per operation.
- **Logging**: operation failures are logged via the accessor's `Logger`.
- **Exception mapping**: Lettuce driver failures are mapped to `RedisAccessorException`
  (both faces map identically; the reactive face applies the mapping via `onErrorMap`, the
  synchronous face by rethrowing the mapped exception).

The reactive face additionally applies the full R0 operator chain around every call:
per-attempt timeout, retry/backoff, backpressure buffering (`commandMany`/`Flux` only),
and dispatch onto the configured scheduler — see the `AbstractReactiveAccessor` section
and `ReactiveOperationConfiguration` knob table above for the full set of
`enabled`/`schedulerId`/`timeout`/retry*/`backpressureBufferSize`/`metricsEnabled`/
`traceEnabled`/`exceptionStoreEnabled` knobs.

**Configuration** — the reactive knobs are configured per Redis client via the
`reactiveOperationConfiguration` block on `DefaultRedisClientServiceConfiguration`; any
subset of fields may be supplied, the rest keep their defaults:

```yaml
defaultDefaultRedisClientIntegrationServiceConfiguration:
  hostPortConfiguration:
    host: "localhost"
    port: 6379
reactiveOperationConfiguration:
  timeout: 5s
```

**Pooling** — each `command`/`commandMany` call borrows one pooled connection per
subscription and returns it automatically via `Mono`/`Flux#usingWhen` (reactive) or a
`try`/`finally` (synchronous); callers never manage borrow/return themselves.

### Reactive Cassandra (P-027 R2a)

`appbase-client-cassandra` wires both faces of the accessor layer over the DataStax Java
driver (`CqlSession`). `CassandraClientService` exposes:

| Accessor | Getter (default pool) | Getter (keyed pool) |
|---|---|---|
| `ReactiveCassandraAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| `CassandraAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |

Each call borrows one pooled `CqlSession` from the corresponding
`AppbaseClientIntegrationService`, runs the statement against it, and returns it to the
pool on completion, error, or cancellation.

**Reactive usage** — `executeReactive` streams every row (`Flux<ReactiveRow>`),
`executeReactiveFirst` takes just the first (`Mono<ReactiveRow>`), and `query` is a general
escape hatch for any driver `Publisher`:

```java
final ReactiveCassandraAccessor reactive = cassandraClientService.getReactiveAccessor();

Flux<ReactiveRow> rows = reactive.executeReactive(
        "select", SimpleStatement.newInstance("SELECT v FROM ks.kv"));

Mono<String> value = reactive
        .executeReactiveFirst("select", SimpleStatement.newInstance("SELECT v FROM ks.kv WHERE k = ?", key))
        .map(row -> row.getString("v"));

Flux<T> custom = reactive.query("custom", session -> session.executeReactive(statement));
```

A raw escape hatch is available for callers that already hold a borrowed `CqlSession` and
want the driver's `ReactiveResultSet` directly, bypassing pooling/borrow-return:

```java
ReactiveResultSet raw = reactive.reactive(session, statement);
```

**Synchronous counterpart** — `executeStatement` runs a statement and returns its
`ResultSet` directly; `command` applies a function to the borrowed `CqlSession`. Both are
declared `throws Exception`: a driver failure is thrown to the caller, mapped to the same
`CassandraAccessorException` the reactive `onErrorMap` produces (so the caller must handle
or propagate it). `executeStatement` is named distinctly to avoid colliding with the
inherited `AbstractAppbaseAccessor#execute(String, Callable)` spine method it delegates to.

```java
final CassandraAccessor sync = cassandraClientService.getAccessor();
try {
    final ResultSet rs = sync.executeStatement("select", SimpleStatement.newInstance("SELECT v FROM ks.kv"));
} catch (final Exception e) {
    // a DataStax failure surfaces here as a CassandraAccessorException —
    // the same mapped type the reactive face emits via onErrorMap
}
```

**Shared spine** — like the Redis twin, every operation through either accessor runs
through the same `AbstractAppbaseAccessor`/`AbstractReactiveAccessor` cross-cutting layer:
per-operation `Timer`/`Counter` metrics on the shared `CompositeMeterRegistry` (auto-exported
to JMX), a config-gated OpenTracing span, failure logging, and DataStax `DriverException`
mapping to `CassandraAccessorException` (both faces map identically via the shared
`CassandraAccessorException.map(...)`). The reactive face additionally applies the full R0
operator chain (timeout, retry/backoff, backpressure buffering for `Flux`, scheduler
dispatch) — see the `AbstractReactiveAccessor` section and `ReactiveOperationConfiguration`
knob table above.

**DataStax driver-native metrics** — beyond the accessor-level `Timer`/`Counter`, the
driver's own metrics (`cql-requests`, `bytes-sent`, `bytes-received`, …) are routed onto
Appbase's shared Micrometer composite registry (and therefore JMX) when
`driverMetricsEnabled` (default `true`) is set on the Cassandra integration-service
configuration. Appbase configures the driver's `MicrometerMetricsFactory` and binds the
composite registry via `SessionBuilder#withMetricRegistry`; the registry is owned by
`MetricService` and is never shut down by the session. This is wired through a programmatic
driver config loader, so it applies when no external file-based `driverConfigLoaderConfiguration`
is supplied (a file-based loader owns its own metrics config).

**Configuration** — the reactive knobs are configured per Cassandra client via the
`reactiveOperationConfiguration` block on `DefaultCassandraClientServiceConfiguration`
(same knob set as Redis: `enabled`/`schedulerId`/`timeout`/retry*/`backpressureBufferSize`/
`metricsEnabled`/`traceEnabled`/`exceptionStoreEnabled`); any subset may be supplied, the
rest keep their defaults:

```yaml
defaultDefaultCassandraClientIntegrationServiceConfiguration:
  hostPortConfigurations:
    - host: "localhost"
      port: 9042
  localDatacenterConfiguration:
    enabled: true
    localDatacenter: "datacenter1"
  driverMetricsEnabled: true
reactiveOperationConfiguration:
  timeout: 5s
```

**Integration test** — `ReactiveCassandraAccessorIT` exercises a reactive
`CREATE KEYSPACE`/`CREATE TABLE` → `INSERT` → `SELECT` round-trip end-to-end against a live
Cassandra; it is gated by `-DCassandraClientServiceTests.enabled=true` (use the Docker
scripts in `infra/docker/cassandra/`).

### Reactive Kafka (P-027 R2a)

`appbase-client-kafka` builds both faces over **reactor-kafka**, across its two services
(producer and consumer, each generic `<K,V>`). Unlike Redis/Cassandra, the reactive path does
NOT reuse the pooled sync `Producer`/`Consumer` — reactor-kafka's `KafkaSender`/`KafkaReceiver`
own their own client, built from the **same** integration-service config (properties + topic
subscription). The pooled sync client is untouched.

| Service | Accessor | Getter (default pool) | Getter (keyed pool) |
|---|---|---|---|
| producer | `ReactiveKafkaProducerAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| producer | `KafkaProducerAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |
| consumer | `ReactiveKafkaConsumerAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| consumer | `KafkaConsumerAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |

**Producer** — `send` is a discrete, bounded operation, so it rides the FULL spine (timeout,
retry, backpressure, per-op timer/span). Reactive `send(op, Publisher<SenderRecord>)` streams
`SenderResult`s; `send(op, ProducerRecord)` is a single-send convenience returning
`Mono<RecordMetadata>`. Synchronous `send(op, ProducerRecord)` blocks and returns the
`RecordMetadata` (`throws Exception`, mapped to `KafkaAccessorException`).

```java
final ReactiveKafkaProducerAccessor<String, String> reactive = producerService.getReactiveAccessor();
Mono<RecordMetadata> ack = reactive.send("send", new ProducerRecord<>(topic, key, value));

final KafkaProducerAccessor<String, String> sync = producerService.getAccessor();
RecordMetadata metadata = sync.send("send", new ProducerRecord<>(topic, key, value)); // throws Exception
```

> **Delivery semantics** — the full spine's retry re-sends on failure, which can DUPLICATE
> records against a non-idempotent producer. Enable `enable.idempotence=true` (or disable reactive
> retry for the client) for at-least-once-without-duplicates.

**Consumer** — `receive` is a long-lived, infinite subscription, so it rides the LIGHTER
`fluxStream` spine (error mapping + failure reporting + scheduler only — NO timeout/retry/
backpressure-buffer/per-op timer/span, each of which is harmful on an unbounded stream: an
idle-topic timeout would kill a healthy consumer, a retry would resubscribe and trigger a
consumer-group rebalance, a per-op timer/span would never close). The reactive receiver
subscribes to the `subscriptionTopics` configured on the consumer integration config.
Synchronous `poll(op, Duration)` is a bounded op and returns `ConsumerRecords`.

```java
final ReactiveKafkaConsumerAccessor<String, String> reactive = consumerService.getReactiveAccessor();
Flux<ReceiverRecord<String, String>> records = reactive.receive("receive");

final KafkaConsumerAccessor<String, String> sync = consumerService.getAccessor();
ConsumerRecords<String, String> polled = sync.poll("poll", Duration.ofSeconds(1)); // throws Exception
```

Escape hatches `sender()` / `receiver()` expose the raw reactor-kafka `KafkaSender`/`KafkaReceiver`.

**Shared spine** — every operation runs through the same `AbstractAppbaseAccessor`/
`AbstractReactiveAccessor` cross-cutting layer: per-op `Timer`/`Counter` metrics on the shared
`CompositeMeterRegistry` (auto-exported to JMX), a config-gated OpenTracing span (bounded ops
only), failure logging, and `org.apache.kafka.common.KafkaException` mapping to
`KafkaAccessorException` (both faces, via the shared `KafkaAccessorException.map(...)`). The sync
producer `send` unwraps the `Future.get()` `ExecutionException` so the same mapping applies.

**Kafka driver-native metrics** — beyond the accessor-level metrics, Kafka's own client metrics
(`records-consumed-*`, `bytes-sent`/`-received`, request latency, consumer lag, …) are routed onto
the composite (→ JMX) via Micrometer `KafkaClientMetrics` when `driverMetricsEnabled` (default
`true`) is set on the producer/consumer integration config. Bound for BOTH faces: the pooled sync
`Producer`/`Consumer` (at client build) and the reactor-kafka `KafkaSender` (`doOnProducer`) /
`KafkaReceiver` (`doOnConsumer`, tied to the receive stream lifecycle). The registry is
`MetricService`'s shared composite — never shut down; only the binders are closed on shutdown.

**Configuration** — the reactive knobs are the same `reactiveOperationConfiguration` block (on
`DefaultKafkaProducerClientServiceConfiguration` / `DefaultKafkaConsumerClientServiceConfiguration`);
`driverMetricsEnabled` and (consumer) `subscriptionTopics` live on the integration config:

```yaml
defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration:
  bootstrapServerConfigurations:
    - host: "localhost"
      port: 9092
  groupId: "my-group"
  subscriptionTopics:
    - "my-topic"
  driverMetricsEnabled: true
reactiveOperationConfiguration:
  timeout: 5s
```

**Integration test** — `ReactiveKafkaAccessorIT` exercises a reactive `send` → `receive`
round-trip end-to-end against a live Kafka broker (consumer subscribed first, then produced via
`StepVerifier#then`); it is gated by `-DKafkaClientServiceTests.enabled=true` (use the Docker
scripts in `infra/docker/kafka/`).

### Reactive Aerospike (P-027 R2a)

`appbase-client-aerospike` wires both faces of the accessor layer over the Aerospike Java
client's reactor module (`aerospike-reactor-client`). `DefaultAerospikeClientService` exposes:

| Accessor | Getter (default pool) | Getter (keyed pool) |
|---|---|---|
| `ReactiveAerospikeAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| `AerospikeAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |

The existing `AbstractAerospikeAccessor` (shared error-mapping/notability base for both faces) is
untouched by this twin — it contributes only `mapError`/`isNotable` wiring, not accessor methods.

**Reactive usage** — every operation (`get`, `put`, `delete`, `exists`, `operate`, `query`) is a
single bounded call/result set, so it rides the FULL `mono`/`flux` spine inherited from
`AbstractReactiveAccessor` (timeout, retry/backoff, backpressure buffering for `query`, scheduler
dispatch, metrics, tracing, exception-store reporting) — there is no lighter `fluxStream` use here,
unlike Kafka's consumer:

```java
final ReactiveAerospikeAccessor reactive = aerospikeClientService.getReactiveAccessor();

Mono<Key> written = reactive.put("put", key, new Bin("v", "hello"));
Mono<KeyRecord> read = reactive.get("get", key);
Flux<KeyRecord> matches = reactive.query("query", statement);
```

**Synchronous counterpart** — `AerospikeAccessor` mirrors the same operation set against the
driver's blocking API; the no-`Policy`/`WritePolicy` overloads pass `null` for the policy
argument, which is the Aerospike driver's own documented convention for "use the client's
configured default policy":

```java
final AerospikeAccessor sync = aerospikeClientService.getAccessor();
sync.put("put", key, new Bin("v", "hello")); // throws Exception
final Record record = sync.get("get", key);
```

**The non-owning reactor client rule** — unlike Kafka's reactive producer/consumer, which build
their own reactor-kafka clients, `ReactiveAerospikeAccessor` is handed an `IAerospikeReactorClient`
(`AerospikeReactorClient`) that **wraps the same shared, pooled `IAerospikeClient`** the
synchronous accessor and the integration service use. This wrapper is cached per
`AppbaseClientKey` (built once, on first reactive-accessor acquisition for that key) and is
**never closed**: calling `IAerospikeReactorClient#close()` would close the shared synchronous
client underneath every other consumer of that connection — a double-close hazard. Lifecycle
(connect/close) stays owned by the integration service that built the shared client; the cached
wrapper is just dropped (not closed) on `shutdownImpl()`.

**Shared spine** — like the other R2a twins, every operation through either accessor runs through
the same `AbstractAppbaseAccessor`/`AbstractReactiveAccessor` cross-cutting layer: per-operation
`Timer`/`Counter` metrics on the shared `CompositeMeterRegistry` (auto-exported to JMX), a
config-gated OpenTracing span, failure logging, and Aerospike driver exception mapping to
`AerospikeAccessorException` (both faces map identically via the shared
`AerospikeAccessorException.map(...)`).

**Aerospike driver-native metrics** — beyond the accessor-level `Timer`/`Counter`, the driver's own
per-node/per-latency-bucket metrics (`MetricsListener` push snapshots: node error/timeout counts,
read/write/batch/query latency histograms) are routed onto Appbase's shared Micrometer composite
registry (and therefore JMX) when `driverMetricsEnabled` (default `true`) is set on the Aerospike
integration-service configuration. Appbase sets `MetricsPolicy#listener` to
`AppbaseAerospikeMetricsListener`, which **replaces** the driver's default file-writer sink with
this push bridge — the driver never writes metrics files under this configuration. The registry is
owned by `MetricService` and is never shut down by `disableMetrics()`; `disableMetrics()` is called
before the shared client is closed on `shutdownImpl()`, so the listener stops receiving snapshots
cleanly.

**Configuration** — the reactive knobs are the same `reactiveOperationConfiguration` block (R0
knobs: `enabled`/`schedulerId`/`timeout`/retry*/`backpressureBufferSize`/`metricsEnabled`/
`traceEnabled`/`exceptionStoreEnabled`) on `DefaultAerospikeClientServiceConfiguration`;
`driverMetricsEnabled` lives on the integration config:

```yaml
defaultDefaultAerospikeClientIntegrationServiceConfiguration:
  defaultAerospikeClientKeyValueConfiguration:
    clusterId: "primary"
  aerospikeNodeConfigurations:
    - hostnameOrIp: "localhost"
      port: 3000
  driverMetricsEnabled: true
reactiveOperationConfiguration:
  timeout: 5s
```

**Integration test** — `ReactiveAerospikeAccessorIT` exercises a reactive `put` → `get` → `delete`
round-trip via `ReactiveAerospikeAccessor`, and a synchronous `put` → `get` → `exists` → `delete` →
`exists` round-trip via `AerospikeAccessor`, end-to-end against a live Aerospike cluster; it is
gated by `-DAerospikeClientServiceTests.enabled=true` (same gate as
`DefaultAerospikeClientServiceTest`). There is no Docker script for Aerospike in this repo yet —
point the default `localhost:3000` config at a reachable instance, or override via the same
config-file system property the other gated Aerospike tests use. Tracked in P-051.

> **Build note** — the `.reactive` package is a distinct bnd `Export-Package` entry on this
> module (alongside the top-level accessor package); when building this module together with its
> reverse dependencies via `-am`, use the `install` goal (not just `compile`/`test`) so the OSGi
> manifest for the reactive package is generated and available to downstream modules.

### Reactive Jasync (P-027 R2a)

`appbase-client-jasync` wires both faces of the accessor layer over
[jasync-sql](https://github.com/jasync-sql/jasync-sql) (MySQL/MariaDB or Postgres).
`DefaultJasyncClientService` exposes:

| Accessor | Getter (default pool) | Getter (keyed pool) |
|---|---|---|
| `ReactiveJasyncAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<TextValue>)` |
| `JasyncAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<TextValue>)` |

**Reactive usage** — `sendQuery`/`sendPreparedStatement` return a `Mono<QueryResult>`; `queryRows`
is a convenience over `sendPreparedStatement` that flattens `QueryResult#getRows()` into a
`Flux<RowData>`:

```java
final ReactiveJasyncAccessor reactive = jasyncClientService.getReactiveAccessor();

Mono<QueryResult> inserted = reactive.sendPreparedStatement(
        "insert", "INSERT INTO kv (k, v) VALUES (?, ?)", List.of("key", "value"));
Flux<RowData> rows = reactive.queryRows("select", "SELECT v FROM kv WHERE k = ?", List.of("key"));
```

**Synchronous counterpart** — `JasyncAccessor` mirrors the same `sendQuery`/`sendPreparedStatement`/
`command` operation set against the driver's blocking API, throwing the mapped exception directly:

```java
final JasyncAccessor sync = jasyncClientService.getAccessor();
final QueryResult inserted = sync.sendPreparedStatement(
        "insert", "INSERT INTO kv (k, v) VALUES (?, ?)", List.of("key", "value")); // throws Exception
```

**The `Mono.fromFuture` bridge** — unlike the other reactive twins in this family, jasync's driver
API is `CompletableFuture`-based, not Reactor-native. `ReactiveJasyncAccessor` bridges every driver
call with `Mono.fromFuture(Supplier)` *lazily* inside the full `mono` spine inherited from
`AbstractReactiveAccessor` (timeout, retry/backoff, scheduler dispatch, metrics, tracing,
exception-store reporting), so a re-subscribed/retried `Mono` re-invokes the driver rather than
replaying a single captured future. Each subscription borrows one pooled
`AppbaseClientIntegrationService` and returns it via `doFinally` on completion, error, or
cancellation. There is deliberately no eager client build or cache: the pooled jasync `Connection`
(a `ConnectionPool` at runtime) IS the client, fetched lazily on each subscription — unlike the
Aerospike twin, which builds and caches a reactive facade once.

**The `.get()` + `ExecutionException` unwrap** — `JasyncAccessor` blocks on the driver's
`CompletableFuture` via `.get()`, then unwraps `ExecutionException` to its cause before the shared
error mapping runs, so the mapped failure is identical in shape to the reactive twin (which sees
failures via `CompletionException` on the `Mono.fromFuture` path). Both faces share
`JasyncAccessorException.map(Throwable, Function)`: a driver `DatabaseException` (unwrapped from
either `CompletionException` or `ExecutionException`) maps to `JasyncAccessorException`; every
other throwable falls through to the inherited default mapping.

**Configuration** — the reactive knobs are the same `reactiveOperationConfiguration` block (R0
knobs: `enabled`/`schedulerId`/`timeout`/retry*/`backpressureBufferSize`/`metricsEnabled`/
`traceEnabled`/`exceptionStoreEnabled`) on `DefaultJasyncClientServiceConfiguration`;
`driverMetricsEnabled` (default `true`) lives on the integration config:

```yaml
defaultDefaultJasyncClientIntegrationServiceConfiguration:
  textValueKeyConfiguration:
    text: "primary"
  databaseType: "MYSQL_OR_MARIADB"
  host: "localhost"
  port: 3306
  driverMetricsEnabled: true
reactiveOperationConfiguration:
  timeout: 5s
```

**Connection-pool gauges** — jasync has NO driver-native metrics registry (unlike Aerospike's
`MetricsListener` push model), so when `driverMetricsEnabled` is set, Appbase instead POLLS the
live `ConnectionPool` each scrape for pool-size gauges — `jasync.pool.connections.inuse`,
`jasync.pool.connections.idle`, and `jasync.pool.connections.waiting` — routed onto the shared
Micrometer composite registry (and therefore JMX). These gauges are unbound from the registry on
shutdown, so a subsequent `repair()`/reconnect re-registers fresh gauges against the new pool
instance rather than leaving stale gauges polling a disconnected `ConnectionPool`.

**Integration test** — `ReactiveJasyncAccessorIT` exercises a reactive `CREATE TABLE` →
parameterized `INSERT` → `SELECT` round-trip via `ReactiveJasyncAccessor` (asserting both the
returned `QueryResult` and the rows emitted by `queryRows`), and a synchronous `INSERT` → `SELECT`
round-trip via `JasyncAccessor`, each dropping its throwaway table in a `finally` block; it is
gated by `-DJasyncClientServiceTests.enabled=true` (same gate as `DefaultJasyncClientServiceTest`)
against a reachable MySQL/MariaDB or Postgres instance:

```shell
mvn test -pl appbase-client/appbase-client-jasync -Dtest=ReactiveJasyncAccessorIT \
  -DJasyncClientServiceTests.enabled=true -P quick-profile
```

Tracked in P-051.

> **Build note** — as with the other reactive twins, the `.reactive` package is a distinct bnd
> `Export-Package` entry on this module; when building this module together with its reverse
> dependencies via `-am`, use the `install` goal (not just `compile`/`test`) so the OSGi manifest
> for the reactive package is generated and available to downstream modules.

### Reactive Elasticsearch (P-027 R2a)

`appbase-client-elasticsearch` wires both faces of the accessor layer over the typed
`ElasticsearchRestClient` (the module's existing low-level REST client wrapper).
`DefaultElasticsearchRestClient` exposes:

| Accessor | Getter (default client) | Getter (keyed client) |
|---|---|---|
| `ReactiveElasticsearchAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| `ElasticsearchAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |

**Reactive usage** — every operation (`search`, `multiSearch`, `createIndex`, `existsIndex`,
`deleteIndex`, `addDocument`, `updateDocument`, `addOrUpdateDocument`, `deleteDocument`,
`cancelTask`) is a single bounded call, so it rides the full `mono` spine inherited from
`AbstractReactiveAccessor` (timeout, retry/backoff, scheduler dispatch, metrics, tracing,
exception-store reporting); `searchHits` is a convenience `Flux<Hits>` layered over `search`,
flattening the response's hit list with no separate driver call:

```java
final ReactiveElasticsearchAccessor reactive = elasticsearchRestClient.getReactiveAccessor();

Mono<CreateIndexResponse> created = reactive.createIndex("createIndex", createIndexRequest);
Mono<AddOrUpdateDocumentResponse> added = reactive.addDocument("addDocument", addDocumentRequest);
Flux<Hits> hits = reactive.searchHits("search", searchRequest);
```

**Synchronous counterpart** — `ElasticsearchAccessor` mirrors the same operation set against the
typed client's blocking methods, throwing the mapped exception directly:

```java
final ElasticsearchAccessor sync = elasticsearchRestClient.getAccessor();
final CreateIndexResponse created = sync.createIndex("createIndex", createIndexRequest); // throws Exception
```

**The `Mono.fromFuture` bridge** — the typed `ElasticsearchRestClient`'s `asyncXxx` methods already
return a `CompletableFuture`, so `ReactiveElasticsearchAccessor` bridges each call with
`Mono.fromFuture(Supplier)` inside the full `mono` spine; Reactor unwraps a future failure's
`CompletionException`, so `mapError` sees the raw driver exception. Unlike the Cassandra/jasync
reactive accessors, there is no `finalizeOperation`/borrow-return step here: the borrow is the
typed `ElasticsearchRestClient` itself (the default client or a key-bound view), not a pooled
`AppbaseClientIntegrationService` — the typed client's `asyncXxx` methods already own their own
integration-service borrow/return per call, so both accessors' borrow is used only to obtain that
client reference.

**Configuration** — the reactive knobs are the same `reactiveOperationConfiguration` block (R0
knobs: `enabled`/`schedulerId`/`timeout`/retry*/`backpressureBufferSize`/`metricsEnabled`/
`traceEnabled`/`exceptionStoreEnabled`); there is no separate driver-metrics knob for this twin (see
below).

**No driver-native metrics bridge** — unlike Aerospike's `MetricsListener` push model or jasync's
polled connection-pool gauges, this twin adds no additional driver-metrics wiring: the low-level
REST client's connection pooling is Apache HttpClient's own, and the accessor-level `Timer`/
`Counter` spine (inherited from `AbstractAppbaseAccessor`/`AbstractReactiveAccessor`) is the only
metrics surface for both faces.

**Integration test** — `ReactiveElasticsearchAccessorIT` exercises a reactive `createIndex` →
`addDocument` → `search` (asserting the indexed document is found) → `deleteIndex` round-trip via
`ReactiveElasticsearchAccessor`, and the same sequence synchronously via `ElasticsearchAccessor`.
This IT is **not** part of the default module build: Surefire's default includes (`*Test`/`*Tests`/
`Test*`/`*TestCase`) exclude `*IT` classes, and this module has no `maven-failsafe-plugin` to pick
them up, so a plain `mvn test`/`install` never selects `ReactiveElasticsearchAccessorIT` regardless
of any gate. It must be run explicitly with `-Dtest=ReactiveElasticsearchAccessorIT` against a live
Elasticsearch reachable at `localhost:9200`. Separately, it is also annotated with
`@EnabledIfSystemProperty(named = "ElasticsearchClientServiceTests.enabled", matches = "true")` —
that gate defaults to `false` in `appbase-client/pom.xml`, like every other client's gate (changed in
N10, internal work item 1300; it was previously the sole gate defaulting `true`), so it must be enabled explicitly with
`-DElasticsearchClientServiceTests.enabled=true` to run, on top of being explicitly targeted:

```shell
# Explicit selection AND explicit gate-enable required -- requires a live Elasticsearch at localhost:9200
mvn test -pl appbase-client/appbase-client-elasticsearch -Dtest=ReactiveElasticsearchAccessorIT \
  -DElasticsearchClientServiceTests.enabled=true -P quick-profile

# Gate left at its default (false) -- the IT is selected by name but skipped by @EnabledIfSystemProperty,
# confirming a compile-only skip with no live Elasticsearch. Passing =false explicitly is equivalent.
mvn test -pl appbase-client/appbase-client-elasticsearch -Dtest=ReactiveElasticsearchAccessorIT \
  -DElasticsearchClientServiceTests.enabled=false -P quick-profile
```

Tracked in P-051.

> **Build note** — as with the other reactive twins, the `.reactive` package (`ReactiveElasticsearchAccessor`)
> is a distinct bnd `Export-Package` entry on this module, so it is what's actually available to
> downstream modules via OSGi; the synchronous `ElasticsearchAccessor`/`ElasticsearchAccessorException`
> and the `ElasticsearchRestClient` interface live in the non-exported `restclient.api` package. When
> building this module together with its reverse dependencies via `-am`, use the `install` goal (not
> just `compile`/`test`) so the OSGi manifest for the reactive package is generated.

### Reactive ZooKeeper (P-027 R2a)

`appbase-client-zookeeper` wires both faces of the accessor layer over `CuratorFramework`
(Apache Curator). `DefaultZooKeeperClientService` exposes:

| Accessor | Getter (default client) | Getter (keyed client) |
|---|---|---|
| `ReactiveZooKeeperAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| `ZooKeeperAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |

**Reactive usage** — the 6 CRUD operations (`create`, `getData`, `setData`, `delete`,
`checkExists`, `getChildren`) are single bounded calls, so each rides the full `mono` spine
inherited from `AbstractReactiveAccessor` (timeout, retry/backoff, scheduler dispatch, metrics,
tracing, exception-store reporting); a generic `command` is also exposed for any other Curator
async call not covered by the 6:

```java
final ReactiveZooKeeperAccessor reactive = zooKeeperClientService.getReactiveAccessor();

Mono<String> created = reactive.create("create", "/my/path", data);
Mono<byte[]> read = reactive.getData("getData", "/my/path");
Mono<List<String>> children = reactive.getChildren("getChildren", "/my");

// Generic command -- any Curator async call, keyed only by op name for metrics/tracing.
Mono<Stat> stat = reactive.command("checkExists", async -> async.checkExists().forPath("/my/path"));
```

`command`'s functional parameter is a plain `Function<AsyncCuratorFramework, CompletionStage<T>>`
— unlike the synchronous twin below, no checked-exception-throwing shape is needed here because
Curator's async fluent calls return a `CompletionStage`/`AsyncStage` rather than throwing.

**Synchronous counterpart** — `ZooKeeperAccessor` mirrors the same 6 operations against Curator's
blocking fluent API, throwing the mapped exception directly; its generic `command` takes a
`ZooKeeperCommand<T>` instead of a `Function`, because Curator's blocking `forPath(...)` terminals
throw checked `Exception`, which `Function.apply` cannot declare:

```java
final ZooKeeperAccessor sync = zooKeeperClientService.getAccessor();
final String createdPath = sync.create("create", "/my/path", data); // throws Exception
final Stat stat = sync.command("checkExists", client -> client.checkExists().forPath("/my/path"));
```

**The `AsyncCuratorFramework`/`Mono.fromCompletionStage` bridge** — `CuratorFramework` itself is
purely blocking; `AsyncCuratorFramework.wrap(CuratorFramework)` (from Curator's `curator-x-async`
extension) adapts it into an async DSL whose operations each return an `AsyncStage<T>`, which
extends `CompletionStage<T>`. `ReactiveZooKeeperAccessor.command` wraps the borrowed client with
`AsyncCuratorFramework.wrap(...)` on every subscription and bridges the resulting
`CompletionStage` into Reactor via `Mono.fromCompletionStage(Supplier)`, inside `Mono.usingWhen`
so the borrowed `AppbaseClientIntegrationService` is returned to the pool on completion, error, or
cancellation alike. `Mono.fromCompletionStage` unwraps any `CompletionException` the stage
completes with, so `mapError` sees the original Curator/ZooKeeper cause rather than a wrapped one.
`checkExists` on an absent znode is the one CRUD op where this matters observably: Curator returns
a `null` `Stat` rather than throwing, and since Reactive Streams disallows `null` `onNext`, the
resulting `Mono<Stat>` simply completes empty instead of emitting `null`.

**Connection-state gauge** — Curator has no driver-native metrics registry (unlike
Aerospike's `MetricsListener` push model or jasync's polled connection-pool gauges), so when the
configuration's `driverMetricsEnabled` flag is set, `DefaultZooKeeperClientIntegrationService`
instead bridges the live `ConnectionStateListener` callback onto a `zookeeper.connection.state`
gauge on the shared Micrometer composite registry (→ JMX): `CONNECTED`/`RECONNECTED` → `1`,
`SUSPENDED` → `2`, `LOST` → `3`, `READ_ONLY` → `4`, `0` for the unknown/initial state. The gauge
is tagged with this service's own `AppbaseClientKey`, stringified, so keyed clients sharing one
composite registry register distinct Micrometer Ids and can be unbound independently; the
listener and gauge are removed on shutdown (and re-bound fresh on any subsequent repair/reconnect)
so a stale listener never keeps driving a gauge for a client that no longer exists. This gauge is
**in addition to, not instead of**, the accessor spine's per-operation `Timer`s/`Counter`s, which
both accessors already get from `AbstractAppbaseAccessor`/`AbstractReactiveAccessor` regardless of
`driverMetricsEnabled`.

**Integration test** — `ReactiveZooKeeperAccessorIT` exercises a reactive `create` → `getData` →
`setData` → `getData` → `getChildren` → `delete` → `checkExists` round-trip via
`ReactiveZooKeeperAccessor`, and the same sequence synchronously via `ZooKeeperAccessor`, each
against a throwaway znode under the ensemble root, cleaned up unconditionally in `@AfterAll`. It
is gated by `@EnabledIfSystemProperty(named = "ZooKeeperClientServiceTests.enabled", matches =
"true")` — like every other client's twin gate (Elasticsearch included since N10, internal work item 1300), this one
**defaults to `false`** in
`appbase-client/pom.xml` (the same gate `DefaultZooKeeperClientServiceTest` uses), so it must be
explicitly enabled. As with the Elasticsearch/jasync twins, this module has no
`maven-failsafe-plugin`, and Surefire's default includes (`*Test`/`*Tests`/`Test*`/`*TestCase`)
exclude `*IT` classes, so a plain `mvn test`/`install` never selects `ReactiveZooKeeperAccessorIT`
regardless of the gate — it must always be run with an explicit `-Dtest=`, against a live
ZooKeeper at `localhost:2181`:

```shell
# Explicit selection AND explicit gate enable are both required -- live ZooKeeper at localhost:2181
mvn test -pl appbase-client/appbase-client-zookeeper -Dtest=ReactiveZooKeeperAccessorIT \
  -DZooKeeperClientServiceTests.enabled=true -P quick-profile

# Default (gate false, not selected either way) -- compiles, reports the class skipped, no live ZooKeeper needed
mvn test -pl appbase-client/appbase-client-zookeeper -Dtest=ReactiveZooKeeperAccessorIT -P quick-profile
```

Tracked in P-051.

> **Build note** — as with the other reactive twins, the `.reactive` package
> (`ReactiveZooKeeperAccessor`) is a distinct bnd `Export-Package` entry on this module, so it is
> what's actually available to downstream modules via OSGi; the synchronous `ZooKeeperAccessor`/
> `ZooKeeperAccessorException`/`ZooKeeperCommand` live in the non-exported `accessor` package. When
> building this module together with its reverse dependencies via `-am`, use the `install` goal
> (not just `compile`/`test`) so the OSGi manifest for the reactive package is generated.

### Reactive Firebase (Auth) (P-027 R2a)

`appbase-client-firebase` wires both faces of the accessor layer over the Admin SDK's
`FirebaseAuth` client. `DefaultFirebaseAuthClientService` exposes:

| Accessor | Getter (default client) | Getter (keyed client) |
|---|---|---|
| `ReactiveFirebaseAuthAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<TextValue>)` |
| `FirebaseAuthAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<TextValue>)` |

**Reactive usage** — the 8 named operations (`createUser`, `getUser`, `getUserByEmail`,
`updateUser`, `deleteUser`, `setCustomUserClaims`, `verifyIdToken`, `createCustomToken`) are each a
single bounded call, so every one rides the full `mono` spine inherited from
`AbstractReactiveAccessor` (timeout, retry/backoff, scheduler dispatch, metrics, tracing,
exception-store reporting); a generic `command` is also exposed for any other Admin SDK async call
not covered by the 8:

```java
final ReactiveFirebaseAuthAccessor reactive = firebaseAuthClientService.getReactiveAccessor();

Mono<UserRecord> created = reactive.createUser("createUser", createRequest);
Mono<UserRecord> user = reactive.getUser("getUser", uid);
Mono<Void> claimsSet = reactive.setCustomUserClaims("setCustomUserClaims", uid, claims);

// Generic command -- any FirebaseAuth ApiFuture-returning call, keyed only by op name for
// metrics/tracing.
Mono<FirebaseToken> token = reactive.command("verifyIdToken", auth -> auth.verifyIdTokenAsync(idToken));
```

`command`'s functional parameter is a plain `Function<FirebaseAuth, ApiFuture<T>>` — unlike the
synchronous twin below, no checked-exception-throwing shape is needed here because the Admin SDK's
async calls return an `ApiFuture` rather than throwing.

**Synchronous counterpart** — `FirebaseAuthAccessor` mirrors the same 8 operations against the
Admin SDK's blocking sync API, throwing the mapped exception directly; its generic `command` takes
a `FirebaseAuthCommand<T>` instead of a `Function`, because the blocking calls' terminals throw
checked `FirebaseAuthException`, which `Function.apply` cannot declare:

```java
final FirebaseAuthAccessor sync = firebaseAuthClientService.getAccessor();
final UserRecord created = sync.createUser("createUser", createRequest); // throws Exception
final FirebaseToken token = sync.command("verifyIdToken", auth -> auth.verifyIdToken(idToken));
```

**The `ApiFuture`/`Mono.create` bridge** — Firebase's `ApiFuture` is not a
`java.util.concurrent.CompletionStage`, so `ReactiveFirebaseAuthAccessor.command` bridges each call
with `Mono.create`: `ApiFutures.addCallback` drives the sink directly on the future's own
completion thread, inside `Mono.usingWhen` so the borrowed `AppbaseClientIntegrationService` is
returned to the pool on completion, error, or cancellation alike. `onSuccess(null)` completes the
sink empty rather than calling `sink.success(null)`, which `Mono` disallows, so `Mono<Void>`
operations (`deleteUser`, `setCustomUserClaims`) complete cleanly; `onCancel` propagates Reactor
cancellation back to the `ApiFuture` via `ApiFuture#cancel(boolean)`.

**No driver-native metrics bridge** — like the Elasticsearch twin, this one adds no additional
driver-metrics wiring beyond the accessor-level `Timer`/`Counter` spine inherited from
`AbstractAppbaseAccessor`/`AbstractReactiveAccessor`; the Admin SDK's own HTTP transport owns its
connection pooling.

**Integration test** — `ReactiveFirebaseAuthAccessorIT` exercises a reactive `createUser` (random
uid/email) → `getUser` (asserting the uid/email) → `setCustomUserClaims` → `getUser` (asserting the
claim is present) → `deleteUser` → `getUser` (asserting the resulting error maps to
`FirebaseAuthAccessorException`, wrapping a `FirebaseAuthException` with `AuthErrorCode
.USER_NOT_FOUND`) round-trip via `ReactiveFirebaseAuthAccessor`, and the same sequence
synchronously via `FirebaseAuthAccessor`. It is gated by
`@EnabledIfSystemProperty(named = "FirebaseAuthClientServiceTests.enabled", matches = "true")` —
this gate **defaults to `false`** in `appbase-client/pom.xml`, the same convention as
ZooKeeper's/jasync's twins (and Elasticsearch's twin gate too, since N10 (internal work item 1300)), so it must
be explicitly enabled. As with the other twins, this module has no `maven-failsafe-plugin`, and
Surefire's default includes (`*Test`/`*Tests`/`Test*`/`*TestCase`) exclude `*IT` classes, so a
plain `mvn test`/`install` never selects `ReactiveFirebaseAuthAccessorIT` regardless of the gate —
it must always be run with an explicit `-Dtest=`, against a live Firebase project reachable via a
real service-account configuration (not the module's classpath test defaults) supplied through the
`appbase.testHelper.configFile.firebaseApp`/`appbase.testHelper.configFile.firebaseAuth` system
properties:

```shell
# Explicit selection AND explicit gate enable are both required -- live Firebase project, with
# real service-account config supplied via the two config-file system properties
mvn test -pl appbase-client/appbase-client-firebase -Dtest=ReactiveFirebaseAuthAccessorIT \
  -DFirebaseAuthClientServiceTests.enabled=true \
  -Dappbase.testHelper.configFile.firebaseApp=/path/to/firebase-app.yaml \
  -Dappbase.testHelper.configFile.firebaseAuth=/path/to/firebase-auth.yaml \
  -P quick-profile

# Selected via -Dtest but reported Skipped: gate defaults false -- compiles, reports the class
# skipped, no live Firebase project needed
mvn test -pl appbase-client/appbase-client-firebase -Dtest=ReactiveFirebaseAuthAccessorIT -P quick-profile
```

Tracked in P-051.

> **Build note** — as with the other reactive twins, the `.reactive` package
> (`ReactiveFirebaseAuthAccessor`) is a distinct bnd `Export-Package` entry on this module, so it is
> what's actually available to downstream modules via OSGi; the synchronous `FirebaseAuthAccessor`/
> `FirebaseAuthAccessorException`/`FirebaseAuthCommand` live in the non-exported `accessor`
> package. When building this module together with its reverse dependencies via `-am`, use the
> `install` goal (not just `compile`/`test`) so the OSGi manifest for the reactive package is
> generated.

### Reactive S3 (P-027 R2a)

`appbase-client-cloud-aws-s3` (module `appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3`,
package `com.viveworks.appbase.client.cloud.aws.s3`) wires both faces of the accessor layer over AWS
SDK v2. Unlike the driver-symmetric twins (Redis/Cassandra), the two faces run over **two
different clients**: the reactive face over the non-blocking `S3AsyncClient` (netty-nio
transport), the synchronous face over the real blocking `S3Client` (apache-client transport).
The sync face deliberately never blocks on the async client — blocking a non-blocking client's
completion still ties up a thread for the call's duration, defeating its purpose.
`S3ClientsService` exposes:

| Accessor | Getter (default pool) | Getter (keyed pool) |
|---|---|---|
| `ReactiveS3Accessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| `S3Accessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |

Each call borrows one pooled `S3Clients` (which IS the client — no eager build or cache) from
the corresponding `AppbaseClientIntegrationService`, applies the driver call, and returns it to
the pool on completion, error, or cancellation. Both accessors stay at the S3 driver-primitive
level: `bucket` is an argument on every named op (not fixed at construction), so one accessor
instance serves any bucket — there are deliberately **no** base-directory conveniences.

**Op vocabulary** (identical on both faces, per-op bucket): `readObjectAsBytes(op, bucket, key)`,
`readObjectAsFile(op, bucket, key, destination)`, `writeObject(op, bucket, key, byte[])`,
`writeObject(op, bucket, key, Path)`, `deleteObject(op, bucket, key)`, `listObjects(op, bucket,
prefix)`, plus the generic `command` escape hatch. The reactive `listObjects` returns
`Flux<String>` and the sync one returns `List<String>`; higher-level directory-style behaviour
(existence checks, prefix sweeps) is built by callers over `listObjects`/`command`.

**Reactive usage** — the reactive face bridges each `S3AsyncClient` `CompletableFuture` into the
Reactor world with `Mono.fromFuture` inside the full `AbstractReactiveAccessor` spine (timeout,
retry, scheduler, metrics, tracing, exception-store reporting):

```java
final ReactiveS3Accessor reactive = s3ClientsService.getReactiveAccessor();

Mono<String> key = reactive.writeObject("write", bucket, "k.txt", bytes);
Mono<byte[]> body = reactive.readObjectAsBytes("read", bucket, "k.txt");
Flux<String> keys = reactive.listObjects("list", bucket, "prefix/");
Mono<Boolean> deleted = reactive.deleteObject("delete", bucket, "k.txt");

// escape hatch — any S3AsyncClient CompletableFuture call
Mono<HeadObjectResponse> head = reactive.command("head",
        client -> client.headObject(HeadObjectRequest.builder().bucket(bucket).key("k.txt").build()));
```

**Synchronous counterpart** — the sync face runs the same ops on the real blocking `S3Client`
and returns plain blocking values (not `Mono`/`Flux`). Every op is declared `throws Exception`: a
driver failure surfaces to the caller mapped to the same `S3AccessorException` the reactive face
emits as an error signal:

```java
final S3Accessor sync = s3ClientsService.getAccessor();
try {
    final String written = sync.writeObject("write", bucket, "k.txt", bytes);
    final byte[] body = sync.readObjectAsBytes("read", bucket, "k.txt");
    final List<String> keys = sync.listObjects("list", bucket, "prefix/");
} catch (final Exception e) {
    // an AWS SDK failure surfaces here as an S3AccessorException —
    // the same mapped type the reactive face emits via onErrorMap
}
```

**Shared spine** — every operation through either accessor runs through the same
`AbstractAppbaseAccessor`/`AbstractReactiveAccessor` cross-cutting layer: per-op `Timer`/`Counter`
metrics on the shared `CompositeMeterRegistry` (auto-exported to JMX), a config-gated OpenTracing
span, failure logging, and AWS SDK `SdkException` (the base of all v2 errors, including
`S3Exception`/`NoSuchKeyException`) mapping to `S3AccessorException` — both faces map identically
via the shared `S3AccessorException.map(...)`, which unwraps the `CompletionException`
(async-callback path) / `ExecutionException` (blocking-`get` path) before the `SdkException` check.
The reactive face additionally applies the full R0 operator chain (timeout, retry/backoff,
backpressure buffering for `Flux`, scheduler dispatch) — see the `AbstractReactiveAccessor` section
and `ReactiveOperationConfiguration` knob table above.

**Configuration** — the reactive knobs are configured via the `reactiveOperationConfiguration`
block on `DefaultS3ClientsServiceConfiguration` (same knob set as the other twins; any subset may
be supplied, the rest keep their defaults):

```yaml
reactiveOperationConfiguration:
  timeout: 5s
```

**Integration test** — `ReactiveS3AccessorIT` exercises a `write` → `read` (payload asserted) →
`list` (asserting the key is present) → `delete` → `read-missing` (asserting the resulting error
maps to `S3AccessorException`, wrapping a `NoSuchKeyException`) round-trip via `ReactiveS3Accessor`
(with `StepVerifier`), and the same sequence synchronously via `S3Accessor` (blocking assertions).
It is gated by `@EnabledIfSystemProperty(named = "S3ClientServiceTests.enabled", matches = "true")`
— the same gate the module's other live S3 tests (`DefaultS3ClientServiceTest`,
`S3ClientServiceGuiceModuleTest`) use — and reuses their bootstrap helper,
`S3ClientServiceTestHelper.defaultS3ClientService()`, to build a live `DefaultS3ClientsService`
from the `appbase-client-aws-s3.yaml` classpath config; a bucket is created up front / torn down
after via the pooled sync `S3Client`. As with the other twins, this module has no
`maven-failsafe-plugin`, and Surefire's default includes (`*Test`/`*Tests`/`Test*`/`*TestCase`)
exclude `*IT` classes, so a plain `mvn test`/`install` never selects `ReactiveS3AccessorIT`
regardless of the gate — it must always be run with an explicit `-Dtest=`, against a real (or
emulated, e.g. LocalStack/MinIO) S3 endpoint reachable via the module's S3/HTTP-client config
(overridable through the `appbase.testHelper.configFile.s3Client` system property):

```shell
# Explicit selection AND explicit gate enable are both required -- live/emulated S3 endpoint
mvn test -pl appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3 -Dtest=ReactiveS3AccessorIT \
  -DS3ClientServiceTests.enabled=true \
  -P quick-profile

# Selected via -Dtest but reported Skipped: gate defaults off -- compiles, reports the class
# skipped, no live S3 endpoint needed
mvn test -pl appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3 -Dtest=ReactiveS3AccessorIT -P quick-profile
```

Tracked in P-051.

> **Build note** — as with the other reactive twins, the `.accessor.reactive` package
> (`ReactiveS3Accessor`) is a distinct bnd `Export-Package` entry on this module; the synchronous
> `S3Accessor`/`S3AccessorException` live in the parent `.accessor` package. When building this
> module together with its reverse dependencies via `-am`, use the `install` goal (not just
> `compile`/`test`) so the OSGi manifest for the reactive package is generated.

### Reactive Vitess (gRPC) (P-027 R2a)

`appbase-client-vitess` wires both faces of the accessor layer over the Vitess Java client.
Unlike the driver-symmetric twins (Redis/Cassandra/ZooKeeper), the two faces run over **two
different connections off the same pooled client**: the reactive face over the non-blocking
`VTGateConnection`, the synchronous face over the real blocking `VTGateBlockingConnection`
(`svc.getClient().getVtGateConnection()` / `getVtGateBlockingConnection()`). The sync face
deliberately never blocks on the async connection — blocking on a non-blocking client's
completion still ties up a thread for the call's duration, defeating the purpose of using it.
`DefaultVitessGrpcClientService` exposes:

| Accessor | Getter (default client) | Getter (keyed client) |
|---|---|---|
| `ReactiveVitessGrpcAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)` |
| `VitessGrpcAccessor` (synchronous) | `getAccessor()` | `getAccessor(AppbaseClientKey<ClusterIdValue>)` |

**Op vocabulary** (identical on both faces): `execute(op, sql, bindVars)`, `executeBatch(op,
sqlList, bindVarsList)`, `streamExecute(op, sql, bindVars)`, plus an explicit-session overload of
each (`execute(op, ctx, session, sql, bindVars)`, etc.) and a generic `command` escape hatch.

**The fresh-vs-explicit `VTSession` model** — every named op comes in two flavors. The
convenience form (`execute(op, sql, bindVars)`) mints a FRESH autocommit `VTSession` per call via
`VitessGrpcClientIntegrationService#newVtSession()` and uses the integration service's default
`Context` — safe for one-shot, non-transactional calls, and concurrency-safe on the reactive face
since each subscription gets its own session. The explicit form (`execute(op, ctx, session, sql,
bindVars)`) instead uses the CALLER's `Context`/`VTSession` verbatim (e.g. a transaction spanning
several calls, or a caller-supplied deadline/caller-id) and must NOT mint a fresh session, since
that would silently break the caller's session affinity:

```java
final ReactiveVitessGrpcAccessor reactive = vitessGrpcClientService.getReactiveAccessor();

// Fresh autocommit session, minted per call.
Mono<Cursor> selected = reactive.execute("select", "SELECT * FROM t WHERE id = 1", Map.of());

// Explicit caller-supplied context/session -- e.g. spanning a transaction.
Mono<Cursor> updated = reactive.execute("update", ctx, session, "UPDATE t SET v = 1", Map.of());

Flux<Row> rows = reactive.streamExecute("stream", "SELECT * FROM t", Map.of());

// Generic command -- any VTGateConnection call, keyed only by op name for metrics/tracing.
Mono<List<CursorWithError>> batch = reactive.command("command",
        (conn, ctx2, session2) -> conn.executeBatch(ctx2, sqlList, bindVarsList, session2));
```

**Synchronous counterpart** — `VitessGrpcAccessor` mirrors the same ops against the real blocking
`VTGateBlockingConnection`, throwing the mapped exception directly, and returns the blocking
`Cursor`/`CursorWithError` list directly (not `Mono`/`Flux`):

```java
final VitessGrpcAccessor sync = vitessGrpcClientService.getAccessor();
final Cursor cursor = sync.execute("select", "SELECT * FROM t WHERE id = 1", Map.of()); // throws Exception
final Cursor stream = sync.streamExecute("stream", "SELECT * FROM t", Map.of());
```

**The `SQLFuture`/`Mono` bridge** — `VTGateConnection`'s driver calls (`execute`/`executeBatch`)
return an `SQLFuture`, a Guava `ListenableFuture`, and throw a checked `SQLException`
synchronously for argument/connection-state failures raised before the future is even handed back
— distinct from a failure surfacing asynchronously through the future itself.
`ReactiveVitessGrpcAccessor`'s private `call`/`callExplicit` bridge helpers handle both: the
synchronous throw is caught and routed to `Mono#create`'s sink, and the future's eventual outcome
is bridged into the same sink via `Futures#addCallback` on Guava's `MoreExecutors#directExecutor()`
(cheap: the callback only forwards to the sink, no blocking work); `sink.onCancel` cancels the
underlying future so a downstream cancellation propagates to the in-flight gRPC call rather than
leaking it. `VTGateBlockingConnection`'s calls instead throw the checked `SQLException` directly —
there is no future to bridge on the sync face — unwrapped in `mapError` via the same
`VitessAccessorException.map(...)` helper both faces share, so error semantics stay identical.

**`streamExecute` Tier-1 offload** — `VTGateConnection.streamExecute`/`VTGateBlockingConnection
.streamExecute` have no reactive/future-based API at all: they return a blocking `Cursor` whose
`Cursor#next()` blocks per row. The reactive `streamExecute` is therefore a Tier-1 offload (see
`AbstractReactiveAccessor`): the driver call itself, and the per-row `cursor.next()` polling loop
that follows, both run on the configured scheduler via `subscribeOn`, so callers get
backpressure-aware `Flux<Row>` semantics without literally being non-blocking end-to-end; the
cursor is closed on completion, error, or cancellation via `Flux#using`. The synchronous
`streamExecute` instead returns the blocking `Cursor` directly, for the caller to drain (`cursor
.next()`) and close.

**Shared spine** — every operation through either accessor runs through the same
`AbstractAppbaseAccessor`/`AbstractReactiveAccessor` cross-cutting layer: per-op `Timer`/`Counter`
metrics on the shared `CompositeMeterRegistry`, a config-gated OpenTracing span, failure logging,
and exception-store reporting, plus Vitess driver failures mapped to `VitessAccessorException` on
both faces via the shared `VitessAccessorException.map(...)` helper. The reactive face
additionally applies the full R0 operator chain (timeout, retry/backoff, backpressure buffering
for `Flux`, scheduler dispatch) — see the `AbstractReactiveAccessor` section and
`ReactiveOperationConfiguration` knob table above. Each operation borrows one pooled
`VitessGrpcClientIntegrationService` per subscription/call and returns it to the pool on
completion, error, or cancellation alike; there is deliberately no eager client build or cache
(Cassandra/S3-shaped, keyed-free acquisition).

**Configuration** — the reactive knobs are configured via the `reactiveOperationConfiguration`
block on `DefaultVitessGrpcClientServiceConfiguration` (same knob set as the other twins; any
subset may be supplied, the rest keep their defaults):

```yaml
reactiveOperationConfiguration:
  timeout: 5s
```

**Integration test** — `ReactiveVitessGrpcAccessorIT` exercises a reactive `create` (table) ->
`insert` (asserting one row affected) -> `select` (draining the returned `Cursor` via `Cursor
.next()`, asserting the inserted value) -> `streamExecute` (asserting the streamed `Row` contains
the expected value) -> `drop` round-trip via `ReactiveVitessGrpcAccessor`, and the same sequence
synchronously via `VitessGrpcAccessor`. It is gated by `@EnabledIfSystemProperty(named =
"VitessClientServiceTests.enabled", matches = "true")` — the same gate the module's other live
Vitess tests (`DefaultVitessGrpcClientServiceTest`, `VitessGrpcClientServiceGuiceModuleTest`) use,
defaulting to `false` in `appbase-client/pom.xml` — and reuses their bootstrap helper,
`VitessGrpcClientServiceTestHelper.defaultVitessGrpcClientService()`, to build a live
`DefaultVitessGrpcClientService` from the `appbase-client-vitess-grpc.yaml` classpath config. As
with the other twins, this module has no `maven-failsafe-plugin`, and Surefire's default includes
(`*Test`/`*Tests`/`Test*`/`*TestCase`) exclude `*IT` classes, so a plain `mvn test`/`install` never
selects `ReactiveVitessGrpcAccessorIT` regardless of the gate — it must always be run with an
explicit `-Dtest=`, against a live vtgate reachable per the classpath config:

```shell
# Explicit selection AND explicit gate enable are both required -- live vtgate
mvn test -pl appbase-client/appbase-client-vitess -Dtest=ReactiveVitessGrpcAccessorIT \
  -DVitessClientServiceTests.enabled=true -P quick-profile

# Selected via -Dtest but reported Skipped: gate defaults false -- compiles, reports the class
# skipped, no live vtgate needed
mvn test -pl appbase-client/appbase-client-vitess -Dtest=ReactiveVitessGrpcAccessorIT -P quick-profile
```

Tracked in P-051.

> **Build note** — as with the other reactive twins, the `.accessor.reactive` package
> (`ReactiveVitessGrpcAccessor`) is a distinct bnd `Export-Package` entry on this module; the
> synchronous `VitessGrpcAccessor`/`VitessAccessorException`/`VitessCommand` live in the parent
> `.accessor` package. When building this module together with its reverse dependencies via `-am`,
> use the `install` goal (not just `compile`/`test`) so the OSGi manifest for the reactive package
> is generated.

### Reactive SQL (R2DBC) (P-027 R2b)

`appbase-client-r2dbc` is the reactive SQL face — genuinely non-blocking SQL over the
[R2DBC](https://r2dbc.io/) SPI, and the reactive counterpart of the blocking `appbase-client-jdbc`.
It is distinct from the Vitess twin above, which is object-query gRPC rather than SQL. See the
[module README](appbase-client/appbase-client-r2dbc/README.md).

**Reactive-only — there is no synchronous accessor, and no `getAccessor()`.** This is the one twin
that deliberately breaks the two-face symmetry: a driver whose entire purpose is non-blocking
access gains nothing from a synchronous face, and blocking on it would tie up a thread per call.
Callers wanting blocking SQL use `appbase-client-jdbc` (which itself has no accessor layer — it
hands out the pooled `DataSource` directly).

**The client model** — R2DBC is to JDBC as `ConnectionPool` is to `DataSource`.
`R2dbcClientService extends AppbaseClientService<ConnectionPool, TextValue>`: config-by-key is
preserved, with one native `io.r2dbc.pool.ConnectionPool` per key. The concrete driver is resolved
by configuration through the R2DBC SPI `ServiceLoader`, not by a compile-time dependency —
`r2dbc-mariadb`/`r2dbc-postgresql` are `optional`, `r2dbc-h2` is `test`-scoped.

| Accessor | Getter (default client) | Getter (keyed client) |
|---|---|---|
| `ReactiveR2dbcAccessor` (reactive) | `getReactiveAccessor()` | `getReactiveAccessor(AppbaseClientKey<TextValue>)` |

**Op vocabulary** — `query(op, sql, mapper, binds...)`, `update(op, sql, binds...)`, `batch(op,
sql, bindsList)`, and `command(op, fn)` run in AUTOCOMMIT against a fresh connection per
subscription. `inTransaction(op, work)` is the managed-transaction face instead.

**Nested `usingWhen` borrow** — every op borrows TWO resources, released in reverse on completion,
error, or cancellation alike: the OUTER `usingWhen` borrows a pooled `R2dbcClientIntegrationService`
and returns it via `finalizeOperation()`; the INNER one borrows a real `io.r2dbc.spi.Connection`
from that service's native pool (`ConnectionPool#create()`) and closes it via `Connection::close`.
Nothing is built eagerly and nothing is cached — both are fetched lazily per subscription
(Cassandra/S3/Vitess-shaped, keyed-free acquisition). R2DBC is natively Reactive Streams, so its
`Publisher`s are adapted with a plain `Flux.from(...)`/`Mono.from(...)` — there is no
future-to-`Mono` bridge here, unlike the Vitess (`SQLFuture`) or Firebase (`ApiFuture`) twins.

**`inTransaction` semantics** — pins ONE connection for the whole `work` function and wraps it in
`beginTransaction`/`commitTransaction`/`rollbackTransaction`. Three behaviours worth internalising:

- `work` must emit AT MOST ONE value; its `Publisher` is collapsed via `Mono.from(...)` before the
  commit step. Reduce multiple statements yourself (`.then(...)`, `collectList()`).
- "At most one" INCLUDES zero. An empty completion is a success, so it still commits — the terminal
  value is boxed in an `Optional` so it survives the commit `flatMap`, which a plain empty `Mono`
  would short-circuit.
- On failure the rollback is attempted and its OWN failure is swallowed, so the caller always sees
  the original error rather than a rollback failure masking it.

```java
final ReactiveR2dbcAccessor accessor = r2dbcClientService.getReactiveAccessor();

Flux<String> names = accessor.query("select", "SELECT name FROM t WHERE id = ?",
        (row, meta) -> row.get("name", String.class), 1);
Mono<Long> inserted = accessor.update("insert", "INSERT INTO t VALUES (?, ?)", 1, "a");

Mono<Long> moved = accessor.inTransaction("transfer", session ->
        Mono.from(session.update("UPDATE acct SET bal = bal - ? WHERE id = ?", 10, 1))
                .then(Mono.from(session.update("UPDATE acct SET bal = bal + ? WHERE id = ?", 10, 2))));
```

**Binds are positional and non-null only** — `Statement#bind(int, Object)` rejects `null`, since
R2DBC needs the SQL type for a null bind via `Statement#bindNull(int, Class)`. Callers needing a
typed null must drop to `command(...)` and bind the `Statement` themselves.

**Configuration** — a `url`, when set, is handed to `ConnectionFactoryOptions.parse(url)` and WINS
OUTRIGHT: every discrete field, `user`/`password` included, is then ignored. Use the discrete
`driver`/`host`/`port`/`database`/`user`/`password` fields when credentials are needed. Native
`ConnectionPool` metrics gauges are published when `driverMetricsEnabled` is `true` (the default);
reactive knobs live under `reactiveOperationConfiguration` as on every other twin.

**Tests** — this is the FIRST reactive twin with real CI coverage of a live backend.
`ReactiveR2dbcAccessorH2Test` is UNGATED and named `*Test` on purpose: H2 is in-memory and needs no
infra, so Surefire selects it in a plain build. It drives a full round-trip through a real,
bootstrapped `DefaultR2dbcClientService` (CREATE -> autocommit INSERT/SELECT -> `inTransaction`
COMMIT -> `inTransaction` ROLLBACK -> DROP), covering the acquisition path that
`ReactiveR2dbcAccessorTest` mocks out.

`ReactiveR2dbcAccessorMariaDbIT` is the server-backed twin of that round-trip, gated exactly like
the other twins' ITs — by `@EnabledIfSystemProperty(named = "R2dbcClientServiceTests.enabled")`
(default `false` in `appbase-client/pom.xml`) AND by its `*IT` suffix, which Surefire never selects
(this module has no `maven-failsafe-plugin`). Both must be overridden. Tracked in P-051.

```shell
# Ungated: runs in any normal build, no infra.
mvn test -pl appbase-client/appbase-client-r2dbc -P quick-profile

# Explicit selection AND explicit gate enable are both required -- live MariaDB on 3306.
./appbase-client/appbase-client-jdbc/infra/docker/mariadb/run-mariadb.sh
mvn test -pl appbase-client/appbase-client-r2dbc -Dtest=ReactiveR2dbcAccessorMariaDbIT \
  -DR2dbcClientServiceTests.enabled=true -P quick-profile

# Selected via -Dtest but reported Skipped: gate defaults false -- no live server needed.
mvn test -pl appbase-client/appbase-client-r2dbc -Dtest=ReactiveR2dbcAccessorMariaDbIT -P quick-profile
```

**Reactive SQL over Vitess** — Vitess speaks the MySQL wire protocol, so `r2dbc-mariadb` talks to
`vtgate` unchanged. Point `port` at `vtgate` (15306) instead of MariaDB (3306) in
`src/test/resources/appbase-client-r2dbc-mariadb.yaml` and run the gated IT above. That is this
umbrella's reactive-SQL-over-Vitess coverage, complementing the gRPC twin's object-query face.

## Reactive web layer (P-027 R1)

The `appbase-bootstrap-reactor-netty` module is the natively non-blocking HTTP face — reactor-netty
directly, no servlet container. It implements the existing `AppbaseWebServer` and is a peer of the
Jetty/Tomcat/Undertow servlet bootstraps, not wired through them. (Its own `README.md` is the
user-facing reference; this section is the build/architecture note.)

### Neutral exchange/filter abstraction lives in `appbase-service-core`

The request/response contract (`AppbaseServiceExchange`) and the filter contract
(`AppbaseServiceFilter` / `AppbaseServiceFilterChain`) are **protocol-neutral** and live in
`appbase-service-core`, not in this module. That is deliberate: the same endpoints and cross-cutting
filters must be expressible on both the servlet face and the reactive face, so the abstraction cannot
depend on either `HttpServletRequest` or reactor-netty types. The reactor-netty module supplies only
the transport-specific `NettyWebExchange` implementation.

### Filter order is a security invariant — authorization runs LAST

The reactive filter chain is `exceptionHandler → CORS → metric → traceId → userSession →
idpTokenCheck → authorization`. **Authorization is last**, so every earlier concern (tracing, session
resolution, IdP token check) has run before the access decision is made. Reordering is a security
regression, and the order is asserted by test.

### Native vs Tier-1 offloaded endpoints

Ten endpoints, all reproducing the read (`doGet`) behaviour of their servlet counterparts:

- **Native** (`/health`, `/metric`, `/serviceRegistry`, `/status`) — the read is in-memory and runs
  on the event loop directly.
- **Tier-1 offloaded** (`/log`, `/exception`, `/configuration`, `/core`, `/jmx`, `/mediator`) — the
  read is wrapped in `Mono.fromCallable(...).subscribeOn(offloadScheduler)`. For `/log`, `/exception`,
  `/configuration` the read genuinely blocks (Lucene / JPA / filesystem) and the offload is removed by
  R3; for `/core`, `/jmx`, `/mediator` the read is in-memory but the route co-hosts slow admin verbs,
  so the whole route is offloaded permanently. Only the read half is reproduced; mutating verbs stay
  servlet-only (P-039).

### Health snapshot — serve from memory, two defects repaired

The reactive `/health` serves a periodically-refreshed snapshot rather than running the checks inline
(running them inline would block the event loop). Building it repaired two live defects in the health
service, both now asserted by test:

1. **A stale snapshot served stale data.** A snapshot older than its freshness window now reports
   **unhealthy** instead of returning stale results as if current.
2. **An unhealthy snapshot re-ran the checks inline.** The old path re-invoked `checkHealth()` when the
   snapshot was unhealthy — blocking work on the caller's thread. A cold or unhealthy snapshot is now
   simply reported unhealthy; it is not a trigger to run checks inline. (A pre-existing test that
   asserted the re-run behaviour was asserting the bug and was corrected.)

### BlockHound enforces the non-blocking claim

The suite installs BlockHound, which fails the build if a blocking call runs on an event-loop thread.
Requires **BlockHound ≥ 1.0.13.RELEASE on JDK 25** (earlier releases fail to instrument), plus
`-XX:+AllowRedefinitionToAddDeleteMethods` and `-XX:+EnableDynamicAgentLoading`, supplied by the
module's surefire `argLine`. `boundedElastic` (the Tier-1 offload scheduler) is allowed to block; only
the event-loop threads are guarded. The end-to-end HTTP and back-pressure tests drive a real server on
an ephemeral port over a real socket.

```shell
# Ungated: runs in any normal build, no infra.
mvn test -pl appbase-bootstrap/appbase-bootstrap-reactor-netty -P quick-profile
```
