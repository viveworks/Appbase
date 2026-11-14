# appbase-bootstrap-reactor-netty

The **natively non-blocking HTTP face** for Appbase, built directly on
[reactor-netty](https://github.com/reactor/reactor-netty) — no servlet container, no thread-per-request
pool. It implements the existing `AppbaseWebServer` contract, so it is a peer of the Jetty / Tomcat /
Undertow servlet bootstraps, not a replacement wired through them.

> **The precise claim this module makes is: _the reactive face never blocks its event loop._**
> It is **not** "every endpoint is natively non-blocking" — six of the twelve endpoints read genuinely
> blocking stores. R3 made two of them (`/log`, `/exception`) **native** by pushing the offload down into
> the reactive core read boundary; the other four keep an explicit endpoint-level offload and are honest
> about it (see the tables below). BlockHound enforces the real claim as a build failure, not a comment.

## Scope boundary

- This module **is** the reactive HTTP server. It must be **excluded from P-033's
  `AppbaseServletContainer` rename** — it is not a servlet container and has no servlet API on its
  request path.
- The **servlet async bridge** (adapting the existing servlet endpoints onto a non-blocking transport)
  is **P-033**, not here. When `appbase-bootstrap-servlet` exists, link it here.

## The twelve endpoints

Every endpoint reproduces the read (`doGet`) behaviour of its servlet counterpart and returns identical
JSON. They split into four groups by where — if anywhere — the blocking work runs.

### Native, in-memory — the event loop runs the read directly (never blocks)

| Path | Read |
|---|---|
| `/health` | MicroProfile health snapshot |
| `/metric` | metric registry snapshot |
| `/serviceRegistry` | service-status registry snapshot |
| `/status` | overall service status |

### Native, reactive Lucene (R3) — no endpoint offload; the blocking read is offloaded inside the core boundary

`NativeLogEndpoint` and `NativeExceptionEndpoint` call the reactive `LogAccessor` / `ExceptionAccessor`
(`read…Reactive`) directly on the event loop — no `Mono.fromCallable(...).subscribeOn(...)`. The blocking
Lucene search + document fetch is offloaded one layer down, inside the N01 reactive read boundary
(`LuceneService.readReactive` → `ReactiveReadBoundary`, which schedules the read on a bounded worker and
applies the per-read timeout). The event loop is never the thread that blocks.

| Path | Read | Blocking work offloaded by |
|---|---|---|
| `/log` | Lucene log records via the reactive `LogAccessor` | the N01 core read boundary (`LuceneService.readReactive`) |
| `/exception` | Lucene exception details via the reactive `ExceptionAccessor` | same boundary |

### Native — user-store reads, resource-scoped authorization (P-027 R4, native reactive stores)

The event loop runs these reads directly through the **reactive** user-account/session stores (R4 N01/N02),
no offload boundary and no endpoint-level `.timeout`. Each enforces the **same resource-scoped
authorization the blocking servlet twin enforces** (spec §4 "no capability dropped"): an
`AuthorizationService.checkAuthorized(DefaultUserAccountService.class, <operation>, ctx)` gate runs
**before** the store is read — `403` on denial (the store is never read), `503` when the authorizer is
absent (fail-closed).

| Path | Read | Authz operation |
|---|---|---|
| `/userSession` | reactive session read (Aerospike twin) | `readSession` (`{appbaseUserId}`) |
| `/userAccount` | reactive account-by-email read (R2DBC twin) | `readAccountByEmail` (`{email}`) |

### Tier-1 offloaded — blocking read wrapped in `Mono.fromCallable(...).subscribeOn(offloadScheduler)`

The event loop is released immediately; the blocking call runs on a bounded worker. Tier-1 is an
explicitly-labelled fallback, never sold as non-blocking — it buys exactly one thing: the event loop is
never stalled.

| Path | What blocks | Offload status |
|---|---|---|
| `/configuration` | reads an in-memory, eventually-consistent snapshot; a background loader does the disk/JDBC scan off the event loop | **retained (R3 Option A)** — the read path does not block on disk/JDBC, so there is nothing to make native. A reactive config-scan mirror (`ReactiveConfigurationStore`) exists, but the endpoint reads the cache, not the scan |
| `/core` | in-memory read, but the route co-hosts slow `reload`/`shutdown` admin verbs | never (admin verbs are inherently slow) |
| `/jmx` | in-memory read, route co-hosts `reload`/`shutdown` | never |
| `/mediator` | in-memory read, route co-hosts `reload`/`shutdown` | never |

Only the read half of each path is reproduced. The mutating/administrative verbs
(`doPost`/`doPut`/`doDelete`: reload, shutdown, clear\*, register/unregister\*, writes) stay
servlet-only until they get a proper reactive treatment (**P-039**, targeted 2.0.0).

## Configuration

```yaml
appbase:
  reactorNettyWebServer:
    enabled: true              # start the reactive face
    host: 0.0.0.0
    port: 8080
    offloadSchedulerId: appbase-reactor-offload   # bounded scheduler for Tier-1 blocking work
    accessLogEnabled: false
    compressionEnabled: false
    contextPropagationEnabled: true
```

All keys except `enabled` have sensible defaults; reactor-netty supports each, and none is required to
prove a non-blocking face.

## BlockHound — how the non-blocking claim is enforced

The suite installs [BlockHound](https://github.com/reactor/BlockHound), which instruments the JVM to
throw when a blocking call executes on a non-blocking (event-loop / `parallel`) thread. This turns
"the event loop never blocks" into a build failure if violated, and asserting it by inspection would be
worthless.

- **BlockHound ≥ `1.0.13.RELEASE` is required on JDK 25** — earlier releases (`1.0.9`/`1.0.11`) fail
  to instrument with `The instrumentation have failed`.
- It needs `-XX:+AllowRedefinitionToAddDeleteMethods` (JDK 13+) and `-XX:+EnableDynamicAgentLoading`
  (JDK 21+), supplied by this module's surefire `argLine`.
- `boundedElastic` (where the Tier-1 offload runs blocking work on purpose) is **allowed** to block —
  BlockHound only flags the event-loop threads.

Two levels of proof back the non-blocking claim:

- **Armed-harness** (`ReactorNettyAppbaseWebServerBlockHoundTest` here, `CoreBlockHoundTest` in
  `appbase-core`): each deliberately blocks on a `parallel` scheduler and asserts it **is** detected, and
  that `boundedElastic` is **allowed** — so no later non-blocking assertion is vacuous.
- **Native-read proof** (`NativeLuceneReadBlockHoundTest`, in `appbase-core`): runs a real native Lucene
  reactive read subscribed on a `parallel` (non-blocking) thread and asserts the blocking read is
  offloaded off it — the read emits on a `boundedElastic` thread, never the subscriber thread (the
  assertion that has teeth even though the NIO-backed Lucene read, via `FileChannel`, is not itself on
  BlockHound's default flag list; verified by mutation). Both native Lucene stores route through the same `LuceneService.readReactive`
  chokepoint, so this covers `/log` and `/exception` at the read layer, in the offline core build.

The full **HTTP end-to-end** BlockHound proof — a real request driven through `NativeLogEndpoint` /
`NativeExceptionEndpoint` under BlockHound — is deferred to the integration tests / `appbase-stacktest`:
the `appbase-bootstrap-reactor-netty` test run needs a reactor-built aws-s3 jar and the maven-jar-plugin
archiver, neither available in the offline core build. That deferred proof adds the transport, not the
read semantics, which the core proof above already covers.

## Running the tests

```shell
# Ungated: runs in any normal build, no external infra.
mvn test -pl appbase-bootstrap/appbase-bootstrap-reactor-netty -P quick-profile
```

The end-to-end HTTP and back-pressure tests start the server on an ephemeral port and drive it with a
real `HttpClient` over a real socket — no mocks on the transport path.
