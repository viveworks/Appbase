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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration representing {@link org.apache.http.client.config.RequestConfig}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RequestConfiguration.Builder.class)
public final class RequestConfiguration {
    private final boolean expectContinueEnabled;
    private final boolean staleConnectionCheckEnabled;
    private final boolean redirectsEnabled;
    private final boolean relativeRedirectsAllowed;
    private final boolean circularRedirectsAllowed;
    private final int maxRedirects;
    private final int connectionRequestTimeout;
    private final int connectTimeout;
    private final int socketTimeout;
    private final boolean contentCompressionEnabled;
    private final boolean normalizeUri;

    private RequestConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.maxRedirects, "builder.maxRedirects");
        AppbaseUtil.positiveOrZero(builder.connectionRequestTimeout, "builder.connectionRequestTimeout");
        AppbaseUtil.positiveOrZero(builder.connectTimeout, "builder.connectTimeout");
        AppbaseUtil.positiveOrZero(builder.socketTimeout, "builder.socketTimeout");

        expectContinueEnabled = builder.expectContinueEnabled;
        staleConnectionCheckEnabled = builder.staleConnectionCheckEnabled;
        redirectsEnabled = builder.redirectsEnabled;
        relativeRedirectsAllowed = builder.relativeRedirectsAllowed;
        circularRedirectsAllowed = builder.circularRedirectsAllowed;
        maxRedirects = builder.maxRedirects;
        connectionRequestTimeout = builder.connectionRequestTimeout;
        connectTimeout = builder.connectTimeout;
        socketTimeout = builder.socketTimeout;
        contentCompressionEnabled = builder.contentCompressionEnabled;
        normalizeUri = builder.normalizeUri;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isExpectContinueEnabled() {
        return expectContinueEnabled;
    }

    public boolean isStaleConnectionCheckEnabled() {
        return staleConnectionCheckEnabled;
    }

    public boolean isRedirectsEnabled() {
        return redirectsEnabled;
    }

    public boolean isRelativeRedirectsAllowed() {
        return relativeRedirectsAllowed;
    }

    public boolean isCircularRedirectsAllowed() {
        return circularRedirectsAllowed;
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public boolean isContentCompressionEnabled() {
        return contentCompressionEnabled;
    }

    public boolean isNormalizeUri() {
        return normalizeUri;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RequestConfiguration that = (RequestConfiguration) o;
        return expectContinueEnabled == that.expectContinueEnabled &&
                staleConnectionCheckEnabled == that.staleConnectionCheckEnabled &&
                redirectsEnabled == that.redirectsEnabled &&
                relativeRedirectsAllowed == that.relativeRedirectsAllowed &&
                circularRedirectsAllowed == that.circularRedirectsAllowed &&
                maxRedirects == that.maxRedirects &&
                connectionRequestTimeout == that.connectionRequestTimeout &&
                connectTimeout == that.connectTimeout &&
                socketTimeout == that.socketTimeout &&
                contentCompressionEnabled == that.contentCompressionEnabled &&
                normalizeUri == that.normalizeUri;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                expectContinueEnabled,
                staleConnectionCheckEnabled,
                redirectsEnabled,
                relativeRedirectsAllowed,
                circularRedirectsAllowed,
                maxRedirects,
                connectionRequestTimeout,
                connectTimeout,
                socketTimeout,
                contentCompressionEnabled,
                normalizeUri);
    }

    @Override
    public String toString() {
        return "RequestConfiguration{" +
                "expectContinueEnabled=" + expectContinueEnabled +
                ", staleConnectionCheckEnabled=" + staleConnectionCheckEnabled +
                ", redirectsEnabled=" + redirectsEnabled +
                ", relativeRedirectsAllowed=" + relativeRedirectsAllowed +
                ", circularRedirectsAllowed=" + circularRedirectsAllowed +
                ", maxRedirects=" + maxRedirects +
                ", connectionRequestTimeout=" + connectionRequestTimeout +
                ", connectTimeout=" + connectTimeout +
                ", socketTimeout=" + socketTimeout +
                ", contentCompressionEnabled=" + contentCompressionEnabled +
                ", normalizeUri=" + normalizeUri +
                '}';
    }


    /**
     * Builder for {@link RequestConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_EXPECT_CONTINUE_ENABLED = true;
        private static final boolean DEFAULT_STALE_CONNECTION_CHECK_ENABLED = false;
        private static final boolean DEFAULT_REDIRECTS_ENABLED = true;
        private static final boolean DEFAULT_RELATIVE_REDIRECTS_ALLOWED = true;
        private static final boolean DEFAULT_CIRCULAR_REDIRECTS_ALLOWED = false;
        private static final int DEFAULT_MAX_REDIRECTS = 50;
        private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 1000;
        private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
        private static final int DEFAULT_SOCKET_TIMEOUT = 30000;
        private static final boolean DEFAULT_CONTENT_COMPRESSION_ENABLED = true;
        private static final boolean DEFAULT_NORMALIZE_URI = true;


        private boolean expectContinueEnabled = DEFAULT_EXPECT_CONTINUE_ENABLED;
        private boolean staleConnectionCheckEnabled = DEFAULT_STALE_CONNECTION_CHECK_ENABLED;
        private boolean redirectsEnabled = DEFAULT_REDIRECTS_ENABLED;
        private boolean relativeRedirectsAllowed = DEFAULT_RELATIVE_REDIRECTS_ALLOWED;
        private boolean circularRedirectsAllowed = DEFAULT_CIRCULAR_REDIRECTS_ALLOWED;
        private int maxRedirects = DEFAULT_MAX_REDIRECTS;
        private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        private boolean contentCompressionEnabled = DEFAULT_CONTENT_COMPRESSION_ENABLED;
        private boolean normalizeUri = DEFAULT_NORMALIZE_URI;


        private Builder() {
        }

        public Builder expectContinueEnabled(final boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public Builder staleConnectionCheckEnabled(final boolean staleConnectionCheckEnabled) {
            this.staleConnectionCheckEnabled = staleConnectionCheckEnabled;
            return this;
        }

        public Builder redirectsEnabled(final boolean redirectsEnabled) {
            this.redirectsEnabled = redirectsEnabled;
            return this;
        }

        public Builder relativeRedirectsAllowed(final boolean relativeRedirectsAllowed) {
            this.relativeRedirectsAllowed = relativeRedirectsAllowed;
            return this;
        }

        public Builder circularRedirectsAllowed(final boolean circularRedirectsAllowed) {
            this.circularRedirectsAllowed = circularRedirectsAllowed;
            return this;
        }

        public Builder maxRedirects(final int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return this;
        }

        public Builder connectionRequestTimeout(final int connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        public Builder connectTimeout(final int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder socketTimeout(final int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder contentCompressionEnabled(final boolean contentCompressionEnabled) {
            this.contentCompressionEnabled = contentCompressionEnabled;
            return this;
        }

        public Builder normalizeUri(final boolean normalizeUri) {
            this.normalizeUri = normalizeUri;
            return this;
        }

        public RequestConfiguration build() {
            return new RequestConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
