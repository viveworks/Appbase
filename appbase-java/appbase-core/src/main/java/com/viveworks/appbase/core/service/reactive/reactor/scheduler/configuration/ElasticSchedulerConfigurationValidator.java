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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfigurationValidator;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ElasticSchedulerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ElasticSchedulerConfigurationValidator
        implements ConstraintValidator<ElasticSchedulerConfiguration.Valid, ElasticSchedulerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ElasticSchedulerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ElasticSchedulerConfiguration.class);

    private static final String ID = "id";
    private static final String THREAD_CAP = "threadCap";
    private static final String QUEUED_TASK_CAP = "queuedTaskCap";
    private static final String TTL_SECONDS = "ttlSeconds";
    private static final String THREAD_FACTORY_CONFIGURATION = "threadNameFormat";

    private static final String ID_BLANK =
            ValidationService.concat(PREFIX, ID, ValidationService.BLANK);
    private static final String THREAD_CAP_NOT_POSITIVE =
            ValidationService.concat(PREFIX, THREAD_CAP, ValidationService.NOT_POSITIVE);
    private static final String QUEUED_TASK_CAP_NOT_POSITIVE =
            ValidationService.concat(PREFIX, QUEUED_TASK_CAP, ValidationService.NOT_POSITIVE);
    private static final String TTL_SECONDS_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, TTL_SECONDS, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String THREAD_FACTORY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, THREAD_FACTORY_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public ElasticSchedulerConfigurationValidator() {
        this(null);
    }

    public ElasticSchedulerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ElasticSchedulerConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getId(),
                    value.getThreadCap(),
                    value.getQueuedTaskCap(),
                    value.getTtlSeconds(),
                    value.getThreadFactoryConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link ElasticSchedulerConfiguration} properties.
     *
     * @param id
     *      {@link ElasticSchedulerConfiguration#getId()}
     * @param ttlSeconds
     *      {@link ElasticSchedulerConfiguration#getTtlSeconds()}
     * @param threadFactoryConfiguration
     *      {@link ElasticSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     * @deprecated Use {@link #validate(String, int, int, int, ThreadFactoryConfiguration)} instead
     */
    @Deprecated
    public static void validate(
            final String id,
            final int ttlSeconds,
            final ThreadFactoryConfiguration threadFactoryConfiguration) throws AppbaseRuntimeException
    {
        validate(id, reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
                reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                ttlSeconds, threadFactoryConfiguration, null, null);
    }

    /**
     * Validate {@link ElasticSchedulerConfiguration} properties.
     *
     * @param id
     *      {@link ElasticSchedulerConfiguration#getId()}
     * @param ttlSeconds
     *      {@link ElasticSchedulerConfiguration#getTtlSeconds()}
     * @param threadFactoryConfiguration
     *      {@link ElasticSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     * @deprecated Use {@link #validate(String, int, int, int, ThreadFactoryConfiguration, ValidationService, ConstraintValidatorContext)} instead
     */
    @Deprecated
    public static boolean validate(
            final String id,
            final int ttlSeconds,
            final ThreadFactoryConfiguration threadFactoryConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        return validate(id, reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
                reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                ttlSeconds, threadFactoryConfiguration, validationService, context);
    }

    /**
     * Validate {@link ElasticSchedulerConfiguration} properties.
     *
     * @param id
     *      {@link ElasticSchedulerConfiguration#getId()}
     * @param threadCap
     *      {@link ElasticSchedulerConfiguration#getThreadCap()}
     * @param queuedTaskCap
     *      {@link ElasticSchedulerConfiguration#getQueuedTaskCap()}
     * @param ttlSeconds
     *      {@link ElasticSchedulerConfiguration#getTtlSeconds()}
     * @param threadFactoryConfiguration
     *      {@link ElasticSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final String id,
            final int threadCap,
            final int queuedTaskCap,
            final int ttlSeconds,
            final ThreadFactoryConfiguration threadFactoryConfiguration) throws AppbaseRuntimeException
    {
        validate(id, threadCap, queuedTaskCap, ttlSeconds, threadFactoryConfiguration, null, null);
    }

    /**
     * Validate {@link ElasticSchedulerConfiguration} properties.
     *
     * @param id
     *      {@link ElasticSchedulerConfiguration#getId()}
     * @param threadCap
     *      {@link ElasticSchedulerConfiguration#getThreadCap()}
     * @param queuedTaskCap
     *      {@link ElasticSchedulerConfiguration#getQueuedTaskCap()}
     * @param ttlSeconds
     *      {@link ElasticSchedulerConfiguration#getTtlSeconds()}
     * @param threadFactoryConfiguration
     *      {@link ElasticSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final String id,
            final int threadCap,
            final int queuedTaskCap,
            final int ttlSeconds,
            final ThreadFactoryConfiguration threadFactoryConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid = false;

        valid = ValidationService.validateBlank(
                id,
                ID_BLANK,
                ID,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                threadCap,
                THREAD_CAP_NOT_POSITIVE,
                THREAD_CAP,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                queuedTaskCap,
                QUEUED_TASK_CAP_NOT_POSITIVE,
                QUEUED_TASK_CAP,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositiveOrZero(
                ttlSeconds,
                TTL_SECONDS_NOT_POSITIVE_OR_ZERO,
                TTL_SECONDS,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                threadFactoryConfiguration,
                THREAD_FACTORY_CONFIGURATION_NULL,
                THREAD_FACTORY_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ThreadFactoryConfigurationValidator.validate(
                threadFactoryConfiguration.getNameFormat(),
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
