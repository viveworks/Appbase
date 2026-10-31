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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.ConfigurationsConsoleFeature;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import com.viveworks.appbase.configwizard.console.ConfigWizardConsoleApp;
import com.viveworks.appbase.configwizard.console.ConsoleFeature;
import com.viveworks.appbase.configwizard.console.TerminalIo;
import com.viveworks.appbase.configwizard.menu.UserAbortException;
import com.viveworks.appbase.configwizard.web.ConfigWizardWebServer;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Picocli entry point for {@code appbase-config-wizard}.
 *
 * <p>Two sub-commands are registered:
 * <ul>
 *   <li>{@code web} ({@link WebCommand}) — starts an embedded Jetty web UI</li>
 *   <li>{@code console} ({@link ConsoleCommand}) — runs the interactive console (default)</li>
 * </ul>
 *
 * <p>When invoked with no sub-command the console is started automatically.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Command(
        name = "appbase-config-wizard",
        subcommands = {ConfigWizardApp.WebCommand.class, ConfigWizardApp.ConsoleCommand.class},
        mixinStandardHelpOptions = true,
        versionProvider = ConfigWizardApp.VersionProvider.class,
        description = "Interactively build YAML configuration files for any Appbase service."
)
public final class ConfigWizardApp implements Callable<Integer> {

    /**
     * When no sub-command is given, runs the interactive console by default.
     *
     * @return exit code (always {@code 0}; console errors propagate as exceptions)
     */
    @Override
    public Integer call() {
        new CommandLine(this).execute("console");
        return 0;
    }

    /**
     * Application entry point — delegates to Picocli and exits with the returned code.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        System.exit(new CommandLine(new ConfigWizardApp()).execute(args));
    }

    // ── Sub-commands ──────────────────────────────────────────────────────────────

    /**
     * {@code web} sub-command: starts the embedded Jetty web UI.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Command(
            name = "web",
            mixinStandardHelpOptions = true,
            description = "Start embedded web UI"
    )
    static final class WebCommand implements Callable<Integer> {

        @Option(names = {"--port", "--web-port"}, defaultValue = "8080",
                description = "Port for web UI (default: ${DEFAULT-VALUE})")
        private int port;

        @Option(names = {"--root"},
                description = "Directory the API may read and write within (default: the current "
                        + "working directory). Paths outside it are refused with 403.")
        private Path root = Path.of(System.getProperty("user.dir"));

        private final WebServerFactory factory;

        /**
         * Exposes the resolved root for testing.
         *
         * @return the directory the API is confined to
         */
        Path getRoot() {
            return root;
        }

        /**
         * Production constructor — uses the default {@link ConfigWizardWebServer}.
         */
        WebCommand() {
            this(ConfigWizardWebServer::new);
        }

        /**
         * Package-private testing constructor — factory injected for unit tests.
         *
         * @param factory factory that creates a web server on the requested port
         */
        WebCommand(final WebServerFactory factory) {
            this.factory = factory;
        }

        @Override
        public Integer call() {
            ConfigWizardWebServer server = factory.create(port, root);
            System.out.println("Starting appbase-config-wizard web UI (preferred port " + port + ")");
            try {
                int actualPort = server.bind();
                System.out.println("Open: http://localhost:" + actualPort);
                System.out.flush();
                server.join();
            } catch (Exception e) {
                throw new RuntimeException("Web server failed: " + e.getMessage(), e);
            }
            return 0;
        }
    }

    /**
     * {@code console} sub-command: runs the interactive console.
     *
     * <p>Resolves the session output directory (from {@code --output} / {@code --output-dir}
     * or an interactive prompt), then launches {@link ConfigWizardConsoleApp} with a
     * {@link ConfigurationsConsoleFeature} registered.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Command(
            name = "console",
            mixinStandardHelpOptions = true,
            description = "Interactive console (default)"
    )
    static final class ConsoleCommand implements Callable<Integer> {

        @Option(names = {"-o", "--output"},
                description = "YAML output file path (default: $TMPDIR/appbase-<Class>-<ts>.yaml)")
        @Nullable
        private Path output;

        @Option(names = {"--output-dir"},
                description = "Directory for all YAML output files this session "
                        + "(skips the startup prompt)")
        @Nullable
        private Path outputDir;

        private final ConfigScanner scanner;
        private final FieldPrompter prompter;
        private final YamlWriter writer;
        private final TerminalFactory terminalFactory;

        /**
         * Production constructor — creates all dependencies with their default configurations.
         */
        ConsoleCommand() {
            this(new ConfigScanner(),
                    new FieldPrompter(new ObjectMapper()
                            .registerModule(new JavaTimeModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)),
                    new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none()),
                    () -> TerminalBuilder.builder().system(true).build());
        }

        /**
         * Package-private testing constructor — all dependencies injected.
         *
         * @param scanner         config-class scanner
         * @param prompter        interactive field prompter
         * @param writer          YAML serialiser
         * @param terminalFactory factory that obtains the JLine3 terminal
         */
        ConsoleCommand(final ConfigScanner scanner,
                       final FieldPrompter prompter,
                       final YamlWriter writer,
                       final TerminalFactory terminalFactory) {
            this.scanner = scanner;
            this.prompter = prompter;
            this.writer = writer;
            this.terminalFactory = terminalFactory;
        }

        @Override
        public Integer call() throws Exception {
            try (Terminal terminal = terminalFactory.create()) {
                Path sessionDir = resolveSessionPath(terminal);
                List<ConsoleFeature> features = List.of(
                        new ConfigurationsConsoleFeature(scanner, prompter, writer)
                );
                try {
                    new ConfigWizardConsoleApp(features, terminal, sessionDir).run();
                } catch (UserAbortException e) {
                    terminal.writer().println(ConsoleColors.yellow("Goodbye.", terminal));
                    terminal.writer().flush();
                }
            }
            return 0;
        }

        /**
         * Determines the session-level write path.
         *
         * <p>Priority:
         * <ol>
         *   <li>{@code --output} flag — specific file path</li>
         *   <li>{@code --output-dir} flag — directory</li>
         *   <li>Interactive prompt — user types a path, or presses Enter for
         *       {@code java.io.tmpdir}</li>
         * </ol>
         *
         * @param terminal JLine3 terminal for I/O
         * @return path to use for every write in this session
         * @throws IOException on terminal read failure
         */
        private Path resolveSessionPath(final Terminal terminal) throws IOException {
            if (output != null) {
                return output;
            }
            if (outputDir != null) {
                return outputDir;
            }
            String defaultDir = System.getProperty("java.io.tmpdir");
            terminal.writer().print(ConsoleColors.cyan("Output directory [" + defaultDir + "]: ", terminal));
            terminal.writer().flush();
            String line = TerminalIo.readLine(terminal);
            return line.isEmpty() ? Path.of(defaultDir) : Path.of(line);
        }

    }

    // ── Inner types ───────────────────────────────────────────────────────────────

    /**
     * Reads the tool version from the JAR {@code MANIFEST.MF}'s
     * {@code Implementation-Version} entry.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    static final class VersionProvider implements IVersionProvider {

        /**
         * Returns the version string derived from the package manifest.
         *
         * @return single-element array with the version, or {@code ["unknown"]} when not
         *         running from a JAR (e.g. during tests)
         */
        @Override
        public String[] getVersion() {
            String v = ConfigWizardApp.class.getPackage().getImplementationVersion();
            return new String[]{v != null ? v : "unknown"};
        }
    }

    /**
     * Factory for obtaining a JLine3 {@link Terminal}, permitting checked I/O exceptions.
     *
     * <p>Separating terminal construction from command construction enables full
     * test-time control over the terminal without relying on a real system terminal.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface TerminalFactory {

        /**
         * Creates or obtains the JLine3 terminal.
         *
         * @return a ready terminal
         * @throws IOException when the system terminal cannot be opened
         */
        Terminal create() throws IOException;
    }

    /**
     * Factory for creating a {@link ConfigWizardWebServer} on the specified port.
     *
     * <p>Separating server construction enables test-time control over the web server
     * without starting a real Jetty instance.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface WebServerFactory {

        /**
         * Creates a {@link ConfigWizardWebServer} listening on the given port.
         *
         * @param port the TCP port; use {@code 0} for an OS-assigned ephemeral port
         * @param root the only directory tree the API may read from or write to
         * @return a ready-to-start web server
         */
        ConfigWizardWebServer create(int port, Path root);
    }
}
