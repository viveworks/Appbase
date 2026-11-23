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
package com.viveworks.appbase.client.service.r2dbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code appbase-client-r2dbc-mariadb.yaml} must have a loadable {@code -container} twin;
 * {@code appbase-client-r2dbc.yaml} must NOT (P-050 N13, issue internal work item 953).
 *
 * <p>⚠️ <b>WHAT THIS DOES NOT ASSERT.</b> It does not prove the CALL SITE routes — that is
 * {@code config-twin-drift.sh}'s job, because proving it by execution would need a reachable MariaDB.
 * <b>This test proves the twin ships; the gate proves it is reached.</b>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class R2dbcConfigTwinResolutionTest {
    private static final String MARIADB_BASE = "appbase-client-r2dbc-mariadb.yaml";
    private static final String MARIADB_TWIN = "appbase-client-r2dbc-mariadb-container.yaml";

    @AfterEach
    void clearProfile() {
        System.clearProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY);
    }

    @Test
    void theContainerProfileNamesALoadableTwin() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        assertEquals(
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + MARIADB_TWIN,
                AppbaseTestInfraProfile.resolve(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + MARIADB_BASE));

        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(MARIADB_BASE),
                MARIADB_BASE + " is missing from the test classpath");
        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(MARIADB_TWIN),
                MARIADB_TWIN + " is missing from the test classpath — the container profile would name a"
                        + " resource that does not exist");
    }

    @Test
    void anUnsetProfileLeavesTheResourceByteIdentical() {
        final String base = AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + MARIADB_BASE;
        assertEquals(base, AppbaseTestInfraProfile.resolve(base));
    }

    @Test
    void theInMemoryResourceHasNoTwinAndMustNotBeRouted() {
        // 🔴 THE INVARIANT, ASSERTED. appbase-client-r2dbc.yaml is r2dbc:h2:mem:///t1 with host: null --
        // no infrastructure, so no twin, so R2dbcClientServiceTestHelper stays unwired on purpose.
        // resolve() derives a twin name UNCONDITIONALLY under the container profile, so wiring it would
        // name a file nothing ships. This asserts the hazard, which is what makes the omission a
        // decision rather than an oversight.
        assertNull(
                getClass().getClassLoader().getResourceAsStream("appbase-client-r2dbc-container.yaml"),
                "appbase-client-r2dbc.yaml has no host to redirect; it must not gain a twin");
    }
}
