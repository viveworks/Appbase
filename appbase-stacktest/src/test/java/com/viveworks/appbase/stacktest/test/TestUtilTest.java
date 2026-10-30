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

package com.viveworks.appbase.stacktest.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Regression for AUD-003: {@link TestUtil}'s id/value helpers must be collision-free.
 *
 * <p>They previously used {@code Math.random() * 24.09} (and call sites used
 * {@code (int)(Math.random() * 10000)}), a range small enough that entity keys — customer emails,
 * SKUs — collided across parallel/repeated runs and produced intermittent stack-test failures. These
 * assertions fail against that implementation and pass against the UUID one.
 *
 * @author Navaneeth Mysore G
 */
class TestUtilTest {

    private static final int N = 10_000;

    @Test
    void randomIdIsUniqueAcrossManyCalls() {
        final Set<String> seen = new HashSet<>(N * 2);
        for (int i = 0; i < N; i++) {
            seen.add(TestUtil.randomId());
        }
        // With the old Math.random()*24.09 this set would be tiny (heavy collisions); a UUID cannot collide.
        assertEquals(N, seen.size(), "randomId() produced duplicates — entity keys will collide and flake");
    }

    @Test
    void randomValueIsUniqueAcrossManyCalls() {
        final Set<String> seen = new HashSet<>(N * 2);
        for (int i = 0; i < N; i++) {
            seen.add(TestUtil.randomValue());
        }
        assertEquals(N, seen.size(), "randomValue() produced duplicates");
    }

    @Test
    void twoConsecutiveIdsDiffer() {
        assertNotEquals(TestUtil.randomId(), TestUtil.randomId());
    }
}
