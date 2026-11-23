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

package com.viveworks.appbase.service.user.session.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for {@link DefaultUserSessionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultUserSessionStoreConfiguration.Builder.class)
public class DefaultUserSessionStoreConfiguration implements AppbaseConfiguration {
    private final String aerospikeSessionNamespace;
    private final String aerospikeSessionSetName;
    private final Long sessionExpirationSeconds;
    private final Integer maxSessionsToKeep;

    private DefaultUserSessionStoreConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.aerospikeSessionNamespace, "builder.aerospikeSessionNamespace");
        AppbaseUtil.notBlank(builder.aerospikeSessionSetName, "builder.aerospikeSessionSetName");
        AppbaseUtil.notNull(builder.sessionExpirationSeconds, "builder.sessionExpirationSeconds");
        AppbaseUtil.notNull(builder.maxSessionsToKeep, "builder.maxSessionsToKeep");
        AppbaseUtil.positive(builder.maxSessionsToKeep, "builder.maxSessionsToKeep");

        this.aerospikeSessionNamespace = builder.aerospikeSessionNamespace;
        this.aerospikeSessionSetName = builder.aerospikeSessionSetName;
        this.sessionExpirationSeconds = builder.sessionExpirationSeconds;
        this.maxSessionsToKeep = builder.maxSessionsToKeep;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAerospikeSessionNamespace() {
        return aerospikeSessionNamespace;
    }

    public String getAerospikeSessionSetName() {
        return aerospikeSessionSetName;
    }

    public Long getSessionExpirationSeconds() {
        return sessionExpirationSeconds;
    }

    public Integer getMaxSessionsToKeep() {
        return maxSessionsToKeep;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultUserSessionStoreConfiguration that = (DefaultUserSessionStoreConfiguration) o;
        return Objects.equals(aerospikeSessionNamespace, that.aerospikeSessionNamespace) &&
                Objects.equals(aerospikeSessionSetName, that.aerospikeSessionSetName) &&
                Objects.equals(sessionExpirationSeconds, that.sessionExpirationSeconds) &&
                Objects.equals(maxSessionsToKeep, that.maxSessionsToKeep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aerospikeSessionNamespace,
                aerospikeSessionSetName,
                sessionExpirationSeconds,
                maxSessionsToKeep);
    }

    @Override
    public String toString() {
        return "DefaultUserSessionStoreConfiguration{" +
                "aerospikeSessionNamespace='" + aerospikeSessionNamespace + '\'' +
                ", aerospikeSessionSetName='" + aerospikeSessionSetName + '\'' +
                ", sessionExpirationSeconds=" + sessionExpirationSeconds +
                ", maxSessionsToKeep=" + maxSessionsToKeep +
                '}';
    }

    /**
     * Builder for {@link DefaultUserSessionStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String AEROSPIKE_SESSION_NAMESPACE = "dev";
        private static final String AEROSPIKE_SESSION_SET_NAME = "appbase-session";
        private static final Long SESSION_EXPIRATION_SECONDS = Duration.ofHours(1).toSeconds();
        private static final Integer MAX_SESSIONS_TO_KEEP = 10;

        private String aerospikeSessionNamespace = AEROSPIKE_SESSION_NAMESPACE;
        private String aerospikeSessionSetName = AEROSPIKE_SESSION_SET_NAME;
        private Long sessionExpirationSeconds = SESSION_EXPIRATION_SECONDS;
        private Integer maxSessionsToKeep = MAX_SESSIONS_TO_KEEP;

        private Builder() {
        }

        public Builder aerospikeSessionNamespace(final String aerospikeSessionNamespace) {
            this.aerospikeSessionNamespace = aerospikeSessionNamespace;
            return this;
        }

        public Builder aerospikeSessionSetName(final String aerospikeSessionSetName) {
            this.aerospikeSessionSetName = aerospikeSessionSetName;
            return this;
        }

        public Builder sessionExpirationSeconds(final Long sessionExpirationSeconds) {
            this.sessionExpirationSeconds = sessionExpirationSeconds;
            return this;
        }

        public Builder maxSessionsToKeep(final Integer maxSessionsToKeep) {
            this.maxSessionsToKeep = maxSessionsToKeep;
            return this;
        }

        public DefaultUserSessionStoreConfiguration build() {
            return new DefaultUserSessionStoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
