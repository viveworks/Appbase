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

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DeleteIndexResponse {
    private final boolean deleted;

    private final boolean found;

    public DeleteIndexResponse(
            final boolean deleted, final boolean found)
    {
        this.deleted = deleted;
        this.found = found;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isFound() {
        return found;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DeleteIndexResponse that = (DeleteIndexResponse) o;
        return deleted == that.deleted && found == that.found;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deleted, found);
    }

    @Override
    public String toString() {
        return "DeleteIndexResponse{" +
                "deleted=" + deleted +
                ", found=" + found +
                '}';
    }
}
