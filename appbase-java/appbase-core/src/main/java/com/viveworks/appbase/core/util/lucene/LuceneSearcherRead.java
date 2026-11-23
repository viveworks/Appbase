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

import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;

/**
 * A blocking Lucene read against a single, point-in-time-consistent {@link IndexSearcher} handed to it by
 * {@link LuceneService#withSearcher(LuceneSearcherRead)}, which acquires the searcher before the call and
 * releases it after, regardless of outcome.
 *
 * <p>When a read needs both a search and a subsequent stored-document lookup (e.g. search then fetch the
 * matched documents by id), perform both against the same {@code searcher} — and its
 * {@link IndexSearcher#getIndexReader()} — inside one {@code read} call, rather than issuing them as two
 * separate {@link LuceneService#withSearcher(LuceneSearcherRead)} invocations. Two separate invocations may
 * acquire different index generations if a refresh lands between them, so document ids from one would not
 * be valid against the other.
 *
 * @author Claude
 * @param <T> Type of value read
 * @since 1.0.0
 */
@FunctionalInterface
public interface LuceneSearcherRead<T> {
    /**
     * Perform a blocking read against {@code searcher}.
     *
     * @param searcher Acquired, point-in-time-consistent searcher
     * @return the value read
     * @throws IOException if the read fails
     */
    T read(IndexSearcher searcher) throws IOException;
}
