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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ValidationServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
    }

    @AfterAll
    static void tearDown() {
        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testConcat() {
        final String prefix = ValidationService.prefix(ValidationServiceTest.class);
        final String concat = ValidationService.concat(prefix, "fieldName", "violation");
        final String expected = ValidationService.MESSAGE_TEMPLATE_PREFIX +
                ValidationServiceTest.class.getName() +
                ValidationService.MESSAGE_TEMPLATE_SEPARATOR +
                "fieldName" +
                ValidationService.MESSAGE_TEMPLATE_SEPARATOR +
                "violation" +
                ValidationService.MESSAGE_TEMPLATE_SEPARATOR +
                ValidationService.MESSAGE +
                ValidationService.MESSAGE_TEMPLATE_SUFFIX;
        Assertions.assertEquals(expected, concat);
    }

    @Test
    void testPrefix() {
        final String prefix = ValidationService.prefix(ValidationServiceTest.class);
        Assertions.assertEquals(ValidationServiceTest.class.getName(), prefix);
    }

    @Test
    void testNodeNamePrefix() {
        final String nodeNamePrefix = ValidationService.nodeNamePrefix(ValidationServiceTest.class);
        final String expected = ValidationService.prefix(ValidationServiceTest.class) +
                ValidationService.MESSAGE_TEMPLATE_SEPARATOR;
        Assertions.assertEquals(expected, nodeNamePrefix);
    }

    @Test
    void testAddViolationOrThrow() {
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            ValidationService.addViolationOrThrow("template.message", "propertyNode", null);
        });
    }

    @Test
    void testValidate() {
        final TestObject testObject = new TestObject();
        Assertions.assertFalse(
                ValidationService.validate(testObject, "TestObject", validationService, null));

        testObject.setName("NAME");
        Assertions.assertTrue(
                ValidationService.validate(testObject, "TestObject", validationService, null));
    }

    @Test
    void testValidateNotNull() {
        final TestObject testObject = new TestObject();
        Assertions.assertFalse(
                ValidationService.validateNull(
                        testObject,
                        "{messageTemplate}",
                        "name",
                        TestObject.class.getName(),
                        validationService,
                        null));

        testObject.setName("NAME");
        Assertions.assertTrue(
                ValidationService.validateNull(
                        testObject,
                        "{messageTemplate}",
                        "name",
                        TestObject.class.getName(),
                        validationService,
                        null));

        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            ValidationService.validateNull(
                    null,
                    "{messageTemplate.null.message}",
                    "name",
                    TestObject.class.getName(),
                    validationService,
                    null);
        });
    }

    @Test
    void testValidatePositive() {
        Assertions.assertTrue(
                ValidationService.validatePositive(
                        1,
                        "{messageTemplate}",
                        "number",
                        TestObject.class.getName(),
                        validationService,
                        null));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            ValidationService.validatePositive(
                    0,
                    "{messageTemplate}",
                    "number",
                    TestObject.class.getName(),
                    validationService,
                    null);
        });
    }

    @Test
    void testValidateNotBlank() {
        Assertions.assertTrue(
                ValidationService.validateBlank(
                        "text",
                        "{messageTemplate}",
                        "number",
                        TestObject.class.getName(),
                        validationService,
                        null));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
           ValidationService.validateBlank(
                   "",
                   "{messageTemplate}",
                   "number",
                   TestObject.class.getName(),
                   validationService,
                   null);
        });
    }

    @Test
    void testValidateEnumNotNull() {
        Assertions.assertTrue(
                ValidationService.validateEnumNull(
                        TestEnum.A,
                        "{messageTemplate}",
                        "number",
                        null));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
                ValidationService.validateEnumNull(
                        null,
                        "{messageTemplate}",
                        "number",
                        null);
        });
    }

    @Test
    void testValidateCollection() {
        Assertions.assertTrue(
                ValidationService.validateCollection(
                        List.of("I1"),
                        "{listMessageTemplate}",
                        "list",
                        "{listItemMessageTemplate}",
                        "listItem",
                        "prefix",
                        validationService,
                        null));
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            Assertions.assertTrue(
                    ValidationService.validateCollection(
                            null,
                            "{listMessageTemplate}",
                            "list",
                            "{listItemMessageTemplate}",
                            "listItem",
                            "prefix",
                            validationService,
                            null));
        });
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            Assertions.assertTrue(
                    ValidationService.validateCollection(
                            List.of(),
                            "{listMessageTemplate}",
                            "list",
                            "{listItemMessageTemplate}",
                            "listItem",
                            "prefix",
                            validationService,
                            null));
        });

        final List<String> list = new ArrayList<>();
        list.add("I1");
        list.add(null);
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            Assertions.assertTrue(
                    ValidationService.validateCollection(
                            list,
                            "{listMessageTemplate}",
                            "list",
                            "{listItemMessageTemplate}",
                            "listItem",
                            "prefix",
                            validationService,
                            null));
        });
    }

    static class TestObject {
        @NotNull
        private String name;

        TestObject() {
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    enum TestEnum {
        A,

        B;
    }
}
