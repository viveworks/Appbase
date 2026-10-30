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
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CreateUnauthenticatedSessionServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/createUnauthenticatedSession";

    private static final long serialVersionUID = 6530504174672093618L;

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateUnauthenticatedSessionRequest> postRequestType() {
        return CreateUnauthenticatedSessionRequest.class;
    }

    @Override
    protected <T extends Traceable> Object postImpl(final T traceable) throws Exception {
        return userAccountService().createUnauthenticatedSession((CreateUnauthenticatedSessionRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateUnauthenticatedSessionResponse> postResponseType() {
        return CreateUnauthenticatedSessionResponse.class;
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
