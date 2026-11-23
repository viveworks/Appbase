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

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.spark.configuration.DefaultSparkClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;

/**
 * Contains {@code Spark} client utilities.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class SparkClientUtil {
    private static final String SPARK_URL_PREFIX = "spark://";

    private SparkClientUtil() {
    }

    /**
     * Generate {@link AppbaseClientKey} used by {@link SparkClientService}.
     *
     * @param defaultSparkClientIntegrationServiceConfiguration {@link DefaultSparkClientIntegrationServiceConfiguration}
     * @return {@link AppbaseClientKey}
     */
    public static AppbaseClientKey<ClusterIdValue> appbaseClientKey(
            final DefaultSparkClientIntegrationServiceConfiguration defaultSparkClientIntegrationServiceConfiguration)
    {
        AppbaseUtil.notNull(
                defaultSparkClientIntegrationServiceConfiguration, "defaultSparkClientIntegrationServiceConfiguration");

        final ClusterIdValueConfiguration clusterIdValueKeyConfiguration =
                defaultSparkClientIntegrationServiceConfiguration.getClusterIdValueKeyConfiguration();
        if (clusterIdValueKeyConfiguration != null) {
            return AppbaseClientUtil.clusterIdValueKey(clusterIdValueKeyConfiguration);
        }

        final HostPortConfiguration masterHostPortConfiguration =
                defaultSparkClientIntegrationServiceConfiguration.getMasterHostPortConfiguration();
        final String masterUrl = SparkClientUtil.masterUrl(masterHostPortConfiguration);
        final ClusterIdValueConfiguration clusterIdValueConfiguration = ClusterIdValueConfiguration
                .builder()
                .clusterId(masterUrl)
                .build();
        return AppbaseClientUtil.clusterIdValueKey(clusterIdValueConfiguration);
    }

    /**
     * Build {@code Spark} master URL of the form: {@code spark://host:port}.
     *
     * @param hostPortConfiguration {@link HostPortConfiguration}
     * @return Master URL
     */
    public static String masterUrl(final HostPortConfiguration hostPortConfiguration) {
        AppbaseUtil.notNull(hostPortConfiguration, "hostPortConfiguration");

        return SPARK_URL_PREFIX +
                hostPortConfiguration.getHost() +
                AppbaseUtil.COLON +
                hostPortConfiguration.getPort();
    }
}
