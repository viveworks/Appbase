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
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A configuration store that reads / writes configuration files from / to a directory recursively.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings("IMC_IMMATURE_CLASS_NO_TOSTRING")
public class DirectoryConfigurationStore implements ConfigurationStore {
    private final Logger logger;
    private final String id;
    private final String rootDirectoryName;
    private final Set<MimeType> supportedMimeTypes;
    private final Set<String> ignoreFilenames;
    private final FileMimeTypeResolver fileMimeTypeResolver;
    private final Map<String, FileConfigurationStore> fileConfigurationStoreMap;
    private final Gauge fileConfigurationStoreMapSizeGauge;
    private final Histogram readBytesHistogram;
    private final Timer readTimer;
    private final Histogram writeBytesHistogram;
    private final Timer writeTimer;
    private final Histogram numberOfFilesReadHistogram;
    private final AtomicBoolean shutdown;

    public DirectoryConfigurationStore(final LogService logService,
                                       final MetricService metricService,
                                       final String rootDirectoryName,
                                       final Set<MimeType> supportedMimeTypes,
                                       final FileMimeTypeResolver fileMimeTypeResolver,
                                       @Nullable final Set<String> ignoreFilenames)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(rootDirectoryName, "rootDirectoryName");
        AppbaseUtil.notEmpty(supportedMimeTypes, "supportedMimeTypes");
        AppbaseUtil.notNull(fileMimeTypeResolver, "fileMimeTypeResolver");

        this.logger = logService.getLogger(getClass());
        this.rootDirectoryName = rootDirectoryName;
        this.id = AppbaseUtil.configurationStoreIdPrefix(this) + rootDirectoryName;
        this.supportedMimeTypes = new ConcurrentSkipListSet<>(supportedMimeTypes);
        this.fileMimeTypeResolver = fileMimeTypeResolver;
        this.ignoreFilenames = ignoreFilenames;
        this.fileConfigurationStoreMap = new ConcurrentHashMap<>();

        this.fileConfigurationStoreMapSizeGauge = fileConfigurationStoreMap::size;
        metricService
                .getMetricRegistry().register(
                        metricService.name(getClass(), "fileConfigurationStoreMap", "size", "gauge"),
                        this.fileConfigurationStoreMapSizeGauge);
        this.readBytesHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "readBytes", "histogram"));
        this.readTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "read", "timer"));
        this.writeBytesHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "writeBytes", "histogram"));
        this.writeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "write", "timer"));
        this.numberOfFilesReadHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "numberOfFilesRead", "histogram"));

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Collection<RawConfiguration> readRawConfigurations(
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Timer.Context context = readTimer.time();
        try {
            logger.debug("Reading raw configurations");
            final Map<String, MimeType> mimeTypeByFilenamesMap;
            try {
                mimeTypeByFilenamesMap = createMimeTypeByFilenamesMap();
            } catch (IOException ioe) {
                throw new ConfigurationStoreException("Error while listing all files under " + rootDirectoryName, ioe);
            }

            final AtomicInteger bytesCounter = new AtomicInteger(0);
            final Collection<RawConfiguration> rawConfigurations = new ArrayList<>();
            for (final Map.Entry<String, MimeType> fileEntry : mimeTypeByFilenamesMap.entrySet()) {
                final StringBuilder fileNameBuilder = new StringBuilder(fileEntry.getKey());
                if (!fileNameBuilder.toString().contains(rootDirectoryName)) {
                    fileNameBuilder.insert(0, rootDirectoryName + File.separator);
                }

                final String fileName = fileNameBuilder.toString();
                final MimeType mimeType = fileEntry.getValue();
                final FileConfigurationStore fileConfigurationStore = fileConfigurationStoreMap
                        .computeIfAbsent(
                                fileName,
                                k -> new FileConfigurationStore(Path.of(rootDirectoryName), fileName, mimeType));
                final Collection<RawConfiguration> fileRawConfigurations =
                        fileConfigurationStore.readRawConfigurations(configurationAccessorContext);
                fileRawConfigurations.forEach(rawConfiguration -> {
                    final byte[] value = rawConfiguration.getValue();
                    if (value != null) {
                        bytesCounter.addAndGet(value.length);
                    }
                });
                rawConfigurations.addAll(fileRawConfigurations);
            }
            readBytesHistogram.update(bytesCounter.get());

            return rawConfigurations;
        } finally {
            context.stop();
        }
    }

    private Map<String, MimeType> createMimeTypeByFilenamesMap() throws IOException {
        final Map<String, MimeType> mimeTypeByFilenamesMap = new HashMap<>();
        final Path rootPath = Path.of(rootDirectoryName);
        final AtomicInteger numberOfFilesCount = new AtomicInteger(0);
        // SEC-4 (CWE-59/CWE-200): no FileVisitOption.FOLLOW_LINKS -- do not descend into a
        // symlinked subdirectory, bounding the traversal to the rootPath tree itself. That alone
        // is not sufficient: Files.isRegularFile(Path) with no LinkOption follows a symlink to
        // test its TARGET, so a symlinked FILE directly inside the root (not a symlinked
        // directory) would still be treated as an ordinary file and read straight through to
        // whatever it points at, including something outside rootPath entirely. The explicit
        // isSymbolicLink exclusion below closes that: an entry that is itself a link is skipped
        // outright, never resolved, matching the same "reject the link, don't follow it" choice
        // already made for ImportApiResource.scan() (L4-07).
        Files.walk(rootPath)
                .filter(path -> !Files.isSymbolicLink(path))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    numberOfFilesCount.incrementAndGet();
                    final File file = path.toFile();
                    if (ignoreFilenames != null && ignoreFilenames.contains(file.getName())) {
                        return;
                    }

                    final String absolutePath = file.getAbsolutePath();
                    final MimeType mimeType = fileMimeTypeResolver.resolveMimeType(absolutePath);
                    if (mimeType != null && supportedMimeTypes.contains(mimeType)) {
                        mimeTypeByFilenamesMap.put(absolutePath, mimeType);
                    }
                });
        numberOfFilesReadHistogram.update(numberOfFilesCount.get());

        return mimeTypeByFilenamesMap;
    }

    /**
     * L2-08 (CWE-22): resolves {@code filename} to an absolute path confined to {@link
     * #rootDirectoryName}, rejecting anything that escapes it.
     *
     * <p>The previous check treated SUBSTRING CONTAINMENT ({@code
     * filename.contains(rootDirectoryName)}) as confinement. Two shapes defeated it: a {@code
     * "../"}-prefixed name escapes the root while never containing its text at all, and a
     * SIBLING directory whose name merely starts with the root's own name (e.g. {@code
     * "<root>-evil/out.yaml"}) textually contains the root string although it is not a child of
     * it -- {@link Path#startsWith(Path)} compares path COMPONENTS, not raw text, so it does not
     * share either mistake.
     *
     * @param filename the configuration name as supplied by the caller
     * @return the resolved, confined, absolute path as a string
     * @throws AppbaseRuntimeException if the resolved path is not confined to the root
     */
    private String resolveConfinedAbsoluteFilename(final String filename) {
        final Path rootPath = Path.of(rootDirectoryName).toAbsolutePath().normalize();
        final Path requestedPath = Path.of(filename);
        final Path candidatePath = (requestedPath.isAbsolute() ? requestedPath : rootPath.resolve(requestedPath))
                .normalize();

        if (!candidatePath.startsWith(rootPath)) {
            throw new AppbaseRuntimeException(
                    "Configuration file must be a child of root " + rootPath + ": " + filename);
        }

        return candidatePath.toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void writeRawConfiguration(
            final RawConfiguration rawConfiguration,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(rawConfiguration, "rawConfiguration");
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Timer.Context context = writeTimer.time();
        try {
            final String filename = rawConfiguration.getName();
            final MimeType mimeType = rawConfiguration.getMimeType();

            final String finalFilename = resolveConfinedAbsoluteFilename(filename);
            final FileConfigurationStore fileConfigurationStore = fileConfigurationStoreMap
                    .computeIfAbsent(
                            finalFilename,
                            k -> new FileConfigurationStore(
                                    Path.of(rootDirectoryName), finalFilename, mimeType));
            fileConfigurationStore.writeRawConfiguration(rawConfiguration, configurationAccessorContext);
            final byte[] value = rawConfiguration.getValue();
            if (value != null) {
                writeBytesHistogram.update(value.length);
            }
        } finally {
            context.stop();
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
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public void shutdown() throws AppbaseRuntimeException {
        this.shutdown.set(true);
    }

    public Gauge getFileConfigurationStoreMapSizeGauge() {
        return fileConfigurationStoreMapSizeGauge;
    }

    @Override
    public String toString() {
        return "DirectoryConfigurationStore{" +
                "id='" + id + '\'' +
                ", rootDirectoryName='" + rootDirectoryName + '\'' +
                ", supportedMimeTypes=" + supportedMimeTypes +
                ", ignoreFilenames=" + ignoreFilenames +
                ", fileMimeTypeResolver=" + fileMimeTypeResolver +
                ", fileConfigurationStoreMap=" + fileConfigurationStoreMap +
                '}';
    }
}
