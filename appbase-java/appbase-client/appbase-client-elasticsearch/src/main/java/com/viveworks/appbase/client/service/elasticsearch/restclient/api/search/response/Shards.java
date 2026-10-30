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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Shards {
    private Integer total;

    private Integer successful;

    private Integer skipped;

    private Integer failed;

    public Integer getTotal() {
        return total;
    }

    public void setTotal(final Integer total) {
        this.total = total;
    }

    public Integer getSuccessful() {
        return successful;
    }

    public void setSuccessful(final Integer successful) {
        this.successful = successful;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public void setSkipped(final Integer skipped) {
        this.skipped = skipped;
    }

    public Integer getFailed() {
        return failed;
    }

    public void setFailed(final Integer failed) {
        this.failed = failed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Shards shards = (Shards) o;
        return Objects.equals(total, shards.total) &&
                Objects.equals(successful, shards.successful) &&
                Objects.equals(skipped, shards.skipped) &&
                Objects.equals(failed, shards.failed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, successful, skipped, failed);
    }

    @Override
    public String toString() {
        return "Shards{" +
                "total=" + total +
                ", successful=" + successful +
                ", skipped=" + skipped +
                ", failed=" + failed +
                '}';
    }
}
