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

package com.viveworks.appbase.client.cloud.aws.common.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.regions.Region;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link Region}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkRegionConfiguration.Builder.class)
public final class AwsSdkRegionConfiguration {
    private final String id;
    private final boolean globalRegion;

    private AwsSdkRegionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.id, "builder.id");

        id = builder.id;
        globalRegion = builder.globalRegion;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the configured id.
     *
     * @return the configured id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns whether global region.
     *
     * @return whether global region
     */
    public boolean isGlobalRegion() {
        return globalRegion;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkRegionConfiguration that = (AwsSdkRegionConfiguration) o;
        return globalRegion == that.globalRegion && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, globalRegion);
    }

    @Override
    public String toString() {
        return "AwsSdkRegionConfiguration{" +
                "id='" + id + '\'' +
                ", globalRegion=" + globalRegion +
                '}';
    }


    /**
     * Builder for {@link AwsSdkRegionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_ID = Region.US_EAST_1.id();
        private static final boolean DEFAULT_GLOBAL_REGION = Region.US_EAST_1.isGlobalRegion();


        private String id = DEFAULT_ID;
        private boolean globalRegion = DEFAULT_GLOBAL_REGION;


        private Builder() {
        }

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder globalRegion(final boolean globalRegion) {
            this.globalRegion = globalRegion;
            return this;
        }

        public AwsSdkRegionConfiguration build() {
            return new AwsSdkRegionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
