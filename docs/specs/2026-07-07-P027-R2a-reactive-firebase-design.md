# P-027 R2a — Reactive Firebase (Auth) Twin — Design

**Status:** approved (brainstorm) · **Date:** 2026-07-07 · **Epic:** `feature/P-027-reactive-appbase` · **Spec family:** P-027 R2a reactive driver twins (9th/final twin: Redis, Cassandra, Kafka, Aerospike, Jasync, Elasticsearch, ZooKeeper, **Firebase**).

## Goal

Add reactive + synchronous **service-acquired** accessor faces to the `auth` sub-module of `appbase-client-firebase` (`FirebaseAuth`), on the shared Appbase accessor spine, bridging Firebase's `ApiFuture` API to Reactor. Uniform with the prior twins: same accessor topology, `getReactiveAccessor()`/`getAccessor()` acquisition (incl. free keyed variants), same config/exception patterns.

## Shape

**jasync-shaped greenfield.** `FirebaseAuthClientService extends AppbaseClientService<FirebaseAuth, TextValue>` hands out the rich `com.google.firebase.auth.FirebaseAuth`, so accessors borrow the pooled integration service (`Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>>`) and return it via `usingWhen`/`finalizeOperation`; keyed variants free. No existing accessor or exception (greenfield). `FirebaseAuth` exposes BOTH a sync face (`createUser(...)` throws checked `FirebaseAuthException`) and an async face (`createUserAsync(...)` → `com.google.api.core.ApiFuture<T>`). Scope is the `auth/` sub-module only; the `app/` sub-module (FirebaseApp bootstrap + `CustomFirebaseThreadManager`) is untouched.

## Decisions (approved)

- **A — `ApiFuture` → Reactor bridge via `Mono.create` + `ApiFutures.addCallback`.** `ApiFuture<T>` is not a `CompletionStage`, so each reactive op is:
  ```
  mono(op, () -> Mono.usingWhen(
      Mono.fromSupplier(borrow),
      svc -> Mono.create(sink -> {
          final ApiFuture<T> f = fn.apply(svc.getClient());
          ApiFutures.addCallback(f, new ApiFutureCallback<T>() {
              public void onSuccess(T v) { if (v == null) sink.success(); else sink.success(v); }
              public void onFailure(Throwable t) { sink.error(t); }
          }, MoreExecutors.directExecutor());
          sink.onCancel(() -> f.cancel(false));
      }),
      svc -> Mono.fromRunnable(svc::finalizeOperation)));
  ```
  No new dependency — `com.google.api.core.{ApiFuture,ApiFutures,ApiFutureCallback}` (api-common) and `com.google.common.util.concurrent.MoreExecutors` (guava) are transitive via firebase-admin. `onSuccess(null)` → `sink.success()` (empty) so `Void`-returning ops (`deleteUser`, `setCustomUserClaims`) complete empty rather than NPE.
- **B — Op surface: 8 named + generic `command`.** `createUser(CreateRequest)`→`UserRecord`, `getUser(uid)`→`UserRecord`, `getUserByEmail(email)`→`UserRecord`, `updateUser(UpdateRequest)`→`UserRecord`, `deleteUser(uid)`→`Void`, `setCustomUserClaims(uid, Map)`→`Void`, `verifyIdToken(idToken)`→`FirebaseToken`, `createCustomToken(uid)`→`String`, on both faces. Reactive generic `<T> Mono<T> command(String op, Function<FirebaseAuth, ApiFuture<T>> fn)`. **Sync generic `<T> T command(String op, FirebaseAuthCommand<T> fn) throws Exception`** — a new `@FunctionalInterface FirebaseAuthCommand<T> { T apply(FirebaseAuth auth) throws Exception; }`, because FirebaseAuth's sync API throws checked `FirebaseAuthException` and `java.util.function.Function` can't express that (the ZooKeeper/Curator lesson repeats). The long tail (listUsers, links, revokeTokens, importUsers, phone lookups) goes through `command`.
- **C — Greenfield `FirebaseAuthAccessorException extends AppbaseClientException`** with ctors `(Throwable)`/`(String, Throwable)` and static `map(Throwable, Function<Throwable,Throwable> fallback)` unwrapping `CompletionException`/`ExecutionException` → cause, mapping `com.google.firebase.auth.FirebaseAuthException` → `FirebaseAuthAccessorException`, else fallback (mirror `JasyncAccessorException.map`).
- **D — Spine Timers only; no driver-metrics task.** firebase-admin exposes no Micrometer registry, and there is no pool/connection-state to gauge (the admin SDK is stateless HTTP over a `CustomFirebaseThreadManager` executor that may be shared/non-`ThreadPoolExecutor` and lives in the `app/` sub-module). Rely on the accessor spine's per-operation Micrometer `Timer` (latency/count/error, tagged per op) — real op-level metrics Firebase lacks today. No `driverMetricsEnabled` gate, no gauge. (Uniform with the Elasticsearch twin's decision.)
- **E — Keyed free** (Cassandra/jasync shape): getters on `FirebaseAuthClientService`, borrow `this::getClientIntegrationService` / `() -> getClientIntegrationService(key)`.

## Components

New (package `…service.firebase.auth.accessor`, reactive sub-package `.reactive`):
- `accessor/FirebaseAuthAccessorException.java`.
- `accessor/FirebaseAuthCommand.java` (`@FunctionalInterface`, `T apply(FirebaseAuth) throws Exception`).
- `accessor/reactive/ReactiveFirebaseAuthAccessor.java` (`AbstractReactiveAccessor`; 8 `Mono` ops + `command`, `Mono.create`+`addCallback` bridge).
- `accessor/FirebaseAuthAccessor.java` (sync `AbstractAppbaseAccessor`; 8 ops via `execute` over the sync FirebaseAuth methods (throw `FirebaseAuthException`) + `command`).

Modified:
- `auth/configuration/FirebaseAuthClientServiceConfiguration.java` (interface, currently empty) + `DefaultFirebaseAuthClientServiceConfiguration.java` — `reactiveOperationConfiguration` (mirror `DefaultCassandraClientServiceConfiguration`; interface returns abstraction, impl covariant).
- `auth/FirebaseAuthClientService.java` (interface) + `DefaultFirebaseAuthClientService.java` — `getReactiveAccessor()`/keyed + `getAccessor()`/keyed (mirror `DefaultCassandraClientService`; borrow `this::getClientIntegrationService`).
- Module `pom.xml` — version-less `reactor-core` (if not resolved) + `reactor-test` (test); Export-Package `…firebase.auth.accessor.reactive`.

## Error / data flow

- **Reactive:** `Mono.create` + `ApiFutures.addCallback` — `onFailure` delivers the raw `FirebaseAuthException` (Reactor sees it directly; no CompletionException wrapping through `Mono.create`, but `map` also unwraps `Completion`/`ExecutionException` defensively).
- **Sync:** FirebaseAuth sync methods throw `FirebaseAuthException` directly through `execute`.
- Shared `FirebaseAuthAccessorException.map` handles both.

## Testing

- Unit: `FirebaseAuthAccessorExceptionTest` (bare `FirebaseAuthException` maps; `ExecutionException`-wrapped unwraps+maps; non-Firebase fallback); `ReactiveFirebaseAuthAccessorTest` (mock integration service → mock `FirebaseAuth`; stub an async op → an immediately-completed/failed `ApiFuture` — use `ApiFutures.immediateFuture(v)` / `immediateFailedFuture(new FirebaseAuthException(...))`; `StepVerifier`; `Timer` recorded; `finalizeOperation` on complete + error); `FirebaseAuthAccessorTest` (sync happy path + `FirebaseAuthException` → `FirebaseAuthAccessorException`, `finalizeOperation`, `Timer`; a `command` test with a real checked-throwing lambda proving `FirebaseAuthCommand` compiles); config round-trip; acquisition (non-null + passthrough + keyed).
- Gated IT: `reactive/ReactiveFirebaseAuthAccessorIT` (reactive + sync round-trip: `createUser` → `getUser` asserts fields → `setCustomUserClaims` → `getUser` asserts claims → `deleteUser` → `getUser` asserts `UserNotFound`), gated `@EnabledIfSystemProperty(named = "FirebaseAuthClientServiceTests.enabled", matches = "true")` (confirm the exact property from an existing module `*Test`). Recorded in **J-018 clause (k)**.
- Docs: `build-guide.md` "Reactive Firebase (Auth)" section.

## Tasks / PRs (max 3 tasks per PR)

Twin branch `feature/P-027-R2a-firebase` off the epic HEAD. New twin sub-issue under internal work item 277; roll up when both merge (close manually).

**PR 1** — `feature/P-027-R2a-firebase-t1t3-reactive`:
- **T1** `reactiveOperationConfiguration` on `FirebaseAuthClientServiceConfiguration` (+ Default, + round-trip test).
- **T2** `FirebaseAuthAccessorException` + `ReactiveFirebaseAuthAccessor` (8 `Mono` ops + `command`, `Mono.create`+`addCallback` bridge; pom reactor + Export-Package); tests.
- **T3** `getReactiveAccessor()` + keyed acquisition (+ test).

**PR 2** — `feature/P-027-R2a-firebase-t4t5-sync-it`:
- **T4** sync `FirebaseAuthAccessor` + `FirebaseAuthCommand` + `getAccessor()` + keyed (+ tests, incl. the checked-command test).
- **T5** gated `ReactiveFirebaseAuthAccessorIT` + `build-guide.md` section + J-018 clause (k).

## Global constraints (inherited)

Java 25, Reactor 3.6.8; Apache header + Javadoc on public methods + `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0` on every file; no FQCNs (imports only); dependency versions only in root `<dependencyManagement>`; non-obvious decisions documented at the code site (the `ApiFuture`/`Mono.create`+`addCallback` bridge incl. `onSuccess(null)`→empty and `onCancel`→`cancel`, the exception unwrap, the `FirebaseAuthCommand` throwing-interface rationale). Build via `mvn -q -pl appbase-client/appbase-client-firebase -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install`; ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): file and plugin deleted; no post-build cleanup needed.** after each build. Gated tests → J-018. Learnings → `appbase-java/LEARNING.md` at the end.
