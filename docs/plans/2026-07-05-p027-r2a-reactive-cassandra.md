# P-027 R2a — Reactive Cassandra twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reactive Cassandra twin (DataStax native `executeReactive`) and a sync `CassandraAccessor`, both on the shared `AbstractAppbaseAccessor` spine, plus driver-native DataStax metrics into Appbase's Micrometer composite/JMX — in the existing `appbase-client-cassandra` module.

**Architecture:** Identical shape to the merged Redis twin. Accessors are plain POJOs the managed `DefaultCassandraClientService` hands out; reactive ops run through `AbstractReactiveAccessor.mono/flux(op, …)`, sync ops through `AbstractAppbaseAccessor.execute(op, Callable)`; each borrows a pooled `CassandraClientIntegrationService` (`getClient()` = `CqlSession`) per operation via `usingWhen` and returns it (`finalizeOperation()`). Driver metrics need DataStax ≥4.13, so a driver bump 4.12.0→4.17.0 is the prerequisite.

**Cross-cutting is inherited, not re-built.** Because both accessors extend the shared `AbstractAppbaseAccessor` spine (merged with the Redis twin), EVERY Cassandra op automatically gets — per operation, config-gated via the `ReactiveOperationConfiguration` field added in Task 2 — **metrics** (`Timer` + error `Counter` → JMX; `metricsEnabled`), **tracing** (child span start/finish, error-tagged; `traceEnabled`), **logging** (ERROR on failure), **exception mapping** (`mapError` → `CassandraAccessorException`), and **selective ExceptionStore reporting** (notable failures → mediator → store; `exceptionStoreEnabled`, default off). The twin only supplies the driver operation + `mapError`/`isNotable` overrides; it does NOT re-implement any cross-cutting concern. Tasks 3 and 5 explicitly assert tracing + ExceptionStore behaviour on the Cassandra path (below) so the inheritance is demonstrated, not assumed.

**Tech Stack:** Java 25, Maven, Project Reactor, DataStax java-driver-core 4.17.0 (reactive API `CqlSession.executeReactive` → `ReactiveResultSet` = Reactive Streams `Publisher<ReactiveRow>`), `java-driver-metrics-micrometer` (4.13+ Micrometer factory), MicroProfile Metrics, OpenTracing, JUnit 5 + Mockito + StepVerifier.

**Design source:** `docs/specs/2026-07-05-P027-R2a-reactive-cassandra-design.md`.
**Reference implementation:** the merged Redis twin (`…client.service.redis.reactive.ReactiveRedisAccessor`, `RedisAccessor`, `RedisAccessorException`, and Redis plan `2026-07-04-p027-r2a-reactive-redis.md`) — mirror it for every shared decision.

## Global Constraints

- **Per-task PR workflow:** each Task is its own branch off `feature/P-027-reactive-appbase` (name `feature/P-027-R2a-cas-t<N>-<slug>`), one PR into `feature/P-027-reactive-appbase`. Sequential — branch a task off the feature branch AFTER the previous task's PR merges. Wait for human merge before the next.
- **No new module.** All in `appbase-client-cassandra` (Cassandra code) / `appbase-core` (none expected — the composite accessor already exists from the Redis twin).
- **Project Reactor only.** Reactor + `reactor-test` already available (reactor-test is a test dep; add to `appbase-client-cassandra/pom.xml` test scope, version-less, if not present — managed in root via `${reactor.version}`).
- **Versions only in root `<dependencyManagement>`.** Bump `cassandra-client.version`; add `java-driver-metrics-micrometer` there.
- **Apache license header** (verbatim, same block as the Redis files) tops every new `.java`.
- **Authorship** — every new class Javadoc carries, in order: `@author Navaneeth Mysore G`, `@author Claude`, `@since 1.0.0`.
- **No fully-qualified class names** in code or tests — imports only.
- **Config pattern** — mirror `DefaultReactiveOperationConfiguration` / the Redis config exactly (immutable + `@JsonPOJOBuilder(withPrefix="")` Builder with `DEFAULT_*` + CheckStyle markers + validator honoring the JSR-303 contract).
- **Sensible defaults; zero-config works.**
- **Build command (verbatim — `-am` aborts at appbase-core without the surefire flag):**
  `cd /Users/nav/Viveworks/Technology/Appbase && mvn -q -pl appbase-java/appbase-client/appbase-client-cassandra -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=<Class> test`
- **Build regenerates `appbase-java/CHANGELOG.adoc`** — never stage/commit it.
- **No co-author / no "Generated with Claude Code"** in commits or PRs.
- **TDD, frequent commits.** Test-first per behavior.

**License header (verbatim):**
```java
/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Known types (verified against the module)

- `CassandraClientService extends AppbaseClientService<CqlSession, ClusterIdValue>`; `getClientIntegrationService()` / `(AppbaseClientKey<ClusterIdValue>)` → `CassandraClientIntegrationService`.
- `DefaultCassandraClientService extends AbstractAppbaseClientService<CqlSession, ClusterIdValue, DefaultCassandraClientServiceConfiguration>`; reaches core via `appbaseClientCore.getAppbaseCore()`; `getConfiguration()` returns the typed `DefaultCassandraClientServiceConfiguration`.
- `CassandraClientIntegrationService` = `AppbaseClientIntegrationService<CqlSession, ClusterIdValue>`; `getClient()` = `CqlSession`, `finalizeOperation()` returns to pool.
- `DefaultCassandraClientServiceConfiguration extends AbstractAppbaseClientServiceConfiguration<…Builder>`; the config interface is `CassandraClientServiceConfiguration`.
- Reactive: `CqlSession.executeReactive(Statement<?>)` → `com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet` (Publisher<`ReactiveRow`>). Sync: `session.execute(Statement<?>)` → `com.datastax.oss.driver.api.core.cql.ResultSet`. Statements: `com.datastax.oss.driver.api.core.cql.{Statement, SimpleStatement}`.
- Driver exceptions: `com.datastax.oss.driver.api.core.DriverException` (base).
- Session build: `AppbaseCassandraSessionBuilder extends SessionBuilder<AppbaseCassandraSessionBuilder, CqlSession>` — the metrics-registry wiring point for Task 6.
- Current pin: `cassandra-client.version = 4.12.0` (root `pom.xml:193`). Micrometer support requires ≥4.13.
- Shared spine already merged: `AbstractAppbaseAccessor`, `AbstractReactiveAccessor` (named `mono/flux(op, Supplier)`, overridable `mapError`/`isNotable`), `ReactiveOperationConfiguration` (+ `metricsEnabled`/`traceEnabled`/`exceptionStoreEnabled`), `MetricService.getCompositeMeterRegistry()`.

---

## Task 1: Bump DataStax driver 4.12.0 → 4.17.0 (prerequisite; own PR)

**Files:** Modify root `pom.xml` (`cassandra-client.version`). Possibly touch `AppbaseCassandraDriverContext.java` / `AppbaseCassandraNettyOptions.java` / `AppbaseCassandraSessionBuilder.java` if the internal driver SPI changed across 4.12→4.17.

**Interfaces:** none new. Produces: the module compiling + testing green on 4.17.0.

- [ ] **Step 1: Bump the version**

In root `pom.xml`, change `<cassandra-client.version>4.12.0</cassandra-client.version>` → `<cassandra-client.version>4.17.0</cassandra-client.version>`. (Covers `java-driver-core`, `java-driver-query-builder`, `java-driver-mapper-runtime` — all three share the property.) Confirm 4.17.0 resolves online.

- [ ] **Step 2: Compile the Cassandra module**

Run: `cd /Users/nav/Viveworks/Technology/Appbase && mvn -q -pl appbase-java/appbase-client/appbase-client-cassandra -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false test-compile`
Expected: compiles. If `AppbaseCassandraDriverContext` (extends the driver's `DefaultDriverContext`) or `AppbaseCassandraNettyOptions` (implements the driver's `NettyOptions`) fails against a changed internal SPI, fix the overrides to match 4.17's signatures — READ the driver's `DefaultDriverContext`/`NettyOptions` in 4.17 and adjust. These are internal-API extensions and are the most likely break points; if the break is deep/ambiguous, report it (BLOCKED) rather than guessing.

- [ ] **Step 3: Run the module unit tests**

Run: `mvn -q -pl appbase-java/appbase-client/appbase-client-cassandra -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: unit tests green (ITs are gated off — `@EnabledIfSystemProperty`, skipped). Note in the report that the Cassandra IT is gated (and may share the J-018 bootstrap gap).

- [ ] **Step 4: Commit + PR**

```bash
git checkout feature/P-027-reactive-appbase && git pull
git checkout -b feature/P-027-R2a-cas-t1-driver-bump
git add pom.xml   # + any driver-context/netty fixes
git commit -m "build(P-027-R2a): bump DataStax java-driver 4.12.0 -> 4.17.0 (enables Micrometer metrics for reactive Cassandra twin)"
git push -u origin feature/P-027-R2a-cas-t1-driver-bump
gh pr create --base feature/P-027-reactive-appbase --title "P-027 R2a Cassandra Task 1: bump DataStax driver 4.12->4.17" --body "…"
```
STOP — wait for human merge.

---

## Task 2: `ReactiveOperationConfiguration` field on the Cassandra client config

**Branch:** `feature/P-027-R2a-cas-t2-config`

**Files:**
- Modify: `.../cassandra/configuration/CassandraClientServiceConfiguration.java` (interface) + `DefaultCassandraClientServiceConfiguration.java` (+ validator if it enumerates fields)
- Test: `.../cassandra/configuration/DefaultCassandraClientServiceConfigurationTest.java`

**Interfaces:**
- Consumes: `ReactiveOperationConfiguration`, `DefaultReactiveOperationConfiguration` (from `appbase-client-core`, already a transitive dep).
- Produces: `CassandraClientServiceConfiguration.getReactiveOperationConfiguration() : ReactiveOperationConfiguration`; `DefaultCassandraClientServiceConfiguration.Builder.reactiveOperationConfiguration(...)`; default = `DefaultReactiveOperationConfiguration.builder().build()`.

- [ ] **Step 1: Failing test**
```java
@Test
void reactiveConfigDefaultsWhenOmitted() {
    final CassandraClientServiceConfiguration c = DefaultCassandraClientServiceConfiguration.builder().build();
    assertNotNull(c.getReactiveOperationConfiguration());
    assertTrue(c.getReactiveOperationConfiguration().isMetricsEnabled());
}
```
- [ ] **Step 2: Run — FAIL** (`getReactiveOperationConfiguration` undefined). Command: `… -Dtest=DefaultCassandraClientServiceConfigurationTest test`.
- [ ] **Step 3: Add the field** — READ `DefaultCassandraClientServiceConfiguration` first and copy an existing object-typed field end-to-end (getter, `private final`, ctor assignment from Builder, equals/hashCode/toString, Builder field + fluent setter + `DEFAULT_REACTIVE_OPERATION_CONFIGURATION = DefaultReactiveOperationConfiguration.builder().build()` inside the CheckStyle markers). Builder setter/field typed to the concrete `DefaultReactiveOperationConfiguration` (Jackson round-trip needs the concrete type — the interface has no `@JsonDeserialize`; this exact reasoning was verified in the Redis twin). Add the getter to the `CassandraClientServiceConfiguration` interface returning the interface type.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: YAML round-trip test** — assert an overridden nested value (e.g. `reactiveOperationConfiguration.timeout` or `.metricsEnabled`) deserializes; mirror an existing config YAML test in the module. Run — PASS.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): reactive operation configuration on Cassandra client config`). STOP for merge.

---

## Task 3: `ReactiveCassandraAccessor` + `CassandraAccessorException`

**Branch:** `feature/P-027-R2a-cas-t3-reactive-accessor`

**Files:**
- Create: `.../cassandra/reactive/CassandraAccessorException.java`
- Create: `.../cassandra/reactive/ReactiveCassandraAccessor.java`
- Test: `.../cassandra/reactive/ReactiveCassandraAccessorTest.java`
- Modify: `.../appbase-client-cassandra/pom.xml` — add `reactor-test` (test scope, version-less) if absent; add the new `.reactive` package to the OSGi `Export-Package`.

**Interfaces:**
- Consumes: `AbstractReactiveAccessor` (ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration)`; protected `mono(String, Supplier<Mono<T>>)`/`flux(String, Supplier<Flux<T>>)`; overridable `mapError`/`isNotable`), `AppbaseClientIntegrationService<CqlSession, ClusterIdValue>`, `CqlSession.executeReactive`, `ReactiveResultSet`/`ReactiveRow`, `Statement`, `DriverException`, `AppbaseClientException`.
- Produces:
  - `CassandraAccessorException extends AppbaseClientException` — ctors `(Throwable)` / `(String, Throwable)`; `serialVersionUID`; a public static `map(Throwable, Function<Throwable,Throwable> fallback)` mapping `DriverException` → `CassandraAccessorException`, else fallback (mirrors `RedisAccessorException.map`).
  - `ReactiveCassandraAccessor extends AbstractReactiveAccessor`; ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrow)`; methods:
    - `Flux<ReactiveRow> executeReactive(String op, Statement<?> statement)` = `flux(op, () -> Flux.usingWhen(Mono.fromSupplier(borrow), svc -> Flux.from(svc.getClient().executeReactive(statement)), svc -> Mono.fromRunnable(svc::finalizeOperation)))`.
    - `Mono<ReactiveRow> executeReactiveFirst(String op, Statement<?> statement)` = `Mono.from(executeReactive(op, statement))` — first row / empty. (Use `.next()` on the Flux to get a `Mono`.)
    - `<T> Flux<T> query(String op, Function<CqlSession, Publisher<T>> fn)` — general reactive escape hatch: `flux(op, () -> Flux.usingWhen(borrow, svc -> Flux.from(fn.apply(svc.getClient())), finalize))`.
    - `ReactiveResultSet reactive(CqlSession session, Statement<?> statement)` = `session.executeReactive(statement)` (raw escape hatch).
    - `@Override protected Throwable mapError(Throwable t)` = `return CassandraAccessorException.map(t, super::mapError);`
    - `@Override protected boolean isNotable(Throwable mapped)` = `return true;` (one-line Javadoc noting future refinement).

  `Publisher` = `org.reactivestreams.Publisher`; `Function`/`Supplier` = `java.util.function.*`.

- [ ] **Step 1: Failing test** — mock `borrow`→`AppbaseClientIntegrationService`→`getClient()`→`CqlSession`; stub `session.executeReactive(any())` → a mocked `ReactiveResultSet` that, as a `Publisher<ReactiveRow>`, emits a mocked `ReactiveRow` (use `Flux.just(row)` behind a thin `ReactiveResultSet` mock, or return a real Publisher via Mockito `thenAnswer`); mock `ReactorService` (`getDefaultScheduler()`→`Schedulers.immediate()`, `getSchedulerById(anyString())`→null); mock `AppbaseCore`→`LogService`/`MetricService`(+registry+`Timer`+`Counter`)/`TracerService`/`MediatorService` (reuse the Redis `ReactiveRedisAccessorTest`/`AbstractReactiveAccessorTest` fixture style — READ them). Config `DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build()`.
```java
@Test void executeReactiveEmitsRows() {
    StepVerifier.create(accessor.executeReactive("select", statement)).expectNext(row).verifyComplete();
}
@Test void integrationServiceReturnedOnSuccessAndError() {
    accessor.executeReactive("select", statement).blockLast();
    accessor.executeReactive("select", errorStatement).onErrorResume(e -> Flux.empty()).blockLast();
    Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
}
@Test void driverExceptionMappedToCassandraAccessorException() {
    // session.executeReactive returns a Publisher that errors with a DataStax DriverException subclass
    StepVerifier.create(accessor.executeReactive("select", statement))
            .expectError(CassandraAccessorException.class).verify();
}
@Test void borrowIsLazyPerSubscription() {
    accessor.executeReactive("select", statement);   // no subscribe
    Mockito.verifyNoInteractions(borrowSupplier);
}
@Test void traceSpanStartedPerOpWhenTraceEnabled() {
    // config with traceEnabled(true) (default); mocked TracerService/Tracer/Span
    accessor.executeReactive("select", statement).blockLast();
    Mockito.verify(tracer).buildSpan(Mockito.anyString());   // a span was started for the op
    Mockito.verify(span).finish();                            // and finished on terminate
}
@Test void notableFailureReportedToExceptionStoreWhenEnabled() {
    // config with exceptionStoreEnabled(true), retryEnabled(false); mocked MediatorService/Mediator
    accessor.executeReactive("select", errorStatement).onErrorResume(e -> Flux.empty()).blockLast();
    Mockito.verify(mediator).asyncHandleMessage(Mockito.any(), Mockito.anyBoolean());  // notable → mediator → ExceptionStore
}
@Test void exceptionStoreNotTouchedWhenDisabled() {
    // config with exceptionStoreEnabled(false) [default]
    accessor.executeReactive("select", errorStatement).onErrorResume(e -> Flux.empty()).blockLast();
    Mockito.verifyNoInteractions(mediator);
}
```
> These three assert the shared spine's tracing + selective-ExceptionStore behaviour on the Cassandra reactive path (metrics/counter already covered above). The spine itself was unit-tested in the Redis Task 1, but re-asserting here proves the Cassandra twin actually routes every op through the spine (nothing bypasses `flux(op,…)`). Reuse the same `TracerService`/`Span`/`MediatorService` mock fixtures the Redis `ReactiveRedisAccessorTest` / `AbstractAppbaseAccessorTest` use.
> For the DriverException test, use a concrete subclass (e.g. `com.datastax.oss.driver.api.core.servererrors.ServerError` or `AllNodesFailedException` if simply constructible) — if none is trivially constructible, mock a `DriverException` subtype or make the publisher error with `new DriverException("x", null, false){}` anonymous subclass.
- [ ] **Step 2: Run — FAIL** (classes absent).
- [ ] **Step 3: Write `CassandraAccessorException` (stub)** — trivial `extends AppbaseClientException` + the static `map(...)` (import `com.datastax.oss.driver.api.core.DriverException`, `java.util.function.Function`).
- [ ] **Step 4: Write `ReactiveCassandraAccessor` (stub)** — the methods above; `Flux.usingWhen` for borrow/return (fires cleanup on complete/error/cancel).
- [ ] **Step 5: pom** — add `reactor-test` (test) if absent; add `com.viveworks.appbase.client.service.cassandra.reactive` to `Export-Package`. `xmllint --noout` the pom.
- [ ] **Step 6: Run — PASS** (4 tests). Then whole module (drop `-Dtest`) — no regressions.
- [ ] **Step 7: Commit + PR** (`feat(P-027-R2a): ReactiveCassandraAccessor with executeReactive + pooled usingWhen borrow/return + DataStax error mapping`). STOP for merge.

---

## Task 4: `getReactiveAccessor()` acquisition on the Cassandra client service

**Branch:** `feature/P-027-R2a-cas-t4-acquisition`

**Files:**
- Modify: `.../cassandra/CassandraClientService.java` (interface) + `DefaultCassandraClientService.java`
- Test: `.../cassandra/DefaultCassandraClientServiceReactiveTest.java`

**Interfaces:**
- Produces: `CassandraClientService.getReactiveAccessor() : ReactiveCassandraAccessor` + `getReactiveAccessor(AppbaseClientKey<ClusterIdValue>)`; `DefaultCassandraClientService` builds the accessor with `appbaseClientCore.getAppbaseCore()`, `.getReactorService()`, `getConfiguration().getReactiveOperationConfiguration()`, borrow = `this::getClientIntegrationService` / `() -> getClientIntegrationService(key)`.

- [ ] **Step 1: Failing test** — build a `DefaultCassandraClientService` via the pool-accepting constructor with mocks and `autoStart(false)` (mirror the Redis `DefaultRedisClientServiceReactiveTest` exactly — non-gated Mockito unit test, no live Cassandra); assert `getReactiveAccessor()` non-null and its `getReactiveOperationConfiguration().isMetricsEnabled()` matches a distinguishable service-config value (set `metricsEnabled=false`).
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Add the methods (stub)** — interface declarations + `DefaultCassandraClientService` impl mirroring Redis Task 4. Import `ReactiveCassandraAccessor`, `ReactorService`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module test** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): CassandraClientService.getReactiveAccessor() acquisition`). STOP for merge.

---

## Task 5: Sync `CassandraAccessor` on the shared spine

**Branch:** `feature/P-027-R2a-cas-t5-sync-accessor`

**Files:**
- Create: `.../cassandra/CassandraAccessor.java`
- Modify: `.../cassandra/CassandraClientService.java` + `DefaultCassandraClientService.java` — add `getAccessor()` / `getAccessor(key)`
- Test: `.../cassandra/CassandraAccessorTest.java`

**Interfaces:**
- Produces: `CassandraAccessor extends AbstractAppbaseAccessor`; ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrow)` → `super(appbaseCore, cfg.isMetricsEnabled(), cfg.isTraceEnabled(), cfg.isExceptionStoreEnabled())`; methods:
  - `ResultSet execute(String op, Statement<?> statement) throws Exception` = `execute(op, () -> { svc = borrow.get(); try { return svc.getClient().execute(statement); } finally { svc.finalizeOperation(); } })` (the base `execute(String, Callable)` — NOTE the name clash: give the public method a distinct name to avoid shadowing the protected base `execute`; call it `executeStatement(String op, Statement<?>)` OR keep `execute` but be careful — RECOMMEND `executeStatement` to avoid confusion with the base `execute(op, Callable)`).
  - `<T> T command(String op, Function<CqlSession, T> fn) throws Exception` — general sync escape hatch via base `execute`.
  - `@Override mapError` = `CassandraAccessorException.map(t, super::mapError)`; `@Override isNotable` = true.
- `CassandraClientService.getAccessor()` / `getAccessor(key)`.

> Naming: the base class already has a protected `<T> T execute(String, Callable<T>)`. The public sync statement method MUST NOT collide — name it `executeStatement(String, Statement<?>)`. Both `executeStatement` and `command` declare `throws Exception` (inherited from the base `execute` contract; `CassandraAccessorException` is a checked `AppbaseClientException`) — this matches the Redis sync accessor's decision.

- [ ] **Step 1: Failing test** — mock borrow→svc→`getClient()`→`CqlSession`; stub `session.execute(any())`→a mocked `ResultSet`; `MetricService`/`TracerService`/`MediatorService` mocks; assert `executeStatement("select", stmt)` returns the ResultSet, records a `Timer`, increments error counter + maps `DriverException`→`CassandraAccessorException` on failure, returns the svc to the pool in both cases, starts+finishes a trace span when `traceEnabled`, and reports a notable failure to the mediator when `exceptionStoreEnabled` (and NOT when disabled) — same cross-cutting assertions as the reactive accessor, proving the sync `execute(op,…)` path also routes through the spine.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Write `CassandraAccessor` (stub).**
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Add `getAccessor()` acquisition + a non-gated unit test** (mirror Task 4 / Redis Task 5). Whole-module — PASS.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): sync CassandraAccessor on shared spine + shared DataStax error mapping`). STOP for merge.

---

## Task 6: DataStax-native driver metrics → Appbase composite/JMX

**Branch:** `feature/P-027-R2a-cas-t6-driver-metrics`

**Files:**
- Modify: root `pom.xml` — add managed `com.datastax.oss:java-driver-metrics-micrometer` at `${cassandra-client.version}` (4.17.0). *(Implementer: confirm the exact artifactId + `MicrometerMetricsFactory` FQCN online — DataStax 4.13+ ships Micrometer support in this add-on module.)*
- Modify: `.../appbase-client-cassandra/pom.xml` — add the `java-driver-metrics-micrometer` dependency.
- Modify: `.../cassandra/configuration/CassandraClientIntegrationServiceConfiguration.java` (interface, if it declares getters) + `DefaultCassandraClientIntegrationServiceConfiguration.java` — add `boolean isDriverMetricsEnabled();` (default true), full config-pattern wiring.
- Modify: `.../cassandra/AppbaseCassandraSessionBuilder.java` and/or `DefaultCassandraClientIntegrationService.java` — wire the Micrometer registry + metrics factory when the flag is on.
- Test: `.../cassandra/DefaultCassandraClientIntegrationServiceMetricsTest.java` + config test.

**Interfaces:**
- Consumes: `MetricService.getCompositeMeterRegistry()` (interface method added in the Redis twin), `SessionBuilder.withMetricRegistry(Object)` (DataStax 4.13+), the driver's `MicrometerMetricsFactory` (set via `advanced.metrics.factory.class` in the driver config loader), the session/node metric enablement config keys.
- Produces: sessions built (when `driverMetricsEnabled`) with `withMetricRegistry(appbaseCore.getMetricService().getCompositeMeterRegistry())` + `MicrometerMetricsFactory` + enabled session metrics → DataStax metrics land in Appbase's composite (→ JMX).

- [ ] **Step 1: Confirm the API online** — resolve `com.datastax.oss:java-driver-metrics-micrometer:4.17.0`; confirm the `MicrometerMetricsFactory` class FQCN and that `SessionBuilder.withMetricRegistry(...)` accepts a Micrometer `MeterRegistry` (4.13+). If the wiring differs (e.g. the factory is set only via config, not a builder call), adapt — READ the driver's Micrometer module. If it can't be made to bind to an external registry, report BLOCKED with findings (do NOT silently fall back to Dropwizard-only).
- [ ] **Step 2: Config flag** — add `isDriverMetricsEnabled()` (default true) to `DefaultCassandraClientIntegrationServiceConfiguration` mirroring an existing boolean field; test defaults + override (mirror Redis Task 7's config test).
- [ ] **Step 3: Failing wiring test** — with the flag true and a mocked `MetricService.getCompositeMeterRegistry()` returning a real `SimpleMeterRegistry`, assert the session-build path consults `getCompositeMeterRegistry()` (Mockito verify) / passes it to `withMetricRegistry`. If the real session build hits a live Cassandra connect that can't be mocked, assert the observable seam (registry consulted) and defer end-to-end to the gated IT — mirror how Redis Task 7 handled the unmockable connect (config-flag + seam assertions). Do NOT invent a bootstrap.
- [ ] **Step 4: Wire it (stub)** — in the session-build path, when `driverMetricsEnabled`: configure the driver config loader with `advanced.metrics.factory.class = MicrometerMetricsFactory` + enabled session metrics, and `sessionBuilder.withMetricRegistry(getCompositeMeterRegistry())`. Reach the composite via `appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry()`.
  > LIFECYCLE: unlike Lettuce's `ClientResources`, the DataStax `MeterRegistry` here IS Appbase's shared composite (owned by `MetricService`) — do NOT shut it down on session close. The session owns no registry lifecycle. Confirm no per-session registry is created that would leak.
- [ ] **Step 5: Run — PASS.** Whole-module — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): route DataStax driver metrics into Appbase composite/JMX (config-gated)`). STOP for merge.

---

## Task 7: Opt-in IT + docs

**Branch:** `feature/P-027-R2a-cas-t7-it-docs`

**Files:**
- Test: `.../cassandra/reactive/ReactiveCassandraAccessorIT.java` (gated like the existing Cassandra IT)
- Modify: `appbase-java/build-guide.md`

**Interfaces:** none new (exercises Tasks 3–6 end-to-end).

- [ ] **Step 1: Write the opt-in IT** — READ the existing Cassandra IT first (find it; `@EnabledIfSystemProperty` gate + `CassandraClientTestHelper` bootstrap) and mirror its gating/`@BeforeAll`/`@AfterAll`. Body: `getReactiveAccessor()`, reactive `CREATE KEYSPACE/TABLE` → `INSERT` → `SELECT` round-trip via `executeReactive(...)` with StepVerifier asserting the selected row; assert a Timer/metric registered for the accessor (and, if Task 6 landed, a DataStax `cql-requests`-style metric in the registry). Use `SimpleStatement.newInstance(cql)`.
- [ ] **Step 2: Run gated OFF** — `… -Dtest=ReactiveCassandraAccessorIT test` (flag unset) → skipped; BUILD SUCCESS. (Note: the Cassandra IT may share the J-018 bootstrap gap — if it can't run gated-on, note it; don't fix here.)
- [ ] **Step 3: Docs** — add `### Reactive Cassandra (P-027 R2a)` to `build-guide.md` after the Redis subsection: `getReactiveAccessor()`/`getAccessor()` (+ keyed), `executeReactive`/`executeReactiveFirst`/`query`/`reactive` escape hatch, sync `executeStatement`/`command` (note `throws`), shared-spine cross-cutting (metrics→JMX, trace, log, DataStax→`CassandraAccessorException`), DataStax driver-native metrics (Task 6, config-gated), the `reactiveOperationConfiguration` knobs (reference the R0 table). Verify: `grep -n "Reactive Cassandra" appbase-java/build-guide.md`.
- [ ] **Step 4: Commit + PR** (`test(P-027-R2a): opt-in reactive Cassandra IT + build-guide docs`). STOP for merge.

---

## Self-Review (completed by plan author)

- **Spec coverage:** Task 1 = driver bump (design §4.1, Decision A/C); Task 2 = config field (§2.6); Task 3 = `ReactiveCassandraAccessor` + exception + `executeReactive`/`usingWhen`/mapping (§2.1–2.5, §3); Task 4 = acquisition (§3); Task 5 = sync accessor (§3); Task 6 = DataStax metrics→composite (§4.6, Decision A); Task 7 = IT + docs (§5). No entity mapping anywhere (Decision B).
- **Placeholder scan:** implementations stubbed per project convention (user reviews each PR); every TDD RED step has real test code + an exact command; the two genuinely-online-only unknowns (4.17 driver-context break points; exact `java-driver-metrics-micrometer` artifact/class) are called out as explicit "confirm online / report BLOCKED" steps, not silent TODOs.
- **Type consistency:** `executeReactive`/`executeReactiveFirst`/`query`/`reactive`, `executeStatement`/`command`, `getReactiveAccessor`/`getAccessor`, `getReactiveOperationConfiguration`, `CassandraAccessorException.map`, `isDriverMetricsEnabled` used consistently across tasks. Sync public method named `executeStatement` to avoid clashing with the base protected `execute(String, Callable)`.
- **Divergences from the Redis plan (intentional):** (a) a driver-bump prerequisite task (Redis needed none); (b) reactive result is Flux-shaped via `executeReactive` (Redis wrapped per-command Monos); (c) Task 6 binds Appbase's OWN composite into the driver (no per-session registry to shut down) — the opposite lifecycle concern from Redis's `ClientResources`.
