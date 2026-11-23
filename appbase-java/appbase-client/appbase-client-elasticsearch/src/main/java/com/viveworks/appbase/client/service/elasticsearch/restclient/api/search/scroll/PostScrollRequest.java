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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.scroll;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostScrollRequest {
    private final String scroll;

    @JsonProperty("scroll_id")
    private final String scrollId;

    public PostScrollRequest(
            final String scroll,
            @JsonProperty("scroll_id")
            final String scrollId)
    {
        AppbaseUtil.notBlank(scroll, "scroll");
        AppbaseUtil.notBlank(scrollId, "scrollId");

        this.scroll = scroll;
        this.scrollId = scrollId;
    }

    public String getScroll() {
        return scroll;
    }

    public String getScrollId() {
        return scrollId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PostScrollRequest that = (PostScrollRequest) o;
        return Objects.equals(scroll, that.scroll) &&
                Objects.equals(scrollId, that.scrollId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scroll, scrollId);
    }

    @Override
    public String toString() {
        return "PostScrollRequest{" +
                "scroll='" + scroll + '\'' +
                ", scrollId='" + scrollId + '\'' +
                '}';
    }
}
