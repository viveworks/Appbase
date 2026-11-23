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

package com.viveworks.appbase.core.service.configuration.accessor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ComparableTypeTest {
    @Test
    void testComparableType() {
        final ComparableType<String> comparableType1 = ComparableType.from(String.class);

        final Class<String> type = comparableType1.getType();
        Assertions.assertEquals(String.class, type);

        final ComparableType<String> comparableType2 = new ComparableType<>(String.class);
        final int compareTo = comparableType1.compareTo(comparableType2);
        Assertions.assertEquals(0, compareTo);

        Assertions.assertEquals(comparableType1, comparableType2);
        Assertions.assertEquals(comparableType1.hashCode(), comparableType2.hashCode());
        Assertions.assertEquals(comparableType1.toString(), comparableType2.toString());
    }
}
