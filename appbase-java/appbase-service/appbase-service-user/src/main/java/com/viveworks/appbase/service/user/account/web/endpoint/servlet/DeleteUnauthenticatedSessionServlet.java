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

import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.service.user.account.data.DeleteUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.DeleteUnauthenticatedSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DeleteUnauthenticatedSessionServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/deleteUnauthenticatedSession";

    private static final long serialVersionUID = -6400087306676149426L;

    @Override
    protected <T extends Traceable> Object deleteImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String appbaseSessionId = nonBlankParameter("appbaseSessionId", httpServletRequest, httpServletResponse);

        final DeleteUnauthenticatedSessionRequest deleteUnauthenticatedSessionRequest =
                new DeleteUnauthenticatedSessionRequest(traceId, appbaseSessionId);
        return userAccountService().deleteUnauthenticatedSession(deleteUnauthenticatedSessionRequest);
    }

    @Override
    protected boolean isDeleteResponseAutoHandled() {
        return false;
    }

    @Override
    protected void handleDeleteResponse(
            final Object response,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        if (response != null && DeleteUnauthenticatedSessionResponse.class.isAssignableFrom(response.getClass())) {
            if (((DeleteUnauthenticatedSessionResponse) response).isDeleted()) {
                sendNoContent(httpServletResponse);
                return;
            }
        }

        sendNotFound("DELETE", httpServletResponse);
    }

    @Override
    protected boolean isDeleteSupported() {
        return true;
    }

    @Override
    protected boolean isPutSupported() {
        return false;
    }

    @Override
    protected boolean isPostSupported() {
        return false;
    }

    @Override
    protected boolean isHeadSupported() {
        return false;
    }

    @Override
    protected boolean isGetSupported() {
        return false;
    }
}
