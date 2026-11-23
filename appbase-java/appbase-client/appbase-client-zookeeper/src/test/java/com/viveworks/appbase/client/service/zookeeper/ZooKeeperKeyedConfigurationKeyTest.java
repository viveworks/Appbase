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

package com.viveworks.appbase.client.service.zookeeper;

import com.viveworks.appbase.client.service.zookeeper.configuration.DefaultZooKeeperClientServiceConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * The ZooKeeper test resource named its keyed configuration set
 * {@code keyedDefaultAerospikeClientIntegrationServiceConfigurations} — an <b>Aerospike</b> key in a
 * <b>ZooKeeper</b> config, while {@code DefaultZooKeeperClientService:233} reads
 * {@code getKeyedDefaultZooKeeperClientIntegrationServiceConfigurations()}.
 *
 * <p>🔴 <b>Nothing failed, and that is the whole problem.</b> The object mapper is configured with
 * {@code FAIL_ON_UNKNOWN_PROPERTIES = false} ({@code ObjectMapperConfigurator:89}), so the misspelled key
 * was silently discarded and the real one defaulted to {@code null}. The shipped value was {@code null}
 * either way, which is exactly why it survived: <b>null-under-the-wrong-name and null-under-the-right-name
 * are the same object.</b> Anyone who later populated it would have watched their configuration vanish
 * with no error.
 *
 * <p>This asserts the key by <b>behaviour</b> — a populated set arrives at the accessor the service
 * actually calls — rather than by grepping the YAML for a string, which would assert config text and
 * prove nothing about binding.
 *
 * <p>Repo-wide check when this was found: every other module names its own type correctly, including
 * {@code vitess-jasync → Jasync} and {@code vitess-jdbc → Jdbc}, which are right because those wrap
 * those client types. ZooKeeper was the only mismatch.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ZooKeeperKeyedConfigurationKeyTest {
    private static final String FIXTURE = "classpath:///zooKeeperKeyedConfigurationKey.yaml";
    private static final String NODE = "zooKeeperKeyed";

    private static DefaultZooKeeperClientServiceConfiguration load() throws IOException {
        return YamlUtil.readConfigurationFromFile(
                FIXTURE, null, null, NODE, DefaultZooKeeperClientServiceConfiguration.class);
    }

    /**
     * The arm with teeth: rename the key in the fixture to the Aerospike spelling and this returns
     * {@code null}, because the mapper discards what it does not recognise.
     */
    @Test
    void aPopulatedKeyedSetReachesTheAccessorTheServiceCalls() throws IOException {
        final DefaultZooKeeperClientServiceConfiguration configuration = load();

        Assertions.assertNotNull(
                configuration.getKeyedDefaultZooKeeperClientIntegrationServiceConfigurations(),
                "the keyed set must bind — a null here means the YAML key does not match the accessor");
        Assertions.assertEquals(
                1,
                configuration.getKeyedDefaultZooKeeperClientIntegrationServiceConfigurations().size(),
                "exactly the one keyed configuration in the fixture");
    }

    /**
     * The control. Without it, a fixture that failed to load at all would satisfy nothing above for the
     * wrong reason — an empty configuration object would also have a null keyed set.
     */
    @Test
    void theFixtureItselfLoads() throws IOException {
        Assertions.assertNotNull(
                load().getDefaultDefaultZooKeeperClientIntegrationServiceConfiguration(),
                "control: the fixture really deserialises, so the assertion above means what it says");
    }
}
