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
package com.viveworks.appbase.client.service.elasticsearch.restclient.accessor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface SearchIndexAccessor<D, R> {
    void addDocument(D document) throws IOException;

    void updateDocument(D document) throws IOException;

    boolean deleteDocument(String id) throws IOException;

    List<D> search(R request) throws IOException;

    boolean createIndex() throws IOException;

    boolean existsIndex() throws IOException;

    boolean deleteIndex() throws IOException;

    CompletableFuture<Void> asyncAddDocument(D document);

    CompletableFuture<Void> asyncUpdateDocument(D document);

    CompletableFuture<Boolean> asyncDeleteDocument(String id);

    CompletableFuture<List<D>> asyncSearch(R request);

    CompletableFuture<Boolean> asyncCreateIndex();

    CompletableFuture<Boolean> asyncExistsIndex();

    CompletableFuture<Boolean> asyncDeleteIndex();
}
