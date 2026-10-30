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

package com.viveworks.appbase.stacktest.test.service.user.account.testimpl;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInResponse;
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
import com.viveworks.appbase.service.user.account.data.GuestUser;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionRequest;
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
import com.viveworks.appbase.service.user.account.data.UpdateAccountRequest;
import com.viveworks.appbase.service.user.account.data.UpdateAccountResponse;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.web.endpoint.jaxrs.UserAccountJaxrsResource;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpCreateUserWithEmailPasswordServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpEmailPasswordSignInServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpGuestSignInServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpPasswordResetSubmitServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpRefreshAuthenticationTokenServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpSignOutServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.CheckTokenServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.CreateUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteAccountByAppbaseUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteAccountByIdpUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteGuestUserServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ExistsAccountByAppbaseUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ExistsAccountByEmailServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ExistsAccountByIdpUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.IsExpiredSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadAccountByAppbaseUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadAccountByEmailServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadAccountByIdpUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadGuestUserSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.RenewSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateAccountServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateGuestUserSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import com.viveworks.appbase.stacktest.test.service.user.account.FirebaseAuthEmulatorIdentity;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.service.user.account.AbstractUserAccountApiTest;
import com.viveworks.appbase.stacktest.test.service.user.account.jaxrs.UserAccountJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.AfterIdpConfirmPasswordResetServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.AfterIdpCreateUserWithEmailPasswordServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.AfterIdpEmailPasswordSignInServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.AfterIdpGuestSignInServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.AfterIdpPasswordResetSubmitServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.AfterIdpRefreshAuthenticationTokenServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.AfterIdpSignOutServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.CheckTokenServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.CreateUnauthenticatedSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.DeleteAccountByAppbaseUserIdServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.DeleteAccountByIdpUserIdServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.DeleteGuestUserServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.DeleteSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.DeleteUnauthenticatedSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ExistsAccountByAppbaseUserIdServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ExistsAccountByEmailServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ExistsAccountByIdpUserIdServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.IsExpiredSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ReadAccountByAppbaseUserIdServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ReadAccountByEmailServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ReadAccountByIdpUserIdServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ReadGuestUserSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ReadSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.ReadUnauthenticatedSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.RenewSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.UpdateAccountServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.UpdateGuestSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.UpdateSessionServletTest;
import com.viveworks.appbase.stacktest.test.service.user.account.servlet.UpdateUnauthenticatedSessionServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserAccountApiTestImpl extends AbstractUserAccountApiTest {
    private final Class<?> testType;

    public UserAccountApiTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        this.testType = testType;
        checkTestType(
                testType,
                UserAccountJaxrsResourceTest.class,
                AfterIdpCreateUserWithEmailPasswordServletTest.class,
                AfterIdpEmailPasswordSignInServletTest.class,
                AfterIdpGuestSignInServletTest.class,
                AfterIdpPasswordResetSubmitServletTest.class,
                AfterIdpConfirmPasswordResetServletTest.class,
                AfterIdpRefreshAuthenticationTokenServletTest.class,
                AfterIdpSignOutServletTest.class,
                CheckTokenServletTest.class,
                CreateUnauthenticatedSessionServletTest.class,
                DeleteAccountByAppbaseUserIdServletTest.class,
                DeleteAccountByIdpUserIdServletTest.class,
                DeleteGuestUserServletTest.class,
                DeleteSessionServletTest.class,
                DeleteUnauthenticatedSessionServletTest.class,
                ExistsAccountByAppbaseUserIdServletTest.class,
                ExistsAccountByEmailServletTest.class,
                ExistsAccountByIdpUserIdServletTest.class,
                IsExpiredSessionServletTest.class,
                ReadAccountByAppbaseUserIdServletTest.class,
                ReadAccountByEmailServletTest.class,
                ReadAccountByIdpUserIdServletTest.class,
                ReadGuestUserSessionServletTest.class,
                ReadSessionServletTest.class,
                ReadUnauthenticatedSessionServletTest.class,
                RenewSessionServletTest.class,
                UpdateAccountServletTest.class,
                UpdateGuestSessionServletTest.class,
                UpdateSessionServletTest.class,
                UpdateUnauthenticatedSessionServletTest.class);
    }

    public void afterIdpCreateUserWithEmailPasswordTestImpl() throws Exception {
        final String testName = "afterIdpCreateUserWithEmailPasswordTestImpl";

        startTest(testType, testName);

        final AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest =
                createUserWithEmailPasswordRequest();

        final String url;
        if (isRestApiTest()) {
            url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                    UserAccountJaxrsResource.AFTER_IDP_CREATE_USER_WITH_EMAIL_PASSWORD_PATH;
        } else {
            url = baseUrl + AfterIdpCreateUserWithEmailPasswordServlet.DEFAULT_PATH;
        }

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    httpPost(url,
                            afterIdpCreateUserWithEmailPasswordRequest,
                            AfterIdpCreateUserWithEmailPasswordRequest.class,
                            AfterIdpCreateUserWithEmailPasswordResponse.class);
            checkNotNull(
                    afterIdpCreateUserWithEmailPasswordResponse,
                    testName,
                    "afterIdpCreateUserWithEmailPasswordResponse");
            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");
            checkNotNull(userSession.getAppbaseSessionId(), testName, "userSession.getAppbaseSessionId()");
            checkNotNull(userSession.getCreationTimestamp(), testName, "userSession.getCreationTimestamp()");
            checkNotNull(userSession.getLastAccessTimestamp(), testName, "userSession.getLastAccessTimestamp()");
            checkNotNull(userSession.getUserSessionData(), testName, "userSession.getUserSessionData()");

            final String idpUserToken = userSession.getIdpUserToken();
            checkNotNull(idpUserToken, testName, "idpUserToken");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getIdpUserToken(),
                    idpUserToken,
                    testName,
                    "idpUserToken");
            checkNull(userSession.getGuestUser(), testName, "userSession.getGuestUser()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getName(),
                    userAccount.getName(),
                    testName,
                    "userAccount.getName()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getEmail(),
                    userAccount.getEmail(),
                    testName,
                    "userAccount.getEmail()");
            checkNull(userAccount.getPasswordHashes(), testName, "userAccount.getPasswordHashes()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getLanguageCode(),
                    userAccount.getLanguageCode(),
                    testName,
                    "userAccount.getLanguageCode()");

            appbaseUserId = userAccount.getAppbaseUserId();
            checkNotNull(userAccount.getAppbaseUserId(), testName, "userAccount.getAppbaseUserId()");
            checkEquals(
                    appbaseUserId,
                    userAccount.getUserTopic(),
                    testName,
                    "userAccount.getAppbaseUserId()");
            checkNotNull(userAccount.getPasswordResetEmailSent(), testName, "userAccount.getPasswordReset()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getIdpUserId(),
                    userAccount.getIdpUserId(),
                    testName,
                    "userAccount.getIdpUserId()");
            checkNotNull(userAccount.getEmailVerified(), testName, "userAccount.getEmailVerified()");
            checkFalse(userAccount.getEmailVerified(), testName, "userAccount.getEmailVerified()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getAddressLine1(),
                    userAccount.getAddressLine1(),
                    testName,
                    "userAccount.getAddressLine1()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getAddressLine2(),
                    userAccount.getAddressLine2(),
                    testName,
                    "userAccount.getAddressLine2()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getCity(),
                    userAccount.getCity(),
                    testName,
                    "userAccount.getCity()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getStateProvince(),
                    userAccount.getStateProvince(),
                    testName,
                    "userAccount.getStateProvince()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getCountry(),
                    userAccount.getCountry(),
                    testName,
                    "userAccount.getCountry()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getPostalCode(),
                    userAccount.getPostalCode(),
                    testName,
                    "userAccount.getPostalCode()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getLatitude(),
                    userAccount.getLatitude(),
                    testName,
                    "userAccount.getLatitude()");
            checkEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getLongitude(),
                    userAccount.getLongitude(),
                    testName,
                    "userAccount.getLongitude()");
            checkNotNull(userAccount.getUserHomeDataCenter(), testName, "userAccount.getUserHomeDataCenter()");
            checkNotNull(userAccount.getDeleted(), testName, "userAccount.getDeleted()");
            checkFalse(userAccount.getDeleted(), testName, "userAccount.getDeleted()");
            checkNotNull(userAccount.getCreationTimestamp(), testName, "userAccount.getCreationTimestamp()");
            checkNotNull(userAccount.getLastAccessTimestamp(), testName, "userAccount.getLastAccessTimestamp()");
        } finally {
            if (appbaseUserId != null) {
                deleteAccountByAppbaseUserId(appbaseUserId);
            }
        }

        endTest(testType, testName);
    }

    public void afterIdpEmailPasswordSignInTestImpl() throws Exception {
        final String testName = "afterIdpEmailPasswordSignInTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final AfterIdpEmailPasswordSignInRequest afterIdpEmailPasswordSignInRequest =
                    new AfterIdpEmailPasswordSignInRequest();
            afterIdpEmailPasswordSignInRequest.setAppName("appName-" + UUID.randomUUID().toString());
            afterIdpEmailPasswordSignInRequest.setEmail(userAccount.getEmail());
            afterIdpEmailPasswordSignInRequest.setClientPasswordHash(
                    userAccount.getPasswordHashes().getClientPasswordHash());
            afterIdpEmailPasswordSignInRequest.setIdpUserId(userAccount.getIdpUserId());
            afterIdpEmailPasswordSignInRequest.setIdpUserToken(userSession.getIdpUserToken());
            afterIdpEmailPasswordSignInRequest.setLanguageCode(userAccount.getLanguageCode());
            afterIdpEmailPasswordSignInRequest.setAppbaseRequesterId(userAccount.getIdpUserId());

            final UserSessionData userSessionData = new UserSessionData();
            userSessionData.setDataMap(Map.of("key1", "value1"));
            afterIdpEmailPasswordSignInRequest.setUserSessionData(userSessionData);

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.AFTER_IDP_EMAIL_PASSWORD_SIGN_IN_PATH;
            } else {
                url = baseUrl + AfterIdpEmailPasswordSignInServlet.DEFAULT_PATH;
            }

            final AfterIdpEmailPasswordSignInResponse afterIdpEmailPasswordSignInResponse = httpPost(url,
                    afterIdpEmailPasswordSignInRequest,
                    AfterIdpEmailPasswordSignInRequest.class,
                    AfterIdpEmailPasswordSignInResponse.class);
            checkNotNull(afterIdpEmailPasswordSignInResponse, testName, "afterIdpEmailPasswordSignInResponse");

            final UserSession userSession1 = afterIdpEmailPasswordSignInResponse.getUserSession();
            checkNotNull(userSession1, testName, "userSession1");

            final UserAccount userAccount1 = userSession1.getUserAccount();
            checkNotNull(userAccount1, testName, "userAccount1");

            checkNotEquals(
                    userSession1.getAppbaseSessionId(),
                    userSession.getAppbaseSessionId(),
                    testName,
                    "userSession1.getAppbaseSessionId()");
            checkEquals(
                    userAccount1.getIdpUserId(),
                    userAccount.getIdpUserId(),
                    testName,
                    "userAccount1.getIdpUserId()");
        } finally {
            if (appbaseUserId != null) {
                deleteAccountByAppbaseUserId(appbaseUserId);
            }
        }

        endTest(testType, testName);
    }

    public void afterIdpGuestSignInTestImpl() throws Exception {
        final String testName = "afterIdpGuestSignInTestImpl";

        startTest(testType, testName);

        AfterIdpGuestSignInResponse afterIdpGuestSignInResponse = null;

        final String url;
        if (isRestApiTest()) {
            url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                    UserAccountJaxrsResource.AFTER_IDP_GUEST_SIGN_IN_PATH;
        } else {
            url = baseUrl + AfterIdpGuestSignInServlet.DEFAULT_PATH;
        }

        try {
            final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest = guestSignInRequest();

            afterIdpGuestSignInResponse = httpPost(
                    url,
                    afterIdpGuestSignInRequest,
                    AfterIdpGuestSignInRequest.class,
                    AfterIdpGuestSignInResponse.class);
            checkNotNull(afterIdpGuestSignInResponse, testName, "afterIdpGuestSignInResponse");

            final UserSession userSession = afterIdpGuestSignInResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final GuestUser guestUser = userSession.getGuestUser();
            checkNotNull(guestUser, testName, "guestUser");
            checkNull(userSession.getUserAccount(), testName, "userSession.getUserAccount()");
            checkEquals(
                    afterIdpGuestSignInRequest.getIdpGuestUserToken(),
                    userSession.getIdpUserToken(),
                    testName,
                    "userSession.getIdpUserToken()");

            checkEquals(
                    afterIdpGuestSignInRequest.getIdpGuestId(),
                    guestUser.getIdpGuestId(),
                    testName,
                    "idpGuestId");
            checkEquals(
                    afterIdpGuestSignInRequest.getName(),
                    guestUser.getName(),
                    testName,
                    "name");
            checkEquals(
                    afterIdpGuestSignInRequest.getLanguageCode(),
                    guestUser.getLanguageCode(),
                    testName,
                    "languageCode");
        } finally {
            if (afterIdpGuestSignInResponse != null) {
                deleteGuestUser(afterIdpGuestSignInResponse);
            }
        }

        endTest(testType, testName);
    }

    public void afterIdpPasswordResetSubmitTestImpl() throws Exception {
        final String testName = "afterIdpPasswordResetSubmitTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.AFTER_IDP_PASSWORD_RESET_SUBMIT_PATH;
            } else {
                url = baseUrl + AfterIdpPasswordResetSubmitServlet.DEFAULT_PATH;
            }

            final AfterIdpPasswordResetSubmitRequest afterIdpPasswordResetSubmitRequest =
                    new AfterIdpPasswordResetSubmitRequest();
            final String id = UUID.randomUUID().toString();
            afterIdpPasswordResetSubmitRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            afterIdpPasswordResetSubmitRequest.setAppbaseRequesterId(id);
            afterIdpPasswordResetSubmitRequest.setTraceId(id);

            final AfterIdpPasswordResetSubmitResponse afterIdpPasswordResetSubmitResponse =
                    httpPost(url,
                            afterIdpPasswordResetSubmitRequest,
                            AfterIdpPasswordResetSubmitRequest.class,
                            AfterIdpPasswordResetSubmitResponse.class);
            checkNotNull(afterIdpPasswordResetSubmitResponse, testName, "afterIdpPasswordResetSubmitResponse");
            checkTrue(
                    afterIdpPasswordResetSubmitResponse.isUpdated(),
                    testName,
                    "afterIdpPasswordResetSubmitResponse.isUpdated()");

            final ReadAccountByIdResponse readAccountByIdResponse =
                    readAccountByAppbaseUserId(userAccount.getAppbaseUserId());
            checkNotNull(readAccountByIdResponse, testName, "readAccountByIdResponse");

            final UserAccount userAccount1 = readAccountByIdResponse.getUserAccount();
            checkNotNull(userAccount1, testName, "userAccount1");
            checkTrue(userAccount1.getPasswordResetEmailSent(), testName, "userAccount1.getPasswordReset()");
        } finally {
            if (appbaseUserId != null) {
                deleteAccountByAppbaseUserId(appbaseUserId);
            }
        }

        endTest(testType, testName);
    }

    public void afterIdpConfirmPasswordResetTestImpl() throws Exception {
        final String testName = "afterIdpConfirmPasswordResetTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();
            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();
            checkNotNull(appbaseUserId, testName, "appbaseUserId");

            final String email = userAccount.getEmail();
            checkNotNull(email, testName, "email");

            final IdService idService = appbaseServices.get(IdService.class);
            final AfterIdpConfirmPasswordResetRequest afterIdpConfirmPasswordResetRequest =
                    new AfterIdpConfirmPasswordResetRequest();
            afterIdpConfirmPasswordResetRequest.setEmail(email);
            afterIdpConfirmPasswordResetRequest.setPasswordResetCodeHash(
                    PasswordHash
                            .builder()
                            .hash("passwordResetCodeHash")
                            .hashAlgorithm(HashAlgorithm.SHA512)
                            .shaRounds(80)
                            .salt("salt123")
                            .build());
            afterIdpConfirmPasswordResetRequest.setAppbaseRequesterId(email);
            afterIdpConfirmPasswordResetRequest.setNewClientPasswordHash(
                    PasswordHash
                            .builder()
                            .hash("newPassword-" + idService.newId())
                            .hashAlgorithm(HashAlgorithm.SHA512)
                            .shaRounds(80)
                            .salt("salt123")
                            .build());
            afterIdpConfirmPasswordResetRequest.setTraceId(idService.newId());

            final UserAccountService userAccountService = appbaseServices.get(UserAccountService.class);
            final AfterIdpConfirmPasswordResetResponse afterIdpConfirmPasswordResetResponse = userAccountService
                    .afterIdpConfirmPasswordReset(afterIdpConfirmPasswordResetRequest);
            checkNotNull(afterIdpConfirmPasswordResetResponse, testName, "afterIdpConfirmPasswordResetResponse");

            final UserAccount responseUserAccount = afterIdpConfirmPasswordResetResponse.getUserAccount();
            checkNotNull(responseUserAccount, testName, "responseUserAccount");

            final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                    idService.newId(), userAccount.getAppbaseUserId(), userAccount.getAppbaseUserId());
            final ReadAccountByIdResponse readAccountByIdResponse =
                    userAccountService.readAccountByAppbaseUserId(readAccountByIdRequest);
            checkNotNull(readAccountByIdResponse, testName, "readAccountByIdResponse");

            final UserAccount readUserAccount = readAccountByIdResponse.getUserAccount();
            checkNotNull(readUserAccount, testName, "readUserAccount");

            final PasswordHash passwordResetCodeHash = readUserAccount.getPasswordResetCodeHash();
            checkNotNull(passwordResetCodeHash, testName, "passwordResetCodeHash");

            checkEquals(
                    afterIdpConfirmPasswordResetRequest.getPasswordResetCodeHash(),
                    passwordResetCodeHash,
                    testName,
                    "passwordResetCode");
            checkEquals(
                    afterIdpConfirmPasswordResetRequest.getNewClientPasswordHash(),
                    readUserAccount.getPasswordHashes().getClientPasswordHash(),
                    testName,
                    "password");
        } finally {
            if (appbaseUserId != null) {
                deleteAccountByAppbaseUserId(appbaseUserId);
            }
        }

        endTest(testType, testName);
    }

    public void afterIdpRefreshAuthenticationTokenTestImpl() throws Exception {
        final String testName = "afterIdpRefreshAuthenticationTokenTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String newId = FirebaseAuthEmulatorIdentity.refreshedEmailToken(userAccount.getIdpUserId());
            checkNotEquals(newId, userSession.getIdpUserToken(), "afterIdpRefreshAuthenticationTokenTestImpl", "newId");

            final AfterIdpRefreshAuthenticationTokenRequest afterIdpRefreshAuthenticationTokenRequest =
                    new AfterIdpRefreshAuthenticationTokenRequest();
            afterIdpRefreshAuthenticationTokenRequest.setCurrentUserSession(userSession);
            afterIdpRefreshAuthenticationTokenRequest.setNewIdpUserToken(newId);
            afterIdpRefreshAuthenticationTokenRequest.setAppbaseRequesterId(newId);

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.AFTER_IDP_REFRESH_AUTHENTICATION_TOKEN_PATH;
            } else {
                url = baseUrl + AfterIdpRefreshAuthenticationTokenServlet.DEFAULT_PATH;
            }

            final AfterIdpRefreshAuthenticationTokenResponse afterIdpRefreshAuthenticationTokenResponse =
                    httpPost(url,
                            afterIdpRefreshAuthenticationTokenRequest,
                            AfterIdpRefreshAuthenticationTokenRequest.class,
                            AfterIdpRefreshAuthenticationTokenResponse.class);
            checkNotNull(afterIdpRefreshAuthenticationTokenResponse,
                    "afterIdpRefreshAuthenticationTokenTestImpl",
                    "afterIdpRefreshAuthenticationTokenResponse");

            final UserSession userSession1 = afterIdpRefreshAuthenticationTokenResponse.getUserSession();
            checkNotNull(userSession1, "afterIdpRefreshAuthenticationTokenTestImpl", "userSession1");
            checkEquals(
                    newId,
                    userSession1.getIdpUserToken(),
                    "afterIdpRefreshAuthenticationTokenTestImpl",
                    "userSession1.getIdpUserToken()");
        } finally {
            if (appbaseUserId != null) {
                deleteAccountByAppbaseUserId(appbaseUserId);
            }
        }

        endTest(testType, testName);
    }

    public void afterIdpSignOutTestImpl() throws Exception {
        final String testName = "afterIdpSignOutTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final AfterIdpSignOutRequest afterIdpSignOutRequest1 = new AfterIdpSignOutRequest();
            afterIdpSignOutRequest1.setAppbaseUserId(userAccount.getAppbaseUserId());
            afterIdpSignOutRequest1.setAppbaseSessionId(userSession.getAppbaseSessionId());
            afterIdpSignOutRequest1.setIdpUserToken(userSession.getIdpUserToken());
            afterIdpSignOutRequest1.setIdpUserId(userAccount.getIdpUserId());
            afterIdpSignOutRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.AFTER_IDP_SIGN_OUT_PATH;
            } else {
                url = baseUrl + AfterIdpSignOutServlet.DEFAULT_PATH;
            }

            final AfterIdpSignOutResponse afterIdpSignOutResponse1 =
                    httpPost(url, afterIdpSignOutRequest1, AfterIdpSignOutRequest.class, AfterIdpSignOutResponse.class);
            checkNotNull(afterIdpSignOutResponse1, testName, "afterIdpSignOutResponse1");
            checkTrue(afterIdpSignOutResponse1.isSignedOut(), testName, "afterIdpSignOutResponse1.isSignedOut()");

            final AfterIdpSignOutRequest afterIdpSignOutRequest2 = new AfterIdpSignOutRequest();
            afterIdpSignOutRequest2.setAppbaseUserId(userAccount.getAppbaseUserId());
            afterIdpSignOutRequest2.setAppbaseSessionId(UUID.randomUUID().toString());
            afterIdpSignOutRequest2.setIdpUserToken(userSession.getIdpUserToken());
            afterIdpSignOutRequest2.setIdpUserId(userAccount.getIdpUserId());
            afterIdpSignOutRequest2.setAppbaseRequesterId(userAccount.getAppbaseUserId());

            final AfterIdpSignOutResponse afterIdpSignOutResponse2 =
                    httpPost(url, afterIdpSignOutRequest2, AfterIdpSignOutRequest.class, AfterIdpSignOutResponse.class);
            checkNotNull(afterIdpSignOutResponse2, testName, "afterIdpSignOutResponse1");
            checkFalse(afterIdpSignOutResponse2.isSignedOut(), testName, "afterIdpSignOutResponse1.isSignedOut()");
        } finally {
            if (appbaseUserId != null) {
                deleteAccountByAppbaseUserId(appbaseUserId);
            }
        }

        endTest(testType, testName);
    }

    public void checkTokenTestImpl() throws Exception {
        final String testName = "checkTokenTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final CheckTokenRequest checkTokenRequest1 = new CheckTokenRequest();
            checkTokenRequest1.setAppbaseUserId(userAccount.getAppbaseUserId());
            checkTokenRequest1.setAppbaseSessionId(userSession.getAppbaseSessionId());
            checkTokenRequest1.setIdpUserToken(userSession.getIdpUserToken());
            checkTokenRequest1.setIdpUserId(userAccount.getIdpUserId());
            checkTokenRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.CHECK_TOKEN_PATH;
            } else {
                url = baseUrl + CheckTokenServlet.DEFAULT_PATH;
            }

            final CheckTokenResponse checkTokenResponse1 =
                    httpPost(url, checkTokenRequest1, CheckTokenRequest.class, CheckTokenResponse.class);
            checkNotNull(checkTokenResponse1, testName, "checkTokenResponse1");
            final UserSession userSession1 = checkTokenResponse1.getUserSession();
            checkUserSession(userSession, userSession1, testName, "userSession");

            final CheckTokenRequest checkTokenRequest2 = new CheckTokenRequest();
            checkTokenRequest2.setAppbaseUserId(userAccount.getAppbaseUserId());
            checkTokenRequest2.setAppbaseSessionId(checkTokenRequest1.getAppbaseSessionId());
            checkTokenRequest2.setIdpUserToken(FirebaseAuthEmulatorIdentity.refreshedEmailToken(
                    userAccount.getIdpUserId()));
            checkTokenRequest2.setIdpUserId(userAccount.getIdpUserId());
            checkTokenRequest2.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            final CheckTokenResponse checkTokenResponse2 =
                    httpPost(url, checkTokenRequest2, CheckTokenRequest.class, CheckTokenResponse.class);
            checkNotNull(checkTokenResponse2, testName, "checkTokenResponse2");
            checkNull(checkTokenResponse2.getUserSession(), testName, "checkTokenResponse2.getUserSession()");
        } finally {
            if (appbaseUserId != null) {
                deleteAccountByAppbaseUserId(appbaseUserId);
            }
        }

        endTest(testType, testName);
    }

    public void createUnauthenticatedSessionTestImpl() throws Exception {
        final String testName = "createUnauthenticatedSessionTestImpl";

        startTest(testType, testName);

        UserSession userSession = null;
        try {
            final CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest =
                    createUnauthenticatedSessionRequest();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.UNAUTHENTICATED_SESSION_PATH;
            } else {
                url = baseUrl + CreateUnauthenticatedSessionServlet.DEFAULT_PATH;
            }

            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    httpPost(url,
                            createUnauthenticatedSessionRequest,
                            CreateUnauthenticatedSessionRequest.class,
                            CreateUnauthenticatedSessionResponse.class);
            checkNotNull(createUnauthenticatedSessionResponse, testName, "createUnauthenticatedSessionResponse");

            userSession = createUnauthenticatedSessionResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");
        } finally {
            if (userSession != null) {
                deleteUnauthenticatedSession(userSession);
            }
        }

        endTest(testType, testName);
    }

    public void deleteAccountByAppbaseUserIdTestImpl() throws Exception {
        final String testName = "deleteAccountByAppbaseUserIdTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_APPBASE_USER_ID_PATH;
            } else {
                url = baseUrl + DeleteAccountByAppbaseUserIdServlet.DEFAULT_PATH;
            }

            httpDelete(url + "?appbaseUserId=" + appbaseUserId + "&appbaseRequesterId=" + appbaseUserId);

            final ReadAccountByIdResponse readAccountByIdResponse = readAccountByAppbaseUserId(appbaseUserId);
            checkNotNull(readAccountByIdResponse, testName, "readAccountByIdResponse");

            final UserAccount userAccount1 = readAccountByIdResponse.getUserAccount();
            checkNull(userAccount1, testName, "userAccount1");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void deleteAccountByIdpUserIdTestImpl() throws Exception {
        final String testName = "deleteAccountByIdpUserIdTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            final String idpUserId = userAccount.getIdpUserId();
            checkNotNull(idpUserId, testName, "idpUserId");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String id = UUID.randomUUID().toString();
            final ReadAccountByIdResponse readAccountByIdResponse1 = appbaseServices
                    .get(UserAccountService.class)
                    .readAccountByIdpUserId(new ReadAccountByIdRequest(id, id, idpUserId));
            checkNotNull(readAccountByIdResponse1, testName, "readAccountByIdResponse1");
            final UserAccount readUserAccount1 = readAccountByIdResponse1.getUserAccount();
            checkNotNull(readUserAccount1, testName, "readUserAccount1");
            checkUserAccount(userAccount, readUserAccount1, testName, "userAccount");

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_IDP_USER_ID_PATH;
            } else {
                url = baseUrl + DeleteAccountByIdpUserIdServlet.DEFAULT_PATH;
            }

            httpDelete(url + "?idpUserId=" + idpUserId + "&appbaseRequesterId=" + idpUserId);

            final ReadAccountByIdResponse readAccountByIdResponse2 = appbaseServices
                    .get(UserAccountService.class)
                    .readAccountByAppbaseUserId(
                            new ReadAccountByIdRequest(id, id, idpUserId));
            checkNotNull(readAccountByIdResponse2, testName, "readAccountByIdResponse2");
            final UserAccount readUserAccount2 = readAccountByIdResponse2.getUserAccount();
            checkNull(readUserAccount2, testName, "readAccountByIdResponse2");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void deleteGuestUserTestImpl() throws Exception {
        final String testName = "deleteGuestUserTestImpl";

        startTest(testType, testName);

        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest1 = guestSignInRequest();
        AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1 = null;
        try {
            afterIdpGuestSignInResponse1 = appbaseServices
                    .get(UserAccountService.class)
                    .afterIdpGuestSignIn(afterIdpGuestSignInRequest1);

            final UserSession userSession1 = afterIdpGuestSignInResponse1.getUserSession();
            checkNotNull(userSession1, testName, "userSession1");

            final GuestUser guestUser = userSession1.getGuestUser();
            checkNotNull(guestUser, testName, "guestUser");
            final String appbaseUserId = guestUser.getAppbaseUserId();
            checkNotNull(appbaseUserId, testName, "appbaseUserId");

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.GUEST_USER_PATH;
            } else {
                url = baseUrl + DeleteGuestUserServlet.DEFAULT_PATH;
            }

            httpDelete(url + "?appbaseUserId=" + appbaseUserId + "&appbaseRequesterId=" + appbaseUserId);

            final ReadGuestUserSessionRequest guestUserSessionRequest = new ReadGuestUserSessionRequest();
            guestUserSessionRequest.setAppbaseUserId(appbaseUserId);
            guestUserSessionRequest.setAppbaseSessionId(userSession1.getAppbaseSessionId());
            guestUserSessionRequest.setAppbaseRequesterId(appbaseUserId);

            try {
                final ReadGuestUserSessionResponse readGuestUserSessionResponse = appbaseServices
                        .get(UserAccountService.class)
                        .readGuestUserSession(guestUserSessionRequest);
                checkNotNull(readGuestUserSessionResponse, testName, "readGuestUserSessionResponse");
                checkNull(
                        readGuestUserSessionResponse.getUserSession(),
                        testName,
                        "readGuestUserSessionResponse.getUserSession()");
            } catch (UserAccountServiceException.EmptyResultException e) {
                // ignore, already deleted
            }
        } finally {
            if (afterIdpGuestSignInResponse1 != null) {
                try {
                    deleteGuestUser(afterIdpGuestSignInResponse1);
                } catch (Exception ex) {
                    // ignore, already deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void deleteSessionTestImpl() throws Exception {
        final String testName = "deleteSessionTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final ReadSessionRequest readSessionRequest1 = new ReadSessionRequest();
            readSessionRequest1.setAppbaseSessionId(userSession.getAppbaseSessionId());
            readSessionRequest1.setAppbaseUserId(userAccount.getAppbaseUserId());
            readSessionRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            final ReadSessionResponse readSessionResponse1 =
                    appbaseServices.get(UserAccountService.class).readSession(readSessionRequest1);
            checkNotNull(readSessionResponse1, testName, "readSessionResponse1");
            final UserSession readUserSession1 = readSessionResponse1.getUserSession();
            checkUserSession(userSession, readUserSession1, testName, "userSession");

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.SESSION_PATH;
            } else {
                url = baseUrl + DeleteSessionServlet.DEFAULT_PATH;
            }

            httpDelete(url +
                    "?appbaseUserId=" + appbaseUserId +
                    "&appbaseSessionId=" + userSession.getAppbaseSessionId() +
                    "&appbaseRequesterId=" + appbaseUserId);

            try {
                final ReadSessionResponse readSessionResponse2 =
                        appbaseServices.get(UserAccountService.class).readSession(readSessionRequest1);
                checkNotNull(readSessionResponse2, testName, "readSessionResponse2");
                checkNull(readSessionResponse2.getUserSession(), testName, "readSessionResponse2.getUserSession()");
            }  catch (UserAccountServiceException.EmptyResultException e) {
                // ignore, it is deleted
            }
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void deleteUnauthenticatedSessionTestImpl() throws Exception {
        final String testName = "deleteUnauthenticatedSessionTestImpl";

        startTest(testType, testName);

        UserSession userSession = null;
        try {
            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    createUnauthenticatedSession();
            checkNotNull(createUnauthenticatedSessionResponse, testName, "createUnauthenticatedSessionResponse");

            userSession = createUnauthenticatedSessionResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");
            checkNotNull(userSession.getAppbaseSessionId(), testName, "userSession.getAppbaseSessionId()");

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.UNAUTHENTICATED_SESSION_PATH;
            } else {
                url = baseUrl + DeleteUnauthenticatedSessionServlet.DEFAULT_PATH;
            }

            httpDelete(url + "?appbaseSessionId=" + userSession.getAppbaseSessionId());

            final ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest =
                    new ReadUnauthenticatedSessionRequest();
            readUnauthenticatedSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            final ReadUnauthenticatedSessionResponse readUnauthenticatedSessionResponse = appbaseServices
                    .get(UserAccountService.class)
                    .readUnauthenticatedSession(readUnauthenticatedSessionRequest);
            checkNotNull(readUnauthenticatedSessionResponse, testName, "readUnauthenticatedSessionResponse");
            final UserSession readUserSession = readUnauthenticatedSessionResponse.getUserSession();
            checkNull(readUserSession, testName, "readUserSession");
        } finally {
            if (userSession != null) {
                try {
                    deleteUnauthenticatedSession(userSession);
                } catch (Exception ex) {
                    // ignore, already deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void existsAccountByAppbaseUserIdTestImpl() throws Exception {
        final String testName = "existsAccountByAppbaseUserIdTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_APPBASE_USER_ID_PATH;
            } else {
                url = baseUrl + ExistsAccountByAppbaseUserIdServlet.DEFAULT_PATH;
            }

            final int code = httpHead(url + "?appbaseUserId=" + appbaseUserId + "&appbaseRequesterId=" + appbaseUserId);
            checkEquals(HttpServletResponse.SC_NO_CONTENT,code,  testName, "code");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void existsAccountByEmailTestImpl() throws Exception {
        final String testName = "existsAccountByEmailTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_EMAIL_PATH;
            } else {
                url = baseUrl + ExistsAccountByEmailServlet.DEFAULT_PATH;
            }

            final int code = httpHead(url +
                    "?email=" + userAccount.getEmail() +
                    "&appbaseRequesterId=" + userAccount.getEmail());
            checkEquals(HttpServletResponse.SC_NO_CONTENT, code, testName, "code");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void existsAccountByIdpUserIdTestImpl() throws Exception {
        final String testName = "existsAccountByIdpUserIdTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_IDP_USER_ID_PATH;
            } else {
                url = baseUrl + ExistsAccountByIdpUserIdServlet.DEFAULT_PATH;
            }

            final int code = httpHead(url +
                    "?idpUserId=" + userAccount.getIdpUserId() +
                    "&appbaseRequesterId=" + userAccount.getIdpUserId());
            checkEquals(HttpServletResponse.SC_NO_CONTENT,code,  testName, "code");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void isExpiredSessionTestImpl() throws Exception {
        final String testName = "isExpiredSessionTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.IS_EXPIRED_SESSION_PATH;
            } else {
                url = baseUrl + IsExpiredSessionServlet.DEFAULT_PATH;
            }

            final IsExpiredSessionRequest isExpiredSessionRequest = new IsExpiredSessionRequest();
            isExpiredSessionRequest.setUserSession(userSession);

            final int code = httpCodeOfPut(url, IsExpiredSessionRequest.class, isExpiredSessionRequest);
            checkEquals(HttpServletResponse.SC_NO_CONTENT,code,  testName, "code");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void readAccountByAppbaseUserIdTestImpl() throws Exception {
        final String testName = "readAccountByAppbaseUserIdTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_APPBASE_USER_ID_PATH;
            } else {
                url = baseUrl + ReadAccountByAppbaseUserIdServlet.DEFAULT_PATH;
            }

            final ReadAccountByIdResponse readAccountByIdResponse = httpGetJson(
                    url + "?appbaseUserId=" + appbaseUserId + "&appbaseRequesterId=" + appbaseUserId,
                    ReadAccountByIdResponse.class);
            checkNotNull(readAccountByIdResponse, testName, "readAccountByIdResponse");
            final UserAccount readUserAccount = readAccountByIdResponse.getUserAccount();
            checkNotNull(readUserAccount, testName, "readUserAccount");
            checkUserAccount(userAccount, readUserAccount, testName, "readUserAccount");
            checkEquals(false, readUserAccount.getPasswordResetEmailSent(), testName, "readUserAccount.getPasswordReset()");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void readAccountByEmailTestImpl() throws Exception {
        final String testName = "readAccountByEmailTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_EMAIL_PATH;
            } else {
                url = baseUrl + ReadAccountByEmailServlet.DEFAULT_PATH;
            }

            final ReadAccountByIdResponse readAccountByIdResponse = httpGetJson(
                    url + "?email=" + userAccount.getEmail() + "&appbaseRequesterId=" + userAccount.getEmail(),
                    ReadAccountByIdResponse.class);
            checkNotNull(readAccountByIdResponse, testName, "readAccountByIdResponse");
            final UserAccount readUserAccount = readAccountByIdResponse.getUserAccount();
            checkNotNull(readUserAccount, testName, "readUserAccount");
            checkUserAccount(userAccount, readUserAccount, testName, "readUserAccount");
            checkEquals(false, readUserAccount.getPasswordResetEmailSent(), testName, "readUserAccount.getPasswordReset()");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void readAccountByIdpUserIdTestImpl() throws Exception {
        final String testName = "readAccountByIdpUserIdTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_BY_IDP_USER_ID_PATH;
            } else {
                url = baseUrl + ReadAccountByIdpUserIdServlet.DEFAULT_PATH;
            }

            final ReadAccountByIdResponse readAccountByIdResponse = httpGetJson(
                    url + "?idpUserId=" + userAccount.getIdpUserId() +
                            "&appbaseRequesterId=" + userAccount.getIdpUserId(),
                    ReadAccountByIdResponse.class);
            checkNotNull(readAccountByIdResponse, testName, "readAccountByIdResponse");
            final UserAccount readUserAccount = readAccountByIdResponse.getUserAccount();
            checkNotNull(readUserAccount, testName, "readUserAccount");
            checkUserAccount(userAccount, readUserAccount, testName, "readUserAccount");
            checkEquals(false, readUserAccount.getPasswordResetEmailSent(), testName, "readUserAccount.getPasswordReset()");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void readGuestUserSessionTestImpl() throws Exception {
        final String testName = "readGuestUserSessionTestImpl";

        startTest(testType, testName);

        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest1 = guestSignInRequest();
        AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1 = null;
        try {
            afterIdpGuestSignInResponse1 = appbaseServices
                    .get(UserAccountService.class)
                    .afterIdpGuestSignIn(afterIdpGuestSignInRequest1);

            final UserSession userSession1 = afterIdpGuestSignInResponse1.getUserSession();
            checkNotNull(userSession1, testName, "userSession1");

            final GuestUser guestUser = userSession1.getGuestUser();
            checkNotNull(guestUser, testName, "guestUser");
            final String appbaseUserId = guestUser.getAppbaseUserId();
            checkNotNull(appbaseUserId, testName, "appbaseUserId");

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.GUEST_USER_SESSION_PATH;
            } else {
                url = baseUrl + ReadGuestUserSessionServlet.DEFAULT_PATH;
            }

            final ReadGuestUserSessionResponse readGuestUserSessionResponse = httpGetJson(
                    url + "?appbaseUserId=" + appbaseUserId +
                            "&appbaseSessionId=" + userSession1.getAppbaseSessionId() +
                            "&appbaseRequesterId=" + appbaseUserId,
                    ReadGuestUserSessionResponse.class);
            checkNotNull(readGuestUserSessionResponse, testName, "readGuestUserSessionResponse");
            final UserSession readUserSession = readGuestUserSessionResponse.getUserSession();
            checkNotNull(readUserSession, testName, "readUserSession");
            checkUserSession(userSession1, readUserSession, testName, "readUserSession");
        } finally {
            if (afterIdpGuestSignInResponse1 != null) {
                try {
                    deleteGuestUser(afterIdpGuestSignInResponse1);
                } catch (Exception ex) {
                    // ignore, already deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void readSessionTestImpl() throws Exception {
        final String testName = "readSessionTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.SESSION_PATH;
            } else {
                url = baseUrl + ReadSessionServlet.DEFAULT_PATH;
            }

            final ReadSessionResponse readSessionResponse = httpGetJson(
                    url + "?appbaseUserId=" + appbaseUserId +
                            "&appbaseSessionId=" + userSession.getAppbaseSessionId() +
                            "&appbaseRequesterId=" + appbaseUserId,
                    ReadSessionResponse.class);
            checkNotNull(readSessionResponse, testName, "readSessionResponse");
            final UserSession readUserSession = readSessionResponse.getUserSession();
            checkUserSession(userSession, readUserSession, testName, "readUserSession");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void readUnauthenticatedSessionTestImpl() throws Exception {
        final String testName = "readUnauthenticatedSessionTestImpl";

        startTest(testType, testName);

        UserSession userSession = null;
        try {
            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    createUnauthenticatedSession();
            checkNotNull(createUnauthenticatedSessionResponse, testName, "createUnauthenticatedSessionResponse");

            userSession = createUnauthenticatedSessionResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");
            checkNotNull(userSession.getAppbaseSessionId(), testName, "userSession.getAppbaseSessionId()");

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.UNAUTHENTICATED_SESSION_PATH;
            } else {
                url = baseUrl + ReadUnauthenticatedSessionServlet.DEFAULT_PATH;
            }

            final ReadUnauthenticatedSessionResponse readUnauthenticatedSessionResponse =
                    httpGetJson(
                            url + "?appbaseSessionId=" + userSession.getAppbaseSessionId(),
                            ReadUnauthenticatedSessionResponse.class);
            final UserSession readUserSession = readUnauthenticatedSessionResponse.getUserSession();
            checkNotNull(readUserSession, testName, "readUserSession");
            checkUnauthenticatedUserSession(userSession, readUserSession, testName, "readUserSession");
        } finally {
            if (userSession != null) {
                try {
                    deleteUnauthenticatedSession(userSession);
                } catch (Exception ex) {
                    // ignore, already deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void renewSessionTestImpl() throws Exception {
        final String testName = "renewSessionTestImpl";

        startTest(testType, testName);

        UserSession userSession = null;
        try {
            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    createUnauthenticatedSession();
            checkNotNull(createUnauthenticatedSessionResponse, testName, "createUnauthenticatedSessionResponse");

            userSession = createUnauthenticatedSessionResponse.getUserSession();
            final String oldAppbaseSessionId = userSession.getAppbaseSessionId();
            checkNotNull(userSession, testName, "userSession");
            checkNotNull(oldAppbaseSessionId, testName, "oldAppbaseSessionId");

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.RENEW_SESSION_PATH;
            } else {
                url = baseUrl + RenewSessionServlet.DEFAULT_PATH;
            }

            final RenewSessionRequest renewSessionRequest = new RenewSessionRequest();
            renewSessionRequest.setExpiredUserSession(userSession);

            final RenewSessionResponse renewSessionResponse =
                    httpPut(url, renewSessionRequest, RenewSessionRequest.class, RenewSessionResponse.class);
            checkNotNull(renewSessionResponse, testName, "renewSessionResponse");
            final UserSession renewedUserSession = renewSessionResponse.getRenewedUserSession();
            checkNotNull(renewedUserSession, testName, "renewedUserSession");
            final String newAppbaseSessionId = renewedUserSession.getAppbaseSessionId();
            checkNotNull(newAppbaseSessionId, testName, "newAppbaseSessionId");
            checkNotEquals(newAppbaseSessionId, oldAppbaseSessionId, testName, "newAppbaseSessionId");
        } finally {
            if (userSession != null) {
                try {
                    deleteSession(userSession);
                } catch (Exception ex) {
                    // ignore, already deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void updateAccountTestImpl() throws Exception {
        final String testName = "updateAccountTestImpl";

        startTest(testType, testName);

        String appbaseUserId = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            final UserSession userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            appbaseUserId = userAccount.getAppbaseUserId();
            checkFalse(userAccount.getEmailVerified(), testName, "userAccount.getEmailVerified()");

            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            final UpdateAccountRequest updateAccountRequest = new UpdateAccountRequest();
            updateAccountRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            updateAccountRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            updateAccountRequest.setReadAndUpdate(true);
            updateAccountRequest.setEmail(userAccount.getEmail());
            updateAccountRequest.setName(userAccount.getName());
            updateAccountRequest.setClientPasswordHash(
                    PasswordHash
                            .builder()
                            .hash("clientPasswordHash")
                            .hashAlgorithm(HashAlgorithm.SHA512)
                            .shaRounds(80)
                            .salt("salt123")
                            .build());
            updateAccountRequest.setPasswordResetEmailSent(userAccount.getPasswordResetEmailSent());
            updateAccountRequest.setEmailVerified(true);
            updateAccountRequest.setLanguageCode(userAccount.getLanguageCode());
            updateAccountRequest.setDeleted(userAccount.getDeleted());
            updateAccountRequest.setUserTopic(userAccount.getUserTopic());
            updateAccountRequest.setUserHomeDataCenter(userAccount.getUserHomeDataCenter());
            updateAccountRequest.setAddressLine1(userAccount.getAddressLine1());
            updateAccountRequest.setAddressLine2(userAccount.getAddressLine2());
            updateAccountRequest.setCity(userAccount.getCity());
            updateAccountRequest.setCountry(userAccount.getCountry());
            updateAccountRequest.setStateProvince(userAccount.getStateProvince());
            updateAccountRequest.setPostalCode(userAccount.getPostalCode());
            updateAccountRequest.setLatitude(userAccount.getLatitude());
            updateAccountRequest.setLongitude(userAccount.getLongitude());
            updateAccountRequest.setLastAccessTimestamp(now);
            updateAccountRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.ACCOUNT_PATH;
            } else {
                url = baseUrl + UpdateAccountServlet.DEFAULT_PATH;
            }

            final UpdateAccountResponse updateAccountResponse =
                    httpPut(url, updateAccountRequest, UpdateAccountRequest.class, UpdateAccountResponse.class);
            checkNotNull(updateAccountResponse, testName, "updateAccountResponse");
            checkTrue(updateAccountResponse.isUpdated(), testName, "updateAccountResponse.isUpdated()");

            final String id = UUID.randomUUID().toString();
            final ReadAccountByIdResponse readAccountByIdResponse = appbaseServices
                    .get(UserAccountService.class)
                    .readAccountByAppbaseUserId(new ReadAccountByIdRequest(id, id, userAccount.getAppbaseUserId()));
            checkNotNull(readAccountByIdResponse, testName, "readAccountByIdResponse");

            final UserAccount userAccount1 = readAccountByIdResponse.getUserAccount();
            checkNotNull(userAccount1, testName, "userAccount1");
            checkTrue(userAccount1.getEmailVerified(), testName, "userAccount1.getEmailVerified()");
            checkTimestampMinutes(
                    userAccount.getLastAccessTimestamp(),
                    userAccount1.getLastAccessTimestamp(),
                    testName,
                    "userAccount1.getLastAccessTimestamp()");
        } finally {
            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void updateGuestSessionTestImpl() throws Exception {
        final String testName = "updateGuestSessionTestImpl";

        startTest(testType, testName);

        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest1 = guestSignInRequest();
        AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1 = null;
        try {
            afterIdpGuestSignInResponse1 = appbaseServices
                    .get(UserAccountService.class)
                    .afterIdpGuestSignIn(afterIdpGuestSignInRequest1);

            final UserSession userSession1 = afterIdpGuestSignInResponse1.getUserSession();
            checkNotNull(userSession1, testName, "userSession1");

            final GuestUser guestUser = userSession1.getGuestUser();
            checkNotNull(guestUser, testName, "guestUser");
            final String appbaseUserId = guestUser.getAppbaseUserId();
            checkNotNull(appbaseUserId, testName, "appbaseUserId");

            final UpdateGuestSessionRequest updateGuestSessionRequest = new UpdateGuestSessionRequest();
            updateGuestSessionRequest.setAppbaseSessionId(userSession1.getAppbaseSessionId());
            updateGuestSessionRequest.setUserSessionData(userSession1.getUserSessionData());
            updateGuestSessionRequest.setAppbaseUserId(guestUser.getAppbaseUserId());
            updateGuestSessionRequest.setIdpUserToken(userSession1.getIdpUserToken());
            updateGuestSessionRequest.setName("newName-" + UUID.randomUUID().toString());
            updateGuestSessionRequest.setLanguageCode(guestUser.getLanguageCode());
            updateGuestSessionRequest.setLastAccessTimestamp(AppbaseServiceUtil.nowSeconds());
            updateGuestSessionRequest.setReadAndUpdate(true);
            updateGuestSessionRequest.setAppbaseRequesterId(guestUser.getAppbaseUserId());

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.GUEST_USER_SESSION_PATH;
            } else {
                url = baseUrl + UpdateGuestUserSessionServlet.DEFAULT_PATH;
            }

            final UpdateGuestSessionResponse updateGuestSessionResponse =
                    httpPut(
                            url,
                            updateGuestSessionRequest,
                            UpdateGuestSessionRequest.class,
                            UpdateGuestSessionResponse.class);
            checkNotNull(updateGuestSessionResponse, testName, "updateGuestSessionResponse");

            final UserSession userSession = updateGuestSessionResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");
            final GuestUser guestUser1 = userSession.getGuestUser();
            checkNotNull(guestUser1, testName, "guestUser1");
            checkTimestampMinutes(
                    updateGuestSessionRequest.getLastAccessTimestamp(),
                    guestUser1.getLastAccessTimestamp(),
                    testName,
                    "guestUser1.getLastAccessTimestamp()");
            checkEquals(updateGuestSessionRequest.getName(), guestUser1.getName(), testName, "guestUser1.getName()");
        } finally {
            if (afterIdpGuestSignInResponse1 != null) {
                try {
                    deleteGuestUser(afterIdpGuestSignInResponse1);
                } catch (Exception ex) {
                    // ignore, already deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void updateSessionTestImpl() throws Exception {
        final String testName = "updateSessionTestImpl";

        startTest(testType, testName);

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();

            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, testName, "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, testName, "userAccount");

            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            final UpdateSessionRequest updateSessionRequest = new UpdateSessionRequest();
            updateSessionRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            updateSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            updateSessionRequest.setReadAndUpdate(true);
            updateSessionRequest.setAuthenticated(userSession.isAuthenticated());
            updateSessionRequest.setIdpUserToken(userSession.getIdpUserToken());
            updateSessionRequest.setUserSessionData(userSession.getUserSessionData());
            updateSessionRequest.setLastAccessTimestamp(now);
            updateSessionRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.SESSION_PATH;
            } else {
                url = baseUrl + UpdateSessionServlet.DEFAULT_PATH;
            }

            final UpdateSessionResponse updateSessionResponse =
                    httpPut(url, updateSessionRequest, UpdateSessionRequest.class, UpdateSessionResponse.class);
            checkNotNull(updateSessionResponse, testName, "updateSessionResponse");
            final UserSession userSession1 = updateSessionResponse.getUserSession();
            checkNotNull(userSession1, testName, "userSession1");
            checkEquals(
                    updateSessionRequest.getIdpUserToken(),
                    userSession1.getIdpUserToken(),
                    testName,
                    "userSession1.getIdpUserToken()");
            checkTimestampMinutes(
                    now,
                    userSession1.getLastAccessTimestamp(),
                    testName,
                    "userSession1.getLastAccessTimestamp()");
        } finally {
            if (userSession != null) {
                try {
                    deleteSession(userSession);
                } catch (Exception ex) {
                    // ignore, already deleted
                }
            }
        }

        endTest(testType, testName);
    }

    public void updateUnauthenticatedSessionTestImpl() throws Exception {
        final String testName = "updateUnauthenticatedSessionTestImpl";

        startTest(testType, testName);

        UserSession userSession = null;
        try {
            final CreateUnauthenticatedSessionResponse unauthenticatedSession = createUnauthenticatedSession();
            userSession = unauthenticatedSession.getUserSession();

            final UpdateUnauthenticatedSessionRequest updateUnauthenticatedSessionRequest =
                    new UpdateUnauthenticatedSessionRequest();
            updateUnauthenticatedSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());

            final AppbaseCore appbaseCore = appbaseServices.getCoreServices().appbaseCore();
            final IdService idService = new DefaultIdService(
                    appbaseCore.getLogService(),
                    appbaseCore.getMetricService(),
                    appbaseCore.getMediatorService(),
                    appbaseCore.getServiceRegistry(),
                    DefaultIdServiceConfiguration.builder().build());
            final String new14CharsId1 = idService.new14CharsId();
            final String new14CharsId2 = idService.new14CharsId();
            final UserSessionData userSessionData = userSession.getUserSessionData();
            final Map<String, String> updateDataMap = new HashMap<>(userSessionData.getDataMap());
            updateDataMap.put(new14CharsId1, new14CharsId2);

            final UserSessionData updateUserSessionData = new UserSessionData();
            updateUserSessionData.setDataMap(updateDataMap);
            updateUnauthenticatedSessionRequest.setUserSessionData(updateUserSessionData);
            updateUnauthenticatedSessionRequest.setLastAccessTimestamp(AppbaseServiceUtil.nowSeconds());
            updateUnauthenticatedSessionRequest.setReadAndUpdate(true);

            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH +
                        UserAccountJaxrsResource.UNAUTHENTICATED_SESSION_PATH;
            } else {
                url = baseUrl + UpdateUnauthenticatedSessionServlet.DEFAULT_PATH;
            }

            final UpdateUnauthenticatedSessionResponse updateUnauthenticatedSessionResponse = httpPut(
                    url,
                    updateUnauthenticatedSessionRequest,
                    UpdateUnauthenticatedSessionRequest.class,
                    UpdateUnauthenticatedSessionResponse.class);
            checkNotNull(updateUnauthenticatedSessionResponse, testName, "updateUnauthenticatedSessionResponse");
            final UserSession updatedUserSession = updateUnauthenticatedSessionResponse.getUserSession();

            final ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest =
                    new ReadUnauthenticatedSessionRequest();
            readUnauthenticatedSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            final ReadUnauthenticatedSessionResponse readUnauthenticatedSessionResponse = appbaseServices
                    .get(UserAccountService.class)
                    .readUnauthenticatedSession(readUnauthenticatedSessionRequest);
            checkNotNull(readUnauthenticatedSessionResponse, testName, "readUnauthenticatedSessionResponse");
            final UserSession readUserSession = readUnauthenticatedSessionResponse.getUserSession();
            checkNotNull(readUserSession, testName, "readUserSession");

            checkUnauthenticatedUserSession(updatedUserSession, readUserSession, testName, "readUserSession");
        } finally {
            if (userSession != null) {
                deleteUnauthenticatedSession(userSession);
            }
        }

        endTest(testType, testName);
    }

    private boolean isRestApiTest() {
        return UserAccountJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
