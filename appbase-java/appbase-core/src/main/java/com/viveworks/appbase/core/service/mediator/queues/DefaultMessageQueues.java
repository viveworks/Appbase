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
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.eclipse.microprofile.metrics.Gauge;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of {@link MessageQueues}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_TOSTRING")
public class DefaultMessageQueues implements MessageQueues {
    private final MetricService metricService;
    private final ConcurrentMap<MessageType, Queue<Message>> messageQueueByMessageTypeMap;
    private final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration;

    public DefaultMessageQueues(final MetricService metricService,
                                final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration)
    {
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(defaultMessageQueuesConfiguration, "defaultMessageQueuesConfiguration");

        this.metricService = metricService;
        this.messageQueueByMessageTypeMap = new ConcurrentHashMap<>();
        this.defaultMessageQueuesConfiguration = defaultMessageQueuesConfiguration;
    }

    @Override
    public void enqueueMessage(final Message message) {
        AppbaseUtil.notNull(message, "message");

        final MessageType messageType = message.getMessageType();
        AppbaseUtil.notNull(messageType, "messageType");

        try {
            messageQueueByMessageTypeMap
                    .computeIfAbsent(messageType, f -> {
                        final Queue<Message> queue = newQueue();
                        final String sizeGaugeName = metricService.name(
                                getClass(), messageType.name(), "size", "gauge");
                        metricService
                                .getMetricRegistry()
                                .register(sizeGaugeName, (Gauge<Integer>) queue::size);
                        return queue;
                    })
                    .add(message);
        } catch (IllegalStateException ise) {
            final Queue<Message> queue = messageQueueByMessageTypeMap.get(messageType);
            if (queue != null && (queue.size() >= defaultMessageQueuesConfiguration.getBoundMessageQueueCapacity())) {
                throw new AppbaseRuntimeException("Message " + message + " cannot be added, the queue is full", ise);
            }
            throw new AppbaseRuntimeException("Failed to enqueue message " + message, ise);
        }
    }

    private Queue<Message> newQueue() {
        if (defaultMessageQueuesConfiguration.getMessageQueueType() == MessageQueueType.BOUND) {
            return new ManyToManyConcurrentArrayQueue<>(
                    defaultMessageQueuesConfiguration.getBoundMessageQueueCapacity());
        }
        return new ConcurrentLinkedQueue<>();
    }

    @Nullable
    @Override
    public Queue<Message> getMessageQueue(final MessageType messageType) {
        AppbaseUtil.notNull(messageType, "messageType");

        return messageQueueByMessageTypeMap.get(messageType);
    }

    @Override
    public void shutdown() {
        messageQueueByMessageTypeMap.clear();
    }
}
