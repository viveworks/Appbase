# P-027 R2a — Reactive Redis twin + accessor cross-cutting integration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first true non-blocking client twin (reactive Redis on Lettuce) and promote the R0 `AbstractReactiveAccessor` into a fully Appbase-integrated accessor base (per-op metrics → JMX, tracing, logging, exception translation, selective exception-store, pool lifecycle) that every later twin inherits.

**Architecture:** A shared `AbstractAppbaseAccessor` cross-cutting spine (metrics/trace/log/exception) with two operation-forks — a sync `execute(op, Callable)` and the reactive `mono/flux` on `AbstractReactiveAccessor extends AbstractAppbaseAccessor`. The Redis twin holds no lifecycle: `DefaultRedisClientService` (already an `AbstractAppbaseClientService`) owns the pool; the accessor borrows a connection per operation via `Mono.usingWhen` and returns it (`finalizeOperation()`). Reactive-ness forks only the operation return type; connection/lifecycle/config stay shared.

**Tech Stack:** Java 25, Maven, Project Reactor (`reactor-core`/`reactor-test`), Lettuce (`io.lettuce:lettuce-core`, already a dep, already reactive), MicroProfile Metrics (`Timer`/`Counter` via `MetricService`, auto-exported to JMX through the Micrometer composite), OpenTracing (`TracerService`), JUnit 5 + Mockito + StepVerifier.

**Design source:** `docs/specs/2026-07-04-P027-R2a-reactive-redis-design.md`.

## Global Constraints

- **Per-task PR workflow (user reviews each):** each Task below is its own branch off `feature/P-027-reactive-appbase` (name `feature/P-027-R2a-t<N>-<slug>`), one PR into `feature/P-027-reactive-appbase`. Tasks are sequential — a task branches off the feature branch **after** the previous task's PR is merged. Do NOT bundle tasks into one PR. Wait for the human merge before starting the next task.
- **No new module.** All shared code in `appbase-client-core`; Redis code in `appbase-client-redis`.
- **Project Reactor only.** `reactor-core`/`reactor-test` already managed in root `pom.xml` via `${reactor.version}` (3.6.8). Never add a version outside root `<dependencyManagement>`.
- **Apache license header** (verbatim, same block as the R0 files) tops every new `.java`.
- **Authorship** — every new class Javadoc carries, in order: `@author Navaneeth Mysore G`, `@author Claude`, `@since 1.0.0`. (Consistent name `Claude`, no model name.)
- **No fully-qualified class names** in code or tests — imports only.
- **Config pattern (mandatory)** for any config change: mirror `DefaultReactiveOperationConfiguration` (R0) / `DefaultReactorServiceConfiguration` — `@Immutable` `Default*` + `@JsonPOJOBuilder(withPrefix="")` `Builder` with `DEFAULT_*` constants inside `// CheckStyle: Builder start`/`end` markers + nested `@interface Valid` + a `Default*Validator` honoring the JSR-303 contract (`isValid` returns/records via `ValidationService.addViolationOrThrow`, throws only when context null).
- **Sensible defaults on every knob.** Zero-config must work.
- **Metrics naming:** `metricService.name(getClass(), "<op>", "<suffix>")`. `getClass()` = the concrete accessor, so each twin gets its own namespace automatically. Metrics recorded via `MetricService` auto-export to JMX (composite `JmxMeterRegistry`) — no JMX wiring in accessors.
- **Build command (verbatim — the `-am` build aborts at `appbase-core` without the surefire flag):**
  `cd /Users/nav/Viveworks/Technology/Appbase && mvn -q -pl <module-path> -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=<Class> test`
  (`<module-path>` = `appbase-java/appbase-client/appbase-client-core` or `.../appbase-client-redis`.)
- **Build regenerates `appbase-java/CHANGELOG.adoc`** (untracked-content artifact) — never stage/commit it.
- **TDD, frequent commits.** Test-first per behavior; every PR very well tested + documented.

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

---

## File Structure

**`appbase-client-core`** (shared spine + R0 amendment), package `com.viveworks.appbase.client.core.accessor` (new) and existing `com.viveworks.appbase.client.core.reactive`:
- `accessor/AbstractAppbaseAccessor.java` — NEW. Cross-cutting spine: metrics (`Timer`+error `Counter` caches), config-gated span, `mapError`/`isNotable` hooks, `logFailure`, config-gated `reportNotable` (→ mediator → ExceptionStore), and the sync `execute(op, Callable)` fork.
- `reactive/ReactiveOperationConfiguration.java` + `DefaultReactiveOperationConfiguration.java` + validator — MODIFY: add `metricsEnabled`/`traceEnabled`/`exceptionStoreEnabled` knobs.
- `reactive/AbstractReactiveAccessor.java` — MODIFY: extend `AbstractAppbaseAccessor`; add named `mono(op, source)`/`flux(op, source)` overloads wrapping the cross-cutting spine around the R0 operators; keep the old nameless overloads delegating with a default op name.

**`appbase-client-redis`** (the twin), package `com.viveworks.appbase.client.service.redis.reactive` (new) + existing `…redis` / `…redis.configuration`:
- `reactive/ReactiveRedisAccessor.java` — NEW.
- `reactive/RedisAccessorException.java` — NEW (`extends AppbaseClientException`).
- `RedisAccessor.java` — NEW (sync, on the shared spine).
- `configuration/RedisClientServiceConfiguration.java` + `DefaultRedisClientServiceConfiguration.java` (+ validator) — MODIFY: add `ReactiveOperationConfiguration` field.
- `RedisClientService.java` + `DefaultRedisClientService.java` — MODIFY: `getReactiveAccessor()` / `getReactiveAccessor(AppbaseClientKey<TextValue>)`.
- `DefaultRedisClientIntegrationService.java` — MODIFY (Task 7, optional): Lettuce `MicrometerCommandLatencyRecorder`.

Docs: `appbase-java/build-guide.md` (Task 6).

---

## Task 1: Shared `AbstractAppbaseAccessor` spine + amend `AbstractReactiveAccessor` + config knobs

**Branch:** `feature/P-027-R2a-t1-accessor-spine`

**Files:**
- Create: `appbase-java/appbase-client/appbase-client-core/src/main/java/com/viveworks/appbase/client/core/accessor/AbstractAppbaseAccessor.java`
- Modify: `.../client/core/reactive/ReactiveOperationConfiguration.java` (+ `Default…` + `…Validator`) — add 3 knobs
- Modify: `.../client/core/reactive/AbstractReactiveAccessor.java` — extend the spine; named `mono`/`flux`
- Test: `.../client/core/accessor/AbstractAppbaseAccessorTest.java`
- Test: modify `.../client/core/reactive/AbstractReactiveAccessorTest.java` (named overloads + metrics/counter/notable)
- Test: modify `.../client/core/reactive/DefaultReactiveOperationConfigurationTest.java` + `…ValidatorTest.java` (new knobs)

**Interfaces:**
- Consumes: `AppbaseCore` (`getMetricService`, `getTracerService`, `getMediatorService`, `getLogService`), `MetricService.name(Class, String, String...)`, `MetricRegistry.timer/counter`, `org.eclipse.microprofile.metrics.{Timer,Counter}`, `io.opentracing.{Span,Tracer}`, `ReactiveErrorMapper.map`, `com.viveworks.appbase.core.util.AppbaseUtil.notNull`.
- Produces:
  - `AbstractAppbaseAccessor(AppbaseCore appbaseCore, boolean metricsEnabled, boolean traceEnabled, boolean exceptionStoreEnabled)`; protected members: `Logger logger`; `Timer operationTimer(String op)`; `Counter operationErrorCounter(String op)`; `@Nullable Span startSpan(String op)`; `void finishSpan(@Nullable Span span, boolean errored)`; `Throwable mapError(Throwable t)`; `boolean isNotable(Throwable mapped)`; `void logFailure(String op, Throwable t)`; `void onOperationError(String op, Throwable t)` (increments counter + logs + reportNotable); `<T> T execute(String op, java.util.concurrent.Callable<T> call) throws Exception`.
  - `ReactiveOperationConfiguration` gains `boolean isMetricsEnabled()`, `boolean isTraceEnabled()`, `boolean isExceptionStoreEnabled()`; `DefaultReactiveOperationConfiguration.Builder` gains `metricsEnabled/traceEnabled/exceptionStoreEnabled` with `DEFAULT_METRICS_ENABLED=true`, `DEFAULT_TRACE_ENABLED=true`, `DEFAULT_EXCEPTION_STORE_ENABLED=false`.
  - `AbstractReactiveAccessor` gains `<T> Mono<T> mono(String op, Supplier<Mono<T>> source)`, `<T> Flux<T> flux(String op, Supplier<Flux<T>> source)`; the existing nameless `mono(Supplier)`/`flux(Supplier)` delegate with op name `"reactiveOperation"`.

- [ ] **Step 1: Write failing config test for the new knobs**

Add to `DefaultReactiveOperationConfigurationTest.java`:
```java
@Test
void crossCuttingKnobsHaveDefaults() {
    final ReactiveOperationConfiguration config =
            DefaultReactiveOperationConfiguration.builder().build();
    assertTrue(config.isMetricsEnabled());
    assertTrue(config.isTraceEnabled());
    assertFalse(config.isExceptionStoreEnabled());
}

@Test
void crossCuttingKnobsOverridable() {
    final ReactiveOperationConfiguration config =
            DefaultReactiveOperationConfiguration.builder()
                    .metricsEnabled(false).traceEnabled(false).exceptionStoreEnabled(true).build();
    assertFalse(config.isMetricsEnabled());
    assertFalse(config.isTraceEnabled());
    assertTrue(config.isExceptionStoreEnabled());
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl appbase-java/appbase-client/appbase-client-core -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DefaultReactiveOperationConfigurationTest test`
Expected: FAIL — `isMetricsEnabled`/`metricsEnabled(...)` undefined.

- [ ] **Step 3: Add the 3 knobs to `ReactiveOperationConfiguration` + `Default…` + `…Validator` (stub)**

`ReactiveOperationConfiguration.java` — add 3 getters (`boolean isMetricsEnabled();` etc.).
`DefaultReactiveOperationConfiguration.java` — add 3 `private final boolean` fields, getters, `equals`/`hashCode`/`toString` entries, and Builder fields + fluent setters + `DEFAULT_*` constants (all mirroring the existing `enabled` boolean field exactly). No new validation rule needed (booleans); leave the validator signature extended only if it already lists every field — otherwise no validator change.
> Stub guidance: copy the `enabled` field end-to-end for each of the 3 new booleans; defaults `true/true/false`.

- [ ] **Step 4: Run config tests green**

Run: `… -Dtest=DefaultReactiveOperationConfigurationTest,DefaultReactiveOperationConfigurationValidatorTest test`
Expected: PASS.

- [ ] **Step 5: Write failing spine test**

Create `AbstractAppbaseAccessorTest.java` — a tiny concrete subclass exposes `execute`; mock `AppbaseCore`→`MetricService`/`MetricRegistry`/`Timer`/`Counter`, `TracerService`/`Tracer`/`Span`, `MediatorService`, `LogService`:
```java
class AbstractAppbaseAccessorTest {
    private static final class TestAccessor extends AbstractAppbaseAccessor {
        TestAccessor(AppbaseCore c, boolean m, boolean t, boolean e) { super(c, m, t, e); }
        <T> T call(String op, java.util.concurrent.Callable<T> c) throws Exception { return execute(op, c); }
    }

    @Test
    void executeRecordsTimerAndReturnsValue() throws Exception {
        final Fixtures f = Fixtures.create(true, false, false);
        final String out = f.accessor.call("get", () -> "v");
        assertEquals("v", out);
        Mockito.verify(f.timer).time();          // timer started
        Mockito.verify(f.timerContext).stop();   // and stopped
    }

    @Test
    void executeOnFailureIncrementsErrorCounterAndRethrows() {
        final Fixtures f = Fixtures.create(true, false, false);
        assertThrows(IllegalStateException.class,
                () -> f.accessor.call("get", () -> { throw new IllegalStateException("boom"); }));
        Mockito.verify(f.errorCounter).inc();
    }

    @Test
    void reportNotableHitsMediatorOnlyWhenEnabledAndNotable() {
        final Fixtures f = Fixtures.create(false, false, /*exceptionStore*/ true);
        assertThrows(RuntimeException.class,
                () -> f.accessor.call("get", () -> { throw new RuntimeException("x"); }));
        Mockito.verify(f.mediator).asyncHandleMessage(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void exceptionStoreDisabledDoesNotTouchMediator() {
        final Fixtures f = Fixtures.create(true, false, /*exceptionStore*/ false);
        assertThrows(RuntimeException.class,
                () -> f.accessor.call("get", () -> { throw new RuntimeException("x"); }));
        Mockito.verifyNoInteractions(f.mediator);
    }
}
```
> `Fixtures` is a small test helper stubbing the `AppbaseCore` getter chain (mirror the R0 `AbstractReactiveAccessorTest` mocking style; `timer.time()` → a mocked `Timer.Context`). Keep mocks minimal.

- [ ] **Step 6: Run to verify it fails**

Run: `… -Dtest=AbstractAppbaseAccessorTest test`
Expected: FAIL — `AbstractAppbaseAccessor` does not exist.

- [ ] **Step 7: Write `AbstractAppbaseAccessor` (stub — signatures + guidance)**

Create `AbstractAppbaseAccessor.java` (license header + author block), signatures:
```java
public abstract class AbstractAppbaseAccessor {
    protected final AppbaseCore appbaseCore;
    protected final Logger logger;
    private final MetricService metricService;
    private final boolean metricsEnabled;
    private final boolean traceEnabled;
    private final boolean exceptionStoreEnabled;
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    protected AbstractAppbaseAccessor(AppbaseCore appbaseCore, boolean metricsEnabled,
                                      boolean traceEnabled, boolean exceptionStoreEnabled) { /* notNull; wire logger + metricService */ }

    protected Timer operationTimer(String op) { /* cache: name(getClass(), op, "timer") */ }
    protected Counter operationErrorCounter(String op) { /* cache: name(getClass(), op, "error") */ }
    @Nullable protected Span startSpan(String op) { /* if traceEnabled: tracer.buildSpan(name(getClass())+"."+op).start(); else null */ }
    protected void finishSpan(@Nullable Span span, boolean errored) { /* if span!=null: if errored Tags.ERROR; span.finish() */ }
    protected Throwable mapError(Throwable t) { return ReactiveErrorMapper.map(t); }   // overridable
    protected boolean isNotable(Throwable mapped) { return true; }                     // overridable
    protected void logFailure(String op, Throwable t) { logger.error("Reactive/sync op {} failed", op, t); }
    protected void reportNotable(String op, Throwable mapped) {
        // if exceptionStoreEnabled && isNotable(mapped): build MessageFactory.exceptionMessage(mediator, getClass(), msg, threadName, mapped)
        //   and appbaseCore.getMediatorService().getMediator().asyncHandleMessage(msg, true);
    }
    protected void onOperationError(String op, Throwable t) {
        if (metricsEnabled) operationErrorCounter(op).inc();
        logFailure(op, t); reportNotable(op, t);
    }
    protected <T> T execute(String op, Callable<T> call) throws Exception {
        final Timer.Context c = metricsEnabled ? operationTimer(op).time() : null;
        final Span span = startSpan(op);
        boolean errored = false;
        try { return call.call(); }
        catch (Exception e) { errored = true; final Throwable m = mapError(e); onOperationError(op, m);
            throw (m instanceof Exception) ? (Exception) m : new RuntimeException(m); }
        finally { finishSpan(span, errored); if (c != null) c.stop(); }
    }
}
```
> Guidance: metric name via `metricService.name(getClass(), op, suffix)`; timers/counters cached in the maps; when `metricsEnabled==false`, skip timer/counter. `MessageFactory.exceptionMessage` + mediator mirror `AbstractAppbaseService.handleException`.

- [ ] **Step 8: Run spine test green**

Run: `… -Dtest=AbstractAppbaseAccessorTest test`
Expected: PASS (4 tests).

- [ ] **Step 9: Write failing reactive named-overload test**

Add to `AbstractReactiveAccessorTest.java`:
```java
@Test
void namedMonoRecordsTimerAndErrorCounterOnFailure() {
    final TestAccessor accessor = accessor(DefaultReactiveOperationConfiguration.builder()
            .retryEnabled(false).build());   // metricsEnabled defaults true
    StepVerifier.create(accessor.exposeMono("get", () -> Mono.error(new IllegalStateException("x"))))
            .expectError(ReactiveOperationException.class).verify();
    // verify the error counter for op "get" was incremented (mock MetricService as in the spine test)
}
```
> Extend the existing `TestAccessor` with `exposeMono(String, Supplier)`/`exposeFlux(String, Supplier)`; upgrade the test's `AppbaseCore` mock to also stub `getMetricService()`/`getTracerService()`/`getMediatorService()` (reuse the spine test's Fixtures approach).

- [ ] **Step 10: Run to verify it fails**

Run: `… -Dtest=AbstractReactiveAccessorTest test`
Expected: FAIL — `mono(String, Supplier)` undefined.

- [ ] **Step 11: Amend `AbstractReactiveAccessor` (stub)**

Change `class AbstractReactiveAccessor` to `extends AbstractAppbaseAccessor implements ReactiveAccessor`; ctor calls `super(appbaseCore, config.isMetricsEnabled(), config.isTraceEnabled(), config.isExceptionStoreEnabled())` and keeps `reactorService`/`configuration` fields. Add named overloads assembling the design snippet (§3.1):
```java
protected <T> Mono<T> mono(String op, Supplier<Mono<T>> source) {
    return Mono.defer(() -> {
        final Timer.Context c = configuration.isMetricsEnabled() ? operationTimer(op).time() : null;
        final Span span = startSpan(op);
        Mono<T> m = source.get().timeout(configuration.getTimeout());
        if (configuration.isRetryEnabled()) { m = m.retryWhen(retrySpec()); }
        return m.onErrorMap(this::mapError)
                .doOnError(e -> onOperationError(op, e))
                .doFinally(sig -> { finishSpan(span, sig != SignalType.ON_COMPLETE); if (c != null) c.stop(); });
    }).subscribeOn(scheduler());
}
// flux(op, source): same + .onBackpressureBuffer(getBackpressureBufferSize()) AFTER retry, before onErrorMap.
protected <T> Mono<T> mono(Supplier<Mono<T>> source) { return mono("reactiveOperation", source); }
protected <T> Flux<T> flux(Supplier<Flux<T>> source) { return flux("reactiveOperation", source); }
```
> Keep `mapError` = base default (`ReactiveErrorMapper.map`) here; twins override. Keep `retrySpec()`/`scheduler()` as in R0 (retry already has `onRetryExhaustedThrow(signal.failure())`).

- [ ] **Step 12: Run all reactive-package tests green**

Run: `mvn -q -pl appbase-java/appbase-client/appbase-client-core -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: BUILD SUCCESS, all existing + new tests pass.

- [ ] **Step 13: Commit + open PR**

```bash
git checkout feature/P-027-reactive-appbase && git pull
git checkout -b feature/P-027-R2a-t1-accessor-spine
git add appbase-java/appbase-client/appbase-client-core/src/
git commit -m "feat(P-027-R2a): shared AbstractAppbaseAccessor spine (metrics/trace/exception) + named reactive mono/flux + cross-cutting config knobs"
git push -u origin feature/P-027-R2a-t1-accessor-spine
gh pr create --base feature/P-027-reactive-appbase --title "P-027 R2a Task 1: accessor cross-cutting spine" --body "…"
```
Then STOP — wait for human merge before Task 2.

---

## Task 2: `ReactiveOperationConfiguration` field on the Redis client config

**Branch:** `feature/P-027-R2a-t2-redis-config`

**Files:**
- Modify: `.../redis/configuration/RedisClientServiceConfiguration.java` (+ `Default…` + `…Validator`)
- Test: `.../redis/configuration/DefaultRedisClientServiceConfigurationTest.java`

**Interfaces:**
- Consumes: `ReactiveOperationConfiguration`, `DefaultReactiveOperationConfiguration` (Task 1).
- Produces: `RedisClientServiceConfiguration.getReactiveOperationConfiguration() : ReactiveOperationConfiguration`; `DefaultRedisClientServiceConfiguration.Builder.reactiveOperationConfiguration(...)`; default = `DefaultReactiveOperationConfiguration.builder().build()`.

- [ ] **Step 1: Failing test**
```java
@Test
void reactiveConfigDefaultsWhenOmitted() {
    final RedisClientServiceConfiguration c = DefaultRedisClientServiceConfiguration.builder().build();
    assertNotNull(c.getReactiveOperationConfiguration());
    assertTrue(c.getReactiveOperationConfiguration().isMetricsEnabled());
}
```
- [ ] **Step 2: Run — FAIL** (`getReactiveOperationConfiguration` undefined). Command: `… -pl …/appbase-client-redis … -Dtest=DefaultRedisClientServiceConfigurationTest test`.
- [ ] **Step 3: Add the field (stub)** — mirror an existing object-typed field on `DefaultRedisClientServiceConfiguration` (add getter, `private final` field, Builder field + setter + `DEFAULT_REACTIVE_OPERATION_CONFIGURATION = DefaultReactiveOperationConfiguration.builder().build()`, equals/hashCode/toString entries). If the validator lists fields, add a passthrough (no new rule).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Add a YAML round-trip test** asserting an overridden `reactiveOperationConfiguration.timeout` deserializes (mirror an existing config YAML test in the module). Run — PASS.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): reactive operation configuration on Redis client config`); base `feature/P-027-reactive-appbase`. STOP for merge.

---

## Task 3: `ReactiveRedisAccessor` + `RedisAccessorException`

**Branch:** `feature/P-027-R2a-t3-reactive-redis-accessor`

**Files:**
- Create: `.../redis/reactive/ReactiveRedisAccessor.java`
- Create: `.../redis/reactive/RedisAccessorException.java`
- Test: `.../redis/reactive/ReactiveRedisAccessorTest.java`

**Interfaces:**
- Consumes: `AbstractReactiveAccessor` (Task 1), `ReactiveOperationConfiguration`, `ReactorService`, `AppbaseCore`, `AppbaseClientException`, Lettuce `StatefulRedisConnection<String,String>`/`RedisReactiveCommands<String,String>`, the pool borrow API (`AppbaseClientIntegrationService.getClient()`, `finalizeOperation()`) — the twin borrows via a supplier passed in by Task 4.
- Produces:
  - `RedisAccessorException extends AppbaseClientException` with `(Throwable cause)` + `(String message, Throwable cause)` ctors.
  - `ReactiveRedisAccessor extends AbstractReactiveAccessor`; ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String,String>, TextValue>> borrow)`; methods:
    - `RedisReactiveCommands<String,String> reactive(StatefulRedisConnection<String,String> conn)` (raw escape hatch helper), and a borrow-scoped `<T> Mono<T> command(String op, Function<RedisReactiveCommands<String,String>, Mono<T>> op)` / `<T> Flux<T> commandMany(String op, Function<…, Flux<T>>)`.
  - overrides `protected Throwable mapError(Throwable)` (Lettuce `RedisException`/`RedisCommandTimeoutException` → `RedisAccessorException`, else super) and `protected boolean isNotable(Throwable)` (false for expected Redis conditions).

- [ ] **Step 1: Failing test** — mock the borrow supplier + integration service + `RedisReactiveCommands`; `ReactorService` immediate scheduler; `MetricService`/`TracerService`/`MediatorService` mocks (reuse Task-1 fixtures):
```java
@Test
void commandRunsAndReturnsValue() {
    StepVerifier.create(accessor.command("get", cmds -> Mono.just("v"))).expectNext("v").verifyComplete();
}
@Test
void integrationServiceReturnedToPoolOnSuccessAndError() {
    accessor.command("get", cmds -> Mono.just("v")).block();
    accessor.command("get", cmds -> Mono.error(new IllegalStateException("x"))).onErrorResume(e -> Mono.empty()).block();
    Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();   // returned both times
}
@Test
void lettuceErrorMappedToRedisAccessorException() {
    StepVerifier.create(accessor.command("get", cmds -> Mono.error(new io.lettuce.core.RedisException("down"))))
            .expectError(RedisAccessorException.class).verify();
}
```
- [ ] **Step 2: Run — FAIL** (classes absent). Command: `… -pl …/appbase-client-redis … -Dtest=ReactiveRedisAccessorTest test`.
- [ ] **Step 3: Write `RedisAccessorException` (stub)** — trivial `extends AppbaseClientException`, two ctors, `serialVersionUID`.
- [ ] **Step 4: Write `ReactiveRedisAccessor` (stub)** — `command(op, fn)` builds `super.mono(op, () -> Mono.usingWhen(Mono.fromSupplier(borrow), svc -> fn.apply(svc.getClient().reactive()), svc -> Mono.fromRunnable(svc::finalizeOperation)))`; `commandMany` the `Flux` analogue via `Flux.usingWhen`. Override `mapError`/`isNotable` for Lettuce.
  > Guidance: `usingWhen`'s async-cleanup runs on success, error, AND cancel — that guarantees pool return. `reactive()` on the borrowed connection yields `RedisReactiveCommands`.
- [ ] **Step 5: Run — PASS** (3 tests).
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): ReactiveRedisAccessor with pooled usingWhen borrow/return + Lettuce error mapping`). STOP for merge.

---

## Task 4: `getReactiveAccessor()` acquisition on the Redis client service

**Branch:** `feature/P-027-R2a-t4-acquisition`

**Files:**
- Modify: `.../redis/RedisClientService.java` (interface) + `.../redis/DefaultRedisClientService.java`
- Test: `.../redis/DefaultRedisClientServiceReactiveTest.java`

**Interfaces:**
- Consumes: `ReactiveRedisAccessor` (Task 3), the config’s `getReactiveOperationConfiguration()` (Task 2), the pool (`getClientIntegrationService(key)`), `appbaseCore.getReactorService()`.
- Produces: `RedisClientService.getReactiveAccessor() : ReactiveRedisAccessor` + `getReactiveAccessor(AppbaseClientKey<TextValue>) : ReactiveRedisAccessor`; builds a `ReactiveRedisAccessor` whose borrow supplier calls `getClientIntegrationService(key)`.

- [ ] **Step 1: Failing test** — start `DefaultRedisClientService` with a test config (mirror the module’s existing service-start test helper / `RedisClientServiceTestHelper`); assert `getReactiveAccessor()` returns non-null and its `getReactiveOperationConfiguration()` matches the config.
- [ ] **Step 2: Run — FAIL** (`getReactiveAccessor` undefined).
- [ ] **Step 3: Add the methods (stub)** — interface declarations + `DefaultRedisClientService` impl constructing the accessor with `appbaseCore`, `appbaseCore.getReactorService()`, `getConfiguration().getReactiveOperationConfiguration()`, and a borrow supplier `() -> getClientIntegrationService(key)`. Default-key overload delegates to `defaultAppbaseClientKey`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module test** (`… test`, no `-Dtest`) — confirm no regression.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): RedisClientService.getReactiveAccessor() acquisition`). STOP for merge.

---

## Task 5: Sync `RedisAccessor` on the shared spine (closes the sync-metrics gap)

**Branch:** `feature/P-027-R2a-t5-sync-redis-accessor`

**Files:**
- Create: `.../redis/RedisAccessor.java`
- Modify: `.../redis/RedisClientService.java` + `.../redis/DefaultRedisClientService.java` — add `getAccessor()` / `getAccessor(key)`
- Test: `.../redis/RedisAccessorTest.java`

**Interfaces:**
- Consumes: `AbstractAppbaseAccessor` (Task 1), the borrow supplier pattern (Task 3/4), Lettuce `RedisCommands<String,String>` (sync) via `connection.sync()`.
- Produces: `RedisAccessor extends AbstractAppbaseAccessor`; ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<…>> borrow)`; `<T> T command(String op, Function<RedisCommands<String,String>, T> fn)` using `execute(op, () -> { borrow; try { return fn.apply(conn.sync()); } finally { finalizeOperation(); } })`; overrides `mapError`/`isNotable` for Lettuce (reuse the same logic — factor a shared static in `RedisAccessorException` or a small util to avoid duplication with Task 3).
- `RedisClientService.getAccessor() / getAccessor(AppbaseClientKey<TextValue>)`.

- [ ] **Step 1: Failing test** — mock borrow + `RedisCommands`; `MetricService` mock; assert `command("get", …)` returns the value, records a `Timer`, increments error counter on failure, and returns the integration service to the pool in both cases.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Write `RedisAccessor` (stub)** as above; factor the Lettuce→`RedisAccessorException` mapping shared with Task 3 (single static helper) to stay DRY.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Add `getAccessor()` acquisition + a start-service test** (mirror Task 4). Run whole-module — PASS.
- [ ] **Step 6: Commit + PR** (`feat(P-027-R2a): sync RedisAccessor on shared spine — per-op metrics for the sync path`). STOP for merge.

---

## Task 6: Integration test (opt-in) + docs

**Branch:** `feature/P-027-R2a-t6-it-docs`

**Files:**
- Test: `.../redis/reactive/ReactiveRedisAccessorIntegrationTest.java` (gated by `RedisClientServiceTests.enabled` / `REDIS_CLIENT_SERVICE_TESTS_ENABLED`, mirror the module’s existing IT gating)
- Modify: `appbase-java/build-guide.md`

**Interfaces:** none new (exercises Tasks 3–5 end-to-end).

- [ ] **Step 1: Write the opt-in IT** — mirror the module’s existing integration-test bootstrap (real Redis via `appbase-client`’s docker infra). Body: start `DefaultRedisClientService`, `getReactiveAccessor()`, `SET k v` then `GET k` reactively, `StepVerifier` asserts `"v"`; assert a Timer named for `ReactiveRedisAccessor` exists in the `MetricRegistry`. Gate with the same `@EnabledIf`/env flag the other Redis ITs use.
- [ ] **Step 2: Run gated OFF** — `… -pl …/appbase-client-redis -am … -Dtest=ReactiveRedisAccessorIntegrationTest test` (flag unset) → the IT is skipped; BUILD SUCCESS.
- [ ] **Step 3: (If Redis available) Run gated ON** — `REDIS_CLIENT_SERVICE_TESTS_ENABLED=true DRedisClientServiceTests.enabled=true …` → PASS. If no Redis locally, note it and rely on CI.
- [ ] **Step 4: Docs** — extend the “Reactive accessors” section of `build-guide.md` with: a reactive Redis example (`getReactiveAccessor().command("get", c -> c.get(key))`), the sync `getAccessor()` counterpart, the `metricsEnabled`/`traceEnabled`/`exceptionStoreEnabled` knobs, and a note that metrics auto-export to JMX. Verify: `grep -n "Reactive Redis" appbase-java/build-guide.md`.
- [ ] **Step 5: Commit + PR** (`test(P-027-R2a): opt-in reactive Redis IT + docs`). STOP for merge.

---

## Task 7 (optional): Lettuce-native command metrics

**Branch:** `feature/P-027-R2a-t7-lettuce-metrics`

**Files:**
- Modify: `.../redis/DefaultRedisClientIntegrationService.java` (`initImpl`)
- Modify: `appbase-java/appbase-client/appbase-client-redis/pom.xml` (add `io.lettuce:lettuce-core` micrometer support is in-core; add `io.micrometer:micrometer-core` if not already transitive — version in root depMgmt only)
- Test: `.../redis/DefaultRedisClientIntegrationServiceMetricsTest.java`

**Interfaces:**
- Consumes: `MetricService.registerMicrometerRegistry(...)` / `getMicrometerRegistries()`, Lettuce `ClientResources`, `io.lettuce.core.metrics.MicrometerCommandLatencyRecorder`, `MicrometerOptions`.
- Produces: Lettuce `RedisClient` built with `ClientResources.builder().commandLatencyRecorder(new MicrometerCommandLatencyRecorder(meterRegistry, MicrometerOptions.create())).build()`, the `meterRegistry` joined to Appbase’s composite via `metricService.registerMicrometerRegistry(...)`.

- [ ] **Step 1: Failing test** — construct the integration service with a mocked `MetricService`; assert `registerMicrometerRegistry(...)` is invoked and the built `RedisClient` uses `ClientResources` with a command-latency recorder (verify via the resources builder path or a seam). *(If verifying the private Lettuce wiring is awkward, assert the observable effect: after a command, a `lettuce.command.*` meter appears in the registry — do this in the opt-in IT instead and keep the unit test to the `registerMicrometerRegistry` call.)*
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Wire it (stub)** in `initImpl`: obtain/create a Micrometer registry, register it with `MetricService`, build `ClientResources` with the recorder, pass to `RedisClient.create(clientResources, redisUri)`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit + PR** (`feat(P-027-R2a): route Lettuce command-latency metrics into Appbase Micrometer/JMX`). STOP for merge.

---

## Self-Review (completed by plan author)

- **Spec coverage:** Task 1 = shared spine + metrics(+JMX auto-export)/trace/exception knobs + named reactive overloads (design §2, §3.1, §3.3, Decisions A/B/C/D/E); Task 2 = Redis config field (§3.3); Task 3 = `ReactiveRedisAccessor` + `RedisAccessorException` + `usingWhen` pool borrow/return + Lettuce mapping (§2.4/2.5, §3.2); Task 4 = acquisition (§3.4); Task 5 = sync accessor parity (§2.6, Decision A); Task 6 = opt-in IT + docs (§4); Task 7 = optional Lettuce-native metrics (§3.2 optional). Every design section maps to a task.
- **Placeholder scan:** implementations are intentionally stubbed (signatures + guidance) per project convention (user reviews each PR); every TDD RED step shows real test code and an exact run command; no "TBD"/"handle edge cases".
- **Type consistency:** `mono(String op, Supplier)`/`flux(String op, Supplier)`, `execute(String op, Callable)`, `operationTimer`/`operationErrorCounter`, `mapError`/`isNotable`, `getReactiveAccessor()`/`getAccessor()`, `getReactiveOperationConfiguration()`, `RedisAccessorException` used consistently across Tasks 1–7. `ReactiveOperationConfiguration` new getters (`isMetricsEnabled`/`isTraceEnabled`/`isExceptionStoreEnabled`) defined in Task 1, consumed in Tasks 1/2/3/5.
- **Open follow-ups (not in this plan):** full OpenTracing active-scope context propagation across the reactive hop (design Decision B) — deferred; a later task if distributed traces need it.
