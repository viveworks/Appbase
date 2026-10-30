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

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
 * <p><strong>Percent escapes are decoded as strict UTF-8.</strong> Percent octets and literal path
 * characters are assembled into one byte sequence, then decoded with malformed and unmappable input
 * set to {@link CodingErrorAction#REPORT}. Invalid UTF-8 is rejected rather than silently becoming
 * {@code U+FFFD}; a legitimately encoded {@code U+FFFD} remains valid and distinct. A raw {@code +}
 * is copied literally, so {@code /a+b} remains {@code /a+b}, never the form-decoder result
 * {@code /a b}.
 *
 * <p>{@link java.net.URI} was tried first and abandoned: it parses a leading {@code //} as an RFC
 * 3986 authority rather than a path, it silently truncates at a raw {@code ?} or {@code #} instead of
 * rejecting them, and -- the defect that forced the switch -- it rejects its own decoded output for
 * any URI-illegal character (a raw space, tab, {@code <}, {@code [}, <code>&#123;</code>, ...), so a path
 * containing an encoded one of those decodes successfully once and then throws on the second
 * application. That made the class non-idempotent while still claiming to be. The strict decoder
 * below applies no URI-syntax restriction to decoded output, which is why the idempotence guarantee
 * below now actually holds. {@code AppbaseServiceUtil} uses {@code URLDecoder} for query parameters, where
 * {@code application/x-www-form-urlencoded} semantics -- {@code +} as space -- are the correct
 * choice; paths instead require literal-plus semantics and strict UTF-8 validation.
 *
 * <p>Deliberately NOT done: trailing-slash folding ({@code /admin/} and {@code /admin} remain
 * distinct keys) and case folding. Both would change matching semantics beyond the scope of the
 * defect this class fixes.
 *
 * <p>A decoded path that still contains a literal {@code %} is rejected, not survived. {@code
 * %2F}/{@code %5C}/{@code %00} are already rejected before decode, so the only way a {@code %}
 * survives one decode pass is a doubly-encoded percent, e.g. {@code /a%2541} decodes once to
 * {@code /a%41} -- which still contains a percent that a second decode pass would reinterpret as a
 * new escape, silently changing the key. This closes that ambiguity the same way the
 * structural-octet rejection closes {@code %2F}/{@code %5C}/{@code %00}, and it is what makes the
 * idempotence guarantee below true rather than merely claimed.
 *
 * <p>Canonicalization is idempotent by construction:
 * {@code canonicalize(canonicalize(x)).equals(canonicalize(x))}. This now holds precisely, not just
 * by sampling, because a second pass has nothing left in the output that it could interpret
 * differently from the first: a canonical result never contains a literal {@code %} (see above), so
 * there is no escape left to decode, and the {@code +}-escaping described above is itself idempotent
 * -- {@code /a+b} re-escapes and re-decodes to {@code /a+b}, not {@code /a b} -- so there is no {@code
 * +} left that a second pass could turn into a space. That is what allows it to be applied at the
 * transport face and again at configuration load without the two coordinating.
 *
 * <p>A canonical result also never contains a raw {@code ?} or {@code #}, for the same by-construction
 * reason: both the raw and the {@code %3F}/{@code %23}-encoded forms are rejected before decode (see
 * {@code rejectStructuralOctets} and the raw-delimiter guard in {@link #canonicalize}). Without the
 * encoded-form rejection, {@code /a%3Fb} would decode to {@code /a?b} and be RETURNED as a "canonical"
 * result, and a second {@code canonicalize} pass over that survivor would then throw on the now-raw
 * {@code ?} -- an idempotence violation, since the second pass could not even equal the first, let
 * alone repeat it. Rejecting the encoded form up front closes that gap the same way the
 * residual-{@code %} rejection closes the doubly-encoded-percent gap.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AppbasePathCanonicalizer {

    private static final String[] STRUCTURAL_OCTETS = {"%2f", "%5c", "%00"};

    // %3f/%23 decode to a raw '?'/'#'. Rejecting them here, pre-decode, alongside the structural
    // octets is what makes rejectsARawQueryOrFragmentDelimiter's guarantee hold for the encoded form
    // too: without this, "/a%3Fb" would decode to "/a?b" and survive as a "canonical" result, and a
    // second canonicalize() pass over that survivor would then throw on the now-raw '?' -- an
    // idempotence violation, not merely a missed rejection. See the class Javadoc.
    private static final String[] ENCODED_QUERY_FRAGMENT_OCTETS = {"%3f", "%23"};
    private static final Pattern DUPLICATE_SLASHES = Pattern.compile("/{2,}");

    private AppbasePathCanonicalizer() {
    }

    /**
     * @param encodedPath a percent-encoded, context-relative path
     * @return the canonical key
     * @throws AppbasePathException on an encoded separator, an encoded NUL, a malformed escape, a raw
     *         or encoded {@code ?} or {@code #}, or a decoded path that still contains a literal
     *         {@code %}
     */
    public static String canonicalize(final String encodedPath) {
        AppbaseUtil.notNull(encodedPath, "encodedPath");

        // A raw '?' or '#' is not a structural octet -- it is unencoded -- so rejectStructuralOctets
        // does not see it (the ENCODED forms %3F/%23 are rejected there instead, alongside %2f/%5c/
        // %00 -- see ENCODED_QUERY_FRAGMENT_OCTETS). A decoder is not required to truncate at either
        // character, but a query or fragment has no business inside a security key regardless of
        // which decoder is in use, so this is rejected outright rather than relying on decoder
        // behaviour to keep it out.
        if (encodedPath.indexOf('?') >= 0 || encodedPath.indexOf('#') >= 0) {
            throw new AppbasePathException(
                    "encoded path '" + encodedPath + "' contains a raw '?' or '#', which has no place in a "
                            + "canonical path key");
        }

        // ORDER IS LOAD-BEARING: reject structural octets, then collapse duplicate slashes on the
        // ENCODED string, and only THEN decode. Do not "tidy" this into decode-first.
        //
        // This order is retained deliberately, independent of which decoder is used: a decoder must
        // never be allowed to manufacture a separator that was not present in the encoded input. A
        // URI-based decoder once parsed a leading "//" as an RFC 3986 authority and returned an EMPTY
        // path, silently dropping the whole segment and letting "//admin" canonicalize to "/" --
        // slipping past an "/admin" security rule. Collapsing on the encoded string before decoding
        // sidesteps that class of hazard regardless of decoder, so the order stays fixed even now that
        // the decoder no longer has that specific defect.
        //
        // This reordering is only sound because {@link #rejectStructuralOctets} has already run and
        // forbidden {@code %2f}/{@code %5c}/{@code %00}. Every literal {@code /} remaining in the
        // encoded string is therefore a genuine separator, never a decode artifact, so collapsing
        // before or after the decode is equivalent everywhere except at the string's head.
        rejectStructuralOctets(encodedPath);
        final String collapsed = DUPLICATE_SLASHES.matcher(encodedPath).replaceAll("/");
        final String decoded = decodeOnce(collapsed);
        final String resolved = removeDotSegments(decoded);

        return resolved.startsWith("/") ? resolved : "/" + resolved;
    }

    /**
     * Canonicalizes an authorization path and aliases a non-root trailing slash to the same key.
     * Thus {@code /admin/}, {@code /admin/.}, {@code /admin/%2e}, and {@code /admin/x/..} all match
     * {@code /admin}. Root remains {@code /}.
     */
    public static String canonicalizeAuthorizationPath(final String encodedPath) {
        final String canonical = canonicalize(encodedPath);
        return canonical.length() > 1 && canonical.endsWith("/")
                ? canonical.substring(0, canonical.length() - 1)
                : canonical;
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

    /**
     * Configured path keys are canonicalized on the way in so that both sides of every lookup share
     * one key space. A key that cannot canonicalize can never match any request, so it fails the
     * build rather than sitting inert in the map.
     *
     * <p>Two kinds of keys are passed through untouched. The wildcard sentinel is not a path. And a
     * key that does not begin with {@code /} is not a path either: the authorization maps are keyed
     * on EITHER a request path OR a {@code Class#getName()}, depending on which {@code isAuthorized}
     * overload the caller used, and canonicalizing a class name would prepend a slash and break
     * every class-name lookup.
     *
     * @param source the raw, as-authored map
     * @param <V> the map's value type
     * @return a new map with every path key canonicalized
     * @throws AppbasePathException if any path key cannot canonicalize
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

    /**
     * The {@code Set<String>} sibling of {@link #canonicalizeKeys}, for the path-keyed collections in
     * {@code AppbaseFilterConfig} that are sets rather than maps ({@code allowedPaths}, {@code
     * blockedPaths}) -- {@code canonicalizeKeys} takes a {@code Map} and does not apply to a bare set.
     * Same wildcard and non-{@code /} guards, same canonicalize-or-throw behaviour per element.
     *
     * @param source the raw, as-authored set
     * @return a new set with every path element canonicalized
     * @throws AppbasePathException if any path element cannot canonicalize
     */
    public static Set<String> canonicalizeSet(final Set<String> source) {
        if (source == null) {
            return Set.of();
        }
        final Set<String> canonical = new LinkedHashSet<>(source.size());
        for (final String element : source) {
            canonical.add(isCanonicalizableKey(element) ? canonicalize(element) : element);
        }
        return Set.copyOf(canonical);
    }

    private static boolean isCanonicalizableKey(final String key) {
        return key.startsWith("/") && !AppbaseServiceConstants.WILDCARD.equals(key);
    }

    private static void rejectStructuralOctets(final String encodedPath) {
        final String lower = encodedPath.toLowerCase(Locale.ROOT);
        for (final String octet : STRUCTURAL_OCTETS) {
            if (lower.contains(octet)) {
                throw new AppbasePathException(
                        "encoded path '" + encodedPath + "' contains the structural octet '" + octet + "'");
            }
        }
        for (final String octet : ENCODED_QUERY_FRAGMENT_OCTETS) {
            if (lower.contains(octet)) {
                throw new AppbasePathException(
                        "encoded path '" + encodedPath + "' contains the encoded query/fragment delimiter '"
                                + octet + "'");
            }
        }
    }

    private static String decodeOnce(final String encodedPath) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(encodedPath.length());
        for (int index = 0; index < encodedPath.length();) {
            if (encodedPath.charAt(index) == '%') {
                if (index + 2 >= encodedPath.length()) {
                    throw malformedPercentEscape(encodedPath);
                }
                final int high = Character.digit(encodedPath.charAt(index + 1), 16);
                final int low = Character.digit(encodedPath.charAt(index + 2), 16);
                if (high < 0 || low < 0) {
                    throw malformedPercentEscape(encodedPath);
                }
                bytes.write((high << 4) | low);
                index += 3;
                continue;
            }
            final int codePoint = encodedPath.codePointAt(index);
            final byte[] encoded = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
            bytes.write(encoded, 0, encoded.length);
            index += Character.charCount(codePoint);
        }

        final String path;
        try {
            path = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray()))
                    .toString();
        } catch (final CharacterCodingException e) {
            throw new AppbasePathException(
                    "encoded path '" + encodedPath + "' contains malformed UTF-8 percent encoding");
        }
        if (path.indexOf('%') >= 0) {
            // A doubly-encoded percent survives this single decode pass as a literal '%', e.g.
            // "%2541" decodes to "%41". Returning that would leave a character in the "canonical"
            // result that a second decode pass could reinterpret as a new escape, which is
            // exactly the idempotence violation this rejection exists to prevent -- see the class
            // Javadoc. Rejecting outright, rather than trying to survive it, matches how
            // %2F/%5C/%00 are already handled: a path key that needs a literal '%' cannot be a
            // security key under this scheme.
            throw new AppbasePathException(
                    "encoded path '" + encodedPath + "' decodes to '" + path + "', which still contains a "
                            + "literal '%'; a canonical key must never contain a character a second decode "
                            + "pass would reinterpret");
        }
        return path;
    }

    private static AppbasePathException malformedPercentEscape(final String encodedPath) {
        return new AppbasePathException(
                "encoded path '" + encodedPath + "' contains a malformed percent-escape");
    }

    /** RFC 3986 section 5.2.4. */
    private static String removeDotSegments(final String path) {
        final String[] segments = path.split("/", -1);
        final Deque<String> out = new ArrayDeque<>();
        for (final String segment : segments) {
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
        final String resolved = joined.isEmpty() ? "/" : joined;

        // RFC 3986 5.2.4 preserves the directory slash left behind by a terminal "." or ".."
        // segment: both denote a directory, never a file, so the resolved path must end in "/". This
        // class deliberately does not fold trailing slashes (see class Javadoc), so dropping this
        // slash would let a request for e.g. "/admin/." or "/admin/x/.." canonicalize to "/admin" and
        // silently miss a directory-form security key of "/admin/".
        final String lastSegment = segments[segments.length - 1];
        final boolean endedInDotSegment = ".".equals(lastSegment) || "..".equals(lastSegment);
        return endedInDotSegment && !resolved.endsWith("/") ? resolved + "/" : resolved;
    }
}
