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

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.ServletException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractUserAccountServlet extends AbstractAppbaseSyncServlet {
    protected static final String DEFAULT_PATH_PREFIX = "/account";

    private static final long serialVersionUID = 7124908748863835749L;

    protected UserAccountService userAccountService() throws ServletException {
        final AppbaseCore appbaseCore = appbaseCore();
        if (appbaseCore == null) {
            throw new ServletException("Cannot find an instance of " + AppbaseCore.class.getName());
        }

        final AppbaseServiceEntry appbaseServiceEntry = appbaseCore
                .getServiceRegistry()
                .getAppbaseService(UserAccountService.class);
        if (appbaseServiceEntry == null) {
            throw new ServletException("Cannot find a service entry for " + UserAccountService.class.getName());
        }

        final UserAccountService userAccountService = (UserAccountService) appbaseServiceEntry.getService();
        if (userAccountService == null) {
            throw new ServletException("Cannot find an instance of " + UserAccountService.class.getName());
        }

        return userAccountService;
    }
}
