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
import com.viveworks.appbase.service.user.account.data.UpdateAccountRequest;
import com.viveworks.appbase.service.user.account.data.UpdateAccountResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UpdateAccountServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/updateAccount";

    private static final long serialVersionUID = 8728996127975618392L;

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateAccountRequest> putRequestType() {
        return UpdateAccountRequest.class;
    }

    @Override
    protected <T extends Traceable> Object putImpl(final T traceable) throws Exception {
        return userAccountService().updateAccount((UpdateAccountRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateAccountResponse> putResponseType() {
        return super.putResponseType();
    }

    @Override
    protected boolean isPutSupported() {
        return true;
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
