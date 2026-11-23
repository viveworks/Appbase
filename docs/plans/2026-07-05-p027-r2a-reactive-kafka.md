# P-027 R2a — Reactive Kafka Twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reactive + synchronous accessor faces to the Kafka producer and consumer client services on reactor-kafka, on the shared Appbase accessor spine, with driver-native metrics into the composite/JMX.

**Architecture:** In-module (`appbase-client-kafka`). reactor-kafka `KafkaSender`/`KafkaReceiver` own their own client built from the same config properties as the pooled sync `Producer`/`Consumer`. Producer `send` runs the full R0 spine; the long-lived consumer `receive` runs a lighter `fluxStream` spine. Four accessors (reactive+sync × producer+consumer). Kafka native metrics bound to the composite for both pooled and reactor-kafka clients, config-gated.

**Tech Stack:** Java 25, Project Reactor 3.6.8, reactor-kafka 1.3.25, kafka-clients (3.7.1 → 3.9.1 if clean), micrometer-core 1.13.1 (`KafkaClientMetrics`), Guice, Maven, JUnit 5 + Mockito + `reactor-test` StepVerifier.

**Spec:** `docs/specs/2026-07-05-P027-R2a-reactive-kafka-design.md`
**Reference twins (mirror these exactly):** merged Redis (`appbase-client-redis/.../reactive/ReactiveRedisAccessor.java`, `RedisAccessor.java`, `RedisAccessorException.java`, `DefaultRedisClientService` acquisition, `RedisAccessorTest.java`, `ReactiveRedisAccessorIT.java`) and Cassandra (`appbase-client-cassandra/.../reactive/*`, `CassandraAccessor.java`, `DefaultCassandraClientIntegrationService.applyProgrammaticDriverConfig`, `DefaultCassandraClientServiceReactiveTest.java`).

## Global Constraints

- **Branch/PR (NESTED twin branch):** one PR PER TASK, branch `feature/P-027-R2a-kafka-t<N>-<slug>`. Task 1 (dep bump, internal work item 295) merges FLAT into `feature/P-027-reactive-appbase` as an exception; from Task 2 on, PRs target the **twin branch** `feature/P-027-R2a-kafka` (created off reactive HEAD once internal work item 295 merges). When all Kafka tasks are merged, open `feature/P-027-R2a-kafka` → `feature/P-027-reactive-appbase` (closes twin issue internal work item 297). STOP for human merge before each next task. Twin-level GitHub issue only (internal work item 297): post each task PR link there in sequence + tick its checkbox.
- **Every Java file:** Apache license header (copy verbatim from a sibling in the module), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`. No fully-qualified class names in code (use imports) — sole exception: string constants naming classes.
- **Decisions documented in code (Decision E):** every non-obvious choice gets a Javadoc/inline rationale at the code site (why `receive` uses `fluxStream`, reactor-kafka owns its client, sender/receiver shutdown leak trap, `doOnConsumer`-needs-active-subscription, metrics binder lifecycle, any name chosen to avoid a base-method collision).
- **Dependency versions ONLY in root `pom.xml` `<dependencyManagement>`**; module poms are version-less.
- **Build command** (module + deps; use `install`, NOT `test` — bnd Export-Package on `.reactive` fails under `test`):
  `cd /Users/nav/Viveworks/Technology/Appbase/appbase-java && mvn -q -pl appbase-client/appbase-client-kafka -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`
  `-Dsurefire.failIfNoSpecifiedTests=false` is MANDATORY (else `-am` aborts at appbase-core). Scope a single class with `-Dtest=<Class>` (still `install`).
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.** (regenerated; never commit).
- **Generics:** all accessors are generic `<K,V>`; key type is `AppbaseClientKey<ClusterIdValue>`.
- **Gated tests → J-018** (per standing policy). **Learnings → `appbase-java/LEARNING.md`** at the end.

---

## File Structure

**Core (appbase-client-core):**
- Modify `.../client/core/reactive/AbstractReactiveAccessor.java` — add protected `fluxStream(op, Supplier<Flux<T>>)`.

**Kafka common (`.../service/kafka/`):**
- Create `KafkaAccessorException.java` — shared driver-error mapping (producer + consumer).

**Producer (`.../service/kafka/producer/`):**
- Modify `configuration/KafkaProducerClientServiceConfiguration.java` + `DefaultKafkaProducerClientServiceConfiguration.java` — `reactiveOperationConfiguration`.
- Modify `configuration/KafkaProducerClientIntegrationServiceConfiguration.java` + `DefaultKafkaProducerClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled`.
- Modify `DefaultKafkaProducerClientIntegrationService.java` — extract `buildProducerProperties()`; bind pooled-`Producer` `KafkaClientMetrics` (config-gated); close on shutdown.
- Create `reactive/ReactiveKafkaProducerAccessor.java`; create `KafkaProducerAccessor.java` (sync).
- Modify `KafkaProducerClientService.java` + `DefaultKafkaProducerClientService.java` — `getReactiveAccessor()`/`getAccessor()`.

**Consumer (`.../service/kafka/consumer/`):** symmetric with producer (`ReactiveKafkaConsumerAccessor` `receive`, `KafkaConsumerAccessor` `poll`, `buildConsumerProperties()`, consumer `driverMetricsEnabled` + `KafkaClientMetrics`).

**Module pom:** add `reactor-kafka`, `micrometer-core` (if not already a direct dep), `reactor-test` (test), and Export-Package the new `.reactive` packages.

**Tests/IT + docs:** unit tests per accessor + acquisition + metrics seam; gated `ReactiveKafkaProducerAccessorIT`/`...ConsumerAccessorIT`; `build-guide.md` "Reactive Kafka" section.

---

## Task 1: reactor-kafka dependency + kafka-clients bump

**Branch:** `feature/P-027-R2a-kafka-t1-deps`
**Files:** Modify root `pom.xml` (`<dependencyManagement>` + `<kafka-clients.version>`); modify `appbase-java/appbase-client/appbase-client-kafka/pom.xml`.

**Interfaces:**
- Produces: `io.projectreactor.kafka:reactor-kafka` available to the kafka module; `kafka-clients` at the chosen version.

- [ ] **Step 1: Add managed dep.** In root `pom.xml` `<dependencyManagement>`, add `io.projectreactor.kafka:reactor-kafka` at a new `<reactor-kafka.version>1.3.25</reactor-kafka.version>` property (add the property near `${reactor.version}`).
- [ ] **Step 2: Bump kafka-clients (conditional).** Change `<kafka-clients.version>3.7.1` → `3.9.1`.
- [ ] **Step 3: Module dep.** In `appbase-client-kafka/pom.xml`, add version-less `io.projectreactor.kafka:reactor-kafka`.
- [ ] **Step 4: Full-build regression** across every Kafka consumer of the pin:
  `cd appbase-java && mvn -q -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install` (whole reactor). Expected: BUILD SUCCESS; kafka producer/consumer modules + `appbase-stacktest` green.
- [ ] **Step 5: Decision gate.** If 3.9.1 causes ANY failure, revert `kafka-clients` to `3.7.1` (the root depMgmt pin still overrides reactor-kafka's transitive 3.9.1) and record the reason in the PR description + the SDD ledger. Do NOT force 3.9.1 through a break.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): add reactor-kafka 1.3.25 + kafka-clients 3.9.1`). STOP for merge.

---

## Task 2: `reactiveOperationConfiguration` on both Kafka client-service configs

**Branch:** `feature/P-027-R2a-kafka-t2-config`
**Files:**
- Modify: `producer/configuration/KafkaProducerClientServiceConfiguration.java` (interface getter) + `DefaultKafkaProducerClientServiceConfiguration.java` (field/Builder/getter/equals/hashCode/toString).
- Modify: `consumer/configuration/KafkaConsumerClientServiceConfiguration.java` + `DefaultKafkaConsumerClientServiceConfiguration.java` (same).
- Test: `producer/configuration/DefaultKafkaProducerClientServiceConfigurationTest.java` + `consumer/.../DefaultKafkaConsumerClientServiceConfigurationTest.java`.

**Interfaces:**
- Produces (both configs): `ReactiveOperationConfiguration getReactiveOperationConfiguration()` returning a non-null `DefaultReactiveOperationConfiguration` (default `DefaultReactiveOperationConfiguration.builder().build()`), mirroring `DefaultCassandraClientServiceConfiguration`'s field exactly (concrete type for Jackson round-trip; `DEFAULT_REACTIVE_OPERATION_CONFIGURATION` constant; Builder default).

- [ ] **Step 1: Failing tests** — assert each config's `getReactiveOperationConfiguration()` defaults non-null with `metricsEnabled()==true`, and a YAML round-trip preserves an overridden `metricsEnabled(false)`. Mirror Cassandra's config test.
- [ ] **Step 2: Run — FAIL** (getter missing). Build cmd with `-Dtest=DefaultKafkaProducerClientServiceConfigurationTest,DefaultKafkaConsumerClientServiceConfigurationTest`.
- [ ] **Step 3: Add the field to both configs** — mirror `DefaultCassandraClientServiceConfiguration` (field, `DEFAULT_REACTIVE_OPERATION_CONFIGURATION`, ctor from builder, getter on interface + impl, Builder setter defaulting to the constant, equals/hashCode/toString). **Confirm** `Builder.getThis()` returns `this` (guard against the null-getThis bug found in Cassandra).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** build — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): reactive operation config on Kafka producer + consumer client configs`). STOP.

---

## Task 3: Core `fluxStream` lighter spine + `KafkaAccessorException` + config→Properties extraction

**Branch:** `feature/P-027-R2a-kafka-t3-foundation`
**Files:**
- Modify: `appbase-client-core/.../client/core/reactive/AbstractReactiveAccessor.java` — add `fluxStream`.
- Create: `.../service/kafka/KafkaAccessorException.java`.
- Modify: `producer/DefaultKafkaProducerClientIntegrationService.java` + `consumer/DefaultKafkaConsumerClientIntegrationService.java` — extract `buildProducerProperties()`/`buildConsumerProperties()`.
- Test: `appbase-client-core/.../core/reactive/AbstractReactiveAccessorTest.java` (fluxStream); `.../service/kafka/KafkaAccessorExceptionTest.java`.

**Interfaces:**
- Produces:
  - `protected <T> Flux<T> fluxStream(String op, Supplier<Flux<T>> source)` on `AbstractReactiveAccessor` — applies ONLY: `subscribeOn(scheduler)`, `onErrorMap(this::mapError)`, `doOnError(t -> { logFailure(op,t); reportNotable(op, mapError(t)); })`. NO timeout/retry/backpressure-buffer/per-op timer/span. Javadoc states WHY (§4 of the spec: idle-kill/rebalance/span-leak/double-backpressure). Per-record metrics are added by the consumer accessor via `doOnNext`, not here.
  - `public class KafkaAccessorException extends AppbaseClientException` with ctors `(Throwable)` / `(String, Throwable)` and `public static Throwable map(Throwable t, Function<Throwable,Throwable> fallback)` mapping `org.apache.kafka.common.KafkaException` → `new KafkaAccessorException(t)`, else `fallback.apply(t)`. Mirror `CassandraAccessorException`.
  - `Properties buildProducerProperties()` / `Properties buildConsumerProperties()` (package-private on the respective integration services) — the exact `Properties` the existing `initImpl` builds, with `initImpl` refactored to call it (no behavior change).

- [ ] **Step 1: Failing test — `fluxStream`.** Assert a `fluxStream("op", () -> Flux.error(new KafkaException("x")))` surfaces a mapped `KafkaAccessorException` (via a test subclass whose `mapError` delegates to `KafkaAccessorException.map`), and that a success `Flux.just(...)` passes through unaltered (no timeout applied — a delayed element beyond any R0 default still completes).
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** `fluxStream` (stub signature above), `KafkaAccessorException`, and extract `buildProducerProperties()`/`buildConsumerProperties()` (move the inline `Properties` block; `initImpl` now calls it). Keep sync `initImpl` behavior identical.
- [ ] **Step 4: Run — PASS** (fluxStream test + KafkaAccessorException map test + existing producer/consumer config/integration tests still green — proves the extraction is behavior-preserving).
- [ ] **Step 5: Whole-module** (core + kafka) — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): fluxStream lighter spine + KafkaAccessorException + shared Kafka property builders`). STOP.

---

## Task 4: Reactive producer accessor

**Branch:** `feature/P-027-R2a-kafka-t4-reactive-producer`
**Files:**
- Create: `producer/reactive/ReactiveKafkaProducerAccessor.java`.
- Modify: `appbase-client-kafka/pom.xml` — add `reactor-test` (test, version-less), Export-Package `...service.kafka.producer.reactive`.
- Test: `producer/reactive/ReactiveKafkaProducerAccessorTest.java`.

**Interfaces:**
- Consumes: `AbstractReactiveAccessor` (`mono`/`flux`), `KafkaAccessorException.map`, `buildProducerProperties()`.
- Produces:
  - `public class ReactiveKafkaProducerAccessor<K,V> extends AbstractReactiveAccessor`
  - ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, KafkaSender<K,V> sender)` — sender injected (built by the service/acquisition; injectable for tests). Calls `super(appbaseCore, reactorService, configuration)`.
  - `public <T> Flux<SenderResult<T>> send(String op, Publisher<SenderRecord<K,V,T>> records)` — wraps `sender.send(records)` in `flux(op, () -> ...)` (full spine).
  - `public Mono<RecordMetadata> send(String op, ProducerRecord<K,V> record)` — single-send convenience via `mono(op, ...)` mapping the `SenderResult` to its `RecordMetadata` (or error).
  - `public KafkaSender<K,V> sender()` — escape hatch.
  - `@Override protected Throwable mapError(Throwable t)` = `KafkaAccessorException.map(t, super::mapError)`; `@Override protected boolean isNotable(Throwable m)` = `true`.

- [ ] **Step 1: Failing test** — mock `KafkaSender`; stub `send(...)` → a `Flux.just(mockSenderResult)`; assert `accessor.send("send", record)` emits the `RecordMetadata` (StepVerifier), a `KafkaException` thrown by the sender maps to `KafkaAccessorException`, and a metrics `Timer` is recorded (mock `MetricService` like `RedisAccessorTest`/reactive). Non-gated.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** the accessor (stubs above).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): ReactiveKafkaProducerAccessor on shared spine`). STOP.

---

## Task 5: Sync producer accessor + producer acquisition

**Branch:** `feature/P-027-R2a-kafka-t5-sync-producer`
**Files:**
- Create: `producer/KafkaProducerAccessor.java`.
- Modify: `producer/KafkaProducerClientService.java` (interface) + `producer/DefaultKafkaProducerClientService.java` (impl) — `getReactiveAccessor()`/`getAccessor()` (+ keyed).
- Test: `producer/KafkaProducerAccessorTest.java`; add acquisition tests to a `producer/DefaultKafkaProducerClientServiceReactiveTest.java`.

**Interfaces:**
- Consumes: `AbstractAppbaseAccessor` (`execute`), `KafkaAccessorException.map`, pooled `Producer` via `getClientIntegrationService`.
- Produces:
  - `public class KafkaProducerAccessor<K,V> extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<Producer<K,V>, ClusterIdValue>> borrow)` mirroring `RedisAccessor`.
  - `public RecordMetadata send(String op, ProducerRecord<K,V> record) throws Exception` — via base `execute`, borrowing the pooled service, `producer.send(record).get()`, `finalizeOperation()` in `finally`.
  - `public <T> T command(String op, Function<Producer<K,V>, T> fn) throws Exception`.
  - `mapError` = `KafkaAccessorException.map(t, super::mapError)`; `isNotable` = true.
  - On the producer service: `getReactiveAccessor()` returns `ReactiveKafkaProducerAccessor` built with a `KafkaSender` from `SenderOptions.create(getClientIntegrationService().buildProducerProperties())` (or the service's own props accessor); `getAccessor()` returns `KafkaProducerAccessor` with the pooled borrow. Mirror `DefaultCassandraClientService.getReactiveAccessor`/`getAccessor`. **KafkaSender lifecycle:** created per `getReactiveAccessor()` call OR cached on the service and closed on `shutdown()` — cache-and-close-on-shutdown to avoid per-call sender churn; document the leak-trap rationale.
- Produces (for Task 8): the `KafkaSender` build site + service shutdown hook where the `KafkaClientMetrics` binder will attach.

- [ ] **Step 1: Failing tests** — `KafkaProducerAccessorTest`: mock integration service → pooled `Producer`; assert `send` returns the `RecordMetadata`, pool returned on success+error (`finalizeOperation` ×2), `KafkaException`→`KafkaAccessorException`, `Timer` recorded (mirror `CassandraAccessorTest`). Acquisition test: non-gated, `autoStart(false)`, `getReactiveAccessor()`/`getAccessor()` non-null + reactive-config passthrough (mirror `DefaultCassandraClientServiceReactiveTest`).
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** accessor + acquisition (stubs above). Confirm `KafkaSender.create(SenderOptions.create(props))` signature against the jar.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): sync KafkaProducerAccessor + producer accessor acquisition`). STOP.

---

## Task 6: Reactive consumer accessor (lighter spine)

**Branch:** `feature/P-027-R2a-kafka-t6-reactive-consumer`
**Files:**
- Create: `consumer/reactive/ReactiveKafkaConsumerAccessor.java`.
- Modify: `appbase-client-kafka/pom.xml` — Export-Package `...service.kafka.consumer.reactive`.
- Test: `consumer/reactive/ReactiveKafkaConsumerAccessorTest.java`.

**Interfaces:**
- Consumes: `AbstractReactiveAccessor.fluxStream`, `KafkaAccessorException.map`, `buildConsumerProperties()`.
- Produces:
  - `public class ReactiveKafkaConsumerAccessor<K,V> extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, KafkaReceiver<K,V> receiver)`.
  - `public Flux<ReceiverRecord<K,V>> receive(String op)` — `fluxStream(op, receiver::receive)` PLUS a `doOnNext` incrementing a per-record `Counter` (`operationErrorCounter`-style helper, but a success/received counter named per op). Javadoc: uses the LIGHTER spine — states why timeout/retry/backpressure/per-op timer/span are omitted (§4).
  - `public KafkaReceiver<K,V> receiver()` — escape hatch.
  - `mapError` = `KafkaAccessorException.map(t, super::mapError)`; `isNotable` = true.

- [ ] **Step 1: Failing test** — mock `KafkaReceiver`; stub `receive()` → `Flux.just(mockReceiverRecord)`; assert `accessor.receive("receive")` emits it (StepVerifier), a `KafkaException` on the stream maps to `KafkaAccessorException`, a per-record `Counter` is incremented, and NO timeout is applied (a `Flux` that delays beyond an R0 default still completes). Non-gated.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** the accessor (stubs above).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): ReactiveKafkaConsumerAccessor on lighter fluxStream spine`). STOP.

---

## Task 7: Sync consumer accessor + consumer acquisition

**Branch:** `feature/P-027-R2a-kafka-t7-sync-consumer`
**Files:**
- Create: `consumer/KafkaConsumerAccessor.java`.
- Modify: `consumer/KafkaConsumerClientService.java` + `consumer/DefaultKafkaConsumerClientService.java` — `getReactiveAccessor()`/`getAccessor()` (+ keyed).
- Test: `consumer/KafkaConsumerAccessorTest.java`; `consumer/DefaultKafkaConsumerClientServiceReactiveTest.java` (acquisition).

**Interfaces:**
- Consumes: `AbstractAppbaseAccessor.execute`, `KafkaAccessorException.map`, pooled `Consumer` via `getClientIntegrationService`, `buildConsumerProperties()`.
- Produces:
  - `public class KafkaConsumerAccessor<K,V> extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<Consumer<K,V>, ClusterIdValue>> borrow)`.
  - `public ConsumerRecords<K,V> poll(String op, Duration timeout) throws Exception` — bounded op, full spine via base `execute`, borrows pooled `Consumer`, `consumer.poll(timeout)`, `finalizeOperation()` in `finally`.
  - `public <T> T command(String op, Function<Consumer<K,V>, T> fn) throws Exception`.
  - `mapError`/`isNotable` as elsewhere.
  - On the consumer service: `getReactiveAccessor()` returns `ReactiveKafkaConsumerAccessor` with a `KafkaReceiver` from `ReceiverOptions.create(buildConsumerProperties()).subscription(topics)` (topics from consumer config); `getAccessor()` returns `KafkaConsumerAccessor` (pooled borrow). Mirror producer Task 5. `KafkaReceiver` lifecycle: cache-and-close-on-shutdown; document.
- Produces (for Task 8): the `KafkaReceiver` build site + shutdown hook for consumer `KafkaClientMetrics`.

- [ ] **Step 1: Failing tests** — `KafkaConsumerAccessorTest`: mock integration service → pooled `Consumer`; assert `poll` returns the `ConsumerRecords`, pool returned on success+error, `KafkaException`→`KafkaAccessorException`, `Timer` recorded. Acquisition test non-gated (autoStart=false), `getReactiveAccessor()`/`getAccessor()` non-null + config passthrough.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** (stubs above). Confirm `KafkaReceiver.create(ReceiverOptions...)` + `ReceiverOptions.subscription(Collection<String>)` signatures against the jar.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module** — no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): sync KafkaConsumerAccessor + consumer accessor acquisition`). STOP.

---

## Task 8: Driver-native metrics (pooled + reactor-kafka), config-gated

**Branch:** `feature/P-027-R2a-kafka-t8-driver-metrics`
**Files:**
- Modify: `producer/configuration/KafkaProducerClientIntegrationServiceConfiguration.java` + `DefaultKafkaProducerClientIntegrationServiceConfiguration.java` — `driverMetricsEnabled` (default true).
- Modify: `consumer/configuration/...IntegrationServiceConfiguration.java` + `Default...` — same.
- Modify: `DefaultKafkaProducerClientIntegrationService.java` + `DefaultKafkaConsumerClientIntegrationService.java` — bind pooled-client `KafkaClientMetrics` at build (gated), close on shutdown.
- Modify: `producer/reactive/ReactiveKafkaProducerAccessor.java` + `consumer/reactive/ReactiveKafkaConsumerAccessor.java` — bind reactor-kafka `KafkaClientMetrics` via `doOnProducer`/`doOnConsumer` (gated), close on accessor/stream close.
- Modify module `pom.xml` if `micrometer-core` is not already a direct dependency (it's transitively present via appbase-core; add explicit direct dep, version-less).
- Test: `producer/DefaultKafkaProducerClientIntegrationServiceMetricsTest.java` + consumer analog + config tests.

**Interfaces:**
- Consumes: `MetricService.getCompositeMeterRegistry()`, `KafkaClientMetrics(Producer)`/`(Consumer)`, `KafkaSender.doOnProducer`, `KafkaReceiver.doOnConsumer`.
- Produces: `boolean isDriverMetricsEnabled()` (default true) on both integration configs; pooled + reactor-kafka native metrics on the composite when enabled.

- [ ] **Step 1: Config flag (RED→GREEN)** — add `driverMetricsEnabled` (primitive boolean default true) to both integration configs; test default + YAML round-trip. Mirror Cassandra Task 6 config.
- [ ] **Step 2: Pooled-client binding (RED→GREEN)** — in each integration service, when `driverMetricsEnabled`, `new KafkaClientMetrics(client).bindTo(getCompositeMeterRegistry())` at build; hold the binder; `close()` in shutdown. Seam test: mock `MetricService.getCompositeMeterRegistry()` → `SimpleMeterRegistry`; verify consulted when enabled, `never()` when disabled — extract a testable helper that stops short of a live client connect (mirror Cassandra `applyProgrammaticDriverConfig`'s testable seam).
- [ ] **Step 3: reactor-kafka producer binding** — in `ReactiveKafkaProducerAccessor`, when enabled, `sender.doOnProducer(p -> { var m = new KafkaClientMetrics(p); m.bindTo(reg); return m; }).subscribe(m -> this.producerMetrics = m)` once at init; `producerMetrics.close()` + `sender.close()` on accessor close. Document: producer proxy whitelists `metrics()`; lazy producer creation.
- [ ] **Step 4: reactor-kafka consumer binding** — in `ReactiveKafkaConsumerAccessor.receive`, bind lazily on subscribe/first-record via `receiver.doOnConsumer(...)`, `close()` on stream terminate; re-bind on re-subscription. Document the `doOnConsumer`-needs-active-subscription `IllegalStateException` constraint + the too-early race (bind on first emitted record).
- [ ] **Step 5: Run — PASS.** Whole-module — no regression. Unit-test the reactor-kafka bindings via mocks (`KafkaSender.doOnProducer` stubbed to invoke the function with a mock `Producer` whose `metrics()` returns an empty map; verify the binder is created + registry bound; consumer analog). Do NOT require a live broker.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): route Kafka driver-native metrics (pooled + reactor-kafka) into composite/JMX, config-gated`). STOP.

---

## Task 9: Opt-in ITs + build-guide docs

**Branch:** `feature/P-027-R2a-kafka-t9-it-docs`
**Files:**
- Create: `producer/reactive/ReactiveKafkaProducerAccessorIT.java` + `consumer/reactive/ReactiveKafkaConsumerAccessorIT.java` (gated).
- Modify: `appbase-java/build-guide.md`.

**Interfaces:** none new (exercises Tasks 4–8 end-to-end).

- [ ] **Step 1: Write gated ITs** — READ the existing Kafka test helpers (`KafkaProducerClientServiceTestHelper`/`KafkaConsumerClientServiceTestHelper`) + gate property (`KafkaClientServiceTests.enabled` — confirm the exact name). Producer IT: `getReactiveAccessor().send(...)` a record. Consumer IT: `getReactiveAccessor().receive(...)` (or sync `poll`) and StepVerifier the round-tripped record. Use the Docker scripts in `infra/docker/kafka/`. Mirror `ReactiveCassandraAccessorIT` gating/`@BeforeAll`/`@AfterAll`.
- [ ] **Step 2: Run gated OFF** — with the flag unset → `Tests run: N, Skipped: N`, BUILD SUCCESS. Note any live-broker bootstrap gap (J-018) — don't fix here.
- [ ] **Step 3: Docs** — add `### Reactive Kafka (P-027 R2a)` to `build-guide.md` after the Cassandra section: producer `send` (reactive Flux/Mono + sync `RecordMetadata`), consumer `receive` (reactive Flux, lighter-spine note) + sync `poll`, escape hatches (`sender()`/`receiver()`), shared-spine cross-cutting (metrics→JMX, trace, log, `KafkaException`→`KafkaAccessorException`), the lighter-spine rationale for `receive`, driver-native metrics (both faces, config-gated `driverMetricsEnabled`), and the `reactiveOperationConfiguration` knobs. Verify: `grep -n "Reactive Kafka" appbase-java/build-guide.md`.
- [ ] **Step 4: Track gated ITs in J-018** — add a clause naming `ReactiveKafkaProducerAccessorIT`/`...ConsumerAccessorIT` (gated by `-DKafkaClientServiceTests.enabled=true`, need live-Kafka bootstrap) to the J-018 row in `Claude-Review.md` (on develop). Per standing policy.
- [ ] **Step 5: Commit + PR** (`test(P-027-R2a): opt-in reactive Kafka ITs + build-guide docs`). STOP.

---

## Post-twin (after Task 9 merges)
- Add reactive Kafka twin learnings to `appbase-java/LEARNING.md` (reactor-kafka owns-its-client; `doOnProducer`/`doOnConsumer` metrics binding + consumer active-subscription constraint; `fluxStream` lighter spine; kafka-clients bump outcome).
- Continue remaining R2a twins one-by-one, then R1.

## Self-Review (plan author)
- **Spec coverage:** Decision A → Tasks 4/6 (reactor-kafka own client) + 5/7 (built from shared props via Task 3 extraction); B → Task 3 `fluxStream` + Task 6 use; C → Tasks 4–7 (4 accessors); D → Task 8 (both faces); E → every task (code-doc constraint). Dep gate → Task 1 (incl. kafka-clients bump). Config → Task 2. ITs+docs → Task 9. J-018 tracking → Task 9 Step 4.
- **Type consistency:** `getReactiveAccessor()`/`getAccessor()`, `send`, `receive`, `poll`, `command`, `sender()`/`receiver()`, `KafkaAccessorException.map`, `fluxStream`, `buildProducerProperties`/`buildConsumerProperties`, `isDriverMetricsEnabled`, `getReactiveOperationConfiguration` — consistent across tasks.
- **Placeholders:** none — each task carries signatures + test intent + exact build/commit; full method bodies intentionally left to the implementer per the project's plan-stub convention (implementer mirrors the named Redis/Cassandra reference files).
