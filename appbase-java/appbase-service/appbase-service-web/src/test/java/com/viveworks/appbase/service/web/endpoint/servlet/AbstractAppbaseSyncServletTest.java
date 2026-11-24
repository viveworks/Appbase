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

package com.viveworks.appbase.service.web.endpoint.servlet;

import com.viveworks.appbase.core.service.tracer.TracerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AUD-20 (CWE-838): {@code AbstractAppbaseSyncServlet.readString} decoded the request body with
 * {@code new String(bytes)} -- the JVM's PLATFORM DEFAULT charset ({@code file.encoding}), an
 * environment property, not a wire contract. The same bytes could decode to a different string
 * depending on that environment property. A single JVM's default charset is fixed for its whole
 * lifetime, so this is verified across TWO separate JVMs, one per {@code file.encoding}, each
 * actually calling the real {@link AbstractAppbaseSyncServlet#readString} method (via {@link
 * CharsetProbeMain}) rather than re-deriving the decode logic here.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractAppbaseSyncServletTest {

    @AfterEach
    void clearMdcBetweenTests() {
        MDC.clear();
    }

    /**
     * AppMDC leak: {@code traceId()} falls through to {@code generateTraceId()} and
     * {@code MDC.put}s the result when no attribute/cookie/header/parameter is already set, but
     * nothing in any {@code doXxx} handler's {@code finally} block used to call
     * {@code MDC.remove}. A reused Jetty worker thread then carried that stale value into a LATER,
     * unrelated request on the same thread -- exactly the mechanism behind L-1291's intermittent
     * {@code TraceIdServletFilterTest} 500-vs-204 failure. Fixed two ways: {@code traceId()} no
     * longer reads ambient MDC at all (only the request attribute, cookie, header, parameter and
     * generation -- see {@link #traceIdPrefersTheCurrentRequestsCookieOverStaleAmbientMdcFromAnEarlierRequest()}),
     * and every handler now restores the ambient MDC snapshot it observed on entry via
     * {@code preserveAmbientTraceId()} rather than unconditionally removing it.
     */
    @Test
    void doHeadClearsTheGeneratedTraceIdSoALaterRequestOnTheSameThreadReadsItsOwnCookie() throws Exception {
        final Timer headTimer = mock(Timer.class);
        when(headTimer.time()).thenReturn(mock(Timer.Context.class));
        final String[] secondRequestTraceId = new String[1];
        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() {
            @Override
            protected int headImpl(
                    final String traceId,
                    final HttpServletRequest httpServletRequest,
                    final HttpServletResponse httpServletResponse)
            {
                secondRequestTraceId[0] = traceId;
                return 0;
            }
        };
        servlet.logger = mock(Logger.class);
        servlet.headTimer = headTimer;

        // First request: no attribute, no ambient MDC, no cookie/header/param -- traceId() must
        // generate a fresh id and MDC.put it as a side effect (the servlet's documented
        // logging-context behavior; not itself the bug).
        final HttpServletRequest firstRequest = mock(HttpServletRequest.class);
        final HttpServletResponse firstResponse = mock(HttpServletResponse.class);
        servlet.doHead(firstRequest, firstResponse);

        Assertions.assertNull(MDC.get(TracerService.TRACE_ID_KEY),
                "the first request's generated trace id must not survive past its own request " +
                        "-- a value still in MDC here is exactly the leak that corrupts a " +
                        "later request on a reused thread");

        // Second request, same thread (this test method runs on one thread, same as a reused
        // Jetty worker would): carries its OWN distinct trace-id cookie. If the first request's
        // id leaked, traceId()'s ambient-MDC branch would return it instead of this cookie.
        final HttpServletRequest secondRequest = mock(HttpServletRequest.class);
        final HttpServletResponse secondResponse = mock(HttpServletResponse.class);
        when(secondRequest.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] {
                new jakarta.servlet.http.Cookie(TracerService.TRACE_ID_KEY, "second-request-cookie-trace-id"),
        });
        servlet.doHead(secondRequest, secondResponse);

        Assertions.assertEquals("second-request-cookie-trace-id", secondRequestTraceId[0],
                "the second request must use its own cookie's trace id, not a value leaked from " +
                        "an earlier request on the same thread");
    }

    /**
     * H1, round 2: {@code traceId()} only bound its result into
     * MDC when it fell through to {@code generateTraceId()} -- a value resolved from the
     * attribute/cookie/header/parameter was returned WITHOUT ever being bound for the handler's
     * own scope, so logging during that request would see whatever was ambient on entry (nothing,
     * or a stale leftover), never the request's own resolved id. Proves both halves of the fix:
     * the fresh cookie is bound into MDC for the duration of {@code headImpl}, and the prior
     * outer value (simulating an outer {@code TraceIdServletFilter}'s own context) is exactly
     * what MDC holds again once the handler returns.
     */
    @Test
    void doHeadBindsTheFreshCookieIntoMdcForItsOwnScopeThenRestoresThePriorOuterValue() throws Exception {
        final Timer headTimer = mock(Timer.class);
        when(headTimer.time()).thenReturn(mock(Timer.Context.class));
        final String[] traceIdDuringHeadImpl = new String[1];
        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() {
            @Override
            protected int headImpl(
                    final String traceId,
                    final HttpServletRequest httpServletRequest,
                    final HttpServletResponse httpServletResponse)
            {
                traceIdDuringHeadImpl[0] = MDC.get(TracerService.TRACE_ID_KEY);
                return 0;
            }
        };
        servlet.logger = mock(Logger.class);
        servlet.headTimer = headTimer;

        MDC.put(TracerService.TRACE_ID_KEY, "prior-outer-value-set-before-this-handler-ran");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] {
                new jakarta.servlet.http.Cookie(TracerService.TRACE_ID_KEY, "this-requests-fresh-cookie"),
        });
        final HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doHead(request, response);

        Assertions.assertEquals("this-requests-fresh-cookie", traceIdDuringHeadImpl[0],
                "MDC must hold this request's own resolved trace id for the duration of the " +
                        "handler, not whatever was ambient when the handler started");
        Assertions.assertEquals("prior-outer-value-set-before-this-handler-ran",
                MDC.get(TracerService.TRACE_ID_KEY),
                "once the handler returns, MDC must be restored to exactly the value that was " +
                        "ambient before it ran -- never left holding this request's own value");
    }

    /**
     * H1: an unconditional {@code MDC.remove} in the servlet's own
     * {@code finally} would delete context an OUTER owner (a {@code TraceIdServletFilter} that
     * has already resolved and {@code MDC.put} a value before calling the servlet) still needs
     * for its own processing after the servlet returns. The fix must preserve, not erase, MDC
     * that this servlet invocation did not itself establish.
     */
    @Test
    void doHeadDoesNotEraseAnOuterOwnedTraceIdThatWasAlreadySetBeforeTheServletRan() throws Exception {
        final Timer headTimer = mock(Timer.class);
        when(headTimer.time()).thenReturn(mock(Timer.Context.class));
        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() { };
        servlet.logger = mock(Logger.class);
        servlet.headTimer = headTimer;

        // Simulates TraceIdServletFilter.doFilter() having already resolved and MDC.put a value,
        // and set it as a request attribute too, before calling chain.doFilter() into the servlet.
        MDC.put(TracerService.TRACE_ID_KEY, "outer-filter-owned-trace-id");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(TracerService.TRACE_ID_KEY)).thenReturn("outer-filter-owned-trace-id");
        final HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doHead(request, response);

        Assertions.assertEquals("outer-filter-owned-trace-id", MDC.get(TracerService.TRACE_ID_KEY),
                "the servlet must not erase MDC context it did not itself establish -- an outer " +
                        "filter still owns this value and needs it after the servlet returns");
    }

    /**
     * H1: the unsupported-method early-return path returns before
     * {@code traceId()} is ever called, so this servlet invocation never touches MDC at all --
     * it must not erase foreign context that happened to be present on entry.
     */
    @Test
    void doHeadDoesNotEraseForeignMdcOnTheUnsupportedMethodEarlyReturnPath() throws Exception {
        final Timer headTimer = mock(Timer.class);
        when(headTimer.time()).thenReturn(mock(Timer.Context.class));
        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() {
            @Override
            protected boolean isHeadSupported() {
                return false;
            }
        };
        servlet.logger = mock(Logger.class);
        servlet.headTimer = headTimer;
        servlet.headNotAllowedCounter = mock(Counter.class);

        MDC.put(TracerService.TRACE_ID_KEY, "foreign-context-unrelated-to-this-call");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doHead(request, response);

        Assertions.assertEquals("foreign-context-unrelated-to-this-call", MDC.get(TracerService.TRACE_ID_KEY),
                "a handler that returns before ever calling traceId() must leave MDC exactly as " +
                        "it found it, even when the method is unsupported");
    }

    /**
     * H2: the root precedence defect -- {@code traceId()} used to check
     * ambient thread MDC before the current request's own cookie/header/parameter, so a stale
     * value from an unrelated earlier request on a reused thread would defeat this request's
     * explicit, fresh trace id. Seeds that stale ambient value deliberately (simulating the leak
     * this fix otherwise prevents at the source) and proves the current request's own cookie
     * still wins -- a defense in depth check, independent of whether the leak itself is fixed.
     */
    @Test
    void traceIdPrefersTheCurrentRequestsCookieOverStaleAmbientMdcFromAnEarlierRequest() throws Exception {
        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() { };

        MDC.put(TracerService.TRACE_ID_KEY, "stale-ambient-trace-id-from-an-earlier-request");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] {
                new jakarta.servlet.http.Cookie(TracerService.TRACE_ID_KEY, "this-requests-own-cookie-trace-id"),
        });
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final String resolved = servlet.traceId(request, response);

        Assertions.assertEquals("this-requests-own-cookie-trace-id", resolved,
                "an explicit request-scoped trace id (cookie/header/parameter) must always win " +
                        "over stale ambient thread-local MDC from an unrelated earlier request");
    }

    @Test
    void authenticationFailureReturnsUnauthorizedWithoutExposingExceptionDetail() throws Exception {
        final Counter counter = mock(Counter.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() {
            @Override
            protected Counter counter(final String name) {
                return counter;
            }
        };
        servlet.logger = mock(Logger.class);

        servlet.handleAuthenticationServiceException("POST", "token validation detail", response);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        verify(response).flushBuffer();
    }

    @Test
    void readStringDecodesTheSameBytesIdenticallyUnderDifferentPlatformDefaultCharsets() throws Exception {
        // "café" in UTF-8 is 5 bytes: 'c','a','f', 0xC3,0xA9 (the two-byte encoding of 'é'). Under
        // ISO-8859-1, those same 5 bytes decode byte-for-byte into 5 DIFFERENT characters --
        // exactly the divergence this fix must eliminate.
        final byte[] bodyBytes = "café".getBytes(StandardCharsets.UTF_8);
        final String base64Body = Base64.getEncoder().encodeToString(bodyBytes);

        final String decodedUnderUtf8 = runProbe(base64Body, "UTF-8");
        final String decodedUnderIso88591 = runProbe(base64Body, "ISO-8859-1");

        Assertions.assertEquals(decodedUnderUtf8, decodedUnderIso88591,
                "the same request body bytes must decode to the same string regardless of the " +
                        "JVM's platform-default charset (file.encoding)");
        Assertions.assertEquals(codePointCsv("café"), decodedUnderUtf8,
                "the decoded string must actually be the UTF-8 interpretation of the bytes, not " +
                        "merely consistent");
    }

    /**
     * Runs {@link CharsetProbeMain} in a fresh JVM with the given {@code file.encoding}, on the
     * same classpath this very test is already running under, and returns the probe's reported
     * code-unit CSV.
     */
    private static String runProbe(final String base64Body, final String fileEncoding) throws Exception {
        final String javaBinary = System.getProperty("java.home") + "/bin/java";
        final String classpath = System.getProperty("java.class.path");

        final Process process = new ProcessBuilder(
                javaBinary,
                "-Dfile.encoding=" + fileEncoding,
                "-cp", classpath,
                CharsetProbeMain.class.getName(),
                base64Body)
                .start();

        final String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        final String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.US_ASCII);
        final int exitCode = process.waitFor();

        Assertions.assertEquals(0, exitCode, "probe process (file.encoding=" + fileEncoding + ") failed: " + stderr);
        return stdout.trim();
    }

    private static String codePointCsv(final String value) {
        final StringBuilder codeUnits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (i > 0) {
                codeUnits.append(',');
            }
            codeUnits.append((int) value.charAt(i));
        }
        return codeUnits.toString();
    }
}
