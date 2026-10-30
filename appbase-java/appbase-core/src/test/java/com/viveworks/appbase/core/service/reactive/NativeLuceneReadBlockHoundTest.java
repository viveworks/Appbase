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

package com.viveworks.appbase.core.service.reactive;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.TimestampBasedIndexCleanerConfiguration;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The direct BlockHound proof that the <b>native Lucene reactive read path</b> does not block a
 * non-blocking (event-loop-class) thread — the internal work item 389 epic "Done when" that {@link CoreBlockHoundTest}'s
 * armed-harness alone leaves as inference. {@code CoreBlockHoundTest} proves only that BlockHound is
 * installed and distinguishes a non-blocking scheduler from {@code boundedElastic}; it never runs a
 * real Lucene read. This class does.
 *
 * <p>Both native Lucene read stores — {@code ReactiveLuceneLogStore} ({@code /log}) and
 * {@code ReactiveLuceneExceptionStore} ({@code /exception}) — route every read through
 * {@link com.viveworks.appbase.core.util.lucene.LuceneService#readReactive(
 * com.viveworks.appbase.core.service.reactive.read.CancellableRead)}, i.e. the N01
 * {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary}. Proving the offload
 * at that shared chokepoint proves the property for both endpoints, in the core module, offline.
 *
 * <p><b>Scope of this proof, and what is deferred.</b> This is the core-module read-chain proof. The
 * full HTTP end-to-end proof — a reactor-netty request driven through {@code NativeLogEndpoint}/
 * {@code NativeExceptionEndpoint} under BlockHound — is deferred to the reactor-netty integration
 * tests / {@code appbase-stacktest}, which cannot execute in the offline core build (the
 * {@code appbase-bootstrap-reactor-netty} test run needs an aws-s3 SNAPSHOT jar and the
 * maven-jar-plugin archiver, neither available offline here). The endpoints read via the same
 * reactive accessor this test exercises, so the deferred proof adds the transport, not the read
 * semantics.
 *
 * <p><b>Why the assertion is not vacuous.</b> The fixture's index is NIO-backed ({@code NIOFSDirectory}
 * — the {@code DirectoryType.NIO} default), whose reads go through {@code FileChannel.read}, which
 * BlockHound's default instrumentation does <b>not</b> flag (it flags {@code RandomAccessFile} /
 * {@code FileInputStream} reads, not positional {@code FileChannel} reads). So the Lucene read is not
 * itself a BlockHound-flagged call — a naive "the read raised no BlockHound error" assertion could pass
 * even with the offload removed. The teeth here are the <i>thread-affinity</i> assertion: the read's
 * terminal signal is emitted on a {@code boundedElastic} offload thread, never the {@code parallel}
 * subscriber thread. Remove the offload (run the blocking work inline in the boundary's
 * {@code Mono.create} consumer) and the read emits on the {@code parallel} subscriber thread, so this
 * test goes red — verified by mutation. BlockHound, armed here, is the belt to that brace: any
 * genuinely-blocking JDK call on the read path, run inline on the {@code parallel} thread, additionally
 * fails the read.
 *
 * <p>Runs in the module's isolated {@code blockhound-test} surefire execution (this class matches
 * {@code **}{@code /*BlockHoundTest.java}), which supplies {@code BlockHound.install()}'s required JVM
 * flags and keeps the JVM-global instrumentation out of the order-sensitive default fork.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class NativeLuceneReadBlockHoundTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);

    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    private DefaultLuceneService luceneService;

    @BeforeAll
    static void setUpAll() {
        // Requires -XX:+AllowRedefinitionToAddDeleteMethods (JDK 13+) and -XX:+EnableDynamicAgentLoading
        // (JDK 21+), supplied by the blockhound-test surefire execution's argLine. Without them install()
        // fails and this whole class is red — the signal that the harness is not armed.
        BlockHound.install();

        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDownAll() {
        if (mediatorService != null) {
            mediatorService.shutdown();
        }
        if (validationService != null) {
            validationService.shutdown();
        }
        if (metricService != null) {
            metricService.shutdown();
        }
        if (logService != null) {
            logService.shutdown();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        luceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                DefaultLuceneServiceConfiguration
                        .builder()
                        .defaultIndexCleanerConfiguration(
                                TimestampBasedIndexCleanerConfiguration.builder().enabled(false).build())
                        .build(),
                CoreTestFixtures.getReactiveReadBoundary());
        indexDocuments(luceneService, 3);
    }

    @AfterEach
    void tearDown() {
        if (luceneService != null) {
            luceneService.shutdown();
        }
    }

    private static void indexDocuments(final DefaultLuceneService service, final int count) throws Exception {
        final IndexWriter indexWriter = service.getIndexWriter();
        for (int i = 0; i < count; i++) {
            final Document document = new Document();
            document.add(new StringField("id", "doc-" + i, Field.Store.YES));
            document.add(new TextField("body", "hello world " + i, Field.Store.YES));
            indexWriter.addDocument(document);
        }
        service.synchronizeIndex();
    }

    @Test
    void nativeLuceneReactiveReadIsOffloadedOffTheNonBlockingSubscriberThread() {
        // "id" is a StringField (exact, not analyzed), so a TermQuery hits it directly — mirrors the
        // real read: a pageSize-bounded search composed with a stored-fields fetch, run as one
        // CancellableRead through LuceneService.readReactive (the N01 boundary).
        final Query idQuery = new TermQuery(new Term("id", "doc-1"));
        final AtomicReference<String> emitThreadName = new AtomicReference<>();

        final Document result = luceneService.readReactive(cancelled -> {
                    final TopDocs topDocs = luceneService.getIndexSearcher().search(idQuery, 1);
                    final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                    Assertions.assertTrue(scoreDocs.length > 0, "expected a hit for id=doc-1");
                    return luceneService.getDirectoryReader().storedFields().document(scoreDocs[0].doc);
                })
                .doOnNext(document -> emitThreadName.set(Thread.currentThread().getName()))
                // Subscribe on a non-blocking (parallel) scheduler thread — the class BlockHound treats
                // like an event loop. With the offload intact, the boundary dispatches the blocking Lucene
                // read to boundedElastic and this thread never blocks; strip the offload and the read runs
                // inline HERE, tripping BlockHound and/or emitting on this parallel thread (asserted below).
                .subscribeOn(Schedulers.parallel())
                .block(BLOCK_TIMEOUT);

        // A real read happened (guards against a vacuous pass that never touched Lucene).
        Assertions.assertNotNull(result, "the native reactive read must emit the stored document");
        Assertions.assertEquals("doc-1", result.get("id"));

        // The teeth: the blocking read was offloaded, so the terminal signal is emitted on a
        // boundedElastic offload thread — never the parallel subscriber thread it would run on inline.
        final String emitThread = emitThreadName.get();
        Assertions.assertNotNull(emitThread, "expected the read to emit a value and record its thread");
        Assertions.assertTrue(emitThread.contains("boundedElastic"),
                "the blocking Lucene read must be offloaded off the non-blocking subscriber thread; expected "
                        + "a boundedElastic offload thread but the read emitted on " + emitThread);
        Assertions.assertFalse(emitThread.contains("parallel"),
                "the blocking Lucene read must not run on the parallel (non-blocking) subscriber thread; it "
                        + "emitted on " + emitThread);
    }
}
