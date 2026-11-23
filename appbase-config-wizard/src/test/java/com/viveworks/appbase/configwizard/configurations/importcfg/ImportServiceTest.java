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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ImportService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class ImportServiceTest {
    private static final String TEXT_VALUE_CONFIGURATION =
            "com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration";
    private ImportService service;
    @TempDir Path tempDir;

    @BeforeEach void setUp() {
        service = new ImportService(new ConfigScanner(),
            new FieldPrompter(new ObjectMapper()),
            new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none()));
    }

    @Test
    void write_withSourceFileAsOutputPath_overwritesOriginal() throws Exception {
        Path orig = tempDir.resolve("redis.yaml");
        Files.writeString(orig, "host: oldvalue\n");
        String fqcn = new ConfigScanner().scan().get(0).fqcn();
        Path result = service.write(fqcn, Map.of(), orig.toString(), orig.toString(), null);
        assertEquals(orig.toAbsolutePath(), result.toAbsolutePath());
        assertTrue(Files.exists(orig));
    }

    @Test
    void write_withNullPaths_writesToTemp() throws Exception {
        String fqcn = new ConfigScanner().scan().get(0).fqcn();
        Path result = service.write(fqcn, Map.of(), null, null, null);
        assertTrue(Files.exists(result));
        assertTrue(result.getFileName().toString().startsWith("appbase-"));
    }

    @Test
    void write_withSessionDir_writesInsideSessionDir() throws Exception {
        Path sessionDir = tempDir.resolve("session");
        Files.createDirectories(sessionDir);
        String fqcn = new ConfigScanner().scan().get(0).fqcn();
        Path result = service.write(fqcn, Map.of(), null, null, sessionDir);
        assertTrue(result.startsWith(sessionDir));
    }

    @Test
    void write_autoCreatesParentDirectories() throws Exception {
        Path deepPath = tempDir.resolve("a/b/c/out.yaml");
        String fqcn = new ConfigScanner().scan().get(0).fqcn();
        Path result = service.write(fqcn, Map.of(), null, deepPath.toString(), null);
        assertTrue(Files.exists(result));
    }

    /**
     * P-042 E01 N08 (internal work item 1379) -- register row AUD-06 (CWE-470), the SECOND site.
     *
     * {@link ImportService#write} reaches the same {@code findByFqcn} allowlist check that
     * {@code ConfigurationsApiResource} does, and it was fixed in the same commit -- but until this
     * arm existed the fix was UNPINNED here: reverting only this site reddened no test in the repo,
     * so the two halves of one fix had unequal protection.
     *
     * The input is deliberately a class that RESOLVES and is simply not a scanned config class. A
     * name that does not resolve cannot distinguish the implementations -- both reject it. Only a
     * loadable non-config class does, because that is exactly what the old
     * {@code orElseGet(...loadClass(fqcn))} fallback let through.
     */
    @Test
    void write_loadableNonConfigClass_isRejectedByTheAllowlist() {
        assertThrows(IllegalArgumentException.class,
                () -> service.write("java.lang.String", Map.of(), null,
                        tempDir.resolve("out.yaml").toString(), null),
                "the scanner allowlist must be the control, not a hint: a class that loads but was "
                        + "never scanned must be refused, not loaded and written");
    }

    @Test
    void write_advertisedNestedConfigClass_isAllowed() throws Exception {
        Path output = tempDir.resolve("nested.yaml");

        Path result = service.write(TEXT_VALUE_CONFIGURATION, Map.of("text", "nested-key"),
                null, output.toString(), null);

        assertEquals(output.toAbsolutePath(), result.toAbsolutePath());
        assertTrue(Files.readString(result).contains("nested-key"));
    }
}
