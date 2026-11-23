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

package com.viveworks.appbase.configwizard.configurations.writer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import com.viveworks.appbase.configwizard.security.PathOutsideRootException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Serialises any config object to YAML via a Jackson {@link ObjectMapper} backed by a YAML factory.
 *
 * <p>When no explicit output path is provided, the generated file
 * ({@code appbase-<SimpleClassName>-<epoch-millis>.yaml}) is placed:
 * <ul>
 *   <li><strong>inside the confinement root</strong>, when this writer is confined — which is
 *       every server-created instance. Falling back to {@code java.io.tmpdir} there would put
 *       every default write outside the root and make the root advisory.</li>
 *   <li>in {@code System.getProperty("java.io.tmpdir")}, only when unconfined
 *       ({@link PathConfinement#none()}) — the interactive console.</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class YamlWriter {

    private final ObjectMapper yamlMapper;
    private final PathConfinement confinement;
    private final BeforeWrite beforeWrite;

    @FunctionalInterface
    interface BeforeWrite {
        void run(Path target) throws IOException;
    }

    /**
     * Constructs a {@code YamlWriter} backed by the supplied YAML {@link ObjectMapper}
     * (e.g. {@link com.viveworks.appbase.core.util.jackson.YamlUtil#getYamlMapper()}).
     *
     * <p>⚠️ There is deliberately no single-argument form. Both API write endpoints converge on
     * this class, so a constructor that defaulted to "unconfined" would make the unsafe choice the
     * silent one — which is how the second unconfined endpoint (internal work item 751 F-08) came to exist alongside
     * the first. Every construction site states which it wants.
     *
     * @param yamlMapper  Jackson ObjectMapper configured with a YAML factory
     * @param confinement restricts every write target; use {@link PathConfinement#none()} for the
     *                    interactive console only
     */
    public YamlWriter(final ObjectMapper yamlMapper, final PathConfinement confinement) {
        this(yamlMapper, confinement, target -> { });
    }

    YamlWriter(final ObjectMapper yamlMapper, final PathConfinement confinement,
            final BeforeWrite beforeWrite) {
        this.yamlMapper = yamlMapper;
        this.confinement = confinement;
        this.beforeWrite = beforeWrite;
    }

    /**
     * Writes the config object to {@code outputPath}, or to a temp file when {@code null}.
     *
     * <p>Three modes:
     * <ul>
     *   <li>{@code outputPath == null} — write to a generated file, <strong>inside the
     *       confinement root when confined</strong>, otherwise in {@code java.io.tmpdir}.</li>
     *   <li>{@code outputPath} is an existing <em>directory</em> — write a generated
     *       filename ({@code appbase-<SimpleClassName>-<epoch>.yaml}) inside that directory.
     *       Useful for session-level output-directory mode.</li>
     *   <li>{@code outputPath} is a file path — write directly to that path. Unconfined console
     *       mode creates missing parents; confined server mode requires parents to exist so it can
     *       retain secure directory descriptors through the leaf open.</li>
     * </ul>
     *
     * <p><strong>Threat-model boundary.</strong> Confined writes serialise into a private
     * {@code CREATE_NEW} sibling, never into the final name, and re-verify after serialisation and
     * immediately before the rename that each retained directory is still the confined path's
     * directory, that the sibling is still the inode this writer created (a file swapped in under its
     * name is refused), a regular file with a link count of one, and that the name is still what the
     * up-front check saw. On any failure the writer's own inode is truncated through its open
     * channel before the sibling name is removed, so a hard link made during serialisation keeps no
     * copy of the document. What remains, for new and existing targets alike, is the window from
     * the last verification to the rename: an ancestor rename (needs local write access to an
     * ancestor's parent), a hard link to the sibling, or a substitute file swapped in under the
     * sibling's name in that window is not excluded, and an entry newly appearing at the name is
     * replaced, because the JDK offers no descriptor-to-path query, no
     * {@code openat2(RESOLVE_BENEATH)} and no no-replace rename (Release 2.0). The sibling's key is
     * read by name immediately after creation, so a swap inside that instant is not excluded either.
     * These fixes are in addition to a stated precondition: the confined directory must not be
     * writable by untrusted processes.
     *
     * @param config     any config object to serialise
     * @param outputPath the target path: a file path, an existing directory, or {@code null}
     * @return the path actually written
     * @throws IOException when the file cannot be written
     */
    public Path write(final Object config, @Nullable final Path outputPath) throws IOException {
        Path target;
        if (outputPath == null) {
            target = buildTempPath(config);
        } else if (Files.isDirectory(outputPath)) {
            target = outputPath.resolve(buildFileName(config));
        } else {
            target = outputPath;
        }
        // ⚠️ Confine BEFORE creating directories. Previously createDirectories ran first, so a
        // request that was going to be refused had already created attacker-chosen directories.
        target = confinement.confine(target);
        if (confinement.isConfined()) {
            writeConfined(config, target);
        } else {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            beforeWrite.run(target);
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                yamlMapper.writeValue(out, config);
            }
        }
        return target;
    }

    private void writeConfined(final Object config, final Path target) throws IOException {
        final Path root = target.getRoot();
        if (root == null) {
            throw new IOException("Confined output path has no filesystem root");
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            if (!(stream instanceof SecureDirectoryStream)) {
                throw new IOException("Filesystem provider does not support secure confined writes");
            }
            @SuppressWarnings("unchecked")
            final SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) stream;
            writeRelative(secure, root, confinement.canonicalRoot().getNameCount(), target.iterator(), config,
                    target, checkedInode(target));
        }
    }

    /**
     * Rejects a pre-existing confined target that has more than one hard link (owner ruling D-C (a),
     * internal work item 812), and returns its inode key, or {@code null} when the target does not exist yet.
     * {@code NOFOLLOW_LINKS} cannot see a hard link: truncating one alters the other pathname's
     * content, which may lie outside the root.
     */
    @Nullable
    private static Object checkedInode(final Path target) throws IOException {
        final BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(target, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException absent) {
            return null;
        }
        if (attributes.isSymbolicLink()) {
            throw new IOException("Confined output path is a symbolic link: " + target);
        }
        if (!attributes.isRegularFile()) {
            // A FIFO or device opened for writing can block indefinitely or have side effects.
            throw new IOException("Confined output path is not a regular file: " + target);
        }
        requireSingleLink(target);
        if (attributes.fileKey() == null) {
            throw new IOException("Confined output file identity is unknown");
        }
        return attributes.fileKey();
    }

    /** Rejects a path whose inode has more than one hard link, or whose link count is unknown. */
    private static void requireSingleLink(final Path path) throws IOException {
        final Object links;
        try {
            links = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        } catch (UnsupportedOperationException | IllegalArgumentException unsupported) {
            throw new IOException("Filesystem provider cannot report the link count", unsupported);
        }
        if (!(links instanceof Integer count) || count != 1) {
            throw new IOException("Confined output file is hard-linked or its link count is unknown");
        }
    }

    private void writeRelative(final SecureDirectoryStream<Path> directory, final Path directoryPath,
            final int rootDepth, final Iterator<Path> names, final Object config, final Path target,
            @Nullable final Object checkedInode) throws IOException {
        if (!names.hasNext()) {
            throw new IOException("Confined output path has no filename");
        }
        final Path name = names.next();
        if (names.hasNext()) {
            final Path childPath = directoryPath.resolve(name);
            try (SecureDirectoryStream<Path> child = directory.newDirectoryStream(
                    name, LinkOption.NOFOLLOW_LINKS)) {
                verifyBeneathRoot(child, childPath, rootDepth);
                writeRelative(child, childPath, rootDepth, names, config, target, checkedInode);
            }
            return;
        }

        beforeWrite.run(target);
        // ⚠️ Re-verify AFTER the last point a caller (or attacker) can interpose and immediately
        // BEFORE the leaf is opened: the retained descriptor must still be the directory that the
        // confined path names today (internal work item 2529 review, retained-directory escape).
        verifyBeneathRoot(directory, directoryPath, rootDepth);
        writeViaTemporarySibling(directory, directoryPath, rootDepth, name, config, target, checkedInode);
    }

    /**
     * Fails closed unless the retained directory handle is provably still the directory the
     * confined {@code path} names, and that path is still canonical and beneath the root.
     *
     * <p>A descriptor stays valid after its directory is renamed, so a directory moved outside the
     * root would otherwise keep receiving the leaf write. The handle's identity ({@code fileKey})
     * is compared with a non-following lookup of the path, and {@link PathConfinement#confine} is
     * re-run to catch a symlinked ancestor component. An unknown identity is a refusal.
     *
     * <p>⚠️ <strong>Threat-model boundary (residual).</strong> This is a check, not an atomic
     * operation: the JDK exposes neither a descriptor-to-path query nor
     * {@code openat2(RESOLVE_BENEATH)}, so a rename of an ancestor directory between the last call
     * of this method (made after serialisation, immediately before the rename onto the final name)
     * and that rename is not excluded. Exploiting it needs local write access to an ancestor
     * directory's parent. Full closure needs a platform-specific call and is deferred to
     * Release 2.0.
     */
    private void verifyBeneathRoot(final SecureDirectoryStream<Path> directory, final Path path,
            final int rootDepth) throws IOException {
        final Object retained = directory.getFileAttributeView(BasicFileAttributeView.class)
                .readAttributes().fileKey();
        final Object named;
        try {
            named = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                    .fileKey();
        } catch (NoSuchFileException gone) {
            throw new IOException("Retained directory is no longer at its confined path", gone);
        }
        if (retained == null || !retained.equals(named)) {
            throw new IOException("Retained directory cannot be proven to be the confined path");
        }
        // An ancestor of the root is outside it by definition: identity alone is its proof. A
        // directory at or beneath the root (by depth against the root canonicalised ONCE when the
        // write began, never re-derived mid-write) must also still canonicalise to itself.
        if (path.getNameCount() >= rootDepth) {
            final Path canonical;
            try {
                canonical = confinement.confine(path);
            } catch (PathOutsideRootException escaped) {
                throw new IOException("Retained directory is no longer beneath the root", escaped);
            }
            if (!canonical.equals(path)) {
                throw new IOException("Confined path is no longer canonical beneath the root");
            }
        }
    }

    /**
     * Writes a confined file without ever serialising into, or opening, the final name: the content
     * goes to a fresh {@code CREATE_NEW} sibling that is renamed onto the name once it is proven
     * sound. For an existing target the name is proven (by a descriptor-relative, non-following
     * lookup) to still be the regular file that passed the hard-link check; for an absent target it
     * is proven to still be absent. The sibling itself must be a regular file with a link count of
     * one, so a hard link made to it while the document was being serialised (no write access to
     * any ancestor needed) is refused instead of becoming the written file.
     *
     * <p>The rename replaces whatever entry holds the name: the JDK exposes no no-replace rename
     * ({@code SecureDirectoryStream.move} is {@code renameat}), so the lookup just before it is the
     * guard, and an entry that appears after that lookup is replaced, never opened or written into.
     * A hard link elsewhere to a replaced inode is untouched, and a FIFO or device raced in is
     * replaced rather than opened, so the writer cannot block on it.
     */
    private void writeViaTemporarySibling(final SecureDirectoryStream<Path> directory,
            final Path directoryPath, final int rootDepth, final Path name, final Object config,
            final Path target, @Nullable final Object checkedInode) throws IOException {
        final Path temp = name.resolveSibling(".appbase-write-" + UUID.randomUUID() + ".tmp");
        // An overwrite keeps the old file's permission bits. The creation attribute alone is not
        // enough: the process umask narrows it, so the bits are also set explicitly below (IR2529-06).
        Set<PosixFilePermission> oldPermissions = null;
        if (checkedInode != null) {
            final PosixFileAttributeView posix = directory.getFileAttributeView(name,
                    PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            oldPermissions = posix == null ? null : posix.readAttributes().permissions();
        }
        final FileAttribute<?>[] permissions = oldPermissions == null ? new FileAttribute<?>[0]
                : new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(oldPermissions)};
        SeekableByteChannel channel = null;
        boolean moved = false;
        try {
            channel = directory.newByteChannel(temp,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS), permissions);
            // The identity of the sibling this writer created, recorded before any caller code runs.
            // The channel stays open until the rename so that, on any failure, the real inode can be
            // truncated: a hard link to it (or a copy of the name elsewhere) then keeps no document.
            final Object siblingKey = directory.getFileAttributeView(temp,
                    BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes().fileKey();
            if (siblingKey == null) {
                throw new IOException("Temporary output file identity is unknown");
            }
            final OutputStream out = Channels.newOutputStream(channel);
            yamlMapper.writer().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET).writeValue(out, config);
            out.flush();
            if (channel instanceof FileChannel fileChannel) {
                fileChannel.force(true);
            }
            // Re-verify after serialisation, which can take arbitrarily long and runs caller code.
            verifyBeneathRoot(directory, directoryPath, rootDepth);
            requireUnsharedTemporary(directory, directoryPath, temp, siblingKey, channel);
            if (oldPermissions != null) {
                // Explicit set on the private sibling, descriptor-relative and non-following, so the
                // umask cannot narrow the preserved mode.
                directory.getFileAttributeView(temp, PosixFileAttributeView.class,
                        LinkOption.NOFOLLOW_LINKS).setPermissions(oldPermissions);
            }
            requireNameUnchanged(directory, name, checkedInode);
            directory.move(temp, directory, name);
            moved = true;
            channel.close();
        } catch (IOException | RuntimeException | Error primary) {
            // The primary failure always wins; every cleanup failure is attached to it as suppressed
            // so the caller can see residue. Once the rename has happened nothing is touched.
            if (!moved) {
                discardSibling(directory, temp, channel, primary);
            }
            throw primary;
        }
    }

    /** Wipes the writer's own inode through its open channel, then removes the sibling name. */
    private static void discardSibling(final SecureDirectoryStream<Path> directory, final Path temp,
            @Nullable final SeekableByteChannel channel, final Throwable primary) {
        if (channel != null) {
            try {
                channel.truncate(0);
                if (channel instanceof FileChannel fileChannel) {
                    fileChannel.force(true);
                }
            } catch (IOException | RuntimeException wipe) {
                primary.addSuppressed(wipe);
            }
            try {
                channel.close();
            } catch (IOException | RuntimeException close) {
                primary.addSuppressed(close);
            }
        }
        try {
            directory.deleteFile(temp);
        } catch (NoSuchFileException absent) {
            // the temp sibling was never created, or is already gone
        } catch (IOException | RuntimeException cleanup) {
            primary.addSuppressed(cleanup);
        }
    }

    /** The name must still be exactly what the up-front check saw: the same regular file, or absent. */
    private static void requireNameUnchanged(final SecureDirectoryStream<Path> directory,
            final Path name, @Nullable final Object checkedInode) throws IOException {
        final BasicFileAttributes current;
        try {
            current = directory.getFileAttributeView(name, BasicFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS).readAttributes();
        } catch (NoSuchFileException absent) {
            if (checkedInode == null) {
                return;
            }
            throw new IOException("Confined output file changed after the link check", absent);
        }
        if (checkedInode == null || !current.isRegularFile()
                || !checkedInode.equals(current.fileKey())) {
            throw new IOException("Confined output file changed after the link check");
        }
    }

    /**
     * The temporary sibling must still be the inode this writer created (its key was recorded at
     * creation), a regular file whose link count is still one. The count is read through the path,
     * which must resolve to that same inode; the retained directory was verified immediately before.
     */
    private static void requireUnsharedTemporary(final SecureDirectoryStream<Path> directory,
            final Path directoryPath, final Path temp, final Object siblingKey,
            final SeekableByteChannel channel) throws IOException {
        final BasicFileAttributes held = directory.getFileAttributeView(temp,
                BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
        final Path tempPath = directoryPath.resolve(temp.getFileName());
        final BasicFileAttributes named = Files.readAttributes(tempPath, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        // Both lookups go by NAME, so each would follow a file swapped in under the name; the key
        // recorded when this writer created the sibling is what exposes the substitution.
        if (!held.isRegularFile() || !siblingKey.equals(held.fileKey())
                || !siblingKey.equals(named.fileKey()) || held.size() != channel.position()) {
            throw new IOException("Temporary output file is not the file this writer created");
        }
        requireSingleLink(tempPath);
    }

    /**
     * Generates a unique output filename for the given config object.
     *
     * @param config the config object whose class simple name is used in the file name
     * @return filename of the form {@code appbase-<SimpleClassName>-<epoch>.yaml}
     */
    private String buildFileName(final Object config) {
        return "appbase-" + config.getClass().getSimpleName() + "-" + System.currentTimeMillis() + ".yaml";
    }

    /**
     * Builds the temp file path for the given config object.
     *
     * @param config the config object whose class simple name is used in the file name
     * @return temp file path of the form {@code $TMPDIR/appbase-<SimpleClassName>-<epoch>.yaml}
     */
    private Path buildTempPath(final Object config) {
        if (confinement.isConfined()) {
            // Under confinement the default target must land in the root; otherwise every write
            // that omits an explicit path escapes to java.io.tmpdir and the root is advisory.
            return confinement.confine(Path.of(buildFileName(config)));
        }
        return Path.of(System.getProperty("java.io.tmpdir"), buildFileName(config));
    }
}
