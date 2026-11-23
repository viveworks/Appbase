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

import com.viveworks.appbase.service.user.account.data.ExistsAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ExistsAccountByIdResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ExistsAccountByIdpUserIdServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/existsAccountByIdpUserId";

    private static final long serialVersionUID = -3936669212058436341L;

    @Override
    protected int headImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String idpUserId = nonBlankParameter("idpUserId", httpServletRequest, httpServletResponse);
        final String appbaseRequesterId = parameter("appbaseRequesterId", httpServletRequest);

        final ExistsAccountByIdRequest existsAccountByIdRequest = new ExistsAccountByIdRequest(
                traceId, appbaseRequesterId, idpUserId);
        final ExistsAccountByIdResponse existsAccountByIdResponse =
                userAccountService().existsAccountByIdpUserId(existsAccountByIdRequest);
        if (existsAccountByIdResponse != null && existsAccountByIdResponse.isExists()) {
            return HttpServletResponse.SC_NO_CONTENT;
        }

        return HttpServletResponse.SC_NOT_FOUND;
    }

    @Override
    protected boolean isHeadResponseAutoHandled() {
        return true;
    }

    @Override
    protected boolean isHeadSupported() {
        return true;
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

    @Override
    protected boolean isGetSupported() {
        return false;
    }
}
