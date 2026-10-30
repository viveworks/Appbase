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

package com.viveworks.appbase.core.service.validation;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ValidationServiceExceptionTest {
    @Test
    void testThrow() {
        final Set<ConstraintViolation<Object>> constraintViolations = Set.of();
        final Object value = -1;
        final String message = "Exception message";

        Assertions.assertThrows(ValidationServiceException.class, () -> {
            throw new ValidationServiceException(value, constraintViolations);
        });

        Assertions.assertThrows(ValidationServiceException.class, () -> {
            throw new ValidationServiceException(message, value, constraintViolations);
        });

        Assertions.assertThrows(ValidationServiceException.class, () -> {
            throw new ValidationServiceException(message, new NullPointerException(), value, constraintViolations);
        });

        Assertions.assertThrows(ValidationServiceException.class, () -> {
            throw new ValidationServiceException(new NullPointerException(), value, constraintViolations);
        });

        Assertions.assertThrows(ValidationServiceException.class, () -> {
            throw new ValidationServiceException(
                    message,
                    new NullPointerException(),
                    true,
                    true,
                    value,
                    constraintViolations);
        });

        try {
            throw new ValidationServiceException(value, constraintViolations);
        } catch (ValidationServiceException vse) {
            Assertions.assertTrue(vse.getTimestamp() > 0);
            Assertions.assertNotNull(vse.getConstraintViolations());
            Assertions.assertNotNull(vse.getValue());
        }
    }
}
