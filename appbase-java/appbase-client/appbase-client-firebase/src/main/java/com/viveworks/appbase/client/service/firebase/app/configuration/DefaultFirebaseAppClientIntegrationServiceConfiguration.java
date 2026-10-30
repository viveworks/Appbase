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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.firebase.ThreadManager;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.firebase.app.DefaultFirebaseAppClientIntegrationService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseDatabaseAuthVariableOverrideFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultFirebaseAppClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultFirebaseAppClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultFirebaseAppClientIntegrationServiceConfiguration
        implements FirebaseAppClientIntegrationServiceConfiguration
{
    @NullableConditionally(dependsOn = "projectId or serviceAccountId")
    private final TextValueConfiguration textValueKeyConfiguration;
    private final String projectId;
    private final boolean emulatedEnvironment;
    private final String serviceAccountId;
    private final TimeoutConfiguration connectTimeoutConfiguration;
    private final TimeoutConfiguration readTimeoutConfiguration;
    private final GoogleCredentialsConfiguration googleCredentialsConfiguration;
    private final String databaseUrl;
    private final String storageBucket;

    private final InjectedTypeConfiguration<? extends FirebaseDatabaseAuthVariableOverrideFactory>
            firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration;

    private final boolean useDefaultJacksonFactory;
    private final InjectedTypeConfiguration<? extends JsonFactory> jsonFactoryInjectedTypeConfiguration;

    private final NetHttpTransportConfiguration netHttpTransportConfiguration;
    private final InjectedTypeConfiguration<? extends HttpTransport> httpTransportInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<? extends ThreadManager> threadManagerInjectedTypeConfiguration;
    private final CustomFirebaseThreadManagerConfiguration customFirebaseThreadManagerConfiguration;


    private DefaultFirebaseAppClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.textValueKeyConfiguration == null) {
            AppbaseUtil.notBlank(builder.projectId, "builder.projectId");
        }

        if (builder.projectId == null) {
            AppbaseUtil.notNull(builder.textValueKeyConfiguration, "builder.textValueKeyConfiguration");
        }

        if (builder.threadManagerInjectedTypeConfiguration == null) {
            AppbaseUtil.notNull(
                    builder.customFirebaseThreadManagerConfiguration,
                    "builder.customFirebaseThreadManagerConfiguration");
        }

        if (builder.customFirebaseThreadManagerConfiguration == null) {
            AppbaseUtil.notNull(
                    builder.threadManagerInjectedTypeConfiguration,
                    "builder.threadManagerInjectedTypeConfiguration");
        }

        emulatedEnvironment = builder.emulatedEnvironment;
        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        projectId = builder.projectId;
        serviceAccountId = builder.serviceAccountId;
        connectTimeoutConfiguration = builder.connectTimeoutConfiguration;
        readTimeoutConfiguration = builder.readTimeoutConfiguration;
        googleCredentialsConfiguration = builder.googleCredentialsConfiguration;
        databaseUrl = builder.databaseUrl;
        firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration =
                builder.firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration;
        storageBucket = builder.storageBucket;

        useDefaultJacksonFactory = builder.useDefaultJacksonFactory;
        jsonFactoryInjectedTypeConfiguration = builder.jsonFactoryInjectedTypeConfiguration;

        netHttpTransportConfiguration = builder.netHttpTransportConfiguration;
        httpTransportInjectedTypeConfiguration = builder.httpTransportInjectedTypeConfiguration;

        threadManagerInjectedTypeConfiguration = builder.threadManagerInjectedTypeConfiguration;
        customFirebaseThreadManagerConfiguration = builder.customFirebaseThreadManagerConfiguration;
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

    public String getProjectId() {
        return projectId;
    }

    public boolean isEmulatedEnvironment() {
        return emulatedEnvironment;
    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public TimeoutConfiguration getConnectTimeoutConfiguration() {
        return connectTimeoutConfiguration;
    }

    public TimeoutConfiguration getReadTimeoutConfiguration() {
        return readTimeoutConfiguration;
    }

    public GoogleCredentialsConfiguration getGoogleCredentialsConfiguration() {
        return googleCredentialsConfiguration;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public InjectedTypeConfiguration<? extends FirebaseDatabaseAuthVariableOverrideFactory>
    getFirebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration()
    {
        return firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public boolean isUseDefaultJacksonFactory() {
        return useDefaultJacksonFactory;
    }

    public InjectedTypeConfiguration<? extends JsonFactory> getJsonFactoryInjectedTypeConfiguration() {
        return jsonFactoryInjectedTypeConfiguration;
    }

    public NetHttpTransportConfiguration getNetHttpTransportConfiguration() {
        return netHttpTransportConfiguration;
    }

    public InjectedTypeConfiguration<? extends HttpTransport> getHttpTransportInjectedTypeConfiguration() {
        return httpTransportInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<? extends ThreadManager> getThreadManagerInjectedTypeConfiguration() {
        return threadManagerInjectedTypeConfiguration;
    }

    public CustomFirebaseThreadManagerConfiguration getCustomFirebaseThreadManagerConfiguration() {
        return customFirebaseThreadManagerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultFirebaseAppClientIntegrationServiceConfiguration that =
                (DefaultFirebaseAppClientIntegrationServiceConfiguration) o;
        return emulatedEnvironment == that.emulatedEnvironment &&
                useDefaultJacksonFactory == that.useDefaultJacksonFactory &&
                Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(projectId, that.projectId) &&
                Objects.equals(serviceAccountId, that.serviceAccountId) &&
                Objects.equals(connectTimeoutConfiguration, that.connectTimeoutConfiguration) &&
                Objects.equals(readTimeoutConfiguration, that.readTimeoutConfiguration) &&
                Objects.equals(googleCredentialsConfiguration, that.googleCredentialsConfiguration) &&
                Objects.equals(databaseUrl, that.databaseUrl) &&
                Objects.equals(
                        firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration,
                        that.firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration) &&
                Objects.equals(storageBucket, that.storageBucket) &&
                Objects.equals(jsonFactoryInjectedTypeConfiguration, that.jsonFactoryInjectedTypeConfiguration) &&
                Objects.equals(netHttpTransportConfiguration, that.netHttpTransportConfiguration) &&
                Objects.equals(httpTransportInjectedTypeConfiguration, that.httpTransportInjectedTypeConfiguration) &&
                Objects.equals(threadManagerInjectedTypeConfiguration, that.threadManagerInjectedTypeConfiguration) &&
                Objects.equals(customFirebaseThreadManagerConfiguration, that.customFirebaseThreadManagerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration,
                projectId,
                emulatedEnvironment,
                serviceAccountId,
                connectTimeoutConfiguration,
                readTimeoutConfiguration,
                googleCredentialsConfiguration,
                databaseUrl,
                firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration,
                storageBucket,
                useDefaultJacksonFactory,
                jsonFactoryInjectedTypeConfiguration,
                netHttpTransportConfiguration,
                httpTransportInjectedTypeConfiguration,
                threadManagerInjectedTypeConfiguration,
                customFirebaseThreadManagerConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultFirebaseAppClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", projectId='" + projectId + '\'' +
                ", emulatedEnvironment=" + emulatedEnvironment +
                ", serviceAccountId='" + serviceAccountId + '\'' +
                ", connectTimeoutConfiguration=" + connectTimeoutConfiguration +
                ", readTimeoutConfiguration=" + readTimeoutConfiguration +
                ", googleCredentialsConfiguration=" + AppbaseUtil.OBFUSCATE +
                ", databaseUrl='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration=" +
                firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration +
                ", storageBucket='" + storageBucket + '\'' +
                ", createJacksonFactory=" + useDefaultJacksonFactory +
                ", jsonFactoryInjectedTypeConfiguration=" + jsonFactoryInjectedTypeConfiguration +
                ", netHttpTransportConfiguration=" + netHttpTransportConfiguration +
                ", httpTransportInjectedTypeConfiguration=" + httpTransportInjectedTypeConfiguration +
                ", threadManagerInjectedTypeConfiguration=" + threadManagerInjectedTypeConfiguration +
                ", customFirebaseThreadManagerConfiguration=" + customFirebaseThreadManagerConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultFirebaseAppClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_PROJECT_ID = "appbase-project";
        private static final boolean DEFAULT_EMULATED_ENVIRONMENT = true;
        private static final TimeoutConfiguration DEFAULT_CONNECT_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(100)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_READ_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(500)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final boolean DEFAULT_USE_DEFAULT_JACKSON_FACTORY = true;
        private static final NetHttpTransportConfiguration DEFAULT_NET_HTTP_TRANSPORT_CONFIGURATION =
                NetHttpTransportConfiguration.builder().build();
        private static final CustomFirebaseThreadManagerConfiguration DEFAULT_CUSTOM_FIREBASE_THREAD_MANAGER_CONFIGURATION =
                CustomFirebaseThreadManagerConfiguration.builder().build();


        private TextValueConfiguration textValueKeyConfiguration;
        private String projectId = DEFAULT_PROJECT_ID;
        private boolean emulatedEnvironment = DEFAULT_EMULATED_ENVIRONMENT;
        private String serviceAccountId;
        private TimeoutConfiguration connectTimeoutConfiguration = DEFAULT_CONNECT_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration readTimeoutConfiguration = DEFAULT_READ_TIMEOUT_CONFIGURATION;
        private GoogleCredentialsConfiguration googleCredentialsConfiguration;
        private String databaseUrl;
        private String storageBucket;

        private InjectedTypeConfiguration<? extends FirebaseDatabaseAuthVariableOverrideFactory>
                firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration;

        private boolean useDefaultJacksonFactory = DEFAULT_USE_DEFAULT_JACKSON_FACTORY;
        private InjectedTypeConfiguration<? extends JsonFactory> jsonFactoryInjectedTypeConfiguration;

        private NetHttpTransportConfiguration netHttpTransportConfiguration =
                DEFAULT_NET_HTTP_TRANSPORT_CONFIGURATION;
        private InjectedTypeConfiguration<? extends HttpTransport> httpTransportInjectedTypeConfiguration;

        private InjectedTypeConfiguration<? extends ThreadManager> threadManagerInjectedTypeConfiguration;
        private CustomFirebaseThreadManagerConfiguration customFirebaseThreadManagerConfiguration =
                DEFAULT_CUSTOM_FIREBASE_THREAD_MANAGER_CONFIGURATION;


        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder projectId(final String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder emulatedEnvironment(final boolean emulatedEnvironment) {
            this.emulatedEnvironment = emulatedEnvironment;
            return this;
        }

        public Builder serviceAccountId(final String serviceAccountId) {
            this.serviceAccountId = serviceAccountId;
            return this;
        }

        public Builder connectTimeoutConfiguration(final TimeoutConfiguration connectTimeoutConfiguration) {
            this.connectTimeoutConfiguration = connectTimeoutConfiguration;
            return this;
        }

        public Builder readTimeoutConfiguration(final TimeoutConfiguration readTimeoutConfiguration) {
            this.readTimeoutConfiguration = readTimeoutConfiguration;
            return this;
        }

        public Builder googleCredentialsConfiguration(final GoogleCredentialsConfiguration googleCredentialsConfiguration) {
            this.googleCredentialsConfiguration = googleCredentialsConfiguration;
            return this;
        }

        public Builder databaseUrl(final String databaseUrl) {
            this.databaseUrl = databaseUrl;
            return this;
        }

        public Builder firebaseDatabaseAuthVariableOverrideFactory(
                final InjectedTypeConfiguration<? extends FirebaseDatabaseAuthVariableOverrideFactory>
                        firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration)
        {
            this.firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration =
                    firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder storageBucket(final String storageBucket) {
            this.storageBucket = storageBucket;
            return this;
        }

        @JsonProperty("useDefaultJacksonFactory")
        public Builder createJacksonFactory(final boolean createJacksonFactory) {
            this.useDefaultJacksonFactory = createJacksonFactory;
            return this;
        }

        public Builder jsonFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends JsonFactory> jsonFactoryInjectedTypeConfiguration)
        {
            this.jsonFactoryInjectedTypeConfiguration = jsonFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder netHttpTransportConfiguration(
                final NetHttpTransportConfiguration netHttpTransportConfiguration)
        {
            this.netHttpTransportConfiguration = netHttpTransportConfiguration;
            return this;
        }

        public Builder httpTransportInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends HttpTransport> httpTransportInjectedTypeConfiguration)
        {
            this.httpTransportInjectedTypeConfiguration = httpTransportInjectedTypeConfiguration;
            return this;
        }

        public Builder threadManagerInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends ThreadManager> threadManagerInjectedTypeConfiguration)
        {
            this.threadManagerInjectedTypeConfiguration = threadManagerInjectedTypeConfiguration;
            return this;
        }

        public Builder customFirebaseThreadManagerConfiguration(
                final CustomFirebaseThreadManagerConfiguration customFirebaseThreadManagerConfiguration)
        {
            this.customFirebaseThreadManagerConfiguration = customFirebaseThreadManagerConfiguration;
            return this;
        }

        public DefaultFirebaseAppClientIntegrationServiceConfiguration build() {
            return new DefaultFirebaseAppClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
