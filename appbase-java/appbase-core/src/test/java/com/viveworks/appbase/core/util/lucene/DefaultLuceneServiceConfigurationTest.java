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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.apache.lucene.index.IndexWriterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLuceneServiceConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration =
                DefaultLuceneServiceConfiguration
                .builder()
                .defaultIndexCleanerConfiguration(TimestampBasedIndexCleanerConfiguration.builder().build())
                .mmapDirectoryConfiguration(MmapDirectoryConfiguration.builder().build())
                .lruQueryCacheConfiguration(LruQueryCacheConfiguration.builder().build())
                .checkPendingFlushUpdate(true)
                .commitOnClose(true)
                .deleteIndexPathUponShutdown(true)
                .directoryType(DirectoryType.NIO)
                .indexBaseDirectory(System.getProperty("java.io.tmpdir"))
                .maxBufferedDocs(100)
                .openMode(IndexWriterConfig.OpenMode.CREATE)
                .ramBufferSizeMB(256)
                .ramPerThreadHardLimitMB(64)
                .readerPooling(true)
                .standardAnalyzerMaxTokenLength(20)
                .standardAnalyzerStopWords(List.of())
                .stopWordsIgnoreCase(true)
                .useCompoundFile(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultLuceneServiceConfiguration.class)
                .writeValueAsString(defaultLuceneServiceConfiguration);

        final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration1 = yamlMapper
                .readerFor(DefaultLuceneServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getDirectoryType(),
                defaultLuceneServiceConfiguration1.getDirectoryType());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getIndexBaseDirectory(),
                defaultLuceneServiceConfiguration1.getIndexBaseDirectory());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getLruQueryCacheConfiguration(),
                defaultLuceneServiceConfiguration1.getLruQueryCacheConfiguration());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getMaxBufferedDocs(),
                defaultLuceneServiceConfiguration1.getMaxBufferedDocs());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getMmapDirectoryConfiguration(),
                defaultLuceneServiceConfiguration1.getMmapDirectoryConfiguration());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getOpenMode(),
                defaultLuceneServiceConfiguration1.getOpenMode());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getRamBufferSizeMB(),
                defaultLuceneServiceConfiguration1.getRamBufferSizeMB());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getRamPerThreadHardLimitMB(),
                defaultLuceneServiceConfiguration1.getRamPerThreadHardLimitMB());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getStandardAnalyzerStopWords(),
                defaultLuceneServiceConfiguration1.getStandardAnalyzerStopWords());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.isStopWordsIgnoreCase(),
                defaultLuceneServiceConfiguration1.isStopWordsIgnoreCase());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.getTimestampBasedIndexCleanerConfiguration(),
                defaultLuceneServiceConfiguration1.getTimestampBasedIndexCleanerConfiguration());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.isCheckPendingFlushUpdate(),
                defaultLuceneServiceConfiguration1.isCheckPendingFlushUpdate());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.isCommitOnClose(),
                defaultLuceneServiceConfiguration1.isCommitOnClose());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.isDeleteIndexPathUponShutdown(),
                defaultLuceneServiceConfiguration1.isDeleteIndexPathUponShutdown());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.isReaderPooling(),
                defaultLuceneServiceConfiguration1.isReaderPooling());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.isUseCompoundFile(),
                defaultLuceneServiceConfiguration1.isUseCompoundFile());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration,
                defaultLuceneServiceConfiguration1);
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.hashCode(),
                defaultLuceneServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultLuceneServiceConfiguration.toString(),
                defaultLuceneServiceConfiguration1.toString());
    }
}
