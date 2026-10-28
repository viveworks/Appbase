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

package com.viveworks.appbase.service.user.session.store;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientIntegrationService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.MutableReference;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.user.account.data.GuestUser;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultUserSessionStore
        extends AbstractStatelessAppbaseService<DefaultUserSessionStoreConfiguration>
        implements UserSessionStore
{
    private final Logger logger;
    private final IdService idService;
    private final AerospikeClientIntegrationService aerospikeClientIntegrationService;

    private final WritePolicy writePolicy;
    private final Policy readPolicy;
    private final Policy existsPolicy;
    private final WritePolicy deletePolicy;
    private final WritePolicy replaceOnSessionDeletePolicy;

    private final Timer createSessionTimer;
    private final Timer updateSessionTimer;
    private final Timer createOrUpdateSessionTimer;
    private final Timer existsSessionTimer;
    private final Timer existsUnauthenticatedSessionTimer;
    private final Timer existsSessionsTimer;
    private final Timer readUnauthenticatedSessionTimer;
    private final Timer readSessionTimer;
    private final Timer readAllSessionsTimer;
    private final Timer deleteSessionTimer;
    private final Timer deleteUnauthenticatedSessionTimer;
    private final Timer deleteAllSessionsTimer;

    @Inject
    public DefaultUserSessionStore(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final IdService idService,
            final AerospikeClientIntegrationService aerospikeClientIntegrationService,
            final DefaultUserSessionStoreConfiguration defaultUserSessionStoreConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, defaultUserSessionStoreConfiguration);

        AppbaseUtil.notNull(idService, "idService");
        AppbaseUtil.notNull(aerospikeClientIntegrationService, "aerospikeClientIntegrationService");
        AppbaseUtil.notNull(defaultUserSessionStoreConfiguration, "defaultUserSessionStoreConfiguration");

        this.logger = logService.getLogger(getClass());
        this.idService = idService;
        this.aerospikeClientIntegrationService = aerospikeClientIntegrationService;
        this.writePolicy = new WritePolicy();
        this.readPolicy = new Policy();
        this.existsPolicy = new Policy();
        this.deletePolicy = new WritePolicy();
        this.replaceOnSessionDeletePolicy = new WritePolicy();
        this.replaceOnSessionDeletePolicy.recordExistsAction = RecordExistsAction.REPLACE;

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "createSessionTimer"));
        this.updateSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateSessionTimer"));
        this.createOrUpdateSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "createOrUpdateSessionTimer"));
        this.existsSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsSessionTimer"));
        this.existsUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsUnauthenticatedSessionTimer"));
        this.existsSessionsTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsSessionsTimer"));
        this.readUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "readUnauthenticatedSessionTimer"));
        this.readSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "readSessionTimer"));
        this.readAllSessionsTimer =
                metricRegistry.timer(metricService.name(getClass(), "readAllSessionsTimer"));
        this.deleteSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteSessionTimer"));
        this.deleteUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteUnauthenticatedSessionTimer"));
        this.deleteAllSessionsTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteAllSessionsTimer"));
    }

    public DefaultUserSessionStore(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final IdService idService,
            final AerospikeClientIntegrationService aerospikeClientIntegrationService,
            final DefaultUserSessionStoreConfiguration defaultUserSessionStoreConfiguration,
            final WritePolicy writePolicy,
            final Policy readPolicy,
            final Policy existsPolicy,
            final WritePolicy deletePolicy,
            final WritePolicy replaceOnSessionDeletePolicy)
    {
        super(logService, metricService, mediatorService, serviceRegistry, defaultUserSessionStoreConfiguration);

        AppbaseUtil.notNull(idService, "idService");
        AppbaseUtil.notNull(aerospikeClientIntegrationService, "aerospikeClientIntegrationService");
        AppbaseUtil.notNull(defaultUserSessionStoreConfiguration, "defaultUserSessionStoreConfiguration");
        AppbaseUtil.notNull(readPolicy, "readPolicy");
        AppbaseUtil.notNull(writePolicy, "writePolicy");
        AppbaseUtil.notNull(existsPolicy, "existsPolicy");
        AppbaseUtil.notNull(deletePolicy, "deletePolicy");
        AppbaseUtil.notNull(replaceOnSessionDeletePolicy, "replaceOnSessionDeletePolicy");

        this.logger = logService.getLogger(getClass());
        this.idService = idService;
        this.aerospikeClientIntegrationService = aerospikeClientIntegrationService;
        this.writePolicy = writePolicy;
        this.readPolicy = readPolicy;
        this.existsPolicy = existsPolicy;
        this.deletePolicy = deletePolicy;
        this.replaceOnSessionDeletePolicy = replaceOnSessionDeletePolicy;

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "createSessionTimer"));
        this.updateSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateSessionTimer"));
        this.createOrUpdateSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "createOrUpdateSessionTimer"));
        this.existsSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsSessionTimer"));
        this.existsUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsUnauthenticatedSessionTimer"));
        this.existsSessionsTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsSessionsTimer"));
        this.readUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "readUnauthenticatedSessionTimer"));
        this.readSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "readSessionTimer"));
        this.readAllSessionsTimer =
                metricRegistry.timer(metricService.name(getClass(), "readAllSessionsTimer"));
        this.deleteSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteSessionTimer"));
        this.deleteUnauthenticatedSessionTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteUnauthenticatedSessionTimer"));
        this.deleteAllSessionsTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteAllSessionsTimer"));
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(UserSessionStore.class, this);
        serviceRegistry.registerAppbaseService(DefaultUserSessionStore.class, this);
    }

    @Override
    public LocalDateTime currentTime() {
        return AppbaseServiceUtil.nowSeconds();
    }

    @Override
    public String generateSessionId() {
        return idService.new14CharsId();
    }

    @Override
    public boolean isExpired(final UserSession userSession) {
        AppbaseUtil.notNull(userSession, "userSession");

        final LocalDateTime lastAccessTime = userSession.getLastAccessTimestamp();
        AppbaseUtil.notNull(lastAccessTime, "lastAccessTime");

        final LocalDateTime currentTime = currentTime();
        return lastAccessTime
                .plus(Duration.ofSeconds(getConfiguration().getSessionExpirationSeconds()))
                .isBefore(currentTime);
    }

    @Override
    public boolean isNotExpired(UserSession userSession) {
        return !isExpired(userSession);
    }

    @Override
    public void createSession(final UserSession userSession) throws UserSessionStoreException {
        final Timer.Context timer = createSessionTimer.time();
        try {
            putSession(userSession);
        } finally {
            timer.stop();
        }
    }

    @Override
    public void updateSession(final UserSession userSession) throws UserSessionStoreException {
        final Timer.Context timer = updateSessionTimer.time();
        try {
            putSession(userSession);
        } finally {
            timer.stop();
        }
    }

    @Override
    public void createOrUpdateSession(final UserSession userSession) throws UserSessionStoreException {
        final Timer.Context timer = createOrUpdateSessionTimer.time();
        try {
            putSession(userSession);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsSession(
            final String appbaseUserId,
            final String appbaseSessionId) throws UserSessionStoreException
    {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");
        AppbaseUtil.notBlank(appbaseSessionId, "appbaseSessionId");

        final Timer.Context timer = existsSessionTimer.time();
        try {
            final Key key = new Key(
                    getConfiguration().getAerospikeSessionNamespace(),
                    getConfiguration().getAerospikeSessionSetName(),
                    appbaseUserId);
            final Record record = aerospikeClientIntegrationService
                    .getClient()
                    .get(existsPolicy, key, appbaseSessionId);
            return record != null && record.bins != null && record.bins.containsKey(appbaseSessionId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsUnauthenticatedSession(final String appbaseSessionId) throws UserSessionStoreException {
        final Timer.Context timer = existsUnauthenticatedSessionTimer.time();
        try {
            return existsSession(appbaseSessionId, appbaseSessionId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsSessions(final String appbaseUserId) throws UserSessionStoreException {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Timer.Context timer = existsSessionsTimer.time();
        try {
            final Key key = new Key(
                    getConfiguration().getAerospikeSessionNamespace(),
                    getConfiguration().getAerospikeSessionSetName(),
                    appbaseUserId);
            return aerospikeClientIntegrationService
                    .getClient()
                    .exists(existsPolicy, key);
        } finally {
            timer.stop();
        }
    }

    /**
     * Both the {@code rowKey} and {@code binName} is {@code appbaseSessionId} in {@code Aerospike}.
     */
    @Override
    @Nullable
    public Optional<UserSession> readUnauthenticatedSession(
            final String appbaseSessionId) throws UserSessionStoreException
    {
        final Timer.Context timer = readUnauthenticatedSessionTimer.time();
        try {
            return readSession(appbaseSessionId, appbaseSessionId);
        } finally {
            timer.stop();
        }
    }

    /**
     * {@code rowKey} is set to {@code appbaseUserId} and {@code binName} is {@code appbaseSessionId} in {@code Aerospike}.
     */
    @Override
    @Nullable
    public Optional<UserSession> readSession(
            final String appbaseUserId,
            final String appbaseSessionId) throws UserSessionStoreException
    {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");
        AppbaseUtil.notBlank(appbaseSessionId, "appbaseSessionId");

        final Timer.Context timer = readSessionTimer.time();
        try {
            final Key key = new Key(
                    getConfiguration().getAerospikeSessionNamespace(),
                    getConfiguration().getAerospikeSessionSetName(),
                    appbaseUserId);
            final Record record = aerospikeClientIntegrationService
                    .getClient()
                    .get(readPolicy, key, appbaseSessionId);

            if (record != null) {
                final Map<String, Object> bins = record.bins;
                if (bins != null && bins.containsKey(appbaseSessionId)) {
                    final Object binValue = bins.get(appbaseSessionId);
                    final byte[] bytes = (byte[]) binValue;
                    return Optional.of(deserialize(bytes));
                }
            }

            return Optional.empty();
        } finally {
            timer.stop();
        }
    }

    @Override
    @Nullable
    public Optional<Collection<UserSession>> readAllSessions(
            final String appbaseUserId) throws UserSessionStoreException
    {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Timer.Context timer = readAllSessionsTimer.time();
        try {
            final Key key = new Key(
                    getConfiguration().getAerospikeSessionNamespace(),
                    getConfiguration().getAerospikeSessionSetName(),
                    appbaseUserId);
            final Record record = aerospikeClientIntegrationService
                    .getClient()
                    .get(readPolicy, key);

            if (record != null) {
                final Collection<UserSession> userSessions = new ArrayList<>();
                final Map<String, Object> bins = record.bins;
                if (bins != null) {
                    for (final Map.Entry<String, Object> entry : bins.entrySet()) {
                        final byte[] bytes = (byte[]) entry.getValue();
                        final UserSession userSession = deserialize(bytes);
                        userSessions.add(userSession);
                    }

                    if (!userSessions.isEmpty()) {
                        return Optional.of(userSessions);
                    }
                }
            }

            return Optional.empty();
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteSession(
            final String appbaseUserId,
            final String appbaseSessionId) throws UserSessionStoreException
    {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");
        AppbaseUtil.notBlank(appbaseSessionId, "appbaseSessionId");

        final Timer.Context timer = deleteSessionTimer.time();
        try {
            final Key key = new Key(
                    getConfiguration().getAerospikeSessionNamespace(),
                    getConfiguration().getAerospikeSessionSetName(),
                    appbaseUserId);
            final Record record = aerospikeClientIntegrationService.getClient().get(readPolicy, key);
            if (record == null) {
                logger.debug("Session not found to delete [appbaseUserId={}, appbaseSessionId={}, key={}]",
                        appbaseUserId, appbaseSessionId, key);
                return false;
            }

            final Map<String, Object> bins = record.bins;
            if (bins != null && bins.containsKey(appbaseSessionId)) {
                bins.remove(appbaseSessionId);
                final Bin[] newBins = new Bin[bins.size()];
                final MutableReference<Integer> index = new MutableReference<>(0);
                bins.forEach((k, v) -> {
                    final Bin bin = new Bin(k, Value.get(v));
                    final Integer indexValue = index.getReference();
                    newBins[indexValue] = bin;
                    index.setReference(indexValue + 1);
                });

                if (newBins.length == 0) {
                    aerospikeClientIntegrationService.getClient().delete(deletePolicy, key);
                } else {
                    aerospikeClientIntegrationService.getClient().put(this.replaceOnSessionDeletePolicy, key, newBins);
                }

                return true;
            }

            return false;
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteUnauthenticatedSession(final String appbaseSessionId) throws UserSessionStoreException {
        final Timer.Context timer = deleteUnauthenticatedSessionTimer.time();
        try {
            return deleteSession(appbaseSessionId, appbaseSessionId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteAllSessions(final String appbaseUserId) throws UserSessionStoreException {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Timer.Context timer = deleteAllSessionsTimer.time();
        try {
            final Key key = new Key(
                    getConfiguration().getAerospikeSessionNamespace(),
                    getConfiguration().getAerospikeSessionSetName(),
                    appbaseUserId);
            return aerospikeClientIntegrationService
                    .getClient()
                    .delete(deletePolicy, key);
        } finally {
            timer.stop();
        }
    }

    private void putSession(final UserSession userSession) throws UserSessionStoreException {
        AppbaseUtil.notNull(userSession, "userSession");

        validate(userSession);
        final String rowKey = rowKey(userSession);
        final Key key = new Key(
                getConfiguration().getAerospikeSessionNamespace(),
                getConfiguration().getAerospikeSessionSetName(),
                rowKey);

        final LocalDateTime currentTime = currentTime();
        if (userSession.getCreationTimestamp() == null) {
            userSession.setCreationTimestamp(currentTime);
        }
        if (userSession.getLastAccessTimestamp() == null) {
            userSession.setLastAccessTimestamp(currentTime);
        }

        final byte[] value = serialize(userSession);
        final Bin bin = new Bin(userSession.getAppbaseSessionId(), value);

        // Create must only be create, not update. But, the key is what it checks for, not bin.
        // So, a new session is a new bin, but the key might already exist...
        writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        aerospikeClientIntegrationService.getClient().put(writePolicy, key, bin);
    }

    private void validate(final UserSession userSession) throws UserSessionStoreException {
        AppbaseUtil.notNull(userSession, "userSession");

        if (AppbaseUtil.isBlank(userSession.getAppbaseSessionId())) {
            throw new UserSessionStoreException("Session ID must not be null in: " + userSession);
        }

        if (userSession.isAuthenticated()) {
            final UserAccount userAccount = userSession.getUserAccount();
            if (userAccount == null) {
                throw new UserSessionStoreException("UserAccount must not be null in: " + userSession);
            }

            if (AppbaseUtil.isBlank(userAccount.getAppbaseUserId())) {
                throw new UserSessionStoreException("User ID must not be null in: " + userSession);
            }
        }
    }

    private byte[] serialize(final UserSession userSession) throws UserSessionStoreException {
        AppbaseUtil.notNull(userSession, "userSession");

        try {
            return JsonUtil
                    .getJsonMapper()
                    .writerFor(UserSession.class)
                    .writeValueAsBytes(nullServerPasswordHash(userSession));
        } catch (IOException ioe) {
            throw new UserSessionStoreException("Failed to serialize user session: " + userSession, ioe);
        }
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

    private String rowKey(final UserSession userSession) throws UserSessionStoreException {
        AppbaseUtil.notNull(userSession, "userSession");

        final UserAccount userAccount = userSession.getUserAccount();
        if (userAccount != null) {
            final String appbaseUserId = userAccount.getAppbaseUserId();
            if (!AppbaseUtil.isBlank(appbaseUserId)) {
                return appbaseUserId;
            }
        }

        final GuestUser guestUser = userSession.getGuestUser();
        if (guestUser != null) {
            final String appbaseUserId = guestUser.getAppbaseUserId();
            if (!AppbaseUtil.isBlank(appbaseUserId)) {
                return appbaseUserId;
            }
        }

        final String appbaseSessionId = userSession.getAppbaseSessionId();
        if (AppbaseUtil.isBlank(appbaseSessionId)) {
            throw new UserSessionStoreException("appbaseSessionId or appbaseUserId must not be null");
        }

        return appbaseSessionId;
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
