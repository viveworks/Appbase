# P-033 — Transport-pluggable bootstrap (servlet · gRPC · future) — Design Note

**Status:** direction note (pre-implementation) · **Item:** Claude-Review.md #68 (P-033) · a formal spec + plan are still required before coding.

The bootstrap layer today assumes ONE delivery transport: an embedded servlet container. With a gRPC server face arriving (and other transports plausible later), the bootstrap should treat the **transport as pluggable** — the same booted Appbase stack (core + all data clients + service-layer logic) exposed over servlet, gRPC, or anything else, without the transport bleeding into the shared core.

## 1. Module restructure

### Today
```
appbase-bootstrap/                      (aggregator)
├─ appbase-bootstrap-core               core + client + service bootstrap, guice, lifecycle,
│                                        AND the servlet/web abstractions (…/bootstrap/server/:
│                                        AppbaseWebServer, AppbaseServletDeployment, AppbaseServletRegistrar)
├─ appbase-bootstrap-jetty              depends on -core
├─ appbase-bootstrap-tomcat             depends on -core
└─ appbase-bootstrap-undertow           depends on -core
```
The web/servlet abstractions sit in `-core`, so every consumer of `-core` (including a future gRPC server that has NOTHING to do with servlets) drags in the servlet API and web wiring.

### Target
```
appbase-bootstrap/                          (aggregator)
├─ appbase-bootstrap-core                   TRANSPORT-NEUTRAL: boots core/client/service, guice,
│                                           lifecycle. NO servlet/web code. Usable by any transport.
├─ appbase-bootstrap-servlet                Servlet/web delivery layer, moved out of -core/server/
│   │                                       (AppbaseWebServer, AppbaseServletDeployment,
│   │                                       AppbaseServletRegistrar, …) + the reactive-servlet
│   │                                       bridge helper (§3). Depends on -core.
│   ├─ appbase-bootstrap-servlet-jetty      (renamed from appbase-bootstrap-jetty)      → depends on -servlet
│   ├─ appbase-bootstrap-servlet-tomcat     (renamed from appbase-bootstrap-tomcat)     → depends on -servlet
│   └─ appbase-bootstrap-servlet-undertow   (renamed from appbase-bootstrap-undertow)   → depends on -servlet
└─ appbase-bootstrap-grpc                   gRPC server + interceptors + reactor-grpc.
                                            Depends on -core ONLY (never -servlet).
```

**What moves:** `appbase-bootstrap-core/…/bootstrap/server/` (the servlet/web abstractions) → `appbase-bootstrap-servlet`. Everything transport-neutral (`…/bootstrap/{core,client,service,guice}/` factories + lifecycle) stays in `-core`.

**Renames (breaking, done atomically as one refactor):** `appbase-bootstrap-{jetty,tomcat,undertow}` → `appbase-bootstrap-servlet-{jetty,tomcat,undertow}`; each now depends on `appbase-bootstrap-servlet` (which depends on `-core`) instead of `-core` directly. Dependents (appbase-examples, appbase-config-wizard, any app main) update their coordinates. Supersedes the module names from P-015.

**Invariant:** `-core` must not reference the servlet API after the move (enforce with a quick dependency check / bnd import scan). `-grpc` and `-servlet` are siblings that never depend on each other — they are alternative faces on the same `-core`.

### Naming precision: web ≠ servlet
**Web = HTTP (a protocol); servlet = a Jakarta spec.** The servlet-container abstraction is currently misnamed `AppbaseWebServer` — a servlet container is not "a web server." Rename as part of the move:
- `AppbaseWebServer` → **`AppbaseServletContainer`** (and any per-container `*WebServer` impls, e.g. a Jetty/Tomcat/Undertow subclass, → `*ServletContainer`).
- `AppbaseServletDeployment`, `AppbaseServletRegistrar` are already servlet-precise — keep.
- Audit the moved `-servlet` code: types that are servlet-spec concerns get `Servlet*`; only genuinely protocol-level HTTP concerns (CORS, HTTP headers/status, JAX-RS) keep `Web`/`Http`. Reserve `Web`/`Http` for the protocol, `Servlet` for the container/spec, and (later) `Grpc` for that transport — so a reader can tell the delivery layer from the type name.

## 2. Async servlets + reactive services — the threading model

A twin returning `Mono`/`Flux` is only as non-blocking as the **edge** that consumes it. On a servlet container there are three ways to bridge, one right.

### 2a. Classic thread-per-request (baseline, fully blocking)
```
             ┌─────────────── container worker thread (from Jetty/Tomcat/Undertow pool) ───────────────┐
 request ───►│ read → dispatch → [BLOCK on DB/IO ..............] → build response → write → close       │───► response
             └──────────────────────────────────────────────────────────────────────────────────────────┘
   one thread PINNED for the whole request lifetime; concurrency ceiling = pool size;
   slow I/O ⇒ threads pile up waiting.
```

### 2b. Blocking a reactive service inside a servlet (ANTI-PATTERN — never do this)
```
 container worker thread:  ... mono.block() ──────────────── PARKED, waiting ───────────────► resume → write
                                   │  subscribe                                   ▲
                                   ▼                                              │ completion signal
 reactor thread (scheduler/event-loop):   [ actual I/O work .................. ]──┘
```
TWO threads consumed per request (worker parked + reactor thread working). Strictly worse than 2a — you pay for the reactive machinery AND still cap throughput at the worker-pool size. `mono.block()` in a request path is the trap.

### 2c. Async servlet (Servlet 3.1+) + non-blocking I/O — CORRECT bridge
```
 T0  container worker thread:  read request → request.startAsync() → mono.subscribe(...) → RETURN TO POOL (freed)
                                                                          │
                                                                          ▼  (no thread pinned to the request now)
     reactor thread(s):  [ non-blocking I/O: reactive driver / event-loop / boundedElastic .......... ]
                                                                          │ onNext / onComplete
                                                                          ▼
 T1  container I/O thread (WriteListener.onWritePossible):  while(out.isReady()) out.write(chunk);
                                                            asyncContext.complete()  ─────────────────► response
```
- `request.startAsync()` **releases the worker thread immediately** — request lifetime is decoupled from a pinned thread.
- Concurrency is bounded by connections + reactor capacity, NOT the worker pool: a few threads serve many in-flight requests.
- Output is driven by `ServletOutputStream.isReady()` + `WriteListener.onWritePossible()` (non-blocking write).
- This is exactly how **JAX-RS async** (`@Suspended AsyncResponse`) and Spring MVC's reactive-return support ride over servlet containers.

### 2d. Native reactor-netty (P-027 R1) — for contrast (no servlet layer)
```
 event-loop thread (few, e.g. #cores):  handles N connections each, never blocks
   conn1 ──┐
   conn2 ──┤► [event loop] ─(reactive pipeline on schedulers)─► write when ready ─► conn
   connN ──┘
   no servlet API, no thread-per-request at all — the fully non-blocking server face.
```

### Two nuances that bite (both faces)
1. **Backpressure:** the servlet write side is pull-based (`isReady()`/`onWritePossible()`). A streaming `Flux` response MUST gate `onNext` on `isReady()` (or buffer with bounds), else it overruns the output. The async servlet gives the *hook*; end-to-end flow control must be wired to Reactor demand.
2. **Context propagation:** hopping reactor-thread → response loses all `ThreadLocal` state (MDC logging, tracing span, security/request scope). It must ride Reactor `Context` (+ `Hooks.enableAutomaticContextPropagation()` / micrometer `context-propagation`). Same nuance as the gRPC interceptor→reactive boundary and the reactor-grpc path.

## 3. `appbase-bootstrap-servlet` reactive bridge helper (make users do it right)

The whole point of putting this in `-servlet` is that users should NOT hand-roll §2c (and fall into §2b). A small helper centralises the correct async-servlet lifecycle, backpressure, error mapping, timeout, and context propagation. Sketch (signatures only — implementation at plan time):

```java
/** Drives the Servlet 3.1 async lifecycle for a reactive result, the right way. */
public interface ReactiveServletSupport {
    /** Unary: subscribe to mono, write one response body, complete; handle error→status, timeout, context. */
    <T> void writeMono(HttpServletRequest req, HttpServletResponse resp, Mono<T> mono, ResponseWriter<T> writer);

    /** Streaming: subscribe to flux, write each element gated on out.isReady() (bounded backpressure), complete. */
    <T> void streamFlux(HttpServletRequest req, HttpServletResponse resp, Flux<T> flux, ResponseWriter<T> writer);
}

/** Serialises one element to the (already non-blocking) output. */
@FunctionalInterface public interface ResponseWriter<T> { void write(T value, ServletOutputStream out) throws IOException; }

/** Extend for a servlet whose handler returns Mono/Flux; base handles startAsync/subscribe/complete. */
public abstract class AbstractReactiveServlet extends HttpServlet { /* template around ReactiveServletSupport */ }
```
Also provide a JAX-RS adapter (`@Suspended AsyncResponse` ← `Mono`/`Flux`) so Jersey endpoints get the same correct bridge. The helper: calls `startAsync()` + sets the async timeout, subscribes on the configured scheduler, writes via `WriteListener` honouring `isReady()`, maps errors to status codes through the existing exception mapping, propagates tracing/MDC/security via Reactor context, and `complete()`s on terminal signal (including cancel/timeout).

This does NOT make servlets as efficient as reactor-netty (R1) — the container's thread-per-request connector still frames the request — but it removes the blocking trap and gives a clean, correct reactive edge on the existing Jetty/Tomcat/Undertow containers for incremental adoption.

## 4. Symmetry across server faces

Servlet-async (`-servlet`), reactor-netty (P-027 R1), and reactor-grpc (`-grpc`) solve the SAME problem three ways: release the accepting thread, run the work on schedulers/event-loops, propagate context across the hop, apply cross-cutting concerns as interceptors/filters over shared services. The transport-pluggable bootstrap is what lets a single booted Appbase stack wear any of these faces.

## 5. Scope for the P-033 item (formal spec/plan still required)

1. Extract `appbase-bootstrap-servlet` from `-core` (move `…/bootstrap/server/`); enforce `-core` has no servlet dependency. Rename `AppbaseWebServer` → `AppbaseServletContainer` (+ any `*WebServer` impls) and audit web-vs-servlet naming per §1.
2. Rename `appbase-bootstrap-{jetty,tomcat,undertow}` → `appbase-bootstrap-servlet-{…}`, re-point to `-servlet`; update all dependents.
3. Add `appbase-bootstrap-grpc` (depends on `-core` only): gRPC `NettyServerBuilder`, Guice-registered services + `ServerInterceptor`s (auth/metrics/tracing/logging over the same services), grpc health service backed by Appbase health checkers; **reactor-grpc** reactive impls (`Mono`/`Flux`, streaming→`Flux`) first-class, opt-in per service; Reactor-context propagation across the interceptor→reactive boundary.
4. Add the `-servlet` reactive bridge helper (§3) + tests.
5. appbase-examples (P-008): a gRPC-bootstrapped microservice (sync + reactive impls) and a servlet-async reactive endpoint demo.

New deps (root `<dependencyManagement>` only): reactor-grpc (codegen plugin + runtime). grpc-java is already in the stack (vitess). Document the threading model (this note) in build-guide/user-guide at implementation.
