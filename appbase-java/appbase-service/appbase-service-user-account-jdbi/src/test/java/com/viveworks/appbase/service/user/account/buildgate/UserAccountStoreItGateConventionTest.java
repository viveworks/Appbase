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

package com.viveworks.appbase.service.user.account.buildgate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Build-convention regression guard for the live-backend test gate in
 * {@code appbase-service-user-account-jdbi} (P-051 · E1135 · task N11 · internal work item 1301).
 *
 * <p>The store tests in this module open a real JDBI {@code Handle} against the vtgate declared by
 * {@code appbase-service-vitess-jdbc.yaml} ({@code localhost:15306}). Until internal work item 1301 they were named
 * {@code *Test}, so Surefire ran them on every ordinary offline build and they failed on
 * connection-refused. internal work item 1301 reclassified them to {@code *IT} — which moves them off Surefire and onto
 * the Failsafe opt-in this module now declares — <b>and</b> gated them behind
 * {@code @EnabledIfSystemProperty(named = "&lt;Backend&gt;ClientServiceTests.enabled", matches = "true")},
 * matching the Aerospike store tests already gated that way in
 * {@code appbase-service-user-session-aerospike}.
 *
 * <p><b>Both halves are load-bearing, which is why this guard checks the second one.</b> The rename
 * alone is not enough: under {@code develop-profile}, {@code staging-profile} or
 * {@code release-profile} Failsafe <i>does</i> select {@code *IT} classes, so an ungated IT would run
 * against a backend that is not there and redden the build again. The annotation is what keeps it
 * skipped until a live vtgate is explicitly opted into.
 *
 * <p>This guard <b>discovers</b> the module's compiled {@code *IT} classes from its own test-output
 * directory rather than checking a fixed list, so a <i>newly added</i> ungated integration test is
 * caught too — that, not a revert of the four classes internal work item 1301 touched, is the realistic regression.
 * The fixed list below is used only as a non-vacuity floor.
 *
 * <p>⚠️ <b>What this does NOT assert.</b> It does not execute the gated classes, and so it does not
 * prove they pass against a live vtgate — that needs infrastructure this gate deliberately does not
 * require. It reads the annotation from the <b>compiled class's runtime metadata</b>, not from source
 * text, and asserts the gate that JUnit itself evaluates. <b>This guard proves the gate is present
 * and correctly addressed; a live-backend run proves the test behind it still works.</b>
 *
 * <p>⚠️ <b>And it does not catch a NEW {@code *Test} that opens a live backend.</b> Its contract is
 * "every {@code *IT} here is gated"; a freshly added {@code *Test} reaching a database reproduces the
 * original defect and this guard stays silent. Detecting that statically is call-graph analysis, not a
 * convention check. What is covered, and mutation-verified, is the realistic regression: an
 * {@code *IT} added or reverted without a gate, a gate removed, or a gate given the wrong value.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class UserAccountStoreItGateConventionTest {

    /** Element-name suffix identifying a per-backend integration-test gate flag. */
    private static final String GATE_FLAG_SUFFIX = "ClientServiceTests.enabled";

    /** The only value {@code @EnabledIfSystemProperty} may be asked to match for these gates. */
    private static final String GATE_MATCHES = "true";

    /**
     * The integration tests internal work item 1301 reclassified in this module. A non-vacuity floor only: the
     * assertions below apply to <b>every</b> discovered {@code *IT}, not merely to these.
     */
    private static final Set<String> RECLASSIFIED_BY_1301 = new TreeSet<>(Set.of(
            "com.viveworks.appbase.service.user.account.store.JdbiUserAccountStoreHealthCheckerIT",
            "com.viveworks.appbase.service.user.account.store.JdbiUserAccountStoreIT"));

    @Test
    void everyIntegrationTestInThisModuleIsGatedBehindABackendFlag() {
        final List<String> ungated = new ArrayList<>();
        for (final Class<?> integrationTest : discoverIntegrationTestClasses()) {
            final EnabledIfSystemProperty gate = integrationTest.getAnnotation(EnabledIfSystemProperty.class);
            if (gate == null) {
                ungated.add(integrationTest.getName() + " (no @EnabledIfSystemProperty)");
                continue;
            }
            if (!gate.named().endsWith(GATE_FLAG_SUFFIX)) {
                ungated.add(integrationTest.getName() + " (gate flag \"" + gate.named()
                        + "\" does not end in \"" + GATE_FLAG_SUFFIX + "\")");
                continue;
            }
            if (!GATE_MATCHES.equals(gate.matches())) {
                ungated.add(integrationTest.getName() + " (matches=\"" + gate.matches()
                        + "\", expected \"" + GATE_MATCHES + "\")");
            }
        }
        assertTrue(
                ungated.isEmpty(),
                "these integration tests would run against a backend that need not be there — each must"
                        + " carry @EnabledIfSystemProperty(named = \"<Backend>" + GATE_FLAG_SUFFIX
                        + "\", matches = \"" + GATE_MATCHES + "\"): " + ungated);
    }

    @Test
    void theDiscoveryWalkIsNonVacuous() {
        final Set<String> discovered = new TreeSet<>();
        for (final Class<?> integrationTest : discoverIntegrationTestClasses()) {
            discovered.add(integrationTest.getName());
        }
        assertFalse(
                discovered.isEmpty(),
                "integration-test discovery found nothing in " + testClassesRoot()
                        + " — the walk is broken and the gate assertion above would pass vacuously");
        assertTrue(
                discovered.containsAll(RECLASSIFIED_BY_1301),
                "discovery did not find every integration test internal work item 1301 reclassified. Expected at least "
                        + RECLASSIFIED_BY_1301 + " but found " + discovered
                        + " — a rename back to *Test would look exactly like this");
    }

    @Test
    void theReclassifiedTestsAreNoLongerSurefireSelected() {
        final List<String> stillSurefire = new ArrayList<>();
        for (final String integrationTest : RECLASSIFIED_BY_1301) {
            final String surefireName =
                    integrationTest.substring(0, integrationTest.length() - "IT".length()) + "Test";
            final Path compiled = testClassesRoot()
                    .resolve(surefireName.replace('.', java.io.File.separatorChar) + ".class");
            if (Files.isRegularFile(compiled)) {
                stillSurefire.add(surefireName);
            }
        }
        assertTrue(
                stillSurefire.isEmpty(),
                "these classes are back under a *Test name, so Surefire selects them again on every"
                        + " ordinary offline build and they will fail on connection-refused — the exact"
                        + " state internal work item 1301 removed: " + stillSurefire);
    }

    // --- discovery -----------------------------------------------------------------------------------

    /** Every compiled {@code *IT} class this module produces, loaded from its own test classpath. */
    private static List<Class<?>> discoverIntegrationTestClasses() {
        final Path root = testClassesRoot();
        final List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (final Path classFile : walk.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith("IT.class"))
                    .toList()) {
                final String binaryName = root.relativize(classFile).toString()
                        .replace(java.io.File.separatorChar, '.')
                        .replaceAll("\\.class$", "");
                if (binaryName.contains("$")) {
                    continue; // nested classes are not test containers in their own right
                }
                try {
                    classes.add(Class.forName(
                            binaryName, false, Thread.currentThread().getContextClassLoader()));
                } catch (final ClassNotFoundException | NoClassDefFoundError e) {
                    throw new IllegalStateException(
                            "compiled " + binaryName + " is not loadable from this module's test"
                                    + " classpath — discovery cannot vouch for its gate", e);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("failed to walk " + root, e);
        }
        return classes;
    }

    /**
     * This module's test-output directory, taken from where this very class was loaded from, so it is
     * correct regardless of the working directory Surefire happens to run in.
     */
    private static Path testClassesRoot() {
        final Path fromCodeSource = codeSourceRoot();
        if (fromCodeSource != null && Files.isDirectory(fromCodeSource)) {
            return fromCodeSource;
        }
        final Path conventional = Path.of("target", "test-classes").toAbsolutePath();
        if (Files.isDirectory(conventional)) {
            return conventional;
        }
        throw new IllegalStateException(
                "could not locate this module's test-classes directory (code source " + fromCodeSource
                        + ", conventional " + conventional + ")");
    }

    private static Path codeSourceRoot() {
        try {
            final java.security.CodeSource codeSource = java.lang.invoke.MethodHandles.lookup()
                    .lookupClass().getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }
            return Path.of(codeSource.getLocation().toURI());
        } catch (final java.net.URISyntaxException e) {
            return null;
        }
    }
}
