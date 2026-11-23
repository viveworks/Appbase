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

package com.viveworks.appbase.service.user.session.web.filter;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.AuthenticationServiceException;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.session.data.UserSession;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface UserSessionFilter {
    UserSession onRequest(
            String requestPath,
            AppbaseCore appbaseCore,
            @Nullable String appbaseSessionId,
            @Nullable String appbaseUserId)
            throws UserSessionExpiredException,
                UserSessionNotFoundException,
                UserAccountServiceException,
                AuthorizationServiceException;

    UserSession onResponse(
            AppbaseCore appbaseCore,
            UserSession userSession)
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException;
}
