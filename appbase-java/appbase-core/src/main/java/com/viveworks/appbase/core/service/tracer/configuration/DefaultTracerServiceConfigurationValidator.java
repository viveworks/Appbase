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

package com.viveworks.appbase.core.service.tracer.configuration;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.tracer.configuration.propagation.JaegerPropagationConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.InMemoryReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.JaegerReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.RemoteReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.GuaranteedThroughputSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.JaegerSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.PerOperationSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ProbabilisticSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RateLimitingSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RemoteControlledSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.JaegerScopeManagerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.ThreadLocalScopeManagerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * JSR 303 validator for {@link DefaultTracerServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultTracerServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultTracerServiceConfiguration.Valid, DefaultTracerServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultTracerServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultTracerServiceConfiguration.class);

    private static final String SERVICE_NAME = "serviceName";
    private static final String JAEGER_PROPAGATION_CONFIGURATION = "jaegerPropagationConfiguration";
    private static final String JAEGER_SAMPLER_CONFIGURATION = "jaegerSamplerConfiguration";
    private static final String JAEGER_SCOPE_MANAGER_CONFIGURATION = "jaegerScopeManagerConfiguration";
    private static final String JAEGER_REPORTER_CONFIGURATIONS = "jaegerReporterConfigurations";
    private static final String JAEGER_REPORTER_CONFIGURATIONS_ITEM =
            "jaegerReporterConfigurations" + ValidationService.MESSAGE_TEMPLATE_SEPARATOR + ValidationService.ITEM;

    private static final String SERVICE_NAME_BLANK =
            ValidationService.concat(PREFIX, SERVICE_NAME, ValidationService.BLANK);
    private static final String JAEGER_PROPAGATION_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, JAEGER_PROPAGATION_CONFIGURATION, ValidationService.NULL);
    private static final String JAEGER_SAMPLER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, JAEGER_SAMPLER_CONFIGURATION, ValidationService.NULL);
    private static final String JAEGER_SAMPLER_CONFIGURATION_UNSUPPORTED =
            ValidationService.concat(PREFIX, JAEGER_SAMPLER_CONFIGURATION, "unsupported");
    private static final String JAEGER_REPORTER_CONFIGURATIONS_NULL =
            ValidationService.concat(PREFIX, JAEGER_REPORTER_CONFIGURATIONS, ValidationService.NULL);
    private static final String JAEGER_REPORTER_CONFIGURATIONS_ITEM_NULL =
            ValidationService.concat(PREFIX, JAEGER_REPORTER_CONFIGURATIONS_ITEM, ValidationService.NULL);
    private static final String JAEGER_REPORTER_CONFIGURATIONS_ITEM_UNSUPPORTED =
            ValidationService.concat(PREFIX, JAEGER_REPORTER_CONFIGURATIONS_ITEM, "unsupported");
    private static final String JAEGER_SCOPE_MANAGER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, JAEGER_SCOPE_MANAGER_CONFIGURATION, ValidationService.NULL);
    private static final String JAEGER_SCOPE_MANAGER_CONFIGURATION_UNSUPPORTED =
            ValidationService.concat(PREFIX, JAEGER_SCOPE_MANAGER_CONFIGURATION, "unsupported");

    @Nullable
    private final ValidationService validationService;

    public DefaultTracerServiceConfigurationValidator() {
        this(null);
    }

    public DefaultTracerServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultTracerServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getServiceName(),
                    value.getJaegerPropagationConfiguration(),
                    value.getJaegerSamplerConfiguration(),
                    value.getJaegerReporterConfigurations(),
                    value.getJaegerScopeManagerConfiguration(),
                    validationService,
                    context);
        }
        return false;
    }

    /**
     * Validate {@link DefaultTracerServiceConfiguration} properties.
     *
     * @param serviceName
     *      {@link DefaultTracerServiceConfiguration#getServiceName()}
     * @param jaegerPropagationConfiguration
     *      {@link DefaultTracerServiceConfiguration#getJaegerPropagationConfiguration()}
     * @param jaegerSamplerConfiguration
     *      {@link DefaultTracerServiceConfiguration#getJaegerSamplerConfiguration()}
     * @param jaegerScopeManagerConfiguration
     *      {@link DefaultTracerServiceConfiguration#getJaegerScopeManagerConfiguration()}
     * @param jaegerReporterConfigurations
     *      {@link DefaultTracerServiceConfiguration#getJaegerReporterConfigurations()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final String serviceName,
                                final JaegerPropagationConfiguration jaegerPropagationConfiguration,
                                final JaegerSamplerConfiguration jaegerSamplerConfiguration,
                                final JaegerScopeManagerConfiguration jaegerScopeManagerConfiguration,
                                final List<JaegerReporterConfiguration> jaegerReporterConfigurations)
                                throws AppbaseRuntimeException
    {
        validate(serviceName,
                jaegerPropagationConfiguration,
                jaegerSamplerConfiguration,
                jaegerReporterConfigurations,
                jaegerScopeManagerConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link DefaultTracerServiceConfiguration} properties.
     *
     * @param serviceName
     *      {@link DefaultTracerServiceConfiguration#getServiceName()}
     * @param jaegerPropagationConfiguration
     *      {@link DefaultTracerServiceConfiguration#getJaegerPropagationConfiguration()}
     * @param jaegerSamplerConfiguration
     *      {@link DefaultTracerServiceConfiguration#getJaegerSamplerConfiguration()}
     * @param jaegerReporterConfigurations
     *      {@link DefaultTracerServiceConfiguration#getJaegerReporterConfigurations()}
     * @param jaegerScopeManagerConfiguration
     *      {@link DefaultTracerServiceConfiguration#getJaegerScopeManagerConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final String serviceName,
                                   final JaegerPropagationConfiguration jaegerPropagationConfiguration,
                                   final JaegerSamplerConfiguration jaegerSamplerConfiguration,
                                   final List<JaegerReporterConfiguration> jaegerReporterConfigurations,
                                   final JaegerScopeManagerConfiguration jaegerScopeManagerConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateBlank(
                serviceName,
                SERVICE_NAME_BLANK,
                SERVICE_NAME,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                jaegerPropagationConfiguration,
                JAEGER_PROPAGATION_CONFIGURATION_NULL,
                JAEGER_PROPAGATION_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                jaegerSamplerConfiguration,
                JAEGER_SAMPLER_CONFIGURATION_NULL,
                JAEGER_SAMPLER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                jaegerScopeManagerConfiguration,
                JAEGER_SCOPE_MANAGER_CONFIGURATION_NULL,
                JAEGER_SCOPE_MANAGER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        boolean validJaegerSamplerConfigurationType = false;
        final Class<? extends JaegerSamplerConfiguration> jaegerSamplerConfigurationType =
                jaegerSamplerConfiguration.getClass();
        if (jaegerSamplerConfigurationType.isAssignableFrom(ConstSamplerConfiguration.class) ||
                jaegerSamplerConfigurationType.isAssignableFrom(GuaranteedThroughputSamplerConfiguration.class) ||
                jaegerSamplerConfigurationType.isAssignableFrom(PerOperationSamplerConfiguration.class) ||
                jaegerSamplerConfigurationType.isAssignableFrom(ProbabilisticSamplerConfiguration.class) ||
                jaegerSamplerConfigurationType.isAssignableFrom(RateLimitingSamplerConfiguration.class) ||
                jaegerSamplerConfigurationType.isAssignableFrom(RemoteControlledSamplerConfiguration.class))
        {
            validJaegerSamplerConfigurationType = true;
        }
        if (!validJaegerSamplerConfigurationType) {
            ValidationService.addViolationOrThrow(
                    JAEGER_SAMPLER_CONFIGURATION_UNSUPPORTED,
                    JAEGER_SAMPLER_CONFIGURATION,
                    context);
            return false;
        }

        boolean validJaegerScopeManagerConfiguration = false;
        final Class<? extends JaegerScopeManagerConfiguration> jaegerScopeManagerConfigurationType =
                jaegerScopeManagerConfiguration.getClass();
        if (jaegerScopeManagerConfigurationType.isAssignableFrom(MDCScopeManagerConfiguration.class) ||
                jaegerScopeManagerConfigurationType.isAssignableFrom(ThreadLocalScopeManagerConfiguration.class))
        {
            validJaegerScopeManagerConfiguration = true;
        }
        if (!validJaegerScopeManagerConfiguration) {
            ValidationService.addViolationOrThrow(
                    JAEGER_SCOPE_MANAGER_CONFIGURATION_UNSUPPORTED,
                    JAEGER_SCOPE_MANAGER_CONFIGURATION,
                    context);
            return false;
        }

        valid = ValidationService.validateNull(
                jaegerReporterConfigurations,
                JAEGER_REPORTER_CONFIGURATIONS_NULL,
                JAEGER_REPORTER_CONFIGURATIONS,
                NODE_NAME_PREFIX,
                validationService,
                context);
        for (final JaegerReporterConfiguration jaegerReporterConfiguration : jaegerReporterConfigurations) {
            if (jaegerReporterConfiguration == null) {
                return ValidationService.validateNull(
                        jaegerReporterConfiguration,
                        JAEGER_REPORTER_CONFIGURATIONS_ITEM_NULL,
                        JAEGER_REPORTER_CONFIGURATIONS_ITEM,
                        NODE_NAME_PREFIX,
                        validationService,
                        context);
            }

            boolean validJaegerReporterConfiguration = false;
            final Class<? extends JaegerReporterConfiguration> jaegerReporterConfigurationType =
                    jaegerReporterConfiguration.getClass();
            if (jaegerReporterConfigurationType.isAssignableFrom(InMemoryReporterConfiguration.class) ||
                    jaegerReporterConfigurationType.isAssignableFrom(LoggingReporterConfiguration.class) ||
                    jaegerReporterConfigurationType.isAssignableFrom(RemoteReporterConfiguration.class))
            {
                validJaegerReporterConfiguration = true;
            }

            if (!validJaegerReporterConfiguration) {
                ValidationService.addViolationOrThrow(
                        JAEGER_REPORTER_CONFIGURATIONS_ITEM_UNSUPPORTED,
                        JAEGER_REPORTER_CONFIGURATIONS_ITEM,
                        context);
                return false;
            }
        }

        return valid;
    }
}
