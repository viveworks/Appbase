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

package com.viveworks.appbase.core.service.configuration.accessor.store;

import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SchedulerOffloadReactiveConfigurationStore} — a {@link ReactiveConfigurationStore} adapter
 * that offloads a {@link ConfigurationStore}'s blocking sync read through the N01
 * {@link ReactiveReadBoundary}. Config is offload-only (no Lucene variant), so this is the sole reactive
 * config store.
 *
 * <p>Per the "add no new dependency" constraint, {@code reactor-test}'s {@code StepVerifier} is unavailable
 * — this suite asserts observable behaviour via {@link Mono#block(Duration)} instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class SchedulerOffloadReactiveConfigurationStoreTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private final ReactiveReadBoundary reactiveReadBoundary = CoreTestFixtures.getReactiveReadBoundary();

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(Exception.class,
                () -> new SchedulerOffloadReactiveConfigurationStore(null, reactiveReadBoundary));
    }

    @Test
    void constructorRejectsNullBoundary() {
        final ConfigurationStore delegate = mock(ConfigurationStore.class);

        assertThrows(Exception.class,
                () -> new SchedulerOffloadReactiveConfigurationStore(delegate, null));
    }

    @Test
    void readRawConfigurationsReactiveOffloadsToDelegateAndReturnsItsResult() throws Exception {
        final ConfigurationStore delegate = mock(ConfigurationStore.class);
        final ConfigurationAccessorContext context = mock(ConfigurationAccessorContext.class);
        final RawConfiguration raw = rawConfiguration("marker.properties");
        when(delegate.readRawConfigurations(any())).thenReturn(List.of(raw));
        final SchedulerOffloadReactiveConfigurationStore store =
                new SchedulerOffloadReactiveConfigurationStore(delegate, reactiveReadBoundary);

        final Collection<RawConfiguration> result =
                store.readRawConfigurationsReactive(context).block(BLOCK_TIMEOUT);

        assertTrue(result != null && result.contains(raw));
        verify(delegate).readRawConfigurations(context);
    }

    @Test
    void delegateErrorSurfacesThroughTheMono() throws Exception {
        final ConfigurationStore delegate = mock(ConfigurationStore.class);
        final ConfigurationAccessorContext context = mock(ConfigurationAccessorContext.class);
        final ConfigurationStoreException boom = new ConfigurationStoreException("boom");
        when(delegate.readRawConfigurations(any())).thenThrow(boom);
        final SchedulerOffloadReactiveConfigurationStore store =
                new SchedulerOffloadReactiveConfigurationStore(delegate, reactiveReadBoundary);

        final Mono<Collection<RawConfiguration>> mono = store.readRawConfigurationsReactive(context);

        final Throwable thrown = assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));
        assertTrue(thrown == boom || thrown.getCause() == boom,
                "expected the underlying ConfigurationStoreException to propagate, got " + thrown);
    }

    private static RawConfiguration rawConfiguration(final String name) {
        return RawConfiguration.builder()
                .name(name)
                .value(new byte[] {1, 2, 3})
                .mimeType(MimeType.PROPERTIES)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
