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

package com.viveworks.appbase.core.service.configuration.accessor.store.jpa;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * Validator for {@link JpaConfigurationStoreConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JpaConfigurationStoreConfigurationValidator
        implements ConstraintValidator<JpaConfigurationStoreConfiguration.Valid, JpaConfigurationStoreConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(JpaConfigurationStoreConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(JpaConfigurationStoreConfiguration.class);

    private static final String DISCRIMINATOR = "discriminator";
    private static final String ID_COLUMN_NAME = "idColumnName";
    private static final String DISCRIMINATOR_COLUMN_NAME = "discriminatorColumnName";
    private static final String KEY_COLUMN_NAME = "keyColumnName";
    private static final String KEY_JAVA_TYPE_NAME_COLUMN_NAME = "keyJavaTypeNameColumnName";
    private static final String VALUE_COLUMN_NAME = "valueColumnName";
    private static final String VALUE_MIME_TYPE_COLUMN_NAME = "valueMimeTypeColumnName";
    private static final String ENTITY_MANAGER_CONFIGURATION = "entityManagerConfiguration";

    private static final String DISCRIMINATOR_BLANK =
            ValidationService.concat(PREFIX, DISCRIMINATOR, ValidationService.BLANK);
    private static final String ID_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, ID_COLUMN_NAME, ValidationService.BLANK);
    private static final String DISCRIMINATOR_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, DISCRIMINATOR_COLUMN_NAME, ValidationService.BLANK);
    private static final String KEY_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, KEY_COLUMN_NAME, ValidationService.BLANK);
    private static final String KEY_JAVA_TYPE_NAME_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, KEY_JAVA_TYPE_NAME_COLUMN_NAME, ValidationService.BLANK);
    private static final String VALUE_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, VALUE_COLUMN_NAME, ValidationService.BLANK);
    private static final String VALUE_MIME_TYPE_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, VALUE_MIME_TYPE_COLUMN_NAME, ValidationService.BLANK);
    private static final String ENTITY_MANAGER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, ENTITY_MANAGER_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public JpaConfigurationStoreConfigurationValidator() {
        this(null);
    }

    public JpaConfigurationStoreConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final JpaConfigurationStoreConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getDiscriminator(),
                    value.getIdColumnName(),
                    value.getDiscriminatorColumnName(),
                    value.getKeyColumnName(),
                    value.getKeyJavaTypeNameColumnName(),
                    value.getValueColumnName(),
                    value.getValueMimeTypeColumnName(),
                    value.getEntityManagerConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link JpaConfigurationStoreConfiguration}.
     *
     * @param enabled {@link JpaConfigurationStoreConfiguration#isEnabled()}
     * @param discriminator {@link JpaConfigurationStoreConfiguration#getDiscriminator()}
     * @param idColumnName {@link JpaConfigurationStoreConfiguration#getIdColumnName()}
     * @param discriminatorColumnName {@link JpaConfigurationStoreConfiguration#getDiscriminatorColumnName()}
     * @param keyColumnName {@link JpaConfigurationStoreConfiguration#getKeyColumnName()}
     * @param keyJavaTypeNameColumnName {@link JpaConfigurationStoreConfiguration#getKeyJavaTypeNameColumnName()}
     * @param valueColumnName {@link JpaConfigurationStoreConfiguration#getValueColumnName()}
     * @param valueMimeTypeColumnName {@link JpaConfigurationStoreConfiguration#getValueMimeTypeColumnName()}
     * @param entityManagerConfiguration {@link JpaConfigurationStoreConfiguration#getEntityManagerConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final String discriminator,
                                final String idColumnName,
                                final String discriminatorColumnName,
                                final String keyColumnName,
                                final String keyJavaTypeNameColumnName,
                                final String valueColumnName,
                                final String valueMimeTypeColumnName,
                                final EntityManagerConfiguration entityManagerConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(enabled,
                discriminator,
                idColumnName,
                discriminatorColumnName,
                keyColumnName,
                keyJavaTypeNameColumnName,
                valueColumnName,
                valueMimeTypeColumnName,
                entityManagerConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link JpaConfigurationStoreConfiguration}.
     *
     * @param enabled {@link JpaConfigurationStoreConfiguration#isEnabled()}
     * @param discriminator {@link JpaConfigurationStoreConfiguration#getDiscriminator()}
     * @param idColumnName {@link JpaConfigurationStoreConfiguration#getIdColumnName()}
     * @param discriminatorColumnName {@link JpaConfigurationStoreConfiguration#getDiscriminatorColumnName()}
     * @param keyColumnName {@link JpaConfigurationStoreConfiguration#getKeyColumnName()}
     * @param keyJavaTypeNameColumnName {@link JpaConfigurationStoreConfiguration#getKeyJavaTypeNameColumnName()}
     * @param valueColumnName {@link JpaConfigurationStoreConfiguration#getValueColumnName()}
     * @param valueMimeTypeColumnName {@link JpaConfigurationStoreConfiguration#getValueMimeTypeColumnName()}
     * @param entityManagerConfiguration {@link JpaConfigurationStoreConfiguration#getEntityManagerConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final String discriminator,
                                   final String idColumnName,
                                   final String discriminatorColumnName,
                                   final String keyColumnName,
                                   final String keyJavaTypeNameColumnName,
                                   final String valueColumnName,
                                   final String valueMimeTypeColumnName,
                                   final EntityManagerConfiguration entityManagerConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context)
                                   throws AppbaseRuntimeException
    {
        boolean valid = true;

        if (enabled) {
            valid = ValidationService.validateBlank(
                    discriminator,
                    DISCRIMINATOR_BLANK,
                    DISCRIMINATOR,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    idColumnName,
                    ID_COLUMN_NAME_BLANK,
                    ID_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    discriminatorColumnName,
                    DISCRIMINATOR_COLUMN_NAME_BLANK,
                    DISCRIMINATOR_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    keyColumnName,
                    KEY_COLUMN_NAME_BLANK,
                    KEY_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    keyJavaTypeNameColumnName,
                    KEY_JAVA_TYPE_NAME_COLUMN_NAME_BLANK,
                    KEY_JAVA_TYPE_NAME_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    valueColumnName,
                    VALUE_COLUMN_NAME_BLANK,
                    VALUE_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    valueMimeTypeColumnName,
                    VALUE_MIME_TYPE_COLUMN_NAME_BLANK,
                    VALUE_MIME_TYPE_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateNull(
                    entityManagerConfiguration,
                    ENTITY_MANAGER_CONFIGURATION_NULL,
                    ENTITY_MANAGER_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }
        }

        return valid;
    }
}
