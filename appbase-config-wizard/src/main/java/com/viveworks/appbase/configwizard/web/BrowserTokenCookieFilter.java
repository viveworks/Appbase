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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Plants the per-run browser token as a same-site cookie on the app-shell response, so the wizard UI
 * can read it and echo it back in {@value BrowserTokenRequestFilter#TOKEN_HEADER} on every
 * {@code /api} call (A10-01, internal work item 1449). Runs on the static (non-{@code /api}) responses only; the
 * {@code /api} data calls are guarded by {@link BrowserTokenRequestFilter}, not cookie-stamped here.
 *
 * <p>The cookie is {@code SameSite=Strict} so a cross-site (DNS-rebinding attacker) page never causes
 * it to be sent, and it is intentionally NOT {@code HttpOnly} because the UI's JavaScript must read it
 * to build the header. {@code Secure} is omitted because the server is loopback HTTP only.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class BrowserTokenCookieFilter implements Filter {

    /** Prefix for the port-scoped cookie name the wizard UI reads. */
    public static final String COOKIE_NAME_PREFIX = "appbase_wizard_token_";

    private final String token;

    /**
     * @param token the per-run token this server minted at startup
     */
    public BrowserTokenCookieFilter(final String token) {
        this.token = token;
    }

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest
                && response instanceof HttpServletResponse httpResponse) {
            final String uri = httpRequest.getRequestURI();
            if (uri == null || !uri.startsWith("/api") || uri.startsWith("/api/swagger-ui")) {
                httpResponse.addHeader("Set-Cookie",
                        cookieName(httpRequest.getLocalPort()) + "=" + token
                                + "; Path=/; SameSite=Strict");
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Cookies are host-scoped rather than port-scoped. Including the listening port in the name
     * prevents two concurrently running wizard instances from overwriting each other's token.
     *
     * @param port the local TCP port serving the response
     * @return the instance-specific cookie name
     */
    static String cookieName(final int port) {
        return COOKIE_NAME_PREFIX + port;
    }
}
