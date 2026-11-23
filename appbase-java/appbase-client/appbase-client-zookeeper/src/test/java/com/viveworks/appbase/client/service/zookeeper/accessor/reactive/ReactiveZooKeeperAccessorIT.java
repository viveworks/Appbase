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

package com.viveworks.appbase.client.service.zookeeper.accessor.reactive;

import com.viveworks.appbase.client.service.zookeeper.DefaultZooKeeperClientService;
import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientTestHelper;
import com.viveworks.appbase.client.service.zookeeper.accessor.ZooKeeperAccessor;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gated integration test exercising both faces of the ZooKeeper accessor layer --
 * {@link ReactiveZooKeeperAccessor} and {@link ZooKeeperAccessor} -- against a live ZooKeeper
 * ensemble, mirroring the bootstrap of {@code DefaultZooKeeperClientServiceTest}.
 *
 * <p>Requires a running ZooKeeper reachable per the module's gated-test config: system property
 * {@code ZooKeeperClientServiceTests.enabled=true} (defaults to {@code false} in {@code
 * appbase-client/pom.xml}, as every backend gate now does -- N10 defaulted the inverted
 * Elasticsearch gate off, so it is no longer the exception), ZooKeeper listening at {@code
 * localhost:2181}. This module opts into the parent's {@code maven-failsafe-plugin} execution in
 * {@code develop-profile}/{@code staging-profile}/{@code release-profile}, so Failsafe selects
 * {@code *IT} classes during {@code verify} and no {@code -Dtest=} selection is needed;
 * {@code quick-profile} never runs them.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "ZooKeeperClientServiceTests.enabled", matches = "true")
class ReactiveZooKeeperAccessorIT {

    private static DefaultZooKeeperClientService defaultZooKeeperClientService;
    private static String reactivePath;
    private static String syncPath;

    /**
     * Boots a live {@link DefaultZooKeeperClientService} against ZooKeeper at {@code
     * localhost:2181}, mirroring {@code DefaultZooKeeperClientServiceTest}'s bootstrap via {@link
     * ZooKeeperClientTestHelper#defaultZooKeeperClientService()}. The service's YAML config has
     * {@code autoStart: true}, so construction blocks until the client is connected.
     */
    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultZooKeeperClientService.registerObjectMapperSubtypes();
        defaultZooKeeperClientService = ZooKeeperClientTestHelper.defaultZooKeeperClientService();

        reactivePath = uniquePath("reactive");
        syncPath = uniquePath("sync");
    }

    /**
     * Best-effort deletes both test znodes before shutting down the service, so a failed
     * assertion in either round trip does not orphan a znode on the live ensemble, then shuts
     * down {@link #defaultZooKeeperClientService}.
     */
    @AfterAll
    static void tearDown() {
        if (defaultZooKeeperClientService != null) {
            final ZooKeeperAccessor accessor = defaultZooKeeperClientService.getAccessor();
            deleteQuietly(accessor, reactivePath);
            deleteQuietly(accessor, syncPath);

            defaultZooKeeperClientService.shutdown();
        }
    }

    private static String uniquePath(final String prefix) {
        return ("/" + prefix + ReactiveZooKeeperAccessorIT.class.getSimpleName()
                + Long.toString(System.nanoTime())).toLowerCase();
    }

    private static String childName(final String path) {
        return path.substring(1);
    }

    private static void deleteQuietly(final ZooKeeperAccessor accessor, final String path) {
        try {
            accessor.delete("cleanupDelete", path);
        } catch (Exception ex) {
            // Best-effort: tolerate NoNode (already deleted by the test itself) and any other
            // failure here, so cleanup never masks the test's own assertion failures.
        }
    }

    /**
     * Reactive round trip via {@link ReactiveZooKeeperAccessor}: {@code create} a znode with a
     * payload -&gt; {@code getData} (asserting the payload) -&gt; {@code setData} a new payload
     * -&gt; {@code getData} (asserting the new payload) -&gt; {@code getChildren} on the root
     * parent (asserting the znode name is present) -&gt; {@code delete} -&gt; {@code checkExists}
     * (asserting the znode is gone -- the reactive accessor completes empty rather than emitting
     * a {@code null} {@link Stat}, since Reactive Streams disallows {@code null} {@code onNext}).
     * Cleanup is handled unconditionally by {@link #tearDown()}, so a failed assertion here does
     * not orphan the znode on the live ensemble.
     */
    @Test
    void reactiveRoundTrip() {
        final ReactiveZooKeeperAccessor reactive = defaultZooKeeperClientService.getReactiveAccessor();
        final byte[] initialData = "reactive-initial".getBytes(StandardCharsets.UTF_8);
        final byte[] updatedData = "reactive-updated".getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(reactive.create("create", reactivePath, initialData))
                .assertNext(createdPath -> Assertions.assertEquals(reactivePath, createdPath))
                .verifyComplete();

        StepVerifier.create(reactive.getData("getData", reactivePath))
                .assertNext(data -> Assertions.assertArrayEquals(initialData, data))
                .verifyComplete();

        StepVerifier.create(reactive.setData("setData", reactivePath, updatedData))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();

        StepVerifier.create(reactive.getData("getData", reactivePath))
                .assertNext(data -> Assertions.assertArrayEquals(updatedData, data))
                .verifyComplete();

        StepVerifier.create(reactive.getChildren("getChildren", "/"))
                .assertNext(children -> Assertions.assertTrue(children.contains(childName(reactivePath))))
                .verifyComplete();

        StepVerifier.create(reactive.delete("delete", reactivePath))
                .verifyComplete();

        StepVerifier.create(reactive.checkExists("checkExists", reactivePath))
                .verifyComplete();
    }

    /**
     * Synchronous round trip via {@link ZooKeeperAccessor}, mirroring the same {@code create}
     * -&gt; {@code getData} -&gt; {@code setData} -&gt; {@code getData} -&gt; {@code getChildren}
     * -&gt; {@code delete} -&gt; {@code checkExists} sequence as {@link #reactiveRoundTrip()}.
     * Cleanup is handled unconditionally by {@link #tearDown()}, so a failed assertion here does
     * not orphan the znode on the live ensemble.
     *
     * @throws Exception the mapped exception, if any operation fails
     */
    @Test
    void syncRoundTrip() throws Exception {
        final ZooKeeperAccessor sync = defaultZooKeeperClientService.getAccessor();
        final byte[] initialData = "sync-initial".getBytes(StandardCharsets.UTF_8);
        final byte[] updatedData = "sync-updated".getBytes(StandardCharsets.UTF_8);

        final String createdPath = sync.create("create", syncPath, initialData);
        Assertions.assertEquals(syncPath, createdPath);

        final byte[] fetchedInitialData = sync.getData("getData", syncPath);
        Assertions.assertArrayEquals(initialData, fetchedInitialData);

        final Stat setDataStat = sync.setData("setData", syncPath, updatedData);
        Assertions.assertNotNull(setDataStat);

        final byte[] fetchedUpdatedData = sync.getData("getData", syncPath);
        Assertions.assertArrayEquals(updatedData, fetchedUpdatedData);

        final List<String> children = sync.getChildren("getChildren", "/");
        Assertions.assertTrue(children.contains(childName(syncPath)));

        sync.delete("delete", syncPath);

        final Stat existsStat = sync.checkExists("checkExists", syncPath);
        Assertions.assertNull(existsStat);
    }
}
