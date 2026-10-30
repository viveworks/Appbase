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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive;

import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.DefaultElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.core.AppbaseCore;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Offline regression coverage for the live Elasticsearch fixture cleanup, folded from PR internal work item 2485.
 *
 * @author Navaneeth Mysore G
 */
class ReactiveElasticsearchAccessorCleanupTest {
    @Test
    void failuresDoNotPreventLaterCleanupAndRemainObservable() {
        final ElasticsearchRestClient client = mock(ElasticsearchRestClient.class);
        final DefaultElasticsearchLowLevelRestClientService service =
                mock(DefaultElasticsearchLowLevelRestClientService.class);
        final AppbaseCore core = mock(AppbaseCore.class);
        final RuntimeException first = new IllegalStateException("reactive delete failed");
        final RuntimeException shutdown = new IllegalStateException("client shutdown failed");
        when(client.asyncDeleteIndex(new DeleteIndexRequest("reactive")))
                .thenReturn(CompletableFuture.failedFuture(first));
        when(client.asyncDeleteIndex(new DeleteIndexRequest("sync")))
                .thenReturn(CompletableFuture.completedFuture(new DeleteIndexResponse(true, true)));
        doThrow(shutdown).when(client).shutdown();

        final Throwable failure = Assertions.assertThrows(Throwable.class,
                () -> ReactiveElasticsearchAccessorIT.cleanup(client, service, core, "reactive", "sync"));

        Assertions.assertAll(
                () -> verify(client).asyncDeleteIndex(new DeleteIndexRequest("reactive")),
                () -> verify(client).asyncDeleteIndex(new DeleteIndexRequest("sync")),
                () -> verify(client).shutdown(),
                () -> verify(service).shutdown(),
                () -> verify(core).shutdown(),
                () -> Assertions.assertTrue(contains(failure, first)),
                () -> Assertions.assertTrue(contains(failure, shutdown)));
    }

    private static boolean contains(final Throwable actual, final Throwable expected) {
        if (actual == expected) return true;
        if (actual.getCause() != null && contains(actual.getCause(), expected)) return true;
        for (Throwable suppressed : actual.getSuppressed()) {
            if (contains(suppressed, expected)) return true;
        }
        return false;
    }
}
