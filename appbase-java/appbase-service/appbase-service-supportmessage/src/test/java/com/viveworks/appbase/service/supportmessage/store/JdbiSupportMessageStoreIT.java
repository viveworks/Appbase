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
package com.viveworks.appbase.service.supportmessage.store;

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
import com.viveworks.appbase.service.AppbaseStoreException;
import com.viveworks.appbase.service.supportmessage.SupportMessage;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Reclassified from {@code JdbiSupportMessageStoreTest} and gated by P-051 · E1135 · N11 (internal work item 1301): this class
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
class JdbiSupportMessageStoreIT {
    private static AppbaseCore appbaseCore;
    private static Handle handle;
    private static JdbiClientService jdbiClientService;
    private static JdbiSupportMessageStore jdbiSupportMessageStore;
    private static String email;
    private static String messageId;

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
        handle = jdbiClientService.getClient().open();
        jdbiSupportMessageStore = new JdbiSupportMessageStore(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                handle,
                JdbiSupportMessageStoreConfiguration.builder().build());
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

    @BeforeEach
    void beforeEach() {
        final String id = String.valueOf((int) (Math.random() * 10_000));
        email = "test-email-" + id + "@appbase.dev";
        messageId = "Message-" + id;
    }

    @Test
    void testRegisterService() throws InterruptedException {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();

        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(SupportMessageStore.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                SupportMessageStore.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(jdbiSupportMessageStore, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 =
                serviceRegistry.getAppbaseService(JdbiSupportMessageStore.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(jdbiSupportMessageStore, appbaseServiceEntry2.getService());
    }

    @Test
    void testCreateSupportMessage() throws AppbaseStoreException {
        try {
            final SupportMessage supportMessage = supportMessage();
            final int result = jdbiSupportMessageStore.createSupportMessage(supportMessage);
            Assertions.assertTrue(result > 0);

            final Optional<SupportMessage> supportMessageResult1 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult1);
            Assertions.assertTrue(supportMessageResult1.isPresent());

            final SupportMessage supportMessage1 = supportMessageResult1.get();
            assertSupportMessage(supportMessage, supportMessage1);

            final String newId = "MessageId-" + (int) (Math.random() * 100_000);
            final Optional<SupportMessage> supportMessageResult2 = jdbiSupportMessageStore.readByMessageId(newId);
            Assertions.assertNotNull(supportMessageResult2);
            Assertions.assertFalse(supportMessageResult2.isPresent());
        } finally {
            deleteSupportMessage();
        }
    }

    @Test
    void testReadByMessageId() throws AppbaseStoreException {
        try {
            final SupportMessage supportMessage = supportMessage();
            final int result = jdbiSupportMessageStore.createSupportMessage(supportMessage);
            Assertions.assertTrue(result > 0);

            final Optional<SupportMessage> supportMessageResult1 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult1);
            Assertions.assertTrue(supportMessageResult1.isPresent());

            final SupportMessage supportMessage1 = supportMessageResult1.get();
            assertSupportMessage(supportMessage, supportMessage1);
        } finally {
            deleteSupportMessage();
        }
    }

    @Test
    void testReadPage() throws AppbaseStoreException {
        try {
            final SupportMessage supportMessage = supportMessage();
            final int result = jdbiSupportMessageStore.createSupportMessage(supportMessage);
            Assertions.assertTrue(result > 0);

            final Collection<SupportMessage> readPageResult = jdbiSupportMessageStore.readPage(0, 10);
            Assertions.assertNotNull(readPageResult);
            Assertions.assertTrue(readPageResult.size() <= 10);

            boolean found = false;
            for (final SupportMessage supportMessage1 : readPageResult) {
                if (supportMessage.getMessageId().equals(supportMessage1.getMessageId())) {
                    assertSupportMessage(supportMessage, supportMessage1);
                    found = true;
                    break;
                }
            }
            Assertions.assertTrue(found);
        } finally {
            deleteSupportMessage();
        }
    }

    @Test
    void testExistsByMessageId() throws AppbaseStoreException {
        try {
            final SupportMessage supportMessage = supportMessage();
            final int result = jdbiSupportMessageStore.createSupportMessage(supportMessage);
            Assertions.assertTrue(result > 0);

            final boolean existsByMessageId1 = jdbiSupportMessageStore.existsByMessageId(messageId);
            Assertions.assertTrue(existsByMessageId1);

            jdbiSupportMessageStore.deleteByMessageId(messageId);

            final boolean existsByMessageId2 = jdbiSupportMessageStore.existsByMessageId(messageId);
            Assertions.assertFalse(existsByMessageId2);
        } finally {
            deleteSupportMessage();
        }
    }

    @Test
    void testUpdateMessage() throws AppbaseStoreException {
        try {
            final SupportMessage supportMessage = supportMessage();
            final int result = jdbiSupportMessageStore.createSupportMessage(supportMessage);
            Assertions.assertTrue(result > 0);

            final Optional<SupportMessage> supportMessageResult1 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult1);
            Assertions.assertTrue(supportMessageResult1.isPresent());
            final SupportMessage supportMessage1 = supportMessageResult1.get();
            assertSupportMessage(supportMessage, supportMessage1);

            final String updatedMessage = "Updated message:" + UUID.randomUUID();
            jdbiSupportMessageStore.updateMessage(supportMessage1.getMessageId(), updatedMessage);

            final Optional<SupportMessage> supportMessageResult2 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult2);
            Assertions.assertTrue(supportMessageResult2.isPresent());
            final SupportMessage supportMessage2 = supportMessageResult2.get();
            Assertions.assertEquals(updatedMessage, supportMessage2.getMessage());
        } finally {
            deleteSupportMessage();
        }
    }

    @Test
    void testUpdateStatus() throws AppbaseStoreException {
        try {
            final SupportMessage supportMessage = supportMessage();
            final int result = jdbiSupportMessageStore.createSupportMessage(supportMessage);
            Assertions.assertTrue(result > 0);

            final Optional<SupportMessage> supportMessageResult1 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult1);
            Assertions.assertTrue(supportMessageResult1.isPresent());
            final SupportMessage supportMessage1 = supportMessageResult1.get();
            assertSupportMessage(supportMessage, supportMessage1);

            final String updatedStatus = "OPEN";
            Assertions.assertNotEquals(updatedStatus, supportMessage1.getStatus());
            jdbiSupportMessageStore.updateStatus(supportMessage1.getMessageId(), updatedStatus);

            final Optional<SupportMessage> supportMessageResult2 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult2);
            Assertions.assertTrue(supportMessageResult2.isPresent());
            final SupportMessage supportMessage2 = supportMessageResult2.get();
            Assertions.assertEquals(updatedStatus, supportMessage2.getStatus());
        } finally {
            deleteSupportMessage();
        }
    }

    @Test
    void testDeleteByMessageId() throws AppbaseStoreException {
        try {
            final SupportMessage supportMessage = supportMessage();
            final int result = jdbiSupportMessageStore.createSupportMessage(supportMessage);
            Assertions.assertTrue(result > 0);

            final Optional<SupportMessage> supportMessageResult1 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult1);
            Assertions.assertTrue(supportMessageResult1.isPresent());

            final SupportMessage supportMessage1 = supportMessageResult1.get();
            assertSupportMessage(supportMessage, supportMessage1);

            jdbiSupportMessageStore.deleteByMessageId(messageId);

            final Optional<SupportMessage> supportMessageResult2 = jdbiSupportMessageStore.readByMessageId(messageId);
            Assertions.assertNotNull(supportMessageResult2);
            Assertions.assertFalse(supportMessageResult2.isPresent());
        } finally {
            deleteSupportMessage();
        }
    }

    private void deleteSupportMessage() throws AppbaseStoreException {
        if (AppbaseUtil.isNotBlank(messageId)) {
            jdbiSupportMessageStore.deleteByMessageId(messageId);
        }
    }

    private SupportMessage supportMessage() {
        final LocalDateTime localDateTime = LocalDateTime.now();
        return SupportMessage
                .builder()
                .email(email)
                .messageId(messageId)
                .message("Random message: " + UUID.randomUUID().toString())
                .appbaseUserId(UUID.randomUUID().toString())
                .status("RESOLVED")
                .deleted(false)
                .creationTimestamp(localDateTime.minusWeeks(10))
                .lastAccessTimestamp(localDateTime)
                .build();
    }

    private void assertSupportMessage(final SupportMessage supportMessage, final SupportMessage supportMessage1) {
        Assertions.assertNotNull(supportMessage);
        Assertions.assertNotNull(supportMessage1);
        Assertions.assertEquals(supportMessage.getMessageId(), supportMessage1.getMessageId());
        Assertions.assertEquals(supportMessage.getEmail(), supportMessage1.getEmail());
        Assertions.assertEquals(supportMessage.getMessage(), supportMessage1.getMessage());
        Assertions.assertEquals(supportMessage.getAppbaseUserId(), supportMessage1.getAppbaseUserId());
        Assertions.assertEquals(supportMessage.getStatus(), supportMessage1.getStatus());
        Assertions.assertEquals(supportMessage.getDeleted(), supportMessage1.getDeleted());
        AppbaseServiceTestHelper.assertTimestampMinutes(
                supportMessage.getCreationTimestamp(), supportMessage1.getCreationTimestamp());
        AppbaseServiceTestHelper.assertTimestampMinutes(
                supportMessage.getLastAccessTimestamp(), supportMessage1.getLastAccessTimestamp());
    }
}
