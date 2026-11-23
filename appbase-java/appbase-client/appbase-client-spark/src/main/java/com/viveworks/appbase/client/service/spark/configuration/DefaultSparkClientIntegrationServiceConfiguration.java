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

package com.viveworks.appbase.client.service.spark.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.spark.DefaultSparkClientIntegrationService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for {@link DefaultSparkClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 * @see <a href="https://spark.apache.org/docs/latest/configuration.html#spark-properties">Spark Properties</a>
 */
@Immutable
@JsonDeserialize(builder = DefaultSparkClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultSparkClientIntegrationServiceConfiguration implements SparkClientIntegrationServiceConfiguration {
    @NullableConditionally(dependsOn = "master")
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;

    private final String appName;

    private final String sparkHome;

    private final HostPortConfiguration masterHostPortConfiguration;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> executorEnvironmentVariables;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final List<String> jarList;

    private final boolean enableHiveSupport;

    private final Map<String, String> sessionProperties;

    private final Duration streamingBatchDuration;

    private final boolean stopGracefully;

    private DefaultSparkClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.masterHostPortConfiguration, "builder.masterHostPortConfiguration");
        AppbaseUtil.notNull(builder.streamingBatchDuration, "builder.streamingBatchDuration");

        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
        appName = builder.appName;
        sparkHome = builder.sparkHome;
        masterHostPortConfiguration = builder.masterHostPortConfiguration;
        executorEnvironmentVariables = builder.executorEnvironmentVariables;
        jarList = builder.jarList;
        enableHiveSupport = builder.enableHiveSupport;
        sessionProperties = builder.sessionProperties;
        streamingBatchDuration = builder.streamingBatchDuration;
        stopGracefully = builder.stopGracefully;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public ClusterIdValueConfiguration getClusterIdValueKeyConfiguration() {
        return clusterIdValueKeyConfiguration;
    }

    public String getAppName() {
        return appName;
    }

    public String getSparkHome() {
        return sparkHome;
    }

    public HostPortConfiguration getMasterHostPortConfiguration() {
        return masterHostPortConfiguration;
    }

    public Map<String, String> getExecutorEnvironmentVariables() {
        return executorEnvironmentVariables;
    }

    public List<String> getJarList() {
        return jarList;
    }

    public boolean isEnableHiveSupport() {
        return enableHiveSupport;
    }

    public Map<String, String> getSessionProperties() {
        return sessionProperties;
    }

    public Duration getStreamingBatchDuration() {
        return streamingBatchDuration;
    }

    public boolean isStopGracefully() {
        return stopGracefully;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultSparkClientIntegrationServiceConfiguration that = (DefaultSparkClientIntegrationServiceConfiguration) o;
        return enableHiveSupport == that.enableHiveSupport &&
                stopGracefully == that.stopGracefully &&
                Objects.equals(clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration) &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(sparkHome, that.sparkHome) &&
                Objects.equals(masterHostPortConfiguration, that.masterHostPortConfiguration) &&
                Objects.equals(executorEnvironmentVariables, that.executorEnvironmentVariables) &&
                Objects.equals(jarList, that.jarList) &&
                Objects.equals(sessionProperties, that.sessionProperties) &&
                Objects.equals(streamingBatchDuration, that.streamingBatchDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueKeyConfiguration,
                appName,
                sparkHome,
                masterHostPortConfiguration,
                executorEnvironmentVariables,
                jarList,
                sessionProperties,
                enableHiveSupport,
                streamingBatchDuration,
                stopGracefully);
    }

    @Override
    public String toString() {
        return "DefaultSparkClientIntegrationServiceConfiguration{" +
                "clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                ", appName='" + appName + '\'' +
                ", sparkHome='" + sparkHome + '\'' +
                ", masterHostPortConfiguration=" + masterHostPortConfiguration +
                ", executorEnvironmentVariables=" + AppbaseUtil.OBFUSCATE +
                ", jarList=" + jarList +
                ", enableHiveSupport=" + enableHiveSupport +
                ", sessionProperties=" + AppbaseUtil.OBFUSCATE +
                ", streamingBatchDuration=" + streamingBatchDuration +
                ", stopGracefully=" + stopGracefully +
                '}';
    }


    /**
     * Builder for {@link DefaultSparkClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        public static final boolean DEFAULT_STOP_GRACEFULLY = true;

        private static final ClusterIdValueConfiguration DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION =
                ClusterIdValueConfiguration.PRIMARY;
        private static final String DEFAULT_APP_NAME = "appbase-spark-test-app";
        private static final String DEFAULT_SPARK_HOME = AppbaseConstants.APPBASE_BASE_DIRECTORY + File.separator + "spark";
        private static final HostPortConfiguration DEFAULT_MASTER_HOST_PORT_CONFIGURATION = HostPortConfiguration
                .builder()
                .host("spark-master.local")
                .port(7077)
                .build();
        private static final Map<String, String> DEFAULT_EXECUTOR_ENVIRONMENT_VARIABLES = Map.of();
        private static final Map<String, String> DEFAULT_SESSION_PROPERTIES = Map.of();
        private static final Duration DEFAULT_STREAMING_BATCH_DURATION = Duration.ofMillis(100);
        private static final boolean DEFAULT_ENABLE_HIVE_SUPPORT = false;


        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration = DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;
        private String appName = DEFAULT_APP_NAME;
        private String sparkHome = DEFAULT_SPARK_HOME;
        private HostPortConfiguration masterHostPortConfiguration = DEFAULT_MASTER_HOST_PORT_CONFIGURATION;
        private Map<String, String> executorEnvironmentVariables = DEFAULT_EXECUTOR_ENVIRONMENT_VARIABLES;
        private List<String> jarList;
        private Map<String, String> sessionProperties = DEFAULT_SESSION_PROPERTIES;
        private boolean enableHiveSupport = DEFAULT_ENABLE_HIVE_SUPPORT;
        private Duration streamingBatchDuration = DEFAULT_STREAMING_BATCH_DURATION;
        private boolean stopGracefully = DEFAULT_STOP_GRACEFULLY;


        private Builder() {
        }

        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        public Builder appName(final String appName) {
            this.appName = appName;
            return this;
        }

        public Builder sparkHome(final String sparkHome) {
            this.sparkHome = sparkHome;
            return this;
        }

        public Builder masterHostPortConfiguration(final HostPortConfiguration masterHostPortConfiguration) {
            this.masterHostPortConfiguration = masterHostPortConfiguration;
            return this;
        }

        public Builder executorEnvironmentVariables(final Map<String, String> executorEnvironmentVariables) {
            this.executorEnvironmentVariables = executorEnvironmentVariables;
            return this;
        }

        public Builder jarList(final List<String> jarList) {
            this.jarList = jarList;
            return this;
        }

        public Builder sessionProperties(final Map<String, String> sessionProperties) {
            this.sessionProperties = sessionProperties;
            return this;
        }

        public Builder enableHiveSupport(final boolean enableHiveSupport) {
            this.enableHiveSupport = enableHiveSupport;
            return this;
        }

        public Builder streamingBatchDuration(final Duration streamingBatchDuration) {
            this.streamingBatchDuration = streamingBatchDuration;
            return this;
        }

        public Builder stopGracefully(final boolean stopGracefully) {
            this.stopGracefully = stopGracefully;
            return this;
        }

        public DefaultSparkClientIntegrationServiceConfiguration build() {
            return new DefaultSparkClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
