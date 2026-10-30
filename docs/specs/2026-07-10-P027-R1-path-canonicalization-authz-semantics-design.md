# P-027 R1 — Canonical path key space and authorization semantics

Date: 2026-07-10
Branch: sub-branch off `feature/P-027-R1-reactive-web`, PR'd back into it
Status: approved 2026-07-10; in implementation on `feature/P-027-R1-path-canonicalization` (issue internal work item 342)
Supersedes: the earlier OPEN DECISION on path canonicalization and authorization semantics.

## 1. Why

Appbase matches security configuration — CORS headers, authorization allow/block lists, session
creation, filter enablement — against the request path as an **exact string key**. Three transport
faces produce three different strings for the same request, and no face normalizes. Config keys are
themselves unnormalized authored strings. The result is a key space where a rule can be written,
loaded, and silently never matched.

This is a pre-1.0.0 correction. There are no production users, so the key space can be fixed with no
migration. Fixed after 1.0.0 it becomes a breaking change to authorization semantics, which is the
one thing that must never appear in a migration guide. That timing argument is the whole reason this
work exists now rather than in P-039.

## 2. Verified facts

Each was checked against the pinned artifact during design. Do not re-litigate.

| # | Fact | Evidence |
|---|---|---|
| 1 | `getRequestURI()` is not decoded, and includes the context path | `jakarta.servlet-api` 6.0.0 javadoc: *"The web container does not decode this String"* |
| 2 | `UriInfo.getPath()` is decoded and context-relative | `jakarta.ws.rs-api` 3.1.0 javadoc: *"All sequences of escaped octets are decoded, equivalent to `getPath(true)`"*; `jersey-server` 3.1.3 `UriRoutingContext.getPath()` bytecode: `iconst_1` → `ContainerRequest.getPath(true)` |
| 3 | `UriInfo.getPath(false)` yields the **encoded**, context-relative path | `javap` on `jakarta.ws.rs-api` 3.1.0: `public abstract String getPath(boolean)` |
| 4 | `NettyWebExchange.path()` returns `request.fullPath()` — decoded, no context path | `NettyWebExchange:103` |
| 5 | The encoded path is already reachable on the reactive face | `NettyWebExchange:93` builds `QueryStringDecoder` from `request.uri()`; `QueryStringDecoder.rawPath()` exists (`javap`, netty-codec-http 4.1.111) |
| 6 | `NettyWebExchange` is the only production implementation of `AppbaseServiceExchange` | 9 other hits are test fakes |
| 7 | An allowlist never denies non-members | `DefaultAuthorizationService:89-95` — the branch can only `return true` (on wildcard); otherwise control falls to `:97` |
| 8 | Configured-but-unmatched is indistinguishable from unconfigured | `DefaultAuthorizationService:74-78` (branch A) and `:97` (branch B) make the same `contextAuthorizer.isAuthorizedContext(...)` call |
| 9 | `DefaultContextAuthorizer` returns `true` unconditionally, but is **not** a shipped default | Never constructed outside 3 test files; apps supply their own via `AppbaseServices.Builder.withContextAuthorizer(...)`, which is `Objects.requireNonNull` |
| 10 | The reactive exchange is constructed before the filter chain is entered | `ReactorNettyAppbaseWebServer:106` (construct) vs `:113` (chain) |
| 11 | Ten path-keyed maps exist across three configuration classes (was twelve/four; `DefaultAuthorizationServiceConfiguration`'s two were dead and are deleted) | §4, §4.1 |
| 12 | `isFilterEnabled` has exactly two callers, both servlet/JAX-RS | `AbstractAppbaseServletFilter:62`, `AbstractAppbaseJaxrsFilter:90` |

## 3. Defects addressed

**D1 — Encoded-path bypass.** A blocklist entry `/admin` does not match a request for `/%61dmin`.
The servlet face hands `getRequestURI()` to `containsKey`, the miss skips
`DefaultAuthorizationService:84`'s `return false`, and the operator's explicit deny is discarded.

**D2 — Allowlist never denies.** `DefaultAuthorizationService:89-95`. A path absent from a populated
allowlist falls through to the context authorizer instead of being denied. Both `isAuthorized`
overloads (`:67` two-arg, `:122` three-arg) carry this logic and must not drift.

**D3 — Three key spaces.** Servlet is encoded + context-inclusive; JAX-RS is decoded +
context-relative; reactive is decoded + context-relative. The servlet/JAX-RS split **predates R1**.
R1 did not introduce a divergence; it matched JAX-RS.

Of D3's two halves, only the **encoding** difference is live. All three servlet containers mount at
root and cannot be configured otherwise (§8), so `getRequestURI()` already equals the
context-relative path and the context-path half cannot occur today. It is fixed here as hardening,
not repair.

**D4 — `DefaultContextAuthorizer` is misnamed.** It authorizes every request. The name `Default`
asserts it is the safe thing you get without thinking. It is the opposite.

### 3.1 Defects found during implementation, and fixed here

The four above were found during design. These were found while building the fix, each by a test or a
review rather than by inspection. All are fixed on the same branch, per the user's instruction to fix
what we find and document it.

**D5 — `//admin` bypassed a `/admin` rule (Task 1).** `new URI("//admin")` parses the leading `//` as
an RFC 3986 network-path reference: `authority=admin, path=""`. The first canonicalizer decoded before
collapsing duplicate slashes, so `//admin` canonicalized to `/` and matched no rule. Caught by the
plan's own `//admin → /admin` test row. Fixed by collapsing on the encoded string first, which is
sound only because structural octets are rejected before that.

**D6 — `/admin/.` bypassed a `/admin/` rule (Task 1, review).** `removeDotSegments` was not faithful
to RFC 3986 §5.2.4: it dropped the trailing slash a terminal `.` or `..` must leave. Because the
design deliberately does **not** fold trailing slashes, `/admin/` is a distinct key an operator can
block, and `/admin/.` slid past it. A partial trailing-slash fold — the one thing §5.1 forbids.

**D7 — `canonicalize` was not idempotent (Task 1, re-review).** `/a%20b` → `/a b`, and canonicalizing
`/a b` **threw**, because `java.net.URI` rejects a raw space. Same for `%09`, `%22`, `%3C`, `%5B`,
`%7B`. The two-boundary design leans on idempotence. The property test could not catch it: none of its
inputs contained those octets. Fixed by replacing the decoder (§5.1) and rejecting a residual `%`,
making idempotence true by construction rather than asserted. Verified by a 300,000-case fuzz.

**D8 — `DefaultAuthorizationServiceConfiguration` was dead, and inverted (Task 2).** See §4.1.
Deleted.

**D9 — a pre-existing test asserted D2 (Task 2).** `DefaultAuthorizationServiceTest.testCheckAuthorized`
asserted that, with a populated allowlist, a non-member class is authorized. That is D2 verbatim. It
also made the test's own next two lines — `addAllowedService(UserSessionStore.class)` followed by the
same assertion — unfalsifiable. The assertion is flipped to `assertThrows`, which makes the `add` line
observable for the first time. Not a production defect; a test that could not fail.

Out of scope, filed separately: the reactive face ignores `AppbaseFilterConfig` entirely
(fact 12). That is a behavior addition to seven filters, not a key-space normalization, and merging
the two produces a diff in which the security change cannot be seen. Fail-safe today (filters run
when they should have been disabled), so it does not gate this work. **It must land before the epic
merges to `develop`.**

## 4. The ten path-keyed maps

**Revised 2026-07-10, during Task 2.** This table originally listed twelve maps across four
configuration classes. `DefaultAuthorizationServiceConfiguration`'s two are **not** among them, and
have been deleted — see §4.1. Ten remain, across three classes.

| Class | Member | Consumer |
|---|---|---|
| `CorsFilterConfiguration` | `pathByCorsHeadersMap` | `DefaultCorsFilter:35` |
| `UserSessionFilterConfiguration` | `createSessionIfAbsentByPathMap` | `DefaultUserSessionFilter:198` |
| `AppbaseFilterConfig` | `allowedPaths` | `AppbaseServiceUtil.isFilterEnabled` |
| `AppbaseFilterConfig` | `blockedPaths` | `AppbaseServiceUtil.isFilterEnabled` |
| `AppbaseFilterConfig` | `allowedPathsByFilterTypeNameMap` † | `AppbaseServiceUtil.isFilterEnabled` |
| `AppbaseFilterConfig` | `blockedPathsByFilterTypeNameMap` † | `AppbaseServiceUtil.isFilterEnabled` |
| `AppbaseFilterConfig` | `allowedMethodsByPathMap` | `AppbaseServiceUtil.isFilterEnabled` |
| `AppbaseFilterConfig` | `blockedMethodsByPathMap` | `AppbaseServiceUtil.isFilterEnabled` |
| `AppbaseFilterConfig` | `allowedMethodsByPathsByFilterTypeNameMap` | `AppbaseServiceUtil.isFilterEnabled` |
| `AppbaseFilterConfig` | `blockedMethodsByPathsByFilterTypeNameMap` | `AppbaseServiceUtil.isFilterEnabled` |

**Correction (found in Task 3):** the outer key of a `...ByFilterTypeNameMap` map is a **filter class
name**, never a path — only the inner structure holds paths. This applies to FOUR maps, not the two
the original table implied:

- `allowedMethodsByPathsByFilterTypeNameMap`, `blockedMethodsByPathsByFilterTypeNameMap` — inner is a
  `Map<path, methods>`; canonicalize the inner map's keys.
- `allowedPathsByFilterTypeNameMap` †, `blockedPathsByFilterTypeNameMap` † — value is a `Set<path>`
  (`isFilterEnabled` does `map.get(filterTypeName)` then `set.contains(path)`, `AppbaseServiceUtil:244-248`);
  canonicalize the **set elements**.

In all four, the outer key is left verbatim. Canonicalizing it would prepend a slash to a class name
and silently break filter lookups — the same failure mode as canonicalizing an authorization
`Class#getName()` key (§4.1). The original table listing these two as plain path-keyed maps was wrong;
this is the corrected reading, verified against `AppbaseServiceUtil.isFilterEnabled`.

### 4.1 `DefaultAuthorizationServiceConfiguration` was dead, and is deleted

Found in Task 2, verified three ways:

1. **Nobody read it.** `DefaultAuthorizationService`'s constructor accepts the configuration, passes
   it to `super(...)`, and then initializes both of its own maps *empty*. Its
   `getAllowedOperationByServiceTypeNameMap()` / `getBlockedOperationByServiceTypeNameMap()` have
   zero callers in the repository. The service's maps are populated **only** by the runtime mutators
   `addAllowedService(...)`, `addAllowedServiceName(...)`, `addBlockedService(...)`,
   `addBlockedServiceName(...)`.
2. **Its builder stored `null` whenever configured.** The private constructor read
   `builder.x == null ? new ConcurrentHashMap<>() : null` — inverted. Supplying a map produced a
   `null` field; omitting it produced an empty map.
3. **The shape did not match.** `Map<String, String>` in the configuration versus
   `Map<String, Map<String, String>>` in the service. It could not have been wired even deliberately.

So an operator could author `allowedOperationByServiceTypeName` in YAML and it would load, parse, and
never affect an authorization decision. That is the same defect class this whole document exists to
close — configuration that is written, loaded, and silently never matched — except one layer earlier.
Deleted rather than repaired: repairing it means designing YAML-driven authorization config, wiring
it to the service, and matching the nested shape. That is a feature, not a bug fix, and nothing
depends on it (pre-1.0.0, no users, no migration).

**Consequence for this design:** authorization keys do not enter the canonicalizer through a
configuration class at all. The only path-shaped authorization keys are the `String` arguments to
`addAllowedServiceName(...)` / `addBlockedServiceName(...)`. The `Class<?>` overloads pass
`serviceType.getName()` and must never be canonicalized.

`DefaultAuthorizationService`'s `Class<?>` overloads (`:105`, `:118`) reduce to
`serviceType.getName()` — Java class names, not paths. **They never enter the canonicalizer.** The
service's two runtime maps are therefore keyed on *either* a canonical path *or* a class name,
depending on which overload the caller used. This spec does not unify that; it only guarantees that
path-shaped keys are canonical on both sides. The ambiguity is pre-existing and is called out here so
the implementer does not "fix" it by canonicalizing class names.

## 5. Design

One canonicalizer, applied at two boundaries, with idempotence as the invariant that makes double
application safe.

```
   servlet: getRequestURI() ──strip encoded ctx path─┐
   jax-rs:  uriInfo.getPath(false) ─────────────────►├──► canonicalize ──► canonical path ──┐
   netty:   queryStringDecoder.rawPath() ───────────►┘                                       │
                                                                                              ├─► containsKey
   config keys (10 maps) ────────────────────────────► canonicalize (at load) ──► canonical ─┘
```

Every face canonicalizes from the **encoded** form. Canonicalizing an already-decoded string is
unsound: `%2F` has become `/` and the evidence is gone. This is why the reactive face moves from
`fullPath()` to `rawPath()`.

Filters never call the canonicalizer. By the time a path reaches `DefaultCorsFilter`,
`DefaultAuthorizationFilter`, `DefaultUserSessionFilter`, or `isFilterEnabled`, it is canonical by
construction. A future face cannot forget a helper call, because there is no helper call to forget —
only a contract on `AppbaseServiceExchange#path()` and on the two servlet/JAX-RS entry points.

### 5.1 Canonical form

Given an encoded, context-relative path (revised 2026-07-10 after Task 1 — the original list was
incomplete on three counts, each of which cost a defect):

1. Reject a raw `?` or `#`. A query or fragment has no business in a security key, and the original
   `URI`-based decoder silently truncated at them.
2. Reject `%2F`, `%2f`, `%5C`, `%5c`, `%00` — octets that would decode into a character with
   structural meaning in the decoded string.
3. Collapse consecutive slashes: `//admin` → `/admin`. **Before decoding, not after.** Safe precisely
   because step 2 has already run, so every literal `/` is a real separator.
4. Percent-decode exactly once, UTF-8, with
   `URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8)`. Reject a malformed escape
   (`%zz`, truncated `%2`). The `+`-to-`%2B` escape neutralises `URLDecoder`'s form-encoding
   semantics, in which a raw `+` would decode to a space.
5. Reject a residual `%` in the decoded result. This is what makes idempotence true by construction:
   the output can contain nothing a second decode pass would reinterpret. Cost, accepted: a path
   carrying a literal percent cannot be a security key.
6. Resolve dot-segments per RFC 3986 §5.2.4: `/x/../admin` → `/admin`, `/x/./y` → `/x/y`, and — the
   part that is easy to get wrong — a terminal `.` or `..` leaves a trailing slash, so
   `/admin/.` → `/admin/` and `/a/b/..` → `/a/`.
7. Ensure a leading `/`; an empty result becomes `/`.

Not done, deliberately: **no trailing-slash folding** (`/admin/` and `/admin` remain distinct keys)
and **no case folding**. Both would change matching semantics beyond the scope of this fix, and both
are defensible either way. Recorded here so the choice is visible rather than accidental.

Note how steps 6 and "no trailing-slash folding" interact. Because `/admin/` is a distinct key an
operator can block, a non-RFC `removeDotSegments` that dropped the trailing slash would let
`GET /admin/.` canonicalize to `/admin` and slip past that rule. That is a *partial* fold, and it was
a live bypass in the first implementation.

**On the decoder.** An earlier draft of this spec mandated `new URI(p).getPath()` and forbade
`URLDecoder`. That was reversed during Task 1. `URI` parses a leading `//` as an RFC 3986 authority
(`new URI("//admin")` → `authority=admin, path=""`), silently truncates at `?`/`#`, and rejects its
own decoded output for any URI-illegal character — which made `canonicalize` non-idempotent
(`/a%20b` → `/a b`, then canonicalizing `/a b` throws). Two of the three were bypasses.

Rationale for step 2 over decoding-then-matching: an encoded separator must never smuggle a segment
boundary into a key. Rationale for step 5 over rejecting traversal: resolving means `/x/../admin`
lands on the `/admin` rule and is evaluated against real config, rather than erroring out and telling
a prober that the input was interesting. This matches Tomcat and Jetty defaults
(`ALLOW_ENCODED_SLASH=false`) and Spring's `StrictHttpFirewall`.

**Invariant:** `canonicalize(canonicalize(x)) == canonicalize(x)`. This is what permits applying it
at both the face and the config-load boundary without coordination. It is a property test, not a
comment.

The `*` wildcard sentinel (`AppbaseServiceConstants.WILDCARD`) is never canonicalized.

### 5.2 New types (`appbase-service-core`, package `...service.path`)

```java
/**
 * Converts a transport-native, percent-encoded request path into the single canonical form
 * used as the key for every path-keyed security configuration in Appbase.
 *
 * @author Navaneeth Mysore G
 */
public final class AppbasePathCanonicalizer {

    /** @throws AppbasePathException on ?, #, %2F, %5C, %00, a malformed escape, or a residual % */
    public static String canonicalize(String encodedPath);

    /** Removes an encoded context-path prefix. Both arguments are encoded. */
    public static String stripContextPath(String encodedRequestUri, String encodedContextPath);

    /** Canonicalizes path keys of a configuration map. Skips the `*` sentinel and any key
     *  not starting with `/` (the authorization maps also hold Class#getName() keys). */
    public static <V> Map<String, V> canonicalizeKeys(Map<String, V> source);
}
```

```java
/** @author Navaneeth Mysore G */
public class AppbasePathException extends AppbaseRuntimeException { }
```

**Unchecked, not checked.** A checked exception forces `build()` to declare `throws`, which fights
Jackson's `@JsonPOJOBuilder` contract on three of the four configuration classes, and forces a checked
catch inside the reactor-netty `handle(...)` lambda. The project convention prefers unchecked unless
callers must handle recovery; here they catch it explicitly at two well-defined boundaries.

One `canonicalize` method serves both boundaries. Only the caller's reaction to the exception
differs: 400 at request time, startup failure at config-load time.

### 5.3 Authorization semantics

`DefaultAuthorizationService`, both overloads:

```java
// allowlist populated, service absent  ->  deny unless wildcard
// (today: falls through to contextAuthorizer, i.e. an allowlist that never denies)
if (!allowedOperationsByServiceTypeNameMap.isEmpty()
        && !allowedOperationsByServiceTypeNameMap.containsKey(service)) {
    return allowedOperationsByServiceTypeNameMap.containsKey(WILDCARD);
}
```

Resulting semantics. This table is the seed of the configuration guide and should be lifted verbatim.

| Configuration | Path matches | Result |
|---|---|---|
| neither map populated | — | delegate to `ContextAuthorizer` |
| blocklist only | listed | **deny** |
| blocklist only | not listed | delegate to `ContextAuthorizer` |
| allowlist populated | listed | allow |
| allowlist populated | **not listed** | **deny** *(changed; today: allow)* |
| both populated | in blocklist | **deny** (blocklist wins) |
| both populated | in allowlist only | allow |
| both populated | in neither | **deny** |
| `*` in blocklist | any | **deny** |
| `*` in allowlist, path unlisted | any | allow |

The unconfigured pass-through is preserved on purpose. `AuthorizationWebFilter`'s javadoc records
why: the filter binds to `/*` on every deployment, so denying-when-unconfigured would 403 every
Appbase app on first boot.

"Blocklist only → not listed → delegate" is not a bug. A blocklist means *deny what is listed, allow
the rest*; delegation is how "the rest" is decided.

### 5.4 Changed call sites

| Face | File | Change |
|---|---|---|
| reactive | `NettyWebExchange:93,103` | `fullPath()` → canonicalize(`queryStringDecoder.rawPath()`), computed once in the constructor |
| reactive | `ReactorNettyAppbaseWebServer:105-113` | catch `AppbasePathException` from the constructor, write 400 via reactor-netty's `response` (no `exchange` exists yet) |
| servlet | `CorsServletFilter:53` | `getRequestURI()` → strip + canonicalize |
| servlet | `UserSessionServletFilter:96` | same |
| servlet | `AuthorizationServletFilter` | same |
| servlet | `AbstractAppbaseServletFilter:62` | pass canonical path to `isFilterEnabled` |
| JAX-RS | `CorsRequestJaxrsFilter`, `CorsResponseJaxrsFilter` | `uriInfo.getPath()` → canonicalize(`getPath(false)`) |
| JAX-RS | `UserSessionJaxrsRequestFilter:99` | same |
| JAX-RS | `AbstractAppbaseJaxrsFilter:90` | same |
| config | the four classes in §4 | canonicalize path keys at build, skipping `*` |
| rename | `DefaultContextAuthorizer` → `PermitAllContextAuthorizer` | javadoc warns it authorizes every request; `withContextAuthorizer(...)` javadoc notes passing it disables path authorization |

Servlet strip-then-decode order matters: `getRequestURI()` and `getContextPath()` are both encoded, so
the prefix is removed on encoded strings and the remainder is decoded once. Reversing this
reintroduces the `%2F` hazard.

### 5.5 Error handling

At request time `AppbasePathException` becomes a 400 **before any filter runs**, so authorization
never sees a path that could not be canonicalized.

- reactive: caught in `HttpServer.handle(...)` (fact 10). **No new filter is added; the seven-filter
  order invariant is untouched and `ReactiveFilterChainOrderTest` does not change.**
- servlet / JAX-RS: caught in the first Appbase filter, which sends 400 and does not continue.

At config-load time the exception propagates out of the configuration builder and fails startup, with
a message naming the offending key. A rule that can never match is a misconfiguration; accepting it
silently is the trap this spec removes.

## 6. Testing

TDD, red first, per the project's three-phase workflow.

**Canonicalizer** — table-driven:

| Input | Expected |
|---|---|
| `/%61dmin` | `/admin` |
| `/a%2Fb`, `/a%2fb` | throw |
| `/a%5Cb` | throw |
| `/a%00b` | throw |
| `/a%zz` | throw |
| `/a%2` | throw |
| `/x/../admin` | `/admin` |
| `/x/./y` | `/x/y` |
| `//admin` | `/admin` |
| `` (empty) | `/` |
| `/admin/` | `/admin/` (no folding) |
| `/Admin` | `/Admin` (no case folding) |

Plus an idempotence property over every row: `canonicalize(canonicalize(x)) == canonicalize(x)`.

**Authorization semantics** — one test per row of §5.3. The `allowlist populated / not listed → deny`
row is the regression test for D2.

**Bypass regressions** — a blocklist entry `/admin` denies `GET /%61dmin` (servlet face; this is D1)
and denies `GET /x/../admin`.

**Face agreement** — the same wire request `GET /%61dmin` driven through servlet, JAX-RS, and
reactive yields one canonical key. This **replaces** the Task 3b test that currently pins the
divergence: it flips from "these differ, deliberately" to "these agree."

**Config load** — a YAML key of `/a%2Fb` fails startup with a message naming the key. A key of `*`
survives untouched.

## 7. Delivery

Own sub-issue under internal work item 340, own branch off `feature/P-027-R1-reactive-web`, PR back into it. Not folded
into PR 2/3: a security-semantics change reviewed alongside route-registry plumbing is a change
nobody reviews properly.

Lands **before Task 5**. `ReactorNettyAppbaseWebServer:107` already does
`routeRegistry.lookup(exchange.method(), exchange.path())`, so Task 5's registry is one more
consumer of the key space and should be born canonical.

Documentation: rationale in javadoc at each decision site (project convention — decisions live in the
code, not only in the spec); the §5.3 semantics table and the §5.1 canonical form seeded into `docs/`
for the configuration guide; the ledger's OPEN DECISION block rewritten to record what was found,
including that the servlet/JAX-RS split predates R1.

## 8. Known risks

- **`isFilterEnabled` is fail-safe on key miss** (unconfigured → `true`). Canonicalizing its eight
  keys changes which requests filters run for. *Verified 2026-07-10:* its twelve tests in
  `AppbaseServiceUtilTest` use the single path literal `/path1`, which is already canonical. No test
  churn expected. Risk retired.
- **`stripContextPath` is new behavior with no existing coverage, and is currently unreachable.**
  *Verified 2026-07-10:* `getContextPath` is called nowhere in the repository, and all three servlet
  containers mount at root and cannot be configured otherwise — Undertow `setContextPath("/")`
  (`UndertowAppbaseWebServer:258`), Jetty `setContextPath("/")` (`JettyAppbaseWebServer:476`), Tomcat
  `addContext("", ...)` (`TomcatAppbaseWebServer:253`). Jetty's `withSwaggerUI(contextPath, ...)`
  installs a separate `ContextHandler` and never reaches these filters.

  Consequence: **the context-path half of D3 cannot occur today.** `getRequestURI()` already equals
  the context-relative path. What remains live in D3 is the encoding difference alone (servlet
  encoded; JAX-RS and reactive decoded), which is D1's mechanism.

  `stripContextPath` stays in scope as hardening: the day someone adds a configurable mount, every
  path-keyed security rule would silently stop matching. That is the precise failure this spec
  exists to prevent. It is not, however, repairing anything that is broken now.

  Implementation note: per the servlet specification a root context's `getContextPath()` returns
  `""`, not `"/"` (Jetty normalizes its `"/"` accordingly). An empty prefix is the normal case, not
  an edge case.
- **`stripContextPath` on a request whose URI does not start with the context path** should not occur
  per the servlet contract, but the implementation must decide: throw, or return the URI unchanged.
  Recommendation: throw — a violated container invariant is not something to paper over.
- **The only percent-encoded literal in the test suite** is `CorsWebFilterTest:146`, in the assertion
  message of the Task 3b divergence pin — the test §6 flips to an agreement test. Nothing else
  depends on encoded paths.
- The two `...ByFilterTypeNameMap` maps have a **nested** key structure. The implementer must
  canonicalize inner keys only. Getting this wrong silently disables filter overrides.
