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

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents an exact timestamp criteria.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class ExactTimestampCriteria implements TimestampCriteria {
    private final long timestamp;

    @JsonCreator
    public ExactTimestampCriteria(@JsonProperty("timestamp") final long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ExactTimestampCriteria)) {
            return false;
        }

        final ExactTimestampCriteria that = (ExactTimestampCriteria) o;
        return timestamp == that.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }

    @Override
    public String toString() {
        return "ExactTimestampCriteria{" +
                "timestamp=" + timestamp +
                '}';
    }
}
