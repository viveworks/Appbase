# P-027 R1 — Reactive Web Layer (design spec)

**Status:** approved · **Date:** 2026-07-09 · **Epic:** P-027 (issue internal work item 277) · **Milestone:** R1
**Umbrella spec:** `docs/specs/2026-07-04-P027-reactive-appbase-design.md`
**Adjacent:** P-033 transport-pluggable bootstrap (`docs/specs/2026-07-08-P033-transport-pluggable-bootstrap-design.md`)

---

## 1. Goal

Give Appbase a **natively non-blocking HTTP face**: a reactor-netty server with no servlet
container, no thread-per-request connector, and an event loop that is never blocked by request
handling.

R1 delivers three things:

1. A transport-neutral request/response abstraction (`AppbaseServiceExchange`) and a reactive filter
   contract (`AppbaseServiceFilter`), so cross-cutting concerns are written once per concern rather
   than once per transport.
2. A new `appbase-bootstrap-reactor-netty` runtime module implementing the existing
   `AppbaseWebServer` lifecycle interface.
3. The health endpoint and the nine core servlets, served over that face.

---

## 2. Scope boundary vs P-033 (RESOLVED)

The P-027 umbrella spec (2026-07-04) lists a **servlet `startAsync` adapter** under R1. The P-033
design note (2026-07-08, four days newer) specifies that same bridge in detail as its clause (4)
(`ReactiveServletSupport`, `AbstractReactiveServlet`, JAX-RS `@Suspended` adapter), and explicitly
frames R1 by contrast as *"the native fully-non-blocking web face … (reactor-netty)"*.

**Decision: P-033 owns the servlet async bridge. R1 does not build it.**

R1 = neutral filter abstraction + reactor-netty runtime. Nothing servlet-shaped.

Rationale: the bridge belongs beside the servlet code that P-033 extracts into
`appbase-bootstrap-servlet`; building it in `-core` now guarantees P-033 immediately moves it.
Consequence: existing Jetty/Tomcat/Undertow deployments gain no reactive edge until P-033 lands.
Accepted — the R5 showcase runs on reactor-netty, so it is unblocked.

**Sequencing.** The prioritized action plan orders P-027 → P-033 → P-020. R1 therefore builds on
`appbase-bootstrap-core` as it exists today. This is safe: `AppbaseWebServer` is already
servlet-free (`AppbaseWebServer.java:31-61`); only `AppbaseServletDeployment` and
`AppbaseServletRegistrar` import `jakarta.servlet`. `ReactorNettyAppbaseWebServer` implements the
existing interface unchanged and touches neither. When P-033 later renames `AppbaseWebServer` →
`AppbaseServletContainer` for the servlet family, R1's server must be **excluded** from that rename:
it is not a servlet container. P-033 must introduce a genuinely neutral lifecycle supertype (or keep
`AppbaseWebServer` as that supertype) rather than renaming it wholesale.

---

## 3. Module topology

R1 adds exactly **one module** and **one package**.

```
appbase-service-core                          [existing · +new package]
  service/exchange/
    ├── AppbaseServiceExchange
    ├── AppbaseServiceFilter
    ├── AppbaseServiceFilterChain
    └── (AppbaseFilterConfig already lives in service/web/filter/)
        ▲
appbase-service-web                           [existing · +reactive filter impls]
    ├── reuse (already transport-neutral): AuthorizationFilter, CorsFilter,
    │         UserSessionFilter, IdpUserTokenCheckFilter
    └── new neutral logic + reactive filters: metric, traceId, exceptionHandler
        ▲
appbase-bootstrap-reactor-netty               [NEW MODULE]
    ├── ReactorNettyAppbaseWebServer implements AppbaseWebServer
    ├── NettyWebExchange implements AppbaseServiceExchange
    ├── AppbaseRouteRegistry
    ├── AppbaseReactiveEndpointRegistrar   (the 10 ported endpoints)
    └── ReactorNettyAppbaseServerModule    (Guice)
```

**Why the abstraction is NOT in `appbase-bootstrap-core`.** The umbrella spec §5 says the neutral
web-filter abstraction "folds into existing `appbase-bootstrap-core`". That is **wrong** and this
spec corrects it. The dependency arrow runs `appbase-bootstrap-core → appbase-service-web →
appbase-service-core` (verified: `appbase-bootstrap-core/pom.xml` depends on `appbase-service-web`).
Filters living in `appbase-service-web` cannot implement an interface defined in a module above
them. The abstraction goes in `appbase-service-core`, beside the `AppbaseFilterConfig` that already
governs per-path filter behaviour.

**Why a new module is justified.** The umbrella spec's rule: *a module exists only to isolate a
third-party dependency; a new module is justified by a new driver-stack JAR, never by a new
interface.* `reactor-netty` is a new runtime JAR. The abstraction is a package, not a module.
Consistent with the spec's own table, which budgets exactly one new `appbase-bootstrap` module.

---

## 4. The exchange and filter contract

```java
/** Transport-neutral view of one HTTP request/response. No servlet, no Netty types. */
public interface AppbaseServiceExchange {
    String method();
    String path();
    @Nullable String queryParam(String name);
    @Nullable String header(String name);
    @Nullable String cookie(String name);

    void status(int code);
    void responseHeader(String name, String value);

    /** Unary response: serialise body, write, complete. */
    Mono<Void> writeJson(Object body);

    /** Streaming response: write each element, gated on downstream write demand. */
    <T> Mono<Void> writeStream(Flux<T> body);

    /** Request body, read without blocking. */
    Mono<byte[]> readBody();
}

/** One cross-cutting concern, transport-neutral and reactive. */
public interface AppbaseServiceFilter {
    Mono<Void> filter(AppbaseServiceExchange exchange, AppbaseServiceFilterChain chain);
    int order();
}

public interface AppbaseServiceFilterChain {
    Mono<Void> next(AppbaseServiceExchange exchange);
}
```

`Mono<Void>` is the return type deliberately. The existing neutral logic interfaces
(`AuthorizationFilter.isAuthorized(service, op, sessionId, requesterId) → boolean`) are
**request-side predicates over extracted primitives**. They work, and R1 reuses them unchanged. But
they cannot express the response side — "write this `Flux`, gated on downstream demand" — which is
precisely what a non-blocking server face exists to do. Hence a response-capable exchange.

### 4.1 Filter chain order — a security invariant

The reactive chain preserves the servlet chain's order exactly
(`AppbaseServletRegistrar.addServiceFilters`, `:134-141`):

```
exceptionHandler → CORS → metric → traceId → userSession → idpTokenCheck → authorization
```

**Authorization runs last.** Reordering is a security regression and must be covered by a test that
asserts the resolved order, not merely that each filter runs.

`AppbaseSyncServletFilter` has **no reactive counterpart** and is absent from the reactive chain: it
exists to bridge async→sync on the servlet path, which is meaningless here.

### 4.2 The two nuances, solved once in the chain

**Context propagation.** Hopping onto a scheduler loses every `ThreadLocal`: MDC logging context,
the OpenTracing span, and the security/request scope. These ride Reactor `Context`, with
`Hooks.enableAutomaticContextPropagation()` plus micrometer `context-propagation`. Solved in the
chain, not per filter. This is the same boundary problem P-033 calls out for its gRPC interceptors.

**Backpressure.** `writeStream` gates `onNext` on reactor-netty's write demand. It must never buffer
unbounded. A test must prove a slow consumer applies back-pressure to a fast `Flux` source rather
than growing heap.

---

## 5. Endpoints: health + the nine core servlets

### 5.1 The blocking audit

Ported endpoints, with what each actually does:

| Endpoint | Blocking? | Detail |
|---|---|---|
| `MetricServlet` | No | In-memory Micrometer/MicroProfile registries |
| `ServiceRegistryServlet` | No | In-memory service-entry map; volatile status reads |
| `StatusServlet` | No | In-memory registry + Jackson |
| `JmxServlet` | Reads no; `reload`/`shutdown` yes | `getMXBean` is a `ConcurrentHashMap` lookup, **not** a JMX proxy — no MBean-server round trip |
| `MediatorServlet` | Reads no; `reload`/`shutdown` yes | In-memory getters |
| `HealthCheckServlet` | **Yes** → fixed in §5.2 | S3 `headBucket`, ZooKeeper `checkExists`, Aerospike read, 2× JDBC `select`, all under a contended `ReentrantLock` |
| `LogServlet` | **Yes** | `LuceneLogStore` → `IndexSearcher.search` (blocking file I/O) |
| `ExceptionServlet` | **Yes** | `LuceneExceptionStore` → `IndexSearcher.search` + `DirectoryReader.document` |
| `ConfigurationServlet` | Reads no; `load`/`reload` yes | Config stores are filesystem/JPA-backed |
| `CoreServlet` | `status` no; lifecycle yes | `reload`/`shutdown` start and stop live services |

Cross-cutting: every POST/PUT `reload`-with-body path blocks on
`getInputStream().readAllBytes()` (`AbstractAppbaseSyncServlet.java:888-889,902`). The reactive
handlers use `exchange.readBody()` instead, which never blocks.

### 5.2 Health: serve from the snapshot (folded into R1)

`DefaultHealthCheckService` **already** schedules `runHealthCheck()` at a fixed delay on the
mediator's shared scheduled executor and stores the result in an `AtomicReference`
(`healthChecksRef`), with the interval configured by `getCacheSchedulerDelayConfiguration()`. The
snapshot machinery exists. The read path bypasses it:

```java
public HealthChecks checkHealth(final boolean force) {
    runHealthCheck();              // blocks; and `force` is IGNORED
    return healthChecksRef.get();
}

public HealthChecks checkHealth() {
    if (healthChecksRef.get() == null || !healthChecksRef.get().isHealthy()) {
        runHealthCheck();          // blocks whenever the last result was unhealthy
    }
    return healthChecksRef.get();
}
```

Two **pre-existing defects on the servlet path**, fixed here:

- **`force` is ignored** (`DefaultHealthCheckService.java:154-157`). Callers passing `force=false`
  expecting a cached read block on five network/JDBC round-trips.
- **Retry storm when unhealthy.** `checkHealth()` re-runs the checks precisely when the last result
  was unhealthy — i.e. it does the most blocking work exactly when a dependency is already slow or
  down, under a contended lock.

Changes, all in `appbase-service-core`, **none in `appbase-core`** (the shared scheduled executor is
already injected from `MediatorService`):

- Add `HealthChecks checkHealthSnapshot()` — a pure `healthChecksRef.get()` read. Never blocks,
  never locks, safe on an event loop.
- Honour `force`: `checkHealth(false)` returns the snapshot; `checkHealth(true)` re-runs.
- Remove the unhealthy-triggers-rerun behaviour. The scheduler owns refresh. A cold snapshot
  (service not yet run once) reports `unhealthy` with an explicit `snapshotAgeMillis` rather than
  synchronously running the checks.
- New config on `DefaultHealthCheckServiceConfiguration`: `cacheEnabled` (default `true`),
  reusing the existing `cacheSchedulerDelayConfiguration` for the interval, plus
  `staleAfter` — a snapshot older than this reports unhealthy rather than serving stale data.
- `HealthChecks` gains `snapshotAgeMillis` so callers can see staleness.

`GET /health?force=true` keeps its exact meaning on both faces. On the reactive face it is
**offloaded to bounded-elastic** (§5.3) — it blocks a worker, never the event loop. Chosen over
"async refresh, return snapshot" and "reject force on the reactive face" because it preserves the
existing contract and keeps the servlet and reactive faces behaviourally identical, at the cost of
one worker thread on a rare admin call.

**Servlet path benefits too.** These are service-layer fixes, not reactive-face fixes. The servlet
`HealthCheckServlet` stops blocking on five dependencies per request as a side effect.

### 5.3 Tier-1 offload for what remains

After §5.2, four endpoints are natively non-blocking (health, metric, serviceRegistry, status), and
Jmx/Mediator reads are too. What remains blocking is offloaded with `subscribeOn(boundedElastic)` —
the Tier-1 offload the umbrella spec defines and `AbstractReactiveAccessor` already uses for Vitess
`streamExecute`. The event loop is released immediately; the blocking call runs on a bounded worker.

Offloaded: `LogServlet` and `ExceptionServlet` reads (Lucene), `ConfigurationServlet.load/reload`,
`CoreServlet` lifecycle, the `reload`/`shutdown` op of every JMX-backed endpoint, and
`health?force=true`.

**Every offload carries a Javadoc note stating why it is offloaded and what removes it**, e.g.:

```java
/**
 * Tier-1 offload: LuceneLogStore.search performs blocking file I/O on the calling thread.
 * The event loop is released via subscribeOn(boundedElastic); the read runs on a worker.
 * Removes when R3 lands reactive core file I/O.
 */
```

This is an honest, documented staging point, not a hidden regression. R1's claim is precise: **the
reactive face never blocks its event loop.** It is not yet "every endpoint is natively
non-blocking," and the spec says so.

### 5.4 Routing

`AppbaseRouteRegistry` maps `(method, path)` to a handler returning `Mono<Void>` over the exchange.
Paths reuse the existing `AppbaseServiceConstants.DEFAULT_*_SERVLET_PATH` constants, so the reactive
face serves the **same paths** as the servlet face. No path-pattern language, no wildcards beyond an
exact match plus a trailing-segment capture. YAGNI: R5's showcase and the ten ported endpoints are
the only consumers.

---

## 6. Configuration

A new `ReactorNettyAppbaseWebServerConfiguration implements AppbaseConfiguration`, following the
established YAML/typed-config pattern. Every knob has a sensible default; the whole block is
optional.

```yaml
reactorNettyAppbaseWebServerConfiguration:
  enabled: true
  host: "0.0.0.0"
  port: 8080
  # Scheduler used for Tier-1 offload of blocking endpoints. Resolved via ReactorService.
  offloadSchedulerId: null        # null → ReactorService.getDefaultScheduler()
  accessLogEnabled: false
  compressionEnabled: false
  maxInitialLineLength: 4096
  maxHeaderSize: 8192
  idleTimeout: 60s
  # Reactor context propagation for MDC / tracing / security across the scheduler hop.
  contextPropagationEnabled: true
```

Health-service additions (`appbase-service-core`):

```yaml
defaultHealthCheckServiceConfiguration:
  cacheEnabled: true
  cacheSchedulerDelayConfiguration:     # exists today
    time: 30
    timeUnit: "SECONDS"
  staleAfter: 90s                        # snapshot older than this → unhealthy
```

**Explicitly out of scope for R1** (YAGNI): TLS/HTTPS, HTTP/2, WebSocket, access-log formats,
custom event-loop group sizing. reactor-netty supports all of them; none is needed to prove a
non-blocking face, and each is additive later.

---

## 7. Error handling

`exceptionHandler` is the outermost filter, mirroring the servlet chain. It maps a failed `Mono` to
an HTTP status via the existing exception mapping, so error semantics are identical across faces.

- `AppbaseException` / `AppbaseRuntimeException` subtypes → their mapped status.
- Timeout / cancellation → 503, and the response is **not** written twice (a cancelled exchange must
  not attempt a body write).
- An error raised *after* the response has begun streaming cannot change the status. The stream is
  terminated and the failure logged; this is inherent to chunked responses and must be tested, not
  papered over.

---

## 8. Testing

| Layer | What is proven | How |
|---|---|---|
| `AppbaseServiceExchange` / chain | Filter order (authorization LAST), short-circuit on reject, context propagation across a scheduler hop | Unit, with a fake exchange |
| Health snapshot | `force=false` never runs checks; `force=true` does; unhealthy does NOT trigger a re-run; stale snapshot → unhealthy | Unit, Mockito, no infra |
| `NettyWebExchange` | Header/cookie/query extraction, status, JSON write | Unit against reactor-netty test support |
| Backpressure | A slow consumer back-pressures a fast `Flux`; heap does not grow | `StepVerifier` + bounded request |
| Event loop never blocks | **The central claim.** Assert that no ported endpoint executes blocking work on an event-loop thread | `BlockHound` |
| End-to-end | All 10 endpoints answer over real HTTP with the full filter chain | `HttpClient` against a real `ReactorNettyAppbaseWebServer` on an ephemeral port — **ungated, no infra**, like R2b's H2 test |

**BlockHound is the load-bearing test.** R1's entire claim is "the event loop never blocks";
asserting it by inspection is worthless. BlockHound instruments the JVM to throw when a blocking call
executes on a non-blocking thread, which turns the claim into a build failure if violated. It must be
wired for the reactor-netty module's tests, with the `boundedElastic` offload threads correctly
marked as blocking-permitted.

The end-to-end test needs no external infra (no container, no database), so like R2b's H2 test it
runs **ungated in CI**.

---

## 9. Success criteria

1. `ReactorNettyAppbaseWebServer` starts, serves all 10 endpoints on the same paths as the servlet
   face, and stops cleanly.
2. BlockHound proves no blocking call executes on an event-loop thread, for every endpoint.
3. The reactive filter chain applies all 7 filters in the servlet chain's order, authorization last.
4. `checkHealthSnapshot()` never blocks; `force=false` returns cached; the unhealthy-rerun storm is
   gone. Servlet face benefits identically.
5. A streaming `Flux` response honours downstream demand.
6. MDC, tracing span, and security context survive the scheduler hop.
7. Every Tier-1 offload carries a Javadoc note naming the milestone that removes it.
8. No change to `appbase-core`. No change to servlet/JAX-RS filters.

---

## 10. Non-goals

- The servlet `startAsync` reactive bridge → **P-033 clause (4)**.
- Extracting `appbase-bootstrap-servlet`, renaming `AppbaseWebServer` → **P-033 clauses (1)/(2)**.
- Porting the ~30 user-account/session servlets. Their stores go reactive in **R4**; porting them
  now would mean offloading all of them.
- Native reactive Lucene reads for Log/Exception → **R3**.
- A reactive `HealthCheckService` whose checkers use the R2a Aerospike/S3/ZooKeeper twins. Tempting
  (the twins exist), but `checkHealth()` is synchronous and runs every checker under a lock, so this
  is a health-service redesign → **R4**.
- TLS, HTTP/2, WebSocket.

---

## 11. Decomposition

Medium-big: new module + new public abstraction + a service-layer bugfix. Sub-issues, one PR per
task, into a feature branch off the epic.

| Task | Content |
|---|---|
| T1 | `AppbaseServiceExchange` / `AppbaseServiceFilter` / `AppbaseServiceFilterChain` in `appbase-service-core` + chain impl with ordering and context propagation |
| T2 | Health snapshot: `checkHealthSnapshot()`, honour `force`, kill the rerun storm, config + `snapshotAgeMillis` |
| T3 | Neutral reactive filters for metric, traceId, exceptionHandler; wire the 4 existing neutral logic interfaces |
| T4 | `appbase-bootstrap-reactor-netty` scaffold: module, `ReactorNettyAppbaseWebServer`, `NettyWebExchange`, config, Guice module |
| T5 | `AppbaseRouteRegistry` + the 4 natively non-blocking endpoints (health, metric, serviceRegistry, status) |
| T6 | The 6 offloaded endpoints (log, exception, configuration, core, jmx, mediator) with Tier-1 offload + Javadoc rationale |
| T7 | BlockHound wiring, end-to-end HTTP test, backpressure test, filter-order test; README + build-guide |

---

## 12. Open items for plan time

- Pin the exact `reactor-netty` patch version on the Reactor 2023.0.x BOM (matches `reactor-core`
  3.6.8) and **verify it does not force a Netty bump** beyond the pinned `4.1.111.Final`. Add to
  root `<dependencyManagement>` only.
- Confirm BlockHound's Java 25 support; if it cannot instrument, substitute an explicit
  event-loop-thread assertion in each handler test and record the downgrade in J-021.
- Decide whether `ReactorNettyAppbaseWebServer` gets a `--container reactor-netty` value in the
  `StackTestMain` selector, or whether that waits for P-033's transport selector. (`StackTestMain`
  is on `develop`, absent from this branch; P-008 is renaming the module to `appbase-examples`.)
