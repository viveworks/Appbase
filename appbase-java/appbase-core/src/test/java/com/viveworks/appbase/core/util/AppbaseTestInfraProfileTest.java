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

package com.viveworks.appbase.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * P-050 N02 (internal work item 921). The integration-test infrastructure is not always on {@code localhost}: in the
 * multi-container agent topology it runs in a separate container reachable by a docker network alias.
 *
 * <p>This resolves <b>which configuration resource</b> a test helper should load — the ordinary one, or
 * its {@code -container} twin. It does <b>not</b> substitute values, because the configuration objects are
 * immutable (builder-only) and the YAML loader performs no interpolation — see
 * {@link com.viveworks.appbase.core.util.jackson.YamlEnvSubstitutionSpikeTest}, which establishes that by
 * execution.
 *
 * <p>🔴 <b>The unset case is the load-bearing one.</b> With no profile selected the resolver must return
 * its input unchanged, so CI and a developer laptop behave exactly as they do today. A knob whose default
 * alters existing behaviour is a migration, not a knob.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AppbaseTestInfraProfileTest {
    private static final String PLAIN = "classpath:///appbase-client-redis.yaml";
    private static final String TWIN = "classpath:///appbase-client-redis-container.yaml";

    @AfterEach
    void clearProfile() {
        System.clearProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY);
    }

    @Test
    void unsetProfileReturnsTheResourceUnchanged() {
        Assertions.assertEquals(
                PLAIN,
                AppbaseTestInfraProfile.resolve(PLAIN),
                "unset: today's behaviour must be byte-identical");
    }

    @Test
    void containerProfileSelectsTheTwin() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        Assertions.assertEquals(
                TWIN,
                AppbaseTestInfraProfile.resolve(PLAIN),
                "container: the -container twin is selected");
    }

    /**
     * ⚠️ An unrecognised value must NOT silently select the twin, and must not silently select the plain
     * file either — it means the operator asked for something that does not exist, and a profile switch
     * that quietly ignores its input is how a test suite ends up running against the wrong backend while
     * reporting success.
     */
    @Test
    void unknownProfileIsRefused() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, "banana");
        final IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> AppbaseTestInfraProfile.resolve(PLAIN),
                "an unrecognised profile must fail loudly, not fall back");
        Assertions.assertTrue(
                thrown.getMessage().contains("banana"),
                "the refusal names the offending value: " + thrown.getMessage());
    }

    /**
     * The twin is derived by suffixing the basename, so a path with directories must not have its
     * directory segments mangled.
     */
    @Test
    void twinIsDerivedFromTheBasenameOnly() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        Assertions.assertEquals(
                "classpath:///nested/dir/appbase-client-kafka-container.yaml",
                AppbaseTestInfraProfile.resolve("classpath:///nested/dir/appbase-client-kafka.yaml"),
                "only the basename gains the suffix");
    }

    /**
     * A resource that is already a twin must not become {@code -container-container.yaml}. Helpers are
     * chained through this resolver from more than one entry point, so idempotence is a real requirement
     * rather than a theoretical nicety.
     */
    @Test
    void resolvingATwinIsIdempotent() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        Assertions.assertEquals(
                TWIN,
                AppbaseTestInfraProfile.resolve(TWIN),
                "resolving an already-resolved twin changes nothing");
    }

    // ── configFile(property, default) ───────────────────────────────────────────────────────────────
    // 🔴 Nine call sites across eight modules depend on this method, and until the review panel asked,
    // its only exercise was INCIDENTAL — through one unrelated test of one caller. A shared method with
    // no direct test is worse than the duplication it replaced: the duplication at least failed in one
    // module at a time.

    private static final String PROP = "appbase.test.helper.config.arbitrary";

    @AfterEach
    void clearHelperProperty() {
        System.clearProperty(PROP);
    }

    @Test
    void configFileReturnsAnExplicitlyNamedResource() {
        System.setProperty(PROP, "classpath:///chosen.yaml");
        Assertions.assertEquals(
                "classpath:///chosen.yaml",
                AppbaseTestInfraProfile.configFile(PROP, PLAIN),
                "an explicitly named file wins");
    }

    @Test
    void configFileFallsBackWhenThePropertyIsUnset() {
        Assertions.assertEquals(
                PLAIN,
                AppbaseTestInfraProfile.configFile(PROP, PLAIN),
                "unset: today's behaviour, byte-identical");
    }

    @Test
    void configFileSelectsTheTwinUnderTheContainerProfile() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        Assertions.assertEquals(
                TWIN,
                AppbaseTestInfraProfile.configFile(PROP, PLAIN),
                "container: the default resolves to its twin");
    }

    /**
     * 🔴 THE BEHAVIOUR CHANGE, ASSERTED. Before this consolidation eight helpers used
     * {@code System.getProperty(KEY, DEFAULT)}, which substitutes the default only when the property is
     * ABSENT — a property set to {@code ""} came back as {@code ""}, yielding a blank config path and a
     * failure far from its cause. This treats blank as unset. It is a deliberate divergence from the
     * convention set in {@code 1deabe977}, and it was documented in the javadoc but asserted nowhere,
     * which is how a change reads as considered while remaining unverified.
     */
    @Test
    void configFileTreatsABlankPropertyAsUnset() {
        System.setProperty(PROP, "   ");
        Assertions.assertEquals(
                PLAIN,
                AppbaseTestInfraProfile.configFile(PROP, PLAIN),
                "a blank-but-set property falls back rather than returning blank");
    }

    /**
     * The explicit file must survive a profile value that {@code resolve()} would refuse — the lazy
     * ordering this method exists for. Written eagerly, this throws.
     */
    @Test
    void configFileWithAnExplicitFileSurvivesAnUnrecognisedProfile() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, "banana");
        System.setProperty(PROP, "classpath:///chosen.yaml");
        Assertions.assertEquals(
                "classpath:///chosen.yaml",
                AppbaseTestInfraProfile.configFile(PROP, PLAIN),
                "an explicit file is not lost to a profile that would otherwise be refused");
    }

    /** …and with no explicit file, the refusal must still happen. */
    @Test
    void configFileWithoutAnExplicitFileStillRefusesAnUnrecognisedProfile() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, "banana");
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> AppbaseTestInfraProfile.configFile(PROP, PLAIN),
                "the refusal is not lost by resolving lazily");
    }
}
