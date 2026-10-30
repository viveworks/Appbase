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
package com.viveworks.appbase.service.user.account.store;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
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
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.DefaultPasswordService;
import com.viveworks.appbase.service.password.DefaultPasswordServiceConfiguration;
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Reclassified from {@code JdbiUserAccountStoreTest} and gated by P-051 · E1135 · N11 (internal work item 1301): this class
 * opens a live JDBI {@code Handle} against the vtgate declared by
 * {@code appbase-service-vitess-jdbc.yaml} ({@code localhost:15306}), so under its former
 * {@code *Test} name Surefire ran it on every ordinary offline build and it failed on
 * connection-refused.
 *
 * <p>The {@code *IT} name moves it off Surefire and onto the Failsafe opt-in this module now
 * declares; the gate below keeps it skipped until a live vtgate is explicitly opted into with
 * {@code -DJdbiClientServiceTests.enabled=true}. Both halves are load-bearing — under
 * {@code develop-profile} Failsafe does select {@code *IT} classes, so the rename alone would
 * not keep an offline build green.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "JdbiClientServiceTests.enabled", matches = "true")
class JdbiUserAccountStoreIT {
    private static AppbaseCore appbaseCore;
    private static Handle handle;
    private static IdService idService;
    private static PasswordService passwordService;
    private static JdbiUserAccountStore jdbiUserAccountStore;
    private static JdbiClientService jdbiClientService;

    @BeforeAll
    static void setUp() {
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

        appbaseCore = JdbiClientServiceTestHelper.appbaseCore();
        jdbiClientService = JdbiClientServiceTestHelper.defaultJdbiClientService();
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
        handle = jdbiClientService.getClient().open();
        jdbiUserAccountStore = new JdbiUserAccountStore(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                passwordService,
                handle,
                JdbiUserAccountStoreConfiguration.builder().build());
    }

    @AfterAll
    static void tearDown() {
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

        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(UserAccountStore.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                UserAccountStore.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(jdbiUserAccountStore, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 =
                serviceRegistry.getAppbaseService(JdbiUserAccountStore.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(jdbiUserAccountStore, appbaseServiceEntry2.getService());
    }

    @Test
    void testCreateUserAccount() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final Optional<UserAccount> readUserAccountResult1 = jdbiUserAccountStore.readByIdpUserId(idpUserId);
            Assertions.assertNotNull(readUserAccountResult1);
            Assertions.assertTrue(readUserAccountResult1.isPresent());
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccountResult1.get());
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testReadByAppbaseUserId() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final Optional<UserAccount> readUserAccountResult1 =
                    jdbiUserAccountStore.readByAppbaseUserId(appbaseUserId);
            Assertions.assertNotNull(readUserAccountResult1);
            Assertions.assertTrue(readUserAccountResult1.isPresent());
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccountResult1.get());

            final Optional<UserAccount> readUserAccountResult2 =
                    jdbiUserAccountStore.readByAppbaseUserId(idService.newId());
            Assertions.assertNotNull(readUserAccountResult2);
            Assertions.assertFalse(readUserAccountResult2.isPresent());
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testReadByIdpUserId() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final Optional<UserAccount> readUserAccountResult1 = jdbiUserAccountStore.readByIdpUserId(idpUserId);
            Assertions.assertNotNull(readUserAccountResult1);
            Assertions.assertTrue(readUserAccountResult1.isPresent());
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccountResult1.get());

            final Optional<UserAccount> readUserAccountResult2 =
                    jdbiUserAccountStore.readByIdpUserId(idService.newId());
            Assertions.assertNotNull(readUserAccountResult2);
            Assertions.assertFalse(readUserAccountResult2.isPresent());
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testReadByEmail() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final Optional<UserAccount> readUserAccountResult1 =
                    jdbiUserAccountStore.readByEmail(userAccount.getEmail());
            Assertions.assertNotNull(readUserAccountResult1);
            Assertions.assertTrue(readUserAccountResult1.isPresent());
            AppbaseServiceTestHelper.assertUserAccount(userAccount, readUserAccountResult1.get());

            final Optional<UserAccount> readUserAccountResult2 =
                    jdbiUserAccountStore.readByEmail(idService.newId() + "@a.com");
            Assertions.assertNotNull(readUserAccountResult2);
            Assertions.assertFalse(readUserAccountResult2.isPresent());
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testExistsByAppbaseUserId() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final boolean exists1 = jdbiUserAccountStore.existsByAppbaseUserId(appbaseUserId);
            Assertions.assertTrue(exists1);

            final boolean exists2 = jdbiUserAccountStore.existsByAppbaseUserId(idService.newId());
            Assertions.assertFalse(exists2);
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testExistsByIdpUserId() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final boolean exists1 = jdbiUserAccountStore.existsByIdpUserId(idpUserId);
            Assertions.assertTrue(exists1);

            final boolean exists2 = jdbiUserAccountStore.existsByIdpUserId(idService.newId());
            Assertions.assertFalse(exists2);
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testExistsByEmail() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final boolean exists1 = jdbiUserAccountStore.existsByEmail(userAccount.getEmail());
            Assertions.assertTrue(exists1);

            final boolean exists2 = jdbiUserAccountStore.existsByEmail(idService.newId() + "@abc.com");
            Assertions.assertFalse(exists2);
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testUpdateUserAccount() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);

            final Optional<UserAccount> readUserAccountResult1 =
                    jdbiUserAccountStore.readByAppbaseUserId(appbaseUserId);
            Assertions.assertNotNull(readUserAccountResult1);
            Assertions.assertTrue(readUserAccountResult1.isPresent());
            Assertions.assertEquals(true, readUserAccountResult1.get().getEmailVerified());


            final UserAccount copy = new UserAccount(userAccount);
            copy.setEmailVerified(false);
            jdbiUserAccountStore.updateUserAccount(copy);

            final Optional<UserAccount> readUserAccountResult2 =
                    jdbiUserAccountStore.readByAppbaseUserId(appbaseUserId);
            Assertions.assertNotNull(readUserAccountResult2);
            Assertions.assertTrue(readUserAccountResult2.isPresent());
            Assertions.assertEquals(false, readUserAccountResult2.get().getEmailVerified());
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testDeleteByIdpUserId() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final boolean deleted1 = jdbiUserAccountStore.deleteByIdpUserId(idpUserId);
            Assertions.assertTrue(deleted1);

            final boolean deleted2 = jdbiUserAccountStore.deleteByIdpUserId(idService.newId());
            Assertions.assertFalse(deleted2);
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testDeleteByAppbaseUserId() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final boolean deleted1 = jdbiUserAccountStore.deleteByAppbaseUserId(appbaseUserId);
            Assertions.assertTrue(deleted1);

            final boolean deleted2 = jdbiUserAccountStore.deleteByAppbaseUserId(idService.newId());
            Assertions.assertFalse(deleted2);
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    @Test
    void testDeleteByEmail() throws UserAccountStoreException {
        final String appbaseUserId = idService.newId();
        final String idpUserId = idService.newId();
        final UserAccount userAccount = userAccount(appbaseUserId, idpUserId);

        try {
            final int result = jdbiUserAccountStore.createUserAccount(userAccount);
            Assertions.assertTrue(result > 0);
            final boolean deleted1 = jdbiUserAccountStore.deleteByEmail(userAccount.getEmail());
            Assertions.assertTrue(deleted1);

            final boolean deleted2 = jdbiUserAccountStore.deleteByEmail(idService.newId() + "@abc.com");
            Assertions.assertFalse(deleted2);
        } finally {
            deleteUserAccount(userAccount);
        }
    }

    private UserAccount userAccount(final String appbaseUserId, final String idpUserId) {
        final LocalDateTime now = LocalDateTime.now();
        return UserAccount
                .builder()
                .appbaseUserId(appbaseUserId)
                .creationTimestamp(now)
                .deleted(false)
                .email("a@b.com")
                .emailVerified(true)
                .idpUserId(idpUserId)
                .languageCode("EN_CA")
                .lastAccessTimestamp(now)
                .name("name123")
                .passwordHashes(passwordHashes())
                .passwordResetEmailSent(false)
                .userTopic("userTopic123")
                .userHomeDataCenter("us-east-1")
                .addressLine1("123 Abcd Street")
                .addressLine2("Line 1 of address")
                .city("My city")
                .postalCode("1A2 3B6")
                .stateProvince("My state")
                .country("My country")
                .latitude("1.23")
                .longitude("12.234")
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

    private void deleteUserAccount(final UserAccount userAccount) throws UserAccountStoreException {
        if (userAccount != null) {
            final String idpUserId = userAccount.getIdpUserId();
            if (!AppbaseUtil.isBlank(idpUserId)) {
                jdbiUserAccountStore.deleteByIdpUserId(idpUserId);
                return;
            }

            final String appbaseUserId = userAccount.getAppbaseUserId();
            if (!AppbaseUtil.isBlank(appbaseUserId)) {
                jdbiUserAccountStore.deleteByAppbaseUserId(appbaseUserId);
            }
        }
    }

}