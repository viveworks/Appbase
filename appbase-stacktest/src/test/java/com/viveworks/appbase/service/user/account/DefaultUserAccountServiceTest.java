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
package com.viveworks.appbase.service.user.account;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientTestHelper;
import com.viveworks.appbase.client.service.aerospike.DefaultAerospikeClientService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientServiceTestHelper;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientService;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientServiceTestHelper;
import com.viveworks.appbase.client.service.jdbc.JdbcClientServiceTestHelper;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.client.service.jdbi.JdbiClientServiceTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.user.testhelper.AppbaseServiceTestHelper;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.authorization.DefaultAuthorizationService;
import com.viveworks.appbase.service.authorization.DefaultAuthorizationServiceConfiguration;
import com.viveworks.appbase.service.authorization.PermitAllContextAuthorizer;
import com.viveworks.appbase.service.cluster.ClusterService;
import com.viveworks.appbase.service.cluster.StaticClusterService;
import com.viveworks.appbase.service.cluster.StaticClusterServiceConfiguration;
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.DefaultPasswordService;
import com.viveworks.appbase.service.password.DefaultPasswordServiceConfiguration;
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordService;
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
import com.viveworks.appbase.service.user.account.data.UpdateAccountRequest;
import com.viveworks.appbase.service.user.account.data.UpdateAccountResponse;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.firebaseauth.DefaultFirebaseAuthService;
import com.viveworks.appbase.service.user.account.firebaseauth.DefaultFirebaseAuthServiceConfiguration;
import com.viveworks.appbase.service.user.account.store.JdbiUserAccountStore;
import com.viveworks.appbase.service.user.account.store.JdbiUserAccountStoreConfiguration;
import com.viveworks.appbase.stacktest.test.service.user.account.FirebaseAuthEmulatorIdentity;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import com.viveworks.appbase.service.user.session.store.DefaultUserSessionStore;
import com.viveworks.appbase.service.user.session.store.DefaultUserSessionStoreConfiguration;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;
import com.viveworks.appbase.service.user.session.store.UserSessionStoreException;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "AerospikeClientServiceTests.enabled", matches = "true")
class DefaultUserAccountServiceTest {
    private static AppbaseCore appbaseCore;
    private static Handle handle;
    private static IdService idService;
    private static PasswordService passwordService;
    private static JdbiClientService jdbiClientService;
    private static UserSessionStore userSessionStore;
    private static DefaultUserAccountService userAccountService;
    private static DefaultAerospikeClientService defaultAerospikeClientService;

    @BeforeAll
    static void beforeAllTests() {
        System.setProperty(
                ClientCoreTestHelper.CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-core.yaml");
        System.setProperty(
                ClientCoreTestHelper.CLIENT_CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-client-core.yaml");
        System.setProperty(
                JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY,
                AppbaseTestInfraProfile.resolve(
                        AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-vitess-jdbc.yaml"));
        System.setProperty(
                JdbiClientServiceTestHelper.JDBI_CLIENT_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-vitess-jdbi.yaml");
        System.setProperty(
                AerospikeClientTestHelper.AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-aerospike.yaml");
        System.setProperty(
                FirebaseAppClientServiceTestHelper.FIREBASE_APP_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-firebase-app.yaml");
        System.setProperty(
                FirebaseAuthClientServiceTestHelper.FIREBASE_AUTH_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-firebase-auth.yaml");

        appbaseCore = AerospikeClientTestHelper.appbaseCore();
        idService = new DefaultIdService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultIdServiceConfiguration.builder().build());
        passwordService = new DefaultPasswordService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                new DefaultPasswordServiceConfiguration());
        final ClusterService clusterService = new StaticClusterService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                StaticClusterServiceConfiguration.builder().homeDataCenter("local-test").build());
        defaultAerospikeClientService = AerospikeClientTestHelper.defaultAerospikeClientService();
        final DefaultUserSessionStoreConfiguration defaultUserSessionStoreConfiguration =
                DefaultUserSessionStoreConfiguration.builder().build();
        userSessionStore = new DefaultUserSessionStore(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                idService,
                defaultAerospikeClientService.getClientIntegrationService(),
                defaultUserSessionStoreConfiguration);
        jdbiClientService = JdbiClientServiceTestHelper.defaultJdbiClientService();
        handle = jdbiClientService.getClient().open();
        final JdbiUserAccountStore jdbiUserAccountStore = new JdbiUserAccountStore(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                passwordService,
                handle,
                JdbiUserAccountStoreConfiguration.builder().build());
        final AuthorizationService authorizationService = new DefaultAuthorizationService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultAuthorizationServiceConfiguration.builder().build(),
                new PermitAllContextAuthorizer());

        final FirebaseAuthClientService firebaseAuthClientService =
                FirebaseAuthClientServiceTestHelper.firebaseAuthClientService(appbaseCore);

        final DefaultFirebaseAuthServiceConfiguration defaultFirebaseAuthServiceConfiguration =
                DefaultFirebaseAuthServiceConfiguration
                        .builder()
                        .userTokenVerificationEnabled(true)
                        .build();
        final DefaultFirebaseAuthService defaultFirebaseAuthService =
                new DefaultFirebaseAuthService(
                        appbaseCore.getLogService(),
                        appbaseCore.getValidationService(),
                        appbaseCore.getMetricService(),
                        appbaseCore.getMediatorService(),
                        appbaseCore.getServiceRegistry(),
                        defaultFirebaseAuthServiceConfiguration,
                        firebaseAuthClientService.getClientIntegrationService());
        userAccountService = new DefaultUserAccountService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getValidationService(),
                appbaseCore.getServiceRegistry(),
                idService,
                clusterService,
                authorizationService,
                userSessionStore,
                jdbiUserAccountStore,
                defaultFirebaseAuthService,
                passwordService,
                new DefaultUserAccountServiceConfiguration());
    }

    @BeforeEach
    void beforeEachTest() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    @AfterAll
    static void afterAllTests() {
        if (defaultAerospikeClientService != null) {
            defaultAerospikeClientService.shutdown();
        }

        if (handle != null) {
            handle.close();
        }

        if (jdbiClientService != null) {
            jdbiClientService.shutdown();
        }
    }

    @Test
    void testRegisterService() throws InterruptedException {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();

        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(UserAccountService.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                UserAccountService.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(userAccountService, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 =
                serviceRegistry.getAppbaseService(DefaultUserAccountService.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(userAccountService, appbaseServiceEntry2.getService());
    }

    @Test
    void testCreateUserWithEmailAndPassword()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest =
                new AfterIdpCreateUserWithEmailPasswordRequest();
        final FirebaseAuthEmulatorIdentity.Identity identity = FirebaseAuthEmulatorIdentity.createEmailUser();
        final String id = identity.uid();
        afterIdpCreateUserWithEmailPasswordRequest.setIdpUserId(id);
        final String token = identity.idToken();
        final String name = "name-" + id;
        final String email = identity.email();
        final PasswordHash passwordHash = PasswordHash
                .builder()
                .hash("passwordHash" + id)
                .salt("salt")
                .shaRounds(80)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .build();
        final String languageCode = Locale.ENGLISH.getLanguage();
        final String appName = "appName-" + id;

        afterIdpCreateUserWithEmailPasswordRequest.setAppbaseRequesterId(id);
        afterIdpCreateUserWithEmailPasswordRequest.setIdpUserToken(token);
        afterIdpCreateUserWithEmailPasswordRequest.setName(name);
        afterIdpCreateUserWithEmailPasswordRequest.setEmail(email);
        afterIdpCreateUserWithEmailPasswordRequest.setClientPasswordHash(passwordHash);
        afterIdpCreateUserWithEmailPasswordRequest.setLanguageCode(languageCode);
        afterIdpCreateUserWithEmailPasswordRequest.setAppName(appName);
        afterIdpCreateUserWithEmailPasswordRequest.setAddressLine1("Address line 1-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setAddressLine2("Address line 2-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setCity("City-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setCountry("Country-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setPostalCode(
                "PostalCode-" + id.substring(0, Math.min(20, id.length())));
        afterIdpCreateUserWithEmailPasswordRequest.setLatitude("24." + id);
        afterIdpCreateUserWithEmailPasswordRequest.setLongitude("9." + id);

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userSession.getAppbaseSessionId());
            Assertions.assertNotNull(userSession.getCreationTimestamp());
            Assertions.assertNotNull(userSession.getLastAccessTimestamp());
            Assertions.assertNotNull(userSession.getUserSessionData());
            final String idpUserToken = userSession.getIdpUserToken();
            Assertions.assertNotNull(idpUserToken);
            Assertions.assertEquals(token, idpUserToken);
            Assertions.assertNull(userSession.getGuestUser());

            Assertions.assertEquals(name, userAccount.getName());
            Assertions.assertEquals(email, userAccount.getEmail());
            Assertions.assertNotNull(userAccount.getPasswordHashes());
            Assertions.assertEquals(languageCode, userAccount.getLanguageCode());

            Assertions.assertNotNull(userAccount.getAppbaseUserId());
            Assertions.assertEquals(userAccount.getAppbaseUserId(), userAccount.getUserTopic());
            Assertions.assertNotNull(userAccount.getPasswordResetEmailSent());
            Assertions.assertEquals(id, userAccount.getIdpUserId());
            Assertions.assertNotNull(userAccount.getEmailVerified());
            Assertions.assertFalse(userAccount.getEmailVerified());
            Assertions.assertNotNull(userAccount.getUserHomeDataCenter());
            Assertions.assertNotNull(userAccount.getDeleted());
            Assertions.assertFalse(userAccount.getDeleted());
            Assertions.assertNotNull(userAccount.getCreationTimestamp());
            Assertions.assertNotNull(userAccount.getLastAccessTimestamp());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getAddressLine1(), userAccount.getAddressLine1());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getAddressLine2(), userAccount.getAddressLine2());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getCity(), userAccount.getCity());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getStateProvince(), userAccount.getStateProvince());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getCountry(), userAccount.getCountry());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getLatitude(), userAccount.getLatitude());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getLongitude(), userAccount.getLongitude());
        } finally {
            if (userSession != null) {
                final DeleteAccountByAppbaseUserIdRequest deleteAccountByAppbaseUserIdRequest =
                        new DeleteAccountByAppbaseUserIdRequest();
                final String appbaseUserId = userSession.getUserAccount().getAppbaseUserId();
                deleteAccountByAppbaseUserIdRequest.setAppbaseRequesterId(appbaseUserId);
                deleteAccountByAppbaseUserIdRequest.setAppbaseUserId(appbaseUserId);
                userAccountService.deleteAccountByAppbaseUserId(deleteAccountByAppbaseUserIdRequest);
            }
        }
    }

    @Test
    void testAfterIdpConfirmPasswordReset() throws Exception {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userAccount.getAppbaseUserId());

            final String email = userAccount.getEmail();
            Assertions.assertNotNull(email);

            final AfterIdpConfirmPasswordResetRequest afterIdpConfirmPasswordResetRequest =
                    new AfterIdpConfirmPasswordResetRequest();
            final PasswordHash passwordResetCodeHash = PasswordHash
                    .builder()
                    .hash(idService.newId())
                    .salt("salt")
                    .shaRounds(80)
                    .hashAlgorithm(HashAlgorithm.SHA512)
                    .build();
            final PasswordHash newClientPasswordHash = PasswordHash
                    .builder()
                    .hash("newPassword-" + idService.new14CharsId())
                    .salt("salt")
                    .shaRounds(80)
                    .hashAlgorithm(HashAlgorithm.SHA512)
                    .build();
            afterIdpConfirmPasswordResetRequest.setEmail(email);
            afterIdpConfirmPasswordResetRequest.setPasswordResetCodeHash(passwordResetCodeHash);
            afterIdpConfirmPasswordResetRequest.setAppbaseRequesterId(email);
            afterIdpConfirmPasswordResetRequest.setNewClientPasswordHash(newClientPasswordHash);
            afterIdpConfirmPasswordResetRequest.setTraceId(idService.newId());

            final AfterIdpConfirmPasswordResetResponse afterIdpConfirmPasswordResetResponse =
                    userAccountService.afterIdpConfirmPasswordReset(afterIdpConfirmPasswordResetRequest);
            Assertions.assertNotNull(afterIdpConfirmPasswordResetResponse);

            final UserAccount responseUserAccount = afterIdpConfirmPasswordResetResponse.getUserAccount();
            Assertions.assertNotNull(responseUserAccount);

            final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                    idService.newId(), userAccount.getAppbaseUserId(), userAccount.getAppbaseUserId());
            final ReadAccountByIdResponse readAccountByIdResponse =
                    userAccountService.readAccountByAppbaseUserId(readAccountByIdRequest);
            Assertions.assertNotNull(readAccountByIdResponse);

            final UserAccount readUserAccount = readAccountByIdResponse.getUserAccount();
            Assertions.assertNotNull(readUserAccount);
            final PasswordHash readPasswordResetCodeHash = readUserAccount.getPasswordResetCodeHash();
            Assertions.assertNotNull(readPasswordResetCodeHash);
            Assertions.assertEquals(
                    afterIdpConfirmPasswordResetRequest.getPasswordResetCodeHash(), readPasswordResetCodeHash);
            Assertions.assertEquals(
                    afterIdpConfirmPasswordResetRequest.getNewClientPasswordHash(),
                    readUserAccount.getPasswordHashes().getClientPasswordHash());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getAddressLine1(), readUserAccount.getAddressLine1());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getAddressLine2(), readUserAccount.getAddressLine2());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getCity(), readUserAccount.getCity());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getStateProvince(), readUserAccount.getStateProvince());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getCountry(), readUserAccount.getCountry());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getLatitude(), readUserAccount.getLatitude());
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getLongitude(), readUserAccount.getLongitude());
        } finally {
            if (userSession != null) {
                deleteSession(userSession);
            }
        }
    }

    @Test
    void testCreateUnauthenticatedSession() throws UserSessionStoreException, UserAccountServiceException {
        final CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest =
                new CreateUnauthenticatedSessionRequest();

        UserSession userSession = null;
        try {
            final UserSessionData userSessionData = new UserSessionData();
            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            userSessionData.setDataMap(Map.of("k1", now.toString()));
            createUnauthenticatedSessionRequest.setUserSessionData(userSessionData);
            createUnauthenticatedSessionRequest.setCreationTimestamp(now);
            createUnauthenticatedSessionRequest.setLastAccessTimestamp(now);

            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    userAccountService.createUnauthenticatedSession(createUnauthenticatedSessionRequest);
            Assertions.assertNotNull(createUnauthenticatedSessionResponse);
            userSession = createUnauthenticatedSessionResponse.getUserSession();
            Assertions.assertNotNull(userSession);
            Assertions.assertNotNull(userSession.getAppbaseSessionId());
            Assertions.assertEquals(userSessionData, userSession.getUserSessionData());
        } finally {
            if (userSession != null) {
                deleteSession(userSession);
            }
        }
    }

    @Test
    void testExistsAccountByAppbaseUserId()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userAccount.getAppbaseUserId());

            final ExistsAccountByIdRequest existsAccountByIdRequest1 = new ExistsAccountByIdRequest();
            existsAccountByIdRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            existsAccountByIdRequest1.setId(userAccount.getAppbaseUserId());
            final ExistsAccountByIdResponse existsAccountByIdResponse1 =
                    userAccountService.existsAccountByAppbaseUserId(existsAccountByIdRequest1);
            Assertions.assertNotNull(existsAccountByIdResponse1);
            Assertions.assertTrue(existsAccountByIdResponse1.isExists());

            final ExistsAccountByIdRequest existsAccountByIdRequest2 = new ExistsAccountByIdRequest();
            existsAccountByIdRequest2.setAppbaseRequesterId(idService.newId());
            existsAccountByIdRequest2.setId(idService.newId());
            final ExistsAccountByIdResponse existsAccountByIdResponse2 =
                    userAccountService.existsAccountByAppbaseUserId(existsAccountByIdRequest2);
            Assertions.assertNotNull(existsAccountByIdResponse2);
            Assertions.assertFalse(existsAccountByIdResponse2.isExists());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testExistsAccountByIdpUserId()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userAccount.getIdpUserId());

            final ExistsAccountByIdRequest existsAccountByIdRequest1 = new ExistsAccountByIdRequest();
            existsAccountByIdRequest1.setAppbaseRequesterId(userAccount.getIdpUserId());
            existsAccountByIdRequest1.setId(userAccount.getIdpUserId());
            final ExistsAccountByIdResponse existsAccountByIdResponse1 =
                    userAccountService.existsAccountByIdpUserId(existsAccountByIdRequest1);
            Assertions.assertNotNull(existsAccountByIdResponse1);
            Assertions.assertTrue(existsAccountByIdResponse1.isExists());

            final ExistsAccountByIdRequest existsAccountByIdRequest2 = new ExistsAccountByIdRequest();
            existsAccountByIdRequest2.setAppbaseRequesterId(idService.newId());
            existsAccountByIdRequest2.setId(idService.newId());
            final ExistsAccountByIdResponse existsAccountByIdResponse2 =
                    userAccountService.existsAccountByAppbaseUserId(existsAccountByIdRequest2);
            Assertions.assertNotNull(existsAccountByIdResponse2);
            Assertions.assertFalse(existsAccountByIdResponse2.isExists());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testExistsAccountByEmail()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userAccount.getEmail());

            final ExistsAccountByIdRequest existsAccountByIdRequest1 = new ExistsAccountByIdRequest();
            existsAccountByIdRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            existsAccountByIdRequest1.setId(userAccount.getEmail());
            final ExistsAccountByIdResponse existsAccountByIdResponse1 =
                    userAccountService.existsAccountByEmail(existsAccountByIdRequest1);
            Assertions.assertNotNull(existsAccountByIdResponse1);
            Assertions.assertTrue(existsAccountByIdResponse1.isExists());

            final ExistsAccountByIdRequest existsAccountByIdRequest2 = new ExistsAccountByIdRequest();
            existsAccountByIdRequest2.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            existsAccountByIdRequest2.setId(idService.newId() + "d.e@f.com");
            final ExistsAccountByIdResponse existsAccountByIdResponse2 =
                    userAccountService.existsAccountByAppbaseUserId(existsAccountByIdRequest2);
            Assertions.assertNotNull(existsAccountByIdResponse2);
            Assertions.assertFalse(existsAccountByIdResponse2.isExists());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testReadAccountByAppbaseUserId()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userAccount.getAppbaseUserId());

            final ReadAccountByIdRequest readAccountByIdRequest1 = new ReadAccountByIdRequest();
            readAccountByIdRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            readAccountByIdRequest1.setId(userAccount.getAppbaseUserId());
            final ReadAccountByIdResponse readAccountByIdResponse1 =
                    userAccountService.readAccountByAppbaseUserId(readAccountByIdRequest1);
            Assertions.assertNotNull(readAccountByIdResponse1);
            final UserAccount readUserAccount1 = readAccountByIdResponse1.getUserAccount();
            Assertions.assertNotNull(readUserAccount1);
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccount1);
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getClientPasswordHash(),
                    readUserAccount1.getPasswordHashes().getClientPasswordHash());
            Assertions.assertEquals(false, readUserAccount1.getPasswordResetEmailSent());

            final ReadAccountByIdRequest readAccountByIdRequest2 = new ReadAccountByIdRequest();
            readAccountByIdRequest2.setAppbaseRequesterId(idService.newId());
            readAccountByIdRequest2.setId(idService.newId());
            final ReadAccountByIdResponse readAccountByIdResponse2 =
                    userAccountService.readAccountByAppbaseUserId(readAccountByIdRequest2);
            Assertions.assertNotNull(readAccountByIdResponse2);
            final UserAccount readUserAccount2 = readAccountByIdResponse2.getUserAccount();
            Assertions.assertNull(readUserAccount2);
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testReadAccountByIdpUserId()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userAccount.getIdpUserId());

            final ReadAccountByIdRequest readAccountByIdRequest1 = new ReadAccountByIdRequest();
            readAccountByIdRequest1.setAppbaseRequesterId(userAccount.getIdpUserId());
            readAccountByIdRequest1.setId(userAccount.getIdpUserId());
            final ReadAccountByIdResponse readAccountByIdResponse1 =
                    userAccountService.readAccountByIdpUserId(readAccountByIdRequest1);
            Assertions.assertNotNull(readAccountByIdResponse1);
            final UserAccount readUserAccount1 = readAccountByIdResponse1.getUserAccount();
            Assertions.assertNotNull(readUserAccount1);
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccount1);
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getClientPasswordHash(),
                    readUserAccount1.getPasswordHashes().getClientPasswordHash());
            Assertions.assertEquals(false, readUserAccount1.getPasswordResetEmailSent());

            final ReadAccountByIdRequest readAccountByIdRequest2 = new ReadAccountByIdRequest();
            readAccountByIdRequest2.setAppbaseRequesterId(idService.newId());
            readAccountByIdRequest2.setId(idService.newId());
            final ReadAccountByIdResponse readAccountByIdResponse2 =
                    userAccountService.readAccountByIdpUserId(readAccountByIdRequest2);
            Assertions.assertNotNull(readAccountByIdResponse2);
            final UserAccount readUserAccount2 = readAccountByIdResponse2.getUserAccount();
            Assertions.assertNull(readUserAccount2);
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testReadAccountByEmail()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertNotNull(userAccount.getEmail());

            final ReadAccountByIdRequest readAccountByIdRequest1 = new ReadAccountByIdRequest();
            readAccountByIdRequest1.setId(userAccount.getEmail());
            readAccountByIdRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            final ReadAccountByIdResponse readAccountByIdResponse1 =
                    userAccountService.readAccountByEmail(readAccountByIdRequest1);
            final UserAccount readUserAccount1 = readAccountByIdResponse1.getUserAccount();
            Assertions.assertNotNull(readUserAccount1);
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccount1);
            Assertions.assertEquals(
                    afterIdpCreateUserWithEmailPasswordRequest.getClientPasswordHash(),
                    readUserAccount1.getPasswordHashes().getClientPasswordHash());
            Assertions.assertEquals(false, readUserAccount1.getPasswordResetEmailSent());

            final ReadAccountByIdRequest readAccountByIdRequest2 = new ReadAccountByIdRequest();
            readAccountByIdRequest2.setId(idService.newId() + "-g@h.com");
            readAccountByIdRequest2.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            final ReadAccountByIdResponse readAccountByIdResponse2 =
                    userAccountService.readAccountByEmail(readAccountByIdRequest2);
            Assertions.assertNotNull(readAccountByIdResponse2);
            final UserAccount readUserAccount2 = readAccountByIdResponse2.getUserAccount();
            Assertions.assertNull(readUserAccount2);
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testReadSession()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);

            final ReadSessionRequest readSessionRequest = new ReadSessionRequest();
            readSessionRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            readSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            readSessionRequest.setAppbaseUserId(userAccount.getAppbaseUserId());

            final ReadSessionResponse readSessionResponse = userAccountService.readSession(readSessionRequest);
            Assertions.assertNotNull(readSessionResponse);
            final UserSession readUserSession = readSessionResponse.getUserSession();
            Assertions.assertNotNull(readUserSession);

            AppbaseServiceTestHelper.assertUserSession(userSession, readUserSession);
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testIsExpiredSession()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final IsExpiredSessionRequest isExpiredSessionRequest = new IsExpiredSessionRequest();
            isExpiredSessionRequest.setUserSession(userSession);

            final IsExpiredSessionResponse isExpiredSessionResponse =
                    userAccountService.isExpiredSession(isExpiredSessionRequest);
            Assertions.assertNotNull(isExpiredSessionResponse);
            Assertions.assertFalse(isExpiredSessionResponse.isExpired());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testReadGuestUserSession()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest1 = guestSignInRequest();
        final AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1 =
                userAccountService.afterIdpGuestSignIn(afterIdpGuestSignInRequest1);
        Assertions.assertNotNull(afterIdpGuestSignInResponse1);

        try {
            final UserSession userSession1 = afterIdpGuestSignInResponse1.getUserSession();
            Assertions.assertNotNull(userSession1);

            final GuestUser guestUser = userSession1.getGuestUser();
            Assertions.assertNotNull(guestUser);
            final String appbaseUserId = guestUser.getAppbaseUserId();
            Assertions.assertNotNull(appbaseUserId);

            final ReadGuestUserSessionRequest readGuestUserSessionRequest = new ReadGuestUserSessionRequest();
            readGuestUserSessionRequest.setAppbaseSessionId(userSession1.getAppbaseSessionId());
            readGuestUserSessionRequest.setAppbaseUserId(appbaseUserId);
            readGuestUserSessionRequest.setAppbaseRequesterId(appbaseUserId);

            final ReadGuestUserSessionResponse readGuestUserSessionResponse =
                    userAccountService.readGuestUserSession(readGuestUserSessionRequest);
            Assertions.assertNotNull(readGuestUserSessionResponse);
            final UserSession readUserSession = readGuestUserSessionResponse.getUserSession();
            Assertions.assertNotNull(readUserSession);
            AppbaseServiceTestHelper.assertUserSession(userSession1, readUserSession);
        } finally {
            deleteGuestUser(afterIdpGuestSignInResponse1);
        }
    }

    @Test
    void testReadUnauthenticatedSession() throws UserAccountServiceException, UserSessionStoreException {
        UserSession userSession = null;
        final CreateUnauthenticatedSessionResponse unauthenticatedSession;
        try {
            unauthenticatedSession = createUnauthenticatedSession();
            if (unauthenticatedSession != null) {
                userSession = unauthenticatedSession.getUserSession();
                final ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest =
                        new ReadUnauthenticatedSessionRequest();
                readUnauthenticatedSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
                final ReadUnauthenticatedSessionResponse readUnauthenticatedSessionResponse =
                        userAccountService.readUnauthenticatedSession(readUnauthenticatedSessionRequest);
                Assertions.assertNotNull(readUnauthenticatedSessionResponse);

                final UserSession readUserSession = readUnauthenticatedSessionResponse.getUserSession();
                Assertions.assertNotNull(readUserSession);
                AppbaseServiceTestHelper.assertUnauthenticatedUserSession(userSession, readUserSession);
            }
        } finally {
            if (userSession != null) {
                deleteSession(userSession);

            }
        }
    }

    @Test
    void testRenewSession() throws UserAccountServiceException, UserSessionStoreException {
        UserSession userSession = null;

        try {
            final CreateUnauthenticatedSessionResponse unauthenticatedSession = createUnauthenticatedSession();
            userSession = unauthenticatedSession.getUserSession();

            final String oldAppbaseSessionId = userSession.getAppbaseSessionId();

            final RenewSessionRequest renewSessionRequest = new RenewSessionRequest();
            renewSessionRequest.setExpiredUserSession(userSession);

            final RenewSessionResponse renewSessionResponse = userAccountService.renewSession(renewSessionRequest);
            Assertions.assertNotNull(renewSessionResponse);
            final UserSession renewedUserSession = renewSessionResponse.getRenewedUserSession();
            Assertions.assertNotNull(renewedUserSession);
            final String newAppbaseSessionId = renewedUserSession.getAppbaseSessionId();
            Assertions.assertNotNull(newAppbaseSessionId);
            Assertions.assertNotEquals(newAppbaseSessionId, oldAppbaseSessionId);
        } finally {
            if (userSession != null) {
                deleteSession(userSession);

            }
        }
    }

    @Test
    void testEmailPasswordSignIn()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount1 = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount1);

            final AfterIdpEmailPasswordSignInRequest afterIdpEmailPasswordSignInRequest =
                    new AfterIdpEmailPasswordSignInRequest();
            afterIdpEmailPasswordSignInRequest.setAppName("appName-" + idService.newId());
            afterIdpEmailPasswordSignInRequest.setIdpUserId(userAccount1.getIdpUserId());
            afterIdpEmailPasswordSignInRequest.setIdpUserToken(userSession.getIdpUserToken());
            afterIdpEmailPasswordSignInRequest.setAppbaseRequesterId(userAccount1.getIdpUserId());
            afterIdpEmailPasswordSignInRequest.setEmail(userAccount1.getEmail());
            afterIdpEmailPasswordSignInRequest.setClientPasswordHash(
                    userAccount1.getPasswordHashes().getClientPasswordHash());
            afterIdpEmailPasswordSignInRequest.setLanguageCode(userAccount1.getLanguageCode());

            final UserSessionData userSessionData = new UserSessionData();
            userSessionData.setDataMap(Map.of("key1", "value1"));
            afterIdpEmailPasswordSignInRequest.setUserSessionData(userSessionData);

            final AfterIdpEmailPasswordSignInResponse afterIdpEmailPasswordSignInResponse =
                    userAccountService.afterIdpEmailPasswordSignIn(afterIdpEmailPasswordSignInRequest);
            Assertions.assertNotNull(afterIdpEmailPasswordSignInResponse);
            final UserSession userSession1 = afterIdpEmailPasswordSignInResponse.getUserSession();
            Assertions.assertNotNull(userSession1);

            final UserAccount userAccount = userSession1.getUserAccount();
            Assertions.assertNotNull(userAccount);

            Assertions.assertNotEquals(userSession1.getAppbaseSessionId(), userSession.getAppbaseSessionId());
            Assertions.assertEquals(userAccount1.getIdpUserId(), userAccount.getIdpUserId());

            final ReadSessionRequest readRequest = new ReadSessionRequest();
            readRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            readRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            readRequest.setAppbaseSessionId(userSession1.getAppbaseSessionId());
            final UserSession storedSession = userAccountService.readSession(readRequest).getUserSession();
            Assertions.assertNotNull(storedSession);
            AppbaseServiceTestHelper.assertUserSession(userSession1, storedSession);

            final CheckTokenRequest checkRequest = new CheckTokenRequest();
            checkRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            checkRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            checkRequest.setAppbaseSessionId(userSession1.getAppbaseSessionId());
            checkRequest.setIdpUserId(userAccount.getIdpUserId());
            checkRequest.setIdpUserToken(userSession1.getIdpUserToken());
            Assertions.assertNotNull(userAccountService.checkToken(checkRequest).getUserSession());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testSignInWithResetEmailFlagStoresUsableSession() throws Exception {
        assertMaintenanceSignInStoresUsableSession("reset-email-flag");
    }

    @Test
    void testSignInWithResetCodeHashStoresUsableSession() throws Exception {
        assertMaintenanceSignInStoresUsableSession("reset-code-hash");
    }

    @Test
    void testSignInWithChangedClientHashStoresUsableSession() throws Exception {
        assertMaintenanceSignInStoresUsableSession("changed-client-hash");
    }

    private void assertMaintenanceSignInStoresUsableSession(final String trigger) throws Exception {
        final AfterIdpCreateUserWithEmailPasswordRequest createRequest = createUserWithEmailPasswordRequest();
        UserSession originalSession = null;
        try {
            originalSession = userAccountService.afterIdpCreateUserWithEmailPassword(createRequest).getUserSession();
            Assertions.assertNotNull(originalSession);
            final UserAccount account = originalSession.getUserAccount();
            Assertions.assertNotNull(account);

            final PasswordHash changedHash = PasswordHash.builder()
                    .hash("changed-password-" + idService.newId())
                    .salt("salt")
                    .shaRounds(80)
                    .hashAlgorithm(HashAlgorithm.SHA512)
                    .build();
            if (!"changed-client-hash".equals(trigger)) {
                final UpdateAccountRequest maintenanceRequest = new UpdateAccountRequest();
                maintenanceRequest.setAppbaseRequesterId(account.getAppbaseUserId());
                maintenanceRequest.setAppbaseUserId(account.getAppbaseUserId());
                maintenanceRequest.setAppbaseSessionId(originalSession.getAppbaseSessionId());
                if ("reset-email-flag".equals(trigger)) {
                    maintenanceRequest.setPasswordResetEmailSent(true);
                } else {
                    maintenanceRequest.setPasswordResetCodeHash(changedHash);
                }
                Assertions.assertTrue(userAccountService.updateAccount(maintenanceRequest).isUpdated(), trigger);
            }

            final UserAccount beforeSignIn = userAccountService.readAccountByAppbaseUserId(
                    new ReadAccountByIdRequest(idService.newId(), account.getAppbaseUserId(),
                            account.getAppbaseUserId())).getUserAccount();
            Assertions.assertNotNull(beforeSignIn, trigger);
            if ("reset-email-flag".equals(trigger)) {
                Assertions.assertTrue(beforeSignIn.getPasswordResetEmailSent(), trigger);
            } else if ("reset-code-hash".equals(trigger)) {
                Assertions.assertEquals(changedHash, beforeSignIn.getPasswordResetCodeHash(), trigger);
            } else {
                Assertions.assertNotEquals(changedHash,
                        beforeSignIn.getPasswordHashes().getClientPasswordHash(), trigger);
            }

            final AfterIdpEmailPasswordSignInRequest signInRequest = new AfterIdpEmailPasswordSignInRequest();
            signInRequest.setAppName("appName-" + idService.newId());
            signInRequest.setIdpUserId(account.getIdpUserId());
            signInRequest.setIdpUserToken(originalSession.getIdpUserToken());
            signInRequest.setAppbaseRequesterId(account.getIdpUserId());
            signInRequest.setEmail(account.getEmail());
            if ("changed-client-hash".equals(trigger)) {
                signInRequest.setClientPasswordHash(changedHash);
            } else {
                signInRequest.setClientPasswordHash(account.getPasswordHashes().getClientPasswordHash());
            }
            signInRequest.setLanguageCode(account.getLanguageCode());

            final UserSession signedIn = userAccountService.afterIdpEmailPasswordSignIn(signInRequest)
                    .getUserSession();
            Assertions.assertNotNull(signedIn, trigger);
            Assertions.assertNotEquals(originalSession.getAppbaseSessionId(), signedIn.getAppbaseSessionId(), trigger);

            final ReadSessionRequest readRequest = new ReadSessionRequest();
            readRequest.setAppbaseRequesterId(account.getAppbaseUserId());
            readRequest.setAppbaseUserId(account.getAppbaseUserId());
            readRequest.setAppbaseSessionId(signedIn.getAppbaseSessionId());
            final UserSession storedSession = userAccountService.readSession(readRequest).getUserSession();
            Assertions.assertNotNull(storedSession, trigger);
            AppbaseServiceTestHelper.assertUserSession(signedIn, storedSession);

            final CheckTokenRequest checkRequest = new CheckTokenRequest();
            checkRequest.setAppbaseRequesterId(account.getAppbaseUserId());
            checkRequest.setAppbaseUserId(account.getAppbaseUserId());
            checkRequest.setAppbaseSessionId(signedIn.getAppbaseSessionId());
            checkRequest.setIdpUserId(account.getIdpUserId());
            checkRequest.setIdpUserToken(signedIn.getIdpUserToken());
            Assertions.assertNotNull(userAccountService.checkToken(checkRequest).getUserSession(), trigger);

            final UserAccount storedAccount = userAccountService.readAccountByAppbaseUserId(
                    new ReadAccountByIdRequest(idService.newId(), account.getAppbaseUserId(),
                            account.getAppbaseUserId())).getUserAccount();
            Assertions.assertNotNull(storedAccount, trigger);
            if ("reset-email-flag".equals(trigger)) {
                Assertions.assertFalse(storedAccount.getPasswordResetEmailSent(), trigger);
            } else if ("reset-code-hash".equals(trigger)) {
                Assertions.assertNull(storedAccount.getPasswordResetCodeHash(), trigger);
            } else {
                Assertions.assertEquals(changedHash,
                        storedAccount.getPasswordHashes().getClientPasswordHash(), trigger);
            }
        } finally {
            deleteAccount(originalSession);
        }
    }

    @Test
    void testGuestSignIn()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest1 = guestSignInRequest();
        final AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1 =
                userAccountService.afterIdpGuestSignIn(afterIdpGuestSignInRequest1);
        Assertions.assertNotNull(afterIdpGuestSignInResponse1);

        try {
            final UserSession userSession1 = afterIdpGuestSignInResponse1.getUserSession();
            Assertions.assertNotNull(userSession1);

            final GuestUser guestUser1 = userSession1.getGuestUser();
            Assertions.assertNotNull(guestUser1);
            Assertions.assertNull(userSession1.getUserAccount());
            Assertions.assertEquals(afterIdpGuestSignInRequest1.getIdpGuestUserToken(), userSession1.getIdpUserToken());
            AppbaseServiceTestHelper.assertAppNameInSessionData(afterIdpGuestSignInRequest1, userSession1);
            Assertions.assertEquals(afterIdpGuestSignInRequest1.getIdpGuestId(), guestUser1.getIdpGuestId());
            Assertions.assertEquals(afterIdpGuestSignInRequest1.getName(), guestUser1.getName());
            Assertions.assertEquals(afterIdpGuestSignInRequest1.getLanguageCode(), guestUser1.getLanguageCode());
        } finally {
            deleteGuestUser(afterIdpGuestSignInResponse1);
        }
    }

    @Test
    void testRefreshAuthenticationToken()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final String newId = FirebaseAuthEmulatorIdentity.refreshedEmailToken(
                    userSession.getUserAccount().getIdpUserId());
            Assertions.assertNotEquals(newId, userSession.getIdpUserToken());

            final AfterIdpRefreshAuthenticationTokenRequest afterIdpRefreshAuthenticationTokenRequest =
                    new AfterIdpRefreshAuthenticationTokenRequest();
            afterIdpRefreshAuthenticationTokenRequest.setCurrentUserSession(userSession);
            afterIdpRefreshAuthenticationTokenRequest.setNewIdpUserToken(newId);
            afterIdpRefreshAuthenticationTokenRequest.setAppbaseRequesterId(newId);

            final AfterIdpRefreshAuthenticationTokenResponse afterIdpRefreshAuthenticationTokenResponse =
                    userAccountService.afterIdpRefreshAuthenticationToken(afterIdpRefreshAuthenticationTokenRequest);
            Assertions.assertNotNull(afterIdpRefreshAuthenticationTokenResponse);

            final UserSession userSession1 = afterIdpRefreshAuthenticationTokenResponse.getUserSession();
            Assertions.assertNotNull(userSession1);
            Assertions.assertEquals(newId, userSession1.getIdpUserToken());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testCheckToken()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);
            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);

            final CheckTokenRequest checkTokenRequest1 = new CheckTokenRequest();
            checkTokenRequest1.setAppbaseUserId(userAccount.getAppbaseUserId());
            checkTokenRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            checkTokenRequest1.setAppbaseSessionId(userSession.getAppbaseSessionId());
            checkTokenRequest1.setIdpUserToken(userSession.getIdpUserToken());
            checkTokenRequest1.setIdpUserId(userAccount.getIdpUserId());

            final CheckTokenResponse checkTokenResponse1 = userAccountService.checkToken(checkTokenRequest1);
            Assertions.assertNotNull(checkTokenResponse1);
            final UserSession userSession1 = checkTokenResponse1.getUserSession();
            AppbaseServiceTestHelper.assertUserSession(userSession, userSession1);

            final CheckTokenRequest checkTokenRequest2 = new CheckTokenRequest();
            checkTokenRequest2.setAppbaseUserId(userAccount.getAppbaseUserId());
            checkTokenRequest2.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            checkTokenRequest2.setAppbaseSessionId(checkTokenRequest1.getAppbaseSessionId());
            checkTokenRequest2.setIdpUserToken(FirebaseAuthEmulatorIdentity.refreshedEmailToken(
                    userAccount.getIdpUserId()));
            checkTokenRequest2.setIdpUserId(userAccount.getIdpUserId());
            final CheckTokenResponse checkTokenResponse2 = userAccountService.checkToken(checkTokenRequest2);
            Assertions.assertNotNull(checkTokenResponse2);
            Assertions.assertNull(checkTokenResponse2.getUserSession());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testUpdateAccount()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);
            Assertions.assertFalse(userAccount.getEmailVerified());

            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            final UpdateAccountRequest updateAccountRequest = new UpdateAccountRequest();
            updateAccountRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            updateAccountRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            updateAccountRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            updateAccountRequest.setReadAndUpdate(true);
            updateAccountRequest.setEmail(userAccount.getEmail());
            updateAccountRequest.setName(userAccount.getName());
            updateAccountRequest.setClientPasswordHash(
                    PasswordHash
                            .builder()
                            .hash("passwordHash")
                            .salt("salt")
                            .shaRounds(80)
                            .hashAlgorithm(HashAlgorithm.SHA512)
                            .build());
            updateAccountRequest.setPasswordResetEmailSent(userAccount.getPasswordResetEmailSent());
            updateAccountRequest.setPasswordResetCodeHash(
                    PasswordHash
                            .builder()
                            .hash("passwordResetCodeHash")
                            .salt("salt")
                            .shaRounds(80)
                            .hashAlgorithm(HashAlgorithm.SHA512)
                            .build());
            updateAccountRequest.setEmailVerified(true);
            updateAccountRequest.setLanguageCode(userAccount.getLanguageCode());
            updateAccountRequest.setDeleted(userAccount.getDeleted());
            updateAccountRequest.setUserTopic(userAccount.getUserTopic());
            updateAccountRequest.setAddressLine1(userAccount.getAddressLine1());
            updateAccountRequest.setAddressLine2(userAccount.getAddressLine2());
            updateAccountRequest.setCity(userAccount.getCity());
            updateAccountRequest.setCountry(userAccount.getCountry());
            updateAccountRequest.setPostalCode(userAccount.getPostalCode());
            updateAccountRequest.setLatitude(userAccount.getLatitude());
            updateAccountRequest.setLongitude(userAccount.getLongitude());
            updateAccountRequest.setUserHomeDataCenter(userAccount.getUserHomeDataCenter());
            updateAccountRequest.setLastAccessTimestamp(now);

            final UpdateAccountResponse updateAccountResponse = userAccountService.updateAccount(updateAccountRequest);
            Assertions.assertNotNull(updateAccountResponse);
            Assertions.assertTrue(updateAccountResponse.isUpdated());

            final ReadAccountByIdResponse readAccountByIdResponse = userAccountService
                    .readAccountByAppbaseUserId(new ReadAccountByIdRequest(
                            idService.newId(), idService.newId(), userAccount.getAppbaseUserId()));
            Assertions.assertNotNull(readAccountByIdResponse);

            final UserAccount userAccount1 = readAccountByIdResponse.getUserAccount();
            Assertions.assertNotNull(userAccount1);
            Assertions.assertTrue(userAccount1.getEmailVerified());
            AppbaseServiceTestHelper.assertTimestampMinutes(
                    userAccount.getLastAccessTimestamp(), userAccount1.getLastAccessTimestamp());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testPasswordReset()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            final AfterIdpPasswordResetSubmitRequest afterIdpPasswordResetSubmitRequest =
                    new AfterIdpPasswordResetSubmitRequest();
            afterIdpPasswordResetSubmitRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            afterIdpPasswordResetSubmitRequest.setAppbaseRequesterId(idService.newId());
            afterIdpPasswordResetSubmitRequest.setTraceId(idService.newId());

            final AfterIdpPasswordResetSubmitResponse afterIdpPasswordResetSubmitResponse =
                    userAccountService.afterIdpPasswordResetSubmit(afterIdpPasswordResetSubmitRequest);
            Assertions.assertNotNull(afterIdpPasswordResetSubmitResponse);
            Assertions.assertTrue(afterIdpPasswordResetSubmitResponse.isUpdated());

            final ReadAccountByIdResponse readAccountByIdResponse = userAccountService
                    .readAccountByAppbaseUserId(new ReadAccountByIdRequest(
                            idService.newId(), idService.newId(), userAccount.getAppbaseUserId()));
            Assertions.assertNotNull(readAccountByIdResponse);

            final UserAccount userAccount1 = readAccountByIdResponse.getUserAccount();
            Assertions.assertNotNull(userAccount1);
            Assertions.assertTrue(userAccount1.getPasswordResetEmailSent());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testUpdateSession()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);

            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            final UpdateSessionRequest updateSessionRequest = new UpdateSessionRequest();
            updateSessionRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            updateSessionRequest.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            updateSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            updateSessionRequest.setReadAndUpdate(true);
            updateSessionRequest.setAuthenticated(userSession.isAuthenticated());
            updateSessionRequest.setIdpUserToken(userSession.getIdpUserToken());
            updateSessionRequest.setUserSessionData(userSession.getUserSessionData());
            updateSessionRequest.setLastAccessTimestamp(now);

            final UpdateSessionResponse updateSessionResponse = userAccountService.updateSession(updateSessionRequest);
            Assertions.assertNotNull(updateSessionResponse);
            final UserSession userSession1 = updateSessionResponse.getUserSession();
            Assertions.assertNotNull(userSession1);
            Assertions.assertEquals(updateSessionRequest.getIdpUserToken(), userSession1.getIdpUserToken());
            AppbaseServiceTestHelper.assertTimestampMinutes(now, userSession1.getLastAccessTimestamp());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testUpdateGuestSession()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest1 = guestSignInRequest();
        final AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1 =
                userAccountService.afterIdpGuestSignIn(afterIdpGuestSignInRequest1);
        Assertions.assertNotNull(afterIdpGuestSignInResponse1);

        try {
            final UserSession userSession1 = afterIdpGuestSignInResponse1.getUserSession();
            Assertions.assertNotNull(userSession1);

            final GuestUser guestUser = userSession1.getGuestUser();
            Assertions.assertNotNull(guestUser);
            final String appbaseUserId = guestUser.getAppbaseUserId();
            Assertions.assertNotNull(appbaseUserId);

            final UpdateGuestSessionRequest updateGuestSessionRequest = new UpdateGuestSessionRequest();
            updateGuestSessionRequest.setAppbaseSessionId(userSession1.getAppbaseSessionId());
            updateGuestSessionRequest.setAppbaseRequesterId(userSession1.getAppbaseSessionId());
            updateGuestSessionRequest.setUserSessionData(userSession1.getUserSessionData());
            updateGuestSessionRequest.setAppbaseUserId(guestUser.getAppbaseUserId());
            updateGuestSessionRequest.setIdpUserToken(userSession1.getIdpUserToken());
            updateGuestSessionRequest.setName("newName-" + idService.newId());
            updateGuestSessionRequest.setLanguageCode(guestUser.getLanguageCode());
            updateGuestSessionRequest.setLastAccessTimestamp(AppbaseServiceUtil.nowSeconds());
            updateGuestSessionRequest.setReadAndUpdate(true);

            final UpdateGuestSessionResponse updateGuestSessionResponse =
                    userAccountService.updateGuestSession(updateGuestSessionRequest);
            Assertions.assertNotNull(updateGuestSessionResponse);

            final UserSession userSession = updateGuestSessionResponse.getUserSession();
            Assertions.assertNotNull(userSession);
            final GuestUser guestUser1 = userSession.getGuestUser();
            Assertions.assertNotNull(guestUser1);
            Assertions.assertEquals(
                    updateGuestSessionRequest.getLastAccessTimestamp(),
                    guestUser1.getLastAccessTimestamp());
            Assertions.assertEquals(updateGuestSessionRequest.getName(), guestUser1.getName());
        } finally {
            deleteGuestUser(afterIdpGuestSignInResponse1);
        }
    }

    @Test
    void testUpdateUnauthenticatedSession() throws UserAccountServiceException, UserSessionStoreException {
        UserSession userSession = null;
        try {
            final CreateUnauthenticatedSessionResponse unauthenticatedSession = createUnauthenticatedSession();
            userSession = unauthenticatedSession.getUserSession();

            final UpdateUnauthenticatedSessionRequest updateUnauthenticatedSessionRequest =
                    new UpdateUnauthenticatedSessionRequest();
            updateUnauthenticatedSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());

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

            final UpdateUnauthenticatedSessionResponse updateUnauthenticatedSessionResponse =
                    userAccountService.updateUnauthenticatedSession(updateUnauthenticatedSessionRequest);
            Assertions.assertNotNull(updateUnauthenticatedSessionResponse);
            final UserSession updatedUserSession = updateUnauthenticatedSessionResponse.getUserSession();

            final ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest =
                    new ReadUnauthenticatedSessionRequest();
            readUnauthenticatedSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            final ReadUnauthenticatedSessionResponse readUnauthenticatedSessionResponse =
                    userAccountService.readUnauthenticatedSession(readUnauthenticatedSessionRequest);
            Assertions.assertNotNull(readUnauthenticatedSessionResponse);
            final UserSession readUserSession = readUnauthenticatedSessionResponse.getUserSession();
            Assertions.assertNotNull(readUserSession);

            AppbaseServiceTestHelper.assertUnauthenticatedUserSession(updatedUserSession, readUserSession);
        } finally {
            if (userSession != null) {
                deleteSession(userSession);
            }
        }
    }

    @Test
    void testDeleteSession()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount.getAppbaseUserId());

            final ReadSessionRequest readSessionRequest1 = new ReadSessionRequest();
            readSessionRequest1.setAppbaseSessionId(userSession.getAppbaseSessionId());
            readSessionRequest1.setAppbaseRequesterId(userSession.getAppbaseSessionId());
            readSessionRequest1.setAppbaseUserId(userAccount.getAppbaseUserId());
            final ReadSessionResponse readSessionResponse1 = userAccountService.readSession(readSessionRequest1);
            Assertions.assertNotNull(readSessionResponse1);
            final UserSession readUserSession1 = readSessionResponse1.getUserSession();
            AppbaseServiceTestHelper.assertUserSession(userSession, readUserSession1);

            final DeleteSessionRequest deleteSessionRequest = new DeleteSessionRequest();
            deleteSessionRequest.setAppbaseSessionId(userSession.getAppbaseSessionId());
            deleteSessionRequest.setAppbaseUserId(userAccount.getAppbaseUserId());
            deleteSessionRequest.setAppbaseRequesterId(userSession.getAppbaseSessionId());
            final DeleteSessionResponse deleteSessionResponse = userAccountService.deleteSession(deleteSessionRequest);
            Assertions.assertNotNull(deleteSessionResponse);
            Assertions.assertTrue(deleteSessionResponse.isDeleted());

            final ReadSessionResponse readSessionResponse2 = userAccountService.readSession(readSessionRequest1);
            Assertions.assertNotNull(readSessionResponse2);
            Assertions.assertNull(readSessionResponse2.getUserSession());
        } finally {
            deleteAccount(userSession);
        }
    }

    @Test
    void testDeleteAccountByIdpUserId()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserAccount userAccount = null;
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            userAccount = userSession.getUserAccount();
            final String idpUserId = userAccount.getIdpUserId();
            Assertions.assertNotNull(idpUserId);

            final ReadAccountByIdResponse readAccountByIdResponse1 =
                    userAccountService.readAccountByIdpUserId(
                            new ReadAccountByIdRequest(idService.newId(), idService.newId(), idpUserId));
            Assertions.assertNotNull(readAccountByIdResponse1);
            final UserAccount readUserAccount1 = readAccountByIdResponse1.getUserAccount();
            Assertions.assertNotNull(readUserAccount1);
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccount1);

            final DeleteAccountByIdpUserIdRequest deleteAccountByIdpUserIdRequest =
                    new DeleteAccountByIdpUserIdRequest(idService.newId(), idService.newId(), idpUserId);
            final DeleteAccountByIdpUserIdResponse deleteAccountByIdpUserIdResponse =
                    userAccountService.deleteAccountByIdpUserId(deleteAccountByIdpUserIdRequest);
            Assertions.assertNotNull(deleteAccountByIdpUserIdResponse);
            Assertions.assertTrue(deleteAccountByIdpUserIdResponse.isDeleted());

            final ReadAccountByIdResponse readAccountByIdResponse2 = userAccountService
                    .readAccountByAppbaseUserId(
                            new ReadAccountByIdRequest(idService.newId(), idService.newId(), idpUserId));
            Assertions.assertNotNull(readAccountByIdResponse2);
            final UserAccount readUserAccount2 = readAccountByIdResponse2.getUserAccount();
            Assertions.assertNull(readUserAccount2);
        } finally {
            if (userSession != null && userAccount != null) {
                final ExistsAccountByIdRequest existsAccountByIdRequest =
                        new ExistsAccountByIdRequest(idService.newId(), idService.newId(), userAccount.getIdpUserId());
                if (userAccountService.existsAccountByIdpUserId(existsAccountByIdRequest).isExists()) {
                    deleteAccount(userSession);
                }
            }
        }
    }

    @Test
    void testDeleteAccountByAppbaseUserId()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserAccount userAccount = null;
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);

            userAccount = userSession.getUserAccount();
            final String appbaseUserId = userAccount.getAppbaseUserId();
            Assertions.assertNotNull(appbaseUserId);

            final ReadAccountByIdResponse readAccountByIdResponse1 =
                    userAccountService.readAccountByAppbaseUserId(
                            new ReadAccountByIdRequest(idService.newId(), idService.newId(), appbaseUserId));
            Assertions.assertNotNull(readAccountByIdResponse1);
            final UserAccount readUserAccount1 = readAccountByIdResponse1.getUserAccount();
            Assertions.assertNotNull(readUserAccount1);
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccount1);

            final DeleteAccountByAppbaseUserIdRequest deleteAccountByAppbaseUserIdRequest =
                    new DeleteAccountByAppbaseUserIdRequest();
            deleteAccountByAppbaseUserIdRequest.setAppbaseUserId(appbaseUserId);
            deleteAccountByAppbaseUserIdRequest.setAppbaseRequesterId(appbaseUserId);
            final DeleteAccountByAppbaseUserIdResponse deleteAccountByAppbaseUserIdResponse =
                    userAccountService.deleteAccountByAppbaseUserId(deleteAccountByAppbaseUserIdRequest);
            Assertions.assertNotNull(deleteAccountByAppbaseUserIdResponse);
            Assertions.assertTrue(deleteAccountByAppbaseUserIdResponse.isDeleted());

            final ReadAccountByIdResponse readAccountByIdResponse2 =
                    userAccountService.readAccountByAppbaseUserId(
                            new ReadAccountByIdRequest(idService.newId(), idService.newId(), appbaseUserId));
            Assertions.assertNotNull(readAccountByIdResponse2);
            final UserAccount readUserAccount2 = readAccountByIdResponse2.getUserAccount();
            Assertions.assertNull(readUserAccount2);
        } finally {
            if (userSession != null && userAccount != null) {
                final ExistsAccountByIdRequest existsAccountByIdRequest =
                        new ExistsAccountByIdRequest(
                                idService.newId(), idService.newId(), userAccount.getAppbaseUserId());
                if (userAccountService.existsAccountByAppbaseUserId(existsAccountByIdRequest).isExists()) {
                    deleteAccount(userSession);
                }
            }
        }
    }

    @Test
    void testDeleteGuestUser()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest1 = guestSignInRequest();
        final AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1 =
                userAccountService.afterIdpGuestSignIn(afterIdpGuestSignInRequest1);
        Assertions.assertNotNull(afterIdpGuestSignInResponse1);

        try {
            final UserSession userSession1 = afterIdpGuestSignInResponse1.getUserSession();
            Assertions.assertNotNull(userSession1);

            final GuestUser guestUser = userSession1.getGuestUser();
            Assertions.assertNotNull(guestUser);
            final String appbaseUserId = guestUser.getAppbaseUserId();
            Assertions.assertNotNull(appbaseUserId);

            final DeleteGuestUserRequest deleteGuestUserRequest = new DeleteGuestUserRequest();
            deleteGuestUserRequest.setAppbaseUserId(appbaseUserId);
            deleteGuestUserRequest.setAppbaseRequesterId(appbaseUserId);
            final DeleteGuestUserResponse deleteGuestUserResponse =
                    userAccountService.deleteGuestUser(deleteGuestUserRequest);
            Assertions.assertNotNull(deleteGuestUserResponse);
            Assertions.assertTrue(deleteGuestUserResponse.isDeleted());
        } finally {
            deleteGuestUser(afterIdpGuestSignInResponse1);
        }
    }

    @Test
    void testDeleteUnauthenticatedSession() throws UserAccountServiceException, UserSessionStoreException {
        UserSession userSession = null;
        try {
            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    createUnauthenticatedSession();
            if (createUnauthenticatedSessionResponse != null) {
                userSession = createUnauthenticatedSessionResponse.getUserSession();
                Assertions.assertNotNull(userSession);
                Assertions.assertNotNull(userSession.getAppbaseSessionId());

                final DeleteUnauthenticatedSessionRequest deleteUnauthenticatedSessionRequest1 =
                        new DeleteUnauthenticatedSessionRequest();
                deleteUnauthenticatedSessionRequest1.setAppbaseSessionId(userSession.getAppbaseSessionId());
                final DeleteUnauthenticatedSessionResponse deleteUnauthenticatedSessionResponse1 =
                        userAccountService.deleteUnauthenticatedSession(deleteUnauthenticatedSessionRequest1);
                Assertions.assertNotNull(deleteUnauthenticatedSessionResponse1);
                Assertions.assertTrue(deleteUnauthenticatedSessionResponse1.isDeleted());

                final DeleteUnauthenticatedSessionRequest deleteUnauthenticatedSessionRequest2 =
                        new DeleteUnauthenticatedSessionRequest();
                deleteUnauthenticatedSessionRequest2.setAppbaseSessionId(idService.newId());
                final DeleteUnauthenticatedSessionResponse deleteUnauthenticatedSessionResponse2 =
                        userAccountService.deleteUnauthenticatedSession(deleteUnauthenticatedSessionRequest2);
                Assertions.assertNotNull(deleteUnauthenticatedSessionResponse2);
                Assertions.assertFalse(deleteUnauthenticatedSessionResponse2.isDeleted());
            }
        } finally {
            if (userSession != null) {
                deleteSession(userSession);
            }
        }
    }

    @Test
    void testSignOutRequest()
            throws UserAccountServiceException, AuthorizationServiceException, AuthenticationServiceException
    {
        final AfterIdpCreateUserWithEmailPasswordRequest
                afterIdpCreateUserWithEmailPasswordRequest = createUserWithEmailPasswordRequest();

        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            Assertions.assertNotNull(userSession);
            final UserAccount userAccount = userSession.getUserAccount();
            Assertions.assertNotNull(userAccount);

            final AfterIdpSignOutRequest afterIdpSignOutRequest1 = new AfterIdpSignOutRequest();
            afterIdpSignOutRequest1.setAppbaseUserId(userAccount.getAppbaseUserId());
            afterIdpSignOutRequest1.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            afterIdpSignOutRequest1.setAppbaseSessionId(userSession.getAppbaseSessionId());
            afterIdpSignOutRequest1.setIdpUserToken(userSession.getIdpUserToken());
            afterIdpSignOutRequest1.setIdpUserId(userAccount.getIdpUserId());

            final AfterIdpSignOutResponse afterIdpSignOutResponse1 =
                    userAccountService.afterIdpSignOut(afterIdpSignOutRequest1);
            Assertions.assertNotNull(afterIdpSignOutResponse1);
            Assertions.assertTrue(afterIdpSignOutResponse1.isSignedOut());

            final AfterIdpSignOutRequest afterIdpSignOutRequest2 = new AfterIdpSignOutRequest();
            afterIdpSignOutRequest2.setAppbaseUserId(userAccount.getAppbaseUserId());
            afterIdpSignOutRequest2.setAppbaseRequesterId(userAccount.getAppbaseUserId());
            afterIdpSignOutRequest2.setAppbaseSessionId(idService.newId());
            afterIdpSignOutRequest2.setIdpUserToken(userSession.getIdpUserToken());
            afterIdpSignOutRequest2.setIdpUserId(userAccount.getIdpUserId());

            final AfterIdpSignOutResponse afterIdpSignOutResponse2 =
                    userAccountService.afterIdpSignOut(afterIdpSignOutRequest2);
            Assertions.assertNotNull(afterIdpSignOutResponse2);
            Assertions.assertFalse(afterIdpSignOutResponse2.isSignedOut());
        } finally {
            deleteAccount(userSession);
        }
    }

    private AfterIdpCreateUserWithEmailPasswordRequest createUserWithEmailPasswordRequest() {
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
                        .hash("passwordHash")
                        .salt("salt")
                        .shaRounds(80)
                        .hashAlgorithm(HashAlgorithm.SHA512)
                        .build());
        afterIdpCreateUserWithEmailPasswordRequest.setLanguageCode(Locale.ENGLISH.getLanguage());
        afterIdpCreateUserWithEmailPasswordRequest.setAppName("appName-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setAddressLine1("Address line 1-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setAddressLine2("Address line 2-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setCity("City-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setCountry("Country-" + id);
        afterIdpCreateUserWithEmailPasswordRequest.setPostalCode(
                "PostalCode-" + id.substring(0, Math.min(20, id.length())));
        afterIdpCreateUserWithEmailPasswordRequest.setLatitude("24." + id);
        afterIdpCreateUserWithEmailPasswordRequest.setLongitude("9." + id);

        return afterIdpCreateUserWithEmailPasswordRequest;
    }

    private CreateUnauthenticatedSessionResponse createUnauthenticatedSession() throws UserAccountServiceException {
        final CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest =
                new CreateUnauthenticatedSessionRequest();
        final UserSessionData userSessionData = new UserSessionData();
        final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
        userSessionData.setDataMap(Map.of("k1", now.toString()));
        createUnauthenticatedSessionRequest.setUserSessionData(userSessionData);
        createUnauthenticatedSessionRequest.setCreationTimestamp(now);
        createUnauthenticatedSessionRequest.setLastAccessTimestamp(now);

        return userAccountService.createUnauthenticatedSession(createUnauthenticatedSessionRequest);
    }

    private AfterIdpGuestSignInRequest guestSignInRequest() {
        final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest = new AfterIdpGuestSignInRequest();
        final FirebaseAuthEmulatorIdentity.Identity identity = FirebaseAuthEmulatorIdentity.createGuestUser();
        final String id = identity.uid();
        afterIdpGuestSignInRequest.setIdpGuestId(id);
        afterIdpGuestSignInRequest.setAppbaseRequesterId(id);
        afterIdpGuestSignInRequest.setIdpGuestUserToken(identity.idToken());
        afterIdpGuestSignInRequest.setAppName("appName-" + id);
        afterIdpGuestSignInRequest.setName("name-" + id);
        afterIdpGuestSignInRequest.setLanguageCode(Locale.ENGLISH.getLanguage());
        afterIdpGuestSignInRequest.setUserSessionData(new UserSessionData(Map.of("key-" + id, "value-" + id)));
        afterIdpGuestSignInRequest.setAddressLine1("Address line 1-" + id);
        afterIdpGuestSignInRequest.setAddressLine2("Address line 2-" + id);
        afterIdpGuestSignInRequest.setCity("City-" + id);
        afterIdpGuestSignInRequest.setCountry("Country-" + id);
        afterIdpGuestSignInRequest.setPostalCode("PostalCode-" + id.substring(0, Math.min(20, id.length())));
        afterIdpGuestSignInRequest.setLatitude("24." + id);
        afterIdpGuestSignInRequest.setLongitude("9." + id);

        return afterIdpGuestSignInRequest;
    }

    private void deleteSession(final UserSession userSession) throws UserSessionStoreException {
        if (userSession != null) {
            userSessionStore.deleteAllSessions(userSession.getAppbaseSessionId());
        }
    }

    private void deleteAccount(final UserSession userSession)
            throws UserAccountServiceException, AuthorizationServiceException
    {
        if (userSession != null) {
            final DeleteAccountByAppbaseUserIdRequest deleteAccountByAppbaseUserIdRequest =
                    new DeleteAccountByAppbaseUserIdRequest();
            final String appbaseUserId = userSession.getUserAccount().getAppbaseUserId();
            deleteAccountByAppbaseUserIdRequest.setAppbaseRequesterId(appbaseUserId);
            deleteAccountByAppbaseUserIdRequest.setAppbaseUserId(appbaseUserId);
            userAccountService.deleteAccountByAppbaseUserId(deleteAccountByAppbaseUserIdRequest);
        }
    }

    private void deleteGuestUser(final AfterIdpGuestSignInResponse afterIdpGuestSignInResponse1)
            throws UserAccountServiceException, AuthorizationServiceException
    {
        if (afterIdpGuestSignInResponse1 != null) {
            final UserSession userSession = afterIdpGuestSignInResponse1.getUserSession();
            if (userSession != null) {
                final UserAccount userAccount = userSession.getUserAccount();
                if (userAccount != null) {
                    final String appbaseUserId = userAccount.getAppbaseUserId();
                    if (AppbaseUtil.isNotBlank(appbaseUserId)) {
                        userAccountService.deleteGuestUser(
                                new DeleteGuestUserRequest(idService.newId(), idService.newId(), appbaseUserId));
                    }
                }
            }
        }
    }
}
