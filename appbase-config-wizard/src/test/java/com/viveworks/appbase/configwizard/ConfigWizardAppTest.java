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

package com.viveworks.appbase.configwizard;

import com.viveworks.appbase.configwizard.web.ConfigWizardWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfigWizardApp}.
 *
 * <p>Tests cover the top-level Picocli wiring: help output, version, and sub-command dispatch.
 * Detailed console flow tests live in {@code ConfigurationsConsoleFeatureTest}.
 *
 * <p>Inline mocking ({@code mock-maker-inline} via
 * {@code src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker}) is
 * required because several dependencies are declared {@code final}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ConfigWizardAppTest {

    @Mock
    private ConfigWizardApp.WebServerFactory webServerFactory;

    @Mock
    private ConfigWizardWebServer webServer;

    /**
     * Smoke-tests that {@code --help} returns exit code 0.
     * Uses the production no-arg constructor — {@code call()} is never invoked when help is
     * requested.
     */
    @Test
    void help_exitCodeIsZero() {
        int exitCode = new CommandLine(new ConfigWizardApp()).execute("--help");
        assertEquals(0, exitCode);
    }

    /**
     * Verifies that the {@code web} sub-command returns exit code 0, prints the startup
     * messages, and does not open an interactive terminal.
     *
     * <p>The web server factory returns a mock server whose {@code bind()} and {@code join()}
     * do nothing by default.
     */
    @Test
    void web_subcommand_printsStartupMessages() throws Exception {
        when(webServerFactory.create(anyInt(), any(Path.class))).thenReturn(webServer);

        ConfigWizardApp.WebCommand webCmd = new ConfigWizardApp.WebCommand(webServerFactory);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(capture));
        try {
            int exitCode = new CommandLine(webCmd).execute();
            assertEquals(0, exitCode);
        } finally {
            System.setOut(original);
        }

        String stdout = capture.toString(StandardCharsets.UTF_8);
        assertTrue(stdout.contains("Starting appbase-config-wizard web UI"),
                "Expected startup message in stdout but got: " + stdout);
        assertTrue(stdout.contains("Open: http://localhost:"),
                "Expected URL hint in stdout but got: " + stdout);
    }

    /**
     * Verifies that the {@code console} sub-command responds to {@code --help} with exit code 0.
     * This confirms the sub-command is registered and its help text is accessible without
     * opening a real terminal.
     */
    @Test
    void console_subcommand_help_exitCodeIsZero() {
        int exitCode = new CommandLine(new ConfigWizardApp()).execute("console", "--help");
        assertEquals(0, exitCode);
    }

    @Test
    void webCommand_rootOption_defaultsToWorkingDirectory() {
        ConfigWizardApp.WebCommand command = new ConfigWizardApp.WebCommand();
        new CommandLine(command).parseArgs("--port", "8080");

        assertEquals(Path.of(System.getProperty("user.dir")), command.getRoot(),
                "AC9 — --root defaults to the current working directory when not supplied");
    }

    @Test
    void webCommand_rootOption_isHonouredWhenSupplied() {
        ConfigWizardApp.WebCommand command = new ConfigWizardApp.WebCommand();
        new CommandLine(command).parseArgs("--root", "/tmp/wizard-root");

        assertEquals(Path.of("/tmp/wizard-root"), command.getRoot());
    }
}
