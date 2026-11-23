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

import com.viveworks.appbase.core.service.mediator.DefaultMediator;
import com.viveworks.appbase.core.service.mediator.message.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintViolation;

import java.util.Set;

/**
 * Exception thrown if a {@link Message} is invalid using the JSR 303 validation by
 * {@link DefaultMediator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_EQUALS")
public class InvalidMessageException extends MediatorException {
    private static final long serialVersionUID = -5464060216012538107L;
    private final Message mediatorMessage;
    private final Set<ConstraintViolation<Message>> constraintViolations;

    public InvalidMessageException(final Message mediatorMessage,
                                   final Set<ConstraintViolation<Message>> constraintViolations)
    {
        this.mediatorMessage = mediatorMessage;
        this.constraintViolations = constraintViolations;
    }

    public InvalidMessageException(final String exceptionMessage,
                                   final Message mediatorMessage,
                                   final Set<ConstraintViolation<Message>> constraintViolations)
    {
        super(exceptionMessage);
        this.mediatorMessage = mediatorMessage;
        this.constraintViolations = constraintViolations;
    }

    public InvalidMessageException(final String exceptionMessage,
                                   final Throwable cause,
                                   final Message mediatorMessage,
                                   final Set<ConstraintViolation<Message>> constraintViolations)
    {
        super(exceptionMessage, cause);
        this.mediatorMessage = mediatorMessage;
        this.constraintViolations = constraintViolations;
    }

    public InvalidMessageException(final Throwable cause,
                                   final Message mediatorMessage,
                                   final Set<ConstraintViolation<Message>> constraintViolations)
    {
        super(cause);
        this.mediatorMessage = mediatorMessage;
        this.constraintViolations = constraintViolations;
    }

    public InvalidMessageException(final String exceptionMessage,
                                   final Throwable cause,
                                   final boolean enableSuppression,
                                   final boolean writableStackTrace,
                                   final Message mediatorMessage,
                                   final Set<ConstraintViolation<Message>> constraintViolations)
    {
        super(exceptionMessage, cause, enableSuppression, writableStackTrace);
        this.mediatorMessage = mediatorMessage;
        this.constraintViolations = constraintViolations;
    }

    public Message getMediatorMessage() {
        return mediatorMessage;
    }

    public Set<ConstraintViolation<Message>> getConstraintViolations() {
        return constraintViolations;
    }

    @Override
    public String toString() {
        return "InvalidMessageException{" +
                "mediatorMessage=" + mediatorMessage +
                ", constraintViolations=" + constraintViolations +
                '}';
    }
}
