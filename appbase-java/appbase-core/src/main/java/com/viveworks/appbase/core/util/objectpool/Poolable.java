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

package com.viveworks.appbase.core.util.objectpool;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Represents an object that can be pooled. Additionally, there is a general contract to define a
 * {@code init}, so it becomes easier to have a {@link #isValid()} object state.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
public interface Poolable {
    /**
     * Check if the object state is valid. If not, a {@link #clean()} and {@code init} should make it a valid object.
     *
     * @return true if value, false otherwise
     */
    boolean isValid();

    /**
     * Check if this {@code poolable} is clean.
     *
     * @return true if clean, false otherwise
     */
    boolean isClean();

    /**
     * Clean the internal state of the object.
     */
    void clean();
}
