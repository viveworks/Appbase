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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoDistanceSortValue implements SortValue {
    @JsonProperty("pin.location")
    @JsonSerialize(contentAs = PinLocation.class)
    private List<PinLocation> pinLocation;

    private String order;

    @JsonProperty("distance_type")
    private String distanceType;

    private String mode;

    private String unit;

    @JsonProperty("ignore_unmapped")
    private Boolean ignoreUnmapped;

    public List<PinLocation> getPinLocation() {
        return pinLocation;
    }

    public void setPinLocation(final List<PinLocation> pinLocation) {
        this.pinLocation = pinLocation;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(final String order) {
        this.order = order;
    }

    public String getDistanceType() {
        return distanceType;
    }

    public void setDistanceType(final String distanceType) {
        this.distanceType = distanceType;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public Boolean getIgnoreUnmapped() {
        return ignoreUnmapped;
    }

    public void setIgnoreUnmapped(final Boolean ignoreUnmapped) {
        this.ignoreUnmapped = ignoreUnmapped;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GeoDistanceSortValue that = (GeoDistanceSortValue) o;
        return Objects.equals(pinLocation, that.pinLocation) &&
                Objects.equals(order, that.order) &&
                Objects.equals(distanceType, that.distanceType) &&
                Objects.equals(mode, that.mode) &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(ignoreUnmapped, that.ignoreUnmapped);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pinLocation, order, distanceType, mode, unit, ignoreUnmapped);
    }

    @Override
    public String toString() {
        return "GeoDistanceSortValue{" +
                "pinLocation=" + pinLocation +
                ", order='" + order + '\'' +
                ", distanceType='" + distanceType + '\'' +
                ", mode='" + mode + '\'' +
                ", unit='" + unit + '\'' +
                ", ignoreUnmapped=" + ignoreUnmapped +
                '}';
    }
}
