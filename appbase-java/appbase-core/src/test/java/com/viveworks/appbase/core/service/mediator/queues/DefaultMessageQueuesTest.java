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
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.message.ServiceStatusMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Queue;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMessageQueuesTest {
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
    void testBoundMessageQueue() {
        final DefaultMessageQueues boundDefaultMessageQueues =
                new DefaultMessageQueues(
                        metricService,
                        CoreTestFixtures.getBoundMessageQueuesConfiguration());
        assertQueue(boundDefaultMessageQueues, true);
    }

    @Test
    void testUnboundMessageQueue() {
        final DefaultMessageQueues unboundDefaultMessageQueues =
                new DefaultMessageQueues(
                        metricService,
                        CoreTestFixtures.getUnboundMessageQueuesConfiguration());
        assertQueue(unboundDefaultMessageQueues, false);
    }

    private void assertQueue(final DefaultMessageQueues defaultMessageQueues,
                             final boolean bound)
    {
        final Queue<Message> lifecycleMessageQueue1 = defaultMessageQueues.getMessageQueue(MessageType.EXCEPTION);
        Assertions.assertNull(lifecycleMessageQueue1);

        for (int i = 0; i < CoreTestFixtures.BOUND_MESSAGE_QUEUE_SIZE; ++i) {
            final Message message = CoreTestFixtures.getMessageWithServiceStatusMessage();
            defaultMessageQueues.enqueueMessage(message);
        }

        final Queue<Message> serviceStatusMessageQueue2 =
                defaultMessageQueues.getMessageQueue(MessageType.SERVICE_STATUS);
        Assertions.assertNotNull(serviceStatusMessageQueue2);
        Assertions.assertEquals(
                CoreTestFixtures.BOUND_MESSAGE_QUEUE_SIZE, serviceStatusMessageQueue2.size());

        if (bound) {
            Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
                final Message message = CoreTestFixtures.getMessageWithServiceStatusMessage();
                defaultMessageQueues.enqueueMessage(message);
            });
        }

        final Queue<Message> serviceStatusMessageQueue3 =
                defaultMessageQueues.getMessageQueue(MessageType.SERVICE_STATUS);
        Assertions.assertNotNull(serviceStatusMessageQueue3);
        for (int i = 0; i < CoreTestFixtures.BOUND_MESSAGE_QUEUE_SIZE; ++i) {
            final Message polledMessage = serviceStatusMessageQueue3.poll();
            Assertions.assertNotNull(polledMessage);

            final MessageType messageType = polledMessage.getMessageType();
            Assertions.assertNotNull(messageType);
            Assertions.assertEquals(MessageType.SERVICE_STATUS, messageType);

            final long timestamp = polledMessage.getTimestamp();
            Assertions.assertTrue(timestamp > 0);

            final ServiceStatusMessage serviceStatusMessage = polledMessage.getServiceStatusMessage();
            Assertions.assertNotNull(serviceStatusMessage);

            final Class<? extends AppbaseService> serviceType = serviceStatusMessage.getServiceType();
            Assertions.assertNotNull(serviceType);

            final ServiceStatus serviceStatus = serviceStatusMessage.getServiceStatus();
            Assertions.assertNotNull(serviceStatus);
        }

        defaultMessageQueues.shutdown();
    }
}
