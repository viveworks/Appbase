# P-027 R2a — Reactive Kafka Twin — Design

**Status:** approved (design phase)
**Date:** 2026-07-05
**Author:** Navaneeth Mysore G · Claude
**Parent:** P-027 (Reactive / Non-Blocking Appbase), R2a driver twins. Follows the merged Redis (internal work item 280–internal work item 286) and Cassandra (internal work item 288–internal work item 294) twins.
**Module:** `appbase-client/appbase-client-kafka`

---

## 1. Goal

Add reactive **and** synchronous accessor faces to Appbase's Kafka producer and consumer client services, built on **reactor-kafka**, reusing the shared `AbstractAppbaseAccessor`/`AbstractReactiveAccessor` cross-cutting spine (metrics→JMX, tracing, exception-store, error mapping) established by the Redis/Cassandra twins. Route Kafka's driver-native metrics into the Appbase composite/JMX for both the pooled sync clients and the reactor-kafka clients. Everything config-gated with sensible defaults. One PR per task.

## 2. Why Kafka differs from Redis/Cassandra (and the decisions it forced)

Kafka's shape is materially different from the prior twins; these differences drove the design decisions below.

- **Two services, generic `<K,V>`.** `KafkaProducerClientService<K,V>` (client = `org.apache.kafka.clients.producer.Producer<K,V>`) and `KafkaConsumerClientService<K,V>` (client = `Consumer<K,V>`). Accessors are generic.
- **No accessor layer exists today.** Callers use the raw pooled `Producer`/`Consumer`. This twin introduces the accessor layer for both faces.
- **reactor-kafka owns its own client** *(Decision A)*. `KafkaSender.create(SenderOptions)` / `KafkaReceiver.create(ReceiverOptions)` build and manage their **own** producer/consumer from config properties — they cannot wrap the existing pooled `Producer`/`Consumer`. So the reactive path is a **separate, independently-managed** client built from the *same* config properties. The pooled sync client is untouched.
- **Two operation shapes with different spine fit** *(Decision B)*. Producer `send` is a discrete bounded op → full R0 spine. Consumer `receive` is a long-lived infinite subscription → the per-op R0 operators are wrong for it (see §4). It gets a **lighter** spine.
- **Kafka clients are thread-safe long-lived singletons** — the per-op borrow/return pool model of Redis/Cassandra does not apply to the reactive path (the sender/receiver is one long-lived instance). The sync accessors still borrow the pooled client per op.

## 3. Scope (decisions)

- **Decision A — reactor-kafka owns its own client**, built from the same integration-service config properties.
- **Decision B — full spine for `send`, lighter spine for `receive`.**
- **Decision C — reactive + sync, both faces.** Four accessors total (reactive producer, sync producer, reactive consumer, sync consumer).
- **Decision D — driver-native metrics for BOTH the pooled sync client and the reactor-kafka client**, config-gated, into the composite/JMX.
- **Decision E — document every non-obvious decision at the code site** (Javadoc/inline), not only in this spec.
- **No entity/serialization abstraction** beyond what the existing services already configure (key/value serializers stay config-driven).

## 4. Spine fit — why `receive` is lighter (documented rationale)

The R0 chain — `source → timeout → retryWhen → onErrorMap → doOnError → onBackpressureBuffer → doFinally(timer.stop + finishSpan) → subscribeOn` — encodes a *single bounded operation*. `send` satisfies it. `receive` (an infinite `Flux<ReceiverRecord>`) breaks each per-op operator:

- **timeout** — errors when no element arrives within the window; a healthy consumer on an idle topic legitimately emits nothing for long stretches → would kill healthy consumers.
- **retryWhen** — retry resubscribes the source; resubscribing a `KafkaReceiver` tears down the consumer, rejoins the group, and triggers a **rebalance** (heavy, stateful) — not a cheap idempotent replay.
- **per-op Timer / span** — measure one op's start→finish; an infinite stream never finishes → a Timer is meaningless and a span never closes (leak). The meaningful consumer signal is **per-record** throughput.
- **onBackpressureBuffer** — `KafkaReceiver` already backpressures (pauses partitions when downstream is slow); an extra buffer defeats that and risks OOM.

**Lighter spine for `receive`** applies only the stream-appropriate cross-cutting: scheduler dispatch (`subscribeOn`/`publishOn`), error **mapping** (`KafkaException` → `KafkaAccessorException`), `doOnError` logging, exception-store report on stream error, and per-**record** counter metrics. It skips timeout/retry/backpressure-buffer/per-op timer/span. Implemented as a new protected `AbstractReactiveAccessor#fluxStream(op, Supplier)` in `appbase-client-core`, documented with this rationale.

## 5. Components

### 5.1 Shared (appbase-client-kafka + core)
- **`KafkaAccessorException extends AppbaseClientException`** — static `map(Throwable, Function<Throwable,Throwable> fallback)` mapping `org.apache.kafka.common.KafkaException` (checked; used by all four faces).
- **`AbstractReactiveAccessor#fluxStream(op, Supplier<Flux<T>>)`** (core) — the lighter-spine path for long-lived streams (§4).
- **Config→Properties extraction** — refactor the inline `Properties` building in each integration service's `initImpl` (producer `initImpl` ~L135–472, consumer ~L135–518) into reusable `buildProducerProperties()`/`buildConsumerProperties()`. The sync client build and the reactive `SenderOptions`/`ReceiverOptions` build from **one** source of truth (DRY). No behavior change to the sync path.

### 5.2 Producer face
- **`ReactiveKafkaProducerAccessor<K,V> extends AbstractReactiveAccessor`** — owns a `KafkaSender<K,V>` built lazily from `SenderOptions.create(buildProducerProperties())`.
  - `Flux<SenderResult<T>> send(String op, Publisher<SenderRecord<K,V,T>> records)` — full spine.
  - `Mono<RecordMetadata> send(String op, ProducerRecord<K,V> record)` — convenience single-send, full spine.
  - `KafkaSender<K,V> sender()` — escape hatch.
- **`KafkaProducerAccessor<K,V> extends AbstractAppbaseAccessor`** (sync) — borrows the pooled `Producer` per op.
  - `RecordMetadata send(String op, ProducerRecord<K,V> record) throws Exception` — via base `execute` (wraps `producer.send(record).get()`).
  - `<T> T command(String op, Function<Producer<K,V>, T> fn) throws Exception`.

### 5.3 Consumer face
- **`ReactiveKafkaConsumerAccessor<K,V> extends AbstractReactiveAccessor`** — owns a `KafkaReceiver<K,V>` built from `ReceiverOptions.create(buildConsumerProperties()).subscription(topics)`.
  - `Flux<ReceiverRecord<K,V>> receive(String op)` — **lighter** spine (§4).
  - `KafkaReceiver<K,V> receiver()` — escape hatch.
- **`KafkaConsumerAccessor<K,V> extends AbstractAppbaseAccessor`** (sync) — borrows the pooled `Consumer` per op.
  - `ConsumerRecords<K,V> poll(String op, Duration timeout) throws Exception` — a **bounded** op, so it takes the full spine.
  - `<T> T command(String op, Function<Consumer<K,V>, T> fn) throws Exception`.

### 5.4 Acquisition
- `KafkaProducerClientService`: `getReactiveAccessor()` / `getAccessor()` (+ keyed).
- `KafkaConsumerClientService`: `getReactiveAccessor()` / `getAccessor()` (+ keyed).
- Built with `appbaseClientCore.getAppbaseCore()`, `.getReactorService()`, `getConfiguration().getReactiveOperationConfiguration()`, borrow = the `getClientIntegrationService` method ref (sync) / config-props (reactive sender/receiver).

## 6. Configuration
- `reactiveOperationConfiguration` block added to **both** `DefaultKafkaProducerClientServiceConfiguration` and `DefaultKafkaConsumerClientServiceConfiguration` (mirror Redis/Cassandra Task 2; same R0 knobs, sensible defaults).
- `driverMetricsEnabled` (default `true`) on both `DefaultKafkaProducerClientIntegrationServiceConfiguration` and `DefaultKafkaConsumerClientIntegrationServiceConfiguration` (gates §7).

## 7. Driver-native metrics (Decision D) — both faces

Bind Kafka's native client metrics to the shared composite via Micrometer `KafkaClientMetrics` (micrometer-core 1.13.1, already on classpath; `KafkaClientMetrics(Producer)`/`(Consumer)`, a `MeterBinder` + `AutoCloseable`). Registry = `MetricService.getCompositeMeterRegistry()` (→ JMX). Owned by `MetricService`; never shut down by the accessor. Config-gated by `driverMetricsEnabled`.

- **Pooled sync client** — bind at client build (we hold the `Producer`/`Consumer` handle); `close()` the binder on service shutdown.
- **reactor-kafka client** — confirmed feasible (jar-verified):
  - **Producer:** `sender.doOnProducer(p -> { var m = new KafkaClientMetrics(p); m.bindTo(reg); return m; }).subscribe(...)` once at accessor init. `doOnProducer` lazily creates the producer; the reactor-kafka producer proxy whitelists `metrics()`, which is all `KafkaClientMetrics` polls. `close()` on shutdown.
  - **Consumer:** `KafkaReceiver.doOnConsumer(...)` passes the real consumer BUT throws `IllegalStateException` if called before a `receive*()` is subscribed (the consumer only exists during an active subscription). So bind **lazily on subscribe/first record** of the `receive` stream, `close()` on stream terminate, and re-bind on re-subscription. This lifecycle is the bulk of the metrics-task effort and is documented in code.

**Lifecycle gotchas (documented in code):** `KafkaClientMetrics` polls `client.metrics()` lazily → the client must stay alive; always `close()` the binder on client/stream close to drop stale meters; producer meters appear only after the lazy producer is created; consumer meters require a live receive subscription.

## 8. Lifecycle
`KafkaSender`/`KafkaReceiver` own Kafka network threads (a leak trap, like Lettuce `ClientResources`): built lazily, **closed on service/accessor shutdown**, with the `KafkaClientMetrics` binders closed alongside. Sync accessors add no lifecycle (pooled client). Documented at the shutdown site.

## 9. Dependencies (gate)
- New: **`io.projectreactor.kafka:reactor-kafka:1.3.25`** — managed in root `<dependencyManagement>`; version-less in the module pom. (Reactor-core 3.4.41 is its transitive baseline, overridden by the repo's `${reactor.version}=3.6.8` BOM pin.)
- **kafka-clients alignment (bump preferred):** reactor-kafka 1.3.25 pulls `kafka-clients:3.9.1` transitively; the repo pins `kafka-clients:3.7.1` in root depMgmt. **Preferred: bump `kafka-clients` 3.7.1 → 3.9.1** (aligns with reactor-kafka's baseline) *if it causes no issues* — Task 1 must regression-check every Kafka consumer of the pin (producer/consumer client modules, `appbase-stacktest`, any Kafka usage) and the full build before bumping. If 3.9.1 surfaces a break, fall back to keeping 3.7.1 (depMgmt overrides the transitive 3.9.1) and record why. Overlaps P-032 dependency hygiene. Decision recorded in the plan.
- micrometer-core 1.13.1 (`KafkaClientMetrics`) — already present.

## 10. Testing
- **Non-gated unit tests** (Mockito): producer `send` (mock `KafkaSender`/pooled `Producer`), sync `poll`, spine cross-cutting (metrics/pool-return/error-map) mirroring `RedisAccessorTest`/`CassandraAccessorTest`; acquisition tests (autoStart=false, no live broker); a metrics-wiring seam test (verify `getCompositeMeterRegistry()` consulted when `driverMetricsEnabled`, `never()` when off).
- **Gated ITs** (`-DKafkaClientServiceTests.enabled=true`): producer `send` → consumer `receive`/`poll` round-trip against a live broker. Gated + skipped by default → **tracked in J-018** (needs a live-Kafka bootstrap; per the standing skipped-tests policy).
- Build note: use the `install` goal for `-am` reactive-module builds (bnd Export-Package on `.reactive` fails under the `test` goal — same as Redis/Cassandra).

## 11. Decision documentation (Decision E)
Every non-obvious choice gets a Javadoc/inline rationale at the code site: why `receive` uses `fluxStream` (idle-kill / rebalance / span-leak / double-backpressure), why reactor-kafka owns its client, the sender/receiver shutdown leak trap, the `doOnConsumer`-requires-active-subscription constraint, the driver-metrics binding lifecycle, and any name chosen to avoid a base-method collision.

## 12. Task breakdown (one PR per task — finalized in the plan)
1. **reactor-kafka dependency + kafka-clients bump** (add `reactor-kafka:1.3.25` to root depMgmt + module pom; bump `kafka-clients` 3.7.1→3.9.1 if the full build + all Kafka consumers pass, else keep 3.7.1 via depMgmt override and record why). Prerequisite PR.
2. **`reactiveOperationConfiguration`** on both producer + consumer client-service configs.
3. **Core `fluxStream` lighter-spine** + **`KafkaAccessorException`** + **config→Properties extraction** (foundation).
4. **Reactive producer accessor** (`ReactiveKafkaProducerAccessor` + `KafkaSender` lifecycle) + send.
5. **Sync producer accessor** + producer acquisition (`getReactiveAccessor`/`getAccessor`).
6. **Reactive consumer accessor** (`ReactiveKafkaConsumerAccessor` + `KafkaReceiver`, lighter spine) + receive.
7. **Sync consumer accessor** (`poll`) + consumer acquisition.
8. **Driver-native metrics** — pooled clients + reactor-kafka (`doOnProducer`/`doOnConsumer`), config-gated, incl. the consumer-lifecycle binding.
9. **Opt-in ITs + build-guide docs** (`### Reactive Kafka (P-027 R2a)`).

## 13. Spec coverage self-check
Decisions A–E each map to components/tasks: A→§2/§5.2/§5.3/Task 4,6; B→§4/Task 3; C→§5.2/§5.3/Tasks 4–7; D→§7/Task 8; E→§11 (all tasks). Dependencies gated in §9/Task 1. Testing §10. No entity layer. No unresolved TBDs.
