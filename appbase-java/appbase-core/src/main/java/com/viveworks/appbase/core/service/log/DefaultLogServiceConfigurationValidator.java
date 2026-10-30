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

package com.viveworks.appbase.core.service.log;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration;
import com.viveworks.appbase.core.service.log.appender.disruptor.DisruptorMediatorEventDelegatingAppenderConfiguration;
import com.viveworks.appbase.core.service.log.appender.lucene.LuceneAppenderConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.DisruptorLogAndTurboLogEventFilterDelegatorConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.listener.lucene.LuceneLogAndTurboLogEventFilterListenerConfiguration;
import com.viveworks.appbase.core.service.log.filter.metrics.MetricsTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultLogServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultLogServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultLogServiceConfiguration.Valid, DefaultLogServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultLogServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultLogServiceConfiguration.class);

    private static final String DEFAULT_LOG_ACCESSOR_CONFIGURATION =
            "defaultLogAccessorConfiguration";
    private static final String METRICS_TURBO_FILTER_HANDLER_CONFIGURATION =
            "metricsTurboFilterHandlerConfiguration";
    private static final String BURST_TURBO_LOG_EVENT_FILTER_HANDLER_CONFIGURATION =
            "burstTurboLogEventFilterHandlerConfiguration";
    private static final String DISRUPTOR_MEDIATOR_EVENT_DELEGATING_APPENDER_CONFIGURATION =
            "disruptorMediatorEventDelegatingAppenderConfiguration";
    private static final String LUCENE_LOG_INDEX_CONFIGURATION =
            "luceneLogIndexConfiguration";
    private static final String LUCENE_APPENDER_CONFIGURATION =
            "luceneAppenderConfiguration";
    private static final String DISRUPTOR_LOG_AND_TURBO_LOG_EVENT_FILTER_DELEGATOR_CONFIGURATION =
            "disruptorLogAndTurboLogEventFilterDelegatorConfiguration";
    private static final String DUPLICATE_LOG_TURBO_FILTER_HANDLER_CONFIGURATION =
            "duplicateLogTurboFilterHandlerConfiguration";
    private static final String LUCENE_LOG_AND_TURBO_LOG_EVENT_FILTER_LISTENER_CONFIGURATION =
            "luceneLogAndTurboLogEventFilterListenerConfiguration";

    private static final String DEFAULT_LOG_ACCESSOR_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_LOG_ACCESSOR_CONFIGURATION, ValidationService.NULL);
    private static final String METRICS_TURBO_FILTER_HANDLER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, METRICS_TURBO_FILTER_HANDLER_CONFIGURATION, ValidationService.NULL);
    private static final String BURST_TURBO_LOG_EVENT_FILTER_HANDLER_CONFIGURATION_NULL =
            ValidationService.concat(
                    PREFIX, BURST_TURBO_LOG_EVENT_FILTER_HANDLER_CONFIGURATION, ValidationService.NULL);
    private static final String DISRUPTOR_MEDIATOR_EVENT_DELEGATING_APPENDER_CONFIGURATION_NULL =
            ValidationService.concat(
                    PREFIX, DISRUPTOR_MEDIATOR_EVENT_DELEGATING_APPENDER_CONFIGURATION, ValidationService.NULL);
    private static final String LUCENE_LOG_INDEX_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, LUCENE_LOG_INDEX_CONFIGURATION, ValidationService.NULL);
    private static final String LUCENE_APPENDER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, LUCENE_APPENDER_CONFIGURATION, ValidationService.NULL);
    private static final String DISRUPTOR_LOG_AND_TURBO_LOG_EVENT_FILTER_DELEGATOR_CONFIGURATION_NULL =
            ValidationService.concat(
                    PREFIX, DISRUPTOR_LOG_AND_TURBO_LOG_EVENT_FILTER_DELEGATOR_CONFIGURATION, ValidationService.NULL);
    private static final String DUPLICATE_LOG_TURBO_FILTER_HANDLER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DUPLICATE_LOG_TURBO_FILTER_HANDLER_CONFIGURATION, ValidationService.NULL);
    private static final String LUCENE_LOG_AND_TURBO_LOG_EVENT_FILTER_LISTENER_CONFIGURATION_NULL =
            ValidationService.concat(
                    PREFIX, LUCENE_LOG_AND_TURBO_LOG_EVENT_FILTER_LISTENER_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultLogServiceConfigurationValidator() {
        this(null);
    }

    public DefaultLogServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultLogServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getDefaultLogAccessorConfiguration(),
                    value.getMetricsTurboFilterHandlerConfiguration(),
                    value.getBurstTurboLogEventFilterHandlerConfiguration(),
                    value.getDisruptorMediatorEventDelegatingAppenderConfiguration(),
                    value.getLuceneLogIndexConfiguration(),
                    value.getLuceneAppenderConfiguration(),
                    value.getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration(),
                    value.getDuplicateLogTurboFilterHandlerConfiguration(),
                    value.getLuceneLogAndTurboLogEventFilterListenerConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate configuration properties.
     *
     * @param defaultLogAccessorConfiguration
     *      {@link DefaultLogServiceConfiguration#getDefaultLogAccessorConfiguration()}
     * @param metricsTurboFilterHandlerConfiguration
     *      {@link DefaultLogServiceConfiguration#getMetricsTurboFilterHandlerConfiguration()}
     * @param burstTurboLogEventFilterHandlerConfiguration
     *      {@link DefaultLogServiceConfiguration#getBurstTurboLogEventFilterHandlerConfiguration()}
     * @param disruptorMediatorEventDelegatingAppenderConfiguration
     *      {@link DefaultLogServiceConfiguration#getDisruptorMediatorEventDelegatingAppenderConfiguration()}
     * @param luceneLogIndexConfiguration
     *      {@link DefaultLogServiceConfiguration#getLuceneLogIndexConfiguration()}
     * @param luceneAppenderConfiguration
     *      {@link DefaultLogServiceConfiguration#getLuceneAppenderConfiguration()}
     * @param disruptorLogAndTurboLogEventFilterDelegatorConfiguration
     *      {@link DefaultLogServiceConfiguration#getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration()}
     * @param duplicateLogTurboFilterHandlerConfiguration
     *      {@link DefaultLogServiceConfiguration#getDuplicateLogTurboFilterHandlerConfiguration()}
     * @param luceneLogAndTurboLogEventFilterListenerConfiguration
     *      {@link DefaultLogServiceConfiguration#getLuceneLogAndTurboLogEventFilterListenerConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration,
            final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration,
            final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration,
            final DisruptorMediatorEventDelegatingAppenderConfiguration
                    disruptorMediatorEventDelegatingAppenderConfiguration,
            final LuceneLogIndexConfiguration luceneLogIndexConfiguration,
            final LuceneAppenderConfiguration luceneAppenderConfiguration,
            final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                    disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
            final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration,
            final LuceneLogAndTurboLogEventFilterListenerConfiguration
                    luceneLogAndTurboLogEventFilterListenerConfiguration)
            throws AppbaseRuntimeException
    {
        validate(defaultLogAccessorConfiguration,
                metricsTurboFilterHandlerConfiguration,
                burstTurboLogEventFilterHandlerConfiguration,
                disruptorMediatorEventDelegatingAppenderConfiguration,
                luceneLogIndexConfiguration,
                luceneAppenderConfiguration,
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                duplicateLogTurboFilterHandlerConfiguration,
                luceneLogAndTurboLogEventFilterListenerConfiguration,
                null,
                null);
    }

    /**
     * Validate configuration properties.
     *
     * @param defaultLogAccessorConfiguration
     *      {@link DefaultLogServiceConfiguration#getDefaultLogAccessorConfiguration()}
     * @param metricsTurboFilterHandlerConfiguration
     *      {@link DefaultLogServiceConfiguration#getMetricsTurboFilterHandlerConfiguration()}
     * @param burstTurboLogEventFilterHandlerConfiguration
     *      {@link DefaultLogServiceConfiguration#getBurstTurboLogEventFilterHandlerConfiguration()}
     * @param disruptorMediatorEventDelegatingAppenderConfiguration
     *      {@link DefaultLogServiceConfiguration#getDisruptorMediatorEventDelegatingAppenderConfiguration()}
     * @param luceneLogIndexConfiguration
     *      {@link DefaultLogServiceConfiguration#getLuceneLogIndexConfiguration()}
     * @param luceneAppenderConfiguration
     *      {@link DefaultLogServiceConfiguration#getLuceneAppenderConfiguration()}
     * @param disruptorLogAndTurboLogEventFilterDelegatorConfiguration
     *      {@link DefaultLogServiceConfiguration#getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration()}
     * @param duplicateLogTurboFilterHandlerConfiguration
     *      {@link DefaultLogServiceConfiguration#getDuplicateLogTurboFilterHandlerConfiguration()}
     * @param luceneLogAndTurboLogEventFilterListenerConfiguration
     *      {@link DefaultLogServiceConfiguration#getLuceneLogAndTurboLogEventFilterListenerConfiguration()}
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration,
            final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration,
            final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration,
            final DisruptorMediatorEventDelegatingAppenderConfiguration
                    disruptorMediatorEventDelegatingAppenderConfiguration,
            final LuceneLogIndexConfiguration luceneLogIndexConfiguration,
            final LuceneAppenderConfiguration luceneAppenderConfiguration,
            final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                    disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
            final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration,
            final LuceneLogAndTurboLogEventFilterListenerConfiguration
                    luceneLogAndTurboLogEventFilterListenerConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateNull(
                defaultLogAccessorConfiguration,
                DEFAULT_LOG_ACCESSOR_CONFIGURATION_NULL,
                DEFAULT_LOG_ACCESSOR_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                metricsTurboFilterHandlerConfiguration,
                METRICS_TURBO_FILTER_HANDLER_CONFIGURATION_NULL,
                METRICS_TURBO_FILTER_HANDLER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                burstTurboLogEventFilterHandlerConfiguration,
                BURST_TURBO_LOG_EVENT_FILTER_HANDLER_CONFIGURATION_NULL,
                BURST_TURBO_LOG_EVENT_FILTER_HANDLER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                disruptorMediatorEventDelegatingAppenderConfiguration,
                DISRUPTOR_MEDIATOR_EVENT_DELEGATING_APPENDER_CONFIGURATION_NULL,
                DISRUPTOR_MEDIATOR_EVENT_DELEGATING_APPENDER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                luceneLogIndexConfiguration,
                LUCENE_LOG_INDEX_CONFIGURATION_NULL,
                LUCENE_LOG_INDEX_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                luceneAppenderConfiguration,
                LUCENE_APPENDER_CONFIGURATION_NULL,
                LUCENE_APPENDER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                DISRUPTOR_LOG_AND_TURBO_LOG_EVENT_FILTER_DELEGATOR_CONFIGURATION_NULL,
                DISRUPTOR_LOG_AND_TURBO_LOG_EVENT_FILTER_DELEGATOR_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                duplicateLogTurboFilterHandlerConfiguration,
                DUPLICATE_LOG_TURBO_FILTER_HANDLER_CONFIGURATION_NULL,
                DUPLICATE_LOG_TURBO_FILTER_HANDLER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                luceneLogAndTurboLogEventFilterListenerConfiguration,
                LUCENE_LOG_AND_TURBO_LOG_EVENT_FILTER_LISTENER_CONFIGURATION_NULL,
                LUCENE_LOG_AND_TURBO_LOG_EVENT_FILTER_LISTENER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
