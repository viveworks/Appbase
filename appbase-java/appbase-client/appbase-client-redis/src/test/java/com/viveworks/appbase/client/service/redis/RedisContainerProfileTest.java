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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

/**
 * P-050 N02 (internal work item 921) — the container profile end to end for one module, which is the pattern the remaining
 * modules follow.
 *
 * <p>⚠️ <b>Resolving to a filename is not the same as that file existing.</b> The resolver is unit-tested
 * in {@code AppbaseTestInfraProfileTest} against strings only, so on its own it would happily point every
 * module at a resource nobody created — and the failure would surface as a confusing load error inside an
 * integration test rather than here. These arms load the twin and read its host.
 *
 * <p>No Redis server is required: this reads configuration, it does not connect.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class RedisContainerProfileTest {
    private static final String CONTAINER_HOST = "appbase-infra";
    private static final String DEFAULT_HOST = "localhost";
    private static final String CONFIGURATION_NODE = "defaultDefaultRedisClientIntegrationServiceConfiguration";

    @AfterEach
    void clearProfile() {
        System.clearProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY);
    }

    @SuppressWarnings("unchecked")
    private static String hostIn(final String resource) throws IOException {
        final Map<String, Object> nodes = YamlUtil.readConfigurationNodes(resource);
        final Map<String, Object> configuration = (Map<String, Object>) nodes.get(CONFIGURATION_NODE);
        Assertions.assertNotNull(configuration, "node '" + CONFIGURATION_NODE + "' missing from " + resource);
        final Map<String, Object> hostPort = (Map<String, Object>) configuration.get("hostPortConfiguration");
        Assertions.assertNotNull(hostPort, "hostPortConfiguration missing from " + resource);
        return (String) hostPort.get("host");
    }

    @Test
    void containerProfileResolvesToAResourceThatExistsAndNamesTheAlias() throws IOException {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);

        final String resolved =
                AppbaseTestInfraProfile.resolve(RedisClientServiceTestHelper.REDIS_CLIENT_CONFIG_CLASSPATH_FILE);

        Assertions.assertEquals(
                CONTAINER_HOST,
                hostIn(resolved),
                "the twin must exist, parse, and name the docker network alias");
    }

    /**
     * 🔴 The arm that protects everyone who never runs a container topology. With no profile set, the
     * default resource is unchanged and still names {@code localhost}.
     */
    @Test
    void withoutTheProfileTheDefaultResourceIsUnchanged() throws IOException {
        final String resolved =
                AppbaseTestInfraProfile.resolve(RedisClientServiceTestHelper.REDIS_CLIENT_CONFIG_CLASSPATH_FILE);

        Assertions.assertEquals(
                RedisClientServiceTestHelper.REDIS_CLIENT_CONFIG_CLASSPATH_FILE,
                resolved,
                "unset: the resource is returned unchanged");
        Assertions.assertEquals(
                DEFAULT_HOST,
                hostIn(resolved),
                "unset: still localhost, exactly as before this change");
    }

    /**
     * An explicit per-module property must still win over the profile — otherwise the profile would
     * silently override a caller who deliberately named a file, which is a narrower switch overriding a
     * wider, more specific instruction.
     */
    @Test
    void anExplicitPropertyStillWinsOverTheProfile() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, AppbaseTestInfraProfile.CONTAINER);
        System.setProperty(RedisClientServiceTestHelper.REDIS_CLIENT_CONFIG_FILE_PROPERTY, "classpath:///chosen.yaml");
        try {
            Assertions.assertEquals(
                    "classpath:///chosen.yaml",
                    RedisClientServiceTestHelper.configFile(),
                    "an explicitly named file is not overridden by the profile");
        } finally {
            System.clearProperty(RedisClientServiceTestHelper.REDIS_CLIENT_CONFIG_FILE_PROPERTY);
        }
    }

    /**
     * 🔴 The arm that caught a real defect in this change. Written as
     * {@code System.getProperty(KEY, AppbaseTestInfraProfile.resolve(DEFAULT))} the default argument is
     * evaluated EAGERLY, so an unrecognised profile threw even for a caller who had explicitly named its
     * own file. The previous version of the test could not see it, because it re-implemented the
     * expression instead of calling the helper — a test that asserts a copy of the code cannot catch the
     * code.
     */
    @Test
    void anExplicitPropertySurvivesAnUnrecognisedProfile() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, "banana");
        System.setProperty(RedisClientServiceTestHelper.REDIS_CLIENT_CONFIG_FILE_PROPERTY, "classpath:///chosen.yaml");
        try {
            Assertions.assertEquals(
                    "classpath:///chosen.yaml",
                    RedisClientServiceTestHelper.configFile(),
                    "an explicit file wins even over a profile value that would otherwise be refused");
        } finally {
            System.clearProperty(RedisClientServiceTestHelper.REDIS_CLIENT_CONFIG_FILE_PROPERTY);
        }
    }

    /** With no explicit file, an unrecognised profile must still be refused. */
    @Test
    void withoutAnExplicitFileAnUnrecognisedProfileIsStillRefused() {
        System.setProperty(AppbaseTestInfraProfile.PROFILE_PROPERTY, "banana");
        Assertions.assertThrows(
                IllegalArgumentException.class,
                RedisClientServiceTestHelper::configFile,
                "the refusal is not lost by resolving lazily");
    }
}
