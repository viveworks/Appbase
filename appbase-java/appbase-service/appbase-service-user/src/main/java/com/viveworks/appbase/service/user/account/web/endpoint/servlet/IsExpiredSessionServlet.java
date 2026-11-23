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

package com.viveworks.appbase.service.user.account.web.endpoint.servlet;

import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionRequest;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class IsExpiredSessionServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/isExpiredSession";

    private static final long serialVersionUID = -2133664070660445422L;

    @Override
    @SuppressWarnings("unchecked")
    protected Class<IsExpiredSessionRequest> putRequestType() {
        return IsExpiredSessionRequest.class;
    }

    @Override
    protected <T extends Traceable> Object putImpl(final T traceable) throws Exception {
        return userAccountService().isExpiredSession((IsExpiredSessionRequest) traceable);
    }

    @Override
    protected void handlePutResponse(
            final Object response,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        if (response != null && IsExpiredSessionResponse.class.isAssignableFrom(response.getClass())) {
            if (!((IsExpiredSessionResponse) response).isExpired()) {
                sendNoContent(httpServletResponse);
                return;
            }
        }

        sendNotFound("PUT", httpServletResponse);
    }

    @Nullable
    @Override
    protected <T> Class<T> putResponseType() {
        return null;
    }

    @Override
    protected boolean isPutSupported() {
        return true;
    }

    @Override
    protected boolean isPutResponseAutoHandled() {
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

    @Override
    protected boolean isDeleteSupported() {
        return false;
    }
}
