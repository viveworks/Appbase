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

package com.viveworks.appbase.core.util.lucene;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import org.apache.lucene.index.IndexWriterConfig;

import java.util.List;

/**
 * Represents {@link LuceneService} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface LuceneServiceConfiguration extends AppbaseConfiguration {
    /**
     * Check if {@link LuceneService} is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();


    /**
     * Get the index's base directory.
     *
     * @return Index's base directory
     */
    String getIndexBaseDirectory();


    /**
     * Get the {@link DirectoryType}.
     *
     * @return Directory type
     */
    DirectoryType getDirectoryType();


    /**
     * {@link org.apache.lucene.store.MMapDirectory} configuration, if {@link #getDirectoryType()} is
     * {@link DirectoryType#MMAP}.
     *
     * @return MMAP directory configuration
     */
    MmapDirectoryConfiguration getMmapDirectoryConfiguration();


    /**
     * Check if index must be deleted before shutdown.
     *
     * @return Delete the index or not
     */
    boolean isDeleteIndexPathUponShutdown();


    /**
     * Check if pending updates must be flushed.
     *
     * @return Flush updates or not
     */
    boolean isCheckPendingFlushUpdate();


    /**
     * Check if index must be committed before close.
     *
     * @return Commit or not
     */
    boolean isCommitOnClose();


    /**
     * Get the max buffered documents.
     *
     * @return Max buffered documents
     */
    int getMaxBufferedDocs();


    /**
     * Get the {@link org.apache.lucene.index.IndexWriterConfig.OpenMode}.
     *
     * @return Open mode
     */
    IndexWriterConfig.OpenMode getOpenMode();


    /**
     * Get the max RAM buffer to use (in MB).
     *
     * @return RAM buffer size
     */
    double getRamBufferSizeMB();


    /**
     * Get the max RAM buffer to use per thread (in MB).
     *
     * @return RAM buffer size per thread
     */
    int getRamPerThreadHardLimitMB();


    /**
     * Check if reader pooling is enabled.
     *
     * @return Reader pooling enabled or not
     */
    boolean isReaderPooling();


    /**
     * Check if compound file must be used.
     *
     * @return Use compound file or not
     */
    boolean isUseCompoundFile();


    /**
     * Get the {@link LruQueryCacheConfiguration} used by {@link org.apache.lucene.search.IndexSearcher}.
     *
     * @return LruQueryCache configuration
     */
    LruQueryCacheConfiguration getLruQueryCacheConfiguration();


    /**
     * Get the {@link TimestampBasedIndexCleanerConfiguration} used to clean the index.
     *
     * @return TimestampBasedIndexCleanerConfiguration
     */
    TimestampBasedIndexCleanerConfiguration getTimestampBasedIndexCleanerConfiguration();


    /**
     * Get the list of stop words used by {@link org.apache.lucene.analysis.standard.StandardAnalyzer}.
     *
     * @return List of stop words
     */
    List<String> getStandardAnalyzerStopWords();


    /**
     * Get the max token length used by {@link org.apache.lucene.analysis.standard.StandardAnalyzer}.
     *
     * @return Max token length
     */
    int getStandardAnalyzerMaxTokenLength();
}
