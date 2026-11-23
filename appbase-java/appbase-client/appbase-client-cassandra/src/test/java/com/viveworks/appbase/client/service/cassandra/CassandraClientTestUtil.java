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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class CassandraClientTestUtil {
    private CassandraClientTestUtil() {
    }

    public static void cassandraClientIntegrationServiceTest(
            final CassandraClientService cassandraClientService,
            final CassandraClientIntegrationService cassandraClientIntegrationService)
    {
        cassandraClientIntegrationServiceTestImpl(cassandraClientService, cassandraClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>(false);
        cassandraClientIntegrationService.shutdown();
        cassandraClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        cassandraClientIntegrationServiceTestImpl(cassandraClientService, cassandraClientIntegrationService);
        cassandraClientService.shutdown();

        Assertions.assertNull(cassandraClientIntegrationService.getClient());
        Assertions.assertFalse(cassandraClientIntegrationService.isHealthy());
    }

    private static void cassandraClientIntegrationServiceTestImpl(
            final CassandraClientService cassandraClientService,
            final CassandraClientIntegrationService cassandraClientIntegrationService)
    {
        Assertions.assertNotNull(cassandraClientService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = cassandraClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(cassandraClientIntegrationService);

        final AppbaseClientKey<ClusterIdValue> appbaseClientKey = cassandraClientIntegrationService.getAppbaseClientKey();
        Assertions.assertNotNull(appbaseClientKey);

        Assertions.assertTrue(cassandraClientIntegrationService.isHealthy());
        Assertions.assertTrue(cassandraClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(CqlSession.class, cassandraClientIntegrationService.getClientType());


        final CqlSession cqlSession = cassandraClientIntegrationService.getClient();
        Assertions.assertNotNull(cqlSession);

        final ResultSet resultSet = cqlSession.execute("SELECT release_version FROM system.local");
        Assertions.assertNotNull(resultSet);

        final List<Row> rows = resultSet.all();
        Assertions.assertNotNull(rows);
        Assertions.assertFalse(rows.isEmpty());
    }
}
