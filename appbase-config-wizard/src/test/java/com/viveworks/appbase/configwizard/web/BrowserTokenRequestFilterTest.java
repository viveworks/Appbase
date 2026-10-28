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
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit coverage for {@link BrowserTokenRequestFilter} — the per-run browser-token half of the
 * A10-01 (internal work item 1449) defence. Loopback binding and the Host/Origin checks in
 * {@link DnsRebindingGuardRequestFilter} are defeated by a DNS-rebinding victim who visits an
 * attacker page; the unguessable per-run token, delivered as a same-site cookie the wizard UI echoes
 * back in a custom header, is the layer an attacker page cannot forge. This filter fails closed when
 * the token is absent or wrong.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class BrowserTokenRequestFilterTest {

    private static final String TOKEN = "the-expected-per-run-token";

    private final BrowserTokenRequestFilter filter = new BrowserTokenRequestFilter(TOKEN);

    private static ContainerRequestContext requestWithToken(final String tokenHeader) {
        final ContainerRequestContext ctx = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(ctx.getHeaderString(BrowserTokenRequestFilter.TOKEN_HEADER)).thenReturn(tokenHeader);
        return ctx;
    }

    private static int abortedStatus(final ContainerRequestContext ctx) {
        final ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        Mockito.verify(ctx).abortWith(response.capture());
        return response.getValue().getStatus();
    }

    @Test
    void aRequestWithNoTokenHeaderIsRejected() {
        final ContainerRequestContext ctx = requestWithToken(null);

        filter.filter(ctx);

        assertEquals(403, abortedStatus(ctx), "a request with no per-run token must be rejected (fail closed)");
    }

    @Test
    void anEmptyTokenHeaderIsRejected() {
        final ContainerRequestContext ctx = requestWithToken("");

        filter.filter(ctx);

        assertEquals(403, abortedStatus(ctx), "an empty per-run token must be rejected (fail closed)");
    }

    @Test
    void aWrongTokenIsRejected() {
        final ContainerRequestContext ctx = requestWithToken("not-the-token");

        filter.filter(ctx);

        assertEquals(403, abortedStatus(ctx), "a mismatched per-run token must be rejected");
    }

    @Test
    void theCorrectTokenIsAllowed() {
        final ContainerRequestContext ctx = requestWithToken(TOKEN);

        filter.filter(ctx);

        Mockito.verify(ctx, Mockito.never()).abortWith(Mockito.any());
    }
}
