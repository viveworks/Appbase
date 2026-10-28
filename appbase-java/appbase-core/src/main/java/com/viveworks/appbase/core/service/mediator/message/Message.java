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

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.Poolable;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.concurrent.NotThreadSafe;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * The mediator message. This class is designed to be poolable using
 * {@link com.viveworks.appbase.core.util.objectpool.ObjectPool}.
 *
 * <p>
 * Typical usage:
 * </p>
 *
 * <pre>
 * final Message message = mediatorService
 *     .getMediator()
 *     .getMessageFromPool();
 * try {
 *     message.clean();
 *     message.setTimestamp(System.currentTimeMillis());
 *     ...
 *
 *     final boolean result = mediatorService
 *         .getMediator()
 *         .syncHandleMessage(message);
 * } finally {
 *     mediatorService
 *         .getMediator()
 *         .putMessageToPool(message);
 * }
 * </pre>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
@Message.Valid
public final class Message implements Poolable {
    private long timestamp;
    private MessageType messageType;
    private final ExceptionMessage exceptionMessage;
    private final ServiceStatusMessage serviceStatusMessage;

    public Message() {
        this.exceptionMessage = new ExceptionMessage();
        this.serviceStatusMessage = new ServiceStatusMessage();
    }

    public Message(final Message message) {
        this.timestamp = message.timestamp;
        this.messageType = message.messageType;
        this.exceptionMessage = message.exceptionMessage;
        this.serviceStatusMessage = message.serviceStatusMessage;
    }

    public Message(final long timestamp,
                   final MessageType messageType,
                   final ExceptionMessage exceptionMessage,
                   final ServiceStatusMessage serviceStatusMessage)
    {
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.exceptionMessage = exceptionMessage;
        this.serviceStatusMessage = serviceStatusMessage;
    }

    /**
     * Set all fields.
     *
     * @param timestamp Timestamp
     * @param messageType {@link MessageType}
     */
    public void setAll(final long timestamp,
                       final MessageType messageType)
    {
        setTimestamp(timestamp);
        setMessageType(messageType);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        AppbaseUtil.positiveOrZero(timestamp, "timestamp");

        this.timestamp = timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(final MessageType messageType) {
        AppbaseUtil.notNull(messageType, "messageType");

        this.messageType = messageType;
    }

    public ExceptionMessage getExceptionMessage() {
        return exceptionMessage;
    }

    public ServiceStatusMessage getServiceStatusMessage() {
        return serviceStatusMessage;
    }

    @Override
    public boolean isValid() {
        return timestamp >= 0 && messageType != null && !exceptionMessage.isClean() && !serviceStatusMessage.isClean();
    }

    @Override
    public boolean isClean() {
        return timestamp == 0 && messageType == null && exceptionMessage.isClean() && serviceStatusMessage.isClean();
    }


    @Override
    public void clean() {
        timestamp = 0;
        messageType = null;

        if (exceptionMessage != null) {
            exceptionMessage.clean();
        }

        if (serviceStatusMessage != null) {
            serviceStatusMessage.clean();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Message message = (Message) o;
        return timestamp == message.timestamp &&
                messageType == message.messageType &&
                Objects.equals(exceptionMessage, message.exceptionMessage) &&
                Objects.equals(serviceStatusMessage, message.serviceStatusMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                timestamp,
                messageType,
                exceptionMessage,
                serviceStatusMessage);
    }

    @Override
    public String toString() {
        return "Message{" +
                "timestamp=" + timestamp +
                ", messageType=" + messageType +
                ", exceptionMessage=" + exceptionMessage +
                ", serviceStatusMessage=" + serviceStatusMessage +
                '}';
    }

    /**
     * JSR 303 validation constraint annotation for {@link Message}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = MessageValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{com.viveworks.appbase.core.service.mediator.message.Message.message}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
