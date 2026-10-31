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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PathConfinement}, the containment primitive behind internal work item 751.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
final class PathConfinementTest {

    @TempDir Path root;

    /**
     * {@code @TempDir} on macOS hands back {@code /var/...}, which is a symlink to
     * {@code /private/var/...}. Comparing against a raw temp path would fail for reasons
     * unrelated to the code under test.
     *
     * @param p the path to canonicalise
     * @return the real path
     */
    private static Path realPath(final Path p) {
        try {
            return p.toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A10-01 (internal work item 1449): a success response must report where a file landed <em>relative to the root</em>,
     * never the absolute filesystem path, which would disclose the server's directory layout.
     */
    @Test
    void toDisplayPath_confined_returnsRelativePathWithinRoot() {
        final PathConfinement confinement = PathConfinement.of(root);
        final Path written = root.resolve("sub").resolve("app.yaml");

        final String display = confinement.toDisplayPath(written);

        assertFalse(Path.of(display).isAbsolute(),
                "display path must be relative, not the absolute filesystem path: " + display);
        assertEquals("sub/app.yaml", display.replace('\\', '/'),
                "display path must be the write target relative to the root");
    }

    /**
     * A10-01 (internal work item 1449) regression: when the root sits behind a symlink, a written path routed through
     * {@link PathConfinement#confine} is symlink-resolved, so relativising it against a merely-normalised
     * root produces a leaky {@code ../real/...} escape — the raw-root vs canonicalised mismatch internal work item 751/internal work item 806
     * burned this file on. The display path must stay a clean, root-relative path with no {@code ..}.
     */
    @Test
    void toDisplayPath_confinedThroughSymlinkedRoot_staysCleanRelative(@TempDir final Path tmp)
            throws IOException {
        final Path realRoot = Files.createDirectory(tmp.resolve("real"));
        final Path linkRoot = Files.createSymbolicLink(tmp.resolve("link"), realRoot);
        final PathConfinement confinement = PathConfinement.of(linkRoot);
        final Path sub = Files.createDirectory(realRoot.resolve("sub"));
        final Path written = Files.createFile(sub.resolve("app.yaml"));

        // confine() returns the symlink-resolved (canonical) path under realRoot.
        final String display = confinement.toDisplayPath(confinement.confine(written.toString()));

        assertFalse(display.contains(".."),
                "display path must not escape the symlinked root via ..: " + display);
        assertFalse(Path.of(display).isAbsolute(), "display path must be relative: " + display);
        assertEquals("sub/app.yaml", display.replace('\\', '/'),
                "display path must be the write target relative to the root");
    }

    /**
     * The console (unconfined) confinement still must not echo an absolute path back; it reduces to
     * the file name.
     */
    @Test
    void toDisplayPath_unconfined_returnsFileNameOnly() {
        assertEquals("app.yaml",
                PathConfinement.none().toDisplayPath(Path.of("/etc/secret/app.yaml")),
                "an unconfined confinement must still not echo an absolute path");
    }

    @Test
    void confine_relativePathInsideRoot_resolvesAgainstRoot() {
        PathConfinement confinement = PathConfinement.of(root);
        Path result = confinement.confine("configs/redis.yaml");
        assertTrue(result.startsWith(realPath(root)),
                "A relative path must resolve inside the root, not the CWD");
        assertTrue(result.endsWith(Path.of("configs", "redis.yaml")));
    }

    @Test
    void confine_targetThatDoesNotExistYet_isAccepted() {
        PathConfinement confinement = PathConfinement.of(root);
        Path result = confinement.confine(root.resolve("not/created/yet.yaml").toString());
        assertFalse(Files.exists(result), "Writes create their target; it must not need to pre-exist");
        assertTrue(result.startsWith(realPath(root)));
    }

    @Test
    void confine_dotDotTraversal_isRejected() {
        PathConfinement confinement = PathConfinement.of(root);
        assertThrows(PathOutsideRootException.class,
                () -> confinement.confine("../../etc/passwd"));
    }

    @Test
    void confine_absolutePathOutsideRoot_isRejected() {
        PathConfinement confinement = PathConfinement.of(root);
        assertThrows(PathOutsideRootException.class,
                () -> confinement.confine("/etc/passwd"));
    }

    @Test
    void confine_prefixSiblingDirectory_isRejected() throws IOException {
        Path base = Files.createDirectory(root.resolve("base"));
        Path sibling = Files.createDirectory(root.resolve("base-evil"));
        PathConfinement confinement = PathConfinement.of(base);
        assertThrows(PathOutsideRootException.class,
                () -> confinement.confine(sibling.resolve("x.yaml").toString()),
                "'/base-evil' shares a string prefix with '/base' but is not inside it");
    }

    @Test
    void confine_pathEscapingOnlyViaSymlink_isRejected() throws IOException {
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path base = Files.createDirectory(root.resolve("base"));
        Files.createSymbolicLink(base.resolve("escape"), outside);
        PathConfinement confinement = PathConfinement.of(base);
        assertThrows(PathOutsideRootException.class,
                () -> confinement.confine(base.resolve("escape/x.yaml").toString()),
                "normalize() alone leaves this inside the root; only canonicalisation catches it");
    }

    @Test
    void confine_danglingSymlinkAsFinalComponent_isRejected() throws IOException {
        Path base = Files.createDirectory(root.resolve("base"));
        Path outsideTarget = root.resolve("outside-target.yaml");
        Files.createSymbolicLink(base.resolve("escape"), outsideTarget);
        assertFalse(Files.exists(outsideTarget), "the link must dangle for this to be the R-4 case");
        PathConfinement confinement = PathConfinement.of(base);
        assertThrows(PathOutsideRootException.class,
                () -> confinement.confine(base.resolve("escape").toString()),
                "Files.exists follows the link and reports false for a dangling one, so the segment "
                        + "reads as 'not created yet' and is re-appended to the real root");
    }

    @Test
    void confine_danglingSymlinkedParent_isRejected() throws IOException {
        Path base = Files.createDirectory(root.resolve("base"));
        Files.createSymbolicLink(base.resolve("outdir"), root.resolve("outside-dir"));
        PathConfinement confinement = PathConfinement.of(base);
        assertThrows(PathOutsideRootException.class,
                () -> confinement.confine(base.resolve("outdir/x.yaml").toString()),
                "A dangling symlinked parent escapes for the same reason as a dangling leaf");
    }

    /**
     * The opposite direction from the two arms above, and the reason the fix has two halves.
     *
     * <p>Counting a dangling link as present is what stops the escape. Resolving the link is what
     * stops the <em>over-refusal</em>: a broken link pointing back inside the root is a legitimate
     * write target, and failing closed on every unresolvable component alone would reject it.
     *
     * <p>⚠️ Without this arm, removing the link resolution left the suite green on macOS — the
     * {@code /var} → {@code /private/var} difference made the lexical fallback fail
     * {@code startsWith} for a platform reason rather than because the guard worked. The mutation
     * survived, and a mutation that survives is a test that is not measuring the mechanism.
     *
     * @throws IOException when the fixture cannot be created
     */
    @Test
    void confine_danglingSymlinkTargetingInsideRoot_isAccepted() throws IOException {
        Path base = Files.createDirectory(root.resolve("base"));
        Path insideTarget = base.resolve("real.yaml");
        Files.createSymbolicLink(base.resolve("link"), insideTarget);
        assertFalse(Files.exists(insideTarget), "the link must dangle, as it would before a write");
        PathConfinement confinement = PathConfinement.of(base);
        Path result = confinement.confine(base.resolve("link").toString());
        assertEquals(realPath(base).resolve("real.yaml"), result,
                "a broken link pointing inside the root resolves to its target, and is permitted");
    }

    /**
     * A component that exists but whose real target cannot be determined must be refused.
     *
     * <p>A symlink cycle exhausts {@code MAX_LINK_DEPTH} and then fails {@code toRealPath} with
     * {@code ELOOP}. The old code answered that by returning the <em>lexical</em> path, which is
     * inside the root by construction and therefore accepted — fail-open in the one class whose
     * whole job is to refuse.
     *
     * <p>⚠️ The root is taken as a real path deliberately. With a raw {@code @TempDir} the macOS
     * {@code /var} → {@code /private/var} difference would make the lexical fallback fail
     * {@code startsWith} anyway, and the arm would pass without the guard existing.
     *
     * @throws IOException when the fixture cannot be created
     */
    @Test
    void confine_symlinkCycle_isRejected() throws IOException {
        Path base = Files.createDirectory(root.resolve("base")).toRealPath();
        Files.createSymbolicLink(base.resolve("a"), base.resolve("b"));
        Files.createSymbolicLink(base.resolve("b"), base.resolve("a"));
        PathConfinement confinement = PathConfinement.of(base);
        assertThrows(PathOutsideRootException.class,
                () -> confinement.confine(base.resolve("a").toString()),
                "an unresolvable component must be refused, not passed through as its lexical path");
    }

    @Test
    void confine_rootItself_isAccepted() {
        PathConfinement confinement = PathConfinement.of(root);
        assertEquals(realPath(root), confinement.confine(root.toString()));
    }

    @Test
    void none_acceptsAnyPath() {
        PathConfinement confinement = PathConfinement.none();
        assertFalse(confinement.isConfined());
        assertEquals(Path.of("/etc/passwd"), confinement.confine("/etc/passwd"));
    }

    @Test
    void EXTRA_canonicalRoot_isTheRealPathAndUnconfinedRefuses() throws IOException {
        Path realRoot = root.toRealPath();

        assertEquals(realRoot, PathConfinement.of(root).canonicalRoot());
        assertThrows(IllegalStateException.class, () -> PathConfinement.none().canonicalRoot());
    }
}
