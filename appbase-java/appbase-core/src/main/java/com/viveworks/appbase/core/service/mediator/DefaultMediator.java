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

package com.viveworks.appbase.core.service.mediator;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.queues.InvalidMessageException;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.mediator.queues.MessageQueues;
import com.viveworks.appbase.core.service.mediator.queues.MessageQueuesCleaner;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import com.viveworks.appbase.core.util.objectpool.ObjectPool;
import com.viveworks.appbase.core.util.objectpool.Poolable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintViolation;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link Mediator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_TOSTRING")
public final class DefaultMediator implements Mediator {
    private static final int ONE = 1;

    private final Logger logger;
    private final MetricService metricService;
    private final ValidationService validationService;

    private final ScheduledExecutorService sharedScheduledExecutorService;
    private final ExecutorService sharedExecutorService;
    private final DisruptorService<MediatorEvent> sharedDisruptorService;
    private final MediatorEventDelegatorRegistry sharedMediatorEventDelegatorRegistry;
    private final ObjectPool<Message> messageObjectPool;
    private final MessageQueues messageQueues;
    @Nullable private final MessageQueuesCleaner messageQueuesCleaner;
    private final EnumSet<MessageType> ignoredMessageTypeSet;
    private final ConcurrentMap<MessageType, MessageHandler> messageHandlerByMessageTypeMap;
    private final ConcurrentMap<String, ObjectPool<?>> objectPoolByPoolableTypeNameMap;
    private final ReentrantLock sharedMessageQueueLock;

    private final Counter asyncHandleMessageErrorCounter;
    private final Counter syncHandleMessageErrorCounter;
    private final Timer syncHandleMessageTimer;
    private final Timer asyncHandleMessageTimer;
    private final Timer publishMediatorEventTimer;
    private final Counter publishMediatorEventErrorCounter;
    private final Counter insufficientRingBufferCapacityCounter;

    private final ConcurrentMap<MessageType, Counter> ignoredMessageCounterByMessageTypeMap;
    private final ConcurrentMap<MessageType, Counter> ignoredMessageHandlerCounterByMessageTypeMap;
    private final AtomicBoolean shutdown;

    private DefaultMediator(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.logService, "builder.logService");
        AppbaseUtil.notNull(builder.metricService, "builder.metricService");
        AppbaseUtil.notNull(builder.validationService, "builder.validationService");
        AppbaseUtil.notNull(builder.sharedScheduledExecutorService, "builder.sharedScheduledExecutorService");
        AppbaseUtil.notNull(builder.sharedDisruptorService, "builder.sharedDisruptorService");
        AppbaseUtil.notNull(
                builder.sharedMediatorEventDelegatorRegistry, "builder.sharedMediatorEventDelegatorRegistry");
        AppbaseUtil.notNull(builder.messageObjectPool, "builder.messageObjectPool");
        AppbaseUtil.notNull(builder.messageQueues, "builder.messageQueues");
        AppbaseUtil.notNull(builder.ignoredMessageTypeSet, "builder.ignoredMessageTypeSet");
        AppbaseUtil.notNull(builder.messageHandlerByMessageTypeMap, "builder.messageHandlerByMessageTypeMap");
        AppbaseUtil.notNull(builder.sharedMessageQueueLock, "builder.sharedMessageQueueLock");
        AppbaseUtil.notNull(builder.appbaseRuntimeExceptionQueue, "builder.appbaseRuntimeExceptionQueue");
        AppbaseUtil.notNull(builder.sharedExecutorService, "builder.sharedExecutorService");

        logger = LoggerFactory.getLogger(getClass());
        metricService = builder.metricService;
        validationService = builder.validationService;
        sharedScheduledExecutorService = builder.sharedScheduledExecutorService;
        sharedDisruptorService = builder.sharedDisruptorService;
        sharedMediatorEventDelegatorRegistry = builder.sharedMediatorEventDelegatorRegistry;
        messageObjectPool = builder.messageObjectPool;
        messageQueues = builder.messageQueues;
        messageQueuesCleaner = builder.messageQueuesCleaner;
        ignoredMessageTypeSet = builder.ignoredMessageTypeSet;
        messageHandlerByMessageTypeMap = builder.messageHandlerByMessageTypeMap;
        sharedMessageQueueLock = builder.sharedMessageQueueLock;
        sharedExecutorService = builder.sharedExecutorService;
        objectPoolByPoolableTypeNameMap = Objects
                .requireNonNullElseGet(builder.objectPoolByPoolableTypeNameMap, ConcurrentHashMap::new);

        asyncHandleMessageErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncHandleMessage", "error", "Counter"));
        syncHandleMessageErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "syncHandleMessage", "error", "counter"));
        publishMediatorEventErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "publishMediatorEvent", "error", "counter"));
        insufficientRingBufferCapacityCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "insufficientRingBufferCapacity", "counter"));

        syncHandleMessageTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "syncHandleMessage", "timer"));
        asyncHandleMessageTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncHandleMessage", "timer"));
        publishMediatorEventTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "publishMediatorEvent", "timer"));

        ignoredMessageCounterByMessageTypeMap = new ConcurrentHashMap<>();
        ignoredMessageHandlerCounterByMessageTypeMap = new ConcurrentHashMap<>();
        shutdown = new AtomicBoolean(false);
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Message getMessageFromPool() {
        return messageObjectPool.pollCleanObject();
    }

    @Override
    public void putMessageToPool(final Message message) {
        AppbaseUtil.notNull(message, "message");

        message.clean();
        messageObjectPool.offerCleanObject(message);
    }

    private void checkMessage(final Message message) throws MediatorException {
        AppbaseUtil.notNull(message, "message");

        final MessageType messageType = message.getMessageType();
        AppbaseUtil.notNull(messageType, "messageType");

        final Set<ConstraintViolation<Message>> violations = validationService.getValidator().validate(message);
        if (violations != null && !violations.isEmpty()) {
            throw new InvalidMessageException(message, violations);
        }

        if (ignoredMessageTypeSet.contains(messageType)) {
            ignoredMessageCounterByMessageTypeMap
                    .computeIfAbsent(messageType, f -> {
                        final String counterName = metricService.name(
                                getClass(), messageType.name(), "ignoredMessage", "counter");
                        return metricService.getMetricRegistry().counter(counterName);
                    })
                    .inc();

            throw new MediatorException("Message type " + messageType.name() + " is ignored. " +
                    "Cannot handle this message type.");
        }
    }

    private void lockAndEnqueueMessage(final Message message) {
        sharedMessageQueueLock.lock();
        try {
            messageQueues.enqueueMessage(new Message(message));
        } finally {
            sharedMessageQueueLock.unlock();
        }
    }

    @Override
    public void asyncHandleMessage(final Message message, final boolean putMessageBackToPool) throws MediatorException {
        checkMessage(message);

        final MessageHandler messageHandler = messageHandlerByMessageTypeMap.get(message.getMessageType());
        if (messageHandler != null) {
            final Timer.Context context = asyncHandleMessageTimer.time();
            sharedExecutorService.submit(() -> {
                try {
                    final CompletableFuture<?> result = new CompletableFuture<>();
                    final MessageHandlerWrapper messageHandlerWrapper =
                            new MessageHandlerWrapper(messageHandler, result, context);
                    messageHandlerWrapper.onMessage(message);
                    result.whenComplete((r, e) -> {
                        if (putMessageBackToPool) {
                            putMessageToPool(message);
                        }
                    });
                } catch (MessageHandlerException mhe) {
                    logger.error("Error while asynchronously handling message: " + message, mhe);
                    asyncHandleMessageErrorCounter.inc();
                }
            });
        } else {
            lockAndEnqueueMessage(message);
        }
    }

    @Override
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void syncHandleMessage(final Message message) throws MediatorException {
        checkMessage(message);

        try {
            final MessageHandler messageHandler = messageHandlerByMessageTypeMap.get(message.getMessageType());
            if (messageHandler != null) {
                final Timer.Context context = syncHandleMessageTimer.time();
                try {
                    messageHandler.onMessage(message);
                } finally {
                    context.stop();
                }
            } else {
                lockAndEnqueueMessage(message);
            }
        } catch (final Exception exception) {
            syncHandleMessageErrorCounter.inc();
            throw new MediatorException("Error handling message: " + message, exception);
        } finally {
            putMessageToPool(message);
        }
    }

    @Override
    @Unmodifiable
    public Map<MessageType, MessageHandler> getMessageHandlerByMessageTypeMap() {
        return Collections.unmodifiableMap(messageHandlerByMessageTypeMap);
    }

    @Override
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void registerMessageHandlerByMessageType(final MessageType messageType,
                                                    final MessageHandler messageHandler) throws MediatorException
    {
        AppbaseUtil.notNull(messageType, "messageType");
        AppbaseUtil.notNull(messageHandler, "messageHandler");

        if (ignoredMessageTypeSet.contains(messageType)) {
            ignoredMessageHandlerCounterByMessageTypeMap
                    .computeIfAbsent(messageType, f -> {
                        final String counterName = metricService.name(
                                getClass(), messageType.name(), "ignoredMessageHandler", "counter");
                        return metricService.getMetricRegistry().counter(counterName);
                    })
                    .inc();

            throw new MediatorException("Message type " + messageType.name() + " is ignored. " +
                    "Handlers cannot be registered for this type.");
        }

        sharedMessageQueueLock.lock();
        try {
            final Queue<? extends Message> messages = messageQueues.getMessageQueue(messageType);
            if (messages != null) {
                for (int i = 0; i < messages.size(); ++i) {
                    final Message polledMessage = messages.poll();
                    try {
                        if (polledMessage != null) {
                            messageHandler.onMessage(polledMessage);
                        }
                    } catch (final Exception exception) {
                        messageQueues.enqueueMessage(new Message(polledMessage));
                    }
                }
            }

            logger.info("Registering message handler {} for type {}", messageHandler.getClass().getName(), messageType);
            messageHandlerByMessageTypeMap.put(messageType, messageHandler);
        } finally {
            sharedMessageQueueLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void unregisterMessageHandlerByMessageType(final MessageType messageType) {
        AppbaseUtil.notNull(messageType, "messageType");

        sharedMessageQueueLock.lock();
        try {
            final MessageHandler messageHandler = messageHandlerByMessageTypeMap.get(messageType);
            if (messageHandler != null) {
                final Queue<? extends Message> messages = messageQueues.getMessageQueue(messageType);
                if (messages != null) {
                    for (int i = 0; i < messages.size(); ++i) {
                        final Message polledMessage = messages.poll();
                        try {
                            if (polledMessage != null) {
                                messageHandler.onMessage(polledMessage);
                            }
                        } catch (final Exception exception) {
                            logger.error("Dropping message while unregistering message handler: {}",
                                    polledMessage, exception);
                        }
                    }
                }

                logger.info("Unregistering message handler {} for type {}",
                        messageHandler.getClass().getName(), messageType);
                messageHandlerByMessageTypeMap.remove(messageType);
            }
        } finally {
            sharedMessageQueueLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void clearMessageHandlerByMessageTypeMap() {
        sharedMessageQueueLock.lock();

        try {
            messageHandlerByMessageTypeMap.forEach((messageType, messageHandler) -> {
                if (messageType != null && messageHandler != null) {
                    final Queue<? extends Message> messages = messageQueues.getMessageQueue(messageType);
                    if (messages != null) {
                        for (int i = 0; i < messages.size(); ++i) {
                            final Message polledMessage = messages.poll();
                            try {
                                if (polledMessage != null) {
                                    messageHandler.onMessage(polledMessage);
                                }
                            } catch (final Exception exception) {
                                logger.error("Dropping message while clearing message handlers: {}",
                                        polledMessage, exception);
                            }
                        }
                    }
                }
            });

            messageHandlerByMessageTypeMap.clear();
        } finally {
            sharedMessageQueueLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ScheduledFuture<T> scheduleWithFixedDelay(final Schedulable<T> schedulable,
                                                         final long initialDelay,
                                                         final long delay,
                                                         final TimeUnit unit)
    {
        return (ScheduledFuture<T>) sharedScheduledExecutorService.scheduleWithFixedDelay(
                schedulable::schedule, initialDelay, delay, unit);
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void publishMediatorEvent(final MediatorEvent mediatorEvent) throws MediatorException {
        AppbaseUtil.notNull(mediatorEvent, "mediatorEvent");

        final RingBuffer<MediatorEvent> ringBuffer = sharedDisruptorService.getRingBuffer();
        if (ringBuffer.hasAvailableCapacity(AppbaseUtil.ONE)) {
            final Timer.Context context = publishMediatorEventTimer.time();
            try {
                final long sequence = ringBuffer.tryNext();
                try {
                    final MediatorEvent pooledMediatorEvent = ringBuffer.get(sequence);
                    pooledMediatorEvent.reset();
                    pooledMediatorEvent.copy(mediatorEvent);
                } finally {
                    ringBuffer.publish(sequence);
                }
            } catch (InsufficientCapacityException ice) {
                insufficientRingBufferCapacityCounter.inc();
                throw new MediatorException("Insufficient capacity while publishing event: " + mediatorEvent, ice);
            } catch (Exception ex) {
                logger.error("Error while publishing event {}", mediatorEvent, ex);
                publishMediatorEventErrorCounter.inc();
                throw new MediatorException("Error while publishing event " + mediatorEvent, ex);
            } finally {
                context.stop();
            }
        } else {
            insufficientRingBufferCapacityCounter.inc();
            throw new MediatorException("Insufficient capacity while publishing event " + mediatorEvent);
        }
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void publishCustomEvent(
            final Class<? extends MediatorEventType> mediatorEventType,
            final Object event) throws MediatorException
    {
        AppbaseUtil.notNull(mediatorEventType, "mediatorEventType");
        AppbaseUtil.notNull(event, "event");

        final RingBuffer<MediatorEvent> ringBuffer = sharedDisruptorService.getRingBuffer();
        if (ringBuffer.hasAvailableCapacity(AppbaseUtil.ONE)) {
            final Timer.Context context = publishMediatorEventTimer.time();
            try {
                final long sequence = ringBuffer.tryNext();

                try {
                    final MediatorEvent mediatorEvent = ringBuffer.get(sequence);
                    mediatorEvent.reset();
                    mediatorEvent.setMediatorEventType(mediatorEventType);
                    mediatorEvent.getCustomEvent().setEvent(event);
                } finally {
                    ringBuffer.publish(sequence);
                }
            } catch (InsufficientCapacityException ice) {
                insufficientRingBufferCapacityCounter.inc();
                throw new MediatorException("Insufficient capacity while publishing event " + event, ice);
            } catch (Exception ex) {
                logger.error("Error while publishing event {}", event, ex);
                publishMediatorEventErrorCounter.inc();
                throw new MediatorException("Error while publishing event " + event, ex);
            } finally {
                context.stop();
            }
        } else {
            insufficientRingBufferCapacityCounter.inc();
            throw new MediatorException("Insufficient capacity while publishing event " + event);
        }
    }

    @Override
    public ExecutorService getSharedExecutorService() {
        return sharedExecutorService;
    }

    @Override
    public ScheduledExecutorService getSharedScheduledExecutorService() {
        return sharedScheduledExecutorService;
    }

    @Override
    public DisruptorService<MediatorEvent> getSharedDisruptorService() {
        return sharedDisruptorService;
    }

    @Override
    public MediatorEventDelegatorRegistry getSharedMediatorEventDelegatorRegistry() {
        return sharedMediatorEventDelegatorRegistry;
    }

    @Override
    public <P extends Poolable> void registerSharedObjectPool(
            final Class<P> poolableType, final ObjectPool<P> objectPool)
    {
        AppbaseUtil.notNull(poolableType, "poolableType");
        AppbaseUtil.notNull(objectPool, "objectPool");

        objectPoolByPoolableTypeNameMap.put(poolableType.getName(), objectPool);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends Poolable> ObjectPool<P> getSharedObjectPool(final Class<P> poolableType) {
        AppbaseUtil.notNull(poolableType, "poolableType");

        return (ObjectPool<P>) objectPoolByPoolableTypeNameMap.get(poolableType.getName());
    }

    @Override
    public <P extends Poolable> void unregisterSharedObjectPool(final Class<P> poolableType) {
        AppbaseUtil.notNull(poolableType, "poolableType");

        objectPoolByPoolableTypeNameMap.remove(poolableType.getName());
    }

    @Override
    public void clearSharedObjectPools() {
        objectPoolByPoolableTypeNameMap.clear();
    }

    @Nullable
    @Override
    public Collection<Message> getRemovedMessages() {
        if (messageQueuesCleaner != null) {
            return messageQueuesCleaner.getRemovedMessages();
        }

        return null;
    }

    @Override
    public void resetRemovedMessages() {
        if (messageQueuesCleaner != null) {
            messageQueuesCleaner.resetRemovedMessages();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void shutdown() {
        if (shutdown.getAndSet(true)) {
            return;
        }

        AppbaseUtil.shutdownExecutor(sharedScheduledExecutorService);
        AppbaseUtil.shutdownExecutor(sharedExecutorService);

        try {
            sharedDisruptorService.shutdown();
        } catch (Exception ex) {
            logger.error("Error shutting down disruptor service", ex);
        }

        messageObjectPool.shutdown();
        messageQueues.shutdown();
        ignoredMessageTypeSet.clear();
        messageHandlerByMessageTypeMap.clear();
        ignoredMessageCounterByMessageTypeMap.clear();
        ignoredMessageHandlerCounterByMessageTypeMap.clear();

        if (messageQueuesCleaner != null) {
            messageQueuesCleaner.resetRemovedMessages();
            messageQueuesCleaner.resetAppbaseRuntimeExceptions();
        }

        clearMessageHandlerByMessageTypeMap();
    }

    class MessageHandlerWrapper implements MessageHandler {
        private final MessageHandler messageHandler;
        private final CompletableFuture<?> completableFuture;
        private final Timer.Context context;

        MessageHandlerWrapper(final MessageHandler messageHandler,
                              final CompletableFuture<?> completableFuture,
                              final Timer.Context context)
        {
            this.messageHandler = messageHandler;
            this.completableFuture = completableFuture;
            this.context = context;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void onMessage(final Message message) throws MessageHandlerException {
            try {
                messageHandler.onMessage(message);
                completableFuture.complete(null);
            } catch (Exception ex) {
                completableFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        }
    }


    /**
     * {@link DefaultMediator} builder.
     *
     * <blockquote>
     *     <p>
     *         <b>NOTE:</b> This builder <i>does not</i> define any defaults. All fields must be set for building
     *         {@link DefaultMediator}.
     *     </p>
     * </blockquote>
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private LogService logService;
        private MetricService metricService;
        private ValidationService validationService;
        private ScheduledExecutorService sharedScheduledExecutorService;
        private DisruptorService<MediatorEvent> sharedDisruptorService;
        private MediatorEventDelegatorRegistry sharedMediatorEventDelegatorRegistry;
        private ObjectPool<Message> messageObjectPool;
        private MessageQueues messageQueues;
        @Nullable private MessageQueuesCleaner messageQueuesCleaner;
        private EnumSet<MessageType> ignoredMessageTypeSet;
        private ConcurrentMap<MessageType, MessageHandler> messageHandlerByMessageTypeMap;
        private ConcurrentMap<String, ObjectPool<?>> objectPoolByPoolableTypeNameMap;
        private ReentrantLock sharedMessageQueueLock;
        private Queue<AppbaseRuntimeException> appbaseRuntimeExceptionQueue;
        private ExecutorService sharedExecutorService;

        private Builder() {
        }

        public Builder logService(final LogService logService) {
            this.logService = logService;
            return this;
        }

        public Builder metricService(final MetricService metricService) {
            this.metricService = metricService;
            return this;
        }

        public Builder validatorService(final ValidationService validationService) {
            this.validationService = validationService;
            return this;
        }

        public Builder sharedScheduledExecutorService(final ScheduledExecutorService sharedScheduledExecutorService) {
            this.sharedScheduledExecutorService = sharedScheduledExecutorService;
            return this;
        }

        public Builder sharedDisruptorService(final DisruptorService<MediatorEvent> sharedDisruptorService) {
            this.sharedDisruptorService = sharedDisruptorService;
            return this;
        }

        public Builder sharedMediatorEventDelegatorRegistry(
                final MediatorEventDelegatorRegistry sharedMediatorEventDelegatorRegistry)
        {
            this.sharedMediatorEventDelegatorRegistry = sharedMediatorEventDelegatorRegistry;
            return this;
        }

        public Builder messageObjectPool(final ObjectPool<Message> messageObjectPool) {
            this.messageObjectPool = messageObjectPool;
            return this;
        }

        public Builder messageQueues(final MessageQueues messageQueues) {
            this.messageQueues = messageQueues;
            return this;
        }

        public Builder messageQueuesCleaner(@Nullable final MessageQueuesCleaner messageQueuesCleaner) {
            this.messageQueuesCleaner = messageQueuesCleaner;
            return this;
        }

        public Builder ignoredMessageTypeSet(final EnumSet<MessageType> ignoredMessageTypeSet) {
            this.ignoredMessageTypeSet = ignoredMessageTypeSet;
            return this;
        }

        public Builder messageHandlerByMessageTypeMap(
                final ConcurrentMap<MessageType, MessageHandler> messageHandlerByMessageTypeMap)
        {
            this.messageHandlerByMessageTypeMap = messageHandlerByMessageTypeMap;
            return this;
        }

        public Builder objectPoolByPoolableTypeNameMap(
                final ConcurrentMap<String, ObjectPool<?>> objectPoolByPoolableTypeNameMap)
        {
            this.objectPoolByPoolableTypeNameMap = objectPoolByPoolableTypeNameMap;
            return this;
        }

        public Builder sharedMessageQueueLock(final ReentrantLock sharedMessageQueueLock) {
            this.sharedMessageQueueLock = sharedMessageQueueLock;
            return this;
        }

        public Builder appbaseRuntimeExceptionQueue(final Queue<AppbaseRuntimeException> appbaseRuntimeExceptionQueue) {
            this.appbaseRuntimeExceptionQueue = appbaseRuntimeExceptionQueue;
            return this;
        }

        @SuppressFBWarnings({"HES_EXECUTOR_NEVER_SHUTDOWN", "HES_EXECUTOR_OVERWRITTEN_WITHOUT_SHUTDOWN"})
        public Builder sharedExecutorService(final ExecutorService sharedExecutorService) {
            this.sharedExecutorService = sharedExecutorService;
            return this;
        }

        public DefaultMediator build() {
            return new DefaultMediator(this);
        }
    }
    // CheckStyle: Builder end
}
