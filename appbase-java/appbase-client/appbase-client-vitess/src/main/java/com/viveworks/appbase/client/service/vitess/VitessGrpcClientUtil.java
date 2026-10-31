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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.DefaultVitessGrpcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.TargetConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.VitessTabletType;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Utilities for this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class VitessGrpcClientUtil {
    private VitessGrpcClientUtil() {
    }

    /**
     * Construct a string of the form {@code keyspace:shard@tabletType, keyspace@tabletType or @tabletType} using
     * {@link TargetConfiguration}.
     *
     * @param targetConfiguration {@link TargetConfiguration}
     * @return target string
     */
    public static String target(final TargetConfiguration targetConfiguration) {
        AppbaseUtil.notNull(targetConfiguration, "targetConfiguration");

        final String keyspace = targetConfiguration.getKeyspace();
        final String shard = targetConfiguration.getShard();

        final VitessTabletType vitessTabletType = targetConfiguration.getVitessTabletType();
        AppbaseUtil.notNull(vitessTabletType, "vitessTabletType");

        //
        if (keyspace != null && shard != null) {
            return keyspace + AppbaseUtil.COLON + shard + AppbaseUtil.AT + vitessTabletType.getText();
        } else if (keyspace != null) {
            return keyspace + AppbaseUtil.AT + vitessTabletType.getText();
        }

        return AppbaseUtil.AT + vitessTabletType.getText();
    }

    /**
     * Generate {@link AppbaseClientKey} used by {@link VitessGrpcClientService}.
     *
     * @param defaultVitessGrpcClientIntegrationServiceConfiguration
     * {@link DefaultVitessGrpcClientIntegrationServiceConfiguration}
     * @return {@link AppbaseClientKey}
     */
    public static AppbaseClientKey<ClusterIdValue> appbaseClientKey(
            final DefaultVitessGrpcClientIntegrationServiceConfiguration
                    defaultVitessGrpcClientIntegrationServiceConfiguration)
    {
        final ClusterIdValueConfiguration clusterIdValueKeyConfiguration =
                defaultVitessGrpcClientIntegrationServiceConfiguration.getClusterIdValueKeyConfiguration();
        if (clusterIdValueKeyConfiguration != null) {
            AppbaseClientUtil.clusterIdValueKey(clusterIdValueKeyConfiguration);
        }

        final TargetConfiguration targetConfiguration = defaultVitessGrpcClientIntegrationServiceConfiguration
                .getCustomGrpcClientFactoryConfiguration()
                .getTargetConfiguration();
        final String target = target(targetConfiguration);
        final ClusterIdValueConfiguration targetClusterIdValueKeyConfiguration = ClusterIdValueConfiguration
                .builder()
                .clusterId(target)
                .build();

        return AppbaseClientUtil.clusterIdValueKey(targetClusterIdValueKeyConfiguration);
    }
}
