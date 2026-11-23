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

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.session.data.UserSession;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserSessionExpiredException extends UserAccountServiceException {
    private static final long serialVersionUID = -810186875099126072L;

    private final UserSession expiredUserSession;

    public UserSessionExpiredException(final UserSession expiredUserSession) {
        AppbaseUtil.notNull(expiredUserSession, "expiredUserSession");

        this.expiredUserSession = expiredUserSession;
    }

    public UserSessionExpiredException(
            final UserSession expiredUserSession,
            final String message)
    {
        super(message);

        AppbaseUtil.notNull(expiredUserSession, "expiredUserSession");
        this.expiredUserSession = expiredUserSession;
    }

    public UserSessionExpiredException(
            final UserSession expiredUserSession,
            final String message,
            final Throwable cause)
    {
        super(message, cause);

        AppbaseUtil.notNull(expiredUserSession, "expiredUserSession");
        this.expiredUserSession = expiredUserSession;
    }

    public UserSessionExpiredException(
            final UserSession expiredUserSession,
            final Throwable cause)
    {
        super(cause);

        AppbaseUtil.notNull(expiredUserSession, "expiredUserSession");
        this.expiredUserSession = expiredUserSession;
    }

    public UserSessionExpiredException(
            final UserSession expiredUserSession,
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);

        AppbaseUtil.notNull(expiredUserSession, "expiredUserSession");
        this.expiredUserSession = expiredUserSession;
    }

    public UserSession getExpiredUserSession() {
        return expiredUserSession;
    }
}
