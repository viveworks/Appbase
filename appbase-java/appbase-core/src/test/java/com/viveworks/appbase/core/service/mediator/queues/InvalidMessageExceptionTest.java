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

import com.viveworks.appbase.core.service.mediator.message.Message;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class InvalidMessageExceptionTest {
    @Test
    void testThrow() {
        final Message message = new Message();
        final Set<ConstraintViolation<Message>> constraintViolations = Set.of();

        Assertions.assertThrows(InvalidMessageException.class, () -> {
            throw new InvalidMessageException(message, constraintViolations);
        });

        Assertions.assertThrows(InvalidMessageException.class, () -> {
            throw new InvalidMessageException("message", message, constraintViolations);
        });

        Assertions.assertThrows(InvalidMessageException.class, () -> {
            throw new InvalidMessageException(
                    "message", new NullPointerException(), message, constraintViolations);
        });

        Assertions.assertThrows(InvalidMessageException.class, () -> {
            throw new InvalidMessageException(new NullPointerException(), message, constraintViolations);
        });

        Assertions.assertThrows(InvalidMessageException.class, () -> {
            throw new InvalidMessageException(
                    "message",
                    new NullPointerException(),
                    false,
                    true,
                    message,
                    constraintViolations);
        });

        try {
            throw new InvalidMessageException(message, constraintViolations);
        } catch (InvalidMessageException ime) {
            Assertions.assertTrue(ime.getTimestamp() > 0);
            Assertions.assertNotNull(ime.getConstraintViolations());
            Assertions.assertNotNull(ime.getMediatorMessage());
        }

        final String string = new InvalidMessageException(message, constraintViolations).toString();
        Assertions.assertTrue(string.contains("mediatorMessage"));
        Assertions.assertTrue(string.contains("constraintViolations"));
    }
}
