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
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit coverage for {@link DnsRebindingGuardRequestFilter}. Drives the filter with a mock
 * {@link ContainerRequestContext} so the Host header (which a real HTTP client controls, not the
 * caller) can be exercised deterministically — the classic DNS-rebinding vector.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DnsRebindingGuardRequestFilterTest {

    private final DnsRebindingGuardRequestFilter filter = new DnsRebindingGuardRequestFilter();

    private static ContainerRequestContext request(final String host, final String origin, final String referer) {
        final ContainerRequestContext ctx = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(ctx.getHeaderString("Host")).thenReturn(host);
        Mockito.when(ctx.getHeaderString("Origin")).thenReturn(origin);
        Mockito.when(ctx.getHeaderString("Referer")).thenReturn(referer);
        return ctx;
    }

    private static int abortedStatus(final ContainerRequestContext ctx) {
        final ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        Mockito.verify(ctx).abortWith(response.capture());
        return response.getValue().getStatus();
    }

    @Test
    void aNonLoopbackHostIsRejected() {
        // The DNS-rebinding vector: the browser sends the attacker's rebound hostname as Host.
        final ContainerRequestContext ctx = request("evil.example", null, null);

        filter.filter(ctx);

        assertEquals(403, abortedStatus(ctx), "a non-loopback Host must be rejected");
    }

    @Test
    void aLoopbackHostWithNoOriginIsAllowed() {
        final ContainerRequestContext ctx = request("127.0.0.1:8080", null, null);

        filter.filter(ctx);

        Mockito.verify(ctx, Mockito.never()).abortWith(Mockito.any());
    }

    @Test
    void aLocalhostAuthorityIsAllowed() {
        final ContainerRequestContext ctx = request("localhost:34567", null, null);

        filter.filter(ctx);

        Mockito.verify(ctx, Mockito.never()).abortWith(Mockito.any());
    }

    @Test
    void anIpv6LoopbackHostIsAllowed() {
        final ContainerRequestContext ctx = request("[::1]:8080", null, null);

        filter.filter(ctx);

        Mockito.verify(ctx, Mockito.never()).abortWith(Mockito.any());
    }

    @Test
    void aCrossOriginRequestIsRejected() {
        final ContainerRequestContext ctx = request("127.0.0.1:8080", "http://evil.example", null);

        filter.filter(ctx);

        assertEquals(403, abortedStatus(ctx), "a cross-origin request must be rejected");
    }

    @Test
    void aLoopbackOriginIsAllowed() {
        final ContainerRequestContext ctx = request("127.0.0.1:8080", "http://127.0.0.1:8080", null);

        filter.filter(ctx);

        Mockito.verify(ctx, Mockito.never()).abortWith(Mockito.any());
    }

    @Test
    void aCrossOriginRefererIsRejectedWhenOriginIsAbsent() {
        final ContainerRequestContext ctx = request("127.0.0.1:8080", null, "http://evil.example/x");

        filter.filter(ctx);

        assertEquals(403, abortedStatus(ctx), "a cross-origin Referer must be rejected when Origin is absent");
    }

    @Test
    void aMissingHostIsRejected() {
        final ContainerRequestContext ctx = request(null, null, null);

        filter.filter(ctx);

        assertEquals(403, abortedStatus(ctx), "a request with no Host header must be rejected (fail closed)");
    }
}
