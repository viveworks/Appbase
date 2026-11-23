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

package com.viveworks.appbase.core.service.validation;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * A service for JSR 303 {@link Validator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ValidationService extends AppbaseService {
    String MESSAGE = "message";
    String MESSAGE_TEMPLATE_PREFIX = "{";
    String MESSAGE_TEMPLATE_SUFFIX = "}";
    String MESSAGE_TEMPLATE_SEPARATOR = ".";
    String ITEM = "item";

    String NULL = "null";
    String BLANK = "blank";
    String EMPTY = "empty";
    String NOT_POSITIVE = "notPositive";
    String NOT_POSITIVE_OR_ZERO = "notPositiveOrZero";
    String INVALID = "invalid";

    /**
     * Return the {@link Validator} that this service manages.
     *
     * @return Validator
     */
    Validator getValidator();


    /**
     * Concatenate arguments with {@link #MESSAGE_TEMPLATE_PREFIX}, {@link #MESSAGE_TEMPLATE_SEPARATOR} and
     * {@link #MESSAGE_TEMPLATE_SUFFIX}.
     *
     * @param args Strings to concatenate
     * @return Concatenated strings
     */
    static String concat(final String... args) {
        final StringBuilder result = new StringBuilder(MESSAGE_TEMPLATE_PREFIX);

        if (args != null) {
            int count = 0;
            for (final String arg : args) {
                if (arg != null) {
                    result.append(arg);
                    ++count;
                }
                if (count < args.length) {
                    result.append(MESSAGE_TEMPLATE_SEPARATOR);
                }
            }
        }

        result.append(MESSAGE_TEMPLATE_SEPARATOR)
                .append(MESSAGE)
                .append(MESSAGE_TEMPLATE_SUFFIX);
        return result.toString();
    }

    /**
     * Return the prefix for message templates.
     *
     * @param type Type of the class that is being validated
     * @return Prefix
     */
    static String prefix(final Class<?> type) {
        AppbaseUtil.notNull(type, "type");

        return type.getName();
    }

    /**
     * Return the node name prefix.
     *
     * @param type Type of the class that is being validated
     * @return Node name prefix
     */
    static String nodeNamePrefix(final Class<?> type) {
        AppbaseUtil.notNull(type, "type");

        return prefix(type) + MESSAGE_TEMPLATE_SEPARATOR;
    }

    /**
     * Handle exceptions thrown in validators.
     *
     * @param context Validator context
     * @param cause Exception thrown
     * @param nodeNamePrefix Prefix of the node name
     */
    static void handleValidationException(
            final ConstraintValidatorContext context,
            final Throwable cause,
            final String nodeNamePrefix)
    {
        if (cause instanceof ValidationServiceException) {
            if (context != null) {
                AppbaseUtil.notNull(cause, "cause");
                AppbaseUtil.notNull(nodeNamePrefix, "nodeNamePrefix");

                final ValidationServiceException vse = (ValidationServiceException) cause;
                final Set<ConstraintViolation<Object>> constraintViolations = vse.getConstraintViolations();
                if (constraintViolations != null) {
                    constraintViolations.forEach(constraintViolation -> {
                        final String messageTemplate = constraintViolation.getMessageTemplate();
                        final Path propertyPath = constraintViolation.getPropertyPath();
                        if (!AppbaseUtil.isBlank(messageTemplate) && propertyPath != null) {
                            propertyPath.forEach(node -> {
                                if (node != null) {
                                    final String nodeName = node.getName();
                                    if (!AppbaseUtil.isBlank(nodeName)) {
                                        context.buildConstraintViolationWithTemplate(messageTemplate)
                                                .addPropertyNode(nodeNamePrefix + nodeName);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        }
    }

    /**
     * Handle constraint violation.
     *
     * @param template Message template
     * @param propertyNode Name of the property node
     * @param context Validator context
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    static void addViolationOrThrow(
            final String template,
            final String propertyNode,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        AppbaseUtil.notNull(template, "template");
        AppbaseUtil.notNull(propertyNode, "propertyNode");

        if (context != null) {
            context.buildConstraintViolationWithTemplate(template)
                    .addPropertyNode(propertyNode)
                    .addConstraintViolation();
        } else {
            throw new AppbaseRuntimeException(template);
        }
    }

    /**
     * Validate and handle validation exception.
     *
     * @param validationService Validation service
     * @param context Validator context
     * @param object Object to validate
     * @param nodeNamePrefix Node name prefix
     * @return true, if valid else false
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    static boolean validate(final Object object,
                            final String nodeNamePrefix,
                            @Nullable final ValidationService validationService,
                            @Nullable final ConstraintValidatorContext context)
    {
        if (validationService != null) {
            try {
                AppbaseUtil.validate(validationService, object);
            } catch (AppbaseRuntimeException are) {
                if (context != null) {
                    handleValidationException(context, are.getCause(), nodeNamePrefix);
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate an object for null.
     *
     * @param object Object to check
     * @param template Message template
     * @param propertyNode Name of the property
     * @param nodeNamePrefix Node name prefix
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, else false
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    static boolean validateNull(final Object object,
                                final String template,
                                final String propertyNode,
                                final String nodeNamePrefix,
                                @Nullable final ValidationService validationService,
                                @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        AppbaseUtil.notBlank(template, "template");
        AppbaseUtil.notBlank(propertyNode, "propertyNode");
        AppbaseUtil.notBlank(nodeNamePrefix, "nodeNamePrefix");

        if (object == null) {
            addViolationOrThrow(
                    template,
                    propertyNode,
                    context);
        } else {
            return validate(
                    object,
                    nodeNamePrefix,
                    validationService,
                    context);
        }

        return true;
    }

    /**
     * Validate an enum for null.
     *
     * @param enumeration Enum to validate
     * @param template Message template
     * @param propertyNode Name of the property
     * @param context Validator context
     * @return true if valid, else false
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    static boolean validateEnumNull(final Enum<?> enumeration,
                                    final String template,
                                    final String propertyNode,
                                    @Nullable final ConstraintValidatorContext context)
                                    throws AppbaseRuntimeException
    {
        if (enumeration == null) {
            addViolationOrThrow(
                    template,
                    propertyNode,
                    context);

            return false;
        }

        return true;
    }

    /**
     * Validate a positive number.
     *
     * @param number Number to check
     * @param template Message template
     * @param propertyNode Name of the property
     * @param nodeNamePrefix Node name prefix
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, else false
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    static boolean validatePositive(final double number,
                                    final String template,
                                    final String propertyNode,
                                    final String nodeNamePrefix,
                                    @Nullable final ValidationService validationService,
                                    @Nullable final ConstraintValidatorContext context)
                                    throws AppbaseRuntimeException
    {
        if (number < 1) {
            addViolationOrThrow(
                    template,
                    propertyNode,
                    context);

            return false;
        }

        return true;
    }

    /**
     * Validate a positive number or zero.
     *
     * @param number Number to check
     * @param template Message template
     * @param propertyNode Name of the property
     * @param nodeNamePrefix Node name prefix
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, else false
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    static boolean validatePositiveOrZero(final double number,
                                          final String template,
                                          final String propertyNode,
                                          final String nodeNamePrefix,
                                          @Nullable final ValidationService validationService,
                                          @Nullable final ConstraintValidatorContext context)
                                          throws AppbaseRuntimeException
    {
        if (number < 0) {
            addViolationOrThrow(
                    template,
                    propertyNode,
                    context);

            return false;
        }

        return true;
    }


    /**
     * Validate a string for blanks.
     *
     * @param text String to check
     * @param template Message template
     * @param propertyNode Name of the property
     * @param nodeNamePrefix Node name prefix
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, else false
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    static boolean validateBlank(final String text,
                                 final String template,
                                 final String propertyNode,
                                 final String nodeNamePrefix,
                                 @Nullable final ValidationService validationService,
                                 @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (AppbaseUtil.isBlank(text)) {
            addViolationOrThrow(
                    template,
                    propertyNode,
                    context);

            return false;
        }

        return true;
    }

    /**
     * Validate an object for null.
     *
     * @param collection Collection to check
     * @param collectionTemplate Template message for the whole collection
     * @param collectionPropertyNode Collection property node
     * @param itemTemplate Collection item template message
     * @param itemPropertyNode Collection item property node
     * @param nodeNamePrefix Node name prefix
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, else false
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    static boolean validateCollection(final Collection<?> collection,
                                      final String collectionTemplate,
                                      final String collectionPropertyNode,
                                      final String itemTemplate,
                                      final String itemPropertyNode,
                                      final String nodeNamePrefix,
                                      @Nullable final ValidationService validationService,
                                      @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        AppbaseUtil.notBlank(collectionTemplate, "collectionTemplate");
        AppbaseUtil.notBlank(collectionPropertyNode, "collectionPropertyNode");
        AppbaseUtil.notBlank(nodeNamePrefix, "nodeNamePrefix");

        if (collection == null || collection.isEmpty()) {
            addViolationOrThrow(
                    collectionTemplate,
                    collectionPropertyNode,
                    context);
        } else {
            for (final Object item : collection) {
                if (item == null) {
                    addViolationOrThrow(
                            itemTemplate,
                            itemPropertyNode,
                            context);
                }

                final boolean valid;
                if (item instanceof Enum<?>) {
                    valid = validateEnumNull(
                            (Enum<?>) item,
                            itemTemplate,
                            itemPropertyNode,
                            context);
                } else {
                    valid = validate(
                            item,
                            nodeNamePrefix,
                            validationService,
                            context);
                }

                if (!valid) {
                    return valid;
                }
            }
        }

        return true;
    }
}
