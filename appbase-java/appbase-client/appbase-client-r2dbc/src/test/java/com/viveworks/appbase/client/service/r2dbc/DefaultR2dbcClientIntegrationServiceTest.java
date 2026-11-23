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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.r2dbc.configuration.DefaultR2dbcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.r2dbc.configuration.R2dbcConnectionConfiguration;
import com.viveworks.appbase.client.service.r2dbc.configuration.R2dbcPoolConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.micrometer.core.instrument.MeterRegistry;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.Connection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link DefaultR2dbcClientIntegrationService}, bootstrapped via
 * {@link R2dbcClientServiceTestHelper} against an in-memory H2 R2DBC URL
 * ({@code r2dbc:h2:mem:///t1;DB_CLOSE_DELAY=-1}). No mocking of the R2DBC SPI: the real
 * {@code io.r2dbc.h2} driver, resolved via {@link io.r2dbc.spi.ConnectionFactories}' service
 * loader, backs the {@link ConnectionPool} under test. Requires no external infra, so this
 * runs unconditionally in CI (unlike the JDBC twin's MariaDB-backed
 * {@code DefaultJdbcClientServiceTest}, which is gated behind a system property).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultR2dbcClientIntegrationServiceTest {
    private static DefaultR2dbcClientService defaultR2dbcClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultR2dbcClientService.registerObjectMapperSubtypes();
        defaultR2dbcClientService = R2dbcClientServiceTestHelper.defaultR2dbcClientService();
    }

    @AfterAll
    static void tearDown() {
        if (defaultR2dbcClientService != null) {
            defaultR2dbcClientService.shutdown();
        }
    }

    @Test
    void testDefaultR2dbcClientIntegrationService() throws AppbaseClientIntegrationService.NotFoundException {
        assertNotNull(defaultR2dbcClientService);

        final R2dbcClientIntegrationService r2dbcClientIntegrationService =
                defaultR2dbcClientService.getClientIntegrationService();
        assertNotNull(r2dbcClientIntegrationService);

        final ConnectionPool connectionPool = r2dbcClientIntegrationService.getClient();
        assertNotNull(connectionPool);
        assertTrue(r2dbcClientIntegrationService.isHealthy());

        final Connection connection = connectionPool.create().block();
        assertNotNull(connection);
        Mono.from(connection.close()).block();

        defaultR2dbcClientService.shutdown();

        assertTrue(connectionPool.isDisposed());
        assertNull(r2dbcClientIntegrationService.getClient());
        assertFalse(r2dbcClientIntegrationService.isHealthy());
    }

    /**
     * Builds a {@link DefaultR2dbcClientIntegrationService} directly (bypassing
     * {@link DefaultR2dbcClientService}) against a fresh, dedicated in-memory H2 database and
     * {@link AppbaseCore}, so the shared Micrometer composite registry it registers gauges onto
     * (via {@code MetricService#getCompositeMeterRegistry()}) can be inspected in isolation from
     * {@link #defaultR2dbcClientService}.
     */
    @SuppressWarnings("unchecked")
    private static DefaultR2dbcClientIntegrationService buildService(
            final AppbaseCore appbaseCore, final String databaseName, final boolean driverMetricsEnabled)
    {
        final DefaultAppbaseClientCore appbaseClientCore = ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        final AppbaseClientPool<ConnectionPool, TextValue> appbaseClientPool = Mockito.mock(AppbaseClientPool.class);

        final DefaultR2dbcClientIntegrationServiceConfiguration configuration =
                DefaultR2dbcClientIntegrationServiceConfiguration.builder()
                        .r2dbcConnectionConfiguration(R2dbcConnectionConfiguration.builder()
                                .url("r2dbc:h2:mem:///" + databaseName + ";DB_CLOSE_DELAY=-1")
                                .build())
                        .r2dbcPoolConfiguration(R2dbcPoolConfiguration.builder().build())
                        .driverMetricsEnabled(driverMetricsEnabled)
                        .build();
        final AppbaseClientKey<TextValue> appbaseClientKey = R2dbcClientUtil.appbaseClientKey(configuration);

        return new DefaultR2dbcClientIntegrationService(appbaseClientCore, appbaseClientPool, appbaseClientKey, configuration);
    }

    @Test
    void testDriverMetricsGateOffRegistersNoPoolGauges() {
        final AppbaseCore appbaseCore = R2dbcClientServiceTestHelper.appbaseCore();
        final DefaultR2dbcClientIntegrationService service =
                buildService(appbaseCore, "metricsGateOff", false);

        try {
            final MeterRegistry registry = appbaseCore.getMetricService().getCompositeMeterRegistry();

            assertTrue(registry.find("r2dbc.pool.connections.acquired").gauges().isEmpty());
            assertTrue(registry.find("r2dbc.pool.connections.idle").gauges().isEmpty());
            assertTrue(registry.find("r2dbc.pool.connections.pending").gauges().isEmpty());
            assertTrue(registry.find("r2dbc.pool.connections.allocated").gauges().isEmpty());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void testDriverMetricsGateEnabledRegistersAndUnbindsPoolGauges() {
        final AppbaseCore appbaseCore = R2dbcClientServiceTestHelper.appbaseCore();
        final DefaultR2dbcClientIntegrationService service =
                buildService(appbaseCore, "metricsGateOn", true);

        final MeterRegistry registry = appbaseCore.getMetricService().getCompositeMeterRegistry();
        final String key = service.getAppbaseClientKey().toString();

        assertNotNull(registry.find("r2dbc.pool.connections.acquired").tags("key", key).gauge());
        assertNotNull(registry.find("r2dbc.pool.connections.idle").tags("key", key).gauge());
        assertNotNull(registry.find("r2dbc.pool.connections.pending").tags("key", key).gauge());
        assertNotNull(registry.find("r2dbc.pool.connections.allocated").tags("key", key).gauge());

        service.shutdown();

        assertNull(registry.find("r2dbc.pool.connections.acquired").tags("key", key).gauge());
        assertNull(registry.find("r2dbc.pool.connections.idle").tags("key", key).gauge());
        assertNull(registry.find("r2dbc.pool.connections.pending").tags("key", key).gauge());
        assertNull(registry.find("r2dbc.pool.connections.allocated").tags("key", key).gauge());
    }
}
