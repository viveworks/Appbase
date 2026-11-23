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

package com.viveworks.appbase.core.service.configuration.accessor.store.filesystem;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.environment.EnvironmentResolver;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.configuration.accessor.target.TargetResolver;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A configuration store that reads / writes configurations from / to a file.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings({"CRLF_INJECTION_LOGS", "IMC_IMMATURE_CLASS_NO_TOSTRING", "DRE_DECLARED_RUNTIME_EXCEPTION"})
public class FileConfigurationStore implements ConfigurationStore {
    private final String id;
    private final Path root;
    private final Path path;
    private final Path relativePath;
    private final MimeType mimeType;
    private final AtomicBoolean shutdown;
    private final BeforeAccess beforeAccess;
    private final BeforeMove beforeMove;

    public FileConfigurationStore(final String filename, final MimeType mimeType) {
        this(parentOf(filename), filename, mimeType, target -> { });
        checkPath();
    }

    public FileConfigurationStore(final Path root,
                                  final String baseFilename,
                                  final EnvironmentResolver environmentResolver,
                                  final FileMimeTypeResolver fileMimeTypeResolver,
                                  final TargetResolver targetResolver)
    {
        this(root, resolveTarget(baseFilename, environmentResolver, fileMimeTypeResolver, targetResolver));
    }

    private FileConfigurationStore(final Path root, final ResolvedTarget resolvedTarget) {
        this(root, resolvedTarget.filename(), resolvedTarget.mimeType(), target -> { });
    }

    public FileConfigurationStore(final Path root, final String filename, final MimeType mimeType) {
        this(root, filename, mimeType, target -> { });
    }

    FileConfigurationStore(final Path requestedRoot, final String filename, final MimeType mimeType,
                           final BeforeAccess beforeAccess)
    {
        this(requestedRoot, filename, mimeType, beforeAccess, target -> { });
    }

    FileConfigurationStore(final Path requestedRoot, final String filename, final MimeType mimeType,
                           final BeforeAccess beforeAccess, final BeforeMove beforeMove)
    {
        AppbaseUtil.notNull(requestedRoot, "root");
        AppbaseUtil.notBlank(filename, "filename");
        AppbaseUtil.notNull(mimeType, "mimeType");
        AppbaseUtil.notNull(beforeAccess, "beforeAccess");
        AppbaseUtil.notNull(beforeMove, "beforeMove");

        final Path requestedAbsoluteRoot = requestedRoot.toAbsolutePath().normalize();
        final Path canonicalRoot;
        try {
            canonicalRoot = requestedRoot.toRealPath();
        } catch (IOException ioe) {
            throw new AppbaseRuntimeException("Cannot resolve configuration root " + requestedRoot, ioe);
        }
        final Path requestedPath = Path.of(filename).toAbsolutePath().normalize();
        final Path requestedRelative;
        if (requestedPath.startsWith(requestedAbsoluteRoot)) {
            requestedRelative = requestedAbsoluteRoot.relativize(requestedPath);
        } else if (requestedPath.startsWith(canonicalRoot)) {
            requestedRelative = canonicalRoot.relativize(requestedPath);
        } else {
            throw new AppbaseRuntimeException(
                    "Configuration file must be a child of root " + canonicalRoot + ": " + requestedPath);
        }
        if (requestedRelative.getNameCount() == 0) {
            throw new AppbaseRuntimeException("Configuration file cannot be the root directory: " + requestedPath);
        }
        final Path normalizedPath = canonicalRoot.resolve(requestedRelative).normalize();

        this.root = canonicalRoot;
        this.path = normalizedPath;
        this.relativePath = canonicalRoot.relativize(normalizedPath);
        this.mimeType = mimeType;
        this.id = AppbaseUtil.configurationStoreIdPrefix(this) + normalizedPath;
        this.shutdown = new AtomicBoolean(false);
        this.beforeAccess = beforeAccess;
        this.beforeMove = beforeMove;
    }

    private static Path parentOf(final String filename) {
        AppbaseUtil.notBlank(filename, "filename");
        final Path absolute = Path.of(filename).toAbsolutePath().normalize();
        final Path parent = absolute.getParent();
        if (parent == null) {
            throw new AppbaseRuntimeException("Configuration file has no parent directory: " + filename);
        }
        return parent;
    }

    private static ResolvedTarget resolveTarget(final String baseFilename,
                                                final EnvironmentResolver environmentResolver,
                                                final FileMimeTypeResolver fileMimeTypeResolver,
                                                final TargetResolver targetResolver)
    {
        AppbaseUtil.notNull(baseFilename, "baseFilename");
        AppbaseUtil.notNull(environmentResolver, "environmentResolver");
        AppbaseUtil.notNull(fileMimeTypeResolver, "fileMimeTypeResolver");
        AppbaseUtil.notNull(targetResolver, "targetResolver");

        final String environment = environmentResolver.resolveEnvironment();
        final String filename = targetResolver.resolveTarget(baseFilename, environment);
        final MimeType resolvedMimeType = fileMimeTypeResolver.resolveMimeType(baseFilename);
        if (resolvedMimeType == null) {
            throw new AppbaseRuntimeException("Cannot resolve MIME type for base file name " + baseFilename);
        }
        return new ResolvedTarget(filename, resolvedMimeType);
    }

    private void checkPath() {
        try {
            final BasicFileAttributes attributes = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) {
                throw new AppbaseRuntimeException("The file " + path + " is not a regular file");
            }
        } catch (IOException ioe) {
            throw new AppbaseRuntimeException("The file " + path + " is not readable", ioe);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Collection<RawConfiguration> readRawConfigurations(
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Collection<RawConfiguration> rawConfigurations = new ArrayList<>();
        final byte[] bytes;
        try {
            bytes = withParentDirectory((directory, directoryPath, name) -> {
                beforeAccess.run(path);
                verifyRetainedDirectory(directory, directoryPath);
                try (SeekableByteChannel channel = directory.newByteChannel(
                        name, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)))
                {
                    return Channels.newInputStream(channel).readAllBytes();
                }
            });
        } catch (IOException ioe) {
            throw new ConfigurationStoreException("Error reading file " + path, ioe);
        }

        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name(path.toFile().getAbsolutePath())
                .value(bytes)
                .mimeType(mimeType)
                .timestamp(System.currentTimeMillis())
                .build();
        rawConfigurations.add(rawConfiguration);

        return rawConfigurations;
    }

    @Override
    public void writeRawConfiguration(
            final RawConfiguration rawConfiguration,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(rawConfiguration, "rawConfiguration");
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final byte[] bytes = rawConfiguration.getValue();
        try {
            withParentDirectory((directory, directoryPath, name) -> {
                beforeAccess.run(path);
                verifyRetainedDirectory(directory, directoryPath);
                writeAtomically(directory, directoryPath, name, bytes);
                return null;
            });
        } catch (IOException ioe) {
            throw new ConfigurationStoreException(
                    "Error while write configuration file " + path.toFile().getAbsolutePath(), ioe);
        }
    }

    private void writeAtomically(final SecureDirectoryStream<Path> directory, final Path directoryPath,
                                 final Path name, final byte[] bytes) throws IOException
    {
        final Path temporary = name.resolveSibling(".appbase-config-" + UUID.randomUUID() + ".tmp");
        boolean moved = false;
        try {
            try (SeekableByteChannel channel = directory.newByteChannel(
                    temporary,
                    Set.<OpenOption>of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS)))
            {
                final ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                if (channel instanceof FileChannel fileChannel) {
                    fileChannel.force(true);
                }
            }
            beforeMove.run(path);
            verifyRetainedDirectory(directory, directoryPath);
            requireRegularOrAbsent(directory, name);
            directory.move(temporary, directory, name);
            moved = true;
        } catch (IOException | RuntimeException primary) {
            // Deliberately not Error: an OutOfMemoryError/StackOverflowError-class failure cannot be
            // safely handled here, and attempting best-effort cleanup under one would risk masking
            // the real condition. IOException/RuntimeException are caught (not narrowed further)
            // because the guarantee this method makes is "the temp file never survives a failed
            // write/move", for whichever failure mode interrupts the sequence above.
            if (!moved) {
                try {
                    directory.deleteFile(temporary);
                } catch (NoSuchFileException ignored) {
                    // The sibling was never created, or was already removed.
                } catch (IOException | RuntimeException cleanup) {
                    primary.addSuppressed(cleanup);
                }
            }
            throw primary;
        }
    }

    private static void requireRegularOrAbsent(final SecureDirectoryStream<Path> directory, final Path name)
            throws IOException
    {
        try {
            final BasicFileAttributes attributes = directory.getFileAttributeView(
                    name, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
            if (!attributes.isRegularFile()) {
                throw new IOException("Configuration target is not a regular file");
            }
        } catch (NoSuchFileException ignored) {
            // Creating a new leaf is permitted.
        }
    }

    private <T> T withParentDirectory(final ParentOperation<T> operation) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            if (!(stream instanceof SecureDirectoryStream)) {
                throw new IOException("Filesystem provider does not support secure configuration access");
            }
            @SuppressWarnings("unchecked")
            final SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) stream;
            return descend(secure, root, relativePath.iterator(), operation);
        }
    }

    private <T> T descend(final SecureDirectoryStream<Path> directory, final Path directoryPath,
                          final Iterator<Path> names, final ParentOperation<T> operation) throws IOException
    {
        if (!names.hasNext()) {
            throw new IOException("Configuration path has no filename");
        }
        final Path name = names.next();
        if (!names.hasNext()) {
            return operation.run(directory, directoryPath, name);
        }
        final Path childPath = directoryPath.resolve(name);
        try (SecureDirectoryStream<Path> child = directory.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS)) {
            verifyRetainedDirectory(child, childPath);
            return descend(child, childPath, names, operation);
        }
    }

    private void verifyRetainedDirectory(final SecureDirectoryStream<Path> directory, final Path directoryPath)
            throws IOException
    {
        final Object retained = directory.getFileAttributeView(BasicFileAttributeView.class)
                .readAttributes().fileKey();
        final Object named = Files.readAttributes(
                directoryPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey();
        final Path canonicalNamed = directoryPath.toRealPath();
        if (retained == null || !retained.equals(named) || !canonicalNamed.equals(directoryPath) ||
                !canonicalNamed.startsWith(root))
        {
            throw new IOException("Retained directory is no longer at its confined path");
        }
    }

    @Override
    public boolean supportsWrite() {
        return true;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        this.shutdown.set(true);
    }

    @Override
    public String toString() {
        return "FileConfigurationStore{" +
                "id=" + id +
                ", root=" + root +
                ", path=" + path +
                ", mimeType=" + mimeType +
                '}';
    }

    @FunctionalInterface
    interface BeforeAccess {
        void run(Path target) throws IOException;
    }

    @FunctionalInterface
    interface BeforeMove {
        void run(Path target) throws IOException;
    }

    private record ResolvedTarget(String filename, MimeType mimeType) { }

    @FunctionalInterface
    private interface ParentOperation<T> {
        T run(SecureDirectoryStream<Path> directory, Path directoryPath, Path name) throws IOException;
    }
}
