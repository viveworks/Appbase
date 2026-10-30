# P-027 R2a — Reactive Elasticsearch Twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reactive + synchronous **service-acquired** accessor faces to `appbase-client-elasticsearch`, on the shared Appbase accessor spine, wrapping the existing typed `ElasticsearchRestClient` (bridging its `asyncXxx` `CompletableFuture` operations to Reactor and giving every operation the spine's `Timer`/trace/exception-store), with keyed parity preserved via a bound-key view of `DefaultElasticsearchRestClient`.

**Architecture:** In-module (`appbase-client-elasticsearch`), **non-greenfield**. Elasticsearch already exposes sync (`search()…` throws `IOException`) + async (`asyncSearch()…` → `CompletableFuture`) on the typed `ElasticsearchRestClient`; only the reactive face is missing, and no face rides the spine. The twin adds `ReactiveElasticsearchAccessor` (`Mono.fromFuture` over `asyncXxx`), a spine sync `ElasticsearchAccessor`, `ElasticsearchAccessorException`, and 4 acquisition getters on `ElasticsearchRestClient`. Getters sit at the **typed layer** (not the low-level `AppbaseClientService`), so keyed variants thread the key through a nullable `boundKey` on `DefaultElasticsearchRestClient` that flows to its two private request executors. No driver-metrics task (Elasticsearch's `RestClient` encapsulates the Apache `HttpAsyncClient`; the spine's per-op `Timer` is the metric). No driver bump.

**Tech Stack:** Java 25, Project Reactor 3.6.8, Elasticsearch 8.5 low-level `RestClient` (Apache `HttpAsyncClient`), Micrometer (spine `Timer`), Guice, Maven, JUnit 5 + Mockito + `reactor-test` StepVerifier.

**Reference twin (mirror exactly):** merged Cassandra — `appbase-client-cassandra/.../reactive/ReactiveCassandraAccessor.java`, `CassandraAccessor.java`, `CassandraAccessorException.java`, `DefaultCassandraClientService.getReactiveAccessor`/`getAccessor` (+ keyed), `DefaultCassandraClientServiceConfiguration` reactive-config field. Jasync for the `CompletableFuture`→`Mono.fromFuture` bridge and the `CompletionException`/`ExecutionException` unwrap.

## Global Constraints

- **Branch/PR (max 3 tasks per PR):** twin branch `feature/P-027-R2a-elasticsearch` off the reactive epic (`feature/P-027-reactive-appbase`) HEAD. **PR 1** = Tasks 1–3 on `feature/P-027-R2a-elasticsearch-t1t3-reactive`; **PR 2** = Tasks 4–5 on `feature/P-027-R2a-elasticsearch-t4t5-sync-it`. Both target the twin branch. When both merge, roll up `feature/P-027-R2a-elasticsearch` → the epic (close the twin sub-issue manually — GitHub auto-close only fires on merge to `develop`). No driver-bump prerequisite, so nothing merges flat into the epic first. Post each PR link to the twin sub-issue in sequence.
- **Every Java file:** Apache license header (copy verbatim from a sibling in the module), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`. No fully-qualified class names in code (imports only) — sole exception: class-name string constants.
- **Decisions documented in code:** every non-obvious choice gets a Javadoc/inline rationale at the code site (the `CompletableFuture`→`Mono.fromFuture` bridge, the `CompletionException`/`ExecutionException` unwrap, the `boundKey` divergence + why the null path is unchanged, why there is no driver-metrics task).
- **Dependency versions ONLY in root `pom.xml` `<dependencyManagement>`**; module poms are version-less. (`reactor`/`reactor-test` are already managed in root from R0.)
- **Build command** (module + deps; use `install`, NOT `test` — bnd Export-Package on `.reactive` fails under `test`):
  `cd /Users/nav/Viveworks/Technology/Appbase/appbase-java && mvn -q -pl appbase-client/appbase-client-elasticsearch -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`
  `-Dsurefire.failIfNoSpecifiedTests=false` is MANDATORY (else `-am` aborts at appbase-core). Scope a single class with `-Dtest=<Class>` (still `install`).
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.** (regenerated; never commit).
- **Key type:** the low-level service is `AppbaseClientService<RestClient, ClusterIdValue>` — keyed accessors use `AppbaseClientKey<ClusterIdValue>` (like Cassandra).
- **Gated tests → J-018.** **Learnings → `appbase-java/LEARNING.md`** at the end.

## Established facts (verified against the codebase)

- `AbstractReactiveAccessor(AppbaseCore, ReactorService, ReactiveOperationConfiguration)`; protected `<T> Mono<T> mono(String op, Supplier<Mono<T>> source)` and `<T> Flux<T> flux(String op, Supplier<Flux<T>> source)`; overridable `mapError`/`isNotable`.
- `AbstractAppbaseAccessor(AppbaseCore, boolean metricsEnabled, boolean traceEnabled, boolean exceptionStoreEnabled)`; protected `execute(String op, Callable<T>)` throwing `Exception`.
- `ReactiveOperationConfiguration` carries `isMetricsEnabled()`, `isTraceEnabled()`, `isExceptionStoreEnabled()`, `getTimeout()`. Both accessors read their flags from it (the sync accessor's `super(...)` pulls the three booleans from the same config object — mirror `CassandraAccessor`).
- `DefaultReactiveOperationConfiguration` + `ReactiveOperationConfiguration` live in `com.viveworks.appbase.client.core.reactive`.
- `DefaultElasticsearchRestClient` **already injects `AppbaseCore`** (ctor arg, currently used only for the logger) — store it as a field for accessor construction (`appbaseCore.getReactorService()`).
- `AppbaseService.getConfiguration()` is generic `<C extends AppbaseConfiguration> C getConfiguration()`; read the reactive config via `elasticsearchLowLevelRestClientService.<ElasticsearchLowLevelRestClientServiceConfiguration>getConfiguration().getReactiveOperationConfiguration()` (LHS-inferred).
- The two private request executors are `DefaultElasticsearchRestClient.sendSyncRequest(Request)` (line ~762) and `sendRequest(Request, CompletableFuture<T>, Function<Response,T>)` (line ~769); both currently resolve `elasticsearchLowLevelRestClientService.getClientIntegrationService().getClient()` (default key). These are the only two borrow sites.
- Typed operations (10): `search`, `multiSearch`, `addDocument`, `updateDocument`, `addOrUpdateDocument`, `deleteDocument`, `createIndex`, `existsIndex`, `deleteIndex`, `cancelTask` — each has a sync (`throws IOException`) and an `asyncXxx` (`CompletableFuture`) form.
- Hits nav for the `Flux` convenience: `SearchResponse.getSearchResponseBody().getHits().getHits()` → `List<Hits>` (element type `Hits`).
- IT gate: `@EnabledIfSystemProperty(named = "ElasticsearchClientServiceTests.enabled", matches = "true")` (matches existing ES tests; env `ELASTICSEARCH_CLIENT_SERVICE_TESTS_ENABLED`).

---

## File Structure

**Elasticsearch module (`.../restclient/`):**
- Modify `lowlevel/configuration/ElasticsearchLowLevelRestClientServiceConfiguration.java` (interface) + `.../DefaultElasticsearchLowLevelRestClientServiceConfiguration.java` — `reactiveOperationConfiguration`.
- Create `api/ElasticsearchAccessorException.java`.
- Create `api/reactive/ReactiveElasticsearchAccessor.java`.
- Create `api/ElasticsearchAccessor.java` (sync).
- Modify `api/ElasticsearchRestClient.java` (interface) + `api/DefaultElasticsearchRestClient.java` — 4 getters, `appbaseCore` field, `boundKey` field + private ctor + `keyBound(key)` helper, route both executors through `boundKey`.
- Module `pom.xml` — version-less `reactor-core` (if not transitively present) + `reactor-test` (test); Export-Package `...restclient.api.reactive`.

**Tests/IT + docs:** unit tests per accessor + acquisition + exception map + config; gated `ReactiveElasticsearchAccessorIT`; `build-guide.md` "Reactive Elasticsearch" section; J-018 clause (i).

---

## PR 1 — branch `feature/P-027-R2a-elasticsearch-t1t3-reactive` (Tasks 1–3)

### Task 1: `reactiveOperationConfiguration` on the low-level client-service config

**Files:**
- Modify: `lowlevel/configuration/ElasticsearchLowLevelRestClientServiceConfiguration.java` (interface getter) + `lowlevel/configuration/DefaultElasticsearchLowLevelRestClientServiceConfiguration.java` (field/Builder/getter/equals/hashCode/toString).
- Test: `lowlevel/configuration/DefaultElasticsearchLowLevelRestClientServiceConfigurationTest.java`.

**Interfaces:**
- Produces: `DefaultReactiveOperationConfiguration getReactiveOperationConfiguration()` returning a non-null value (default `DefaultReactiveOperationConfiguration.builder().build()`), mirroring `DefaultCassandraClientServiceConfiguration` exactly (field, `DEFAULT_REACTIVE_OPERATION_CONFIGURATION`, ctor-from-builder with `AppbaseUtil.notNull`, interface + impl getter, Builder setter defaulting to the constant, equals/hashCode/toString).

- [ ] **Step 1: Write the failing test.** In `DefaultElasticsearchLowLevelRestClientServiceConfigurationTest`, assert the getter defaults non-null with `metricsEnabled()==true`, and a Jackson YAML round-trip preserves an overridden `metricsEnabled(false)`. Mirror `DefaultCassandraClientServiceConfigurationTest`.
- [ ] **Step 2: Run — verify it FAILS.** `mvn … -pl appbase-client/appbase-client-elasticsearch -am … -Dtest=DefaultElasticsearchLowLevelRestClientServiceConfigurationTest install`. Expected: compile failure (`getReactiveOperationConfiguration` undefined) or assertion failure.
- [ ] **Step 3: Add the interface getter.** In `ElasticsearchLowLevelRestClientServiceConfiguration`:
  ```java
  DefaultReactiveOperationConfiguration getReactiveOperationConfiguration();
  ```
- [ ] **Step 4: Add the field to the Default impl.** Mirror `DefaultCassandraClientServiceConfiguration`: `private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;`, a `DEFAULT_REACTIVE_OPERATION_CONFIGURATION` constant, ctor-from-builder assignment (`AppbaseUtil.notNull(builder.reactiveOperationConfiguration, …)`), the getter, a Builder setter defaulting to the constant (inside `// CheckStyle: Builder start/end` markers), and `reactiveOperationConfiguration` in equals/hashCode/toString. **Confirm `Builder.getThis()` returns `this`.**
- [ ] **Step 5: Run — verify it PASSES.** Same `-Dtest=…` command. Expected: PASS.
- [ ] **Step 6: Whole-module build — no regression.** Run the build command with no `-Dtest`. Then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 7: Commit.** `feat(P-027-R2a): reactive operation config on Elasticsearch low-level client-service config`.

### Task 2: `ElasticsearchAccessorException` + `ReactiveElasticsearchAccessor`

**Files:**
- Create: `api/ElasticsearchAccessorException.java`; `api/reactive/ReactiveElasticsearchAccessor.java`.
- Modify: `appbase-client-elasticsearch/pom.xml` — version-less `io.projectreactor:reactor-core` (if `reactor.core.publisher.*` not already resolved) + `io.projectreactor:reactor-test` (test); Export-Package `...restclient.api.reactive`.
- Test: `api/ElasticsearchAccessorExceptionTest.java`; `api/reactive/ReactiveElasticsearchAccessorTest.java`.

**Interfaces:**
- Consumes: `AbstractReactiveAccessor` (`mono`/`flux`), a lazy `Supplier<ElasticsearchRestClient> borrow`.
- Produces:
  - `public final class ElasticsearchAccessorException extends AppbaseClientException` with ctors `(Throwable)` / `(String, Throwable)` and the shared mapper (mirror `JasyncAccessorException.map`, substituting the ES exception type):
    ```java
    public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
        final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof ResponseException) {          // org.elasticsearch.client.ResponseException
            return new ElasticsearchAccessorException(cause);
        }
        return fallback.apply(cause);
    }
    ```
    (`org.elasticsearch.client.ResponseException`, `java.util.concurrent.{CompletionException,ExecutionException}`, `java.util.function.Function`.) Javadoc: `ResponseException` is Elasticsearch's non-2xx signal (extends `IOException`); other `IOException`s (transport failures) deliberately fall through to the default mapping.
  - `public class ReactiveElasticsearchAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<ElasticsearchRestClient> borrow)` → `super(appbaseCore, reactorService, configuration)`; `this.borrow = AppbaseUtil.notNull(borrow, "borrow")`.
    - Ten operations, one per typed op, each of the shape:
      ```java
      public Mono<SearchResponse> search(final String op, final SearchRequest request) {
          AppbaseUtil.notNull(op, "op");
          AppbaseUtil.notNull(request, "request");
          return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncSearch(request)));
      }
      ```
      (Same shape for `multiSearch`/`addDocument`/`updateDocument`/`addOrUpdateDocument`/`deleteDocument`/`createIndex`/`existsIndex`/`deleteIndex`/`cancelTask` over the matching `asyncXxx` + request/response types.)
    - One convenience:
      ```java
      public Flux<Hits> searchHits(final String op, final SearchRequest request) {
          return search(op, request).flatMapMany(r -> Flux.fromIterable(hitsOf(r)));
      }
      ```
      with a private null-safe `List<Hits> hitsOf(SearchResponse)` navigating `getSearchResponseBody().getHits().getHits()` (empty list if any level null). Javadoc: convenience stream over the search hit list; no separate driver call.
    - `@Override protected Throwable mapError(Throwable t)` = `ElasticsearchAccessorException.map(t, super::mapError)`; `@Override protected boolean isNotable(Throwable mapped)` = `true`.
    - **Javadoc: `Mono.fromFuture` bridges the typed client's `asyncXxx` `CompletableFuture` API to Reactor (Reactor unwraps `CompletionException`); the borrow is the typed `ElasticsearchRestClient` (default or key-bound view), which owns its own integration-service lifecycle — so, unlike the Cassandra/jasync accessors, there is no `finalizeOperation` here.**

- [ ] **Step 1: Write the failing tests.** `ElasticsearchAccessorExceptionTest`: a bare `ResponseException` maps to `ElasticsearchAccessorException`; a `CompletionException(cause=ResponseException)` unwraps+maps; a non-ES throwable delegates to the fallback. `ReactiveElasticsearchAccessorTest`: mock `ElasticsearchRestClient`; stub `asyncSearch(req)` → `CompletableFuture.completedFuture(mockSearchResponse)`; assert `search("q", req)` emits it (StepVerifier); a failed future carrying a `ResponseException` surfaces `ElasticsearchAccessorException`; a `Timer` is recorded (mirror `ReactiveCassandraAccessorTest`). Add one `searchHits` test (stub a `SearchResponse` with two `Hits` → `StepVerifier` expects 2).
- [ ] **Step 2: Run — verify it FAILS.** `-Dtest=ElasticsearchAccessorExceptionTest,ReactiveElasticsearchAccessorTest`. Expected: compile failure (types undefined).
- [ ] **Step 3: Implement** the exception, the accessor (10 `Mono` ops + `Flux searchHits` + `hitsOf`), and the pom changes (reactor-core/reactor-test + Export-Package).
- [ ] **Step 4: Run — verify it PASSES.** Same `-Dtest=…`. Expected: PASS.
- [ ] **Step 5: Whole-module build — no regression.** Build command, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit.** `feat(P-027-R2a): ElasticsearchAccessorException + ReactiveElasticsearchAccessor (Mono.fromFuture spine)`.

### Task 3: `getReactiveAccessor()` acquisition + `boundKey` plumbing

**Files:**
- Modify: `api/ElasticsearchRestClient.java` (interface) + `api/DefaultElasticsearchRestClient.java` — reactive getters, `appbaseCore` field, `boundKey` field + private ctor + `keyBound`, executor routing.
- Test: `api/DefaultElasticsearchRestClientReactiveAcquisitionTest.java`.

**Interfaces:**
- Consumes: Task 1's `getReactiveOperationConfiguration()`, Task 2's `ReactiveElasticsearchAccessor`.
- Produces (add to `ElasticsearchRestClient`):
  ```java
  ReactiveElasticsearchAccessor getReactiveAccessor();
  ReactiveElasticsearchAccessor getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
  ```
- `DefaultElasticsearchRestClient` changes:
  - Store the injected `AppbaseCore` as `private final AppbaseCore appbaseCore;` (already an `@Inject` ctor arg — assign it; keep the existing logger derivation).
  - Add `@Nullable private final AppbaseClientKey<ClusterIdValue> boundKey;`. Existing `@Inject` ctor sets `boundKey = null`. Add a private ctor:
    ```java
    private DefaultElasticsearchRestClient(
            final AppbaseCore appbaseCore,
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService,
            final AppbaseClientKey<ClusterIdValue> boundKey) { … }   // sets boundKey; no registerObjectMapperSubtypes re-run needed
    ```
  - Add a private key-bound integration-service resolver and route BOTH executors through it (null → current default behavior, byte-identical):
    ```java
    private ElasticsearchLowLevelRestClientIntegrationService integrationService() {
        return boundKey == null
                ? elasticsearchLowLevelRestClientService.getClientIntegrationService()
                : elasticsearchLowLevelRestClientService.getClientIntegrationService(boundKey);
    }
    ```
    `sendSyncRequest`/`sendRequest` call `integrationService().getClient()` instead of `…getClientIntegrationService().getClient()`.
  - Add `private ElasticsearchRestClient keyBound(final AppbaseClientKey<ClusterIdValue> key) { return new DefaultElasticsearchRestClient(appbaseCore, elasticsearchLowLevelRestClientService, key); }`.
  - Implement the getters:
    ```java
    @Override public ReactiveElasticsearchAccessor getReactiveAccessor() {
        return new ReactiveElasticsearchAccessor(appbaseCore, appbaseCore.getReactorService(),
                reactiveOperationConfiguration(), () -> this);
    }
    @Override public ReactiveElasticsearchAccessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> key) {
        return new ReactiveElasticsearchAccessor(appbaseCore, appbaseCore.getReactorService(),
                reactiveOperationConfiguration(), () -> keyBound(key));
    }
    ```
    with a private helper `ReactiveOperationConfiguration reactiveOperationConfiguration()` returning `elasticsearchLowLevelRestClientService.<ElasticsearchLowLevelRestClientServiceConfiguration>getConfiguration().getReactiveOperationConfiguration()`.
  - **Javadoc at the `boundKey` field + `integrationService()`:** getters live at the typed layer, so keyed multi-cluster is threaded here (not free from an `AppbaseClientService`); `boundKey == null` is the default cluster and preserves the pre-twin behavior exactly.

- [ ] **Step 1: Write the failing test.** `DefaultElasticsearchRestClientReactiveAcquisitionTest` with a mocked `AppbaseCore` (returns a real/mocked `ReactorService`, log service) + a mocked `ElasticsearchLowLevelRestClientService` whose `getConfiguration()` returns a `DefaultElasticsearchLowLevelRestClientServiceConfiguration` with a known reactive config: assert `getReactiveAccessor()` is non-null; assert the keyed `getReactiveAccessor(key)` is non-null; assert that invoking a reactive op on the keyed accessor causes `elasticsearchLowLevelRestClientService.getClientIntegrationService(key)` to be used (verify the keyed integration-service lookup — Mockito `verify`), while the default accessor uses the no-arg lookup.
- [ ] **Step 2: Run — verify it FAILS.** `-Dtest=DefaultElasticsearchRestClientReactiveAcquisitionTest`. Expected: compile failure (getters undefined).
- [ ] **Step 3: Implement** the `appbaseCore` field, `boundKey` field + private ctor, `integrationService()` routing in both executors, `keyBound`, `reactiveOperationConfiguration()` helper, and the two reactive getters (interface + impl).
- [ ] **Step 4: Run — verify it PASSES.** Same `-Dtest=…`. Expected: PASS.
- [ ] **Step 5: Whole-module build — no regression.** Build command (confirms existing `DefaultElasticsearchRestClient` behavior compiles + existing tests green), then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit.** `feat(P-027-R2a): getReactiveAccessor acquisition + bound-key view on Elasticsearch rest client`.
- [ ] **Step 7: Open PR 1** (Tasks 1–3) into the twin branch; STOP for merge.

---

## PR 2 — branch `feature/P-027-R2a-elasticsearch-t4t5-sync-it` (Tasks 4–5, off the twin branch after PR 1 merges)

### Task 4: Sync `ElasticsearchAccessor` + `getAccessor()` (+ keyed)

**Files:**
- Create: `api/ElasticsearchAccessor.java`.
- Modify: `api/ElasticsearchRestClient.java` + `api/DefaultElasticsearchRestClient.java` — `getAccessor()` + keyed (reuses Task 3's `boundKey`/`keyBound`/`reactiveOperationConfiguration()`).
- Test: `api/ElasticsearchAccessorTest.java`; sync acquisition cases in `DefaultElasticsearchRestClientReactiveAcquisitionTest.java` (or a sibling `…SyncAcquisitionTest`).

**Interfaces:**
- Consumes: `AbstractAppbaseAccessor` (`execute`), `ElasticsearchAccessorException.map`, the lazy `Supplier<ElasticsearchRestClient> borrow`, Task 3's helpers.
- Produces:
  - `public class ElasticsearchAccessor extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration, Supplier<ElasticsearchRestClient> borrow)` → `super(appbaseCore, config.isMetricsEnabled(), config.isTraceEnabled(), config.isExceptionStoreEnabled())` (mirror `CassandraAccessor`, pulling all three flags from the `ReactiveOperationConfiguration`).
    - Ten operations, one per typed op, each of the shape:
      ```java
      public SearchResponse search(final String op, final SearchRequest request) throws Exception {
          AppbaseUtil.notNull(op, "op");
          AppbaseUtil.notNull(request, "request");
          return execute(op, () -> borrow.get().search(request));   // typed sync method already throws IOException
      }
      ```
      (No `.get()`/unwrap: the typed sync methods are already synchronous and throw `IOException` directly. Same shape for the other 9 ops.)
    - `@Override protected Throwable mapError(Throwable t)` = `ElasticsearchAccessorException.map(t, super::mapError)`; `@Override protected boolean isNotable(Throwable mapped)` = `true`.
  - On `ElasticsearchRestClient`:
    ```java
    ElasticsearchAccessor getAccessor();
    ElasticsearchAccessor getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
    ```
    impl mirrors the reactive getters (`() -> this` / `() -> keyBound(key)`), passing only `appbaseCore` + `reactiveOperationConfiguration()` + borrow.

- [ ] **Step 1: Write the failing tests.** `ElasticsearchAccessorTest`: mock `ElasticsearchRestClient`; assert `search`/`addDocument` return the stubbed typed response; a thrown `ResponseException` surfaces `ElasticsearchAccessorException`; a `Timer` is recorded. Acquisition: `getAccessor()` non-null + config passthrough; keyed `getAccessor(key)` routes to `getClientIntegrationService(key)` (Mockito `verify`, same style as Task 3).
- [ ] **Step 2: Run — verify it FAILS.** `-Dtest=ElasticsearchAccessorTest,DefaultElasticsearchRestClientReactiveAcquisitionTest`. Expected: compile failure.
- [ ] **Step 3: Implement** the sync accessor (10 ops) + the two `getAccessor` getters (interface + impl).
- [ ] **Step 4: Run — verify it PASSES.** Same `-Dtest=…`. Expected: PASS.
- [ ] **Step 5: Whole-module build — no regression.** Build command, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 6: Commit.** `feat(P-027-R2a): sync ElasticsearchAccessor + getAccessor acquisition`.

### Task 5: Gated reactive+sync IT + build-guide + J-018 clause

**Files:**
- Create: `src/test/java/.../restclient/api/reactive/ReactiveElasticsearchAccessorIT.java`.
- Modify: `appbase-java/build-guide.md` (add "Reactive Elasticsearch" section); `Claude-Review.md` (J-018 clause (i)).
- (No production changes.)

**Interfaces:**
- Consumes: `DefaultElasticsearchRestClient.getReactiveAccessor()`/`getAccessor()`, a live Elasticsearch bootstrapped via the module's existing ES test helper/config.

- [ ] **Step 1: Write the gated IT.** `@EnabledIfSystemProperty(named = "ElasticsearchClientServiceTests.enabled", matches = "true")` on the class. `@BeforeAll` boot the typed client against a live Elasticsearch (reuse the module's existing ES service test wiring). One reactive round-trip: `createIndex` → `addDocument` → `search` (StepVerifier asserts the indexed doc is found) → `deleteIndex`; one sync round-trip via `getAccessor()` mirroring the same sequence. Javadoc: requires a running Elasticsearch reachable per the module's gated-test config (`-DElasticsearchClientServiceTests.enabled=true`).
- [ ] **Step 2: Run — verify it is SKIPPED by default.** Build command with no property. Expected: the IT is skipped (0 run for that class); module build green.
- [ ] **Step 3: (Optional, if a live ES is available) run gated.** `-DElasticsearchClientServiceTests.enabled=true -Dtest=ReactiveElasticsearchAccessorIT`. If no live ES, note it as un-run and record under J-018.
- [ ] **Step 4: Add the build-guide section.** In `appbase-java/build-guide.md`, a "Reactive Elasticsearch" subsection: how to acquire `getReactiveAccessor()`/`getAccessor()` (+ keyed), the `Mono.fromFuture` bridge note, and the gated-IT command.
- [ ] **Step 5: Add J-018 clause (i).** In `Claude-Review.md`, append clause (i) to the J-018 action-plan table row: the new `ReactiveElasticsearchAccessorIT` (reactive + sync `createIndex`→`addDocument`→`search`→`deleteIndex` round-trip) gated by `-DElasticsearchClientServiceTests.enabled=true`, skipped by default; needs a live Elasticsearch + test-resource wiring; un-gate, run, fix here. (Do this on `develop` at roll-up time per the track-gated-tests rule, alongside the LEARNING.md append.)
- [ ] **Step 6: Whole-module build — no regression.** Build command, then ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- [ ] **Step 7: Commit.** `test(P-027-R2a): gated reactive+sync Elasticsearch IT + build-guide`. Then open **PR 2** (Tasks 4–5) into the twin branch; STOP for merge.

---

## Post-merge (both PRs merged into the twin branch)

- Roll up `feature/P-027-R2a-elasticsearch` → `feature/P-027-reactive-appbase` (merge, do NOT fast-forward-only if the epic has advanced); push the epic.
- Close the twin sub-issue manually (comment the roll-up merge SHA; GitHub auto-close only fires on merge to `develop`).
- On `develop`: append the Elasticsearch learnings to `appbase-java/LEARNING.md` (the typed-layer twin shape, the bound-key keyed divergence, why no driver metrics) and the J-018 clause (i) from Task 5 Step 5 (if not already committed).

## Self-review notes (spec coverage)

- Decisions A–F all mapped: A→Tasks 2+4 (both faces); B→Tasks 2–4 (typed-layer borrow `Supplier<ElasticsearchRestClient>`); C→Task 2 (`Mono.fromFuture`); D→Task 3 (`boundKey` view, both keyed getters); E→no driver-metrics task (spine `Timer` inherent in every `mono`/`execute`); F→no driver-bump task.
- Type consistency: `ReactiveOperationConfiguration` (not the low-level config) supplies `isMetricsEnabled/isTraceEnabled/isExceptionStoreEnabled` to the sync accessor `super(...)`; `getReactiveOperationConfiguration()` returns `DefaultReactiveOperationConfiguration` (a `ReactiveOperationConfiguration`); keyed methods use `AppbaseClientKey<ClusterIdValue>` throughout; the `Flux` element type is `Hits`.
