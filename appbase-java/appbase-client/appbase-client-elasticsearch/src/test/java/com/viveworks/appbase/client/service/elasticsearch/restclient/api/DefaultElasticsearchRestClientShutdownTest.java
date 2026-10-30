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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientIntegrationService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultElasticsearchRestClient#shutdown()} timeout behaviour.
 *
 * <p>Test 1 ({@code shutdown_completesWithinTimeout_whenCloseHangs}) is the <em>RED</em> test:
 * it will FAIL against the current implementation because {@code shutdown()} has no timeout
 * guard and will block for the full 30-second simulated hang. It will PASS once a 5-second
 * timeout is added.</p>
 *
 * <p>Test 2 ({@code shutdown_closesClient_successfully}) is a baseline sanity check that
 * verifies the happy path works and should PASS right now.</p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DefaultElasticsearchRestClientShutdownTest {

    private static final long TIMEOUT_MS = 7_000L;

    @Mock
    private AppbaseCore appbaseCore;

    @Mock
    private LogService logService;

    @Mock
    private Logger logger;

    @Mock
    private ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService;

    @Mock
    private ElasticsearchLowLevelRestClientIntegrationService integrationService;

    @Mock
    private RestClient restClient;

    // -----------------------------------------------------------------------
    // Test 1 – RED: shutdown() must honour a 5-second timeout when close() hangs.
    //   FAILS NOW  because the current implementation calls restClient.close()
    //   directly (no timeout), so it blocks for 30 seconds.
    //   PASSES ONCE a 5-second timeout is wired in.
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@link DefaultElasticsearchRestClient#shutdown()} returns in under
     * {@value #TIMEOUT_MS} ms even when the underlying {@link RestClient#close()} hangs.
     *
     * <p>This test intentionally FAILS against the current implementation (no timeout
     * guard exists). It will PASS once a 5-second timeout is added to {@code shutdown()}.</p>
     */
    @Test
    void shutdown_completesWithinTimeout_whenCloseHangs() throws IOException {
        // Arrange
        when(appbaseCore.getLogService()).thenReturn(logService);
        when(logService.getLogger(any(Class.class))).thenReturn(logger);
        when(elasticsearchLowLevelRestClientService.getClientIntegrationService())
                .thenReturn(integrationService);
        when(integrationService.getClient()).thenReturn(restClient);

        // Simulate a hanging close() — sleeps for 30 seconds
        doAnswer(invocation -> {
            Thread.sleep(30_000L);
            return null;
        }).when(restClient).close();

        final DefaultElasticsearchRestClient client =
                new DefaultElasticsearchRestClient(appbaseCore, elasticsearchLowLevelRestClientService);

        // Act
        final long startMs = System.currentTimeMillis();
        try {
            client.shutdown();
        } catch (AppbaseRuntimeException ignored) {
            // A timeout-enforcing implementation may throw; that is acceptable.
        }
        final long elapsedMs = System.currentTimeMillis() - startMs;

        // Assert — must return well within the 7-second guard window
        Assertions.assertTrue(
                elapsedMs < TIMEOUT_MS,
                "shutdown() blocked for " + elapsedMs + " ms; expected < " + TIMEOUT_MS + " ms. "
                        + "A 5-second timeout must be added to DefaultElasticsearchRestClient.shutdown().");
    }

    // -----------------------------------------------------------------------
    // Test 2 – GREEN baseline: shutdown() must complete normally when close() is fast.
    //   PASSES NOW and must continue to PASS after the timeout is implemented.
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@link DefaultElasticsearchRestClient#shutdown()} completes without
     * exception when the underlying {@link RestClient#close()} returns immediately.
     */
    @Test
    void shutdown_closesClient_successfully() throws IOException {
        // Arrange
        when(appbaseCore.getLogService()).thenReturn(logService);
        when(logService.getLogger(any(Class.class))).thenReturn(logger);
        when(elasticsearchLowLevelRestClientService.getClientIntegrationService())
                .thenReturn(integrationService);
        when(integrationService.getClient()).thenReturn(restClient);
        doNothing().when(restClient).close();

        final DefaultElasticsearchRestClient client =
                new DefaultElasticsearchRestClient(appbaseCore, elasticsearchLowLevelRestClientService);

        // Act + Assert — must not throw
        Assertions.assertDoesNotThrow(client::shutdown);
    }
}
