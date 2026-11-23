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
package com.viveworks.appbase.stacktest.test;

import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class TestUtil {
    public static String randomKey() {
        return ConfigurationService.class.getName() + "." + randomValue();
    }

    /**
     * A process-unique value. Previously {@code Math.random() * 24.09} — absurdly low entropy that
     * collided across parallel/repeated runs and made stack-test entity keys (emails, SKUs) flaky
     * (AUD-003). A UUID cannot collide, so uniqueness constraints and equality assertions are stable.
     */
    public static String randomValue() {
        return UUID.randomUUID().toString();
    }

    public static String randomId() {
        return randomValue();
    }

    public static long timestamp1() {
        return System.currentTimeMillis();
    }

    public static long timestamp2() {
        return timestamp1() + 1000000L;
    }
}
