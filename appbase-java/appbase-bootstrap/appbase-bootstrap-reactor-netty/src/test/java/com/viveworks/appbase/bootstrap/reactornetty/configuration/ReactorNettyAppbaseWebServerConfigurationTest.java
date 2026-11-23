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

package com.viveworks.appbase.bootstrap.reactornetty.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactorNettyAppbaseWebServerConfigurationTest {

    @Test
    void defaultMaxPageSizeIsOneThousand() {
        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder().build();

        Assertions.assertEquals(1000, configuration.getMaxPageSize());
    }

    @Test
    void defaultOffloadTimeoutSecondsIsThirty() {
        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder().build();

        Assertions.assertEquals(30, configuration.getOffloadTimeoutSeconds());
    }

    @Test
    void builderSetsAGivenMaxPageSize() {
        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder().maxPageSize(250).build();

        Assertions.assertEquals(250, configuration.getMaxPageSize());
    }

    @Test
    void builderSetsAGivenOffloadTimeoutSeconds() {
        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder().offloadTimeoutSeconds(60).build();

        Assertions.assertEquals(60, configuration.getOffloadTimeoutSeconds());
    }

    @Test
    void twoConfigurationsWithTheSameValuesAreEqual() {
        final ReactorNettyAppbaseWebServerConfiguration first =
                ReactorNettyAppbaseWebServerConfiguration.builder().maxPageSize(250).offloadTimeoutSeconds(60).build();
        final ReactorNettyAppbaseWebServerConfiguration second =
                ReactorNettyAppbaseWebServerConfiguration.builder().maxPageSize(250).offloadTimeoutSeconds(60).build();

        Assertions.assertEquals(first, second);
    }

    @Test
    void twoConfigurationsWithDifferentMaxPageSizeAreNotEqual() {
        final ReactorNettyAppbaseWebServerConfiguration first =
                ReactorNettyAppbaseWebServerConfiguration.builder().maxPageSize(250).build();
        final ReactorNettyAppbaseWebServerConfiguration second =
                ReactorNettyAppbaseWebServerConfiguration.builder().maxPageSize(500).build();

        Assertions.assertNotEquals(first, second);
    }

    @Test
    void twoConfigurationsWithDifferentOffloadTimeoutSecondsAreNotEqual() {
        final ReactorNettyAppbaseWebServerConfiguration first =
                ReactorNettyAppbaseWebServerConfiguration.builder().offloadTimeoutSeconds(30).build();
        final ReactorNettyAppbaseWebServerConfiguration second =
                ReactorNettyAppbaseWebServerConfiguration.builder().offloadTimeoutSeconds(60).build();

        Assertions.assertNotEquals(first, second);
    }
}
