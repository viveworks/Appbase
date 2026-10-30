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

import java.util.Collection;
import java.util.Map;

/**
 * Bounds the size of a caller-supplied {@code fieldValues} map before it reaches
 * {@code FieldPrompter#buildFromMap} (Lens 4 / L4-02, CWE-400; OWASP A04:2021).
 *
 * <p>Config-wizard's {@code /api/configurations/{fqcn}/build} and {@code /write} endpoints (and
 * the import-scan equivalents) accepted an arbitrary {@code Map<String, Object>} with no
 * application-level bound on field count, nesting depth, or per-value size — an attacker could
 * amplify Jackson conversion and Hibernate Validator work with one oversized request. This class
 * enforces three independent bounds by walking the structure once:
 * <ul>
 *   <li>{@link #MAX_FIELD_COUNT} — the total number of map entries and collection elements
 *       encountered anywhere in the structure, not just the top level;</li>
 *   <li>{@link #MAX_NESTING_DEPTH} — how many levels of nested map/collection are permitted;</li>
 *   <li>{@link #MAX_VALUE_LENGTH} — the maximum length of any individual string value.</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class FieldValuesLimits {

    /**
     * Upper bound on the total number of map entries and collection elements anywhere in the
     * structure. Chosen generously above any real Appbase configuration class's field count
     * (the largest on this classpath has well under 100 properties, even nested).
     */
    public static final int MAX_FIELD_COUNT = 500;

    /** Upper bound on nested map/collection depth. Real configuration nesting is at most a few levels. */
    public static final int MAX_NESTING_DEPTH = 10;

    /** Upper bound on any single string value's length, in characters (64 KiB). */
    public static final int MAX_VALUE_LENGTH = 65_536;

    private FieldValuesLimits() {
    }

    /**
     * Validates {@code fieldValues} against all three bounds.
     *
     * @param fieldValues the caller-supplied structure; {@code null} is treated as empty and
     *                    always passes
     * @throws FieldValuesTooLargeException when any bound is exceeded
     */
    public static void validate(final Map<String, Object> fieldValues) {
        if (fieldValues == null) {
            return;
        }
        walk(fieldValues, 1, new int[] { 0 });
    }

    private static void walk(final Object value, final int depth, final int[] fieldCount) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new FieldValuesTooLargeException();
        }
        if (value instanceof Map<?, ?> map) {
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                countOne(fieldCount);
                // The key is not walked recursively (a map key is never itself a map/collection in
                // any structure this class validates), but it is the same free-form, caller-supplied
                // string a value can be, so it must be checked against MAX_VALUE_LENGTH the same
                // way -- otherwise an oversized key bypasses this bound entirely while still
                // depositing an arbitrarily large string into the object graph this class exists to
                // bound (adversarial-QA finding, code-review gate re-run).
                if (entry.getKey() instanceof CharSequence keyCharSequence
                        && keyCharSequence.length() > MAX_VALUE_LENGTH) {
                    throw new FieldValuesTooLargeException();
                }
                walk(entry.getValue(), depth + 1, fieldCount);
            }
        } else if (value instanceof Collection<?> collection) {
            for (final Object element : collection) {
                countOne(fieldCount);
                walk(element, depth + 1, fieldCount);
            }
        } else if (value instanceof CharSequence charSequence) {
            if (charSequence.length() > MAX_VALUE_LENGTH) {
                throw new FieldValuesTooLargeException();
            }
        }
    }

    private static void countOne(final int[] fieldCount) {
        fieldCount[0]++;
        if (fieldCount[0] > MAX_FIELD_COUNT) {
            throw new FieldValuesTooLargeException();
        }
    }
}
