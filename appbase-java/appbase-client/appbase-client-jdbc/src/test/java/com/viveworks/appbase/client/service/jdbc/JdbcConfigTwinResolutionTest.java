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
package com.viveworks.appbase.client.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code appbase-client-jdbc.yaml} is {@code localhost:3306} and must have a loadable
 * {@code -container} twin (P-050 N13, issue internal work item 953).
 *
 * <p>Unlike the four resources this task wraps at their call sites, this one is the helper's OWN
 * default, so {@link JdbcClientServiceTestHelper} is wired through
 * {@link AppbaseTestInfraProfile#configFile} instead — the mechanism the nine helpers N02 converted
 * already use. The distinction is not stylistic: {@code configFile} reads the caller's explicit
 * property FIRST, which is what keeps an explicitly named file beating the profile.
 *
 * <p>⚠️ <b>WHAT THIS DOES NOT ASSERT.</b> It does not prove the helper routes — that is
 * {@code config-twin-drift.sh}'s job. <b>This test proves the twin ships; the gate proves it is
 * reached.</b>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class JdbcConfigTwinResolutionTest {
    private static final String BASE = "appbase-client-jdbc.yaml";
    private static final String TWIN = "appbase-client-jdbc-container.yaml";

    @AfterEach
    void clearProperties() {
        System.clearProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY);
        System.clearProperty(JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY);
    }

    @Test
    void theContainerProfileNamesALoadableTwin() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        assertEquals(
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + TWIN,
                AppbaseTestInfraProfile.configFile(
                        JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY,
                        JdbcClientServiceTestHelper.JDBC_CONFIG_CLASSPATH_FILE));

        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(BASE),
                BASE + " is missing from the test classpath");
        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(TWIN),
                TWIN + " is missing from the test classpath — the container profile would name a"
                        + " resource that does not exist");
    }

    @Test
    void anUnsetProfileLeavesTheResourceByteIdentical() {
        assertEquals(
                JdbcClientServiceTestHelper.JDBC_CONFIG_CLASSPATH_FILE,
                AppbaseTestInfraProfile.configFile(
                        JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY,
                        JdbcClientServiceTestHelper.JDBC_CONFIG_CLASSPATH_FILE));
    }

    @Test
    void anExplicitlyNamedFileStillBeatsTheContainerProfile() {
        // 🔴 THE PRECEDENCE, ASSERTED WHERE IT IS CONSUMED. The six service call sites in this task
        // set this very property, and they rely on their choice surviving the profile. Inverting it
        // shipped once in N02 and was caught by the internal work item 921 security review; AppbaseTestInfraProfileTest
        // covers the mechanism, this covers the helper's constants being fed to it in the right order.
        final String explicit = AppbaseConstants.CLASSPATH_FILE_PREFIX + "/some-other-resource.yaml";
        System.setProperty(JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY, explicit);
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);

        assertEquals(
                explicit,
                AppbaseTestInfraProfile.configFile(
                        JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY,
                        JdbcClientServiceTestHelper.JDBC_CONFIG_CLASSPATH_FILE));
    }
}
