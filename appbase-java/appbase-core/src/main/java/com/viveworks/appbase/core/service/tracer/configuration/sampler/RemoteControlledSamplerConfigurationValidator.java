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

package com.viveworks.appbase.core.service.tracer.configuration.sampler;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link RemoteControlledSamplerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class RemoteControlledSamplerConfigurationValidator
        implements ConstraintValidator
        <RemoteControlledSamplerConfiguration.Valid, RemoteControlledSamplerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(RemoteControlledSamplerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(RemoteControlledSamplerConfiguration.class);

    private static final String HOST_PORT = "hostPort";
    private static final String SERVICE_NAME = "serviceName";
    private static final String POLLING_INTERVAL_MILLIS = "pollingIntervalMillis";
    private static final String METRICS_PREFIX = "metricsPrefix";

    private static final String HOST_PORT_BLANK =
            ValidationService.concat(PREFIX, HOST_PORT, ValidationService.BLANK);
    private static final String SERVICE_NAME_NULL =
            ValidationService.concat(PREFIX, SERVICE_NAME, ValidationService.NULL);
    private static final String POLLING_INTERVAL_MILLIS_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, POLLING_INTERVAL_MILLIS, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String METRICS_PREFIX_BLANK =
            ValidationService.concat(PREFIX, METRICS_PREFIX, ValidationService.BLANK);

    @Nullable
    private final ValidationService validationService;

    public RemoteControlledSamplerConfigurationValidator() {
        this(null);
    }

    public RemoteControlledSamplerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final RemoteControlledSamplerConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.getHostPort(),
                    value.getServiceName(),
                    value.getPollingIntervalMillis(),
                    value.getMetricsPrefix(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link RemoteControlledSamplerConfiguration} properties.
     *
     * @param hostPort
     *      {@link RemoteControlledSamplerConfiguration#getHostPort()}
     * @param serviceName
     *      {@link RemoteControlledSamplerConfiguration#getServiceName()}
     * @param pollingIntervalMillis
     *      {@link RemoteControlledSamplerConfiguration#getPollingIntervalMillis()}
     * @param metricsPrefix
     *      {@link RemoteControlledSamplerConfiguration#getMetricsPrefix()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final String hostPort,
                                final String serviceName,
                                final int pollingIntervalMillis,
                                final String metricsPrefix) throws AppbaseRuntimeException
    {
        validate(hostPort, serviceName, pollingIntervalMillis, metricsPrefix, null, null);
    }

    /**
     * Validate {@link RemoteControlledSamplerConfiguration} properties.
     *
     * @param hostPort
     *      {@link RemoteControlledSamplerConfiguration#getHostPort()}
     * @param serviceName
     *      {@link RemoteControlledSamplerConfiguration#getServiceName()}
     * @param pollingIntervalMillis
     *      {@link RemoteControlledSamplerConfiguration#getPollingIntervalMillis()}
     * @param metricsPrefix
     *      {@link RemoteControlledSamplerConfiguration#getMetricsPrefix()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final String hostPort,
                                   final String serviceName,
                                   final int pollingIntervalMillis,
                                   final String metricsPrefix,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateBlank(
                hostPort,
                HOST_PORT_BLANK,
                HOST_PORT,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateBlank(
                serviceName,
                SERVICE_NAME_NULL,
                SERVICE_NAME,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositiveOrZero(
                pollingIntervalMillis,
                POLLING_INTERVAL_MILLIS_NOT_POSITIVE_OR_ZERO,
                POLLING_INTERVAL_MILLIS,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateBlank(
                metricsPrefix,
                METRICS_PREFIX_BLANK,
                METRICS_PREFIX,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
