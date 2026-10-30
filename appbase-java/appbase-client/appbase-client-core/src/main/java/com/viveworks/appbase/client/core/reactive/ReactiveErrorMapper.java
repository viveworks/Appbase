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

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Maps a {@link Throwable} raised in a reactive pipeline to an Appbase exception:
 * Appbase exceptions pass through unchanged; everything else is wrapped in a
 * {@link ReactiveOperationException}, preserving the cause. Keeps error semantics
 * identical to the synchronous face.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ReactiveErrorMapper {

    private ReactiveErrorMapper() {
    }

    public static Throwable map(final Throwable throwable) {
        AppbaseUtil.notNull(throwable, "throwable");
        if (throwable instanceof AppbaseException || throwable instanceof AppbaseRuntimeException) {
            return throwable;
        }
        final String message = throwable.getMessage() != null
                ? throwable.getMessage() : throwable.getClass().getName();
        return new ReactiveOperationException("Reactive operation failed: " + message, throwable);
    }
}
