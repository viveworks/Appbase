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
package com.viveworks.appbase.client.cloud.aws.httpclient.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.Objects;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize
public class AwsSdkApacheProxyConfiguration {
    private final URI endpoint;
    private final String username;
    private final String password;
    private final String ntlmDomain;
    private final String ntlmWorkstation;
    private final Set<String> nonProxyHosts;
    private final Boolean preemptiveBasicAuthenticationEnabled;
    private final Boolean useSystemPropertyValues;

    private AwsSdkApacheProxyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        endpoint = builder.endpoint;
        username = builder.username;
        password = builder.password;
        ntlmDomain = builder.ntlmDomain;
        ntlmWorkstation = builder.ntlmWorkstation;
        nonProxyHosts = builder.nonProxyHosts;
        preemptiveBasicAuthenticationEnabled = builder.preemptiveBasicAuthenticationEnabled;
        useSystemPropertyValues = builder.useSystemPropertyValues;
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
     * Returns the configured endpoint.
     *
     * @return the configured endpoint
     */
    public URI getEndpoint() {
        return endpoint;
    }

    /**
     * Returns the configured username.
     *
     * @return the configured username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the configured password.
     *
     * @return the configured password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the configured ntlm domain.
     *
     * @return the configured ntlm domain
     */
    public String getNtlmDomain() {
        return ntlmDomain;
    }

    /**
     * Returns the configured ntlm workstation.
     *
     * @return the configured ntlm workstation
     */
    public String getNtlmWorkstation() {
        return ntlmWorkstation;
    }

    /**
     * Returns the configured non proxy hosts.
     *
     * @return the configured non proxy hosts
     */
    public Set<String> getNonProxyHosts() {
        return nonProxyHosts;
    }

    /**
     * Returns the configured preemptive basic authentication enabled.
     *
     * @return the configured preemptive basic authentication enabled
     */
    public Boolean getPreemptiveBasicAuthenticationEnabled() {
        return preemptiveBasicAuthenticationEnabled;
    }

    /**
     * Returns the configured use system property values.
     *
     * @return the configured use system property values
     */
    public Boolean getUseSystemPropertyValues() {
        return useSystemPropertyValues;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkApacheProxyConfiguration that = (AwsSdkApacheProxyConfiguration) o;
        return Objects.equals(endpoint, that.endpoint) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(ntlmDomain, that.ntlmDomain) &&
                Objects.equals(ntlmWorkstation, that.ntlmWorkstation) &&
                Objects.equals(nonProxyHosts, that.nonProxyHosts) &&
                Objects.equals(preemptiveBasicAuthenticationEnabled, that.preemptiveBasicAuthenticationEnabled) &&
                Objects.equals(useSystemPropertyValues, that.useSystemPropertyValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint,
                username,
                password,
                ntlmDomain,
                ntlmWorkstation,
                nonProxyHosts,
                preemptiveBasicAuthenticationEnabled,
                useSystemPropertyValues);
    }

    @Override
    public String toString() {
        // AUD-34/L4-06: endpoint (may embed userinfo credentials in its URI) and nonProxyHosts
        // (an open-ended collection) are redacted alongside password, matching the broader
        // redaction internal work item 2558 established for this same class after review found a single-field
        // redaction insufficient.
        return "AwsSdkApacheProxyConfiguration{" +
                "endpoint=" + AppbaseUtil.OBFUSCATE +
                ", username='" + username + '\'' +
                ", password='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", ntlmDomain='" + ntlmDomain + '\'' +
                ", ntlmWorkstation='" + ntlmWorkstation + '\'' +
                ", nonProxyHosts=" + AppbaseUtil.OBFUSCATE +
                ", preemptiveBasicAuthenticationEnabled=" + preemptiveBasicAuthenticationEnabled +
                ", useSystemPropertyValues=" + useSystemPropertyValues +
                '}';
    }

    /**
     * Builder for {@link AwsSdkApacheProxyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private URI endpoint;
        private String username;
        private String password;
        private String ntlmDomain;
        private String ntlmWorkstation;
        private Set<String> nonProxyHosts;
        private Boolean preemptiveBasicAuthenticationEnabled;
        private Boolean useSystemPropertyValues;

        private Builder() {
        }

        public Builder endpoint(final URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder ntlmDomain(final String ntlmDomain) {
            this.ntlmDomain = ntlmDomain;
            return this;
        }

        public Builder ntlmWorkstation(final String ntlmWorkstation) {
            this.ntlmWorkstation = ntlmWorkstation;
            return this;
        }

        public Builder nonProxyHosts(final Set<String> nonProxyHosts) {
            this.nonProxyHosts = nonProxyHosts;
            return this;
        }

        public Builder preemptiveBasicAuthenticationEnabled(
                final Boolean preemptiveBasicAuthenticationEnabled)
        {
            this.preemptiveBasicAuthenticationEnabled = preemptiveBasicAuthenticationEnabled;
            return this;
        }

        public Builder useSystemPropertyValues(final Boolean useSystemPropertyValues) {
            this.useSystemPropertyValues = useSystemPropertyValues;
            return this;
        }

        public AwsSdkApacheProxyConfiguration build() {
            return new AwsSdkApacheProxyConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
