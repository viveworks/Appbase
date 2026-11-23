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

package com.viveworks.appbase.configwizard.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.ConsoleColors;
import com.viveworks.appbase.configwizard.configurations.importcfg.ImportService;
import com.viveworks.appbase.configwizard.configurations.importcfg.TypeInferenceEngine;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.TypeCandidateDto;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.console.ConsoleFeature;
import com.viveworks.appbase.configwizard.console.TerminalIo;
import com.viveworks.appbase.configwizard.menu.FilterMenu;
import com.viveworks.appbase.configwizard.security.ImportScanLimits;
import com.viveworks.appbase.configwizard.security.ImportScanTooLargeException;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Console feature providing the Configurations sub-menu:
 *
 * <ol>
 *   <li>{@code [1] Create} — interactively build a new YAML configuration</li>
 *   <li>{@code [2] Import} — import an existing YAML file from a directory and
 *       optionally edit its fields before saving</li>
 *   <li>{@code [B] Back} — return to the main menu</li>
 * </ol>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ConfigurationsConsoleFeature implements ConsoleFeature {

    private final ConfigScanner scanner;
    private final FieldPrompter prompter;
    private final YamlWriter writer;
    private final TypeInferenceEngine engine;
    private final ImportService importService;
    private final MenuFactory menuFactory;
    private final ObjectMapper previewMapper = YamlUtil.getYamlMapper();

    /**
     * Production constructor — creates all dependencies from the supplied collaborators.
     *
     * @param scanner  discovers Appbase configuration classes on the classpath
     * @param prompter introspects and builds configuration objects from field-value maps
     * @param writer   writes serialised YAML to a target path
     */
    public ConfigurationsConsoleFeature(final ConfigScanner scanner,
                                        final FieldPrompter prompter,
                                        final YamlWriter writer) {
        this(scanner, prompter, writer, FilterMenu::new);
    }

    /**
     * Package-private testing constructor that accepts an injected {@link MenuFactory}.
     *
     * @param scanner     discovers Appbase configuration classes on the classpath
     * @param prompter    introspects and builds configuration objects from field-value maps
     * @param writer      writes serialised YAML to a target path
     * @param menuFactory factory for creating the filter-and-pick menu
     */
    ConfigurationsConsoleFeature(final ConfigScanner scanner,
                                 final FieldPrompter prompter,
                                 final YamlWriter writer,
                                 final MenuFactory menuFactory) {
        this.scanner = scanner;
        this.prompter = prompter;
        this.writer = writer;
        this.menuFactory = menuFactory;
        this.engine = new TypeInferenceEngine(scanner.scan());
        this.importService = new ImportService(scanner, prompter, writer);
    }

    @Override
    public String menuLabel() {
        return "Configurations";
    }

    /**
     * Displays the Configurations sub-menu in a loop until the user chooses {@code [B] Back}.
     *
     * @param terminal         JLine3 terminal for I/O; caller owns lifecycle
     * @param sessionOutputDir the default write location for this session
     * @throws Exception on terminal I/O error, YAML write failure, or user abort
     */
    @Override
    public void run(final Terminal terminal, final Path sessionOutputDir) throws Exception {
        while (true) {
            printSubMenu(terminal);
            char choice = readSubMenuChar(terminal);
            switch (choice) {
                case '1' -> runCreate(terminal, sessionOutputDir);
                case '2' -> runImport(terminal, sessionOutputDir);
                case 'B', 'b' -> { return; }
                case (char) -1 -> { return; }
                default -> { /* ignore invalid input */ }
            }
        }
    }

    // ── Create flow ────────────────────────────────────────────────────────────────

    /**
     * Interactive create flow: scan → pick class → prompt fields → YAML preview → write → repeat?
     *
     * <p>This is the logic previously in {@code ConfigWizardApp.runConsole()}, now encapsulated here.
     * The loop continues until the user declines to create another configuration.
     *
     * @param terminal         JLine3 terminal for I/O
     * @param sessionOutputDir path to which YAML files are written
     * @throws Exception on terminal I/O error, YAML write failure, or user abort
     */
    private void runCreate(final Terminal terminal, final Path sessionOutputDir) throws Exception {
        while (true) {
            FilterMenu menu = menuFactory.create(terminal);
            List<ConfigClassInfo> all = scanner.scan();
            ConfigClassInfo chosen = menu.pick(all);
            Map<String, Object> values = prompter.prompt(chosen, terminal);
            Object built;
            try {
                built = prompter.buildFromMap(chosen, values);
            } catch (IllegalArgumentException e) {
                PrintWriter out = terminal.writer();
                out.println(ConsoleColors.red("Cannot build configuration: " + e.getMessage(), terminal));
                out.flush();
                continue;
            }

            String yamlPreview;
            try {
                yamlPreview = previewMapper.writeValueAsString(built);
            } catch (Exception e) {
                PrintWriter pw = terminal.writer();
                pw.println(ConsoleColors.red("Cannot preview configuration: " + e.getMessage(), terminal));
                pw.flush();
                continue;
            }
            PrintWriter out = terminal.writer();
            out.println();
            out.println(ConsoleColors.cyan("── YAML Preview ──────────────────────────────────────", terminal));
            out.print(yamlPreview);
            out.println(ConsoleColors.cyan("──────────────────────────────────────────────────────", terminal));
            out.flush();

            if (!askWrite(terminal)) {
                continue;
            }

            Path outPath;
            try {
                outPath = writer.write(built, sessionOutputDir);
            } catch (Exception e) {
                out.println(ConsoleColors.red("Cannot write configuration: " + e.getMessage(), terminal));
                out.flush();
                continue;
            }
            out.println(ConsoleColors.green("✓ Written to: " + outPath, terminal));
            out.flush();

            if (!askContinue(terminal, "Create another? [Y / n to go back] ")) {
                return;
            }
        }
    }

    // ── Import flow ────────────────────────────────────────────────────────────────

    /**
     * Interactive import flow: prompt dir → scan → list files with confidence → select file →
     * confirm type → optional field edit → choose save destination.
     *
     * @param terminal         JLine3 terminal for I/O
     * @param sessionOutputDir session-level write directory used when the user picks save option 2
     * @throws Exception on terminal I/O error, YAML write failure, or user abort
     */
    private void runImport(final Terminal terminal, final Path sessionOutputDir) throws Exception {
        PrintWriter out = terminal.writer();

        out.print(ConsoleColors.cyan("Source directory: ", terminal));
        out.flush();
        String dirStr = TerminalIo.readLine(terminal);
        if (dirStr.isEmpty()) {
            return;
        }

        Path srcDir = Path.of(dirStr);
        if (!Files.isDirectory(srcDir)) {
            out.println(ConsoleColors.red("Not a directory: " + srcDir, terminal));
            out.flush();
            return;
        }

        // L2-11 (CWE-400): the console import enumerates and reads the same directories with
        // the same Files.list + read-string shape as the HTTP import scan, so the same
        // ImportScanLimits bounds apply — a directory with too many YAML entries, an
        // oversized file, or an over-budget total is refused with a stable message instead
        // of being enumerated and parsed unboundedly. Budget admission runs during
        // enumeration (before the sort buffers the result) and stops at the cap.
        final ImportScanLimits.Budget budget = new ImportScanLimits.Budget();
        final List<Path> yamlFiles;
        try (var stream = Files.list(srcDir)) {
            final List<Path> admitted = new ArrayList<>();
            for (final Iterator<Path> it = stream.iterator(); it.hasNext(); ) {
                final Path p = it.next();
                final String name = p.getFileName().toString().toLowerCase();
                if (!name.endsWith(".yaml") && !name.endsWith(".yml")) {
                    continue;
                }
                try {
                    budget.admit(p);
                } catch (final UncheckedIOException e) {
                    // Vanished between listing and admission — skip, not an attack.
                    continue;
                }
                admitted.add(p);
            }
            admitted.sort(Comparator.naturalOrder());
            yamlFiles = admitted;
        } catch (final ImportScanTooLargeException e) {
            out.println(ConsoleColors.red(e.getMessage(), terminal));
            out.flush();
            return;
        }

        if (yamlFiles.isEmpty()) {
            out.println(ConsoleColors.yellow("No YAML files found in " + srcDir, terminal));
            out.flush();
            return;
        }

        // List files with inferred type and confidence
        out.println();
        out.println(ConsoleColors.cyan("── YAML Files ───────────────────────────────────────", terminal));
        for (int i = 0; i < yamlFiles.size(); i++) {
            Path f = yamlFiles.get(i);
            List<TypeCandidateDto> candidates = engine.infer(f);
            String topMatch = candidates.isEmpty()
                    ? "(unknown)"
                    : candidates.get(0).simpleName()
                            + " (" + String.format("%.0f%%", candidates.get(0).confidence() * 100) + ")";
            out.println("  [" + (i + 1) + "] " + f.getFileName() + " — " + topMatch);
        }
        out.println("  [B] Back");
        out.println(ConsoleColors.cyan("─────────────────────────────────────────────────────", terminal));
        out.print("Select file: ");
        out.flush();

        String fileSel = TerminalIo.readLine(terminal);
        if ("B".equalsIgnoreCase(fileSel) || fileSel.isEmpty()) {
            return;
        }

        int fileIdx;
        try {
            fileIdx = Integer.parseInt(fileSel) - 1;
            if (fileIdx < 0 || fileIdx >= yamlFiles.size()) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            out.println(ConsoleColors.red("Invalid selection.", terminal));
            out.flush();
            return;
        }
        Path selectedFile = yamlFiles.get(fileIdx);

        // Confirm/select type
        String fqcn = selectType(terminal, selectedFile);
        if (fqcn == null) {
            return;
        }

        // Optional field edit
        Map<String, Object> fieldValues = optionalFieldEdit(terminal, fqcn);

        // Save options
        Path writtenPath = chooseSaveDestination(terminal, selectedFile, sessionOutputDir, fqcn, fieldValues);
        if (writtenPath == null) {
            return;
        }

        out.println(ConsoleColors.green("✓ Written to: " + writtenPath, terminal));
        out.flush();
    }

    /**
     * Shows type candidates for the selected file and lets the user confirm or override.
     *
     * @param terminal     JLine3 terminal for I/O
     * @param selectedFile the selected YAML file to infer types for
     * @return the chosen FQCN, or {@code null} when the user cancels
     * @throws IOException on terminal I/O failure
     */
    private String selectType(final Terminal terminal, final Path selectedFile) throws IOException {
        PrintWriter out = terminal.writer();
        List<TypeCandidateDto> candidates = engine.infer(selectedFile);

        if (candidates.isEmpty()) {
            out.print(ConsoleColors.cyan("No type inferred. Enter FQCN (or blank to cancel): ", terminal));
            out.flush();
            String fqcn = TerminalIo.readLine(terminal);
            return fqcn.isEmpty() ? null : fqcn;
        }

        out.println();
        out.println(ConsoleColors.cyan("── Type Candidates ──────────────────────────────────", terminal));
        for (int i = 0; i < candidates.size(); i++) {
            TypeCandidateDto c = candidates.get(i);
            out.println("  [" + (i + 1) + "] " + c.simpleName()
                    + " (" + String.format("%.0f%%", c.confidence() * 100) + ")");
        }
        out.println("  [C] Enter custom FQCN");
        out.println("  [B] Cancel");
        out.println(ConsoleColors.cyan("─────────────────────────────────────────────────────", terminal));
        out.print("Select type [1]: ");
        out.flush();

        String typeSel = TerminalIo.readLine(terminal);
        if ("B".equalsIgnoreCase(typeSel)) {
            return null;
        }
        if ("C".equalsIgnoreCase(typeSel)) {
            out.print("FQCN: ");
            out.flush();
            String fqcn = TerminalIo.readLine(terminal);
            return fqcn.isEmpty() ? null : fqcn;
        }

        int typeIdx = 0;
        if (!typeSel.isEmpty()) {
            try {
                typeIdx = Integer.parseInt(typeSel) - 1;
                if (typeIdx < 0 || typeIdx >= candidates.size()) {
                    typeIdx = 0;
                }
            } catch (NumberFormatException e) {
                typeIdx = 0;
            }
        }
        return candidates.get(typeIdx).fqcn();
    }

    /**
     * Offers to run the interactive field edit loop using {@link FieldPrompter}.
     *
     * @param terminal JLine3 terminal for I/O
     * @param fqcn     fully qualified class name to use for field introspection
     * @return field value map (possibly empty when user skips editing or class not found)
     * @throws Exception on terminal I/O failure
     */
    private Map<String, Object> optionalFieldEdit(final Terminal terminal, final String fqcn) throws Exception {
        PrintWriter out = terminal.writer();
        out.print(ConsoleColors.cyan("Edit fields? [y/N] ", terminal));
        out.flush();
        int editCh = TerminalIo.readChar(terminal);

        if (editCh != 'y' && editCh != 'Y') {
            return Collections.emptyMap();
        }

        ConfigClassInfo info = scanner.scan().stream()
                .filter(c -> c.fqcn().equals(fqcn) || c.simpleName().equals(fqcn))
                .findFirst()
                .orElse(null);
        if (info == null) {
            out.println(ConsoleColors.yellow("Config class not found on classpath: " + fqcn, terminal));
            out.flush();
            return Collections.emptyMap();
        }
        return prompter.prompt(info, terminal);
    }

    /**
     * Shows save-destination options and writes the file to the chosen path.
     *
     * <p>Options:
     * <ol>
     *   <li>Overwrite the source file in place</li>
     *   <li>Save to the session output directory (default)</li>
     *   <li>Enter a custom file path</li>
     * </ol>
     *
     * @param terminal         JLine3 terminal for I/O
     * @param selectedFile     original source file (used for option 1)
     * @param sessionOutputDir session-level output directory (used for option 2)
     * @param fqcn             fully qualified class name of the configuration type
     * @param fieldValues      field override map (may be empty)
     * @return the written file path, or {@code null} when the user cancels
     * @throws Exception on I/O failure
     */
    private Path chooseSaveDestination(final Terminal terminal,
                                       final Path selectedFile,
                                       final Path sessionOutputDir,
                                       final String fqcn,
                                       final Map<String, Object> fieldValues) throws Exception {
        PrintWriter out = terminal.writer();
        out.println();
        out.println(ConsoleColors.cyan("── Save As ──────────────────────────────────────────", terminal));
        out.println("  [1] Overwrite source file (" + selectedFile.getFileName() + ")");
        out.println("  [2] Save to session directory (default)");
        out.println("  [3] Enter custom path");
        out.println("  [B] Cancel");
        out.println(ConsoleColors.cyan("─────────────────────────────────────────────────────", terminal));
        out.print("Save option [2]: ");
        out.flush();

        String saveOpt = TerminalIo.readLine(terminal);

        if ("B".equalsIgnoreCase(saveOpt)) {
            return null;
        }
        if ("1".equals(saveOpt)) {
            return importService.write(fqcn, fieldValues, selectedFile.toString(),
                    selectedFile.toString(), null);
        }
        if ("3".equals(saveOpt)) {
            out.print("Custom path: ");
            out.flush();
            String customPath = TerminalIo.readLine(terminal);
            if (customPath.isEmpty()) {
                return null;
            }
            return importService.write(fqcn, fieldValues, selectedFile.toString(), customPath, null);
        }
        // Default: session directory (option 2 or blank)
        return importService.write(fqcn, fieldValues, selectedFile.toString(), null, sessionOutputDir);
    }

    // ── Terminal helpers ──────────────────────────────────────────────────────────

    /**
     * Prints the Configurations sub-menu.
     *
     * @param terminal JLine3 terminal for I/O
     */
    private void printSubMenu(final Terminal terminal) {
        PrintWriter out = terminal.writer();
        out.println();
        out.println(ConsoleColors.cyan("── Configurations ────────────────────────────────────", terminal));
        out.println("  [1] Create new YAML configuration");
        out.println("  [2] Import YAML configurations from directory");
        out.println("  [B] Back to main menu");
        out.println(ConsoleColors.cyan("─────────────────────────────────────────────────────", terminal));
        out.print("Choice: ");
        out.flush();
    }

    /**
     * Reads a single character from the terminal for menu navigation.
     *
     * @param terminal JLine3 terminal for I/O
     * @return the character pressed, or {@code (char) -1} on EOF
     * @throws IOException on terminal I/O failure
     */
    private char readSubMenuChar(final Terminal terminal) throws IOException {
        return (char) TerminalIo.readChar(terminal);
    }

    /**
     * Prompts the user to confirm writing the YAML preview to disk.
     *
     * <p>Enter, {@code y}, or {@code Y} → write; {@code n}, {@code N}, or EOF → skip.
     *
     * @param terminal the JLine3 terminal
     * @return {@code true} to write, {@code false} to go back without writing
     * @throws IOException on terminal I/O failure
     */
    private boolean askWrite(final Terminal terminal) throws IOException {
        terminal.writer().print(ConsoleColors.cyan("Write this config to file? [Y / n to go back] ", terminal));
        terminal.writer().flush();
        int ch = TerminalIo.readChar(terminal);
        return ch != 'n' && ch != 'N' && ch != -1;
    }

    /**
     * Prompts the user with a yes/no question (defaulting to yes).
     *
     * <p>Enter, {@code y}, or {@code Y} → {@code true}; anything else or EOF → {@code false}.
     *
     * @param terminal the JLine3 terminal
     * @param prompt   the question to display
     * @return {@code true} to continue, {@code false} to stop
     * @throws IOException on terminal I/O failure
     */
    private boolean askContinue(final Terminal terminal, final String prompt) throws IOException {
        terminal.writer().println();
        terminal.writer().print(ConsoleColors.cyan(prompt, terminal));
        terminal.writer().flush();
        int ch = TerminalIo.readChar(terminal);
        return ch != 'n' && ch != 'N' && ch != 'q' && ch != 'Q' && ch != -1;
    }

    // ── Inner types ───────────────────────────────────────────────────────────────

    /**
     * Factory for creating a {@link FilterMenu} bound to a specific terminal.
     *
     * <p>Separating menu construction from feature construction enables terminal injection
     * at runtime while the feature instance is created at startup.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @FunctionalInterface
    interface MenuFactory {

        /**
         * Creates a new {@link FilterMenu} backed by the given terminal.
         *
         * @param terminal the JLine3 terminal to use for input and output
         * @return a ready-to-use filter menu
         */
        FilterMenu create(Terminal terminal);
    }
}
