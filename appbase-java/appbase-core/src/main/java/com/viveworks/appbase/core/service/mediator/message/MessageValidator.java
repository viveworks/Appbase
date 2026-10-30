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

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * JSR 303 validation for {@link Message}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressWarnings({"checkstyle:DeclarationOrder", "WeakerAccess"})
public class MessageValidator implements ConstraintValidator<Message.Valid, Message> {
    private static final String MESSAGE = "message";
    static final String TIMESTAMP = "timestamp";
    static final String MESSAGE_TYPE = "messageType";
    static final String EXCEPTION_MESSAGE = "exceptionMessage";
    static final String SERVICE_STATUS_MESSAGE = "serviceStatusMessage";
    static final String EXCEPTION_MESSAGE_SOURCE = "exceptionMessage.source";
    static final String EXCEPTION_MESSAGE_THROWABLE = "exceptionMessage.throwable";

    private static final String PREFIX =
            ValidationService.prefix(Message.class);

    private static final String MESSAGE_NULL =
            ValidationService.concat(PREFIX, ValidationService.NULL);

    static final String MESSAGE_NON_POSITIVE_TIMESTAMP =
            ValidationService.concat(PREFIX, ValidationService.NOT_POSITIVE);

    static final String MESSAGE_ALL_MESSAGES_NULL =
            ValidationService.concat(PREFIX, "allMessages", ValidationService.NULL);

    static final String MESSAGE_TYPE_NULL =
            ValidationService.concat(PREFIX, "messageType", ValidationService.NULL);

    static final String MESSAGE_TYPE_UNKNOWN =
            ValidationService.concat(PREFIX, "messageType", "unknown");

    static final String EXCEPTION_MESSAGE_INVALID =
            ValidationService.concat(PREFIX, EXCEPTION_MESSAGE, ValidationService.INVALID);

    static final String EXCEPTION_MESSAGE_NULL_SOURCE =
            ValidationService.concat(PREFIX, EXCEPTION_MESSAGE, "source", ValidationService.NULL);

    static final String EXCEPTION_MESSAGE_NULL_THROWABLE =
            ValidationService.concat(PREFIX, EXCEPTION_MESSAGE, "throwable", ValidationService.NULL);

    static final String SERVICE_STATUS_MESSAGE_INVALID =
            ValidationService.concat(PREFIX, SERVICE_STATUS_MESSAGE, ValidationService.INVALID);

    static final String SERVICE_STATUS_MESSAGE_NULL_SERVICE_TYPE =
            ValidationService.concat(PREFIX, SERVICE_STATUS_MESSAGE, "serviceType", ValidationService.NULL);

    static final String SERVICE_STATUS_MESSAGE_NULL_SERVICE_STATUS =
            ValidationService.concat(PREFIX, SERVICE_STATUS_MESSAGE, "serviceStatus", ValidationService.NULL);

    private void addConstraintViolation(
            @Nullable final ConstraintValidatorContext context,
            final String template,
            final String propertyNode)
    {
        if (context != null) {
            context.buildConstraintViolationWithTemplate(template)
                    .addPropertyNode(propertyNode)
                    .addConstraintViolation();
        }
    }

    @Override
    public boolean isValid(final Message message, @Nullable final ConstraintValidatorContext context) {
        if (message == null) {
            addConstraintViolation(context, MESSAGE_NULL, MESSAGE);
            return false;
        }

        boolean result = true;
        if (message.getTimestamp() < 1) {
            addConstraintViolation(context, MESSAGE_NON_POSITIVE_TIMESTAMP, TIMESTAMP);
            result = false;
        }

        final ExceptionMessage exceptionMessage = message.getExceptionMessage();
        final ServiceStatusMessage serviceStatusMessage = message.getServiceStatusMessage();
        if (exceptionMessage == null && serviceStatusMessage == null) {
            addConstraintViolation(context, MESSAGE_ALL_MESSAGES_NULL, MESSAGE);
            return false;
        }

        if (message.getMessageType() == null) {
            addConstraintViolation(context, MESSAGE_TYPE_NULL, MESSAGE_TYPE);
            return false;
        }

        switch (message.getMessageType()) {
            case EXCEPTION:
                final boolean validExceptionMessage = isValidExceptionMessage(message.getExceptionMessage(), context);
                if (!validExceptionMessage) {
                    addConstraintViolation(context, EXCEPTION_MESSAGE_INVALID, EXCEPTION_MESSAGE);
                    result = false;
                }
                break;
            case SERVICE_STATUS:
                final boolean validServiceStatusMessage =
                        isValidServiceStatusMessage(message.getServiceStatusMessage(), context);
                if (!validServiceStatusMessage) {
                    addConstraintViolation(context, SERVICE_STATUS_MESSAGE_INVALID, SERVICE_STATUS_MESSAGE);
                    result = false;
                }
                break;
            default:
                addConstraintViolation(context, MESSAGE_TYPE_UNKNOWN, MESSAGE_TYPE);
                result = false;
                break;
        }

        return result;
    }

    private boolean isValidExceptionMessage(final ExceptionMessage exceptionMessage,
                                            final ConstraintValidatorContext context)
    {
        boolean result = true;
        final Class<?> source = exceptionMessage.getSourceType();
        if (source == null) {
            addConstraintViolation(context, EXCEPTION_MESSAGE_NULL_SOURCE, EXCEPTION_MESSAGE_SOURCE);
            result = false;
        }

        final Throwable throwable = exceptionMessage.getThrowable();
        if (throwable == null) {
            addConstraintViolation(context, EXCEPTION_MESSAGE_NULL_THROWABLE, EXCEPTION_MESSAGE_THROWABLE);
            result = false;
        }

        return result;
    }

    private boolean isValidServiceStatusMessage(final ServiceStatusMessage serviceStatusMessage,
                                                final ConstraintValidatorContext context)
    {
        boolean result = true;
        final ServiceStatus serviceStatus = serviceStatusMessage.getServiceStatus();
        if (serviceStatus == null) {
            addConstraintViolation(context, SERVICE_STATUS_MESSAGE_NULL_SERVICE_STATUS, SERVICE_STATUS_MESSAGE);
            result = false;
        }

        final Class<? extends AppbaseService> serviceType = serviceStatusMessage.getServiceType();
        if (serviceType == null) {
            addConstraintViolation(context, SERVICE_STATUS_MESSAGE_NULL_SERVICE_TYPE, SERVICE_STATUS_MESSAGE);
            result = false;
        }

        return result;
    }
}
