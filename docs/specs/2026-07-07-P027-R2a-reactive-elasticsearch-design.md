# P-027 R2a — Reactive Elasticsearch Twin — Design

**Status:** approved (brainstorm) · **Date:** 2026-07-07 · **Epic:** `feature/P-027-reactive-appbase` · **Spec family:** P-027 R2a reactive driver twins (7th twin: Redis, Cassandra, Kafka, Aerospike, Jasync, **Elasticsearch**).

## Goal

Add reactive + synchronous **service-acquired** accessor faces to `appbase-client-elasticsearch`, on the shared Appbase accessor spine, wrapping the existing high-level typed `ElasticsearchRestClient` — bridging its `asyncXxx` `CompletableFuture` operations to Project Reactor and giving every operation the spine's per-operation `Timer` / trace / exception-store integration. Keep the twin **uniform** with the six prior twins: same accessor topology, same `getReactiveAccessor()`/`getAccessor()` acquisition (incl. keyed variants), same config/exception patterns.

## Why Elasticsearch is different from the prior twins

Every prior twin wrapped a driver that an `AppbaseClientService<Driver, V>` handed out, where `integrationService.getClient()` returned the *rich* driver (`CqlSession`, jasync `Connection`, …) carrying all the operations. Elasticsearch has **two** client layers:

1. **Low-level** — `ElasticsearchLowLevelRestClientService extends AppbaseClientService<RestClient, ClusterIdValue>` hands out the raw `org.elasticsearch.client.RestClient` (no typed operations; `performRequest`/`performRequestAsync` over `Request`/`Response`).
2. **Typed** — `ElasticsearchRestClient` (interface, `extends Shutdownable`; impl `DefaultElasticsearchRestClient` injected with the low-level service) is a hand-built typed client with rich request/response POJOs. It **already exposes both faces**: sync (`search`/`multiSearch`/`addDocument`/`updateDocument`/`addOrUpdateDocument`/`deleteDocument`/`createIndex`/`existsIndex`/`deleteIndex`/`cancelTask`, throwing `IOException`) and async (`asyncSearch`/… → `CompletableFuture`, via `RestClient.performRequestAsync`).

So only the **reactive (Reactor) face** is missing, and none of the existing faces ride the shared accessor spine. The twin's job is therefore *add a reactive face + bring Elasticsearch onto the accessor spine* over the typed client — not *wrap a driver*.

## Decisions (approved)

- **A — Full parity.** Add BOTH `ReactiveElasticsearchAccessor` (`getReactiveAccessor()`) and sync `ElasticsearchAccessor` (`getAccessor()`), each on the shared spine, plus keyed variants. Uniform with all prior twins; both faces gain the spine `Timer`/trace/exception-store the bare typed client lacks. Accepted cost: the sync accessor's operations mirror the typed client's own sync methods, but now spine-instrumented.
- **B — Typed layer.** Getters live on `ElasticsearchRestClient` (+ `DefaultElasticsearchRestClient`); accessors borrow a lazy `Supplier<ElasticsearchRestClient>` (default = `() -> this`). The typed client is the Elasticsearch analog of "the client that carries the operations", exactly as `CqlSession` is for Cassandra. Rejected: mapping onto the low-level `AppbaseClientService` (would operate on raw `Request`/`Response`, duplicating the typed request/response mapping `DefaultElasticsearchRestClient` already does, and losing the typed API).
- **C — `Mono.fromFuture` bridge.** The reactive accessor calls `mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncSearch(req)))` per operation. Lazy per-subscription, so the reactive accessor takes the SAME lazy borrow as the sync one; no eager client build, no per-key cache of reactor clients.
- **D — Keyed parity via a bound-key view.** Unlike prior twins (keyed is free because getters sit on the `AppbaseClientService`, whose `getClientIntegrationService(key)` is native), Elasticsearch getters sit at the typed layer, and the typed client is currently hardwired to the default key: every operation resolves its `RestClient` through the no-arg `getClientIntegrationService().getClient()` (in the two private executors `sendSyncRequest`/`sendRequest`). To keep the **same external keyed contract** without a silent-default lie and without doubling the operation surface:
  - Add nullable `private final AppbaseClientKey<ClusterIdValue> boundKey` to `DefaultElasticsearchRestClient`. The existing `@Inject` ctor sets it `null`; a new private ctor `(logSource, lowLevelService, boundKey)` builds key-bound views.
  - The two private executors resolve `boundKey == null ? svc.getClientIntegrationService() : svc.getClientIntegrationService(boundKey)`. **Null path byte-identical to today** — zero behavior change for the default cluster.
  - Private helper `keyBound(key)` → `new DefaultElasticsearchRestClient(lowLevelService, key)` (cheap: holds logger + service + key; the low-level service already pools an integration service per `ClusterIdValue`).
  - The 10 typed operations stay untouched — they become key-aware for free because the bound key flows through the two executors. A keyed accessor calls the same `es.asyncSearch(req)`; it now resolves the bound cluster.
- **E — Spine Timers only; no driver-metrics task.** Elasticsearch's `RestClient` fully encapsulates the Apache `CloseableHttpAsyncClient`; `PoolStats` are only reachable by injecting and retaining our own `PoolingNHttpClientConnectionManager` in `HttpClientConfigCallbackImpl` (currently only sets `setConnectionManagerShared`), which takes over connection-manager ownership and risks changing pooling/IOReactor behavior. Rejected as invasive. Instead rely on the per-operation Micrometer `Timer` every accessor operation records on the spine — real op-level metrics (latency/count/error, tagged per op) Elasticsearch lacks today. No `driverMetricsEnabled` gate, no `HttpClientConfigCallbackImpl` change.
- **F — Non-greenfield; no driver bump.** Elasticsearch 8.5 is already a dependency; nothing merges flat into the epic first (unlike Cassandra/Aerospike driver bumps). No existing reactive accessor/exception to preserve.

## Components

New (typed package `…service.elasticsearch.restclient.api`, reactive sub-package `.reactive`):
- `ElasticsearchAccessorException extends AppbaseClientException` — ctors `(Throwable)` / `(String, Throwable)`; static `map(Throwable, Function<Throwable,Throwable> fallback)` unwraps `CompletionException`/`ExecutionException` → cause, maps `org.elasticsearch.client.ResponseException` (and `IOException`) → `ElasticsearchAccessorException`, else `fallback`. Mirrors `CassandraAccessorException`/`JasyncAccessorException`.
- `reactive/ReactiveElasticsearchAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<ElasticsearchRestClient> borrow)`. Ten `Mono<XxxResponse>` operations, each `mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncXxx(req)))`; one convenience `Flux<SearchHits> searchHits(op, SearchRequest)` = `search(op, req).flatMapMany(r -> Flux.fromIterable(hits(r)))` (mirrors jasync `queryRows`). `mapError` = `ElasticsearchAccessorException.map(t, super::mapError)`; `isNotable` = `true`.
- `ElasticsearchAccessor extends AbstractAppbaseAccessor` (sync) — ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<ElasticsearchRestClient> borrow)` → `super(appbaseCore, config.isMetricsEnabled(), config.isTraceEnabled(), config.isExceptionStoreEnabled())`. Ten operations, each `execute(op, () -> borrow.get().search(req))` (the typed sync methods already throw `IOException`; no `.get()` unwrap needed). Same `mapError`/`isNotable`.

Modified:
- `ElasticsearchRestClient` (interface) + `DefaultElasticsearchRestClient` — add 4 getters (`getReactiveAccessor()` + keyed, `getAccessor()` + keyed); add `boundKey` field + private ctor + `keyBound(key)` helper; route the two private executors through `boundKey`.
- `ElasticsearchLowLevelRestClientServiceConfiguration` (interface) + `DefaultElasticsearchLowLevelRestClientServiceConfiguration` — add `getReactiveOperationConfiguration()` (default non-null `DefaultReactiveOperationConfiguration`), mirroring `DefaultCassandraClientServiceConfiguration`. The typed client reads it (and `isMetricsEnabled`/`isTraceEnabled`/`isExceptionStoreEnabled`) via `lowLevelService.getConfiguration()`.
- Module `pom.xml` — version-less `io.projectreactor:reactor-core` (if not already resolved) + `reactor-test` (test); Export-Package `…restclient.api.reactive`.

## Data / error flow

- **Reactive:** subscribe → `borrow.get()` (typed client, default or key-bound) → `asyncSearch(req)` (`CompletableFuture` from `performRequestAsync`) → `Mono.fromFuture` → Reactor **unwraps** any `CompletionException` so `mapError` sees the raw `ResponseException`/`IOException`. Spine records the op `Timer`.
- **Sync:** `execute(op, …)` → `borrow.get().search(req)` throws `IOException` directly → `mapError` maps it. Spine records the op `Timer`.
- Shared `ElasticsearchAccessorException.map` handles both depths (unwrapped reactive cause + directly-thrown sync `IOException`).

## Testing

- Unit: `ElasticsearchAccessorExceptionTest` (bare `ResponseException` maps; `CompletionException(cause=ResponseException)` unwraps+maps; non-ES throwable → fallback); `ReactiveElasticsearchAccessorTest` (mock typed client, stub `asyncSearch` → completed/failed future, `StepVerifier`, `Timer` recorded); `ElasticsearchAccessorTest` (sync happy path + `IOException` → `ElasticsearchAccessorException`, `Timer`); config round-trip test for `reactiveOperationConfiguration`; acquisition tests (`getReactiveAccessor()`/`getAccessor()` non-null + config passthrough; **keyed** getters route to `getClientIntegrationService(key)` — mockable via the bound-key view; default path unchanged).
- Gated IT: `reactive/ReactiveElasticsearchAccessorIT` (reactive + sync round-trip: `createIndex` → `addDocument` → `search` asserts the doc → `deleteIndex`), `@EnabledIfSystemProperty(named = "ElasticsearchClientServiceTests.enabled", matches = "true")`. Recorded in **J-018 clause (i)** per the track-gated-tests rule.
- Docs: `build-guide.md` "Reactive Elasticsearch" section.

## Tasks / PRs (max 3 tasks per PR)

Twin branch `feature/P-027-R2a-elasticsearch` off the epic HEAD. New twin sub-issue; post each PR link in sequence. Roll up to the epic when both PRs merge (close the sub-issue manually — GitHub auto-close only fires on merge to `develop`).

**PR 1** — branch `feature/P-027-R2a-elasticsearch-t1t3-reactive`:
- **T1** `reactiveOperationConfiguration` on `ElasticsearchLowLevelRestClientServiceConfiguration` (+ Default, + round-trip test).
- **T2** `ElasticsearchAccessorException` + `ReactiveElasticsearchAccessor` (10 `Mono` ops + `Flux searchHits`; pom reactor-core/reactor-test + Export-Package; + tests).
- **T3** `getReactiveAccessor()` + keyed acquisition on `ElasticsearchRestClient`/`DefaultElasticsearchRestClient`, incl. the `boundKey` plumbing (field, private ctor, `keyBound`, executor routing) (+ tests). Then open PR 1; STOP for merge.

**PR 2** — branch `feature/P-027-R2a-elasticsearch-t4t5-sync-it` (off the twin branch after PR 1 merges):
- **T4** sync `ElasticsearchAccessor` + `getAccessor()` + keyed acquisition (reuses `boundKey` from T3) (+ tests).
- **T5** gated `ReactiveElasticsearchAccessorIT` + `build-guide.md` section + J-018 clause (i).

## Out of scope / follow-ups

- Key-aware *typed operations* on `ElasticsearchRestClient` (e.g. `asyncSearch(key, req)`): unnecessary — the bound-key view (Decision D) already delivers keyed accessors. Only needed if a caller wants per-call multi-cluster targeting on the typed client directly.
- Connection-pool `PoolStats` gauges (Decision E) — revisit only if op-level `Timer`s prove insufficient and the connection-manager-ownership risk is accepted.

## Global constraints (inherited)

Java 25, Reactor 3.6.8; every Java file gets the Apache header + Javadoc on public methods + `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`; no FQCNs (imports only); dependency versions only in root `<dependencyManagement>`; non-obvious decisions documented at the code site (the `Mono.fromFuture` bridge, the exception-unwrap asymmetry, the `boundKey` divergence, why no driver metrics). Build via `mvn -q -pl appbase-client/appbase-client-elasticsearch -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`; ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): file and plugin deleted; no post-build cleanup needed.** after each build. Learnings → `appbase-java/LEARNING.md` at the end.
