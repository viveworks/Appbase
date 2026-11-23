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

package com.viveworks.appbase.client.service.zookeeper.accessor;

import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ZooKeeperAccessorException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ZooKeeperAccessorExceptionTest {

    @Test
    void mapsBareKeeperExceptionToZooKeeperAccessorException() {
        final KeeperException keeperException = KeeperException.create(KeeperException.Code.NONODE);
        final Throwable mapped = ZooKeeperAccessorException.map(
                keeperException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(ZooKeeperAccessorException.class, mapped);
        assertSame(keeperException, mapped.getCause());
    }

    @Test
    void unwrapsCompletionExceptionWrappingKeeperException() {
        final KeeperException keeperException = KeeperException.create(KeeperException.Code.NONODE);
        final CompletionException completionException = new CompletionException(keeperException);
        final Throwable mapped = ZooKeeperAccessorException.map(
                completionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(ZooKeeperAccessorException.class, mapped);
        assertSame(keeperException, mapped.getCause());
    }

    @Test
    void unwrapsExecutionExceptionWrappingKeeperException() {
        final KeeperException keeperException = KeeperException.create(KeeperException.Code.NONODE);
        final ExecutionException executionException = new ExecutionException(keeperException);
        final Throwable mapped = ZooKeeperAccessorException.map(
                executionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(ZooKeeperAccessorException.class, mapped);
        assertSame(keeperException, mapped.getCause());
    }

    @Test
    void delegatesNonKeeperThrowableToFallbackWithUnwrappedCause() {
        final RuntimeException foreign = new RuntimeException("x");
        final CompletionException completionException = new CompletionException(foreign);
        final IllegalStateException fallbackResult = new IllegalStateException("fallback");
        final Throwable mapped = ZooKeeperAccessorException.map(
                completionException, throwable -> {
                    assertSame(foreign, throwable);
                    return fallbackResult;
                });
        assertSame(fallbackResult, mapped);
    }
}
