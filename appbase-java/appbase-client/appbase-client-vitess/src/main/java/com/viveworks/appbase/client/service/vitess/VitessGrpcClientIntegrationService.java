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
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import io.vitess.client.Context;
import io.vitess.client.VTSession;
import io.vitess.proto.grpc.VitessGrpc;

/**
 * Represents {@link AppbaseClientIntegrationService} for {@link VitessGrpc}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface VitessGrpcClientIntegrationService
        extends AppbaseClientIntegrationService<VitessGrpcClient, ClusterIdValue>
{
    /**
     * Mint a fresh context per operation so relative deadlines start when the operation starts.
     *
     * @return a newly-built {@link Context}
     */
    Context newContext();

    /**
     * Mint a FRESH autocommit VTSession per call from this integration service's config --
     * concurrency-safe; not for cross-op transactions.
     *
     * @return a newly-built {@link VTSession}
     */
    VTSession newVtSession();
}
