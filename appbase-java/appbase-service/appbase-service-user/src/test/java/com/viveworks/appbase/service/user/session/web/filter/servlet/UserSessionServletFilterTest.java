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

package com.viveworks.appbase.service.user.session.web.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

/**
 * Unit coverage for {@link UserSessionServletFilter} focused on the A10-11 transport rule: the
 * session/user id are resolved from cookies and headers only. Tests drive the protected {@code
 * doFilter(request, response, chain, appbaseCore)} overload directly (same package).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class UserSessionServletFilterTest {

    private final UserSessionFilter userSessionFilter = Mockito.mock(UserSessionFilter.class);
    private final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    private final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    private final FilterChain chain = Mockito.mock(FilterChain.class);
    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

    private UserSessionServletFilter filter() {
        return new UserSessionServletFilter(userSessionFilter);
    }

    /**
     * A10-11 (CWE-598): an identity supplied in the URL query string is rejected before the neutral
     * filter can create an anonymous session.
     */
    @Test
    void anIdentitySuppliedInTheQueryStringIsRejected() throws Exception {
        Mockito.when(request.getQueryString()).thenReturn("appbaseUserId=queryUid");
        Mockito.when(request.getParameterMap()).thenReturn(Map.of(
                AppbaseServiceConstants.APPBASE_USER_ID_PARAM, new String[] {"queryUid"}));

        filter().doFilter(request, response, chain, appbaseCore);

        Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        Mockito.verify(response).flushBuffer();
        Mockito.verifyNoInteractions(userSessionFilter, chain);
    }
}
