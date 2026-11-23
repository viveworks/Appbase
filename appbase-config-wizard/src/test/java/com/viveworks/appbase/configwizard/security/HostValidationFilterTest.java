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

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Direct unit coverage for {@link HostValidationFilter}, complementing the end-to-end,
 * real-HTTP coverage in {@code ConfigWizardWebServerTest} (code-review Agent 6/Agent 1
 * follow-up, final gate re-run): the end-to-end tests never exercised an IPv6-literal
 * {@code Host} header, since the server binds IPv4 loopback only and a real socket connection
 * to it cannot carry one over the wire in a way that exercises the bracket-stripping branch
 * directly. Unit-level mocking of {@link HttpServletRequest} lets this test drive that branch
 * (and every other {@code isAllowedHost}/{@code stripPort} path) without a running server.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class HostValidationFilterTest {

    private final HostValidationFilter filter = new HostValidationFilter();

    @ParameterizedTest
    @ValueSource(strings = { "localhost", "127.0.0.1", "[::1]", "localhost:8080", "127.0.0.1:8080", "[::1]:8080" })
    void doFilter_allowedHost_continuesTheChain(final String hostHeader) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn(hostHeader);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "evil.example.com", "evil.example.com:8080", "127.0.0.1.evil.com",
            "attacker-controlled", "LOCALHOST.evil.com" })
    void doFilter_foreignHost_rejectsWithoutInvokingTheChain(final String hostHeader) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn(hostHeader);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_missingHostHeader_rejectsWithoutInvokingTheChain() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_blankHostHeader_rejectsWithoutInvokingTheChain() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn("   ");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN), org.mockito.ArgumentMatchers.anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://evil.example.com", "http://evil.example.com:8080",
            "null", "https://localhost", "http://user@localhost", "not an origin" })
    void doFilter_foreignOrMalformedOrigin_rejectsWithoutInvokingTheChain(final String origin)
            throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn("127.0.0.1:8080");
        when(request.getHeader("Origin")).thenReturn(origin);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN),
                org.mockito.ArgumentMatchers.anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://localhost", "http://localhost:8080",
            "http://127.0.0.1:8080", "http://[::1]:8080" })
    void doFilter_loopbackHttpOrigin_continuesTheChain(final String origin) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn("127.0.0.1:8080");
        when(request.getHeader("Origin")).thenReturn(origin);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Code-review Agent 3 / Agent 6, and the security-review defeat-this pass (final gate
     * re-run): an {@code Origin} header whose port is malformed or overflows {@code int} makes
     * {@link java.net.URI} fall back to a registry-based authority with a {@code null} host.
     * {@code isAllowedOrigin} must reject this cleanly rather than let the immutable, null-hostile
     * allowlist throw a {@link NullPointerException} out of the filter — the exact disclosure the
     * method's own {@code REJECTION_MESSAGE} exists to avoid.
     */
    @ParameterizedTest
    @ValueSource(strings = { "http://localhost:99999999999999999999", "http:///x",
            "http://localhost:-1", "http:////localhost" })
    void doFilter_originWithNullHostFromMalformedAuthority_rejectsWithoutInvokingTheChain(
            final String origin) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn("127.0.0.1:8080");
        when(request.getHeader("Origin")).thenReturn(origin);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN),
                org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Code-review Agent 4 (prior-PR-comments, final gate re-run): DNS hostnames are
     * case-insensitive, so a legitimate browser-sent {@code Host: LOCALHOST} or
     * {@code Origin: http://LOCALHOST} must not be rejected merely because of its letter case.
     * This closes an unresolved Copilot comment from internal work item 2627 against {@code isAllowedHost}, and
     * the identical gap newly introduced by this diff's {@code isAllowedOrigin}.
     */
    @Test
    void doFilter_caseVariantLoopbackHostAndOrigin_continuesTheChain() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn("LOCALHOST:8080");
        when(request.getHeader("Origin")).thenReturn("http://LOCALHOST:8080");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Code-review Agent 4 (prior-PR-comments, final gate re-run): the original {@code stripPort}
     * took everything through the closing {@code ]} and silently discarded any trailing garbage
     * after it -- the identical bypass class a prior reviewer (internal work item 1510 F9) flagged against a
     * now-abandoned sibling filter's {@code hostOf()}. {@code "[::1].evil"} or {@code "[::1]junk"}
     * stripped to {@code "[::1]"}, which IS in the allowlist, so the forged host was accepted.
     * The only valid continuation after the closing bracket is nothing or {@code :<port>}; any
     * other suffix means this is not actually the bracketed literal alone and must fail closed.
     */
    @ParameterizedTest
    @ValueSource(strings = { "[::1].evil", "[::1]junk", "[::1]evil.com", "[::1]:8080junk" })
    void doFilter_ipv6LiteralWithTrailingGarbageAfterBracket_rejectsWithoutInvokingTheChain(
            final String hostHeader) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn(hostHeader);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Code-review Agent 6 (adversarial QA, final gate re-run): {@code stripPort}'s non-bracket
     * path truncated at the first {@code ':'} unconditionally, with no check that what followed
     * was actually numeric -- so {@code "127.0.0.1:evil.com"} stripped to the allowlisted
     * {@code "127.0.0.1"}. Confirmed via direct reflection on the compiled class that this was a
     * real (if not live-exploitable through this host's current Jetty connector, which happens
     * to reject a non-numeric port upstream) latent bypass in the filter's own logic -- this
     * filter must not depend on a fronting connector's behavior to stay fail-closed.
     */
    @ParameterizedTest
    @ValueSource(strings = { "127.0.0.1:evil.com", "localhost:evil.com", "127.0.0.1:8080evil" })
    void doFilter_hostnameFollowedByNonNumericPort_rejectsWithoutInvokingTheChain(
            final String hostHeader) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn(hostHeader);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * An IPv6 literal without its brackets is not a value {@code ALLOWED_HOSTNAMES} contains
     * (the filter's allowlist entry is {@code "[::1]"}, bracketed) -- documents that this is a
     * deliberately narrow allowlist match, not a loose one, since the connector this filter
     * protects binds IPv4 loopback only and a bare {@code "::1"} is not a form any legitimate
     * request to it would carry.
     */
    @Test
    void doFilter_unbracketedIpv6Literal_rejectsWithoutInvokingTheChain() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Host")).thenReturn("::1");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendError(org.mockito.ArgumentMatchers.eq(HttpServletResponse.SC_FORBIDDEN), org.mockito.ArgumentMatchers.anyString());
    }
}
