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
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.time.Duration;

/**
 * Validates a {@link DefaultReactiveOperationConfiguration}: positive timeout and
 * backoffs, non-negative retry attempts, jitter in [0,1], positive buffer size.
 *
 * <p>Implements {@link ConstraintValidator} (rather than being a pure static-utility
 * class) so it can be referenced from {@link DefaultReactiveOperationConfiguration.Valid}'s
 * {@code validatedBy}. The {@link #isValid} entry point returns {@code true}/{@code false}
 * and records violations on the {@link ConstraintValidatorContext} (JSR-303 contract), while
 * the programmatic {@link #validate(boolean, String, Duration, boolean, int, Duration, Duration,
 * double, int)} entry point used by the Builder still fails fast by throwing.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultReactiveOperationConfigurationValidator
        implements ConstraintValidator<DefaultReactiveOperationConfiguration.Valid,
                DefaultReactiveOperationConfiguration> {

    private static final String PREFIX =
            ValidationService.prefix(DefaultReactiveOperationConfiguration.class);

    private static final String TIMEOUT = "timeout";
    private static final String RETRY_MIN_BACKOFF = "retryMinBackoff";
    private static final String RETRY_MAX_BACKOFF = "retryMaxBackoff";
    private static final String RETRY_MAX_ATTEMPTS = "retryMaxAttempts";
    private static final String RETRY_JITTER_FACTOR = "retryJitterFactor";
    private static final String BACKPRESSURE_BUFFER_SIZE = "backpressureBufferSize";

    private static final String TIMEOUT_NOT_POSITIVE =
            ValidationService.concat(PREFIX, TIMEOUT, ValidationService.NOT_POSITIVE);
    private static final String RETRY_MIN_BACKOFF_NOT_POSITIVE =
            ValidationService.concat(PREFIX, RETRY_MIN_BACKOFF, ValidationService.NOT_POSITIVE);
    private static final String RETRY_MAX_BACKOFF_NOT_POSITIVE =
            ValidationService.concat(PREFIX, RETRY_MAX_BACKOFF, ValidationService.NOT_POSITIVE);
    private static final String RETRY_MAX_ATTEMPTS_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, RETRY_MAX_ATTEMPTS, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String RETRY_JITTER_FACTOR_INVALID =
            ValidationService.concat(PREFIX, RETRY_JITTER_FACTOR, ValidationService.INVALID);
    private static final String BACKPRESSURE_BUFFER_SIZE_NOT_POSITIVE =
            ValidationService.concat(PREFIX, BACKPRESSURE_BUFFER_SIZE, ValidationService.NOT_POSITIVE);

    public DefaultReactiveOperationConfigurationValidator() {
    }

    @Override
    public boolean isValid(@Nullable final DefaultReactiveOperationConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException {
        if (value != null) {
            return validate(value.isEnabled(), value.getSchedulerId(), value.getTimeout(), value.isRetryEnabled(),
                    value.getRetryMaxAttempts(), value.getRetryMinBackoff(), value.getRetryMaxBackoff(),
                    value.getRetryJitterFactor(), value.getBackpressureBufferSize(), context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultReactiveOperationConfiguration} properties.
     *
     * @param enabled {@link DefaultReactiveOperationConfiguration#isEnabled()}
     * @param schedulerId {@link DefaultReactiveOperationConfiguration#getSchedulerId()}
     * @param timeout {@link DefaultReactiveOperationConfiguration#getTimeout()}
     * @param retryEnabled {@link DefaultReactiveOperationConfiguration#isRetryEnabled()}
     * @param retryMaxAttempts {@link DefaultReactiveOperationConfiguration#getRetryMaxAttempts()}
     * @param retryMinBackoff {@link DefaultReactiveOperationConfiguration#getRetryMinBackoff()}
     * @param retryMaxBackoff {@link DefaultReactiveOperationConfiguration#getRetryMaxBackoff()}
     * @param retryJitterFactor {@link DefaultReactiveOperationConfiguration#getRetryJitterFactor()}
     * @param backpressureBufferSize {@link DefaultReactiveOperationConfiguration#getBackpressureBufferSize()}
     * @throws AppbaseRuntimeException if invalid
     */
    public static void validate(final boolean enabled,
                                @Nullable final String schedulerId,
                                final Duration timeout,
                                final boolean retryEnabled,
                                final int retryMaxAttempts,
                                final Duration retryMinBackoff,
                                final Duration retryMaxBackoff,
                                final double retryJitterFactor,
                                final int backpressureBufferSize) throws AppbaseRuntimeException {
        validate(enabled, schedulerId, timeout, retryEnabled, retryMaxAttempts, retryMinBackoff, retryMaxBackoff,
                retryJitterFactor, backpressureBufferSize, null);
    }

    /**
     * Validate {@link DefaultReactiveOperationConfiguration} properties.
     *
     * @param enabled {@link DefaultReactiveOperationConfiguration#isEnabled()}
     * @param schedulerId {@link DefaultReactiveOperationConfiguration#getSchedulerId()}
     * @param timeout {@link DefaultReactiveOperationConfiguration#getTimeout()}
     * @param retryEnabled {@link DefaultReactiveOperationConfiguration#isRetryEnabled()}
     * @param retryMaxAttempts {@link DefaultReactiveOperationConfiguration#getRetryMaxAttempts()}
     * @param retryMinBackoff {@link DefaultReactiveOperationConfiguration#getRetryMinBackoff()}
     * @param retryMaxBackoff {@link DefaultReactiveOperationConfiguration#getRetryMaxBackoff()}
     * @param retryJitterFactor {@link DefaultReactiveOperationConfiguration#getRetryJitterFactor()}
     * @param backpressureBufferSize {@link DefaultReactiveOperationConfiguration#getBackpressureBufferSize()}
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if context is not null
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static boolean validate(final boolean enabled,
                                   @Nullable final String schedulerId,
                                   final Duration timeout,
                                   final boolean retryEnabled,
                                   final int retryMaxAttempts,
                                   final Duration retryMinBackoff,
                                   final Duration retryMaxBackoff,
                                   final double retryJitterFactor,
                                   final int backpressureBufferSize,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException {
        if (isDurationNotPositive(timeout)) {
            ValidationService.addViolationOrThrow(TIMEOUT_NOT_POSITIVE, TIMEOUT, context);
            return false;
        }

        if (isDurationNotPositive(retryMinBackoff)) {
            ValidationService.addViolationOrThrow(RETRY_MIN_BACKOFF_NOT_POSITIVE, RETRY_MIN_BACKOFF, context);
            return false;
        }

        if (isDurationNotPositive(retryMaxBackoff)) {
            ValidationService.addViolationOrThrow(RETRY_MAX_BACKOFF_NOT_POSITIVE, RETRY_MAX_BACKOFF, context);
            return false;
        }

        if (retryMaxAttempts < 0) {
            ValidationService.addViolationOrThrow(RETRY_MAX_ATTEMPTS_NOT_POSITIVE_OR_ZERO, RETRY_MAX_ATTEMPTS, context);
            return false;
        }

        if (retryJitterFactor < 0.0d || retryJitterFactor > 1.0d) {
            ValidationService.addViolationOrThrow(RETRY_JITTER_FACTOR_INVALID, RETRY_JITTER_FACTOR, context);
            return false;
        }

        if (backpressureBufferSize <= 0) {
            ValidationService.addViolationOrThrow(
                    BACKPRESSURE_BUFFER_SIZE_NOT_POSITIVE, BACKPRESSURE_BUFFER_SIZE, context);
            return false;
        }

        return true;
    }

    private static boolean isDurationNotPositive(@Nullable final Duration value) {
        return value == null || value.isZero() || value.isNegative();
    }
}
