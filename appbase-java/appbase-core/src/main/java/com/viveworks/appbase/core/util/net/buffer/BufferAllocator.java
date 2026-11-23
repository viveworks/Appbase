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

package com.viveworks.appbase.core.util.net.buffer;

import io.netty.buffer.ByteBufAllocator;

/**
 * Wrapper for {@link ByteBufAllocator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface BufferAllocator {
    /**
     * Get a {@link Buffer}.
     *
     * @return {@link Buffer}
     */
    Buffer buffer();

    /**
     * Get a {@link Buffer} with initial capacity.
     *
     * @param initialCapacity Initial capacity
     * @return {@link Buffer}
     */
    Buffer buffer(int initialCapacity);

    /**
     * Get a {@link Buffer} with initial capacity.
     *
     * @param initialCapacity Initial capacity
     * @param maxCapacity Max capacity
     * @return {@link Buffer}
     */
    Buffer buffer(int initialCapacity, int maxCapacity);

    /**
     * Get the {@link BufferPoolingStrategy} used by this buffer allocator.
     *
     * @return {@link BufferPoolingStrategy}
     */
    BufferPoolingStrategy getBufferPoolingStrategy();

    /**
     * Get the {@link BufferSource} used by this buffer allocator.
     *
     * @return {@link BufferSource}
     */
    BufferSource getBufferSource();

    /**
     * Get {@code netty} {@link ByteBufAllocator}.
     *
     * @return {@link ByteBufAllocator}
     */
    ByteBufAllocator getNettyByteBufAllocator();
}
