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

package com.viveworks.appbase.service.web.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AbstractAppbaseJaxrsFilter#isFiltered(ContainerRequestContext)} canonicalizes the request
 * path once neither {@code resourceInfo}'s class nor method supplied one. A malformed encoded path
 * (e.g. a raw {@code %2F}) makes {@link com.viveworks.appbase.service.path.AppbasePathCanonicalizer}
 * throw {@link com.viveworks.appbase.service.path.AppbasePathException}; that must not propagate
 * uncaught into a JAX-RS default 500 -- it must come back as a clean, fail-closed 400.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractAppbaseJaxrsFilterTest {

    /**
     * Test seam only: supplies {@code appbaseCoreFromContext()} directly, bypassing the {@code
     * Providers}-based JAX-RS context lookup that requires a running JAX-RS runtime -- same pattern as
     * {@code UserSessionJaxrsRequestFilter}'s test constructor.
     */
    private static final class TestFilter extends AbstractAppbaseJaxrsFilter {
        private final AppbaseCore appbaseCore;

        TestFilter(final AppbaseCore appbaseCore) {
            this.appbaseCore = appbaseCore;
        }

        @Override
        protected AppbaseCore appbaseCoreFromContext() {
            return appbaseCore;
        }
    }

    @Test
    void isFilteredSendsACleanBadRequestOnAMalformedPath() {
        final AppbaseCore appbaseCore = mock(AppbaseCore.class);
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        when(appbaseCore.getConfigurationService()).thenReturn(configurationService);

        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath(false)).thenReturn("/a%2Fb");

        final ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);

        // resourceInfo is left null (default constructor), so isFiltered() falls through to
        // canonicalizing containerRequestContext.getUriInfo().getPath(false) -- the line under test.
        final TestFilter filter = new TestFilter(appbaseCore);

        final WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> filter.isFiltered(containerRequestContext));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(),
                "a malformed path must fail closed with a clean 400, not an uncaught 500");
    }
}
