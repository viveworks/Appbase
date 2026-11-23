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

import com.viveworks.appbase.service.user.account.data.ReadAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ReadAccountByAppbaseUserIdServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/readAccountByAppbaseUserId";

    private static final long serialVersionUID = 2553995012452414085L;

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String appbaseUserId = nonBlankParameter("appbaseUserId", httpServletRequest, httpServletResponse);
        final String appbaseRequesterId = parameter("appbaseRequesterId", httpServletRequest);

        final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                traceId, appbaseRequesterId, appbaseUserId);
        return (T) userAccountService().readAccountByAppbaseUserId(readAccountByIdRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<ReadAccountByIdResponse> getResponseType() {
        return ReadAccountByIdResponse.class;
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
