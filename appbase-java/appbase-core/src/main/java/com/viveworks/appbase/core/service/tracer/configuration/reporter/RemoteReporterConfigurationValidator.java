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

package com.viveworks.appbase.core.service.tracer.configuration.reporter;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link RemoteReporterConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class RemoteReporterConfigurationValidator
        implements ConstraintValidator
        <RemoteReporterConfiguration.Valid, RemoteReporterConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(RemoteReporterConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(RemoteReporterConfiguration.class);

    private static final String AGENT_HOST = "agentHost";
    private static final String AGENT_PORT = "agentPort";
    private static final String COLLECTOR_ENDPOINT = "collectorEndpoint";
    private static final String MAX_PACKET_SIZE_BYTES = "maxPacketSizeBytes";
    private static final String FLUSH_INTERVAL_MILLIS = "flushIntervalMillis";
    private static final String CLOSE_ENQUEUE_TIMEOUT_MILLIS = "closeEnqueueTimeoutMillis";
    private static final String MAX_QUEUE_SIZE = "maxQueueSize";
    private static final String METRICS_PREFIX = "metricsPrefix";

    private static final String AGENT_HOST_BLANK =
            ValidationService.concat(PREFIX, AGENT_HOST, ValidationService.BLANK);
    private static final String AGENT_PORT_INVALID =
            ValidationService.concat(PREFIX, AGENT_PORT, ValidationService.INVALID);
    private static final String COLLECTOR_ENDPOINT_BLANK =
            ValidationService.concat(PREFIX, COLLECTOR_ENDPOINT, ValidationService.BLANK);
    private static final String MAX_PACKET_SIZE_BYTES_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, MAX_PACKET_SIZE_BYTES, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String FLUSH_INTERVAL_MILLIS_NOT_POSITIVE =
            ValidationService.concat(PREFIX, FLUSH_INTERVAL_MILLIS, ValidationService.NOT_POSITIVE);
    private static final String CLOSE_ENQUEUE_TIMEOUT_MILLIS_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, CLOSE_ENQUEUE_TIMEOUT_MILLIS, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String MAX_QUEUE_SIZE_NOT_POSITIVE =
            ValidationService.concat(PREFIX, MAX_QUEUE_SIZE, ValidationService.NOT_POSITIVE);
    private static final String METRICS_PREFIX_BLANK =
            ValidationService.concat(PREFIX, METRICS_PREFIX, ValidationService.BLANK);

    @Nullable
    private final ValidationService validationService;

    public RemoteReporterConfigurationValidator() {
        this(null);
    }

    public RemoteReporterConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final RemoteReporterConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(value.isEnabled(),
                    value.getAgentHost(),
                    value.getAgentPort(),
                    value.getCollectorEndpoint(),
                    value.getMaxPacketSizeBytes(),
                    value.getFlushIntervalMillis(),
                    value.getCloseEnqueueTimeoutMillis(),
                    value.getMaxQueueSize(),
                    value.getMetricsPrefix(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link RemoteReporterConfiguration} properties.
     *
     * @param enabled
     *      {@link RemoteReporterConfiguration#isEnabled()}
     * @param agentHost
     *      {@link RemoteReporterConfiguration#getAgentHost()}
     * @param agentPort
     *      {@link RemoteReporterConfiguration#getAgentPort()}
     * @param collectorEndpoint
     *      {@link RemoteReporterConfiguration#getCollectorEndpoint()}
     * @param maxPacketSizeBytes
     *      {@link RemoteReporterConfiguration#getMaxPacketSizeBytes()}
     * @param flushIntervalMillis
     *      {@link RemoteReporterConfiguration#getFlushIntervalMillis()}
     * @param closeEnqueueTimeoutMillis
     *      {@link RemoteReporterConfiguration#getCloseEnqueueTimeoutMillis()}
     * @param maxQueueSize
     *      {@link RemoteReporterConfiguration#getMaxQueueSize()}
     * @param metricsPrefix
     *      {@link RemoteReporterConfiguration#getMetricsPrefix()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final String agentHost,
                                final int agentPort,
                                final String collectorEndpoint,
                                final int maxPacketSizeBytes,
                                final int flushIntervalMillis,
                                final int closeEnqueueTimeoutMillis,
                                final int maxQueueSize,
                                final String metricsPrefix) throws AppbaseRuntimeException
    {
        validate(enabled,
                agentHost,
                agentPort,
                collectorEndpoint,
                maxPacketSizeBytes,
                flushIntervalMillis,
                closeEnqueueTimeoutMillis,
                maxQueueSize,
                metricsPrefix,
                null,
                null);
    }

    /**
     * Validate {@link RemoteReporterConfiguration} properties.
     *
     * @param enabled
     *      {@link RemoteReporterConfiguration#isEnabled()}
     * @param agentHost
     *      {@link RemoteReporterConfiguration#getAgentHost()}
     * @param agentPort
     *      {@link RemoteReporterConfiguration#getAgentPort()}
     * @param collectorEndpoint
     *      {@link RemoteReporterConfiguration#getCollectorEndpoint()}
     * @param maxPacketSizeBytes
     *      {@link RemoteReporterConfiguration#getMaxPacketSizeBytes()}
     * @param flushIntervalMillis
     *      {@link RemoteReporterConfiguration#getFlushIntervalMillis()}
     * @param closeEnqueueTimeoutMillis
     *      {@link RemoteReporterConfiguration#getCloseEnqueueTimeoutMillis()}
     * @param maxQueueSize
     *      {@link RemoteReporterConfiguration#getMaxQueueSize()}
     * @param metricsPrefix
     *      {@link RemoteReporterConfiguration#getMetricsPrefix()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final String agentHost,
                                   final int agentPort,
                                   final String collectorEndpoint,
                                   final int maxPacketSizeBytes,
                                   final int flushIntervalMillis,
                                   final int closeEnqueueTimeoutMillis,
                                   final int maxQueueSize,
                                   final String metricsPrefix,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validateBlank(
                    agentHost,
                    AGENT_HOST_BLANK,
                    AGENT_HOST,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositiveOrZero(
                    agentPort,
                    AGENT_PORT_INVALID,
                    AGENT_PORT,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    collectorEndpoint,
                    COLLECTOR_ENDPOINT_BLANK,
                    COLLECTOR_ENDPOINT,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositiveOrZero(
                    maxPacketSizeBytes,
                    MAX_PACKET_SIZE_BYTES_NOT_POSITIVE_OR_ZERO,
                    MAX_PACKET_SIZE_BYTES,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositive(
                    flushIntervalMillis,
                    FLUSH_INTERVAL_MILLIS_NOT_POSITIVE,
                    FLUSH_INTERVAL_MILLIS,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositiveOrZero(
                    closeEnqueueTimeoutMillis,
                    CLOSE_ENQUEUE_TIMEOUT_MILLIS_NOT_POSITIVE_OR_ZERO,
                    CLOSE_ENQUEUE_TIMEOUT_MILLIS,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositive(
                    maxQueueSize,
                    MAX_QUEUE_SIZE_NOT_POSITIVE,
                    MAX_QUEUE_SIZE,
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
        }

        return true;
    }
}
