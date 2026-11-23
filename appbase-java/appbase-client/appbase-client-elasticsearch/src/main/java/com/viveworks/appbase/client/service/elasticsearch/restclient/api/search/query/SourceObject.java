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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceObject {
    @JsonSerialize(contentAs = String.class)
    private List<String> excludes;

    @JsonSerialize(contentAs = String.class)
    private List<String> includes;

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(final List<String> excludes) {
        this.excludes = excludes;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(final List<String> includes) {
        this.includes = includes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SourceObject sourceObject = (SourceObject) o;
        return Objects.equals(excludes, sourceObject.excludes) &&
                Objects.equals(includes, sourceObject.includes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(excludes, includes);
    }

    @Override
    public String toString() {
        return "Source{" +
                "excludes=" + excludes +
                ", includes=" + includes +
                '}';
    }
}
