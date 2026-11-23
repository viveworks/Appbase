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
package com.viveworks.appbase.service.supportmessage;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.service.AppbaseServiceException;
import com.viveworks.appbase.service.supportmessage.data.DeleteSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ExistsSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesRequest;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesResponse;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageResponse;
import com.viveworks.appbase.service.supportmessage.data.SupportMessageIdRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateSupportMessageResponse;
import com.viveworks.appbase.service.supportmessage.data.UpdateStatusRequest;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface SupportMessageService extends AppbaseService {
    SendSupportMessageResponse sendSupportMessage(
            SendSupportMessageRequest sendSupportMessageRequest) throws AppbaseServiceException;

    ReadSupportMessagesResponse readSupportMessages(
            ReadSupportMessagesRequest readSupportMessagesRequest) throws AppbaseServiceException;

    ReadSupportMessageByIdResponse readSupportMessageById(
            SupportMessageIdRequest supportMessageIdRequest) throws AppbaseServiceException;

    ExistsSupportMessageByIdResponse existsSupportMessageById(
            SupportMessageIdRequest supportMessageIdRequest) throws AppbaseServiceException;

    UpdateSupportMessageResponse updateMessage(
            UpdateMessageRequest updateMessageRequest) throws AppbaseServiceException;

    UpdateSupportMessageResponse updateStatus(
            UpdateStatusRequest updateStatusRequest) throws AppbaseServiceException;

    DeleteSupportMessageByIdResponse deleteSupportMessageById(
            SupportMessageIdRequest supportMessageIdRequest) throws AppbaseServiceException;

    void registerSupportMessageListener(SupportMessageListener supportMessageListener);
}
