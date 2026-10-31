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
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UpdateGuestUserSessionServlet extends AbstractUserAccountServlet {
    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/updateGuestUserSession";

    private static final long serialVersionUID = -6473128709647891771L;

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateGuestSessionRequest> putRequestType() {
        return UpdateGuestSessionRequest.class;
    }

    @Override
    protected <T extends Traceable> Object putImpl(final T traceable) throws Exception {
        return userAccountService().updateGuestSession((UpdateGuestSessionRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateGuestSessionResponse> putResponseType() {
        return UpdateGuestSessionResponse.class;
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
