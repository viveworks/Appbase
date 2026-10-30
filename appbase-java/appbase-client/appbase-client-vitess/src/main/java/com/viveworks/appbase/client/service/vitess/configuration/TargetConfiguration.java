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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@code target} represented as {@code keyspace@shard:tabletType}. Valid
 * names are: keyspace:shard@tabletType, keyspace@tabletType or @tabletType
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = TargetConfiguration.Builder.class)
public final class TargetConfiguration {
    private final String keyspace;
    private final String shard;
    private final VitessTabletType vitessTabletType;

    private TargetConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.vitessTabletType, "builder.vitessTabletType");

        keyspace = builder.keyspace;
        shard = builder.shard;
        vitessTabletType = builder.vitessTabletType;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getKeyspace() {
        return keyspace;
    }

    public String getShard() {
        return shard;
    }

    public VitessTabletType getVitessTabletType() {
        return vitessTabletType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TargetConfiguration that = (TargetConfiguration) o;
        return Objects.equals(keyspace, that.keyspace) &&
                Objects.equals(shard, that.shard) &&
                vitessTabletType == that.vitessTabletType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyspace, shard, vitessTabletType);
    }

    @Override
    public String toString() {
        return "TargetConfiguration{" +
                "keyspace='" + keyspace + '\'' +
                ", shard='" + shard + '\'' +
                ", vitessTabletType=" + vitessTabletType +
                '}';
    }

    /**
     * Builder for {@link TargetConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_KEYSPACE = "appbase-vitess-test-keyspace";
        private static final String DEFAULT_SHARD = "0";
        private static final VitessTabletType DEFAULT_VITESS_TABLET_TYPE = VitessTabletType.PRIMARY;

        private String keyspace = DEFAULT_KEYSPACE;
        private String shard = DEFAULT_SHARD;
        private VitessTabletType vitessTabletType = DEFAULT_VITESS_TABLET_TYPE;

        private Builder() {
        }

        public Builder keyspace(final String keyspace) {
            this.keyspace = keyspace;
            return this;
        }

        public Builder shard(final String shard) {
            this.shard = shard;
            return this;
        }

        public Builder vitessTabletType(final VitessTabletType vitessTabletType) {
            this.vitessTabletType = vitessTabletType;
            return this;
        }

        public TargetConfiguration build() {
            return new TargetConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
