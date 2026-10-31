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

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Rescore {
    @JsonProperty("window_size")
    private Integer windowSize;

    private RescoreQuery query;

    public Integer getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(final Integer windowSize) {
        this.windowSize = windowSize;
    }

    public RescoreQuery getQuery() {
        return query;
    }

    public void setQuery(final RescoreQuery query) {
        this.query = query;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Rescore rescore = (Rescore) o;
        return Objects.equals(windowSize, rescore.windowSize) &&
                Objects.equals(query, rescore.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowSize, query);
    }

    @Override
    public String toString() {
        return "Rescore{" +
                "windowSize=" + windowSize +
                ", query=" + query +
                '}';
    }
}
