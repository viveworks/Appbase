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

package com.viveworks.appbase.core.util.lucene;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultLuceneService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultLuceneServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultLuceneServiceConfiguration.Builder.class)
public final class DefaultLuceneServiceConfiguration implements LuceneServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultLuceneServiceConfiguration";

    private final boolean enabled;
    private final String indexBaseDirectory;
    private final DirectoryType directoryType;
    private final MmapDirectoryConfiguration mmapDirectoryConfiguration;
    private final boolean deleteIndexPathUponShutdown;
    private final boolean checkPendingFlushUpdate;
    private final boolean commitOnClose;
    private final int maxBufferedDocs;
    private final IndexWriterConfig.OpenMode openMode;
    private final double ramBufferSizeMB;
    private final int ramPerThreadHardLimitMB;
    private final boolean readerPooling;
    private final boolean useCompoundFile;
    private final LruQueryCacheConfiguration lruQueryCacheConfiguration;
    private final List<String> standardAnalyzerStopWords;
    private final int standardAnalyzerMaxTokenLength;
    private final boolean stopWordsIgnoreCase;
    private final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration;
    private final SchedulableConfiguration indexSynchronizerSchedulableConfiguration;

    private DefaultLuceneServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultLuceneServiceConfigurationValidator.validate(
                builder.enabled,
                builder.indexBaseDirectory,
                builder.directoryType,
                builder.mmapDirectoryConfiguration);

        enabled = builder.enabled;
        indexBaseDirectory = builder.indexBaseDirectory;
        directoryType = builder.directoryType;
        mmapDirectoryConfiguration = builder.mmapDirectoryConfiguration;
        deleteIndexPathUponShutdown = builder.deleteIndexPathUponShutdown;
        checkPendingFlushUpdate = builder.checkPendingFlushUpdate;
        commitOnClose = builder.commitOnClose;
        maxBufferedDocs = builder.maxBufferedDocs;
        openMode = builder.openMode;
        ramBufferSizeMB = builder.ramBufferSizeMB;
        ramPerThreadHardLimitMB = builder.ramPerThreadHardLimitMB;
        readerPooling = builder.readerPooling;
        useCompoundFile = builder.useCompoundFile;
        lruQueryCacheConfiguration = builder.lruQueryCacheConfiguration;
        timestampBasedIndexCleanerConfiguration = builder.timestampBasedIndexCleanerConfiguration;
        standardAnalyzerStopWords = builder.standardAnalyzerStopWords;
        standardAnalyzerMaxTokenLength = builder.standardAnalyzerMaxTokenLength;
        stopWordsIgnoreCase = builder.stopWordsIgnoreCase;
        indexSynchronizerSchedulableConfiguration = builder.indexSynchronizerSchedulableConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getIndexBaseDirectory() {
        return indexBaseDirectory;
    }

    @Override
    public DirectoryType getDirectoryType() {
        return directoryType;
    }

    @Override
    public MmapDirectoryConfiguration getMmapDirectoryConfiguration() {
        return mmapDirectoryConfiguration;
    }

    @Override
    public boolean isDeleteIndexPathUponShutdown() {
        return deleteIndexPathUponShutdown;
    }

    @Override
    public boolean isCheckPendingFlushUpdate() {
        return checkPendingFlushUpdate;
    }

    @Override
    public boolean isCommitOnClose() {
        return commitOnClose;
    }

    @Override
    public int getMaxBufferedDocs() {
        return maxBufferedDocs;
    }

    @Override
    public IndexWriterConfig.OpenMode getOpenMode() {
        return openMode;
    }

    @Override
    public double getRamBufferSizeMB() {
        return ramBufferSizeMB;
    }

    @Override
    public int getRamPerThreadHardLimitMB() {
        return ramPerThreadHardLimitMB;
    }

    @Override
    public boolean isReaderPooling() {
        return readerPooling;
    }

    @Override
    public boolean isUseCompoundFile() {
        return useCompoundFile;
    }

    @Override
    public LruQueryCacheConfiguration getLruQueryCacheConfiguration() {
        return lruQueryCacheConfiguration;
    }

    @Override
    public TimestampBasedIndexCleanerConfiguration getTimestampBasedIndexCleanerConfiguration() {
        return timestampBasedIndexCleanerConfiguration;
    }

    @Override
    public List<String> getStandardAnalyzerStopWords() {
        return standardAnalyzerStopWords;
    }

    @Override
    public int getStandardAnalyzerMaxTokenLength() {
        return standardAnalyzerMaxTokenLength;
    }

    public boolean isStopWordsIgnoreCase() {
        return stopWordsIgnoreCase;
    }

    public SchedulableConfiguration getIndexSynchronizerSchedulableConfiguration() {
        return indexSynchronizerSchedulableConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultLuceneServiceConfiguration)) {
            return false;
        }

        final DefaultLuceneServiceConfiguration that = (DefaultLuceneServiceConfiguration) o;
        return enabled == that.enabled &&
                deleteIndexPathUponShutdown == that.deleteIndexPathUponShutdown &&
                checkPendingFlushUpdate == that.checkPendingFlushUpdate &&
                commitOnClose == that.commitOnClose &&
                maxBufferedDocs == that.maxBufferedDocs &&
                Double.compare(that.ramBufferSizeMB, ramBufferSizeMB) == 0 &&
                ramPerThreadHardLimitMB == that.ramPerThreadHardLimitMB &&
                readerPooling == that.readerPooling &&
                useCompoundFile == that.useCompoundFile &&
                standardAnalyzerMaxTokenLength == that.standardAnalyzerMaxTokenLength &&
                Objects.equals(indexBaseDirectory, that.indexBaseDirectory) &&
                directoryType == that.directoryType &&
                Objects.equals(mmapDirectoryConfiguration, that.mmapDirectoryConfiguration) &&
                openMode == that.openMode &&
                Objects.equals(lruQueryCacheConfiguration, that.lruQueryCacheConfiguration) &&
                Objects.equals(standardAnalyzerStopWords, that.standardAnalyzerStopWords) &&
                stopWordsIgnoreCase == that.stopWordsIgnoreCase &&
                Objects.equals(timestampBasedIndexCleanerConfiguration, that.timestampBasedIndexCleanerConfiguration) &&
                Objects.equals(
                        indexSynchronizerSchedulableConfiguration,
                        that.indexSynchronizerSchedulableConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                indexBaseDirectory,
                directoryType,
                mmapDirectoryConfiguration,
                deleteIndexPathUponShutdown,
                checkPendingFlushUpdate,
                commitOnClose,
                maxBufferedDocs,
                openMode,
                ramBufferSizeMB,
                ramPerThreadHardLimitMB,
                readerPooling,
                useCompoundFile,
                lruQueryCacheConfiguration,
                standardAnalyzerStopWords,
                standardAnalyzerMaxTokenLength,
                stopWordsIgnoreCase,
                timestampBasedIndexCleanerConfiguration,
                indexSynchronizerSchedulableConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultLuceneServiceConfiguration{" +
                "enabled='" + enabled +
                ", indexBaseDirectory='" + indexBaseDirectory + '\'' +
                ", directoryType=" + directoryType +
                ", mmapDirectoryConfiguration=" + mmapDirectoryConfiguration +
                ", deleteIndexPathUponShutdown=" + deleteIndexPathUponShutdown +
                ", checkPendingFlushUpdate=" + checkPendingFlushUpdate +
                ", commitOnClose=" + commitOnClose +
                ", maxBufferedDocs=" + maxBufferedDocs +
                ", openMode=" + openMode +
                ", ramBufferSizeMB=" + ramBufferSizeMB +
                ", ramPerThreadHardLimitMB=" + ramPerThreadHardLimitMB +
                ", readerPooling=" + readerPooling +
                ", useCompoundFile=" + useCompoundFile +
                ", lruQueryCacheConfiguration=" + lruQueryCacheConfiguration +
                ", standardAnalyzerStopWords=" + standardAnalyzerStopWords +
                ", standardAnalyzerMaxTokenLength=" + standardAnalyzerMaxTokenLength +
                ", stopWordsIgnoreCase=" + stopWordsIgnoreCase +
                ", timestampBasedIndexCleanerConfiguration=" + timestampBasedIndexCleanerConfiguration +
                ", indexSynchronizerSchedulableConfiguration=" + indexSynchronizerSchedulableConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultLuceneServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private final boolean DEFAULT_ENABLED = true;
        private static final String DEFAULT_INDEX_BASE_DIRECTORY =
                System.getProperty("java.io.tmpdir") +
                        File.separator +
                        DefaultLuceneService.class.getSimpleName();
        private static final DirectoryType DEFAULT_DIRECTORY_TYPE = DirectoryType.NIO;
        private static final MmapDirectoryConfiguration DEFAULT_MMAP_DIRECTORY_CONFIGURATION =
                MmapDirectoryConfiguration.builder().build();
        private static final boolean DEFAULT_CHECK_PENDING_FLUSH_UPDATE = true;
        private static final boolean DEFAULT_DELETE_INDEX_PATH_UPON_SHUTDOWN = true;
        private static final boolean DEFAULT_COMMIT_ON_CLOSE = true;
        private static final int DEFAULT_MAX_BUFFERED_DOCS = 1024;
        private static final IndexWriterConfig.OpenMode DEFAULT_OPEN_MODE = IndexWriterConfig.OpenMode.CREATE_OR_APPEND;
        private static final double DEFAULT_RAM_BUFFER_SIZE_MB = 512;
        private static final int DEFAULT_RAM_PER_THREAD_HARD_LIMIT_MB = 128;
        private static final boolean DEFAULT_READER_POOLING = true;
        private static final boolean DEFAULT_USE_COMPOUND_FILE = true;
        private static final LruQueryCacheConfiguration DEFAULT_LRU_QUERY_CACHE_CONFIGURATION =
                LruQueryCacheConfiguration.builder().build();
        private static final TimestampBasedIndexCleanerConfiguration DEFAULT_DEFAULT_INDEX_CLEANER_CONFIGURATION =
                TimestampBasedIndexCleanerConfiguration.builder().build();
        private static final List<String> DEFAULT_STANDARD_ANALYZER_STOP_WORDS = List.of();
        private static final int DEFAULT_STANDARD_ANALYZER_MAX_TOKEN_LENGTH = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;
        private static final boolean DEFAULT_STOP_WORDS_IGNORE_CASE = true;
        private static final SchedulableConfiguration DEFAULT_INDEX_SYNCHRONIZER_SCHEDULABLE_CONFIGURATION =
                SchedulableConfiguration.builder().delay(30L).timeUnit(TimeUnit.SECONDS).build();

        private boolean enabled = DEFAULT_ENABLED;
        private String indexBaseDirectory = DEFAULT_INDEX_BASE_DIRECTORY + File.separator + UUID.randomUUID();
        private DirectoryType directoryType = DEFAULT_DIRECTORY_TYPE;
        private MmapDirectoryConfiguration mmapDirectoryConfiguration = DEFAULT_MMAP_DIRECTORY_CONFIGURATION;
        private boolean deleteIndexPathUponShutdown = DEFAULT_DELETE_INDEX_PATH_UPON_SHUTDOWN;
        private boolean checkPendingFlushUpdate = DEFAULT_CHECK_PENDING_FLUSH_UPDATE;
        private boolean commitOnClose = DEFAULT_COMMIT_ON_CLOSE;
        private int maxBufferedDocs = DEFAULT_MAX_BUFFERED_DOCS;
        private IndexWriterConfig.OpenMode openMode = DEFAULT_OPEN_MODE;
        private double ramBufferSizeMB = DEFAULT_RAM_BUFFER_SIZE_MB;
        private int ramPerThreadHardLimitMB = DEFAULT_RAM_PER_THREAD_HARD_LIMIT_MB;
        private boolean readerPooling = DEFAULT_READER_POOLING;
        private boolean useCompoundFile = DEFAULT_USE_COMPOUND_FILE;
        private LruQueryCacheConfiguration lruQueryCacheConfiguration = DEFAULT_LRU_QUERY_CACHE_CONFIGURATION;
        private TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration =
                DEFAULT_DEFAULT_INDEX_CLEANER_CONFIGURATION;
        private List<String> standardAnalyzerStopWords = DEFAULT_STANDARD_ANALYZER_STOP_WORDS;
        private int standardAnalyzerMaxTokenLength = DEFAULT_STANDARD_ANALYZER_MAX_TOKEN_LENGTH;
        private boolean stopWordsIgnoreCase = DEFAULT_STOP_WORDS_IGNORE_CASE;
        private SchedulableConfiguration indexSynchronizerSchedulableConfiguration =
                DEFAULT_INDEX_SYNCHRONIZER_SCHEDULABLE_CONFIGURATION;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder indexBaseDirectory(final String indexBaseDirectory) {
            this.indexBaseDirectory = indexBaseDirectory;
            return this;
        }

        public Builder directoryType(final DirectoryType directoryType) {
            this.directoryType = directoryType;
            return this;
        }

        public Builder mmapDirectoryConfiguration(final MmapDirectoryConfiguration mmapDirectoryConfiguration) {
            this.mmapDirectoryConfiguration = mmapDirectoryConfiguration;
            return this;
        }

        public Builder deleteIndexPathUponShutdown(final boolean deleteIndexPathUponShutdown) {
            this.deleteIndexPathUponShutdown = deleteIndexPathUponShutdown;
            return this;
        }

        public Builder checkPendingFlushUpdate(final boolean checkPendingFlushUpdate) {
            this.checkPendingFlushUpdate = checkPendingFlushUpdate;
            return this;
        }

        public Builder commitOnClose(final boolean commitOnClose) {
            this.commitOnClose = commitOnClose;
            return this;
        }

        public Builder maxBufferedDocs(final int maxBufferedDocs) {
            this.maxBufferedDocs = maxBufferedDocs;
            return this;
        }

        public Builder openMode(final IndexWriterConfig.OpenMode openMode) {
            this.openMode = openMode;
            return this;
        }

        public Builder ramBufferSizeMB(final double ramBufferSizeMB) {
            this.ramBufferSizeMB = ramBufferSizeMB;
            return this;
        }

        public Builder ramPerThreadHardLimitMB(final int ramPerThreadHardLimitMB) {
            this.ramPerThreadHardLimitMB = ramPerThreadHardLimitMB;
            return this;
        }

        public Builder readerPooling(final boolean readerPooling) {
            this.readerPooling = readerPooling;
            return this;
        }

        public Builder useCompoundFile(final boolean useCompoundFile) {
            this.useCompoundFile = useCompoundFile;
            return this;
        }

        public Builder defaultIndexCleanerConfiguration(
                final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration)
        {
            this.timestampBasedIndexCleanerConfiguration = timestampBasedIndexCleanerConfiguration;
            return this;
        }

        public Builder lruQueryCacheConfiguration(final LruQueryCacheConfiguration lruQueryCacheConfiguration) {
            this.lruQueryCacheConfiguration = lruQueryCacheConfiguration;
            return this;
        }

        public Builder standardAnalyzerStopWords(final List<String> standardAnalyzerStopWords) {
            this.standardAnalyzerStopWords = standardAnalyzerStopWords;
            return this;
        }

        public Builder standardAnalyzerMaxTokenLength(final int standardAnalyzerMaxTokenLength) {
            this.standardAnalyzerMaxTokenLength = standardAnalyzerMaxTokenLength;
            return this;
        }

        public Builder stopWordsIgnoreCase(final boolean stopWordsIgnoreCase) {
            this.stopWordsIgnoreCase = stopWordsIgnoreCase;
            return this;
        }

        public Builder indexSynchronizerSchedulableConfiguration(
                SchedulableConfiguration indexSynchronizerSchedulableConfiguration)
        {
            this.indexSynchronizerSchedulableConfiguration = indexSynchronizerSchedulableConfiguration;
            return this;
        }

        public DefaultLuceneServiceConfiguration build() {
            return new DefaultLuceneServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultLuceneServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultLuceneServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.lucene." +
                "DefaultLuceneServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
