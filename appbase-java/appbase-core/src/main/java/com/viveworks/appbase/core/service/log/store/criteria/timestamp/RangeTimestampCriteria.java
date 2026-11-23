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

package com.viveworks.appbase.core.service.log.store.criteria.timestamp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents a timestamp range criteria.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class RangeTimestampCriteria implements TimestampCriteria {
    private final long inclusiveStartTimestamp;
    private final long exclusiveEndTimestamp;

    @JsonCreator
    public RangeTimestampCriteria(
            @JsonProperty("inclusiveStartTimestamp")
            final long inclusiveStartTimestamp,
            @JsonProperty("exclusiveEndTimestamp")
            final long exclusiveEndTimestamp)
    {
        this.inclusiveStartTimestamp = inclusiveStartTimestamp;
        this.exclusiveEndTimestamp = exclusiveEndTimestamp;
    }

    public long getInclusiveStartTimestamp() {
        return inclusiveStartTimestamp;
    }

    public long getExclusiveEndTimestamp() {
        return exclusiveEndTimestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof RangeTimestampCriteria)) {
            return false;
        }

        final RangeTimestampCriteria that = (RangeTimestampCriteria) o;
        return inclusiveStartTimestamp == that.inclusiveStartTimestamp &&
                exclusiveEndTimestamp == that.exclusiveEndTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inclusiveStartTimestamp, exclusiveEndTimestamp);
    }

    @Override
    public String toString() {
        return "RangeTimestampCriteria{" +
                "inclusiveStartTimestamp=" + inclusiveStartTimestamp +
                ", exclusiveEndTimestamp=" + exclusiveEndTimestamp +
                '}';
    }
}
