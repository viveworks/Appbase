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

package com.viveworks.appbase.stacktest.test.service.user.account;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInResponse;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByAppbaseUserIdRequest;
import com.viveworks.appbase.service.user.account.data.DeleteGuestUserRequest;
import com.viveworks.appbase.service.user.account.data.DeleteGuestUserResponse;
import com.viveworks.appbase.service.user.account.data.GuestUser;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;
import com.viveworks.appbase.service.user.session.store.UserSessionStoreException;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import static com.viveworks.appbase.service.user.account.UserAccountService.APP_NAME_KEY;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractUserAccountApiTest extends AbstractTest {
    private static final int MAX_POSTAL_CODE_SUFFIX_LENGTH = 20;

    public AbstractUserAccountApiTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);
    }

    public void checkTimestampMinutes(
            final LocalDateTime expected,
            final LocalDateTime actual,
            final String testName,
            final String valueName)
    {
        checkEquals(expected.getDayOfMonth(), actual.getDayOfMonth(), testName, valueName);
        checkEquals(expected.getMonth(), actual.getMonth(), testName, valueName);
        checkEquals(expected.getYear(), actual.getYear(), testName, valueName);
        if (expected.getMinute() < 58) {
            checkEquals(expected.getHour(), actual.getHour(), testName, valueName);
            if (expected.getSecond() < 58) {
                checkEquals(expected.getMinute(), actual.getMinute(), testName, valueName);
            }
        }
    }

    public void checkUserAccount(
            final UserAccount expected,
            final UserAccount actual,
            final String testName,
            final String valueName) {
        checkEquals(expected.getAppbaseUserId(), actual.getAppbaseUserId(), testName, valueName);
        checkEquals(expected.getIdpUserId(), actual.getIdpUserId(), testName, valueName);
        checkEquals(expected.getEmail(), actual.getEmail(), testName, valueName);
        checkEquals(expected.getEmailVerified(), actual.getEmailVerified(), testName, valueName);
        checkEquals(expected.getUserTopic(), actual.getUserTopic(), testName, valueName);
        checkEquals(expected.getUserHomeDataCenter(), actual.getUserHomeDataCenter(), testName, valueName);
        checkEquals(expected.getLanguageCode(), actual.getLanguageCode(), testName, valueName);
        checkEquals(expected.getAddressLine1(), actual.getAddressLine1(), testName, valueName);
        checkEquals(expected.getAddressLine2(), actual.getAddressLine2(), testName, valueName);
        checkEquals(expected.getCity(), actual.getCity(), testName, valueName);
        checkEquals(expected.getStateProvince(), actual.getStateProvince(), testName, valueName);
        checkEquals(expected.getCountry(), actual.getCountry(), testName, valueName);
        checkEquals(expected.getPostalCode(), actual.getPostalCode(), testName, valueName);
        checkEquals(expected.getLatitude(), actual.getLatitude(), testName, valueName);
        checkEquals(expected.getLongitude(), actual.getLongitude(), testName, valueName);
        checkEquals(expected.getDeleted(), actual.getDeleted(), testName, valueName);
        checkTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp(), testName, valueName);
        checkTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp(), testName, valueName);
    }

    public void checkUnauthenticatedUserSession(
            final UserSession expected,
            final UserSession actual,
            final String testName,
            final String valueName)
    {
        checkNotNull(expected, testName, valueName);
        checkNotNull(actual, testName, valueName);

        checkEquals(expected.getUserSessionData(), actual.getUserSessionData(), testName, valueName);
        checkTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp(), testName, valueName);
        checkTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp(), testName, valueName);
    }

    public void checkGuestUser(
            final GuestUser expected,
            final GuestUser actual,
            final String testName,
            final String valueName)
    {
        checkEquals(expected.getAppbaseUserId(), actual.getAppbaseUserId(), testName, valueName);
        checkEquals(expected.getIdpGuestId(), actual.getIdpGuestId(), testName, valueName);
        checkEquals(expected.getLanguageCode(), actual.getLanguageCode(), testName, valueName);
        checkEquals(expected.getName(), actual.getName(), testName, valueName);
        checkTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp(), testName, valueName);
        checkTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp(), testName, valueName);
    }

    public void checkUserSession(
            final UserSession expected,
            final UserSession actual,
            final String testName,
            final String valueName)
    {
        checkNotNull(expected, testName, valueName);
        checkNotNull(actual, testName, valueName);

        checkEquals(expected.getAppbaseSessionId(), actual.getAppbaseSessionId(), testName, valueName);
        checkEquals(expected.getIdpUserToken(), actual.getIdpUserToken(), testName, valueName);
        checkEquals(expected.getUserSessionData(), actual.getUserSessionData(), testName, valueName);
        checkTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp(), testName, valueName);
        checkTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp(), testName, valueName);

        final GuestUser expectedGuestUser = expected.getGuestUser();
        final GuestUser actualGuestUser = actual.getGuestUser();
        if (expectedGuestUser != null) {
            checkNotNull(actualGuestUser, testName, valueName);
            checkGuestUser(expectedGuestUser, actualGuestUser, testName, valueName);
        } else {
            checkNull(actualGuestUser, testName, valueName);
            checkUserAccount(expected.getUserAccount(), actual.getUserAccount(), testName, valueName);
        }
    }

    public void checkAppNameInSessionData(
            final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest,
            final UserSession userSession,
            final String testName,
            final String valueName)
    {
        checkNotNull(afterIdpGuestSignInRequest, testName, valueName);
        checkNotNull(userSession, testName, valueName);

        final String appName = afterIdpGuestSignInRequest.getAppName();
        final UserSessionData userSessionData1 = userSession.getUserSessionData();

        final Map<String, String> dataMap1 = userSessionData1.getDataMap();
        if (appName != null) {
            checkTrue(dataMap1.containsKey(APP_NAME_KEY), testName, valueName);
            checkEquals(appName, dataMap1.get(APP_NAME_KEY), testName, valueName);
        }
    }

    protected CreateUnauthenticatedSessionResponse createUnauthenticatedSession() throws UserAccountServiceException {
        final CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest =
                createUnauthenticatedSessionRequest();

        return appbaseServices
                .get(UserAccountService.class)
                .createUnauthenticatedSession(createUnauthenticatedSessionRequest);
    }

    protected CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest() {
        final UserSessionData userSessionData = new UserSessionData();
        final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
        userSessionData.setDataMap(Map.of("k1", now.toString()));

        final CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest =
                new CreateUnauthenticatedSessionRequest();
        createUnauthenticatedSessionRequest.setUserSessionData(userSessionData);
        createUnauthenticatedSessionRequest.setCreationTimestamp(now);
        createUnauthenticatedSessionRequest.setLastAccessTimestamp(now);

        return createUnauthenticatedSessionRequest;
    }

    protected void deleteAccountByAppbaseUserId(final String appbaseUserId)
            throws AuthorizationServiceException, UserAccountServiceException
    {
        final DeleteAccountByAppbaseUserIdRequest deleteAccountByAppbaseUserIdRequest =
                new DeleteAccountByAppbaseUserIdRequest();
        deleteAccountByAppbaseUserIdRequest.setAppbaseUserId(appbaseUserId);
        deleteAccountByAppbaseUserIdRequest.setAppbaseRequesterId(appbaseUserId);

        appbaseServices.get(UserAccountService.class).deleteAccountByAppbaseUserId(deleteAccountByAppbaseUserIdRequest);
    }

    protected AfterIdpCreateUserWithEmailPasswordRequest createUserWithEmailPasswordRequest() {
        final AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest =
                new AfterIdpCreateUserWithEmailPasswordRequest();
        final FirebaseAuthEmulatorIdentity.Identity identity = FirebaseAuthEmulatorIdentity.createEmailUser();
        final String id = identity.uid();
        afterIdpCreateUserWithEmailPasswordRequest.setAppbaseRequesterId(id);
        afterIdpCreateUserWithEmailPasswordRequest.setIdpUserId(id);
        afterIdpCreateUserWithEmailPasswordRequest.setIdpUserToken(identity.idToken());
        afterIdpCreateUserWithEmailPasswordRequest.setName("name-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setEmail(identity.email());
        afterIdpCreateUserWithEmailPasswordRequest.setClientPasswordHash(
                PasswordHash
                        .builder()
                        .hash("clientPasswordHash-" + id)
                        .hashAlgorithm(HashAlgorithm.SHA512)
                        .shaRounds(80)
                        .salt("salt123")
                        .build());
        afterIdpCreateUserWithEmailPasswordRequest.setLanguageCode(Locale.ENGLISH.getLanguage());
        afterIdpCreateUserWithEmailPasswordRequest.setAppName("appName-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setAddressLine1("Address line 1-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setAddressLine2("Address line 2-" + id) ;
        afterIdpCreateUserWithEmailPasswordRequest.setCity("City-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setStateProvince("StateProvince-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setCountry("Country-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setPostalCode(
                "PostalCode-" + id.substring(0, Math.min(MAX_POSTAL_CODE_SUFFIX_LENGTH, id.length())));
        afterIdpCreateUserWithEmailPasswordRequest.setLatitude("24." + id);
        afterIdpCreateUserWithEmailPasswordRequest.setLongitude("9." + id);

        return afterIdpCreateUserWithEmailPasswordRequest;
    }

    protected AfterIdpCreateUserWithEmailPasswordResponse createUserWithEmailPassword() throws Exception {
        final AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest =
                createUserWithEmailPasswordRequest();
        return appbaseServices
                .get(UserAccountService.class)
                .afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
    }

    protected AfterIdpGuestSignInRequest guestSignInRequest() {
        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest = new AfterIdpGuestSignInRequest();
        final FirebaseAuthEmulatorIdentity.Identity identity = FirebaseAuthEmulatorIdentity.createGuestUser();
        final String id = identity.uid();
        afterIdpGuestSignInRequest.setAppbaseRequesterId(id);
        afterIdpGuestSignInRequest.setIdpGuestId(id);
        afterIdpGuestSignInRequest.setIdpGuestUserToken(identity.idToken());
        afterIdpGuestSignInRequest.setAppName("appName-" + id);
        afterIdpGuestSignInRequest.setName("name-" + id);
        afterIdpGuestSignInRequest.setLanguageCode(Locale.ENGLISH.getLanguage());
        afterIdpGuestSignInRequest.setUserSessionData(new UserSessionData(Map.of("key-" + id, "value-" + id)));
        afterIdpGuestSignInRequest.setAddressLine1("Address line 1-" + id);
        afterIdpGuestSignInRequest.setAddressLine2("Address line 2-" + id) ;
        afterIdpGuestSignInRequest.setCity("City-" + id);
        afterIdpGuestSignInRequest.setCountry("Country-" + id);
        afterIdpGuestSignInRequest.setPostalCode("PostalCode-" +
                id.substring(0, Math.min(MAX_POSTAL_CODE_SUFFIX_LENGTH, id.length())));
        afterIdpGuestSignInRequest.setLatitude("23.30");
        afterIdpGuestSignInRequest.setLongitude("24.09");

        return afterIdpGuestSignInRequest;
    }

    protected void deleteGuestUser(final AfterIdpGuestSignInResponse afterIdpGuestSignInResponse) throws Exception {
        if (afterIdpGuestSignInResponse != null) {
            final UserSession userSession = afterIdpGuestSignInResponse.getUserSession();
            if (userSession != null) {
                final UserAccount userAccount = userSession.getUserAccount();
                if (userAccount != null) {
                    final String appbaseUserId = userAccount.getAppbaseUserId();
                    if (AppbaseUtil.isNotBlank(appbaseUserId)) {
                        final DeleteGuestUserRequest deleteGuestUserRequest = new DeleteGuestUserRequest();
                        deleteGuestUserRequest.setAppbaseUserId(appbaseUserId);
                        deleteGuestUserRequest.setAppbaseRequesterId(appbaseUserId);
                        final DeleteGuestUserResponse deleteGuestUserResponse =
                                appbaseServices.get(UserAccountService.class).deleteGuestUser(deleteGuestUserRequest);
                        checkNotNull(deleteGuestUserResponse, getClass().getName(), "deleteGuestUser");
                        checkTrue(deleteGuestUserResponse.isDeleted(), getClass().getName(), "deleteGuestUser");
                    }
                }
            }
        }
    }

    protected ReadAccountByIdResponse readAccountByAppbaseUserId(final String appbaseUserId) throws Exception {
        final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest();
        readAccountByIdRequest.setId(appbaseUserId);
        readAccountByIdRequest.setAppbaseRequesterId(appbaseUserId);

        return appbaseServices.get(UserAccountService.class).readAccountByAppbaseUserId(readAccountByIdRequest);
    }

    protected void deleteUnauthenticatedSession(final UserSession userSession) throws UserSessionStoreException {
        if (userSession != null) {
            appbaseServices.get(UserSessionStore.class).deleteUnauthenticatedSession(userSession.getAppbaseSessionId());
        }

    }

    protected void deleteSession(final UserSession userSession) throws UserSessionStoreException {
        if (userSession != null) {
            appbaseServices.get(UserSessionStore.class).deleteAllSessions(userSession.getAppbaseSessionId());
        }
    }
}
