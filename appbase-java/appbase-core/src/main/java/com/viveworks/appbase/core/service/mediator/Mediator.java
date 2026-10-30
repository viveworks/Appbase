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

import com.viveworks.appbase.core.service.mediator.event.CustomEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventHandlerDelegator;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.util.Unmodifiable;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import com.viveworks.appbase.core.util.objectpool.ObjectPool;
import com.viveworks.appbase.core.util.objectpool.Poolable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class represents a mediator between <code>core</code> components of <code>Appbase</code>.
 *
 * <p>
 *     The different core components can communicate with each other using message via this class. This removes direct
 *     dependencies between them, which would have otherwise lead to complex initialization logic.
 * </p>
 *
 * <p>
 *     Also, it provides an interface to {@link Message}'s object pool and a scheduled executor that can be utilized
 *     throughout <code>Appbase</code>. Thus, reducing the amount of threads that would have been needed otherwise, i.e
 *     if other modules used their own scheduler.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface Mediator {
    /**
     * Get a {@link Message} object from pool. The returned object can be populated and used to handle the message.
     *
     * <pre>
     *     final Message message = mediator.getMessageFromPool();
     *     try {
     *         message.clean();
     *         message.setTimestamp(System.currentTimeMillis());
     *         message.setMessageType(MessageType.LIFECYCLE);
     *         message.getLifecycleMessage().setServiceType(DefaultMediatorService.class);
     *         message.getLifecycleMessage().setLifecycleStatus(LifecycleStatus.STARTED);
     *         mediator.syncHandleMessage(message);
     *     } finally {
     *         mediator.putMessageToPool(message);
     *     }
     * </pre>
     *
     * @return Message from the pool
     */
    Message getMessageFromPool();


    /**
     * Put a {@link Message} back to pool.
     *
     * @param message Message to put
     * @see #getMessageFromPool()
     */
    void putMessageToPool(Message message);


    /**
     * Asynchronously handle a {@link Message} by delegating to a registered {@link MessageHandler}. If a handler is
     * not registered, makes a copy of the message and enqueues it until one is available.
     *
     * <p>
     * <b>NOTE:</b> If a handler is not registered before a configurable duration, the
     * {@link com.viveworks.appbase.core.service.mediator.queues.MessageQueuesCleaner} is meant to remove the message
     * from the queue.
     * </p>
     *
     * @param message Message to handle
     * @param putMessageBackToPool If the message must be put back to the pool after handling it
     * @throws MediatorException if there were any errors
     * @see com.viveworks.appbase.core.service.mediator.queues.InvalidMessageException
     * @see #getMessageFromPool()
     */
    void asyncHandleMessage(Message message, boolean putMessageBackToPool) throws MediatorException;


    /**
     * Handle a {@link Message} by delegating to a registered {@link MessageHandler}. If a handler is not registered,
     * makes a copy of the message and enqueues it until one is available.
     *
     * <p>
     * <b>NOTE:</b> If a handler is not registered before a configurable duration, the
     * {@link com.viveworks.appbase.core.service.mediator.queues.MessageQueuesCleaner} is meant to remove the message
     * from the queue.
     * </p>
     *
     * @param message Message to handle
     * @throws MediatorException if there were any errors
     * @see com.viveworks.appbase.core.service.mediator.queues.InvalidMessageException
     * @see #getMessageFromPool()
     */
    void syncHandleMessage(Message message) throws MediatorException;


    /**
     * Get {@link MessageHandler} by {@link MessageType} map.
     *
     * @return Message handlers by message type map. Empty map if no mapping exists
     */
    @Unmodifiable
    Map<MessageType, MessageHandler> getMessageHandlerByMessageTypeMap();


    /**
     * Register a {@link Message} handler based on {@link MessageType}.
     *
     * @param messageType Message type handled by the handler
     * @param messageHandler The handler for specific message type
     * @throws MediatorException if the message type is ignored
     */
    void registerMessageHandlerByMessageType(
            MessageType messageType,
            MessageHandler messageHandler) throws MediatorException;


    /**
     * Unregister {@link MessageHandler} by {@link MessageType}.
     *
     * @param messageType Message type
     */
    void unregisterMessageHandlerByMessageType(MessageType messageType);


    /**
     * Clear {@link MessageHandler} by {@link MessageType} map.
     */
    void clearMessageHandlerByMessageTypeMap();


    /**
     * A common scheduled executor that can be used by multiple components, thus reducing the number of schedulers
     * and threads required otherwise.
     *
     * @param schedulable Schedulable that needs to be executed on a schedule
     * @param initialDelay If the first execution of the schedulable needs to be delayed
     * @param delay Delay between subsequent executions
     * @param unit Time unit for both initialDelay and delay
     * @param <T> The type returned by the scheduled future
     * @return The scheduled future from the executor
     */
    <T> ScheduledFuture<T> scheduleWithFixedDelay(Schedulable<T> schedulable,
                                                  long initialDelay,
                                                  long delay,
                                                  TimeUnit unit);

    /**
     * Publish the {@link MediatorEvent}. To be able to register a
     * {@link com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventListener}, use a
     * {@link MediatorEventHandlerDelegator}. This method is only
     * a convenience method to publish message. The reason being, there can be multiple delegators registered and
     * they all shared a common {@link MediatorEventDelegatorRegistry}. For example,
     * {@link com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService} can use multiple delegators.
     *
     * @param mediatorEvent Mediator event
     * @throws MediatorException if there are any errors
     */
    void publishMediatorEvent(MediatorEvent mediatorEvent) throws MediatorException;


    /**
     * Publish {@link CustomEvent#getEvent()}. To be able to register a
     * {@link com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventListener}, use a
     * {@link MediatorEventHandlerDelegator}. This method is only
     * a convenience method to publish message. The reason being, there can be multiple delegators registered and
     * they all shared a common {@link MediatorEventDelegatorRegistry}. For example,
     * {@link com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService} can use multiple delegators.
     *
     * @param event Event object
     * @param mediatorEventType {@link MediatorEventType} type
     * @throws MediatorException if there are any errors
     */
    void publishCustomEvent(Class<? extends MediatorEventType> mediatorEventType, Object event) throws MediatorException;



    /**
     * Get the shared disruptor service.
     *
     * @return Shared disruptor service
     */
    DisruptorService<MediatorEvent> getSharedDisruptorService();


    /**
     * Get the shared {@link MediatorEventDelegatorRegistry}.
     *
     * @return Mediator event delegator registry
     */
    MediatorEventDelegatorRegistry getSharedMediatorEventDelegatorRegistry();


    /**
     * Get the shared executor service.
     *
     * @return Shared executor service
     */
    ExecutorService getSharedExecutorService();

    /**
     * Get the shared scheduled executor service.
     *
     * @return Shared scheduled executor service
     */
    ScheduledExecutorService getSharedScheduledExecutorService();


    /**
     * Register an {@link ObjectPool} for a {@link Poolable} type.
     *
     * @param poolableType Object pool's {@link Poolable} type
     * @param objectPool {@link ObjectPool} for the {@link Poolable}
     * @param <P> Poolable type
     */
    <P extends Poolable> void registerSharedObjectPool(Class<P> poolableType, ObjectPool<P> objectPool);

    /**
     * Get an already registered shared {@link ObjectPool} by {@link Poolable} type name.
     *
     * @param poolableType Object pool's {@link Poolable} type
     * @param <P> {@link Poolable} type
     * @return Object pool
     */
    <P extends Poolable> ObjectPool<P> getSharedObjectPool(Class<P> poolableType);


    /**
     * Unregister an {@link ObjectPool} for a {@link Poolable} type.
     *
     * @param poolableType poolableType Object pool's {@link Poolable} type
     * @param <P> Poolable type
     */
    <P extends Poolable> void unregisterSharedObjectPool(Class<P> poolableType);


    /**
     * Clear {@link ObjectPool} map.
     */
    void clearSharedObjectPools();


    /**
     * Get a read-only copy of all the removed message.
     *
     * @return Read only copy of all removed message
     */
    @Nullable
    Collection<Message> getRemovedMessages();


    /**
     * Clear the removed message collection. This can be used by a manager task that can handle removed message
     * using {@link #getRemovedMessages()} and then clear this collection.
     */
    void resetRemovedMessages();


    /**
     * Check if shutdown.
     *
     * @return true if shutdown, false otherwise
     */
    boolean isShutdown();


    /**
     * Shutdown.
     */
    void shutdown();
}
