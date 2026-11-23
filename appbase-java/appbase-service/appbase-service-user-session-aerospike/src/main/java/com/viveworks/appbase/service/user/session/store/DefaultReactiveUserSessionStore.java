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

package com.viveworks.appbase.service.user.session.store;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.query.KeyRecord;
import com.viveworks.appbase.client.service.aerospike.reactive.ReactiveAerospikeAccessor;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Natively non-blocking Aerospike-backed implementation of {@link ReactiveUserSessionStore}. Reads
 * go directly through the injected {@link ReactiveAerospikeAccessor} -- no offload boundary -- and
 * mirror {@link DefaultUserSessionStore}'s key model and JSON (de)serialization exactly.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public final class DefaultReactiveUserSessionStore
        extends AbstractStatelessAppbaseService<DefaultUserSessionStoreConfiguration>
        implements ReactiveUserSessionStore
{
    private final IdService idService;
    private final ReactiveAerospikeAccessor accessor;
    private final Policy readPolicy;
    private final Policy existsPolicy;

    public DefaultReactiveUserSessionStore(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final IdService idService,
            final ReactiveAerospikeAccessor accessor,
            final DefaultUserSessionStoreConfiguration defaultUserSessionStoreConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, defaultUserSessionStoreConfiguration);

        this.idService = AppbaseUtil.notNull(idService, "idService");
        this.accessor = AppbaseUtil.notNull(accessor, "accessor");
        this.readPolicy = new Policy();
        this.existsPolicy = new Policy();
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(ReactiveUserSessionStore.class, this);
        serviceRegistry.registerAppbaseService(DefaultReactiveUserSessionStore.class, this);
    }

    /**
     * {@code rowKey} is set to {@code appbaseUserId} and {@code binName} is {@code appbaseSessionId} in
     * {@code Aerospike}.
     */
    @Override
    public Mono<Boolean> existsSessionReactive(final String appbaseUserId, final String appbaseSessionId) {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");
        AppbaseUtil.notBlank(appbaseSessionId, "appbaseSessionId");

        final Key key = sessionKey(appbaseUserId);
        return accessor.get("existsSessionReactive", existsPolicy, key)
                .map(keyRecord -> binValue(keyRecord, appbaseSessionId) != null)
                .defaultIfEmpty(false);
    }

    /**
     * Both the {@code rowKey} and {@code binName} is {@code appbaseSessionId} in {@code Aerospike}.
     */
    @Override
    public Mono<Boolean> existsUnauthenticatedSessionReactive(final String appbaseSessionId) {
        return existsSessionReactive(appbaseSessionId, appbaseSessionId);
    }

    @Override
    public Mono<Boolean> existsSessionsReactive(final String appbaseUserId) {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Key key = sessionKey(appbaseUserId);
        return accessor.exists("existsSessionsReactive", key)
                .map(existingKey -> true)
                .defaultIfEmpty(false);
    }

    /**
     * {@code rowKey} is set to {@code appbaseUserId} and {@code binName} is {@code appbaseSessionId} in
     * {@code Aerospike}.
     */
    @Override
    public Mono<UserSession> readSessionReactive(final String appbaseUserId, final String appbaseSessionId) {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");
        AppbaseUtil.notBlank(appbaseSessionId, "appbaseSessionId");

        final Key key = sessionKey(appbaseUserId);
        return accessor.get("readSessionReactive", readPolicy, key)
                .flatMap(keyRecord -> {
                    final Object bin = binValue(keyRecord, appbaseSessionId);
                    return bin == null ? Mono.empty() : Mono.fromCallable(() -> deserialize((byte[]) bin));
                });
    }

    @Override
    public Mono<UserSession> readUnauthenticatedSessionReactive(final String appbaseSessionId) {
        return readSessionReactive(appbaseSessionId, appbaseSessionId);
    }

    @Override
    public Mono<Collection<UserSession>> readAllSessionsReactive(final String appbaseUserId) {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Key key = sessionKey(appbaseUserId);
        return accessor.get("readAllSessionsReactive", readPolicy, key)
                .flatMap(keyRecord -> {
                    final Record record = keyRecord.record;
                    if (record == null || record.bins == null || record.bins.isEmpty()) {
                        return Mono.empty();
                    }

                    return Mono.fromCallable(() -> {
                        final Collection<UserSession> userSessions = new ArrayList<>();
                        for (final Map.Entry<String, Object> entry : record.bins.entrySet()) {
                            userSessions.add(deserialize((byte[]) entry.getValue()));
                        }
                        return userSessions;
                    });
                });
    }

    private Key sessionKey(final String rowKey) {
        return new Key(
                getConfiguration().getAerospikeSessionNamespace(),
                getConfiguration().getAerospikeSessionSetName(),
                rowKey);
    }

    private Object binValue(final KeyRecord keyRecord, final String binName) {
        final Record record = keyRecord.record;
        if (record == null || record.bins == null) {
            return null;
        }

        return record.bins.get(binName);
    }

    private UserSession deserialize(final byte[] bytes) throws UserSessionStoreException {
        AppbaseUtil.notNull(bytes, "bytes");

        try {
            final UserSession userSession = JsonUtil
                    .getJsonMapper()
                    .readerFor(UserSession.class)
                    .readValue(bytes);
            return nullServerPasswordHash(userSession);
        } catch (IOException ioe) {
            throw new UserSessionStoreException("Failed to deserialize user session", ioe);
        }
    }

    private UserSession nullServerPasswordHash(final UserSession userSession) {
        if (userSession != null) {
            final UserAccount userAccount = userSession.getUserAccount();
            if (userAccount != null) {
                final PasswordHashes passwordHashes = userAccount.getPasswordHashes();
                if (passwordHashes != null && passwordHashes.getServerPasswordHash() != null) {
                    final PasswordHashes newPasswordHashes = PasswordHashes
                            .builder()
                            .clientPasswordHash(passwordHashes.getClientPasswordHash())
                            .serverPasswordHash(null)
                            .build();
                    final UserAccount newUserAccount = new UserAccount(userAccount, newPasswordHashes);
                    return new UserSession(userSession, newUserAccount);
                }
            }
        }

        return userSession;
    }
}
