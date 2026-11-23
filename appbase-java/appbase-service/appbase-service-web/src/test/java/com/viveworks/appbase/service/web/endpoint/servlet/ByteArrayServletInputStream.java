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

package com.viveworks.appbase.service.web.endpoint.servlet;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * A minimal, real {@link ServletInputStream} over an in-memory byte array, for tests that need a
 * request body without standing up a servlet container. {@code ServletInputStream} is abstract
 * and a Mockito mock of it answers {@code read()} with {@code -1} by default, which would make
 * {@code readAllBytes()} see an empty body regardless of what the test intends to feed in.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class ByteArrayServletInputStream extends ServletInputStream {
    private final ByteArrayInputStream delegate;

    ByteArrayServletInputStream(final byte[] bytes) {
        this.delegate = new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean isFinished() {
        return delegate.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(final ReadListener readListener) {
        // Not needed for the synchronous reads these tests exercise.
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }
}
