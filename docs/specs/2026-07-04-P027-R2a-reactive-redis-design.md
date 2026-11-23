# P-027 R2a — Reactive Redis twin + reactive-accessor cross-cutting integration (design)

**Status:** Design / brainstorm output for review. Sub-project of the P-027 umbrella spec
(`2026-07-04-P027-reactive-appbase-design.md`), building on the merged R0 reactive core
foundation. Next artifact after approval: a per-task implementation plan.

**Goal:** deliver the first true non-blocking client twin — reactive Redis on Lettuce — *and*
promote the R0 `AbstractReactiveAccessor` into a properly Appbase-integrated accessor base
(metrics, tracing, logging, exception translation, pool lifecycle), so every later twin
(Cassandra, Kafka, …) inherits the same cross-cutting behaviour.

---

## 1. What the codebase already does (investigation findings)

Verified against source (full citations in the R2a investigation notes). Summary:

**Lifecycle spine.** `AppbaseService` → `AbstractAppbaseService` (status `AtomicReference` +
lifecycle timers + mediator status/exception broadcast) → `AbstractAppbaseManagedService<C>`
(typed config + `initImpl` template + config-listener wiring) → `AbstractAppbaseClientService<T,V,C>`
(pool of `AppbaseClientIntegrationService`s). `DefaultRedisClientService extends
AbstractAppbaseClientService<StatefulRedisConnection<String,String>, TextValue, …>`.
`AbstractAppbaseClientIntegrationService` is **not** a managed service — a minimal
`init`/`shutdown`/`repair`/`isHealthy` lifecycle over one native connection.

**Accessors are plain POJOs.** `AbstractSearchIndexAccessor`, `AbstractObjectStoreAccessor`,
`AbstractAerospikeAccessor` implement **no lifecycle interface**. They take `AppbaseCore` + a
driver handle in the constructor and pull their own `Timer`s from `appbaseCore.getMetricService()`.
This is the exemplar the reactive accessor should follow — **not** `AbstractAppbaseManagedService`.

**Metrics idiom.** One MicroProfile `Timer` per operation, created in the constructor:
`metricRegistry.timer(metricService.name(getClass(), "<op>Timer"))`. Sync ops:
`Timer.Context c = t.time(); try { … } finally { c.stop(); }`. Async ops (ES/S3): start the
context, then `future.whenComplete((r,e) -> c.stop())` on the **returned** future so it fires once.
Druid shows the alternate error-`Counter` idiom.

**Sync Redis has NO per-operation metrics** — `DefaultRedisClientService`/`…IntegrationService`/
`RedisClientUtil` only inherit lifecycle timers; `getClient()` returns the raw connection and
callers issue Lettuce commands entirely outside Appbase instrumentation.

**Logging.** `appbaseCore.getLogService().getLogger(getClass())`; DEBUG for request/entry, INFO
for milestones, WARN for soft anomalies, ERROR (throwable last arg) for failures.

**Exceptions.** Op idiom = catch native driver throwable, translate to an
`AppbaseClientException` subtype (e.g. `AerospikeAccessorException(ae)`), rethrow. **No** op writes
to `ExceptionService` (that store is fed automatically by the mediator from *lifecycle* failures
only). No generic per-driver mapper exists except R0's `ReactiveErrorMapper`. No Redis mapper yet.

**Tracing.** `AppbaseCore.getTracerService().getTracer()` (OpenTracing). **No client op creates
spans today** — a reactive accessor would be the first. Template = `AbstractAppbaseHandler`:
`tracer.buildSpan(name).asChildOf(parent).start()` … `span.finish()` on complete/error. Web trace
filters only propagate a trace-id via SLF4J MDC (not spans).

**Pool borrow/return (critical).** A client op does not touch a raw connection — it borrows an
`AppbaseClientIntegrationService<T,V>` from the `AppbaseClientPool` and **must** return it via
`finalizeOperation()`, honouring `repair()`/`isHealthy()`.

**`AppbaseCore` service getters** (the full menu an accessor can reach): `getLogService`,
`getMetricService`, `getValidationService`, `getMediatorService`, `getConfigurationService`,
`getServiceRegistry`, `getExceptionService`, `getTracerService`, `getReactorService`,
`getJmxService`, `getLuceneService`, `getGuiceInjectorService`.

---

## 2. Key decisions

1. **The reactive accessor is a plain POJO, not a managed service.** It does not extend
   `AbstractAppbaseManagedService`/`AbstractAppbaseClientService`. Lifecycle stays in
   `DefaultRedisClientService` (already managed); the accessor is produced/owned by it and reaches
   cross-cutting services through `AppbaseCore` — exactly like `AbstractSearchIndexAccessor`.

2. **Cross-cutting behaviour lives in the R0 base, not in Redis.** Amend `AbstractReactiveAccessor`
   so its `mono`/`flux` helpers uniformly apply, around every operation: per-op **metrics**,
   optional **trace span**, **error logging**, and **pluggable exception mapping** — on top of the
   existing scheduler/timeout/retry/backpressure/error-map. Every future reactive twin inherits it
   for free. Redis is the first consumer.

3. **Operations carry a name.** To get per-operation metrics/spans (like the exemplars’
   `deleteTimer`, not one coarse timer), the wrapped helpers take an operation name:
   `mono(String op, Supplier<Mono<T>> source)` / `flux(String op, …)`. The base lazily creates and
   caches a `Timer` per (`getClass()`,`op`) and names the span with it. The old nameless
   `mono(Supplier)`/`flux(Supplier)` remain (delegate with a default op name) so R0 stays
   backward-compatible.

4. **Driver-specific exception translation via an overridable hook.** Add
   `protected Throwable mapError(Throwable)` to the base (default = `ReactiveErrorMapper.map`).
   The Redis twin overrides it to translate Lettuce `RedisException`/`RedisCommandTimeoutException`
   into a new `RedisAccessorException extends AppbaseClientException`, then defers to super for the
   rest. Matches the per-client-subtype precedent (Aerospike).

5. **Pool borrow/return happens inside the twin’s operation supplier, per subscription.** The
   base stays driver-agnostic (it wraps whatever `Supplier<Mono/Flux>` it’s given). The Redis
   twin’s supplier uses `Mono.usingWhen`/`Flux.usingWhen` to borrow the integration service from
   the pool, run the Lettuce reactive command on `connection.reactive()`, and `finalizeOperation()`
   on terminate (success/error/cancel) — so the connection is always returned.

6. **Close the sync metrics gap with a parallel sync accessor (shared spine).** *(Locked.)* Because
   sync Redis exposes only the raw connection, per-op metrics require wrapped ops. Introduce a
   shared `AbstractAppbaseAccessor` cross-cutting base and a matching sync `RedisAccessor` that
   returns plain values, giving the sync path the same metrics/log/trace/exception parity as the
   reactive twin — true shared-spine → operation-fork at the accessor level.

---

## 3. Architecture

### 3.1 Cross-cutting spine (shared by sync + reactive)

Extract the cross-cutting concerns that are identical for sync and reactive into a small shared
base so we honour shared-spine → operation-fork *at the accessor level* too:

- `AbstractAppbaseAccessor` (new, `appbase-client-core`) — holds `AppbaseCore`, `logger`, a
  per-(class,op) `Timer` cache, a per-(class,op) error `Counter` cache, and the `TracerService`;
  provides:
  - `protected Timer operationTimer(String op)` — lazily `metricRegistry.timer(name(getClass(), op))`.
  - `protected Counter operationErrorCounter(String op)` — lazily
    `metricRegistry.counter(name(getClass(), op, "error"))`; incremented on every failure.
  - `protected Span startSpan(String op)` / span finish helpers (no-op when tracing disabled).
  - `protected Throwable mapError(Throwable)` — default `ReactiveErrorMapper.map`; overridable.
  - `protected void logFailure(String op, Throwable)` — ERROR with throwable last arg.
  - `protected void reportNotable(String op, Throwable)` — when `exceptionStoreEnabled`, builds an
    `ExceptionMessage` (`MessageFactory.exceptionMessage`) and publishes it via
    `appbaseCore.getMediatorService().getMediator().asyncHandleMessage(...)` so it lands in the
    `ExceptionStore` (async + `stackTraceHash` dedup). Called ONLY for *notable* failures — the
    mapped error’s cause is not a routine business error/timeout — never per every failure.

  **Metrics → JMX (no extra work):** `DefaultMetricService` composes a Micrometer
  `CompositeMeterRegistry` and adds a `JmxMeterRegistry` when its
  `jmxMeterRegistryConfiguration.enabled` — so every `Timer`/`Counter` recorded here is exported to
  JMX automatically. `JmxService` (a generic MBean manager) needs no accessor wiring.
- **Sync fork:** `AbstractAppbaseAccessor` gains `protected <T> T execute(String op, Callable<T>)`
  applying timer (`try/finally stop`), span, exception mapping, logging.
- **Reactive fork:** `AbstractReactiveAccessor extends AbstractAppbaseAccessor` (R0, amended) —
  its `mono(op, source)`/`flux(op, source)` apply the *same* timer/span/mapError/log, then the R0
  reactor operators (timeout/retry/backpressure/scheduler). Reactive timing is per-subscription:
  ```
  Mono.defer(() -> {
      Timer.Context c = operationTimer(op).time();
      Span span = startSpan(op);
      return source.get()
              .timeout(timeout)               // R0 operators (inside retry = per-attempt) …
              .retryWhen(...)                 // (retry only when enabled)
              .onErrorMap(this::mapError)     // native → Appbase (after retry exhaustion)
              .doOnError(e -> { operationErrorCounter(op).inc(); logFailure(op, e); reportNotable(op, e); })
              .doFinally(sig -> { finishSpan(span, sig); c.stop(); });
  }).subscribeOn(scheduler());
  ```

The shared `AbstractAppbaseAccessor` base is introduced now (both faces use it).

### 3.2 Reactive Redis twin (`appbase-client-redis`, new sub-package `…redis.reactive`)

- `ReactiveRedisAccessor extends AbstractReactiveAccessor`. Constructed from `AppbaseCore`,
  `ReactorService` (`appbaseCore.getReactorService()`), the owning `RedisClientService` (or the
  pool/key it borrows from), and the `ReactiveOperationConfiguration`. Surface:
  - `RedisReactiveCommands<String,String> reactive()` — raw escape hatch on a borrowed connection.
  - `<T> Mono<T> mono(String op, Function<RedisReactiveCommands<String,String>, Mono<T>>)` and
    `flux(String op, Function<…, Flux<T>>)` — borrow→run→return via `usingWhen`, wrapped by the base.
  - Overrides `mapError` for Lettuce → `RedisAccessorException`, and (optionally) `isNotable(cause)`
    so business/expected Redis errors are not pushed to the ExceptionStore.
- `RedisAccessorException extends AppbaseClientException` (new).

**Optional — Lettuce-native command metrics (own task).** The sync Redis integration wires no
Lettuce metrics today. `DefaultRedisClientIntegrationService.initImpl` can build `ClientResources`
with a `MicrometerCommandLatencyRecorder` bound to a Micrometer registry joined via
`metricService.registerMicrometerRegistry(...)`, giving per-command latency percentiles at the
driver level (and, through the composite, JMX). Independent of the app-level per-op `Timer`;
tracked as an optional task, not required for the R2a slice.

### 3.3 Config

Add an optional `ReactiveOperationConfiguration` field (R0) to
`DefaultRedisClientServiceConfiguration` (+ getter on the interface), YAML-configurable per client,
defaulting to R0 defaults so zero-config still works. New knobs for the cross-cutting base
(all defaulted), added to `ReactiveOperationConfiguration`:
- `metricsEnabled=true` — per-op `Timer` + error `Counter` (auto-exported to JMX via the composite).
- `traceEnabled=true` — per-op child span.
- `exceptionStoreEnabled=false` — when true, *notable* failures are reported to the `ExceptionStore`
  via the mediator. Default off to protect JPA/Lucene stores from per-op write amplification.

### 3.4 Acquisition

`RedisClientService.getReactiveAccessor()` + keyed `getReactiveAccessor(AppbaseClientKey<TextValue>)`,
mirroring `getClient()`/`getClientIntegrationService()`. Builds the thin accessor bound to the pool
+ key + injected `ReactorService` + config.

---

## 4. Testing

- **Unit** (per class, StepVerifier): mock `RedisReactiveCommands`, `ReactorService`
  (immediate scheduler), `MetricService`/`Timer`, `TracerService`; assert (a) the op runs and
  emits, (b) a `Timer` is recorded once per subscription, (c) failures are mapped to
  `RedisAccessorException` and logged, (d) the pooled integration service is returned
  (`finalizeOperation()` called) on success, error, and cancel, (e) span started+finished.
- **Integration** (opt-in via existing `REDIS_CLIENT_SERVICE_TESTS_ENABLED`, real Redis via the
  module’s docker infra): reactive `SET`→`GET` round-trip through `getReactiveAccessor()`; assert
  non-blocking round-trip and that a timer metric is registered under the accessor’s name.
- **Docs:** extend the `build-guide.md` reactive section with a Redis example + the metrics/trace
  knobs.

---

## 5. Decomposition → per-task PRs

Per the R1+ rule (one PR per task, each a branch → PR into `feature/P-027-reactive-appbase`):

1. **Shared accessor spine + base cross-cutting amendment** — introduce `AbstractAppbaseAccessor`
   (metrics `Timer` + error `Counter` caches, config-gated `startSpan`/finish, `mapError` hook,
   `logFailure`, config-gated `reportNotable` → mediator → ExceptionStore) and have
   `AbstractReactiveAccessor` extend it; add named `mono(op,…)`/`flux(op,…)` overloads wrapping
   timer + counter + span + mapError + logging + notable-report around the R0 operators; add
   `metricsEnabled`/`traceEnabled`/`exceptionStoreEnabled` knobs to `ReactiveOperationConfiguration`;
   unit tests (incl. that metrics reach the registry, counter increments on failure, and
   ExceptionStore is hit only when enabled + notable). *(No Redis yet — pure base upgrade.)*
2. **Redis config** — `ReactiveOperationConfiguration` field on `DefaultRedisClientServiceConfiguration`
   + getter + tests (config also carries `metricsEnabled`/`traceEnabled`, added to
   `ReactiveOperationConfiguration` in Task 1).
3. **`ReactiveRedisAccessor` + `RedisAccessorException`** — wrap+escape-hatch, `usingWhen` pool
   borrow/return, Lettuce error mapping; unit tests (mocked).
4. **Acquisition** — `getReactiveAccessor()` on `RedisClientService`/`DefaultRedisClientService`
   wiring `ReactorService`; unit tests.
5. **Sync `RedisAccessor`** on the shared spine (`execute(op, Callable)`) with per-op metrics/span/
   logging/exception mapping — closes the sync-metrics gap; unit tests.
6. **Integration test (opt-in) + docs.**
7. *(Optional)* **Lettuce-native command metrics** — wire `MicrometerCommandLatencyRecorder` into
   `DefaultRedisClientIntegrationService.initImpl` via `metricService.registerMicrometerRegistry`,
   for driver-level per-command latency (also surfaced in JMX through the composite). Independent of
   the app-level per-op metrics; can be deferred.

---

## 6. Resolved decisions

- **A. Sync metrics parity — LOCKED: yes.** Extract the shared `AbstractAppbaseAccessor` spine and
  ship a parallel sync `RedisAccessor` (Task 5) so both faces get per-op metrics/log/trace/exception.
- **B. Tracing scope — LOCKED: child span per op, config-gated.** Base starts a child span per
  operation (start → finish in `doFinally`, error tag on failure), toggled by `traceEnabled`
  (default on). No deep active-scope thread propagation in R2a; revisit full OpenTracing context
  propagation across the reactive hop in a later task if distributed traces need it.
- **C. Spine location — LOCKED: shared base now.** `AbstractAppbaseAccessor` is introduced in
  Task 1 and used by both the sync and reactive accessors.
- **D. JMX — no accessor work needed.** `DefaultMetricService`’s composite Micrometer registry
  auto-exports every `Timer`/`Counter` to JMX (via its `JmxMeterRegistry`, when enabled). `JmxService`
  is a generic MBean manager, unrelated. Optionally wire Lettuce-native command metrics into the
  same Micrometer composite (Task 7) for driver-level detail.
- **E. ExceptionStore — LOCKED: selective + config-gated, via mediator.** Every failure gets a cheap
  error `Counter` (→ JMX) + ERROR log always. Only *notable* failures (unexpected/unmapped, not
  routine business errors/timeouts) are reported to the `ExceptionStore`, and only when
  `exceptionStoreEnabled=true` (default off) — routed through the mediator (async + `stackTraceHash`
  dedup), never a direct per-op store write, to avoid JPA/Lucene write amplification.
