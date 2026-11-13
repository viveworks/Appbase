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

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.reactive.read.CancellableRead;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.QueryBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A service for <code>Apache Lucene</code> index.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface LuceneService extends Shutdownable {
    /**
     * Get the index path on the filesystem.
     *
     * @return Index path
     */
    Path getIndexPath();


    /**
     * Get the index {@link Directory}.
     *
     * @return Index directory
     */
    Directory getDirectory();


    /**
     * Get the {@link IndexWriter}.
     *
     * @return Index writer
     * @throws LuceneServiceShutdownException if shutdown has completed
     */
    IndexWriter getIndexWriter();


    /**
     * Get the {@link IndexWriter}.
     *
     * @param commit Commit index writer before returning
     * @return Index writer
     * @throws LuceneServiceException on any errors
     * @throws LuceneServiceShutdownException if shutdown has completed
     */
    IndexWriter getIndexWriter(boolean commit) throws LuceneServiceException;


    /**
     * Synchronize index. This commits the {@link IndexWriter} and resets {@link DirectoryReader} and
     * {@link IndexSearcher}. This is usually called after adding documents to the index, so the readers can get the
     * latest indexed documents.
     *
     * @throws LuceneServiceException on any errors
     */
    void synchronizeIndex() throws LuceneServiceException;


    /**
     * Acquire a point-in-time-consistent {@link IndexSearcher} for the duration of {@code read}, and
     * release it afterward regardless of outcome.
     *
     * <p>Replaces the former {@code getDirectoryReader()} / {@code getIndexSearcher()} accessors, which
     * handed out a live reference with no release signal: a concurrent {@link #synchronizeIndex()} could
     * then close the exact reader a caller was still searching, throwing
     * {@link org.apache.lucene.store.AlreadyClosedException}. When a read needs both a search and a
     * stored-document lookup, do both inside one {@code withSearcher} call (via
     * {@link IndexSearcher#getIndexReader()}) rather than two separate calls — see
     * {@link LuceneSearcherRead}.
     *
     * @param read the search/read to run against the acquired searcher
     * @param <T> result type
     * @return whatever {@code read} returns
     * @throws IOException if acquiring the searcher, or {@code read} itself, fails
     * @throws LuceneServiceShutdownException if shutdown has completed
     */
    <T> T withSearcher(LuceneSearcherRead<T> read) throws IOException;


    /**
     * Get the {@link QueryBuilder}.
     *
     * @return Query builder
     */
    QueryBuilder getQueryBuilder();


    /**
     * Get the {@link QueryParser} for the query, configured with a default {@link org.apache.lucene.analysis.Analyzer}.
     *
     * @param fieldName Field name
     * @return Query parser
     */
    QueryParser getQueryParser(String fieldName);

    /**
     * Reactive counterpart of {@code getIndexSearcher().search(query, maxHits)} — offloads the blocking
     * Lucene search onto the configured {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary}
     * rather than blocking the calling thread.
     *
     * @param query Query to search
     * @param maxHits Maximum number of hits to return
     * @return a {@link Mono} that emits the {@link TopDocs} result, or errors — see
     *      {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary#read(java.util.concurrent.Callable)}
     */
    Mono<TopDocs> searchReactive(Query query, int maxHits);

    /**
     * Reactive counterpart of fetching a stored {@link Document} by id — offloads the blocking Lucene
     * document fetch onto the configured {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary}
     * rather than blocking the calling thread.
     *
     * @param docId Lucene internal document id
     * @return a {@link Mono} that emits the stored {@link Document}, or errors — see
     *      {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary#read(java.util.concurrent.Callable)}
     */
    Mono<Document> documentReactive(int docId);

    /**
     * Offload an arbitrary, cooperatively-cancellable, blocking Lucene read (e.g. a composed search-then-fetch)
     * onto the configured {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary}.
     *
     * @param cancellableRead Blocking, cooperatively-cancellable read to offload
     * @param <T> Type of value read
     * @return a {@link Mono} that emits the read result, or errors — see
     *      {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary#read(CancellableRead)}
     * @throws LuceneServiceShutdownException through the returned {@link Mono} if the supplied read accesses a
     *      guarded Lucene resource after {@link #shutdown()} has returned. Those resource guards deliberately
     *      strengthen the general {@link Shutdownable} convention.
     */
    <T> Mono<T> readReactive(CancellableRead<T> cancellableRead);
}
