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
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.KeyRecord;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientIntegrationService;
import com.viveworks.appbase.client.service.aerospike.reactive.ReactiveAerospikeAccessor;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Parity tests for {@link DefaultReactiveUserSessionStore} against the blocking
 * {@link DefaultUserSessionStore}. Both stores are driven off the SAME in-memory fake Aerospike
 * backend (shared {@code backend} map): the blocking store's {@code createSession} seeds it through
 * a mocked {@link IAerospikeClient}, and the reactive store reads it back through a mocked
 * {@link ReactiveAerospikeAccessor} -- so a passing test is genuine behavioural parity, not just two
 * independently-asserted expectations.
 *
 * <p>Two tests deliberately step outside the shared-backend parity contract because their branch has
 * no blocking-store counterpart to match against: {@code readSessionReactive_withNullPasswordHashes_*}
 * and {@code readSessionReactive_malformedBytes_*} seed the backend DIRECTLY and assert only on the
 * reactive store's behaviour (a null-passwordHashes read, and a deserialize-failure surface).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultReactiveUserSessionStoreTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private Map<String, Map<String, Object>> backend;
    private DefaultUserSessionStore blockingStore;
    private DefaultReactiveUserSessionStore reactiveStore;

    @BeforeEach
    void setUp() {
        backend = new LinkedHashMap<>();

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenAnswer(invocation -> LoggerFactory.getLogger((Class<?>) invocation.getArgument(0)));

        final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
        final org.eclipse.microprofile.metrics.MetricRegistry metricRegistry =
                Mockito.mock(org.eclipse.microprofile.metrics.MetricRegistry.class);
        final org.eclipse.microprofile.metrics.Timer timer =
                Mockito.mock(org.eclipse.microprofile.metrics.Timer.class);
        final org.eclipse.microprofile.metrics.Timer.Context timerContext =
                Mockito.mock(org.eclipse.microprofile.metrics.Timer.Context.class);
        final org.eclipse.microprofile.metrics.Counter counter =
                Mockito.mock(org.eclipse.microprofile.metrics.Counter.class);
        Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
        Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(counter);
        Mockito.when(timer.time()).thenReturn(timerContext);

        final MediatorService mediatorService = Mockito.mock(MediatorService.class);
        final Mediator mediator = Mockito.mock(Mediator.class);
        Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
        Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

        final ServiceRegistry serviceRegistry = Mockito.mock(ServiceRegistry.class);
        final IdService idService = Mockito.mock(IdService.class);
        final DefaultUserSessionStoreConfiguration configuration =
                DefaultUserSessionStoreConfiguration.builder().build();

        final IAerospikeClient aerospikeClient = Mockito.mock(IAerospikeClient.class);
        stubAerospikeClient(aerospikeClient);
        final AerospikeClientIntegrationService aerospikeClientIntegrationService =
                Mockito.mock(AerospikeClientIntegrationService.class);
        Mockito.when(aerospikeClientIntegrationService.getClient()).thenReturn(aerospikeClient);

        final ReactiveAerospikeAccessor reactiveAccessor = Mockito.mock(ReactiveAerospikeAccessor.class);
        stubReactiveAccessor(reactiveAccessor);

        blockingStore = new DefaultUserSessionStore(
                logService, metricService, mediatorService, serviceRegistry,
                idService, aerospikeClientIntegrationService, configuration);

        reactiveStore = new DefaultReactiveUserSessionStore(
                logService, metricService, mediatorService, serviceRegistry,
                idService, reactiveAccessor, configuration);
    }

    // ------------------------------------------------------------------
    // Fake Aerospike backend wiring -- both stores read/write the same `backend` map.
    // ------------------------------------------------------------------

    private void stubAerospikeClient(final IAerospikeClient aerospikeClient) {
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            final Key key = (Key) args[1];
            final Map<String, Object> record = backend.computeIfAbsent(rowKeyOf(key), k -> new LinkedHashMap<>());
            // Varargs (Bin...) may arrive either collapsed into a Bin[] or, when the call site
            // passes a single Bin, as a loose trailing element -- handle both shapes.
            if (args.length == 3 && args[2] instanceof Bin[]) {
                for (final Bin bin : (Bin[]) args[2]) {
                    record.put(bin.name, bin.value.getObject());
                }
            } else {
                for (int i = 2; i < args.length; i++) {
                    final Bin bin = (Bin) args[i];
                    record.put(bin.name, bin.value.getObject());
                }
            }
            return null;
        }).when(aerospikeClient).put(Mockito.any(WritePolicy.class), Mockito.any(Key.class), Mockito.any(Bin[].class));

        Mockito.when(aerospikeClient.get(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenAnswer(invocation -> toRecord(backend.get(rowKeyOf(invocation.getArgument(1)))));

        Mockito.when(aerospikeClient.get(Mockito.any(Policy.class), Mockito.any(Key.class), Mockito.any(String.class)))
                .thenAnswer(invocation -> toRecord(backend.get(rowKeyOf(invocation.getArgument(1)))));

        Mockito.when(aerospikeClient.exists(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenAnswer(invocation -> backend.containsKey(rowKeyOf(invocation.getArgument(1))));
    }

    private void stubReactiveAccessor(final ReactiveAerospikeAccessor reactiveAccessor) {
        Mockito.when(reactiveAccessor.get(Mockito.anyString(), Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenAnswer(invocation -> {
                    final Key key = invocation.getArgument(2);
                    final Record record = toRecord(backend.get(rowKeyOf(key)));
                    return record == null ? Mono.empty() : Mono.just(new KeyRecord(key, record));
                });

        Mockito.when(reactiveAccessor.exists(Mockito.anyString(), Mockito.any(Key.class)))
                .thenAnswer(invocation -> {
                    final Key key = invocation.getArgument(1);
                    return backend.containsKey(rowKeyOf(key)) ? Mono.just(key) : Mono.empty();
                });
    }

    private static String rowKeyOf(final Key key) {
        return String.valueOf(key.userKey.getObject());
    }

    private static Record toRecord(final Map<String, Object> bins) {
        return bins == null ? null : new Record(new LinkedHashMap<>(bins), 0, 0);
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private UserSession userSession(final String appbaseUserId) {
        final UserSession userSession = new UserSession();
        userSession.setAuthenticated(true);
        userSession.setAppbaseSessionId(UUID.randomUUID().toString());
        userSession.setCreationTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(15));
        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10));

        final UserSessionData userSessionData = new UserSessionData();
        userSessionData.setDataMap(Map.of("key1", "value1"));
        userSession.setUserSessionData(userSessionData);

        userSession.setUserAccount(userAccount(appbaseUserId));

        return userSession;
    }

    private UserSession unauthenticatedSession() {
        final UserSession userSession = new UserSession();
        userSession.setAuthenticated(false);
        userSession.setAppbaseSessionId(UUID.randomUUID().toString());
        userSession.setCreationTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(15));
        userSession.setLastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10));

        final UserSessionData userSessionData = new UserSessionData();
        userSessionData.setDataMap(Map.of("key1", "value1"));
        userSession.setUserSessionData(userSessionData);

        return userSession;
    }

    private UserAccount userAccount(final String appbaseUserId) {
        return UserAccount
                .builder()
                .appbaseUserId(appbaseUserId)
                .idpUserId(UUID.randomUUID().toString())
                .name("name1")
                .email("abc@domain1.com")
                .passwordHashes(passwordHashes())
                .languageCode("EN_CA")
                .userHomeDataCenter("east1")
                .userTopic("name1")
                .emailVerified(true)
                .creationTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                .lastAccessTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private PasswordHashes passwordHashes() {
        return PasswordHashes
                .builder()
                .clientPasswordHash(
                        PasswordHash
                                .builder()
                                .hash("clientPasswordHash-123")
                                .hashAlgorithm(HashAlgorithm.SHA512)
                                .shaRounds(80)
                                .salt("salt123")
                                .build())
                .serverPasswordHash(
                        PasswordHash
                                .builder()
                                .hash("serverPasswordHash-123")
                                .hashAlgorithm(HashAlgorithm.SHA512)
                                .shaRounds(80)
                                .salt("salt123")
                                .build())
                .build();
    }

    // ------------------------------------------------------------------
    // Tests -- one per ReactiveUserSessionStore method
    // ------------------------------------------------------------------

    @Test
    void existsSessionReactive_matchesBlockingExists() throws UserSessionStoreException {
        final UserSession seeded = userSession(UUID.randomUUID().toString());
        blockingStore.createSession(seeded);
        final String appbaseUserId = seeded.getUserAccount().getAppbaseUserId();
        final String appbaseSessionId = seeded.getAppbaseSessionId();

        final boolean expectedPresent =
                blockingStore.existsSession(appbaseUserId, appbaseSessionId);
        final Boolean actualPresent =
                reactiveStore.existsSessionReactive(appbaseUserId, appbaseSessionId).block(BLOCK_TIMEOUT);
        Assertions.assertEquals(expectedPresent, actualPresent);
        Assertions.assertTrue(actualPresent);

        final boolean expectedAbsent =
                blockingStore.existsSession(appbaseUserId, UUID.randomUUID().toString());
        final Boolean actualAbsent =
                reactiveStore.existsSessionReactive(appbaseUserId, UUID.randomUUID().toString()).block(BLOCK_TIMEOUT);
        Assertions.assertEquals(expectedAbsent, actualAbsent);
        Assertions.assertFalse(actualAbsent);
    }

    @Test
    void existsUnauthenticatedSessionReactive_matchesBlockingExists() throws UserSessionStoreException {
        final UserSession seeded = unauthenticatedSession();
        blockingStore.createSession(seeded);
        final String appbaseSessionId = seeded.getAppbaseSessionId();

        final boolean expectedPresent = blockingStore.existsUnauthenticatedSession(appbaseSessionId);
        final Boolean actualPresent =
                reactiveStore.existsUnauthenticatedSessionReactive(appbaseSessionId).block(BLOCK_TIMEOUT);
        Assertions.assertEquals(expectedPresent, actualPresent);
        Assertions.assertTrue(actualPresent);

        final String missingSessionId = UUID.randomUUID().toString();
        final boolean expectedAbsent = blockingStore.existsUnauthenticatedSession(missingSessionId);
        final Boolean actualAbsent =
                reactiveStore.existsUnauthenticatedSessionReactive(missingSessionId).block(BLOCK_TIMEOUT);
        Assertions.assertEquals(expectedAbsent, actualAbsent);
        Assertions.assertFalse(actualAbsent);
    }

    @Test
    void existsSessionsReactive_matchesBlockingExists() throws UserSessionStoreException {
        final String appbaseUserId = UUID.randomUUID().toString();
        blockingStore.createSession(userSession(appbaseUserId));

        final boolean expectedPresent = blockingStore.existsSessions(appbaseUserId);
        final Boolean actualPresent = reactiveStore.existsSessionsReactive(appbaseUserId).block(BLOCK_TIMEOUT);
        Assertions.assertEquals(expectedPresent, actualPresent);
        Assertions.assertTrue(actualPresent);

        final String missingUserId = UUID.randomUUID().toString();
        final boolean expectedAbsent = blockingStore.existsSessions(missingUserId);
        final Boolean actualAbsent = reactiveStore.existsSessionsReactive(missingUserId).block(BLOCK_TIMEOUT);
        Assertions.assertEquals(expectedAbsent, actualAbsent);
        Assertions.assertFalse(actualAbsent);
    }

    @Test
    void readSessionReactive_matchesBlockingRead() throws UserSessionStoreException {
        final UserSession seeded = userSession(UUID.randomUUID().toString());
        blockingStore.createSession(seeded);
        final String appbaseUserId = seeded.getUserAccount().getAppbaseUserId();
        final String appbaseSessionId = seeded.getAppbaseSessionId();

        final UserSession expected =
                blockingStore.readSession(appbaseUserId, appbaseSessionId).orElse(null);
        final UserSession actual =
                reactiveStore.readSessionReactive(appbaseUserId, appbaseSessionId).block(BLOCK_TIMEOUT);
        Assertions.assertNotNull(expected);
        Assertions.assertEquals(expected, actual);

        final UserSession actualAbsent =
                reactiveStore.readSessionReactive(appbaseUserId, UUID.randomUUID().toString()).block(BLOCK_TIMEOUT);
        Assertions.assertNull(actualAbsent);
    }

    @Test
    void readUnauthenticatedSessionReactive_matchesBlockingRead() throws UserSessionStoreException {
        final UserSession seeded = unauthenticatedSession();
        blockingStore.createSession(seeded);
        final String appbaseSessionId = seeded.getAppbaseSessionId();

        final UserSession expected =
                blockingStore.readUnauthenticatedSession(appbaseSessionId).orElse(null);
        final UserSession actual =
                reactiveStore.readUnauthenticatedSessionReactive(appbaseSessionId).block(BLOCK_TIMEOUT);
        Assertions.assertNotNull(expected);
        Assertions.assertEquals(expected, actual);

        final UserSession actualAbsent = reactiveStore
                .readUnauthenticatedSessionReactive(UUID.randomUUID().toString())
                .block(BLOCK_TIMEOUT);
        Assertions.assertNull(actualAbsent);
    }

    @Test
    void readAllSessionsReactive_matchesBlockingRead() throws UserSessionStoreException {
        final String appbaseUserId = UUID.randomUUID().toString();
        final UserSession seeded1 = userSession(appbaseUserId);
        final UserSession seeded2 = userSession(appbaseUserId);
        blockingStore.createSession(seeded1);
        blockingStore.createSession(seeded2);

        final Collection<UserSession> expected =
                blockingStore.readAllSessions(appbaseUserId).orElse(null);
        final Collection<UserSession> actual =
                reactiveStore.readAllSessionsReactive(appbaseUserId).block(BLOCK_TIMEOUT);
        Assertions.assertNotNull(expected);
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertEquals(
                new java.util.HashSet<>(expected),
                new java.util.HashSet<>(actual));

        final Collection<UserSession> actualAbsent =
                reactiveStore.readAllSessionsReactive(UUID.randomUUID().toString()).block(BLOCK_TIMEOUT);
        Assertions.assertNull(actualAbsent);
    }

    /**
     * Regression test for the {@code nullServerPasswordHash} NPE (N01 quality ledger finding,
     * folded into implementation N02): a stored session whose {@link UserAccount} has a {@code null}
     * {@code passwordHashes} used to blow up {@code deserialize()} with a
     * {@link NullPointerException} on {@code passwordHashes.getServerPasswordHash()}, since the
     * guard only checked {@code userAccount != null}, never {@code passwordHashes != null}.
     *
     * <p>The backend is seeded DIRECTLY (bypassing {@code blockingStore.createSession}, whose
     * {@code putSession -> serialize()} ALSO calls {@code nullServerPasswordHash} and would throw
     * on the same null before this test could isolate the READ path) so this test exercises only
     * {@code DefaultReactiveUserSessionStore.deserialize()} -&gt; {@code nullServerPasswordHash()}.
     *
     * <p>Mutation-verify: remove the {@code passwordHashes != null &&} guard added in this PR from
     * {@code DefaultReactiveUserSessionStore.nullServerPasswordHash} and this test must fail with a
     * {@link NullPointerException} thrown out of {@code .block(BLOCK_TIMEOUT)}. This test exercises
     * ONLY the REACTIVE store's {@code deserialize()} path -- it never calls the blocking {@code
     * DefaultUserSessionStore}, so reverting only the blocking store's byte-identical guard would
     * NOT fail this test. The blocking store's guard is covered by identical-code parity with the
     * reactive one, not by a test that exercises it: {@code DefaultUserSessionStore}'s own test
     * class is gated behind live Aerospike infra ({@code @EnabledIfSystemProperty(named =
     * "AerospikeClientServiceTests.enabled", ...)}) and does not run in CI.
     */
    @Test
    void readSessionReactive_withNullPasswordHashes_completesWithoutNpe() throws Exception {
        final String appbaseUserId = UUID.randomUUID().toString();
        final UserSession seeded = userSession(appbaseUserId);
        final UserAccount accountWithNullPasswordHashes = new UserAccount(seeded.getUserAccount());
        accountWithNullPasswordHashes.setPasswordHashes(null);
        seeded.setUserAccount(accountWithNullPasswordHashes);
        final String appbaseSessionId = seeded.getAppbaseSessionId();

        final byte[] bytes = JsonUtil
                .getJsonMapper()
                .writerFor(UserSession.class)
                .writeValueAsBytes(seeded);
        backend.computeIfAbsent(appbaseUserId, k -> new LinkedHashMap<>()).put(appbaseSessionId, bytes);

        final UserSession actual =
                reactiveStore.readSessionReactive(appbaseUserId, appbaseSessionId).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(actual);
        Assertions.assertNotNull(actual.getUserAccount());
        Assertions.assertNull(actual.getUserAccount().getPasswordHashes());
    }

    /**
     * A stored session whose bytes are not valid JSON must surface as a {@link
     * UserSessionStoreException}, not a raw Jackson {@link java.io.IOException}. Because {@code
     * UserSessionStoreException} is a CHECKED exception, {@code Mono.block()} re-throws it wrapped in
     * a runtime exception (Reactor's {@code Exceptions.propagate}), so the assertion walks the cause
     * chain to reach it.
     *
     * <p>Mutation-verify: change {@code DefaultReactiveUserSessionStore.deserialize}'s {@code catch}
     * to swallow the {@code IOException} and return {@code null} and this test must fail (no
     * exception is thrown).
     */
    @Test
    void readSessionReactive_malformedBytes_surfaceAsUserSessionStoreException() {
        final String appbaseUserId = UUID.randomUUID().toString();
        final String appbaseSessionId = UUID.randomUUID().toString();
        // Not valid JSON: an object-open followed by an illegal token.
        backend.computeIfAbsent(appbaseUserId, k -> new LinkedHashMap<>())
                .put(appbaseSessionId, new byte[] {0x7B, 0x21});

        final RuntimeException thrown = Assertions.assertThrows(
                RuntimeException.class,
                () -> reactiveStore.readSessionReactive(appbaseUserId, appbaseSessionId).block(BLOCK_TIMEOUT));

        // block() re-throws the CHECKED UserSessionStoreException wrapped via Reactor's
        // Exceptions.propagate, so walk the cause chain to reach it.
        boolean found = false;
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (t instanceof UserSessionStoreException) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(
                found, "expected UserSessionStoreException in the cause chain, got: " + thrown);
    }
}
