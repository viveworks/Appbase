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

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for {@link BrowserTokenCookieFilter}. When the browser loads the app shell (any
 * non-{@code /api} path), the filter plants the per-run token as a same-site cookie the UI later
 * echoes back in {@value BrowserTokenRequestFilter#TOKEN_HEADER} (A10-01, internal work item 1449). It must NOT set the
 * cookie on {@code /api} responses (those are data calls, not the shell) and must always continue the
 * chain.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class BrowserTokenCookieFilterTest {

    private static final String TOKEN = "the-per-run-token";

    private final BrowserTokenCookieFilter filter = new BrowserTokenCookieFilter(TOKEN);

    private static HttpServletRequest requestFor(final String uri) {
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURI()).thenReturn(uri);
        Mockito.when(req.getLocalPort()).thenReturn(8123);
        return req;
    }

    @Test
    void theAppShellResponseGetsASameSiteStrictTokenCookie() throws Exception {
        final HttpServletRequest req = requestFor("/");
        final HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        final FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        final ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        Mockito.verify(res).addHeader(Mockito.eq("Set-Cookie"), header.capture());
        final String cookie = header.getValue();
        assertTrue(cookie.contains(BrowserTokenCookieFilter.cookieName(8123) + "=" + TOKEN),
                "the cookie must carry the per-run token: " + cookie);
        assertTrue(cookie.contains("SameSite=Strict"),
                "the cookie must be SameSite=Strict so it is not sent cross-site: " + cookie);
        assertTrue(cookie.contains("Path=/"), "the cookie must be scoped to the whole app: " + cookie);
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void apiResponsesDoNotGetTheCookie() throws Exception {
        final HttpServletRequest req = requestFor("/api/configurations");
        final HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        final FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Mockito.verify(res, Mockito.never()).addHeader(Mockito.eq("Set-Cookie"), Mockito.anyString());
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void swaggerUiResponseGetsTheBootstrapCookie() throws Exception {
        final HttpServletRequest req = requestFor("/api/swagger-ui/index.html");
        final HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        final FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Mockito.verify(res).addHeader(Mockito.eq("Set-Cookie"),
                Mockito.contains(BrowserTokenCookieFilter.cookieName(8123) + "=" + TOKEN));
        Mockito.verify(chain).doFilter(req, res);
    }
}
