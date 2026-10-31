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
import com.viveworks.appbase.service.user.account.data.CheckTokenRequest;
import com.viveworks.appbase.service.user.account.data.CheckTokenResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CheckTokenServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/checkToken";

    private static final long serialVersionUID = 591775769910168806L;

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CheckTokenRequest> postRequestType() {
        return CheckTokenRequest.class;
    }

    @Override
    protected <T extends Traceable> Object postImpl(final T traceable) throws Exception {
        return userAccountService().checkToken((CheckTokenRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CheckTokenResponse> postResponseType() {
        return CheckTokenResponse.class;
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
