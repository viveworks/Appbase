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
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullWhenEnabled;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link org.apache.http.impl.nio.client.HttpAsyncClientBuilder}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = HttpAsyncClientConfiguration.Builder.class)
public final class HttpAsyncClientConfiguration {
    private final boolean enabled;

    @Nullable
    private final String userAgent;

    private final boolean sharedConnectionManager;

    private final int maxConnectionsPerRoute;

    private final int totalMaxConnections;

    @NotNullWhenEnabled
    private final ThreadFactoryConfiguration threadFactoryConfiguration;



    private HttpAsyncClientConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.enabled) {
            AppbaseUtil.notNull(builder.threadFactoryConfiguration, "builder.threadFactoryConfiguration");
        }

        enabled = builder.enabled;
        userAgent = builder.userAgent;
        sharedConnectionManager = builder.sharedConnectionManager;
        maxConnectionsPerRoute = builder.maxConnectionsPerRoute;
        totalMaxConnections = builder.totalMaxConnections;
        threadFactoryConfiguration = builder.threadFactoryConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public String getUserAgent() {
        return userAgent;
    }

    public boolean isSharedConnectionManager() {
        return sharedConnectionManager;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public int getTotalMaxConnections() {
        return totalMaxConnections;
    }

    @NotNullWhenEnabled
    public ThreadFactoryConfiguration getThreadFactoryConfiguration() {
        return threadFactoryConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HttpAsyncClientConfiguration that = (HttpAsyncClientConfiguration) o;
        return enabled == that.enabled &&
                sharedConnectionManager == that.sharedConnectionManager &&
                maxConnectionsPerRoute == that.maxConnectionsPerRoute &&
                totalMaxConnections == that.totalMaxConnections &&
                Objects.equals(userAgent, that.userAgent) &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                userAgent,
                sharedConnectionManager,
                maxConnectionsPerRoute,
                totalMaxConnections,
                threadFactoryConfiguration);
    }

    @Override
    public String toString() {
        return "HttpAsyncClientConfiguration{" +
                "enabled=" + enabled +
                ", userAgent='" + userAgent + '\'' +
                ", sharedConnectionManager=" + sharedConnectionManager +
                ", maxConnectionsPerRoute=" + maxConnectionsPerRoute +
                ", totalMaxConnections=" + totalMaxConnections +
                ", threadFactoryConfiguration=" + threadFactoryConfiguration +
                '}';
    }


    /**
     * Builder for {@link HttpAsyncClientConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final String DEFAULT_USER_AGENT = null;
        private static final boolean DEFAULT_SHARED_CONNECTION_MANAGER = false;
        private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 10;
        private static final int DEFAULT_TOTAL_MAX_CONNECTIONS = 30;
        private static final ThreadFactoryConfiguration DEFAULT_THREAD_FACTORY_CONFIGURATION = ThreadFactoryConfiguration
                .builder()
                .enableThreadAffinity(true)
                .nameFormat("appbase-elasticsearch-httpclient-%s")
                .daemon(false)
                .build();



        private boolean enabled = DEFAULT_ENABLED;

        @Nullable
        private String userAgent = DEFAULT_USER_AGENT;

        private boolean sharedConnectionManager = DEFAULT_SHARED_CONNECTION_MANAGER;

        private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;

        private int totalMaxConnections = DEFAULT_TOTAL_MAX_CONNECTIONS;

        @NotNullWhenEnabled
        private ThreadFactoryConfiguration threadFactoryConfiguration = DEFAULT_THREAD_FACTORY_CONFIGURATION;


        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder userAgent(@Nullable final String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder sharedConnectionManager(final boolean sharedConnectionManager) {
            this.sharedConnectionManager = sharedConnectionManager;
            return this;
        }

        public Builder maxConnectionsPerRoute(final int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            return this;
        }

        public Builder totalMaxConnections(final int totalMaxConnections) {
            this.totalMaxConnections = totalMaxConnections;
            return this;
        }

        public Builder threadFactoryConfiguration(
                @NotNullWhenEnabled
                final ThreadFactoryConfiguration threadFactoryConfiguration)
        {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public HttpAsyncClientConfiguration build() {
            return new HttpAsyncClientConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
