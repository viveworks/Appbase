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

package com.viveworks.appbase.configwizard.console;

import com.viveworks.appbase.configwizard.ConsoleColors;
import org.jline.terminal.Terminal;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;

/**
 * Main console loop for Appbase Config Wizard.
 *
 * <p>Shows a numbered menu of registered {@link ConsoleFeature} instances and dispatches
 * to the selected feature. Pressing {@code [E]} exits the application.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ConfigWizardConsoleApp {

    private final List<ConsoleFeature> features;
    private final Terminal terminal;
    private final Path sessionOutputDir;

    /**
     * Constructs an {@code ConfigWizardConsoleApp} with the given features, terminal, and output directory.
     *
     * @param features         ordered list of console features to display in the menu
     * @param terminal         the JLine3 terminal for I/O; caller owns lifecycle
     * @param sessionOutputDir the directory (or file path) for session YAML output
     */
    public ConfigWizardConsoleApp(final List<ConsoleFeature> features,
                           final Terminal terminal,
                           final Path sessionOutputDir) {
        this.features = List.copyOf(features);
        this.terminal = terminal;
        this.sessionOutputDir = sessionOutputDir;
    }

    /**
     * Runs the main menu loop until the user presses {@code E} (exit) or EOF.
     *
     * <p>Prints the banner once on entry, then repeatedly:
     * <ol>
     *   <li>Displays the numbered feature menu</li>
     *   <li>Reads a single character</li>
     *   <li>Dispatches to the matching {@link ConsoleFeature} or exits on {@code E}</li>
     * </ol>
     *
     * @throws Exception if a feature throws or the terminal encounters an I/O error
     */
    public void run() throws Exception {
        ConsoleColors.printBanner(terminal);
        while (true) {
            printMainMenu();
            int choice = TerminalIo.readChar(terminal);
            if (choice == 'E' || choice == 'e' || choice == -1) {
                break;
            }
            int idx = choice - '1';
            if (idx >= 0 && idx < features.size()) {
                features.get(idx).run(terminal, sessionOutputDir);
            }
        }
        terminal.writer().println(ConsoleColors.yellow("Goodbye.", terminal));
        terminal.writer().flush();
    }

    /**
     * Prints the main menu with numbered feature entries and an exit option.
     */
    private void printMainMenu() {
        PrintWriter out = terminal.writer();
        out.println();
        out.println(ConsoleColors.cyan("── Main Menu ──────────────────────────────────", terminal));
        for (int i = 0; i < features.size(); i++) {
            out.println("  [" + (i + 1) + "] " + features.get(i).menuLabel());
        }
        out.println("  [E] Exit");
        out.println(ConsoleColors.cyan("───────────────────────────────────────────────", terminal));
        out.print("Choice: ");
        out.flush();
    }

}
