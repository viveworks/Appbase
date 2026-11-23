/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
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

package com.viveworks.appbase.configwizard.security;

import com.viveworks.appbase.configwizard.web.ConfigWizardWebServer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Rejects every request whose {@code Host} header does not name a loopback hostname, or whose
 * explicit browser {@code Origin} does not name a loopback HTTP origin (SEC-1, CWE-346/350).
 *
 * <p>{@link ConfigWizardWebServer} binds only {@code 127.0.0.1} and has no authentication of any
 * kind (it is a local developer tool), but a bound loopback address alone does not stop a
 * <strong>DNS-rebinding</strong> attack: a victim's browser visits an attacker-controlled domain
 * whose DNS record is swapped (or has a very short TTL and is re-pointed) to resolve to
 * {@code 127.0.0.1}. The browser then happily sends same-origin-looking requests to that domain —
 * which land on this server — with a {@code Host} header naming the attacker's domain, not
 * {@code localhost}/{@code 127.0.0.1}. The bind address restricts <em>who can reach the socket</em>;
 * it says nothing about <em>what the browser believes the origin is</em>, and this server had no
 * check on that at all before this filter existed.
 *
 * <p>Registered as the first filter on every {@link ServletContextHandler} this
 * server mounts, so it covers the Jersey API, the static Flutter web UI, and the Swagger UI alike
 * — none of them had a Host check otherwise.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class HostValidationFilter implements Filter {

    /**
     * The only hostnames (host portion of the {@code Host} header, port stripped) this server
     * accepts. Mirrors {@link ConfigWizardWebServer}'s own loopback-only bind — a request not
     * addressed to one of these names did not reach this server as a legitimate same-origin
     * request, whatever socket it actually arrived on.
     */
    private static final Set<String> ALLOWED_HOSTNAMES = Set.of("localhost", "127.0.0.1", "[::1]");
    private static final Set<String> ALLOWED_ORIGIN_HOSTNAMES = Set.of("localhost", "127.0.0.1", "[::1]");

    /** The disclosure-free message returned to a rejected caller; names no configuration detail. */
    private static final String REJECTION_MESSAGE =
            "This server only accepts requests addressed to a loopback hostname.";

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!isAllowedHost(httpRequest.getHeader("Host"))
                || !isAllowedOrigin(httpRequest.getHeader("Origin"))) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, REJECTION_MESSAGE);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Checks {@code hostHeader}'s hostname portion (port stripped) against the allowlist.
     * The comparison is case-insensitive ({@link Locale#ROOT}): DNS hostnames are
     * case-insensitive, and a browser-originated {@code Host} header is not guaranteed to be
     * lower-case (code-review Agent 4, prior-PR-comments pass, final gate re-run — citing an
     * unresolved finding from internal work item 2627 against this exact comparison).
     *
     * @param hostHeader the raw {@code Host} header value, or {@code null} when absent
     * @return {@code true} only when the hostname is explicitly permitted
     */
    private static boolean isAllowedHost(final String hostHeader) {
        if (hostHeader == null || hostHeader.isBlank()) {
            // HTTP/1.1 requires a Host header; its absence is not a legitimate same-origin
            // browser request and must not be treated as one. Fail closed.
            return false;
        }
        return ALLOWED_HOSTNAMES.contains(stripPort(hostHeader.trim()).toLowerCase(Locale.ROOT));
    }

    /**
     * Accepts requests with no {@code Origin} header (non-browser clients and browser navigations
     * commonly omit it), but fails closed when a caller supplies an opaque, malformed, non-HTTP,
     * credential-bearing, or non-loopback origin. An explicit foreign origin must never reach the
     * unauthenticated configuration API even when it forges an otherwise-valid loopback Host.
     *
     * <p>Two defects fixed during gate re-review, both in this method: {@link URI#getHost()}
     * returns an IPv6 literal <em>with</em> its enclosing brackets (e.g. {@code "[::1]"}), so the
     * allowlist entry must be bracketed like {@link #ALLOWED_HOSTNAMES}'s is — an unbracketed
     * entry here silently rejected every legitimate IPv6 loopback origin (code-review Agent 3 /
     * Agent 6, and the security-review defeat-this pass, final gate re-run, each independently
     * reproducing this against a live JDK). Separately, a malformed or overflowing port (e.g.
     * {@code "http://localhost:99999999999999999999"}) makes {@code URI} fall back to a
     * registry-based authority with a {@code null} host, which the immutable, null-hostile
     * {@code Set.of(...)} allowlist would otherwise throw a {@link NullPointerException} on
     * (code-review Agent 3, and the security-review defeat-this pass) — undermining this very
     * method's own disclosure-free, fail-closed design by letting a crafted header crash the
     * filter instead of cleanly rejecting it. The host is now null-checked, and both the
     * {@code Host} and {@code Origin} hostname comparisons are case-insensitive for the same
     * reason {@link #isAllowedHost} is.
     *
     * @param originHeader the serialized browser origin, or {@code null} when absent
     * @return {@code true} only for an absent header or a loopback HTTP origin
     */
    private static boolean isAllowedOrigin(final String originHeader) {
        if (originHeader == null) {
            return true;
        }
        if (originHeader.isBlank() || "null".equals(originHeader)) {
            return false;
        }
        try {
            final URI origin = new URI(originHeader);
            final String originHost = origin.getHost();
            return !origin.isOpaque()
                    && "http".equalsIgnoreCase(origin.getScheme())
                    && origin.getUserInfo() == null
                    && originHost != null
                    && ALLOWED_ORIGIN_HOSTNAMES.contains(originHost.toLowerCase(Locale.ROOT))
                    && origin.getQuery() == null
                    && origin.getFragment() == null
                    && (origin.getPath() == null || origin.getPath().isEmpty())
                    && origin.getPort() <= 65535;
        } catch (final URISyntaxException e) {
            return false;
        }
    }

    /**
     * Strips a trailing {@code :port} from a {@code Host} header value, leaving an IPv6 literal's
     * brackets intact (e.g. {@code "[::1]:8080"} -> {@code "[::1]"}) so it is not mistaken for a
     * {@code host:port} pair at the literal's internal colons.
     *
     * @param hostHeader the trimmed, non-blank header value
     * @return the hostname portion only
     */
    private static String stripPort(final String hostHeader) {
        if (hostHeader.startsWith("[")) {
            final int closingBracket = hostHeader.indexOf(']');
            if (closingBracket < 0) {
                return hostHeader;
            }
            final String bracketed = hostHeader.substring(0, closingBracket + 1);
            final String afterBracket = hostHeader.substring(closingBracket + 1);
            // The only valid continuation after the closing bracket is nothing, or ":<port>".
            // Anything else (e.g. "[::1].evil", "[::1]junk") means this is NOT actually the
            // bracketed literal alone -- fail closed rather than being silently truncated into a
            // false match (code-review Agent 4, citing the identical bypass class on internal work item 1510 F9).
            return hasOnlyAValidPortSuffix(afterBracket) ? bracketed : hostHeader;
        }
        final int colon = hostHeader.indexOf(':');
        if (colon < 0) {
            return hostHeader;
        }
        final String hostname = hostHeader.substring(0, colon);
        final String afterColon = hostHeader.substring(colon);
        // Symmetric with the bracketed case above (code-review Agent 6, adversarial QA, final
        // gate re-run): truncating at the first ':' unconditionally would accept
        // "127.0.0.1:evil.com" as hostname "127.0.0.1" -- this host's current connector happens
        // to reject a non-numeric port before this filter ever runs, but that is the fronting
        // connector's behavior, not this filter's own, and must not be relied on as the only
        // thing enforcing it. Fail closed when what follows the colon is not a valid port.
        return hasOnlyAValidPortSuffix(afterColon) ? hostname : hostHeader;
    }

    /**
     * @param colonAndRemainder a string starting with {@code ':'} (the port separator itself),
     *                          or empty
     * @return {@code true} when {@code colonAndRemainder} is empty, or is exactly {@code ':'}
     *         followed by zero or more digits (i.e. a syntactically valid, possibly-empty port)
     */
    private static boolean hasOnlyAValidPortSuffix(final String colonAndRemainder) {
        return colonAndRemainder.isEmpty() || colonAndRemainder.matches(":\\d*");
    }
}
