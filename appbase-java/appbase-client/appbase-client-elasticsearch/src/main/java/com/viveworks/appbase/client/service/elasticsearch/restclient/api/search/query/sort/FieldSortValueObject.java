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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldSortValueObject {
    private String type;

    @JsonProperty("numeric_type")
    private String numericType;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getNumericType() {
        return numericType;
    }

    public void setNumericType(final String numericType) {
        this.numericType = numericType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FieldSortValueObject that = (FieldSortValueObject) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(numericType, that.numericType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, numericType);
    }

    @Override
    public String toString() {
        return "FieldSortValueObject{" +
                "type='" + type + '\'' +
                ", numericType='" + numericType + '\'' +
                '}';
    }
}
