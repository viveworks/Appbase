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
package com.viveworks.appbase.client.service.vitess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Every resource this module names directly must have a loadable {@code -container} twin, and must
 * resolve to it under the container profile (P-050 N13, issue internal work item 953).
 *
 * <p>⚠️ <b>WHAT THIS DOES NOT ASSERT, stated so it is not mistaken for coverage it lacks.</b> It does
 * not prove the CALL SITES route. Proving that by execution would need a reachable database, so
 * {@code config-twin-drift.sh} is what checks it — the gate refuses a resource that has a twin and a
 * declaration but no routing. The two are complementary and neither is sufficient: <b>this test proves
 * the twin ships, the gate proves it is reached.</b>
 *
 * <p>Unlike the tests around it this one is NOT gated on {@code VitessClientServiceTests.enabled}: it
 * opens no connection and needs no infrastructure, so gating it would make the check disappear in
 * exactly the default configuration where a missing twin should be caught.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class VitessConfigTwinResolutionTest {
    private static final String CONTAINER_SUFFIX = "-container.yaml";
    private static final String YAML_SUFFIX = ".yaml";

    @AfterEach
    void clearProfile() {
        System.clearProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "appbase-client-vitess-jasync.yaml",
            "appbase-client-vitess-jdbc.yaml",
            "appbase-client-vitess-mysql-jdbc.yaml"})
    void theContainerProfileNamesALoadableTwin(final String baseName) {
        final String twinName = baseName.replace(YAML_SUFFIX, CONTAINER_SUFFIX);

        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        assertEquals(
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + twinName,
                AppbaseTestInfraProfile.resolve(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + baseName));

        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(baseName),
                baseName + " is missing from the test classpath");
        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(twinName),
                twinName + " is missing from the test classpath — the container profile would name a"
                        + " resource that does not exist");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "appbase-client-vitess-jasync.yaml",
            "appbase-client-vitess-jdbc.yaml",
            "appbase-client-vitess-mysql-jdbc.yaml"})
    void anUnsetProfileLeavesTheResourceByteIdentical(final String baseName) {
        final String base = AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + baseName;
        assertEquals(base, AppbaseTestInfraProfile.resolve(base));
    }
}
