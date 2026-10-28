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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.service.r2dbc.accessor.reactive.ReactiveR2dbcAccessor;

import io.r2dbc.pool.ConnectionPool;

/**
 * Represents {@link AppbaseClientService} for {@code R2DBC} (Reactive Relational Database
 * Connectivity). Each keyed {@link R2dbcClientIntegrationService} holds a native
 * {@link ConnectionPool} rather than a blocking {@code javax.sql.DataSource}; this module is
 * reactive-only &mdash; for blocking JDBC access use {@code appbase-client-jdbc} instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface R2dbcClientService extends AppbaseClientService<ConnectionPool, TextValue> {
    @Override
    R2dbcClientIntegrationService getClientIntegrationService() throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    R2dbcClientIntegrationService getClientIntegrationService(
            AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveR2dbcAccessor} bound to the default {@link
     * R2dbcClientIntegrationService} of this service.
     *
     * @return reactive accessor
     */
    ReactiveR2dbcAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveR2dbcAccessor} bound to the {@link R2dbcClientIntegrationService}
     * keyed by {@code appbaseClientKey}.
     *
     * @param appbaseClientKey client key identifying the keyed integration service
     * @return reactive accessor
     */
    ReactiveR2dbcAccessor getReactiveAccessor(AppbaseClientKey<TextValue> appbaseClientKey);
}
