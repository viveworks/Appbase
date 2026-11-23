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

package com.viveworks.appbase.configwizard.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Requires every {@code /api} request to carry the unguessable per-run browser token, delivered as a
 * same-site cookie the wizard UI echoes back in {@value #TOKEN_HEADER}. This is the defence-in-depth
 * layer on top of {@link DnsRebindingGuardRequestFilter} (A10-01, internal work item 1449): a DNS-rebinding victim's
 * browser sends a loopback {@code Host} and no cross-origin {@code Origin}, so the transport-header
 * checks pass — but the attacker's page cannot read the SameSite=Strict cookie bound to the wizard's
 * origin, so it cannot forge this header. The request therefore fails closed here.
 *
 * <p>Fails closed when the header is absent, blank, or does not match. The comparison is
 * constant-time to keep the token out of a timing side-channel.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class BrowserTokenRequestFilter implements ContainerRequestFilter {

    /** The request header the wizard UI sends, carrying the per-run token read from its cookie. */
    public static final String TOKEN_HEADER = "X-Appbase-Wizard-Token";

    private final String expectedToken;

    /**
     * @param expectedToken the per-run token this server minted at startup; every {@code /api} request
     *                      must present exactly this value in {@value #TOKEN_HEADER}
     */
    public BrowserTokenRequestFilter(final String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        final String presented = requestContext.getHeaderString(TOKEN_HEADER);
        if (presented == null || presented.isEmpty() || !matches(presented)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Forbidden: missing or invalid per-run browser token")
                    .build());
        }
    }

    /**
     * @param presented the token value from the request header
     * @return {@code true} when it equals the expected token, compared in constant time
     */
    private boolean matches(final String presented) {
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                expectedToken.getBytes(StandardCharsets.UTF_8));
    }
}
