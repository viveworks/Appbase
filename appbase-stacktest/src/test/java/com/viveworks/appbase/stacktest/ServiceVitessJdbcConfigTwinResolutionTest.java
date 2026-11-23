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
package com.viveworks.appbase.stacktest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code appbase-service-vitess-jdbc.yaml} is {@code localhost:15306} and must have a loadable
 * {@code -container} twin in THIS module (P-050 N13, issue internal work item 953).
 *
 * <p><b>Per module, deliberately.</b> Three modules carry a byte-identical copy of this resource on
 * their own test classpath, so one shared test class could pass while another module's copy had no
 * twin. The duplication of this class mirrors a duplication that already exists in the tree.
 *
 * <p>⚠️ <b>WHAT THIS DOES NOT ASSERT.</b> It does not prove the call sites route — that is
 * {@code config-twin-drift.sh}'s job, because proving it by execution would need a reachable vtgate.
 * <b>This test proves the twin ships; the gate proves it is reached.</b>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ServiceVitessJdbcConfigTwinResolutionTest {
    private static final String BASE = "appbase-service-vitess-jdbc.yaml";
    private static final String TWIN = "appbase-service-vitess-jdbc-container.yaml";

    @AfterEach
    void clearProfile() {
        System.clearProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY);
    }

    @Test
    void theContainerProfileNamesALoadableTwin() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        assertEquals(
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + TWIN,
                AppbaseTestInfraProfile.resolve(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + BASE));

        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(BASE),
                BASE + " is missing from this module's test classpath");
        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(TWIN),
                TWIN + " is missing from this module's test classpath — the container profile would name"
                        + " a resource that does not exist");
    }

    @Test
    void anUnsetProfileLeavesTheResourceByteIdentical() {
        final String base = AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + BASE;
        assertEquals(base, AppbaseTestInfraProfile.resolve(base));
    }
}
