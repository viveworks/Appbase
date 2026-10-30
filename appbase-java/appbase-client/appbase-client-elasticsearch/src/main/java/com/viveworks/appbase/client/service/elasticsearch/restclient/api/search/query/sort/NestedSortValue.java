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
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.Filter;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NestedSortValue implements SortValue {
    private String path;

    private Filter filter;

    @JsonProperty("max_children")
    private Integer maxChildren;

    private NestedSortValue nested;

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(final Filter filter) {
        this.filter = filter;
    }

    public Integer getMaxChildren() {
        return maxChildren;
    }

    public void setMaxChildren(final Integer maxChildren) {
        this.maxChildren = maxChildren;
    }

    public NestedSortValue getNested() {
        return nested;
    }

    public void setNested(final NestedSortValue nested) {
        this.nested = nested;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NestedSortValue that = (NestedSortValue) o;
        return Objects.equals(path, that.path) &&
                Objects.equals(filter, that.filter) &&
                Objects.equals(maxChildren, that.maxChildren) &&
                Objects.equals(nested, that.nested);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, filter, maxChildren, nested);
    }

    @Override
    public String toString() {
        return "NestedSortValue{" +
                "path='" + path + '\'' +
                ", filter=" + filter +
                ", maxChildren=" + maxChildren +
                ", nested=" + nested +
                '}';
    }
}
