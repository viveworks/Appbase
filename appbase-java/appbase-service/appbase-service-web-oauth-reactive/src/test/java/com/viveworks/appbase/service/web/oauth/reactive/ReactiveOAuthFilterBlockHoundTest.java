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

package com.viveworks.appbase.service.web.oauth.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenKeyHashing;
import com.viveworks.appbase.service.web.oauth.TokenValidator;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * The non-blocking guarantee for {@link ReactiveOAuthFilter} (S02, spec §4.5 / §10 bullet 6). The
 * filter's SPI work — {@code validate} and the cache {@code get}/{@code put} — is blocking (the Nimbus
 * engine does crypto; a cache backend does I/O), so it is offloaded to
 * {@link Schedulers#boundedElastic()} and must never run on the reactor event loop.
 *
 * <p>This suite proves that with BlockHound, which rewrites JDK internals to throw when a blocking call
 * runs on a non-blocking scheduler thread. {@link #blockHoundIsArmedAndDetectsBlockingOnANonBlockingThread()}
 * guards against vacuity: if BlockHound silently failed to install, the offload assertion would pass
 * whether or not the code blocks. {@link #offloadsBlockingValidationOffANonBlockingSubscriberThread()} is
 * the real assertion — subscribing the filter on {@link Schedulers#parallel()} (standing in for the event
 * loop) with a deliberately blocking validator, and proving the subscriber thread is never blocked.
 *
 * <p>Mutation-verified: deleting {@code .subscribeOn(Schedulers.boundedElastic())} from the filter runs
 * the blocking validator on the {@code parallel} subscriber thread; BlockHound throws a
 * {@code BlockingOperationError} inside the validation callable, the filter's fail-closed
 * {@code onErrorResume} converts it to a 401, and the offload test turns red because the validated token
 * is then never attached and the chain never continues. The token-attached / chain-continued assertion is
 * therefore the discriminating one — it holds only when the blocking work actually ran, offloaded.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReactiveOAuthFilterBlockHoundTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String RAW_TOKEN = "eyJhbGciOiJSUzI1NiJ9.payload.signature";
    private static final TokenKeyHasher HASHER = new Sha256TokenKeyHasher();
    private static final String ISSUER = "https://issuer.example";

    @BeforeAll
    static void installBlockHound() {
        // Requires -XX:+AllowRedefinitionToAddDeleteMethods, supplied by the module's surefire argLine.
        // BlockHound 1.0.13 is the first release that instruments on JDK 25.
        BlockHound.install();
    }

    @Test
    void blockHoundIsArmedAndDetectsBlockingOnANonBlockingThread() {
        assertThrows(Throwable.class, () ->
                        Mono.fromRunnable(() -> sleep(10))
                                .subscribeOn(Schedulers.parallel())
                                .block(Duration.ofSeconds(5)),
                "BlockHound must detect Thread.sleep on a parallel scheduler thread; if this does not "
                        + "throw, BlockHound failed to install and the offload assertion is vacuous");
    }

    @Test
    void offloadsBlockingValidationOffANonBlockingSubscriberThread() {
        TokenValidator validator = mock(TokenValidator.class);
        // The blocking SPI call. On a non-blocking thread BlockHound would throw here; the filter's
        // boundedElastic offload is what keeps it off the parallel subscriber thread below.
        when(validator.validate(RAW_TOKEN)).thenAnswer(invocation -> {
            sleep(20);
            return validToken(NOW.plusSeconds(300));
        });
        TokenCache cache = mock(TokenCache.class);
        when(cache.get(anyString())).thenReturn(Optional.empty());

        AppbaseServiceExchange exchange = mock(AppbaseServiceExchange.class);
        when(exchange.header("Authorization")).thenReturn("Bearer " + RAW_TOKEN);
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.attributes()).thenReturn(attributes);
        ReactiveWebFilterChain chain = mock(ReactiveWebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        ReactiveOAuthFilter filter = new ReactiveOAuthFilter(validator, cache, HASHER,
                config(), Clock.fixed(NOW, ZoneOffset.UTC));

        // Subscribe on the parallel scheduler — a BlockHound-instrumented non-blocking thread, standing
        // in for the reactor event loop. If the blocking validate() leaks onto that thread (offload lost),
        // BlockHound throws inside the callable; the filter's fail-closed onErrorResume then rejects with a
        // 401, so the token is never attached and the chain never runs. block() completes on the (main)
        // test thread, which is not instrumented.
        filter.filter(exchange, chain).subscribeOn(Schedulers.parallel()).block(Duration.ofSeconds(5));

        // The discriminating assertion: the request was authenticated and forwarded, which happens only
        // when the blocking validation actually ran — offloaded off the parallel subscriber thread. Under
        // the offload-removed mutation, BlockHound trips and this token is absent (a 401 instead).
        assertEquals(validToken(NOW.plusSeconds(300)),
                attributes.get(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE));
        verify(chain).filter(exchange);
    }

    private static OAuthConfiguration config() {
        return new OAuthConfiguration(URI.create("https://issuer.example/jwks"), ISSUER, "client-1",
                "aud", 30L, 300L, TokenKeyHashing.SHA256, null);
    }

    private static ValidatedToken validToken(Instant expiresAt) {
        return new ValidatedToken("sub", ISSUER, Set.of("api"), expiresAt, Map.of(), "unused-in-n01");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
