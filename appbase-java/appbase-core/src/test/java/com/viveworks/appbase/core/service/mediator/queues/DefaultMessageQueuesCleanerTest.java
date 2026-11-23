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
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MessageHandler;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.test.CoreTestUtil;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMessageQueuesCleanerTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testCleanOrSchedule() throws Exception {
        final DefaultMessageQueues messageQueues =
                new DefaultMessageQueues(
                        metricService,
                        CoreTestFixtures.getBoundMessageQueuesConfiguration());
        final Map<MessageType, MessageHandler> sharedMessageHandlerByMessageTypeMap = new ConcurrentHashMap<>();
        final DefaultMessageQueuesCleaner defaultMessageQueuesCleaner = new DefaultMessageQueuesCleaner(
                metricService,
                messageQueues,
                sharedMessageHandlerByMessageTypeMap,
                CoreTestFixtures.OLD_MESSAGE_THRESHOLD_MILLIS,
                new ReentrantLock(),
                new ManyToManyConcurrentArrayQueue<>(4),
                new ManyToManyConcurrentArrayQueue<>(4),
                SchedulableConfiguration.builder().build());

        // Make sure droppedMessagesQueue is empty
        assertCleanOrSchedule(defaultMessageQueuesCleaner, true);


        // Not expired, No MessageHandler -> Re-queued
        messageQueues.enqueueMessage(CoreTestFixtures.getMessageWithServiceStatusMessage());
        defaultMessageQueuesCleaner.clean();
        final Queue<Message> serviceStatusQueue = messageQueues.getMessageQueue(MessageType.SERVICE_STATUS);
        Assertions.assertNotNull(serviceStatusQueue);
        Assertions.assertFalse(serviceStatusQueue.isEmpty());
        Assertions.assertEquals(1, serviceStatusQueue.size());
        assertCleanOrSchedule(defaultMessageQueuesCleaner, true);


        // Expired, No MessageHandler -> Dropped
        defaultMessageQueuesCleaner.resetRemovedMessages();
        Thread.sleep(CoreTestFixtures.OLD_MESSAGE_THRESHOLD_MILLIS + 100);
        final Void schedule1 = defaultMessageQueuesCleaner.schedule();
        Assertions.assertNull(schedule1);
        assertCleanOrSchedule(defaultMessageQueuesCleaner, false);


        // Has MessageHandler, Not expired -> Handled
        final CoreTestFixtures.TestMessageHandler messageHandler =
                new CoreTestFixtures.TestMessageHandler();
        sharedMessageHandlerByMessageTypeMap.put(MessageType.SERVICE_STATUS, messageHandler);
        messageQueues.enqueueMessage(CoreTestFixtures.getMessageWithServiceStatusMessage());
        defaultMessageQueuesCleaner.clean();
        Assertions.assertTrue(messageHandler.getHandled().get());


        // Has expired, Has MessageHandler -> Dropped
        defaultMessageQueuesCleaner.resetRemovedMessages();
        final Message message3 = CoreTestFixtures.getMessageWithServiceStatusMessage();
        Thread.sleep(CoreTestFixtures.OLD_MESSAGE_THRESHOLD_MILLIS);
        messageQueues.enqueueMessage(message3);
        final Void schedule2 = defaultMessageQueuesCleaner.schedule();
        Assertions.assertNull(schedule2);
        assertCleanOrSchedule(defaultMessageQueuesCleaner, false);

        defaultMessageQueuesCleaner.resetRemovedMessages();
        assertCleanOrSchedule(defaultMessageQueuesCleaner, true);

        Assertions.assertNotNull(defaultMessageQueuesCleaner.getSchedulableConfiguration());
    }

    @Test
    void testExceptionHandlingInClean() {
        final DefaultMessageQueues messageQueues =
                new DefaultMessageQueues(
                        metricService,
                        CoreTestFixtures.getBoundMessageQueuesConfiguration());
        final Map<MessageType, MessageHandler> sharedMessageHandlerByMessageTypeMap = new ConcurrentHashMap<>();
        final DefaultMessageQueuesCleaner defaultMessageQueuesCleaner = new DefaultMessageQueuesCleaner(
                metricService,
                messageQueues,
                sharedMessageHandlerByMessageTypeMap,
                CoreTestFixtures.OLD_MESSAGE_THRESHOLD_MILLIS,
                new ReentrantLock(),
                new ManyToManyConcurrentArrayQueue<>(4),
                new ManyToManyConcurrentArrayQueue<>(4),
                SchedulableConfiguration.builder().build());
        final Collection<AppbaseRuntimeException> appbaseRuntimeExceptions =
                defaultMessageQueuesCleaner.getAppbaseRuntimeExceptions();
        Assertions.assertNotNull(appbaseRuntimeExceptions);
        Assertions.assertTrue(appbaseRuntimeExceptions.isEmpty());

        final CoreTestFixtures.ThrowingMessageHandler messageHandler =
                new CoreTestFixtures.ThrowingMessageHandler();
        sharedMessageHandlerByMessageTypeMap.put(MessageType.SERVICE_STATUS, messageHandler);
        messageQueues.enqueueMessage(CoreTestFixtures.getMessageWithServiceStatusMessage());

        defaultMessageQueuesCleaner.clean();
        final Collection<AppbaseRuntimeException> appbaseRuntimeExceptions1 =
                defaultMessageQueuesCleaner.getAppbaseRuntimeExceptions();
        Assertions.assertNotNull(appbaseRuntimeExceptions1);
        Assertions.assertFalse(appbaseRuntimeExceptions1.isEmpty());

        defaultMessageQueuesCleaner.resetAppbaseRuntimeExceptions();
        final Collection<AppbaseRuntimeException> appbaseRuntimeExceptions2 =
                defaultMessageQueuesCleaner.getAppbaseRuntimeExceptions();
        Assertions.assertNotNull(appbaseRuntimeExceptions2);
        Assertions.assertTrue(appbaseRuntimeExceptions2.isEmpty());
    }

    private void assertCleanOrSchedule(final DefaultMessageQueuesCleaner defaultMessageQueuesCleaner,
                                       final boolean condition)
    {
        final Collection<Message> droppedMessages = defaultMessageQueuesCleaner.getRemovedMessages();
        Assertions.assertNotNull(droppedMessages);

        if (condition) {
            Assertions.assertTrue(droppedMessages.isEmpty());
        } else {
            Assertions.assertFalse(droppedMessages.isEmpty());
        }
    }

    @Test
    void testShouldRemoveMessage() throws Exception {
        final DefaultMessageQueues messageQueues =
                new DefaultMessageQueues(
                        metricService,
                        CoreTestFixtures.getBoundMessageQueuesConfiguration());
        final DefaultMessageQueuesCleaner messageQueueCleaner = new DefaultMessageQueuesCleaner(
                metricService,
                messageQueues,
                new ConcurrentHashMap<>(),
                CoreTestFixtures.OLD_MESSAGE_THRESHOLD_MILLIS,
                new ReentrantLock(),
                new ManyToManyConcurrentArrayQueue<>(4),
                new ManyToManyConcurrentArrayQueue<>(4),
                SchedulableConfiguration.builder().build());
        final Method shouldRemoveMessageMethod =
                CoreTestUtil.getMethod(messageQueueCleaner.getClass(), "shouldRemoveMessage", long.class, long.class);

        final long expiredMessageThresholdMillis = CoreTestFixtures.OLD_MESSAGE_THRESHOLD_MILLIS;
        assertShouldRemoveMessage(
                true, shouldRemoveMessageMethod, messageQueueCleaner, 0L, expiredMessageThresholdMillis);
        assertShouldRemoveMessage(
                true, shouldRemoveMessageMethod, messageQueueCleaner, expiredMessageThresholdMillis, 0L);
        assertShouldRemoveMessage(
                true, shouldRemoveMessageMethod, messageQueueCleaner, 0L, 0L);
        assertShouldRemoveMessage(
                true, shouldRemoveMessageMethod, messageQueueCleaner, -1, expiredMessageThresholdMillis);
        assertShouldRemoveMessage(
                true, shouldRemoveMessageMethod, messageQueueCleaner, expiredMessageThresholdMillis, -1);
        assertShouldRemoveMessage(
                true,
                shouldRemoveMessageMethod,
                messageQueueCleaner,
                expiredMessageThresholdMillis,
                expiredMessageThresholdMillis * 2);
        assertShouldRemoveMessage(
                true,
                shouldRemoveMessageMethod,
                messageQueueCleaner,
                expiredMessageThresholdMillis * 2,
                expiredMessageThresholdMillis);
        assertShouldRemoveMessage(
                true,
                shouldRemoveMessageMethod,
                messageQueueCleaner,
                expiredMessageThresholdMillis * 2 + 1,
                expiredMessageThresholdMillis);
        assertShouldRemoveMessage(
                true,
                shouldRemoveMessageMethod,
                messageQueueCleaner,
                expiredMessageThresholdMillis * 2,
                expiredMessageThresholdMillis - 1);
        assertShouldRemoveMessage(
                false,
                shouldRemoveMessageMethod,
                messageQueueCleaner,
                expiredMessageThresholdMillis * 2 - 1,
                expiredMessageThresholdMillis);
        assertShouldRemoveMessage(
                false,
                shouldRemoveMessageMethod,
                messageQueueCleaner,
                expiredMessageThresholdMillis * 2,
                expiredMessageThresholdMillis + 1);
    }

    private void assertShouldRemoveMessage(final boolean condition,
                                           final Method shouldRemoveMessageMethod,
                                           final MessageQueuesCleaner messageQueueCleaner,
                                           final long now,
                                           final long messageTimestamp) throws Exception
    {
        final boolean result =
                CoreTestUtil.invokeMethod(shouldRemoveMessageMethod, messageQueueCleaner, now, messageTimestamp);
        if (condition) {
            Assertions.assertTrue(result);
        } else {
            Assertions.assertFalse(result);
        }
    }
}
