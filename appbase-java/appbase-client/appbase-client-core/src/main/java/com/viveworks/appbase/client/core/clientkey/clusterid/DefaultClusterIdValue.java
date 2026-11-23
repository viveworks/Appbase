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

package com.viveworks.appbase.client.core.clientkey.clusterid;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default {@link ClusterIdValue}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class DefaultClusterIdValue implements MutableClusterIdValue {
    private static final boolean DEFAULT_USE_LOWER_CASE_COMPARE_TO = true;

    private final AtomicBoolean useLowerCaseCompareToRef;
    private final AtomicReference<String> clusterIdRef;

    /** Creates a clean pooled value whose first initialization compares case-insensitively. */
    public DefaultClusterIdValue() {
        this.useLowerCaseCompareToRef = new AtomicBoolean(DEFAULT_USE_LOWER_CASE_COMPARE_TO);
        this.clusterIdRef = new AtomicReference<>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final String clusterId) {
        init(clusterId, DEFAULT_USE_LOWER_CASE_COMPARE_TO);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final String clusterId, final boolean useLowerCaseCompareTo) {
        AppbaseUtil.notNull(clusterId, "clusterId");

        this.clusterIdRef.set(clusterId);
        this.useLowerCaseCompareToRef.set(useLowerCaseCompareTo);
    }

    /** {@inheritDoc} */
    @Override
    public String getClusterId() {
        return clusterIdRef.get();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        return getClusterId() != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClean() {
        return getClusterId() == null;
    }

    /** {@inheritDoc} */
    @Override
    public void clean() {
        clusterIdRef.set(null);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(@NotNull final Object other) {
        AppbaseUtil.notNull(other, "other");

        if (!DefaultClusterIdValue.class.isAssignableFrom(other.getClass())) {
            throw new AppbaseRuntimeException("other must be of type " + DefaultClusterIdValue.class.getName());
        }

        final DefaultClusterIdValue defaultClusterId = (DefaultClusterIdValue) other;
        AppbaseUtil.valid(this, "this");
        AppbaseUtil.valid(defaultClusterId, "defaultClusterId");

        if (useLowerCaseCompareToRef.get()) {
            return getClusterId().toLowerCase().compareTo(defaultClusterId.getClusterId().toLowerCase());
        }
        return getClusterId().compareTo(defaultClusterId.getClusterId());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultClusterIdValue that = (DefaultClusterIdValue) o;
        return Objects.equals(useLowerCaseCompareToRef.get(), that.useLowerCaseCompareToRef.get()) &&
                Objects.equals(clusterIdRef.get(), that.clusterIdRef.get());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(useLowerCaseCompareToRef.get(), clusterIdRef.get());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DefaultClusterIdValue{" +
                "useLowerCaseCompareTo=" + useLowerCaseCompareToRef.get() +
                ", clusterId=" + clusterIdRef.get() +
                '}';
    }


    /**
     * {@link ObjectFactory} for {@link DefaultClusterIdValue}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<MutableClusterIdValue> {
        /** {@inheritDoc} */
        @Override
        public MutableClusterIdValue newObject() {
            return new DefaultClusterIdValue();
        }
    }
}
