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
package com.viveworks.appbase.client.service.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * This helper performs TWO configuration lookups and only one of them is routed (P-050 N13, issue
 * internal work item 953). The asymmetry is the point, so it is asserted rather than left to a comment.
 *
 * <p><b>The jdbc lookup is routed</b> — {@code appbase-client-jdbc.yaml} is {@code localhost:3306} and
 * has a twin. <b>The jdbi lookup is not</b> — {@code appbase-client-jdbi.yaml} declares no host at all,
 * so it can have no twin, and {@link AppbaseTestInfraProfile#resolve} would name a file nothing ships.
 *
 * <p>⚠️ <b>MEASURED 2026-08-02, and it is why this class asserts less than its siblings do:</b>
 * {@code appbase-client-jdbc.yaml} is <b>not on this module's test classpath</b>. The module depends on
 * {@code appbase-client-jdbc}'s main artifact, not its test-jar, and the resource lives in that module's
 * {@code src/test/resources}. So neither the base nor its twin is loadable here and this class cannot
 * assert their presence the way {@code JdbcConfigTwinResolutionTest} does.
 *
 * <p>That gap is pre-existing and currently unreachable: the only two callers of this helper's no-arg
 * methods — {@code DefaultJdbiClientServiceTest} and {@code JdbiClientServiceGuiceModuleTest} — are both
 * gated on {@code JdbiClientServiceTests.enabled}, defaulted to {@code false} in
 * {@code appbase-client/pom.xml}, so the module's whole suite is two tests and both are skipped. Every
 * other caller sets {@code JDBC_CONFIG_FILE_PROPERTY} explicitly and never reaches the default.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class JdbiConfigTwinResolutionTest {
    private static final String JDBI_BASE = "appbase-client-jdbi.yaml";
    private static final String JDBI_TWIN = "appbase-client-jdbi-container.yaml";

    @AfterEach
    void clearProfile() {
        System.clearProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY);
    }

    @Test
    void theJdbiResourceHasNoTwinAndMustNotBeRouted() {
        assertNotNull(
                getClass().getClassLoader().getResourceAsStream(JDBI_BASE),
                JDBI_BASE + " is missing from the test classpath");

        // 🔴 THE INVARIANT, ASSERTED. appbase-client-jdbi.yaml declares no host, so config-twin-drift.sh
        // would refuse a twin for it ("no host line differs from the base"). It therefore must not have
        // one, and the jdbi lookup must stay raw.
        assertNull(
                getClass().getClassLoader().getResourceAsStream(JDBI_TWIN),
                JDBI_BASE + " has no host to redirect; it must not gain a twin");
    }

    @Test
    void resolveWouldNameAFileNothingShipsWhichIsWhyTheJdbiLookupIsRaw() {
        // This asserts the HAZARD, not the fix. resolve() derives a twin name unconditionally once the
        // profile is set -- it does not check that the file exists. Wiring the jdbi lookup would
        // therefore turn a working test into a container-only failure. Asserting it here is what makes
        // the omission in JdbiClientServiceTestHelper a decision rather than an oversight.
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);

        final String resolved =
                AppbaseTestInfraProfile.resolve(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + JDBI_BASE);

        assertEquals(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + JDBI_TWIN, resolved);
        assertNull(
                getClass().getClassLoader().getResourceAsStream(JDBI_TWIN),
                "resolve() named " + resolved + ", which does not exist — the reason this lookup is"
                        + " deliberately not wired");
    }

    @Test
    void anUnsetProfileLeavesTheJdbiResourceByteIdentical() {
        final String base = AppbaseConstants.CLASSPATH_FILE_PREFIX + "/" + JDBI_BASE;
        assertEquals(base, AppbaseTestInfraProfile.resolve(base));
    }
}
