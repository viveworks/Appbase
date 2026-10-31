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
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AfterIdpCreateUserWithEmailPasswordServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/afterIdpCreateUserWithEmailPassword";

    private static final long serialVersionUID = 3867996379800052078L;

    @Override
    @SuppressWarnings("unchecked")
    protected Class<AfterIdpCreateUserWithEmailPasswordRequest> postRequestType() {
        return AfterIdpCreateUserWithEmailPasswordRequest.class;
    }

    @Override
    protected <T extends Traceable> AfterIdpCreateUserWithEmailPasswordResponse postImpl(
            final T traceable) throws Exception
    {
        final AfterIdpCreateUserWithEmailPasswordResponse response = userAccountService()
                .afterIdpCreateUserWithEmailPassword((AfterIdpCreateUserWithEmailPasswordRequest) traceable);
        final UserSession userSession = response.getUserSession();
        final UserAccount sanitizedUserAccount = new UserAccount(userSession.getUserAccount(), null);
        final UserSession sanitizedUserSession = new UserSession(userSession, sanitizedUserAccount);
        return new AfterIdpCreateUserWithEmailPasswordResponse(response.getTraceId(), sanitizedUserSession);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<AfterIdpCreateUserWithEmailPasswordResponse> postResponseType() {
        return AfterIdpCreateUserWithEmailPasswordResponse.class;
    }

    @Override
    protected boolean isPostSupported() {
        return true;
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
    protected boolean isPutSupported() {
        return false;
    }

    @Override
    protected boolean isDeleteSupported() {
        return false;
    }
}
