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

package com.viveworks.appbase.configwizard.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Bounds the work one import scan may perform (Lens 2 / L2-11, CWE-400; OWASP A04:2021).
 *
 * <p>Import-scan confinement ({@link PathConfinement}) controls <em>where</em> a scan may read,
 * not <em>how much</em>: one request enumerates every YAML entry in a permitted directory and
 * reads each file fully before parsing it. A directory with a very large number of entries, one
 * huge file, or many mid-sized files could exhaust heap and CPU from a single loopback request.
 * This class enforces three independent scan-level bounds via a per-scan {@link Budget}, plus
 * the parser-level YAML expansion bounds the inference engine configures explicitly:
 * <ul>
 *   <li>{@link #MAX_SCAN_ENTRIES} — how many YAML entries one scan may enumerate;</li>
 *   <li>{@link #MAX_YAML_FILE_BYTES} — the size of any single admitted file;</li>
 *   <li>{@link #MAX_AGGREGATE_YAML_BYTES} — the combined size of all files admitted to one scan;</li>
 *   <li>{@link #MAX_YAML_NESTING_DEPTH} and {@link #MAX_YAML_ALIASES} — the YAML nesting depth
 *       and alias count the parser accepts (the billion-laughs / entity-expansion guard).</li>
 * </ul>
 *
 * <p>The same bounds apply to the interactive console import flow and the HTTP API.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ImportScanLimits {

    /**
     * Upper bound on YAML entries enumerated by one scan. Generously above any real
     * configuration directory; small enough that enumerating, sorting, and parsing the
     * admitted set is bounded work.
     */
    public static final int MAX_SCAN_ENTRIES = 500;

    /** Upper bound on the size of any single YAML file admitted by a scan (1 MiB). */
    public static final long MAX_YAML_FILE_BYTES = 1024L * 1024L;

    /** Upper bound on the combined size of all YAML files admitted to one scan (8 MiB). */
    public static final long MAX_AGGREGATE_YAML_BYTES = 8L * 1024L * 1024L;

    /**
     * Parser-level cap on YAML nesting depth. Real configuration files nest a few levels;
     * anything deeper is either hostile or unparseable by the wizard anyway.
     */
    public static final int MAX_YAML_NESTING_DEPTH = 50;

    /**
     * Parser-level cap on aliases to collection nodes — the direct bound on anchor/alias
     * expansion (each alias materialises a full copy of the anchored subgraph, so an uncapped
     * count turns a tiny file into an unbounded object graph).
     */
    public static final int MAX_YAML_ALIASES = 50;

    private ImportScanLimits() {
    }

    /**
     * Per-scan admission budget. One instance is created per scan (HTTP or console); every
     * enumerated YAML entry is admitted through {@link #admit(Path)} before any content is
     * read or parsed.
     */
    public static final class Budget {

        private int entries;
        private long aggregateBytes;

        /**
         * Admits one enumerated YAML file against the entry-count, per-file, and aggregate
         * bounds. The file size is inspected <em>without following symlinks</em> (the link's
         * own length, never the target's), so admitting a symlinked entry can neither be
         * tricked by a huge target nor hang on a cyclic one.
         *
         * @param yamlFile an enumerated {@code .yaml}/{@code .yml} entry
         * @throws ImportScanTooLargeException when any bound is exceeded
         * @throws UncheckedIOException        when the entry's attributes cannot be read
         */
        public void admit(final Path yamlFile) {
            final long size;
            try {
                size = Files.readAttributes(
                        yamlFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).size();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            entries++;
            if (entries > MAX_SCAN_ENTRIES
                    || size > MAX_YAML_FILE_BYTES
                    || aggregateBytes + size > MAX_AGGREGATE_YAML_BYTES) {
                throw new ImportScanTooLargeException();
            }
            aggregateBytes += size;
        }
    }
}
