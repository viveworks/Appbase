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

package com.viveworks.appbase.configwizard.console;

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ConfigWizardConsoleApp}.
 *
 * <p>Full interactive tests (menu dispatch, output rendering) are covered by pexpect tests
 * in Task 10. This class provides compile-level verification and structural assertions.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
final class ConfigWizardConsoleAppTest {

    /**
     * Verifies that {@link ConfigWizardConsoleApp} can be constructed with a fake feature and that
     * the feature contract compiles correctly. Actual menu-loop behaviour is tested in Task 10.
     */
    @Test
    void construction_withFakeFeature_compiles() {
        AtomicBoolean featureRan = new AtomicBoolean(false);
        ConsoleFeature fake = new ConsoleFeature() {
            @Override
            public String menuLabel() {
                return "Test";
            }

            @Override
            public void run(final Terminal t, final Path d) {
                featureRan.set(true);
            }
        };

        // Verify: the class can be instantiated and the feature label is accessible.
        // Full E2E loop behaviour (menu dispatch on 'E') is covered by pexpect (Task 10).
        new ConfigWizardConsoleApp(List.of(fake), null, Path.of("/tmp"));
        assertTrue(fake.menuLabel().equals("Test"), "Feature label should be 'Test'");
        assertTrue(!featureRan.get(), "Feature should not have run during construction");
    }
}
