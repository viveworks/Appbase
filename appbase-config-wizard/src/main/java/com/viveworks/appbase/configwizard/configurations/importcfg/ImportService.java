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

package com.viveworks.appbase.configwizard.configurations.importcfg;

import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.scan.SupportedConfigClassRegistry;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Orchestrates writing imported YAML configurations with three path-resolution strategies:
 * explicit output path, session directory, or system temp.
 *
 * <p>Output path priority (first match wins):
 * <ol>
 *   <li>{@code outputPath} is non-null → write to that explicit path (parent dirs created).</li>
 *   <li>{@code sessionDir} is non-null → write a generated filename inside the session directory.</li>
 *   <li>All null → write to a system temp file with a generated name.</li>
 * </ol>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ImportService {

    private final FieldPrompter prompter;
    private final SupportedConfigClassRegistry supportedClasses;
    private final YamlWriter writer;

    /**
     * Constructs an {@code ImportService} with the given collaborators.
     *
     * @param scanner  discovers Appbase configuration classes on the classpath
     * @param prompter introspects and builds configuration objects from field-value maps
     * @param writer   writes serialised YAML to a target path
     */
    public ImportService(final ConfigScanner scanner,
                         final FieldPrompter prompter,
                         final YamlWriter writer) {
        this.prompter = prompter;
        this.supportedClasses = new SupportedConfigClassRegistry(scanner, prompter);
        this.writer = writer;
    }

    /**
     * Builds the configuration for {@code fqcn} from {@code fieldValues} and writes it
     * to the resolved output path.
     *
     * <p>Output path resolution (first match wins):
     * <ol>
     *   <li>{@code outputPath != null} → write to {@code Path.of(outputPath)}, creating
     *       parent directories as needed.</li>
     *   <li>{@code sessionDir != null} → write a generated filename inside
     *       {@code sessionDir} (directory is assumed to exist).</li>
     *   <li>Both null → write to a system temp file with a generated name prefixed
     *       {@code appbase-}.</li>
     * </ol>
     *
     * @param fqcn        fully qualified class name of the configuration to build
     * @param fieldValues map of Jackson property names to override values; empty map uses all defaults
     * @param sourceFile  path of the original source YAML file, used for informational purposes only;
     *                    may be {@code null}
     * @param outputPath  explicit target file path; when non-null takes priority over {@code sessionDir};
     *                    may be {@code null}
     * @param sessionDir  session-level output directory; when non-null and {@code outputPath} is null,
     *                    the file is written inside this directory; may be {@code null}
     * @return the path of the written YAML file
     * @throws IOException              when the file cannot be written
     * @throws IllegalArgumentException when {@code fqcn} does not identify a known config class
     */
    public Path write(final String fqcn,
                      final Map<String, Object> fieldValues,
                      @Nullable final String sourceFile,
                      @Nullable final String outputPath,
                      @Nullable final Path sessionDir) throws IOException {
        ConfigClassInfo info = findByFqcn(fqcn);
        Map<String, Object> values = fieldValues != null ? fieldValues : Collections.emptyMap();
        Object built = buildConfig(info, values);

        Path targetPath;
        if (outputPath != null) {
            targetPath = Path.of(outputPath);
        } else if (sessionDir != null) {
            // YamlWriter handles directory paths by generating a filename inside the directory
            targetPath = sessionDir;
        } else {
            targetPath = null;
        }
        return writer.write(built, targetPath);
    }

    /**
     * Builds a configuration object from the given field values, with resilient fallback
     * strategies for classes that cannot be constructed via Jackson deserialization.
     *
     * <p>Strategy (first success wins):
     * <ol>
     *   <li>Jackson {@code convertValue(fieldValues, configClass)} — the primary path.</li>
     *   <li>Use {@link ConfigClassInfo#defaultInstance()} when available — handles classes
     *       with builder patterns that Jackson cannot deserialize from an empty map.</li>
     *   <li>Return an empty {@link LinkedHashMap} — writes a minimal {@code {}} YAML template
     *       when neither Jackson nor a default instance is available. The file serves as a
     *       starting-point template for manual editing.</li>
     * </ol>
     *
     * @param info   the config class metadata
     * @param values the field override map (may be empty)
     * @return a non-null object suitable for YAML serialization
     */
    private Object buildConfig(final ConfigClassInfo info, final Map<String, Object> values) {
        try {
            return prompter.buildFromMap(info, values);
        } catch (IllegalArgumentException e) {
            // Jackson cannot construct the class from the given field map.
            // Fall back to the pre-built default instance when available.
            if (info.defaultInstance() != null) {
                return info.defaultInstance();
            }
            // No default instance — return an empty map that serialises to minimal YAML.
            return new LinkedHashMap<>();
        }
    }

    /**
     * Finds a {@link ConfigClassInfo} by exact FQCN or simple-name match within scanned
     * configurations and the nested types reachable from their advertised field metadata.
     *
     * @param fqcn fully qualified class name or simple name to look up
     * @return the matching config class metadata
     * @throws IllegalArgumentException when the name is absent from that bounded registry,
     *                                  even if an unrelated class with the name is loadable
     */
    private ConfigClassInfo findByFqcn(final String fqcn) {
        // The scanner's result is the ALLOWLIST, and it is the control -- not a hint. The previous
        // orElseGet(...loadClass(fqcn)) made a miss WIDEN instead of reject: an fqcn the scanner never
        // returned was loaded from the context class loader anyway and handed to scanner.infoFor(...),
        // so an unvalidated path parameter selected the class. orElseThrow keeps the allowlist
        // authoritative -- a name that is not in it is refused whether or not it happens to resolve.
        // (P-042 E01 N08, internal work item 1379 -- register row AUD-06, CWE-470. Same defect, second site: fixing one
        // of two is the internal work item 751 F-08 shape this epic keeps re-finding.)
        ConfigClassInfo info = supportedClasses.resolve(fqcn);
        if (info == null) {
            throw new IllegalArgumentException("Config class not found: " + fqcn);
        }
        return info;
    }
}
