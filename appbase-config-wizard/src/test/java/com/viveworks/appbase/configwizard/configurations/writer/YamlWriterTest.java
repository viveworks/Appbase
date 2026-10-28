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

package com.viveworks.appbase.configwizard.configurations.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import com.viveworks.appbase.configwizard.security.PathOutsideRootException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for {@link YamlWriter}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class YamlWriterTest {

    @Test
    void write_shippedPropertylessDefaults_buildsAndProducesParseableYaml(
            @TempDir final Path tmp) throws Exception {
        var scanner = new ConfigScanner();
        var prompter = new FieldPrompter(new ObjectMapper());
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none());

        for (String simpleName : List.of(
                "DefaultSupportMessageServiceConfiguration",
                "DefaultUserAccountServiceConfiguration")) {
            var info = scanner.scan().stream()
                    .filter(candidate -> simpleName.equals(candidate.simpleName()))
                    .findFirst().orElseThrow();
            assertTrue(prompter.introspect(info).isEmpty(),
                    simpleName + " must remain a propertyless marker configuration");

            Object built = prompter.buildFromMap(info, Map.of());
            Path written = writer.write(built, tmp.resolve(simpleName + ".yaml"));

            assertTrue(Files.exists(written), simpleName + " YAML was not written");
            var parsed = YamlUtil.getYamlMapper().readTree(Files.readString(written));
            assertTrue(parsed.isObject(), simpleName + " YAML must parse as an object");
            assertTrue(parsed.isEmpty(), simpleName + " YAML must preserve empty-object semantics");
        }
    }

    @Test
    void write_toTempFile_producesReadableYaml(@TempDir final Path tmp) throws Exception {
        var scanner = new ConfigScanner();
        var prompter = new FieldPrompter(new ObjectMapper());
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none());

        var info = scanner.scan().stream()
                .filter(c -> c.simpleName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();

        var fieldValues = Map.<String, Object>of("ssl", false);
        var built = prompter.buildFromMap(info, fieldValues);

        var written = writer.write(built, tmp.resolve("out.yaml"));

        assertTrue(Files.exists(written));
        var content = Files.readString(written);
        assertTrue(content.contains("ssl:"), "Expected 'ssl:' in:\n" + content);
    }

    @Test
    void write_nullOutputPath_createsTempFile() throws Exception {
        var scanner = new ConfigScanner();
        var prompter = new FieldPrompter(new ObjectMapper());
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none());

        var info = scanner.scan().stream()
                .filter(c -> c.simpleName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();

        var built = prompter.buildFromMap(info, Map.of("ssl", false));
        Path result = writer.write(built, null);

        assertNotNull(result);
        assertTrue(result.getFileName().toString().startsWith("appbase-"),
                "Temp file name should start with 'appbase-'");
        assertTrue(result.getFileName().toString().endsWith(".yaml"),
                "Temp file name should end with '.yaml'");
        assertTrue(Files.exists(result), "Temp file should exist after write");
        Files.deleteIfExists(result);
    }

    @Test
    void write_directoryPath_createsFileInsideDirectory(@TempDir final Path dir) throws Exception {
        var scanner = new ConfigScanner();
        var prompter = new FieldPrompter(new ObjectMapper());
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none());

        var info = scanner.scan().stream()
                .filter(c -> c.simpleName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();

        var built = prompter.buildFromMap(info, Map.of());
        // Pass an existing directory — writer must auto-generate a file name inside it
        Path result = writer.write(built, dir);

        assertNotNull(result);
        assertEquals(dir, result.getParent(), "Written file must be inside the supplied directory");
        assertTrue(result.getFileName().toString().startsWith("appbase-"),
                "Auto-generated name should start with 'appbase-'");
        assertTrue(result.getFileName().toString().endsWith(".yaml"),
                "Auto-generated name should end with '.yaml'");
        assertTrue(Files.exists(result), "File must exist after write");
    }

    @Test
    void write_outputPathOutsideRoot_throwsPathOutsideRootException(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));
        Path target = outside.resolve("stolen.yaml");

        assertThrows(PathOutsideRootException.class,
                () -> writer.write(Map.of("host", "localhost"), target));
        assertFalse(Files.exists(target), "A rejected write must not create its target");
    }

    @Test
    void write_outputPathInsideRoot_succeeds(@TempDir final Path tmp) throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));

        Path written = writer.write(Map.of("host", "localhost"), root.resolve("ok.yaml"));

        assertTrue(Files.exists(written));
        assertTrue(written.startsWith(root.toRealPath()));
    }

    @Test
    void write_nullOutputPathUnderConfinement_staysInsideRoot(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));

        Path written = writer.write(Map.of("host", "localhost"), null);

        assertTrue(written.startsWith(root.toRealPath()),
                "Under confinement the temp-file fallback must land in the root, not java.io.tmpdir");
    }

    @Test
    void write_unconfined_stillWritesAnywhere_consoleRegression(@TempDir final Path tmp)
            throws Exception {
        Path anywhere = Files.createDirectory(tmp.resolve("operator-chosen"));
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none());

        Path written = writer.write(Map.of("host", "localhost"), anywhere.resolve("out.yaml"));

        assertTrue(Files.exists(written),
                "AC7 — the console operator types their own output directory and must not be confined");
    }

    @Test
    void write_unconfined_preservesConsoleLeafSymlinkCompatibility(@TempDir final Path tmp)
            throws Exception {
        Path destination = tmp.resolve("destination.yaml");
        Path link = tmp.resolve("operator-selected.yaml");
        Files.createSymbolicLink(link, destination);
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none());

        writer.write(Map.of("host", "localhost"), link);

        assertTrue(Files.readString(destination).contains("localhost"));
    }

    @Test
    void write_existingLongerDocument_truncatesOldContent(@TempDir final Path tmp)
            throws Exception {
        Path target = tmp.resolve("out.yaml");
        Files.writeString(target, "obsolete: this trailing content must disappear\n");
        var mapper = YamlUtil.getYamlMapper();
        var config = Map.of("a", "b");
        var writer = new YamlWriter(mapper, PathConfinement.none());

        writer.write(config, target);

        assertEquals(mapper.writeValueAsString(config), Files.readString(target));
    }

    @Test
    void write_confinedMissingParent_isRejectedWithoutOutsideSideEffect(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path target = root.resolve("missing/child/out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));
        assertFalse(Files.exists(outside.resolve("child")),
                "A missing confined parent must not create directories outside the root");
    }

    @Test
    void write_ancestorSwappedAfterSecureTraversal_doesNotWriteOutside(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path ancestor = Files.createDirectory(root.resolve("ancestor"));
        Path parked = root.resolve("parked");
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path target = ancestor.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root), ignored -> {
            Files.move(ancestor, parked);
            Files.createSymbolicLink(ancestor, outside);
        });

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));

        assertFalse(Files.exists(outside.resolve("out.yaml")),
                "Replacing a traversed ancestor must not redirect the descriptor-relative write");
        assertFalse(Files.exists(parked.resolve("out.yaml")),
                "A retained directory that no longer sits at its confined path must fail closed");
    }

    @Test
    void test_AC812_3_existingHardLinkedTargetIsRejectedAndSharedInodeUntouched(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = tmp.resolve("outside.yaml");
        Files.writeString(outside, "sentinel");
        Path target = root.resolve("out.yaml");
        Files.createLink(target, outside);
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));
        assertEquals("sentinel", Files.readString(outside),
                "A confined write must not truncate an inode that has another hard link");
    }

    @Test
    void EXTRA_write_confinedTargetReplacedByHardLinkAfterCheck_isRejected(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = tmp.resolve("outside.yaml");
        Files.writeString(outside, "sentinel");
        Path target = root.resolve("out.yaml");
        Files.writeString(target, "original");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root), confined -> {
            Files.delete(confined);
            Files.createLink(confined, outside);
        });

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));
        assertEquals("sentinel", Files.readString(outside),
                "An inode swapped in after the link-count check must not be truncated");
    }

    @Test
    void test_AC812_4_preplantedFifoTargetFailsFastAndIsNeverOpened(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path target = root.resolve("out.yaml");
        makeFifo(target);
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));

        try {
            IOException rejected = assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThrows(IOException.class,
                            () -> writer.write(Map.of("host", "localhost"), target)));
            assertTrue(rejected.getMessage().contains("not a regular file"),
                    "The special file must be rejected by the type check up front: "
                            + rejected.getMessage());
        } finally {
            releaseBlockedFifoWriter(target);
        }
        assertFalse(Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS),
                "A rejected special file must not be replaced by a regular file");
    }

    @Test
    void EXTRA_write_fifoRacedInAfterCheck_failsFastAndIsNeverOpened(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path target = root.resolve("out.yaml");
        Files.writeString(target, "original");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root), confined -> {
            Files.delete(confined);
            makeFifo(confined);
        });

        try {
            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThrows(IOException.class,
                            () -> writer.write(Map.of("host", "localhost"), target)));
        } finally {
            releaseBlockedFifoWriter(target);
        }
    }

    @Test
    void EXTRA_write_confinedOverwriteLeavesNoTemporarySibling(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path target = root.resolve("out.yaml");
        Files.writeString(target, "old");

        new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root))
                .write(Map.of("a", "b"), target);

        try (var names = Files.list(root)) {
            assertEquals(java.util.List.of("out.yaml"),
                    names.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void EXTRA_write_confinedOverwritePreservesPermissionBits(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        // Modes with group/other bits: an ordinary umask (022/077) narrows these at creation, so
        // only an explicit set on the replacement can preserve them (IR2529-06). 0600 alone would
        // pass vacuously.
        for (String text : new String[] {"rw-rw----", "rw-rw-r--", "rw-r--r--", "rw-------"}) {
            Path target = root.resolve("out-" + text + ".yaml");
            Files.writeString(target, "old");
            java.util.Set<java.nio.file.attribute.PosixFilePermission> mode =
                    java.nio.file.attribute.PosixFilePermissions.fromString(text);
            try {
                Files.setPosixFilePermissions(target, mode);
            } catch (UnsupportedOperationException notPosix) {
                assumeTrue(false, "POSIX permissions are not supported on this platform");
            }

            new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root))
                    .write(Map.of("a", "b"), target);

            assertEquals(mode, Files.getPosixFilePermissions(target, LinkOption.NOFOLLOW_LINKS),
                    "mode " + text);
        }
    }

    private static void makeFifo(final Path path) throws IOException {
        int exit;
        try {
            exit = new ProcessBuilder("mkfifo", path.toString()).redirectErrorStream(true).start().waitFor();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while creating a FIFO", interrupted);
        } catch (IOException unavailable) {
            exit = -1;
        }
        assumeTrue(exit == 0, "mkfifo is not available on this platform");
    }

    /** Unblocks a writer wedged in open(2) on a FIFO (only reachable when the guard is removed). */
    private static void releaseBlockedFifoWriter(final Path fifo) {
        Thread reader = new Thread(() -> {
            try (var in = Files.newInputStream(fifo)) {
                in.read();
            } catch (IOException ignored) {
                // nothing to release
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    @Test
    void EXTRA_write_confinedSingleLinkExistingTarget_isStillOverwritten(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path target = root.resolve("out.yaml");
        Files.writeString(target, "obsolete: long old content that must be replaced\n");
        var mapper = YamlUtil.getYamlMapper();
        var config = Map.of("a", "b");

        new YamlWriter(mapper, PathConfinement.of(root)).write(config, target);

        assertEquals(mapper.writeValueAsString(config), Files.readString(target));
    }

    @Test
    void test_AC812_1_symlinkPlantedAfterConfinementIsNotFollowed(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = tmp.resolve("outside.yaml");
        Files.writeString(outside, "sentinel");
        Path target = root.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root),
                confinedTarget -> Files.createSymbolicLink(confinedTarget, outside));

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));
        assertEquals("sentinel", Files.readString(outside),
                "A link planted after confinement must not redirect the write outside the root");
    }

    @Test
    void test_AC812_2_danglingSymlinkTargetingInsideRootStillWrites(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path realTarget = root.resolve("real.yaml");
        Path suppliedLink = root.resolve("link.yaml");
        Files.createSymbolicLink(suppliedLink, realTarget);
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));

        Path written = writer.write(Map.of("host", "localhost"), suppliedLink);

        assertEquals(realTarget.toRealPath(), written.toRealPath());
        String content = Files.readString(realTarget);
        assertTrue(content.contains("host:") && content.contains("localhost"));
    }

    @Test
    void test_AC812_5_ancestorMovedOutsideRootAfterTraversal_failsWithoutWritingOutside(
            @TempDir final Path tmp) throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path ancestor = Files.createDirectory(root.resolve("ancestor"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path parked = outside.resolve("parked");
        Path target = ancestor.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root), ignored -> {
            Files.move(ancestor, parked);
            Files.createSymbolicLink(ancestor, outside);
        });

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));

        assertFalse(Files.exists(parked.resolve("out.yaml")),
                "A retained directory moved outside the root must not receive the leaf write");
    }

    @Test
    void EXTRA_write_retainedDirectoryNoLongerAtConfinedPath_failsClosed(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path ancestor = Files.createDirectory(root.resolve("ancestor"));
        Path parked = root.resolve("parked");
        Path target = ancestor.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root),
                ignored -> Files.move(ancestor, parked));

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));

        assertFalse(Files.exists(parked.resolve("out.yaml")),
                "A directory that can no longer be proven at its confined path must not be written");
    }

    /** Serialises by chmod-ing its directory read-only and then failing, so cleanup cannot delete. */
    public static final class FailingAfterLockingDirectory {
        private final Path directory;

        FailingAfterLockingDirectory(final Path directory) {
            this.directory = directory;
        }

        public String getHost() throws IOException {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("r-x------"));
            throw new IllegalStateException("primary serialization failure");
        }
    }

    @Test
    void test_AC812_6_cleanupFailureIsSuppressedUnderThePrimaryFailure(@TempDir final Path tmp)
            throws Exception {
        assumeTrue(Files.getFileStore(tmp).supportsFileAttributeView("posix"));
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path target = root.resolve("out.yaml");
        Files.writeString(target, "original");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("r-x------"));
        boolean bypassesPermissions = Files.isWritable(root);
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        assumeFalse(bypassesPermissions, "permission bits are not enforced for this user (root)");
        try {
            IOException thrown = assertThrows(IOException.class,
                    () -> writer.write(new FailingAfterLockingDirectory(root), target));
            Throwable cause = thrown;
            boolean primaryFound = false;
            while (cause != null && !primaryFound) {
                primaryFound = String.valueOf(cause.getMessage()).contains("primary serialization failure");
                cause = cause.getCause();
            }
            assertTrue(primaryFound, "The serialization failure must remain the primary failure: " + thrown);
            assertTrue(java.util.Arrays.stream(thrown.getSuppressed()).anyMatch(
                            suppressed -> suppressed instanceof java.nio.file.FileSystemException
                                    && String.valueOf(((java.nio.file.FileSystemException) suppressed)
                                            .getFile()).contains(".appbase-write-")),
                    "The failed temp-sibling deletion must be attached as suppressed, was: "
                            + java.util.Arrays.toString(thrown.getSuppressed()));
        } finally {
            Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        }
    }

    @Test
    void EXTRA_write_nestedAncestorMovedOutsideRootAfterTraversal_failsClosed(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path top = Files.createDirectory(root.resolve("top"));
        Files.createDirectory(top.resolve("leafdir"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path target = top.resolve("leafdir").resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root),
                ignored -> Files.move(top, outside.resolve("parked")));

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));

        assertFalse(Files.exists(outside.resolve("parked").resolve("leafdir").resolve("out.yaml")),
                "Moving a non-immediate ancestor outside the root must not receive the leaf write");
    }

    @Test
    void EXTRA_write_unmovedRetainedDirectories_stillWriteNestedLeaf(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Files.createDirectories(root.resolve("a").resolve("b"));
        Path target = root.resolve("a").resolve("b").resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));

        writer.write(Map.of("host", "localhost"), target);

        assertTrue(Files.readString(target).contains("localhost"));
    }

    @Test
    void EXTRA_write_ancestorReplacedByDifferentDirectory_failsClosed(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path ancestor = Files.createDirectory(root.resolve("ancestor"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path parked = outside.resolve("parked");
        Path target = ancestor.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root), ignored -> {
            Files.move(ancestor, parked);
            Files.createDirectory(ancestor);
        });

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));

        assertFalse(Files.exists(parked.resolve("out.yaml")),
                "A different directory at the same path must not be mistaken for the retained one");
        assertFalse(Files.exists(ancestor.resolve("out.yaml")));
    }

    @Test
    void EXTRA_write_rootSwappedForSymlinkAfterTraversalStart_failsClosed(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path ancestor = Files.createDirectory(root.resolve("ancestor"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path target = ancestor.resolve("out.yaml");
        // The original directory is moved to a real directory outside, and the root name becomes a
        // symlink to that outside directory: the retained handle's identity then matches the lookup
        // of the confined path THROUGH the symlinked root, so only the root-relative canonical
        // re-check can refuse it.
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root), ignored -> {
            Files.move(ancestor, outside.resolve("ancestor"));
            Files.delete(root);
            Files.createSymbolicLink(root, outside);
        });

        assertThrows(IOException.class,
                () -> writer.write(Map.of("host", "localhost"), target));

        assertFalse(Files.exists(outside.resolve("ancestor").resolve("out.yaml")),
                "A root swapped for a symlink mid-write must not redirect the leaf write");
    }

    /** Runs a caller-supplied action while Jackson serialises it, i.e. between leaf open and close. */
    public static final class ActingDuringSerialization {
        private final IoAction action;

        ActingDuringSerialization(final IoAction action) {
            this.action = action;
        }

        public String getHost() throws IOException {
            action.run();
            return "localhost";
        }
    }

    @FunctionalInterface
    interface IoAction {
        void run() throws IOException;
    }

    private static java.util.List<String> names(final Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void test_AC812_7_hardLinkToNewFileMadeDuringSerialization_failsClosed(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path linked = outside.resolve("linked.yaml");
        Path target = root.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));
        var config = new ActingDuringSerialization(() -> {
            try (var stream = Files.list(root)) {
                Files.createLink(linked, stream.findFirst().orElseThrow());
            }
        });

        assertThrows(IOException.class, () -> writer.write(config, target));

        assertFalse(Files.exists(target),
                "A new file hard-linked elsewhere during serialization must not appear at the name");
        assertEquals(java.util.List.of(), names(root), "No temporary sibling may be left behind");
        assertEquals(0L, Files.size(linked),
                "No copy of the document may survive through the hard link");
    }

    @Test
    void test_AC812_8_ancestorMovedAfterLeafOpenBeforeSerializationEnds_failsClosed(
            @TempDir final Path tmp) throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path ancestor = Files.createDirectory(root.resolve("ancestor"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path parked = outside.resolve("parked");
        Path target = ancestor.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));
        var config = new ActingDuringSerialization(() -> {
            Files.move(ancestor, parked);
            Files.createSymbolicLink(ancestor, outside);
        });

        assertThrows(IOException.class, () -> writer.write(config, target));

        assertFalse(Files.exists(parked.resolve("out.yaml")),
                "A directory moved during serialization must not end up holding the final file");
        assertEquals(java.util.List.of(), names(parked), "No temporary sibling may be left behind");
    }

    @Test
    void test_AC812_9_siblingSubstitutedDuringSerialization_failsClosedAndWipesDocument(
            @TempDir final Path tmp) throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path keep = outside.resolve("keep.yaml");
        Path target = root.resolve("out.yaml");
        var writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root));
        // The attacker's file has exactly the size of the document, so only the recorded identity
        // (not the size comparison) can tell it from the writer's own sibling.
        int documentSize = YamlUtil.getYamlMapper()
                .writeValueAsBytes(new ActingDuringSerialization(() -> { })).length;
        var config = new ActingDuringSerialization(() -> {
            Path sibling;
            try (var stream = Files.list(root)) {
                sibling = stream.findFirst().orElseThrow();
            }
            // Keep a link to the writer's real inode, then put an attacker file under its name.
            Files.createLink(keep, sibling);
            Files.delete(sibling);
            Files.writeString(sibling, "x".repeat(documentSize));
        });

        assertThrows(IOException.class, () -> writer.write(config, target));

        assertFalse(Files.exists(target), "The attacker's file must not become the final target");
        assertEquals(java.util.List.of(), names(root), "No sibling may be left behind");
        assertEquals(0L, Files.size(keep),
                "The writer's own inode must be wiped, so no copy of the document survives");
    }
}
