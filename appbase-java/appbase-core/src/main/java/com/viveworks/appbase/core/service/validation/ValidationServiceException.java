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

package com.viveworks.appbase.core.service.validation;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintViolation;

import java.util.Set;

/**
 * Exception thrown when validation using {@link ValidationService#getValidator()} fails.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_EQUALS")
public class ValidationServiceException extends AppbaseException {
    private static final long serialVersionUID = 941496534752616095L;
    private final Object value;
    private final Set<ConstraintViolation<Object>> constraintViolations;

    public ValidationServiceException(final Object value) {
        AppbaseUtil.notNull(value, "value");

        this.value = value;
        this.constraintViolations = Set.of();
    }

    public ValidationServiceException(final Object value,
                                      final Set<ConstraintViolation<Object>> constraintViolations)
    {
        AppbaseUtil.notNull(value, "value");
        AppbaseUtil.notNull(constraintViolations, "constraintViolations");

        this.value = value;
        this.constraintViolations = constraintViolations;
    }

    public ValidationServiceException(final String message,
                                      final Object value,
                                      final Set<ConstraintViolation<Object>> constraintViolations)
    {
        super(message);

        AppbaseUtil.notNull(value, "value");
        AppbaseUtil.notNull(constraintViolations, "constraintViolations");

        this.value = value;
        this.constraintViolations = constraintViolations;
    }

    public ValidationServiceException(final String message,
                                      final Throwable cause,
                                      final Object value,
                                      final Set<ConstraintViolation<Object>> constraintViolations)
    {
        super(message, cause);

        AppbaseUtil.notNull(value, "value");
        AppbaseUtil.notNull(constraintViolations, "constraintViolations");

        this.value = value;
        this.constraintViolations = constraintViolations;
    }

    public ValidationServiceException(final Throwable cause,
                                      final Object value,
                                      final Set<ConstraintViolation<Object>> constraintViolations)
    {
        super(cause);

        AppbaseUtil.notNull(value, "value");
        AppbaseUtil.notNull(constraintViolations, "constraintViolations");

        this.value = value;
        this.constraintViolations = constraintViolations;
    }

    public ValidationServiceException(final String message,
                                      final Throwable cause,
                                      final boolean enableSuppression,
                                      final boolean writableStackTrace,
                                      final Object value,
                                      final Set<ConstraintViolation<Object>> constraintViolations)
    {
        super(message, cause, enableSuppression, writableStackTrace);

        AppbaseUtil.notNull(value, "value");
        AppbaseUtil.notNull(constraintViolations, "constraintViolations");

        this.value = value;
        this.constraintViolations = constraintViolations;
    }

    public Object getValue() {
        return value;
    }

    public Set<ConstraintViolation<Object>> getConstraintViolations() {
        return constraintViolations;
    }

    @Override
    public String toString() {
        return "ValidationServiceException{" +
                "value=" + value +
                ", constraintViolations=" + constraintViolations +
                "} " + super.toString();
    }
}
