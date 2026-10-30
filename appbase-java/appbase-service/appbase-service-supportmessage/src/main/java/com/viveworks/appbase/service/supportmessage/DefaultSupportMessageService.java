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

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.AppbaseServiceException;
import com.viveworks.appbase.service.AppbaseServiceUtil;
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
import com.viveworks.appbase.service.supportmessage.store.SupportMessageStore;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultSupportMessageService extends
        AbstractStatelessAppbaseService<DefaultSupportMessageServiceConfiguration>
        implements SupportMessageService
{
    private final IdService idService;
    private final SupportMessageStore supportMessageStore;
    private final AtomicReference<SupportMessageListener> supportMessageListenerRef;

    private final Timer sendSupportMessageTimer;
    private final Timer readSupportMessagesTimer;
    private final Timer readSupportMessageByIdTimer;
    private final Timer existsSupportMessageByIdTimer;
    private final Timer updateMessageTimer;
    private final Timer updateStatusTimer;
    private final Timer deleteSupportMessageByIdTimer;

    @Inject
    public DefaultSupportMessageService(
            final SupportMessageStore supportMessageStore,
            final IdService idService,
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultSupportMessageServiceConfiguration serviceConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, serviceConfiguration);

        AppbaseUtil.notNull(idService, "idService");
        AppbaseUtil.notNull(supportMessageStore, "supportMessageStore");

        this.idService = idService;
        this.supportMessageStore = supportMessageStore;
        this.supportMessageListenerRef = new AtomicReference<>();

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.sendSupportMessageTimer =
                metricRegistry.timer(metricService.name(getClass(), "sendSupportMessageTimer"));
        this.readSupportMessagesTimer =
                metricRegistry.timer(metricService.name(getClass(), "readSupportMessagesTimer"));
        this.readSupportMessageByIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readSupportMessageByIdTimer"));
        this.existsSupportMessageByIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsSupportMessageByIdTimer"));
        this.updateMessageTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateMessageTimer"));
        this.updateStatusTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateStatusTimer"));
        this.deleteSupportMessageByIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteSupportMessageByIdTimer"));
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(SupportMessageService.class, this);
        serviceRegistry.registerAppbaseService(DefaultSupportMessageService.class, this);
    }

    @Override
    public SendSupportMessageResponse sendSupportMessage(
            final SendSupportMessageRequest sendSupportMessageRequest) throws AppbaseServiceException
    {
        AppbaseUtil.notNull(sendSupportMessageRequest, "sendSupportMessageRequest");
        AppbaseUtil.validate(new SendSupportMessageRequest.Validator(), sendSupportMessageRequest);

        final Timer.Context context = sendSupportMessageTimer.time();
        try {
            final String messageId = idService.newId();
            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();

            final SupportMessage supportMessage = SupportMessage
                    .builder()
                    .messageId(messageId)
                    .email(sendSupportMessageRequest.getEmail())
                    .message(sendSupportMessageRequest.getMessage())
                    .status(sendSupportMessageRequest.getStatus())
                    .appbaseUserId(sendSupportMessageRequest.getAppbaseUserId())
                    .deleted(false)
                    .creationTimestamp(now)
                    .lastAccessTimestamp(now)
                    .build();
            final int create = supportMessageStore.createSupportMessage(supportMessage);
            if (create < 1) {
                throw new AppbaseServiceException("Unexpected response from storage. Could not send message ");
            }

            final SupportMessageListener supportMessageListener = supportMessageListenerRef.get();
            if (supportMessageListener != null) {
                supportMessageListener.onSupportMessage(supportMessage);
            }

            return new SendSupportMessageResponse(supportMessage, sendSupportMessageRequest.getTraceId());
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadSupportMessagesResponse readSupportMessages(
            final ReadSupportMessagesRequest readSupportMessagesRequest)
            throws AppbaseServiceException
    {
        AppbaseUtil.notNull(readSupportMessagesRequest, "readSupportMessagesRequest");
        AppbaseUtil.validate(new ReadSupportMessagesRequest.Validator(), readSupportMessagesRequest);

        final Timer.Context context = readSupportMessagesTimer.time();
        try {
            final Collection<SupportMessage> supportMessages = supportMessageStore.readPage(
                    readSupportMessagesRequest.getOffset(),
                    readSupportMessagesRequest.getLimit());

            return new ReadSupportMessagesResponse(
                    supportMessages != null ? supportMessages : Collections.emptyList(),
                    readSupportMessagesRequest.getTraceId());
        } finally {
            context.stop();
        }
    }

    @Override
    public ReadSupportMessageByIdResponse readSupportMessageById(
            final SupportMessageIdRequest supportMessageIdRequest)
            throws AppbaseServiceException
    {
        AppbaseUtil.notNull(supportMessageIdRequest, "supportMessageIdRequest");
        AppbaseUtil.validate(new SupportMessageIdRequest.Validator(), supportMessageIdRequest);

        final Timer.Context context = readSupportMessageByIdTimer.time();
        try {
            final Optional<SupportMessage> supportMessageOptional =
                    supportMessageStore.readByMessageId(supportMessageIdRequest.getMessageId());

            return supportMessageOptional
                    .map(supportMessage ->
                            new ReadSupportMessageByIdResponse(supportMessage, supportMessageIdRequest.getTraceId()))
                    .orElseGet(() ->
                            new ReadSupportMessageByIdResponse(null, supportMessageIdRequest.getTraceId()));
        } finally {
            context.stop();
        }
    }

    @Override
    public ExistsSupportMessageByIdResponse existsSupportMessageById(
            final SupportMessageIdRequest supportMessageIdRequest)
            throws AppbaseServiceException
    {
        AppbaseUtil.notNull(supportMessageIdRequest, "supportMessageIdRequest");
        AppbaseUtil.validate(new SupportMessageIdRequest.Validator(), supportMessageIdRequest);

        final Timer.Context context = existsSupportMessageByIdTimer.time();
        try {
            final boolean exists = supportMessageStore.existsByMessageId(supportMessageIdRequest.getMessageId());

            return new ExistsSupportMessageByIdResponse(exists, supportMessageIdRequest.getTraceId());
        } finally {
            context.stop();
        }
    }

    @Override
    public UpdateSupportMessageResponse updateMessage(
            final UpdateMessageRequest updateMessageRequest)
            throws AppbaseServiceException
    {
        AppbaseUtil.notNull(updateMessageRequest, "updateMessageRequest");
        AppbaseUtil.validate(new UpdateMessageRequest.Validator(), updateMessageRequest);

        final Timer.Context context = updateMessageTimer.time();
        try {
            final int updated = supportMessageStore.updateMessage(
                    updateMessageRequest.getMessageId(), updateMessageRequest.getMessage());

            return new UpdateSupportMessageResponse(updated > 0, updateMessageRequest.getTraceId());
        } finally {
            context.stop();
        }
    }

    @Override
    public UpdateSupportMessageResponse updateStatus(
            final UpdateStatusRequest updateStatusRequest)
            throws AppbaseServiceException
    {
        AppbaseUtil.notNull(updateStatusRequest, "updateStatusRequest");
        AppbaseUtil.validate(new UpdateStatusRequest.Validator(), updateStatusRequest);

        final Timer.Context context = updateStatusTimer.time();
        try {
            final int updated = supportMessageStore.updateStatus(
                    updateStatusRequest.getMessageId(), updateStatusRequest.getStatus());

            return new UpdateSupportMessageResponse(updated > 0, updateStatusRequest.getTraceId());
        } finally {
            context.stop();
        }
    }

    @Override
    public DeleteSupportMessageByIdResponse deleteSupportMessageById(
            final SupportMessageIdRequest supportMessageIdRequest)
            throws AppbaseServiceException
    {
        AppbaseUtil.notNull(supportMessageIdRequest, "supportMessageIdRequest");
        AppbaseUtil.validate(new SupportMessageIdRequest.Validator(), supportMessageIdRequest);

        final Timer.Context context = deleteSupportMessageByIdTimer.time();
        try {
            final boolean deleted = supportMessageStore.deleteByMessageId(supportMessageIdRequest.getMessageId());

            return new DeleteSupportMessageByIdResponse(deleted, supportMessageIdRequest.getTraceId());
        } finally {
            context.stop();
        }
    }

    @Override
    public void registerSupportMessageListener(final SupportMessageListener supportMessageListener) {
        AppbaseUtil.notNull(supportMessageListener, "supportMessageListener");

        supportMessageListenerRef.set(supportMessageListener);
    }
}
