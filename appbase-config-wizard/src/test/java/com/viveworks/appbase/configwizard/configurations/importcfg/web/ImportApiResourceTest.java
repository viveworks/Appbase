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

package com.viveworks.appbase.configwizard.configurations.importcfg.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.ImportScanResultDto;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.ImportWriteRequestDto;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.web.dto.WriteResultDto;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.security.ImportScanLimits;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ImportApiResource} exercising resource methods directly
 * without an HTTP server.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
final class ImportApiResourceTest {
    private ImportApiResource resource;
    @TempDir Path tempDir;

    @BeforeEach void setUp() {
        resource = new ImportApiResource(new ConfigScanner(),
            new FieldPrompter(new ObjectMapper()), new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none()),
            PathConfinement.none());
    }

    /**
     * Builds a resource confined to {@code root}, with its writer confined to the same root.
     *
     * @param root the only tree the resource may read or write
     * @return a confined resource
     */
    private static ImportApiResource confinedTo(final Path root) {
        return new ImportApiResource(new ConfigScanner(),
                new FieldPrompter(new ObjectMapper()),
                new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root)),
                PathConfinement.of(root));
    }

    @Test
    void scan_directoryOutsideRoot_returnsForbidden() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.writeString(outside.resolve("secret.yaml"), "token: hunter2\n");

        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> confinedTo(root).scan(outside.toString()));
        assertEquals(403, thrown.getResponse().getStatus());
    }

    @Test
    void write_outputPathOutsideRoot_returnsForbiddenAndWritesNothing() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root2"));
        Path outside = Files.createDirectory(tempDir.resolve("outside2"));
        Path target = outside.resolve("authorized_keys");
        String fqcn = new ConfigScanner().scan().get(0).fqcn();

        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> confinedTo(root).write(fqcn,
                        new ImportWriteRequestDto(null, Map.of(), target.toString())));
        assertEquals(403, thrown.getResponse().getStatus());
        assertFalse(Files.exists(target), "A rejected write must not create its target");
    }

    @Test
    void write_forbiddenResponse_disclosesNeitherRootNorRequestedPath() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root3"));
        Path target = tempDir.resolve("outside3").resolve("x.yaml");
        String fqcn = new ConfigScanner().scan().get(0).fqcn();

        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> confinedTo(root).write(fqcn,
                        new ImportWriteRequestDto(null, Map.of(), target.toString())));
        String body = String.valueOf(thrown.getResponse().getEntity());
        assertFalse(body.contains(root.toString()), "must not disclose the configured root");
        assertFalse(body.contains(target.toString()), "must not echo the requested path");
    }

    @Test
    void scan_emptyDirectory_returnsEmptyFiles() {
        ImportScanResultDto result = resource.scan(tempDir.toString());
        assertNotNull(result);
        assertTrue(result.files().isEmpty(), "Empty dir has no YAML files");
    }

    @Test
    void scan_directoryWithYaml_returnsFileEntries() throws Exception {
        Files.writeString(tempDir.resolve("redis.yaml"), "host: localhost\n");
        ImportScanResultDto result = resource.scan(tempDir.toString());
        assertEquals(1, result.files().size());
        assertEquals("redis.yaml", result.files().get(0).fileName());
        assertFalse(result.files().get(0).candidates().isEmpty());
    }

    /**
     * L4-07 / AUD-07-adjacent (CWE-59, CWE-200): {@code scan()} confines only the directory, then
     * follows a {@code .yaml}/{@code .yml} symlink entry when inferring its type candidates —
     * {@link com.viveworks.appbase.configwizard.configurations.importcfg.TypeInferenceEngine#infer}
     * calls {@code Files.readString}, which follows symlinks. A symlink planted inside the
     * confined root can therefore cause a read of content outside the root.
     */
    @Test
    void scan_yamlSymlinkInsideRoot_doesNotFollowItOutsideRoot() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root4"));
        Path outside = Files.createDirectory(tempDir.resolve("outside4"));
        Path secret = outside.resolve("secret.yaml");
        Files.writeString(secret, "sentinelOutsideField: sentinel-aud-l407-value\n");
        Path legitimate = root.resolve("redis.yaml");
        Files.writeString(legitimate, "host: localhost\n");
        Path link = root.resolve("leak.yaml");
        Files.createSymbolicLink(link, secret);

        ImportScanResultDto result = confinedTo(root).scan(root.toString());

        assertEquals(1, result.files().size(), "the symlink entry must not be listed");
        assertEquals("redis.yaml", result.files().get(0).fileName());
    }

    @Test
    void scan_confined_pathIsRootRelative_notAbsolute() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path env = Files.createDirectory(root.resolve("env"));
        Files.writeString(env.resolve("app.yaml"), "host: localhost\n");

        // scan is Files.list (non-recursive), so scan the subdirectory; toDisplayPath still redacts
        // the entry relative to the confinement ROOT, so the response carries "env/app.yaml", never
        // the absolute server layout that the old toAbsolutePath() disclosed (A10-01, internal work item 1513).
        ImportScanResultDto result = confinedTo(root).scan(env.toString());

        assertEquals(1, result.files().size());
        String path = result.files().get(0).path();
        assertEquals("env/app.yaml", path);
        assertFalse(path.contains(root.toString()), "scan must not disclose the absolute layout");
    }

    @Test
    void scan_entryWithUnresolvableSymlink_stillReturnsSiblings_noThrow() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root-symlink"));
        Files.writeString(root.resolve("good.yaml"), "host: localhost\n");
        // A symlink cycle cannot be canonicalised, so toDisplayPath throws PathOutsideRootException.
        // toFileEntry must swallow it per entry (falling back to the file name, which never leaks the
        // layout) and let the scan of the readable siblings continue — the resilience contract that
        // the redaction change (internal work item 1513) would otherwise break, since scan() catches only IOException.
        Files.createSymbolicLink(root.resolve("loop.yaml"), root.resolve("loop2.yaml"));
        Files.createSymbolicLink(root.resolve("loop2.yaml"), root.resolve("loop.yaml"));

        ImportScanResultDto result =
                assertDoesNotThrow(() -> confinedTo(root).scan(root.toString()));

        assertTrue(result.files().stream().anyMatch(f -> "good.yaml".equals(f.path())),
                "a readable sibling must survive an unresolvable-symlink entry");
        assertTrue(result.files().stream().noneMatch(f -> f.path().contains(root.toString())),
                "no entry may disclose the absolute layout, even on the fallback path");
    }

    @Test
    void scan_nonExistentDirectory_returnsEmptyOrBadRequest() {
        // Should not throw unchecked — return empty or error gracefully
        assertDoesNotThrow(() -> resource.scan("/no/such/dir"));
    }

    /**
     * L4-02 (CWE-400): {@code POST /api/configurations/import/{fqcn}/write} must reject a
     * {@code fieldValues} map carrying more entries than {@link
     * com.viveworks.appbase.configwizard.security.FieldValuesLimits#MAX_FIELD_COUNT} before it
     * ever reaches {@code ImportService#write} / {@code FieldPrompter#buildFromMap} — with a
     * disclosure-free error message, not a thrown exception, and without writing a file.
     */
    @Test
    void write_rejectsTooManyOrOversizeFieldValues() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root5"));
        String fqcn = new ConfigScanner().scan().get(0).fqcn();
        Map<String, Object> tooMany = new java.util.HashMap<>();
        for (int i = 0; i <
                com.viveworks.appbase.configwizard.security.FieldValuesLimits.MAX_FIELD_COUNT + 1;
                i++) {
            tooMany.put("field" + i, "value");
        }
        Path target = root.resolve("rejected.yaml");

        WriteResultDto result = confinedTo(root).write(
                fqcn, new ImportWriteRequestDto(null, tooMany, target.toString()));

        assertNotNull(result.errorMessage());
        assertFalse(Files.exists(target), "an oversize request must not be written");
    }

    /**
     * L2-11 (CWE-400): one import-scan request must not perform unbounded work inside the
     * permitted root. Path confinement controls <em>where</em> the scan reads, not
     * <em>how much</em>: pre-fix, the scan enumerates every YAML entry, sorts them all into
     * memory, and reads each file fully. A directory holding more than
     * {@link ImportScanLimits#MAX_SCAN_ENTRIES} YAML files, or files whose combined size
     * exceeds {@link ImportScanLimits#MAX_AGGREGATE_YAML_BYTES}, must instead produce a
     * stable 413 refusal that discloses neither the scanned root nor any count.
     */
    @Test
    void scan_rejectsFileCountAndAggregateYamlByteLimit() throws Exception {
        // Arm 1 — file count: one entry over the cap.
        Path countRoot = Files.createDirectory(tempDir.resolve("l211-count"));
        for (int i = 0; i <= ImportScanLimits.MAX_SCAN_ENTRIES; i++) {
            Files.writeString(countRoot.resolve("f" + i + ".yaml"), "host: localhost\n");
        }
        WebApplicationException byCount = assertThrows(WebApplicationException.class,
                () -> confinedTo(countRoot).scan(countRoot.toString()));
        assertEquals(413, byCount.getResponse().getStatus());
        assertFalse(String.valueOf(byCount.getResponse().getEntity()).contains(countRoot.toString()),
                "the refusal must not disclose the scanned root");

        // Arm 2 — aggregate bytes: every file within the per-file bound, together over the cap.
        Path aggregateRoot = Files.createDirectory(tempDir.resolve("l211-aggregate"));
        final byte[] oneMiB = new byte[(int) ImportScanLimits.MAX_YAML_FILE_BYTES];
        final int filesNeeded = (int) (ImportScanLimits.MAX_AGGREGATE_YAML_BYTES
                / ImportScanLimits.MAX_YAML_FILE_BYTES) + 1;
        for (int i = 0; i < filesNeeded; i++) {
            Files.write(aggregateRoot.resolve("m" + i + ".yaml"), oneMiB);
        }
        WebApplicationException byAggregate = assertThrows(WebApplicationException.class,
                () -> confinedTo(aggregateRoot).scan(aggregateRoot.toString()));
        assertEquals(413, byAggregate.getResponse().getStatus());
    }

    /**
     * L2-11 (CWE-400): a single YAML file larger than
     * {@link ImportScanLimits#MAX_YAML_FILE_BYTES} must be refused before its content is
     * materialized — pre-fix the scan reads the whole file into a String and hands it to the
     * parser regardless of size.
     */
    @Test
    void scan_rejectsPerFileYamlByteLimit() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("l211-perfile"));
        Files.write(root.resolve("big.yaml"), new byte[(int) ImportScanLimits.MAX_YAML_FILE_BYTES + 1]);

        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> confinedTo(root).scan(root.toString()));

        assertEquals(413, thrown.getResponse().getStatus());
        assertNotNull(thrown.getResponse().getEntity());
    }

    @Test
    void write_validRequest_returnsPath() throws Exception {
        Path orig = tempDir.resolve("redis.yaml");
        Files.writeString(orig, "host: localhost\n");
        String fqcn = new ConfigScanner().scan().get(0).fqcn();
        ImportWriteRequestDto req = new ImportWriteRequestDto(
            orig.toString(), Map.of(), null);
        WriteResultDto result = resource.write(fqcn, req);
        assertNotNull(result);
        assertNotNull(result.path(), "Should return written path");
    }

    /**
     * AUD-09 (CWE-209/497): a write failure must not echo the raw exception/type/message back
     * to the client. {@link #write_validRequest_returnsPath} already pins the success path;
     * this pins that a failure on the same path discloses nothing but an opaque correlation id,
     * while the full detail (sentinel path/type/field, original throwable, stack trace) is
     * retained server-side under that id.
     */
    @Test
    @org.junit.jupiter.api.parallel.ResourceLock("ImportApiResource.logger")
    void write_errorDoesNotExposeRawExceptionAndLogsCorrelationId() throws Exception {
        final String sentinelPath = "/private/aud09-import/secrets.yaml";
        final String sentinelType = "com.example.Aud09ImportSecretType";
        final String sentinelField = "importSecretPropertyName";
        final java.io.IOException failure = new java.io.IOException(
                sentinelPath + " " + sentinelType + " " + sentinelField);
        final ObjectMapper throwingWriteMapper = new ObjectMapper() {
            @Override
            public void writeValue(final java.io.OutputStream out, final Object value) throws java.io.IOException {
                throw failure;
            }
        };
        final ImportApiResource failing = new ImportApiResource(
                new ConfigScanner(),
                new FieldPrompter(new ObjectMapper()),
                new YamlWriter(throwingWriteMapper, PathConfinement.none()),
                PathConfinement.none());
        final String fqcn = new ConfigScanner().scan().get(0).fqcn();
        final Path target = tempDir.resolve("out.yaml");

        final java.util.logging.Logger logger =
                java.util.logging.Logger.getLogger(ImportApiResource.class.getName());
        final java.util.logging.Level previousLevel = logger.getLevel();
        final java.util.List<java.util.logging.LogRecord> records = new java.util.ArrayList<>();
        final java.util.logging.Handler handler = new java.util.logging.Handler() {
            @Override
            public void publish(final java.util.logging.LogRecord record) {
                if (record.getLevel().intValue() >= java.util.logging.Level.SEVERE.intValue()) {
                    records.add(record);
                }
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        handler.setLevel(java.util.logging.Level.ALL);
        logger.addHandler(handler);
        logger.setLevel(java.util.logging.Level.ALL);
        final WriteResultDto result;
        try {
            result = failing.write(fqcn,
                    new ImportWriteRequestDto(null, Map.of(), target.toString()));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            handler.close();
        }

        assertNotNull(result.errorMessage());
        final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "Import request failed; reference=([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-"
                        + "[89ab][0-9a-f]{3}-[0-9a-f]{12})").matcher(result.errorMessage());
        assertTrue(matcher.matches(), "client error must be only the stable generic text and opaque id");
        assertFalse(result.errorMessage().contains(sentinelPath));
        assertFalse(result.errorMessage().contains(sentinelType));
        assertFalse(result.errorMessage().contains(sentinelField));
        assertFalse(result.errorMessage().contains("IOException"));

        final String correlationId = matcher.group(1);
        assertEquals(1, records.size(), "one server error event per failed request");
        final java.util.logging.LogRecord record = records.get(0);
        assertTrue(record.getMessage().contains(correlationId));
        assertNotNull(record.getThrown(), "server event must retain the caught throwable/stack trace");
        assertEquals(failure, record.getThrown(), "server event must retain the original throwable");
        assertTrue(record.getThrown().getMessage().contains(sentinelPath));
        assertTrue(record.getThrown().getMessage().contains(sentinelType));
        assertTrue(record.getThrown().getMessage().contains(sentinelField));
    }
}
