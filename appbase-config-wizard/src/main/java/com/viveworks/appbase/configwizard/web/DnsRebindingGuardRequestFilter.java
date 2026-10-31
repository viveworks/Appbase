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

package com.viveworks.appbase.configwizard.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

import java.util.Locale;
import java.util.Set;

/**
 * Fails a request closed unless it plausibly originates from the loopback UI this server serves. The
 * config wizard is unauthenticated and bound to loopback, so loopback binding was the only boundary; a
 * victim who visits an attacker-controlled hostname that DNS-rebinds to {@code 127.0.0.1} would
 * otherwise let attacker JavaScript read and mutate the local file-write API (A10-01, internal work item 1449).
 *
 * <p>Two checks, both fail-closed:
 * <ul>
 *   <li><strong>Host</strong> — the {@code Host} header authority must be loopback ({@code 127.0.0.1},
 *       {@code localhost}, or {@code [::1]}, with any port). A rebinding attack makes the browser send
 *       the attacker's hostname here, so this alone defeats the classic case.</li>
 *   <li><strong>Origin / Referer</strong> — when present, must be a loopback origin. A cross-origin
 *       fetch from the attacker page carries its own {@code Origin}, which is rejected.</li>
 * </ul>
 *
 * <p>A per-run browser token adds defence-in-depth on top of these (see the cookie-based token check
 * layered in separately). This filter is the transport-header half.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class DnsRebindingGuardRequestFilter implements ContainerRequestFilter {

    private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost", "::1");

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        final String host = requestContext.getHeaderString("Host");
        if (!isLoopbackAuthority(host)) {
            abort(requestContext, "Host header is not a loopback authority");
            return;
        }

        // Origin is the reliable cross-origin signal; fall back to Referer only when Origin is absent
        // (some navigations omit Origin). When neither is present the request is same-document, allowed.
        final String origin = requestContext.getHeaderString("Origin");
        if (origin != null) {
            if (!isLoopbackOrigin(origin)) {
                abort(requestContext, "Origin is not a loopback origin");
            }
            return;
        }
        final String referer = requestContext.getHeaderString("Referer");
        if (referer != null && !isLoopbackOrigin(referer)) {
            abort(requestContext, "Referer is not a loopback origin");
        }
    }

    /**
     * @param hostHeader the raw {@code Host} header, e.g. {@code 127.0.0.1:8080}
     * @return {@code true} when the authority's host is a loopback name, ignoring any port
     */
    private static boolean isLoopbackAuthority(final String hostHeader) {
        if (hostHeader == null || hostHeader.isBlank()) {
            return false;
        }
        return LOOPBACK_HOSTS.contains(hostOf(hostHeader.trim()));
    }

    /**
     * @param originOrReferer an {@code Origin} ({@code scheme://host[:port]}) or {@code Referer} URL
     * @return {@code true} when its host component is a loopback name
     */
    private static boolean isLoopbackOrigin(final String originOrReferer) {
        final String value = originOrReferer.trim();
        final int schemeEnd = value.indexOf("://");
        if (schemeEnd < 0) {
            return false;
        }
        String authority = value.substring(schemeEnd + 3);
        // Strip any path/query that a Referer carries beyond the authority.
        final int slash = authority.indexOf('/');
        if (slash >= 0) {
            authority = authority.substring(0, slash);
        }
        return LOOPBACK_HOSTS.contains(hostOf(authority));
    }

    /**
     * Extracts the host from an {@code authority}, dropping any {@code :port} but preserving a bracketed
     * IPv6 literal such as {@code [::1]}.
     *
     * @param authority a {@code host[:port]} authority
     * @return the lower-cased host with brackets stripped
     */
    private static String hostOf(final String authority) {
        String host = authority;
        if (host.startsWith("[")) {
            final int close = host.indexOf(']');
            if (close >= 0) {
                return host.substring(1, close).toLowerCase(Locale.ROOT);
            }
        }
        final int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        return host.toLowerCase(Locale.ROOT);
    }

    private static void abort(final ContainerRequestContext requestContext, final String reason) {
        requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("Forbidden: " + reason)
                .build());
    }
}
