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

package com.viveworks.appbase.core.service.mediator.queues;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.MessageHandler;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Counter;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

/**
 * Default implementation of {@link MessageQueuesCleaner}.
 *
 * <b>This calls {@link MessageHandler}s synchronously. If the handlers take long, they must do the work in another
 * thread.</b>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_TOSTRING")
public class DefaultMessageQueuesCleaner implements MessageQueuesCleaner {
    private final MessageQueues sharedMessageQueues;
    private final Map<MessageType, MessageHandler> sharedMessageHandlerByMessageTypeMap;
    private final long expiredMessageThresholdMillis;
    private final Lock sharedMessageQueueLock;
    private final EnumSet<MessageType> messageTypeEnumSet;
    private final Queue<Message> removedMessageQueue;
    private final Queue<AppbaseRuntimeException> appbaseRuntimeExceptionQueue;
    private final SchedulableConfiguration schedulableConfiguration;

    private final Counter appbaseRuntimeExceptionOfferErrorCounter;
    private final Counter requeueErrorCounter;

    public DefaultMessageQueuesCleaner(final MetricService metricService,
                                       final MessageQueues sharedMessageQueues,
                                       final Map<MessageType, MessageHandler> sharedMessageHandlerByMessageTypeMap,
                                       final long expiredMessageThresholdMillis,
                                       final Lock sharedMessageQueueLock,
                                       final Queue<Message> removedMessageQueue,
                                       final Queue<AppbaseRuntimeException> appbaseRuntimeExceptionQueue,
                                       final SchedulableConfiguration schedulableConfiguration)
    {
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(sharedMessageQueues, "sharedMessageQueues");
        AppbaseUtil.notNull(sharedMessageHandlerByMessageTypeMap, "sharedMessageHandlerByMessageTypeMap");
        AppbaseUtil.positive(expiredMessageThresholdMillis, "expiredMessageThresholdMillis");
        AppbaseUtil.notNull(sharedMessageQueueLock, "sharedMessageQueueLock");
        AppbaseUtil.notNull(removedMessageQueue, "removedMessageQueue");
        AppbaseUtil.notNull(appbaseRuntimeExceptionQueue, "appbaseRuntimeExceptionQueue");
        AppbaseUtil.notNull(schedulableConfiguration, "schedulableConfiguration");

        this.sharedMessageQueues = sharedMessageQueues;
        this.sharedMessageHandlerByMessageTypeMap = sharedMessageHandlerByMessageTypeMap;
        this.expiredMessageThresholdMillis = expiredMessageThresholdMillis;
        this.sharedMessageQueueLock = sharedMessageQueueLock;
        this.messageTypeEnumSet = EnumSet.allOf(MessageType.class);
        this.removedMessageQueue = removedMessageQueue;
        this.appbaseRuntimeExceptionQueue = appbaseRuntimeExceptionQueue;
        this.schedulableConfiguration = schedulableConfiguration;

        this.appbaseRuntimeExceptionOfferErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "appbaseRuntimeExceptionOffer", "error", "counter"));
        this.requeueErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "requeue", "error", "counter"));
    }

    @Override
    public Void schedule() {
        clean();
        return null;
    }

    @Override
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void clean() {
        sharedMessageQueueLock.lock();
        try {
            final long now = System.currentTimeMillis();

            messageTypeEnumSet.forEach(messageType -> {
                final Queue<Message> queue = sharedMessageQueues.getMessageQueue(messageType);

                if (queue != null) {
                    final MessageHandler messageHandler = sharedMessageHandlerByMessageTypeMap.get(messageType);

                    for (int i = 0; i < queue.size(); ++i) {
                        final Message queuedMessage = queue.poll();
                        if (queuedMessage != null) {
                            try {
                                final long messageTimestamp = queuedMessage.getTimestamp();
                                if (shouldRemoveMessage(now, messageTimestamp)) {
                                    removedMessageQueue.add(queuedMessage);
                                }

                                if (messageHandler != null) {
                                    messageHandler.onMessage(queuedMessage);
                                } else {
                                    if (!queue.offer(queuedMessage)) {
                                        requeueErrorCounter.inc();
                                    }
                                }
                            } catch (final Exception exception) {
                                final AppbaseRuntimeException appbaseRuntimeException =
                                        new AppbaseRuntimeException(
                                                "Error cleaning message " + queuedMessage, exception);
                                if (!appbaseRuntimeExceptionQueue.offer(appbaseRuntimeException)) {
                                    appbaseRuntimeExceptionOfferErrorCounter.inc();
                                }
                            }
                        }
                    }
                }
            });
        } catch (final Exception exception) {
            if (!appbaseRuntimeExceptionQueue.offer(new AppbaseRuntimeException(exception))) {
                appbaseRuntimeExceptionOfferErrorCounter.inc();
            }
        } finally {
            sharedMessageQueueLock.unlock();
        }
    }

    private boolean shouldRemoveMessage(final long now, final long messageTimestamp) {
        return (messageTimestamp <= 0) ||
                (now < messageTimestamp) ||
                ((now - messageTimestamp) >= expiredMessageThresholdMillis);
    }

    @Override
    public Collection<Message> getRemovedMessages() {
        return Collections.unmodifiableCollection(removedMessageQueue);
    }

    @Override
    public void resetRemovedMessages() {
        removedMessageQueue.clear();
    }

    @Override
    public Collection<AppbaseRuntimeException> getAppbaseRuntimeExceptions() {
        return Collections.unmodifiableCollection(appbaseRuntimeExceptionQueue);
    }

    @Override
    public void resetAppbaseRuntimeExceptions() {
        appbaseRuntimeExceptionQueue.clear();
    }

    @Override
    public SchedulableConfiguration getSchedulableConfiguration() {
        return schedulableConfiguration;
    }
}
