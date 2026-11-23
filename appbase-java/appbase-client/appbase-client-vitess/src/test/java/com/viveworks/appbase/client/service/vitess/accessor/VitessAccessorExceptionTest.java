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

package com.viveworks.appbase.client.service.vitess.accessor;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link VitessAccessorException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class VitessAccessorExceptionTest {

    @Test
    void mapsBareSqlExceptionToVitessAccessorException() {
        final SQLException sqlException = new SQLException("boom");
        final Throwable mapped = VitessAccessorException.map(
                sqlException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(VitessAccessorException.class, mapped);
        assertSame(sqlException, mapped.getCause());
    }

    @Test
    void unwrapsCompletionExceptionWrappingSqlException() {
        final SQLException sqlException = new SQLException("x");
        final CompletionException completionException = new CompletionException(sqlException);
        final Throwable mapped = VitessAccessorException.map(
                completionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(VitessAccessorException.class, mapped);
        assertSame(sqlException, mapped.getCause());
    }

    @Test
    void unwrapsExecutionExceptionWrappingSqlException() {
        final SQLException sqlException = new SQLException("y");
        final ExecutionException executionException = new ExecutionException(sqlException);
        final Throwable mapped = VitessAccessorException.map(
                executionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(VitessAccessorException.class, mapped);
        assertSame(sqlException, mapped.getCause());
    }

    @Test
    void delegatesNonSqlThrowableToFallbackUnchanged() {
        final RuntimeException foreign = new RuntimeException("not sql");
        final Function<Throwable, Throwable> identity = Function.identity();
        final Throwable mapped = VitessAccessorException.map(foreign, identity);
        assertSame(foreign, mapped);
    }
}
