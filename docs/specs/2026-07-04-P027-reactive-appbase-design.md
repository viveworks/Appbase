# P-027 — Reactive / Non-Blocking Appbase (Umbrella Spec)

**Status:** Design / brainstorm output — umbrella spec. Each sub-project (R0–R5)
gets its own spec + plan.
**Date:** 2026-07-04
**Priority:** current. **OAuth stays in P-020** — its own epic, not owned here.
P-020 will build its dual sync + reactive token validator on top of R0's reactive
SPI; P-027 just makes that SPI available.
**Companion brainstorm transcript:** `~/Temporary/appbase-reactive-brainstorm.md`

---

## 1. Vision & positioning

Make Appbase **dual-mode: synchronous AND true non-blocking / reactive** — but
only *where it matters*: network I/O and file I/O. A single unified
managed-service + YAML/SPI model exposes both faces, opt-in per I/O boundary,
without forcing an app to pick a stack.

Positioning (be honest, don't over-claim):
- Appbase is closest to **Spring Boot in shape** (opinionated, embedded pluggable
  containers, YAML-externalized config, one-shot bootstrap, batteries-included
  integrations) but on different foundations: **Guice** (not Spring beans),
  **JAX-RS/Jersey** (not Spring MVC), **YAML + SPI/factories** (not annotation
  scanning).
- Reactive Java is a crowded field (WebFlux, Quarkus/Mutiny, Vert.x, Micronaut,
  Helidon, Ktor). Appbase is **not "unique"** for being reactive — that's table
  stakes. The distinctive combination is: *one explicit, config-driven,
  Guice-based service framework that runs sync **and** true non-blocking on a
  single unified model, across pluggable runtimes (3 servlet containers + a
  reactive one), without forcing a stack choice.*

The same reactive treatment extends to **appbase-cloud**, **appbase-ai**, and
**appbase-saas**; the R0 core reactive SPI must accommodate all of them.

## 2. Non-negotiable principle: async ≠ non-blocking

Wrapping a blocking call in `Mono.fromCallable().subscribeOn(boundedElastic())`
yields a reactive-*looking* API that still burns one thread per in-flight call —
no scalability win. **The target is Tier-2 true non-blocking**, via genuinely
non-blocking drivers. The async-facade (Tier-1, scheduler-offload of a blocking
driver) is permitted **only as an explicitly-labeled fallback** where no reactive
driver exists — never sold as non-blocking.

## 3. Scope — the I/O catalog

Audited every service/store/accessor/client in `appbase-core`, `appbase-client`,
`appbase-service`, `appbase-bootstrap`. **~29 reactive-candidate interfaces**
(~27 network I/O + ~5–7 file I/O). Only 4 have any async surface today (all
`CompletableFuture`, zero Reactor): `LogStore`/`LogAccessor`,
`ExceptionStore`/`ExceptionAccessor`, `SearchIndexAccessor`, `ObjectStoreAccessor`.
Everything else — every client-service facade and all three service-layer stores
(`UserSessionStore`, `UserAccountStore`, `SupportMessageStore`) plus
`FirebaseAuthService` — is fully synchronous. No OAuth/JWKS token store exists;
identity/token verification runs through the Firebase path.

**Driver-support tiers** (drives module & phasing decisions):

| Tier | Backends | Reactive dependency | Module impact |
|---|---|---|---|
| 1 — reactive API in the **same jar** | Redis/Lettuce, Cassandra/DataStax, Elasticsearch, S3+AWS SDK v2, Aerospike, Firebase, Vitess gRPC, Jasync | none (same driver) | in-module |
| 2 — reactive **wrapper on the same driver** | Kafka (`reactor-kafka` on `kafka-clients`), ZooKeeper (`curator-x-async` on Curator) | small add-on jar, same underlying driver | in-module |
| 3 — genuinely **different driver stack** | JDBC / JDBi / JPA → **R2DBC** / Hibernate-Reactive | whole new connection stack | **separate module** |

Blocking-only, no reactive driver (facade-offload or defer): SMTP (Jakarta Mail),
ClamAV (raw socket), Druid (Avatica JDBC/HTTP), Spark, and the Lucene file stores
+ filesystem config scanners.

**In scope:** network + file I/O reactive twins where a real reactive driver
exists (Tiers 1–2 first, Tier 3 as a harder track). **Out of scope:** reactive
for CPU-only/in-memory services (authz, id, password, registry, JMX, in-memory
stores) — no benefit. Reactive-everything is explicitly a non-goal.

## 4. Architecture — one organizing principle

**Separate "having a connection to X" (lifecycle + config + driver — always
shared) from "how you invoke an operation on it" (sync value vs `Mono`/`Flux` —
the only fork).** Every reactive twin is a thin *operation-surface* addition on
top of an already-shared spine. No duplicated lifecycle, no parallel config
trees.

```
AppbaseManagedService              (core lifecycle: init/start/stop)     ── shared
  AppbaseClientIntegrationService  (connect/disconnect a driver)         ── shared, reused verbatim
  AppbaseClientService<Driver,KV>  (holds/hands out native driver)       ── shared

AbstractAccessor<Driver>           (holds driver + config)               ── shared base
  ├─ AbstractSyncAccessor      → returns T / List<T>                     ── appbase-client-core (sync sub-package)
  └─ AbstractReactiveAccessor  → returns Mono<T> / Flux<T>               ── appbase-client-core (reactive sub-package)
```

The same shared→fork shape recurs at every tier:
- **Config:** `AppbaseClientServiceConfiguration` (shared: host/port/pool/timeouts)
  ← `ReactiveClientServiceConfiguration` (adds scheduler/backpressure/retry only).
- **Stores:** `AbstractStore` (lifecycle/config/driver) → `SyncStore` (`read : Entity`)
  vs `ReactiveStore` (`read : Mono<Entity>`).
- **Web:** `AppbaseWebFilter` holds shared filter *logic*; only the runtime
  *adapter* forks (servlet vs reactor-netty).
- **Orchestration services** (LogService, ExceptionService) mostly *delegate*, so
  they can expose both faces by delegating to the sync or reactive accessor
  without their own fork.

**Reactive-ness is a property of the operation surface, orthogonal to being a
managed service with a connection.** Build on the **existing** reactive scaffold
in `appbase-core` (`service.reactive.reactor.ReactorService` + the pluggable
`SchedulerFactory` SPI with 7 scheduler variants) — R0 formalizes and extends it,
it does not start from scratch.

## 5. Module topology (LOCKED)

**Rule:** *abstraction goes in packages; a module exists only to isolate a
third-party dependency. A new module is justified by a new driver-stack JAR,
never by a new interface.* (This is exactly why the existing fine-grained sync
modules exist — each isolates a driver.)

- Reactive **base/SPI → sub-packages, never modules** (reactor is already a
  transitive dep everywhere via `appbase-core`; `appbase-client-core` already
  references `DefaultReactorServiceConfiguration`). So **no
  `appbase-client-reactor-core` module** — the reactive accessor base is a
  sub-package of `appbase-client-core`. Every client is "prepared to be reactive"
  by construction.
- Reactive **impl for a backend → same module as its sync impl** for Tiers 1–2
  (same driver, or a small wrapper on it).
- **Separate `-reactive` module → only Tier 3** (R2DBC / Hibernate-Reactive — a
  different driver stack) and the new reactive **runtime**.

| Layer | New modules | Justification |
|---|---|---|
| `appbase-core` | **0** | reactive SPI = additive package (already seeded) |
| `appbase-client` | **~1–2** | R2DBC / SQL family only (Tier 3) |
| `appbase-service` | **~1–2** | R2DBC store twins (`UserAccountStore`, `SupportMessageStore`) |
| `appbase-bootstrap` | **1** | `appbase-bootstrap-reactor-netty` (new runtime JAR); neutral web-filter abstraction folds into existing `appbase-bootstrap-core` |

**~3–5 new modules total**, every one from a genuinely new driver/runtime jar.
`appbase-core` stays a single module (not split).

## 6. Configurability (first-class requirement)

Configurability is one of Appbase's biggest values. **Two mandates, both
binding on every reactive knob added:**

1. **Everything configurable — YAML *and* programmatic.** Every reactive setting
   follows the established Appbase pattern, no exceptions:
   `XxxConfiguration` interface + `DefaultXxxConfiguration` (immutable, Jackson
   `@JsonDeserialize(builder=…)` builder, `DEFAULT_*` constants, a
   `DEFAULT_YAML_NODE_NAME`) + `DefaultXxxConfigurationValidator`; pluggable
   variants get a `Factory` SPI (mirroring `SchedulerFactory`'s 7 variants).
2. **Sensible defaults — zero-config must just work.** Every knob has a
   `DEFAULT_*`; a user configuring *nothing* gets a working, sane reactive setup.
   Users are never *required* to specify a value — but if they want to, **every**
   value is overridable via YAML or the builder. Convention over configuration,
   with total override available.

**Enumerated reactive config surface** (each = full Config+Default+Validator,
each with a sensible default; pluggable ones also get a Factory):

- **Global enable** — `reactive.enabled` (default: available-but-opt-in per
  service; the existing `ReactorServiceConfiguration.enabled` default is `true`).
- **Schedulers** (already implemented — reuse): choice of BoundedElastic /
  Parallel / Single / Immediate / Elastic / ExecutorService /
  MediatorExecutorService; per-scheduler thread count, thread-name prefix, queue
  bounds, TTL. Default: MediatorExecutorService (shares the mediator/disruptor
  executor).
- **Backpressure** — strategy (BUFFER / DROP / LATEST / ERROR), buffer size,
  overflow behavior. Default: BUFFER with a bounded default size.
- **Timeouts** — per-operation reactive timeout (`Mono.timeout`), default derived
  from the existing sync client timeout config so sync/reactive stay consistent.
- **Retry / backoff** — enabled, max attempts, base backoff, max backoff, jitter,
  retryable-error predicate. Default: a conservative bounded exponential backoff.
- **Error handling / fallback** — on-error strategy (propagate / fallback-value /
  fallback-publisher), error mapping to Appbase exception types. Default:
  propagate with Appbase error mapping.
- **Reactor context propagation** — enable MDC/tracing/context propagation across
  the reactive chain (integrate with the existing tracer/log services). Default:
  on (so tracing/log correlation survives the async hop).
- **Reactive web runtime (`reactor-netty`)** — event-loop thread count, worker
  selection, connection/read/write idle timeouts, max in-flight, HTTP/2 toggle,
  TLS (reuse existing `SslKeyStore` config), graceful-shutdown drain. Sensible
  server defaults; fully overridable.
- **Web layer selection** — which runtime serves (Jetty/Tomcat/Undertow servlet,
  with or without `startAsync`, or reactor-netty), consistent with the existing
  pluggable-container config. Default: current servlet container, sync.
- **Per-driver reactive options** — e.g. `reactor-kafka` `SenderOptions`/
  `ReceiverOptions`, `curator-x-async` retry policy, R2DBC pool
  (initial/max/maxIdle/maxLifeTime/acquireTimeout), Lettuce/DataStax reactive
  publisher settings. Each surfaced through that backend's existing config class,
  extended with reactive-only fields, each defaulted.
- **File-I/O offload** — the bounded scheduler + queue used for Lucene / file
  reactive boundaries (see §7); pool size, queue bound, rejection policy.
  Defaults sized conservatively.

Anywhere the design introduces a value a user might conceivably want to tune, it
**must** be surfaced as a defaulted, overridable config field. During
implementation, actively *look* for hidden hard-coded constants and lift them
into config.

## 7. Error handling, backpressure, file I/O

- **Error model:** reactive operators map driver errors to the same Appbase
  exception hierarchy the sync path uses, so error semantics are identical across
  faces. Configurable on-error strategy (§6).
- **Backpressure:** honored end-to-end; strategy + bounds configurable. Reactive
  drivers (Lettuce, DataStax reactive, reactor-kafka, R2DBC) provide real demand
  signaling; Tier-1 facades document that backpressure is bounded by the offload
  scheduler, not the source.
- **File I/O reality:** the JVM has no true async file I/O
  (`AsynchronousFileChannel` is thread-pool-backed). Reactive file boundaries
  (Lucene log/exception stores, config scan) use **bounded-scheduler offload**,
  clearly labeled Tier-1. **Keep the disruptor for log/event handoff** (it's the
  mediator + logging spine and already excellent there; default wait strategy is
  `SleepingWaitStrategy` — low-CPU, no busy-spin); do **not** force disruptor onto
  every file boundary.

## 8. Testing strategy

- Each reactive twin: `StepVerifier`-based unit tests asserting emission,
  completion, error mapping, backpressure, and timeout behavior.
- Contract parity tests: sync and reactive faces of the same operation return
  equivalent results against the same backing store.
- Config tests: every new `DefaultXxxConfiguration` gets round-trip YAML
  (de)serialization + validator tests + a **zero-config default** test proving it
  works with nothing specified.
- Integration: real reactive drivers under the existing docker-per-backend IT
  lifecycle; the P-008/R5 showcase exercises the full non-blocking chain
  end-to-end. Include a **Vitess reactive-SQL IT via `r2dbc-mariadb`/`r2dbc-mysql`**
  (Vitess speaks the MySQL/MariaDB wire protocol) in R2b.

## 9. Decomposition & phasing

| # | Sub-project | Own spec | Phase |
|---|---|---|---|
| **R0** | Reactive core abstraction + SPI (formalize/extend existing `ReactorService`; accessor/store/service reactive bases; config surface §6) | yes | **P1** |
| **R1** | Reactive web layer (neutral `AppbaseWebFilter` + servlet `startAsync` adapter + `appbase-bootstrap-reactor-netty`) | yes | P2 |
| **R2a** | Tier-1/2 client twins (Redis, Cassandra, Kafka, ES, S3, Aerospike, ZooKeeper, Vitess, Firebase, Jasync) | yes | P2→P3 |
| **R2b** | Tier-3 SQL: **R2DBC only** (new `appbase-client-r2dbc`, native `r2dbc-pool` ConnectionPool; jdbc/jdbi reactive counterpart); **Vitess-over-SQL via `r2dbc-mariadb`/`r2dbc-mysql`** (Vitess speaks MySQL/MariaDB protocol) with ITs — thorny, separate. **SCOPE DECISION 2026-07-08: Hibernate-Reactive (JPA) DEFERRED out of R2b → R4** (see R4 note). | yes | P4 |
| **R3** | Reactive core file-I/O (Lucene stores, config scan; offload); **flip Lucene indexing default → `enabled=false`** | yes | P3 |
| **R4** | Service-layer store twins (UserSession → Aerospike/R2a; UserAccount/SupportMessage → R2DBC/R2b). **Hibernate-Reactive (JPA) lands HERE — deferred from R2b (2026-07-08):** the reactive JPA face (`appbase-client-jpa-reactive`, Hibernate Reactive Mutiny/Stage `SessionFactory` over Vert.x-SQL — a different stack from R2DBC) is a thornier ORM effort (session lifecycle, entity mapping) and naturally pairs with the reactive persistence R4's store twins need; JPA stays sync-only until then. **⚠️ As-built (R4 internal work item 390 close-out, 2026-08-12):** R4 delivered the reactive UserAccount path as an **R2DBC-backed store** (`R2dbcReactiveUserAccountStore`, raw SQL mirroring the blocking JDBI twin) plus the Aerospike session twin. **Hibernate-Reactive was NOT built** — the "reactive JPA" reading resolved to the R2DBC path; the tracked release decision's Unknowns §1 / Questions Q1 records that outcome. A Hibernate-Reactive face remains unshipped. | yes | follows driver |
| **R5** | Reactive showcase — non-blocking CRUD chain (reactor-netty → reactive filter → reactive Redis/Cassandra CRUD → reactor-kafka). **SCOPE CHANGE 2026-07-09:** lands inside the EXISTING `appbase-stacktest`, NOT `appbase-examples`. The `appbase-stacktest` → `appbase-examples` rename/overhaul is deferred to 2.0.0, so R5 must not depend on it. | in P-008 | after P2 |

OAuth dual-mode (sync + reactive JWKS) validation is **not** a P-027 deliverable —
it lives in **P-020** (the OAuth epic), which consumes R0's reactive SPI.

**Phasing = prove-then-scale:** P1 R0 → P2 thin vertical slice (R1 + Redis + minimal
R5 showcase, proven non-blocking end-to-end on one path) → P3 fan out
R2a/R3/R4-native → P4 R2b (R2DBC), stragglers.

## 10. Resolved decisions

- **Lucene log indexing default → `enabled=false`.** It's file I/O on the hot log
  path; nothing should be slowed down by default. Users who want indexed/
  searchable logs opt in explicitly. (Flip the current `DEFAULT_ENABLED=true` in
  `DefaultLuceneLogIndexConfiguration`; applied in R3.)
- **JDBi / JPA reactive → use the best option available per driver.** It's known
  to be hard: JDBi is JDBC-only (no native reactive — reactive path goes through
  the R2DBC track or a labeled offload facade); JPA → Hibernate Reactive (a
  Vert.x-SQL stack, not R2DBC). Pick the best-available per driver in R2b rather
  than forcing one mechanism.
- **Tier-2 transitive dep → accepted.** Fine to pull the small reactive wrapper
  (`reactor-kafka` / `curator-x-async`) into the existing Kafka/ZooKeeper modules;
  keeping them in-module (one fewer module) outweighs the tiny transitive cost.
- **Reactive parity for appbase-cloud, appbase-ai, appbase-saas → yes.** R0's SPI
  must serve all three. S3 async handled in appbase-cloud (AWS SDK v2 async).
- **Vitess reactive SQL via R2DBC.** Vitess speaks the **MariaDB/MySQL wire
  protocol**, so `r2dbc-mariadb` (or `r2dbc-mysql`) works for reactive
  Vitess-over-SQL access (distinct from the Tier-1 `appbase-client-vitess` gRPC
  path). Cover this explicitly with integration tests against Vitess using the
  R2DBC MariaDB/MySQL driver in R2b.

## 11. Non-goals (YAGNI)

- No reactive for CPU-only/in-memory services.
- No async-facade masquerading as non-blocking.
- No rewrite of the sync surface — reactive is purely additive; sync stays
  first-class and backward-compatible.
- No new module merely to hold an interface.

## 12. Success criteria

- Sync API unchanged and fully backward-compatible.
- A user can run a **non-blocking** CRUD chain end-to-end (reactive web → reactive
  client CRUD → reactive Kafka) with **zero config** and sane defaults, and tune
  **every** knob via YAML or builder if desired.
- Reactive twins exist only where a real reactive driver backs them; Tier-1
  facades are labeled as such.
- `appbase-core` remains a single module; ~3–5 new modules total, each isolating
  a genuine new driver/runtime dependency.
