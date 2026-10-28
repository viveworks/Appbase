# P-027 R1 Task 9 — Secure-by-Default Authorization (both faces)

**Status:** Design — pending user review
**Epic:** P-027 R1 — Reactive web layer (internal work item 340). Branch merges into `feature/P-027-R1-reactive-web`.
**Sequencing:** ships BEFORE the epic's audit task (per user direction). This task absorbs additional
secure-by-default cases as they surface, rather than spawning new tasks.

## 1. Problem

The Task 8 security audit (finding D1) confirmed: on a **default** boot, the admin read endpoints
(`/configuration`, `/log`, `/exception`, `/core`, `/jmx`, `/mediator`, plus `/metric`,
`/serviceRegistry`) are served **unauthenticated** on BOTH the servlet and reactive faces. Two
compounding causes, both in shared code:

1. **`PermitAllContextAuthorizer`** (`appbase-service-core/.../authorization/PermitAllContextAuthorizer.java`)
   — the only shipped `ContextAuthorizer` — returns `true` for every `(service, operation, context)`.
2. Even when `isAuthorized(...)` returns `false`, the filter (`AuthorizationWebFilter` reactive,
   `AuthorizationServletFilter` servlet) only denies if an `AuthorizationServiceExceptionConfiguration`
   with a forbidden/redirect/status action is registered — and **no production bootstrap registers
   one**, so an unauthorized request passes through.

Net effect: unauthenticated secret disclosure (secure-property values, config JSON, logs, exception
stacks, MXBean internals, a server filesystem path) once any face is started. This is *faithful to the
historical posture* but unacceptable as the reactive face goes live.

## 2. Goals / Non-goals

**Goals**
- **Secure by default**: an unauthenticated request to a non-public endpoint is **denied (403)** on a
  default boot, on BOTH the servlet and reactive faces, with identical behaviour.
- **Public paths stay open**: `/health` (liveness/readiness probes) is reachable unauthenticated by
  default; the public set is configurable.
- **Configurable in the builder**: `secureByDefault(boolean)` (default `true`) and
  `publicPaths(Set<String>)` (default `{"/health"}`) on **`AppbaseServices.Builder`** — the single
  place that owns the authorization strategy (via the existing `withContextAuthorizer`). All four web
  faces (reactive + jetty/tomcat/undertow) consume `AppbaseServices` and inherit the posture
  automatically, so they stay identical by construction.

**Non-goals**
- No new authentication mechanism — this is authorization posture only (deny-vs-permit default).
- No change to the filter *machinery* (both faces already deny 403 when the authorizer says no AND a
  forbidden exception-config is present — this task supplies both, reusing that path).
- No per-operation rule authoring UI; existing rule config still governs fine-grained allow rules.

## 3. Design

**Key enabler (verified):** both filter faces call the SAME
`authorizationFilter.isAuthorized(appbaseCore, service=path, operation=method, sessionId, requesterId)`,
and both already emit **403** when that returns `false` AND an
`AuthorizationServiceExceptionConfiguration` with a FORBIDDEN action is present. So secure-by-default
is achieved by wiring the right *authorizer* and *exception-config* — **no filter code change** — and
both faces inherit it.

Two coordinated pieces, both in the shared `appbase-service-core` layer:

### 3.1 `SecureByDefaultContextAuthorizer` (new)

A `ContextAuthorizer` that replaces `PermitAllContextAuthorizer` when secure-by-default is on. For
`(service = request path, operation, context)`:
- returns `true` if `service` is in the configured **public paths** (default `{"/health"}`);
- otherwise delegates to the existing rule-based decision (so any explicitly-configured allow rule
  still permits);
- returns `false` when neither matches — i.e. **deny by default**.

It composes with, not replaces, the existing rule evaluation — an operator who has configured allow
rules keeps them; the change is only the *default* for unmatched paths (permit → deny).

### 3.2 Default forbidden exception-config

When secure-by-default is on, the wiring registers a default
`AuthorizationServiceExceptionConfiguration` whose action is **FORBIDDEN (403)** (unless the operator
already registered one — theirs wins). This is what turns a `false` authorization decision into a 403
response instead of a pass-through, on both faces, via the existing filter machinery.

### 3.3 Builder configuration — one setter on `AppbaseServices.Builder` (both faces inherit)

**Architecture (verified):** the `ContextAuthorizer` is set on `AppbaseServices.Builder` via the
existing `withContextAuthorizer(...)`; the authorization service is built from it (through
`DefaultAuthorizationServiceFactory`) and registered in `appbaseCore`. Both the reactive
(`ReactorNettyAppbaseWebServer.Builder.from(AppbaseServices)`) and the three servlet faces consume a
**pre-built `AppbaseServices`**, and both faces' filters read the authorization service from
`appbaseCore`. So the single source of truth is `AppbaseServices.Builder` — the web-server builders do
NOT own the authorization strategy and must not each duplicate it.

Therefore the secure-by-default posture is configured in **one place**:
- `AppbaseServices.Builder.secureByDefault(boolean)` — default **true**.
- `AppbaseServices.Builder.publicPaths(Set<String>)` — default **`{"/health"}`**.

When `secureByDefault` is true and no explicit `withContextAuthorizer(...)` was supplied, the builder
selects the `SecureByDefaultContextAuthorizer` (constructed with `publicPaths`) instead of the default
`PermitAllContextAuthorizer`, and arranges the default FORBIDDEN(403) exception-config. An explicit
`withContextAuthorizer(...)` still wins (operator override). **All four web faces inherit the posture
automatically** because they consume the same `AppbaseServices` — the cross-face invariant holds by
construction, and it is structurally impossible for the faces to diverge. No per-web-builder setter is
added.

**Default public set:** `/health` only. `/metric` and `/serviceRegistry` expose operational internals
and are **gated by default** (an operator can add them to `publicPaths` if their monitoring needs it).
This is the conservative default; revisit if a concrete case emerges (per the "absorb cases" scope).

## 4. Testing

- **Shared-layer unit tests** (`SecureByDefaultContextAuthorizerTest`): public path → permit; a
  configured-allow path → permit; an unmatched path → deny; `ALL_OPERATIONS` handling matches the
  `ContextAuthorizer` contract.
- **Reactive end-to-end** (extends the Task 7/8 HTTP tests): a server built with `secureByDefault(true)`
  → `GET /configuration?...` returns **403**; `GET /health` returns **200**; a server built with
  `secureByDefault(false)` → `/configuration` passes through (legacy posture). This is the test the
  Task 8 audit (D5) said was missing — it drives a real request through the assembled chain and asserts
  the deny.
- **Servlet parity test**: the same secure-by-default boot denies `/configuration` and permits `/health`
  on a servlet bootstrap — proving both faces behave identically.
- **Builder config**: `publicPaths(...)` override opens a chosen path; `secureByDefault(false)` restores
  permit-all.
- Regression: existing authorization tests (rule-configured allow/deny) stay green.

All ungated, no external infra. Module builds under `develop-profile` (jacoco 90%).

## 5. Security & audit

Per-PR 5-lens audit, heavy on the new authorizer + wiring. The headline check: on a default-built
server of EACH face, a secret-exposing admin read returns 403, and `/health` returns 200 — asserted
end-to-end, not assumed. Append the finding→fix→regression-test rows to `docs/Quality-Report.md`
(this closes audit finding D1).

## 6. Open items / scope notes

- **Absorb emerging cases** (per user): if the audit or implementation surfaces additional
  unauthenticated-exposure paths or faces, fold the fix into this task rather than a new one.
- The exact mechanism for the builder to override the Guice-bound `ContextAuthorizer` is a
  plan-time detail (constructor injection vs a module override) — resolved in the implementation plan
  against the actual DI wiring.
- Still tracked separately (NOT this task): reactive face ignoring `AppbaseFilterConfig`;
  `Quality-Report.md` backfill.
