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

package com.viveworks.appbase.service.web.trace.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * H2 (L-1291 parity): {@code TraceIdJaxrsRequestFilter}
 * checked ambient thread MDC before the current request's own cookie/header/query parameter, and
 * {@code TraceIdJaxrsResponseFilter} unconditionally removed MDC instead of restoring whatever was
 * ambient before the request filter ran -- the same two defects fixed on the servlet/filter side.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class TraceIdJaxrsFilterPairTest {

    private final AppbaseCore appbaseCore = mock(AppbaseCore.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private final Map<String, Object> requestProperties = new HashMap<>();

    @BeforeEach
    void wireAppbaseCore() {
        when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
    }

    @AfterEach
    void clearMdcBetweenTests() {
        MDC.clear();
    }

    private ContainerRequestContext fakeRequestContext() {
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        doAnswer(invocation -> requestProperties.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(requestContext).setProperty(anyString(), org.mockito.ArgumentMatchers.any());
        when(requestContext.getProperty(anyString()))
                .thenAnswer(invocation -> requestProperties.get(invocation.getArgument(0, String.class)));
        return requestContext;
    }

    @Test
    void requestFilterPrefersTheCurrentRequestsCookieOverStaleAmbientMdcFromAnEarlierRequest() throws Exception {
        MDC.put(TracerService.TRACE_ID_KEY, "stale-ambient-trace-id-from-an-earlier-request");
        final ContainerRequestContext requestContext = fakeRequestContext();
        when(requestContext.getCookies()).thenReturn(Map.of(
                TracerService.TRACE_ID_KEY, new Cookie(TracerService.TRACE_ID_KEY, "this-requests-own-cookie-trace-id")));
        final TraceIdJaxrsRequestFilter filter = new TraceIdJaxrsRequestFilter() {
            @Override
            protected AppbaseCore appbaseCoreFromContext() {
                return appbaseCore;
            }
        };

        filter.filterRequest(requestContext);

        Assertions.assertEquals("this-requests-own-cookie-trace-id",
                MDC.get(TracerService.TRACE_ID_KEY),
                "an explicit request-scoped trace id (cookie/header/query parameter) must always " +
                        "win over stale ambient thread-local MDC from an unrelated earlier request");
    }

    @Test
    void responseFilterRestoresThePriorAmbientValueInsteadOfUnconditionallyRemoving() throws Exception {
        MDC.put(TracerService.TRACE_ID_KEY, "prior-outer-value-set-before-this-request-ran");
        final ContainerRequestContext requestContext = fakeRequestContext();
        when(requestContext.getCookies()).thenReturn(Map.of(
                TracerService.TRACE_ID_KEY, new Cookie(TracerService.TRACE_ID_KEY, "this-requests-own-cookie-trace-id")));
        final TraceIdJaxrsRequestFilter requestFilter = new TraceIdJaxrsRequestFilter() {
            @Override
            protected AppbaseCore appbaseCoreFromContext() {
                return appbaseCore;
            }
        };
        requestFilter.filterRequest(requestContext);

        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        when(responseContext.getHeaders()).thenReturn(new jakarta.ws.rs.core.MultivaluedHashMap<>());
        final TraceIdJaxrsResponseFilter responseFilter = new TraceIdJaxrsResponseFilter() {
            @Override
            protected AppbaseCore appbaseCoreFromContext() {
                return appbaseCore;
            }
        };

        responseFilter.filterResponse(requestContext, responseContext);

        Assertions.assertEquals("prior-outer-value-set-before-this-request-ran",
                MDC.get(TracerService.TRACE_ID_KEY),
                "the response filter must restore exactly the value that was ambient before the " +
                        "request filter ran, never leave this request's own value behind and " +
                        "never erase a value it did not itself establish");
    }
}
