# P-027 R2a — Reactive S3 (object-store) Twin — Design

**Status:** approved (brainstorm) · **Date:** 2026-07-07 · **Epic:** `feature/P-027-reactive-appbase` · **Depends on:** the AWS→appbase-cloud migration (`2026-07-07-P018-aws-to-appbase-cloud-migration-design.md`) landing on the epic first — this twin is built in the **new** `appbase-cloud-aws-s3` module (package `com.viveworks.appbase.cloud.aws.s3.*`). **Completes R2a-in-repo** (the last remaining R2a twin after S3 is Vitess-gRPC).

## Goal

Add reactive + synchronous **service-acquired** accessor faces to `appbase-cloud-aws-s3`, on the shared Appbase accessor spine, over AWS SDK v2's S3 clients. Uniform with the prior twins: `getReactiveAccessor()`/`getAccessor()` (+ keyed), per-driver `*AccessorException.map`, spine per-op Timers.

## Shape — ES-like existing-async surface, Cassandra-like acquisition, true sync ≠ non-blocking

- `S3ClientsService extends AppbaseClientService<S3Clients, ClusterIdValue>` hands out `S3Clients`, a holder of **both** AWS clients: `getS3Client()` (sync `S3Client`, apache-client HTTP) and `getS3AsyncClient()` (async `S3AsyncClient`, netty-nio HTTP). Accessors borrow the pooled integration service (`Supplier<AppbaseClientIntegrationService<S3Clients, ClusterIdValue>>`); **keyed free** (Cassandra shape).
- The module already ships a bespoke `ObjectStoreAccessor`/`AbstractObjectStoreAccessor` with **both** faces (sync over `S3Client`; async `CompletableFuture` over `S3AsyncClient`) but NOT on the shared spine. The twin adds the **reactive (`Mono`/`Flux`)** face + brings both faces onto the shared spine (per-op `Timer`/trace/exception-store). The existing accessor is left in place (it proves both clients are wired); the twin is additive.
- **True sync ≠ non-blocking (P-027 §2):** the sync accessor uses the real **`S3Client`** (apache-client, blocking HTTP); the reactive accessor uses **`S3AsyncClient`** (netty-nio, non-blocking HTTP) → `CompletableFuture` → `Mono.fromFuture`. The sync face NEVER calls the async client and blocks — two genuinely different SDK clients + HTTP transports (both provided by `appbase-cloud-aws-httpclient`).

## Decisions

- **A — Reactive bridge `Mono.fromFuture`** over `S3AsyncClient` ops (they return `CompletableFuture`). E.g. `getObject(req, AsyncResponseTransformer.toBytes())` → `CompletableFuture<ResponseBytes<GetObjectResponse>>` → `mono(op, () -> Mono.usingWhen(borrow, svc -> Mono.fromFuture(() -> svc.getClient().getS3AsyncClient().getObject(...)), svc -> finalizeOperation))`.
- **B — Op surface: `bucket` is a per-op argument (CORRECTED 2026-07-08), keeping the accessor twin-consistent (keyed-free, borrow the integration service — NOT bucket-scoped).** The old bespoke `AbstractObjectStoreAccessor` is bucket-scoped + service-direct; the twin deliberately does NOT copy that — it stays uniform with the other twins (borrow + `usingWhen` + `finalizeOperation`) and carries `(bucket, key)` on each op (like `path` in ZooKeeper). Reactive ops: `readObjectAsBytes(op, bucket, key)`→`Mono<byte[]>`, `readObjectAsFile(op, bucket, key, Path dest)`→`Mono<Path>`, `writeObject(op, bucket, key, byte[])`/`writeObject(op, bucket, key, Path)`→`Mono<String>`, `deleteObject(op, bucket, key)`→`Mono<Boolean>`, `listObjects(op, bucket, prefix)`→`Flux<String>` (via `listObjectsV2` → keys); generic `<T> Mono<T> command(op, Function<S3AsyncClient, CompletableFuture<T>>)`. **The bespoke base-directory ops (`existsBaseDirectory`/`createBaseDirectory`/`deleteBaseDirectory`) are OMITTED** — a convenience of the old accessor, reachable via `listObjects`/`command`; the twin stays at bucket+key primitives. Sync accessor (Task 4) mirrors the same `(op, bucket, key, …)` set over the blocking `S3Client`; generic sync `<T> T command(op, Function<S3Client,T>)` — S3 SDK sync ops throw **unchecked** `SdkException`, so a plain `Function<S3Client,T>` suffices (NO throwing interface needed, unlike ZooKeeper).
- **C — Greenfield `S3AccessorException extends AppbaseClientException`** + static `map` unwrapping `CompletionException`/`ExecutionException` → cause, mapping `software.amazon.awssdk.core.exception.SdkException` (base of all AWS SDK errors, incl. `S3Exception`/`NoSuchKeyException`) → `S3AccessorException`, else fallback. Mirror `JasyncAccessorException.map`.
- **D — Spine Timers only; no driver-metrics task.** AWS SDK v2 has a `MetricPublisher` SPI, but that is a separate cross-cutting concern (and would belong to the client build, not the accessor); the spine's per-op `Timer` is the twin's metric (uniform with ES/Firebase). (The existing `AbstractObjectStoreAccessor` keeps its own Timers; the new spine accessors use the spine's.)
- **E — Keyed free** (getters on `S3ClientsService`, borrow `this::getClientIntegrationService`).

## Components (in `appbase-cloud-aws-s3`, package `…cloud.aws.s3`)

New:
- `accessor/S3AccessorException.java` (+ static `map`).
- `accessor/reactive/ReactiveS3Accessor.java` (`AbstractReactiveAccessor`; the `Mono` ops + `command` over `S3AsyncClient`).
- `accessor/S3Accessor.java` (sync `AbstractAppbaseAccessor`; same ops over `S3Client`).
- (`accessor/S3Command.java` only if a checked sync op requires it.)

Modified:
- `configuration/S3ClientsServiceConfiguration.java` (interface, currently empty) + `DefaultS3ClientsServiceConfiguration.java` — `reactiveOperationConfiguration` (mirror Cassandra; interface abstraction / impl covariant).
- `S3ClientsService.java` (interface) + `DefaultS3ClientsService.java` — `getReactiveAccessor()`/keyed + `getAccessor()`/keyed (mirror `DefaultCassandraClientService`).
- `appbase-cloud-aws-s3/pom.xml` — version-less `reactor-core` (if not resolved) + `reactor-test` (test); Export-Package `…cloud.aws.s3.accessor.reactive`.

## Testing

- Unit: `S3AccessorExceptionTest` (bare `S3Exception`/`SdkException` maps; `CompletionException`-wrapped unwraps; non-SDK fallback); `ReactiveS3AccessorTest` (mock integration service → mock `S3Clients` → mock `S3AsyncClient`; stub an op → `CompletableFuture.completedFuture(...)`; `StepVerifier`; `Timer`; `finalizeOperation` on complete + error); `S3AccessorTest` (sync over mocked `S3Client` + `SdkException`→`S3AccessorException`, `Timer`, finalize); config round-trip; acquisition (non-null + passthrough + keyed).
- Gated IT: `accessor/reactive/ReactiveS3AccessorIT` (reactive + sync round-trip against a pre-existing test bucket: `writeObject(bucket, key, bytes)` → `readObjectAsBytes(bucket, key)` asserts payload → `listObjects(bucket, prefix)` asserts the key present → `deleteObject(bucket, key)` → `readObjectAsBytes(bucket, key)` asserts `NoSuchKeyException`→`S3AccessorException`), gated `@EnabledIfSystemProperty(named = "S3ClientServiceTests.enabled", matches = "true")` (confirm/establish the gate from the module's existing tests; LocalStack/MinIO or real S3). Recorded in **J-018** (new clause).
- Docs: `build-guide.md` "Reactive S3" section.

## Tasks / PRs (max 3 per PR)

Built in `appbase-cloud-aws-s3` on the P-027 epic, after the AWS migration merges. Own P-027 sub-issue; roll up into the epic like the other twins.
- **PR 1** (`…-t1t3-reactive`): T1 `reactiveOperationConfiguration` on `S3ClientsServiceConfiguration` · T2 `S3AccessorException` + `ReactiveS3Accessor` (Mono ops + command, `Mono.fromFuture` over `S3AsyncClient`; pom reactor + Export-Package) · T3 `getReactiveAccessor()` + keyed.
- **PR 2** (`…-t4t5-sync-it`): T4 sync `S3Accessor` (+ `S3Command` iff needed) + `getAccessor()` + keyed · T5 gated `ReactiveS3AccessorIT` + build-guide + J-018 clause.

## Global constraints (inherited)

Java 25, Reactor 3.6.8; Apache header + Javadoc + `@author Navaneeth Mysore G` + `@author Claude` + `@since 1.0.0` on every file; no FQCNs; versions only in root depMgmt; decisions documented at the code site (the `S3Client` vs `S3AsyncClient` sync/non-blocking split; the `Mono.fromFuture` bridge; the `SdkException` unwrap). Build via the root reactor `mvn -q -pl appbase-cloud/appbase-cloud-aws/appbase-cloud-aws-s3 -am … install`; ~~`git checkout -- appbase-java/CHANGELOG.adoc`~~ ⚠️ **OBSOLETE (internal work item 413): file and plugin deleted; no post-build cleanup needed.** after each build. Gated tests → J-018. Learnings → `appbase-java/LEARNING.md`.
