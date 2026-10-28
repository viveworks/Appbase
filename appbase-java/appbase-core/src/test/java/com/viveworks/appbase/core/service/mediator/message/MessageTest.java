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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MessageTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = new DefaultValidationService(
                logService, metricService);
        validator = validationService.getValidator();
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
    void testMessageWithExceptionMessage() {
        final Message message = new Message();

        final long timestamp = System.currentTimeMillis();
        message.setTimestamp(timestamp);
        message.setMessageType(MessageType.EXCEPTION);
        final ExceptionMessage exceptionMessage = message.getExceptionMessage();
        final ExceptionMessage exceptionMessage1 = CoreTestFixtures.getExceptionMessage();
        exceptionMessage.setSourceType(exceptionMessage1.getSourceType());
        exceptionMessage.setThrowable(exceptionMessage1.getThrowable());
        exceptionMessage.setMessage(exceptionMessage1.getMessage());
        exceptionMessage.addAllContext(exceptionMessage1.getContext());

        Assertions.assertEquals(timestamp, message.getTimestamp());
        Assertions.assertEquals(MessageType.EXCEPTION, message.getMessageType());
        Assertions.assertNotNull(message.getExceptionMessage());
        Assertions.assertEquals(exceptionMessage, message.getExceptionMessage());

        final Message message1 = new Message(message);
        Assertions.assertEquals(message, message1);
        Assertions.assertEquals(message.hashCode(), message1.hashCode());
        Assertions.assertEquals(message.toString(), message1.toString());

        message.clean();
        Assertions.assertEquals(0, message.getTimestamp());
        Assertions.assertNull(message.getMessageType());
        Assertions.assertNotNull(message.getExceptionMessage());
        Assertions.assertNotNull(message.getServiceStatusMessage());
    }

    @Test
    void testNonPositiveTimestampValidation() {
        final Message message = new Message();
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> message.setTimestamp(-1));
    }

    @Test
    void testMessageWithoutMessageType() {
        final Message message = new Message();

        final long timestamp = System.currentTimeMillis();
        message.setTimestamp(timestamp);

        final Set<ConstraintViolation<Message>> violations = validator.validate(message);
        Assertions.assertNotNull(violations);
        Assertions.assertFalse(violations.isEmpty());
        final boolean validProperty = validProperty(
                violations,
                MessageValidator.MESSAGE_TYPE,
                MessageValidator.MESSAGE_TYPE_NULL);
        Assertions.assertFalse(validProperty);

        message.setMessageType(MessageType.SERVICE_STATUS);
        final Set<ConstraintViolation<Message>> violations1 = validator.validate(message);
        Assertions.assertNotNull(violations1);
        Assertions.assertFalse(violations1.isEmpty());
        final boolean validProperty1 = validProperty(
                violations1,
                MessageValidator.MESSAGE_TYPE,
                MessageValidator.MESSAGE_TYPE_NULL);
        Assertions.assertTrue(validProperty1);
    }

    @Test
    void testExceptionMessageValidation() {
        final ExceptionMessage exceptionMessage = CoreTestFixtures.getExceptionMessage();
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> exceptionMessage.setThrowable(null));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> exceptionMessage.setSourceType(null));
    }

    @Test
    void testServiceStatusMessageValidation() {
        final ServiceStatusMessage serviceStatusMessage = CoreTestFixtures.getServiceStatusMessage();
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> serviceStatusMessage.setServiceType(null));
    }

    private boolean validProperty(final Set<ConstraintViolation<Message>> violations,
                                  final String propertyNode,
                                  final String violationTemplate)
    {

        boolean valid = true;
        for (final ConstraintViolation<Message> violation : violations) {
            final Path propertyPath = violation.getPropertyPath();
            if (propertyNode.equals(propertyPath.toString()) &&
                    violation.getMessageTemplate().equals(violationTemplate))
            {
                valid = false;
            }
        }

        return valid;
    }
}
