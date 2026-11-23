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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MessageFactoryTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDown() {
        if (mediatorService != null) {
            mediatorService.shutdown();
        }

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
    void exceptionMessage() {
        final Message message = MessageFactory.exceptionMessage(
                mediatorService,
                getClass(),
                "message",
                AppbaseUtil.threadName(),
                new NullPointerException());
        Assertions.assertNotNull(message);

        final ExceptionMessage exceptionMessage = message.getExceptionMessage();
        Assertions.assertNotNull(exceptionMessage);
        Assertions.assertEquals(getClass(), exceptionMessage.getSourceType());
        Assertions.assertEquals("message", exceptionMessage.getMessage());
        final Throwable throwable = exceptionMessage.getThrowable();
        Assertions.assertNotNull(throwable);
        Assertions.assertEquals(NullPointerException.class, throwable.getClass());
    }

    @Test
    void exceptionMessage1() {
        final Map<String, String> context = new HashMap<>();
        context.put("key1", "value1");

        final Message message = MessageFactory.exceptionMessage(
                mediatorService,
                getClass(),
                "message",
                AppbaseUtil.threadName(),
                new NullPointerException(),
                context);
        Assertions.assertNotNull(message);

        final ExceptionMessage exceptionMessage = message.getExceptionMessage();
        Assertions.assertNotNull(exceptionMessage);
        Assertions.assertEquals(getClass(), exceptionMessage.getSourceType());
        Assertions.assertEquals("message", exceptionMessage.getMessage());
        final Throwable throwable = exceptionMessage.getThrowable();
        Assertions.assertNotNull(throwable);
        Assertions.assertEquals(NullPointerException.class, throwable.getClass());
        Assertions.assertEquals(context, exceptionMessage.getContext());
    }

    @Test
    void serviceStatusMessage() {
        final Message message = MessageFactory.serviceStatusMessage(
                mediatorService,
                MediatorService.class,
                ServiceStatus.STARTED);
        Assertions.assertNotNull(message);

        final ServiceStatusMessage serviceStatusMessage = message.getServiceStatusMessage();
        Assertions.assertNotNull(serviceStatusMessage);
        Assertions.assertEquals(MediatorService.class, serviceStatusMessage.getServiceType());
        Assertions.assertEquals(ServiceStatus.STARTED, serviceStatusMessage.getServiceStatus());
    }

    @Test
    void serviceStatusMessage1() {
        final Map<String, String> context = new HashMap<>();
        context.put("key1", "value1");
        final Message message = MessageFactory.serviceStatusMessage(
                mediatorService,
                MediatorService.class,
                ServiceStatus.STARTED,
                context);
        Assertions.assertNotNull(message);

        final ServiceStatusMessage serviceStatusMessage = message.getServiceStatusMessage();
        Assertions.assertNotNull(serviceStatusMessage);
        Assertions.assertEquals(MediatorService.class, serviceStatusMessage.getServiceType());
        Assertions.assertEquals(ServiceStatus.STARTED, serviceStatusMessage.getServiceStatus());
        Assertions.assertEquals(context, serviceStatusMessage.getContext());
    }
}
