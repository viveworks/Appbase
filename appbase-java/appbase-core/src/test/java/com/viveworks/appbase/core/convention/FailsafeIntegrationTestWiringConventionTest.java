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

package com.viveworks.appbase.core.convention;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * P-051 E01 N05 (internal work item 1295). A reactor-wide convention gate for the Failsafe integration-test wiring.
 *
 * <p>Failsafe is declared only in the root POM's {@code develop/staging/release} profile
 * {@code pluginManagement}; a module's {@code *IT.java} classes stay invisible to Failsafe until the
 * module <b>opts in</b> by re-declaring the plugin (with no version, so it inherits the managed version
 * and executions) inside those same three profiles. Miss that opt-in and the build stays green having
 * executed <b>zero</b> integration tests — the exact silent-skip this task removes.
 *
 * <p>This test walks the {@code appbase-java} reactor from disk (it parses POMs; it neither builds nor
 * runs any module) and asserts that <b>every module carrying an {@code *IT.java}</b> binds Failsafe in
 * all three profiles, save for a small, self-validating exclusion set. It is deliberately
 * <b>non-vacuous</b>: it fails if the walk discovers nothing, if any in-scope module loses its wiring,
 * if a new unwired IT-bearing module appears, or if the exclusion list rots.
 *
 * <p><b>Scope note.</b> internal work item 1295 fixes the task at the <b>12 driver modules</b> in {@link #NAMED_IT_MODULES}.
 * {@code appbase-service-user-account-r2dbc} also carries an IT ({@code R2dbcUserAccountStoreMariaDbIT},
 * added by internal work item 390 after this epic's plan was written); it is now wired into the three profiles and checked
 * by {@link #noItBearingModuleIsSilentlyUnwiredOutsideTheExclusion()} like every other IT-bearing module,
 * so {@link #OUT_OF_SCOPE_UNWIRED} is empty.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class FailsafeIntegrationTestWiringConventionTest {

    /** The three profiles whose {@code pluginManagement} defines the Failsafe version + executions. */
    private static final List<String> FAILSAFE_PROFILE_IDS =
            List.of("develop-profile", "staging-profile", "release-profile");

    /** The 12 modules internal work item 1295 names, as paths relative to {@code appbase-java/}. */
    private static final Set<String> NAMED_IT_MODULES = new TreeSet<>(Set.of(
            "appbase-bootstrap/appbase-bootstrap-servlet-undertow",
            "appbase-client/appbase-client-aerospike",
            "appbase-client/appbase-client-cassandra",
            "appbase-client/appbase-client-elasticsearch",
            "appbase-client/appbase-client-firebase",
            "appbase-client/appbase-client-jasync",
            "appbase-client/appbase-client-kafka",
            "appbase-client/appbase-client-r2dbc",
            "appbase-client/appbase-client-redis",
            "appbase-client/appbase-client-vitess",
            "appbase-client/appbase-client-zookeeper",
            "appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3"));

    /**
     * IT-bearing modules deliberately left unwired by internal work item 1295. Each entry MUST be an IT-bearing module
     * that is genuinely still unwired (enforced by {@link #theExclusionListIsHonest()}), so this set
     * cannot silently hide a regression. Currently empty: every IT-bearing module is wired.
     */
    private static final Set<String> OUT_OF_SCOPE_UNWIRED = new TreeSet<>(Set.<String>of());

    @Test
    void everyNamedItModuleBindsFailsafeInAllThreeProfiles() {
        Path reactor = reactorRoot();
        List<String> notWired = new ArrayList<>();
        for (String module : NAMED_IT_MODULES) {
            Path pom = reactor.resolve(module).resolve("pom.xml");
            Assertions.assertTrue(Files.isRegularFile(pom), "named module POM is missing: " + module);
            if (!bindsFailsafeInAllThreeProfiles(pom)) {
                notWired.add(module);
            }
        }
        Assertions.assertTrue(
                notWired.isEmpty(),
                "these named IT modules do not bind Failsafe in all of "
                        + FAILSAFE_PROFILE_IDS + " (no version, inheriting pluginManagement): " + notWired);
    }

    @Test
    void noItBearingModuleIsSilentlyUnwiredOutsideTheExclusion() {
        Path reactor = reactorRoot();
        Set<String> unwired = new TreeSet<>();
        for (String module : discoverItModules(reactor)) {
            if (!bindsFailsafeInAllThreeProfiles(reactor.resolve(module).resolve("pom.xml"))) {
                unwired.add(module);
            }
        }
        unwired.removeAll(OUT_OF_SCOPE_UNWIRED);
        Assertions.assertTrue(
                unwired.isEmpty(),
                "IT-bearing modules unwired for Failsafe and NOT in the documented exclusion set "
                        + OUT_OF_SCOPE_UNWIRED + ": " + unwired);
    }

    @Test
    void theDiscoveryWalkIsNonVacuous() {
        Path reactor = reactorRoot();
        Set<String> discovered = discoverItModules(reactor);
        Assertions.assertFalse(discovered.isEmpty(), "IT-module discovery found nothing — the walk is broken");
        Assertions.assertTrue(
                discovered.containsAll(NAMED_IT_MODULES),
                "IT-module discovery did not find every named module; missing: "
                        + minus(NAMED_IT_MODULES, discovered));
    }

    @Test
    void theExclusionListIsHonest() {
        Path reactor = reactorRoot();
        Set<String> discovered = discoverItModules(reactor);
        for (String excluded : OUT_OF_SCOPE_UNWIRED) {
            Assertions.assertTrue(
                    discovered.contains(excluded),
                    "exclusion names a module with no *IT.java (stale entry): " + excluded);
            Assertions.assertFalse(
                    bindsFailsafeInAllThreeProfiles(reactor.resolve(excluded).resolve("pom.xml")),
                    "exclusion names an already-wired module — remove it from OUT_OF_SCOPE_UNWIRED: " + excluded);
        }
    }

    // --- helpers -------------------------------------------------------------------------------------

    /** Locates {@code appbase-java} by walking up from the working directory (module basedir or reactor root). */
    private static Path reactorRoot() {
        Path here = Path.of("").toAbsolutePath();
        for (Path p = here; p != null; p = p.getParent()) {
            Path candidate = p.resolve("appbase-java");
            if (Files.isDirectory(candidate) && Files.isRegularFile(candidate.resolve("pom.xml"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("could not locate the appbase-java reactor from " + here);
    }

    /** Every module (path relative to {@code appbase-java/}) that owns at least one {@code *IT.java}. */
    private static Set<String> discoverItModules(Path reactor) {
        Set<String> modules = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(reactor)) {
            walk.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith("IT.java"))
                    .forEach(f -> {
                        Path module = moduleOf(f);
                        if (module != null) {
                            modules.add(reactor.relativize(module).toString().replace('\\', '/'));
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("failed to walk " + reactor, e);
        }
        return modules;
    }

    /** The module directory owning a test source file: the ancestor immediately above its {@code src/test/} root. */
    private static Path moduleOf(Path itFile) {
        for (Path p = itFile; p != null; p = p.getParent()) {
            Path parent = p.getParent();
            if (parent != null && p.getFileName().toString().equals("src")) {
                Path rel = p.relativize(itFile);
                if (rel.getNameCount() >= 1 && "test".equals(rel.getName(0).toString())) {
                    return parent;
                }
            }
        }
        // Fall back to the nearest ancestor that has a pom.xml (covers any non-standard layout).
        for (Path p = itFile.getParent(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("pom.xml"))) {
                return p;
            }
        }
        return null;
    }

    private static boolean bindsFailsafeInAllThreeProfiles(Path pom) {
        Element project = parse(pom);
        for (String id : FAILSAFE_PROFILE_IDS) {
            Element profile = profileById(project, id);
            if (profile == null || !hasVersionlessFailsafe(profile)) {
                return false;
            }
        }
        return true;
    }

    private static Element profileById(Element project, String id) {
        NodeList profiles = project.getElementsByTagName("profile");
        for (int i = 0; i < profiles.getLength(); i++) {
            Element profile = (Element) profiles.item(i);
            if (id.equals(directChildText(profile, "id"))) {
                return profile;
            }
        }
        return null;
    }

    /** A {@code maven-failsafe-plugin} declaration anywhere under {@code scope} that carries no explicit version. */
    private static boolean hasVersionlessFailsafe(Element scope) {
        NodeList plugins = scope.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            if ("maven-failsafe-plugin".equals(directChildText(plugin, "artifactId"))) {
                Assertions.assertNull(
                        directChild(plugin, "version"),
                        "Failsafe opt-in must not pin a version (inherit pluginManagement): " + scope);
                return true;
            }
        }
        return false;
    }

    private static Element directChild(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && tag.equals(n.getNodeName())) {
                return (Element) n;
            }
        }
        return null;
    }

    private static String directChildText(Element parent, String tag) {
        Element child = directChild(parent, tag);
        return child == null ? null : child.getTextContent().trim();
    }

    private static Element parse(Path pom) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            // Harden against XXE (CWE-611): these POMs are trusted, but the parser is not given the
            // chance to resolve a DOCTYPE or any external entity regardless of what it is fed.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(pom.toFile()).getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse " + pom, e);
        }
    }

    private static Set<String> minus(Set<String> a, Set<String> b) {
        Set<String> r = new TreeSet<>(a);
        r.removeAll(b);
        return r;
    }
}
