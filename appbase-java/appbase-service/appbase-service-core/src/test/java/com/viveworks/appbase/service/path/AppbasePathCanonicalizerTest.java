/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.service.path;

import com.viveworks.appbase.service.AppbaseServiceConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AppbasePathCanonicalizerTest {

    @ParameterizedTest
    @CsvSource({
            "/%61dmin,      /admin",
            "/x/../admin,   /admin",
            "/x/./y,        /x/y",
            "//admin,       /admin",
            "///admin,      /admin",
            "/a//b,         /a/b",
            "//x/../admin,  /admin",
            "/a+b,          /a+b",
            "/a%2Bb,        /a+b",
            "/a%20b,        /a b",
            "/admin/,       /admin/",
            "/Admin,        /Admin",
            "/../admin,     /admin",
            "/a/b/..,       /a/",
            "/a/b/.,        /a/b/",
            "/admin/.,      /admin/",
            "/..,           /",
            "/a/../..,      /",
            "/a/..,         /"
    })
    void canonicalizesToTheExpectedKey(final String encoded, final String expected) {
        assertEquals(expected, AppbasePathCanonicalizer.canonicalize(encoded));
    }

    /**
     * Regression pin for a leading {@code //}. A {@code URI}-based decoder would parse {@code admin}
     * as an RFC 3986 authority and return an EMPTY path, so decoding before collapsing duplicate
     * slashes would silently drop the segment and let {@code //admin} canonicalize to {@code /} --
     * slipping past an {@code /admin} security rule. The canonicalizer collapses duplicate slashes on
     * the encoded string before decoding, deliberately, so this bypass cannot resurface even if the
     * decoder changes -- collapsing first must never be "simplified" into decode-first.
     */
    @Test
    void aLeadingDoubleSlashCollapsesRatherThanBecomingAnAuthority() {
        assertEquals("/admin", AppbasePathCanonicalizer.canonicalize("//admin"));
    }

    @Test
    void anEmptyPathBecomesRoot() {
        assertEquals("/", AppbasePathCanonicalizer.canonicalize(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a%2Fb", "/a%2fb", "/a%5Cb", "/a%5cb", "/a%00b", "/a%zz", "/a%2",
            "/a%3Fb", "/a%3fb", "/a%23b"})
    void rejectsStructuralOctetsAndMalformedEscapes(final String encoded) {
        assertThrows(AppbasePathException.class, () -> AppbasePathCanonicalizer.canonicalize(encoded));
    }

    @Test
    void rejectsMalformedUtf8InsteadOfReplacingIt() {
        assertThrows(AppbasePathException.class,
                () -> AppbasePathCanonicalizer.canonicalize("/admin/%C3%28"));
    }

    @Test
    void aLegitimateReplacementCharacterRemainsDistinctFromMalformedUtf8() {
        assertEquals("/admin/\uFFFD", AppbasePathCanonicalizer.canonicalize("/admin/%EF%BF%BD"));
        assertThrows(AppbasePathException.class,
                () -> AppbasePathCanonicalizer.canonicalize("/admin/%EF%BF"));
    }

    /**
     * A doubly-encoded percent decodes once to a literal {@code %} that a second decode pass would
     * reinterpret as the start of a new escape, e.g. {@code /a%2541} decodes to {@code /a%41} and, if
     * decoded again, to {@code /aA}. Rejecting a residual {@code %} closes this the same way
     * {@code %2F}/{@code %5C}/{@code %00} are closed before decode.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/a%2541", "/a%2525", "/a%252Fb"})
    void rejectsAResidualPercentLeftAfterDecoding(final String encoded) {
        assertThrows(AppbasePathException.class, () -> AppbasePathCanonicalizer.canonicalize(encoded));
    }

    /**
     * {@code new URI(...)} parses a raw {@code ?} or {@code #} as the start of a query or fragment and
     * {@code getPath()} silently drops everything from that character onward. No face passes a query
     * to {@code canonicalize} today, but it is a public entry point and silently truncating its input
     * is wrong regardless of who calls it.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/a?b", "/a#b"})
    void rejectsARawQueryOrFragmentDelimiter(final String encoded) {
        assertThrows(AppbasePathException.class, () -> AppbasePathCanonicalizer.canonicalize(encoded));
    }

    /**
     * This property now holds by construction rather than by sampling: {@link
     * AppbasePathCanonicalizer#canonicalize} rejects any input that decodes to a string still
     * containing a literal {@code %} (see {@link #rejectsAResidualPercentLeftAfterDecoding}), so a
     * canonical result never contains a {@code %} and a second decode pass has nothing left to
     * reinterpret. Before that rejection existed, this test's sampled inputs happened to avoid
     * {@code %25} and so could not have caught the violation -- a test that could not fail.
     *
     * <p>{@code /a%3Fb} and {@code /a%23b} are included for the same reason: before the encoded
     * query/fragment delimiter was rejected pre-decode, {@code /a%3Fb} canonicalized once to
     * {@code /a?b} and then THREW on the second pass instead of returning an equal result -- a
     * canonicalize-twice property test that omits these rows cannot catch that. They are now rejected
     * on the FIRST pass, before there is a "once" to compare against a second pass, so idempotence
     * holds vacuously for them -- there is no canonical form to be non-idempotent about.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/%61dmin", "/x/../admin", "//admin", "///admin", "/a+b", "/admin/", "",
            "/a%20b", "/a%09b", "/a%3Cb", "/a%2Bb", "/a%C3%A9b", "/a%3Fb", "/a%23b"})
    void canonicalizationIsIdempotent(final String encoded) {
        final String once;
        try {
            once = AppbasePathCanonicalizer.canonicalize(encoded);
        } catch (final AppbasePathException e) {
            // A rejected input has no canonical form to compare a second pass against, so
            // idempotence holds vacuously -- see the Javadoc above for /a%3Fb and /a%23b.
            return;
        }
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

    @Test
    void canonicalizeKeysCanonicalizesAPathKey() {
        final Map<String, String> canonical =
                AppbasePathCanonicalizer.canonicalizeKeys(Map.of("/%61dmin", "value"));

        assertTrue(canonical.containsKey("/admin"), "a key authored as /%61dmin must match a request for /admin");
    }

    @Test
    void canonicalizeKeysLeavesTheWildcardSentinelUntouched() {
        final Map<String, String> canonical =
                AppbasePathCanonicalizer.canonicalizeKeys(Map.of(AppbaseServiceConstants.WILDCARD, "value"));

        assertTrue(canonical.containsKey(AppbaseServiceConstants.WILDCARD));
    }

    @Test
    void canonicalizeKeysLeavesAClassNameKeyUntouched() {
        final Map<String, String> canonical =
                AppbasePathCanonicalizer.canonicalizeKeys(Map.of("com.viveworks.appbase.Foo", "value"));

        assertTrue(canonical.containsKey("com.viveworks.appbase.Foo"),
                "a key that does not begin with / is not a path and must not gain a leading slash");
    }

    @Test
    void canonicalizeKeysThrowsOnAKeyThatCanNeverMatch() {
        assertThrows(AppbasePathException.class,
                () -> AppbasePathCanonicalizer.canonicalizeKeys(Map.of("/a%2Fb", "value")));
    }

    /**
     * {@code AppbaseFilterConfig}'s {@code allowedPaths}/{@code blockedPaths} are {@code Set<String>},
     * not a map, so {@link AppbasePathCanonicalizer#canonicalizeKeys} does not apply directly. This
     * mirrors it for a set: same wildcard and non-{@code /} guards, same canonicalize-or-throw
     * behaviour per element.
     */
    @Test
    void canonicalizeSetCanonicalizesAPathElement() {
        final Set<String> canonical = AppbasePathCanonicalizer.canonicalizeSet(Set.of("/%61dmin"));

        assertTrue(canonical.contains("/admin"), "an element authored as /%61dmin must match a request for /admin");
    }

    @Test
    void canonicalizeSetLeavesTheWildcardSentinelUntouched() {
        final Set<String> canonical =
                AppbasePathCanonicalizer.canonicalizeSet(Set.of(AppbaseServiceConstants.WILDCARD));

        assertTrue(canonical.contains(AppbaseServiceConstants.WILDCARD));
    }

    @Test
    void canonicalizeSetLeavesANonPathElementUntouched() {
        final Set<String> result = AppbasePathCanonicalizer.canonicalizeSet(Set.of("com.example.NotAPath"));

        assertTrue(result.contains("com.example.NotAPath"),
                "a non-path element (no leading slash) must be left verbatim -- e.g. a class name");
    }

    @Test
    void canonicalizeSetThrowsOnAnElementThatCanNeverMatch() {
        assertThrows(AppbasePathException.class,
                () -> AppbasePathCanonicalizer.canonicalizeSet(Set.of("/a%2Fb")));
    }

    @Test
    void authorizationPathAliasesTrailingSlashAndTerminalDotSegments() {
        assertEquals("/admin", AppbasePathCanonicalizer.canonicalizeAuthorizationPath("/admin/"));
        assertEquals("/", AppbasePathCanonicalizer.canonicalizeAuthorizationPath("/"));
        for (final String equivalent : Set.of(
                "/admin/.", "/admin/%2e", "/admin/%2E", "/admin/x/..", "/admin/x/%2e%2e")) {
            final String canonical = AppbasePathCanonicalizer.canonicalizeAuthorizationPath(equivalent);
            assertEquals("/admin", canonical, equivalent);
            assertEquals(canonical, AppbasePathCanonicalizer.canonicalizeAuthorizationPath(canonical),
                    "authorization canonicalization must be idempotent for " + equivalent);
        }
    }
}
