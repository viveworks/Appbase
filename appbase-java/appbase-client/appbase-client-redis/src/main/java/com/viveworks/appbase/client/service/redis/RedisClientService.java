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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.service.redis.reactive.ReactiveRedisAccessor;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Client service for Redis using Lettuce.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public interface RedisClientService
        extends AppbaseClientService<StatefulRedisConnection<String, String>, TextValue>
{
    @Override
    RedisClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    RedisClientIntegrationService getClientIntegrationService(AppbaseClientKey<TextValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveRedisAccessor} bound to this service's default connection pool.
     *
     * @return configured reactive accessor
     */
    ReactiveRedisAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveRedisAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey client key identifying the pool to bind to
     * @return configured reactive accessor
     */
    ReactiveRedisAccessor getReactiveAccessor(AppbaseClientKey<TextValue> appbaseClientKey);

    /**
     * Returns a {@link RedisAccessor} bound to this service's default connection pool.
     *
     * @return configured synchronous accessor
     */
    RedisAccessor getAccessor();

    /**
     * Returns a {@link RedisAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey client key identifying the pool to bind to
     * @return configured synchronous accessor
     */
    RedisAccessor getAccessor(AppbaseClientKey<TextValue> appbaseClientKey);
}
