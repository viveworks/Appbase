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

package com.viveworks.appbase.service.user.session.store;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientTestHelper;
import com.viveworks.appbase.client.service.aerospike.DefaultAerospikeClientService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.user.testhelper.AppbaseServiceTestHelper;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "AerospikeClientServiceTests.enabled", matches = "true")
class DefaultUserSessionStoreTest {
    private static AppbaseCore appbaseCore;
    private static DefaultAerospikeClientService defaultAerospikeClientService;
    private static IdService idService;
    private static UserSessionStore userSessionStore;

    @BeforeAll
    static void setUp() {
        System.setProperty(
                ClientCoreTestHelper.CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-core.yaml");
        System.setProperty(
                ClientCoreTestHelper.CLIENT_CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-client-core.yaml");
        System.setProperty(
                AerospikeClientTestHelper.AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-aerospike.yaml");

        appbaseCore = AerospikeClientTestHelper.appbaseCore();
        idService = new DefaultIdService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultIdServiceConfiguration.builder().build());
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
    }

    @AfterAll
    static void tearDown() {
        if (defaultAerospikeClientService != null) {
            defaultAerospikeClientService.shutdown();
        }
    }

    @Test
    void testRegisterService() throws InterruptedException {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();

        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(UserSessionStore.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                UserSessionStore.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(userSessionStore, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 =
                serviceRegistry.getAppbaseService(DefaultUserSessionStore.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(userSessionStore, appbaseServiceEntry2.getService());
    }

    @Test
    void testCurrentTime() throws InterruptedException {
        final LocalDateTime currentTime1 = userSessionStore.currentTime();
        Assertions.assertNotNull(currentTime1);
        Thread.sleep(2000);

        final LocalDateTime currentTime2 = userSessionStore.currentTime();
        Assertions.assertNotNull(currentTime2);

        Assertions.assertNotEquals(currentTime1, currentTime2);
    }

    @Test
    void testGenerateSessionId() {
        final String sessionId1 = userSessionStore.generateSessionId();
        Assertions.assertNotNull(sessionId1);

        final String sessionId2 = userSessionStore.generateSessionId();
        Assertions.assertNotNull(sessionId2);

        Assertions.assertNotEquals(sessionId1, sessionId2);
    }

    @Test
    void testIsExpired() {
        final UserSession userSession = new UserSession();
        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC).minus(1L, ChronoUnit.WEEKS));
        final boolean expired1 = userSessionStore.isExpired(userSession);
        Assertions.assertTrue(expired1);

        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        final boolean expired2 = userSessionStore.isExpired(userSession);
        Assertions.assertFalse(expired2);
    }

    @Test
    void testIsNotExpired() {
        final UserSession userSession = new UserSession();
        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC).minus(1L, ChronoUnit.WEEKS));
        final boolean expired1 = userSessionStore.isNotExpired(userSession);
        Assertions.assertFalse(expired1);

        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        final boolean expired2 = userSessionStore.isNotExpired(userSession);
        Assertions.assertTrue(expired2);
    }

    @Test
    void testCreateSession() {
        final UserSession userSession1 = userSession(idService.newId());

        try {
            userSessionStore.createSession(userSession1);
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession1);
        }


        Assertions.assertThrows(UserSessionStoreException.class, () -> {
            userSession1.setUserAccount(null);
            userSessionStore.createSession(userSession1);
        });


        final UserSession userSession2 = userSession(null);
        Assertions.assertThrows(UserSessionStoreException.class, () -> userSessionStore.createSession(userSession2));
    }

    @Test
    void testUpdateSession() {
        final UserSession userSession1 = userSession(idService.newId());

        try {
            userSessionStore.createSession(userSession1);

            final LocalDateTime updatedLastAccessTime = LocalDateTime
                    .now(ZoneOffset.UTC)
                    .minusMinutes(8)
                    .minusSeconds(20);
            userSession1.setLastAccessTimestamp(updatedLastAccessTime);
            userSessionStore.updateSession(userSession1);

            final Optional<UserSession> userSession2 = userSessionStore
                    .readSession(userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId());
            Assertions.assertNotNull(userSession2);
            Assertions.assertTrue(userSession2.isPresent());
            AppbaseServiceTestHelper.assertUserSession(userSession1, userSession2.get());
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession1);
        }
    }

    @Test
    void testCreateOrUpdateSession() {
        final UserSession userSession1 = userSession(idService.newId());

        try {
            userSessionStore.createOrUpdateSession(userSession1);
            final Optional<UserSession> userSession2 = userSessionStore
                    .readSession(userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId());
            Assertions.assertNotNull(userSession2);
            Assertions.assertTrue(userSession2.isPresent());
            AppbaseServiceTestHelper.assertUserSession(userSession1, userSession2.get());

            final LocalDateTime updatedLastAccessTime = LocalDateTime
                    .now(ZoneOffset.UTC)
                    .minusMinutes(8)
                    .minusSeconds(20);
            userSession1.setLastAccessTimestamp(updatedLastAccessTime);
            userSessionStore.createOrUpdateSession(userSession1);

            final Optional<UserSession> userSession3 = userSessionStore
                    .readSession(userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId());
            Assertions.assertNotNull(userSession3);
            Assertions.assertTrue(userSession3.isPresent());
            AppbaseServiceTestHelper.assertUserSession(userSession1, userSession3.get());
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession1);
        }
    }

    @Test
    void testExistsSession() {
        final UserSession userSession1 = userSession(idService.newId());

        try {
            userSessionStore.createSession(userSession1);

            final boolean existsSession1 = userSessionStore
                    .existsSession(userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId());
            Assertions.assertTrue(existsSession1);

            final boolean existsSession2 = userSessionStore
                    .existsSession(userSession1.getUserAccount().getAppbaseUserId(), userSessionStore.generateSessionId());
            Assertions.assertFalse(existsSession2);

            final boolean existsSession3 = userSessionStore
                    .existsSession(idService.newId(), userSessionStore.generateSessionId());
            Assertions.assertFalse(existsSession3);

            Assertions.assertTrue(userSessionStore.deleteAllSessions(userSession1.getUserAccount().getAppbaseUserId()));
            final boolean existsSession4 = userSessionStore
                    .existsSession(userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId());
            Assertions.assertFalse(existsSession4);
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession1);
        }
    }

    @Test
    void testExistsUnauthenticatedSession() {
        final UserSession userSession = unauthenticatedSession();

        try {
            userSessionStore.createSession(userSession);

            final boolean exists1 = userSessionStore.existsUnauthenticatedSession(userSession.getAppbaseSessionId());
            Assertions.assertTrue(exists1);

            Assertions.assertTrue(userSessionStore.deleteAllSessions(userSession.getAppbaseSessionId()));

            final boolean exists2 = userSessionStore.existsUnauthenticatedSession(userSession.getAppbaseSessionId());
            Assertions.assertFalse(exists2);

            final boolean exists3 = userSessionStore.existsUnauthenticatedSession(userSessionStore.generateSessionId());
            Assertions.assertFalse(exists3);
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession);
        }
    }

    @Test
    void testExistsSessions() {
        final String appbaseUserId = idService.newId();
        final UserSession userSession1 = userSession(appbaseUserId);
        final UserSession userSession2 = userSession(appbaseUserId);

        try {
            userSessionStore.createSession(userSession1);
            userSessionStore.createSession(userSession2);

            final boolean exists1 = userSessionStore.existsSessions(appbaseUserId);
            Assertions.assertTrue(exists1);

            Assertions.assertTrue(userSessionStore.deleteAllSessions(appbaseUserId));

            final boolean exists2 = userSessionStore.existsSessions(appbaseUserId);
            Assertions.assertFalse(exists2);

            final boolean exists3 = userSessionStore.existsSessions(idService.newId());
            Assertions.assertFalse(exists3);
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession1);
            deleteAllSessions(userSession2);
        }
    }

    @Test
    void testReadSession() {
        final UserSession userSession = userSession(idService.newId());

        try {
            userSessionStore.createSession(userSession);

            final Optional<UserSession> userSession1 = userSessionStore
                    .readSession(userSession.getUserAccount().getAppbaseUserId(), userSession.getAppbaseSessionId());
            Assertions.assertNotNull(userSession1);
            Assertions.assertTrue(userSession1.isPresent());
            AppbaseServiceTestHelper.assertUserSession(userSession, userSession1.get());

            Assertions.assertTrue(
                    userSessionStore.deleteSession(
                            userSession.getUserAccount().getAppbaseUserId(), userSession.getAppbaseSessionId()));

            final Optional<UserSession> userSession2 = userSessionStore
                    .readSession(userSession.getUserAccount().getAppbaseUserId(), userSession.getAppbaseSessionId());
            Assertions.assertNotNull(userSession2);
            Assertions.assertTrue(userSession2.isEmpty());
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession);
        }
    }

    @Test
    void testReadUnauthenticatedSession() {
        final UserSession userSession = unauthenticatedSession();

        try {
            userSessionStore.createSession(userSession);

            final Optional<UserSession> userSession1 =
                    userSessionStore.readUnauthenticatedSession(userSession.getAppbaseSessionId());
            Assertions.assertNotNull(userSession1);
            Assertions.assertTrue(userSession1.isPresent());
            Assertions.assertEquals(userSession, userSession1.get());

            Assertions.assertTrue(userSessionStore.deleteUnauthenticatedSession(userSession.getAppbaseSessionId()));

            final Optional<UserSession> userSession2 =
                    userSessionStore.readUnauthenticatedSession(userSession.getAppbaseSessionId());
            Assertions.assertNotNull(userSession2);
            Assertions.assertTrue(userSession2.isEmpty());
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession);
        }
    }

    @Test
    void testReadAllSessions() {
        final String appbaseUserId = idService.newId();
        final UserSession userSession1 = userSession(appbaseUserId);
        final UserSession userSession2 = userSession(appbaseUserId);

        try {
            userSessionStore.createSession(userSession1);
            userSessionStore.createSession(userSession2);

            final Optional<Collection<UserSession>> userSessions = userSessionStore.readAllSessions(appbaseUserId);
            Assertions.assertNotNull(userSessions);
            Assertions.assertTrue(userSessions.isPresent());

            boolean containsUserSession1 = false;
            boolean containsUserSession2 = false;
            for (final UserSession userSession : userSessions.get()) {
                if (equalUserSessions(userSession, userSession1)) {
                    containsUserSession1 = true;
                } else if (equalUserSessions(userSession, userSession2)) {
                    containsUserSession2 = true;
                }
            }
            Assertions.assertTrue(containsUserSession1);
            Assertions.assertTrue(containsUserSession2);

            Assertions.assertTrue(userSessionStore.deleteAllSessions(appbaseUserId));

            final Optional<Collection<UserSession>> userSessions1 = userSessionStore.readAllSessions(appbaseUserId);
            Assertions.assertNotNull(userSessions1);
            Assertions.assertTrue(userSessions1.isEmpty());
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession1);
            deleteAllSessions(userSession2);
        }
    }

    private boolean equalUserSessions(final UserSession userSession1, final UserSession userSession2) {
        final UserAccount userAccount1 = userSession1.getUserAccount();
        final UserAccount userAccount2 = userSession2.getUserAccount();
        if (!Objects.equals(userAccount1.getAppbaseUserId(), userAccount2.getAppbaseUserId()) ||
                !Objects.equals(userAccount1.getIdpUserId(), userAccount2.getIdpUserId()) ||
                !Objects.equals(userAccount1.getEmailVerified(), userAccount2.getEmailVerified()) ||
                !Objects.equals(userAccount1.getEmail(), userAccount2.getEmail()) ||
                !Objects.equals(userAccount1.getName(), userAccount2.getName()) ||
                !Objects.equals(userAccount1.getCreationTimestamp(), userAccount2.getCreationTimestamp()) ||
                !Objects.equals(userAccount1.getLastAccessTimestamp(), userAccount2.getLastAccessTimestamp()) ||
                !Objects.equals(userAccount1.getDeleted(), userAccount2.getDeleted()) ||
                !Objects.equals(userAccount1.getLanguageCode(), userAccount2.getLanguageCode()) ||
                !Objects.equals(userAccount1.getAddressLine1(), userAccount2.getAddressLine1()) ||
                !Objects.equals(userAccount1.getAddressLine2(), userAccount2.getAddressLine2()) ||
                !Objects.equals(userAccount1.getCity(), userAccount2.getCity()) ||
                !Objects.equals(userAccount1.getCountry(), userAccount2.getCountry()) ||
                !Objects.equals(userAccount1.getStateProvince(), userAccount2.getStateProvince()) ||
                !Objects.equals(userAccount1.getPostalCode(), userAccount2.getPostalCode()) ||
                !Objects.equals(userAccount1.getLatitude(), userAccount2.getLatitude()) ||
                !Objects.equals(userAccount1.getLongitude(), userAccount2.getLongitude()) ||
                !Objects.equals(userAccount1.getUserHomeDataCenter(), userAccount2.getUserHomeDataCenter()) ||
                !Objects.equals(userAccount1.getUserTopic(), userAccount2.getUserTopic()))
        {
            return false;
        }

        return userSession1.isAuthenticated() == userSession2.isAuthenticated() &&
                userSession1.isExpired() == userSession2.isExpired() &&
                Objects.equals(userSession1.getAppbaseSessionId(), userSession2.getAppbaseSessionId()) &&
                Objects.equals(userSession1.getIdpUserToken(), userSession2.getIdpUserToken()) &&
                Objects.equals(userSession1.getGuestUser(), userSession2.getGuestUser()) &&
                Objects.equals(userSession1.getUserSessionData(), userSession2.getUserSessionData()) &&
                Objects.equals(userSession1.getCreationTimestamp(), userSession2.getCreationTimestamp()) &&
                Objects.equals(userSession1.getLastAccessTimestamp(), userSession2.getLastAccessTimestamp());
    }

    @Test
    void testDeleteSession() {
        final UserSession userSession = userSession(idService.newId());

        final String appbaseUserId = idService.newId();
        final UserSession userSession1 = userSession(appbaseUserId);
        final UserSession userSession2 = userSession(appbaseUserId);

        try {
            userSessionStore.createSession(userSession);
            Assertions.assertTrue(userSessionStore.existsSession(
                    userSession.getUserAccount().getAppbaseUserId(), userSession.getAppbaseSessionId()));
            Assertions.assertTrue(
                    userSessionStore.deleteSession(
                            userSession.getUserAccount().getAppbaseUserId(), userSession.getAppbaseSessionId()));
            Assertions.assertFalse(userSessionStore
                    .existsSession(userSession.getUserAccount().getAppbaseUserId(), userSession.getAppbaseSessionId()));
            Assertions.assertFalse(
                    userSessionStore.deleteSession(
                            userSession.getUserAccount().getAppbaseUserId(), userSession.getAppbaseSessionId()));


            userSessionStore.createSession(userSession1);
            userSessionStore.createSession(userSession2);
            Assertions.assertEquals(
                    userSession1.getUserAccount().getAppbaseUserId(), userSession2.getUserAccount().getAppbaseUserId());
            Assertions.assertTrue(userSessionStore.existsSession(
                    userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId()));
            Assertions.assertTrue(userSessionStore.existsSession(
                    userSession2.getUserAccount().getAppbaseUserId(), userSession2.getAppbaseSessionId()));
            Assertions.assertTrue(userSessionStore.existsSessions(userSession1.getUserAccount().getAppbaseUserId()));

            Assertions.assertTrue(
                    userSessionStore.deleteSession(
                            userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId()));
            Assertions.assertFalse(userSessionStore.existsSession(
                    userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId()));
            Assertions.assertFalse(
                    userSessionStore.deleteSession(
                            userSession1.getUserAccount().getAppbaseUserId(), userSession1.getAppbaseSessionId()));

            Assertions.assertTrue(
                    userSessionStore.deleteSession(
                            userSession2.getUserAccount().getAppbaseUserId(), userSession2.getAppbaseSessionId()));
            Assertions.assertFalse(userSessionStore.existsSession(
                    userSession2.getUserAccount().getAppbaseUserId(), userSession2.getAppbaseSessionId()));
            Assertions.assertFalse(userSessionStore.existsSessions(userSession1.getUserAccount().getAppbaseUserId()));
            Assertions.assertFalse(
                    userSessionStore.deleteSession(
                            userSession2.getUserAccount().getAppbaseUserId(), userSession2.getAppbaseSessionId()));
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession);
            deleteAllSessions(userSession1);
            deleteAllSessions(userSession2);
        }
    }

    @Test
    void testDeleteUnauthenticatedSession() {
        final UserSession userSession = unauthenticatedSession();

        try {
            userSessionStore.createSession(userSession);
            Assertions.assertTrue(userSessionStore.existsUnauthenticatedSession(userSession.getAppbaseSessionId()));

            Assertions.assertTrue(userSessionStore.deleteUnauthenticatedSession(userSession.getAppbaseSessionId()));
            Assertions.assertFalse(userSessionStore.existsUnauthenticatedSession(userSession.getAppbaseSessionId()));
            Assertions.assertFalse(userSessionStore.deleteUnauthenticatedSession(userSession.getAppbaseSessionId()));
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession);
        }
    }

    @Test
    void testDeleteAllSessions() {
        final String appbaseUserId = idService.newId();
        final UserSession userSession1 = userSession(appbaseUserId);
        final UserSession userSession2 = userSession(appbaseUserId);

        try {
            userSessionStore.createSession(userSession1);
            userSessionStore.createSession(userSession2);
            Assertions.assertTrue(userSessionStore.existsSessions(appbaseUserId));
            Assertions.assertTrue(userSessionStore.deleteAllSessions(appbaseUserId));
            Assertions.assertFalse(userSessionStore.existsSessions(appbaseUserId));
            Assertions.assertFalse(userSessionStore.deleteAllSessions(appbaseUserId));
        } catch (UserSessionStoreException e) {
            Assertions.fail(e);
        } finally {
            deleteAllSessions(userSession1);
            deleteAllSessions(userSession2);
        }
    }

    private UserSession unauthenticatedSession() {
        final UserSession userSession = new UserSession();
        userSession.setAuthenticated(false);
        userSession.setAppbaseSessionId(userSessionStore.generateSessionId());
        userSession.setCreationTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(15));
        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10));

        final UserSessionData userSessionData = new UserSessionData();
        userSessionData.setDataMap(Map.of("key1", "value1"));
        userSession.setUserSessionData(userSessionData);

        return userSession;
    }

    private UserSession userSession(final String appbaseUserId) {
        final UserSession userSession = new UserSession();
        userSession.setAuthenticated(true);
        userSession.setAppbaseSessionId(userSessionStore.generateSessionId());
        userSession.setCreationTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(15));
        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10));

        final UserSessionData userSessionData = new UserSessionData();
        userSessionData.setDataMap(Map.of("key1", "value1"));
        userSession.setUserSessionData(userSessionData);

        final UserAccount userAccount = userAccount(appbaseUserId);
        userSession.setUserAccount(userAccount);

        return userSession;
    }

    private UserAccount userAccount(final String appbaseUserId) {
        final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
        return UserAccount
                .builder()
                .appbaseUserId(appbaseUserId)
                .idpUserId(idService.newId())
                .name("name1")
                .email("abc@domain1.com")
                .passwordHashes(passwordHashes())
                .languageCode("EN_CA")
                .userHomeDataCenter("east1")
                .userTopic("name1")
                .emailVerified(true)
                .addressLine1("123 Abcd Street")
                .addressLine2("Line 1 of address")
                .city("My city")
                .postalCode("1A2 3B6")
                .stateProvince("My state")
                .country("My country")
                .latitude("1.23")
                .longitude("12.234")
                .creationTimestamp(now)
                .lastAccessTimestamp(now)
                .build();
    }

    private PasswordHashes passwordHashes() {
        return PasswordHashes
                .builder()
                .clientPasswordHash(
                        PasswordHash
                                .builder()
                                .hash("clientPasswordHash-123")
                                .hashAlgorithm(HashAlgorithm.SHA512)
                                .shaRounds(80)
                                .salt("salt123")
                                .build())
                .serverPasswordHash(
                        PasswordHash
                                .builder()
                                .hash("serverPasswordHash-123")
                                .hashAlgorithm(HashAlgorithm.SHA512)
                                .shaRounds(80)
                                .salt("salt123")
                                .build())
                .build();
    }

    private void deleteAllSessions(final UserSession userSession) {
        final UserAccount userAccount = userSession.getUserAccount();
        if (userAccount != null && !AppbaseUtil.isBlank(userAccount.getAppbaseUserId())) {
            try {
                userSessionStore.deleteAllSessions(userAccount.getAppbaseUserId());
            } catch (UserSessionStoreException e) {
                Assertions.fail(e);
            }
        }
    }
}
