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

package com.viveworks.appbase.core.service.reactive.read;

import java.util.function.BooleanSupplier;

/**
 * A blocking read that cooperatively aborts when {@code cancelled} becomes {@code true}, checked between
 * units of work (e.g. once per document in a multi-document fetch loop) rather than relying on
 * {@link Thread#interrupt()}.
 *
 * @param <T> Type of value read
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@FunctionalInterface
public interface CancellableRead<T> {
    /**
     * Perform a blocking read, checking {@code cancelled} between units of work and aborting cooperatively
     * once it becomes {@code true}.
     *
     * @param cancelled Supplies {@code true} once the caller has abandoned this read
     * @return the value read
     * @throws Exception if the read fails
     */
    T read(BooleanSupplier cancelled) throws Exception;
}
