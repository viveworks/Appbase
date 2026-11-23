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
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AfterIdpEmailPasswordSignInServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/afterIdpEmailPasswordSignIn";

    private static final long serialVersionUID = -373291622970589137L;

    @Override
    @SuppressWarnings("unchecked")
    protected Class<AfterIdpEmailPasswordSignInRequest> postRequestType() {
        return AfterIdpEmailPasswordSignInRequest.class;
    }

    @Override
    protected <T extends Traceable> AfterIdpEmailPasswordSignInResponse postImpl(final T traceable) throws Exception {
        return userAccountService()
                .afterIdpEmailPasswordSignIn((AfterIdpEmailPasswordSignInRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<AfterIdpEmailPasswordSignInResponse> postResponseType() {
        return AfterIdpEmailPasswordSignInResponse.class;
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
