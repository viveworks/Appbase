# P-027 R2a — Reactive Firebase (Auth) Twin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reactive + synchronous **service-acquired** accessor faces to the `auth` sub-module of `appbase-client-firebase` (`FirebaseAuth`), on the shared Appbase accessor spine, bridging Firebase's `ApiFuture` API to Reactor via `Mono.create` + `ApiFutures.addCallback`.

**Architecture:** In-module, **jasync-shaped greenfield**. `FirebaseAuthClientService extends AppbaseClientService<FirebaseAuth, TextValue>` hands out the rich `FirebaseAuth`, so accessors borrow the pooled integration service (`Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>>`) and return it via `usingWhen`/`finalizeOperation`; keyed variants free. No existing accessor/exception. `FirebaseAuth` has both sync (`createUser(...)` throws checked `FirebaseAuthException`) and async (`createUserAsync(...)` → `ApiFuture<T>`). Spine per-op Timers only — no driver-metrics task. Scope = `auth/` only (`app/` untouched).

**Tech Stack:** Java 25, Project Reactor 3.6.8, firebase-admin 7.0.0 (`com.google.firebase.auth.FirebaseAuth`; `com.google.api.core.{ApiFuture,ApiFutures,ApiFutureCallback}`; `com.google.common.util.concurrent.MoreExecutors`), Micrometer (spine `Timer`), Guice, Maven, JUnit 5 + Mockito + `reactor-test`.

**Reference twins (mirror exactly):** Cassandra (`ReactiveCassandraAccessor` `Mono.usingWhen(Mono.fromSupplier(borrow), svc -> …, svc -> finalizeOperation)`; `CassandraAccessor`; `DefaultCassandraClientService` getters/config). Jasync (`JasyncAccessorException.map` unwrap; greenfield accessor). ZooKeeper (`ZooKeeperCommand` throwing-interface pattern for the sync generic command). All present in the working tree (branch off the epic).

## Global Constraints

- **Branch/PR (max 3 tasks per PR):** twin branch `feature/P-027-R2a-firebase` off the reactive epic (`feature/P-027-reactive-appbase`) HEAD. **PR 1** = Tasks 1–3 on `feature/P-027-R2a-firebase-t1t3-reactive`; **PR 2** = Tasks 4–5 on `feature/P-027-R2a-firebase-t4t5-sync-it`. Both target the twin branch. When both merge, roll up `feature/P-027-R2a-firebase` → the epic (close the twin sub-issue manually). No driver-bump prerequisite. Post each PR link to the twin sub-issue in sequence.
- **Every Java file:** Apache license header (copy verbatim from a sibling in the module), Javadoc on public methods, `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0`. No fully-qualified class names in code (imports only) — sole exception: class-name string constants.
- **Decisions documented in code:** the `ApiFuture`/`Mono.create`+`addCallback` bridge (incl. `onSuccess(null)`→`sink.success()` empty for `Void` ops, and `sink.onCancel(() -> f.cancel(false))`), the `CompletionException`/`ExecutionException` unwrap, the `FirebaseAuthCommand` throwing-interface rationale.
- **Dependency versions ONLY in root `pom.xml` `<dependencyManagement>`**; module poms version-less. (`reactor`/`reactor-test` already managed from R0. `ApiFutures`/`MoreExecutors` are transitive via firebase-admin — **no new dependency**.)
- **Build command:** `cd /Users/nav/Viveworks/Technology/Appbase/appbase-java && mvn -q -pl appbase-client/appbase-client-firebase -am -Dmaven.gitcommitid.skip=true -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false install` (use `install`, NOT `test`). Scope one class with `-Dtest=<Class>` (still `install`). `-Dsurefire.failIfNoSpecifiedTests=false` MANDATORY.
- **After each build:** ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**
- **Key type:** `AppbaseClientKey<TextValue>` (like jasync).
- **Gated tests → J-018.** **Learnings → `appbase-java/LEARNING.md`** at the end.

## Established facts (verified against the codebase / firebase-admin 7.0.0)

- `AbstractReactiveAccessor(AppbaseCore, ReactorService, ReactiveOperationConfiguration)`; protected `<T> Mono<T> mono(String op, Supplier<Mono<T>> source)`; overridable `mapError`/`isNotable`.
- `AbstractAppbaseAccessor(AppbaseCore, boolean metricsEnabled, boolean traceEnabled, boolean exceptionStoreEnabled)`; protected `execute(String op, Callable<T>)` throws `Exception`. Sync accessor reads the three flags from `ReactiveOperationConfiguration`.
- `DefaultReactiveOperationConfiguration`/`ReactiveOperationConfiguration` in `com.viveworks.appbase.client.core.reactive`.
- `FirebaseAuthClientService extends AppbaseClientService<FirebaseAuth, TextValue>` with `getClientIntegrationService()` + keyed. `DefaultFirebaseAuthClientService extends AbstractAppbaseClientService<FirebaseAuth, TextValue, DefaultFirebaseAuthClientServiceConfiguration>`; has `appbaseClientCore` (→ `getAppbaseCore()`/`getReactorService()`) and `getConfiguration()`. Mirror `DefaultCassandraClientService` acquisition.
- `FirebaseAuthClientServiceConfiguration extends AppbaseClientServiceConfiguration {}` is EMPTY — Task 1 adds the getter.
- No existing accessor/exception under `…firebase.auth`.
- **FirebaseAuth async op signatures (verified via javap on firebase-admin-7.0.0):**
  - `createUserAsync(UserRecord.CreateRequest)` → `ApiFuture<UserRecord>`
  - `getUserAsync(String)` → `ApiFuture<UserRecord>`
  - `getUserByEmailAsync(String)` → `ApiFuture<UserRecord>`
  - `updateUserAsync(UserRecord.UpdateRequest)` → `ApiFuture<UserRecord>`
  - `deleteUserAsync(String)` → `ApiFuture<Void>`
  - `setCustomUserClaimsAsync(String, Map<String,Object>)` → `ApiFuture<Void>`
  - `verifyIdTokenAsync(String)` → `ApiFuture<FirebaseToken>`
  - `createCustomTokenAsync(String)` → `ApiFuture<String>`
  Sync variants: same names without `Async`, returning `T` directly and throwing `com.google.firebase.auth.FirebaseAuthException` (`deleteUser`/`setCustomUserClaims` return `void`). (`com.google.firebase.auth.{UserRecord,FirebaseToken,FirebaseAuthException}`; `UserRecord.CreateRequest`/`UserRecord.UpdateRequest`.)
- `com.google.api.core.ApiFutures.addCallback(ApiFuture<V>, ApiFutureCallback<? super V>, Executor)`; `com.google.common.util.concurrent.MoreExecutors.directExecutor()`. Test doubles: `ApiFutures.immediateFuture(v)` / `ApiFutures.immediateFailedFuture(t)`.
- Gate: confirm the exact property from an existing `*Test` in the module (expected `FirebaseAuthClientServiceTests.enabled`); test helper `FirebaseAuthClientServiceTestHelper` exists.

---

## File Structure

- Modify `auth/configuration/FirebaseAuthClientServiceConfiguration.java` (interface) + `DefaultFirebaseAuthClientServiceConfiguration.java` — `reactiveOperationConfiguration`.
- Create `auth/accessor/FirebaseAuthAccessorException.java`.
- Create `auth/accessor/FirebaseAuthCommand.java`.
- Create `auth/accessor/reactive/ReactiveFirebaseAuthAccessor.java`.
- Create `auth/accessor/FirebaseAuthAccessor.java` (sync).
- Modify `auth/FirebaseAuthClientService.java` (interface) + `DefaultFirebaseAuthClientService.java` — getters.
- Module `pom.xml` — version-less `reactor-core` (if needed) + `reactor-test` (test); Export-Package `…firebase.auth.accessor.reactive`.

**Tests/IT + docs:** unit tests per accessor + acquisition + exception map + config; gated `ReactiveFirebaseAuthAccessorIT`; `build-guide.md` section; J-018 clause (k).

---

## PR 1 — branch `feature/P-027-R2a-firebase-t1t3-reactive` (Tasks 1–3)

### Task 1: `reactiveOperationConfiguration` on the FirebaseAuth client-service config

**Files:** Modify `auth/configuration/FirebaseAuthClientServiceConfiguration.java` + `DefaultFirebaseAuthClientServiceConfiguration.java`. Test: `auth/configuration/DefaultFirebaseAuthClientServiceConfigurationTest.java`.

**Interfaces:** Interface getter returns the abstraction `ReactiveOperationConfiguration getReactiveOperationConfiguration()`; impl returns concrete `DefaultReactiveOperationConfiguration` (covariant); field concrete. Mirror `DefaultCassandraClientServiceConfiguration` EXACTLY (field, `DEFAULT_REACTIVE_OPERATION_CONFIGURATION`, ctor `AppbaseUtil.notNull`, Builder setter inside `// CheckStyle: Builder start`/`end`, equals/hashCode/toString).

- [ ] **Step 1: Failing test** — getter defaults non-null `metricsEnabled()==true`; Jackson YAML round-trip preserves `metricsEnabled(false)`. Mirror `DefaultCassandraClientServiceConfigurationTest`.
- [ ] **Step 2: Run — FAIL** (`-Dtest=DefaultFirebaseAuthClientServiceConfigurationTest`).
- [ ] **Step 3: Implement** interface getter (abstraction) + Default impl (concrete covariant). Confirm `Builder.getThis()` returns `this`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): reactive operation config on FirebaseAuth client-service config`.

### Task 2: `FirebaseAuthAccessorException` + `ReactiveFirebaseAuthAccessor`

**Files:** Create `auth/accessor/FirebaseAuthAccessorException.java`, `auth/accessor/reactive/ReactiveFirebaseAuthAccessor.java`. Modify module `pom.xml` (reactor-core if needed + reactor-test test; Export-Package `…firebase.auth.accessor.reactive`). Tests: `auth/accessor/FirebaseAuthAccessorExceptionTest.java`, `auth/accessor/reactive/ReactiveFirebaseAuthAccessorTest.java`.

**Interfaces:**
- `FirebaseAuthAccessorException extends AppbaseClientException` — ctors `(Throwable)`/`(String, Throwable)`; static:
  ```java
  public static Throwable map(final Throwable throwable, final Function<Throwable, Throwable> fallback) {
      final Throwable cause = (throwable instanceof CompletionException || throwable instanceof ExecutionException)
              && throwable.getCause() != null ? throwable.getCause() : throwable;
      if (cause instanceof FirebaseAuthException) {          // com.google.firebase.auth.FirebaseAuthException
          return new FirebaseAuthAccessorException(cause);
      }
      return fallback.apply(cause);
  }
  ```
  (`com.google.firebase.auth.FirebaseAuthException`, `java.util.concurrent.{CompletionException,ExecutionException}`, `java.util.function.Function`.) Mirror `JasyncAccessorException.map`.
- `ReactiveFirebaseAuthAccessor extends AbstractReactiveAccessor` — ctor `(AppbaseCore, ReactorService, ReactiveOperationConfiguration, Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow)` → `super(appbaseCore, reactorService, configuration)`. A private bridge helper:
  ```java
  private <T> Mono<T> call(final String op, final Function<FirebaseAuth, ApiFuture<T>> fn) {
      return mono(op, () -> Mono.usingWhen(
              Mono.fromSupplier(borrow),
              svc -> Mono.create(sink -> {
                  final ApiFuture<T> f = fn.apply(svc.getClient());
                  ApiFutures.addCallback(f, new ApiFutureCallback<T>() {
                      @Override public void onSuccess(final T v) { if (v == null) { sink.success(); } else { sink.success(v); } }
                      @Override public void onFailure(final Throwable t) { sink.error(t); }
                  }, MoreExecutors.directExecutor());
                  sink.onCancel(() -> f.cancel(false));
              }),
              svc -> Mono.fromRunnable(svc::finalizeOperation)));
  }
  ```
  8 public ops delegating to `call`, e.g. `public Mono<UserRecord> getUser(String op, String uid) { return call(op, a -> a.getUserAsync(uid)); }` — plus `createUser(op, CreateRequest)`→`Mono<UserRecord>`, `getUserByEmail(op, email)`→`Mono<UserRecord>`, `updateUser(op, UpdateRequest)`→`Mono<UserRecord>`, `deleteUser(op, uid)`→`Mono<Void>`, `setCustomUserClaims(op, uid, Map)`→`Mono<Void>`, `verifyIdToken(op, idToken)`→`Mono<FirebaseToken>`, `createCustomToken(op, uid)`→`Mono<String>`. Public generic `<T> Mono<T> command(String op, Function<FirebaseAuth, ApiFuture<T>> fn)` = `call(op, fn)`. `mapError` = `FirebaseAuthAccessorException.map(t, super::mapError)`; `isNotable` = `true`. **Javadoc: `Mono.create` + `ApiFutures.addCallback` bridges Firebase's `ApiFuture` (not a `CompletionStage`) to Reactor; `onSuccess(null)` completes empty for `Void` ops; `onCancel` cancels the `ApiFuture`; `usingWhen` returns the borrowed integration service on complete/error/cancel.**

- [ ] **Step 1: Failing tests** — `FirebaseAuthAccessorExceptionTest` (bare `FirebaseAuthException` maps; `ExecutionException(cause=FirebaseAuthException)` unwraps+maps; non-Firebase → fallback; construct `FirebaseAuthException` via its public ctor or a mock — check the SDK's accessible ctor). `ReactiveFirebaseAuthAccessorTest` (mock integration service → mock `FirebaseAuth`; stub `getUserAsync(uid)` → `ApiFutures.immediateFuture(userRecord)`; assert `getUser("op", uid)` emits it via StepVerifier; a `ApiFutures.immediateFailedFuture(firebaseAuthException)` surfaces `FirebaseAuthAccessorException`; `finalizeOperation` on complete AND error; `Timer` recorded — mirror `ReactiveCassandraAccessorTest`. Add a `deleteUser` test proving `ApiFuture<Void>` → empty `Mono` completes).
- [ ] **Step 2: Run — FAIL** (`-Dtest=FirebaseAuthAccessorExceptionTest,ReactiveFirebaseAuthAccessorTest`).
- [ ] **Step 3: Implement** exception, accessor (8 ops + `call` + `command`), pom.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): FirebaseAuthAccessorException + ReactiveFirebaseAuthAccessor (ApiFuture->Mono.create bridge)`.

### Task 3: `getReactiveAccessor()` acquisition

**Files:** Modify `auth/FirebaseAuthClientService.java` (interface) + `DefaultFirebaseAuthClientService.java`. Test: `auth/DefaultFirebaseAuthClientServiceReactiveTest.java`.

**Interfaces:** `ReactiveFirebaseAuthAccessor getReactiveAccessor()` + `getReactiveAccessor(AppbaseClientKey<TextValue>)` on the interface; impl mirrors `DefaultCassandraClientService`:
```java
@Override public ReactiveFirebaseAuthAccessor getReactiveAccessor() {
    return new ReactiveFirebaseAuthAccessor(appbaseClientCore.getAppbaseCore(),
            appbaseClientCore.getAppbaseCore().getReactorService(),
            getConfiguration().getReactiveOperationConfiguration(),
            this::getClientIntegrationService);
}
// keyed: () -> getClientIntegrationService(appbaseClientKey)
```
No cache; no `shutdownImpl` change.

- [ ] **Step 1: Failing test** — `autoStart(false)` + mocked core (mirror `DefaultCassandraClientServiceReactiveTest`): `getReactiveAccessor()` non-null + reactive-config passthrough; keyed non-null.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** getters (interface + impl).
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): getReactiveAccessor acquisition on FirebaseAuth client service`. Then open **PR 1** (Tasks 1–3) into the twin branch; STOP for merge.

---

## PR 2 — branch `feature/P-027-R2a-firebase-t4t5-sync-it` (Tasks 4–5, off the twin branch after PR 1 merges)

### Task 4: Sync `FirebaseAuthAccessor` + `FirebaseAuthCommand` + `getAccessor()`

**Files:** Create `auth/accessor/FirebaseAuthCommand.java`, `auth/accessor/FirebaseAuthAccessor.java`. Modify `auth/FirebaseAuthClientService.java` + `DefaultFirebaseAuthClientService.java` — `getAccessor()` + keyed. Tests: `auth/accessor/FirebaseAuthAccessorTest.java`; sync-acquisition cases in `DefaultFirebaseAuthClientServiceReactiveTest.java`.

**Interfaces:**
- `@FunctionalInterface public interface FirebaseAuthCommand<T> { T apply(FirebaseAuth auth) throws Exception; }` (import `com.google.firebase.auth.FirebaseAuth`). Rationale (Javadoc): FirebaseAuth's sync API throws checked `FirebaseAuthException`, which `java.util.function.Function` cannot express — mirrors `ZooKeeperCommand`.
- `FirebaseAuthAccessor extends AbstractAppbaseAccessor` — ctor `(AppbaseCore, ReactiveOperationConfiguration configuration, Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow)` → `super(appbaseCore, configuration.isMetricsEnabled(), configuration.isTraceEnabled(), configuration.isExceptionStoreEnabled())` (mirror `CassandraAccessor`, `AppbaseUtil.notNull`). Each op via a private helper:
  ```java
  private <T> T run(final String op, final FirebaseAuthCommand<T> fn) throws Exception {
      return execute(op, () -> {
          final AppbaseClientIntegrationService<FirebaseAuth, TextValue> svc = borrow.get();
          try { return fn.apply(svc.getClient()); } finally { svc.finalizeOperation(); }
      });
  }
  ```
  8 ops delegating to `run`, e.g. `public UserRecord getUser(String op, String uid) throws Exception { return run(op, a -> a.getUser(uid)); }`; `deleteUser`/`setCustomUserClaims` are `void` (`run(op, a -> { a.deleteUser(uid); return null; })` typed `public void deleteUser(...)` discarding, or return `Void` — pick the cleaner, consistent idiom). Public `<T> T command(String op, FirebaseAuthCommand<T> fn) throws Exception` = `run(op, fn)`. `mapError`/`isNotable` as in the reactive accessor.
- On the service: `FirebaseAuthAccessor getAccessor()` + keyed (mirror the reactive getters, borrow `this::getClientIntegrationService` / `() -> getClientIntegrationService(key)`).

- [ ] **Step 1: Failing tests** — `FirebaseAuthAccessorTest` (mock integration service → mock `FirebaseAuth`; assert `getUser`/`createUser` return the value, `finalizeOperation` on success AND error, a thrown `FirebaseAuthException` surfaces `FirebaseAuthAccessorException`, `Timer` recorded; a `command` test with a REAL checked-throwing lambda `a -> a.getUser(uid)` proving `FirebaseAuthCommand` compiles + finalizes). Acquisition: `getAccessor()` non-null + config passthrough; keyed non-null.
- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement** `FirebaseAuthCommand`, the sync accessor (8 ops + `run` + `command`), the two getters.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `feat(P-027-R2a): sync FirebaseAuthAccessor + FirebaseAuthCommand + getAccessor acquisition`.

### Task 5: Gated reactive+sync IT + build-guide + J-018 clause

**Files:** Create `src/test/java/.../auth/accessor/reactive/ReactiveFirebaseAuthAccessorIT.java`. Modify `appbase-java/build-guide.md`; `Claude-Review.md` (J-018 clause (k) — on develop at roll-up).

- [ ] **Step 1: Write the gated IT** — confirm the exact gate property from an existing module `*Test` (expected `@EnabledIfSystemProperty(named = "FirebaseAuthClientServiceTests.enabled", matches = "true")`); read that `*Test` + `FirebaseAuthClientServiceTestHelper` and mirror the bootstrap (a live Firebase project via service-account credentials). Reactive round-trip via `getReactiveAccessor()`: `createUser` (random uid/email) → `getUser` (StepVerifier asserts the uid/email) → `setCustomUserClaims` → `getUser` (asserts the claim present) → `deleteUser` → `getUser` (asserts the error is `FirebaseAuthException` with `AuthErrorCode.USER_NOT_FOUND`, mapped to `FirebaseAuthAccessorException`). Sync round-trip via `getAccessor()` mirroring the sequence. `@AfterAll` best-effort `deleteUser` cleanup (tolerate not-found). Javadoc: requires a configured live Firebase project per the module's gated-test config.
- [ ] **Step 2: Run — verify SKIPPED by default** (build command → IT skipped, module green). Live run needs Firebase creds; note un-run + record under J-018.
- [ ] **Step 3: build-guide section** — "Reactive Firebase (Auth)": acquire `getReactiveAccessor()`/`getAccessor()` (+ keyed via `AppbaseClientKey<TextValue>`); the 8 ops + generic `command` (reactive `Function<FirebaseAuth, ApiFuture<T>>`; sync throwing `FirebaseAuthCommand<T>`); the `ApiFuture`/`Mono.create`+`addCallback` bridge note; the gated-IT command (note `*IT` needs `-Dtest=` if the module has no Failsafe — confirm and state accurately). Mirror the prior twins' sections.
- [ ] **Step 4: J-018 clause (k)** — append to the J-018 row: the new `ReactiveFirebaseAuthAccessorIT` (reactive + sync `createUser`→`getUser`→`setCustomUserClaims`→`getUser`→`deleteUser`→`getUser`(not-found) round-trip) gated by `-DFirebaseAuthClientServiceTests.enabled=true`, skipped by default; needs a live Firebase project + service-account credentials + test-resource wiring; note whether `*IT` needs `-Dtest=` (no Failsafe). Un-gate, run, fix here. **(On `develop` at roll-up time, with the LEARNING.md append.)**
- [ ] **Step 5: Whole-module build — no regression** (~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): the file and its generating plugin are deleted, so this command now errors. No post-build cleanup is needed.**).
- [ ] **Step 6: Commit** — `test(P-027-R2a): gated reactive+sync FirebaseAuth IT + build-guide`. Then open **PR 2** (Tasks 4–5) into the twin branch; STOP for merge.

---

## Post-merge (both PRs merged into the twin branch)

- Roll up `feature/P-027-R2a-firebase` → `feature/P-027-reactive-appbase` (merge; push the epic).
- Close the twin sub-issue manually (comment the roll-up merge SHA).
- On `develop`: append the Firebase learnings to `appbase-java/LEARNING.md` (the `ApiFuture`/`Mono.create`+`addCallback` bridge incl. `onSuccess(null)`→empty + `onCancel`; the `FirebaseAuthCommand` throwing-interface for the checked sync API — the ZooKeeper lesson recurring; `ApiFutures.immediateFuture`/`immediateFailedFuture` as clean test doubles) and the J-018 clause (k). **This completes the P-027 R2a twin set — note in LEARNING.md that all 9 driver twins are done and the epic can now be prepared for its `develop` merge (JaCoCo develop-profile + gated ITs across all twins).**

## Self-review notes (spec coverage)

- Decisions A–E mapped: A→Task 2 (`Mono.create`+`addCallback`); B→Tasks 2+4 (8 ops + `command`; `FirebaseAuthCommand` for sync); C→Task 2 (`FirebaseAuthAccessorException.map`); D→no driver-metrics task (spine Timers inherent); E→Tasks 3+4 (keyed getters on the client service).
- Type consistency: `ReactiveOperationConfiguration` supplies the sync accessor flags; keyed uses `AppbaseClientKey<TextValue>`; reactive op return types match the `ApiFuture<T>` element types (`UserRecord`/`Void`/`FirebaseToken`/`String`); reactive `command` uses `Function<FirebaseAuth, ApiFuture<T>>`, sync `command` uses `FirebaseAuthCommand<T>`.
