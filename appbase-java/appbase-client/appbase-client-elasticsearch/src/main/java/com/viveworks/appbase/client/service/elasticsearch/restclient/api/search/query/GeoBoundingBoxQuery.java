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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoBoundingBoxQuery implements SearchQuery {
    @JsonProperty("geo_bounding_box")
    @JsonSerialize(keyAs = String.class, contentAs = FieldParams.class)
    @JsonDeserialize(keyAs = String.class, contentAs = FieldParams.class)
    private Map<String, FieldParams> geoBoundingBox;

    public Map<String, FieldParams> getGeoBoundingBox() {
        return geoBoundingBox;
    }

    public void setGeoBoundingBox(final Map<String, FieldParams> geoBoundingBox) {
        this.geoBoundingBox = geoBoundingBox;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GeoBoundingBoxQuery that = (GeoBoundingBoxQuery) o;
        return Objects.equals(geoBoundingBox, that.geoBoundingBox);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geoBoundingBox);
    }

    @Override
    public String toString() {
        return "GeoBoundingBoxQuery{" +
                "geoBoundingBox=" + geoBoundingBox +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldParams {
        @JsonProperty("top_left")
        private GeoLocation topLeft;

        @JsonProperty("bottom_right")
        private GeoLocation bottomRight;

        public GeoLocation getTopLeft() {
            return topLeft;
        }

        public void setTopLeft(final GeoLocation topLeft) {
            this.topLeft = topLeft;
        }

        public GeoLocation getBottomRight() {
            return bottomRight;
        }

        public void setBottomRight(final GeoLocation bottomRight) {
            this.bottomRight = bottomRight;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FieldParams that = (FieldParams) o;
            return Objects.equals(topLeft, that.topLeft) &&
                    Objects.equals(bottomRight, that.bottomRight);
        }

        @Override
        public int hashCode() {
            return Objects.hash(topLeft, bottomRight);
        }

        @Override
        public String toString() {
            return "FieldParams{" +
                    "topLeft=" + topLeft +
                    ", bottomRight=" + bottomRight +
                    '}';
        }
    }
}
