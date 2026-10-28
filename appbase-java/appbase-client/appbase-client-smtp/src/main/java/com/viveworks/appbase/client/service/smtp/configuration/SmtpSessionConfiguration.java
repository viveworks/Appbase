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

package com.viveworks.appbase.client.service.smtp.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.hubspot.smtp.client.SendInterceptor;
import com.hubspot.smtp.client.SmtpSessionConfig;
import com.viveworks.appbase.client.service.smtp.SmtpExceptionHandler;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link SmtpSessionConfig}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SmtpSessionConfiguration.Builder.class)
public final class SmtpSessionConfiguration {
    private final HostPortConfiguration localAddressHostPortConfiguration;
    private final HostPortConfiguration remoteAddressHostPortConfiguration;
    private final String connectionId;
    private final Set<SmtpExtension> disabledSmtpExtensions;
    private final TimeoutConfiguration initialResponseTimeoutConfiguration;
    private final TimeoutConfiguration keepAliveTimeoutConfiguration;
    private final TimeoutConfiguration readTimeoutConfiguration;
    private final InjectedTypeConfiguration<SmtpExceptionHandler> smtpExceptionHandlerInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<SendInterceptor> sendInterceptorInjectedTypeConfiguration;


    private SmtpSessionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.remoteAddressHostPortConfiguration, "builder.remoteAddressConfiguration");

        localAddressHostPortConfiguration = builder.localAddressHostPortConfiguration;
        remoteAddressHostPortConfiguration = builder.remoteAddressHostPortConfiguration;
        connectionId = builder.connectionId;
        disabledSmtpExtensions = builder.disabledSmtpExtensions;
        initialResponseTimeoutConfiguration = builder.initialResponseTimeoutConfiguration;
        keepAliveTimeoutConfiguration = builder.keepAliveTimeoutConfiguration;
        readTimeoutConfiguration = builder.readTimeoutConfiguration;
        smtpExceptionHandlerInjectedTypeConfiguration = builder.smtpExceptionHandlerInjectedTypeConfiguration;
        sendInterceptorInjectedTypeConfiguration = builder.sendInterceptorInjectedTypeConfiguration;
    }


    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public HostPortConfiguration getLocalAddressHostPortConfiguration() {
        return localAddressHostPortConfiguration;
    }

    public HostPortConfiguration getRemoteAddressHostPortConfiguration() {
        return remoteAddressHostPortConfiguration;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Set<SmtpExtension> getDisabledSmtpExtensions() {
        return disabledSmtpExtensions;
    }

    public TimeoutConfiguration getInitialResponseTimeoutConfiguration() {
        return initialResponseTimeoutConfiguration;
    }

    public TimeoutConfiguration getKeepAliveTimeoutConfiguration() {
        return keepAliveTimeoutConfiguration;
    }

    public TimeoutConfiguration getReadTimeoutConfiguration() {
        return readTimeoutConfiguration;
    }

    public InjectedTypeConfiguration<SmtpExceptionHandler> getSmtpExceptionHandlerInjectedTypeConfiguration() {
        return smtpExceptionHandlerInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<SendInterceptor> getSendInterceptorInjectedTypeConfiguration() {
        return sendInterceptorInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SmtpSessionConfiguration that = (SmtpSessionConfiguration) o;
        return Objects.equals(localAddressHostPortConfiguration, that.localAddressHostPortConfiguration) &&
                Objects.equals(remoteAddressHostPortConfiguration, that.remoteAddressHostPortConfiguration) &&
                Objects.equals(connectionId, that.connectionId) &&
                Objects.equals(disabledSmtpExtensions, that.disabledSmtpExtensions) &&
                Objects.equals(initialResponseTimeoutConfiguration, that.initialResponseTimeoutConfiguration) &&
                Objects.equals(keepAliveTimeoutConfiguration, that.keepAliveTimeoutConfiguration) &&
                Objects.equals(readTimeoutConfiguration, that.readTimeoutConfiguration) &&
                Objects.equals(
                        smtpExceptionHandlerInjectedTypeConfiguration,
                        that.smtpExceptionHandlerInjectedTypeConfiguration) &&
                Objects.equals(sendInterceptorInjectedTypeConfiguration, that.sendInterceptorInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                localAddressHostPortConfiguration,
                remoteAddressHostPortConfiguration,
                connectionId,
                disabledSmtpExtensions,
                initialResponseTimeoutConfiguration,
                keepAliveTimeoutConfiguration,
                readTimeoutConfiguration,
                smtpExceptionHandlerInjectedTypeConfiguration,
                sendInterceptorInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "SmtpSessionConfiguration{" +
                "localAddressConfiguration=" + localAddressHostPortConfiguration +
                ", remoteAddressConfiguration=" + remoteAddressHostPortConfiguration +
                ", connectionId='" + connectionId + '\'' +
                ", disabledSmtpExtensions=" + disabledSmtpExtensions +
                ", initialResponseTimeoutConfiguration=" + initialResponseTimeoutConfiguration +
                ", keepAliveTimeoutConfiguration=" + keepAliveTimeoutConfiguration +
                ", readTimeoutConfiguration=" + readTimeoutConfiguration +
                ", smtpExceptionHandlerInjectedTypeConfiguration=" + smtpExceptionHandlerInjectedTypeConfiguration +
                ", sendInterceptorInjectedTypeConfiguration=" + sendInterceptorInjectedTypeConfiguration +
                '}';
    }


    /**
     * Builder for {@link SmtpSessionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final HostPortConfiguration DEFAULT_REMOTE_ADDRESS_HOST_PORT_CONFIGURATION =
                HostPortConfiguration
                        .builder()
                        .host("postfix.local")
                        .port(25)
                        .build();
        private static final TimeoutConfiguration DEFAULT_INITIAL_RESPONSE_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeoutTimeUnit(TimeUnit.SECONDS)
                        .timeout(10)
                        .build();
        private static final TimeoutConfiguration DEFAULT_KEEP_ALIVE_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(1)
                        .timeoutTimeUnit(TimeUnit.HOURS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_READ_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(5)
                        .timeoutTimeUnit(TimeUnit.SECONDS)
                        .build();


        private HostPortConfiguration localAddressHostPortConfiguration;
        private HostPortConfiguration remoteAddressHostPortConfiguration = DEFAULT_REMOTE_ADDRESS_HOST_PORT_CONFIGURATION;
        private String connectionId;
        private Set<SmtpExtension> disabledSmtpExtensions;
        private TimeoutConfiguration initialResponseTimeoutConfiguration = DEFAULT_INITIAL_RESPONSE_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration keepAliveTimeoutConfiguration = DEFAULT_KEEP_ALIVE_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration readTimeoutConfiguration = DEFAULT_READ_TIMEOUT_CONFIGURATION;
        private InjectedTypeConfiguration<SmtpExceptionHandler> smtpExceptionHandlerInjectedTypeConfiguration;
        private InjectedTypeConfiguration<SendInterceptor> sendInterceptorInjectedTypeConfiguration;


        private Builder() {
        }

        public Builder localAddressHostPortConfiguration(final HostPortConfiguration localAddressHostPortConfiguration) {
            this.localAddressHostPortConfiguration = localAddressHostPortConfiguration;
            return this;
        }

        public Builder remoteAddressHostPortConfiguration(final HostPortConfiguration remoteAddressHostPortConfiguration) {
            this.remoteAddressHostPortConfiguration = remoteAddressHostPortConfiguration;
            return this;
        }

        public Builder connectionId(final String connectionId) {
            this.connectionId = connectionId;
            return this;
        }

        public Builder disabledSmtpExtensions(final Set<SmtpExtension> disabledSmtpExtensions) {
            this.disabledSmtpExtensions = disabledSmtpExtensions;
            return this;
        }

        public Builder initialResponseTimeoutConfiguration(final TimeoutConfiguration initialResponseTimeoutConfiguration) {
            this.initialResponseTimeoutConfiguration = initialResponseTimeoutConfiguration;
            return this;
        }

        public Builder keepAliveTimeoutConfiguration(final TimeoutConfiguration keepAliveTimeoutConfiguration) {
            this.keepAliveTimeoutConfiguration = keepAliveTimeoutConfiguration;
            return this;
        }

        public Builder readTimeoutConfiguration(final TimeoutConfiguration readTimeoutConfiguration) {
            this.readTimeoutConfiguration = readTimeoutConfiguration;
            return this;
        }

        public Builder smtpExceptionHandlerInjectedTypeConfiguration(
                final InjectedTypeConfiguration<SmtpExceptionHandler> smtpExceptionHandlerInjectedTypeConfiguration)
        {
            this.smtpExceptionHandlerInjectedTypeConfiguration = smtpExceptionHandlerInjectedTypeConfiguration;
            return this;
        }

        public Builder sendInterceptorInjectedTypeConfiguration(
                final InjectedTypeConfiguration<SendInterceptor> sendInterceptorInjectedTypeConfiguration)
        {
            this.sendInterceptorInjectedTypeConfiguration = sendInterceptorInjectedTypeConfiguration;
            return this;
        }

        public SmtpSessionConfiguration build() {
            return new SmtpSessionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
