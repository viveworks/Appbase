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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.event.LogEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventListener;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageObjectFactory;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.message.ServiceStatusMessage;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.objectpool.ObjectPool;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultMediatorTest {
    private static final AtomicInteger DISRUPTOR_EVENT_COUNTER = new AtomicInteger(0);
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static Mediator mediator;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediator = CoreTestFixtures.getMediator(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediator != null) {
            mediator.shutdown();
        }
    }

    @Test
    @Order(1)
    void testDefaultMediator() throws Exception {
        // Assert initial map states
        Assertions.assertNotNull(mediator.getRemovedMessages());
        Assertions.assertTrue(mediator.getRemovedMessages().isEmpty());


        // Assert syncHandleMessage ignores
        final Message message1 = mediator.getMessageFromPool();
        Assertions.assertNotNull(message1);
        try {
            message1.clean();
            message1.setTimestamp(System.currentTimeMillis());
            message1.setMessageType(MessageType.EXCEPTION);

            final ExceptionMessage exceptionMessage = message1.getExceptionMessage();
            exceptionMessage.setThrowable(new NullPointerException());
            exceptionMessage.setSourceType(getClass());
            exceptionMessage.setMessage("Exception message");

            Assertions.assertThrows(MediatorException.class, () -> mediator.syncHandleMessage(message1));
        } finally {
            mediator.putMessageToPool(message1);
        }


        // Assert clean state
        mediator.resetRemovedMessages();
        Assertions.assertNotNull(mediator.getRemovedMessages());
        Assertions.assertTrue(mediator.getRemovedMessages().isEmpty());


        // Assert syncHandleMessage without MessageHandler
        final Message message2 = mediator.getMessageFromPool();
        try {
            message2.clean();
            message2.setTimestamp(System.currentTimeMillis());
            message2.setMessageType(MessageType.SERVICE_STATUS);
            final ServiceStatusMessage serviceStatusMessage = message2.getServiceStatusMessage();
            final ServiceStatusMessage serviceStatusMessage1 = CoreTestFixtures.getServiceStatusMessage();
            serviceStatusMessage.setServiceStatus(serviceStatusMessage1.getServiceStatus());
            serviceStatusMessage.setServiceType(serviceStatusMessage1.getServiceType());
            serviceStatusMessage.addAllContext(serviceStatusMessage1.getContext());
            Assertions.assertDoesNotThrow(() -> mediator.syncHandleMessage(message2));
        } finally {
            mediator.putMessageToPool(message2);
        }


        // Assert syncHandleMessage with MessageHandler
        final MessageHandler messageHandler = new CoreTestFixtures.TestMessageHandler();
        Assertions.assertDoesNotThrow(() ->
                mediator.registerMessageHandlerByMessageType(MessageType.SERVICE_STATUS, messageHandler));


        // Async. message handling
        final Message message8 = mediator.getMessageFromPool();
        message8.clean();
        message8.setTimestamp(System.currentTimeMillis());
        message8.setMessageType(MessageType.SERVICE_STATUS);
        final ServiceStatusMessage serviceStatusMessage111 = message8.getServiceStatusMessage();
        final ServiceStatusMessage serviceStatusMessage112 = CoreTestFixtures.getServiceStatusMessage();
        serviceStatusMessage111.setServiceStatus(serviceStatusMessage112.getServiceStatus());
        serviceStatusMessage111.setServiceType(serviceStatusMessage112.getServiceType());
        serviceStatusMessage111.addAllContext(serviceStatusMessage112.getContext());
        Assertions.assertDoesNotThrow(() -> mediator.asyncHandleMessage(message8, true));

        final Map<MessageType, MessageHandler> messageHandlerByMessageTypeMap1 =
                mediator.getMessageHandlerByMessageTypeMap();
        Assertions.assertNotNull(messageHandlerByMessageTypeMap1);

        final MessageHandler gotMessageHandler1 = messageHandlerByMessageTypeMap1.get(MessageType.SERVICE_STATUS);
        Assertions.assertNotNull(gotMessageHandler1);
        Assertions.assertEquals(CoreTestFixtures.TestMessageHandler.class, gotMessageHandler1.getClass());

        final Message message3 = mediator.getMessageFromPool();
        try {
            message3.clean();
            message3.setTimestamp(System.currentTimeMillis());
            message3.setMessageType(MessageType.SERVICE_STATUS);
            final ServiceStatusMessage serviceStatusMessage = message3.getServiceStatusMessage();
            final ServiceStatusMessage serviceStatusMessage1 = CoreTestFixtures.getServiceStatusMessage();
            serviceStatusMessage.setServiceStatus(serviceStatusMessage1.getServiceStatus());
            serviceStatusMessage.setServiceType(serviceStatusMessage1.getServiceType());
            serviceStatusMessage.addAllContext(serviceStatusMessage1.getContext());
            Assertions.assertDoesNotThrow(() -> mediator.syncHandleMessage(message3));
        } finally {
            mediator.putMessageToPool(message3);
        }


        // Assert syncHandleMessage with exception in MessageHandler
        Assertions.assertDoesNotThrow(() -> mediator.registerMessageHandlerByMessageType(
                MessageType.SERVICE_STATUS, new CoreTestFixtures.ThrowingMessageHandler()));
        final Message message4 = mediator.getMessageFromPool();
        try {
            message4.clean();
            message4.setTimestamp(System.currentTimeMillis());
            message4.setMessageType(MessageType.SERVICE_STATUS);
            final ServiceStatusMessage serviceStatusMessage = message4.getServiceStatusMessage();
            final ServiceStatusMessage serviceStatusMessage1 = CoreTestFixtures.getServiceStatusMessage();
            serviceStatusMessage.setServiceStatus(serviceStatusMessage1.getServiceStatus());
            serviceStatusMessage.setServiceType(serviceStatusMessage1.getServiceType());
            serviceStatusMessage.addAllContext(serviceStatusMessage1.getContext());
            Assertions.assertThrows(MediatorException.class, () -> mediator.syncHandleMessage(message4));
        } finally {
            mediator.putMessageToPool(message4);
        }


        // Assert registerMessageHandlerByMessageType ignores state
        Assertions.assertThrows(
                MediatorException.class,
                () -> mediator.registerMessageHandlerByMessageType(
                        MessageType.EXCEPTION, new CoreTestFixtures.TestMessageHandler()));


        // Assert registerMessageHandlerByMessageType that throws
        final Message message5 = mediator.getMessageFromPool();
        try {
            message5.setTimestamp(System.currentTimeMillis());
            message5.setMessageType(MessageType.EXCEPTION);
            Assertions.assertThrows(MediatorException.class, () -> mediator.syncHandleMessage(message5));
        } finally {
            mediator.putMessageToPool(message5);
        }
        final MessageHandler messageHandler1 = new CoreTestFixtures.ThrowingMessageHandler();
        Assertions.assertDoesNotThrow(() -> mediator.registerMessageHandlerByMessageType(
                MessageType.SERVICE_STATUS, messageHandler1));


        // Async. message handling
        final Message message7 = mediator.getMessageFromPool();
        message7.clean();
        message7.setTimestamp(System.currentTimeMillis());
        message7.setMessageType(MessageType.SERVICE_STATUS);
        final ServiceStatusMessage serviceStatusMessage11 = message7.getServiceStatusMessage();
        final ServiceStatusMessage serviceStatusMessage12 = CoreTestFixtures.getServiceStatusMessage();
        serviceStatusMessage11.setServiceStatus(serviceStatusMessage12.getServiceStatus());
        serviceStatusMessage11.setServiceType(serviceStatusMessage12.getServiceType());
        serviceStatusMessage11.addAllContext(serviceStatusMessage12.getContext());
        Assertions.assertDoesNotThrow(() -> mediator.asyncHandleMessage(message7, true));

        // Assert executors
        final CoreTestFixtures.TestsSchedulable schedulable = new CoreTestFixtures.TestsSchedulable();
        final ScheduledFuture<?> scheduledFuture =
                mediator.scheduleWithFixedDelay(schedulable, 0, 10, TimeUnit.MICROSECONDS);
        Assertions.assertNotNull(scheduledFuture);

        final ExecutorService sharedExecutorService = mediator.getSharedExecutorService();
        Assertions.assertNotNull(sharedExecutorService);

        // Check shared executors
        Assertions.assertNotNull(mediator.getSharedScheduledExecutorService());
        Assertions.assertNotNull(mediator.getSharedDisruptorService());
        Assertions.assertNotNull(mediator.getSharedMediatorEventDelegatorRegistry());
        Assertions.assertNotNull(mediator.getSharedExecutorService());

        // Check publishing
        mediator.getSharedMediatorEventDelegatorRegistry().registerMediatorEventListener(
                LogEvent.class,
                new TestMediatorEventListener());
        final LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setMessage("Log Message");
        loggingEvent.setLevel(Level.INFO);
        final MediatorEvent mediatorEvent = new MediatorEvent();
        mediatorEvent.reset();
        mediatorEvent.setMediatorEventType(LogEvent.class);
        mediatorEvent.getLogEvent().setLoggingEvent(loggingEvent);
        mediator.publishMediatorEvent(mediatorEvent);
        Thread.sleep(3000);
        Assertions.assertEquals(1, DISRUPTOR_EVENT_COUNTER.get());


        // Assert shutdown state
        mediator.shutdown();
        Assertions.assertTrue(mediator.getRemovedMessages().isEmpty());
        Assertions.assertTrue(mediator.isShutdown());
    }

    @Test
    @Order(2)
    void testSharedObjectPool() {
        final ObjectPool<Message> messageObjectPool =
                new ManyToManyQueuedFixedObjectPool<>(metricService, 4, new MessageObjectFactory());

        mediator.registerSharedObjectPool(Message.class, messageObjectPool);
        Assertions.assertNotNull(mediator.getSharedObjectPool(Message.class));

        mediator.unregisterSharedObjectPool(Message.class);
        Assertions.assertNull(mediator.getSharedObjectPool(Message.class));
        mediator.registerSharedObjectPool(Message.class, messageObjectPool);
        Assertions.assertNotNull(mediator.getSharedObjectPool(Message.class));

        mediator.clearSharedObjectPools();
        Assertions.assertNull(mediator.getSharedObjectPool(Message.class));
    }

    @Test
    @Order(3)
    void testCollections() throws Exception {
        mediator.clearMessageHandlerByMessageTypeMap();

        mediator.registerMessageHandlerByMessageType(MessageType.EXCEPTION, new TestMessageHandler());
        final Map<MessageType, MessageHandler> messageHandlerByMessageTypeMap1 =
                mediator.getMessageHandlerByMessageTypeMap();
        Assertions.assertNotNull(messageHandlerByMessageTypeMap1);
        Assertions.assertEquals(1, messageHandlerByMessageTypeMap1.size());

        mediator.unregisterMessageHandlerByMessageType(MessageType.EXCEPTION);
        final Map<MessageType, MessageHandler> messageHandlerByMessageTypeMap2 =
                mediator.getMessageHandlerByMessageTypeMap();
        Assertions.assertNotNull(messageHandlerByMessageTypeMap2);
        Assertions.assertTrue(messageHandlerByMessageTypeMap2.isEmpty());

        mediator.registerMessageHandlerByMessageType(MessageType.EXCEPTION, new TestMessageHandler());
        final Map<MessageType, MessageHandler> messageHandlerByMessageTypeMap3 =
                mediator.getMessageHandlerByMessageTypeMap();
        Assertions.assertNotNull(messageHandlerByMessageTypeMap3);
        Assertions.assertEquals(1, messageHandlerByMessageTypeMap3.size());

        mediator.clearMessageHandlerByMessageTypeMap();
        final Map<MessageType, MessageHandler> messageHandlerByMessageTypeMap4 =
                mediator.getMessageHandlerByMessageTypeMap();
        Assertions.assertNotNull(messageHandlerByMessageTypeMap4);
        Assertions.assertTrue(messageHandlerByMessageTypeMap4.isEmpty());
    }

    public static class TestMediatorEventListener implements MediatorEventListener {
        @Override
        public void onMediatorEvent(final MediatorEvent mediatorEvent) {
            Assertions.assertNotNull(mediatorEvent);
            Assertions.assertEquals(LogEvent.class, mediatorEvent.getMediatorEventType());
            Assertions.assertNull(mediatorEvent.getTurboLogEvent().getTurboLog());
            Assertions.assertNull(mediatorEvent.getCustomEvent().getEvent());

            final ILoggingEvent loggingEvent = mediatorEvent.getLogEvent().getLoggingEvent();
            Assertions.assertNotNull(loggingEvent);
            Assertions.assertEquals("Log Message", loggingEvent.getMessage());
            Assertions.assertEquals(Level.INFO, loggingEvent.getLevel());

            DISRUPTOR_EVENT_COUNTER.incrementAndGet();
        }
    }

    private static class TestMessageHandler implements MessageHandler {
        @Override
        public void onMessage(final Message message) throws MessageHandlerException {
        }
    }
}
