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

package com.viveworks.appbase.bootstrap.reactornetty.endpoint;

import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable pair of the two knobs that govern a single offloaded, blocking read: how many rows/items
 * a page may hold ({@code maxPageSize}) and how long the offloaded work may run before it is
 * considered stuck ({@code timeout}). Passed as one unit so callers cannot supply one half without
 * the other.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
public final class OffloadPolicy {

    private final int maxPageSize;

    private final Duration timeout;

    private OffloadPolicy(final int maxPageSize, final Duration timeout) {
        this.maxPageSize = maxPageSize;
        this.timeout = timeout;
    }

    /**
     * Create a validated {@link OffloadPolicy}.
     *
     * @param maxPageSize the maximum page size; must be {@code >= 1}
     * @param timeout     the offload timeout; must be non-null and positive
     * @return the new policy
     * @throws IllegalArgumentException if {@code maxPageSize < 1}, or {@code timeout} is {@code null}
     *                                  or not positive
     */
    public static OffloadPolicy of(final int maxPageSize, final Duration timeout) {
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("maxPageSize must be >= 1, got " + maxPageSize);
        }

        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be non-null and positive, got " + timeout);
        }

        return new OffloadPolicy(maxPageSize, timeout);
    }

    /**
     * @return the maximum page size
     */
    public int maxPageSize() {
        return maxPageSize;
    }

    /**
     * @return the offload timeout
     */
    public Duration timeout() {
        return timeout;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OffloadPolicy that = (OffloadPolicy) o;
        return maxPageSize == that.maxPageSize && Objects.equals(timeout, that.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxPageSize, timeout);
    }

    @Override
    public String toString() {
        return "OffloadPolicy{" +
                "maxPageSize=" + maxPageSize +
                ", timeout=" + timeout +
                '}';
    }
}
