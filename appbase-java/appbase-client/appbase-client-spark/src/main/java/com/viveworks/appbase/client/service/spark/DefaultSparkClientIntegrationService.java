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

package com.viveworks.appbase.client.service.spark;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.spark.configuration.DefaultSparkClientIntegrationServiceConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link SparkClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultSparkClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <SparkClient, ClusterIdValue, DefaultSparkClientIntegrationServiceConfiguration>
        implements SparkClientIntegrationService
{
    private final AtomicReference<SparkClient> sparkClientRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultSparkClientIntegrationServiceConfiguration defaultSparkClientIntegrationServiceConfiguration;



    public DefaultSparkClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SparkClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final SparkClient sparkClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SparkClient.class);

        AppbaseUtil.notNull(sparkClient, "sparkClient");

        this.sparkClientRef = new AtomicReference<>(sparkClient);
        this.defaultSparkClientIntegrationServiceConfiguration = null;
    }

    public DefaultSparkClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SparkClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultSparkClientIntegrationServiceConfiguration defaultSparkClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SparkClient.class);

        AppbaseUtil.notNull(
                defaultSparkClientIntegrationServiceConfiguration,
                "defaultSparkClientIntegrationServiceConfiguration");

        this.sparkClientRef = new AtomicReference<>();
        this.defaultSparkClientIntegrationServiceConfiguration = defaultSparkClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    private SparkConf sparkConf() {
        final SparkConf sparkConf = new SparkConf();
        final HostPortConfiguration masterHostPortConfiguration =
                defaultSparkClientIntegrationServiceConfiguration.getMasterHostPortConfiguration();
        final String masterUrl = SparkClientUtil.masterUrl(masterHostPortConfiguration);
        sparkConf.setMaster(masterUrl);


        final String sparkHome = defaultSparkClientIntegrationServiceConfiguration.getSparkHome();
        if (sparkHome != null) {
            sparkConf.setSparkHome(sparkHome);
        }


        final String appName = defaultSparkClientIntegrationServiceConfiguration.getAppName();
        if (appName != null) {
            sparkConf.setAppName(appName);
        }


        final List<String> jarList = defaultSparkClientIntegrationServiceConfiguration.getJarList();
        if (jarList != null) {
            sparkConf.setJars(jarList.toArray(new String[0]));
        }


        final Map<String, String> executorEnvironmentVariables =
                defaultSparkClientIntegrationServiceConfiguration.getExecutorEnvironmentVariables();
        if (executorEnvironmentVariables != null) {
            executorEnvironmentVariables.forEach(sparkConf::setExecutorEnv);
        }

        return sparkConf;
    }

    private SparkSession sparkSession(final SparkContext sparkContext, final SparkConf sparkConf) {
        AppbaseUtil.notNull(sparkContext, "sparkContext");
        AppbaseUtil.notNull(sparkConf, "sparkConf");

        final SparkSession.Builder sparkSessionBuilder = SparkSession
                .builder()
                .sparkContext(sparkContext)
                .config(sparkConf);
        final Map<String, String> sessionProperties =
                defaultSparkClientIntegrationServiceConfiguration.getSessionProperties();
        if (sessionProperties != null) {
            sessionProperties.forEach(sparkSessionBuilder::config);
        }

        final boolean enableHiveSupport = defaultSparkClientIntegrationServiceConfiguration.isEnableHiveSupport();
        if (enableHiveSupport) {
            sparkSessionBuilder.enableHiveSupport();
        }

        return sparkSessionBuilder.getOrCreate();
    }

    private StreamingContext streamingContext(final SparkContext sparkContext) {
        AppbaseUtil.notNull(sparkContext, "sparkContext");

        final Duration streamingBatchDuration =
                defaultSparkClientIntegrationServiceConfiguration.getStreamingBatchDuration();
        final org.apache.spark.streaming.Duration duration =
                new org.apache.spark.streaming.Duration(streamingBatchDuration.toMillis());

        return new StreamingContext(sparkContext, duration);
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultSparkClientIntegrationServiceConfiguration, "defaultSparkClientIntegrationServiceConfiguration");

        final SparkConf sparkConf = sparkConf();

        final DefaultSparkClient.Builder sparkClientBuilder = DefaultSparkClient.builder();

        final SparkContext sparkContext = new SparkContext(sparkConf);
        sparkClientBuilder.sparkContext(sparkContext);

        final JavaSparkContext javaSparkContext = new JavaSparkContext(sparkContext);
        sparkClientBuilder.javaSparkContext(javaSparkContext);

        final SparkSession sparkSession = sparkSession(sparkContext, sparkConf);
        sparkClientBuilder.sparkSession(sparkSession);

        final StreamingContext streamingContext = streamingContext(sparkContext);
        sparkClientBuilder.streamingContext(streamingContext);

        final JavaStreamingContext javaStreamingContext = new JavaStreamingContext(streamingContext);
        sparkClientBuilder.javaStreamingContext(javaStreamingContext);

        final DefaultSparkClient defaultSparkClient = sparkClientBuilder.build();
        sparkClientRef.set(defaultSparkClient);
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultSparkClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final SparkClient sparkClient = getClient();

        if (sparkClient != null) {
            boolean stopGracefully = DefaultSparkClientIntegrationServiceConfiguration.Builder.DEFAULT_STOP_GRACEFULLY;
            if (defaultSparkClientIntegrationServiceConfiguration != null) {
                stopGracefully = defaultSparkClientIntegrationServiceConfiguration.isStopGracefully();
            }
            sparkClient.getJavaStreamingContext().stop(false, stopGracefully);
            sparkClient.getStreamingContext().stop(false, stopGracefully);

            sparkClient.getSparkSession().stop();

            sparkClient.getJavaSparkContext().close();
            sparkClient.getSparkContext().stop();
        }

        sparkClientRef.set(null);
    }

    @Override
    public SparkClient getClient() {
        return sparkClientRef.get();
    }

    @Override
    public boolean isHealthy() {
        final SparkClient sparkClient = getClient();

        if (sparkClient == null) {
            return false;
        }

        return !sparkClient.getSparkContext().stopped().get();
    }
}
