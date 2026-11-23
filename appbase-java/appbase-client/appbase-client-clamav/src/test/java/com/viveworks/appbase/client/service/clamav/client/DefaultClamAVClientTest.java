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

package com.viveworks.appbase.client.service.clamav.client;

import com.viveworks.appbase.client.service.clamav.ClamAVClientTestHelper;
import com.viveworks.appbase.client.service.clamav.ClamAVClientTestUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "ClamAVClientServiceTests.enabled", matches = "true")
class DefaultClamAVClientTest {
    private static DefaultClamAVClient defaultClamAVClient;

    @BeforeAll
    static void setUp() {
        defaultClamAVClient = ClamAVClientTestHelper.defaultClamAVClient();
    }

    @AfterAll
    static void tearDown() {
        if (defaultClamAVClient != null) {
            defaultClamAVClient.shutdown();
        }
    }

    @Test
    void testPing() {
        ClamAVClientTestUtil.pingTest(defaultClamAVClient);
    }

    @Test
    void testScanPath() {
        ClamAVClientTestUtil.scanPathTest(defaultClamAVClient);
    }

    @Test
    void testScanStream() {
        ClamAVClientTestUtil.scanStreamTest(defaultClamAVClient);
    }
}
