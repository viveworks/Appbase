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

package com.viveworks.appbase.client.service.jasync;

import com.github.jasync.sql.db.exceptions.DatabaseException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link JasyncAccessorException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class JasyncAccessorExceptionTest {

    @Test
    void mapsBareDatabaseExceptionToJasyncAccessorException() {
        final DatabaseException databaseException = new DatabaseException("boom");
        final Throwable mapped = JasyncAccessorException.map(
                databaseException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(JasyncAccessorException.class, mapped);
        assertSame(databaseException, mapped.getCause());
    }

    @Test
    void unwrapsCompletionExceptionWrappingDatabaseException() {
        final DatabaseException databaseException = new DatabaseException("boom");
        final CompletionException completionException = new CompletionException(databaseException);
        final Throwable mapped = JasyncAccessorException.map(
                completionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(JasyncAccessorException.class, mapped);
        assertSame(databaseException, mapped.getCause());
    }

    @Test
    void unwrapsExecutionExceptionWrappingDatabaseException() {
        final DatabaseException databaseException = new DatabaseException("boom");
        final ExecutionException executionException = new ExecutionException(databaseException);
        final Throwable mapped = JasyncAccessorException.map(
                executionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(JasyncAccessorException.class, mapped);
        assertSame(databaseException, mapped.getCause());
    }

    @Test
    void delegatesNonDatabaseThrowableToFallbackWithUnwrappedCause() {
        final RuntimeException foreign = new RuntimeException("x");
        final CompletionException completionException = new CompletionException(foreign);
        final IllegalStateException fallbackResult = new IllegalStateException("fallback");
        final Throwable mapped = JasyncAccessorException.map(
                completionException, throwable -> {
                    assertSame(foreign, throwable);
                    return fallbackResult;
                });
        assertSame(fallbackResult, mapped);
    }
}
