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

package com.viveworks.appbase.service.user.account.web.endpoint.servlet;

import com.viveworks.appbase.service.user.account.data.ReadSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ReadSessionServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/readSession";

    private static final long serialVersionUID = -2324266232244164831L;

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String appbaseUserId = nonBlankParameter("appbaseUserId", httpServletRequest, httpServletResponse);
        final String appbaseSessionId = nonBlankParameter("appbaseSessionId", httpServletRequest, httpServletResponse);
        final String appbaseRequesterId = parameter("appbaseRequesterId", httpServletRequest);

        final ReadSessionRequest readSessionRequest = new ReadSessionRequest(
                traceId, appbaseRequesterId, appbaseUserId, appbaseSessionId);
        return (T) userAccountService().readSession(readSessionRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<ReadSessionResponse> getResponseType() {
        return ReadSessionResponse.class;
    }

    @Override
    protected boolean isGetSupported() {
        return true;
    }

    @Override
    protected boolean isHeadSupported() {
        return false;
    }

    @Override
    protected boolean isDeleteSupported() {
        return false;
    }

    @Override
    protected boolean isPutSupported() {
        return false;
    }

    @Override
    protected boolean isPostSupported() {
        return false;
    }
}
