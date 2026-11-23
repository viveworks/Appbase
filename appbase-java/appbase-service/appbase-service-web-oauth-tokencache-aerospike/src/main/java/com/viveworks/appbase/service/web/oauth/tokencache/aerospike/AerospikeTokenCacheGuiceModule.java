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

package com.viveworks.appbase.service.web.oauth.tokencache.aerospike;

import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;
import com.viveworks.appbase.service.web.oauth.TokenCache;

/**
 * Binds {@link TokenCache} to an {@link AerospikeTokenCache} instance, the way the client modules bind
 * their services. Instance-bound rather than type-bound because the backend needs a configured
 * {@link AerospikeClientService} and the namespace, which the bootstrap supplies here.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AerospikeTokenCacheGuiceModule extends AbstractGenericGuiceModule<TokenCache> {

    /**
     * @param client    the configured Aerospike client
     * @param namespace the Aerospike namespace the token-cache set lives in
     */
    public AerospikeTokenCacheGuiceModule(AerospikeClientService client, String namespace) {
        super(TokenCache.class, new AerospikeTokenCache(client, namespace));
    }
}
