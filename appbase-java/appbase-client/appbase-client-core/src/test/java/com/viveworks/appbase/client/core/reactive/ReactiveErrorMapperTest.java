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

package com.viveworks.appbase.client.core.reactive;

import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ReactiveErrorMapper}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveErrorMapperTest {

    @Test
    void appbaseRuntimeExceptionPassesThroughUnchanged() {
        final AppbaseRuntimeException original = new AppbaseRuntimeException("boom");
        assertSame(original, ReactiveErrorMapper.map(original));
    }

    @Test
    void appbaseExceptionPassesThroughUnchanged() {
        final AppbaseClientException original = new AppbaseClientException("boom");
        assertSame(original, ReactiveErrorMapper.map(original));
    }

    @Test
    void foreignExceptionWrappedInReactiveOperationException() {
        final Throwable mapped = ReactiveErrorMapper.map(new TimeoutException("slow"));
        assertInstanceOf(ReactiveOperationException.class, mapped);
        assertInstanceOf(TimeoutException.class, mapped.getCause());
    }

    @Test
    void foreignExceptionWithNullMessageFallsBackToClassName() {
        final Throwable mapped = ReactiveErrorMapper.map(new TimeoutException());
        assertInstanceOf(ReactiveOperationException.class, mapped);
        assertEquals("Reactive operation failed: " + TimeoutException.class.getName(), mapped.getMessage());
    }
}
