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

package com.viveworks.appbase.client.service.jdbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for {@code database URL} of the form:
 * <code>jdbc:<em>subprotocol</em>:<em>subname</em>?propertyKey1=propertyValue1&amp;propertyKey2=propertyValue2</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = JdbcUrlConfiguration.Builder.class)
public final class JdbcUrlConfiguration {
    private final String rdbmsName;

    private final HostPortConfiguration hostPortConfiguration;

    private final String databaseName;

    private final String user;

    private final String password;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> additionalProperties;

    private JdbcUrlConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.rdbmsName, "builder.rdbmsName");
        AppbaseUtil.notNull(builder.hostPortConfiguration, "builder.hostPortConfiguration");

        if (builder.user != null || builder.password != null || builder.additionalProperties != null) {
            AppbaseUtil.notBlank(builder.databaseName, "builder.databaseName");
        }

        rdbmsName = builder.rdbmsName;
        hostPortConfiguration = builder.hostPortConfiguration;
        databaseName = builder.databaseName;
        user = builder.user;
        password = builder.password;
        additionalProperties = builder.additionalProperties;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getRdbmsName() {
        return rdbmsName;
    }

    public HostPortConfiguration getHostPortConfiguration() {
        return hostPortConfiguration;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JdbcUrlConfiguration that = (JdbcUrlConfiguration) o;
        return Objects.equals(rdbmsName, that.rdbmsName) &&
                Objects.equals(hostPortConfiguration, that.hostPortConfiguration) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(user, that.user) &&
                Objects.equals(password, that.password) &&
                Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rdbmsName, hostPortConfiguration, databaseName, user, password, additionalProperties);
    }

    @Override
    public String toString() {
        return "JdbcUrlConfiguration{" +
                "rdbmsName='" + rdbmsName + '\'' +
                ", hostPortConfiguration=" + hostPortConfiguration +
                ", databaseName='" + databaseName + '\'' +
                ", user='" + user + '\'' +
                ", password='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", additionalProperties=" + AppbaseUtil.OBFUSCATE +
                '}';
    }

    /**
     * Builder for {@link JdbcUrlConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String rdbmsName;

        private HostPortConfiguration hostPortConfiguration;

        private String databaseName;

        private String user;

        private String password;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> additionalProperties;


        private Builder() {
        }

        public Builder rdbmsName(final String rdbmsName) {
            this.rdbmsName = rdbmsName;
            return this;
        }

        public Builder hostPortConfiguration(final HostPortConfiguration hostPortConfiguration) {
            this.hostPortConfiguration = hostPortConfiguration;
            return this;
        }

        public Builder databaseName(final String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder user(final String user) {
            this.user = user;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder additionalProperties(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> additionalProperties)
        {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public JdbcUrlConfiguration build() {
            return new JdbcUrlConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
