# Canonical Path Key Space and Authorization Semantics — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every Appbase transport face one canonical path key space, and make a populated authorization allowlist actually deny non-members.

**Architecture:** One canonicalizer in `appbase-service-core`, applied at two boundaries — each transport face (request side) and each configuration class's private constructor (rule side). Idempotence is the invariant that makes double application safe. Filters never call the canonicalizer, so a future face has no helper to forget.

**Tech Stack:** Java 25, JUnit Jupiter 5, Mockito, Maven. `URLDecoder` for decoding, with `+` escaped to `%2B` first (see amendment A4 — `java.net.URI` was tried and abandoned). No new dependencies.

**Spec:** `docs/specs/2026-07-10-P027-R1-path-canonicalization-authz-semantics-design.md` (commits `1ac1f1878`, `ed4657c9b`, `3f280d7ac`)

## Global Constraints

- TDD, red-green-refactor. Use `tdd-test-writer` / `tdd-implementer` / `tdd-refactorer` sub-agents per phase.
- No fully-qualified class names in code. Import instead.
- `@author Navaneeth Mysore G` on every new class.
- All dependency versions live in the root `dependencyManagement` only. This plan adds no dependencies.
- Non-obvious design decisions get Javadoc rationale **at the code site**, not only in the spec.
- Fast feedback: `./build.sh quick`. Before pushing: `./build.sh develop`.
- Branch off `feature/P-027-R1-reactive-web`; PR back into it. Lands **before** R1 Task 5.
- **`ReactiveFilterChainOrderTest` must not change.** No new filter is added. Needing to touch it means the design was misread — stop and escalate.
- Apache 2.0 licence header on every new file, matching `AuthorizationWebFilter.java`.

## Amendments (user-approved 2026-07-10, after a pre-flight review of this plan)

Three places where this plan, as first written, mandated something the review rubric correctly
treats as a defect. All three are fixed in the task text below.

**A1 — Task 3 no longer duplicates `canonicalizeKeys` into four classes.** The original reason for
duplicating ("the four classes live in two modules") was wrong: `AppbaseFilterConfig` and
`DefaultAuthorizationServiceConfiguration` are in `appbase-service-core`, and the other two are in
modules that already depend on it. The helper is a static method on `AppbasePathCanonicalizer`,
written once in Task 1. A four-way copy of a security-relevant loop diverges the first time anyone
fixes a bug in one copy.

**A2 — Task 6's test no longer asserts against a mock.** The original stubbed `UriInfo.getPath(false)`
and then canonicalized the stub's return value, which would pass even if the filter still called
`getPath()`. It now drives `UserSessionJaxrsRequestFilter` and captures what reaches the neutral
`UserSessionFilter.onRequest(...)`.

**A4 — the decoder is `URLDecoder`, not `URI` (user decision, after Task 1 review).** This REVERSES
correction 2 below and the code in Task 1 Step 3, both of which are left in place as the record of
what was tried. `java.net.URI` produced three defects in one task:

1. It parsed a leading `//` as an RFC 3986 authority — `new URI("//admin")` yields `authority=admin,
   path=""` — so `//admin` canonicalized to `/` and missed an `/admin` blocklist entry. A bypass.
2. It silently truncated at a raw `?` or `#`, discarding input.
3. It rejects its own decoded output for any URI-illegal character, which made `canonicalize`
   **non-idempotent**: `/a%20b` → `/a b`, and canonicalizing `/a b` throws. Same for `%09`, `%22`,
   `%3C`, `%5B`, `%7B`. The two-boundary design leans on idempotence.

The decoder is now `URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8)`. The
`+`-means-space hazard that originally banned `URLDecoder` is neutralised by escaping `+` first —
guarded by the `/a+b → /a+b` test row. Idempotence is now true by construction: the output can
contain no `%` (residual `%` is rejected) and no re-interpretable `+`. Verified by a 300,000-case
fuzz over an alphabet including `%20`, `%09`, `%3C`, `%7F`, `%0A`, `+`, `%2B`, and raw non-ASCII:
199,775 accepted, zero idempotence violations, zero outputs containing `%`.

Two further rules the spec's §5.1 does not yet list, both added in Task 1: **`removeDotSegments`
follows RFC 3986 §5.2.4 faithfully**, so a terminal `.` or `..` leaves a trailing slash
(`/admin/.` → `/admin/`) — without this, `/admin/.` slipped past a configured `/admin/` key, a second
bypass. And **a raw `?` or `#`, or a residual `%` after decoding, is rejected.**

**A5 — Task 3 drops `DefaultAuthorizationServiceConfiguration` (found in Task 2).** Its two maps were
never read, its builder stored `null` when configured, and its shape never matched the service's.
Deleted rather than canonicalized. Ten maps across three classes remain, and authorization's only
path-shaped keys are the `String` arguments to `addAllowedServiceName` / `addBlockedServiceName`.

**A3 — Task 8's three-face agreement test is deleted, not weakened.** The original compared
`canonicalize(x)` with `canonicalize(x)` — two identical expressions — and proved only determinism,
which Task 1's idempotence property already covers. A real cross-face test is impossible in one
module: `appbase-bootstrap-reactor-netty` can see the servlet and JAX-RS filters (via
`appbase-service-web`) but **`jakarta.servlet-api` and `jakarta.ws.rs-api` are not on its classpath**,
and adding them would pull the servlet API into the one module defined by not having it.

Agreement is instead established by three real assertions, each in the module that owns its face,
each against the same wire path `/%61dmin` and the same expected key `/admin`: Task 4
(`NettyWebExchange.path()`), Task 5 (what `AuthorizationServletFilter` passes to `isAuthorized`),
Task 6 (what `UserSessionJaxrsRequestFilter` passes to `onRequest`). If those three literals ever
drift apart, the faces have diverged. A comment in each test names the other two.

## Corrections to the spec, discovered during planning

Both are already reflected below. **Folded back into the spec (Task 8, 2026-07-10) — done.**

1. **`AppbasePathException` is unchecked**, extending `AppbaseRuntimeException`, not `AppbaseException`. The spec said checked. A checked exception forces `build()` to declare `throws`, which fights Jackson's `@JsonPOJOBuilder` contract, and forces a checked catch inside the reactor-netty `handle(...)` lambda. Project convention prefers unchecked.

2. ~~**`URLDecoder` must not be used.**~~ **SUPERSEDED BY AMENDMENT A4 — see above.** The `+`-means-space hazard is real, and is neutralised by escaping `+` to `%2B` before decoding. `java.net.URI` turned out to be the worse choice: it parses a leading `//` as an authority, truncates at `?`/`#`, and rejects its own decoded output, breaking idempotence. Left here as the record of what was tried and why it was abandoned. `AppbaseServiceUtil:287` uses `URLDecoder` for *query parameters*, where form-encoding semantics are correct.

Also learned: all four configuration classes have a **private constructor taking their Builder**, and three of them carry `@JsonDeserialize(builder = ...Builder.class)`. That constructor is therefore the single canonicalization point covering both YAML and programmatic creation.

## File Structure

**Create**
- `appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/path/AppbasePathCanonicalizer.java` — the canonicalizer. Static, stateless.
- `appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/path/AppbasePathException.java` — unchecked failure signal.
- `appbase-service/appbase-service-core/src/test/java/com/viveworks/appbase/service/path/AppbasePathCanonicalizerTest.java`
- `appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/authorization/PermitAllContextAuthorizer.java` — renamed from `DefaultContextAuthorizer`.
- `docs/configuration/authorization-and-path-matching.md` — guide seed.

**Modify**
- `.../service/authorization/DefaultAuthorizationService.java` — allowlist denies non-members (both overloads); canonicalize the `String` mutators (Task 3).
- `.../service/authorization/DefaultAuthorizationServiceConfiguration.java` — canonicalize two maps.
- `.../service/web/cors/CorsFilterConfiguration.java` — canonicalize `pathByCorsHeadersMap`.
- `.../service/user/session/web/filter/UserSessionFilterConfiguration.java` — canonicalize `createSessionIfAbsentByPathMap`.
- `.../service/web/filter/AppbaseFilterConfig.java` — canonicalize eight maps; nested maps' **inner** keys only.
- `appbase-bootstrap-reactor-netty/.../server/NettyWebExchange.java` — `rawPath()` + canonicalize in constructor.
- `appbase-bootstrap-reactor-netty/.../server/ReactorNettyAppbaseWebServer.java` — catch, emit 400.
- `.../service/web/cors/CorsServletFilter.java`, `.../filter/servlet/AbstractAppbaseServletFilter.java`, `.../session/web/filter/servlet/UserSessionServletFilter.java`, `.../authorization/filter/servlet/AuthorizationServletFilter.java` — strip + canonicalize.
- `.../service/web/cors/CorsRequestJaxrsFilter.java`, `CorsResponseJaxrsFilter.java`, `.../filter/jaxrs/AbstractAppbaseJaxrsFilter.java`, `.../session/web/filter/jaxrs/UserSessionJaxrsRequestFilter.java` — `getPath(false)` + canonicalize.
- `.../service/web/reactive/CorsWebFilterTest.java` — divergence pin becomes agreement test.
- Rewrite the historical OPEN DECISION block in the design ledger.

---

### Task 1: The canonicalizer

Everything else depends on this. Nothing depends on Task 2, so Tasks 1 and 2 may run in parallel.

**Files:**
- Create: `appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/path/AppbasePathException.java`
- Create: `appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/path/AppbasePathCanonicalizer.java`
- Test: `appbase-java/appbase-service/appbase-service-core/src/test/java/com/viveworks/appbase/service/path/AppbasePathCanonicalizerTest.java`

**Interfaces:**
- Consumes: `AppbaseRuntimeException`, `AppbaseUtil.notNull`
- Produces:
  - `AppbasePathCanonicalizer.canonicalize(String encodedPath) -> String` (throws unchecked `AppbasePathException`)
  - `AppbasePathCanonicalizer.stripContextPath(String encodedRequestUri, String encodedContextPath) -> String`
  - `AppbasePathCanonicalizer.canonicalizeKeys(Map<String, V> source) -> Map<String, V>` (amendment A1 — written once here, used by all four configuration classes in Task 3)
  - `AppbasePathException(String message)`

- [ ] **Step 1: Write the failing test**

```java
package com.viveworks.appbase.service.path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppbasePathCanonicalizerTest {

    @ParameterizedTest
    @CsvSource({
            "/%61dmin,      /admin",
            "/x/../admin,   /admin",
            "/x/./y,        /x/y",
            "//admin,       /admin",
            "/a+b,          /a+b",
            "/admin/,       /admin/",
            "/Admin,        /Admin",
            "/../admin,     /admin"
    })
    void canonicalizesToTheExpectedKey(final String encoded, final String expected) {
        assertEquals(expected, AppbasePathCanonicalizer.canonicalize(encoded));
    }

    @Test
    void anEmptyPathBecomesRoot() {
        assertEquals("/", AppbasePathCanonicalizer.canonicalize(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a%2Fb", "/a%2fb", "/a%5Cb", "/a%5cb", "/a%00b", "/a%zz", "/a%2"})
    void rejectsStructuralOctetsAndMalformedEscapes(final String encoded) {
        assertThrows(AppbasePathException.class, () -> AppbasePathCanonicalizer.canonicalize(encoded));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/%61dmin", "/x/../admin", "//admin", "/a+b", "/admin/", ""})
    void canonicalizationIsIdempotent(final String encoded) {
        final String once = AppbasePathCanonicalizer.canonicalize(encoded);
        assertEquals(once, AppbasePathCanonicalizer.canonicalize(once));
    }

    @Test
    void stripsAnEncodedContextPathPrefix() {
        assertEquals("/admin", AppbasePathCanonicalizer.stripContextPath("/myapp/admin", "/myapp"));
    }

    @Test
    void anEmptyContextPathIsTheNormalRootCase() {
        assertEquals("/admin", AppbasePathCanonicalizer.stripContextPath("/admin", ""));
    }

    @Test
    void aRequestUriNotUnderTheContextPathIsAViolatedContainerInvariant() {
        assertThrows(AppbasePathException.class,
                () -> AppbasePathCanonicalizer.stripContextPath("/other/admin", "/myapp"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-core -Dtest=AppbasePathCanonicalizerTest -P quick-profile`
Expected: FAIL — compilation error, `AppbasePathCanonicalizer` does not exist.

- [ ] **Step 3: Write minimal implementation**

`AppbasePathException.java` (Apache 2.0 header omitted here for brevity — copy it from `AuthorizationWebFilter.java`):

```java
package com.viveworks.appbase.service.path;

import com.viveworks.appbase.core.AppbaseRuntimeException;

/**
 * Signals that a request path or a configured path key cannot be reduced to the canonical form.
 *
 * <p>Unchecked on purpose. The configuration classes canonicalize inside a Jackson
 * {@code @JsonPOJOBuilder}-driven constructor, which cannot declare a checked exception without
 * fighting Jackson's builder contract, and the reactive face constructs its exchange inside a
 * reactor-netty lambda. Both callers still catch it explicitly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbasePathException extends AppbaseRuntimeException {

    public AppbasePathException(final String message) {
        super(message);
    }
}
```

`AppbasePathCanonicalizer.java`:

```java
package com.viveworks.appbase.service.path;

import com.viveworks.appbase.core.util.AppbaseUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Reduces a transport-native, percent-encoded request path to the single canonical form used as the
 * key for every path-keyed security configuration in Appbase.
 *
 * <p>Three faces spell the same request three ways: the servlet container does not decode
 * {@code getRequestURI()}, JAX-RS decodes {@code UriInfo.getPath()}, and reactor-netty decodes
 * {@code fullPath()}. Matching security rules against any of those directly is unsafe in both
 * directions -- a raw {@code /%61dmin} slips past a {@code /admin} blocklist entry, while a naively
 * decoded {@code /a%2Fb} collapses onto {@code /a/b}. This class removes the ambiguity once, and
 * configuration keys are pushed through it too, so both sides of every lookup share one key space.
 *
 * <p><strong>{@link URI} is the decoder, deliberately not {@code URLDecoder}.</strong>
 * {@code URLDecoder} implements {@code application/x-www-form-urlencoded}, in which {@code +} means
 * a space. In a URI path {@code +} is a literal plus, so decoding {@code /a+b} with
 * {@code URLDecoder} yields {@code /a b} and the key never matches. {@code URI.getPath()} decodes
 * escaped octets, leaves {@code +} alone, and rejects a malformed escape. {@code AppbaseServiceUtil}
 * uses {@code URLDecoder} for query parameters, where it is the correct choice.
 *
 * <p>Deliberately NOT done: trailing-slash folding ({@code /admin/} and {@code /admin} remain
 * distinct keys) and case folding. Both would change matching semantics beyond the scope of the
 * defect this class fixes.
 *
 * <p>Canonicalization is idempotent: {@code canonicalize(canonicalize(x)).equals(canonicalize(x))}.
 * That is what allows it to be applied at the transport face and again at configuration load without
 * the two coordinating.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AppbasePathCanonicalizer {

    private static final String[] STRUCTURAL_OCTETS = {"%2f", "%5c", "%00"};

    private AppbasePathCanonicalizer() {
    }

    /**
     * @param encodedPath a percent-encoded, context-relative path
     * @return the canonical key
     * @throws AppbasePathException on an encoded separator, an encoded NUL, or a malformed escape
     */
    public static String canonicalize(final String encodedPath) {
        AppbaseUtil.notNull(encodedPath, "encodedPath");

        rejectStructuralOctets(encodedPath);
        final String decoded = decodeOnce(encodedPath);
        final String collapsed = decoded.replaceAll("/{2,}", "/");
        final String resolved = removeDotSegments(collapsed);

        return resolved.startsWith("/") ? resolved : "/" + resolved;
    }

    /**
     * Removes an encoded context-path prefix. Both arguments are still encoded: the servlet
     * container returns {@code getRequestURI()} and {@code getContextPath()} in encoded form, and
     * stripping must happen before decoding or an encoded separator inside the prefix could shift
     * the boundary.
     *
     * <p>A root context returns {@code ""} from {@code getContextPath()}, not {@code "/"}, so an
     * empty prefix is the normal case.
     *
     * @throws AppbasePathException when the request URI does not start with the context path, which
     *         the servlet contract forbids and which is not safe to paper over
     */
    public static String stripContextPath(final String encodedRequestUri, final String encodedContextPath) {
        AppbaseUtil.notNull(encodedRequestUri, "encodedRequestUri");
        AppbaseUtil.notNull(encodedContextPath, "encodedContextPath");

        if (encodedContextPath.isEmpty() || "/".equals(encodedContextPath)) {
            return encodedRequestUri;
        }
        if (!encodedRequestUri.startsWith(encodedContextPath)) {
            throw new AppbasePathException(
                    "request URI '" + encodedRequestUri + "' is not under context path '" + encodedContextPath + "'");
        }

        return encodedRequestUri.substring(encodedContextPath.length());
    }

    private static void rejectStructuralOctets(final String encodedPath) {
        final String lower = encodedPath.toLowerCase(Locale.ROOT);
        for (final String octet : STRUCTURAL_OCTETS) {
            if (lower.contains(octet)) {
                throw new AppbasePathException(
                        "encoded path '" + encodedPath + "' contains the structural octet '" + octet + "'");
            }
        }
    }

    private static String decodeOnce(final String encodedPath) {
        try {
            final String path = new URI(encodedPath).getPath();
            if (path == null) {
                throw new AppbasePathException("encoded path '" + encodedPath + "' has no path component");
            }
            return path;
        } catch (final URISyntaxException e) {
            throw new AppbasePathException("encoded path '" + encodedPath + "' is not a valid URI: " + e.getMessage());
        }
    }

    /** RFC 3986 section 5.2.4. */
    private static String removeDotSegments(final String path) {
        final Deque<String> out = new ArrayDeque<>();
        for (final String segment : path.split("/", -1)) {
            if (".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                out.pollLast();
                continue;
            }
            out.addLast(segment);
        }

        final String joined = String.join("/", out);
        return joined.isEmpty() ? "/" : joined;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-core -Dtest=AppbasePathCanonicalizerTest -P quick-profile`
Expected: PASS, all rows.

If `/admin/` fails by losing its trailing slash: `split("/", -1)` keeps the trailing empty segment; verify the `-1` limit is present. If `""` fails: `removeDotSegments` returns `/` when the join is empty.

- [ ] **Step 5: Verify the mutation is caught**

Break the implementation deliberately — swap `new URI(encodedPath).getPath()` for `URLDecoder.decode(encodedPath, StandardCharsets.UTF_8)`. Re-run.
Expected: `canonicalizesToTheExpectedKey` FAILS on the `/a+b` row with `expected: </a+b> but was: </a b>`.
Restore the implementation. This proves the row is load-bearing rather than decorative.

- [ ] **Step 6: Commit**

```bash
git add appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/path/ \
        appbase-java/appbase-service/appbase-service-core/src/test/java/com/viveworks/appbase/service/path/
git commit -m "feat(P-027-R1): canonical path key space

One canonicalizer, applied at the transport face and again at configuration
load. Idempotent, so the two need not coordinate. Rejects encoded separators
and malformed escapes; resolves dot-segments and duplicate slashes. Decodes
with URI rather than URLDecoder, which would turn a literal + into a space."
```

---

### Task 2: A populated allowlist denies non-members

Independent of Task 1. Fixes spec defect D2.

**Files:**
- Modify: `appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/authorization/DefaultAuthorizationService.java` (two-arg overload at `:67`, three-arg at `:122`)
- Test: `appbase-java/appbase-service/appbase-service-user/src/test/java/com/viveworks/appbase/service/authorization/DefaultAuthorizationServiceTest.java`

**Interfaces:**
- Consumes: `AppbaseServiceConstants.WILDCARD` (`"*"`), `ContextAuthorizer`
- Produces: no new signatures. Behavior change only.

- [ ] **Step 1: Write the failing test**

Append to `DefaultAuthorizationServiceTest`. The existing tests construct the service with `new DefaultContextAuthorizer()` — after Task 7 that becomes `PermitAllContextAuthorizer`. Use whichever name exists when you run.

```java
    @Test
    void aPopulatedAllowlistDeniesAPathThatIsNotAMember() {
        final DefaultAuthorizationServiceConfiguration configuration =
                DefaultAuthorizationServiceConfiguration.builder()
                        .allowedOperationsByServiceTypeNameMap(Map.of("/admin", new MapOfStringStringValue(Map.of())))
                        .build();
        final DefaultAuthorizationService service =
                new DefaultAuthorizationService(configuration, new DefaultContextAuthorizer());

        assertFalse(service.isAuthorized("/secrets", new AuthorizationContext()),
                "an allowlist that does not deny non-members is not an allowlist");
    }

    @Test
    void aPopulatedAllowlistAllowsItsMembers() {
        final DefaultAuthorizationServiceConfiguration configuration =
                DefaultAuthorizationServiceConfiguration.builder()
                        .allowedOperationsByServiceTypeNameMap(Map.of("/admin", new MapOfStringStringValue(Map.of())))
                        .build();
        final DefaultAuthorizationService service =
                new DefaultAuthorizationService(configuration, new DefaultContextAuthorizer());

        assertTrue(service.isAuthorized("/admin", new AuthorizationContext()));
    }

    @Test
    void aWildcardAllowlistAllowsAnUnlistedPath() {
        final DefaultAuthorizationServiceConfiguration configuration =
                DefaultAuthorizationServiceConfiguration.builder()
                        .allowedOperationsByServiceTypeNameMap(
                                Map.of(AppbaseServiceConstants.WILDCARD, new MapOfStringStringValue(Map.of())))
                        .build();
        final DefaultAuthorizationService service =
                new DefaultAuthorizationService(configuration, new DefaultContextAuthorizer());

        assertTrue(service.isAuthorized("/anything", new AuthorizationContext()));
    }

    @Test
    void aBlocklistOnlyConfigurationStillDelegatesForAnUnlistedPath() {
        final DefaultAuthorizationServiceConfiguration configuration =
                DefaultAuthorizationServiceConfiguration.builder()
                        .blockedOperationsByServiceTypeNameMap(Map.of("/admin", new MapOfStringStringValue(Map.of())))
                        .build();
        final DefaultAuthorizationService service =
                new DefaultAuthorizationService(configuration, new DefaultContextAuthorizer());

        assertTrue(service.isAuthorized("/public", new AuthorizationContext()),
                "a blocklist denies what it lists and delegates the rest; this is not the allowlist bug");
    }

    @Test
    void anUnconfiguredServiceStillDelegatesSoAFreshAppDoesNotForbidEveryRequest() {
        final DefaultAuthorizationServiceConfiguration configuration =
                DefaultAuthorizationServiceConfiguration.builder().build();
        final DefaultAuthorizationService service =
                new DefaultAuthorizationService(configuration, new DefaultContextAuthorizer());

        assertTrue(service.isAuthorized("/anything", new AuthorizationContext()));
    }

    @Test
    void withBothListsTheBlocklistWins() {
        final DefaultAuthorizationServiceConfiguration configuration =
                DefaultAuthorizationServiceConfiguration.builder()
                        .allowedOperationsByServiceTypeNameMap(Map.of("/admin", new MapOfStringStringValue(Map.of())))
                        .blockedOperationsByServiceTypeNameMap(Map.of("/admin", new MapOfStringStringValue(Map.of())))
                        .build();
        final DefaultAuthorizationService service =
                new DefaultAuthorizationService(configuration, new DefaultContextAuthorizer());

        assertFalse(service.isAuthorized("/admin", new AuthorizationContext()));
    }
```

Check the builder setter names and `MapOfStringStringValue`'s constructor against
`DefaultAuthorizationServiceConfiguration.java` before running; adjust if they differ.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-user -Dtest=DefaultAuthorizationServiceTest -P quick-profile`
Expected: `aPopulatedAllowlistDeniesAPathThatIsNotAMember` FAILS —
`an allowlist that does not deny non-members is not an allowlist ==> expected: <false> but was: <true>`.
The other five PASS. Only the first is the bug.

- [ ] **Step 3: Write minimal implementation**

In the **two-arg** overload, replace lines 88–95:

```java
        // If only blocked list is used, do not check allowed list
        if (!allowedOperationsByServiceTypeNameMap.isEmpty() &&
                !allowedOperationsByServiceTypeNameMap.containsKey(service))
        {
            if (allowedOperationsByServiceTypeNameMap.containsKey(AppbaseServiceConstants.WILDCARD)) {
                return true;
            }
        }
```

with:

```java
        // A populated allowlist denies whatever it does not list. Reaching contextAuthorizer here
        // would make "configured but unmatched" indistinguishable from "unconfigured", which is how
        // an allowlist silently stopped denying anything. A blocklist-only configuration falls
        // through on purpose: a blocklist denies what it lists and delegates the rest.
        if (!allowedOperationsByServiceTypeNameMap.isEmpty() &&
                !allowedOperationsByServiceTypeNameMap.containsKey(service))
        {
            return allowedOperationsByServiceTypeNameMap.containsKey(AppbaseServiceConstants.WILDCARD);
        }
```

Apply the identical change to the **three-arg** overload. The two carry parallel logic; leaving one behind reintroduces the bug on whichever call path uses it.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-user -Dtest=DefaultAuthorizationServiceTest -P quick-profile`
Expected: PASS, including the pre-existing tests. If a pre-existing test now fails, it was asserting the bug — read it, and escalate rather than editing it to match.

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/authorization/DefaultAuthorizationService.java \
        appbase-java/appbase-service/appbase-service-user/src/test/java/com/viveworks/appbase/service/authorization/DefaultAuthorizationServiceTest.java
git commit -m "fix(P-027-R1): a populated allowlist denies non-members

The allowlist branch could only return true, on a wildcard key. Any other
unlisted path fell through to the context authorizer -- the same call an
unconfigured service makes. Configured-but-unmatched was indistinguishable
from unconfigured, so an allowlist never denied anything.

Blocklist-only still delegates: a blocklist denies what it lists and allows
the rest. Both isAuthorized overloads carry the logic and both are fixed."
```

---

### Task 3: Configuration keys canonicalize at load

Depends on Task 1.

**SCOPE REDUCED during Task 2 (amendment A5).** `DefaultAuthorizationServiceConfiguration`'s two maps
are **not** in scope: they were never read by `DefaultAuthorizationService` (which populates its own
maps only via `addAllowedService*` / `addBlockedService*`), their builder stored `null` whenever a map
was supplied, and their `Map<String, String>` shape never matched the service's
`Map<String, Map<String, String>>`. They are deleted, not canonicalized. See spec §4.1.

Ten maps across three classes remain. Authorization keys therefore do **not** enter the canonicalizer
through a configuration class at all — the only path-shaped authorization keys are the `String`
arguments to `addAllowedServiceName(...)` / `addBlockedServiceName(...)`. Canonicalize **those two
mutators**, not the deleted config. The `Class<?>` overloads pass `serviceType.getName()` and must
never be canonicalized; `canonicalizeKeys`'s `startsWith("/")` guard does not protect a mutator, so
guard the call site explicitly.

**Files:**
- Modify: `.../service/authorization/DefaultAuthorizationService.java` — canonicalize the `String`
  argument of `addAllowedServiceName` / `addBlockedServiceName` (and their `*Operation` siblings),
  only when it starts with `/`
- Modify: `.../service/web/cors/CorsFilterConfiguration.java`
- Modify: `.../service/user/session/web/filter/UserSessionFilterConfiguration.java`
- Modify: `.../service/web/filter/AppbaseFilterConfig.java`
- Test: new `AppbaseFilterConfigTest` methods, plus one per other class

**Interfaces:**
- Consumes: `AppbasePathCanonicalizer.canonicalize(String)`, `AppbaseServiceConstants.WILDCARD`
- Produces: no new signatures. Keys stored canonical.

Each class canonicalizes in its **private constructor**, which is the single point every creation
path reaches — Jackson's `@JsonDeserialize(builder = ...)` and programmatic `builder()` alike.

The two nested maps in `AppbaseFilterConfig` — `allowedMethodsByPathsByFilterTypeNameMap` and
`blockedMethodsByPathsByFilterTypeNameMap` — are keyed on **filter class name** at the outer level.
Canonicalize the **inner** keys only. Getting this wrong silently disables filter overrides.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void configuredKeysAreStoredCanonical() {
        final UserSessionFilterConfiguration configuration = UserSessionFilterConfiguration.builder()
                .createSessionIfAbsentByPathMap(Map.of("/%61dmin", Boolean.TRUE))
                .build();

        assertTrue(configuration.getCreateSessionIfAbsentByPathMap().containsKey("/admin"),
                "a key authored as /%61dmin must match a request for /admin");
    }

    @Test
    void theWildcardSentinelIsNeverCanonicalized() {
        final UserSessionFilterConfiguration configuration = UserSessionFilterConfiguration.builder()
                .createSessionIfAbsentByPathMap(Map.of(AppbaseServiceConstants.WILDCARD, Boolean.TRUE))
                .build();

        assertTrue(configuration.getCreateSessionIfAbsentByPathMap()
                .containsKey(AppbaseServiceConstants.WILDCARD));
    }

    @Test
    void aKeyThatCanNeverMatchFailsAtLoadRatherThanSilently() {
        assertThrows(AppbasePathException.class, () -> UserSessionFilterConfiguration.builder()
                .createSessionIfAbsentByPathMap(Map.of("/a%2Fb", Boolean.TRUE))
                .build());
    }
```

And for the nested case in `AppbaseFilterConfigTest`:

```java
    @Test
    void onlyTheInnerKeysOfTheNestedMapsAreCanonicalized() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .allowedMethodsByPathsByFilterTypeNameMap(Map.of(
                        "com.viveworks.appbase.service.web.cors.CorsServletFilter",
                        new MapOfStringSetOfStringsValue(Map.of("/%61dmin", new SetOfStringsValue(Set.of("GET"))))))
                .build();

        final MapOfStringSetOfStringsValue byPath = config.getAllowedMethodsByPathsByFilterTypeNameMap()
                .get("com.viveworks.appbase.service.web.cors.CorsServletFilter");

        assertNotNull(byPath, "the outer key is a filter class name and must not be canonicalized");
        assertTrue(byPath.getValue().containsKey("/admin"), "the inner key is a path and must be canonicalized");
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-core -Dtest='AppbaseFilterConfigTest+UserSessionFilterConfigurationTest' -P quick-profile`
Expected: FAIL — `containsKey("/admin")` is false because the raw `/%61dmin` was stored; `aKeyThatCanNeverMatchFailsAtLoadRatherThanSilently` fails because nothing throws.

- [ ] **Step 3: Write minimal implementation**

**Amendment A1:** use the shared `AppbasePathCanonicalizer.canonicalizeKeys(...)` written in Task 1.
Do NOT copy a private helper into each class. All four configuration classes are in
`appbase-service-core` or in modules that already depend on it, so the shared method is reachable
from every one of them without a new dependency.

Its contract, defined once in Task 1:

```java
    /**
     * Configured path keys are canonicalized on the way in so that both sides of every lookup share
     * one key space. A key that cannot canonicalize can never match any request, so it fails the
     * build rather than sitting inert in the map.
     *
     * <p>Two keys are passed through untouched. The wildcard sentinel is not a path. And a key that
     * does not begin with {@code /} is not a path either: the authorization maps are keyed on
     * EITHER a request path OR a {@code Class#getName()}, depending on which {@code isAuthorized}
     * overload the caller used, and canonicalizing a class name would prepend a slash and break
     * every class-name lookup.
     */
    public static <V> Map<String, V> canonicalizeKeys(final Map<String, V> source) {
        if (source == null) {
            return Map.of();
        }
        final Map<String, V> canonical = new LinkedHashMap<>(source.size());
        for (final Map.Entry<String, V> entry : source.entrySet()) {
            final String key = isCanonicalizableKey(entry.getKey())
                    ? canonicalize(entry.getKey())
                    : entry.getKey();
            canonical.put(key, entry.getValue());
        }
        return Map.copyOf(canonical);
    }

    private static boolean isCanonicalizableKey(final String key) {
        return key.startsWith("/") && !AppbaseServiceConstants.WILDCARD.equals(key);
    }
```

Call it from each private constructor. For `UserSessionFilterConfiguration`:

```java
        createSessionIfAbsentByPathMap = canonicalizeKeys(builder.createSessionIfAbsentByPathMap);
```

For `CorsFilterConfiguration`:

```java
        pathByCorsHeadersMap = canonicalizeKeys(builder.pathByCorsHeadersMap);
```

For `DefaultAuthorizationServiceConfiguration`, both maps — the shared helper's `startsWith("/")`
guard is what makes this safe. Those two maps are ALSO keyed on Java class names, by
`DefaultAuthorizationService`'s `Class<?>` overloads (`:105`, `:118`, via `serviceType.getName()`).
A class name contains no `%`, no `//`, and no `.`/`..` *segment*, so `canonicalize` would map
`com.viveworks.Foo` to `/com.viveworks.Foo` — **it gains a leading slash** — and every class-name
lookup would miss. The guard skips it. Pin that with a test in
`DefaultAuthorizationServiceConfigurationTest`:

```java
    @Test
    void aClassNameKeyIsNotTreatedAsAPath() {
        final DefaultAuthorizationServiceConfiguration configuration =
                DefaultAuthorizationServiceConfiguration.builder()
                        .blockedOperationsByServiceTypeNameMap(
                                Map.of("com.viveworks.appbase.Foo", new MapOfStringStringValue(Map.of())))
                        .build();

        assertTrue(configuration.getBlockedOperationsByServiceTypeNameMap()
                        .containsKey("com.viveworks.appbase.Foo"),
                "the authorization maps are keyed on either a path or a Class#getName(); "
                        + "only the former is a path");
    }
```

For `AppbaseFilterConfig`, apply `canonicalizeKeys` to the six flat maps, `Set.copyOf(...)` with
canonicalized elements to `allowedPaths` and `blockedPaths`, and for the two nested maps rebuild the
inner `MapOfStringSetOfStringsValue` with canonicalized inner keys, leaving the outer key untouched.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd appbase-java && ./build.sh quick`
Expected: PASS. `AppbaseServiceUtilTest`'s twelve `isFilterEnabled` tests use the literal `/path1`,
which is already canonical, so they should be untouched. If one fails, stop — the helper is
mangling something it should not.

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-service/
git commit -m "feat(P-027-R1): canonicalize path keys at configuration load

Twelve path-keyed maps across four configuration classes. Each canonicalizes
in its private constructor, the one point both Jackson's builder-driven
deserialization and programmatic construction reach.

A key that cannot canonicalize fails the build. It could never have matched a
request, so accepting it silently was the trap.

Two subtleties, both pinned by tests. The nested ...ByFilterTypeNameMap maps
are keyed on a filter class name at the outer level; only inner keys are paths.
And the two authorization maps hold either a path or a Class#getName(),
depending on which isAuthorized overload the caller used -- so only keys
starting with / are canonicalized."
```

---

### Task 4: The reactive face adopts the canonical key

Depends on Task 1.

**Files:**
- Modify: `appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty/src/main/java/com/viveworks/appbase/bootstrap/reactornetty/server/NettyWebExchange.java` (`:93`, `:102-104`)
- Modify: `.../server/ReactorNettyAppbaseWebServer.java` (`:105-114`)
- Test: `.../reactornetty/server/NettyWebExchangeTest.java`

**Interfaces:**
- Consumes: `AppbasePathCanonicalizer.canonicalize(String)`, `AppbasePathException`
- Produces: `NettyWebExchange.path()` returns the canonical key. Constructor throws `AppbasePathException`.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void pathIsCanonicalNotMerelyDecoded() {
        final HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.uri()).thenReturn("/%61dmin?q=1");

        final NettyWebExchange exchange = new NettyWebExchange(request, mock(HttpServerResponse.class), objectMapper);

        // Same wire path and expected key as AuthorizationServletFilterTest (Task 5) and
        // UserSessionJaxrsRequestFilterTest (Task 6). Three faces, one key. If these literals
        // drift apart, the faces have drifted.
        assertEquals("/admin", exchange.path());
    }

    @Test
    void anEncodedSeparatorIsRejectedAtConstruction() {
        final HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.uri()).thenReturn("/a%2Fb");

        assertThrows(AppbasePathException.class,
                () -> new NettyWebExchange(request, mock(HttpServerResponse.class), objectMapper));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd appbase-java && mvn test -pl appbase-bootstrap/appbase-bootstrap-reactor-netty -Dtest=NettyWebExchangeTest -P quick-profile`
Expected: FAIL — `pathIsCanonicalNotMerelyDecoded` currently returns whatever `fullPath()` stubs to;
`anEncodedSeparatorIsRejectedAtConstruction` throws nothing.

- [ ] **Step 3: Write minimal implementation**

In `NettyWebExchange`, replace the `path()` body and compute once in the constructor. `fullPath()`
is already decoded, and canonicalizing an already-decoded string is unsound — `%2F` has become `/`
and the evidence is gone. `queryStringDecoder` is already built from `request.uri()` at `:93`, and
its `rawPath()` is the encoded path.

```java
    private final String path;

    // in the constructor, after queryStringDecoder is assigned:
    // rawPath(), not fullPath(): canonicalization must see the ENCODED path. fullPath() has already
    // decoded %2F into a separator, and no later step can tell it from a real one.
    this.path = AppbasePathCanonicalizer.canonicalize(queryStringDecoder.rawPath());

    @Override
    public String path() {
        return path;
    }
```

In `ReactorNettyAppbaseWebServer`, wrap the exchange construction. The exchange does not exist when
the constructor throws, so the 400 goes out through reactor-netty's `response` directly:

```java
                .handle((request, response) -> {
                    final NettyWebExchange exchange;
                    try {
                        exchange = new NettyWebExchange(request, response, objectMapper);
                    } catch (final AppbasePathException e) {
                        // Emitted before the filter chain is entered, so authorization never sees a
                        // path that could not be canonicalized. No filter is added: the seven-filter
                        // order invariant is untouched.
                        return response.status(400).send();
                    }

                    final AppbaseServiceHandler handler = routeRegistry.lookup(exchange.method(), exchange.path());
                    final AppbaseServiceHandler terminal = handler != null ? handler : notFound();

                    return new DefaultAppbaseServiceFilterChain(filters, terminal).next(exchange);
                });
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd appbase-java && mvn test -pl appbase-bootstrap/appbase-bootstrap-reactor-netty -P quick-profile`
Expected: PASS.

- [ ] **Step 5: Confirm the chain-order invariant is untouched**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-user -Dtest=ReactiveFilterChainOrderTest -P quick-profile`
Expected: PASS, with **zero changes to that file**. If it needed editing, the design was misread — stop and escalate.

- [ ] **Step 6: Commit**

```bash
git add appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty/
git commit -m "feat(P-027-R1): the reactive face keys on the canonical path

NettyWebExchange canonicalizes queryStringDecoder.rawPath() once in its
constructor. rawPath(), not fullPath(): canonicalization must see the encoded
path, because fullPath() has already turned %2F into a separator and nothing
downstream can tell it from a real one.

A path that cannot canonicalize becomes a 400 written through reactor-netty's
response, before the filter chain is entered -- so authorization never sees it.
No filter is added; the seven-filter order invariant is untouched."
```

---

### Task 5: The servlet face adopts the canonical key

Depends on Task 1. Carries the D1 bypass regression test.

**Files:**
- Modify: `.../service/web/cors/CorsServletFilter.java:53`
- Modify: `.../service/web/filter/servlet/AbstractAppbaseServletFilter.java:62`
- Modify: `.../service/user/session/web/filter/servlet/UserSessionServletFilter.java:96`
- Modify: `.../service/web/authorization/filter/servlet/AuthorizationServletFilter.java`
- Test: `.../web/authorization/filter/servlet/AuthorizationServletFilterTest.java`

**Interfaces:**
- Consumes: `AppbasePathCanonicalizer.canonicalize`, `AppbasePathCanonicalizer.stripContextPath`
- Produces: every servlet filter passes a canonical path into neutral logic.

- [ ] **Step 1: Write the failing test — this is the live bypass**

```java
    @Test
    void anEncodedPathDoesNotBypassABlocklistEntry() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/%61dmin");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");

        // appbaseCore is configured with a blocklist entry for "/admin"
        final AuthorizationServletFilter filter = new AuthorizationServletFilter(appbaseCoreBlocking("/admin"));
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(403);
    }

    @Test
    void aTraversalPathDoesNotBypassABlocklistEntry() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/x/../admin");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");

        final AuthorizationServletFilter filter = new AuthorizationServletFilter(appbaseCoreBlocking("/admin"));
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void theServletFaceHandsTheCanonicalPathToTheNeutralFilter() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/%61dmin");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");

        final AuthorizationFilter neutral = mock(AuthorizationFilter.class);
        when(neutral.isAuthorized(any(), any(), any(), any(), any())).thenReturn(true);
        new AuthorizationServletFilter(appbaseCore, neutral).doFilter(request, response, chain);

        final ArgumentCaptor<String> service = ArgumentCaptor.forClass(String.class);
        verify(neutral).isAuthorized(any(), service.capture(), any(), any(), any());

        // Same wire path and expected key as NettyWebExchangeTest (Task 4) and
        // UserSessionJaxrsRequestFilterTest (Task 6). Three faces, one key.
        assertEquals("/admin", service.getValue());
    }
```

The blocklist path denies only when an `AuthorizationServiceExceptionConfiguration` is registered —
pass-through-when-unconfigured is deliberate (see `AuthorizationWebFilter`'s Javadoc). Build
`appbaseCoreBlocking(...)` to register one with `respondForbidden = true`, otherwise the filter will
correctly pass the request through and the test will assert the wrong thing.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-web -Dtest=AuthorizationServletFilterTest -P quick-profile`
Expected: FAIL — the chain IS invoked, because `/%61dmin` misses the `/admin` key and the filter passes it through. **This failure is the defect.**

- [ ] **Step 3: Write minimal implementation**

Each servlet filter replaces `request.getRequestURI()` with:

```java
        final String path = AppbasePathCanonicalizer.canonicalize(
                AppbasePathCanonicalizer.stripContextPath(request.getRequestURI(), request.getContextPath()));
```

Strip before decode: `getRequestURI()` and `getContextPath()` are both encoded (the servlet
specification says the container does not decode the request URI), and stripping after decoding
would let an encoded separator inside the prefix shift the boundary.

Apply at `CorsServletFilter:53`, `AbstractAppbaseServletFilter:62` (the value passed to
`isFilterEnabled`), `UserSessionServletFilter:96`, and in `AuthorizationServletFilter`.

Catch `AppbasePathException` in each and send 400 without continuing the chain.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd appbase-java && ./build.sh quick`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-service/appbase-service-web/ appbase-java/appbase-service/appbase-service-user/
git commit -m "fix(P-027-R1): the servlet face keys on the canonical path

A blocklist entry for /admin did not match a request for /%61dmin. The servlet
container does not decode getRequestURI(), so the exact-string containsKey
missed, the deny never fired, and the operator's explicit block was discarded.

Strip the context path before decoding: both are encoded, and stripping after
would let an encoded separator inside the prefix shift the boundary."
```

---

### Task 6: The JAX-RS face adopts the canonical key

Depends on Task 1.

**Files:**
- Modify: `.../service/web/cors/CorsRequestJaxrsFilter.java`, `CorsResponseJaxrsFilter.java`
- Modify: `.../service/web/filter/jaxrs/AbstractAppbaseJaxrsFilter.java:90`
- Modify: `.../service/user/session/web/filter/jaxrs/UserSessionJaxrsRequestFilter.java:99`

**Interfaces:**
- Consumes: `AppbasePathCanonicalizer.canonicalize`
- Produces: every JAX-RS filter passes a canonical path into neutral logic.

- [ ] **Step 1: Write the failing test**

**Amendment A2:** the test must drive the production filter and capture what reaches the neutral
logic. Stubbing `UriInfo` and then canonicalizing the stub's return value asserts nothing about
whether the filter was changed.

```java
    @Test
    void theJaxrsFaceHandsTheCanonicalPathToTheNeutralFilter() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath(false)).thenReturn("/%61dmin");
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);

        final UserSessionFilter neutral = mock(UserSessionFilter.class);
        new UserSessionJaxrsRequestFilter(appbaseCore, neutral).filter(containerRequestContext);

        final ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(neutral).onRequest(path.capture(), any(), any(), any());

        // Same wire path and same expected key as NettyWebExchangeTest (Task 4) and
        // AuthorizationServletFilterTest (Task 5). If these three literals drift, the faces have.
        assertEquals("/admin", path.getValue());
    }
```

`UserSessionJaxrsRequestFilter` may not currently take a `UserSessionFilter` as a constructor seam.
If it does not, add one, mirroring `AuthorizationWebFilter`'s package-private test-seam constructor
(`AuthorizationWebFilter:88`). Do not make the field non-final or reach in reflectively.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd appbase-java && mvn test -pl appbase-service/appbase-service-web -P quick-profile`
Expected: FAIL — production code still calls `uriInfo.getPath()`, which decodes.

- [ ] **Step 3: Write minimal implementation**

Replace `uriInfo.getPath()` with:

```java
        // getPath(false) is the ENCODED, base-URI-relative path. getPath() decodes -- see
        // jakarta.ws.rs-api: "All sequences of escaped octets are decoded, equivalent to
        // getPath(true)". Canonicalization must see the encoded form.
        final String path = AppbasePathCanonicalizer.canonicalize(uriInfo.getPath(false));
```

No context-path arithmetic: `getPath(boolean)` is already relative to the base URI.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd appbase-java && ./build.sh quick`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-service/
git commit -m "fix(P-027-R1): the JAX-RS face keys on the canonical path

uriInfo.getPath() decodes -- the API contract says it is equivalent to
getPath(true). getPath(false) is the encoded, base-URI-relative path, which is
what canonicalization must see."
```

---

### Task 7: Rename `DefaultContextAuthorizer`

Independent of Tasks 1–6, but Task 2's tests reference the name; run this after Task 2 to avoid a rename mid-test.

**Files:**
- Create: `.../service/authorization/PermitAllContextAuthorizer.java`
- Delete: `.../service/authorization/DefaultContextAuthorizer.java`
- Modify: `.../bootstrap/service/AppbaseServices.java:210-222` (Javadoc)
- Modify: 3 test files referencing the old name

- [ ] **Step 1: Rename and document**

```java
/**
 * A {@link ContextAuthorizer} that authorizes every request.
 *
 * <p><strong>This authorizes everything.</strong> It was previously named
 * {@code DefaultContextAuthorizer}, which read as though it were the safe thing you get without
 * thinking about it. It is not, and it is not wired by default -- an application must pass it
 * explicitly to {@code AppbaseServices.Builder.withContextAuthorizer(...)}. Useful for local
 * development and tests. Passing it in production disables path authorization entirely.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class PermitAllContextAuthorizer implements ContextAuthorizer {
```

Add to `AppbaseServices.Builder.withContextAuthorizer(...)`'s Javadoc:

```java
     * <p>Passing {@link PermitAllContextAuthorizer} disables path authorization entirely: every
     * request not matched by a blocklist entry is authorized.
```

- [ ] **Step 2: Update references**

```bash
cd appbase-java && grep -rl "DefaultContextAuthorizer" --include="*.java" . | grep -v "/.claude/"
```
Expected: 3 test files. Update each.

- [ ] **Step 3: Run the full build**

Run: `cd appbase-java && ./build.sh quick`
Expected: PASS. Zero remaining references: `grep -r DefaultContextAuthorizer --include="*.java" . | grep -v /.claude/` returns nothing.

- [ ] **Step 4: Commit**

```bash
git add appbase-java/
git commit -m "refactor(P-027-R1): DefaultContextAuthorizer -> PermitAllContextAuthorizer

It authorizes every request. The name Default asserted it was the safe thing
you get without thinking. It is not wired by default -- an application must
pass it to withContextAuthorizer(...) explicitly -- but the name invited
exactly the mistake it describes."
```

---

### Task 8: Face agreement, ledger, and the guide seed

Depends on Tasks 1, 4, 5, 6.

**Files:**
- Modify: `.../service/web/reactive/CorsWebFilterTest.java:126,146` — divergence pin becomes agreement test
- Create: `docs/configuration/authorization-and-path-matching.md`
- Modify: the historical design ledger's OPEN DECISION block
- Modify: the spec — fold in the two planning corrections

- [ ] **Step 1: Retire the divergence pin**

`CorsWebFilterTest:126` and its assertion message at `:146` document that `exchange.path()` forwards
reactor-netty's decoded `fullPath()` verbatim and is "NOT re-encoded to /a%20b". That comment records
the divergence this work removes, and it is now false.

**Amendment A3: do NOT write a three-face agreement test here.** The version this plan originally
carried compared `canonicalize(x)` with `canonicalize(x)` and proved only determinism, which Task 1's
idempotence property already covers. A genuine cross-face test is impossible in any single module:
`appbase-bootstrap-reactor-netty` can reach the servlet and JAX-RS filters through
`appbase-service-web`, but `jakarta.servlet-api` and `jakarta.ws.rs-api` are not on its classpath,
and putting them there would pull the servlet API into the one module defined by not having it.

Instead:

1. Update `CorsWebFilterTest`'s stale comment and assertion message so they describe canonical
   behaviour rather than the retired divergence. Assert that a `CorsWebFilter` driven with an
   exchange whose `path()` is `/admin` looks up the `/admin` CORS key.
2. Confirm the three per-face assertions written in Tasks 4, 5, and 6 all use the **same wire path
   `/%61dmin` and the same expected key `/admin`**, and that each carries a comment naming the other
   two. Those three assertions, each exercising its own face's production code in its own module, are
   what establishes agreement. If the literals ever drift apart, the faces have drifted.

- [ ] **Step 2: Write the guide seed**

Create `docs/configuration/authorization-and-path-matching.md` containing, verbatim from the spec:
§5.1's canonical-form rules (including the two deliberate non-normalizations), and §5.3's
authorization semantics table. State that keys are canonicalized at load and a key that cannot
canonicalize fails startup.

- [ ] **Step 3: Rewrite the ledger's OPEN DECISION block**

Replace `progress.md:105-117` with a record of what was found: the three options on `path()`
dissolve, because the divergence was a normalization gap, not a choice between key spaces. Note that
the servlet/JAX-RS split predates R1; that D3's context-path half is unreachable today (all three
containers mount at root); and that the reactive face ignoring `AppbaseFilterConfig` is tracked
separately and must land before the epic merges to `develop`.

- [ ] **Step 4: Run the full build**

Run: `cd appbase-java && ./build.sh develop`
Expected: PASS, coverage gate included.

- [ ] **Step 5: Commit and open the PR**

```bash
git add docs/ appbase-java/
git commit -m "docs(P-027-R1): face-agreement test, guide seed, ledger rewrite

The Task 3b test that pinned the servlet/reactive path divergence now asserts
the three faces agree. The OPEN DECISION on path() dissolves: it was a
normalization gap, not a choice between key spaces."
gh pr create --base feature/P-027-R1-reactive-web --title "P-027 R1: canonical path key space and authorization semantics"
```

---

## Self-Review

**Spec coverage.** §5.1 canonical form → Task 1. §5.2 new types → Task 1. §5.3 authorization
semantics → Task 2. §4's twelve maps → Task 3. §5.4 face call sites → Tasks 4, 5, 6. §5.5 error
handling → Tasks 4 (reactive 400), 5 (servlet 400), 3 (config-load failure). §6 testing → each
task's tests, plus Task 8's face-agreement test. D4 → Task 7. Ledger and guide → Task 8.

**Gap found and closed during review.** Task 3 originally canonicalized every key in the two
authorization maps. Those maps are also keyed on `Class#getName()` via the `Class<?>` overloads at
`DefaultAuthorizationService:105,118`, and `canonicalize("com.viveworks.Foo")` would return
`/com.viveworks.Foo` — a leading slash that breaks class-name lookups. Task 3 now canonicalizes only
keys beginning with `/`, and pins it with `aClassNameKeyIsNotTreatedAsAPath`.

**Type consistency.** `canonicalize(String) -> String` and `stripContextPath(String, String) -> String`
are used with those exact signatures in Tasks 3, 4, 5, 6, 8. `AppbasePathException` is unchecked
throughout; no task declares `throws`.

**Ordering.** Task 1 gates 3, 4, 5, 6, 8. Task 2 is independent but should precede Task 7, whose
rename touches Task 2's tests. Tasks 4, 5, 6 are mutually independent and may run in parallel.
