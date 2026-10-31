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

package com.viveworks.appbase.configwizard.security;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Confines caller-supplied filesystem paths to a root directory.
 *
 * <p>Resolution: a relative path resolves against the root, never the process working directory.
 * The result is canonicalised before the containment check, because {@code normalize()} alone
 * leaves a symlinked parent inside the root while the real target is outside it.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class PathConfinement {

    @Nullable
    private final Path root;

    private PathConfinement(@Nullable final Path root) {
        this.root = root;
    }

    /**
     * Creates a confinement rooted at the given directory.
     *
     * @param root the only directory tree that {@link #confine} will permit; must not be null
     * @return a confinement that rejects everything outside {@code root}
     */
    public static PathConfinement of(final Path root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null; use none() for the console");
        }
        return new PathConfinement(root.toAbsolutePath().normalize());
    }

    /**
     * Creates an unconfined instance that permits every path.
     *
     * <p>⚠️ For the <strong>interactive console only</strong>, where the operator types the output
     * directory and is the same person who started the process. It MUST NOT be used in any server
     * assembly — doing so restores the unauthenticated arbitrary-file-write of internal work item 751.
     *
     * @return a confinement that permits every path
     */
    public static PathConfinement none() {
        return new PathConfinement(null);
    }

    /**
     * Reports whether this instance restricts anything.
     *
     * @return true when a root is configured
     */
    public boolean isConfined() {
        return root != null;
    }

    /**
     * Returns the canonical form of the configured root, resolved now.
     *
     * @return the canonical root
     * @throws IllegalStateException when this instance is unconfined
     * @throws PathOutsideRootException when the root cannot be canonicalised
     */
    public Path canonicalRoot() {
        if (root == null) {
            throw new IllegalStateException("unconfined");
        }
        return canonicalise(root);
    }

    /**
     * Renders a written path for an API response without disclosing the absolute filesystem layout
     * (A10-01, internal work item 1449). When confined, returns the path relative to the root; when unconfined (the
     * console), reduces to the file name so no absolute prefix is echoed either way.
     *
     * @param written the path a write landed at
     * @return a root-relative display string when confined, otherwise the file name
     */
    public String toDisplayPath(final Path written) {
        final Path normalized = written.toAbsolutePath().normalize();
        if (root != null) {
            // Relativise against the CANONICAL root, not the raw field. confine() returns a
            // symlink-resolved path, so relativising it against a merely-normalised root produces a leaky
            // `../real/...` escape when the root sits behind a symlink — the raw-root vs canonicalised
            // mismatch internal work item 751/internal work item 806 burned this file on.
            // Canonicalise both sides (the same primitive confine() uses, which tolerates a
            // not-yet-existing target). Fall back to the file name if the write somehow lands outside
            // the root, so no absolute prefix leaks.
            final Path canonicalRoot = canonicalise(root);
            final Path canonicalWritten = canonicalise(normalized);
            if (canonicalWritten.startsWith(canonicalRoot)) {
                return canonicalRoot.relativize(canonicalWritten).toString();
            }
        }
        return normalized.getFileName().toString();
    }

    /**
     * Canonicalises {@code userPath} and asserts it is contained by the root.
     *
     * @param userPath a caller-supplied path; relative paths resolve against the root
     * @return the absolute, canonical path
     * @throws PathOutsideRootException when the resolved path escapes the root, or when an existing
     *                                  component's real target cannot be determined — an unreadable
     *                                  link, a permission error, or a symlink cycle. Such a path is
     *                                  refused rather than treated as inside, because a path that
     *                                  cannot be canonicalised is one whose target is unknown
     */
    public Path confine(final String userPath) {
        if (userPath == null) {
            throw new IllegalArgumentException("userPath must not be null");
        }
        return confine(Path.of(userPath));
    }

    /**
     * Canonicalises {@code userPath} and asserts it is contained by the root.
     *
     * @param userPath a caller-supplied path; relative paths resolve against the root
     * @return the absolute, canonical path
     * @throws PathOutsideRootException when the resolved path escapes the root, or when an existing
     *                                  component's real target cannot be determined — an unreadable
     *                                  link, a permission error, or a symlink cycle. Such a path is
     *                                  refused rather than treated as inside, because a path that
     *                                  cannot be canonicalised is one whose target is unknown
     */
    public Path confine(final Path userPath) {
        if (userPath == null) {
            throw new IllegalArgumentException("userPath must not be null");
        }
        if (root == null) {
            return userPath;
        }
        final Path resolved = userPath.isAbsolute()
                ? userPath.normalize()
                : root.resolve(userPath).normalize();

        final Path canonical = canonicalise(resolved);
        final Path canonicalRoot = canonicalise(root);

        if (!canonical.startsWith(canonicalRoot)) {
            throw new PathOutsideRootException();
        }
        return canonical;
    }

    /**
     * Bounds symlink-chain following, mirroring the kernel's {@code ELOOP} limit. A cyclic pair of
     * links would otherwise recurse forever.
     */
    private static final int MAX_LINK_DEPTH = 40;

    /**
     * Resolves symlinks as far as the path exists, then re-appends the remaining segments.
     *
     * <p>{@link Path#toRealPath} throws when the target does not exist, but writes create their
     * target, so the nearest existing ancestor is canonicalised instead. Without this, a symlinked
     * parent directory escapes the root while every string-level check passes.
     *
     * @param path an absolute, normalised path
     * @return the path with its existing prefix canonicalised
     */
    private static Path canonicalise(final Path path) {
        return canonicalise(path, MAX_LINK_DEPTH);
    }

    /**
     * Canonicalises {@code path}, following at most {@code depth} further symlink hops.
     *
     * <p>⚠️ The climb tests for presence with {@link LinkOption#NOFOLLOW_LINKS}. Plain
     * {@link Files#exists(Path, LinkOption...)} <em>follows</em> a symlink and reports
     * {@code false} for a dangling one, so a link inside the root pointing at a not-yet-existing
     * outside path read as a segment that did not exist yet, was re-appended to the real root prefix
     * and accepted — while the write followed the link and landed outside the root (internal work item 806 R-4).
     *
     * <p>⚠️ {@code NOFOLLOW_LINKS} alone is <strong>not</strong> the fix. Counting a dangling link as
     * present only moves the failure: {@link Path#toRealPath} then throws on it, and the old
     * {@code catch} returned the lexical path, which is accepted for the same reason. A link that
     * exists must therefore be resolved through {@link Files#readSymbolicLink} and canonicalised
     * again, so that a broken link is followed to where it <em>would</em> write.
     *
     * <p>⚠️ It <strong>fails closed</strong>. The previous version returned the lexical path whenever
     * canonicalisation failed, which is fail-open in a containment primitive: a component whose real
     * target cannot be determined — an unreadable link, a permission error, a symlink cycle that
     * exhausts {@link #MAX_LINK_DEPTH} — then reached a {@code startsWith} check on a string the
     * caller has no grounds to trust. Unresolvable now means refused.
     *
     * @param path an absolute, normalised path
     * @param depth remaining symlink hops before the chain is treated as unresolvable
     * @return the path with its existing prefix canonicalised
     * @throws PathOutsideRootException when an existing component cannot be canonicalised
     */
    private static Path canonicalise(final Path path, final int depth) {
        Path existing = path;
        int missingSegments = 0;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
            missingSegments++;
        }
        if (existing == null) {
            return path;
        }
        final Path realExisting = resolveExisting(existing, depth);
        if (realExisting == null) {
            throw new PathOutsideRootException();
        }
        if (missingSegments == 0) {
            return realExisting;
        }
        final int nameCount = path.getNameCount();
        return realExisting.resolve(path.subpath(nameCount - missingSegments, nameCount));
    }

    /**
     * Canonicalises a path component that is known to exist, resolving it as a link when it is one.
     *
     * @param existing a path that exists, possibly as a dangling symlink
     * @param depth remaining symlink hops
     * @return the canonical path, or null when it cannot be determined
     */
    @Nullable
    private static Path resolveExisting(final Path existing, final int depth) {
        if (Files.isSymbolicLink(existing) && depth > 0) {
            try {
                final Path target = Files.readSymbolicLink(existing);
                final Path parent = existing.getParent();
                final Path resolvedTarget = target.isAbsolute() || parent == null
                        ? target
                        : parent.resolve(target);
                return canonicalise(resolvedTarget.toAbsolutePath().normalize(), depth - 1);
            } catch (IOException e) {
                return null;
            }
        }
        try {
            return existing.toRealPath();
        } catch (IOException e) {
            return null;
        }
    }
}
