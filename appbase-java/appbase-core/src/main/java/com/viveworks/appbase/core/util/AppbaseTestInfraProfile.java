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

package com.viveworks.appbase.core.util;

/**
 * Selects which configuration resource an integration-test helper loads: the ordinary one, or its
 * {@code -container} twin (P-050 N02, issue internal work item 921).
 *
 * <p><b>Why a twin file rather than an overridden value.</b> The configuration objects are immutable —
 * {@code HostPortConfiguration} is {@code final} with a builder and no setters — and the YAML loader
 * performs no {@code ${VAR}} interpolation, which was established by execution rather than assumed
 * (see {@code YamlEnvSubstitutionSpikeTest}). There is therefore nothing to poke after load. A twin
 * resource needs no new mechanism at all: every helper already resolves its configuration file through a
 * system property.
 *
 * <p><b>Why ONE switch instead of per-module properties.</b> <b>22</b> distinct
 * {@code *_CONFIG_FILE_PROPERTY} constants already exist across the client modules — counted with
 * {@code grep -rhoE '[A-Z0-9_]+_CONFIG_FILE_PROPERTY' --include=*.java appbase-java --exclude-dir=target
 * | grep -v PREFIX | sort -u | wc -l} on 2026-08-01. A caller that had to know all of them would be
 * maintaining a list that goes stale the moment a module is added — silently, and in the direction of
 * *not* switching, which is the direction nobody notices.
 *
 * <p>🔴 <b>The unset case is load-bearing.</b> With no profile selected {@link #resolve} returns its
 * input unchanged, so CI and a developer laptop behave exactly as they do today. <i>A knob whose default
 * alters existing behaviour is a migration, not a knob.</i>
 *
 * <p>⚠️ <b>One qualification, because "byte-identical" was overstated:</b> {@link #configFile} treats a
 * blank-but-set property as unset, where the sibling convention returned the blank string verbatim. For
 * the seven call sites converted from that form, setting their own {@code *_CONFIG_FILE_PROPERTY} to
 * {@code ""} now falls back instead of yielding a blank path. Nobody does that in practice, but the
 * unqualified claim was wrong and is asserted either way by
 * {@code configFileTreatsABlankPropertyAsUnset}.
 *
 * <p>⚠️ <b>Only the system property is read, deliberately.</b> An environment-variable arm cannot be set
 * from inside a JVM, so it could not be unit-tested here — and an untested path in the thing that decides
 * which backend a suite runs against is precisely where "silently does nothing" hides.
 *
 * <p>🔴 <b>NOTHING PASSES {@code -Dappbase.infra.profile=container} TODAY, AND THIS COMMENT USED TO SAY
 * IT DID.</b> It read <i>"The container topology passes {@code -Dappbase.infra.profile=container}, which
 * is one fully-tested path"</i> — a claim about wiring that does not exist. Measured 2026-08-04 during
 * P-050's close-out sweep (internal work item 1055): the property appears in no {@code .sh}, {@code .json}, {@code .xml}
 * or {@code .yaml} outside comments and message text; {@code APPBASE_INFRA_HOST} appears nowhere outside
 * prose; and {@code devcontainer.json} carries no {@code --network} or {@code --network-alias}, so the
 * host name {@code appbase-infra} that the container-profile config twins resolve to is created by
 * nothing.
 *
 * <p>So the default — {@code standalone} — is the only profile any suite has ever actually run under,
 * and a lane that ran the integration tests today would resolve {@code localhost} and start its own copy
 * of the stack inside its own rootless daemon. That is the outcome P-050 exists to prevent, and the
 * mechanism to prevent it is specified but unbuilt. <b>The resolver below is correct and fully tested;
 * what is missing is every caller.</b> Registered on
 * internal work item 884.
 *
 * <p><i>A comment asserting a wiring that does not exist is worse than no comment: it tells the next
 * reader the question has been answered.</i>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AppbaseTestInfraProfile {
    /** System property naming the active infrastructure profile. Unset means "today's behaviour". */
    public static final String PROFILE_PROPERTY = "appbase.infra.profile";

    /** The only recognised profile: infrastructure runs in its own container, reached by a network alias. */
    public static final String CONTAINER = "container";

    private static final String TWIN_SUFFIX = "-container";
    private static final String YAML_EXTENSION = ".yaml";

    private AppbaseTestInfraProfile() {
    }

    /**
     * Resolve a configuration resource for the active profile.
     *
     * @param configurationResource the ordinary configuration resource, for example
     *                              {@code classpath:///appbase-client-redis.yaml}
     * @return the same resource when no profile is active, or its {@code -container} twin
     * @throws IllegalArgumentException if the profile property holds an unrecognised value
     */
    public static String resolve(final String configurationResource) {
        AppbaseUtil.notBlank(configurationResource, "configurationResource");

        final String profile = System.getProperty(PROFILE_PROPERTY);
        if (profile == null || AppbaseUtil.isBlank(profile)) {
            return configurationResource;
        }
        if (!CONTAINER.equals(profile)) {
            // Refuse rather than fall back. A profile switch that quietly ignores its input is how a
            // suite ends up running against the wrong backend while reporting success.
            throw new IllegalArgumentException(
                    "Unrecognised " + PROFILE_PROPERTY + " value '" + profile
                            + "' — the only recognised profile is '" + CONTAINER + "'");
        }
        return twin(configurationResource);
    }

    /**
     * Resolve a helper's configuration resource: an explicitly named file, else the profile's choice.
     *
     * <p>🔴 <b>The explicit property is read FIRST, and that ordering is the whole point of this method
     * existing.</b> Written inline as {@code System.getProperty(KEY, resolve(DEFAULT))} the default
     * argument evaluates <b>eagerly</b>, so an unrecognised profile throws even for a caller who named
     * its own file — a narrower, more specific instruction losing to a wider switch's failure. That
     * defect shipped once here and was caught by the security review (internal work item 921); it exists in one place now
     * so it cannot be re-introduced independently in each of the client helpers.
     *
     * <p>A blank-but-set property is treated as unset. The sibling convention established in
     * {@code 1deabe977} returns it verbatim, which yields a blank config path and a confusing failure
     * far from its cause.
     *
     * @param propertyName    the helper's own {@code *_CONFIG_FILE_PROPERTY}
     * @param defaultResource the helper's default classpath resource
     * @return the resource the helper should load
     */
    public static String configFile(final String propertyName, final String defaultResource) {
        AppbaseUtil.notBlank(propertyName, "propertyName");
        AppbaseUtil.notBlank(defaultResource, "defaultResource");

        final String explicit = System.getProperty(propertyName);
        if (explicit != null && !AppbaseUtil.isBlank(explicit)) {
            return explicit;
        }
        return resolve(defaultResource);
    }

    /**
     * Derive the twin resource by suffixing the BASENAME, leaving any directory segments alone.
     *
     * <p>Idempotent: a resource that already names a twin is returned unchanged, because helpers reach
     * this resolver from more than one entry point and {@code -container-container.yaml} would be a
     * file that does not exist.
     */
    private static String twin(final String configurationResource) {
        if (configurationResource.endsWith(TWIN_SUFFIX + YAML_EXTENSION)) {
            return configurationResource;
        }
        if (!configurationResource.endsWith(YAML_EXTENSION)) {
            throw new IllegalArgumentException(
                    "Cannot derive a container twin for '" + configurationResource
                            + "' — expected a resource ending in " + YAML_EXTENSION);
        }
        final int extensionAt = configurationResource.length() - YAML_EXTENSION.length();
        return configurationResource.substring(0, extensionAt) + TWIN_SUFFIX + YAML_EXTENSION;
    }
}
