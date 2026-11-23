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

package com.viveworks.appbase.client.service.r2dbc.accessor;

import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link R2dbcAccessorException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class R2dbcAccessorExceptionTest {

    @Test
    void mapsBareR2dbcExceptionToR2dbcAccessorException() {
        final R2dbcException r2dbcException = new R2dbcBadGrammarException("bad sql");

        final Throwable mapped = R2dbcAccessorException.map(r2dbcException, Function.identity());

        assertTrue(mapped instanceof R2dbcAccessorException);
        assertSame(r2dbcException, mapped.getCause());
    }

    @Test
    void unwrapsCompletionExceptionAroundR2dbcException() {
        final R2dbcException r2dbcException = new R2dbcBadGrammarException("bad sql");
        final CompletionException completionException = new CompletionException(r2dbcException);

        final Throwable mapped = R2dbcAccessorException.map(completionException, Function.identity());

        assertTrue(mapped instanceof R2dbcAccessorException);
        assertSame(r2dbcException, mapped.getCause());
    }

    @Test
    void unwrapsExecutionExceptionAroundR2dbcException() {
        final R2dbcException r2dbcException = new R2dbcBadGrammarException("bad sql");
        final ExecutionException executionException = new ExecutionException(r2dbcException);

        final Throwable mapped = R2dbcAccessorException.map(executionException, Function.identity());

        assertTrue(mapped instanceof R2dbcAccessorException);
        assertSame(r2dbcException, mapped.getCause());
    }

    @Test
    void nonR2dbcThrowableDelegatesToFallback() {
        final RuntimeException runtimeException = new RuntimeException("not r2dbc");

        final Throwable mapped = R2dbcAccessorException.map(runtimeException, Function.identity());

        assertSame(runtimeException, mapped);
    }
}
