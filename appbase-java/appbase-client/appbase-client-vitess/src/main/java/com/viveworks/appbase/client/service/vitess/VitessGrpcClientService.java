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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.service.vitess.accessor.VitessGrpcAccessor;
import com.viveworks.appbase.client.service.vitess.accessor.reactive.ReactiveVitessGrpcAccessor;

/**
 * Represents {@link AppbaseClientService} for {@code Vitess}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface VitessGrpcClientService extends AppbaseClientService<VitessGrpcClient, ClusterIdValue> {
    @Override
    VitessGrpcClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    VitessGrpcClientIntegrationService getClientIntegrationService(AppbaseClientKey<ClusterIdValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveVitessGrpcAccessor} bound to the default {@link
     * VitessGrpcClientIntegrationService} of this service.
     *
     * @return reactive accessor
     */
    ReactiveVitessGrpcAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveVitessGrpcAccessor} bound to the {@link
     * VitessGrpcClientIntegrationService} keyed by {@code appbaseClientKey}.
     *
     * @param appbaseClientKey client key identifying the keyed integration service
     * @return reactive accessor
     */
    ReactiveVitessGrpcAccessor getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Returns a synchronous {@link VitessGrpcAccessor} bound to the default {@link
     * VitessGrpcClientIntegrationService} of this service.
     *
     * @return synchronous accessor
     */
    VitessGrpcAccessor getAccessor();

    /**
     * Returns a synchronous {@link VitessGrpcAccessor} bound to the {@link
     * VitessGrpcClientIntegrationService} keyed by {@code appbaseClientKey}.
     *
     * @param appbaseClientKey client key identifying the keyed integration service
     * @return synchronous accessor
     */
    VitessGrpcAccessor getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
