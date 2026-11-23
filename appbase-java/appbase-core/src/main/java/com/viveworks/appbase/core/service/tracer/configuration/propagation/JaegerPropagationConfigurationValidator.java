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

package com.viveworks.appbase.core.service.tracer.configuration.propagation;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * JSR 303 validator for {@link JaegerPropagationConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JaegerPropagationConfigurationValidator
        implements ConstraintValidator
        <JaegerPropagationConfiguration.Valid, JaegerPropagationConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(JaegerPropagationConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(JaegerPropagationConfiguration.class);

    private static final String JAEGER_PROPAGATION_TYPES = "jaegerPropagationTypes";
    private static final String JAEGER_PROPAGATION_TYPES_ITEM =
            "jaegerPropagationTypes" + ValidationService.MESSAGE_TEMPLATE_SEPARATOR + ValidationService.ITEM;
    private static final String BAGGAGE_PREFIX = "baggagePrefix";

    private static final String JAEGER_PROPAGATION_TYPES_EMPTY =
            ValidationService.concat(PREFIX, JAEGER_PROPAGATION_TYPES, ValidationService.EMPTY);
    private static final String JAEGER_PROPAGATION_TYPES_ITEM_NULL =
            ValidationService.concat(PREFIX, JAEGER_PROPAGATION_TYPES_ITEM, ValidationService.NULL);
    private static final String BAGGAGE_PREFIX_NULL =
            ValidationService.concat(PREFIX, BAGGAGE_PREFIX, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public JaegerPropagationConfigurationValidator() {
        this(null);
    }

    public JaegerPropagationConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final JaegerPropagationConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(value.getJaegerPropagationTypes(), value.getBaggagePrefix(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link JaegerPropagationConfiguration} properties.
     *
     * @param jaegerPropagationTypes {@link JaegerPropagationConfiguration#getJaegerPropagationTypes()}
     * @param baggagePrefix {@link JaegerPropagationConfiguration#getBaggagePrefix()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final Set<JaegerPropagationType> jaegerPropagationTypes,
                                final String baggagePrefix) throws AppbaseRuntimeException
    {
        validate(jaegerPropagationTypes, baggagePrefix, null, null);
    }

    /**
     * Validate {@link JaegerPropagationConfiguration} properties.
     *
     * @param jaegerPropagationTypes {@link JaegerPropagationConfiguration#getJaegerPropagationTypes()}
     * @param baggagePrefix {@link JaegerPropagationConfiguration#getBaggagePrefix()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final Set<JaegerPropagationType> jaegerPropagationTypes,
                                   final String baggagePrefix,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateCollection(
                jaegerPropagationTypes,
                JAEGER_PROPAGATION_TYPES_EMPTY,
                JAEGER_PROPAGATION_TYPES,
                JAEGER_PROPAGATION_TYPES_ITEM_NULL,
                JAEGER_PROPAGATION_TYPES_ITEM,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        if (jaegerPropagationTypes.contains(JaegerPropagationType.B3)) {
            valid = ValidationService.validateNull(
                    baggagePrefix,
                    BAGGAGE_PREFIX_NULL,
                    BAGGAGE_PREFIX,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
        }

        return valid;
    }
}
