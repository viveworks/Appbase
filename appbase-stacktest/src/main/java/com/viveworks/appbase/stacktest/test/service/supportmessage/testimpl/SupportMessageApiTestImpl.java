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

package com.viveworks.appbase.stacktest.test.service.supportmessage.testimpl;

import com.viveworks.appbase.stacktest.test.TestUtil;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceException;
import com.viveworks.appbase.service.supportmessage.SupportMessage;
import com.viveworks.appbase.service.supportmessage.SupportMessageService;
import com.viveworks.appbase.service.supportmessage.data.ExistsSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesResponse;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageResponse;
import com.viveworks.appbase.service.supportmessage.data.SupportMessageIdRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateStatusRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateSupportMessageResponse;
import com.viveworks.appbase.service.supportmessage.endpoint.jaxrs.SupportMessageJaxrsResource;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.DeleteSupportMessageByIdServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.ExistsSupportMessageByIdServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.ReadSupportMessageByIdServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.ReadSupportMessagesServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.SendSupportMessageServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.UpdateMessageServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.UpdateStatusServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.jaxrs.SupportMessageJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.servlet.DeleteSupportMessageByIdServletTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.servlet.ReadSupportMessageByIdServletTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.servlet.ReadSupportMessagesServletTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.servlet.SendSupportMessageServletTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.servlet.SupportMessageByIdServletTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.servlet.UpdateMessageServletTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.servlet.UpdateStatusServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.Collection;
import java.util.UUID;

import static com.viveworks.appbase.service.supportmessage.endpoint.jaxrs.SupportMessageJaxrsResource.UPDATE_MESSAGES_PATH;
import static com.viveworks.appbase.service.supportmessage.endpoint.jaxrs.SupportMessageJaxrsResource.UPDATE_STATUS_PATH;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SupportMessageApiTestImpl extends AbstractTest {
    private final Class<?> testType;

    public SupportMessageApiTestImpl(
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
                SupportMessageJaxrsResourceTest.class,
                DeleteSupportMessageByIdServletTest.class,
                ReadSupportMessageByIdServletTest.class,
                ReadSupportMessagesServletTest.class,
                SendSupportMessageServletTest.class,
                SupportMessageByIdServletTest.class,
                UpdateMessageServletTest.class,
                UpdateStatusServletTest.class);
    }

    public void existsSupportMessageByIdTestImpl() throws Exception {
        final String testName = "existsSupportMessageByIdTestImpl";

        startTest(testType, testName);

        SupportMessage supportMessage = null;
        try {
            supportMessage = createSupportMessage();

            final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest();
            supportMessageIdRequest.setMessageId(supportMessage.getMessageId());
            final ExistsSupportMessageByIdResponse existsSupportMessageByIdResponse = appbaseServices
                    .get(SupportMessageService.class)
                    .existsSupportMessageById(supportMessageIdRequest);
            checkNotNull(
                    existsSupportMessageByIdResponse,
                    "existsSupportMessageByIdTestImpl",
                    "existsSupportMessageByIdResponse");
            checkTrue(
                    existsSupportMessageByIdResponse.isExists(),
                    "existsSupportMessageByIdTestImpl",
                    "existsSupportMessageByIdResponse.exists");

            String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH;
            } else {
                url = baseUrl + ExistsSupportMessageByIdServlet.DEFAULT_PATH;
            }
            url = url + "?" + AppbaseServiceConstants.MESSAGE_ID_PARAM + "=" + supportMessage.getMessageId();

            final int status1 = httpHead(url);
            checkEquals(HttpServletResponse.SC_NO_CONTENT, status1, "existsSupportMessageByIdTestImpl", "status1");

            deleteSupportMessage(supportMessage);

            final int status2 = httpHead(url);
            checkEquals(HttpServletResponse.SC_NOT_FOUND, status2, "existsSupportMessageByIdTestImpl", "status2");
        } finally {
            deleteSupportMessage(supportMessage);
            endTest(testType, testName);
        }
    }

    public void readSupportMessagesTestImpl() throws Exception {
        final String testName = "readSupportMessagesTestImpl";

        startTest(testType, testName);

        SupportMessage supportMessage = null;
        try {
            String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH +
                        SupportMessageJaxrsResource.READ_SUPPORT_MESSAGES_PATH;
            } else {
                url = baseUrl + ReadSupportMessagesServlet.DEFAULT_PATH;
            }

            supportMessage = createSupportMessage();
            url = url
                    + "?" + AppbaseServiceConstants.OFFSET_PARAM + "=0"
                    + "&" + AppbaseServiceConstants.LIMIT_PARAM + "=10";

            final ReadSupportMessagesResponse readSupportMessagesResponse =
                    httpGetJson(url, ReadSupportMessagesResponse.class);
            checkNotNull(readSupportMessagesResponse, "readSupportMessagesTestImpl", "readSupportMessagesResponse");

            final Collection<SupportMessage> supportMessages = readSupportMessagesResponse.getSupportMessages();
            checkNotNull(supportMessages, "readSupportMessagesTestImpl", "supportMessages");
            checkFalse(supportMessages.isEmpty(), "readSupportMessagesTestImpl", "supportMessages.empty");

            boolean found = false;
            for (final SupportMessage supportMessage1 : supportMessages) {
                if (supportMessage.getMessageId().equals(supportMessage1.getMessageId())) {
                    found = true;
                    break;
                }
            }
            checkTrue(found, "readSupportMessagesTestImpl", "found");
        } finally {
            deleteSupportMessage(supportMessage);
            endTest(testType, testName);
        }
    }

    public void sendSupportMessagesTestImpl() throws Exception {
        final String testName = "sendSupportMessagesTestImpl";

        startTest(testType, testName);

        SupportMessage supportMessage = null;
        try {
            final String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH +
                        SupportMessageJaxrsResource.SEND_SUPPORT_MESSAGES_PATH;
            } else {
                url = baseUrl + SendSupportMessageServlet.DEFAULT_PATH;
            }

            final String id = UUID.randomUUID().toString();
            final String email = "Email-" + id;
            final String message = "Message-" + id;
            final String appbaseUserId = "AppbaseUserId-" + id;

            final SendSupportMessageRequest sendSupportMessageRequest = new SendSupportMessageRequest();
            sendSupportMessageRequest.setEmail(email);
            sendSupportMessageRequest.setMessage(message);
            sendSupportMessageRequest.setAppbaseUserId(appbaseUserId);
            sendSupportMessageRequest.setStatus("CREATING");

            final SendSupportMessageResponse sendSupportMessageResponse = httpPost(
                    url, sendSupportMessageRequest, SendSupportMessageRequest.class, SendSupportMessageResponse.class);
            checkNotNull(sendSupportMessageResponse, "sendSupportMessagesTestImpl", "sendSupportMessageResponse");

            supportMessage = sendSupportMessageResponse.getSupportMessage();
            checkNotNull(supportMessage, "sendSupportMessagesTestImpl", "supportMessage");
            checkEquals(email, supportMessage.getEmail(), "sendSupportMessagesTestImpl", "supportMessage.email");
            checkEquals(message, supportMessage.getMessage(), "sendSupportMessagesTestImpl", "supportMessage.message");

            checkNotNull(supportMessage.getStatus(), "sendSupportMessagesTestImpl", "supportMessage.status");
            checkEquals(
                    "CREATING",
                    supportMessage.getStatus(),
                    "sendSupportMessagesTestImpl",
                    "supportMessage.status");

            checkNotNull(
                    supportMessage.getAppbaseUserId(),
                    "sendSupportMessagesTestImpl",
                    "supportMessage.appbaseUserId");
            checkEquals(
                    appbaseUserId,
                    supportMessage.getAppbaseUserId(),
                    "sendSupportMessagesTestImpl",
                    "supportMessage.appbaseUserId");

            checkFalse(
                    supportMessage.getDeleted(),
                    "sendSupportMessagesTestImpl",
                    "supportMessage.deleted");
            checkNotNull(
                    supportMessage.getCreationTimestamp(),
                    "sendSupportMessagesTestImpl",
                    "supportMessage,creationTimestamp");
            checkNotNull(
                    supportMessage.getLastAccessTimestamp(),
                    "sendSupportMessagesTestImpl",
                    "supportMessage,lastAccessTimestamp");
        } finally {
            deleteSupportMessage(supportMessage);
            endTest(testType, testName);
        }
    }

    public void readSupportMessageByIdTestImpl() throws Exception {
        final String testName = "readSupportMessageByIdTestImpl";

        SupportMessage supportMessage = null;
        startTest(testType, testName);
        try {
            supportMessage = createSupportMessage();

            String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH;
            } else {
                url = baseUrl + ReadSupportMessageByIdServlet.DEFAULT_PATH;
            }
            url = url + "?" + AppbaseServiceConstants.MESSAGE_ID_PARAM + "=" + supportMessage.getMessageId();

            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse =
                    httpGetJson(url, ReadSupportMessageByIdResponse.class);
            checkNotNull(
                    readSupportMessageByIdResponse, "readSupportMessageByIdTestImpl", "readSupportMessageByIdResponse");

            final SupportMessage readSupportMessage = readSupportMessageByIdResponse.getSupportMessage();
            checkNotNull(readSupportMessage, "readSupportMessageByIdTestImpl", "readSupportMessage");

            checkNotNull(
                    readSupportMessage.getMessage(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.message");
            checkEquals(
                    supportMessage.getMessage(),
                    readSupportMessage.getMessage(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.message");

            checkNotNull(
                    readSupportMessage.getMessageId(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.messageId");
            checkEquals(
                    supportMessage.getMessageId(),
                    readSupportMessage.getMessageId(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.messageId");

            checkNotNull(
                    readSupportMessage.getEmail(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.email");
            checkEquals(
                    supportMessage.getEmail(),
                    readSupportMessage.getEmail(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.status");

            checkNotNull(
                    readSupportMessage.getStatus(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.status");
            checkEquals(
                    supportMessage.getStatus(),
                    readSupportMessage.getStatus(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.status");

            checkNotNull(
                    readSupportMessage.getDeleted(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.deleted");
            checkEquals(
                    supportMessage.getDeleted(),
                    readSupportMessage.getDeleted(),
                    "readSupportMessageByIdTestImpl",
                    "readSupportMessage.status");
        } finally {
            deleteSupportMessage(supportMessage);
            endTest(testType, testName);
        }
    }

    public void updateMessageTestImpl() throws Exception {
        final String testName = "updateMessageTestImpl";

        SupportMessage supportMessage = null;
        startTest(testType, testName);
        try {
            supportMessage = createSupportMessage();

            final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest();
            supportMessageIdRequest.setMessageId(supportMessage.getMessageId());

            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse1 = appbaseServices
                    .get(SupportMessageService.class)
                    .readSupportMessageById(supportMessageIdRequest);
            checkNotNull(readSupportMessageByIdResponse1, "updateMessageTestImpl", "readSupportMessageByIdResponse1");
            checkNotNull(
                    readSupportMessageByIdResponse1.getSupportMessage(),
                    "updateMessageTestImpl",
                    "readSupportMessageByIdResponse1.supportMessage");
            checkEquals(
                    supportMessage.getMessageId(),
                    readSupportMessageByIdResponse1.getSupportMessage().getMessageId(),
                    "updateMessageTestImpl",
                    "supportMessage.messageId");

            String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH + UPDATE_MESSAGES_PATH;
            } else {
                url = baseUrl + UpdateMessageServlet.DEFAULT_PATH;
            }

            final UpdateMessageRequest updateMessageRequest = new UpdateMessageRequest();
            updateMessageRequest.setMessageId(supportMessage.getMessageId());

            final String updatedMessage = supportMessage.getMessage() + "-" + UUID.randomUUID();
            updateMessageRequest.setMessageId(supportMessage.getMessageId());
            updateMessageRequest.setMessage(updatedMessage);

            final UpdateSupportMessageResponse updateSupportMessageResponse =
                    httpPut(url, updateMessageRequest, UpdateMessageRequest.class, UpdateSupportMessageResponse.class);
            checkNotNull(
                    updateSupportMessageResponse,
                    "updateMessageTestImpl",
                    "updateSupportMessageResponse");
            checkTrue(
                    updateSupportMessageResponse.isUpdated(),
                    "updateMessageTestImpl",
                    "updateSupportMessageResponse.updated");


            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse2 = appbaseServices
                    .get(SupportMessageService.class)
                    .readSupportMessageById(supportMessageIdRequest);
            checkNotNull(readSupportMessageByIdResponse2, "updateMessageTestImpl", "readSupportMessageByIdResponse2");
            checkNotNull(
                    readSupportMessageByIdResponse2.getSupportMessage(),
                    "updateMessageTestImpl",
                    "readSupportMessageByIdResponse2.supportMessage");
            checkEquals(
                    updatedMessage,
                    readSupportMessageByIdResponse2.getSupportMessage().getMessage(),
                    "updateMessageTestImpl",
                    "readSupportMessageByIdResponse2.supportMessage.message");
        } finally {
            deleteSupportMessage(supportMessage);
            endTest(testType, testName);
        }
    }

    public void updateStatusTestImpl() throws Exception {
        final String testName = "updateStatusTestImpl";

        SupportMessage supportMessage = null;
        startTest(testType, testName);
        try {
            supportMessage = createSupportMessage();

            final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest();
            supportMessageIdRequest.setMessageId(supportMessage.getMessageId());

            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse = appbaseServices
                    .get(SupportMessageService.class)
                    .readSupportMessageById(supportMessageIdRequest);
            checkNotNull(readSupportMessageByIdResponse, "updateMessageTestImpl", "readSupportMessageByIdResponse");
            checkNotNull(
                    readSupportMessageByIdResponse.getSupportMessage(),
                    "updateStatusTestImpl",
                    "readSupportMessageByIdResponse.supportMessage");
            checkEquals(
                    supportMessage.getMessageId(),
                    readSupportMessageByIdResponse.getSupportMessage().getMessageId(),
                    "updateStatusTestImpl",
                    "supportMessage.messageId");

            String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH + UPDATE_STATUS_PATH;
            } else {
                url = baseUrl + UpdateStatusServlet.DEFAULT_PATH;
            }

            final UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest();
            updateStatusRequest.setMessageId(supportMessage.getMessageId());
            final String newStatus = "RESOLVED";
            updateStatusRequest.setStatus(newStatus);

            final UpdateSupportMessageResponse updateSupportMessageResponse =
                    httpPut(url, updateStatusRequest, UpdateStatusRequest.class, UpdateSupportMessageResponse.class);
            checkNotNull(
                    updateSupportMessageResponse,
                    "updateStatusTestImpl",
                    "updateSupportMessageResponse");
            checkTrue(
                    updateSupportMessageResponse.isUpdated(),
                    "updateStatusTestImpl",
                    "updateSupportMessageResponse.updated");


            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse2 = appbaseServices
                    .get(SupportMessageService.class)
                    .readSupportMessageById(supportMessageIdRequest);
            checkNotNull(readSupportMessageByIdResponse2, "updateMessageTestImpl", "readSupportMessageByIdResponse2");
            checkNotNull(
                    readSupportMessageByIdResponse2.getSupportMessage(),
                    "updateStatusTestImpl",
                    "readSupportMessageByIdResponse2.supportMessage");
            checkNotNull(
                    readSupportMessageByIdResponse2.getSupportMessage().getStatus(),
                    "updateStatusTestImpl",
                    "readSupportMessageByIdResponse2.supportMessage.status");
            checkEquals(
                    newStatus,
                    readSupportMessageByIdResponse2.getSupportMessage().getStatus(),
                    "updateStatusTestImpl",
                    "readSupportMessageByIdResponse2.supportMessage.status");
        } finally {
            deleteSupportMessage(supportMessage);
            endTest(testType, testName);
        }
    }

    public void deleteSupportMessageByIdTestImpl() throws Exception {
        final String testName = "deleteSupportMessageById";

        SupportMessage supportMessage = null;
        startTest(testType, testName);
        try {
            supportMessage = createSupportMessage();

            final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest();
            supportMessageIdRequest.setMessageId(supportMessage.getMessageId());

            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse1 = appbaseServices
                    .get(SupportMessageService.class)
                    .readSupportMessageById(supportMessageIdRequest);
            checkNotNull(
                    readSupportMessageByIdResponse1,
                    "deleteSupportMessageByIdTestImpl",
                    "readSupportMessageByIdResponse1");
            checkNotNull(
                    readSupportMessageByIdResponse1.getSupportMessage(),
                    "readSupportMessageByIdResponse1.supportMessage",
                    "readSupportMessageByIdResponse1");
            checkEquals(
                    supportMessage.getMessageId(),
                    readSupportMessageByIdResponse1.getSupportMessage().getMessageId(),
                    "deleteSupportMessageByIdTestImpl",
                    "readSupportMessageByIdResponse1.supportMessage.messageId");

            String url;
            if (isRestApiTest()) {
                url = baseUrl + AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH;
            } else {
                url = baseUrl + DeleteSupportMessageByIdServlet.DEFAULT_PATH;
            }
            url = url + "?" + AppbaseServiceConstants.MESSAGE_ID_PARAM + "=" + supportMessage.getMessageId();

            httpDelete(url);

            final ExistsSupportMessageByIdResponse existsSupportMessageByIdResponse = appbaseServices
                    .get(SupportMessageService.class)
                    .existsSupportMessageById(supportMessageIdRequest);
            checkNotNull(
                    existsSupportMessageByIdResponse,
                    "deleteSupportMessageByIdTestImpl",
                    "existsSupportMessageByIdResponse");
            checkFalse(
                    existsSupportMessageByIdResponse.isExists(),
                    "deleteSupportMessageByIdTestImpl",
                    "existsSupportMessageByIdResponse.exists");
        } finally {
            deleteSupportMessage(supportMessage);
            endTest(testType, testName);
        }
    }

    private SupportMessage createSupportMessage() throws AppbaseServiceException {
        final String id = TestUtil.randomId();
        final String email = "test-email-" + id + "@appbase.dev";

        final SendSupportMessageRequest sendSupportMessageRequest = new SendSupportMessageRequest();
        sendSupportMessageRequest.setMessage("Random message: " + UUID.randomUUID());
        sendSupportMessageRequest.setEmail(email);
        sendSupportMessageRequest.setStatus("CREATING");

        final SupportMessageService supportMessageService = appbaseServices.get(SupportMessageService.class);
        final SendSupportMessageResponse sendSupportMessageResponse =
                supportMessageService.sendSupportMessage(sendSupportMessageRequest);

        return sendSupportMessageResponse.getSupportMessage();
    }

    private void deleteSupportMessage(final SupportMessage supportMessage) throws AppbaseServiceException {
        if (supportMessage != null) {
            appbaseServices
                    .get(SupportMessageService.class)
                    .deleteSupportMessageById(new SupportMessageIdRequest(supportMessage.getMessageId(), null));
        }
    }

    private boolean isRestApiTest() {
        return SupportMessageJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
