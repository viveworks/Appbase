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

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A configuration store that reads / writes configurations from / to a file in classpathFile.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class ClasspathConfigurationStore implements ConfigurationStore {
    private final String id;
    private final String classpathFile;
    private final MimeType mimeType;
    private final AtomicBoolean shutdown;

    public ClasspathConfigurationStore(final String classpathFile,
                                       final MimeType mimeType)
    {
        AppbaseUtil.notBlank(classpathFile, "classpathFile");
        AppbaseUtil.notNull(mimeType, "mimeType");

        this.id = AppbaseUtil.configurationStoreIdPrefix(this) + classpathFile;
        this.classpathFile = classpathFile;
        this.mimeType = mimeType;
        this.shutdown = new AtomicBoolean(false);
    }

    public ClasspathConfigurationStore(final String baseClasspathFilename,
                                       final EnvironmentResolver environmentResolver,
                                       final FileMimeTypeResolver fileMimeTypeResolver,
                                       final TargetResolver targetResolver)
    {
        AppbaseUtil.notNull(baseClasspathFilename, "baseClasspathFilename");
        AppbaseUtil.notNull(environmentResolver, "environmentResolver");
        AppbaseUtil.notNull(fileMimeTypeResolver, "fileMimeTypeResolver");

        final String strippedBaseClasspathFilename = AppbaseUtil.stripClasspathPrefix(baseClasspathFilename);
        final String environment = environmentResolver.resolveEnvironment();
        classpathFile = targetResolver.resolveTarget(strippedBaseClasspathFilename, environment);

        this.id = AppbaseUtil.configurationStoreIdPrefix(this) + classpathFile;

        this.mimeType = fileMimeTypeResolver.resolveMimeType(strippedBaseClasspathFilename);
        if (mimeType == null) {
            throw new AppbaseRuntimeException(
                    "Cannot resolve MIME type for base file name " + strippedBaseClasspathFilename);
        }
        this.shutdown = new AtomicBoolean(false);
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

        final String strippedClasspathFile = AppbaseUtil.stripClasspathPrefix(classpathFile);
        final Collection<RawConfiguration> rawConfigurations = new ArrayList<>();
        final byte[] bytes;

        InputStream resourceAsStream = getClass().getResourceAsStream(strippedClasspathFile);
        if (resourceAsStream == null) {
            resourceAsStream = getClass().getClassLoader().getResourceAsStream(strippedClasspathFile);
        }

        if (resourceAsStream == null) {
            throw new ConfigurationStoreException("Cannot find file " + strippedClasspathFile + " in classpath");
        }

        try {
            bytes = resourceAsStream.readAllBytes();
        } catch (IOException ioe) {
            throw new ConfigurationStoreException(
                    "Error while reading file " + strippedClasspathFile + " from classpath", ioe);
        } finally {
            try {
                resourceAsStream.close();
            } catch (IOException ioe) {
                // no-op
            }
        }

        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name(classpathFile)
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
        throw new ConfigurationStoreException("Cannot write files to classpath");
    }

    @Override
    public boolean supportsWrite() {
        return false;
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        shutdown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public String toString() {
        return "ClasspathConfigurationStore{" +
                "id='" + id + '\'' +
                "classpathFile='" + classpathFile + '\'' +
                ", mimeType=" + mimeType +
                '}';
    }
}
