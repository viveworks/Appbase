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

package com.viveworks.appbase.client.core.reactive;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultReactiveOperationConfigurationValidatorTest {

    @Test
    void defaultsAreValid() {
        assertDoesNotThrow(() -> DefaultReactiveOperationConfiguration.builder().build());
    }

    @Test
    void zeroTimeoutRejected() {
        assertThrows(AppbaseRuntimeException.class,
                () -> DefaultReactiveOperationConfiguration.builder().timeout(Duration.ZERO).build());
    }

    @Test
    void negativeRetryAttemptsRejected() {
        assertThrows(AppbaseRuntimeException.class,
                () -> DefaultReactiveOperationConfiguration.builder().retryMaxAttempts(-1).build());
    }

    @Test
    void jitterOutOfRangeRejected() {
        assertThrows(AppbaseRuntimeException.class,
                () -> DefaultReactiveOperationConfiguration.builder().retryJitterFactor(1.5d).build());
    }

    @Test
    void nonPositiveBufferRejected() {
        assertThrows(AppbaseRuntimeException.class,
                () -> DefaultReactiveOperationConfiguration.builder().backpressureBufferSize(0).build());
    }

    private static ConstraintValidatorContext mockContext() {
        final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        final ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
        final NodeBuilderCustomizableContext nodeBuilder = mock(NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        return context;
    }

    @Test
    void isValidWithValidConfigurationReturnsTrueWithoutThrowing() {
        final DefaultReactiveOperationConfigurationValidator validator =
                new DefaultReactiveOperationConfigurationValidator();
        final DefaultReactiveOperationConfiguration valid =
                DefaultReactiveOperationConfiguration.builder().build();

        assertTrue(validator.isValid(valid, mockContext()));
    }

    @Test
    void isValidWithNullReturnsFalse() {
        final DefaultReactiveOperationConfigurationValidator validator =
                new DefaultReactiveOperationConfigurationValidator();

        assertFalse(validator.isValid(null, mockContext()));
    }

    @Test
    void declarativeInvalidTimeoutReturnsFalseWithoutThrowing() {
        final ConstraintValidatorContext context = mockContext();

        assertFalse(assertDoesNotThrow(() -> DefaultReactiveOperationConfigurationValidator.validate(
                true, null, Duration.ZERO, true, 3,
                Duration.ofMillis(100), Duration.ofSeconds(5), 0.5d, 256, context)));
    }

    @Test
    void declarativeInvalidRetryMinBackoffReturnsFalseWithoutThrowing() {
        final ConstraintValidatorContext context = mockContext();

        assertFalse(assertDoesNotThrow(() -> DefaultReactiveOperationConfigurationValidator.validate(
                true, null, Duration.ofSeconds(30), true, 3,
                Duration.ZERO, Duration.ofSeconds(5), 0.5d, 256, context)));
    }

    @Test
    void declarativeInvalidRetryMaxBackoffReturnsFalseWithoutThrowing() {
        final ConstraintValidatorContext context = mockContext();

        assertFalse(assertDoesNotThrow(() -> DefaultReactiveOperationConfigurationValidator.validate(
                true, null, Duration.ofSeconds(30), true, 3,
                Duration.ofMillis(100), Duration.ZERO, 0.5d, 256, context)));
    }

    @Test
    void declarativeInvalidRetryMaxAttemptsReturnsFalseWithoutThrowing() {
        final ConstraintValidatorContext context = mockContext();

        assertFalse(assertDoesNotThrow(() -> DefaultReactiveOperationConfigurationValidator.validate(
                true, null, Duration.ofSeconds(30), true, -1,
                Duration.ofMillis(100), Duration.ofSeconds(5), 0.5d, 256, context)));
    }

    @Test
    void declarativeInvalidRetryJitterFactorReturnsFalseWithoutThrowing() {
        final ConstraintValidatorContext context = mockContext();

        assertFalse(assertDoesNotThrow(() -> DefaultReactiveOperationConfigurationValidator.validate(
                true, null, Duration.ofSeconds(30), true, 3,
                Duration.ofMillis(100), Duration.ofSeconds(5), 1.5d, 256, context)));
    }

    @Test
    void declarativeInvalidBackpressureBufferSizeReturnsFalseWithoutThrowing() {
        final ConstraintValidatorContext context = mockContext();

        assertFalse(assertDoesNotThrow(() -> DefaultReactiveOperationConfigurationValidator.validate(
                true, null, Duration.ofSeconds(30), true, 3,
                Duration.ofMillis(100), Duration.ofSeconds(5), 0.5d, 0, context)));
    }
}
