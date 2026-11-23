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

package com.viveworks.appbase.client.cloud.aws.s3;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.AppbaseClientRuntimeException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkClientAsyncConfigurationFactory;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkClientOverrideConfigurationFactory;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkCredentialsProviderFactory;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkProfileFileFactory;
import com.viveworks.appbase.client.cloud.aws.common.DefaultAwsSdkClientAsyncConfigurationFactory;
import com.viveworks.appbase.client.cloud.aws.common.DefaultAwsSdkClientOverrideConfigurationFactory;
import com.viveworks.appbase.client.cloud.aws.common.DefaultAwsSdkCredentialsProviderFactory;
import com.viveworks.appbase.client.cloud.aws.common.DefaultAwsSdkProfileFileFactory;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkClientAsyncConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkClientOverrideConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkCredentialsProviderConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkProfileFileConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkRegionConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.AwsSdkS3Configuration;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.DefaultS3ClientsIntegrationServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link S3ClientsIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultS3ClientsIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <S3Clients, ClusterIdValue, DefaultS3ClientsIntegrationServiceConfiguration>
        implements S3ClientsIntegrationService
{
    private final AtomicReference<S3Clients> s3ClientsRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final AwsSdkHttpClientService awsSdkHttpClientService;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultS3ClientsIntegrationServiceConfiguration defaultS3ClientsIntegrationServiceConfiguration;


    /**
     * Creates a DefaultS3ClientsIntegrationService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<S3Clients, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final S3Client s3Client,
            final S3AsyncClient s3AsyncClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, S3Clients.class);

        AppbaseUtil.notNull(s3Client, "s3Client");
        AppbaseUtil.notNull(s3AsyncClient, "s3AsyncClient");

        final S3Clients s3Clients = new DefaultS3Clients(s3Client, s3AsyncClient);
        this.s3ClientsRef = new AtomicReference<>(s3Clients);
        this.awsSdkHttpClientService = null;
        this.awsSdkAsyncHttpClientService = null;
        this.defaultS3ClientsIntegrationServiceConfiguration = null;
    }

    /**
     * Creates a DefaultS3ClientsIntegrationService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<S3Clients, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultS3ClientsIntegrationServiceConfiguration defaultS3ClientsIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, S3Clients.class);

        AppbaseUtil.notNull(
                defaultS3ClientsIntegrationServiceConfiguration, "defaultS3ClientsIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                defaultS3ClientsIntegrationServiceConfiguration.getCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(),
                "defaultS3ClientsIntegrationServiceConfiguration" +
                        ".getCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration()");
        AppbaseUtil.notNull(
                defaultS3ClientsIntegrationServiceConfiguration.getCustomDefaultAwsSdkHttpClientServiceConfiguration(),
                "defaultS3ClientsIntegrationServiceConfiguration" +
                        ".getCustomDefaultAwsSdkHttpClientServiceConfiguration()");

        this.s3ClientsRef = new AtomicReference<>();
        this.awsSdkHttpClientService = null;
        this.awsSdkAsyncHttpClientService = null;
        this.defaultS3ClientsIntegrationServiceConfiguration = defaultS3ClientsIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    /**
     * Creates a DefaultS3ClientsIntegrationService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<S3Clients, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final DefaultS3ClientsIntegrationServiceConfiguration defaultS3ClientsIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, S3Clients.class);

        AppbaseUtil.notNull(awsSdkHttpClientService, "awsSdkHttpClientService");
        AppbaseUtil.notNull(awsSdkAsyncHttpClientService, "awsSdkAsyncHttpClientService");
        AppbaseUtil.notNull(
                defaultS3ClientsIntegrationServiceConfiguration, "defaultS3ClientsIntegrationServiceConfiguration");

        this.s3ClientsRef = new AtomicReference<>();
        this.awsSdkHttpClientService = awsSdkHttpClientService;
        this.awsSdkAsyncHttpClientService = awsSdkAsyncHttpClientService;
        this.defaultS3ClientsIntegrationServiceConfiguration = defaultS3ClientsIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultS3ClientsIntegrationServiceConfiguration,
                "defaultS3ClientsIntegrationServiceConfiguration");

        final S3ClientBuilder s3ClientBuilder = S3Client.builder();
        final S3AsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient.builder();


        final TextValueConfiguration awsSdkHttpClientIntegrationServiceKeyConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkHttpClientIntegrationServiceKeyConfiguration();
        final DefaultAwsSdkHttpClientServiceConfiguration customDefaultAwsSdkHttpClientServiceConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getCustomDefaultAwsSdkHttpClientServiceConfiguration();
        final SdkHttpClient sdkHttpClient = sdkHttpClient(
                awsSdkHttpClientIntegrationServiceKeyConfiguration,
                customDefaultAwsSdkHttpClientServiceConfiguration);
        AppbaseUtil.notNull(sdkHttpClient, "sdkHttpClient");
        s3ClientBuilder.httpClient(sdkHttpClient);

        final TextValueConfiguration awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkAsyncHttpClientIntegrationServiceKeyConfiguration();
        final DefaultAwsSdkAsyncHttpClientServiceConfiguration customDefaultAwsSdkAsyncHttpClientServiceConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration();
        final SdkAsyncHttpClient sdkAsyncHttpClient = sdkAsyncHttpClient(
                awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration,
                customDefaultAwsSdkAsyncHttpClientServiceConfiguration);
        AppbaseUtil.notNull(sdkAsyncHttpClient, "sdkAsyncHttpClient");
        s3AsyncClientBuilder.httpClient(sdkAsyncHttpClient);


        final AwsSdkS3Configuration awsSdkS3Configuration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkS3Configuration();
        final S3Configuration s3Configuration = s3Configuration(awsSdkS3Configuration);
        AppbaseUtil.notNull(s3Configuration, "s3Configuration");
        s3ClientBuilder.serviceConfiguration(s3Configuration);
        s3AsyncClientBuilder.serviceConfiguration(s3Configuration);


        final AwsSdkRegionConfiguration awsSdkRegionConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkRegionConfiguration();
        if (awsSdkRegionConfiguration != null) {
            final Region region = region(awsSdkRegionConfiguration);
            AppbaseUtil.notNull(region, "region");
            s3ClientBuilder.region(region);
            s3AsyncClientBuilder.region(region);
        }


        final AwsSdkClientAsyncConfiguration awsSdkClientAsyncConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkClientAsyncConfiguration();
        if (awsSdkClientAsyncConfiguration != null) {
            final ClientAsyncConfiguration clientAsyncConfiguration =
                    clientAsyncConfiguration(awsSdkClientAsyncConfiguration);
            s3AsyncClientBuilder.asyncConfiguration(clientAsyncConfiguration);
        }


        final AwsSdkClientOverrideConfiguration awsSdkClientOverrideConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkClientOverrideConfiguration();
        if (awsSdkClientOverrideConfiguration != null) {
            final ClientOverrideConfiguration clientOverrideConfiguration =
                    clientOverrideConfiguration(awsSdkClientOverrideConfiguration);
            s3ClientBuilder.overrideConfiguration(clientOverrideConfiguration);
            s3AsyncClientBuilder.overrideConfiguration(clientOverrideConfiguration);
        }


        final AwsSdkCredentialsProviderConfiguration awsSdkCredentialsProviderConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkCredentialsProviderConfiguration();
        if (awsSdkCredentialsProviderConfiguration != null) {
            final AwsCredentialsProvider awsCredentialsProvider = awsCredentialsProvider(awsSdkCredentialsProviderConfiguration);
            s3ClientBuilder.credentialsProvider(awsCredentialsProvider);
            s3AsyncClientBuilder.credentialsProvider(awsCredentialsProvider);
        }


        final String endpointOverrideUri =
                defaultS3ClientsIntegrationServiceConfiguration.getEndpointOverrideUri();
        if (endpointOverrideUri != null) {
            final URI uri = URI.create(endpointOverrideUri);
            s3ClientBuilder.endpointOverride(uri);
            s3AsyncClientBuilder.endpointOverride(uri);
        }


        final S3Client s3Client = s3ClientBuilder.build();
        // NOTE: The connection is made lazily. This call ensures that the sync. client is actually connected.
        // For some reason, this initial connection (and auth negotiation) is needed for the async. client to work
        // properly. Without this, the async. client throws an authentication exception. Maybe, a later version of the
        // client will not have this problem. [FIXME]
        s3Client.listBuckets();

        this.s3ClientsRef.set(new DefaultS3Clients(s3Client, s3AsyncClientBuilder.build()));
    }

    private SdkHttpClient sdkHttpClient(
            final TextValueConfiguration awsSdkHttpClientIntegrationServiceKeyConfiguration,
            final DefaultAwsSdkHttpClientServiceConfiguration customDefaultAwsSdkHttpClientServiceConfiguration)
    {
        if (awsSdkHttpClientService != null) {
            return sdkHttpClient(awsSdkHttpClientService, awsSdkHttpClientIntegrationServiceKeyConfiguration);
        }

        final AwsSdkHttpClientService localAwsSdkHttpClientService =
                new DefaultAwsSdkHttpClientService(appbaseClientCore, customDefaultAwsSdkHttpClientServiceConfiguration);
        return sdkHttpClient(localAwsSdkHttpClientService, awsSdkHttpClientIntegrationServiceKeyConfiguration);
    }

    private SdkHttpClient sdkHttpClient(
            final AwsSdkHttpClientService localAwsSdkHttpClientService,
            final TextValueConfiguration awsSdkHttpClientIntegrationServiceKeyConfiguration)
    {
        if (awsSdkHttpClientIntegrationServiceKeyConfiguration == null) {
            return localAwsSdkHttpClientService.getClient();
        }

        final AppbaseClientKey<TextValue> appbaseClientKey =
                AppbaseClientUtil.textValueKey(awsSdkHttpClientIntegrationServiceKeyConfiguration);
        return localAwsSdkHttpClientService.getClientIntegrationService(appbaseClientKey).getClient();
    }


    private SdkAsyncHttpClient sdkAsyncHttpClient(
            final TextValueConfiguration awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration,
            final DefaultAwsSdkAsyncHttpClientServiceConfiguration customDefaultAwsSdkAsyncHttpClientServiceConfiguration)
    {
        if (awsSdkAsyncHttpClientService != null) {
            return sdkAsyncHttpClient(awsSdkAsyncHttpClientService, awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration);
        }

        final AwsSdkAsyncHttpClientService localAwsSdkAsyncHttpClientService =
                new DefaultAwsSdkAsyncHttpClientService(
                        appbaseClientCore, customDefaultAwsSdkAsyncHttpClientServiceConfiguration);
        return sdkAsyncHttpClient(
                localAwsSdkAsyncHttpClientService, awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration);
    }


    private SdkAsyncHttpClient sdkAsyncHttpClient(
            final AwsSdkAsyncHttpClientService localAwsSdkAsyncHttpClientService,
            final TextValueConfiguration awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration)
    {
        if (awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration == null) {
            return localAwsSdkAsyncHttpClientService.getClient();
        }

        final AppbaseClientKey<TextValue> appbaseClientKey =
                AppbaseClientUtil.textValueKey(awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration);
        return localAwsSdkAsyncHttpClientService.getClientIntegrationService(appbaseClientKey).getClient();
    }


    private ClientAsyncConfiguration clientAsyncConfiguration(
            final AwsSdkClientAsyncConfiguration awsSdkClientAsyncConfiguration)
    {
        AppbaseUtil.notNull(awsSdkClientAsyncConfiguration, "awsSdkClientAsyncConfiguration");

        final AwsSdkClientAsyncConfigurationFactory awsSdkClientAsyncConfigurationFactory =
                new DefaultAwsSdkClientAsyncConfigurationFactory(appbaseClientCore, awsSdkClientAsyncConfiguration);
        return awsSdkClientAsyncConfigurationFactory.getInstance();
    }


    private ClientOverrideConfiguration clientOverrideConfiguration(
            final AwsSdkClientOverrideConfiguration awsSdkClientOverrideConfiguration)
    {
        AppbaseUtil.notNull(awsSdkClientOverrideConfiguration, "awsSdkClientOverrideConfiguration");

        final AwsSdkClientOverrideConfigurationFactory awsSdkClientOverrideConfigurationFactory =
                new DefaultAwsSdkClientOverrideConfigurationFactory(appbaseClientCore, awsSdkClientOverrideConfiguration);
        return awsSdkClientOverrideConfigurationFactory.getInstance();
    }


    private AwsCredentialsProvider awsCredentialsProvider(
            final AwsSdkCredentialsProviderConfiguration awsSdkCredentialsProviderConfiguration)
    {
        AppbaseUtil.notNull(awsSdkCredentialsProviderConfiguration, "awsSdkCredentialsProviderConfiguration");

        final AwsSdkCredentialsProviderFactory awsSdkCredentialsProviderFactory =
                new DefaultAwsSdkCredentialsProviderFactory(awsSdkCredentialsProviderConfiguration);
        return awsSdkCredentialsProviderFactory.getInstance();
    }


    private Region region(final AwsSdkRegionConfiguration awsSdkRegionConfiguration) {
        AppbaseUtil.notNull(awsSdkRegionConfiguration, "awsSdkRegionConfiguration");

        final String id = awsSdkRegionConfiguration.getId();
        final boolean globalRegion = awsSdkRegionConfiguration.isGlobalRegion();

        if (globalRegion) {
            if (id.equals(Region.AWS_GLOBAL.id())) {
                return Region.AWS_GLOBAL;
            } else if (id.equals(Region.AWS_CN_GLOBAL.id())) {
                return Region.AWS_CN_GLOBAL;
            } else if (id.equals(Region.AWS_US_GOV_GLOBAL.id())) {
                return Region.AWS_US_GOV_GLOBAL;
            } else if (id.equals(Region.AWS_ISO_GLOBAL.id())) {
                return Region.AWS_ISO_GLOBAL;
            } else if (id.equals(Region.AWS_ISO_B_GLOBAL.id())) {
                return Region.AWS_ISO_B_GLOBAL;
            } else {
                throw new AppbaseClientRuntimeException("Unknown global region: " + awsSdkRegionConfiguration);
            }
        }

        return Region.of(id);
    }


    private S3Configuration s3Configuration(final AwsSdkS3Configuration awsSdkS3Configuration) {
        AppbaseUtil.notNull(awsSdkS3Configuration, "awsSdkS3Configuration");

        final S3Configuration.Builder builder = S3Configuration.builder();


        final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration =
                awsSdkS3Configuration.getAwsSdkProfileFileConfiguration();
        if (awsSdkProfileFileConfiguration != null) {
            final AwsSdkProfileFileFactory awsSdkProfileFileFactory =
                    new DefaultAwsSdkProfileFileFactory(awsSdkProfileFileConfiguration);
            final ProfileFile profileFile = awsSdkProfileFileFactory.getInstance();
            builder.profileFile(profileFile);
        }


        final String profileName = awsSdkS3Configuration.getProfileName();
        if (profileName != null) {
            builder.profileName(profileName);
        }


        final Boolean accelerateModeEnabled = awsSdkS3Configuration.getAccelerateModeEnabled();
        if (accelerateModeEnabled != null) {
            builder.accelerateModeEnabled(accelerateModeEnabled);
        }


        final Boolean checksumValidationEnabled = awsSdkS3Configuration.getChecksumValidationEnabled();
        if (checksumValidationEnabled != null) {
            builder.checksumValidationEnabled(checksumValidationEnabled);
        }


        final Boolean chunkedEncodingEnabled = awsSdkS3Configuration.getChunkedEncodingEnabled();
        if (chunkedEncodingEnabled != null) {
            builder.chunkedEncodingEnabled(chunkedEncodingEnabled);
        }


        final Boolean dualStackEnabled = awsSdkS3Configuration.getDualStackEnabled();
        if (dualStackEnabled != null) {
            builder.dualstackEnabled(dualStackEnabled);
        }


        final Boolean pathStyleAccessEnabled = awsSdkS3Configuration.getPathStyleAccessEnabled();
        if (pathStyleAccessEnabled != null) {
            builder.pathStyleAccessEnabled(pathStyleAccessEnabled);
        }


        final Boolean useArnRegionEnabled = awsSdkS3Configuration.getUseArnRegionEnabled();
        if (useArnRegionEnabled != null) {
            builder.useArnRegionEnabled(useArnRegionEnabled);
        }


        return builder.build();
    }


    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultS3ClientsIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final S3Clients s3Clients = getClient();

        if (s3Clients != null) {
            s3Clients.shutdown();
        }

        s3ClientsRef.set(null);
    }

    @Override
    public S3Clients getClient() {
        return s3ClientsRef.get();
    }

    @Override
    public boolean isHealthy() {
        final S3Clients s3Clients = getClient();
        return s3Clients != null;
    }
}
