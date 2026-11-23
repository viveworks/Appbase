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
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.blockhound.BlockHound;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * BlockHound guard for {@link DefaultReactiveUserSessionStore}'s reactive read chains (internal work item 1113,
 * P-027 R4 N05).
 *
 * <p>The arm: {@link BlockHound#install()} in {@link #armBlockHound()} instruments the JDK so any
 * blocking call made on one of Reactor's non-blocking worker threads throws
 * {@code BlockingOperationError}. Each read below is subscribed on {@link Schedulers#parallel()} (a
 * non-blocking scheduler BlockHound guards); a clean completion proves the store's read chain --
 * the {@code map}/{@code flatMap}/{@code Mono.fromCallable(deserialize)} steps and the
 * {@code nullServerPasswordHash} JSON post-processing -- never blocked that worker. Mutation-verify
 * by inserting a {@code Thread.sleep(..)} into, e.g.,
 * {@code DefaultReactiveUserSessionStore.readSessionReactive} -- this test then goes red with a
 * {@code BlockingOperationError}.
 *
 * <p>Unlike the R2DBC twin (whose guard runs against a real embedded H2 R2DBC engine), the Aerospike
 * driver has no offline-executable fake, so the backend is a mocked {@link ReactiveAerospikeAccessor}
 * over an in-memory map -- mirroring {@link DefaultReactiveUserSessionStoreTest}'s fixture. That
 * keeps the guarded surface the store's OWN reactive chain (which is where an accidental blocking
 * call would be introduced), and lets this test execute unconditionally offline and in CI with no
 * gated Aerospike infra.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReactiveUserSessionStoreBlockHoundTest {
    /**
     * Await bound for the calling (main) thread only. BlockHound does not guard the main thread, so
     * blocking here to collect the parallel-worker result is legitimate; the guarantee under test is
     * that nothing blocks the {@link Schedulers#parallel()} worker the read is subscribed on. The
     * accessor is mocked and emits synchronously, so there is no JaCoCo cold-start warmup to outrun
     * (contrast the R2DBC twin, whose real H2 first-op instrumentation forced an unbounded wait).
     */
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private Map<String, Map<String, Object>> backend;
    private DefaultReactiveUserSessionStore reactiveStore;

    @BeforeAll
    static void armBlockHound() {
        // Install once for the JVM; idempotent. Must precede the reads it guards.
        BlockHound.install();
    }

    @BeforeEach
    void setUp() {
        backend = new LinkedHashMap<>();

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenAnswer(invocation -> LoggerFactory.getLogger((Class<?>) invocation.getArgument(0)));

        final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
        final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
        final Timer timer = Mockito.mock(Timer.class);
        final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
        final Counter counter = Mockito.mock(Counter.class);
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

        final ReactiveAerospikeAccessor reactiveAccessor = Mockito.mock(ReactiveAerospikeAccessor.class);
        stubReactiveAccessor(reactiveAccessor);

        reactiveStore = new DefaultReactiveUserSessionStore(
                logService, metricService, mediatorService, serviceRegistry,
                idService, reactiveAccessor, configuration);
    }

    // ------------------------------------------------------------------
    // Fake Aerospike backend wiring -- reads served from an in-memory map.
    // ------------------------------------------------------------------

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

    /**
     * Seeds an authenticated session directly into the fake backend (rowKey = {@code appbaseUserId},
     * binName = {@code appbaseSessionId}) and returns it, so the guarded reads traverse the full
     * deserialize path on the parallel worker rather than short-circuiting on an empty result.
     */
    private UserSession seedAuthenticatedSession(final String appbaseUserId) throws Exception {
        final UserSession session = userSession(appbaseUserId);
        seed(appbaseUserId, session.getAppbaseSessionId(), session);
        return session;
    }

    private void seed(final String rowKey, final String binName, final UserSession session) throws Exception {
        final byte[] bytes = JsonUtil
                .getJsonMapper()
                .writerFor(UserSession.class)
                .writeValueAsBytes(session);
        backend.computeIfAbsent(rowKey, k -> new LinkedHashMap<>()).put(binName, bytes);
    }

    // ------------------------------------------------------------------
    // Guarded reads -- one per ReactiveUserSessionStore read method.
    // ------------------------------------------------------------------

    @Test
    void existsSessionReactive_neverBlocksANonBlockingThread() throws Exception {
        final String appbaseUserId = UUID.randomUUID().toString();
        final UserSession seeded = seedAuthenticatedSession(appbaseUserId);
        assertReadIsNonBlocking(
                reactiveStore.existsSessionReactive(appbaseUserId, seeded.getAppbaseSessionId()));
    }

    @Test
    void existsUnauthenticatedSessionReactive_neverBlocksANonBlockingThread() throws Exception {
        final UserSession seeded = unauthenticatedSession();
        seed(seeded.getAppbaseSessionId(), seeded.getAppbaseSessionId(), seeded);
        assertReadIsNonBlocking(
                reactiveStore.existsUnauthenticatedSessionReactive(seeded.getAppbaseSessionId()));
    }

    @Test
    void existsSessionsReactive_neverBlocksANonBlockingThread() throws Exception {
        final String appbaseUserId = UUID.randomUUID().toString();
        seedAuthenticatedSession(appbaseUserId);
        assertReadIsNonBlocking(reactiveStore.existsSessionsReactive(appbaseUserId));
    }

    @Test
    void readSessionReactive_neverBlocksANonBlockingThread() throws Exception {
        final String appbaseUserId = UUID.randomUUID().toString();
        final UserSession seeded = seedAuthenticatedSession(appbaseUserId);
        assertReadIsNonBlocking(
                reactiveStore.readSessionReactive(appbaseUserId, seeded.getAppbaseSessionId()));
    }

    @Test
    void readUnauthenticatedSessionReactive_neverBlocksANonBlockingThread() throws Exception {
        final UserSession seeded = unauthenticatedSession();
        seed(seeded.getAppbaseSessionId(), seeded.getAppbaseSessionId(), seeded);
        assertReadIsNonBlocking(
                reactiveStore.readUnauthenticatedSessionReactive(seeded.getAppbaseSessionId()));
    }

    @Test
    void readAllSessionsReactive_neverBlocksANonBlockingThread() throws Exception {
        final String appbaseUserId = UUID.randomUUID().toString();
        seedAuthenticatedSession(appbaseUserId);
        assertReadIsNonBlocking(reactiveStore.readAllSessionsReactive(appbaseUserId));
    }

    /**
     * Subscribes the read on a BlockHound-guarded {@link Schedulers#parallel()} worker and awaits it
     * from the (unguarded) main thread. A {@code BlockingOperationError} raised on the worker
     * propagates as an {@code onError} and is rethrown out of {@code block(..)}, failing the test;
     * a clean return proves the read chain ran without blocking the worker. The {@code assertNotNull}
     * pins that the chain produced a value rather than erroring out on the worker; for the
     * {@code read}/{@code readAll} callers that value is the deserialized session(s) (so the
     * {@code deserialize} path also ran on the worker), while the {@code exists*} callers return a
     * non-null {@code Boolean}.
     */
    private static void assertReadIsNonBlocking(final Mono<?> read) {
        final Object result = read.subscribeOn(Schedulers.parallel()).block(BLOCK_TIMEOUT);
        Assertions.assertNotNull(result);
    }

    // ------------------------------------------------------------------
    // Fixtures (mirror DefaultReactiveUserSessionStoreTest).
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
}
