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

package com.viveworks.appbase.client.service.jasync;

import com.github.jasync.sql.db.Connection;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.service.jasync.reactive.ReactiveJasyncAccessor;

/**
 * Represents {@link AppbaseClientService} for <a href="https://github.com/jasync-sql/jasync-sql">jasync-sql</a>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface JasyncClientService extends AppbaseClientService<Connection, TextValue> {
    @Override
    JasyncClientIntegrationService getClientIntegrationService() throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    JasyncClientIntegrationService getClientIntegrationService(
            AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveJasyncAccessor} bound to this service's default connection pool.
     *
     * @return reactive accessor for the default jasync connection pool
     */
    ReactiveJasyncAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveJasyncAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target jasync connection pool
     * @return reactive accessor for the keyed jasync connection pool
     */
    ReactiveJasyncAccessor getReactiveAccessor(AppbaseClientKey<TextValue> appbaseClientKey);

    /**
     * Returns a {@link JasyncAccessor} bound to this service's default connection pool.
     *
     * @return synchronous accessor for the default jasync connection pool
     */
    JasyncAccessor getAccessor();

    /**
     * Returns a {@link JasyncAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target jasync connection pool
     * @return synchronous accessor for the keyed jasync connection pool
     */
    JasyncAccessor getAccessor(AppbaseClientKey<TextValue> appbaseClientKey);
}
