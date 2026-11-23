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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.service.vitess.CustomGrpcClientFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link CustomGrpcClientFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = CustomGrpcClientFactoryConfiguration.Builder.class)
public final class CustomGrpcClientFactoryConfiguration {
    private final RetryingInterceptorConfiguration retryingInterceptorConfiguration;
    private final Boolean useTracing;
    private final ContextConfiguration contextConfiguration;
    private final NettyChannelConfiguration nettyChannelConfiguration;
    private final TargetConfiguration targetConfiguration;
    private final TlsOptionsConfiguration tlsOptionsConfiguration;
    private final CallCredentialsConfiguration callCredentialsConfiguration;
    private final LoadBalancerFactoryConfiguration loadBalancerFactoryConfiguration;
    private final NameResolverFactoryConfiguration nameResolverFactoryConfiguration;


    private CustomGrpcClientFactoryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.contextConfiguration, "builder.contextConfiguration");
        AppbaseUtil.notNull(builder.retryingInterceptorConfiguration, "builder.retryingInterceptorConfiguration");
        AppbaseUtil.notNull(builder.nettyChannelConfiguration, "builder.nettyChannelConfiguration");
        AppbaseUtil.notNull(builder.targetConfiguration, "builder.targetConfiguration");

        retryingInterceptorConfiguration = builder.retryingInterceptorConfiguration;
        useTracing = builder.useTracing;
        contextConfiguration = builder.contextConfiguration;
        nettyChannelConfiguration = builder.nettyChannelConfiguration;
        targetConfiguration = builder.targetConfiguration;
        tlsOptionsConfiguration = builder.tlsOptionsConfiguration;
        callCredentialsConfiguration = builder.callCredentialsConfiguration;
        loadBalancerFactoryConfiguration = builder.loadBalancerFactoryConfiguration;
        nameResolverFactoryConfiguration = builder.nameResolverFactoryConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public RetryingInterceptorConfiguration getRetryingInterceptorConfiguration() {
        return retryingInterceptorConfiguration;
    }

    public Boolean getUseTracing() {
        return useTracing;
    }

    public ContextConfiguration getContextConfiguration() {
        return contextConfiguration;
    }

    public NettyChannelConfiguration getNettyChannelConfiguration() {
        return nettyChannelConfiguration;
    }

    public TargetConfiguration getTargetConfiguration() {
        return targetConfiguration;
    }

    public TlsOptionsConfiguration getTlsOptionsConfiguration() {
        return tlsOptionsConfiguration;
    }

    public CallCredentialsConfiguration getCallCredentialsConfiguration() {
        return callCredentialsConfiguration;
    }

    public LoadBalancerFactoryConfiguration getLoadBalancerFactoryConfiguration() {
        return loadBalancerFactoryConfiguration;
    }

    public NameResolverFactoryConfiguration getNameResolverFactoryConfiguration() {
        return nameResolverFactoryConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CustomGrpcClientFactoryConfiguration that = (CustomGrpcClientFactoryConfiguration) o;
        return Objects.equals(useTracing, that.useTracing) &&
                Objects.equals(retryingInterceptorConfiguration, that.retryingInterceptorConfiguration) &&
                Objects.equals(contextConfiguration, that.contextConfiguration) &&
                Objects.equals(nettyChannelConfiguration, that.nettyChannelConfiguration) &&
                Objects.equals(targetConfiguration, that.targetConfiguration) &&
                Objects.equals(tlsOptionsConfiguration, that.tlsOptionsConfiguration) &&
                Objects.equals(callCredentialsConfiguration, that.callCredentialsConfiguration) &&
                Objects.equals(loadBalancerFactoryConfiguration, that.loadBalancerFactoryConfiguration) &&
                Objects.equals(nameResolverFactoryConfiguration, that.nameResolverFactoryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                retryingInterceptorConfiguration,
                useTracing,
                contextConfiguration,
                nettyChannelConfiguration,
                targetConfiguration,
                tlsOptionsConfiguration,
                callCredentialsConfiguration,
                loadBalancerFactoryConfiguration,
                nameResolverFactoryConfiguration);
    }

    @Override
    public String toString() {
        return "CustomGrpcClientFactoryConfiguration{" +
                "retryingInterceptorConfiguration=" + retryingInterceptorConfiguration +
                ", useTracing=" + useTracing +
                ", contextConfiguration=" + contextConfiguration +
                ", nettyChannelConfiguration=" + nettyChannelConfiguration +
                ", targetConfiguration=" + targetConfiguration +
                ", tlsOptionsConfiguration=" + tlsOptionsConfiguration +
                ", callCredentialsConfiguration=" + callCredentialsConfiguration +
                ", loadBalancerFactoryConfiguration=" + loadBalancerFactoryConfiguration +
                ", nameResolverFactoryConfiguration=" + nameResolverFactoryConfiguration +
                '}';
    }


    /**
     * Builder for {@link CustomGrpcClientFactoryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final RetryingInterceptorConfiguration DEFAULT_RETRYING_INTERCEPTOR_CONFIGURATION =
                RetryingInterceptorConfiguration.builder().build();
        private static final boolean DEFAULT_USE_TRACING = true;
        private static final ContextConfiguration DEFAULT_CONTEXT_CONFIGURATION = ContextConfiguration.builder().build();
        private static final NettyChannelConfiguration DEFAULT_NETTY_CHANNEL_CONFIGURATION =
                NettyChannelConfiguration.builder().build();
        private static final TargetConfiguration DEFAULT_TARGET_CONFIGURATION = TargetConfiguration.builder().build();
        private static final CallCredentialsConfiguration DEFAULT_CALL_CREDENTIALS_CONFIGURATION =
                CallCredentialsConfiguration.builder().build();
        private static final LoadBalancerFactoryConfiguration DEFAULT_LOAD_BALANCER_FACTORY_CONFIGURATION = null;
        private static final NameResolverFactoryConfiguration DEFAULT_NAME_RESOLVER_FACTORY_CONFIGURATION = null;
        private static final TlsOptionsConfiguration DEFAULT_TLS_OPTIONS_CONFIGURATION = null;


        private RetryingInterceptorConfiguration retryingInterceptorConfiguration =
                DEFAULT_RETRYING_INTERCEPTOR_CONFIGURATION;
        private Boolean useTracing = DEFAULT_USE_TRACING;
        private ContextConfiguration contextConfiguration = DEFAULT_CONTEXT_CONFIGURATION;
        private NettyChannelConfiguration nettyChannelConfiguration = DEFAULT_NETTY_CHANNEL_CONFIGURATION;
        private TargetConfiguration targetConfiguration = DEFAULT_TARGET_CONFIGURATION;
        private CallCredentialsConfiguration callCredentialsConfiguration = DEFAULT_CALL_CREDENTIALS_CONFIGURATION;
        private LoadBalancerFactoryConfiguration loadBalancerFactoryConfiguration =
                DEFAULT_LOAD_BALANCER_FACTORY_CONFIGURATION;
        private NameResolverFactoryConfiguration nameResolverFactoryConfiguration =
                DEFAULT_NAME_RESOLVER_FACTORY_CONFIGURATION;
        private TlsOptionsConfiguration tlsOptionsConfiguration = DEFAULT_TLS_OPTIONS_CONFIGURATION;


        private Builder() {
        }

        public Builder retryingInterceptorConfiguration(
                final RetryingInterceptorConfiguration retryingInterceptorConfiguration)
        {
            this.retryingInterceptorConfiguration = retryingInterceptorConfiguration;
            return this;
        }

        public Builder useTracing(final Boolean useTracing) {
            this.useTracing = useTracing;
            return this;
        }

        public Builder contextConfiguration(final ContextConfiguration contextConfiguration) {
            this.contextConfiguration = contextConfiguration;
            return this;
        }

        public Builder nettyChannelConfiguration(final NettyChannelConfiguration nettyChannelConfiguration) {
            this.nettyChannelConfiguration = nettyChannelConfiguration;
            return this;
        }

        public Builder targetConfiguration(final TargetConfiguration targetConfiguration) {
            this.targetConfiguration = targetConfiguration;
            return this;
        }

        public Builder callCredentialsConfiguration(final CallCredentialsConfiguration callCredentialsConfiguration) {
            this.callCredentialsConfiguration = callCredentialsConfiguration;
            return this;
        }

        public Builder loadBalancerFactoryConfiguration(
                final LoadBalancerFactoryConfiguration loadBalancerFactoryConfiguration)
        {
            this.loadBalancerFactoryConfiguration = loadBalancerFactoryConfiguration;
            return this;
        }

        public Builder nameResolverFactoryConfiguration(
                final NameResolverFactoryConfiguration nameResolverFactoryConfiguration)
        {
            this.nameResolverFactoryConfiguration = nameResolverFactoryConfiguration;
            return this;
        }

        public Builder tlsOptionsConfiguration(final TlsOptionsConfiguration tlsOptionsConfiguration) {
            this.tlsOptionsConfiguration = tlsOptionsConfiguration;
            return this;
        }

        public CustomGrpcClientFactoryConfiguration build() {
            return new CustomGrpcClientFactoryConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
