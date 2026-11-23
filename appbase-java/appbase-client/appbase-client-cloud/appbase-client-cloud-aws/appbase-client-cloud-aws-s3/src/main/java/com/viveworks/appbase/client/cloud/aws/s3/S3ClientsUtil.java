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

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.DefaultMutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.core.clientkey.clusterid.DefaultClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.DefaultS3ClientsIntegrationServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Utilities for this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class S3ClientsUtil {
    private S3ClientsUtil() {
    }

    /**
     * Generate {@link AppbaseClientKey} used by {@link S3ClientsService}.
     *
     * @param defaultS3ClientsIntegrationServiceConfiguration {@link DefaultS3ClientsIntegrationServiceConfiguration}
     * @return {@link AppbaseClientKey}
     */
    public static AppbaseClientKey<ClusterIdValue> appbaseClientKey(
            final DefaultS3ClientsIntegrationServiceConfiguration defaultS3ClientsIntegrationServiceConfiguration)
    {
        AppbaseUtil.notNull(
                defaultS3ClientsIntegrationServiceConfiguration, "defaultS3ClientsIntegrationServiceConfiguration");

        final ClusterIdValueConfiguration clusterIdValueKeyConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getClusterIdValueKeyConfiguration();
        if (clusterIdValueKeyConfiguration != null) {
            return AppbaseClientUtil.clusterIdValueKey(clusterIdValueKeyConfiguration);
        }

        final TextValueConfiguration awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration =
                defaultS3ClientsIntegrationServiceConfiguration.getAwsSdkAsyncHttpClientIntegrationServiceKeyConfiguration();
        final DefaultClusterIdValue defaultClusterIdValue = new DefaultClusterIdValue();
        defaultClusterIdValue.init(awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration.getText());

        final DefaultMutableAppbaseClientKey<ClusterIdValue> defaultMutableAppbaseClientKey =
                new DefaultMutableAppbaseClientKey<>();
        defaultMutableAppbaseClientKey.initValue(defaultClusterIdValue);

        return defaultMutableAppbaseClientKey;
    }
}
