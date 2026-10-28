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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import org.elasticsearch.client.ResponseException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ElasticsearchAccessorException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ElasticsearchAccessorExceptionTest {

    @Test
    void mapsBareResponseExceptionToElasticsearchAccessorException() {
        final ResponseException responseException = Mockito.mock(ResponseException.class);
        final Throwable mapped = ElasticsearchAccessorException.map(
                responseException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(ElasticsearchAccessorException.class, mapped);
        assertSame(responseException, mapped.getCause());
    }

    @Test
    void unwrapsCompletionExceptionWrappingResponseException() {
        final ResponseException responseException = Mockito.mock(ResponseException.class);
        final CompletionException completionException = new CompletionException(responseException);
        final Throwable mapped = ElasticsearchAccessorException.map(
                completionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(ElasticsearchAccessorException.class, mapped);
        assertSame(responseException, mapped.getCause());
    }

    @Test
    void unwrapsExecutionExceptionWrappingResponseException() {
        final ResponseException responseException = Mockito.mock(ResponseException.class);
        final ExecutionException executionException = new ExecutionException(responseException);
        final Throwable mapped = ElasticsearchAccessorException.map(
                executionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(ElasticsearchAccessorException.class, mapped);
        assertSame(responseException, mapped.getCause());
    }

    @Test
    void delegatesNonResponseThrowableToFallbackWithUnwrappedCause() {
        final RuntimeException foreign = new RuntimeException("x");
        final CompletionException completionException = new CompletionException(foreign);
        final IllegalStateException fallbackResult = new IllegalStateException("fallback");
        final Throwable mapped = ElasticsearchAccessorException.map(
                completionException, throwable -> {
                    assertSame(foreign, throwable);
                    return fallbackResult;
                });
        assertSame(fallbackResult, mapped);
    }
}
