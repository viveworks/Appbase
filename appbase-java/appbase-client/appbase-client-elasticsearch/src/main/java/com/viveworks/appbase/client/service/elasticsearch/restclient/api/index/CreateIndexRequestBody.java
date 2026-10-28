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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.Filter;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateIndexRequestBody {
    @Nullable
    private Mappings mappings;

    @JsonSerialize(keyAs = String.class, contentAs = AliasDefinition.class)
    @JsonDeserialize(keyAs = String.class, contentAs = AliasDefinition.class)
    private Map<String, AliasDefinition> aliases;

    private Map<String, Object> settings;

    @Nullable
    public Mappings getMappings() {
        return mappings;
    }

    public void setMappings(@Nullable final Mappings mappings) {
        this.mappings = mappings;
    }

    public Map<String, AliasDefinition> getAliases() {
        return aliases;
    }

    public void setAliases(final Map<String, AliasDefinition> aliases) {
        this.aliases = aliases;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(final Map<String, Object> settings) {
        this.settings = settings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CreateIndexRequestBody that = (CreateIndexRequestBody) o;
        return Objects.equals(mappings, that.mappings) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(settings, that.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappings, aliases, settings);
    }

    @Override
    public String toString() {
        return "CreateIndexRequestBody{" +
                "mappings=" + mappings +
                ", aliases=" + aliases +
                ", settings=" + settings +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AliasDefinition {
        private Filter filter;

        @JsonProperty("index_routing")
        private String indexRouting;

        @JsonProperty("is_hidden")
        private Boolean isHidden;

        @JsonProperty("is_write_index")
        private Boolean isWriteIndex;

        private String routing;

        @JsonProperty("search_routing")
        private String searchRouting;

        public Filter getFilter() {
            return filter;
        }

        public void setFilter(final Filter filter) {
            this.filter = filter;
        }

        public String getIndexRouting() {
            return indexRouting;
        }

        public void setIndexRouting(final String indexRouting) {
            this.indexRouting = indexRouting;
        }

        public Boolean getHidden() {
            return isHidden;
        }

        public void setHidden(final Boolean hidden) {
            isHidden = hidden;
        }

        public Boolean getWriteIndex() {
            return isWriteIndex;
        }

        public void setWriteIndex(final Boolean writeIndex) {
            isWriteIndex = writeIndex;
        }

        public String getRouting() {
            return routing;
        }

        public void setRouting(final String routing) {
            this.routing = routing;
        }

        public String getSearchRouting() {
            return searchRouting;
        }

        public void setSearchRouting(final String searchRouting) {
            this.searchRouting = searchRouting;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final AliasDefinition that = (AliasDefinition) o;
            return Objects.equals(filter, that.filter) &&
                    Objects.equals(indexRouting, that.indexRouting) &&
                    Objects.equals(isHidden, that.isHidden) &&
                    Objects.equals(isWriteIndex, that.isWriteIndex) &&
                    Objects.equals(routing, that.routing) &&
                    Objects.equals(searchRouting, that.searchRouting);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    filter,
                    indexRouting,
                    isHidden,
                    isWriteIndex,
                    routing,
                    searchRouting);
        }

        @Override
        public String toString() {
            return "AliasDefinition{" +
                    "filter=" + filter +
                    ", indexRouting='" + indexRouting + '\'' +
                    ", isHidden=" + isHidden +
                    ", isWriteIndex=" + isWriteIndex +
                    ", routing='" + routing + '\'' +
                    ", searchRouting='" + searchRouting + '\'' +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Mappings {
        @JsonSerialize(keyAs = String.class, contentAs = FieldProperties.class)
        @JsonDeserialize(keyAs = String.class, contentAs = FieldProperties.class)
        private Map<String, FieldProperties> properties;

        public Map<String, FieldProperties> getProperties() {
            return properties;
        }

        public void setProperties(
                final Map<String, FieldProperties> properties)
        {
            this.properties = properties;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Mappings mappings = (Mappings) o;
            return Objects.equals(properties, mappings.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(properties);
        }

        @Override
        public String toString() {
            return "Mappings{" +
                    "properties=" + properties +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldProperties {
        private String type;
        private String format;
        private Boolean index;

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(final String format) {
            this.format = format;
        }

        public Boolean getIndex() {
            return index;
        }

        public void setIndex(final Boolean index) {
            this.index = index;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FieldProperties that = (FieldProperties) o;
            return Objects.equals(type, that.type) &&
                    Objects.equals(format, that.format) &&
                    Objects.equals(index, that.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, format, index);
        }

        @Override
        public String toString() {
            return "FieldProperties{" +
                    "type='" + type + '\'' +
                    "format='" + format + '\'' +
                    ", index=" + index +
                    '}';
        }
    }
}
