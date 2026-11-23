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

package com.viveworks.appbase.client.service.smtp.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.smtp.DefaultSmtpClientIntegrationService;
import com.viveworks.appbase.client.util.BlockingOperationConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultSmtpClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultSmtpClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultSmtpClientIntegrationServiceConfiguration implements SmtpClientIntegrationServiceConfiguration {
    private final TextValueConfiguration textValueKeyConfiguration;
    private final SmtpSessionFactoryConfiguration smtpSessionFactoryConfiguration;
    private final SmtpSessionConfiguration smtpSessionConfiguration;
    private final String senderDomainName;
    private final boolean startTls;
    private final SmtpAuthConfiguration smtpAuthConfiguration;
    private final BlockingOperationConfiguration connectBlockingOperationConfiguration;
    private final BlockingOperationConfiguration ehloRequestBlockingOperationConfiguration;
    private final BlockingOperationConfiguration startTlsBlockingOperationConfiguration;
    private final BlockingOperationConfiguration authBlockingOperationConfiguration;
    private final BlockingOperationConfiguration closeBlockingOperationConfiguration;


    private DefaultSmtpClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.textValueKeyConfiguration, "builder.textValueKeyConfiguration");
        AppbaseUtil.notNull(builder.smtpSessionFactoryConfiguration, "builder.smtpSessionFactoryConfiguration");
        AppbaseUtil.notNull(builder.smtpSessionConfiguration, "builder.smtpSessionConfiguration");
        AppbaseUtil.notNull(builder.senderDomainName, "builder.senderDomainName");

        AppbaseUtil.notNull(builder.connectBlockingOperationConfiguration, "builder.connectBlockingOperationConfiguration");
        AppbaseUtil.notFalse(
                builder.connectBlockingOperationConfiguration.isEnabled(),
                "builder.connectBlockingOperationConfiguration.isEnabled()");

        AppbaseUtil.notNull(
                builder.ehloRequestBlockingOperationConfiguration, "builder.ehloRequestBlockingOperationConfiguration");
        AppbaseUtil.notFalse(
                builder.ehloRequestBlockingOperationConfiguration.isEnabled(),
                "builder.ehloRequestBlockingOperationConfiguration.isEnabled()");

        AppbaseUtil.notNull(builder.closeBlockingOperationConfiguration, "builder.closeBlockingOperationConfiguration");
        AppbaseUtil.notFalse(
                builder.closeBlockingOperationConfiguration.isEnabled(),
                "builder.closeBlockingOperationConfiguration.isEnabled()");

        if (builder.startTls) {
            AppbaseUtil.notNull(
                    builder.startTlsBlockingOperationConfiguration, "startTlsBlockingOperationConfiguration");
        }

        if (builder.smtpAuthConfiguration != null) {
            AppbaseUtil.notNull(
                    builder.authBlockingOperationConfiguration, "builder.authBlockingOperationConfiguration");
        }

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        smtpSessionFactoryConfiguration = builder.smtpSessionFactoryConfiguration;
        smtpSessionConfiguration = builder.smtpSessionConfiguration;
        senderDomainName = builder.senderDomainName;
        startTls = builder.startTls;
        smtpAuthConfiguration = builder.smtpAuthConfiguration;
        connectBlockingOperationConfiguration = builder.connectBlockingOperationConfiguration;
        ehloRequestBlockingOperationConfiguration = builder.ehloRequestBlockingOperationConfiguration;
        startTlsBlockingOperationConfiguration = builder.startTlsBlockingOperationConfiguration;
        authBlockingOperationConfiguration = builder.authBlockingOperationConfiguration;
        closeBlockingOperationConfiguration = builder.closeBlockingOperationConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    public SmtpSessionFactoryConfiguration getSmtpSessionFactoryConfiguration() {
        return smtpSessionFactoryConfiguration;
    }

    public SmtpSessionConfiguration getSmtpSessionConfiguration() {
        return smtpSessionConfiguration;
    }

    public boolean isStartTls() {
        return startTls;
    }

    public String getSenderDomainName() {
        return senderDomainName;
    }

    public SmtpAuthConfiguration getSmtpAuthConfiguration() {
        return smtpAuthConfiguration;
    }

    public BlockingOperationConfiguration getConnectBlockingOperationConfiguration() {
        return connectBlockingOperationConfiguration;
    }

    public BlockingOperationConfiguration getEhloRequestBlockingOperationConfiguration() {
        return ehloRequestBlockingOperationConfiguration;
    }

    public BlockingOperationConfiguration getStartTlsBlockingOperationConfiguration() {
        return startTlsBlockingOperationConfiguration;
    }

    public BlockingOperationConfiguration getAuthBlockingOperationConfiguration() {
        return authBlockingOperationConfiguration;
    }

    public BlockingOperationConfiguration getCloseBlockingOperationConfiguration() {
        return closeBlockingOperationConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultSmtpClientIntegrationServiceConfiguration that = (DefaultSmtpClientIntegrationServiceConfiguration) o;
        return startTls == that.startTls &&
                Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(smtpSessionFactoryConfiguration, that.smtpSessionFactoryConfiguration) &&
                Objects.equals(smtpSessionConfiguration, that.smtpSessionConfiguration) &&
                Objects.equals(senderDomainName, that.senderDomainName) &&
                Objects.equals(smtpAuthConfiguration, that.smtpAuthConfiguration) &&
                Objects.equals(connectBlockingOperationConfiguration, that.connectBlockingOperationConfiguration) &&
                Objects.equals(ehloRequestBlockingOperationConfiguration, that.ehloRequestBlockingOperationConfiguration) &&
                Objects.equals(startTlsBlockingOperationConfiguration, that.startTlsBlockingOperationConfiguration) &&
                Objects.equals(authBlockingOperationConfiguration, that.authBlockingOperationConfiguration) &&
                Objects.equals(closeBlockingOperationConfiguration, that.closeBlockingOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration,
                smtpSessionFactoryConfiguration,
                smtpSessionConfiguration,
                senderDomainName,
                startTls,
                smtpAuthConfiguration,
                connectBlockingOperationConfiguration,
                ehloRequestBlockingOperationConfiguration,
                startTlsBlockingOperationConfiguration,
                authBlockingOperationConfiguration,
                closeBlockingOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultSmtpClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", smtpSessionFactoryConfiguration=" + smtpSessionFactoryConfiguration +
                ", smtpSessionConfiguration=" + smtpSessionConfiguration +
                ", senderDomainName=" + senderDomainName +
                ", startTls=" + startTls +
                ", smtpAuthConfiguration=" + smtpAuthConfiguration +
                ", connectBlockingOperationConfiguration=" + connectBlockingOperationConfiguration +
                ", ehloRequestBlockingOperationConfiguration=" + ehloRequestBlockingOperationConfiguration +
                ", startTlsBlockingOperationConfiguration=" + startTlsBlockingOperationConfiguration +
                ", authBlockingOperationConfiguration=" + authBlockingOperationConfiguration +
                ", closeBlockingOperationConfiguration=" + closeBlockingOperationConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultSmtpClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final TextValueConfiguration DEFAULT_TEXT_VALUE_KEY_CONFIGURATION = TextValueConfiguration.PRIMARY;
        private static final SmtpSessionFactoryConfiguration DEFAULT_SMTP_SESSION_FACTORY_CONFIGURATION =
                SmtpSessionFactoryConfiguration.builder().build();
        private static final SmtpSessionConfiguration DEFAULT_SMTP_SESSION_CONFIGURATION =
                SmtpSessionConfiguration.builder().build();
        private static final String DEFAULT_SENDER_DOMAIN_NAME = "devenv.local";
        private static final boolean DEFAULT_START_TLS = false;
        private static final SmtpAuthConfiguration DEFAULT_SMTP_AUTH_CONFIGURATION =
                SmtpAuthConfiguration.builder().build();
        private static final BlockingOperationConfiguration DEFAULT_CONNECT_BLOCKING_OPERATION_CONFIGURATION =
                BlockingOperationConfiguration.builder().build();
        private static final BlockingOperationConfiguration DEFAULT_EHLO_REQUEST_BLOCKING_OPERATION_CONFIGURATION =
                BlockingOperationConfiguration.builder().build();
        private static final BlockingOperationConfiguration DEFAULT_AUTH_BLOCKING_OPERATION_CONFIGURATION =
                BlockingOperationConfiguration.builder().build();
        private static final BlockingOperationConfiguration DEFAULT_START_TLS_BLOCKING_OPERATION_CONFIGURATION =
                BlockingOperationConfiguration.builder().build();
        private static final BlockingOperationConfiguration DEFAULT_CLOSE_BLOCKING_OPERATION_CONFIGURATION =
                BlockingOperationConfiguration.builder().build();


        private TextValueConfiguration textValueKeyConfiguration = DEFAULT_TEXT_VALUE_KEY_CONFIGURATION;
        private SmtpSessionFactoryConfiguration smtpSessionFactoryConfiguration = DEFAULT_SMTP_SESSION_FACTORY_CONFIGURATION;
        private SmtpSessionConfiguration smtpSessionConfiguration = DEFAULT_SMTP_SESSION_CONFIGURATION;
        private String senderDomainName = DEFAULT_SENDER_DOMAIN_NAME;
        private boolean startTls = DEFAULT_START_TLS;
        private SmtpAuthConfiguration smtpAuthConfiguration = DEFAULT_SMTP_AUTH_CONFIGURATION;
        private BlockingOperationConfiguration connectBlockingOperationConfiguration =
                DEFAULT_CONNECT_BLOCKING_OPERATION_CONFIGURATION;
        private BlockingOperationConfiguration ehloRequestBlockingOperationConfiguration =
                DEFAULT_EHLO_REQUEST_BLOCKING_OPERATION_CONFIGURATION;
        private BlockingOperationConfiguration authBlockingOperationConfiguration =
                DEFAULT_AUTH_BLOCKING_OPERATION_CONFIGURATION;
        private BlockingOperationConfiguration startTlsBlockingOperationConfiguration =
                DEFAULT_START_TLS_BLOCKING_OPERATION_CONFIGURATION;
        private BlockingOperationConfiguration closeBlockingOperationConfiguration =
                DEFAULT_CLOSE_BLOCKING_OPERATION_CONFIGURATION;


        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder smtpSessionFactoryConfiguration(
                final SmtpSessionFactoryConfiguration smtpSessionFactoryConfiguration)
        {
            this.smtpSessionFactoryConfiguration = smtpSessionFactoryConfiguration;
            return this;
        }

        public Builder smtpSessionConfiguration(final SmtpSessionConfiguration smtpSessionConfiguration) {
            this.smtpSessionConfiguration = smtpSessionConfiguration;
            return this;
        }

        public Builder senderDomainName(final String senderDomainName) {
            this.senderDomainName = senderDomainName;
            return this;
        }

        public Builder startTls(final boolean startTls) {
            this.startTls = startTls;
            return this;
        }

        public Builder smtpAuthConfiguration(final SmtpAuthConfiguration smtpAuthConfiguration) {
            this.smtpAuthConfiguration = smtpAuthConfiguration;
            return this;
        }

        public Builder connectBlockingOperationConfiguration(
                final BlockingOperationConfiguration connectBlockingOperationConfiguration)
        {
            this.connectBlockingOperationConfiguration = connectBlockingOperationConfiguration;
            return this;
        }

        public Builder ehloRequestBlockingOperationConfiguration(
                final BlockingOperationConfiguration ehloRequestBlockingOperationConfiguration)
        {
            this.ehloRequestBlockingOperationConfiguration = ehloRequestBlockingOperationConfiguration;
            return this;
        }

        public Builder startTlsBlockingOperationConfiguration(
                final BlockingOperationConfiguration startTlsBlockingOperationConfiguration)
        {
            this.startTlsBlockingOperationConfiguration = startTlsBlockingOperationConfiguration;
            return this;
        }

        public Builder authBlockingOperationConfiguration(
                final BlockingOperationConfiguration authBlockingOperationConfiguration)
        {
            this.authBlockingOperationConfiguration = authBlockingOperationConfiguration;
            return this;
        }

        public Builder closeBlockingOperationConfiguration(
                final BlockingOperationConfiguration closeBlockingOperationConfiguration)
        {
            this.closeBlockingOperationConfiguration = closeBlockingOperationConfiguration;
            return this;
        }

        public DefaultSmtpClientIntegrationServiceConfiguration build() {
            return new DefaultSmtpClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
