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
public class RangeQuery implements SearchQuery {
    @JsonSerialize(keyAs = String.class, contentAs = FieldParams.class)
    @JsonDeserialize(keyAs = String.class, contentAs = FieldParams.class)
    private Map<String, FieldParams> range;

    public Map<String, FieldParams> getRange() {
        return range;
    }

    public void setRange(final Map<String, FieldParams> range) {
        this.range = range;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RangeQuery that = (RangeQuery) o;
        return Objects.equals(range, that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(range);
    }

    @Override
    public String toString() {
        return "RangeQuery{" +
                "range=" + range +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldParams {
        @JsonProperty("gt")
        private String greaterThan;

        @JsonProperty("gte")
        private String greaterThanOrEqualTo;

        @JsonProperty("lt")
        private String lessThan;

        @JsonProperty("lte")
        private String lessThanOrEqualTo;

        private String format;

        private String relation;

        @JsonProperty("time_zone")
        private String timeZone;

        private Float boost;

        public String getGreaterThan() {
            return greaterThan;
        }

        public void setGreaterThan(final String greaterThan) {
            this.greaterThan = greaterThan;
        }

        public String getGreaterThanOrEqualTo() {
            return greaterThanOrEqualTo;
        }

        public void setGreaterThanOrEqualTo(
                final String greaterThanOrEqualTo)
        {
            this.greaterThanOrEqualTo = greaterThanOrEqualTo;
        }

        public String getLessThan() {
            return lessThan;
        }

        public void setLessThan(final String lessThan) {
            this.lessThan = lessThan;
        }

        public String getLessThanOrEqualTo() {
            return lessThanOrEqualTo;
        }

        public void setLessThanOrEqualTo(
                final String lessThanOrEqualTo)
        {
            this.lessThanOrEqualTo = lessThanOrEqualTo;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(final String format) {
            this.format = format;
        }

        public String getRelation() {
            return relation;
        }

        public void setRelation(final String relation) {
            this.relation = relation;
        }

        public String getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(final String timeZone) {
            this.timeZone = timeZone;
        }

        public Float getBoost() {
            return boost;
        }

        public void setBoost(final Float boost) {
            this.boost = boost;
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
            return Objects.equals(greaterThan, that.greaterThan) &&
                    Objects.equals(greaterThanOrEqualTo, that.greaterThanOrEqualTo) &&
                    Objects.equals(lessThan, that.lessThan) &&
                    Objects.equals(lessThanOrEqualTo, that.lessThanOrEqualTo) &&
                    Objects.equals(format, that.format) &&
                    Objects.equals(relation, that.relation) &&
                    Objects.equals(timeZone, that.timeZone) &&
                    Objects.equals(boost, that.boost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    greaterThan,
                    greaterThanOrEqualTo,
                    lessThan,
                    lessThanOrEqualTo,
                    format,
                    relation,
                    timeZone,
                    boost);
        }

        @Override
        public String toString() {
            return "FieldParams{" +
                    "greaterThan='" + greaterThan + '\'' +
                    ", greaterThanOrEqualTo='" + greaterThanOrEqualTo + '\'' +
                    ", lessThan='" + lessThan + '\'' +
                    ", lessThanOrEqualTo='" + lessThanOrEqualTo + '\'' +
                    ", format='" + format + '\'' +
                    ", relation='" + relation + '\'' +
                    ", timeZone='" + timeZone + '\'' +
                    ", boost=" + boost +
                    '}';
        }
    }
}
