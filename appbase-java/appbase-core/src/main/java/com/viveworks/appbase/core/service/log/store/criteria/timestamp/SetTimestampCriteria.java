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

package com.viveworks.appbase.core.service.log.store.criteria.timestamp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a set of timestamp criteria.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class SetTimestampCriteria implements TimestampCriteria {
    private final Set<Long> timestamps;

    public SetTimestampCriteria() {
        this.timestamps = new HashSet<>();
    }

    @JsonCreator
    public SetTimestampCriteria(@JsonProperty("timestamps")
                                @JsonSerialize(contentAs = Long.class)
                                @JsonDeserialize(contentAs = Long.class)
                                final Set<Long> timestamps)
    {
        AppbaseUtil.notNull(timestamps, "timestamps");
        this.timestamps = new HashSet<>(timestamps);
    }

    /**
     * Add a timestamp criteria to the contained set.
     *
     * @param timestamp Timestamp to add
     */
    public void addTimestamp(final long timestamp) {
        timestamps.add(timestamp);
    }

    public Set<Long> getTimestamps() {
        return Collections.unmodifiableSet(timestamps);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SetTimestampCriteria)) {
            return false;
        }

        final SetTimestampCriteria that = (SetTimestampCriteria) o;
        return Objects.equals(timestamps, that.timestamps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamps);
    }

    @Override
    public String toString() {
        return "SetTimestampCriteria{" +
                "timestamps=" + timestamps +
                '}';
    }
}
