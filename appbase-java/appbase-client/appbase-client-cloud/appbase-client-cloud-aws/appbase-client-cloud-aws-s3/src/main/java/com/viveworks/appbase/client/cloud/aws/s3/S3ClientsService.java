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

package com.viveworks.appbase.client.cloud.aws.s3;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.S3Accessor;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.reactive.ReactiveS3Accessor;

/**
 * Represents {@link AppbaseClientService} for {@code AWS S3} object store.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface S3ClientsService extends AppbaseClientService<S3Clients, ClusterIdValue> {
    @Override
    S3ClientsIntegrationService getClientIntegrationService() throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    S3ClientsIntegrationService getClientIntegrationService(
            AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveS3Accessor} bound to this service's default connection pool.
     *
     * @return reactive accessor for the default S3 connection pool
     */
    ReactiveS3Accessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveS3Accessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target S3 connection pool
     * @return reactive accessor for the keyed S3 connection pool
     */
    ReactiveS3Accessor getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Returns a synchronous {@link S3Accessor} bound to this service's default connection pool.
     *
     * @return synchronous accessor for the default S3 connection pool
     */
    S3Accessor getAccessor();

    /**
     * Returns a synchronous {@link S3Accessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target S3 connection pool
     * @return synchronous accessor for the keyed S3 connection pool
     */
    S3Accessor getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
