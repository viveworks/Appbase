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
package com.viveworks.appbase.service.supportmessage;

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
import com.viveworks.appbase.service.AppbaseServiceException;
import com.viveworks.appbase.service.user.testhelper.AppbaseServiceTestHelper;
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.supportmessage.data.DeleteSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ExistsSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesRequest;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesResponse;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageResponse;
import com.viveworks.appbase.service.supportmessage.data.SupportMessageIdRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateStatusRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateSupportMessageResponse;
import com.viveworks.appbase.service.supportmessage.store.JdbiSupportMessageStore;
import com.viveworks.appbase.service.supportmessage.store.JdbiSupportMessageStoreConfiguration;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Collection;
import java.util.UUID;

/**
 * Reclassified from {@code DefaultSupportMessageServiceTest} and gated by P-051 · E1135 · N11 (internal work item 1301): this class
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
class DefaultSupportMessageServiceIT {
    private static AppbaseCore appbaseCore;
    private static Handle handle;
    private static JdbiClientService jdbiClientService;
    private static JdbiSupportMessageStore jdbiSupportMessageStore;
    private static IdService idService;
    private static SupportMessageService supportMessageService;
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
        idService = new DefaultIdService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultIdServiceConfiguration.builder().build());
        supportMessageService = new DefaultSupportMessageService(
                jdbiSupportMessageStore,
                idService,
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                new DefaultSupportMessageServiceConfiguration());
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

        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(SupportMessageService.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                SupportMessageService.class,
                appbaseServiceEntry1);

        final AppbaseServiceEntry appbaseServiceEntry2 =
                serviceRegistry.getAppbaseService(SupportMessageService.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(supportMessageService, appbaseServiceEntry2.getService());
    }

    @Test
    void testSendSupportMessage() throws AppbaseServiceException {
        SupportMessage supportMessage = null;
        try {
            final SendSupportMessageRequest sendSupportMessageRequest = new SendSupportMessageRequest();
            sendSupportMessageRequest.setEmail(email);
            sendSupportMessageRequest.setMessage("Message-" + UUID.randomUUID());
            sendSupportMessageRequest.setAppbaseUserId("AppbaseUserId-" + UUID.randomUUID());
            sendSupportMessageRequest.setStatus("CREATING");

            final SendSupportMessageResponse sendSupportMessageResponse =
                    supportMessageService.sendSupportMessage(sendSupportMessageRequest);
            Assertions.assertNotNull(sendSupportMessageResponse);

            supportMessage = sendSupportMessageResponse.getSupportMessage();
            Assertions.assertNotNull(supportMessage);

            Assertions.assertEquals(email, supportMessage.getEmail());
            Assertions.assertEquals(sendSupportMessageRequest.getMessage(), supportMessage.getMessage());
            Assertions.assertEquals(sendSupportMessageRequest.getAppbaseUserId(), supportMessage.getAppbaseUserId());
            Assertions.assertNotNull(supportMessage.getCreationTimestamp());
            Assertions.assertNotNull(supportMessage.getLastAccessTimestamp());
            Assertions.assertFalse(supportMessage.getDeleted());
        } finally {
            deleteSupportMessage(supportMessage);
        }
    }

    @Test
    void testReadSupportMessages() throws AppbaseServiceException {
        final SupportMessage supportMessage = supportMessage();

        try {
            final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest();
            supportMessageIdRequest.setMessageId(supportMessage.getMessageId());

            final ReadSupportMessagesResponse readSupportMessagesResponse =
                    supportMessageService.readSupportMessages(new ReadSupportMessagesRequest(0, 10, null));
            Assertions.assertNotNull(readSupportMessagesResponse);

            final Collection<SupportMessage> supportMessages = readSupportMessagesResponse.getSupportMessages();
            Assertions.assertNotNull(supportMessages);
            Assertions.assertFalse(supportMessages.isEmpty());

            boolean found = false;
            for (final SupportMessage supportMessage1 : supportMessages) {
                if (supportMessage.equals(supportMessage1)) {
                    found = true;
                    break;
                }
            }
            Assertions.assertTrue(found);
        } finally {
            deleteSupportMessage(supportMessage);
        }
    }

    @Test
    void testReadSupportMessageById() throws AppbaseServiceException {
        final SupportMessage supportMessage = supportMessage();

        try {
            final SupportMessageIdRequest supportMessageIdRequest =
                    new SupportMessageIdRequest(supportMessage.getMessageId(), null);
            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse =
                    supportMessageService.readSupportMessageById(supportMessageIdRequest);

            Assertions.assertNotNull(readSupportMessageByIdResponse);
            Assertions.assertEquals(supportMessage, readSupportMessageByIdResponse.getSupportMessage());
        } finally {
            deleteSupportMessage(supportMessage);
        }
    }

    @Test
    void testExistsSupportMessageById() throws AppbaseServiceException {
        final SupportMessage supportMessage = supportMessage();

        try {
            final SupportMessageIdRequest supportMessageIdRequest =
                    new SupportMessageIdRequest(supportMessage.getMessageId(), null);
            final ExistsSupportMessageByIdResponse existsSupportMessageByIdResponse =
                    supportMessageService.existsSupportMessageById(supportMessageIdRequest);

            Assertions.assertNotNull(existsSupportMessageByIdResponse);
            Assertions.assertTrue(existsSupportMessageByIdResponse.isExists());
        } finally {
            deleteSupportMessage(supportMessage);
        }
    }

    @Test
    void testUpdateMessage() throws AppbaseServiceException {
        final SupportMessage supportMessage = supportMessage();

        try {
            final UpdateMessageRequest updateMessageRequest = new UpdateMessageRequest();
            updateMessageRequest.setMessageId(supportMessage.getMessageId());
            updateMessageRequest.setMessage("New-message-" + UUID.randomUUID());

            final UpdateSupportMessageResponse updateSupportMessageResponse =
                    supportMessageService.updateMessage(updateMessageRequest);
            Assertions.assertNotNull(updateSupportMessageResponse);
            Assertions.assertTrue(updateSupportMessageResponse.isUpdated());

            final SupportMessageIdRequest supportMessageIdRequest =
                    new SupportMessageIdRequest(supportMessage.getMessageId(), null);
            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse =
                    supportMessageService.readSupportMessageById(supportMessageIdRequest);
            Assertions.assertNotNull(readSupportMessageByIdResponse);

            final SupportMessage supportMessage1 = readSupportMessageByIdResponse.getSupportMessage();
            Assertions.assertNotNull(supportMessage1);
            Assertions.assertEquals(updateMessageRequest.getMessageId(), supportMessage1.getMessageId());
            Assertions.assertNotEquals(supportMessage.getMessage(), supportMessage1.getMessage());
            Assertions.assertEquals(updateMessageRequest.getMessage(), supportMessage1.getMessage());
        } finally {
            deleteSupportMessage(supportMessage);
        }
    }

    @Test
    void testUpdateStatus() throws AppbaseServiceException {
        final SupportMessage supportMessage = supportMessage();

        try {
            final UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest();
            updateStatusRequest.setMessageId(supportMessage.getMessageId());
            updateStatusRequest.setStatus("NEW_STATUS-" + UUID.randomUUID());

            final UpdateSupportMessageResponse updateSupportMessageResponse =
                    supportMessageService.updateStatus(updateStatusRequest);
            Assertions.assertNotNull(updateSupportMessageResponse);
            Assertions.assertTrue(updateSupportMessageResponse.isUpdated());

            final SupportMessageIdRequest supportMessageIdRequest =
                    new SupportMessageIdRequest(supportMessage.getMessageId(), null);
            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse =
                    supportMessageService.readSupportMessageById(supportMessageIdRequest);
            Assertions.assertNotNull(readSupportMessageByIdResponse);

            final SupportMessage supportMessage1 = readSupportMessageByIdResponse.getSupportMessage();
            Assertions.assertNotNull(supportMessage1);
            Assertions.assertEquals(updateStatusRequest.getMessageId(), supportMessage1.getMessageId());
            Assertions.assertNotEquals(supportMessage.getStatus(), supportMessage1.getStatus());
            Assertions.assertEquals(updateStatusRequest.getStatus(), supportMessage1.getStatus());
            Assertions.assertNotNull(supportMessage1);
        } finally {
            deleteSupportMessage(supportMessage);
        }
    }

    @Test
    void testDeleteSupportMessageById() throws AppbaseServiceException {
        final SupportMessage supportMessage = supportMessage();

        try {
            final SupportMessageIdRequest supportMessageIdRequest =
                    new SupportMessageIdRequest(supportMessage.getMessageId(), null);
            final DeleteSupportMessageByIdResponse deleteSupportMessageByIdResponse =
                    supportMessageService.deleteSupportMessageById(supportMessageIdRequest);

            Assertions.assertNotNull(deleteSupportMessageByIdResponse);
            Assertions.assertTrue(deleteSupportMessageByIdResponse.isDeleted());
        } finally {
            deleteSupportMessage(supportMessage);
        }
    }

    private void deleteSupportMessage(final SupportMessage supportMessage) throws AppbaseServiceException {
        if (supportMessage != null) {
            supportMessageService.deleteSupportMessageById(
                    new SupportMessageIdRequest(supportMessage.getMessageId(), null));
        }
    }

    private SupportMessage supportMessage() throws AppbaseServiceException {
        final SendSupportMessageRequest sendSupportMessageRequest = new SendSupportMessageRequest();
        sendSupportMessageRequest.setEmail(email);
        sendSupportMessageRequest.setMessage("Message-" + UUID.randomUUID());
        sendSupportMessageRequest.setAppbaseUserId("AppbaseUserId-" + UUID.randomUUID());
        sendSupportMessageRequest.setStatus("CREATING");

        final SendSupportMessageResponse sendSupportMessageResponse =
                supportMessageService.sendSupportMessage(sendSupportMessageRequest);
        Assertions.assertNotNull(sendSupportMessageResponse);

        final SupportMessage supportMessage = sendSupportMessageResponse.getSupportMessage();
        Assertions.assertNotNull(supportMessage);

        return supportMessage;
    }
}