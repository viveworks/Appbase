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

package com.viveworks.appbase.core.util.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.viveworks.appbase.core.service.Shutdownable;

/**
 * A service for {@link com.lmax.disruptor.dsl.Disruptor}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Event type
 * @since 1.0.0
 */
public interface DisruptorService<T> extends Shutdownable {
    /**
     * Get the {@link Disruptor}.
     *
     * @return {@link Disruptor}
     */
    Disruptor<T> getDisruptor();

    /**
     * Get the {@link RingBuffer}.
     *
     * @return Ring buffer
     */
    RingBuffer<T> getRingBuffer();
}
