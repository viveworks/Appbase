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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.Poolable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class for all message that have a context.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
public abstract class AbstractContextualMessage implements Poolable {
    protected final Map<String, String> context = new ConcurrentHashMap<>();
    private long timestamp;

    /**
     * Add a pair of key-value to context.
     *
     * @param key Key to be added
     * @param value Value for that key
     */
    public void addContext(final String key, final String value) {
        if (key != null && value != null) {
            context.put(key, value);
        }
    }

    /**
     * Add all the pairs in the map to the context.
     *
     * @param context Context to add
     */
    public void addAllContext(final Map<String, String> context) {
        if (context != null) {
            context.forEach(this.context::put);
        }
    }

    public Map<String, String> getContext() {
        return context;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        AppbaseUtil.positiveOrZero(timestamp, "timestamp");

        this.timestamp = timestamp;
    }

    @Override
    public boolean isValid() {
        return timestamp >= 0;
    }

    public boolean isClean() {
        return context.isEmpty();
    }

    /**
     * Clear the object state.
     */
    public void clean() {
        context.clear();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AbstractContextualMessage that = (AbstractContextualMessage) o;
        return Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context);
    }

    @Override
    public String toString() {
        return "AbstractContextualMessage{" +
                "context=" + context +
                '}';
    }
}
