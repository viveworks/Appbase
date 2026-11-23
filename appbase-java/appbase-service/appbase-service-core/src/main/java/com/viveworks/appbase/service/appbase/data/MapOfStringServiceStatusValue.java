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

package com.viveworks.appbase.service.appbase.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.ServiceStatus;

import java.util.Map;
import java.util.Objects;

/**
  * Jackson value object carrying a mapping from service names to their current statuses. The supplied status map is
  * retained rather than copied,
 * so later caller mutations are observable through the accessor; equality and hashing delegate to it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class MapOfStringServiceStatusValue {
    @JsonSerialize(keyAs = String.class, contentAs = ServiceStatus.class)
    @JsonDeserialize(keyAs = String.class, contentAs = ServiceStatus.class)
    private final Map<String, ServiceStatus> serviceStatusByNameMap;

    /**
     * Binds the JSON property directly to this wrapper's retained status map.
     *
     * @param serviceStatusByNameMap payload to retain
     */
    @JsonCreator
    public MapOfStringServiceStatusValue(
            @JsonProperty("serviceStatusByNameMap")
            final Map<String, ServiceStatus> serviceStatusByNameMap)
    {
        this.serviceStatusByNameMap = serviceStatusByNameMap;
    }

    /**
     * Wraps the supplied status map without defensive copying.
     *
     * @param serviceStatusByNameMap payload to retain
     * @return a new MapOfStringServiceStatusValue referencing that payload
     */
    public static MapOfStringServiceStatusValue from(final Map<String, ServiceStatus> serviceStatusByNameMap) {
        return new MapOfStringServiceStatusValue(serviceStatusByNameMap);
    }

    /**
     * Exposes the retained status map; callers receive the live reference, not a copy.
     * @return the stored payload
     */
    public Map<String, ServiceStatus> getServiceStatusByNameMap() {
        return serviceStatusByNameMap;
    }

    /**
     * Compares MapOfStringServiceStatusValue instances by the wrapped status map.
     * @param o candidate MapOfStringServiceStatusValue wrapper
     * @return true when both wrappers carry equal payloads
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MapOfStringServiceStatusValue that = (MapOfStringServiceStatusValue) o;
        return Objects.equals(serviceStatusByNameMap, that.serviceStatusByNameMap);
    }

    /**
     * Computes a hash from the wrapped status map.
     * @return MapOfStringServiceStatusValue payload-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(serviceStatusByNameMap);
    }

    /**
     * Formats the wrapper name and its serviceStatusByNameMap payload for diagnostics.
     * @return diagnostic representation containing the MapOfStringServiceStatusValue payload
     */
    @Override
    public String toString() {
        return "MapOfStringServiceStatusValue{" +
                "serviceStatusByNameMap=" + serviceStatusByNameMap +
                '}';
    }
}
