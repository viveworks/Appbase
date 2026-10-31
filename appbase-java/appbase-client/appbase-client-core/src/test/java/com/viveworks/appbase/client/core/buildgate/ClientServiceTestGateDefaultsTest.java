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

package com.viveworks.appbase.client.core.buildgate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Build-convention regression test guarding the Surefire gate that decides which per-backend
 * client integration tests run by default.
 *
 * <p>Every backend's driver tests are annotated
 * {@code @EnabledIfSystemProperty(named = "<Backend>ClientServiceTests.enabled", matches = "true")},
 * and the default value of each of those system properties is declared once, in the
 * {@code maven-surefire-plugin} configuration of the {@code appbase-client} aggregator POM. A
 * backend whose flag defaults to {@code true} runs against a live service on an ordinary offline
 * build and fails on connection-refused. The Elasticsearch flag was the sole one inverted to
 * {@code true} (spec T6); this test fails if any {@code *ClientServiceTests.enabled} default is
 * ever set — or reverted — to {@code true}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ClientServiceTestGateDefaultsTest {

    /** Element name suffix that identifies a per-backend integration-test gate flag. */
    private static final String GATE_FLAG_SUFFIX = "ClientServiceTests.enabled";

    /**
     * Lower bound on the number of gate flags the aggregator POM is expected to declare. There are
     * currently eighteen backends; additions remain valid, but losing a declared gate must fail.
     * This also refuses to pass vacuously if the POM is parsed but the
     * flags cannot be found (a wrong file, a restructured POM, or a namespace regression).
     */
    private static final int MIN_EXPECTED_GATE_FLAGS = 18;

    @Test
    void everyBackendIntegrationTestGateDefaultsToDisabled() throws Exception {
        final File pom = locateAppbaseClientPom();
        final TreeMap<String, String> gateDefaults = readGateFlagDefaults(pom);

        // Non-vacuity guards: the POM was found, parsed, and the flags were actually located.
        assertTrue(
                gateDefaults.size() >= MIN_EXPECTED_GATE_FLAGS,
                () -> "Expected at least " + MIN_EXPECTED_GATE_FLAGS + " '*" + GATE_FLAG_SUFFIX
                        + "' gate flags in " + pom + " but found " + gateDefaults.size()
                        + ": " + gateDefaults.keySet());
        assertTrue(
                gateDefaults.containsKey("ElasticsearchClientServiceTests.enabled"),
                () -> "Elasticsearch gate flag missing from " + pom + "; found " + gateDefaults.keySet());

        // The actual convention: no backend integration-test gate defaults to on.
        final List<String> defaultedOn = new ArrayList<>();
        for (final var entry : gateDefaults.entrySet()) {
            if (!"false".equals(entry.getValue())) {
                defaultedOn.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        assertEquals(
                List.of(),
                defaultedOn,
                () -> "Backend integration-test gates must default to disabled ('false') so an offline "
                        + "build does not run driver tests against a live service; these default to on: "
                        + defaultedOn + " in " + pom);
    }

    /**
     * Matches a gate flag <em>name</em> wherever it appears as a Java string literal, e.g.
     * {@code "RedisClientServiceTests.enabled"}. The backend prefix is required to be non-empty, which
     * is what excludes this class's own {@link #GATE_FLAG_SUFFIX} constant — a bare
     * {@code "ClientServiceTests.enabled"} is the suffix itself, not a gate anybody consults.
     */
    private static final Pattern GATE_FLAG_CONSULT =
            Pattern.compile("\"([A-Za-z0-9_]+" + Pattern.quote(GATE_FLAG_SUFFIX) + ")\"");

    /**
     * Lower bound on the number of distinct gate flags the client test sources are expected to consult.
     * Same purpose as {@link #MIN_EXPECTED_GATE_FLAGS}: additions remain valid, but losing any of
     * the eighteen currently consulted gates fails (including a moved module tree or broken regex).
     */
    private static final int MIN_EXPECTED_GATE_CONSULTS = 18;

    /**
     * Every gate flag the client test sources actually consult must be declared in the aggregator POM.
     *
     * <p>{@link #everyBackendIntegrationTestGateDefaultsToDisabled()} constrains the flags the POM
     * declares; it is by construction blind to a flag the POM never mentions. That blind spot is not
     * hypothetical: {@code RedisClientServiceTests.enabled} was consulted by four Redis test classes
     * while being declared in no POM in the repository, leaving the one gate with no declared default
     * and absent from the block that indexes the gate set — the same block the epic plan sends driver-IT
     * implementers to in order to read the gate names. {@code build.sh} passed the flag explicitly,
     * which is precisely what kept the gap invisible: the only entry point that compensated for it was
     * also the only one routinely run.
     *
     * <p>Only the used-implies-declared direction is asserted. The converse (a declared flag nothing
     * consults) is deliberately <em>not</em> checked here, because gate flags are legitimately consulted
     * from modules outside {@code appbase-client} — the S3 flag is shared with {@code appbase-cloud} —
     * so a reverse check scoped to this tree would fail for correct code.
     */
    @Test
    void everyConsultedIntegrationTestGateIsDeclaredInThePom() throws Exception {
        final File pom = locateAppbaseClientPom();
        final TreeMap<String, String> declared = readGateFlagDefaults(pom);
        final TreeSet<String> consulted = readGateFlagConsults(pom.getParentFile().toPath());

        // Non-vacuity guard: a scan that found nothing must not read as agreement.
        assertTrue(
                consulted.size() >= MIN_EXPECTED_GATE_CONSULTS,
                () -> "Expected at least " + MIN_EXPECTED_GATE_CONSULTS + " distinct '*"
                        + GATE_FLAG_SUFFIX + "' consults under " + pom.getParentFile()
                        + " but found " + consulted.size() + ": " + consulted);

        final List<String> undeclared = new ArrayList<>();
        for (final String flag : consulted) {
            if (!declared.containsKey(flag)) {
                undeclared.add(flag);
            }
        }
        assertEquals(
                List.of(),
                undeclared,
                () -> "Every integration-test gate the client tests consult must be declared in " + pom
                        + " so it has a recorded default and is visible in the block that indexes the "
                        + "gate set; these are consulted but undeclared: " + undeclared
                        + " (declared: " + declared.keySet() + ")");
    }

    @Test
    void sourceScanExcludesItsOwnCompilationUnit(@TempDir final Path clientRoot) throws Exception {
        final Path scanner = clientRoot.resolve(
                "appbase-client-core/src/test/java/com/viveworks/appbase/client/core/buildgate/"
                        + "ClientServiceTestGateDefaultsTest.java");
        Files.createDirectories(scanner.getParent());
        Files.writeString(scanner, "// \"RetiredClientServiceTests.enabled\"");
        assertEquals(new TreeSet<>(), readGateFlagConsults(clientRoot),
                "Scanner examples must not masquerade as backend gate consults");
    }

    @Test
    void sourceScanKeepsRealConsultsAndExcludesBuildOutput(@TempDir final Path clientRoot)
            throws Exception {
        final Path tests = clientRoot.resolve("backend/src/test/java");
        Files.createDirectories(tests);
        Files.writeString(tests.resolve("AnnotatedTest.java"),
                "@EnabledIfSystemProperty(named = \"AnnotatedClientServiceTests.enabled\", matches = \"true\")");
        // A same-named file in another module is not this scanner's compilation unit.
        Files.writeString(tests.resolve("ClientServiceTestGateDefaultsTest.java"),
                "Boolean.getBoolean(\"ReactiveClientServiceTests.enabled\")");
        Files.writeString(clientRoot.resolve("backend/pom.xml"), "<project/>");
        final Path generated = clientRoot.resolve("backend/target/generated-test-sources");
        Files.createDirectories(generated);
        Files.writeString(generated.resolve("StaleTest.java"),
                "\"StaleClientServiceTests.enabled\"");
        assertEquals(new TreeSet<>(List.of("AnnotatedClientServiceTests.enabled",
                        "ReactiveClientServiceTests.enabled")), readGateFlagConsults(clientRoot));
    }

    @Test
    void sourceScanIgnoresTargetSegmentsAboveClientRoot(@TempDir final Path temporaryDirectory)
            throws Exception {
        final Path clientRoot = temporaryDirectory.resolve("target/Appbase/appbase-client");
        final Path tests = clientRoot.resolve("backend/src/test/java");
        Files.createDirectories(tests);
        Files.writeString(tests.resolve("AnnotatedTest.java"),
                "@EnabledIfSystemProperty(named = \"AncestorClientServiceTests.enabled\", "
                        + "matches = \"true\")");

        assertEquals(new TreeSet<>(List.of("AncestorClientServiceTests.enabled")),
                readGateFlagConsults(clientRoot),
                "A target directory above the client root is not Maven build output for that root");
    }

    @Test
    void sourceScanKeepsJavaPackagesNamedTarget(@TempDir final Path clientRoot) throws Exception {
        final Path tests = clientRoot.resolve("backend/src/test/java/example/target");
        Files.createDirectories(tests);
        Files.writeString(tests.resolve("AnnotatedTest.java"),
                "@EnabledIfSystemProperty(named = \"TargetPackageClientServiceTests.enabled\", "
                        + "matches = \"true\")");

        assertEquals(new TreeSet<>(List.of("TargetPackageClientServiceTests.enabled")),
                readGateFlagConsults(clientRoot),
                "A Java package named target is source, not Maven build output");
    }

    /**
     * Collects every distinct gate flag name appearing as a string literal in a {@code .java} file
     * under the {@code appbase-client} tree. Scanning for the flag <em>name</em> rather than for the
     * {@code @EnabledIfSystemProperty} annotation is deliberate: a flag read through
     * {@code Boolean.getBoolean(...)} gates just as effectively, and
     * {@code DefaultRedisClientServiceReactiveTest} does exactly that — a scanner that knew only the
     * annotation would under-report and could pass while the gap it exists to catch was present.
     */
    private static TreeSet<String> readGateFlagConsults(final Path clientRoot) throws IOException {
        final Path scannerSource = clientRoot.resolve(
                "appbase-client-core/src/test/java/com/viveworks/appbase/client/core/buildgate/"
                        + "ClientServiceTestGateDefaultsTest.java");
        final TreeSet<String> flags = new TreeSet<>();
        try (Stream<Path> tree = Files.walk(clientRoot)) {
            for (final Path file : tree.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    // This scanner's examples and assertions are not backend gate consumers.
                    .filter(p -> !p.equals(scannerSource))
                    .filter(p -> isNotBuildOutput(clientRoot, clientRoot.relativize(p)))
                    .toList()) {
                // ISO-8859-1 rather than UTF-8 deliberately: every byte sequence decodes, so a source
                // file in an unexpected encoding cannot turn this test into an error. The pattern is
                // pure ASCII, so nothing it looks for can be mis-decoded by the single-byte charset.
                final Matcher matcher = GATE_FLAG_CONSULT.matcher(
                        Files.readString(file, StandardCharsets.ISO_8859_1));
                while (matcher.find()) {
                    flags.add(matcher.group(1));
                }
            }
        }
        return flags;
    }

    /**
     * Excludes Maven build output from the scan. This matters because of <em>when</em> this test runs:
     * Surefire executes it during {@code mvn test}, by which point every module has a {@code target/}
     * that can hold generated and copied sources. A stale generated copy naming a gate the tree no
     * longer declares would fail this test for a defect that does not exist — the fixture the test was
     * authored against (a clean checkout, where {@code target/} is absent) differs from the tree it
     * actually executes in. The repo's other tree scanners exclude {@code target/} for the same reason.
     *
     * <p>{@code file} must be relative to the scanned client root. A {@code target} is build output
     * only when its parent is a Maven module (has a {@code pom.xml}); this also covers nested modules.
     * A directory above the root, or a Java package named {@code target}, remains source.
     */
    private static boolean isNotBuildOutput(final Path clientRoot, final Path file) {
        Path parent = clientRoot;
        for (final Path segment : file) {
            if ("target".equals(segment.toString()) && Files.isRegularFile(parent.resolve("pom.xml"))) {
                return false;
            }
            parent = parent.resolve(segment);
        }
        return true;
    }

    /**
     * A {@link DocumentBuilder} hardened against XXE. Every POM this test reads is a trusted in-repo
     * file, but a build-time XML read has no business resolving external entities or a DOCTYPE, so
     * <em>all</em> parse sites here go through this one factory rather than a bare
     * {@code newInstance()} — the hardening cannot then drift between call sites.
     */
    private static DocumentBuilder hardenedBuilder() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder();
    }

    /**
     * Reads every {@code *ClientServiceTests.enabled} element anywhere in the given POM, keyed by
     * element name. In the {@code appbase-client} aggregator these gate flags are declared only in the
     * {@code maven-surefire-plugin} {@code systemPropertyVariables} block and appear nowhere else, so
     * a whole-document scan collects exactly the gate flags and nothing more.
     */
    private static TreeMap<String, String> readGateFlagDefaults(final File pom) throws Exception {
        final NodeList elements = hardenedBuilder().parse(pom).getElementsByTagName("*");

        final TreeMap<String, String> flags = new TreeMap<>();
        for (int i = 0; i < elements.getLength(); i++) {
            final Node node = elements.item(i);
            if (node instanceof Element element && element.getTagName().endsWith(GATE_FLAG_SUFFIX)) {
                flags.put(element.getTagName(), element.getTextContent().trim());
            }
        }
        return flags;
    }

    /**
     * Locates the {@code appbase-client} aggregator POM by walking up from the module base
     * directory. Fails rather than returning a fallback so that a POM that cannot be found is a red
     * test, never a silent pass.
     */
    private static File locateAppbaseClientPom() {
        File dir = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        for (int depth = 0; dir != null && depth < 8; depth++, dir = dir.getParentFile()) {
            final File candidate = new File(dir, "pom.xml");
            if (candidate.isFile() && isAppbaseClientAggregatorPom(candidate)) {
                return candidate;
            }
        }
        return fail("Could not locate the 'appbase-client' aggregator pom.xml by walking up from "
                + System.getProperty("user.dir"));
    }

    /**
     * True if this POM is the {@code appbase-client} aggregator: it declares
     * {@code <artifactId>appbase-client</artifactId>} <em>and</em> carries the gate-flag block. The
     * artifactId text alone is not decisive — a child module names {@code appbase-client} in its
     * {@code <parent>} block — so the gate-flag presence is what actually disambiguates.
     */
    private static boolean isAppbaseClientAggregatorPom(final File pom) {
        try {
            final NodeList artifactIds = hardenedBuilder().parse(pom).getElementsByTagName("artifactId");
            // Scan every <artifactId> in the document (project, parent, plugin and dependency ids
            // alike); a match on "appbase-client" is confirmed only when the marker gate-flag block is
            // also present, which is unique to the aggregator. Element order is not relied upon.
            for (int i = 0; i < artifactIds.getLength(); i++) {
                if ("appbase-client".equals(artifactIds.item(i).getTextContent().trim())) {
                    return pomContainsGateFlags(pom);
                }
            }
        } catch (final Exception ignored) {
            // A pom.xml that cannot be parsed here is simply not the one we are looking for.
        }
        return false;
    }

    private static boolean pomContainsGateFlags(final File pom) throws Exception {
        final NodeList elements = hardenedBuilder().parse(pom).getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.item(i).getNodeName().endsWith(GATE_FLAG_SUFFIX)) {
                return true;
            }
        }
        return false;
    }
}
