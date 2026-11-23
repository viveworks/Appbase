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

package com.viveworks.appbase.service.user.account;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthorizationContext;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.cluster.ClusterService;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetEmailSentRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetEmailSentResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetSubmitRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetSubmitResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpRefreshAuthenticationTokenRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpRefreshAuthenticationTokenResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpSignOutRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpSignOutResponse;
import com.viveworks.appbase.service.user.account.data.CheckTokenRequest;
import com.viveworks.appbase.service.user.account.data.CheckTokenResponse;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByAppbaseUserIdRequest;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByAppbaseUserIdResponse;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByIdpUserIdRequest;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByIdpUserIdResponse;
import com.viveworks.appbase.service.user.account.data.DeleteGuestUserRequest;
import com.viveworks.appbase.service.user.account.data.DeleteGuestUserResponse;
import com.viveworks.appbase.service.user.account.data.DeleteSessionRequest;
import com.viveworks.appbase.service.user.account.data.DeleteSessionResponse;
import com.viveworks.appbase.service.user.account.data.DeleteUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.DeleteUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.ExistsAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ExistsAccountByIdResponse;
import com.viveworks.appbase.service.user.account.data.GuestUser;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionRequest;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdResponse;
import com.viveworks.appbase.service.user.account.data.ReadGuestUserSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadGuestUserSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.RenewSessionRequest;
import com.viveworks.appbase.service.user.account.data.RenewSessionResponse;
import com.viveworks.appbase.service.user.account.data.SyncIdpUserRequest;
import com.viveworks.appbase.service.user.account.data.SyncIdpUserResponse;
import com.viveworks.appbase.service.user.account.data.UpdateAccountRequest;
import com.viveworks.appbase.service.user.account.data.UpdateAccountResponse;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.store.UserAccountStore;
import com.viveworks.appbase.service.user.account.store.UserAccountStoreException;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;
import com.viveworks.appbase.service.user.session.store.UserSessionStoreException;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This account service mainly acts as a storage backup for Firebase Auth or another IDP. It just stores user account
 * details and manages user session. It does not actually do authentication.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultUserAccountService
        extends AbstractStatelessAppbaseService<DefaultUserAccountServiceConfiguration>
        implements UserAccountService
{
    private final ValidationService validationService;
    private final IdService idService;
    private final ClusterService clusterService;
    private final AuthorizationService authorizationService;
    private final UserSessionStore userSessionStore;
    private final UserAccountStore userAccountStore;
    private final IdpTokenVerifier idpTokenVerifier;
    private final PasswordService passwordService;

    private final Timer createUserWithEmailAndPasswordTimer;
    private final Timer createUnauthenticatedSessionTimer;
    private final Timer existsAccountByAppbaseUserIdTimer;
    private final Timer existsAccountByIdpUserIdTimer;
    private final Timer existsAccountByEmailTimer;
    private final Timer readAccountByAppbaseUserIdTimer;
    private final Timer readAccountByIdpUserIdTimer;
    private final Timer readAccountByEmailTimer;
    private final Timer readSessionTimer;
    private final Timer readUnauthenticatedSessionTimer;
    private final Timer emailPasswordSignInTimer;
    private final Timer guestSignInTimer;
    private final Timer readGuestUserSessionTimer;
    private final Timer refreshAuthenticationTokenTimer;
    private final Timer signOutRequestTimer;
    private final Timer checkTokenTimer;
    private final Timer isExpiredSessionTimer;
    private final Timer renewSessionTimer;
    private final Timer updateAccountTimer;
    private final Timer updatePasswordResetEmailSentTimer;
    private final Timer updateGuestSessionTimer;
    private final Timer updateSessionTimer;
    private final Timer updateUnauthenticatedSessionTimer;
    private final Timer passwordResetTimer;
    private final Timer confirmPasswordResetTimer;
    private final Timer deleteSessionTimer;
    private final Timer deleteAccountByIdpUserIdTimer;
    private final Timer deleteAccountByAppbaseUserIdTimer;
    private final Timer deleteGuestUserTimer;
    private final Timer deleteUnauthenticatedSessionTimer;
    private final Timer syncIdpUserTimer;

    public DefaultUserAccountService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ServiceRegistry serviceRegistry,
            final IdService idService,
            final ClusterService clusterService,
            final AuthorizationService authorizationService,
            final UserSessionStore userSessionStore,
            final UserAccountStore userAccountStore,
            final IdpTokenVerifier idpTokenVerifier,
            final PasswordService passwordService,
            final DefaultUserAccountServiceConfiguration defaultUserAccountServiceConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, defaultUserAccountServiceConfiguration);

        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(idService, "idService");
        AppbaseUtil.notNull(clusterService, "clusterService");
        AppbaseUtil.notNull(authorizationService, "authorizationService");
        AppbaseUtil.notNull(userSessionStore, "userSessionStore");
        AppbaseUtil.notNull(userAccountStore, "userAccountStore");
        AppbaseUtil.notNull(idpTokenVerifier, "idpTokenVerifier");
        AppbaseUtil.notNull(passwordService, "passwordService");
        AppbaseUtil.notNull(defaultUserAccountServiceConfiguration, "defaultUserAccountServiceConfiguration");

        this.validationService = validationService;
        this.idService = idService;
        this.clusterService = clusterService;
        this.authorizationService = authorizationService;
        this.userSessionStore = userSessionStore;
        this.userAccountStore = userAccountStore;
        this.idpTokenVerifier = idpTokenVerifier;
        this.passwordService = passwordService;

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createUserWithEmailAndPasswordTimer =
                metricRegistry.timer(metricService.name(getClass(), "createUserWithEmailAndPasswordTimer"));
        this.createUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "createUnauthenticatedSessionTimer"));
        this.existsAccountByAppbaseUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsAccountByAppbaseUserIdTimer"));
        this.existsAccountByIdpUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsAccountByIdpUserIdTimer"));
        this.existsAccountByEmailTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsAccountByEmailTimer"));
        this.readAccountByAppbaseUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readAccountByAppbaseUserIdTimer"));
        this.readAccountByIdpUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readAccountByIdpUserIdTimer"));
        this.readAccountByEmailTimer =
                metricRegistry.timer(metricService.name(getClass(), "readAccountByEmailTimer"));
        this.readSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "readSessionTimer"));
        this.emailPasswordSignInTimer =
                metricRegistry.timer(metricService.name(getClass(), "emailPasswordSignInTimer"));
        this.guestSignInTimer =
                metricRegistry.timer(metricService.name(getClass(), "guestSignInTimer"));
        this.readGuestUserSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "readGuestUserSessionTimer"));
        this.readUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "readUnauthenticatedSessionTimer"));
        this.refreshAuthenticationTokenTimer =
                metricRegistry.timer(metricService.name(getClass(), "refreshAuthenticationTokenTimer"));
        this.signOutRequestTimer =
                metricRegistry.timer(metricService.name(getClass(), "signOutRequestTimer"));
        this.checkTokenTimer =
                metricRegistry.timer(metricService.name(getClass(), "checkTokenTimer"));
        this.isExpiredSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "isExpiredSessionTimer"));
        this.renewSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "renewSessionTimer"));
        this.updateAccountTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateAccountTimer"));
        this.updatePasswordResetEmailSentTimer =
                metricRegistry.timer(metricService.name(getClass(), "updatePasswordResetEmailSentTimer"));
        this.updateSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateSessionTimer"));
        this.updateGuestSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateGuestSessionTimer"));
        this.updateUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateUnauthenticatedSessionTimer"));
        this.passwordResetTimer =
                metricRegistry.timer(metricService.name(getClass(), "passwordResetTimer"));
        this.confirmPasswordResetTimer =
                metricRegistry.timer(metricService.name(getClass(), "confirmPasswordResetTimer"));
        this.deleteSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteSessionTimer"));
        this.deleteAccountByIdpUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteAccountByIdpUserIdTimer"));
        this.deleteAccountByAppbaseUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteAccountByAppbaseUserIdTimer"));
        this.deleteGuestUserTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteGuestUserTimer"));
        this.deleteUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteUnauthenticatedSessionTimer"));
        this.syncIdpUserTimer =
                metricRegistry.timer(metricService.name(getClass(), "syncIdpUserTimer"));
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(UserAccountService.class, this);
        serviceRegistry.registerAppbaseService(DefaultUserAccountService.class, this);
    }

    @Override
    public AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPassword(
            final AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpCreateUserWithEmailPasswordRequest, "afterIdpCreateUserWithEmailPasswordRequest");
        AppbaseUtil.validate(
                new AfterIdpCreateUserWithEmailPasswordRequest.Validator(), afterIdpCreateUserWithEmailPasswordRequest);
        verifyUserIdentity(
                afterIdpCreateUserWithEmailPasswordRequest.getIdpUserToken(),
                afterIdpCreateUserWithEmailPasswordRequest.getIdpUserId(),
                afterIdpCreateUserWithEmailPasswordRequest.getEmail());

        final Timer.Context context = createUserWithEmailAndPasswordTimer.time();
        try {
            final String homeDataCenter = clusterService.getHomeDataCenter();
            final String appbaseUserId = idService.newId();
            final String idpUserId = afterIdpCreateUserWithEmailPasswordRequest.getIdpUserId();
            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            final PasswordHash clientPasswordHash = afterIdpCreateUserWithEmailPasswordRequest.getClientPasswordHash();
            final PasswordHash serverPasswordHash = passwordService.hashWithRandomSalt(clientPasswordHash.getHash());
            final PasswordHashes passwordHashes = PasswordHashes
                    .builder()
                    .clientPasswordHash(clientPasswordHash)
                    .serverPasswordHash(serverPasswordHash)
                    .build();

            final UserAccount userAccount = UserAccount
                    .builder()
                    .appbaseUserId(appbaseUserId)
                    .creationTimestamp(now)
                    .deleted(false)
                    .email(afterIdpCreateUserWithEmailPasswordRequest.getEmail())
                    .emailVerified(false)
                    .idpUserId(idpUserId)
                    .languageCode(afterIdpCreateUserWithEmailPasswordRequest.getLanguageCode())
                    .lastAccessTimestamp(now)
                    .name(afterIdpCreateUserWithEmailPasswordRequest.getName())
                    .passwordResetEmailSent(false)
                    .passwordHashes(passwordHashes)
                    .userTopic(appbaseUserId)
                    .userHomeDataCenter(homeDataCenter)
                    .addressLine1(afterIdpCreateUserWithEmailPasswordRequest.getAddressLine1())
                    .addressLine2(afterIdpCreateUserWithEmailPasswordRequest.getAddressLine2())
                    .city(afterIdpCreateUserWithEmailPasswordRequest.getCity())
                    .stateProvince(afterIdpCreateUserWithEmailPasswordRequest.getStateProvince())
                    .country(afterIdpCreateUserWithEmailPasswordRequest.getCountry())
                    .postalCode(afterIdpCreateUserWithEmailPasswordRequest.getPostalCode())
                    .latitude(afterIdpCreateUserWithEmailPasswordRequest.getLatitude())
                    .longitude(afterIdpCreateUserWithEmailPasswordRequest.getLongitude())
                    .build();
            checkAuthorized(
                    afterIdpCreateUserWithEmailPasswordRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("userAccount", userAccount),
                    "createUserWithEmailAndPassword");

            final int created;
            try {
                created = userAccountStore.createUserAccount(userAccount);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.StoreAccountException(userAccount);
            }

            if (created != 1) {
                throw new UserAccountServiceException.CreateAccountException(userAccount);
            }

            final UserSession userSession = storeNewUserSession(
                    userAccount,
                    afterIdpCreateUserWithEmailPasswordRequest.getIdpUserToken(),
                    afterIdpCreateUserWithEmailPasswordRequest.getAppName(),
                    afterIdpCreateUserWithEmailPasswordRequest.getUserSessionData());
            return new AfterIdpCreateUserWithEmailPasswordResponse(
                    afterIdpCreateUserWithEmailPasswordRequest.getTraceId(), userSession);
        } finally {
            context.stop();
        }
    }

    @Override
    public AfterIdpEmailPasswordSignInResponse afterIdpEmailPasswordSignIn(
            final AfterIdpEmailPasswordSignInRequest afterIdpEmailPasswordSignInRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpEmailPasswordSignInRequest, "afterIdpEmailPasswordSignInRequest");
        AppbaseUtil.validate(new AfterIdpEmailPasswordSignInRequest.Validator(), afterIdpEmailPasswordSignInRequest);
        verifyUserIdentity(
                afterIdpEmailPasswordSignInRequest.getIdpUserToken(),
                afterIdpEmailPasswordSignInRequest.getIdpUserId(),
                afterIdpEmailPasswordSignInRequest.getEmail());

        final Timer.Context context = emailPasswordSignInTimer.time();
        try {
            UserAccount userAccount;
            UserSession userSession = null;
            try {
                final String idpUserId = afterIdpEmailPasswordSignInRequest.getIdpUserId();
                checkAuthorized(
                        afterIdpEmailPasswordSignInRequest.getAppbaseRequesterId(),
                        null,
                        Map.of("idpUserId", idpUserId),
                        "emailPasswordSignIn");

                final Optional<UserAccount> userAccountOptional = userAccountStore.readByIdpUserId(idpUserId);
                if (userAccountOptional.isEmpty()) {
                    final SyncIdpUserRequest syncIdpUserRequest = new SyncIdpUserRequest();
                    syncIdpUserRequest.setIdpUserId(afterIdpEmailPasswordSignInRequest.getIdpUserId());
                    syncIdpUserRequest.setIdpUserToken(afterIdpEmailPasswordSignInRequest.getIdpUserToken());
                    syncIdpUserRequest.setEmail(afterIdpEmailPasswordSignInRequest.getEmail());
                    syncIdpUserRequest.setClientPasswordHash(
                            afterIdpEmailPasswordSignInRequest.getClientPasswordHash());
                    syncIdpUserRequest.setAppbaseRequesterId(
                            afterIdpEmailPasswordSignInRequest.getAppbaseRequesterId());
                    syncIdpUserRequest.setUserSessionData(afterIdpEmailPasswordSignInRequest.getUserSessionData());
                    syncIdpUserRequest.setTraceId(afterIdpEmailPasswordSignInRequest.getTraceId());
                    syncIdpUserRequest.setAppName(afterIdpEmailPasswordSignInRequest.getAppName());
                    syncIdpUserRequest.setLanguageCode(afterIdpEmailPasswordSignInRequest.getLanguageCode());

                    final SyncIdpUserResponse syncIdpUserResponse = syncIdpUser(syncIdpUserRequest);
                    userSession = syncIdpUserResponse.getUserSession();
                    userAccount = userSession.getUserAccount();
                } else {
                    userAccount = userAccountOptional.get();
                }
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException(
                        "Error reading account from store: " + afterIdpEmailPasswordSignInRequest, e);
            }

            userAccount = updateOnLoginIfNeeded(
                    userAccount,
                    afterIdpEmailPasswordSignInRequest.getClientPasswordHash(),
                    afterIdpEmailPasswordSignInRequest.getAppbaseRequesterId(),
                    afterIdpEmailPasswordSignInRequest.getTraceId());

            // syncIdpUser() might have already created the session, do not store it again
            if (userSession == null) {
                userSession = storeNewUserSession(
                        userAccount,
                        afterIdpEmailPasswordSignInRequest.getIdpUserToken(),
                        afterIdpEmailPasswordSignInRequest.getAppName(),
                        afterIdpEmailPasswordSignInRequest.getUserSessionData());
            }

            return new AfterIdpEmailPasswordSignInResponse(
                    afterIdpEmailPasswordSignInRequest.getTraceId(), userSession);
        } finally {
            context.stop();
        }
    }

    private UserAccount updateOnLoginIfNeeded(
            final UserAccount currentUserAccount,
            final PasswordHash clientPasswordHash,
            final String appbaseRequesterId,
            @Nullable final String traceId)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        final UpdateAccountRequest updateAccountRequest = new UpdateAccountRequest();
        updateAccountRequest.setReadAndUpdate(false);
        updateAccountRequest.setTraceId(traceId);
        updateAccountRequest.setAppbaseRequesterId(appbaseRequesterId);
        updateAccountRequest.setAppbaseUserId(currentUserAccount.getAppbaseUserId());

        boolean update = false;
        final boolean clearPasswordResetCodeHash = currentUserAccount.getPasswordResetCodeHash() != null;
        if (currentUserAccount.getPasswordResetEmailSent() != null && currentUserAccount.getPasswordResetEmailSent()) {
            update = true;
            updateAccountRequest.setPasswordResetEmailSent(false);
        }

        if (clearPasswordResetCodeHash) {
            update = true;
        }

        if (!clientPasswordHash.equals(currentUserAccount.getPasswordHashes().getClientPasswordHash())) {
            update = true;
            updateAccountRequest.setClientPasswordHash(clientPasswordHash);
        }

        if (update) {
            final UpdateAccountResponse updateAccountResponse = updateAccount(
                    updateAccountRequest, clearPasswordResetCodeHash);
            if (!updateAccountResponse.isUpdated()) {
                throw new UserAccountServiceException.EmptyResultException(
                        "Account maintenance did not update user: " + currentUserAccount.getAppbaseUserId());
            }

            try {
                return userAccountStore.readByAppbaseUserId(currentUserAccount.getAppbaseUserId())
                        .orElseThrow(() -> new UserAccountServiceException.EmptyResultException(
                                "Account missing after sign-in maintenance: "
                                        + currentUserAccount.getAppbaseUserId()));
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException(
                        "Error reading account after sign-in maintenance: "
                                + currentUserAccount.getAppbaseUserId(), e);
            }
        }

        return currentUserAccount;
    }

    @Override
    public AfterIdpGuestSignInResponse afterIdpGuestSignIn(
            final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpGuestSignInRequest, "afterIdpGuestSignInRequest");
        AppbaseUtil.validate(new AfterIdpGuestSignInRequest.Validator(), afterIdpGuestSignInRequest);
        verifyUserToken(afterIdpGuestSignInRequest.getIdpGuestUserToken());

        final Timer.Context context = guestSignInTimer.time();
        try {
            final String appbaseUserId = idService.newId();
            final GuestUser guestUser = new GuestUser();
            guestUser.setAppbaseUserId(appbaseUserId);
            guestUser.setIdpGuestId(afterIdpGuestSignInRequest.getIdpGuestId());
            guestUser.setName(afterIdpGuestSignInRequest.getName());
            guestUser.setLanguageCode(afterIdpGuestSignInRequest.getLanguageCode());
            guestUser.setAddressLine1(afterIdpGuestSignInRequest.getAddressLine1());
            guestUser.setAddressLine2(afterIdpGuestSignInRequest.getAddressLine2());
            guestUser.setCity(afterIdpGuestSignInRequest.getCity());
            guestUser.setStateProvince(afterIdpGuestSignInRequest.getStateProvince());
            guestUser.setCountry(afterIdpGuestSignInRequest.getCountry());
            guestUser.setPostalCode(afterIdpGuestSignInRequest.getPostalCode());
            guestUser.setLatitude(afterIdpGuestSignInRequest.getLatitude());
            guestUser.setLongitude(afterIdpGuestSignInRequest.getLongitude());

            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            guestUser.setCreationTimestamp(now);
            guestUser.setLastAccessTimestamp(now);

            final UserSessionData userSessionData = afterIdpGuestSignInRequest.getUserSessionData();
            final Map<String, String> dataMap = new ConcurrentHashMap<>();
            if (userSessionData != null && userSessionData.getDataMap() != null) {
                dataMap.putAll(userSessionData.getDataMap());
            }

            final String appName = afterIdpGuestSignInRequest.getAppName();
            if (appName != null) {
                dataMap.put(APP_NAME_KEY, appName);
            }
            checkAuthorized(
                    afterIdpGuestSignInRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("guestUser", guestUser),
                    "guestSignIn");

            final UserSession userSession =
                    guestUserSession(guestUser, afterIdpGuestSignInRequest.getIdpGuestUserToken(), dataMap);
            userSessionStore.createSession(userSession);
            return new AfterIdpGuestSignInResponse(afterIdpGuestSignInRequest.getTraceId(), userSession);
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException("Error creating guest user session", e);
        } finally {
            context.stop();
        }
    }

    @Override
    public AfterIdpRefreshAuthenticationTokenResponse afterIdpRefreshAuthenticationToken(
            final AfterIdpRefreshAuthenticationTokenRequest afterIdpRefreshAuthenticationTokenRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpRefreshAuthenticationTokenRequest, "afterIdpRefreshAuthenticationTokenRequest");
        AppbaseUtil.validate(
                new AfterIdpRefreshAuthenticationTokenRequest.Validator(), afterIdpRefreshAuthenticationTokenRequest);
        verifyUserToken(afterIdpRefreshAuthenticationTokenRequest.getNewIdpUserToken());

        final Timer.Context context = refreshAuthenticationTokenTimer.time();
        try {
            final UserSession currentUserSession = afterIdpRefreshAuthenticationTokenRequest.getCurrentUserSession();
            checkAuthorized(
                    afterIdpRefreshAuthenticationTokenRequest.getAppbaseRequesterId(),
                    currentUserSession.getAppbaseSessionId(),
                    Map.of("currentUserSession", currentUserSession),
                    "refreshAuthenticationToken");

            final UserSession userSession = newUserSession(
                    currentUserSession,
                    afterIdpRefreshAuthenticationTokenRequest.getNewIdpUserToken());
            try {
                userSessionStore.updateSession(userSession);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Failed to update session: " + afterIdpRefreshAuthenticationTokenRequest, e);
            }

            return new AfterIdpRefreshAuthenticationTokenResponse(
                    afterIdpRefreshAuthenticationTokenRequest.getTraceId(), userSession);
        } finally {
            context.stop();
        }
    }

    @Override
    public AfterIdpPasswordResetSubmitResponse afterIdpPasswordResetSubmit(
            final AfterIdpPasswordResetSubmitRequest afterIdpPasswordResetSubmitRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpPasswordResetSubmitRequest, "afterIdpPasswordResetSubmitRequest");
        AppbaseUtil.validate(new AfterIdpPasswordResetSubmitRequest.Validator(), afterIdpPasswordResetSubmitRequest);

        final Timer.Context context = passwordResetTimer.time();
        try {
            final String appbaseUserId = afterIdpPasswordResetSubmitRequest.getAppbaseUserId();
            checkAuthorized(
                    afterIdpPasswordResetSubmitRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("appbaseUserId", appbaseUserId),
                    "passwordReset");

            final UpdateAccountRequest updateAccountRequest = new UpdateAccountRequest();
            updateAccountRequest.setReadAndUpdate(false);
            updateAccountRequest.setPasswordResetEmailSent(true);
            updateAccountRequest.setAppbaseUserId(afterIdpPasswordResetSubmitRequest.getAppbaseUserId());
            updateAccountRequest.setAppbaseRequesterId(afterIdpPasswordResetSubmitRequest.getAppbaseRequesterId());

            final UpdateAccountResponse updateAccountResponse = updateAccount(updateAccountRequest);
            return new AfterIdpPasswordResetSubmitResponse(
                    afterIdpPasswordResetSubmitRequest.getTraceId(), updateAccountResponse.isUpdated());
        } finally {
            context.stop();
        }
    }

    @Override
    public AfterIdpPasswordResetEmailSentResponse afterIdpPasswordResetEmailSent(
            AfterIdpPasswordResetEmailSentRequest afterIdpPasswordResetEmailSentRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpPasswordResetEmailSentRequest, "afterIdpPasswordResetEmailSentRequest");
        AppbaseUtil.validate(
                new AfterIdpPasswordResetEmailSentRequest.Validator(),
                afterIdpPasswordResetEmailSentRequest);

        final Timer.Context context = updatePasswordResetEmailSentTimer.time();
        try {
            final String email = afterIdpPasswordResetEmailSentRequest.getEmail();
            final IdpToken idpToken = verifyUserTokenRequired(
                    afterIdpPasswordResetEmailSentRequest.getIdpUserToken());
            if (!email.equalsIgnoreCase(idpToken.getEmail())) {
                throw new AuthenticationServiceException("IDP token email does not match reset callback");
            }
            final String appbaseRequesterId = idpToken.getUid();
            final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                    afterIdpPasswordResetEmailSentRequest.getTraceId(),
                    appbaseRequesterId,
                    email);
            checkAuthorized(
                    appbaseRequesterId,
                    null,
                    null,
                    "updatePasswordResetEmailSent");

            final ReadAccountByIdResponse readAccountByIdResponse = readAccountByEmail(readAccountByIdRequest);
            final UserAccount userAccount = readAccountByIdResponse.getUserAccount();
            if (userAccount == null) {
                throw new UserAccountServiceException.EmptyResultException(
                        "No such user exists: " + afterIdpPasswordResetEmailSentRequest.getEmail());
            }
            if (!idpToken.getUid().equals(userAccount.getIdpUserId())) {
                throw new AuthenticationServiceException("IDP token UID does not match reset account");
            }

            final UpdateAccountRequest updateAccountRequest = new UpdateAccountRequest();
            updateAccountRequest.setTraceId(afterIdpPasswordResetEmailSentRequest.getTraceId());
            updateAccountRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            updateAccountRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            updateAccountRequest.setReadAndUpdate(false);
            updateAccountRequest.setPasswordResetEmailSent(
                    afterIdpPasswordResetEmailSentRequest.getPasswordResetEmailSent());

            final UpdateAccountResponse updateAccountResponse = updateAccount(updateAccountRequest);
            return new AfterIdpPasswordResetEmailSentResponse(
                    updateAccountResponse.getTraceId(),
                    updateAccountResponse.isUpdated());
        } finally {
            context.stop();
        }
    }

    @Override
    public AfterIdpConfirmPasswordResetResponse afterIdpConfirmPasswordReset(
            final AfterIdpConfirmPasswordResetRequest afterIdpConfirmPasswordResetRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpConfirmPasswordResetRequest, "afterIdpConfirmPasswordResetRequest");
        AppbaseUtil.validate(new AfterIdpConfirmPasswordResetRequest.Validator(), afterIdpConfirmPasswordResetRequest);

        final Timer.Context context = confirmPasswordResetTimer.time();
        try {
            final String email = afterIdpConfirmPasswordResetRequest.getEmail();

            String appbaseRequesterId = afterIdpConfirmPasswordResetRequest.getAppbaseRequesterId();
            if (AppbaseUtil.isBlank(appbaseRequesterId)) {
                appbaseRequesterId = email;
            }
            checkAuthorized(
                    appbaseRequesterId,
                    null,
                    Map.of("email", email),
                    "passwordReset");

            final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                    afterIdpConfirmPasswordResetRequest.getTraceId(), appbaseRequesterId, email);
            final ReadAccountByIdResponse readAccountByIdResponse = readAccountByEmail(readAccountByIdRequest);

            if (readAccountByIdResponse == null) {
                throw new UserAccountServiceException(
                        "Failed to read account by email for request: " + afterIdpConfirmPasswordResetRequest);
            }

            final UserAccount userAccount = readAccountByIdResponse.getUserAccount();
            if (userAccount == null) {
                throw new UserAccountServiceException(
                        "No user account found for request: " + afterIdpConfirmPasswordResetRequest);
            }

            final PasswordHash serverPasswordHash = passwordService.hashWithRandomSalt(
                    afterIdpConfirmPasswordResetRequest.getNewClientPasswordHash().getHash());
            final PasswordHashes passwordHashes = PasswordHashes
                    .builder()
                    .clientPasswordHash(afterIdpConfirmPasswordResetRequest.getNewClientPasswordHash())
                    .serverPasswordHash(serverPasswordHash)
                    .build();
            final UserAccount updateUserAccount = new UserAccount(
                    userAccount,
                    false,
                    afterIdpConfirmPasswordResetRequest.getPasswordResetCodeHash(),
                    passwordHashes);


            // Update account store
            try {
                userAccountStore.updateUserAccount(updateUserAccount);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException(
                        "Error updating user account for request: " + afterIdpConfirmPasswordResetRequest +
                                ", userAccount=" + updateUserAccount, e);
            }

            return new AfterIdpConfirmPasswordResetResponse(
                    afterIdpConfirmPasswordResetRequest.getTraceId(), updateUserAccount);
        } finally {
            context.stop();
        }
    }

    @Override
    public AfterIdpSignOutResponse afterIdpSignOut(final AfterIdpSignOutRequest afterIdpSignOutRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(afterIdpSignOutRequest, "afterIdpSignOutRequest");
        AppbaseUtil.validate(new AfterIdpSignOutRequest.Validator(), afterIdpSignOutRequest);

        final Timer.Context context = signOutRequestTimer.time();
        try {
            final String appbaseUserId = afterIdpSignOutRequest.getAppbaseUserId();
            final String appbaseSessionId = afterIdpSignOutRequest.getAppbaseSessionId();
            checkAuthorized(
                    afterIdpSignOutRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "afterIdpSignOutRequest");

            final boolean deleteSession;
            try {
                deleteSession = userSessionStore.deleteSession(appbaseUserId, appbaseSessionId);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Failed to delete session: " + afterIdpSignOutRequest, e);
            }

            return new AfterIdpSignOutResponse(afterIdpSignOutRequest.getTraceId(), deleteSession);
        } finally {
            context.stop();
        }
    }

    @Override
    public CreateUnauthenticatedSessionResponse createUnauthenticatedSession(
            final CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest)
            throws UserAccountServiceException
    {
        AppbaseUtil.notNull(createUnauthenticatedSessionRequest, "createUnauthenticatedSessionRequest");
        AppbaseUtil.validate(new CreateUnauthenticatedSessionRequest.Validator(), createUnauthenticatedSessionRequest);

        final Timer.Context context = createUnauthenticatedSessionTimer.time();
        try {
            final UserSession userSession = new UserSession();
            userSession.setAuthenticated(false);

            final String appbaseSessionId = idService.new14CharsId();
            userSession.setAppbaseSessionId(appbaseSessionId);

            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            if (createUnauthenticatedSessionRequest.getCreationTimestamp() == null) {
                userSession.setCreationTimestamp(now);
            }
            if (createUnauthenticatedSessionRequest.getLastAccessTimestamp() == null) {
                userSession.setLastAccessTimestamp(now);
            }

            final UserSessionData userSessionData = createUnauthenticatedSessionRequest.getUserSessionData();
            if (userSessionData != null) {
                userSession.setUserSessionData(userSessionData);
            }

            userSessionStore.createSession(userSession);
            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    new CreateUnauthenticatedSessionResponse();
            createUnauthenticatedSessionResponse.setUserSession(userSession);
            return createUnauthenticatedSessionResponse;
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error creating unauthenticated session: " + createUnauthenticatedSessionRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ExistsAccountByIdResponse existsAccountByAppbaseUserId(
            final ExistsAccountByIdRequest existsAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(existsAccountByIdRequest, "existsAccountByIdRequest");
        AppbaseUtil.validate(new ExistsAccountByIdRequest.Validator(), existsAccountByIdRequest);

        final Timer.Context context = existsAccountByAppbaseUserIdTimer.time();
        try {
            final String appbaseUserId = existsAccountByIdRequest.getId();
            checkAuthorized(
                    existsAccountByIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("appbaseUserId", appbaseUserId),
                    "existsAccountByAppbaseUserId");
            final boolean exists = userAccountStore.existsByAppbaseUserId(appbaseUserId);
            return new ExistsAccountByIdResponse(existsAccountByIdRequest.getTraceId(), exists);
        } catch (UserAccountStoreException e) {
            throw new UserAccountServiceException.ExistsAccountByIdException(existsAccountByIdRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ExistsAccountByIdResponse existsAccountByIdpUserId(
            final ExistsAccountByIdRequest existsAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(existsAccountByIdRequest, "existsAccountByIdRequest");
        AppbaseUtil.validate(new ExistsAccountByIdRequest.Validator(), existsAccountByIdRequest);

        final Timer.Context context = existsAccountByIdpUserIdTimer.time();
        try {
            final String idpUserId = existsAccountByIdRequest.getId();
            checkAuthorized(
                    existsAccountByIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("idpUserId", idpUserId),
                    "existsAccountByIdpUserId");
            final boolean exists = userAccountStore.existsByIdpUserId(idpUserId);
            return new ExistsAccountByIdResponse(existsAccountByIdRequest.getTraceId(), exists);
        } catch (UserAccountStoreException e) {
            throw new UserAccountServiceException.ExistsAccountByIdException(existsAccountByIdRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ExistsAccountByIdResponse existsAccountByEmail(
            final ExistsAccountByIdRequest existsAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(existsAccountByIdRequest, "existsAccountByIdRequest");
        AppbaseUtil.validate(new ExistsAccountByIdRequest.Validator(), existsAccountByIdRequest);

        final Timer.Context context = existsAccountByEmailTimer.time();
        try {
            final String email = existsAccountByIdRequest.getId();
            checkAuthorized(
                    existsAccountByIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("email", email),
                    "existsAccountByEmail");
            final boolean exists = userAccountStore.existsByEmail(email);
            return new ExistsAccountByIdResponse(existsAccountByIdRequest.getTraceId(), exists);
        } catch (UserAccountStoreException e) {
            throw new UserAccountServiceException.ExistsAccountByIdException(existsAccountByIdRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadAccountByIdResponse readAccountByAppbaseUserId(
            final ReadAccountByIdRequest readAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(readAccountByIdRequest, "readAccountByIdRequest");
        AppbaseUtil.validate(new ReadAccountByIdRequest.Validator(), readAccountByIdRequest);

        final Timer.Context context = readAccountByAppbaseUserIdTimer.time();
        try {
            final String appbaseUserId = readAccountByIdRequest.getId();
            checkAuthorized(
                    readAccountByIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("appbaseUserId", appbaseUserId),
                    "readAccountByAppbaseUserId");

            final Optional<UserAccount> userAccountOptional = userAccountStore.readByAppbaseUserId(appbaseUserId);
            return new ReadAccountByIdResponse(readAccountByIdRequest.getTraceId(), userAccountOptional.orElse(null));
        } catch (UserAccountStoreException e) {
            throw new UserAccountServiceException.ReadAccountByIdException(readAccountByIdRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadAccountByIdResponse readAccountByIdpUserId(
            final ReadAccountByIdRequest readAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(readAccountByIdRequest, "readAccountByIdRequest");
        AppbaseUtil.validate(new ReadAccountByIdRequest.Validator(), readAccountByIdRequest);

        final Timer.Context context = readAccountByIdpUserIdTimer.time();
        try {
            final String idpUserId = readAccountByIdRequest.getId();
            checkAuthorized(
                    readAccountByIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("idpUserId", idpUserId),
                    "readAccountByIdpUserId");

            final Optional<UserAccount> userAccountOptional = userAccountStore.readByIdpUserId(idpUserId);
            return new ReadAccountByIdResponse(readAccountByIdRequest.getTraceId(), userAccountOptional.orElse(null));
        } catch (UserAccountStoreException e) {
            throw new UserAccountServiceException.ReadAccountByIdException(readAccountByIdRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadAccountByIdResponse readAccountByEmail(
            final ReadAccountByIdRequest readAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(readAccountByIdRequest, "readAccountByIdRequest");
        AppbaseUtil.validate(new ReadAccountByIdRequest.Validator(), readAccountByIdRequest);

        final Timer.Context context = readAccountByEmailTimer.time();
        try {
            final String email = readAccountByIdRequest.getId();
            checkAuthorized(
                    readAccountByIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("email", email),
                    "readAccountByEmail");

            final Optional<UserAccount> userAccountOptional = userAccountStore.readByEmail(email);
            return new ReadAccountByIdResponse(readAccountByIdRequest.getTraceId(), userAccountOptional.orElse(null));
        } catch (UserAccountStoreException e) {
            throw new UserAccountServiceException.ReadAccountByIdException(readAccountByIdRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadSessionResponse readSession(
            final ReadSessionRequest readSessionRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(readSessionRequest, "readSessionRequest");
        AppbaseUtil.validate(new ReadSessionRequest.Validator(), readSessionRequest);

        final Timer.Context context = readSessionTimer.time();
        try {
            final String appbaseUserId = readSessionRequest.getAppbaseUserId();
            final String appbaseSessionId = readSessionRequest.getAppbaseSessionId();
            checkAuthorized(
                    readSessionRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "readSession");

            final Optional<UserSession> userSession = userSessionStore.readSession(appbaseUserId, appbaseSessionId);
            return new ReadSessionResponse(readSessionRequest.getTraceId(), userSession.orElse(null));
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error reading session: " + readSessionRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadUnauthenticatedSessionResponse readUnauthenticatedSession(
            final ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest)
            throws UserAccountServiceException
    {
        AppbaseUtil.notNull(readUnauthenticatedSessionRequest, "readUnauthenticatedSessionRequest");
        AppbaseUtil.validate(new ReadUnauthenticatedSessionRequest.Validator(), readUnauthenticatedSessionRequest);

        final Timer.Context context = readUnauthenticatedSessionTimer.time();
        try {
            final Optional<UserSession> userSession = userSessionStore
                    .readUnauthenticatedSession(readUnauthenticatedSessionRequest.getAppbaseSessionId());
            return new ReadUnauthenticatedSessionResponse(
                    readUnauthenticatedSessionRequest.getTraceId(), userSession.orElse(null));
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error reading unauthenticated session: " + readUnauthenticatedSessionRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadGuestUserSessionResponse readGuestUserSession(
            final ReadGuestUserSessionRequest readGuestUserSessionRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(readGuestUserSessionRequest, "readGuestUserSessionRequest");
        AppbaseUtil.validate(new ReadGuestUserSessionRequest.Validator(), readGuestUserSessionRequest);

        final Timer.Context context = readGuestUserSessionTimer.time();
        try {
            final String appbaseUserId = readGuestUserSessionRequest.getAppbaseUserId();
            final String appbaseSessionId = readGuestUserSessionRequest.getAppbaseSessionId();
            checkAuthorized(
                    readGuestUserSessionRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "readGuestUserSession");

            final Optional<UserSession> userSessionOptional = userSessionStore.readSession(
                    appbaseUserId,
                    appbaseSessionId);
            if (userSessionOptional.isEmpty()) {
                throw new UserAccountServiceException.EmptyResultException(
                        "Cannot find user account: " + readGuestUserSessionRequest);
            }

            return new ReadGuestUserSessionResponse(
                    readGuestUserSessionRequest.getTraceId(), userSessionOptional.get());
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error reading guest user session: " + readGuestUserSessionRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public CheckTokenResponse checkToken(
            final CheckTokenRequest checkTokenRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(checkTokenRequest, "checkTokenRequest");
        AppbaseUtil.validate(new CheckTokenRequest.Validator(), checkTokenRequest);
        verifyUserToken(checkTokenRequest.getIdpUserToken());

        final Timer.Context context = checkTokenTimer.time();
        try {
            final String appbaseUserId = checkTokenRequest.getAppbaseUserId();
            final String appbaseSessionId = checkTokenRequest.getAppbaseSessionId();
            checkAuthorized(
                    checkTokenRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "checkToken");

            final Optional<UserSession> userSessionOptional;
            try {
                userSessionOptional = userSessionStore.readSession(appbaseUserId, appbaseSessionId);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Failed to read session: " + checkTokenRequest, e);
            }

            if (userSessionOptional.isEmpty()) {
                throw new UserAccountServiceException.EmptyResultException(
                        "No such session exists: " + checkTokenRequest);
            }

            final UserSession userSession = userSessionOptional.get();
            if (Objects.equals(userSession.getIdpUserToken(), checkTokenRequest.getIdpUserToken())) {
                return new CheckTokenResponse(checkTokenRequest.getTraceId(), userSession);
            }

            return new CheckTokenResponse(checkTokenRequest.getTraceId(), null);
        } finally {
            context.stop();
        }
    }

    @Override
    public IsExpiredSessionResponse isExpiredSession(
            final IsExpiredSessionRequest isExpiredSessionRequest)
            throws UserAccountServiceException
    {
        AppbaseUtil.notNull(isExpiredSessionRequest, "isExpiredSessionRequest");
        AppbaseUtil.validate(new IsExpiredSessionRequest.Validator(), isExpiredSessionRequest);

        final Timer.Context context = isExpiredSessionTimer.time();
        try {
            final boolean expired = userSessionStore.isExpired(isExpiredSessionRequest.getUserSession());
            return new IsExpiredSessionResponse(isExpiredSessionRequest.getTraceId(), expired);
        } finally {
            context.stop();
        }
    }

    @Override
    public RenewSessionResponse renewSession(
            final RenewSessionRequest renewSessionRequest)
            throws UserAccountServiceException
    {
        AppbaseUtil.notNull(renewSessionRequest, "renewSessionRequest");
        AppbaseUtil.validate(new RenewSessionRequest.Validator(), renewSessionRequest);

        final Timer.Context context = renewSessionTimer.time();
        try {
            final UserSession expiredUserSession = renewSessionRequest.getExpiredUserSession();
            final String oldAppbaseSessionId = expiredUserSession.getAppbaseSessionId();
            final String newAppbaseSessionId = idService.new14CharsId();
            expiredUserSession.setAppbaseSessionId(newAppbaseSessionId);

            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            expiredUserSession.setLastAccessTimestamp(now);

            try {
                userSessionStore.createOrUpdateSession(expiredUserSession);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Error creating renewed session: " + renewSessionRequest, e);
            }

            if (expiredUserSession.getUserAccount() == null && expiredUserSession.getGuestUser() == null) {
                try {
                    userSessionStore.deleteUnauthenticatedSession(oldAppbaseSessionId);
                } catch (UserSessionStoreException e) {
                    throw new UserAccountServiceException.SessionStoreException(
                            "Error deleting old session: " + oldAppbaseSessionId, e);
                }
            }

            return new RenewSessionResponse(renewSessionRequest.getTraceId(), expiredUserSession);
        } finally {
            context.stop();
        }
    }

    @Override
    public UpdateAccountResponse updateAccount(
            final UpdateAccountRequest updateAccountRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        return updateAccount(updateAccountRequest, false);
    }

    private UpdateAccountResponse updateAccount(
            final UpdateAccountRequest updateAccountRequest,
            final boolean clearPasswordResetCodeHash)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(updateAccountRequest, "updateAccountRequest");
        AppbaseUtil.validate(new UpdateAccountRequest.Validator(), updateAccountRequest);

        final Timer.Context context = updateAccountTimer.time();
        final Optional<UserAccount> userAccountOptional;
        try {
            final String appbaseUserId = updateAccountRequest.getAppbaseUserId();
            final String appbaseSessionId = updateAccountRequest.getAppbaseSessionId();
            checkAuthorized(
                    updateAccountRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "updateAccount");

            // Read account
            try {
                userAccountOptional = userAccountStore.readByAppbaseUserId(appbaseUserId);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException(
                        "Error reading user account during update: " + updateAccountRequest, e);
            }

            if (userAccountOptional.isEmpty()) {
                throw new UserAccountServiceException.EmptyResultException(
                        "No such user exists to update: " + updateAccountRequest);
            }
            final UserAccount updateUserAccount = setUpdateUserAccount(
                    userAccountOptional.get(), updateAccountRequest, clearPasswordResetCodeHash);


            // Retain the old session until the account write has succeeded.
            UserSession userSession = null;
            if (AppbaseUtil.isNotBlank(appbaseSessionId)) {
                try {
                    final Optional<UserSession> userSessionOptional = userSessionStore.readSession(
                            appbaseUserId,
                            appbaseSessionId);
                    if (userSessionOptional.isPresent()) {
                        userSession = userSessionOptional.get();
                    }
                } catch (UserSessionStoreException e) {
                    throw new UserAccountServiceException.SessionStoreException(
                            "Error reading session: " + appbaseSessionId, e);
                }
            }


            // Update account store
            final int updated;
            try {
                updated = userAccountStore.updateUserAccount(updateUserAccount);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException(
                        "Error updating user account: " + updateAccountRequest +
                                ", userAccount=" + updateUserAccount, e);
            }


            // Replace the existing session only after the account was persisted.
            if (updated > 0 && userSession != null) {
                userSession.setUserAccount(updateUserAccount);
                userSession.setLastAccessTimestamp(AppbaseServiceUtil.nowSeconds());
                try {
                    userSessionStore.updateSession(userSession);
                } catch (UserSessionStoreException e) {
                    // SQL has already committed. Best effort: prevent a stale embedded account
                    // snapshot from being used if Aerospike can still accept a delete.
                    try {
                        final boolean invalidated = userSessionStore.deleteSession(
                                appbaseUserId, appbaseSessionId);
                        throw new UserAccountServiceException.SessionStoreException(
                                "The account was updated, but session refresh failed; " +
                                        (invalidated ? "session invalidated" :
                                                "session invalidation could not be confirmed") +
                                        "; caller must reauthenticate", e);
                    } catch (UserSessionStoreException invalidationError) {
                        final UserAccountServiceException.SessionStoreException failure =
                                new UserAccountServiceException.SessionStoreException(
                                        "The account was updated, but session refresh and invalidation failed; " +
                                                "the old session may remain stale; caller must reauthenticate", e);
                        failure.addSuppressed(invalidationError);
                        throw failure;
                    }
                }
            }

            return new UpdateAccountResponse(updateAccountRequest.getTraceId(), updated > 0);
        } finally {
            context.stop();
        }
    }

    @Override
    public UpdateSessionResponse updateSession(
            final UpdateSessionRequest updateSessionRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(updateSessionRequest, "updateSessionRequest");
        AppbaseUtil.validate(new UpdateSessionRequest.Validator(), updateSessionRequest);
        if (!updateSessionRequest.isReadAndUpdate() && updateSessionRequest.getIdpUserToken() != null) {
            verifyUserToken(updateSessionRequest.getIdpUserToken());
        }

        final Timer.Context context = updateSessionTimer.time();
        try {
            final String appbaseUserId = updateSessionRequest.getAppbaseUserId();
            final String appbaseSessionId = updateSessionRequest.getAppbaseSessionId();
            checkAuthorized(
                    updateSessionRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "updateSession");

            final UserSession originalUserSession;
            try {
                final Optional<UserSession> userSessionOptional = userSessionStore
                        .readSession(appbaseUserId, appbaseSessionId);
                if (userSessionOptional.isEmpty()) {
                    throw new UserAccountServiceException.EmptyResultException(
                            "No such session exists: " + updateSessionRequest);
                }

                originalUserSession = userSessionOptional.get();
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Error reading session: " + updateSessionRequest, e);
            }

            final UserSession updateSession = new UserSession();
            updateSession.setAppbaseSessionId(originalUserSession.getAppbaseSessionId());
            updateSession.setGuestUser(originalUserSession.getGuestUser());
            updateSession.setUserAccount(originalUserSession.getUserAccount());
            updateSession.setCreationTimestamp(originalUserSession.getCreationTimestamp());
            updateSession.setAuthenticated(updateSessionRequest.isAuthenticated());

            if (updateSessionRequest.isReadAndUpdate()) {
                updateSession.setIdpUserToken(updateSessionRequest.getIdpUserToken());
                updateSession.setUserSessionData(updateSessionRequest.getUserSessionData());
                updateSession.setLastAccessTimestamp(updateSessionRequest.getLastAccessTimestamp());
            } else {
                if (AppbaseUtil.isNotNull(updateSessionRequest.getIdpUserToken())) {
                    updateSession.setIdpUserToken(updateSessionRequest.getIdpUserToken());
                } else {
                    updateSession.setIdpUserToken(originalUserSession.getIdpUserToken());
                }

                if (AppbaseUtil.isNotNull(updateSessionRequest.getUserSessionData())) {
                    updateSession.setUserSessionData(updateSessionRequest.getUserSessionData());
                } else {
                    updateSession.setUserSessionData(originalUserSession.getUserSessionData());
                }

                if (AppbaseUtil.isNotNull(updateSessionRequest.getLastAccessTimestamp())) {
                    updateSession.setLastAccessTimestamp(updateSessionRequest.getLastAccessTimestamp());
                } else {
                    updateSession.setLastAccessTimestamp(AppbaseServiceUtil.nowSeconds());
                }
            }

            try {
                userSessionStore.updateSession(updateSession);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Error updating session: " + updateSession, e);
            }

            return new UpdateSessionResponse(updateSessionRequest.getTraceId(), updateSession);
        } finally {
            context.stop();
        }
    }

    @Override
    public UpdateGuestSessionResponse updateGuestSession(
            final UpdateGuestSessionRequest updateGuestSessionRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(updateGuestSessionRequest, "updateGuestSessionRequest");
        AppbaseUtil.validate(new UpdateGuestSessionRequest.Validator(), updateGuestSessionRequest);
        if (!updateGuestSessionRequest.isReadAndUpdate() && updateGuestSessionRequest.getIdpUserToken() != null) {
            verifyUserToken(updateGuestSessionRequest.getIdpUserToken());
        }

        final Timer.Context context = updateGuestSessionTimer.time();
        try {
            final String appbaseUserId = updateGuestSessionRequest.getAppbaseUserId();
            final String appbaseSessionId = updateGuestSessionRequest.getAppbaseSessionId();
            checkAuthorized(
                    updateGuestSessionRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "updateGuestSession");

            final Optional<UserSession> userSessionOptional;
            try {
                userSessionOptional = userSessionStore.readSession(appbaseUserId, appbaseSessionId);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Error reading guest user session: " + updateGuestSessionRequest, e);
            }

            if (userSessionOptional.isEmpty()) {
                throw new UserAccountServiceException.EmptyResultException(
                        "No such guest user session exists: " + updateGuestSessionRequest);
            }

            final UserSession originalUserSession = userSessionOptional.get();

            final UserSession updateSession = new UserSession();
            updateSession.setAppbaseSessionId(originalUserSession.getAppbaseSessionId());
            updateSession.setAuthenticated(originalUserSession.isAuthenticated());
            updateSession.setUserAccount(originalUserSession.getUserAccount());
            updateSession.setCreationTimestamp(originalUserSession.getCreationTimestamp());

            final GuestUser originalGuestUser = originalUserSession.getGuestUser();
            final GuestUser updateGuestUser = new GuestUser();
            updateGuestUser.setIdpGuestId(originalGuestUser.getIdpGuestId());
            updateGuestUser.setAppbaseUserId(appbaseUserId);

            if (updateGuestSessionRequest.isReadAndUpdate()) {
                updateSession.setIdpUserToken(updateGuestSessionRequest.getIdpUserToken());
                updateSession.setLastAccessTimestamp(updateGuestSessionRequest.getLastAccessTimestamp());
                updateGuestUser.setLastAccessTimestamp(updateGuestSessionRequest.getLastAccessTimestamp());
                updateSession.setUserSessionData(updateGuestSessionRequest.getUserSessionData());
                updateGuestUser.setName(updateGuestSessionRequest.getName());
                updateGuestUser.setLanguageCode(updateGuestSessionRequest.getLanguageCode());
            } else {
                if (AppbaseUtil.isNotNull(updateGuestSessionRequest.getIdpUserToken())) {
                    updateSession.setIdpUserToken(updateGuestSessionRequest.getIdpUserToken());
                } else {
                    updateSession.setIdpUserToken(originalUserSession.getIdpUserToken());
                }

                if (AppbaseUtil.isNotNull(updateGuestSessionRequest.getLastAccessTimestamp())) {
                    updateSession.setLastAccessTimestamp(updateGuestSessionRequest.getLastAccessTimestamp());
                    updateGuestUser.setLastAccessTimestamp(updateGuestSessionRequest.getLastAccessTimestamp());
                } else {
                    final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
                    updateSession.setLastAccessTimestamp(now);
                    updateGuestUser.setLastAccessTimestamp(now);
                }

                if (AppbaseUtil.isNotNull(updateGuestSessionRequest.getUserSessionData())) {
                    updateSession.setUserSessionData(updateGuestSessionRequest.getUserSessionData());
                } else {
                    updateSession.setUserSessionData(originalUserSession.getUserSessionData());
                }

                if (AppbaseUtil.isNotNull(updateGuestSessionRequest.getName())) {
                    updateGuestUser.setName(updateGuestSessionRequest.getName());
                } else {
                    updateGuestUser.setName(originalGuestUser.getName());
                }

                if (AppbaseUtil.isNotNull(updateGuestSessionRequest.getLanguageCode())) {
                    updateGuestUser.setLanguageCode(updateGuestSessionRequest.getLanguageCode());
                } else {
                    updateGuestUser.setLanguageCode(originalGuestUser.getLanguageCode());
                }
            }

            updateSession.setGuestUser(updateGuestUser);

            try {
                userSessionStore.updateSession(updateSession);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Error updating session: " + updateSession, e);
            }

            return new UpdateGuestSessionResponse(updateGuestSessionRequest.getTraceId(), updateSession);
        } finally {
            context.stop();
        }
    }

    @Override
    public UpdateUnauthenticatedSessionResponse updateUnauthenticatedSession(
            final UpdateUnauthenticatedSessionRequest updateUnauthenticatedSessionRequest)
            throws UserAccountServiceException
    {
        AppbaseUtil.notNull(updateUnauthenticatedSessionRequest, "updateUnauthenticatedSessionRequest");
        AppbaseUtil.validate(new UpdateUnauthenticatedSessionRequest.Validator(), updateUnauthenticatedSessionRequest);

        final String appbaseSessionId = updateUnauthenticatedSessionRequest.getAppbaseSessionId();

        final Timer.Context context = updateUnauthenticatedSessionTimer.time();
        try {
            final Optional<UserSession> userSessionOptional;
            try {
                userSessionOptional = userSessionStore.readSession(appbaseSessionId, appbaseSessionId);
            } catch (UserSessionStoreException e) {
                throw new UserAccountServiceException.SessionStoreException(
                        "Error reading unauthenticated user session: " + updateUnauthenticatedSessionRequest, e);
            }

            if (userSessionOptional.isEmpty()) {
                throw new UserAccountServiceException.EmptyResultException(
                        "No such session exists: " + updateUnauthenticatedSessionRequest);
            }

            final UserSession userSession = userSessionOptional.get();
            if (updateUnauthenticatedSessionRequest.isReadAndUpdate()) {
                userSession.setLastAccessTimestamp(updateUnauthenticatedSessionRequest.getLastAccessTimestamp());
                userSession.setUserSessionData(updateUnauthenticatedSessionRequest.getUserSessionData());
            } else {
                if (AppbaseUtil.isNotNull(updateUnauthenticatedSessionRequest.getLastAccessTimestamp())) {
                    userSession.setLastAccessTimestamp(updateUnauthenticatedSessionRequest.getLastAccessTimestamp());
                } else {
                    userSession.setLastAccessTimestamp(AppbaseServiceUtil.nowSeconds());
                }

                if (AppbaseUtil.isNotNull(updateUnauthenticatedSessionRequest.getUserSessionData())) {
                    userSession.setUserSessionData(updateUnauthenticatedSessionRequest.getUserSessionData());
                } else {
                    userSession.setUserSessionData(userSession.getUserSessionData());
                }
            }

            userSessionStore.updateSession(userSession);
            return new UpdateUnauthenticatedSessionResponse(
                    updateUnauthenticatedSessionRequest.getTraceId(), userSession);
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error updating unauthenticated session: " + updateUnauthenticatedSessionRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public DeleteSessionResponse deleteSession(
            final DeleteSessionRequest deleteSessionRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(deleteSessionRequest, "deleteSessionRequest");
        AppbaseUtil.validate(new DeleteSessionRequest.Validator(), deleteSessionRequest);

        final Timer.Context context = deleteSessionTimer.time();
        try {
            final String appbaseUserId = deleteSessionRequest.getAppbaseUserId();
            final String appbaseSessionId = deleteSessionRequest.getAppbaseSessionId();
            checkAuthorized(
                    deleteSessionRequest.getAppbaseRequesterId(),
                    appbaseSessionId,
                    Map.of("appbaseUserId", appbaseUserId),
                    "deleteSession");

            final boolean deleted = userSessionStore.deleteSession(appbaseUserId, appbaseSessionId);
            return new DeleteSessionResponse(deleteSessionRequest.getTraceId(), deleted);
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error deleting session: " + deleteSessionRequest , e);
        } finally {
            context.stop();
        }
    }

    @Override
    public DeleteAccountByAppbaseUserIdResponse deleteAccountByAppbaseUserId(
            final DeleteAccountByAppbaseUserIdRequest deleteAccountByAppbaseUserIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(deleteAccountByAppbaseUserIdRequest, "deleteAccountByAppbaseUserIdRequest");
        AppbaseUtil.validate(new DeleteAccountByAppbaseUserIdRequest.Validator(), deleteAccountByAppbaseUserIdRequest);

        final Timer.Context context = deleteAccountByAppbaseUserIdTimer.time();
        try {
            final String appbaseUserId = deleteAccountByAppbaseUserIdRequest.getAppbaseUserId();
            checkAuthorized(
                    deleteAccountByAppbaseUserIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("appbaseUserId", appbaseUserId),
                    "deleteAccountByAppbaseUserId");

            final Optional<UserAccount> userAccountOptional;
            try {
                userAccountOptional = userAccountStore.readByAppbaseUserId(appbaseUserId);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException("Failed to read user: " + appbaseUserId, e);
            }

            if (userAccountOptional.isEmpty()) {
                throw new UserAccountServiceException.EmptyResultException("No such user exists: " + appbaseUserId);
            }

            final boolean deleted;
            try {
                deleted = userAccountStore.deleteByAppbaseUserId(appbaseUserId);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException(
                        "Failed to delete user: " + appbaseUserId, e);
            }

            if (deleted) {
                try {
                    userSessionStore.deleteAllSessions(appbaseUserId);
                } catch (UserSessionStoreException e) {
                    throw new UserAccountServiceException.SessionStoreException(
                            "Failed to delete session for: " + appbaseUserId, e);
                }
            }

            return new DeleteAccountByAppbaseUserIdResponse(deleteAccountByAppbaseUserIdRequest.getTraceId(), deleted);
        } finally {
            context.stop();
        }
    }

    @Override
    public DeleteAccountByIdpUserIdResponse deleteAccountByIdpUserId(
            final DeleteAccountByIdpUserIdRequest deleteAccountByIdpUserIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(deleteAccountByIdpUserIdRequest, "deleteAccountByIdpUserIdRequest");
        AppbaseUtil.validate(new DeleteAccountByIdpUserIdRequest.Validator(), deleteAccountByIdpUserIdRequest);

        final Timer.Context context = deleteAccountByIdpUserIdTimer.time();
        try {
            final String idpUserId = deleteAccountByIdpUserIdRequest.getIdpUserId();
            checkAuthorized(
                    deleteAccountByIdpUserIdRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("idpUserId", idpUserId),
                    "deleteAccountByIdpUserId");

            final Optional<UserAccount> userAccountOptional;
            try {
                userAccountOptional = userAccountStore.readByIdpUserId(idpUserId);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException("Failed to read user: " + idpUserId, e);
            }

            if (userAccountOptional.isEmpty()) {
                throw new UserAccountServiceException.EmptyResultException("No such user exists: " + idpUserId);
            }

            final UserAccount userAccount = userAccountOptional.get();
            final boolean deleted;
            try {
                deleted = userAccountStore.deleteByIdpUserId(idpUserId);
            } catch (UserAccountStoreException e) {
                throw new UserAccountServiceException.AccountStoreException("Failed to delete user: " + idpUserId, e);
            }

            if (deleted) {
                try {
                    userSessionStore.deleteAllSessions(userAccount.getAppbaseUserId());
                } catch (UserSessionStoreException e) {
                    throw new UserAccountServiceException.SessionStoreException(
                            "Failed to delete session for: " + idpUserId, e);
                }
            }

            return new DeleteAccountByIdpUserIdResponse(deleteAccountByIdpUserIdRequest.getTraceId(), deleted);
        } finally {
            context.stop();
        }
    }

    @Override
    public DeleteGuestUserResponse deleteGuestUser(
            final DeleteGuestUserRequest deleteGuestUserRequest)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(deleteGuestUserRequest, "deleteGuestUserRequest");
        AppbaseUtil.validate(new DeleteGuestUserRequest.Validator(), deleteGuestUserRequest);

        final Timer.Context context = deleteGuestUserTimer.time();
        try {
            final String appbaseUserId = deleteGuestUserRequest.getAppbaseUserId();
            checkAuthorized(
                    deleteGuestUserRequest.getAppbaseRequesterId(),
                    null,
                    Map.of("appbaseUserId", appbaseUserId),
                    "deleteGuestUser");

            final boolean deleted = userSessionStore.deleteAllSessions(appbaseUserId);
            return new DeleteGuestUserResponse(deleteGuestUserRequest.getTraceId(), deleted);
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error deleting all session: " + deleteGuestUserRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public DeleteUnauthenticatedSessionResponse deleteUnauthenticatedSession(
            final DeleteUnauthenticatedSessionRequest deleteUnauthenticatedSessionRequest)
            throws UserAccountServiceException
    {
        AppbaseUtil.notNull(deleteUnauthenticatedSessionRequest, "deleteUnauthenticatedSessionRequest");
        AppbaseUtil.validate(new DeleteUnauthenticatedSessionRequest.Validator(), deleteUnauthenticatedSessionRequest);

        final Timer.Context context = deleteUnauthenticatedSessionTimer.time();
        try {
            final boolean deleted = userSessionStore
                    .deleteAllSessions(deleteUnauthenticatedSessionRequest.getAppbaseSessionId());
            return new DeleteUnauthenticatedSessionResponse(deleteUnauthenticatedSessionRequest.getTraceId(), deleted);
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error deleting unauthenticated session: " + deleteUnauthenticatedSessionRequest, e);
        } finally {
            context.stop();
        }
    }

    @Override
    public SyncIdpUserResponse syncIdpUser(
            final SyncIdpUserRequest syncIdpUserRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException
    {
        AppbaseUtil.notNull(syncIdpUserRequest, "syncIdpUserRequest");
        AppbaseUtil.validate(validationService, syncIdpUserRequest);

        final Timer.Context context = syncIdpUserTimer.time();

        try {
            final IdpToken idpToken = idpTokenVerifier.verifyUserToken(syncIdpUserRequest.getIdpUserToken());
            if (!syncIdpUserRequest.getEmail().equalsIgnoreCase(idpToken.getEmail())) {
                throw new UserAccountServiceException("IDP token verification failed: Email does not match");
            }

            if (!syncIdpUserRequest.getIdpUserId().equals(idpToken.getUid())) {
                throw new UserAccountServiceException("IDP token verification failed: UID does not match");
            }

            final AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest =
                    new AfterIdpCreateUserWithEmailPasswordRequest();
            afterIdpCreateUserWithEmailPasswordRequest.setEmail(syncIdpUserRequest.getEmail());
            afterIdpCreateUserWithEmailPasswordRequest.setClientPasswordHash(
                    syncIdpUserRequest.getClientPasswordHash());
            afterIdpCreateUserWithEmailPasswordRequest.setIdpUserId(syncIdpUserRequest.getIdpUserId());
            afterIdpCreateUserWithEmailPasswordRequest.setIdpUserToken(syncIdpUserRequest.getIdpUserToken());
            afterIdpCreateUserWithEmailPasswordRequest.setAppName(syncIdpUserRequest.getAppName());
            afterIdpCreateUserWithEmailPasswordRequest.setAppbaseRequesterId(
                    syncIdpUserRequest.getAppbaseRequesterId());
            afterIdpCreateUserWithEmailPasswordRequest.setLanguageCode(syncIdpUserRequest.getLanguageCode());
            afterIdpCreateUserWithEmailPasswordRequest.setName(idpToken.getName());
            afterIdpCreateUserWithEmailPasswordRequest.setAddressLine1(syncIdpUserRequest.getAddressLine1());
            afterIdpCreateUserWithEmailPasswordRequest.setAddressLine2(syncIdpUserRequest.getAddressLine2());
            afterIdpCreateUserWithEmailPasswordRequest.setCity(syncIdpUserRequest.getCity());
            afterIdpCreateUserWithEmailPasswordRequest.setStateProvince(syncIdpUserRequest.getStateProvince());
            afterIdpCreateUserWithEmailPasswordRequest.setCountry(syncIdpUserRequest.getCountry());
            afterIdpCreateUserWithEmailPasswordRequest.setPostalCode(syncIdpUserRequest.getPostalCode());
            afterIdpCreateUserWithEmailPasswordRequest.setLatitude(syncIdpUserRequest.getLatitude());
            afterIdpCreateUserWithEmailPasswordRequest.setLongitude(syncIdpUserRequest.getLongitude());
            afterIdpCreateUserWithEmailPasswordRequest.setUserSessionData(syncIdpUserRequest.getUserSessionData());
            afterIdpCreateUserWithEmailPasswordRequest.setTraceId(syncIdpUserRequest.getTraceId());

            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);

            return new SyncIdpUserResponse(
                    afterIdpCreateUserWithEmailPasswordResponse.getTraceId(),
                    afterIdpCreateUserWithEmailPasswordResponse.getUserSession());
        } finally {
            context.stop();
        }
    }

    private void checkAuthorized(
            @Nullable
            final String appbaseRequesterId,
            @Nullable
            final String appbaseSessionId,
            @Nullable
            final Map<String, Object> parameters,
            final String operation)
            throws AuthorizationServiceException
    {
        final AuthorizationContext authorizationContext = new AuthorizationContext();
        if (!AppbaseUtil.isBlank(appbaseRequesterId)) {
            authorizationContext.setAppbaseRequesterId(appbaseRequesterId);
        }

        if (!AppbaseUtil.isBlank(appbaseSessionId)) {
            authorizationContext.setAppbaseSessionId(appbaseSessionId);
        }

        if (parameters != null) {
            authorizationContext.setParameters(parameters);
        }

        authorizationService.checkAuthorized(getClass(), operation, authorizationContext);
    }

    private UserSession storeNewUserSession(
            final UserAccount userAccount,
            final String idpUserToken,
            final String appName,
            final UserSessionData userSessionData)
            throws UserAccountServiceException
    {
        final UserSession userSession = userSession(userAccount, idpUserToken, appName, userSessionData);
        try {
            userSessionStore.createSession(userSession);
        } catch (UserSessionStoreException e) {
            throw new UserAccountServiceException.SessionStoreException(
                    "Error creating user session: " + userSession, e);
        }

        return userSession;
    }

    private UserSession newUserSession(final UserSession userSession, final String newIdpUserToken) {
        AppbaseUtil.notNull(userSession, "userSession");
        AppbaseUtil.notBlank(newIdpUserToken, "newIdpUserToken");

        return UserSession
                .builder()
                .appbaseSessionId(userSession.getAppbaseSessionId())
                .authenticated(userSession.isAuthenticated())
                .idpUserToken(newIdpUserToken)
                .userAccount(userSession.getUserAccount())
                .userSessionData(userSession.getUserSessionData())
                .creationTimestamp(userSession.getCreationTimestamp())
                .lastAccessTimestamp(userSession.getLastAccessTimestamp())
                .expired(userSession.isExpired())
                .build();
    }

    private UserSession guestUserSession(
            final GuestUser guestUser,
            @Nullable
            final String idpUserToken,
            @Nullable
            final Map<String, String> dataMap)
    {
        final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
        final String sessionId = userSessionStore.generateSessionId();
        final UserSessionData userSessionData = new UserSessionData(dataMap);
        return UserSession
                .builder()
                .appbaseSessionId(sessionId)
                .authenticated(false)
                .idpUserToken(idpUserToken)
                .userAccount(null)
                .guestUser(guestUser)
                .userSessionData(userSessionData)
                .creationTimestamp(now)
                .lastAccessTimestamp(now)
                .expired(false)
                .build();
    }

    private UserSession userSession(
            final UserAccount userAccount,
            final String idpUserToken,
            final String appName,
            final UserSessionData userSessionData)
    {
        final String sessionId = userSessionStore.generateSessionId();

        final UserSessionData finalUserSessionData =
                Objects.requireNonNullElseGet(userSessionData, UserSessionData::new);

        final Map<String, String> dataMap;
        if (finalUserSessionData.getDataMap() == null) {
            dataMap = new ConcurrentHashMap<>();
            dataMap.put(APP_NAME_KEY, appName);
        } else {
            dataMap = finalUserSessionData.getDataMap();
        }
        finalUserSessionData.setDataMap(dataMap);

        return UserSession
                .builder()
                .appbaseSessionId(sessionId)
                .authenticated(true)
                .idpUserToken(idpUserToken)
                .userAccount(userAccount)
                .userSessionData(finalUserSessionData)
                .creationTimestamp(userAccount.getCreationTimestamp())
                .lastAccessTimestamp(userAccount.getLastAccessTimestamp())
                .expired(false)
                .newSession(false)
                .build();
    }

    private UserAccount setUpdateUserAccount(
            final UserAccount originalUserAccount,
            final UpdateAccountRequest updateAccountRequest,
            final boolean clearPasswordResetCodeHash)
    {
        final UserAccount updateUserAccount = new UserAccount();
        updateUserAccount.setAppbaseUserId(originalUserAccount.getAppbaseUserId());
        updateUserAccount.setIdpUserId(originalUserAccount.getIdpUserId());
        updateUserAccount.setCreationTimestamp(originalUserAccount.getCreationTimestamp());

        final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
        if (updateAccountRequest.isReadAndUpdate()) {
            final PasswordHashes passwordHashes;
            final PasswordHash clientPasswordHash = updateAccountRequest.getClientPasswordHash();
            if (!originalUserAccount.getPasswordHashes().getClientPasswordHash().equals(clientPasswordHash)){
                final PasswordHash serverPasswordHash =
                        passwordService.hashWithRandomSalt(clientPasswordHash.getHash());
                passwordHashes = PasswordHashes
                        .builder()
                        .clientPasswordHash(clientPasswordHash)
                        .serverPasswordHash(serverPasswordHash)
                        .build();
            } else {
                passwordHashes = originalUserAccount.getPasswordHashes();
            }

            updateUserAccount.setEmail(updateAccountRequest.getEmail());
            updateUserAccount.setName(updateUserAccount.getName());
            updateUserAccount.setPasswordHashes(passwordHashes);
            updateUserAccount.setPasswordResetEmailSent(updateAccountRequest.getPasswordResetEmailSent());
            updateUserAccount.setPasswordResetCodeHash(updateAccountRequest.getPasswordResetCodeHash());
            updateUserAccount.setEmailVerified(updateAccountRequest.getEmailVerified());
            updateUserAccount.setLanguageCode(updateAccountRequest.getLanguageCode());
            updateUserAccount.setLastAccessTimestamp(updateAccountRequest.getLastAccessTimestamp());
            updateUserAccount.setDeleted(updateAccountRequest.getDeleted());
            updateUserAccount.setUserTopic(updateAccountRequest.getUserTopic());
            updateUserAccount.setUserHomeDataCenter(updateAccountRequest.getUserHomeDataCenter());
            updateUserAccount.setAddressLine1(updateAccountRequest.getAddressLine1());
            updateUserAccount.setAddressLine2(updateAccountRequest.getAddressLine2());
            updateUserAccount.setCity(updateAccountRequest.getCity());
            updateUserAccount.setStateProvince(updateAccountRequest.getStateProvince());
            updateUserAccount.setCountry(updateAccountRequest.getCountry());
            updateUserAccount.setPostalCode(updateAccountRequest.getPostalCode());
            updateUserAccount.setLatitude(updateAccountRequest.getLatitude());
            updateUserAccount.setLongitude(updateAccountRequest.getLongitude());
        } else {
            if (AppbaseUtil.isNotNull(updateAccountRequest.getEmail())) {
                updateUserAccount.setEmail(updateAccountRequest.getEmail());
            } else {
                updateUserAccount.setEmail(originalUserAccount.getEmail());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getName())) {
                updateUserAccount.setName(updateAccountRequest.getName());
            } else {
                updateUserAccount.setName(originalUserAccount.getName());
            }

            if (clearPasswordResetCodeHash) {
                updateUserAccount.setPasswordResetCodeHash(null);
            } else if (AppbaseUtil.isNotNull(updateAccountRequest.getPasswordResetCodeHash())) {
                updateUserAccount.setPasswordResetCodeHash(updateAccountRequest.getPasswordResetCodeHash());
            } else {
                updateUserAccount.setPasswordResetCodeHash(originalUserAccount.getPasswordResetCodeHash());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getClientPasswordHash())) {
                final PasswordHash serverPasswordHash = passwordService.hashWithRandomSalt(
                        updateAccountRequest.getClientPasswordHash().getHash());
                final PasswordHashes passwordHashes = PasswordHashes
                        .builder()
                        .clientPasswordHash(updateAccountRequest.getClientPasswordHash())
                        .serverPasswordHash(serverPasswordHash)
                        .build();
                updateUserAccount.setPasswordHashes(passwordHashes);
            } else {
                updateUserAccount.setPasswordHashes(originalUserAccount.getPasswordHashes());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getPasswordResetEmailSent())) {
                updateUserAccount.setPasswordResetEmailSent(updateAccountRequest.getPasswordResetEmailSent());
            } else {
                updateUserAccount.setPasswordResetEmailSent(originalUserAccount.getPasswordResetEmailSent());

            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getEmailVerified())) {
                updateUserAccount.setEmailVerified(updateAccountRequest.getEmailVerified());
            } else {
                updateUserAccount.setEmailVerified(originalUserAccount.getEmailVerified());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getLanguageCode())) {
                updateUserAccount.setLanguageCode(updateAccountRequest.getLanguageCode());
            } else {
                updateUserAccount.setLanguageCode(originalUserAccount.getLanguageCode());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getLastAccessTimestamp())) {
                updateUserAccount.setLastAccessTimestamp(updateAccountRequest.getLastAccessTimestamp());
            } else {
                updateUserAccount.setLastAccessTimestamp(now);
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getDeleted())) {
                updateUserAccount.setDeleted(updateAccountRequest.getDeleted());
            } else {
                updateUserAccount.setDeleted(originalUserAccount.getDeleted());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getUserTopic())) {
                updateUserAccount.setUserTopic(updateAccountRequest.getUserTopic());
            } else {
                updateUserAccount.setUserTopic(originalUserAccount.getUserTopic());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getUserHomeDataCenter())) {
                updateUserAccount.setUserHomeDataCenter(updateAccountRequest.getUserHomeDataCenter());
            } else {
                updateUserAccount.setUserHomeDataCenter(originalUserAccount.getUserHomeDataCenter());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getAddressLine1())) {
                updateUserAccount.setAddressLine1(updateAccountRequest.getAddressLine1());
            } else {
                updateUserAccount.setAddressLine1(originalUserAccount.getAddressLine1());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getAddressLine2())) {
                updateUserAccount.setAddressLine2(updateAccountRequest.getAddressLine2());
            } else {
                updateUserAccount.setAddressLine2(originalUserAccount.getAddressLine2());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getCity())) {
                updateUserAccount.setCity(updateAccountRequest.getCity());
            } else {
                updateUserAccount.setCity(originalUserAccount.getCity());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getCountry())) {
                updateUserAccount.setCountry(updateAccountRequest.getCountry());
            } else {
                updateUserAccount.setCountry(originalUserAccount.getCountry());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getStateProvince())) {
                updateUserAccount.setStateProvince(updateAccountRequest.getStateProvince());
            } else {
                updateUserAccount.setStateProvince(originalUserAccount.getStateProvince());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getPostalCode())) {
                updateUserAccount.setPostalCode(updateAccountRequest.getPostalCode());
            } else {
                updateUserAccount.setPostalCode(originalUserAccount.getPostalCode());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getLatitude())) {
                updateUserAccount.setLatitude(updateAccountRequest.getLatitude());
            } else {
                updateUserAccount.setLatitude(originalUserAccount.getLatitude());
            }

            if (AppbaseUtil.isNotNull(updateAccountRequest.getLongitude())) {
                updateUserAccount.setLongitude(updateAccountRequest.getLongitude());
            } else {
                updateUserAccount.setLongitude(originalUserAccount.getLongitude());
            }
        }

        return updateUserAccount;
    }

    private void verifyUserIdentity(
            final String idpUserToken,
            final String idpUserId,
            final String email)
            throws AuthenticationServiceException
    {
        final IdpToken idpToken = verifyUserTokenRequired(idpUserToken);
        if (idpToken == null || !idpUserId.equals(idpToken.getUid()) ||
                !email.equalsIgnoreCase(idpToken.getEmail()))
        {
            throw new AuthenticationServiceException("IDP token identity does not match request");
        }
    }

    private IdpToken verifyUserTokenRequired(final String idpUserToken) throws AuthenticationServiceException {
        if (!idpTokenVerifier.isUserTokenVerificationEnabled()) {
            throw new AuthenticationServiceException("IDP token verification is required for account flows");
        }
        final IdpToken idpToken = idpTokenVerifier.verifyUserToken(idpUserToken);
        if (idpToken == null) {
            throw new AuthenticationServiceException("IDP token verification returned no identity");
        }
        return idpToken;
    }

    private void verifyUserToken(final String idpUserToken) throws AuthenticationServiceException {
        if (idpTokenVerifier.isUserTokenVerificationEnabled()) {
            idpTokenVerifier.verifyUserToken(idpUserToken);
        }
    }
}
