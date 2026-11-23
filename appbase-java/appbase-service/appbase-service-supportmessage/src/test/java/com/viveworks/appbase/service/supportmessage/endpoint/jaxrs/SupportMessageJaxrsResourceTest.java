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

package com.viveworks.appbase.service.supportmessage.endpoint.jaxrs;

import com.viveworks.appbase.service.AppbaseServiceConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lens 4 (input/output validation) regression coverage for L4-03: {@code readSupportMessages}'s
 * {@code limit} query parameter was bounded only by {@code @PositiveOrZero}, so a caller could
 * request an arbitrarily large result set (CWE-400). These tests exercise the real Jakarta Bean
 * Validation {@link ExecutableValidator} against the resource method's own declared parameter
 * constraints -- the identical mechanism the JAX-RS runtime (Jersey's bean-validation feature)
 * applies to this method in production -- so they prove the annotation's effect rather than a
 * mock's behaviour.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class SupportMessageJaxrsResourceTest {
    private static ExecutableValidator executableValidator;

    @BeforeAll
    static void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        executableValidator = validator.forExecutables();
    }

    @Test
    void readSupportMessages_rejectsLimitAboveMaximum() throws NoSuchMethodException {
        final SupportMessageJaxrsResource resource = new SupportMessageJaxrsResource();
        final Method method = readSupportMessagesMethod();

        final Object[] parameterValues = { 0, Integer.MAX_VALUE, null, null };
        final Set<ConstraintViolation<Object>> violations =
                executableValidator.validateParameters(resource, method, parameterValues);

        assertEquals(
                1,
                violations.size(),
                "limit above the documented maximum must be rejected by bean validation");
    }

    @Test
    void readSupportMessages_acceptsLimitAtMaximum() throws NoSuchMethodException {
        final SupportMessageJaxrsResource resource = new SupportMessageJaxrsResource();
        final Method method = readSupportMessagesMethod();

        final Object[] parameterValues =
                { 0, AppbaseServiceConstants.MAX_SUPPORT_MESSAGE_READ_LIMIT, null, null };
        final Set<ConstraintViolation<Object>> violations =
                executableValidator.validateParameters(resource, method, parameterValues);

        assertTrue(violations.isEmpty(), "limit exactly at the documented maximum must be accepted");
    }

    @Test
    void readSupportMessages_rejectsLimitOneAboveMaximum() throws NoSuchMethodException {
        final SupportMessageJaxrsResource resource = new SupportMessageJaxrsResource();
        final Method method = readSupportMessagesMethod();

        final Object[] parameterValues =
                { 0, AppbaseServiceConstants.MAX_SUPPORT_MESSAGE_READ_LIMIT + 1, null, null };
        final Set<ConstraintViolation<Object>> violations =
                executableValidator.validateParameters(resource, method, parameterValues);

        assertEquals(1, violations.size(), "limit one above the maximum must still be rejected");
    }

    private static Method readSupportMessagesMethod() throws NoSuchMethodException {
        return SupportMessageJaxrsResource.class.getMethod(
                "readSupportMessages",
                Integer.class,
                Integer.class,
                String.class,
                jakarta.servlet.ServletContext.class);
    }
}
