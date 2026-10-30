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

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import io.vitess.client.Context;
import io.vitess.client.SQLFuture;
import io.vitess.client.VTGateConnection;
import io.vitess.client.VTSession;
import io.vitess.client.cursor.Cursor;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class VitessGrpcClientServiceTestUtil {
    private VitessGrpcClientServiceTestUtil() {
    }

    public static void vitessGrpcClientServiceTest(
            final VitessGrpcClientService vitessGrpcClientService,
            final VitessGrpcClientIntegrationService vitessGrpcClientIntegrationService)
    {
        vitessGrpcClientServiceTestImpl(vitessGrpcClientService, vitessGrpcClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        vitessGrpcClientIntegrationService.shutdown();
        vitessGrpcClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
            @Override
            public void onSuccess() {
                repaired.setReference(true);
            }

            @Override
            public void onFailure() {
                repaired.setReference(false);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                throw new AppbaseRuntimeException(throwable);
            }
        });
        Assertions.assertTrue(repaired.getReference());

        vitessGrpcClientServiceTestImpl(vitessGrpcClientService, vitessGrpcClientIntegrationService);
        vitessGrpcClientService.shutdown();

        Assertions.assertNull(vitessGrpcClientIntegrationService.getClient());
        Assertions.assertFalse(vitessGrpcClientIntegrationService.isHealthy());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void vitessGrpcClientServiceTestImpl(
            final VitessGrpcClientService vitessGrpcClientService,
            final VitessGrpcClientIntegrationService vitessGrpcClientIntegrationService)
    {
        Assertions.assertNotNull(vitessGrpcClientService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey =
                vitessGrpcClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(vitessGrpcClientIntegrationService);

        final VitessGrpcClient vitessGrpcClient = vitessGrpcClientIntegrationService.getClient();
        Assertions.assertNotNull(vitessGrpcClient);

        final Context defaultContext = vitessGrpcClient.getDefaultContext();
        final VTGateConnection vtGateConnection = vitessGrpcClient.getVtGateConnection();
        final VTSession vtSession = vitessGrpcClient.getVtSession();
        try {
            final SQLFuture<Cursor> sqlFuture =
                    vtGateConnection.execute(defaultContext, "SELECT * FROM TestTable", null, vtSession);
            final Cursor cursor = sqlFuture.get(60, TimeUnit.SECONDS);
            Assertions.assertNotNull(cursor);
        } catch (Exception ex) {
            Assertions.fail(ex);
        }

        Assertions.assertTrue(vitessGrpcClientIntegrationService.isHealthy());
        Assertions.assertTrue(vitessGrpcClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(VitessGrpcClient.class, vitessGrpcClientIntegrationService.getClientType());
    }
 }
