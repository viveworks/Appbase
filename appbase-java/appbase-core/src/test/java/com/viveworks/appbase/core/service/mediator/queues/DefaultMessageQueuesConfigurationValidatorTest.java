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

package com.viveworks.appbase.core.service.mediator.queues;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMessageQueuesConfigurationValidatorTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
    }

    @AfterAll
    static void tearDown() {
        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testValidator() {
        final DefaultMessageQueuesConfigurationValidator defaultMessageQueuesConfigurationValidator1 =
                new DefaultMessageQueuesConfigurationValidator();
        final DefaultMessageQueuesConfigurationValidator defaultMessageQueuesConfigurationValidator2 =
                new DefaultMessageQueuesConfigurationValidator(validationService);
        final DefaultMessageQueuesConfiguration messageQueuesConfiguration =
                DefaultMessageQueuesConfiguration.builder().build();

        Assertions.assertTrue(
                defaultMessageQueuesConfigurationValidator1.isValid(messageQueuesConfiguration, null));
        Assertions.assertTrue(
                defaultMessageQueuesConfigurationValidator2.isValid(messageQueuesConfiguration, null));

        Assertions.assertFalse(
                defaultMessageQueuesConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                defaultMessageQueuesConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                DefaultMessageQueuesConfigurationValidator.validate(
                        messageQueuesConfiguration.getMessageQueueType(),
                        messageQueuesConfiguration.getBoundMessageQueueCapacity(),
                        messageQueuesConfiguration.isRemoveExpiredMessages(),
                        messageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                        messageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis(),
                        messageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis()));
        Assertions.assertDoesNotThrow(() ->
                DefaultMessageQueuesConfigurationValidator.validate(
                        messageQueuesConfiguration.getMessageQueueType(),
                        messageQueuesConfiguration.getBoundMessageQueueCapacity(),
                        messageQueuesConfiguration.isRemoveExpiredMessages(),
                        messageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                        messageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis(),
                        messageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultMessageQueuesConfigurationValidator.validate(
                            null,
                            messageQueuesConfiguration.getBoundMessageQueueCapacity(),
                            messageQueuesConfiguration.isRemoveExpiredMessages(),
                            messageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                            messageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis(),
                            messageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultMessageQueuesConfigurationValidator.validate(
                            MessageQueueType.BOUND,
                            0,
                            messageQueuesConfiguration.isRemoveExpiredMessages(),
                            messageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                            messageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis(),
                            messageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultMessageQueuesConfigurationValidator.validate(
                            messageQueuesConfiguration.getMessageQueueType(),
                            messageQueuesConfiguration.getBoundMessageQueueCapacity(),
                            true,
                            0,
                            messageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis(),
                            messageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultMessageQueuesConfigurationValidator.validate(
                            messageQueuesConfiguration.getMessageQueueType(),
                            messageQueuesConfiguration.getBoundMessageQueueCapacity(),
                            true,
                            messageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                            -1,
                            messageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultMessageQueuesConfigurationValidator.validate(
                            messageQueuesConfiguration.getMessageQueueType(),
                            messageQueuesConfiguration.getBoundMessageQueueCapacity(),
                            true,
                            messageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                            messageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis(),
                            0);
                });
    }
}
