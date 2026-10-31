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

package com.viveworks.appbase.configwizard.security;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression coverage for Lens 4 / L4-03... L4-02 (CWE-400): config-wizard's
 * {@code /build} and {@code /write} endpoints applied no application-level field-count,
 * nesting-depth or per-value length bound, so an oversized structure reached
 * {@code FieldPrompter#buildFromMap} with no Appbase-specific rejection.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class FieldValuesLimitsTest {

    @Test
    void validate_withinAllBounds_doesNotThrow() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("host", "localhost");
        values.put("port", 9092);
        assertDoesNotThrow(() -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_nullMap_doesNotThrow() {
        assertDoesNotThrow(() -> FieldValuesLimits.validate(null));
    }

    @Test
    void validate_tooManyTopLevelFields_throws() {
        final Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < FieldValuesLimits.MAX_FIELD_COUNT + 1; i++) {
            values.put("field" + i, "value");
        }
        assertThrows(FieldValuesTooLargeException.class, () -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_exactlyAtFieldCountLimit_doesNotThrow() {
        final Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < FieldValuesLimits.MAX_FIELD_COUNT; i++) {
            values.put("field" + i, "value");
        }
        assertDoesNotThrow(() -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_tooManyFieldsCountedAcrossNestedCollections_throws() {
        // A single top-level key whose list value carries enough elements to cross the bound
        // when every element is counted — not just top-level keys.
        final Map<String, Object> values = new HashMap<>();
        final List<Object> many = new java.util.ArrayList<>();
        for (int i = 0; i < FieldValuesLimits.MAX_FIELD_COUNT + 1; i++) {
            many.add("x");
        }
        values.put("list", many);
        assertThrows(FieldValuesTooLargeException.class, () -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_nestingDeeperThanMaximum_throws() {
        Object innermost = "leaf";
        for (int depth = 0; depth <= FieldValuesLimits.MAX_NESTING_DEPTH + 1; depth++) {
            innermost = Map.of("nested", innermost);
        }
        final Map<String, Object> values = new HashMap<>();
        values.put("root", innermost);
        assertThrows(FieldValuesTooLargeException.class, () -> FieldValuesLimits.validate(values));
    }

    /**
     * {@code validate(Map.of("root", nestedMap(levels)))}'s leaf is walked at depth
     * {@code levels + 2} ({@code 1} for the outer map passed to {@code validate}, {@code +1} for
     * the "root" entry, {@code +levels} for the wrapper chain). {@code levels =
     * MAX_NESTING_DEPTH - 2} therefore puts the leaf's check at exactly {@code MAX_NESTING_DEPTH}.
     */
    private static Object nestedMap(final int levels) {
        Object innermost = "leaf";
        for (int i = 0; i < levels; i++) {
            innermost = Map.of("nested", innermost);
        }
        return innermost;
    }

    @Test
    void validate_nestingExactlyAtMaximum_doesNotThrow() {
        final Map<String, Object> values = new HashMap<>();
        values.put("root", nestedMap(FieldValuesLimits.MAX_NESTING_DEPTH - 2));
        assertDoesNotThrow(() -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_nestingOneLevelPastMaximum_throws() {
        final Map<String, Object> values = new HashMap<>();
        values.put("root", nestedMap(FieldValuesLimits.MAX_NESTING_DEPTH - 1));
        assertThrows(FieldValuesTooLargeException.class, () -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_oversizeStringValue_throws() {
        final Map<String, Object> values = new HashMap<>();
        values.put("comment", "x".repeat(FieldValuesLimits.MAX_VALUE_LENGTH + 1));
        assertThrows(FieldValuesTooLargeException.class, () -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_stringValueExactlyAtMaximumLength_doesNotThrow() {
        final Map<String, Object> values = new HashMap<>();
        values.put("comment", "x".repeat(FieldValuesLimits.MAX_VALUE_LENGTH));
        assertDoesNotThrow(() -> FieldValuesLimits.validate(values));
    }

    // Adversarial-QA finding (code-review gate re-run, this PR): walk()'s Map branch recursed
    // only into entry.getValue() and never checked entry.getKey() against MAX_VALUE_LENGTH, so an
    // oversized map KEY bypassed the size gate entirely while still depositing an arbitrarily
    // large string into the object graph this class exists to bound.
    @Test
    void validate_oversizeMapKey_throws() {
        final Map<String, Object> values = new HashMap<>();
        values.put("x".repeat(FieldValuesLimits.MAX_VALUE_LENGTH + 1), "v");
        assertThrows(FieldValuesTooLargeException.class, () -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_mapKeyExactlyAtMaximumLength_doesNotThrow() {
        final Map<String, Object> values = new HashMap<>();
        values.put("x".repeat(FieldValuesLimits.MAX_VALUE_LENGTH), "v");
        assertDoesNotThrow(() -> FieldValuesLimits.validate(values));
    }

    @Test
    void validate_oversizeNestedMapKey_throws() {
        // The bypass is not limited to the top-level map: a key on any NESTED map entry must be
        // checked the same way, since the walker recurses through every level.
        final Map<String, Object> values = new HashMap<>();
        final Map<String, Object> nested = new HashMap<>();
        nested.put("x".repeat(FieldValuesLimits.MAX_VALUE_LENGTH + 1), "v");
        values.put("outer", nested);
        assertThrows(FieldValuesTooLargeException.class, () -> FieldValuesLimits.validate(values));
    }
}
