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

package com.viveworks.appbase.service.user.session.web.filter;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.AuthenticationServiceException;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionRequest;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultUserSessionFilter implements UserSessionFilter {
    @Override
    public UserSession onRequest(
            final String requestPath,
            final AppbaseCore appbaseCore,
            @Nullable final String appbaseSessionId,
            @Nullable final String appbaseUserId)
            throws UserAccountServiceException, AuthorizationServiceException
    {
        AppbaseUtil.notBlank(requestPath, "requestPath");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        // Resolve UserAccountService lazily -- ONLY on the paths that actually use it (reading an
        // existing session, and the expiry check on a resolved session). A bare anonymous request
        // (blank appbaseSessionId with no createSessionIfAbsent configured) resolves no session and
        // must NOT require a UserAccountService to be registered: eagerly resolving it here would make
        // every anonymous request fail before the session-id check, so secure-by-default could not
        // work out of the box on a plain core with no user-account service wired.
        UserSession userSession;
        if (AppbaseUtil.isBlank(appbaseSessionId)) {
            userSession = handleNewSession(appbaseCore, requestPath);
        } else {
            userSession = readSession(userAccountService(appbaseCore), appbaseSessionId, appbaseUserId);
        }

        if (userSession != null) {
            final IsExpiredSessionRequest isExpiredSessionRequest = new IsExpiredSessionRequest();
            isExpiredSessionRequest.setUserSession(userSession);
            final IsExpiredSessionResponse isExpiredSessionResponse =
                    userAccountService(appbaseCore).isExpiredSession(isExpiredSessionRequest);
            if (isExpiredSessionResponse.isExpired()) {
                throw new UserSessionExpiredException(userSession);
            }
        }

        return userSession;
    }

    @Override
    public UserSession onResponse(
            final AppbaseCore appbaseCore,
            final UserSession userSession)
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(userSession, "userSession");

        final UserAccountService userAccountService = userAccountService(appbaseCore);
        if (userSession.getUserAccount() == null ||
                AppbaseUtil.isBlank(userSession.getUserAccount().getAppbaseUserId()))
        {
            if (!AppbaseUtil.isBlank(userSession.getAppbaseSessionId())) {
                final UpdateUnauthenticatedSessionRequest updateUnauthenticatedSessionRequest =
                        new UpdateUnauthenticatedSessionRequest();
                updateUnauthenticatedSessionRequest.setUserSessionData(userSession.getUserSessionData());
                updateUnauthenticatedSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
                updateUnauthenticatedSessionRequest.setReadAndUpdate(true);
                updateUnauthenticatedSessionRequest.setLastAccessTimestamp(userSession.getLastAccessTimestamp());

                final UpdateUnauthenticatedSessionResponse updateUnauthenticatedSessionResponse =
                        userAccountService.updateUnauthenticatedSession(updateUnauthenticatedSessionRequest);
                if (updateUnauthenticatedSessionResponse == null) {
                    throw new UserAccountServiceException("Failed to update: " + updateUnauthenticatedSessionRequest);
                }

                return updateUnauthenticatedSessionResponse.getUserSession();
            }
        } else {
            final UpdateSessionRequest updateSessionRequest = new UpdateSessionRequest();
            updateSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            updateSessionRequest.setAppbaseRequesterId(userSession.getAppbaseSessionId());
            updateSessionRequest.setUserSessionData(userSession.getUserSessionData());

            final UserAccount userAccount = userSession.getUserAccount();
            if (userAccount != null) {
                updateSessionRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
                updateSessionRequest.setReadAndUpdate(true);
            }
            updateSessionRequest.setAuthenticated(userSession.isAuthenticated());
            updateSessionRequest.setIdpUserToken(userSession.getIdpUserToken());
            updateSessionRequest.setLastAccessTimestamp(userSession.getLastAccessTimestamp());

            final UpdateSessionResponse updateSessionResponse = userAccountService.updateSession(updateSessionRequest);
            if (updateSessionResponse == null) {
                throw new UserAccountServiceException("Failed to update: " + updateSessionRequest);
            }

            return updateSessionResponse.getUserSession();
        }

        return userSession;
    }

    private UserSession readSession(
            final UserAccountService userAccountService,
            final String appbaseSessionId,
            @Nullable final String appbaseUserId)
            throws UserAccountServiceException, AuthorizationServiceException
    {
        UserSession userSession;
        if (AppbaseUtil.isBlank(appbaseUserId)) {
            final ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest =
                    new ReadUnauthenticatedSessionRequest();
            readUnauthenticatedSessionRequest.setAppbaseSessionId(appbaseSessionId);
            final ReadUnauthenticatedSessionResponse readUnauthenticatedSessionResponse =
                    userAccountService.readUnauthenticatedSession(readUnauthenticatedSessionRequest);
            if (readUnauthenticatedSessionResponse == null) {
                throw new UserSessionNotFoundException("No session response for appbaseSessionId=" + appbaseSessionId);
            }

            userSession = readUnauthenticatedSessionResponse.getUserSession();
            if (userSession == null) {
                throw new UserSessionNotFoundException("No session found for appbaseSessionId=" + appbaseSessionId);
            }
        } else {
            final ReadSessionRequest readSessionRequest = new ReadSessionRequest();
            readSessionRequest.setAppbaseUserId(appbaseUserId);
            readSessionRequest.setAppbaseSessionId(appbaseSessionId);
            readSessionRequest.setAppbaseRequesterId(appbaseUserId);
            final ReadSessionResponse readSessionResponse = userAccountService.readSession(readSessionRequest);
            if (readSessionResponse == null) {
                throw new UserSessionNotFoundException("No session response for [appbaseSessionId=" +
                        appbaseSessionId + ", appbaseUserId=" + appbaseUserId + "]");
            }

            userSession = readSessionResponse.getUserSession();
            if (userSession == null) {
                throw new UserSessionNotFoundException("No session found for appbaseSessionId=" + appbaseSessionId);
            }
        }

        return userSession;
    }

    private UserSession handleNewSession(final AppbaseCore appbaseCore, final String requestPath) {
        UserSession userSession = null;
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final UserSessionFilterConfiguration userSessionFilterConfiguration =
                configurationService.getTypedConfiguration(UserSessionFilterConfiguration.class);
        if (userSessionFilterConfiguration != null) {
            final Map<String, Boolean> createSessionIfAbsentByPathMap =
                    userSessionFilterConfiguration.getCreateSessionIfAbsentByPathMap();
            if (createSessionIfAbsentByPathMap != null) {
                final boolean containsWildcard =
                        createSessionIfAbsentByPathMap.containsKey(AppbaseServiceConstants.WILDCARD) &&
                        createSessionIfAbsentByPathMap.get(AppbaseServiceConstants.WILDCARD);
                final boolean createSessionIfAbsent =
                        createSessionIfAbsentByPathMap.containsKey(requestPath) &&
                                createSessionIfAbsentByPathMap.get(requestPath);
                if (containsWildcard || createSessionIfAbsent) {
                    // Do not set the id of the new session here since UserAccountService does it.
                    // We also do not want to rely on another service (IdService) here.
                    // The response filter will call UserAccountService.createUnauthenticatedSession() and
                    // put this new session in the session store.
                    final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
                    userSession = new UserSession();
                    userSession.setNewSession(true);
                    userSession.setAuthenticated(false);
                    userSession.setUserSessionData(new UserSessionData(new ConcurrentHashMap<>()));
                    userSession.setCreationTimestamp(now);
                    userSession.setLastAccessTimestamp(now);
                }
            }
        }

        return userSession;
    }

    private UserAccountService userAccountService(final AppbaseCore appbaseCore) throws UserAccountServiceException {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();

        UserAccountService userAccountService = null;
        final AppbaseServiceEntry userAccountServiceEntry = serviceRegistry.getAppbaseService(UserAccountService.class);
        if (userAccountServiceEntry != null) {
            userAccountService = (UserAccountService) userAccountServiceEntry.getService();
        }

        if (userAccountService == null) {
            throw new UserAccountServiceException("Cannot find an instance of " + UserAccountService.class.getName());
        }

        return userAccountService;
    }
}
