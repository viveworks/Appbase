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

package com.viveworks.appbase.core.service.exception.store;

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Default implementation of {@link StackTraceHashGenerator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultStackTraceHashGenerator implements StackTraceHashGenerator {
    private static final int DEFAULT_STACK_TRACE_STRING_MAX_LENGTH = 2048;

    private final int stackTraceStringMaxLength;

    public DefaultStackTraceHashGenerator() {
        this.stackTraceStringMaxLength = DEFAULT_STACK_TRACE_STRING_MAX_LENGTH;
    }

    public DefaultStackTraceHashGenerator(final int stackTraceStringMaxLength) {
        AppbaseUtil.positive(stackTraceStringMaxLength, "stackTraceStringMaxLength");

        this.stackTraceStringMaxLength = stackTraceStringMaxLength;
    }

    @Override
    public int generateHash(final String stackTraceString) {
        AppbaseUtil.notBlank(stackTraceString, "stackTraceString");

        String finalStackTraceString = stackTraceString;
        if (finalStackTraceString.length() > stackTraceStringMaxLength) {
            finalStackTraceString = finalStackTraceString.substring(0, stackTraceStringMaxLength);
        }

        return finalStackTraceString.hashCode();
    }
}
