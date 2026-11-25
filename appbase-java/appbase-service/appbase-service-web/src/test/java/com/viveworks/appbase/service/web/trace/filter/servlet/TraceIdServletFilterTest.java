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

package com.viveworks.appbase.service.web.trace.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M1 parity (L-1291): {@code AbstractAppbaseSyncServlet#traceId()} had
 * the same defect fixed here -- checking ambient thread MDC before the current request's own
 * cookie/header/parameter, so a stale value left by an unrelated earlier request on a reused
 * servlet-container worker thread could defeat this request's explicit, fresh trace id.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class TraceIdServletFilterTest {

    private final AppbaseCore appbaseCore = mock(AppbaseCore.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private final TraceIdServletFilter filter = new TraceIdServletFilter();

    @BeforeEach
    void wireAppbaseCore() {
        when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
    }

    @AfterEach
    void clearMdcBetweenTests() {
        MDC.clear();
    }

    @Test
    void doFilterPrefersTheCurrentRequestsCookieOverStaleAmbientMdcFromAnEarlierRequest() throws Exception {
        MDC.put(TracerService.TRACE_ID_KEY, "stale-ambient-trace-id-from-an-earlier-request");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] {
                new jakarta.servlet.http.Cookie(TracerService.TRACE_ID_KEY, "this-requests-own-cookie-trace-id"),
        });
        final HttpServletResponse response = mock(HttpServletResponse.class);

        // The filter's own finally always clears MDC once chain.doFilter(...) returns -- by
        // design, that value is scoped to the downstream chain, not to the filter's own caller.
        // Capture it DURING the chain call, same as the downstream code this filter exists to
        // serve would observe it.
        final String[] traceIdDuringChain = new String[1];
        final FilterChain capturingChain = (req, res) -> traceIdDuringChain[0] = MDC.get(TracerService.TRACE_ID_KEY);

        filter.doFilter(request, response, capturingChain, appbaseCore);

        Assertions.assertEquals("this-requests-own-cookie-trace-id", traceIdDuringChain[0],
                "an explicit request-scoped trace id (cookie/header/parameter) must always win " +
                        "over stale ambient thread-local MDC from an unrelated earlier request");
    }
}
